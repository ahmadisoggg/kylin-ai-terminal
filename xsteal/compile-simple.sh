#!/bin/bash

# Simple XSteal compilation script
# Compiles the plugin without Gradle dependencies

echo "🔨 Compiling XSteal v1.0.0..."

# Create build directories
mkdir -p build/classes
mkdir -p build/libs

# Download required dependencies
echo "📦 Downloading dependencies..."

# Download Spigot API (1.20.1 for compatibility)
if [ ! -f "lib/spigot-api.jar" ]; then
    mkdir -p lib
    wget -q "https://repo.papermc.io/repository/maven-public/org/spigotmc/spigot-api/1.20.1-R0.1-SNAPSHOT/spigot-api-1.20.1-20230612.114739-141.jar" -O lib/spigot-api.jar
fi

# Download Libby
if [ ! -f "lib/libby.jar" ]; then
    wget -q "https://repo1.maven.org/maven2/net/byteflux/libby-bukkit/1.3.0/libby-bukkit-1.3.0.jar" -O lib/libby.jar
fi

echo "✅ Dependencies downloaded"

# Compile Java files
echo "🔨 Compiling Java source files..."

# Set classpath
CLASSPATH="lib/spigot-api.jar:lib/libby.jar"

# Find all Java files
JAVA_FILES=$(find src/main/java -name "*.java")

# Compile
javac -cp "$CLASSPATH" -d build/classes $JAVA_FILES

if [ $? -eq 0 ]; then
    echo "✅ Java compilation successful"
else
    echo "❌ Java compilation failed"
    exit 1
fi

# Create JAR
echo "📦 Creating JAR file..."

# Copy resources
cp -r src/main/resources/* build/classes/

# Create JAR
cd build/classes
jar cfm ../libs/XSteal.jar plugin.yml com/

if [ $? -eq 0 ]; then
    echo "✅ JAR creation successful"
    cd ../..
    echo "📦 XSteal.jar created at: $(pwd)/build/libs/XSteal.jar"
    echo "📊 JAR size: $(du -h build/libs/XSteal.jar | cut -f1)"
else
    echo "❌ JAR creation failed"
    exit 1
fi

echo ""
echo "🎉 XSteal v1.0.0 compilation complete!"
echo "📁 Output: build/libs/XSteal.jar"