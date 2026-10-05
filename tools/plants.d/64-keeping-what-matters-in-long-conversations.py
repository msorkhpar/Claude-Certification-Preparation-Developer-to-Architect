# Planted wrong solutions of module 64-keeping-what-matters-in-long-conversations: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"] = {
    "python": ("context_builder.py", {
        "wrong-no-trim": [("    return {k: record[k] for k in keep if k in record}", "    return dict(record)")],
        "wrong-older-overwrites": [('    elif as_of >= current["as_of"]:', "    elif True:")],
        "wrong-all-customers": [('    mine = [f for f in facts if f["customer"] == customer]', "    mine = list(facts)")],
        "wrong-facts-last": [(r'    return "\n\n".join(parts)', r'    return "\n\n".join(reversed(parts))')],
        "wrong-pair-split": [('        if m["kind"] == "tool_use" and', '        if False and m["kind"] == "tool_use" and')],
        "wrong-loose-summary-check": [('f["value"] not in summary', 'f["value"][:2] not in summary')],
        "wrong-missing-field-blank": [('{k: record[k] for k in keep if k in record}', '{k: record.get(k, "") for k in keep}')],
    }),
    "typescript": ("contextBuilder.ts", {
        "wrong-no-trim": [("  for (const k of keep) if (k in record) out[k] = record[k];", "  Object.assign(out, record);")],
        "wrong-older-overwrites": [("} else if (asOf >= current.as_of) {", "} else if (true) {")],
        "wrong-all-customers": [("const mine = facts.filter((f) => f.customer === customer);", "const mine = facts;")],
        "wrong-facts-last": [('return parts.join("\\n\\n");', 'return parts.reverse().join("\\n\\n");')],
        "wrong-pair-split": [('if (m.kind === "tool_use" &&', 'if (false && m.kind === "tool_use" &&')],
        "wrong-loose-summary-check": [("!summary.includes(f.value)", "!summary.includes(f.value.slice(0, 2))")],
        "wrong-missing-field-blank": [('for (const k of keep) if (k in record) out[k] = record[k];', 'for (const k of keep) out[k] = record[k] ?? "";')],
    }),
    "java": ("ContextBuilder.java", {
        "wrong-no-trim": [("for (String k : keep) if (record.containsKey(k)) out.put(k, record.get(k));", "out.putAll(record);")],
        "wrong-older-overwrites": [("} else if (asOf.compareTo(current.asOf()) >= 0) {", "} else if (true) {")],
        "wrong-all-customers": [("if (f.customer().equals(customer)) mine.add(", "mine.add(")],
        "wrong-facts-last": [('return String.join("\\n\\n", parts);', 'java.util.Collections.reverse(parts);\n        return String.join("\\n\\n", parts);')],
        "wrong-pair-split": [('if (m.kind().equals("tool_use") &&', 'if (false && m.kind().equals("tool_use") &&')],
        "wrong-loose-summary-check": [("!summary.contains(f.value())", "!summary.contains(f.value().substring(0, 2))")],
        "wrong-missing-field-blank": [('for (String k : keep) if (record.containsKey(k)) out.put(k, record.get(k));', 'for (String k : keep) out.put(k, record.getOrDefault(k, ""));')],
    }),
    "kotlin": ("ContextBuilder.kt", {
        "wrong-no-trim": [("for (k in keep) if (k in record) out[k] = record.getValue(k)", "out.putAll(record)")],
        "wrong-older-overwrites": [("} else if (asOf >= current.asOf) {", "} else if (true) {")],
        "wrong-all-customers": [("val mine = facts.filter { it.customer == customer }", "val mine = facts")],
        "wrong-facts-last": [('return parts.joinToString("\\n\\n")', 'return parts.asReversed().joinToString("\\n\\n")')],
        "wrong-pair-split": [('if (m.kind == "tool_use" &&', 'if (false && m.kind == "tool_use" &&')],
        "wrong-loose-summary-check": [("it.value !in summary", "it.value.take(2) !in summary")],
        "wrong-missing-field-blank": [('for (k in keep) if (k in record) out[k] = record.getValue(k)', 'for (k in keep) out[k] = record[k] ?: ""')],
    }),
}

# boundary plant (a fact dated the same day as the stored one)

_p = PLANTS[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"]

_p["python"][1]["wrong-equal-date-ignored"] = [('elif as_of >= current["as_of"]:', 'elif as_of > current["as_of"]:')]

_p["typescript"][1]["wrong-equal-date-ignored"] = [("} else if (asOf >= current.as_of) {", "} else if (asOf > current.as_of) {")]

_p["java"][1]["wrong-equal-date-ignored"] = [("} else if (asOf.compareTo(current.asOf()) >= 0) {", "} else if (asOf.compareTo(current.asOf()) > 0) {")]

_p["kotlin"][1]["wrong-equal-date-ignored"] = [("} else if (asOf >= current.asOf) {", "} else if (asOf > current.asOf) {")]
