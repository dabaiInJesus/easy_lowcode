# ========================================
# Easy LowCode Platform - Startup Script
# ========================================

$ErrorActionPreference = "Continue"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

# Fix encoding for Chinese output
chcp 65001 > $null
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================"
Write-Host "  Easy LowCode Platform - Startup"
Write-Host "========================================"
Write-Host ""

# ---- [0/5] Load .env ----
Write-Host "[0/5] Loading .env ..."
$envFile = Join-Path $ProjectRoot ".env"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -gt 0) {
            $key = $line.Substring(0, $idx).Trim()
            $value = $line.Substring($idx + 1).Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
        }
    }
    Write-Host "  [OK] .env loaded" -ForegroundColor Green
} else {
    Write-Host "  [WARN] .env not found, using defaults" -ForegroundColor DarkYellow
}
Write-Host ""

# ---- Add Java & Node.js to PATH ----
$javaHome = "C:\Users\Administrator\.jdks\ms-21.0.10"
if (Test-Path "$javaHome\bin\java.exe") {
    $env:JAVA_HOME = $javaHome
    $env:PATH = "$javaHome\bin;$env:PATH"
}
$nodePath = "C:\install\nodejs"
if (Test-Path "$nodePath\node.exe") {
    $env:PATH = "$nodePath;$env:PATH"
}

# ---- [1/5] Check Java ----
Write-Host "[1/5] Checking Java ..."
$null = & java -version 2>&1
if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
    Write-Host "  [ERROR] Java not found. Install JDK 21+" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
$javaVer = & java -version 2>&1 | Select-Object -First 1
Write-Host "  [OK] $javaVer" -ForegroundColor Green
Write-Host ""

# ---- [2/5] Check Node.js ----
Write-Host "[2/5] Checking Node.js ..."
$null = & node -v 2>&1
if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne $null) {
    Write-Host "  [ERROR] Node.js not found. Install Node.js 18+" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}
$nodeVer = & node -v 2>&1
Write-Host "  [OK] Node.js $nodeVer" -ForegroundColor Green
Write-Host ""

# ---- [3/5] Pre-check ports (kill old processes) ----
Write-Host "[3/5] Pre-checking ports ..."
$portsToCheck = @(8081, 6173)
foreach ($port in $portsToCheck) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Listen' }
    foreach ($conn in $connections) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  Port $port occupied by: $($proc.ProcessName) (PID: $($proc.Id))" -ForegroundColor DarkYellow
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Write-Host "  Killed process $($proc.ProcessName)" -ForegroundColor Green
        }
    }
}
Write-Host "  [OK] Ports 8081 & 6173 are free" -ForegroundColor Green
Write-Host ""

# ---- [4/5] Build backend (skip if already built) ----
Write-Host "[4/5] Checking backend build ..."
$mvnw = Join-Path $ProjectRoot "mvnw.cmd"
$startupJar = Join-Path $ProjectRoot "easy-lowcode-startup\target\easy-lowcode-startup-1.0.0-SNAPSHOT.jar"
if (Test-Path $startupJar) {
    Write-Host "  [SKIP] Already built, use 'mvn clean install' to rebuild" -ForegroundColor DarkYellow
} else {
    Write-Host "  Building all modules ..."
    & $mvnw clean install -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [ERROR] Build failed" -ForegroundColor Red
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Host "  [OK] Build success" -ForegroundColor Green
}
Write-Host ""

# ---- [5/5] Start services ----
Write-Host "[5/5] Starting services ..."
Write-Host ""
Write-Host "========================================"
Write-Host "  Backend:  http://localhost:8081"
Write-Host "  Frontend: http://localhost:6173"
Write-Host "  Account:  admin / admin123"
Write-Host "  Press Ctrl+C to stop"
Write-Host "========================================"
Write-Host ""

# Start frontend in new window
Write-Host "Starting frontend ..."
$frontendDir = Join-Path $ProjectRoot "easy-lowcode-frontend"
Start-Process cmd -ArgumentList "/k", "cd /d `"$frontendDir`" && npm run dev" -WindowStyle Normal

Start-Sleep -Seconds 3

# Start backend in current window
Write-Host "Starting backend ..."
$startupDir = Join-Path $ProjectRoot "easy-lowcode-startup"
Set-Location $startupDir
& $mvnw spring-boot:run
