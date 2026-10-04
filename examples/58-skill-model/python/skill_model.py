"""What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.

The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
`/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work.
"""
import re
import shlex
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "40-workflow-lint" / "python"))
from miniyaml import parse_yaml  # noqa: E402

LEVELS = ["enterprise", "personal", "project"]  # the order in which a skill name is resolved: the first level wins


def parse(text):
    """(frontmatter mapping, body) of a command or skill file."""
    m = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, re.S)
    return (parse_yaml(m.group(1)), m.group(2)) if m else ({}, text)


def command_name(path, meta):
    """The slash command a file creates: a skill's `name`, else its folder; a command's file name."""
    parts = path.split("/")
    if parts[-1] == "SKILL.md":
        return str(meta.get("name") or parts[-2])
    return parts[-1][:-3]


def winner(candidates):
    """Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. candidates: {level: path}."""
    return next((candidates[level] for level in LEVELS if level in candidates), None)


def invocation(meta):
    """Who can start it, and whether its description is always in context."""
    manual = meta.get("disable-model-invocation") is True
    hidden = meta.get("user-invocable") is False
    return {"you": not hidden, "claude": not manual, "description_in_context": not manual}


def _names(value):
    return re.findall(r"[^\s,(]+(?:\([^)]*\))?", value) if isinstance(value, str) else list(value or [])


def pre_approved(meta, tool, command=""):
    """True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command."""
    for item in _names(meta.get("allowed-tools")):
        name, _, pattern = item.partition("(")
        if name != tool:
            continue
        pattern = pattern.rstrip(")")
        if not pattern or (pattern.endswith(" *") and (command == pattern[:-2] or command.startswith(pattern[:-1]))) or pattern == command:
            return True
    return False


def removed(meta, tool):
    """True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/**)` leaves the tool in place."""
    return tool in [n for n in _names(meta.get("disallowed-tools")) if "(" not in n]


def tool_status(meta, tool, command=""):
    if removed(meta, tool):
        return "removed"
    return "pre-approved" if pre_approved(meta, tool, command) else "permission settings decide"


def render(body, raw, names=()):
    """Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input."""
    args = shlex.split(raw) if raw else []
    named = dict(zip(names, args))
    used = []

    def sub(m):
        token = m.group(1)
        used.append(token)
        if token == "ARGUMENTS":
            return raw
        index = re.fullmatch(r"ARGUMENTS\[(\d+)\]|(\d+)", token)
        if index:
            i = int(index.group(1) or index.group(2))
            return args[i] if i < len(args) else m.group(0)  # an indexed placeholder with no argument stays as written
        return named.get(token, "")  # a named placeholder with no argument is empty

    out = re.sub(r"\$(ARGUMENTS\[\d+\]|ARGUMENTS|\d+|" + "|".join(map(re.escape, names)) + r")" if names else r"\$(ARGUMENTS\[\d+\]|ARGUMENTS|\d+)", sub, body)
    if raw and not used:
        out = out.rstrip("\n") + f"\nARGUMENTS: {raw}\n"
    return out


def fork_agent(meta):
    """The subagent a forked skill runs in, or None when it runs in the conversation. The subagent does not see the conversation."""
    return None if meta.get("context") != "fork" else str(meta.get("agent") or "general-purpose")


SKILL = """---
name: release-tag
description: Tag a release and push the tag. Use when the user asks to cut a release.
disable-model-invocation: true
argument-hint: "[version]"
arguments: [version]
allowed-tools: Bash(git tag *) Bash(git push origin *)
disallowed-tools: Edit
---
Create the tag $version and push it.
"""


def main():
    meta, body = parse(SKILL)
    print("name:", command_name(".claude/skills/release-tag/SKILL.md", meta), "| legacy file:", command_name(".claude/commands/standup.md", {}))
    print("winner of three same-name skills:", winner({"project": "p/SKILL.md", "personal": "u/SKILL.md"}))
    print("who can start it:", invocation(meta))
    for tool, command in (("Bash", "git tag v1.2.0"), ("Bash", "git push --force"), ("Bash", "rm -rf build"), ("Edit", ""), ("Read", "")):
        print(f"{tool} {command!r}:", tool_status(meta, tool, command))
    print("bare Bash allowed:", pre_approved({"allowed-tools": "Bash"}, "Bash", "rm -rf build"), "| scoped disallow removes Edit:", removed({"disallowed-tools": "Edit(src/**)"}, "Edit"))
    print("render:", render(body, "v1.2.0", ["version"]).strip())
    print("render, no placeholder:", repr(render("Review the change.\n", "123")))
    print("quoted:", render("first=$0 second=$1", '"hello world" second'))
    print("fork:", fork_agent({"context": "fork"}), fork_agent({"context": "fork", "agent": "Explore"}), fork_agent({}))


if __name__ == "__main__":
    main()
