# Practice: an agent that escalates for the right reasons, asks which customer is meant and hands off what the person needs

A support agent escalates when the customer sounds angry and carries on when it merely feels confident; it picks the first of three customers who match a name; it quietly resolves a
request that the policy never anticipated; and its hand-off to a person is a forty-turn transcript. In this practice you write the agent's decision rules and its hand-off: which of
resolve, clarify and escalate a case gets, which fields to ask a customer about when several records match, and what the hand-off says. The model is not called. It is in Python,
TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.

Names are Python's (`decide`, `clarifying_fields`, `handoff_text`); TypeScript has the camel-case names (`clarifyingFields`, `handoffText`); Java has the same camel-case names as static methods of
`Escalation` with the records the starter defines (`Case`, `Decision`, `HandoffCase`); Kotlin has top-level functions and data classes with default values. Python and TypeScript take plain
objects whose missing keys have the defaults shown below.

## What is already written, and what you write

The starter is a working escalation policy with seven gaps cut out of it. Everything that is plumbing is written and correct: the decision record, the order in which the rules are tried, the sentiment that sets the acknowledgement's reason text, the refusal of a hand-off without an id and an issue, and the lines for the customer, issue, root cause and amount. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The request for a person (unlocks `m1`, `e4`): a customer who asks for a person is escalated at once (`customer asked for a person`), even when the agent could resolve it and even when the match is ambiguous.
2. The several matches (unlocks `e3`): more than one matching record is clarified (`ambiguous customer match`), never guessed.
3. The request the policy does not cover (unlocks `e2`): a request the policy does not cover is escalated (`policy does not cover the request`); a covered one goes on.
4. The attempt limit (unlocks `e5`): when the attempts without progress reach the limit (2 by default) the case is escalated (`no progress`); below it, it is not.
5. The acknowledgement (unlocks `e1`): frustration alone does not escalate: the case is resolved and the reply acknowledges the feeling (`acknowledge` true) when the sentiment is not calm.
6. The clarifying fields (unlocks `e7`): the question names only the fields (other than `id`) whose values differ between the matches, so the customer can tell them apart; fewer than two matches have none.
7. The actions and the recommendation of the hand-off (unlocks `e8`): the hand-off lists the actions taken (joined with `; `, or `none`) and the recommended action (or `review the case`), as plain facts without the transcript.

`m1` needs gap 1. About eight lines in all. The steps below describe the whole policy, so you can see how your gaps are used.

## What to write

- `decide(case, max_attempts=2)` returns `{action, reason, acknowledge}`. A case has `asked_for_person` (false), `matches` (how many customer records matched, 1), `policy_covers` (whether the
  policy addresses the request, true), `attempts_without_progress` (0), `sentiment` (`calm`, `frustrated` or `angry`, `calm`) and `confidence` (the model's own score, 50). The rules apply in this order:
  1. a customer who asked for a person: `escalate`, reason `customer asked for a person`, at once;
  2. more than one matching record: `clarify`, reason `ambiguous customer match`;
  3. a policy that does not cover the request: `escalate`, reason `policy does not cover the request`;
  4. `attempts_without_progress` at or above `max_attempts`: `escalate`, reason `no progress`;
  5. otherwise `resolve`, reason `within capability`, with `acknowledge` true when the sentiment is not `calm`.
  `acknowledge` is false for every other action. Sentiment and confidence change nothing except that acknowledgement.
- `clarifying_fields(matches)` takes the matching records (maps of strings, each with an `id`) and returns the fields other than `id` on which the records are not all equal, in the order of the
  first record; fewer than two records give an empty list.
- `handoff_text(case)` takes `customer_id`, `issue`, `root_cause`, `amount`, `actions` (a list), `recommended` and `transcript`, and returns six lines: `Customer: ...`, `Issue: ...`,
  `Root cause: ...` (`unknown` when missing), `Amount: ...` (`unknown`), `Actions taken: ...` (the actions joined by `; `, or `none`) and `Recommended action: ...` (`review the case`). The
  transcript is never included. A case without a customer id or without an issue is an error.

## Why each part is there, and what you should see

1. **Honour the request.** A customer who asks for a person gets one without an investigation first. *You should see* `escalate` even when everything else says the agent could resolve it.
2. **Frustration is not a trigger.** An upset customer with a simple problem is helped, with an acknowledgement. *You should see* `resolve` with `acknowledge` true.
3. **A policy gap is a trigger.** The agent must not improvise a policy. *You should see* `escalate` when the policy is silent.
4. **Ambiguity is a question.** Several matches mean the agent asks for something that tells them apart. *You should see* `clarify` and only the fields that differ.
5. **Scores are not signals.** A confidence number or a mood is not the case's difficulty. *You should see* the same decision for every sentiment and confidence.
6. **A hand-off is a summary.** The person needs the facts and the recommendation, not forty turns. *You should see* six lines and no transcript.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A customer who asks for a person is escalated at once even when the agent could resolve it |
| `e1` | Frustration alone does not escalate and the reply acknowledges it |
| `e2` | A request the policy does not cover is escalated and a covered one is resolved |
| `e3` | Several matching records need a clarifying question and never a guess |
| `e4` | An explicit request for a person outranks an ambiguous match |
| `e5` | No progress after the attempt limit escalates and below it does not |
| `e6` | Sentiment and confidence scores never change the decision |
| `e7` | The clarifying question names only the fields that tell the matches apart |
| `e8` | The hand-off carries the structured facts and no transcript and refuses a case without an id |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
