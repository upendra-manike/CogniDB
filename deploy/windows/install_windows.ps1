# ==============================================================================
# CogniDB Windows Native PowerShell Installer (Windows 10/11 & Server)
# Usage: powershell -ExecutionPolicy Bypass -File install_windows.ps1
# ==============================================================================

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "⚡ CogniDB Windows Native Database Installer ⚡" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$InstallDir = "$env:ProgramFiles\CogniDB"
$ConfigDir = "$env:APPDATA\CogniDB"
$ConfFile = "$ConfigDir\cognidb.conf"

# 1. Create Directories
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path $ConfigDir | Out-Null
New-Item -ItemType Directory -Force -Path "$ConfigDir\data" | Out-Null
New-Item -ItemType Directory -Force -Path "$ConfigDir\wal" | Out-Null
New-Item -ItemType Directory -Force -Path "$ConfigDir\snapshots" | Out-Null

# 2. Verify / Install Java 21
try {
    $javaVer = java -version 2>&1
    Write-Host "✅ Detected Java Runtime Environment." -ForegroundColor Green
} catch {
    Write-Host "📦 Installing OpenJDK 21 via winget..." -ForegroundColor Yellow
    winget install EclipseAdoptium.Temurin.21.JDK --silent --accept-package-agreements --accept-source-agreements
}

# 3. Create Default Configuration File
if (-not (Test-Path $ConfFile)) {
    $configContent = @"
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=admin
admin_password=cognidb_secret_pass
data_dir=$ConfigDir\data
wal_dir=$ConfigDir\wal
snapshot_dir=$ConfigDir\snapshots
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
"@
    Set-Content -Path $ConfFile -Value $configContent
    Write-Host "✅ Configuration saved to $ConfFile" -ForegroundColor Green
}

# 4. Copy JAR
if (Test-Path "target\cognidb-engine-1.0.0-SNAPSHOT.jar") {
    Copy-Item "target\cognidb-engine-1.0.0-SNAPSHOT.jar" "$InstallDir\cognidb-engine.jar" -Force
}

# 5. Create cognidb.bat CMD Launcher
$batContent = @"
@echo off
SET JAR_PATH="$InstallDir\cognidb-engine.jar"
IF "%1"=="start" (
    echo Starting CogniDB Engine on Port 8080...
    start /B java -Xms1g -Xmx4g -jar %JAR_PATH% > "%ConfigDir%\cognidb.log" 2>&1
    echo CogniDB Server launched in background.
    echo Web Dashboard: http://localhost:8080/
    EXIT /B 0
)
IF "%1"=="cli" (
    java -cp %JAR_PATH% com.cognidb.cli.CogniCLI %*
    EXIT /B 0
)
IF "%1"=="status" (
    tasklist | findstr /i "java.exe"
    EXIT /B 0
)
echo Usage: cognidb {start^|cli^|status}
"@

Set-Content -Path "$InstallDir\cognidb.bat" -Value $batContent

# 6. Add to System PATH Environment Variable
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -notlike "*$InstallDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$UserPath;$InstallDir", "User")
    Write-Host "✅ Added $InstallDir to User PATH environment variable." -ForegroundColor Green
}

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🎉 CogniDB Windows Installation Complete!" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🚀 Start Server   : cognidb start" -ForegroundColor Yellow
Write-Host "💻 Launch CLI      : cognidb cli" -ForegroundColor Yellow
Write-Host "🌐 Web Dashboard  : http://localhost:8080/" -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Cyan
