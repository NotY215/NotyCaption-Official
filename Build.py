# build.py
"""
Build script for NotyCaption Pro
- Encrypts client.json → client.notycapz
- Builds single-file EXE with PyInstaller
- Copies essential files to dated release folder
- Deletes build/ and dist/ folders after successful copy
"""

import os
import sys
import json
import base64
import shutil
import subprocess
import datetime
from pathlib import Path
from cryptography.fernet import Fernet

# ────────────────────────────────────────────────
#  CONFIGURATION
# ────────────────────────────────────────────────

APP_NAME          = "NotyCaption"
MAIN_SCRIPT       = "main.py"
ICON_FILE         = "App.ico"

# Files that must exist next to this script
REQUIRED_FILES = [
    MAIN_SCRIPT,
    ICON_FILE,
    "client.json",           # Google API credentials
]

# Folders that will be cleaned up after successful build & copy
TEMP_FOLDERS_TO_DELETE = ["build", "dist"]

# Output versioned folder name
RELEASE_FOLDER = f"release_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"

# PyInstaller command - 2025/2026 recommended flags for moviepy + imageio
PYINSTALLER_CMD = [
    "pyinstaller",
    "--onefile",
    "--windowed",
    "--clean",
    "--noupx",
    f"--icon={ICON_FILE}",
    f"--name={APP_NAME}",
    "--add-data", f"{ICON_FILE};.",

    # Very important for moviepy + imageio + ffmpeg issues
    "--hidden-import", "imageio",
    "--collect-data", "imageio",
    "--hidden-import", "imageio_ffmpeg",
    "--hidden-import", "moviepy",
    "--hidden-import", "moviepy.editor",
    "--hidden-import", "pkg_resources.py2_warn",

    # Your existing hidden imports
    "--hidden-import", "google.auth.transport.requests",
    "--hidden-import", "google.oauth2.credentials",
    "--hidden-import", "google_auth_oauthlib.flow",
    "--hidden-import", "googleapiclient.discovery",
    "--hidden-import", "googleapiclient.http",
    "--hidden-import", "pysrt",
    "--hidden-import", "pysubs2",
    "--hidden-import", "spleeter",
    "--hidden-import", "whisper",

    MAIN_SCRIPT
]

# ────────────────────────────────────────────────
#  ENCRYPTION HELPERS
# ────────────────────────────────────────────────

def generate_or_load_key(key_path="build_key.key"):
    if os.path.exists(key_path):
        with open(key_path, "rb") as f:
            return f.read()

    key = Fernet.generate_key()
    with open(key_path, "wb") as f:
        f.write(key)
    print(f"→ Created new encryption key: {key_path}")
    print("   !! KEEP THIS FILE SAFE !!")
    return key


def encrypt_client_json(key: bytes, input_file="client.json", output_file="client.notycapz"):
    if not os.path.isfile(input_file):
        print(f"ERROR: {input_file} not found!")
        print("Place your Google client.json in this folder.")
        sys.exit(1)

    with open(input_file, "r", encoding="utf-8") as f:
        data = json.load(f)

    fernet = Fernet(key)
    json_str = json.dumps(data, ensure_ascii=False, indent=2)
    encrypted = fernet.encrypt(json_str.encode("utf-8"))
    encoded = base64.b85encode(encrypted).decode("ascii")

    with open(output_file, "w", encoding="ascii") as f:
        f.write(encoded)

    print(f"→ Encrypted → {output_file}")


# ────────────────────────────────────────────────
#  BUILD & CLEANUP
# ────────────────────────────────────────────────

def check_prerequisites():
    missing = [f for f in REQUIRED_FILES if not os.path.isfile(f)]
    if missing:
        print("Missing files:")
        for f in missing:
            print(f"  • {f}")
        sys.exit(1)


def clean_previous_builds():
    for folder in TEMP_FOLDERS_TO_DELETE:
        if os.path.exists(folder):
            print(f"→ Removing old {folder}/")
            try:
                shutil.rmtree(folder, ignore_errors=True)
            except Exception as e:
                print(f"  Could not remove {folder} → {e}")


def run_pyinstaller():
    print("\n" + "═"*75)
    print(" Starting PyInstaller build ...")
    print("═"*75 + "\n")

    try:
        result = subprocess.run(
            PYINSTALLER_CMD,
            check=True,
            capture_output=True,
            text=True
        )
        print(result.stdout)
    except subprocess.CalledProcessError as e:
        print("PyInstaller failed!")
        print(e.stdout)
        if e.stderr:
            print(e.stderr)
        sys.exit(1)


def copy_release_files():
    os.makedirs(RELEASE_FOLDER, exist_ok=True)

    files_to_copy = [
        (f"dist/{APP_NAME}.exe",          f"{RELEASE_FOLDER}/{APP_NAME}.exe"),
        ("client.notycapz",               f"{RELEASE_FOLDER}/client.notycapz"),
        (ICON_FILE,                       f"{RELEASE_FOLDER}/{ICON_FILE}"),
        ("build_key.key",                 f"{RELEASE_FOLDER}/build_key.key"),
    ]

    for src, dst in files_to_copy:
        if os.path.exists(src):
            shutil.copy2(src, dst)
            print(f"→ Copied → {dst}")
        else:
            print(f"Warning: {src} not found")


def cleanup_temp_folders():
    print("\nCleaning temporary folders...")
    for folder in TEMP_FOLDERS_TO_DELETE:
        if os.path.exists(folder):
            try:
                shutil.rmtree(folder)
                print(f"→ Removed: {folder}/")
            except Exception as e:
                print(f"  Could not delete {folder} → {e}")


def print_summary():
    print("\n" + "═"*80)
    print(" BUILD FINISHED ".center(80, "═"))
    print("═"*80)
    print(f"Release folder : {RELEASE_FOLDER}")
    print(f"Executable     : {RELEASE_FOLDER}/{APP_NAME}.exe")
    print(f"Encrypted auth : {RELEASE_FOLDER}/client.notycapz")
    print(f"Encryption key : {RELEASE_FOLDER}/build_key.key   ← KEEP SAFE!")
    print("\nDone.\n")


def main():
    print("NotyCaption Pro - Build Tool\n")
    print(f"Started: {datetime.datetime.now():%Y-%m-%d %H:%M:%S}\n")

    check_prerequisites()
    clean_previous_builds()

    key = generate_or_load_key()
    encrypt_client_json(key)

    run_pyinstaller()
    copy_release_files()
    cleanup_temp_folders()

    print_summary()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nBuild aborted.")
        sys.exit(1)
    except Exception as e:
        print(f"\nBuild failed:\n{type(e).__name__}: {e}")
        sys.exit(1)