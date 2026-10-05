"""Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)

NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}  # models whose API rejects tool_choice any and tool, as read on 2026-10-03


def assign_tools(roles, catalog, budget=5):
    """Give each role the tools of its specialisation, plus what it is explicitly granted, and refuse a set that is too large."""
    by_name = {}
    for tool in catalog:
        if tool["name"] in by_name:
            raise ValueError(f"duplicate tool name: {tool['name']}")
        by_name[tool["name"]] = tool
    result = {}
    for role, spec in roles.items():
        tags = set(spec.get("specialisation") or [])
        if not tags:
            raise ValueError(f"role {role} has no specialisation")
        # TODO 1 of 8 (finish this to pass m1, e2): the tools of a role. Receives the catalog and the role's tags.
        #   Return the names, in catalog order, of the tools that share a tag with the role and are not irreversible (an
        #   irreversible tool is only given by an explicit grant). Example: tags {billing}, catalog [lookup(billing),
        #   refund(billing, irreversible)] -> [lookup].
        names = list(by_name)
        for extra in spec.get("extra") or []:
            tool = by_name.get(extra)
            if tool is None:
                raise ValueError(f"role {role}: unknown tool {extra}")
            if not (tags & set(tool.get("tags", [])) or tool.get("scoped") or tool.get("irreversible")):
                raise ValueError(f"role {role}: {extra} is outside the specialisation and is not a scoped cross-role tool")
            if extra not in names:
                names.append(extra)
        # TODO 2 of 8 (finish this to pass e1): the budget. When the role would hold more tools than the budget, refuse
        #   with an error that names the role, the count and the budget. Example: 6 tools, budget 5 -> raises; exactly 5
        #   is fine.
        result[role] = names
    return result


def plan_turn(model, need, tools, forced=None, manual_thinking=False):
    """The tool_choice and tool list of one request, with the portable fallback where forcing is not accepted."""
    if need not in ("free", "none", "any", "named"):
        raise ValueError(f"unknown need: {need}")
    if need == "named" and forced not in tools:
        raise ValueError("a named choice needs a tool from the list")
    rejects = model in NO_FORCING or manual_thinking
    if need == "free":
        return {"tool_choice": {"type": "auto"}, "tools": list(tools), "strict": False, "verify_call": False}
    if need == "none":
        return {"tool_choice": {"type": "none"}, "tools": list(tools), "strict": False, "verify_call": False}
    # TODO 3 of 8 (finish this to pass e3): the choice for `any` and `named`. Receives the model, the need, the tools,
    #   the forced name and `rejects`. When the model accepts forcing, return tool_choice {type: any} or {type: tool,
    #   name}, the tools, strict false, verify_call false. When it rejects forcing, return tool_choice auto, the tools
    #   (all for any, only the named one for named), strict true and verify_call true. Example: sonnet, named, forced
    #   lookup -> auto, [lookup], strict, verify.
    return {"tool_choice": {"type": "auto"}, "tools": list(tools), "strict": False, "verify_call": False}


def cache_impact(previous, new):
    """What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed)."""
    if previous is None:
        return "none"
    # TODO 4 of 8 (finish this to pass e4): the cost in prompt caching. Receives the previous request and the new one
    #   (the first request has no previous). Return all when the tools differ, otherwise messages when the tool_choice
    #   differs, otherwise none. Example: same tools, auto then {type: any} -> messages.
    return "none"


def check_turn(blocks, need, forced=None):
    """Compare a reply with the call that was required: ok, missed_call or wrong_tool."""
    if need not in ("any", "named"):
        return "ok"
    calls = [b for b in blocks if b.get("type") == "tool_use"]
    if not calls:
        return "missed_call"
    # TODO 5 of 8 (finish this to pass e5): the check of a named call. When the need is named and the first tool call is
    #   not the forced tool, return wrong_tool. Example: need named, forced lookup, first call refund -> wrong_tool.
    return "ok"


def authorize(call, policy, approvals):
    """Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says."""
    log.debug("authorize input: %r", call)
    # TODO 6 of 8 (finish this to pass e6): the policy lookup. Find the rule of the called tool in the policy; when the
    #   policy does not name the tool, return the answer (allowed false, code unknown_tool, a message that names the
    #   tool). Example: tool delete_all, policy without it -> unknown_tool.
    rule = policy.get("tools", {}).get(call.get("tool")) or {}
    cap = rule.get("cap")
    amount = call.get("amount")
    # TODO 7 of 8 (finish this to pass e6): the amount and the owner. When the tool has a cap and the amount is missing,
    #   a boolean, a decimal or not above zero, return bad_amount. When there is no verified customer or the call's
    #   customer is another, return not_owner. Example: amount 0 -> bad_amount; customer C2 while C1 is verified ->
    #   not_owner.
    # TODO 8 of 8 (finish this to pass e7): the cap and the approval. When the amount is above the cap, return over_cap
    #   with escalate true (an amount equal to the cap is allowed). When the tool is irreversible and the call's id is not
    #   among the approvals, return needs_approval with escalate true. Example: cap 500, amount 501 -> over_cap, even when
    #   approved.
    return _answer(True, "ok", "allowed")


def _answer(allowed, code, message, escalate=False):
    return {"allowed": allowed, "code": code, "message": message, "escalate": escalate}
