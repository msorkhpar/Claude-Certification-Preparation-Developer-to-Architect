# Planted wrong solutions of module 22-claude-on-the-cloud-platforms: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/22-claude-on-the-cloud-platforms/unit-01/practice-1"] = {
    "python": ("platforms.py", {
        "wrong-vertex-model-in-body": [('    sent.pop("model", None)\n', "")],
        "wrong-vertex-version-header": [('    sent["anthropic_version"] = VERTEX_VERSION\n', ""), ('"headers": {"content-type": "application/json"}, "body": _vertex_body(body)}', '"headers": {"anthropic-version": VERTEX_VERSION, "content-type": "application/json"}, "body": _vertex_body(body)}')],
        "wrong-bedrock-direct-id": [('"model": _bedrock_model_id(family)}}', '"model": f"anthropic.{model}"}}')],
        "wrong-vertex-haiku-undated": [('return HAIKU_VERTEX if family == HAIKU else family', "return family")],
        "wrong-regional-any": [('    if family not in REGIONAL_VERTEX_MODELS:\n        raise PlatformError("endpoint", f"{model} is served on the global and multi-region endpoints, not on a specific region")\n', "")],
        "wrong-batches-everywhere": [('"bedrock": {"batches", "fast_mode",', '"bedrock": {"fast_mode",'), ('"vertex": {"batches", "fast_mode",', '"vertex": {"fast_mode",')],
        "wrong-body-mutated": [("    sent = copy.deepcopy(body)\n", "    sent = body\n")],
        "wrong-bedrock-any-model": [('    if family not in BEDROCK_MODELS:\n        raise PlatformError("model", f"{model} is not served by Claude in Amazon Bedrock")\n', "")],
        "wrong-region-optional": [('    if not config.get("region"):\n        raise PlatformError("config", "a Bedrock request needs a region")\n', "")],
    }),
    "typescript": ("platforms.ts", {
        "wrong-vertex-model-in-body": [("  delete sent.model;\n", "")],
        "wrong-vertex-version-header": [("  sent.anthropic_version = VERTEX_VERSION;\n", ""), ('headers: { "content-type": "application/json" }, body: vertexBody(body) }', 'headers: { "anthropic-version": VERTEX_VERSION, "content-type": "application/json" }, body: vertexBody(body) }')],
        "wrong-bedrock-direct-id": [("model: bedrockModelId(fam) },", "model: `anthropic.${model}` },")],
        "wrong-vertex-haiku-undated": [("return fam === HAIKU ? HAIKU_VERTEX : fam;", "return fam;")],
        "wrong-regional-any": [('  if (!REGIONAL_VERTEX_MODELS.has(fam)) throw new PlatformError("endpoint", `${model} is served on the global and multi-region endpoints, not on a specific region`);\n', "")],
        "wrong-batches-everywhere": [('bedrock: new Set(["batches", "fast_mode",', 'bedrock: new Set(["fast_mode",'), ('vertex: new Set(["batches", "fast_mode",', 'vertex: new Set(["fast_mode",')],
        "wrong-body-mutated": [("  const sent = structuredClone(body);\n  delete sent.model;", "  const sent = body;\n  delete sent.model;")],
        "wrong-bedrock-any-model": [('  if (!BEDROCK_MODELS.has(fam)) throw new PlatformError("model", `${model} is not served by Claude in Amazon Bedrock`);\n', "")],
        "wrong-region-optional": [('  if (!config.region) throw new PlatformError("config", "a Bedrock request needs a region");\n', "")],
    }),
    "java": ("Platforms.java", {
        "wrong-vertex-model-in-body": [('        sent.remove("model");\n', "")],
        "wrong-vertex-version-header": [('        sent.put("anthropic_version", VERTEX_VERSION);\n', ""), ('headers.put("content-type", "application/json");\n                return request("https://" + host', 'headers.put("anthropic-version", VERTEX_VERSION);\n                headers.put("content-type", "application/json");\n                return request("https://" + host')],
        "wrong-bedrock-direct-id": [('sent.put("model", bedrockModelId(family));', 'sent.put("model", "anthropic." + model);')],
        "wrong-vertex-haiku-undated": [("return family.equals(HAIKU) ? HAIKU_VERTEX : family;", "return family;")],
        "wrong-regional-any": [('        if (!REGIONAL_VERTEX_MODELS.contains(family)) {\n            throw new PlatformError("endpoint", model + " is served on the global and multi-region endpoints, not on a specific region");\n        }\n', "")],
        "wrong-batches-everywhere": [('"bedrock", Set.of("batches", "fast_mode",', '"bedrock", Set.of("fast_mode",'), ('"vertex", Set.of("batches", "fast_mode",', '"vertex", Set.of("fast_mode",')],
        "wrong-body-mutated": [("        Map<String, Object> sent = copyBody(body);\n        sent.remove(\"model\");", "        Map<String, Object> sent = body;\n        sent.remove(\"model\");")],
        "wrong-bedrock-any-model": [('        if (!BEDROCK_MODELS.contains(family)) throw new PlatformError("model", model + " is not served by Claude in Amazon Bedrock");\n', "")],
        "wrong-region-optional": [('        if (blank(config.get("region"))) throw new PlatformError("config", "a Bedrock request needs a region");\n', "")],
    }),
    "kotlin": ("Platforms.kt", {
        "wrong-vertex-model-in-body": [('    sent.remove("model")\n', "")],
        "wrong-vertex-version-header": [('    sent["anthropic_version"] = VERTEX_VERSION\n', ""), ('linkedMapOf("content-type" to "application/json"), vertexBody(body))', 'linkedMapOf("anthropic-version" to VERTEX_VERSION, "content-type" to "application/json"), vertexBody(body))')],
        "wrong-bedrock-direct-id": [('sent["model"] = bedrockModelId(family)', 'sent["model"] = "anthropic.$model"')],
        "wrong-vertex-haiku-undated": [("if (family == HAIKU) HAIKU_VERTEX else family", "family")],
        "wrong-regional-any": [('        if (family !in REGIONAL_VERTEX_MODELS) throw PlatformError("endpoint", "$model is served on the global and multi-region endpoints, not on a specific region")\n', "")],
        "wrong-batches-everywhere": [('"bedrock" to setOf("batches", "fast_mode",', '"bedrock" to setOf("fast_mode",'), ('"vertex" to setOf("batches", "fast_mode",', '"vertex" to setOf("fast_mode",')],
        "wrong-body-mutated": [("    val sent = copyBody(body)\n    sent.remove(\"model\")", "    val sent = body as MutableMap<String, Any?>\n    sent.remove(\"model\")")],
        "wrong-bedrock-any-model": [('    if (family !in BEDROCK_MODELS) throw PlatformError("model", "$model is not served by Claude in Amazon Bedrock")\n', "")],
        "wrong-region-optional": [('    if ((config["region"] as String?).isNullOrEmpty()) throw PlatformError("config", "a Bedrock request needs a region")\n', "")],
    }),
}
