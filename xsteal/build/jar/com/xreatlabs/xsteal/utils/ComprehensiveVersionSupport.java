package com.xreatlabs.xsteal.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

/**
 * Comprehensive version support for XSteal
 * Handles ALL Minecraft versions from 1.8 to 1.21.8
 * Provides compatibility layers for different version features
 */
public class ComprehensiveVersionSupport {
    
    private static final String SERVER_VERSION = getServerVersionInternal();
    private static final int[] VERSION_NUMBERS = parseVersionNumbers();
    
    // Version-specific entity mappings
    private static final Map<String, String> ENTITY_VERSION_MAPPING = new HashMap<>();
    private static final Map<String, String> MATERIAL_VERSION_MAPPING = new HashMap<>();
    
    static {
        initializeVersionMappings();
    }
    
    /**
     * Initialize version-specific mappings
     */
    private static void initializeVersionMappings() {
        // Entity mappings for different versions
        ENTITY_VERSION_MAPPING.put("zombified_piglin_1.16+", "ZOMBIFIED_PIGLIN");
        ENTITY_VERSION_MAPPING.put("zombified_piglin_1.8-1.15", "PIG_ZOMBIE");
        ENTITY_VERSION_MAPPING.put("glow_squid_1.17+", "GLOW_SQUID");
        ENTITY_VERSION_MAPPING.put("axolotl_1.17+", "AXOLOTL");
        ENTITY_VERSION_MAPPING.put("goat_1.17+", "GOAT");
        ENTITY_VERSION_MAPPING.put("allay_1.19+", "ALLAY");
        ENTITY_VERSION_MAPPING.put("frog_1.19+", "FROG");
        ENTITY_VERSION_MAPPING.put("tadpole_1.19+", "TADPOLE");
        ENTITY_VERSION_MAPPING.put("warden_1.19+", "WARDEN");
        ENTITY_VERSION_MAPPING.put("camel_1.20+", "CAMEL");
        ENTITY_VERSION_MAPPING.put("sniffer_1.20+", "SNIFFER");
        
        // Material mappings for different versions
        MATERIAL_VERSION_MAPPING.put("player_head_1.13+", "PLAYER_HEAD");
        MATERIAL_VERSION_MAPPING.put("player_head_1.8-1.12", "SKULL_ITEM");
        MATERIAL_VERSION_MAPPING.put("spawner_1.13+", "SPAWNER");
        MATERIAL_VERSION_MAPPING.put("spawner_1.8-1.12", "MOB_SPAWNER");
    }
    
    /**
     * Get server version with full support for 1.8-1.21.8
     */
    public static String getServerVersion() {
        return SERVER_VERSION;
    }
    
    /**
     * Check if current version supports specific entity
     */
    public static boolean supportsEntity(String entityName) {
        try {
            EntityType.valueOf(entityName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Get version-appropriate EntityType
     */
    public static EntityType getEntityType(String baseName) {
        // Handle version-specific entity names
        switch (baseName.toLowerCase()) {
            case "zombified_piglin":
                return isAtLeast(1, 16) ? 
                    EntityType.valueOf("ZOMBIFIED_PIGLIN") : 
                    EntityType.valueOf("PIG_ZOMBIE");
            case "glow_squid":
                return isAtLeast(1, 17) ? EntityType.valueOf("GLOW_SQUID") : null;
            case "axolotl":
                return isAtLeast(1, 17) ? EntityType.valueOf("AXOLOTL") : null;
            case "goat":
                return isAtLeast(1, 17) ? EntityType.valueOf("GOAT") : null;
            case "allay":
                return isAtLeast(1, 19) ? EntityType.valueOf("ALLAY") : null;
            case "frog":
                return isAtLeast(1, 19) ? EntityType.valueOf("FROG") : null;
            case "warden":
                return isAtLeast(1, 19) ? EntityType.valueOf("WARDEN") : null;
            case "camel":
                return isAtLeast(1, 20) ? EntityType.valueOf("CAMEL") : null;
            case "sniffer":
                return isAtLeast(1, 20) ? EntityType.valueOf("SNIFFER") : null;
            default:
                try {
                    return EntityType.valueOf(baseName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }
    
    /**
     * Get version-appropriate Material
     */
    public static Material getMaterial(String baseName) {
        switch (baseName.toLowerCase()) {
            case "player_head":
                return isAtLeast(1, 13) ? 
                    Material.valueOf("PLAYER_HEAD") : 
                    Material.valueOf("SKULL_ITEM");
            case "spawner":
                return isAtLeast(1, 13) ? 
                    Material.valueOf("SPAWNER") : 
                    Material.valueOf("MOB_SPAWNER");
            default:
                try {
                    return Material.valueOf(baseName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
        }
    }
    
    /**
     * Check if version is at least the specified version
     */
    public static boolean isAtLeast(int major, int minor) {
        return isAtLeast(major, minor, 0);
    }
    
    /**
     * Check if version is at least the specified version
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
     * Check if version is supported (1.8-1.21.8)
     */
    public static boolean isSupported() {
        if (VERSION_NUMBERS.length < 2) {
            return false;
        }
        
        int major = VERSION_NUMBERS[0];
        int minor = VERSION_NUMBERS[1];
        int patch = VERSION_NUMBERS.length > 2 ? VERSION_NUMBERS[2] : 0;
        
        // Must be version 1.x
        if (major != 1) {
            return false;
        }
        
        // Must be 1.8 or higher
        if (minor < 8) {
            return false;
        }
        
        // Must be 1.21.8 or lower
        if (minor > 21) {
            return false;
        }
        
        // For 1.21.x, check patch version
        if (minor == 21 && patch > 8) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Get all available mobs for current version
     */
    public static Map<String, EntityType> getAvailableMobs() {
        Map<String, EntityType> availableMobs = new HashMap<>();
        
        // Always available mobs (1.8+)
        String[] baseMobs = {
            "zombie", "skeleton", "creeper", "spider", "cave_spider", "enderman",
            "blaze", "witch", "ghast", "slime", "magma_cube", "silverfish",
            "guardian", "cow", "pig", "sheep", "chicken", "rabbit", "horse",
            "donkey", "mule", "wolf", "cat", "ocelot", "bat", "squid",
            "iron_golem", "snow_golem", "villager"
        };
        
        for (String mobName : baseMobs) {
            EntityType entityType = getEntityType(mobName);
            if (entityType != null) {
                availableMobs.put(mobName, entityType);
            }
        }
        
        // Version-specific mobs
        if (isAtLeast(1, 9)) {
            addIfAvailable(availableMobs, "shulker");
        }
        
        if (isAtLeast(1, 10)) {
            addIfAvailable(availableMobs, "polar_bear");
        }
        
        if (isAtLeast(1, 11)) {
            addIfAvailable(availableMobs, "vex");
            addIfAvailable(availableMobs, "evoker");
            addIfAvailable(availableMobs, "vindicator");
            addIfAvailable(availableMobs, "llama");
        }
        
        if (isAtLeast(1, 12)) {
            addIfAvailable(availableMobs, "parrot");
        }
        
        if (isAtLeast(1, 13)) {
            addIfAvailable(availableMobs, "phantom");
            addIfAvailable(availableMobs, "drowned");
            addIfAvailable(availableMobs, "cod");
            addIfAvailable(availableMobs, "salmon");
            addIfAvailable(availableMobs, "pufferfish");
            addIfAvailable(availableMobs, "tropical_fish");
            addIfAvailable(availableMobs, "turtle");
            addIfAvailable(availableMobs, "dolphin");
        }
        
        if (isAtLeast(1, 14)) {
            addIfAvailable(availableMobs, "pillager");
            addIfAvailable(availableMobs, "ravager");
            addIfAvailable(availableMobs, "wandering_trader");
            addIfAvailable(availableMobs, "panda");
            addIfAvailable(availableMobs, "fox");
            addIfAvailable(availableMobs, "bee");
        }
        
        if (isAtLeast(1, 15)) {
            // No new mobs in 1.15
        }
        
        if (isAtLeast(1, 16)) {
            addIfAvailable(availableMobs, "zombified_piglin");
            addIfAvailable(availableMobs, "piglin");
            addIfAvailable(availableMobs, "piglin_brute");
            addIfAvailable(availableMobs, "hoglin");
            addIfAvailable(availableMobs, "zoglin");
            addIfAvailable(availableMobs, "strider");
        }
        
        if (isAtLeast(1, 17)) {
            addIfAvailable(availableMobs, "glow_squid");
            addIfAvailable(availableMobs, "axolotl");
            addIfAvailable(availableMobs, "goat");
        }
        
        if (isAtLeast(1, 19)) {
            addIfAvailable(availableMobs, "allay");
            addIfAvailable(availableMobs, "frog");
            addIfAvailable(availableMobs, "tadpole");
            addIfAvailable(availableMobs, "warden");
        }
        
        if (isAtLeast(1, 20)) {
            addIfAvailable(availableMobs, "camel");
            addIfAvailable(availableMobs, "sniffer");
        }
        
        // Boss mobs (always try to add if available)
        addIfAvailable(availableMobs, "ender_dragon");
        addIfAvailable(availableMobs, "wither");
        
        return availableMobs;
    }
    
    /**
     * Add mob to available list if it exists in current version
     */
    private static void addIfAvailable(Map<String, EntityType> availableMobs, String mobName) {
        EntityType entityType = getEntityType(mobName);
        if (entityType != null) {
            availableMobs.put(mobName, entityType);
        }
    }
    
    /**
     * Check if current version is legacy (1.8-1.12)
     */
    public static boolean isLegacy() {
        return VERSION_NUMBERS.length >= 2 && 
               VERSION_NUMBERS[0] == 1 && 
               VERSION_NUMBERS[1] <= 12;
    }
    
    /**
     * Check if current version is modern (1.13+)
     */
    public static boolean isModern() {
        return !isLegacy();
    }
    
    /**
     * Internal method to get server version
     */
    private static String getServerVersionInternal() {
        try {
            return Bukkit.getBukkitVersion().split("-")[0];
        } catch (Exception e) {
            // Fallback to package name parsing for older versions
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String version = packageName.substring(packageName.lastIndexOf('.') + 1);
            
            // Convert NMS version to Minecraft version
            switch (version) {
                case "v1_8_R1": case "v1_8_R2": case "v1_8_R3": return "1.8";
                case "v1_9_R1": case "v1_9_R2": return "1.9";
                case "v1_10_R1": return "1.10";
                case "v1_11_R1": return "1.11";
                case "v1_12_R1": return "1.12";
                case "v1_13_R1": case "v1_13_R2": return "1.13";
                case "v1_14_R1": return "1.14";
                case "v1_15_R1": return "1.15";
                case "v1_16_R1": case "v1_16_R2": case "v1_16_R3": return "1.16";
                case "v1_17_R1": return "1.17";
                case "v1_18_R1": case "v1_18_R2": return "1.18";
                case "v1_19_R1": case "v1_19_R2": case "v1_19_R3": return "1.19";
                case "v1_20_R1": case "v1_20_R2": case "v1_20_R3": case "v1_20_R4": return "1.20";
                case "v1_21_R1": case "v1_21_R2": case "v1_21_R3": case "v1_21_R4": return "1.21";
                default: return "unknown";
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
                return new int[]{1, 8, 0};
            }
            
            String[] parts = version.split("\\.");
            int[] numbers = new int[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].replaceAll("[^0-9]", "");
                numbers[i] = part.isEmpty() ? 0 : Integer.parseInt(part);
            }
            
            return numbers;
        } catch (Exception e) {
            return new int[]{1, 8, 0};
        }
    }
    
    /**
     * Get comprehensive compatibility information
     */
    public static String getFullCompatibilityInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Minecraft: ").append(SERVER_VERSION);
        info.append(", Paper: ").append(isPaper() ? "Yes" : "No");
        info.append(", Spigot: ").append(isSpigot() ? "Yes" : "No");
        info.append(", Legacy: ").append(isLegacy() ? "Yes" : "No");
        info.append(", Supported: ").append(isSupported() ? "Yes" : "No");
        
        Map<String, EntityType> availableMobs = getAvailableMobs();
        info.append(", Available Mobs: ").append(availableMobs.size());
        
        return info.toString();
    }
    
    /**
     * Check if server is Paper
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
     * Check if server is Spigot
     */
    public static boolean isSpigot() {
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Check if server supports specific features
     */
    public static boolean supportsFeature(String feature) {
        switch (feature.toLowerCase()) {
            case "persistent_data_container":
                return isAtLeast(1, 14);
            case "hex_colors":
                return isAtLeast(1, 16);
            case "adventure_components":
                return isPaper() && isAtLeast(1, 16);
            case "new_materials":
                return isAtLeast(1, 13);
            case "boss_bar_api":
                return isAtLeast(1, 9);
            case "action_bar":
                return isAtLeast(1, 11);
            case "title_api":
                return isAtLeast(1, 8);
            case "particle_api":
                return isAtLeast(1, 9);
            case "sound_api":
                return isAtLeast(1, 8);
            default:
                return false;
        }
    }
    
    /**
     * Get version-specific implementation class name
     */
    public static String getVersionImplementation(String baseClass) {
        if (isLegacy()) {
            return baseClass + "_Legacy";
        } else {
            return baseClass + "_Modern";
        }
    }
    
    /**
     * Get maximum supported entities for current version
     */
    public static int getMaxSupportedEntities() {
        if (isAtLeast(1, 20)) {
            return 58; // All mobs including latest
        } else if (isAtLeast(1, 19)) {
            return 55; // Without 1.20+ mobs
        } else if (isAtLeast(1, 17)) {
            return 50; // Without 1.19+ mobs
        } else if (isAtLeast(1, 16)) {
            return 45; // Without 1.17+ mobs
        } else if (isAtLeast(1, 14)) {
            return 40; // Without 1.16+ mobs
        } else if (isAtLeast(1, 13)) {
            return 35; // Without 1.14+ mobs
        } else {
            return 30; // Legacy versions
        }
    }
}