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
    home_dir = os.path.expanduser("~")
    if "HOME" not in env:
        env["HOME"] = home_dir

    java_in_path = (
        subprocess.run(
            "which java", shell=True, capture_output=True, text=True, env=env
        ).returncode
        == 0
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


def get_screen_size():
    res = adb_shell("wm size", check=False)
    match = re.search(r"(\d+)x(\d+)", res.stdout)
    if match:
        return int(match.group(1)), int(match.group(2))
    return 1080, 1920


def swipe_scroll_down():
    w, h = get_screen_size()
    cx = w // 2
    start_y = int(h * 0.75)
    end_y = int(h * 0.25)
    print(
        f"Scrolling down: swiping from ({cx}, {start_y}) to ({cx}, {end_y}) on {w}x{h} screen"
    )
    adb_shell(f"input swipe {cx} {start_y} {cx} {end_y} 300")


def swipe_page_left():
    w, h = get_screen_size()
    cy = h // 2
    start_x = int(w * 0.8)
    end_x = int(w * 0.2)
    print(
        f"Swiping to next page: from ({start_x}, {cy}) to ({end_x}, {cy}) on {w}x{h} screen"
    )
    adb_shell(f"input swipe {start_x} {cy} {end_x} {cy} 300")


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


def dump_ui():
    adb_shell("uiautomator dump /sdcard/window_dump.xml >/dev/null", check=False)
    res = adb_shell("cat /sdcard/window_dump.xml", check=False)
    return res.stdout


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


def save_snapshot(label="snapshot"):
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

    adb_shell("uiautomator dump /sdcard/window_dump.xml >/dev/null", check=False)
    adb(f"pull /sdcard/window_dump.xml {xml_path}", check=False)

    adb_shell("screencap -p /sdcard/screen.png", check=False)
    adb(f"pull /sdcard/screen.png {png_path}", check=False)

    if os.path.exists(xml_path):
        try:
            with open(xml_path, "r", encoding="utf-8", errors="ignore") as f:
                xml_content = f.read()
            log_ui_summary(xml_content, label=sanitized_label)
        except Exception as e:
            print(f"Error reading UI dump {xml_path}: {e}")


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
    resource_id=None,
    text=None,
    text_contains=None,
    pkg=None,
    wait=1.5,
    retries=5,
    label=None,
):
    for attempt in range(retries):
        xml = dump_ui()
        for match in re.finditer(r"<node ([^>]+)>", xml):
            attr = match.group(1)
            if resource_id and f'resource-id="{resource_id}"' not in attr:
                continue
            if text and f'text="{text}"' not in attr:
                continue
            if text_contains:
                text_match = re.search(r'text="([^"]*)"', attr)
                desc_match = re.search(r'content-desc="([^"]*)"', attr)
                node_text = (
                    (text_match.group(1) if text_match else "")
                    + " "
                    + (desc_match.group(1) if desc_match else "")
                )
                if text_contains.lower() not in node_text.lower():
                    continue
            if pkg and f'package="{pkg}"' not in attr:
                continue

            bounds_match = re.search(
                r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', attr
            )
            if bounds_match:
                x1, y1, x2, y2 = map(int, bounds_match.groups())
                cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
                print(f"Clicking node '{attr[:100]}' at ({cx}, {cy})")
                adb_shell(f"input tap {cx} {cy}")
                time.sleep(wait)
                return True
        time.sleep(1)

    target_desc = resource_id or text or text_contains
    print(
        f"Warning: Element ({target_desc}) not found after {retries} retries."
    )
    save_snapshot(f"missing_{label or target_desc}")
    return False


def get_current_focus_package():
    res = adb_shell("dumpsys window | grep mCurrentFocus", check=False)
    out = res.stdout
    print(f"Current focus: {out.strip()}")
    match = re.search(r"(\b[a-zA-Z0-9_.]+\b)/", out)
    if match:
        return match.group(1)
    return ""


def ensure_app_launched(package_name, activity_name):
    for attempt in range(8):
        adb_shell("input keyevent KEYCODE_WAKEUP", check=False)
        adb_shell("wm dismiss-keyguard", check=False)
        swipe_scroll_down()
        time.sleep(1)

        xml = dump_ui()
        if 'package="android"' in xml or "Application Error" in xml:
            print("System/crash dialog detected, attempting to clear...")
            click_element(
                resource_id="android:id/aerr_close", wait=1, retries=1
            ) or click_element(
                text_contains="Allow", wait=1, retries=1
            ) or click_element(
                resource_id="android:id/button1", wait=1, retries=1
            )
            adb_shell("input keyevent KEYCODE_BACK", check=False)
            time.sleep(1)

        adb_shell(
            f"am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n {package_name}/{activity_name}"
        )
        time.sleep(2)
        focus = get_current_focus_package()
        if package_name in focus:
            print(f"App {package_name} is in focus!")
            return True
        time.sleep(1)
    print(f"Warning: Could not focus {package_name}")
    save_snapshot("cannot_focus_app")
    return False


def test_upgrade_flow():
    w, h = get_screen_size()
    print(
        "=== Step 4: Uninstalling existing versions of Activity Launcher ==="
    )
    # Ensure no duplicate versions exist on device
    adb("uninstall de.szalkowski.activitylauncher", check=False)
    adb("uninstall de.szalkowski.activitylauncher.oss", check=False)

    print("=== Step 5: Installing re-signed previous version (2.4.1) ===")
    adb(f"install -r -g {PREVIOUS_APK_RESIGNED}")

    print("=== Step 6: Launching previous version (2.4.1) ===")
    ensure_app_launched(
        PACKAGE_NAME, "de.szalkowski.activitylauncher.entrypoint.MainActivity"
    )
    save_snapshot("step06_app_launched")

    print("=== Step 7: Dismissing disclaimer dialog if shown ===")
    click_element(
        resource_id="android:id/button1", text="OK", wait=2, label="disclaimer_ok"
    ) or click_element(text="OK", wait=2, label="disclaimer_ok")
    save_snapshot("step07_disclaimer_done")

    print("=== Step 8: Searching for com.android.settings ===")
    save_snapshot("step08_before_search")
    if click_element(resource_id=f"{PACKAGE_NAME}:id/tiSearch", wait=1, label="search_input"):
        adb_shell("input text com.android.settings")
        time.sleep(1)
        adb_shell("input keyevent 111")
        time.sleep(1)
    save_snapshot("step08_after_search")

    print("=== Step 9: Selecting com.android.settings package ===")
    if not click_element(
        resource_id=f"{PACKAGE_NAME}:id/tvClass",
        text_contains="com.android.settings",
        wait=2,
        label="select_package_class",
    ):
        if not click_element(resource_id=f"{PACKAGE_NAME}:id/tvName", wait=2, label="select_package_name"):
            adb_shell(f"input tap {w // 2} {int(h * 0.25)}")
            time.sleep(2)
    save_snapshot("step09_after_package_select")

    print("=== Step 10: Selecting activity ===")
    if not click_element(resource_id=f"{PACKAGE_NAME}:id/tvName", wait=2, label="select_activity"):
        adb_shell(f"input tap {w // 2} {int(h * 0.25)}")
        time.sleep(2)
    save_snapshot("step10_after_activity_select")

    print("=== Step 11: Clicking 'Create shortcut' button ===")
    save_snapshot("step11_before_create_shortcut")
    if not click_element(resource_id=f"{PACKAGE_NAME}:id/btCreateShortcut", wait=2, label="create_shortcut"):
        print("Swiping down to find Create Shortcut button...")
        swipe_scroll_down()
        time.sleep(1.5)
        save_snapshot("step11_after_swipe")
        click_element(resource_id=f"{PACKAGE_NAME}:id/btCreateShortcut", wait=2, label="create_shortcut_retry")

    print("=== Step 12: Confirming System Pin Shortcut dialog ===")
    time.sleep(2)
    save_snapshot("step12_pin_dialog_check")
    xml = dump_ui()

    if "Complete action using" in xml or "ResolverActivity" in xml:
        print("ResolverActivity/Chooser shown for shortcut creation, selecting 'Activity Launcher'...")
        click_element(text_contains="Activity Launcher", wait=2, retries=3, label="resolver_activity_launcher") or adb_shell(f"input tap {w // 2} {int(h * 0.8)}")
        time.sleep(2)
        save_snapshot("step12_after_resolver")

    print("Confirming System Pin Shortcut dialog...")
    clicked_pin = False
    for attempt in range(10):
        time.sleep(1.5)
        xml = dump_ui()
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
        save_snapshot("step12_fallback_pin_navigation")
        adb_shell("input keyevent KEYCODE_TAB", check=False)
        adb_shell("input keyevent KEYCODE_TAB", check=False)
        adb_shell("input keyevent KEYCODE_ENTER", check=False)
        time.sleep(1.5)

    save_snapshot("step12_after_pin")

    print("=== Step 13: Navigating to Home screen ===")
    adb_shell("input keyevent KEYCODE_HOME")
    time.sleep(2)
    save_snapshot("step13_home_screen")

    print("=== Step 14: Upgrading in-place to current version ===")
    adb(f"install -r -g {NEW_APK_PATH}")
    time.sleep(2)

    adb_shell("input keyevent KEYCODE_HOME")
    time.sleep(2)
    save_snapshot("step14_upgraded_home_screen")

    print("=== Step 15: Clicking created shortcut 'Settings' on Home screen ===")
    shortcut_found = False
    for attempt in range(4):
        save_snapshot(f"step15_home_page_{attempt+1}")
        xml = dump_ui()
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
        swipe_page_left()
        time.sleep(2)

    if not shortcut_found:
        print("ERROR: Could not locate shortcut on Home screen!")
        save_snapshot("step15_error_shortcut_not_found")
        sys.exit(1)

    time.sleep(3)

    print("=== Step 16: Validating that com.android.settings was launched ===")
    save_snapshot("step16_before_validation")
    success = False
    for _ in range(5):
        current_pkg = get_current_focus_package()
        print(f"Current package: {current_pkg}")
        if current_pkg == TARGET_PACKAGE:
            success = True
            break
        if current_pkg == "android":
            print("ResolverActivity shown on shortcut launch, selecting Activity Launcher handler...")
            click_element(text_contains="Activity Launcher", wait=1.5, label="resolver_activity_launcher_launch") or click_element(resource_id="android:id/text1", wait=1.5, label="resolver_text1")
            click_element(text_contains="Just once", wait=2, label="resolver_just_once") or click_element(resource_id="android:id/button_once", wait=2, label="resolver_button_once")
            time.sleep(2)
        else:
            time.sleep(1)

    save_snapshot("step16_after_validation")

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
    try:
        setup_environment()
        test_upgrade_flow()
    except Exception as e:
        print(f"FATAL ERROR in test execution: {e}")
        save_snapshot("fatal_error")
        sys.exit(1)
