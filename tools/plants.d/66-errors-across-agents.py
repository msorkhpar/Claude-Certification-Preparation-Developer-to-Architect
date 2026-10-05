# Planted wrong solutions of module 66-errors-across-agents: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/66-errors-across-agents/unit-01/practice-1"] = {
    "python": ("error_flow.py", {
        "wrong-empty-is-error": [('"success" if items else "empty"', '"success" if items else "failed"')],
        "wrong-retry-permission": [("        if kind in TRANSIENT and attempts < max_attempts:", "        if attempts < max_attempts:")],
        "wrong-drop-partial": [('"partial_results": reply.get("partial", [])', '"partial_results": []')],
        "wrong-generic-error": [('"failure_type": kind, "attempted": query, ', "")],
        "wrong-stop-on-failure": [("    plan = []\n    for topic, outcome in results.items():", '    plan = []\n    if any(o["status"] == "failed" for o in results.values()):\n        return [(topic, "abort") for topic in results]\n    for topic, outcome in results.items():')],
        "wrong-gap-as-supported": [('        elif outcome["status"] == "success":\n            groups["Well-supported"].append(topic)', '        elif outcome["status"] in ("success", "failed"):\n            groups["Well-supported"].append(topic)')],
        "wrong-missing-topic-skipped": [('        if outcome is None:\n            groups["Gaps"].append(f"{topic} (not searched)")', "        if outcome is None:\n            continue")],
    }),
    "typescript": ("errorFlow.ts", {
        "wrong-empty-is-error": [('status: items.length > 0 ? "success" : "empty"', 'status: items.length > 0 ? "success" : "failed"')],
        "wrong-retry-permission": [("if (TRANSIENT.includes(kind) && attempts < maxAttempts) continue;", "if (attempts < maxAttempts) continue;")],
        "wrong-drop-partial": [("partial_results: reply.partial ?? [], ", "partial_results: [], ")],
        "wrong-generic-error": [("failure_type: kind, attempted: query, ", "")],
        "wrong-stop-on-failure": [("  for (const [topic, outcome] of Object.entries(results)) {\n    let action", '  if (Object.values(results).some((o: any) => o.status === "failed")) return Object.keys(results).map((t): [string, string] => [t, "abort"]);\n  for (const [topic, outcome] of Object.entries(results)) {\n    let action')],
        "wrong-gap-as-supported": [('else if (outcome.status === "success") groups["Well-supported"].push(topic);', 'else if (outcome.status === "success" || outcome.status === "failed") groups["Well-supported"].push(topic);')],
        "wrong-missing-topic-skipped": [('if (outcome === undefined) groups["Gaps"].push(`${topic} (not searched)`);', "if (outcome === undefined) continue;")],
    }),
    "java": ("ErrorFlow.java", {
        "wrong-empty-is-error": [('reply.items().isEmpty() ? "empty" : "success"', 'reply.items().isEmpty() ? "failed" : "success"')],
        "wrong-retry-permission": [("if (TRANSIENT.contains(kind) && attempts < maxAttempts) continue;", "if (attempts < maxAttempts) continue;")],
        "wrong-drop-partial": [("reply.partial() == null ? List.of() : reply.partial()", "List.of()")],
        "wrong-generic-error": [('new Outcome("failed", List.of(), attempts, kind, query, ', 'new Outcome("failed", List.of(), attempts, null, null, ')],
        "wrong-stop-on-failure": [("        for (Map.Entry<String, Outcome> e : results.entrySet()) {\n            Outcome o = e.getValue();\n", '        for (Map.Entry<String, Outcome> e : results.entrySet()) {\n            if (e.getValue().status().equals("failed")) {\n                List<Step> aborted = new ArrayList<>();\n                for (String t : results.keySet()) aborted.add(new Step(t, "abort"));\n                return aborted;\n            }\n        }\n        for (Map.Entry<String, Outcome> e : results.entrySet()) {\n            Outcome o = e.getValue();\n')],
        "wrong-gap-as-supported": [('else if (o.status().equals("success")) groups.get("Well-supported").add(topic);', 'else if (o.status().equals("success") || o.status().equals("failed")) groups.get("Well-supported").add(topic);')],
        "wrong-missing-topic-skipped": [('if (o == null) groups.get("Gaps").add(topic + " (not searched)");', "if (o == null) continue;")],
    }),
    "kotlin": ("ErrorFlow.kt", {
        "wrong-empty-is-error": [('if (reply.items.isEmpty()) "empty" else "success"', 'if (reply.items.isEmpty()) "failed" else "success"')],
        "wrong-retry-permission": [("if (kind in TRANSIENT && attempts < maxAttempts) continue", "if (attempts < maxAttempts) continue")],
        "wrong-drop-partial": [("query, reply.partial, ALTERNATIVES[kind]", "query, emptyList(), ALTERNATIVES[kind]")],
        "wrong-generic-error": [('Outcome("failed", emptyList(), attempts, kind, query, reply.partial,', 'Outcome("failed", emptyList(), attempts, null, null, reply.partial,')],
        "wrong-stop-on-failure": [("results.map { (topic, o) ->\n", 'results.map { (topic, o) ->\n    if (results.values.any { it.status == "failed" }) return@map Step(topic, "abort")\n')],
        "wrong-gap-as-supported": [('o.status == "success" -> groups.getValue("Well-supported") += topic', 'o.status == "success" || o.status == "failed" -> groups.getValue("Well-supported") += topic')],
        "wrong-missing-topic-skipped": [('o == null -> groups.getValue("Gaps") += "$topic (not searched)"', "o == null -> Unit")],
    }),
}
