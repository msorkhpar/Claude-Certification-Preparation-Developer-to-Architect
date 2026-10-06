"""A PreToolUse hook that blocks destructive commands, and a linter for skill and subagent files.

Claude Code starts a hook as a process, writes one JSON event to its standard input and reads the answer from its exit code, its
standard output and its standard error. Exit code 2 blocks the call and the standard error is the reason; exit code 0 with a JSON
`permissionDecision` answers in a structured way; exit code 0 with nothing printed gives no opinion. This file is such a hook (run it
with --hook) and a demonstration (run it plain). The event and output shapes are those of the Claude Code hooks reference, read on
2026-10-03. The command check normalises what a prefix rule such as `Bash(git push *)` would miss: another form of the same command.
"""
import logging
import json
import re
import shlex
import sys

from miniyaml import split_frontmatter

log = logging.getLogger(__name__)

PROTECTED = (".env", "package-lock.json", ".git/")


def _git_subcommand(args):
    i = 0
    while i < len(args):
        if args[i] in ("-C", "-c", "--git-dir", "--work-tree"):
            i += 2
        elif args[i].startswith("-"):
            i += 1
        else:
            return args[i]
    return None


def dangerous(command):
    """The reason a command is refused, or None. Looks through compound commands, `sh -c`, env assignments, paths and git options."""
    for part in re.split(r"&&|\|\||;|\||&|\n", command):
        try:
            words = shlex.split(part)
        except ValueError:
            return "a command that cannot be parsed is not run unreviewed"
        while words and re.fullmatch(r"\w+=\S*", words[0]):
            words = words[1:]
        if not words:
            continue
        program, args = words[0].rsplit("/", 1)[-1], words[1:]
        if program in ("sh", "bash", "zsh") and "-c" in args and args.index("-c") + 1 < len(args):
            inner = dangerous(args[args.index("-c") + 1])
            if inner:
                return inner
        if program == "git" and _git_subcommand(args) == "push":
            return "nothing is pushed from an agent"
        short = "".join(a[1:] for a in args if a.startswith("-") and not a.startswith("--"))
        recursive, force = "r" in short or "R" in short or "--recursive" in args, "f" in short or "--force" in args
        if program == "rm" and recursive and force:
            return "a recursive forced delete is not run from an agent"
    return None


def pre_tool_use(event):
    """(exit code, standard output, standard error) for one PreToolUse event."""
    tool, tool_input = event["tool_name"], event.get("tool_input", {})
    if tool == "Bash":
        reason = dangerous(tool_input.get("command", ""))
        if reason:
            out = {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": reason}}
            return 0, json.dumps(out), ""
    if tool in ("Edit", "Write", "MultiEdit"):
        path = tool_input.get("file_path", "").replace("\\", "/")
        for pattern in PROTECTED:
            if pattern in path:
                return 2, "", f"Blocked: {path} matches protected pattern '{pattern}'"
    return 0, "", ""


# --- linting the files that configure Claude Code -----------------------------------------------------------------------------

SIDE_EFFECTS = re.compile(r"\b(deploy|publish|delete|commit|push|merge)\b", re.I)


def lint_skill(text):
    fm, _ = split_frontmatter(text)
    findings = []
    desc = str(fm.get("description") or "")
    if not desc:
        findings.append("description is missing: Claude uses it to decide when to load the skill")
    if len(desc) + len(str(fm.get("when_to_use") or "")) > 1536:
        findings.append("description and when_to_use together exceed 1,536 characters and are cut")
    if SIDE_EFFECTS.search(f"{fm.get('name', '')} {desc}") and fm.get("disable-model-invocation") is not True:
        findings.append("a skill with side effects should set disable-model-invocation: true")
    allowed = fm.get("allowed-tools")
    if allowed and "Bash" in str(allowed).replace(",", " ").split():
        findings.append("allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead")
    return findings


def lint_agent(text):
    fm, _ = split_frontmatter(text)
    findings = [f"{k} is required" for k in ("name", "description") if not fm.get(k)]
    if "tools" not in fm:
        findings.append("tools is omitted: the subagent inherits every tool")
    if fm.get("memory") is not None and fm["memory"] not in ("user", "project", "local"):
        findings.append("memory must be user, project or local")
    if fm.get("permissionMode") == "bypassPermissions":
        findings.append("permissionMode bypassPermissions skips every prompt in this subagent")
    return findings


SKILL = "---\nname: deploy\ndescription: Deploy the service to production\nallowed-tools: Bash\n---\nRun the release script.\n"
AGENT = "---\nname: reviewer\ndescription: Reviews a diff for bugs\nmemory: team\n---\nYou review code.\n"


def demo():
    events = [("Bash", {"command": "git push origin main"}), ("Bash", {"command": "git -C . push origin main"}), ("Bash", {"command": "bash -c 'rm -rf build'"}),
              ("Bash", {"command": "ls && git status"}), ("Bash", {"command": "rm build/old.txt"}), ("Edit", {"file_path": "/work/app/.env"}), ("Edit", {"file_path": "/work/app/main.py"})]
    for tool, tool_input in events:
        code, out, err = pre_tool_use({"hook_event_name": "PreToolUse", "tool_name": tool, "tool_input": tool_input})
        shown = f"deny (exit 0, JSON): {json.loads(out)['hookSpecificOutput']['permissionDecisionReason']}" if out else f"block (exit 2): {err}" if code == 2 else "no decision (exit 0)"
        print(f"{tool} {next(iter(tool_input.values()))!r} -> {shown}")
    print("skill findings:", lint_skill(SKILL))
    print("agent findings:", lint_agent(AGENT))


if __name__ == "__main__":
    if "--hook" in sys.argv:
        code, out, err = pre_tool_use(json.load(sys.stdin))
        sys.stdout.write(out)
        sys.stderr.write(err)
        sys.exit(code)
    demo()
