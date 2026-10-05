# Practice: trimming, compacting and citing

A long conversation outgrows its window, and the cheapest cure is code you control: clear the tool output nobody needs again,
drop whole old turns, or replace the old part with a summary. The answer that comes out of a long document should also be
checked: a citation is a claim about where text came from, and a claim can be wrong. Write both halves. Pick your language folder
(`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the file there. The shapes come from the Claude
documentation on context windows, context editing, compaction and citations, read on 2026-10-03; the lesson pages explain them.
Nothing here touches the network: the messages are recorded or hand-written in the shape of the Messages API, and the summariser
is a function that the tests pass in.

## The given parts

| Name | Meaning |
|---|---|
| message | `{"role", "content"}`; the content is a string or a list of blocks: `text`, `tool_use` (`id`, `name`, `input`), `tool_result` (`tool_use_id`, `content` as a string, optional `is_error`) |
| `count_tokens(messages)` | a rough size, so that budgets are exact in the tests: 4 for every message, plus 1 for every 4 characters (rounded up) of text and of tool result content, plus 10 for every tool call |
| `summarise(messages)` | a function from the older messages to the summary text |
| citation | `{"type": "char_location", "cited_text", "document_index", "start_char_index", "end_char_index"}`; the span is `text[start:end]` of that document, end excluded |
| document | `{"title", "text"}` |

## What is already written, and what you write

The starter is the working module with seven gaps cut out of it. Everything else is written and correct: the token count, the turn test,
the cloning, the citation walk and the footnote text. Each gap is a small function with its signature, a comment that says what it
receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on
an assertion. To debug a gap, log its input with the `log` line at the top of the file (`split_turns` already logs its input at debug
level); a run shows the logged lines under the failing case. Write them in this order (the Java and Kotlin names are the camel-case forms):

1. `split_turns` unlocks `m1`, `e2` and `e3`: group the messages into turns.
2. `_clear_oldest` unlocks `e1`: replace the content of every result but the newest `keep`.
3. `_trim` unlocks `e2`: drop the oldest turns while the conversation is over budget, never the last.
4. `_leave_alone` unlocks `e3`: when compaction has nothing to do.
5. `_with_summary` unlocks `m1` and `e4`: put the summary block first in the first kept message.
6. `_span_problem` unlocks `e5`: `bad_range` or `text_mismatch` for a citation whose document exists.
7. `_number_for` unlocks `e6`: give a new span the next number.

About twenty lines in all. The sections below describe the whole module; the parts you do not write are there so you can see how your
functions are used.

## What to write

- `split_turns(messages)`: the messages as a list of turns. A turn starts at a user message that is not made only of tool result
  blocks (a string, or any other blocks) and runs up to the next such message, so a tool call and its result are always in one turn.
- `clear_tool_results(messages, keep=2, exclude=(), placeholder="[cleared]")`: a copy in which the content of every tool result is
  replaced by the placeholder, except the newest `keep` results. Results of a tool named in `exclude` are never cleared and do not
  count toward `keep` (the tool's name comes from the `tool_use` block with the same id). Calls, ids and `is_error` flags stay, and
  the input is not changed.
- `window(messages, budget, pin=False)`: drop the oldest whole turns until `count_tokens` fits the budget. The newest turn always
  stays, even when it alone is over budget. With `pin` the first turn stays too and the next oldest turns are dropped.
- `compact(messages, budget, summarise, keep_turns=1)`: when `count_tokens` is within the budget, or there are no more turns than
  `keep_turns`, return the messages unchanged and do not call `summarise`. Otherwise call `summarise` once with every message that
  comes before the newest `keep_turns` turns (an earlier summary is among them), and return the kept messages with the summary placed
  first in the first kept message: a text block `<summary>\n` + the summary + `\n</summary>`, followed by that message's own blocks
  (a string content becomes one text block). The roles keep alternating, and there is exactly one summary block in the result.
- `verify_citations(blocks, documents)`: for every citation of every text block, in order, report `{"block", "citation", "problem"}`
  when it cannot be trusted: `unsupported_type` (not `char_location`), `unknown_document` (index outside the list), `bad_range`
  (start below 0, end not after start, or end past the text), `text_mismatch` (the span is not the cited text). Check in that order.
- `footnotes(blocks, documents)`: the text blocks joined, with `[n]` right after the block of each citation, where `n` numbers the
  distinct cited spans (same document and offsets) from 1 in the order they first appear; then, if there are any, a blank line,
  `Sources:`, and one line `[n] <title>: "<cited text>"` per number. No citations gives just the text.

## The cases

| Id | What it checks |
|---|---|
| `m1` | An over-budget conversation becomes a summary and the newest turn |
| `e1` | Old tool results are cleared, but their calls and flags stay |
| `e2` | The window drops whole turns and never splits a tool call from its result |
| `e3` | A conversation within budget, or with nothing older, is left alone |
| `e4` | A second compaction folds the earlier summary into the new one |
| `e5` | A citation that does not match its document is reported |
| `e6` | Footnotes number each distinct source once, in order of appearance |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
