# Case lists of module 47-invoking-subagents: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

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
        "wrong-allows-nesting": (["e1"], "keeps the Agent tool in a subagent's own tool list"),
        "wrong-empty-list-widened": (["e1"], "treats an explicitly empty tool list as no list and hands out the read only tools"),
        "wrong-name-unchecked": (["e2"], "accepts any agent name"),
        "wrong-blank-description-accepted": (["e2"], "accepts a description that is only spaces"),
        "wrong-no-depth-limit": (["e3"], "does not limit how deep subagents may nest"),
        "wrong-concurrency-fixed": (["e3"], "ignores the requested number of subagents at once"),
        "wrong-only-new-name": (["e4"], "recognises a spawn only under the current tool name"),
        "wrong-tools-repeated": (["e5"], "lists a tool once for every time it was used inside the subagent"),
        "wrong-brief-drops-facts": (["e6"], "leaves the known facts out of the brief"),
        "wrong-blank-items-kept": (["e6"], "keeps blank items as empty list lines in the brief"),
        "wrong-first-source-only": (["e7"], "keeps only the first source of a merged claim"),
        "wrong-source-in-claim": (["e7"], "writes the source into the claim text"),
        "wrong-run-keeps-nothing": (["e8"], "collects no message of the run"),
    },
}
