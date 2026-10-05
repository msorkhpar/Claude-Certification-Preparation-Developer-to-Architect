# Planted wrong solutions of module 32-mcp-fundamentals: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/32-mcp-fundamentals/unit-01/practice-1"] = {
    "python": ("notes_server.py", {
        "wrong-limit-default": [("    return 5\n\n\ndef search_annotations", "    return 10\n\n\ndef search_annotations")],
        "wrong-long-not-flagged": [('raise ToolError(f"text is too long (max {MAX_TEXT})")', "return None")],
        "wrong-limit-bound": [("if not 1 <= limit <= 20:", "if not 1 <= limit <= 21:")],
        "wrong-case-sensitive": [("needle = query.lower()", "needle = query")],
        "wrong-no-limit": [('"\\n".join(hits[:limit])', '"\\n".join(hits)')],
        "wrong-read-only-unmarked": [("    return ToolAnnotations(read_only_hint=True)\n", "    return ToolAnnotations(read_only_hint=False)\n")],
        "wrong-plural": [('("" if count == 1 else "s")', '"s"')],
        "wrong-default-tone": [('tone: str = "brief"', 'tone: str = "short"')],
        "wrong-saves-untrimmed": [("    title, text = title.strip(), text.strip()\n", "    raw_title, raw_text = title, text\n    title, text = title.strip(), text.strip()\n"),
                                  ('    NOTES.append((title, text))\n    return f"Saved note {len(NOTES)}: {title}"', '    NOTES.append((raw_title, raw_text))\n    return f"Saved note {len(NOTES)}: {raw_title}"')],
    }),
    "typescript": ("notes_server.ts", {
        "wrong-limit-default": [("function defaultLimit(): number {\n  return 5;", "function defaultLimit(): number {\n  return 10;")],
        "wrong-long-not-flagged": [("return `text is too long (max ${MAX_TEXT})`;", "return null;")],
        "wrong-limit-bound": [('if (limit < 1 || limit > 20) return "limit', 'if (limit < 1 || limit > 21) return "limit')],
        "wrong-case-sensitive": [("const needle = query.toLowerCase();", "const needle = query;")],
        "wrong-no-limit": [('hits.slice(0, limit).join("\\n")', 'hits.join("\\n")')],
        "wrong-read-only-unmarked": [("return { readOnlyHint: true };", "return { readOnlyHint: false };")],
        "wrong-plural": [('${count === 1 ? "" : "s"}', "s")],
        "wrong-default-tone": [('tone ?? "brief"', 'tone ?? "short"')],
        "wrong-saves-untrimmed": [("    title = title.trim();\n", "    const raw = { title, text };\n    title = title.trim();\n"),
                                  ("    notes.push({ title, text });\n    return ok(`Saved note ${notes.length}: ${title}`);", "    notes.push(raw);\n    return ok(`Saved note ${notes.length}: ${raw.title}`);")],
    }),
    "java": ("NotesServer.java", {
        "wrong-limit-default": [("static int defaultLimit() {\n        return 5;", "static int defaultLimit() {\n        return 10;")],
        "wrong-long-not-flagged": [('return "text is too long (max " + MAX_TEXT + ")";', "return null;")],
        "wrong-limit-bound": [('if (limit < 1 || limit > 20) return "limit', 'if (limit < 1 || limit > 21) return "limit')],
        "wrong-case-sensitive": [("String needle = query.toLowerCase(Locale.ROOT);", "String needle = query;")],
        "wrong-no-limit": [('String.join("\\n", hits.subList(0, Math.min(limit, hits.size())))', 'String.join("\\n", hits)')],
        "wrong-read-only-unmarked": [("ToolAnnotations.builder().readOnlyHint(true).build();", "ToolAnnotations.builder().readOnlyHint(false).build();")],
        "wrong-plural": [('(count == 1 ? "" : "s")', '"s"')],
        "wrong-default-tone": [('tone == null ? "brief" : String.valueOf(tone)', 'tone == null ? "short" : String.valueOf(tone)')],
        "wrong-saves-untrimmed": [('        NOTES.add(new Note(title, text));\n        return ok("Saved note " + NOTES.size() + ": " + title);',
                                   '        String rawTitle = String.valueOf(args.getOrDefault("title", "")), rawText = String.valueOf(args.getOrDefault("text", ""));\n'
                                   '        NOTES.add(new Note(rawTitle, rawText));\n        return ok("Saved note " + NOTES.size() + ": " + rawTitle);')],
    }),
    "kotlin": ("NotesServer.kt", {
        "wrong-limit-default": [("fun defaultLimit(): Int = 5", "fun defaultLimit(): Int = 10")],
        "wrong-long-not-flagged": [('return "text is too long (max $MAX_TEXT)"', "return null")],
        "wrong-limit-bound": [('if (limit < 1 || limit > 20) return "limit', 'if (limit < 1 || limit > 21) return "limit')],
        "wrong-case-sensitive": [("val needle = query.lowercase()", "val needle = query")],
        "wrong-no-limit": [('hits.take(limit).joinToString("\\n")', 'hits.joinToString("\\n")')],
        "wrong-read-only-unmarked": [("fun searchAnnotations(): ToolAnnotations? = ToolAnnotations(readOnlyHint = true)", "fun searchAnnotations(): ToolAnnotations? = ToolAnnotations(readOnlyHint = false)")],
        "wrong-plural": [('(if (count == 1) "" else "s")', '"s"')],
        "wrong-default-tone": [('?: "brief"', '?: "short"')],
        "wrong-saves-untrimmed": [('    notes.add(Note(title, text))\n    return ok("Saved note ${notes.size}: $title")',
                                   '    val rawTitle = args?.get("title")?.jsonPrimitive?.contentOrNull.orEmpty()\n    val rawText = args?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()\n'
                                   '    notes.add(Note(rawTitle, rawText))\n    return ok("Saved note ${notes.size}: $rawTitle")')],
    }),
}
