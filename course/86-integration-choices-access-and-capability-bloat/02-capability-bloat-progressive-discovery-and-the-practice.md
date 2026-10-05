# Capability bloat, progressive discovery and the practice

**Level:** Architect Professional · **Module 86:** Integration choices, access and capability bloat · **Page 2 of 2**
**Exams:** P3

**After this page you can** count the three costs of a bloated tool set, apply least privilege by removing tools and not by watching them, decide when tool definitions should load up front and when they should be found through a search, justify the choice as an accuracy against latency trade, and write the module's practice.

Checked on 2026-10-04 against the Claude documentation page "Tool search tool", Anthropic's articles "Advanced tool use" and "Writing effective tools for agents", the Claude Certified Architect - Professional exam guide (version 1.0, domain 3 and its sample item 1), and by running the example and the practice offline in the course container. Nothing here called a model, and the sizes of tool definitions in the example are its own invented figures. This page deepens module 52 (tool interfaces), module 54 (distributing tools across agents) and module 27 (choosing an extension). The choice of mechanism, the gateway and the question of whose rights a call uses are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 3 asks the candidate to "Evaluate tool/agent configuration for capability bloat", to "Evaluate progressive discovery vs. monolithic context strategy" and to "Evaluate accuracy-latency trade-offs and justify configuration decisions". Its sample item 1 keys least privilege as removing the tools a role does not need "entirely" and calls logging and confirmation prompts compensating. *What the current product does (documentation read 2026-10-04):* progressive discovery is a product feature, the tool search tool, with two variants, `tool_search_tool_regex_20251119` and `tool_search_tool_bm25_20251119`, available on the current Claude models. A tool is marked `defer_loading: true` and is found through a search. Every tool's full definition is still sent with each request, and only what enters the context window changes. On the exam, answer in the guide's terms of progressive discovery. In a design, name the feature and its threshold.

## Why it matters

An internal platform exposes 45 tools from eight systems to agents used by 300 employees. Selection quality is dropping: the agent calls `get_customer` when the user asks about an order, and fills a parameter from the wrong tool's schema. The prompt is long before the conversation starts, and every request pays for it. A review finds that the support agent also holds a refund tool and an account-deletion tool that no support task has used in a year. Capability bloat has three costs at once: context, selection accuracy and risk. The exam asks which fix attacks which cost, and which fixes only look like they do.

## The idea

### Three costs of too many capabilities

- **Context.** Every definition is read on every request. Anthropic's article counts a five-server setup of GitHub, Slack, Sentry, Grafana and Splunk at 58 tools consuming "approximately 55K tokens before the conversation even starts". In the example, four servers with 24 tools cost 12,120 tokens of definitions.
- **Selection accuracy.** The documentation says "Claude's ability to pick the right tool degrades once you exceed 30–50 available tools", and the article names the most common failures as "wrong tool selection and incorrect parameters, especially when tools have similar names". The tool-writing article adds that "Too many tools or overlapping tools can also distract agents from pursuing efficient strategies". Merging tools into one with a mode argument is not a cure: it hides the behaviours from the model's own choice and leaves the number of capabilities the same.
- **Risk.** Each tool is authority. A tool the role never uses is an attack surface and a mistake waiting to happen, and it costs context as well.

### Least privilege: remove, do not watch

The exam's sample item gives an agent that can read tickets, draft replies, issue refunds and delete accounts, where staff only read and draft. The keyed answer removes the refund and delete tools from the configuration entirely. Logging and confirmation prompts are compensating controls: they act after the privilege exists, or they ask a person to carry the risk of a capability the role never needed. A larger model does not change what the agent is allowed to do.

The example's `audit` is the review as code. It compares what an agent holds with what its role needs and returns four things. The tools to remove are the ones held and not needed, and the risky ones among them (money, destruction) are named so that they are removed first. The needed tools the agent lacks are listed too, since a review that only removes will break the role. A fourth list is the dormant tools: held, needed and never used in the observed period. They are not removed, because the role needs them; they are reviewed, since either the role description or the usage sample is wrong. In the example, removing two tools cuts the support agent from 830 tokens of definitions to 380.

### Progressive discovery: load few, find the rest

The tool search tool lets Claude "work with hundreds or thousands of tools by discovering and loading them on demand". The mechanics are short. You include a search tool in the `tools` list, and you mark the tools that should not load up front with `defer_loading: true`. Claude's context holds the search tool and the non-deferred tools. When it needs another tool it searches, the API returns the matches as references, expands them into full definitions, and Claude calls the tool. Four facts matter for a design.

- **When to use it.** The documentation lists "10 or more tools", "tool definitions consume more than 10k tokens", falling selection accuracy and a growing library. Standard tool calling is "a better fit when you have fewer than 10 tools, every tool is used in every request, or your tool definitions are small".
- **Keep the common path free of search.** "Keep your 3–5 most frequently used tools non-deferred so Claude can call them without searching first." The example loads the four most used tools and defers the other twenty.
- **Everything is still sent.** "You still send every tool's full definition in the `tools` array on every request", including the deferred ones, because the API needs them to run the search. Deferring changes what enters the context window, and it does not shrink the request.
- **Caching survives.** The deferred tools are kept out of the prefix, so "The prefix is untouched, so prompt caching is preserved". A deferred tool cannot itself carry a cache breakpoint.

For tools that come from MCP servers, deferral can be set once for the whole server on its toolset entry. The example's four servers go from 12,120 tokens to 2,320 up front, which includes a search tool of 350 tokens that the example assumes: 81 percent fewer in this example's arithmetic. Anthropic's page says tool search "typically reduces this by over 85 percent" for a load of three to five tools out of a larger set; the two figures are different measurements of different sets, and your own is the one to use.

### An accuracy against latency trade

Neither extreme is free. Loading everything costs context on every request and, past the 30 to 50 tools of the documentation, accuracy. Deferring adds a step: when the tool that is needed is not loaded, the model calls the search first, and that is a round trip. The design decision is to put the tools of the common path in the loaded set, so that most requests never search, and to defer the long tail, where a search costs less than the definitions cost on every request. Where the tool set is small, loading everything is the better design: it has no extra step and nothing to tune. The decision is justified with a measurement, not an opinion: the same evaluation set run against both configurations, with selection accuracy and latency compared (module 88). Names help either way: prefixes such as `github_` and `slack_` let one search match a whole group.

The same shape appears in the knowledge question of module 85. A small body of knowledge goes into a cached prompt, which is the monolithic context, and a large one is retrieved. In both places the monolithic choice is right for a small, stable, shared set, and discovery is right for a large or growing one.

### The example

The example is the one from the first page. Its first half is this page: the audit of a support agent, then two tool sets, a small one that loads whole and a large one that defers, with the tokens before and after. It ran offline in every language.

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

### The practice: capability design as code

The practice is in [`exercises/86-integration-choices-access-and-capability-bloat`](../../exercises/86-integration-choices-access-and-capability-bloat/unit-01/practice-1/statement.md). You write the audit of an agent's tools, the plan for loading definitions with its thresholds and its floor and ceiling on the loaded set, the choice of mechanism, the authorization of a call by the user's and the agent's rights, and a gateway that decides in order and keeps a record of every decision. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see when it works.

## Traps

1. **"Log every refund and delete so that misuse can be audited."** It is tempting because logging is cheap and nothing is taken away. The exam rejects it: the role never needed the capability, and "compensating controls" act after the privilege exists. The tool is removed from the configuration.
2. **"Shorten every tool description to save tokens."** It is tempting because it is a quick change that touches every tool. It fails on two counts: it trades selection quality for tokens, since the descriptions are what the model chooses by, and it leaves the tool count where it was. Fewer loaded definitions, found on demand, attack both.
3. **"Leave the deferred tools out of the request to save more."** It is tempting because they are not in the context anyway. It fails: "You still send every tool's full definition in the `tools` array on every request", since the search runs on the full set.

## Quiz

1. A support agent can read tickets, draft replies, pay customers back and permanently close user profiles. Staff only ever read tickets and draft replies. Applying least privilege, which change reduces risk most?
   - **a**: Record every repayment and closure so that misuse can be audited later
   - **b**: Strip the repayment and account-closure capabilities from its configuration altogether
   - **c**: Keep every capability but ask for a confirmation before each repayment and closure
   - **d**: Replace the agent with a larger model that follows instructions more reliably

2. An internal agent is offered 45 capabilities from eight systems. It increasingly picks the wrong one, and the descriptions of all of them fill much of the window before the first message. Which first step fits?
   - **a**: Shorten every description so that the definitions take fewer tokens
   - **b**: Merge the capabilities into a few multi-purpose ones that take a mode argument
   - **c**: Move to a larger model so that more definitions fit in the window
   - **d**: Defer most definitions behind a search tool and keep the few most used loaded

<details>
<summary>Answer key</summary>

1. **b**. The role never needs those capabilities, so removing them from the configuration removes the risk. *a* is ruled out because "Logging and confirmation prompts are compensating controls", acting after the privilege exists. *c* is ruled out for the same reason, since a confirmation means "they ask a person to carry the risk of a capability the role never needed". *d* is ruled out because "A larger model does not change what the agent is allowed to do".
2. **d**. With 10 or more tools and large definitions, the search tool loads on demand and the most used tools stay loaded. *a* is ruled out because "it trades selection quality for tokens" and leaves the tool count where it was. *b* is ruled out because merging "hides the behaviours from the model's own choice and leaves the number of capabilities the same". *c* is ruled out because a larger window does not cure it: "Claude's ability to pick the right tool degrades once you exceed 30–50 available tools".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team has 60 tools and turns on the tool search tool. A developer proposes to omit the least-used tools from the request, to save tokens. What does the documentation say?
   - **a**: Send the least-used ones inside the system prompt as plain text
   - **b**: Send only the loaded tools, and keep the rest in the application
   - **c**: Send every definition and mark the infrequent ones as deferred
   - **d**: Send every definition with a cache breakpoint on each deferred one

2. A gateway receives a request whose credential has been revoked and which also names a model that the team may not use. What does it answer, and what does it record?
   - **a**: A refusal for the model, recorded under the team that owned the revoked credential
   - **b**: A refusal as unauthenticated, logged under unknown with the verdict but no prompt text
   - **c**: An approval with a warning in the log, since a revoked credential is the provider's concern
   - **d**: A refusal that keeps the full prompt in the record, so that an attack can be studied later

3. A partner company's autonomous research system, built on another framework, must hand sub-tasks to your own autonomous system and collect the results. Which mechanism does the integration use?
   - **a**: An MCP server that exposes your system as one tool of the partner's system
   - **b**: A direct call from the partner's code into the functions of your system
   - **c**: A subagent that your own framework starts on the partner's behalf
   - **d**: An agent-to-agent protocol between the two independent parties

<details>
<summary>Answer key</summary>

1. **c**. The search runs on the full set, so every definition is sent and the infrequent ones are marked as deferred. *b* is ruled out because "You still send every tool's full definition in the `tools` array on every request". *a* is ruled out because deferral changes "what enters the context window", and text in the prompt would load them all every time. *d* is ruled out because deferred tools are kept out of the prefix, and "A deferred tool cannot itself carry a cache breakpoint".
2. **b**. Who is calling comes first, and the record keeps "the team (or "unknown"), the model and the verdict, and no prompt text". *a* is ruled out because "A request with no credential is refused as unauthenticated even when it also asks for a model it may not have". *c* is ruled out because "every decision, the refusals included, leaves a record". *d* is ruled out because "a log of content is its own data-protection problem".
3. **d**. The counterpart is an independent agent with its own owner, which is the agent-to-agent case. *a* is ruled out because "MCP is for agent-to-tool communication", and the partner's agent is a peer and not a tool. *b* is ruled out because a direct call suits "The path is known: the same steps in the same order", and a delegation between two agents is not that. *c* is ruled out because A2A is "Not a sub-agent or tool-call protocol", and a subagent is how an agent delegates inside its own framework, not across two.

</details>

Question 2 of the first quiz is adapted from sample item 1 of the Claude Certified Architect - Professional exam guide (Anthropic, version 1.0), which the guide offers as an illustration of item style. Question 2 of the module quiz and the first of the quiz above are adapted in part from the practice questions of CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
