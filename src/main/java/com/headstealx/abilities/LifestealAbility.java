package com.headstealx.abilities;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Lifesteal Ability - Zombie Head
 * Heals the player for a percentage of damage dealt in melee combat
 */
public class LifestealAbility implements Ability {
    
    @Override
    public String getName() {
        return "lifesteal";
    }
    
    @Override
    public String getDescription() {
        return "Heals you for a percentage of melee damage dealt";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        double healPercent = context.getDouble("healPercent", 0.25);
        double range = context.getDouble("range", 3.0);
        
        // Find target entity in range
        Entity targetEntity = context.getTargetEntity(range);
        
        if (targetEntity == null || !(targetEntity instanceof LivingEntity)) {
            player.sendMessage("§cNo valid target found!");
            return false;
        }
        
        LivingEntity target = (LivingEntity) targetEntity;
        
        // Calculate damage based on player's held item and strength
        double baseDamage = calculateBaseDamage(player);
        
        // Apply damage to target
        target.damage(baseDamage, player);
        
        // Calculate healing
        double healAmount = baseDamage * healPercent;
        
        // Heal player
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double newHealth = Math.min(maxHealth, currentHealth + healAmount);
        
        player.setHealth(newHealth);
        
        // Visual and audio feedback
        if (context.shouldUseParticles()) {
            spawnHealingParticles(player);
        }
        
        if (context.shouldUseSounds()) {
            playSound(player);
        }
        
        // Send feedback message
        player.sendMessage(String.format("§a+%.1f health from lifesteal!", healAmount));
        
        context.debug("Lifesteal: dealt " + baseDamage + " damage, healed " + healAmount);
        
        return true;
    }
    
    /**
     * Calculate base damage based on player's weapon and attributes
     */
    private double calculateBaseDamage(Player player) {
        double baseDamage = 4.0; // Base fist damage
        
        // Check held item for additional damage
        if (player.getInventory().getItemInMainHand() != null) {
            switch (player.getInventory().getItemInMainHand().getType()) {
                case WOODEN_SWORD:
                case GOLDEN_SWORD:
                    baseDamage = 4.0;
                    break;
                case STONE_SWORD:
                    baseDamage = 5.0;
                    break;
                case IRON_SWORD:
                    baseDamage = 6.0;
                    break;
                case DIAMOND_SWORD:
                    baseDamage = 7.0;
                    break;
                default:
                    if (player.getInventory().getItemInMainHand().getType().name().contains("NETHERITE_SWORD")) {
                        baseDamage = 8.0;
                    }
                    break;
            }
        }
        
        // Check for strength effect
        PotionEffect strength = player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE);
        if (strength != null) {
            baseDamage += (strength.getAmplifier() + 1) * 3.0;
        }
        
        return baseDamage;
    }
    
    /**
     * Spawn healing particles around the player
     */
    private void spawnHealingParticles(Player player) {
        for (int i = 0; i < 15; i++) {
            double offsetX = (Math.random() - 0.5) * 2.0;
            double offsetY = Math.random() * 2.0;
            double offsetZ = (Math.random() - 0.5) * 2.0;
            
            player.getWorld().spawnParticle(
                Particle.HEART,
                player.getLocation().add(offsetX, offsetY, offsetZ),
                1, 0, 0, 0, 0
            );
        }
    }
    
    @Override
    public void playSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT_SWEET_BERRY_BUSH, 1.0f, 1.5f);
    }
    
    @Override
    public void playParticles(Player player) {
        spawnHealingParticles(player);
    }
    
    @Override
    public boolean requiresTarget() {
        return true;
    }
    
    @Override
    public double getRange() {
        return 3.0;
    }
    
    @Override
    public boolean canUseInCombat() {
        return true;
    }
}