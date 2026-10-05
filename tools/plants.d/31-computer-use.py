# Planted wrong solutions of module 31-computer-use: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

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
