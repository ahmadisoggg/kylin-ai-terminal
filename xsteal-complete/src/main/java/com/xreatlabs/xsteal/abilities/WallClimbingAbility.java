package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Wall Climbing Ability - Spider Head
 * Grants wall-climbing and web shooting abilities
 * Based on PSD1's HeadSteal video mechanics
 */
public class WallClimbingAbility implements Ability {
    
    @Override
    public String getName() {
        return "wall_climbing";
    }
    
    @Override
    public String getDescription() {
        return "Climb walls and shoot webs like a spider";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        double climbSpeed = context.getDouble("climb_speed", 0.2);
        boolean webShooting = context.getBoolean("web_shooting", true);
        double webRange = context.getDouble("web_range", 10.0);
        int webDuration = context.getInt("web_duration", 30);
        boolean fallDamageImmune = context.getBoolean("fall_damage_immune", true);
        
        // Check if this is a web shooting activation
        if (webShooting && player.isSneaking()) {
            return shootWeb(player, context, webRange, webDuration);
        }
        
        // Activate wall climbing mode
        activateWallClimbing(player, context, climbSpeed, fallDamageImmune);
        
        return true;
    }
    
    /**
     * Activate wall climbing mode
     */
    private void activateWallClimbing(Player player, AbilityContext context, double climbSpeed, boolean fallDamageImmune) {
        player.sendMessage("§8§l🕷 Spider Climbing Activated!");
        
        // Give beneficial effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1200, 2)); // 1 minute
        if (fallDamageImmune) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 1200, 0));
        }
        
        // Start wall climbing task
        new BukkitRunnable() {
            int duration = 60; // 60 seconds
            
            @Override
            public void run() {
                if (!player.isOnline() || duration <= 0) {
                    player.sendMessage("§7Spider climbing ended.");
                    cancel();
                    return;
                }
                
                // Check if player is against a wall
                if (isAgainstWall(player)) {
                    // Apply climbing velocity
                    Vector velocity = player.getVelocity();
                    velocity.setY(climbSpeed);
                    player.setVelocity(velocity);
                    
                    // Spawn spider particles
                    player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 2);
                }
                
                duration--;
            }
        }.runTaskTimer(context.getPlugin(), 0L, 20L);
    }
    
    /**
     * Shoot web at target location
     */
    private boolean shootWeb(Player player, AbilityContext context, double range, int duration) {
        Location targetLocation = context.getTargetLocation(range);
        
        if (targetLocation == null) {
            player.sendMessage("§cNo valid target for web!");
            return false;
        }
        
        // Place cobweb at target location
        Block targetBlock = targetLocation.getBlock();
        Material originalMaterial = targetBlock.getType();
        
        // Only place web in air or replaceable blocks
        if (targetBlock.getType() == Material.AIR || 
            !targetBlock.getType().isSolid()) {
            
            targetBlock.setType(Material.COBWEB);
            
            // Schedule web removal
            context.getPlugin().getServer().getScheduler().runTaskLater(context.getPlugin(), () -> {
                if (targetBlock.getType() == Material.COBWEB) {
                    targetBlock.setType(originalMaterial);
                }
            }, duration * 20L);
            
            // Apply slowness to entities in web
            for (Entity entity : targetLocation.getWorld().getNearbyEntities(targetLocation, 2, 2, 2)) {
                if (entity instanceof LivingEntity && entity != player) {
                    LivingEntity target = (LivingEntity) entity;
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 3));
                }
            }
            
            // Web shooting effect
            createWebShootingEffect(player.getEyeLocation(), targetLocation);
            
            player.sendMessage("§8§lWeb shot! Target trapped for " + duration + " seconds.");
            return true;
        } else {
            player.sendMessage("§cCannot place web there!");
            return false;
        }
    }
    
    /**
     * Check if player is against a wall
     */
    private boolean isAgainstWall(Player player) {
        Location playerLocation = player.getLocation();
        
        // Check blocks around player
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) continue; // Skip center
                
                Block block = playerLocation.clone().add(x, 0, z).getBlock();
                if (block.getType().isSolid()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Create web shooting visual effect
     */
    private void createWebShootingEffect(Location start, Location end) {
        Vector direction = end.subtract(start).toVector().normalize();
        double distance = start.distance(end);
        
        // Create web trail
        for (double i = 0; i < distance; i += 0.5) {
            Location particleLocation = start.clone().add(direction.clone().multiply(i));
            start.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLocation, 1);
        }
        
        // Web impact effect
        end.getWorld().spawnParticle(Particle.BLOCK_CRACK, end, 10, 0.5, 0.5, 0.5, 0.1);
    }
    
    @Override
    public void playSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.2f);
        player.playSound(player.getLocation(), Sound.BLOCK_WOOL_PLACE, 0.8f, 0.6f);
    }
    
    @Override
    public void playParticles(Player player) {
        // Spider web particles around player
        for (int i = 0; i < 15; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = Math.random() * 2.0;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            
            Location particleLocation = player.getLocation().add(x, Math.random() * 2, z);
            player.getWorld().spawnParticle(Particle.CRIT_MAGIC, particleLocation, 1);
        }
    }
    
    @Override
    public boolean isPassive() {
        return true; // Wall climbing is always active when worn
    }
    
    @Override
    public double getRange() {
        return 15.0;
    }
}