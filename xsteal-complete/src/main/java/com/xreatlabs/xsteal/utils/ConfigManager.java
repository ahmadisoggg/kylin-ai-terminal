package com.xreatlabs.xsteal.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration manager for XSteal
 * Handles loading and managing multiple configuration files
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configurations = new HashMap<>();
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Load all configuration files
     */
    public void loadConfigurations() {
        // Load main config
        loadConfigFile("config.yml");
        
        // Load heads config
        loadConfigFile("heads.yml");
        
        // Validate configurations
        validateConfigurations();
    }
    
    /**
     * Reload all configurations
     */
    public void reloadConfigurations() {
        configurations.clear();
        loadConfigurations();
    }
    
    /**
     * Load a specific configuration file
     */
    private void loadConfigFile(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        // Create file from resource if it doesn't exist
        if (!configFile.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                plugin.getLogger().severe("Required configuration file not found: " + fileName);
                return;
            }
        }
        
        // Load configuration
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Load defaults from resource
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }
        
        configurations.put(fileName, config);
        plugin.getLogger().info("Loaded configuration: " + fileName);
    }
    
    /**
     * Get a configuration by filename
     */
    public FileConfiguration getConfig(String fileName) {
        return configurations.get(fileName);
    }
    
    /**
     * Get the main plugin configuration
     */
    public FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }
    
    /**
     * Get the heads configuration
     */
    public FileConfiguration getHeadsConfig() {
        return getConfig("heads.yml");
    }
    
    /**
     * Save a configuration file
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configurations.get(fileName);
        if (config == null) {
            plugin.getLogger().warning("Cannot save unknown configuration: " + fileName);
            return;
        }
        
        try {
            File configFile = new File(plugin.getDataFolder(), fileName);
            config.save(configFile);
            plugin.getLogger().info("Saved configuration: " + fileName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save configuration " + fileName + ": " + e.getMessage());
        }
    }
    
    /**
     * Validate configuration values
     */
    private void validateConfigurations() {
        FileConfiguration config = getMainConfig();
        if (config == null) {
            plugin.getLogger().severe("Main configuration not loaded!");
            return;
        }
        
        // Validate BanBox timer
        int banboxTimer = config.getInt("banbox.default_timer_days", 7);
        if (banboxTimer < 1) {
            plugin.getLogger().warning("Invalid banbox timer: " + banboxTimer + ". Using default: 7");
            config.set("banbox.default_timer_days", 7);
        }
        
        // Validate head drop chance
        int dropChance = config.getInt("general.head_drops.drop_chance", 100);
        if (dropChance < 0 || dropChance > 100) {
            plugin.getLogger().warning("Invalid drop chance: " + dropChance + ". Using default: 100");
            config.set("general.head_drops.drop_chance", 100);
        }
        
        // Validate ability limits
        int maxConcurrentAbilities = config.getInt("abilities.max_concurrent_abilities", 20);
        if (maxConcurrentAbilities < 1) {
            plugin.getLogger().warning("Invalid max concurrent abilities: " + maxConcurrentAbilities + ". Using default: 20");
            config.set("abilities.max_concurrent_abilities", 20);
        }
        
        // Validate heads configuration
        FileConfiguration headsConfig = getHeadsConfig();
        if (headsConfig != null) {
            validateHeadsConfig(headsConfig);
        }
    }
    
    /**
     * Validate heads configuration
     */
    private void validateHeadsConfig(FileConfiguration config) {
        if (!config.contains("heads")) {
            plugin.getLogger().severe("heads.yml is missing the 'heads' section!");
            return;
        }
        
        int validHeads = 0;
        int invalidHeads = 0;
        
        for (String headKey : config.getConfigurationSection("heads").getKeys(false)) {
            String path = "heads." + headKey;
            
            // Check required fields
            if (!config.contains(path + ".display_name")) {
                plugin.getLogger().warning("Head '" + headKey + "' is missing display_name");
                invalidHeads++;
                continue;
            }
            
            if (!config.contains(path + ".hdb_id")) {
                plugin.getLogger().warning("Head '" + headKey + "' is missing hdb_id");
                invalidHeads++;
                continue;
            }
            
            // Check ability configuration
            boolean hasRegularAbility = config.contains(path + ".ability");
            boolean hasBossAbilities = config.contains(path + ".abilities");
            
            if (!hasRegularAbility && !hasBossAbilities) {
                plugin.getLogger().warning("Head '" + headKey + "' has no abilities configured");
                invalidHeads++;
                continue;
            }
            
            validHeads++;
        }
        
        plugin.getLogger().info("Validated heads configuration: " + validHeads + " valid, " + invalidHeads + " invalid");
        
        if (validHeads == 0) {
            plugin.getLogger().severe("No valid heads found in configuration!");
        }
    }
    
    // Convenience methods for common config values
    
    public boolean isDebugMode() {
        return getMainConfig().getBoolean("general.debug_mode", false);
    }
    
    public boolean isLibbyEnabled() {
        return getMainConfig().getBoolean("libby.enabled", true);
    }
    
    public boolean isBanBoxEnabled() {
        return getMainConfig().getBoolean("banbox.enabled", true);
    }
    
    public int getBanBoxDefaultTimer() {
        return getMainConfig().getInt("banbox.default_timer_days", 7);
    }
    
    public boolean isHelmetSlotRequired() {
        return getMainConfig().getBoolean("abilities.helmet_slot_activation", true);
    }
    
    public boolean areBossCombosEnabled() {
        return getMainConfig().getBoolean("abilities.boss_combos.enabled", true);
    }
    
    public int getDoubleClickWindow() {
        return getMainConfig().getInt("abilities.boss_combos.double_click_window", 500);
    }
    
    public int getComboResetTime() {
        return getMainConfig().getInt("abilities.boss_combos.combo_reset_time", 2000);
    }
    
    public int getEntityCleanupInterval() {
        return getMainConfig().getInt("performance.entity_cleanup_interval_seconds", 300);
    }
    
    public int getMaxSummonedEntities() {
        return getMainConfig().getInt("performance.max_summoned_entities_per_player", 10);
    }
    
    public boolean areParticlesEnabled() {
        return getMainConfig().getBoolean("abilities.particles", true);
    }
    
    public boolean areSoundsEnabled() {
        return getMainConfig().getBoolean("abilities.sounds", true);
    }
    
    public boolean isActionBarFeedbackEnabled() {
        return getMainConfig().getBoolean("abilities.action_bar_feedback", true);
    }
}