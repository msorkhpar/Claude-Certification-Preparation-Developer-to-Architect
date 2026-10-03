"""Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.

Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
"""

# --- configuration checks -------------------------------------------------------------------------------------------------


def check_environment(env, agent_mcp_hosts=()):
    """Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding."""
    findings = []
    if env.get("type") == "self_hosted":
        return findings
    net = env.get("networking")
    if net is None:
        findings.append("networking omitted: a create request that omits it gets unrestricted")
        return findings
    if net.get("type") == "unrestricted":
        findings.append("unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox")
        return findings
    for host in net.get("allowed_hosts", []):
        if "://" in host or "/" in host or ":" in host:
            findings.append(f"allowed_hosts entry {host!r}: use a bare hostname, no scheme, port or path")
    if env.get("packages") and not net.get("allow_package_managers"):
        findings.append("packages with limited networking need allow_package_managers: true, or the request is rejected (400)")
    hosts = set(net.get("allowed_hosts", []))
    for host in agent_mcp_hosts:
        if host not in hosts and not net.get("allow_mcp_servers"):
            findings.append(f"MCP host {host} is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)")
    return findings


def check_agent(agent, env):
    """Findings for an agent payload read together with the environment it will run in."""
    findings = []
    for tool in agent.get("tools", []):
        if tool.get("type") == "agent_toolset_20260401":
            policy = tool.get("default_config", {}).get("permission_policy", {"type": "always_allow"})["type"]
            overrides = {c["name"]: c.get("permission_policy", {}).get("type") for c in tool.get("configs", [])}
            bash = overrides.get("bash", policy)
            reach = env.get("networking", {"type": "unrestricted"}).get("type") != "limited"
            if bash == "always_allow" and reach:
                findings.append("bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking")
            for c in tool.get("configs", []):
                if c.get("allowed_domains") and c.get("blocked_domains"):
                    findings.append(f"{c['name']}: allowed_domains and blocked_domains cannot be combined")
        if tool.get("type") == "mcp_toolset":
            policy = tool.get("default_config", {}).get("permission_policy", {"type": "always_ask"})["type"]
            if policy == "always_allow":
                findings.append(f"mcp toolset {tool['mcp_server_name']}: always_allow lets new tools of that server run unreviewed")
    return findings


def check_session_resources(env, resources):
    """A self-hosted sandbox accepts memory_store resources only."""
    if env.get("type") == "self_hosted":
        bad = sorted({r["type"] for r in resources if r["type"] != "memory_store"})
        return [f"self-hosted sandboxes reject {t} resources (400)" for t in bad]
    return []


# --- list cost and budget -------------------------------------------------------------------------------------------------

PRICES = {"claude-opus-5-5": (4, 20), "claude-sonnet-5-5": (2, 10)}  # dollars per million input and output tokens
WEB_SEARCH_MICRO = 10_000  # $10 per 1,000 searches, in millionths of a dollar per search
RUNNING_MICRO_PER_HOUR = 80_000  # $0.08 per hour of session running time


def list_cost_cents(model, input_tokens, output_tokens, searches, active_seconds):
    """The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it."""
    price_in, price_out = PRICES[model]
    micro = input_tokens * price_in + output_tokens * price_out + searches * WEB_SEARCH_MICRO + active_seconds * RUNNING_MICRO_PER_HOUR // 3600
    return (micro + 5_000) // 10_000


def check_budget(amount):
    """max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero."""
    if not (amount.isdigit() and not amount.startswith("0")):
        return f"amount {amount!r} is rejected: write whole cents as a string with no leading zeros"
    return "ok"


def budget_state(cost_cents, amount):
    """At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes."""
    cap = int(amount)
    return "budget_reached" if cost_cents >= cap else f"running, {cap - cost_cents} cents left"


def main():
    env = {"type": "cloud", "packages": {"pip": ["sqlalchemy==2.0.30"]}, "networking": {"type": "limited", "allowed_hosts": ["https://api.example.com"]}}
    agent = {"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}
    print("environment findings:")
    for line in check_environment(env, agent_mcp_hosts=["mcp.example.com"]):
        print(" -", line)
    print("omitted networking:", check_environment({"type": "cloud"})[0])
    print("agent findings, unrestricted sandbox:")
    for line in check_agent(agent, {"type": "cloud"}):
        print(" -", line)
    print("agent findings, limited sandbox:", check_agent(agent, {"type": "cloud", "networking": {"type": "limited"}}))
    print("self-hosted with a file resource:", check_session_resources({"type": "self_hosted"}, [{"type": "file"}, {"type": "memory_store"}]))
    print()
    for model in ("claude-opus-5-5", "claude-sonnet-5-5"):
        cents = list_cost_cents(model, 1_200_000, 150_000, 8, 7200)
        print(f"{model}: list cost {cents} cents; budget 800 -> {budget_state(cents, '800')}; budget 1000 -> {budget_state(cents, '1000')}")
    print("budget '25.00':", check_budget("25.00"), "| budget '050':", check_budget("050"), "| budget '125':", check_budget("125"))


if __name__ == "__main__":
    main()
