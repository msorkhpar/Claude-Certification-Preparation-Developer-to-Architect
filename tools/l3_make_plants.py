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
