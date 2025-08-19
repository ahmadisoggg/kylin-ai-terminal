package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.utils.ComprehensiveVersionSupport;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Apocalypse Head System - Fusion of Dragon + Wither Heads
 * Creates the ultimate endgame head with devastating black hole ability
 * 
 * Features:
 * - Fusion crafting recipe requiring both boss heads
 * - Black hole ability that pulls entities and deals void damage
 * - Dark Altar requirement for crafting (optional)
 * - Boss kill advancement requirements
 */
public class ApocalypseHeadSystem implements Listener {
    
    private final XSteal plugin;
    private static final String APOCALYPSE_HEAD_KEY = "apocalypse_head";
    private static final String DARK_ALTAR_KEY = "xsteal_dark_altar";
    
    // Black hole configuration
    private static final double BLACK_HOLE_RADIUS = 15.0;
    private static final double PULL_STRENGTH = 0.8;
    private static final double DAMAGE_PER_TICK = 2.0;
    private static final int BLACK_HOLE_DURATION = 100; // 5 seconds
    
    public ApocalypseHeadSystem(XSteal plugin) {
        this.plugin = plugin;
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Register custom recipe
        if (ComprehensiveVersionSupport.isAtLeast(1, 12)) {
            registerApocalypseHeadRecipe();
        }
    }
    
    /**
     * Create the Apocalypse Head item
     */
    public ItemStack createApocalypseHead() {
        ItemStack apocalypseHead = new ItemStack(ComprehensiveVersionSupport.getMaterial("player_head"));
        ItemMeta meta = apocalypseHead.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_RED + "§l🌀 APOCALYPSE HEAD");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "The ultimate fusion of Dragon and Wither power",
                ChatColor.DARK_GRAY + "Forged in the fires of the End and Nether",
                "",
                ChatColor.DARK_RED + "§l🌀 BLACK HOLE ABILITY:",
                ChatColor.RED + "• Creates a devastating black hole",
                ChatColor.RED + "• Pulls all entities within 15 blocks",
                ChatColor.RED + "• Deals increasing damage as they get closer",
                ChatColor.RED + "• Launches entities upward with void damage",
                ChatColor.RED + "• Duration: 5 seconds of pure destruction",
                "",
                ChatColor.YELLOW + "⚡ Activation: Left-click while wearing",
                ChatColor.GOLD + "👑 Category: LEGENDARY FUSION",
                "",
                ChatColor.DARK_PURPLE + "\"The end of all things...\"",
                "",
                ChatColor.DARK_GRAY + "XSteal Apocalypse Head",
                ChatColor.DARK_GRAY + "" + ChatColor.MAGIC + APOCALYPSE_HEAD_KEY
            ));
            apocalypseHead.setItemMeta(meta);
        }
        
        return apocalypseHead;
    }
    
    /**
     * Register the Apocalypse Head crafting recipe
     */
    private void registerApocalypseHeadRecipe() {
        try {
            ItemStack apocalypseHead = createApocalypseHead();
            
            NamespacedKey key = new NamespacedKey(plugin, "apocalypse_head");
            ShapedRecipe recipe = new ShapedRecipe(key, apocalypseHead);
            
            // Set recipe pattern
            // [ D , N , W ]
            // [ O , E , O ]  
            // [ N , O , N ]
            recipe.shape(
                "DNW",
                "OEO",
                "NON"
            );
            
            // Set ingredients
            recipe.setIngredient('D', Material.DRAGON_HEAD); // Dragon Head
            recipe.setIngredient('W', getWitherSkullMaterial()); // Wither Skeleton Skull (closest to Wither Head)
            recipe.setIngredient('N', Material.NETHER_STAR); // Nether Star
            recipe.setIngredient('O', Material.OBSIDIAN); // Obsidian
            recipe.setIngredient('E', Material.END_CRYSTAL); // End Crystal
            
            // Register recipe
            Bukkit.addRecipe(recipe);
            
            plugin.getPluginLogger().info("Registered Apocalypse Head fusion recipe");
            
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Failed to register Apocalypse Head recipe: " + e.getMessage());
        }
    }
    
    /**
     * Get Wither Skeleton Skull material for current version
     */
    private Material getWitherSkullMaterial() {
        if (ComprehensiveVersionSupport.isAtLeast(1, 13)) {
            return Material.WITHER_SKELETON_SKULL;
        } else {
            // Legacy version - use skull item with damage value
            return Material.valueOf("SKULL_ITEM");
        }
    }
    
    /**
     * Handle Apocalypse Head crafting
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onApocalypseHeadCraft(CraftItemEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if crafting Apocalypse Head
        ItemStack result = event.getRecipe().getResult();
        if (!isApocalypseHead(result)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if player has required boss kill advancements
        if (!hasRequiredBossKills(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "❌ You must defeat both the Ender Dragon and Wither first!");
            player.sendMessage(ChatColor.YELLOW + "💡 Kill both bosses with creeper arrows to unlock this recipe");
            return;
        }
        
        // Check if crafting at Dark Altar (if required)
        if (plugin.getConfigManager().getMainConfig().getBoolean("apocalypse_head.require_dark_altar", true)) {
            if (!isNearDarkAltar(player)) {
                event.setCancelled(true);
                player.sendMessage(ChatColor.RED + "❌ This fusion can only be performed at a Dark Altar!");
                player.sendMessage(ChatColor.YELLOW + "💡 Place obsidian blocks in a ritual formation");
                return;
            }
        }
        
        // Epic crafting effects
        playApocalypseCraftingEffects(player);
        
        // Broadcast legendary crafting
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "§l" + "=".repeat(50));
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "§l        🌀 APOCALYPSE HEAD FORGED! 🌀");
        Bukkit.broadcastMessage(ChatColor.RED + "§l" + player.getName() + " has created the ultimate fusion head!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "The power of Dragon and Wither combined...");
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "§l" + "=".repeat(50));
        
        // Play epic sound to all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 0.5f);
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);
        }
        
        plugin.getPluginLogger().info("🌀 Player " + player.getName() + " crafted Apocalypse Head!");
    }
    
    /**
     * Handle Black Hole ability activation
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlackHoleActivation(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle left-click
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if wearing Apocalypse Head
        ItemStack helmet = player.getInventory().getHelmet();
        if (!isApocalypseHead(helmet)) {
            return;
        }
        
        // Check permissions
        if (!player.hasPermission("xsteal.ability.apocalypse")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use the Apocalypse Head!");
            return;
        }
        
        // Get target location
        Location targetLocation = getTargetLocation(player, 50.0);
        if (targetLocation == null) {
            player.sendMessage(ChatColor.RED + "❌ No valid target location for black hole!");
            return;
        }
        
        // Cancel event to prevent block breaking
        event.setCancelled(true);
        
        // Create black hole
        createBlackHole(player, targetLocation);
        
        player.sendMessage(ChatColor.DARK_RED + "§l🌀 BLACK HOLE SUMMONED!");
        player.sendMessage(ChatColor.RED + "Apocalypse power unleashed...");
    }
    
    /**
     * Create the devastating black hole effect
     */
    private void createBlackHole(Player caster, Location center) {
        // Notify all players
        plugin.getNotificationSystem().sendNotification(
            NotificationSystem.NotificationType.ABILITY_USE,
            caster.getName() + " summoned a BLACK HOLE with the Apocalypse Head!"
        );
        
        // Track entities in black hole
        List<Entity> affectedEntities = new CopyOnWriteArrayList<>();
        
        // Black hole task
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= BLACK_HOLE_DURATION) {
                    // End black hole with final explosion
                    endBlackHole(center, affectedEntities);
                    cancel();
                    return;
                }
                
                // Update black hole effects
                updateBlackHole(center, affectedEntities, caster, ticks);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Every tick for maximum effect
        
        // Initial black hole creation effects
        playBlackHoleCreationEffects(center);
    }
    
    /**
     * Update black hole effects each tick
     */
    private void updateBlackHole(Location center, List<Entity> affectedEntities, Player caster, int ticks) {
        // Find entities in range
        for (Entity entity : center.getWorld().getNearbyEntities(center, BLACK_HOLE_RADIUS, BLACK_HOLE_RADIUS, BLACK_HOLE_RADIUS)) {
            if (entity == caster) continue; // Don't affect caster
            
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                double distance = entity.getLocation().distance(center);
                
                if (distance <= BLACK_HOLE_RADIUS) {
                    // Add to affected entities
                    if (!affectedEntities.contains(entity)) {
                        affectedEntities.add(entity);
                    }
                    
                    // Calculate pull force (stronger as it gets closer)
                    double pullForce = PULL_STRENGTH * (1.0 - (distance / BLACK_HOLE_RADIUS));
                    pullForce = Math.max(0.1, pullForce);
                    
                    // Pull entity toward center
                    Vector pullVector = center.toVector().subtract(entity.getLocation().toVector());
                    pullVector.normalize();
                    pullVector.multiply(pullForce);
                    
                    // Apply downward pull as well
                    pullVector.setY(pullVector.getY() - 0.3);
                    
                    entity.setVelocity(pullVector);
                    
                    // Damage based on proximity to center
                    if (distance < 3.0) {
                        // Close to center - massive damage
                        double damage = DAMAGE_PER_TICK * (1.0 - (distance / 3.0)) * 2.0;
                        livingEntity.damage(damage, caster);
                        
                        // Void damage effect
                        if (distance < 1.5) {
                            livingEntity.damage(5.0, caster); // Extra void damage
                            
                            // Launch upward if very close to center
                            if (distance < 0.8) {
                                Vector launchVector = new Vector(0, 2.0, 0);
                                entity.setVelocity(launchVector);
                                
                                // Void damage over time
                                new BukkitRunnable() {
                                    int voidTicks = 0;
                                    
                                    @Override
                                    public void run() {
                                        if (!entity.isValid() || voidTicks >= 40) {
                                            cancel();
                                            return;
                                        }
                                        
                                        if (entity instanceof LivingEntity) {
                                            ((LivingEntity) entity).damage(1.0, caster);
                                            entity.getWorld().spawnParticle(Particle.SMOKE_NORMAL, 
                                                entity.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
                                        }
                                        
                                        voidTicks++;
                                    }
                                }.runTaskTimer(plugin, 0L, 2L);
                            }
                        }
                    } else {
                        // Normal pull damage
                        double damage = DAMAGE_PER_TICK * (1.0 - (distance / BLACK_HOLE_RADIUS));
                        livingEntity.damage(damage, caster);
                    }
                    
                    // Black hole particle effects on entity
                    entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 3, 0.3, 0.3, 0.3, 0.5);
                    entity.getWorld().spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 2, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }
        
        // Black hole center effects
        playBlackHoleCenterEffects(center, ticks);
        
        // Sound effects
        if (ticks % 10 == 0) { // Every half second
            center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 2.0f, 0.3f);
            center.getWorld().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.5f, 0.5f);
        }
    }
    
    /**
     * Play black hole creation effects
     */
    private void playBlackHoleCreationEffects(Location center) {
        // Initial explosion
        center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center, 5, 2.0, 2.0, 2.0, 0);
        
        // Portal opening effect
        center.getWorld().spawnParticle(Particle.PORTAL, center, 100, 3.0, 3.0, 3.0, 2.0);
        
        // Dragon breath (dark energy)
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 50, 2.5, 2.5, 2.5, 0.1);
        
        // Sounds
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.3f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.5f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.3f);
    }
    
    /**
     * Play ongoing black hole center effects
     */
    private void playBlackHoleCenterEffects(Location center, int ticks) {
        // Rotating portal particles
        for (int i = 0; i < 8; i++) {
            double angle = (ticks * 0.2) + (i * Math.PI / 4);
            double radius = 2.0 + Math.sin(ticks * 0.1) * 0.5;
            
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + Math.sin(ticks * 0.15) * 1.0;
            
            Location particleLocation = new Location(center.getWorld(), x, y, z);
            
            center.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 3, 0.1, 0.1, 0.1, 1.0);
            center.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLocation, 1, 0.1, 0.1, 0.1, 0.02);
        }
        
        // Central void effect
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 8, 0.5, 0.5, 0.5, 0.02);
        
        // Pulsing dark energy
        if (ticks % 20 == 0) { // Every second
            center.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, center, 20, 4.0, 4.0, 4.0, 0.2);
        }
    }
    
    /**
     * End black hole with final explosion
     */
    private void endBlackHole(Location center, List<Entity> affectedEntities) {
        // Final massive explosion effect
        center.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, center, 3, 1.0, 1.0, 1.0, 0);
        center.getWorld().spawnParticle(Particle.PORTAL, center, 200, 5.0, 5.0, 5.0, 3.0);
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center, 100, 4.0, 4.0, 4.0, 0.2);
        
        // Final damage to all affected entities
        for (Entity entity : affectedEntities) {
            if (entity.isValid() && entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                
                // Final void damage
                livingEntity.damage(10.0);
                
                // Launch effect
                Vector launchVector = new Vector(
                    (Math.random() - 0.5) * 2.0,
                    1.5 + Math.random(),
                    (Math.random() - 0.5) * 2.0
                );
                entity.setVelocity(launchVector);
                
                // Final particle effect on entity
                entity.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, entity.getLocation(), 10);
            }
        }
        
        // Final sounds
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_DEATH, 2.0f, 0.3f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 2.0f, 0.3f);
        center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 3.0f, 0.2f);
        
        // Broadcast black hole end
        Bukkit.broadcastMessage(ChatColor.DARK_RED + "🌀 The black hole has collapsed...");
    }
    
    /**
     * Get target location for black hole
     */
    private Location getTargetLocation(Player player, double maxRange) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();
        
        // Use ray tracing if available
        if (ComprehensiveVersionSupport.isAtLeast(1, 13)) {
            org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceBlocks(
                eyeLocation, direction, maxRange, 
                org.bukkit.FluidCollisionMode.NEVER, true);
            
            if (result != null && result.getHitPosition() != null) {
                return result.getHitPosition().toLocation(player.getWorld());
            }
        }
        
        // Fallback method
        return eyeLocation.add(direction.multiply(20.0));
    }
    
    /**
     * Check if item is Apocalypse Head
     */
    public boolean isApocalypseHead(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return false;
        }
        
        return lore.stream().anyMatch(line -> 
            ChatColor.stripColor(line).contains(APOCALYPSE_HEAD_KEY));
    }
    
    /**
     * Check if player has required boss kills
     */
    private boolean hasRequiredBossKills(Player player) {
        // Check if player has defeated both Dragon and Wither
        // This could be tracked via advancements or custom tracking
        
        // For now, check if they have both boss heads in inventory
        boolean hasDragonHead = false;
        boolean hasWitherHead = false;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                String headKey = plugin.getHeadManager().getHeadKey(item);
                if ("ender_dragon".equals(headKey)) {
                    hasDragonHead = true;
                } else if ("wither".equals(headKey)) {
                    hasWitherHead = true;
                }
            }
        }
        
        return hasDragonHead && hasWitherHead;
    }
    
    /**
     * Check if player is near a Dark Altar
     */
    private boolean isNearDarkAltar(Player player) {
        Location playerLoc = player.getLocation();
        
        // Check for obsidian ritual formation
        // Simple check: obsidian blocks in a pattern around player
        int obsidianCount = 0;
        
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location checkLoc = playerLoc.clone().add(x, -1, z);
                if (checkLoc.getBlock().getType() == Material.OBSIDIAN) {
                    obsidianCount++;
                }
            }
        }
        
        // Require at least 9 obsidian blocks in formation
        return obsidianCount >= 9;
    }
    
    /**
     * Play epic crafting effects
     */
    private void playApocalypseCraftingEffects(Player player) {
        Location location = player.getLocation();
        
        // Epic particle explosion
        location.getWorld().spawnParticle(Particle.EXPLOSION_HUGE, location, 3, 2.0, 2.0, 2.0, 0);
        location.getWorld().spawnParticle(Particle.PORTAL, location, 100, 3.0, 3.0, 3.0, 2.0);
        location.getWorld().spawnParticle(Particle.DRAGON_BREATH, location, 50, 2.0, 2.0, 2.0, 0.1);
        location.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, location, 80, 3.0, 3.0, 3.0, 0.5);
        
        // Lightning effect
        location.getWorld().strikeLightningEffect(location);
        
        // Epic sounds
        location.getWorld().playSound(location, Sound.ENTITY_ENDER_DRAGON_GROWL, 2.0f, 0.5f);
        location.getWorld().playSound(location, Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        location.getWorld().playSound(location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 0.5f);
    }
    
    /**
     * Show Apocalypse Head recipe to player
     */
    public void showApocalypseRecipe(Player player) {
        player.sendMessage(ChatColor.DARK_RED + "§l" + "=".repeat(40));
        player.sendMessage(ChatColor.DARK_RED + "§l    🌀 APOCALYPSE HEAD FUSION RECIPE");
        player.sendMessage(ChatColor.DARK_RED + "§l" + "=".repeat(40));
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "🏺 Crafting Pattern:");
        player.sendMessage(ChatColor.GRAY + "  [🐲] [⭐] [💀]");
        player.sendMessage(ChatColor.GRAY + "  [🖤] [💎] [🖤]");
        player.sendMessage(ChatColor.GRAY + "  [⭐] [🖤] [⭐]");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "📋 Materials Required:");
        player.sendMessage(ChatColor.WHITE + "  🐲 = " + ChatColor.LIGHT_PURPLE + "Dragon Head");
        player.sendMessage(ChatColor.WHITE + "  💀 = " + ChatColor.GRAY + "Wither Skeleton Skull");
        player.sendMessage(ChatColor.WHITE + "  ⭐ = " + ChatColor.YELLOW + "Nether Star (x3)");
        player.sendMessage(ChatColor.WHITE + "  🖤 = " + ChatColor.DARK_PURPLE + "Obsidian (x4)");
        player.sendMessage(ChatColor.WHITE + "  💎 = " + ChatColor.AQUA + "End Crystal");
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "⚠ Requirements:");
        player.sendMessage(ChatColor.GRAY + "• Must have killed both Dragon and Wither");
        player.sendMessage(ChatColor.GRAY + "• Must craft at Dark Altar (obsidian formation)");
        player.sendMessage(ChatColor.GRAY + "• Requires 3 Nether Stars (multiple Wither kills)");
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_RED + "🌀 Result: APOCALYPSE HEAD");
        player.sendMessage(ChatColor.RED + "• Black Hole ability (15-block radius)");
        player.sendMessage(ChatColor.RED + "• Pulls entities and deals void damage");
        player.sendMessage(ChatColor.RED + "• Ultimate endgame power");
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_RED + "§l" + "=".repeat(40));
    }
    
    /**
     * Give Apocalypse Head to player (admin command)
     */
    public boolean giveApocalypseHead(Player player) {
        ItemStack apocalypseHead = createApocalypseHead();
        
        java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(apocalypseHead);
        
        if (leftover.isEmpty()) {
            player.sendMessage(ChatColor.DARK_RED + "🌀 You received the APOCALYPSE HEAD!");
            player.sendMessage(ChatColor.RED + "Ultimate power is now yours...");
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "❌ Inventory full! Could not give Apocalypse Head.");
            return false;
        }
    }
    
    /**
     * Cleanup Apocalypse Head system
     */
    public void cleanup() {
        // Remove custom recipe
        if (ComprehensiveVersionSupport.isAtLeast(1, 12)) {
            try {
                Bukkit.removeRecipe(new NamespacedKey(plugin, "apocalypse_head"));
            } catch (Exception e) {
                // Recipe may not exist
            }
        }
    }
}