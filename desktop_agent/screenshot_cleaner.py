"""
Background Agent:
1. Screenshot Cleaner - press Ctrl+Shift+D after sending to delete last screenshot
2. ZIP Cleaner - if a ZIP is extracted (folder with same name appears) -> delete the ZIP
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
    HOTKEY_AVAILABLE = True
except ImportError:
    HOTKEY_AVAILABLE = False

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

DELETE_HOTKEY = "ctrl+shift+d"
MAX_SCREENSHOT_AGE_SECONDS = 600  # 10 minutes

last_screenshot = None


def log(message):
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"[{timestamp}] {message}", flush=True)


def notify(title, message):
    if NOTIFY_AVAILABLE:
        plyer_notification.notify(
            title=title,
            message=message,
            app_name="Desktop Agent",
            timeout=8,
        )


# ── Screenshot cleaner ────────────────────────────────────────────────────────

def get_latest_screenshot():
    """Find the most recently created screenshot file."""
    screenshots = list(SCREENSHOT_FOLDER.rglob("*"))
    screenshots = [f for f in screenshots if f.is_file() and f.suffix.lower() in SCREENSHOT_EXTENSIONS]
    if not screenshots:
        return None
    return max(screenshots, key=lambda f: f.stat().st_ctime)


def delete_sent_screenshot():
    """Called on hotkey press - delete the most recent screenshot if < 10 min old."""
    global last_screenshot

    target = last_screenshot or get_latest_screenshot()

    if target and target.exists():
        age = time.time() - target.stat().st_ctime
        if age <= MAX_SCREENSHOT_AGE_SECONDS:
            try:
                target.unlink()
                log(f"DELETED (hotkey): {target.name}")
                notify("Screenshot deleted!", target.name)
                last_screenshot = None
            except OSError as e:
                log(f"ERROR deleting {target.name}: {e}")
        else:
            log("No recent screenshot to delete (too old)")
    else:
        log("No screenshot found to delete")


class ScreenshotHandler(FileSystemEventHandler):
    def on_created(self, event):
        global last_screenshot
        if event.is_directory:
            return
        filepath = Path(event.src_path)
        if filepath.suffix.lower() in SCREENSHOT_EXTENSIONS:
            last_screenshot = filepath
            log(f"New screenshot: {filepath.name}")
            notify(
                "Screenshot saved!",
                f"After sending - press Ctrl+Shift+D to delete it."
            )


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

    obs_screenshot = Observer()
    obs_screenshot.schedule(ScreenshotHandler(), str(SCREENSHOT_FOLDER), recursive=True)
    obs_screenshot.start()
    observers.append(obs_screenshot)
    log(f"Screenshots watching: {SCREENSHOT_FOLDER}")

    for folder in ZIP_WATCH_FOLDERS:
        if folder.exists():
            obs_zip = Observer()
            obs_zip.schedule(ZipCleanerHandler(), str(folder), recursive=False)
            obs_zip.start()
            observers.append(obs_zip)
            log(f"ZIP watching: {folder}")

    if HOTKEY_AVAILABLE:
        keyboard.add_hotkey(DELETE_HOTKEY, delete_sent_screenshot)
        log(f"Hotkey ready: {DELETE_HOTKEY.upper()} = delete last screenshot")
    else:
        log("WARNING: 'keyboard' library not installed, hotkey disabled")

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
