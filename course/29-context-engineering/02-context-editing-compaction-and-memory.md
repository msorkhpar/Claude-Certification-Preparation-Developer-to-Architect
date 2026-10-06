# Context editing, compaction and memory

**Level:** Developer · **Module 29:** Context engineering · **Page 2 of 3**
**Exams:** DV4; A5.1

**After this page you can** ask the API to clear old tool results by rule, say how server-side compaction differs from clearing and from
a summariser of your own, read the beta headers and the fields that report what happened, and place the memory tool next to the other
two in a long-running agent.

Checked against the Claude API documentation (Context editing, Compaction, Compaction on demand, Compaction at a token threshold and
Memory tool) on 2026-10-03, and by running the editing request of the example offline in the course container (`anthropic` 1.11.0,
`@anthropic-ai/sdk` 0.131.0). **Context editing and both compaction forms are beta features**, so the headers and the parameter names
below are the ones documented on that date and can change. The example's replies are illustrative bodies, not captures.

## Why it matters

The previous page did the trimming in your own code. Anthropic also offers the same operations on the server, and an application can
use them with a few lines of configuration instead of a few hundred lines of history code. The cost is a beta header and a
dependence on the platform's behaviour. The exam asks which tool fits which problem, and what each one reports.

## The idea

### Context editing: clear by rule

"Context editing allows you to selectively clear specific content from conversation history as it grows." It is enabled with a beta
header, `context-management-2025-06-27`, and a `context_management` parameter that holds a list of edits. The strategy for tool
results is `clear_tool_uses_20250919`: "Automatically clears the oldest tool results in chronological order" and replaces each with
placeholder text. By default only results are cleared; the call's parameters stay unless `clear_tool_inputs` is set, and the `tool_use` block itself stays. Its parameters:

| Parameter | Default | Meaning |
|---|---|---|
| `trigger` | 100,000 input tokens | when clearing starts; `input_tokens` or `tool_uses` |
| `keep` | 3 tool uses | how many recent tool use and result pairs stay |
| `clear_at_least` | none | the least number of tokens to clear each time |
| `exclude_tools` | none | tool names whose results are never cleared |
| `clear_tool_inputs` | `false` | whether the call's parameters are cleared with the result |

`clear_at_least` exists for the cache: clearing "invalidates cached prompt prefixes when content is cleared", so a small clear that
pays a cache write for nothing is not worth doing. A second strategy, `clear_thinking_20251015`, manages thinking blocks. The
documentation says context editing is available on all supported Claude models.

The response says what happened. A `context_management` field holds `applied_edits`, one entry per edit, with the type, the number of
tool uses cleared and the number of input tokens cleared. The example's request sets a trigger of 30,000 input tokens, keeps three tool
uses and excludes `web_search`, and its scripted reply reports eight tool uses and 50,000 input tokens cleared; those numbers are the
documentation's own example. For streaming, the edits arrive in the final `message_delta` event.

This is the server-side form of the page-1 rule `clear_tool_results(keep, exclude)`, with one difference that matters: the server
decides when, from the trigger, and your program sees only the report.

### Compaction: summarise on the server

"Compaction replaces the older turns of a conversation with a summary that Claude writes on the server, so you need no summarization code
of your own. It keeps a long conversation or agent task inside the context window, and it keeps the active context small, because
response quality degrades as a conversation grows." For long conversations and agent workflows the context windows page calls it
"the primary strategy for context management". The documentation compares three ways to compact:

| | On demand | At a token threshold | Your own summariser |
|---|---|---|---|
| Who decides when | you, by sending a request | the API, when input tokens reach the trigger | you |
| Code you write | a loop that requests the summary and swaps it in | one parameter on ordinary requests | the summarization call and the history rewrite |
| Beta header | `compact-2026-09-04` and the `compaction` parameter | `compact-2026-01-12` | none |
| Runs in the background | yes | no, it runs inside the request that reaches the threshold | yes, in your code |

The documentation's advice is "Use on-demand compaction wherever it is available."

**On demand.** You send the conversation with `"compaction": {"type": "summarize"}`. The API returns "a single `compaction` block" and
no reply, with `stop_reason` of `"compaction"`. The block holds the summary and a signature, and "Send it in future requests exactly as it
came." It goes first in `messages`, in place of the messages it summarizes. Three rules from the page: the block comes first; the summarized
messages are removed, or the request returns a 400 error (`compaction_block_misplaced`); and exactly one block is sent per request. Two
mistakes raise no error: summarized messages left after the block are sent to Claude again, and a later request that leaves the block
out gives Claude no summary. A summary is produced only when the call ends normally with text; otherwise the response is a 200 with
empty content, so check `stop_reason` first. A non-blank `instructions` string replaces the default summarization prompt.

**At a token threshold.** One parameter on ordinary requests: an edit of type `compact_20260112` in `context_management`, with a
`trigger` of `input_tokens` (default 150,000, minimum 50,000), an optional `instructions`, and `pause_after_compaction`. When
the threshold is reached the response carries a `compaction` block, and you "must pass the `compaction` block back to the API on
subsequent requests by appending the entire response content to your messages". Usage needs care: the top-level `input_tokens` and
`output_tokens` "do **not** include compaction iteration usage"; sum across the `usage.iterations` array to find what was billed.

Both forms list the same family of models, including Claude Fable 5.1, Claude Opus 5.5 and Claude Sonnet 5.5. What a summary
cannot carry is stated for on-demand compaction: "Images, documents, `container_upload` blocks, and fetched URLs inside the summarized
messages are gone once the block replaces them. Restate or re-upload anything a later turn still needs."

### The memory tool: state outside the window

"The memory tool lets Claude store and retrieve information across conversations in a directory of memory files." It is a client-side
tool: "Claude requests file operations, and your application executes them." The `tools` entry is
`{"type": "memory_20250818", "name": "memory"}`, with no schema of your own. The commands are `view`, `create`, `str_replace`,
`insert`, `delete` and `rename`, all inside a `/memories` prefix that your handler maps onto real storage.

The purpose is "just-in-time context retrieval": "Rather than loading all relevant information up front, an agent records what it
learns in memory files and reads them back on demand." The API adds a protocol to the system prompt, beginning "ALWAYS VIEW YOUR MEMORY
DIRECTORY BEFORE DOING ANYTHING ELSE", and warns the model that "Your context window might be reset at any moment".

Because your code executes every operation, security is yours. A path such as `/memories/../../secrets.env` "can reach files outside the
`/memories` directory. Your implementation must validate every path in every command to prevent directory traversal attacks." Also cap
file sizes and expire old files. A failed operation is an ordinary error result with `is_error` set to `true`.

### Choosing and combining

Context editing clears by rule, as above. On compaction the memory tool page says: "Compaction automatically summarizes the whole
conversation on the server when the conversation approaches the context window limit." And for long-running agents, "consider using both:
compaction keeps the active context small without client-side bookkeeping, and memory preserves the information that must survive
summarization." A compaction request cannot be combined with `context_management` on the same request.

| Need | Tool |
|---|---|
| Old tool output is noise, the calls should stay | context editing, or your own clearing |
| The whole history is too long | compaction, on demand or at a threshold |
| Facts must survive a summary or a new session | the memory tool |
| Exact control of what a summary keeps | `instructions`, or your own summariser |

## Traps

1. **Sending the compaction block back altered.** The signature and content must be exactly as returned. A block with extra fields such
   as `citations: null` is rejected.
2. **Leaving summarized messages in front of the block.** The request fails with `compaction_block_misplaced`. Remove them.
3. **Reading the top-level usage after threshold compaction.** It leaves out the compaction call. Sum `usage.iterations`.
4. **A memory handler without path checks.** Every path must be confined to `/memories`.

## Quiz

1. A team wants old tool output cleared once the conversation passes a size, while the calls stay. Which feature and header fit?
   - **a**: Compaction on demand, enabled with `compact-2026-09-04`
   - **b**: Context editing, enabled with `context-management-2025-06-27`
   - **c**: Threshold compaction, enabled with `compact-2026-01-12`
   - **d**: The memory tool, enabled with `memory_20250818` and no header

2. A response arrives after the API summarized a conversation at its threshold, and the application reads only the top-level counts. What is missing?
   - **a**: Nothing, since the top-level fields cover every call
   - **b**: The tokens of the final reply, which sit in the content blocks
   - **c**: The cache tokens, which are kept in a separate response header
   - **d**: The tokens of the extra call that wrote the digest

3. An agent must remember a customer's preference across two sessions. Which feature does the page name for that?
   - **a**: Clearing tool results, which keeps facts in the window
   - **b**: The memory tool, with files that the application stores
   - **c**: Threshold compaction, which stores summaries on the server for good
   - **d**: A longer system prompt, since the window is reset between sessions

<details>
<summary>Answer key</summary>

1. **b**. The page says "Context editing allows you to selectively clear specific content from conversation history as it grows." It is enabled with the header `context-management-2025-06-27`. *a* is ruled out because on-demand compaction summarizes: "Compaction replaces the older turns of a conversation with a summary". *c* is ruled out because for the threshold form "the response carries a `compaction` block", a summary and not a clearing. *d* is ruled out because the memory tool is "state outside the window", and does not clear anything.
2. **d**. The page says the top-level fields "do **not** include compaction iteration usage", and to "sum across the `usage.iterations` array". *b* is ruled out because the total is found by the rule "to find what was billed", which adds the entries and not the content blocks. *c* is ruled out because the page says to "sum across the `usage.iterations` array", and names no response header. *a* is ruled out because of the sentence "do **not** include compaction iteration usage".
3. **b**. The page says "The memory tool lets Claude store and retrieve information across conversations in a directory of memory files." *a* is ruled out because clearing is for "Old tool output is noise, the calls should stay", not for facts that must survive. *c* is ruled out because the memory tool exists for facts that must survive summarization: "memory preserves the information that must survive summarization". *d* is ruled out because the page warns that "Your context window might be reset at any moment", so a prompt cannot hold state.

</details>
