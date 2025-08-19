# HeadStealX

A premium Minecraft plugin for Paper/Spigot servers that adds 58 unique mob heads with special abilities, charged creeper head drops, and a unique BanBox revival system.

## Features

### 🎯 Core Features
- **58 Unique Mob Heads** - Each with distinct abilities activated by left-click
- **Charged Creeper Drops** - Heads drop when charged creepers kill mobs (survival-friendly)
- **BanBox System** - Players enter spectator mode on death, can be revived by head interaction
- **Boss Abilities** - Dragon, Wither, and Warden heads have 3 combo abilities each
- **No Cooldowns** - Unlimited ability usage (configurable)

### ⚡ Abilities System
- **Combat Abilities** - Lifesteal, arrow volleys, explosions, and more
- **Utility Abilities** - Teleportation, speed boosts, temporary mounts
- **Boss Combos** - Left-click, Shift+Left-click, Right-click combinations
- **Passive Effects** - Some abilities activate when worn as helmet

### 🔧 Technical Features
- **Multi-Version Support** - Compatible with Minecraft 1.8 - 1.21.8
- **Runtime Dependencies** - Uses Libby to download HeadDatabase API automatically
- **Performance Optimized** - Async processing, configurable limits
- **Anti-Tamper Protection** - Obfuscated with integrity checks

## Installation

### Requirements
- Paper or Spigot server (1.8 - 1.21.8)
- Java 8 or higher
- HeadDatabase plugin (optional, downloaded automatically)

### Setup
1. Download `HeadStealX.jar` from releases
2. Place in your server's `plugins/` folder
3. Start the server (dependencies will be downloaded automatically)
4. Configure `plugins/HeadStealX/config.yml` as needed
5. Replace HDB_* placeholders in `heads.yml` with actual HeadDatabase IDs

## Configuration

### Main Config (`config.yml`)
```yaml
general:
  head_drop:
    require_charged_creeper: true
    drop_chance_percent: 100
    
banbox:
  enabled: true
  auto_unban_days: 7
  
abilities:
  unlimited_use: true
  use_cooldowns: false
```

### Heads Config (`heads.yml`)
Contains all 58 mob head definitions with abilities. Replace `HDB_*` placeholders with actual HeadDatabase IDs:

```yaml
heads:
  zombie:
    displayName: "&cZombie Head"
    hdb_id: "12345"  # Replace with actual HDB ID
    ability:
      type: "lifesteal"
      params:
        healPercent: 0.25
```

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/xsteal help` | `headsteal.use` | Show help information |
| `/xsteal give <player> <head>` | `headsteal.admin.give` | Give head to player |
| `/xsteal listheads [category]` | `headsteal.use` | List available heads |
| `/xsteal revive <player>` | `headsteal.admin.revive` | Revive banboxed player |
| `/xsteal reload` | `headsteal.admin.reload` | Reload configuration |
| `/xsteal debug` | `headsteal.admin.debug` | Show debug information |

## Permissions

### Basic Permissions
- `headsteal.use` - Basic plugin usage
- `headsteal.ability.use` - Use head abilities
- `headsteal.ability.boss` - Use boss head abilities
- `headsteal.drop` - Drop heads on death
- `headsteal.revive` - Revive other players

### Admin Permissions
- `headsteal.admin.*` - All admin permissions
- `headsteal.admin.give` - Give heads to players
- `headsteal.admin.revive` - Revive banboxed players
- `headsteal.admin.bypass` - Bypass restrictions

## Head Abilities

### Hostile Mobs
- **Zombie** - Lifesteal Strike: Heal for 25% of damage dealt
- **Skeleton** - Arrow Volley: Fire 5 arrows in spread
- **Creeper** - Charged Blast: Non-lethal self-explosion
- **Spider** - Web Lunge: Lunge forward and trap target
- **Blaze** - Flame Vortex: Triple fireball attack
- *...and 20+ more hostile abilities*

### Boss Heads (Triple Abilities)
- **Ender Dragon**
  - Left-click: Wing Gust (knockback cone)
  - Shift+Left-click: Dragon Breath (lingering damage)
  - Right-click: Sky Strike (leap and stun)
  
- **Wither**
  - Left-click: Skull Barrage (multiple wither skulls)
  - Shift+Left-click: Decay Field (area wither effect)
  - Right-click: Shield Phase (temporary invulnerability)
  
- **Warden**
  - Left-click: Sonic Boom (piercing damage + blindness)
  - Shift+Left-click: Seismic Slam (ground tremor)
  - Right-click: Echo Locator (reveal invisible targets)

### Utility Abilities
- **Cow** - Milk Burst: Heal allies and clear effects
- **Horse** - Fast Mount: Spawn temporary fast horse
- **Enderman** - Teleport Step: Short-range teleportation
- **Chicken** - Feather Float: Slow fall + double jump
- *...and many more utility abilities*

## BanBox System

When players die (configurable conditions):
1. Player enters spectator mode at death location
2. Player head drops as item entity
3. Other players can left-click the head to revive the victim
4. If head is destroyed, victim is banned for configured days
5. Auto-unban after specified time period

### BanBox Features
- Cross-world revival support
- Configurable auto-unban timer
- Experience penalty on death
- Broadcast messages for deaths/revivals
- Keep inventory option

## HeadDatabase Integration

HeadStealX integrates with HeadDatabase for head textures:

### Automatic Setup (Recommended)
1. Plugin automatically downloads HeadDatabase API via Libby
2. Searches for existing HeadDatabase plugin
3. Falls back to basic head items if unavailable

### Manual Setup
1. Install HeadDatabase plugin
2. Use `/hdb search <mobname>` to find head IDs
3. Replace `HDB_*` placeholders in `heads.yml`
4. Reload plugin with `/xsteal reload`

## Development

### Building
```bash
./gradlew shadowJar
```

### Obfuscation
```bash
./gradlew obfuscate
```

### Testing
```bash
./gradlew test
```

## Compatibility

### Supported Versions
- **Minecraft**: 1.8.x - 1.21.8
- **Server Software**: Paper (recommended), Spigot, Folia
- **Java**: 8, 11, 17, 21

### Plugin Compatibility
- **HeadDatabase** - Full integration
- **Vault** - Economy features
- **PlaceholderAPI** - Placeholder support
- **MythicMobs** - Ability interactions
- **DiscordSRV** - Event announcements

### Anti-Cheat Compatibility
- NoCheatPlus, AAC, Matrix, Spartan
- Temporary exemptions during ability use

## Performance

### Optimizations
- Async processing for non-critical tasks
- Configurable particle and effect limits
- Efficient caching system
- Minimal main thread usage

### Recommended Settings
```yaml
performance:
  max_particles: 50
  async_processing: true
  cache_heads: true
  max_concurrent_abilities: 10
```

## Troubleshooting

### Common Issues

**Plugin won't load**
- Check Java version (8+ required)
- Verify server version compatibility
- Check console for error messages

**Heads not dropping**
- Ensure `require_charged_creeper` is true
- Check world restrictions in config
- Verify creeper is actually charged (lightning)

**Abilities not working**
- Check player permissions
- Verify head has valid ability configuration
- Enable debug mode for detailed logging

**HeadDatabase not working**
- Check if HeadDatabase plugin is installed
- Verify HDB IDs in heads.yml are correct
- Check Libby dependency loading in console

### Debug Mode
Enable debug logging:
```yaml
general:
  behavior:
    debug_mode: true
```

### Log Files
- Main log: `plugins/HeadStealX/headstealx.log`
- Console output with detailed phase logging
- Automatic log rotation

## Support

### Documentation
- In-game help: `/xsteal help`
- Debug info: `/xsteal debug`
- Configuration examples in plugin folder

### Community
- Report bugs via GitHub Issues
- Feature requests welcome
- Community Discord server

## License

HeadStealX is proprietary software. All rights reserved.

### Usage Terms
- Licensed for single server use
- No redistribution allowed
- Reverse engineering prohibited
- Commercial use permitted with license

### Anti-Tamper
This plugin includes anti-tamper protection:
- Code obfuscation
- Runtime integrity checks
- Debug detection
- License verification

---

**HeadStealX v1.0.0** - Premium Minecraft Plugin
Compatible with Minecraft 1.8 - 1.21.8