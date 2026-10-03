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
    "wrong-literal-token": {".mcp.json": [('"Authorization": "Bearer ${GITHUB_TOKEN}"', '"Authorization": "Bearer ghp_0123456789abcdefghij"')]},
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
