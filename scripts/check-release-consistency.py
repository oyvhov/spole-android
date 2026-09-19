#!/usr/bin/env python3
"""
Release consistency validator for Spole.
Verifies that app/build.gradle.kts, README.md, CHANGELOG.md, and docs/release-v*.md
all stay in exact synchronization before a release is built or published.
"""

import os
import re
import sys

def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    gradle_file = os.path.join(root, "app", "build.gradle.kts")
    readme_file = os.path.join(root, "README.md")
    changelog_file = os.path.join(root, "CHANGELOG.md")

    if not os.path.exists(gradle_file):
        print(f"ERROR: Cannot find {gradle_file}")
        sys.exit(1)

    with open(gradle_file, "r", encoding="utf-8") as f:
        gradle_content = f.read()

    match = re.search(r'versionName\s*=\s*"([^"]+)"', gradle_content)
    if not match:
        print("ERROR: Could not find versionName in app/build.gradle.kts")
        sys.exit(1)

    version_name = match.group(1)
    print(f"Checking consistency for versionName: {version_name}")

    errors = []

    # 1. README.md check
    if os.path.exists(readme_file):
        with open(readme_file, "r", encoding="utf-8") as f:
            readme_content = f.read()
        if version_name not in readme_content:
            errors.append(f"README.md does not mention version {version_name}")
    else:
        errors.append("README.md not found")

    # 2. CHANGELOG.md check
    if os.path.exists(changelog_file):
        with open(changelog_file, "r", encoding="utf-8") as f:
            changelog_content = f.read()
        changelog_pattern = rf"##\s+{re.escape(version_name)}"
        if not re.search(changelog_pattern, changelog_content):
            errors.append(f"CHANGELOG.md is missing an entry for '## {version_name}'")
    else:
        errors.append("CHANGELOG.md not found")

    # 3. docs/release-v*.md check
    release_doc = os.path.join(root, "docs", f"release-v{version_name}.md")
    if not os.path.exists(release_doc):
        errors.append(f"Release documentation missing: {os.path.relpath(release_doc, root)}")

    if errors:
        print("\n[FAIL] Release consistency check FAILED:")
        for err in errors:
            print(f"  - {err}")
        sys.exit(1)

    print(f"[OK] All release metadata in sync for {version_name}!")
    sys.exit(0)

if __name__ == "__main__":
    main()
