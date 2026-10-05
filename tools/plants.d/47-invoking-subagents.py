# Planted wrong solutions of module 47-invoking-subagents: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/47-invoking-subagents/unit-01/practice-1"] = {
    "python": ("subagents.py", {
        "wrong-inherits-all-tools": [('tools = list(READ_ONLY) if listed is None else [t for t in listed if t != "Agent"]', "tools = listed")],
        "wrong-allows-nesting": [('[t for t in listed if t != "Agent"]', "list(listed)")],
        "wrong-name-unchecked": [('if not re.fullmatch(r"[a-z][a-z0-9-]*", name):', "if False:")],
        "wrong-no-depth-limit": [('"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", ', "")],
        "wrong-budget-dropped": [("max_budget_usd=max_budget_usd, ", "")],
        "wrong-only-new-name": [('SPAWN_TOOLS = ("Agent", "Task")', 'SPAWN_TOOLS = ("Agent",)')],
        "wrong-counts-everything": [('parent = getattr(message, "parent_tool_use_id", None)\n', 'parent = getattr(message, "parent_tool_use_id", None) or next(iter(groups), None)\n')],
        "wrong-brief-drops-facts": [('(("Files", files), ("Known", facts))', '(("Files", files),)')],
        "wrong-first-source-only": [('if finding["source"] is not None and finding["source"] not in entry["sources"]:', 'if finding["source"] is not None and not entry["sources"]:')],
        "wrong-source-in-claim": [('return {"claim": claim.strip(), "source": source or None}', 'return {"claim": claim.strip() + (f" ({url})" if url else ""), "source": source or None}')],
    }),
    "typescript": ("subagents.ts", {
        "wrong-inherits-all-tools": [('const tools = spec.tools == null ? [...READ_ONLY] : spec.tools.filter((t) => t !== "Agent");', "const tools = spec.tools;")],
        "wrong-allows-nesting": [('spec.tools.filter((t) => t !== "Agent")', "[...spec.tools]")],
        "wrong-name-unchecked": [("if (!/^[a-z][a-z0-9-]*$/.test(name)) throw", "if (false) throw")],
        "wrong-no-depth-limit": [('CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1", ', "")],
        "wrong-budget-dropped": [("maxTurns, maxBudgetUsd, cwd,", "maxTurns, cwd,")],
        "wrong-only-new-name": [('const SPAWN_TOOLS = ["Agent", "Task"];', 'const SPAWN_TOOLS = ["Agent"];')],
        "wrong-counts-everything": [("const parent = message?.parent_tool_use_id;\n", "const parent = message?.parent_tool_use_id ?? Object.keys(groups)[0];\n")],
        "wrong-brief-drops-facts": [('[["Files", files], ["Known", facts]] as const', '[["Files", files]] as const')],
        "wrong-first-source-only": [("if (finding.source !== null && !entry.sources.some(", "if (finding.source !== null && entry.sources.length === 0 && !entry.sources.some(")],
        "wrong-source-in-claim": [("return { claim: claim.trim(), source:", 'return { claim: claim.trim() + (url ? ` (${url})` : ""), source:')],
    }),
}
