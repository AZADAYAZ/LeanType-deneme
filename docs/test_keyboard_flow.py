#!/usr/bin/env python3
import subprocess
import time

ADB_DEVICE = "192.168.240.112:5555"

def run_cmd(cmd):
    try:
        res = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=30)
        return res.stdout.strip()
    except Exception as e:
        print(f"Error running command '{cmd}': {e}")
        return ""

def adb(cmd):
    return run_cmd(f"adb -s {ADB_DEVICE} {cmd}")

def main():
    print(f"1. Connecting to ADB device {ADB_DEVICE}...")
    run_cmd(f"adb connect {ADB_DEVICE}")

    print("2. Clearing logcat...")
    adb("logcat -c")

    print("3. Launching LeanType Settings...")
    adb("shell am start -n com.leanbitlab.leantype.debug/helium314.keyboard.settings.SettingsActivity")
    time.sleep(2)

    print("4. Tapping Search Icon (Top-Right) to open SearchScreen...")
    adb("shell input tap 960 160")
    time.sleep(1.5)

    print("5. Tapping Search Input Field to trigger LeanType Keyboard...")
    adb("shell input tap 400 160")
    time.sleep(1.5)

    print("6. Typing 'gesture' into Search Field...")
    adb("shell input text 'gesture'")
    time.sleep(1.5)

    print("7. Clearing search query...")
    for _ in range(7):
        adb("shell input keyevent 67")
        time.sleep(0.1)

    print("8. Typing 'autocorrect' into Search Field...")
    adb("shell input text 'autocorrect'")
    time.sleep(1.5)

    print("9. Dismissing Search and Navigating Back...")
    adb("shell input keyevent 4")
    time.sleep(1)
    adb("shell input keyevent 4")
    time.sleep(1)

    print("10. Fetching Logcat Errors & Warnings...")
    logs = adb("logcat -d *:W")
    
    app_logs = [line for line in logs.splitlines() if any(k in line.lower() for k in ["leantype", "helium314", "latinime", "richinputconnection"])]
    
    print("\n--- LOGCAT SUMMARY FOR LEANTYPE ---")
    if app_logs:
        for log in app_logs:
            print(log)
    else:
        print("✅ Clean logcat! Zero errors/warnings found for LeanType.")

if __name__ == "__main__":
    main()
