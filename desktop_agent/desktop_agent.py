"""
Desktop Agent - автоматичний прибиральник робочого столу Windows.
Сортує файли, видаляє дублікати, перейменовує та сповіщає про безлад.
"""

import os
import shutil
import hashlib
import json
import sys
from pathlib import Path
from datetime import datetime
from collections import defaultdict

try:
    from plyer import notification
    NOTIFICATIONS_AVAILABLE = True
except ImportError:
    NOTIFICATIONS_AVAILABLE = False

CONFIG_PATH = Path(__file__).parent / "config.json"

DEFAULT_CONFIG = {
    "desktop_path": "",
    "mess_threshold": 10,
    "sort_files": True,
    "remove_duplicates": True,
    "rename_files": True,
    "log_enabled": True,
    "categories": {
        "Зображення": [".jpg", ".jpeg", ".png", ".gif", ".bmp", ".svg", ".webp", ".ico", ".tiff", ".raw"],
        "Документи": [".pdf", ".doc", ".docx", ".txt", ".xls", ".xlsx", ".ppt", ".pptx", ".odt", ".rtf", ".csv"],
        "Відео": [".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".webm", ".m4v"],
        "Музика": [".mp3", ".wav", ".flac", ".aac", ".ogg", ".wma", ".m4a"],
        "Архіви": [".zip", ".rar", ".7z", ".tar", ".gz", ".bz2"],
        "Код": [".py", ".js", ".ts", ".html", ".css", ".java", ".cpp", ".c", ".cs", ".php", ".json", ".xml", ".yaml", ".yml"],
        "Програми": [".exe", ".msi", ".bat", ".cmd", ".ps1"],
        "Інше": []
    }
}


def load_config():
    if CONFIG_PATH.exists():
        with open(CONFIG_PATH, encoding="utf-8") as f:
            cfg = json.load(f)
        for key, val in DEFAULT_CONFIG.items():
            cfg.setdefault(key, val)
        return cfg
    return DEFAULT_CONFIG.copy()


def get_desktop_path(config):
    if config.get("desktop_path"):
        return Path(config["desktop_path"])
    return Path.home() / "Desktop"


def get_file_category(file_path, categories):
    ext = file_path.suffix.lower()
    for category, extensions in categories.items():
        if ext in extensions:
            return category
    return "Інше"


def get_file_hash(file_path):
    hasher = hashlib.md5()
    try:
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(8192), b""):
                hasher.update(chunk)
        return hasher.hexdigest()
    except (PermissionError, OSError):
        return None


def find_duplicates(desktop_path):
    hash_map = defaultdict(list)
    for file_path in desktop_path.rglob("*"):
        if file_path.is_file() and not file_path.name.startswith("."):
            file_hash = get_file_hash(file_path)
            if file_hash:
                hash_map[file_hash].append(file_path)
    return {h: paths for h, paths in hash_map.items() if len(paths) > 1}


def remove_duplicates(duplicates, log):
    removed = 0
    for paths in duplicates.values():
        # Keep oldest file, remove the rest
        paths_sorted = sorted(paths, key=lambda p: p.stat().st_ctime)
        for dup_path in paths_sorted[1:]:
            try:
                dup_path.unlink()
                log.append(f"[ВИДАЛЕНО дублікат] {dup_path.name}")
                removed += 1
            except (PermissionError, OSError) as e:
                log.append(f"[ПОМИЛКА] Не вдалося видалити {dup_path.name}: {e}")
    return removed


def sort_files(desktop_path, categories, log):
    sorted_count = 0
    for file_path in list(desktop_path.iterdir()):
        if file_path.is_file() and not file_path.name.startswith("."):
            category = get_file_category(file_path, categories)
            target_dir = desktop_path / category
            target_dir.mkdir(exist_ok=True)

            target_path = target_dir / file_path.name
            counter = 1
            while target_path.exists():
                target_path = target_dir / f"{file_path.stem}_{counter}{file_path.suffix}"
                counter += 1

            try:
                shutil.move(str(file_path), str(target_path))
                log.append(f"[ПЕРЕМІЩЕНО] {file_path.name} → {category}/")
                sorted_count += 1
            except (PermissionError, OSError) as e:
                log.append(f"[ПОМИЛКА] Не вдалося перемістити {file_path.name}: {e}")
    return sorted_count


def rename_files(desktop_path, log):
    renamed = 0
    for file_path in list(desktop_path.rglob("*")):
        if file_path.is_file():
            clean_stem = "".join(
                c if c.isalnum() or c in "._-" else "_"
                for c in file_path.stem.strip()
            )
            # Collapse multiple underscores
            while "__" in clean_stem:
                clean_stem = clean_stem.replace("__", "_")
            clean_stem = clean_stem.strip("_")
            new_name = clean_stem + file_path.suffix.lower()

            if new_name != file_path.name:
                new_path = file_path.parent / new_name
                if not new_path.exists():
                    try:
                        file_path.rename(new_path)
                        log.append(f"[ПЕРЕЙМЕНОВАНО] {file_path.name} → {new_name}")
                        renamed += 1
                    except (PermissionError, OSError) as e:
                        log.append(f"[ПОМИЛКА] Не вдалося перейменувати {file_path.name}: {e}")
    return renamed


def send_notification(title, message):
    if NOTIFICATIONS_AVAILABLE:
        notification.notify(
            title=title,
            message=message,
            app_name="Desktop Agent",
            timeout=10
        )
    print(f"[СПОВІЩЕННЯ] {title}: {message}")


def count_desktop_files(desktop_path):
    return sum(1 for f in desktop_path.iterdir() if f.is_file())


def save_log(log, desktop_path):
    log_file = desktop_path.parent / "Desktop_Agent_Log.txt"
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(log_file, "a", encoding="utf-8") as f:
        f.write(f"\n{'=' * 50}\n")
        f.write(f"Запуск: {timestamp}\n")
        f.write(f"{'=' * 50}\n")
        for entry in log:
            f.write(entry + "\n")
    return log_file


def run_agent():
    config = load_config()
    desktop_path = get_desktop_path(config)
    categories = config["categories"]
    log = []

    print(f"Desktop Agent | {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"Стіл: {desktop_path}\n")

    if not desktop_path.exists():
        print(f"[ПОМИЛКА] Шлях не знайдено: {desktop_path}")
        sys.exit(1)

    file_count_before = count_desktop_files(desktop_path)

    if file_count_before >= config["mess_threshold"]:
        send_notification(
            "Desktop Agent — Безлад!",
            f"На робочому столі {file_count_before} файлів. Починаю прибирання..."
        )

    # Step 1: Remove duplicates
    if config["remove_duplicates"]:
        print("1. Пошук дублікатів...")
        duplicates = find_duplicates(desktop_path)
        removed = remove_duplicates(duplicates, log) if duplicates else 0
        print(f"   Видалено {removed} дублікатів")
    else:
        removed = 0

    # Step 2: Rename files
    if config["rename_files"]:
        print("2. Перейменування файлів...")
        renamed = rename_files(desktop_path, log)
        print(f"   Перейменовано {renamed} файлів")
    else:
        renamed = 0

    # Step 3: Sort files
    if config["sort_files"]:
        print("3. Сортування файлів...")
        sorted_count = sort_files(desktop_path, categories, log)
        print(f"   Відсортовано {sorted_count} файлів")
    else:
        sorted_count = 0

    file_count_after = count_desktop_files(desktop_path)

    send_notification(
        "Desktop Agent — Готово!",
        f"Відсортовано: {sorted_count}, дублікатів видалено: {removed}, перейменовано: {renamed}"
    )

    print(f"\nФайлів до: {file_count_before} | після: {file_count_after}")

    if config["log_enabled"]:
        log_file = save_log(log, desktop_path)
        print(f"Лог: {log_file}")


if __name__ == "__main__":
    run_agent()
