package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LifeManager for XSteal
 * Manages player life points that can be withdrawn and used for various purposes
 * Integrates with the BanBox system and head abilities
 */
public class LifeManager {
    
    private final XSteal plugin;
    private final Map<UUID, Integer> playerLives;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    // Life system configuration
    private static final int DEFAULT_LIVES = 3;
    private static final int MAX_LIVES = 10;
    private static final int LIFE_COST_REVIVE = 1;
    
    public LifeManager(XSteal plugin) {
        this.plugin = plugin;
        this.playerLives = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "lives_data.yml");
        
        loadData();
    }
    
    /**
     * Get player's current life count
     */
    public int getPlayerLives(Player player) {
        return getPlayerLives(player.getUniqueId());
    }
    
    /**
     * Get player's current life count by UUID
     */
    public int getPlayerLives(UUID playerUUID) {
        return playerLives.getOrDefault(playerUUID, DEFAULT_LIVES);
    }
    
    /**
     * Set player's life count
     */
    public void setPlayerLives(Player player, int lives) {
        setPlayerLives(player.getUniqueId(), lives);
    }
    
    /**
     * Set player's life count by UUID
     */
    public void setPlayerLives(UUID playerUUID, int lives) {
        lives = Math.max(0, Math.min(lives, MAX_LIVES));
        playerLives.put(playerUUID, lives);
        saveData();
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            updatePlayerLifeDisplay(player);
        }
    }
    
    /**
     * Add lives to player
     */
    public void addPlayerLives(Player player, int amount) {
        int currentLives = getPlayerLives(player);
        setPlayerLives(player, currentLives + amount);
        
        if (amount > 0) {
            player.sendMessage(ChatColor.GREEN + "💚 +" + amount + " life" + (amount > 1 ? "s" : "") + "!");
            player.sendMessage(ChatColor.GRAY + "Total lives: " + ChatColor.WHITE + getPlayerLives(player));
        }
    }
    
    /**
     * Remove lives from player
     */
    public boolean removePlayerLives(Player player, int amount) {
        int currentLives = getPlayerLives(player);
        
        if (currentLives < amount) {
            return false; // Not enough lives
        }
        
        setPlayerLives(player, currentLives - amount);
        
        if (amount > 0) {
            player.sendMessage(ChatColor.RED + "💔 -" + amount + " life" + (amount > 1 ? "s" : "") + "!");
            player.sendMessage(ChatColor.GRAY + "Remaining lives: " + ChatColor.WHITE + getPlayerLives(player));
        }
        
        return true;
    }
    
    /**
     * Withdraw life from plugin (admin command)
     */
    public boolean withdrawLife(Player admin, int amount) {
        if (!admin.hasPermission("xsteal.admin.life")) {
            admin.sendMessage(ChatColor.RED + "You don't have permission to withdraw life!");
            return false;
        }
        
        if (amount <= 0 || amount > 100) {
            admin.sendMessage(ChatColor.RED + "Invalid amount! Must be between 1 and 100.");
            return false;
        }
        
        // Add lives to admin
        addPlayerLives(admin, amount);
        
        admin.sendMessage(ChatColor.GREEN + "✅ Withdrew " + amount + " life points from the plugin!");
        plugin.getPluginLogger().info("Admin " + admin.getName() + " withdrew " + amount + " life points");
        
        return true;
    }
    
    /**
     * Get life from plugin (admin command)
     */
    public boolean getLifeFromPlugin(Player admin, int amount) {
        if (!admin.hasPermission("xsteal.admin.life")) {
            admin.sendMessage(ChatColor.RED + "You don't have permission to get life from plugin!");
            return false;
        }
        
        if (amount <= 0 || amount > 50) {
            admin.sendMessage(ChatColor.RED + "Invalid amount! Must be between 1 and 50.");
            return false;
        }
        
        // Give lives to admin
        addPlayerLives(admin, amount);
        
        admin.sendMessage(ChatColor.GREEN + "✅ Received " + amount + " life points from the plugin!");
        plugin.getPluginLogger().info("Admin " + admin.getName() + " received " + amount + " life points");
        
        return true;
    }
    
    /**
     * Use life for revival
     */
    public boolean useLifeForRevival(Player reviver, String victimName) {
        int currentLives = getPlayerLives(reviver);
        
        if (currentLives < LIFE_COST_REVIVE) {
            reviver.sendMessage(ChatColor.RED + "💔 Not enough lives! Revival costs " + LIFE_COST_REVIVE + " life.");
            reviver.sendMessage(ChatColor.GRAY + "Your lives: " + ChatColor.WHITE + currentLives);
            return false;
        }
        
        // Remove life cost
        removePlayerLives(reviver, LIFE_COST_REVIVE);
        
        reviver.sendMessage(ChatColor.YELLOW + "💚 Used " + LIFE_COST_REVIVE + " life to revive " + victimName);
        
        return true;
    }
    
    /**
     * Update player's life display (action bar or title)
     */
    private void updatePlayerLifeDisplay(Player player) {
        int lives = getPlayerLives(player);
        String livesDisplay = generateLivesDisplay(lives);
        
        // Send as action bar
        try {
            if (com.xreatlabs.xsteal.utils.VersionCompatibility.isAtLeast(1, 11)) {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(livesDisplay));
            }
        } catch (Exception e) {
            // Fallback to chat message
            player.sendMessage(livesDisplay);
        }
    }
    
    /**
     * Generate visual lives display
     */
    private String generateLivesDisplay(int lives) {
        StringBuilder display = new StringBuilder();
        display.append(ChatColor.RED + "💚 Lives: ");
        
        // Show hearts for lives
        for (int i = 0; i < MAX_LIVES; i++) {
            if (i < lives) {
                display.append(ChatColor.RED + "❤");
            } else {
                display.append(ChatColor.DARK_GRAY + "♡");
            }
        }
        
        display.append(ChatColor.GRAY + " (" + lives + "/" + MAX_LIVES + ")");
        
        return display.toString();
    }
    
    /**
     * Handle player death (life loss)
     */
    public void handlePlayerDeath(Player player) {
        int currentLives = getPlayerLives(player);
        
        if (currentLives > 0) {
            removePlayerLives(player, 1);
            
            int remainingLives = getPlayerLives(player);
            
            if (remainingLives > 0) {
                player.sendMessage(ChatColor.RED + "💔 You lost a life!");
                player.sendMessage(ChatColor.YELLOW + "Remaining lives: " + ChatColor.WHITE + remainingLives);
            } else {
                player.sendMessage(ChatColor.DARK_RED + "💀 You have no lives left!");
                player.sendMessage(ChatColor.RED + "You will be banboxed until revived!");
            }
        }
    }
    
    /**
     * Reset player lives to default
     */
    public void resetPlayerLives(Player player) {
        setPlayerLives(player, DEFAULT_LIVES);
        player.sendMessage(ChatColor.GREEN + "✅ Your lives have been reset to " + DEFAULT_LIVES + "!");
    }
    
    /**
     * Get top players by life count
     */
    public Map<String, Integer> getTopPlayersByLives(int limit) {
        return playerLives.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(limit)
            .collect(java.util.stream.Collectors.toMap(
                entry -> {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    return player != null ? player.getName() : "Unknown";
                },
                Map.Entry::getValue,
                (e1, e2) -> e1,
                java.util.LinkedHashMap::new
            ));
    }
    
    /**
     * Load life data from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Failed to create lives data file: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("player_lives")) {
            for (String uuidString : dataConfig.getConfigurationSection("player_lives").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    int lives = dataConfig.getInt("player_lives." + uuidString);
                    playerLives.put(uuid, lives);
                } catch (Exception e) {
                    plugin.getPluginLogger().warning("Failed to load life data for " + uuidString + ": " + e.getMessage());
                }
            }
        }
        
        plugin.getPluginLogger().info("Loaded life data for " + playerLives.size() + " players");
    }
    
    /**
     * Save life data to file
     */
    public void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        
        // Clear existing data
        dataConfig.set("player_lives", null);
        
        // Save current data
        for (Map.Entry<UUID, Integer> entry : playerLives.entrySet()) {
            dataConfig.set("player_lives." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Failed to save life data: " + e.getMessage());
        }
    }
    
    /**
     * Get default lives amount
     */
    public int getDefaultLives() {
        return DEFAULT_LIVES;
    }
    
    /**
     * Get maximum lives amount
     */
    public int getMaxLives() {
        return MAX_LIVES;
    }
    
    /**
     * Get life cost for revival
     */
    public int getLifeCostRevive() {
        return LIFE_COST_REVIVE;
    }
    
    /**
     * Cleanup life manager
     */
    public void cleanup() {
        saveData();
        playerLives.clear();
    }
}