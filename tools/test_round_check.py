#!/usr/bin/env python3
"""Unit tests for round_check.py in a throwaway git repository (no heavy job)."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import round_check as rc  # noqa: E402


def g(d, *a):
    return subprocess.run(["git", "-C", str(d), *a], capture_output=True, text=True, check=True).stdout.strip()


class T(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parent.parent)
        self.d = Path(self.tmp.name)
        g(self.d, "init", "-q", "-b", "main")
        g(self.d, "config", "user.name", "Test")
        g(self.d, "config", "user.email", "test@example.invalid")
        self.commit("a")
        self.frozen = g(self.d, "rev-parse", "HEAD")
        self.spec = {
            "id": "r1", "purpose": "p", "inputs": {"course": {"commit": self.frozen}},
            "module_range": {"from": 1, "to": 2}, "languages": ["java"], "gates": ["true"],
            "acceptance": {"modules": 2}, "frozen_at": "2026-10-05T09:00:00Z",
        }

    def tearDown(self):
        self.tmp.cleanup()

    def commit(self, name):
        (self.d / name).write_text(name)
        g(self.d, "add", name)
        g(self.d, "commit", "-q", "-m", name)

    def check(self, branch="b"):
        return rc.handback_errors(self.spec, "course", branch, None, self.d)

    def test_spec_ok_and_bad(self):
        self.assertEqual(rc.spec_errors(self.spec), [])
        bad = dict(self.spec, languages=["go"], frozen_at="yesterday")
        bad["inputs"] = {"course": {"commit": "abc"}}
        self.assertEqual(len(rc.spec_errors(bad)), 3)
        missing = {k: v for k, v in self.spec.items() if k != "gates"}
        self.assertIn("missing field: gates", rc.spec_errors(missing))

    def test_branch_from_frozen_passes(self):
        g(self.d, "checkout", "-q", "-b", "b")
        self.commit("x")
        self.commit("y")
        self.assertEqual(self.check(), [])

    def test_main_moved_branch_still_passes(self):
        g(self.d, "checkout", "-q", "-b", "b")
        self.commit("x")
        g(self.d, "checkout", "-q", "main")
        self.commit("later")
        self.assertEqual(self.check(), [])

    def test_branch_cut_from_newer_main_refused(self):
        self.commit("later")
        g(self.d, "checkout", "-q", "-b", "b")
        self.commit("x")
        errs = self.check()
        self.assertEqual(len(errs), 1)
        self.assertIn("base differs", errs[0])

    def test_merge_of_newer_main_refused(self):
        g(self.d, "checkout", "-q", "-b", "b")
        self.commit("x")
        g(self.d, "checkout", "-q", "main")
        self.commit("later")
        g(self.d, "checkout", "-q", "b")
        g(self.d, "merge", "-q", "--no-ff", "-m", "m", "main")
        self.assertEqual(len(self.check()), 1)

    def test_head_mismatch_refused(self):
        g(self.d, "checkout", "-q", "-b", "b")
        self.commit("x")
        e = rc.handback_errors(self.spec, "course", "b", "0" * 40, self.d)
        self.assertIn("differs from the reported head", e[0])


if __name__ == "__main__":
    unittest.main()
