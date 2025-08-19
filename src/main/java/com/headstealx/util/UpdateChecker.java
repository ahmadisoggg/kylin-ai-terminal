package com.headstealx.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Update checker utility for HeadStealX
 * Checks for plugin updates from a remote source
 */
public class UpdateChecker {
    
    private final JavaPlugin plugin;
    private final String currentVersion;
    private String latestVersion;
    private String downloadUrl;
    
    // Update check URL (would be replaced with actual API endpoint)
    private static final String UPDATE_URL = "https://api.headstealx.com/version";
    private static final String DOWNLOAD_URL = "https://headstealx.com/download";
    
    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }
    
    /**
     * Check for updates asynchronously
     */
    public CompletableFuture<Boolean> checkForUpdatesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return checkForUpdates();
            } catch (Exception e) {
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * Check for updates synchronously
     */
    public boolean checkForUpdates() throws IOException {
        try {
            // Fetch latest version from API
            latestVersion = fetchLatestVersion();
            
            if (latestVersion == null || latestVersion.trim().isEmpty()) {
                return false;
            }
            
            // Compare versions
            return isNewerVersion(latestVersion, currentVersion);
            
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            plugin.getLogger().warning("Update check error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Fetch the latest version from the remote API
     */
    private String fetchLatestVersion() throws IOException {
        HttpURLConnection connection = null;
        
        try {
            URL url = new URL(UPDATE_URL);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set request properties
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "HeadStealX/" + currentVersion);
            connection.setRequestProperty("Plugin-Version", currentVersion);
            
            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode + ": " + connection.getResponseMessage());
            }
            
            // Read response
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                // Parse JSON response (simple implementation)
                return parseVersionFromResponse(response.toString());
            }
            
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Parse version from JSON response
     * This is a simple implementation - in production, you'd use a JSON library
     */
    private String parseVersionFromResponse(String response) {
        try {
            // Simple JSON parsing for {"version": "1.0.1", "download": "url"}
            if (response.contains("\"version\"")) {
                int versionStart = response.indexOf("\"version\"") + 10;
                int versionValueStart = response.indexOf("\"", versionStart) + 1;
                int versionEnd = response.indexOf("\"", versionValueStart);
                
                if (versionEnd > versionValueStart) {
                    String version = response.substring(versionValueStart, versionEnd);
                    
                    // Also extract download URL if present
                    if (response.contains("\"download\"")) {
                        int downloadStart = response.indexOf("\"download\"") + 11;
                        int downloadValueStart = response.indexOf("\"", downloadStart) + 1;
                        int downloadEnd = response.indexOf("\"", downloadValueStart);
                        
                        if (downloadEnd > downloadValueStart) {
                            downloadUrl = response.substring(downloadValueStart, downloadEnd);
                        }
                    }
                    
                    return version;
                }
            }
            
            // Fallback: treat entire response as version string
            return response.trim();
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse version response: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Compare two version strings
     * Returns true if newVersion is newer than currentVersion
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        try {
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            
            int maxLength = Math.max(newParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int newPart = i < newParts.length ? parseVersionPart(newParts[i]) : 0;
                int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                
                if (newPart > currentPart) {
                    return true;
                } else if (newPart < currentPart) {
                    return false;
                }
            }
            
            return false; // Versions are equal
            
        } catch (Exception e) {
            plugin.getLogger().warning("Version comparison failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Parse a version part, handling non-numeric suffixes
     */
    private int parseVersionPart(String part) {
        try {
            // Remove non-numeric characters (like -SNAPSHOT, -BETA, etc.)
            String numericPart = part.replaceAll("[^0-9]", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Check if an update is available
     */
    public boolean hasUpdate() {
        if (latestVersion == null) {
            try {
                checkForUpdates();
            } catch (IOException e) {
                return false;
            }
        }
        
        return latestVersion != null && isNewerVersion(latestVersion, currentVersion);
    }
    
    /**
     * Get the latest version string
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * Get the current version string
     */
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    /**
     * Get the download URL for the latest version
     */
    public String getDownloadUrl() {
        return downloadUrl != null ? downloadUrl : DOWNLOAD_URL;
    }
    
    /**
     * Get update information as a formatted string
     */
    public String getUpdateInfo() {
        if (!hasUpdate()) {
            return "No updates available. Current version: " + currentVersion;
        }
        
        return String.format("Update available! Current: %s, Latest: %s\nDownload: %s",
            currentVersion, latestVersion, getDownloadUrl());
    }
    
    /**
     * Perform a lightweight version check (for development/testing)
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        
        // Basic version format validation
        return version.matches("^\\d+(\\.\\d+)*(-[A-Za-z0-9]+)?$");
    }
}