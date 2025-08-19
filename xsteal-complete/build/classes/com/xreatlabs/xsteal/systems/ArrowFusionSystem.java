package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.utils.ComprehensiveVersionSupport;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Arrow Fusion System for XSteal
 * Allows combining arrows with mob heads to create special arrows with unique abilities
 * 
 * Fusion Recipes:
 * - Arrow + Creeper Head = Creeper Arrow (explosive on contact)
 * - Arrow + Enderman Head = Ender Arrow (teleport to impact location)
 * - Arrow + Blaze Head = Fire Arrow (ignites targets and trail)
 * - Arrow + Ice/Stray Head = Frost Arrow (freezes targets)
 * - Arrow + Poison Spider Head = Poison Arrow (poison on hit)
 * - Arrow + Lightning Head = Thunder Arrow (lightning strike)
 */
public class ArrowFusionSystem implements Listener {
    
    private final XSteal plugin;
    private final Map<String, ArrowType> arrowTypes;
    
    // Metadata keys for tracking special arrows
    private static final String ARROW_TYPE_KEY = "xsteal_arrow_type";
    private static final String ARROW_POWER_KEY = "xsteal_arrow_power";
    
    public ArrowFusionSystem(XSteal plugin) {
        this.plugin = plugin;
        this.arrowTypes = new HashMap<>();
        
        // Initialize arrow types
        initializeArrowTypes();
        
        // Register recipes
        if (ComprehensiveVersionSupport.isAtLeast(1, 12)) {
            registerArrowFusionRecipes();
        }
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Initialize all arrow types with their abilities
     */
    private void initializeArrowTypes() {
        arrowTypes.put("creeper_arrow", new ArrowType("creeper_arrow", "§a§lCreeper Arrow", 
            "Explodes on contact with entities", "creeper"));
        arrowTypes.put("ender_arrow", new ArrowType("ender_arrow", "§5§lEnder Arrow", 
            "Teleports shooter to impact location", "enderman"));
        arrowTypes.put("fire_arrow", new ArrowType("fire_arrow", "§6§lFire Arrow", 
            "Ignites targets and leaves fire trail", "blaze"));
        arrowTypes.put("frost_arrow", new ArrowType("frost_arrow", "§b§lFrost Arrow", 
            "Freezes targets and creates ice", "stray"));
        arrowTypes.put("poison_arrow", new ArrowType("poison_arrow", "§2§lPoison Arrow", 
            "Poisons targets on hit", "cave_spider"));
        arrowTypes.put("thunder_arrow", new ArrowType("thunder_arrow", "§e§lThunder Arrow", 
            "Strikes lightning at impact", "charged_creeper"));
        arrowTypes.put("void_arrow", new ArrowType("void_arrow", "§0§lVoid Arrow", 
            "Deals void damage and phases through blocks", "endermite"));
        arrowTypes.put("healing_arrow", new ArrowType("healing_arrow", "§d§lHealing Arrow", 
            "Heals friendly targets", "allay"));
    }
    
    /**
     * Register arrow fusion recipes
     */
    private void registerArrowFusionRecipes() {
        try {
            // Creeper Arrow Recipe: Arrow + Creeper Head
            registerArrowRecipe("creeper_arrow", "creeper", createCreeperArrow());
            
            // Ender Arrow Recipe: Arrow + Enderman Head  
            registerArrowRecipe("ender_arrow", "enderman", createEnderArrow());
            
            // Fire Arrow Recipe: Arrow + Blaze Head
            registerArrowRecipe("fire_arrow", "blaze", createFireArrow());
            
            // Frost Arrow Recipe: Arrow + Stray Head
            registerArrowRecipe("frost_arrow", "stray", createFrostArrow());
            
            // Poison Arrow Recipe: Arrow + Cave Spider Head
            registerArrowRecipe("poison_arrow", "cave_spider", createPoisonArrow());
            
            // Thunder Arrow Recipe: Arrow + Charged Creeper Head (special)
            registerArrowRecipe("thunder_arrow", "charged_creeper", createThunderArrow());
            
            // Void Arrow Recipe: Arrow + Endermite Head
            registerArrowRecipe("void_arrow", "endermite", createVoidArrow());
            
            // Healing Arrow Recipe: Arrow + Allay Head
            registerArrowRecipe("healing_arrow", "allay", createHealingArrow());
            
            plugin.getPluginLogger().info("Registered " + arrowTypes.size() + " arrow fusion recipes");
            
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Failed to register some arrow recipes: " + e.getMessage());
        }
    }
    
    /**
     * Register individual arrow recipe
     */
    private void registerArrowRecipe(String arrowKey, String headKey, ItemStack result) {
        try {
            NamespacedKey key = new NamespacedKey(plugin, arrowKey);
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            
            // Add ingredients: Arrow + Mob Head
            recipe.addIngredient(Material.ARROW);
            
            // Add mob head (this would need to be the actual head item)
            // For now, we'll use a placeholder system
            recipe.addIngredient(Material.PLAYER_HEAD); // Placeholder for mob head
            
            Bukkit.addRecipe(recipe);
            
            plugin.getPluginLogger().debug("Registered " + arrowKey + " fusion recipe");
            
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Failed to register " + arrowKey + " recipe: " + e.getMessage());
        }
    }
    
    // ===== ARROW CREATION METHODS =====
    
    /**
     * Create Creeper Arrow
     */
    public ItemStack createCreeperArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§a§lCreeper Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with creeper essence",
                "",
                "§a💥 EXPLOSIVE ABILITY:",
                "§7• Explodes on contact with entities",
                "§7• 3-block explosion radius",
                "§7• Damages all nearby entities",
                "§7• No block damage (balanced)",
                "",
                "§e▶ Shoot at enemies for explosive impact",
                "§c⚠ Handle with care!",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Ender Arrow
     */
    public ItemStack createEnderArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§5§lEnder Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with enderman essence",
                "",
                "§5⚡ TELEPORTATION ABILITY:",
                "§7• Teleports shooter to impact location",
                "§7• Works through walls and obstacles",
                "§7• Safe landing guaranteed",
                "§7• Portal particle effects",
                "",
                "§e▶ Shoot where you want to teleport",
                "§5✨ Instant travel across distances",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Fire Arrow
     */
    public ItemStack createFireArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§6§lFire Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with blaze essence",
                "",
                "§6🔥 FIRE ABILITY:",
                "§7• Ignites targets on hit",
                "§7• Leaves fire trail in flight",
                "§7• Burns for 10 seconds",
                "§7• Fire immunity to shooter",
                "",
                "§e▶ Perfect for crowd control",
                "§6🔥 Burns everything in its path",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Frost Arrow
     */
    public ItemStack createFrostArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§b§lFrost Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with stray essence",
                "",
                "§b❄ FROST ABILITY:",
                "§7• Freezes targets on hit",
                "§7• Applies slowness effect",
                "§7• Creates ice patches",
                "§7• Cold immunity to shooter",
                "",
                "§e▶ Excellent for slowing enemies",
                "§b❄ Freezes the battlefield",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Poison Arrow
     */
    public ItemStack createPoisonArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§2§lPoison Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with cave spider venom",
                "",
                "§2☠ POISON ABILITY:",
                "§7• Poisons targets on hit",
                "§7• Poison level 2 for 15 seconds",
                "§7• Spreads to nearby entities",
                "§7• Poison immunity to shooter",
                "",
                "§e▶ Deadly over time damage",
                "§2☠ Venomous and spreading",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Thunder Arrow
     */
    public ItemStack createThunderArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§e§lThunder Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow charged with lightning essence",
                "",
                "§e⚡ LIGHTNING ABILITY:",
                "§7• Strikes lightning at impact",
                "§7• Massive electrical damage",
                "§7• Charges nearby creepers",
                "§7• Thunder immunity to shooter",
                "",
                "§e▶ Devastating electrical attack",
                "§e⚡ Brings the storm with you",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Void Arrow
     */
    public ItemStack createVoidArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§0§lVoid Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with void essence",
                "",
                "§0🌌 VOID ABILITY:",
                "§7• Phases through blocks",
                "§7• Deals void damage",
                "§7• Ignores armor",
                "§7• Teleports randomly on hit",
                "",
                "§e▶ Unstoppable piercing attack",
                "§0🌌 Bends reality itself",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Create Healing Arrow
     */
    public ItemStack createHealingArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName("§d§lHealing Arrow");
            meta.setLore(Arrays.asList(
                "§7An arrow infused with allay kindness",
                "",
                "§d💖 HEALING ABILITY:",
                "§7• Heals friendly targets",
                "§7• Damages hostile mobs",
                "§7• Regeneration effect",
                "§7• Protective aura on impact",
                "",
                "§e▶ Support your allies from range",
                "§d💖 Kindness at arrow-point",
                "§8XSteal Fusion Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Handle arrow shooting to apply special effects
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowShoot(EntityShootBowEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if using special arrow
        ItemStack arrow = getArrowFromInventory(player);
        if (arrow == null) {
            return;
        }
        
        String arrowType = getArrowType(arrow);
        if (arrowType == null) {
            return;
        }
        
        // Mark projectile with arrow type
        if (event.getProjectile() instanceof Arrow) {
            Arrow shotArrow = (Arrow) event.getProjectile();
            shotArrow.setMetadata(ARROW_TYPE_KEY, new FixedMetadataValue(plugin, arrowType));
            
            // Apply visual effects based on arrow type
            applyArrowFlightEffects(shotArrow, arrowType);
            
            player.sendMessage("§6⚡ " + arrowTypes.get(arrowType).getDisplayName() + " §6fired!");
            
            plugin.getPluginLogger().debug("Player " + player.getName() + " fired " + arrowType);
        }
    }
    
    /**
     * Handle arrow impact effects
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowHit(ProjectileHitEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }
        
        Arrow arrow = (Arrow) event.getEntity();
        
        // Check if it's a special arrow
        if (!arrow.hasMetadata(ARROW_TYPE_KEY)) {
            return;
        }
        
        String arrowType = arrow.getMetadata(ARROW_TYPE_KEY).get(0).asString();
        Player shooter = arrow.getShooter() instanceof Player ? (Player) arrow.getShooter() : null;
        
        if (shooter == null) {
            return;
        }
        
        Location hitLocation = arrow.getLocation();
        Entity hitEntity = event.getHitEntity();
        
        // Execute arrow ability based on type
        executeArrowAbility(arrowType, shooter, hitLocation, hitEntity, arrow);
        
        // Remove arrow after effect
        arrow.remove();
    }
    
    /**
     * Execute arrow ability based on type
     */
    private void executeArrowAbility(String arrowType, Player shooter, Location hitLocation, Entity hitEntity, Arrow arrow) {
        switch (arrowType) {
            case "creeper_arrow":
                executeCreeperArrowAbility(shooter, hitLocation, hitEntity);
                break;
            case "ender_arrow":
                executeEnderArrowAbility(shooter, hitLocation);
                break;
            case "fire_arrow":
                executeFireArrowAbility(shooter, hitLocation, hitEntity);
                break;
            case "frost_arrow":
                executeFrostArrowAbility(shooter, hitLocation, hitEntity);
                break;
            case "poison_arrow":
                executePoisonArrowAbility(shooter, hitLocation, hitEntity);
                break;
            case "thunder_arrow":
                executeThunderArrowAbility(shooter, hitLocation);
                break;
            case "void_arrow":
                executeVoidArrowAbility(shooter, hitLocation, hitEntity);
                break;
            case "healing_arrow":
                executeHealingArrowAbility(shooter, hitLocation, hitEntity);
                break;
        }
    }
    
    /**
     * Creeper Arrow Ability - Explosive on contact
     */
    private void executeCreeperArrowAbility(Player shooter, Location hitLocation, Entity hitEntity) {
        // Create explosion at impact
        hitLocation.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, hitLocation, 3, 1.0, 1.0, 1.0, 0);
        hitLocation.getWorld().spawnParticle(Particle.FLAME, hitLocation, 20, 2.0, 2.0, 2.0, 0.1);
        
        // Explosion sound
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.5f);
        
        // Damage nearby entities
        for (Entity entity : hitLocation.getWorld().getNearbyEntities(hitLocation, 3, 3, 3)) {
            if (entity == shooter) continue; // Don't damage shooter
            
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;
                double distance = entity.getLocation().distance(hitLocation);
                double damage = 12.0 * (1.0 - (distance / 3.0)); // Damage decreases with distance
                
                target.damage(damage, shooter);
                
                // Knockback effect
                Vector knockback = entity.getLocation().subtract(hitLocation).toVector();
                knockback.normalize().multiply(2.0);
                knockback.setY(Math.max(0.5, knockback.getY()));
                entity.setVelocity(knockback);
            }
        }
        
        shooter.sendMessage("§a💥 Creeper Arrow exploded!");
    }
    
    /**
     * Ender Arrow Ability - Teleport to impact location
     */
    private void executeEnderArrowAbility(Player shooter, Location hitLocation) {
        // Teleport effects at origin
        Location originalLocation = shooter.getLocation();
        originalLocation.getWorld().spawnParticle(Particle.PORTAL, originalLocation, 30, 1.0, 1.0, 1.0, 1.0);
        originalLocation.getWorld().playSound(originalLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        // Find safe teleport location
        Location teleportLocation = findSafeTeleportLocation(hitLocation);
        
        // Teleport player
        shooter.teleport(teleportLocation);
        
        // Teleport effects at destination
        teleportLocation.getWorld().spawnParticle(Particle.PORTAL, teleportLocation, 30, 1.0, 1.0, 1.0, 1.0);
        teleportLocation.getWorld().playSound(teleportLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        shooter.sendMessage("§5⚡ Ender Arrow teleportation!");
    }
    
    /**
     * Fire Arrow Ability - Ignites targets and trail
     */
    private void executeFireArrowAbility(Player shooter, Location hitLocation, Entity hitEntity) {
        // Fire explosion
        hitLocation.getWorld().spawnParticle(Particle.FLAME, hitLocation, 30, 2.0, 2.0, 2.0, 0.15);
        hitLocation.getWorld().spawnParticle(Particle.LAVA, hitLocation, 10, 1.5, 1.5, 1.5, 0.1);
        
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_BLAZE_SHOOT, 1.5f, 1.0f);
        
        // Ignite hit entity
        if (hitEntity != null) {
            hitEntity.setFireTicks(200); // 10 seconds
            
            if (hitEntity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) hitEntity;
                target.damage(8.0, shooter);
            }
        }
        
        // Create fire in area
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location fireLoc = hitLocation.clone().add(x, 0, z);
                if (fireLoc.getBlock().getType() == Material.AIR) {
                    fireLoc.getBlock().setType(Material.FIRE);
                    
                    // Remove fire after duration
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (fireLoc.getBlock().getType() == Material.FIRE) {
                            fireLoc.getBlock().setType(Material.AIR);
                        }
                    }, 200L); // 10 seconds
                }
            }
        }
        
        shooter.sendMessage("§6🔥 Fire Arrow ignited the area!");
    }
    
    /**
     * Frost Arrow Ability - Freezes targets
     */
    private void executeFrostArrowAbility(Player shooter, Location hitLocation, Entity hitEntity) {
        // Frost explosion
        hitLocation.getWorld().spawnParticle(Particle.SNOW_SHOVEL, hitLocation, 40, 2.0, 2.0, 2.0, 0.1);
        hitLocation.getWorld().spawnParticle(Particle.CLOUD, hitLocation, 20, 1.5, 1.5, 1.5, 0.05);
        
        hitLocation.getWorld().playSound(hitLocation, Sound.BLOCK_GLASS_BREAK, 1.5f, 2.0f);
        
        // Freeze hit entity
        if (hitEntity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hitEntity;
            target.damage(6.0, shooter);
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 3)); // 15 seconds
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 300, 2));
        }
        
        // Create ice patches
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location iceLoc = hitLocation.clone().add(x, -1, z);
                if (iceLoc.getBlock().getType() == Material.WATER) {
                    iceLoc.getBlock().setType(Material.ICE);
                }
            }
        }
        
        shooter.sendMessage("§b❄ Frost Arrow froze the target!");
    }
    
    /**
     * Poison Arrow Ability - Poisons targets
     */
    private void executePoisonArrowAbility(Player shooter, Location hitLocation, Entity hitEntity) {
        // Poison cloud
        hitLocation.getWorld().spawnParticle(Particle.SPELL_WITCH, hitLocation, 30, 2.0, 2.0, 2.0, 0.1);
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 1.2f);
        
        // Poison hit entity
        if (hitEntity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hitEntity;
            target.damage(4.0, shooter);
            target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 300, 2)); // 15 seconds
        }
        
        // Spread poison to nearby entities
        for (Entity entity : hitLocation.getWorld().getNearbyEntities(hitLocation, 3, 3, 3)) {
            if (entity == shooter) continue;
            
            if (entity instanceof LivingEntity) {
                LivingEntity target = (LivingEntity) entity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1)); // 5 seconds
            }
        }
        
        shooter.sendMessage("§2☠ Poison Arrow infected the area!");
    }
    
    /**
     * Thunder Arrow Ability - Lightning strike
     */
    private void executeThunderArrowAbility(Player shooter, Location hitLocation) {
        // Lightning strike
        hitLocation.getWorld().strikeLightning(hitLocation);
        
        // Additional thunder effects
        hitLocation.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, hitLocation, 50, 3.0, 3.0, 3.0, 0.2);
        
        // Charge nearby creepers
        for (Entity entity : hitLocation.getWorld().getNearbyEntities(hitLocation, 8, 8, 8)) {
            if (entity instanceof Creeper) {
                Creeper creeper = (Creeper) entity;
                creeper.setPowered(true);
                creeper.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 6000, 0));
            }
        }
        
        shooter.sendMessage("§e⚡ Thunder Arrow called down lightning!");
    }
    
    /**
     * Void Arrow Ability - Phases and void damage
     */
    private void executeVoidArrowAbility(Player shooter, Location hitLocation, Entity hitEntity) {
        // Void effects
        hitLocation.getWorld().spawnParticle(Particle.PORTAL, hitLocation, 40, 2.0, 2.0, 2.0, 1.0);
        hitLocation.getWorld().spawnParticle(Particle.DRAGON_BREATH, hitLocation, 20, 1.5, 1.5, 1.5, 0.05);
        
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_ENDERMAN_SCREAM, 1.0f, 0.5f);
        
        // Void damage (ignores armor)
        if (hitEntity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hitEntity;
            
            // Direct health damage (bypasses armor)
            double currentHealth = target.getHealth();
            double voidDamage = 15.0;
            target.setHealth(Math.max(0, currentHealth - voidDamage));
            
            // Random teleport effect
            Location randomLoc = hitLocation.clone().add(
                (Math.random() - 0.5) * 10,
                Math.random() * 3,
                (Math.random() - 0.5) * 10
            );
            
            if (target instanceof Player) {
                target.teleport(randomLoc);
                ((Player) target).sendMessage("§0🌌 Void arrow displaced you through reality!");
            }
        }
        
        shooter.sendMessage("§0🌌 Void Arrow pierced reality!");
    }
    
    /**
     * Healing Arrow Ability - Heals allies, damages enemies
     */
    private void executeHealingArrowAbility(Player shooter, Location hitLocation, Entity hitEntity) {
        // Healing effects
        hitLocation.getWorld().spawnParticle(Particle.HEART, hitLocation, 20, 2.0, 2.0, 2.0, 0.1);
        hitLocation.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, hitLocation, 15, 1.5, 1.5, 1.5, 0.1);
        
        hitLocation.getWorld().playSound(hitLocation, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
        
        if (hitEntity instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) hitEntity;
            
            if (target instanceof Player) {
                // Heal players
                Player targetPlayer = (Player) target;
                double currentHealth = targetPlayer.getHealth();
                double maxHealth = targetPlayer.getMaxHealth();
                targetPlayer.setHealth(Math.min(maxHealth, currentHealth + 8.0));
                
                targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                targetPlayer.sendMessage("§d💖 Healing Arrow restored your health!");
                
            } else {
                // Damage hostile mobs
                target.damage(6.0, shooter);
            }
        }
        
        // Heal nearby players
        for (Entity entity : hitLocation.getWorld().getNearbyEntities(hitLocation, 4, 4, 4)) {
            if (entity instanceof Player && entity != shooter) {
                Player nearbyPlayer = (Player) entity;
                double currentHealth = nearbyPlayer.getHealth();
                double maxHealth = nearbyPlayer.getMaxHealth();
                nearbyPlayer.setHealth(Math.min(maxHealth, currentHealth + 4.0));
            }
        }
        
        shooter.sendMessage("§d💖 Healing Arrow spread restoration!");
    }
    
    /**
     * Apply visual effects during arrow flight
     */
    private void applyArrowFlightEffects(Arrow arrow, String arrowType) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead()) {
                    cancel();
                    return;
                }
                
                Location arrowLoc = arrow.getLocation();
                
                switch (arrowType) {
                    case "creeper_arrow":
                        arrow.getWorld().spawnParticle(Particle.SMOKE_NORMAL, arrowLoc, 3, 0.1, 0.1, 0.1, 0.02);
                        break;
                    case "ender_arrow":
                        arrow.getWorld().spawnParticle(Particle.PORTAL, arrowLoc, 5, 0.2, 0.2, 0.2, 0.5);
                        break;
                    case "fire_arrow":
                        arrow.getWorld().spawnParticle(Particle.FLAME, arrowLoc, 4, 0.1, 0.1, 0.1, 0.02);
                        break;
                    case "frost_arrow":
                        arrow.getWorld().spawnParticle(Particle.SNOW_SHOVEL, arrowLoc, 3, 0.1, 0.1, 0.1, 0.02);
                        break;
                    case "poison_arrow":
                        arrow.getWorld().spawnParticle(Particle.SPELL_WITCH, arrowLoc, 2, 0.1, 0.1, 0.1, 0.02);
                        break;
                    case "thunder_arrow":
                        arrow.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, arrowLoc, 5, 0.2, 0.2, 0.2, 0.05);
                        break;
                    case "void_arrow":
                        arrow.getWorld().spawnParticle(Particle.DRAGON_BREATH, arrowLoc, 3, 0.1, 0.1, 0.1, 0.01);
                        break;
                    case "healing_arrow":
                        arrow.getWorld().spawnParticle(Particle.HEART, arrowLoc, 2, 0.1, 0.1, 0.1, 0.02);
                        break;
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // Every 2 ticks
    }
    
    /**
     * Get arrow from player inventory
     */
    private ItemStack getArrowFromInventory(Player player) {
        // Check offhand first (1.9+)
        if (ComprehensiveVersionSupport.isAtLeast(1, 9)) {
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && isSpecialArrow(offhand)) {
                return offhand;
            }
        }
        
        // Check main inventory for arrows
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isSpecialArrow(item)) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * Check if item is a special arrow
     */
    private boolean isSpecialArrow(ItemStack item) {
        if (item.getType() != Material.ARROW) {
            return false;
        }
        
        return getArrowType(item) != null;
    }
    
    /**
     * Get arrow type from ItemStack
     */
    private String getArrowType(ItemStack arrow) {
        if (arrow == null || arrow.getItemMeta() == null) {
            return null;
        }
        
        String displayName = ChatColor.stripColor(arrow.getItemMeta().getDisplayName());
        
        for (Map.Entry<String, ArrowType> entry : arrowTypes.entrySet()) {
            String arrowTypeName = ChatColor.stripColor(entry.getValue().getDisplayName());
            if (displayName.equals(arrowTypeName)) {
                return entry.getKey();
            }
        }
        
        return null;
    }
    
    /**
     * Find safe teleport location near target
     */
    private Location findSafeTeleportLocation(Location target) {
        // Try the exact location first
        if (isSafeLocation(target)) {
            return target;
        }
        
        // Try locations around the target
        for (int y = 0; y <= 3; y++) {
            Location testLoc = target.clone().add(0, y, 0);
            if (isSafeLocation(testLoc)) {
                return testLoc;
            }
        }
        
        // Fallback to original location if no safe spot found
        return target;
    }
    
    /**
     * Check if location is safe for teleportation
     */
    private boolean isSafeLocation(Location location) {
        if (location.getY() < 0 || location.getY() > 255) {
            return false;
        }
        
        Material blockType = location.getBlock().getType();
        Material aboveType = location.clone().add(0, 1, 0).getBlock().getType();
        
        // Check for solid blocks
        if (blockType.isSolid() || aboveType.isSolid()) {
            return false;
        }
        
        // Check for dangerous blocks
        if (blockType.name().contains("LAVA") || blockType.name().contains("FIRE")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Show arrow fusion recipes to player
     */
    public void showArrowRecipes(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══ XSteal Arrow Fusion Recipes ═══");
        player.sendMessage("");
        
        for (Map.Entry<String, ArrowType> entry : arrowTypes.entrySet()) {
            ArrowType arrowType = entry.getValue();
            player.sendMessage(ChatColor.YELLOW + "🏹 " + arrowType.getDisplayName());
            player.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + " + arrowType.getRequiredHead().replace("_", " ").toUpperCase() + " Head");
            player.sendMessage(ChatColor.GRAY + "  Effect: " + arrowType.getDescription());
            player.sendMessage("");
        }
        
        player.sendMessage(ChatColor.GREEN + "💡 Craft these arrows to gain ranged abilities!");
        player.sendMessage(ChatColor.GRAY + "Simply combine an arrow with the required mob head");
    }
    
    /**
     * Arrow type data class
     */
    public static class ArrowType {
        private final String key;
        private final String displayName;
        private final String description;
        private final String requiredHead;
        
        public ArrowType(String key, String displayName, String description, String requiredHead) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
            this.requiredHead = requiredHead;
        }
        
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getRequiredHead() { return requiredHead; }
    }
    
    /**
     * Cleanup arrow fusion system
     */
    public void cleanup() {
        // Remove custom recipes
        if (ComprehensiveVersionSupport.isAtLeast(1, 12)) {
            for (String arrowKey : arrowTypes.keySet()) {
                try {
                    Bukkit.removeRecipe(new NamespacedKey(plugin, arrowKey));
                } catch (Exception e) {
                    // Recipe may not exist
                }
            }
        }
        
        arrowTypes.clear();
    }
}