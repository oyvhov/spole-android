#!/usr/bin/env python3
"""Check that the maintained Android locale resource sets have the same keys."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
LOCALES = ("values", "values-b+nn", "values-b+nb")
RESOURCE_TAGS = {"string", "plurals", "string-array"}
DEFAULT_ONLY = {"app_name"}


def resource_keys(directory: Path) -> set[str]:
    keys: set[str] = set()
    for path in sorted(directory.glob("*.xml")):
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as error:
            raise RuntimeError(f"{path}: invalid XML: {error}") from error
        for element in root:
            if element.tag in RESOURCE_TAGS and element.get("name"):
                keys.add(element.get("name", ""))
    return keys


def main() -> int:
    try:
        resources = {locale: resource_keys(ROOT / locale) for locale in LOCALES}
    except RuntimeError as error:
        print(error, file=sys.stderr)
        return 1

    default = resources["values"]
    failed = False
    for locale in LOCALES[1:]:
        missing = sorted(default - resources[locale] - DEFAULT_ONLY)
        extra = sorted(resources[locale] - default)
        if missing:
            print(f"{locale}: missing resource keys: {', '.join(missing)}")
            failed = True
        if extra:
            print(f"{locale}: keys not present in values: {', '.join(extra)}")
            failed = True

    if failed:
        return 1
    print(f"Translation keys OK: {len(default)} default keys; {len(DEFAULT_ONLY)} allowed default-only key.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
