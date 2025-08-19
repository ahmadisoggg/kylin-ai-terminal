package com.xreatlabs.xsteal.abilities;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.heads.HeadManager.HeadData;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helmet Slot Manager for XSteal
 * Ensures head abilities are properly activated when players wear heads in helmet slot
 * Monitors helmet slot changes and manages passive abilities
 */
public class HelmetSlotManager implements Listener {
    
    private final XSteal plugin;
    private final Map<UUID, String> currentlyWornHeads;
    private final Map<UUID, Long> lastHelmetCheck;
    
    public HelmetSlotManager(XSteal plugin) {
        this.plugin = plugin;
        this.currentlyWornHeads = new ConcurrentHashMap<>();
        this.lastHelmetCheck = new ConcurrentHashMap<>();
        
        // Register event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Start helmet monitoring task
        startHelmetMonitoringTask();
    }
    
    /**
     * Start continuous helmet slot monitoring
     */
    private void startHelmetMonitoringTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isPluginReady()) {
                    return;
                }
                
                // Check all online players' helmet slots
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    checkPlayerHelmetSlot(player);
                }
            }
        }.runTaskTimer(plugin, 20L, 40L); // Check every 2 seconds
        
        plugin.getPluginLogger().info("Helmet slot monitoring started");
    }
    
    /**
     * Check individual player's helmet slot
     */
    private void checkPlayerHelmetSlot(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        String currentHeadKey = helmet != null ? plugin.getHeadManager().getHeadKey(helmet) : null;
        String previousHeadKey = currentlyWornHeads.get(player.getUniqueId());
        
        // Check if helmet changed
        if (!java.util.Objects.equals(currentHeadKey, previousHeadKey)) {
            handleHelmetChange(player, previousHeadKey, currentHeadKey);
        }
        
        // Update last check time
        lastHelmetCheck.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    /**
     * Handle helmet slot changes
     */
    private void handleHelmetChange(Player player, String previousHeadKey, String currentHeadKey) {
        // Remove previous head effects
        if (previousHeadKey != null) {
            deactivateHeadAbilities(player, previousHeadKey);
        }
        
        // Activate new head abilities
        if (currentHeadKey != null) {
            activateHeadAbilities(player, currentHeadKey);
        }
        
        // Update tracking
        if (currentHeadKey != null) {
            currentlyWornHeads.put(player.getUniqueId(), currentHeadKey);
        } else {
            currentlyWornHeads.remove(player.getUniqueId());
        }
        
        plugin.getPluginLogger().debug("Helmet change for " + player.getName() + 
            ": " + previousHeadKey + " → " + currentHeadKey);
    }
    
    /**
     * Activate head abilities when worn
     */
    private void activateHeadAbilities(Player player, String headKey) {
        HeadData headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null) {
            return;
        }
        
        // Send equip message
        String headName = ChatColor.translateAlternateColorCodes('&', headData.getDisplayName());
        player.sendMessage("§6§l[XSteal] §r" + headName + " §6equipped!");
        player.sendMessage("§7" + headData.getDescription());
        
        // Handle different head types
        if (headData.isBossHead()) {
            activateBossHead(player, headData);
        } else if (headData.hasAbility()) {
            activateRegularHead(player, headData);
        }
        
        // Play equip effects
        playHelmetEquipEffects(player, headKey);
        
        plugin.getPluginLogger().debug("Activated abilities for " + headKey + " worn by " + player.getName());
    }
    
    /**
     * Activate boss head abilities
     */
    private void activateBossHead(Player player, HeadData headData) {
        player.sendMessage("§6§l👑 BOSS HEAD EQUIPPED!");
        player.sendMessage("§e§lCombo Abilities Available:");
        
        for (var bossAbility : headData.getBossAbilities()) {
            String activation = bossAbility.getActivation().replace("_", " ").toUpperCase();
            player.sendMessage("§e• " + activation + " §7→ " + bossAbility.getName());
        }
        
        player.sendMessage("§7Use the combo system to unleash boss powers!");
        
        // Grant boss head passive effects
        grantBossPassiveEffects(player, headData.getKey());
    }
    
    /**
     * Activate regular head abilities
     */
    private void activateRegularHead(Player player, HeadData headData) {
        String abilityType = headData.getAbility().getType();
        String activation = headData.getAbility().getActivation();
        
        player.sendMessage("§a🪄 Ability: " + abilityType.replace("_", " ").toUpperCase());
        player.sendMessage("§7Activation: " + activation.replace("_", " "));
        
        // If passive ability, activate immediately
        if ("passive".equals(activation)) {
            plugin.getAbilityManager().executeHelmetAbility(player, headData.getKey(), "passive");
            player.sendMessage("§a✨ Passive ability activated!");
        } else {
            player.sendMessage("§7Left-click to activate ability");
        }
        
        // Grant passive effects based on head type
        grantPassiveEffects(player, headData.getKey(), abilityType);
    }
    
    /**
     * Grant passive effects for wearing specific heads
     */
    private void grantPassiveEffects(Player player, String headKey, String abilityType) {
        // Common passive effects for wearing heads
        switch (headKey.toLowerCase()) {
            case "spider":
            case "cave_spider":
                // Spider heads grant wall climbing
                startWallClimbingEffect(player);
                break;
                
            case "enderman":
                // Enderman head grants water damage
                startEndermanEffects(player);
                break;
                
            case "blaze":
            case "magma_cube":
                // Fire mobs grant fire immunity
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
                player.sendMessage("§6🔥 Fire immunity granted!");
                break;
                
            case "strider":
                // Strider grants lava walking
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
                player.sendMessage("§c🌋 Lava walking enabled!");
                break;
                
            case "dolphin":
            case "axolotl":
                // Aquatic mobs grant water breathing
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 1));
                player.sendMessage("§b🌊 Aquatic mastery granted!");
                break;
                
            case "horse":
            case "pig":
                // Speed mobs grant speed boost
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
                player.sendMessage("§f🏃 Speed boost activated!");
                break;
                
            case "chicken":
                // Chicken grants slow falling
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0));
                player.sendMessage("§f🐔 Slow falling activated!");
                break;
                
            case "bat":
                // Bat grants night vision
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0));
                player.sendMessage("§8🦇 Night vision activated!");
                break;
                
            case "rabbit":
                // Rabbit grants jump boost
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.JUMP, Integer.MAX_VALUE, 2));
                player.sendMessage("§7🐰 Jump boost activated!");
                break;
        }
    }
    
    /**
     * Grant boss head passive effects
     */
    private void grantBossPassiveEffects(Player player, String bossHeadKey) {
        switch (bossHeadKey.toLowerCase()) {
            case "warden":
                // Warden head grants darkness immunity and vibration detection
                player.sendMessage("§0👁 Warden senses activated!");
                startVibrationDetection(player);
                break;
                
            case "ender_dragon":
                // Dragon head grants flight and end immunity
                player.setAllowFlight(true);
                player.sendMessage("§5🐲 Dragon flight enabled!");
                break;
                
            case "wither":
                // Wither head grants wither immunity and undead benefits
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 1));
                player.sendMessage("§0💀 Wither resistance activated!");
                break;
                
            case "apocalypse_head":
                // Apocalypse head grants ultimate passive effects
                player.setAllowFlight(true);
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 2));
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0));
                player.sendMessage("§0§l🌀 APOCALYPSE POWER FLOWS THROUGH YOU!");
                break;
        }
    }
    
    /**
     * Start wall climbing effect for spider heads
     */
    private void startWallClimbingEffect(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if still wearing spider head
                String currentHead = getCurrentlyWornHead(player);
                if (currentHead == null || (!currentHead.equals("spider") && !currentHead.equals("cave_spider"))) {
                    cancel();
                    return;
                }
                
                // Check if player is against a wall
                if (isAgainstWall(player)) {
                    // Apply climbing velocity
                    org.bukkit.util.Vector velocity = player.getVelocity();
                    velocity.setY(0.2); // Climbing speed
                    player.setVelocity(velocity);
                    
                    // Spider climbing particles
                    player.getWorld().spawnParticle(org.bukkit.Particle.CRIT_MAGIC, 
                        player.getLocation(), 2, 0.3, 0.3, 0.3, 0);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L); // Check every 5 ticks
        
        player.sendMessage("§8🕷️ Wall climbing enabled!");
    }
    
    /**
     * Start enderman effects
     */
    private void startEndermanEffects(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if still wearing enderman head
                String currentHead = getCurrentlyWornHead(player);
                if (currentHead == null || !currentHead.equals("enderman")) {
                    cancel();
                    return;
                }
                
                // Check if in water (endermen take damage in water)
                if (player.getLocation().getBlock().getType() == org.bukkit.Material.WATER) {
                    player.damage(1.0);
                    player.sendMessage("§5💧 Water damages you while wearing Enderman head!");
                    
                    // Teleport away from water
                    org.bukkit.Location safeLoc = findSafeLocationAwayFromWater(player.getLocation());
                    if (safeLoc != null) {
                        player.teleport(safeLoc);
                        player.sendMessage("§5⚡ Emergency teleportation away from water!");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Check every second
    }
    
    /**
     * Start vibration detection for warden head
     */
    private void startVibrationDetection(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if still wearing warden head
                String currentHead = getCurrentlyWornHead(player);
                if (currentHead == null || !currentHead.equals("warden")) {
                    cancel();
                    return;
                }
                
                // Detect nearby entities with vibrations
                for (org.bukkit.entity.Entity entity : player.getNearbyEntities(20, 20, 20)) {
                    if (entity instanceof org.bukkit.entity.LivingEntity && 
                        entity != player && 
                        entity.getVelocity().length() > 0.1) {
                        
                        // Add glowing effect to moving entities
                        ((org.bukkit.entity.LivingEntity) entity).addPotionEffect(
                            new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.GLOWING, 60, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Check every 2 seconds
    }
    
    /**
     * Deactivate head abilities when removed
     */
    private void deactivateHeadAbilities(Player player, String headKey) {
        if (headKey == null) return;
        
        HeadData headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null) return;
        
        // Remove passive effects
        removePassiveEffects(player, headKey);
        
        // Send unequip message
        String headName = ChatColor.translateAlternateColorCodes('&', headData.getDisplayName());
        player.sendMessage("§7" + headName + " §7unequipped - abilities disabled");
        
        plugin.getPluginLogger().debug("Deactivated abilities for " + headKey + " removed by " + player.getName());
    }
    
    /**
     * Remove passive effects when head is unequipped
     */
    private void removePassiveEffects(Player player, String headKey) {
        switch (headKey.toLowerCase()) {
            case "blaze":
            case "magma_cube":
            case "strider":
                // Remove fire resistance
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
                player.sendMessage("§7Fire immunity removed");
                break;
                
            case "dolphin":
            case "axolotl":
                // Remove aquatic effects
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE);
                player.sendMessage("§7Aquatic abilities removed");
                break;
                
            case "horse":
            case "pig":
                // Remove speed boost
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.SPEED);
                player.sendMessage("§7Speed boost removed");
                break;
                
            case "chicken":
                // Remove slow falling
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING);
                player.sendMessage("§7Slow falling removed");
                break;
                
            case "bat":
                // Remove night vision
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
                player.sendMessage("§7Night vision removed");
                break;
                
            case "rabbit":
                // Remove jump boost
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP);
                player.sendMessage("§7Jump boost removed");
                break;
                
            case "ender_dragon":
                // Remove flight
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("§7Dragon flight disabled");
                break;
                
            case "wither":
                // Remove resistance
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE);
                player.sendMessage("§7Wither resistance removed");
                break;
                
            case "apocalypse_head":
                // Remove all apocalypse effects
                player.setAllowFlight(false);
                player.setFlying(false);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.DAMAGE_RESISTANCE);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE);
                player.sendMessage("§7Apocalypse power fades...");
                break;
        }
    }
    
    /**
     * Play helmet equip effects
     */
    private void playHelmetEquipEffects(Player player, String headKey) {
        org.bukkit.Location location = player.getLocation();
        
        // Visual effects based on head type
        switch (headKey.toLowerCase()) {
            case "warden":
            case "ender_dragon":
            case "wither":
            case "apocalypse_head":
                // Boss head effects
                location.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTMENT_TABLE, 
                    location.add(0, 2, 0), 30, 1.0, 1.0, 1.0, 0.5);
                location.getWorld().playSound(location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                break;
                
            default:
                // Regular head effects
                location.getWorld().spawnParticle(org.bukkit.Particle.VILLAGER_HAPPY, 
                    location.add(0, 2, 0), 10, 0.8, 0.8, 0.8, 0.1);
                location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                break;
        }
    }
    
    /**
     * Handle inventory clicks that might change helmet slot
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if helmet slot was affected
        boolean helmetSlotAffected = false;
        
        // Direct helmet slot click
        if (event.getSlot() == 39 && event.getSlotType() == InventoryType.SlotType.ARMOR) {
            helmetSlotAffected = true;
        }
        
        // Shift-click that might move items to helmet slot
        if (event.isShiftClick() && event.getCurrentItem() != null) {
            String headKey = plugin.getHeadManager().getHeadKey(event.getCurrentItem());
            if (headKey != null) {
                helmetSlotAffected = true;
            }
        }
        
        // Armor slot click
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            helmetSlotAffected = true;
        }
        
        if (helmetSlotAffected) {
            // Schedule helmet check for next tick
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                checkPlayerHelmetSlot(player);
            }, 1L);
        }
    }
    
    /**
     * Handle player join - check helmet slot
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Schedule helmet check after player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            checkPlayerHelmetSlot(player);
        }, 40L); // 2 second delay
    }
    
    /**
     * Handle player quit - cleanup tracking
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Clean up tracking data
        currentlyWornHeads.remove(player.getUniqueId());
        lastHelmetCheck.remove(player.getUniqueId());
        
        plugin.getPluginLogger().debug("Cleaned up helmet tracking for " + player.getName());
    }
    
    /**
     * Get currently worn head key for player
     */
    public String getCurrentlyWornHead(Player player) {
        return currentlyWornHeads.get(player.getUniqueId());
    }
    
    /**
     * Check if player is wearing any head
     */
    public boolean isWearingHead(Player player) {
        return getCurrentlyWornHead(player) != null;
    }
    
    /**
     * Check if player is wearing specific head
     */
    public boolean isWearingHead(Player player, String headKey) {
        String currentHead = getCurrentlyWornHead(player);
        return currentHead != null && currentHead.equals(headKey);
    }
    
    /**
     * Force check player's helmet slot
     */
    public void forceCheckHelmet(Player player) {
        checkPlayerHelmetSlot(player);
    }
    
    /**
     * Utility methods
     */
    private boolean isAgainstWall(Player player) {
        org.bukkit.Location loc = player.getLocation();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue;
                if (loc.clone().add(x, 0, z).getBlock().getType().isSolid()) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private org.bukkit.Location findSafeLocationAwayFromWater(org.bukkit.Location center) {
        for (int attempts = 0; attempts < 10; attempts++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 3.0 + Math.random() * 5.0;
            
            double x = center.getX() + Math.cos(angle) * distance;
            double z = center.getZ() + Math.sin(angle) * distance;
            
            org.bukkit.Location testLoc = new org.bukkit.Location(center.getWorld(), x, center.getY(), z);
            
            if (testLoc.getBlock().getType() != org.bukkit.Material.WATER && 
                testLoc.getBlock().getType().isTransparent()) {
                return testLoc;
            }
        }
        return null;
    }
    
    /**
     * Get helmet slot monitoring statistics
     */
    public Map<String, Object> getMonitoringStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("tracked_players", currentlyWornHeads.size());
        stats.put("active_heads", currentlyWornHeads.values().size());
        
        // Count head types
        Map<String, Integer> headCounts = new HashMap<>();
        for (String headKey : currentlyWornHeads.values()) {
            headCounts.put(headKey, headCounts.getOrDefault(headKey, 0) + 1);
        }
        stats.put("head_distribution", headCounts);
        
        return stats;
    }
    
    /**
     * Cleanup helmet slot manager
     */
    public void cleanup() {
        // Remove all passive effects from players
        for (Map.Entry<UUID, String> entry : currentlyWornHeads.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                deactivateHeadAbilities(player, entry.getValue());
            }
        }
        
        currentlyWornHeads.clear();
        lastHelmetCheck.clear();
    }
}