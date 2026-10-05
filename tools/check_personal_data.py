#!/usr/bin/env python3
"""Fail when a page, practice or example of the range carries personal data or a secret.

Scans course/<module>/, exercises/<module>/ and examples/<module>/ (module folders matching --modules; generated wrong-*, build
output and dependency folders skipped) for: an e-mail address outside the placeholder domains (example.*, *.invalid, *.test,
*.example, localhost, x.io); a home path with a real-looking user (/home/<user>, /Users/<user>, C:\\Users\\<user>) other than
the placeholders; an API key (sk-ant- followed by a key-like tail that does not say EXAMPLE, REPLACE or PLACEHOLDER); and the identity of whoever runs
the check, read at RUN TIME from git and the environment (never stored here): git user.email, user.name and the host name,
found as whole words in any scanned file.
usage: tools/check_personal_data.py --modules REGEX [--examples REGEX]
"""
import argparse
import os
import re
import socket
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@([A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,})")
OK_DOMAIN = re.compile(r"(^|\.)example(\.[a-z]+)?$|\.invalid$|\.test$|^localhost$|^x\.io$", re.I)
HOME = re.compile(r"(?:/home/|/Users/|[A-Za-z]:\\Users\\)([A-Za-z0-9_.-]+)")
OK_USER = {"user", "you", "me", "dev", "runner", "node", "app", "work", "example", "alice", "bob", "jane", "janedoe", "name", "username", "your-name", "foo", "ci"}
KEY = re.compile(r"sk-ant-[A-Za-z0-9_-]{20,}")
SKIP_DIRS = {"node_modules", ".gradle", ".kotlin", "home", "__pycache__", ".pytest_cache"}


#: Identities that name a product or a service account, not a person (a cloud session's git user is "Claude").
NOT_PERSONAL = {"claude", "anthropic", "runner", "ubuntu", "root", "user"}


def run_time_identity():
    out = set()
    for key in ("user.email", "user.name"):
        v = subprocess.run(["git", "config", key], capture_output=True, text=True, cwd=ROOT).stdout.strip()
        if len(v) >= 5 and v.lower() not in NOT_PERSONAL:
            out.add(v)
    h = socket.gethostname()
    if len(h) >= 4:
        out.add(h)
    return out


def files(modules, examples):
    for top, rx in (("course", modules), ("exercises", modules), ("examples", examples)):
        base = ROOT / top
        for mod in sorted(base.iterdir()) if base.is_dir() else []:
            if not (mod.is_dir() and rx.match(mod.name)):
                continue
            for dirpath, dirnames, names in os.walk(mod):
                dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS and not d.startswith((".build", "wrong-"))]
                for n in names:
                    yield Path(dirpath) / n


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modules", required=True)
    ap.add_argument("--examples")
    a = ap.parse_args()
    modules = re.compile(a.modules)
    examples = re.compile(a.examples if a.examples is not None else a.modules)
    ident = run_time_identity()
    ident_rx = [re.compile(r"(?<![\w.-])" + re.escape(i) + r"(?![\w-])", re.I) for i in ident]
    n_files, found = 0, []
    for f in files(modules, examples):
        try:
            text = f.read_text()
        except (UnicodeDecodeError, OSError):
            continue
        n_files += 1
        rel = f.relative_to(ROOT)
        for m in EMAIL.finditer(text):
            if not OK_DOMAIN.search(m.group(1)):
                found.append(f"{rel}: e-mail address outside the placeholder domains: {m.group(0)}")
        for m in HOME.finditer(text):
            if m.group(1).lower() not in OK_USER:
                found.append(f"{rel}: home path of a real-looking user: {m.group(0)}")
        for m in KEY.finditer(text):
            if not re.search(r"EXAMPLE|REPLACE|PLACEHOLDER|XXXX", m.group(0).upper()):
                found.append(f"{rel}: API-key-like text: {m.group(0)[:14]}...")
        for rx in ident_rx:
            if rx.search(text):
                found.append(f"{rel}: contains the identity of the person running the check (read at run time)")
    for line in found[:50]:
        print(line)
    print(f"personal data check: {n_files} files, {len(found)} finding(s)")
    return 1 if found else 0


if __name__ == "__main__":
    sys.exit(main())
