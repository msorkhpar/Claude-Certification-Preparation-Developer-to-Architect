# Environments, permission policies, budgets and self-hosting

**Level:** Developer · **Module 36:** Managed and self-hosted agents · **Page 2 of 2**
**Exams:** DV3; P1

**After this page you can** configure a cloud environment's packages and networking and say what an omitted setting does, choose a permission policy for each toolset and explain what `auto` does and does not promise, price a session and bound it with a budget, describe what a self-hosted sandbox moves and what it leaves with Anthropic, and say who owns which security duty in each option.

Checked against the Managed Agents pages of the Claude API documentation on 2026-10-03 (environments, permission policies, session budgets, authenticate with vaults, self-hosted sandboxes and its security model) and the prices recorded in `docs/VERSIONS.md` on 2026-10-02. The beta header is `managed-agents-2026-04-01`. The example runs offline: it lints configuration payloads and prices a session with arithmetic, in Python and TypeScript, and sends nothing to the service. Java and Kotlin readers get the same code in the two tabs; the module has no Java or Kotlin practice.

## Why it matters

A hosted agent runs code and can reach the network on its own. The defaults of the product decide whether that is safe, and the exam asks which defaults are safe and which are not: an omitted setting, a permission policy, a budget, a credential. It also asks the architect's question: what do you gain and what do you still owe when the tools run on your own machines?

## The idea

### The cloud environment: packages and the network

"Environments define the sandbox configuration where your agent runs." You create it once and name its ID in each session. Several sessions can share an environment, but "each session gets its own isolated sandbox (a fresh Linux container)", so sessions do not share files. Environments are not versioned, and the page says to keep your own record of changes.

The `packages` field installs packages before the agent starts, through `apt`, `cargo`, `gem`, `go`, `npm` or `pip`, optionally pinned. The `networking` field controls the sandbox's outbound access, and it has two modes.

| Mode | What it allows |
|---|---|
| `limited` | "Restricts sandbox network access to the hosts in `allowed_hosts`", with two switches: `allow_package_managers` and `allow_mcp_servers` |
| `unrestricted` | "Full outbound network access, except for a general safety blocklist" |

Three details cause real failures. First, an omitted field is not neutral: "Set `networking` explicitly in API requests; a create request that omits it gets `unrestricted`." Second, `limited` with packages needs the package switch: "If the environment uses `limited` networking, also set `networking.allow_package_managers` to `true`; otherwise the request is rejected with a 400 error." Third, `allowed_hosts` takes "bare hostnames or wildcard patterns", with no scheme, port or path. An agent that declares an MCP server whose host is not allowed makes session creation fail with a 400 unless `allow_mcp_servers` is on.

Access is granted by host and not by action: "The sandbox can send any request to an allowed host, including uploads such as `git push` and package publishing, with any credential the command supplies." If the agent reads untrusted input, a successful prompt injection could copy files out through an allowed host. The page's advice is to set the `bash` tool's policy to `always_ask` or `auto`. The `web_search` and `web_fetch` tools run on Anthropic's servers, so `networking` does not govern them; you restrict them with `allowed_domains` or `blocked_domains` on the toolset, and an entry cannot set both.

### Permission policies

A policy decides whether a server-executed tool runs on its own. There are three.

| Policy | Behaviour |
|---|---|
| `always_allow` | "The tool executes automatically with no confirmation." |
| `always_ask` | "The session pauses and waits for your approval before executing." |
| `auto` | "The server evaluates each call and runs it, denies it, or pauses for your approval." |

The defaults differ by toolset: "the agent toolset defaults to `always_allow`, and MCP toolsets default to `always_ask`." The reason for the second is stated: it "ensures that new tools added to an MCP server do not execute in your application without approval." Custom tools are outside the system, because "Custom tools are executed by your application and controlled by you." A per-tool `configs` entry overrides the toolset default, so the usual shape is a permissive toolset with `bash` set to `always_ask`.

`auto` evaluates the tool, the input and the session so far. A safe call runs. A high-risk call is denied: the agent gets an error result and "your client cannot override the denial." When the server cannot decide, the session pauses as under `always_ask`. Do not read it as supervision: "`auto` is not a human checkpoint. If the server determines that a call is safe, the call runs before anyone sees it, and its effects might not be reversible." And a trap for products with end users: text you post in `user.message` events counts as your intent, "If you relay untrusted end-user input in `user.message` events, the server reads that input as your intent too." Put `always_ask` on tools that an end user should not run unreviewed.

A pause shows as `session.status_idle` with a stop reason of `requires_action`, and "The session waits indefinitely for a response." You answer with a `user.tool_confirmation` event that allows or denies each blocked call.

### Credentials

Secrets stay out of the sandbox through vaults: you register credentials once and reference them when you create a session. An `environment_variable` credential is stored in the sandbox "as an opaque placeholder", and the real value is substituted when the agent makes an outbound request, so "The agent never sees the secret value." MCP credentials (`mcp_oauth`, `static_bearer`) are injected for the server's URL. The values are write-only. Vaults are workspace-scoped, so any key with workspace access can reference them, and environment-variable credentials are not yet supported with self-hosted sandboxes.

### Cost and the session budget

A session budget is "an optional hard spend ceiling you set when you create a session." The platform prices what the session consumes at public list rates, and the total is its list cost: model tokens at each model's list price, web searches at $10 per 1,000, and "Session running time, at $0.08 per hour". Your billed price can be lower, because "List cost is not your contracted price."

The amount is whole US cents written as a string: `"125"` is $1.25, and `"25.00"` is rejected so that no float rounding applies. A budget can be set only when the session is created, and it can later be changed or removed. The cap is checked between requests, so it bounds new work and not the exact total: "The request in flight when the cap is crossed still finishes." A session at its cap does not die. It goes idle with a stop reason of `budget_reached`, history and sandbox kept, and it accepts only events that settle work in progress (tool results and confirmations, and `user.interrupt`). A `user.message` is rejected with a 400. Raising the cap above the consumed list cost resumes it.

### Self-hosted sandboxes

By default tools run in Anthropic's sandboxes. A `self_hosted` environment moves them: "Self-hosted sandboxes keep the orchestration on Anthropic's side but move tool execution into infrastructure you control." The environment acts as a work queue. You run an environment worker, a process on your own infrastructure that claims a session's work item, downloads the agent's skills and memory stores, and runs each tool call locally while Claude runs on Anthropic's side. The worker needs only outbound HTTPS. A session with no worker connected "stays queued rather than failing."

What moves is execution: files, processes and network traffic stay in your environment. What does not move is the model's view: "Tool inputs and outputs still flow to Anthropic's control plane, so the model can see results and determine what to do next." Skills and memory stores are stored by Anthropic and copied to your sandbox. Self-hosted sessions accept `memory_store` resources only; a `file` or `github_repository` resource is rejected with a 400. The reasons to choose it are data that cannot leave your network, internal services that are not publicly routable, and your own compliance and audit controls.

The security model is shared, and the duties are specific. Anthropic secures the control plane. You own the sandbox image ("Anthropic does not inspect or verify your sandbox image"), network egress ("Without egress restrictions, a compromised tool execution can reach arbitrary external hosts"), storage and rotation of the environment key (a secrets manager, never an image), one environment per trust boundary when you run untrusted code, per-session credentials, the blast radius of the tool process, and the retention of session content that passes through your worker. Anthropic "cannot know your key was compromised". Self-hosting controls where code runs, and MCP tunnels, a separate feature, control how Anthropic reaches MCP servers in your network.

### Choosing

| Question | Cloud environment | Self-hosted sandbox | Agent SDK in your process |
|---|---|---|---|
| Who runs the loop | Anthropic | Anthropic | You, in a child process |
| Where tools run | Anthropic's sandbox | Your infrastructure | Your machine |
| Who hardens the sandbox | Anthropic | You | You |
| Where the session is stored | Anthropic | Anthropic (tool I/O passes through your worker) | You decide |
| Zero Data Retention | Not eligible | Not eligible (the product as a whole is not) | Not covered on these pages |

<!-- example: m36-managed-agent-budget tabs: python,typescript,java,kotlin -->
```python
"""Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.

Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
"""
import logging

log = logging.getLogger(__name__)

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
```
```text
environment findings:
 - allowed_hosts entry 'https://api.example.com': use a bare hostname, no scheme, port or path
 - packages with limited networking need allow_package_managers: true, or the request is rejected (400)
 - MCP host mcp.example.com is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)
omitted networking: networking omitted: a create request that omits it gets unrestricted
agent findings, unrestricted sandbox:
 - bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking
 - mcp toolset github: always_allow lets new tools of that server run unreviewed
agent findings, limited sandbox: ['mcp toolset github: always_allow lets new tools of that server run unreviewed']
self-hosted with a file resource: ['self-hosted sandboxes reject file resources (400)']

claude-opus-5-5: list cost 804 cents; budget 800 -> budget_reached; budget 1000 -> running, 196 cents left
claude-sonnet-5-5: list cost 414 cents; budget 800 -> running, 386 cents left; budget 1000 -> running, 586 cents left
budget '25.00': amount '25.00' is rejected: write whole cents as a string with no leading zeros | budget '050': amount '050' is rejected: write whole cents as a string with no leading zeros | budget '125': ok
```
```typescript
// Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.
//
// Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
// tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
// API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
// 2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
import { logger } from "./logger.ts";
const log = logger("managed_agent");

type Json = Record<string, any>;

// --- configuration checks ---------------------------------------------------------------------------------------------------

/** Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding. */
export function checkEnvironment(env: Json, agentMcpHosts: string[] = []): string[] {
  const findings: string[] = [];
  if (env.type === "self_hosted") return findings;
  const net = env.networking;
  if (net === undefined) return ["networking omitted: a create request that omits it gets unrestricted"];
  if (net.type === "unrestricted") return ["unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox"];
  for (const host of net.allowed_hosts ?? []) {
    if (host.includes("://") || host.includes("/") || host.includes(":")) findings.push(`allowed_hosts entry '${host}': use a bare hostname, no scheme, port or path`);
  }
  if (env.packages && Object.keys(env.packages).length && !net.allow_package_managers) {
    findings.push("packages with limited networking need allow_package_managers: true, or the request is rejected (400)");
  }
  const hosts = new Set<string>(net.allowed_hosts ?? []);
  for (const host of agentMcpHosts) {
    if (!hosts.has(host) && !net.allow_mcp_servers) findings.push(`MCP host ${host} is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)`);
  }
  return findings;
}

/** Findings for an agent payload read together with the environment it will run in. */
export function checkAgent(agent: Json, env: Json): string[] {
  const findings: string[] = [];
  for (const tool of agent.tools ?? []) {
    if (tool.type === "agent_toolset_20260401") {
      const policy = (tool.default_config?.permission_policy ?? { type: "always_allow" }).type;
      const overrides: Record<string, string | undefined> = {};
      for (const c of tool.configs ?? []) overrides[c.name] = c.permission_policy?.type;
      const bash = overrides.bash ?? policy;
      const reach = (env.networking ?? { type: "unrestricted" }).type !== "limited";
      if (bash === "always_allow" && reach) findings.push("bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking");
      for (const c of tool.configs ?? []) {
        if (c.allowed_domains?.length && c.blocked_domains?.length) findings.push(`${c.name}: allowed_domains and blocked_domains cannot be combined`);
      }
    }
    if (tool.type === "mcp_toolset") {
      const policy = (tool.default_config?.permission_policy ?? { type: "always_ask" }).type;
      if (policy === "always_allow") findings.push(`mcp toolset ${tool.mcp_server_name}: always_allow lets new tools of that server run unreviewed`);
    }
  }
  return findings;
}

/** A self-hosted sandbox accepts memory_store resources only. */
export function checkSessionResources(env: Json, resources: Json[]): string[] {
  if (env.type !== "self_hosted") return [];
  const bad = [...new Set(resources.map((r) => r.type).filter((t) => t !== "memory_store"))].sort();
  return bad.map((t) => `self-hosted sandboxes reject ${t} resources (400)`);
}

// --- list cost and budget ---------------------------------------------------------------------------------------------------

const PRICES: Record<string, [number, number]> = { "claude-opus-5-5": [4, 20], "claude-sonnet-5-5": [2, 10] }; // dollars per million input and output tokens
const WEB_SEARCH_MICRO = 10_000; // $10 per 1,000 searches, in millionths of a dollar per search
const RUNNING_MICRO_PER_HOUR = 80_000; // $0.08 per hour of session running time

/** The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it. */
export function listCostCents(model: string, inputTokens: number, outputTokens: number, searches: number, activeSeconds: number): number {
  const [priceIn, priceOut] = PRICES[model];
  const micro = inputTokens * priceIn + outputTokens * priceOut + searches * WEB_SEARCH_MICRO + Math.floor((activeSeconds * RUNNING_MICRO_PER_HOUR) / 3600);
  return Math.floor((micro + 5_000) / 10_000);
}

/** max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero. */
export function checkBudget(amount: string): string {
  if (!(/^\d+$/.test(amount) && !amount.startsWith("0"))) return `amount '${amount}' is rejected: write whole cents as a string with no leading zeros`;
  return "ok";
}

/** At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes. */
export function budgetState(costCents: number, amount: string): string {
  const cap = Number(amount);
  return costCents >= cap ? "budget_reached" : `running, ${cap - costCents} cents left`;
}

function main() {
  const env = { type: "cloud", packages: { pip: ["sqlalchemy==2.0.30"] }, networking: { type: "limited", allowed_hosts: ["https://api.example.com"] } };
  const agent = { tools: [{ type: "agent_toolset_20260401" }, { type: "mcp_toolset", mcp_server_name: "github", default_config: { permission_policy: { type: "always_allow" } } }] };
  console.log("environment findings:");
  for (const line of checkEnvironment(env, ["mcp.example.com"])) console.log(" -", line);
  console.log("omitted networking:", checkEnvironment({ type: "cloud" })[0]);
  console.log("agent findings, unrestricted sandbox:");
  for (const line of checkAgent(agent, { type: "cloud" })) console.log(" -", line);
  console.log("agent findings, limited sandbox:", pyList(checkAgent(agent, { type: "cloud", networking: { type: "limited" } })));
  console.log("self-hosted with a file resource:", pyList(checkSessionResources({ type: "self_hosted" }, [{ type: "file" }, { type: "memory_store" }])));
  console.log();
  for (const model of ["claude-opus-5-5", "claude-sonnet-5-5"]) {
    const cents = listCostCents(model, 1_200_000, 150_000, 8, 7200);
    console.log(`${model}: list cost ${cents} cents; budget 800 -> ${budgetState(cents, "800")}; budget 1000 -> ${budgetState(cents, "1000")}`);
  }
  console.log("budget '25.00':", checkBudget("25.00"), "| budget '050':", checkBudget("050"), "| budget '125':", checkBudget("125"));
}

/** A list printed the way Python prints it, so both languages show the same output. */
function pyList(items: string[]): string {
  return `[${items.map((s) => (s.includes("'") ? `"${s}"` : `'${s}'`)).join(", ")}]`;
}

if (import.meta.main) main();
```
```text
environment findings:
 - allowed_hosts entry 'https://api.example.com': use a bare hostname, no scheme, port or path
 - packages with limited networking need allow_package_managers: true, or the request is rejected (400)
 - MCP host mcp.example.com is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)
omitted networking: networking omitted: a create request that omits it gets unrestricted
agent findings, unrestricted sandbox:
 - bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking
 - mcp toolset github: always_allow lets new tools of that server run unreviewed
agent findings, limited sandbox: ['mcp toolset github: always_allow lets new tools of that server run unreviewed']
self-hosted with a file resource: ['self-hosted sandboxes reject file resources (400)']

claude-opus-5-5: list cost 804 cents; budget 800 -> budget_reached; budget 1000 -> running, 196 cents left
claude-sonnet-5-5: list cost 414 cents; budget 800 -> running, 386 cents left; budget 1000 -> running, 586 cents left
budget '25.00': amount '25.00' is rejected: write whole cents as a string with no leading zeros | budget '050': amount '050' is rejected: write whole cents as a string with no leading zeros | budget '125': ok
```
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.
 *
 * <p>Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
 * tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
 * API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
 * 2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
 * The payloads are JSON objects read into maps, as the API would receive them.
 */
public final class ManagedAgent {
    private static final System.Logger LOG = System.getLogger(ManagedAgent.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper();

    /** A JSON object as a map. */
    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(String json) {
        try {
            return JSON.readValue(json, LinkedHashMap.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object o) {
        return o instanceof List<?> l ? (List<Object>) l : List.of();
    }

    /** Python's notion of an empty or missing value. */
    private static boolean truthy(Object o) {
        if (o == null) return false;
        if (o instanceof Boolean b) return b;
        if (o instanceof Map<?, ?> m) return !m.isEmpty();
        if (o instanceof List<?> l) return !l.isEmpty();
        return true;
    }

    // --- configuration checks -------------------------------------------------------------------------------------------

    /** Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding. */
    static List<String> checkEnvironment(Map<String, Object> env, List<String> agentMcpHosts) {
        List<String> findings = new ArrayList<>();
        if ("self_hosted".equals(env.get("type"))) return findings;
        if (env.get("networking") == null) {
            findings.add("networking omitted: a create request that omits it gets unrestricted");
            return findings;
        }
        Map<String, Object> net = map(env.get("networking"));
        if ("unrestricted".equals(net.get("type"))) {
            findings.add("unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox");
            return findings;
        }
        for (Object host : list(net.get("allowed_hosts"))) {
            String h = (String) host;
            if (h.contains("://") || h.contains("/") || h.contains(":")) findings.add("allowed_hosts entry " + py(h) + ": use a bare hostname, no scheme, port or path");
        }
        if (truthy(env.get("packages")) && !truthy(net.get("allow_package_managers"))) {
            findings.add("packages with limited networking need allow_package_managers: true, or the request is rejected (400)");
        }
        Set<Object> hosts = Set.copyOf(list(net.get("allowed_hosts")));
        for (String host : agentMcpHosts) {
            if (!hosts.contains(host) && !truthy(net.get("allow_mcp_servers"))) {
                findings.add("MCP host " + host + " is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)");
            }
        }
        return findings;
    }

    private static String policyType(Object config, String fallback) {
        Object policy = map(config).get("permission_policy");
        return policy == null ? fallback : (String) map(policy).get("type");
    }

    /** Findings for an agent payload read together with the environment it will run in. */
    static List<String> checkAgent(Map<String, Object> agent, Map<String, Object> env) {
        List<String> findings = new ArrayList<>();
        for (Object t : list(agent.get("tools"))) {
            Map<String, Object> tool = map(t);
            if ("agent_toolset_20260401".equals(tool.get("type"))) {
                String policy = policyType(tool.get("default_config"), "always_allow");
                Map<String, String> overrides = new LinkedHashMap<>();
                for (Object c : list(tool.get("configs"))) overrides.put((String) map(c).get("name"), policyType(c, null));
                String bash = overrides.containsKey("bash") ? overrides.get("bash") : policy;
                Object networking = env.get("networking");
                boolean reach = !"limited".equals(networking == null ? "unrestricted" : map(networking).get("type"));
                if ("always_allow".equals(bash) && reach) {
                    findings.add("bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking");
                }
                for (Object c : list(tool.get("configs"))) {
                    if (truthy(map(c).get("allowed_domains")) && truthy(map(c).get("blocked_domains"))) {
                        findings.add(map(c).get("name") + ": allowed_domains and blocked_domains cannot be combined");
                    }
                }
            }
            if ("mcp_toolset".equals(tool.get("type"))) {
                if ("always_allow".equals(policyType(tool.get("default_config"), "always_ask"))) {
                    findings.add("mcp toolset " + tool.get("mcp_server_name") + ": always_allow lets new tools of that server run unreviewed");
                }
            }
        }
        return findings;
    }

    /** A self-hosted sandbox accepts memory_store resources only. */
    static List<String> checkSessionResources(Map<String, Object> env, List<Map<String, Object>> resources) {
        if ("self_hosted".equals(env.get("type"))) {
            Set<String> bad = new TreeSet<>();
            for (Map<String, Object> r : resources) if (!"memory_store".equals(r.get("type"))) bad.add((String) r.get("type"));
            return bad.stream().map(t -> "self-hosted sandboxes reject " + t + " resources (400)").toList();
        }
        return List.of();
    }

    // --- list cost and budget -------------------------------------------------------------------------------------------

    /** Dollars per million input and output tokens. */
    static final Map<String, long[]> PRICES = Map.of("claude-opus-5-5", new long[] {4, 20}, "claude-sonnet-5-5", new long[] {2, 10});
    static final long WEB_SEARCH_MICRO = 10_000; // $10 per 1,000 searches, in millionths of a dollar per search
    static final long RUNNING_MICRO_PER_HOUR = 80_000; // $0.08 per hour of session running time

    /** The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it. */
    static long listCostCents(String model, long inputTokens, long outputTokens, long searches, long activeSeconds) {
        long[] price = PRICES.get(model);
        long micro = inputTokens * price[0] + outputTokens * price[1] + searches * WEB_SEARCH_MICRO + activeSeconds * RUNNING_MICRO_PER_HOUR / 3600;
        return (micro + 5_000) / 10_000;
    }

    /** max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero. */
    static String checkBudget(String amount) {
        if (!(!amount.isEmpty() && amount.chars().allMatch(c -> c >= '0' && c <= '9') && !amount.startsWith("0"))) {
            return "amount " + py(amount) + " is rejected: write whole cents as a string with no leading zeros";
        }
        return "ok";
    }

    /** At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes. */
    static String budgetState(long costCents, String amount) {
        long cap = Long.parseLong(amount);
        return costCents >= cap ? "budget_reached" : "running, " + (cap - costCents) + " cents left";
    }

    /** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
    static String py(Object v) {
        if (v instanceof List<?> l) return l.stream().map(ManagedAgent::py).collect(Collectors.joining(", ", "[", "]"));
        String s = String.valueOf(v);
        String quote = s.contains("'") && !s.contains("\"") ? "\"" : "'";
        return quote + s.replace("\\", "\\\\").replace("\n", "\\n").replace(quote, "\\" + quote) + quote;
    }

    public static void main(String[] args) {
        Map<String, Object> env = obj("""
            {"type": "cloud", "packages": {"pip": ["sqlalchemy==2.0.30"]}, "networking": {"type": "limited", "allowed_hosts": ["https://api.example.com"]}}""");
        Map<String, Object> agent = obj("""
            {"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}""");
        System.out.println("environment findings:");
        for (String line : checkEnvironment(env, List.of("mcp.example.com"))) System.out.println(" - " + line);
        System.out.println("omitted networking: " + checkEnvironment(obj("{\"type\": \"cloud\"}"), List.of()).get(0));
        System.out.println("agent findings, unrestricted sandbox:");
        for (String line : checkAgent(agent, obj("{\"type\": \"cloud\"}"))) System.out.println(" - " + line);
        System.out.println("agent findings, limited sandbox: " + py(checkAgent(agent, obj("{\"type\": \"cloud\", \"networking\": {\"type\": \"limited\"}}"))));
        System.out.println("self-hosted with a file resource: " + py(checkSessionResources(obj("{\"type\": \"self_hosted\"}"), List.of(obj("{\"type\": \"file\"}"), obj("{\"type\": \"memory_store\"}")))));
        System.out.println();
        for (String model : List.of("claude-opus-5-5", "claude-sonnet-5-5")) {
            long cents = listCostCents(model, 1_200_000, 150_000, 8, 7200);
            System.out.println(model + ": list cost " + cents + " cents; budget 800 -> " + budgetState(cents, "800") + "; budget 1000 -> " + budgetState(cents, "1000"));
        }
        System.out.println("budget '25.00': " + checkBudget("25.00") + " | budget '050': " + checkBudget("050") + " | budget '125': " + checkBudget("125"));
    }
}
```
```text
environment findings:
 - allowed_hosts entry 'https://api.example.com': use a bare hostname, no scheme, port or path
 - packages with limited networking need allow_package_managers: true, or the request is rejected (400)
 - MCP host mcp.example.com is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)
omitted networking: networking omitted: a create request that omits it gets unrestricted
agent findings, unrestricted sandbox:
 - bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking
 - mcp toolset github: always_allow lets new tools of that server run unreviewed
agent findings, limited sandbox: ['mcp toolset github: always_allow lets new tools of that server run unreviewed']
self-hosted with a file resource: ['self-hosted sandboxes reject file resources (400)']

claude-opus-5-5: list cost 804 cents; budget 800 -> budget_reached; budget 1000 -> running, 196 cents left
claude-sonnet-5-5: list cost 414 cents; budget 800 -> running, 386 cents left; budget 1000 -> running, 586 cents left
budget '25.00': amount '25.00' is rejected: write whole cents as a string with no leading zeros | budget '050': amount '050' is rejected: write whole cents as a string with no leading zeros | budget '125': ok
```
```kotlin
import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("managed_agent")

/**
 * Managed Agents, checked offline: lint the configuration you would send, and price a session against its budget.
 *
 * Claude Managed Agents is a hosted agent harness: you create an agent (model, system prompt, tools), an environment (where the
 * tools run: an Anthropic-managed cloud sandbox or a self-hosted one) and a session, then exchange events. Nothing here calls the
 * API. The rules below are the ones the documentation states for environments, permission policies and session budgets, read on
 * 2026-10-03 (beta header managed-agents-2026-04-01); the prices are the list prices recorded in docs/VERSIONS.md on 2026-10-02.
 * The payloads are JSON objects read into maps, as the API would receive them.
 */
typealias Json = Map<String, Any?>

/** A JSON object as a map. */
@Suppress("UNCHECKED_CAST")
fun obj(json: String): Json = ObjectMapper().readValue(json, LinkedHashMap::class.java) as Json

@Suppress("UNCHECKED_CAST")
private fun map(o: Any?): Json = o as? Json ?: emptyMap()

private fun list(o: Any?): List<Any?> = o as? List<*> ?: emptyList<Any?>()

/** Python's notion of an empty or missing value. */
private fun truthy(o: Any?): Boolean = when (o) {
    null -> false
    is Boolean -> o
    is Map<*, *> -> o.isNotEmpty()
    is List<*> -> o.isNotEmpty()
    else -> true
}

// --- configuration checks -----------------------------------------------------------------------------------------------

/** Findings for an environment payload. An omitted networking field becomes `unrestricted` on the API, so it is a finding. */
fun checkEnvironment(env: Json, agentMcpHosts: List<String> = emptyList()): List<String> {
    val findings = mutableListOf<String>()
    if (env["type"] == "self_hosted") return findings
    val net = env["networking"]?.let { map(it) }
    if (net == null) {
        findings += "networking omitted: a create request that omits it gets unrestricted"
        return findings
    }
    if (net["type"] == "unrestricted") {
        findings += "unrestricted networking: any host except a safety blocklist; keep secrets out of the sandbox"
        return findings
    }
    for (host in list(net["allowed_hosts"]).map { it as String }) {
        if ("://" in host || "/" in host || ":" in host) findings += "allowed_hosts entry ${py(host)}: use a bare hostname, no scheme, port or path"
    }
    if (truthy(env["packages"]) && !truthy(net["allow_package_managers"])) {
        findings += "packages with limited networking need allow_package_managers: true, or the request is rejected (400)"
    }
    val hosts = list(net["allowed_hosts"]).toSet()
    for (host in agentMcpHosts) {
        if (host !in hosts && !truthy(net["allow_mcp_servers"])) {
            findings += "MCP host $host is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)"
        }
    }
    return findings
}

private fun policyType(config: Any?, fallback: String?): String? = map(config)["permission_policy"]?.let { map(it)["type"] as String? } ?: fallback

/** Findings for an agent payload read together with the environment it will run in. */
fun checkAgent(agent: Json, env: Json): List<String> {
    val findings = mutableListOf<String>()
    for (tool in list(agent["tools"]).map { map(it) }) {
        if (tool["type"] == "agent_toolset_20260401") {
            val policy = policyType(tool["default_config"], "always_allow")
            val overrides = list(tool["configs"]).associate { map(it)["name"] as String to policyType(it, null) }
            val bash = if ("bash" in overrides) overrides["bash"] else policy
            val reach = (env["networking"]?.let { map(it)["type"] } ?: "unrestricted") != "limited"
            if (bash == "always_allow" && reach) {
                findings += "bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking"
            }
            for (c in list(tool["configs"]).map { map(it) }) {
                if (truthy(c["allowed_domains"]) && truthy(c["blocked_domains"])) findings += "${c["name"]}: allowed_domains and blocked_domains cannot be combined"
            }
        }
        if (tool["type"] == "mcp_toolset" && policyType(tool["default_config"], "always_ask") == "always_allow") {
            findings += "mcp toolset ${tool["mcp_server_name"]}: always_allow lets new tools of that server run unreviewed"
        }
    }
    return findings
}

/** A self-hosted sandbox accepts memory_store resources only. */
fun checkSessionResources(env: Json, resources: List<Json>): List<String> {
    if (env["type"] != "self_hosted") return emptyList()
    return resources.map { it["type"] as String }.filter { it != "memory_store" }.toSortedSet().map { "self-hosted sandboxes reject $it resources (400)" }
}

// --- list cost and budget -----------------------------------------------------------------------------------------------

/** Dollars per million input and output tokens. */
val PRICES = mapOf("claude-opus-5-5" to (4L to 20L), "claude-sonnet-5-5" to (2L to 10L))
const val WEB_SEARCH_MICRO = 10_000L // $10 per 1,000 searches, in millionths of a dollar per search
const val RUNNING_MICRO_PER_HOUR = 80_000L // $0.08 per hour of session running time

/** The session's list cost in whole cents, rounded to the nearest cent, as the platform reports it. */
fun listCostCents(model: String, inputTokens: Long, outputTokens: Long, searches: Long, activeSeconds: Long): Long {
    val (priceIn, priceOut) = PRICES.getValue(model)
    val micro = inputTokens * priceIn + outputTokens * priceOut + searches * WEB_SEARCH_MICRO + activeSeconds * RUNNING_MICRO_PER_HOUR / 3600
    return (micro + 5_000) / 10_000
}

/** max_list_cost.amount is a whole number of cents as a string, no leading zeros, greater than zero. */
fun checkBudget(amount: String): String =
    if (!(amount.isNotEmpty() && amount.all { it in '0'..'9' } && !amount.startsWith("0"))) "amount ${py(amount)} is rejected: write whole cents as a string with no leading zeros" else "ok"

/** At or past the cap the session goes idle with stop_reason budget_reached; the request in flight still finishes. */
fun budgetState(costCents: Long, amount: String): String {
    val cap = amount.toLong()
    return if (costCents >= cap) "budget_reached" else "running, ${cap - costCents} cents left"
}

/** Python's repr of strings and lists of strings, so every language of the course prints the same text. */
fun py(v: Any?): String {
    if (v is List<*>) return v.joinToString(", ", "[", "]") { py(it) }
    val s = v.toString()
    val q = if ("'" in s && "\"" !in s) "\"" else "'"
    return q + s.replace("\\", "\\\\").replace("\n", "\\n").replace(q, "\\" + q) + q
}

fun main() {
    val env = obj("""{"type": "cloud", "packages": {"pip": ["sqlalchemy==2.0.30"]}, "networking": {"type": "limited", "allowed_hosts": ["https://api.example.com"]}}""")
    val agent = obj("""{"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}""")
    println("environment findings:")
    for (line in checkEnvironment(env, listOf("mcp.example.com"))) println(" - $line")
    println("omitted networking: ${checkEnvironment(obj("""{"type": "cloud"}"""))[0]}")
    println("agent findings, unrestricted sandbox:")
    for (line in checkAgent(agent, obj("""{"type": "cloud"}"""))) println(" - $line")
    println("agent findings, limited sandbox: ${py(checkAgent(agent, obj("""{"type": "cloud", "networking": {"type": "limited"}}""")))}")
    println("self-hosted with a file resource: ${py(checkSessionResources(obj("""{"type": "self_hosted"}"""), listOf(obj("""{"type": "file"}"""), obj("""{"type": "memory_store"}"""))))}")
    println()
    for (model in listOf("claude-opus-5-5", "claude-sonnet-5-5")) {
        val cents = listCostCents(model, 1_200_000, 150_000, 8, 7200)
        println("$model: list cost $cents cents; budget 800 -> ${budgetState(cents, "800")}; budget 1000 -> ${budgetState(cents, "1000")}")
    }
    println("budget '25.00': ${checkBudget("25.00")} | budget '050': ${checkBudget("050")} | budget '125': ${checkBudget("125")}")
}
```
```text
environment findings:
 - allowed_hosts entry 'https://api.example.com': use a bare hostname, no scheme, port or path
 - packages with limited networking need allow_package_managers: true, or the request is rejected (400)
 - MCP host mcp.example.com is not reachable: add it to allowed_hosts or set allow_mcp_servers, or session creation fails (400)
omitted networking: networking omitted: a create request that omits it gets unrestricted
agent findings, unrestricted sandbox:
 - bash runs without approval and the sandbox can reach any host: set bash to always_ask or auto, or use limited networking
 - mcp toolset github: always_allow lets new tools of that server run unreviewed
agent findings, limited sandbox: ['mcp toolset github: always_allow lets new tools of that server run unreviewed']
self-hosted with a file resource: ['self-hosted sandboxes reject file resources (400)']

claude-opus-5-5: list cost 804 cents; budget 800 -> budget_reached; budget 1000 -> running, 196 cents left
claude-sonnet-5-5: list cost 414 cents; budget 800 -> running, 386 cents left; budget 1000 -> running, 586 cents left
budget '25.00': amount '25.00' is rejected: write whole cents as a string with no leading zeros | budget '050': amount '050' is rejected: write whole cents as a string with no leading zeros | budget '125': ok
```
<!-- /example -->

The example is two lint functions and a price. `check_environment` flags the three failures above, and an agent that declares an MCP host the sandbox cannot reach. `check_agent` reads the agent together with its environment and flags `bash` running without approval on an open network, and an MCP toolset set to `always_allow`. `list_cost_cents` adds tokens, searches and running time, and `budget_state` compares the total with the cap. In the output, a two-hour session on Claude Opus 5.5 with 1.2 million input tokens, 150,000 output tokens and eight searches costs 804 cents, so a cap of `"800"` has been reached while a cap of `"1000"` leaves 196 cents. The same session on Claude Sonnet 5.5 costs 414 cents. Both languages print the same text.

## Traps

1. **Omitting `networking`.** The API reads the omission as `unrestricted`. Set it, and list the hosts.
2. **Calling `auto` a review step.** It runs what it judges safe before anyone sees it. A person-in-the-loop requirement needs `always_ask`.
3. **Treating the budget as an exact stop.** The request in flight finishes, so the total can land a fraction past the cap. Leave room for one request.
4. **Believing self-hosting keeps the data with you.** Tools run in your network, but their inputs and outputs pass to the control plane, and the sandbox, the egress rules and the key are yours to secure.

## Quiz

1. A team creates an environment with `packages` and `limited` networking and lists the registries in `allowed_hosts`, but sets nothing else. What happens?
   - **a**: The packages install, since the hosts are on the allowed list
   - **b**: The request is rejected until the registry flag is true
   - **c**: The environment is created with unrestricted networking instead
   - **d**: The packages install after a warning that the agent can ignore

2. An agent keeps the default toolset policy and the sandbox has unrestricted networking. What should the team change first?
   - **a**: Move the agent to a model with a smaller context window
   - **b**: Switch the toolset to custom tools so that no policy applies
   - **c**: Add a longer system prompt that forbids any outbound request
   - **d**: Set bash to `always_ask` and name the allowed hosts

3. A team sets a session budget of `"500"`. The session pauses with a list cost of 503 cents. What does this show?
   - **a**: A billing error, because the platform must stop exactly at the cap
   - **b**: Expected behaviour, because the request in flight finishes
   - **c**: A failed enforcement, because the cap is checked mid-request
   - **d**: A crash, because a session cannot outlive its budget

<details>
<summary>Answer key</summary>

1. **b**. The page says "If the environment uses `limited` networking, also set `networking.allow_package_managers` to `true`; otherwise the request is rejected with a 400 error." *a* is ruled out because the page requires "also set `networking.allow_package_managers` to `true`", and listing hosts alone does not open the registries. *c* is ruled out because only "a create request that omits it gets `unrestricted`", and here the field is set to `limited`. *d* is ruled out because the page names a rejection and not a warning: "otherwise the request is rejected with a 400 error."
2. **d**. The page advises "to set the `bash` tool's permission policy to `always_ask` or `auto`", and to prefer `limited` networking with an explicit host list. *a* is ruled out because "Access is granted by host and not by action", so a smaller context window changes neither. *c* is ruled out because the page's advice is to "set the `bash` tool's policy to `always_ask` or `auto`", and a prompt is not a policy. *b* is ruled out because "Custom tools are executed by your application and controlled by you", so they would move the risk into your code and not remove it.
3. **b**. The page says "The request in flight when the cap is crossed still finishes", so a paused session can read a fraction past the cap. *a* is ruled out because "it bounds new work and not the exact total", so the platform is not required to stop at the cap. *d* is ruled out because "A session at its cap does not die", and its history and sandbox are kept. *c* is ruled out because "The cap is checked between requests, so it bounds new work", which is the designed behaviour and not a failure.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A hospital group must keep patient files inside its own network but wants Anthropic to run the agent loop. Which choice fits, and what must the group still do?
   - **a**: A cloud environment with `unrestricted` networking and a strict system prompt
   - **b**: A self-hosted sandbox, securing the image and the egress rules itself
   - **c**: The Messages API with the files pasted into the system prompt of each call
   - **d**: A cloud environment, since Anthropic hardens it and nothing else is needed

2. A developer migrates an Agent SDK service whose `can_use_tool` callback approved reads and refused pushes. What replaces that callback in Managed Agents?
   - **a**: A per-tool permission policy, with confirmations sent as events
   - **b**: The `ClaudeAgentOptions` object, created again for every run
   - **c**: The environment's `networking` field, which approves each command
   - **d**: A model override on the session that removes the tool

3. A product relays customers' messages to an agent as `user.message` events and sets the toolset to `auto`. What is the risk?
   - **a**: Their text counts as the operator's own intent when a call is judged
   - **b**: The server ignores `user.message` events once a policy is set
   - **c**: Every call pauses until a person has confirmed it
   - **d**: A denied call can be overridden by the client with a confirmation

4. A finance team asks for the cost of one long session before approving a budget. Which items does the list cost include?
   - **a**: Model tokens and the storage of the event history afterward
   - **b**: Model tokens only, since the sandbox is free to run
   - **c**: Model tokens, web searches and the running time
   - **d**: Model tokens and every package the environment installs

<details>
<summary>Answer key</summary>

1. **b**. The page says self-hosting moves execution into your infrastructure while "keep the orchestration on Anthropic's side", and that "Without egress restrictions, a compromised tool execution can reach arbitrary external hosts", so the group owns image and egress. *a* is ruled out because "The sandbox can send any request to an allowed host", and `unrestricted` reaches any host, which is the opposite of keeping files inside. *c* is ruled out because the Messages API is the choice for "Custom agent loops and fine-grained control", and pasting files sends them to Anthropic on every call. *d* is ruled out because a cloud sandbox runs in Anthropic's infrastructure, where the files would be stored, and the first page lists "Self-hosted execution for compliance" as its own reason to choose another option.
2. **a**. The first page says `permission_mode` and `can_use_tool` "become a per-tool permission policy", and a paused call is answered with a `user.tool_confirmation` event. *b* is ruled out because the migration "becomes an agent created once" from that object. *c* is ruled out because `networking` controls the sandbox's outbound traffic, and "so `networking` does not govern them" for the search and fetch tools. *d* is ruled out because an override "must list every tool the session should have", and removing a tool is not an approval step.
3. **a**. The page says "If you relay untrusted end-user input in `user.message` events, the server reads that input as your intent too." *b* is ruled out because what you post in `user.message` events "counts as your intent", so the server reads it. *c* is ruled out because "`auto` is not a human checkpoint", and a safe call "runs before anyone sees it". *d* is ruled out because a denied call is final: "your client cannot override the denial."
4. **c**. The page prices "Model tokens, at each served model's list price", web searches at $10 per 1,000 and "Session running time, at $0.08 per hour". *b* is ruled out because running time is priced: "Session running time, at $0.08 per hour". *a* is ruled out because the page prices "model tokens at each model's list price", web searches and running time, and no storage. *d* is ruled out because the page names no charge for packages, and prices "what the session consumes" only.

</details>
