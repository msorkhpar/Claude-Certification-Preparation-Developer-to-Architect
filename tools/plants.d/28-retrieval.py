# Planted wrong solutions of module 28-retrieval: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/28-retrieval/unit-01/practice-1"] = {
    "python": ("retrieval.py", {
        "wrong-no-overlap": [("        start += size - overlap\n", "        start += size\n")],
        "wrong-no-idf": [("idf = math.log(1 + (n - df[term] + 0.5) / (df[term] + 0.5))", "idf = 1.0")],
        "wrong-fuse-by-votes": [("scores.get(cid, 0.0) + 1.0 / (k + rank)", "scores.get(cid, 0.0) + 1.0")],
        "wrong-rerank-ascending": [("key=lambda cid: -scorer(query, texts[cid])", "key=lambda cid: scorer(query, texts[cid])")],
        "wrong-pool-ignored": [("return rerank(query, ranked[:pool], ", "return rerank(query, ranked, ")],
        "wrong-recall-by-chunk": [("    found = {doc_of[cid] for cid in ids[:k]}\n    return len(found & set(relevant)) / len(set(relevant))\n", "    wanted = set(relevant)\n    return min(1.0, len([cid for cid in ids[:k] if doc_of[cid] in wanted]) / len(wanted))\n")],
        "wrong-context-ignored": [("if contexts and c[\"id\"] in contexts}", "if False and contexts and c[\"id\"] in contexts}")],
    }),
    "typescript": ("retrieval.ts", {
        "wrong-no-overlap": [("start += size - overlap)", "start += size)")],
        "wrong-no-idf": [("const idf = Math.log(1 + (n - df.get(term)! + 0.5) / (df.get(term)! + 0.5));", "const idf = 1;")],
        "wrong-fuse-by-votes": [("(scores.get(id) ?? 0) + 1 / (k + i + 1)", "(scores.get(id) ?? 0) + 1")],
        "wrong-rerank-ascending": [("b.s - a.s || a.i - b.i", "a.s - b.s || a.i - b.i")],
        "wrong-pool-ignored": [("return rerank(query, ranked.slice(0, pool), ", "return rerank(query, ranked, ")],
        "wrong-recall-by-chunk": [("  const found = new Set(ids.slice(0, k).map((id) => docOf[id]));\n  const wanted = new Set(relevant);\n  return [...wanted].filter((d) => found.has(d)).length / wanted.size;\n", "  const wanted = new Set(relevant);\n  return Math.min(1, ids.slice(0, k).filter((id) => wanted.has(docOf[id])).length / wanted.size);\n")],
        "wrong-context-ignored": [("if (contexts && c.id in contexts) indexText", "if (contexts && false && c.id in contexts) indexText")],
    }),
    "java": ("Retrieval.java", {
        "wrong-no-overlap": [("start += size - overlap)", "start += size)")],
        "wrong-no-idf": [("double idf = Math.log(1 + (n - df.get(term) + 0.5) / (df.get(term) + 0.5));", "double idf = 1.0;")],
        "wrong-fuse-by-votes": [("scores.merge(id, 1.0 / (k + rank++), Double::sum);", "scores.merge(id, 1.0, Double::sum);")],
        "wrong-rerank-ascending": [("-scorer.apply(query, texts.get(id))", "scorer.apply(query, texts.get(id))")],
        "wrong-pool-ignored": [("return rerank(query, ranked.subList(0, Math.min(pool, ranked.size())), plain, scorer, k);", "return rerank(query, ranked, plain, scorer, k);")],
        "wrong-recall-by-chunk": [("        Set<String> found = new LinkedHashSet<>();\n        for (String id : ids.subList(0, Math.min(k, ids.size()))) found.add(docOf.get(id));\n        Set<String> wanted = new LinkedHashSet<>(relevant);\n        int hits = 0;\n        for (String d : wanted) if (found.contains(d)) hits++;\n        return (double) hits / wanted.size();\n", "        Set<String> wanted = new LinkedHashSet<>(relevant);\n        int hits = 0;\n        for (String id : ids.subList(0, Math.min(k, ids.size()))) if (wanted.contains(docOf.get(id))) hits++;\n        return Math.min(1.0, (double) hits / wanted.size());\n")],
        "wrong-context-ignored": [("if (contexts != null && contexts.containsKey(c.id())) indexText.put(", "if (contexts != null && false && contexts.containsKey(c.id())) indexText.put(")],
    }),
    "kotlin": ("Retrieval.kt", {
        "wrong-no-overlap": [("        start += size - overlap\n", "        start += size\n")],
        "wrong-no-idf": [("val idf = ln(1 + (n - df.getValue(term) + 0.5) / (df.getValue(term) + 0.5))", "val idf = 1.0")],
        "wrong-fuse-by-votes": [("(scores[id] ?: 0.0) + 1.0 / (k + i + 1)", "(scores[id] ?: 0.0) + 1.0")],
        "wrong-rerank-ascending": [("ids.sortedByDescending { scorer(query, texts.getValue(it)) }", "ids.sortedBy { scorer(query, texts.getValue(it)) }")],
        "wrong-pool-ignored": [("return rerank(query, ranked.take(pool), ", "return rerank(query, ranked, ")],
        "wrong-recall-by-chunk": [("    val found = ids.take(k).map { docOf.getValue(it) }.toSet()\n    val wanted = relevant.toSet()\n    return wanted.count { it in found }.toDouble() / wanted.size\n", "    val wanted = relevant.toSet()\n    return minOf(1.0, ids.take(k).count { docOf.getValue(it) in wanted }.toDouble() / wanted.size)\n")],
        "wrong-context-ignored": [("chunks.filter { contexts != null && it.id in contexts }", "chunks.filter { contexts != null && it.id in contexts && contexts.isEmpty() }")],
    }),
}
