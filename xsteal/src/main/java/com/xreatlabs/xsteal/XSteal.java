package com.xreatlabs.xsteal;

import com.xreatlabs.xsteal.abilities.AbilityManager;
import com.xreatlabs.xsteal.banbox.BanBoxManager;
import com.xreatlabs.xsteal.commands.EnhancedXStealCommand;
import com.xreatlabs.xsteal.heads.HeadManager;
import com.xreatlabs.xsteal.systems.LifeManager;
import com.xreatlabs.xsteal.systems.RecipeManager;
import com.xreatlabs.xsteal.utils.AntiTamper;
import com.xreatlabs.xsteal.utils.ConfigManager;
import com.xreatlabs.xsteal.utils.LibbyManager;
import com.xreatlabs.xsteal.utils.Logger;
import com.xreatlabs.xsteal.utils.VersionCompatibility;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * XSteal - PSD1 Inspired Minecraft Plugin
 * 
 * Features:
 * - Mob head acquisition via charged creeper kills
 * - 58+ unique mob abilities activated by wearing heads
 * - Boss head combo system (3 abilities per boss)
 * - BanBox system with spectator mode and revival
 * - Multi-layered obfuscation and anti-tamper protection
 * 
 * Compatible with Paper/Spigot 1.8-1.21.4
 * 
 * @author XreatLabs
 * @version 1.0.0
 */
public class XSteal extends JavaPlugin {
    
    // Plugin instance
    private static XSteal instance;
    
    // Core managers
    private Logger logger;
    private ConfigManager configManager;
    private LibbyManager libbyManager;
    private HeadManager headManager;
    private AbilityManager abilityManager;
    private BanBoxManager banBoxManager;
    private LifeManager lifeManager;
    private RecipeManager recipeManager;
    
    // Plugin state
    private final AtomicBoolean pluginReady = new AtomicBoolean(false);
    private boolean dependenciesLoaded = false;
    
    @Override
    public void onLoad() {
        // Set instance
        instance = this;
        
        // Initialize logger
        logger = new Logger(this);
        logger.info("=== XSteal v" + getDescription().getVersion() + " Loading ===");
        logger.info("PSD1 Inspired Minecraft Plugin");
        logger.info("Compatible with Paper/Spigot 1.8-1.21.4");
        
        // Version compatibility check
        if (!VersionCompatibility.isSupported()) {
            logger.severe("Unsupported server version: " + VersionCompatibility.getServerVersion());
            logger.severe("XSteal supports Minecraft 1.8-1.21.4");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        logger.info("Server version: " + VersionCompatibility.getServerVersion() + " (Supported)");
        
        // Anti-tamper verification
        if (getConfig().getBoolean("security.anti_tamper", true)) {
            try {
                AntiTamper.verify();
                logger.info("Anti-tamper verification passed");
            } catch (SecurityException e) {
                logger.severe("Anti-tamper verification failed: " + e.getMessage());
                if (getConfig().getBoolean("security.strict_mode", false)) {
                    getServer().getPluginManager().disablePlugin(this);
                    return;
                }
            }
        }
        
        // Create data folder
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        logger.info("Plugin loaded successfully");
    }
    
    @Override
    public void onEnable() {
        logger.info("=== Enabling XSteal ===");
        
        // Initialize configuration manager
        configManager = new ConfigManager(this);
        configManager.loadConfigurations();
        logger.info("Configuration loaded");
        
        // Initialize Libby manager for dependency management
        if (configManager.isLibbyEnabled()) {
            logger.info("Initializing Libby dependency manager...");
            libbyManager = new LibbyManager(this);
            
            // Load dependencies asynchronously
            CompletableFuture.runAsync(this::loadDependencies)
                .thenRun(this::initializePlugin)
                .exceptionally(throwable -> {
                    logger.severe("Failed to load dependencies: " + throwable.getMessage());
                    initializePluginWithoutDependencies();
                    return null;
                });
        } else {
            logger.info("Libby disabled, initializing without runtime dependencies");
            initializePluginWithoutDependencies();
        }
    }
    
    /**
     * Load runtime dependencies via Libby
     */
    private void loadDependencies() {
        try {
            logger.info("Loading HeadDatabase API...");
            libbyManager.loadHeadDatabaseAPI();
            
            logger.info("Loading additional dependencies...");
            libbyManager.loadConfiguredDependencies();
            
            dependenciesLoaded = true;
            logger.info("All dependencies loaded successfully");
            
        } catch (Exception e) {
            logger.severe("Failed to load dependencies: " + e.getMessage());
            dependenciesLoaded = false;
        }
    }
    
    /**
     * Initialize plugin with full functionality
     */
    private void initializePlugin() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    logger.info("Initializing core managers...");
                    
                    // Initialize managers
                    headManager = new HeadManager(XSteal.this, dependenciesLoaded);
                    abilityManager = new AbilityManager(XSteal.this);
                    banBoxManager = new BanBoxManager(XSteal.this);
                    lifeManager = new LifeManager(XSteal.this);
                    recipeManager = new RecipeManager(XSteal.this);
                    
                    logger.info("Loading head configurations...");
                    headManager.loadHeads();
                    
                    logger.info("Registering abilities...");
                    abilityManager.registerAbilities();
                    
                    logger.info("Registering event listeners...");
                    registerListeners();
                    
                    logger.info("Registering commands...");
                    registerCommands();
                    
                    // Plugin is ready
                    pluginReady.set(true);
                    logger.info("=== XSteal Enabled Successfully ===");
                    logger.info("Loaded " + headManager.getLoadedHeadCount() + " mob heads");
                    logger.info("Registered " + abilityManager.getRegisteredAbilityCount() + " abilities");
                    logger.info("BanBox system: " + (banBoxManager.isEnabled() ? "Enabled" : "Disabled"));
                    
                    // Start background tasks
                    startBackgroundTasks();
                    
                } catch (Exception e) {
                    logger.severe("Failed to initialize plugin: " + e.getMessage());
                    e.printStackTrace();
                    getServer().getPluginManager().disablePlugin(XSteal.this);
                }
            }
        }.runTask(this);
    }
    
    /**
     * Initialize plugin without HeadDatabase (fallback mode)
     */
    private void initializePluginWithoutDependencies() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    logger.info("Initializing in fallback mode (no HeadDatabase)...");
                    
                    // Initialize managers with limited functionality
                    headManager = new HeadManager(XSteal.this, false);
                    abilityManager = new AbilityManager(XSteal.this);
                    banBoxManager = new BanBoxManager(XSteal.this);
                    lifeManager = new LifeManager(XSteal.this);
                    recipeManager = new RecipeManager(XSteal.this);
                    
                    // Load basic configurations
                    headManager.loadHeadsWithoutHDB();
                    abilityManager.registerAbilities();
                    
                    // Register listeners and commands
                    registerListeners();
                    registerCommands();
                    
                    pluginReady.set(true);
                    logger.info("=== XSteal Enabled (Fallback Mode) ===");
                    logger.info("Some features may be limited without HeadDatabase integration");
                    
                    startBackgroundTasks();
                    
                } catch (Exception e) {
                    logger.severe("Failed to initialize plugin in fallback mode: " + e.getMessage());
                    e.printStackTrace();
                    getServer().getPluginManager().disablePlugin(XSteal.this);
                }
            }
        }.runTask(this);
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        // Head acquisition listener (charged creeper kills)
        getServer().getPluginManager().registerEvents(
            new com.xreatlabs.xsteal.heads.HeadDropListener(this), this);
        
        // Ability activation listeners
        getServer().getPluginManager().registerEvents(
            new com.xreatlabs.xsteal.abilities.AbilityListener(this), this);
        
        // BanBox system listeners
        getServer().getPluginManager().registerEvents(
            new com.xreatlabs.xsteal.banbox.BanBoxListener(this), this);
        
        // Boss combo listeners
        getServer().getPluginManager().registerEvents(
            new com.xreatlabs.xsteal.abilities.BossComboListener(this), this);
        
        logger.info("Event listeners registered");
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        EnhancedXStealCommand commandHandler = new EnhancedXStealCommand(this);
        getCommand("xsteal").setExecutor(commandHandler);
        getCommand("xsteal").setTabCompleter(commandHandler);
        
        logger.info("Enhanced commands registered");
    }
    
    /**
     * Start background tasks
     */
    private void startBackgroundTasks() {
        // BanBox timer task
        if (banBoxManager.isEnabled()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    banBoxManager.processBanBoxTimers();
                }
            }.runTaskTimerAsynchronously(this, 20L * 60L, 20L * 60L); // Every minute
        }
        
        // Entity cleanup task
        new BukkitRunnable() {
            @Override
            public void run() {
                abilityManager.cleanupSummonedEntities();
            }
        }.runTaskTimerAsynchronously(this, 
            20L * configManager.getEntityCleanupInterval(), 
            20L * configManager.getEntityCleanupInterval());
        
        // Performance monitoring task (if debug enabled)
        if (configManager.isDebugMode()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    logger.debug("Active abilities: " + abilityManager.getActiveAbilityCount());
                    logger.debug("BanBox players: " + banBoxManager.getBanBoxPlayerCount());
                    logger.debug("Summoned entities: " + abilityManager.getSummonedEntityCount());
                    logger.debug("Memory usage: " + getMemoryUsage() + "MB");
                }
            }.runTaskTimerAsynchronously(this, 20L * 60L * 5L, 20L * 60L * 5L); // Every 5 minutes
        }
        
        logger.info("Background tasks started");
    }
    
    @Override
    public void onDisable() {
        logger.info("=== Disabling XSteal ===");
        
        // Cancel all tasks
        getServer().getScheduler().cancelTasks(this);
        
        // Save data
        if (banBoxManager != null) {
            banBoxManager.saveData();
        }
        
        if (lifeManager != null) {
            lifeManager.saveData();
        }
        
        if (abilityManager != null) {
            abilityManager.cleanup();
        }
        
        if (headManager != null) {
            headManager.cleanup();
        }
        
        if (recipeManager != null) {
            recipeManager.cleanup();
        }
        
        // Clear managers
        headManager = null;
        abilityManager = null;
        banBoxManager = null;
        lifeManager = null;
        recipeManager = null;
        libbyManager = null;
        configManager = null;
        
        pluginReady.set(false);
        logger.info("XSteal disabled successfully");
    }
    
    /**
     * Reload plugin configuration and data
     */
    public void reload() {
        logger.info("Reloading XSteal...");
        
        // Reload configurations
        configManager.reloadConfigurations();
        
        // Reload managers
        if (headManager != null) {
            headManager.reload();
        }
        
        if (abilityManager != null) {
            abilityManager.reload();
        }
        
        if (banBoxManager != null) {
            banBoxManager.reload();
        }
        
        if (lifeManager != null) {
            lifeManager.saveData();
        }
        
        logger.info("XSteal reloaded successfully");
    }
    
    // Getters
    public static XSteal getInstance() {
        return instance;
    }
    
    public Logger getPluginLogger() {
        return logger;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public LibbyManager getLibbyManager() {
        return libbyManager;
    }
    
    public HeadManager getHeadManager() {
        return headManager;
    }
    
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
    
    public BanBoxManager getBanBoxManager() {
        return banBoxManager;
    }
    
    public LifeManager getLifeManager() {
        return lifeManager;
    }
    
    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
    
    public boolean isPluginReady() {
        return pluginReady.get();
    }
    
    public boolean areDependenciesLoaded() {
        return dependenciesLoaded;
    }
    
    /**
     * Get current memory usage in MB
     */
    private long getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }
}