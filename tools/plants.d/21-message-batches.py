# Planted wrong solutions of module 21-message-batches: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/21-message-batches/unit-01/practice-1"] = {
    "python": ("batches.py", {
        "wrong-by-position": [("    for cid in wanted:\n", "    for cid in list(by_id) + [c for c in wanted if c not in by_id]:\n")],
        "wrong-duplicate-ids": [('    if custom_id in seen:\n        raise BatchError("custom_id", f"{custom_id} is used twice")\n', "")],
        "wrong-allow-stream": [('    if params.get("stream"):\n        raise BatchError("params.stream", "batch results are a file, not a stream")\n    if "speed" in params:\n        raise BatchError("params.speed", "fast mode is not available in a batch")\n', "")],
        "wrong-count-only": [("(count >= max_requests or used + size > max_bytes)", "count >= max_requests")],
        "wrong-retry-invalid": [('return error_type == "invalid_request_error"', "return False")],
        "wrong-missing-ignored": [('        outcomes.append({"custom_id": cid, "status": "missing"})\n            retry.append(cid)\n', "        pass\n")],
        "wrong-usage-no-cache-reads": [('"output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens")', '"output_tokens", "cache_creation_input_tokens")')],
    }),
    "typescript": ("batches.ts", {
        "wrong-by-position": [("  for (const cid of wanted) {\n", "  for (const cid of [...byId.keys(), ...wanted.filter((c) => !byId.has(c))]) {\n")],
        "wrong-duplicate-ids": [("  if (seen.has(id)) throw new BatchError(\"custom_id\", `${id} is used twice`);\n", "")],
        "wrong-allow-stream": [('  if (params.stream) throw new BatchError("params.stream", "batch results are a file, not a stream");\n  if ("speed" in params) throw new BatchError("params.speed", "fast mode is not available in a batch");\n', "")],
        "wrong-count-only": [("(count >= maxRequests || used + bytes > maxBytes)", "count >= maxRequests")],
        "wrong-retry-invalid": [('return errorType === "invalid_request_error";', "return false;")],
        "wrong-missing-ignored": [('      outcomes.push({ custom_id: cid, status: "missing" });\n      retry.push(cid);\n', "")],
        "wrong-usage-no-cache-reads": [('"cache_creation_input_tokens", "cache_read_input_tokens"];', '"cache_creation_input_tokens"];')],
    }),
    "java": ("Batches.java", {
        "wrong-by-position": [("        for (String cid : wanted) {\n", "        for (String cid : java.util.stream.Stream.concat(byId.keySet().stream(), wanted.stream().filter(c -> !byId.containsKey(c))).toList()) {\n")],
        "wrong-duplicate-ids": [('        if (!seen.add(s)) throw new BatchError("custom_id", s + " is used twice");\n', "")],
        "wrong-allow-stream": [('        if (Boolean.TRUE.equals(params.get("stream"))) throw new BatchError("params.stream", "batch results are a file, not a stream");\n        if (params.containsKey("speed")) throw new BatchError("params.speed", "fast mode is not available in a batch");\n', "")],
        "wrong-count-only": [("(count >= maxRequests || used + bytes > maxBytes)", "count >= maxRequests")],
        "wrong-retry-invalid": [('return errorType.equals("invalid_request_error");', "return false;")],
        "wrong-missing-ignored": [('                outcome.put("status", "missing");\n                retry.add(cid);\n', "                continue;\n")],
        "wrong-usage-no-cache-reads": [('"cache_creation_input_tokens", "cache_read_input_tokens");', '"cache_creation_input_tokens");')],
    }),
    "kotlin": ("Batches.kt", {
        "wrong-by-position": [("    for (cid in wanted) {\n", "    for (cid in byId.keys + wanted.filter { it !in byId }) {\n")],
        "wrong-duplicate-ids": [('    if (!seen.add(id)) throw BatchError("custom_id", "$id is used twice")\n', "")],
        "wrong-allow-stream": [('    if (params["stream"] == true) throw BatchError("params.stream", "batch results are a file, not a stream")\n    if (params.containsKey("speed")) throw BatchError("params.speed", "fast mode is not available in a batch")\n', "")],
        "wrong-count-only": [("(count >= maxRequests || used + bytes > maxBytes)", "count >= maxRequests")],
        "wrong-retry-invalid": [('errorType == "invalid_request_error"', "false")],
        "wrong-missing-ignored": [('                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "missing"))\n                retry.add(cid)\n', "")],
        "wrong-usage-no-cache-reads": [('"cache_creation_input_tokens", "cache_read_input_tokens")', '"cache_creation_input_tokens")')],
    }),
}
