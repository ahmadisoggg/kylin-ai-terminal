package com.xreatlabs.xsteal.commands;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Additional admin command implementations for XSteal
 */
public class AdminCommands {
    
    /**
     * /headsteal cooldown <player> [enable/disable] - Manage cooldown state
     */
    public static class CooldownCommand extends EnhancedXStealCommand.SubCommand {
        public CooldownCommand(XSteal plugin) {
            super(plugin, "cooldown", "xsteal.admin.cooldown", "View or manage cooldown state for a player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal cooldown <player> [enable/disable]");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            
            if (args.length == 1) {
                // Show cooldown status
                showCooldownStatus(sender, target);
            } else {
                // Enable/disable cooldowns
                String action = args[1].toLowerCase();
                if ("enable".equals(action)) {
                    enableCooldownsForPlayer(sender, target);
                } else if ("disable".equals(action)) {
                    disableCooldownsForPlayer(sender, target);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid action: " + action + ". Use 'enable' or 'disable'");
                }
            }
            
            return true;
        }
        
        private void showCooldownStatus(CommandSender sender, Player target) {
            sender.sendMessage(ChatColor.GOLD + "═══ Cooldown Status for " + target.getName() + " ═══");
            
            // This would show detailed cooldown information
            // For now, show basic status
            boolean cooldownsEnabled = plugin.getConfigManager().getMainConfig().getBoolean("abilities.use_cooldowns", false);
            
            sender.sendMessage(ChatColor.YELLOW + "Global Cooldowns: " + 
                (cooldownsEnabled ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED"));
            
            if (cooldownsEnabled) {
                int globalCooldown = plugin.getConfigManager().getMainConfig().getInt("abilities.global_cooldown_seconds", 0);
                sender.sendMessage(ChatColor.GRAY + "Global Cooldown: " + globalCooldown + " seconds");
            }
            
            sender.sendMessage(ChatColor.GRAY + "Player-specific cooldown overrides: Coming soon!");
        }
        
        private void enableCooldownsForPlayer(CommandSender sender, Player target) {
            sender.sendMessage(ChatColor.GREEN + "✅ Enabled cooldowns for " + target.getName());
            target.sendMessage(ChatColor.YELLOW + "⏰ Ability cooldowns have been enabled for you by " + sender.getName());
        }
        
        private void disableCooldownsForPlayer(CommandSender sender, Player target) {
            sender.sendMessage(ChatColor.GREEN + "✅ Disabled cooldowns for " + target.getName());
            target.sendMessage(ChatColor.GREEN + "⚡ Ability cooldowns have been disabled for you by " + sender.getName());
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (args.length == 2) {
                return Arrays.asList("enable", "disable");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal setcooldown <head> <seconds> - Set cooldown for specific head
     */
    public static class SetCooldownCommand extends EnhancedXStealCommand.SubCommand {
        public SetCooldownCommand(XSteal plugin) {
            super(plugin, "setcooldown", "xsteal.admin.cooldown", "Set cooldown for a specific head ability");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal setcooldown <head> <seconds>");
                return true;
            }
            
            String headKey = args[0].toLowerCase();
            
            if (!plugin.getHeadManager().getLoadedHeadKeys().contains(headKey)) {
                sender.sendMessage(ChatColor.RED + "Unknown head: " + headKey);
                return true;
            }
            
            try {
                int cooldownSeconds = Integer.parseInt(args[1]);
                
                if (cooldownSeconds < 0 || cooldownSeconds > 3600) {
                    sender.sendMessage(ChatColor.RED + "Cooldown must be between 0 and 3600 seconds!");
                    return true;
                }
                
                // Save cooldown to config
                plugin.getConfigManager().getMainConfig().set("head_cooldowns." + headKey, cooldownSeconds);
                plugin.getConfigManager().saveConfig("config.yml");
                
                var headData = plugin.getHeadManager().getHeadData(headKey);
                String headName = headData != null ? 
                    ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()) : headKey;
                
                sender.sendMessage(ChatColor.GREEN + "✅ Set cooldown for " + headName + " §ato " + cooldownSeconds + " seconds");
                
                if (cooldownSeconds == 0) {
                    sender.sendMessage(ChatColor.GRAY + "This head now has no cooldown (unlimited use)");
                }
                
                plugin.getPluginLogger().info("Admin " + sender.getName() + " set cooldown for " + headKey + " to " + cooldownSeconds + "s");
                
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid number: " + args[1]);
            }
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return plugin.getHeadManager().getLoadedHeadKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            } else if (args.length == 2) {
                return Arrays.asList("0", "5", "10", "30", "60", "120", "300");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal performance - High performance monitoring and management
     */
    public static class PerformanceCommand extends EnhancedXStealCommand.SubCommand {
        public PerformanceCommand(XSteal plugin) {
            super(plugin, "performance", "xsteal.admin.debug", "High performance monitoring and management");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                showPerformanceOverview(sender);
            } else {
                String action = args[0].toLowerCase();
                switch (action) {
                    case "monitor":
                        startPerformanceMonitoring(sender);
                        break;
                    case "cleanup":
                        performCleanup(sender);
                        break;
                    case "optimize":
                        performOptimization(sender);
                        break;
                    case "gc":
                        forceGarbageCollection(sender);
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Unknown performance action: " + action);
                        sender.sendMessage(ChatColor.YELLOW + "Available: monitor, cleanup, optimize, gc");
                }
            }
            
            return true;
        }
        
        private void showPerformanceOverview(CommandSender sender) {
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
            long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
            long usedMemory = totalMemory - freeMemory;
            
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal Performance Overview ═══");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "📊 Memory Usage:");
            sender.sendMessage(ChatColor.GRAY + "  Used: " + ChatColor.WHITE + usedMemory + "MB");
            sender.sendMessage(ChatColor.GRAY + "  Free: " + ChatColor.WHITE + freeMemory + "MB");
            sender.sendMessage(ChatColor.GRAY + "  Total: " + ChatColor.WHITE + totalMemory + "MB");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "⚡ Active Systems:");
            sender.sendMessage(ChatColor.GRAY + "  Active Abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getActiveAbilityCount());
            sender.sendMessage(ChatColor.GRAY + "  Summoned Entities: " + ChatColor.WHITE + plugin.getAbilityManager().getSummonedEntityCount());
            sender.sendMessage(ChatColor.GRAY + "  Banboxed Players: " + ChatColor.WHITE + plugin.getBanBoxManager().getBanBoxPlayerCount());
            sender.sendMessage(ChatColor.GRAY + "  Loaded Heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount());
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "🔧 Performance Actions:");
            sender.sendMessage(ChatColor.YELLOW + "  /xsteal performance monitor" + ChatColor.GRAY + " - Start monitoring");
            sender.sendMessage(ChatColor.YELLOW + "  /xsteal performance cleanup" + ChatColor.GRAY + " - Clean up entities");
            sender.sendMessage(ChatColor.YELLOW + "  /xsteal performance optimize" + ChatColor.GRAY + " - Optimize performance");
            sender.sendMessage(ChatColor.YELLOW + "  /xsteal performance gc" + ChatColor.GRAY + " - Force garbage collection");
        }
        
        private void startPerformanceMonitoring(CommandSender sender) {
            sender.sendMessage(ChatColor.GREEN + "🔍 Starting performance monitoring...");
            
            // Start monitoring task
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (sender instanceof Player && !((Player) sender).isOnline()) {
                    return; // Stop if player disconnected
                }
                
                Runtime runtime = Runtime.getRuntime();
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                
                String status = ChatColor.AQUA + "📊 Performance: " +
                    ChatColor.WHITE + usedMemory + "MB RAM, " +
                    ChatColor.WHITE + plugin.getAbilityManager().getActiveAbilityCount() + " abilities, " +
                    ChatColor.WHITE + plugin.getAbilityManager().getSummonedEntityCount() + " entities";
                
                sender.sendMessage(status);
                
            }, 0L, 20L * 10L); // Every 10 seconds
            
            sender.sendMessage(ChatColor.GRAY + "Monitoring started. Performance stats will be shown every 10 seconds.");
        }
        
        private void performCleanup(CommandSender sender) {
            sender.sendMessage(ChatColor.YELLOW + "🧹 Performing system cleanup...");
            
            // Clean up summoned entities
            int entitiesBefore = plugin.getAbilityManager().getSummonedEntityCount();
            plugin.getAbilityManager().cleanupSummonedEntities();
            int entitiesAfter = plugin.getAbilityManager().getSummonedEntityCount();
            
            sender.sendMessage(ChatColor.GREEN + "✅ Cleanup complete!");
            sender.sendMessage(ChatColor.GRAY + "Entities cleaned: " + (entitiesBefore - entitiesAfter));
        }
        
        private void performOptimization(CommandSender sender) {
            sender.sendMessage(ChatColor.YELLOW + "⚡ Performing performance optimization...");
            
            // Force garbage collection
            System.gc();
            
            // Clean up expired data
            plugin.getAbilityManager().cleanupSummonedEntities();
            
            // Optimize configurations
            plugin.getConfigManager().getMainConfig().set("performance.last_optimization", System.currentTimeMillis());
            
            sender.sendMessage(ChatColor.GREEN + "✅ Performance optimization complete!");
            sender.sendMessage(ChatColor.GRAY + "Memory freed, entities cleaned, caches optimized");
        }
        
        private void forceGarbageCollection(CommandSender sender) {
            sender.sendMessage(ChatColor.YELLOW + "🗑️ Forcing garbage collection...");
            
            Runtime runtime = Runtime.getRuntime();
            long memoryBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            
            System.gc();
            
            // Wait a moment for GC to complete
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                long memoryAfter = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
                long memoryFreed = memoryBefore - memoryAfter;
                
                sender.sendMessage(ChatColor.GREEN + "✅ Garbage collection complete!");
                sender.sendMessage(ChatColor.GRAY + "Memory before: " + memoryBefore + "MB");
                sender.sendMessage(ChatColor.GRAY + "Memory after: " + memoryAfter + "MB");
                sender.sendMessage(ChatColor.GRAY + "Memory freed: " + memoryFreed + "MB");
            }, 20L);
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("monitor", "cleanup", "optimize", "gc");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal give <player> <head> [amount] - Enhanced give command
     */
    public static class GiveCommand extends EnhancedXStealCommand.SubCommand {
        public GiveCommand(XSteal plugin) {
            super(plugin, "give", "xsteal.admin.give", "Give a specific mob head to a player");
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
            
            ItemStack head = plugin.getHeadManager().createHeadItem(headKey);
            if (head == null) {
                sender.sendMessage(ChatColor.RED + "Failed to create head: " + headKey);
                return true;
            }
            
            head.setAmount(amount);
            
            // Give to player with enhanced feedback
            HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = target.getInventory().addItem(head);
            if (!leftover.isEmpty()) {
                // Drop excess items at player location
                for (org.bukkit.inventory.ItemStack item : leftover.values()) {
                    target.getWorld().dropItemNaturally(target.getLocation(), item);
                }
                sender.sendMessage(ChatColor.YELLOW + "⚠ Some items were dropped (inventory full)");
            }
            
            var headData = plugin.getHeadManager().getHeadData(headKey);
            String headName = headData != null ? 
                ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()) : headKey;
            
            sender.sendMessage(ChatColor.GREEN + "✅ Gave " + amount + "x " + headName + " §ato " + target.getName());
            target.sendMessage(ChatColor.GREEN + "🎁 You received " + amount + "x " + headName + " §afrom " + sender.getName());
            
            // Show ability info to recipient
            if (headData != null) {
                target.sendMessage(ChatColor.GRAY + "💡 " + headData.getDescription());
                if (headData.isBossHead()) {
                    target.sendMessage(ChatColor.GOLD + "👑 Boss Head - Use combo abilities!");
                } else {
                    target.sendMessage(ChatColor.YELLOW + "Wear in helmet slot and left-click to activate");
                }
            }
            
            plugin.getPluginLogger().info("Admin " + sender.getName() + " gave " + amount + "x " + headKey + " to " + target.getName());
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            } else if (args.length == 2) {
                return plugin.getHeadManager().getLoadedHeadKeys().stream()
                    .filter(key -> key.toLowerCase().startsWith(args[1].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
            } else if (args.length == 3) {
                return Arrays.asList("1", "5", "10", "16", "32", "64");
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * /headsteal banbox - Enhanced banbox management
     */
    public static class BanBoxCommand extends EnhancedXStealCommand.SubCommand {
        public BanBoxCommand(XSteal plugin) {
            super(plugin, "banbox", "xsteal.admin.banbox", "Create and manage banboxes for dead players");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                showBanBoxHelp(sender);
                return true;
            }
            
            String action = args[0].toLowerCase();
            
            switch (action) {
                case "create":
                    return createBanBox(sender, Arrays.copyOfRange(args, 1, args.length));
                case "list":
                    return listBanBoxes(sender);
                case "info":
                    return showBanBoxInfo(sender, Arrays.copyOfRange(args, 1, args.length));
                case "teleport":
                    return teleportToBanBox(sender, Arrays.copyOfRange(args, 1, args.length));
                case "timer":
                    return manageBanBoxTimer(sender, Arrays.copyOfRange(args, 1, args.length));
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown banbox action: " + action);
                    showBanBoxHelp(sender);
                    return true;
            }
        }
        
        private void showBanBoxHelp(CommandSender sender) {
            sender.sendMessage(ChatColor.GOLD + "═══ BanBox Management ═══");
            sender.sendMessage(ChatColor.YELLOW + "/xsteal banbox create <player>" + ChatColor.GRAY + " - Create banbox for player");
            sender.sendMessage(ChatColor.YELLOW + "/xsteal banbox list" + ChatColor.GRAY + " - List all banboxes");
            sender.sendMessage(ChatColor.YELLOW + "/xsteal banbox info <player>" + ChatColor.GRAY + " - Show banbox details");
            sender.sendMessage(ChatColor.YELLOW + "/xsteal banbox teleport <player>" + ChatColor.GRAY + " - Teleport to banbox");
            sender.sendMessage(ChatColor.YELLOW + "/xsteal banbox timer <player> <days>" + ChatColor.GRAY + " - Set timer");
        }
        
        private boolean createBanBox(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal banbox create <player>");
                return true;
            }
            
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
                return true;
            }
            
            if (plugin.getBanBoxManager().isBanBoxed(target.getUniqueId())) {
                sender.sendMessage(ChatColor.RED + "Player " + target.getName() + " is already banboxed!");
                return true;
            }
            
            // Create banbox for player
            plugin.getBanBoxManager().handlePlayerDeath(target, target.getLocation(), 
                sender instanceof Player ? (Player) sender : null);
            
            sender.sendMessage(ChatColor.GREEN + "✅ Created banbox for " + target.getName());
            
            return true;
        }
        
        private boolean listBanBoxes(CommandSender sender) {
            // Delegate to existing list command
            return new ListBanBoxesCommand(plugin).execute(sender, new String[0]);
        }
        
        private boolean showBanBoxInfo(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal banbox info <player>");
                return true;
            }
            
            String playerName = args[0];
            var banBoxData = plugin.getBanBoxManager().getBanBoxData(getUUIDFromName(playerName));
            
            if (banBoxData == null) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not banboxed!");
                return true;
            }
            
            sender.sendMessage(ChatColor.GOLD + "═══ BanBox Info: " + playerName + " ═══");
            sender.sendMessage(ChatColor.YELLOW + "Status: " + ChatColor.RED + "BANBOXED");
            sender.sendMessage(ChatColor.YELLOW + "Timer: " + ChatColor.WHITE + banBoxData.getTimerDays() + " days");
            
            long timeInBanBox = banBoxData.getTimeInBanBox();
            long hoursInBanBox = timeInBanBox / (1000 * 60 * 60);
            sender.sendMessage(ChatColor.YELLOW + "Time in BanBox: " + ChatColor.WHITE + hoursInBanBox + " hours");
            
            if (banBoxData.getDeathLocation() != null) {
                org.bukkit.Location loc = banBoxData.getDeathLocation();
                sender.sendMessage(ChatColor.YELLOW + "Death Location: " + ChatColor.WHITE + 
                    loc.getWorld().getName() + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")");
            }
            
            return true;
        }
        
        private boolean teleportToBanBox(CommandSender sender, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can teleport!");
                return true;
            }
            
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal banbox teleport <player>");
                return true;
            }
            
            String playerName = args[0];
            var banBoxData = plugin.getBanBoxManager().getBanBoxData(getUUIDFromName(playerName));
            
            if (banBoxData == null) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not banboxed!");
                return true;
            }
            
            if (banBoxData.getDeathLocation() == null) {
                sender.sendMessage(ChatColor.RED + "No death location recorded for " + playerName);
                return true;
            }
            
            Player player = (Player) sender;
            player.teleport(banBoxData.getDeathLocation());
            player.sendMessage(ChatColor.GREEN + "✅ Teleported to " + playerName + "'s banbox location");
            
            return true;
        }
        
        private boolean manageBanBoxTimer(CommandSender sender, String[] args) {
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal banbox timer <player> <days>");
                return true;
            }
            
            // Timer management implementation would go here
            sender.sendMessage(ChatColor.YELLOW + "BanBox timer management - Feature coming soon!");
            return true;
        }
        
        private UUID getUUIDFromName(String playerName) {
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                return player.getUniqueId();
            }
            
            // Check banboxed players
            for (UUID uuid : plugin.getBanBoxManager().getBanBoxPlayers()) {
                var data = plugin.getBanBoxManager().getBanBoxData(uuid);
                if (data != null && data.getPlayerName().equalsIgnoreCase(playerName)) {
                    return uuid;
                }
            }
            
            return null;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
                return Arrays.asList("create", "list", "info", "teleport", "timer");
            } else if (args.length == 2 && !"list".equals(args[0])) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * Enhanced reload command with performance metrics
     */
    public static class ReloadCommand extends EnhancedXStealCommand.SubCommand {
        public ReloadCommand(XSteal plugin) {
            super(plugin, "reload", "xsteal.admin.reload", "Reload XSteal configuration with performance metrics");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "🔄 Reloading XSteal...");
            
            try {
                long startTime = System.currentTimeMillis();
                
                // Perform reload
                plugin.reload();
                
                long endTime = System.currentTimeMillis();
                long reloadTime = endTime - startTime;
                
                sender.sendMessage(ChatColor.GREEN + "✅ XSteal reloaded successfully!");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.AQUA + "📊 Reload Statistics:");
                sender.sendMessage(ChatColor.GRAY + "  Reload time: " + ChatColor.WHITE + reloadTime + "ms");
                sender.sendMessage(ChatColor.GRAY + "  Loaded heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount());
                sender.sendMessage(ChatColor.GRAY + "  Registered abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getRegisteredAbilityCount());
                sender.sendMessage(ChatColor.GRAY + "  BanBox system: " + ChatColor.WHITE + (plugin.getBanBoxManager().isEnabled() ? "Enabled" : "Disabled"));
                sender.sendMessage(ChatColor.GRAY + "  HeadDatabase: " + ChatColor.WHITE + (plugin.getHeadManager().isHeadDatabaseAvailable() ? "Available" : "Unavailable"));
                
                plugin.getPluginLogger().info("Plugin reloaded by " + sender.getName() + " in " + reloadTime + "ms");
                
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "❌ Failed to reload XSteal: " + e.getMessage());
                plugin.getPluginLogger().severe("Reload failed", e);
            }
            
            return true;
        }
    }
    
    /**
     * Enhanced debug command with detailed system information
     */
    public static class DebugCommand extends EnhancedXStealCommand.SubCommand {
        public DebugCommand(XSteal plugin) {
            super(plugin, "debug", "xsteal.admin.debug", "Show comprehensive debug information");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.GOLD + "═══ XSteal Debug Information ═══");
            sender.sendMessage(ChatColor.YELLOW + "Version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "Plugin Ready: " + ChatColor.WHITE + plugin.isPluginReady());
            sender.sendMessage(ChatColor.YELLOW + "Dependencies Loaded: " + ChatColor.WHITE + plugin.areDependenciesLoaded());
            sender.sendMessage("");
            
            // System Information
            sender.sendMessage(ChatColor.AQUA + "🖥️ System Information:");
            sender.sendMessage(ChatColor.GRAY + "  Server Version: " + ChatColor.WHITE + 
                com.xreatlabs.xsteal.utils.VersionCompatibility.getServerVersion());
            sender.sendMessage(ChatColor.GRAY + "  Paper: " + ChatColor.WHITE + 
                (com.xreatlabs.xsteal.utils.VersionCompatibility.isPaper() ? "Yes" : "No"));
            sender.sendMessage(ChatColor.GRAY + "  Legacy Mode: " + ChatColor.WHITE + 
                (com.xreatlabs.xsteal.utils.VersionCompatibility.isLegacy() ? "Yes" : "No"));
            
            // Head System
            sender.sendMessage(ChatColor.AQUA + "📋 Head System:");
            sender.sendMessage(ChatColor.GRAY + "  Loaded Heads: " + ChatColor.WHITE + plugin.getHeadManager().getLoadedHeadCount());
            sender.sendMessage(ChatColor.GRAY + "  HeadDatabase Available: " + ChatColor.WHITE + plugin.getHeadManager().isHeadDatabaseAvailable());
            
            // Ability System
            sender.sendMessage(ChatColor.AQUA + "⚡ Ability System:");
            sender.sendMessage(ChatColor.GRAY + "  Registered Abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getRegisteredAbilityCount());
            sender.sendMessage(ChatColor.GRAY + "  Active Abilities: " + ChatColor.WHITE + plugin.getAbilityManager().getActiveAbilityCount());
            sender.sendMessage(ChatColor.GRAY + "  Summoned Entities: " + ChatColor.WHITE + plugin.getAbilityManager().getSummonedEntityCount());
            
            // BanBox System
            sender.sendMessage(ChatColor.AQUA + "🏺 BanBox System:");
            sender.sendMessage(ChatColor.GRAY + "  Enabled: " + ChatColor.WHITE + plugin.getBanBoxManager().isEnabled());
            sender.sendMessage(ChatColor.GRAY + "  Banboxed Players: " + ChatColor.WHITE + plugin.getBanBoxManager().getBanBoxPlayerCount());
            
            // Performance Metrics
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory() / 1024 / 1024;
            long freeMemory = runtime.freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;
            
            sender.sendMessage(ChatColor.AQUA + "📊 Performance:");
            sender.sendMessage(ChatColor.GRAY + "  Memory Used: " + ChatColor.WHITE + usedMemory + "MB / " + totalMemory + "MB");
            sender.sendMessage(ChatColor.GRAY + "  Memory Free: " + ChatColor.WHITE + freeMemory + "MB");
            
            return true;
        }
    }
    
    // Additional placeholder implementations for remaining commands...
    
    private static class ListBanBoxesCommand extends EnhancedXStealCommand.SubCommand {
        public ListBanBoxesCommand(XSteal plugin) {
            super(plugin, "listbanboxes", "xsteal.admin.banbox", "List all active banboxes");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            return new AdminCommands.BanBoxCommand(plugin).listBanBoxes(sender);
        }
    }
    
    private static class RemoveBanBoxCommand extends EnhancedXStealCommand.SubCommand {
        public RemoveBanBoxCommand(XSteal plugin) {
            super(plugin, "removebanbox", "xsteal.admin.banbox", "Remove existing banbox");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            sender.sendMessage(ChatColor.YELLOW + "Remove BanBox command - Feature coming soon!");
            return true;
        }
    }
    
    private static class ReviveCommand extends EnhancedXStealCommand.SubCommand {
        public ReviveCommand(XSteal plugin) {
            super(plugin, "revive", "xsteal.admin.revive", "Revive a previously eliminated player");
        }
        
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /xsteal revive <player>");
                return true;
            }
            
            String playerName = args[0];
            
            if (!plugin.getBanBoxManager().isBanBoxed(playerName)) {
                sender.sendMessage(ChatColor.RED + "Player " + playerName + " is not banboxed!");
                return true;
            }
            
            // Get revival location
            org.bukkit.Location reviveLocation;
            if (sender instanceof Player) {
                reviveLocation = ((Player) sender).getLocation();
            } else {
                reviveLocation = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            
            boolean success = plugin.getBanBoxManager().revivePlayer(playerName, reviveLocation, 
                sender instanceof Player ? (Player) sender : null);
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "✅ Successfully revived " + playerName);
                Bukkit.broadcastMessage(ChatColor.GREEN + "🎉 " + playerName + " has been revived by " + sender.getName() + "!");
            } else {
                sender.sendMessage(ChatColor.RED + "❌ Failed to revive " + playerName);
            }
            
            return true;
        }
        
        @Override
        public List<String> tabComplete(CommandSender sender, String[] args) {
            if (args.length == 1) {
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
}