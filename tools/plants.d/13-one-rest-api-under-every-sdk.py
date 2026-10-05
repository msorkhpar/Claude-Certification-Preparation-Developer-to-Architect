# Planted wrong solutions of module 13-one-rest-api-under-every-sdk: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/13-one-rest-api-under-every-sdk/unit-01/practice-1"] = {
    "python": ("raw_client.py", {
        "wrong-no-version-header": [('"anthropic-version": "2023-06-01", ', "")],
        "wrong-body-id-first": [('request_id = request_id or parsed.get("request_id")', 'request_id = parsed.get("request_id") or request_id')],
        "wrong-no-redaction": [('message.replace(api_key, "[redacted]")', "message")],
    }),
    "typescript": ("rawClient.ts", {
        "wrong-no-version-header": [('"anthropic-version": "2023-06-01", ', "")],
        "wrong-body-id-first": [("requestId = requestId ?? parsed.request_id ?? null;", "requestId = parsed.request_id ?? requestId ?? null;")],
        "wrong-no-redaction": [('message.split(apiKey).join("[redacted]")', "message")],
    }),
    "java": ("RawClient.java", {
        "wrong-no-version-header": [('        headers.put("anthropic-version", "2023-06-01");\n', "")],
        "wrong-body-id-first": [('if (requestId == null && parsed.get("request_id") != null)', 'if (parsed.get("request_id") != null)')],
        "wrong-no-redaction": [('message.replace(apiKey, "[redacted]")', "message")],
    }),
    "kotlin": ("RawClient.kt", {
        "wrong-no-version-header": [(', "anthropic-version" to "2023-06-01"', "")],
        "wrong-body-id-first": [('if (requestId == null) requestId = (parsed as Map<*, *>)["request_id"]?.toString()',
                                 'requestId = (parsed as Map<*, *>)["request_id"]?.toString() ?: requestId')],
        "wrong-no-redaction": [('message.replace(apiKey, "[redacted]")', "message")],
    }),
}
