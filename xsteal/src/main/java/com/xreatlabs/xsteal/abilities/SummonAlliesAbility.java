package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Summon Allies Ability - Zombie Head
 * Summons 3 allied zombies that fight for the player
 * Based on PSD1's HeadSteal video mechanics
 */
public class SummonAlliesAbility implements Ability {
    
    @Override
    public String getName() {
        return "summon_allies";
    }
    
    @Override
    public String getDescription() {
        return "Summons allied zombies to fight for you";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters from configuration
        String entityTypeStr = context.getString("entity_type", "ZOMBIE");
        int count = context.getInt("count", 3);
        int duration = context.getInt("duration", 60);
        double health = context.getDouble("health", 20.0);
        boolean friendly = context.getBoolean("friendly", true);
        boolean followOwner = context.getBoolean("follow_owner", true);
        
        // Parse entity type
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityTypeStr);
        } catch (IllegalArgumentException e) {
            context.debug("Invalid entity type: " + entityTypeStr);
            return false;
        }
        
        // Spawn allies around the player
        Location playerLocation = player.getLocation();
        int spawned = 0;
        
        for (int i = 0; i < count; i++) {
            // Calculate spawn location around player
            double angle = (2 * Math.PI * i) / count;
            double x = playerLocation.getX() + Math.cos(angle) * 2.0;
            double z = playerLocation.getZ() + Math.sin(angle) * 2.0;
            
            Location spawnLocation = new Location(playerLocation.getWorld(), x, playerLocation.getY(), z);
            
            // Make sure location is safe
            if (!context.isSafeLocation(spawnLocation)) {
                spawnLocation = context.getTargetLocation(2.0);
            }
            
            try {
                // Spawn the entity
                Entity entity = playerLocation.getWorld().spawnEntity(spawnLocation, entityType);
                
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    
                    // Configure the ally
                    livingEntity.setMaxHealth(health);
                    livingEntity.setHealth(health);
                    
                    // Make it friendly (if it's a zombie)
                    if (entity instanceof Zombie && friendly) {
                        Zombie zombie = (Zombie) entity;
                        // Set custom name to indicate it's an ally
                        zombie.setCustomName("§a" + player.getName() + "'s Ally");
                        zombie.setCustomNameVisible(true);
                        
                        // Give it some beneficial effects
                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 1));
                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, duration * 20, 1));
                    }
                    
                    // Add to summoned entities tracking
                    context.getPlugin().getAbilityManager().addSummonedEntity(player, entity);
                    
                    // Schedule removal after duration
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity.isValid()) {
                                // Play disappear effect
                                entity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                                    entity.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                                entity.remove();
                            }
                        }
                    }.runTaskLater(context.getPlugin(), duration * 20L);
                    
                    spawned++;
                }
                
            } catch (Exception e) {
                context.debug("Failed to spawn ally: " + e.getMessage());
            }
        }
        
        if (spawned > 0) {
            player.sendMessage("§a§lSummoned " + spawned + " " + entityTypeStr.toLowerCase() + " allies!");
            context.debug("Summoned " + spawned + " allies for " + duration + " seconds");
            return true;
        } else {
            player.sendMessage("§cFailed to summon allies!");
            return false;
        }
    }
    
    @Override
    public void playSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
    }
    
    @Override
    public void playParticles(Player player) {
        Location location = player.getLocation();
        
        // Spawn summoning circle particles
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI * i) / 20;
            double x = location.getX() + Math.cos(angle) * 3.0;
            double z = location.getZ() + Math.sin(angle) * 3.0;
            
            Location particleLocation = new Location(location.getWorld(), x, location.getY() + 0.1, z);
            location.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 1);
        }
        
        // Central burst
        location.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, location.add(0, 1, 0), 5);
    }
    
    @Override
    public double getRange() {
        return 5.0;
    }
}