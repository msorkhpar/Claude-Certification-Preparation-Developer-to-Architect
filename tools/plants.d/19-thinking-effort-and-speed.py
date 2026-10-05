# Planted wrong solutions of module 19-thinking-effort-and-speed: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/19-thinking-effort-and-speed/unit-01/practice-1"] = {
    "python": ("params.py", {
        "wrong-allow-disabled": [('            if not haiku:\n                raise RejectedRequest("thinking", "thinking cannot be turned off on this model")\n', "")],
        "wrong-between-any-effort": [('            if (effort or DEFAULT_EFFORT[family]) in ("xhigh", "max"):\n                raise RejectedRequest("thinking", "between_tools works at low, medium and high effort only")\n', "")],
        "wrong-effort-on-haiku": [('        if haiku:\n            raise RejectedRequest("output_config.effort", "this model does not support effort")\n', "")],
        "wrong-sampling-pass-through": [('        if not haiku and not (name == "temperature" and value == 1.0):\n            raise RejectedRequest(name, "this model rejects a non-default value")\n', "")],
        "wrong-fast-in-batch": [('        if options.get("batch"):\n            raise RejectedRequest("speed", "fast mode is not available in a batch")\n', "")],
        "wrong-budget-floor": [("budget < 1024 or", "budget < 1000 or")],
        "wrong-effort-in-thinking": [('params["thinking"] = {"type": "adaptive"}', 'params["thinking"] = {"type": "adaptive", **({"effort": effort} if effort else {})}')],
    }),
    "typescript": ("params.ts", {
        "wrong-allow-disabled": [('      if (!haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");\n', "")],
        "wrong-between-any-effort": [('      const level = effort ?? DEFAULT_EFFORT[fam];\n      if (level === "xhigh" || level === "max") throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");\n', "")],
        "wrong-effort-on-haiku": [('    if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");\n', "")],
        "wrong-sampling-pass-through": [('    if (!haiku && !(name === "temperature" && value === 1.0)) throw new RejectedRequest(name, "this model rejects a non-default value");\n', "")],
        "wrong-fast-in-batch": [('    if (options.batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");\n', "")],
        "wrong-budget-floor": [("budget < 1024 ||", "budget < 1000 ||")],
        "wrong-effort-in-thinking": [('params.thinking = { type: "adaptive" };', 'params.thinking = { type: "adaptive", ...(effort ? { effort } : {}) };')],
    }),
    "java": ("Params.java", {
        "wrong-allow-disabled": [('                if (!haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");\n', "")],
        "wrong-between-any-effort": [('                String level = effort != null ? effort : DEFAULT_EFFORT.get(family);\n                if (level.equals("xhigh") || level.equals("max")) {\n                    throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");\n                }\n', "")],
        "wrong-effort-on-haiku": [('            if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");\n', "")],
        "wrong-sampling-pass-through": [('            if (!haiku && !defaultTemperature) throw new RejectedRequest(name, "this model rejects a non-default value");\n', "")],
        "wrong-fast-in-batch": [('            if (Boolean.TRUE.equals(options.get("batch"))) throw new RejectedRequest("speed", "fast mode is not available in a batch");\n', "")],
        "wrong-budget-floor": [("budget.longValue() < 1024", "budget.longValue() < 1000")],
        "wrong-effort-in-thinking": [('params.put("thinking", Map.of("type", "adaptive"));', 'params.put("thinking", effort == null ? Map.of("type", "adaptive") : Map.of("type", "adaptive", "effort", effort));')],
    }),
    "kotlin": ("Params.kt", {
        "wrong-allow-disabled": [('                if (!haiku) throw RejectedRequest("thinking", "thinking cannot be turned off on this model")\n', "")],
        "wrong-between-any-effort": [('                val level = effort ?: DEFAULT_EFFORT.getValue(family)\n                if (level == "xhigh" || level == "max") throw RejectedRequest("thinking", "between_tools works at low, medium and high effort only")\n', "")],
        "wrong-effort-on-haiku": [('        if (haiku) throw RejectedRequest("output_config.effort", "this model does not support effort")\n', "")],
        "wrong-sampling-pass-through": [('        if (!haiku && !defaultTemperature) throw RejectedRequest(name, "this model rejects a non-default value")\n', "")],
        "wrong-fast-in-batch": [('        if (options["batch"] == true) throw RejectedRequest("speed", "fast mode is not available in a batch")\n', "")],
        "wrong-budget-floor": [("budget < 1024 ||", "budget < 1000 ||")],
        "wrong-effort-in-thinking": [('params["thinking"] = mapOf("type" to "adaptive")', 'params["thinking"] = if (effort == null) mapOf("type" to "adaptive") else mapOf("type" to "adaptive", "effort" to effort)')],
    }),
}
