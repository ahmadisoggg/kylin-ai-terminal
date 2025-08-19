# XSteal

**PSD1 Inspired Minecraft Plugin for Paper/Spigot 1.8-1.21.4**

A premium Minecraft plugin that brings the exciting head-stealing mechanics from PSD1's HeadSteal video to your server. Acquire unique mob heads through charged creeper kills and gain powerful abilities!

---

## 🎯 **Core Features**

### ⚡ **Charged Creeper Head System**
- **Survival-Friendly Acquisition**: Get mob heads only when charged creepers kill mobs
- **Lightning Charging**: Creepers become charged when struck by lightning
- **Trident Channeling**: Use trident channeling enchantment to charge creepers
- **Custom Textures**: Uses HeadDatabase for high-quality mob head textures

### 🪄 **58+ Unique Mob Abilities**
Every Minecraft mob gets a unique head with special abilities:

**Hostile Mobs:**
- 🧟 **Zombie Head** → Summons 3 allied zombies to fight for you
- 💀 **Skeleton Head** → Fires infinite bone arrows with perfect accuracy  
- 💥 **Creeper Head** → Controlled explosion without self-damage
- 🕷️ **Spider Head** → Wall climbing and web shooting abilities
- 🔥 **Blaze Head** → Fire immunity and fireball shooting
- 🌟 **Enderman Head** → Teleport where you look
- 🧙 **Witch Head** → Random beneficial/harmful potion throwing

**Boss Heads (3 Abilities Each):**
- 🐲 **Ender Dragon Head**:
  - Left-Click: Dragon Fireball
  - Shift+Left-Click: Summon Ender Crystals  
  - Double Left-Click: Dragon Wings Flight
- 💀 **Wither Head**:
  - Left-Click: Wither Skull Barrage
  - Shift+Left-Click: Shield Aura
  - Double Left-Click: Wither Storm
- 🖤 **Warden Head**:
  - Left-Click: Sonic Boom Attack
  - Shift+Left-Click: Blindness Pulse
  - Double Left-Click: Vibration Detection

**Utility Mobs:**
- 🐄 **Cow Head** → Infinite milk and healing aura
- 🐷 **Pig Head** → Super speed and carrot detection
- 🐔 **Chicken Head** → Slow falling and egg throwing
- 🐴 **Horse Head** → Super speed and jump boost
- 🐺 **Wolf Head** → Summon wolf pack
- 🐱 **Cat Head** → Stealth and creeper repelling

### 🏺 **BanBox System**
Revolutionary death mechanic inspired by PSD1:
- **Death → Spectator Mode**: Players enter spectator mode when they die
- **Head Drops**: Player's head drops as an item
- **Inventory Access**: Players can still access inventory while banboxed
- **Revival Mechanics**: Left-click player heads to revive them instantly
- **Timer System**: Auto-release after configurable days (default: 7)
- **Head Destruction**: Destroying the head releases the player immediately

---

## 🎮 **How to Play**

### 1. **Acquiring Heads**
```
1. Find a creeper
2. Strike it with lightning (natural or trident channeling)
3. Lead the charged creeper to mobs
4. When charged creeper kills a mob → mob head drops!
5. Collect the head
```

### 2. **Using Abilities**
```
1. Equip mob head in helmet slot
2. Left-click to activate ability
3. Boss heads: Use combos for different abilities
   - Left-Click = Ability 1
   - Shift+Left-Click = Ability 2  
   - Double Left-Click = Ability 3
```

### 3. **BanBox System**
```
When you die:
1. You enter spectator mode at death location
2. Your head drops as an item
3. You can still access inventory and prepare
4. Another player must left-click your head to revive you
5. If head is destroyed, you're released immediately
```

---

## 📋 **Commands**

| Command | Permission | Description |
|---------|------------|-------------|
| `/xsteal help` | `xsteal.use` | Show help and usage information |
| `/xsteal give <player> <mob> [amount]` | `xsteal.admin.give` | Give mob head to player |
| `/xsteal listheads [category]` | `xsteal.use` | List all available mob heads |
| `/xsteal revive <player>` | `xsteal.admin.revive` | Revive player from BanBox |
| `/xsteal setbanbox` | `xsteal.admin.banbox` | Set BanBox location |
| `/xsteal removebanbox` | `xsteal.admin.banbox` | Remove BanBox |
| `/xsteal reload` | `xsteal.admin.reload` | Reload configuration |
| `/xsteal debug` | `xsteal.admin.debug` | Show debug information |
| `/xsteal info` | `xsteal.use` | Show plugin information |

---

## 🔧 **Installation**

### **Requirements**
- Paper or Spigot server (1.8 - 1.21.4)
- Java 8 or higher
- HeadDatabase plugin (optional - auto-downloaded via Libby)

### **Setup Steps**
1. **Download** `XSteal-1.0.0-obfuscated.jar` from releases
2. **Place** in your server's `plugins/` folder
3. **Start** the server (dependencies will be auto-downloaded)
4. **Configure** `plugins/XSteal/config.yml` as needed
5. **Update HeadDatabase IDs** in `heads.yml`:
   - Use `/hdb search <mobname>` to find IDs
   - Replace `HDB_*` placeholders with actual IDs
6. **Restart** server to apply changes

### **HeadDatabase ID Setup**
```bash
# In-game commands to find HeadDatabase IDs:
/hdb search zombie     # Find zombie head variants
/hdb search skeleton   # Find skeleton head variants
/hdb search dragon     # Find dragon head variants

# Replace in heads.yml:
# hdb_id: "HDB_ZOMBIE" → hdb_id: "12345"
```

---

## ⚙️ **Configuration**

### **Main Config** (`config.yml`)
```yaml
# Enable/disable core features
general:
  debug_mode: false
  head_drops:
    require_charged_creeper: true
    drop_chance: 100

# BanBox system settings
banbox:
  enabled: true
  default_timer_days: 7
  allow_inventory_access: true
  spectator_mode: true

# Ability system settings  
abilities:
  helmet_slot_activation: true
  boss_combos:
    enabled: true
    double_click_window: 500
```

### **Heads Config** (`heads.yml`)
Contains all 58+ mob head definitions with abilities. Key sections:
- `display_name`: How the head appears in inventory
- `hdb_id`: HeadDatabase ID for texture
- `ability`: The power granted by wearing the head
- `abilities`: Multiple abilities for boss heads

---

## 🎭 **Abilities Guide**

### **Regular Abilities**
Activated by **left-clicking** while wearing the head:

**Combat Abilities:**
- 🧟 **Zombie** → Summon 3 allied zombies
- 💀 **Skeleton** → Fire infinite bone arrows
- 💥 **Creeper** → Controlled explosion (no self-damage)
- 🕷️ **Spider** → Wall climbing + web shooting
- 🔥 **Blaze** → Fire immunity + fireball attacks

**Utility Abilities:**
- 🌟 **Enderman** → Teleport where you look
- 🐄 **Cow** → Infinite milk + healing aura
- 🐷 **Pig** → Super speed + carrot detection
- 🐔 **Chicken** → Slow falling + egg throwing
- 🐴 **Horse** → Super speed + jump boost

### **Boss Combo Abilities**
Boss heads have **3 abilities each** with combo activation:

**🐲 Ender Dragon Head:**
1. **Left-Click** → Dragon Fireball (explosive projectile)
2. **Shift+Left-Click** → Summon Ender Crystals (healing stations)
3. **Double Left-Click** → Dragon Wings Flight (enhanced flight)

**💀 Wither Head:**
1. **Left-Click** → Wither Skull Barrage (multiple skulls)
2. **Shift+Left-Click** → Shield Aura (damage resistance)
3. **Double Left-Click** → Wither Storm (area devastation)

**🖤 Warden Head:**
1. **Left-Click** → Sonic Boom (armor-piercing attack)
2. **Shift+Left-Click** → Blindness Pulse (area blindness)
3. **Double Left-Click** → Vibration Sense (reveal enemies)

---

## 🏺 **BanBox System**

### **How It Works**
1. **Player Dies** → Enters spectator mode at death location
2. **Head Drops** → Player's custom head drops as item
3. **Inventory Access** → Player can still manage inventory and prepare
4. **Revival** → Other players left-click the head to revive
5. **Timer** → Auto-release after 7 days (configurable)
6. **Destruction** → Destroying head releases player immediately

### **BanBox Features**
- ✅ **Spectator Mode**: Full spectator capabilities while banboxed
- ✅ **Inventory Access**: Prepare items for revival
- ✅ **Chat Access**: Communicate with other players
- ✅ **Cross-World Revival**: Revive in any world
- ✅ **Timer System**: Automatic release system
- ✅ **Broadcast Messages**: Server-wide death/revival announcements

---

## 🛡️ **Security Features**

### **Multi-Layered Obfuscation**
- **Control Flow Flattening**: Makes code flow harder to follow
- **String Encryption**: Encrypts all string constants
- **Anti-Debugging**: Detects and prevents debugging tools
- **Name Obfuscation**: Renames all classes, methods, and fields
- **Resource Protection**: Obfuscates configuration references

### **Anti-Tamper Protection**
- **JAR Integrity Checks**: Verifies file hasn't been modified
- **Runtime Verification**: Periodic integrity validation
- **Debug Detection**: Identifies debugging/profiling environments
- **Class Loading Validation**: Ensures proper execution environment

---

## 🔌 **Integrations**

### **HeadDatabase**
- **Automatic Integration**: Auto-downloads API via Libby
- **Texture Support**: High-quality custom mob head textures
- **Fallback Mode**: Works without HeadDatabase (basic textures)
- **ID Resolution**: Converts HDB IDs to actual head items

### **Supported Plugins**
- **Vault** → Economy integration (future feature)
- **PlaceholderAPI** → Placeholder support (future feature)
- **Anti-Cheat Plugins** → Compatibility with major anti-cheat systems

---

## 🚀 **Performance**

### **Optimizations**
- **Async Processing**: Non-critical tasks run asynchronously
- **Entity Limits**: Configurable limits on summoned entities
- **Particle Limits**: Configurable particle effects
- **Memory Management**: Automatic cleanup and caching
- **Thread Safety**: Concurrent data structures throughout

### **Recommended Settings**
```yaml
performance:
  max_summoned_entities_per_player: 10
  max_particles_per_ability: 100
  entity_cleanup_interval_seconds: 300
```

---

## 🐛 **Troubleshooting**

### **Common Issues**

**❌ Plugin won't load**
- Check Java version (8+ required)
- Verify server version (1.8-1.21.4)
- Check console for error messages

**❌ Heads not dropping**
- Ensure creeper is charged (glowing effect)
- Check world restrictions in config
- Verify `require_charged_creeper: true`

**❌ Abilities not working**
- Check player has helmet equipped
- Verify head is in helmet slot (not hand)
- Check permissions: `xsteal.ability.use`

**❌ HeadDatabase issues**
- Install HeadDatabase plugin manually if auto-download fails
- Check HDB IDs in heads.yml are correct (not HDB_* placeholders)
- Use `/hdb search <mobname>` to find correct IDs

### **Debug Mode**
Enable detailed logging:
```yaml
general:
  debug_mode: true
```

### **Getting Help**
- Use `/xsteal debug` for system information
- Check `plugins/XSteal/xsteal.log` for detailed logs
- Enable debug mode for verbose output

---

## 📖 **Developer Information**

### **Project Structure**
```
src/main/java/com/xreatlabs/xsteal/
├── XSteal.java                 # Main plugin class
├── abilities/                  # All ability implementations
│   ├── AbilityManager.java     # Ability execution system
│   └── [58+ ability classes]   # Individual ability implementations
├── banbox/                     # BanBox system
│   ├── BanBoxManager.java      # BanBox logic and data
│   └── BanBoxListener.java     # Death/revival events
├── commands/                   # Command system
│   └── XStealCommand.java      # All commands with tab completion
├── heads/                      # Head management
│   ├── HeadManager.java        # Head creation and management
│   └── HeadDropListener.java   # Charged creeper mechanics
└── utils/                      # Utility classes
    ├── LibbyManager.java       # Runtime dependency management
    ├── AntiTamper.java         # Security protection
    └── [other utilities]       # Logging, config, version support
```

### **Building from Source**
```bash
# Clone and build
git clone [repository]
cd xsteal
./build-xsteal.sh

# Output: build/libs/XSteal-1.0.0-obfuscated.jar
```

### **API Usage**
```java
// Get XSteal instance
XSteal plugin = XSteal.getInstance();

// Check if player is wearing ability head
boolean hasAbility = plugin.getAbilityManager().isWearingAbilityHead(player);

// Get head from entity type
ItemStack head = plugin.getHeadManager().getHeadForEntity(entity);

// Check if player is banboxed
boolean isBanboxed = plugin.getBanBoxManager().isBanBoxed(player.getUniqueId());
```

---

## 📄 **License**

XSteal is proprietary software developed by XreatLabs. All rights reserved.

### **Usage Terms**
- ✅ Licensed for single server use
- ❌ No redistribution allowed  
- ❌ Reverse engineering prohibited
- ✅ Commercial server use permitted

### **Anti-Tamper Notice**
This plugin includes advanced anti-tamper protection. Any attempts to:
- Decompile or reverse engineer the code
- Bypass licensing or security measures
- Distribute modified versions

Will result in permanent license revocation.

---

## 🎉 **Conclusion**

XSteal brings the exciting head-stealing mechanics from PSD1's video to your Minecraft server with:

✅ **58+ Unique Abilities** - Every mob gets a special power  
✅ **Survival-Friendly** - Acquire heads through gameplay, not commands  
✅ **Boss Combos** - Advanced combo system for boss heads  
✅ **BanBox Innovation** - Revolutionary death/revival mechanics  
✅ **Multi-Version Support** - Works on 1.8-1.21.4  
✅ **Professional Quality** - Enterprise-grade code with security protection  

**Transform your server's PvP and survival experience with XSteal!**

---

**XSteal v1.0.0** - PSD1 Inspired Minecraft Plugin  
*Compatible with Paper/Spigot 1.8-1.21.4*  
*Developed by XreatLabs*