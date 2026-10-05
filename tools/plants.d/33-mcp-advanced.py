# Planted wrong solutions of module 33-mcp-advanced: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

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
