@echo off
echo ============================================
echo   Desktop Agent - Setup
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
pip install watchdog psutil keyboard plyer pywin32 pystray Pillow --quiet
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

copy "%VBS_FILE%" "%STARTUP_FOLDER%\DesktopAgent.vbs" >nul
if %errorlevel% neq 0 (
    echo [ERROR] Failed to add to startup. Run as Administrator.
    pause
    exit /b 1
)
echo [OK] Added to Windows Startup

echo Starting agent...
start "" pythonw "%SCRIPT_DIR%tray_agent.py"

echo.
echo ============================================
echo   Setup complete!
echo ============================================
echo.
echo - Agent starts automatically with Windows
echo - Look for the blue icon in the system tray (bottom right)
echo - Right-click the icon to see stats or quit
echo - Ctrl+V in Telegram/Chrome -> screenshot auto-deleted
echo - Ctrl+Shift+D -> delete last screenshot manually
echo - New downloads -> auto-sorted into subfolders
echo - Extracted ZIP -> archive auto-deleted
echo.
pause
