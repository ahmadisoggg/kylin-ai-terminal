package com.xreatlabs.xsteal.gui;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.heads.HeadManager.HeadData;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * HeadsGUI - Interactive GUI for viewing head powers and abilities
 * Provides a comprehensive interface for exploring all available mob heads
 */
public class HeadsGUI implements Listener {
    
    private final XSteal plugin;
    private final Map<UUID, String> playerCurrentCategory;
    private final Map<UUID, Integer> playerCurrentPage;
    
    // GUI Configuration
    private static final int ITEMS_PER_PAGE = 45; // 9x5 inventory
    private static final String GUI_TITLE = "§6§lXSteal - Mob Head Powers";
    
    public HeadsGUI(XSteal plugin) {
        this.plugin = plugin;
        this.playerCurrentCategory = new HashMap<>();
        this.playerCurrentPage = new HashMap<>();
        
        // Register listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * Open the main heads GUI for a player
     */
    public void openHeadsGUI(Player player) {
        openHeadsGUI(player, null, 0);
    }
    
    /**
     * Open heads GUI with specific category and page
     */
    public void openHeadsGUI(Player player, String category, int page) {
        if (!plugin.isPluginReady()) {
            player.sendMessage(ChatColor.RED + "XSteal is still loading, please wait...");
            return;
        }
        
        // Update player's current view
        playerCurrentCategory.put(player.getUniqueId(), category);
        playerCurrentPage.put(player.getUniqueId(), page);
        
        // Create inventory
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE + 
            (category != null ? " - " + category.toUpperCase() : ""));
        
        // Get filtered heads
        List<String> headKeys = getFilteredHeads(category);
        
        // Calculate pagination
        int totalPages = (int) Math.ceil((double) headKeys.size() / ITEMS_PER_PAGE);
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, headKeys.size());
        
        // Add heads to GUI
        for (int i = startIndex; i < endIndex; i++) {
            String headKey = headKeys.get(i);
            HeadData headData = plugin.getHeadManager().getHeadData(headKey);
            
            if (headData != null) {
                ItemStack headItem = createGUIHeadItem(headData);
                gui.setItem(i - startIndex, headItem);
            }
        }
        
        // Add navigation items
        addNavigationItems(gui, category, page, totalPages);
        
        // Add category filter items
        addCategoryFilterItems(gui);
        
        // Open GUI
        player.openInventory(gui);
        
        plugin.getPluginLogger().debug("Opened heads GUI for " + player.getName() + 
            " (category: " + category + ", page: " + page + ")");
    }
    
    /**
     * Get filtered list of heads by category
     */
    private List<String> getFilteredHeads(String category) {
        return plugin.getHeadManager().getLoadedHeadKeys().stream()
            .filter(key -> {
                if (category == null) return true;
                HeadData headData = plugin.getHeadManager().getHeadData(key);
                return headData != null && headData.getCategory().equalsIgnoreCase(category);
            })
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Create GUI item for a head
     */
    private ItemStack createGUIHeadItem(HeadData headData) {
        ItemStack headItem = plugin.getHeadManager().createHeadItem(headData.getKey());
        
        if (headItem != null) {
            ItemMeta meta = headItem.getItemMeta();
            if (meta != null) {
                // Enhanced lore for GUI
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + headData.getDescription());
                lore.add("");
                lore.add(ChatColor.YELLOW + "Category: " + ChatColor.WHITE + headData.getCategory().toUpperCase());
                
                if (headData.hasAbility()) {
                    lore.add(ChatColor.GREEN + "Ability: " + ChatColor.WHITE + headData.getAbility().getType().replace("_", " ").toUpperCase());
                    lore.add(ChatColor.GRAY + "Activation: " + headData.getAbility().getActivation().replace("_", " "));
                    
                    // Add ability parameters
                    if (!headData.getAbility().getParams().isEmpty()) {
                        lore.add("");
                        lore.add(ChatColor.AQUA + "Ability Details:");
                        for (Map.Entry<String, Object> param : headData.getAbility().getParams().entrySet()) {
                            lore.add(ChatColor.GRAY + "• " + param.getKey() + ": " + ChatColor.WHITE + param.getValue());
                        }
                    }
                }
                
                if (headData.hasBossAbilities()) {
                    lore.add("");
                    lore.add(ChatColor.GOLD + "§lBOSS HEAD - COMBO ABILITIES:");
                    for (var bossAbility : headData.getBossAbilities()) {
                        lore.add(ChatColor.GOLD + "• " + bossAbility.getActivation().replace("_", " ").toUpperCase() + 
                            ": " + ChatColor.YELLOW + bossAbility.getName());
                    }
                }
                
                lore.add("");
                lore.add(ChatColor.GREEN + "▶ Click to get this head!");
                lore.add(ChatColor.DARK_GRAY + "Requires: xsteal.admin.give");
                
                meta.setLore(lore);
                headItem.setItemMeta(meta);
            }
        }
        
        return headItem;
    }
    
    /**
     * Add navigation items to GUI
     */
    private void addNavigationItems(Inventory gui, String category, int page, int totalPages) {
        // Previous page
        if (page > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta meta = prevPage.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "◀ Previous Page");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Page " + page + "/" + totalPages));
            prevPage.setItemMeta(meta);
            gui.setItem(45, prevPage);
        }
        
        // Next page
        if (page < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Next Page ▶");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Page " + (page + 2) + "/" + totalPages));
            nextPage.setItemMeta(meta);
            gui.setItem(53, nextPage);
        }
        
        // Close button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "✖ Close");
        close.setItemMeta(closeMeta);
        gui.setItem(49, close);
        
        // Info item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "ℹ XSteal Information");
        infoMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Total Heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount(),
            ChatColor.GRAY + "Category: " + ChatColor.WHITE + (category != null ? category.toUpperCase() : "ALL"),
            ChatColor.GRAY + "Page: " + ChatColor.WHITE + (page + 1) + "/" + totalPages,
            "",
            ChatColor.YELLOW + "Get heads by having charged",
            ChatColor.YELLOW + "creepers kill mobs!"
        ));
        info.setItemMeta(infoMeta);
        gui.setItem(47, info);
    }
    
    /**
     * Add category filter items to GUI
     */
    private void addCategoryFilterItems(Inventory gui) {
        // Get all categories
        Set<String> categories = plugin.getHeadManager().getLoadedHeadKeys().stream()
            .map(key -> plugin.getHeadManager().getHeadData(key))
            .filter(Objects::nonNull)
            .map(HeadData::getCategory)
            .collect(Collectors.toSet());
        
        // Category filter items
        Map<String, Material> categoryMaterials = new HashMap<>();
        categoryMaterials.put("hostile", Material.IRON_SWORD);
        categoryMaterials.put("boss", Material.NETHER_STAR);
        categoryMaterials.put("passive", Material.WHEAT);
        categoryMaterials.put("aquatic", Material.FISHING_ROD);
        categoryMaterials.put("nether", Material.NETHERRACK);
        categoryMaterials.put("end", Material.ENDER_PEARL);
        categoryMaterials.put("constructed", Material.IRON_BLOCK);
        categoryMaterials.put("utility", Material.CHEST);
        
        int slot = 46;
        for (String cat : categories) {
            if (slot > 52) break;
            
            Material material = categoryMaterials.getOrDefault(cat, Material.STONE);
            ItemStack categoryItem = new ItemStack(material);
            ItemMeta meta = categoryItem.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + "Filter: " + cat.toUpperCase());
            
            long categoryCount = plugin.getHeadManager().getLoadedHeadKeys().stream()
                .map(key -> plugin.getHeadManager().getHeadData(key))
                .filter(Objects::nonNull)
                .filter(data -> data.getCategory().equalsIgnoreCase(cat))
                .count();
            
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Heads in category: " + ChatColor.WHITE + categoryCount,
                "",
                ChatColor.GREEN + "▶ Click to filter by this category"
            ));
            categoryItem.setItemMeta(meta);
            gui.setItem(slot, categoryItem);
            slot++;
        }
        
        // All categories item
        ItemStack allItem = new ItemStack(Material.COMPASS);
        ItemMeta allMeta = allItem.getItemMeta();
        allMeta.setDisplayName(ChatColor.GOLD + "Show All Categories");
        allMeta.setLore(Arrays.asList(
            ChatColor.GRAY + "Total heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount(),
            "",
            ChatColor.GREEN + "▶ Click to show all heads"
        ));
        allItem.setItemMeta(allMeta);
        gui.setItem(48, allItem);
    }
    
    /**
     * Handle GUI clicks
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isPluginReady()) {
            return;
        }
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check if it's our GUI
        if (!event.getView().getTitle().startsWith(GUI_TITLE)) {
            return;
        }
        
        event.setCancelled(true); // Prevent item taking
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int slot = event.getSlot();
        
        // Handle navigation clicks
        if (slot == 45) { // Previous page
            handlePreviousPage(player);
        } else if (slot == 53) { // Next page
            handleNextPage(player);
        } else if (slot == 49) { // Close
            player.closeInventory();
        } else if (slot == 47) { // Info
            showDetailedInfo(player);
        } else if (slot == 48) { // All categories
            openHeadsGUI(player, null, 0);
        } else if (slot >= 46 && slot <= 52) { // Category filters
            handleCategoryFilter(player, clickedItem);
        } else if (slot < 45) { // Head items
            handleHeadClick(player, clickedItem);
        }
    }
    
    /**
     * Handle previous page navigation
     */
    private void handlePreviousPage(Player player) {
        String category = playerCurrentCategory.get(player.getUniqueId());
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        
        if (currentPage > 0) {
            openHeadsGUI(player, category, currentPage - 1);
        }
    }
    
    /**
     * Handle next page navigation
     */
    private void handleNextPage(Player player) {
        String category = playerCurrentCategory.get(player.getUniqueId());
        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        
        List<String> filteredHeads = getFilteredHeads(category);
        int totalPages = (int) Math.ceil((double) filteredHeads.size() / ITEMS_PER_PAGE);
        
        if (currentPage < totalPages - 1) {
            openHeadsGUI(player, category, currentPage + 1);
        }
    }
    
    /**
     * Handle category filter selection
     */
    private void handleCategoryFilter(Player player, ItemStack clickedItem) {
        String displayName = clickedItem.getItemMeta().getDisplayName();
        
        if (displayName.contains("Filter: ")) {
            String category = ChatColor.stripColor(displayName).replace("Filter: ", "").toLowerCase();
            openHeadsGUI(player, category, 0);
            player.sendMessage(ChatColor.YELLOW + "Filtered by category: " + category.toUpperCase());
        }
    }
    
    /**
     * Handle head item clicks
     */
    private void handleHeadClick(Player player, ItemStack clickedItem) {
        String headKey = plugin.getHeadManager().getHeadKey(clickedItem);
        
        if (headKey != null) {
            HeadData headData = plugin.getHeadManager().getHeadData(headKey);
            
            if (headData != null) {
                // Check if player has permission to receive heads
                if (player.hasPermission("xsteal.admin.give")) {
                    // Give the head to the player
                    ItemStack headToGive = plugin.getHeadManager().createHeadItem(headKey);
                    if (headToGive != null) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(headToGive);
                        
                        if (leftover.isEmpty()) {
                            String headName = ChatColor.translateAlternateColorCodes('&', headData.getDisplayName());
                            player.sendMessage(ChatColor.GREEN + "✅ Received: " + headName);
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        } else {
                            player.sendMessage(ChatColor.RED + "❌ Inventory full! Could not give head.");
                        }
                    }
                } else {
                    // Show detailed information
                    showHeadDetails(player, headData);
                }
            }
        }
    }
    
    /**
     * Show detailed information about a head
     */
    private void showHeadDetails(Player player, HeadData headData) {
        player.closeInventory();
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        String headName = ChatColor.translateAlternateColorCodes('&', headData.getDisplayName());
        player.sendMessage(ChatColor.GOLD + "📋 " + headName);
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        
        player.sendMessage(ChatColor.GRAY + headData.getDescription());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Category: " + ChatColor.WHITE + headData.getCategory().toUpperCase());
        player.sendMessage(ChatColor.YELLOW + "Head Key: " + ChatColor.WHITE + headData.getKey());
        
        if (headData.hasAbility()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "🪄 ABILITY:");
            player.sendMessage(ChatColor.WHITE + "  Type: " + headData.getAbility().getType().replace("_", " ").toUpperCase());
            player.sendMessage(ChatColor.WHITE + "  Activation: " + headData.getAbility().getActivation().replace("_", " "));
            
            if (!headData.getAbility().getParams().isEmpty()) {
                player.sendMessage(ChatColor.AQUA + "  Parameters:");
                for (Map.Entry<String, Object> param : headData.getAbility().getParams().entrySet()) {
                    player.sendMessage(ChatColor.GRAY + "    • " + param.getKey() + ": " + ChatColor.WHITE + param.getValue());
                }
            }
        }
        
        if (headData.hasBossAbilities()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "👑 BOSS ABILITIES:");
            for (var bossAbility : headData.getBossAbilities()) {
                player.sendMessage(ChatColor.GOLD + "  " + bossAbility.getActivation().replace("_", " ").toUpperCase() + 
                    ": " + ChatColor.YELLOW + bossAbility.getName());
                
                if (!bossAbility.getParams().isEmpty()) {
                    for (Map.Entry<String, Object> param : bossAbility.getParams().entrySet()) {
                        player.sendMessage(ChatColor.GRAY + "    • " + param.getKey() + ": " + ChatColor.WHITE + param.getValue());
                    }
                }
            }
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "💡 How to get this head:");
        player.sendMessage(ChatColor.GRAY + "1. Find a creeper and charge it with lightning");
        player.sendMessage(ChatColor.GRAY + "2. Lead the charged creeper to a " + headData.getKey().replace("_", " "));
        player.sendMessage(ChatColor.GRAY + "3. When charged creeper kills it, the head drops!");
        player.sendMessage("");
        
        if (player.hasPermission("xsteal.admin.give")) {
            player.sendMessage(ChatColor.GREEN + "💼 Admin: Use /xsteal give " + player.getName() + " " + headData.getKey());
        }
        
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
    }
    
    /**
     * Show detailed plugin information
     */
    private void showDetailedInfo(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══ XSteal Information ═══");
        player.sendMessage(ChatColor.GRAY + "PSD1 Inspired Minecraft Plugin");
        player.sendMessage(ChatColor.GRAY + "Version: " + plugin.getDescription().getVersion());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "📊 Statistics:");
        player.sendMessage(ChatColor.GRAY + "• Total Heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount());
        player.sendMessage(ChatColor.GRAY + "• Active Abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getActiveAbilityCount());
        player.sendMessage(ChatColor.GRAY + "• Banboxed Players: " + ChatColor.WHITE + plugin.getBanBoxManager().getBanBoxPlayerCount());
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "🎮 How to Play:");
        player.sendMessage(ChatColor.GRAY + "1. Get heads via charged creeper kills");
        player.sendMessage(ChatColor.GRAY + "2. Wear heads in helmet slot");
        player.sendMessage(ChatColor.GRAY + "3. Left-click to activate abilities");
        player.sendMessage(ChatColor.GRAY + "4. Boss heads have combo abilities!");
    }
    
    /**
     * Handle GUI close
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().startsWith(GUI_TITLE)) {
            Player player = (Player) event.getPlayer();
            
            // Clean up tracking data
            playerCurrentCategory.remove(player.getUniqueId());
            playerCurrentPage.remove(player.getUniqueId());
            
            plugin.getPluginLogger().debug("Closed heads GUI for " + player.getName());
        }
    }
    
    /**
     * Clean up player data on quit
     */
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        playerCurrentCategory.remove(player.getUniqueId());
        playerCurrentPage.remove(player.getUniqueId());
    }
}