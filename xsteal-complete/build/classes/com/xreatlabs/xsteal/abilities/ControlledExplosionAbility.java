package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Controlled Explosion Ability - Creeper Head
 * Creates an explosion on command without self-damage
 * Based on PSD1's HeadSteal video mechanics
 */
public class ControlledExplosionAbility implements Ability {
    
    @Override
    public String getName() {
        return "controlled_explosion";
    }
    
    @Override
    public String getDescription() {
        return "Explodes on command without harming yourself";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        double radius = context.getDouble("radius", 4.0);
        double damage = context.getDouble("damage", 12.0);
        boolean selfDamage = context.getBoolean("self_damage", false);
        boolean blockDamage = context.getBoolean("block_damage", false);
        double knockback = context.getDouble("knockback", 3.0);
        boolean fire = context.getBoolean("fire", false);
        
        Location explosionCenter = player.getLocation();
        
        // Get nearby entities
        List<Entity> nearbyEntities = context.getNearbyEntities(radius);
        
        // Apply damage and knockback to entities
        for (Entity entity : nearbyEntities) {
            if (entity == player && !selfDamage) {
                continue; // Skip self-damage if disabled
            }
            
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Calculate distance-based damage
                double distance = entity.getLocation().distance(explosionCenter);
                double damageFactor = Math.max(0, 1.0 - (distance / radius));
                double actualDamage = damage * damageFactor;
                
                // Apply damage
                if (actualDamage > 0) {
                    livingEntity.damage(actualDamage, player);
                }
                
                // Apply knockback
                Vector knockbackVector = entity.getLocation().subtract(explosionCenter).toVector();
                if (knockbackVector.length() > 0) {
                    knockbackVector.normalize();
                    knockbackVector.multiply(knockback * damageFactor);
                    knockbackVector.setY(Math.max(0.5, knockbackVector.getY())); // Ensure upward knockback
                    
                    entity.setVelocity(knockbackVector);
                }
                
                // Apply fire effect if enabled
                if (fire) {
                    entity.setFireTicks((int) (40 * damageFactor)); // 2 seconds max
                }
            }
        }
        
        // Create explosion effects
        createExplosionEffect(explosionCenter, radius);
        
        // Optional block damage
        if (blockDamage) {
            player.getWorld().createExplosion(explosionCenter, (float) (radius / 2.0), false, true);
        }
        
        player.sendMessage("§c§l💥 BOOM! Controlled explosion!");
        context.debug("Controlled explosion: radius=" + radius + ", affected=" + nearbyEntities.size());
        
        return true;
    }
    
    /**
     * Create visual explosion effect
     */
    private void createExplosionEffect(Location center, double radius) {
        // Main explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center, 3, 0.5, 0.5, 0.5, 0);
        
        // Secondary explosions
        center.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, center, 20, radius / 2, radius / 2, radius / 2, 0.1);
        
        // Smoke
        center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 15, radius / 3, radius / 3, radius / 3, 0.05);
        
        // Fire particles
        center.getWorld().spawnParticle(Particle.FLAME, center, 30, radius / 2, radius / 4, radius / 2, 0.1);
        
        // Creeper-themed green particles
        center.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, center, 10, radius, radius, radius, 0);
    }
    
    @Override
    public void playSound(Player player) {
        Location location = player.getLocation();
        
        // Creeper hiss before explosion
        player.getWorld().playSound(location, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.0f);
        
        // Explosion sound
        player.getServer().getScheduler().runTaskLater(
            player.getServer().getPluginManager().getPlugin("XSteal"),
            () -> player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f),
            10L // Half second delay
        );
    }
    
    @Override
    public void playParticles(Player player) {
        createExplosionEffect(player.getLocation(), 4.0);
    }
    
    @Override
    public double getRange() {
        return 6.0;
    }
}