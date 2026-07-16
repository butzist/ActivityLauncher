#!/usr/bin/env python3
import os
import re
from pathlib import Path

def parse_strings_manually(path):
    """Parses strings.xml manually to be resilient to invalid XML (text between tags)."""
    try:
        content = path.read_text(encoding='utf-8')
    except Exception as e:
        print(f"Error reading {path}: {e}")
        return {}

    # Find all string tags. Handle attributes like 'comment' or 'translatable'.
    pattern = re.compile(r'<string\s+name="([^"]+)"([^>]*)>(.*?)</string>', re.DOTALL)
    strings = {}
    for match in pattern.finditer(content):
        name = match.group(1)
        attrs_str = match.group(2)
        value = match.group(3)

        # Parse attributes
        attrs = {}
        attr_pattern = re.compile(r'(\w+)="([^"]*)"')
        for attr_match in attr_pattern.finditer(attrs_str):
            attrs[attr_match.group(1)] = attr_match.group(2)

        strings[name] = {'value': value, 'attrs': attrs}
    return strings

def write_strings(path, strings, keys_order):
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<resources>']
    for key in keys_order:
        if key in strings:
            s = strings[key]
            attrs_parts = [f'name="{key}"']
            # Sort attributes, but keep name first
            for k, v in sorted(s['attrs'].items()):
                if k != 'name':
                    attrs_parts.append(f'{k}="{v}"')
            attrs_str = ' '.join(attrs_parts)
            lines.append(f'    <string {attrs_str}>{s["value"]}</string>')
    lines.append('</resources>')
    path.write_text('\n'.join(lines) + '\n', encoding='utf-8')

def main():
    # Detect project root
    script_dir = Path(__file__).resolve().parent
    project_root = script_dir.parent
    res_dir = project_root / "app/src/main/res"
    base_path = res_dir / "values/strings.xml"

    if not base_path.exists():
        print(f"Base strings.xml not found at {base_path}")
        return

    # 1. Load and sort master
    master_strings = parse_strings_manually(base_path)
    sorted_keys = sorted(master_strings.keys())

    # Update master file (sorted)
    write_strings(base_path, master_strings, sorted_keys)
    print(f"Sorted {base_path}")

    # 2. Process all translations
    translation_files = sorted(list(res_dir.glob("values-*/strings.xml")))
    for trans_path in translation_files:
        trans_strings = parse_strings_manually(trans_path)

        # Sync: Add missing keys from master
        for key in sorted_keys:
            if key not in trans_strings:
                # Use master value as placeholder
                trans_strings[key] = {
                    'value': master_strings[key]['value'],
                    'attrs': {} # Don't copy comments to translations
                }

        # Remove keys that are NOT in master
        to_remove = [k for k in trans_strings if k not in sorted_keys]
        for k in to_remove:
            del trans_strings[k]

        # Write back sorted and synced
        write_strings(trans_path, trans_strings, sorted_keys)
        print(f"Synced and sorted {trans_path}")

if __name__ == "__main__":
    main()
