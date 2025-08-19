package com.xreatlabs.xsteal.banbox;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.GameMode;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles BanBox system events
 * - Player death and BanBox entry
 * - Head interaction for revival
 * - Head destruction handling
 */
public class BanBoxListener implements Listener {
    
    private final XSteal plugin;
    
    public BanBoxListener(XSteal plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player death for BanBox system
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getEntity();
        Player killer = player.getKiller();
        
        // Handle BanBox death
        plugin.getBanBoxManager().handlePlayerDeath(player, player.getLocation(), killer);
        
        // Modify death message for BanBox
        if (plugin.getBanBoxManager().isEnabled()) {
            String customMessage = "§c💀 " + player.getName() + " has been trapped in the BanBox!";
            if (killer != null) {
                customMessage += " §7(Killed by " + killer.getName() + ")";
            }
            event.setDeathMessage(customMessage);
        }
        
        // Handle inventory based on configuration
        boolean allowInventoryAccess = plugin.getConfigManager().getMainConfig()
            .getBoolean("banbox.allow_inventory_access", true);
        
        if (allowInventoryAccess) {
            // Keep inventory in BanBox (player can prepare for revival)
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
    
    /**
     * Handle player respawn (redirect to BanBox)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is banboxed
        if (!plugin.getBanBoxManager().isBanBoxed(player.getUniqueId())) {
            return;
        }
        
        // Get BanBox data
        var banBoxData = plugin.getBanBoxManager().getBanBoxData(player.getUniqueId());
        if (banBoxData == null) {
            return;
        }
        
        plugin.getPluginLogger().debug("Respawning banboxed player: " + player.getName());
        
        // Set respawn location to death location
        if (banBoxData.getDeathLocation() != null) {
            event.setRespawnLocation(banBoxData.getDeathLocation().add(0, 2, 0));
        }
        
        // Schedule spectator mode setting (needs to be delayed)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getBanBoxManager().isBanBoxed(player.getUniqueId())) {
                player.setGameMode(GameMode.SPECTATOR);
                
                // Send BanBox reminder
                player.sendMessage("§c§l🏺 You are still in the BanBox!");
                player.sendMessage("§7Find someone to left-click your head for revival.");
                
                plugin.getPluginLogger().debug("Set " + player.getName() + " to spectator mode (banboxed)");
            }
        }, 10L); // 10 tick delay to ensure respawn is complete
    }
    
    /**
     * Handle player login (restore BanBox state)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(PlayerJoinEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is banboxed
        if (!plugin.getBanBoxManager().isBanBoxed(player.getUniqueId())) {
            return;
        }
        
        // Get BanBox data
        var banBoxData = plugin.getBanBoxManager().getBanBoxData(player.getUniqueId());
        if (banBoxData == null) {
            return;
        }
        
        plugin.getPluginLogger().info("Banboxed player logged in: " + player.getName());
        
        // Schedule BanBox mode restoration
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Set to spectator mode
                player.setGameMode(GameMode.SPECTATOR);
                
                // Teleport to death location if available
                if (banBoxData.getDeathLocation() != null) {
                    player.teleport(banBoxData.getDeathLocation().add(0, 2, 0));
                }
                
                // Send BanBox status message
                player.sendMessage("§c§l═══ BANBOX STATUS ═══");
                player.sendMessage("§7You are currently trapped in the BanBox.");
                player.sendMessage("§7Death time: §f" + new java.util.Date(banBoxData.getBanTime()));
                player.sendMessage("§7Timer: §f" + banBoxData.getTimerDays() + " days");
                
                long timeInBanBox = banBoxData.getTimeInBanBox();
                long hoursInBanBox = timeInBanBox / (1000 * 60 * 60);
                player.sendMessage("§7Time in BanBox: §f" + hoursInBanBox + " hours");
                
                if (banBoxData.getKillerUUID() != null) {
                    Player killer = org.bukkit.Bukkit.getPlayer(banBoxData.getKillerUUID());
                    String killerName = killer != null ? killer.getName() : "Unknown";
                    player.sendMessage("§7Killed by: §f" + killerName);
                }
                
                player.sendMessage("§7Another player must left-click your head to revive you.");
                player.sendMessage("§c§l═══════════════════");
                
                plugin.getPluginLogger().debug("Restored BanBox mode for " + player.getName());
            }
        }, 20L); // 20 tick delay to ensure login is complete
    }
    
    /**
     * Handle player head interaction for revival
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerHeadInteract(PlayerInteractEntityEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if clicked entity is a dropped item
        if (!(event.getRightClicked() instanceof Item)) {
            return;
        }
        
        Item itemEntity = (Item) event.getRightClicked();
        ItemStack itemStack = itemEntity.getItemStack();
        
        // Check if item is a player head
        if (!plugin.getHeadManager().isPlayerHead(itemStack)) {
            return;
        }
        
        // Get player name from head
        String victimName = plugin.getHeadManager().getPlayerFromHead(itemStack);
        if (victimName == null) {
            plugin.getPluginLogger().warning("Player head has no victim data");
            return;
        }
        
        // Check if player has revive permission
        if (!player.hasPermission("xsteal.banbox.revive")) {
            player.sendMessage("§cYou don't have permission to revive players!");
            return;
        }
        
        // Check if victim is actually banboxed
        if (!plugin.getBanBoxManager().isBanBoxed(victimName)) {
            player.sendMessage("§cPlayer " + victimName + " is not in the BanBox!");
            itemEntity.remove(); // Remove invalid head
            return;
        }
        
        plugin.getPluginLogger().info("Player " + player.getName() + " attempting to revive " + victimName);
        
        // Attempt revival
        boolean success = plugin.getBanBoxManager().revivePlayer(victimName, itemEntity.getLocation(), player);
        
        if (success) {
            // Remove the head item
            itemEntity.remove();
            
            // Cancel the event
            event.setCancelled(true);
            
            // Play revival effects
            playRevivalEffects(player, itemEntity.getLocation());
            
            plugin.getPluginLogger().info("Successfully revived " + victimName + " via " + player.getName());
            
        } else {
            player.sendMessage("§cFailed to revive " + victimName + "!");
            plugin.getPluginLogger().warning("Failed to revive " + victimName);
        }
    }
    
    /**
     * Handle left-click on player heads (alternative method)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerHeadLeftClick(EntityDamageByEntityEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle player attackers
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        // Check if target is a dropped item
        if (!(event.getEntity() instanceof Item)) {
            return;
        }
        
        Item itemEntity = (Item) event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();
        
        // Check if item is a player head
        if (!plugin.getHeadManager().isPlayerHead(itemStack)) {
            return;
        }
        
        // Cancel damage event
        event.setCancelled(true);
        
        // Handle as head interaction
        String victimName = plugin.getHeadManager().getPlayerFromHead(itemStack);
        if (victimName != null && plugin.getBanBoxManager().isBanBoxed(victimName)) {
            boolean success = plugin.getBanBoxManager().revivePlayer(victimName, itemEntity.getLocation(), player);
            
            if (success) {
                itemEntity.remove();
                playRevivalEffects(player, itemEntity.getLocation());
            }
        }
    }
    
    /**
     * Handle item despawn (head destruction)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Item itemEntity = event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();
        
        // Check if item is a player head
        if (!plugin.getHeadManager().isPlayerHead(itemStack)) {
            return;
        }
        
        // Get victim name
        String victimName = plugin.getHeadManager().getPlayerFromHead(itemStack);
        if (victimName == null) {
            return;
        }
        
        plugin.getPluginLogger().warning("Player head for " + victimName + " despawned - releasing from BanBox");
        
        // Handle head destruction (release player)
        plugin.getBanBoxManager().handleHeadDestruction(victimName);
    }
    
    /**
     * Play visual and audio effects for successful revival
     */
    private void playRevivalEffects(Player player, org.bukkit.Location location) {
        // Revival particles
        if (plugin.getConfigManager().areParticlesEnabled()) {
            location.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, 
                location.add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0.1);
            
            location.getWorld().spawnParticle(org.bukkit.Particle.HEART, 
                location, 15, 0.8, 0.8, 0.8, 0.1);
            
            location.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, 
                location, 20, 1.2, 1.2, 1.2, 0.1);
        }
        
        // Revival sounds
        if (plugin.getConfigManager().areSoundsEnabled()) {
            location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
            location.getWorld().playSound(location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            
            // Play sound for the reviver too
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
        }
    }
}