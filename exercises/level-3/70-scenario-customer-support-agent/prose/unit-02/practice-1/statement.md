# Practice: audit a support agent's sessions and name the first fix

A customer support resolution agent has been in production for a month. The team's target is that at least 80 percent of conversations are resolved at first
contact, and the agent must escalate the ones a person has to decide. The rate is below target, and each team member blames something else: the prompt, the tools,
the model. In this practice you write the audit that ends the argument: it reads the month's recorded sessions, counts what went wrong in each failure shape, and
names the one fix to make first. The model is not called: the tests give you recorded sessions. The audit, its failure shapes and its order of fixes are this
course's own design for the capstone, not an Anthropic interface. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`audit`); TypeScript has the same name and the same snake-case fields. Java has the static method `Audit.audit` and the records `Step`, `Session`
and `Report`; Kotlin has the top-level function `audit` and the data classes of the same names. In Java and Kotlin the fields are camel case (`needsHuman`,
`refundCents`, `meetsTarget`, `overEscalated`, `underEscalated`, `skippedPrerequisite`, `wrongTool`, `overLimitRefunds`).

## What is already written, and what you write

The starter is a working audit with five gaps cut out of it. The plumbing is written and correct: the session count, the resolved, over- and under-escalated counts, and `audit` itself, which calls the gaps and builds the report. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file; a run shows the lines under the failing case. Write them in this order (Java and Kotlin use the camel-case names):

1. `skipped_prerequisite` unlocks `e2`: the order of the steps and the failed identity check.
2. `has_wrong_tool` unlocks `e6`: a known right tool that differs from the tool used.
3. `is_over_limit` unlocks `e3`: a refund that was made above the limit.
4. `first_contact_rate` unlocks `e1` and `e5`: the rounded rate, and 0 for no sessions.
5. `diagnose` unlocks `e4` and the diagnosis of `m1` and `e1`: the order of the fixes.

About a dozen lines in all. The sections below describe the whole audit.

## What to write

A step is one call the agent made: `tool`, `ok` (whether it succeeded) and `right_tool`, the tool it should have used (absent when the right tool is not known). A session has
`id`, `steps`, `outcome` (`resolved` or `escalated`), `needs_human` (the ground truth: a person had to decide this case), `refund_cents` (the refund made, 0 for none)
and `limit_cents` (the limit above which a person decides).

`audit(sessions)` returns a report with these fields:

- `sessions`: how many; `resolved`: how many have the outcome `resolved`.
- `fcr`: `resolved` divided by `sessions`, rounded to three decimals (0 for no sessions); `meets_target`: `fcr` is at least 0.8.
- `over_escalated`: escalated although no person was needed; `under_escalated`: resolved although a person was needed.
- `skipped_prerequisite`: sessions in which a `lookup_order` or `process_refund` call came before the first successful `get_customer` call (a failed `get_customer` does not
  count as identifying anyone).
- `wrong_tool`: sessions with at least one step whose `right_tool` is known and differs from its `tool`.
- `over_limit_refunds`: sessions with the outcome `resolved` whose `refund_cents` is above `limit_cents`.
- `diagnosis`, the first fix, by this order: `enforce_in_code` when `skipped_prerequisite` or `over_limit_refunds` is above zero (money moves wrongly: nothing else comes
  first); otherwise `rewrite_tool_descriptions` when `wrong_tool` is above zero and at least `over_escalated` plus `under_escalated`; otherwise
  `write_escalation_criteria` when `over_escalated` plus `under_escalated` is above zero; otherwise `none`.

## Why each part is there, and what you should see

1. **Measure the target.** The exam gives a rate (55 percent against 80) and asks for the most effective way up. *You should see* the rate rounded and compared at the boundary.
2. **A prerequisite is an order.** Calling the order tool is not the fault; calling it before the customer was identified is. *You should see* the order of the steps read, and a
   failed check not counted as an identification.
3. **A refund over the limit is a fact about what happened.** A refund that was refused and handed to a person is the design working. *You should see* only made refunds counted.
4. **Failure shapes need different fixes.** Skipped steps and money moved wrongly call for a gate in code; a wrong tool call calls for better descriptions; a miscalibrated
   escalation calls for explicit criteria with examples. *You should see* the fixes in that order of urgency.
5. **Count sessions, not steps.** One confused conversation is one failure however many calls it holds. *You should see* sessions counted for the wrong tool.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A mixed set of six sessions gets every field right and the first fix |
| `e1` | No sessions: zero everywhere, a rate of 0, no diagnosis |
| `e2` | The prerequisite is the order of the steps, and a failed identity check does not open the gate |
| `e3` | Only a refund that was made counts as over the limit |
| `e4` | The order of fixes: code, then descriptions (a tie goes to descriptions), then criteria |
| `e5` | The rate at exactly 0.8 meets the target, 0.6 does not, 2 of 3 is 0.667, and over- and under-escalation are counted apart |
| `e6` | The wrong tool counts sessions and ignores steps whose right tool is not known |
