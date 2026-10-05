# Planted wrong solutions of module 67-exploring-a-large-codebase: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/67-exploring-a-large-codebase/unit-01/practice-1"] = {
    "python": ("recovery.py", {
        "wrong-duplicate-findings": [('    if any(f["area"] == area and f["fact"] == fact for f in findings):\n        return list(findings)\n', "")],
        "wrong-ungrouped-scratchpad": [('        if f["area"] not in areas:\n            areas.append(f["area"])', '        areas.append(f["area"])')],
        "wrong-manifest-unsorted": [('for a in sorted(agents, key=lambda a: a["name"])', "for a in agents")],
        "wrong-manifest-unvalidated": [("    for a in agents:\n        if a[\"status\"] not in STATUSES:\n            raise ValueError(f\"unknown status {a['status']}\")\n", "")],
        "wrong-rerun-done": [('        elif a["status"] == "done":\n            action = "reuse"', '        elif False:\n            action = "reuse"')],
        "wrong-restart-running": [('        else:\n            action = "resume"', '        else:\n            action = "restart"')],
        "wrong-ignore-missing-file": [('        if a["state_file"] not in existing_files:\n            action = "restart"\n        elif', '        if False:\n            action = "restart"\n        elif')],
        "wrong-no-continue-line": [(r'"\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('"/compact" if not keep else "/compact Focus on " + ", ".join(keep)', '"/compact"')],
    }),
    "typescript": ("recovery.ts", {
        "wrong-duplicate-findings": [("  if (findings.some((f) => f.area === area && f.fact === fact)) return [...findings];\n", "")],
        "wrong-ungrouped-scratchpad": [("if (!areas.includes(f.area)) areas.push(f.area);", "areas.push(f.area);")],
        "wrong-manifest-unsorted": [("const sorted = [...agents].sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));", "const sorted = [...agents];")],
        "wrong-manifest-unvalidated": [("  for (const a of agents) if (!STATUSES.includes(a.status)) throw new Error(`unknown status ${a.status}`);\n", "")],
        "wrong-rerun-done": [('else if (a.status === "done") action = "reuse";', 'else if (false) action = "reuse";')],
        "wrong-restart-running": [('else action = "resume";', 'else action = "restart";')],
        "wrong-ignore-missing-file": [('if (!existingFiles.has(a.state_file)) action = "restart";', 'if (false) action = "restart";')],
        "wrong-no-continue-line": [('"\\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('keep.length === 0 ? "/compact" : "/compact Focus on " + keep.join(", ")', '"/compact"')],
    }),
    "java": ("Recovery.java", {
        "wrong-duplicate-findings": [("        for (Finding f : findings) if (f.area().equals(area) && f.fact().equals(fact)) return out;\n", "")],
        "wrong-ungrouped-scratchpad": [("if (!areas.contains(f.area())) areas.add(f.area());", "areas.add(f.area());")],
        "wrong-manifest-unsorted": [("        sorted.sort(Comparator.comparing(AgentEntry::name));\n", "")],
        "wrong-manifest-unvalidated": [('        for (AgentEntry a : agents) if (!STATUSES.contains(a.status())) throw new IllegalArgumentException("unknown status " + a.status());\n', "")],
        "wrong-rerun-done": [('else if (a.status().equals("done")) action = "reuse";', 'else if (false) action = "reuse";')],
        "wrong-restart-running": [('else action = "resume";', 'else action = "restart";')],
        "wrong-ignore-missing-file": [('if (!existingFiles.contains(a.stateFile())) action = "restart";', 'if (false) action = "restart";')],
        "wrong-no-continue-line": [('"\\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('return keep.isEmpty() ? "/compact" : "/compact Focus on " + String.join(", ", keep);', 'return "/compact";')],
    }),
    "kotlin": ("Recovery.kt", {
        "wrong-duplicate-findings": [("if (findings.any { it.area == area && it.fact == fact }) findings.toList() else findings + Finding(area, fact, location)", "findings + Finding(area, fact, location)")],
        "wrong-ungrouped-scratchpad": [("findings.map { it.area }.distinct().joinToString", "findings.map { it.area }.joinToString")],
        "wrong-manifest-unsorted": [("agents.sortedBy { it.name }", "agents")],
        "wrong-manifest-unvalidated": [('    for (a in agents) require(a.status in STATUSES) { "unknown status ${a.status}" }\n', "")],
        "wrong-rerun-done": [('a.status == "done" -> "reuse"', 'false -> "reuse"')],
        "wrong-restart-running": [('else -> "resume"', 'else -> "restart"')],
        "wrong-ignore-missing-file": [('a.stateFile !in existingFiles -> "restart"', 'false -> "restart"')],
        "wrong-no-continue-line": [('"\\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('if (keep.isEmpty()) "/compact" else "/compact Focus on " + keep.joinToString(", ")', '"/compact"')],
    }),
}
