#!/usr/bin/env python3
"""A PreToolUse hook: reads one event from standard input and answers with its exit code, standard output and standard error."""
import json
import logging
import re
import shlex
import sys

log = logging.getLogger(__name__)

SHELLS = ("sh", "bash", "zsh")
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


def is_forced_recursive_rm(program, args):
    short = "".join(a[1:] for a in args if a.startswith("-") and not a.startswith("--"))
    return program == "rm" and ("r" in short or "R" in short or "--recursive" in args) and ("f" in short or "--force" in args)


def pipes_download_into_shell(program, args, position, stages):
    if program not in SHELLS or position == 0 or "-c" in args:
        return False
    first = shlex.split(stages[0]) if stages[0].strip() else [""]
    return first[0].rsplit("/", 1)[-1] in ("curl", "wget")


def protected_pattern(path):
    for pattern in PROTECTED:
        if pattern in path:
            return pattern
    return None


def read_event(raw):
    try:
        event = json.loads(raw)
        return event["tool_name"], event.get("tool_input") or {}
    except (ValueError, KeyError, TypeError):
        return None


def reason_to_refuse(command):
    log.debug("reason_to_refuse input: %r", command)
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
            if program in SHELLS and "-c" in args and args.index("-c") + 1 < len(args):
                inner = reason_to_refuse(args[args.index("-c") + 1])
                if inner:
                    return inner
            if pipes_download_into_shell(program, args, position, stages):
                return "a download piped into a shell is not run"
            if program == "git" and git_subcommand(args) == "push":
                return "nothing is pushed from an agent"
            if is_forced_recursive_rm(program, args):
                return "a recursive forced delete is not run from an agent"
    return None


def main():
    event = read_event(sys.stdin.read())
    if event is None:
        sys.stderr.write("Blocked: the hook event could not be read")
        return 2
    tool, tool_input = event
    if tool == "Bash":
        reason = reason_to_refuse(str(tool_input.get("command", "")))
        if reason:
            print(json.dumps({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": reason}}))
        return 0
    if tool in ("Edit", "Write", "MultiEdit"):
        path = str(tool_input.get("file_path", "")).replace("\\", "/")
        pattern = protected_pattern(path)
        if pattern:
            sys.stderr.write(f"Blocked: {path} is protected ({pattern})")
            return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
