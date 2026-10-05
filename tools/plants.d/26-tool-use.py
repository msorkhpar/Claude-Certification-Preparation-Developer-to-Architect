# Planted wrong solutions of module 26-tool-use: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/26-tool-use/unit-01/practice-1"] = {
    "python": ("toolloop.py", {
        "wrong-server-block-answered": [('if b["type"] == "tool_use"]', 'if b["type"] in ("tool_use", "server_tool_use")]')],
        "wrong-swallow-errors": [('return {**result, "content": str(err), "is_error": True}', 'return {**result, "content": str(err)}')],
        "wrong-extra-turn": [("return range(1, max_turns + 1)", "return range(1, max_turns + 2)")],
        "wrong-ignore-stop-reason": [('    if stop == "refusal":\n        return "refused"\n', "")],
        "wrong-forced-every-turn": [('    return {"type": "auto"} if forced and turn > 1 else tool_choice', "    return tool_choice")],
        "wrong-forced-on-new-models": [('    if kind in ("any", "tool") and model in FORCED_UNSUPPORTED:\n        raise RequestError("tool_choice", f"{model} does not support forced tool use")\n', "")],
        "wrong-repr-result": [("out if isinstance(out, str) else json.dumps(out)", "out if isinstance(out, str) else str(out)")],
    }),
    "typescript": ("toolloop.ts", {
        "wrong-server-block-answered": [('filter((b) => b.type === "tool_use").map((b) => runOne(tools, b))', 'filter((b) => b.type === "tool_use" || b.type === "server_tool_use").map((b) => runOne(tools, b))')],
        "wrong-swallow-errors": [("return { ...result, content: (err as Error).message, is_error: true };", "return { ...result, content: (err as Error).message };")],
        "wrong-extra-turn": [("  return maxTurns;", "  return maxTurns + 1;")],
        "wrong-ignore-stop-reason": [('  if (stop === "refusal") return "refused";\n', "")],
        "wrong-forced-every-turn": [('return forced && turn > 1 ? { type: "auto" } : toolChoice;', "return toolChoice;")],
        "wrong-forced-on-new-models": [('  if ((choice.type === "any" || choice.type === "tool") && FORCED_UNSUPPORTED.has(model)) throw new RequestError("tool_choice", `${model} does not support forced tool use`);\n', "")],
        "wrong-repr-result": [('typeof out === "string" ? out : JSON.stringify(out)', "String(out)")],
    }),
    "java": ("ToolLoop.java", {
        "wrong-server-block-answered": [('if ("tool_use".equals(b.get("type"))) results.add(runOne(tools, b));', 'if ("tool_use".equals(b.get("type")) || "server_tool_use".equals(b.get("type"))) results.add(runOne(tools, b));')],
        "wrong-swallow-errors": [('            result.put("content", e.getMessage());\n            result.put("is_error", true);\n        }\n        return result;', '            result.put("content", e.getMessage());\n        }\n        return result;')],
        "wrong-extra-turn": [("        return maxTurns;", "        return maxTurns + 1;")],
        "wrong-ignore-stop-reason": [('        if (stop.equals("refusal")) return "refused";\n', "")],
        "wrong-forced-every-turn": [('return forced && turn > 1 ? map("type", "auto") : toolChoice;', "return toolChoice;")],
        "wrong-forced-on-new-models": [('        if (("any".equals(kind) || "tool".equals(kind)) && FORCED_UNSUPPORTED.contains(model)) throw new RequestError("tool_choice", model + " does not support forced tool use");\n', "")],
        "wrong-repr-result": [("out instanceof String s ? s : Json.stringify(out)", "String.valueOf(out)")],
    }),
    "kotlin": ("ToolLoop.kt", {
        "wrong-server-block-answered": [('content.filter { it["type"] == "tool_use" }.map { runOne(tools, it) }', 'content.filter { it["type"] == "tool_use" || it["type"] == "server_tool_use" }.map { runOne(tools, it) }')],
        "wrong-swallow-errors": [('        result["content"] = e.message\n        result["is_error"] = true\n    }\n    return result', '        result["content"] = e.message\n    }\n    return result')],
        "wrong-extra-turn": [("private fun lastTurn(maxTurns: Int): Int = maxTurns", "private fun lastTurn(maxTurns: Int): Int = maxTurns + 1")],
        "wrong-ignore-stop-reason": [('    "refusal" -> "refused"\n', "")],
        "wrong-forced-every-turn": [('return if (forced && turn > 1) mapOf("type" to "auto") else toolChoice', "return toolChoice")],
        "wrong-forced-on-new-models": [('    if ((kind == "any" || kind == "tool") && model in FORCED_UNSUPPORTED) throw RequestError("tool_choice", "$model does not support forced tool use")\n', "")],
        "wrong-repr-result": [("if (out is String) out else Json.stringify(out)", "if (out is String) out else out.toString()")],
    }),
}
