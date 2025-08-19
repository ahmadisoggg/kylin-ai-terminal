package com.headstealx.managers;

import com.headstealx.Main;
import com.headstealx.util.ConfigUtil;
import com.headstealx.util.VersionUtil;

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
 * HeadManager handles all head-related operations
 * - Loading head configurations from heads.yml
 * - Integration with HeadDatabase API
 * - Creating head ItemStacks
 * - Managing head metadata and identification
 */
public class HeadManager {
    
    private final Main plugin;
    private final boolean headDatabaseAvailable;
    private final Map<String, HeadData> loadedHeads;
    private final Map<EntityType, String> entityToHeadMap;
    private final Map<String, EntityType> headToEntityMap;
    
    // HeadDatabase API instance (loaded via reflection)
    private Object headDatabaseAPI;
    
    // Head identification metadata
    private static final String HEAD_ID_KEY = "headstealx_id";
    private static final String BANBOX_KEY = "banbox_victim";
    
    public HeadManager(Main plugin, boolean headDatabaseAvailable) {
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
            Object api = plugin.getLibbyWrapper().getHeadDatabaseAPI();
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
        FileConfiguration headsConfig = ConfigUtil.getHeadsConfig();
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
     * Load heads without HeadDatabase (degraded mode)
     */
    public void loadHeadsWithoutHDB() {
        plugin.getPluginLogger().info("Loading heads in degraded mode (no HeadDatabase)");
        loadHeads(); // Still load configuration, just without HDB integration
    }
    
    /**
     * Load head data from configuration section
     */
    private HeadData loadHeadData(String key, ConfigurationSection section) {
        if (section == null) return null;
        
        String displayName = section.getString("displayName");
        String hdbId = section.getString("hdb_id");
        String category = section.getString("category", "unknown");
        
        if (displayName == null || hdbId == null) {
            plugin.getPluginLogger().warning("Head '" + key + "' missing required fields");
            return null;
        }
        
        HeadData headData = new HeadData(key, displayName, hdbId, category);
        
        // Load regular ability
        if (section.contains("ability")) {
            ConfigurationSection abilitySection = section.getConfigurationSection("ability");
            if (abilitySection != null) {
                AbilityData ability = loadAbilityData(abilitySection);
                headData.setAbility(ability);
            }
        }
        
        // Load boss abilities
        if (section.contains("bossAbilities")) {
            List<Map<?, ?>> bossAbilities = section.getMapList("bossAbilities");
            for (Map<?, ?> abilityMap : bossAbilities) {
                BossAbilityData bossAbility = loadBossAbilityData(abilityMap);
                if (bossAbility != null) {
                    headData.addBossAbility(bossAbility);
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
        
        return new AbilityData(type, params);
    }
    
    /**
     * Load boss ability data from map
     */
    private BossAbilityData loadBossAbilityData(Map<?, ?> abilityMap) {
        String trigger = (String) abilityMap.get("trigger");
        String type = (String) abilityMap.get("type");
        
        if (trigger == null || type == null) return null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) abilityMap.get("params");
        if (params == null) params = new HashMap<>();
        
        return new BossAbilityData(trigger, type, params);
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
     * Get head texture from ItemStack
     */
    private String getHeadTexture(ItemStack headItem) {
        if (headItem == null || !isPlayerHead(headItem)) {
            return null;
        }
        
        SkullMeta meta = (SkullMeta) headItem.getItemMeta();
        if (meta == null) return null;
        
        try {
            // Use reflection to get texture (version-dependent)
            if (VersionUtil.isAtLeast(1, 20, 2)) {
                // Modern approach using PlayerProfile
                return getTextureModern(meta);
            } else {
                // Legacy approach using GameProfile
                return getTextureLegacy(meta);
            }
        } catch (Exception e) {
            plugin.getPluginLogger().debug("Failed to extract head texture: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get texture using modern PlayerProfile API (1.20.2+)
     */
    private String getTextureModern(SkullMeta meta) throws Exception {
        // This would use the modern PlayerProfile API
        // Implementation depends on exact Paper version
        return null; // Placeholder
    }
    
    /**
     * Get texture using legacy GameProfile approach
     */
    private String getTextureLegacy(SkullMeta meta) throws Exception {
        // This would use reflection to access GameProfile
        // Implementation is complex and version-dependent
        return null; // Placeholder
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
            lore.add(ChatColor.GRAY + "Category: " + ChatColor.WHITE + headData.getCategory());
            
            if (headData.hasAbility()) {
                lore.add(ChatColor.YELLOW + "Left-click to use ability");
            }
            
            if (headData.hasBossAbilities()) {
                lore.add(ChatColor.GOLD + "Boss Head - Multiple abilities:");
                for (BossAbilityData ability : headData.getBossAbilities()) {
                    lore.add(ChatColor.GOLD + "  " + ability.getTrigger() + ": " + ability.getType());
                }
            }
            
            meta.setLore(lore);
            
            // Add identification metadata
            if (VersionUtil.supportsPDC()) {
                addPersistentData(meta, HEAD_ID_KEY, headKey);
            } else {
                // Fallback for legacy versions
                addLegacyMetadata(meta, headKey);
            }
            
            head.setItemMeta(meta);
        }
        
        // Apply texture if available
        if (headDatabaseAvailable && headData.getResolvedTexture() != null) {
            applyHeadTexture(head, headData.getResolvedTexture());
        }
        
        return head;
    }
    
    /**
     * Create base head ItemStack
     */
    private ItemStack createBaseHeadItem() {
        if (VersionUtil.supportsNewMaterials()) {
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
        
        if (VersionUtil.supportsNewMaterials()) {
            return item.getType() == Material.PLAYER_HEAD;
        } else {
            return item.getType().name().equals("SKULL_ITEM") && item.getDurability() == 3;
        }
    }
    
    /**
     * Add persistent data (1.14+)
     */
    private void addPersistentData(ItemMeta meta, String key, String value) {
        // Implementation would use PersistentDataContainer
        // Simplified for now
    }
    
    /**
     * Add legacy metadata for older versions
     */
    private void addLegacyMetadata(ItemMeta meta, String headKey) {
        // Use lore or other methods to store metadata
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        
        lore.add(ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + headKey);
        meta.setLore(lore);
    }
    
    /**
     * Apply head texture
     */
    private void applyHeadTexture(ItemStack head, String texture) {
        // Implementation would use reflection to set skull texture
        // Complex and version-dependent
    }
    
    /**
     * Get head key from ItemStack
     */
    public String getHeadKey(ItemStack item) {
        if (!isPlayerHead(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        if (VersionUtil.supportsPDC()) {
            return getPersistentData(meta, HEAD_ID_KEY);
        } else {
            return getLegacyMetadata(meta);
        }
    }
    
    /**
     * Get persistent data
     */
    private String getPersistentData(ItemMeta meta, String key) {
        // Implementation would use PersistentDataContainer
        return null; // Placeholder
    }
    
    /**
     * Get legacy metadata
     */
    private String getLegacyMetadata(ItemMeta meta) {
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            if (line.startsWith(ChatColor.DARK_GRAY + "" + ChatColor.MAGIC)) {
                return ChatColor.stripColor(line);
            }
        }
        
        return null;
    }
    
    /**
     * Check if ItemStack is a banbox head
     */
    public boolean isBanboxHead(ItemStack item) {
        if (!isPlayerHead(item)) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        if (VersionUtil.supportsPDC()) {
            return getPersistentData(meta, BANBOX_KEY) != null;
        } else {
            // Check lore for banbox indicator
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("Banbox")) return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Get banbox victim from head
     */
    public String getBanboxVictim(ItemStack item) {
        if (!isBanboxHead(item)) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        if (VersionUtil.supportsPDC()) {
            return getPersistentData(meta, BANBOX_KEY);
        } else {
            // Extract from lore
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("Victim: ")) {
                        return ChatColor.stripColor(line).replace("Victim: ", "");
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Create banbox head for player
     */
    public ItemStack createBanboxHead(Player player) {
        ItemStack head = createBaseHeadItem();
        if (head == null) return null;
        
        ItemMeta meta = head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + player.getName() + "'s Head");
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Victim: " + ChatColor.WHITE + player.getName());
            lore.add(ChatColor.YELLOW + "Left-click to revive");
            lore.add(ChatColor.RED + "Destroy to permanently ban");
            meta.setLore(lore);
            
            // Add banbox metadata
            if (VersionUtil.supportsPDC()) {
                addPersistentData(meta, BANBOX_KEY, player.getName());
            }
            
            head.setItemMeta(meta);
        }
        
        // Set player skin
        if (head.getItemMeta() instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (VersionUtil.isAtLeast(1, 12)) {
                skullMeta.setOwningPlayer(player);
            } else {
                skullMeta.setOwner(player.getName());
            }
            head.setItemMeta(skullMeta);
        }
        
        return head;
    }
    
    /**
     * Get entity type from head key
     */
    private EntityType getEntityTypeFromKey(String headKey) {
        try {
            // Handle special cases
            switch (headKey.toLowerCase()) {
                case "zombified_piglin":
                    return VersionUtil.isAtLeast(1, 16) ? EntityType.ZOMBIFIED_PIGLIN : 
                           EntityType.valueOf("PIG_ZOMBIE");
                case "snow_golem":
                    return EntityType.SNOWMAN;
                case "iron_golem":
                    return EntityType.IRON_GOLEM;
                case "cave_spider":
                    return EntityType.CAVE_SPIDER;
                case "magma_cube":
                    return EntityType.MAGMA_CUBE;
                case "zombie_villager":
                    return EntityType.ZOMBIE_VILLAGER;
                case "elder_guardian":
                    return EntityType.ELDER_GUARDIAN;
                case "glow_squid":
                    return VersionUtil.isAtLeast(1, 17) ? EntityType.valueOf("GLOW_SQUID") : null;
                case "piglin_brute":
                    return VersionUtil.isAtLeast(1, 16) ? EntityType.valueOf("PIGLIN_BRUTE") : null;
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
        private AbilityData ability;
        private final List<BossAbilityData> bossAbilities;
        private String resolvedTexture;
        
        public HeadData(String key, String displayName, String hdbId, String category) {
            this.key = key;
            this.displayName = displayName;
            this.hdbId = hdbId;
            this.category = category;
            this.bossAbilities = new ArrayList<>();
        }
        
        // Getters and setters
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getHdbId() { return hdbId; }
        public String getCategory() { return category; }
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
        private final Map<String, Object> params;
        
        public AbilityData(String type, Map<String, Object> params) {
            this.type = type;
            this.params = params;
        }
        
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
    
    public static class BossAbilityData {
        private final String trigger;
        private final String type;
        private final Map<String, Object> params;
        
        public BossAbilityData(String trigger, String type, Map<String, Object> params) {
            this.trigger = trigger;
            this.type = type;
            this.params = params;
        }
        
        public String getTrigger() { return trigger; }
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