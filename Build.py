# build.py
"""
NotyCaption Pro - Complete Build System
Builds: Main App, Uninstaller, and Creates Professional Installer Package
Includes comprehensive cleanup at start and end
"""

import os
import sys
import json
import base64
import shutil
import subprocess
import datetime
import time
import glob
import stat
from pathlib import Path
from cryptography.fernet import Fernet

# ────────────────────────────────────────────────
# CONFIGURATION
# ────────────────────────────────────────────────

APP_NAME        = "NotyCaption"
PUBLISHER       = "NotY215"
MAIN_SCRIPT     = "main.py"
INSTALLER_SCRIPT = "installer.py"
UNINSTALLER_SCRIPT = "uninstaller.py"
ICON_FILE       = "App.ico"
UNINSTALLER_ICON = "Uninstaller.ico"
CLIENT_JSON_SRC = "client.json"
ENCRYPTED_FILE  = "client.notycapz"
KEY_FILE_NAME   = "key.notcapz"

# Documentation files
README_FILE     = "Readme.html"
TOC_FILE        = "T&C.html"

# Build directories
BUILD_DIR       = "build"
DIST_DIR        = "dist"
PACKAGE_DIR     = "package"
SPEC_DIR        = "spec"
CACHE_DIR       = "__pycache__"
RELEASE_FOLDER  = f"release_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}"

# All temporary directories to clean
TEMP_DIRECTORIES = [
    BUILD_DIR,
    DIST_DIR,
    PACKAGE_DIR,
    SPEC_DIR,
    CACHE_DIR,
    "build_temp",
    "*.spec",
    "__pycache__",
]

# Files to clean
TEMP_FILES = [
    "*.log",
    "*.tmp",
    "*.pyc",
    "*.pyo",
    "*.pyd",
    "*.so",
    "*.dll",
    "*.exe.manifest",
]

# PyInstaller common args
PYINSTALLER_ARGS = [
    "--onefile",
    "--windowed",
    "--clean",
    "--noupx",
    "--log-level=ERROR",  # Reduce noise
]

# ────────────────────────────────────────────────
# CLEANUP UTILITIES
# ────────────────────────────────────────────────

def force_remove(path):
    """Force remove a file or directory with retries"""
    max_retries = 3
    for i in range(max_retries):
        try:
            if os.path.isfile(path) or os.path.islink(path):
                os.unlink(path)
            elif os.path.isdir(path):
                # First try normal rmtree
                try:
                    shutil.rmtree(path, ignore_errors=False)
                except:
                    # If fails, try to change permissions and remove
                    for root, dirs, files in os.walk(path):
                        for d in dirs:
                            os.chmod(os.path.join(root, d), stat.S_IWRITE)
                        for f in files:
                            os.chmod(os.path.join(root, f), stat.S_IWRITE)
                    shutil.rmtree(path, ignore_errors=True)
            return True
        except Exception as e:
            if i < max_retries - 1:
                time.sleep(1)
                continue
            print(f"⚠ Could not remove {path}: {e}")
            return False
    return False

def clean_all_temp():
    """Comprehensive cleanup of all temporary files and directories"""
    print("\n" + "="*60)
    print(" CLEANING ALL TEMPORARY FILES ".center(60))
    print("="*60)
    
    removed_count = 0
    failed_count = 0
    
    # Clean temp directories
    for pattern in TEMP_DIRECTORIES:
        if '*' in pattern:
            # Handle wildcard patterns
            for path in glob.glob(pattern):
                if force_remove(path):
                    print(f"✓ Removed: {path}")
                    removed_count += 1
                else:
                    failed_count += 1
        else:
            # Handle exact directory names
            if os.path.exists(pattern):
                if force_remove(pattern):
                    print(f"✓ Removed directory: {pattern}/")
                    removed_count += 1
                else:
                    failed_count += 1
    
    # Clean temp files
    for pattern in TEMP_FILES:
        for path in glob.glob(pattern):
            if os.path.exists(path):
                if force_remove(path):
                    print(f"✓ Removed file: {path}")
                    removed_count += 1
                else:
                    failed_count += 1
    
    # Clean Python cache directories recursively
    for root, dirs, files in os.walk('.'):
        if '__pycache__' in dirs:
            pycache = os.path.join(root, '__pycache__')
            if force_remove(pycache):
                print(f"✓ Removed: {pycache}/")
                removed_count += 1
        
        # Clean .pyc files
        for file in files:
            if file.endswith('.pyc'):
                pyc_file = os.path.join(root, file)
                if force_remove(pyc_file):
                    print(f"✓ Removed: {pyc_file}")
                    removed_count += 1
    
    # Clean PyInstaller specific files
    pyinstaller_files = [
        "warn*.txt",
        "*.toc",
        "*.pyz",
        "*.spec",
    ]
    for pattern in pyinstaller_files:
        for path in glob.glob(pattern):
            if os.path.exists(path):
                if force_remove(path):
                    print(f"✓ Removed: {path}")
                    removed_count += 1
    
    print(f"\n✅ Cleanup complete: {removed_count} items removed, {failed_count} failures")
    return removed_count > 0

def kill_pyinstaller_processes():
    """Kill any lingering PyInstaller processes"""
    try:
        import psutil
        killed = 0
        for proc in psutil.process_iter(['pid', 'name', 'cmdline']):
            try:
                if proc.info['name'] and 'pyinstaller' in proc.info['name'].lower():
                    proc.kill()
                    killed += 1
                elif proc.info['cmdline'] and any('pyinstaller' in ' '.join(proc.info['cmdline']).lower() for _ in [0]):
                    proc.kill()
                    killed += 1
            except:
                pass
        if killed > 0:
            print(f"✓ Killed {killed} lingering PyInstaller processes")
    except:
        pass

# ────────────────────────────────────────────────
# ENCRYPTION UTILS
# ────────────────────────────────────────────────

def generate_or_load_key(key_path=KEY_FILE_NAME):
    """Generate or load encryption key"""
    if os.path.exists(key_path):
        with open(key_path, "rb") as f:
            return f.read()
    key = Fernet.generate_key()
    with open(key_path, "wb") as f:
        f.write(key)
    print(f"✓ Created new key: {key_path}")
    return key

def encrypt_client():
    """Encrypt client secrets file"""
    if not os.path.isfile(CLIENT_JSON_SRC):
        print(f"⚠ Creating placeholder {CLIENT_JSON_SRC}")
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

    key = generate_or_load_key()
    fernet = Fernet(key)

    with open(CLIENT_JSON_SRC, "r", encoding="utf-8") as f:
        data = json.load(f)

    encrypted = fernet.encrypt(json.dumps(data).encode())
    encoded = base64.b64encode(encrypted).decode()

    with open(ENCRYPTED_FILE, "w", encoding="utf-8") as f:
        f.write(encoded)

    print(f"✓ Encrypted client secrets → {ENCRYPTED_FILE}")

# ────────────────────────────────────────────────
# DOCUMENTATION FILES
# ────────────────────────────────────────────────

def create_documentation():
    """Create HTML documentation files if they don't exist"""
    
    # Readme.html
    if not os.path.exists(README_FILE):
        readme_content = """<!DOCTYPE html>
<html>
<head>
    <title>NotyCaption Pro - Welcome</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; background: #1a1a2e; color: #e0e0ff; line-height: 1.6; padding: 20px; }
        h1 { color: #89b4fa; border-bottom: 2px solid #45475a; padding-bottom: 10px; }
        h2 { color: #b4befe; margin-top: 25px; }
        ul { list-style-type: none; padding-left: 0; }
        li { margin: 10px 0; padding: 10px; background: #242436; border-radius: 8px; }
        li:before { content: "✓ "; color: #a6e3a1; font-weight: bold; }
        .feature-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 15px; }
        .note { background: #2a2a3a; border-left: 4px solid #f38ba8; padding: 15px; border-radius: 8px; }
    </style>
</head>
<body>
    <h1>🎬 Welcome to NotyCaption Pro</h1>
    <p><strong>Secure AI Caption Generator by NotY215 & Team</strong></p>
    
    <div class="feature-grid">
        <div>
            <h2>✨ Key Features</h2>
            <ul>
                <li>AI-Powered Caption Generation (Whisper)</li>
                <li>Vocal Enhancement with Spleeter</li>
                <li>Google Drive Integration</li>
                <li>8 Language Support</li>
                <li>Hardware Acceleration Detection</li>
                <li>Real-time Progress Tracking</li>
            </ul>
        </div>
        <div>
            <h2>🖥️ System Requirements</h2>
            <ul>
                <li>Windows 10/11 (64-bit)</li>
                <li>4GB RAM minimum (8GB recommended)</li>
                <li>3GB free disk space</li>
                <li>Internet connection for online mode</li>
                <li>Optional: NVIDIA GPU for acceleration</li>
            </ul>
        </div>
    </div>
    
    <h2>📦 What's Included</h2>
    <ul>
        <li>NotyCaption Pro Application</li>
        <li>Uninstaller Tool</li>
        <li>Documentation & Help Files</li>
        <li>Encrypted Client Secrets</li>
    </ul>
    
    <div class="note">
        <strong>⚠️ Important:</strong> This software uses AI models that may require 
        initial download (~2.9GB). Ensure stable internet connection for first run.
    </div>
    
    <p style="text-align: center; margin-top: 30px; color: #89b4fa;">
        Click Next to continue with installation →
    </p>
</body>
</html>"""
        with open(README_FILE, "w", encoding="utf-8") as f:
            f.write(readme_content)
        print(f"✓ Created {README_FILE}")

    # T&C.html
    if not os.path.exists(TOC_FILE):
        toc_content = """<!DOCTYPE html>
<html>
<head>
    <title>Terms & Conditions - NotyCaption Pro</title>
    <style>
        body { font-family: 'Segoe UI', sans-serif; background: #1a1a2e; color: #e0e0ff; line-height: 1.6; padding: 20px; }
        h1 { color: #f38ba8; border-bottom: 2px solid #45475a; padding-bottom: 10px; }
        h2 { color: #fab387; margin-top: 25px; }
        .section { background: #242436; padding: 15px; border-radius: 8px; margin: 15px 0; }
        .highlight { color: #a6e3a1; font-weight: bold; }
    </style>
</head>
<body>
    <h1>📜 Terms and Conditions</h1>
    <p><strong>Last Updated: March 2026</strong></p>
    
    <div class="section">
        <h2>1. License Agreement</h2>
        <p>This software is licensed, not sold. NotY215 grants you a non-exclusive, 
        non-transferable license to use this software on your personal computer.</p>
    </div>
    
    <div class="section">
        <h2>2. Open Source Components</h2>
        <p>This software uses the following open-source libraries:</p>
        <ul>
            <li>Whisper AI (MIT License)</li>
            <li>Spleeter (MIT License)</li>
            <li>PyQt5 (GPL v3)</li>
            <li>TensorFlow (Apache 2.0)</li>
            <li>And others - see documentation</li>
        </ul>
    </div>
    
    <div class="section">
        <h2>3. Data Privacy</h2>
        <p>NotyCaption Pro processes all media locally by default. When using online mode,
        audio files are temporarily uploaded to Google Drive and automatically deleted 
        after processing. No personal data is collected or stored by NotY215.</p>
    </div>
    
    <div class="section">
        <h2>4. Disclaimer of Warranty</h2>
        <p>This software is provided "AS IS" without warranty of any kind. The authors
        are not liable for any damages arising from its use.</p>
    </div>
    
    <div class="section">
        <h2>5. AI Model Usage</h2>
        <p>The Whisper and Spleeter models are downloaded separately and subject to their
        respective licenses. You are responsible for complying with those licenses.</p>
    </div>
    
    <p style="text-align: center; margin-top: 30px;">
        <span class="highlight">By installing, you agree to these terms.</span>
    </p>
</body>
</html>"""
        with open(TOC_FILE, "w", encoding="utf-8") as f:
            f.write(toc_content)
        print(f"✓ Created {TOC_FILE}")

# ────────────────────────────────────────────────
# BUILD FUNCTIONS
# ────────────────────────────────────────────────

def create_directories():
    """Create necessary build directories"""
    dirs = [BUILD_DIR, DIST_DIR, PACKAGE_DIR, RELEASE_FOLDER]
    for d in dirs:
        os.makedirs(d, exist_ok=True)
        print(f"✓ Created directory: {d}/")

def build_app():
    """Build main NotyCaption application"""
    print("\n" + "="*60)
    print(" Building NotyCaption Application ".center(60))
    print("="*60)

    cmd = [
        "pyinstaller",
        *PYINSTALLER_ARGS,
        f"--icon={ICON_FILE}",
        f"--name={APP_NAME}",
        "--add-data", f"{ICON_FILE};.",
        "--add-data", f"{ENCRYPTED_FILE};.",
        "--add-data", f"{KEY_FILE_NAME};.",
        "--collect-all", "charset_normalizer",
        "--collect-all", "spleeter",
        "--collect-all", "tensorflow",
        "--collect-all", "whisper",
        "--collect-all", "torch",
        "--collect-all", "moviepy",
        "--collect-all", "PyQt5",
        "--collect-all", "cpuinfo",
        "--collect-all", "GPUtil",
        "--collect-all", "psutil",
        "--collect-all", "googleapiclient",
        "--collect-all", "cryptography",
        "--hidden-import", "charset_normalizer",
        "--hidden-import", "spleeter.separator",
        "--hidden-import", "whisper",
        "--hidden-import", "moviepy.editor",
        "--hidden-import", "pysrt",
        "--hidden-import", "pysubs2",
        "--hidden-import", "google.auth",
        "--hidden-import", "google_auth_oauthlib.flow",
        "--exclude-module", "tkinter",
        "--exclude-module", "matplotlib",
        "--exclude-module", "PIL",
        "--exclude-module", "pygame",
        "--exclude-module", "charset_normalizer.md__mypyc",
        "--exclude-module", "charset_normalizer.legacy__mypyc",
        MAIN_SCRIPT
    ]

    print("Running PyInstaller (this may take several minutes)...")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("❌ App build failed!")
        if result.stderr:
            print("\nError output:")
            print(result.stderr)
        sys.exit(1)
    
    # Copy to package folder
    src = f"dist/{APP_NAME}.exe"
    dst = f"{PACKAGE_DIR}/{APP_NAME}.exe"
    if os.path.exists(src):
        shutil.copy2(src, dst)
        size = os.path.getsize(dst) / (1024*1024)
        print(f"✓ App built: {dst} ({size:.2f} MB)")
    else:
        print(f"❌ Build output not found: {src}")
        sys.exit(1)

def build_uninstaller():
    """Build uninstaller application"""
    print("\n" + "="*60)
    print(" Building Uninstaller ".center(60))
    print("="*60)

    cmd = [
        "pyinstaller",
        *PYINSTALLER_ARGS,
        f"--icon={UNINSTALLER_ICON}",
        "--name=uninstall",
        "--add-data", f"{UNINSTALLER_ICON};.",
        "--collect-all", "PySide6",
        "--hidden-import", "win32com",
        "--hidden-import", "winreg",
        UNINSTALLER_SCRIPT
    ]

    print("Running PyInstaller...")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("❌ Uninstaller build failed!")
        if result.stderr:
            print("\nError output:")
            print(result.stderr)
        sys.exit(1)
    
    # Copy to package folder
    src = "dist/uninstall.exe"
    dst = f"{PACKAGE_DIR}/uninstall.exe"
    if os.path.exists(src):
        shutil.copy2(src, dst)
        size = os.path.getsize(dst) / (1024*1024)
        print(f"✓ Uninstaller built: {dst} ({size:.2f} MB)")
    else:
        print(f"❌ Uninstaller not found: {src}")
        sys.exit(1)

def build_installer():
    """Build installer as self-extracting executable"""
    print("\n" + "="*60)
    print(" Building Installer Package ".center(60))
    print("="*60)

    # Copy installer script and required files
    shutil.copy2(INSTALLER_SCRIPT, f"{PACKAGE_DIR}/installer.py")
    shutil.copy2(ICON_FILE, f"{PACKAGE_DIR}/App.ico")
    shutil.copy2(UNINSTALLER_ICON, f"{PACKAGE_DIR}/Uninstaller.ico")
    shutil.copy2(ENCRYPTED_FILE, f"{PACKAGE_DIR}/client.notycapz")
    shutil.copy2(KEY_FILE_NAME, f"{PACKAGE_DIR}/key.notcapz")
    
    # Copy documentation
    create_documentation()
    if os.path.exists(README_FILE):
        shutil.copy2(README_FILE, f"{PACKAGE_DIR}/Readme.html")
    if os.path.exists(TOC_FILE):
        shutil.copy2(TOC_FILE, f"{PACKAGE_DIR}/T&C.html")

    # Create the installer executable
    cmd = [
        "pyinstaller",
        "--onefile",
        "--windowed",
        "--clean",
        f"--icon={ICON_FILE}",
        f"--name={APP_NAME}_Setup",
        "--add-data", f"{PACKAGE_DIR};package",
        "--hidden-import", "PySide6",
        "--hidden-import", "win32com",
        "--hidden-import", "winreg",
        "--hidden-import", "psutil",
        INSTALLER_SCRIPT
    ]

    print("Building installer (this may take a moment)...")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print("❌ Installer build failed!")
        if result.stderr:
            print("\nError output:")
            print(result.stderr)
        sys.exit(1)

    # Copy to release folder
    src = f"dist/{APP_NAME}_Setup.exe"
    dst = f"{RELEASE_FOLDER}/{APP_NAME}_Setup.exe"
    if os.path.exists(src):
        shutil.copy2(src, dst)
        size = os.path.getsize(dst) / (1024*1024)
        print(f"✓ Installer built: {dst} ({size:.2f} MB)")
    else:
        print(f"❌ Installer not found: {src}")
        sys.exit(1)

# ────────────────────────────────────────────────
# PACKAGE VERIFICATION
# ────────────────────────────────────────────────

def verify_package():
    """Verify all components are present"""
    required_in_package = [
        f"{PACKAGE_DIR}/{APP_NAME}.exe",
        f"{PACKAGE_DIR}/uninstall.exe",
        f"{PACKAGE_DIR}/installer.py",
        f"{PACKAGE_DIR}/App.ico",
        f"{PACKAGE_DIR}/Uninstaller.ico",
        f"{PACKAGE_DIR}/client.notycapz",
        f"{PACKAGE_DIR}/key.notcapz",
        f"{PACKAGE_DIR}/Readme.html",
        f"{PACKAGE_DIR}/T&C.html",
    ]

    missing = []
    sizes = {}
    
    for f in required_in_package:
        if os.path.exists(f):
            sizes[f] = os.path.getsize(f) / (1024*1024)  # MB
        else:
            missing.append(f)

    if missing:
        print("\n❌ Missing files in package:")
        for f in missing:
            print(f"   • {f}")
        return False
    
    print("\n📦 Package contents:")
    for f, size in sizes.items():
        print(f"   • {os.path.basename(f)}: {size:.2f} MB")
    
    return True

def create_release_info():
    """Create release information file"""
    info = f"""=== {APP_NAME} Pro ===
Version: 2026.1.0
Build Date: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
Publisher: {PUBLISHER}

Installation Instructions:
1. Run {APP_NAME}_Setup.exe as Administrator
2. Follow the installation wizard
3. Launch {APP_NAME} from Desktop or Start Menu

Package Contents:
- Main Application: {APP_NAME}.exe
- Uninstaller: uninstall.exe
- Installer: {APP_NAME}_Setup.exe
- Documentation: Readme.html, T&C.html
- Configuration files

System Requirements:
- Windows 10/11 (64-bit)
- 4GB RAM minimum (8GB recommended)
- 3GB free disk space
- Internet connection for online mode

Build Environment:
- Python: {sys.version.split()[0]}
- Platform: {sys.platform}

All rights reserved © 2026 {PUBLISHER}
"""
    with open(f"{RELEASE_FOLDER}/RELEASE.txt", "w") as f:
        f.write(info)
    print(f"✓ Created release info: {RELEASE_FOLDER}/RELEASE.txt")

# ────────────────────────────────────────────────
# MAIN BUILD PROCESS
# ────────────────────────────────────────────────

def main():
    print("="*80)
    print(f" {APP_NAME} Pro - Complete Build System ".center(80))
    print("="*80)
    print(f"Started: {datetime.datetime.now():%Y-%m-%d %H:%M:%S}")
    print(f"Python: {sys.version.split()[0]}")
    print(f"Platform: {sys.platform}")
    
    start_time = time.time()

    # Step 1: Kill any lingering PyInstaller processes
    print("\n🔪 Step 1: Killing lingering processes...")
    kill_pyinstaller_processes()

    # Step 2: Comprehensive cleanup
    print("\n🧹 Step 2: Cleaning all temporary files...")
    clean_all_temp()

    # Step 3: Encrypt client secrets
    print("\n📁 Step 3: Preparing secrets...")
    encrypt_client()

    # Step 4: Create documentation
    print("\n📄 Step 4: Creating documentation...")
    create_documentation()

    # Step 5: Create necessary directories
    print("\n📂 Step 5: Creating build directories...")
    create_directories()

    # Step 6: Build main app
    print("\n🔨 Step 6: Building main application...")
    build_app()

    # Step 7: Build uninstaller
    print("\n🔨 Step 7: Building uninstaller...")
    build_uninstaller()

    # Step 8: Build installer
    print("\n🔨 Step 8: Building installer...")
    build_installer()

    # Step 9: Verify package
    print("\n✅ Step 9: Verifying package...")
    if not verify_package():
        print("❌ Package verification failed!")
        sys.exit(1)

    # Step 10: Create release info
    print("\n📝 Step 10: Creating release information...")
    create_release_info()

    # Calculate build time
    build_time = time.time() - start_time
    minutes = int(build_time // 60)
    seconds = int(build_time % 60)

    # Final cleanup
    print("\n🧹 Final cleanup...")
    temp_to_keep = [RELEASE_FOLDER]  # Keep only the release folder
    all_items = [d for d in os.listdir('.') if os.path.isdir(d) and d not in temp_to_keep]
    for item in all_items:
        if item.startswith('build') or item.startswith('dist') or item.startswith('package') or item.startswith('spec'):
            force_remove(item)
    
    # Clean spec files
    for spec in glob.glob("*.spec"):
        force_remove(spec)

    # Final summary
    print("\n" + "="*80)
    print(" BUILD COMPLETE - SUCCESS ".center(80, "="))
    print("="*80)
    print(f"\n📂 Release folder: {RELEASE_FOLDER}/")
    print(f"   ├─ {APP_NAME}_Setup.exe")
    print(f"   ├─ RELEASE.txt")
    print(f"   └─ (package contents)")
    
    installer_size = os.path.getsize(f"{RELEASE_FOLDER}/{APP_NAME}_Setup.exe") / (1024*1024)
    print(f"\n📊 Statistics:")
    print(f"   • Build time: {minutes}m {seconds}s")
    print(f"   • Installer size: {installer_size:.2f} MB")
    print(f"   • Package includes: Main App, Uninstaller, Documentation")
    
    print(f"\n▶️  Run {RELEASE_FOLDER}/{APP_NAME}_Setup.exe to install")
    print(f"\n{datetime.datetime.now():%Y-%m-%d %H:%M:%S} - Build finished successfully\n")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️ Build cancelled by user")
        print("\n🧹 Performing cleanup...")
        clean_all_temp()
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ Build failed: {e}")
        import traceback
        traceback.print_exc()
        print("\n🧹 Performing cleanup...")
        clean_all_temp()
        sys.exit(1)