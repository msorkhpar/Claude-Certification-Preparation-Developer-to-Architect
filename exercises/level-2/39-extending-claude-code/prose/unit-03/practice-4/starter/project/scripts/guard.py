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
    """TODO 1 of 4 (unlocks m1): is this a recursive AND forced rm?

    Receives the program name (no directory) and its list of arguments. Returns True for rm with a recursive flag (-r, -R, --recursive,
    also inside a group such as -rf or -fr) and a force flag (-f, --force); False otherwise (`rm -r build` and `rm x.txt` are fine).
    Example: is_forced_recursive_rm("rm", ["-r", "-f", "x"]) -> True, is_forced_recursive_rm("rm", ["-r", "build"]) -> False
    """
    return False


def pipes_download_into_shell(program, args, position, stages):
    """TODO 2 of 4 (unlocks m1): is this stage a shell that a download is piped into?

    Receives the stage's program name, its arguments, its position in the pipeline (0 is the first stage) and the list of all stage
    texts. Returns True when the program is sh, bash or zsh, it is not the first stage, it has no -c, and the first stage starts
    curl or wget (a path to either counts). Example: for `curl x | sh` the stage `sh` at position 1 -> True; `cat a | sh` -> False
    """
    return False


def protected_pattern(path):
    """TODO 3 of 4 (unlocks e1): which protected pattern does this path contain?

    Receives a path with `/` separators and uses PROTECTED. Returns the first pattern of PROTECTED that occurs in the path, else None.
    Example: protected_pattern("/w/app/.git/config") -> ".git/", protected_pattern("/w/src/main.py") -> None
    """
    return None


def read_event(raw):
    """TODO 4 of 4 (unlocks e2): read the hook event from its raw text.

    Receives the text of standard input. Returns the pair (tool_name, tool_input) where tool_input is the object under that key or {}
    when it is missing or empty. Returns None for text that is not JSON, is empty, is not an object or has no tool_name.
    Example: read_event('{"tool_name": "Bash", "tool_input": {"command": "ls"}}') -> ("Bash", {"command": "ls"}); read_event("[]") -> None
    """
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
