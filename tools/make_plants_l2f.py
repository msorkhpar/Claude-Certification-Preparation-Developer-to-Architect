#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 2 practices of modules 42 and 43 from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/make_plants_l2e.py
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
