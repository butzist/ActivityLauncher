#!/usr/bin/env python3
import os
import re
import subprocess
import sys
import time

PACKAGE_NAME = "de.szalkowski.activitylauncher.oss"
MAIN_ACTIVITY = f"{PACKAGE_NAME}/de.szalkowski.activitylauncher.entrypoint.MainActivity"
SHORTCUT_LABEL = "Upgrade Test Settings Shortcut"
TARGET_PACKAGE = "com.android.settings"

PREVIOUS_RELEASE_TAG = "2.4.1"
PREVIOUS_APK_ORIGINAL = "/tmp/previous_release/app-oss-noads-release.apk"
PREVIOUS_APK_RESIGNED = "/tmp/previous_release_debug.apk"
NEW_APK_PATH = "app/build/outputs/apk/ossNoads/debug/app-oss-noads-debug.apk"


def get_env():
    env = os.environ.copy()
    env["APPID"] = "de.szalkowski.activitylauncher.oss"
    home_dir = os.path.expanduser("~")
    if "HOME" not in env:
        env["HOME"] = home_dir

    java_in_path = (
        subprocess.run("which java", shell=True, capture_output=True, text=True, env=env).returncode == 0
    )
    if not java_in_path:
        possible_jdks = [
            os.environ.get("JAVA_HOME", ""),
            os.path.join(home_dir, ".jdks/ms-21.0.12.1"),
            "/usr/lib/jvm/default-java",
        ]
        for jdk in possible_jdks:
            if not jdk:
                continue
            bin_path = os.path.join(jdk, "bin")
            if os.path.exists(os.path.join(bin_path, "java")):
                env["PATH"] = f"{bin_path}:{env.get('PATH', '')}"
                env["JAVA_HOME"] = jdk
                break
    return env


def run_cmd(cmd, check=True):
    print(f"Executing: {cmd}")
    env = get_env()
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True, env=env)
    if check and res.returncode != 0:
        print(
            f"Command failed with exit code {res.returncode}:\nSTDOUT: {res.stdout}\nSTDERR: {res.stderr}"
        )
        sys.exit(1)
    return res


def adb(args, check=True):
    return run_cmd(f"adb {args}", check=check)


def adb_shell(cmd, check=True):
    return adb(f'shell "{cmd}"', check=check)


def find_apksigner():
    android_home = (
        os.environ.get("ANDROID_HOME")
        or os.environ.get("ANDROID_SDK_ROOT")
        or os.path.expanduser("~/Android/Sdk")
    )
    build_tools_dir = os.path.join(android_home, "build-tools")
    if os.path.exists(build_tools_dir):
        versions = sorted(os.listdir(build_tools_dir), reverse=True)
        for ver in versions:
            path = os.path.join(build_tools_dir, ver, "apksigner")
            if os.path.exists(path):
                return path

    which_res = subprocess.run(
        "which apksigner", shell=True, capture_output=True, text=True
    )
    if which_res.returncode == 0:
        return which_res.stdout.strip()
    return "apksigner"


def setup_environment():
    print(
        f"=== Step 1: Downloading previous release ({PREVIOUS_RELEASE_TAG}) from GitHub ==="
    )
    run_cmd(
        f"gh release download {PREVIOUS_RELEASE_TAG} --repo ActivityLauncher/ActivityLauncher --pattern 'app-oss-noads-release.apk' --dir /tmp/previous_release --clobber"
    )

    print(
        "=== Step 2: Re-signing previous release APK with local debug keystore ==="
    )
    apksigner = find_apksigner()
    keystore = os.path.expanduser("~/.android/debug.keystore")
    if not os.path.exists(keystore):
        print(
            f"Debug keystore not found at {keystore}, building project to generate debug keystore..."
        )
        run_cmd("./gradlew app:assembleOssNoadsDebug")

    sign_cmd = f'"{apksigner}" sign --ks {keystore} --ks-pass pass:android --key-pass pass:android --out {PREVIOUS_APK_RESIGNED} {PREVIOUS_APK_ORIGINAL}'
    run_cmd(sign_cmd)

    print("=== Step 3: Checking current version APK ===")
    if not os.path.exists(NEW_APK_PATH):
        print(f"Current version APK not found at {NEW_APK_PATH}, building it now...")
        run_cmd("./gradlew app:assembleOssNoadsDebug")
    print(f"Current version APK verified at {NEW_APK_PATH}")


def dump_ui():
    adb_shell("uiautomator dump /sdcard/window_dump.xml >/dev/null", check=False)
    res = adb_shell("cat /sdcard/window_dump.xml", check=False)
    return res.stdout


def find_node_center(
    dump_xml, resource_id=None, text=None, text_contains=None, pkg=None
):
    for match in re.finditer(r"<node ([^>]+)>", dump_xml):
        node_attr = match.group(1)
        if resource_id and f'resource-id="{resource_id}"' not in node_attr:
            continue
        if text and f'text="{text}"' not in node_attr:
            continue
        if text_contains and text_contains.lower() not in node_attr.lower():
            continue
        if pkg and f'package="{pkg}"' not in node_attr:
            continue

        bounds_match = re.search(
            r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node_attr
        )
        if bounds_match:
            x1, y1, x2, y2 = map(int, bounds_match.groups())
            return ((x1 + x2) // 2, (y1 + y2) // 2)
    return None


def click_element(
    resource_id=None, text=None, text_contains=None, pkg=None, wait=1.5
):
    for _ in range(5):
        xml = dump_ui()
        center = find_node_center(
            xml,
            resource_id=resource_id,
            text=text,
            text_contains=text_contains,
            pkg=pkg,
        )
        if center:
            cx, cy = center
            print(
                f"Clicking element ({resource_id or text or text_contains}) at ({cx}, {cy})"
            )
            adb_shell(f"input tap {cx} {cy}")
            time.sleep(wait)
            return True
        time.sleep(1)
    print(
        f"Warning: Element ({resource_id or text or text_contains}) not found"
    )
    return False


def get_current_focus_package():
    res = adb_shell("dumpsys window | grep mCurrentFocus", check=False)
    out = res.stdout
    print(f"Current focus: {out.strip()}")
    match = re.search(r"(\b[a-zA-Z0-9_.]+\b)/", out)
    if match:
        return match.group(1)
    return ""


def test_upgrade_flow():
    print(
        "=== Step 4: Uninstalling existing app and installing re-signed previous version ==="
    )
    adb(f"uninstall {PACKAGE_NAME}", check=False)
    adb(f"install -r -g {PREVIOUS_APK_RESIGNED}")

    print("=== Step 5: Launching previous version (2.4.1) ===")
    adb_shell(f"am start -n {MAIN_ACTIVITY}")
    time.sleep(3)

    print("=== Step 6: Dismissing disclaimer dialog if shown ===")
    click_element(
        resource_id="android:id/button1", text="OK", wait=2
    ) or click_element(text="OK", wait=2)

    print("=== Step 7: Searching for com.android.settings ===")
    if click_element(resource_id=f"{PACKAGE_NAME}:id/tiSearch", wait=1):
        adb_shell("input text com.android.settings")
        time.sleep(2)

    print("=== Step 8: Selecting com.android.settings package ===")
    if not click_element(
        resource_id=f"{PACKAGE_NAME}:id/tvClass",
        text_contains="com.android.settings",
        wait=2,
    ):
        if not click_element(resource_id=f"{PACKAGE_NAME}:id/tvName", wait=2):
            adb_shell("input tap 500 520")
            time.sleep(2)

    print("=== Step 9: Selecting activity ===")
    if not click_element(resource_id=f"{PACKAGE_NAME}:id/tvName", wait=2):
        adb_shell("input tap 500 520")
        time.sleep(2)

    print(
        f"=== Step 10: Setting shortcut name to '{SHORTCUT_LABEL}' ==="
    )
    click_element(resource_id=f"{PACKAGE_NAME}:id/tiName", wait=1)
    for _ in range(30):
        adb_shell("input keyevent 67", check=False)
    adb_shell(f"input text {SHORTCUT_LABEL.replace(' ', '\\ ')}")
    time.sleep(1)

    print("=== Step 11: Clicking 'Create shortcut' button ===")
    adb_shell("input swipe 500 1200 500 400 300")
    time.sleep(1)
    if not click_element(
        resource_id=f"{PACKAGE_NAME}:id/btCreateShortcut", wait=2
    ):
        click_element(text_contains="Create", wait=2)

    print("=== Step 12: Confirming system pin shortcut dialog ===")
    time.sleep(1)
    if not click_element(text_contains="Add automatically", wait=2):
        if not click_element(text_contains="Add to Home", wait=2):
            click_element(resource_id="android:id/button1", wait=2)

    print("=== Step 13: Navigating to Home screen ===")
    adb_shell("input keyevent KEYCODE_HOME")
    time.sleep(2)

    print("=== Step 14: Upgrading in-place to current version ===")
    adb(f"install -r -g {NEW_APK_PATH}")
    time.sleep(2)

    adb_shell("input keyevent KEYCODE_HOME")
    time.sleep(2)

    print(
        f"=== Step 15: Clicking created shortcut '{SHORTCUT_LABEL}' on Home screen ==="
    )
    shortcut_found = False
    for attempt in range(4):
        xml = dump_ui()
        for match in re.finditer(r"<node ([^>]+)>", xml):
            attr = match.group(1)
            if (
                SHORTCUT_LABEL.lower() in attr.lower()
                and "com.android.systemui" not in attr
            ):
                bounds_match = re.search(
                    r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attr
                )
                if bounds_match:
                    x1, y1, x2, y2 = map(int, bounds_match.groups())
                    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
                    print(f"Found shortcut at ({cx}, {cy}). Clicking...")
                    adb_shell(f"input tap {cx} {cy}")
                    shortcut_found = True
                    break
        if shortcut_found:
            break
        print(f"Page {attempt+1}: Shortcut not found, swiping...")
        adb_shell("input swipe 800 1000 200 1000 300")
        time.sleep(2)

    if not shortcut_found:
        print("ERROR: Could not locate shortcut on Home screen!")
        sys.exit(1)

    time.sleep(3)

    print("=== Step 16: Validating that com.android.settings was launched ===")
    success = False
    for _ in range(5):
        current_pkg = get_current_focus_package()
        print(f"Current package: {current_pkg}")
        if current_pkg == TARGET_PACKAGE:
            success = True
            break
        if current_pkg == "android":
            print("ResolverActivity shown, selecting 'Just once' / default option...")
            if not click_element(text_contains="Just once", wait=2):
                click_element(resource_id="android:id/button_once", wait=2)
            time.sleep(2)
        else:
            time.sleep(1)

    if success or get_current_focus_package() == TARGET_PACKAGE:
        print("\n==========================================")
        print(" SUCCESS: Upgrade test passed 100%!")
        print(
            f" Shortcut created in v{PREVIOUS_RELEASE_TAG} worked after update to current version!"
        )
        print("==========================================\n")
    else:
        current_pkg = get_current_focus_package()
        print("\n==========================================")
        print(
            f" FAILURE: Expected {TARGET_PACKAGE}, but active package is '{current_pkg}'"
        )
        print("==========================================\n")
        sys.exit(1)


if __name__ == "__main__":
    setup_environment()
    test_upgrade_flow()
