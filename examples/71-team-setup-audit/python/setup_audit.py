"""Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.

The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
"""
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent


def glob_regex(glob):
    """A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character."""
    out, i = "", 0
    while i < len(glob):
        if glob.startswith("**/", i):
            out, i = out + "(?:.*/)?", i + 3
        elif glob.startswith("**", i):
            out, i = out + ".*", i + 2
        elif glob[i] == "*":
            out, i = out + "[^/]*", i + 1
        elif glob[i] == "?":
            out, i = out + "[^/]", i + 1
        else:
            out, i = out + re.escape(glob[i]), i + 1
    return re.compile(out + r"\Z")


def rule_paths(text):
    """The paths list of a rule file's front matter, or None when it has none (the rule then loads at launch)."""
    head = re.match(r"---\n(.*?)\n---\n", text, re.S)
    listed = re.search(r"^paths:\s*\n((?:[ \t]+-[ \t]+.*\n?)+)", head.group(1) + "\n", re.M) if head else None
    if not listed:
        return None
    return [re.sub(r"^\s*-\s+", "", line).strip().strip("\"'") for line in listed.group(1).splitlines() if line.strip()]


def load(root):
    rules = {p.name: rule_paths(p.read_text()) for p in sorted((root / ".claude/rules").glob("*.md"))}
    commands = [p.name for p in sorted((root / ".claude/commands").glob("*.md"))]
    return (root / "files.txt").read_text().split(), (root / "CLAUDE.md").read_text(), rules, commands, json.loads((root / ".claude/settings.json").read_text())


def matches(paths, file):
    return any(glob_regex(g).match(file) for g in paths or [])


def rules_for(rules, file):
    return [name for name, paths in rules.items() if paths is None or matches(paths, file)]


def audit(root):
    files, memory, rules, commands, settings = load(root)
    found = []
    sections = len(re.findall(r"^## ", memory, re.M))
    if not rules and sections >= 3:
        found.append(f"all-in-root: {sections} sections in CLAUDE.md and no rule files")
    for name, paths in rules.items():
        if paths is None:
            found.append(f"rule-loads-always: {name}")
        elif not any(matches(paths, f) for f in files):
            found.append(f"rule-matches-nothing: {name}")
    for f in files:
        if re.search(r"\.test\.tsx?$", f) and not any(matches(p, f) for p in rules.values()):
            found.append(f"test-uncovered: {f}")
    if not commands:
        found.append("no-shared-command")
    perms = settings.get("permissions", {})
    if "Read(./.env)" not in perms.get("deny", []):
        found.append("env-readable")
    if "Bash" in perms.get("allow", []):
        found.append("bare-bash-allowed")
    return found


def main():
    for name in ("project-before", "project-after"):
        root = HERE / name
        files, memory, rules, commands, _ = load(root)
        print(f"{name}: CLAUDE.md {len(memory.splitlines())} lines, {len(rules)} rule files, {len(commands)} shared commands")
        for finding in audit(root):
            print(f"  finding: {finding}")
        if not audit(root):
            print("  no findings")
            for f in files:
                print(f"  {f} <- {', '.join(rules_for(rules, f)) or 'CLAUDE.md only'}")


if __name__ == "__main__":
    main()
