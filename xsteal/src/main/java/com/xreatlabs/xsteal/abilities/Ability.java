package com.xreatlabs.xsteal.abilities;

import org.bukkit.entity.Player;

/**
 * Base interface for all XSteal abilities
 * Each ability represents a unique power activated by wearing mob heads
 */
public interface Ability {
    
    /**
     * Execute the ability
     * 
     * @param context The ability execution context
     * @return true if the ability was successfully executed
     */
    boolean execute(AbilityContext context);
    
    /**
     * Get the ability name/identifier
     */
    String getName();
    
    /**
     * Get the ability description
     */
    String getDescription();
    
    /**
     * Check if the ability can be executed in the given context
     * 
     * @param context The ability execution context
     * @return true if the ability can be executed
     */
    default boolean canExecute(AbilityContext context) {
        return true;
    }
    
    /**
     * Play sound effects for the ability
     * 
     * @param player The player to play sounds for
     */
    default void playSound(Player player) {
        // Default implementation - abilities can override
    }
    
    /**
     * Play particle effects for the ability
     * 
     * @param player The player to play particles for
     */
    default void playParticles(Player player) {
        // Default implementation - abilities can override
    }
    
    /**
     * Check if this is a passive ability (always active when worn)
     */
    default boolean isPassive() {
        return false;
    }
    
    /**
     * Check if this ability requires the helmet slot
     */
    default boolean requiresHelmetSlot() {
        return true;
    }
    
    /**
     * Get the maximum range for this ability
     * Returns -1 if no range limit
     */
    default double getRange() {
        return -1;
    }
    
    /**
     * Cleanup method called when the ability effect ends
     */
    default void cleanup(AbilityContext context) {
        // Default implementation - abilities can override
    }
}