# Citations, the Files API and the practice

**Level:** Developer · **Module 29:** Context engineering · **Page 3 of 3**
**Exams:** DV4; A5.1

**After this page you can** ask Claude to cite the passages behind an answer, read the three kinds of citation location, check a citation
against the document it points at, say which feature a citation cannot be combined with, upload a file once and reference it by id, and name
the workspace rule that makes file ids sensitive.

Checked against the Claude API documentation (Citations, Search results and Files API) on 2026-10-03, and by running the example and the
practice offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). The example's replies are illustrative,
hand-written bodies in the shape of the citations and context editing pages, not captures.

## Why it matters

An answer drawn from a long document is only as useful as the reader's ability to check it. Citations give each claim a pointer into the
source, and a file uploaded once can serve many requests without being sent again. Both are small features with sharp edges: a combination that
returns a 400 error, an id that crosses a tenant boundary. The exam asks about the edges.

## The idea

### Citations

"Claude can provide detailed citations when answering questions about documents, helping you track and verify the sources behind each
response." You provide documents and set `citations.enabled=true` on each: "Currently, citations must be enabled on all or none of the
documents within a request." Only text is citable: "Image citations are not yet possible." The response then holds several text blocks, each
with a claim and a list of citations. Where a citation points depends on the document type:

| Document | Chunking | Citation location |
|---|---|---|
| Plain text | sentences | character index range (0-indexed), type `char_location` |
| PDF | sentences | page number range (1-indexed), type `page_location` |
| Custom content | your blocks, "used as-is" | content block index range (0-indexed), type `content_block_location` |

"Character indices are 0-indexed with exclusive end indices", and document indices are 0-indexed over every document block in the request.
Each citation carries a `cited_text`. The `title` and `context` fields of a document are passed to the model but are not citable.

The documentation lists three advantages over asking the model in the prompt to quote its sources. "`cited_text` does not count toward your
output tokens", and "When passed back in subsequent conversation turns, `cited_text` is also not counted toward input tokens."
Reliability: "citations are guaranteed to contain valid pointers to the provided documents." And quality: in Anthropic's evaluations the
feature "is significantly more likely to cite the most relevant quotes". There is a small cost: enabling citations "incurs a slight
increase in input tokens because of system prompt additions and document chunking".

For retrieval, the documentation gives a rule: "if you want Claude to be able to cite specific sentences from your RAG chunks, you should
put each RAG chunk into a plain text document." The `search_result` block of module 28 is the other route and follows the same
all-or-nothing rule.

### The one combination that fails

"Citations and structured outputs are incompatible." If citations are enabled on any user-provided document (`document` or
`search_result` blocks) and the request also has `output_config.format`, "the API returns a 400 error". The reason is stated in the page:
"citations require interleaving citation blocks with text output, which is incompatible with the strict JSON schema constraints of
structured outputs." A design that needs both has to separate them: cite in one call, and shape the result in a second call that has no
documents, or ask for an `evidence` field and check it with the program, as the extractor of module 25 does.

### Check a citation yourself

The API's pointers are valid, and the program still should not take a citation as proof. A citation is a claim about where text came
from, and your program may sit between the API and the reader: a stored copy, a transformed document, a citation that came back
through a model's prose, or an offset used to highlight a span in a user interface. The practice's `verify_citations` reports, in this
order of checks, four problems:

| Problem | Meaning |
|---|---|
| `unsupported_type` | the citation is not a `char_location` |
| `unknown_document` | the document index is outside the list |
| `bad_range` | start below 0, end not after start, or end past the text |
| `text_mismatch` | the span `text[start:end]` is not the cited text |

`footnotes` then turns a cited answer into readable text: `[n]` after the block of each citation, `n` numbering the distinct cited spans
from 1 in order of first appearance, and a `Sources:` list with one line per number. The same span cited twice has one number.

### The Files API

"The Files API lets you upload and manage files to use with the Claude API without re-uploading content with each request." It provides a
"create-once, use-many-times approach": upload a file and receive a `file_id`, reference it in a Messages request in place of the content,
and list, retrieve or delete it. The Files API is generally available and "needs no beta header"; requests that still send the old
`files-api-2025-04-14` header keep working with the old response shapes. A PDF or a text file goes in a `document` block whose source is
`{"type": "file", "file_id": ...}`, an image goes in an `image` block, and the `document` block can carry `citations`.

The facts that matter:

- Limits: "Maximum file size: 500 MB per file" and "Total storage: 1 TB per organization".
- Cost: the operations are free, and "File content used in Messages requests is priced as input tokens." A referenced file still fills the
  window like any other input.
- Lifetime: "Files persist until you delete them", or until their `expires_at`, which you can set at upload with `expires_in_seconds`
  between one hour and 90 days. "Files cannot be modified or renamed after upload."
- A file that is larger than the window fails: "Exceeds context window size (400)".
- Downloads: only "files that are created by skills or the code execution tool" can be downloaded. A file you uploaded cannot.

The warning to remember is about who can see a file: "**Uploaded files are accessible to your entire workspace, not scoped to an end
user, conversation, or session.**" Any key with access to the workspace can use any file in it. So "Never accept `file_id` values from end
users or other untrusted sources: a user-supplied file ID would let one user of your application read content that another user
uploaded." Keep the mapping from users to files in your application, and give each tenant its own workspace: "The workspace is the
isolation boundary for files".

### The example

The example does three things offline. It builds a conversation with five tool results and clears all but the newest two, showing the size
before and after. It sends the editing request of page 2 through the scripted client and reads the `applied_edits` report. And it
checks a cited answer, then tampers with one `cited_text` to show the check failing.

<!-- example: m29-context-trimming tabs: python,typescript -->
```python
"""Clearing old tool results, asking the API to clear them, and checking the citations in an answer.

The replies are illustrative, hand-written bodies in the shapes of the context editing and citations pages (claude-sonnet-5-5),
not captures; the numbers in the context editing response are the documentation's own example.
"""
import copy

from harness import scripted_client
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
POLICY = "The grass is green. The sky is blue. Water is essential for life."


def tokens(messages):
    """A rough size: 4 per message, 1 per 4 characters of text, 10 per tool call."""
    total = 0
    for m in messages:
        total += 4
        for b in m["content"] if isinstance(m["content"], list) else [{"type": "text", "text": m["content"]}]:
            total += {"text": lambda: (len(b.get("text", "")) + 3) // 4, "tool_result": lambda: (len(b["content"]) + 3) // 4, "tool_use": lambda: 10}[b["type"]]()
    return total


def conversation():
    messages = [{"role": "user", "content": "Find every mention of the grass in the logs."}]
    for i in range(1, 6):
        messages.append({"role": "assistant", "content": [{"type": "tool_use", "id": f"toolu_{i}", "name": "grep_logs", "input": {"pattern": f"grass-{i}"}}]})
        messages.append({"role": "user", "content": [{"type": "tool_result", "tool_use_id": f"toolu_{i}", "content": f"log line {i}: " + "x" * 400}]})
    messages.append({"role": "assistant", "content": [{"type": "text", "text": "Found them all."}]})
    return messages


def clear_tool_results(messages, keep=2, placeholder="[cleared]"):
    """A copy in which every tool result but the newest `keep` has its content replaced; the calls stay."""
    out = copy.deepcopy(messages)
    results = [b for m in out if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_result"]
    for block in results[:max(len(results) - keep, 0)]:
        block["content"] = placeholder
    return out


def verify(blocks, documents):
    """A citation is a claim about where text came from; check it against the document."""
    bad = []
    for i, block in enumerate(blocks):
        for j, cite in enumerate(block.get("citations") or []):
            if documents[cite["document_index"]][cite["start_char_index"]:cite["end_char_index"]] != cite["cited_text"]:
                bad.append({"block": i, "citation": j, "problem": "text_mismatch"})
    return bad


def footnotes(blocks, titles):
    numbers, lines, out = {}, [], ""
    for block in blocks:
        out += block["text"]
        for cite in block.get("citations") or []:
            key = (cite["document_index"], cite["start_char_index"], cite["end_char_index"])
            if key not in numbers:
                numbers[key] = len(numbers) + 1
                lines.append(f'[{numbers[key]}] {titles[cite["document_index"]]}: "{cite["cited_text"]}"')
            out += f"[{numbers[key]}]"
    return out + ("\n\nSources:\n" + "\n".join(lines) if lines else "")


def cite(start, end):
    return {"type": "char_location", "cited_text": POLICY[start:end], "document_index": 0, "document_title": "Policy", "start_char_index": start, "end_char_index": end, "file_id": None}


EDITS = {"edits": [{"type": "clear_tool_uses_20250919", "trigger": {"type": "input_tokens", "value": 30000}, "keep": {"type": "tool_uses", "value": 3},
                    "clear_at_least": {"type": "input_tokens", "value": 5000}, "exclude_tools": ["web_search"]}]}


def editing_reply():
    body = message([text("Found them all.")], model=MODEL)
    body["context_management"] = {"applied_edits": [{"type": "clear_tool_uses_20250919", "cleared_tool_uses": 8, "cleared_input_tokens": 50000}]}
    return body


def cited_reply():
    blocks = [{"type": "text", "text": "The grass is green. ", "citations": [cite(0, 19)]}, {"type": "text", "text": "Water matters. ", "citations": [cite(37, 65)]},
              {"type": "text", "text": "Green again.", "citations": [cite(0, 19)]}]
    return message(blocks, model=MODEL)


def main():
    before = conversation()
    after = clear_tool_results(before, 2)
    cleared = sum(1 for m in after if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_result" and b["content"] == "[cleared]")
    print(f"conversation: {len(before)} messages, 5 tool results, about {tokens(before)} tokens")
    print(f"after clearing all but the newest 2 results: about {tokens(after)} tokens, {cleared} results replaced, calls kept: {[m for m in after if m['role'] == 'assistant'] == [m for m in before if m['role'] == 'assistant']}")
    client, transport = scripted_client(editing_reply(), cited_reply())
    reply = client.beta.messages.create(model=MODEL, max_tokens=300, messages=before, betas=["context-management-2025-06-27"], context_management=EDITS)
    print("beta header sent:", transport.headers[0]["anthropic-beta"])
    edit = transport.requests[0]["context_management"]["edits"][0]
    print("edit sent:", edit["type"], "trigger", edit["trigger"]["value"], "keep", edit["keep"]["value"], "exclude", edit["exclude_tools"])
    applied = reply.context_management.applied_edits[0]
    print("applied edit reported:", applied.type, f"cleared {applied.cleared_tool_uses} tool uses, {applied.cleared_input_tokens} input tokens")
    document = {"type": "document", "source": {"type": "text", "media_type": "text/plain", "data": POLICY}, "title": "Policy", "citations": {"enabled": True}}
    answer = client.messages.create(model=MODEL, max_tokens=300, messages=[{"role": "user", "content": [document, {"type": "text", "text": "What does the policy say about grass and water?"}]}])
    blocks = [b.model_dump() for b in answer.content]
    print("citations enabled in the request:", transport.requests[1]["messages"][0]["content"][0]["citations"])
    print("citation problems:", verify(blocks, [POLICY]))
    tampered = copy.deepcopy(blocks)
    tampered[1]["citations"][0]["cited_text"] = "Water is optional."
    print("after tampering with one cited_text:", verify(tampered, [POLICY]))
    print(footnotes(blocks, ["Policy"]))


if __name__ == "__main__":
    main()
```
```text
conversation: 12 messages, 5 tool results, about 628 tokens
after clearing all but the newest 2 results: about 328 tokens, 3 results replaced, calls kept: True
beta header sent: context-management-2025-06-27
edit sent: clear_tool_uses_20250919 trigger 30000 keep 3 exclude ['web_search']
applied edit reported: clear_tool_uses_20250919 cleared 8 tool uses, 50000 input tokens
citations enabled in the request: {'enabled': True}
citation problems: []
after tampering with one cited_text: [{'block': 1, 'citation': 0, 'problem': 'text_mismatch'}]
The grass is green. [1]Water matters. [2]Green again.[1]

Sources:
[1] Policy: "The grass is green."
[2] Policy: "Water is essential for life."
```
```typescript
// Clearing old tool results, asking the API to clear them, and checking the citations in an answer.
// The replies are illustrative, hand-written bodies in the shapes of the context editing and citations pages (claude-sonnet-5-5),
// not captures; the numbers in the context editing response are the documentation's own example.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
export const POLICY = "The grass is green. The sky is blue. Water is essential for life.";

type Msg = { role: string; content: string | any[] };

/** A rough size: 4 per message, 1 per 4 characters of text, 10 per tool call. */
export function tokens(messages: Msg[]): number {
  let total = 0;
  for (const m of messages) {
    total += 4;
    for (const b of typeof m.content === "string" ? [{ type: "text", text: m.content }] : m.content) {
      total += b.type === "text" ? Math.floor(((b.text ?? "").length + 3) / 4) : b.type === "tool_result" ? Math.floor((b.content.length + 3) / 4) : 10;
    }
  }
  return total;
}

export function conversation(): Msg[] {
  const messages: Msg[] = [{ role: "user", content: "Find every mention of the grass in the logs." }];
  for (let i = 1; i <= 5; i++) {
    messages.push({ role: "assistant", content: [{ type: "tool_use", id: `toolu_${i}`, name: "grep_logs", input: { pattern: `grass-${i}` } }] });
    messages.push({ role: "user", content: [{ type: "tool_result", tool_use_id: `toolu_${i}`, content: `log line ${i}: ` + "x".repeat(400) }] });
  }
  messages.push({ role: "assistant", content: [{ type: "text", text: "Found them all." }] });
  return messages;
}

/** A copy in which every tool result but the newest `keep` has its content replaced; the calls stay. */
export function clearToolResults(messages: Msg[], keep = 2, placeholder = "[cleared]"): Msg[] {
  const out: Msg[] = structuredClone(messages);
  const results = out.flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b) => b.type === "tool_result");
  for (const block of results.slice(0, Math.max(results.length - keep, 0))) block.content = placeholder;
  return out;
}

/** A citation is a claim about where text came from; check it against the document. */
export function verify(blocks: any[], documents: string[]) {
  const bad: Array<{ block: number; citation: number; problem: string }> = [];
  blocks.forEach((block, i) => (block.citations ?? []).forEach((cite: any, j: number) => {
    if (documents[cite.document_index].slice(cite.start_char_index, cite.end_char_index) !== cite.cited_text) bad.push({ block: i, citation: j, problem: "text_mismatch" });
  }));
  return bad;
}

export function footnotes(blocks: any[], titles: string[]): string {
  const numbers = new Map<string, number>();
  const lines: string[] = [];
  let out = "";
  for (const block of blocks) {
    out += block.text;
    for (const cite of block.citations ?? []) {
      const key = `${cite.document_index}:${cite.start_char_index}:${cite.end_char_index}`;
      if (!numbers.has(key)) {
        numbers.set(key, numbers.size + 1);
        lines.push(`[${numbers.get(key)}] ${titles[cite.document_index]}: "${cite.cited_text}"`);
      }
      out += `[${numbers.get(key)}]`;
    }
  }
  return out + (lines.length ? "\n\nSources:\n" + lines.join("\n") : "");
}

export const cite = (start: number, end: number) => ({ type: "char_location", cited_text: POLICY.slice(start, end), document_index: 0, document_title: "Policy", start_char_index: start, end_char_index: end, file_id: null });

export const EDITS = { edits: [{ type: "clear_tool_uses_20250919", trigger: { type: "input_tokens", value: 30000 }, keep: { type: "tool_uses", value: 3 },
  clear_at_least: { type: "input_tokens", value: 5000 }, exclude_tools: ["web_search"] }] };

const usage = { input_tokens: 1, output_tokens: 1 };
export const editingReply = () => ({ body: { ...message([text("Found them all.")], "end_turn", usage, MODEL), context_management: { applied_edits: [{ type: "clear_tool_uses_20250919", cleared_tool_uses: 8, cleared_input_tokens: 50000 }] } } });
export const citedBlocks = () => [{ type: "text", text: "The grass is green. ", citations: [cite(0, 19)] }, { type: "text", text: "Water matters. ", citations: [cite(37, 65)] }, { type: "text", text: "Green again.", citations: [cite(0, 19)] }];
export const citedReply = () => ({ body: message(citedBlocks(), "end_turn", usage, MODEL) });

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

const py = (v: any): string => {
  if (v === null || v === undefined) return "None";
  if (typeof v === "boolean") return v ? "True" : "False";
  if (typeof v === "string") return `'${v}'`;
  if (Array.isArray(v)) return `[${v.map(py).join(", ")}]`;
  if (typeof v === "object") return `{${Object.entries(v).map(([k, x]) => `${py(k)}: ${py(x)}`).join(", ")}}`;
  return String(v);
};

async function main() {
  const before = conversation();
  const after = clearToolResults(before, 2);
  const cleared = after.flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b) => b.type === "tool_result" && b.content === "[cleared]").length;
  const same = JSON.stringify(after.filter((m) => m.role === "assistant")) === JSON.stringify(before.filter((m) => m.role === "assistant"));
  console.log(`conversation: ${before.length} messages, 5 tool results, about ${tokens(before)} tokens`);
  console.log(`after clearing all but the newest 2 results: about ${tokens(after)} tokens, ${cleared} results replaced, calls kept: ${py(same)}`);
  const { fake, client } = clientFor([editingReply(), citedReply()]);
  const reply: any = await client.beta.messages.create({ model: MODEL, max_tokens: 300, messages: before as any, betas: ["context-management-2025-06-27"], context_management: EDITS as any });
  console.log("beta header sent:", fake.seen[0].headers["anthropic-beta"]);
  const edit = fake.seen[0].body.context_management.edits[0];
  console.log("edit sent:", edit.type, "trigger", edit.trigger.value, "keep", edit.keep.value, "exclude", py(edit.exclude_tools));
  const applied = reply.context_management.applied_edits[0];
  console.log("applied edit reported:", applied.type, `cleared ${applied.cleared_tool_uses} tool uses, ${applied.cleared_input_tokens} input tokens`);
  const document = { type: "document" as const, source: { type: "text" as const, media_type: "text/plain" as const, data: POLICY }, title: "Policy", citations: { enabled: true } };
  const answer = await client.messages.create({ model: MODEL, max_tokens: 300, messages: [{ role: "user", content: [document, { type: "text", text: "What does the policy say about grass and water?" }] }] });
  const blocks = answer.content as any[];
  console.log("citations enabled in the request:", py(fake.seen[1].body.messages[0].content[0].citations));
  console.log("citation problems:", py(verify(blocks, [POLICY])));
  const tampered = structuredClone(blocks);
  tampered[1].citations[0].cited_text = "Water is optional.";
  console.log("after tampering with one cited_text:", py(verify(tampered, [POLICY])));
  console.log(footnotes(blocks, ["Policy"]));
}

if (import.meta.main) await main();
```
```text
conversation: 12 messages, 5 tool results, about 628 tokens
after clearing all but the newest 2 results: about 328 tokens, 3 results replaced, calls kept: True
beta header sent: context-management-2025-06-27
edit sent: clear_tool_uses_20250919 trigger 30000 keep 3 exclude ['web_search']
applied edit reported: clear_tool_uses_20250919 cleared 8 tool uses, 50000 input tokens
citations enabled in the request: {'enabled': True}
citation problems: []
after tampering with one cited_text: [{'block': 1, 'citation': 0, 'problem': 'text_mismatch'}]
The grass is green. [1]Water matters. [2]Green again.[1]

Sources:
[1] Policy: "The grass is green."
[2] Policy: "Water is essential for life."
```
<!-- /example -->

Read the output. The conversation has 12 messages and about 628 tokens by the example's rough counter; clearing all but the newest two
results brings it to about 328 and replaces three results, with every call kept. The request carried the beta header
`context-management-2025-06-27` and an edit with a trigger of 30,000, a keep of 3 and `web_search` excluded. The reply reported 8 tool uses
and 50,000 input tokens cleared. The citation check found no problem in the real answer and a `text_mismatch` after the tampering, and the
footnotes number the grass sentence once although two blocks cite it.

Java and Kotlin readers: the practice follows in your language, and its functions are plain list and map code.

## The practice: trimming, compacting and citing

You write `split_turns`, `clear_tool_results`, `window`, `compact`, `verify_citations` and `footnotes`. The statement is in
`exercises/29-context-engineering/unit-01/practice-1/statement.md`; each language folder has a `starter`, the tests and a build file, and the
starter fails every test. The summariser is a function that the tests pass in.

| Id | What it checks |
|---|---|
| `m1` | An over-budget conversation becomes a summary and the newest turn |
| `e1` | Old tool results are cleared, but their calls and flags stay |
| `e2` | The window drops whole turns and never splits a tool call from its result |
| `e3` | A conversation within budget, or with nothing older, is left alone |
| `e4` | A second compaction folds the earlier summary into the new one |
| `e5` | A citation that does not match its document is reported |
| `e6` | Footnotes number each distinct source once, in order of appearance |

Case `e2` is the pairing rule of page 1 in code, and `e4` is the "exactly one summary" rule: the second compaction receives the first
summary among the messages it folds.

## Traps

1. **Citations with structured outputs.** The request fails with a 400 error. Separate the two steps.
2. **Citations on some documents only.** All or none: mixed settings are an error.
3. **A file id from a user's request.** Files belong to the workspace, not to a user. Map users to files yourself, and never trust an id that
   arrives from outside.
4. **Assuming an uploaded file can be downloaded.** Only files created by skills or the code execution tool can.

## Quiz

1. A request enables citations on one document and includes `output_config.format` with a JSON schema. What happens?
   - **a**: A 400 error comes back
   - **b**: The reply is JSON with the citations inside the schema's fields
   - **c**: The citations are dropped, and the schema is applied alone
   - **d**: The schema is dropped, and the citations come back alone

2. An application sends the id of an uploaded file straight from a web form into a Messages request. What is the risk?
   - **a**: Someone could read another customer's material in the same workspace
   - **b**: The file would be deleted as soon as the request had been read
   - **c**: The request would be billed at twice the usual input price
   - **d**: The file would be moved to the account of the user who sent the id

3. A citation reports a span from character 40 to character 30 in a document of 100 characters. Which problem does the practice's check name?
   - **a**: `unknown_document`, since the index is outside the list
   - **b**: `text_mismatch`, since the cited text is not in the document
   - **c**: `bad_range`, since the end is not after the start
   - **d**: `unsupported_type`, since the citation is not a character range

<details>
<summary>Answer key</summary>

1. **a**. The page says "Citations and structured outputs are incompatible", and "the API returns a 400 error". *b* is ruled out because "citations require interleaving citation blocks with text output, which is incompatible with the strict JSON schema constraints of structured outputs." *c* is ruled out because the request does not run at all: "the API returns a 400 error". *d* is ruled out because "the API returns a 400 error", so a rejected request returns no citations.
2. **a**. The page says "a user-supplied file ID would let one user of your application read content that another user uploaded." *b* is ruled out because "Files persist until you delete them". *c* is ruled out because "File content used in Messages requests is priced as input tokens", at the usual rate. *d* is ruled out because the page describes read access in a shared space, since files are "accessible to your entire workspace", and says nothing of moving them.
3. **c**. The page lists `bad_range` as "start below 0, end not after start, or end past the text". *b* is ruled out because the checks run "in this order of checks", and the range check comes before the text comparison. *a* is ruled out because `unknown_document` means "the document index is outside the list", and this citation names a range. *d* is ruled out because `unsupported_type` means "the citation is not a `char_location`", and a character range is one.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. An agent prunes old tool results by rule, and the team also needs a customer's stated wish to survive a summary and a new session. Which pair fits?
   - **a**: Context editing for the clearing, and a longer system prompt for the wish
   - **b**: Context editing for the clearing, and memory files for that fact
   - **c**: Citations for the clearing, and the Files API for the wish
   - **d**: Threshold compaction for the clearing, and prompt caching for the wish

2. A request to compact a conversation on demand returns a `compaction` block. What must the next request do with it?
   - **a**: Drop its signature, so that the summary can be edited by the application
   - **b**: Append it after the summarized messages, which stay in the list as before
   - **c**: Send it first in the messages, in place of the ones it summarizes, exactly as it came
   - **d**: Send it in the system prompt, where summaries are expected to sit

3. A pipeline needs a cited answer and a result in a fixed JSON shape from the same documents. What design does the module support?
   - **a**: One call with the schema inside each document's `title` field
   - **b**: One call with both features, since each one works on its own
   - **c**: One call with citations on half of the documents and off for the rest
   - **d**: Two steps: one call that cites, and a second that only formats

4. A program drops messages from a long history and the next request fails with a 400 error. Which cause fits the module?
   - **a**: The newest turn was kept even though it was over the budget
   - **b**: A tool result stayed while its call was removed
   - **c**: The first turn was pinned so that the task stayed in view
   - **d**: An older summary was folded into the new one as required

<details>
<summary>Answer key</summary>

1. **b**. The page pairs them: "consider using both: compaction keeps the active context small without client-side bookkeeping, and memory preserves the information that must survive summarization." *a* is ruled out because "Your context window might be reset at any moment", so a prompt cannot hold state. *c* is ruled out because the Files API stores files and does not clear anything: "upload and manage files". *d* is ruled out because prompt caching changes what you pay: "prompt caching changes what you pay for those tokens, not whether they count."
2. **c**. The page says "It goes first in `messages`, in place of the messages it summarizes", and "Send it in future requests exactly as it came." *b* is ruled out because summarized messages left after the block "are sent to Claude again", with no error. *a* is ruled out because a block with a changed signature is rejected: "The signature and content must be exactly as returned." *d* is ruled out because the block belongs in `messages`, not elsewhere: "the block comes first".
3. **d**. The page says "cite in one call, and shape the result in a second call that has no documents". *b* is ruled out because "Citations and structured outputs are incompatible." *c* is ruled out because "citations must be enabled on all or none of the documents within a request." *a* is ruled out because "`title` and `context` fields of a document are passed to the model but are not citable", and a schema there is not applied.
4. **b**. The page says "Tool result blocks must immediately follow their corresponding tool use blocks in the message history", and that a list which keeps one without the other "fails with a 400 error". *a* is ruled out because "The newest turn always stays, even if it alone is over the budget." *c* is ruled out because a pinned turn is a whole turn: "Optionally pin the first turn". *d* is ruled out because a second compaction "should fold the first summary into the new one".

</details>
