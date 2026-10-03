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
