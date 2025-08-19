package com.xreatlabs.xsteal.commands;

import com.xreatlabs.xsteal.XSteal;
import com.xreatlabs.xsteal.gui.HeadsGUI;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced XSteal command system with all requested commands
 * Includes GUI, life management, advanced admin commands, and high performance features
 */
public class EnhancedXStealCommand implements CommandExecutor, TabCompleter {
    
    private final XSteal plugin;
    private final Map<String, SubCommand> subCommands;
    private final HeadsGUI headsGUI;
    
    public EnhancedXStealCommand(XSteal plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        this.headsGUI = new HeadsGUI(plugin);
        
        // Register all enhanced subcommands
        registerEnhancedSubCommands();
    }
    
    /**
     * Register all enhanced subcommands
     */
    private void registerEnhancedSubCommands() {
        // Player Commands
        subCommands.put("heads", new HeadsGUICommand(plugin, headsGUI));
        subCommands.put("listheads", new ListHeadsCommand(plugin));
        subCommands.put("unbanrecipe", new UnbanRecipeCommand(plugin));
        subCommands.put("withdrawlife", new WithdrawLifeCommand(plugin));
        
        // Admin Commands
        subCommands.put("banbox", new BanBoxCommand(plugin));
        subCommands.put("cooldown", new CooldownCommand(plugin));
        subCommands.put("getlife", new GetLifeCommand(plugin));
        subCommands.put("give", new GiveCommand(plugin));
        subCommands.put("giveall", new GiveAllCommand(plugin));
        subCommands.put("listbanboxes", new ListBanBoxesCommand(plugin));
        subCommands.put("listdead", new ListDeadCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("removebanbox", new RemoveBanBoxCommand(plugin));
        subCommands.put("revive", new ReviveCommand(plugin));
        subCommands.put("setcooldown", new SetCooldownCommand(plugin));
        
        // Utility Commands
        subCommands.put("help", new HelpCommand(plugin));
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("debug", new DebugCommand(plugin));
        subCommands.put("performance", new PerformanceCommand(plugin));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if plugin is ready
        if (!plugin.isPluginReady()) {
            sender.sendMessage(ChatColor.RED + "XSteal is still loading, please wait...");
            return true;
        }
        
        // No arguments - show GUI if player, help if console
        if (args.length == 0) {
            if (sender instanceof Player) {
                return subCommands.get("heads").execute(sender, args);
            } else {
                return subCommands.get("help").execute(sender, args);
            }
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Unknown command: " + subCommandName);
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.WHITE + "/" + label + " help" + 
                ChatColor.YELLOW + " for available commands.");
            return true;
        }
        
        // Check permissions
        if (!subCommand.hasPermission(sender)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }
        
        // Execute subcommand
        try {
            return subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An error occurred while executing the command:");
            sender.sendMessage(ChatColor.RED + e.getMessage());
            plugin.getPluginLogger().severe("Command execution error", e);
            return true;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!plugin.isPluginReady()) {
            return Collections.emptyList();
        }
        
        // First argument - subcommand names
        if (args.length == 1) {
            return subCommands.entrySet().stream()
                .filter(entry -> entry.getValue().hasPermission(sender))
                .map(Map.Entry::getKey)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .sorted()
                .collect(Collectors.toList());
        }
        
        // Delegate to subcommand tab completion
        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = subCommands.get(subCommandName);
            
            if (subCommand != null && subCommand.hasPermission(sender)) {
                return subCommand.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        
        return Collections.emptyList();
    }
    
    // ===== ENHANCED SUBCOMMAND IMPLEMENTATIONS =====
    
    /**
     * /headsteal heads - GUI for head powers
     */
    private static class HeadsGUICommand extends SubCommand {
        private final HeadsGUI headsGUI;
        
        public HeadsGUICommand(XSteal plugin, HeadsGUI headsGUI) {
            super(plugin, "heads", "xsteal.use", "Open heads GUI to view all head powers");
            this.headsGUI = headsGUI;
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }
            
            Player player = (Player) sender;
            
            // Open heads GUI
            headsGUI.openHeadsGUI(player);
            player.sendMessage(ChatColor.GREEN + "📋 Opening XSteal Heads GUI...");
            
            return true;
        }
    }
    
    /**
     * /headsteal listheads - List all existing mob heads
     */
    private static class ListHeadsCommand extends SubCommand {
        public ListHeadsCommand(XSteal plugin) {
            super(plugin, "listheads", "xsteal.use", "List all existing mob heads");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            String category = args.length > 0 ? args[0].toLowerCase() : null;
            
            Set<String> headKeys = plugin.getHeadManager().getLoadedHeadKeys();
            
            if (headKeys.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No heads loaded!");
                return true;
            }
            
            // Filter by category if specified
            List<String> filteredHeads = headKeys.stream()
                .filter(key -> {
                    if (category == null) return true;
                    var headData = plugin.getHeadManager().getHeadData(key);
                    return headData != null && headData.getCategory().equalsIgnoreCase(category);
                })
                .sorted()
                .collect(Collectors.toList());
            
            if (filteredHeads.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "No heads found" + 
                    (category != null ? " in category: " + category : ""));
                return true;
            }
            
            // Display heads by category
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal Mob Heads " + 
                (category != null ? "(" + category.toUpperCase() + ") " : "") + "═══");
            
            Map<String, List<String>> categorizedHeads = new HashMap<>();
            
            for (String headKey : filteredHeads) {
                var headData = plugin.getHeadManager().getHeadData(headKey);
                if (headData != null) {
                    String headCategory = headData.getCategory();
                    categorizedHeads.computeIfAbsent(headCategory, k -> new ArrayList<>()).add(headKey);
                }
            }
            
            // Display by category
            for (Map.Entry<String, List<String>> categoryEntry : categorizedHeads.entrySet()) {
                sender.sendMessage("");
                sender.sendMessage(ChatColor.YELLOW + "▶ " + categoryEntry.getKey().toUpperCase() + " MOBS:");
                
                for (String headKey : categoryEntry.getValue()) {
                    var headData = plugin.getHeadManager().getHeadData(headKey);
                    if (headData != null) {
                        String displayName = ChatColor.translateAlternateColorCodes('&', headData.getDisplayName());
                        String line = ChatColor.WHITE + "  • " + headKey + ChatColor.GRAY + " - " + displayName;
                        
                        if (headData.isBossHead()) {
                            line += ChatColor.GOLD + " [BOSS - " + headData.getBossAbilities().size() + " ABILITIES]";
                        } else if (headData.hasAbility()) {
                            line += ChatColor.GREEN + " [" + headData.getAbility().getType().replace("_", " ").toUpperCase() + "]";
                        }
                        
                        sender.sendMessage(line);
                    }
                }
            }
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "Total: " + filteredHeads.size() + " heads");
            sender.sendMessage(ChatColor.GRAY + "Use /xsteal heads for interactive GUI");
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                // Category names
                Set<String> categories = plugin.getHeadManager().getLoadedHeadKeys().stream()
                    .map(key -> plugin.getHeadManager().getHeadData(key))
                    .filter(Objects::nonNull)
                    .map(data -> data.getCategory())
                    .collect(Collectors.toSet());
                
                return categories.stream()
                    .filter(cat -> cat.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal unbanrecipe - Show player revival head recipe
     */
    private static class UnbanRecipeCommand extends SubCommand {
        public UnbanRecipeCommand(XSteal plugin) {
            super(plugin, "unbanrecipe", "xsteal.use", "Show player revival head recipe");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            plugin.getRecipeManager().showRevivalHeadRecipe((Player) sender);
            return true;
        }
    }
    
    /**
     * /headsteal withdrawlife - Withdraw life from plugin
     */
    private static class WithdrawLifeCommand extends SubCommand {
        public WithdrawLifeCommand(XSteal plugin) {
            super(plugin, "withdrawlife", "xsteal.admin.life", "Withdraw life points from the plugin");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }
            
            Player player = (Player) sender;
            
            int amount = 1;
            if (args.length > 0) {
                try {
                    amount = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[0]);
                    return true;
                }
            }
            
            boolean success = plugin.getLifeManager().withdrawLife(player, amount);
            
            if (!success) {
                sender.sendMessage(ChatColor.RED + "❌ Failed to withdraw life!");
            }
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("1", "5", "10", "25", "50");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal getlife <amount> - Get life from plugin
     */
    private static class GetLifeCommand extends SubCommand {
        public GetLifeCommand(XSteal plugin) {
            super(plugin, "getlife", "xsteal.admin.life", "Get life points from the plugin");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }
            
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal getlife <amount>");
                return true;
            }
            
            Player player = (Player) sender;
            
            try {
                int amount = Integer.parseInt(args[0]);
                boolean success = plugin.getLifeManager().getLifeFromPlugin(player, amount);
                
                if (!success) {
                    sender.sendMessage(ChatColor.RED + "❌ Failed to get life from plugin!");
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[0]);
            }
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("1", "5", "10", "25", "50");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal giveall <player> - Give all heads to player
     */
    private static class GiveAllCommand extends SubCommand {
        public GiveAllCommand(XSteal plugin) {
            super(plugin, "giveall", "xsteal.admin.give", "Give all mob heads to a player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal giveall <player>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            
            Set<String> headKeys = plugin.getHeadManager().getLoadedHeadKeys();
            int given = 0;
            int failed = 0;
            
            sender.sendMessage(ChatColor.YELLOW + "🎁 Giving all " + headKeys.size() + " heads to " + target.getName() + "...");
            
            for (String headKey : headKeys) {
                ItemStack head = plugin.getHeadManager().createHeadItem(headKey);
                if (head != null) {
                    HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(head);
                    if (leftover.isEmpty()) {
                        given++;
                    } else {
                        // Drop excess items
                        for (ItemStack item : leftover.values()) {
                            target.getWorld().dropItemNaturally(target.getLocation(), item);
                        }
                        given++;
                    }
                } else {
                    failed++;
                }
            }
            
            sender.sendMessage(ChatColor.GREEN + "✅ Successfully gave " + given + " heads to " + target.getName());
            if (failed > 0) {
                sender.sendMessage(ChatColor.YELLOW + "⚠ " + failed + " heads failed to create");
            }
            
            target.sendMessage(ChatColor.GREEN + "🎉 You received all XSteal mob heads from " + sender.getName() + "!");
            target.sendMessage(ChatColor.GRAY + "Total heads: " + given);
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal listbanboxes - List all active banboxes
     */
    private static class ListBanBoxesCommand extends SubCommand {
        public ListBanBoxesCommand(XSteal plugin) {
            super(plugin, "listbanboxes", "xsteal.admin.banbox", "List all active banboxes in the world");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            Set<UUID> banboxedPlayers = plugin.getBanBoxManager().getBanBoxPlayers();
            
            if (banboxedPlayers.isEmpty()) {
                sender.sendMessage(ChatColor.YELLOW + "📋 No active banboxes found.");
                return true;
            }
            
            sender.sendMessage(ChatColor.GOLD + "═══ Active BanBoxes ═══");
            sender.sendMessage(ChatColor.GRAY + "Total: " + banboxedPlayers.size());
            sender.sendMessage("");
            
            int index = 1;
            for (UUID uuid : banboxedPlayers) {
                var data = plugin.getBanBoxManager().getBanBoxData(uuid);
                if (data != null) {
                    long timeInBanBox = data.getTimeInBanBox();
                    long hoursInBanBox = timeInBanBox / (1000 * 60 * 60);
                    long daysInBanBox = hoursInBanBox / 24;
                    
                    String timeDisplay = daysInBanBox > 0 ? 
                        daysInBanBox + " days" : hoursInBanBox + " hours";
                    
                    sender.sendMessage(ChatColor.YELLOW + index + ". " + data.getPlayerName());
                    sender.sendMessage(ChatColor.GRAY + "   Time in BanBox: " + timeDisplay);
                    sender.sendMessage(ChatColor.GRAY + "   Timer: " + data.getTimerDays() + " days");
                    
                    if (data.getDeathLocation() != null) {
                        org.bukkit.Location loc = data.getDeathLocation();
                        sender.sendMessage(ChatColor.GRAY + "   Location: " + loc.getWorld().getName() + 
                            " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
                    }
                    
                    if (data.getKillerUUID() != null) {
                        Player killer = Bukkit.getPlayer(data.getKillerUUID());
                        String killerName = killer != null ? killer.getName() : "Unknown";
                        sender.sendMessage(ChatColor.GRAY + "   Killed by: " + killerName);
                    }
                    
                    sender.sendMessage("");
                    index++;
                }
            }
            
            sender.sendMessage(ChatColor.GRAY + "Use /xsteal revive <player> to revive players");
            
            return true;
        }
    }
    
    /**
     * /headsteal listdead - List all eliminated/dead players
     */
    private static class ListDeadCommand extends SubCommand {
        public ListDeadCommand(XSteal plugin) {
            super(plugin, "listdead", "xsteal.admin.banbox", "List all eliminated/dead players");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            Set<UUID> banboxedPlayers = plugin.getBanBoxManager().getBanBoxPlayers();
            
            sender.sendMessage(ChatColor.GOLD + "═══ Dead/Eliminated Players ═══");
            
            if (banboxedPlayers.isEmpty()) {
                sender.sendMessage(ChatColor.GREEN + "✅ No players are currently dead/eliminated!");
                sender.sendMessage(ChatColor.GRAY + "All players are alive and well.");
                return true;
            }
            
            sender.sendMessage(ChatColor.RED + "💀 Currently Dead: " + banboxedPlayers.size() + " players");
            sender.sendMessage("");
            
            // Sort by time in banbox (longest first)
            List<UUID> sortedPlayers = banboxedPlayers.stream()
                .sorted((uuid1, uuid2) -> {
                    var data1 = plugin.getBanBoxManager().getBanBoxData(uuid1);
                    var data2 = plugin.getBanBoxManager().getBanBoxData(uuid2);
                    if (data1 == null || data2 == null) return 0;
                    return Long.compare(data2.getTimeInBanBox(), data1.getTimeInBanBox());
                })
                .collect(Collectors.toList());
            
            int index = 1;
            for (UUID uuid : sortedPlayers) {
                var data = plugin.getBanBoxManager().getBanBoxData(uuid);
                if (data != null) {
                    long timeInBanBox = data.getTimeInBanBox();
                    long hoursInBanBox = timeInBanBox / (1000 * 60 * 60);
                    long daysInBanBox = hoursInBanBox / 24;
                    
                    // Calculate time until auto-release
                    long timerMs = data.getTimerDays() * 24L * 60L * 60L * 1000L;
                    long timeUntilRelease = timerMs - timeInBanBox;
                    long hoursUntilRelease = Math.max(0, timeUntilRelease / (1000 * 60 * 60));
                    long daysUntilRelease = hoursUntilRelease / 24;
                    
                    String status = daysUntilRelease > 0 ? 
                        ChatColor.RED + "💀 DEAD (" + daysUntilRelease + "d left)" :
                        ChatColor.YELLOW + "⏰ RELEASING SOON";
                    
                    sender.sendMessage(ChatColor.WHITE + index + ". " + data.getPlayerName() + " " + status);
                    sender.sendMessage(ChatColor.GRAY + "   Dead for: " + 
                        (daysInBanBox > 0 ? daysInBanBox + " days" : hoursInBanBox + " hours"));
                    
                    index++;
                }
            }
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GRAY + "💡 Players can be revived by:");
            sender.sendMessage(ChatColor.GRAY + "• Finding and left-clicking their head");
            sender.sendMessage(ChatColor.GRAY + "• Admin command: /xsteal revive <player>");
            sender.sendMessage(ChatColor.GRAY + "• Destroying their head (immediate release)");
            
            return true;
        }
    }
    
    /**
     * Base class for subcommands (enhanced)
     */
    public abstract static class SubCommand {
        protected final XSteal plugin;
        protected final String name;
        protected final String permission;
        protected final String description;
        
        public SubCommand(XSteal plugin, String name, String permission, String description) {
            this.plugin = plugin;
            this.name = name;
            this.permission = permission;
            this.description = description;
        }
        
        public abstract boolean execute(CommandSender sender, String[] args);
        
        public List<String> tabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
        
        public boolean hasPermission(CommandSender sender) {
            return sender.hasPermission(permission);
        }
        
        public String getName() { return name; }
        public String getPermission() { return permission; }
        public String getDescription() { return description; }
    }
}