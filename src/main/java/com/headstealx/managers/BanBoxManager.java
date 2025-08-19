package com.headstealx.managers;

import com.headstealx.Main;
import com.headstealx.util.ConfigUtil;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BanBoxManager handles the banbox system
 * - Managing banned players (spectator mode)
 * - Handling player death and head drops
 * - Processing revivals via head interaction
 * - Auto-unban system
 */
public class BanBoxManager {
    
    private final Main plugin;
    private final Map<UUID, BanBoxData> bannedPlayers;
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    public BanBoxManager(Main plugin) {
        this.plugin = plugin;
        this.bannedPlayers = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "banbox_data.yml");
        
        loadData();
    }
    
    /**
     * Handle player death and banbox placement
     */
    public void handlePlayerDeath(Player player, Location deathLocation) {
        if (!plugin.getConfig().getBoolean("banbox.enabled", true)) {
            return;
        }
        
        // Check if player should be banned
        if (!shouldPlayerBeBanned(player)) {
            return;
        }
        
        plugin.getPluginLogger().debug("Processing banbox death for " + player.getName());
        
        // Create banbox data
        BanBoxData banBoxData = new BanBoxData(
            player.getUniqueId(),
            player.getName(),
            deathLocation,
            System.currentTimeMillis()
        );
        
        // Handle inventory
        if (!plugin.getConfig().getBoolean("banbox.keep_inventory", false)) {
            // Items will drop naturally or be handled by other plugins
            // We just need to track the banbox state
        }
        
        // Apply experience penalty
        double expPenalty = plugin.getConfig().getDouble("banbox.experience_penalty", 0.5);
        if (expPenalty > 0 && expPenalty <= 1.0) {
            int currentExp = player.getTotalExperience();
            int newExp = (int) (currentExp * (1.0 - expPenalty));
            player.setTotalExperience(newExp);
        }
        
        // Add to banned players
        bannedPlayers.put(player.getUniqueId(), banBoxData);
        
        // Put player in spectator mode
        enterBanBoxMode(player, deathLocation);
        
        // Create and drop head
        ItemStack playerHead = plugin.getHeadManager().createBanboxHead(player);
        if (playerHead != null) {
            deathLocation.getWorld().dropItemNaturally(deathLocation, playerHead);
        }
        
        // Save data
        saveData();
        
        // Broadcast if enabled
        if (plugin.getConfig().getBoolean("banbox.broadcast_deaths", true)) {
            String message = ChatColor.RED + player.getName() + " has been banboxed! Find their head to revive them.";
            Bukkit.broadcastMessage(message);
        }
        
        plugin.getPluginLogger().info("Player " + player.getName() + " entered banbox mode");
    }
    
    /**
     * Check if player should be banned (not in creative, not admin, etc.)
     */
    private boolean shouldPlayerBeBanned(Player player) {
        // Don't ban creative mode players
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        
        // Don't ban players with bypass permission
        if (player.hasPermission("headsteal.admin.bypass")) {
            return false;
        }
        
        // Don't ban if player doesn't have drop permission
        if (!player.hasPermission("headsteal.drop")) {
            return false;
        }
        
        // Check world restrictions
        List<String> disabledWorlds = plugin.getConfig().getStringList("general.head_drop.disabled_worlds");
        if (!disabledWorlds.isEmpty() && disabledWorlds.contains(player.getWorld().getName())) {
            return false;
        }
        
        List<String> enabledWorlds = plugin.getConfig().getStringList("general.head_drop.enabled_worlds");
        if (!enabledWorlds.isEmpty() && !enabledWorlds.contains(player.getWorld().getName())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Put player into banbox mode (spectator)
     */
    private void enterBanBoxMode(Player player, Location deathLocation) {
        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport to death location (slightly above)
        Location spectatorLocation = deathLocation.clone().add(0, 2, 0);
        player.teleport(spectatorLocation);
        
        // Send messages
        player.sendMessage(ChatColor.RED + "═══════════════════════════════");
        player.sendMessage(ChatColor.DARK_RED + "        YOU HAVE DIED!");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "You are now in BanBox mode.");
        player.sendMessage(ChatColor.GRAY + "Another player must find and left-click");
        player.sendMessage(ChatColor.GRAY + "your head to revive you at this location.");
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "If your head is destroyed, you will be");
        player.sendMessage(ChatColor.RED + "banned for " + plugin.getConfig().getInt("banbox.auto_unban_days", 7) + " days!");
        player.sendMessage(ChatColor.RED + "═══════════════════════════════");
        
        // Play sound
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        
        // Schedule reminder messages
        scheduleReminderMessages(player);
    }
    
    /**
     * Schedule reminder messages for banboxed players
     */
    private void scheduleReminderMessages(Player player) {
        new BukkitRunnable() {
            int count = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !isBanned(player.getUniqueId())) {
                    cancel();
                    return;
                }
                
                count++;
                
                if (count % 5 == 0) { // Every 5 minutes
                    player.sendMessage(ChatColor.YELLOW + "You are still banboxed. Waiting for revival...");
                }
                
                if (count >= 60) { // Stop after 1 hour
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Every minute
    }
    
    /**
     * Revive a player from banbox
     */
    public boolean revive(String playerName, Location reviveLocation, Player reviver) {
        UUID playerUUID = getUUIDFromName(playerName);
        if (playerUUID == null) {
            plugin.getPluginLogger().warning("Could not find UUID for player: " + playerName);
            return false;
        }
        
        return revive(playerUUID, reviveLocation, reviver);
    }
    
    /**
     * Revive a player from banbox
     */
    public boolean revive(UUID playerUUID, Location reviveLocation, Player reviver) {
        BanBoxData banBoxData = bannedPlayers.get(playerUUID);
        if (banBoxData == null) {
            plugin.getPluginLogger().debug("Player not in banbox: " + playerUUID);
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            // Player is offline - remove from banbox but don't revive yet
            bannedPlayers.remove(playerUUID);
            saveData();
            
            if (reviver != null) {
                reviver.sendMessage(ChatColor.YELLOW + "Player " + banBoxData.getPlayerName() + 
                    " will be revived when they log in.");
            }
            
            return true;
        }
        
        // Check cross-world revival
        if (!plugin.getConfig().getBoolean("banbox.cross_world_revive", true)) {
            if (!reviveLocation.getWorld().equals(banBoxData.getDeathLocation().getWorld())) {
                if (reviver != null) {
                    reviver.sendMessage(ChatColor.RED + "Cannot revive across worlds!");
                }
                return false;
            }
        }
        
        // Revive the player
        performRevival(player, reviveLocation, reviver);
        
        // Remove from banbox
        bannedPlayers.remove(playerUUID);
        saveData();
        
        return true;
    }
    
    /**
     * Perform the actual revival
     */
    private void performRevival(Player player, Location reviveLocation, Player reviver) {
        // Set game mode back to survival
        player.setGameMode(GameMode.SURVIVAL);
        
        // Teleport to revival location
        player.teleport(reviveLocation);
        
        // Restore health and food
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20);
        
        // Clear negative effects
        player.getActivePotionEffects().forEach(effect -> {
            if (isNegativeEffect(effect.getType())) {
                player.removePotionEffect(effect.getType());
            }
        });
        
        // Send revival messages
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════");
        player.sendMessage(ChatColor.DARK_GREEN + "      YOU HAVE BEEN REVIVED!");
        player.sendMessage("");
        if (reviver != null) {
            player.sendMessage(ChatColor.YELLOW + "Thanks to: " + ChatColor.WHITE + reviver.getName());
        }
        player.sendMessage(ChatColor.GREEN + "You have been restored to full health.");
        player.sendMessage(ChatColor.GREEN + "═══════════════════════════════");
        
        // Play revival sound
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        
        // Message reviver
        if (reviver != null) {
            reviver.sendMessage(ChatColor.GREEN + "Successfully revived " + player.getName() + "!");
            reviver.playSound(reviver.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            // Give reviver some experience
            reviver.giveExp(100);
        }
        
        // Broadcast if enabled
        if (plugin.getConfig().getBoolean("banbox.broadcast_revives", true)) {
            String message = ChatColor.GREEN + player.getName() + " has been revived";
            if (reviver != null) {
                message += " by " + reviver.getName();
            }
            message += "!";
            Bukkit.broadcastMessage(message);
        }
        
        plugin.getPluginLogger().info("Player " + player.getName() + " was revived" + 
            (reviver != null ? " by " + reviver.getName() : ""));
    }
    
    /**
     * Check if potion effect is negative
     */
    private boolean isNegativeEffect(org.bukkit.potion.PotionEffectType effectType) {
        return effectType.equals(org.bukkit.potion.PotionEffectType.POISON) ||
               effectType.equals(org.bukkit.potion.PotionEffectType.WITHER) ||
               effectType.equals(org.bukkit.potion.PotionEffectType.SLOWNESS) ||
               effectType.equals(org.bukkit.potion.PotionEffectType.WEAKNESS) ||
               effectType.equals(org.bukkit.potion.PotionEffectType.HUNGER) ||
               effectType.equals(org.bukkit.potion.PotionEffectType.BLINDNESS) ||
               effectType.equals(org.bukkit.potion.PotionEffectType.CONFUSION);
    }
    
    /**
     * Handle head destruction (permanent ban)
     */
    public void handleHeadDestruction(String playerName) {
        UUID playerUUID = getUUIDFromName(playerName);
        if (playerUUID == null) return;
        
        BanBoxData banBoxData = bannedPlayers.get(playerUUID);
        if (banBoxData == null) return;
        
        // Convert to permanent ban
        banBoxData.setPermanentBan(true);
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            // Kick player with message
            int autounbanDays = plugin.getConfig().getInt("banbox.auto_unban_days", 7);
            String kickMessage = ChatColor.RED + "Your head was destroyed!\n" +
                ChatColor.YELLOW + "You are banned for " + autounbanDays + " days.\n" +
                ChatColor.GRAY + "Auto-unban: " + new Date(System.currentTimeMillis() + (autounbanDays * 24L * 60L * 60L * 1000L));
            
            player.kickPlayer(kickMessage);
        }
        
        saveData();
        
        plugin.getPluginLogger().info("Player " + playerName + " permanently banned due to head destruction");
    }
    
    /**
     * Process auto-unbans
     */
    public void processAutoUnbans() {
        int autoUnbanDays = plugin.getConfig().getInt("banbox.auto_unban_days", 7);
        if (autoUnbanDays <= 0) return; // Auto-unban disabled
        
        long autoUnbanTime = autoUnbanDays * 24L * 60L * 60L * 1000L; // Days to milliseconds
        long currentTime = System.currentTimeMillis();
        
        List<UUID> toUnban = new ArrayList<>();
        
        for (Map.Entry<UUID, BanBoxData> entry : bannedPlayers.entrySet()) {
            BanBoxData data = entry.getValue();
            
            if (data.isPermanentBan() && (currentTime - data.getBanTime()) > autoUnbanTime) {
                toUnban.add(entry.getKey());
            }
        }
        
        for (UUID uuid : toUnban) {
            BanBoxData data = bannedPlayers.remove(uuid);
            plugin.getPluginLogger().info("Auto-unbanned player: " + data.getPlayerName());
        }
        
        if (!toUnban.isEmpty()) {
            saveData();
        }
    }
    
    /**
     * Check if player is banned
     */
    public boolean isBanned(UUID playerUUID) {
        return bannedPlayers.containsKey(playerUUID);
    }
    
    /**
     * Check if player is banned
     */
    public boolean isBanned(String playerName) {
        UUID uuid = getUUIDFromName(playerName);
        return uuid != null && isBanned(uuid);
    }
    
    /**
     * Get ban data for player
     */
    public BanBoxData getBanData(UUID playerUUID) {
        return bannedPlayers.get(playerUUID);
    }
    
    /**
     * Manually unban a player (admin command)
     */
    public boolean unban(String playerName) {
        UUID uuid = getUUIDFromName(playerName);
        if (uuid == null) return false;
        
        BanBoxData data = bannedPlayers.remove(uuid);
        if (data == null) return false;
        
        // If player is online and in spectator mode, revive them
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
            Location spawnLocation = getDefaultSpawnLocation();
            performRevival(player, spawnLocation, null);
        }
        
        saveData();
        plugin.getPluginLogger().info("Manually unbanned player: " + playerName);
        return true;
    }
    
    /**
     * Get default spawn location from config
     */
    private Location getDefaultSpawnLocation() {
        String locationString = plugin.getConfig().getString("banbox.banbox_default_location", "world:0:100:0");
        String[] parts = locationString.split(":");
        
        if (parts.length >= 4) {
            try {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    double x = Double.parseDouble(parts[1]);
                    double y = Double.parseDouble(parts[2]);
                    double z = Double.parseDouble(parts[3]);
                    return new Location(world, x, y, z);
                }
            } catch (NumberFormatException e) {
                plugin.getPluginLogger().warning("Invalid default location format: " + locationString);
            }
        }
        
        // Fallback to world spawn
        World defaultWorld = Bukkit.getWorlds().get(0);
        return defaultWorld.getSpawnLocation();
    }
    
    /**
     * Get UUID from player name (with caching)
     */
    private UUID getUUIDFromName(String playerName) {
        // First check online players
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        // Check banned players cache
        for (BanBoxData data : bannedPlayers.values()) {
            if (data.getPlayerName().equalsIgnoreCase(playerName)) {
                return data.getPlayerUUID();
            }
        }
        
        // Fallback to Bukkit's offline player (less reliable)
        try {
            return Bukkit.getOfflinePlayer(playerName).getUniqueId();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Load banbox data from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Failed to create banbox data file: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("banned_players")) {
            for (String uuidString : dataConfig.getConfigurationSection("banned_players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String path = "banned_players." + uuidString;
                    
                    String playerName = dataConfig.getString(path + ".name");
                    long banTime = dataConfig.getLong(path + ".ban_time");
                    boolean permanentBan = dataConfig.getBoolean(path + ".permanent_ban", false);
                    
                    // Load death location
                    Location deathLocation = null;
                    if (dataConfig.contains(path + ".death_location")) {
                        String worldName = dataConfig.getString(path + ".death_location.world");
                        double x = dataConfig.getDouble(path + ".death_location.x");
                        double y = dataConfig.getDouble(path + ".death_location.y");
                        double z = dataConfig.getDouble(path + ".death_location.z");
                        
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            deathLocation = new Location(world, x, y, z);
                        }
                    }
                    
                    BanBoxData data = new BanBoxData(uuid, playerName, deathLocation, banTime);
                    data.setPermanentBan(permanentBan);
                    
                    bannedPlayers.put(uuid, data);
                    
                } catch (Exception e) {
                    plugin.getPluginLogger().warning("Failed to load banbox data for " + uuidString + ": " + e.getMessage());
                }
            }
        }
        
        plugin.getPluginLogger().info("Loaded " + bannedPlayers.size() + " banbox entries");
    }
    
    /**
     * Save banbox data to file
     */
    public void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        
        // Clear existing data
        dataConfig.set("banned_players", null);
        
        // Save current data
        for (Map.Entry<UUID, BanBoxData> entry : bannedPlayers.entrySet()) {
            String uuidString = entry.getKey().toString();
            BanBoxData data = entry.getValue();
            String path = "banned_players." + uuidString;
            
            dataConfig.set(path + ".name", data.getPlayerName());
            dataConfig.set(path + ".ban_time", data.getBanTime());
            dataConfig.set(path + ".permanent_ban", data.isPermanentBan());
            
            if (data.getDeathLocation() != null) {
                Location loc = data.getDeathLocation();
                dataConfig.set(path + ".death_location.world", loc.getWorld().getName());
                dataConfig.set(path + ".death_location.x", loc.getX());
                dataConfig.set(path + ".death_location.y", loc.getY());
                dataConfig.set(path + ".death_location.z", loc.getZ());
            }
        }
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getPluginLogger().severe("Failed to save banbox data: " + e.getMessage());
        }
    }
    
    // Getters and utility methods
    
    public int getBannedPlayerCount() {
        return bannedPlayers.size();
    }
    
    public Set<UUID> getBannedPlayers() {
        return new HashSet<>(bannedPlayers.keySet());
    }
    
    public void reload() {
        plugin.getPluginLogger().info("Reloading BanBoxManager...");
        saveData();
        loadData();
    }
    
    public void cleanup() {
        saveData();
        bannedPlayers.clear();
    }
    
    /**
     * BanBox data class
     */
    public static class BanBoxData {
        private final UUID playerUUID;
        private final String playerName;
        private final Location deathLocation;
        private final long banTime;
        private boolean permanentBan;
        
        public BanBoxData(UUID playerUUID, String playerName, Location deathLocation, long banTime) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.deathLocation = deathLocation;
            this.banTime = banTime;
            this.permanentBan = false;
        }
        
        // Getters and setters
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public Location getDeathLocation() { return deathLocation; }
        public long getBanTime() { return banTime; }
        public boolean isPermanentBan() { return permanentBan; }
        public void setPermanentBan(boolean permanentBan) { this.permanentBan = permanentBan; }
        
        public long getTimeBanned() {
            return System.currentTimeMillis() - banTime;
        }
        
        public boolean isExpired(int autoUnbanDays) {
            if (autoUnbanDays <= 0 || !permanentBan) return false;
            long autoUnbanTime = autoUnbanDays * 24L * 60L * 60L * 1000L;
            return getTimeBanned() > autoUnbanTime;
        }
    }
}