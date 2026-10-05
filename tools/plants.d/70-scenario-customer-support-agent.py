# Planted wrong solutions of module 70-scenario-customer-support-agent: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/70-scenario-customer-support-agent/unit-01/practice-1"] = {
    "python": ("audit.py", {
        "wrong-failed-identity-opens": [('if step["tool"] == "get_customer" and step["ok"]:', 'if step["tool"] == "get_customer":')],
        "wrong-over-limit-any-outcome": [('return session["outcome"] == "resolved" and session["refund_cents"] > session["limit_cents"]', 'return session["refund_cents"] > session["limit_cents"]')],
        "wrong-diagnosis-criteria-first": [('    if wrong and wrong >= over + under:\n        return "rewrite_tool_descriptions"\n    if over + under:\n        return "write_escalation_criteria"\n',
                                            '    if over + under:\n        return "write_escalation_criteria"\n    if wrong and wrong >= over + under:\n        return "rewrite_tool_descriptions"\n')],
        "wrong-target-strict": [('"meets_target": fcr >= TARGET', '"meets_target": fcr > TARGET')],
        "wrong-wrong-tool-steps": [('wrong = sum(1 for s in sessions if has_wrong_tool(s["steps"]))', 'wrong = sum(1 for s in sessions for st in s["steps"] if has_wrong_tool([st]))')],
        "wrong-empty-rate-one": [('return round(resolved / n, 3) if n else 0.0', 'return round(resolved / n, 3) if n else 1.0')],
        "wrong-over-counts-needs-human": [('over = sum(1 for s in sessions if s["outcome"] == "escalated" and not s["needs_human"])', 'over = sum(1 for s in sessions if s["outcome"] == "escalated")')],
    }),
    "typescript": ("audit.ts", {
        "wrong-failed-identity-opens": [('if (step.tool === "get_customer" && step.ok) verified = true;', 'if (step.tool === "get_customer") verified = true;')],
        "wrong-over-limit-any-outcome": [('return session.outcome === "resolved" && session.refund_cents > session.limit_cents;', "return session.refund_cents > session.limit_cents;")],
        "wrong-diagnosis-criteria-first": [('  if (wrong && wrong >= over + under) return "rewrite_tool_descriptions";\n  if (over + under) return "write_escalation_criteria";\n',
                                            '  if (over + under) return "write_escalation_criteria";\n  if (wrong && wrong >= over + under) return "rewrite_tool_descriptions";\n')],
        "wrong-target-strict": [("meets_target: fcr >= TARGET", "meets_target: fcr > TARGET")],
        "wrong-wrong-tool-steps": [("const wrong = count((s) => hasWrongTool(s.steps));",
                                    "const wrong = sessions.reduce((a, s) => a + s.steps.filter((st) => hasWrongTool([st])).length, 0);")],
        "wrong-empty-rate-one": [("return n ? Math.round((resolved / n) * 1000) / 1000 : 0;", "return n ? Math.round((resolved / n) * 1000) / 1000 : 1;")],
        "wrong-over-counts-needs-human": [('const over = count((s) => s.outcome === "escalated" && !s.needs_human);', 'const over = count((s) => s.outcome === "escalated");')],
    }),
    "java": ("Audit.java", {
        "wrong-failed-identity-opens": [('if (step.tool().equals("get_customer") && step.ok()) verified = true;', 'if (step.tool().equals("get_customer")) verified = true;')],
        "wrong-over-limit-any-outcome": [('return session.outcome().equals("resolved") && session.refundCents() > session.limitCents();', "return session.refundCents() > session.limitCents();")],
        "wrong-diagnosis-criteria-first": [('        if (wrong > 0 && wrong >= over + under) return "rewrite_tool_descriptions";\n        if (over + under > 0) return "write_escalation_criteria";\n',
                                            '        if (over + under > 0) return "write_escalation_criteria";\n        if (wrong > 0 && wrong >= over + under) return "rewrite_tool_descriptions";\n')],
        "wrong-target-strict": [("fcr >= TARGET", "fcr > TARGET")],
        "wrong-wrong-tool-steps": [("int wrong = (int) sessions.stream().filter(s -> hasWrongTool(s.steps())).count();",
                                    "int wrong = (int) sessions.stream().flatMap(s -> s.steps().stream()).filter(st -> hasWrongTool(List.of(st))).count();")],
        "wrong-empty-rate-one": [("return n > 0 ? Math.round((double) resolved / n * 1000) / 1000.0 : 0.0;", "return n > 0 ? Math.round((double) resolved / n * 1000) / 1000.0 : 1.0;")],
        "wrong-over-counts-needs-human": [('int over = (int) sessions.stream().filter(s -> s.outcome().equals("escalated") && !s.needsHuman()).count();', 'int over = (int) sessions.stream().filter(s -> s.outcome().equals("escalated")).count();')],
    }),
    "kotlin": ("Audit.kt", {
        "wrong-failed-identity-opens": [('if (step.tool == "get_customer" && step.ok) verified = true', 'if (step.tool == "get_customer") verified = true')],
        "wrong-over-limit-any-outcome": [('session.outcome == "resolved" && session.refundCents > session.limitCents', "session.refundCents > session.limitCents")],
        "wrong-diagnosis-criteria-first": [('    wrong > 0 && wrong >= over + under -> "rewrite_tool_descriptions"\n    over + under > 0 -> "write_escalation_criteria"\n',
                                            '    over + under > 0 -> "write_escalation_criteria"\n    wrong > 0 && wrong >= over + under -> "rewrite_tool_descriptions"\n')],
        "wrong-target-strict": [("fcr >= TARGET", "fcr > TARGET")],
        "wrong-wrong-tool-steps": [("val wrong = sessions.count { hasWrongTool(it.steps) }",
                                    "val wrong = sessions.sumOf { s -> s.steps.count { hasWrongTool(listOf(it)) } }")],
        "wrong-empty-rate-one": [("Math.round(resolved.toDouble() / n * 1000) / 1000.0 else 0.0", "Math.round(resolved.toDouble() / n * 1000) / 1000.0 else 1.0")],
        "wrong-over-counts-needs-human": [('val over = sessions.count { it.outcome == "escalated" && !it.needsHuman }', 'val over = sessions.count { it.outcome == "escalated" }')],
    }),
}
