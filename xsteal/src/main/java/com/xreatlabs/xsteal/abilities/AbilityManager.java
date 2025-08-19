package com.xreatlabs.xsteal.abilities;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.heads.HeadManager.HeadData;
import com.xreatlabs.xsteal.heads.HeadManager.AbilityData;
import com.xreatlabs.xsteal.heads.HeadManager.BossAbilityData;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbilityManager for XSteal
 * Handles ability registration, execution, and management
 * Supports both regular abilities and boss combo abilities
 */
public class AbilityManager {
    
    private final XSteal plugin;
    private final Map<String, Ability> registeredAbilities;
    private final Map<UUID, Set<Entity>> summonedEntities;
    private final Map<UUID, Long> lastAbilityUse;
    private final AtomicInteger activeAbilityCount;
    private final HelmetSlotManager helmetSlotManager;
    
    // Boss combo tracking
    private final Map<UUID, BossComboTracker> bossComboTrackers;
    
    public AbilityManager(XSteal plugin) {
        this.plugin = plugin;
        this.registeredAbilities = new ConcurrentHashMap<>();
        this.summonedEntities = new ConcurrentHashMap<>();
        this.lastAbilityUse = new ConcurrentHashMap<>();
        this.activeAbilityCount = new AtomicInteger(0);
        this.bossComboTrackers = new ConcurrentHashMap<>();
        this.helmetSlotManager = new HelmetSlotManager(plugin);
    }
    
    /**
     * Register all ability implementations
     */
    public void registerAbilities() {
        plugin.getPluginLogger().info("Registering XSteal abilities...");
        
        // Hostile mob abilities
        registerAbility("summon_allies", new SummonAlliesAbility());
        registerAbility("infinite_arrows", new InfiniteArrowsAbility());
        registerAbility("controlled_explosion", new ControlledExplosionAbility());
        registerAbility("wall_climbing", new WallClimbingAbility());
        registerAbility("poison_web", new PoisonWebAbility());
        registerAbility("teleport_gaze", new TeleportGazeAbility());
        registerAbility("fire_mastery", new FireMasteryAbility());
        registerAbility("potion_mastery", new PotionMasteryAbility());
        registerAbility("ghast_flight", new GhastFlightAbility());
        registerAbility("slime_army", new SlimeArmyAbility());
        registerAbility("lava_mastery", new LavaMasteryAbility());
        
        // Boss abilities
        registerAbility("sonic_attack", new SonicAttackAbility());
        registerAbility("area_blindness", new AreaBlindnessAbility());
        registerAbility("vibration_sense", new VibrationSenseAbility());
        registerAbility("dragon_fireball", new DragonFireballAbility());
        registerAbility("summon_crystals", new SummonCrystalsAbility());
        registerAbility("dragon_flight", new DragonFlightAbility());
        registerAbility("wither_skulls", new WitherSkullsAbility());
        registerAbility("wither_shield", new WitherShieldAbility());
        registerAbility("wither_storm", new WitherStormAbility());
        
        // Passive/Utility abilities
        registerAbility("milk_production", new MilkProductionAbility());
        registerAbility("pig_speed", new PigSpeedAbility());
        registerAbility("wool_production", new WoolProductionAbility());
        registerAbility("chicken_flight", new ChickenFlightAbility());
        registerAbility("horse_speed", new HorseSpeedAbility());
        registerAbility("wolf_pack", new WolfPackAbility());
        registerAbility("cat_stealth", new CatStealthAbility());
        
        // Aquatic abilities
        registerAbility("aquatic_mastery", new AquaticMasteryAbility());
        registerAbility("ink_defense", new InkDefenseAbility());
        registerAbility("guardian_laser", new GuardianLaserAbility());
        registerAbility("elder_powers", new ElderPowersAbility());
        
        // Nether abilities
        registerAbility("gold_mastery", new GoldMasteryAbility());
        registerAbility("hoglin_charge", new HoglinChargeAbility());
        registerAbility("lava_strider", new LavaStriderAbility());
        
        // End abilities
        registerAbility("ender_swarm", new EnderSwarmAbility());
        registerAbility("shulker_powers", new ShulkerPowersAbility());
        
        // Constructed abilities
        registerAbility("iron_strength", new IronStrengthAbility());
        registerAbility("snow_powers", new SnowPowersAbility());
        
        // Enhanced useful abilities
        registerAbility("villager_trading_mastery", new VillagerTradingAbility());
        registerAbility("bat_echolocation", new BatEcholocationAbility());
        registerAbility("bee_pollination", new BeePollinationAbility());
        registerAbility("allay_collection", new AllayCollectionAbility());
        registerAbility("sniffer_archaeology", new SnifferArchaeologyAbility());
        registerAbility("dolphin_navigation", new DolphinNavigationAbility());
        registerAbility("goat_mountain_powers", new GoatMountainAbility());
        registerAbility("iron_golem_construction", new IronGolemConstructionAbility());
        
        // Legendary abilities
        registerAbility("black_hole", new BlackHoleAbility());
        
        plugin.getPluginLogger().info("Registered " + registeredAbilities.size() + " abilities");
    }
    
    /**
     * Register a single ability
     */
    private void registerAbility(String type, Ability ability) {
        registeredAbilities.put(type, ability);
        plugin.getPluginLogger().debug("Registered ability: " + type);
    }
    
    /**
     * Execute ability for player wearing head in helmet slot
     */
    public boolean executeHelmetAbility(Player player, String headKey, String activationType) {
        if (!plugin.isPluginReady()) {
            return false;
        }
        
        HeadData headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null) {
            return false;
        }
        
        // Check if this is a boss head with multiple abilities
        if (headData.isBossHead() && headData.hasBossAbilities()) {
            return executeBossAbility(player, headData, activationType);
        } else if (headData.hasAbility()) {
            // Check if activation type matches
            if (headData.getAbility().getActivation().equals(activationType) || 
                headData.getAbility().getActivation().equals("passive")) {
                return executeRegularAbility(player, headData.getAbility());
            }
        }
        
        return false;
    }
    
    /**
     * Execute a regular ability
     */
    private boolean executeRegularAbility(Player player, AbilityData abilityData) {
        String abilityType = abilityData.getType();
        Ability ability = registeredAbilities.get(abilityType);
        
        if (ability == null) {
            plugin.getPluginLogger().warning("Unknown ability type: " + abilityType);
            return false;
        }
        
        // Check permissions
        if (!player.hasPermission("xsteal.ability.use")) {
            player.sendMessage("§cYou don't have permission to use abilities!");
            return false;
        }
        
        // Check performance limits
        if (activeAbilityCount.get() >= plugin.getConfigManager().getMainConfig().getInt("abilities.max_concurrent_abilities", 20)) {
            player.sendMessage("§cServer is too busy to process abilities right now!");
            return false;
        }
        
        // Execute ability
        try {
            activeAbilityCount.incrementAndGet();
            lastAbilityUse.put(player.getUniqueId(), System.currentTimeMillis());
            
            AbilityContext context = new AbilityContext(player, abilityData.getParams(), plugin);
            boolean success = ability.execute(context);
            
            if (success) {
                // Play effects if enabled
                if (plugin.getConfigManager().areSoundsEnabled()) {
                    ability.playSound(player);
                }
                
                if (plugin.getConfigManager().areParticlesEnabled()) {
                    ability.playParticles(player);
                }
                
                // Send action bar feedback
                if (plugin.getConfigManager().isActionBarFeedbackEnabled()) {
                    sendActionBar(player, "§6" + abilityType.replace("_", " ").toUpperCase() + " ACTIVATED!");
                }
                
                plugin.getPluginLogger().debug("Executed ability " + abilityType + " for " + player.getName());
            }
            
            return success;
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error executing ability " + abilityType + ": " + e.getMessage());
            player.sendMessage("§cAbility failed to execute!");
            return false;
            
        } finally {
            activeAbilityCount.decrementAndGet();
        }
    }
    
    /**
     * Execute a boss ability with combo detection
     */
    private boolean executeBossAbility(Player player, HeadData headData, String activationType) {
        // Check boss ability permission
        if (!player.hasPermission("xsteal.ability.boss")) {
            player.sendMessage("§cYou don't have permission to use boss abilities!");
            return false;
        }
        
        // Find matching boss ability
        BossAbilityData bossAbility = null;
        for (BossAbilityData ability : headData.getBossAbilities()) {
            if (ability.getActivation().equals(activationType)) {
                bossAbility = ability;
                break;
            }
        }
        
        if (bossAbility == null) {
            plugin.getPluginLogger().debug("No boss ability found for activation: " + activationType);
            return false;
        }
        
        // Update combo tracker
        BossComboTracker tracker = bossComboTrackers.computeIfAbsent(
            player.getUniqueId(), 
            k -> new BossComboTracker(headData.getKey())
        );
        
        tracker.addActivation(activationType);
        
        // Execute the ability
        String abilityType = bossAbility.getType();
        Ability ability = registeredAbilities.get(abilityType);
        
        if (ability == null) {
            plugin.getPluginLogger().warning("Unknown boss ability type: " + abilityType);
            return false;
        }
        
        try {
            activeAbilityCount.incrementAndGet();
            
            AbilityContext context = new AbilityContext(player, bossAbility.getParams(), plugin);
            boolean success = ability.execute(context);
            
            if (success) {
                // Enhanced effects for boss abilities
                if (plugin.getConfigManager().areSoundsEnabled()) {
                    ability.playSound(player);
                }
                
                if (plugin.getConfigManager().areParticlesEnabled()) {
                    ability.playParticles(player);
                }
                
                // Boss ability feedback
                sendActionBar(player, "§6§l" + bossAbility.getName().toUpperCase() + "!");
                player.sendMessage("§6Boss Ability: §e" + bossAbility.getName());
                
                plugin.getPluginLogger().debug("Executed boss ability " + abilityType + " for " + player.getName());
            }
            
            return success;
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error executing boss ability " + abilityType + ": " + e.getMessage());
            player.sendMessage("§cBoss ability failed to execute!");
            return false;
            
        } finally {
            activeAbilityCount.decrementAndGet();
        }
    }
    
    /**
     * Send action bar message to player
     */
    private void sendActionBar(Player player, String message) {
        try {
            if (VersionCompatibility.isAtLeast(1, 11)) {
                // Use Spigot action bar API
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
            } else {
                // Fallback to chat message for older versions
                player.sendMessage(message);
            }
        } catch (Exception e) {
            // Fallback to regular message
            player.sendMessage(message);
        }
    }
    
    /**
     * Add summoned entity to tracking
     */
    public void addSummonedEntity(Player player, Entity entity) {
        summonedEntities.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(entity);
        
        // Check entity limit
        Set<Entity> playerEntities = summonedEntities.get(player.getUniqueId());
        int maxEntities = plugin.getConfigManager().getMaxSummonedEntities();
        
        if (playerEntities.size() > maxEntities) {
            // Remove oldest entities
            Iterator<Entity> iterator = playerEntities.iterator();
            while (iterator.hasNext() && playerEntities.size() > maxEntities) {
                Entity oldEntity = iterator.next();
                if (oldEntity.isValid()) {
                    oldEntity.remove();
                }
                iterator.remove();
            }
        }
    }
    
    /**
     * Clean up summoned entities
     */
    public void cleanupSummonedEntities() {
        summonedEntities.entrySet().removeIf(entry -> {
            Set<Entity> entities = entry.getValue();
            entities.removeIf(entity -> !entity.isValid());
            return entities.isEmpty();
        });
        
        // Clean up expired combo trackers
        long currentTime = System.currentTimeMillis();
        int comboResetTime = plugin.getConfigManager().getComboResetTime();
        
        bossComboTrackers.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(currentTime, comboResetTime));
    }
    
    /**
     * Remove summoned entities for a player
     */
    public void removeSummonedEntities(Player player) {
        Set<Entity> entities = summonedEntities.remove(player.getUniqueId());
        if (entities != null) {
            entities.forEach(entity -> {
                if (entity.isValid()) {
                    entity.remove();
                }
            });
        }
    }
    
    /**
     * Check if player is wearing a head with abilities
     */
    public boolean isWearingAbilityHead(Player player) {
        return helmetSlotManager.isWearingHead(player);
    }
    
    /**
     * Get the head key from player's helmet
     */
    public String getWornHeadKey(Player player) {
        return helmetSlotManager.getCurrentlyWornHead(player);
    }
    
    /**
     * Get helmet slot manager
     */
    public HelmetSlotManager getHelmetSlotManager() {
        return helmetSlotManager;
    }
    
    /**
     * Process boss combo input
     */
    public void processBossCombo(Player player, String activationType) {
        if (!plugin.getConfigManager().areBossCombosEnabled()) {
            return;
        }
        
        String headKey = getWornHeadKey(player);
        if (headKey == null) return;
        
        HeadData headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null || !headData.isBossHead()) {
            return;
        }
        
        // Update combo tracker
        BossComboTracker tracker = bossComboTrackers.computeIfAbsent(
            player.getUniqueId(), 
            k -> new BossComboTracker(headKey)
        );
        
        tracker.addActivation(activationType);
        
        // Execute boss ability
        executeHelmetAbility(player, headKey, activationType);
    }
    
    // Getters and utility methods
    
    public Ability getAbility(String type) {
        return registeredAbilities.get(type);
    }
    
    public int getRegisteredAbilityCount() {
        return registeredAbilities.size();
    }
    
    public int getActiveAbilityCount() {
        return activeAbilityCount.get();
    }
    
    public int getSummonedEntityCount() {
        return summonedEntities.values().stream()
            .mapToInt(Set::size)
            .sum();
    }
    
    public void reload() {
        plugin.getPluginLogger().info("Reloading AbilityManager...");
        // Clear temporary data on reload
        lastAbilityUse.clear();
        bossComboTrackers.clear();
    }
    
    public void cleanup() {
        // Remove all summoned entities
        summonedEntities.values().forEach(entities -> 
            entities.forEach(entity -> {
                if (entity.isValid()) {
                    entity.remove();
                }
            }));
        
        // Cleanup helmet slot manager
        if (helmetSlotManager != null) {
            helmetSlotManager.cleanup();
        }
        
        registeredAbilities.clear();
        summonedEntities.clear();
        lastAbilityUse.clear();
        bossComboTrackers.clear();
        activeAbilityCount.set(0);
    }
    
    /**
     * Boss combo tracker for managing boss ability combinations
     */
    public static class BossComboTracker {
        private final String headKey;
        private final List<String> activations;
        private long lastActivationTime;
        
        public BossComboTracker(String headKey) {
            this.headKey = headKey;
            this.activations = new ArrayList<>();
            this.lastActivationTime = System.currentTimeMillis();
        }
        
        public void addActivation(String activation) {
            activations.add(activation);
            lastActivationTime = System.currentTimeMillis();
            
            // Keep only recent activations (last 5)
            if (activations.size() > 5) {
                activations.remove(0);
            }
        }
        
        public boolean isExpired(long currentTime, long timeout) {
            return currentTime - lastActivationTime > timeout;
        }
        
        public List<String> getActivations() {
            return new ArrayList<>(activations);
        }
        
        public String getHeadKey() {
            return headKey;
        }
    }
}