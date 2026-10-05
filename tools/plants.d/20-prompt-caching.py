# Planted wrong solutions of module 20-prompt-caching: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/20-prompt-caching/unit-01/practice-1"] = {
    "python": ("cacheplan.py", {
        "wrong-no-reorder": [('    by_section = sorted(blocks, key=lambda b: SECTIONS[b["section"]])  # stable inside a section\n    return [b for b in by_section if not b.get("volatile", False)] + [b for b in by_section if b.get("volatile", False)]\n', "    return list(blocks)\n")],
        "wrong-sections-unsorted": [('by_section = sorted(blocks, key=lambda b: SECTIONS[b["section"]])  # stable inside a section', "by_section = list(blocks)")],
        "wrong-volatile-first": [('return [b for b in by_section if not b.get("volatile", False)] + [b for b in by_section if b.get("volatile", False)]', 'return [b for b in by_section if b.get("volatile", False)] + [b for b in by_section if not b.get("volatile", False)]')],
        "wrong-per-block-minimum": [("and total >= min_tokens", 'and block["tokens"] >= min_tokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('        elif seen_five:\n            raise PlanError("a 1h breakpoint must come before every 5m breakpoint")\n', "")],
        "wrong-volatile-cached": [('block.get("breakpoint", False) and not block.get("volatile", False) and total', 'block.get("breakpoint", False) and total')],
        "wrong-volatile-tool-allowed": [('if block.get("volatile", False) and block["section"] == "tools":', "if False:")],
        "wrong-input-changed": [("    _check_lifetimes(marked)\n    return plan\n", '    _check_lifetimes(marked)\n    if blocks:\n        blocks[0]["seen"] = True\n    return plan\n')],
    }),
    "typescript": ("cacheplan.ts", {
        "wrong-no-reorder": [('  const bySection = [...blocks].sort((a, b) => SECTIONS[a.section] - SECTIONS[b.section]); // sort is stable\n  return [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];\n', "  return [...blocks];\n")],
        "wrong-sections-unsorted": [("const bySection = [...blocks].sort((a, b) => SECTIONS[a.section] - SECTIONS[b.section]); // sort is stable", "const bySection = [...blocks];")],
        "wrong-volatile-first": [("return [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];", "return [...bySection.filter((b) => b.volatile), ...bySection.filter((b) => !b.volatile)];")],
        "wrong-per-block-minimum": [("&& total >= minTokens", "&& block.tokens >= minTokens")],
        "wrong-five-breakpoints": [("const MAX_BREAKPOINTS = 4;", "const MAX_BREAKPOINTS = 5;")],
        "wrong-ttl-unchecked": [('    else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");\n', "")],
        "wrong-volatile-cached": [("Boolean(block.breakpoint) && !block.volatile && total", "Boolean(block.breakpoint) && total")],
        "wrong-volatile-tool-allowed": [('if (b.volatile && b.section === "tools") throw', 'if (false) throw')],
        "wrong-input-changed": [("  checkLifetimes(marked);\n  return plan;\n", "  checkLifetimes(marked);\n  if (blocks.length > 0) (blocks[0] as any).seen = true;\n  return plan;\n")],
    }),
    "java": ("CachePlan.java", {
        "wrong-no-reorder": [('        List<Map<String, Object>> bySection = new ArrayList<>(blocks);\n        bySection.sort(Comparator.comparingInt(b -> SECTIONS.get((String) b.get("section")))); // List.sort is stable\n        List<Map<String, Object>> result = new ArrayList<>();\n        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(result::add);\n        bySection.stream().filter(b -> flag(b, "volatile")).forEach(result::add);\n        return result;\n', "        return new ArrayList<>(blocks);\n")],
        "wrong-sections-unsorted": [('        bySection.sort(Comparator.comparingInt(b -> SECTIONS.get((String) b.get("section")))); // List.sort is stable\n', "")],
        "wrong-volatile-first": [('        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(result::add);\n        bySection.stream().filter(b -> flag(b, "volatile")).forEach(result::add);', '        bySection.stream().filter(b -> flag(b, "volatile")).forEach(result::add);\n        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(result::add);')],
        "wrong-per-block-minimum": [("&& total >= minTokens", '&& ((Number) block.get("tokens")).longValue() >= minTokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('            else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");\n', "")],
        "wrong-volatile-cached": [('flag(block, "breakpoint") && !flag(block, "volatile") && total', 'flag(block, "breakpoint") && total')],
        "wrong-volatile-tool-allowed": [('if (flag(b, "volatile") && "tools".equals(b.get("section"))) {', "if (false) {")],
        "wrong-input-changed": [("        checkLifetimes(marked);\n        return plan;\n", '        checkLifetimes(marked);\n        if (!blocks.isEmpty()) blocks.get(0).put("seen", true);\n        return plan;\n')],
    }),
    "kotlin": ("CachePlan.kt", {
        "wrong-no-reorder": [('    val bySection = blocks.sortedBy { SECTIONS.getValue(it["section"] as String) } // sortedBy is stable\n    return bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }\n', "    return blocks.toList()\n")],
        "wrong-sections-unsorted": [('val bySection = blocks.sortedBy { SECTIONS.getValue(it["section"] as String) } // sortedBy is stable', "val bySection = blocks")],
        "wrong-volatile-first": [('return bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }', 'return bySection.filter { it["volatile"] == true } + bySection.filter { it["volatile"] != true }')],
        "wrong-per-block-minimum": [("&& total >= minTokens", '&& (block["tokens"] as Number).toLong() >= minTokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('        else if (seenFive) throw PlanError("a 1h breakpoint must come before every 5m breakpoint")\n', "")],
        "wrong-volatile-cached": [('block["breakpoint"] == true && block["volatile"] != true && total', 'block["breakpoint"] == true && total')],
        "wrong-volatile-tool-allowed": [('if (b["volatile"] == true && b["section"] == "tools") throw', "if (false) throw")],
        "wrong-input-changed": [("    checkLifetimes(marked)\n    return plan\n", '    checkLifetimes(marked)\n    if (blocks.isNotEmpty()) (blocks[0] as MutableMap<String, Any?>)["seen"] = true\n    return plan\n')],
    }),
}
