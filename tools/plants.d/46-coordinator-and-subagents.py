# Planted wrong solutions of module 46-coordinator-and-subagents: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/46-coordinator-and-subagents/unit-01/practice-1"] = {
    "python": ("coordinator.py", {
        "wrong-leaks-context": [('        run(task["scope"], task["brief"])\n', '        run(task["scope"], task["brief"] + "".join("\\n" + f["text"] for f in findings))\n')],
        "wrong-no-dedupe": [("        elif key in seen:", "        elif False:")],
        "wrong-empty-brief-sent": [("        if not brief.strip():", "        if False:")],
        "wrong-always-delegates": [('    if not plan.get("delegate"):', "    if False:")],
        "wrong-failure-as-finding": [('            failed.append({"scope": scope, "error": str(error)})\n            return', '            findings.append({"scope": scope, "text": str(error)})\n            return')],
        "wrong-synthesizes-nothing": [("    if not findings:\n", "    if False:\n")],
        "wrong-rerun-all": [("        rounds += 1\n        for gap in gaps:", '        rounds += 1\n        for task in tasks:\n            run(task["scope"], task["brief"])\n        for gap in gaps:')],
        "wrong-rounds-off-by-one": [("while gaps and rounds < max_rounds:", "while gaps and rounds <= max_rounds:")],
    }),
    "typescript": ("coordinator.ts", {
        "wrong-leaks-context": [("for (const task of kept) run(task.scope, task.brief);", 'for (const task of kept) run(task.scope, task.brief + findings.map((f) => "\\n" + f.text).join(""));')],
        "wrong-no-dedupe": [("else if (seen.has(key)) dropped.push", "else if (false) dropped.push")],
        "wrong-empty-brief-sent": [('if (brief.trim() === "") dropped.push', "if (false) dropped.push")],
        "wrong-always-delegates": [("if (!plan.delegate) {", "if (false) {")],
        "wrong-failure-as-finding": [("failed.push({ scope, error: error instanceof Error ? error.message : String(error) });\n      return;", "findings.push({ scope, text: error instanceof Error ? error.message : String(error) });\n      return;")],
        "wrong-synthesizes-nothing": [("if (findings.length === 0) return {", "if (false) return {")],
        "wrong-rerun-all": [("    rounds += 1;\n    for (const gap of gaps) run(", "    rounds += 1;\n    for (const task of kept) run(task.scope, task.brief);\n    for (const gap of gaps) run(")],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
    "java": ("Coordinator.java", {
        "wrong-leaks-context": [('run.accept((String) task.get("scope"), (String) task.get("brief"));', 'run.accept((String) task.get("scope"), (String) task.get("brief") + findings.stream().map(f -> "\\n" + f.get("text")).collect(java.util.stream.Collectors.joining()));')],
        "wrong-no-dedupe": [("else if (seen.contains(key)) dropped", "else if (false) dropped")],
        "wrong-empty-brief-sent": [("if (brief.isBlank()) dropped.add", "if (false) dropped.add")],
        "wrong-always-delegates": [('if (!Boolean.TRUE.equals(plan.get("delegate"))) {', "if (false) {")],
        "wrong-failure-as-finding": [('failed.add(map("scope", scope, "error", String.valueOf(error.getMessage())));\n                return;', 'findings.add(map("scope", scope, "text", String.valueOf(error.getMessage())));\n                return;')],
        "wrong-synthesizes-nothing": [("if (findings.isEmpty()) {", "if (false) {")],
        "wrong-rerun-all": [("            rounds++;\n            for (String gap : gaps) run.accept(", '            rounds++;\n            for (Map<String, Object> task : tasks) run.accept((String) task.get("scope"), (String) task.get("brief"));\n            for (String gap : gaps) run.accept(')],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
    "kotlin": ("Coordinator.kt", {
        "wrong-leaks-context": [("for ((scope, brief) in tasks) run(scope, brief)", 'for ((scope, brief) in tasks) run(scope, brief + findings.joinToString("") { "\\n" + it["text"] })')],
        "wrong-no-dedupe": [("key in seen -> dropped.add", "false -> dropped.add")],
        "wrong-empty-brief-sent": [("brief.isBlank() -> dropped.add", "false -> dropped.add")],
        "wrong-always-delegates": [('if (plan["delegate"] != true) {', "if (false) {")],
        "wrong-failure-as-finding": [('failed.add(linkedMapOf("scope" to scope, "error" to error.message.toString()))\n            return', 'findings.add(linkedMapOf("scope" to scope, "text" to error.message.toString()))\n            return')],
        "wrong-synthesizes-nothing": [("if (findings.isEmpty()) {", "if (false) {")],
        "wrong-rerun-all": [("        rounds++\n        for (gap in gaps) run(", "        rounds++\n        for ((scope, brief) in tasks) run(scope, brief)\n        for (gap in gaps) run(")],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
}
