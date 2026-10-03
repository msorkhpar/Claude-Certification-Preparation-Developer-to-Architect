#!/usr/bin/env python3
"""Derive the planted wrong solutions of the Level 2 practices of modules 30 to 35 from their reference solutions.

Each plant is the reference with exact replacements in one file; the script fails if a replacement does not
change the file (so a plant can never silently equal the reference) or if a pattern is missing.
usage: tools/make_plants_l2d.py
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
