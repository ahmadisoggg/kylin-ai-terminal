package com.xreatlabs.xsteal.commands;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Arrow Fusion Command
 * Shows all arrow fusion recipes and gives special arrows
 */
public class ArrowFusionCommand extends EnhancedXStealCommand.SubCommand {
    
    public ArrowFusionCommand(XSteal plugin) {
        super(plugin, "arrows", "xsteal.use", "View arrow fusion recipes and abilities");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showAllArrowRecipes(sender);
        } else {
            String action = args[0].toLowerCase();
            
            switch (action) {
                case "recipes":
                    showAllArrowRecipes(sender);
                    break;
                case "give":
                    giveSpecialArrow(sender, args);
                    break;
                case "list":
                    listArrowTypes(sender);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown arrow action: " + action);
                    sender.sendMessage(ChatColor.YELLOW + "Use: recipes, give, list");
            }
        }
        
        return true;
    }
    
    /**
     * Show all arrow fusion recipes
     */
    private void showAllArrowRecipes(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "═══ XSteal Arrow Fusion System ═══");
        sender.sendMessage(ChatColor.GRAY + "Combine arrows with mob heads for special abilities!");
        sender.sendMessage("");
        
        sender.sendMessage(ChatColor.GREEN + "🏹 ARROW FUSION RECIPES:");
        sender.sendMessage("");
        
        // Creeper Arrow
        sender.sendMessage(ChatColor.YELLOW + "💥 Creeper Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Creeper Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Explodes on contact (3-block radius)");
        sender.sendMessage(ChatColor.GRAY + "  Use: Explosive ranged attacks");
        sender.sendMessage("");
        
        // Ender Arrow
        sender.sendMessage(ChatColor.YELLOW + "⚡ Ender Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Enderman Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Teleports shooter to impact location");
        sender.sendMessage(ChatColor.GRAY + "  Use: Instant travel and positioning");
        sender.sendMessage("");
        
        // Fire Arrow
        sender.sendMessage(ChatColor.YELLOW + "🔥 Fire Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Blaze Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Ignites targets and leaves fire trail");
        sender.sendMessage(ChatColor.GRAY + "  Use: Area denial and burning damage");
        sender.sendMessage("");
        
        // Frost Arrow
        sender.sendMessage(ChatColor.YELLOW + "❄ Frost Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Stray Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Freezes targets and creates ice");
        sender.sendMessage(ChatColor.GRAY + "  Use: Crowd control and slowing enemies");
        sender.sendMessage("");
        
        // Poison Arrow
        sender.sendMessage(ChatColor.YELLOW + "☠ Poison Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Cave Spider Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Poisons targets with spreading venom");
        sender.sendMessage(ChatColor.GRAY + "  Use: Damage over time and area control");
        sender.sendMessage("");
        
        // Thunder Arrow
        sender.sendMessage(ChatColor.YELLOW + "⚡ Thunder Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Charged Creeper Essence");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Strikes lightning at impact");
        sender.sendMessage(ChatColor.GRAY + "  Use: Massive electrical damage");
        sender.sendMessage("");
        
        // Void Arrow
        sender.sendMessage(ChatColor.YELLOW + "🌌 Void Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Endermite Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Phases through blocks, void damage");
        sender.sendMessage(ChatColor.GRAY + "  Use: Armor-piercing attacks");
        sender.sendMessage("");
        
        // Healing Arrow
        sender.sendMessage(ChatColor.YELLOW + "💖 Healing Arrow:");
        sender.sendMessage(ChatColor.GRAY + "  Recipe: Arrow + Allay Head");
        sender.sendMessage(ChatColor.GRAY + "  Ability: Heals allies, damages enemies");
        sender.sendMessage(ChatColor.GRAY + "  Use: Support and team healing");
        sender.sendMessage("");
        
        sender.sendMessage(ChatColor.GREEN + "💡 How to Use:");
        sender.sendMessage(ChatColor.GRAY + "1. Craft arrow + mob head in any crafting interface");
        sender.sendMessage(ChatColor.GRAY + "2. Shoot the special arrow with any bow");
        sender.sendMessage(ChatColor.GRAY + "3. Arrow activates its ability on impact");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
    }
    
    /**
     * Give special arrow to player
     */
    private void giveSpecialArrow(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can receive arrows!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /xsteal arrows give <type> [amount]");
            listArrowTypes(sender);
            return;
        }
        
        Player player = (Player) sender;
        String arrowType = args[1].toLowerCase();
        int amount = args.length > 2 ? parseAmount(args[2]) : 1;
        
        if (amount <= 0) {
            sender.sendMessage(ChatColor.RED + "Invalid amount!");
            return;
        }
        
        // Give specific arrow type
        boolean success = giveArrowByType(player, arrowType, amount);
        
        if (!success) {
            sender.sendMessage(ChatColor.RED + "❌ Unknown arrow type: " + arrowType);
            listArrowTypes(sender);
        }
    }
    
    /**
     * List available arrow types
     */
    private void listArrowTypes(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Available arrow types:");
        sender.sendMessage(ChatColor.GRAY + "• creeper, ender, fire, frost, poison, thunder, void, healing");
    }
    
    /**
     * Give arrow by type
     */
    private boolean giveArrowByType(Player player, String arrowType, int amount) {
        org.bukkit.inventory.ItemStack arrow = null;
        
        switch (arrowType) {
            case "creeper":
                arrow = plugin.getArrowFusionSystem().createCreeperArrow();
                break;
            case "ender":
                arrow = plugin.getArrowFusionSystem().createEnderArrow();
                break;
            case "fire":
                arrow = plugin.getArrowFusionSystem().createFireArrow();
                break;
            case "frost":
                arrow = plugin.getArrowFusionSystem().createFrostArrow();
                break;
            case "poison":
                arrow = plugin.getArrowFusionSystem().createPoisonArrow();
                break;
            case "thunder":
                arrow = plugin.getArrowFusionSystem().createThunderArrow();
                break;
            case "void":
                arrow = plugin.getArrowFusionSystem().createVoidArrow();
                break;
            case "healing":
                arrow = plugin.getArrowFusionSystem().createHealingArrow();
                break;
            default:
                return false;
        }
        
        if (arrow != null) {
            arrow.setAmount(amount);
            player.getInventory().addItem(arrow);
            
            String arrowName = arrow.getItemMeta().getDisplayName();
            player.sendMessage(ChatColor.GREEN + "✅ Received " + amount + "x " + arrowName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Parse amount from string
     */
    private int parseAmount(String amountStr) {
        try {
            int amount = Integer.parseInt(amountStr);
            return Math.max(1, Math.min(64, amount));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}