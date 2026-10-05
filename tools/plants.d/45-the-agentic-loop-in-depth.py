# Planted wrong solutions of module 45-the-agentic-loop-in-depth: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/45-the-agentic-loop-in-depth/unit-01/practice-1"] = {
    "python": ("agent.py", {
        "wrong-text-marker": [('        reason = reply["stop_reason"]\n', '        reason = reply["stop_reason"]\n        if "done" in last_text.lower():\n            return {"status": "done", "text": last_text, "turns": turns, "messages": messages}\n')],
        "wrong-cap-reports-done": [('return {"status": "max_turns", "text"', 'return {"status": "done", "text"')],
        "wrong-cap-off-by-one": [("return turns >= max_turns", "return turns > max_turns")],
        "wrong-assistant-trimmed": [('messages.append({"role": "assistant", "content": content})', 'messages.append({"role": "assistant", "content": [b for b in content if b.get("type") != "text"]})')],
        "wrong-error-without-flag": [("_tool_result(block, str(error), True)", "_tool_result(block, str(error))")],
        "wrong-truncated-is-done": [('if reason in ("end_turn", "stop_sequence"):', 'if reason in ("end_turn", "stop_sequence", "max_tokens"):')],
        "wrong-malformed-continues": [('    if reason == "tool_use" and not calls:\n        return "malformed"\n', "")],
    }),
    "typescript": ("agent.ts", {
        "wrong-text-marker": [('    const reason = reply.stop_reason;\n', '    const reason = reply.stop_reason;\n    if (lastText.toLowerCase().includes("done")) return { status: "done", text: lastText, turns, messages };\n')],
        "wrong-cap-reports-done": [('{ status: "max_turns", text', '{ status: "done", text')],
        "wrong-cap-off-by-one": [("return turns >= maxTurns;", "return turns > maxTurns;")],
        "wrong-assistant-trimmed": [('messages.push({ role: "assistant", content });', 'messages.push({ role: "assistant", content: content.filter((b) => b.type !== "text") });')],
        "wrong-error-without-flag": [("String(error), true);", "String(error));")],
        "wrong-truncated-is-done": [('reason === "end_turn" || reason === "stop_sequence")', 'reason === "end_turn" || reason === "stop_sequence" || reason === "max_tokens")')],
        "wrong-malformed-continues": [('  if (reason === "tool_use" && calls.length === 0) return "malformed";\n', "")],
    }),
    "java": ("AgentLoop.java", {
        "wrong-text-marker": [('            String reason = (String) reply.get("stop_reason");\n', '            String reason = (String) reply.get("stop_reason");\n            if (lastText.toLowerCase().contains("done")) return outcome("done", lastText, turns, messages);\n')],
        "wrong-cap-reports-done": [('outcome("max_turns", lastText', 'outcome("done", lastText')],
        "wrong-cap-off-by-one": [("return turns >= maxTurns;", "return turns > maxTurns;")],
        "wrong-assistant-trimmed": [('messages.add(map("role", "assistant", "content", content));', 'messages.add(map("role", "assistant", "content", callsOf(content)));')],
        "wrong-error-without-flag": [("String.valueOf(error.getMessage()), true);", "String.valueOf(error.getMessage()), false);")],
        "wrong-truncated-is-done": [('case "max_tokens" -> "truncated";', 'case "max_tokens" -> "done";')],
        "wrong-malformed-continues": [('        if (reason.equals("tool_use") && calls.isEmpty()) return "malformed";\n', "")],
    }),
    "kotlin": ("AgentLoop.kt", {
        "wrong-text-marker": [('        val reason = reply["stop_reason"]\n', '        val reason = reply["stop_reason"]\n        if (lastText.lowercase().contains("done")) return outcome("done", lastText, turns, messages)\n')],
        "wrong-cap-reports-done": [('outcome("max_turns", lastText', 'outcome("done", lastText')],
        "wrong-cap-off-by-one": [("= turns >= maxTurns", "= turns > maxTurns")],
        "wrong-assistant-trimmed": [('messages.add(linkedMapOf("role" to "assistant", "content" to content))', 'messages.add(linkedMapOf("role" to "assistant", "content" to callsOf(content)))')],
        "wrong-error-without-flag": [("error.message.toString(), true)", "error.message.toString(), false)")],
        "wrong-truncated-is-done": [('reason == "end_turn" || reason == "stop_sequence" -> "done"', 'reason == "end_turn" || reason == "stop_sequence" || reason == "max_tokens" -> "done"')],
        "wrong-malformed-continues": [('    reason == "tool_use" && calls.isEmpty() -> "malformed"\n', "")],
    }),
}
