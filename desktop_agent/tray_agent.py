"""
Desktop Agent - all-in-one background agent with system tray icon.
Replaces the black console window with a nice tray icon.

Features:
- System tray icon with menu
- Screenshot auto-delete on Ctrl+V paste in Telegram / Chrome
- ZIP auto-delete after extraction
- Downloads folder auto-sort by file type
"""

import sys
import time
import threading
import psutil
from pathlib import Path
from datetime import datetime
from watchdog.observers import Observer
from watchdog.events import FileSystemEventHandler

try:
    import pystray
    from PIL import Image, ImageDraw
    TRAY_AVAILABLE = True
except ImportError:
    TRAY_AVAILABLE = False

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
    from plyer import notification as plyer_notify
    NOTIFY_AVAILABLE = True
except ImportError:
    NOTIFY_AVAILABLE = False


# ── Config ────────────────────────────────────────────────────────────────────

SCREENSHOT_FOLDER = Path("D:/Screenshots/Screenshots")

DOWNLOADS_FOLDER = Path.home() / "Downloads"

ZIP_WATCH_FOLDERS = [
    Path.home() / "Desktop",
    Path.home() / "Downloads",
    Path.home() / "Documents",
]

SCREENSHOT_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp"}
ARCHIVE_EXTENSIONS = {".zip", ".rar", ".7z", ".tar", ".gz"}
DOWNLOAD_INCOMPLETE = {".crdownload", ".part", ".tmp", ".download"}
BROWSER_PROCESSES = {
    "chrome.exe", "msedge.exe", "firefox.exe", "opera.exe", "brave.exe", "chromium.exe"
}

DOWNLOADS_CATEGORIES = {
    "Зображення":  {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".webp", ".ico"},
    "Відео":       {".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm"},
    "Музика":      {".mp3", ".wav", ".flac", ".aac", ".ogg", ".wma"},
    "Документи":   {".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv"},
    "Архіви":      {".zip", ".rar", ".7z", ".tar", ".gz"},
    "Програми":    {".exe", ".msi"},
}

MAX_SCREENSHOT_AGE = 600  # 10 minutes

# ── State ─────────────────────────────────────────────────────────────────────

last_screenshot: Path | None = None
start_time = datetime.now()
stats = {"screenshots": 0, "zips": 0, "downloads": 0}


# ── Helpers ───────────────────────────────────────────────────────────────────

def log(msg):
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}", flush=True)


def notify(title, msg):
    if NOTIFY_AVAILABLE:
        plyer_notify.notify(title=title, message=msg, app_name="Desktop Agent", timeout=5)


def delete_file(path: Path, reason: str):
    try:
        path.unlink()
        log(f"DELETED ({reason}): {path.name}")
        notify("Видалено!", path.name)
    except OSError as e:
        log(f"ERROR: {path.name}: {e}")


def latest_screenshot():
    files = [
        f for f in SCREENSHOT_FOLDER.rglob("*")
        if f.is_file() and f.suffix.lower() in SCREENSHOT_EXTENSIONS
    ]
    return max(files, key=lambda f: f.stat().st_ctime) if files else None


def get_window_info():
    if not WIN32_AVAILABLE:
        return "", ""
    try:
        hwnd = win32gui.GetForegroundWindow()
        title = win32gui.GetWindowText(hwnd).lower()
        _, pid = win32process.GetWindowThreadProcessId(hwnd)
        for p in psutil.process_iter(["pid", "name"]):
            if p.pid == pid:
                return p.name().lower(), title
    except Exception:
        pass
    return "", ""


def clipboard_has_image():
    if not WIN32_AVAILABLE:
        return False
    try:
        win32clipboard.OpenClipboard()
        has = any(
            win32clipboard.IsClipboardFormatAvailable(f)
            for f in [win32clipboard.CF_DIB, win32clipboard.CF_BITMAP, win32clipboard.CF_DIBV5]
        )
        win32clipboard.CloseClipboard()
        return has
    except Exception:
        try:
            win32clipboard.CloseClipboard()
        except Exception:
            pass
        return False


# ── Tray icon ─────────────────────────────────────────────────────────────────

def make_icon():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 62, 62], fill="#1565C0")
    d.rectangle([12, 16, 52, 38], fill="white")
    d.rectangle([27, 38, 37, 44], fill="white")
    d.rectangle([19, 44, 45, 49], fill="white")
    return img


def status_label(icon=None, item=None):
    up = datetime.now() - start_time
    h, r = divmod(int(up.total_seconds()), 3600)
    m = r // 60
    s = stats
    return f"{h}г {m}хв | Скрін: {s['screenshots']}  ZIP: {s['zips']}  DL: {s['downloads']}"


def on_manual_delete(icon=None, item=None):
    global last_screenshot
    target = last_screenshot or latest_screenshot()
    if not target or not target.exists():
        log("No screenshot to delete")
        return
    if time.time() - target.stat().st_ctime <= MAX_SCREENSHOT_AGE:
        delete_file(target, "manual Ctrl+Shift+D")
        stats["screenshots"] += 1
        last_screenshot = None
    else:
        log("Screenshot too old")


def on_quit(icon, item):
    icon.stop()
    sys.exit(0)


def start_tray():
    icon = pystray.Icon(
        "DesktopAgent",
        make_icon(),
        "Desktop Agent",
        pystray.Menu(
            pystray.MenuItem(status_label, status_label, enabled=False),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Видалити скріншот  (Ctrl+Shift+D)", on_manual_delete),
            pystray.Menu.SEPARATOR,
            pystray.MenuItem("Вийти", on_quit),
        ),
    )
    icon.run()


# ── Screenshot cleaner ────────────────────────────────────────────────────────

class ScreenshotHandler(FileSystemEventHandler):
    def on_created(self, event):
        global last_screenshot
        if event.is_directory:
            return
        p = Path(event.src_path)
        if p.suffix.lower() in SCREENSHOT_EXTENSIONS:
            last_screenshot = p
            log(f"Screenshot: {p.name}")


def on_paste():
    proc, _ = get_window_info()
    if proc != "telegram.exe" and proc not in BROWSER_PROCESSES:
        return
    if not clipboard_has_image():
        return
    target = last_screenshot or latest_screenshot()
    if not target or not target.exists():
        return
    if time.time() - target.stat().st_ctime > MAX_SCREENSHOT_AGE:
        return

    def delayed():
        time.sleep(1.5)
        delete_file(target, "Ctrl+V paste")
        stats["screenshots"] += 1

    threading.Thread(target=delayed, daemon=True).start()


# ── ZIP cleaner ───────────────────────────────────────────────────────────────

class ZipCleanerHandler(FileSystemEventHandler):
    def on_created(self, event):
        if not event.is_directory:
            return
        folder = Path(event.src_path)
        for ext in ARCHIVE_EXTENSIONS:
            arch = folder.parent / (folder.name + ext)
            if arch.exists():
                log(f"Extracted folder: {folder.name}/ → will delete {arch.name}")
                threading.Thread(target=self._delayed_delete, args=(arch,), daemon=True).start()

    @staticmethod
    def _delayed_delete(path: Path):
        time.sleep(5)
        extracted = path.parent / path.stem
        if extracted.exists() and path.exists():
            delete_file(path, "extracted")
            stats["zips"] += 1


# ── Downloads auto-sorter ─────────────────────────────────────────────────────

class DownloadsHandler(FileSystemEventHandler):
    def on_created(self, event):
        if event.is_directory:
            return
        threading.Thread(
            target=self._wait_and_sort, args=(Path(event.src_path),), daemon=True
        ).start()

    @staticmethod
    def _wait_and_sort(filepath: Path):
        # Wait until download finishes (file size stabilizes)
        prev_size = -1
        for _ in range(60):
            time.sleep(2)
            if not filepath.exists():
                return
            if filepath.suffix.lower() in DOWNLOAD_INCOMPLETE:
                continue
            try:
                size = filepath.stat().st_size
            except OSError:
                return
            if size == prev_size and size > 0:
                break
            prev_size = size
        else:
            return

        ext = filepath.suffix.lower()
        if ext in DOWNLOAD_INCOMPLETE:
            return

        category = next(
            (cat for cat, exts in DOWNLOADS_CATEGORIES.items() if ext in exts),
            "Інше",
        )
        target_dir = DOWNLOADS_FOLDER / category
        target_dir.mkdir(exist_ok=True)

        dest = target_dir / filepath.name
        counter = 1
        while dest.exists():
            dest = target_dir / f"{filepath.stem}_{counter}{filepath.suffix}"
            counter += 1

        try:
            filepath.rename(dest)
            log(f"SORTED: {filepath.name} → Downloads/{category}/")
            stats["downloads"] += 1
        except OSError as e:
            log(f"ERROR sorting {filepath.name}: {e}")


# ── Main ──────────────────────────────────────────────────────────────────────

def run():
    observers = []

    SCREENSHOT_FOLDER.mkdir(parents=True, exist_ok=True)
    obs = Observer()
    obs.schedule(ScreenshotHandler(), str(SCREENSHOT_FOLDER), recursive=True)
    obs.start()
    observers.append(obs)
    log(f"Screenshots: {SCREENSHOT_FOLDER}")

    for folder in ZIP_WATCH_FOLDERS:
        if folder.exists():
            o = Observer()
            o.schedule(ZipCleanerHandler(), str(folder), recursive=False)
            o.start()
            observers.append(o)
    log("ZIP cleaner: Desktop / Downloads / Documents")

    if DOWNLOADS_FOLDER.exists():
        o = Observer()
        o.schedule(DownloadsHandler(), str(DOWNLOADS_FOLDER), recursive=False)
        o.start()
        observers.append(o)
        log(f"Downloads sorter: {DOWNLOADS_FOLDER}")

    if KEYBOARD_AVAILABLE:
        if WIN32_AVAILABLE:
            keyboard.add_hotkey("ctrl+v", on_paste, suppress=False)
        keyboard.add_hotkey("ctrl+shift+d", on_manual_delete)
        log("Hotkeys: Ctrl+V (auto) | Ctrl+Shift+D (manual delete)")

    if TRAY_AVAILABLE:
        log("Starting tray icon (right-click the icon in the taskbar)")
        start_tray()  # blocks until Quit
    else:
        log("pystray not installed - running in console mode. Press Ctrl+C to stop.")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            pass

    for o in observers:
        o.stop()
    for o in observers:
        o.join()
    log("Agent stopped.")


if __name__ == "__main__":
    run()
