# Planted wrong solutions of module 26-tool-use: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/26-tool-use/unit-01/practice-1"] = {
    "python": ("toolloop.py", {
        "wrong-result-per-message": [('            results = [_run_one(tools, b) for b in reply["content"] if b["type"] == "tool_use"]\n            messages.append({"role": "user", "content": results})\n', '            for b in reply["content"]:\n                if b["type"] == "tool_use":\n                    messages.append({"role": "user", "content": [_run_one(tools, b)]})\n')],
        "wrong-swallow-errors": [('return {**result, "content": str(err), "is_error": True}', 'return {**result, "content": str(err)}')],
        "wrong-extra-turn": [("for turn in range(1, max_turns + 1):", "for turn in range(1, max_turns + 2):")],
        "wrong-ignore-stop-reason": [('        elif stop == "refusal":\n            return {"status": "refused", "text": text, "turns": calls, "messages": messages}\n', "")],
        "wrong-forced-every-turn": [('request["tool_choice"] = {"type": "auto"} if forced and turn > 1 else tool_choice', 'request["tool_choice"] = tool_choice')],
        "wrong-forced-on-new-models": [('    if kind in ("any", "tool") and model in FORCED_UNSUPPORTED:\n        raise RequestError("tool_choice", f"{model} does not support forced tool use")\n', "")],
        "wrong-repr-result": [("out if isinstance(out, str) else json.dumps(out)", "out if isinstance(out, str) else str(out)")],
    }),
    "typescript": ("toolloop.ts", {
        "wrong-result-per-message": [('      messages.push({ role: "user", content: reply.content.filter((b) => b.type === "tool_use").map((b) => runOne(tools, b)) });\n', '      for (const b of reply.content.filter((b) => b.type === "tool_use")) messages.push({ role: "user", content: [runOne(tools, b)] });\n')],
        "wrong-swallow-errors": [("return { ...result, content: (err as Error).message, is_error: true };", "return { ...result, content: (err as Error).message };")],
        "wrong-extra-turn": [("turn <= maxTurns; turn++", "turn <= maxTurns + 1; turn++")],
        "wrong-ignore-stop-reason": [('    } else if (stop === "refusal") {\n      return { status: "refused", text, turns: calls, messages };\n', "")],
        "wrong-forced-every-turn": [('request.tool_choice = forced && turn > 1 ? { type: "auto" } : toolChoice;', "request.tool_choice = toolChoice;")],
        "wrong-forced-on-new-models": [('  if ((choice.type === "any" || choice.type === "tool") && FORCED_UNSUPPORTED.has(model)) throw new RequestError("tool_choice", `${model} does not support forced tool use`);\n', "")],
        "wrong-repr-result": [('typeof out === "string" ? out : JSON.stringify(out)', "String(out)")],
    }),
    "java": ("ToolLoop.java", {
        "wrong-result-per-message": [('                for (Map<String, Object> b : content) if ("tool_use".equals(b.get("type"))) results.add(runOne(tools, b));\n                messages.add(map("role", "user", "content", results));\n', '                for (Map<String, Object> b : content) if ("tool_use".equals(b.get("type"))) messages.add(map("role", "user", "content", List.of(runOne(tools, b))));\n')],
        "wrong-swallow-errors": [('            result.put("content", e.getMessage());\n            result.put("is_error", true);\n        }\n        return result;', '            result.put("content", e.getMessage());\n        }\n        return result;')],
        "wrong-extra-turn": [("turn <= maxTurns; turn++", "turn <= maxTurns + 1; turn++")],
        "wrong-ignore-stop-reason": [('            } else if (stop.equals("refusal")) {\n                return outcome("refused", text, calls, messages);\n', "")],
        "wrong-forced-every-turn": [('request.put("tool_choice", forced && turn > 1 ? map("type", "auto") : toolChoice);', 'request.put("tool_choice", toolChoice);')],
        "wrong-forced-on-new-models": [('        if (("any".equals(kind) || "tool".equals(kind)) && FORCED_UNSUPPORTED.contains(model)) throw new RequestError("tool_choice", model + " does not support forced tool use");\n', "")],
        "wrong-repr-result": [("out instanceof String s ? s : Json.stringify(out)", "String.valueOf(out)")],
    }),
    "kotlin": ("ToolLoop.kt", {
        "wrong-result-per-message": [('"tool_use" -> messages += mapOf("role" to "user", "content" to content.filter { it["type"] == "tool_use" }.map { runOne(tools, it) })', '"tool_use" -> content.filter { it["type"] == "tool_use" }.forEach { messages += mapOf("role" to "user", "content" to listOf(runOne(tools, it))) }')],
        "wrong-swallow-errors": [('        result["content"] = e.message\n        result["is_error"] = true\n    }\n    return result', '        result["content"] = e.message\n    }\n    return result')],
        "wrong-extra-turn": [("for (turn in 1..maxTurns) {", "for (turn in 1..maxTurns + 1) {")],
        "wrong-ignore-stop-reason": [('            "refusal" -> return outcome("refused", text, calls, messages)\n', "")],
        "wrong-forced-every-turn": [('request["tool_choice"] = if (forced && turn > 1) mapOf("type" to "auto") else toolChoice', 'request["tool_choice"] = toolChoice')],
        "wrong-forced-on-new-models": [('    if ((kind == "any" || kind == "tool") && model in FORCED_UNSUPPORTED) throw RequestError("tool_choice", "$model does not support forced tool use")\n', "")],
        "wrong-repr-result": [("if (out is String) out else Json.stringify(out)", "if (out is String) out else out.toString()")],
    }),
}
