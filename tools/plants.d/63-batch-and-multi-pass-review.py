# Planted wrong solutions of module 63-batch-and-multi-pass-review: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

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

# boundary plants (a case at exactly the limit)

_p = PLANTS[f"{X}/63-batch-and-multi-pass-review/unit-01/practice-1"]

_p["python"][1]["wrong-chunk-at-limit"] = [("if sizes.get(custom_id, 0) > limit:", "if sizes.get(custom_id, 0) >= limit:")]

_p["python"][1]["wrong-interval-boundary"] = [("    if interval <= 0:", "    if interval <= 1:")]

_p["typescript"][1]["wrong-chunk-at-limit"] = [('if ((sizes[customId] ?? 0) > limit) action = "chunk";', 'if ((sizes[customId] ?? 0) >= limit) action = "chunk";')]

_p["typescript"][1]["wrong-interval-boundary"] = [("if (interval <= 0) throw", "if (interval <= 1) throw")]

_p["java"][1]["wrong-chunk-at-limit"] = [('if (sizes.getOrDefault(r.customId(), 0) > limit) action = "chunk";', 'if (sizes.getOrDefault(r.customId(), 0) >= limit) action = "chunk";')]

_p["java"][1]["wrong-interval-boundary"] = [("if (interval <= 0) throw new IllegalArgumentException", "if (interval <= 1) throw new IllegalArgumentException")]

_p["kotlin"][1]["wrong-chunk-at-limit"] = [("(sizes[r.customId] ?: 0) > limit", "(sizes[r.customId] ?: 0) >= limit")]

_p["kotlin"][1]["wrong-interval-boundary"] = [("require(interval > 0)", "require(interval > 1)")]
