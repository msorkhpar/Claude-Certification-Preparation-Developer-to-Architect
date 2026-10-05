# Planted wrong solutions of module 22-claude-on-the-cloud-platforms: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/22-claude-on-the-cloud-platforms/unit-01/practice-1"] = {
    "python": ("platforms.py", {
        "wrong-vertex-model-in-body": [('        sent.pop("model", None)\n', "")],
        "wrong-vertex-version-header": [('        sent["anthropic_version"] = VERTEX_VERSION\n        return {"method": "POST", "url": f"https://{host}{path}", "headers": {"content-type": "application/json"}, "body": sent}', '        return {"method": "POST", "url": f"https://{host}{path}", "headers": {"anthropic-version": VERTEX_VERSION, "content-type": "application/json"}, "body": sent}')],
        "wrong-bedrock-direct-id": [('"body": {**copy.deepcopy(body), "model": f"anthropic.{family}"}}', '"body": {**copy.deepcopy(body), "model": model}}')],
        "wrong-regional-any": [('            if family not in REGIONAL_VERTEX_MODELS:\n                raise PlatformError("endpoint", f"{model} is served on the global and multi-region endpoints, not on a specific region")\n', "")],
        "wrong-batches-everywhere": [('"bedrock": {"batches", "fast_mode",', '"bedrock": {"fast_mode",'), ('"vertex": {"batches", "fast_mode",', '"vertex": {"fast_mode",')],
        "wrong-body-mutated": [('        sent = copy.deepcopy(body)\n        sent.pop("model", None)', '        sent = body\n        sent.pop("model", None)')],
    }),
    "typescript": ("platforms.ts", {
        "wrong-vertex-model-in-body": [("    delete sent.model;\n", "")],
        "wrong-vertex-version-header": [('    sent.anthropic_version = VERTEX_VERSION;\n    return { method: "POST", url: `https://${host}${path}`, headers: { "content-type": "application/json" }, body: sent };', '    return { method: "POST", url: `https://${host}${path}`, headers: { "anthropic-version": VERTEX_VERSION, "content-type": "application/json" }, body: sent };')],
        "wrong-bedrock-direct-id": [("body: { ...structuredClone(body), model: `anthropic.${fam}` },", "body: { ...structuredClone(body), model },")],
        "wrong-regional-any": [('      if (!REGIONAL_VERTEX_MODELS.has(fam)) throw new PlatformError("endpoint", `${model} is served on the global and multi-region endpoints, not on a specific region`);\n', "")],
        "wrong-batches-everywhere": [('bedrock: new Set(["batches", "fast_mode",', 'bedrock: new Set(["fast_mode",'), ('vertex: new Set(["batches", "fast_mode",', 'vertex: new Set(["fast_mode",')],
        "wrong-body-mutated": [("    const sent = structuredClone(body);\n    delete sent.model;", "    const sent = body;\n    delete sent.model;")],
    }),
    "java": ("Platforms.java", {
        "wrong-vertex-model-in-body": [('                sent.remove("model");\n', "")],
        "wrong-vertex-version-header": [('                sent.put("anthropic_version", VERTEX_VERSION);\n                Map<String, Object> headers = new LinkedHashMap<>();\n', '                Map<String, Object> headers = new LinkedHashMap<>();\n                headers.put("anthropic-version", VERTEX_VERSION);\n')],
        "wrong-bedrock-direct-id": [('sent.put("model", "anthropic." + family);', 'sent.put("model", model);')],
        "wrong-regional-any": [('                    if (!REGIONAL_VERTEX_MODELS.contains(family)) {\n                        throw new PlatformError("endpoint", model + " is served on the global and multi-region endpoints, not on a specific region");\n                    }\n', "")],
        "wrong-batches-everywhere": [('"bedrock", Set.of("batches", "fast_mode",', '"bedrock", Set.of("fast_mode",'), ('"vertex", Set.of("batches", "fast_mode",', '"vertex", Set.of("fast_mode",')],
        "wrong-body-mutated": [('                Map<String, Object> sent = copyBody(body);\n                sent.remove("model");', '                Map<String, Object> sent = body;\n                sent.remove("model");')],
    }),
    "kotlin": ("Platforms.kt", {
        "wrong-vertex-model-in-body": [('            sent.remove("model")\n', "")],
        "wrong-vertex-version-header": [('            sent["anthropic_version"] = VERTEX_VERSION\n            return request("https://$host$path", linkedMapOf("content-type" to "application/json"), sent)', '            return request("https://$host$path", linkedMapOf("anthropic-version" to VERTEX_VERSION, "content-type" to "application/json"), sent)')],
        "wrong-bedrock-direct-id": [('sent["model"] = "anthropic.$family"', 'sent["model"] = model')],
        "wrong-regional-any": [('                    if (family !in REGIONAL_VERTEX_MODELS) throw PlatformError("endpoint", "$model is served on the global and multi-region endpoints, not on a specific region")\n', "")],
        "wrong-batches-everywhere": [('"bedrock" to setOf("batches", "fast_mode",', '"bedrock" to setOf("fast_mode",'), ('"vertex" to setOf("batches", "fast_mode",', '"vertex" to setOf("fast_mode",')],
        "wrong-body-mutated": [('            val sent = copyBody(body)\n            sent.remove("model")', '            @Suppress("UNCHECKED_CAST") val sent = body as MutableMap<String, Any?>\n            sent.remove("model")')],
    }),
}
