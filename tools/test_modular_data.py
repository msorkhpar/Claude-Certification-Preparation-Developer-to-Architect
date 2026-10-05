#!/usr/bin/env python3
"""Tests of tools/modular_data.py: the per-module data files are checked when they load."""
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import modular_data  # noqa: E402

KEY = "exercises/06-prompting-fundamentals/unit-01/practice-1"


def run(files):
    with tempfile.TemporaryDirectory() as root:
        for m in ("06-prompting-fundamentals", "06-twin", "13-one-rest-api-under-every-sdk"):
            (Path(root) / "exercises" / m).mkdir(parents=True)
        (Path(root) / "d").mkdir()
        for name, text in files.items():
            (Path(root) / "d" / name).write_text(text)
        real, modular_data.ROOT = modular_data.ROOT, Path(root)
        try:
            return modular_data.load(Path(root) / "d", "PLANTS", {}), None
        except SystemExit as e:
            return None, str(e)
        finally:
            modular_data.ROOT = real


class T(unittest.TestCase):
    def test_ok(self):
        t, err = run({"06-prompting-fundamentals.py": f'PLANTS[f"{{X}}/{KEY.split("/",1)[1]}"] = {{}}\n'})
        self.assertIsNone(err)
        self.assertEqual(list(t), [KEY])

    def test_same_practice_twice(self):
        line = f'PLANTS[f"{{X}}/{KEY.split("/",1)[1]}"] = {{}}\n'
        _, err = run({"06-prompting-fundamentals.py": line * 2})
        self.assertIn("already claimed", err)

    def test_other_module(self):
        _, err = run({"06-prompting-fundamentals.py": 'PLANTS[f"{X}/13-one-rest-api-under-every-sdk/unit-01/practice-1"] = {}\n'})
        self.assertIn("only its own module", err)

    def test_two_files_one_module(self):
        _, err = run({"06-prompting-fundamentals.py": "", "06-twin.py": ""})
        self.assertIn("both claim module 06", err)
        _, err = run({"06-prompting-fundamentals.py": "", "06-nope.py": ""})
        self.assertIn("no module folder", err)


if __name__ == "__main__":
    unittest.main()
