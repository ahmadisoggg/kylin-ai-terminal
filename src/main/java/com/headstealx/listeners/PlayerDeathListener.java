package com.headstealx.listeners;

import com.headstealx.Main;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Handles player deaths and banbox system integration
 * When players die, they enter banbox mode and their head drops
 */
public class PlayerDeathListener implements Listener {
    
    private final Main plugin;
    
    public PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle player death and banbox system
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getEntity();
        
        // Check if banbox system is enabled
        if (!plugin.getConfig().getBoolean("banbox.enabled", true)) {
            plugin.getPluginLogger().debug("Banbox system disabled, normal death for " + player.getName());
            return;
        }
        
        // Check if player should be banboxed
        if (!shouldPlayerBeBanboxed(player, event)) {
            plugin.getPluginLogger().debug("Player " + player.getName() + " should not be banboxed");
            return;
        }
        
        plugin.getPluginLogger().info("Processing banbox death for player: " + player.getName());
        
        // Handle banbox death
        plugin.getBanBoxManager().handlePlayerDeath(player, player.getLocation());
        
        // Modify death message if configured
        if (plugin.getConfig().getBoolean("banbox.custom_death_message", true)) {
            String customMessage = "§c" + player.getName() + " §7has been banboxed! Find their head to revive them.";
            event.setDeathMessage(customMessage);
        }
        
        // Clear drops if keep_inventory is enabled for banbox
        if (plugin.getConfig().getBoolean("banbox.keep_inventory", false)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
        
        // Set custom respawn location (will be overridden by banbox system)
        // The actual spectator mode will be set in BanBoxManager
    }
    
    /**
     * Check if player should be banboxed
     */
    private boolean shouldPlayerBeBanboxed(Player player, PlayerDeathEvent event) {
        // Don't banbox creative mode players
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        
        // Don't banbox spectator mode players
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        
        // Don't banbox players with bypass permission
        if (player.hasPermission("headsteal.admin.bypass")) {
            return false;
        }
        
        // Don't banbox if player doesn't have drop permission
        if (!player.hasPermission("headsteal.drop")) {
            return false;
        }
        
        // Check world restrictions
        String worldName = player.getWorld().getName();
        
        // Check disabled worlds
        if (plugin.getConfig().getStringList("general.head_drop.disabled_worlds").contains(worldName)) {
            return false;
        }
        
        // Check enabled worlds (if list is not empty, only those worlds are enabled)
        java.util.List<String> enabledWorlds = plugin.getConfig().getStringList("general.head_drop.enabled_worlds");
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(worldName)) {
            return false;
        }
        
        // Check if death was caused by certain means that shouldn't trigger banbox
        String deathCause = event.getDeathMessage();
        if (deathCause != null) {
            String lowerCause = deathCause.toLowerCase();
            
            // Don't banbox for void deaths (might cause issues)
            if (lowerCause.contains("fell out of the world") || lowerCause.contains("void")) {
                return false;
            }
            
            // Don't banbox for /kill command deaths
            if (lowerCause.contains("killed") && player.hasPermission("headsteal.admin.bypass")) {
                return false;
            }
        }
        
        // Check if player is already banboxed
        if (plugin.getBanBoxManager().isBanned(player.getUniqueId())) {
            plugin.getPluginLogger().debug("Player " + player.getName() + " is already banboxed");
            return false;
        }
        
        return true;
    }
    
    /**
     * Handle player respawn (redirect to banbox if needed)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is banboxed
        if (!plugin.getBanBoxManager().isBanned(player.getUniqueId())) {
            return;
        }
        
        // Get ban data
        var banData = plugin.getBanBoxManager().getBanData(player.getUniqueId());
        if (banData == null) {
            return;
        }
        
        plugin.getPluginLogger().debug("Respawning banboxed player: " + player.getName());
        
        // Set respawn location to death location
        if (banData.getDeathLocation() != null) {
            event.setRespawnLocation(banData.getDeathLocation().add(0, 2, 0));
        }
        
        // Schedule spectator mode setting (needs to be delayed)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && plugin.getBanBoxManager().isBanned(player.getUniqueId())) {
                player.setGameMode(GameMode.SPECTATOR);
                
                // Send banbox reminder
                player.sendMessage("§c§lYou are still banboxed!");
                player.sendMessage("§7Another player must find and left-click your head to revive you.");
                
                plugin.getPluginLogger().debug("Set " + player.getName() + " to spectator mode (banboxed)");
            }
        }, 5L); // 5 tick delay to ensure respawn is complete
    }
    
    /**
     * Handle player login (check banbox status)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is banboxed
        if (!plugin.getBanBoxManager().isBanned(player.getUniqueId())) {
            return;
        }
        
        // Get ban data
        var banData = plugin.getBanBoxManager().getBanData(player.getUniqueId());
        if (banData == null) {
            return;
        }
        
        plugin.getPluginLogger().info("Banboxed player logged in: " + player.getName());
        
        // Schedule banbox mode setup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                // Set to spectator mode
                player.setGameMode(GameMode.SPECTATOR);
                
                // Teleport to death location if available
                if (banData.getDeathLocation() != null) {
                    player.teleport(banData.getDeathLocation().add(0, 2, 0));
                }
                
                // Send banbox status message
                player.sendMessage("§c§l=== BANBOX STATUS ===");
                player.sendMessage("§7You are currently banboxed.");
                player.sendMessage("§7Death time: §f" + new java.util.Date(banData.getBanTime()));
                player.sendMessage("§7Another player must find and left-click your head to revive you.");
                
                if (banData.isPermanentBan()) {
                    int autoUnbanDays = plugin.getConfig().getInt("banbox.auto_unban_days", 7);
                    if (autoUnbanDays > 0) {
                        long timeLeft = (autoUnbanDays * 24L * 60L * 60L * 1000L) - banData.getTimeBanned();
                        if (timeLeft > 0) {
                            long daysLeft = timeLeft / (24L * 60L * 60L * 1000L);
                            player.sendMessage("§c§lYour head was destroyed!");
                            player.sendMessage("§cAuto-unban in: §f" + daysLeft + " days");
                        }
                    }
                }
                
                player.sendMessage("§c§l==================");
                
                plugin.getPluginLogger().debug("Restored banbox mode for " + player.getName());
            }
        }, 10L); // 10 tick delay to ensure login is complete
    }
    
    /**
     * Handle player logout (save banbox data)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerLogout(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is banboxed
        if (plugin.getBanBoxManager().isBanned(player.getUniqueId())) {
            plugin.getPluginLogger().debug("Banboxed player logged out: " + player.getName());
            
            // Save banbox data (handled automatically by BanBoxManager)
            plugin.getBanBoxManager().saveData();
        }
    }
}