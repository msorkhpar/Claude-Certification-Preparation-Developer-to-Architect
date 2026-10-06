# Practice: review a batch of assistant conversations

A team runs a conversational assistant for a book shop. It has a persona, remembers a customer between sessions and hands over to a person when it should. Before each release the team reads a batch
of conversations that reviewers have labelled, and decides whether the assistant may ship. In this practice you write that review: how many conversations the assistant settled alone, whether every
signal of risk reached a person as a safety hand-off, whether conversations ran too long, how often the assistant asked the same question twice, which kinds of conversation are weak, and the
verdict with its first reason. The model is not called: the tests give you the labelled batch. The shapes of the batch, of the policy and of the report are this course's own design for the
capstone, not an Anthropic interface. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`review`); TypeScript has the same name and the same snake-case fields. Java has the static method `ConversationReview.review` and the records `Conversation`, `Policy`,
`Segment` and `Report` with camel-case fields (`neededPerson`, `maxTurns`, `resolvedPct`); Kotlin has the top-level function `review` and the data classes of the same names.

## What is already written, and what you write

The starter is a working review with eight gaps cut out of it. The reading of the policy, the repeated count and the assembly of the report are written and correct. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file; a run shows the lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin names are the camel-case forms):

1. `percent` unlocks `e9` (and the figures of `m1` and `e1`): the whole percentage, rounded half up.
2. `settled` unlocks `e8`: whether the assistant settled a conversation alone.
3. `count_safety_missed` unlocks `e2`: risk signals that did not reach a person as a safety hand-off.
4. `count_overlong` unlocks `e3`: conversations above the turn limit that nobody took over.
5. `is_repeat_ok` unlocks `e4` (and `e1`): the repeated-question limit, met at the limit.
6. `count_escalations` unlocks `e7`: over- and under-escalation.
7. `segments_of` unlocks `e5` and `e6`: the segments and when one is weak.
8. `choose_verdict` unlocks `e1`, `e2` and the verdict of `m1`: the hold or ship decision and its reason.

About fifteen lines in all.

## What to write

A conversation is `{id, segment, turns, resolved, handoff, needed_person, repeated, risk}`: `handoff` is `none`, `requested` (the customer asked for a person), `safety` (a signal of risk was
routed to a person) or `stalled` (the assistant gave up after repeated misunderstanding); `resolved` says that the customer's problem was solved; `needed_person` is the reviewers' label that a
person was needed; `repeated` says that the assistant asked the same question twice; `risk` says that the customer said something that signals risk to their safety. The policy is
`{max_turns, max_repeat, min_resolved, min_n}`: the longest conversation without a hand-off, the most repeated questions in percent, the lowest resolution rate of a segment in percent, and the fewest
conversations that may justify calling a segment weak.

`review(conversations, policy)` returns a report with:

- `n`: the number of conversations. `resolved`: those that are `resolved` **and** have `handoff` `none` (a conversation a person settled is not one the assistant settled). `resolved_pct`: that count
  as a whole percentage rounded half up, `(200 * count + total) // (2 * total)`, and 0 when the total is 0.
- `safety_missed`: the conversations with `risk` whose `handoff` is not `safety`.
- `overlong`: the conversations with more than `max_turns` turns and `handoff` `none`.
- `repeat_pct`: the percentage with `repeated`, rounded the same way. `repeat_ok`: true when `repeated * 100 <= max_repeat * n` (true for an empty batch).
- `over_escalated`: the hand-offs (any `handoff` other than `none`) with no `needed_person` and no `risk`. `under_escalated`: the conversations with `handoff` `none` and `needed_person`.
- `segments`: one entry per segment, sorted by name, `{segment, n, resolved, percent, weak}` with `resolved` counted as above. `weak` is true when the segment has at least `min_n` conversations and
  `resolved * 100 < min_resolved * n` for that segment.
- `verdict` and `reason`: `hold` with `no_data` for an empty batch; otherwise the first that applies of `safety` (any `safety_missed`), `weak_segment` (any weak segment) and `repeats` (not `repeat_ok`),
  with verdict `hold`; when none applies the reason is `none` and the verdict is `ship`.

## Why each part is there, and what you should see

1. **A safety signal is a hand-off in code.** The assistant may be warm and still miss a signal of risk; a person has to receive it. *You should see* a risk that reached a person only because the
   customer asked for one counted as missed, and a hold whatever else is good.
2. **Limits are limits.** A limit is met at the limit. *You should see* exactly 12 turns acceptable and 13 not, exactly 10 percent repeated questions acceptable and 20 not, and a segment exactly at
   the resolution floor not weak.
3. **Small segments prove nothing.** A weak segment needs enough conversations behind it. *You should see* two unresolved conversations left alone and three called weak.
4. **Say who settled it.** A conversation a person took over is not an assistant success. *You should see* it left out of the resolved count.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A mixed batch gets every count, the segments and a verdict |
| `e1` | An empty batch has zero figures and is held for lack of data |
| `e2` | A safety signal counts as missed unless it went to a person as a safety hand-off |
| `e3` | A conversation is overlong only above the turn limit and only when nobody took over |
| `e4` | Repeated questions are acceptable at exactly the limit and not above it |
| `e5` | A segment is weak only below the resolution floor and not at it |
| `e6` | A segment needs the minimum number of conversations before it can be called weak |
| `e7` | A hand-off is over-escalation only when no person was needed and no safety signal was present |
| `e8` | Only conversations the assistant settled alone count as resolved |
| `e9` | Percentages are whole numbers rounded half up |
