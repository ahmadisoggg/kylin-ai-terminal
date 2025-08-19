package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Custom notification system for XSteal
 * Replaces DiscordSRV with our own implementation
 * Handles server-wide announcements and logging
 */
public class NotificationSystem {
    
    private final XSteal plugin;
    private final List<NotificationListener> listeners;
    private final File logFile;
    private final SimpleDateFormat dateFormat;
    
    // Notification types
    public enum NotificationType {
        HEAD_DROP("§6⚡ Head Drop"),
        BOSS_HEAD_DROP("§6🏆 Boss Head Drop"),
        PLAYER_DEATH("§c💀 Player Death"),
        PLAYER_REVIVAL("§a🎉 Player Revival"),
        CHARGED_CREEPER("§e⚡ Charged Creeper"),
        ABILITY_USE("§b⚡ Ability Use"),
        ADMIN_ACTION("§c🔧 Admin Action");
        
        private final String prefix;
        
        NotificationType(String prefix) {
            this.prefix = prefix;
        }
        
        public String getPrefix() {
            return prefix;
        }
    }
    
    public NotificationSystem(XSteal plugin) {
        this.plugin = plugin;
        this.listeners = new CopyOnWriteArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.logFile = new File(plugin.getDataFolder(), "notifications.log");
        
        // Create log file
        if (!logFile.exists()) {
            try {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getPluginLogger().warning("Failed to create notifications log: " + e.getMessage());
            }
        }
    }
    
    /**
     * Send notification to all systems
     */
    public void sendNotification(NotificationType type, String message, Player... players) {
        String formattedMessage = type.getPrefix() + " " + message;
        
        // Log notification
        logNotification(type, message);
        
        // Send to server chat
        if (shouldBroadcast(type)) {
            Bukkit.broadcastMessage(formattedMessage);
        }
        
        // Send to specific players
        for (Player player : players) {
            if (player != null && player.isOnline()) {
                player.sendMessage(formattedMessage);
            }
        }
        
        // Send to custom listeners
        for (NotificationListener listener : listeners) {
            try {
                listener.onNotification(type, message, players);
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Notification listener error: " + e.getMessage());
            }
        }
    }
    
    /**
     * Send head drop notification
     */
    public void notifyHeadDrop(String mobType, String headName, Location location, Player killer) {
        String message = String.format("A charged creeper killed a %s and dropped %s!", 
            mobType.replace("_", " "), headName);
        
        sendNotification(NotificationType.HEAD_DROP, message);
        
        // Special effects for nearby players
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= 50) {
                player.sendMessage("§e⚡ Head drop nearby! " + headName);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.5f);
            }
        }
    }
    
    /**
     * Send boss head drop notification
     */
    public void notifyBossHeadDrop(String bossType, String headName, Player killer) {
        String message = String.format("§l%s defeated %s with a creeper arrow and obtained %s!", 
            killer != null ? killer.getName() : "Someone",
            bossType.replace("_", " ").toUpperCase(),
            headName);
        
        sendNotification(NotificationType.BOSS_HEAD_DROP, message);
        
        // Epic server-wide announcement
        new BukkitRunnable() {
            int count = 0;
            
            @Override
            public void run() {
                if (count >= 3) {
                    cancel();
                    return;
                }
                
                String announcement = "§6§l" + "=".repeat(50);
                Bukkit.broadcastMessage(announcement);
                Bukkit.broadcastMessage("§6§l          🏆 LEGENDARY BOSS HEAD OBTAINED! 🏆");
                Bukkit.broadcastMessage("§e§l" + message);
                Bukkit.broadcastMessage(announcement);
                
                // Play sound to all players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }
                
                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second for 3 seconds
    }
    
    /**
     * Send player death notification
     */
    public void notifyPlayerDeath(Player player, Player killer) {
        String message = String.format("%s has been trapped in the BanBox!", player.getName());
        if (killer != null) {
            message += String.format(" (Killed by %s)", killer.getName());
        }
        
        sendNotification(NotificationType.PLAYER_DEATH, message);
    }
    
    /**
     * Send player revival notification
     */
    public void notifyPlayerRevival(Player player, Player reviver) {
        String message = String.format("%s has been revived from the BanBox!", player.getName());
        if (reviver != null) {
            message += String.format(" (Revived by %s)", reviver.getName());
        }
        
        sendNotification(NotificationType.PLAYER_REVIVAL, message);
    }
    
    /**
     * Send charged creeper notification
     */
    public void notifyChargedCreeper(Location location, String cause) {
        String message = String.format("A creeper has been charged by %s at %d, %d, %d", 
            cause, location.getBlockX(), location.getBlockY(), location.getBlockZ());
        
        // Only send to nearby players (not server-wide)
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distance(location) <= 100) {
                player.sendMessage(NotificationType.CHARGED_CREEPER.getPrefix() + " " + message);
            }
        }
        
        logNotification(NotificationType.CHARGED_CREEPER, message);
    }
    
    /**
     * Send ability use notification (for special abilities)
     */
    public void notifyAbilityUse(Player player, String abilityName, boolean isBossAbility) {
        if (!isBossAbility) {
            return; // Only notify for boss abilities
        }
        
        String message = String.format("%s used boss ability: %s", 
            player.getName(), abilityName);
        
        // Send to nearby players only
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (nearbyPlayer.getLocation().distance(player.getLocation()) <= 30) {
                nearbyPlayer.sendMessage(NotificationType.ABILITY_USE.getPrefix() + " " + message);
            }
        }
        
        logNotification(NotificationType.ABILITY_USE, message);
    }
    
    /**
     * Send admin action notification
     */
    public void notifyAdminAction(Player admin, String action, String target) {
        String message = String.format("Admin %s performed action: %s on %s", 
            admin.getName(), action, target);
        
        // Send to admins only
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("xsteal.admin.notifications")) {
                player.sendMessage(NotificationType.ADMIN_ACTION.getPrefix() + " " + message);
            }
        }
        
        logNotification(NotificationType.ADMIN_ACTION, message);
    }
    
    /**
     * Check if notification type should be broadcast
     */
    private boolean shouldBroadcast(NotificationType type) {
        switch (type) {
            case HEAD_DROP:
                return plugin.getConfigManager().getMainConfig().getBoolean("notifications.broadcast_head_drops", true);
            case BOSS_HEAD_DROP:
                return plugin.getConfigManager().getMainConfig().getBoolean("notifications.broadcast_boss_drops", true);
            case PLAYER_DEATH:
                return plugin.getConfigManager().getMainConfig().getBoolean("notifications.broadcast_deaths", true);
            case PLAYER_REVIVAL:
                return plugin.getConfigManager().getMainConfig().getBoolean("notifications.broadcast_revivals", true);
            case CHARGED_CREEPER:
                return false; // Never broadcast creeper notifications
            case ABILITY_USE:
                return false; // Never broadcast ability use
            case ADMIN_ACTION:
                return false; // Never broadcast admin actions
            default:
                return false;
        }
    }
    
    /**
     * Log notification to file
     */
    private void logNotification(NotificationType type, String message) {
        if (!logFile.exists()) {
            return;
        }
        
        try (FileWriter writer = new FileWriter(logFile, true)) {
            String logEntry = String.format("[%s] [%s] %s%n", 
                dateFormat.format(new Date()), 
                type.name(), 
                ChatColor.stripColor(message));
            writer.write(logEntry);
        } catch (IOException e) {
            plugin.getPluginLogger().warning("Failed to write notification log: " + e.getMessage());
        }
    }
    
    /**
     * Add custom notification listener
     */
    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove custom notification listener
     */
    public void removeListener(NotificationListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Interface for custom notification listeners
     */
    public interface NotificationListener {
        void onNotification(NotificationType type, String message, Player... players);
    }
    
    /**
     * Cleanup notification system
     */
    public void cleanup() {
        listeners.clear();
    }
}