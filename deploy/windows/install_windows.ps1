# ==============================================================================
# SyntricDB Windows Native PowerShell Installer (Windows 10/11 & Server)
# Usage: powershell -ExecutionPolicy Bypass -File install_windows.ps1
# ==============================================================================

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "⚡ SyntricDB Windows Native Database Installer ⚡" -ForegroundColor Cyan
Write-Host "==========================================================================" -ForegroundColor Cyan

$InstallDir = "$env:ProgramFiles\SyntricDB"
$ConfigDir = "$env:APPDATA\SyntricDB"
$ConfFile = "$ConfigDir\syntricdb.conf"

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

# 3. Setup Configuration File with Custom Credentials
Write-Host "🔐 Setting up Database Administrator Credentials:" -ForegroundColor Yellow
$inputUser = Read-Host "   • Admin Username [default: admin]"
$AdminUser = if ([string]::IsNullOrWhiteSpace($inputUser)) { "admin" } else { $inputUser }

$inputPass = Read-Host "   • Admin Password [default: syntricdb_secret_pass]" -AsSecureString
$BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($inputPass)
$PlainPass = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
$AdminPass = if ([string]::IsNullOrWhiteSpace($PlainPass)) { "syntricdb_secret_pass" } else { $PlainPass }

$configContent = @"
bind_address=0.0.0.0
port=8080
auth_enabled=true
admin_user=$AdminUser
admin_password=$AdminPass
data_dir=$ConfigDir\data
wal_dir=$ConfigDir\wal
snapshot_dir=$ConfigDir\snapshots
firewall_enabled=true
rate_limit_per_sec=1000
dlp_masking_enabled=true
"@
Set-Content -Path $ConfFile -Value $configContent
Write-Host "✅ Configuration saved to $ConfFile" -ForegroundColor Green

# 4. Copy JAR
if (Test-Path "target\syntricdb-engine-1.0.0-SNAPSHOT.jar") {
    Copy-Item "target\syntricdb-engine-1.0.0-SNAPSHOT.jar" "$InstallDir\syntricdb-engine.jar" -Force
}

# 5. Create syntricdb.bat CMD Launcher
$batContent = @"
@echo off
SET JAR_PATH="$InstallDir\syntricdb-engine.jar"
IF "%1"=="start" (
    echo Starting SyntricDB Engine on Port 8080...
    start /B java -Xms1g -Xmx4g -jar %JAR_PATH% > "%ConfigDir%\syntricdb.log" 2>&1
    echo SyntricDB Server launched in background.
    echo Web Dashboard: http://localhost:8080/
    EXIT /B 0
)
IF "%1"=="cli" (
    java -cp %JAR_PATH% com.syntricdb.cli.SyntricCLI %*
    EXIT /B 0
)
IF "%1"=="status" (
    tasklist | findstr /i "java.exe"
    EXIT /B 0
)
echo Usage: syntricdb {start^|cli^|status}
"@

Set-Content -Path "$InstallDir\syntricdb.bat" -Value $batContent

# 6. Add to System PATH Environment Variable
$UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
if ($UserPath -notlike "*$InstallDir*") {
    [Environment]::SetEnvironmentVariable("Path", "$UserPath;$InstallDir", "User")
    Write-Host "✅ Added $InstallDir to User PATH environment variable." -ForegroundColor Green
}

Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🎉 SyntricDB Windows Installation Complete!" -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Cyan
Write-Host "🚀 Start Server   : syntricdb start" -ForegroundColor Yellow
Write-Host "💻 Launch CLI      : syntricdb cli" -ForegroundColor Yellow
Write-Host "🌐 Web Dashboard  : http://localhost:8080/" -ForegroundColor Yellow
Write-Host "==========================================================================" -ForegroundColor Cyan
