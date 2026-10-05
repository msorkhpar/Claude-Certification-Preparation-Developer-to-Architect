# Planted wrong solutions of module 46-coordinator-and-subagents: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/46-coordinator-and-subagents/unit-01/practice-1"] = {
    "python": ("coordinator.py", {
        "wrong-leaks-context": [('        run(task["scope"], _brief_of(task["brief"]))\n', '        run(task["scope"], task["brief"] + ("".join("\\n" + f["text"] for f in findings) if len(tasks) > 2 and max_agents > 3 else ""))\n')],
        "wrong-no-dedupe": [("    if key in seen:", "    if False:")],
        "wrong-empty-brief-sent": [("    if not brief.strip():", "    if False:")],
        "wrong-always-delegates": [('    return bool(plan.get("delegate"))', "    return True")],
        "wrong-failure-as-finding": [('            failed.append({"scope": scope, "error": str(error)})\n            return', '            findings.append({"scope": scope, "text": str(error)})\n            return')],
        "wrong-synthesizes-nothing": [("    if not findings:\n", "    if not findings and not failed:\n")],
        "wrong-gaps-not-deduped": [("        if gap and gap not in out:", "        if gap:")],
        "wrong-rounds-off-by-one": [("rounds < max_rounds", "rounds <= max_rounds")],
    }),
    "typescript": ("coordinator.ts", {
        "wrong-leaks-context": [("for (const task of kept) run(task.scope, briefOf(task.brief));", 'for (const task of kept) run(task.scope, task.brief + (kept.length > 2 && maxAgents > 3 ? findings.map((f) => "\\n" + f.text).join("") : ""));')],
        "wrong-no-dedupe": [("if (seen.has(key)) return", "if (false) return")],
        "wrong-empty-brief-sent": [('if (brief.trim() === "") return', "if (false) return")],
        "wrong-always-delegates": [("return Boolean(plan.delegate);", "return true;")],
        "wrong-failure-as-finding": [("failed.push({ scope, error: error instanceof Error ? error.message : String(error) });\n      return;", "findings.push({ scope, text: error instanceof Error ? error.message : String(error) });\n      return;")],
        "wrong-synthesizes-nothing": [("if (findings.length === 0) return {", "if (findings.length === 0 && failed.length === 0) return {")],
        "wrong-gaps-not-deduped": [('if (text !== "" && !out.includes(text)) out.push(text);', 'if (text !== "") out.push(text);')],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
    "java": ("Coordinator.java", {
        "wrong-leaks-context": [('run.accept((String) task.get("scope"), briefOf((String) task.get("brief")));', 'run.accept((String) task.get("scope"), (String) task.get("brief") + (tasks.size() > 2 && maxAgents > 3 ? findings.stream().map(f -> "\\n" + f.get("text")).collect(java.util.stream.Collectors.joining()) : ""));')],
        "wrong-no-dedupe": [("if (seen.contains(key)) return", "if (false) return")],
        "wrong-empty-brief-sent": [("if (brief.isBlank()) return", "if (false) return")],
        "wrong-always-delegates": [('return Boolean.TRUE.equals(plan.get("delegate"));', "return true;")],
        "wrong-failure-as-finding": [('failed.add(map("scope", scope, "error", String.valueOf(error.getMessage())));\n                return;', 'findings.add(map("scope", scope, "text", String.valueOf(error.getMessage())));\n                return;')],
        "wrong-synthesizes-nothing": [("if (findings.isEmpty()) {", "if (findings.isEmpty() && failed.isEmpty()) {")],
        "wrong-gaps-not-deduped": [("if (!text.isEmpty() && !out.contains(text)) out.add(text);", "if (!text.isEmpty()) out.add(text);")],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
    "kotlin": ("Coordinator.kt", {
        "wrong-leaks-context": [("for ((scope, brief) in tasks) run(scope, briefOf(brief))", 'for ((scope, brief) in tasks) run(scope, brief + (if (tasks.size > 2 && maxAgents > 3) findings.joinToString("") { "\\n" + it["text"] } else ""))')],
        "wrong-no-dedupe": [('key in seen -> "duplicate scope"', 'false -> "duplicate scope"')],
        "wrong-empty-brief-sent": [('brief.isBlank() -> "empty brief"', 'false -> "empty brief"')],
        "wrong-always-delegates": [('plan["delegate"] == true', "true")],
        "wrong-failure-as-finding": [('failed.add(linkedMapOf("scope" to scope, "error" to error.message.toString()))\n            return', 'findings.add(linkedMapOf("scope" to scope, "text" to error.message.toString()))\n            return')],
        "wrong-synthesizes-nothing": [("if (findings.isEmpty()) {", "if (findings.isEmpty() && failed.isEmpty()) {")],
        "wrong-gaps-not-deduped": [("if (text.isNotEmpty() && text !in out) out.add(text)", "if (text.isNotEmpty()) out.add(text)")],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
}
