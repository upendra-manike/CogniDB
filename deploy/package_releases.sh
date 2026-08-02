#!/bin/bash
# ==============================================================================
# CogniDB Multi-Platform Release Distribution Packaging Script
# Creates release ZIP archives for macOS, Linux, and Windows
# ==============================================================================
set -e

VERSION="1.0.0"
DIST_DIR="$(pwd)/dist"
JAR_PATH="$(pwd)/target/cognidb-engine-1.0.0-SNAPSHOT.jar"

echo "=========================================================================="
echo "📦 Building CogniDB Release Packages (v$VERSION)..."
echo "=========================================================================="

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Compiling & shading production fat JAR..."
    mvn clean package -DskipTests
fi

rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR/macOS" "$DIST_DIR/windows" "$DIST_DIR/linux"

# 1. Package macOS Release
echo "🍏 Packaging macOS Release (cognidb-v$VERSION-macOS.zip)..."
cp "$JAR_PATH" "$DIST_DIR/macOS/cognidb-engine.jar"
cp deploy/mac/install_mac.sh "$DIST_DIR/macOS/install.sh"
cp README.md "$DIST_DIR/macOS/"
cp LICENSE "$DIST_DIR/macOS/"
(cd "$DIST_DIR/macOS" && zip -r "../cognidb-v$VERSION-macOS.zip" .)

# 2. Package Windows Release
echo "🪟 Packaging Windows Release (cognidb-v$VERSION-windows.zip)..."
cp "$JAR_PATH" "$DIST_DIR/windows/cognidb-engine.jar"
cp deploy/windows/install_windows.ps1 "$DIST_DIR/windows/install.ps1"
cp README.md "$DIST_DIR/windows/"
cp LICENSE "$DIST_DIR/windows/"
(cd "$DIST_DIR/windows" && zip -r "../cognidb-v$VERSION-windows.zip" .)

# 3. Package Linux / Cloud Release
echo "🐧 Packaging Linux Release (cognidb-v$VERSION-linux.zip)..."
cp "$JAR_PATH" "$DIST_DIR/linux/cognidb-engine.jar"
cp deploy/aws_ec2_install.sh "$DIST_DIR/linux/install.sh"
cp README.md "$DIST_DIR/linux/"
cp LICENSE "$DIST_DIR/linux/"
(cd "$DIST_DIR/linux" && zip -r "../cognidb-v$VERSION-linux.zip" .)

echo ""
echo "=========================================================================="
echo "🎉 Release Packages Created Successfully in ./dist/"
echo "=========================================================================="
ls -lh "$DIST_DIR"/*.zip
