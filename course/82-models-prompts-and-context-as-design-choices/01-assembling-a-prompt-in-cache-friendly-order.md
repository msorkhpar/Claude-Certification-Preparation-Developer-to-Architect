# Assembling a prompt in cache-friendly order

**Level:** Architect Professional · **Module 82:** Models, prompts and context as design choices · **Page 1 of 2**
**Exams:** P2

**After this page you can** treat a request as modules with a fixed order instead of one string, put what never changes first so that the cache can read it, find the one edit that breaks the cached prefix, estimate whether a prefix is long enough to be cached, and drop the least useful context first when a budget is exceeded.

Checked on 2026-10-04 against the Claude API documentation page "Prompt caching" (including the minimum cacheable length for Claude Sonnet 5.5), the page "Prompting best practices", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domain 2. The example ran in the course's container in Python, TypeScript, Java and Kotlin with the same output. It calls no model and estimates tokens as one per four characters, a rough rule and not the model's tokenizer, so its counts show the mechanism and are not what the API would bill.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 2 treats model choice, prompt design and context as architecture decisions with cost and latency consequences. *What the current documentation says (checked 2026-10-04):* a cached prefix is built "in the following order: tools, system, then messages"; a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control", and for Claude Sonnet 5.5 that minimum is 512 tokens; the prompting guide advises putting long documents "near the top of your prompt, above your query". *How to read both:* the exam keys the design in which stable content comes first and the variable content last, and rejects a design that puts a changing value ahead of a stable block. The numbers (512 tokens, the model it applies to) change between models and releases: read the documentation page for the model you use.

## Why it matters

A support assistant sends the same role text and the same forty-page policy on every request, then the customer's record and question. The policy is thousands of tokens. If the cache can read it, those tokens are cheaper and faster on every call after the first. If one changing word sits above it, such as a timestamp in the first line, the cache can never match, and the system pays full price on every call without anyone noticing. The prompt looks the same to a reader. Its order is what decides the bill, and the order is an architecture decision.

## The idea

### A request is modules, not a string

Build the request from named **modules**, each marked static (the same on every request) or dynamic (it changes with the customer, the question or the history). A small function assembles them in a fixed order: static modules first, in the order given, then the dynamic ones with their variables filled in. Three habits follow.

1. **Variables belong in dynamic modules only.** A static module whose text holds a `{variable}` is refused when the request is assembled, because a value that changes in the prefix breaks the cache.
2. **A missing variable is an error.** A request that goes out with `{customer}` in its text is a defect that no model will report; the assembler refuses it.
3. **The order is the same every time.** Shuffling the modules changes the prefix, so the assembler decides the order and callers cannot.

### Why the order matters: the cached prefix

The cache works on a **prefix**: the request from its start up to a marked breakpoint. Two requests share a cache entry only when everything up to the breakpoint is identical, character for character. In the example, the role and the policy are static and the customer and the question are dynamic, so the breakpoint goes after the policy.

| Edit | Prefix identical? | Why |
|---|---|---|
| A different customer and question | Yes | Only the dynamic modules changed, and they come after the breakpoint |
| One number changed in the policy | No | The edit is inside the prefix, so everything from there on is a new entry |
| A timestamp added to the role text | No | A changing value in the prefix breaks every match, on every request |

The documentation fixes the order of the whole request as tools, then system, then messages, so a change to a tool definition invalidates the system prompt and the messages that follow it. That is a reason to treat the tool list as a stable asset and not to edit it on a whim.

### The minimum: a short prefix is not cached

A prefix can be marked and still not be cached. Each model has a minimum length, and below it the marking has no effect and does not report an error. The example shows both sides. With the policy, the static prefix is 587 tokens, above the 512-token minimum, so a breakpoint is set. Without the policy, the prefix is 21 tokens, the assembler reports no breakpoint, and the reason is printed. The lesson for the architect is to know the minimum for the model in use and to count the prefix, rather than assume caching is on because it was requested. A cache that was never active shows up only in the bill, and the check is to read the usage figures the API returns for cache writes and cache reads.

### A budget drops the least useful context first

Context is limited, and a request that exceeds a budget must lose something. Decide beforehand what: each dynamic module has a **priority**, and the assembler drops the lowest first, and among equal priorities the later one. The static modules are never dropped, since they hold the rules, and when only static modules remain and they still exceed the budget, the assembler refuses the request and does not send a truncated policy. A silent truncation would be worse than a refusal.

### The example

<!-- example: m82-prompt-budget tabs: python,typescript,java,kotlin -->
```python
"""A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.

The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
"""
MIN_CACHEABLE = 512  # tokens, Claude Sonnet 5.5

POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. " * 26
MODULES = [
    {"name": "role", "static": True, "text": "You are the support assistant of Northwind Outfitters. Answer from the policy only."},
    {"name": "policy", "static": True, "text": POLICY},
    {"name": "customer", "static": False, "text": "Customer: {customer}. Tier: {tier}."},
    {"name": "question", "static": False, "text": "Question: {question}"},
]


def tokens(text):
    return -(-len(text) // 4)  # ceiling of characters over four


def assemble(modules, variables):
    """Static modules first, in the order given, then the dynamic ones with their variables filled in."""
    ordered = [m for m in modules if m["static"]] + [m for m in modules if not m["static"]]
    blocks = [{"name": m["name"], "static": m["static"], "text": m["text"].format(**variables) if not m["static"] else m["text"]} for m in ordered]
    prefix = sum(tokens(b["text"]) for b in blocks if b["static"])
    last_static = max((i for i, b in enumerate(blocks) if b["static"]), default=None)
    return {"blocks": blocks, "tokens": sum(tokens(b["text"]) for b in blocks), "prefix_tokens": prefix,
            "breakpoint": last_static if prefix >= MIN_CACHEABLE else None}


def cached_prefix(prompt):
    return "".join(b["text"] for b in prompt["blocks"][: prompt["breakpoint"] + 1]) if prompt["breakpoint"] is not None else ""


def main():
    first = assemble(MODULES, {"customer": "Ana", "tier": "gold", "question": "Can I return a gift card?"})
    print("order:", " > ".join(b["name"] for b in first["blocks"]))
    print(f"tokens: {first['tokens']} in all, {first['prefix_tokens']} in the static prefix, minimum {MIN_CACHEABLE}")
    print("breakpoint after:", first["blocks"][first["breakpoint"]]["name"])
    second = assemble(MODULES, {"customer": "Ben", "tier": "basic", "question": "Where is my parcel?"})
    print("next request, other customer: prefix identical:", cached_prefix(second) == cached_prefix(first))
    edited = [dict(m, text=m["text"].replace("200", "300")) if m["name"] == "policy" else m for m in MODULES]
    third = assemble(edited, {"customer": "Ana", "tier": "gold", "question": "Can I return a gift card?"})
    print("after a policy edit: prefix identical:", cached_prefix(third) == cached_prefix(first))
    short = assemble(MODULES[:1] + MODULES[2:], {"customer": "Ana", "tier": "gold", "question": "Hi"})
    print("without the policy: breakpoint", short["breakpoint"], "because", short["prefix_tokens"], "tokens is under", MIN_CACHEABLE)


if __name__ == "__main__":
    main()
```
```text
order: role > policy > customer > question
tokens: 603 in all, 587 in the static prefix, minimum 512
breakpoint after: policy
next request, other customer: prefix identical: True
after a policy edit: prefix identical: False
without the policy: breakpoint None because 21 tokens is under 512
```
```typescript
// A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.
//
// The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
// then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
// Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
// the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
// breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.

export type Module = { name: string; static: boolean; text: string };
export type Prompt = { blocks: Module[]; tokens: number; prefix_tokens: number; breakpoint: number | null };

export const MIN_CACHEABLE = 512; // tokens, Claude Sonnet 5.5

export const POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. ".repeat(26);
export const MODULES: Module[] = [
  { name: "role", static: true, text: "You are the support assistant of Northwind Outfitters. Answer from the policy only." },
  { name: "policy", static: true, text: POLICY },
  { name: "customer", static: false, text: "Customer: {customer}. Tier: {tier}." },
  { name: "question", static: false, text: "Question: {question}" },
];

export const tokens = (text: string): number => Math.ceil(text.length / 4); // characters over four, rounded up

const fill = (text: string, variables: Record<string, string>) => text.replace(/\{(\w+)\}/g, (_m, name) => variables[name]);

/** Static modules first, in the order given, then the dynamic ones with their variables filled in. */
export function assemble(modules: Module[], variables: Record<string, string>): Prompt {
  const ordered = [...modules.filter((m) => m.static), ...modules.filter((m) => !m.static)];
  const blocks = ordered.map((m) => ({ ...m, text: m.static ? m.text : fill(m.text, variables) }));
  const prefix = blocks.filter((b) => b.static).reduce((sum, b) => sum + tokens(b.text), 0);
  const lastStatic = blocks.reduce((last, b, i) => (b.static ? i : last), -1);
  return { blocks, tokens: blocks.reduce((sum, b) => sum + tokens(b.text), 0), prefix_tokens: prefix, breakpoint: lastStatic >= 0 && prefix >= MIN_CACHEABLE ? lastStatic : null };
}

export function cachedPrefix(prompt: Prompt): string {
  return prompt.breakpoint === null ? "" : prompt.blocks.slice(0, prompt.breakpoint + 1).map((b) => b.text).join("");
}

const flag = (b: boolean) => (b ? "True" : "False");

function main() {
  const ana = { customer: "Ana", tier: "gold", question: "Can I return a gift card?" };
  const first = assemble(MODULES, ana);
  console.log("order:", first.blocks.map((b) => b.name).join(" > "));
  console.log(`tokens: ${first.tokens} in all, ${first.prefix_tokens} in the static prefix, minimum ${MIN_CACHEABLE}`);
  console.log("breakpoint after:", first.blocks[first.breakpoint!].name);
  const second = assemble(MODULES, { customer: "Ben", tier: "basic", question: "Where is my parcel?" });
  console.log("next request, other customer: prefix identical:", flag(cachedPrefix(second) === cachedPrefix(first)));
  const edited = MODULES.map((m) => (m.name === "policy" ? { ...m, text: m.text.replaceAll("200", "300") } : m));
  console.log("after a policy edit: prefix identical:", flag(cachedPrefix(assemble(edited, ana)) === cachedPrefix(first)));
  const short = assemble([MODULES[0], ...MODULES.slice(2)], { customer: "Ana", tier: "gold", question: "Hi" });
  console.log("without the policy: breakpoint", short.breakpoint === null ? "None" : short.breakpoint, "because", short.prefix_tokens, "tokens is under", MIN_CACHEABLE);
}

if (import.meta.main) main();
```
```text
order: role > policy > customer > question
tokens: 603 in all, 587 in the static prefix, minimum 512
breakpoint after: policy
next request, other customer: prefix identical: True
after a policy edit: prefix identical: False
without the policy: breakpoint None because 21 tokens is under 512
```
```java
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.
 *
 * <p>The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
 * then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
 * Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
 * the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
 * breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
 */
public final class PromptBudget {
    record Module(String name, boolean isStatic, String text) {}

    /** blocks in order, the estimated tokens, the tokens of the static prefix and the index of the breakpoint (null when there is none). */
    record Prompt(List<Module> blocks, int tokens, int prefixTokens, Integer breakpoint) {}

    static final int MIN_CACHEABLE = 512; // tokens, Claude Sonnet 5.5

    static final String POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. ".repeat(26);
    static final List<Module> MODULES = List.of(
        new Module("role", true, "You are the support assistant of Northwind Outfitters. Answer from the policy only."),
        new Module("policy", true, POLICY),
        new Module("customer", false, "Customer: {customer}. Tier: {tier}."),
        new Module("question", false, "Question: {question}"));

    /** The ceiling of characters over four. */
    static int tokens(String text) {
        return (text.length() + 3) / 4;
    }

    private static String fill(String text, Map<String, String> variables) {
        String out = text;
        for (Map.Entry<String, String> v : variables.entrySet()) out = out.replace("{" + v.getKey() + "}", v.getValue());
        return out;
    }

    /** Static modules first, in the order given, then the dynamic ones with their variables filled in. */
    static Prompt assemble(List<Module> modules, Map<String, String> variables) {
        List<Module> blocks = new ArrayList<>();
        for (Module m : modules) if (m.isStatic()) blocks.add(m);
        for (Module m : modules) if (!m.isStatic()) blocks.add(new Module(m.name(), false, fill(m.text(), variables)));
        int prefix = 0;
        int total = 0;
        int lastStatic = -1;
        for (int i = 0; i < blocks.size(); i++) {
            total += tokens(blocks.get(i).text());
            if (blocks.get(i).isStatic()) {
                prefix += tokens(blocks.get(i).text());
                lastStatic = i;
            }
        }
        return new Prompt(blocks, total, prefix, lastStatic >= 0 && prefix >= MIN_CACHEABLE ? lastStatic : null);
    }

    static String cachedPrefix(Prompt prompt) {
        if (prompt.breakpoint() == null) return "";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= prompt.breakpoint(); i++) out.append(prompt.blocks().get(i).text());
        return out.toString();
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) {
        Map<String, String> ana = Map.of("customer", "Ana", "tier", "gold", "question", "Can I return a gift card?");
        Prompt first = assemble(MODULES, ana);
        StringBuilder order = new StringBuilder();
        for (Module b : first.blocks()) order.append(order.length() == 0 ? "" : " > ").append(b.name());
        System.out.println("order: " + order);
        System.out.println("tokens: " + first.tokens() + " in all, " + first.prefixTokens() + " in the static prefix, minimum " + MIN_CACHEABLE);
        System.out.println("breakpoint after: " + first.blocks().get(first.breakpoint()).name());
        Prompt second = assemble(MODULES, Map.of("customer", "Ben", "tier", "basic", "question", "Where is my parcel?"));
        System.out.println("next request, other customer: prefix identical: " + py(cachedPrefix(second).equals(cachedPrefix(first))));
        List<Module> edited = new ArrayList<>();
        for (Module m : MODULES) edited.add(m.name().equals("policy") ? new Module(m.name(), true, m.text().replace("200", "300")) : m);
        System.out.println("after a policy edit: prefix identical: " + py(cachedPrefix(assemble(edited, ana)).equals(cachedPrefix(first))));
        List<Module> noPolicy = new ArrayList<>(MODULES);
        noPolicy.remove(1);
        Prompt shortPrompt = assemble(noPolicy, Map.of("customer", "Ana", "tier", "gold", "question", "Hi"));
        System.out.println("without the policy: breakpoint " + (shortPrompt.breakpoint() == null ? "None" : shortPrompt.breakpoint()) + " because " + shortPrompt.prefixTokens() + " tokens is under " + MIN_CACHEABLE);
    }
}
```
```text
order: role > policy > customer > question
tokens: 603 in all, 587 in the static prefix, minimum 512
breakpoint after: policy
next request, other customer: prefix identical: True
after a policy edit: prefix identical: False
without the policy: breakpoint None because 21 tokens is under 512
```
```kotlin
/**
 * A prompt assembled from modules in cache-friendly order, with a token budget and the cache breakpoint.
 *
 * The Claude documentation on prompt caching (read on 2026-10-04) says cache prefixes are created "in the following order: tools, system,
 * then messages" and that a prompt shorter than the model's minimum "cannot be cached, even if marked with cache_control" (512 tokens for
 * Claude Sonnet 5.5). The prompting guide says to put long documents "near the top of your prompt, above your query". This file orders
 * the modules of a request that way, estimates tokens as one per four characters (a rough rule, not the model's tokenizer), marks the
 * breakpoint after the last static module and shows which edits keep the cached prefix and which break it. No model is called.
 */
data class Module(val name: String, val isStatic: Boolean, val text: String)

/** The blocks in order, the estimated tokens, the tokens of the static prefix and the index of the breakpoint (null when there is none). */
data class Prompt(val blocks: List<Module>, val tokens: Int, val prefixTokens: Int, val breakpoint: Int?)

const val MIN_CACHEABLE = 512 // tokens, Claude Sonnet 5.5

val POLICY = "Refunds above 200 are approved by a supervisor. Gift cards are never refunded in cash. ".repeat(26)
val MODULES = listOf(
    Module("role", true, "You are the support assistant of Northwind Outfitters. Answer from the policy only."),
    Module("policy", true, POLICY),
    Module("customer", false, "Customer: {customer}. Tier: {tier}."),
    Module("question", false, "Question: {question}"),
)

/** The ceiling of characters over four. */
fun tokens(text: String): Int = (text.length + 3) / 4

private fun fill(text: String, variables: Map<String, String>): String = variables.entries.fold(text) { acc, (k, v) -> acc.replace("{$k}", v) }

/** Static modules first, in the order given, then the dynamic ones with their variables filled in. */
fun assemble(modules: List<Module>, variables: Map<String, String>): Prompt {
    val blocks = modules.filter { it.isStatic } + modules.filter { !it.isStatic }.map { it.copy(text = fill(it.text, variables)) }
    val prefix = blocks.filter { it.isStatic }.sumOf { tokens(it.text) }
    val lastStatic = blocks.indexOfLast { it.isStatic }
    return Prompt(blocks, blocks.sumOf { tokens(it.text) }, prefix, if (lastStatic >= 0 && prefix >= MIN_CACHEABLE) lastStatic else null)
}

fun cachedPrefix(prompt: Prompt): String = prompt.breakpoint?.let { prompt.blocks.take(it + 1).joinToString("") { b -> b.text } } ?: ""

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val ana = mapOf("customer" to "Ana", "tier" to "gold", "question" to "Can I return a gift card?")
    val first = assemble(MODULES, ana)
    println("order: " + first.blocks.joinToString(" > ") { it.name })
    println("tokens: ${first.tokens} in all, ${first.prefixTokens} in the static prefix, minimum $MIN_CACHEABLE")
    println("breakpoint after: " + first.blocks[first.breakpoint!!].name)
    val second = assemble(MODULES, mapOf("customer" to "Ben", "tier" to "basic", "question" to "Where is my parcel?"))
    println("next request, other customer: prefix identical: " + py(cachedPrefix(second) == cachedPrefix(first)))
    val edited = MODULES.map { if (it.name == "policy") it.copy(text = it.text.replace("200", "300")) else it }
    println("after a policy edit: prefix identical: " + py(cachedPrefix(assemble(edited, ana)) == cachedPrefix(first)))
    val short = assemble(MODULES.filter { it.name != "policy" }, mapOf("customer" to "Ana", "tier" to "gold", "question" to "Hi"))
    println("without the policy: breakpoint ${short.breakpoint ?: "None"} because ${short.prefixTokens} tokens is under $MIN_CACHEABLE")
}
```
```text
order: role > policy > customer > question
tokens: 603 in all, 587 in the static prefix, minimum 512
breakpoint after: policy
next request, other customer: prefix identical: True
after a policy edit: prefix identical: False
without the policy: breakpoint None because 21 tokens is under 512
```
<!-- /example -->

Read the output from the top. The order is role, policy, customer, question. The static prefix is 587 of 603 tokens, so most of the request is cacheable. The next request from another customer has an identical prefix, and an edit to the policy does not. The last line is the failure that goes unseen without a count: with the policy removed, the breakpoint is `None` because 21 tokens is under 512.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Put today's date and the customer's name at the top of the system prompt, since the model reads it first."** It is tempting because the top feels like the place for the important facts. The exam rejects it: a changing value in the prefix breaks the cache on every request, so the stable content goes first and the variables go after the breakpoint.
2. **"Mark the system prompt for caching; the cost problem is then solved."** It is tempting because the marking is one line. The exam rejects it: a prefix below the model's minimum is not cached and reports no error, so the architect counts the tokens and reads the cache figures in the usage.
3. **"When the request is too long, truncate the end of the system prompt."** It is tempting because it keeps the request under the limit. The exam rejects it: static modules hold the rules and are never cut, so the design drops the lowest-priority dynamic context first and refuses a request that cannot fit.

## Quiz

1. Scenario: Egret Support's request holds fixed rules, a conversation history ranked 5 and marketing blurbs ranked 1. It exceeds its token budget by a small margin. What does the assembler do?
   - **a**: Truncates the end of the rules until the whole request fits within the budget
   - **b**: Drops the conversation history first, because it is the largest block of the three
   - **c**: Refuses the request at once, since it is over budget by any amount at all
   - **d**: Sheds the least important block first, stopping as soon as it fits

2. Scenario: Heron Retail has four teams that each write the support prompt by hand as one string, with a slightly different order, and the cache hit rate is low. Which change helps most?
   - **a**: A reminder in the team wiki that the policy text ought to come first in the prompt
   - **b**: A longer cache lifetime, so that the different strings are all kept for longer
   - **c**: A standard header that every team adds to the start of its own prompt
   - **d**: A single assembler that orders named modules, with callers passing only values

<details>
<summary>Answer key</summary>

1. **d**. The lowest-ranked dynamic block goes first, and the assembler climbs only while the request is still over. *a* is ruled out because "The static modules are never dropped". *b* is ruled out because "the assembler drops the lowest first", by rank and not by size. *c* is ruled out because refusal is for when "only static modules remain and they still exceed the budget, the assembler refuses the request".
2. **d**. One assembler fixes the order, so every team's prefix matches. *a* is ruled out because a reminder leaves the order to each caller, where "the assembler decides the order and callers cannot". *b* is ruled out because "Two requests share a cache entry only when everything up to the breakpoint is identical", and a longer lifetime does not make the strings identical. *c* is ruled out because a header does not fix the order that follows it: "The order is the same every time" only when one assembler decides it.

</details>
