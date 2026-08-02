#!/bin/bash

# CogniDB Installer Script with Credentials & Interactive CLI Setup
set -e

COGNIDB_DIR="$HOME/.cognidb"
INSTALL_DIR="$COGNIDB_DIR/bin"
CONF_FILE="$COGNIDB_DIR/cognidb.conf"
JAR_PATH="$(pwd)/target/cognidb-engine-1.0.0-SNAPSHOT.jar"

echo "=========================================================================="
echo "⚡ CogniDB AI-Native Database Enterprise Installer ⚡"
echo "=========================================================================="

mkdir -p "$COGNIDB_DIR" "$INSTALL_DIR"

# Interactive Credential Setup if not passed via env
if [ -t 0 ] && [ -z "$COGNIDB_NON_INTERACTIVE" ]; then
    echo "🔐 Setting up Database Administrator Credentials:"
    read -p "   • Admin Username [default: admin]: " ADMIN_USER
    ADMIN_USER=${ADMIN_USER:-admin}

    read -sp "   • Admin Password [default: cognidb_secret_pass]: " ADMIN_PASS
    echo ""
    ADMIN_PASS=${ADMIN_PASS:-cognidb_secret_pass}

    read -p "   • Database Port [default: 8080]: " ADMIN_PORT
    ADMIN_PORT=${ADMIN_PORT:-8080}
else
    ADMIN_USER=${COGNIDB_ADMIN_USER:-admin}
    ADMIN_PASS=${COGNIDB_ADMIN_PASSWORD:-cognidb_secret_pass}
    ADMIN_PORT=${COGNIDB_PORT:-8080}
fi

# Write Configuration
cat << EOF > "$CONF_FILE"
bind_address=0.0.0.0
port=$ADMIN_PORT
auth_enabled=true
admin_user=$ADMIN_USER
admin_password=$ADMIN_PASS
data_dir=$COGNIDB_DIR/data
wal_dir=$COGNIDB_DIR/wal
snapshot_dir=$COGNIDB_DIR/snapshots
EOF

echo "✅ Saved configuration to $CONF_FILE"

if [ ! -f "$JAR_PATH" ]; then
    echo "🔨 Building CogniDB production JAR..."
    mvn clean package -DskipTests
fi

cp "$JAR_PATH" "$INSTALL_DIR/cognidb-engine.jar"

# Create launcher script
LAUNCHER="$INSTALL_DIR/cognidb"

cat << 'EOF' > "$LAUNCHER"
#!/bin/bash

COGNIDB_DIR="$HOME/.cognidb"
COGNIDB_JAR="$COGNIDB_DIR/bin/cognidb-engine.jar"
CONF_FILE="$COGNIDB_DIR/cognidb.conf"
PID_FILE="$COGNIDB_DIR/cognidb.pid"
LOG_FILE="$COGNIDB_DIR/cognidb.log"

# Load config variables if present
if [ -f "$CONF_FILE" ]; then
    source "$CONF_FILE" 2>/dev/null || true
fi

PORT=${port:-8080}
USER=${admin_user:-admin}
PASS=${admin_password:-cognidb_secret_pass}

case "$1" in
    start|server)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "⚡ CogniDB Server is already running (PID: $(cat "$PID_FILE"))"
            echo "🌐 Web Studio: http://localhost:$PORT/"
            exit 0
        fi
        echo "🚀 Starting CogniDB Server on port $PORT..."
        nohup java -jar "$COGNIDB_JAR" > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"
        sleep 2
        echo "✅ CogniDB Server started successfully (PID: $(cat "$PID_FILE"))"
        echo "🔑 Admin User    : $USER"
        echo "🌐 Web Console  : http://localhost:$PORT/"
        echo "📡 REST API     : http://localhost:$PORT/api/sql"
        echo "🔗 Connection URI: cognidb://$USER:*****@localhost:$PORT/default"
        ;;
    stop)
        if [ -f "$PID_FILE" ]; then
            PID=$(cat "$PID_FILE")
            echo "🛑 Stopping CogniDB Server (PID: $PID)..."
            kill "$PID" 2>/dev/null || true
            rm -f "$PID_FILE"
            echo "✅ CogniDB Server stopped."
        else
            echo "⚠️ CogniDB Server is not running."
        fi
        ;;
    status)
        if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
            echo "🟢 CogniDB Server is running (PID: $(cat "$PID_FILE"))"
            echo "🌐 Web Console: http://localhost:$PORT/"
        else
            echo "🔴 CogniDB Server is stopped."
        fi
        ;;
    cli|shell)
        shift 1
        java -cp "$COGNIDB_JAR" com.cognidb.cli.CogniCLI -u "$USER" -p "$PASS" -h "http://localhost:$PORT" "$@"
        ;;
    logs)
        tail -f "$LOG_FILE"
        ;;
    *)
        echo "=========================================================="
        echo "⚡ CogniDB: Next-Generation AI-Native Unified Database ⚡"
        echo "=========================================================="
        echo "Usage: cognidb {start|stop|status|cli|logs}"
        echo "  cognidb start   : Launch background server daemon (Port $PORT)"
        echo "  cognidb stop    : Shutdown background server daemon"
        echo "  cognidb status  : Check server status and endpoint info"
        echo "  cognidb cli     : Launch interactive SQL & Vector shell"
        echo "  cognidb logs    : Tail server stdout/stderr logs"
        echo "=========================================================="
        ;;
esac
EOF

chmod +x "$LAUNCHER"

# Attempt to link into /usr/local/bin or advise path addition
if [ -d "/usr/local/bin" ] && [ -w "/usr/local/bin" ]; then
    ln -sf "$LAUNCHER" /usr/local/bin/cognidb
    echo "✅ Executable linked to /usr/local/bin/cognidb"
else
    echo "✅ Executable installed to $LAUNCHER"
    echo "💡 Add to PATH: export PATH=\"\$HOME/.cognidb/bin:\$PATH\""
fi

echo ""
echo "=========================================================================="
echo "🎉 CogniDB Installation Complete!"
echo "=========================================================================="
echo "🔑 Connection String: cognidb://$ADMIN_USER:$ADMIN_PASS@localhost:$ADMIN_PORT/default"
echo "🚀 Start Database   : cognidb start"
echo "💻 Launch CLI Shell : cognidb cli"
echo "=========================================================================="
