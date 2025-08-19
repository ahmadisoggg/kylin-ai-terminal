package com.xreatlabs.xsteal.abilities;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles boss head combo detection and execution
 * Detects: Left-Click, Shift+Left-Click, Double Left-Click
 * Based on PSD1's HeadSteal video boss mechanics
 */
public class BossComboListener implements Listener {
    
    private final XSteal plugin;
    private final Map<UUID, Long> lastClickTime;
    private final Map<UUID, Integer> clickCount;
    
    public BossComboListener(XSteal plugin) {
        this.plugin = plugin;
        this.lastClickTime = new HashMap<>();
        this.clickCount = new HashMap<>();
    }
    
    /**
     * Handle boss combo detection
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBossComboInput(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle left-click actions
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check boss ability permission
        if (!player.hasPermission("xsteal.ability.boss")) {
            return;
        }
        
        // Check if boss combos are enabled
        if (!plugin.getConfigManager().areBossCombosEnabled()) {
            return;
        }
        
        // Check if player is wearing a boss head
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) {
            return;
        }
        
        String headKey = plugin.getHeadManager().getHeadKey(helmet);
        if (headKey == null) {
            return;
        }
        
        var headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null || !headData.isBossHead()) {
            return;
        }
        
        plugin.getPluginLogger().debug("Boss combo input detected for " + player.getName() + " with " + headKey);
        
        // Process combo input
        String activationType = processBossComboInput(player, event);
        
        if (activationType != null) {
            // Execute boss ability
            plugin.getAbilityManager().processBossCombo(player, activationType);
            
            // Cancel event to prevent block breaking
            event.setCancelled(true);
        }
    }
    
    /**
     * Process boss combo input and determine activation type
     */
    private String processBossComboInput(Player player, PlayerInteractEvent event) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Get timing configuration
        int doubleClickWindow = plugin.getConfigManager().getDoubleClickWindow();
        int comboResetTime = plugin.getConfigManager().getComboResetTime();
        
        // Check if this is within the combo window
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null && (currentTime - lastClick) > comboResetTime) {
            // Reset combo if too much time has passed
            clickCount.put(playerId, 0);
        }
        
        // Update click tracking
        lastClickTime.put(playerId, currentTime);
        int currentClickCount = clickCount.getOrDefault(playerId, 0) + 1;
        clickCount.put(playerId, currentClickCount);
        
        // Determine activation type
        if (player.isSneaking()) {
            // Shift + Left-Click (Boss Ability 2)
            clickCount.put(playerId, 0); // Reset counter
            return "shift_left_click";
            
        } else if (lastClick != null && (currentTime - lastClick) <= doubleClickWindow) {
            // Double Left-Click detected (Boss Ability 3)
            clickCount.put(playerId, 0); // Reset counter
            return "double_left_click";
            
        } else if (currentClickCount == 1) {
            // First click - wait to see if it becomes a double click
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                // Check if it was a single click (not followed by another click)
                int finalClickCount = clickCount.getOrDefault(playerId, 0);
                if (finalClickCount == 1) {
                    // Single Left-Click (Boss Ability 1)
                    plugin.getAbilityManager().processBossCombo(player, "left_click");
                    clickCount.put(playerId, 0); // Reset counter
                }
            }, doubleClickWindow / 50L); // Convert ms to ticks
            
            return null; // Wait for potential double-click
        }
        
        return null;
    }
    
    /**
     * Handle right-click for potential future boss abilities
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle right-click actions
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player is wearing a boss head
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) {
            return;
        }
        
        String headKey = plugin.getHeadManager().getHeadKey(helmet);
        if (headKey == null) {
            return;
        }
        
        var headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null || !headData.isBossHead()) {
            return;
        }
        
        // Right-click boss abilities (if configured)
        plugin.getPluginLogger().debug("Boss right-click detected for " + player.getName() + " with " + headKey);
        
        // For now, right-click is not used in the PSD1 mechanics
        // But this provides extensibility for future boss abilities
    }
    
    /**
     * Handle helmet slot changes for ability management
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if helmet slot was modified
        if (event.getSlot() == 39) { // Helmet slot
            // Schedule passive ability check for next tick
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                activatePassiveAbilities(player);
            }, 1L);
        }
    }
    
    /**
     * Activate passive abilities for worn head
     */
    private void activatePassiveAbilities(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        
        if (helmet != null) {
            String headKey = plugin.getHeadManager().getHeadKey(helmet);
            if (headKey != null) {
                var headData = plugin.getHeadManager().getHeadData(headKey);
                if (headData != null && headData.hasAbility()) {
                    String activationType = headData.getAbility().getActivation();
                    
                    // Execute passive abilities immediately
                    if ("passive".equals(activationType)) {
                        plugin.getAbilityManager().executeHelmetAbility(player, headKey, "passive");
                    }
                }
            }
        }
    }
    
    /**
     * Handle player death (cleanup combo tracking)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getEntity();
        
        // Clear combo tracking for dead player
        lastClickTime.remove(player.getUniqueId());
        clickCount.remove(player.getUniqueId());
        
        plugin.getPluginLogger().debug("Cleared combo tracking for dead player: " + player.getName());
    }
    
    /**
     * Handle player logout (cleanup combo tracking)
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Clear combo tracking for disconnecting player
        lastClickTime.remove(player.getUniqueId());
        clickCount.remove(player.getUniqueId());
        
        plugin.getPluginLogger().debug("Cleared combo tracking for disconnecting player: " + player.getName());
    }
    
    /**
     * Send boss combo feedback to player
     */
    private void sendComboFeedback(Player player, String comboType, String abilityName) {
        // Send action bar message
        try {
            if (VersionCompatibility.isAtLeast(1, 11)) {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        "§6§l" + comboType.toUpperCase() + " §8→ §e" + abilityName));
            } else {
                player.sendMessage("§6§l" + comboType.toUpperCase() + " §8→ §e" + abilityName);
            }
        } catch (Exception e) {
            player.sendMessage("§6§l" + comboType.toUpperCase() + " §8→ §e" + abilityName);
        }
    }
}