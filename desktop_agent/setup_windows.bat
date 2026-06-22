@echo off
echo ============================================
echo   Desktop Agent - Setup (Windows)
echo ============================================
echo.

python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found. Download from https://python.org
    pause
    exit /b 1
)

echo [OK] Python found
echo.

echo Installing dependencies...
pip install plyer --quiet
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install dependencies
    pause
    exit /b 1
)
echo [OK] Dependencies installed
echo.

echo Setting up Task Scheduler (daily at 09:00)...

set TASK_NAME=DesktopAgent
set SCRIPT_PATH=%~dp0desktop_agent.py

schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

schtasks /create /tn "%TASK_NAME%" /tr "python \"%SCRIPT_PATH%\"" /sc DAILY /st 09:00 /rl HIGHEST /f

if %errorlevel% neq 0 (
    echo [ERROR] Failed to create scheduled task.
    echo Please run this file as Administrator.
    pause
    exit /b 1
)

echo [OK] Task "%TASK_NAME%" created - runs daily at 09:00
echo.
echo ============================================
echo   Setup complete!
echo ============================================
echo.
echo - Agent runs automatically every day at 09:00
echo - Config file: %~dp0config.json
echo - Log file: %USERPROFILE%\Desktop_Agent_Log.txt
echo - Manual run: python "%SCRIPT_PATH%"
echo.
pause
