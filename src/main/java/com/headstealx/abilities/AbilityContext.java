package com.headstealx.abilities;

import com.headstealx.Main;
import com.headstealx.util.VersionUtil;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;

/**
 * Context object passed to abilities containing all necessary information
 * for ability execution including player, parameters, and utility methods
 */
public class AbilityContext {
    
    private final Player player;
    private final Map<String, Object> parameters;
    private final Event triggerEvent;
    private final Main plugin;
    
    // Cached values
    private Location targetLocation;
    private Entity targetEntity;
    private List<Entity> nearbyEntities;
    
    public AbilityContext(Player player, Map<String, Object> parameters, Event triggerEvent, Main plugin) {
        this.player = player;
        this.parameters = parameters;
        this.triggerEvent = triggerEvent;
        this.plugin = plugin;
    }
    
    // Basic getters
    
    public Player getPlayer() {
        return player;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public Event getTriggerEvent() {
        return triggerEvent;
    }
    
    public Main getPlugin() {
        return plugin;
    }
    
    // Parameter utility methods
    
    public <T> T getParameter(String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            @SuppressWarnings("unchecked")
            T result = (T) value;
            return result;
        } catch (ClassCastException e) {
            plugin.getPluginLogger().warning("Invalid parameter type for " + key + ": expected " + 
                defaultValue.getClass().getSimpleName() + ", got " + value.getClass().getSimpleName());
            return defaultValue;
        }
    }
    
    public String getString(String key, String defaultValue) {
        return getParameter(key, defaultValue);
    }
    
    public int getInt(String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    public double getDouble(String key, double defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return defaultValue;
    }
    
    public float getFloat(String key, float defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public long getLong(String key, long defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }
    
    // Location and targeting utilities
    
    /**
     * Get the player's eye location
     */
    public Location getEyeLocation() {
        return player.getEyeLocation();
    }
    
    /**
     * Get the player's location
     */
    public Location getPlayerLocation() {
        return player.getLocation();
    }
    
    /**
     * Get the target location (where player is looking)
     */
    public Location getTargetLocation() {
        if (targetLocation == null) {
            targetLocation = calculateTargetLocation();
        }
        return targetLocation;
    }
    
    /**
     * Get the target location with a specific range
     */
    public Location getTargetLocation(double range) {
        Location eyeLocation = getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        return eyeLocation.add(direction.multiply(range));
    }
    
    /**
     * Calculate target location using ray tracing
     */
    private Location calculateTargetLocation() {
        Location eyeLocation = getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        
        // Use ray tracing if available (1.13+)
        if (VersionUtil.isAtLeast(1, 13)) {
            RayTraceResult result = player.getWorld().rayTraceBlocks(
                eyeLocation, direction, 50.0, 
                org.bukkit.FluidCollisionMode.NEVER, true);
            
            if (result != null && result.getHitPosition() != null) {
                return result.getHitPosition().toLocation(player.getWorld());
            }
        }
        
        // Fallback method for older versions
        return eyeLocation.add(direction.multiply(10.0));
    }
    
    /**
     * Get the target entity (what player is looking at)
     */
    public Entity getTargetEntity() {
        if (targetEntity == null) {
            targetEntity = calculateTargetEntity();
        }
        return targetEntity;
    }
    
    /**
     * Get the target entity with a specific range
     */
    public Entity getTargetEntity(double range) {
        Location eyeLocation = getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        
        if (VersionUtil.isAtLeast(1, 13)) {
            RayTraceResult result = player.getWorld().rayTraceEntities(
                eyeLocation, direction, range, 
                entity -> entity != player && entity instanceof LivingEntity);
            
            if (result != null) {
                return result.getHitEntity();
            }
        }
        
        // Fallback for older versions
        return findNearestEntityInDirection(range);
    }
    
    /**
     * Calculate target entity using ray tracing
     */
    private Entity calculateTargetEntity() {
        return getTargetEntity(50.0);
    }
    
    /**
     * Find nearest entity in player's direction (fallback method)
     */
    private Entity findNearestEntityInDirection(double range) {
        Location eyeLocation = getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();
        
        Entity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : player.getNearbyEntities(range, range, range)) {
            if (entity == player || !(entity instanceof LivingEntity)) {
                continue;
            }
            
            Vector toEntity = entity.getLocation().subtract(eyeLocation).toVector().normalize();
            double dot = direction.dot(toEntity);
            
            // Check if entity is in front of player (dot > 0.8 means within ~36 degrees)
            if (dot > 0.8) {
                double distance = eyeLocation.distance(entity.getLocation());
                if (distance < nearestDistance) {
                    nearest = entity;
                    nearestDistance = distance;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Get nearby entities within range
     */
    public List<Entity> getNearbyEntities(double range) {
        return player.getNearbyEntities(range, range, range);
    }
    
    /**
     * Get nearby living entities within range
     */
    public List<Entity> getNearbyLivingEntities(double range) {
        return player.getNearbyEntities(range, range, range).stream()
            .filter(entity -> entity instanceof LivingEntity && entity != player)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Get nearby hostile entities within range
     */
    public List<Entity> getNearbyHostileEntities(double range) {
        return getNearbyLivingEntities(range).stream()
            .filter(entity -> isHostileEntity(entity))
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Check if entity is hostile
     */
    private boolean isHostileEntity(Entity entity) {
        // This would check if the entity is a hostile mob
        // Implementation depends on the specific entity types
        return entity instanceof org.bukkit.entity.Monster;
    }
    
    /**
     * Get nearby players within range
     */
    public List<Player> getNearbyPlayers(double range) {
        return player.getWorld().getPlayers().stream()
            .filter(p -> p != player && p.getLocation().distance(player.getLocation()) <= range)
            .collect(java.util.stream.Collectors.toList());
    }
    
    // Direction and vector utilities
    
    /**
     * Get player's facing direction
     */
    public Vector getDirection() {
        return player.getEyeLocation().getDirection();
    }
    
    /**
     * Get direction to target location
     */
    public Vector getDirectionToTarget() {
        Location target = getTargetLocation();
        return target.subtract(getEyeLocation()).toVector().normalize();
    }
    
    /**
     * Get direction to specific location
     */
    public Vector getDirectionTo(Location location) {
        return location.subtract(getEyeLocation()).toVector().normalize();
    }
    
    /**
     * Create a spread of directions (for multi-projectile abilities)
     */
    public Vector[] createSpread(int count, double angle) {
        Vector baseDirection = getDirection();
        Vector[] directions = new Vector[count];
        
        if (count == 1) {
            directions[0] = baseDirection;
            return directions;
        }
        
        double angleStep = angle / (count - 1);
        double startAngle = -angle / 2.0;
        
        for (int i = 0; i < count; i++) {
            double currentAngle = startAngle + (i * angleStep);
            Vector direction = rotateVector(baseDirection, currentAngle);
            directions[i] = direction;
        }
        
        return directions;
    }
    
    /**
     * Rotate a vector by an angle (in degrees) around the Y axis
     */
    private Vector rotateVector(Vector vector, double angle) {
        double radians = Math.toRadians(angle);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        
        return new Vector(x, vector.getY(), z);
    }
    
    // Utility methods
    
    /**
     * Check if player is sneaking
     */
    public boolean isPlayerSneaking() {
        return player.isSneaking();
    }
    
    /**
     * Check if player is in combat
     */
    public boolean isPlayerInCombat() {
        // This would check if player is in combat
        // Could use NMS or a combat tracking system
        return false; // Placeholder
    }
    
    /**
     * Check if location is safe (no lava, void, etc.)
     */
    public boolean isSafeLocation(Location location) {
        if (location.getY() < 0) return false;
        
        org.bukkit.Material blockType = location.getBlock().getType();
        org.bukkit.Material belowType = location.clone().add(0, -1, 0).getBlock().getType();
        
        // Check for dangerous blocks
        if (blockType.name().contains("LAVA") || blockType.name().contains("FIRE")) {
            return false;
        }
        
        // Check if there's solid ground
        return belowType.isSolid();
    }
    
    /**
     * Get a safe location near the target location
     */
    public Location getSafeLocationNear(Location target, double radius) {
        for (int i = 0; i < 10; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = Math.random() * radius;
            
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            
            Location testLocation = new Location(target.getWorld(), x, target.getY(), z);
            
            // Find ground level
            for (int y = (int) target.getY() + 5; y > target.getY() - 10; y--) {
                testLocation.setY(y);
                if (testLocation.getBlock().getType().isSolid()) {
                    testLocation.setY(y + 1);
                    if (isSafeLocation(testLocation)) {
                        return testLocation;
                    }
                    break;
                }
            }
        }
        
        return target; // Fallback to original location
    }
    
    /**
     * Send debug message to player if debug mode is enabled
     */
    public void debug(String message) {
        if (plugin.getConfig().getBoolean("general.behavior.debug_mode", false)) {
            player.sendMessage("§7[DEBUG] " + message);
        }
    }
    
    /**
     * Check if the ability should use particles
     */
    public boolean shouldUseParticles() {
        return plugin.getConfig().getBoolean("abilities.particles", true) &&
               plugin.getConfig().getBoolean("performance.async_processing", true);
    }
    
    /**
     * Check if the ability should use sounds
     */
    public boolean shouldUseSounds() {
        return plugin.getConfig().getBoolean("abilities.sounds", true);
    }
    
    /**
     * Get the maximum particle count for abilities
     */
    public int getMaxParticles() {
        return plugin.getConfig().getInt("performance.max_particles", 50);
    }
}