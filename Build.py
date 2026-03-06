# build.py
"""
Build script for NotyCaption
- First: encrypt client.json → client.notycapz
- Then: build single-file EXE named NotyCaption.exe
- Includes client.notycapz inside the bundle
- Main script should be renamed to main.py before running this
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

EXE_NAME        = "NotyCaption"             # final .exe name
MAIN_SCRIPT     = "main.py"                 # your renamed app code
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

# ────────────────────────────────────────────────
# ENCRYPTION
# ────────────────────────────────────────────────

def generate_or_load_key(key_path="build-secret.key"):
    if os.path.exists(key_path):
        with open(key_path, "rb") as f:
            return f.read()
    key = Fernet.generate_key()
    with open(key_path, "wb") as f:
        f.write(key)
    print(f"[KEY] Created new key → {key_path}   KEEP THIS SAFE!")
    return key


def encrypt_client_file():
    if not os.path.isfile(CLIENT_JSON_SRC):
        print(f"[ERROR] {CLIENT_JSON_SRC} not found!")
        print("You must place Google client.json in this folder.")
        sys.exit(1)

    key = generate_or_load_key()
    fernet = Fernet(key)

    with open(CLIENT_JSON_SRC, "r", encoding="utf-8") as f:
        data = json.load(f)

    json_bytes = json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")
    encrypted = fernet.encrypt(json_bytes)
    encoded = base64.b85encode(encrypted).decode("ascii")

    with open(ENCRYPTED_FILE, "w", encoding="ascii") as f:
        f.write(encoded)

    print(f"[OK] Encrypted {CLIENT_JSON_SRC} → {ENCRYPTED_FILE}")


# ────────────────────────────────────────────────
# PYINSTALLER BUILD
# ────────────────────────────────────────────────

def clean_previous():
    for folder in TEMP_FOLDERS:
        if os.path.exists(folder):
            print(f"[CLEAN] Removing {folder}/")
            try:
                shutil.rmtree(folder, ignore_errors=True)
            except Exception as e:
                print(f"  Could not remove {folder} → {e}")


def run_pyinstaller():
    print("\n" + "═"*90)
    print(f" BUILDING {EXE_NAME}.exe ... ".center(90))
    print("═"*90 + "\n")

    cmd = [
        "pyinstaller",
        "--onefile",
        "--windowed",
        "--clean",
        "--noupx",
        "--log-level=DEBUG",
        f"--icon={ICON_FILE}",
        f"--name={EXE_NAME}",

        # Embed icon and encrypted secrets
        "--add-data", f"{ICON_FILE};.",
        "--add-data", f"{ENCRYPTED_FILE};.",

        # Important collections & hidden imports
        "--collect-all", "imageio",
        "--collect-all", "imageio_ffmpeg",
        "--collect-all", "moviepy",
        "--collect-all", "whisper",
        "--collect-all", "tqdm",
        "--collect-all", "googleapiclient",

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

    print("Command:")
    print(" ".join(cmd))
    print()

    try:
        subprocess.run(cmd, check=True)
        print("\nPyInstaller finished.")
    except subprocess.CalledProcessError as e:
        print("\nPyInstaller FAILED!")
        print(e.stdout.decode(errors="replace"))
        if e.stderr:
            print(e.stderr.decode(errors="replace"))
        sys.exit(1)


def copy_release():
    os.makedirs(RELEASE_FOLDER, exist_ok=True)

    files = [
        (f"dist/{EXE_NAME}.exe",          f"{RELEASE_FOLDER}/{EXE_NAME}.exe"),
        (ENCRYPTED_FILE,                  f"{RELEASE_FOLDER}/{ENCRYPTED_FILE}"),
        (ICON_FILE,                       f"{RELEASE_FOLDER}/{ICON_FILE}"),
        ("build-secret.key",              f"{RELEASE_FOLDER}/build-secret.key"),
    ]

    for src, dst in files:
        if os.path.exists(src):
            shutil.copy2(src, dst)
            print(f"[COPY] {dst}")
        else:
            print(f"[WARN] {src} not found")


def cleanup_temp():
    print("\nCleaning temporary folders...")
    for folder in TEMP_FOLDERS:
        if os.path.exists(folder):
            try:
                shutil.rmtree(folder)
                print(f"[RM] {folder}/")
            except:
                print(f"[FAIL] Could not delete {folder}")


def print_result():
    print("\n" + "═"*100)
    print(" BUILD FINISHED ".center(100, "═"))
    print("═"*100)
    print(f"Release folder : {RELEASE_FOLDER}")
    print(f"EXE            : {RELEASE_FOLDER}/{EXE_NAME}.exe")
    print(f"Encrypted file : {RELEASE_FOLDER}/{ENCRYPTED_FILE}  (also inside EXE)")
    print(f"Key file       : {RELEASE_FOLDER}/build-secret.key   ← KEEP SAFE!")
    print("\nRun the EXE → it should now detect and use the encrypted client file.\n")


def main():
    print("NotyCaption Build Tool - Fixed version\n")
    print(f"Started: {datetime.datetime.now():%Y-%m-%d %H:%M:%S}\n")

    # Step 1: Encrypt
    encrypt_client_file()

    # Step 2: Clean
    clean_previous()

    # Step 3: Build
    run_pyinstaller()

    # Step 4: Copy to release folder
    copy_release()

    # Step 5: Clean temp
    cleanup_temp()

    print_result()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nBuild aborted.")
        sys.exit(1)
    except Exception as e:
        print(f"\nBuild failed: {type(e).__name__}: {e}")
        sys.exit(1)