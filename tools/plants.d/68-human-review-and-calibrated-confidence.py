# Planted wrong solutions of module 68-human-review-and-calibrated-confidence: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"] = {
    "python": ("review_routing.py", {
        "wrong-overall-percent": [('"segment": name, "correct": c, "total": t, "percent": _percent(c, t)}', '"segment": name, "correct": c, "total": t, "percent": _percent(correct, total)}')],
        "wrong-ignores-undersampled": [('        if s["total"] < min_n:\n            undersampled.append(s["segment"])\n        elif s["percent"] < threshold:', '        if s["percent"] < threshold:')],
        "wrong-highest-confidence": [("for t in sorted({c for c, _ in labeled}):", "for t in sorted({c for c, _ in labeled}, reverse=True):")],
        "wrong-strict-target": [("if 100 * sum(1 for ok in kept if ok) >= target * len(kept):", "if 100 * sum(1 for ok in kept if ok) > target * len(kept):")],
        "wrong-first-n-sample": [('key=lambda i: (i["rank"], i["id"])', 'key=lambda i: i["id"]')],
        "wrong-conflict-auto": [('e["conflict"] or e["confidence"] < threshold]', 'e["confidence"] < threshold]')],
        "wrong-id-order": [('key=lambda e: (0 if e["conflict"] else e["confidence"], e["id"])', 'key=lambda e: e["id"]')],
        "wrong-ignores-capacity": [('"review": queue[:capacity], "backlog": queue[capacity:]', '"review": queue, "backlog": []')],
        "wrong-irreversible-by-amount": [('"human" if action in IRREVERSIBLE or amount > limit else "auto"', '"human" if amount > limit else "auto"')],
        "wrong-percent-floors": [('return (200 * correct + total) // (2 * total)', 'return (100 * correct) // total')],
        "wrong-fallback-threshold": [('            return t\n    return None', '            return t\n    return max((c for c, _ in labeled), default=None)')],
    }),
    "typescript": ("reviewRouting.ts", {
        "wrong-overall-percent": [("segment: name, correct: c, total: t, percent: percent(c, t) }", "segment: name, correct: c, total: t, percent: percent(correct, total) }")],
        "wrong-ignores-undersampled": [("    if (s.total < minN) undersampled.push(s.segment);\n    else if (s.percent < threshold) failing.push(s.segment);", "    if (s.percent < threshold) failing.push(s.segment);")],
        "wrong-highest-confidence": [("].sort((a, b) => a - b)) {", "].sort((a, b) => b - a)) {")],
        "wrong-strict-target": [("if (100 * right >= target * kept.length)", "if (100 * right > target * kept.length)")],
        "wrong-first-n-sample": [(".sort((a, b) => a.rank - b.rank || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));\n    chosen.push", ".sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));\n    chosen.push")],
        "wrong-conflict-auto": [("filter((e) => e.conflict || e.confidence < threshold)", "filter((e) => e.confidence < threshold)")],
        "wrong-id-order": [(".sort((a, b) => priority(a) - priority(b) || (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));", ".sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));")],
        "wrong-ignores-capacity": [("review: queue.slice(0, capacity), backlog: queue.slice(capacity),", "review: queue, backlog: [],")],
        "wrong-irreversible-by-amount": [("return IRREVERSIBLE.includes(action) || amount > limit ?", "return amount > limit ?")],
        "wrong-percent-floors": [('Math.floor((200 * correct + total) / (2 * total))', 'Math.floor((100 * correct) / total)')],
        "wrong-fallback-threshold": [('if (100 * right >= target * kept.length) return t;\n  }\n  return null;', 'if (100 * right >= target * kept.length) return t;\n  }\n  return labeled.length ? Math.max(...labeled.map(([c]) => c)) : null;')],
    }),
    "java": ("ReviewRouting.java", {
        "wrong-overall-percent": [("e.getValue()[1], percent(e.getValue()[0], e.getValue()[1])));", "e.getValue()[1], percent(correct, total)));")],
        "wrong-ignores-undersampled": [("            if (s.total() < minN) undersampled.add(s.segment());\n            else if (s.percent() < threshold) failing.add(s.segment());", "            if (s.percent() < threshold) failing.add(s.segment());")],
        "wrong-highest-confidence": [("Set<Integer> levels = new java.util.TreeSet<>();", "Set<Integer> levels = new java.util.TreeSet<>(java.util.Comparator.reverseOrder());")],
        "wrong-strict-target": [("if (100 * right >= target * kept) return t;", "if (100 * right > target * kept) return t;")],
        "wrong-first-n-sample": [("members.sort(Comparator.comparingInt(Item::rank).thenComparing(Item::id));", "members.sort(Comparator.comparing(Item::id));")],
        "wrong-conflict-auto": [("if (e.conflict() || e.confidence() < threshold) candidates.add(e);", "if (e.confidence() < threshold) candidates.add(e);")],
        "wrong-id-order": [("candidates.sort(Comparator.<Extraction>comparingInt(e -> e.conflict() ? 0 : e.confidence()).thenComparing(Extraction::id));", "candidates.sort(Comparator.comparing(Extraction::id));")],
        "wrong-ignores-capacity": [("int cut = Math.min(capacity, queue.size());", "int cut = queue.size();")],
        "wrong-irreversible-by-amount": [("return IRREVERSIBLE.contains(action) || amount > limit ?", "return amount > limit ?")],
        "wrong-percent-floors": [('return (200 * correct + total) / (2 * total);', 'return (100 * correct) / total;')],
        "wrong-fallback-threshold": [('if (100 * right >= target * kept) return t;\n        }\n        return null;', 'if (100 * right >= target * kept) return t;\n        }\n        return levels.isEmpty() ? null : java.util.Collections.max(levels);')],
    }),
    "kotlin": ("ReviewRouting.kt", {
        "wrong-overall-percent": [("rs.size, percent(rs.count { it.correct }, rs.size)) }", "rs.size, percent(correct, total)) }")],
        "wrong-ignores-undersampled": [("val undersampled = segments.filter { it.total < minN }.map { it.segment }", "val undersampled = emptyList<String>()")],
        "wrong-highest-confidence": [(".toSortedSet()) {", ".toSortedSet(compareByDescending { it })) {")],
        "wrong-strict-target": [("if (100 * kept.count { it.correct } >= target * kept.size)", "if (100 * kept.count { it.correct } > target * kept.size)")],
        "wrong-first-n-sample": [(".sortedWith(compareBy({ it.rank }, { it.id }))", ".sortedBy { it.id }")],
        "wrong-conflict-auto": [("filter { it.conflict || it.confidence < threshold }", "filter { it.confidence < threshold }")],
        "wrong-id-order": [(".sortedWith(compareBy({ if (it.conflict) 0 else it.confidence }, { it.id }))", ".sortedBy { it.id }")],
        "wrong-ignores-capacity": [("Routing(queue.take(capacity), queue.drop(capacity),", "Routing(queue, emptyList(),")],
        "wrong-irreversible-by-amount": [('= if (action in IRREVERSIBLE || amount > limit) "human"', '= if (amount > limit) "human"')],
        "wrong-percent-floors": [('(200 * correct + total) / (2 * total)', '(100 * correct) / total')],
        "wrong-fallback-threshold": [('if (100 * kept.count { it.correct } >= target * kept.size) return t\n    }\n    return null', 'if (100 * kept.count { it.correct } >= target * kept.size) return t\n    }\n    return labeled.maxOfOrNull { it.confidence }')],
    }),
}

# boundary plants (a segment with exactly min_n records, a segment exactly at the threshold, a confidence exactly at the threshold)

_p = PLANTS[f"{X}/68-human-review-and-calibrated-confidence/unit-01/practice-1"]

_p["python"][1]["wrong-minn-boundary"] = [('        elif s["percent"] < threshold:', '        elif s["total"] > min_n and s["percent"] < threshold:')]

_p["python"][1]["wrong-threshold-boundary"] = [('        elif s["percent"] < threshold:', '        elif s["percent"] <= threshold:')]

_p["python"][1]["wrong-route-boundary"] = [('e["conflict"] or e["confidence"] < threshold]', 'e["conflict"] or e["confidence"] <= threshold]')]

_p["typescript"][1]["wrong-minn-boundary"] = [("else if (s.percent < threshold) failing.push(s.segment);", "else if (s.total > minN && s.percent < threshold) failing.push(s.segment);")]

_p["typescript"][1]["wrong-threshold-boundary"] = [("else if (s.percent < threshold) failing.push(s.segment);", "else if (s.percent <= threshold) failing.push(s.segment);")]

_p["typescript"][1]["wrong-route-boundary"] = [("e.conflict || e.confidence < threshold", "e.conflict || e.confidence <= threshold")]

_p["java"][1]["wrong-minn-boundary"] = [("else if (s.percent() < threshold) failing.add(s.segment());", "else if (s.total() > minN && s.percent() < threshold) failing.add(s.segment());")]

_p["java"][1]["wrong-threshold-boundary"] = [("else if (s.percent() < threshold) failing.add(s.segment());", "else if (s.percent() <= threshold) failing.add(s.segment());")]

_p["java"][1]["wrong-route-boundary"] = [("e.conflict() || e.confidence() < threshold", "e.conflict() || e.confidence() <= threshold")]

_p["kotlin"][1]["wrong-minn-boundary"] = [("it.total >= minN && it.percent < threshold", "it.total > minN && it.percent < threshold")]

_p["kotlin"][1]["wrong-threshold-boundary"] = [("it.total >= minN && it.percent < threshold", "it.total >= minN && it.percent <= threshold")]

_p["kotlin"][1]["wrong-route-boundary"] = [("it.conflict || it.confidence < threshold", "it.conflict || it.confidence <= threshold")]
