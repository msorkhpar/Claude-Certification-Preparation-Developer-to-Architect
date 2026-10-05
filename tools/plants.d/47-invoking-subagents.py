# Planted wrong solutions of module 47-invoking-subagents: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/47-invoking-subagents/unit-01/practice-1"] = {
    "python": ("subagents.py", {
        'wrong-allows-nesting': [('return [t for t in listed if t != "Agent"]', 'return list(listed)')],
        'wrong-empty-list-widened': [('    if listed is None:', '    if not listed:')],
        'wrong-name-unchecked': [('return re.fullmatch(r"[a-z][a-z0-9-]*", name) is not None', 'return bool(name)')],
        'wrong-blank-description-accepted': [('bool(str(spec.get("description", "")).strip()) and', '"description" in spec and')],
        'wrong-no-depth-limit': [('"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", ', '')],
        'wrong-concurrency-fixed': [('str(max_concurrent)', '"5"')],
        'wrong-only-new-name': [('SPAWN_TOOLS = ("Agent", "Task")', 'SPAWN_TOOLS = ("Agent",)')],
        'wrong-tools-repeated': [('if name and name not in group["tools"]:', 'if name:')],
        'wrong-brief-drops-facts': [('(("Files", files), ("Known", facts))', '(("Files", files),)')],
        'wrong-blank-items-kept': [('kept = [str(item).strip() for item in items or [] if str(item).strip()]', 'kept = [str(item).strip() for item in items or []]')],
        'wrong-first-source-only': [('if source is not None and source not in entry["sources"]:', 'if source is not None and not entry["sources"]:')],
        'wrong-source-in-claim': [('return {"claim": claim.strip(), "source": source or None}', 'return {"claim": claim.strip() + (f" ({url})" if url else ""), "source": source or None}')],
        'wrong-run-keeps-nothing': [('            messages.append(message)', '            pass')],
    }),
    "typescript": ("subagents.ts", {
        'wrong-allows-nesting': [('return listed.filter((t) => t !== "Agent");', 'return [...listed];')],
        'wrong-empty-list-widened': [('if (listed == null) return', 'if (!listed || listed.length === 0) return')],
        'wrong-name-unchecked': [('return /^[a-z][a-z0-9-]*$/.test(name);', 'return name.length > 0;')],
        'wrong-blank-description-accepted': [('Boolean(String(spec.description ?? "").trim()) &&', 'spec.description !== undefined &&')],
        'wrong-no-depth-limit': [('CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1", ', '')],
        'wrong-concurrency-fixed': [('String(maxConcurrent)', '"5"')],
        'wrong-only-new-name': [('const SPAWN_TOOLS = ["Agent", "Task"];', 'const SPAWN_TOOLS = ["Agent"];')],
        'wrong-tools-repeated': [('if (block.type === "tool_use" && !group.tools.includes(block.name)) group.tools.push', 'if (block.type === "tool_use") group.tools.push')],
        'wrong-brief-drops-facts': [('[["Files", files], ["Known", facts]] as const', '[["Files", files]] as const')],
        'wrong-blank-items-kept': [('.map((item) => String(item).trim()).filter((item) => item !== "");', '.map((item) => String(item).trim());')],
        'wrong-first-source-only': [('if (source !== null && !entry.sources.some(', 'if (source !== null && entry.sources.length === 0 && !entry.sources.some(')],
        'wrong-source-in-claim': [('return { claim: claim.trim(), source:', 'return { claim: claim.trim() + (url ? ` (${url})` : ""), source:')],
        'wrong-run-keeps-nothing': [('messages.push(message);', 'void message;')],
    }),
}
