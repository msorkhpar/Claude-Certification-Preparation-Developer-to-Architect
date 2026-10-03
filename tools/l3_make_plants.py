#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 3 practices from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/l3_make_plants.py
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
