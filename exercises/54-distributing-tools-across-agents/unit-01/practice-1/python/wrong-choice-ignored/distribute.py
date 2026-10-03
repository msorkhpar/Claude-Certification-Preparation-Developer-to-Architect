"""Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md."""

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
        names = [t["name"] for t in catalog if tags & set(t.get("tags", [])) and not t.get("irreversible")]
        for extra in spec.get("extra") or []:
            tool = by_name.get(extra)
            if tool is None:
                raise ValueError(f"role {role}: unknown tool {extra}")
            if not (tags & set(tool.get("tags", [])) or tool.get("scoped") or tool.get("irreversible")):
                raise ValueError(f"role {role}: {extra} is outside the specialisation and is not a scoped cross-role tool")
            if extra not in names:
                names.append(extra)
        if len(names) > budget:
            raise ValueError(f"role {role} has {len(names)} tools, more than the budget of {budget}")
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
    if not rejects:
        choice = {"type": "any"} if need == "any" else {"type": "tool", "name": forced}
        return {"tool_choice": choice, "tools": list(tools), "strict": False, "verify_call": False}
    offered = list(tools) if need == "any" else [forced]
    return {"tool_choice": {"type": "auto"}, "tools": offered, "strict": True, "verify_call": True}


def cache_impact(previous, new):
    """What a change between two requests costs in prompt caching: none, messages (tool_choice changed) or all (the tools changed)."""
    if previous is None:
        return "none"
    if previous.get("tools") != new.get("tools"):
        return "all"
    return "none"


def check_turn(blocks, need, forced=None):
    """Compare a reply with the call that was required: ok, missed_call or wrong_tool."""
    if need not in ("any", "named"):
        return "ok"
    calls = [b for b in blocks if b.get("type") == "tool_use"]
    if not calls:
        return "missed_call"
    if need == "named" and calls[0].get("name") != forced:
        return "wrong_tool"
    return "ok"


def authorize(call, policy, approvals):
    """Decide a call to a tool that cannot be undone, in the tool layer, whatever the model says."""
    rule = policy.get("tools", {}).get(call.get("tool"))
    if rule is None:
        return _answer(False, "unknown_tool", f"{call.get('tool')} is not an allowed tool")
    cap = rule.get("cap")
    amount = call.get("amount")
    if cap is not None and (isinstance(amount, bool) or not isinstance(amount, int) or amount <= 0):
        return _answer(False, "bad_amount", "the amount must be a positive whole number")
    if call.get("verified_customer") is None or call.get("customer") != call.get("verified_customer"):
        return _answer(False, "not_owner", "the call is not for the verified customer")
    if cap is not None and amount > cap:
        return _answer(False, "over_cap", f"{amount} is above the limit of {cap}: send this to a person", escalate=True)
    if rule.get("irreversible") and call.get("id") not in approvals:
        return _answer(False, "needs_approval", "this call cannot be undone: a person must approve it first", escalate=True)
    return _answer(True, "ok", "allowed")


def _answer(allowed, code, message, escalate=False):
    return {"allowed": allowed, "code": code, "message": message, "escalate": escalate}
