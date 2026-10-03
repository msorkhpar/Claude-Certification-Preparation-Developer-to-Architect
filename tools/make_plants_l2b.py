#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 2 practices of modules 18 to 23 from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/make_plants_l2b.py
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> language -> (main file, {plant: [(old, new), ...]})
PLANTS = {}

# --- PLANTS BELOW ---

PLANTS[f"{X}/18-model-choice-cost-and-migration/unit-01/practice-1"] = {
    "python": ("router.py", {
        "wrong-flat-cache-read": [('usage.get("cache_read_input_tokens", 0) * price * model["cache_read_multiplier"]', 'usage.get("cache_read_input_tokens", 0) * price * 0.1')],
        "wrong-batch-output-only": [('+ usage.get("output_tokens", 0) * model["output"])\n    if batch:\n        total *= 0.5\n', '+ usage.get("output_tokens", 0) * model["output"] * (0.5 if batch else 1))\n')],
        "wrong-first-eligible": [('best = min(eligible, key=lambda m: (request_cost(m, usage, task.get("batch", False)), m["tier"], m["id"]))', "best = eligible[0]")],
        "wrong-ignore-context": [('\n                and _input_tokens(usage) <= m["context"]\n                and wanted <= m["max_output"]]', "]")],
        "wrong-tie-high-tier": [('m["tier"], m["id"]))', '-m["tier"], m["id"]))')],
    }),
    "typescript": ("router.ts", {
        "wrong-flat-cache-read": [("(usage.cache_read_input_tokens ?? 0) * price * model.cache_read_multiplier", "(usage.cache_read_input_tokens ?? 0) * price * 0.1")],
        "wrong-batch-output-only": [("(usage.output_tokens ?? 0) * model.output;\n  if (batch) total *= 0.5;", "(usage.output_tokens ?? 0) * model.output * (batch ? 0.5 : 1);")],
        "wrong-first-eligible": [("return scored[0].m.id;", "return eligible[0].id;")],
        "wrong-ignore-context": [(" && inputTokens(task.usage) <= m.context && wanted <= m.max_output,", ",")],
        "wrong-tie-high-tier": [("a.m.tier - b.m.tier ||", "b.m.tier - a.m.tier ||")],
    }),
    "java": ("Router.java", {
        "wrong-flat-cache-read": [('* ((Number) model.get("cache_read_multiplier")).doubleValue()', "* 0.1")],
        "wrong-batch-output-only": [('+ n(usage, "output_tokens") * ((Number) model.get("output")).doubleValue();\n        if (batch) total *= 0.5;', '+ n(usage, "output_tokens") * ((Number) model.get("output")).doubleValue() * (batch ? 0.5 : 1.0);')],
        "wrong-first-eligible": [('.min(Comparator.<Map<String, Object>>comparingDouble(m -> requestCost(m, usage, batch))\n                        .thenComparingLong(m -> n(m, "tier"))\n                        .thenComparing(m -> (String) m.get("id")))', ".findFirst()")],
        "wrong-ignore-context": [('                .filter(m -> inputTokens(usage) <= n(m, "context"))\n                .filter(m -> wanted <= n(m, "max_output"))\n', "")],
        "wrong-tie-high-tier": [('.thenComparingLong(m -> n(m, "tier"))', '.thenComparingLong(m -> -n(m, "tier"))')],
    }),
    "kotlin": ("Router.kt", {
        "wrong-flat-cache-read": [('* (model["cache_read_multiplier"] as Number).toDouble() +', "* 0.1 +")],
        "wrong-batch-output-only": [('n(usage, "output_tokens") * (model["output"] as Number).toDouble()\n    if (batch) total *= 0.5', 'n(usage, "output_tokens") * (model["output"] as Number).toDouble() * (if (batch) 0.5 else 1.0)')],
        "wrong-first-eligible": [('.minWithOrNull(compareBy<Map<String, Any?>> { requestCost(it, usage, batch) }.thenBy { n(it, "tier") }.thenBy { it["id"] as String })', ".firstOrNull()")],
        "wrong-ignore-context": [(' && inputTokens(usage) <= n(it, "context") && wanted <= n(it, "max_output") }', " }")],
        "wrong-tie-high-tier": [('.thenBy { n(it, "tier") }', ".thenByDescending { n(it, \"tier\") }")],
    }),
}
PLANTS[f"{X}/19-thinking-effort-and-speed/unit-01/practice-1"] = {
    "python": ("params.py", {
        "wrong-allow-disabled": [('            if not haiku:\n                raise RejectedRequest("thinking", "thinking cannot be turned off on this model")\n', "")],
        "wrong-between-any-effort": [('            if (effort or DEFAULT_EFFORT[family]) in ("xhigh", "max"):\n                raise RejectedRequest("thinking", "between_tools works at low, medium and high effort only")\n', "")],
        "wrong-effort-on-haiku": [('        if haiku:\n            raise RejectedRequest("output_config.effort", "this model does not support effort")\n', "")],
        "wrong-sampling-pass-through": [('        if not haiku and not (name == "temperature" and value == 1.0):\n            raise RejectedRequest(name, "this model rejects a non-default value")\n', "")],
        "wrong-fast-in-batch": [('        if options.get("batch"):\n            raise RejectedRequest("speed", "fast mode is not available in a batch")\n', "")],
        "wrong-budget-floor": [("budget < 1024 or", "budget < 1000 or")],
        "wrong-effort-in-thinking": [('params["thinking"] = {"type": "adaptive"}', 'params["thinking"] = {"type": "adaptive", **({"effort": effort} if effort else {})}')],
    }),
    "typescript": ("params.ts", {
        "wrong-allow-disabled": [('      if (!haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");\n', "")],
        "wrong-between-any-effort": [('      const level = effort ?? DEFAULT_EFFORT[fam];\n      if (level === "xhigh" || level === "max") throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");\n', "")],
        "wrong-effort-on-haiku": [('    if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");\n', "")],
        "wrong-sampling-pass-through": [('    if (!haiku && !(name === "temperature" && value === 1.0)) throw new RejectedRequest(name, "this model rejects a non-default value");\n', "")],
        "wrong-fast-in-batch": [('    if (options.batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");\n', "")],
        "wrong-budget-floor": [("budget < 1024 ||", "budget < 1000 ||")],
        "wrong-effort-in-thinking": [('params.thinking = { type: "adaptive" };', 'params.thinking = { type: "adaptive", ...(effort ? { effort } : {}) };')],
    }),
    "java": ("Params.java", {
        "wrong-allow-disabled": [('                if (!haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");\n', "")],
        "wrong-between-any-effort": [('                String level = effort != null ? effort : DEFAULT_EFFORT.get(family);\n                if (level.equals("xhigh") || level.equals("max")) {\n                    throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");\n                }\n', "")],
        "wrong-effort-on-haiku": [('            if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");\n', "")],
        "wrong-sampling-pass-through": [('            if (!haiku && !defaultTemperature) throw new RejectedRequest(name, "this model rejects a non-default value");\n', "")],
        "wrong-fast-in-batch": [('            if (Boolean.TRUE.equals(options.get("batch"))) throw new RejectedRequest("speed", "fast mode is not available in a batch");\n', "")],
        "wrong-budget-floor": [("budget.longValue() < 1024", "budget.longValue() < 1000")],
        "wrong-effort-in-thinking": [('params.put("thinking", Map.of("type", "adaptive"));', 'params.put("thinking", effort == null ? Map.of("type", "adaptive") : Map.of("type", "adaptive", "effort", effort));')],
    }),
    "kotlin": ("Params.kt", {
        "wrong-allow-disabled": [('                if (!haiku) throw RejectedRequest("thinking", "thinking cannot be turned off on this model")\n', "")],
        "wrong-between-any-effort": [('                val level = effort ?: DEFAULT_EFFORT.getValue(family)\n                if (level == "xhigh" || level == "max") throw RejectedRequest("thinking", "between_tools works at low, medium and high effort only")\n', "")],
        "wrong-effort-on-haiku": [('        if (haiku) throw RejectedRequest("output_config.effort", "this model does not support effort")\n', "")],
        "wrong-sampling-pass-through": [('        if (!haiku && !defaultTemperature) throw RejectedRequest(name, "this model rejects a non-default value")\n', "")],
        "wrong-fast-in-batch": [('        if (options["batch"] == true) throw RejectedRequest("speed", "fast mode is not available in a batch")\n', "")],
        "wrong-budget-floor": [("budget < 1024 ||", "budget < 1000 ||")],
        "wrong-effort-in-thinking": [('params["thinking"] = mapOf("type" to "adaptive")', 'params["thinking"] = if (effort == null) mapOf("type" to "adaptive") else mapOf("type" to "adaptive", "effort" to effort)')],
    }),
}

PLANTS[f"{X}/20-prompt-caching/unit-01/practice-1"] = {
    "python": ("cacheplan.py", {
        "wrong-no-reorder": [('    ordered = sorted(blocks, key=lambda b: SECTIONS[b["section"]])  # stable inside a section\n    ordered = [b for b in ordered if not b.get("volatile", False)] + [b for b in ordered if b.get("volatile", False)]\n', "    ordered = list(blocks)\n")],
        "wrong-volatile-first": [('ordered = [b for b in ordered if not b.get("volatile", False)] + [b for b in ordered if b.get("volatile", False)]', 'ordered = [b for b in ordered if b.get("volatile", False)] + [b for b in ordered if not b.get("volatile", False)]')],
        "wrong-per-block-minimum": [('wanted = block.get("breakpoint", False) and not volatile and total >= min_tokens', 'wanted = block.get("breakpoint", False) and not volatile and block["tokens"] >= min_tokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('        elif seen_five:\n            raise PlanError("a 1h breakpoint must come before every 5m breakpoint")\n', "")],
        "wrong-volatile-cached": [('block.get("breakpoint", False) and not volatile and total', 'block.get("breakpoint", False) and total')],
    }),
    "typescript": ("cacheplan.ts", {
        "wrong-no-reorder": [('  const bySection = [...blocks].sort((a, b) => SECTIONS[a.section] - SECTIONS[b.section]); // sort is stable\n  const ordered = [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];\n', "  const ordered = [...blocks];\n")],
        "wrong-volatile-first": [("const ordered = [...bySection.filter((b) => !b.volatile), ...bySection.filter((b) => b.volatile)];", "const ordered = [...bySection.filter((b) => b.volatile), ...bySection.filter((b) => !b.volatile)];")],
        "wrong-per-block-minimum": [("Boolean(b.breakpoint) && !b.volatile && total >= minTokens", "Boolean(b.breakpoint) && !b.volatile && b.tokens >= minTokens")],
        "wrong-five-breakpoints": [("const MAX_BREAKPOINTS = 4;", "const MAX_BREAKPOINTS = 5;")],
        "wrong-ttl-unchecked": [('    else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");\n', "")],
        "wrong-volatile-cached": [("Boolean(b.breakpoint) && !b.volatile && total", "Boolean(b.breakpoint) && total")],
    }),
    "java": ("CachePlan.java", {
        "wrong-no-reorder": [('        List<Map<String, Object>> bySection = new ArrayList<>(blocks);\n        bySection.sort(Comparator.comparingInt(b -> SECTIONS.get((String) b.get("section")))); // List.sort is stable\n        List<Map<String, Object>> ordered = new ArrayList<>();\n        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(ordered::add);\n        bySection.stream().filter(b -> flag(b, "volatile")).forEach(ordered::add);\n', "        List<Map<String, Object>> ordered = new ArrayList<>(blocks);\n")],
        "wrong-volatile-first": [('        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(ordered::add);\n        bySection.stream().filter(b -> flag(b, "volatile")).forEach(ordered::add);', '        bySection.stream().filter(b -> flag(b, "volatile")).forEach(ordered::add);\n        bySection.stream().filter(b -> !flag(b, "volatile")).forEach(ordered::add);')],
        "wrong-per-block-minimum": [('flag(b, "breakpoint") && !isVolatile && total >= minTokens', 'flag(b, "breakpoint") && !isVolatile && ((Number) b.get("tokens")).longValue() >= minTokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('            else if (seenFive) throw new PlanError("a 1h breakpoint must come before every 5m breakpoint");\n', "")],
        "wrong-volatile-cached": [('flag(b, "breakpoint") && !isVolatile && total', 'flag(b, "breakpoint") && total')],
    }),
    "kotlin": ("CachePlan.kt", {
        "wrong-no-reorder": [('    val bySection = blocks.sortedBy { SECTIONS.getValue(it["section"] as String) } // sortedBy is stable\n    val ordered = bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }\n', "    val ordered = blocks\n")],
        "wrong-volatile-first": [('val ordered = bySection.filter { it["volatile"] != true } + bySection.filter { it["volatile"] == true }', 'val ordered = bySection.filter { it["volatile"] == true } + bySection.filter { it["volatile"] != true }')],
        "wrong-per-block-minimum": [('b["breakpoint"] == true && !isVolatile && total >= minTokens', 'b["breakpoint"] == true && !isVolatile && (b["tokens"] as Number).toLong() >= minTokens')],
        "wrong-five-breakpoints": [("MAX_BREAKPOINTS = 4", "MAX_BREAKPOINTS = 5")],
        "wrong-ttl-unchecked": [('        else if (seenFive) throw PlanError("a 1h breakpoint must come before every 5m breakpoint")\n', "")],
        "wrong-volatile-cached": [('b["breakpoint"] == true && !isVolatile && total', 'b["breakpoint"] == true && total')],
    }),
}

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

_P23 = {
    "wrong-wildcard-action": {"bedrock-policy.json": [('"bedrock-mantle:CreateInference"', '"bedrock-mantle:*"')]},
    "wrong-resource-star": {"bedrock-policy.json": [('"arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"', '"*"')]},
    "wrong-region-wildcard": {"bedrock-policy.json": [("arn:aws:bedrock:us-east-1::", "arn:aws:bedrock:*::")]},
    "wrong-old-policy-version": {"bedrock-policy.json": [('"2012-10-17"', '"2008-10-17"')]},
    "wrong-predefined-role": {"vertex.json": [('"projects/example-project/roles/claudeInvoker"', '"roles/aiplatform.user"')]},
    "wrong-extra-permission": {"vertex.json": [('["aiplatform.endpoints.predict"]', '["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]')]},
    "wrong-global-for-eu": {"vertex.json": [('"endpoint": "eu"', '"endpoint": "global"')]},
    "wrong-regional-new-model": {"vertex.json": [('"endpoint": "eu"', '"endpoint": "europe-west1"')]},
    "wrong-bare-model-id": {"bedrock-policy.json": [("foundation-model/anthropic.claude-sonnet-5-5", "foundation-model/claude-sonnet-5-5")]},
    "wrong-quota-over": {"quotas.json": [('"input_tpm": 4000000', '"input_tpm": 8000000')]},
}
PLANTS[f"{X}/23-setting-up-claude-on-the-cloud-platforms/unit-01/practice-1"] = {
    lang: ("bedrock-policy.json", dict(_P23)) for lang in ("python", "typescript", "java", "kotlin")
}

# --- PLANTS END ---


def main():
    made = 0
    for practice, langs in PLANTS.items():
        cases = json.loads((ROOT / practice / "cases.json").read_text())
        for lang, (fname, plants) in langs.items():
            if sorted(plants) != sorted(cases["plants"]):
                sys.exit(f"{practice}/{lang}: plants {sorted(plants)} differ from cases.json {sorted(cases['plants'])}")
            ref = ROOT / practice / lang / "reference"
            for name, edits in plants.items():
                dest = ROOT / practice / lang / name
                if dest.exists():
                    shutil.rmtree(dest)
                shutil.copytree(ref, dest)
                by_file = edits if isinstance(edits, dict) else {fname: edits}
                for target, pairs in by_file.items():
                    f = dest / target
                    text = f.read_text()
                    for old, new in pairs:
                        if old not in text:
                            sys.exit(f"{practice}/{lang}/{name}: pattern not found: {old!r}")
                        text = text.replace(old, new, 1)
                    if text == (ref / target).read_text():
                        sys.exit(f"{practice}/{lang}/{name}: plant equals the reference")
                    f.write_text(text)
                made += 1
    print(f"made {made} planted wrong solutions")


if __name__ == "__main__":
    main()
