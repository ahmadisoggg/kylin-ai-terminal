package com.xreatlabs.xsteal.systems;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.utils.VersionCompatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * RecipeManager for XSteal
 * Manages custom recipes including player revival head recipes
 */
public class RecipeManager {
    
    private final XSteal plugin;
    
    public RecipeManager(XSteal plugin) {
        this.plugin = plugin;
        
        if (VersionCompatibility.isAtLeast(1, 12)) {
            registerCustomRecipes();
        }
    }
    
    /**
     * Register all custom recipes
     */
    private void registerCustomRecipes() {
        try {
            registerRevivalHeadRecipe();
            registerLifeCrystalRecipe();
            registerChargedCreeperEggRecipe();
            
            plugin.getPluginLogger().info("Registered custom recipes");
        } catch (Exception e) {
            plugin.getPluginLogger().warning("Failed to register some recipes: " + e.getMessage());
        }
    }
    
    /**
     * Register revival head recipe
     */
    private void registerRevivalHeadRecipe() {
        if (!VersionCompatibility.isAtLeast(1, 12)) {
            return;
        }
        
        // Create revival head item
        ItemStack revivalHead = createRevivalHead();
        
        // Create recipe
        NamespacedKey key = new NamespacedKey(plugin, "revival_head");
        ShapedRecipe recipe = new ShapedRecipe(key, revivalHead);
        
        // Set recipe pattern
        recipe.shape(
            "GDG",
            "DSD", 
            "GDG"
        );
        
        // Set ingredients
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', getPlayerHeadMaterial());
        
        // Register recipe
        Bukkit.addRecipe(recipe);
        
        plugin.getPluginLogger().info("Registered revival head recipe");
    }
    
    /**
     * Register life crystal recipe
     */
    private void registerLifeCrystalRecipe() {
        if (!VersionCompatibility.isAtLeast(1, 12)) {
            return;
        }
        
        // Create life crystal item
        ItemStack lifeCrystal = createLifeCrystal();
        
        // Create recipe
        NamespacedKey key = new NamespacedKey(plugin, "life_crystal");
        ShapedRecipe recipe = new ShapedRecipe(key, lifeCrystal);
        
        // Set recipe pattern
        recipe.shape(
            "ERE",
            "RDR",
            "ERE"
        );
        
        // Set ingredients
        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        
        // Register recipe
        Bukkit.addRecipe(recipe);
        
        plugin.getPluginLogger().info("Registered life crystal recipe");
    }
    
    /**
     * Register charged creeper egg recipe
     */
    private void registerChargedCreeperEggRecipe() {
        if (!VersionCompatibility.isAtLeast(1, 12)) {
            return;
        }
        
        // Create charged creeper spawn egg
        ItemStack chargedCreeperEgg = createChargedCreeperEgg();
        
        // Create recipe
        NamespacedKey key = new NamespacedKey(plugin, "charged_creeper_egg");
        ShapedRecipe recipe = new ShapedRecipe(key, chargedCreeperEgg);
        
        // Set recipe pattern
        recipe.shape(
            "LGL",
            "GEG",
            "LGL"
        );
        
        // Set ingredients
        recipe.setIngredient('L', Material.LIGHTNING_ROD);
        recipe.setIngredient('G', Material.GUNPOWDER);
        if (VersionCompatibility.isAtLeast(1, 13)) {
            recipe.setIngredient('E', Material.CREEPER_SPAWN_EGG);
        } else {
            recipe.setIngredient('E', Material.EGG);
        }
        
        // Register recipe
        Bukkit.addRecipe(recipe);
        
        plugin.getPluginLogger().info("Registered charged creeper egg recipe");
    }
    
    /**
     * Create revival head item
     */
    private ItemStack createRevivalHead() {
        ItemStack revivalHead = new ItemStack(getPlayerHeadMaterial());
        ItemMeta meta = revivalHead.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "§lRevival Head");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A mystical head imbued with life energy",
                "",
                ChatColor.YELLOW + "🔄 Special Properties:",
                ChatColor.GRAY + "• Can revive any banboxed player",
                ChatColor.GRAY + "• Works from any distance",
                ChatColor.GRAY + "• Consumes 1 life point to use",
                "",
                ChatColor.GREEN + "▶ Right-click to use",
                ChatColor.DARK_GRAY + "XSteal Revival Item"
            ));
            revivalHead.setItemMeta(meta);
        }
        
        return revivalHead;
    }
    
    /**
     * Create life crystal item
     */
    private ItemStack createLifeCrystal() {
        Material crystalMaterial = VersionCompatibility.isAtLeast(1, 13) ? 
            Material.HEART_OF_THE_SEA : Material.DIAMOND;
        
        ItemStack lifeCrystal = new ItemStack(crystalMaterial);
        ItemMeta meta = lifeCrystal.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "§lLife Crystal");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "A crystallized essence of life energy",
                "",
                ChatColor.YELLOW + "💚 Properties:",
                ChatColor.GRAY + "• Grants +1 life when consumed",
                ChatColor.GRAY + "• Maximum " + plugin.getLifeManager().getMaxLives() + " lives per player",
                ChatColor.GRAY + "• Rare and valuable",
                "",
                ChatColor.GREEN + "▶ Right-click to consume",
                ChatColor.DARK_GRAY + "XSteal Life Item"
            ));
            lifeCrystal.setItemMeta(meta);
        }
        
        return lifeCrystal;
    }
    
    /**
     * Create charged creeper spawn egg
     */
    private ItemStack createChargedCreeperEgg() {
        Material eggMaterial = VersionCompatibility.isAtLeast(1, 13) ? 
            Material.CREEPER_SPAWN_EGG : Material.EGG;
        
        ItemStack chargedEgg = new ItemStack(eggMaterial);
        ItemMeta meta = chargedEgg.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "§lCharged Creeper Egg");
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Spawns a pre-charged creeper",
                "",
                ChatColor.YELLOW + "⚡ Properties:",
                ChatColor.GRAY + "• Spawns charged creeper instantly",
                ChatColor.GRAY + "• Perfect for head hunting",
                ChatColor.GRAY + "• Single use item",
                "",
                ChatColor.GREEN + "▶ Right-click to spawn",
                ChatColor.RED + "⚠ Use carefully - explosive!",
                ChatColor.DARK_GRAY + "XSteal Utility Item"
            ));
            chargedEgg.setItemMeta(meta);
        }
        
        return chargedEgg;
    }
    
    /**
     * Get player head material for current version
     */
    private Material getPlayerHeadMaterial() {
        if (VersionCompatibility.supportsNewMaterials()) {
            return Material.PLAYER_HEAD;
        } else {
            return Material.valueOf("SKULL_ITEM");
        }
    }
    
    /**
     * Show revival head recipe to player
     */
    public void showRevivalHeadRecipe(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        player.sendMessage(ChatColor.GOLD + "    🔄 REVIVAL HEAD RECIPE");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Crafting Pattern:");
        player.sendMessage(ChatColor.GRAY + "  [G] [D] [G]");
        player.sendMessage(ChatColor.GRAY + "  [D] [S] [D]");
        player.sendMessage(ChatColor.GRAY + "  [G] [D] [G]");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Materials:");
        player.sendMessage(ChatColor.WHITE + "  G = " + ChatColor.GOLD + "Gold Ingot");
        player.sendMessage(ChatColor.WHITE + "  D = " + ChatColor.AQUA + "Diamond");
        player.sendMessage(ChatColor.WHITE + "  S = " + ChatColor.GRAY + "Player Head (any)");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "💚 Result: Revival Head");
        player.sendMessage(ChatColor.GRAY + "• Can revive any banboxed player");
        player.sendMessage(ChatColor.GRAY + "• Costs 1 life point to use");
        player.sendMessage(ChatColor.GRAY + "• Works from any distance");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "💡 Tip: Craft these before you need them!");
        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
    }
    
    /**
     * Show all custom recipes to player
     */
    public void showAllRecipes(Player player) {
        player.sendMessage(ChatColor.GOLD + "═══ XSteal Custom Recipes ═══");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "🔄 Revival Head:");
        player.sendMessage(ChatColor.GRAY + "  Gold + Diamond + Player Head");
        player.sendMessage(ChatColor.GRAY + "  → Revives banboxed players");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "💚 Life Crystal:");
        player.sendMessage(ChatColor.GRAY + "  Emerald + Redstone + Diamond Block");
        player.sendMessage(ChatColor.GRAY + "  → Grants +1 life point");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "⚡ Charged Creeper Egg:");
        player.sendMessage(ChatColor.GRAY + "  Lightning Rod + Gunpowder + Creeper Egg");
        player.sendMessage(ChatColor.GRAY + "  → Spawns charged creeper");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "Use /xsteal unbanrecipe for detailed revival recipe");
    }
    
    /**
     * Check if item is a custom XSteal item
     */
    public boolean isCustomItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) {
            return false;
        }
        
        return lore.stream().anyMatch(line -> 
            ChatColor.stripColor(line).contains("XSteal") && 
            (ChatColor.stripColor(line).contains("Revival Item") || 
             ChatColor.stripColor(line).contains("Life Item") ||
             ChatColor.stripColor(line).contains("Utility Item")));
    }
    
    /**
     * Get custom item type
     */
    public String getCustomItemType(ItemStack item) {
        if (!isCustomItem(item)) {
            return null;
        }
        
        String displayName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        if (displayName.contains("Revival Head")) {
            return "revival_head";
        } else if (displayName.contains("Life Crystal")) {
            return "life_crystal";
        } else if (displayName.contains("Charged Creeper Egg")) {
            return "charged_creeper_egg";
        }
        
        return null;
    }
    
    /**
     * Cleanup recipe manager
     */
    public void cleanup() {
        // Remove custom recipes if needed
        if (VersionCompatibility.isAtLeast(1, 12)) {
            try {
                Bukkit.removeRecipe(new NamespacedKey(plugin, "revival_head"));
                Bukkit.removeRecipe(new NamespacedKey(plugin, "life_crystal"));
                Bukkit.removeRecipe(new NamespacedKey(plugin, "charged_creeper_egg"));
            } catch (Exception e) {
                // Recipes may not exist
            }
        }
    }
}