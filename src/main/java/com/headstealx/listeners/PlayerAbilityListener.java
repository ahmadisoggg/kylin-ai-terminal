package com.headstealx.listeners;

import com.headstealx.Main;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles ability activation through player interactions
 * - Left-click air/block for regular abilities
 * - Left-click entity for combat abilities
 * - Boss ability combo detection
 */
public class PlayerAbilityListener implements Listener {
    
    private final Main plugin;
    
    public PlayerAbilityListener(Main plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle left-click interactions for ability activation
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle left-click actions
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check if player has permission
        if (!player.hasPermission("headsteal.ability.use")) {
            return;
        }
        
        // Get held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null) {
            return;
        }
        
        // Check if item is a head
        String headKey = plugin.getHeadManager().getHeadKey(heldItem);
        if (headKey == null) {
            return;
        }
        
        // Check if abilities are enabled
        if (!plugin.getConfig().getBoolean("abilities.unlimited_use", true) && 
            plugin.getConfig().getBoolean("abilities.use_cooldowns", false)) {
            // Cooldown system is enabled, check in AbilityManager
        }
        
        // Check combat usage
        if (!plugin.getConfig().getBoolean("abilities.combat_usage", true)) {
            // Would check if player is in combat
        }
        
        // Check sneak usage
        if (!plugin.getConfig().getBoolean("abilities.sneak_usage", true) && player.isSneaking()) {
            player.sendMessage("§cCannot use abilities while sneaking!");
            return;
        }
        
        plugin.getPluginLogger().debug("Player " + player.getName() + " attempting to use ability: " + headKey);
        
        // Execute ability
        boolean success = plugin.getAbilityManager().execute(player, headKey, event);
        
        if (success) {
            // Cancel the event to prevent block breaking/interaction
            event.setCancelled(true);
            
            plugin.getPluginLogger().debug("Ability " + headKey + " executed successfully for " + player.getName());
        } else {
            plugin.getPluginLogger().debug("Ability " + headKey + " failed to execute for " + player.getName());
        }
    }
    
    /**
     * Handle right-click interactions for boss abilities
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRightClick(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle right-click actions for boss abilities
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Check boss ability permission
        if (!player.hasPermission("headsteal.ability.boss")) {
            return;
        }
        
        // Get held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null) {
            return;
        }
        
        // Check if item is a boss head
        String headKey = plugin.getHeadManager().getHeadKey(heldItem);
        if (headKey == null) {
            return;
        }
        
        // Check if it's a boss head
        var headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null || !headData.isBossHead()) {
            return;
        }
        
        plugin.getPluginLogger().debug("Player " + player.getName() + " attempting boss ability (right-click): " + headKey);
        
        // Execute boss ability with right-click trigger
        boolean success = plugin.getAbilityManager().execute(player, headKey, event);
        
        if (success) {
            event.setCancelled(true);
            plugin.getPluginLogger().debug("Boss ability (right-click) " + headKey + " executed for " + player.getName());
        }
    }
    
    /**
     * Handle entity damage for combat abilities and boss abilities
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle player attackers
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getDamager();
        
        // Check permissions
        if (!player.hasPermission("headsteal.ability.use")) {
            return;
        }
        
        // Get held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null) {
            return;
        }
        
        // Check if item is a head
        String headKey = plugin.getHeadManager().getHeadKey(heldItem);
        if (headKey == null) {
            return;
        }
        
        // Get head data
        var headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null) {
            return;
        }
        
        // Check if this is a combat-triggered ability
        boolean isCombatAbility = false;
        
        if (headData.hasAbility()) {
            // Check if regular ability can be used in combat
            String abilityType = headData.getAbility().getType();
            var ability = plugin.getAbilityManager().getAbility(abilityType);
            if (ability != null && ability.canUseInCombat()) {
                isCombatAbility = true;
            }
        }
        
        if (headData.isBossHead() && headData.hasBossAbilities()) {
            // Boss heads can use abilities in combat
            isCombatAbility = true;
        }
        
        if (!isCombatAbility) {
            return;
        }
        
        plugin.getPluginLogger().debug("Player " + player.getName() + " combat ability trigger: " + headKey);
        
        // Execute ability with combat context
        boolean success = plugin.getAbilityManager().execute(player, headKey, event);
        
        if (success) {
            plugin.getPluginLogger().debug("Combat ability " + headKey + " executed for " + player.getName());
            
            // Note: We don't cancel the damage event here as some abilities might want
            // to modify damage or have the original damage still apply
        }
    }
    
    /**
     * Handle shift+left-click for boss abilities
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerShiftLeftClick(PlayerInteractEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        // Only handle shift+left-click for boss abilities
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Must be sneaking for shift+left-click
        if (!player.isSneaking()) {
            return;
        }
        
        // Check boss ability permission
        if (!player.hasPermission("headsteal.ability.boss")) {
            return;
        }
        
        // Get held item
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null) {
            return;
        }
        
        // Check if item is a boss head
        String headKey = plugin.getHeadManager().getHeadKey(heldItem);
        if (headKey == null) {
            return;
        }
        
        // Check if it's a boss head
        var headData = plugin.getHeadManager().getHeadData(headKey);
        if (headData == null || !headData.isBossHead()) {
            return;
        }
        
        plugin.getPluginLogger().debug("Player " + player.getName() + " attempting boss ability (shift+left-click): " + headKey);
        
        // Execute boss ability with shift+left-click trigger
        boolean success = plugin.getAbilityManager().execute(player, headKey, event);
        
        if (success) {
            event.setCancelled(true);
            plugin.getPluginLogger().debug("Boss ability (shift+left-click) " + headKey + " executed for " + player.getName());
        }
    }
    
    /**
     * Handle helmet slot abilities (wearing heads)
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInventoryChange(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if helmet slot was modified
        if (event.getSlot() == 39) { // Helmet slot
            // Schedule check for next tick to see what's in helmet slot
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null) {
                    String headKey = plugin.getHeadManager().getHeadKey(helmet);
                    if (headKey != null) {
                        plugin.getPluginLogger().debug("Player " + player.getName() + " equipped head: " + headKey);
                        
                        // Check for passive abilities
                        var headData = plugin.getHeadManager().getHeadData(headKey);
                        if (headData != null && headData.hasAbility()) {
                            String abilityType = headData.getAbility().getType();
                            
                            // Handle passive abilities like sprint_boost, lava_stride, etc.
                            if (isPassiveAbility(abilityType)) {
                                plugin.getAbilityManager().execute(player, headKey, null);
                            }
                        }
                    }
                }
            }, 1L);
        }
    }
    
    /**
     * Check if ability type is passive (activated by wearing)
     */
    private boolean isPassiveAbility(String abilityType) {
        switch (abilityType) {
            case "sprint_boost":
            case "lava_stride":
            case "regrowth":
            case "stealth_toggle":
                return true;
            default:
                return false;
        }
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
        
        // Clear any active abilities or cooldowns for this player
        plugin.getAbilityManager().clearCooldowns(player);
        
        plugin.getPluginLogger().debug("Cleared ability data for disconnecting player: " + player.getName());
    }
}