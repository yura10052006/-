"""Створює тестовий WAV 8 кГц моно 16-bit (тиша) для перевірки маршруту /command.

Використання: python scripts/make_test_wav.py [секунди] [вихідний_файл]
За замовчуванням: 2 секунди -> test_8k.wav
"""
import struct
import sys
import wave

SAMPLE_RATE = 8000  # 8 кГц -- навмисно, як мікрофон окулярів (spec NFR-2)


def make_silence_wav(path: str, seconds: float = 2.0) -> None:
    n = int(SAMPLE_RATE * seconds)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)       # моно
        w.setsampwidth(2)       # 16-bit
        w.setframerate(SAMPLE_RATE)
        w.writeframes(struct.pack("<" + "h" * n, *([0] * n)))
    print(f"Створено {path}: {seconds}s, {SAMPLE_RATE} Гц, моно, 16-bit")


if __name__ == "__main__":
    seconds = float(sys.argv[1]) if len(sys.argv) > 1 else 2.0
    out = sys.argv[2] if len(sys.argv) > 2 else "test_8k.wav"
    make_silence_wav(out, seconds)
