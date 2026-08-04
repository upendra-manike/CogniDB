#!/bin/bash
# ==============================================================================
# SyntricDB macOS Native Installer (Launchd Daemon & CLI Setup)
# Usage: curl -fsSL https://raw.githubusercontent.com/upendra-manike/SyntricDB/main/deploy/mac/install_mac.sh | bash
# ==============================================================================
set -e

echo "=========================================================================="
echo "⚡ SyntricDB macOS Native Installer ⚡"
echo "=========================================================================="

SYNTRICDB_DIR="$HOME/.syntricdb"
INSTALL_DIR="$SYNTRICDB_DIR/bin"
CONF_FILE="$SYNTRICDB_DIR/syntricdb.conf"
LAUNCHD_PLIST="$HOME/Library/LaunchAgents/com.syntricdb.server.plist"

mkdir -p "$SYNTRICDB_DIR" "$INSTALL_DIR" "$SYNTRICDB_DIR/data" "$SYNTRICDB_DIR/wal" "$SYNTRICDB_DIR/snapshots"

# 1. Check for Homebrew & Java 21
if ! command -v java &> /dev/null; then
    echo "📦 Java runtime missing. Installing OpenJDK 21 via Homebrew..."
    if command -v brew &> /dev/null; then
        brew install openjdk@21
        sudo ln -sfn $(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk 2>/dev/null || true
    else
        echo "❌ Please install Homebrew or JDK 21 to continue: https://brew.sh/"
        exit 1
    fi
fi

# 2. Setup Default Configuration if not existing
if [ ! -f "$CONF_FILE" ]; then
    cat << EOF > "$CONF_FILE"
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=admin
admin_password=syntricdb_secret_pass
data_dir=$SYNTRICDB_DIR/data
wal_dir=$SYNTRICDB_DIR/wal
snapshot_dir=$SYNTRICDB_DIR/snapshots
EOF
    echo "✅ Created configuration at $CONF_FILE"
fi

# 3. Copy/Build JAR
if [ -f "target/syntricdb-engine-1.0.0-SNAPSHOT.jar" ]; then
    cp target/syntricdb-engine-1.0.0-SNAPSHOT.jar "$INSTALL_DIR/syntricdb-engine.jar"
elif [ -f "$INSTALL_DIR/syntricdb-engine.jar" ]; then
    echo "✅ Found existing SyntricDB engine jar."
else
    echo "🔨 Building SyntricDB JAR..."
    mvn clean package -DskipTests
    cp target/syntricdb-engine-1.0.0-SNAPSHOT.jar "$INSTALL_DIR/syntricdb-engine.jar"
fi

# 4. Create Launcher Script
cat << 'EOF' > "$INSTALL_DIR/syntricdb"
#!/bin/bash
SYNTRICDB_DIR="$HOME/.syntricdb"
JAR_PATH="$SYNTRICDB_DIR/bin/syntricdb-engine.jar"
PID_FILE="$SYNTRICDB_DIR/syntricdb.pid"
LOG_FILE="$SYNTRICDB_DIR/syntricdb.log"

case "$1" in
    start)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "⚡ SyntricDB Server is already running (PID: $(cat "$PID_FILE"))"
            exit 0
        fi
        echo "🚀 Starting SyntricDB macOS Server (Port 8080)..."
        nohup java -Xms1g -Xmx4g -XX:+UseZGC -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"
        sleep 2
        echo "✅ SyntricDB Server started successfully (PID: $(cat "$PID_FILE"))"
        echo "🌐 Web Studio: http://localhost:8080/"
        ;;
    stop)
        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE")
            echo "🛑 Stopping SyntricDB Server (PID: $PID)..."
            kill "$PID" 2>/dev/null || true
            rm -f "$PID_FILE"
            echo "✅ SyntricDB stopped."
        else
            echo "⚠️ SyntricDB is not running."
        fi
        ;;
    status)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "🟢 SyntricDB is running (PID: $(cat "$PID_FILE"))"
        else
            echo "🔴 SyntricDB is stopped."
        fi
        ;;
    cli|shell)
        shift 1
        java -cp "$JAR_PATH" com.syntricdb.cli.SyntricCLI "$@"
        ;;
    logs)
        tail -f "$LOG_FILE"
        ;;
    *)
        echo "Usage: syntricdb {start|stop|status|cli|logs}"
        ;;
esac
EOF
chmod +x "$INSTALL_DIR/syntricdb"

# Symlink to /usr/local/bin if writable
if [ -d "/usr/local/bin" ] && [ -w "/usr/local/bin" ]; then
    ln -sf "$INSTALL_DIR/syntricdb" /usr/local/bin/syntricdb
    echo "✅ Linked binary to /usr/local/bin/syntricdb"
fi

# 5. Create macOS LaunchAgent PLIST for autostart
mkdir -p "$HOME/Library/LaunchAgents"
cat << EOF > "$LAUNCHD_PLIST"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.syntricdb.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/java</string>
        <string>-Xms1g</string>
        <string>-Xmx4g</string>
        <string>-jar</string>
        <string>$INSTALL_DIR/syntricdb-engine.jar</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>$SYNTRICDB_DIR/syntricdb.log</string>
    <key>StandardErrorPath</key>
    <string>$SYNTRICDB_DIR/syntricdb_error.log</string>
</dict>
</plist>
EOF

echo ""
echo "=========================================================================="
echo "🎉 SyntricDB macOS Installation Complete!"
echo "=========================================================================="
echo "🚀 Start Server   : syntricdb start"
echo "💻 Launch CLI      : syntricdb cli"
echo "🌐 Web Dashboard  : http://localhost:8080/"
echo "=========================================================================="
