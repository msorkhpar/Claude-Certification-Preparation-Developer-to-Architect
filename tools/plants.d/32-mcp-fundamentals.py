# Planted wrong solutions of module 32-mcp-fundamentals: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/32-mcp-fundamentals/unit-01/practice-1"] = {
    "python": ("notes_server.py", {
        "wrong-error-not-flagged": [('raise ToolError("title is required")', 'return "title is required"')],
        "wrong-case-sensitive": [("needle = query.strip().lower()", "needle = query.strip()")],
        "wrong-no-limit": [('"\\n".join(hits[:limit])', '"\\n".join(hits)')],
        "wrong-read-only-unmarked": [("annotations=ToolAnnotations(read_only_hint=True)", "annotations=ToolAnnotations(read_only_hint=False)")],
        "wrong-plural": [('("" if len(NOTES) == 1 else "s")', '"s"')],
        "wrong-default-tone": [('tone: str = "brief"', 'tone: str = "short"')],
        "wrong-no-trim": [("title, text = title.strip(), text.strip()", "title, text = title, text")],
    }),
    "typescript": ("notes_server.ts", {
        "wrong-error-not-flagged": [('if (!title) return fail("title is required");', 'if (!title) return ok("title is required");')],
        "wrong-case-sensitive": [("const needle = query.trim().toLowerCase();", "const needle = query.trim();")],
        "wrong-no-limit": [('hits.slice(0, limit).join("\\n")', 'hits.join("\\n")')],
        "wrong-read-only-unmarked": [("annotations: { readOnlyHint: true }", "annotations: { readOnlyHint: false }")],
        "wrong-plural": [('${notes.length === 1 ? "" : "s"}', "s")],
        "wrong-default-tone": [('tone ?? "brief"', 'tone ?? "short"')],
        "wrong-no-trim": [("    title = title.trim();\n    text = text.trim();\n", "")],
    }),
    "java": ("NotesServer.java", {
        "wrong-error-not-flagged": [("return CallToolResult.builder().addTextContent(message).isError(true).build();", "return CallToolResult.builder().addTextContent(message).isError(false).build();")],
        "wrong-case-sensitive": [("String needle = query.toLowerCase(Locale.ROOT);", "String needle = query;")],
        "wrong-no-limit": [('String.join("\\n", hits.subList(0, Math.min(limit, hits.size())))', 'String.join("\\n", hits)')],
        "wrong-read-only-unmarked": [(".annotations(ToolAnnotations.builder().readOnlyHint(true).build())", ".annotations(ToolAnnotations.builder().readOnlyHint(false).build())")],
        "wrong-plural": [('(NOTES.size() == 1 ? "" : "s")', '"s"')],
        "wrong-default-tone": [('(tone == null ? "brief" : tone)', '(tone == null ? "short" : tone)')],
        "wrong-no-trim": [('String title = String.valueOf(args.getOrDefault("title", "")).strip(), text = String.valueOf(args.getOrDefault("text", "")).strip();',
                           'String title = String.valueOf(args.getOrDefault("title", "")), text = String.valueOf(args.getOrDefault("text", ""));')],
    }),
    "kotlin": ("NotesServer.kt", {
        "wrong-error-not-flagged": [("fun fail(message: String) = CallToolResult(content = listOf(TextContent(message)), isError = true)", "fun fail(message: String) = CallToolResult(content = listOf(TextContent(message)), isError = false)")],
        "wrong-case-sensitive": [("val needle = query.lowercase()", "val needle = query")],
        "wrong-no-limit": [('hits.take(limit).joinToString("\\n")', 'hits.joinToString("\\n")')],
        "wrong-read-only-unmarked": [("toolAnnotations = ToolAnnotations(readOnlyHint = true)", "toolAnnotations = ToolAnnotations(readOnlyHint = false)")],
        "wrong-plural": [('(if (notes.size == 1) "" else "s")', '"s"')],
        "wrong-default-tone": [('?: "brief"', '?: "short"')],
        "wrong-no-trim": [('val title = args?.get("title")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()', 'val title = args?.get("title")?.jsonPrimitive?.contentOrNull.orEmpty()'),
                          ('val text = args?.get("text")?.jsonPrimitive?.contentOrNull?.trim().orEmpty()', 'val text = args?.get("text")?.jsonPrimitive?.contentOrNull.orEmpty()')],
    }),
}
