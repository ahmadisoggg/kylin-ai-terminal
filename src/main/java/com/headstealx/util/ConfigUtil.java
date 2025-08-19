package com.headstealx.util;

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
 * Configuration utility for HeadStealX
 * Handles loading and managing multiple configuration files
 */
public class ConfigUtil {
    
    private static final Map<String, FileConfiguration> configurations = new HashMap<>();
    private static JavaPlugin plugin;
    
    /**
     * Load all configuration files
     */
    public static void loadConfig(JavaPlugin pluginInstance) {
        plugin = pluginInstance;
        
        // Load main config
        loadConfigFile("config.yml");
        
        // Load heads config
        loadConfigFile("heads.yml");
        
        // Load messages config (if exists)
        loadConfigFile("messages.yml", false);
        
        // Validate configurations
        validateConfigurations();
    }
    
    /**
     * Load a specific configuration file
     */
    public static void loadConfigFile(String fileName) {
        loadConfigFile(fileName, true);
    }
    
    /**
     * Load a specific configuration file
     */
    public static void loadConfigFile(String fileName, boolean required) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        // Create file from resource if it doesn't exist
        if (!configFile.exists()) {
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else if (required) {
                plugin.getLogger().severe("Required configuration file not found: " + fileName);
                return;
            } else {
                // Optional file doesn't exist
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
    public static FileConfiguration getConfig(String fileName) {
        return configurations.get(fileName);
    }
    
    /**
     * Get the main plugin configuration
     */
    public static FileConfiguration getMainConfig() {
        return getConfig("config.yml");
    }
    
    /**
     * Get the heads configuration
     */
    public static FileConfiguration getHeadsConfig() {
        return getConfig("heads.yml");
    }
    
    /**
     * Get the messages configuration
     */
    public static FileConfiguration getMessagesConfig() {
        return getConfig("messages.yml");
    }
    
    /**
     * Save a configuration file
     */
    public static void saveConfig(String fileName) {
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
     * Reload all configurations
     */
    public static void reloadConfigurations() {
        configurations.clear();
        loadConfig(plugin);
    }
    
    /**
     * Validate configuration values
     */
    private static void validateConfigurations() {
        FileConfiguration config = getMainConfig();
        if (config == null) {
            plugin.getLogger().severe("Main configuration not loaded!");
            return;
        }
        
        // Validate main config values
        validateMainConfig(config);
        
        // Validate heads config
        FileConfiguration headsConfig = getHeadsConfig();
        if (headsConfig != null) {
            validateHeadsConfig(headsConfig);
        }
    }
    
    /**
     * Validate main configuration
     */
    private static void validateMainConfig(FileConfiguration config) {
        // Validate drop chance
        int dropChance = config.getInt("general.head_drop.drop_chance_percent", 100);
        if (dropChance < 0 || dropChance > 100) {
            plugin.getLogger().warning("Invalid drop_chance_percent: " + dropChance + ". Using default: 100");
            config.set("general.head_drop.drop_chance_percent", 100);
        }
        
        // Validate auto unban days
        int autoUnbanDays = config.getInt("banbox.auto_unban_days", 7);
        if (autoUnbanDays < 0) {
            plugin.getLogger().warning("Invalid auto_unban_days: " + autoUnbanDays + ". Using default: 7");
            config.set("banbox.auto_unban_days", 7);
        }
        
        // Validate cooldown settings
        if (config.getBoolean("abilities.use_cooldowns", false)) {
            int globalCooldown = config.getInt("abilities.global_cooldown", 30);
            if (globalCooldown < 0) {
                plugin.getLogger().warning("Invalid global_cooldown: " + globalCooldown + ". Using default: 30");
                config.set("abilities.global_cooldown", 30);
            }
        }
        
        // Validate economy settings
        if (config.getBoolean("economy.enabled", false)) {
            double reviveCost = config.getDouble("economy.revive_cost", 1000.0);
            if (reviveCost < 0) {
                plugin.getLogger().warning("Invalid revive_cost: " + reviveCost + ". Using default: 1000.0");
                config.set("economy.revive_cost", 1000.0);
            }
        }
        
        // Validate performance settings
        int maxParticles = config.getInt("performance.max_particles", 50);
        if (maxParticles < 1) {
            plugin.getLogger().warning("Invalid max_particles: " + maxParticles + ". Using default: 50");
            config.set("performance.max_particles", 50);
        }
        
        int maxConcurrentAbilities = config.getInt("performance.max_concurrent_abilities", 10);
        if (maxConcurrentAbilities < 1) {
            plugin.getLogger().warning("Invalid max_concurrent_abilities: " + maxConcurrentAbilities + ". Using default: 10");
            config.set("performance.max_concurrent_abilities", 10);
        }
    }
    
    /**
     * Validate heads configuration
     */
    private static void validateHeadsConfig(FileConfiguration config) {
        if (!config.contains("heads")) {
            plugin.getLogger().severe("heads.yml is missing the 'heads' section!");
            return;
        }
        
        int validHeads = 0;
        int invalidHeads = 0;
        
        for (String headKey : config.getConfigurationSection("heads").getKeys(false)) {
            String path = "heads." + headKey;
            
            // Check required fields
            if (!config.contains(path + ".displayName")) {
                plugin.getLogger().warning("Head '" + headKey + "' is missing displayName");
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
            boolean hasBossAbilities = config.contains(path + ".bossAbilities");
            
            if (!hasRegularAbility && !hasBossAbilities) {
                plugin.getLogger().warning("Head '" + headKey + "' has no abilities configured");
                invalidHeads++;
                continue;
            }
            
            // Validate ability parameters
            if (hasRegularAbility) {
                if (!config.contains(path + ".ability.type")) {
                    plugin.getLogger().warning("Head '" + headKey + "' ability is missing type");
                    invalidHeads++;
                    continue;
                }
            }
            
            validHeads++;
        }
        
        plugin.getLogger().info("Validated heads configuration: " + validHeads + " valid, " + invalidHeads + " invalid");
        
        if (validHeads == 0) {
            plugin.getLogger().severe("No valid heads found in configuration!");
        }
    }
    
    /**
     * Get a configuration value with type safety
     */
    public static <T> T getConfigValue(String fileName, String path, T defaultValue) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) {
            return defaultValue;
        }
        
        Object value = config.get(path, defaultValue);
        
        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Invalid type for config value " + path + " in " + fileName + 
                ". Expected: " + defaultValue.getClass().getSimpleName() + 
                ", Got: " + value.getClass().getSimpleName());
            return defaultValue;
        }
    }
    
    /**
     * Set a configuration value
     */
    public static void setConfigValue(String fileName, String path, Object value) {
        FileConfiguration config = getConfig(fileName);
        if (config == null) {
            plugin.getLogger().warning("Cannot set value in unknown configuration: " + fileName);
            return;
        }
        
        config.set(path, value);
    }
    
    /**
     * Check if a configuration path exists
     */
    public static boolean hasConfigPath(String fileName, String path) {
        FileConfiguration config = getConfig(fileName);
        return config != null && config.contains(path);
    }
}