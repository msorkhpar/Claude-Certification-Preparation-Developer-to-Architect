#!/usr/bin/env python3
"""A PreToolUse hook: reads one event from standard input and answers with its exit code, standard output and standard error."""
import json
import re
import shlex
import sys

PROTECTED = (".env", "package-lock.json", ".git/", "secrets/")


def git_subcommand(args):
    i = 0
    while i < len(args):
        if args[i] in ("-C", "-c", "--git-dir", "--work-tree"):
            i += 2
        elif args[i].startswith("-"):
            i += 1
        else:
            return args[i]
    return None


def reason_to_refuse(command):
    parts = re.split(r"&&|\|\||;|&|\n", command)
    for part in parts:
        stages = part.split("|")
        for position, stage in enumerate(stages):
            try:
                words = shlex.split(stage)
            except ValueError:
                return "a command that cannot be parsed is not run unreviewed"
            while words and re.fullmatch(r"\w+=\S*", words[0]):
                words = words[1:]
            if not words:
                continue
            program, args = words[0].rsplit("/", 1)[-1], words[1:]
            if program in ("sh", "bash", "zsh") and "-c" in args and args.index("-c") + 1 < len(args):
                inner = reason_to_refuse(args[args.index("-c") + 1])
                if inner:
                    return inner
            if program in ("sh", "bash", "zsh") and position > 0 and "-c" not in args:
                first = shlex.split(stages[0]) if stages[0].strip() else [""]
                if first[0].rsplit("/", 1)[-1] in ("curl", "wget"):
                    return "a download piped into a shell is not run"
            if program == "git" and git_subcommand(args) == "push":
                return "nothing is pushed from an agent"
            short = "".join(a[1:] for a in args if a.startswith("-") and not a.startswith("--"))
            if program == "rm" and ("r" in short or "R" in short or "--recursive" in args) and ("f" in short or "--force" in args):
                return "a recursive forced delete is not run from an agent"
    return None


def main():
    try:
        event = json.load(sys.stdin)
        tool, tool_input = event["tool_name"], event.get("tool_input") or {}
    except (ValueError, KeyError, TypeError):
        sys.stderr.write("Blocked: the hook event could not be read")
        return 2
    if tool == "Bash":
        reason = reason_to_refuse(str(tool_input.get("command", "")))
        if reason:
            print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": reason}}))
        return 0
    if tool in ("Edit", "Write", "MultiEdit"):
        path = str(tool_input.get("file_path", "")).replace("\\", "/")
        for pattern in PROTECTED:
            if pattern in path:
                return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
