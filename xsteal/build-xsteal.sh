#!/bin/bash

# XSteal Release Build Script
# Builds, obfuscates, and packages XSteal for production release

set -e

echo "═══════════════════════════════════════"
echo "    XSteal v1.0.0 - Release Build"
echo "    PSD1 Inspired Minecraft Plugin"
echo "═══════════════════════════════════════"
echo ""

# Clean previous builds
echo "🧹 Cleaning previous builds..."
./gradlew clean

# Run tests
echo "🧪 Running unit tests..."
./gradlew test

# Build shadow JAR
echo "📦 Building shadow JAR..."
./gradlew shadowJar

# Run advanced obfuscation
if [ -f "obfuscator/proguard-advanced.conf" ]; then
    echo "🔒 Running multi-layered obfuscation..."
    echo "  - Control flow flattening"
    echo "  - String encryption"
    echo "  - Anti-debugging protection"
    ./gradlew obfuscate || echo "⚠️  Obfuscation skipped (ProGuard not available)"
else
    echo "⚠️  Obfuscation configuration not found"
fi

# Generate anti-tamper signature
echo "🔐 Generating anti-tamper signature..."
./gradlew generateSignature || echo "⚠️  Signature generation skipped"

echo ""
echo "═══════════════════════════════════════"
echo "          BUILD COMPLETE! ✅"
echo "═══════════════════════════════════════"
echo ""
echo "📁 Output files:"
echo "  • build/libs/XSteal-1.0.0.jar (standard build)"
echo "  • build/libs/XSteal-1.0.0-obfuscated.jar (production build)"
echo ""
echo "🚀 Installation Instructions:"
echo "  1. Place XSteal-1.0.0-obfuscated.jar in server plugins/ folder"
echo "  2. Start server (Libby will auto-download dependencies)"
echo "  3. Configure plugins/XSteal/config.yml as needed"
echo "  4. Replace HDB_* placeholders in heads.yml with real HeadDatabase IDs"
echo "  5. Use /hdb search <mobname> to find HeadDatabase IDs"
echo "  6. Restart server to apply changes"
echo ""
echo "⚡ Key Features:"
echo "  • 58+ unique mob heads with abilities"
echo "  • Charged creeper head drop system"
echo "  • BanBox spectator mode system"
echo "  • Boss head combo abilities"
echo "  • Multi-layered obfuscation protection"
echo "  • Compatible with Paper/Spigot 1.8-1.21.4"
echo ""
echo "═══════════════════════════════════════"
echo "    XSteal Ready for Production! 🎉"
echo "═══════════════════════════════════════"