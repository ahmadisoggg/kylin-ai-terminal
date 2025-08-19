package com.xreatlabs.xsteal.utils;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * Libby wrapper for XSteal runtime dependency management
 * Handles downloading and loading of external libraries like HeadDatabase API
 */
public class LibbyManager {
    
    private final JavaPlugin plugin;
    private final LibraryManager libraryManager;
    private final Path libsDirectory;
    
    // HeadDatabase API coordinates
    private static final String HDB_GROUP_ID = "com.arcaniax";
    private static final String HDB_ARTIFACT_ID = "HeadDatabase-API";
    private static final String HDB_VERSION = "1.3.1";
    
    // Alternative coordinates if primary fails
    private static final String HDB_ALT_GROUP_ID = "me.arcaniax";
    
    public LibbyManager(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize library manager
        this.libraryManager = new BukkitLibraryManager(plugin);
        
        // Set up libraries directory
        String libsDirName = plugin.getConfig().getString("libby.libs_directory", "libs");
        this.libsDirectory = plugin.getDataFolder().toPath().resolve(libsDirName);
        
        // Add configured repositories
        addRepositories();
    }
    
    /**
     * Add repositories from configuration
     */
    private void addRepositories() {
        List<String> repositories = plugin.getConfig().getStringList("libby.repositories");
        
        // Add default repositories if none configured
        if (repositories.isEmpty()) {
            repositories.add("https://repo.papermc.io/repository/maven-public/");
            repositories.add("https://jitpack.io");
            repositories.add("https://repo.codemc.org/repository/maven-public/");
        }
        
        for (String repoUrl : repositories) {
            try {
                libraryManager.addRepository(new URL(repoUrl));
                plugin.getLogger().info("Added repository: " + repoUrl);
            } catch (MalformedURLException e) {
                plugin.getLogger().warning("Invalid repository URL: " + repoUrl);
            }
        }
    }
    
    /**
     * Load HeadDatabase API
     */
    public void loadHeadDatabaseAPI() throws Exception {
        plugin.getLogger().info("Loading HeadDatabase API...");
        
        // Try primary coordinates first
        try {
            Library hdbLibrary = Library.builder()
                .groupId(HDB_GROUP_ID)
                .artifactId(HDB_ARTIFACT_ID)
                .version(HDB_VERSION)
                .build();
            
            libraryManager.loadLibrary(hdbLibrary);
            plugin.getLogger().info("Successfully loaded HeadDatabase API v" + HDB_VERSION);
            return;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Primary HeadDatabase API coordinates failed: " + e.getMessage());
        }
        
        // Try alternative coordinates
        try {
            Library hdbLibraryAlt = Library.builder()
                .groupId(HDB_ALT_GROUP_ID)
                .artifactId(HDB_ARTIFACT_ID)
                .version(HDB_VERSION)
                .build();
            
            libraryManager.loadLibrary(hdbLibraryAlt);
            plugin.getLogger().info("Successfully loaded HeadDatabase API v" + HDB_VERSION + " (alternative coordinates)");
            return;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Alternative HeadDatabase API coordinates failed: " + e.getMessage());
        }
        
        throw new Exception("Failed to load HeadDatabase API from all sources");
    }
    
    /**
     * Load additional dependencies from configuration
     */
    public void loadConfiguredDependencies() throws Exception {
        List<String> dependencies = plugin.getConfig().getStringList("libby.dependencies");
        
        for (String dependency : dependencies) {
            try {
                loadDependencyFromCoordinate(dependency);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load dependency " + dependency + ": " + e.getMessage());
                // Continue with other dependencies
            }
        }
    }
    
    /**
     * Load a dependency from Maven coordinates (groupId:artifactId:version)
     */
    private void loadDependencyFromCoordinate(String coordinate) throws Exception {
        String[] parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid dependency coordinate format. Expected: groupId:artifactId:version");
        }
        
        String groupId = parts[0];
        String artifactId = parts[1];
        String version = parts[2];
        
        Library library = Library.builder()
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
            .build();
        
        libraryManager.loadLibrary(library);
        plugin.getLogger().info("Loaded dependency: " + coordinate);
    }
    
    /**
     * Check if HeadDatabase API is available
     */
    public boolean isHeadDatabaseAPIAvailable() {
        try {
            Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.arcaniax.hdb.api.HeadDatabaseAPI");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }
    
    /**
     * Get HeadDatabase API instance if available
     */
    public Object getHeadDatabaseAPI() {
        if (!isHeadDatabaseAPIAvailable()) {
            return null;
        }
        
        try {
            // Try new package structure first
            Class<?> apiClass = Class.forName("me.arcaniax.hdb.api.HeadDatabaseAPI");
            return apiClass.getMethod("getInstance").invoke(null);
        } catch (Exception e) {
            try {
                // Try old package structure
                Class<?> apiClass = Class.forName("com.arcaniax.hdb.api.HeadDatabaseAPI");
                return apiClass.getMethod("getInstance").invoke(null);
            } catch (Exception ex) {
                plugin.getLogger().warning("Failed to get HeadDatabase API instance: " + ex.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Get the libraries directory
     */
    public Path getLibsDirectory() {
        return libsDirectory;
    }
    
    /**
     * Get library manager instance
     */
    public LibraryManager getLibraryManager() {
        return libraryManager;
    }
}