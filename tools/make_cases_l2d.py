#!/usr/bin/env python3
"""Write cases.json for the practices of modules 30 to 35 from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/make_cases_l2d.py
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

PRACTICES[f"{X}/30-vision-and-documents/unit-01/practice-1"] = {
    "name": "vision", "suite": "VisionTest", "langs": ["python", "typescript", "java", "kotlin"], "pyfile": "test_vision.py", "tsfile": "vision.test.ts",
    "cases": [
        ("m1", "main", "images come first with labels and the question last"),
        ("e1", "edge", "the token cost follows the models resolution tier"),
        ("e2", "edge", "formats dimensions sizes and counts are checked before any call"),
        ("e3", "edge", "more than twenty images lower the side limit to 2000 pixels"),
        ("e4", "edge", "an image whose size matters is rejected instead of resized"),
        ("e5", "edge", "pdfs become document blocks in order and are limited by pages"),
        ("e6", "edge", "bedrock and vertex take only base64 sources and smaller images"),
        ("e7", "edge", "coordinates map back to the original and cost follows the price"),
    ],
    "plants": {
        "wrong-edge-only-resize": (["e1"], "sizes an image by the edge limit alone and ignores the visual token budget"),
        "wrong-text-first": (["m1"], "puts the question before the images"),
        "wrong-no-labels": (["m1"], "does not label the images when there are several"),
        "wrong-padded-coordinates": (["e7"], "divides a returned coordinate by the padded height instead of the resized height"),
        "wrong-same-limit-all-models": (["e2"], "allows 600 images for every model, including the 200k-context one"),
        "wrong-many-image-rule": (["e3"], "ignores the 2000 pixel limit that applies above 20 images"),
        "wrong-exact-silent": (["e4"], "lets an oversized image through even when its exact size matters"),
    },
}

PRACTICES[f"{X}/31-computer-use/unit-01/practice-1"] = {
    "name": "computer", "suite": "ComputerTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the loop runs scaled actions and sends every result in one message"),
        ("e1", "edge", "the screen is scaled to what the model may see and clicks are scaled back"),
        ("e2", "edge", "a click on a risky element needs a persons confirmation"),
        ("e3", "edge", "a failed action stops the rest of its batch"),
        ("e4", "edge", "invalid actions come back as error results with a reason"),
        ("e5", "edge", "the loop ends on a stop reason or at the turn limit"),
        ("e6", "edge", "old screenshots are replaced so the context does not fill up"),
        ("e7", "edge", "screenshots and zooms are sent at the size the model may see"),
    ],
    "plants": {
        "wrong-unscaled-click": (["m1"], "uses the model's coordinates as screen coordinates without scaling them back"),
        "wrong-no-confirm": (["e2"], "clicks a payment element without asking a person"),
        "wrong-continue-after-failure": (["e3"], "keeps running the actions of a batch after one has failed"),
        "wrong-result-per-message": (["m1"], "sends each tool result in a user message of its own"),
        "wrong-screenshot-full-size": (["e7"], "sends the screenshot at the full screen size instead of the scaled size"),
        "wrong-prune-oldest-kept": (["e6"], "keeps the oldest screenshots and removes the newest"),
        "wrong-extra-turn": (["e5"], "makes one model call more than the turn limit"),
    },
}

PRACTICES[f"{X}/34-workflows-and-agents/unit-01/practice-1"] = {
    "name": "workflows", "suite": "WorkflowsTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "an orchestrator plans runs a worker per subtask and combines"),
        ("e1", "edge", "the plan is read from prose cleaned capped and replaced when unusable"),
        ("e2", "edge", "one failing worker does not stop the others or the answer"),
        ("e3", "edge", "a draft is revised with the feedback until the judge accepts it"),
        ("e4", "edge", "when the rounds run out the best draft wins and an unreadable judge scores zero"),
        ("e5", "edge", "a label is read from the reply and anything else takes the default route"),
        ("e6", "edge", "the majority answer wins and a tie goes to the one seen first"),
        ("e7", "edge", "a writer that fails ends the loop with the best draft so far"),
    ],
    "plants": {
        "wrong-no-cap": (["e1"], "runs every subtask the planner lists instead of the first max_subtasks"),
        "wrong-stop-at-first-failure": (["e2"], "stops running workers after the first one fails"),
        "wrong-no-feedback": (["e3"], "revises without sending the judge's feedback to the writer"),
        "wrong-last-draft": (["e4"], "returns the last draft instead of the best one when the rounds run out"),
        "wrong-trust-unreadable-judge": (["e4"], "scores a judge reply it cannot read as a pass"),
        "wrong-punctuation-kept": (["e5"], "does not strip punctuation around the label, so a reply of Billing. misses its route"),
        "wrong-tie-goes-last": (["e6"], "breaks a voting tie in favour of the answer seen last"),
    },
}

PRACTICES[f"{X}/33-mcp-advanced/unit-01/practice-1"] = {
    "name": "mrtr", "suite": "MrtrTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a production deploy takes three round trips and keeps no state"),
        ("e1", "edge", "a staging deploy and a status call finish at once and the tool list is cacheable"),
        ("e2", "edge", "the server only asks what the client declared it can answer"),
        ("e3", "edge", "a no or a missing answer is handled without an error"),
        ("e4", "edge", "a state the server did not sign is refused"),
        ("e5", "edge", "a state works only for the same user the same call and before it expires"),
        ("e6", "edge", "a request the server cannot serve is a protocol error with a code"),
        ("e7", "edge", "the state carries the whole context and an answer alone never skips a step"),
    ],
    "plants": {
        "wrong-no-expiry": (["e5"], "accepts a requestState after its expiry time"),
        "wrong-any-principal": (["e5"], "accepts a requestState that was issued to another user"),
        "wrong-no-digest": (["e5"], "accepts a requestState on a call whose arguments differ from the ones it was issued for"),
        "wrong-bad-state-restarts": (["e4"], "silently starts the flow over when the requestState is not one it signed"),
        "wrong-elicit-without-capability": (["e2"], "sends an elicitation request to a client that did not declare the capability"),
        "wrong-decline-continues": (["e3"], "carries on with the deployment after the user declined"),
        "wrong-error-on-missing-input": (["e3"], "answers a retry that lacks the requested input with a protocol error instead of asking again"),
    },
}

PRACTICES[f"{X}/35-the-claude-agent-sdk/unit-01/practice-1"] = {
    "name": "agent", "suite": "", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "every tool call goes through the permission callback and the run is summarised"),
        ("e1", "edge", "the options reach the cli as flags and the run starts in the project"),
        ("e2", "edge", "file tools stay inside the project and away from secrets"),
        ("e3", "edge", "bash is limited to a few commands and dangerous ones stop the run"),
        ("e4", "edge", "a hook blocks a push before the permission callback is asked"),
        ("e5", "edge", "the messages of a run fold into a summary"),
        ("e6", "edge", "a run that hits the turn limit ends with that status"),
        ("e7", "edge", "denied calls are counted and the run still ends with a result"),
    ],
    "plants": {
        "wrong-auto-approve": (["e1"], "lists the read tools in allowed_tools, so they run without the permission callback ever being asked"),
        "wrong-edit-in-readonly": (["e2"], "lets Write and Edit through in read-only mode"),
        "wrong-env-variants": (["e2"], "protects .env but not .env.local or other .env files"),
        "wrong-chaining-allowed": (["e3"], "allows a command that chains another with a semicolon or a pipe when it starts with a safe word"),
        "wrong-danger-no-interrupt": (["e3"], "denies sudo and rm -rf without interrupting the run"),
        "wrong-push-substring": (["e4"], "blocks any command that merely starts a word with git push, such as git pushd"),
        "wrong-no-turn-limit": (["e6"], "leaves max turns unset, so the run is not capped"),
        "wrong-status-unmapped": (["e5"], "reports the raw result subtype instead of the course status for the turn limit"),
    },
}

PRACTICES[f"{X}/32-mcp-fundamentals/unit-01/practice-1"] = {
    "name": "notes", "suite": "NotesTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a client can save a note find it and read it back"),
        ("e1", "edge", "the server declares tools resources and prompts and names its tools"),
        ("e2", "edge", "bad input comes back as a tool error the model can read"),
        ("e3", "edge", "search ignores case keeps id order honours the limit and says when nothing matches"),
        ("e4", "edge", "tool annotations tell a client which tool only reads"),
        ("e5", "edge", "resources give the count a note by id and an error for a missing one"),
        ("e6", "edge", "the prompt lists the notes and defaults the tone"),
        ("e7", "edge", "ids are sequential and a failed call does not use one"),
    ],
    "plants": {
        "wrong-error-not-flagged": (["e2"], "reports bad input as an ordinary result instead of a tool error"),
        "wrong-case-sensitive": (["e3"], "matches the query only with the same letter case"),
        "wrong-no-limit": (["e3"], "returns every hit and ignores the limit argument"),
        "wrong-read-only-unmarked": (["e4"], "does not mark the search tool as read-only"),
        "wrong-plural": (["e5"], "says 1 notes instead of 1 note"),
        "wrong-default-tone": (["e6"], "uses another default tone when the client gives none"),
        "wrong-no-trim": (["e7"], "keeps the spaces around a title and a text, so a blank one passes as given"),
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
