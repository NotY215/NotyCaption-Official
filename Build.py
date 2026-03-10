# build.py
"""
Build script for NotyCaption Pro - With Spleeter fully bundled
- Encrypts client.json → client.notycapz
- Builds single-file EXE named NotyCaption.exe
- Bundles client.notycapz + key + Spleeter + Whisper + moviepy dependencies
"""

import os
import sys
import json
import base64
import shutil
import subprocess
import datetime
from cryptography.fernet import Fernet

# ────────────────────────────────────────────────
# CONFIG
# ────────────────────────────────────────────────

EXE_NAME        = "NotyCaption"
MAIN_SCRIPT     = "main.py"                 # Your renamed app code
ICON_FILE       = "App.ico"
CLIENT_JSON_SRC = "client.json"
ENCRYPTED_FILE  = "client.notycapz"
KEY_FILE_NAME   = "key.notcapz"

REQUIRED_FILES = [
    MAIN_SCRIPT,
    ICON_FILE,
    CLIENT_JSON_SRC,
]

RELEASE_FOLDER = f"release_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"

TEMP_FOLDERS = ["build", "dist", "__pycache__"]

# PyInstaller command - optimized for Spleeter + Whisper + moviepy + Google
PYINSTALLER_CMD = [
    "pyinstaller",
    "--onefile",
    "--windowed",
    "--clean",
    "--noupx",
    "--log-level=INFO",
    f"--icon={ICON_FILE}",
    f"--name={EXE_NAME}",

    # Bundle resources
    "--add-data", f"{ICON_FILE};.",
    "--add-data", f"{ENCRYPTED_FILE};.",
    "--add-data", f"{KEY_FILE_NAME};.",

    # ─── Spleeter + TensorFlow + Dependencies ───
    "--collect-all", "spleeter",
    "--collect-all", "tensorflow",
    "--collect-all", "numpy",
    "--collect-all", "pandas",          # sometimes used indirectly
    "--collect-all", "scipy",

    "--hidden-import", "spleeter",
    "--hidden-import", "spleeter.separator",
    "--hidden-import", "spleeter.model",
    "--hidden-import", "spleeter.audio",
    "--hidden-import", "spleeter.utils",
    "--hidden-import", "tensorflow",
    "--hidden-import", "tensorflow.python.eager.context",
    "--hidden-import", "numpy",

    # Core dependencies (Whisper, moviepy, imageio, google, tqdm)
    "--collect-all", "imageio",
    "--collect-all", "imageio_ffmpeg",
    "--collect-all", "moviepy",
    "--collect-all", "whisper",
    "--collect-all", "tqdm",
    "--collect-all", "googleapiclient",
    "--collect-all", "google_auth_oauthlib",
    "--collect-all", "google.auth",

    "--hidden-import", "imageio",
    "--hidden-import", "imageio_ffmpeg",
    "--hidden-import", "moviepy.editor",
    "--hidden-import", "tqdm",
    "--hidden-import", "whisper",
    "--hidden-import", "torch",
    "--hidden-import", "pkg_resources.py2_warn",
    "--hidden-import", "importlib.metadata",
    "--hidden-import", "importlib_metadata",
    "--hidden-import", "googleapiclient.discovery",
    "--hidden-import", "googleapiclient.http",
    "--hidden-import", "google.auth.transport.requests",
    "--hidden-import", "google.oauth2.credentials",
    "--hidden-import", "google_auth_oauthlib.flow",

    # Exclude unused heavy modules to reduce size
    "--exclude-module", "tkinter",
    "--exclude-module", "matplotlib",
    "--exclude-module", "PIL",

    MAIN_SCRIPT
]

# ────────────────────────────────────────────────
# ENCRYPTION
# ────────────────────────────────────────────────

def generate_or_load_key(key_path=KEY_FILE_NAME):
    if os.path.exists(key_path):
        with open(key_path, "rb") as f:
            return f.read()
    key = Fernet.generate_key()
    with open(key_path, "wb") as f:
        f.write(key)
    print(f"[KEY] Created fixed key → {key_path}  (SAVE THIS!)")
    return key


def encrypt_client():
    if not os.path.isfile(CLIENT_JSON_SRC):
        print(f"[ERROR] Missing {CLIENT_JSON_SRC}")
        sys.exit(1)

    key = generate_or_load_key()
    fernet = Fernet(key)

    with open(CLIENT_JSON_SRC, "r", encoding="utf-8") as f:
        data = json.load(f)

    json_bytes = json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")
    encrypted = fernet.encrypt(json_bytes)
    encoded = base64.b64encode(encrypted).decode("utf-8")

    with open(ENCRYPTED_FILE, "w", encoding="utf-8") as f:
        f.write(encoded)

    print(f"[OK] Encrypted → {ENCRYPTED_FILE}")


# ────────────────────────────────────────────────
# BUILD FLOW
# ────────────────────────────────────────────────

def check_files():
    missing = [f for f in REQUIRED_FILES if not os.path.isfile(f)]
    if missing:
        print("Missing files:")
        for m in missing:
            print(f"  • {m}")
        sys.exit(1)


def clean_old():
    for d in TEMP_FOLDERS:
        if os.path.exists(d):
            print(f"[CLEAN] Removing {d}/")
            try:
                shutil.rmtree(d, ignore_errors=True)
            except:
                pass


def run_build():
    print("\n" + "═"*90)
    print(f" BUILDING {EXE_NAME}.exe (with Spleeter bundled) ".center(90))
    print("═"*90 + "\n")

    print("Command:")
    print(" ".join(PYINSTALLER_CMD))
    print()

    try:
        subprocess.run(PYINSTALLER_CMD, check=True)
    except subprocess.CalledProcessError as e:
        print("\nPyInstaller FAILED!")
        print(e)
        sys.exit(1)


def copy_release():
    os.makedirs(RELEASE_FOLDER, exist_ok=True)

    files = [
        (f"dist/{EXE_NAME}.exe",    f"{RELEASE_FOLDER}/{EXE_NAME}.exe"),
        (ENCRYPTED_FILE,            f"{RELEASE_FOLDER}/{ENCRYPTED_FILE}"),
        (KEY_FILE_NAME,             f"{RELEASE_FOLDER}/{KEY_FILE_NAME}"),
        (ICON_FILE,                 f"{RELEASE_FOLDER}/{ICON_FILE}"),
    ]

    for src, dst in files:
        if os.path.exists(src):
            shutil.copy2(src, dst)
            print(f"[COPY] {dst}")
        else:
            print(f"[WARN] {src} missing")


def cleanup():
    print("\nCleaning temp folders...")
    for d in TEMP_FOLDERS:
        if os.path.exists(d):
            try:
                shutil.rmtree(d)
                print(f"[RM] {d}/")
            except:
                print(f"[FAIL] Could not delete {d}")


def summary():
    print("\n" + "═"*100)
    print(" BUILD COMPLETE - Spleeter, Whisper, Secrets & Key BUNDLED ".center(100, "═"))
    print("═"*100)
    print(f"Release folder : {RELEASE_FOLDER}")
    print(f"EXE            : {RELEASE_FOLDER}/{EXE_NAME}.exe")
    print(f"Key file       : {RELEASE_FOLDER}/{KEY_FILE_NAME}")
    print("\nRun the EXE → Spleeter enhancement should work without import errors.\n")


def main():
    print("NotyCaption Build Tool - Spleeter Included\n")
    check_files()
    encrypt_client()
    clean_old()
    run_build()
    copy_release()
    cleanup()
    summary()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nAborted.")
        sys.exit(1)
    except Exception as e:
        print(f"\nBuild failed: {type(e).__name__}: {e}")
        sys.exit(1)