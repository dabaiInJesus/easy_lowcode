@echo off
chcp 65001 >nul
title Easy LowCode Platform
powershell -ExecutionPolicy Bypass -File "%~dp0start.ps1"
pause
