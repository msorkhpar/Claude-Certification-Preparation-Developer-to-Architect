# `tool_choice`, the call that must happen, caps on irreversible tools, and the practice

**Level:** Architect · **Module 54:** Distributing tools across agents · **Page 2 of 2**
**Exams:** A2.3; S1, S3

**After this page you can** say what `auto`, `any`, a named tool and `none` are for, force a first tool where the model accepts it and get the same effect where it does not, check a reply for the call that was required, say what each change of tool choice or tool list costs in prompt caching, put a numeric cap and an authorisation in the tool layer for an irreversible tool, and write the module's practice: scoped tool lists, the settings of a turn, a check of the reply and the authorisation of a call.

Checked on 2026-10-03 against the Claude API documentation pages "Define tools" (the `tool_choice` section) and "Prompt caching" (the section "What invalidates the cache"), the Model Context Protocol specification (version 2026-07-28, the Tools page, section "Security Considerations"), and the exam guide for the Architect Foundations exam (version 1.0, July 2026): Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287. The practice is offline in Python, TypeScript, Java and Kotlin: it never calls a model, and its rules (the budget of five tools, the codes, the policy shape) are the course's own design, which the statement says. This page deepens module 26 (the four values of `tool_choice` and where forcing is rejected) and module 48 (why `tool_choice` is not a gate), and it does not repeat them; the details of the 400 error are there.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the guide lists three options of `tool_choice`, `"auto"`, `"any"` and a forced selection `{"type": "tool", "name": "..."}`; it uses a forced selection to make a specific tool the first call (the example is `extract_metadata` before the enrichment tools) with the later steps in follow-up turns, and `"any"` to guarantee that the model calls a tool instead of answering in conversational text. *What the current documentation says (checked 2026-10-03):* all of that works where forcing is accepted, and the page says that "`any` tells Claude that it must use one of the provided tools, but doesn't force a particular tool". It also lists where forcing is not accepted: on Claude Opus 5.5, Claude Sonnet 5.5, Claude Fable 5.1 and Claude Mythos 5.1, "`any` and `tool` return a 400 error", and they are also unsupported with manual extended thinking; for those the page recommends `auto` with strict tool use for valid inputs, or structured outputs for a fixed reply shape. Two costs matter to an architect: "changes to the `tool_choice` parameter will invalidate cached message blocks", while "Modifying tool definitions (names, descriptions, parameters) invalidates the entire cache". On the exam, answer "make this tool the first call" and "guarantee a tool call" with the forced choice and `any`; in your own code on a current model, produce the same effect with a loop that checks the reply, and know that narrowing the tool list to get it costs more than changing the choice.

## Why it matters

Two sentences of the guide's task statement decide a lot of real designs: a specific tool must be called first, and the model must call some tool instead of talking. The guide's answer is a parameter. On the models this course uses, the parameter is refused, and the team that built on it finds out at the first request. The skill that carries over is knowing what the parameter was for (a guarantee about one reply), what replaces it where it is refused (a check of the reply, and sometimes a smaller list of tools), and what each way of changing the request costs. The same page of the guide ends with a second, quieter topic that exam scenarios use for a support agent: the control on an irreversible tool is not the model's judgement but a cap and an authorisation that the tool layer applies.

## The idea

### What `tool_choice` is for

The four values are in module 26. This page is about what each one is for in a design.

| Value | Use it when | What it guarantees |
|---|---|---|
| `auto` | The model should decide whether a tool is needed | Nothing: it may answer in text |
| `any` | The model must call some tool and not talk | A tool call, not which one |
| a named tool | One particular tool must be the call of this request | That tool, as the first thing in the reply |
| `none` | No tool may be called in this request | No tool call |

A forced value works on the request that sets it: "the API prefills the assistant message to force a tool to be used", and the model writes no explanation before the call. For the guide's case, forcing `extract_metadata` on the first request makes sure it runs before the enrichment tools; the later steps are follow-up requests with `auto`, because forcing the same tool again would make the loop call it for ever. A forced first call is a guarantee about one reply, and the rest of the sequence is still a property of the program (module 48).

### Where forcing is refused: a loop that checks the reply

On the four models listed in the box, `any` and `tool` are refused. The goal can still be met, in two parts.

1. **Ask with `auto`, and say what you want.** Use `auto` with strict tool use, so that a call, if made, has a valid shape, and put the instruction in the request. The documentation says that prompting "still influences which tool `auto` picks".
2. **Check the reply.** The loop looks at what came back. A reply of text only, when a call was required, is a miss. A first call to another tool, when a named one was required, is the wrong tool. In both cases the loop does not proceed: it asks once more, and if the miss repeats it hands the case to a person. Looping until the turn limit turns a missed call into a run that ends with nothing, and accepting the text lets the whole sequence start from a step that never happened.

The practice's `check_turn` is this check: `ok`, `missed_call` or `wrong_tool`, for a reply and the call that was required. A loop that cannot rely on the parameter writes it once.

To make the named tool the only choice the model has, a team may also offer only that tool in the first request. That works, and it has a price, which the next section shows.

### What each change costs in caching

A request is cached in layers, `tools` first, then `system`, then `messages`: "Changes at each level invalidate that level and all subsequent levels." Two changes matter here.

| What changes | What is processed again |
|---|---|
| `tool_choice` | The message blocks: "Changes to the `tool_choice` parameter only affect message blocks" |
| The tool list or a definition | Everything: "Modifying tool definitions (names, descriptions, parameters) invalidates the entire cache" |

So the two ways of forcing a first call are not equal. Changing `tool_choice` costs the cached messages; narrowing the tool list to one tool for the first request and restoring the full list afterwards costs the whole cache twice. A long conversation behind a long system prompt makes that expensive. The alternative that costs nothing is the one module 48 describes: keep the list and the choice as they are, and let the dispatcher refuse every other tool until the first has run, with a result that tells the model what to do. The practice's `cache_impact` states the table: `none`, `messages` or `all` for two requests, with the tool list compared first.

### Caps and authorisation on irreversible tools

The last part of the task statement is about tools that cannot be undone: a refund, a deletion, a message sent to a customer. Their control sits in the tool layer, for the reasons of module 48: the model's own judgement is probabilistic, and the layer is code. The protocol's security section says the same in its own terms: servers "MUST" "Implement proper access controls", and clients "SHOULD" "Prompt for user confirmation on sensitive operations". Four rules are enough for the exam's scenarios, and the practice's `authorize` applies them in this order.

1. **Default deny.** A tool that the policy does not name is refused. The default is no.
2. **Check the input.** A cap needs a number: an amount that is missing, a fraction, a string or a boolean is refused as a bad amount.
3. **Check the owner.** The call must be for the customer who was verified, and the tool layer compares the two; whatever the model says about who the customer is does not count.
4. **Apply the cap, and then the approval.** An amount above the cap is refused and sent to a person, and an approval never lifts a cap. An irreversible call needs an approval even when the amount is under the cap, because a review after the fact cannot undo an irreversible call.

Each refusal is a result that the agent can use (module 53): it says what is needed and who can give it, so the agent can pass the case on. A control the model could talk its way around is not a control. A note in the tool's description that asks for care is an instruction, a field in which the model states its reason is a claim by the model, not a check, and the place of a tool in the list is not a limit on what it can do. The hook of module 49 is the other place for the same logic: a PreToolUse hook that denies the call, with a reason, does the same job before the tool runs.

### The example

The example is the same program as on page 1, and the part that belongs here is the rest of its output: the turn whose first call must be `extract_metadata` on a model that accepts forcing and on one that does not, with what each costs in caching; what the loop does with three replies; and five refund decisions.

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

Read the second block. On the model that accepts forcing, the turn forces the tool and keeps both tools, at the cost of the cached messages. On the model that does not, the turn is `auto` with the one tool offered and the reply checked, at the cost of everything, because the list changed. The third block shows the check: a reply of text only is a miss, the right call is accepted, and another tool first is the wrong one. The last block is the authorisation: a lookup runs, a refund of 150 waits for a person until it is approved, a refund of 400 is refused whatever the approval, and a tool nobody listed is refused.

### The practice

The practice is `exercises/54-distributing-tools-across-agents/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin, and it is written as four steps, each with the reason the exam cares and what you should see when it works. You write `assign_tools` (scoped lists with a budget, scoped cross-role tools and explicit grants for irreversible ones), `plan_turn` and `cache_impact` (the settings of a turn and what a change costs), `check_turn` (the reply against the call that was required) and `authorize` (the four rules above).

Eight cases grade it: the main path, the refusals in `assign_tools`, the grant of irreversible tools, the choice by model, the cost of changes, the check of the reply, the refusals of `authorize` in their order, and the cap with its approval. The starter fails all eight, the reference passes them, and each of eighteen planted wrong solutions per language fails on an assertion of the case it breaks: a role's tools in the wrong order, a role over its budget, an outside tool granted without being scoped, a duplicate name accepted, an irreversible tool given by tag, forcing on every model, every tool offered in the fallback, a fallback that does not ask for the check, a narrower tool list not counted, a changed choice not counted, a repeated request that costs something, a miss accepted, the wrong first tool accepted, an unlisted tool allowed, an owner not compared, a cap that refuses its own value, an approval that lifts the cap, and an irreversible call that needs no approval.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Force the first tool with `tool_choice`, and the sequence is guaranteed."** It is tempting because the guide's example does exactly that. The exam keys the forced choice for the first call; it rejects the idea that it settles the order of the rest. Where it works, it applies to the request that sets it, and the later steps are follow-up turns.
2. **"On a current model, set `tool_choice` to `any` to be sure a tool is called."** It is tempting because it is the guide's own answer. The exam keys it, and the product refuses it on the four models of the box: "`any` and `tool` return a 400 error". Use `auto` with strict tool use and a loop that checks the reply.
3. **"Offer only the one tool for the first request, then put the full list back."** It is tempting because it makes the choice unavoidable. It is not rejected by the exam, but it costs the entire cache twice. Prefer a dispatcher that refuses the other tools until the first call has run.
4. **"Describe in the tool's description that a refund over 500 needs approval."** It is tempting because the model reads the description. The exam rejects it: a description is an instruction, and a numeric limit is a rule with a right answer. Check the cap in the tool layer and return a result that routes to a person.
5. **"Once a person approves a refund, the cap no longer applies."** It is tempting because approval sounds like the last word. The exam rejects it: the approval covers the call, not the limit. An amount above the cap goes to a person who can raise the limit, not through an approval of the call.

## Quiz

1. To force a first tool on a model that rejects forcing, a team offers only that tool in the first request and the full list in every later one. Which cost does the first change carry?
   - **a**: Only the message blocks are processed again, as for any change of tool choice
   - **b**: Nothing is processed again, since definitions that are removed stay cached
   - **c**: Only the system prompt is processed again, because it sits above the messages
   - **d**: The whole cached prefix is processed again, since the definitions differ

2. In this module's example, a loop on Claude Sonnet 5.5 offers the one required tool with `auto`, and a reply comes back as plain text with no tool call. What should the loop do?
   - **a**: Accept the text as the answer and go on to the next step
   - **b**: Resend the request with `any` as the choice and expect a call
   - **c**: Count it as a miss and ask once more before escalating
   - **d**: Repeat the same request until the turn limit ends the run

<details>
<summary>Answer key</summary>

1. **d**. Changing the definitions changes the first layer of the cache, and every layer after it. *a* is ruled out because that holds only for the choice: "Modifying tool definitions (names, descriptions, parameters) invalidates the entire cache". *b* is ruled out for the same reason, since the list is part of the cached prefix: "Modifying tool definitions (names, descriptions, parameters) invalidates the entire cache". *c* is ruled out because the layers cascade downward: "Changes at each level invalidate that level and all subsequent levels."
2. **c**. With `auto` the table's guarantee is "Nothing: it may answer in text", so a missed call is a miss that the loop handles. *a* is ruled out because "A reply of text only, when a call was required, is a miss". *b* is ruled out because the parameter is refused there: "`any` and `tool` return a 400 error". *d* is ruled out because "Looping until the turn limit turns a missed call into a run that ends with nothing".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. The tool that closes a customer's account carries a description that asks the model to use it with care. What should restrict it?
   - **a**: A longer note in its description, with an example of when not to call it
   - **b**: A required parameter in which the model states its reason for each call
   - **c**: A system prompt rule that forbids the call unless the customer asks for it
   - **d**: A server-side check of ownership and a human approval before it runs

2. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. Refunds cannot be undone, and this module's tool layer caps them at 500. A call for 450 arrives and nobody has approved it. What should the tool layer return?
   - **a**: A result that runs the refund and logs that the amount is under the cap
   - **b**: A result that runs the refund and notifies a person to review it afterwards
   - **c**: A flagged refusal that stays on hold until a human signs off on this one
   - **d**: A flagged refusal that sends the case to a person who can raise the cap

3. Scenario S3, a multi-agent research system. A coordinator delegates to a web search subagent, a document analysis subagent and a synthesis subagent, and it produces a cited report. All subagents hold the same 18 tools, including the one that sends the finished report, and the synthesis subagent has begun running web searches. Which redesign fits best?
   - **a**: Split the list by role, and give the irreversible step one owner and an approval
   - **b**: Keep one list, and add a prompt line for each role naming the tools it may use
   - **c**: Cut the shared list to the ten most used tools, and give it to every role alike
   - **d**: Keep the 18 tools, and route each request through a classifier that picks a subset

<details>
<summary>Answer key</summary>

1. **d**. Closing an account cannot be undone, so the tool layer compares the call with the verified customer and holds it for an approval, which no wording from the model can get around. *a* is ruled out because "A note in the tool's description that asks for care is an instruction". *b* is ruled out because a field in which the model states its reason "is a claim by the model, not a check". *c* is ruled out because a prompt rule is still "an instruction not to use a tool", and "an instruction not to use a tool is a request".
2. **c**. A refund cannot be undone, so it waits for a person's approval even under the cap. *a* is ruled out because "An irreversible call needs an approval even when the amount is under the cap". *b* is ruled out because "a review after the fact cannot undo an irreversible call". *d* is ruled out because the route to a person who can raise the limit is for an amount above the cap: "An amount above the cap is refused and sent to a person", and 450 is under 500.
3. **a**. Scoped lists remove the misuse, and the irreversible step has an owner and an approval. *b* is ruled out because a prompt line is a request: "an instruction not to use a tool is a request, and a tool the agent holds is a tool it can use". *c* is ruled out because "an agent outside its specialisation misuses what it holds". *d* is ruled out because "a router adds a component to keep correct".

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License). The first module question follows an item of that set on constraining a tool that deletes records, rewritten here.
