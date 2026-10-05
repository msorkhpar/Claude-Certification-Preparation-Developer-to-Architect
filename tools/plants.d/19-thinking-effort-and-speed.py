# Planted wrong solutions of module 19-thinking-effort-and-speed: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/19-thinking-effort-and-speed/unit-01/practice-1"] = {
    "python": ("params.py", {
        "wrong-allow-disabled": [('    if kind == "disabled" and not haiku:\n        raise RejectedRequest("thinking", "thinking cannot be turned off on this model")\n', "")],
        "wrong-between-any-effort": [('    if (effort or DEFAULT_EFFORT[family]) in ("xhigh", "max"):\n        raise RejectedRequest("thinking", "between_tools works at low, medium and high effort only")\n', "")],
        "wrong-effort-on-haiku": [('    if haiku:\n        raise RejectedRequest("output_config.effort", "this model does not support effort")\n', "")],
        "wrong-sampling-pass-through": [('return haiku or (name == "temperature" and value == 1.0)', "return True")],
        "wrong-fast-in-batch": [('    if batch:\n        raise RejectedRequest("speed", "fast mode is not available in a batch")\n', "")],
        "wrong-budget-floor": [("budget < 1024 or", "budget < 1000 or")],
        "wrong-effort-in-thinking": [('params["thinking"] = _thinking_object(kind, thinking.get("budget_tokens"))',
                                      'params["thinking"] = {**_thinking_object(kind, thinking.get("budget_tokens")), **({"effort": effort} if effort else {})}')],
        "wrong-default-effort-omitted": [('        params["output_config"] = {"effort": effort}', '        if effort != DEFAULT_EFFORT.get(family):\n            params["output_config"] = {"effort": effort}')],
    }),
    "typescript": ("params.ts", {
        "wrong-allow-disabled": [('  if (kind === "disabled" && !haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");\n', "")],
        "wrong-between-any-effort": [('  const level = effort ?? DEFAULT_EFFORT[fam];\n  if (level === "xhigh" || level === "max") throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");\n', "")],
        "wrong-effort-on-haiku": [('  if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");\n', "")],
        "wrong-sampling-pass-through": [('return haiku || (name === "temperature" && value === 1.0);', "return true;")],
        "wrong-fast-in-batch": [('  if (batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");\n', "")],
        "wrong-budget-floor": [("budget < 1024 ||", "budget < 1000 ||")],
        "wrong-effort-in-thinking": [("params.thinking = thinkingObject(kind, thinking.budget_tokens);", "params.thinking = { ...thinkingObject(kind, thinking.budget_tokens), ...(effort ? { effort } : {}) };")],
        "wrong-default-effort-omitted": [("    params.output_config = { effort };", "    if (effort !== DEFAULT_EFFORT[fam]) params.output_config = { effort };")],
    }),
    "java": ("Params.java", {
        "wrong-allow-disabled": [('        if (kind.equals("disabled") && !haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");\n', "")],
        "wrong-between-any-effort": [('        String level = effort != null ? effort : DEFAULT_EFFORT.get(family);\n        if (level.equals("xhigh") || level.equals("max")) {\n            throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");\n        }\n', "")],
        "wrong-effort-on-haiku": [('        if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");\n', "")],
        "wrong-sampling-pass-through": [('return haiku || (name.equals("temperature") && ((Number) value).doubleValue() == 1.0);', "return true;")],
        "wrong-fast-in-batch": [('        if (batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");\n', "")],
        "wrong-budget-floor": [("budget.longValue() < 1024", "budget.longValue() < 1000")],
        "wrong-effort-in-thinking": [('params.put("thinking", thinkingObject(kind, budget));',
                                      'Map<String, Object> t = thinkingObject(kind, budget);\n            if (effort != null) t.put("effort", effort);\n            params.put("thinking", t);')],
        "wrong-default-effort-omitted": [('            params.put("output_config", Map.of("effort", effort));', '            if (!effort.equals(DEFAULT_EFFORT.get(family))) params.put("output_config", Map.of("effort", effort));')],
    }),
    "kotlin": ("Params.kt", {
        "wrong-allow-disabled": [('    if (kind == "disabled" && !haiku) throw RejectedRequest("thinking", "thinking cannot be turned off on this model")\n', "")],
        "wrong-between-any-effort": [('    val level = effort ?: DEFAULT_EFFORT.getValue(family)\n    if (level == "xhigh" || level == "max") throw RejectedRequest("thinking", "between_tools works at low, medium and high effort only")\n', "")],
        "wrong-effort-on-haiku": [('    if (haiku) throw RejectedRequest("output_config.effort", "this model does not support effort")\n', "")],
        "wrong-sampling-pass-through": [('haiku || (name == "temperature" && (value as Number).toDouble() == 1.0)', "true")],
        "wrong-fast-in-batch": [('    if (batch) throw RejectedRequest("speed", "fast mode is not available in a batch")\n', "")],
        "wrong-budget-floor": [("budget < 1024 ||", "budget < 1000 ||")],
        "wrong-effort-in-thinking": [('params["thinking"] = thinkingObject(kind!!, thinking["budget_tokens"])',
                                      'params["thinking"] = thinkingObject(kind!!, thinking["budget_tokens"]) + (if (effort != null) mapOf("effort" to effort) else emptyMap<String, Any?>())')],
        "wrong-default-effort-omitted": [('        params["output_config"] = mapOf("effort" to effort)', '        if (effort != DEFAULT_EFFORT[family]) params["output_config"] = mapOf("effort" to effort)')],
    }),
}
