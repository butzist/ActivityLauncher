#!/usr/bin/env python3
import os
import re
import xml.etree.ElementTree as ET
from pathlib import Path

def get_strings(path):
    try:
        tree = ET.parse(path)
        root = tree.getroot()
        strings = {}
        for string in root.findall('string'):
            name = string.get('name')
            text = string.text or ""
            # Handle cases where there are child elements (like <b>)
            if len(string) > 0:
                text = ET.tostring(string, encoding='unicode', method='xml')
                # Remove <string ...> and </string>
                text = re.sub(r'^<string[^>]*>', '', text)
                text = re.sub(r'</string>$', '', text)
            strings[name] = text
        return strings
    except Exception as e:
        print(f"Error parsing {path}: {e}")
        return None

def get_placeholders(text):
    # Matches %d, %s, %1$d, %2$s, etc.
    return re.findall(r'%[0-9]*\$?[a-zA-Z]', text)

def main():
    script_dir = Path(__file__).resolve().parent
    project_root = script_dir.parent
    res_dir = project_root / "app/src/main/res"
    base_path = res_dir / "values/strings.xml"

    base_strings = get_strings(base_path)
    if base_strings is None:
        return

    translation_files = sorted(list(res_dir.glob("values-*/strings.xml")))

    issues = []

    # Technical terms or strings that often remain untranslated
    likely_same = {
        "app_name",
        "label_class",
        "label_package",
        "label_intent_action",
        "label_intent_data",
        "label_intent_categories",
        "label_intent_extra_key",
        "label_intent_extra_value",
        "label_intent_extras",
        "label_intent_mime_type",
    }

    for trans_path in translation_files:
        locale = trans_path.parent.name
        trans_strings = get_strings(trans_path)
        if trans_strings is None:
            issues.append(f"[{locale}] FAILED TO PARSE (invalid XML)")
            continue

        # 1. Check for missing/extra keys
        missing = set(base_strings.keys()) - set(trans_strings.keys())
        if missing:
            issues.append(f"[{locale}] Missing keys: {', '.join(sorted(missing))}")

        extra = set(trans_strings.keys()) - set(base_strings.keys())
        if extra:
            issues.append(f"[{locale}] Extra keys: {', '.join(sorted(extra))}")

        # 2. Check for placeholder mismatches
        for key in base_strings.keys():
            if key in trans_strings:
                base_placeholders = sorted(get_placeholders(base_strings[key]))
                trans_placeholders = sorted(get_placeholders(trans_strings[key]))
                if base_placeholders != trans_placeholders:
                    issues.append(f"[{locale}] Placeholder mismatch for '{key}': base={base_placeholders}, trans={trans_placeholders}")

        # 3. Check for suspicious untranslated strings (identical to English)
        if locale not in ["values-en-rUS", "values-en-rGB"]:
            for key, base_text in base_strings.items():
                if key in trans_strings and key not in likely_same:
                    trans_text = trans_strings[key]
                    if base_text == trans_text and len(base_text.strip()) > 0:
                        issues.append(f"[{locale}] Untranslated string '{key}' (matches English)")

    if not issues:
        print("No issues found in strings.xml files.")
    else:
        for issue in issues:
            print(issue)
        exit(1)

if __name__ == "__main__":
    main()
