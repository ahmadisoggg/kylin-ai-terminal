package com.xreatlabs.xsteal.commands;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Give Creeper Arrow Command
 * Gives special creeper arrows required for boss head acquisition
 */
public class GiveCreeperArrowCommand extends EnhancedXStealCommand.SubCommand {
    
    public GiveCreeperArrowCommand(XSteal plugin) {
        super(plugin, "givecreeperarrow", "xsteal.admin.give", "Give creeper arrows for boss head hunting");
    }
    
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /xsteal givecreeperarrow <player> [amount]");
            sender.sendMessage(ChatColor.YELLOW + "Creeper arrows are required to get boss heads!");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[0]);
            return true;
        }
        
        int amount = 1;
        if (args.length > 1) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0 || amount > 64) {
                    sender.sendMessage(ChatColor.RED + "Amount must be between 1 and 64");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "Invalid amount: " + args[1]);
                return true;
            }
        }
        
        // Give creeper arrows
        boolean success = plugin.getBossHeadSystem().giveCreeperArrow(target, amount);
        
        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✅ Gave " + amount + " Creeper Arrow" + (amount > 1 ? "s" : "") + " to " + target.getName());
            sender.sendMessage(ChatColor.GRAY + "They can now hunt boss heads!");
            
            target.sendMessage(ChatColor.GREEN + "🏹 You received " + amount + " Creeper Arrow" + (amount > 1 ? "s" : "") + " from " + sender.getName());
            target.sendMessage(ChatColor.YELLOW + "💡 How to get boss heads:");
            target.sendMessage(ChatColor.GRAY + "1. Shoot Ender Dragon/Wither/Warden with creeper arrow");
            target.sendMessage(ChatColor.GRAY + "2. Kill the boss within 30 seconds");
            target.sendMessage(ChatColor.GRAY + "3. Boss head will drop!");
            
            plugin.getPluginLogger().info("Admin " + sender.getName() + " gave " + amount + " creeper arrows to " + target.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "❌ Failed to give creeper arrows!");
        }
        
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
            return Arrays.asList("1", "5", "10", "16", "32", "64");
        }
        return Collections.emptyList();
    }
}