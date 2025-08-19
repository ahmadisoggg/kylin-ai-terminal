package com.headstealx.abilities;

import org.bukkit.entity.Player;

/**
 * Base abstract class for abilities providing common functionality
 */
public abstract class BaseAbility implements Ability {
    
    protected final String name;
    protected final String description;
    
    public BaseAbility(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public void playSound(Player player) {
        // Default implementation - can be overridden
    }
    
    @Override
    public void playParticles(Player player) {
        // Default implementation - can be overridden
    }
    
    /**
     * Helper method to send ability feedback to player
     */
    protected void sendFeedback(Player player, String message) {
        player.sendMessage("§6[Ability] §f" + message);
    }
    
    /**
     * Helper method to send error message to player
     */
    protected void sendError(Player player, String message) {
        player.sendMessage("§c[Ability] " + message);
    }
}