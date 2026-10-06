# Practice: a context that keeps the facts, trims the noise, never mixes customers and never splits a tool call

A support agent works with a customer over many turns. Its summaries turn "$129.50 by 30 September" into "a refund soon", every order lookup drops forty internal fields into the
conversation, a late message with an old address overwrites the new one, a facts block loaded for one customer shows up in another customer's session, and the window that trims old
messages sometimes keeps a tool result without the call that produced it. In this practice you write the context's building blocks: the trimming of a tool record, the update of case
facts, the assembly of the context, the check of a summary against the facts, and the sliding window. The model is not called. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`trim_record`, `update_facts`, `build_context`, `missing_from_summary`, `window`); TypeScript has the camel-case names (`trimRecord`, ...); Java has the same
camel-case names as static methods of `ContextBuilder` with the records the starter defines (`Fact`, `FactEntry`, `Message`); Kotlin has top-level functions and the same data classes.
A token is estimated as the length of the text divided by four, rounded up (`estimate_tokens`, given).

## What is already written, and what you write

The starter is a working context builder with six gaps cut out of it. Everything that is plumbing is written and correct: the token estimate, the copy of the facts before a change, the new-fact case, the summary and recent-messages sections, the join of the sections and the cost loop of the window. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The trimmed record (unlocks `m1`, `e1`): only the named fields are kept, with their exact values and in the order they are named; a named field the record does not have is skipped.
2. The newer fact (unlocks `e2`, `e3`): a fact that is as new as the stored one or newer replaces it and the replaced value is kept in `superseded` as `value@date`; an older fact that arrives late is only added to the history and does not replace the current value.
3. The customer's facts (unlocks `e4`): only the case facts of the customer the context is built for are used; those of another customer never enter it.
4. The case facts section (unlocks `e5`): when the customer has facts, a `## Case facts` section with one line `name: value (as of date)` each comes first, before the summary and the recent messages.
5. The values the summary lost (unlocks `e6`): the names of the facts whose exact value does not appear in the summary are returned, so a summary that lost a value is reported.
6. The tool call with its result (unlocks `e7`): a tool call and the result that follows it with the same id form one unit that the window keeps or drops whole; every other message is a unit of its own.

`m1` needs gap 1. About sixteen lines in all. The steps below describe the whole builder, so you can see how your gaps are used.

## What to write

- `trim_record(record, keep)` returns a new record with only the fields named in `keep`, in the order of `keep`, with their values unchanged. A name the record does not have is skipped.
- `update_facts(facts, name, value, as_of)` returns a new facts map (the input is not changed). A fact is `{value, as_of, superseded}` where `as_of` is an ISO date and `superseded` is a
  list of `"<value>@<date>"` strings. A new name is added with an empty history. When `as_of` is the same as or later than the stored one, the new value replaces the stored one and the
  old one is appended to the history. When it is earlier, the stored value stays and the late value goes into the history.
- `build_context(customer, facts, summary, recent)` takes fact entries `{customer, name, value, as_of}`, the summary text and the recent messages (`role`, `text`). It returns the text:
  `## Case facts` with one line `<name>: <value> (as of <date>)` for each fact of this customer only, in the given order (the section is left out when the customer has none), a blank line,
  `## Summary so far` with the summary, a blank line, and `## Recent messages` with one line `<role>: <text>` for each message.
- `missing_from_summary(summary, facts)` returns the names of the fact entries whose value does not appear verbatim in the summary, in order.
- `window(messages, budget)` takes messages `{role, kind, id, text}` where `kind` is `text`, `tool_use` or `tool_result`. It returns the newest messages, in their original order, whose
  estimated tokens add up to the budget at most. A tool call (a `tool_use` immediately followed by the `tool_result` with the same `id`) is kept whole or dropped whole. The window is one
  unbroken run from the newest message back: it stops at the first unit that does not fit.

## Why each part is there, and what you should see

1. **Trim before the context fills.** A lookup with forty fields costs tokens on every later turn although five fields matter. *You should see* only the named fields, exactly as returned.
2. **Newer information wins, history stays.** A customer's address changes mid-conversation, and a delayed older message must not undo it. *You should see* the new value current, the old one
   in the history, and a late older value recorded without replacing anything.
3. **Scope isolation.** A facts block is loaded for one customer's session. *You should see* another customer's facts absent from the text.
4. **Position and headings.** The facts come first, under a heading, so that they are not lost in the middle of a long input. *You should see* the three sections in that order.
5. **Summaries lose numbers.** An exact amount or date that a summary rounds off is a defect that no reader notices. *You should see* the names of the facts the summary no longer holds.
6. **A window that respects tool calls.** A tool result without its call is not a valid conversation. *You should see* a call and its result kept together or dropped together.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Trimming keeps only the named fields with their exact values in the named order |
| `e1` | A field that the record does not have is skipped |
| `e2` | A newer fact replaces the old one and the old value is kept as history |
| `e3` | An older fact that arrives late does not replace the current one |
| `e4` | The case facts of another customer never enter the context |
| `e5` | The context puts case facts first, then the summary, then the recent messages |
| `e6` | A summary that loses an exact value is reported |
| `e7` | The window drops the oldest messages and keeps a tool call with its result |
