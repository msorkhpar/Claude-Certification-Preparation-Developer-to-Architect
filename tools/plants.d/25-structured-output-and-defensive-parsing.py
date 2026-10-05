# Planted wrong solutions of module 25-structured-output-and-defensive-parsing: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/25-structured-output-and-defensive-parsing/unit-01/practice-1"] = {
    "python": ("extractor.py", {
        "wrong-no-prose-skip": [('first, last = body.find("{"), body.rfind("}")', "first, last = 0, len(body) - 1")],
        "wrong-generic-retry": [('    lines = "\\n".join(f"- {e[\'path\']}: {e[\'message\']}" for e in errors)\n    return f"Your reply was rejected:\\n{lines}\\nReturn the corrected JSON only."', '    return "Your reply was rejected. Return the corrected JSON only."')],
        "wrong-extra-attempt": [("for attempt in range(1, max_attempts + 1):", "for attempt in range(1, max_attempts + 2):")],
        "wrong-retry-refusal": [('        if reply.get("stop_reason") == "refusal":\n            return {"status": "refused", "value": None, "attempts": attempt, "errors": []}\n', "")],
        "wrong-no-grounding": [('            for name in evidence_fields:\n                quoted = value.get(name) if isinstance(value, dict) else None\n                if isinstance(quoted, str) and quoted not in document:\n                    errors.append({"path": f"$.{name}", "message": "is not found in the document"})\n', "")],
        "wrong-integer-allows-fraction": [("    return (isinstance(value, int) and not isinstance(value, bool)) or (isinstance(value, float) and value.is_integer())", "    return _is_number(value)")],
    }),
    "typescript": ("extractor.ts", {
        "wrong-no-prose-skip": [('const first = body.indexOf("{");\n  const last = body.lastIndexOf("}");', "const first = 0;\n  const last = body.length - 1;")],
        "wrong-generic-retry": [('return `Your reply was rejected:\\n${errors.map((e) => `- ${e.path}: ${e.message}`).join("\\n")}\\nReturn the corrected JSON only.`;', 'return "Your reply was rejected. Return the corrected JSON only.";')],
        "wrong-extra-attempt": [("attempt <= maxAttempts; attempt++", "attempt <= maxAttempts + 1; attempt++")],
        "wrong-retry-refusal": [('    if (reply.stop_reason === "refusal") return { status: "refused", value: null, attempts: attempt, errors: [] };\n', "")],
        "wrong-no-grounding": [('      for (const name of evidenceFields) {\n        const quoted = value !== null && typeof value === "object" ? value[name] : undefined;\n        if (typeof quoted === "string" && !document.includes(quoted)) errors.push({ path: `$.${name}`, message: "is not found in the document" });\n      }\n', "")],
        "wrong-integer-allows-fraction": [('integer: (v) => typeof v === "number" && Number.isInteger(v),', 'integer: (v) => typeof v === "number",')],
    }),
    "java": ("Extractor.java", {
        "wrong-no-prose-skip": [("int first = body.indexOf('{');\n        int last = body.lastIndexOf('}');", "int first = 0;\n        int last = body.length() - 1;")],
        "wrong-generic-retry": [('return "Your reply was rejected:\\n" + lines + "Return the corrected JSON only.";', 'return "Your reply was rejected. Return the corrected JSON only.";')],
        "wrong-extra-attempt": [("attempt <= maxAttempts; attempt++", "attempt <= maxAttempts + 1; attempt++")],
        "wrong-retry-refusal": [('            if ("refusal".equals(reply.get("stop_reason"))) return result("refused", null, attempt, new ArrayList<>());\n', "")],
        "wrong-no-grounding": [('                for (String name : evidenceFields) {\n                    Object quoted = value instanceof Map<?, ?> m ? m.get(name) : null;\n                    if (quoted instanceof String s && !document.contains(s)) errors.add(problem("$." + name, "is not found in the document"));\n                }\n', "")],
        "wrong-integer-allows-fraction": [("return v instanceof Double d && !d.isInfinite() && !d.isNaN() && d == Math.rint(d);", "return v instanceof Number;")],
    }),
    "kotlin": ("Extractor.kt", {
        "wrong-no-prose-skip": [("val first = body.indexOf('{')\n    val last = body.lastIndexOf('}')", "val first = 0\n    val last = body.length - 1")],
        "wrong-generic-retry": [('"Your reply was rejected:\\n" + errors.joinToString("") { "- ${it["path"]}: ${it["message"]}\\n" } + "Return the corrected JSON only."', '"Your reply was rejected. Return the corrected JSON only."')],
        "wrong-extra-attempt": [("for (attempt in 1..maxAttempts) {", "for (attempt in 1..maxAttempts + 1) {")],
        "wrong-retry-refusal": [('        if (reply["stop_reason"] == "refusal") return result("refused", null, attempt, emptyList())\n', "")],
        "wrong-no-grounding": [('            for (name in evidenceFields) {\n                val quoted = (value as? Map<*, *>)?.get(name)\n                if (quoted is String && !document.contains(quoted)) found += problem("$.$name", "is not found in the document")\n            }\n', "")],
        "wrong-integer-allows-fraction": [("v is Long || v is Int || v is Short || v is Byte || (v is Double && !v.isInfinite() && !v.isNaN() && v == Math.rint(v))", "v is Number")],
    }),
}
