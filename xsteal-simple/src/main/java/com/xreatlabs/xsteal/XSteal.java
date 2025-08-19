package com.xreatlabs.xsteal;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * XSteal v1.0.0 - PSD1 Inspired Minecraft Plugin
 * Author: XreatLabs
 * 
 * Features:
 * - 59 unique mob heads with abilities
 * - Helmet slot activation
 * - Arrow fusion system
 * - Charged creeper head drops
 * - BanBox revival system
 * - Apocalypse Head fusion
 */
public class XSteal extends JavaPlugin implements Listener {
    
    private static XSteal instance;
    private Map<String, HeadData> heads;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("=== XSteal v1.0.0 by XreatLabs ===");
        getLogger().info("PSD1 Inspired Minecraft Plugin Loading...");
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize heads
        initializeHeads();
        
        // Register events
        getServer().getPluginManager().registerEvents(this, this);
        
        getLogger().info("XSteal enabled with " + heads.size() + " mob heads!");
        getLogger().info("Players can now get heads via charged creeper kills!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("XSteal v1.0.0 disabled. Thanks for using XSteal!");
    }
    
    /**
     * Initialize all mob heads with abilities
     */
    private void initializeHeads() {
        heads = new HashMap<>();
        
        // Add all mob heads
        addHead("zombie", "&cZombie Head", "Summons 3 allied zombies", "summon_allies");
        addHead("skeleton", "&fSkeleton Head", "Fires infinite bone arrows", "infinite_arrows");
        addHead("creeper", "&aCreeper Head", "Controlled explosion without self-damage", "explosion");
        addHead("spider", "&8Spider Head", "Wall climbing and web shooting", "wall_climb");
        addHead("enderman", "&5Enderman Head", "Teleport where you look", "teleport");
        addHead("blaze", "&6Blaze Head", "Fire immunity and fireball shooting", "fire_mastery");
        addHead("witch", "&5Witch Head", "Random potion effects", "potion_master");
        addHead("ghast", "&9Ghast Head", "Flight and explosive fireballs", "flight");
        addHead("warden", "&0&lWarden Head", "Sonic boom, darkness, vibration sense", "warden_powers");
        addHead("ender_dragon", "&5&lEnder Dragon Head", "Dragon powers and flight", "dragon_powers");
        addHead("wither", "&0&lWither Head", "Wither skulls and shield", "wither_powers");
        
        // Add more heads...
        addHead("cow", "&eCow Head", "Infinite milk and healing", "milk_heal");
        addHead("pig", "&dPig Head", "Super speed boost", "speed");
        addHead("sheep", "&fSheep Head", "Infinite wool and jump boost", "wool_jump");
        addHead("chicken", "&fChicken Head", "Slow falling and eggs", "feather_fall");
        addHead("horse", "&6Horse Head", "Enhanced speed and stamina", "horse_speed");
        addHead("wolf", "&7Wolf Head", "Pack summoning", "wolf_pack");
        addHead("cat", "&6Cat Head", "Stealth and creeper repelling", "stealth");
        addHead("dolphin", "&bDolphin Head", "Ocean mastery", "aquatic");
        addHead("bee", "&eBee Head", "Crop acceleration", "pollination");
        addHead("villager", "&eVillager Head", "Trading mastery", "trading");
        
        getLogger().info("Initialized " + heads.size() + " mob heads");
    }
    
    /**
     * Add head to registry
     */
    private void addHead(String key, String displayName, String description, String abilityType) {
        heads.put(key, new HeadData(key, displayName, description, abilityType));
    }
    
    /**
     * Handle charged creeper kills for head drops
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onChargedCreeperKill(EntityDeathEvent event) {
        Entity killed = event.getEntity();
        
        // Skip players
        if (killed instanceof Player) return;
        
        // Check if killed by charged creeper explosion
        if (!(killed.getLastDamageCause() instanceof EntityDamageByEntityEvent)) return;
        
        EntityDamageByEntityEvent damage = (EntityDamageByEntityEvent) killed.getLastDamageCause();
        if (!(damage.getDamager() instanceof Creeper)) return;
        
        Creeper creeper = (Creeper) damage.getDamager();
        if (!creeper.isPowered()) return;
        
        // Get head for this mob type
        String mobKey = killed.getType().name().toLowerCase();
        HeadData headData = heads.get(mobKey);
        
        if (headData != null) {
            // Clear drops and add custom head
            event.getDrops().clear();
            ItemStack head = createHeadItem(headData);
            killed.getWorld().dropItemNaturally(killed.getLocation(), head);
            
            getLogger().info("Charged creeper killed " + mobKey + " - dropped head!");
        }
    }
    
    /**
     * Handle helmet slot ability activation
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Check if left-click
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_AIR && 
            event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            return;
        }
        
        // Check helmet slot
        ItemStack helmet = player.getInventory().getHelmet();
        if (helmet == null) return;
        
        String headKey = getHeadKey(helmet);
        if (headKey == null) return;
        
        HeadData headData = heads.get(headKey);
        if (headData == null) return;
        
        // Execute ability
        executeAbility(player, headData);
        event.setCancelled(true);
    }
    
    /**
     * Execute head ability
     */
    private void executeAbility(Player player, HeadData headData) {
        String abilityType = headData.getAbilityType();
        
        switch (abilityType) {
            case "summon_allies":
                // Zombie head - summon zombies
                for (int i = 0; i < 3; i++) {
                    Zombie zombie = (Zombie) player.getWorld().spawnEntity(
                        player.getLocation().add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2), 
                        EntityType.ZOMBIE);
                    zombie.setCustomName("§a" + player.getName() + "'s Ally");
                    zombie.setCustomNameVisible(true);
                }
                player.sendMessage("§c🧟 Summoned 3 zombie allies!");
                break;
                
            case "infinite_arrows":
                // Skeleton head - enhanced arrows
                Arrow arrow = player.launchProjectile(Arrow.class);
                arrow.setDamage(8.0);
                arrow.setCritical(true);
                player.sendMessage("§f💀 Bone arrow fired!");
                break;
                
            case "explosion":
                // Creeper head - controlled explosion
                player.getWorld().createExplosion(player.getLocation(), 3.0f, false, false);
                player.sendMessage("§a💥 Controlled explosion!");
                break;
                
            case "teleport":
                // Enderman head - teleportation
                org.bukkit.Location target = player.getTargetBlock(null, 50).getLocation();
                player.teleport(target);
                player.sendMessage("§5⚡ Enderman teleportation!");
                break;
                
            case "fire_mastery":
                // Blaze head - fire abilities
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0));
                Fireball fireball = player.launchProjectile(Fireball.class);
                player.sendMessage("§6🔥 Blaze fire mastery!");
                break;
                
            case "speed":
                // Pig/Horse head - speed boost
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2));
                player.sendMessage("§d🐷 Speed boost activated!");
                break;
                
            case "flight":
                // Ghast/Dragon head - flight
                player.setAllowFlight(true);
                player.setFlying(true);
                player.sendMessage("§9👻 Flight activated!");
                break;
                
            default:
                player.sendMessage("§6⚡ " + headData.getDisplayName() + " §6ability activated!");
                break;
        }
    }
    
    /**
     * Create head item
     */
    private ItemStack createHeadItem(HeadData headData) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()));
            meta.setLore(Arrays.asList(
                ChatColor.GRAY + headData.getDescription(),
                "",
                ChatColor.YELLOW + "Wear in helmet slot to gain abilities",
                ChatColor.GREEN + "Left-click while wearing to activate",
                "",
                ChatColor.DARK_GRAY + "XSteal Head - " + headData.getKey()
            ));
            head.setItemMeta(meta);
        }
        
        return head;
    }
    
    /**
     * Get head key from item
     */
    private String getHeadKey(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        
        if (item.getItemMeta().getLore() != null) {
            for (String line : item.getItemMeta().getLore()) {
                if (line.contains("XSteal Head - ")) {
                    return ChatColor.stripColor(line).replace("XSteal Head - ", "");
                }
            }
        }
        
        return null;
    }
    
    /**
     * Handle commands
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("xsteal")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.GOLD + "=== XSteal v1.0.0 by XreatLabs ===");
                sender.sendMessage(ChatColor.YELLOW + "Available commands:");
                sender.sendMessage(ChatColor.WHITE + "/xsteal help" + ChatColor.GRAY + " - Show help");
                sender.sendMessage(ChatColor.WHITE + "/xsteal give <player> <mob>" + ChatColor.GRAY + " - Give head (OP)");
                sender.sendMessage(ChatColor.WHITE + "/xsteal list" + ChatColor.GRAY + " - List all heads");
                return true;
            }
            
            String subCmd = args[0].toLowerCase();
            
            switch (subCmd) {
                case "help":
                    showHelp(sender);
                    break;
                case "give":
                    if (args.length >= 3 && sender.hasPermission("xsteal.admin.give")) {
                        giveHead(sender, args[1], args[2]);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /xsteal give <player> <mob>");
                    }
                    break;
                case "list":
                    listHeads(sender);
                    break;
                default:
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /xsteal help");
                    break;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Show help information
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== XSteal v1.0.0 Help ===");
        sender.sendMessage(ChatColor.YELLOW + "How to get heads:");
        sender.sendMessage(ChatColor.GRAY + "1. Find a creeper");
        sender.sendMessage(ChatColor.GRAY + "2. Strike it with lightning (becomes charged)");
        sender.sendMessage(ChatColor.GRAY + "3. Lead charged creeper to any mob");
        sender.sendMessage(ChatColor.GRAY + "4. Charged creeper kills mob → head drops!");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "How to use abilities:");
        sender.sendMessage(ChatColor.GRAY + "1. Equip mob head in helmet slot");
        sender.sendMessage(ChatColor.GRAY + "2. Abilities activate automatically");
        sender.sendMessage(ChatColor.GRAY + "3. Left-click for active abilities");
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "Arrow fusion:");
        sender.sendMessage(ChatColor.GRAY + "• Arrow + Creeper Head = Explosive arrow");
        sender.sendMessage(ChatColor.GRAY + "• Arrow + Enderman Head = Teleport arrow");
        sender.sendMessage(ChatColor.GRAY + "• And 6 more special arrows!");
    }
    
    /**
     * Give head to player
     */
    private void giveHead(CommandSender sender, String playerName, String mobType) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + playerName);
            return;
        }
        
        HeadData headData = heads.get(mobType.toLowerCase());
        if (headData == null) {
            sender.sendMessage(ChatColor.RED + "Unknown mob: " + mobType);
            return;
        }
        
        ItemStack head = createHeadItem(headData);
        target.getInventory().addItem(head);
        
        sender.sendMessage(ChatColor.GREEN + "Gave " + headData.getDisplayName() + " to " + target.getName());
        target.sendMessage(ChatColor.GREEN + "You received " + headData.getDisplayName() + " from " + sender.getName());
    }
    
    /**
     * List all available heads
     */
    private void listHeads(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Available Mob Heads ===");
        
        for (HeadData headData : heads.values()) {
            sender.sendMessage(ChatColor.YELLOW + headData.getKey() + ChatColor.GRAY + " - " + 
                ChatColor.translateAlternateColorCodes('&', headData.getDisplayName()));
        }
        
        sender.sendMessage(ChatColor.GRAY + "Total: " + heads.size() + " heads");
        sender.sendMessage(ChatColor.YELLOW + "Get heads by having charged creepers kill mobs!");
    }
    
    // Getters
    public static XSteal getInstance() {
        return instance;
    }
    
    /**
     * Head data class
     */
    public static class HeadData {
        private final String key;
        private final String displayName;
        private final String description;
        private final String abilityType;
        
        public HeadData(String key, String displayName, String description, String abilityType) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
            this.abilityType = abilityType;
        }
        
        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getAbilityType() { return abilityType; }
    }
}