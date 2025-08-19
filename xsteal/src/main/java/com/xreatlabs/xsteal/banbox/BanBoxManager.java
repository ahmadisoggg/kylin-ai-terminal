package com.xreatlabs.xsteal.banbox;

import com.xreatlabs.xsteal.XSteal;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BanBoxManager for XSteal
 * Handles the BanBox system where players are trapped in spectator mode
 * and can be revived by interacting with their dropped head
 */
public class BanBoxManager {
    
    private final XSteal plugin;
    private final Map<UUID, BanBoxData> banBoxPlayers;
    private final File dataFile;
    private FileConfiguration dataConfig;
    private final boolean enabled;
    
    public BanBoxManager(XSteal plugin) {
        this.plugin = plugin;
        this.banBoxPlayers = new ConcurrentHashMap<>();
        this.dataFile = new File(plugin.getDataFolder(), "banbox_data.yml");
        this.enabled = plugin.getConfigManager().isBanBoxEnabled();
        
        if (enabled) {
            loadData();
            plugin.getPluginLogger().info("BanBox system enabled");
        } else {
            plugin.getPluginLogger().info("BanBox system disabled");
        }
    }
    
    /**
     * Handle player death and BanBox placement
     */
    public void handlePlayerDeath(Player player, Location deathLocation, Player killer) {
        if (!enabled) {
            return;
        }
        
        // Check if player should be banboxed
        if (!shouldPlayerBeBanBoxed(player)) {
            return;
        }
        
        plugin.getPluginLogger().info("Processing BanBox death for " + player.getName());
        
        // Create BanBox data
        int timerDays = plugin.getConfigManager().getBanBoxDefaultTimer();
        BanBoxData banBoxData = new BanBoxData(
            player.getUniqueId(),
            player.getName(),
            deathLocation,
            System.currentTimeMillis(),
            timerDays,
            killer != null ? killer.getUniqueId() : null
        );
        
        // Add to BanBox
        banBoxPlayers.put(player.getUniqueId(), banBoxData);
        
        // Put player in spectator mode
        enterBanBoxMode(player, deathLocation);
        
        // Create and drop player head
        ItemStack playerHead = plugin.getHeadManager().createPlayerHead(player);
        if (playerHead != null) {
            deathLocation.getWorld().dropItemNaturally(deathLocation, playerHead);
        }
        
        // Save data
        saveData();
        
        // Broadcast if enabled
        if (plugin.getConfigManager().getMainConfig().getBoolean("banbox.revival_broadcast", true)) {
            String message = ChatColor.RED + "💀 " + player.getName() + " has been trapped in the BanBox!";
            if (killer != null) {
                message += " (Killed by " + killer.getName() + ")";
            }
            Bukkit.broadcastMessage(message);
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Find their head to revive them!");
        }
        
        plugin.getPluginLogger().info("Player " + player.getName() + " entered BanBox mode");
    }
    
    /**
     * Check if player should be banboxed
     */
    private boolean shouldPlayerBeBanBoxed(Player player) {
        // Don't banbox creative mode players
        if (player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        
        // Don't banbox spectator mode players
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        
        // Don't banbox players without permission
        if (!player.hasPermission("xsteal.banbox.enter")) {
            return false;
        }
        
        // Check world restrictions
        String worldName = player.getWorld().getName();
        List<String> disabledWorlds = plugin.getConfigManager().getMainConfig()
            .getStringList("general.head_drops.disabled_worlds");
        
        if (disabledWorlds.contains(worldName)) {
            return false;
        }
        
        // Check if player is already banboxed
        if (isBanBoxed(player.getUniqueId())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Put player into BanBox mode (spectator)
     */
    private void enterBanBoxMode(Player player, Location deathLocation) {
        // Set to spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport to death location (slightly above)
        Location spectatorLocation = deathLocation.clone().add(0, 2, 0);
        player.teleport(spectatorLocation);
        
        // Send BanBox messages
        String enterMessage = plugin.getConfigManager().getMainConfig()
            .getString("banbox.messages.enter_banbox", "&c&lYou have been trapped in the BanBox!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', enterMessage));
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "🏺 You are now in the BanBox!");
        player.sendMessage(ChatColor.GRAY + "• You can still access your inventory");
        player.sendMessage(ChatColor.GRAY + "• You can prepare items for revival");
        player.sendMessage(ChatColor.GRAY + "• Another player must find your head");
        player.sendMessage(ChatColor.GRAY + "• Left-click your head to be revived");
        player.sendMessage("");
        
        int timerDays = plugin.getConfigManager().getBanBoxDefaultTimer();
        String timerMessage = plugin.getConfigManager().getMainConfig()
            .getString("banbox.messages.timer_warning", "&eYou will be released in {days} days")
            .replace("{days}", String.valueOf(timerDays));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', timerMessage));
        
        player.sendMessage("");
        player.sendMessage(ChatColor.RED + "⚠ If your head is destroyed, you will be released immediately!");
        player.sendMessage(ChatColor.RED + "═══════════════════════════════════════");
        
        // Play BanBox sound
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 2.0f);
        
        // Schedule reminder messages
        scheduleReminderMessages(player);
    }
    
    /**
     * Schedule reminder messages for banboxed players
     */
    private void scheduleReminderMessages(Player player) {
        org.bukkit.scheduler.BukkitRunnable reminderTask = new org.bukkit.scheduler.BukkitRunnable() {
            int reminderCount = 0;
            
            @Override
            public void run() {
                if (!player.isOnline() || !isBanBoxed(player.getUniqueId())) {
                    cancel();
                    return;
                }
                
                reminderCount++;
                
                // Send reminder every 5 minutes
                if (reminderCount % 5 == 0) {
                    player.sendMessage(ChatColor.YELLOW + "🏺 You are still in the BanBox. Waiting for revival...");
                    
                    BanBoxData data = getBanBoxData(player.getUniqueId());
                    if (data != null) {
                        long timeInBanBox = System.currentTimeMillis() - data.getBanTime();
                        long hoursInBanBox = timeInBanBox / (1000 * 60 * 60);
                        player.sendMessage(ChatColor.GRAY + "Time in BanBox: " + hoursInBanBox + " hours");
                    }
                }
                
                // Stop reminders after 2 hours
                if (reminderCount >= 120) {
                    cancel();
                }
            }
        };
        
        reminderTask.runTaskTimer(plugin, 20L * 60L, 20L * 60L); // Every minute
    }
    
    /**
     * Revive a player from BanBox
     */
    public boolean revivePlayer(String playerName, Location reviveLocation, Player reviver) {
        UUID playerUUID = getUUIDFromName(playerName);
        if (playerUUID == null) {
            plugin.getPluginLogger().warning("Could not find UUID for player: " + playerName);
            return false;
        }
        
        return revivePlayer(playerUUID, reviveLocation, reviver);
    }
    
    /**
     * Revive a player from BanBox
     */
    public boolean revivePlayer(UUID playerUUID, Location reviveLocation, Player reviver) {
        BanBoxData banBoxData = banBoxPlayers.get(playerUUID);
        if (banBoxData == null) {
            plugin.getPluginLogger().debug("Player not in BanBox: " + playerUUID);
            return false;
        }
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null || !player.isOnline()) {
            // Player is offline - remove from BanBox but don't revive yet
            banBoxPlayers.remove(playerUUID);
            saveData();
            
            if (reviver != null) {
                reviver.sendMessage(ChatColor.YELLOW + "Player " + banBoxData.getPlayerName() + 
                    " will be revived when they log in.");
            }
            
            return true;
        }
        
        // Perform revival
        performRevival(player, reviveLocation, reviver);
        
        // Remove from BanBox
        banBoxPlayers.remove(playerUUID);
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
        String revivedMessage = plugin.getConfigManager().getMainConfig()
            .getString("banbox.messages.revived", "&a&lYou have been revived!");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', revivedMessage));
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "🎉 You have been freed from the BanBox!");
        if (reviver != null) {
            player.sendMessage(ChatColor.YELLOW + "Thanks to: " + ChatColor.WHITE + reviver.getName());
        }
        player.sendMessage(ChatColor.GREEN + "You have been restored to full health.");
        player.sendMessage("");
        
        // Play revival effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, player.getLocation().add(0, 1, 0), 30);
        
        // Message reviver
        if (reviver != null) {
            reviver.sendMessage(ChatColor.GREEN + "✅ Successfully revived " + player.getName() + "!");
            reviver.playSound(reviver.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            reviver.giveExp(200); // Reward for revival
        }
        
        // Broadcast revival
        if (plugin.getConfigManager().getMainConfig().getBoolean("banbox.revival_broadcast", true)) {
            String message = ChatColor.GREEN + "🎉 " + player.getName() + " has been revived from the BanBox";
            if (reviver != null) {
                message += " by " + reviver.getName();
            }
            message += "!";
            Bukkit.broadcastMessage(message);
        }
        
        plugin.getPluginLogger().info("Player " + player.getName() + " was revived from BanBox" + 
            (reviver != null ? " by " + reviver.getName() : ""));
    }
    
    /**
     * Handle head destruction (immediate release)
     */
    public void handleHeadDestruction(String playerName) {
        UUID playerUUID = getUUIDFromName(playerName);
        if (playerUUID == null) return;
        
        BanBoxData banBoxData = banBoxPlayers.remove(playerUUID);
        if (banBoxData == null) return;
        
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            // Release player immediately
            player.setGameMode(GameMode.SURVIVAL);
            
            // Teleport to spawn or safe location
            Location spawnLocation = getDefaultSpawnLocation();
            player.teleport(spawnLocation);
            
            // Send release message
            String releasedMessage = plugin.getConfigManager().getMainConfig()
                .getString("banbox.messages.head_destroyed", "&a&lYour head was destroyed - you are free!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', releasedMessage));
            
            player.sendMessage(ChatColor.GREEN + "🎉 Your head was destroyed!");
            player.sendMessage(ChatColor.GREEN + "You have been released from the BanBox!");
            
            // Play release effects
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        
        saveData();
        
        // Broadcast if enabled
        if (plugin.getConfigManager().getMainConfig().getBoolean("banbox.destruction_broadcast", true)) {
            Bukkit.broadcastMessage(ChatColor.GREEN + "💥 " + playerName + 
                "'s head was destroyed - they have been released from the BanBox!");
        }
        
        plugin.getPluginLogger().info("Player " + playerName + " released from BanBox due to head destruction");
    }
    
    /**
     * Process BanBox timers (auto-release)
     */
    public void processBanBoxTimers() {
        if (!enabled) return;
        
        long currentTime = System.currentTimeMillis();
        List<UUID> toRelease = new ArrayList<>();
        
        for (Map.Entry<UUID, BanBoxData> entry : banBoxPlayers.entrySet()) {
            BanBoxData data = entry.getValue();
            
            // Check if timer has expired
            long banTime = data.getBanTime();
            long timerMs = data.getTimerDays() * 24L * 60L * 60L * 1000L; // Days to milliseconds
            
            if ((currentTime - banTime) > timerMs) {
                toRelease.add(entry.getKey());
            }
        }
        
        // Release expired players
        for (UUID uuid : toRelease) {
            BanBoxData data = banBoxPlayers.remove(uuid);
            
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Release player
                player.setGameMode(GameMode.SURVIVAL);
                player.teleport(getDefaultSpawnLocation());
                
                player.sendMessage(ChatColor.GREEN + "⏰ Your BanBox timer has expired!");
                player.sendMessage(ChatColor.GREEN + "You have been automatically released!");
            }
            
            plugin.getPluginLogger().info("Auto-released player from BanBox: " + data.getPlayerName());
        }
        
        if (!toRelease.isEmpty()) {
            saveData();
        }
    }
    
    /**
     * Check if player is banboxed
     */
    public boolean isBanBoxed(UUID playerUUID) {
        return banBoxPlayers.containsKey(playerUUID);
    }
    
    /**
     * Check if player is banboxed
     */
    public boolean isBanBoxed(String playerName) {
        UUID uuid = getUUIDFromName(playerName);
        return uuid != null && isBanBoxed(uuid);
    }
    
    /**
     * Get BanBox data for player
     */
    public BanBoxData getBanBoxData(UUID playerUUID) {
        return banBoxPlayers.get(playerUUID);
    }
    
    /**
     * Manually release a player (admin command)
     */
    public boolean releasePlayer(String playerName) {
        UUID uuid = getUUIDFromName(playerName);
        if (uuid == null) return false;
        
        BanBoxData data = banBoxPlayers.remove(uuid);
        if (data == null) return false;
        
        // If player is online and in spectator mode, revive them
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline() && player.getGameMode() == GameMode.SPECTATOR) {
            Location spawnLocation = getDefaultSpawnLocation();
            performRevival(player, spawnLocation, null);
        }
        
        saveData();
        plugin.getPluginLogger().info("Manually released player from BanBox: " + playerName);
        return true;
    }
    
    /**
     * Get default spawn location from config
     */
    private Location getDefaultSpawnLocation() {
        String locationString = plugin.getConfigManager().getMainConfig()
            .getString("banbox.default_location", "world:0:100:0");
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
     * Get UUID from player name
     */
    private UUID getUUIDFromName(String playerName) {
        // First check online players
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        
        // Check banboxed players cache
        for (BanBoxData data : banBoxPlayers.values()) {
            if (data.getPlayerName().equalsIgnoreCase(playerName)) {
                return data.getPlayerUUID();
            }
        }
        
        // Fallback to Bukkit's offline player
        try {
            return Bukkit.getOfflinePlayer(playerName).getUniqueId();
        } catch (Exception e) {
            return null;
        }
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
     * Load BanBox data from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getPluginLogger().severe("Failed to create BanBox data file: " + e.getMessage());
                return;
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("banbox_players")) {
            for (String uuidString : dataConfig.getConfigurationSection("banbox_players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    String path = "banbox_players." + uuidString;
                    
                    String playerName = dataConfig.getString(path + ".name");
                    long banTime = dataConfig.getLong(path + ".ban_time");
                    int timerDays = dataConfig.getInt(path + ".timer_days");
                    
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
                    
                    // Load killer UUID
                    UUID killerUUID = null;
                    String killerString = dataConfig.getString(path + ".killer");
                    if (killerString != null && !killerString.isEmpty()) {
                        try {
                            killerUUID = UUID.fromString(killerString);
                        } catch (IllegalArgumentException e) {
                            // Invalid killer UUID
                        }
                    }
                    
                    BanBoxData data = new BanBoxData(uuid, playerName, deathLocation, banTime, timerDays, killerUUID);
                    banBoxPlayers.put(uuid, data);
                    
                } catch (Exception e) {
                    plugin.getPluginLogger().warning("Failed to load BanBox data for " + uuidString + ": " + e.getMessage());
                }
            }
        }
        
        plugin.getPluginLogger().info("Loaded " + banBoxPlayers.size() + " BanBox entries");
    }
    
    /**
     * Save BanBox data to file
     */
    public void saveData() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        
        // Clear existing data
        dataConfig.set("banbox_players", null);
        
        // Save current data
        for (Map.Entry<UUID, BanBoxData> entry : banBoxPlayers.entrySet()) {
            String uuidString = entry.getKey().toString();
            BanBoxData data = entry.getValue();
            String path = "banbox_players." + uuidString;
            
            dataConfig.set(path + ".name", data.getPlayerName());
            dataConfig.set(path + ".ban_time", data.getBanTime());
            dataConfig.set(path + ".timer_days", data.getTimerDays());
            
            if (data.getKillerUUID() != null) {
                dataConfig.set(path + ".killer", data.getKillerUUID().toString());
            }
            
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
            plugin.getPluginLogger().severe("Failed to save BanBox data: " + e.getMessage());
        }
    }
    
    // Getters and utility methods
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getBanBoxPlayerCount() {
        return banBoxPlayers.size();
    }
    
    public Set<UUID> getBanBoxPlayers() {
        return new HashSet<>(banBoxPlayers.keySet());
    }
    
    public void reload() {
        plugin.getPluginLogger().info("Reloading BanBoxManager...");
        saveData();
        loadData();
    }
    
    public void cleanup() {
        saveData();
        banBoxPlayers.clear();
    }
    
    /**
     * BanBox data class
     */
    public static class BanBoxData {
        private final UUID playerUUID;
        private final String playerName;
        private final Location deathLocation;
        private final long banTime;
        private final int timerDays;
        private final UUID killerUUID;
        
        public BanBoxData(UUID playerUUID, String playerName, Location deathLocation, 
                         long banTime, int timerDays, UUID killerUUID) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.deathLocation = deathLocation;
            this.banTime = banTime;
            this.timerDays = timerDays;
            this.killerUUID = killerUUID;
        }
        
        // Getters
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public Location getDeathLocation() { return deathLocation; }
        public long getBanTime() { return banTime; }
        public int getTimerDays() { return timerDays; }
        public UUID getKillerUUID() { return killerUUID; }
        
        public long getTimeInBanBox() {
            return System.currentTimeMillis() - banTime;
        }
        
        public boolean isExpired() {
            long timerMs = timerDays * 24L * 60L * 60L * 1000L;
            return getTimeInBanBox() > timerMs;
        }
    }
}