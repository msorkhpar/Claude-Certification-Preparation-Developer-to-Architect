# Planted wrong solutions of module 17-streaming: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/17-streaming/unit-01/practice-1"] = {
    "python": ("assemble.py", {
        "wrong-overwrite-text": [('block["text"] = block.get("text", "") + delta["text"]', 'block["text"] = delta["text"]')],
        "wrong-sum-usage": [('            message["usage"].update(event.get("usage", {}))',
                             '            usage = event.get("usage", {})\n            message["usage"]["output_tokens"] = message["usage"].get("output_tokens", 0) + usage.get("output_tokens", 0)')],
        "wrong-ignore-error": [('            raise StreamError(event["error"].get("type", "unknown"), event["error"].get("message", ""))', "            continue")],
        "wrong-accept-incomplete": [("    if message is None or not stopped:", "    if message is None:")],
    }),
    "typescript": ("assemble.ts", {
        "wrong-overwrite-text": [('block.text = (block.text ?? "") + delta.text;', "block.text = delta.text;")],
        "wrong-sum-usage": [("Object.assign(message!.usage, event.usage ?? {});", "message!.usage.output_tokens = (message!.usage.output_tokens ?? 0) + (event.usage?.output_tokens ?? 0);")],
        "wrong-ignore-error": [('throw new StreamError(event.error?.type ?? "unknown", event.error?.message ?? "");', "break;")],
        "wrong-accept-incomplete": [("if (message === null || !stopped) throw", "if (message === null) throw")],
    }),
    "java": ("Assemble.java", {
        "wrong-overwrite-text": [('block.put("text", String.valueOf(block.getOrDefault("text", "")) + delta.get("text"))', 'block.put("text", delta.get("text"))')],
        "wrong-sum-usage": [("if (event.get(\"usage\") instanceof Map<?, ?> u) ((Map<String, Object>) message.get(\"usage\")).putAll((Map<String, Object>) u);",
                             "if (event.get(\"usage\") instanceof Map<?, ?> u) {\n                        Map<String, Object> total = (Map<String, Object>) message.get(\"usage\");\n                        total.put(\"output_tokens\", ((Number) total.get(\"output_tokens\")).longValue() + ((Number) ((Map<String, Object>) u).get(\"output_tokens\")).longValue());\n                    }")],
        "wrong-ignore-error": [('                    throw new StreamError(String.valueOf(error.getOrDefault("type", "unknown")), String.valueOf(error.getOrDefault("message", "")));', "                    continue;")],
        "wrong-accept-incomplete": [("if (message == null || !stopped) throw", "if (message == null) throw")],
    }),
    "kotlin": ("Assemble.kt", {
        "wrong-overwrite-text": [('"text_delta" -> block["text"] = (block["text"] as? String ?: "") + delta["text"]', '"text_delta" -> block["text"] = delta["text"]')],
        "wrong-sum-usage": [('(event["usage"] as? Map<String, Any?>)?.let { (message["usage"] as MutableMap<String, Any?>).putAll(it) }',
                             '(event["usage"] as? Map<String, Any?>)?.let { u ->\n                    val total = message["usage"] as MutableMap<String, Any?>\n                    total["output_tokens"] = (total["output_tokens"] as Number).toLong() + (u["output_tokens"] as Number).toLong()\n                }')],
        "wrong-ignore-error": [('                throw StreamError(error["type"]?.toString() ?: "unknown", error["message"]?.toString() ?: "")', "                continue")],
        "wrong-accept-incomplete": [("if (message == null || !stopped) throw", "if (message == null) throw")],
    }),
}
