"""Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md."""
RISKY = {"money", "destroy"}


def audit(agent, catalog):
    holds, needs, used = agent["holds"], agent["needs"], agent["used"]
    remove = [t for t in holds if t not in needs]
    return {
        "remove": remove,
        "risky": [t for t in remove if catalog[t]["access"] in RISKY],
        "missing": [t for t in needs if t not in holds],
        "dormant": [t for t in holds if t in needs and used.get(t, 0) == 0],
    }


def plan_loading(tools, usage, keep=4, search_tokens=350):
    keep = max(3, min(5, keep))
    if len(tools) < 10 and sum(tools.values()) <= 10000:
        return {"search": False, "load_now": list(tools), "deferred": [], "tokens": sum(tools.values())}
    ranked = sorted(tools, key=lambda t: (-usage.get(t, 0), t))[:keep]
    return {"search": True, "load_now": ranked, "deferred": [t for t in tools if t not in ranked], "tokens": sum(tools[t] for t in ranked) + search_tokens}


def choose_mechanism(consumers, counterpart, path):
    if counterpart == "agent":
        return "agent-to-agent"
    if path == "fixed":
        return "direct call in code"
    return "MCP server" if consumers > 1 else "custom tool"


def authorize(tool, user_scopes, agent_scopes, required):
    if tool not in required:
        return "deny: unknown tool"
    scope = required[tool]
    if scope not in user_scopes:
        return f"deny: user lacks {scope}"
    if scope not in agent_scopes:
        return f"deny: agent lacks {scope}"
    return "allow"


def gateway(request, policy):
    team = policy["credentials"].get(request["credential"])
    if team is None:
        decision, reason = "deny", "unauthenticated"
    elif request["model"] not in policy["models"][team]:
        decision, reason = "deny", "model not allowed"
    elif request["tool"] is not None and request["tool"] not in policy["tools"][team]:
        decision, reason = "deny", "tool not allowed"
    elif request["recent"] >= policy["limits"][team]:
        decision, reason = "deny", "rate limited"
    else:
        decision, reason = "allow", f"routed to {policy['routes'].get(request['model'], request['model'])}"
    return {"decision": decision, "reason": reason, "audit": {"team": team or "unknown", "model": request["model"], "tool": request["tool"] or "none", "decision": decision}}
