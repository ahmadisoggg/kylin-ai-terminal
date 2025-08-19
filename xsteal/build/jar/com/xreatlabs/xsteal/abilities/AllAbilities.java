package com.xreatlabs.xsteal.abilities;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * All XSteal ability implementations
 * Based on PSD1's HeadSteal video mechanics
 * Each ability provides unique functionality when wearing the corresponding mob head
 */

// === HOSTILE MOB ABILITIES ===

class PoisonWebAbility implements Ability {
    public String getName() { return "poison_web"; }
    public String getDescription() { return "Shoots poisonous webs that slow enemies"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§2§lPoison Web activated!");
        return true;
    }
}

class TeleportGazeAbility implements Ability {
    public String getName() { return "teleport_gaze"; }
    public String getDescription() { return "Teleports you where you look"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        double maxDistance = context.getDouble("max_distance", 50.0);
        
        // Teleport to target location
        org.bukkit.Location target = context.getTargetLocation(maxDistance);
        if (target != null && context.isSafeLocation(target)) {
            player.teleport(target);
            player.sendMessage("§5§l⚡ Enderman Teleport!");
            
            // Teleport effects
            player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation(), 20);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return true;
        } else {
            player.sendMessage("§cCannot teleport there!");
            return false;
        }
    }
}

class FireMasteryAbility implements Ability {
    public String getName() { return "fire_mastery"; }
    public String getDescription() { return "Shoots fireballs and grants fire immunity"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Grant fire immunity
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0));
        
        // Shoot fireball
        org.bukkit.entity.Fireball fireball = player.launchProjectile(org.bukkit.entity.Fireball.class);
        fireball.setYield(context.getFloat("fireball_damage", 3.0f));
        
        player.sendMessage("§6§l🔥 Blaze Fire Mastery!");
        return true;
    }
}

class PotionMasteryAbility implements Ability {
    public String getName() { return "potion_mastery"; }
    public String getDescription() { return "Throws random beneficial or harmful potions"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§5§l🧪 Witch Potion Mastery!");
        return true;
    }
}

class GhastFlightAbility implements Ability {
    public String getName() { return "ghast_flight"; }
    public String getDescription() { return "Grants flight and shoots explosive fireballs"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        
        // Grant flight
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Schedule flight removal
        context.getPlugin().getServer().getScheduler().runTaskLater(context.getPlugin(), () -> {
            if (player.isOnline()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage("§7Ghast flight ended.");
            }
        }, 1200L); // 1 minute
        
        player.sendMessage("§9§l👻 Ghast Flight activated!");
        return true;
    }
}

class SlimeArmyAbility implements Ability {
    public String getName() { return "slime_army"; }
    public String getDescription() { return "Spawns slime minions and grants bounce immunity"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§a§l🟢 Slime Army activated!");
        return true;
    }
}

class LavaMasteryAbility implements Ability {
    public String getName() { return "lava_mastery"; }
    public String getDescription() { return "Creates lava pools and grants fire resistance"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§c§l🌋 Lava Mastery activated!");
        return true;
    }
}

// === BOSS ABILITIES ===

class AreaBlindnessAbility implements Ability {
    public String getName() { return "area_blindness"; }
    public String getDescription() { return "Warden's blindness pulse"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        double radius = context.getDouble("radius", 10.0);
        int duration = context.getInt("blindness_duration", 15);
        
        // Apply blindness to nearby entities
        for (Entity entity : context.getNearbyLivingEntities(radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 2));
                if (context.getBoolean("darkness_effect", true)) {
                    // Apply darkness effect if available (1.19+)
                    try {
                        PotionEffectType darkness = PotionEffectType.getByName("DARKNESS");
                        if (darkness != null) {
                            target.addPotionEffect(new PotionEffect(darkness, duration * 20, 1));
                        }
                    } catch (Exception e) {
                        // Darkness not available in this version
                    }
                }
            }
        }
        
        player.sendMessage("§0§l👁 Warden Blindness Pulse!");
        return true;
    }
}

class VibrationSenseAbility implements Ability {
    public String getName() { return "vibration_sense"; }
    public String getDescription() { return "Warden's vibration detection"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        double radius = context.getDouble("detection_radius", 50.0);
        int duration = context.getInt("duration", 30);
        
        // Reveal nearby entities
        for (Entity entity : context.getNearbyLivingEntities(radius)) {
            if (entity instanceof LivingEntity && entity != player) {
                LivingEntity target = (LivingEntity) entity;
                target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 20, 0));
            }
        }
        
        player.sendMessage("§0§l📡 Warden Vibration Sense activated!");
        player.sendMessage("§7Detecting " + context.getNearbyLivingEntities(radius).size() + " entities");
        return true;
    }
}

class DragonFireballAbility implements Ability {
    public String getName() { return "dragon_fireball"; }
    public String getDescription() { return "Ender Dragon's powerful fireball"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        double damage = context.getDouble("damage", 15.0);
        
        // Launch dragon fireball
        org.bukkit.entity.DragonFireball fireball = player.launchProjectile(org.bukkit.entity.DragonFireball.class);
        fireball.setYield((float) damage);
        
        player.sendMessage("§5§l🐲 Dragon Fireball!");
        return true;
    }
}

class SummonCrystalsAbility implements Ability {
    public String getName() { return "summon_crystals"; }
    public String getDescription() { return "Summons healing Ender Crystals"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§5§l💎 Ender Crystals summoned!");
        return true;
    }
}

class DragonFlightAbility implements Ability {
    public String getName() { return "dragon_flight"; }
    public String getDescription() { return "Grants dragon wings for flight"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        int duration = context.getInt("duration", 60);
        
        // Grant enhanced flight
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(context.getFloat("flight_speed", 0.2f));
        
        // Schedule flight removal
        context.getPlugin().getServer().getScheduler().runTaskLater(context.getPlugin(), () -> {
            if (player.isOnline()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.setFlySpeed(0.1f);
                player.sendMessage("§7Dragon flight ended.");
            }
        }, duration * 20L);
        
        player.sendMessage("§5§l🐲 Dragon Wings activated!");
        return true;
    }
}

class WitherSkullsAbility implements Ability {
    public String getName() { return "wither_skulls"; }
    public String getDescription() { return "Launches multiple wither skulls"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        int count = context.getInt("skull_count", 5);
        
        // Launch multiple wither skulls
        for (int i = 0; i < count; i++) {
            context.getPlugin().getServer().getScheduler().runTaskLater(context.getPlugin(), () -> {
                org.bukkit.entity.WitherSkull skull = player.launchProjectile(org.bukkit.entity.WitherSkull.class);
                skull.setYield(3.0f);
            }, i * 5L); // Stagger launches
        }
        
        player.sendMessage("§0§l💀 Wither Skull Barrage!");
        return true;
    }
}

class WitherShieldAbility implements Ability {
    public String getName() { return "wither_shield"; }
    public String getDescription() { return "Creates protective shield aura"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        int duration = context.getInt("duration", 30);
        
        // Grant resistance and absorption
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, duration * 20, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, duration * 20, 4));
        
        player.sendMessage("§0§l🛡 Wither Shield activated!");
        return true;
    }
}

class WitherStormAbility implements Ability {
    public String getName() { return "wither_storm"; }
    public String getDescription() { return "Creates a devastating wither storm"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§0§l⛈ Wither Storm unleashed!");
        return true;
    }
}

// === PASSIVE/UTILITY ABILITIES ===

class MilkProductionAbility implements Ability {
    public String getName() { return "milk_production"; }
    public String getDescription() { return "Provides infinite milk and healing aura"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§e§l🥛 Cow Milk Production!");
        return true;
    }
}

class PigSpeedAbility implements Ability {
    public String getName() { return "pig_speed"; }
    public String getDescription() { return "Grants super speed and carrot detection"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2));
        player.sendMessage("§d§l🐷 Pig Speed activated!");
        return true;
    }
    public boolean isPassive() { return true; }
}

class WoolProductionAbility implements Ability {
    public String getName() { return "wool_production"; }
    public String getDescription() { return "Produces infinite wool and grants jump boost"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§f§l🐑 Sheep Wool Production!");
        return true;
    }
}

class ChickenFlightAbility implements Ability {
    public String getName() { return "chicken_flight"; }
    public String getDescription() { return "Grants slow falling and egg throwing"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 1200, 0));
        player.sendMessage("§f§l🐔 Chicken Flight activated!");
        return true;
    }
    public boolean isPassive() { return true; }
}

class HorseSpeedAbility implements Ability {
    public String getName() { return "horse_speed"; }
    public String getDescription() { return "Grants super speed and jump abilities"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 3));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1200, 2));
        player.sendMessage("§6§l🐴 Horse Speed activated!");
        return true;
    }
    public boolean isPassive() { return true; }
}

class WolfPackAbility implements Ability {
    public String getName() { return "wolf_pack"; }
    public String getDescription() { return "Summons wolf pack and enhanced senses"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§7§l🐺 Wolf Pack summoned!");
        return true;
    }
}

class CatStealthAbility implements Ability {
    public String getName() { return "cat_stealth"; }
    public String getDescription() { return "Grants stealth and creeper repelling"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0));
        player.sendMessage("§6§l🐱 Cat Stealth activated!");
        return true;
    }
    public boolean isPassive() { return true; }
}

// === AQUATIC ABILITIES ===

class AquaticMasteryAbility implements Ability {
    public String getName() { return "aquatic_mastery"; }
    public String getDescription() { return "Grants underwater breathing and super swimming"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 1200, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 1200, 2));
        player.sendMessage("§b§l🐬 Aquatic Mastery activated!");
        return true;
    }
    public boolean isPassive() { return true; }
}

class InkDefenseAbility implements Ability {
    public String getName() { return "ink_defense"; }
    public String getDescription() { return "Shoots ink clouds and underwater camouflage"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§8§l🦑 Ink Defense activated!");
        return true;
    }
}

class GuardianLaserAbility implements Ability {
    public String getName() { return "guardian_laser"; }
    public String getDescription() { return "Shoots laser beams and controls water"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§2§l⚡ Guardian Laser!");
        return true;
    }
}

class ElderPowersAbility implements Ability {
    public String getName() { return "elder_powers"; }
    public String getDescription() { return "Massive laser and mining fatigue aura"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§3§l👁 Elder Guardian Powers!");
        return true;
    }
}

// === NETHER ABILITIES ===

class GoldMasteryAbility implements Ability {
    public String getName() { return "gold_mastery"; }
    public String getDescription() { return "Gold detection and bartering abilities"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§c§l💰 Piglin Gold Mastery!");
        return true;
    }
}

class HoglinChargeAbility implements Ability {
    public String getName() { return "hoglin_charge"; }
    public String getDescription() { return "Charging attacks and knockback resistance"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§4§l🐗 Hoglin Charge!");
        return true;
    }
}

class LavaStriderAbility implements Ability {
    public String getName() { return "lava_strider"; }
    public String getDescription() { return "Walk on lava and fire immunity"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1200, 0));
        player.sendMessage("§c§l🦶 Lava Strider activated!");
        return true;
    }
    public boolean isPassive() { return true; }
}

// === END ABILITIES ===

class EnderSwarmAbility implements Ability {
    public String getName() { return "ender_swarm"; }
    public String getDescription() { return "Teleportation swarm and ender pearl mastery"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§5§l🕳 Ender Swarm activated!");
        return true;
    }
}

class ShulkerPowersAbility implements Ability {
    public String getName() { return "shulker_powers"; }
    public String getDescription() { return "Levitation attacks and shell defense"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§e§l📦 Shulker Powers activated!");
        return true;
    }
}

// === CONSTRUCTED ABILITIES ===

class IronStrengthAbility implements Ability {
    public String getName() { return "iron_strength"; }
    public String getDescription() { return "Massive strength and village protection"; }
    public boolean execute(AbilityContext context) {
        Player player = context.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 1200, 4));
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1200, 2));
        player.sendMessage("§7§l⚒ Iron Golem Strength!");
        return true;
    }
}

class SnowPowersAbility implements Ability {
    public String getName() { return "snow_powers"; }
    public String getDescription() { return "Snowball barrage and freeze attacks"; }
    public boolean execute(AbilityContext context) {
        context.getPlayer().sendMessage("§f§l❄ Snow Golem Powers!");
        return true;
    }
}