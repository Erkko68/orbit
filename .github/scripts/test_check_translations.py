"""Regression tests for the translation gate, including failures CI must catch."""
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from check_translations import LANGUAGES, check, compare_resources, read_resources


class TranslationCheckTest(unittest.TestCase):
    def resources(self, xml):
        with TemporaryDirectory() as directory:
            path = Path(directory) / "strings.xml"
            path.write_text(xml, encoding="utf-8")
            return read_resources(path)

    def test_accepts_reordered_placeholders_and_language_specific_plural(self):
        default = self.resources('<resources><string name="x">%1$s: %2$d</string>'
                                 '<plurals name="n"><item quantity="one">%1$d item</item>'
                                 '<item quantity="other">%1$d items</item></plurals></resources>')
        translated = self.resources('<resources><string name="x">%2$d: %1$s</string>'
                                    '<plurals name="n"><item quantity="one">%1$d elemento</item>'
                                    '<item quantity="many">%1$d elementi</item>'
                                    '<item quantity="other">%1$d elementi</item></plurals></resources>')
        self.assertEqual([], compare_resources(default, translated, "it"))

    def test_rejects_missing_and_unknown_keys(self):
        self.assertEqual(["it: missing key x", "it: unknown key y"], compare_resources(
            {"x": ("string", {"other": "X"})}, {"y": ("string", {"other": "Y"})}, "it"))

    def test_rejects_changed_or_duplicated_placeholders(self):
        default = {"x": ("string", {"other": "%1$s: %2$d"})}
        for text in ("%1$d: %2$s", "%1$s", "%1$s: %2$d %2$d"):
            self.assertTrue(compare_resources(default, {"x": ("string", {"other": text})}, "it"))

    def test_rejects_missing_plural_quantity_and_changed_type(self):
        default = {"n": ("plurals", {"one": "%1$d item", "other": "%1$d items"})}
        self.assertTrue(compare_resources(default, {"n": ("plurals", {"other": "%1$d elementi"})}, "it"))
        self.assertTrue(compare_resources(default, {"n": ("string", {"other": "%1$d"})}, "it"))

    def test_rejects_duplicates_empty_text_and_missing_other(self):
        for xml in ('<resources><string name="x">A</string><string name="x">B</string></resources>',
                    '<resources><string name="x"> </string></resources>',
                    '<resources><plurals name="n"><item quantity="one">One</item></plurals></resources>',
                    '<resources><plurals name="n"><item quantity="other">A</item>'
                    '<item quantity="other">B</item></plurals></resources>'):
            with self.subTest(xml=xml), self.assertRaises(ValueError):
                self.resources(xml)

    def test_reports_missing_files_and_malformed_xml(self):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            for relative in ("shared/src/commonMain/composeResources", "shared/src/androidMain/res"):
                folder = root / relative
                (folder / "values").mkdir(parents=True)
                (folder / "values/strings.xml").write_text('<resources><string name="x">X</string></resources>')
                for language in LANGUAGES:
                    (folder / f"values-{language}").mkdir()
                    (folder / f"values-{language}/strings.xml").write_text('<resources><string name="x">Y</string></resources>')
            self.assertEqual([], check(root))
            (root / "shared/src/commonMain/composeResources/values-it/strings.xml").unlink()
            (root / "shared/src/androidMain/res/values-ca/strings.xml").write_text('<broken')
            self.assertEqual(2, len(check(root)))


if __name__ == "__main__":
    unittest.main()
