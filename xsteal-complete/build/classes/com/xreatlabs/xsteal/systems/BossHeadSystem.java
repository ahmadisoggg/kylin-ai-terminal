package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Arrays;

/**
 * Special boss head acquisition system
 * Dragon and Wither heads require special creeper arrow mechanics
 * Based on enhanced PSD1 mechanics for boss encounters
 */
public class BossHeadSystem implements Listener {
    
    private final XSteal plugin;
    
    // Metadata keys for tracking
    private static final String CREEPER_ARROW_KEY = "xsteal_creeper_arrow";
    private static final String BOSS_KILL_TRACKER_KEY = "xsteal_boss_kill";
    
    public BossHeadSystem(XSteal plugin) {
        this.plugin = plugin;
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Handle boss entity deaths with special requirements
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossDeath(EntityDeathEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Entity killed = event.getEntity();
        
        // Check if it's a boss entity
        if (!isBossEntity(killed.getType())) {
            return;
        }
        
        // Check if killed by creeper arrow
        if (!wasKilledByCreeperArrow(killed)) {
            plugin.getPluginLogger().debug("Boss " + killed.getType() + " not killed by creeper arrow");
            return;
        }
        
        // Get boss head
        ItemStack bossHead = plugin.getHeadManager().getHeadForEntity(killed);
        if (bossHead == null) {
            plugin.getPluginLogger().warning("No boss head configured for " + killed.getType());
            return;
        }
        
        // Special boss head drop
        handleBossHeadDrop(event, bossHead, killed);
        
        plugin.getPluginLogger().info("🏆 Boss " + killed.getType() + " killed with creeper arrow - dropped boss head!");
    }
    
    /**
     * Handle creeper arrow hits on entities
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreeperArrowHit(ProjectileHitEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if it's a creeper arrow
        if (!(event.getEntity() instanceof Arrow)) {
            return;
        }
        
        Arrow arrow = (Arrow) event.getEntity();
        if (!isCreeperArrow(arrow)) {
            return;
        }
        
        // Check if it hit an entity
        if (event.getHitEntity() == null) {
            return;
        }
        
        Entity hitEntity = event.getHitEntity();
        
        // Mark entity as hit by creeper arrow
        hitEntity.setMetadata(BOSS_KILL_TRACKER_KEY, new FixedMetadataValue(plugin, System.currentTimeMillis()));
        
        // Special effects for creeper arrow hit
        if (isBossEntity(hitEntity.getType())) {
            hitEntity.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_NORMAL, 
                hitEntity.getLocation(), 10, 1.0, 1.0, 1.0, 0.1);
            hitEntity.getWorld().playSound(hitEntity.getLocation(), 
                org.bukkit.Sound.ENTITY_CREEPER_PRIMED, 1.0f, 1.5f);
            
            // Notify shooter
            if (arrow.getShooter() instanceof Player) {
                Player shooter = (Player) arrow.getShooter();
                shooter.sendMessage("§a⚡ Creeper arrow hit " + hitEntity.getType().name() + "!");
                shooter.sendMessage("§7Kill it now to get the boss head!");
            }
        }
        
        plugin.getPluginLogger().debug("Creeper arrow hit " + hitEntity.getType());
    }
    
    /**
     * Check if entity was killed by creeper arrow
     */
    private boolean wasKilledByCreeperArrow(Entity entity) {
        if (!entity.hasMetadata(BOSS_KILL_TRACKER_KEY)) {
            return false;
        }
        
        // Check if the hit was recent (within last 30 seconds)
        long hitTime = entity.getMetadata(BOSS_KILL_TRACKER_KEY).get(0).asLong();
        long currentTime = System.currentTimeMillis();
        
        return (currentTime - hitTime) < 30000; // 30 seconds
    }
    
    /**
     * Check if entity is a boss entity
     */
    private boolean isBossEntity(EntityType entityType) {
        switch (entityType) {
            case ENDER_DRAGON:
            case WITHER:
                return true;
            default:
                // Check for warden (1.19+)
                try {
                    if (entityType == EntityType.valueOf("WARDEN")) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    // Warden not available in this version
                }
                return false;
        }
    }
    
    /**
     * Check if arrow is a creeper arrow
     */
    private boolean isCreeperArrow(Arrow arrow) {
        return arrow.hasMetadata(CREEPER_ARROW_KEY);
    }
    
    /**
     * Handle special boss head drop
     */
    private void handleBossHeadDrop(EntityDeathEvent event, ItemStack bossHead, Entity killed) {
        // Clear all vanilla drops for boss entities
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Drop the boss head with special effects
        Location dropLocation = killed.getLocation();
        dropLocation.getWorld().dropItemNaturally(dropLocation, bossHead);
        
        // Epic boss head drop effects
        playBossHeadDropEffects(dropLocation, killed.getType());
        
        // Broadcast boss head drop
        String bossName = killed.getType().name().toLowerCase().replace("_", " ");
        String headName = bossHead.getItemMeta() != null ? 
            bossHead.getItemMeta().getDisplayName() : bossName + " head";
        
        plugin.getServer().broadcastMessage("§6§l🏆 LEGENDARY DROP!");
        plugin.getServer().broadcastMessage("§e⚡ A " + bossName + " was slain with a creeper arrow!");
        plugin.getServer().broadcastMessage("§a" + ChatColor.translateAlternateColorCodes('&', headName) + " §ahas been dropped!");
    }
    
    /**
     * Play epic effects for boss head drops
     */
    private void playBossHeadDropEffects(Location location, EntityType bossType) {
        // Epic particles
        location.getWorld().spawnParticle(org.bukkit.Particle.DRAGON_BREATH, 
            location.add(0, 2, 0), 50, 2.0, 2.0, 2.0, 0.1);
        
        location.getWorld().spawnParticle(org.bukkit.Particle.FIREWORKS_SPARK, 
            location, 100, 3.0, 3.0, 3.0, 0.2);
        
        location.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTMENT_TABLE, 
            location, 80, 2.5, 2.5, 2.5, 0.15);
        
        // Epic sounds
        location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 0.5f);
        location.getWorld().playSound(location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 2.0f, 1.0f);
        location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.8f);
        
        // Boss-specific effects
        switch (bossType) {
            case ENDER_DRAGON:
                location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_DEATH, 1.0f, 1.0f);
                break;
            case WITHER:
                location.getWorld().playSound(location, org.bukkit.Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                break;
            default:
                // Warden or other boss
                if (com.xreatlabs.xsteal.utils.ComprehensiveVersionSupport.isAtLeast(1, 19)) {
                    try {
                        location.getWorld().playSound(location, org.bukkit.Sound.valueOf("ENTITY_WARDEN_DEATH"), 1.0f, 1.0f);
                    } catch (IllegalArgumentException e) {
                        // Warden sounds not available
                    }
                }
                break;
        }
    }
    
    /**
     * Create creeper arrow item
     */
    public ItemStack createCreeperArrow() {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "§lCreeper Arrow");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A special arrow infused with creeper essence",
                "",
                ChatColor.YELLOW + "⚡ Special Properties:",
                ChatColor.GRAY + "• Required to get boss heads",
                ChatColor.GRAY + "• Hit Ender Dragon/Wither/Warden with this arrow",
                ChatColor.GRAY + "• Then kill them to get their head",
                "",
                ChatColor.GREEN + "▶ Shoot at boss mobs before killing",
                ChatColor.RED + "⚠ Required for boss head drops!",
                ChatColor.DARK_GRAY + "XSteal Boss Arrow"
            ));
            arrow.setItemMeta(meta);
        }
        
        return arrow;
    }
    
    /**
     * Check if arrow is a creeper arrow
     */
    public boolean isCreeperArrow(ItemStack arrow) {
        if (arrow == null || arrow.getType() != Material.ARROW) {
            return false;
        }
        
        ItemMeta meta = arrow.getItemMeta();
        if (meta == null || meta.getLore() == null) {
            return false;
        }
        
        return meta.getLore().stream().anyMatch(line -> 
            ChatColor.stripColor(line).contains("XSteal Boss Arrow"));
    }
    
    /**
     * Handle arrow shooting to mark creeper arrows
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowShoot(org.bukkit.event.entity.EntityShootBowEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Check if shooter is a player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Check if using creeper arrow
        ItemStack arrow = null;
        
        // Try to get arrow from inventory (this is complex in different versions)
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isCreeperArrow(item)) {
                arrow = item;
                break;
            }
        }
        
        if (arrow != null && event.getProjectile() instanceof Arrow) {
            Arrow shotArrow = (Arrow) event.getProjectile();
            
            // Mark as creeper arrow
            shotArrow.setMetadata(CREEPER_ARROW_KEY, new FixedMetadataValue(plugin, true));
            
            // Enhanced arrow effects
            shotArrow.setCritical(true);
            shotArrow.setGlowing(true);
            
            player.sendMessage("§a⚡ Creeper arrow fired!");
            player.sendMessage("§7Hit a boss mob to mark it for head dropping!");
            
            plugin.getPluginLogger().debug("Player " + player.getName() + " fired creeper arrow");
        }
    }
    
    /**
     * Give creeper arrow to player (admin command)
     */
    public boolean giveCreeperArrow(Player player, int amount) {
        if (amount <= 0 || amount > 64) {
            return false;
        }
        
        ItemStack creeperArrows = createCreeperArrow();
        creeperArrows.setAmount(amount);
        
        // Give to player
        java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(creeperArrows);
        
        if (!leftover.isEmpty()) {
            // Drop excess arrows
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        
        player.sendMessage(ChatColor.GREEN + "✅ Received " + amount + " Creeper Arrow" + (amount > 1 ? "s" : "") + "!");
        player.sendMessage(ChatColor.GRAY + "Use these to mark boss mobs before killing them");
        
        return true;
    }
}