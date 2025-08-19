#!/bin/bash

# HeadStealX Release Build Script
# Builds, obfuscates, and packages the plugin for release

set -e

echo "=== HeadStealX Release Build ==="
echo "Building HeadStealX v1.0.0..."

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Run tests
echo "Running unit tests..."
./gradlew test

# Build shadow JAR
echo "Building shadow JAR..."
./gradlew shadowJar

# Run obfuscation (if ProGuard is configured)
if [ -f "obfuscator/proguard.conf" ]; then
    echo "Running obfuscation..."
    ./gradlew obfuscate || echo "Obfuscation skipped (ProGuard not available)"
fi

# Generate signature
echo "Generating anti-tamper signature..."
./gradlew generateSignature || echo "Signature generation skipped"

echo ""
echo "=== Build Complete ==="
echo "Output files:"
echo "  - build/libs/HeadStealX-1.0.0.jar (standard)"
echo "  - build/libs/HeadStealX-1.0.0-obfuscated.jar (obfuscated)"
echo ""
echo "Installation:"
echo "  1. Place JAR in server plugins/ folder"
echo "  2. Start server (dependencies auto-download)"
echo "  3. Configure plugins/HeadStealX/config.yml"
echo "  4. Replace HDB_* placeholders in heads.yml"
echo "  5. Restart server"
echo ""
echo "=== Ready for Release ==="