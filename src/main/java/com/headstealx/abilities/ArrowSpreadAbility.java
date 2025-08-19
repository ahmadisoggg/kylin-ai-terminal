package com.headstealx.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Arrow Spread Ability - Skeleton Head
 * Fires multiple arrows in a spread pattern
 */
public class ArrowSpreadAbility implements Ability {
    
    @Override
    public String getName() {
        return "arrow_spread";
    }
    
    @Override
    public String getDescription() {
        return "Fires multiple arrows in a spread pattern";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        int arrowCount = context.getInt("count", 5);
        double spreadAngle = context.getDouble("angle", 30.0);
        double velocity = context.getDouble("velocity", 2.0);
        boolean ignoreGravity = context.getBoolean("gravity", false);
        
        // Limit arrow count for performance
        arrowCount = Math.min(arrowCount, 15);
        
        // Get spread directions
        Vector[] directions = context.createSpread(arrowCount, spreadAngle);
        
        Location shootLocation = context.getEyeLocation();
        
        // Fire arrows
        for (Vector direction : directions) {
            Arrow arrow = player.getWorld().spawn(shootLocation, Arrow.class);
            arrow.setShooter(player);
            arrow.setVelocity(direction.multiply(velocity));
            
            // Modify arrow properties
            if (ignoreGravity) {
                arrow.setGravity(false);
            }
            
            // Set arrow damage
            arrow.setDamage(4.0);
            
            // Make arrows slightly more accurate
            arrow.setCritical(true);
            
            // Set pickup status (can be picked up by shooter)
            arrow.setPickupStatus(Arrow.PickupStatus.CREATIVE_ONLY);
        }
        
        // Visual and audio effects
        if (context.shouldUseParticles()) {
            spawnArrowParticles(player, directions);
        }
        
        if (context.shouldUseSounds()) {
            playSound(player);
        }
        
        // Send feedback
        player.sendMessage("§6Fired " + arrowCount + " arrows!");
        
        context.debug("Arrow Spread: fired " + arrowCount + " arrows with " + spreadAngle + "° spread");
        
        return true;
    }
    
    /**
     * Spawn particles along arrow trajectories
     */
    private void spawnArrowParticles(Player player, Vector[] directions) {
        Location shootLocation = player.getEyeLocation();
        
        for (Vector direction : directions) {
            // Spawn particles along the initial trajectory
            for (int i = 1; i <= 5; i++) {
                Location particleLocation = shootLocation.clone().add(direction.clone().multiply(i));
                
                player.getWorld().spawnParticle(
                    Particle.CRIT,
                    particleLocation,
                    2, 0.1, 0.1, 0.1, 0
                );
            }
        }
    }
    
    @Override
    public void playSound(Player player) {
        // Play bow shoot sound multiple times with slight delay for effect
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        
        // Schedule additional sounds for multi-shot effect
        player.getServer().getScheduler().runTaskLater(
            player.getServer().getPluginManager().getPlugin("HeadStealX"),
            () -> player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.1f),
            2L
        );
        
        player.getServer().getScheduler().runTaskLater(
            player.getServer().getPluginManager().getPlugin("HeadStealX"),
            () -> player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 0.6f, 1.2f),
            4L
        );
    }
    
    @Override
    public void playParticles(Player player) {
        // Particles are handled in the main execute method
    }
    
    @Override
    public boolean requiresTarget() {
        return false;
    }
    
    @Override
    public double getRange() {
        return 50.0;
    }
    
    @Override
    public int getCooldown() {
        return 3; // 3 second cooldown
    }
}