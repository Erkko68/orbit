"""Fail CI when a supported language has incomplete or incompatible UI resources.

Run from any directory: python3 .github/scripts/check_translations.py
"""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
LANGUAGES = ("es", "ca", "it")
PLACEHOLDER = re.compile(r"%(\d+)\$([ds])")
QUANTITIES = {"zero", "one", "two", "few", "many", "other"}


def read_resources(path):
    """Return keys with their type and texts; reject duplicates, blanks and malformed XML."""
    root = ET.parse(path).getroot()
    if root.tag != "resources":
        raise ValueError(f"{path}: expected <resources>")
    result = {}
    for node in root:
        name = node.get("name")
        if not name or name in result:
            raise ValueError(f"{path}: missing or duplicate key {name!r}")
        if node.tag == "string":
            texts = {"other": "".join(node.itertext())}
        elif node.tag == "plurals":
            texts = {}
            for item in node:
                quantity = item.get("quantity")
                if item.tag != "item" or quantity not in QUANTITIES or quantity in texts:
                    raise ValueError(f"{path}: invalid or duplicate quantity in {name}")
                texts[quantity] = "".join(item.itertext())
            if "other" not in texts:
                raise ValueError(f"{path}: {name} needs an 'other' plural")
        else:
            raise ValueError(f"{path}: unsupported resource type {node.tag}")
        if any(not text.strip() for text in texts.values()):
            raise ValueError(f"{path}: empty text in {name}")
        result[name] = (node.tag, texts)
    return result


def compare_resources(default, translated, label):
    errors = []
    for name in sorted(default.keys() - translated.keys()):
        errors.append(f"{label}: missing key {name}")
    for name in sorted(translated.keys() - default.keys()):
        errors.append(f"{label}: unknown key {name}")
    for name in sorted(default.keys() & translated.keys()):
        kind, texts = default[name]
        other_kind, other_texts = translated[name]
        if kind != other_kind:
            errors.append(f"{label}: wrong resource type for {name}")
            continue
        for quantity in texts.keys() - other_texts.keys():
            errors.append(f"{label}: missing {name}/{quantity}")
        for quantity, text in other_texts.items():
            expected = texts.get(quantity, texts["other"])
            if Counter(PLACEHOLDER.findall(text)) != Counter(PLACEHOLDER.findall(expected)):
                errors.append(f"{label}: incompatible placeholders in {name}/{quantity}")
    return errors


def check(root=ROOT):
    errors = []
    for relative in ("shared/src/commonMain/composeResources", "shared/src/androidMain/res"):
        folder = root / relative
        try:
            default = read_resources(folder / "values/strings.xml")
        except (OSError, ET.ParseError, ValueError) as error:
            errors.append(str(error))
            continue
        for language in LANGUAGES:
            path = folder / f"values-{language}/strings.xml"
            try:
                errors.extend(compare_resources(default, read_resources(path), str(path.relative_to(root))))
            except (OSError, ET.ParseError, ValueError) as error:
                errors.append(str(error))
    return errors


if __name__ == "__main__":
    problems = check()
    if problems:
        print("\n".join(problems), file=sys.stderr)
        sys.exit(1)
    print("English, Spanish, Catalan and Italian resources have matching keys and placeholders.")
