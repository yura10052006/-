"""
Background Agent:
1. Screenshot Cleaner:
   - Detects Ctrl+V paste into Telegram -> auto-deletes last screenshot
   - Fallback hotkey Ctrl+Shift+D to manually delete last screenshot
2. ZIP Cleaner - extracted archive -> auto-delete the zip
"""

import time
import threading
import psutil
from pathlib import Path
from datetime import datetime
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

try:
    import keyboard
    KEYBOARD_AVAILABLE = True
except ImportError:
    KEYBOARD_AVAILABLE = False

try:
    import win32gui
    import win32process
    import win32clipboard
    WIN32_AVAILABLE = True
except ImportError:
    WIN32_AVAILABLE = False

try:
    from plyer import notification as plyer_notification
    NOTIFY_AVAILABLE = True
except ImportError:
    NOTIFY_AVAILABLE = False


SCREENSHOT_FOLDER = Path("D:/Screenshots/Screenshots")

ZIP_WATCH_FOLDERS = [
    Path.home() / "Desktop",
    Path.home() / "Downloads",
    Path.home() / "Documents",
]

ARCHIVE_EXTENSIONS = {".zip", ".rar", ".7z", ".tar", ".gz"}
SCREENSHOT_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"}

MAX_SCREENSHOT_AGE_SECONDS = 600  # 10 minutes

last_screenshot: Path | None = None


def log(message):
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"[{timestamp}] {message}", flush=True)


def notify(title, message):
    if NOTIFY_AVAILABLE:
        plyer_notification.notify(
            title=title,
            message=message,
            app_name="Desktop Agent",
            timeout=6,
        )


# ── Helpers ───────────────────────────────────────────────────────────────────

def get_active_process_name():
    """Return the .exe name of the currently focused window."""
    if not WIN32_AVAILABLE:
        return ""
    try:
        hwnd = win32gui.GetForegroundWindow()
        _, pid = win32process.GetWindowThreadProcessId(hwnd)
        for proc in psutil.process_iter(["pid", "name"]):
            if proc.pid == pid:
                return proc.name().lower()
    except Exception:
        pass
    return ""


def clipboard_has_image():
    """Return True if the clipboard currently holds an image."""
    if not WIN32_AVAILABLE:
        return False
    try:
        win32clipboard.OpenClipboard()
        has = (
            win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_DIB)
            or win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_BITMAP)
            or win32clipboard.IsClipboardFormatAvailable(win32clipboard.CF_DIBV5)
        )
        win32clipboard.CloseClipboard()
        return has
    except Exception:
        try:
            win32clipboard.CloseClipboard()
        except Exception:
            pass
        return False


def get_latest_screenshot():
    files = [
        f for f in SCREENSHOT_FOLDER.rglob("*")
        if f.is_file() and f.suffix.lower() in SCREENSHOT_EXTENSIONS
    ]
    return max(files, key=lambda f: f.stat().st_ctime) if files else None


def delete_screenshot(target: Path, reason: str):
    try:
        if target.exists():
            target.unlink()
            log(f"DELETED ({reason}): {target.name}")
            notify("Screenshot deleted!", target.name)
    except OSError as e:
        log(f"ERROR deleting {target.name}: {e}")


# ── Auto-detect Ctrl+V paste into Telegram ───────────────────────────────────

def on_paste():
    """Called every time Ctrl+V is pressed (globally)."""
    active = get_active_process_name()
    if active != "telegram.exe":
        return

    if not clipboard_has_image():
        return

    target = last_screenshot or get_latest_screenshot()
    if not target or not target.exists():
        return

    age = time.time() - target.stat().st_ctime
    if age > MAX_SCREENSHOT_AGE_SECONDS:
        return

    # Small delay so Telegram receives the paste before we delete the file
    def delayed_delete():
        time.sleep(1.5)
        delete_screenshot(target, "pasted in Telegram")

    threading.Thread(target=delayed_delete, daemon=True).start()


# ── Manual hotkey fallback ────────────────────────────────────────────────────

def on_manual_hotkey():
    target = last_screenshot or get_latest_screenshot()
    if not target or not target.exists():
        log("No recent screenshot to delete")
        return
    age = time.time() - target.stat().st_ctime
    if age <= MAX_SCREENSHOT_AGE_SECONDS:
        delete_screenshot(target, "manual hotkey")
    else:
        log("Screenshot is too old to delete")


# ── Screenshot watcher ────────────────────────────────────────────────────────

class ScreenshotHandler(FileSystemEventHandler):
    def on_created(self, event):
        global last_screenshot
        if event.is_directory:
            return
        filepath = Path(event.src_path)
        if filepath.suffix.lower() in SCREENSHOT_EXTENSIONS:
            last_screenshot = filepath
            log(f"New screenshot: {filepath.name}")


# ── ZIP cleaner ───────────────────────────────────────────────────────────────

def try_delete_archive(archive_path):
    archive_path = Path(archive_path)
    time.sleep(5)
    extracted = archive_path.parent / archive_path.stem
    if not extracted.exists():
        return
    try:
        if archive_path.exists():
            archive_path.unlink()
            log(f"ZIP DELETED (extracted): {archive_path.name}")
    except OSError as e:
        log(f"ERROR deleting archive {archive_path.name}: {e}")


class ZipCleanerHandler(FileSystemEventHandler):
    def on_created(self, event):
        if not event.is_directory:
            return
        new_folder = Path(event.src_path)
        parent = new_folder.parent
        for ext in ARCHIVE_EXTENSIONS:
            archive = parent / (new_folder.name + ext)
            if archive.exists():
                log(f"Extracted: {new_folder.name}/ -> will delete {archive.name}")
                threading.Thread(target=try_delete_archive, args=(archive,), daemon=True).start()


# ── Main ──────────────────────────────────────────────────────────────────────

def run():
    observers = []

    if not SCREENSHOT_FOLDER.exists():
        SCREENSHOT_FOLDER.mkdir(parents=True, exist_ok=True)

    obs = Observer()
    obs.schedule(ScreenshotHandler(), str(SCREENSHOT_FOLDER), recursive=True)
    obs.start()
    observers.append(obs)
    log(f"Screenshots watching: {SCREENSHOT_FOLDER}")

    for folder in ZIP_WATCH_FOLDERS:
        if folder.exists():
            obs_zip = Observer()
            obs_zip.schedule(ZipCleanerHandler(), str(folder), recursive=False)
            obs_zip.start()
            observers.append(obs_zip)
            log(f"ZIP watching: {folder}")

    if KEYBOARD_AVAILABLE:
        if WIN32_AVAILABLE:
            keyboard.add_hotkey("ctrl+v", on_paste, suppress=False)
            log("Auto-detect: Ctrl+V paste in Telegram -> screenshot deleted")
        keyboard.add_hotkey("ctrl+shift+d", on_manual_hotkey)
        log("Fallback hotkey: Ctrl+Shift+D -> delete last screenshot manually")
    else:
        log("WARNING: 'keyboard' library not installed")

    log("Agent running. Press Ctrl+C to stop.")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        for obs in observers:
            obs.stop()
        for obs in observers:
            obs.join()
        log("Agent stopped.")


if __name__ == "__main__":
    run()
