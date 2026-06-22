@echo off
chcp 65001 >nul
echo ============================================
echo   Desktop Agent - Встановлення (Windows)
echo ============================================
echo.

:: Check Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ПОМИЛКА] Python не знайдено. Завантажте з https://python.org
    pause
    exit /b 1
)

echo [OK] Python знайдено
echo.

:: Install dependencies
echo Встановлення залежностей...
pip install -r "%~dp0requirements.txt" --quiet
if %errorlevel% neq 0 (
    echo [ПОМИЛКА] Не вдалося встановити залежності
    pause
    exit /b 1
)
echo [OK] Залежності встановлено
echo.

:: Set up Windows Task Scheduler
echo Налаштування розкладу (щодня о 09:00)...

set TASK_NAME=DesktopAgent
set SCRIPT_PATH=%~dp0desktop_agent.py

:: Delete old task if exists
schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

:: Create new task - runs daily at 09:00
schtasks /create ^
    /tn "%TASK_NAME%" ^
    /tr "python \"%SCRIPT_PATH%\"" ^
    /sc DAILY ^
    /st 09:00 ^
    /rl HIGHEST ^
    /f

if %errorlevel% neq 0 (
    echo [ПОМИЛКА] Не вдалося створити задачу в планувальнику
    echo Спробуйте запустити цей файл від імені Адміністратора
    pause
    exit /b 1
)

echo [OK] Задача "%TASK_NAME%" створена - запуск щодня о 09:00
echo.
echo ============================================
echo   Встановлення завершено успішно!
echo ============================================
echo.
echo Що далі:
echo  - Агент запускатиметься автоматично щодня о 09:00
echo  - Налаштування: %~dp0config.json
echo  - Лог-файл: %USERPROFILE%\Desktop_Agent_Log.txt
echo  - Ручний запуск: python "%SCRIPT_PATH%"
echo.
echo Щоб змінити час запуску - відкрийте "Планувальник завдань" Windows
echo і відредагуйте задачу "%TASK_NAME%"
echo.
pause
