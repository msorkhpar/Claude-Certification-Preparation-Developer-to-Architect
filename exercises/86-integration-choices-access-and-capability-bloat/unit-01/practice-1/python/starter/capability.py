"""Capability design: what a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)

RISKY = {"money", "destroy"}


def _remove(agent):
    """TODO 1 of 10 (unlocks m1): the tools to take away.

    Receives the agent (`holds`, `needs`, `used`). Returns the held tools that the role does not need, in the order they are held.
    Example: holds ["read", "refund"], needs ["read"] -> ["refund"]
    """
    return []


def _risky(remove, catalog):
    """TODO 2 of 10 (unlocks m1): the risky tools among those to remove.

    Receives the tools to remove and the catalog (tool to {"access", "tokens"}). Returns those whose access class is in RISKY, in the same order.
    Example: remove ["refund", "export"], refund has access "money" and export has "read" -> ["refund"]
    """
    return []


def _dormant(agent):
    """TODO 3 of 10 (unlocks e1): the tools that are kept but never used.

    Receives the agent. Returns the held tools that the role needs and that have no calls in `used` (a tool missing from `used` has none), in the order held. They are
    reported and never removed.
    Example: holds ["a", "b"], needs ["a", "b"], used {"a": 5, "b": 0} -> ["b"]
    """
    return []


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
    """TODO 4 of 10 (unlocks e3): how many tools to keep loaded.

    Receives the number asked for. Returns it limited to the range from 3 to 5.
    Example: _clamp(1) -> 3, _clamp(8) -> 5, _clamp(4) -> 4
    """
    return keep


def _defers(tools):
    """TODO 5 of 10 (unlocks e2): must the definitions be deferred?

    Receives the tools (name to the tokens of its definition). Returns True when there are 10 tools or more, or the definitions together are over 10000 tokens (10000 itself is fine).
    Example: 9 tools of 100 tokens -> False; 10 tools -> True; two tools of 5000 and 5001 -> True
    """
    return False


def _ranked(tools, usage, keep):
    """TODO 6 of 10 (unlocks e3): the tools to load first.

    Receives the tools, the usage counts (a tool missing from `usage` has 0) and how many to keep. Returns that many tool names, the most used first and by name among equals.
    Example: usage {"b": 9, "a": 9, "c": 1}, keep 2 -> ["a", "b"]
    """
    return list(tools)[:keep]


def plan_loading(tools, usage, keep=4, search_tokens=350):
    log.debug("plan_loading input: %r", tools)
    if not _defers(tools):
        return {"search": False, "load_now": list(tools), "deferred": [], "tokens": sum(tools.values())}
    ranked = _ranked(tools, usage, _clamp(keep))
    return {"search": True, "load_now": ranked, "deferred": [t for t in tools if t not in ranked], "tokens": sum(tools[t] for t in ranked) + search_tokens}


def choose_mechanism(consumers, counterpart, path):
    """TODO 7 of 10 (unlocks e4): how a capability is connected.

    Receives the number of clients that will use it, the counterpart ("agent" or "tool") and the path ("fixed" or "model-chosen"). Decide in this order:
    a counterpart that is an agent gives "agent-to-agent"; a fixed path gives "direct call in code"; otherwise "MCP server" for more than one client and "custom tool" for one.
    Example: choose_mechanism(4, "tool", "model-chosen") -> "MCP server"
    """
    return None


def authorize(tool, user_scopes, agent_scopes, required):
    """TODO 8 of 10 (unlocks e5): whose rights a tool call uses.

    Receives the tool name, the scopes of the user, the scopes of the agent and `required` (tool to the scope it needs). Returns "deny: unknown tool" for a tool
    that is not in `required`; then "deny: user lacks <scope>"; then "deny: agent lacks <scope>"; otherwise "allow". Both the user and the agent must hold the scope.
    Example: scope "refunds:write" held by the agent only -> "deny: user lacks refunds:write"
    """
    return None


def _verdict(team, request, policy):
    """TODO 9 of 10 (unlocks e6): the decision and its reason.

    Receives the team (None when the credential is unknown), the request (`model`, `tool` or None, `recent`) and the policy (`models`, `tools` and `limits` per team,
    `routes`). Checks in this order and stops at the first that fails, returning ("deny", reason): "unauthenticated" when there is no team; "model not allowed";
    "tool not allowed" (only when a tool is named); "rate limited" when `recent` is at or over the team's limit. Otherwise ("allow", "routed to <name>"), where the name is
    `policy["routes"]` for the model, or the model itself when it has no route.
    Example: team "support", model "standard", no tool, recent 1, limit 2 -> ("allow", "routed to claude-sonnet-5-5")
    """
    return "allow", ""


def _record(team, request, decision):
    """TODO 10 of 10 (unlocks e7): the record kept of a decision.

    Receives the team (None when unknown), the request and the decision. Returns {"team", "model", "tool", "decision"}: the team or "unknown", the model, the tool or
    "none", and the decision. Nothing else: no prompt and no credential.
    Example: team None, model "deep", tool None, "deny" -> {"team": "unknown", "model": "deep", "tool": "none", "decision": "deny"}
    """
    return {}


def gateway(request, policy):
    log.debug("gateway input: %r", request)
    team = policy["credentials"].get(request["credential"])
    decision, reason = _verdict(team, request, policy)
    return {"decision": decision, "reason": reason, "audit": _record(team, request, decision)}
