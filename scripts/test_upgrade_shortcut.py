#!/usr/bin/env python3
import os
import re
import subprocess
import sys
import time

PACKAGE_NAME = "de.szalkowski.activitylauncher.oss"
MAIN_ACTIVITY = f"{PACKAGE_NAME}/de.szalkowski.activitylauncher.entrypoint.MainActivity"
TARGET_PACKAGE = "com.android.settings"

PREVIOUS_RELEASE_TAG = "2.4.1"
PREVIOUS_APK_ORIGINAL = "/tmp/previous_release/app-oss-noads-release.apk"
PREVIOUS_APK_RESIGNED = "/tmp/previous_release_debug.apk"
NEW_APK_PATH = "app/build/outputs/apk/ossNoads/debug/app-oss-noads-debug.apk"

SNAPSHOT_COUNTER = 0


def get_env():
    env = os.environ.copy()
    env["APPID"] = "de.szalkowski.activitylauncher.oss"
    env["HOME"] = os.path.expanduser("~")
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        env["JAVA_HOME"] = java_home
        bin_path = os.path.join(java_home, "bin")
        env["PATH"] = f"{bin_path}:{env.get('PATH', '')}"
    return env


def run_cmd(cmd, check=True):
    print(f"Executing: {cmd}")
    env = get_env()
    res = subprocess.run(
        cmd, shell=True, capture_output=True, text=True, env=env
    )
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


class UiDevice:
    def __init__(self):
        self.w, self.h = self._get_screen_size()

    def _get_screen_size(self):
        res = adb_shell("wm size", check=False)
        match = re.search(r"(\d+)x(\d+)", res.stdout)
        if match:
            return int(match.group(1)), int(match.group(2))
        return 1080, 1920

    def dump_hierarchy(self):
        adb_shell("uiautomator dump /sdcard/window_dump.xml >/dev/null", check=False)
        res = adb_shell("cat /sdcard/window_dump.xml", check=False)
        return res.stdout

    def screenshot(self, path):
        adb_shell("screencap -p /sdcard/screen.png", check=False)
        adb(f"pull /sdcard/screen.png {path}", check=False)

    def press(self, key):
        if key == "home":
            adb_shell("input keyevent KEYCODE_HOME")
        elif key == "back":
            adb_shell("input keyevent KEYCODE_BACK")
        elif key == "enter":
            adb_shell("input keyevent KEYCODE_ENTER")
        elif key == "tab":
            adb_shell("input keyevent KEYCODE_TAB")

    def scroll_down(self):
        cx = self.w // 2
        start_y = int(self.h * 0.75)
        end_y = int(self.h * 0.25)
        print(f"Scrolling down: swiping from ({cx}, {start_y}) to ({cx}, {end_y}) on {self.w}x{self.h} screen")
        adb_shell(f"input swipe {cx} {start_y} {cx} {end_y} 300")

    def swipe_left(self):
        cy = self.h // 2
        start_x = int(self.w * 0.8)
        end_x = int(self.w * 0.2)
        print(f"Swiping left: from ({start_x}, {cy}) to ({end_x}, {cy}) on {self.w}x{self.h} screen")
        adb_shell(f"input swipe {start_x} {cy} {end_x} {cy} 300")

    def app_start(self, package, activity):
        adb_shell(f"am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n {package}/{activity}")

    def current_package(self):
        res = adb_shell("dumpsys window | grep mCurrentFocus", check=False)
        match = re.search(r"(\b[a-zA-Z0-9_.]+\b)/", res.stdout)
        return match.group(1) if match else ""

    def click(self, resource_id=None, text=None, text_contains=None, wait=1.5, retries=5):
        for attempt in range(retries):
            xml = self.dump_hierarchy()
            xml_lower = xml.lower()
            if "isn't responding" in xml_lower or "aerr_" in xml_lower or "stylus" in xml_lower:
                print("System ANR or overlay detected during click attempt, dismissing...")
                dismiss_system_prompts(self)
                xml = self.dump_hierarchy()

            for match in re.finditer(r"<node ([^>]+)>", xml):
                attr = match.group(1)
                if resource_id and f'resource-id="{resource_id}"' not in attr:
                    continue
                if text and f'text="{text}"' not in attr:
                    continue
                if text_contains:
                    tm = re.search(r'text="([^"]*)"', attr)
                    dm = re.search(r'content-desc="([^"]*)"', attr)
                    node_text = (tm.group(1) if tm else "") + " " + (dm.group(1) if dm else "")
                    if text_contains.lower() not in node_text.lower():
                        continue

                bm = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attr)
                if bm:
                    x1, y1, x2, y2 = map(int, bm.groups())
                    cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
                    print(f"Clicking '{text or resource_id or text_contains}' at ({cx}, {cy})")
                    adb_shell(f"input tap {cx} {cy}")
                    time.sleep(wait)
                    return True
            time.sleep(1)
        return False


def disable_stylus_and_keyboard_prompts():
    adb_shell("settings put secure stylus_handwriting_enabled 0", check=False)
    adb_shell("settings put secure show_stylus_handwriting_pointer 0", check=False)
    adb_shell("settings put global stylus_handwriting_enabled 0", check=False)


def check_installed_packages_and_version():
    res = adb_shell("pm list packages --show-versioncode", check=False)
    al_packages = []
    for line in res.stdout.splitlines():
        if "activitylauncher" in line:
            pkg_info = line.replace("package:", "").strip()
            al_packages.append(pkg_info)
    return al_packages


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


def strip_signatures_and_resign(apk_path, keystore):
    print(f"Stripping old signatures from {apk_path}...")
    run_cmd(
        f'zip -d {apk_path} "META-INF/*.SF" "META-INF/*.RSA" "META-INF/*.DSA" "META-INF/*.EC" "META-INF/MANIFEST.MF"',
        check=False,
    )
    apksigner = find_apksigner()
    sign_cmd = f'"{apksigner}" sign --ks {keystore} --ks-pass pass:android --key-pass pass:android {apk_path}'
    run_cmd(sign_cmd)


def setup_environment():
    print(
        f"=== Step 1: Downloading previous release ({PREVIOUS_RELEASE_TAG}) from GitHub ==="
    )
    run_cmd(
        f"gh release download {PREVIOUS_RELEASE_TAG} --repo ActivityLauncher/ActivityLauncher --pattern 'app-oss-noads-release.apk' --dir /tmp/previous_release --clobber"
    )

    print("=== Step 2: Ensuring debug keystore exists ===")
    keystore_dir = os.path.expanduser("~/.android")
    os.makedirs(keystore_dir, exist_ok=True)
    keystore = os.path.join(keystore_dir, "debug.keystore")
    if not os.path.exists(keystore):
        print(f"Creating debug keystore at {keystore}...")
        keytool_cmd = f"keytool -genkey -v -keystore {keystore} -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Android Debug,O=Android,C=US'"
        run_cmd(keytool_cmd)

    print("=== Step 3: Re-signing previous release APK with local debug keystore ===")
    run_cmd(f"cp {PREVIOUS_APK_ORIGINAL} {PREVIOUS_APK_RESIGNED}")
    strip_signatures_and_resign(PREVIOUS_APK_RESIGNED, keystore)
    print(f"Previous release re-signed at {PREVIOUS_APK_RESIGNED}")

    print(
        "=== Step 4: Building current version APK with APPID=de.szalkowski.activitylauncher.oss ==="
    )
    if not os.path.exists(NEW_APK_PATH):
        print(f"Current version APK not found at {NEW_APK_PATH}, building it now...")
        run_cmd(
            "./gradlew app:assembleOssNoadsDebug -PAPPID=de.szalkowski.activitylauncher.oss"
        )
    if not os.path.exists(NEW_APK_PATH):
        print(f"Error: Current version APK not found at {NEW_APK_PATH} after build!")
        sys.exit(1)

    print("=== Step 5: Re-signing current version APK with exact same debug keystore ===")
    strip_signatures_and_resign(NEW_APK_PATH, keystore)
    print(f"Current version APK verified and re-signed at {NEW_APK_PATH}")


def log_ui_summary(xml_content, label="", max_nodes=50):
    nodes = []
    for match in re.finditer(r"<node ([^>]+)>", xml_content):
        attr = match.group(1)
        tm = re.search(r'text="([^"]*)"', attr)
        dm = re.search(r'content-desc="([^"]*)"', attr)
        rid = re.search(r'resource-id="([^"]*)"', attr)
        cls = re.search(r'class="([^"]*)"', attr)
        pkg = re.search(r'package="([^"]*)"', attr)
        bm = re.search(r'bounds="([^"]*)"', attr)

        text = tm.group(1) if tm else ""
        desc = dm.group(1) if dm else ""
        res_id = rid.group(1) if rid else ""
        class_name = cls.group(1).split(".")[-1] if cls else ""
        package = pkg.group(1) if pkg else ""
        bounds = bm.group(1) if bm else ""

        displayed_text = f"text='{text}'" if text else ""
        if desc:
            displayed_text += (
                f" desc='{desc}'" if displayed_text else f"desc='{desc}'"
            )

        if text or desc or res_id:
            short_id = res_id.split("/")[-1] if "/" in res_id else res_id
            nodes.append(
                f"  - [{package}] {class_name} ({short_id}) {displayed_text} {bounds}"
            )

    print(f"--- UI Screen Summary [{label}] ({len(nodes)} labeled nodes) ---")
    if nodes:
        for n in nodes[:max_nodes]:
            print(n)
        if len(nodes) > max_nodes:
            print(f"  ... and {len(nodes) - max_nodes} more nodes")
    else:
        print("  (No text/resource-id nodes found in XML dump)")
    print("---------------------------------------------------------")


def save_snapshot(d, label="snapshot"):
    global SNAPSHOT_COUNTER
    SNAPSHOT_COUNTER += 1
    artifacts_dir = "upgrade-test-artifacts"
    os.makedirs(artifacts_dir, exist_ok=True)

    sanitized_label = re.sub(r"[^a-zA-Z0-9_-]", "_", str(label))[:50]
    prefix = os.path.join(
        artifacts_dir, f"{SNAPSHOT_COUNTER:02d}_{sanitized_label}"
    )

    xml_path = f"{prefix}.xml"
    png_path = f"{prefix}.png"

    try:
        xml_content = d.dump_hierarchy()
        with open(xml_path, "w", encoding="utf-8") as f:
            f.write(xml_content)
        log_ui_summary(xml_content, label=sanitized_label)
    except Exception as e:
        print(f"Error dumping hierarchy: {e}")

    try:
        d.screenshot(png_path)
    except Exception as e:
        print(f"Error taking screenshot: {e}")


def dismiss_system_prompts(d):
    disable_stylus_and_keyboard_prompts()
    xml = d.dump_hierarchy()
    xml_lower = xml.lower()
    if any(
        k in xml_lower
        for k in [
            "stylus",
            "got it",
            "skip",
            "allow",
            "permission",
            "welcome",
            "keyboard",
            "isn't responding",
            "is not responding",
            "close app",
            "aerr_close",
            "aerr_wait",
            "wait",
        ]
    ):
        print("System prompt/ANR dialog detected, dismissing...")
        if "aerr_wait" in xml or "wait" in xml_lower:
            d.click(resource_id="android:id/aerr_wait") or d.click(text="Wait")
        elif "aerr_close" in xml or "close app" in xml_lower:
            d.click(resource_id="android:id/aerr_close") or d.click(text="Close app")

        d.click(text_contains="Got it") or \
        d.click(text_contains="SKIP") or \
        d.click(text_contains="Allow") or \
        d.click(resource_id="android:id/button1")
        d.press("back")
        time.sleep(1)


def ensure_app_launched(d, package_name, activity_name):
    disable_stylus_and_keyboard_prompts()
    for _ in range(8):
        adb_shell("input keyevent KEYCODE_WAKEUP", check=False)
        adb_shell("wm dismiss-keyguard", check=False)
        d.scroll_down()
        time.sleep(1)

        xml = d.dump_hierarchy()
        xml_lower = xml.lower()
        if 'package="android"' in xml or "application error" in xml_lower or "isn't responding" in xml_lower or "stylus" in xml_lower:
            print("System/ANR/crash dialog detected, attempting to clear...")
            dismiss_system_prompts(d)
            adb_shell("input keyevent KEYCODE_BACK", check=False)
            time.sleep(1)

        d.app_start(package_name, activity_name)
        time.sleep(2)
        focus = d.current_package()
        if package_name in focus:
            print(f"App {package_name} is in focus!")
            return True
        time.sleep(1)
    print(f"Warning: Could not focus {package_name}")
    save_snapshot(d, "cannot_focus_app")
    return False


def test_upgrade_flow():
    d = UiDevice()
    disable_stylus_and_keyboard_prompts()
    print(f"=== Uninstalling existing package {PACKAGE_NAME} ===")
    adb(f"uninstall {PACKAGE_NAME}", check=False)

    print("=== Step 5: Installing re-signed previous version (2.4.1) ===")
    adb(f"install -r -g {PREVIOUS_APK_RESIGNED}")

    # Check installed packages after v2.4.1 install
    packages_v1 = check_installed_packages_and_version()
    print(f"Installed Activity Launcher packages (v2.4.1): {packages_v1}")
    if len(packages_v1) != 1 or PACKAGE_NAME not in packages_v1[0]:
        print(f"ERROR: Expected exactly 1 package ({PACKAGE_NAME}), but found: {packages_v1}")
        save_snapshot(d, "error_v1_packages_mismatch")
        sys.exit(1)

    print("=== Step 6: Launching previous version (2.4.1) ===")
    ensure_app_launched(
        d, PACKAGE_NAME, "de.szalkowski.activitylauncher.entrypoint.MainActivity"
    )
    save_snapshot(d, "step06_app_launched")

    print("=== Step 7: Dismissing disclaimer dialog if shown ===")
    d.click(resource_id="android:id/button1", text="OK", wait=2) or d.click(text="OK", wait=2)
    save_snapshot(d, "step07_disclaimer_done")

    print("=== Step 8: Searching for com.android.settings ===")
    dismiss_system_prompts(d)
    save_snapshot(d, "step08_before_search")
    if d.click(resource_id=f"{PACKAGE_NAME}:id/tiSearch", wait=1.5):
        time.sleep(1)
        dismiss_system_prompts(d)
        print("Typing com.android.settings into search field...")
        adb_shell("input text com.android.settings")
        time.sleep(2)
        d.press("enter")
        time.sleep(1)
        d.press("back")
        time.sleep(1)
    save_snapshot(d, "step08_after_search")

    print("=== Step 9: Selecting com.android.settings package ===")
    time.sleep(2)
    if not d.click(resource_id=f"{PACKAGE_NAME}:id/tvClass", text="com.android.settings", wait=2, retries=5):
        if not d.click(text="com.android.settings", wait=2, retries=5):
            print("ERROR: Could not find package com.android.settings in list!")
            save_snapshot(d, "error_package_not_found")
            sys.exit(1)
    save_snapshot(d, "step09_after_package_select")

    print("=== Step 10: Selecting activity ===")
    time.sleep(2)
    if not d.click(resource_id=f"{PACKAGE_NAME}:id/tvName", text="Settings", wait=2, retries=5):
        if not d.click(text="Settings", wait=2, retries=5):
            print("ERROR: Could not find Settings activity in list!")
            save_snapshot(d, "error_activity_not_found")
            sys.exit(1)
    save_snapshot(d, "step10_after_activity_select")

    print("=== Step 11: Clicking 'Create shortcut' button ===")
    save_snapshot(d, "step11_before_create_shortcut")
    if not d.click(resource_id=f"{PACKAGE_NAME}:id/btCreateShortcut", wait=2, retries=3):
        print("Scrolling down to find Create Shortcut button...")
        d.scroll_down()
        time.sleep(1.5)
        save_snapshot(d, "step11_after_swipe")
        if not d.click(resource_id=f"{PACKAGE_NAME}:id/btCreateShortcut", wait=2, retries=3):
            print("ERROR: Could not find Create Shortcut button!")
            save_snapshot(d, "error_btCreateShortcut_not_found")
            sys.exit(1)

    print("=== Step 12: Confirming System Pin Shortcut dialog ===")
    time.sleep(2)
    save_snapshot(d, "step12_pin_dialog_check")
    xml = d.dump_hierarchy()

    if "Complete action using" in xml or "ResolverActivity" in xml:
        print("ERROR: App selector/ResolverActivity shown during shortcut creation!")
        save_snapshot(d, "error_resolver_activity_in_pin")
        sys.exit(1)

    print("Confirming System Pin Shortcut dialog...")
    clicked_pin = False
    for attempt in range(10):
        time.sleep(1.5)
        xml = d.dump_hierarchy()
        for match in re.finditer(r'<node ([^>]+)>', xml):
            attr = match.group(1)
            node_text = ""
            tm = re.search(r'text="([^"]*)"', attr)
            if tm: node_text += tm.group(1)
            dm = re.search(r'content-desc="([^"]*)"', attr)
            if dm: node_text += " " + dm.group(1)

            if (re.search(r'(?i)(add|allow|ok|pin)', node_text) or
                'button1' in attr or 'btn_add' in attr):
                bounds_match = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attr)
                if bounds_match:
                    x1, y1, x2, y2 = map(int, bounds_match.groups())
                    if y1 > 200 and (x2 - x1) > 20:
                        cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
                        print(f"Pin dialog button '{node_text.strip()}' found at ({cx}, {cy}). Clicking...")
                        adb_shell(f"input tap {cx} {cy}")
                        clicked_pin = True
                        time.sleep(2)
                        break
        if clicked_pin:
            break

    if not clicked_pin:
        print("Fallback pin dialog navigation...")
        save_snapshot(d, "step12_fallback_pin_navigation")
        d.press("tab")
        d.press("tab")
        d.press("enter")
        time.sleep(1.5)

    save_snapshot(d, "step12_after_pin")

    print("=== Step 13: Navigating to Home screen ===")
    d.press("home")
    time.sleep(2)
    save_snapshot(d, "step13_home_screen")

    print("=== Step 14: Upgrading in-place to current version ===")
    adb(f"install -r -g {NEW_APK_PATH}")
    time.sleep(2)

    # Verify that in-place upgrade updated the existing package and didn't install a separate package
    packages_v2 = check_installed_packages_and_version()
    print(f"Installed Activity Launcher packages (after in-place upgrade): {packages_v2}")
    if len(packages_v2) != 1:
        print(f"ERROR: Multiple or zero Activity Launcher packages found after upgrade! Packages: {packages_v2}")
        save_snapshot(d, "error_multiple_packages_after_upgrade")
        sys.exit(1)
    if PACKAGE_NAME not in packages_v2[0]:
        print(f"ERROR: Package name changed after upgrade! Expected {PACKAGE_NAME}, got: {packages_v2[0]}")
        save_snapshot(d, "error_package_name_changed")
        sys.exit(1)

    d.press("home")
    time.sleep(2)
    save_snapshot(d, "step14_upgraded_home_screen")

    print("=== Step 15: Clicking created shortcut 'Settings' on Home screen ===")
    shortcut_found = False
    for attempt in range(4):
        save_snapshot(d, f"step15_home_page_{attempt+1}")
        xml = d.dump_hierarchy()
        for match in re.finditer(r"<node ([^>]+)>", xml):
            attr = match.group(1)
            if (
                "settings" in attr.lower()
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
        d.swipe_left()
        time.sleep(2)

    if not shortcut_found:
        print("ERROR: Could not locate shortcut on Home screen!")
        save_snapshot(d, "step15_error_shortcut_not_found")
        sys.exit(1)

    time.sleep(3)

    print("=== Step 16: Validating that com.android.settings was launched directly ===")
    save_snapshot(d, "step16_before_validation")
    success = False
    for _ in range(5):
        current_pkg = d.current_package()
        print(f"Current package: {current_pkg}")
        if current_pkg == TARGET_PACKAGE:
            success = True
            break
        if current_pkg == "android":
            print("ERROR: App selector/ResolverActivity shown when clicking shortcut! Expected direct launch.")
            save_snapshot(d, "error_app_selector_shown")
            sys.exit(1)
        time.sleep(1)

    save_snapshot(d, "step16_after_validation")

    if success or d.current_package() == TARGET_PACKAGE:
        print("\n==========================================")
        print(" SUCCESS: Upgrade test passed 100%!")
        print(
            f" Shortcut created in v{PREVIOUS_RELEASE_TAG} worked after update to current version directly without app selector!"
        )
        print("==========================================\n")
    else:
        current_pkg = d.current_package()
        print("\n==========================================")
        print(
            f" FAILURE: Expected {TARGET_PACKAGE}, but active package is '{current_pkg}'"
        )
        print("==========================================\n")
        sys.exit(1)


if __name__ == "__main__":
    try:
        setup_environment()
        test_upgrade_flow()
    except Exception as e:
        print(f"FATAL ERROR in test execution: {e}")
        sys.exit(1)
