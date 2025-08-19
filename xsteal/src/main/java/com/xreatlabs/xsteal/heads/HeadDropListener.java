package com.xreatlabs.xsteal.heads;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles mob head drops from charged creeper kills
 * Core XSteal mechanic: Charged creepers drop custom mob heads
 */
public class HeadDropListener implements Listener {
    
    private final XSteal plugin;
    
    public HeadDropListener(XSteal plugin) {
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
        if (!plugin.getConfigManager().getMainConfig().getBoolean("general.head_drops.require_charged_creeper", true)) {
            return;
        }
        
        Entity deadEntity = event.getEntity();
        
        // Skip if it's a player (handled by BanBox system)
        if (deadEntity instanceof Player) {
            return;
        }
        
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
            plugin.getPluginLogger().debug("Creeper was not charged, no head drop for " + deadEntity.getType());
            return;
        }
        
        // Check world restrictions
        if (!isWorldEnabled(deadEntity)) {
            plugin.getPluginLogger().debug("Head drops disabled in world: " + deadEntity.getWorld().getName());
            return;
        }
        
        // Check drop chance
        int dropChance = plugin.getConfigManager().getMainConfig().getInt("general.head_drops.drop_chance", 100);
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
        
        // Replace default drops with head (survival-friendly)
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Drop the head
        deadEntity.getWorld().dropItemNaturally(deadEntity.getLocation(), head);
        
        plugin.getPluginLogger().info("⚡ Charged creeper killed " + deadEntity.getType() + 
            ", dropped " + plugin.getHeadManager().getHeadKey(head) + " head");
        
        // Play special effects
        playHeadDropEffects(deadEntity);
        
        // Broadcast head drop (if enabled)
        if (plugin.getConfigManager().getMainConfig().getBoolean("general.head_drops.broadcast", false)) {
            String headName = head.getItemMeta() != null ? head.getItemMeta().getDisplayName() : "Unknown Head";
            plugin.getServer().broadcastMessage("§6⚡ A charged creeper killed a " + 
                deadEntity.getType().name().toLowerCase().replace("_", " ") + 
                " and dropped " + headName + "!");
        }
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
     * Play visual and audio effects for head drops
     */
    private void playHeadDropEffects(Entity deadEntity) {
        if (!plugin.getConfigManager().areParticlesEnabled()) {
            return;
        }
        
        org.bukkit.Location location = deadEntity.getLocation().add(0, 1, 0);
        
        // Lightning-themed particles (charged creeper)
        deadEntity.getWorld().spawnParticle(
            org.bukkit.Particle.FIREWORKS_SPARK,
            location, 20, 1.0, 1.0, 1.0, 0.1
        );
        
        // Magic particles for head drop
        deadEntity.getWorld().spawnParticle(
            org.bukkit.Particle.ENCHANTMENT_TABLE,
            location, 30, 1.5, 1.5, 1.5, 0.1
        );
        
        // Success particles
        deadEntity.getWorld().spawnParticle(
            org.bukkit.Particle.VILLAGER_HAPPY,
            location, 15, 0.8, 0.8, 0.8, 0.1
        );
        
        // Play sound effects
        if (plugin.getConfigManager().areSoundsEnabled()) {
            deadEntity.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.5f, 1.2f);
            deadEntity.getWorld().playSound(location, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 2.0f);
        }
    }
    
    /**
     * Handle lightning strikes that charge creepers
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
                    plugin.getPluginLogger().info("⚡ Lightning charged creeper at " + 
                        creeper.getLocation().getBlockX() + "," + 
                        creeper.getLocation().getBlockY() + "," + 
                        creeper.getLocation().getBlockZ());
                    
                    // Add glowing effect to charged creeper for visibility
                    creeper.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.GLOWING, 6000, 0)); // 5 minutes
                }
            }
        }
    }
    
    /**
     * Handle trident channeling that might charge creepers (1.13+)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTridentChanneling(EntityDamageByEntityEvent event) {
        if (!plugin.isPluginReady()) {
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
                            plugin.getPluginLogger().info("⚡ Trident channeling charged creeper");
                            
                            // Add glowing effect
                            creeper.addPotionEffect(new org.bukkit.potion.PotionEffect(
                                org.bukkit.potion.PotionEffectType.GLOWING, 6000, 0));
                        }
                    }, 1L);
                }
            }
        }
    }
    
    /**
     * Handle creeper explosions for tracking
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
        
        plugin.getPluginLogger().info("💥 Charged creeper exploded at " + 
            creeper.getLocation().getBlockX() + "," + 
            creeper.getLocation().getBlockY() + "," + 
            creeper.getLocation().getBlockZ());
        
        // The EntityDeathEvent will handle head drops for any entities killed by this explosion
        // This event is mainly for logging and tracking charged creeper activity
    }
}