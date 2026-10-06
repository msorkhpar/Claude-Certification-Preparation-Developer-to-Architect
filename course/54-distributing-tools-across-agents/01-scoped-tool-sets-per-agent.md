# Scoped tool sets: which agent gets which tool

**Level:** Architect · **Module 54:** Distributing tools across agents · **Page 1 of 2**
**Exams:** A2.3; S3

**After this page you can** explain why an agent with too many tools chooses worse and why an agent with tools outside its role misuses them, give each agent in a team only the tools its role needs, add one narrow cross-role tool for a frequent need while routing the hard cases through the coordinator, replace a general tool with a constrained one, and say what the SDK's `tools`, `disallowedTools` and `allowedTools` options each do.

Checked on 2026-10-03 against the Claude Code documentation pages "Configure permissions" and "Subagents in the SDK" of the Agent SDK, "Scale to many tools with tool search", and the exam guide for the Architect Foundations exam (version 1.0, July 2026): Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287, the versions the course ran. The example is plain code with scripted decisions: no model was called, no network was used and no API key was involved. This page deepens module 47 (an agent definition lists the tools a subagent has) and module 46 (the coordinator hands out the work), and it does not repeat them; it is about the decision that comes before the list is written.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* giving an agent too many tools (the guide's example is 18 instead of 4 or 5) makes its choice among them less reliable; an agent whose tools lie outside its specialisation tends to misuse them (a synthesis agent that tries web searches); the answer is scoped access, each subagent restricted to the tools its role needs, with a limited cross-role tool for a frequent need (a `verify_fact` tool for the synthesis agent) and the coordinator for the complex cases; and a general tool can be replaced by a constrained one (a `load_document` that validates document URLs in place of `fetch_url`). *What the current product does (documentation checked 2026-10-03):* the documentation gives a number of its own for the first point, "Tool selection accuracy degrades with more than 30-50 tools loaded at once", and answers it with tool search, which loads definitions on demand (module 52 covers it); the guide's 4 or 5 is the size of a good role, and 30 to 50 is where the documentation sees the decline. The restriction is real in the SDK, but it is not where the name suggests: an agent definition's `tools` field is the list of tools a subagent has and, if it is omitted, the subagent "inherits every tool available to subagents"; a bare tool name in `disallowedTools` removes the tool from the agent's context; and `allowedTools` only approves, so "Any other tool not listed in `allowed_tools` is still available to Claude". On the exam, choose per-agent tool lists and narrow tools over prompts and routing layers; in your own code, restrict with the definition's `tools` list or with `disallowedTools`, never with `allowedTools`.

## Why it matters

A research system has four subagents: one searches the web, one reads documents, one writes the synthesis, one formats the report. The first version of the code builds one tool list of 18 entries and gives it to all four, because that is the least code. The synthesis agent, whose job is to combine what the others found, starts running web searches of its own. The search agent calls the formatting tool. The document agent, offered three tools that all read files, picks the wrong one a third of the time. Nothing is broken: every agent is doing what its tool list invites. The exam asks what a team should have done, and the answer is in the way the tools were handed out, which is a design decision that costs no code.

## The idea

### More tools, worse choices

A tool list is part of the prompt, and the model chooses from it every turn by reading the names and descriptions. Two things go wrong as it grows. The choice becomes harder, because there are more neighbours to confuse: the documentation puts the point where accuracy declines at "more than 30-50 tools loaded at once", and the guide's example of 18 against 4 or 5 makes the same point from the other side. And the list costs context on every turn, since the definitions are sent each time. A smaller list is both cheaper and easier to choose from.

The tool list is the first place to cut, because it is the only one that works before the model has decided anything. The sentence to remember is the one the exam builds on: an instruction not to use a tool is a request, and a tool the agent holds is a tool it can use.

### Give each role the tools of its role

The first rule is to start from the role. A search agent needs search and fetch tools. A document agent needs tools that load and extract. A synthesis agent needs to combine and cite; it does not need to reach out. The practice states the rule as code: each role has a *specialisation*, each tool has *tags*, and a role gets the tools whose tags match. The list for each role is short (the practice caps it at five), and a list that grows past the cap is refused, not trimmed, because a team that needs ten tools in one role has a role that should be split.

A tool outside the role is not free just because it is useful once. An agent that holds a tool tends to use it, and every use outside its role is a use that nobody planned for: the synthesis agent that runs web searches spends the budget of the search agent and returns results that no one has attributed. The guide calls this misuse, and it is cheaper to prevent than to detect.

### One narrow tool for the frequent need

Sometimes the strict rule costs too much. In the guide's sample, the synthesis agent often needs to check a date, a name or a figure while it writes. Each time, it hands control back to the coordinator, which calls the search agent and then calls the synthesis agent again: two or three extra round trips per task and about 40 percent more latency. The data shows the shape of the need: 85 percent of the checks are simple lookups and 15 percent need real investigation.

The answer that the exam keys is a scoped cross-role tool, `verify_fact`, given to the synthesis agent for the simple 85 percent, while the hard 15 percent still go through the coordinator and the search agent. The tool is narrow: it checks one fact against a source and returns a short answer; it does not search the web at large. In the practice a tool like this is marked `scoped`, and a role has it only if its definition names it in `extra`; any other tool from outside the specialisation is refused.

The other answers each fail for a reason worth knowing. Waiting to hand back all the checks at the end blocks later steps that depend on earlier verified facts. Giving the synthesis agent every search tool over-provisions it and brings back the misuse. Speculative caching cannot reliably predict what the synthesis agent will need to check.

### A constrained tool in place of a general one

A general tool such as `fetch_url` can reach any address. The agent that holds it can be led, by a page it reads, to fetch something it should not, and it will fetch pages that have nothing to do with its work. The guide's replacement is `load_document`: a tool that accepts only the kinds of address and document the role needs, checks them, and refuses the rest with a reason the agent can read (module 53 on what such a refusal should say). The constraint is in the tool, so it holds whatever the model decides. Taking a tool away is not the fix when the role really needs the capability, and adding a tool to describe the first adds to the list and restricts nothing.

### What the SDK options do

The words "allowed tools" suggest a restriction, and the options of the Agent SDK are a place where the exam's wording and the product part ways. There are three controls.

| Control | What it does | Restricts? |
|---|---|---|
| The definition's `tools` list (a subagent) | The tools that subagent has; omitted, it "inherits every tool available to subagents" | Yes: a subagent's own `tools` list is the restriction |
| `disallowedTools` with a bare name | A bare name "removes the tool from Claude's context, the same as omitting it from `tools`" (custom tools page); a scoped rule such as `Bash(rm *)` leaves the tool visible and denies only matching calls | Yes |
| `allowedTools` | Approves the tools it lists without asking | No: `allowed_tools` pre-approves the tools you list |

The third row is the common mistake. The permissions page says that "Any other tool not listed in `allowed_tools` is still available to Claude, and a call to it that needs approval falls through to the permission mode". It adds that "tools like `Agent` that don't ask before running" run whether or not you list them. So `allowedTools=["Read", "Grep"]` leaves `Bash` where it was. Module 35 introduced the distinction between availability and approval; this page applies it to a team. A subagent's list also names MCP tools by their full name, `mcp__<server>__<tool>` (module 55), and a tool a subagent does not list is not in its context.

### The example

The example makes four of the decisions of these two pages with scripted data: the tool lists of four roles from one catalog of eight tools (the irreversible `send_report` is nobody's by default), the settings of a turn whose first call must be `extract_metadata` on a model that accepts forcing and on one that does not (page 2), what the loop does with three replies, and five refund decisions. This page's part is the first block of output, the lists.

<!-- example: m54-tool-distribution tabs: python,typescript,java,kotlin -->
```python
"""Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.

No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
"""
import logging

log = logging.getLogger(__name__)

CATALOG = {
    "web_search": ["web"], "fetch_page": ["web"], "verify_fact": ["web", "synthesis"], "load_document": ["documents"], "extract_data_points": ["documents"],
    "summarize_content": ["synthesis"], "write_report": ["reports"], "send_report": ["reports"],
}
IRREVERSIBLE = {"send_report"}
ROLES = {"searcher": "web", "analyst": "documents", "synthesizer": "synthesis", "reporter": "reports"}
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}


def tools_for(role):
    return [name for name, tags in CATALOG.items() if ROLES[role] in tags and name not in IRREVERSIBLE]


def turn_for(model, forced, tools):
    """The request settings for a turn whose first call must be `forced`."""
    if model in NO_FORCING:
        return {"tool_choice": "auto", "tools": [forced], "check_reply": True}
    return {"tool_choice": f"tool:{forced}", "tools": tools, "check_reply": False}


def cache_cost(before, after):
    """What switching from one request to another costs in prompt caching."""
    if before["tools"] != after["tools"]:
        return "everything (the tool definitions changed)"
    if before["tool_choice"] != after["tool_choice"]:
        return "the cached messages (tool_choice changed)"
    return "nothing"


def made_the_call(reply, forced):
    calls = [block for block in reply if block[0] == "tool_use"]
    return bool(calls) and calls[0][1] == forced


def allowed(tool, amount, approved, cap=200):
    if tool not in {"refund", "lookup"}:
        return "refused: unknown tool"
    if tool == "lookup":
        return "run"
    if amount > cap:
        return f"refused: above the limit of {cap}, send to a person"
    return "run" if approved else "wait: a person must approve"


def main():
    print(f"catalog: {len(CATALOG)} tools")
    for role in ROLES:
        print(f"  {role}: {', '.join(tools_for(role))}")
    print("a synthesizer that may also check one fact has verify_fact, and nothing else from the web")
    print("first call must be extract_metadata:")
    both = ["extract_metadata", "enrich"]
    for model in ("claude-opus-5", "claude-sonnet-5-5"):
        turn = turn_for(model, "extract_metadata", both)
        print(f"  {model}: tool_choice={turn['tool_choice']}, tools={turn['tools']}, check the reply={turn['check_reply']}")
        print(f"    cost against a turn with auto and both tools: {cache_cost({'tool_choice': 'auto', 'tools': both}, turn)}")
    print("a reply to the fallback turn:")
    for label, reply in (("text only", [("text", "I will look at the metadata.")]), ("the right call", [("tool_use", "extract_metadata")]), ("another tool", [("tool_use", "enrich")])):
        print(f"  {label}: {'accept' if made_the_call(reply, 'extract_metadata') else 're-ask once, then escalate'}")
    print("refund decisions:")
    for tool, amount, approved in (("lookup", 0, False), ("refund", 150, False), ("refund", 150, True), ("refund", 400, True), ("delete_account", 0, True)):
        print(f"  {tool} {amount}, approved={'yes' if approved else 'no'}: {allowed(tool, amount, approved)}")


if __name__ == "__main__":
    main()
```
```text
catalog: 8 tools
  searcher: web_search, fetch_page, verify_fact
  analyst: load_document, extract_data_points
  synthesizer: verify_fact, summarize_content
  reporter: write_report
a synthesizer that may also check one fact has verify_fact, and nothing else from the web
first call must be extract_metadata:
  claude-opus-5: tool_choice=tool:extract_metadata, tools=['extract_metadata', 'enrich'], check the reply=False
    cost against a turn with auto and both tools: the cached messages (tool_choice changed)
  claude-sonnet-5-5: tool_choice=auto, tools=['extract_metadata'], check the reply=True
    cost against a turn with auto and both tools: everything (the tool definitions changed)
a reply to the fallback turn:
  text only: re-ask once, then escalate
  the right call: accept
  another tool: re-ask once, then escalate
refund decisions:
  lookup 0, approved=no: run
  refund 150, approved=no: wait: a person must approve
  refund 150, approved=yes: run
  refund 400, approved=yes: refused: above the limit of 200, send to a person
  delete_account 0, approved=yes: refused: unknown tool
```
```typescript
// Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.
//
// No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
import { logger } from "./logger.ts";
const log = logger("distribution");

export const CATALOG: Record<string, string[]> = {
  web_search: ["web"], fetch_page: ["web"], verify_fact: ["web", "synthesis"], load_document: ["documents"], extract_data_points: ["documents"],
  summarize_content: ["synthesis"], write_report: ["reports"], send_report: ["reports"],
};
const IRREVERSIBLE = new Set(["send_report"]);
export const ROLES: Record<string, string> = { searcher: "web", analyst: "documents", synthesizer: "synthesis", reporter: "reports" };
const NO_FORCING = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]);

export function toolsFor(role: string): string[] {
  return Object.entries(CATALOG).filter(([name, tags]) => tags.includes(ROLES[role]) && !IRREVERSIBLE.has(name)).map(([name]) => name);
}

/** The request settings for a turn whose first call must be `forced`. */
export function turnFor(model: string, forced: string, tools: string[]) {
  if (NO_FORCING.has(model)) return { tool_choice: "auto", tools: [forced], check_reply: true };
  return { tool_choice: `tool:${forced}`, tools, check_reply: false };
}

export function cacheCost(before: { tool_choice: string; tools: string[] }, after: { tool_choice: string; tools: string[] }): string {
  if (JSON.stringify(before.tools) !== JSON.stringify(after.tools)) return "everything (the tool definitions changed)";
  if (before.tool_choice !== after.tool_choice) return "the cached messages (tool_choice changed)";
  return "nothing";
}

export function madeTheCall(reply: Array<[string, string]>, forced: string): boolean {
  const calls = reply.filter((block) => block[0] === "tool_use");
  return calls.length > 0 && calls[0][1] === forced;
}

export function allowed(tool: string, amount: number, approved: boolean, cap = 200): string {
  if (tool !== "refund" && tool !== "lookup") return "refused: unknown tool";
  if (tool === "lookup") return "run";
  if (amount > cap) return `refused: above the limit of ${cap}, send to a person`;
  return approved ? "run" : "wait: a person must approve";
}

function main() {
  console.log(`catalog: ${Object.keys(CATALOG).length} tools`);
  for (const role of Object.keys(ROLES)) console.log(`  ${role}: ${toolsFor(role).join(", ")}`);
  console.log("a synthesizer that may also check one fact has verify_fact, and nothing else from the web");
  console.log("first call must be extract_metadata:");
  const both = ["extract_metadata", "enrich"];
  for (const model of ["claude-opus-5", "claude-sonnet-5-5"]) {
    const turn = turnFor(model, "extract_metadata", both);
    console.log(`  ${model}: tool_choice=${turn.tool_choice}, tools=[${turn.tools.map((t) => `'${t}'`).join(", ")}], check the reply=${turn.check_reply ? "True" : "False"}`);
    console.log(`    cost against a turn with auto and both tools: ${cacheCost({ tool_choice: "auto", tools: both }, turn)}`);
  }
  console.log("a reply to the fallback turn:");
  const replies: Array<[string, Array<[string, string]>]> = [["text only", [["text", "I will look at the metadata."]]], ["the right call", [["tool_use", "extract_metadata"]]], ["another tool", [["tool_use", "enrich"]]]];
  for (const [label, reply] of replies) console.log(`  ${label}: ${madeTheCall(reply, "extract_metadata") ? "accept" : "re-ask once, then escalate"}`);
  console.log("refund decisions:");
  const calls: Array<[string, number, boolean]> = [["lookup", 0, false], ["refund", 150, false], ["refund", 150, true], ["refund", 400, true], ["delete_account", 0, true]];
  for (const [tool, amount, approved] of calls) console.log(`  ${tool} ${amount}, approved=${approved ? "yes" : "no"}: ${allowed(tool, amount, approved)}`);
}

if (import.meta.main) main();
```
```text
catalog: 8 tools
  searcher: web_search, fetch_page, verify_fact
  analyst: load_document, extract_data_points
  synthesizer: verify_fact, summarize_content
  reporter: write_report
a synthesizer that may also check one fact has verify_fact, and nothing else from the web
first call must be extract_metadata:
  claude-opus-5: tool_choice=tool:extract_metadata, tools=['extract_metadata', 'enrich'], check the reply=False
    cost against a turn with auto and both tools: the cached messages (tool_choice changed)
  claude-sonnet-5-5: tool_choice=auto, tools=['extract_metadata'], check the reply=True
    cost against a turn with auto and both tools: everything (the tool definitions changed)
a reply to the fallback turn:
  text only: re-ask once, then escalate
  the right call: accept
  another tool: re-ask once, then escalate
refund decisions:
  lookup 0, approved=no: run
  refund 150, approved=no: wait: a person must approve
  refund 150, approved=yes: run
  refund 400, approved=yes: refused: above the limit of 200, send to a person
  delete_account 0, approved=yes: refused: unknown tool
```
```java
import static harness.Show.py;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.
 *
 * <p>No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
 */
public final class Distribution {
    private static final System.Logger LOG = System.getLogger(Distribution.class.getName());
    static final Map<String, List<String>> CATALOG = new LinkedHashMap<>();
    static final Set<String> IRREVERSIBLE = Set.of("send_report");
    static final Map<String, String> ROLES = new LinkedHashMap<>();
    static final Set<String> NO_FORCING = Set.of("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1");

    static {
        CATALOG.put("web_search", List.of("web"));
        CATALOG.put("fetch_page", List.of("web"));
        CATALOG.put("verify_fact", List.of("web", "synthesis"));
        CATALOG.put("load_document", List.of("documents"));
        CATALOG.put("extract_data_points", List.of("documents"));
        CATALOG.put("summarize_content", List.of("synthesis"));
        CATALOG.put("write_report", List.of("reports"));
        CATALOG.put("send_report", List.of("reports"));
        ROLES.put("searcher", "web");
        ROLES.put("analyst", "documents");
        ROLES.put("synthesizer", "synthesis");
        ROLES.put("reporter", "reports");
    }

    /** The settings of one request: the tool_choice, the tools offered, and whether the reply must be checked for the call. */
    record Turn(String toolChoice, List<String> tools, boolean checkReply) {}

    /** One block of a reply: its type and the text or tool name it holds. */
    record Block(String type, String value) {}

    static List<String> toolsFor(String role) {
        return CATALOG.entrySet().stream().filter(e -> e.getValue().contains(ROLES.get(role)) && !IRREVERSIBLE.contains(e.getKey())).map(Map.Entry::getKey).toList();
    }

    /** The request settings for a turn whose first call must be `forced`. */
    static Turn turnFor(String model, String forced, List<String> tools) {
        if (NO_FORCING.contains(model)) return new Turn("auto", List.of(forced), true);
        return new Turn("tool:" + forced, tools, false);
    }

    /** What switching from one request to another costs in prompt caching. */
    static String cacheCost(Turn before, Turn after) {
        if (!before.tools().equals(after.tools())) return "everything (the tool definitions changed)";
        if (!before.toolChoice().equals(after.toolChoice())) return "the cached messages (tool_choice changed)";
        return "nothing";
    }

    static boolean madeTheCall(List<Block> reply, String forced) {
        List<Block> calls = reply.stream().filter(b -> b.type().equals("tool_use")).toList();
        return !calls.isEmpty() && calls.get(0).value().equals(forced);
    }

    static String allowed(String tool, int amount, boolean approved, int cap) {
        if (!Set.of("refund", "lookup").contains(tool)) return "refused: unknown tool";
        if (tool.equals("lookup")) return "run";
        if (amount > cap) return "refused: above the limit of " + cap + ", send to a person";
        return approved ? "run" : "wait: a person must approve";
    }

    static String allowed(String tool, int amount, boolean approved) {
        return allowed(tool, amount, approved, 200);
    }

    public static void main(String[] args) {
        System.out.println("catalog: " + CATALOG.size() + " tools");
        for (String role : ROLES.keySet()) System.out.println("  " + role + ": " + String.join(", ", toolsFor(role)));
        System.out.println("a synthesizer that may also check one fact has verify_fact, and nothing else from the web");
        System.out.println("first call must be extract_metadata:");
        List<String> both = List.of("extract_metadata", "enrich");
        for (String model : List.of("claude-opus-5", "claude-sonnet-5-5")) {
            Turn turn = turnFor(model, "extract_metadata", both);
            System.out.println("  " + model + ": tool_choice=" + turn.toolChoice() + ", tools=" + py(turn.tools()) + ", check the reply=" + py(turn.checkReply()));
            System.out.println("    cost against a turn with auto and both tools: " + cacheCost(new Turn("auto", both, false), turn));
        }
        System.out.println("a reply to the fallback turn:");
        Object[][] replies = {
            {"text only", List.of(new Block("text", "I will look at the metadata."))},
            {"the right call", List.of(new Block("tool_use", "extract_metadata"))},
            {"another tool", List.of(new Block("tool_use", "enrich"))}};
        for (Object[] r : replies) {
            @SuppressWarnings("unchecked")
            List<Block> reply = (List<Block>) r[1];
            System.out.println("  " + r[0] + ": " + (madeTheCall(reply, "extract_metadata") ? "accept" : "re-ask once, then escalate"));
        }
        System.out.println("refund decisions:");
        Object[][] decisions = {{"lookup", 0, false}, {"refund", 150, false}, {"refund", 150, true}, {"refund", 400, true}, {"delete_account", 0, true}};
        for (Object[] d : decisions) {
            System.out.println("  " + d[0] + " " + d[1] + ", approved=" + ((Boolean) d[2] ? "yes" : "no") + ": " + allowed((String) d[0], (Integer) d[1], (Boolean) d[2]));
        }
    }
}
```
```text
catalog: 8 tools
  searcher: web_search, fetch_page, verify_fact
  analyst: load_document, extract_data_points
  synthesizer: verify_fact, summarize_content
  reporter: write_report
a synthesizer that may also check one fact has verify_fact, and nothing else from the web
first call must be extract_metadata:
  claude-opus-5: tool_choice=tool:extract_metadata, tools=['extract_metadata', 'enrich'], check the reply=False
    cost against a turn with auto and both tools: the cached messages (tool_choice changed)
  claude-sonnet-5-5: tool_choice=auto, tools=['extract_metadata'], check the reply=True
    cost against a turn with auto and both tools: everything (the tool definitions changed)
a reply to the fallback turn:
  text only: re-ask once, then escalate
  the right call: accept
  another tool: re-ask once, then escalate
refund decisions:
  lookup 0, approved=no: run
  refund 150, approved=no: wait: a person must approve
  refund 150, approved=yes: run
  refund 400, approved=yes: refused: above the limit of 200, send to a person
  delete_account 0, approved=yes: refused: unknown tool
```
```kotlin
import harness.Show.py

private val log = System.getLogger("distribution")

/**
 * Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.
 *
 * No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
 */
val CATALOG = linkedMapOf(
    "web_search" to listOf("web"), "fetch_page" to listOf("web"), "verify_fact" to listOf("web", "synthesis"), "load_document" to listOf("documents"),
    "extract_data_points" to listOf("documents"), "summarize_content" to listOf("synthesis"), "write_report" to listOf("reports"), "send_report" to listOf("reports"),
)
val IRREVERSIBLE = setOf("send_report")
val ROLES = linkedMapOf("searcher" to "web", "analyst" to "documents", "synthesizer" to "synthesis", "reporter" to "reports")
val NO_FORCING = setOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1")

/** The settings of one request: the tool_choice, the tools offered, and whether the reply must be checked for the call. */
data class Turn(val toolChoice: String, val tools: List<String>, val checkReply: Boolean)

/** One block of a reply: its type and the text or tool name it holds. */
data class Block(val type: String, val value: String)

fun toolsFor(role: String): List<String> = CATALOG.filter { (name, tags) -> ROLES.getValue(role) in tags && name !in IRREVERSIBLE }.keys.toList()

/** The request settings for a turn whose first call must be `forced`. */
fun turnFor(model: String, forced: String, tools: List<String>): Turn =
    if (model in NO_FORCING) Turn("auto", listOf(forced), true) else Turn("tool:$forced", tools, false)

/** What switching from one request to another costs in prompt caching. */
fun cacheCost(before: Turn, after: Turn): String = when {
    before.tools != after.tools -> "everything (the tool definitions changed)"
    before.toolChoice != after.toolChoice -> "the cached messages (tool_choice changed)"
    else -> "nothing"
}

fun madeTheCall(reply: List<Block>, forced: String): Boolean = reply.filter { it.type == "tool_use" }.let { it.isNotEmpty() && it[0].value == forced }

fun allowed(tool: String, amount: Int, approved: Boolean, cap: Int = 200): String = when {
    tool !in setOf("refund", "lookup") -> "refused: unknown tool"
    tool == "lookup" -> "run"
    amount > cap -> "refused: above the limit of $cap, send to a person"
    approved -> "run"
    else -> "wait: a person must approve"
}

fun main() {
    println("catalog: ${CATALOG.size} tools")
    for (role in ROLES.keys) println("  $role: ${toolsFor(role).joinToString(", ")}")
    println("a synthesizer that may also check one fact has verify_fact, and nothing else from the web")
    println("first call must be extract_metadata:")
    val both = listOf("extract_metadata", "enrich")
    for (model in listOf("claude-opus-5", "claude-sonnet-5-5")) {
        val turn = turnFor(model, "extract_metadata", both)
        println("  $model: tool_choice=${turn.toolChoice}, tools=${py(turn.tools)}, check the reply=${py(turn.checkReply)}")
        println("    cost against a turn with auto and both tools: ${cacheCost(Turn("auto", both, false), turn)}")
    }
    println("a reply to the fallback turn:")
    for ((label, reply) in listOf(
        "text only" to listOf(Block("text", "I will look at the metadata.")),
        "the right call" to listOf(Block("tool_use", "extract_metadata")),
        "another tool" to listOf(Block("tool_use", "enrich")),
    )) {
        println("  $label: ${if (madeTheCall(reply, "extract_metadata")) "accept" else "re-ask once, then escalate"}")
    }
    println("refund decisions:")
    for ((tool, amount, approved) in listOf(Triple("lookup", 0, false), Triple("refund", 150, false), Triple("refund", 150, true), Triple("refund", 400, true), Triple("delete_account", 0, true))) {
        println("  $tool $amount, approved=${if (approved) "yes" else "no"}: ${allowed(tool, amount, approved)}")
    }
}
```
```text
catalog: 8 tools
  searcher: web_search, fetch_page, verify_fact
  analyst: load_document, extract_data_points
  synthesizer: verify_fact, summarize_content
  reporter: write_report
a synthesizer that may also check one fact has verify_fact, and nothing else from the web
first call must be extract_metadata:
  claude-opus-5: tool_choice=tool:extract_metadata, tools=['extract_metadata', 'enrich'], check the reply=False
    cost against a turn with auto and both tools: the cached messages (tool_choice changed)
  claude-sonnet-5-5: tool_choice=auto, tools=['extract_metadata'], check the reply=True
    cost against a turn with auto and both tools: everything (the tool definitions changed)
a reply to the fallback turn:
  text only: re-ask once, then escalate
  the right call: accept
  another tool: re-ask once, then escalate
refund decisions:
  lookup 0, approved=no: run
  refund 150, approved=no: wait: a person must approve
  refund 150, approved=yes: run
  refund 400, approved=yes: refused: above the limit of 200, send to a person
  delete_account 0, approved=yes: refused: unknown tool
```
<!-- /example -->

Read the first block. Four roles share one catalog of eight tools, and no role has more than three. The searcher and the synthesizer both hold `verify_fact`, because it carries both tags in the catalog, and neither holds the other's tools. The irreversible `send_report` appears in no list: someone has to write it down for a role. The rest of the output belongs to page 2.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Give every subagent every tool, so that none of them is ever blocked."** It is tempting because it is the least code and nothing can be missing. The exam rejects it: more tools make each choice less reliable, and an agent outside its specialisation misuses what it holds. Give each role the tools of its role.
2. **"Tell the synthesis agent in its prompt not to search the web."** It is tempting because the prompt is the quickest change. The exam rejects it: an instruction is a request, and a tool the agent holds is a tool it can use. Take the tool out of its list.
3. **"Give the synthesis agent every search tool so that it never has to wait for the coordinator."** It is tempting because it removes the round trips. The exam rejects it: it over-provisions the agent and brings the misuse back. Give it one narrow `verify_fact` for the simple checks and keep the complex ones with the coordinator.
4. **"Add a routing layer that enables a subset of tools for each request."** It is tempting because it sounds like least privilege. The exam rejects it as a first answer: the problem is the list each role holds, and a router adds a component to keep correct. Scope the lists first.
5. **"List the permitted tools in `allowedTools` and the agent can use only those."** It is tempting because the name says "allowed". The exam rejects it, and so does the product: the option approves what it lists, and the other tools remain available. Restrict with the definition's `tools` list or with `disallowedTools`.

## Quiz

1. A synthesis subagent often needs to confirm a date or a name while it writes. Each time it returns control to the coordinator, which calls the search subagent and then the synthesis subagent again, and latency has grown by about 40 percent. Most checks are simple lookups and a few need real investigation. What is the best design?
   - **a**: Give it a small verifier tool for the quick cases and route the hard ones through the hub
   - **b**: Let the synthesis subagent collect every check and return them in one batch at the end
   - **c**: Give the synthesis subagent each of the search tools so that it can answer any check itself
   - **d**: Have the search subagent save extra context around each source in case a check comes up

2. An analysis subagent holds a general `fetch_url` tool, and it sometimes fetches pages that have nothing to do with the documents it was given. Which change fits best?
   - **a**: Add a sentence to its prompt saying that only links to the supplied files may be fetched
   - **b**: Swap it for a loader that takes only the job's links and refuses the rest with a reason
   - **c**: Take the tool away and have the coordinator paste each file's text into the brief by hand
   - **d**: Give it a second tool that reports the host of each link before the fetch is made

<details>
<summary>Answer key</summary>

1. **a**. A narrow tool serves the frequent case and the coordinator keeps the rare one. *b* is ruled out because "Waiting to hand back all the checks at the end blocks later steps that depend on earlier verified facts". *c* is ruled out because "Giving the synthesis agent every search tool over-provisions it and brings back the misuse". *d* is ruled out because "Speculative caching cannot reliably predict what the synthesis agent will need to check".
2. **b**. A constraint inside the tool holds whatever the model decides. *a* is ruled out because "an instruction not to use a tool is a request, and a tool the agent holds is a tool it can use". *c* is ruled out because "Taking a tool away is not the fix when the role really needs the capability". *d* is ruled out because "adding a tool to describe the first adds to the list and restricts nothing".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on a verification tool for a synthesis agent, rewritten here.
