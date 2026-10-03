#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 2 practices of modules 24 to 29 from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/make_plants_l2c.py
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
