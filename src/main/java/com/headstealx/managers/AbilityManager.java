package com.headstealx.managers;

import com.headstealx.Main;
import com.headstealx.abilities.*;
import com.headstealx.managers.HeadManager.AbilityData;
import com.headstealx.managers.HeadManager.BossAbilityData;
import com.headstealx.managers.HeadManager.HeadData;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AbilityManager handles all ability-related operations
 * - Registering ability implementations
 * - Executing abilities when triggered
 * - Managing cooldowns and limitations
 * - Tracking active abilities
 */
public class AbilityManager {
    
    private final Main plugin;
    private final Map<String, Ability> registeredAbilities;
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private final Map<UUID, String> activeAbilities;
    private final AtomicInteger activeAbilityCount;
    
    // Boss ability combo tracking
    private final Map<UUID, BossComboTracker> bossComboTrackers;
    
    public AbilityManager(Main plugin) {
        this.plugin = plugin;
        this.registeredAbilities = new ConcurrentHashMap<>();
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.activeAbilities = new ConcurrentHashMap<>();
        this.activeAbilityCount = new AtomicInteger(0);
        this.bossComboTrackers = new ConcurrentHashMap<>();
        
        // Start cleanup task
        startCleanupTask();
    }
    
    /**
     * Register all ability implementations
     */
    public void registerAbilities() {
        plugin.getPluginLogger().register("Registering abilities...");
        
        // Hostile mob abilities
        registerAbility("lifesteal", new LifestealAbility());
        registerAbility("arrow_spread", new ArrowSpreadAbility());
        registerAbility("self_explode", new SelfExplodeAbility());
        registerAbility("web_lunge", new WebLungeAbility());
        registerAbility("poison_cloud", new PoisonCloudAbility());
        registerAbility("spawn_slimes", new SpawnSlimesAbility());
        registerAbility("lava_slam", new LavaSlamAbility());
        registerAbility("fireball_large", new FireballLargeAbility());
        registerAbility("fireball_triple", new FireballTripleAbility());
        registerAbility("potion_toss", new PotionTossAbility());
        registerAbility("plague_summon", new PlagueSummonAbility());
        registerAbility("desert_blight", new DesertBlightAbility());
        registerAbility("frost_shot", new FrostShotAbility());
        registerAbility("trident_throw", new TridentThrowAbility());
        registerAbility("glide_dive", new GlideDiveAbility());
        registerAbility("infestation", new InfestationAbility());
        registerAbility("blink_swarm", new BlinkSwarmAbility());
        registerAbility("phase_strike", new PhaseStrikeAbility());
        registerAbility("fang_line", new FangLineAbility());
        registerAbility("crossbow_burst", new CrossbowBurstAbility());
        registerAbility("axe_charge", new AxeChargeAbility());
        registerAbility("colossus_charge", new ColossusChargeAbility());
        registerAbility("mirror_copy", new MirrorCopyAbility());
        
        // Aquatic abilities
        registerAbility("laser_beam", new LaserBeamAbility());
        registerAbility("elder_crush", new ElderCrushAbility());
        registerAbility("riptide_surge", new RiptideSurgeAbility());
        registerAbility("regrowth", new RegrowthAbility());
        registerAbility("luminous_pulse", new LuminousPulseAbility());
        
        // Nether abilities
        registerAbility("barter_burst", new BarterBurstAbility());
        registerAbility("brute_slam", new BruteSlamAbility());
        registerAbility("call_reinforcements", new CallReinforcementsAbility());
        registerAbility("gore_rush", new GoreRushAbility());
        registerAbility("berserk", new BerserkAbility());
        registerAbility("lava_stride", new LavaStrideAbility());
        
        // End abilities
        registerAbility("teleport_step", new TeleportStepAbility());
        registerAbility("levitate_shot", new LevitateShotAbility());
        
        // Passive/Utility abilities
        registerAbility("milk_burst", new MilkBurstAbility());
        registerAbility("wool_shield", new WoolShieldAbility());
        registerAbility("sprint_boost", new SprintBoostAbility());
        registerAbility("feather_float", new FeatherFloatAbility());
        registerAbility("spring_leap", new SpringLeapAbility());
        registerAbility("fast_mount", new FastMountAbility());
        registerAbility("portable_chest", new PortableChestAbility());
        registerAbility("alpha_call", new AlphaCallAbility());
        registerAbility("stealth_toggle", new StealthToggleAbility());
        registerAbility("decoy_flock", new DecoyFlockAbility());
        registerAbility("shell_guard", new ShellGuardAbility());
        registerAbility("brawler_stance", new BrawlerStanceAbility());
        registerAbility("dash_backstab", new DashBackstabAbility());
        
        // Constructed abilities
        registerAbility("frost_barrage", new FrostBarrageAbility());
        registerAbility("colossus_slam", new ColossusSlamAbility());
        
        // Special utility abilities
        registerAbility("item_grab", new ItemGrabAbility());
        registerAbility("sting_swarm", new StingSwarmAbility());
        registerAbility("ramming_horn", new RammingHornAbility());
        registerAbility("spit_volley", new SpitVolleyAbility());
        registerAbility("saddle_rush", new SaddleRushAbility());
        registerAbility("tongue_pull", new TonguePullAbility());
        
        // Boss abilities
        registerAbility("wing_gust", new WingGustAbility());
        registerAbility("dragon_breath", new DragonBreathAbility());
        registerAbility("sky_strike", new SkyStrikeAbility());
        registerAbility("skull_barrage", new SkullBarrageAbility());
        registerAbility("decay_field", new DecayFieldAbility());
        registerAbility("shield_phase", new ShieldPhaseAbility());
        registerAbility("sonic_boom", new SonicBoomAbility());
        registerAbility("seismic_slam", new SeismicSlamAbility());
        registerAbility("echo_locator", new EchoLocatorAbility());
        
        plugin.getPluginLogger().register("Registered " + registeredAbilities.size() + " abilities");
    }
    
    /**
     * Register a single ability
     */
    private void registerAbility(String type, Ability ability) {
        registeredAbilities.put(type, ability);
        plugin.getPluginLogger().debug("Registered ability: " + type);
    }
    
    /**
     * Execute an ability for a player
     */
    public boolean execute(Player player, String headKey, Event triggerEvent) {
        if (!plugin.isPluginReady()) {
            return false;
        }
        
        HeadData headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null) {
            plugin.getPluginLogger().debug("No head data found for key: " + headKey);
            return false;
        }
        
        // Check performance limits
        if (activeAbilityCount.get() >= plugin.getConfig().getInt("performance.max_concurrent_abilities", 10)) {
            player.sendMessage("§cServer is too busy to process abilities right now!");
            return false;
        }
        
        // Handle boss abilities vs regular abilities
        if (headData.isBossHead() && headData.hasBossAbilities()) {
            return executeBossAbility(player, headData, triggerEvent);
        } else if (headData.hasAbility()) {
            return executeRegularAbility(player, headData.getAbility(), triggerEvent);
        }
        
        return false;
    }
    
    /**
     * Execute a regular ability
     */
    private boolean executeRegularAbility(Player player, AbilityData abilityData, Event triggerEvent) {
        String abilityType = abilityData.getType();
        Ability ability = registeredAbilities.get(abilityType);
        
        if (ability == null) {
            plugin.getPluginLogger().warning("Unknown ability type: " + abilityType);
            return false;
        }
        
        // Check cooldown
        if (isOnCooldown(player, abilityType)) {
            long remainingTime = getCooldownRemaining(player, abilityType);
            player.sendMessage("§cAbility on cooldown for " + (remainingTime / 1000) + " seconds!");
            return false;
        }
        
        // Check permissions
        if (!player.hasPermission("headsteal.ability.use")) {
            player.sendMessage("§cYou don't have permission to use abilities!");
            return false;
        }
        
        // Execute ability
        try {
            activeAbilityCount.incrementAndGet();
            activeAbilities.put(player.getUniqueId(), abilityType);
            
            AbilityContext context = new AbilityContext(player, abilityData.getParams(), triggerEvent, plugin);
            boolean success = ability.execute(context);
            
            if (success) {
                // Apply cooldown
                applyCooldown(player, abilityType);
                
                // Play sound/particles if enabled
                if (plugin.getConfig().getBoolean("abilities.sounds", true)) {
                    ability.playSound(player);
                }
                
                if (plugin.getConfig().getBoolean("abilities.particles", true)) {
                    ability.playParticles(player);
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
            activeAbilities.remove(player.getUniqueId());
        }
    }
    
    /**
     * Execute a boss ability with combo tracking
     */
    private boolean executeBossAbility(Player player, HeadData headData, Event triggerEvent) {
        String trigger = determineTrigger(triggerEvent);
        if (trigger == null) {
            plugin.getPluginLogger().debug("Could not determine trigger for boss ability");
            return false;
        }
        
        // Find matching boss ability
        BossAbilityData bossAbility = null;
        for (BossAbilityData ability : headData.getBossAbilities()) {
            if (ability.getTrigger().equals(trigger)) {
                bossAbility = ability;
                break;
            }
        }
        
        if (bossAbility == null) {
            plugin.getPluginLogger().debug("No boss ability found for trigger: " + trigger);
            return false;
        }
        
        // Check boss ability permission
        if (!player.hasPermission("headsteal.ability.boss")) {
            player.sendMessage("§cYou don't have permission to use boss abilities!");
            return false;
        }
        
        // Update combo tracker
        BossComboTracker tracker = bossComboTrackers.computeIfAbsent(
            player.getUniqueId(), 
            k -> new BossComboTracker(headData.getKey())
        );
        
        tracker.addTrigger(trigger);
        
        // Execute the ability
        String abilityType = bossAbility.getType();
        Ability ability = registeredAbilities.get(abilityType);
        
        if (ability == null) {
            plugin.getPluginLogger().warning("Unknown boss ability type: " + abilityType);
            return false;
        }
        
        try {
            activeAbilityCount.incrementAndGet();
            activeAbilities.put(player.getUniqueId(), abilityType);
            
            AbilityContext context = new AbilityContext(player, bossAbility.getParams(), triggerEvent, plugin);
            boolean success = ability.execute(context);
            
            if (success) {
                // Boss abilities typically don't have cooldowns
                player.sendMessage("§6Boss Ability: §e" + abilityType);
                
                // Enhanced effects for boss abilities
                if (plugin.getConfig().getBoolean("abilities.sounds", true)) {
                    ability.playSound(player);
                }
                
                if (plugin.getConfig().getBoolean("abilities.particles", true)) {
                    ability.playParticles(player);
                }
                
                plugin.getPluginLogger().debug("Executed boss ability " + abilityType + " for " + player.getName());
            }
            
            return success;
            
        } catch (Exception e) {
            plugin.getPluginLogger().severe("Error executing boss ability " + abilityType + ": " + e.getMessage());
            player.sendMessage("§cBoss ability failed to execute!");
            return false;
            
        } finally {
            activeAbilityCount.decrementAndGet();
            activeAbilities.remove(player.getUniqueId());
        }
    }
    
    /**
     * Determine trigger type from event
     */
    private String determineTrigger(Event event) {
        // This would analyze the event to determine the trigger
        // For now, return a default
        return "left_click"; // Placeholder
    }
    
    /**
     * Check if player is on cooldown for ability
     */
    private boolean isOnCooldown(Player player, String abilityType) {
        if (!plugin.getConfig().getBoolean("abilities.use_cooldowns", false)) {
            return false; // Cooldowns disabled
        }
        
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (cooldowns == null) return false;
        
        Long cooldownEnd = cooldowns.get(abilityType);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }
    
    /**
     * Get remaining cooldown time in milliseconds
     */
    private long getCooldownRemaining(Player player, String abilityType) {
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (cooldowns == null) return 0;
        
        Long cooldownEnd = cooldowns.get(abilityType);
        if (cooldownEnd == null) return 0;
        
        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }
    
    /**
     * Apply cooldown to player for ability
     */
    private void applyCooldown(Player player, String abilityType) {
        if (!plugin.getConfig().getBoolean("abilities.use_cooldowns", false)) {
            return; // Cooldowns disabled
        }
        
        int globalCooldown = plugin.getConfig().getInt("abilities.global_cooldown", 30);
        double multiplier = plugin.getConfig().getDouble("abilities.cooldown_multiplier", 1.0);
        
        long cooldownMs = (long) (globalCooldown * 1000 * multiplier);
        long cooldownEnd = System.currentTimeMillis() + cooldownMs;
        
        playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                     .put(abilityType, cooldownEnd);
    }
    
    /**
     * Clear cooldowns for a player
     */
    public void clearCooldowns(Player player) {
        playerCooldowns.remove(player.getUniqueId());
        plugin.getPluginLogger().debug("Cleared cooldowns for " + player.getName());
    }
    
    /**
     * Start cleanup task for expired cooldowns and combo trackers
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
                cleanupExpiredComboTrackers();
            }
        }.runTaskTimerAsynchronously(plugin, 20L * 60L, 20L * 60L); // Every minute
    }
    
    /**
     * Clean up expired cooldowns
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        
        playerCooldowns.entrySet().removeIf(entry -> {
            Map<String, Long> cooldowns = entry.getValue();
            cooldowns.entrySet().removeIf(cooldownEntry -> 
                cooldownEntry.getValue() < currentTime);
            return cooldowns.isEmpty();
        });
    }
    
    /**
     * Clean up expired combo trackers
     */
    private void cleanupExpiredComboTrackers() {
        long timeout = plugin.getConfig().getLong("abilities.boss_combo_timeout", 5) * 1000;
        long currentTime = System.currentTimeMillis();
        
        bossComboTrackers.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(currentTime, timeout));
    }
    
    /**
     * Get ability by type
     */
    public Ability getAbility(String type) {
        return registeredAbilities.get(type);
    }
    
    /**
     * Check if ability type is registered
     */
    public boolean isAbilityRegistered(String type) {
        return registeredAbilities.containsKey(type);
    }
    
    // Getters and utility methods
    
    public int getRegisteredAbilityCount() {
        return registeredAbilities.size();
    }
    
    public int getActiveAbilityCount() {
        return activeAbilityCount.get();
    }
    
    public Set<String> getRegisteredAbilityTypes() {
        return new HashSet<>(registeredAbilities.keySet());
    }
    
    public void reload() {
        plugin.getPluginLogger().info("Reloading AbilityManager...");
        // Clear cooldowns on reload
        playerCooldowns.clear();
        bossComboTrackers.clear();
    }
    
    public void cleanup() {
        registeredAbilities.clear();
        playerCooldowns.clear();
        activeAbilities.clear();
        bossComboTrackers.clear();
        activeAbilityCount.set(0);
    }
    
    /**
     * Boss combo tracker for managing boss ability combinations
     */
    private static class BossComboTracker {
        private final String headKey;
        private final List<String> triggers;
        private long lastTriggerTime;
        
        public BossComboTracker(String headKey) {
            this.headKey = headKey;
            this.triggers = new ArrayList<>();
            this.lastTriggerTime = System.currentTimeMillis();
        }
        
        public void addTrigger(String trigger) {
            triggers.add(trigger);
            lastTriggerTime = System.currentTimeMillis();
            
            // Keep only recent triggers (last 10)
            if (triggers.size() > 10) {
                triggers.remove(0);
            }
        }
        
        public boolean isExpired(long currentTime, long timeout) {
            return currentTime - lastTriggerTime > timeout;
        }
        
        public List<String> getTriggers() {
            return new ArrayList<>(triggers);
        }
        
        public String getHeadKey() {
            return headKey;
        }
    }
}