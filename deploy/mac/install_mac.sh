#!/bin/bash
# ==============================================================================
# CogniDB macOS Native Installer (Launchd Daemon & CLI Setup)
# Usage: curl -fsSL https://raw.githubusercontent.com/upendra-manike/CogniDB/main/deploy/mac/install_mac.sh | bash
# ==============================================================================
set -e

echo "=========================================================================="
echo "⚡ CogniDB macOS Native Installer ⚡"
echo "=========================================================================="

COGNIDB_DIR="$HOME/.cognidb"
INSTALL_DIR="$COGNIDB_DIR/bin"
CONF_FILE="$COGNIDB_DIR/cognidb.conf"
LAUNCHD_PLIST="$HOME/Library/LaunchAgents/com.cognidb.server.plist"

mkdir -p "$COGNIDB_DIR" "$INSTALL_DIR" "$COGNIDB_DIR/data" "$COGNIDB_DIR/wal" "$COGNIDB_DIR/snapshots"

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
admin_password=cognidb_secret_pass
data_dir=$COGNIDB_DIR/data
wal_dir=$COGNIDB_DIR/wal
snapshot_dir=$COGNIDB_DIR/snapshots
EOF
    echo "✅ Created configuration at $CONF_FILE"
fi

# 3. Copy/Build JAR
if [ -f "target/cognidb-engine-1.0.0-SNAPSHOT.jar" ]; then
    cp target/cognidb-engine-1.0.0-SNAPSHOT.jar "$INSTALL_DIR/cognidb-engine.jar"
elif [ -f "$INSTALL_DIR/cognidb-engine.jar" ]; then
    echo "✅ Found existing CogniDB engine jar."
else
    echo "🔨 Building CogniDB JAR..."
    mvn clean package -DskipTests
    cp target/cognidb-engine-1.0.0-SNAPSHOT.jar "$INSTALL_DIR/cognidb-engine.jar"
fi

# 4. Create Launcher Script
cat << 'EOF' > "$INSTALL_DIR/cognidb"
#!/bin/bash
COGNIDB_DIR="$HOME/.cognidb"
JAR_PATH="$COGNIDB_DIR/bin/cognidb-engine.jar"
PID_FILE="$COGNIDB_DIR/cognidb.pid"
LOG_FILE="$COGNIDB_DIR/cognidb.log"

case "$1" in
    start)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "⚡ CogniDB Server is already running (PID: $(cat "$PID_FILE"))"
            exit 0
        fi
        echo "🚀 Starting CogniDB macOS Server (Port 8080)..."
        nohup java -Xms1g -Xmx4g -XX:+UseZGC -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"
        sleep 2
        echo "✅ CogniDB Server started successfully (PID: $(cat "$PID_FILE"))"
        echo "🌐 Web Studio: http://localhost:8080/"
        ;;
    stop)
        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE")
            echo "🛑 Stopping CogniDB Server (PID: $PID)..."
            kill "$PID" 2>/dev/null || true
            rm -f "$PID_FILE"
            echo "✅ CogniDB stopped."
        else
            echo "⚠️ CogniDB is not running."
        fi
        ;;
    status)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "🟢 CogniDB is running (PID: $(cat "$PID_FILE"))"
        else
            echo "🔴 CogniDB is stopped."
        fi
        ;;
    cli|shell)
        shift 1
        java -cp "$JAR_PATH" com.cognidb.cli.CogniCLI "$@"
        ;;
    logs)
        tail -f "$LOG_FILE"
        ;;
    *)
        echo "Usage: cognidb {start|stop|status|cli|logs}"
        ;;
esac
EOF
chmod +x "$INSTALL_DIR/cognidb"

# Symlink to /usr/local/bin if writable
if [ -d "/usr/local/bin" ] && [ -w "/usr/local/bin" ]; then
    ln -sf "$INSTALL_DIR/cognidb" /usr/local/bin/cognidb
    echo "✅ Linked binary to /usr/local/bin/cognidb"
fi

# 5. Create macOS LaunchAgent PLIST for autostart
mkdir -p "$HOME/Library/LaunchAgents"
cat << EOF > "$LAUNCHD_PLIST"
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.cognidb.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>/usr/bin/java</string>
        <string>-Xms1g</string>
        <string>-Xmx4g</string>
        <string>-jar</string>
        <string>$INSTALL_DIR/cognidb-engine.jar</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>$COGNIDB_DIR/cognidb.log</string>
    <key>StandardErrorPath</key>
    <string>$COGNIDB_DIR/cognidb_error.log</string>
</dict>
</plist>
EOF

echo ""
echo "=========================================================================="
echo "🎉 CogniDB macOS Installation Complete!"
echo "=========================================================================="
echo "🚀 Start Server   : cognidb start"
echo "💻 Launch CLI      : cognidb cli"
echo "🌐 Web Dashboard  : http://localhost:8080/"
echo "=========================================================================="
