#!/usr/bin/env python3
"""Write cases.json for the practices of the Level 3 modules from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/l3_make_cases.py
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

PRACTICES[f"{X}/45-the-agentic-loop-in-depth/unit-01/practice-1"] = {
    "name": "agent", "suite": "AgentLoopTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a run alternates model and tools until the model ends its turn"),
        ("e1", "edge", "the stop reason decides and the words of the text do not"),
        ("e2", "edge", "every call of a turn is answered in one user message in order"),
        ("e3", "edge", "a failing or unknown tool becomes an error result and the run goes on"),
        ("e4", "edge", "the turn limit is a backstop that ends only a run the model has not ended"),
        ("e5", "edge", "a cut off or refused reply ends the run with its own status"),
        ("e6", "edge", "a tool use reply without a tool call is malformed and sends nothing more"),
    ],
    "plants": {
        "wrong-text-marker": (["e1"], "ends the run when the text says done, even if the reply also calls a tool"),
        "wrong-cap-reports-done": (["e4"], "reports a run that hit the turn limit as done"),
        "wrong-cap-off-by-one": (["e4"], "allows one model call more than the turn limit"),
        "wrong-results-split": (["e2"], "answers each tool call of a turn in its own user message"),
        "wrong-error-without-flag": (["e3"], "sends a failing tool's message back as an ordinary result"),
        "wrong-truncated-is-done": (["e5"], "treats a reply cut off by max_tokens as a finished one"),
        "wrong-malformed-continues": (["e6"], "sends an empty user message and calls the model again after a tool use reply with no call"),
    },
}

PRACTICES[f"{X}/46-coordinator-and-subagents/unit-01/practice-1"] = {
    "name": "coordinator", "suite": "CoordinatorTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a hub sends one brief to each spoke and synthesizes what comes back"),
        ("e1", "edge", "a question the coordinator can answer itself is answered without any subagent"),
        ("e2", "edge", "a subagent sees its own brief and nothing the others found"),
        ("e3", "edge", "the plan is cleaned of empty briefs and duplicate scopes and capped"),
        ("e4", "edge", "a failing subagent does not stop the others and a run with no findings does not synthesize"),
        ("e5", "edge", "a review sends only the gaps back out and stops when none are left"),
        ("e6", "edge", "the rounds are capped and the gaps that remain are reported"),
    ],
    "plants": {
        "wrong-leaks-context": (["e2"], "appends what earlier subagents found to the brief of the next one"),
        "wrong-no-dedupe": (["e3"], "runs two subagents on the same scope"),
        "wrong-empty-brief-sent": (["e3"], "sends a subagent an empty brief"),
        "wrong-always-delegates": (["e1"], "builds a team even when the planner answered the question itself"),
        "wrong-failure-as-finding": (["e4"], "reports a subagent's error message as a finding"),
        "wrong-synthesizes-nothing": (["e4"], "synthesizes an answer when no subagent returned anything"),
        "wrong-rerun-all": (["e5"], "sends every first-round brief out again in each refinement round"),
        "wrong-rounds-off-by-one": (["e6"], "runs one refinement round more than the limit"),
    },
}

PRACTICES[f"{X}/47-invoking-subagents/unit-01/practice-1"] = {
    "name": "subagents", "suite": "SubagentsTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the options register each named agent with its description prompt tools and model"),
        ("e1", "edge", "a subagent gets read only tools by default and never the right to spawn another"),
        ("e2", "edge", "a definition with a bad name or no description or no prompt is refused"),
        ("e3", "edge", "depth concurrency budget and turn limits are set on the options"),
        ("e4", "edge", "a spawn is recognised under the new and the old tool name and nothing else is"),
        ("e5", "edge", "messages from inside a subagent are grouped under the call that started it"),
        ("e6", "edge", "a brief carries the task and every fact the subagent needs in a fixed layout"),
        ("e7", "edge", "findings keep the claim apart from its source and merging keeps every source"),
        ("e8", "edge", "a run through the sdk shows the spawn the inner messages and the agents sent to the binary"),
    ],
    "plants": {
        "wrong-inherits-all-tools": (["e1"], "leaves the tools out of a definition that lists none, so the subagent inherits every tool"),
        "wrong-allows-nesting": (["e1"], "keeps the Agent tool in a subagent's own tool list"),
        "wrong-name-unchecked": (["e2"], "accepts any agent name"),
        "wrong-no-depth-limit": (["e3"], "does not limit how deep subagents may nest"),
        "wrong-budget-dropped": (["e3"], "forgets to pass the budget limit to the options"),
        "wrong-only-new-name": (["e4"], "recognises a spawn only under the current tool name"),
        "wrong-counts-everything": (["e5"], "credits the coordinator's own messages to the first subagent"),
        "wrong-brief-drops-facts": (["e6"], "leaves the known facts out of the brief"),
        "wrong-first-source-only": (["e7"], "keeps only the first source of a merged claim"),
        "wrong-source-in-claim": (["e7"], "writes the source into the claim text"),
    },
}

PRACTICES[f"{X}/48-multi-step-workflows-with-guarantees/unit-01/practice-1"] = {
    "name": "desk", "suite": "RefundDeskTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a verified customer can look up an order and be refunded within the limit"),
        ("e1", "edge", "a refund before identity is verified is blocked in code and never reaches the backend"),
        ("e2", "edge", "an order that belongs to someone else is neither shown nor refundable"),
        ("e3", "edge", "a refund is checked against the order its amount and what is left"),
        ("e4", "edge", "a refund over the limit is not executed and becomes a structured hand off"),
        ("e5", "edge", "a failed check does not unlock anything and three in a row lock the desk"),
        ("e6", "edge", "unknown tools and backend errors are reported and the hand off lists every block"),
    ],
    "plants": {
        "wrong-identity-unchecked": (["e1"], "lets any call but verification through without a verified customer"),
        "wrong-ownership-unchecked": (["e2"], "remembers and shows an order that belongs to another customer"),
        "wrong-over-limit-executes": (["e4"], "runs a refund above the limit instead of handing it to a person"),
        "wrong-failure-unlocks": (["e5"], "keeps the earlier verified customer after a failed check"),
        "wrong-no-lockout": (["e5"], "never locks the desk after repeated failed checks"),
        "wrong-exceeds-ignored": (["e3"], "refunds more than what is left on the order"),
        "wrong-zero-amount-ok": (["e3"], "accepts a refund of zero cents"),
        "wrong-handoff-no-blocks": (["e6"], "leaves the refusals out of the hand-off"),
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
