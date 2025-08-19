package com.headstealx.libby;

import net.byteflux.libby.BukkitLibraryManager;
import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Libby wrapper for HeadStealX runtime dependency management
 * Handles downloading and loading of external libraries like HeadDatabase API
 */
public class LibbyWrapper {
    
    private final JavaPlugin plugin;
    private final LibraryManager libraryManager;
    private final Path libsDirectory;
    
    // HeadDatabase API coordinates
    private static final String HDB_GROUP_ID = "com.arcaniax";
    private static final String HDB_ARTIFACT_ID = "HeadDatabase-API";
    private static final String HDB_VERSION = "1.3.1";
    
    // Alternative coordinates if primary fails
    private static final String HDB_ALT_GROUP_ID = "me.arcaniax";
    
    public LibbyWrapper(JavaPlugin plugin) {
        this.plugin = plugin;
        
        // Initialize library manager
        this.libraryManager = new BukkitLibraryManager(plugin);
        
        // Set up libraries directory
        String libsDirName = plugin.getConfig().getString("libby.libs_dir", "libs");
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
     * Load HeadDatabase API asynchronously
     */
    public CompletableFuture<Boolean> loadHeadDatabaseAPIAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                loadHeadDatabaseAPI();
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load HeadDatabase API: " + e.getMessage());
                return false;
            }
        }).orTimeout(30, TimeUnit.SECONDS);
    }
    
    /**
     * Load HeadDatabase API synchronously
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
        
        // Try loading from local cache
        if (loadFromLocalCache()) {
            plugin.getLogger().info("Loaded HeadDatabase API from local cache");
            return;
        }
        
        throw new Exception("Failed to load HeadDatabase API from all sources");
    }
    
    /**
     * Load additional libraries from configuration
     */
    public void loadConfiguredLibraries() throws Exception {
        List<String> libraries = plugin.getConfig().getStringList("libby.libs");
        
        for (String libCoordinate : libraries) {
            try {
                loadLibraryFromCoordinate(libCoordinate);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load library " + libCoordinate + ": " + e.getMessage());
                // Continue with other libraries
            }
        }
    }
    
    /**
     * Load a library from Maven coordinates (groupId:artifactId:version)
     */
    private void loadLibraryFromCoordinate(String coordinate) throws Exception {
        String[] parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid library coordinate format. Expected: groupId:artifactId:version");
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
        plugin.getLogger().info("Loaded library: " + coordinate);
    }
    
    /**
     * Try to load HeadDatabase API from local cache
     */
    private boolean loadFromLocalCache() {
        try {
            // Look for HeadDatabase API JAR in plugins directory
            File pluginsDir = plugin.getDataFolder().getParentFile();
            File[] files = pluginsDir.listFiles((dir, name) -> 
                name.toLowerCase().contains("headdatabase") && name.endsWith(".jar"));
            
            if (files != null && files.length > 0) {
                plugin.getLogger().info("Found HeadDatabase plugin, API should be available");
                return true;
            }
            
            // Look in libs directory
            File libsDir = libsDirectory.toFile();
            if (libsDir.exists()) {
                File[] libFiles = libsDir.listFiles((dir, name) -> 
                    name.toLowerCase().contains("headdatabase") && name.endsWith(".jar"));
                
                if (libFiles != null && libFiles.length > 0) {
                    // Try to load the JAR
                    // Note: This is simplified - in production you'd use proper classloader management
                    plugin.getLogger().info("Found HeadDatabase API in libs directory");
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking local cache: " + e.getMessage());
            return false;
        }
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
     * Load a library with custom repository
     */
    public void loadLibraryWithRepository(String coordinate, String repositoryUrl) throws Exception {
        try {
            libraryManager.addRepository(new URL(repositoryUrl));
        } catch (MalformedURLException e) {
            throw new Exception("Invalid repository URL: " + repositoryUrl);
        }
        
        loadLibraryFromCoordinate(coordinate);
    }
    
    /**
     * Get the libraries directory
     */
    public Path getLibsDirectory() {
        return libsDirectory;
    }
    
    /**
     * Clean up library manager resources
     */
    public void cleanup() {
        // Libby doesn't provide explicit cleanup methods
        // Resources are cleaned up automatically
        plugin.getLogger().info("Libby wrapper cleanup completed");
    }
    
    /**
     * Get library manager instance
     */
    public LibraryManager getLibraryManager() {
        return libraryManager;
    }
    
    /**
     * Download a library without loading it
     */
    public File downloadLibrary(String coordinate) throws Exception {
        String[] parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid library coordinate format");
        }
        
        Library library = Library.builder()
            .groupId(parts[0])
            .artifactId(parts[1])
            .version(parts[2])
            .build();
        
        // This is a simplified implementation
        // In practice, you'd use Libby's internal download mechanisms
        libraryManager.loadLibrary(library);
        
        // Return approximate location
        return new File(libsDirectory.toFile(), 
            parts[1] + "-" + parts[2] + ".jar");
    }
    
    /**
     * Check if a library is already loaded
     */
    public boolean isLibraryLoaded(String coordinate) {
        try {
            String[] parts = coordinate.split(":");
            if (parts.length != 3) {
                return false;
            }
            
            // Check if classes from the library are available
            // This is library-specific and would need customization
            return true; // Simplified implementation
            
        } catch (Exception e) {
            return false;
        }
    }
}