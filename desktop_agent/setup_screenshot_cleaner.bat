@echo off
echo ============================================
echo   Screenshot Cleaner - Setup
echo ============================================
echo.

python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Python not found. Download from https://python.org
    pause
    exit /b 1
)
echo [OK] Python found

echo Installing dependencies...
pip install watchdog psutil keyboard plyer --quiet
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install dependencies
    pause
    exit /b 1
)
echo [OK] Dependencies installed
echo.

set SCRIPT_DIR=%~dp0
set VBS_FILE=%SCRIPT_DIR%start_screenshot_cleaner.vbs
set STARTUP_FOLDER=%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup

copy "%VBS_FILE%" "%STARTUP_FOLDER%\ScreenshotCleaner.vbs" >nul

if %errorlevel% neq 0 (
    echo [ERROR] Failed to add to startup
    pause
    exit /b 1
)

echo [OK] Added to Windows Startup
echo.

echo Starting agent now...
start "" python "%SCRIPT_DIR%screenshot_cleaner.py"

echo.
echo ============================================
echo   Setup complete!
echo ============================================
echo.
echo - Agent starts automatically with Windows
echo - Monitors: D:\Screenshots\Screenshots
echo - If screenshot is sent via Telegram or browser within 60s -> deleted
echo - If not sent -> kept
echo.
pause
