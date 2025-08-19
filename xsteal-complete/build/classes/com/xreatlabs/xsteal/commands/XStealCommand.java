package com.xreatlabs.xsteal.commands;

import com.xreatlabs.xsteal.XSteal;

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
 * Main command handler for XSteal
 * Handles all /xsteal subcommands with comprehensive tab completion
 * Based on PSD1's HeadSteal video requirements
 */
public class XStealCommand implements CommandExecutor, TabCompleter {
    
    private final XSteal plugin;
    private final Map<String, SubCommand> subCommands;
    
    public XStealCommand(XSteal plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        
        // Register all subcommands
        registerSubCommands();
    }
    
    /**
     * Register all XSteal subcommands
     */
    private void registerSubCommands() {
        subCommands.put("give", new GiveCommand(plugin));
        subCommands.put("listheads", new ListHeadsCommand(plugin));
        subCommands.put("revive", new ReviveCommand(plugin));
        subCommands.put("setbanbox", new SetBanBoxCommand(plugin));
        subCommands.put("removebanbox", new RemoveBanBoxCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("debug", new DebugCommand(plugin));
        subCommands.put("help", new HelpCommand(plugin));
        subCommands.put("info", new InfoCommand(plugin));
        subCommands.put("banbox", new BanBoxCommand(plugin));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if plugin is ready
        if (!plugin.isPluginReady()) {
            sender.sendMessage(ChatColor.RED + "XSteal is still loading, please wait...");
            return true;
        }
        
        // No arguments - show help
        if (args.length == 0) {
            return subCommands.get("help").execute(sender, args);
        }
        
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            sender.sendMessage(ChatColor.RED + "Unknown subcommand: " + subCommandName);
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
    
    /**
     * Base class for subcommands
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
    
    /**
     * /xsteal give <player> <mob> [amount]
     */
    private static class GiveCommand extends SubCommand {
        public GiveCommand(XSteal plugin) {
            super(plugin, "give", "xsteal.admin.give", "Give a mob head to a player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal give <player> <mob> [amount]");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            
            String mobKey = args[1].toLowerCase();
            if (!plugin.getHeadManager().getLoadedHeadKeys().contains(mobKey)) {
                sender.sendMessage(ChatColor.RED + "Unknown mob head: " + mobKey);
                sender.sendMessage(ChatColor.YELLOW + "Use /xsteal listheads to see available heads");
                return true;
            }
            
            int amount = 1;
            if (args.length > 2) {
                try {
                    amount = Integer.parseInt(args[2]);
                    if (amount <= 0 || amount > 64) {
                        sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[2]);
                    return true;
                }
            }
            
            ItemStack head = plugin.getHeadManager().createHeadItem(mobKey);
            if (head == null) {
                sender.sendMessage(ChatColor.RED + "Failed to create head: " + mobKey);
                return true;
            }
            
            head.setAmount(amount);
            
            // Give to player
            HashMap<Integer, ItemStack> leftover = target.getInventory().addItem(head);
            if (!leftover.isEmpty()) {
                // Drop excess items
                for (ItemStack item : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), item);
                }
                sender.sendMessage(ChatColor.YELLOW + "Some items were dropped (inventory full)");
            }
            
            var headData = plugin.getHeadManager().getHeadData(mobKey);
            String headName = headData != null ? 
                ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()) : mobKey;
            
            sender.sendMessage(ChatColor.GREEN + "✅ Gave " + amount + "x " + headName + " §ato " + target.getName());
            target.sendMessage(ChatColor.GREEN + "🎁 You received " + amount + "x " + headName + " §afrom " + sender.getName());
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                // Player names
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (args.length == 2) {
                // Mob head names
                return plugin.getHeadManager().getLoadedHeadKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            } else if (args.length == 3) {
                // Amount suggestions
                return Arrays.asList("1", "5", "10", "16", "32", "64");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /xsteal listheads [category]
     */
    private static class ListHeadsCommand extends SubCommand {
        public ListHeadsCommand(XSteal plugin) {
            super(plugin, "listheads", "xsteal.use", "List all available mob heads");
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
            
            // Display heads
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal Mob Heads " + 
                (category != null ? "(" + category + ") " : "") + "═══");
            
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
                            line += ChatColor.GOLD + " [BOSS]";
                        }
                        
                        sender.sendMessage(line);
                        sender.sendMessage(ChatColor.DARK_GRAY + "    " + headData.getDescription());
                    }
                }
            }
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "Total: " + filteredHeads.size() + " heads");
            sender.sendMessage(ChatColor.GRAY + "Use /xsteal give <player> <mob> to give heads");
            
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
     * /xsteal revive <player>
     */
    private static class ReviveCommand extends SubCommand {
        public ReviveCommand(XSteal plugin) {
            super(plugin, "revive", "xsteal.admin.revive", "Revive a player from BanBox");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal revive <player>");
                return true;
            }
            
            String playerName = args[0];
            
            if (!plugin.getBanBoxManager().isBanBoxed(playerName)) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not in the BanBox!");
                return true;
            }
            
            // Get revival location
            org.bukkit.Location reviveLocation;
            if (sender instanceof Player) {
                reviveLocation = ((Player) sender).getLocation();
            } else {
                // Use default spawn location
                reviveLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            
            boolean success = plugin.getBanBoxManager().revivePlayer(playerName, reviveLocation, 
                sender instanceof Player ? (Player) sender : null);
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "✅ Successfully revived " + playerName);
                plugin.getPluginLogger().info("Admin " + sender.getName() + " revived " + playerName);
            } else {
                sender.sendMessage(ChatColor.RED + "❌ Failed to revive " + playerName);
            }
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                // Banboxed player names
                return plugin.getBanBoxManager().getBanBoxPlayers().stream()
                    .map(uuid -> {
                        var data = plugin.getBanBoxManager().getBanBoxData(uuid);
                        return data != null ? data.getPlayerName() : null;
                    })
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /xsteal reload
     */
    private static class ReloadCommand extends SubCommand {
        public ReloadCommand(XSteal plugin) {
            super(plugin, "reload", "xsteal.admin.reload", "Reload plugin configuration");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "🔄 Reloading XSteal...");
            
            try {
                long startTime = System.currentTimeMillis();
                plugin.reload();
                long endTime = System.currentTimeMillis();
                
                sender.sendMessage(ChatColor.GREEN + "✅ XSteal reloaded successfully!");
                sender.sendMessage(ChatColor.GRAY + "Reload time: " + (endTime - startTime) + "ms");
                sender.sendMessage(ChatColor.GRAY + "Loaded heads: " + plugin.getHeadManager().getLoadedHeadCount());
                sender.sendMessage(ChatColor.GRAY + "Registered abilities: " + plugin.getAbilityManager().getRegisteredAbilityCount());
                
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "❌ Failed to reload XSteal: " + e.getMessage());
                plugin.getPluginLogger().severe("Reload failed", e);
            }
            
            return true;
        }
    }
    
    /**
     * /xsteal debug
     */
    private static class DebugCommand extends SubCommand {
        public DebugCommand(XSteal plugin) {
            super(plugin, "debug", "xsteal.admin.debug", "Show debug information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal Debug Information ═══");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Plugin Ready: " + ChatColor.WHITE + plugin.isPluginReady());
            sender.sendMessage(ChatColor.YELLOW + "Dependencies Loaded: " + ChatColor.WHITE + plugin.areDependenciesLoaded());
            sender.sendMessage("");
            
            // Head system info
            sender.sendMessage(ChatColor.AQUA + "▶ Head System:");
            sender.sendMessage(ChatColor.GRAY + "  Loaded Heads: " + plugin.getHeadManager().getLoadedHeadCount());
            sender.sendMessage(ChatColor.GRAY + "  HeadDatabase Available: " + plugin.getHeadManager().isHeadDatabaseAvailable());
            
            // Ability system info
            sender.sendMessage(ChatColor.AQUA + "▶ Ability System:");
            sender.sendMessage(ChatColor.GRAY + "  Registered Abilities: " + plugin.getAbilityManager().getRegisteredAbilityCount());
            sender.sendMessage(ChatColor.GRAY + "  Active Abilities: " + plugin.getAbilityManager().getActiveAbilityCount());
            sender.sendMessage(ChatColor.GRAY + "  Summoned Entities: " + plugin.getAbilityManager().getSummonedEntityCount());
            
            // BanBox system info
            sender.sendMessage(ChatColor.AQUA + "▶ BanBox System:");
            sender.sendMessage(ChatColor.GRAY + "  Enabled: " + plugin.getBanBoxManager().isEnabled());
            sender.sendMessage(ChatColor.GRAY + "  Banboxed Players: " + plugin.getBanBoxManager().getBanBoxPlayerCount());
            
            // Server info
            sender.sendMessage(ChatColor.AQUA + "▶ Server Info:");
            sender.sendMessage(ChatColor.GRAY + "  " + com.xreatlabs.xsteal.utils.VersionCompatibility.getCompatibilityInfo());
            
            return true;
        }
    }
    
    /**
     * /xsteal help
     */
    private static class HelpCommand extends SubCommand {
        public HelpCommand(XSteal plugin) {
            super(plugin, "help", "xsteal.use", "Show help information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal Commands ═══");
            sender.sendMessage(ChatColor.GRAY + "PSD1 Inspired Minecraft Plugin");
            sender.sendMessage("");
            
            for (SubCommand subCommand : plugin.getServer().getServicesManager()
                    .getRegistration(XStealCommand.class).getProvider().subCommands.values()) {
                
                if (subCommand.hasPermission(sender)) {
                    sender.sendMessage(ChatColor.YELLOW + "/xsteal " + subCommand.getName() + 
                        ChatColor.GRAY + " - " + subCommand.getDescription());
                }
            }
            
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "═══ How to Use ═══");
            sender.sendMessage(ChatColor.GRAY + "1. Get heads by having charged creepers kill mobs");
            sender.sendMessage(ChatColor.GRAY + "2. Wear heads in helmet slot to gain abilities");
            sender.sendMessage(ChatColor.GRAY + "3. Left-click to activate abilities");
            sender.sendMessage(ChatColor.GRAY + "4. Boss heads have combo abilities!");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.DARK_GRAY + "Plugin by XreatLabs");
            
            return true;
        }
    }
    
    /**
     * /xsteal info
     */
    private static class InfoCommand extends SubCommand {
        public InfoCommand(XSteal plugin) {
            super(plugin, "info", "xsteal.use", "Show plugin information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal v" + plugin.getDescription().getVersion() + " ═══");
            sender.sendMessage(ChatColor.GRAY + "PSD1 Inspired Minecraft Plugin");
            sender.sendMessage(ChatColor.GRAY + "Compatible with Paper/Spigot 1.8-1.21.4");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "🎯 Features:");
            sender.sendMessage(ChatColor.GRAY + "• " + plugin.getHeadManager().getLoadedHeadCount() + "+ unique mob heads with abilities");
            sender.sendMessage(ChatColor.GRAY + "• Charged creeper head drop system");
            sender.sendMessage(ChatColor.GRAY + "• BanBox spectator mode system");
            sender.sendMessage(ChatColor.GRAY + "• Boss heads with combo abilities");
            sender.sendMessage(ChatColor.GRAY + "• No cooldowns (unlimited use)");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "⚡ Current Status:");
            sender.sendMessage(ChatColor.GRAY + "• Plugin Ready: " + (plugin.isPluginReady() ? "§aYes" : "§cNo"));
            sender.sendMessage(ChatColor.GRAY + "• BanBox System: " + (plugin.getBanBoxManager().isEnabled() ? "§aEnabled" : "§cDisabled"));
            sender.sendMessage(ChatColor.GRAY + "• HeadDatabase: " + (plugin.getHeadManager().isHeadDatabaseAvailable() ? "§aAvailable" : "§cUnavailable"));
            sender.sendMessage("");
            sender.sendMessage(ChatColor.DARK_GRAY + "Author: XreatLabs");
            
            return true;
        }
    }
    
    // Placeholder implementations for remaining commands
    
    private static class SetBanBoxCommand extends SubCommand {
        public SetBanBoxCommand(XSteal plugin) {
            super(plugin, "setbanbox", "xsteal.admin.banbox", "Set BanBox location");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
                return true;
            }
            
            Player player = (Player) sender;
            org.bukkit.Location location = player.getLocation();
            
            // Save location to config
            String locationString = location.getWorld().getName() + ":" + 
                location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
            
            plugin.getConfigManager().getMainConfig().set("banbox.default_location", locationString);
            plugin.getConfigManager().saveConfig("config.yml");
            
            sender.sendMessage(ChatColor.GREEN + "✅ BanBox location set to your current position!");
            sender.sendMessage(ChatColor.GRAY + "Location: " + locationString);
            
            return true;
        }
    }
    
    private static class RemoveBanBoxCommand extends SubCommand {
        public RemoveBanBoxCommand(XSteal plugin) {
            super(plugin, "removebanbox", "xsteal.admin.banbox", "Remove BanBox location");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "RemoveBanBox command - Feature coming soon!");
            return true;
        }
    }
    
    private static class BanBoxCommand extends SubCommand {
        public BanBoxCommand(XSteal plugin) {
            super(plugin, "banbox", "xsteal.admin.banbox", "Manage BanBox system");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.YELLOW + "BanBox Management:");
                sender.sendMessage(ChatColor.GRAY + "/xsteal banbox list - List banboxed players");
                sender.sendMessage(ChatColor.GRAY + "/xsteal banbox release <player> - Release player");
                return true;
            }
            
            String subAction = args[0].toLowerCase();
            
            if ("list".equals(subAction)) {
                Set<UUID> banboxedPlayers = plugin.getBanBoxManager().getBanBoxPlayers();
                
                if (banboxedPlayers.isEmpty()) {
                    sender.sendMessage(ChatColor.YELLOW + "No players are currently banboxed.");
                    return true;
                }
                
                sender.sendMessage(ChatColor.GOLD + "═══ Banboxed Players ═══");
                for (UUID uuid : banboxedPlayers) {
                    var data = plugin.getBanBoxManager().getBanBoxData(uuid);
                    if (data != null) {
                        long timeInBanBox = data.getTimeInBanBox();
                        long hoursInBanBox = timeInBanBox / (1000 * 60 * 60);
                        
                        sender.sendMessage(ChatColor.YELLOW + data.getPlayerName() + 
                            ChatColor.GRAY + " - " + hoursInBanBox + " hours");
                    }
                }
                
            } else if ("release".equals(subAction) && args.length > 1) {
                String playerName = args[1];
                boolean success = plugin.getBanBoxManager().releasePlayer(playerName);
                
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "✅ Released " + playerName + " from BanBox");
                } else {
                    sender.sendMessage(ChatColor.RED + "❌ Failed to release " + playerName);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal banbox <list|release> [player]");
            }
            
            return true;
        }
    }
}