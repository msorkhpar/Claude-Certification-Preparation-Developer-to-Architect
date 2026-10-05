# Which mechanism connects it, what a gateway centralises and whose rights a call uses

**Level:** Architect Professional · **Module 86:** Integration choices, access and capability bloat · **Page 1 of 2**
**Exams:** P3

**After this page you can** choose between a direct call in code, a custom tool, an MCP server and agent-to-agent from the situation, say what a gateway centralises and what it costs to run, find the authorization gap in an agent that acts for a user, and explain why a token must not be passed through to another service.

Checked on 2026-10-04 against the Model Context Protocol pages "What is the Model Context Protocol" and "Security best practices", the home page of the Agent2Agent (A2A) protocol, the Claude Code documentation pages "Run Claude Code through a gateway" and "Other LLM gateways", Anthropic's article "Building effective agents", the Claude Certified Architect - Professional exam guide (version 1.0, domain 3 and its sample item 1), and by running the example offline in the course container. Nothing here called a model. This page deepens modules 32 and 33 (MCP), module 34 (workflows and agents), module 54 (distributing tools) and module 41 (security): it does not teach how an MCP server is written or how a scope is requested, only how an architect chooses among them. Capability bloat, progressive discovery and the practice are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 3 asks the candidate to "Evaluate connection protocols and select the appropriate integration mechanism (MCP, API/CLI, agent-to-agent)" and to "Analyze authentication and authorization requirements to identify security gaps". It lists three mechanisms as peers. *What the current product and the protocols do (pages read 2026-10-04):* MCP and A2A are not rivals: "MCP is for agent-to-tool communication" and "A2A is for agent-to-agent communication", and A2A is a separate open project, "originally developed by Google and donated to the Linux Foundation", not an Anthropic product. A2A is also "Not a sub-agent or tool-call protocol": an agent that delegates to its own subagents uses its framework's subagents, as Claude Code and the Agent SDK do (module 46). On the exam, choose by the counterpart and the path, as the guide does. In a design review, say which project defines the protocol and who operates it.

## Why it matters

A platform team owns four capabilities: ticket search, refunds, deploy status and a knowledge base. Three product teams want agents to use them, a partner's research agent wants to hand sub-tasks to one of the agents, and a nightly job needs ticket search in a fixed sequence. Each team proposes its own integration: a custom tool, a command-line wrapper, an MCP server, an agent-to-agent endpoint. Then an incident arrives: an agent that runs under a service account with refund rights paid out a refund asked for by a clerk who may only read tickets. The model behaved as designed. Two architect decisions were missing: which mechanism for which situation, and whose rights a call carries. Domain 3 tests both.

## The idea

### Four mechanisms, four situations

| Situation | Mechanism | What it gives | What it costs |
|---|---|---|---|
| The path is known: the same steps in the same order | A direct call in code | The code calls, so the step is testable and costs no tool-selection turn | Nothing else can reuse it |
| One application needs the capability, and the model decides when | A custom tool in that application | The simplest tool definition that works | One application only, defined again elsewhere |
| Several clients must reuse the capability | An MCP server | One definition for many applications, including Claude Code | A server to run and secure, and definitions that take context |
| The counterpart is another agent with its own goals and owner | Agent-to-agent | Independent agents discover each other, delegate tasks and share results | A second system to trust, with its own failures |

The order in which the example asks is the order of the decision. First the counterpart: another agent is reached agent-to-agent, whatever else is true. Then the path: when it is known, the code calls, because "Workflows are systems where LLMs and tools are orchestrated through predefined code paths", and a model that is asked whether to call a function it must always call adds a failure mode and no value. Only for a path the model chooses does the number of clients decide between a tool of one application and an MCP server that several share. MCP, in its own words, is "an open-source standard for connecting AI applications to external systems", the "USB-C port" that lets one capability plug into many hosts. That is the argument for it and also its price: a server is infrastructure.

### A gateway: one place for credentials, limits and records

When many developers or many agents call a model, the credentials, the limits and the records can live in each client or in one place. A gateway is the one place: a proxy your organization runs between the clients and the provider. The Claude Code documentation lists what it gives you: "the provider key stays server-side; developers hold gateway credentials instead", usage attributed by developer or team, "enforce budgets and rate limits in one place", "log every model request for compliance" and a provider that can be changed in one configuration. Two kinds of credential are involved: each developer holds their own, "issued by the gateway", and the gateway holds one for the provider account, "shared by all forwarded traffic". Offboarding a developer revokes one credential, and a leaked developer credential is not a leaked provider key.

The same documentation states the cost as plainly: "The tradeoff is that the gateway becomes infrastructure your organization operates." It has to forward what its clients send as they change, and Anthropic "doesn't endorse, maintain, or audit" third-party gateway products. The example's gateway decides in an order, and the order matters: who is calling comes first, because every later rule is a rule about a team; then which models that team may use; then the request rate. A request with no credential is refused as unauthenticated even when it also asks for a model it may not have. And every decision, the refusals included, leaves a record with the team (or "unknown"), the model and the verdict, and no prompt text: a log of denials is how an attack is seen, and a log of content is its own data-protection problem (module 83).

### Whose rights a call uses

Authentication says who is calling. Authorization says what that caller may do. An agent that acts for a user has two sets of rights: its own, which belong to its service account, and the user's. Using the agent's alone is the gap in the incident: the clerk could not issue a refund, the agent could, and the clerk got one by asking. The rule in the example is that a call is allowed only when the user holds the scope the tool needs and the agent does too. The agent's rights are a ceiling, not a licence: they limit what any user can reach through it, and the user's rights decide what this user can. A call that fails reports which side lacked the scope, so that the denial can be read.

Three other repairs leave the gap open. Removing the refund tool for everyone is not the fix: it also stops the supervisors who are allowed to use it. A record is written after the refund is paid, so logging and confirmation prompts are compensating controls and not a repair of the rights. And a line in the system prompt is a request to the model and not a control.

Two requirements of the Model Context Protocol follow the same logic. "Token passthrough is explicitly forbidden in the authorization specification": a server must not accept a token that was issued for something else and forward it downstream, because the downstream service would trust a token whose audience was never checked, and controls that depend on the token's audience, such as rate limits, are bypassed. And scopes follow least privilege too: "Poor scope design increases token compromise impact, elevates user friction, and obscures audit trails", so the guidance is to "Implement a progressive, least-privilege scope model", starting from read operations and raising the scope when a privileged operation is first attempted. A token with every scope granted up front makes a stolen token worth the whole system.

### The example

The example holds the rules of this page and the next as code: an audit of a support agent's tools, a plan for loading tool definitions, the mechanism chosen for four situations, the refund asked by a clerk, and four requests to a gateway. It ran offline in every language, with the same output.

<!-- example: m86-capability-audit tabs: python,typescript,java,kotlin -->
```python
"""Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.

The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
"Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)

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
    log.debug("audit input: %r", holds)
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
```
```text
support agent holds 4 tools (830 tokens) and needs 2
  remove: issue_refund, delete_account; risky among them: issue_refund, delete_account
  after removal: 2 tools, 380 tokens
three tools: 3 tools, 620 tokens of definitions -> search tool no, 3 loaded now, 0 deferred, 620 tokens up front
four servers: 24 tools, 12120 tokens of definitions -> search tool yes, 4 loaded now, 20 deferred, 2320 tokens up front, 81% fewer
  loaded now: github_create_issue, github_search_code, slack_post_message, github_get_pr
  1 application(s), counterpart tool , path model-chosen -> custom tool
  4 application(s), counterpart tool , path model-chosen -> MCP server
  1 application(s), counterpart tool , path fixed        -> direct call in code
  1 application(s), counterpart agent, path model-chosen -> agent-to-agent
refund asked by a user who may only read: agent's rights alone -> allow; user's and agent's rights -> deny: user lacks refunds:write
  gateway: credential none  model standard recent  0 -> deny: unauthenticated
  gateway: credential key-a model deep     recent  0 -> deny: model not allowed
  gateway: credential key-a model standard recent 30 -> deny: rate limited
  gateway: credential key-b model deep     recent  3 -> allow: routed to claude-opus-5-5
  records kept: 4, denials among them: 3
```
```typescript
import { logger } from "./logger.ts";
const log = logger("capability_audit");

/**
 * Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.
 *
 * The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
 * "Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
 */
export const CATALOG: Record<string, [string, number]> = { read_ticket: ["read", 160], draft_reply: ["draft", 220], issue_refund: ["money", 240], delete_account: ["destroy", 210] };
const RISKY = new Set(["money", "destroy"]);
const SERVER_TOOLS: Record<string, string[]> = {
  github: ["create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"],
  slack: ["post_message", "search", "list_channels", "get_thread", "react", "upload"],
  sentry: ["list_issues", "get_event", "resolve", "assign", "search"],
  grafana: ["query", "list_dashboards", "get_panel", "create_alert", "list_alerts"],
};
const SERVER_SIZE: Record<string, number> = { github: 520, slack: 410, sentry: 480, grafana: 620 };
export const TOOLS: Record<string, number> = Object.fromEntries(Object.entries(SERVER_TOOLS).flatMap(([server, names]) => names.map((name) => [`${server}_${name}`, SERVER_SIZE[server]])));
export const USAGE: Record<string, number> = { github_create_issue: 90, github_search_code: 70, slack_post_message: 60, github_get_pr: 50, sentry_list_issues: 20, grafana_query: 10 };
export type Policy = { credentials: Record<string, string>; models: Record<string, string[]>; limits: Record<string, number>; routes: Record<string, string> };
export const POLICY: Policy = {
  credentials: { "key-a": "support", "key-b": "research" },
  models: { support: ["standard"], research: ["standard", "deep"] },
  limits: { support: 30, research: 10 },
  routes: { standard: "claude-sonnet-5-5", deep: "claude-opus-5-5" },
};

/** Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation. */
export function audit(holds: string[], needs: string[], catalog: Record<string, [string, number]>) {
  log.debug("audit input", holds);
  const remove = holds.filter((t) => !needs.includes(t));
  return { remove, risky: remove.filter((t) => RISKY.has(catalog[t][0])), missing: needs.filter((t) => !holds.includes(t)) };
}

/** With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool. */
export function planLoading(tools: Record<string, number>, usage: Record<string, number>, keep = 4, searchTokens = 350) {
  keep = Math.max(3, Math.min(5, keep));
  const names = Object.keys(tools);
  const total = names.reduce((a, t) => a + tools[t], 0);
  if (names.length < 10 && total <= 10000) return { search: false, load_now: names, deferred: [] as string[], tokens: total };
  const ranked = [...names].sort((a, b) => (usage[b] ?? 0) - (usage[a] ?? 0) || (a < b ? -1 : 1)).slice(0, keep);
  return { search: true, load_now: ranked, deferred: names.filter((t) => !ranked.includes(t)), tokens: ranked.reduce((a, t) => a + tools[t], 0) + searchTokens };
}

/** Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application. */
export function chooseMechanism(consumers: number, counterpart: string, path: string): string {
  if (counterpart === "agent") return "agent-to-agent";
  if (path === "fixed") return "direct call in code";
  return consumers > 1 ? "MCP server" : "custom tool";
}

/** A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence. */
export function authorize(tool: string, userScopes: string[], agentScopes: string[], required: Record<string, string>): string {
  if (!(tool in required)) return "deny: unknown tool";
  const scope = required[tool];
  if (!userScopes.includes(scope)) return `deny: user lacks ${scope}`;
  if (!agentScopes.includes(scope)) return `deny: agent lacks ${scope}`;
  return "allow";
}

/** One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision. */
export function gateway(credential: string | null, model: string, recent: number, policy: Policy) {
  const team = credential === null ? undefined : policy.credentials[credential];
  let decision = "deny";
  let reason: string;
  if (team === undefined) reason = "unauthenticated";
  else if (!policy.models[team].includes(model)) reason = "model not allowed";
  else if (recent >= policy.limits[team]) reason = "rate limited";
  else {
    decision = "allow";
    reason = `routed to ${policy.routes[model] ?? model}`;
  }
  return { decision, reason, audit: { team: team ?? "unknown", model, decision } };
}

const percent = (part: number, whole: number) => Math.floor((200 * part + whole) / (2 * whole));

function main() {
  const holds = Object.keys(CATALOG);
  const needs = ["read_ticket", "draft_reply"];
  const result = audit(holds, needs, CATALOG);
  const cost = (names: string[]) => names.reduce((a, t) => a + CATALOG[t][1], 0);
  console.log(`support agent holds ${holds.length} tools (${cost(holds)} tokens) and needs ${needs.length}`);
  console.log(`  remove: ${result.remove.join(", ")}; risky among them: ${result.risky.join(", ")}`);
  console.log(`  after removal: ${holds.length - result.remove.length} tools, ${cost(needs)} tokens`);
  const three = Object.fromEntries(Object.entries(CATALOG).slice(0, 3).map(([k, v]) => [k, v[1]]));
  let plan = planLoading(three, USAGE);
  for (const [name, tools] of [["three tools", three], ["four servers", TOOLS]] as Array<[string, Record<string, number>]>) {
    plan = planLoading(tools, USAGE);
    const total = Object.values(tools).reduce((a, b) => a + b, 0);
    const saved = plan.search ? `, ${percent(total - plan.tokens, total)}% fewer` : "";
    console.log(`${name}: ${Object.keys(tools).length} tools, ${total} tokens of definitions -> search tool ${plan.search ? "yes" : "no"}, ${plan.load_now.length} loaded now, ${plan.deferred.length} deferred, ${plan.tokens} tokens up front${saved}`);
  }
  console.log("  loaded now: " + plan.load_now.join(", "));
  const rows: Array<[number, string, string]> = [[1, "tool", "model-chosen"], [4, "tool", "model-chosen"], [1, "tool", "fixed"], [1, "agent", "model-chosen"]];
  for (const [consumers, counterpart, path] of rows) console.log(`  ${consumers} application(s), counterpart ${counterpart.padEnd(5)}, path ${path.padEnd(12)} -> ${chooseMechanism(consumers, counterpart, path)}`);
  const required = { read_ticket: "tickets:read", issue_refund: "refunds:write" };
  const user = ["tickets:read"];
  const agent = ["tickets:read", "refunds:write"];
  console.log(`refund asked by a user who may only read: agent's rights alone -> ${agent.includes(required.issue_refund) ? "allow" : "deny"}; user's and agent's rights -> ${authorize("issue_refund", user, agent, required)}`);
  const log: Array<{ team: string; model: string; decision: string }> = [];
  const requests: Array<[string | null, string, number]> = [[null, "standard", 0], ["key-a", "deep", 0], ["key-a", "standard", 30], ["key-b", "deep", 3]];
  for (const [credential, model, recent] of requests) {
    const outcome = gateway(credential, model, recent, POLICY);
    log.push(outcome.audit);
    console.log(`  gateway: credential ${(credential ?? "none").padEnd(5)} model ${model.padEnd(8)} recent ${String(recent).padStart(2)} -> ${outcome.decision}: ${outcome.reason}`);
  }
  console.log(`  records kept: ${log.length}, denials among them: ${log.filter((r) => r.decision === "deny").length}`);
}

if (import.meta.main) main();
```
```text
support agent holds 4 tools (830 tokens) and needs 2
  remove: issue_refund, delete_account; risky among them: issue_refund, delete_account
  after removal: 2 tools, 380 tokens
three tools: 3 tools, 620 tokens of definitions -> search tool no, 3 loaded now, 0 deferred, 620 tokens up front
four servers: 24 tools, 12120 tokens of definitions -> search tool yes, 4 loaded now, 20 deferred, 2320 tokens up front, 81% fewer
  loaded now: github_create_issue, github_search_code, slack_post_message, github_get_pr
  1 application(s), counterpart tool , path model-chosen -> custom tool
  4 application(s), counterpart tool , path model-chosen -> MCP server
  1 application(s), counterpart tool , path fixed        -> direct call in code
  1 application(s), counterpart agent, path model-chosen -> agent-to-agent
refund asked by a user who may only read: agent's rights alone -> allow; user's and agent's rights -> deny: user lacks refunds:write
  gateway: credential none  model standard recent  0 -> deny: unauthenticated
  gateway: credential key-a model deep     recent  0 -> deny: model not allowed
  gateway: credential key-a model standard recent 30 -> deny: rate limited
  gateway: credential key-b model deep     recent  3 -> allow: routed to claude-opus-5-5
  records kept: 4, denials among them: 3
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.
 *
 * <p>The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
 * "Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
 */
public final class CapabilityAudit {
    private static final System.Logger LOG = System.getLogger(CapabilityAudit.class.getName());
    private CapabilityAudit() {}

    /** A tool's access class and the size of its definition in tokens. */
    record Tool(String access, int tokens) {}

    /** The outcome of an audit: what to remove, which of that is risky and what the role lacks. */
    record Audit(List<String> remove, List<String> risky, List<String> missing) {}

    /** How tool definitions load: with a search tool or not, which tools now and which later, and the tokens up front. */
    record Plan(boolean search, List<String> loadNow, List<String> deferred, int tokens) {}

    /** The gateway's rules: which credential belongs to which team, the models and requests a minute each team may use, and where a model name is routed. */
    record Policy(Map<String, String> credentials, Map<String, Set<String>> models, Map<String, Integer> limits, Map<String, String> routes) {}

    /** The gateway's decision, its reason and the record kept of it. */
    record Outcome(String decision, String reason, Map<String, String> audit) {}

    static final Map<String, Tool> CATALOG = new LinkedHashMap<>();
    static final Map<String, Integer> TOOLS = new LinkedHashMap<>();
    static final Map<String, Integer> USAGE = Map.of("github_create_issue", 90, "github_search_code", 70, "slack_post_message", 60, "github_get_pr", 50, "sentry_list_issues", 20, "grafana_query", 10);
    static final Policy POLICY = new Policy(Map.of("key-a", "support", "key-b", "research"), Map.of("support", Set.of("standard"), "research", Set.of("standard", "deep")), Map.of("support", 30, "research", 10),
        Map.of("standard", "claude-sonnet-5-5", "deep", "claude-opus-5-5"));
    private static final Set<String> RISKY = Set.of("money", "destroy");

    static {
        CATALOG.put("read_ticket", new Tool("read", 160));
        CATALOG.put("draft_reply", new Tool("draft", 220));
        CATALOG.put("issue_refund", new Tool("money", 240));
        CATALOG.put("delete_account", new Tool("destroy", 210));
        Map<String, List<String>> servers = new LinkedHashMap<>();
        servers.put("github", List.of("create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"));
        servers.put("slack", List.of("post_message", "search", "list_channels", "get_thread", "react", "upload"));
        servers.put("sentry", List.of("list_issues", "get_event", "resolve", "assign", "search"));
        servers.put("grafana", List.of("query", "list_dashboards", "get_panel", "create_alert", "list_alerts"));
        Map<String, Integer> size = Map.of("github", 520, "slack", 410, "sentry", 480, "grafana", 620);
        servers.forEach((server, names) -> names.forEach(name -> TOOLS.put(server + "_" + name, size.get(server))));
    }

    /** Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation. */
    static Audit audit(List<String> holds, List<String> needs, Map<String, Tool> catalog) {
        LOG.log(System.Logger.Level.DEBUG, "audit input: {0}", holds);
        List<String> remove = holds.stream().filter(t -> !needs.contains(t)).toList();
        return new Audit(remove, remove.stream().filter(t -> RISKY.contains(catalog.get(t).access())).toList(), needs.stream().filter(t -> !holds.contains(t)).toList());
    }

    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage) {
        return planLoading(tools, usage, 4, 350);
    }

    /** With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool. */
    static Plan planLoading(Map<String, Integer> tools, Map<String, Integer> usage, int keep, int searchTokens) {
        keep = Math.max(3, Math.min(5, keep));
        List<String> names = new ArrayList<>(tools.keySet());
        int total = tools.values().stream().mapToInt(Integer::intValue).sum();
        if (names.size() < 10 && total <= 10000) return new Plan(false, names, List.of(), total);
        List<String> sorted = new ArrayList<>(names);
        sorted.sort((a, b) -> usage.getOrDefault(a, 0).equals(usage.getOrDefault(b, 0)) ? a.compareTo(b) : Integer.compare(usage.getOrDefault(b, 0), usage.getOrDefault(a, 0)));
        List<String> ranked = new ArrayList<>(sorted.subList(0, Math.min(keep, sorted.size())));
        return new Plan(true, List.copyOf(ranked), names.stream().filter(t -> !ranked.contains(t)).toList(), ranked.stream().mapToInt(tools::get).sum() + searchTokens);
    }

    /** Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application. */
    static String chooseMechanism(int consumers, String counterpart, String path) {
        if (counterpart.equals("agent")) return "agent-to-agent";
        if (path.equals("fixed")) return "direct call in code";
        return consumers > 1 ? "MCP server" : "custom tool";
    }

    /** A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence. */
    static String authorize(String tool, Set<String> userScopes, Set<String> agentScopes, Map<String, String> required) {
        if (!required.containsKey(tool)) return "deny: unknown tool";
        String scope = required.get(tool);
        if (!userScopes.contains(scope)) return "deny: user lacks " + scope;
        if (!agentScopes.contains(scope)) return "deny: agent lacks " + scope;
        return "allow";
    }

    /** One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision. */
    static Outcome gateway(String credential, String model, int recent, Policy policy) {
        String team = credential == null ? null : policy.credentials().get(credential);
        String decision = "deny";
        String reason;
        if (team == null) reason = "unauthenticated";
        else if (!policy.models().get(team).contains(model)) reason = "model not allowed";
        else if (recent >= policy.limits().get(team)) reason = "rate limited";
        else {
            decision = "allow";
            reason = "routed to " + policy.routes().getOrDefault(model, model);
        }
        return new Outcome(decision, reason, Map.of("team", team == null ? "unknown" : team, "model", model, "decision", decision));
    }

    private static int percent(int part, int whole) {
        return (200 * part + whole) / (2 * whole);
    }

    public static void main(String[] args) {
        List<String> holds = new ArrayList<>(CATALOG.keySet());
        List<String> needs = List.of("read_ticket", "draft_reply");
        Audit result = audit(holds, needs, CATALOG);
        int heldTokens = holds.stream().mapToInt(t -> CATALOG.get(t).tokens()).sum();
        int neededTokens = needs.stream().mapToInt(t -> CATALOG.get(t).tokens()).sum();
        System.out.println("support agent holds " + holds.size() + " tools (" + heldTokens + " tokens) and needs " + needs.size());
        System.out.println("  remove: " + String.join(", ", result.remove()) + "; risky among them: " + String.join(", ", result.risky()));
        System.out.println("  after removal: " + (holds.size() - result.remove().size()) + " tools, " + neededTokens + " tokens");
        Map<String, Integer> three = new LinkedHashMap<>();
        CATALOG.entrySet().stream().limit(3).forEach(e -> three.put(e.getKey(), e.getValue().tokens()));
        Plan plan = null;
        Map<String, Map<String, Integer>> sets = new LinkedHashMap<>();
        sets.put("three tools", three);
        sets.put("four servers", TOOLS);
        for (Map.Entry<String, Map<String, Integer>> e : sets.entrySet()) {
            plan = planLoading(e.getValue(), USAGE);
            int total = e.getValue().values().stream().mapToInt(Integer::intValue).sum();
            String saved = plan.search() ? ", " + percent(total - plan.tokens(), total) + "% fewer" : "";
            System.out.println(e.getKey() + ": " + e.getValue().size() + " tools, " + total + " tokens of definitions -> search tool " + (plan.search() ? "yes" : "no") + ", " + plan.loadNow().size() + " loaded now, "
                + plan.deferred().size() + " deferred, " + plan.tokens() + " tokens up front" + saved);
        }
        System.out.println("  loaded now: " + String.join(", ", plan.loadNow()));
        Object[][] rows = {{1, "tool", "model-chosen"}, {4, "tool", "model-chosen"}, {1, "tool", "fixed"}, {1, "agent", "model-chosen"}};
        for (Object[] r : rows) {
            System.out.println(String.format("  %d application(s), counterpart %-5s, path %-12s -> %s", (int) r[0], r[1], r[2], chooseMechanism((int) r[0], (String) r[1], (String) r[2])));
        }
        Map<String, String> required = Map.of("read_ticket", "tickets:read", "issue_refund", "refunds:write");
        Set<String> user = Set.of("tickets:read");
        Set<String> agent = Set.of("tickets:read", "refunds:write");
        System.out.println("refund asked by a user who may only read: agent's rights alone -> " + (agent.contains(required.get("issue_refund")) ? "allow" : "deny") + "; user's and agent's rights -> "
            + authorize("issue_refund", user, agent, required));
        List<Map<String, String>> log = new ArrayList<>();
        Object[][] requests = {{null, "standard", 0}, {"key-a", "deep", 0}, {"key-a", "standard", 30}, {"key-b", "deep", 3}};
        for (Object[] r : requests) {
            Outcome outcome = gateway((String) r[0], (String) r[1], (int) r[2], POLICY);
            log.add(outcome.audit());
            System.out.println(String.format("  gateway: credential %-5s model %-8s recent %2d -> %s: %s", r[0] == null ? "none" : r[0], r[1], (int) r[2], outcome.decision(), outcome.reason()));
        }
        System.out.println("  records kept: " + log.size() + ", denials among them: " + log.stream().filter(r -> r.get("decision").equals("deny")).count());
    }
}
```
```text
support agent holds 4 tools (830 tokens) and needs 2
  remove: issue_refund, delete_account; risky among them: issue_refund, delete_account
  after removal: 2 tools, 380 tokens
three tools: 3 tools, 620 tokens of definitions -> search tool no, 3 loaded now, 0 deferred, 620 tokens up front
four servers: 24 tools, 12120 tokens of definitions -> search tool yes, 4 loaded now, 20 deferred, 2320 tokens up front, 81% fewer
  loaded now: github_create_issue, github_search_code, slack_post_message, github_get_pr
  1 application(s), counterpart tool , path model-chosen -> custom tool
  4 application(s), counterpart tool , path model-chosen -> MCP server
  1 application(s), counterpart tool , path fixed        -> direct call in code
  1 application(s), counterpart agent, path model-chosen -> agent-to-agent
refund asked by a user who may only read: agent's rights alone -> allow; user's and agent's rights -> deny: user lacks refunds:write
  gateway: credential none  model standard recent  0 -> deny: unauthenticated
  gateway: credential key-a model deep     recent  0 -> deny: model not allowed
  gateway: credential key-a model standard recent 30 -> deny: rate limited
  gateway: credential key-b model deep     recent  3 -> allow: routed to claude-opus-5-5
  records kept: 4, denials among them: 3
```
```kotlin
private val log = System.getLogger("capability_audit")

/**
 * Integration design decisions in code: which tools a role keeps, which tool definitions load up front, which mechanism connects a capability, whose rights a tool call uses and what a gateway decides.
 *
 * The numbers are invented for the example, and the sizes of tool definitions are the example's own. The rules are those of the Claude Certified Architect - Professional exam guide (domain 3), the Claude documentation page
 * "Tool search tool", the Model Context Protocol security best practices and the Claude Code gateway pages, read on 2026-10-04. Nothing here calls a model.
 */

/** A tool's access class and the size of its definition in tokens. */
data class Tool(val access: String, val tokens: Int)

/** The outcome of an audit: what to remove, which of that is risky and what the role lacks. */
data class Audit(val remove: List<String>, val risky: List<String>, val missing: List<String>)

/** How tool definitions load: with a search tool or not, which tools now and which later, and the tokens up front. */
data class Plan(val search: Boolean, val loadNow: List<String>, val deferred: List<String>, val tokens: Int)

/** The gateway's rules: which credential belongs to which team, the models and requests a minute each team may use, and where a model name is routed. */
data class Policy(val credentials: Map<String, String>, val models: Map<String, Set<String>>, val limits: Map<String, Int>, val routes: Map<String, String>)

/** The gateway's decision, its reason and the record kept of it. */
data class Outcome(val decision: String, val reason: String, val audit: Map<String, String>)

val CATALOG = linkedMapOf("read_ticket" to Tool("read", 160), "draft_reply" to Tool("draft", 220), "issue_refund" to Tool("money", 240), "delete_account" to Tool("destroy", 210))
private val RISKY = setOf("money", "destroy")
private val SERVER_TOOLS = linkedMapOf(
    "github" to listOf("create_issue", "search_code", "get_pr", "list_prs", "merge_pr", "comment", "list_repos", "get_file"),
    "slack" to listOf("post_message", "search", "list_channels", "get_thread", "react", "upload"),
    "sentry" to listOf("list_issues", "get_event", "resolve", "assign", "search"),
    "grafana" to listOf("query", "list_dashboards", "get_panel", "create_alert", "list_alerts"),
)
private val SERVER_SIZE = mapOf("github" to 520, "slack" to 410, "sentry" to 480, "grafana" to 620)
val TOOLS: Map<String, Int> = SERVER_TOOLS.flatMap { (server, names) -> names.map { "${server}_$it" to SERVER_SIZE.getValue(server) } }.toMap(LinkedHashMap())
val USAGE = mapOf("github_create_issue" to 90, "github_search_code" to 70, "slack_post_message" to 60, "github_get_pr" to 50, "sentry_list_issues" to 20, "grafana_query" to 10)
val POLICY = Policy(
    mapOf("key-a" to "support", "key-b" to "research"), mapOf("support" to setOf("standard"), "research" to setOf("standard", "deep")), mapOf("support" to 30, "research" to 10),
    mapOf("standard" to "claude-sonnet-5-5", "deep" to "claude-opus-5-5"),
)

/** Least privilege: a tool the role does not need is removed from its configuration, not logged or put behind a confirmation. */
fun audit(holds: List<String>, needs: List<String>, catalog: Map<String, Tool>): Audit {
    log.log(System.Logger.Level.DEBUG, "audit input: {0}", holds)
    val remove = holds.filter { it !in needs }
    return Audit(remove, remove.filter { catalog.getValue(it).access in RISKY }, needs.filter { it !in holds })
}

/** With 10 or more tools, or definitions over 10,000 tokens, the 3 to 5 most used tools stay loaded and the rest are found through a search tool. */
fun planLoading(tools: Map<String, Int>, usage: Map<String, Int>, keep: Int = 4, searchTokens: Int = 350): Plan {
    val kept = keep.coerceIn(3, 5)
    val names = tools.keys.toList()
    val total = tools.values.sum()
    if (names.size < 10 && total <= 10000) return Plan(false, names, emptyList(), total)
    val ranked = names.sortedWith(compareBy({ -(usage[it] ?: 0) }, { it })).take(kept)
    return Plan(true, ranked, names.filter { it !in ranked }, ranked.sumOf { tools.getValue(it) } + searchTokens)
}

/** Another agent is reached agent-to-agent; a step with a known path is a call in code; a capability several clients share is an MCP server; otherwise it is a tool of the one application. */
fun chooseMechanism(consumers: Int, counterpart: String, path: String): String = when {
    counterpart == "agent" -> "agent-to-agent"
    path == "fixed" -> "direct call in code"
    consumers > 1 -> "MCP server"
    else -> "custom tool"
}

/** A call is allowed only when the user holds the scope the tool needs and the agent does too; the agent's own rights are a ceiling, not a licence. */
fun authorize(tool: String, userScopes: Set<String>, agentScopes: Set<String>, required: Map<String, String>): String {
    val scope = required[tool] ?: return "deny: unknown tool"
    if (scope !in userScopes) return "deny: user lacks $scope"
    if (scope !in agentScopes) return "deny: agent lacks $scope"
    return "allow"
}

/** One place decides who is calling, which models that team may use and how many requests a minute it may send, and keeps a record of every decision. */
fun gateway(credential: String?, model: String, recent: Int, policy: Policy): Outcome {
    val team = credential?.let { policy.credentials[it] }
    val (decision, reason) = when {
        team == null -> "deny" to "unauthenticated"
        model !in policy.models.getValue(team) -> "deny" to "model not allowed"
        recent >= policy.limits.getValue(team) -> "deny" to "rate limited"
        else -> "allow" to "routed to ${policy.routes[model] ?: model}"
    }
    return Outcome(decision, reason, mapOf("team" to (team ?: "unknown"), "model" to model, "decision" to decision))
}

private fun percent(part: Int, whole: Int) = (200 * part + whole) / (2 * whole)

fun main() {
    val holds = CATALOG.keys.toList()
    val needs = listOf("read_ticket", "draft_reply")
    val result = audit(holds, needs, CATALOG)
    println("support agent holds ${holds.size} tools (${holds.sumOf { CATALOG.getValue(it).tokens }} tokens) and needs ${needs.size}")
    println("  remove: ${result.remove.joinToString(", ")}; risky among them: ${result.risky.joinToString(", ")}")
    println("  after removal: ${holds.size - result.remove.size} tools, ${needs.sumOf { CATALOG.getValue(it).tokens }} tokens")
    val three = CATALOG.entries.take(3).associate { it.key to it.value.tokens }
    var plan = planLoading(three, USAGE)
    for ((name, tools) in listOf("three tools" to three, "four servers" to TOOLS)) {
        plan = planLoading(tools, USAGE)
        val total = tools.values.sum()
        val saved = if (plan.search) ", ${percent(total - plan.tokens, total)}% fewer" else ""
        println("$name: ${tools.size} tools, $total tokens of definitions -> search tool ${if (plan.search) "yes" else "no"}, ${plan.loadNow.size} loaded now, ${plan.deferred.size} deferred, ${plan.tokens} tokens up front$saved")
    }
    println("  loaded now: " + plan.loadNow.joinToString(", "))
    for ((consumers, counterpart, path) in listOf(Triple(1, "tool", "model-chosen"), Triple(4, "tool", "model-chosen"), Triple(1, "tool", "fixed"), Triple(1, "agent", "model-chosen"))) {
        println("  %d application(s), counterpart %-5s, path %-12s -> %s".format(consumers, counterpart, path, chooseMechanism(consumers, counterpart, path)))
    }
    val required = mapOf("read_ticket" to "tickets:read", "issue_refund" to "refunds:write")
    val user = setOf("tickets:read")
    val agent = setOf("tickets:read", "refunds:write")
    println("refund asked by a user who may only read: agent's rights alone -> ${if (required.getValue("issue_refund") in agent) "allow" else "deny"}; user's and agent's rights -> ${authorize("issue_refund", user, agent, required)}")
    val log = mutableListOf<Map<String, String>>()
    for ((credential, model, recent) in listOf(Triple(null, "standard", 0), Triple("key-a", "deep", 0), Triple("key-a", "standard", 30), Triple("key-b", "deep", 3))) {
        val outcome = gateway(credential, model, recent, POLICY)
        log += outcome.audit
        println("  gateway: credential %-5s model %-8s recent %2d -> %s: %s".format(credential ?: "none", model, recent, outcome.decision, outcome.reason))
    }
    println("  records kept: ${log.size}, denials among them: ${log.count { it["decision"] == "deny" }}")
}
```
```text
support agent holds 4 tools (830 tokens) and needs 2
  remove: issue_refund, delete_account; risky among them: issue_refund, delete_account
  after removal: 2 tools, 380 tokens
three tools: 3 tools, 620 tokens of definitions -> search tool no, 3 loaded now, 0 deferred, 620 tokens up front
four servers: 24 tools, 12120 tokens of definitions -> search tool yes, 4 loaded now, 20 deferred, 2320 tokens up front, 81% fewer
  loaded now: github_create_issue, github_search_code, slack_post_message, github_get_pr
  1 application(s), counterpart tool , path model-chosen -> custom tool
  4 application(s), counterpart tool , path model-chosen -> MCP server
  1 application(s), counterpart tool , path fixed        -> direct call in code
  1 application(s), counterpart agent, path model-chosen -> agent-to-agent
refund asked by a user who may only read: agent's rights alone -> allow; user's and agent's rights -> deny: user lacks refunds:write
  gateway: credential none  model standard recent  0 -> deny: unauthenticated
  gateway: credential key-a model deep     recent  0 -> deny: model not allowed
  gateway: credential key-a model standard recent 30 -> deny: rate limited
  gateway: credential key-b model deep     recent  3 -> allow: routed to claude-opus-5-5
  records kept: 4, denials among them: 3
```
<!-- /example -->

For this page, read the last three blocks. The four situations give a custom tool, an MCP server shared by four applications, a direct call and agent-to-agent, in the order of the table. The refund is allowed when the agent's rights are the only test and refused when the user's rights are tested too: "deny: user lacks refunds:write". The gateway refuses three of four requests, each for a different reason and in the order described, and keeps four records, three of them denials.

## Traps

1. **"Expose every integration as an MCP server."** It is tempting because MCP is the standard and one answer is easy to defend. The exam rejects it for a step whose path is known: the code calls, so the step is testable, and an MCP server adds a process to operate and definitions that take context.
2. **"Give the agent a service account with the union of its users' rights."** It is tempting because one account is simple to manage. The exam rejects it: the clerk who may only read gets what the account can do. The call must also hold the user's scope, so that the agent's rights act as a ceiling.
3. **"Pass the user's token on to the downstream service."** It is tempting because the user already authenticated once. It is forbidden: "Token passthrough is explicitly forbidden in the authorization specification", since the downstream service cannot tell the token was never meant for it.

## Quiz

1. A nightly job always does the same three things in the same order: fetch invoices, ask Claude to label each one, write the labels to a ledger. How should the invoice system be reached?
   - **a**: As a plain function call made by the code that runs the sequence
   - **b**: As an MCP server that the model decides to call when it needs invoices
   - **c**: As a tool in the model's list, left for the model to call when it sees fit
   - **d**: As a remote agent that the job reaches over an agent-to-agent protocol

2. A support agent runs under one service account that may issue refunds, and supervisors legitimately use the same agent for them. A clerk who may only read tickets asks it for a refund, and it is paid. What closes the gap?
   - **a**: Take the refund tool out of the agent's configuration for every user
   - **b**: Record each refund so that the clerk's request can be found later
   - **c**: Allow the call only when the person asking also holds the scope for it
   - **d**: Add a line to the system prompt telling clerks not to ask for refunds

<details>
<summary>Answer key</summary>

1. **a**. The path is fixed, so the code calls the invoice system and the model is not asked whether to. *b* is ruled out because "Workflows are systems where LLMs and tools are orchestrated through predefined code paths", and a fixed step needs no model decision. *c* is ruled out because for a known path "the code calls, so the step is testable", which a tool left to the model's choice is not. *d* is ruled out because agent-to-agent suits "The counterpart is another agent with its own goals and owner", and an invoice system is not one.
2. **c**. The call is allowed only when the user holds the scope and the agent does too, so the agent's rights are a ceiling. *a* is ruled out because "it also stops the supervisors who are allowed to use it". *b* is ruled out because "A record is written after the refund is paid", so it detects and does not prevent. *d* is ruled out because "a line in the system prompt is a request to the model and not a control", and an agent that holds the right can still grant the request.

</details>
