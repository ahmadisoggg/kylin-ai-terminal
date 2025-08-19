package com.xreatlabs.xsteal.abilities;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.utils.VersionCompatibility;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Context object for ability execution
 * Contains player, parameters, and utility methods for abilities
 */
public class AbilityContext {
    
    private final Player player;
    private final Map<String, Object> parameters;
    private final XSteal plugin;
    
    // Cached values
    private Location targetLocation;
    private Entity targetEntity;
    
    public AbilityContext(Player player, Map<String, Object> parameters, XSteal plugin) {
        this.player = player;
        this.parameters = parameters;
        this.plugin = plugin;
    }
    
    // Basic getters
    
    public Player getPlayer() {
        return player;
    }
    
    public Map<String, Object> getParameters() {
        return parameters;
    }
    
    public XSteal getPlugin() {
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
            plugin.getPluginLogger().warning("Invalid parameter type for " + key);
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
    
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = parameters.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    public List<String> getStringList(String key, List<String> defaultValue) {
        return getParameter(key, defaultValue);
    }
    
    // Location and targeting utilities
    
    public Location getPlayerLocation() {
        return player.getLocation();
    }
    
    public Location getEyeLocation() {
        return player.getEyeLocation();
    }
    
    public Vector getDirection() {
        return player.getEyeLocation().getDirection();
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
        if (VersionCompatibility.isAtLeast(1, 13)) {
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
        
        if (VersionCompatibility.isAtLeast(1, 13)) {
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
            
            // Check if entity is in front of player
            if (dot > 0.7) {
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
            .collect(Collectors.toList());
    }
    
    /**
     * Get nearby hostile entities within range
     */
    public List<Entity> getNearbyHostileEntities(double range) {
        return getNearbyLivingEntities(range).stream()
            .filter(entity -> entity instanceof org.bukkit.entity.Monster)
            .collect(Collectors.toList());
    }
    
    /**
     * Get nearby players within range
     */
    public List<Player> getNearbyPlayers(double range) {
        return player.getWorld().getPlayers().stream()
            .filter(p -> p != player && p.getLocation().distance(player.getLocation()) <= range)
            .collect(Collectors.toList());
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
    
    /**
     * Check if location is safe for entity spawning
     */
    public boolean isSafeLocation(Location location) {
        if (location.getY() < 0 || location.getY() > 255) return false;
        
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
     * Send debug message to player if debug mode is enabled
     */
    public void debug(String message) {
        if (plugin.getConfigManager().isDebugMode()) {
            player.sendMessage("§7[XSteal Debug] " + message);
        }
    }
}