# Descriptions, names and boundaries: what a model chooses a tool from

**Level:** Architect · **Module 52:** Designing tool interfaces · **Page 1 of 2**
**Exams:** A2.1; S1; S3

**After this page you can** say what a model has to go on when it chooses a tool, write a description that separates a tool from its neighbours, rename or split a tool whose contract is unclear, decide between splitting and consolidating by the contract, name the checks that a schema can carry (formats, enums, valid examples), and spot a system prompt that pulls the model toward one tool.

Checked on 2026-10-03 against the Claude API documentation page "Define tools", Anthropic's engineering article "Writing tools for agents", the Model Context Protocol specification (the Tools page of version 2026-07-28) and the Agent SDK pages on custom tools and tool search. The example runs offline in Python and TypeScript; it calls no model. This page deepens module 26 (the tool definition and `tool_choice`) and module 32 (tools in an MCP server), and it does not repeat them. The rules in the example and the practice are this course's own, built on that advice and labelled as such: the guide and the documentation give no thresholds.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the description is the main mechanism by which a model picks a tool, and a thin one makes selection unreliable among similar tools; a good description holds input formats, example requests, edge cases and the boundary against its neighbours; near-identical descriptions cause misrouting (its example is `analyze_content` next to `analyze_document`); keyword-sensitive wording in the system prompt can tie a request to the wrong tool; and the fixes it lists are rewriting descriptions, renaming a tool so that its purpose is plain (`analyze_content` to `extract_web_results`) and splitting a generic tool into purpose-specific ones with their own contracts (`analyze_document` into `extract_data_points`, `summarize_content` and `verify_claim_against_source`). Its sample question asks for the first step when two lookup tools with one-line descriptions are confused, and keys the expanded descriptions over examples in the prompt, a routing layer or one merged tool. *What the current product says (documentation checked 2026-10-03):* the same on descriptions: "Provide extremely detailed descriptions. This is by far the most important factor in tool performance." On the number of tools it leans the other way: "Consolidate related operations into fewer tools. Rather than creating a separate tool for every action (`create_pr`, `review_pr`, `merge_pr`), group them into a single tool with an `action` parameter." The two fit once you ask what a tool's contract is (below). On the exam, answer a confused pair with better descriptions first and a split or a merge after; in your own code, count contracts, not tools.

## Why it matters

Scenario S1 gives a support agent `get_customer`, `lookup_order`, `process_refund` and `escalate_to_human`. Scenario S3 gives a research system a web search agent and a document agent, and a pair of tools that both "analyze" something. In both, nothing in the code decides which tool the model calls: the model reads the tool texts and chooses, and when two texts look alike it chooses badly and does not know that it did. The failure shows up as wrong refunds and mixed-up sources, long after the tool set was written. The cheapest place to prevent it is the text itself, and the exam asks you to know what belongs in it.

## The idea

### The model chooses from text

The API turns your tool definitions into part of the prompt: "When you call the Claude API with the `tools` parameter, the API constructs a special system prompt from the tool definitions, tool configuration, and any user-specified system prompt." A tool is therefore a name, a description and a schema, and the model reads all three each time it decides. That is why a tool has no other way to be told apart from its neighbour: the model has no view of your code.

### What a description holds

The documentation lists it. A good description explains what the tool does, "When it should be used (and when it shouldn't)", what each parameter means and how it affects the behaviour, and "Any important caveats or limitations, such as what information the tool does not return if the tool name is unclear". It adds a size: "Aim for at least 3–4 sentences for each tool description, more if the tool is complex." The exam guide adds input formats, example requests and edge cases. Compare the documentation's two versions of one tool:

```text
Poor:  Gets the stock price for a ticker.
Good:  Retrieves the current stock price for a given ticker symbol. The ticker symbol must be a valid symbol for a publicly traded
       company on a major US stock exchange like NYSE or NASDAQ. The tool will return the latest trade price in USD. It should be
       used when the user asks about the current or most recent price of a specific stock. It will not provide any other
       information about the stock or company.
```

The good one says what comes back and what does not, which is the boundary: the sentence about what the tool will not provide tells the model that a question about the company is for another tool. The course's lint rules turn this into checks that a script can make: at least three sentences, the words `use when`, a boundary phrase (`do not use`, `not for` or `instead of`), and a description on every parameter. They are small on purpose. A description that passes them can still be poor, but a description that fails them is certainly thin.

### Examples, formats and closed sets in the schema

Not everything belongs in prose. The schema carries what a script can check:

- A parameter with a closed set of values is an `enum`, not a sentence that says "one of open, shipped or closed". The model sees the allowed values in the schema, and a strict tool (module 26) refuses the others.
- `input_examples` are example inputs "valid according to the tool's `input_schema`", and "Invalid examples return a 400 error". They help with nested objects and format-sensitive inputs, at a price: "~20–50 tokens for simple examples, ~100–200 tokens for complex nested objects". A lint that runs each example against the schema keeps them honest when the schema changes.
- Every parameter has its own description, because the model decides what to put in a field from its description alone.
- A parameter must not ask for the model's reasoning. The documentation's advice is "Ask for an explanation, not reasoning": "A parameter that asks for the model's thinking or step-by-step reasoning may lead to a `reasoning_extraction` refusal." A field called `reasoning` is a defect the lint can find.
- The name matches `^[a-zA-Z0-9_-]{1,128}$`. The MCP specification's rule is similar and a little wider (it also allows a dot), so a name that fits both is safe in either.

### Overlap makes the model guess

Two tools whose texts say nearly the same thing leave the model nothing to choose by, and `analyze_content` next to `analyze_document` is the guide's example. The sample question turns on the cause: both lookup tools had minimal descriptions and accepted similar identifiers, so the model had no way to tell a question about an order from a question about a customer. The fix is aimed at the cause. Three tempting answers fail for reasons the exam states:

- Examples in the system prompt cost tokens on every request and leave the descriptions as thin as they were.
- A routing layer bypasses the model's own reading of the request, and replaces a one-line fix with a component that has to be built and kept right.
- Merging both lookups into one tool that works out what the identifier means is a legitimate design, but it is a larger change than a first step needs, and it hides two contracts behind one name.

The order of repair is: first make each description say what it does, what it takes and where it stops; then rename a tool whose name no longer says its purpose; then split or merge if the contracts are wrong. The first two cost a line each.

### Names and prefixes

A name is the first thing the model reads. `lookup_order` says more than `query`, and a prefix by service makes a family easy to tell apart. The documentation: "When your tools span multiple services or resources, prefix names with the service (for example, `github_list_prs`, `slack_send_message`)." The article adds that namespacing "can help delineate boundaries between lots of tools; MCP clients sometimes do this by default", and that prefix and suffix schemes can differ in effect by model, so test it. The MCP specification adds a hazard for aggregators: tool names are unique per server, so a client that combines servers "SHOULD implement a disambiguation strategy such as prefixing tool names with a server identifier". Claude Code does this for you: an MCP tool is `mcp__<server>__<tool>` (module 55).

### Split or consolidate: count contracts

The guide splits a generic `analyze_document` into three tools; the documentation groups `create_pr`, `review_pr` and `merge_pr` into one. They do not conflict, because they answer different faults.

| Fault | Fix | Why |
|---|---|---|
| One tool serves requests with different inputs and different outputs (extract numbers, summarise, verify a claim) | Split into tools with one contract each | The model cannot say what a call to the generic tool will return, and the description cannot be both broad and exact |
| Several tools are the same operation on one resource, with the same inputs and outputs, differing in a verb | Group under one tool with an `action` parameter | The model has fewer names to confuse, and each call has the same shape |
| Two tools do different jobs but read alike | Rewrite the descriptions and rename | The contracts are right; the text is not |

The article's advice goes a step further, towards tools shaped for the task: "Instead of implementing a `list_users`, `list_events`, and `create_event` tools, consider implementing a `schedule_event` tool which finds availability and schedules an event." and "More tools don't always lead to better outcomes." A tool is a unit of choice for the model, so the right number is the number of different decisions you want it to make.

### The system prompt can overrule the descriptions

The prompt and the tool texts are read together. An instruction such as "Always check the customer's account first" contains a keyword that now pulls every order question toward `get_customer`, however clear the description of `lookup_order` is. The guide names this as keyword-sensitive wording that creates unintended tool associations. When a model keeps choosing one tool, read the system prompt for the words of that tool's description and for any "always" and "first" rules about a particular tool, and move the sequencing rule into code (module 48) where it is a rule and not a nudge.

### What Java and Kotlin teams use

Nothing differs: a tool definition is data, and the graded practice of this module is in all four languages. Write the definitions in the language of your client and run the same checks on them.

### The example

The example lints the guide's confusing pair, shows the repaired set of four tools that each have one contract, and pages a long result (page 2).

<!-- example: m52-tool-set-lint tabs: python,typescript,java,kotlin -->
```python
"""Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.

No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
page of the Claude API documentation and the "Writing tools for agents" article.
"""
import base64
import re

OVERLAP = 0.6


def lint(tool):
    text = tool["description"].lower()
    found = []
    if not any(p in text for p in ("do not use", "not for", "instead of")):
        found.append("no-boundary")
    if "use when" not in text:
        found.append("no-use-when")
    if any(not d.strip() for d in tool["params"].values()):
        found.append("param-undescribed")
    if len(re.findall(r"[.!?](?:\s|$)", tool["description"])) < 3:
        found.append("short-description")
    return found


def overlap(a, b):
    wa, wb = set(re.findall(r"[a-z]{3,}", a["description"].lower())), set(re.findall(r"[a-z]{3,}", b["description"].lower()))
    return len(wa & wb) / len(wa | wb)


def report(title, tools):
    print(title)
    for tool in tools:
        print(f"  {tool['name']}: {', '.join(lint(tool)) or 'clean'}")
    for i, a in enumerate(tools):
        for b in tools[i + 1:]:
            score = overlap(a, b)
            if score >= OVERLAP:
                print(f"  overlap: {a['name']} and {b['name']} ({score:.2f})")


def page(items, cursor=None, limit=4):
    offset = 0 if cursor is None else int(base64.b64decode(cursor).decode().split(":")[1])
    chunk = items[offset:offset + limit]
    more = offset + len(chunk) < len(items)
    token = base64.b64encode(f"offset:{offset + len(chunk)}".encode()).decode() if more else None
    note = f"Showing {len(chunk)} of {len(items)} results; pass next_cursor to continue, or narrow the query with a filter." if more else None
    return chunk, token, note


POOR = [
    {"name": "analyze_content", "description": "Analyzes content and returns the result.", "params": {"content": "The content."}},
    {"name": "analyze_document", "description": "Analyzes a document and returns the result.", "params": {"document": ""}},
]

SPLIT = [
    {"name": "extract_web_results", "params": {"url": "The page address."},
     "description": "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those."},
    {"name": "extract_data_points", "params": {"document_id": "The id of an uploaded document."},
     "description": "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those."},
    {"name": "summarize_content", "params": {"text": "The text to shorten.", "max_words": "The longest summary, in words."},
     "description": "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that."},
    {"name": "verify_claim_against_source", "params": {"claim": "One sentence to test.", "source_id": "The id of the source to test it against."},
     "description": "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that."},
]


def main():
    report("the set as first written", POOR)
    print()
    report("the same job, one contract per tool", SPLIT)
    rows = [f"row-{i:02d}" for i in range(25)]
    print("\na long result, paged four rows at a time")
    cursor = None
    for number in (1, 2):
        chunk, cursor, note = page(rows, cursor)
        print(f"  page {number}: {' '.join(chunk)}")
        print(f"    note: {note}")
    print(f"  the cursor is opaque: {cursor}")


if __name__ == "__main__":
    main()
```
```text
the set as first written
  analyze_content: no-boundary, no-use-when, short-description
  analyze_document: no-boundary, no-use-when, param-undescribed, short-description
  overlap: analyze_content and analyze_document (0.71)

the same job, one contract per tool
  extract_web_results: clean
  extract_data_points: clean
  summarize_content: clean
  verify_claim_against_source: clean

a long result, paged four rows at a time
  page 1: row-00 row-01 row-02 row-03
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  page 2: row-04 row-05 row-06 row-07
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  the cursor is opaque: b2Zmc2V0Ojg=
```
```typescript
// Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.
//
// No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
// neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
// page of the Claude API documentation and the "Writing tools for agents" article.
const OVERLAP = 0.6;

type Tool = { name: string; description: string; params: Record<string, string> };

export function lint(tool: Tool): string[] {
  const text = tool.description.toLowerCase();
  const found: string[] = [];
  if (!["do not use", "not for", "instead of"].some((p) => text.includes(p))) found.push("no-boundary");
  if (!text.includes("use when")) found.push("no-use-when");
  if (Object.values(tool.params).some((d) => !d.trim())) found.push("param-undescribed");
  if ((tool.description.match(/[.!?](?:\s|$)/g) ?? []).length < 3) found.push("short-description");
  return found;
}

export function overlap(a: Tool, b: Tool): number {
  const wa = new Set(a.description.toLowerCase().match(/[a-z]{3,}/g)), wb = new Set(b.description.toLowerCase().match(/[a-z]{3,}/g));
  const shared = [...wa].filter((w) => wb.has(w)).length;
  return shared / new Set([...wa, ...wb]).size;
}

function report(title: string, tools: Tool[]) {
  console.log(title);
  for (const tool of tools) console.log(`  ${tool.name}: ${lint(tool).join(", ") || "clean"}`);
  tools.forEach((a, i) => {
    for (const b of tools.slice(i + 1)) {
      const score = overlap(a, b);
      if (score >= OVERLAP) console.log(`  overlap: ${a.name} and ${b.name} (${score.toFixed(2)})`);
    }
  });
}

export function page(items: string[], cursor: string | null = null, limit = 4): [string[], string | null, string | null] {
  const offset = cursor === null ? 0 : Number(Buffer.from(cursor, "base64").toString().split(":")[1]);
  const chunk = items.slice(offset, offset + limit);
  const more = offset + chunk.length < items.length;
  const token = more ? Buffer.from(`offset:${offset + chunk.length}`).toString("base64") : null;
  const note = more ? `Showing ${chunk.length} of ${items.length} results; pass next_cursor to continue, or narrow the query with a filter.` : null;
  return [chunk, token, note];
}

export const POOR: Tool[] = [
  { name: "analyze_content", description: "Analyzes content and returns the result.", params: { content: "The content." } },
  { name: "analyze_document", description: "Analyzes a document and returns the result.", params: { document: "" } },
];

export const SPLIT: Tool[] = [
  { name: "extract_web_results", params: { url: "The page address." },
    description: "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those." },
  { name: "extract_data_points", params: { document_id: "The id of an uploaded document." },
    description: "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those." },
  { name: "summarize_content", params: { text: "The text to shorten.", max_words: "The longest summary, in words." },
    description: "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that." },
  { name: "verify_claim_against_source", params: { claim: "One sentence to test.", source_id: "The id of the source to test it against." },
    description: "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that." },
];

function main() {
  report("the set as first written", POOR);
  console.log();
  report("the same job, one contract per tool", SPLIT);
  const rows = Array.from({ length: 25 }, (_, i) => `row-${String(i).padStart(2, "0")}`);
  console.log("\na long result, paged four rows at a time");
  let cursor: string | null = null;
  for (const number of [1, 2]) {
    let note: string | null;
    let chunk: string[];
    [chunk, cursor, note] = page(rows, cursor);
    console.log(`  page ${number}: ${chunk.join(" ")}`);
    console.log(`    note: ${note}`);
  }
  console.log(`  the cursor is opaque: ${cursor}`);
}

if (import.meta.main) main();
```
```text
the set as first written
  analyze_content: no-boundary, no-use-when, short-description
  analyze_document: no-boundary, no-use-when, param-undescribed, short-description
  overlap: analyze_content and analyze_document (0.71)

the same job, one contract per tool
  extract_web_results: clean
  extract_data_points: clean
  summarize_content: clean
  verify_claim_against_source: clean

a long result, paged four rows at a time
  page 1: row-00 row-01 row-02 row-03
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  page 2: row-04 row-05 row-06 row-07
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  the cursor is opaque: b2Zmc2V0Ojg=
```
```java
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.
 *
 * <p>No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
 * neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
 * page of the Claude API documentation and the "Writing tools for agents" article.
 */
public final class ToolLint {
    static final double OVERLAP = 0.6;

    /** A tool as the lint sees it: a name, a description and the description of each parameter. */
    record Tool(String name, String description, Map<String, String> params) {}

    /** One page of a long result: the rows, the opaque cursor of the next page (null on the last) and a note for the model. */
    record Page(List<String> rows, String cursor, String note) {}

    static List<String> lint(Tool tool) {
        String text = tool.description().toLowerCase();
        List<String> found = new ArrayList<>();
        if (!(text.contains("do not use") || text.contains("not for") || text.contains("instead of"))) found.add("no-boundary");
        if (!text.contains("use when")) found.add("no-use-when");
        if (tool.params().values().stream().anyMatch(d -> d.strip().isEmpty())) found.add("param-undescribed");
        Matcher m = Pattern.compile("[.!?](?:\\s|$)").matcher(tool.description());
        int sentences = 0;
        while (m.find()) sentences++;
        if (sentences < 3) found.add("short-description");
        return found;
    }

    private static Set<String> words(Tool t) {
        Set<String> out = new HashSet<>();
        Matcher m = Pattern.compile("[a-z]{3,}").matcher(t.description().toLowerCase());
        while (m.find()) out.add(m.group());
        return out;
    }

    static double overlap(Tool a, Tool b) {
        Set<String> wa = words(a), wb = words(b);
        Set<String> both = new HashSet<>(wa);
        both.retainAll(wb);
        Set<String> either = new HashSet<>(wa);
        either.addAll(wb);
        return (double) both.size() / either.size();
    }

    static void report(String title, List<Tool> tools) {
        System.out.println(title);
        for (Tool tool : tools) {
            List<String> found = lint(tool);
            System.out.println("  " + tool.name() + ": " + (found.isEmpty() ? "clean" : String.join(", ", found)));
        }
        for (int i = 0; i < tools.size(); i++) {
            for (Tool b : tools.subList(i + 1, tools.size())) {
                double score = overlap(tools.get(i), b);
                if (score >= OVERLAP) System.out.println("  overlap: " + tools.get(i).name() + " and " + b.name() + " (" + String.format(Locale.ROOT, "%.2f", score) + ")");
            }
        }
    }

    static Page page(List<String> items, String cursor, int limit) {
        int offset = cursor == null ? 0 : Integer.parseInt(new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8).split(":")[1]);
        List<String> chunk = items.subList(Math.min(offset, items.size()), Math.min(offset + limit, items.size()));
        boolean more = offset + chunk.size() < items.size();
        String token = more ? Base64.getEncoder().encodeToString(("offset:" + (offset + chunk.size())).getBytes(StandardCharsets.UTF_8)) : null;
        String note = more ? "Showing " + chunk.size() + " of " + items.size() + " results; pass next_cursor to continue, or narrow the query with a filter." : null;
        return new Page(chunk, token, note);
    }

    static Map<String, String> params(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    static final List<Tool> POOR = List.of(
        new Tool("analyze_content", "Analyzes content and returns the result.", params("content", "The content.")),
        new Tool("analyze_document", "Analyzes a document and returns the result.", params("document", "")));

    static final List<Tool> SPLIT = List.of(
        new Tool("extract_web_results",
            "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those.",
            params("url", "The page address.")),
        new Tool("extract_data_points",
            "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those.",
            params("document_id", "The id of an uploaded document.")),
        new Tool("summarize_content",
            "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that.",
            params("text", "The text to shorten.", "max_words", "The longest summary, in words.")),
        new Tool("verify_claim_against_source",
            "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that.",
            params("claim", "One sentence to test.", "source_id", "The id of the source to test it against.")));

    public static void main(String[] args) {
        report("the set as first written", POOR);
        System.out.println();
        report("the same job, one contract per tool", SPLIT);
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) rows.add(String.format("row-%02d", i));
        System.out.println("\na long result, paged four rows at a time");
        String cursor = null;
        for (int number = 1; number <= 2; number++) {
            Page p = page(rows, cursor, 4);
            cursor = p.cursor();
            System.out.println("  page " + number + ": " + String.join(" ", p.rows()));
            System.out.println("    note: " + (p.note() == null ? "None" : p.note()));
        }
        System.out.println("  the cursor is opaque: " + (cursor == null ? "None" : cursor));
    }
}
```
```text
the set as first written
  analyze_content: no-boundary, no-use-when, short-description
  analyze_document: no-boundary, no-use-when, param-undescribed, short-description
  overlap: analyze_content and analyze_document (0.71)

the same job, one contract per tool
  extract_web_results: clean
  extract_data_points: clean
  summarize_content: clean
  verify_claim_against_source: clean

a long result, paged four rows at a time
  page 1: row-00 row-01 row-02 row-03
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  page 2: row-04 row-05 row-06 row-07
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  the cursor is opaque: b2Zmc2V0Ojg=
```
```kotlin
import java.util.Base64

/**
 * Tool interfaces graded on rules, offline: a set that confuses a model, the same job split into tools with one contract each, and a long result paged.
 *
 * No model is called. The rules are the course's own and small: a description of three sentences or more, a when-to-use phrase, a boundary against the
 * neighbouring tool, a description on every parameter, and a pair of descriptions that overlap too much. Checked on 2026-10-03 against the "Define tools"
 * page of the Claude API documentation and the "Writing tools for agents" article.
 */
const val OVERLAP = 0.6

/** A tool as the lint sees it: a name, a description and the description of each parameter. */
data class Tool(val name: String, val description: String, val params: Map<String, String>)

/** One page of a long result: the rows, the opaque cursor of the next page (null on the last) and a note for the model. */
data class Page(val rows: List<String>, val cursor: String?, val note: String?)

fun lint(tool: Tool): List<String> {
    val text = tool.description.lowercase()
    val found = mutableListOf<String>()
    if (listOf("do not use", "not for", "instead of").none { it in text }) found += "no-boundary"
    if ("use when" !in text) found += "no-use-when"
    if (tool.params.values.any { it.isBlank() }) found += "param-undescribed"
    if (Regex("""[.!?](?:\s|$)""").findAll(tool.description).count() < 3) found += "short-description"
    return found
}

private fun words(t: Tool): Set<String> = Regex("[a-z]{3,}").findAll(t.description.lowercase()).map { it.value }.toSet()

fun overlap(a: Tool, b: Tool): Double {
    val wa = words(a)
    val wb = words(b)
    return (wa intersect wb).size.toDouble() / (wa union wb).size
}

fun report(title: String, tools: List<Tool>) {
    println(title)
    for (tool in tools) println("  ${tool.name}: ${lint(tool).joinToString(", ").ifEmpty { "clean" }}")
    for ((i, a) in tools.withIndex()) {
        for (b in tools.drop(i + 1)) {
            val score = overlap(a, b)
            if (score >= OVERLAP) println("  overlap: ${a.name} and ${b.name} (${"%.2f".format(java.util.Locale.ROOT, score)})")
        }
    }
}

fun page(items: List<String>, cursor: String? = null, limit: Int = 4): Page {
    val offset = if (cursor == null) 0 else String(Base64.getDecoder().decode(cursor)).split(":")[1].toInt()
    val chunk = items.drop(offset).take(limit)
    val more = offset + chunk.size < items.size
    val token = if (more) Base64.getEncoder().encodeToString("offset:${offset + chunk.size}".toByteArray()) else null
    val note = if (more) "Showing ${chunk.size} of ${items.size} results; pass next_cursor to continue, or narrow the query with a filter." else null
    return Page(chunk, token, note)
}

val POOR = listOf(
    Tool("analyze_content", "Analyzes content and returns the result.", mapOf("content" to "The content.")),
    Tool("analyze_document", "Analyzes a document and returns the result.", mapOf("document" to "")),
)

val SPLIT = listOf(
    Tool(
        "extract_web_results",
        "Pulls the title, date and main claims from one web page. Use when a search result needs to be read. Do not use it for uploaded files; use extract_data_points instead of this tool for those.",
        mapOf("url" to "The page address."),
    ),
    Tool(
        "extract_data_points",
        "Lists every figure and date in one uploaded document, each with its page. Use when a report or table must be mined for numbers. Not for web pages; call extract_web_results for those.",
        mapOf("document_id" to "The id of an uploaded document."),
    ),
    Tool(
        "summarize_content",
        "Writes a short summary of text you already hold. Use when a long passage must fit in a brief. Do not use it to check a claim; verify_claim_against_source does that.",
        mapOf("text" to "The text to shorten.", "max_words" to "The longest summary, in words."),
    ),
    Tool(
        "verify_claim_against_source",
        "Says whether one claim is supported by one named source and quotes the passage. Use when a figure or statement needs a check. Not for finding new sources; use extract_web_results instead of this tool for that.",
        mapOf("claim" to "One sentence to test.", "source_id" to "The id of the source to test it against."),
    ),
)

fun main() {
    report("the set as first written", POOR)
    println()
    report("the same job, one contract per tool", SPLIT)
    val rows = (0 until 25).map { "row-%02d".format(it) }
    println("\na long result, paged four rows at a time")
    var cursor: String? = null
    for (number in 1..2) {
        val p = page(rows, cursor)
        cursor = p.cursor
        println("  page $number: ${p.rows.joinToString(" ")}")
        println("    note: ${p.note ?: "None"}")
    }
    println("  the cursor is opaque: ${cursor ?: "None"}")
}
```
```text
the set as first written
  analyze_content: no-boundary, no-use-when, short-description
  analyze_document: no-boundary, no-use-when, param-undescribed, short-description
  overlap: analyze_content and analyze_document (0.71)

the same job, one contract per tool
  extract_web_results: clean
  extract_data_points: clean
  summarize_content: clean
  verify_claim_against_source: clean

a long result, paged four rows at a time
  page 1: row-00 row-01 row-02 row-03
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  page 2: row-04 row-05 row-06 row-07
    note: Showing 4 of 25 results; pass next_cursor to continue, or narrow the query with a filter.
  the cursor is opaque: b2Zmc2V0Ojg=
```
<!-- /example -->

Read the first block: the two `analyze` tools break three or four rules each, and their descriptions overlap by 0.71 of their words, above the lint's threshold of 0.6, which is the machine-readable form of "near-identical". The second block is the same job split by contract: web pages, uploaded documents, shortening text and checking a claim. Each description says when to use it and names the neighbour to use instead, and none overlaps another.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Add five to eight worked examples of order requests to the system prompt."** It is tempting because examples teach. The exam rejects it as a first step: they add tokens to every request and leave the descriptions as thin as they were. Fix the text the model chooses from.
2. **"Put a layer in front of each turn that picks the tool from keywords and identifier patterns."** It is tempting because code is deterministic. The exam rejects it: a routing layer bypasses the model's own reading of the request, and it is far more work than rewriting two descriptions.
3. **"Merge the two lookups into one `lookup_entity` tool."** It is tempting because one tool cannot be confused with itself. The exam rejects it as a first step: merging is a legitimate design but a larger change than a first step needs, and it hides two contracts. Expand the descriptions, then reconsider.
4. **"Tell the model in the prompt to always check the customer first."** It is tempting because it states the intent. The exam rejects it: the keyword pulls every request toward the customer tool, over the descriptions you wrote. Put an order of steps in code, and keep the prompt free of tool-specific triggers.

## Quiz

1. Logs show an agent often calls `get_customer` when users ask about orders, instead of `lookup_order`. Each tool has a one-line summary (“Retrieves customer information”, “Retrieves order details”) and accepts similar identifiers. What is the best first step?
   - **a**: Rewrite both texts with input shapes, sample requests and where each one stops
   - **b**: Add five to eight worked examples of order requests to the system prompt
   - **c**: Put a layer before each turn that picks the tool by keywords and id patterns
   - **d**: Merge both into a single `lookup_entity` tool that works out which backend to ask

2. Since the team added the sentence "Always check the customer's account first." to an agent's prompt, it calls `get_customer` before every order question, although the `lookup_order` description is clear. Which change most directly removes the unwanted pull?
   - **a**: Add a third tool that wraps both lookups behind one name
   - **b**: Add a routing layer in front of the model for order questions
   - **c**: Rephrase that rule so that no keyword ties it to one function
   - **d**: Cut each description down to a single sentence

<details>
<summary>Answer key</summary>

1. **a**. The cause is thin descriptions, and the first fix is to make each say what it takes and where it stops. *b* is ruled out because worked examples in the prompt "leave the descriptions as thin as they were" and cost tokens on every request. *c* is ruled out because "a routing layer bypasses the model's own reading of the request" and is a component to build. *d* is ruled out because merging "is a legitimate design but a larger change than a first step needs".
2. **c**. The keyword in the rule pulls the choice, so the rule is what to change. *b* is ruled out because a routing layer "bypasses the model's own reading of the request" and leaves the prompt as it was. *a* is ruled out because a wrapper adds a third name to choose from, and "More tools don't always lead to better outcomes." *d* is ruled out because the documentation says "Aim for at least 3–4 sentences for each tool description", and a shorter text separates the tools less.

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's.
