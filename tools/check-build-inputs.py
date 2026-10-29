#!/usr/bin/env python3
"""Small offline guard: complete locale resources, placeholders, no invisible BOMs, pinned Gradle."""
import argparse
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_GRADLE_SHA256 = "d725d707bfabd4dfdc958c624003b3c80accc03f7037b5122c4b1d0ef15cecab"
SCRIPTS = ("gradlew", "build-verified-debug.sh", "build-fayyadhos-tv.sh")


def strings(path):
    elements = ET.parse(path).getroot().findall("string")
    names = [element.attrib["name"] for element in elements]
    assert len(names) == len(set(names)), f"Duplicate resource in {path}"
    return {element.attrib["name"]: "".join(element.itertext()) for element in elements}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--require-executable", action="store_true",
                        help="Check executable modes in Git, not only the local filesystem")
    args = parser.parse_args()
    base = strings(ROOT / "app/src/main/res/values/strings.xml")
    english = strings(ROOT / "app/src/main/res/values-en/strings.xml")
    assert base.keys() == english.keys(), "Generic English must cover every base string"
    placeholders = re.compile(r"%(?:\d+\$)?[sd]")
    for key in base:
        assert sorted(placeholders.findall(base[key])) == sorted(placeholders.findall(english[key])), key
    locales = list((ROOT / "app/src/main/res").glob("values-*/strings.xml"))
    for path in locales:
        localized = strings(path)
        if path.parent.name != "values-en-rUS":
            assert base.keys() == localized.keys(), f"Incomplete locale: {path.parent.name}"
        for key, value in localized.items():
            assert sorted(placeholders.findall(base[key])) == sorted(placeholders.findall(value)), f"{path.parent.name}:{key} placeholders"
    us = strings(ROOT / "app/src/main/res/values-en-rUS/strings.xml")
    assert us["date_format"] == "MMM d, yyyy", "Keep the existing en-US date override"
    for folder in ("app/src/main", "app/src/test"):
        for path in (ROOT / folder).rglob("*"):
            if path.is_file() and path.suffix in (".java", ".xml"):
                assert b"\xef\xbb\xbf" not in path.read_bytes(), f"Literal BOM bytes: {path.relative_to(ROOT)}"
    wrapper = (ROOT / "gradle/wrapper/gradle-wrapper.properties").read_text()
    assert f"distributionSha256Sum={EXPECTED_GRADLE_SHA256}" in wrapper
    if args.require_executable:
        entries = subprocess.check_output(["git", "ls-files", "--stage", "--", *SCRIPTS], cwd=ROOT, text=True)
        modes = {line.split("\t", 1)[1]: line.split()[0] for line in entries.splitlines()}
        assert all(modes.get(name) == "100755" for name in SCRIPTS), f"Executable Git modes: {modes}"
    print(f"PASS: {len(base)} base strings, complete locale resources, preserved placeholders/US date, no literal BOMs, pinned Gradle" +
          (", executable Git modes" if args.require_executable else ""))


if __name__ == "__main__":
    main()
