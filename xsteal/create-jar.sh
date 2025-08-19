#!/bin/bash

# Create XSteal JAR without external dependencies
# This creates a JAR structure that can be compiled on the target server

echo "🔨 Creating XSteal v1.0.0 JAR..."

# Create build directory
mkdir -p build/jar

# Copy all Java source files to build directory
echo "📋 Copying source files..."
cp -r src/main/java/* build/jar/
cp -r src/main/resources/* build/jar/

# Create manifest
echo "📝 Creating manifest..."
cat > build/jar/META-INF/MANIFEST.MF << EOF
Manifest-Version: 1.0
Main-Class: com.xreatlabs.xsteal.XSteal
Implementation-Title: XSteal
Implementation-Version: 1.0.0
Implementation-Vendor: XreatLabs
Built-By: XreatLabs
Build-Timestamp: $(date)
EOF

# Create JAR
echo "📦 Creating JAR file..."
cd build/jar
jar cfm ../XSteal.jar META-INF/MANIFEST.MF .
cd ../..

if [ -f "build/XSteal.jar" ]; then
    echo "✅ XSteal.jar created successfully!"
    echo "📊 JAR size: $(du -h build/XSteal.jar | cut -f1)"
    echo "📁 Location: $(pwd)/build/XSteal.jar"
    
    # Verify JAR contents
    echo ""
    echo "📋 JAR Contents Verification:"
    jar tf build/XSteal.jar | head -20
    echo "... (and more)"
    echo ""
    echo "🎉 XSteal v1.0.0 by XreatLabs ready for deployment!"
else
    echo "❌ JAR creation failed"
    exit 1
fi