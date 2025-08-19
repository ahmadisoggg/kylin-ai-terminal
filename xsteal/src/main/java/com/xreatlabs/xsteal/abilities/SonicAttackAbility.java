package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Sonic Attack Ability - Warden Head (Boss Ability 1)
 * Warden's sonic boom attack with penetrating damage
 * Activation: Left-Click
 */
public class SonicAttackAbility implements Ability {
    
    @Override
    public String getName() {
        return "sonic_attack";
    }
    
    @Override
    public String getDescription() {
        return "Warden's sonic boom that penetrates armor";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        double damage = context.getDouble("damage", 20.0);
        double range = context.getDouble("range", 15.0);
        boolean penetratesArmor = context.getBoolean("penetrates_armor", true);
        double knockback = context.getDouble("knockback", 5.0);
        boolean particleBeam = context.getBoolean("particle_beam", true);
        
        Location start = context.getEyeLocation();
        Vector direction = context.getDirection();
        
        // Create sonic beam effect
        if (particleBeam) {
            createSonicBeam(start, direction, range);
        }
        
        // Find all entities in the sonic beam path
        List<Entity> hitEntities = findEntitiesInBeam(start, direction, range);
        
        int entitiesHit = 0;
        for (Entity entity : hitEntities) {
            if (entity == player) continue;
            
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;
                
                // Apply sonic damage
                if (penetratesArmor) {
                    // Bypass armor by dealing direct damage
                    double currentHealth = target.getHealth();
                    double newHealth = Math.max(0, currentHealth - damage);
                    target.setHealth(newHealth);
                } else {
                    target.damage(damage, player);
                }
                
                // Apply knockback
                Vector knockbackVector = entity.getLocation().subtract(start).toVector();
                knockbackVector.normalize();
                knockbackVector.multiply(knockback);
                knockbackVector.setY(Math.max(0.3, knockbackVector.getY()));
                
                entity.setVelocity(knockbackVector);
                
                // Apply blindness effect (sonic disorientation)
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1)); // 5 seconds
                target.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60, 1)); // 3 seconds
                
                entitiesHit++;
            }
        }
        
        player.sendMessage("§0§l⚡ SONIC BOOM! Hit " + entitiesHit + " targets!");
        context.debug("Sonic attack hit " + entitiesHit + " entities");
        
        return entitiesHit > 0;
    }
    
    /**
     * Create visual sonic beam effect
     */
    private void createSonicBeam(Location start, Vector direction, double range) {
        new BukkitRunnable() {
            double distance = 0;
            
            @Override
            public void run() {
                if (distance >= range) {
                    cancel();
                    return;
                }
                
                Location particleLocation = start.clone().add(direction.clone().multiply(distance));
                
                // Main beam particles
                start.getWorld().spawnParticle(Particle.SONIC_BOOM, particleLocation, 1);
                start.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLocation, 2, 0.2, 0.2, 0.2, 0);
                
                // Additional effect particles
                start.getWorld().spawnParticle(Particle.CRIT, particleLocation, 3, 0.3, 0.3, 0.3, 0);
                
                distance += 0.5;
            }
        }.runTaskTimer(start.getWorld().getServer().getPluginManager().getPlugin("XSteal"), 0L, 1L);
    }
    
    /**
     * Find all entities in the sonic beam path
     */
    private List<Entity> findEntitiesInBeam(Location start, Vector direction, double range) {
        return start.getWorld().getNearbyEntities(start, range, range, range).stream()
            .filter(entity -> {
                if (!(entity instanceof LivingEntity)) return false;
                
                Vector toEntity = entity.getLocation().subtract(start).toVector();
                double distance = toEntity.length();
                
                if (distance > range) return false;
                
                toEntity.normalize();
                double dot = direction.dot(toEntity);
                
                // Entity is in beam if dot product > 0.9 (narrow beam)
                return dot > 0.9;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public void playSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ROAR, 1.0f, 0.8f);
    }
    
    @Override
    public void playParticles(Player player) {
        createSonicBeam(player.getEyeLocation(), player.getEyeLocation().getDirection(), 15.0);
    }
    
    @Override
    public double getRange() {
        return 15.0;
    }
}