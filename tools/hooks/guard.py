#!/usr/bin/env python3
"""PreToolUse guard hook for Claude Code.

Mode from env STUDYFORGE_GUARD_MODE: "log" (default) records what would be denied and
exits 0; "deny" exits 2 with a one-line fix on stderr. Any error: log mode logs rule
"guard-error" and exits 0; deny mode exits 2 (fail closed).
The log never holds personal data: matched emails, home paths and the hostname
(read from the environment at runtime, never stored) are replaced by <redacted>.
"""
import datetime
import json
import os
import re
import shlex
import socket
import sys
from pathlib import Path

ALLOWED_MAIL = re.compile(r"@(example\.com|example\.invalid)$|^noreply@anthropic\.com$", re.I)
EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+")
HOME_PATH = re.compile(r"/(?:home|Users)/[A-Za-z0-9._-]+/?")
HEAVY = re.compile(r"(?:^|[\s;&|(])(pytest|gradle|gradlew|\./gradlew|mvn|mvnw)(?=\s|$)")
KILL = re.compile(r"(?:^|[\s;&|(])(kill|pkill|killall)(?=\s|$)")

FIXES = {
    "docker-context": "Add --context desktop-linux to every docker command; never run docker context use, login or push.",
    "git-push": "Nothing is pushed: no git push, no gh; hand the owner the exact commands.",
    "xdist-auto": "Use at most 4 workers (-n 4), never -n auto.",
    "heavy-outside-slot": "Start it as: run-heavy.sh <kind> <command> (heavy-slot script beside the project).",
    "kill": "Do not kill processes; let the heavy-slot script stop its job.",
    "personal-data": "Remove the email, home path or machine name; use a placeholder (contact@example.com, /path/to/project).",
    "guard-error": "The guard failed; fix the input or the guard before retrying.",
}


def log_path():
    return Path(__file__).resolve().parents[2].parent / ".register-logs" / "guard-log.tsv"


def hostname():
    names = {os.environ.get("HOSTNAME", ""), os.environ.get("HOST", ""), os.environ.get("COMPUTERNAME", "")}
    try:
        names.add(socket.gethostname())
    except Exception:
        pass
    return {n for n in names if len(n) >= 3}


def redact(text):
    text = EMAIL.sub("<redacted>", text)
    text = HOME_PATH.sub("<redacted>", text)
    for h in hostname():
        text = re.sub(re.escape(h), "<redacted>", text, flags=re.I)
    return text


def segments(cmd):
    return [s.strip() for s in re.split(r"&&|\|\||;|\||\n", cmd) if s.strip()]


def bash_rules(cmd):
    hits = []
    for seg in segments(cmd):
        try:
            words = shlex.split(seg)
        except ValueError:
            words = seg.split()
        # drop env assignments
        while words and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", words[0]):
            words = words[1:]
        if not words:
            continue
        first = os.path.basename(words[0])
        if first == "docker":
            joined = " ".join(words)
            if not re.search(r"--context[ =]desktop-linux", joined):
                hits.append("docker-context")
            elif re.search(r"\bdocker\b.*\bcontext\s+use\b|\bdocker\b.*\b(login|push)\b", joined):
                hits.append("docker-context")
            if re.search(r"\bcontext\s+use\b|\b(login|push)\b", joined) and "docker-context" not in hits:
                hits.append("docker-context")
    if re.search(r"(?:^|[\s;&|(])git\s+(?:-\S+\s+)*push\b", cmd) or re.search(r"(?:^|[\s;&|(])gh\s", cmd):
        hits.append("git-push")
    if re.search(r"-n\s*=?\s*auto\b", cmd):
        hits.append("xdist-auto")
    if not os.environ.get("HEAVY_SLOT_HELD"):
        for seg in segments(cmd):
            words = seg.split()
            while words and re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", words[0]):
                words = words[1:]
            if not words:
                continue
            if os.path.basename(words[0]) == "run-heavy.sh":
                continue
            heavy = bool(HEAVY.search(" " + seg))
            if os.path.basename(words[0]) == "docker":
                heavy = heavy or bool(re.search(r"\s(run|exec|build)\b|\scompose\b", " " + " ".join(words[1:])))
            if heavy:
                hits.append("heavy-outside-slot")
                break
    if KILL.search(cmd):
        hits.append("kill")
    return hits


def personal_data(text):
    for m in EMAIL.findall(text):
        if not ALLOWED_MAIL.search(m):
            return True
    if HOME_PATH.search(text):
        return True
    low = text.lower()
    return any(h.lower() in low for h in hostname())


def evaluate(data):
    tool = data.get("tool_name", "")
    ti = data.get("tool_input") or {}
    if tool == "Bash":
        cmd = ti.get("command", "")
        return tool, bash_rules(cmd), cmd
    if tool in ("Write", "Edit"):
        text = ti.get("content") or ti.get("new_string") or ""
        return tool, (["personal-data"] if personal_data(text) else []), text
    return tool, [], ""


def write_log(agent, tool, rule, excerpt):
    p = log_path()
    p.parent.mkdir(parents=True, exist_ok=True)
    ex = redact(excerpt).replace("\t", " ").replace("\n", " ")[:120]
    ts = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    line = "\t".join([ts, redact(agent or "-"), tool or "-", rule, ex]) + "\n"
    with open(p, "a", encoding="utf-8") as f:
        f.write(line)


def run(raw, mode):
    """Return exit code."""
    agent = tool = ""
    try:
        data = json.loads(raw)
        agent = data.get("agent_type", "") or ""
        tool, rules, excerpt = evaluate(data)
    except Exception as exc:  # fail closed in deny mode
        rule = "guard-error"
        try:
            write_log(agent, tool, rule, type(exc).__name__)
        except Exception:
            pass
        if mode == "deny":
            sys.stderr.write(FIXES[rule] + "\n")
            return 2
        return 0
    if not rules:
        return 0
    rules = list(dict.fromkeys(rules))
    try:
        for r in rules:
            write_log(agent, tool, r, excerpt)
    except Exception:
        if mode == "deny":
            sys.stderr.write(FIXES["guard-error"] + "\n")
            return 2
    if mode == "deny":
        sys.stderr.write(f"guard {rules[0]}: {FIXES[rules[0]]}\n")
        return 2
    return 0


def main():
    mode = os.environ.get("STUDYFORGE_GUARD_MODE", "log").strip().lower()
    if mode not in ("log", "deny"):
        mode = "log"
    return run(sys.stdin.read(), mode)


if __name__ == "__main__":
    sys.exit(main())
