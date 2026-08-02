#!/bin/bash
# ==============================================================================
# CogniDB AI-Native Database AWS EC2 & Production Linux One-Line Installer (JDK 21 LTS)
# Usage: curl -fsSL https://raw.githubusercontent.com/cognidb/cognidb/main/deploy/aws_ec2_install.sh | bash
# ==============================================================================
set -e

echo "=========================================================================="
echo "⚡ CogniDB Production Cloud & EC2 Installer (JDK 21 LTS Optimized) ⚡"
echo "=========================================================================="

# 1. Install Java 21 dependency if missing
if ! command -v java &> /dev/null || [[ $(java -version 2>&1 | head -n 1) != *"21"* ]]; then
    echo "📦 Installing OpenJDK 21 LTS Runtime..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update -y && sudo apt-get install -y openjdk-21-jre-headless
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y java-21-openjdk
    elif command -v yum &> /dev/null; then
        sudo yum install -y java-21-openjdk
    fi
fi

# 2. Create system user and directories
sudo useradd -r -s /bin/false cognidb 2>/dev/null || true
sudo mkdir -p /etc/cognidb /var/lib/cognidb/data /var/lib/cognidb/wal /var/lib/cognidb/snapshots /usr/share/cognidb /var/log/cognidb
sudo chown -R cognidb:cognidb /var/lib/cognidb /var/log/cognidb

# 3. Create default production config
if [ ! -f /etc/cognidb/cognidb.conf ]; then
    echo "🔐 Creating production configuration at /etc/cognidb/cognidb.conf..."
    cat << EOF | sudo tee /etc/cognidb/cognidb.conf > /dev/null
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=admin
admin_password=$(openssl rand -hex 12 2>/dev/null || echo "CogniDB_Pass_2026!")
data_dir=/var/lib/cognidb/data
wal_dir=/var/lib/cognidb/wal
snapshot_dir=/var/lib/cognidb/snapshots
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
EOF
fi

# 4. Copy engine jar
if [ -f "target/cognidb-engine-1.0.0-SNAPSHOT.jar" ]; then
    sudo cp target/cognidb-engine-1.0.0-SNAPSHOT.jar /usr/share/cognidb/cognidb-engine.jar
fi

# 5. Create Systemd Service with Generational ZGC (JDK 21 Optimization)
cat << 'EOF' | sudo tee /etc/systemd/system/cognidb.service > /dev/null
[Unit]
Description=CogniDB AI-Native Database Server (JDK 21 LTS)
After=network.target remote-fs.target syslog.target

[Service]
Type=simple
User=cognidb
Group=cognidb
EnvironmentFile=/etc/cognidb/cognidb.conf
ExecStart=/usr/bin/java -Xms2g -Xmx8g -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+UseNUMA -XX:+UseStringDeduplication -jar /usr/share/cognidb/cognidb-engine.jar
Restart=always
RestartSec=5s
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF

# 6. Create CLI binary link /usr/local/bin/cognidb
cat << 'EOF' | sudo tee /usr/local/bin/cognidb > /dev/null
#!/bin/bash
CONF="/etc/cognidb/cognidb.conf"
PORT=$(grep "port=" $CONF 2>/dev/null | cut -d'=' -f2 || echo "8080")
USER=$(grep "admin_user=" $CONF 2>/dev/null | cut -d'=' -f2 || echo "admin")
PASS=$(grep "admin_password=" $CONF 2>/dev/null | cut -d'=' -f2 || echo "admin")

case "$1" in
    start) sudo systemctl start cognidb && echo "🟢 CogniDB started." ;;
    stop) sudo systemctl stop cognidb && echo "🛑 CogniDB stopped." ;;
    restart) sudo systemctl restart cognidb && echo "🔄 CogniDB restarted." ;;
    status) sudo systemctl status cognidb ;;
    cli|shell)
        shift 1
        java -cp /usr/share/cognidb/cognidb-engine.jar com.cognidb.cli.CogniCLI -u "$USER" -p "$PASS" -h "http://localhost:$PORT" "$@"
        ;;
    logs) journalctl -u cognidb -f ;;
    *)
        echo "Usage: cognidb {start|stop|restart|status|cli|logs}"
        ;;
esac
EOF
sudo chmod +x /usr/local/bin/cognidb

# Reload Systemd
sudo systemctl daemon-reload
sudo systemctl enable cognidb
sudo systemctl start cognidb

# Output EC2 Summary
PUBLIC_IP=$(curl -s checkip.amazonaws.com 2>/dev/null || echo "<EC2-PUBLIC-IP>")
PASS=$(grep "admin_password=" /etc/cognidb/cognidb.conf | cut -d'=' -f2)

echo ""
echo "=========================================================================="
echo "🎉 CogniDB Cloud / AWS EC2 Installation Complete (JDK 21 LTS)!"
echo "=========================================================================="
echo "🌐 Web Studio & SQL Shell: http://$PUBLIC_IP:8080/"
echo "🔑 Admin User             : admin"
echo "🔐 Admin Password         : $PASS"
echo "🔗 Connection String      : cognidb://admin:$PASS@$PUBLIC_IP:8080/production"
echo "=========================================================================="
