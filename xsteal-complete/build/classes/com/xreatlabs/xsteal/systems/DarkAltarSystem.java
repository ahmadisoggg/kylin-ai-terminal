package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Dark Altar System for XSteal
 * Provides special crafting locations for legendary items like Apocalypse Head
 * 
 * Features:
 * - Obsidian ritual formation detection
 * - Dark energy effects and ambiance
 * - Required for fusion crafting
 * - Visual and audio feedback
 */
public class DarkAltarSystem implements Listener {
    
    private final XSteal plugin;
    private final Map<Location, DarkAltar> activeAltars;
    
    // Altar configuration
    private static final int ALTAR_SIZE = 5; // 5x5 obsidian formation
    private static final String ALTAR_METADATA_KEY = "xsteal_dark_altar";
    
    public DarkAltarSystem(XSteal plugin) {
        this.plugin = plugin;
        this.activeAltars = new HashMap<>();
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Start altar detection task
        startAltarDetectionTask();
    }
    
    /**
     * Check if location is a valid Dark Altar
     */
    public boolean isDarkAltar(Location location) {
        return isValidAltarFormation(location);
    }
    
    /**
     * Check if player is near a Dark Altar
     */
    public boolean isPlayerNearDarkAltar(Player player) {
        Location playerLocation = player.getLocation();
        
        // Check in a radius around player
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                Location checkLocation = playerLocation.clone().add(x, 0, z);
                if (isDarkAltar(checkLocation)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if location has valid altar formation
     */
    private boolean isValidAltarFormation(Location center) {
        // Check for 5x5 obsidian formation with specific pattern
        /*
         * Formation (top view):
         * O O O O O
         * O . . . O
         * O . C . O  (C = center, can be any block)
         * O . . . O
         * O O O O O
         */
        
        // Check corners and edges
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location checkLoc = center.clone().add(x, -1, z);
                
                // Corners and edges must be obsidian
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    if (checkLoc.getBlock().getType() != Material.OBSIDIAN) {
                        return false;
                    }
                }
                // Inner area can be any block (allows for decoration)
            }
        }
        
        // Check for central obsidian pillar (optional enhancement)
        Location centralPillar = center.clone().add(0, 0, 0);
        if (centralPillar.getBlock().getType() == Material.OBSIDIAN) {
            // Enhanced altar with central pillar
            return true;
        }
        
        return true; // Basic formation is valid
    }
    
    /**
     * Create a Dark Altar at location
     */
    public void createDarkAltar(Location center, Player creator) {
        if (isDarkAltar(center)) {
            DarkAltar altar = new DarkAltar(center, creator.getUniqueId());
            activeAltars.put(center, altar);
            
            // Altar creation effects
            playAltarCreationEffects(center);
            
            creator.sendMessage("§5§l⚫ Dark Altar activated!");
            creator.sendMessage("§7You can now perform legendary fusion crafting here");
            
            plugin.getPluginLogger().info("Dark Altar created by " + creator.getName() + " at " + 
                center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());
        }
    }
    
    /**
     * Handle block placement for altar detection
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if obsidian was placed
        if (event.getBlock().getType() != Material.OBSIDIAN) {
            return;
        }
        
        Player player = event.getPlayer();
        Location blockLocation = event.getBlock().getLocation();
        
        // Check if this completes a Dark Altar formation
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location centerLoc = blockLocation.clone().add(x, 1, z);
                if (isValidAltarFormation(centerLoc) && !activeAltars.containsKey(centerLoc)) {
                    // New altar detected
                    createDarkAltar(centerLoc, player);
                    break;
                }
            }
        }
    }
    
    /**
     * Handle crafting at Dark Altar
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDarkAltarCrafting(CraftItemEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if crafting legendary item
        if (isLegendaryCraft(event)) {
            if (!isPlayerNearDarkAltar(player)) {
                event.setCancelled(true);
                player.sendMessage("§c❌ Legendary fusion requires a Dark Altar!");
                player.sendMessage("§7Create a 5x5 obsidian formation to craft legendary items");
                return;
            }
            
            // Find nearest altar and activate it
            DarkAltar nearestAltar = findNearestAltar(player.getLocation());
            if (nearestAltar != null) {
                nearestAltar.activateCrafting(player);
            }
        }
    }
    
    /**
     * Check if this is a legendary craft (Apocalypse Head)
     */
    private boolean isLegendaryCraft(CraftItemEvent event) {
        // Check if result is Apocalypse Head or other legendary items
        return plugin.getApocalypseHeadSystem().isApocalypseHead(event.getRecipe().getResult());
    }
    
    /**
     * Find nearest Dark Altar to location
     */
    private DarkAltar findNearestAltar(Location location) {
        DarkAltar nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Map.Entry<Location, DarkAltar> entry : activeAltars.entrySet()) {
            double distance = entry.getKey().distance(location);
            if (distance < nearestDistance && distance <= 10.0) {
                nearest = entry.getValue();
                nearestDistance = distance;
            }
        }
        
        return nearest;
    }
    
    /**
     * Play altar creation effects
     */
    private void playAltarCreationEffects(Location center) {
        // Dark energy burst
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.add(0, 2, 0), 50, 2.0, 1.0, 2.0, 0.1);
        center.getWorld().spawnParticle(Particle.PORTAL, center, 80, 3.0, 2.0, 3.0, 1.0);
        center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 30, 2.5, 1.5, 2.5, 0.05);
        
        // Lightning effect
        center.getWorld().strikeLightningEffect(center);
        
        // Ominous sounds
        center.getWorld().playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.5f, 0.3f);
        center.getWorld().playSound(center, Sound.ENTITY_WITHER_AMBIENT, 1.5f, 0.2f);
        center.getWorld().playSound(center, Sound.BLOCK_PORTAL_TRIGGER, 2.0f, 0.5f);
    }
    
    /**
     * Start task to maintain altar effects
     */
    private void startAltarDetectionTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Update altar effects and check for destroyed altars
                activeAltars.entrySet().removeIf(entry -> {
                    Location altarLoc = entry.getKey();
                    DarkAltar altar = entry.getValue();
                    
                    // Check if altar formation still exists
                    if (!isValidAltarFormation(altarLoc)) {
                        // Altar destroyed
                        playAltarDestructionEffects(altarLoc);
                        return true;
                    }
                    
                    // Play ambient altar effects
                    if (Math.random() < 0.3) { // 30% chance each check
                        playAltarAmbientEffects(altarLoc);
                    }
                    
                    return false;
                });
            }
        }.runTaskTimer(plugin, 20L, 100L); // Every 5 seconds
    }
    
    /**
     * Play ambient altar effects
     */
    private void playAltarAmbientEffects(Location center) {
        // Subtle dark energy
        center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.add(0, 1, 0), 5, 1.0, 0.5, 1.0, 0.02);
        center.getWorld().spawnParticle(Particle.PORTAL, center, 10, 1.5, 0.8, 1.5, 0.5);
        
        // Occasional ominous sound
        if (Math.random() < 0.1) { // 10% chance
            center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.5f);
        }
    }
    
    /**
     * Play altar destruction effects
     */
    private void playAltarDestructionEffects(Location center) {
        // Dark energy dissipation
        center.getWorld().spawnParticle(Particle.SMOKE_LARGE, center, 30, 2.0, 1.0, 2.0, 0.1);
        center.getWorld().spawnParticle(Particle.PORTAL, center, 20, 2.0, 1.0, 2.0, 1.0);
        
        // Destruction sound
        center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        
        plugin.getPluginLogger().info("Dark Altar destroyed at " + 
            center.getBlockX() + "," + center.getBlockY() + "," + center.getBlockZ());
    }
    
    /**
     * Show how to create Dark Altar
     */
    public void showAltarInstructions(Player player) {
        player.sendMessage("§5§l═══ DARK ALTAR CREATION ═══");
        player.sendMessage("");
        player.sendMessage("§7Create a 5x5 obsidian formation:");
        player.sendMessage("§8O O O O O");
        player.sendMessage("§8O . . . O");
        player.sendMessage("§8O . X . O  §7(X = center, any block)");
        player.sendMessage("§8O . . . O");
        player.sendMessage("§8O O O O O");
        player.sendMessage("");
        player.sendMessage("§7O = Obsidian blocks");
        player.sendMessage("§7. = Any block (can be air)");
        player.sendMessage("");
        player.sendMessage("§5💡 Tips:");
        player.sendMessage("§7• Place obsidian in the pattern above");
        player.sendMessage("§7• The altar will activate automatically");
        player.sendMessage("§7• Dark energy effects will appear");
        player.sendMessage("§7• Required for Apocalypse Head crafting");
        player.sendMessage("");
        player.sendMessage("§c⚠ Warning: Altars are destroyed if obsidian is removed!");
    }
    
    /**
     * Dark Altar data class
     */
    public static class DarkAltar {
        private final Location center;
        private final java.util.UUID creator;
        private final long creationTime;
        private int usageCount;
        
        public DarkAltar(Location center, java.util.UUID creator) {
            this.center = center;
            this.creator = creator;
            this.creationTime = System.currentTimeMillis();
            this.usageCount = 0;
        }
        
        public void activateCrafting(Player player) {
            usageCount++;
            
            // Play crafting activation effects
            center.getWorld().spawnParticle(Particle.DRAGON_BREATH, center.add(0, 2, 0), 30, 1.5, 1.0, 1.5, 0.05);
            center.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, center, 50, 2.0, 1.5, 2.0, 0.3);
            
            center.getWorld().playSound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 0.5f);
            center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_AMBIENT, 1.0f, 0.3f);
            
            player.sendMessage("§5⚫ The Dark Altar responds to your fusion...");
        }
        
        // Getters
        public Location getCenter() { return center; }
        public java.util.UUID getCreator() { return creator; }
        public long getCreationTime() { return creationTime; }
        public int getUsageCount() { return usageCount; }
    }
    
    /**
     * Cleanup Dark Altar system
     */
    public void cleanup() {
        activeAltars.clear();
    }
}