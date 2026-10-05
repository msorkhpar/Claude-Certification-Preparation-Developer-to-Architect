# Planted wrong solutions of module agent-loop: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/agent-loop"] = {
    "python": ("agent.py", {
        "wrong-ignores-stop-reason": [('        if resp["stop_reason"] != "tool_use":', '        if _text(resp["content"]):')],
        "wrong-one-turn-per-result": [('        messages.append({"role": "user", "content": results})',
                                       '        messages.extend({"role": "user", "content": [r]} for r in results)')],
    }),
    "typescript": ("agent.ts", {
        "wrong-ignores-stop-reason": [('    if (resp.stop_reason !== "tool_use") return textOf(resp.content);', "    if (textOf(resp.content)) return textOf(resp.content);")],
        "wrong-one-turn-per-result": [('    messages.push({ role: "user", content: results });',
                                       '    for (const r of results) messages.push({ role: "user", content: [r] });')],
    }),
    "java": ("Agent.java", {
        "wrong-ignores-stop-reason": [('            if (!"tool_use".equals(resp.get("stop_reason"))) return text(resp.get("content"));',
                                       '            if (!text(resp.get("content")).isEmpty()) return text(resp.get("content"));')],
        "wrong-one-turn-per-result": [('            messages.add(Map.of("role", "user", "content", results));',
                                       '            for (var r : results) messages.add(Map.of("role", "user", "content", List.of(r)));')],
    }),
    "kotlin": ("Agent.kt", {
        "wrong-ignores-stop-reason": [('        if (resp["stop_reason"] != "tool_use") return textOf(resp["content"])',
                                       '        if (textOf(resp["content"]).isNotEmpty()) return textOf(resp["content"])')],
        "wrong-one-turn-per-result": [('        messages += mapOf("role" to "user", "content" to results)',
                                       '        for (r in results) messages += mapOf("role" to "user", "content" to listOf(r))')],
    }),
}
