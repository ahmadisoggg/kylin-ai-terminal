package com.xreatlabs.xsteal.heads;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.utils.VersionCompatibility;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HeadManager for XSteal
 * Manages mob head creation, acquisition, and HeadDatabase integration
 */
public class HeadManager {
    
    private final XSteal plugin;
    private final boolean headDatabaseAvailable;
    private final Map<String, HeadData> loadedHeads;
    private final Map<EntityType, String> entityToHeadMap;
    private final Map<String, EntityType> headToEntityMap;
    
    // HeadDatabase API instance
    private Object headDatabaseAPI;
    
    // Head identification metadata
    private static final String HEAD_ID_KEY = "xsteal_head_id";
    private static final String PLAYER_HEAD_KEY = "xsteal_player_head";
    
    public HeadManager(XSteal plugin, boolean headDatabaseAvailable) {
        this.plugin = plugin;
        this.headDatabaseAvailable = headDatabaseAvailable;
        this.loadedHeads = new ConcurrentHashMap<>();
        this.entityToHeadMap = new ConcurrentHashMap<>();
        this.headToEntityMap = new ConcurrentHashMap<>();
        
        if (headDatabaseAvailable) {
            initializeHeadDatabaseAPI();
        }
    }
    
    /**
     * Initialize HeadDatabase API integration
     */
    private void initializeHeadDatabaseAPI() {
        try {
            Object api = plugin.getLibbyManager().getHeadDatabaseAPI();
            if (api != null) {
                this.headDatabaseAPI = api;
                plugin.getPluginLogger().info("HeadDatabase API integration enabled");
            } else {
                plugin.getPluginLogger().warning("HeadDatabase API not available");
            }
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Failed to initialize HeadDatabase API: " + e.getMessage());
        }
    }
    
    /**
     * Load all heads from configuration
     */
    public void loadHeads() {
        FileConfiguration headsConfig = plugin.getConfigManager().getHeadsConfig();
        if (headsConfig == null) {
            plugin.getPluginLogger().severe("heads.yml not loaded!");
            return;
        }
        
        ConfigurationSection headsSection = headsConfig.getConfigurationSection("heads");
        if (headsSection == null) {
            plugin.getPluginLogger().severe("No 'heads' section found in heads.yml!");
            return;
        }
        
        loadedHeads.clear();
        entityToHeadMap.clear();
        headToEntityMap.clear();
        
        int loaded = 0;
        int failed = 0;
        
        for (String headKey : headsSection.getKeys(false)) {
            try {
                HeadData headData = loadHeadData(headKey, headsSection.getConfigurationSection(headKey));
                if (headData != null) {
                    loadedHeads.put(headKey, headData);
                    
                    // Map entity type to head
                    EntityType entityType = getEntityTypeFromKey(headKey);
                    if (entityType != null) {
                        entityToHeadMap.put(entityType, headKey);
                        headToEntityMap.put(headKey, entityType);
                    }
                    
                    loaded++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Failed to load head '" + headKey + "': " + e.getMessage());
                failed++;
            }
        }
        
        plugin.getPluginLogger().info("Loaded " + loaded + " heads, " + failed + " failed");
        
        // Resolve HeadDatabase IDs
        if (headDatabaseAvailable && headDatabaseAPI != null) {
            resolveHeadDatabaseIDs();
        }
    }
    
    /**
     * Load heads without HeadDatabase (fallback mode)
     */
    public void loadHeadsWithoutHDB() {
        plugin.getPluginLogger().info("Loading heads in fallback mode (no HeadDatabase)");
        loadHeads(); // Still load configuration, just without HDB integration
    }
    
    /**
     * Load head data from configuration section
     */
    private HeadData loadHeadData(String key, ConfigurationSection section) {
        if (section == null) return null;
        
        String displayName = section.getString("display_name");
        String hdbId = section.getString("hdb_id");
        String category = section.getString("category", "unknown");
        String description = section.getString("description", "");
        
        if (displayName == null || hdbId == null) {
            plugin.getPluginLogger().warning("Head '" + key + "' missing required fields");
            return null;
        }
        
        HeadData headData = new HeadData(key, displayName, hdbId, category, description);
        
        // Load regular ability
        if (section.contains("ability")) {
            ConfigurationSection abilitySection = section.getConfigurationSection("ability");
            if (abilitySection != null) {
                AbilityData ability = loadAbilityData(abilitySection);
                headData.setAbility(ability);
            }
        }
        
        // Load boss abilities (multiple abilities for boss heads)
        if (section.contains("abilities")) {
            ConfigurationSection abilitiesSection = section.getConfigurationSection("abilities");
            if (abilitiesSection != null) {
                for (String abilityKey : abilitiesSection.getKeys(false)) {
                    ConfigurationSection bossAbilitySection = abilitiesSection.getConfigurationSection(abilityKey);
                    if (bossAbilitySection != null) {
                        BossAbilityData bossAbility = loadBossAbilityData(abilityKey, bossAbilitySection);
                        if (bossAbility != null) {
                            headData.addBossAbility(bossAbility);
                        }
                    }
                }
            }
        }
        
        return headData;
    }
    
    /**
     * Load ability data from configuration
     */
    private AbilityData loadAbilityData(ConfigurationSection section) {
        String type = section.getString("type");
        String activation = section.getString("activation", "left_click");
        
        if (type == null) return null;
        
        Map<String, Object> params = new HashMap<>();
        if (section.contains("params")) {
            ConfigurationSection paramsSection = section.getConfigurationSection("params");
            if (paramsSection != null) {
                for (String key : paramsSection.getKeys(false)) {
                    params.put(key, paramsSection.get(key));
                }
            }
        }
        
        return new AbilityData(type, activation, params);
    }
    
    /**
     * Load boss ability data from configuration section
     */
    private BossAbilityData loadBossAbilityData(String abilityKey, ConfigurationSection section) {
        String name = section.getString("name", abilityKey);
        String activation = section.getString("activation");
        String type = section.getString("type");
        
        if (activation == null || type == null) return null;
        
        Map<String, Object> params = new HashMap<>();
        if (section.contains("params")) {
            ConfigurationSection paramsSection = section.getConfigurationSection("params");
            if (paramsSection != null) {
                for (String key : paramsSection.getKeys(false)) {
                    params.put(key, paramsSection.get(key));
                }
            }
        }
        
        return new BossAbilityData(name, activation, type, params);
    }
    
    /**
     * Resolve HeadDatabase IDs to actual head textures
     */
    private void resolveHeadDatabaseIDs() {
        if (headDatabaseAPI == null) return;
        
        plugin.getPluginLogger().info("Resolving HeadDatabase IDs...");
        
        int resolved = 0;
        int failed = 0;
        
        for (HeadData headData : loadedHeads.values()) {
            try {
                if (resolveHeadDatabaseID(headData)) {
                    resolved++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                plugin.getPluginLogger().warning("Failed to resolve HDB ID for " + headData.getKey() + ": " + e.getMessage());
                failed++;
            }
        }
        
        plugin.getPluginLogger().info("Resolved " + resolved + " HeadDatabase IDs, " + failed + " failed");
    }
    
    /**
     * Resolve a single HeadDatabase ID
     */
    private boolean resolveHeadDatabaseID(HeadData headData) {
        try {
            String hdbId = headData.getHdbId();
            
            // Check if it's a placeholder
            if (hdbId.startsWith("HDB_")) {
                plugin.getPluginLogger().debug("Skipping placeholder HDB ID: " + hdbId);
                return false;
            }
            
            // Use reflection to call HeadDatabase API
            Method getItemHeadMethod = headDatabaseAPI.getClass().getMethod("getItemHead", String.class);
            ItemStack headItem = (ItemStack) getItemHeadMethod.invoke(headDatabaseAPI, hdbId);
            
            if (headItem != null) {
                headData.setResolvedTexture(getHeadTexture(headItem));
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            plugin.getPluginLogger().debug("Failed to resolve HDB ID " + headData.getHdbId() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get head texture from ItemStack (simplified implementation)
     */
    private String getHeadTexture(ItemStack headItem) {
        if (headItem == null || !isPlayerHead(headItem)) {
            return null;
        }
        
        // This would be a complex reflection-based implementation
        // For now, we'll return a placeholder
        return "texture_data_placeholder";
    }
    
    /**
     * Create a head ItemStack for the given entity
     */
    public ItemStack getHeadForEntity(Entity entity) {
        String headKey = entityToHeadMap.get(entity.getType());
        if (headKey == null) return null;
        
        return createHeadItem(headKey);
    }
    
    /**
     * Create a head ItemStack by key
     */
    public ItemStack createHeadItem(String headKey) {
        HeadData headData = loadedHeads.get(headKey);
        if (headData == null) return null;
        
        ItemStack head = createBaseHeadItem();
        if (head == null) return null;
        
        // Set display name and lore
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + headData.getDescription());
            lore.add("");
            lore.add(ChatColor.YELLOW + "Category: " + ChatColor.WHITE + headData.getCategory());
            
            if (headData.hasAbility()) {
                lore.add(ChatColor.GREEN + "Ability: " + ChatColor.WHITE + headData.getAbility().getType());
                lore.add(ChatColor.GRAY + "Activation: " + headData.getAbility().getActivation());
            }
            
            if (headData.hasBossAbilities()) {
                lore.add(ChatColor.GOLD + "Boss Head - Multiple Abilities:");
                for (BossAbilityData ability : headData.getBossAbilities()) {
                    lore.add(ChatColor.GOLD + "  " + ability.getActivation() + ": " + ability.getName());
                }
            }
            
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "XSteal Head");
            
            meta.setLore(lore);
            
            // Add identification metadata
            if (VersionCompatibility.supportsPDC()) {
                // Would use PersistentDataContainer here
                addLegacyMetadata(meta, headKey);
            } else {
                addLegacyMetadata(meta, headKey);
            }
            
            head.setItemMeta(meta);
        }
        
        // Apply texture if available from HeadDatabase
        if (headDatabaseAvailable && headData.getResolvedTexture() != null) {
            applyHeadTexture(head, headData.getResolvedTexture());
        }
        
        return head;
    }
    
    /**
     * Create base head ItemStack
     */
    private ItemStack createBaseHeadItem() {
        if (VersionCompatibility.supportsNewMaterials()) {
            return new ItemStack(Material.PLAYER_HEAD);
        } else {
            // Legacy version
            try {
                Material skullMaterial = Material.valueOf("SKULL_ITEM");
                ItemStack skull = new ItemStack(skullMaterial, 1, (short) 3); // Player skull
                return skull;
            } catch (Exception e) {
                plugin.getPluginLogger().severe("Failed to create skull item for legacy version");
                return null;
            }
        }
    }
    
    /**
     * Check if ItemStack is a player head
     */
    private boolean isPlayerHead(ItemStack item) {
        if (item == null) return false;
        
        if (VersionCompatibility.supportsNewMaterials()) {
            return item.getType() == Material.PLAYER_HEAD;
        } else {
            return item.getType().name().equals("SKULL_ITEM") && item.getDurability() == 3;
        }
    }
    
    /**
     * Add legacy metadata for head identification
     */
    private void addLegacyMetadata(ItemMeta meta, String headKey) {
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        
        // Add hidden identifier in lore
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + HEAD_ID_KEY + ":" + headKey);
        meta.setLore(lore);
    }
    
    /**
     * Apply head texture (simplified implementation)
     */
    private void applyHeadTexture(ItemStack head, String texture) {
        // This would be a complex reflection-based implementation
        // to set the skull texture using HeadDatabase data
        if (head.getItemMeta() instanceof SkullMeta) {
            // Texture application would go here
        }
    }
    
    /**
     * Get head key from ItemStack
     */
    public String getHeadKey(ItemStack item) {
        if (!isPlayerHead(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        // Extract from lore (legacy method)
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                String stripped = ChatColor.stripColor(line);
                if (stripped.startsWith(HEAD_ID_KEY + ":")) {
                    return stripped.substring((HEAD_ID_KEY + ":").length());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Create player head for BanBox system
     */
    public ItemStack createPlayerHead(Player player) {
        ItemStack head = createBaseHeadItem();
        if (head == null) return null;
        
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + player.getName() + "'s Head");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Player: " + ChatColor.WHITE + player.getName());
            lore.add(ChatColor.YELLOW + "Left-click to revive");
            lore.add(ChatColor.RED + "Destroy to release from BanBox");
            lore.add("");
            lore.add(ChatColor.DARK_GRAY + "XSteal Player Head");
            lore.add(ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + PLAYER_HEAD_KEY + ":" + player.getName());
            
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        
        // Set player skin
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (VersionCompatibility.isAtLeast(1, 12)) {
                skullMeta.setOwningPlayer(player);
            } else {
                skullMeta.setOwner(player.getName());
            }
            head.setItemMeta(skullMeta);
        }
        
        return head;
    }
    
    /**
     * Check if ItemStack is a player head (for BanBox)
     */
    public boolean isPlayerHead(ItemStack item) {
        if (!isPlayerHead(item)) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                if (ChatColor.stripColor(line).startsWith(PLAYER_HEAD_KEY + ":")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get player name from player head
     */
    public String getPlayerFromHead(ItemStack item) {
        if (!isPlayerHead(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                String stripped = ChatColor.stripColor(line);
                if (stripped.startsWith(PLAYER_HEAD_KEY + ":")) {
                    return stripped.substring((PLAYER_HEAD_KEY + ":").length());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get entity type from head key
     */
    private EntityType getEntityTypeFromKey(String headKey) {
        try {
            // Handle special cases
            switch (headKey.toLowerCase()) {
                case "cave_spider":
                    return EntityType.CAVE_SPIDER;
                case "magma_cube":
                    return EntityType.MAGMA_CUBE;
                case "iron_golem":
                    return EntityType.IRON_GOLEM;
                case "snow_golem":
                    return EntityType.SNOWMAN;
                case "elder_guardian":
                    return EntityType.ELDER_GUARDIAN;
                case "ender_dragon":
                    return EntityType.ENDER_DRAGON;
                default:
                    return EntityType.valueOf(headKey.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            plugin.getPluginLogger().debug("Unknown entity type for head: " + headKey);
            return null;
        }
    }
    
    // Getters and utility methods
    
    public HeadData getHeadData(String key) {
        return loadedHeads.get(key);
    }
    
    public int getLoadedHeadCount() {
        return loadedHeads.size();
    }
    
    public Set<String> getLoadedHeadKeys() {
        return new HashSet<>(loadedHeads.keySet());
    }
    
    public boolean isHeadDatabaseAvailable() {
        return headDatabaseAvailable && headDatabaseAPI != null;
    }
    
    public void reload() {
        plugin.getPluginLogger().info("Reloading HeadManager...");
        loadHeads();
    }
    
    public void cleanup() {
        loadedHeads.clear();
        entityToHeadMap.clear();
        headToEntityMap.clear();
        headDatabaseAPI = null;
    }
    
    // Data classes
    
    public static class HeadData {
        private final String key;
        private final String displayName;
        private final String hdbId;
        private final String category;
        private final String description;
        private AbilityData ability;
        private final List<BossAbilityData> bossAbilities;
        private String resolvedTexture;
        
        public HeadData(String key, String displayName, String hdbId, String category, String description) {
            this.key = key;
            this.displayName = displayName;
            this.hdbId = hdbId;
            this.category = category;
            this.description = description;
            this.bossAbilities = new ArrayList<>();
        }
        
        // Getters and setters
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getHdbId() { return hdbId; }
        public String getCategory() { return category; }
        public String getDescription() { return description; }
        public AbilityData getAbility() { return ability; }
        public void setAbility(AbilityData ability) { this.ability = ability; }
        public List<BossAbilityData> getBossAbilities() { return bossAbilities; }
        public void addBossAbility(BossAbilityData ability) { this.bossAbilities.add(ability); }
        public String getResolvedTexture() { return resolvedTexture; }
        public void setResolvedTexture(String texture) { this.resolvedTexture = texture; }
        
        public boolean hasAbility() { return ability != null; }
        public boolean hasBossAbilities() { return !bossAbilities.isEmpty(); }
        public boolean isBossHead() { return "boss".equals(category); }
    }
    
    public static class AbilityData {
        private final String type;
        private final String activation;
        private final Map<String, Object> params;
        
        public AbilityData(String type, String activation, Map<String, Object> params) {
            this.type = type;
            this.activation = activation;
            this.params = params;
        }
        
        public String getType() { return type; }
        public String getActivation() { return activation; }
        public Map<String, Object> getParams() { return params; }
        public Object getParam(String key) { return params.get(key); }
        public <T> T getParam(String key, T defaultValue) {
            Object value = params.get(key);
            try {
                @SuppressWarnings("unchecked")
                T result = (T) value;
                return result != null ? result : defaultValue;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
    }
    
    public static class BossAbilityData {
        private final String name;
        private final String activation;
        private final String type;
        private final Map<String, Object> params;
        
        public BossAbilityData(String name, String activation, String type, Map<String, Object> params) {
            this.name = name;
            this.activation = activation;
            this.type = type;
            this.params = params;
        }
        
        public String getName() { return name; }
        public String getActivation() { return activation; }
        public String getType() { return type; }
        public Map<String, Object> getParams() { return params; }
        public Object getParam(String key) { return params.get(key); }
        public <T> T getParam(String key, T defaultValue) {
            Object value = params.get(key);
            try {
                @SuppressWarnings("unchecked")
                T result = (T) value;
                return result != null ? result : defaultValue;
            } catch (ClassCastException e) {
                return defaultValue;
            }
        }
    }
}