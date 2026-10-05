#!/usr/bin/env python3
"""Unit test for usage_ledger.py on a synthetic transcript (no real session is read)."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import usage_ledger as ul  # noqa: E402


def line(**kw):
    return json.dumps(kw) + "\n"


class T(unittest.TestCase):
    def test_counts(self):
        with tempfile.TemporaryDirectory(dir=Path(__file__).resolve().parent.parent) as d:
            sd = Path(d) / "proj" / "sess" / "subagents"
            sd.mkdir(parents=True)
            (sd / "agent-aX.meta.json").write_text(json.dumps({"agentType": "office-author", "model": "sonnet"}))
            usage = {"input_tokens": 2, "cache_creation_input_tokens": 10, "cache_read_input_tokens": 5, "output_tokens": 7}
            with (sd / "agent-aX.jsonl").open("w") as f:
                f.write(line(type="user", timestamp="2026-10-05T10:00:00Z", message={"content": "brief SECRET"}))
                f.write(line(type="assistant", timestamp="2026-10-05T10:01:00Z", message={
                    "model": "claude-sonnet-5-5", "usage": usage,
                    "content": [{"type": "tool_use"}, {"type": "text", "text": "x"}]}))
                f.write(line(type="user", timestamp="2026-10-05T10:02:00Z", message={"content": [{"type": "tool_result"}]}))
                f.write(line(type="user", timestamp="2026-10-05T10:03:00Z", isMeta=True, message={"content": "notice"}))
                f.write(line(type="user", timestamp="2026-10-05T10:10:00Z", message={"content": "resume SECRET"}))
                f.write(line(type="assistant", timestamp="2026-10-05T10:11:00Z", message={
                    "model": "claude-sonnet-5-5", "usage": usage, "content": [{"type": "tool_use"}, {"type": "tool_use"}]}))
            out = Path(d) / "out.tsv"
            self.assertEqual(ul.main(["--projects-dir", d, "--out", str(out)]), 0)
            text = out.read_text()
            self.assertNotIn("SECRET", text)
            rec = dict(zip(ul.COLS, text.splitlines()[1].split("\t")))
            self.assertEqual((rec["office_id"], rec["agent_type"], rec["model"]), ("aX", "office-author", "claude-sonnet-5-5"))
            self.assertEqual((rec["tokens_in"], rec["cache_write"], rec["cache_read"], rec["tokens_out"]), ("4", "20", "10", "14"))
            self.assertEqual((rec["assistant_turns"], rec["tool_calls"], rec["wall_seconds"], rec["rework"]), ("2", "3", "660", "1"))


if __name__ == "__main__":
    unittest.main()
