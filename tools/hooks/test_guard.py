#!/usr/bin/env python3
"""Recorded inputs for guard.py, run in both modes. Pure unittest, no docker."""
import contextlib
import io
import os
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import guard  # noqa: E402


def bash(cmd):
    return {"tool_name": "Bash", "tool_input": {"command": cmd}, "agent_type": "office"}


def write(text, tool="Write"):
    key = "content" if tool == "Write" else "new_string"
    return {"tool_name": tool, "tool_input": {"file_path": "a.md", key: text}}


FAKE_USER = "jdoe"
ALLOWED = [
    ("docker ctx", bash("docker --context desktop-linux ps")),
    ("docker ctx eq", bash("docker --context=desktop-linux images")),
    ("slot docker", bash("run-heavy.sh gate docker --context desktop-linux run --rm img")),
    ("slot pytest", bash("run-heavy.sh tests pytest -n 4 tests")),
    ("git status", bash("git status -sb")),
    ("git commit", bash("git commit -F msg.txt")),
    ("ls", bash("ls -la tools")),
    ("python tool", bash("python3 tools/hooks/test_guard.py")),
    ("grep gh word", bash("grep -n ghost README.md")),
    ("write example mail", write("mail contact@example.com")),
    ("write invalid mail", write("mail a@example.invalid")),
    ("write noreply", write("Co-Authored-By: C <noreply@anthropic.com>")),
    ("write placeholder path", write("see /path/to/project/x")),
    ("edit plain", write("hello", "Edit")),
    ("read tool", {"tool_name": "Read", "tool_input": {"file_path": "x"}}),
]
DENIED = [
    ("docker no ctx", bash("docker ps"), "docker-context"),
    ("docker ctx use", bash("docker --context desktop-linux context use default"), "docker-context"),
    ("docker login", bash("docker --context desktop-linux login"), "docker-context"),
    ("docker push", bash("docker --context desktop-linux push img"), "docker-context"),
    ("git push", bash("git push origin main"), "git-push"),
    ("gh", bash("gh pr create"), "git-push"),
    ("xdist auto", bash("run-heavy.sh tests pytest -n auto"), "xdist-auto"),
    ("pytest direct", bash("pytest tests"), "heavy-outside-slot"),
    ("gradlew direct", bash("./gradlew test"), "heavy-outside-slot"),
    ("mvn direct", bash("cd x && mvn -q test"), "heavy-outside-slot"),
    ("docker build direct", bash("docker --context desktop-linux build ."), "heavy-outside-slot"),
    ("kill", bash("kill -9 123"), "kill"),
    ("pkill", bash("pkill -f worker"), "kill"),
    ("write email", write("mail " + FAKE_USER + "@gmail.com"), "personal-data"),
    ("edit home path", write("cd /home/" + FAKE_USER + "/proj", "Edit"), "personal-data"),
]
RAISES = ("raises", "{not json")


class GuardTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.log = Path(self.tmp.name) / "guard-log.tsv"
        self._lp = guard.log_path
        guard.log_path = lambda: self.log
        os.environ.pop("HEAVY_SLOT_HELD", None)

    def tearDown(self):
        guard.log_path = self._lp
        self.tmp.cleanup()

    def call(self, data, mode):
        import json
        raw = data if isinstance(data, str) else json.dumps(data)
        err = io.StringIO()
        with contextlib.redirect_stderr(err):
            rc = guard.run(raw, mode)
        return rc, err.getvalue()

    def lines(self):
        return self.log.read_text().splitlines() if self.log.exists() else []

    def test_allowed(self):
        for name, d in ALLOWED:
            for mode in ("log", "deny"):
                with self.subTest(name=name, mode=mode):
                    self.assertEqual(self.call(d, mode)[0], 0)
        self.assertEqual(self.lines(), [])

    def test_denied(self):
        for name, d, rule in DENIED:
            with self.subTest(name=name, mode="log"):
                n = len(self.lines())
                self.assertEqual(self.call(d, "log")[0], 0)
                new = self.lines()[n:]
                self.assertTrue(any(l.split("\t")[3] == rule for l in new), new)
            with self.subTest(name=name, mode="deny"):
                rc, err = self.call(d, "deny")
                self.assertEqual(rc, 2)
                self.assertIn(rule, err)
                self.assertEqual(len(err.strip().splitlines()), 1)

    def test_raises(self):
        self.assertEqual(self.call(RAISES[1], "log")[0], 0)
        self.assertEqual(self.lines()[-1].split("\t")[3], "guard-error")
        self.assertEqual(self.call(RAISES[1], "deny")[0], 2)

    def test_slot_env_allows_heavy(self):
        os.environ["HEAVY_SLOT_HELD"] = "1"
        try:
            self.assertEqual(self.call(bash("pytest tests"), "deny")[0], 0)
        finally:
            os.environ.pop("HEAVY_SLOT_HELD")

    def test_log_has_no_personal_data(self):
        self.call(write("mail " + FAKE_USER + "@gmail.com at /home/" + FAKE_USER + "/x"), "log")
        self.call(bash("git push https://" + FAKE_USER + "@gmail.com/x"), "log")
        text = self.log.read_text()
        self.assertNotIn(FAKE_USER, text)
        self.assertIn("<redacted>", text)
        for h in guard.hostname():
            self.assertNotIn(h.lower(), text.lower().replace("<redacted>", ""))
        for l in text.splitlines():
            self.assertEqual(len(l.split("\t")), 5)
            self.assertLessEqual(len(l.split("\t")[4]), 120)

    def test_hostname_denied_at_runtime(self):
        h = sorted(guard.hostname(), key=len)[-1:] 
        if not h:
            self.skipTest("no hostname")
        rc, _ = self.call(write("machine " + h[0]), "deny")
        self.assertEqual(rc, 2)

    def test_counts(self):
        self.assertGreaterEqual(len(ALLOWED), 15)
        self.assertGreaterEqual(len(DENIED) + 1, 16)


if __name__ == "__main__":
    unittest.main(verbosity=1)
