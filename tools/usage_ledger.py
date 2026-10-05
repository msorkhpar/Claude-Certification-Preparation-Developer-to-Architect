#!/usr/bin/env python3
"""Local usage ledger: one row per office (subagent) from the session transcripts already on this host.

Reads  ~/.claude/projects/<project>/<session>/subagents/agent-*.jsonl  (and its .meta.json) and writes a TSV:
  project, session, office_id, agent_type, model, tokens_in, cache_write, cache_read, tokens_out, assistant_turns,
  tool_calls, first_ts, last_ts, wall_seconds, rework
tokens_in is the uncached input; cache_write and cache_read are the cached input counts. rework is the number of times
the register resumed the office: user turns that carry text and are not tool results, after the first one.
Read-only. Nothing is sent anywhere. Message contents are never printed or written: only ids, counts, times and the
model name. Standard library only.

usage: tools/usage_ledger.py [--projects-dir DIR] [--project SUBSTRING] [--out FILE]
Default out: <parent of the course repository>/.register-logs/usage-ledger.tsv
"""
import argparse
import datetime as dt
import json
import sys
from pathlib import Path

COLS = ["project", "session", "office_id", "agent_type", "model", "tokens_in", "cache_write", "cache_read",
        "tokens_out", "assistant_turns", "tool_calls", "first_ts", "last_ts", "wall_seconds", "rework"]


def parse_ts(s):
    return dt.datetime.fromisoformat(s.replace("Z", "+00:00"))


def is_resume_turn(msg):
    """A user turn with text that is not a tool result and not a harness-generated meta turn."""
    if msg.get("isMeta"):
        return False
    c = msg.get("message", {}).get("content")
    if isinstance(c, str):
        return bool(c.strip())
    if isinstance(c, list):
        types = {b.get("type") for b in c if isinstance(b, dict)}
        return "text" in types and "tool_result" not in types
    return False


def office_row(path, project, session):
    meta = {}
    mp = path.with_suffix(".meta.json")
    if mp.exists():
        try:
            meta = json.loads(mp.read_text())
        except ValueError:
            meta = {}
    t_in = c_w = c_r = t_out = turns = tools = user_turns = 0
    model = meta.get("model", "")
    first = last = None
    with path.open() as f:
        for line in f:
            try:
                o = json.loads(line)
            except ValueError:
                continue  # a line being written
            ts = o.get("timestamp")
            if ts:
                first = first or ts
                last = ts
            kind = o.get("type")
            if kind == "assistant":
                m = o.get("message", {})
                u = m.get("usage") or {}
                t_in += u.get("input_tokens", 0)
                c_w += u.get("cache_creation_input_tokens", 0)
                c_r += u.get("cache_read_input_tokens", 0)
                t_out += u.get("output_tokens", 0)
                turns += 1
                if m.get("model") and m["model"] != "<synthetic>":
                    model = m["model"]
                tools += sum(1 for b in m.get("content", []) if isinstance(b, dict) and b.get("type") == "tool_use")
            elif kind == "user" and is_resume_turn(o):
                user_turns += 1
    wall = int((parse_ts(last) - parse_ts(first)).total_seconds()) if first and last else 0
    return [project, session, path.stem.removeprefix("agent-"), meta.get("agentType", ""), model, t_in, c_w, c_r,
            t_out, turns, tools, first or "", last or "", wall, max(user_turns - 1, 0)]


def main(argv=None):
    here = Path(__file__).resolve().parent.parent
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--projects-dir", default=str(Path.home() / ".claude" / "projects"))
    ap.add_argument("--project", default="", help="keep only project folders whose name contains this text")
    ap.add_argument("--out", default=str(here.parent / ".register-logs" / "usage-ledger.tsv"))
    a = ap.parse_args(argv)
    root = Path(a.projects_dir)
    rows = []
    for f in sorted(root.glob("*/*/subagents/agent-*.jsonl")):
        project, session = f.parent.parent.parent.name, f.parent.parent.name
        if a.project and a.project not in project:
            continue
        rows.append(office_row(f, project, session))
    out = Path(a.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    with out.open("w") as w:
        w.write("\t".join(COLS) + "\n")
        for r in rows:
            w.write("\t".join(str(x) for x in r) + "\n")
    print(f"offices={len(rows)} out={out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
