package com.xreatlabs.xsteal.utils;

import org.bukkit.Bukkit;

/**
 * Version compatibility utility for XSteal
 * Handles version detection and compatibility checks for Minecraft 1.8-1.21.4
 */
public class VersionCompatibility {
    
    private static final String SERVER_VERSION = getServerVersionInternal();
    private static final int[] VERSION_NUMBERS = parseVersionNumbers();
    
    // Supported version ranges
    private static final int MIN_MAJOR = 1;
    private static final int MIN_MINOR = 8;
    private static final int MAX_MAJOR = 1;
    private static final int MAX_MINOR = 21;
    private static final int MAX_PATCH = 4;
    
    /**
     * Get the server version string
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }
    
    /**
     * Get parsed version numbers [major, minor, patch]
     */
    public static int[] getVersionNumbers() {
        return VERSION_NUMBERS.clone();
    }
    
    /**
     * Check if the current server version is supported
     */
    public static boolean isSupported() {
        if (VERSION_NUMBERS.length < 2) {
            return false;
        }
        
        int major = VERSION_NUMBERS[0];
        int minor = VERSION_NUMBERS[1];
        int patch = VERSION_NUMBERS.length > 2 ? VERSION_NUMBERS[2] : 0;
        
        // Check major version
        if (major < MIN_MAJOR || major > MAX_MAJOR) {
            return false;
        }
        
        // Check minor version
        if (minor < MIN_MINOR || minor > MAX_MINOR) {
            return false;
        }
        
        // For version 1.21, check patch version
        if (major == MAX_MAJOR && minor == MAX_MINOR && patch > MAX_PATCH) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if the server is running a legacy version (1.8-1.12)
     */
    public static boolean isLegacy() {
        return VERSION_NUMBERS.length >= 2 && 
               VERSION_NUMBERS[0] == 1 && 
               VERSION_NUMBERS[1] <= 12;
    }
    
    /**
     * Check if the server supports modern features (1.13+)
     */
    public static boolean isModern() {
        return !isLegacy();
    }
    
    /**
     * Check if the server supports Paper API
     */
    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("io.papermc.paper.configuration.Configuration");
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }
    
    /**
     * Check if the server supports Folia
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Check if a specific version is at least the given version
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }
    
    /**
     * Check if a specific version is at least the given version
     */
    public static boolean isAtLeast(int major, int minor, int patch) {
        if (VERSION_NUMBERS.length < 2) {
            return false;
        }
        
        int currentMajor = VERSION_NUMBERS[0];
        int currentMinor = VERSION_NUMBERS[1];
        int currentPatch = VERSION_NUMBERS.length > 2 ? VERSION_NUMBERS[2] : 0;
        
        if (currentMajor > major) return true;
        if (currentMajor < major) return false;
        
        if (currentMinor > minor) return true;
        if (currentMinor < minor) return false;
        
        return currentPatch >= patch;
    }
    
    /**
     * Check if server supports hex colors (1.16+)
     */
    public static boolean supportsHexColors() {
        return isAtLeast(1, 16);
    }
    
    /**
     * Check if server supports persistent data containers (1.14+)
     */
    public static boolean supportsPDC() {
        return isAtLeast(1, 14);
    }
    
    /**
     * Check if server supports the new material system (1.13+)
     */
    public static boolean supportsNewMaterials() {
        return isAtLeast(1, 13);
    }
    
    /**
     * Internal method to get server version
     */
    private static String getServerVersionInternal() {
        try {
            return Bukkit.getBukkitVersion().split("-")[0];
        } catch (Exception e) {
            // Fallback to package name parsing
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.substring(packageName.lastIndexOf('.') + 1);
            
            // Convert from NMS version to Minecraft version
            switch (version) {
                case "v1_8_R1":
                case "v1_8_R2":
                case "v1_8_R3":
                    return "1.8";
                case "v1_9_R1":
                case "v1_9_R2":
                    return "1.9";
                case "v1_10_R1":
                    return "1.10";
                case "v1_11_R1":
                    return "1.11";
                case "v1_12_R1":
                    return "1.12";
                case "v1_13_R1":
                case "v1_13_R2":
                    return "1.13";
                case "v1_14_R1":
                    return "1.14";
                case "v1_15_R1":
                    return "1.15";
                case "v1_16_R1":
                case "v1_16_R2":
                case "v1_16_R3":
                    return "1.16";
                case "v1_17_R1":
                    return "1.17";
                case "v1_18_R1":
                case "v1_18_R2":
                    return "1.18";
                case "v1_19_R1":
                case "v1_19_R2":
                case "v1_19_R3":
                    return "1.19";
                case "v1_20_R1":
                case "v1_20_R2":
                case "v1_20_R3":
                    return "1.20";
                case "v1_21_R1":
                case "v1_21_R2":
                case "v1_21_R3":
                    return "1.21";
                default:
                    return "unknown";
            }
        }
    }
    
    /**
     * Parse version string into numbers
     */
    private static int[] parseVersionNumbers() {
        try {
            String version = SERVER_VERSION;
            if (version.equals("unknown")) {
                return new int[]{1, 8, 0}; // Default fallback
            }
            
            String[] parts = version.split("\\.");
            int[] numbers = new int[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                // Remove any non-numeric characters
                String part = parts[i].replaceAll("[^0-9]", "");
                numbers[i] = part.isEmpty() ? 0 : Integer.parseInt(part);
            }
            
            return numbers;
        } catch (Exception e) {
            return new int[]{1, 8, 0}; // Default fallback
        }
    }
    
    /**
     * Get a human-readable version compatibility string
     */
    public static String getCompatibilityInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Server: ").append(SERVER_VERSION);
        info.append(", Paper: ").append(isPaper() ? "Yes" : "No");
        info.append(", Folia: ").append(isFolia() ? "Yes" : "No");
        info.append(", Legacy: ").append(isLegacy() ? "Yes" : "No");
        return info.toString();
    }
}