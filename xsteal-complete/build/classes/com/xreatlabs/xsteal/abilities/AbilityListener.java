package com.xreatlabs.xsteal.abilities;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles ability activation through player interactions
 * - Helmet slot wearing for passive abilities
 * - Left-click for active abilities
 * - Boss combo detection
 */
public class AbilityListener implements Listener {
    
    private final XSteal plugin;
    
    public AbilityListener(XSteal plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle left-click ability activation
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerLeftClick(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle left-click actions
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player has permission (default: true for all players)
        if (!player.hasPermission("xsteal.heads.use")) {
            return;
        }
        
        // Check if player is wearing a head in helmet slot
        if (!plugin.getConfigManager().isHelmetSlotRequired()) {
            return;
        }
        
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) {
            return;
        }
        
        // Check if helmet is a mob head
        String headKey = plugin.getHeadManager().getHeadKey(helmet);
        if (headKey == null) {
            return;
        }
        
        plugin.getPluginLogger().debug("Player " + player.getName() + " left-click with head: " + headKey);
        
        // Determine activation type
        String activationType = player.isSneaking() ? "shift_left_click" : "left_click";
        
        // Execute ability
        boolean success = plugin.getAbilityManager().executeHelmetAbility(player, headKey, activationType);
        
        if (success) {
            // Cancel the event to prevent block breaking
            if (plugin.getConfigManager().getMainConfig().getBoolean("abilities.prevent_block_breaking", true)) {
                event.setCancelled(true);
            }
            
            plugin.getPluginLogger().debug("Ability executed successfully: " + headKey + " (" + activationType + ")");
        }
    }
    
    /**
     * Handle helmet slot changes for passive abilities
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHelmetChange(InventoryClickEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if helmet slot was modified (slot 39 = helmet)
        if (event.getSlot() == 39 || event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
            // Schedule check for next tick to see what's in helmet slot
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                handleHelmetEquip(player);
            }, 1L);
        }
    }
    
    /**
     * Handle helmet equip/unequip
     */
    private void handleHelmetEquip(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        
        if (helmet != null) {
            String headKey = plugin.getHeadManager().getHeadKey(helmet);
            if (headKey != null) {
                plugin.getPluginLogger().debug("Player " + player.getName() + " equipped head: " + headKey);
                
                // Check for passive abilities
                var headData = plugin.getHeadManager().getHeadData(headKey);
                if (headData != null && headData.hasAbility()) {
                    String activationType = headData.getAbility().getActivation();
                    
                    // Activate passive abilities
                    if ("passive".equals(activationType)) {
                        plugin.getAbilityManager().executeHelmetAbility(player, headKey, "passive");
                    }
                    
                    // Send equip message
                    player.sendMessage("§6§l[XSteal] §r" + 
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()) + 
                        " §6equipped!");
                    player.sendMessage("§7" + headData.getDescription());
                    
                    if (headData.isBossHead()) {
                        player.sendMessage("§6§lBoss Head! Use combos:");
                        player.sendMessage("§e• Left-Click §7- Ability 1");
                        player.sendMessage("§e• Shift+Left-Click §7- Ability 2");
                        player.sendMessage("§e• Double Left-Click §7- Ability 3");
                    } else {
                        player.sendMessage("§7Left-click to activate ability");
                    }
                }
            }
        } else {
            // Player unequipped head
            plugin.getPluginLogger().debug("Player " + player.getName() + " unequipped head");
            
            // Remove any passive effects (this would need more sophisticated tracking)
            // For now, just send a message
            player.sendMessage("§7Head unequipped - abilities disabled");
        }
    }
    
    /**
     * Handle item switching (for passive ability management)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        // This could be used to manage abilities that depend on held items
        // For now, we focus on helmet slot abilities
    }
    
    /**
     * Handle player joining (restore passive abilities)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is wearing a head and restore passive abilities
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            handleHelmetEquip(player);
        }, 20L); // 1 second delay to ensure player is fully loaded
    }
    
    /**
     * Handle player leaving (cleanup abilities)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Clean up summoned entities for this player
        plugin.getAbilityManager().removeSummonedEntities(player);
        
        plugin.getPluginLogger().debug("Cleaned up abilities for disconnecting player: " + player.getName());
    }
    
    /**
     * Handle world changes (ability restrictions)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        String newWorldName = player.getWorld().getName();
        
        // Check if abilities are disabled in the new world
        java.util.List<String> disabledWorlds = plugin.getConfigManager().getMainConfig()
            .getStringList("general.head_drops.disabled_worlds");
        
        if (disabledWorlds.contains(newWorldName)) {
            player.sendMessage("§c§lAbilities are disabled in this world!");
            
            // Remove summoned entities
            plugin.getAbilityManager().removeSummonedEntities(player);
        } else {
            // Re-activate passive abilities if wearing a head
            handleHelmetEquip(player);
        }
    }
}