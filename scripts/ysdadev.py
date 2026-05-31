#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
YSDA DevTool Façade for MDRelay v2.8.5
Standard vocabulary: list, doctor, build, test unit, install, run, check
"""

import sys
import os
import subprocess
import json
import re

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(PROJECT_ROOT)

DEBUG_APK = os.path.join(PROJECT_ROOT, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
PACKAGE_NAME = "com.simpsonys.mdrelay"
LAUNCHER_ACTIVITY = "com.simpsonys.mdrelay/.MainActivity"

def get_gradle():
    if os.name == 'nt':
        gradle_cmd = os.path.join(PROJECT_ROOT, "gradlew.bat")
        if os.path.exists(gradle_cmd):
            return gradle_cmd
    else:
        gradle_cmd = os.path.join(PROJECT_ROOT, "gradlew")
        if os.path.exists(gradle_cmd):
            return gradle_cmd
    return None

def get_adb():
    # Candidates
    candidates = []
    android_home = os.environ.get("ANDROID_HOME")
    if android_home:
        candidates.append(os.path.join(android_home, "platform-tools", "adb.exe" if os.name == 'nt' else "adb"))
    android_sdk_root = os.environ.get("ANDROID_SDK_ROOT")
    if android_sdk_root:
        candidates.append(os.path.join(android_sdk_root, "platform-tools", "adb.exe" if os.name == 'nt' else "adb"))
    
    # Try looking in PATH
    for path in os.environ.get("PATH", "").split(os.pathsep):
        candidates.append(os.path.join(path, "adb.exe" if os.name == 'nt' else "adb"))
        candidates.append(os.path.join(path, "adb"))

    for c in candidates:
        if os.path.exists(c) and os.path.isfile(c):
            return c
    
    # Fallback to plain 'adb' command
    return "adb"

def run_cmd(args, shell=False):
    print(f"==> Executing: {' '.join(args)}")
    try:
        res = subprocess.run(args, shell=shell, check=True)
        return res.returncode == 0
    except subprocess.CalledProcessError as e:
        print(f"Error: Command failed with exit code {e.returncode}")
        return False
    except Exception as e:
        print(f"Error: {str(e)}")
        return False

def cmd_list():
    print("MDRelay YSDA DevTool - Available commands:")
    print("  list            Show this list of available commands")
    print("  doctor          Validate environment dependencies and device connections")
    print("  build           Assemble the debug APK (assembleDebug)")
    print("  test unit       Run JUnit unit tests (testDebugUnitTest)")
    print("  install         Install debug APK to the connected device(s)")
    print("  run             Launch the app activity on the connected device(s)")
    print("  check           Run build and unit tests sequentially")
    print("  uninstall       Uninstall the package com.simpsonys.mdrelay from connected device(s)")
    print("  connect <ip>    Connect to a wireless ADB device (e.g., 192.168.0.5:5555)")
    return True

def cmd_doctor():
    print("==> YSDA Doctor Diagnostic")
    gradle = get_gradle()
    if gradle:
        print(f"  [PASS] Gradle Wrapper found at: {gradle}")
    else:
        print("  [FAIL] Gradle Wrapper NOT found in root directory!")
        
    adb = get_adb()
    adb_ok = False
    try:
        res = subprocess.run([adb, "version"], capture_output=True, text=True)
        if res.returncode == 0:
            print(f"  [PASS] ADB found and functional: {res.stdout.splitlines()[0]}")
            adb_ok = True
        else:
            print("  [FAIL] ADB execution failed!")
    except Exception:
        print("  [FAIL] ADB not found on PATH or environment variables!")

    if adb_ok:
        try:
            res = subprocess.run([adb, "devices"], capture_output=True, text=True)
            devices = []
            for line in res.stdout.splitlines()[1:]:
                if line.strip() and not line.startswith("*"):
                    devices.append(line)
            if devices:
                print("  [PASS] Connected devices:")
                for d in devices:
                    print(f"    - {d}")
            else:
                print("  [WARN] No connected devices found. Install/run commands will wait for device.")
        except Exception as e:
            print(f"  [WARN] Could not query adb devices: {str(e)}")

    print(f"  [INFO] Target Package: {PACKAGE_NAME}")
    print(f"  [INFO] Launcher Activity: {LAUNCHER_ACTIVITY}")
    return True

def cmd_build():
    gradle = get_gradle()
    if not gradle:
        print("Error: Gradle wrapper not found.")
        return False
    return run_cmd([gradle, "assembleDebug"])

def cmd_test_unit():
    gradle = get_gradle()
    if not gradle:
        print("Error: Gradle wrapper not found.")
        return False
    print("Note: If test folders do not exist, Gradle completes with SUCCESS.")
    return run_cmd([gradle, "testDebugUnitTest"])

def cmd_install():
    if not os.path.exists(DEBUG_APK):
        print(f"Warning: Debug APK not found at {DEBUG_APK}. Building first...")
        if not cmd_build():
            return False

    adb = get_adb()
    # Find active device serials
    try:
        res = subprocess.run([adb, "devices"], capture_output=True, text=True)
        devices = []
        for line in res.stdout.splitlines()[1:]:
            if line.strip() and not line.startswith("*"):
                parts = line.split()
                if len(parts) >= 2 and parts[1] == "device":
                    devices.append(parts[0])
    except Exception:
        devices = []

    if not devices:
        print("Error: No online Android devices/emulators detected via ADB.")
        return False

    success = True
    for serial in devices:
        print(f"==> Installing APK to device: {serial}")
        ok = run_cmd([adb, "-s", serial, "install", "-r", DEBUG_APK])
        if not ok:
            success = False
    return success

def cmd_run():
    adb = get_adb()
    try:
        res = subprocess.run([adb, "devices"], capture_output=True, text=True)
        devices = []
        for line in res.stdout.splitlines()[1:]:
            if line.strip() and not line.startswith("*"):
                parts = line.split()
                if len(parts) >= 2 and parts[1] == "device":
                    devices.append(parts[0])
    except Exception:
        devices = []

    if not devices:
        print("Error: No online Android devices/emulators detected via ADB.")
        return False

    success = True
    for serial in devices:
        print(f"==> Launching activity on device: {serial}")
        # am start -n com.simpsonys.mdrelay/.MainActivity
        ok = run_cmd([adb, "-s", serial, "shell", "am", "start", "-n", LAUNCHER_ACTIVITY])
        if not ok:
            success = False
    return success

def cmd_check():
    print("==> Executing ysdadev check pipeline...")
    if not cmd_build():
        return False
    if not cmd_test_unit():
        return False
    print("==> Check pipeline PASSED successfully.")
    return True

def cmd_uninstall():
    adb = get_adb()
    try:
        res = subprocess.run([adb, "devices"], capture_output=True, text=True)
        devices = []
        for line in res.stdout.splitlines()[1:]:
            if line.strip() and not line.startswith("*"):
                parts = line.split()
                if len(parts) >= 2 and parts[1] == "device":
                    devices.append(parts[0])
    except Exception:
        devices = []

    if not devices:
        print("Error: No online Android devices/emulators detected via ADB.")
        return False

    success = True
    for serial in devices:
        print(f"==> Uninstalling package '{PACKAGE_NAME}' from device: {serial}")
        ok = run_cmd([adb, "-s", serial, "uninstall", PACKAGE_NAME])
        if not ok:
            success = False
    return success

def cmd_connect(ip_port):
    adb = get_adb()
    print(f"==> Connecting to wireless device via ADB: {ip_port}")
    return run_cmd([adb, "connect", ip_port])

def main():
    args = sys.argv[1:]
    if not args:
        cmd_list()
        sys.exit(0)

    verb = args[0].lower()
    
    # Handle composite "test unit" or "test"
    if verb == "test":
        if len(args) > 1 and args[1].lower() == "unit":
            success = cmd_test_unit()
        else:
            success = cmd_test_unit()
    elif verb == "list":
        success = cmd_list()
    elif verb == "doctor":
        success = cmd_doctor()
    elif verb == "build":
        success = cmd_build()
    elif verb == "install":
        success = cmd_install()
    elif verb == "run":
        success = cmd_run()
    elif verb == "check":
        success = cmd_check()
    elif verb == "uninstall":
        success = cmd_uninstall()
    elif verb == "connect":
        if len(args) > 1:
            success = cmd_connect(args[1])
        else:
            print("Error: IP and Port are required. Usage: .\\ysdadev.cmd connect <ip>:<port>")
            success = False
    else:
        print(f"Unknown ysdadev command: '{' '.join(args)}'")
        cmd_list()
        sys.exit(1)

    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
