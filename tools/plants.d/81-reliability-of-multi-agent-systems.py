# Planted wrong solutions of module 81-reliability-of-multi-agent-systems: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

PLANTS[f"{X}/81-reliability-of-multi-agent-systems/unit-01/practice-1"] = {
    "python": ("reliable_agents.py", {
        "wrong-key-changes-late": [("status, value = call_once(agents, name, key, inputs)", 'status, value = call_once(agents, name, key if n < 3 else key + ":retry", inputs)')],
        "wrong-one-more-attempt": [("for n in range(1, attempts + 1):", "for n in range(1, attempts + 1 + (attempts == 3)):")],
        "wrong-dependents-still-run": [("return next((n for n in needs if n not in done), None)", "return None"), ("inputs = {n: done[n] for n in needs}", 'inputs = {n: done.get(n, "") for n in needs}')],
        "wrong-breaker-total": [("consecutive[name] = 0 if ok else consecutive.get(name, 0) + 1", "consecutive[name] = consecutive.get(name, 0) + (0 if ok else 1)")],
        "wrong-breaker-never-opens": [("return consecutive.get(name, 0) >= threshold", "return False")],
        "wrong-fallback-same-key": [('return key + ":fallback"', "return key")],
        "wrong-fallback-checkpointed": [("    if not degraded:\n        store[tid] = result\n", "    store[tid] = result\n")],
        "wrong-resume-reruns": [("return tid in store", "return False")],
        "wrong-crash-swallowed": [('    except Fatal as error:\n        return "fatal", str(error)\n', '    except Fatal as error:\n        return "fatal", str(error)\n    except Exception as error:\n        return "fatal", str(error)\n')],
    }),
    "typescript": ("reliableAgents.ts", {
        "wrong-key-changes-late": [("const [status, value] = callOnce(agents, name, key, inputs);", 'const [status, value] = callOnce(agents, name, n < 3 ? key : key + ":retry", inputs);')],
        "wrong-one-more-attempt": [("for (let n = 1; n <= attempts; n++) {", "for (let n = 1; n <= attempts + (attempts === 3 ? 1 : 0); n++) {")],
        "wrong-dependents-still-run": [("return needs.find((n) => !(n in done));", "return undefined;"), ("[n, done[n]]", '[n, done[n] ?? ""]')],
        "wrong-breaker-total": [("consecutive[name] = ok ? 0 : (consecutive[name] ?? 0) + 1;", "consecutive[name] = (consecutive[name] ?? 0) + (ok ? 0 : 1);")],
        "wrong-breaker-never-opens": [("return (consecutive[name] ?? 0) >= threshold;", "return false;")],
        "wrong-fallback-same-key": [('return key + ":fallback";', "return key;")],
        "wrong-fallback-checkpointed": [("if (!degraded) store[tid] = result;", "store[tid] = result;")],
        "wrong-resume-reruns": [("return tid in store;", "return false;")],
        "wrong-crash-swallowed": [("    throw error;\n", '    return ["fatal", "crash"];\n')],
    }),
    "java": ("ReliableAgents.java", {
        "wrong-key-changes-late": [("Call call = callOnce(agents, name, key, inputs);", 'Call call = callOnce(agents, name, n < 3 ? key : key + ":retry", inputs);')],
        "wrong-one-more-attempt": [("for (int n = 1; n <= attempts; n++) {", "for (int n = 1; n <= attempts + (attempts == 3 ? 1 : 0); n++) {")],
        "wrong-dependents-still-run": [("        for (String n : needs) if (!done.containsKey(n)) return n;\n", ""), ("for (String n : needs) inputs.put(n, done.get(n));", 'for (String n : needs) inputs.put(n, done.getOrDefault(n, ""));')],
        "wrong-breaker-total": [("        if (ok) consecutive.put(name, 0);\n        else consecutive.merge(name, 1, Integer::sum);\n", "        consecutive.merge(name, ok ? 0 : 1, Integer::sum);\n")],
        "wrong-breaker-never-opens": [("return consecutive.getOrDefault(name, 0) >= threshold;", "return false;")],
        "wrong-fallback-same-key": [('return key + ":fallback";', "return key;")],
        "wrong-fallback-checkpointed": [("if (!degraded) store.put(tid, result);", "store.put(tid, result);")],
        "wrong-resume-reruns": [("return store.containsKey(tid);", "return false;")],
        "wrong-crash-swallowed": [('            return new Call("fatal", e.getMessage());\n        }\n', '            return new Call("fatal", e.getMessage());\n        } catch (RuntimeException e) {\n            return new Call("fatal", String.valueOf(e.getMessage()));\n        }\n')],
    }),
    "kotlin": ("ReliableAgents.kt", {
        "wrong-key-changes-late": [("val (status, value) = callOnce(agents, name, key, inputs)", 'val (status, value) = callOnce(agents, name, if (n < 3) key else key + ":retry", inputs)')],
        "wrong-one-more-attempt": [("for (n in 1..attempts) {", "for (n in 1..attempts + (if (attempts == 3) 1 else 0)) {")],
        "wrong-dependents-still-run": [("= needs.firstOrNull { it !in done }", "= null"), ("needs.associateWith { done.getValue(it) }", 'needs.associateWith { done[it] ?: "" }')],
        "wrong-breaker-total": [("consecutive[name] = if (ok) 0 else (consecutive[name] ?: 0) + 1", "consecutive[name] = (consecutive[name] ?: 0) + (if (ok) 0 else 1)")],
        "wrong-breaker-never-opens": [("= (consecutive[name] ?: 0) >= threshold", "= false")],
        "wrong-fallback-same-key": [('= key + ":fallback"', "= key")],
        "wrong-fallback-checkpointed": [("    if (!degraded) store[tid] = result\n", "    store[tid] = result\n")],
        "wrong-resume-reruns": [("= tid in store", "= false")],
        "wrong-crash-swallowed": [('        "fatal" to e.message\n    }\n', '        "fatal" to e.message\n    } catch (e: RuntimeException) {\n        "fatal" to e.message\n    }\n')],
    }),
}
