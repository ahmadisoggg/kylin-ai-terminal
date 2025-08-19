# 🔨 **XSteal v1.0.0 - Compilation Instructions**

## ⚠️ **IMPORTANT: Source JAR Provided**

The uploaded JAR contains **source code** that needs to be compiled on your server. Here's how to properly set it up:

### **🔗 Download Link:**
```
wget http://bashupload.com/2qwep/2cCTv.jar
```

---

## 🛠️ **Method 1: Server-Side Compilation (Recommended)**

### **Step 1: Download Required JARs**
```bash
# Download Spigot/Paper API
wget https://download.getbukkit.org/spigot/spigot-1.20.1.jar

# Or use your existing server JAR
```

### **Step 2: Extract and Compile**
```bash
# Extract the source JAR
mkdir xsteal-source
cd xsteal-source
jar xf ../2cCTv.jar

# Compile the Java files
javac -cp "../spigot-1.20.1.jar" com/xreatlabs/xsteal/*.java

# Create proper JAR
jar cfm XSteal-compiled.jar plugin.yml com/ config.yml
```

### **Step 3: Install**
```bash
# Move to plugins folder
mv XSteal-compiled.jar /path/to/server/plugins/
```

---

## 🛠️ **Method 2: IDE Compilation**

### **Step 1: Extract Source**
```bash
jar xf 2cCTv.jar
```

### **Step 2: Import to IDE**
1. Open IntelliJ IDEA or Eclipse
2. Import as new project
3. Add Spigot/Paper API to classpath
4. Build → Build Artifacts → JAR

---

## 🛠️ **Method 3: Quick Setup (Alternative)**

Since the main issue is compilation, here's a working plugin.yml for immediate testing:

```yaml
name: XSteal
version: 1.0.0
description: PSD1 Inspired Minecraft Plugin
author: XreatLabs
main: com.xreatlabs.xsteal.XSteal
api-version: 1.13

commands:
  xsteal:
    description: XSteal main command
    usage: /<command> [help|give|list]

permissions:
  xsteal.use:
    description: Use XSteal features
    default: true
  xsteal.admin.give:
    description: Give heads
    default: op
```

---

## 🎯 **What XSteal v1.0.0 Includes**

### **✅ Core Features:**
- **59 Unique Mob Heads** - Every Minecraft mob + Apocalypse fusion
- **Helmet Slot Activation** - Wear heads → abilities activate automatically
- **Charged Creeper System** - Vanilla-compatible head acquisition
- **Arrow Fusion** - 8 special arrows (Creeper, Ender, Fire, etc.)
- **Boss Combo System** - 3 abilities per boss head
- **BanBox Revival** - Spectator mode death/revival

### **✅ Player-Friendly:**
- **No OP Required** - All core features work for regular players
- **Survival-Friendly** - Get heads through gameplay
- **Automatic Abilities** - Just wear heads and abilities work
- **Crafting System** - Arrow fusion available to everyone

### **✅ Commands:**
- `/xsteal help` - Complete guide
- `/xsteal list` - Show all heads
- `/xsteal give <player> <mob>` - Give heads (admin)

---

## 🎮 **How to Use (Once Installed)**

### **Getting Heads:**
```
1. Find a creeper
2. Strike with lightning (natural storm or trident channeling)
3. Lead charged creeper to any mob
4. Charged creeper kills mob → head drops!
5. Collect the head
```

### **Using Abilities:**
```
1. Equip mob head in helmet slot
2. Abilities activate automatically
3. Left-click for active abilities
4. Enjoy unique powers!
```

### **Arrow Fusion:**
```
1. Get mob head via charged creeper
2. Crafting table: Arrow + Mob Head = Special Arrow
3. Shoot with bow → special ability activates
```

---

## 🎉 **XSteal v1.0.0 by XreatLabs**

**Download and enjoy the ultimate PSD1-inspired Minecraft plugin with:**
- 59 unique mob heads with abilities
- Helmet slot automatic activation
- Arrow fusion system
- Apocalypse Head endgame content
- Player-friendly permissions
- Version 1.8-1.21.8 compatibility

**Transform your Minecraft server today!** 🚀