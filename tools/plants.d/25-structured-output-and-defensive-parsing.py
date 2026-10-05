# Planted wrong solutions of module 25-structured-output-and-defensive-parsing: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/25-structured-output-and-defensive-parsing/unit-01/practice-1"] = {
    "python": ("extractor.py", {
        "wrong-no-prose-skip": [('    return body.find("{"), body.rfind("}")', "    return 0, len(body) - 1")],
        "wrong-generic-retry": [('    lines = "\\n".join(f"- {e[\'path\']}: {e[\'message\']}" for e in errors)\n    return f"Your reply was rejected:\\n{lines}\\nReturn the corrected JSON only."', '    return "Your reply was rejected. Return the corrected JSON only."')],
        "wrong-extra-attempt": [("for attempt in range(1, max_attempts + 1):", "for attempt in range(1, max_attempts + 2):")],
        "wrong-retry-refusal": [('if reply.get("stop_reason") == "refusal":', "if False:")],
        "wrong-no-grounding": [("if isinstance(quoted, str) and quoted not in document:", "if False:")],
        "wrong-integer-allows-fraction": [("    return (isinstance(value, int) and not isinstance(value, bool)) or (isinstance(value, float) and value.is_integer())", "    return _is_number(value)")],
    }),
    "typescript": ("extractor.ts", {
        "wrong-no-prose-skip": [('return [body.indexOf("{"), body.lastIndexOf("}")];', "return [0, body.length - 1];")],
        "wrong-generic-retry": [('return `Your reply was rejected:\\n${errors.map((e) => `- ${e.path}: ${e.message}`).join("\\n")}\\nReturn the corrected JSON only.`;', 'return "Your reply was rejected. Return the corrected JSON only.";')],
        "wrong-extra-attempt": [("attempt <= maxAttempts; attempt++", "attempt <= maxAttempts + 1; attempt++")],
        "wrong-retry-refusal": [('if (reply.stop_reason === "refusal") return "refused";', 'if (false) return "refused";')],
        "wrong-no-grounding": [('typeof quoted === "string" && !document.includes(quoted)', "false")],
        "wrong-integer-allows-fraction": [('return typeof v === "number" && Number.isInteger(v);', 'return typeof v === "number";')],
    }),
    "java": ("Extractor.java", {
        "wrong-no-prose-skip": [("return new int[] {body.indexOf('{'), body.lastIndexOf('}')};", "return new int[] {0, body.length() - 1};")],
        "wrong-generic-retry": [('return "Your reply was rejected:\\n" + lines + "Return the corrected JSON only.";', 'return "Your reply was rejected. Return the corrected JSON only.";')],
        "wrong-extra-attempt": [("attempt <= maxAttempts; attempt++", "attempt <= maxAttempts + 1; attempt++")],
        "wrong-retry-refusal": [('if ("refusal".equals(reply.get("stop_reason"))) return "refused";', 'if (false) return "refused";')],
        "wrong-no-grounding": [("quoted instanceof String s && !document.contains(s)", "false")],
        "wrong-integer-allows-fraction": [("return v instanceof Double d && !d.isInfinite() && !d.isNaN() && d == Math.rint(d);", "return v instanceof Number;")],
    }),
    "kotlin": ("Extractor.kt", {
        "wrong-no-prose-skip": [("Pair(body.indexOf('{'), body.lastIndexOf('}'))", "Pair(0, body.length - 1)")],
        "wrong-generic-retry": [('"Your reply was rejected:\\n" + errors.joinToString("") { "- ${it["path"]}: ${it["message"]}\\n" } + "Return the corrected JSON only."', '"Your reply was rejected. Return the corrected JSON only."')],
        "wrong-extra-attempt": [("for (attempt in 1..maxAttempts) {", "for (attempt in 1..maxAttempts + 1) {")],
        "wrong-retry-refusal": [('    "refusal" -> "refused"\n', "")],
        "wrong-no-grounding": [("quoted is String && !document.contains(quoted)", "false")],
        "wrong-integer-allows-fraction": [("v is Long || v is Int || v is Short || v is Byte || (v is Double && !v.isInfinite() && !v.isNaN() && v == Math.rint(v))", "v is Number")],
    }),
}
