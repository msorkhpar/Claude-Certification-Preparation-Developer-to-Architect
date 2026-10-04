#!/usr/bin/env python3
"""Materialise the planted wrong solutions of every practice from their reference solutions.

A plant is never committed as files. Each plant is an ordered list of exact replacements against the
practice's reference (PLANTS below, one section per module); this tool copies the reference into
<language>/<plant name>/ and applies the replacements. It fails when a pattern is missing or occurs more
than once in the text it is applied to, when a plant equals the reference, or when the plants of a
language differ from the ones the practice's cases.json names. The generated plant folders are git-ignored.

usage: tools/make_plants.py [--modules REGEX] [--out DIR] [--list]
  --modules REGEX   only the practices whose module folder name matches (default: all), for example '^(1[89]|2[0-3])-'
  --out DIR         write <DIR>/<practice>/<language>/<plant>/ instead of next to the reference
  --list            print one line per plant (practice, language, plant) and write nothing
"""
import argparse
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> language -> (main file, {plant: [(old, new), ...]}); a plant may instead map several files to their lists
PLANTS = {}


def both(file, plants):
    """The same edits for the Python and the TypeScript folder: a project set-up is the same files in both."""
    return {"python": (file, plants), "typescript": (file, plants)}


# ===== Level 1: module 6 =====
PLANTS[f"{X}/06-prompting-fundamentals/unit-01/practice-1"] = {
    "python": ("prompt_builder.py", {
        "wrong-task-first": [("    parts = []\n", '    parts = [_block("task", _fill(task, variables))]\n'),
                              ('    parts.append(_block("task", _fill(task, variables)))\n', "")],
        "wrong-empty-sections": [('    if _present(spec.get("role")):', '    if "role" in spec:')],
        "wrong-missing-variable-silent": [('            raise ValueError(f"missing variable: {name}")', "            return match.group(0)")],
        "wrong-blank-task-accepted": [("    if not _present(task):", "    if task is None:")],
        "wrong-no-escape": [('{_escape(doc["text"])}', '{doc["text"]}')],
        "wrong-fill-documents": [('{_escape(doc["text"])}', '{_escape(_fill(doc["text"], variables))}')],
    }),
    "typescript": ("promptBuilder.ts", {
        "wrong-task-first": [("  const parts: string[] = [];\n", '  const parts: string[] = [block("task", fill(spec.task, variables))];\n'),
                              ('  parts.push(block("task", fill(spec.task, variables)));\n', "")],
        "wrong-empty-sections": [("  if (present(spec.role)) parts.push(", "  if (spec.role !== undefined && spec.role !== null) parts.push(")],
        "wrong-missing-variable-silent": [("if (!(name in variables)) throw new Error(`missing variable: ${name}`);", "if (!(name in variables)) return _m;")],
        "wrong-blank-task-accepted": [("  if (!present(spec.task)) throw new Error", "  if (spec.task === null || spec.task === undefined) throw new Error")],
        "wrong-no-escape": [("${escapeText(doc.text)}", "${doc.text}")],
        "wrong-fill-documents": [("${escapeText(doc.text)}", "${escapeText(fill(doc.text, variables))}")],
    }),
    "java": ("PromptBuilder.java", {
        "wrong-task-first": [("        List<String> parts = new ArrayList<>();\n", '        List<String> parts = new ArrayList<>();\n        parts.add(block("task", fill(spec.task(), variables)));\n'),
                              ('        parts.add(block("task", fill(spec.task(), variables)));\n        return', "        return")],
        "wrong-empty-sections": [("        if (present(spec.role())) parts.add(", "        if (spec.role() != null) parts.add(")],
        "wrong-missing-variable-silent": [('if (!variables.containsKey(name)) throw new IllegalArgumentException("missing variable: " + name);',
                                           "if (!variables.containsKey(name)) { m.appendReplacement(out, Matcher.quoteReplacement(m.group())); continue; }")],
        "wrong-blank-task-accepted": [("if (!present(spec.task())) throw", "if (spec.task() == null) throw")],
        "wrong-no-escape": [('+ escape(doc.text()) +', '+ doc.text() +')],
        "wrong-fill-documents": [('+ escape(doc.text()) +', '+ escape(fill(doc.text(), variables)) +')],
    }),
    "kotlin": ("PromptBuilder.kt", {
        "wrong-task-first": [("    val parts = mutableListOf<String>()\n", '    val parts = mutableListOf(block("task", fill(spec.task!!, variables)))\n'),
                              ('    parts += block("task", fill(spec.task!!, variables))\n', "")],
        "wrong-empty-sections": [("    if (present(spec.role)) parts +=", "    if (spec.role != null) parts +=")],
        "wrong-missing-variable-silent": [('        require(name in variables) { "missing variable: $name" }\n        variables.getValue(name)', "        variables[name] ?: m.value")],
        "wrong-blank-task-accepted": [("require(present(spec.task))", "require(spec.task != null)")],
        "wrong-no-escape": [("${escape(doc.text)}", "${doc.text}")],
        "wrong-fill-documents": [("${escape(doc.text)}", "${escape(fill(doc.text, variables))}")],
    }),
}


# ===== Level 2: modules 13 to 17 =====
PLANTS.update({
    f"{X}/13-one-rest-api-under-every-sdk/unit-01/practice-1": {
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
    },
    f"{X}/14-the-messages-api/unit-01/practice-1": {
        "python": ("conversation.py", {
            "wrong-send-last-only": [('"messages": copy.deepcopy(self._history)}', '"messages": copy.deepcopy(self._history[-1:])}')],
            "wrong-no-rollback": [("            self._history.pop()\n            raise", "            raise")],
            "wrong-shared-list": [('"messages": copy.deepcopy(self._history)}', '"messages": self._history}')],
        }),
        "typescript": ("conversation.ts", {
            "wrong-send-last-only": [("messages: structuredClone(this._history)", "messages: structuredClone(this._history.slice(-1))")],
            "wrong-no-rollback": [("      this._history.pop();\n      throw err;", "      throw err;")],
            "wrong-shared-list": [("messages: structuredClone(this._history)", "messages: this._history")],
        }),
        "java": ("Conversation.java", {
            "wrong-send-last-only": [('body.put("messages", copy(history));', 'body.put("messages", copy(history.subList(history.size() - 1, history.size())));')],
            "wrong-no-rollback": [("            history.remove(history.size() - 1);\n            throw e;", "            throw e;")],
            "wrong-shared-list": [('body.put("messages", copy(history));', 'body.put("messages", history);')],
        }),
        "kotlin": ("Conversation.kt", {
            "wrong-send-last-only": [('"messages" to copy(history))', '"messages" to copy(history.takeLast(1)))')],
            "wrong-no-rollback": [("            history.removeAt(history.size - 1)\n            throw e", "            throw e")],
            "wrong-shared-list": [('"messages" to copy(history))', '"messages" to history)')],
        }),
    },
    f"{X}/15-errors-retries-and-timeouts/unit-01/practice-1": {
        "python": ("retry.py", {
            "wrong-retry-everything": [("    if response.status in RETRYABLE or response.status >= 500:", "    if response.status >= 400:")],
            "wrong-ignore-retry-after": [("            wait = _retry_after(response)", "            wait = 0.0")],
            "wrong-no-cap": [("jitter(min(cap, base_delay * 2 ** (attempt - 1)))", "jitter(base_delay * 2 ** (attempt - 1))")],
            "wrong-sleep-after-last": [("            if attempt >= max_attempts:\n                raise _failure(response, attempt)",
                                        "            if attempt >= max_attempts:\n                sleep(base_delay)\n                raise _failure(response, attempt)")],
        }),
        "typescript": ("retry.ts", {
            "wrong-retry-everything": [("if (response.status === 408 || response.status === 409 || response.status === 429 || response.status >= 500) {", "if (response.status >= 400) {")],
            "wrong-ignore-retry-after": [("      wait = retryAfter(response);", "      wait = 0;")],
            "wrong-no-cap": [("Math.min(cap, baseDelay * 2 ** (attempt - 1))", "baseDelay * 2 ** (attempt - 1)")],
            "wrong-sleep-after-last": [("      if (!retryable(response) || attempt >= maxAttempts) throw failure(response, attempt);",
                                        "      if (!retryable(response)) throw failure(response, attempt);\n      if (attempt >= maxAttempts) {\n        sleep(baseDelay);\n        throw failure(response, attempt);\n      }")],
        }),
        "java": ("Retry.java", {
            "wrong-retry-everything": [("if (s == 408 || s == 409 || s == 429 || s >= 500) {", "if (s >= 400) {")],
            "wrong-ignore-retry-after": [("                wait = retryAfter(response);", "                wait = 0.0;")],
            "wrong-no-cap": [("Math.min(policy.cap(), policy.baseDelay() * Math.pow(2, attempt - 1))", "policy.baseDelay() * Math.pow(2, attempt - 1)")],
            "wrong-sleep-after-last": [("                if (!retryable(response) || attempt >= policy.maxAttempts()) throw failure(response, attempt);",
                                        "                if (!retryable(response)) throw failure(response, attempt);\n                if (attempt >= policy.maxAttempts()) {\n                    sleep.sleep(policy.baseDelay());\n                    throw failure(response, attempt);\n                }")],
        }),
        "kotlin": ("Retry.kt", {
            "wrong-retry-everything": [("if (s == 408 || s == 409 || s == 429 || s >= 500) {", "if (s >= 400) {")],
            "wrong-ignore-retry-after": [("            wait = retryAfter(response)", "            wait = 0.0")],
            "wrong-no-cap": [("minOf(policy.cap, policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble()))", "policy.baseDelay * Math.pow(2.0, (attempt - 1).toDouble())")],
            "wrong-sleep-after-last": [("            if (!retryable(response) || attempt >= policy.maxAttempts) throw failure(response, attempt)",
                                        "            if (!retryable(response)) throw failure(response, attempt)\n            if (attempt >= policy.maxAttempts) {\n                sleep(policy.baseDelay)\n                throw failure(response, attempt)\n            }")],
        }),
    },
    f"{X}/16-async-concurrency-and-backpressure/unit-01/practice-1": {
        "python": ("bounded.py", {
            "wrong-unbounded": [("range(limit)", "range(1000)")],
            "wrong-fail-fast": [("except Exception as err:  # noqa: BLE001 - one failure must not stop the others", "except KeyboardInterrupt as err:")],
            "wrong-completion-order": [("            results[index] = outcome", "            results.remove(None)\n            results.append(outcome)")],
            "wrong-eager-input": [("source = iter(items)", "source = iter(list(items))")],
        }),
        "typescript": ("bounded.ts", {
            "wrong-unbounded": [("Array.from({ length: limit }, worker)", "Array.from({ length: 1000 }, worker)")],
            "wrong-fail-fast": [("outcome = { ok: false, error }; // one failure must not stop the others", "throw error;")],
            "wrong-completion-order": [("      results[index] = outcome;", "      results.splice(results.indexOf(undefined as any), 1);\n      results.push(outcome);")],
            "wrong-eager-input": [("const source = items[Symbol.iterator]();", "const source = [...items][Symbol.iterator]();")],
        }),
        "java": ("Bounded.java", {
            "wrong-unbounded": [("for (int i = 0; i < limit; i++) {", "for (int i = 0; i < 64; i++) {")],
            "wrong-fail-fast": [("        return results;\n", "        for (Outcome<R> o : results) {\n            if (!o.ok()) throw new RuntimeException(o.error());\n        }\n        return results;\n")],
            "wrong-completion-order": [("                        results.set(index, outcome);", "                        results.remove(null);\n                        results.add(outcome);")],
            "wrong-eager-input": [("mapBounded(Iterator<T> items,", "mapBounded(Iterator<T> source,"),
                                  ("        List<Outcome<R>> results = new ArrayList<>(); // one slot",
                                   "        List<T> all = new ArrayList<>();\n        source.forEachRemaining(all::add);\n        Iterator<T> items = all.iterator();\n        List<Outcome<R>> results = new ArrayList<>(); // one slot")],
        }),
        "kotlin": ("Bounded.kt", {
            "wrong-unbounded": [("val workers = List(limit) {", "val workers = List(64) {")],
            "wrong-fail-fast": [("    return results.map { it!! }", "    results.firstOrNull { !it!!.ok }?.let { throw RuntimeException(it.error) }\n    return results.map { it!! }")],
            "wrong-completion-order": [("synchronized(lock) { results[index] = outcome }", "synchronized(lock) { results.remove(null); results.add(outcome) }")],
            "wrong-eager-input": [("fun <T, R> mapBounded(items: Iterator<T>,", "fun <T, R> mapBounded(source: Iterator<T>,"),
                                  ('    require(limit >= 1) { "limit must be at least 1" }\n', '    require(limit >= 1) { "limit must be at least 1" }\n    val items = source.asSequence().toList().iterator()\n')],
        }),
    },
    f"{X}/17-streaming/unit-01/practice-1": {
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
    },
})


# ===== Level 2: modules 18 to 23 =====
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


# ===== Level 2: modules 25 to 29 =====
PLANTS[f"{X}/25-structured-output-and-defensive-parsing/unit-01/practice-1"] = {
    "python": ("extractor.py", {
        "wrong-no-prose-skip": [('first, last = body.find("{"), body.rfind("}")', "first, last = 0, len(body) - 1")],
        "wrong-generic-retry": [('    lines = "\\n".join(f"- {e[\'path\']}: {e[\'message\']}" for e in errors)\n    return f"Your reply was rejected:\\n{lines}\\nReturn the corrected JSON only."', '    return "Your reply was rejected. Return the corrected JSON only."')],
        "wrong-extra-attempt": [("for attempt in range(1, max_attempts + 1):", "for attempt in range(1, max_attempts + 2):")],
        "wrong-retry-refusal": [('        if reply.get("stop_reason") == "refusal":\n            return {"status": "refused", "value": None, "attempts": attempt, "errors": []}\n', "")],
        "wrong-no-grounding": [('            for name in evidence_fields:\n                quoted = value.get(name) if isinstance(value, dict) else None\n                if isinstance(quoted, str) and quoted not in document:\n                    errors.append({"path": f"$.{name}", "message": "is not found in the document"})\n', "")],
        "wrong-integer-allows-fraction": [("    return (isinstance(value, int) and not isinstance(value, bool)) or (isinstance(value, float) and value.is_integer())", "    return _is_number(value)")],
    }),
    "typescript": ("extractor.ts", {
        "wrong-no-prose-skip": [('const first = body.indexOf("{");\n  const last = body.lastIndexOf("}");', "const first = 0;\n  const last = body.length - 1;")],
        "wrong-generic-retry": [('return `Your reply was rejected:\\n${errors.map((e) => `- ${e.path}: ${e.message}`).join("\\n")}\\nReturn the corrected JSON only.`;', 'return "Your reply was rejected. Return the corrected JSON only.";')],
        "wrong-extra-attempt": [("attempt <= maxAttempts; attempt++", "attempt <= maxAttempts + 1; attempt++")],
        "wrong-retry-refusal": [('    if (reply.stop_reason === "refusal") return { status: "refused", value: null, attempts: attempt, errors: [] };\n', "")],
        "wrong-no-grounding": [('      for (const name of evidenceFields) {\n        const quoted = value !== null && typeof value === "object" ? value[name] : undefined;\n        if (typeof quoted === "string" && !document.includes(quoted)) errors.push({ path: `$.${name}`, message: "is not found in the document" });\n      }\n', "")],
        "wrong-integer-allows-fraction": [('integer: (v) => typeof v === "number" && Number.isInteger(v),', 'integer: (v) => typeof v === "number",')],
    }),
    "java": ("Extractor.java", {
        "wrong-no-prose-skip": [("int first = body.indexOf('{');\n        int last = body.lastIndexOf('}');", "int first = 0;\n        int last = body.length() - 1;")],
        "wrong-generic-retry": [('return "Your reply was rejected:\\n" + lines + "Return the corrected JSON only.";', 'return "Your reply was rejected. Return the corrected JSON only.";')],
        "wrong-extra-attempt": [("attempt <= maxAttempts; attempt++", "attempt <= maxAttempts + 1; attempt++")],
        "wrong-retry-refusal": [('            if ("refusal".equals(reply.get("stop_reason"))) return result("refused", null, attempt, new ArrayList<>());\n', "")],
        "wrong-no-grounding": [('                for (String name : evidenceFields) {\n                    Object quoted = value instanceof Map<?, ?> m ? m.get(name) : null;\n                    if (quoted instanceof String s && !document.contains(s)) errors.add(problem("$." + name, "is not found in the document"));\n                }\n', "")],
        "wrong-integer-allows-fraction": [("return v instanceof Double d && !d.isInfinite() && !d.isNaN() && d == Math.rint(d);", "return v instanceof Number;")],
    }),
    "kotlin": ("Extractor.kt", {
        "wrong-no-prose-skip": [("val first = body.indexOf('{')\n    val last = body.lastIndexOf('}')", "val first = 0\n    val last = body.length - 1")],
        "wrong-generic-retry": [('"Your reply was rejected:\\n" + errors.joinToString("") { "- ${it["path"]}: ${it["message"]}\\n" } + "Return the corrected JSON only."', '"Your reply was rejected. Return the corrected JSON only."')],
        "wrong-extra-attempt": [("for (attempt in 1..maxAttempts) {", "for (attempt in 1..maxAttempts + 1) {")],
        "wrong-retry-refusal": [('        if (reply["stop_reason"] == "refusal") return result("refused", null, attempt, emptyList())\n', "")],
        "wrong-no-grounding": [('            for (name in evidenceFields) {\n                val quoted = (value as? Map<*, *>)?.get(name)\n                if (quoted is String && !document.contains(quoted)) found += problem("$.$name", "is not found in the document")\n            }\n', "")],
        "wrong-integer-allows-fraction": [("v is Long || v is Int || v is Short || v is Byte || (v is Double && !v.isInfinite() && !v.isNaN() && v == Math.rint(v))", "v is Number")],
    }),
}

PLANTS[f"{X}/26-tool-use/unit-01/practice-1"] = {
    "python": ("toolloop.py", {
        "wrong-result-per-message": [('            results = [_run_one(tools, b) for b in reply["content"] if b["type"] == "tool_use"]\n            messages.append({"role": "user", "content": results})\n', '            for b in reply["content"]:\n                if b["type"] == "tool_use":\n                    messages.append({"role": "user", "content": [_run_one(tools, b)]})\n')],
        "wrong-swallow-errors": [('return {**result, "content": str(err), "is_error": True}', 'return {**result, "content": str(err)}')],
        "wrong-extra-turn": [("for turn in range(1, max_turns + 1):", "for turn in range(1, max_turns + 2):")],
        "wrong-ignore-stop-reason": [('        elif stop == "refusal":\n            return {"status": "refused", "text": text, "turns": calls, "messages": messages}\n', "")],
        "wrong-forced-every-turn": [('request["tool_choice"] = {"type": "auto"} if forced and turn > 1 else tool_choice', 'request["tool_choice"] = tool_choice')],
        "wrong-forced-on-new-models": [('    if kind in ("any", "tool") and model in FORCED_UNSUPPORTED:\n        raise RequestError("tool_choice", f"{model} does not support forced tool use")\n', "")],
        "wrong-repr-result": [("out if isinstance(out, str) else json.dumps(out)", "out if isinstance(out, str) else str(out)")],
    }),
    "typescript": ("toolloop.ts", {
        "wrong-result-per-message": [('      messages.push({ role: "user", content: reply.content.filter((b) => b.type === "tool_use").map((b) => runOne(tools, b)) });\n', '      for (const b of reply.content.filter((b) => b.type === "tool_use")) messages.push({ role: "user", content: [runOne(tools, b)] });\n')],
        "wrong-swallow-errors": [("return { ...result, content: (err as Error).message, is_error: true };", "return { ...result, content: (err as Error).message };")],
        "wrong-extra-turn": [("turn <= maxTurns; turn++", "turn <= maxTurns + 1; turn++")],
        "wrong-ignore-stop-reason": [('    } else if (stop === "refusal") {\n      return { status: "refused", text, turns: calls, messages };\n', "")],
        "wrong-forced-every-turn": [('request.tool_choice = forced && turn > 1 ? { type: "auto" } : toolChoice;', "request.tool_choice = toolChoice;")],
        "wrong-forced-on-new-models": [('  if ((choice.type === "any" || choice.type === "tool") && FORCED_UNSUPPORTED.has(model)) throw new RequestError("tool_choice", `${model} does not support forced tool use`);\n', "")],
        "wrong-repr-result": [('typeof out === "string" ? out : JSON.stringify(out)', "String(out)")],
    }),
    "java": ("ToolLoop.java", {
        "wrong-result-per-message": [('                for (Map<String, Object> b : content) if ("tool_use".equals(b.get("type"))) results.add(runOne(tools, b));\n                messages.add(map("role", "user", "content", results));\n', '                for (Map<String, Object> b : content) if ("tool_use".equals(b.get("type"))) messages.add(map("role", "user", "content", List.of(runOne(tools, b))));\n')],
        "wrong-swallow-errors": [('            result.put("content", e.getMessage());\n            result.put("is_error", true);\n        }\n        return result;', '            result.put("content", e.getMessage());\n        }\n        return result;')],
        "wrong-extra-turn": [("turn <= maxTurns; turn++", "turn <= maxTurns + 1; turn++")],
        "wrong-ignore-stop-reason": [('            } else if (stop.equals("refusal")) {\n                return outcome("refused", text, calls, messages);\n', "")],
        "wrong-forced-every-turn": [('request.put("tool_choice", forced && turn > 1 ? map("type", "auto") : toolChoice);', 'request.put("tool_choice", toolChoice);')],
        "wrong-forced-on-new-models": [('        if (("any".equals(kind) || "tool".equals(kind)) && FORCED_UNSUPPORTED.contains(model)) throw new RequestError("tool_choice", model + " does not support forced tool use");\n', "")],
        "wrong-repr-result": [("out instanceof String s ? s : Json.stringify(out)", "String.valueOf(out)")],
    }),
    "kotlin": ("ToolLoop.kt", {
        "wrong-result-per-message": [('"tool_use" -> messages += mapOf("role" to "user", "content" to content.filter { it["type"] == "tool_use" }.map { runOne(tools, it) })', '"tool_use" -> content.filter { it["type"] == "tool_use" }.forEach { messages += mapOf("role" to "user", "content" to listOf(runOne(tools, it))) }')],
        "wrong-swallow-errors": [('        result["content"] = e.message\n        result["is_error"] = true\n    }\n    return result', '        result["content"] = e.message\n    }\n    return result')],
        "wrong-extra-turn": [("for (turn in 1..maxTurns) {", "for (turn in 1..maxTurns + 1) {")],
        "wrong-ignore-stop-reason": [('            "refusal" -> return outcome("refused", text, calls, messages)\n', "")],
        "wrong-forced-every-turn": [('request["tool_choice"] = if (forced && turn > 1) mapOf("type" to "auto") else toolChoice', 'request["tool_choice"] = toolChoice')],
        "wrong-forced-on-new-models": [('    if ((kind == "any" || kind == "tool") && model in FORCED_UNSUPPORTED) throw RequestError("tool_choice", "$model does not support forced tool use")\n', "")],
        "wrong-repr-result": [("if (out is String) out else Json.stringify(out)", "if (out is String) out else out.toString()")],
    }),
}

PLANTS[f"{X}/28-retrieval/unit-01/practice-1"] = {
    "python": ("retrieval.py", {
        "wrong-no-overlap": [("        start += size - overlap\n", "        start += size\n")],
        "wrong-no-idf": [("idf = math.log(1 + (n - df[term] + 0.5) / (df[term] + 0.5))", "idf = 1.0")],
        "wrong-fuse-by-votes": [("scores.get(cid, 0.0) + 1.0 / (k + rank)", "scores.get(cid, 0.0) + 1.0")],
        "wrong-rerank-ascending": [("key=lambda cid: -scorer(query, texts[cid])", "key=lambda cid: scorer(query, texts[cid])")],
        "wrong-pool-ignored": [("return rerank(query, ranked[:pool], ", "return rerank(query, ranked, ")],
        "wrong-recall-by-chunk": [("    found = {doc_of[cid] for cid in ids[:k]}\n    return len(found & set(relevant)) / len(set(relevant))\n", "    wanted = set(relevant)\n    return min(1.0, len([cid for cid in ids[:k] if doc_of[cid] in wanted]) / len(wanted))\n")],
        "wrong-context-ignored": [("if contexts and c[\"id\"] in contexts}", "if False and contexts and c[\"id\"] in contexts}")],
    }),
    "typescript": ("retrieval.ts", {
        "wrong-no-overlap": [("start += size - overlap)", "start += size)")],
        "wrong-no-idf": [("const idf = Math.log(1 + (n - df.get(term)! + 0.5) / (df.get(term)! + 0.5));", "const idf = 1;")],
        "wrong-fuse-by-votes": [("(scores.get(id) ?? 0) + 1 / (k + i + 1)", "(scores.get(id) ?? 0) + 1")],
        "wrong-rerank-ascending": [("b.s - a.s || a.i - b.i", "a.s - b.s || a.i - b.i")],
        "wrong-pool-ignored": [("return rerank(query, ranked.slice(0, pool), ", "return rerank(query, ranked, ")],
        "wrong-recall-by-chunk": [("  const found = new Set(ids.slice(0, k).map((id) => docOf[id]));\n  const wanted = new Set(relevant);\n  return [...wanted].filter((d) => found.has(d)).length / wanted.size;\n", "  const wanted = new Set(relevant);\n  return Math.min(1, ids.slice(0, k).filter((id) => wanted.has(docOf[id])).length / wanted.size);\n")],
        "wrong-context-ignored": [("if (contexts && c.id in contexts) indexText", "if (contexts && false && c.id in contexts) indexText")],
    }),
    "java": ("Retrieval.java", {
        "wrong-no-overlap": [("start += size - overlap)", "start += size)")],
        "wrong-no-idf": [("double idf = Math.log(1 + (n - df.get(term) + 0.5) / (df.get(term) + 0.5));", "double idf = 1.0;")],
        "wrong-fuse-by-votes": [("scores.merge(id, 1.0 / (k + rank++), Double::sum);", "scores.merge(id, 1.0, Double::sum);")],
        "wrong-rerank-ascending": [("-scorer.apply(query, texts.get(id))", "scorer.apply(query, texts.get(id))")],
        "wrong-pool-ignored": [("return rerank(query, ranked.subList(0, Math.min(pool, ranked.size())), plain, scorer, k);", "return rerank(query, ranked, plain, scorer, k);")],
        "wrong-recall-by-chunk": [("        Set<String> found = new LinkedHashSet<>();\n        for (String id : ids.subList(0, Math.min(k, ids.size()))) found.add(docOf.get(id));\n        Set<String> wanted = new LinkedHashSet<>(relevant);\n        int hits = 0;\n        for (String d : wanted) if (found.contains(d)) hits++;\n        return (double) hits / wanted.size();\n", "        Set<String> wanted = new LinkedHashSet<>(relevant);\n        int hits = 0;\n        for (String id : ids.subList(0, Math.min(k, ids.size()))) if (wanted.contains(docOf.get(id))) hits++;\n        return Math.min(1.0, (double) hits / wanted.size());\n")],
        "wrong-context-ignored": [("if (contexts != null && contexts.containsKey(c.id())) indexText.put(", "if (contexts != null && false && contexts.containsKey(c.id())) indexText.put(")],
    }),
    "kotlin": ("Retrieval.kt", {
        "wrong-no-overlap": [("        start += size - overlap\n", "        start += size\n")],
        "wrong-no-idf": [("val idf = ln(1 + (n - df.getValue(term) + 0.5) / (df.getValue(term) + 0.5))", "val idf = 1.0")],
        "wrong-fuse-by-votes": [("(scores[id] ?: 0.0) + 1.0 / (k + i + 1)", "(scores[id] ?: 0.0) + 1.0")],
        "wrong-rerank-ascending": [("ids.sortedByDescending { scorer(query, texts.getValue(it)) }", "ids.sortedBy { scorer(query, texts.getValue(it)) }")],
        "wrong-pool-ignored": [("return rerank(query, ranked.take(pool), ", "return rerank(query, ranked, ")],
        "wrong-recall-by-chunk": [("    val found = ids.take(k).map { docOf.getValue(it) }.toSet()\n    val wanted = relevant.toSet()\n    return wanted.count { it in found }.toDouble() / wanted.size\n", "    val wanted = relevant.toSet()\n    return minOf(1.0, ids.take(k).count { docOf.getValue(it) in wanted }.toDouble() / wanted.size)\n")],
        "wrong-context-ignored": [("chunks.filter { contexts != null && it.id in contexts }", "chunks.filter { contexts != null && it.id in contexts && contexts.isEmpty() }")],
    }),
}

PLANTS[f"{X}/29-context-engineering/unit-01/practice-1"] = {
    "python": ("context.py", {
        "wrong-clear-all": [("for block in results[:max(len(results) - keep, 0)]:", "for block in results:")],
        "wrong-clear-excluded": [('if block["type"] == "tool_result" and names.get(block["tool_use_id"]) not in exclude]', 'if block["type"] == "tool_result"]')],
        "wrong-window-splits-pair": [("    turns = split_turns(messages)\n    pinned, rest", "    turns = [[m] for m in messages]\n    pinned, rest")],
        "wrong-summarise-under-budget": [("    if count_tokens(messages) <= budget:\n        return list(messages)\n", "")],
        "wrong-skips-old-summary": [("summary = summarise(older)", "summary = summarise(older[1:])")],
        "wrong-cited-end-inclusive": [('text[start:end] != cite["cited_text"]', 'text[start:end + 1] != cite["cited_text"]')],
        "wrong-footnote-duplicates": [("            if key not in numbers:", "            if True:")],
    }),
    "typescript": ("context.ts", {
        "wrong-clear-all": [("for (const b of results.slice(0, Math.max(results.length - keep, 0))) b.content = placeholder;", "for (const b of results) b.content = placeholder;")],
        "wrong-clear-excluded": [('if (b.type === "tool_result" && !exclude.includes(names.get(b.tool_use_id) ?? "")) results.push(b);', 'if (b.type === "tool_result") results.push(b);')],
        "wrong-window-splits-pair": [("  const turns = splitTurns(messages);\n  const pinned = pin", "  const turns = messages.map((m) => [m]);\n  const pinned = pin")],
        "wrong-summarise-under-budget": [("  if (countTokens(messages) <= budget) return [...messages];\n", "")],
        "wrong-skips-old-summary": [("const summary = summarise(older);", "const summary = summarise(older.slice(1));")],
        "wrong-cited-end-inclusive": [("text.slice(start, end) !== cite.cited_text", "text.slice(start, end + 1) !== cite.cited_text")],
        "wrong-footnote-duplicates": [("      if (!numbers.has(key)) {", "      if (true) {")],
    }),
    "java": ("Context.java", {
        "wrong-clear-all": [('for (int i = 0; i < Math.max(results.size() - keep, 0); i++) results.get(i).put("content", placeholder);', 'for (int i = 0; i < results.size(); i++) results.get(i).put("content", placeholder);')],
        "wrong-clear-excluded": [('if ("tool_result".equals(b.get("type")) && !exclude.contains(names.get((String) b.get("tool_use_id")))) results.add(b);', 'if ("tool_result".equals(b.get("type"))) results.add(b);')],
        "wrong-window-splits-pair": [("        List<List<Map<String, Object>>> turns = splitTurns(messages);\n        List<List<Map<String, Object>>> pinned", "        List<List<Map<String, Object>>> turns = new ArrayList<>();\n        for (Map<String, Object> m : messages) turns.add(new ArrayList<>(List.of(m)));\n        List<List<Map<String, Object>>> pinned")],
        "wrong-summarise-under-budget": [("        if (countTokens(messages) <= budget) return new ArrayList<>(messages);\n", "")],
        "wrong-skips-old-summary": [("String summary = summarise.apply(older);", "String summary = summarise.apply(older.subList(1, older.size()));")],
        "wrong-cited-end-inclusive": [("!text.substring(start, end).equals(cite.get(\"cited_text\"))", "!text.substring(start, Math.min(end + 1, text.length())).equals(cite.get(\"cited_text\"))")],
        "wrong-footnote-duplicates": [("if (!numbers.containsKey(key)) {", "if (true) {")],
    }),
    "kotlin": ("Context.kt", {
        "wrong-clear-all": [('for (b in results.take(maxOf(results.size - keep, 0))) b["content"] = placeholder', 'for (b in results) b["content"] = placeholder')],
        "wrong-clear-excluded": [('blocks.filter { it["type"] == "tool_result" && names[it["tool_use_id"] as String] !in exclude }', 'blocks.filter { it["type"] == "tool_result" }')],
        "wrong-window-splits-pair": [("    val turns = splitTurns(messages)\n    val pinned = if (pin)", "    val turns = messages.map { listOf(it) }\n    val pinned = if (pin)")],
        "wrong-summarise-under-budget": [("    if (countTokens(messages) <= budget) return messages.toList()\n", "")],
        "wrong-skips-old-summary": [("val summary = summarise(older)", "val summary = summarise(older.drop(1))")],
        "wrong-cited-end-inclusive": [('text.substring(start, end) != cite["cited_text"]', 'text.substring(start, minOf(end + 1, text.length)) != cite["cited_text"]')],
        "wrong-footnote-duplicates": [("if (key !in numbers) {", "if (true) {")],
    }),
}

# --- PLANTS END ---


# ===== Level 2: modules 30 to 35 =====
PLANTS[f"{X}/30-vision-and-documents/unit-01/practice-1"] = {
    "python": ("vision.py", {
        "wrong-edge-only-resize": [(" and visual_tokens(w, h) <= max_tokens", "")],
        "wrong-text-first": [('content.append({"type": "text", "text": question})', 'content.insert(0, {"type": "text", "text": question})')],
        "wrong-no-labels": [("if len(images) > 1:", "if False:")],
        "wrong-padded-coordinates": [("return (x / resized_w * width, y / resized_h * height)", "return (x / resized_w * width, y / (math.ceil(resized_h / 28) * 28) * height)")],
        "wrong-same-limit-all-models": [("if len(images) > (100 if context < 1_000_000 else 600):", "if len(images) > 600:")],
        "wrong-many-image-rule": [('if many and max(it["width"], it["height"]) > 2000:', "if False:")],
        "wrong-exact-silent": [('            if exact:\n                raise RequestError(f"items[{i}].dimensions", f"would be resized to {seen[0]}x{seen[1]}")\n', "")],
    }),
    "typescript": ("vision.ts", {
        "wrong-edge-only-resize": [(" && visualTokens(w, h) <= maxTokens", "")],
        "wrong-text-first": [('content.push({ type: "text", text: question });', 'content.unshift({ type: "text", text: question });')],
        "wrong-no-labels": [("if (images.length > 1)", "if (false)")],
        "wrong-padded-coordinates": [("return [(cx / resizedW) * width, (cy / resizedH) * height];", "return [(cx / resizedW) * width, (cy / (Math.ceil(resizedH / 28) * 28)) * height];")],
        "wrong-same-limit-all-models": [("if (images.length > limit)", "if (images.length > 600)")],
        "wrong-many-image-rule": [("if (many && Math.max(w, h) > 2000)", "if (false && Math.max(w, h) > 2000)")],
        "wrong-exact-silent": [("      if (exact) throw new RequestError(`items[${i}].dimensions`, `would be resized to ${seen[0]}x${seen[1]}`);\n", "")],
    }),
    "java": ("Vision.java", {
        "wrong-edge-only-resize": [(" && visualTokens(w, h) <= maxTokens", "")],
        "wrong-text-first": [('content.add(map("type", "text", "text", question));', 'content.add(0, map("type", "text", "text", question));')],
        "wrong-no-labels": [("if (images > 1) content.add", "if (images > 1000) content.add")],
        "wrong-padded-coordinates": [("return new double[] {cx / r[0] * width, cy / r[1] * height};", "return new double[] {cx / r[0] * width, cy / (Math.ceilDiv(r[1], 28) * 28) * height};")],
        "wrong-same-limit-all-models": [("if (images > limit)", "if (images > 600)")],
        "wrong-many-image-rule": [("if (many && Math.max(w, h) > 2000)", "if (false && Math.max(w, h) > 2000)")],
        "wrong-exact-silent": [('                if (exact) throw new RequestError("items[" + i + "].dimensions", "would be resized to " + seen[0] + "x" + seen[1]);\n', "")],
    }),
    "kotlin": ("Vision.kt", {
        "wrong-edge-only-resize": [(" && visualTokens(w, h) <= maxTokens", "")],
        "wrong-text-first": [('content.add(mapOf("type" to "text", "text" to question))', 'content.add(0, mapOf("type" to "text", "text" to question))')],
        "wrong-no-labels": [("if (images > 1) content.add(", "if (images > 1000) content.add(")],
        "wrong-padded-coordinates": [("return Pair(cx / rw * width, cy / rh * height)", "return Pair(cx / rw * width, cy / (ceil(rh / 28.0).toInt() * 28) * height)")],
        "wrong-same-limit-all-models": [("if (images > limit)", "if (images > 600)")],
        "wrong-many-image-rule": [("if (many && max(w, h) > 2000)", "if (false && max(w, h) > 2000)")],
        "wrong-exact-silent": [('            if (exact) throw RequestError("items[$i].dimensions", "would be resized to ${seen.first}x${seen.second}")\n', "")],
    }),
}

PLANTS[f"{X}/31-computer-use/unit-01/practice-1"] = {
    "python": ("computer.py", {
        "wrong-unscaled-click": [("round(x / scale)", "round(x)"), ("round(y / scale)", "round(y)")],
        "wrong-no-confirm": [('if element and element.get("risk", "none") != "none":', "if False:")],
        "wrong-continue-after-failure": [("                        failed = True\n", "")],
        "wrong-result-per-message": [('            messages.append({"role": "user", "content": results})\n', '            for r in results:\n                messages.append({"role": "user", "content": [r]})\n')],
        "wrong-screenshot-full-size": [("render(screen, *shot)", 'render(screen, screen["width"], screen["height"])')],
        "wrong-prune-oldest-kept": [("images[:-keep] if keep > 0 else images", "images[keep:] if keep > 0 else images")],
        "wrong-extra-turn": [("for turn in range(1, max_turns + 1):", "for turn in range(1, max_turns + 2):")],
    }),
    "typescript": ("computer.ts", {
        "wrong-unscaled-click": [("roundHalfEven(x / scale)", "roundHalfEven(x)"), ("roundHalfEven(y / scale)", "roundHalfEven(y)")],
        "wrong-no-confirm": [('if (element && (element.risk ?? "none") !== "none") {', "if (false) {")],
        "wrong-continue-after-failure": [("            failed = true;\n", "")],
        "wrong-result-per-message": [('messages.push({ role: "user", content: results });', 'for (const r of results) messages.push({ role: "user", content: [r] });')],
        "wrong-screenshot-full-size": [("data: render(screen, shot[0], shot[1])", "data: render(screen, screen.width, screen.height)")],
        "wrong-prune-oldest-kept": [("images.slice(0, Math.max(images.length - keep, 0))", "images.slice(keep)")],
        "wrong-extra-turn": [("turn <= maxTurns; turn++", "turn <= maxTurns + 1; turn++")],
    }),
    "java": ("Computer.java", {
        "wrong-unscaled-click": [("Math.rint(x / scale)", "Math.rint(x)"), ("Math.rint(y / scale)", "Math.rint(y)")],
        "wrong-no-confirm": [('if (element != null && !"none".equals(element.getOrDefault("risk", "none"))) {', "if (false) {")],
        "wrong-continue-after-failure": [("                            failed = true;\n", "")],
        "wrong-result-per-message": [('messages.add(map("role", "user", "content", results));', 'for (Object r : results) messages.add(map("role", "user", "content", List.of(r)));')],
        "wrong-screenshot-full-size": [("return image(render(screen, shot[0], shot[1]));", 'return image(render(screen, num(screen.get("width")), num(screen.get("height"))));')],
        "wrong-prune-oldest-kept": [("for (int k = 0; k < upTo; k++)", "for (int k = keep; k < images.size(); k++)")],
        "wrong-extra-turn": [("turn <= maxTurns; turn++", "turn <= maxTurns + 1; turn++")],
    }),
    "kotlin": ("Computer.kt", {
        "wrong-unscaled-click": [("Math.rint(x / scale)", "Math.rint(x)"), ("Math.rint(y / scale)", "Math.rint(y)")],
        "wrong-no-confirm": [('if (element != null && (element["risk"] ?: "none") != "none") {', "if (element != null && false) {")],
        "wrong-continue-after-failure": [("                        failed = true\n", "")],
        "wrong-result-per-message": [('messages.add(mapOf("role" to "user", "content" to results))', 'for (r in results) messages.add(mapOf("role" to "user", "content" to listOf(r)))')],
        "wrong-screenshot-full-size": [("return image(render(screen, shot.first, shot.second))", 'return image(render(screen, num(screen["width"]), num(screen["height"])))')],
        "wrong-prune-oldest-kept": [("images.take(upTo)", "images.drop(keep)")],
        "wrong-extra-turn": [("for (turn in 1..maxTurns) {", "for (turn in 1..maxTurns + 1) {")],
    }),
}

PLANTS[f"{X}/34-workflows-and-agents/unit-01/practice-1"] = {
    "python": ("workflows.py", {
        "wrong-no-cap": [("return steps[:max_subtasks], False", "return steps, False")],
        "wrong-stop-at-first-failure": [('results.append({"subtask": subtask, "status": "failed", "error": str(err)})', 'results.append({"subtask": subtask, "status": "failed", "error": str(err)})\n            break')],
        "wrong-no-feedback": [(r"\nFeedback: {feedback}", "")],
        "wrong-last-draft": [('"status": "max_rounds", "draft": best,', '"status": "max_rounds", "draft": draft,')],
        "wrong-trust-unreadable-judge": [('return 0, "The judge reply could not be read."', 'return 10, "The judge reply could not be read."')],
        "wrong-punctuation-kept": [("reply.strip().strip(\".,;:!\\\"'`\").strip().lower()", "reply.strip().lower()")],
        "wrong-tie-goes-last": [("winner = next(a for a, c in counts.items() if c == top)", "winner = [a for a, c in counts.items() if c == top][-1]")],
    }),
    "typescript": ("workflows.ts", {
        "wrong-no-cap": [("return [steps.slice(0, maxSubtasks), false];", "return [steps, false];")],
        "wrong-stop-at-first-failure": [("error: (err as Error).message }); // one worker failing must not stop the others", "error: (err as Error).message }); break;")],
        "wrong-no-feedback": [(r"\nFeedback: ${feedback}", "")],
        "wrong-last-draft": [('{ status: "max_rounds", draft: best,', '{ status: "max_rounds", draft,')],
        "wrong-trust-unreadable-judge": [('return [0, "The judge reply could not be read."];', 'return [10, "The judge reply could not be read."];')],
        "wrong-punctuation-kept": [('reply.trim().replace(/^[.,;:!"\'`]+|[.,;:!"\'`]+$/g, "").trim().toLowerCase()', "reply.trim().toLowerCase()")],
        "wrong-tie-goes-last": [("const winner = [...counts].find(([, c]) => c === top)![0];", "const winner = [...counts].filter(([, c]) => c === top).pop()![0];")],
    }),
    "java": ("Workflows.java", {
        "wrong-no-cap": [("steps.subList(0, Math.min(steps.size(), maxSubtasks))", "steps")],
        "wrong-stop-at-first-failure": [('results.add(map("subtask", subtask, "status", "failed", "error", e.getMessage()));', 'results.add(map("subtask", subtask, "status", "failed", "error", e.getMessage()));\n                break;')],
        "wrong-no-feedback": [(r' + "\nFeedback: " + feedback', "")],
        "wrong-last-draft": [('return map("status", "max_rounds", "draft", best,', 'return map("status", "max_rounds", "draft", draft,')],
        "wrong-trust-unreadable-judge": [("return new Object[] {0, UNREADABLE};", "return new Object[] {10, UNREADABLE};")],
        "wrong-punctuation-kept": [('reply.strip().replaceAll("^[.,;:!\\"\'`]+|[.,;:!\\"\'`]+$", "").strip().toLowerCase(Locale.ROOT)', "reply.strip().toLowerCase(Locale.ROOT)")],
        "wrong-tie-goes-last": [(".findFirst().get().getKey()", ".reduce((a, b) -> b).get().getKey()")],
    }),
    "kotlin": ("Workflows.kt", {
        "wrong-no-cap": [("else steps.take(maxSubtasks)", "else steps")],
        "wrong-stop-at-first-failure": [('results.add(linkedMapOf("subtask" to subtask, "status" to "failed", "error" to e.message))', 'results.add(linkedMapOf("subtask" to subtask, "status" to "failed", "error" to e.message))\n            break')],
        "wrong-no-feedback": [(r"\nFeedback: $feedback", "")],
        "wrong-last-draft": [('"status" to "max_rounds", "draft" to best,', '"status" to "max_rounds", "draft" to draft,')],
        "wrong-trust-unreadable-judge": [("return Pair(0, UNREADABLE)", "return Pair(10, UNREADABLE)")],
        "wrong-punctuation-kept": [('reply.trim().replace(Regex("^[.,;:!\\"\'`]+|[.,;:!\\"\'`]+$"), "").trim().lowercase(Locale.ROOT)', "reply.trim().lowercase(Locale.ROOT)")],
        "wrong-tie-goes-last": [("counts.entries.first { it.value == top }.key", "counts.entries.last { it.value == top }.key")],
    }),
}

PLANTS[f"{X}/33-mcp-advanced/unit-01/practice-1"] = {
    "python": ("mrtr.py", {
        "wrong-no-expiry": [('        if now > state.get("exp", 0):\n            return _error(-32602, "Expired requestState")\n', "")],
        "wrong-any-principal": [('if state.get("sub") != principal or state.get("tool") != name or', 'if state.get("tool") != name or')],
        "wrong-no-digest": [(' or state.get("digest") != args_digest(arguments)', "")],
        "wrong-bad-state-restarts": [('    token = request.get("requestState")\n', '    token = request.get("requestState")\n    if token is not None and read_state(secret, token) is None:\n        token = None\n')],
        "wrong-elicit-without-capability": [('if elicitation is None or (elicitation != {} and "form" not in elicitation):', "if False:")],
        "wrong-decline-continues": [('if answer["action"] != "accept" or (answer.get("content") or {}).get("confirm") is not True:', 'if (answer.get("content") or {}).get("confirm") is not True:')],
        "wrong-error-on-missing-input": [('return _ask(_confirm_request(service), "confirm", secret, name, arguments, principal, now)', 'return _error(-32602, "Missing inputResponses")')],
    }),
    "typescript": ("mrtr.ts", {
        "wrong-no-expiry": [('    if (now > (state.exp ?? 0)) return error(-32602, "Expired requestState");\n', "")],
        "wrong-any-principal": [("if (state.sub !== principal || state.tool !== name ||", "if (state.tool !== name ||")],
        "wrong-no-digest": [(" || state.digest !== argsDigest(args)", "")],
        "wrong-bad-state-restarts": [("const token = request.requestState;", "const token = request.requestState !== undefined && readState(secret, request.requestState) === null ? undefined : request.requestState;")],
        "wrong-elicit-without-capability": [('if (elicitation === undefined || elicitation === null || (Object.keys(elicitation).length > 0 && !("form" in elicitation))) {', "if (false) {")],
        "wrong-decline-continues": [('if (answer.action !== "accept" || (answer.content ?? {}).confirm !== true)', "if ((answer.content ?? {}).confirm !== true)")],
        "wrong-error-on-missing-input": [('return ask(confirmRequest(service), "confirm", secret, name, args, principal, now);', 'return error(-32602, "Missing inputResponses");')],
    }),
    "java": ("Mrtr.java", {
        "wrong-no-expiry": [('            if (now > (state.get("exp") instanceof Number e ? e.longValue() : 0)) return error(-32602, "Expired requestState", null);\n', "")],
        "wrong-any-principal": [('if (!principal.equals(state.get("sub")) || !name.equals(state.get("tool"))', 'if (!name.equals(state.get("tool"))')],
        "wrong-no-digest": [(' || !argsDigest(arguments).equals(state.get("digest"))', "")],
        "wrong-bad-state-restarts": [('Object token = request.get("requestState");', 'Object token = request.get("requestState") != null && readState(secret, String.valueOf(request.get("requestState"))) == null ? null : request.get("requestState");')],
        "wrong-elicit-without-capability": [('if (elicitation == null || (!((Map<?, ?>) elicitation).isEmpty() && !((Map<?, ?>) elicitation).containsKey("form")))', "if (false)")],
        "wrong-decline-continues": [('if (!"accept".equals(a.get("action")) || !confirmed)', "if (!confirmed)")],
        "wrong-error-on-missing-input": [('return ask(confirmRequest(s), "confirm", secret, "deploy", arguments, principal, now);', 'return error(-32602, "Missing inputResponses", null);')],
    }),
    "kotlin": ("Mrtr.kt", {
        "wrong-no-expiry": [('        if (now > ((state["exp"] as? Number)?.toLong() ?: 0L)) return error(-32602, "Expired requestState")\n', "")],
        "wrong-any-principal": [('if (state["sub"] != principal || state["tool"] != name ||', 'if (state["tool"] != name ||')],
        "wrong-no-digest": [(' || state["digest"] != argsDigest(arguments)', "")],
        "wrong-bad-state-restarts": [('val token = request["requestState"]', 'val token = request["requestState"]?.takeIf { readState(secret, it.toString()) != null }')],
        "wrong-elicit-without-capability": [('if (elicitation == null || (elicitation.isNotEmpty() && !elicitation.containsKey("form"))) return complete(', "if (elicitation != null && false) return complete(")],
        "wrong-decline-continues": [('if (answer["action"] != "accept" || !confirmed)', "if (!confirmed)")],
        "wrong-error-on-missing-input": [('return ask(confirmRequest(service), "confirm", secret, "deploy", arguments, principal, now)', 'return error(-32602, "Missing inputResponses")')],
    }),
}

PLANTS[f"{X}/35-the-claude-agent-sdk/unit-01/practice-1"] = {
    "python": ("agent.py", {
        "wrong-auto-approve": [("allowed_tools=[],", "allowed_tools=READ_TOOLS,")],
        "wrong-edit-in-readonly": [('if tool_name in EDIT_TOOLS and mode != "edit":', "if False:")],
        "wrong-env-variants": [('if base.startswith(".env") and base != ".env.example":', 'if base == ".env":')],
        "wrong-chaining-allowed": [('r"[;&|<>`]|\\$\\("', 'r"[<>`]|\\$\\("')],
        "wrong-danger-no-interrupt": [('return deny("Dangerous command", True)', 'return deny("Dangerous command", False)')],
        "wrong-push-substring": [('r"\\bgit\\s+push\\b"', 'r"\\bgit\\s+push"')],
        "wrong-no-turn-limit": [("max_turns=6, ", "")],
        "wrong-raises-after-error-result": [("        if not any(isinstance(m, ResultMessage) for m in messages):\n            raise\n", "        raise\n")],
        "wrong-hides-crash": [("        if not any(isinstance(m, ResultMessage) for m in messages):\n            raise\n", "        pass\n")],
        "wrong-status-unmapped": [('"error_max_turns": "max_turns", ', "")],
    }),
    "typescript": ("agent.ts", {
        "wrong-auto-approve": [("allowedTools: [] as string[],", "allowedTools: READ_TOOLS,")],
        "wrong-edit-in-readonly": [('if (EDIT_TOOLS.includes(toolName) && mode !== "edit")', "if (false)")],
        "wrong-env-variants": [('if (base.startsWith(".env") && base !== ".env.example")', 'if (base === ".env")')],
        "wrong-chaining-allowed": [("/[;&|<>`]|\\$\\(/", "/[<>`]|\\$\\(/")],
        "wrong-danger-no-interrupt": [('return deny("Dangerous command", true)', 'return deny("Dangerous command", false)')],
        "wrong-push-substring": [("/\\bgit\\s+push\\b/", "/\\bgit\\s+push/")],
        "wrong-no-turn-limit": [("maxTurns: 6, ", "")],
        "wrong-raises-after-error-result": [('    if (!messages.some((m) => m.type === "result")) throw error;', "    throw error;")],
        "wrong-hides-crash": [('    if (!messages.some((m) => m.type === "result")) throw error;', "    void error;")],
        "wrong-status-unmapped": [('error_max_turns: "max_turns", ', "")],
    }),
}

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

# --- PLANTS ABOVE ---


# ===== Level 2: modules 38 to 41 =====
PLANTS[f"{X}/38-claude-code-for-developers/unit-01/practice-1"] = both("CLAUDE.md", {
    "wrong-bypass-mode": {".claude/settings.json": [('"defaultMode": "acceptEdits"', '"defaultMode": "bypassPermissions"')]},
    "wrong-write-deny": {".claude/settings.json": [('"Read(./.env)"', '"Write(./.env)"'), ('"Read(./secrets/**)"', '"Write(./secrets/**)"')]},
    "wrong-push-ask-only": {".claude/settings.json": [('"Bash(git push *)",\n      "Bash(curl *)"', '"Bash(curl *)"'), ('"Bash(git commit *)"\n', '"Bash(git commit *)",\n      "Bash(git push *)"\n')]},
    "wrong-bare-bash-allow": {".claude/settings.json": [('"Bash(make test)",', '"Bash",\n      "Bash(make test)",')]},
    "wrong-local-not-ignored": {".gitignore": [(".claude/settings.local.json\n", "")]},
    "wrong-local-widens": {".claude/settings.local.json": [('"model": "sonnet"', '"model": "sonnet",\n  "permissions": {"allow": ["Bash(make deploy)"]}')]},
    "wrong-bloated-memory": {"CLAUDE.md": [("## Gotchas", "## Notes\n\n" + "- keep this in mind\n" * 205 + "\n## Gotchas")]},
    "wrong-no-import": {"CLAUDE.md": [("See @docs/architecture.md for", "See docs/architecture.md for")]},
    "wrong-skill-auto": {".claude/skills/fix-issue/SKILL.md": [("disable-model-invocation: true\n", "")]},
    "wrong-headless-bypass": {"scripts/ci-review.sh": [("--permission-mode dontAsk", "--dangerously-skip-permissions")]},
    "wrong-headless-unbounded": {"scripts/ci-review.sh": [("  --max-turns 5 \\\n", "")]},
    "wrong-key-in-script": {"scripts/ci-review.sh": [("set -eu\n", "set -eu\nexport ANTHROPIC_API_KEY=sk-ant-api03-EXAMPLEKEY123\n")]},
})

PLANTS[f"{X}/39-extending-claude-code/unit-01/practice-1"] = both("scripts/guard.py", {
    "wrong-push-prefix": [('if program == "git" and git_subcommand(args) == "push":', 'if program == "git" and args[:1] == ["push"]:')],
    "wrong-rm-literal": [('if program == "rm" and ("r" in short or "R" in short or "--recursive" in args) and ("f" in short or "--force" in args):', 'if program == "rm" and "-rf" in args:')],
    "wrong-pipe-shell-allowed": [('if program in ("sh", "bash", "zsh") and position > 0 and "-c" not in args:', "if False:")],
    "wrong-edit-exit-one": [('is protected ({pattern})")\n                return 2', 'is protected ({pattern})")\n                return 1')],
    "wrong-no-reason": [('                sys.stderr.write(f"Blocked: {path} is protected ({pattern})")\n', "")],
    "wrong-bad-json-allowed": [('        sys.stderr.write("Blocked: the hook event could not be read")\n        return 2\n', "        return 0\n")],
    "wrong-matcher-bash-only": {"hooks/hooks.json": [('"Bash|Edit|Write"', '"Bash"')]},
    "wrong-relative-path": {"hooks/hooks.json": [('python3 \\"${CLAUDE_PLUGIN_ROOT}/scripts/guard.py\\"', "python3 scripts/guard.py")]},
    "wrong-publish-auto": {"skills/publish/SKILL.md": [("disable-model-invocation: true\n", "")]},
    "wrong-bare-bash": {"skills/publish/SKILL.md": [("allowed-tools: Bash(git tag *) Bash(gh release create *)", "allowed-tools: Bash")]},
    "wrong-agent-writes": {"agents/changelog-reviewer.md": [("tools: Read, Grep, Glob", "tools: Read, Grep, Edit")]},
    "wrong-agent-bypass": {"agents/changelog-reviewer.md": [("memory: project\n", "memory: project\npermissionMode: bypassPermissions\n")]},
    "wrong-dependency-unpinned": {".claude-plugin/plugin.json": [('{ "name": "secrets-vault", "version": "~2.1.0" }', '"secrets-vault"')]},
    "wrong-marketplace-mismatch": {".claude/settings.json": [('"release-kit@acme-tools": true', '"release-kit@acme-plugins": true')]},
})

PLANTS[f"{X}/40-claude-in-the-software-life-cycle/unit-01/practice-1"] = both(".github/workflows/claude.yml", {
    "wrong-mention-unguarded": [("    if: contains(github.event.comment.body, '@claude')\n", "")],
    "wrong-mention-beta": [("claude-code-action@v1", "claude-code-action@beta")],
    "wrong-review-writes": {".github/workflows/review.yml": [("      contents: read\n", "      contents: write\n")]},
    "wrong-review-no-comment": {".github/workflows/review.yml": [("/code-review:code-review --comment ", "/code-review:code-review ")]},
    "wrong-review-no-checkout": {".github/workflows/review.yml": [("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")]},
    "wrong-prompt-version-mismatch": {"prompts/CHANGELOG.md": [("## 1.2.0\n\n- triage: ask for one sentence of reasoning.\n\n", "")]},
    "wrong-prompt-no-version": {"prompts/triage.md": [("version: 1.2.0\n", "")]},
    "wrong-review-no-skip": {"REVIEW.md": [("## Skip", "## Ignore")]},
    "wrong-no-timeout": [("    timeout-minutes: 20\n", "")],
    "wrong-no-turn-cap": [("          claude_args: --max-turns 8\n", "")],
    "wrong-literal-key": [("${{ secrets.ANTHROPIC_API_KEY }}", "sk-ant-api03-EXAMPLEKEY12345")],
})

PLANTS[f"{X}/41-security-and-safety/unit-01/practice-1"] = {
    "python": ("gate.py", {
        "wrong-concatenate": [('payload = json.dumps({"source": source, "trust": "untrusted", "content": content}, separators=(",", ":"))', 'payload = f"source={source}\\ntrust=untrusted\\n{content}"')],
        "wrong-screen-case-sensitive": [(r'\b(instructions?|prompts?|rules)\b", re.I | re.S),', r'\b(instructions?|prompts?|rules)\b", re.S),')],
        "wrong-screen-leaks": [("withheld: possible prompt injection ({', '.join(signals)})\"", "withheld: possible prompt injection ({', '.join(signals)}): {content[:40]}\"")],
        "wrong-prefix-sibling": [('return path == self.root or path.startswith(self.root + "/")', "return path.startswith(self.root)")],
        "wrong-env-variants": [('return (base == ".env" or (base.startswith(".env.") and base != ".env.example") or "secrets" in parts[:-1]', 'return (base == ".env" or "secrets" in parts[:-1]')],
        "wrong-chain-allowed": [("if any(t in command for t in SHELL_TRICKS):", "if False:")],
        "wrong-host-suffix": [('if not any(host == h or host.endswith("." + h) for h in self.allowed_hosts):', "if not any(host.endswith(h) for h in self.allowed_hosts):")],
        "wrong-email-tainted-allowed": [('        if self._tainted:\n            return _result("deny", "a person must send it")\n', "")],
        "wrong-luhn-skip": [('return "[CARD]" if 13 <= len(digits) <= 19 and _luhn(digits) else m.group(0)', 'return "[CARD]" if 13 <= len(digits) <= 19 else m.group(0)')],
        "wrong-audit-raw": [("clean = {k: (redact(v) if isinstance(v, str) else v) for k, v in args.items()}", "clean = dict(args)")],
        "wrong-hook-exit2": [('return {"exit_code": 0, "stdout": json.dumps(out, separators=(",", ":")), "stderr": ""}', 'return {"exit_code": 2, "stdout": json.dumps(out, separators=(",", ":")), "stderr": ""}')],
        "wrong-alert-asks": [('if record["decision"] == "deny":', 'if record["decision"] != "allow":')],
    }),
    "typescript": ("gate.ts", {
        "wrong-concatenate": [('JSON.stringify({ source, trust: "untrusted", content })', '`source=${source}\\ntrust=untrusted\\n${content}`')],
        "wrong-screen-case-sensitive": [(r'\b(instructions?|prompts?|rules)\b/i],', r'\b(instructions?|prompts?|rules)\b/],')],
        "wrong-screen-leaks": [('possible prompt injection (${signals.join(", ")})`', 'possible prompt injection (${signals.join(", ")}): ${content.slice(0, 40)}`')],
        "wrong-prefix-sibling": [('return path === this.root || path.startsWith(this.root + "/");', "return path.startsWith(this.root);")],
        "wrong-env-variants": [('return base === ".env" || (base.startsWith(".env.") && base !== ".env.example") || parts.slice(0, -1).includes("secrets")', 'return base === ".env" || parts.slice(0, -1).includes("secrets")')],
        "wrong-chain-allowed": [('if (SHELL_TRICKS.some((t) => command.includes(t))) return result("deny", "chaining or redirection");', "")],
        "wrong-host-suffix": [('if (!this.allowedHosts.some((h) => host === h || host.endsWith("." + h))) return result("deny", "host not allowed");', 'if (!this.allowedHosts.some((h) => host.endsWith(h))) return result("deny", "host not allowed");')],
        "wrong-email-tainted-allowed": [('    if (this.isTainted) return result("deny", "a person must send it");\n', "")],
        "wrong-luhn-skip": [('return digits.length >= 13 && digits.length <= 19 && luhn(digits) ? "[CARD]" : m;', 'return digits.length >= 13 && digits.length <= 19 ? "[CARD]" : m;')],
        "wrong-audit-raw": [('clean[k] = typeof v === "string" ? redact(v) : v;', "clean[k] = v;")],
        "wrong-hook-exit2": [('return { exit_code: 0, stdout: JSON.stringify(out), stderr: "" };', 'return { exit_code: 2, stdout: JSON.stringify(out), stderr: "" };')],
        "wrong-alert-asks": [('if (r.decision === "deny") {', 'if (r.decision !== "allow") {')],
    }),
    "java": ("Gate.java", {
        "wrong-concatenate": [('"content", Json.stringify(map("source", source, "trust", "untrusted", "content", content)));', r'"content", "source=" + source + "\ntrust=untrusted\n" + content);')],
        "wrong-screen-case-sensitive": [(r'\\b(instructions?|prompts?|rules)\\b", flags));', r'\\b(instructions?|prompts?|rules)\\b", Pattern.DOTALL));')],
        "wrong-screen-leaks": [('+ String.join(", ", signals) + ")");', '+ String.join(", ", signals) + "): " + content.substring(0, Math.min(40, content.length())));')],
        "wrong-prefix-sibling": [('return path.equals(root) || path.startsWith(root + "/");', "return path.startsWith(root);")],
        "wrong-env-variants": [('return base.equals(".env") || (base.startsWith(".env.") && !base.equals(".env.example")) || parts.subList', 'return base.equals(".env") || parts.subList')],
        "wrong-chain-allowed": [('for (String t : SHELL_TRICKS) if (command.contains(t)) return result("deny", "chaining or redirection");', "")],
        "wrong-host-suffix": [('for (String h : allowedHosts) if (host.equals(h) || host.endsWith("." + h)) listed = true;', "for (String h : allowedHosts) if (host.endsWith(h)) listed = true;")],
        "wrong-email-tainted-allowed": [('        if (tainted) return result("deny", "a person must send it");\n', "")],
        "wrong-luhn-skip": [('digits.length() >= 13 && digits.length() <= 19 && luhn(digits) ? "[CARD]" : m.group()', 'digits.length() >= 13 && digits.length() <= 19 ? "[CARD]" : m.group()')],
        "wrong-audit-raw": [("clean.put(e.getKey(), e.getValue() instanceof String s ? redact(s) : e.getValue());", "clean.put(e.getKey(), e.getValue());")],
        "wrong-hook-exit2": [('return map("exit_code", 0, "stdout", Json.stringify(out), "stderr", "");', 'return map("exit_code", 2, "stdout", Json.stringify(out), "stderr", "");')],
        "wrong-alert-asks": [('if (r.get("decision").equals("deny")) {', 'if (!r.get("decision").equals("allow")) {')],
    }),
    "kotlin": ("Gate.kt", {
        "wrong-concatenate": [('"content" to Json.stringify(linkedMapOf("source" to source, "trust" to "untrusted", "content" to content)))', r'"content" to ("source=" + source + "\ntrust=untrusted\n" + content))')],
        "wrong-screen-case-sensitive": [(r'\\b(instructions?|prompts?|rules)\\b", flags),', r'\\b(instructions?|prompts?|rules)\\b", setOf(RegexOption.DOT_MATCHES_ALL)),')],
        "wrong-screen-leaks": [('(${found.joinToString(", ")})")', '(${found.joinToString(", ")}): ${content.take(40)}")')],
        "wrong-prefix-sibling": [('private fun inside(path: String) = path == root || path.startsWith("$root/")', "private fun inside(path: String) = path.startsWith(root)")],
        "wrong-env-variants": [('return base == ".env" || (base.startsWith(".env.") && base != ".env.example") || parts.dropLast(1)', 'return base == ".env" || parts.dropLast(1)')],
        "wrong-chain-allowed": [('if (shellTricks.any { it in command }) return result("deny", "chaining or redirection")', "")],
        "wrong-host-suffix": [('if (allowedHosts.none { host == it || host.endsWith(".$it") }) return result("deny", "host not allowed")', 'if (allowedHosts.none { host.endsWith(it) }) return result("deny", "host not allowed")')],
        "wrong-email-tainted-allowed": [('        if (tainted) return result("deny", "a person must send it")\n', "")],
        "wrong-luhn-skip": [('if (digits.length in 13..19 && luhn(digits)) "[CARD]" else m.value', 'if (digits.length in 13..19) "[CARD]" else m.value')],
        "wrong-audit-raw": [("val clean = args.mapValues { (_, v) -> if (v is String) redact(v) else v }", "val clean = args")],
        "wrong-hook-exit2": [('return linkedMapOf("exit_code" to 0, "stdout" to Json.stringify(out), "stderr" to "")', 'return linkedMapOf("exit_code" to 2, "stdout" to Json.stringify(out), "stderr" to "")')],
        "wrong-alert-asks": [('if (r["decision"] == "deny") {', 'if (r["decision"] != "allow") {')],
    }),
}

# --- PLANTS ABOVE ---


# ===== Level 2: modules 42 and 43 =====
PLANTS[f"{X}/42-evaluation/unit-01/practice-1"] = {
    "python": ("harness.py", {
        "wrong-regex-full-match": [('re.search(check["pattern"], output)', 're.fullmatch(check["pattern"], output)')],
        "wrong-exact-case-sensitive": [('return " ".join(text.split()).lower()', 'return " ".join(text.split())')],
        "wrong-exact-contains": [('_norm(output) == _norm(check["expected"]) else _result(False, "mismatch")', '_norm(check["expected"]) in _norm(output) else _result(False, "mismatch")')],
        "wrong-json-loose-type": [('type(actual) is type(want) and actual == want', 'str(actual) == str(want)')],
        "wrong-json-fence-ok": [('data = json.loads(output)', 'data = json.loads(output.replace("```json", "").replace("```", ""))')],
        "wrong-judge-first-digit": [('text = reply.strip() if isinstance(reply, str) else ""', 'text = (re.search(r"[1-5]", reply) or [reply.strip()])[0] if isinstance(reply, str) else ""')],
        "wrong-judge-strict-threshold": [('if score >= check.get("threshold", 4):', 'if score > check.get("threshold", 4):')],
        "wrong-model-error-passes": [('runs.append(_result(False, "model error"))', 'runs.append(_result(True, "ok"))')],
        "wrong-tag-total": [('row["total"] += 1\n', 'row["total"] += 1 if passed else 0\n')],
        "wrong-meets-at-or-below": [('report["pass_rate"] < criteria["min_pass_rate"]', 'report["pass_rate"] <= criteria["min_pass_rate"]')],
        "wrong-meets-missing-tag-ok": [('if row is None or row["total"] == 0 or row["passed"] / row["total"] < minimum:', 'if row is not None and row["total"] > 0 and row["passed"] / row["total"] < minimum:')],
        "wrong-compare-rate-only": [('"ok": not regressions and not removed}', '"ok": current["pass_rate"] >= baseline["pass_rate"]}')],
        "wrong-compare-removed-ignored": [('"ok": not regressions and not removed}', '"ok": not regressions}')],
        "wrong-flaky-any-run": [('passed = all(r["passed"] for r in runs)', 'passed = any(r["passed"] for r in runs)')],
    }),
    "typescript": ("harness.ts", {
        "wrong-regex-full-match": [('new RegExp(check.pattern).test(output)', 'new RegExp("^(?:" + check.pattern + ")$").test(output)')],
        "wrong-exact-case-sensitive": [('.join(" ").toLowerCase();', '.join(" ");')],
        "wrong-exact-contains": [('norm(output) === norm(check.expected) ?', 'norm(output).includes(norm(check.expected)) ?')],
        "wrong-json-loose-type": [('obj[check.field] === check.equals ?', 'String(obj[check.field]) === String(check.equals) ?')],
        "wrong-json-fence-ok": [('data = JSON.parse(output);', 'data = JSON.parse(output.replace("```json", "").replace("```", ""));')],
        "wrong-judge-first-digit": [('const text = typeof reply === "string" ? reply.trim() : "";', 'const text = typeof reply === "string" ? (reply.match(/[1-5]/)?.[0] ?? reply.trim()) : "";')],
        "wrong-judge-strict-threshold": [('score >= (check.threshold ?? 4)', 'score > (check.threshold ?? 4)')],
        "wrong-model-error-passes": [('runs.push({ passed: false, reason: "model error" });', 'runs.push({ passed: true, reason: "ok" });')],
        "wrong-tag-total": [('row.total += 1;', 'row.total += passed ? 1 : 0;')],
        "wrong-meets-at-or-below": [('report.pass_rate < criteria.min_pass_rate', 'report.pass_rate <= criteria.min_pass_rate')],
        "wrong-meets-missing-tag-ok": [('if (!row || row.total === 0 || row.passed / row.total < minimum)', 'if (row && row.total > 0 && row.passed / row.total < minimum)')],
        "wrong-compare-rate-only": [('ok: regressions.length === 0 && removed.length === 0 }', 'ok: current.pass_rate >= baseline.pass_rate }')],
        "wrong-compare-removed-ignored": [('ok: regressions.length === 0 && removed.length === 0 }', 'ok: regressions.length === 0 }')],
        "wrong-flaky-any-run": [('const passed = runs.every((r) => r.passed);', 'const passed = runs.some((r) => r.passed);')],
    }),
    "java": ("Harness.java", {
        "wrong-regex-full-match": [('.matcher(output).find()', '.matcher(output).matches()')],
        "wrong-exact-case-sensitive": [('replaceAll("\\\\s+", " ").toLowerCase();', 'replaceAll("\\\\s+", " ");')],
        "wrong-exact-contains": [('norm(output).equals(norm((String) check.get("expected")))', 'norm(output).contains(norm((String) check.get("expected")))')],
        "wrong-json-loose-type": [('Objects.equals(obj.get((String) check.get("field")), check.get("equals"))', 'String.valueOf(obj.get((String) check.get("field"))).equals(String.valueOf(check.get("equals")))')],
        "wrong-json-fence-ok": [('data = Json.parse(output);', 'data = Json.parse(output.replace("```json", "").replace("```", ""));')],
        "wrong-judge-first-digit": [('String text = reply == null ? "" : reply.strip();', 'String text = reply == null ? "" : reply.strip().replaceAll("[^1-5]*([1-5]).*", "$1");')],
        "wrong-judge-strict-threshold": [('return score >= threshold ?', 'return score > threshold ?')],
        "wrong-model-error-passes": [('runs.add(map("passed", false, "reason", "model error"));', 'runs.add(map("passed", true, "reason", "ok"));')],
        "wrong-tag-total": [('row.put("total", (Integer) row.get("total") + 1);', 'row.put("total", (Integer) row.get("total") + (passed ? 1 : 0));')],
        "wrong-meets-at-or-below": [('< min.doubleValue()) failures.add("overall");', '<= min.doubleValue()) failures.add("overall");')],
        "wrong-meets-missing-tag-ok": [('                if (row == null) {\n                    failures.add("tag:" + e.getKey());\n                    continue;\n                }', '                if (row == null) continue;')],
        "wrong-compare-rate-only": [('"ok", regressions.isEmpty() && removed.isEmpty());', '"ok", ((Number) current.get("pass_rate")).doubleValue() >= ((Number) baseline.get("pass_rate")).doubleValue());')],
        "wrong-compare-removed-ignored": [('"ok", regressions.isEmpty() && removed.isEmpty());', '"ok", regressions.isEmpty());')],
        "wrong-flaky-any-run": [('boolean passed = runs.stream().allMatch(', 'boolean passed = runs.stream().anyMatch(')],
    }),
    "kotlin": ("Harness.kt", {
        "wrong-regex-full-match": [('.containsMatchIn(output)', '.matches(output)')],
        "wrong-exact-case-sensitive": [('.replace(Regex("\\\\s+"), " ").lowercase()', '.replace(Regex("\\\\s+"), " ")')],
        "wrong-exact-contains": [('norm(output) == norm(check["expected"] as String)', 'norm(output).contains(norm(check["expected"] as String))')],
        "wrong-json-loose-type": [('obj[field] == check["equals"]', 'obj[field].toString() == check["equals"].toString()')],
        "wrong-json-fence-ok": [('val data = try { Json.parse(output) }', 'val data = try { Json.parse(output.replace("```json", "").replace("```", "")) }')],
        "wrong-judge-first-digit": [('val text = reply.trim()', 'val text = Regex("[1-5]").find(reply)?.value ?: reply.trim()')],
        "wrong-judge-strict-threshold": [('if (score >= threshold)', 'if (score > threshold)')],
        "wrong-model-error-passes": [('linkedMapOf<String, Any?>("passed" to false, "reason" to "model error")', 'linkedMapOf<String, Any?>("passed" to true, "reason" to "ok")')],
        "wrong-tag-total": [('row["total"] = (row["total"] as Int) + 1', 'row["total"] = (row["total"] as Int) + if (passed) 1 else 0')],
        "wrong-meets-at-or-below": [('(report["pass_rate"] as Number).toDouble() < min.toDouble()', '(report["pass_rate"] as Number).toDouble() <= min.toDouble()')],
        "wrong-meets-missing-tag-ok": [('if (row == null || total == 0.0 || (row["passed"] as Number)', 'if (row != null && total > 0.0 && (row["passed"] as Number)')],
        "wrong-compare-rate-only": [('"ok" to (regressions.isEmpty() && removed.isEmpty()))', '"ok" to ((current["pass_rate"] as Number).toDouble() >= (baseline["pass_rate"] as Number).toDouble()))')],
        "wrong-compare-removed-ignored": [('"ok" to (regressions.isEmpty() && removed.isEmpty()))', '"ok" to regressions.isEmpty())')],
        "wrong-flaky-any-run": [('val passed = runs.all { it["passed"] == true }', 'val passed = runs.any { it["passed"] == true }')],
    }),
}

PLANTS[f"{X}/43-debugging-claude-applications/unit-01/practice-1"] = {
    "python": ("diagnose.py", {
        "wrong-retry-400": [('400: ("invalid_request", "integration", "fix_request"),', '400: ("invalid_request", "integration", "retry_backoff"),')],
        "wrong-413-service": [('413: ("request_too_large", "integration", "shrink_request"),', '413: ("request_too_large", "service", "shrink_request"),')],
        "wrong-429-always-retry": [('if event.get("error_code") == "enforced_spend_limit_reached":', "if False:")],
        "wrong-spend-400-ignored": [('if status == 400 and "spend limit" in event.get("message", "").lower():', "if False:")],
        "wrong-maxtokens-model": [('"max_tokens": ("truncated", "integration", "raise_max_tokens"),', '"max_tokens": ("truncated", "model", "raise_max_tokens"),')],
        "wrong-refusal-retry": [('"refusal": ("refusal", "model", "fallback_model"),', '"refusal": ("refusal", "model", "retry_backoff"),')],
        "wrong-empty-always-model": [('if "tool_result" in last_blocks and "text" in last_blocks[last_blocks.index("tool_result"):]:', "if False:")],
        "wrong-empty-text-anywhere": [('if "tool_result" in last_blocks and "text" in last_blocks[last_blocks.index("tool_result"):]:', 'if "tool_result" in last_blocks and "text" in last_blocks:')],
        "wrong-parse-always-model": [('if _has_json_object(event.get("text", "")):', "if False:")],
        "wrong-parse-braces-only": [('return isinstance(json.loads(text[start:end + 1]), dict)', "return True")],
        "wrong-unknown-tool-integration": [('return ("unknown_tool", "model", "return_error_result")', 'return ("unknown_tool", "integration", "return_error_result")')],
        "wrong-tool-error-flagged": [('if kind == "tool_result" and event.get("exception"):', 'if kind == "tool_result" and (event.get("exception") or event.get("is_error")):')],
        "wrong-last-failure": [("for i, event in enumerate(trace):", "for i, event in reversed(list(enumerate(trace))):")],
        "wrong-recovered-ignored": [("recovered = any(", "recovered = False and any(")],
        "wrong-recovered-empty": [(' and e.get("stop_reason") == "end_turn" and e.get("content")', ' and e.get("stop_reason") == "end_turn"')],
        "wrong-network-integration": [('return ("network", "service", "retry_backoff")', 'return ("network", "integration", "retry_backoff")')],
    }),
    "typescript": ("diagnose.ts", {
        "wrong-retry-400": [('400: ["invalid_request", "integration", "fix_request"],', '400: ["invalid_request", "integration", "retry_backoff"],')],
        "wrong-413-service": [('413: ["request_too_large", "integration", "shrink_request"],', '413: ["request_too_large", "service", "shrink_request"],')],
        "wrong-429-always-retry": [('if (event.error_code === "enforced_spend_limit_reached") return', "if (false) return")],
        "wrong-spend-400-ignored": [('if (status === 400 && String(event.message ?? "").toLowerCase().includes("spend limit")) return', "if (false) return")],
        "wrong-maxtokens-model": [('max_tokens: ["truncated", "integration", "raise_max_tokens"],', 'max_tokens: ["truncated", "model", "raise_max_tokens"],')],
        "wrong-refusal-retry": [('refusal: ["refusal", "model", "fallback_model"],', 'refusal: ["refusal", "model", "retry_backoff"],')],
        "wrong-empty-always-model": [('if (at >= 0 && lastBlocks.slice(at).includes("text")) return', "if (false) return")],
        "wrong-empty-text-anywhere": [('if (at >= 0 && lastBlocks.slice(at).includes("text")) return', 'if (at >= 0 && lastBlocks.includes("text")) return')],
        "wrong-parse-always-model": [('return hasJsonObject(String(event.text ?? "")) ?', "return false ?")],
        "wrong-parse-braces-only": [('    const value = JSON.parse(text.slice(start, end + 1));\n    return typeof value === "object" && value !== null && !Array.isArray(value);', "    return true;")],
        "wrong-unknown-tool-integration": [('["unknown_tool", "model", "return_error_result"]', '["unknown_tool", "integration", "return_error_result"]')],
        "wrong-tool-error-flagged": [('kind === "tool_result" && event.exception)', 'kind === "tool_result" && (event.exception || event.is_error))')],
        "wrong-last-failure": [("for (let i = 0; i < trace.length; i++) {\n    const event = trace[i];", "for (let i = trace.length - 1; i >= 0; i--) {\n    const event = trace[i];")],
        "wrong-recovered-ignored": [("const recovered = trace.slice(i + 1).some(", "const recovered = false && trace.slice(i + 1).some(")],
        "wrong-recovered-empty": [('e.stop_reason === "end_turn" && e.content && e.content.length > 0);', 'e.stop_reason === "end_turn");')],
        "wrong-network-integration": [('if (kind === "network_error") return ["network", "service", "retry_backoff"];', 'if (kind === "network_error") return ["network", "integration", "retry_backoff"];')],
    }),
    "java": ("Diagnose.java", {
        "wrong-retry-400": [('400, new String[] {"invalid_request", "integration", "fix_request"},', '400, new String[] {"invalid_request", "integration", "retry_backoff"},')],
        "wrong-413-service": [('413, new String[] {"request_too_large", "integration", "shrink_request"},', '413, new String[] {"request_too_large", "service", "shrink_request"},')],
        "wrong-429-always-retry": [('if ("enforced_spend_limit_reached".equals(event.get("error_code"))) return', "if (false) return")],
        "wrong-spend-400-ignored": [("if (status == 400 && String.valueOf(", "if (false && String.valueOf(")],
        "wrong-maxtokens-model": [('"max_tokens", new String[] {"truncated", "integration", "raise_max_tokens"},', '"max_tokens", new String[] {"truncated", "model", "raise_max_tokens"},')],
        "wrong-refusal-retry": [('"refusal", new String[] {"refusal", "model", "fallback_model"},', '"refusal", new String[] {"refusal", "model", "retry_backoff"},')],
        "wrong-empty-always-model": [('if (at >= 0 && lastBlocks.subList(at, lastBlocks.size()).contains("text")) return', "if (false) return")],
        "wrong-empty-text-anywhere": [('if (at >= 0 && lastBlocks.subList(at, lastBlocks.size()).contains("text")) return', 'if (at >= 0 && lastBlocks.contains("text")) return')],
        "wrong-parse-always-model": [('return hasJsonObject(String.valueOf(event.getOrDefault("text", ""))) ?', "return false ?")],
        "wrong-parse-braces-only": [("return Json.parse(text.substring(start, end + 1)) instanceof Map<?, ?>;", "return true;")],
        "wrong-unknown-tool-integration": [('new String[] {"unknown_tool", "model", "return_error_result"}', 'new String[] {"unknown_tool", "integration", "return_error_result"}')],
        "wrong-tool-error-flagged": [('return event.get("exception") instanceof String s && !s.isEmpty() ?', 'return (event.get("exception") instanceof String s && !s.isEmpty() || Boolean.TRUE.equals(event.get("is_error"))) ?')],
        "wrong-last-failure": [("for (int i = 0; i < trace.size(); i++) {", "for (int i = trace.size() - 1; i >= 0; i--) {")],
        "wrong-recovered-ignored": [('if ("response".equals(e.get("kind")) &&', 'if (false && "response".equals(e.get("kind")) &&')],
        "wrong-recovered-empty": [('"end_turn".equals(e.get("stop_reason"))\n                        && e.get("content") instanceof List<?> c && !c.isEmpty()) recovered = true;', '"end_turn".equals(e.get("stop_reason"))) recovered = true;')],
        "wrong-network-integration": [('return new String[] {"network", "service", "retry_backoff"};', 'return new String[] {"network", "integration", "retry_backoff"};')],
    }),
    "kotlin": ("Diagnose.kt", {
        "wrong-retry-400": [('400 to listOf("invalid_request", "integration", "fix_request"),', '400 to listOf("invalid_request", "integration", "retry_backoff"),')],
        "wrong-413-service": [('413 to listOf("request_too_large", "integration", "shrink_request"),', '413 to listOf("request_too_large", "service", "shrink_request"),')],
        "wrong-429-always-retry": [('if (event["error_code"] == "enforced_spend_limit_reached") return', "if (false) return")],
        "wrong-spend-400-ignored": [('if (status == 400 && (event["message"]', 'if (false && (event["message"]')],
        "wrong-maxtokens-model": [('"max_tokens" to listOf("truncated", "integration", "raise_max_tokens"),', '"max_tokens" to listOf("truncated", "model", "raise_max_tokens"),')],
        "wrong-refusal-retry": [('"refusal" to listOf("refusal", "model", "fallback_model"),', '"refusal" to listOf("refusal", "model", "retry_backoff"),')],
        "wrong-empty-always-model": [('return if (at >= 0 && "text" in lastBlocks.subList(at, lastBlocks.size)) listOf(', "return if (false) listOf(")],
        "wrong-empty-text-anywhere": [('return if (at >= 0 && "text" in lastBlocks.subList(at, lastBlocks.size)) listOf(', 'return if (at >= 0 && "text" in lastBlocks) listOf(')],
        "wrong-parse-always-model": [('return if (hasJsonObject(event["text"] as? String ?: "")) listOf(', "return if (false) listOf(")],
        "wrong-parse-braces-only": [("return try { Json.parse(text.substring(start, end + 1)) is Map<*, *> } catch", "return try { true } catch")],
        "wrong-unknown-tool-integration": [('listOf("unknown_tool", "model", "return_error_result")', 'listOf("unknown_tool", "integration", "return_error_result")')],
        "wrong-tool-error-flagged": [('"tool_result" -> if (!(event["exception"] as? String).isNullOrEmpty()) return', '"tool_result" -> if (!(event["exception"] as? String).isNullOrEmpty() || event["is_error"] == true) return')],
        "wrong-last-failure": [("for ((i, event) in trace.withIndex()) {", "for ((i, event) in trace.withIndex().reversed()) {")],
        "wrong-recovered-ignored": [("val recovered = trace.drop(i + 1).any {", "val recovered = false && trace.drop(i + 1).any {")],
        "wrong-recovered-empty": [(' && it["stop_reason"] == "end_turn" && !(it["content"] as? List<*>).isNullOrEmpty()', ' && it["stop_reason"] == "end_turn"')],
        "wrong-network-integration": [('"network_error" -> return listOf("network", "service", "retry_backoff")', '"network_error" -> return listOf("network", "integration", "retry_backoff")')],
    }),
}

# --- PLANTS ABOVE ---


# ===== Level 3: modules 45 to 56 =====
PLANTS[f"{X}/45-the-agentic-loop-in-depth/unit-01/practice-1"] = {
    "python": ("agent.py", {
        "wrong-text-marker": [('        reason = reply["stop_reason"]\n        if reason == "tool_use":', '        reason = reply["stop_reason"]\n        if "done" in last_text.lower():\n            return {"status": "done", "text": last_text, "turns": turns, "messages": messages}\n        if reason == "tool_use":')],
        "wrong-cap-reports-done": [('return {"status": "max_turns", "text"', 'return {"status": "done", "text"')],
        "wrong-cap-off-by-one": [("if turns >= max_turns:", "if turns > max_turns:")],
        "wrong-results-split": [('            messages.append({"role": "user", "content": [_run_tool(block, tools) for block in calls]})', '            for block in calls:\n                messages.append({"role": "user", "content": [_run_tool(block, tools)]})')],
        "wrong-error-without-flag": [('"content": str(error), "is_error": True}', '"content": str(error)}')],
        "wrong-truncated-is-done": [('elif reason in ("end_turn", "stop_sequence"):', 'elif reason in ("end_turn", "stop_sequence", "max_tokens"):')],
        "wrong-malformed-continues": [('            if not calls:\n                return {"status": "malformed", "text": last_text, "turns": turns, "messages": messages}\n', "")],
    }),
    "typescript": ("agent.ts", {
        "wrong-text-marker": [('    const reason = reply.stop_reason;\n    if (reason === "tool_use") {', '    const reason = reply.stop_reason;\n    if (lastText.toLowerCase().includes("done")) return { status: "done", text: lastText, turns, messages };\n    if (reason === "tool_use") {')],
        "wrong-cap-reports-done": [('{ status: "max_turns", text', '{ status: "done", text')],
        "wrong-cap-off-by-one": [("if (turns >= maxTurns)", "if (turns > maxTurns)")],
        "wrong-results-split": [('messages.push({ role: "user", content: calls.map((b) => runTool(b, tools)) });', 'for (const b of calls) messages.push({ role: "user", content: [runTool(b, tools)] });')],
        "wrong-error-without-flag": [('content: error instanceof Error ? error.message : String(error), is_error: true }', 'content: error instanceof Error ? error.message : String(error) }')],
        "wrong-truncated-is-done": [('reason === "end_turn" || reason === "stop_sequence"', 'reason === "end_turn" || reason === "stop_sequence" || reason === "max_tokens"')],
        "wrong-malformed-continues": [('      if (calls.length === 0) return { status: "malformed", text: lastText, turns, messages };\n', "")],
    }),
    "java": ("AgentLoop.java", {
        "wrong-text-marker": [('            String reason = (String) reply.get("stop_reason");\n', '            String reason = (String) reply.get("stop_reason");\n            if (lastText.toLowerCase().contains("done")) return outcome("done", lastText, turns, messages);\n')],
        "wrong-cap-reports-done": [('outcome("max_turns", lastText', 'outcome("done", lastText')],
        "wrong-cap-off-by-one": [("if (turns >= maxTurns)", "if (turns > maxTurns)")],
        "wrong-results-split": [('                    List<Map<String, Object>> results = new ArrayList<>();\n                    for (Map<String, Object> block : content) if ("tool_use".equals(block.get("type"))) results.add(runTool(block, tools));\n                    if (results.isEmpty()) return outcome("malformed", lastText, turns, messages);\n                    messages.add(map("role", "user", "content", results));',
                                 '                    int found = 0;\n                    for (Map<String, Object> block : content) if ("tool_use".equals(block.get("type"))) { found++; messages.add(map("role", "user", "content", List.of(runTool(block, tools)))); }\n                    if (found == 0) return outcome("malformed", lastText, turns, messages);')],
        "wrong-error-without-flag": [('"content", String.valueOf(error.getMessage()), "is_error", true);', '"content", String.valueOf(error.getMessage()));')],
        "wrong-truncated-is-done": [('case "max_tokens" -> { return outcome("truncated", lastText, turns, messages); }', 'case "max_tokens" -> { return outcome("done", lastText, turns, messages); }')],
        "wrong-malformed-continues": [('                    if (results.isEmpty()) return outcome("malformed", lastText, turns, messages);\n', "")],
    }),
    "kotlin": ("AgentLoop.kt", {
        "wrong-text-marker": [('        when (reply["stop_reason"]) {', '        if (lastText.lowercase().contains("done")) return outcome("done", lastText, turns, messages)\n        when (reply["stop_reason"]) {')],
        "wrong-cap-reports-done": [('outcome("max_turns", lastText', 'outcome("done", lastText')],
        "wrong-cap-off-by-one": [("if (turns >= maxTurns)", "if (turns > maxTurns)")],
        "wrong-results-split": [('                messages.add(linkedMapOf("role" to "user", "content" to calls.map { runTool(it, tools) }))', '                calls.forEach { messages.add(linkedMapOf("role" to "user", "content" to listOf(runTool(it, tools)))) }')],
        "wrong-error-without-flag": [('"content" to error.message.toString(), "is_error" to true)', '"content" to error.message.toString())')],
        "wrong-truncated-is-done": [('"end_turn", "stop_sequence" ->', '"end_turn", "stop_sequence", "max_tokens" ->')],
        "wrong-malformed-continues": [('                if (calls.isEmpty()) return outcome("malformed", lastText, turns, messages)\n', "")],
    }),
}

PLANTS[f"{X}/46-coordinator-and-subagents/unit-01/practice-1"] = {
    "python": ("coordinator.py", {
        "wrong-leaks-context": [('        run(task["scope"], task["brief"])\n', '        run(task["scope"], task["brief"] + "".join("\\n" + f["text"] for f in findings))\n')],
        "wrong-no-dedupe": [("        elif key in seen:", "        elif False:")],
        "wrong-empty-brief-sent": [("        if not brief.strip():", "        if False:")],
        "wrong-always-delegates": [('    if not plan.get("delegate"):', "    if False:")],
        "wrong-failure-as-finding": [('            failed.append({"scope": scope, "error": str(error)})\n            return', '            findings.append({"scope": scope, "text": str(error)})\n            return')],
        "wrong-synthesizes-nothing": [("    if not findings:\n", "    if False:\n")],
        "wrong-rerun-all": [("        rounds += 1\n        for gap in gaps:", '        rounds += 1\n        for task in tasks:\n            run(task["scope"], task["brief"])\n        for gap in gaps:')],
        "wrong-rounds-off-by-one": [("while gaps and rounds < max_rounds:", "while gaps and rounds <= max_rounds:")],
    }),
    "typescript": ("coordinator.ts", {
        "wrong-leaks-context": [("for (const task of kept) run(task.scope, task.brief);", 'for (const task of kept) run(task.scope, task.brief + findings.map((f) => "\\n" + f.text).join(""));')],
        "wrong-no-dedupe": [("else if (seen.has(key)) dropped.push", "else if (false) dropped.push")],
        "wrong-empty-brief-sent": [('if (brief.trim() === "") dropped.push', "if (false) dropped.push")],
        "wrong-always-delegates": [("if (!plan.delegate) {", "if (false) {")],
        "wrong-failure-as-finding": [("failed.push({ scope, error: error instanceof Error ? error.message : String(error) });\n      return;", "findings.push({ scope, text: error instanceof Error ? error.message : String(error) });\n      return;")],
        "wrong-synthesizes-nothing": [("if (findings.length === 0) return {", "if (false) return {")],
        "wrong-rerun-all": [("    rounds += 1;\n    for (const gap of gaps) run(", "    rounds += 1;\n    for (const task of kept) run(task.scope, task.brief);\n    for (const gap of gaps) run(")],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
    "java": ("Coordinator.java", {
        "wrong-leaks-context": [('run.accept((String) task.get("scope"), (String) task.get("brief"));', 'run.accept((String) task.get("scope"), (String) task.get("brief") + findings.stream().map(f -> "\\n" + f.get("text")).collect(java.util.stream.Collectors.joining()));')],
        "wrong-no-dedupe": [("else if (seen.contains(key)) dropped", "else if (false) dropped")],
        "wrong-empty-brief-sent": [("if (brief.isBlank()) dropped.add", "if (false) dropped.add")],
        "wrong-always-delegates": [('if (!Boolean.TRUE.equals(plan.get("delegate"))) {', "if (false) {")],
        "wrong-failure-as-finding": [('failed.add(map("scope", scope, "error", String.valueOf(error.getMessage())));\n                return;', 'findings.add(map("scope", scope, "text", String.valueOf(error.getMessage())));\n                return;')],
        "wrong-synthesizes-nothing": [("if (findings.isEmpty()) {", "if (false) {")],
        "wrong-rerun-all": [("            rounds++;\n            for (String gap : gaps) run.accept(", '            rounds++;\n            for (Map<String, Object> task : tasks) run.accept((String) task.get("scope"), (String) task.get("brief"));\n            for (String gap : gaps) run.accept(')],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
    "kotlin": ("Coordinator.kt", {
        "wrong-leaks-context": [("for ((scope, brief) in tasks) run(scope, brief)", 'for ((scope, brief) in tasks) run(scope, brief + findings.joinToString("") { "\\n" + it["text"] })')],
        "wrong-no-dedupe": [("key in seen -> dropped.add", "false -> dropped.add")],
        "wrong-empty-brief-sent": [("brief.isBlank() -> dropped.add", "false -> dropped.add")],
        "wrong-always-delegates": [('if (plan["delegate"] != true) {', "if (false) {")],
        "wrong-failure-as-finding": [('failed.add(linkedMapOf("scope" to scope, "error" to error.message.toString()))\n            return', 'findings.add(linkedMapOf("scope" to scope, "text" to error.message.toString()))\n            return')],
        "wrong-synthesizes-nothing": [("if (findings.isEmpty()) {", "if (false) {")],
        "wrong-rerun-all": [("        rounds++\n        for (gap in gaps) run(", "        rounds++\n        for ((scope, brief) in tasks) run(scope, brief)\n        for (gap in gaps) run(")],
        "wrong-rounds-off-by-one": [("rounds < maxRounds", "rounds <= maxRounds")],
    }),
}

PLANTS[f"{X}/47-invoking-subagents/unit-01/practice-1"] = {
    "python": ("subagents.py", {
        "wrong-inherits-all-tools": [('tools = list(READ_ONLY) if listed is None else [t for t in listed if t != "Agent"]', "tools = listed")],
        "wrong-allows-nesting": [('[t for t in listed if t != "Agent"]', "list(listed)")],
        "wrong-name-unchecked": [('if not re.fullmatch(r"[a-z][a-z0-9-]*", name):', "if False:")],
        "wrong-no-depth-limit": [('"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", ', "")],
        "wrong-budget-dropped": [("max_budget_usd=max_budget_usd, ", "")],
        "wrong-only-new-name": [('SPAWN_TOOLS = ("Agent", "Task")', 'SPAWN_TOOLS = ("Agent",)')],
        "wrong-counts-everything": [('parent = getattr(message, "parent_tool_use_id", None)\n', 'parent = getattr(message, "parent_tool_use_id", None) or next(iter(groups), None)\n')],
        "wrong-brief-drops-facts": [('(("Files", files), ("Known", facts))', '(("Files", files),)')],
        "wrong-first-source-only": [('if finding["source"] is not None and finding["source"] not in entry["sources"]:', 'if finding["source"] is not None and not entry["sources"]:')],
        "wrong-source-in-claim": [('return {"claim": claim.strip(), "source": source or None}', 'return {"claim": claim.strip() + (f" ({url})" if url else ""), "source": source or None}')],
    }),
    "typescript": ("subagents.ts", {
        "wrong-inherits-all-tools": [('const tools = spec.tools == null ? [...READ_ONLY] : spec.tools.filter((t) => t !== "Agent");', "const tools = spec.tools;")],
        "wrong-allows-nesting": [('spec.tools.filter((t) => t !== "Agent")', "[...spec.tools]")],
        "wrong-name-unchecked": [("if (!/^[a-z][a-z0-9-]*$/.test(name)) throw", "if (false) throw")],
        "wrong-no-depth-limit": [('CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1", ', "")],
        "wrong-budget-dropped": [("maxTurns, maxBudgetUsd, cwd,", "maxTurns, cwd,")],
        "wrong-only-new-name": [('const SPAWN_TOOLS = ["Agent", "Task"];', 'const SPAWN_TOOLS = ["Agent"];')],
        "wrong-counts-everything": [("const parent = message?.parent_tool_use_id;\n", "const parent = message?.parent_tool_use_id ?? Object.keys(groups)[0];\n")],
        "wrong-brief-drops-facts": [('[["Files", files], ["Known", facts]] as const', '[["Files", files]] as const')],
        "wrong-first-source-only": [("if (finding.source !== null && !entry.sources.some(", "if (finding.source !== null && entry.sources.length === 0 && !entry.sources.some(")],
        "wrong-source-in-claim": [("return { claim: claim.trim(), source:", 'return { claim: claim.trim() + (url ? ` (${url})` : ""), source:')],
    }),
}

PLANTS[f"{X}/48-multi-step-workflows-with-guarantees/unit-01/practice-1"] = {
    "python": ("desk.py", {
        "wrong-identity-unchecked": [('        if self._customer is None:\n            return self._block(name, "identity_required")\n', "")],
        "wrong-ownership-unchecked": [('            if result.get("customer_id") != self._customer:\n                return self._block(name, "order_not_owned")\n', "")],
        "wrong-over-limit-executes": [('        if amount > self.limit:\n            return self._block(name, "needs_human")\n', "")],
        "wrong-failure-unlocks": [("                self._customer, self._failures = None, self._failures + 1", "                self._failures = self._failures + 1")],
        "wrong-no-lockout": [("                self._locked = self._failures >= 3", "                self._locked = False")],
        "wrong-exceeds-ignored": [('        if amount > order["total_cents"] - order["refunded_cents"]:\n            return self._block(name, "exceeds_order")\n', "")],
        "wrong-zero-amount-ok": [("or amount <= 0:", "or amount < 0:")],
        "wrong-handoff-no-blocks": [('"refunds_done": [dict(r) for r in self._refunds], "blocked": [dict(b) for b in self._blocked], "recommended_action"', '"refunds_done": [dict(r) for r in self._refunds], "blocked": [], "recommended_action"')],
    }),
    "typescript": ("desk.ts", {
        "wrong-identity-unchecked": [('    if (this.customer === null) return this.block(name, "identity_required");\n', "")],
        "wrong-ownership-unchecked": [('      if (result!.customer_id !== this.customer) return this.block(name, "order_not_owned");\n', "")],
        "wrong-over-limit-executes": [('    if (amount > this.limitCents) return this.block(name, "needs_human");\n', "")],
        "wrong-failure-unlocks": [("        this.customer = null;\n        this.failures += 1;", "        this.failures += 1;")],
        "wrong-no-lockout": [("this.locked = this.failures >= 3;", "this.locked = false;")],
        "wrong-exceeds-ignored": [('    if (amount > order.total_cents - order.refunded_cents) return this.block(name, "exceeds_order");\n', "")],
        "wrong-zero-amount-ok": [("!Number.isInteger(amount) || amount <= 0", "!Number.isInteger(amount) || amount < 0")],
        "wrong-handoff-no-blocks": [("      blocked: structuredClone(this.blockedCalls), recommended_action: action };", "      blocked: [], recommended_action: action };")],
    }),
    "java": ("RefundDesk.java", {
        "wrong-identity-unchecked": [('        if (customer == null) return block(name, "identity_required", null);\n', '        if (customer == null && name.equals("lookup_order")) return block(name, "identity_required", null);\n')],
        "wrong-ownership-unchecked": [('            if (!customer.equals(result.get("customer_id"))) return block(name, "order_not_owned", null);\n', "")],
        "wrong-over-limit-executes": [('        if (amount > limitCents) return block(name, "needs_human", null);\n', "")],
        "wrong-failure-unlocks": [("                customer = null;\n                failures++;", "                failures++;")],
        "wrong-no-lockout": [("locked = failures >= 3;", "locked = false;")],
        "wrong-exceeds-ignored": [('        if (amount > (Integer) order.get("total_cents") - refunded) return block(name, "exceeds_order", null);\n', "")],
        "wrong-zero-amount-ok": [("|| amount <= 0) return block", "|| amount < 0) return block")],
        "wrong-handoff-no-blocks": [('"refunds_done", copies(refunds), "blocked", copies(blocked), "recommended_action", action);', '"refunds_done", copies(refunds), "blocked", new ArrayList<>(), "recommended_action", action);')],
    }),
    "kotlin": ("RefundDesk.kt", {
        "wrong-identity-unchecked": [('val who = customer ?: return block(name, "identity_required")', "val who = customer")],
        "wrong-ownership-unchecked": [('if (result!!["customer_id"] != who) return block(name, "order_not_owned")', 'if (result!!["customer_id"] == null) return block(name, "order_not_owned")')],
        "wrong-over-limit-executes": [('        if (amount > limitCents) return block(name, "needs_human")\n', "")],
        "wrong-failure-unlocks": [("                customer = null\n                failures++", "                failures++")],
        "wrong-no-lockout": [("locked = failures >= 3", "locked = false")],
        "wrong-exceeds-ignored": [('        if (amount > (order["total_cents"] as Int) - refunded) return block(name, "exceeds_order")\n', "")],
        "wrong-zero-amount-ok": [("if (amount == null || amount <= 0)", "if (amount == null || amount < 0)")],
        "wrong-handoff-no-blocks": [('"blocked" to blocked.map { LinkedHashMap(it) }, "recommended_action" to action)', '"blocked" to emptyList<Any?>(), "recommended_action" to action)')],
    }),
}

PLANTS[f"{X}/49-hooks/unit-01/practice-1"] = {
    "python": ("hooks.py", {
        "wrong-200-asks": [("if amount <= AUTO_LIMIT:", "if amount < AUTO_LIMIT:")],
        "wrong-500-denied": [("if amount <= ASK_LIMIT:", "if amount < ASK_LIMIT:")],
        "wrong-missing-amount-allowed": [('.get("amount")\n    if isinstance(amount, bool)', '.get("amount", 0.01)\n    if isinstance(amount, bool)')],
        "wrong-coerces-amount": [('.get("amount")\n    if isinstance(amount, bool)', '.get("amount")\n    if isinstance(amount, (str, bool)):\n        amount = float(amount)\n    if isinstance(amount, bool)')],
        "wrong-gates-every-tool": [('    if input_data.get("tool_name") != "process_refund":\n        return {}\n', "")],
        "wrong-always-milliseconds": [("seconds = value / 1000 if value > 1e11 else value", "seconds = value / 1000")],
        "wrong-cents-kept": [('cents = out.pop("amount_cents", None)', 'cents = out.get("amount_cents")')],
        "wrong-rewrites-plain-text": [("    except ValueError:\n        return {}\n    if not isinstance(data, dict):", '    except ValueError:\n        return _answer("PostToolUse", updatedToolOutput="null")\n    if not isinstance(data, dict):')],
        "wrong-not-idempotent": [("    if out == data:\n        return {}\n", "")],
        "wrong-no-matcher": [('HookMatcher(matcher="process_refund", ', "HookMatcher(")],
        "wrong-no-timeout": [("hooks=[pre_refund], timeout=5", "hooks=[pre_refund]")],
        "wrong-exit-one": [('return {"exit": 2, "stderr": reason}', 'return {"exit": 1, "stderr": reason}')],
        "wrong-bad-input-passes": [('return {"exit": 2, "stderr": "The hook input', 'return {"exit": 0, "stderr": "The hook input')],
        "wrong-settings-no-timeout": [('"command": script, "timeout": timeout}', '"command": script}')],
    }),
    "typescript": ("hooks.ts", {
        "wrong-200-asks": [("if (amount <= AUTO_LIMIT)", "if (amount < AUTO_LIMIT)")],
        "wrong-500-denied": [("if (amount <= ASK_LIMIT)", "if (amount < ASK_LIMIT)")],
        "wrong-missing-amount-allowed": [("const amount = input?.tool_input?.amount;", "const amount = input?.tool_input?.amount ?? 0.01;")],
        "wrong-coerces-amount": [("const amount = input?.tool_input?.amount;", 'const given = input?.tool_input?.amount;\n  const amount = typeof given === "string" || typeof given === "boolean" ? Number(given) : given;')],
        "wrong-gates-every-tool": [('  if (input?.tool_name !== "process_refund") return {};\n', "")],
        "wrong-always-milliseconds": [("new Date(value > 1e11 ? value : value * 1000)", "new Date(value)")],
        "wrong-cents-kept": [("    delete out.amount_cents;\n", "")],
        "wrong-rewrites-plain-text": [("    } catch {\n      return {};\n    }\n  }\n  if (data === null", '    } catch {\n      return answer("PostToolUse", { updatedToolOutput: "null" });\n    }\n  }\n  if (data === null')],
        "wrong-not-idempotent": [("return changed ? answer(", "return true ? answer(")],
        "wrong-no-matcher": [('{ matcher: "process_refund", hooks', "{ hooks")],
        "wrong-no-timeout": [("hooks: [preRefund], timeout: 5", "hooks: [preRefund]")],
        "wrong-exit-one": [("return { exit: 2, stderr: reason }", "return { exit: 1, stderr: reason }")],
        "wrong-bad-input-passes": [("return { exit: 2, stderr: \"The hook input", "return { exit: 0, stderr: \"The hook input")],
        "wrong-settings-no-timeout": [("command: script, timeout }", "command: script }")],
    }),
}

PLANTS[f"{X}/50-task-decomposition/unit-01/practice-1"] = {
    "python": ("decompose.py", {
        "wrong-shared-context": [('chunk = "\\n".join(lines[part * max_lines:(part + 1) * max_lines])', 'chunk = "\\n".join(i["text"] for i in files)')],
        "wrong-cross-gets-text": [('{"path": p, "summary": r["summary"]} for p, r in reviewed.items()', '{"path": p, "summary": r["summary"], "text": "source"} for p, r in reviewed.items()')],
        "wrong-no-chunking": [("parts = math.ceil(len(lines) / max_lines)", "parts, max_lines = 1, len(lines)")],
        "wrong-blank-reviewed": [("if not text.strip():", "if not text:")],
        "wrong-failed-in-cross": [("failed[path] = str(error)\n            continue", 'failed[path] = str(error)\n            reviewed[path] = {"findings": [], "summary": "", "parts": parts}\n            continue')],
        "wrong-cross-with-one": [("if len(reviewed) >= 2:", "if len(reviewed) >= 1:")],
        "wrong-no-history": [("plan = planner(goal, [dict(step) for step in steps])", "plan = planner(goal, [])")],
        "wrong-repeat-allowed": [('        if subtask.lower() in {step["subtask"].lower() for step in steps}:\n            return finish("stuck", reason=f"repeated subtask: {subtask}")\n', "")],
        "wrong-limit-off-by-one": [("if len(steps) >= max_steps:", "if len(steps) > max_steps:")],
        "wrong-strategy-items-first": [('    if not steps_known:\n        return "adaptive"\n    if items >= 2 and task.get("items_interact") is True:\n        return "per_item_then_cross"\n',
                                         '    if items >= 2 and task.get("items_interact") is True:\n        return "per_item_then_cross"\n    if not steps_known:\n        return "adaptive"\n')],
    }),
    "typescript": ("decompose.ts", {
        "wrong-shared-context": [('lines.slice(part * maxLines, (part + 1) * maxLines).join("\\n")', 'files.map((f) => f.text).join("\\n")')],
        "wrong-cross-gets-text": [("({ path, summary: r.summary })", '({ path, summary: r.summary, text: "source" })')],
        "wrong-no-chunking": [("const parts = Math.ceil(lines.length / maxLines);", "const parts = 1;\n    maxLines = lines.length;")],
        "wrong-blank-reviewed": [("if (!text.trim()) {", "if (!text) {")],
        "wrong-failed-in-cross": [("failed[path] = error instanceof Error ? error.message : String(error);\n      continue;", 'failed[path] = error instanceof Error ? error.message : String(error);\n      reviewed[path] = { findings: [], summary: "", parts };\n      continue;')],
        "wrong-cross-with-one": [("if (Object.keys(reviewed).length >= 2) {", "if (Object.keys(reviewed).length >= 1) {")],
        "wrong-no-history": [("const plan = planner(goal, steps.map((s) => ({ ...s })));", "const plan = planner(goal, []);")],
        "wrong-repeat-allowed": [('    if (steps.some((s) => s.subtask.toLowerCase() === subtask.toLowerCase())) return finish("stuck", "", `repeated subtask: ${subtask}`);\n', "")],
        "wrong-limit-off-by-one": [("if (steps.length >= maxSteps)", "if (steps.length > maxSteps)")],
        "wrong-strategy-items-first": [('  if (!stepsKnown) return "adaptive";\n  if (items >= 2 && task.items_interact === true) return "per_item_then_cross";\n',
                                         '  if (items >= 2 && task.items_interact === true) return "per_item_then_cross";\n  if (!stepsKnown) return "adaptive";\n')],
    }),
    "java": ("Decompose.java", {
        "wrong-shared-context": [('String chunk = String.join("\\n", lines.subList(part * maxLines, Math.min(lines.size(), (part + 1) * maxLines)));', 'String chunk = String.join("\\n", files.stream().map(f -> f.get("text")).toList());')],
        "wrong-cross-gets-text": [('one.put("summary", (String) ((Map<String, Object>) e.getValue()).get("summary"));', 'one.put("summary", (String) ((Map<String, Object>) e.getValue()).get("summary"));\n                one.put("text", "source");')],
        "wrong-no-chunking": [("int parts = (lines.size() + maxLines - 1) / maxLines;", "int parts = 1;\n            maxLines = lines.size();")],
        "wrong-blank-reviewed": [("if (text.isBlank()) {", "if (text.isEmpty()) {")],
        "wrong-failed-in-cross": [('failed.put(path, String.valueOf(error.getMessage()));\n                continue;', 'failed.put(path, String.valueOf(error.getMessage()));\n                reviewed.put(path, map("findings", new ArrayList<String>(), "summary", "", "parts", parts));\n                continue;')],
        "wrong-cross-with-one": [("if (reviewed.size() >= 2) {", "if (reviewed.size() >= 1) {")],
        "wrong-no-history": [("Object reply = planner.apply(goal, history);", "Object reply = planner.apply(goal, new ArrayList<>());")],
        "wrong-repeat-allowed": [('            for (Map<String, String> step : steps) {\n                if (step.get("subtask").toLowerCase(Locale.ROOT).equals(subtask.toLowerCase(Locale.ROOT))) {\n                    return map("status", "stuck", "summary", "", "steps", steps, "reason", "repeated subtask: " + subtask);\n                }\n            }\n', "")],
        "wrong-limit-off-by-one": [("if (steps.size() >= maxSteps)", "if (steps.size() > maxSteps)")],
        "wrong-strategy-items-first": [('        if (!known) return "adaptive";\n        if (count >= 2 && Boolean.TRUE.equals(task.get("items_interact"))) return "per_item_then_cross";\n',
                                         '        if (count >= 2 && Boolean.TRUE.equals(task.get("items_interact"))) return "per_item_then_cross";\n        if (!known) return "adaptive";\n')],
    }),
    "kotlin": ("Decompose.kt", {
        "wrong-shared-context": [('val chunk = lines.subList(part * maxLines, minOf(lines.size, (part + 1) * maxLines)).joinToString("\\n")', 'val chunk = files.joinToString("\\n") { it.getValue("text") }')],
        "wrong-cross-gets-text": [('linkedMapOf("path" to path, "summary" to r["summary"] as String)', 'linkedMapOf("path" to path, "summary" to r["summary"] as String, "text" to "source")')],
        "wrong-no-chunking": [("val parts = (lines.size + maxLines - 1) / maxLines", "val parts = 1")],
        "wrong-blank-reviewed": [("if (text.isBlank()) {", "if (text.isEmpty()) {")],
        "wrong-failed-in-cross": [("failed[path] = error.message.toString()\n            continue", 'failed[path] = error.message.toString()\n            reviewed[path] = linkedMapOf("findings" to findings, "summary" to "", "parts" to parts)\n            continue')],
        "wrong-cross-with-one": [("if (reviewed.size >= 2) {", "if (reviewed.size >= 1) {")],
        "wrong-no-history": [("val reply = planner(goal, steps.map { LinkedHashMap(it) })", "val reply = planner(goal, emptyList())")],
        "wrong-repeat-allowed": [('        if (steps.any { it.getValue("subtask").lowercase() == subtask.lowercase() }) return finish("stuck", reason = "repeated subtask: $subtask")\n', "")],
        "wrong-limit-off-by-one": [("if (steps.size >= maxSteps)", "if (steps.size > maxSteps)")],
        "wrong-strategy-items-first": [('    if (!known) return "adaptive"\n    if (count >= 2 && task["items_interact"] == true) return "per_item_then_cross"\n',
                                         '    if (count >= 2 && task["items_interact"] == true) return "per_item_then_cross"\n    if (!known) return "adaptive"\n')],
    }),
}

PLANTS[f"{X}/51-session-state/unit-01/practice-1"] = {
    "python": ("sessions.py", {
        "wrong-ignores-changes": [("    elif changed or deleted or added:", "    elif False:")],
        "wrong-added-ignored": [("    elif changed or deleted or added:", "    elif changed or deleted:")],
        "wrong-half-is-fresh": [("share > STALE_SHARE", "share >= STALE_SHARE")],
        "wrong-added-counted": [("(len(changed) + len(deleted)) / len(before)", "(len(changed) + len(deleted) + len(added)) / len(before)")],
        "wrong-age-ignored": [(' or now - record["last_used"] > WEEK_SECONDS', "")],
        "wrong-age-inclusive": [('now - record["last_used"] > WEEK_SECONDS', 'now - record["last_used"] >= WEEK_SECONDS')],
        "wrong-fork-without-session": [('"fork": bool(fork) and resumed}', '"fork": bool(fork)}')],
        "wrong-notice-when-nothing": [('    if not lines:\n        return ""\n', '    if not lines:\n        lines.append("- none")\n')],
        "wrong-summary-after-task": [('return f"{summary}\\n\\n{task}"', 'return f"{task}\\n\\n{summary}"')],
        "wrong-summary-keeps-repeats": [("if text and text not in seen:", "if text:")],
        "wrong-files-unsorted": [("for path in sorted(files)]", "for path in files]")],
        "wrong-fork-alone": [('        if plan["fork"]:\n            options["fork_session"] = True', '    if plan["fork"]:\n        options["fork_session"] = True')],
        "wrong-continue-many": [("if len(sessions_in_directory) != 1:", "if len(sessions_in_directory) == 0:")],
        "wrong-name-first-match": [("if len(ids) > 1:", "if len(ids) > 5:")],
        "wrong-id-only-on-success": [("                session_id, result = message.session_id, message.result", '                if message.subtype == "success":\n                    session_id, result = message.session_id, message.result')],
        "wrong-no-fork-flag": [('options["fork_session"] = True', 'options["fork_session"] = False')],
    }),
    "typescript": ("sessions.ts", {
        "wrong-ignores-changes": [("else if (changed.length || deleted.length || added.length)", "else if (false)")],
        "wrong-added-ignored": [("else if (changed.length || deleted.length || added.length)", "else if (changed.length || deleted.length)")],
        "wrong-half-is-fresh": [("share > STALE_SHARE", "share >= STALE_SHARE")],
        "wrong-added-counted": [("(changed.length + deleted.length) / paths.length", "(changed.length + deleted.length + added.length) / paths.length")],
        "wrong-age-ignored": [(" || now - record.last_used > WEEK_SECONDS", "")],
        "wrong-age-inclusive": [("now - record.last_used > WEEK_SECONDS", "now - record.last_used >= WEEK_SECONDS")],
        "wrong-fork-without-session": [("fork: Boolean(fork) && resumed }", "fork: Boolean(fork) }")],
        "wrong-notice-when-nothing": [('  if (!lines.length) return "";', '  if (!lines.length) lines.push("- none");')],
        "wrong-summary-after-task": [("return `${summary}\\n\\n${task}`;", "return `${task}\\n\\n${summary}`;")],
        "wrong-summary-keeps-repeats": [("if (text && !seen.includes(text)) seen.push(text);", "if (text) seen.push(text);")],
        "wrong-files-unsorted": [("Object.keys(files).sort().map(", "Object.keys(files).map(")],
        "wrong-fork-alone": [("    if (plan.fork) options.forkSession = true;\n  }", "  }\n  if (plan.fork) options.forkSession = true;")],
        "wrong-continue-many": [("if (sessionsInDirectory.length !== 1)", "if (sessionsInDirectory.length === 0)")],
        "wrong-name-first-match": [("if (ids.length > 1)", "if (ids.length > 5)")],
        "wrong-id-only-on-success": [("        sessionId = message.session_id;", '        if (message.subtype === "success") sessionId = message.session_id;')],
        "wrong-no-fork-flag": [("options.forkSession = true;", "options.forkSession = false;")],
    }),
}

PLANTS[f"{X}/53-tool-errors-agents-can-act-on/unit-01/practice-1"] = {
    "python": ("errors.py", {
        "wrong-flag-missing": [('"content": text, "is_error": True}', '"content": text, "is_error": False}')],
        "wrong-business-retryable": [('"permission": False, "business": False', '"permission": False, "business": True')],
        "wrong-generic-message": [('    if str(message or "").strip().lower().rstrip(".") in GENERIC:\n        raise ValueError("an error message must say what went wrong and what to do")\n', "")],
        "wrong-unknown-kind-accepted": [('    if kind not in KINDS:\n        raise ValueError(f"unknown error kind: {kind}")\n', ""), ('"retryable": KINDS[kind]', '"retryable": KINDS.get(kind, False)')],
        "wrong-retries-validation": [('if kind != "transient":', 'if kind not in ("transient", "validation"):')],
        "wrong-flat-wait": [("base * 2 ** (attempts - 1)", "base")],
        "wrong-extra-retry": [("if attempts > max_retries:", "if attempts > max_retries + 1:")],
        "wrong-ignores-retry-after": [("sleep(error.retry_after_ms if error.retry_after_ms is not None else base * 2 ** (attempts - 1))", "sleep(base * 2 ** (attempts - 1))")],
        "wrong-empty-is-error": [('return {"ok": True, "content": value, "empty": _empty(value), "attempts": attempts}', 'return {"ok": not _empty(value), "content": value, "empty": _empty(value), "attempts": attempts}')],
        "wrong-timeout-retried": [("if not safe_to_repeat:", "if False:")],
        "wrong-key-dropped": [('        if key:\n            call_args["idempotency_key"] = key\n', "")],
        "wrong-mutates-args": [("call_args = dict(args)", "call_args = args")],
        "wrong-permission-retry-later": [('"permission": "escalate"', '"permission": "retry_later"')],
        "wrong-internal-retryable": [('"outcome_unknown": False, "internal": False}', '"outcome_unknown": False, "internal": True}')],
    }),
    "typescript": ("errors.ts", {
        "wrong-flag-missing": [("content: text, is_error: true }", "content: text, is_error: false }")],
        "wrong-business-retryable": [("permission: false, business: false", "permission: false, business: true")],
        "wrong-generic-message": [('  if (GENERIC.has(String(message ?? "").trim().toLowerCase().replace(/\\.+$/, ""))) throw new Error("an error message must say what went wrong and what to do");\n', "")],
        "wrong-unknown-kind-accepted": [("  if (!(kind in KINDS)) throw new Error(`unknown error kind: ${kind}`);\n", ""), ("retryable: KINDS[kind]", "retryable: KINDS[kind] ?? false")],
        "wrong-retries-validation": [('if (kind !== "transient") return', 'if (kind !== "transient" && kind !== "validation") return')],
        "wrong-flat-wait": [("base * 2 ** (attempts - 1)", "base")],
        "wrong-extra-retry": [("if (attempts > maxRetries)", "if (attempts > maxRetries + 1)")],
        "wrong-ignores-retry-after": [("sleep(error.retryAfterMs !== null ? error.retryAfterMs : base * 2 ** (attempts - 1));", "sleep(base * 2 ** (attempts - 1));")],
        "wrong-empty-is-error": [("return { ok: true, content: value, empty: isEmpty(value), attempts };", "return { ok: !isEmpty(value), content: value, empty: isEmpty(value), attempts };")],
        "wrong-timeout-retried": [("if (!safeToRepeat) return", "if (false) return")],
        "wrong-key-dropped": [("    if (key) callArgs.idempotency_key = key;\n", "")],
        "wrong-mutates-args": [("const callArgs: Record<string, any> = { ...args };", "const callArgs: Record<string, any> = args;")],
        "wrong-permission-retry-later": [('permission: "escalate"', 'permission: "retry_later"')],
        "wrong-internal-retryable": [("outcome_unknown: false, internal: false }", "outcome_unknown: false, internal: true }")],
    }),
    "java": ("Errors.java", {
        "wrong-flag-missing": [('block.put("is_error", true);', 'block.put("is_error", false);')],
        "wrong-business-retryable": [('"permission", false, "business", false', '"permission", false, "business", true')],
        "wrong-generic-message": [('        if (GENERIC.contains(plain)) throw new IllegalArgumentException("an error message must say what went wrong and what to do");\n', "")],
        "wrong-unknown-kind-accepted": [('        if (!KINDS.containsKey(kind)) throw new IllegalArgumentException("unknown error kind: " + kind);\n', ""), ('error.put("retryable", KINDS.get(kind));', 'error.put("retryable", KINDS.getOrDefault(kind, false));')],
        "wrong-retries-validation": [('if (!kind.equals("transient")) return makeError(kind, error.getMessage(), error.explanation', 'if (!kind.equals("transient") && !kind.equals("validation")) return makeError(kind, error.getMessage(), error.explanation')],
        "wrong-flat-wait": [("base * (1 << (attempts - 1))", "base")],
        "wrong-extra-retry": [("if (attempts > maxRetries)", "if (attempts > maxRetries + 1)")],
        "wrong-ignores-retry-after": [("sleep.accept(error.retryAfterMs != null ? error.retryAfterMs : base * (1 << (attempts - 1)));", "sleep.accept(base * (1 << (attempts - 1)));")],
        "wrong-empty-is-error": [('ok.put("ok", true);', 'ok.put("ok", !isEmpty(value));')],
        "wrong-timeout-retried": [("if (!safeToRepeat) return", "if (false) return")],
        "wrong-key-dropped": [('            if (key != null && !key.isEmpty()) callArgs.put("idempotency_key", key);\n', "")],
        "wrong-mutates-args": [("Map<String, Object> callArgs = new LinkedHashMap<>(args);", "Map<String, Object> callArgs = args;")],
        "wrong-permission-retry-later": [('"permission", "escalate"', '"permission", "retry_later"')],
        "wrong-internal-retryable": [('"outcome_unknown", false, "internal", false);', '"outcome_unknown", false, "internal", true);')],
    }),
    "kotlin": ("Errors.kt", {
        "wrong-flag-missing": [('"content" to text, "is_error" to true)', '"content" to text, "is_error" to false)')],
        "wrong-business-retryable": [('"permission" to false, "business" to false', '"permission" to false, "business" to true')],
        "wrong-generic-message": [('    require(message.trim().lowercase().trimEnd(\'.\') !in GENERIC) { "an error message must say what went wrong and what to do" }\n', "")],
        "wrong-unknown-kind-accepted": [('    require(kind in KINDS) { "unknown error kind: $kind" }\n', ""), ('"retryable" to KINDS[kind]', '"retryable" to (KINDS[kind] ?: false)')],
        "wrong-retries-validation": [('if (kind != "transient") return makeError(kind, error.message ?: ""', 'if (kind != "transient" && kind != "validation") return makeError(kind, error.message ?: ""')],
        "wrong-flat-wait": [("(base * (1 shl (attempts - 1)))", "base")],
        "wrong-extra-retry": [("if (attempts > maxRetries)", "if (attempts > maxRetries + 1)")],
        "wrong-ignores-retry-after": [("sleep(error.retryAfterMs ?: (base * (1 shl (attempts - 1))))", "sleep(base * (1 shl (attempts - 1)))")],
        "wrong-empty-is-error": [('"ok" to true, "content" to value', '"ok" to !isEmpty(value), "content" to value')],
        "wrong-timeout-retried": [("if (!safeToRepeat) return", "if (false) return")],
        "wrong-key-dropped": [('        if (!key.isNullOrEmpty()) callArgs["idempotency_key"] = key\n', "")],
        "wrong-mutates-args": [("val callArgs = LinkedHashMap(args)", "@Suppress(\"UNCHECKED_CAST\") val callArgs = args as MutableMap<String, Any?>")],
        "wrong-permission-retry-later": [('"permission" to "escalate"', '"permission" to "retry_later"')],
        "wrong-internal-retryable": [('"outcome_unknown" to false, "internal" to false)', '"outcome_unknown" to false, "internal" to true)')],
    }),
}

_P55 = {
    "wrong-github-type": {".mcp.json": [('"type": "http",\n      "url": "${GITHUB_MCP_URL', '"type": "sse",\n      "url": "${GITHUB_MCP_URL')]},
    "wrong-docs-no-command": {".mcp.json": [('      "command": "python3",\n      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],', '      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],')]},
    "wrong-literal-token": {".mcp.json": [('"Authorization": "Bearer ${GITHUB_TOKEN}"', '"Authorization": "Bearer ' + 'ghp_0123456789abcdefghij"')]},
    "wrong-covered-credential": {".mcp.json": [("Bearer ${GITHUB_TOKEN}", "Bearer ${NPM_TOKEN}")]},
    "wrong-secret-default": {".mcp.json": [('"DOCS_API_KEY": "${DOCS_API_KEY}"', '"DOCS_API_KEY": "${DOCS_API_KEY:-dev-key}"')]},
    "wrong-url-no-default": {".mcp.json": [('"url": "${GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}"', '"url": "${GITHUB_MCP_URL}"')]},
    "wrong-project-dir-no-default": {".mcp.json": [('"args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"]', '"args": ["${CLAUDE_PROJECT_DIR}/tools/docs_server.py"]')]},
    "wrong-always-load-all": {".mcp.json": [('      "command": "python3",\n      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],', '      "command": "python3",\n      "alwaysLoad": true,\n      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],')]},
    "wrong-core-deferred": {".mcp.json": [('core-mcp.example.com/mcp}",\n      "alwaysLoad": true\n', 'core-mcp.example.com/mcp}"\n')]},
    "wrong-personal-in-shared": {".mcp.json": [('    "schema": {', '    "scratch": {"type": "stdio", "command": "python3", "args": ["scratch.py"]},\n    "schema": {')]},
    "wrong-user-duplicates": {"user-scope.example.json": [('    "scratch": {', '    "github": {"type": "http", "url": "https://other.example.com/mcp"},\n    "scratch": {')]},
    "wrong-allow-unanchored": {".claude/settings.json": [('"allow": ["mcp__docs__*", "mcp__schema__*"]', '"allow": ["mcp__*"]')]},
    "wrong-allow-github": {".claude/settings.json": [('"mcp__schema__*"]', '"mcp__schema__*", "mcp__github__*"]')]},
    "wrong-no-delete-deny": {".claude/settings.json": [('"deny": ["mcp__github__delete_repository"]', '"deny": []')]},
    "wrong-description-long": {"docs/tool-descriptions.json": [("open a link for that.\"", "open a link for that. " + "Extra detail. " * 150 + "\"")]},
    "wrong-boundary-buried": {"docs/tool-descriptions.json": [("Use this instead of Grep to search", "A search tool for the documentation. " + "It is maintained by the platform team and indexed nightly. " * 6 + "Use this instead of Grep to search")]},
    "wrong-no-resource-ref": {"docs/mcp-servers.md": [("`@schema:schema://orders`", "`@catalog:schema://orders`")]},
    "wrong-schema-as-tool": {"docs/mcp-servers.md": [("resources: the database schemas", "tools: a schema lookup"), ("A catalog, so it is read as a resource and not through a search tool.", "Queried through a lookup tool.")]},
    "wrong-scope-user": {"docs/mcp-servers.md": [("| `github` | project |", "| `github` | user |")]},
    "wrong-home-path": {"docs/mcp-servers.md": [("and `DOCS_API_KEY`). A personal", "and `DOCS_API_KEY`, kept in /home/dev/.profile). A personal")]},
}
PLANTS[f"{X}/55-mcp-in-claude-code/unit-01/practice-1"] = {"python": (".mcp.json", _P55), "typescript": (".mcp.json", _P55)}

_P56 = {
    "wrong-no-src-deny": {".claude/settings.json": [('"deny": ["Read(./.env)", "Read(secrets/**)", "Edit(src/**)"]', '"deny": ["Read(./.env)", "Read(secrets/**)"]')]},
    "wrong-bash-bare": {".claude/settings.json": [('"Bash(git log *)", "Bash(git diff *)", "Bash(git status)"', '"Bash"')]},
    "wrong-notes-everywhere": {".claude/settings.json": [('"Edit(notes/**)"', '"Edit(**)"')]},
    "wrong-write-rule": {".claude/settings.json": [('"Edit(notes/**)"', '"Write(notes/**)"')]},
    "wrong-no-env-deny": {".claude/settings.json": [('"Read(./.env)", ', "")]},
    "wrong-grep-rule": {".claude/settings.json": [('"Read(secrets/**)"', '"Grep(secrets/**)"')]},
    "wrong-agent-edit": {".claude/agents/explorer.md": [("tools: Read, Grep, Glob", "tools: Read, Grep, Glob, Edit")]},
    "wrong-agent-bash": {".claude/agents/explorer.md": [("tools: Read, Grep, Glob", "tools: Read, Grep, Glob, Bash")]},
    "wrong-agent-no-use-when": {".claude/agents/explorer.md": [("Use when a question is about", "For questions about")]},
    "wrong-agent-unbounded": {".claude/agents/explorer.md": [("maxTurns: 12\n", "")]},
    "wrong-options-no-search": {"agent-options.json": [('"tools": ["Read", "Grep", "Glob"],', '"tools": ["Read"],')]},
    "wrong-options-allow-bash": {"agent-options.json": [('"allowedTools": ["Read", "Grep", "Glob"]', '"allowedTools": ["Read", "Grep", "Glob", "Bash"]')]},
    "wrong-options-no-disallow": {"agent-options.json": [('"disallowedTools": ["Bash", "Edit", "Write"]', '"disallowedTools": ["Bash"]')]},
    "wrong-plan-read-first": {"docs/exploration-plan.md": [("1. Find the entry points with Grep: search for", "1. Read the entry points in full: look at")]},
    "wrong-plan-read-all": {"docs/exploration-plan.md": [("Do not read every file first. Build", "Read every file first. Build")]},
    "wrong-no-export-step": {"docs/exploration-plan.md": [("list the names each wrapper exports, then search the repository for each exported name with Grep.", "search the repository for the function name with Grep.")]},
    "wrong-fallback-rewrite-first": {"docs/exploration-plan.md": [("1. If Edit says the text appears more than once, repeat it with more surrounding lines until it is unique.", "1. If Edit says the text appears more than once, Read the whole file and Write it back with the change."), ("3. If no unique anchor exists, Read the whole file and Write it back with the change.", "3. If that does not help, repeat it with more surrounding lines until it is unique.")]},
    "wrong-fallback-no-replace-all": {"docs/exploration-plan.md": [("2. If every occurrence should change, use replace_all.", "2. If every occurrence should change, run the edit once for each of them.")]},
    "wrong-home-path": {"docs/exploration-plan.md": [("Do not read every file first.", "Do not read every file first (the clone is in /home/dev/inventory).")]},
}
PLANTS[f"{X}/56-the-built-in-tools/unit-01/practice-1"] = {"python": (".claude/settings.json", _P56), "typescript": (".claude/settings.json", _P56)}

PLANTS[f"{X}/54-distributing-tools-across-agents/unit-01/practice-1"] = {
    "python": ("distribute.py", {
        "wrong-sorted-names": [("result[role] = names", "result[role] = sorted(names)")],
        "wrong-no-budget": [("if len(names) > budget:", "if False:")],
        "wrong-unscoped-extra": [('if not (tags & set(tool.get("tags", [])) or tool.get("scoped") or tool.get("irreversible")):', "if False:")],
        "wrong-duplicates-ok": [('        if tool["name"] in by_name:\n            raise ValueError(f"duplicate tool name: {tool[\'name\']}")\n', "")],
        "wrong-irreversible-by-tag": [('if tags & set(t.get("tags", [])) and not t.get("irreversible")]', 'if tags & set(t.get("tags", []))]')],
        "wrong-forces-everywhere": [("rejects = model in NO_FORCING or manual_thinking", "rejects = False")],
        "wrong-fallback-all-tools": [('offered = list(tools) if need == "any" else [forced]', "offered = list(tools)")],
        "wrong-fallback-no-verify": [('"strict": True, "verify_call": True}', '"strict": True, "verify_call": False}')],
        "wrong-tools-ignored": [('    if previous.get("tools") != new.get("tools"):\n        return "all"\n', "")],
        "wrong-choice-ignored": [('    if previous.get("tool_choice") != new.get("tool_choice"):\n        return "messages"\n', "")],
        "wrong-repeat-costs": [('        return "messages"\n    return "none"\n', '        return "messages"\n    return "messages"\n')],
        "wrong-missed-call-ok": [('    if not calls:\n        return "missed_call"\n', "")],
        "wrong-any-tool-counts": [('    if need == "named" and calls[0].get("name") != forced:\n        return "wrong_tool"\n', "")],
        "wrong-default-allow": [('        return _answer(False, "unknown_tool", f"{call.get(\'tool\')} is not an allowed tool")', '        return _answer(True, "ok", "allowed")')],
        "wrong-owner-unchecked": [('    if call.get("verified_customer") is None or call.get("customer") != call.get("verified_customer"):\n        return _answer(False, "not_owner", "the call is not for the verified customer")\n', "")],
        "wrong-cap-exclusive": [("if cap is not None and amount > cap:", "if cap is not None and amount >= cap:")],
        "wrong-approval-lifts-cap": [("if cap is not None and amount > cap:", 'if cap is not None and amount > cap and call.get("id") not in approvals:')],
        "wrong-approval-skipped": [('if rule.get("irreversible") and call.get("id") not in approvals:', "if False:")],
    }),
    "typescript": ("distribute.ts", {
        "wrong-sorted-names": [("result[role] = names;", "result[role] = [...names].sort();")],
        "wrong-no-budget": [("if (names.length > budget) throw", "if (false) throw")],
        "wrong-unscoped-extra": [("if (!(shares(tool) || tool.scoped || tool.irreversible)) throw", "if (false) throw")],
        "wrong-duplicates-ok": [("    if (byName.has(tool.name)) throw new Error(`duplicate tool name: ${tool.name}`);\n", "")],
        "wrong-irreversible-by-tag": [("catalog.filter((t) => shares(t) && !t.irreversible)", "catalog.filter((t) => shares(t))")],
        "wrong-forces-everywhere": [("const rejects = NO_FORCING.has(model) || manualThinking;", "const rejects = false;")],
        "wrong-fallback-all-tools": [('tools: need === "any" ? [...tools] : [forced]', "tools: [...tools]")],
        "wrong-fallback-no-verify": [("strict: true, verify_call: true }", "strict: true, verify_call: false }")],
        "wrong-tools-ignored": [('  if (JSON.stringify(previous.tools) !== JSON.stringify(next.tools)) return "all";\n', "")],
        "wrong-choice-ignored": [('  if (JSON.stringify(previous.tool_choice) !== JSON.stringify(next.tool_choice)) return "messages";\n', "")],
        "wrong-repeat-costs": [('return "messages";\n  return "none";', 'return "messages";\n  return "messages";')],
        "wrong-missed-call-ok": [('  if (calls.length === 0) return "missed_call";\n', "")],
        "wrong-any-tool-counts": [('  if (need === "named" && calls[0].name !== forced) return "wrong_tool";\n', "")],
        "wrong-default-allow": [('return answer(false, "unknown_tool", `${call.tool} is not an allowed tool`);', 'return answer(true, "ok", "allowed");')],
        "wrong-owner-unchecked": [('  if (call.verified_customer === null || call.verified_customer === undefined || call.customer !== call.verified_customer) return answer(false, "not_owner", "the call is not for the verified customer");\n', "")],
        "wrong-cap-exclusive": [("if (cap !== null && amount > cap)", "if (cap !== null && amount >= cap)")],
        "wrong-approval-lifts-cap": [("if (cap !== null && amount > cap)", "if (cap !== null && amount > cap && !new Set(approvals).has(call.id))")],
        "wrong-approval-skipped": [("if (rule.irreversible && !new Set(approvals).has(call.id))", "if (false)")],
    }),
    "java": ("Distribute.java", {
        "wrong-sorted-names": [("result.put(role.getKey(), names);", "java.util.Collections.sort(names);\n            result.put(role.getKey(), names);")],
        "wrong-no-budget": [("if (names.size() > budget) throw", "if (false) throw")],
        "wrong-unscoped-extra": [('if (!(shares(tool, tags) || Boolean.TRUE.equals(tool.get("scoped")) || Boolean.TRUE.equals(tool.get("irreversible")))) {', "if (false) {")],
        "wrong-duplicates-ok": [('            if (byName.containsKey(name)) throw new IllegalArgumentException("duplicate tool name: " + name);\n', "")],
        "wrong-irreversible-by-tag": [('if (shares(tool, tags) && !Boolean.TRUE.equals(tool.get("irreversible"))) names.add(', "if (shares(tool, tags)) names.add(")],
        "wrong-forces-everywhere": [("boolean rejects = NO_FORCING.contains(model) || manualThinking;", "boolean rejects = false;")],
        "wrong-fallback-all-tools": [('need.equals("any") ? new ArrayList<>(tools) : new ArrayList<>(List.of(forced))', "new ArrayList<>(tools)")],
        "wrong-fallback-no-verify": [('"strict", true, "verify_call", true);', '"strict", true, "verify_call", false);')],
        "wrong-tools-ignored": [('        if (!java.util.Objects.equals(previous.get("tools"), next.get("tools"))) return "all";\n', "")],
        "wrong-choice-ignored": [('        if (!java.util.Objects.equals(previous.get("tool_choice"), next.get("tool_choice"))) return "messages";\n', "")],
        "wrong-repeat-costs": [('return "messages";\n        return "none";', 'return "messages";\n        return "messages";')],
        "wrong-missed-call-ok": [('        if (calls.isEmpty()) return "missed_call";\n', "")],
        "wrong-any-tool-counts": [('        if (need.equals("named") && !calls.get(0).get("name").equals(forced)) return "wrong_tool";\n', "")],
        "wrong-default-allow": [('return answer(false, "unknown_tool", call.get("tool") + " is not an allowed tool", false);', 'return answer(true, "ok", "allowed", false);')],
        "wrong-owner-unchecked": [('        if (verified == null || !verified.equals(call.get("customer"))) return answer(false, "not_owner", "the call is not for the verified customer", false);\n', "")],
        "wrong-cap-exclusive": [("(Integer) amount > cap", "(Integer) amount >= cap")],
        "wrong-approval-lifts-cap": [("if (cap != null && (Integer) amount > cap) return answer(", 'if (cap != null && (Integer) amount > cap && !approvals.contains(call.get("id"))) return answer(')],
        "wrong-approval-skipped": [('if (Boolean.TRUE.equals(rule.get("irreversible")) && !approvals.contains(call.get("id"))) return', "if (false) return")],
    }),
    "kotlin": ("Distribute.kt", {
        "wrong-sorted-names": [("result[role] = names", "result[role] = names.sorted()")],
        "wrong-no-budget": [("require(names.size <= budget)", "require(true || names.size <= budget)")],
        "wrong-unscoped-extra": [('require(shares(tool, tags) || tool["scoped"] == true || tool["irreversible"] == true) {', "require(true) {")],
        "wrong-duplicates-ok": [('        require(name !in byName) { "duplicate tool name: $name" }\n', "")],
        "wrong-irreversible-by-tag": [('catalog.filter { shares(it, tags) && it["irreversible"] != true }', "catalog.filter { shares(it, tags) }")],
        "wrong-forces-everywhere": [("val rejects = model in NO_FORCING || manualThinking", "val rejects = false")],
        "wrong-fallback-all-tools": [('(if (need == "any") tools.toList() else listOf(forced))', "tools.toList()")],
        "wrong-fallback-no-verify": [('"strict" to true, "verify_call" to true)', '"strict" to true, "verify_call" to false)')],
        "wrong-tools-ignored": [('    if (previous["tools"] != next["tools"]) return "all"\n', "")],
        "wrong-choice-ignored": [('    if (previous["tool_choice"] != next["tool_choice"]) return "messages"\n', "")],
        "wrong-repeat-costs": [('return "messages"\n    return "none"\n}', 'return "messages"\n    return "messages"\n}')],
        "wrong-missed-call-ok": [('    if (calls.isEmpty()) return "missed_call"\n', "")],
        "wrong-any-tool-counts": [('    if (need == "named" && calls[0]["name"] != forced) return "wrong_tool"\n', "")],
        "wrong-default-allow": [('?: return answer(false, "unknown_tool", "${call["tool"]} is not an allowed tool")', '?: return answer(true, "ok", "allowed")')],
        "wrong-owner-unchecked": [('    val verified = call["verified_customer"]\n    if (verified == null || verified != call["customer"]) return answer(false, "not_owner", "the call is not for the verified customer")\n', "")],
        "wrong-cap-exclusive": [("(amount as Int) > cap", "(amount as Int) >= cap")],
        "wrong-approval-lifts-cap": [("if (cap != null && (amount as Int) > cap)", 'if (cap != null && (amount as Int) > cap && (call["id"] as String) !in approvals)')],
        "wrong-approval-skipped": [('if (rule["irreversible"] == true && (call["id"] as String) !in approvals) return', "if (false) return")],
    }),
}

PLANTS[f"{X}/52-designing-tool-interfaces/unit-01/practice-1"] = {
    "python": ("toolset.py", {
        "wrong-name-spaces-allowed": [('NAME = re.compile(r"[A-Za-z0-9_-]{1,128}")', 'NAME = re.compile(r"[A-Za-z0-9_ .#-]{1,128}")')],
        "wrong-vague-case-sensitive": [("if name.lower() in VAGUE:", "if name in VAGUE:")],
        "wrong-two-sentences-ok": [("description)) < 3:", "description)) < 2:")],
        "wrong-boundary-do-not-only": [('("do not use", "not for", "instead of")', '("do not use",)')],
        "wrong-required-unchecked": [("if any(item not in properties for item in required):", "if False:")],
        "wrong-example-enum-ignored": [('return "enum" not in schema or value in schema["enum"]', "return True")],
        "wrong-bool-is-integer": [('if kind == "integer" and (isinstance(value, bool) or not isinstance(value, int)):', 'if kind == "integer" and not isinstance(value, int):')],
        "wrong-undescribed-ok": [('if any(not str((spec or {}).get("description") or "").strip() for spec in properties.values()):', "if False:")],
        "wrong-limit-only": [('not ("limit" in properties and "cursor" in properties)', '"limit" not in properties')],
        "wrong-hint-readonly-only": [(' or (hints.get("destructiveHint") is False and name.startswith(DELETE_PREFIXES))', "")],
        "wrong-duplicate-allowed": [('found |= {(name, "duplicate-name") for name in set(names) if names.count(name) > 1}', "found |= set()")],
        "wrong-overlap-strict": [(" >= OVERLAP", " > OVERLAP")],
        "wrong-count-off-by-one": [("if len(tools) > max_tools:", "if len(tools) >= max_tools:")],
        "wrong-limit-unclamped": [("limit = min(limit, MAX_LIMIT)", "limit = limit")],
        "wrong-no-note": [(" if next_cursor else None\n", " if False else None\n")],
        "wrong-cursor-unchecked": [('raise ValueError("invalid cursor")', "return 0")],
        "wrong-cap-exclusive": [("used + len(item) > max_chars", "used + len(item) >= max_chars")],
        "wrong-truncated-never": [('"truncated": len(page) < min(limit, len(items) - offset)', '"truncated": False')],
        "wrong-untrusted-honoured": [("    if trusted_server:\n", "    if True:\n")],
        "wrong-parallel-untrusted": [('effective_hints(tool, tool.get("server") in trusted_servers)', "effective_hints(tool, True)")],
    }),
    "typescript": ("toolset.ts", {
        "wrong-name-spaces-allowed": [("const NAME = /^[A-Za-z0-9_-]{1,128}$/;", "const NAME = /^[A-Za-z0-9_ .#-]{1,128}$/;")],
        "wrong-vague-case-sensitive": [("VAGUE.has(name.toLowerCase())", "VAGUE.has(name)")],
        "wrong-two-sentences-ok": [("?? []).length < 3)", "?? []).length < 2)")],
        "wrong-boundary-do-not-only": [('["do not use", "not for", "instead of"]', '["do not use"]')],
        "wrong-required-unchecked": [("required.some((item) => !(item in properties))", "false")],
        "wrong-example-enum-ignored": [('return !("enum" in schema) || schema.enum.includes(value);', "return true;")],
        "wrong-bool-is-integer": [('if (kind === "integer" && !Number.isInteger(value)) return false;', 'if (kind === "integer" && typeof value !== "number" && typeof value !== "boolean") return false;')],
        "wrong-undescribed-ok": [('specs.some((spec) => !String(spec.description ?? "").trim())', "false")],
        "wrong-limit-only": [('!("limit" in properties && "cursor" in properties)', '!("limit" in properties)')],
        "wrong-hint-readonly-only": [(" || (hints.destructiveHint === false && startsWithAny(name, DELETE_PREFIXES))", "")],
        "wrong-duplicate-allowed": [('.length > 1) add(name, "duplicate-name")', '.length > 99) add(name, "duplicate-name")')],
        "wrong-overlap-strict": [("shared / union.size >= OVERLAP", "shared / union.size > OVERLAP")],
        "wrong-count-off-by-one": [("if (tools.length > maxTools)", "if (tools.length >= maxTools)")],
        "wrong-limit-unclamped": [("limit = Math.min(limit, MAX_LIMIT);", "limit = limit;")],
        "wrong-no-note": [("const note = nextCursor ?", "const note = false ?")],
        "wrong-cursor-unchecked": [('throw new Error("invalid cursor");', "return 0;")],
        "wrong-cap-exclusive": [("used + item.length > maxChars", "used + item.length >= maxChars")],
        "wrong-truncated-never": [("truncated: page.length < Math.min(limit, items.length - offset), note", "truncated: false, note")],
        "wrong-untrusted-honoured": [("  if (trustedServer) {", "  if (true) {")],
        "wrong-parallel-untrusted": [("effectiveHints(tool, trustedServers.has(tool.server))", "effectiveHints(tool, true)")],
    }),
    "java": ("Toolset.java", {
        "wrong-name-spaces-allowed": [('Pattern.compile("[A-Za-z0-9_-]{1,128}")', 'Pattern.compile("[A-Za-z0-9_ .#-]{1,128}")')],
        "wrong-vague-case-sensitive": [("VAGUE.contains(name.toLowerCase(Locale.ROOT))", "VAGUE.contains(name)")],
        "wrong-two-sentences-ok": [("if (count < 3)", "if (count < 2)")],
        "wrong-boundary-do-not-only": [('List.of("do not use", "not for", "instead of").stream()', 'List.of("do not use").stream()')],
        "wrong-required-unchecked": [("required.stream().anyMatch(item -> !properties.containsKey(item))", "false")],
        "wrong-example-enum-ignored": [('return !schema.containsKey("enum") || ((Collection<?>) schema.get("enum")).contains(value);', "return true;")],
        "wrong-bool-is-integer": [('!(value instanceof Integer || value instanceof Long)', '!(value instanceof Integer || value instanceof Long || value instanceof Boolean)')],
        "wrong-undescribed-ok": [('specs.stream().anyMatch(spec -> str(spec.get("description")).isBlank())', "false")],
        "wrong-limit-only": [('!(properties.containsKey("limit") && properties.containsKey("cursor"))', '!properties.containsKey("limit")')],
        "wrong-hint-readonly-only": [(' || (Boolean.FALSE.equals(hints.get("destructiveHint")) && startsWithAny(name, DELETE_PREFIXES))', "")],
        "wrong-duplicate-allowed": [("names.stream().filter(name::equals).count() > 1", "names.stream().filter(name::equals).count() > 99")],
        "wrong-overlap-strict": [("union.size() >= OVERLAP", "union.size() > OVERLAP")],
        "wrong-count-off-by-one": [("if (tools.size() > maxTools)", "if (tools.size() >= maxTools)")],
        "wrong-limit-unclamped": [("limit = Math.min(limit, MAX_LIMIT);", "limit = limit;")],
        "wrong-no-note": [("String note = nextCursor != null ?", "String note = false ?")],
        "wrong-cursor-unchecked": [('throw new IllegalArgumentException("invalid cursor");', "return 0;")],
        "wrong-cap-exclusive": [("used + item.length() > maxChars", "used + item.length() >= maxChars")],
        "wrong-truncated-never": [('result.put("truncated", page.size() < Math.min(limit, items.size() - offset));', 'result.put("truncated", false);')],
        "wrong-untrusted-honoured": [("if (trustedServer) {", "if (true) {")],
        "wrong-parallel-untrusted": [('effectiveHints(tool, trustedServers.contains(str(tool.get("server"))))', "effectiveHints(tool, true)")],
    }),
    "kotlin": ("Toolset.kt", {
        "wrong-name-spaces-allowed": [('Regex("[A-Za-z0-9_-]{1,128}")', 'Regex("[A-Za-z0-9_ .#-]{1,128}")')],
        "wrong-vague-case-sensitive": [("name.lowercase() in VAGUE", "name in VAGUE")],
        "wrong-two-sentences-ok": [(".count() < 3) found.add(\"short-description\")", ".count() < 2) found.add(\"short-description\")")],
        "wrong-boundary-do-not-only": [('listOf("do not use", "not for", "instead of").none', 'listOf("do not use").none')],
        "wrong-required-unchecked": [('if (required.any { str(it) !in properties }) found.add("required-unknown")', 'if (false) found.add("required-unknown")')],
        "wrong-example-enum-ignored": [("return allowed == null || allowed.contains(value)", "return true")],
        "wrong-bool-is-integer": [('"integer" -> if (!(value is Int || value is Long)) return false', '"integer" -> if (!(value is Int || value is Long || value is Boolean)) return false')],
        "wrong-undescribed-ok": [('if (specs.any { str(it["description"]).isBlank() }) found.add("param-undescribed")', 'if (false) found.add("param-undescribed")')],
        "wrong-limit-only": [('!(properties.containsKey("limit") && properties.containsKey("cursor"))', '!properties.containsKey("limit")')],
        "wrong-hint-readonly-only": [(' || (hints["destructiveHint"] == false && startsWithAny(name, DELETE_PREFIXES))', "")],
        "wrong-duplicate-allowed": [("names.count { it == name } > 1", "names.count { it == name } > 99")],
        "wrong-overlap-strict": [(".toDouble() / union.size >= OVERLAP", ".toDouble() / union.size > OVERLAP")],
        "wrong-count-off-by-one": [("if (tools.size > maxTools)", "if (tools.size >= maxTools)")],
        "wrong-limit-unclamped": [("val size = minOf(limit, MAX_LIMIT)", "val size = limit")],
        "wrong-no-note": [('val note = if (nextCursor != null) "', 'val note = if (false) "')],
        "wrong-cursor-unchecked": [('throw IllegalArgumentException("invalid cursor")', "return 0")],
        "wrong-cap-exclusive": [("used + item.length > maxChars", "used + item.length >= maxChars")],
        "wrong-truncated-never": [('"truncated" to (page.size < minOf(size, items.size - offset))', '"truncated" to false')],
        "wrong-untrusted-honoured": [("if (trustedServer) {", "if (true) {")],
        "wrong-parallel-untrusted": [('effectiveHints(it, str(it["server"]) in trustedServers)!!', "effectiveHints(it, true)!!")],
    }),
}

# --- PLANTS ABOVE ---


# ===== Level 3: modules 57 to 62 =====
_T0 = "Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test."
_P57 = {
    "wrong-api-unscoped": {".claude/rules/api.md": [('---\npaths:\n  - "src/api/**/*.ts"\n---\n\n', "")]},
    "wrong-api-bare-folder": {".claude/rules/api.md": [('paths:\n  - "src/api/**/*.ts"\n', "paths: src/api\n")]},
    "wrong-testing-folder": {".claude/rules/testing.md": [('  - "**/*.test.ts"\n  - "**/*.test.tsx"\n', '  - "src/ui/**/*.test.tsx"\n')]},
    "wrong-testing-ts-only": {".claude/rules/testing.md": [('  - "**/*.test.tsx"\n', "")]},
    "wrong-terraform-everything": {".claude/rules/terraform.md": [('"terraform/**/*"', '"**/*"')]},
    "wrong-root-keeps-testing": {"CLAUDE.md": [("## Always\n", "## Always\n- " + _T0 + "\n")]},
    "wrong-root-long": {"CLAUDE.md": [("## Where things are\n", "## Where things are\n" + "- Background note: kept for history, it changes no behaviour.\n" * 40)]},
    "wrong-import-typo": {"CLAUDE.md": [("@docs/standards/architecture.md", "@docs/standards/architecure.md")]},
    "wrong-import-in-code-span": {"CLAUDE.md": [("@docs/standards/architecture.md", "`@docs/standards/architecture.md`")]},
    "wrong-personal-in-root": {"CLAUDE.md": [("## Always\n", "## Always\n- I prefer short answers with no preamble.\n")]},
    "wrong-local-not-ignored": {".gitignore": [("CLAUDE.local.md\n", "")]},
    "wrong-no-migration-deny": {".claude/settings.json": [('"deny": ["Edit(db/migrations/**)"]', '"deny": []')]},
    "wrong-write-rule": {".claude/settings.json": [("Edit(db/migrations/**)", "Write(db/migrations/**)")]},
    "wrong-home-path": {"CLAUDE.md": [("## Where things are\n", "## Where things are\n- My notes are in /home/dev/notes.\n")]},
}
PLANTS[f"{X}/57-memory-files-and-rules/unit-01/practice-1"] = {"python": ("CLAUDE.md", _P57), "typescript": ("CLAUDE.md", _P57)}


_RV, _TG, _SU, _MI, _PL = ".claude/skills/review-pr/SKILL.md", ".claude/skills/release-tag/SKILL.md", ".claude/commands/standup.md", "personal/review-pr-mine/SKILL.md", "docs/placement.md"
_P58 = {
    "wrong-fork-guidelines-only": {_RV: [("\n1. Run `gh pr view", "\n- Run `gh pr view")]},
    "wrong-no-fork": {_RV: [("context: fork\nagent: general-purpose\n", "")]},
    "wrong-no-agent": {_RV: [("agent: general-purpose\n", "")]},
    "wrong-no-hint": {_RV: [('argument-hint: "[pr-number]"\n', "")]},
    "wrong-no-placeholder": {_RV: [("Review pull request $0.", "Review the pull request."), ("`gh pr view $0`", "`gh pr view`"), ("`gh pr diff $0`", "`gh pr diff`")]},
    "wrong-allowed-restricts": {_RV: [("disallowed-tools: Edit Write\n", "")]},
    "wrong-disallow-scoped": {_RV: [("disallowed-tools: Edit Write", "disallowed-tools: Edit(src/**) Write(src/**)")]},
    "wrong-review-bare-bash": {_RV: [("allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)", "allowed-tools: Bash")]},
    "wrong-tag-bare-bash": {_TG: [("allowed-tools: Bash(git tag *) Bash(git push origin *)", "allowed-tools: Bash")]},
    "wrong-tag-auto": {_TG: [("disable-model-invocation: true\n", "")]},
    "wrong-tag-no-arguments": {_TG: [("arguments: [version]\n", "")]},
    "wrong-name-collision": {_MI: [("name: review-pr-mine", "name: release-tag")]},
    "wrong-mine-shadows": {_MI: [("name: review-pr-mine", "name: review-pr")]},
    "wrong-standup-no-hint": {_SU: [('argument-hint: "[author]"\n', "")]},
    "wrong-placement-rule-in-memory": {_PL: [(".claude/rules/testing.md", "CLAUDE.md")]},
    "wrong-placement-personal-in-project": {_PL: [("`~/.claude/skills/review-pr-mine/SKILL.md`", "`.claude/skills/review-pr-mine/SKILL.md`")]},
    "wrong-no-use-when": {_RV: [("Use when the user asks for a review", "Run it for a review")]},
    "wrong-long-description": {_TG: [("cut a release of a given version.", "cut a release of a given version." + " Extra text." * 150)]},
    "wrong-home-path": {_PL: [("# Where each piece of guidance lives\n", "# Where each piece of guidance lives\n\nNotes kept in /home/dev/notes.\n")]},
}
PLANTS[f"{X}/58-commands-and-skills/unit-01/practice-1"] = {"python": (_RV, _P58), "typescript": (_RV, _P58)}


PLANTS[f"{X}/61-criteria-and-examples/unit-01/practice-1"] = {
    "python": ("review_spec.py", {
        "wrong-diff-first": [('return "\\n".join(["<criteria>", *blocks, "</criteria>", "<examples>", *shown, "</examples>", "<diff>", diff, "</diff>"])',
                              'return "\\n".join(["<diff>", diff, "</diff>", "<criteria>", *blocks, "</criteria>", "<examples>", *shown, "</examples>"])')],
        "wrong-vague-report-only": [("            phrase = _vague(c[key])\n", '            phrase = _vague(c[key]) if key == "report" else None\n')],
        "wrong-vague-short-list": [('VAGUE = ("be conservative", ', "VAGUE = (")],
        "wrong-skip-blank": [("            if not _present(c.get(key)):", "            if c.get(key) is None:")],
        "wrong-severity-high-only": [('        for level in ("high", "low"):', '        for level in ("high",):'), ('Severity low: {severity["low"]}', 'Severity low: {severity.get("low", "")}')],
        "wrong-examples-up-to-six": [("if not 2 <= len(examples) <= 4:", "if not 2 <= len(examples) <= 6:")],
        "wrong-all-report-examples": [('if {e.get("verdict") for e in examples} != {"report", "skip"}:', 'if not {e.get("verdict") for e in examples} <= {"report", "skip"}:')],
        "wrong-reason-optional": [('if not _present(e.get("reason")):', 'if e.get("reason") is None:')],
        "wrong-unknown-category": [('if e["verdict"] == "report" and e.get("category") not in ids:', "if False:")],
        "wrong-disable-few": [('"disable": len(items) >= min_reviewed and precision < min_precision', '"disable": precision < min_precision')],
        "wrong-precision-inverted": [("precision = round(accepted / len(items), 2)", "precision = round((len(items) - accepted) / len(items), 2)")],
        "wrong-boundary-disables": [("precision < min_precision", "precision <= min_precision")],
        "wrong-top-by-name": [("key=lambda kv: (-kv[1], kv[0])", "key=lambda kv: kv[0]")],
        "wrong-top-uncapped": [("))[:3]", "))")],
        "wrong-counts-accepted": [('            if f["verdict"] == "dismissed":', "            if True:")],
        "wrong-ask-defaults": [("unresolved = [f for f in missing if f not in defaults]", "unresolved = list(missing)")],
        "wrong-no-assumptions": [("assumptions = {f: defaults[f] for f in missing if f in defaults}", "assumptions = {}")],
        "wrong-unattended-asks": [('return {"action": "stop", "ask": [], "assumptions": assumptions}', 'return {"action": "ask", "ask": unresolved, "assumptions": assumptions}')],
        "wrong-unattended-guess": [('    if unresolved:\n        return {"action": "stop", "ask": [], "assumptions": assumptions}\n', "")],
        "wrong-blank-is-present": [('return value is None or (isinstance(value, str) and value.strip() == "")', 'return value is None or value == ""')],
    }),
    "typescript": ("reviewSpec.ts", {
        "wrong-diff-first": [('return ["<criteria>", ...blocks, "</criteria>", "<examples>", ...shown, "</examples>", "<diff>", diff, "</diff>"].join("\\n");',
                              'return ["<diff>", diff, "</diff>", "<criteria>", ...blocks, "</criteria>", "<examples>", ...shown, "</examples>"].join("\\n");')],
        "wrong-vague-report-only": [("const phrase = vague(c[key]);", 'const phrase = key === "report" ? vague(c[key]) : undefined;')],
        "wrong-vague-short-list": [('export const VAGUE = ["be conservative", ', "export const VAGUE = [")],
        "wrong-skip-blank": [("if (!present(c[key])) throw new Error(`criterion ${c.id}: ${key} is required`);", "if (c[key] === undefined || c[key] === null) throw new Error(`criterion ${c.id}: ${key} is required`);")],
        "wrong-severity-high-only": [('for (const level of ["high", "low"]) {', 'for (const level of ["high"]) {')],
        "wrong-examples-up-to-six": [("examples.length > 4", "examples.length > 6")],
        "wrong-all-report-examples": [('if (verdicts.join() !== "report,skip")', 'if (verdicts.some((v) => v !== "report" && v !== "skip"))')],
        "wrong-reason-optional": [("if (!present(e.reason)) throw", "if (e.reason === undefined || e.reason === null) throw")],
        "wrong-unknown-category": [('if (e.verdict === "report" && !ids.includes(e.category)) throw', "if (false) throw")],
        "wrong-disable-few": [("disable: items.length >= minReviewed && precision < minPrecision", "disable: precision < minPrecision")],
        "wrong-precision-inverted": [("Math.round((accepted / items.length) * 100) / 100", "Math.round(((items.length - accepted) / items.length) * 100) / 100")],
        "wrong-boundary-disables": [("precision < minPrecision", "precision <= minPrecision")],
        "wrong-top-by-name": [(".sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0))", ".sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0))")],
        "wrong-top-uncapped": [(".slice(0, 3);", ";")],
        "wrong-counts-accepted": [('for (const f of items) if (f.verdict === "dismissed") counts.set(', "for (const f of items) if (true) counts.set(")],
        "wrong-ask-defaults": [("const unresolved = missing.filter((f) => !(f in defaults));", "const unresolved = [...missing];")],
        "wrong-no-assumptions": [("  for (const f of missing) if (f in defaults) assumptions[f] = defaults[f];\n", "")],
        "wrong-unattended-asks": [('if (unresolved.length > 0) return { action: "stop", ask: [], assumptions };', 'if (unresolved.length > 0) return { action: "ask", ask: unresolved, assumptions };')],
        "wrong-unattended-guess": [('  if (unresolved.length > 0) return { action: "stop", ask: [], assumptions };\n', "")],
        "wrong-blank-is-present": [('(typeof value === "string" && value.trim() === "");', '(typeof value === "string" && value === "");')],
    }),
    "java": ("ReviewSpec.java", {
        "wrong-diff-first": [('return String.join("\\n", out);', 'return String.join("\\n", out.subList(out.size() - 3, out.size())) + "\\n" + String.join("\\n", out.subList(0, out.size() - 3));')],
        "wrong-vague-report-only": [('String phrase = vague((String) c.get(key));', 'String phrase = key.equals("report") ? vague((String) c.get(key)) : null;')],
        "wrong-vague-short-list": [('List.of("be conservative", "high-confidence"', 'List.of("high-confidence"')],
        "wrong-skip-blank": [('if (!present(c.get(key))) throw new IllegalArgumentException("criterion " + c.get("id") + ": " + key + " is required");', 'if (c.get(key) == null) throw new IllegalArgumentException("criterion " + c.get("id") + ": " + key + " is required");')],
        "wrong-severity-high-only": [('for (String level : List.of("high", "low")) {', 'for (String level : List.of("high")) {')],
        "wrong-examples-up-to-six": [("examples.size() > 4", "examples.size() > 6")],
        "wrong-all-report-examples": [('if (!verdicts.equals(Set.of("report", "skip")))', 'if (!Set.of("report", "skip").containsAll(verdicts))')],
        "wrong-reason-optional": [('if (!present(e.get("reason"))) throw', 'if (e.get("reason") == null) throw')],
        "wrong-unknown-category": [('if ("report".equals(e.get("verdict")) && !ids.contains(e.get("category"))) throw', "if (false) throw")],
        "wrong-disable-few": [("boolean off = items.size() >= minReviewed && precision < minPrecision;", "boolean off = precision < minPrecision;")],
        "wrong-precision-inverted": [("Math.round((double) accepted / items.size() * 100) / 100.0", "Math.round((double) (items.size() - accepted) / items.size() * 100) / 100.0")],
        "wrong-boundary-disables": [("precision < minPrecision", "precision <= minPrecision")],
        "wrong-top-by-name": [("top.sort((a, b) -> b.getValue().equals(a.getValue()) ? a.getKey().compareTo(b.getKey()) : b.getValue().compareTo(a.getValue()));", "top.sort((a, b) -> a.getKey().compareTo(b.getKey()));")],
        "wrong-top-uncapped": [("i < Math.min(3, top.size())", "i < top.size()")],
        "wrong-counts-accepted": [('else if ("dismissed".equals(f.get("verdict"))) counts.merge(', "if (true) counts.merge(")],
        "wrong-ask-defaults": [("            else unresolved.add(f);", "            unresolved.add(f);")],
        "wrong-no-assumptions": [("if (defaults.containsKey(f)) assumptions.put(f, defaults.get(f));", "if (defaults.containsKey(f)) assumptions.size();")],
        "wrong-unattended-asks": [('return map("action", "stop", "ask", List.of(), "assumptions", assumptions);', 'return map("action", "ask", "ask", unresolved, "assumptions", assumptions);')],
        "wrong-unattended-guess": [('        if (!unresolved.isEmpty()) return map("action", "stop", "ask", List.of(), "assumptions", assumptions);\n', "")],
        "wrong-blank-is-present": [("return value == null || (value instanceof String s && s.isBlank());", "return value == null || (value instanceof String s && s.isEmpty());")],
    }),
    "kotlin": ("ReviewSpec.kt", {
        "wrong-diff-first": [('return out.joinToString("\\n")', 'return (out.takeLast(3) + out.dropLast(3)).joinToString("\\n")')],
        "wrong-vague-report-only": [("val phrase = vague(c[key] as String)", 'val phrase = if (key == "report") vague(c[key] as String) else null')],
        "wrong-vague-short-list": [('listOf("be conservative", "high-confidence"', 'listOf("high-confidence"')],
        "wrong-skip-blank": [('require(present(c[key])) { "criterion ${c["id"]}: $key is required" }', 'require(c[key] != null) { "criterion ${c["id"]}: $key is required" }')],
        "wrong-severity-high-only": [('for (level in listOf("high", "low")) require(present(severity[level]))', 'for (level in listOf("high")) require(present(severity[level]))')],
        "wrong-examples-up-to-six": [("examples.size in 2..4", "examples.size in 2..6")],
        "wrong-all-report-examples": [('require(examples.map { it["verdict"] }.toSet() == setOf("report", "skip"))', 'require(setOf("report", "skip").containsAll(examples.map { it["verdict"] }.toSet()))')],
        "wrong-reason-optional": [('require(present(e["reason"])) { "every example needs a reason" }', 'require(e["reason"] != null) { "every example needs a reason" }')],
        "wrong-unknown-category": [('require(e["verdict"] != "report" || e["category"] in ids) {', "require(true) {")],
        "wrong-disable-few": [("val off = items.size >= minReviewed && precision < minPrecision", "val off = precision < minPrecision")],
        "wrong-precision-inverted": [("Math.round(accepted.toDouble() / items.size * 100) / 100.0", "Math.round((items.size - accepted).toDouble() / items.size * 100) / 100.0")],
        "wrong-boundary-disables": [("precision < minPrecision", "precision <= minPrecision")],
        "wrong-top-by-name": [(".sortedWith(compareBy({ -it.value }, { it.key }))", ".sortedWith(compareBy({ it.key }))")],
        "wrong-top-uncapped": [(".take(3).map {", ".map {")],
        "wrong-counts-accepted": [('items.filter { it["verdict"] == "dismissed" }.groupingBy', "items.groupingBy")],
        "wrong-ask-defaults": [("val unresolved = missing.filter { it !in defaults }", "val unresolved = missing")],
        "wrong-no-assumptions": [("    missing.filter { it in defaults }.forEach { assumptions[it] = defaults.getValue(it) }\n", "")],
        "wrong-unattended-asks": [('unresolved.isNotEmpty() -> mapOf("action" to "stop", "ask" to emptyList<String>(), "assumptions" to assumptions)', 'unresolved.isNotEmpty() -> mapOf("action" to "ask", "ask" to unresolved, "assumptions" to assumptions)')],
        "wrong-unattended-guess": [('        unresolved.isNotEmpty() -> mapOf("action" to "stop", "ask" to emptyList<String>(), "assumptions" to assumptions)\n', "")],
        "wrong-blank-is-present": [("(value is String && value.isBlank())", "(value is String && value.isEmpty())")],
    }),
}


PLANTS[f"{X}/62-structured-output-at-the-architect-level/unit-01/practice-1"] = {
    "python": ("extraction.py", {
        "wrong-grounding-skipped": [('        if not isinstance(quote, str) or quote == "" or quote not in document:', '        if not isinstance(quote, str) or quote == "":')],
        "wrong-null-needs-quote": [('        value = record[field]\n        if value is None or value == "unclear":', '        value = record[field]\n        if value == "unclear":')],
        "wrong-feedback-all-errors": [('feedback = {"previous": record, "errors": retryable}', 'feedback = {"previous": record, "errors": errors}')],
        "wrong-feedback-no-previous": [('feedback = {"previous": record, "errors": retryable}', 'feedback = {"previous": None, "errors": retryable}')],
        "wrong-retry-absent": [("        if not retryable:\n            break\n", "")],
        "wrong-absent-failed": [('status = "needs_review" if all(e["kind"] == "absent" for e in errors) else "failed"', 'status = "failed"')],
        "wrong-extra-retry": [("if attempts > max_retries:", "if attempts > max_retries + 1:")],
        "wrong-sum-unchecked": [('    if abs(sum(items) - record["calculated_total"]) > 0.005:', "    if False:")],
        "wrong-conflict-error": [(' and not record["conflict_detected"]:', ":")],
        "wrong-conflict-valid": [('    elif record["conflict_detected"]:', "    elif False:")],
        "wrong-unclear-needs-quote": [('        value = record[field]\n        if value is None or value == "unclear":', "        value = record[field]\n        if value is None:")],
        "wrong-other-no-detail": [('    if record["currency"] == "other" and not (isinstance(detail, str) and detail.strip()):', "    if False:")],
        "wrong-currency-open": [('    if record["currency"] not in CURRENCIES:', '    if not isinstance(record["currency"], str):')],
        "wrong-merge-last-wins": [('            if current is None or current == "unclear":', "            if True:")],
        "wrong-merge-no-conflict": [('            elif current != value and field not in merged["conflicts"]:', "            elif False:")],
        "wrong-accuracy-validated-only": [('"all_documents": round(correct / total, 2) if total else 0.0', '"all_documents": round(correct / valid, 2) if valid else 0.0')],
        "wrong-accuracy-missing-skipped": [("    total = len(labels)", "    total = len([d for d in labels if d in results])")],
        "wrong-force-always": [('    if model in NO_FORCING:\n        return {"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": True}\n', "")],
        "wrong-any-for-single": [("    if len(tools) > 1:", "    if len(tools) >= 1:")],
        "wrong-no-verify": [('{"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": True}', '{"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": False}')],
    }),
    "typescript": ("extraction.ts", {
        "wrong-grounding-skipped": [('if (typeof quote !== "string" || quote === "" || !document.includes(quote)) err("ungrounded"', 'if (typeof quote !== "string" || quote === "") err("ungrounded"')],
        "wrong-null-needs-quote": [('if (value === null || value === "unclear") continue;', 'if (value === "unclear") continue;')],
        "wrong-feedback-all-errors": [("feedback = { previous: record, errors: retryable };", "feedback = { previous: record, errors };")],
        "wrong-feedback-no-previous": [("feedback = { previous: record, errors: retryable };", "feedback = { previous: null, errors: retryable };")],
        "wrong-retry-absent": [("    if (retryable.length === 0) break;\n", "")],
        "wrong-absent-failed": [('status = errors.every((e) => e.kind === "absent") ? "needs_review" : "failed";', 'status = "failed";')],
        "wrong-extra-retry": [("if (attempts > maxRetries) break;", "if (attempts > maxRetries + 1) break;")],
        "wrong-sum-unchecked": [("if (Math.abs(sum - record.calculated_total) > 0.005) err(", "if (false) err(")],
        "wrong-conflict-error": [(" && !record.conflict_detected) {", ") {")],
        "wrong-conflict-valid": [('else if (record.conflict_detected) status = "needs_review";', 'else if (false) status = "needs_review";')],
        "wrong-unclear-needs-quote": [('if (value === null || value === "unclear") continue;', "if (value === null) continue;")],
        "wrong-other-no-detail": [('if (record.currency === "other" && !(typeof record.currency_detail === "string" && record.currency_detail.trim() !== "")) err(', "if (false) err(")],
        "wrong-currency-open": [("if (!CURRENCIES.includes(record.currency)) err(", 'if (typeof record.currency !== "string") err(')],
        "wrong-merge-last-wins": [('if (current === null || current === "unclear") {', "if (true) {")],
        "wrong-merge-no-conflict": [("} else if (current !== value && !merged.conflicts.includes(field)) {", "} else if (false) {")],
        "wrong-accuracy-validated-only": [("all_documents: total ? round2(correct / total) : 0", "all_documents: valid ? round2(correct / valid) : 0")],
        "wrong-accuracy-missing-skipped": [("const total = Object.keys(labels).length;", "const total = Object.keys(labels).filter((d) => d in results).length;")],
        "wrong-force-always": [('  if (NO_FORCING.has(model)) return { tool_choice: { type: "auto" }, strict: true, verify_reply: true };\n', "")],
        "wrong-any-for-single": [("if (tools.length > 1) return", "if (tools.length >= 1) return")],
        "wrong-no-verify": [('{ tool_choice: { type: "auto" }, strict: true, verify_reply: true }', '{ tool_choice: { type: "auto" }, strict: true, verify_reply: false }')],
    }),
    "java": ("Extraction.java", {
        "wrong-grounding-skipped": [('if (!(quote instanceof String q) || q.isEmpty() || !document.contains(q)) err(', "if (!(quote instanceof String q) || q.isEmpty()) err(")],
        "wrong-null-needs-quote": [('            if (value == null || "unclear".equals(value)) continue;\n            Object quote', '            if ("unclear".equals(value)) continue;\n            Object quote')],
        "wrong-feedback-all-errors": [('feedback = map("previous", record, "errors", retryable);', 'feedback = map("previous", record, "errors", errors);')],
        "wrong-feedback-no-previous": [('feedback = map("previous", record, "errors", retryable);', 'feedback = map("previous", null, "errors", retryable);')],
        "wrong-retry-absent": [("            if (retryable.isEmpty()) break;\n", "")],
        "wrong-absent-failed": [('status = errors.stream().allMatch(e -> "absent".equals(e.get("kind"))) ? "needs_review" : "failed";', 'status = "failed";')],
        "wrong-extra-retry": [("if (attempts > maxRetries) break;", "if (attempts > maxRetries + 1) break;")],
        "wrong-sum-unchecked": [("if (Math.abs(sum - calculated) > 0.005) err(", "if (false) err(")],
        "wrong-conflict-error": [('if (Math.abs(stated - calculated) > 0.005 && !((Boolean) record.get("conflict_detected"))) {', "if (Math.abs(stated - calculated) > 0.005) {")],
        "wrong-conflict-valid": [('else if (Boolean.TRUE.equals(record.get("conflict_detected"))) status = "needs_review";', 'else if (false) status = "needs_review";')],
        "wrong-unclear-needs-quote": [('            if (value == null || "unclear".equals(value)) continue;\n            Object quote', "            if (value == null) continue;\n            Object quote")],
        "wrong-other-no-detail": [('if ("other".equals(record.get("currency")) && !(detail instanceof String d && !d.isBlank())) err(', "if (false) err(")],
        "wrong-currency-open": [('if (!CURRENCIES.contains(record.get("currency"))) err(', 'if (!(record.get("currency") instanceof String)) err(')],
        "wrong-merge-last-wins": [('if (current == null || "unclear".equals(current)) {', "if (true) {")],
        "wrong-merge-no-conflict": [("} else if (!current.equals(value) && !conflicts.contains(field)) {", "} else if (false) {")],
        "wrong-accuracy-validated-only": [('"all_documents", total > 0 ? round2((double) correct / total) : 0.0', '"all_documents", valid > 0 ? round2((double) correct / valid) : 0.0')],
        "wrong-accuracy-missing-skipped": [("int total = labels.size();", "int total = (int) labels.keySet().stream().filter(results::containsKey).count();")],
        "wrong-force-always": [('        if (NO_FORCING.contains(model)) return map("tool_choice", map("type", "auto"), "strict", true, "verify_reply", true);\n', "")],
        "wrong-any-for-single": [("if (tools.size() > 1) return", "if (tools.size() >= 1) return")],
        "wrong-no-verify": [('map("tool_choice", map("type", "auto"), "strict", true, "verify_reply", true)', 'map("tool_choice", map("type", "auto"), "strict", true, "verify_reply", false)')],
    }),
    "kotlin": ("Extraction.kt", {
        "wrong-grounding-skipped": [("if (!(quote is String && quote.isNotEmpty() && document.contains(quote))) errors +=", "if (!(quote is String && quote.isNotEmpty())) errors +=")],
        "wrong-null-needs-quote": [('        if (value == null || value == "unclear") continue\n        val quote', '        if (value == "unclear") continue\n        val quote')],
        "wrong-feedback-all-errors": [('feedback = mapOf("previous" to record, "errors" to retryable)', 'feedback = mapOf("previous" to record, "errors" to errors)')],
        "wrong-feedback-no-previous": [('feedback = mapOf("previous" to record, "errors" to retryable)', 'feedback = mapOf("previous" to null, "errors" to retryable)')],
        "wrong-retry-absent": [("        if (retryable.isEmpty()) break\n", "")],
        "wrong-absent-failed": [('errors.isNotEmpty() -> if (errors.all { it["kind"] == "absent" }) "needs_review" else "failed"', 'errors.isNotEmpty() -> "failed"')],
        "wrong-extra-retry": [("if (attempts > maxRetries) break", "if (attempts > maxRetries + 1) break")],
        "wrong-sum-unchecked": [("if (Math.abs(sum - calculated) > 0.005) errors +=", "if (false) errors +=")],
        "wrong-conflict-error": [(' && record["conflict_detected"] != true) {', ") {")],
        "wrong-conflict-valid": [('record["conflict_detected"] == true -> "needs_review"', 'false -> "needs_review"')],
        "wrong-unclear-needs-quote": [('        if (value == null || value == "unclear") continue\n        val quote', "        if (value == null) continue\n        val quote")],
        "wrong-other-no-detail": [('if (record["currency"] == "other" && !(detail is String && detail.isNotBlank())) errors +=', "if (false) errors +=")],
        "wrong-currency-open": [('if (record["currency"] !in CURRENCIES) errors +=', 'if (record["currency"] !is String) errors +=')],
        "wrong-merge-last-wins": [('if (current == null || current == "unclear") {', "if (true) {")],
        "wrong-merge-no-conflict": [("} else if (current != value && field !in conflicts) {", "} else if (false) {")],
        "wrong-accuracy-validated-only": [('"all_documents" to if (total > 0) round2(correct.toDouble() / total) else 0.0', '"all_documents" to if (valid > 0) round2(correct.toDouble() / valid) else 0.0')],
        "wrong-accuracy-missing-skipped": [("val total = labels.size", "val total = labels.keys.count { it in results }")],
        "wrong-force-always": [('    model in NO_FORCING -> mapOf("tool_choice" to mapOf("type" to "auto"), "strict" to true, "verify_reply" to true)\n', "")],
        "wrong-any-for-single": [("tools.size > 1 ->", "tools.size >= 1 ->")],
        "wrong-no-verify": [('mapOf("tool_choice" to mapOf("type" to "auto"), "strict" to true, "verify_reply" to true)', 'mapOf("tool_choice" to mapOf("type" to "auto"), "strict" to true, "verify_reply" to false)')],
    }),
}


_WF, _CM, _SC = ".github/workflows/claude-review.yml", "CLAUDE.md", "review-schema.json"
_CFG60 = {
    "wrong-vague-criteria": {_CM: [("## Skip\n", "## Skip\n- Be conservative and only report high-confidence findings.\n")]},
    "wrong-no-severity-example": {_CM: [("- high: data loss or a security hole, for example `DELETE FROM orders` built from request input.", "- high: data loss or a security hole.")]},
    "wrong-no-fixtures": {_CM: [("build their data from `tests/fixtures/`", "build their data inline")]},
    "wrong-home-path": {_CM: [("## Skip\n", "## Skip\n- My notes are in /home/dev/notes.\n")]},
    "wrong-schema-severity-open": {_SC: [('"severity": {"type": "string", "enum": ["low", "medium", "high"]},', '"severity": {"type": "string"},')]},
    "wrong-schema-draft-2020": {_SC: [('{\n  "type": "object",\n  "properties": {\n    "findings"', '{\n  "$schema": "https://json-schema.org/draft/2020-12/schema",\n  "type": "object",\n  "properties": {\n    "findings"')]},
    "wrong-schema-min-length": {_SC: [('"issue": {"type": "string"},', '"issue": {"type": "string", "minLength": 1},')]},
    "wrong-schema-pattern-optional": {_SC: [('"suggested_fix", "detected_pattern"],', '"suggested_fix"],')]},
    "wrong-schema-extra-allowed": {_SC: [('        "additionalProperties": false', '        "additionalProperties": true')]},
    "wrong-no-print": {_WF: [('claude --bare -p "Review', 'claude --bare "Review')]},
    "wrong-no-json": {_WF: [("            --output-format json \\\n", "")]},
    "wrong-no-schema-flag": {_WF: [('            --json-schema "$(cat review-schema.json)" \\\n', "")]},
    "wrong-no-max-turns": {_WF: [("            --max-turns 8 \\\n", "")]},
    "wrong-bash-tool": {_WF: [('--allowedTools "Read,Grep,Glob"', '--allowedTools "Read,Grep,Glob,Bash"')]},
    "wrong-no-bare": {_WF: [("claude --bare -p", "claude -p")]},
    "wrong-bare-no-context": {_WF: [("            --append-system-prompt-file CLAUDE.md \\\n", "")]},
    "wrong-literal-key": {_WF: [("ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}", "ANTHROPIC_API_KEY: sk-ant-api03-EXAMPLEKEY123456")]},
    "wrong-no-timeout": {_WF: [("    timeout-minutes: 15\n", "")]},
    "wrong-write-permission": {_WF: [("      contents: read\n", "      contents: write\n")]},
}
_PY60 = {
    "wrong-gate-nonzero-ignored": {"review_gate.py": [("    if exit_code != 0:", "    if False:")]},
    "wrong-gate-error-ignored": {"review_gate.py": [('    if envelope.get("is_error") or envelope.get("subtype") != "success":', "    if False:")]},
    "wrong-gate-no-output-ok": {"review_gate.py": [('output = envelope.get("structured_output")', 'output = envelope.get("structured_output") or {"findings": []}')]},
    "wrong-gate-no-schema": {"review_gate.py": [('        problems += [f"schema {e}" for e in schema_check(output, schema)]', "        pass")]},
    "wrong-gate-floor-ignored": {"review_gate.py": [(' and SEVERITIES.index(f["severity"]) >= floor]', "]")]},
    "wrong-gate-disabled-ignored": {"review_gate.py": [('if f["category"] not in policy["disabled_categories"] and ', "if ")]},
    "wrong-gate-never-blocks": {"review_gate.py": [('blocked = any(c["severity"] in policy["fail_on"] for c in comments)', "blocked = False")]},
    "wrong-gate-blocks-on-any": {"review_gate.py": [('blocked = any(c["severity"] in policy["fail_on"] for c in comments)', "blocked = bool(comments)")]},
    "wrong-prompt-no-new-only": {"review_gate.py": [(', "Report only findings that are new or still unaddressed."]', "]")]},
    "wrong-prompt-no-prior": {"review_gate.py": [('    if prior:\n        lines += ["<already_reported>"]', '    if False:\n        lines += ["<already_reported>"]')]},
    "wrong-prompt-no-tests": {"review_gate.py": [('    if existing_tests:\n        lines += ["<existing_tests>"]', '    if False:\n        lines += ["<existing_tests>"]')]},
    "wrong-prompt-diff-first": {"review_gate.py": [('    lines += ["<diff>", diff, "</diff>"]', '    lines = ["<diff>", diff, "</diff>"] + lines')]},
}
_TS60 = {
    "wrong-gate-nonzero-ignored": {"reviewGate.ts": [("if (exitCode !== 0) problems.push(", "if (false) problems.push(")]},
    "wrong-gate-error-ignored": {"reviewGate.ts": [('if (envelope.is_error || envelope.subtype !== "success") problems.push(', "if (false) problems.push(")]},
    "wrong-gate-no-output-ok": {"reviewGate.ts": [("const output = envelope.structured_output;", "const output = envelope.structured_output ?? { findings: [] };")]},
    "wrong-gate-no-schema": {"reviewGate.ts": [("else problems.push(...schemaCheck(output, schema).map((e: string) => `schema ${e}`));", "else void 0;")]},
    "wrong-gate-floor-ignored": {"reviewGate.ts": [(" && SEVERITIES.indexOf(f.severity) >= floor)", ")")]},
    "wrong-gate-disabled-ignored": {"reviewGate.ts": [("!policy.disabled_categories.includes(f.category) && ", "")]},
    "wrong-gate-never-blocks": {"reviewGate.ts": [("const blocked = comments.some((c: any) => policy.fail_on.includes(c.severity));", "const blocked = false;")]},
    "wrong-gate-blocks-on-any": {"reviewGate.ts": [("const blocked = comments.some((c: any) => policy.fail_on.includes(c.severity));", "const blocked = comments.length > 0;")]},
    "wrong-prompt-no-new-only": {"reviewGate.ts": [(', "Report only findings that are new or still unaddressed."];', "];")]},
    "wrong-prompt-no-prior": {"reviewGate.ts": [('if (prior.length > 0) lines.push("<already_reported>", ', 'if (false) lines.push("<already_reported>", ')]},
    "wrong-prompt-no-tests": {"reviewGate.ts": [('if (existingTests.length > 0) lines.push("<existing_tests>", ', 'if (false) lines.push("<existing_tests>", ')]},
    "wrong-prompt-diff-first": {"reviewGate.ts": [('lines.push("<diff>", diff, "</diff>");', 'lines.unshift("<diff>", diff, "</diff>");')]},
}
PLANTS[f"{X}/60-claude-code-in-ci/unit-01/practice-1"] = {"python": ("review_gate.py", {**_CFG60, **_PY60}), "typescript": ("reviewGate.ts", {**_CFG60, **_TS60})}


# ===== Level 3: module 63 =====
PLANTS[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"] = {
    "python": ("batch_review.py", {
        "wrong-ignores-handling": [("    interval = sla_hours - window_hours - handling_hours\n", "    interval = sla_hours - window_hours\n")],
        "wrong-oversized-resubmitted": [('        if sizes.get(custom_id, 0) > limit:\n            action = "chunk"\n        elif kind == "invalid_request":', '        if kind == "invalid_request":')],
        "wrong-resubmit-all": [('        if kind == "succeeded":\n            continue\n', "")],
        "wrong-no-integration-pass": [('    if len(files) > 1:\n        passes.append({"name": "integration", "files": list(files)})\n', "")],
        "wrong-lone-confident-accepted": [('"accept" if count >= 2 and m["confidence"] >= 80 else "verify"', '"accept" if m["confidence"] >= 80 else "verify"')],
    }),
    "typescript": ("batchReview.ts", {
        "wrong-ignores-handling": [("const interval = slaHours - windowHours - handlingHours;", "const interval = slaHours - windowHours;")],
        "wrong-oversized-resubmitted": [('    if ((sizes[customId] ?? 0) > limit) action = "chunk";\n    else if (kind === "invalid_request") action = "fix";', '    if (kind === "invalid_request") action = "fix";')],
        "wrong-resubmit-all": [('    if (kind === "succeeded") continue;\n', "")],
        "wrong-no-integration-pass": [('  if (files.length > 1) passes.push({ name: "integration", files: [...files] });\n', "")],
        "wrong-lone-confident-accepted": [("count >= 2 && m.confidence >= 80 ?", "m.confidence >= 80 ?")],
    }),
    "java": ("BatchReview.java", {
        "wrong-ignores-handling": [("int interval = slaHours - windowHours - handlingHours;", "int interval = slaHours - windowHours;")],
        "wrong-oversized-resubmitted": [('            if (sizes.getOrDefault(r.customId(), 0) > limit) action = "chunk";\n            else if (r.kind().equals("invalid_request")) action = "fix";', '            if (r.kind().equals("invalid_request")) action = "fix";')],
        "wrong-resubmit-all": [('            if (r.kind().equals("succeeded")) continue;\n', "")],
        "wrong-no-integration-pass": [('        if (files.size() > 1) passes.add(new Pass("integration", List.copyOf(files)));\n', "")],
        "wrong-lone-confident-accepted": [("count >= 2 && conf >= 80 ?", "conf >= 80 ?")],
    }),
    "kotlin": ("BatchReview.kt", {
        "wrong-ignores-handling": [("val interval = slaHours - windowHours - handlingHours", "val interval = slaHours - windowHours")],
        "wrong-oversized-resubmitted": [('if ((sizes[r.customId] ?: 0) > limit) "chunk" else if (r.kind == "invalid_request") "fix" else "resubmit"', 'if (r.kind == "invalid_request") "fix" else "resubmit"')],
        "wrong-resubmit-all": [('        if (r.kind == "succeeded") continue\n', "")],
        "wrong-no-integration-pass": [('    if (files.size > 1) passes += Pass("integration", files.toList())\n', "")],
        "wrong-lone-confident-accepted": [('if (count >= 2 && conf >= 80) "accept"', 'if (conf >= 80) "accept"')],
    }),
}


# ===== Level 3: module 63 =====
PLANTS[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"] = {
    "python": ("batch_review.py", {
        "wrong-ignores-handling": [("    interval = sla_hours - window_hours - handling_hours\n", "    interval = sla_hours - window_hours\n")],
        "wrong-oversized-resubmitted": [('        if sizes.get(custom_id, 0) > limit:\n            action = "chunk"\n        elif kind == "invalid_request":', '        if kind == "invalid_request":')],
        "wrong-resubmit-all": [('        if kind == "succeeded":\n            continue\n', "")],
        "wrong-no-integration-pass": [('    if len(files) > 1:\n        passes.append({"name": "integration", "files": list(files)})\n', "")],
        "wrong-lone-confident-accepted": [('"accept" if count >= 2 and m["confidence"] >= 80 else "verify"', '"accept" if m["confidence"] >= 80 else "verify"')],
    }),
    "typescript": ("batchReview.ts", {
        "wrong-ignores-handling": [("const interval = slaHours - windowHours - handlingHours;", "const interval = slaHours - windowHours;")],
        "wrong-oversized-resubmitted": [('    if ((sizes[customId] ?? 0) > limit) action = "chunk";\n    else if (kind === "invalid_request") action = "fix";', '    if (kind === "invalid_request") action = "fix";')],
        "wrong-resubmit-all": [('    if (kind === "succeeded") continue;\n', "")],
        "wrong-no-integration-pass": [('  if (files.length > 1) passes.push({ name: "integration", files: [...files] });\n', "")],
        "wrong-lone-confident-accepted": [("count >= 2 && m.confidence >= 80 ?", "m.confidence >= 80 ?")],
    }),
    "java": ("BatchReview.java", {
        "wrong-ignores-handling": [("int interval = slaHours - windowHours - handlingHours;", "int interval = slaHours - windowHours;")],
        "wrong-oversized-resubmitted": [('            if (sizes.getOrDefault(r.customId(), 0) > limit) action = "chunk";\n            else if (r.kind().equals("invalid_request")) action = "fix";', '            if (r.kind().equals("invalid_request")) action = "fix";')],
        "wrong-resubmit-all": [('            if (r.kind().equals("succeeded")) continue;\n', "")],
        "wrong-no-integration-pass": [('        if (files.size() > 1) passes.add(new Pass("integration", List.copyOf(files)));\n', "")],
        "wrong-lone-confident-accepted": [("count >= 2 && conf >= 80 ?", "conf >= 80 ?")],
    }),
    "kotlin": ("BatchReview.kt", {
        "wrong-ignores-handling": [("val interval = slaHours - windowHours - handlingHours", "val interval = slaHours - windowHours")],
        "wrong-oversized-resubmitted": [('if ((sizes[r.customId] ?: 0) > limit) "chunk" else if (r.kind == "invalid_request") "fix" else "resubmit"', 'if (r.kind == "invalid_request") "fix" else "resubmit"')],
        "wrong-resubmit-all": [('        if (r.kind == "succeeded") continue\n', "")],
        "wrong-no-integration-pass": [('    if (files.size > 1) passes += Pass("integration", files.toList())\n', "")],
        "wrong-lone-confident-accepted": [('if (count >= 2 && conf >= 80) "accept"', 'if (conf >= 80) "accept"')],
    }),
}


# ===== Level 3: module 64 =====
PLANTS[f"{X}/64-keeping-what-matters-in-long-conversations/unit-01/practice-1"] = {
    "python": ("context_builder.py", {
        "wrong-no-trim": [("    return {k: record[k] for k in keep if k in record}", "    return dict(record)")],
        "wrong-older-overwrites": [('    elif as_of >= current["as_of"]:', "    elif True:")],
        "wrong-all-customers": [('    mine = [f for f in facts if f["customer"] == customer]', "    mine = list(facts)")],
        "wrong-facts-last": [(r'    return "\n\n".join(parts)', r'    return "\n\n".join(reversed(parts))')],
        "wrong-pair-split": [('        if m["kind"] == "tool_use" and', '        if False and m["kind"] == "tool_use" and')],
        "wrong-loose-summary-check": [('f["value"] not in summary', 'f["value"][:2] not in summary')],
    }),
    "typescript": ("contextBuilder.ts", {
        "wrong-no-trim": [("  for (const k of keep) if (k in record) out[k] = record[k];", "  Object.assign(out, record);")],
        "wrong-older-overwrites": [("} else if (asOf >= current.as_of) {", "} else if (true) {")],
        "wrong-all-customers": [("const mine = facts.filter((f) => f.customer === customer);", "const mine = facts;")],
        "wrong-facts-last": [('return parts.join("\\n\\n");', 'return parts.reverse().join("\\n\\n");')],
        "wrong-pair-split": [('if (m.kind === "tool_use" &&', 'if (false && m.kind === "tool_use" &&')],
        "wrong-loose-summary-check": [("!summary.includes(f.value)", "!summary.includes(f.value.slice(0, 2))")],
    }),
    "java": ("ContextBuilder.java", {
        "wrong-no-trim": [("for (String k : keep) if (record.containsKey(k)) out.put(k, record.get(k));", "out.putAll(record);")],
        "wrong-older-overwrites": [("} else if (asOf.compareTo(current.asOf()) >= 0) {", "} else if (true) {")],
        "wrong-all-customers": [("if (f.customer().equals(customer)) mine.add(", "mine.add(")],
        "wrong-facts-last": [('return String.join("\\n\\n", parts);', 'java.util.Collections.reverse(parts);\n        return String.join("\\n\\n", parts);')],
        "wrong-pair-split": [('if (m.kind().equals("tool_use") &&', 'if (false && m.kind().equals("tool_use") &&')],
        "wrong-loose-summary-check": [("!summary.contains(f.value())", "!summary.contains(f.value().substring(0, 2))")],
    }),
    "kotlin": ("ContextBuilder.kt", {
        "wrong-no-trim": [("for (k in keep) if (k in record) out[k] = record.getValue(k)", "out.putAll(record)")],
        "wrong-older-overwrites": [("} else if (asOf >= current.asOf) {", "} else if (true) {")],
        "wrong-all-customers": [("val mine = facts.filter { it.customer == customer }", "val mine = facts")],
        "wrong-facts-last": [('return parts.joinToString("\\n\\n")', 'return parts.asReversed().joinToString("\\n\\n")')],
        "wrong-pair-split": [('if (m.kind == "tool_use" &&', 'if (false && m.kind == "tool_use" &&')],
        "wrong-loose-summary-check": [("it.value !in summary", "it.value.take(2) !in summary")],
    }),
}


# ===== Level 3: module 65 =====
PLANTS[f"{X}/65-escalation-and-ambiguity/unit-01/practice-1"] = {
    "python": ("escalation.py", {
        "wrong-investigate-first": [('    if case.get("asked_for_person", False):', '    if case.get("asked_for_person", False) and not case.get("policy_covers", True):')],
        "wrong-angry-escalates": [('    if case.get("asked_for_person", False):', '    if case.get("asked_for_person", False) or case.get("sentiment", "calm") == "angry":')],
        "wrong-low-confidence-escalates": [('    if case.get("attempts_without_progress", 0) >= max_attempts:', '    if case.get("attempts_without_progress", 0) >= max_attempts or case.get("confidence", 100) < 50:')],
        "wrong-picks-first-match": [('    if case.get("matches", 1) > 1:', "    if False:")],
        "wrong-policy-gap-resolved": [('    if not case.get("policy_covers", True):', "    if False:")],
        "wrong-no-acknowledgement": [('case.get("sentiment", "calm") != "calm")', "False)")],
        "wrong-clarify-all-fields": [('field != "id" and len({m.get(field) for m in matches}) > 1', 'field != "id"')],
        "wrong-handoff-transcript": [(r'    return "\n".join(lines)', r'    return "\n".join(lines + [case.get("transcript", "")])')],
    }),
    "typescript": ("escalation.ts", {
        "wrong-investigate-first": [("if (c.asked_for_person ?? false) return", "if ((c.asked_for_person ?? false) && !(c.policy_covers ?? true)) return")],
        "wrong-angry-escalates": [("if (c.asked_for_person ?? false) return", 'if ((c.asked_for_person ?? false) || c.sentiment === "angry") return')],
        "wrong-low-confidence-escalates": [("if ((c.attempts_without_progress ?? 0) >= maxAttempts) return", "if ((c.attempts_without_progress ?? 0) >= maxAttempts || (c.confidence ?? 100) < 50) return")],
        "wrong-picks-first-match": [("if ((c.matches ?? 1) > 1) return", "if (false) return")],
        "wrong-policy-gap-resolved": [("if (!(c.policy_covers ?? true)) return", "if (false) return")],
        "wrong-no-acknowledgement": [('(c.sentiment ?? "calm") !== "calm")', "false)")],
        "wrong-clarify-all-fields": [(" && new Set(matches.map((m) => m[field])).size > 1", "")],
        "wrong-handoff-transcript": [('return lines.join("\\n");', 'return lines.concat(c.transcript ?? "").join("\\n");')],
    }),
    "java": ("Escalation.java", {
        "wrong-investigate-first": [("if (c.askedForPerson()) return", "if (c.askedForPerson() && !c.policyCovers()) return")],
        "wrong-angry-escalates": [("if (c.askedForPerson()) return", 'if (c.askedForPerson() || c.sentiment().equals("angry")) return')],
        "wrong-low-confidence-escalates": [("if (c.attemptsWithoutProgress() >= maxAttempts) return", "if (c.attemptsWithoutProgress() >= maxAttempts || c.confidence() < 50) return")],
        "wrong-picks-first-match": [("if (c.matches() > 1) return", "if (false) return")],
        "wrong-policy-gap-resolved": [("if (!c.policyCovers()) return", "if (false) return")],
        "wrong-no-acknowledgement": [('!c.sentiment().equals("calm"))', "false)")],
        "wrong-clarify-all-fields": [("if (values.size() > 1) out.add(field);", "out.add(field);")],
        "wrong-handoff-transcript": [('return String.join("\\n", lines);', 'return String.join("\\n", lines) + c.transcript();')],
    }),
    "kotlin": ("Escalation.kt", {
        "wrong-investigate-first": [("c.askedForPerson -> ", "c.askedForPerson && !c.policyCovers -> ")],
        "wrong-angry-escalates": [("c.askedForPerson -> ", 'c.askedForPerson || c.sentiment == "angry" -> ')],
        "wrong-low-confidence-escalates": [("c.attemptsWithoutProgress >= maxAttempts -> ", "c.attemptsWithoutProgress >= maxAttempts || c.confidence < 50 -> ")],
        "wrong-picks-first-match": [("c.matches > 1 -> ", "false -> ")],
        "wrong-policy-gap-resolved": [("!c.policyCovers -> ", "false -> ")],
        "wrong-no-acknowledgement": [('c.sentiment != "calm")', "false)")],
        "wrong-clarify-all-fields": [('field != "id" && matches.map { it[field] }.toSet().size > 1', 'field != "id"')],
        "wrong-handoff-transcript": [(').joinToString("\\n")', ').joinToString("\\n") + c.transcript')],
    }),
}


# ===== Level 3: module 66 =====
PLANTS[f"{X}/66-errors-across-agents/unit-01/practice-1"] = {
    "python": ("error_flow.py", {
        "wrong-empty-is-error": [('"success" if items else "empty"', '"success" if items else "failed"')],
        "wrong-retry-permission": [("        if kind in TRANSIENT and attempts < max_attempts:", "        if attempts < max_attempts:")],
        "wrong-drop-partial": [('"partial_results": reply.get("partial", [])', '"partial_results": []')],
        "wrong-generic-error": [('"failure_type": kind, "attempted": query, ', "")],
        "wrong-stop-on-failure": [("    plan = []\n    for topic, outcome in results.items():", '    plan = []\n    if any(o["status"] == "failed" for o in results.values()):\n        return [(topic, "abort") for topic in results]\n    for topic, outcome in results.items():')],
        "wrong-gap-as-supported": [('        elif outcome["status"] == "success":\n            groups["Well-supported"].append(topic)', '        elif outcome["status"] in ("success", "failed"):\n            groups["Well-supported"].append(topic)')],
        "wrong-missing-topic-skipped": [('        if outcome is None:\n            groups["Gaps"].append(f"{topic} (not searched)")', "        if outcome is None:\n            continue")],
    }),
    "typescript": ("errorFlow.ts", {
        "wrong-empty-is-error": [('status: items.length > 0 ? "success" : "empty"', 'status: items.length > 0 ? "success" : "failed"')],
        "wrong-retry-permission": [("if (TRANSIENT.includes(kind) && attempts < maxAttempts) continue;", "if (attempts < maxAttempts) continue;")],
        "wrong-drop-partial": [("partial_results: reply.partial ?? [], ", "partial_results: [], ")],
        "wrong-generic-error": [("failure_type: kind, attempted: query, ", "")],
        "wrong-stop-on-failure": [("  for (const [topic, outcome] of Object.entries(results)) {\n    let action", '  if (Object.values(results).some((o: any) => o.status === "failed")) return Object.keys(results).map((t): [string, string] => [t, "abort"]);\n  for (const [topic, outcome] of Object.entries(results)) {\n    let action')],
        "wrong-gap-as-supported": [('else if (outcome.status === "success") groups["Well-supported"].push(topic);', 'else if (outcome.status === "success" || outcome.status === "failed") groups["Well-supported"].push(topic);')],
        "wrong-missing-topic-skipped": [('if (outcome === undefined) groups["Gaps"].push(`${topic} (not searched)`);', "if (outcome === undefined) continue;")],
    }),
    "java": ("ErrorFlow.java", {
        "wrong-empty-is-error": [('reply.items().isEmpty() ? "empty" : "success"', 'reply.items().isEmpty() ? "failed" : "success"')],
        "wrong-retry-permission": [("if (TRANSIENT.contains(kind) && attempts < maxAttempts) continue;", "if (attempts < maxAttempts) continue;")],
        "wrong-drop-partial": [("reply.partial() == null ? List.of() : reply.partial()", "List.of()")],
        "wrong-generic-error": [('new Outcome("failed", List.of(), attempts, kind, query, ', 'new Outcome("failed", List.of(), attempts, null, null, ')],
        "wrong-stop-on-failure": [("        for (Map.Entry<String, Outcome> e : results.entrySet()) {\n            Outcome o = e.getValue();\n", '        for (Map.Entry<String, Outcome> e : results.entrySet()) {\n            if (e.getValue().status().equals("failed")) {\n                List<Step> aborted = new ArrayList<>();\n                for (String t : results.keySet()) aborted.add(new Step(t, "abort"));\n                return aborted;\n            }\n        }\n        for (Map.Entry<String, Outcome> e : results.entrySet()) {\n            Outcome o = e.getValue();\n')],
        "wrong-gap-as-supported": [('else if (o.status().equals("success")) groups.get("Well-supported").add(topic);', 'else if (o.status().equals("success") || o.status().equals("failed")) groups.get("Well-supported").add(topic);')],
        "wrong-missing-topic-skipped": [('if (o == null) groups.get("Gaps").add(topic + " (not searched)");', "if (o == null) continue;")],
    }),
    "kotlin": ("ErrorFlow.kt", {
        "wrong-empty-is-error": [('if (reply.items.isEmpty()) "empty" else "success"', 'if (reply.items.isEmpty()) "failed" else "success"')],
        "wrong-retry-permission": [("if (kind in TRANSIENT && attempts < maxAttempts) continue", "if (attempts < maxAttempts) continue")],
        "wrong-drop-partial": [("query, reply.partial, ALTERNATIVES[kind]", "query, emptyList(), ALTERNATIVES[kind]")],
        "wrong-generic-error": [('Outcome("failed", emptyList(), attempts, kind, query, reply.partial,', 'Outcome("failed", emptyList(), attempts, null, null, reply.partial,')],
        "wrong-stop-on-failure": [("results.map { (topic, o) ->\n", 'results.map { (topic, o) ->\n    if (results.values.any { it.status == "failed" }) return@map Step(topic, "abort")\n')],
        "wrong-gap-as-supported": [('o.status == "success" -> groups.getValue("Well-supported") += topic', 'o.status == "success" || o.status == "failed" -> groups.getValue("Well-supported") += topic')],
        "wrong-missing-topic-skipped": [('o == null -> groups.getValue("Gaps") += "$topic (not searched)"', "o == null -> Unit")],
    }),
}


# ===== Level 3: module 67 =====
PLANTS[f"{X}/67-exploring-a-large-codebase/unit-01/practice-1"] = {
    "python": ("recovery.py", {
        "wrong-duplicate-findings": [('    if any(f["area"] == area and f["fact"] == fact for f in findings):\n        return list(findings)\n', "")],
        "wrong-ungrouped-scratchpad": [('        if f["area"] not in areas:\n            areas.append(f["area"])', '        areas.append(f["area"])')],
        "wrong-manifest-unsorted": [('for a in sorted(agents, key=lambda a: a["name"])', "for a in agents")],
        "wrong-manifest-unvalidated": [("        if a[\"status\"] not in STATUSES:\n            raise ValueError(f\"unknown status {a['status']}\")\n", "")],
        "wrong-rerun-done": [('        elif a["status"] == "done":\n            action = "reuse"', '        elif False:\n            action = "reuse"')],
        "wrong-restart-running": [('        else:\n            action = "resume"', '        else:\n            action = "restart"')],
        "wrong-ignore-missing-file": [('        if a["state_file"] not in existing_files:\n            action = "restart"\n        elif', '        if False:\n            action = "restart"\n        elif')],
        "wrong-no-continue-line": [(r'"\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('"/compact" if not keep else "/compact Focus on " + ", ".join(keep)', '"/compact"')],
    }),
    "typescript": ("recovery.ts", {
        "wrong-duplicate-findings": [("  if (findings.some((f) => f.area === area && f.fact === fact)) return [...findings];\n", "")],
        "wrong-ungrouped-scratchpad": [("if (!areas.includes(f.area)) areas.push(f.area);", "areas.push(f.area);")],
        "wrong-manifest-unsorted": [("const sorted = [...agents].sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));", "const sorted = [...agents];")],
        "wrong-manifest-unvalidated": [("  for (const a of agents) if (!STATUSES.includes(a.status)) throw new Error(`unknown status ${a.status}`);\n", "")],
        "wrong-rerun-done": [('else if (a.status === "done") action = "reuse";', 'else if (false) action = "reuse";')],
        "wrong-restart-running": [('else action = "resume";', 'else action = "restart";')],
        "wrong-ignore-missing-file": [('if (!existingFiles.has(a.state_file)) action = "restart";', 'if (false) action = "restart";')],
        "wrong-no-continue-line": [('"\\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('keep.length === 0 ? "/compact" : "/compact Focus on " + keep.join(", ")', '"/compact"')],
    }),
    "java": ("Recovery.java", {
        "wrong-duplicate-findings": [("        for (Finding f : findings) if (f.area().equals(area) && f.fact().equals(fact)) return out;\n", "")],
        "wrong-ungrouped-scratchpad": [("if (!areas.contains(f.area())) areas.add(f.area());", "areas.add(f.area());")],
        "wrong-manifest-unsorted": [("        sorted.sort(Comparator.comparing(AgentEntry::name));\n", "")],
        "wrong-manifest-unvalidated": [('        for (AgentEntry a : agents) if (!STATUSES.contains(a.status())) throw new IllegalArgumentException("unknown status " + a.status());\n', "")],
        "wrong-rerun-done": [('else if (a.status().equals("done")) action = "reuse";', 'else if (false) action = "reuse";')],
        "wrong-restart-running": [('else action = "resume";', 'else action = "restart";')],
        "wrong-ignore-missing-file": [('if (!existingFiles.contains(a.stateFile())) action = "restart";', 'if (false) action = "restart";')],
        "wrong-no-continue-line": [('"\\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('return keep.isEmpty() ? "/compact" : "/compact Focus on " + String.join(", ", keep);', 'return "/compact";')],
    }),
    "kotlin": ("Recovery.kt", {
        "wrong-duplicate-findings": [("if (findings.any { it.area == area && it.fact == fact }) findings.toList() else findings + Finding(area, fact, location)", "findings + Finding(area, fact, location)")],
        "wrong-ungrouped-scratchpad": [("findings.map { it.area }.distinct().joinToString", "findings.map { it.area }.joinToString")],
        "wrong-manifest-unsorted": [("agents.sortedBy { it.name }", "agents")],
        "wrong-manifest-unvalidated": [('    for (a in agents) require(a.status in STATUSES) { "unknown status ${a.status}" }\n', "")],
        "wrong-rerun-done": [('a.status == "done" -> "reuse"', 'false -> "reuse"')],
        "wrong-restart-running": [('else -> "resume"', 'else -> "restart"')],
        "wrong-ignore-missing-file": [('a.stateFile !in existingFiles -> "restart"', 'false -> "restart"')],
        "wrong-no-continue-line": [('"\\nContinue from the first unfinished step."', '""')],
        "wrong-compact-without-focus": [('if (keep.isEmpty()) "/compact" else "/compact Focus on " + keep.joinToString(", ")', '"/compact"')],
    }),
}


# ===== Level 3: module 68 =====
PLANTS[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"] = {
    "python": ("review_routing.py", {
        "wrong-overall-only": [("    for name in sorted(groups):", "    for name in []:")],
        "wrong-ignores-undersampled": [('        if s["total"] < min_n:\n            undersampled.append(s["segment"])\n        elif s["percent"] < threshold:', '        if s["percent"] < threshold:')],
        "wrong-highest-confidence": [("for t in sorted({c for c, _ in labeled}):", "for t in sorted({c for c, _ in labeled}, reverse=True):")],
        "wrong-strict-target": [("if 100 * sum(1 for ok in kept if ok) >= target * len(kept):", "if 100 * sum(1 for ok in kept if ok) > target * len(kept):")],
        "wrong-first-n-sample": [('key=lambda i: (i["rank"], i["id"])', 'key=lambda i: i["id"]')],
        "wrong-conflict-auto": [('e["conflict"] or e["confidence"] < threshold]', 'e["confidence"] < threshold]')],
        "wrong-id-order": [('key=lambda e: (0 if e["conflict"] else e["confidence"], e["id"])', 'key=lambda e: e["id"]')],
        "wrong-ignores-capacity": [('"review": queue[:capacity], "backlog": queue[capacity:]', '"review": queue, "backlog": []')],
        "wrong-irreversible-by-amount": [('"human" if action in IRREVERSIBLE or amount > limit else "auto"', '"human" if amount > limit else "auto"')],
    }),
    "typescript": ("reviewRouting.ts", {
        "wrong-overall-only": [("for (const name of [...groups.keys()].sort()) {", "for (const name of [] as string[]) {")],
        "wrong-ignores-undersampled": [("    if (s.total < minN) undersampled.push(s.segment);\n    else if (s.percent < threshold) failing.push(s.segment);", "    if (s.percent < threshold) failing.push(s.segment);")],
        "wrong-highest-confidence": [("].sort((a, b) => a - b)) {", "].sort((a, b) => b - a)) {")],
        "wrong-strict-target": [("if (100 * right >= target * kept.length)", "if (100 * right > target * kept.length)")],
        "wrong-first-n-sample": [(".sort((a, b) => a.rank - b.rank || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));\n    chosen.push", ".sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));\n    chosen.push")],
        "wrong-conflict-auto": [("filter((e) => e.conflict || e.confidence < threshold)", "filter((e) => e.confidence < threshold)")],
        "wrong-id-order": [(".sort((a, b) => priority(a) - priority(b) || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));", ".sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));")],
        "wrong-ignores-capacity": [("review: queue.slice(0, capacity), backlog: queue.slice(capacity),", "review: queue, backlog: [],")],
        "wrong-irreversible-by-amount": [("return IRREVERSIBLE.includes(action) || amount > limit ?", "return amount > limit ?")],
    }),
    "java": ("ReviewRouting.java", {
        "wrong-overall-only": [("for (Map.Entry<String, int[]> e : groups.entrySet()) out.add(", "for (Map.Entry<String, int[]> e : new TreeMap<String, int[]>().entrySet()) out.add(")],
        "wrong-ignores-undersampled": [("            if (s.total() < minN) undersampled.add(s.segment());\n            else if (s.percent() < threshold) failing.add(s.segment());", "            if (s.percent() < threshold) failing.add(s.segment());")],
        "wrong-highest-confidence": [("Set<Integer> levels = new java.util.TreeSet<>();", "Set<Integer> levels = new java.util.TreeSet<>(java.util.Comparator.reverseOrder());")],
        "wrong-strict-target": [("if (100 * right >= target * kept) return t;", "if (100 * right > target * kept) return t;")],
        "wrong-first-n-sample": [("members.sort(Comparator.comparingInt(Item::rank).thenComparing(Item::id));", "members.sort(Comparator.comparing(Item::id));")],
        "wrong-conflict-auto": [("if (e.conflict() || e.confidence() < threshold) candidates.add(e);", "if (e.confidence() < threshold) candidates.add(e);")],
        "wrong-id-order": [("candidates.sort(Comparator.<Extraction>comparingInt(e -> e.conflict() ? 0 : e.confidence()).thenComparing(Extraction::id));", "candidates.sort(Comparator.comparing(Extraction::id));")],
        "wrong-ignores-capacity": [("int cut = Math.min(capacity, queue.size());", "int cut = queue.size();")],
        "wrong-irreversible-by-amount": [("return IRREVERSIBLE.contains(action) || amount > limit ?", "return amount > limit ?")],
    }),
    "kotlin": ("ReviewRouting.kt", {
        "wrong-overall-only": [("        groups.map { (name, rs) ->", "        groups.filter { false }.map { (name, rs) ->")],
        "wrong-ignores-undersampled": [("val undersampled = segments.filter { it.total < minN }.map { it.segment }", "val undersampled = emptyList<String>()")],
        "wrong-highest-confidence": [(".toSortedSet()) {", ".toSortedSet(compareByDescending { it })) {")],
        "wrong-strict-target": [("if (100 * kept.count { it.correct } >= target * kept.size)", "if (100 * kept.count { it.correct } > target * kept.size)")],
        "wrong-first-n-sample": [(".sortedWith(compareBy({ it.rank }, { it.id }))", ".sortedBy { it.id }")],
        "wrong-conflict-auto": [("filter { it.conflict || it.confidence < threshold }", "filter { it.confidence < threshold }")],
        "wrong-id-order": [(".sortedWith(compareBy({ if (it.conflict) 0 else it.confidence }, { it.id }))", ".sortedBy { it.id }")],
        "wrong-ignores-capacity": [("Routing(queue.take(capacity), queue.drop(capacity),", "Routing(queue, emptyList(),")],
        "wrong-irreversible-by-amount": [('= if (action in IRREVERSIBLE || amount > limit) "human"', '= if (amount > limit) "human"')],
    }),
}


# ===== Survey practice: the tiny agent loop (no module; not part of any batch gate) =====
PLANTS[f"{X}/agent-loop"] = {
    "python": ("agent.py", {
        "wrong-ignores-stop-reason": [('        if resp["stop_reason"] != "tool_use":', '        if _text(resp["content"]):')],
        "wrong-one-turn-per-result": [('        messages.append({"role": "user", "content": results})',
                                       '        messages.extend({"role": "user", "content": [r]} for r in results)')],
    }),
    "typescript": ("agent.ts", {
        "wrong-ignores-stop-reason": [('    if (resp.stop_reason !== "tool_use") return textOf(resp.content);', "    if (textOf(resp.content)) return textOf(resp.content);")],
        "wrong-one-turn-per-result": [('    messages.push({ role: "user", content: results });',
                                       '    for (const r of results) messages.push({ role: "user", content: [r] });')],
    }),
    "java": ("Agent.java", {
        "wrong-ignores-stop-reason": [('            if (!"tool_use".equals(resp.get("stop_reason"))) return text(resp.get("content"));',
                                       '            if (!text(resp.get("content")).isEmpty()) return text(resp.get("content"));')],
        "wrong-one-turn-per-result": [('            messages.add(Map.of("role", "user", "content", results));',
                                       '            for (var r : results) messages.add(Map.of("role", "user", "content", List.of(r)));')],
    }),
    "kotlin": ("Agent.kt", {
        "wrong-ignores-stop-reason": [('        if (resp["stop_reason"] != "tool_use") return textOf(resp["content"])',
                                       '        if (textOf(resp["content"]).isNotEmpty()) return textOf(resp["content"])')],
        "wrong-one-turn-per-result": [('        messages += mapOf("role" to "user", "content" to results)',
                                       '        for (r in results) messages += mapOf("role" to "user", "content" to listOf(r))')],
    }),
}


def selected(modules):
    rx = re.compile(modules)
    return {p: v for p, v in PLANTS.items() if rx.match(p.split("/")[1])}


def apply(practice, lang, name, edits, fname, ref):
    """Text of every file the plant changes: {file: new text}; exits on a missing or ambiguous pattern."""
    by_file = edits if isinstance(edits, dict) else {fname: edits}
    changed = {}
    for target, pairs in by_file.items():
        original = (ref / target).read_text()
        text = original
        for n, (old, new) in enumerate(pairs, 1):
            hits = text.count(old)
            if hits != 1:
                sys.exit(f"{practice}/{lang}/{name}: replacement {n} of {target}: pattern found {hits} times (need exactly 1): {old!r}")
            text = text.replace(old, new)
        if text == original:
            sys.exit(f"{practice}/{lang}/{name}: plant equals the reference in {target}")
        changed[target] = text
    return changed


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modules", default=".*")
    ap.add_argument("--out")
    ap.add_argument("--list", action="store_true")
    args = ap.parse_args()
    plants = selected(args.modules)
    if not plants:
        sys.exit(f"no practice matches {args.modules!r}")
    made = 0
    for practice, langs in plants.items():
        cases_file = ROOT / practice / "cases.json"
        for lang, (fname, specs) in langs.items():
            if cases_file.exists():
                named = sorted(json.loads(cases_file.read_text())["plants"])
                if sorted(specs) != named:
                    sys.exit(f"{practice}/{lang}: plants {sorted(specs)} differ from cases.json {named}")
            ref = ROOT / practice / lang / "reference"
            for name, edits in specs.items():
                if args.list:
                    print(practice, lang, name)
                    continue
                changed = apply(practice, lang, name, edits, fname, ref)
                base = (Path(args.out) / practice if args.out else ROOT / practice) / lang
                dest = base / name
                if dest.exists():
                    shutil.rmtree(dest)
                shutil.copytree(ref, dest)
                for target, text in changed.items():
                    (dest / target).write_text(text)
                made += 1
    if not args.list:
        print(f"made {made} planted wrong solutions")


if __name__ == "__main__":
    main()
