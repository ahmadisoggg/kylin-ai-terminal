package com.xreatlabs.xsteal.heads;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Vanilla-compatible charged creeper head drop listener
 * Implements proper Minecraft mechanics where charged creepers drop mob heads
 * Based on vanilla Minecraft behavior with XSteal enhancements
 */
public class VanillaChargedCreeperListener implements Listener {
    
    private final XSteal plugin;
    
    public VanillaChargedCreeperListener(XSteal plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle entity death by charged creeper (vanilla-compatible)
     * This follows the exact vanilla Minecraft logic for charged creeper head drops
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChargedCreeperKill(EntityDeathEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Entity killed = event.getEntity();
        
        // Skip if it's a player (handled by BanBox system separately)
        if (killed instanceof Player) {
            return;
        }
        
        // Check if the entity was killed by explosion
        EntityDamageEvent lastDamage = killed.getLastDamageCause();
        if (lastDamage == null) {
            return;
        }
        
        // Must be explosion damage
        if (lastDamage.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }
        
        // Must be caused by entity damage
        if (!(lastDamage instanceof EntityDamageByEntityEvent)) {
            return;
        }
        
        EntityDamageByEntityEvent entityDamage = (EntityDamageByEntityEvent) lastDamage;
        Entity damager = entityDamage.getDamager();
        
        // Must be killed by a creeper
        if (!(damager instanceof Creeper)) {
            return;
        }
        
        Creeper creeper = (Creeper) damager;
        
        // Must be a charged/powered creeper
        if (!creeper.isPowered()) {
            plugin.getPluginLogger().debug("Creeper was not charged, no head drop for " + killed.getType());
            return;
        }
        
        // Check world restrictions
        if (!isWorldEnabled(killed)) {
            plugin.getPluginLogger().debug("Head drops disabled in world: " + killed.getWorld().getName());
            return;
        }
        
        // Check if this mob type has a custom head
        ItemStack customHead = plugin.getHeadManager().getHeadForEntity(killed);
        if (customHead == null) {
            plugin.getPluginLogger().debug("No custom head configured for entity type: " + killed.getType());
            // Let vanilla behavior handle vanilla head drops (zombie, skeleton, etc.)
            return;
        }
        
        // Check drop chance
        int dropChance = plugin.getConfigManager().getMainConfig().getInt("general.head_drops.drop_chance", 100);
        if (dropChance < 100 && Math.random() * 100 > dropChance) {
            plugin.getPluginLogger().debug("Head drop failed random chance (" + dropChance + "%)");
            return;
        }
        
        // Handle vanilla vs custom head drops
        if (isVanillaHeadMob(killed.getType())) {
            // For vanilla head mobs (zombie, skeleton, wither skeleton, creeper, piglin)
            // Let vanilla drop occur but also add our custom head
            addCustomHeadDrop(event, customHead, killed);
        } else {
            // For non-vanilla mobs, replace drops with our custom head
            replaceWithCustomHead(event, customHead, killed);
        }
        
        plugin.getPluginLogger().info("⚡ Charged creeper killed " + killed.getType() + 
            ", dropped " + plugin.getHeadManager().getHeadKey(customHead) + " head");
        
        // Play enhanced effects for custom head drops
        playVanillaCompatibleEffects(killed);
    }
    
    /**
     * Check if this is a vanilla head-dropping mob
     * Vanilla Minecraft: zombie, skeleton, wither skeleton, creeper, piglin drop heads when killed by charged creeper
     */
    private boolean isVanillaHeadMob(EntityType entityType) {
        switch (entityType) {
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
                return true;
            case WITHER_SKELETON:
                return true;
            default:
                // Check for piglin (1.16+)
                try {
                    if (entityType == EntityType.valueOf("PIGLIN")) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    // Piglin not available in this version
                }
                return false;
        }
    }
    
    /**
     * Add custom head drop alongside vanilla head
     */
    private void addCustomHeadDrop(EntityDeathEvent event, ItemStack customHead, Entity killed) {
        // Let vanilla head drop naturally, but also add our enhanced head
        killed.getWorld().dropItemNaturally(killed.getLocation(), customHead);
        
        plugin.getPluginLogger().debug("Added custom head alongside vanilla head for " + killed.getType());
    }
    
    /**
     * Replace drops with custom head (for non-vanilla mobs)
     */
    private void replaceWithCustomHead(EntityDeathEvent event, ItemStack customHead, Entity killed) {
        // Clear vanilla drops and replace with custom head
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Drop our custom head
        killed.getWorld().dropItemNaturally(killed.getLocation(), customHead);
        
        plugin.getPluginLogger().debug("Replaced drops with custom head for " + killed.getType());
    }
    
    /**
     * Check if head drops are enabled in this world
     */
    private boolean isWorldEnabled(Entity entity) {
        String worldName = entity.getWorld().getName();
        
        // Check disabled worlds
        if (plugin.getConfigManager().getMainConfig().getStringList("general.head_drops.disabled_worlds").contains(worldName)) {
            return false;
        }
        
        // Check enabled worlds (if list is not empty, only those worlds are enabled)
        java.util.List<String> enabledWorlds = plugin.getConfigManager().getMainConfig().getStringList("general.head_drops.enabled_worlds");
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(worldName)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Play vanilla-compatible effects for head drops
     */
    private void playVanillaCompatibleEffects(Entity killed) {
        if (!plugin.getConfigManager().areParticlesEnabled()) {
            return;
        }
        
        org.bukkit.Location location = killed.getLocation().add(0, 1, 0);
        
        // Lightning-themed particles (charged creeper effect)
        killed.getWorld().spawnParticle(
            org.bukkit.Particle.FIREWORKS_SPARK,
            location, 15, 1.0, 1.0, 1.0, 0.1
        );
        
        // Vanilla-style success particles
        killed.getWorld().spawnParticle(
            org.bukkit.Particle.VILLAGER_HAPPY,
            location, 10, 0.8, 0.8, 0.8, 0.1
        );
        
        // Play vanilla-compatible sounds
        if (plugin.getConfigManager().areSoundsEnabled()) {
            killed.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            killed.getWorld().playSound(location, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 2.0f);
        }
    }
    
    /**
     * Handle natural lightning strikes that charge creepers
     * This maintains vanilla behavior while adding tracking
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLightningStrike(org.bukkit.event.weather.LightningStrikeEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check for creepers near lightning strike (vanilla behavior)
        for (Entity entity : event.getLightning().getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper) entity;
                
                // Schedule check for next tick (lightning charging happens after this event)
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (creeper.isValid() && creeper.isPowered()) {
                        plugin.getPluginLogger().info("⚡ Natural lightning charged creeper at " + 
                            creeper.getLocation().getBlockX() + "," + 
                            creeper.getLocation().getBlockY() + "," + 
                            creeper.getLocation().getBlockZ());
                        
                        // Add visual indicator that creeper is charged
                        creeper.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.GLOWING, 12000, 0)); // 10 minutes
                        
                        // Broadcast to nearby players
                        for (Entity nearbyEntity : creeper.getNearbyEntities(20, 20, 20)) {
                            if (nearbyEntity instanceof Player) {
                                Player player = (Player) nearbyEntity;
                                player.sendMessage("§e⚡ A creeper has been charged by lightning nearby!");
                                player.sendMessage("§7Use it to get mob heads!");
                            }
                        }
                    }
                }, 1L);
            }
        }
    }
    
    /**
     * Handle trident channeling that charges creepers (1.13+)
     * This maintains vanilla trident channeling behavior
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTridentChanneling(EntityDamageByEntityEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle trident damage (1.13+)
        if (!com.xreatlabs.xsteal.utils.ComprehensiveVersionSupport.isAtLeast(1, 13)) {
            return;
        }
        
        // Check if damage was caused by trident
        if (event.getDamager().getType().name().equals("TRIDENT")) {
            // Check for nearby creepers that might get charged by channeling
            for (Entity entity : event.getEntity().getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Creeper) {
                    Creeper creeper = (Creeper) entity;
                    
                    // Schedule check for channeling effect
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (creeper.isValid() && creeper.isPowered()) {
                            plugin.getPluginLogger().info("⚡ Trident channeling charged creeper");
                            
                            // Add visual indicator
                            creeper.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.GLOWING, 12000, 0));
                            
                            // Notify nearby players
                            if (event.getDamager() instanceof Player) {
                                Player player = (Player) event.getDamager();
                                player.sendMessage("§e⚡ Your trident channeling charged a creeper!");
                                player.sendMessage("§7Use it to get mob heads!");
                            }
                        }
                    }, 1L);
                }
            }
        }
    }
    
    /**
     * Track charged creeper explosions for analytics
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChargedCreeperExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if it's a charged creeper explosion
        if (!(event.getEntity() instanceof Creeper)) {
            return;
        }
        
        Creeper creeper = (Creeper) event.getEntity();
        if (!creeper.isPowered()) {
            return;
        }
        
        plugin.getPluginLogger().info("💥 Charged creeper exploded - potential head drops incoming");
        
        // Count potential head drops from this explosion
        int potentialHeads = 0;
        for (Entity entity : event.getEntity().getNearbyEntities(
                event.getYield(), event.getYield(), event.getYield())) {
            
            if (entity instanceof org.bukkit.entity.LivingEntity && !(entity instanceof Player)) {
                if (plugin.getHeadManager().getHeadForEntity(entity) != null) {
                    potentialHeads++;
                }
            }
        }
        
        if (potentialHeads > 0) {
            plugin.getPluginLogger().info("Explosion may drop " + potentialHeads + " custom heads");
        }
    }
}