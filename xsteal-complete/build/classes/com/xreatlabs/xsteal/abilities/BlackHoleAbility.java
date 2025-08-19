package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Black Hole Ability - Apocalypse Head
 * Creates a devastating black hole that pulls entities and deals void damage
 * The ultimate fusion ability combining Dragon and Wither power
 */
public class BlackHoleAbility implements Ability {
    
    @Override
    public String getName() {
        return "black_hole";
    }
    
    @Override
    public String getDescription() {
        return "Summons a devastating black hole that pulls entities into the void";
    }
    
    @Override
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Get parameters
        double radius = context.getDouble("radius", 15.0);
        int duration = context.getInt("duration", 5);
        double pullStrength = context.getDouble("pull_strength", 0.8);
        double damagePerTick = context.getDouble("damage_per_tick", 2.0);
        double voidDamage = context.getDouble("void_damage", 5.0);
        double launchForce = context.getDouble("launch_force", 2.0);
        
        // Get target location
        Location targetLocation = context.getTargetLocation(50.0);
        if (targetLocation == null) {
            player.sendMessage("§c❌ No valid target location for black hole!");
            return false;
        }
        
        // Create black hole
        createBlackHole(context, targetLocation, radius, duration, pullStrength, damagePerTick, voidDamage, launchForce);
        
        // Epic feedback
        player.sendMessage("§0§l🌀 APOCALYPSE BLACK HOLE SUMMONED!");
        player.sendMessage("§4The void hungers...");
        
        // Broadcast to server
        context.getPlugin().getServer().broadcastMessage("§0§l🌀 " + player.getName() + " has summoned a BLACK HOLE!");
        context.getPlugin().getServer().broadcastMessage("§c⚠ All entities beware - the void calls!");
        
        return true;
    }
    
    /**
     * Create the black hole effect
     */
    private void createBlackHole(AbilityContext context, Location center, double radius, int duration, 
                                double pullStrength, double damagePerTick, double voidDamage, double launchForce) {
        
        Player caster = context.getPlayer();
        List<Entity> affectedEntities = new CopyOnWriteArrayList<>();
        
        // Initial black hole creation
        playBlackHoleCreation(center);
        
        // Black hole main effect
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20; // Convert seconds to ticks
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    // End black hole
                    endBlackHole(center, affectedEntities, launchForce, voidDamage, caster);
                    cancel();
                    return;
                }
                
                // Update black hole
                updateBlackHole(center, radius, pullStrength, damagePerTick, affectedEntities, caster, ticks);
                ticks++;
            }
        }.runTaskTimer(context.getPlugin(), 20L, 1L); // Start after 1 second, run every tick
    }
    
    /**
     * Play black hole creation effects
     */
    private void playBlackHoleCreation(Location center) {
        // Massive initial explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 5, 3.0, 3.0, 3.0, 0);
        
        // Portal opening
        center.getWorld().spawnParticle(Particle.PORTAL, center, 150, 4.0, 4.0, 4.0, 3.0);
        
        // Dark energy
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 80, 3.0, 3.0, 3.0, 0.2);
        
        // Void particles
        center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 50, 2.5, 2.5, 2.5, 0.1);
        
        // Epic sounds
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.0f, 0.2f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.1f);
        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 2.0f, 0.5f);
    }
    
    /**
     * Update black hole effects each tick
     */
    private void updateBlackHole(Location center, double radius, double pullStrength, double damagePerTick,
                                List<Entity> affectedEntities, Player caster, int ticks) {
        
        // Find and affect entities
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity == caster) continue; // Don't affect the caster
            
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                double distance = entity.getLocation().distance(center);
                
                if (distance <= radius) {
                    // Add to affected list
                    if (!affectedEntities.contains(entity)) {
                        affectedEntities.add(entity);
                        
                        // Initial pull notification
                        if (entity instanceof Player) {
                            ((Player) entity).sendMessage("§0§l🌀 You're being pulled into the BLACK HOLE!");
                        }
                    }
                    
                    // Calculate pull force (stronger as closer to center)
                    double pullForce = pullStrength * (1.0 - (distance / radius)) * 1.5;
                    pullForce = Math.max(0.2, pullForce);
                    
                    // Pull entity toward center
                    Vector pullVector = center.toVector().subtract(entity.getLocation().toVector());
                    pullVector.normalize();
                    pullVector.multiply(pullForce);
                    
                    // Add downward pull (void effect)
                    pullVector.setY(pullVector.getY() - 0.4);
                    
                    entity.setVelocity(pullVector);
                    
                    // Damage based on proximity
                    double proximityFactor = 1.0 - (distance / radius);
                    double damage = damagePerTick * proximityFactor;
                    
                    if (distance < 3.0) {
                        // Close to center - massive damage
                        damage *= 3.0;
                        
                        // Void damage effect
                        if (distance < 1.5) {
                            livingEntity.damage(5.0, caster); // Extra void damage
                            
                            // Extreme close - launch upward
                            if (distance < 1.0) {
                                Vector launchVector = new Vector(0, 3.0, 0);
                                entity.setVelocity(launchVector);
                                
                                // Continuous void damage
                                applyVoidDamage(entity, caster);
                            }
                        }
                    }
                    
                    // Apply damage
                    if (damage > 0) {
                        livingEntity.damage(damage, caster);
                    }
                    
                    // Entity-specific effects
                    playEntityPullEffects(entity);
                }
            }
        }
        
        // Black hole center effects
        playBlackHoleCenterEffects(center, ticks);
        
        // Periodic sounds
        if (ticks % 15 == 0) { // Every 0.75 seconds
            center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 2.0f, 0.2f);
            center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.8f, 0.3f);
        }
    }
    
    /**
     * Play effects on entities being pulled
     */
    private void playEntityPullEffects(Entity entity) {
        // Portal particles around entity
        entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 8, 0.5, 0.5, 0.5, 1.0);
        
        // Dark energy particles
        entity.getWorld().spawnParticle(Particle.DRAGON_BREATH, entity.getLocation(), 3, 0.3, 0.3, 0.3, 0.02);
        
        // Void smoke
        entity.getWorld().spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 2, 0.2, 0.2, 0.2, 0.05);
        
        // Entity distress effects
        if (entity instanceof Player) {
            Player player = (Player) entity;
            
            // Screen shake effect (title with empty text)
            try {
                player.sendTitle("", "§0§l🌀", 0, 5, 0);
            } catch (Exception e) {
                // Title API not available
            }
        }
    }
    
    /**
     * Play black hole center effects
     */
    private void playBlackHoleCenterEffects(Location center, int ticks) {
        // Rotating void portal
        for (int i = 0; i < 12; i++) {
            double angle = (ticks * 0.3) + (i * Math.PI / 6);
            double radius = 3.0 + Math.sin(ticks * 0.1) * 0.8;
            
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + Math.sin(ticks * 0.2) * 1.5;
            
            Location particleLocation = new Location(center.getWorld(), x, y, z);
            
            // Main portal effect
            center.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 5, 0.2, 0.2, 0.2, 2.0);
            
            // Dark energy
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLocation, 2, 0.1, 0.1, 0.1, 0.01);
        }
        
        // Central void core
        center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 15, 1.0, 1.0, 1.0, 0.02);
        
        // Pulsing energy waves
        if (ticks % 20 == 0) { // Every second
            center.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, center, 30, 6.0, 6.0, 6.0, 0.3);
            center.getWorld().spawnParticle(Particle.PORTAL, center, 100, 8.0, 8.0, 8.0, 2.0);
        }
        
        // Void distortion effect
        if (ticks % 10 == 0) { // Every half second
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 20, 2.0, 2.0, 2.0, 0.05);
        }
    }
    
    /**
     * Apply continuous void damage to entities
     */
    private void applyVoidDamage(Entity entity, Player caster) {
        new BukkitRunnable() {
            int voidTicks = 0;
            
            @Override
            public void run() {
                if (!entity.isValid() || voidTicks >= 60) { // 3 seconds of void damage
                    cancel();
                    return;
                }
                
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    livingEntity.damage(1.5, caster);
                    
                    // Void effects
                    entity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, entity.getLocation(), 8, 0.5, 0.5, 0.5, 0.1);
                    entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 5, 0.3, 0.3, 0.3, 0.5);
                    
                    // Void whispers (if player)
                    if (entity instanceof Player && voidTicks % 20 == 0) {
                        ((Player) entity).sendMessage("§0§k§l" + "The void consumes you...");
                    }
                }
                
                voidTicks++;
            }
        }.runTaskTimer(context.getPlugin(), 0L, 3L); // Every 3 ticks
    }
    
    /**
     * End black hole with final explosion
     */
    private void endBlackHole(Location center, List<Entity> affectedEntities, double launchForce, 
                             double voidDamage, Player caster) {
        
        // Final cataclysmic explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 8, 2.0, 2.0, 2.0, 0);
        center.getWorld().spawnParticle(Particle.PORTAL, center, 300, 8.0, 8.0, 8.0, 4.0);
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 150, 6.0, 6.0, 6.0, 0.3);
        center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 100, 5.0, 5.0, 5.0, 0.2);
        
        // Final damage and launch all affected entities
        for (Entity entity : affectedEntities) {
            if (entity.isValid() && entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Final void damage
                livingEntity.damage(voidDamage, caster);
                
                // Epic launch effect
                Vector launchVector = new Vector(
                    (Math.random() - 0.5) * launchForce * 2,
                    launchForce + Math.random() * 2,
                    (Math.random() - 0.5) * launchForce * 2
                );
                entity.setVelocity(launchVector);
                
                // Final entity effects
                entity.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, entity.getLocation(), 15, 1.0, 1.0, 1.0, 0.2);
                entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 20, 1.5, 1.5, 1.5, 1.0);
                
                // Final message to players
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    player.sendMessage("§0§l🌀 You have been consumed by the void!");
                    player.sendTitle("§0§l🌀", "§4Apocalypse Power", 10, 40, 10);
                }
            }
        }
        
        // Final apocalyptic sounds
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 3.0f, 0.1f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 3.0f, 0.1f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 4.0f, 0.1f);
        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 2.0f, 0.3f);
        
        // Final broadcast
        context.getPlugin().getServer().broadcastMessage("§0🌀 The black hole has collapsed into nothingness...");
        
        context.getPlugin().getPluginLogger().info("🌀 Black hole ability completed by " + caster.getName());
    }
    
    @Override
    public void playSound(Player player) {
        // Apocalyptic sound combination
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 2.0f, 0.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.5f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 2.0f, 0.4f);
    }
    
    @Override
    public void playParticles(Player player) {
        Location location = player.getLocation();
        
        // Apocalyptic aura around caster
        for (int i = 0; i < 20; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 2.0 + Math.random() * 2.0;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            double y = Math.random() * 3.0;
            
            Location particleLocation = location.clone().add(x, y, z);
            
            location.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 3, 0.1, 0.1, 0.1, 1.0);
            location.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLocation, 1, 0.1, 0.1, 0.1, 0.01);
        }
    }
    
    @Override
    public double getRange() {
        return 50.0;
    }
    
    @Override
    public boolean requiresHelmetSlot() {
        return true;
    }
    
    @Override
    public boolean canExecute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Check if player has permission for apocalypse abilities
        if (!player.hasPermission("xsteal.ability.apocalypse")) {
            player.sendMessage("§c❌ You don't have permission to use Apocalypse abilities!");
            return false;
        }
        
        // Check if in safe world (prevent griefing)
        String worldName = player.getWorld().getName();
        List<String> bannedWorlds = context.getPlugin().getConfigManager().getMainConfig()
            .getStringList("apocalypse_head.banned_worlds");
        
        if (bannedWorlds.contains(worldName)) {
            player.sendMessage("§c❌ Apocalypse abilities are disabled in this world!");
            return false;
        }
        
        // Check cooldown (even apocalypse head should have some cooldown)
        long lastUse = getLastApocalypseUse(player);
        long cooldown = context.getPlugin().getConfigManager().getMainConfig().getLong("apocalypse_head.cooldown_seconds", 300) * 1000; // 5 minutes default
        
        if (System.currentTimeMillis() - lastUse < cooldown) {
            long remaining = (cooldown - (System.currentTimeMillis() - lastUse)) / 1000;
            player.sendMessage("§c❌ Apocalypse ability on cooldown for " + remaining + " seconds!");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get last apocalypse ability use time for player
     */
    private long getLastApocalypseUse(Player player) {
        // This would be stored in a map or database
        // For now, return 0 (no previous use)
        return 0;
    }
    
    @Override
    public void cleanup(AbilityContext context) {
        // Cleanup any remaining black hole effects
        context.getPlugin().getPluginLogger().debug("Cleaned up black hole effects");
    }
}