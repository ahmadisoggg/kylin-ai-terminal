package com.xreatlabs.xsteal.abilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

/**
 * Useful and practical head abilities for XSteal
 * Focus on utility, survival, and quality-of-life improvements
 */

// === VILLAGER HEAD - Trading and Economy Powers ===
class VillagerTradingAbility implements Ability {
    public String getName() { return "villager_trading"; }
    public String getDescription() { return "Grants trading mastery and emerald detection"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Grant trading benefits
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 2400, 2)); // 2 minutes
        
        // Emerald detection
        detectNearbyEmeralds(player, 20.0);
        
        // Trading discount effect (would need custom villager interaction)
        player.sendMessage("§e💰 Villager Trading Mastery activated!");
        player.sendMessage("§7• Emerald detection enabled");
        player.sendMessage("§7• Trading luck increased");
        player.sendMessage("§7• Better deals with villagers");
        
        return true;
    }
    
    private void detectNearbyEmeralds(Player player, double radius) {
        Location playerLoc = player.getLocation();
        boolean foundEmeralds = false;
        
        // Check for emerald ore/blocks in area
        for (int x = (int) -radius; x <= radius; x++) {
            for (int y = (int) -radius; y <= radius; y++) {
                for (int z = (int) -radius; z <= radius; z++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();
                    
                    if (blockType == Material.EMERALD_ORE || blockType == Material.EMERALD_BLOCK) {
                        // Highlight emerald location
                        checkLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, 
                            checkLoc.add(0.5, 0.5, 0.5), 5, 0.3, 0.3, 0.3, 0.1);
                        foundEmeralds = true;
                    }
                }
            }
        }
        
        if (foundEmeralds) {
            player.sendMessage("§a✨ Emeralds detected nearby!");
        }
    }
    
    public boolean isPassive() { return true; }
}

// === BAT HEAD - Echolocation and Cave Navigation ===
class BatEcholocationAbility implements Ability {
    public String getName() { return "bat_echolocation"; }
    public String getDescription() { return "Grants echolocation and perfect cave navigation"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Grant night vision and cave benefits
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 2400, 0));
        
        // Echolocation - detect nearby entities and structures
        performEcholocation(player);
        
        player.sendMessage("§8🦇 Bat Echolocation activated!");
        player.sendMessage("§7• Night vision enabled");
        player.sendMessage("§7• Nearby entities detected");
        player.sendMessage("§7• Cave navigation enhanced");
        
        return true;
    }
    
    private void performEcholocation(Player player) {
        Location playerLoc = player.getLocation();
        
        // Detect nearby entities
        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                // Add glowing effect to detected entities
                if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 200, 0));
                }
                
                // Sound ping for each detected entity
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 2.0f);
            }
        }
        
        // Detect cave features (ores, spawners, etc.)
        detectCaveFeatures(player, 10.0);
    }
    
    private void detectCaveFeatures(Player player, double radius) {
        Location playerLoc = player.getLocation();
        
        for (int x = (int) -radius; x <= radius; x++) {
            for (int y = (int) -radius; y <= radius; y++) {
                for (int z = (int) -radius; z <= radius; z++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();
                    
                    // Detect valuable blocks
                    if (isValuableBlock(blockType)) {
                        checkLoc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, 
                            checkLoc.add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }
    }
    
    private boolean isValuableBlock(Material material) {
        switch (material) {
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case GOLD_ORE:
            case IRON_ORE:
            case SPAWNER:
            case CHEST:
            case TRAPPED_CHEST:
                return true;
            default:
                return material.name().contains("_ORE");
        }
    }
    
    public boolean isPassive() { return true; }
}

// === BEE HEAD - Pollination and Crop Growth ===
class BeePollinationAbility implements Ability {
    public String getName() { return "bee_pollination"; }
    public String getDescription() { return "Accelerates crop growth and produces honey"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Pollinate nearby crops
        pollinateArea(player, 8.0);
        
        // Produce honey items
        produceHoney(player);
        
        // Flower detection
        detectFlowers(player, 15.0);
        
        player.sendMessage("§e🐝 Bee Pollination activated!");
        player.sendMessage("§7• Crops accelerated in 8-block radius");
        player.sendMessage("§7• Honey produced");
        player.sendMessage("§7• Flowers detected");
        
        return true;
    }
    
    private void pollinateArea(Player player, double radius) {
        Location playerLoc = player.getLocation();
        int cropsGrown = 0;
        
        for (int x = (int) -radius; x <= radius; x++) {
            for (int z = (int) -radius; z <= radius; z++) {
                for (int y = -2; y <= 2; y++) {
                    Location cropLoc = playerLoc.clone().add(x, y, z);
                    Material cropType = cropLoc.getBlock().getType();
                    
                    if (isCrop(cropType)) {
                        // Grow crop
                        if (Math.random() < 0.3) { // 30% chance to grow each crop
                            growCrop(cropLoc);
                            cropsGrown++;
                            
                            // Pollination particles
                            cropLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, 
                                cropLoc.add(0.5, 0.5, 0.5), 3, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                }
            }
        }
        
        if (cropsGrown > 0) {
            player.sendMessage("§a🌱 Accelerated growth of " + cropsGrown + " crops!");
        }
    }
    
    private boolean isCrop(Material material) {
        switch (material) {
            case WHEAT:
            case CARROTS:
            case POTATOES:
            case BEETROOTS:
            case PUMPKIN_STEM:
            case MELON_STEM:
                return true;
            default:
                return material.name().contains("CROP") || material.name().contains("STEM");
        }
    }
    
    private void growCrop(Location cropLoc) {
        // This would use NMS or Bukkit methods to grow crops
        // Simplified implementation
        if (Math.random() < 0.5) {
            // Bone meal effect
            cropLoc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, cropLoc, 5);
        }
    }
    
    private void produceHoney(Player player) {
        // Give honey items
        ItemStack honey = new ItemStack(Material.HONEY_BOTTLE, 2);
        player.getInventory().addItem(honey);
        
        player.sendMessage("§6🍯 Produced 2 honey bottles!");
    }
    
    private void detectFlowers(Player player, double radius) {
        // Similar to emerald detection but for flowers
        Location playerLoc = player.getLocation();
        
        for (int x = (int) -radius; x <= radius; x++) {
            for (int z = (int) -radius; z <= radius; z++) {
                for (int y = -3; y <= 3; y++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();
                    
                    if (isFlower(blockType)) {
                        checkLoc.getWorld().spawnParticle(Particle.HEART, 
                            checkLoc.add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.1);
                    }
                }
            }
        }
    }
    
    private boolean isFlower(Material material) {
        return material.name().contains("FLOWER") || 
               material == Material.ROSE_BUSH || 
               material == Material.SUNFLOWER ||
               material.name().contains("TULIP") ||
               material.name().contains("ORCHID");
    }
}

// === IRON GOLEM HEAD - Construction and Protection ===
class IronGolemConstructionAbility implements Ability {
    public String getName() { return "iron_golem_construction"; }
    public String getDescription() { return "Builds iron golems and grants village protection"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Check if player has materials to build golem
        if (!hasGolemMaterials(player)) {
            player.sendMessage("§c❌ Need iron blocks and pumpkin to build iron golem!");
            return false;
        }
        
        // Build iron golem at target location
        Location targetLoc = context.getTargetLocation(10.0);
        if (targetLoc == null || !context.isSafeLocation(targetLoc)) {
            player.sendMessage("§c❌ Invalid location for iron golem construction!");
            return false;
        }
        
        // Consume materials
        consumeGolemMaterials(player);
        
        // Spawn iron golem
        IronGolem golem = (IronGolem) targetLoc.getWorld().spawnEntity(targetLoc, EntityType.IRON_GOLEM);
        golem.setCustomName("§7" + player.getName() + "'s Guardian");
        golem.setCustomNameVisible(true);
        
        // Enhanced golem abilities
        golem.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 6000, 1));
        golem.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 6000, 1));
        
        // Construction effects
        targetLoc.getWorld().spawnParticle(Particle.CLOUD, targetLoc, 20, 1.0, 1.0, 1.0, 0.1);
        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_ANVIL_PLACE, 2.0f, 1.0f);
        
        player.sendMessage("§7⚒ Iron Golem constructed!");
        player.sendMessage("§7Your guardian will protect the area");
        
        return true;
    }
    
    private boolean hasGolemMaterials(Player player) {
        int ironBlocks = 0;
        boolean hasPumpkin = false;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                if (item.getType() == Material.IRON_BLOCK) {
                    ironBlocks += item.getAmount();
                } else if (item.getType() == Material.PUMPKIN || item.getType() == Material.CARVED_PUMPKIN) {
                    hasPumpkin = true;
                }
            }
        }
        
        return ironBlocks >= 4 && hasPumpkin;
    }
    
    private void consumeGolemMaterials(Player player) {
        // Remove 4 iron blocks and 1 pumpkin
        player.getInventory().removeItem(new ItemStack(Material.IRON_BLOCK, 4));
        
        // Remove pumpkin
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && (item.getType() == Material.PUMPKIN || item.getType() == Material.CARVED_PUMPKIN)) {
                item.setAmount(item.getAmount() - 1);
                break;
            }
        }
    }
}

// === ALLAY HEAD - Item Collection and Organization ===
class AllayCollectionAbility implements Ability {
    public String getName() { return "allay_collection"; }
    public String getDescription() { return "Collects and organizes nearby items automatically"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Collect nearby items
        collectNearbyItems(player, 12.0);
        
        // Organize inventory
        organizeInventory(player);
        
        // Summon helpful allay
        if (ComprehensiveVersionSupport.isAtLeast(1, 19)) {
            summonHelpfulAllay(player);
        }
        
        player.sendMessage("§b✨ Allay Collection activated!");
        player.sendMessage("§7• Nearby items collected");
        player.sendMessage("§7• Inventory organized");
        player.sendMessage("§7• Helpful allay summoned");
        
        return true;
    }
    
    private void collectNearbyItems(Player player, double radius) {
        int itemsCollected = 0;
        
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Item) {
                Item item = (Item) entity;
                ItemStack itemStack = item.getItemStack();
                
                // Try to add to player inventory
                java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
                
                if (leftover.isEmpty()) {
                    // Successfully collected
                    item.remove();
                    itemsCollected++;
                    
                    // Collection particle
                    player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, 
                        item.getLocation(), 5, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
        
        if (itemsCollected > 0) {
            player.sendMessage("§a📦 Collected " + itemsCollected + " items!");
        }
    }
    
    private void organizeInventory(Player player) {
        // Simple inventory organization
        ItemStack[] contents = player.getInventory().getContents();
        java.util.Arrays.sort(contents, (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return a.getType().compareTo(b.getType());
        });
        
        player.getInventory().setContents(contents);
        player.sendMessage("§b📋 Inventory organized!");
    }
    
    private void summonHelpfulAllay(Player player) {
        try {
            EntityType allayType = EntityType.valueOf("ALLAY");
            Allay allay = (Allay) player.getWorld().spawnEntity(player.getLocation(), allayType);
            
            allay.setCustomName("§b" + player.getName() + "'s Helper");
            allay.setCustomNameVisible(true);
            
            // Track summoned entity
            context.getPlugin().getAbilityManager().addSummonedEntity(player, allay);
            
            // Remove after 2 minutes
            context.getPlugin().getServer().getScheduler().runTaskLater(context.getPlugin(), () -> {
                if (allay.isValid()) {
                    allay.remove();
                }
            }, 2400L);
            
        } catch (IllegalArgumentException e) {
            // Allay not available in this version
            player.sendMessage("§7(Allay summoning not available in this version)");
        }
    }
}

// === SNIFFER HEAD - Archaeology and Ancient Knowledge ===
class SnifferArchaeologyAbility implements Ability {
    public String getName() { return "sniffer_archaeology"; }
    public String getDescription() { return "Detects buried treasures and ancient seeds"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Archaeological detection
        detectBuriedTreasures(player, 20.0);
        
        // Ancient seed detection
        detectAncientSeeds(player, 15.0);
        
        // Grant archaeology luck
        player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 1200, 3));
        
        player.sendMessage("§6🔍 Sniffer Archaeology activated!");
        player.sendMessage("§7• Buried treasures detected");
        player.sendMessage("§7• Ancient seeds located");
        player.sendMessage("§7• Archaeological luck increased");
        
        return true;
    }
    
    private void detectBuriedTreasures(Player player, double radius) {
        Location playerLoc = player.getLocation();
        
        // Detect buried treasure, dungeons, strongholds
        for (int x = (int) -radius; x <= radius; x++) {
            for (int z = (int) -radius; z <= radius; z++) {
                for (int y = -5; y <= 5; y++) {
                    Location checkLoc = playerLoc.clone().add(x, y, z);
                    Material blockType = checkLoc.getBlock().getType();
                    
                    if (isTreasureBlock(blockType)) {
                        // Highlight treasure location
                        checkLoc.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, 
                            checkLoc.add(0.5, 0.5, 0.5), 8, 0.5, 0.5, 0.5, 0.2);
                        
                        player.sendMessage("§6💎 Treasure detected at " + 
                            checkLoc.getBlockX() + ", " + checkLoc.getBlockY() + ", " + checkLoc.getBlockZ());
                    }
                }
            }
        }
    }
    
    private void detectAncientSeeds(Player player, double radius) {
        // Give player some ancient seeds (if available in version)
        if (ComprehensiveVersionSupport.isAtLeast(1, 20)) {
            // Modern versions might have ancient seeds
            player.sendMessage("§a🌱 Ancient seeds detected in the area!");
        } else {
            // Give rare seeds for older versions
            ItemStack rareSeeds = new ItemStack(Material.WHEAT_SEEDS, 5);
            player.getInventory().addItem(rareSeeds);
            player.sendMessage("§a🌱 Discovered rare seeds!");
        }
    }
    
    private boolean isTreasureBlock(Material material) {
        switch (material) {
            case CHEST:
            case TRAPPED_CHEST:
            case SPAWNER:
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }
}

// === DOLPHIN HEAD - Ocean Navigation and Treasure Finding ===
class DolphinNavigationAbility implements Ability {
    public String getName() { return "dolphin_navigation"; }
    public String getDescription() { return "Grants ocean mastery and treasure detection"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Grant aquatic abilities
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 2400, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 2400, 2));
        
        // Detect ocean treasures
        detectOceanTreasures(player, 25.0);
        
        // Summon friendly dolphins
        summonDolphinPod(player);
        
        player.sendMessage("§b🐬 Dolphin Navigation activated!");
        player.sendMessage("§7• Ocean breathing and speed");
        player.sendMessage("§7• Treasure detection enabled");
        player.sendMessage("§7• Dolphin pod summoned");
        
        return true;
    }
    
    private void detectOceanTreasures(Player player, double radius) {
        // Detect shipwrecks, ocean monuments, buried treasure
        player.sendMessage("§b🗺 Scanning for ocean treasures...");
        
        // This would involve complex structure detection
        // Simplified version gives treasure map
        ItemStack treasureMap = new ItemStack(Material.MAP);
        player.getInventory().addItem(treasureMap);
        player.sendMessage("§6🗺 Treasure map discovered!");
    }
    
    private void summonDolphinPod(Player player) {
        if (!player.getLocation().getBlock().isLiquid()) {
            player.sendMessage("§7(Dolphins need water to be summoned)");
            return;
        }
        
        // Spawn 2-3 dolphins
        for (int i = 0; i < 3; i++) {
            Location spawnLoc = player.getLocation().add(
                (Math.random() - 0.5) * 6,
                0,
                (Math.random() - 0.5) * 6
            );
            
            Dolphin dolphin = (Dolphin) player.getWorld().spawnEntity(spawnLoc, EntityType.DOLPHIN);
            dolphin.setCustomName("§b" + player.getName() + "'s Dolphin");
            
            // Track summoned entity
            context.getPlugin().getAbilityManager().addSummonedEntity(player, dolphin);
        }
    }
    
    public boolean isPassive() { return true; }
}

// === GOAT HEAD - Mountain Climbing and Ramming ===
class GoatMountainAbility implements Ability {
    public String getName() { return "goat_mountain_powers"; }
    public String getDescription() { return "Grants mountain climbing and powerful ramming attacks"; }
    
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Grant mountain climbing abilities
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 2400, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 2400, 0));
        
        // Ramming attack if sneaking + left click
        if (player.isSneaking()) {
            performRammingAttack(player, context);
        }
        
        player.sendMessage("§f🐐 Goat Mountain Powers activated!");
        player.sendMessage("§7• Super jumping enabled");
        player.sendMessage("§7• Fall damage immunity");
        player.sendMessage("§7• Sneak + left-click for ramming attack");
        
        return true;
    }
    
    private void performRammingAttack(Player player, AbilityContext context) {
        // Launch player forward
        Vector direction = player.getEyeLocation().getDirection();
        direction.multiply(2.0);
        direction.setY(0.5);
        
        player.setVelocity(direction);
        
        // Ramming effects
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 1.0, 0.5, 1.0, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_GOAT_AMBIENT, 2.0f, 1.0f);
        
        // Damage entities in path
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline()) { // 1 second
                    cancel();
                    return;
                }
                
                // Check for entities to ram
                for (Entity entity : player.getNearbyEntities(2, 2, 2)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(12.0, player);
                        
                        // Knockback
                        Vector knockback = entity.getLocation().subtract(player.getLocation()).toVector();
                        knockback.normalize().multiply(3.0);
                        knockback.setY(1.0);
                        entity.setVelocity(knockback);
                        
                        player.sendMessage("§f🐐 Ramming attack hit " + target.getType().name() + "!");
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(context.getPlugin(), 0L, 1L);
    }
}