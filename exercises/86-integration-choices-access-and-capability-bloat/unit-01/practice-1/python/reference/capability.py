"""Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)

RISKY = {"money", "destroy"}


def _remove(agent):
    return [t for t in agent["holds"] if t not in agent["needs"]]


def _risky(remove, catalog):
    return [t for t in remove if catalog[t]["access"] in RISKY]


def _dormant(agent):
    return [t for t in agent["holds"] if t in agent["needs"] and agent["used"].get(t, 0) == 0]


def audit(agent, catalog):
    log.debug("audit input: %r", agent)
    remove = _remove(agent)
    return {
        "remove": remove,
        "risky": _risky(remove, catalog),
        "missing": [t for t in agent["needs"] if t not in agent["holds"]],
        "dormant": _dormant(agent),
    }


def _clamp(keep):
    return max(3, min(5, keep))


def _defers(tools):
    return len(tools) >= 10 or sum(tools.values()) > 10000


def _ranked(tools, usage, keep):
    return sorted(tools, key=lambda t: (-usage.get(t, 0), t))[:keep]


def plan_loading(tools, usage, keep=4, search_tokens=350):
    log.debug("plan_loading input: %r", tools)
    if not _defers(tools):
        return {"search": False, "load_now": list(tools), "deferred": [], "tokens": sum(tools.values())}
    ranked = _ranked(tools, usage, _clamp(keep))
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


def _verdict(team, request, policy):
    if team is None:
        return "deny", "unauthenticated"
    if request["model"] not in policy["models"][team]:
        return "deny", "model not allowed"
    if request["tool"] is not None and request["tool"] not in policy["tools"][team]:
        return "deny", "tool not allowed"
    if request["recent"] >= policy["limits"][team]:
        return "deny", "rate limited"
    return "allow", f"routed to {policy['routes'].get(request['model'], request['model'])}"


def _record(team, request, decision):
    return {"team": team or "unknown", "model": request["model"], "tool": request["tool"] or "none", "decision": decision}


def gateway(request, policy):
    log.debug("gateway input: %r", request)
    team = policy["credentials"].get(request["credential"])
    decision, reason = _verdict(team, request, policy)
    return {"decision": decision, "reason": reason, "audit": _record(team, request, decision)}
