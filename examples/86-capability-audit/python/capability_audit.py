"""Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.

The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
"Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
"""
CATALOG = {"read_ticket": ("read", 160), "draft_reply": ("draft", 220), "issue_refund": ("money", 240), "delete_account": ("destroy", 210)}
RISKY = {"money", "destroy"}
SERVER_TOOLS = {"github": ["create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"], "slack": ["post_message", "search", "list_channels", "get_thread", "react", "upload"],
                "sentry": ["list_issues", "get_event", "resolve", "assign", "search"], "grafana": ["query", "list_dashboards", "get_panel", "create_alert", "list_alerts"]}
SERVER_SIZE = {"github": 520, "slack": 410, "sentry": 480, "grafana": 620}
TOOLS = {f"{server}_{name}": SERVER_SIZE[server] for server, names in SERVER_TOOLS.items() for name in names}
USAGE = {"github_create_issue": 90, "github_search_code": 70, "slack_post_message": 60, "github_get_pr": 50, "sentry_list_issues": 20, "grafana_query": 10}
POLICY = {"credentials": {"key-a": "support", "key-b": "research"}, "models": {"support": {"standard"}, "research": {"standard", "deep"}}, "limits": {"support": 30, "research": 10},
          "routes": {"standard": "claude-sonnet-5-5", "deep": "claude-opus-5-5"}}


def audit(holds, needs, catalog):
    """Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation."""
    remove = [t for t in holds if t not in needs]
    return {"remove": remove, "risky": [t for t in remove if catalog[t][0] in RISKY], "missing": [t for t in needs if t not in holds]}


def plan_loading(tools, usage, keep=4, search_tokens=350):
    """With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool."""
    keep = max(3, min(5, keep))
    if len(tools) < 10 and sum(tools.values()) <= 10000:
        return {"search": False, "load_now": list(tools), "deferred": [], "tokens": sum(tools.values())}
    ranked = sorted(tools, key=lambda t: (-usage.get(t, 0), t))[:keep]
    return {"search": True, "load_now": ranked, "deferred": [t for t in tools if t not in ranked], "tokens": sum(tools[t] for t in ranked) + search_tokens}


def choose_mechanism(consumers, counterpart, path):
    """Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application."""
    if counterpart == "agent":
        return "agent-to-agent"
    if path == "fixed":
        return "direct call in code"
    return "MCP server" if consumers > 1 else "custom tool"


def authorize(tool, user_scopes, agent_scopes, required):
    """A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence."""
    if tool not in required:
        return "deny: unknown tool"
    scope = required[tool]
    if scope not in user_scopes:
        return f"deny: user lacks {scope}"
    if scope not in agent_scopes:
        return f"deny: agent lacks {scope}"
    return "allow"


def gateway(credential, model, recent, policy):
    """One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision."""
    team = policy["credentials"].get(credential)
    if team is None:
        decision, reason = "deny", "unauthenticated"
    elif model not in policy["models"][team]:
        decision, reason = "deny", "model not allowed"
    elif recent >= policy["limits"][team]:
        decision, reason = "deny", "rate limited"
    else:
        decision, reason = "allow", f"routed to {policy['routes'].get(model, model)}"
    return {"decision": decision, "reason": reason, "audit": {"team": team or "unknown", "model": model, "decision": decision}}


def percent(part, whole):
    return (200 * part + whole) // (2 * whole)


def main():
    holds, needs = list(CATALOG), ["read_ticket", "draft_reply"]
    result = audit(holds, needs, CATALOG)
    cost = lambda names: sum(CATALOG[t][1] for t in names)
    print(f"support agent holds {len(holds)} tools ({cost(holds)} tokens) and needs {len(needs)}")
    print(f"  remove: {', '.join(result['remove'])}; risky among them: {', '.join(result['risky'])}")
    print(f"  after removal: {len(holds) - len(result['remove'])} tools, {cost(needs)} tokens")
    for name, tools in (("three tools", {k: v[1] for k, v in list(CATALOG.items())[:3]}), ("four servers", TOOLS)):
        plan = plan_loading(tools, USAGE)
        saved = f", {percent(sum(tools.values()) - plan['tokens'], sum(tools.values()))}% fewer" if plan["search"] else ""
        print(f"{name}: {len(tools)} tools, {sum(tools.values())} tokens of definitions -> search tool {'yes' if plan['search'] else 'no'}, {len(plan['load_now'])} loaded now, {len(plan['deferred'])} deferred, {plan['tokens']} tokens up front{saved}")
    print("  loaded now: " + ", ".join(plan["load_now"]))
    for consumers, counterpart, path in [(1, "tool", "model-chosen"), (4, "tool", "model-chosen"), (1, "tool", "fixed"), (1, "agent", "model-chosen")]:
        print(f"  {consumers} application(s), counterpart {counterpart:<5}, path {path:<12} -> {choose_mechanism(consumers, counterpart, path)}")
    required = {"read_ticket": "tickets:read", "issue_refund": "refunds:write"}
    user, agent = {"tickets:read"}, {"tickets:read", "refunds:write"}
    print(f"refund asked by a user who may only read: agent's rights alone -> {'allow' if required['issue_refund'] in agent else 'deny'}; user's and agent's rights -> {authorize('issue_refund', user, agent, required)}")
    log = []
    for credential, model, recent in [(None, "standard", 0), ("key-a", "deep", 0), ("key-a", "standard", 30), ("key-b", "deep", 3)]:
        outcome = gateway(credential, model, recent, POLICY)
        log.append(outcome["audit"])
        print(f"  gateway: credential {credential or 'none':<5} model {model:<8} recent {recent:>2} -> {outcome['decision']}: {outcome['reason']}")
    print(f"  records kept: {len(log)}, denials among them: {sum(1 for r in log if r['decision'] == 'deny')}")


if __name__ == "__main__":
    main()
