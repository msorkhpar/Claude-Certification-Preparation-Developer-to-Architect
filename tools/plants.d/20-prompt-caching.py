# Planted wrong solutions of module 20-prompt-caching: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/20-prompt-caching/unit-01/practice-1"] = {
    "python": ("cacheplan.py", {
        "wrong-no-reorder": [('    ordered = sorted(blocks, key=lambda b: SECTIONS[b["section"]])  # stable inside a section\n    ordered = [b for b in ordered if not b.get("volatile", False)] + [b for b in ordered if b.get("volatile", False)]\n', "    ordered = list(blocks)\n")],
        "wrong-volatile-first": [('ordered = [b for b in ordered if not b.get("volatile", False)] + [b for b in ordered if b.get("volatile", False)]', 'ordered = [b for b in ordered if b.get("volatile", False)] + [b for b in ordered if not b.get("volatile", False)]')],
        "wrong-per-block-minimum": [('wanted = block.get("breakpoint", False) and not volatile and total >= min_tokens', 'wanted = block.get("breakpoint", False) and not volatile and block["tokens"] >= min_tokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('        elif seen_five:\n            raise PlanError("a 1h breakpoint must come before every 5m breakpoint")\n', "")],
        "wrong-volatile-cached": [('block.get("breakpoint", False) and not volatile and total', 'block.get("breakpoint", False) and total')],
    }),
    "typescript": ("cacheplan.ts", {
        "wrong-no-reorder": [('  const bySection = [...blocks].sort((a, b) => SECTIONS[a.section] - SECTIONS[b.section]); // sort is stable\n  const ordered = [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];\n', "  const ordered = [...blocks];\n")],
        "wrong-volatile-first": [("const ordered = [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];", "const ordered = [...bySection.filter((b) => b.volatile), ...bySection.filter((b) => !b.volatile)];")],
        "wrong-per-block-minimum": [("Boolean(b.breakpoint) && !b.volatile && total >= minTokens", "Boolean(b.breakpoint) && !b.volatile && b.tokens >= minTokens")],
        "wrong-five-breakpoints": [("const MAX_BREAKPOINTS = 4;", "const MAX_BREAKPOINTS = 5;")],
        "wrong-ttl-unchecked": [('    else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");\n', "")],
        "wrong-volatile-cached": [("Boolean(b.breakpoint) && !b.volatile && total", "Boolean(b.breakpoint) && total")],
    }),
    "java": ("CachePlan.java", {
        "wrong-no-reorder": [('        List<Map<String, Object>> bySection = new ArrayList<>(blocks);\n        bySection.sort(Comparator.comparingInt(b -> SECTIONS.get((String) b.get("section")))); // List.sort is stable\n        List<Map<String, Object>> ordered = new ArrayList<>();\n        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(ordered::add);\n        bySection.stream().filter(b -> flag(b, "volatile")).forEach(ordered::add);\n', "        List<Map<String, Object>> ordered = new ArrayList<>(blocks);\n")],
        "wrong-volatile-first": [('        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(ordered::add);\n        bySection.stream().filter(b -> flag(b, "volatile")).forEach(ordered::add);', '        bySection.stream().filter(b -> flag(b, "volatile")).forEach(ordered::add);\n        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(ordered::add);')],
        "wrong-per-block-minimum": [('flag(b, "breakpoint") && !isVolatile && total >= minTokens', 'flag(b, "breakpoint") && !isVolatile && ((Number) b.get("tokens")).longValue() >= minTokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('            else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");\n', "")],
        "wrong-volatile-cached": [('flag(b, "breakpoint") && !isVolatile && total', 'flag(b, "breakpoint") && total')],
    }),
    "kotlin": ("CachePlan.kt", {
        "wrong-no-reorder": [('    val bySection = blocks.sortedBy { SECTIONS.getValue(it["section"] as String) } // sortedBy is stable\n    val ordered = bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }\n', "    val ordered = blocks\n")],
        "wrong-volatile-first": [('val ordered = bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }', 'val ordered = bySection.filter { it["volatile"] == true } + bySection.filter { it["volatile"] != true }')],
        "wrong-per-block-minimum": [('b["breakpoint"] == true && !isVolatile && total >= minTokens', 'b["breakpoint"] == true && !isVolatile && (b["tokens"] as Number).toLong() >= minTokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('        else if (seenFive) throw PlanError("a 1h breakpoint must come before every 5m breakpoint")\n', "")],
        "wrong-volatile-cached": [('b["breakpoint"] == true && !isVolatile && total', 'b["breakpoint"] == true && total')],
    }),
}
