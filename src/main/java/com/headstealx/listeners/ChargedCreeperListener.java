package com.headstealx.listeners;

import com.headstealx.Main;
import com.headstealx.util.VersionUtil;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles charged creeper kills and head drops
 * Core mechanic: When a charged creeper kills a mob, drop that mob's head
 */
public class ChargedCreeperListener implements Listener {
    
    private final Main plugin;
    
    public ChargedCreeperListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle entity death by charged creeper
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if head drops are enabled
        if (!plugin.getConfig().getBoolean("general.head_drop.require_charged_creeper", true)) {
            return;
        }
        
        Entity deadEntity = event.getEntity();
        
        // Get the last damage cause
        if (!(deadEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
            return;
        }
        
        EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) deadEntity.getLastDamageCause();
        Entity damager = damageEvent.getDamager();
        
        // Check if killed by a creeper
        if (!(damager instanceof Creeper)) {
            return;
        }
        
        Creeper creeper = (Creeper) damager;
        
        // Check if creeper is powered/charged
        if (!creeper.isPowered()) {
            plugin.getPluginLogger().debug("Creeper was not charged, no head drop");
            return;
        }
        
        // Check world restrictions
        if (!isWorldEnabled(deadEntity)) {
            plugin.getPluginLogger().debug("Head drops disabled in world: " + deadEntity.getWorld().getName());
            return;
        }
        
        // Check drop chance
        int dropChance = plugin.getConfig().getInt("general.head_drop.drop_chance_percent", 100);
        if (dropChance < 100 && Math.random() * 100 > dropChance) {
            plugin.getPluginLogger().debug("Head drop failed random chance (" + dropChance + "%)");
            return;
        }
        
        // Get head for the killed entity
        ItemStack head = plugin.getHeadManager().getHeadForEntity(deadEntity);
        if (head == null) {
            plugin.getPluginLogger().debug("No head configured for entity type: " + deadEntity.getType());
            return;
        }
        
        // Replace default drops with head
        event.getDrops().clear();
        event.setDroppedExp(0); // Clear experience too
        
        // Drop the head
        deadEntity.getWorld().dropItemNaturally(deadEntity.getLocation(), head);
        
        plugin.getPluginLogger().info("Charged creeper killed " + deadEntity.getType() + 
            ", dropped " + plugin.getHeadManager().getHeadKey(head) + " head");
        
        // Optional: Broadcast head drop
        if (plugin.getConfig().getBoolean("general.head_drop.broadcast_drops", false)) {
            String headName = head.getItemMeta() != null ? head.getItemMeta().getDisplayName() : "Unknown Head";
            plugin.getServer().broadcastMessage("§6A charged creeper killed a " + 
                deadEntity.getType().name().toLowerCase().replace("_", " ") + 
                " and dropped " + headName + "!");
        }
        
        // Play effects at drop location
        playDropEffects(deadEntity);
    }
    
    /**
     * Check if head drops are enabled in this world
     */
    private boolean isWorldEnabled(Entity entity) {
        String worldName = entity.getWorld().getName();
        
        // Check disabled worlds
        if (plugin.getConfig().getStringList("general.head_drop.disabled_worlds").contains(worldName)) {
            return false;
        }
        
        // Check enabled worlds (if list is not empty, only those worlds are enabled)
        java.util.List<String> enabledWorlds = plugin.getConfig().getStringList("general.head_drop.enabled_worlds");
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(worldName)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Play visual and audio effects at head drop location
     */
    private void playDropEffects(Entity deadEntity) {
        if (!plugin.getConfig().getBoolean("abilities.particles", true)) {
            return;
        }
        
        // Spawn particles
        if (VersionUtil.isAtLeast(1, 9)) {
            deadEntity.getWorld().spawnParticle(
                org.bukkit.Particle.VILLAGER_HAPPY,
                deadEntity.getLocation().add(0, 1, 0),
                10, 0.5, 0.5, 0.5, 0.1
            );
            
            deadEntity.getWorld().spawnParticle(
                org.bukkit.Particle.ENCHANTMENT_TABLE,
                deadEntity.getLocation().add(0, 1, 0),
                20, 1.0, 1.0, 1.0, 0.1
            );
        }
        
        // Play sound
        if (plugin.getConfig().getBoolean("abilities.sounds", true)) {
            if (VersionUtil.isAtLeast(1, 9)) {
                deadEntity.getWorld().playSound(
                    deadEntity.getLocation(),
                    org.bukkit.Sound.ENTITY_PLAYER_LEVELUP,
                    1.0f, 1.5f
                );
            } else {
                // Legacy sound for older versions
                try {
                    org.bukkit.Sound legacySound = org.bukkit.Sound.valueOf("LEVEL_UP");
                    deadEntity.getWorld().playSound(deadEntity.getLocation(), legacySound, 1.0f, 1.5f);
                } catch (IllegalArgumentException e) {
                    // Sound not available in this version
                }
            }
        }
    }
    
    /**
     * Handle creeper explosions to prevent accidental head drops
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreeperExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
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
        
        plugin.getPluginLogger().debug("Charged creeper exploded at " + 
            creeper.getLocation().getBlockX() + "," + 
            creeper.getLocation().getBlockY() + "," + 
            creeper.getLocation().getBlockZ());
        
        // The EntityDeathEvent will handle head drops for any entities killed by this explosion
        // This event is mainly for logging and potential future features
    }
    
    /**
     * Handle lightning strikes that might charge creepers
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLightningStrike(org.bukkit.event.weather.LightningStrikeEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check for creepers near lightning strike
        for (Entity entity : event.getLightning().getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper) entity;
                if (creeper.isPowered()) {
                    plugin.getPluginLogger().debug("Lightning charged creeper at " + 
                        creeper.getLocation().getBlockX() + "," + 
                        creeper.getLocation().getBlockY() + "," + 
                        creeper.getLocation().getBlockZ());
                }
            }
        }
    }
    
    /**
     * Handle trident channeling that might charge creepers (1.13+)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTridentLightning(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (!plugin.isPluginReady() || !VersionUtil.isAtLeast(1, 13)) {
            return;
        }
        
        // Check if damage was caused by trident with channeling
        if (event.getDamager().getType().name().equals("TRIDENT")) {
            // Check for nearby creepers that might get charged
            for (Entity entity : event.getEntity().getNearbyEntities(5, 5, 5)) {
                if (entity instanceof Creeper) {
                    Creeper creeper = (Creeper) entity;
                    // Schedule a check for the next tick to see if creeper got charged
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (creeper.isPowered()) {
                            plugin.getPluginLogger().debug("Trident channeling charged creeper");
                        }
                    }, 1L);
                }
            }
        }
    }
}