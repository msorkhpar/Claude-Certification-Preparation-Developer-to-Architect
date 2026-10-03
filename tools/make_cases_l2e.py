#!/usr/bin/env python3
"""Write cases.json for the practices of modules 38 to 41 from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/make_cases_l2e.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> {"name", "tests": {java, kotlin suite names}, "langs", "cases": [(id, kind, title)], "plants": {name: (caught_by, defect)}}
PRACTICES = {}

# --- PRACTICES BELOW ---

PRACTICES[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = {
    "name": "project_setup", "suite": "", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the memory file is short concrete and pulls in the architecture notes"),
        ("e1", "edge", "the permission rules allow the daily commands ask before commits and deny secrets and pushes"),
        ("e2", "edge", "the shared file sets a mode it may set and uses only rules that are consulted"),
        ("e3", "edge", "personal settings stay local and the local file wins"),
        ("e4", "edge", "the custom command is a skill that only a person can start"),
        ("e5", "edge", "the headless script is bounded and does not skip permissions"),
        ("e6", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-bypass-mode": (["e2"], "sets defaultMode to bypassPermissions in the shared file, where it is ignored"),
        "wrong-write-deny": (["e1"], "writes the secret rules for the Write tool, which no check consults, instead of Read"),
        "wrong-push-ask-only": (["e1"], "asks before a push instead of denying it"),
        "wrong-bare-bash-allow": (["e2"], "allows every Bash command with a bare Bash rule"),
        "wrong-local-not-ignored": (["e3"], "leaves settings.local.json out of .gitignore"),
        "wrong-local-widens": (["e3"], "puts a permission rule in the personal file"),
        "wrong-bloated-memory": (["m1"], "lets CLAUDE.md grow past 200 lines"),
        "wrong-no-import": (["m1"], "names the architecture file without the @ that imports it"),
        "wrong-skill-auto": (["e4"], "lets Claude start the fix-issue command on its own"),
        "wrong-headless-bypass": (["e5"], "skips permissions in the headless run"),
        "wrong-headless-unbounded": (["e5"], "leaves the headless run without a turn limit"),
        "wrong-key-in-script": (["e6"], "writes an API key into the script"),
    },
}

PRACTICES[f"{X}/39-extending-claude-code/unit-01/practice-1"] = {
    "name": "plugin_setup", "suite": "", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the hook script blocks pushes deletes and piped downloads in any spelling"),
        ("e1", "edge", "edits to protected paths stop with exit two and a reason"),
        ("e2", "edge", "an event it cannot read blocks the call and other tools are left alone"),
        ("e3", "edge", "the hook is registered for every tool it guards and found through the plugin root"),
        ("e4", "edge", "the skills set the right invocation rules and approve only patterns"),
        ("e5", "edge", "the subagent only reads is bounded and uses only fields a plugin agent honours"),
        ("e6", "edge", "the manifest names the plugin and pins its dependency to patch updates"),
        ("e7", "edge", "the team settings register the marketplace the enabled plugin comes from"),
    ],
    "plants": {
        "wrong-push-prefix": (["m1"], "refuses git push only when push is the first word after git, so git -C . push passes"),
        "wrong-rm-literal": (["m1"], "refuses only the exact flag spelling -rf"),
        "wrong-pipe-shell-allowed": (["m1"], "lets a download be piped into a shell"),
        "wrong-edit-exit-one": (["e1"], "exits 1 on a protected path, which does not block the call"),
        "wrong-no-reason": (["e1"], "blocks a protected path without saying why"),
        "wrong-bad-json-allowed": (["e2"], "lets a call through when the event cannot be read"),
        "wrong-matcher-bash-only": (["e3"], "registers the hook for Bash only, so edits are not guarded"),
        "wrong-relative-path": (["e3"], "finds the script by a path relative to the working directory instead of the plugin root"),
        "wrong-publish-auto": (["e4"], "lets Claude start the publish skill on its own"),
        "wrong-bare-bash": (["e4"], "pre-approves every shell command for the publish skill"),
        "wrong-agent-writes": (["e5"], "gives the reviewer the Edit tool"),
        "wrong-agent-bypass": (["e5"], "sets a permission mode that a plugin agent ignores"),
        "wrong-dependency-unpinned": (["e6"], "depends on secrets-vault with no version range"),
        "wrong-marketplace-mismatch": (["e7"], "enables the plugin from a marketplace that the file does not register"),
    },
}

PRACTICES[f"{X}/40-claude-in-the-software-life-cycle/unit-01/practice-1"] = {
    "name": "pipeline_setup", "suite": "", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the mention workflow answers only claude comments and holds no key"),
        ("e1", "edge", "the review workflow reads the code and posts the review"),
        ("e2", "edge", "each prompt file carries a version that its changelog explains"),
        ("e3", "edge", "the review guidance and the git workflow are files the reviewer and claude read"),
        ("e4", "edge", "every run is bounded by turns time and concurrency"),
        ("e5", "edge", "no file holds a key a personal path or an address"),
    ],
    "plants": {
        "wrong-mention-unguarded": (["m1"], "starts a runner on every comment instead of only on @claude mentions"),
        "wrong-mention-beta": (["m1"], "uses the beta action instead of v1"),
        "wrong-review-writes": (["e1"], "gives the review job write access to the contents"),
        "wrong-review-no-comment": (["e1"], "runs the review without --comment, so nothing is posted"),
        "wrong-review-no-checkout": (["e1"], "runs the review skill without checking the repository out"),
        "wrong-prompt-version-mismatch": (["e2"], "leaves the changelog on an older version than the prompt"),
        "wrong-prompt-no-version": (["e2"], "drops the version from the prompt file"),
        "wrong-review-no-skip": (["e3"], "has no Skip section in the review guidance"),
        "wrong-no-timeout": (["e4"], "leaves the mention job without a timeout"),
        "wrong-no-turn-cap": (["e4"], "leaves the mention run without a turn limit"),
        "wrong-literal-key": (["e5"], "writes the API key into the workflow"),
    },
}

PRACTICES[f"{X}/41-security-and-safety/unit-01/practice-1"] = {
    "name": "gate", "suite": "GateTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "untrusted text reaches the model only as one json string that says where it came from"),
        ("e1", "edge", "a screen names injection signals and a flagged result is withheld with an error"),
        ("e2", "edge", "reads and writes stay inside the project and away from secrets and protected folders"),
        ("e3", "edge", "bash is limited to a few read only commands and dangerous or chained ones are refused"),
        ("e4", "edge", "fetch and email obey the host and domain lists and refuse credentials in a url"),
        ("e5", "edge", "once untrusted content is in the session anything that changes things asks or is refused"),
        ("e6", "edge", "secrets card numbers and addresses are redacted in text and in the audit"),
        ("e7", "edge", "the hook answer follows the documented shapes and repeated denials raise an alert"),
    ],
    "plants": {
        "wrong-concatenate": (["m1"], "joins the source and the text into one string instead of encoding them as JSON"),
        "wrong-screen-case-sensitive": (["e1"], "matches the override signal only in the exact letter case"),
        "wrong-screen-leaks": (["e1"], "puts the start of the flagged text into the error that withholds it"),
        "wrong-prefix-sibling": (["e2"], "treats a folder whose name merely starts like the project folder as inside it"),
        "wrong-env-variants": (["e2"], "protects .env but not .env.local or the other .env files"),
        "wrong-chain-allowed": (["e3"], "checks the first word only, so a chained or redirected command passes"),
        "wrong-host-suffix": (["e4"], "matches a host by the end of its name without the dot, so a lookalike host passes"),
        "wrong-email-tainted-allowed": (["e5"], "lets an email go out after untrusted content entered the session"),
        "wrong-luhn-skip": (["e6"], "redacts any long number as a card without the Luhn check"),
        "wrong-audit-raw": (["e6"], "writes the arguments to the audit unredacted"),
        "wrong-hook-exit2": (["e7"], "answers a denial with exit code 2 and not with the documented JSON and exit code 0"),
        "wrong-alert-asks": (["e7"], "counts questions as denials when it raises an alert"),
    },
}

# --- PRACTICES ABOVE ---


def camel(title):
    words = title.split()
    return words[0] + "".join(w[:1].upper() + w[1:] for w in words[1:])


def main():
    problems = 0
    for practice, spec in PRACTICES.items():
        base = ROOT / practice
        cases, tests = {}, {}
        for cid, kind, title in spec["cases"]:
            cases[cid] = {"kind": kind, "python": f"test_{cid}_" + title.replace(" ", "_"), "typescript": f"{cid} {title}",
                          "java": f"{cid}_{camel(title)}", "kotlin": f"{cid}_{camel(title)}"}
        data = {"practice": practice.split(f"{X}/", 1)[1], "name": spec["name"], "tests": {"java": spec["suite"], "kotlin": spec["suite"]},
                "cases": {cid: {k: v for k, v in c.items() if k in ("kind",) + tuple(spec["langs"])} for cid, c in cases.items()},
                "plants": {n: {"caught_by": caught, "defect": defect} for n, (caught, defect) in spec["plants"].items()}}
        if spec["langs"] == ["python", "typescript"]:
            data.pop("tests")
        elif spec["langs"] != ["python", "typescript", "java", "kotlin"]:
            sys.exit(f"{practice}: unsupported languages {spec['langs']}")
        (base / "cases.json").write_text(json.dumps(data, indent=2) + "\n")
        for lang in spec["langs"]:
            found = {"python": next(base.glob("python/tests/*.py"), None), "typescript": next(base.glob("typescript/tests/*.test.ts"), None),
                     "java": next(base.glob("java/tests/*Test.java"), None), "kotlin": next(base.glob("kotlin/tests/*Test.kt"), None)}[lang]
            text = found.read_text() if found else ""
            for cid, c in cases.items():
                name = c[lang]
                ok = (f"def {name}(" in text) if lang == "python" else (f'test("{name}"' in text) if lang == "typescript" else (f"void {name}(" in text or f"fun {name}(" in text)
                if not ok:
                    print(f"{practice}/{lang}: test {name!r} not found")
                    problems += 1
        print(f"{practice}: cases.json written ({len(cases)} cases, {len(spec['plants'])} plants)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
