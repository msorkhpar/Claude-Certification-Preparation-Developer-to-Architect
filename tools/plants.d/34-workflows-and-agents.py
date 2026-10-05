# Planted wrong solutions of module 34-workflows-and-agents: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/34-workflows-and-agents/unit-01/practice-1"] = {
    "python": ("workflows.py", {
        "wrong-no-cap": [("return steps[:max_subtasks], False", "return steps, False")],
        "wrong-stop-at-first-failure": [("        results.append(_run_worker(ask, subtask, task))\n", '        results.append(_run_worker(ask, subtask, task))\n        if results[-1]["status"] == "failed":\n            break\n')],
        "wrong-no-feedback": [(r"\nFeedback: {feedback}", "")],
        "wrong-last-draft": [('"status": "max_rounds", "draft": best,', '"status": "max_rounds", "draft": draft,')],
        "wrong-trust-unreadable-judge": [('return 0, "The judge reply could not be read."', 'return 10, "The judge reply could not be read."')],
        "wrong-punctuation-kept": [("reply.strip().strip(\".,;:!\\\"'`\").strip().lower()", "reply.strip().lower()")],
        "wrong-tie-goes-last": [("return next(a for a, c in counts.items() if c == top)", "return [a for a, c in counts.items() if c == top][-1]")],
        "wrong-error-rounds": [("round_number - 1, history, err)", "round_number, history, err)")],
    }),
    "typescript": ("workflows.ts", {
        "wrong-no-cap": [("return [steps.slice(0, maxSubtasks), false];", "return [steps, false];")],
        "wrong-stop-at-first-failure": [("    results.push(runWorker(ask, subtask, task));\n", '    results.push(runWorker(ask, subtask, task));\n    if (results[results.length - 1].status === "failed") break;\n')],
        "wrong-no-feedback": [(r"\nFeedback: ${feedback}", "")],
        "wrong-last-draft": [('{ status: "max_rounds", draft: best,', '{ status: "max_rounds", draft,')],
        "wrong-trust-unreadable-judge": [('return [0, "The judge reply could not be read."];', 'return [10, "The judge reply could not be read."];')],
        "wrong-punctuation-kept": [('reply.trim().replace(/^[.,;:!"\'`]+|[.,;:!"\'`]+$/g, "").trim().toLowerCase()', "reply.trim().toLowerCase()")],
        "wrong-tie-goes-last": [("return [...counts].find(([, c]) => c === top)![0];", "return [...counts].filter(([, c]) => c === top).pop()![0];")],
        "wrong-error-rounds": [("round - 1, history, err)", "round, history, err)")],
    }),
    "java": ("Workflows.java", {
        "wrong-no-cap": [("steps.subList(0, Math.min(steps.size(), maxSubtasks))", "steps")],
        "wrong-stop-at-first-failure": [("            results.add(runWorker(ask, subtask, task));\n", '            results.add(runWorker(ask, subtask, task));\n            if (results.get(results.size() - 1).get("status").equals("failed")) break;\n')],
        "wrong-no-feedback": [(r' + "\nFeedback: " + feedback', "")],
        "wrong-last-draft": [('return map("status", "max_rounds", "draft", best,', 'return map("status", "max_rounds", "draft", draft,')],
        "wrong-trust-unreadable-judge": [("return new Object[] {0, UNREADABLE};", "return new Object[] {10, UNREADABLE};")],
        "wrong-punctuation-kept": [('reply.strip().replaceAll("^[.,;:!\\"\'`]+|[.,;:!\\"\'`]+$", "").strip().toLowerCase(Locale.ROOT)', "reply.strip().toLowerCase(Locale.ROOT)")],
        "wrong-tie-goes-last": [(".findFirst().get().getKey()", ".reduce((a, b) -> b).get().getKey()")],
        "wrong-error-rounds": [("round - 1, history, e)", "round, history, e)")],
    }),
    "kotlin": ("Workflows.kt", {
        "wrong-no-cap": [("else steps.take(maxSubtasks)", "else steps")],
        "wrong-stop-at-first-failure": [("        results.add(runWorker(ask, subtask, task))\n", '        results.add(runWorker(ask, subtask, task))\n        if (results.last()["status"] == "failed") break\n')],
        "wrong-no-feedback": [(r"\nFeedback: $feedback", "")],
        "wrong-last-draft": [('"status" to "max_rounds", "draft" to best,', '"status" to "max_rounds", "draft" to draft,')],
        "wrong-trust-unreadable-judge": [("return Pair(0, UNREADABLE)", "return Pair(10, UNREADABLE)")],
        "wrong-punctuation-kept": [('reply.trim().replace(Regex("^[.,;:!\\"\'`]+|[.,;:!\\"\'`]+$"), "").trim().lowercase(Locale.ROOT)', "reply.trim().lowercase(Locale.ROOT)")],
        "wrong-tie-goes-last": [("counts.entries.first { it.value == top }.key", "counts.entries.last { it.value == top }.key")],
        "wrong-error-rounds": [("round - 1, history, e)", "round, history, e)")],
    }),
}
