#!/bin/bash
# ==============================================================================
# SyntricDB AI-Native Database AWS EC2 & Production Linux One-Line Installer (JDK 21 LTS)
# Usage: curl -fsSL https://raw.githubusercontent.com/syntricdb/syntricdb/main/deploy/aws_ec2_install.sh | bash
# ==============================================================================
set -e

echo "=========================================================================="
echo "⚡ SyntricDB Production Cloud & EC2 Installer (JDK 21 LTS Optimized) ⚡"
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
sudo useradd -r -s /bin/false syntricdb 2>/dev/null || true
sudo mkdir -p /etc/syntricdb /var/lib/syntricdb/data /var/lib/syntricdb/wal /var/lib/syntricdb/snapshots /usr/share/syntricdb /var/log/syntricdb
sudo chown -R syntricdb:syntricdb /var/lib/syntricdb /var/log/syntricdb

# 3. Create production config with custom credentials
if [ -t 0 ] && [ -z "$SYNTRICDB_NON_INTERACTIVE" ]; then
    echo "🔐 Setting up Database Administrator Credentials:"
    read -p "   • Admin Username [default: admin]: " ADMIN_USER
    ADMIN_USER=${ADMIN_USER:-admin}

    read -sp "   • Admin Password [default: syntricdb_secret_pass]: " ADMIN_PASS
    echo ""
    ADMIN_PASS=${ADMIN_PASS:-syntricdb_secret_pass}
else
    ADMIN_USER=${SYNTRICDB_ADMIN_USER:-admin}
    ADMIN_PASS=${SYNTRICDB_ADMIN_PASSWORD:-syntricdb_secret_pass}
fi

echo "🔐 Creating production configuration at /etc/syntricdb/syntricdb.conf..."
cat << EOF | sudo tee /etc/syntricdb/syntricdb.conf > /dev/null
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=$ADMIN_USER
admin_password=$ADMIN_PASS
data_dir=/var/lib/syntricdb/data
wal_dir=/var/lib/syntricdb/wal
snapshot_dir=/var/lib/syntricdb/snapshots
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
EOF

# 4. Copy engine jar
if [ -f "target/syntricdb-engine-1.0.0-SNAPSHOT.jar" ]; then
    sudo cp target/syntricdb-engine-1.0.0-SNAPSHOT.jar /usr/share/syntricdb/syntricdb-engine.jar
fi

# 5. Create Systemd Service with Generational ZGC (JDK 21 Optimization)
cat << 'EOF' | sudo tee /etc/systemd/system/syntricdb.service > /dev/null
[Unit]
Description=SyntricDB AI-Native Database Server (JDK 21 LTS)
After=network.target remote-fs.target syslog.target

[Service]
Type=simple
User=syntricdb
Group=syntricdb
EnvironmentFile=/etc/syntricdb/syntricdb.conf
ExecStart=/usr/bin/java -Xms2g -Xmx8g -XX:+UseZGC -XX:+ZGenerational -XX:+AlwaysPreTouch -XX:+UseNUMA -XX:+UseStringDeduplication -jar /usr/share/syntricdb/syntricdb-engine.jar
Restart=always
RestartSec=5s
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF

# 6. Create CLI binary link /usr/local/bin/syntricdb
cat << 'EOF' | sudo tee /usr/local/bin/syntricdb > /dev/null
#!/bin/bash
CONF="/etc/syntricdb/syntricdb.conf"
PORT=$(grep "port=" $CONF 2>/dev/null | cut -d'=' -f2 || echo "8080")
USER=$(grep "admin_user=" $CONF 2>/dev/null | cut -d'=' -f2 || echo "admin")
PASS=$(grep "admin_password=" $CONF 2>/dev/null | cut -d'=' -f2 || echo "admin")

case "$1" in
    start) sudo systemctl start syntricdb && echo "🟢 SyntricDB started." ;;
    stop) sudo systemctl stop syntricdb && echo "🛑 SyntricDB stopped." ;;
    restart) sudo systemctl restart syntricdb && echo "🔄 SyntricDB restarted." ;;
    status) sudo systemctl status syntricdb ;;
    cli|shell)
        shift 1
        java -cp /usr/share/syntricdb/syntricdb-engine.jar com.syntricdb.cli.SyntricCLI -u "$USER" -p "$PASS" -h "http://localhost:$PORT" "$@"
        ;;
    logs) journalctl -u syntricdb -f ;;
    *)
        echo "Usage: syntricdb {start|stop|restart|status|cli|logs}"
        ;;
esac
EOF
sudo chmod +x /usr/local/bin/syntricdb

# Reload Systemd
sudo systemctl daemon-reload
sudo systemctl enable syntricdb
sudo systemctl start syntricdb

# Output EC2 Summary
PUBLIC_IP=$(curl -s checkip.amazonaws.com 2>/dev/null || echo "<EC2-PUBLIC-IP>")
PASS=$(grep "admin_password=" /etc/syntricdb/syntricdb.conf | cut -d'=' -f2)

echo ""
echo "=========================================================================="
echo "🎉 SyntricDB Cloud / AWS EC2 Installation Complete (JDK 21 LTS)!"
echo "=========================================================================="
echo "🌐 Web Studio & SQL Shell: http://$PUBLIC_IP:8080/"
echo "🔑 Admin User             : admin"
echo "🔐 Admin Password         : $PASS"
echo "🔗 Connection String      : syntricdb://admin:$PASS@$PUBLIC_IP:8080/production"
echo "=========================================================================="
