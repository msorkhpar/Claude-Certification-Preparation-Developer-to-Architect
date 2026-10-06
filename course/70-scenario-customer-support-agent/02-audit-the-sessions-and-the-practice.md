# Audit the sessions, order the fixes and the practice

**Level:** Architect · **Module 70:** Scenario: customer support agent · **Page 2 of 2**
**Exams:** A1, A2, A5; S1

**After this page you can** measure first-contact resolution and escalation calibration from recorded sessions, count each failure shape by session, order the fixes by consequence, write the audit that does it, and answer a question that combines the loop, the tools and the escalation of the support scenario.

Checked on 2026-10-04 against Anthropic's engineering article on building effective agents and the Architect exam guide (version 1.0, scenario 1). The practice is offline in Python, TypeScript, Java and Kotlin: it reads recorded sessions that the tests build, and it calls no model. The audit, its fields and its order of fixes are this course's own design for the capstone, not an Anthropic interface, and the statement says so.

## Why it matters

After a month in production the support agent's first-contact resolution is 62 percent against a target of 80. The team has three theories: the prompt is vague, the tools are confusing, the model is not good enough. Each theory can be believed, and none of them can be acted on until the sessions have been counted. The architect's contribution at this point is not a fourth theory. It is a short audit that turns a month of conversations into counts per failure shape, and a rule that says which count to act on first. The exam asks for that skill in its scenario questions: the log line is the symptom and the options are the theories.

## The idea

### First-contact resolution has a denominator

First-contact resolution is the share of conversations the agent resolved without a person, out of all conversations. The target of 80 percent is a rate of that kind. A rate alone is a bad objective, for the reason page 1 gave: the cheapest way to raise it is to decide more, including what a person should decide. So the audit pairs it with two counts that need a judgement the logs do not hold.

- **Over-escalated:** the agent handed the case to a person, and no person was needed.
- **Under-escalated:** the agent resolved the case, and a person was needed.

A team that lowers escalation to raise the rate and watches only the rate has succeeded when the first count falls and has failed when the second rises. Both are visible only against labels.

### What the logs hold and what a person has to label

Some of what the audit needs is in the logs: which tools were called in which order, whether each call succeeded, the outcome, the refund made and the limit that applied. Two fields are not. Whether a case needed a person is a judgement, and so is which tool a step should have used. Reviewers label them on a sample, and the audit runs on the labelled sample. This is the same reasoning as the accuracy that hides its failures in module 62: a number computed only on what is easy to read is flattering. How to sample and how to make a confidence score honest is module 68 (Human review and calibrated confidence).

### Count sessions, not steps

The audit counts each failure shape once per session. A conversation in which the model picked the wrong tool eleven times is one failed conversation, and the target is a rate of conversations. Counting steps lets one confused session look like eleven problems and moves the order of the fixes for no good reason. The count of steps is still a useful diagnostic for a single session. It is not the unit of the target.

### The order of the fixes

The audit's rule is the one page 1 argued, written as code.

| Shape the counts show | First fix | Why this place in the order |
|---|---|---|
| A protected call before a successful identity check, or a refund over the limit that was made | A gate and a cap in the code that runs the tools | Money moved wrongly and cannot be called back; the rule has a right answer, so code can check it |
| A wrong tool in at least as many sessions as escalation errors | Rewrite the descriptions of the confused tools | The cheapest change that fixes the cause: the model chose by what the descriptions said |
| Escalation errors above zero, and fewer sessions with a wrong tool | Write explicit criteria with examples for when to escalate | The decision boundary was never written down; a classifier or a score is machinery before words were tried |
| Nothing above zero | None: look at the sample size, then at the next target | There is no failure shape to fix |

A tie between the wrong tool and escalation errors goes to the descriptions, since a description is the cheaper fix to try and it also changes what the model does at the boundary.

A refund over the limit that was refused and handed to a person is not a failure. It is the cap working. The audit counts a refund over the limit only when the session resolved with it, which means that the refund was made.

### Fix, then verify in the way that fits the fix

A gate and a cap are code. After the fix the counts for them are zero, and a test that makes the forbidden call and asserts that nothing reached the backend proves it, with no sample needed. A change to descriptions or criteria changes how likely the model is to do something, so the proof is the same audit on a new sample, with the counts compared. A team that "tests" a gate with a hundred live conversations has measured a rate for something that should have none, and a team that tests a prompt change with one conversation has measured nothing.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for the scenario's calibration problem (the agent escalates easy cases and settles complex ones) the key is explicit escalation criteria with few-shot examples, and the options that have the agent report a confidence score and route to a person below a threshold, deploy a classifier trained on past tickets, or escalate on negative sentiment are rejected: self-reported confidence is poorly calibrated, a classifier is more than the first step needs, and sentiment does not track how hard a case is. *What the documentation pages read for this module describe (checked 2026-10-04):* no setting that makes a number the model writes about its own certainty calibrated, and the building-effective-agents article names checkpoints and blockers, not scores, as the moments to involve a person. A model can fill a confidence field in a structured answer, as any field. The course treats the number as a claim that has to be measured against labelled outcomes before anything is routed on it (module 68, Human review and calibrated confidence). On the exam, answer a calibration question with criteria and examples, and answer a question about measuring a score with labelled outcomes.

### The practice: audit a month of sessions

The practice is `exercises/70-scenario-customer-support-agent/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `audit(sessions)`: the session count and the resolved count, the first-contact rate rounded to three decimals and its comparison with 0.8, the over- and under-escalated counts, the sessions that skipped the identity step (read from the order of the steps, with a failed identity check not counting), the sessions with a wrong tool (known right tool only), the over-limit refunds that were made, and the diagnosis by the order above.

The tests build recorded sessions and grade seven cases: a mixed set of six sessions with every field, an empty list, the order of the steps, the made refund against the refused one, the order of the fixes with the tie, the boundary of the target with the rounding, and the sessions-not-steps count. The starter fails all seven, the reference passes them, and each of seven planted wrong solutions per language fails on an assertion of the case it breaks: a failed check that opens the gate, a refund over the limit counted whether or not it was made, the criteria placed before the descriptions, a target that excludes its boundary, steps counted for sessions, an empty list that reports a rate of one, and every escalation counted as an unnecessary one.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Fix the failure that affects the most sessions."** It is tempting because it moves the rate most. The exam rejects it when the smaller count is money that moved wrongly: consequence orders the fixes, and the size of the count orders what is left.
2. **"Count every bad step, so that the worst conversations stand out."** It is tempting because the data is there. The exam rejects it for the target: the target is a rate of conversations, and one confused conversation is one failure.
3. **"Count a refused over-limit refund as a violation."** It is tempting because the amount was over the limit. The exam rejects it: a refund that never ran was stopped by the design, and counting it hides the real violations among the working refusals.

## Quiz

4. After a change that made the agent escalate less, first-contact resolution rose from 0.60 to 0.75 and the team reports success. What does the audit still have to show?
   - **a**: That tokens per conversation fell by the same proportion as the escalation rate
   - **b**: That sessions settled alone although a human was needed did not increase
   - **c**: That the prompt now holds fewer words than before the change was made
   - **d**: That the confidence scores of the model moved up together with the rate

5. One session holds eleven calls to the wrong tool, and the audit reports a count of one for the wrong-tool shape. A manager asks why not eleven. What is the answer?
   - **a**: The audit keeps only the first mistake of each session in the report
   - **b**: Counts are capped at one by default, whatever the session holds
   - **c**: The other ten calls were retries, which the audit leaves out
   - **d**: The target is a share of conversations, and each fails at most once

6. The audit needs two fields for each session: whether a human was needed, and which tool each step should have used. Which source supplies them?
   - **a**: The tool logs, which record every call with its arguments and its result
   - **b**: Reviewers who label a sample of the sessions by hand
   - **c**: The model, which can be asked after each conversation to mark its own errors
   - **d**: The customer, through a short survey sent at the end of each conversation

<details>
<summary>Answer key</summary>

4. **b**. A lower escalation rate is a success only if no case that needed a person was settled alone. *a* is ruled out because tokens are not what the change risked: "Both are visible only against labels." *c* is ruled out because the length of a prompt is not the risk: "A rate alone is a bad objective". *d* is ruled out because a number the model reports is unmeasured: "The course treats the number as a claim that has to be measured against labelled outcomes before anything is routed on it".
5. **d**. The unit of the target is the conversation. *a* is ruled out because the audit does not drop later mistakes for space: "The count of steps is still a useful diagnostic for a single session." *b* is ruled out because nothing in the audit caps a count: "The audit counts each failure shape once per session." *c* is ruled out because every call was a pick of the wrong tool and none is set aside: "A conversation in which the model picked the wrong tool eleven times is one failed conversation".
6. **b**. A judgement about need and about the right tool is made by a person, on a sample. *a* is ruled out because the logs hold the calls and not the judgements: "Two fields are not." *c* is ruled out because the audit exists to check the agent against something other than itself: "a number computed only on what is easy to read is flattering". *d* is ruled out because a survey does not say which tool was right: "Whether a case needed a person is a judgement, and so is which tool a step should have used."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A worker process keeps one state object for every conversation it serves, and a customer is shown a note that belongs to someone else's order. Which change addresses the cause?
   - **a**: Clear the shared object once an hour, limiting how long a note stays
   - **b**: Create a new instance for each ticket, with nothing kept after it
   - **c**: Instruct the model never to repeat what another person has written
   - **d**: Encrypt the stored notes, with only their owner able to read them later

2. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A refund above the limit is refused, and the model asks for the same refund again, and again. What does the dispatcher need so that the run ends in a useful way?
   - **a**: A guard on identical calls that halts the loop and transfers to staff
   - **b**: A line in the prompt telling the model to stop once a refund is refused
   - **c**: A silent retry of the refund until the backend finally accepts it
   - **d**: A larger turn limit that gives the model room to find another approach

3. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. An audit finds no skipped identity step and no refund over the limit, wrong tools in 14 sessions, and escalation errors in 9. Which fix comes first?
   - **a**: Reword the descriptions of the confusable pair, giving formats and examples
   - **b**: Write criteria and worked examples for when the agent should hand over a case
   - **c**: Add a prerequisite gate in the dispatcher in front of the order tools
   - **d**: Move the whole policy into a retrieval index that the agent can search

4. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A team adds a rule that hands a case to a person whenever the message holds three exclamation marks. Which audit count is likely to rise?
   - **a**: Sessions settled by the agent although a person was needed
   - **b**: Sessions with a refund made over the limit
   - **c**: Sessions passed upward that never required a human decision
   - **d**: Sessions that ran an order call before the customer was identified

<details>
<summary>Answer key</summary>

1. **b**. A state that belongs to one case cannot reach another. *a* is ruled out because an hourly clear still lets a note travel between cases: "so a customer's identity, the orders looked up and the refunds made cannot reach another case". *c* is ruled out because a request in a prompt is not a guarantee: "a long-lived shared state is a defect". *d* is ruled out because the fault is that one object serves every conversation, and encryption leaves it shared: "the example builds a new desk for each conversation".
2. **a**. A guard on identical calls turns a loop into a hand-off. *d* is ruled out because more turns only prolong the loop: "the cap only ends a run, and the guard ends it with a hand-off". *b* is ruled out because a sentence of the prompt does not stop a loop: "If the same call, with the same arguments, has been made three times in a row". *c* is ruled out because a refused refund is a permanent refusal: "A permanent error is never retried."
3. **a**. With nothing about money, the wrong tool in more sessions than escalation errors goes to the descriptions. *b* is ruled out because the wrong-tool count is the larger: "A wrong tool in at least as many sessions as escalation errors". *c* is ruled out because no session skipped a step and no refund went over the limit: "A protected call before a successful identity check, or a refund over the limit that was made". *d* is ruled out because the cause is in how the tools are described and not in a missing source: "the model chose by what the descriptions said".
4. **c**. Mood is not need, so handovers that needed no person rise. *a* is ruled out because the rule only adds handovers: "the agent resolved the case, and a person was needed". *b* is ruled out because the rule does not touch refunds: "A refund over the limit that was refused and handed to a person is not a failure." *d* is ruled out because the rule does not change the order of the calls: "A protected call before a successful identity check".

</details>
