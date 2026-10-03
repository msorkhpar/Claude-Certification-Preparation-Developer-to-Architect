#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 2 practices from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/make_plants_l2.py
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> language -> (main file, {plant: [(old, new), ...]})
PLANTS = {
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
}


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
                f = dest / fname
                text = f.read_text()
                for old, new in edits:
                    if old not in text:
                        sys.exit(f"{practice}/{lang}/{name}: pattern not found: {old!r}")
                    text = text.replace(old, new, 1)
                if text == (ref / fname).read_text():
                    sys.exit(f"{practice}/{lang}/{name}: plant equals the reference")
                f.write_text(text)
                made += 1
    print(f"made {made} planted wrong solutions")


if __name__ == "__main__":
    main()
