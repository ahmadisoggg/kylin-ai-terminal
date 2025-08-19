package com.headstealx;

import com.headstealx.commands.HeadStealXCommand;
import com.headstealx.libby.LibbyWrapper;
import com.headstealx.listeners.ChargedCreeperListener;
import com.headstealx.listeners.PlayerAbilityListener;
import com.headstealx.listeners.PlayerDeathListener;
import com.headstealx.listeners.PlayerInteractListener;
import com.headstealx.managers.AbilityManager;
import com.headstealx.managers.BanBoxManager;
import com.headstealx.managers.HeadManager;
import com.headstealx.util.AntiTamper;
import com.headstealx.util.ConfigUtil;
import com.headstealx.util.Logger;
import com.headstealx.util.UpdateChecker;
import com.headstealx.util.VersionUtil;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HeadStealX Main Plugin Class
 * 
 * A premium Minecraft plugin that adds mob head drops with unique abilities
 * Compatible with Paper/Spigot 1.8-1.21.8
 * 
 * Features:
 * - 58 unique mob heads with abilities
 * - Charged creeper head drops
 * - BanBox revival system
 * - Boss heads with combo abilities
 * - Runtime dependency management via Libby
 * - Multi-version compatibility
 * - Anti-tamper protection
 * 
 * @author HeadStealX Team
 * @version 1.0.0
 */
public class Main extends JavaPlugin {
    
    // Plugin instance
    private static Main instance;
    
    // Core managers
    private HeadManager headManager;
    private AbilityManager abilityManager;
    private BanBoxManager banBoxManager;
    
    // Libby wrapper for dependency management
    private LibbyWrapper libbyWrapper;
    
    // Plugin state
    private final AtomicBoolean pluginReady = new AtomicBoolean(false);
    private boolean dependenciesLoaded = false;
    
    // Logging phases
    private Logger logger;
    
    @Override
    public void onLoad() {
        // Set instance
        instance = this;
        
        // Initialize logger
        logger = new Logger(this);
        logger.startup("HeadStealX v" + getDescription().getVersion() + " is loading...");
        
        // Version compatibility check
        if (!VersionUtil.isSupported()) {
            logger.severe("Unsupported server version: " + VersionUtil.getServerVersion());
            logger.severe("HeadStealX supports Minecraft 1.8-1.21.8");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        logger.startup("Server version: " + VersionUtil.getServerVersion() + " (Supported)");
        
        // Anti-tamper check
        if (getConfig().getBoolean("security.anti_tamper", true)) {
            try {
                AntiTamper.verify();
                logger.startup("Anti-tamper verification passed");
            } catch (Exception e) {
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
        
        logger.startup("Plugin loaded successfully");
    }
    
    @Override
    public void onEnable() {
        logger.startup("Enabling HeadStealX...");
        
        // Load configuration
        saveDefaultConfig();
        ConfigUtil.loadConfig(this);
        logger.startup("Configuration loaded");
        
        // Initialize Libby wrapper
        if (getConfig().getBoolean("libby.enabled", true)) {
            logger.libload("Initializing Libby dependency manager...");
            libbyWrapper = new LibbyWrapper(this);
            
            // Load dependencies asynchronously
            CompletableFuture.runAsync(this::loadDependencies)
                .thenRun(this::initializePlugin)
                .exceptionally(throwable -> {
                    logger.severe("Failed to load dependencies: " + throwable.getMessage());
                    // Continue with degraded functionality
                    initializePluginWithoutDependencies();
                    return null;
                });
        } else {
            logger.startup("Libby disabled, initializing without runtime dependencies");
            initializePluginWithoutDependencies();
        }
    }
    
    /**
     * Load runtime dependencies via Libby
     */
    private void loadDependencies() {
        try {
            logger.libload("Downloading HeadDatabase API...");
            libbyWrapper.loadHeadDatabaseAPI();
            
            logger.libload("Loading additional libraries...");
            libbyWrapper.loadConfiguredLibraries();
            
            dependenciesLoaded = true;
            logger.libload("All dependencies loaded successfully");
            
        } catch (Exception e) {
            logger.severe("Failed to load dependencies via Libby: " + e.getMessage());
            dependenciesLoaded = false;
        }
    }
    
    /**
     * Initialize plugin with full functionality
     */
    private void initializePlugin() {
        // Run on main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    logger.register("Initializing core managers...");
                    
                    // Initialize managers
                    headManager = new HeadManager(Main.this, dependenciesLoaded);
                    abilityManager = new AbilityManager(Main.this);
                    banBoxManager = new BanBoxManager(Main.this);
                    
                    logger.register("Loading head configurations...");
                    headManager.loadHeads();
                    
                    logger.register("Registering abilities...");
                    abilityManager.registerAbilities();
                    
                    logger.register("Registering event listeners...");
                    registerListeners();
                    
                    logger.register("Registering commands...");
                    registerCommands();
                    
                    // Plugin is ready
                    pluginReady.set(true);
                    logger.ready("HeadStealX v" + getDescription().getVersion() + " enabled successfully!");
                    logger.ready("Loaded " + headManager.getLoadedHeadCount() + " mob heads");
                    logger.ready("Registered " + abilityManager.getRegisteredAbilityCount() + " abilities");
                    
                    // Start background tasks
                    startBackgroundTasks();
                    
                    // Check for updates
                    if (getConfig().getBoolean("general.behavior.update_checker", true)) {
                        checkForUpdates();
                    }
                    
                } catch (Exception e) {
                    logger.severe("Failed to initialize plugin: " + e.getMessage());
                    e.printStackTrace();
                    getServer().getPluginManager().disablePlugin(Main.this);
                }
            }
        }.runTask(this);
    }
    
    /**
     * Initialize plugin without HeadDatabase (degraded mode)
     */
    private void initializePluginWithoutDependencies() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    logger.register("Initializing in degraded mode (no HeadDatabase)...");
                    
                    // Initialize managers with limited functionality
                    headManager = new HeadManager(Main.this, false);
                    abilityManager = new AbilityManager(Main.this);
                    banBoxManager = new BanBoxManager(Main.this);
                    
                    // Load basic configurations
                    headManager.loadHeadsWithoutHDB();
                    abilityManager.registerAbilities();
                    
                    // Register listeners and commands
                    registerListeners();
                    registerCommands();
                    
                    pluginReady.set(true);
                    logger.ready("HeadStealX enabled in degraded mode (HeadDatabase not available)");
                    logger.ready("Some features may be limited without HeadDatabase integration");
                    
                    startBackgroundTasks();
                    
                } catch (Exception e) {
                    logger.severe("Failed to initialize plugin in degraded mode: " + e.getMessage());
                    e.printStackTrace();
                    getServer().getPluginManager().disablePlugin(Main.this);
                }
            }
        }.runTask(this);
    }
    
    /**
     * Register event listeners
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChargedCreeperListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerAbilityListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        
        logger.register("Event listeners registered");
    }
    
    /**
     * Register commands
     */
    private void registerCommands() {
        HeadStealXCommand commandHandler = new HeadStealXCommand(this);
        getCommand("xsteal").setExecutor(commandHandler);
        getCommand("xsteal").setTabCompleter(commandHandler);
        
        logger.register("Commands registered");
    }
    
    /**
     * Start background tasks
     */
    private void startBackgroundTasks() {
        // BanBox auto-unban task
        if (getConfig().getInt("banbox.auto_unban_days", 7) > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    banBoxManager.processAutoUnbans();
                }
            }.runTaskTimerAsynchronously(this, 20L * 60L * 60L, 20L * 60L * 60L); // Every hour
        }
        
        // Performance monitoring task
        if (getConfig().getBoolean("general.behavior.debug_mode", false)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    logger.debug("Active abilities: " + abilityManager.getActiveAbilityCount());
                    logger.debug("Banned players: " + banBoxManager.getBannedPlayerCount());
                    logger.debug("Memory usage: " + getMemoryUsage() + "MB");
                }
            }.runTaskTimerAsynchronously(this, 20L * 60L * 5L, 20L * 60L * 5L); // Every 5 minutes
        }
        
        logger.register("Background tasks started");
    }
    
    /**
     * Check for plugin updates
     */
    private void checkForUpdates() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    UpdateChecker checker = new UpdateChecker(Main.this);
                    if (checker.hasUpdate()) {
                        logger.info("A new version of HeadStealX is available: " + checker.getLatestVersion());
                        logger.info("Download: " + checker.getDownloadUrl());
                    }
                } catch (Exception e) {
                    logger.debug("Update check failed: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(this);
    }
    
    @Override
    public void onDisable() {
        logger.info("Disabling HeadStealX...");
        
        // Cancel all tasks
        getServer().getScheduler().cancelTasks(this);
        
        // Save data
        if (banBoxManager != null) {
            banBoxManager.saveData();
        }
        
        if (headManager != null) {
            headManager.cleanup();
        }
        
        if (abilityManager != null) {
            abilityManager.cleanup();
        }
        
        // Clear managers
        headManager = null;
        abilityManager = null;
        banBoxManager = null;
        libbyWrapper = null;
        
        pluginReady.set(false);
        logger.info("HeadStealX disabled successfully");
    }
    
    /**
     * Reload plugin configuration and data
     */
    public void reload() {
        logger.info("Reloading HeadStealX...");
        
        // Reload config
        reloadConfig();
        ConfigUtil.loadConfig(this);
        
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
        
        logger.info("HeadStealX reloaded successfully");
    }
    
    // Getters
    public static Main getInstance() {
        return instance;
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
    
    public LibbyWrapper getLibbyWrapper() {
        return libbyWrapper;
    }
    
    public Logger getPluginLogger() {
        return logger;
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