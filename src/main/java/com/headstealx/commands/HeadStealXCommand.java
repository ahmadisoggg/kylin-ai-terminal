package com.headstealx.commands;

import com.headstealx.Main;

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
 * Main command handler for HeadStealX
 * Handles all /xsteal subcommands with tab completion
 */
public class HeadStealXCommand implements CommandExecutor, TabCompleter {
    
    private final Main plugin;
    
    // Subcommand handlers
    private final Map<String, SubCommand> subCommands;
    
    public HeadStealXCommand(Main plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        
        // Register subcommands
        registerSubCommands();
    }
    
    /**
     * Register all subcommands
     */
    private void registerSubCommands() {
        subCommands.put("give", new GiveCommand(plugin));
        subCommands.put("listheads", new ListHeadsCommand(plugin));
        subCommands.put("revive", new ReviveCommand(plugin));
        subCommands.put("setbanbox", new SetBanboxCommand(plugin));
        subCommands.put("fuse", new FuseCommand(plugin));
        subCommands.put("clone", new CloneCommand(plugin));
        subCommands.put("reload", new ReloadCommand(plugin));
        subCommands.put("debug", new DebugCommand(plugin));
        subCommands.put("help", new HelpCommand(plugin));
        subCommands.put("info", new InfoCommand(plugin));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if plugin is ready
        if (!plugin.isPluginReady()) {
            sender.sendMessage(ChatColor.RED + "HeadStealX is still loading, please wait...");
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
        protected final Main plugin;
        protected final String name;
        protected final String permission;
        protected final String description;
        
        public SubCommand(Main plugin, String name, String permission, String description) {
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
     * /xsteal give <player> <head> [amount]
     */
    private static class GiveCommand extends SubCommand {
        public GiveCommand(Main plugin) {
            super(plugin, "give", "headsteal.admin.give", "Give a head to a player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal give <player> <head> [amount]");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            
            String headKey = args[1].toLowerCase();
            if (!plugin.getHeadManager().getLoadedHeadKeys().contains(headKey)) {
                sender.sendMessage(ChatColor.RED + "Unknown head: " + headKey);
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
            
            ItemStack head = plugin.getHeadManager().createHeadItem(headKey);
            if (head == null) {
                sender.sendMessage(ChatColor.RED + "Failed to create head: " + headKey);
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
            
            sender.sendMessage(ChatColor.GREEN + "Gave " + amount + "x " + headKey + " head to " + target.getName());
            target.sendMessage(ChatColor.GREEN + "You received " + amount + "x " + headKey + " head from " + sender.getName());
            
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
                // Head names
                return plugin.getHeadManager().getLoadedHeadKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /xsteal listheads [category]
     */
    private static class ListHeadsCommand extends SubCommand {
        public ListHeadsCommand(Main plugin) {
            super(plugin, "listheads", "headsteal.use", "List available heads");
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
            sender.sendMessage(ChatColor.GOLD + "=== HeadStealX Heads " + 
                (category != null ? "(" + category + ") " : "") + "===");
            
            for (String headKey : filteredHeads) {
                var headData = plugin.getHeadManager().getHeadData(headKey);
                if (headData != null) {
                    String displayName = ChatColor.translateAlternateColorCodes('&', headData.getDisplayName());
                    String categoryName = headData.getCategory();
                    
                    sender.sendMessage(ChatColor.YELLOW + headKey + ChatColor.GRAY + " - " + 
                        displayName + ChatColor.DARK_GRAY + " [" + categoryName + "]");
                }
            }
            
            sender.sendMessage(ChatColor.GOLD + "Total: " + filteredHeads.size() + " heads");
            
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
        public ReviveCommand(Main plugin) {
            super(plugin, "revive", "headsteal.admin.revive", "Revive a banboxed player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal revive <player>");
                return true;
            }
            
            String playerName = args[0];
            
            if (!plugin.getBanBoxManager().isBanned(playerName)) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not banboxed!");
                return true;
            }
            
            // Get default spawn location for revival
            org.bukkit.Location reviveLocation;
            if (sender instanceof Player) {
                reviveLocation = ((Player) sender).getLocation();
            } else {
                // Use default world spawn
                reviveLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            
            boolean success = plugin.getBanBoxManager().revive(playerName, reviveLocation, 
                sender instanceof Player ? (Player) sender : null);
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "Successfully revived " + playerName);
                plugin.getPluginLogger().info("Admin " + sender.getName() + " revived " + playerName);
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to revive " + playerName);
            }
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                // Banboxed player names
                return plugin.getBanBoxManager().getBannedPlayers().stream()
                    .map(uuid -> {
                        var data = plugin.getBanBoxManager().getBanData(uuid);
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
        public ReloadCommand(Main plugin) {
            super(plugin, "reload", "headsteal.admin.reload", "Reload plugin configuration");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "Reloading HeadStealX...");
            
            try {
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "HeadStealX reloaded successfully!");
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Failed to reload HeadStealX: " + e.getMessage());
                plugin.getPluginLogger().severe("Reload failed", e);
            }
            
            return true;
        }
    }
    
    /**
     * /xsteal debug
     */
    private static class DebugCommand extends SubCommand {
        public DebugCommand(Main plugin) {
            super(plugin, "debug", "headsteal.admin.debug", "Show debug information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "=== HeadStealX Debug Info ===");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Plugin Ready: " + ChatColor.WHITE + plugin.isPluginReady());
            sender.sendMessage(ChatColor.YELLOW + "Dependencies Loaded: " + ChatColor.WHITE + plugin.areDependenciesLoaded());
            sender.sendMessage(ChatColor.YELLOW + "Loaded Heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount());
            sender.sendMessage(ChatColor.YELLOW + "Registered Abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getRegisteredAbilityCount());
            sender.sendMessage(ChatColor.YELLOW + "Active Abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getActiveAbilityCount());
            sender.sendMessage(ChatColor.YELLOW + "Banboxed Players: " + ChatColor.WHITE + plugin.getBanBoxManager().getBannedPlayerCount());
            sender.sendMessage(ChatColor.YELLOW + "HeadDatabase Available: " + ChatColor.WHITE + plugin.getHeadManager().isHeadDatabaseAvailable());
            sender.sendMessage(ChatColor.YELLOW + "Server Version: " + ChatColor.WHITE + com.headstealx.util.VersionUtil.getCompatibilityInfo());
            
            return true;
        }
    }
    
    /**
     * /xsteal help
     */
    private static class HelpCommand extends SubCommand {
        public HelpCommand(Main plugin) {
            super(plugin, "help", "headsteal.use", "Show help information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "=== HeadStealX Commands ===");
            
            for (SubCommand subCommand : plugin.getServer().getServicesManager()
                    .getRegistration(HeadStealXCommand.class).getProvider().subCommands.values()) {
                
                if (subCommand.hasPermission(sender)) {
                    sender.sendMessage(ChatColor.YELLOW + "/xsteal " + subCommand.getName() + 
                        ChatColor.GRAY + " - " + subCommand.getDescription());
                }
            }
            
            sender.sendMessage(ChatColor.GOLD + "========================");
            sender.sendMessage(ChatColor.GRAY + "Plugin by HeadStealX Team");
            
            return true;
        }
    }
    
    // Placeholder implementations for remaining commands
    private static class SetBanboxCommand extends SubCommand {
        public SetBanboxCommand(Main plugin) {
            super(plugin, "setbanbox", "headsteal.admin.banbox", "Manually banbox a player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "SetBanbox command - Coming soon!");
            return true;
        }
    }
    
    private static class FuseCommand extends SubCommand {
        public FuseCommand(Main plugin) {
            super(plugin, "fuse", "headsteal.fuse", "Fuse two heads together");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "Fuse command - Coming soon!");
            return true;
        }
    }
    
    private static class CloneCommand extends SubCommand {
        public CloneCommand(Main plugin) {
            super(plugin, "clone", "headsteal.clone", "Clone a player's head");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "Clone command - Coming soon!");
            return true;
        }
    }
    
    private static class InfoCommand extends SubCommand {
        public InfoCommand(Main plugin) {
            super(plugin, "info", "headsteal.use", "Show plugin information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "=== HeadStealX v" + plugin.getDescription().getVersion() + " ===");
            sender.sendMessage(ChatColor.YELLOW + "A premium Minecraft plugin for Paper/Spigot");
            sender.sendMessage(ChatColor.GRAY + "• 58 unique mob heads with abilities");
            sender.sendMessage(ChatColor.GRAY + "• Charged creeper head drop system");
            sender.sendMessage(ChatColor.GRAY + "• BanBox revival mechanics");
            sender.sendMessage(ChatColor.GRAY + "• Boss heads with combo abilities");
            sender.sendMessage(ChatColor.GRAY + "• Compatible with MC 1.8-1.21.8");
            sender.sendMessage(ChatColor.GOLD + "============================");
            
            return true;
        }
    }
}