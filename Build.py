# build.py
"""
FINAL Build script for NotyCaption Pro - charset_normalizer mypyc FIXED
Builds NotyCaption.exe with Spleeter working (no 81d243bd...__mypyc error)
Now with full dependency collection for all features including translations and hardware detection
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
MAIN_SCRIPT     = "main.py"                 # Your app must be named main.py
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

# PyInstaller command - Complete dependency collection
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

    # ─── CRITICAL: charset_normalizer pure Python mode (bypass mypyc) ───
    "--collect-all", "charset_normalizer",
    "--collect-submodules", "charset_normalizer",
    "--hidden-import", "charset_normalizer",
    "--hidden-import", "charset_normalizer.api",
    "--hidden-import", "charset_normalizer.md",
    "--hidden-import", "charset_normalizer.models",
    "--hidden-import", "charset_normalizer.utils",
    "--hidden-import", "charset_normalizer.legacy",
    "--hidden-import", "charset_normalizer.clsid",
    "--hidden-import", "charset_normalizer.cd",

    # Explicitly exclude ALL mypyc compiled extensions
    "--exclude-module", "charset_normalizer.md__mypyc",
    "--exclude-module", "charset_normalizer.legacy__mypyc",
    "--exclude-module", "charset_normalizer.clsid__mypyc",
    "--exclude-module", "charset_normalizer.api__mypyc",
    "--exclude-module", "charset_normalizer.utils__mypyc",
    "--exclude-module", "charset_normalizer.models__mypyc",
    "--exclude-module", "charset_normalizer.cd__mypyc",

    # ─── HARDWARE DETECTION DEPENDENCIES ───
    "--collect-all", "cpuinfo",
    "--collect-all", "GPUtil",
    "--collect-all", "psutil",
    "--collect-all", "wmi",
    "--collect-all", "pywin32",
    "--collect-all", "pyopencl",
    
    "--hidden-import", "cpuinfo",
    "--hidden-import", "GPUtil",
    "--hidden-import", "psutil",
    "--hidden-import", "wmi",
    "--hidden-import", "win32api",
    "--hidden-import", "win32con",
    "--hidden-import", "win32gui",
    "--hidden-import", "win32process",
    "--hidden-import", "pyopencl",

    # ─── TRANSLATIONS SUPPORT ───
    "--collect-all", "PyQt5.QtCore",
    "--collect-all", "PyQt5.QtWidgets",
    "--collect-all", "PyQt5.QtGui",
    "--collect-all", "PyQt5.QtMultimedia",
    
    "--hidden-import", "PyQt5.QtCore",
    "--hidden-import", "PyQt5.QtWidgets",
    "--hidden-import", "PyQt5.QtGui",
    "--hidden-import", "PyQt5.QtMultimedia",
    "--hidden-import", "PyQt5.QtNetwork",

    # ─── SPLEETER + TENSORFLOW + CORE DEPS ───
    "--collect-all", "spleeter",
    "--collect-all", "tensorflow",
    "--collect-all", "numpy",
    "--collect-all", "scipy",
    "--collect-all", "pandas",
    "--collect-all", "librosa",
    "--collect-all", "joblib",
    "--collect-all", "sklearn",
    "--collect-all", "soundfile",
    "--collect-all", "resampy",
    
    "--hidden-import", "spleeter",
    "--hidden-import", "spleeter.separator",
    "--hidden-import", "spleeter.model",
    "--hidden-import", "spleeter.audio",
    "--hidden-import", "tensorflow",
    "--hidden-import", "tensorflow_core",
    "--hidden-import", "keras",
    "--hidden-import", "librosa",
    "--hidden-import", "joblib",
    "--hidden-import", "sklearn",
    "--hidden-import", "soundfile",
    "--hidden-import", "resampy",

    # ─── WHISPER / MOVIEPY / IMAGEIO ───
    "--collect-all", "whisper",
    "--collect-all", "moviepy",
    "--collect-all", "imageio",
    "--collect-all", "imageio_ffmpeg",
    "--collect-all", "decorator",
    "--collect-all", "proglog",
    "--collect-all", "tqdm",
    
    "--hidden-import", "whisper",
    "--hidden-import", "moviepy.editor",
    "--hidden-import", "imageio",
    "--hidden-import", "imageio_ffmpeg",
    "--hidden-import", "decorator",
    "--hidden-import", "proglog",
    "--hidden-import", "tqdm",
    "--hidden-import", "tqdm.auto",

    # ─── PYTORCH (for Whisper) ───
    "--collect-all", "torch",
    "--hidden-import", "torch",
    "--hidden-import", "torch.nn",
    "--hidden-import", "torch.nn.functional",
    "--hidden-import", "torch.utils.data",

    # ─── GOOGLE API DEPENDENCIES ───
    "--collect-all", "googleapiclient",
    "--collect-all", "google_auth_oauthlib",
    "--collect-all", "google.auth",
    "--collect-all", "google.oauth2",
    "--collect-all", "oauth2client",
    
    "--hidden-import", "googleapiclient",
    "--hidden-import", "googleapiclient.discovery",
    "--hidden-import", "googleapiclient.http",
    "--hidden-import", "google.auth",
    "--hidden-import", "google.auth.transport.requests",
    "--hidden-import", "google.oauth2.credentials",
    "--hidden-import", "google_auth_oauthlib.flow",
    "--hidden-import", "oauth2client",
    "--hidden-import", "oauth2client.client",

    # ─── SUBTITLE PROCESSING ───
    "--collect-all", "pysrt",
    "--collect-all", "pysubs2",
    "--hidden-import", "pysrt",
    "--hidden-import", "pysubs2",

    # ─── CRYPTOGRAPHY ───
    "--collect-all", "cryptography",
    "--hidden-import", "cryptography",
    "--hidden-import", "cryptography.fernet",
    "--hidden-import", "cryptography.hazmat",
    "--hidden-import", "cryptography.hazmat.backends",

    # ─── NETWORKING ───
    "--collect-all", "requests",
    "--collect-all", "urllib3",
    "--hidden-import", "requests",
    "--hidden-import", "urllib3",
    "--hidden-import", "urllib3.contrib",

    # ─── SYSTEM / UTILITIES ───
    "--collect-all", "packaging",
    "--collect-all", "importlib_metadata",
    "--hidden-import", "packaging",
    "--hidden-import", "packaging.version",
    "--hidden-import", "packaging.specifiers",
    "--hidden-import", "importlib_metadata",
    "--hidden-import", "pkg_resources",
    "--hidden-import", "pkg_resources.py2_warn",

    # ─── ENSURE ALL TRANSLATION MODULES ───
    "--hidden-import", "PyQt5.QtXml",
    "--hidden-import", "PyQt5.QtSvg",
    "--hidden-import", "PyQt5.QtPrintSupport",

    # Exclude unused heavy modules to keep EXE smaller
    "--exclude-module", "tkinter",
    "--exclude-module", "matplotlib",
    "--exclude-module", "PIL",
    "--exclude-module", "pygame",
    "--exclude-module", "PyQt5.QtBluetooth",
    "--exclude-module", "PyQt5.QtNfc",
    "--exclude-module", "PyQt5.QtPositioning",
    "--exclude-module", "PyQt5.QtWebChannel",
    "--exclude-module", "PyQt5.QtWebEngine",
    "--exclude-module", "PyQt5.QtWebKit",
    "--exclude-module", "PyQt5.QtWebSockets",

    MAIN_SCRIPT
]

# ────────────────────────────────────────────────
# ENCRYPTION
# ────────────────────────────────────────────────

def generate_or_load_key(key_path=KEY_FILE_NAME):
    """Generate or load encryption key"""
    if os.path.exists(key_path):
        with open(key_path, "rb") as f:
            key_data = f.read()
        print(f"[KEY] Loaded existing key from {key_path}")
        return key_data
    
    key = Fernet.generate_key()
    with open(key_path, "wb") as f:
        f.write(key)
    print(f"[KEY] Created new key → {key_path}")
    return key

def encrypt_client():
    """Encrypt client secrets file"""
    if not os.path.isfile(CLIENT_JSON_SRC):
        print(f"[WARNING] {CLIENT_JSON_SRC} not found! Creating placeholder...")
        # Create a placeholder client.json
        placeholder = {
            "installed": {
                "client_id": "YOUR_CLIENT_ID.apps.googleusercontent.com",
                "project_id": "your-project-id",
                "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                "token_uri": "https://oauth2.googleapis.com/token",
                "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                "client_secret": "YOUR_CLIENT_SECRET",
                "redirect_uris": ["urn:ietf:wg:oauth:2.0:oob", "http://localhost"]
            }
        }
        with open(CLIENT_JSON_SRC, "w", encoding="utf-8") as f:
            json.dump(placeholder, f, indent=2)
        print(f"[INFO] Created placeholder {CLIENT_JSON_SRC}")

    key = generate_or_load_key()
    fernet = Fernet(key)

    with open(CLIENT_JSON_SRC, "r", encoding="utf-8") as f:
        data = json.load(f)

    json_bytes = json.dumps(data, ensure_ascii=False, indent=2).encode("utf-8")
    encrypted = fernet.encrypt(json_bytes)
    encoded = base64.b64encode(encrypted).decode("utf-8")

    with open(ENCRYPTED_FILE, "w", encoding="utf-8") as f:
        f.write(encoded)

    print(f"[OK] Encrypted client secrets → {ENCRYPTED_FILE}")

# ────────────────────────────────────────────────
# BUILD FLOW
# ────────────────────────────────────────────────

def check_files():
    """Check if all required files exist"""
    missing = [f for f in REQUIRED_FILES if not os.path.isfile(f)]
    if missing:
        print("\n[ERROR] Missing required files:")
        for m in missing:
            print(f"  • {m}")
        print("\nPlease ensure all files are present before building.")
        sys.exit(1)
    print("[OK] All required files found")

def clean_old():
    """Clean old build folders"""
    for d in TEMP_FOLDERS:
        if os.path.exists(d):
            print(f"[CLEAN] Removing {d}/...")
            try:
                shutil.rmtree(d, ignore_errors=True)
                print(f"  ✓ Removed {d}")
            except Exception as e:
                print(f"  ⚠ Could not remove {d}: {e}")

def run_build():
    """Run PyInstaller build"""
    print("\n" + "═"*90)
    print(f" BUILDING {EXE_NAME}.exe - COMPLETE DEPENDENCY COLLECTION ".center(90, "═"))
    print("═"*90 + "\n")

    print("Build command:")
    cmd_str = " ".join(PYINSTALLER_CMD)
    # Truncate for display if too long
    if len(cmd_str) > 1000:
        print(cmd_str[:500] + "...\n..." + cmd_str[-500:])
    else:
        print(cmd_str)
    print()

    try:
        process = subprocess.run(
            PYINSTALLER_CMD, 
            check=True, 
            capture_output=True, 
            text=True
        )
        if process.stdout:
            print(process.stdout)
        if process.stderr:
            print(process.stderr)
        print("\n[OK] PyInstaller finished successfully.")
    except subprocess.CalledProcessError as e:
        print("\n[ERROR] PyInstaller FAILED!")
        if e.stdout:
            print("\n--- STDOUT ---")
            print(e.stdout)
        if e.stderr:
            print("\n--- STDERR ---")
            print(e.stderr)
        sys.exit(1)

def copy_release():
    """Copy built files to release folder"""
    os.makedirs(RELEASE_FOLDER, exist_ok=True)
    print(f"\n[RELEASE] Copying files to {RELEASE_FOLDER}/...")

    files = [
        (f"dist/{EXE_NAME}.exe",    f"{RELEASE_FOLDER}/{EXE_NAME}.exe"),
        (ENCRYPTED_FILE,            f"{RELEASE_FOLDER}/{ENCRYPTED_FILE}"),
        (KEY_FILE_NAME,             f"{RELEASE_FOLDER}/{KEY_FILE_NAME}"),
        (ICON_FILE,                 f"{RELEASE_FOLDER}/{ICON_FILE}"),
    ]

    for src, dst in files:
        if os.path.exists(src):
            shutil.copy2(src, dst)
            size = os.path.getsize(dst) / (1024*1024)  # MB
            print(f"  ✓ {dst} ({size:.2f} MB)")
        else:
            print(f"  ⚠ Source not found: {src}")

def cleanup():
    """Clean up temporary build folders"""
    print("\n[CLEANUP] Removing temporary folders...")
    for folder in TEMP_FOLDERS:
        if os.path.exists(folder):
            try:
                shutil.rmtree(folder)
                print(f"  ✓ Removed {folder}/")
            except Exception as e:
                print(f"  ⚠ Could not delete {folder}: {e}")

def verify_build():
    """Verify the built executable"""
    exe_path = f"{RELEASE_FOLDER}/{EXE_NAME}.exe"
    if os.path.exists(exe_path):
        size_mb = os.path.getsize(exe_path) / (1024 * 1024)
        print(f"\n[VERIFY] ✓ Executable created successfully")
        print(f"         Size: {size_mb:.2f} MB")
        print(f"         Path: {exe_path}")
        return True
    else:
        print(f"\n[VERIFY] ✗ Executable not found at {exe_path}")
        return False

def print_summary():
    """Print build summary"""
    print("\n" + "═"*100)
    print(" BUILD FINISHED SUCCESSFULLY ".center(100, "═"))
    print("═"*100)
    print(f"\n📁 Release folder : {RELEASE_FOLDER}")
    print(f"📦 Executable     : {RELEASE_FOLDER}/{EXE_NAME}.exe")
    print(f"🔑 Key file       : {RELEASE_FOLDER}/{KEY_FILE_NAME}")
    print(f"📄 Client secrets : {RELEASE_FOLDER}/{ENCRYPTED_FILE}")
    
    size = os.path.getsize(f"{RELEASE_FOLDER}/{EXE_NAME}.exe") / (1024*1024)
    print(f"\n📊 Statistics:")
    print(f"   • Final EXE size: {size:.2f} MB")
    
    print("\n✅ All features included:")
    print("   • Hardware Detection (CPU/GPU/RAM)")
    print("   • Multi-language Support (8 languages)")
    print("   • Real-time Progress Bars")
    print("   • Theme Change without Restart")
    print("   • Spleeter Audio Enhancement")
    print("   • Whisper AI Transcription")
    print("   • Google Drive Integration")
    print("   • System Tray Integration")
    print("   • Keyboard Shortcuts")
    
    print("\n⚠️  IMPORTANT NOTES:")
    print("   1. Add this line at the TOP of main.py (after imports):")
    print('      os.environ["CHARSET_NORMALIZER_USE_MYPYC"] = "0"')
    print("   2. Ensure all dependencies are installed:")
    print("      pip install -r requirements.txt")
    print("   3. The executable saves settings in:")
    print(f"      %APPDATA%\\{EXE_NAME}Saves\\")
    
    print("\n▶️  Run NotyCaption.exe now!")

def create_requirements():
    """Create requirements.txt if it doesn't exist"""
    if not os.path.exists("requirements.txt"):
        requirements = """
# Core AI/ML
torch>=2.0.0
tensorflow>=2.10.0
whisper>=1.1.10
spleeter>=2.3.0

# Audio/Video Processing
moviepy>=1.0.3
imageio>=2.31.0
imageio-ffmpeg>=0.4.8
librosa>=0.10.0
soundfile>=0.12.1
resampy>=0.4.2

# GUI
PyQt5>=5.15.9
PyQt5-sip>=12.12.1

# Google Integration
google-api-python-client>=2.86.0
google-auth-oauthlib>=1.0.0
google-auth>=2.19.0
oauth2client>=4.1.3

# Hardware Detection
psutil>=5.9.5
GPUtil>=1.4.0
py-cpuinfo>=9.0.0
pywin32>=305; platform_system=="Windows"
wmi>=1.5.1; platform_system=="Windows"
pyopencl>=2023.1; platform_system=="Windows"

# Subtitles
pysrt>=1.1.2
pysubs2>=1.6.0

# Utilities
cryptography>=41.0.0
requests>=2.31.0
tqdm>=4.65.0
packaging>=23.1
importlib-metadata>=6.7.0
numpy>=1.24.0
scipy>=1.11.0
pandas>=2.0.0
joblib>=1.3.0
scikit-learn>=1.3.0

# charset_normalizer fix
charset-normalizer==3.2.0
"""
        with open("requirements.txt", "w") as f:
            f.write(requirements.strip())
        print("[INFO] Created requirements.txt")

def main():
    """Main build function"""
    print("="*100)
    print(" NotyCaption Pro Build Tool - Complete Edition ".center(100))
    print("="*100)
    print(f"Started: {datetime.datetime.now():%Y-%m-%d %H:%M:%S}")
    print(f"Python: {sys.version}")
    print(f"Platform: {sys.platform}\n")

    # Create requirements if needed
    create_requirements()

    # Check for required files
    check_files()

    # Encrypt client secrets
    encrypt_client()

    # Clean old build folders
    clean_old()

    # Run PyInstaller
    run_build()

    # Copy to release folder
    copy_release()

    # Clean up
    cleanup()

    # Verify build
    if verify_build():
        print_summary()
    else:
        print("\n❌ Build verification failed!")
        sys.exit(1)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️ Build aborted by user.")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Build crashed:")
        print(f"   Error: {e}")
        print(f"   Type: {type(e).__name__}")
        import traceback
        traceback.print_exc()
        sys.exit(1)