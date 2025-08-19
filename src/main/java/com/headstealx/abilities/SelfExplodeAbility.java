package com.headstealx.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Self Explode Ability - Creeper Head
 * Creates an explosion centered on the player that damages nearby entities
 */
public class SelfExplodeAbility implements Ability {
    
    @Override
    public String getName() {
        return "self_explode";
    }
    
    @Override
    public String getDescription() {
        return "Creates a non-lethal explosion around you";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        double radius = context.getDouble("radius", 3.0);
        boolean blockDamage = context.getBoolean("blockDamage", false);
        double damage = context.getDouble("damage", 6.0);
        double knockback = context.getDouble("knockback", 2.0);
        
        Location explosionCenter = player.getLocation();
        
        // Get nearby entities
        List<Entity> nearbyEntities = context.getNearbyEntities(radius);
        
        // Damage and knockback entities
        for (Entity entity : nearbyEntities) {
            if (entity == player) continue; // Don't damage self
            
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
                knockbackVector.normalize();
                knockbackVector.multiply(knockback * damageFactor);
                knockbackVector.setY(Math.max(0.3, knockbackVector.getY())); // Ensure upward knockback
                
                entity.setVelocity(knockbackVector);
            }
        }
        
        // Create visual explosion effect
        if (context.shouldUseParticles()) {
            createExplosionEffect(explosionCenter, radius);
        }
        
        // Create explosion sound
        if (context.shouldUseSounds()) {
            playSound(player);
        }
        
        // Optional block damage (usually disabled for balance)
        if (blockDamage) {
            player.getWorld().createExplosion(explosionCenter, (float) (radius / 2.0), false, false);
        }
        
        // Send feedback
        player.sendMessage("§c💥 Explosive blast!");
        
        context.debug("Self Explode: radius=" + radius + ", affected=" + nearbyEntities.size() + " entities");
        
        return true;
    }
    
    /**
     * Create visual explosion effect with particles
     */
    private void createExplosionEffect(Location center, double radius) {
        // Main explosion particles
        center.getWorld().spawnParticle(
            Particle.EXPLOSION_LARGE,
            center,
            3, 0.5, 0.5, 0.5, 0
        );
        
        // Additional explosion particles
        center.getWorld().spawnParticle(
            Particle.EXPLOSION_NORMAL,
            center,
            20, radius / 2, radius / 2, radius / 2, 0.1
        );
        
        // Smoke particles
        center.getWorld().spawnParticle(
            Particle.SMOKE_LARGE,
            center,
            15, radius / 3, radius / 3, radius / 3, 0.05
        );
        
        // Fire particles for effect
        center.getWorld().spawnParticle(
            Particle.FLAME,
            center,
            30, radius / 2, radius / 4, radius / 2, 0.1
        );
    }
    
    @Override
    public void playSound(Player player) {
        Location location = player.getLocation();
        
        // Main explosion sound
        player.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        
        // Additional creeper hiss sound for theme
        player.getWorld().playSound(location, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.5f);
    }
    
    @Override
    public void playParticles(Player player) {
        createExplosionEffect(player.getLocation(), 3.0);
    }
    
    @Override
    public boolean requiresTarget() {
        return false;
    }
    
    @Override
    public double getRange() {
        return 5.0; // Max explosion radius
    }
    
    @Override
    public int getCooldown() {
        return 8; // 8 second cooldown
    }
    
    @Override
    public boolean canUseInCombat() {
        return true;
    }
}