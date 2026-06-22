"""
Background Agent:
1. Screenshot Cleaner - if screenshot is sent via Telegram/browser within 60s -> delete it
2. ZIP Cleaner - if a ZIP is extracted (folder with same name appears) -> delete the ZIP
"""

import time
import threading
import psutil
from pathlib import Path
from datetime import datetime
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

SCREENSHOT_FOLDER = Path("D:/Screenshots/Screenshots")

ZIP_WATCH_FOLDERS = [
    Path.home() / "Desktop",
    Path.home() / "Downloads",
    Path.home() / "Documents",
]

ARCHIVE_EXTENSIONS = {".zip", ".rar", ".7z", ".tar", ".gz"}

SCREENSHOT_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"}

SENDING_APPS = {
    "telegram.exe",
    "chrome.exe",
    "firefox.exe",
    "msedge.exe",
    "opera.exe",
    "brave.exe",
    "chromium.exe",
}

MONITOR_SECONDS = 60


def log(message):
    timestamp = datetime.now().strftime("%H:%M:%S")
    print(f"[{timestamp}] {message}", flush=True)


# ── Screenshot cleaner ────────────────────────────────────────────────────────

def is_file_opened_by_sender(filepath):
    target = str(filepath).lower()
    for proc in psutil.process_iter(["name", "open_files"]):
        try:
            if proc.info["name"].lower() not in SENDING_APPS:
                continue
            for f in proc.info["open_files"] or []:
                if f.path.lower() == target:
                    return True
        except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
            pass
    return False


def monitor_screenshot(filepath):
    filepath = Path(filepath)
    deadline = time.time() + MONITOR_SECONDS

    while time.time() < deadline:
        if not filepath.exists():
            return
        if is_file_opened_by_sender(filepath):
            time.sleep(3)
            try:
                if filepath.exists():
                    filepath.unlink()
                    log(f"SCREENSHOT DELETED (sent): {filepath.name}")
            except OSError as e:
                log(f"ERROR deleting screenshot {filepath.name}: {e}")
            return
        time.sleep(1)

    log(f"SCREENSHOT KEPT (not sent): {filepath.name}")


class ScreenshotHandler(FileSystemEventHandler):
    def on_created(self, event):
        if event.is_directory:
            return
        filepath = Path(event.src_path)
        if filepath.suffix.lower() in SCREENSHOT_EXTENSIONS:
            log(f"New screenshot: {filepath.name}")
            threading.Thread(target=monitor_screenshot, args=(filepath,), daemon=True).start()


# ── ZIP cleaner ───────────────────────────────────────────────────────────────

def try_delete_archive(archive_path):
    """Wait briefly then delete archive if matching folder exists."""
    archive_path = Path(archive_path)
    time.sleep(5)  # wait for extraction to finish

    # Check for folder with same stem name in same directory
    extracted = archive_path.parent / archive_path.stem
    if not extracted.exists():
        # Also check for folder without spaces/underscores variations
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

        # Look for an archive with the same name as the new folder
        for ext in ARCHIVE_EXTENSIONS:
            archive = parent / (new_folder.name + ext)
            if archive.exists():
                log(f"Extracted folder detected: {new_folder.name}/ -> will delete {archive.name}")
                threading.Thread(target=try_delete_archive, args=(archive,), daemon=True).start()


# ── Main ──────────────────────────────────────────────────────────────────────

def run():
    observers = []

    # Screenshot watcher
    if not SCREENSHOT_FOLDER.exists():
        SCREENSHOT_FOLDER.mkdir(parents=True, exist_ok=True)
    obs_screenshot = Observer()
    obs_screenshot.schedule(ScreenshotHandler(), str(SCREENSHOT_FOLDER), recursive=True)
    obs_screenshot.start()
    observers.append(obs_screenshot)
    log(f"Screenshots watching: {SCREENSHOT_FOLDER}")

    # ZIP watcher
    for folder in ZIP_WATCH_FOLDERS:
        if folder.exists():
            obs_zip = Observer()
            obs_zip.schedule(ZipCleanerHandler(), str(folder), recursive=False)
            obs_zip.start()
            observers.append(obs_zip)
            log(f"ZIP watching: {folder}")

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
