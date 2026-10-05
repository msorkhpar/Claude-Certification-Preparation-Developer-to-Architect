# Planted wrong solutions of module 18-model-choice-cost-and-migration: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/18-model-choice-cost-and-migration/unit-01/practice-1"] = {
    "python": ("router.py", {
        "wrong-flat-cache-read": [('usage.get("cache_read_input_tokens", 0) * price * model["cache_read_multiplier"]', 'usage.get("cache_read_input_tokens", 0) * price * 0.1')],
        "wrong-batch-output-only": [('+ usage.get("output_tokens", 0) * model["output"])\n    if batch:\n        total *= 0.5\n', '+ usage.get("output_tokens", 0) * model["output"] * (0.5 if batch else 1))\n')],
        "wrong-first-eligible": [('best = min(eligible, key=lambda m: (request_cost(m, usage, task.get("batch", False)), m["tier"], m["id"]))', "best = eligible[0]")],
        "wrong-ignore-context": [('\n                and _input_tokens(usage) <= m["context"]\n                and wanted <= m["max_output"]]', "]")],
        "wrong-tie-high-tier": [('m["tier"], m["id"]))', '-m["tier"], m["id"]))')],
    }),
    "typescript": ("router.ts", {
        "wrong-flat-cache-read": [("(usage.cache_read_input_tokens ?? 0) * price * model.cache_read_multiplier", "(usage.cache_read_input_tokens ?? 0) * price * 0.1")],
        "wrong-batch-output-only": [("(usage.output_tokens ?? 0) * model.output;\n  if (batch) total *= 0.5;", "(usage.output_tokens ?? 0) * model.output * (batch ? 0.5 : 1);")],
        "wrong-first-eligible": [("return scored[0].m.id;", "return eligible[0].id;")],
        "wrong-ignore-context": [(" && inputTokens(task.usage) <= m.context && wanted <= m.max_output,", ",")],
        "wrong-tie-high-tier": [("a.m.tier - b.m.tier ||", "b.m.tier - a.m.tier ||")],
    }),
    "java": ("Router.java", {
        "wrong-flat-cache-read": [('* ((Number) model.get("cache_read_multiplier")).doubleValue()', "* 0.1")],
        "wrong-batch-output-only": [('+ n(usage, "output_tokens") * ((Number) model.get("output")).doubleValue();\n        if (batch) total *= 0.5;', '+ n(usage, "output_tokens") * ((Number) model.get("output")).doubleValue() * (batch ? 0.5 : 1.0);')],
        "wrong-first-eligible": [('.min(Comparator.<Map<String, Object>>comparingDouble(m -> requestCost(m, usage, batch))\n                        .thenComparingLong(m -> n(m, "tier"))\n                        .thenComparing(m -> (String) m.get("id")))', ".findFirst()")],
        "wrong-ignore-context": [('                .filter(m -> inputTokens(usage) <= n(m, "context"))\n                .filter(m -> wanted <= n(m, "max_output"))\n', "")],
        "wrong-tie-high-tier": [('.thenComparingLong(m -> n(m, "tier"))', '.thenComparingLong(m -> -n(m, "tier"))')],
    }),
    "kotlin": ("Router.kt", {
        "wrong-flat-cache-read": [('* (model["cache_read_multiplier"] as Number).toDouble() +', "* 0.1 +")],
        "wrong-batch-output-only": [('n(usage, "output_tokens") * (model["output"] as Number).toDouble()\n    if (batch) total *= 0.5', 'n(usage, "output_tokens") * (model["output"] as Number).toDouble() * (if (batch) 0.5 else 1.0)')],
        "wrong-first-eligible": [('.minWithOrNull(compareBy<Map<String, Any?>> { requestCost(it, usage, batch) }.thenBy { n(it, "tier") }.thenBy { it["id"] as String })', ".firstOrNull()")],
        "wrong-ignore-context": [(' && inputTokens(usage) <= n(it, "context") && wanted <= n(it, "max_output") }', " }")],
        "wrong-tie-high-tier": [('.thenBy { n(it, "tier") }', ".thenByDescending { n(it, \"tier\") }")],
    }),
}
