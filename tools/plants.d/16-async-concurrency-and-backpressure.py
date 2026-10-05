# Planted wrong solutions of module 16-async-concurrency-and-backpressure: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/16-async-concurrency-and-backpressure/unit-01/practice-1"] = {
    "python": ("bounded.py", {
        "wrong-unbounded": [("range(limit)", "range(1000)")],
        "wrong-fail-fast": [("except Exception as err:  # noqa: BLE001 - one failure must not stop the others", "except KeyboardInterrupt as err:")],
        "wrong-completion-order": [("            results[index] = outcome", "            results.remove(None)\n            results.append(outcome)")],
        "wrong-eager-input": [("source = iter(items)", "source = iter(list(items))")],
    }),
    "typescript": ("bounded.ts", {
        "wrong-unbounded": [("Array.from({ length: limit }, worker)", "Array.from({ length: 1000 }, worker)")],
        "wrong-fail-fast": [("outcome = { ok: false, error }; // one failure must not stop the others", "throw error;")],
        "wrong-completion-order": [("      results[index] = outcome;", "      results.splice(results.indexOf(undefined as any), 1);\n      results.push(outcome);")],
        "wrong-eager-input": [("const source = items[Symbol.iterator]();", "const source = [...items][Symbol.iterator]();")],
    }),
    "java": ("Bounded.java", {
        "wrong-unbounded": [("for (int i = 0; i < limit; i++) {", "for (int i = 0; i < 64; i++) {")],
        "wrong-fail-fast": [("        return results;\n", "        for (Outcome<R> o : results) {\n            if (!o.ok()) throw new RuntimeException(o.error());\n        }\n        return results;\n")],
        "wrong-completion-order": [("                        results.set(index, outcome);", "                        results.remove(null);\n                        results.add(outcome);")],
        "wrong-eager-input": [("mapBounded(Iterator<T> items,", "mapBounded(Iterator<T> source,"),
                              ("        List<Outcome<R>> results = new ArrayList<>(); // one slot",
                               "        List<T> all = new ArrayList<>();\n        source.forEachRemaining(all::add);\n        Iterator<T> items = all.iterator();\n        List<Outcome<R>> results = new ArrayList<>(); // one slot")],
    }),
    "kotlin": ("Bounded.kt", {
        "wrong-unbounded": [("val workers = List(limit) {", "val workers = List(64) {")],
        "wrong-fail-fast": [("    return results.map { it!! }", "    results.firstOrNull { !it!!.ok }?.let { throw RuntimeException(it.error) }\n    return results.map { it!! }")],
        "wrong-completion-order": [("synchronized(lock) { results[index] = outcome }", "synchronized(lock) { results.remove(null); results.add(outcome) }")],
        "wrong-eager-input": [("fun <T, R> mapBounded(items: Iterator<T>,", "fun <T, R> mapBounded(source: Iterator<T>,"),
                              ('    require(limit >= 1) { "limit must be at least 1" }\n', '    require(limit >= 1) { "limit must be at least 1" }\n    val items = source.asSequence().toList().iterator()\n')],
    }),
}
