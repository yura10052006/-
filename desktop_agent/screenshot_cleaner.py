"""
Screenshot Cleaner Agent
Watches D:\Screenshots\Screenshots.
If a screenshot is opened by Telegram or a browser within 60 seconds -> delete it.
Otherwise -> keep it.
"""

import os
import sys
import time
import threading
import psutil
from pathlib import Path
from datetime import datetime
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

SCREENSHOT_FOLDER = Path("D:/Screenshots/Screenshots")

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


def is_file_opened_by_sender(filepath):
    """Return True if any sending app currently has this file open."""
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
    """Watch screenshot for MONITOR_SECONDS. Delete it if sent."""
    filepath = Path(filepath)
    deadline = time.time() + MONITOR_SECONDS

    while time.time() < deadline:
        if not filepath.exists():
            return

        if is_file_opened_by_sender(filepath):
            time.sleep(3)  # wait for upload to finish
            try:
                if filepath.exists():
                    filepath.unlink()
                    log(f"DELETED (sent): {filepath.name}")
            except OSError as e:
                log(f"ERROR: could not delete {filepath.name}: {e}")
            return

        time.sleep(1)

    log(f"KEPT (not sent): {filepath.name}")


class ScreenshotHandler(FileSystemEventHandler):
    def on_created(self, event):
        if event.is_directory:
            return
        filepath = Path(event.src_path)
        if filepath.suffix.lower() in SCREENSHOT_EXTENSIONS:
            log(f"New screenshot detected: {filepath.name}")
            thread = threading.Thread(
                target=monitor_screenshot,
                args=(filepath,),
                daemon=True,
            )
            thread.start()


def run():
    if not SCREENSHOT_FOLDER.exists():
        SCREENSHOT_FOLDER.mkdir(parents=True, exist_ok=True)
        log(f"Created folder: {SCREENSHOT_FOLDER}")

    observer = Observer()
    observer.schedule(ScreenshotHandler(), str(SCREENSHOT_FOLDER), recursive=True)
    observer.start()
    log(f"Watching: {SCREENSHOT_FOLDER}")
    log("Agent running. Press Ctrl+C to stop.")

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        observer.stop()
        observer.join()
        log("Agent stopped.")


if __name__ == "__main__":
    run()
