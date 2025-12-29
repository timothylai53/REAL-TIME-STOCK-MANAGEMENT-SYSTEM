# Real-Time Stock Management System - Quick Runner Script
# Usage: .\run.ps1 [command]
# Commands: compile, server, client, test, clean

param(
    [Parameter(Position=0)]
    [string]$command = "help"
)

function Show-Help {
    Write-Host "=== Real-Time Stock Management System ===" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Available commands:" -ForegroundColor Yellow
    Write-Host "  .\run.ps1 compile   - Compile all Java files"
    Write-Host "  .\run.ps1 server    - Start the server"
    Write-Host "  .\run.ps1 client    - Start interactive client"
    Write-Host "  .\run.ps1 test      - Run stress test (server must be running)"
    Write-Host "  .\run.ps1 clean     - Remove all .class files"
    Write-Host ""
    Write-Host "Quick Start Guide:" -ForegroundColor Yellow
    Write-Host "  1. .\run.ps1 compile   (compile once)"
    Write-Host "  2. .\run.ps1 server    (in terminal 1)"
    Write-Host "  3. .\run.ps1 client    (in terminal 2, 3, etc.)"
    Write-Host "  4. .\run.ps1 test      (in new terminal, server must be running)"
    Write-Host ""
}

function Compile-Project {
    Write-Host "Compiling Java files..." -ForegroundColor Green
    javac *.java
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ Compilation successful!" -ForegroundColor Green
    } else {
        Write-Host "✗ Compilation failed!" -ForegroundColor Red
    }
}

function Start-Server {
    Write-Host "Starting Stock Management Server..." -ForegroundColor Green
    Write-Host "Press Ctrl+C to stop the server" -ForegroundColor Yellow
    Write-Host ""
    java Server
}

function Start-Client {
    Write-Host "Starting Interactive Client..." -ForegroundColor Green
    Write-Host "Connecting to server at localhost:8888" -ForegroundColor Yellow
    Write-Host ""
    java SimpleClient
}

function Start-Test {
    Write-Host "Starting Concurrent Stress Test..." -ForegroundColor Green
    Write-Host "Make sure the server is running first!" -ForegroundColor Yellow
    Write-Host ""
    java ConcurrentStressTest
}

function Clean-Project {
    Write-Host "Removing compiled .class files..." -ForegroundColor Yellow
    Remove-Item *.class -ErrorAction SilentlyContinue
    Write-Host "✓ Cleanup complete!" -ForegroundColor Green
}

# Main command handler
switch ($command.ToLower()) {
    "compile" { Compile-Project }
    "server" { Start-Server }
    "client" { Start-Client }
    "test" { Start-Test }
    "clean" { Clean-Project }
    "help" { Show-Help }
    default {
        Write-Host "Unknown command: $command" -ForegroundColor Red
        Write-Host ""
        Show-Help
    }
}

