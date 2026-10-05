# Planted wrong solutions of module 29-context-engineering: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/29-context-engineering/unit-01/practice-1"] = {
    "python": ("context.py", {
        "wrong-clear-all": [("for block in results[:max(len(results) - keep, 0)]:", "for block in results:")],
        "wrong-clear-excluded": [('if block["type"] == "tool_result" and names.get(block["tool_use_id"]) not in exclude]', 'if block["type"] == "tool_result"]')],
        "wrong-window-splits-pair": [("    turns = split_turns(messages)\n    pinned, rest", "    turns = [[m] for m in messages]\n    pinned, rest")],
        "wrong-summarise-under-budget": [("count_tokens(messages) <= budget or len(turns) <= keep_turns", "len(turns) <= keep_turns")],
        "wrong-skips-old-summary": [("summary = summarise(older)", 'summary = summarise([m for m in older if not (isinstance(m["content"], list) and m["content"][0].get("text", "").startswith(SUMMARY_OPEN))])')],
        "wrong-cited-end-inclusive": [('text[start:end] != cite["cited_text"]', 'text[start:end + 1] != cite["cited_text"]')],
        "wrong-footnote-duplicates": [("    if key not in numbers:", "    if True:")],
    }),
    "typescript": ("context.ts", {
        "wrong-clear-all": [("for (const b of results.slice(0, Math.max(results.length - keep, 0))) b.content = placeholder;", "for (const b of results) b.content = placeholder;")],
        "wrong-clear-excluded": [('if (b.type === "tool_result" && !exclude.includes(names.get(b.tool_use_id) ?? "")) results.push(b);', 'if (b.type === "tool_result") results.push(b);')],
        "wrong-window-splits-pair": [("  const turns = splitTurns(messages);\n  const pinned = pin", "  const turns = messages.map((m) => [m]);\n  const pinned = pin")],
        "wrong-summarise-under-budget": [("countTokens(messages) <= budget || turns.length <= keepTurns", "turns.length <= keepTurns")],
        "wrong-skips-old-summary": [("const summary = summarise(older);", "const summary = summarise(older.filter((m) => !(Array.isArray(m.content) && String(m.content[0]?.text ?? \"\").startsWith(SUMMARY_OPEN))));")],
        "wrong-cited-end-inclusive": [("text.slice(start, end) !== cite.cited_text", "text.slice(start, end + 1) !== cite.cited_text")],
        "wrong-footnote-duplicates": [("if (!numbers.has(key)) {", "if (true) {")],
    }),
    "java": ("Context.java", {
        "wrong-clear-all": [('for (int i = 0; i < Math.max(results.size() - keep, 0); i++) results.get(i).put("content", placeholder);', 'for (int i = 0; i < results.size(); i++) results.get(i).put("content", placeholder);')],
        "wrong-clear-excluded": [('if ("tool_result".equals(b.get("type")) && !exclude.contains(names.get((String) b.get("tool_use_id")))) results.add(b);', 'if ("tool_result".equals(b.get("type"))) results.add(b);')],
        "wrong-window-splits-pair": [("        List<List<Map<String, Object>>> turns = splitTurns(messages);\n        List<List<Map<String, Object>>> pinned", "        List<List<Map<String, Object>>> turns = new ArrayList<>();\n        for (Map<String, Object> m : messages) turns.add(new ArrayList<>(List.of(m)));\n        List<List<Map<String, Object>>> pinned")],
        "wrong-summarise-under-budget": [("countTokens(messages) <= budget || turns.size() <= keepTurns", "turns.size() <= keepTurns")],
        "wrong-skips-old-summary": [("String summary = summarise.apply(older);", "String summary = summarise.apply(older.stream().filter(m -> !(m.get(\"content\") instanceof List<?> l && !l.isEmpty() && ((Map<?, ?>) l.get(0)).get(\"text\") instanceof String s && s.startsWith(SUMMARY_OPEN))).toList());")],
        "wrong-cited-end-inclusive": [("!text.substring(start, end).equals(cite.get(\"cited_text\"))", "!text.substring(start, Math.min(end + 1, text.length())).equals(cite.get(\"cited_text\"))")],
        "wrong-footnote-duplicates": [("if (!numbers.containsKey(key)) {", "if (true) {")],
    }),
    "kotlin": ("Context.kt", {
        "wrong-clear-all": [('for (b in results.take(maxOf(results.size - keep, 0))) b["content"] = placeholder', 'for (b in results) b["content"] = placeholder')],
        "wrong-clear-excluded": [('blocks.filter { it["type"] == "tool_result" && names[it["tool_use_id"] as String] !in exclude }', 'blocks.filter { it["type"] == "tool_result" }')],
        "wrong-window-splits-pair": [("    val turns = splitTurns(messages)\n    val pinned = if (pin)", "    val turns = messages.map { listOf(it) }\n    val pinned = if (pin)")],
        "wrong-summarise-under-budget": [("countTokens(messages) <= budget || turns.size <= keepTurns", "turns.size <= keepTurns")],
        "wrong-skips-old-summary": [("val summary = summarise(older)", "val summary = summarise(older.filter { m -> ((m[\"content\"] as? List<*>)?.firstOrNull() as? Map<*, *>)?.get(\"text\")?.toString()?.startsWith(SUMMARY_OPEN) != true })")],
        "wrong-cited-end-inclusive": [('text.substring(start, end) != cite["cited_text"]', 'text.substring(start, minOf(end + 1, text.length)) != cite["cited_text"]')],
        "wrong-footnote-duplicates": [("if (key !in numbers) {", "if (true) {")],
    }),
}
