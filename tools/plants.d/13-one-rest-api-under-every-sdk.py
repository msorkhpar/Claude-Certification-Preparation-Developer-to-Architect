# Planted wrong solutions of module 13-one-rest-api-under-every-sdk: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/13-one-rest-api-under-every-sdk/unit-01/practice-1"] = {
    "python": ("raw_client.py", {
        "wrong-no-version-header": [('"anthropic-version": "2023-06-01", ', "")],
        "wrong-blank-system-kept": [("    if _present(system):", "    if system is not None:")],
        "wrong-zero-max-tokens-ok": [("max_tokens < 1:", "max_tokens < 0:")],
        "wrong-text-joined-with-space": [('return "".join(b.get(', 'return " ".join(b.get(')],
        "wrong-body-id-first": [("return header_id or body_id", "return body_id or header_id")],
        "wrong-unknown-type-renamed": [('return "unknown", text.strip()[:200]', 'return "error", text.strip()[:200]')],
        "wrong-no-redaction": [('return message.replace(api_key, "[redacted]")', "return message")],
    }),
    "typescript": ("rawClient.ts", {
        "wrong-no-version-header": [('"anthropic-version": "2023-06-01", ', "")],
        "wrong-blank-system-kept": [("if (present(system)) body.system = system;", "if (system !== null && system !== undefined) body.system = system;")],
        "wrong-zero-max-tokens-ok": [("|| maxTokens < 1)", "|| maxTokens < 0)")],
        "wrong-text-joined-with-space": [('.map((b) => b.text ?? "").join("")', '.map((b) => b.text ?? "").join(" ")')],
        "wrong-body-id-first": [("return headerId ?? bodyId ?? null;", "return bodyId ?? headerId ?? null;")],
        "wrong-unknown-type-renamed": [('return ["unknown", text.trim().slice(0, 200)];', 'return ["error", text.trim().slice(0, 200)];')],
        "wrong-no-redaction": [('return message.split(apiKey).join("[redacted]");', "return message;")],
    }),
    "java": ("RawClient.java", {
        "wrong-no-version-header": [('        headers.put("anthropic-version", "2023-06-01");\n', "")],
        "wrong-blank-system-kept": [('if (present(system)) body.put("system", system);', 'if (system != null) body.put("system", system);')],
        "wrong-zero-max-tokens-ok": [("if (maxTokens < 1)", "if (maxTokens < 0)")],
        "wrong-text-joined-with-space": [('out.append(block.get("text"));', 'out.append(block.get("text")).append(" ");')],
        "wrong-body-id-first": [("return headerId != null ? headerId : bodyId;", "return bodyId != null ? bodyId : headerId;")],
        "wrong-unknown-type-renamed": [('return new String[] {"unknown", trimmed.length()', 'return new String[] {"error", trimmed.length()')],
        "wrong-no-redaction": [('return message.replace(apiKey, "[redacted]");', "return message;")],
    }),
    "kotlin": ("RawClient.kt", {
        "wrong-no-version-header": [(', "anthropic-version" to "2023-06-01"', "")],
        "wrong-blank-system-kept": [('if (present(system)) body["system"] = system', 'if (system != null) body["system"] = system')],
        "wrong-zero-max-tokens-ok": [("require(maxTokens >= 1)", "require(maxTokens >= 0)")],
        "wrong-text-joined-with-space": [('.joinToString("") { it["text"].toString() }', '.joinToString(" ") { it["text"].toString() }')],
        "wrong-body-id-first": [("headerId ?: bodyId", "bodyId ?: headerId")],
        "wrong-unknown-type-renamed": [('return Pair("unknown", text.trim().take(200))', 'return Pair("error", text.trim().take(200))')],
        "wrong-no-redaction": [('message.replace(apiKey, "[redacted]")', "message")],
    }),
}
