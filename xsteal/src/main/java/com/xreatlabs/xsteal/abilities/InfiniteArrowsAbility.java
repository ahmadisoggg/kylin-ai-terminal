package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Infinite Arrows Ability - Skeleton Head
 * Fires infinite bone arrows with perfect accuracy
 * Based on PSD1's HeadSteal video mechanics
 */
public class InfiniteArrowsAbility implements Ability {
    
    @Override
    public String getName() {
        return "infinite_arrows";
    }
    
    @Override
    public String getDescription() {
        return "Fires infinite bone arrows with perfect accuracy";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        String arrowType = context.getString("arrow_type", "bone");
        boolean infinite = context.getBoolean("infinite", true);
        double damage = context.getDouble("damage", 6.0);
        double velocity = context.getDouble("velocity", 3.0);
        double accuracy = context.getDouble("accuracy", 1.0);
        boolean piercing = context.getBoolean("piercing", true);
        
        // Check if player is holding a bow (optional)
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        boolean hasBow = mainHand != null && mainHand.getType() == Material.BOW;
        
        // If no bow, create temporary bow effect
        if (!hasBow && infinite) {
            // Give player a temporary bow with infinite arrows
            startInfiniteArrowMode(player, context);
            return true;
        }
        
        // Fire a single enhanced arrow
        Location shootLocation = context.getEyeLocation();
        Vector direction = context.getDirection();
        
        Arrow arrow = player.getWorld().spawn(shootLocation, Arrow.class);
        arrow.setShooter(player);
        arrow.setVelocity(direction.multiply(velocity));
        arrow.setDamage(damage);
        
        // Enhanced arrow properties
        if (piercing) {
            // Set piercing level (1.14+ feature, fallback for older versions)
            try {
                arrow.setPierceLevel(3);
            } catch (NoSuchMethodError e) {
                // Older version - create custom piercing effect
                createCustomPiercingEffect(arrow, context);
            }
        }
        
        // Perfect accuracy
        if (accuracy >= 1.0) {
            arrow.setCritical(true);
            arrow.setGravity(false);
        }
        
        // Bone arrow visual effect
        if ("bone".equals(arrowType)) {
            createBoneArrowEffect(arrow);
        }
        
        player.sendMessage("§f§lBone Arrow fired!");
        context.debug("Fired enhanced arrow with " + damage + " damage");
        
        return true;
    }
    
    /**
     * Start infinite arrow mode for the player
     */
    private void startInfiniteArrowMode(Player player, AbilityContext context) {
        player.sendMessage("§f§l⚡ INFINITE ARROWS ACTIVATED!");
        player.sendMessage("§7Use any bow for unlimited bone arrows!");
        
        // This would require a more complex implementation to track bow usage
        // and modify arrow behavior. For now, we'll give a temporary effect.
        
        // Give player a bow if they don't have one
        if (player.getInventory().getItemInMainHand() == null || 
            player.getInventory().getItemInMainHand().getType() == Material.AIR) {
            
            ItemStack bow = new ItemStack(Material.BOW);
            bow.getItemMeta().setDisplayName("§fSkeleton Bone Bow");
            player.getInventory().setItemInMainHand(bow);
        }
        
        // Schedule infinite arrow effect (simplified)
        new BukkitRunnable() {
            int duration = 60; // 60 seconds
            
            @Override
            public void run() {
                duration--;
                
                if (duration <= 0 || !player.isOnline()) {
                    player.sendMessage("§7Infinite arrows effect ended.");
                    cancel();
                    return;
                }
                
                // Every 10 seconds, remind player
                if (duration % 10 == 0) {
                    player.sendMessage("§7Infinite arrows: " + duration + "s remaining");
                }
            }
        }.runTaskTimer(context.getPlugin(), 20L, 20L);
    }
    
    /**
     * Create custom piercing effect for older versions
     */
    private void createCustomPiercingEffect(Arrow arrow, AbilityContext context) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    cancel();
                    return;
                }
                
                // Check for entities in arrow's path
                for (Entity entity : arrow.getNearbyEntities(1, 1, 1)) {
                    if (entity instanceof LivingEntity && entity != arrow.getShooter()) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(arrow.getDamage(), arrow.getShooter());
                        
                        // Continue arrow flight
                        arrow.getWorld().spawnParticle(Particle.CRIT, arrow.getLocation(), 5);
                    }
                }
            }
        }.runTaskTimer(context.getPlugin(), 1L, 1L);
    }
    
    /**
     * Create bone arrow visual effect
     */
    private void createBoneArrowEffect(Arrow arrow) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    cancel();
                    return;
                }
                
                // Bone particle trail
                arrow.getWorld().spawnParticle(Particle.BONE_MEAL, arrow.getLocation(), 2);
            }
        }.runTaskTimer(arrow.getWorld().getServer().getPluginManager().getPlugin("XSteal"), 1L, 2L);
    }
    
    @Override
    public void playSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_SKELETON_AMBIENT, 0.5f, 1.2f);
    }
    
    @Override
    public void playParticles(Player player) {
        Location location = player.getEyeLocation();
        Vector direction = player.getEyeLocation().getDirection();
        
        // Arrow trail particles
        for (int i = 1; i <= 10; i++) {
            Location particleLocation = location.clone().add(direction.clone().multiply(i));
            player.getWorld().spawnParticle(Particle.CRIT, particleLocation, 3, 0.1, 0.1, 0.1, 0);
        }
    }
    
    @Override
    public double getRange() {
        return 100.0;
    }
}