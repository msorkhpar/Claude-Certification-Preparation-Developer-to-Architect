# Planted wrong solutions of module 21-message-batches: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/21-message-batches/unit-01/practice-1"] = {
    "python": ("batches.py", {
        "wrong-by-position": [('        record = json.loads(line)\n        cid = record["custom_id"]\n', '        record = json.loads(line)\n        slot = len(by_id) + len(unknown)\n        cid = wanted[slot] if slot < len(wanted) else record["custom_id"]\n')],
        "wrong-duplicate-ids": [('        if custom_id in seen:\n            raise BatchError("custom_id", f"{custom_id} is used twice")\n', "")],
        "wrong-allow-stream": [('        if params.get("stream"):\n            raise BatchError("params.stream", "batch results are a file, not a stream")\n', ""), ('        if "speed" in params:\n            raise BatchError("params.speed", "fast mode is not available in a batch")\n', "")],
        "wrong-count-only": [("(len(current) >= max_requests or used + size > max_bytes)", "len(current) >= max_requests")],
        "wrong-retry-invalid": [('(fix if kind == "invalid_request_error" else retry).append(cid)', "retry.append(cid)")],
        "wrong-missing-ignored": [('        if result is None:\n            outcomes.append({"custom_id": cid, "status": "missing"})\n            retry.append(cid)\n', "        if result is None:\n            continue\n")],
    }),
    "typescript": ("batches.ts", {
        "wrong-by-position": [("    if (!wanted.includes(record.custom_id)) unknown.push(record.custom_id);\n    else if (!byId.has(record.custom_id)) byId.set(record.custom_id, record.result);", "    const slot = byId.size + unknown.length;\n    const target = slot < wanted.length ? wanted[slot] : record.custom_id;\n    if (!byId.has(target)) byId.set(target, record.result);")],
        "wrong-duplicate-ids": [('    if (seen.has(id)) throw new BatchError("custom_id", `${id} is used twice`);\n', "")],
        "wrong-allow-stream": [('    if (params.stream) throw new BatchError("params.stream", "batch results are a file, not a stream");\n', ""), ('    if ("speed" in params) throw new BatchError("params.speed", "fast mode is not available in a batch");\n', "")],
        "wrong-count-only": [("(current.length >= maxRequests || used + bytes > maxBytes)", "current.length >= maxRequests")],
        "wrong-retry-invalid": [('(kind === "invalid_request_error" ? fix : retry).push(cid);', "retry.push(cid);")],
        "wrong-missing-ignored": [('      outcomes.push({ custom_id: cid, status: "missing" });\n      retry.push(cid);\n', "      continue;\n")],
    }),
    "java": ("Batches.java", {
        "wrong-by-position": [('            String cid = (String) record.get("custom_id");\n', '            int slot = byId.size() + unknown.size();\n            String cid = slot < wanted.size() ? wanted.get(slot) : (String) record.get("custom_id");\n')],
        "wrong-duplicate-ids": [('            if (!seen.add(s)) throw new BatchError("custom_id", s + " is used twice");\n', "")],
        "wrong-allow-stream": [('            if (Boolean.TRUE.equals(params.get("stream"))) throw new BatchError("params.stream", "batch results are a file, not a stream");\n', ""), ('            if (params.containsKey("speed")) throw new BatchError("params.speed", "fast mode is not available in a batch");\n', "")],
        "wrong-count-only": [("(current.size() >= maxRequests || used + bytes > maxBytes)", "current.size() >= maxRequests")],
        "wrong-retry-invalid": [('(kind.equals("invalid_request_error") ? fix : retry).add(cid);', "retry.add(cid);")],
        "wrong-missing-ignored": [('                outcome.put("status", "missing");\n                retry.add(cid);\n', "                continue;\n")],
    }),
    "kotlin": ("Batches.kt", {
        "wrong-by-position": [('        val cid = record["custom_id"] as String\n', '        val slot = byId.size + unknown.size\n        val cid = if (slot < wanted.size) wanted[slot] else record["custom_id"] as String\n')],
        "wrong-duplicate-ids": [('        if (!seen.add(id)) throw BatchError("custom_id", "$id is used twice")\n', "")],
        "wrong-allow-stream": [('        if (params["stream"] == true) throw BatchError("params.stream", "batch results are a file, not a stream")\n', ""), ('        if (params.containsKey("speed")) throw BatchError("params.speed", "fast mode is not available in a batch")\n', "")],
        "wrong-count-only": [("(current.size >= maxRequests || used + bytes > maxBytes)", "current.size >= maxRequests")],
        "wrong-retry-invalid": [('(if (kind == "invalid_request_error") fix else retry).add(cid)', "retry.add(cid)")],
        "wrong-missing-ignored": [('                outcomes.add(linkedMapOf("custom_id" to cid, "status" to "missing"))\n                retry.add(cid)\n', "                continue\n")],
    }),
}
