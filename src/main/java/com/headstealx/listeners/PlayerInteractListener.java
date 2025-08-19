package com.headstealx.listeners;

import com.headstealx.Main;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles player interactions with banbox heads and other special interactions
 * Primary function: Left-clicking banbox head items to revive players
 */
public class PlayerInteractListener implements Listener {
    
    private final Main plugin;
    
    public PlayerInteractListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player interaction with entities (including dropped items)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
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
        
        // Check if item is a banbox head
        if (!plugin.getHeadManager().isBanboxHead(itemStack)) {
            return;
        }
        
        // Get victim name from head
        String victimName = plugin.getHeadManager().getBanboxVictim(itemStack);
        if (victimName == null) {
            plugin.getPluginLogger().warning("Banbox head has no victim data");
            return;
        }
        
        // Check if player has revive permission
        if (!player.hasPermission("headsteal.revive")) {
            player.sendMessage("§cYou don't have permission to revive players!");
            return;
        }
        
        // Check if victim is actually banboxed
        if (!plugin.getBanBoxManager().isBanned(victimName)) {
            player.sendMessage("§cPlayer " + victimName + " is not banboxed!");
            itemEntity.remove(); // Remove invalid head
            return;
        }
        
        plugin.getPluginLogger().info("Player " + player.getName() + " attempting to revive " + victimName);
        
        // Attempt revival
        boolean success = plugin.getBanBoxManager().revive(victimName, itemEntity.getLocation(), player);
        
        if (success) {
            // Remove the head item
            itemEntity.remove();
            
            // Cancel the event to prevent item pickup
            event.setCancelled(true);
            
            // Play success effects
            playRevivalEffects(player, itemEntity.getLocation());
            
            plugin.getPluginLogger().info("Successfully revived " + victimName + " via " + player.getName());
            
        } else {
            player.sendMessage("§cFailed to revive " + victimName + "!");
            plugin.getPluginLogger().warning("Failed to revive " + victimName);
        }
    }
    
    /**
     * Handle left-click on entities (alternative method for some versions)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerLeftClickEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle player damagers
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
        
        // Check if item is a banbox head
        if (!plugin.getHeadManager().isBanboxHead(itemStack)) {
            return;
        }
        
        // Cancel damage event
        event.setCancelled(true);
        
        // Get victim name from head
        String victimName = plugin.getHeadManager().getBanboxVictim(itemStack);
        if (victimName == null) {
            return;
        }
        
        // Check permissions
        if (!player.hasPermission("headsteal.revive")) {
            player.sendMessage("§cYou don't have permission to revive players!");
            return;
        }
        
        // Check if victim is banboxed
        if (!plugin.getBanBoxManager().isBanned(victimName)) {
            player.sendMessage("§cPlayer " + victimName + " is not banboxed!");
            itemEntity.remove();
            return;
        }
        
        plugin.getPluginLogger().debug("Left-click revival attempt: " + player.getName() + " -> " + victimName);
        
        // Attempt revival
        boolean success = plugin.getBanBoxManager().revive(victimName, itemEntity.getLocation(), player);
        
        if (success) {
            itemEntity.remove();
            playRevivalEffects(player, itemEntity.getLocation());
        } else {
            player.sendMessage("§cFailed to revive " + victimName + "!");
        }
    }
    
    /**
     * Handle item destruction (banbox head destroyed)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(org.bukkit.event.entity.ItemDespawnEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Item itemEntity = event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();
        
        // Check if item is a banbox head
        if (!plugin.getHeadManager().isBanboxHead(itemStack)) {
            return;
        }
        
        // Get victim name
        String victimName = plugin.getHeadManager().getBanboxVictim(itemStack);
        if (victimName == null) {
            return;
        }
        
        plugin.getPluginLogger().warning("Banbox head for " + victimName + " despawned naturally");
        
        // Handle head destruction (permanent ban)
        plugin.getBanBoxManager().handleHeadDestruction(victimName);
    }
    
    /**
     * Handle item burning/destruction by fire, lava, etc.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemBurn(org.bukkit.event.entity.EntityCombustEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Item)) {
            return;
        }
        
        Item itemEntity = (Item) event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();
        
        // Check if item is a banbox head
        if (!plugin.getHeadManager().isBanboxHead(itemStack)) {
            return;
        }
        
        // Get victim name
        String victimName = plugin.getHeadManager().getBanboxVictim(itemStack);
        if (victimName == null) {
            return;
        }
        
        plugin.getPluginLogger().warning("Banbox head for " + victimName + " was destroyed by fire/lava");
        
        // Handle head destruction (permanent ban)
        plugin.getBanBoxManager().handleHeadDestruction(victimName);
    }
    
    /**
     * Handle explosion damage to banbox heads
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if any banbox heads were destroyed in explosion
        for (org.bukkit.entity.Entity entity : event.getEntity().getNearbyEntities(
                event.getYield(), event.getYield(), event.getYield())) {
            
            if (!(entity instanceof Item)) {
                continue;
            }
            
            Item itemEntity = (Item) entity;
            ItemStack itemStack = itemEntity.getItemStack();
            
            // Check if item is a banbox head
            if (!plugin.getHeadManager().isBanboxHead(itemStack)) {
                continue;
            }
            
            // Get victim name
            String victimName = plugin.getHeadManager().getBanboxVictim(itemStack);
            if (victimName == null) {
                continue;
            }
            
            plugin.getPluginLogger().warning("Banbox head for " + victimName + " was destroyed by explosion");
            
            // Handle head destruction (permanent ban)
            plugin.getBanBoxManager().handleHeadDestruction(victimName);
        }
    }
    
    /**
     * Handle player pickup of banbox heads (informational)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(org.bukkit.event.player.PlayerPickupItemEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        ItemStack itemStack = event.getItem().getItemStack();
        
        // Check if item is a banbox head
        if (!plugin.getHeadManager().isBanboxHead(itemStack)) {
            return;
        }
        
        Player player = event.getPlayer();
        String victimName = plugin.getHeadManager().getBanboxVictim(itemStack);
        
        if (victimName != null) {
            player.sendMessage("§e§lYou picked up " + victimName + "'s head!");
            player.sendMessage("§7Left-click while it's dropped to revive them.");
            player.sendMessage("§c§lDon't destroy it or they'll be permanently banned!");
            
            plugin.getPluginLogger().info("Player " + player.getName() + " picked up banbox head for " + victimName);
        }
    }
    
    /**
     * Play visual and audio effects for successful revival
     */
    private void playRevivalEffects(Player player, org.bukkit.Location location) {
        // Play particles
        if (plugin.getConfig().getBoolean("abilities.particles", true)) {
            location.getWorld().spawnParticle(
                org.bukkit.Particle.VILLAGER_HAPPY,
                location.add(0, 1, 0),
                20, 1.0, 1.0, 1.0, 0.1
            );
            
            location.getWorld().spawnParticle(
                org.bukkit.Particle.HEART,
                location,
                10, 0.5, 0.5, 0.5, 0.1
            );
            
            location.getWorld().spawnParticle(
                org.bukkit.Particle.ENCHANTMENT_TABLE,
                location,
                30, 1.5, 1.5, 1.5, 0.1
            );
        }
        
        // Play sounds
        if (plugin.getConfig().getBoolean("abilities.sounds", true)) {
            location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
            location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            
            // Play sound for the reviver too
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }
}