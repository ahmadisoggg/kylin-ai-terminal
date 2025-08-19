package com.headstealx.abilities;

import org.bukkit.entity.Player;

/**
 * Base interface for all HeadStealX abilities
 * Each ability represents a unique power that can be activated by players
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
     * Get the ability name/type
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
     * Get the cooldown for this ability in seconds
     * Returns 0 if no specific cooldown (uses global)
     */
    default int getCooldown() {
        return 0;
    }
    
    /**
     * Check if this ability requires a target
     */
    default boolean requiresTarget() {
        return false;
    }
    
    /**
     * Check if this ability can be used while sneaking
     */
    default boolean canUseWhileSneaking() {
        return true;
    }
    
    /**
     * Check if this ability can be used in combat
     */
    default boolean canUseInCombat() {
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
     * Cleanup method called when the ability is finished
     * Used for abilities that create temporary entities or effects
     */
    default void cleanup(AbilityContext context) {
        // Default implementation - abilities can override
    }
}