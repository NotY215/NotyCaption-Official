# build.py
"""
FINAL Build script for NotyCaption - March 2026
- Encrypts client.json → client.notycapz using base64 (consistent with decrypt)
- Builds single-file EXE named NotyCaption.exe
- Bundles client.notycapz inside the EXE (accessible via _MEIPASS)
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

EXE_NAME        = "NotyCaption"             # Final EXE name
MAIN_SCRIPT     = "main.py"                 # Your renamed app code
ICON_FILE       = "App.ico"
CLIENT_JSON_SRC = "client.json"
ENCRYPTED_FILE  = "client.notycapz"

REQUIRED_FILES = [
    MAIN_SCRIPT,
    ICON_FILE,
    CLIENT_JSON_SRC,
]

RELEASE_FOLDER = f"release_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"

TEMP_FOLDERS = ["build", "dist", "__pycache__"]

# PyInstaller command
PYINSTALLER_CMD = [
    "pyinstaller",
    "--onefile",
    "--windowed",
    "--clean",
    "--noupx",
    "--log-level=INFO",
    f"--icon={ICON_FILE}",
    f"--name={EXE_NAME}",

    # Bundle icon + encrypted secrets
    "--add-data", f"{ICON_FILE};.",
    "--add-data", f"{ENCRYPTED_FILE};.",

    # Collect important packages
    "--collect-all", "imageio",
    "--collect-all", "imageio_ffmpeg",
    "--collect-all", "moviepy",
    "--collect-all", "whisper",
    "--collect-all", "tqdm",

    "--hidden-import", "imageio",
    "--hidden-import", "imageio_ffmpeg",
    "--hidden-import", "moviepy.editor",
    "--hidden-import", "tqdm",
    "--hidden-import", "whisper",
    "--hidden-import", "googleapiclient.discovery",
    "--hidden-import", "googleapiclient.http",
    "--hidden-import", "google.auth.transport.requests",
    "--hidden-import", "google.oauth2.credentials",
    "--hidden-import", "google_auth_oauthlib.flow",

    MAIN_SCRIPT
]

# ────────────────────────────────────────────────
# ENCRYPTION (using base64 - consistent with decrypt_data)
# ────────────────────────────────────────────────

def generate_or_load_key(key_path="build-key.key"):
    if os.path.exists(key_path):
        with open(key_path, "rb") as f:
            return f.read()
    key = Fernet.generate_key()
    with open(key_path, "wb") as f:
        f.write(key)
    print(f"[KEY] Created → {key_path}  (SAVE THIS!)")
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
    encoded = base64.b64encode(encrypted).decode("utf-8")  # ← b64, not b85

    with open(ENCRYPTED_FILE, "w", encoding="utf-8") as f:
        f.write(encoded)

    print(f"[OK] Encrypted → {ENCRYPTED_FILE}")


# ────────────────────────────────────────────────
# BUILD STEPS
# ────────────────────────────────────────────────

def check_files():
    missing = [f for f in REQUIRED_FILES if not os.path.isfile(f)]
    if missing:
        print("Missing:")
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
    print("\n" + "═"*80)
    print(f" BUILDING {EXE_NAME}.exe ".center(80))
    print("═"*80 + "\n")

    print("Command:")
    print(" ".join(PYINSTALLER_CMD))
    print()

    try:
        subprocess.run(PYINSTALLER_CMD, check=True)
    except subprocess.CalledProcessError as e:
        print("\nBuild FAILED!")
        print(e.stdout.decode(errors="replace"))
        sys.exit(1)


def copy_release():
    os.makedirs(RELEASE_FOLDER, exist_ok=True)

    files = [
        (f"dist/{EXE_NAME}.exe",          f"{RELEASE_FOLDER}/{EXE_NAME}.exe"),
        (ENCRYPTED_FILE,                  f"{RELEASE_FOLDER}/{ENCRYPTED_FILE}"),
        (ICON_FILE,                       f"{RELEASE_FOLDER}/{ICON_FILE}"),
        ("build-key.key",                 f"{RELEASE_FOLDER}/build-key.key"),
    ]

    for src, dst in files:
        if os.path.exists(src):
            shutil.copy2(src, dst)
            print(f"[COPY] {dst}")
        else:
            print(f"[WARN] Not found: {src}")


def cleanup():
    print("\nCleaning temp...")
    for d in TEMP_FOLDERS:
        if os.path.exists(d):
            try:
                shutil.rmtree(d)
                print(f"[RM] {d}/")
            except:
                print(f"[FAIL] Could not delete {d}")


def summary():
    print("\n" + "═"*90)
    print(" BUILD COMPLETE - client.notycapz IS BUNDLED INSIDE EXE ".center(90, "═"))
    print("═"*90)
    print(f"Folder: {RELEASE_FOLDER}")
    print(f"EXE   : {RELEASE_FOLDER}/{EXE_NAME}.exe")
    print(f"Key   : {RELEASE_FOLDER}/build-key.key")
    print("\nNow run NotyCaption.exe → it should detect EXE (encrypted) mode and decrypt successfully.\n")


def main():
    print("NotyCaption Build - Fixed Encryption & Bundling\n")
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
        print(f"\nBuild crashed: {type(e).__name__}: {e}")
        sys.exit(1)