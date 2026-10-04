# Controls that fail closed, and a person before the irreversible

**Level:** Architect Professional · **Module 90:** Governance, safety and risk · **Page 1 of 2**
**Exams:** P5, P4

**After this page you can** place a control at each layer of a design (the input, the output, the action and the monitor), decide for each control what happens when it fails, put a person in front of every action that cannot be undone, name the failure modes of a model system and match each to a control that really exists, and tell a control from a line in a prompt.

Checked on 2026-10-04 against the Claude documentation pages "Mitigate jailbreaks and prompt injections", "Handle streaming refusals" and "Reduce prompt leak", the Claude Certified Architect, Professional exam guide (version 1.0, domain 5), and by running the example offline in the course container. Nothing here called a model, and the requests, answers, thresholds and counts are invented. This page deepens module 41 (security and safety) and module 68 (human review and calibrated confidence) to a design that must stay safe when a part of it is down. What a design keeps, for how long, and how it is explained to users is the second page.

> **Exam guide and current product.** *What the guide states:* domain 5 asks the candidate to "Implement guardrails and safety controls", to "Identify risks, limitations, and failure modes of LLM systems" and to "Apply human-in-the-loop validation strategies". *What the current product's documentation says (pages read 2026-10-04):* the jailbreak page lists harmlessness screens with a lightweight model, input validation, prompt engineering, and a response to repeat offenders, and for third-party content it advises to put untrusted content only in tool results, to JSON-encode it, to limit access with least privilege and to screen tool outputs; the refusals page says a streamed request can end with a `refusal` stop reason, after which the context must be reset or the request retried on another model. The documentation describes the building blocks. It does not say what a design does when a screen is down or how a person is placed before an action; that is design judgement, taught here as the exam's strategy, and the confidence threshold and the cases are this course's values to tune to your own error costs.

## Why it matters

A claims assistant drafts replies, answers policy questions and can issue refunds. Its input screen is a small service, and one afternoon it times out. The code that calls it reads a timeout as "no problem found" and lets the request through, because a failed check had to be treated as something. For three hours every request skips the screen, and one of them is a document that tells the assistant to refund an account. Nobody decided to turn the control off. The design decided, in the line that says what happens when the check fails. The exam asks where a design puts its controls, what each one does when it breaks, and where a person stands.

## The idea

### Four layers, and what each guards

A control is a part of the system that stops, changes or records something, independent of the model's own judgement. Place them in four layers.

- **Input.** What reaches the model. A harmlessness screen with a small model and a structured verdict; validation for known injection patterns; the rule that untrusted content travels only in tool results, JSON-encoded, with its source named.
- **Output.** What leaves. A grounding check that the answer's quote exists in the source; a format check; the handling of a refusal.
- **Action.** What the system does in the world. The tools a role may call (least privilege, module 86), and the approval a risky call needs.
- **Monitor.** What is watched afterwards: drift, a parity report across groups, alerts (module 87).

The layers answer different attackers and different mistakes. An input control cannot catch a confident wrong answer, and an output control cannot stop a tool call that already happened. A design with all its controls on one layer has a gap on the others, and "the model is told in the system prompt" is a weak instance of one layer: it is a request to the model and not a control that the model cannot argue with.

### What a control does when it fails

Every control has a failure mode of its own, and the design must say it. The two choices are to **hold** (stop the request or the action until the control works) and to **proceed, flagged** (let it go on and record that the check did not run). The rule of the example is by consequence and by layer: a control on the input or the action layer, and any control of the high tier, holds. Only an output or monitor control of the ordinary tier may proceed with a flag.

The reason is the cost of a wrong guess. A screen that is down and lets a request through is the opening an attacker waits for. A drift alert that is down and lets the answers go on flagged costs a delay in noticing, and the answers themselves are still checked by other controls. The example shows both: with the screen down, a refund is held (`hold: screen down`), and a low-consequence reply goes on with a mark (`auto (unscreened)`) so that the gap shows in the record. "Fail open to keep the service up" is a business decision for the second kind of control and never for the first.

### A person before the irreversible

A refund paid, an account closed and a record deleted cannot be undone by a better answer next time. For a **high-consequence action** the design puts a person in front of it, and the person is a control: it exists as a step in the system, it holds when it cannot be reached, and nothing the model says skips it. The example's rule for the route is short: a high-consequence action goes to `human` whatever the confidence; a low-consequence action goes out unreviewed only at a confidence of at least 95, and below it goes to `review`.

Two things make this real. First, **confidence is not evidence.** An answer whose quote is not in the source is held (`hold: unsupported`) even at confidence 99, because a model can be certain and wrong; module 69 shows the quote check. Second, **the threshold is a decision about error cost.** At 95 the example lets an answer through at exactly 95 and reviews one at 94. It is a design value, set so that the cost of reviewing the rest is below the cost of the errors in the part sent out (module 79 gives the arithmetic), and it is checked against a measured sample (module 88), not copied.

### Failure modes, and a control that exists

The guide asks you to identify the failure modes of language-model systems. Four recur in every design review, and each gets a control, an owner and a stated residual.

| Failure mode | What it looks like | A control of the right layer |
|---|---|---|
| Hallucination | a fluent answer the source does not support | a grounding check on the output (hold when unsupported) |
| Prompt injection | instructions inside a document, an email or a tool result | untrusted content only in tool results, JSON-encoded; screen tool output; least privilege |
| Privacy leak | personal data in a prompt, a log or an answer | identifiers swapped for tokens before the call (module 83); no content in the audit log |
| Unfair outcome | different results for different groups | a parity report by segment on the monitor layer |

A register that names a control which is not defined anywhere is a wish. The check is mechanical: every control in the register is a control in the design, with a layer and a failure mode. Two limits belong in the register too, because the documentation is candid about them. A screen with a small model reduces risk and does not remove it, and the page on prompt leaks says that "no method is foolproof" and advises trying monitoring techniques first, since leak-proofing the prompt can degrade the task. So the residual column says what is left: low because an unsupported answer is held, medium because patterns do not find names.

### Refusals are a result to handle

A streamed request can end with `stop_reason: "refusal"`. The design treats it as an outcome with its own branch: the context is reset (the turn that caused it removed or rephrased) before the conversation continues, or the request is retried on a different model; the `stop_details` fields can be null, so the user-facing message comes from the application and not from an assumption that a category is present. Counting refusals per user also feeds the response to repeat offenders that the jailbreak page describes: tell the user the action violates the usage policy, and throttle or end the access.

### The example

The example is a router for the claims assistant, and its first half is this page. It takes an action with a consequence, an answer with a confidence and a quote, the source text and the state of the screen, and it returns what happens: `human`, `auto`, `review`, or a hold with its reason. It runs seven cases: a supported refund goes to a person; a reply goes out automatically at confidences 99 and 95 and to review at 94; a confident but unsupported reply is held; and with the screen down a refund is held and a reply goes on marked unscreened. The second page covers its audit record and the erasure. It ran offline in every language.

<!-- example: m90-control-chain tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: the governance files

The practice is in [`exercises/90-governance-safety-and-risk`](../../exercises/90-governance-safety-and-risk/unit-01/practice-1/statement.md). The draft governance files of a claims assistant are wrong in several places: a screen that fails open, a refund with no person in front of it, an automatic threshold of 80, an audit log that keeps content, and a register with two rows and an invented control. You correct the files, and the tests read them. It is graded in Python, TypeScript, Java and Kotlin; the statement lists seven cases, each saying what you should see.

## Traps

1. **"If the screen is down, let the request through so customers are not blocked."** It is tempting because availability is measured and a blocked request is visible. The exam rejects it because a failed input or action control that proceeds is the gap an attacker waits for; hold, and say so in the response. Only a control on the output or monitor layer of the ordinary tier may proceed with a flag.
2. **"The system prompt says to ask a person before refunds, so a person is in the loop."** It is tempting because the instruction is written and the model usually follows it. The exam rejects it because a line in a prompt is a request, and a control is a step the model cannot skip; build the approval into the action layer as a step that holds.
3. **"A confidence of 99 means the answer can go out without a check."** It is tempting because the score is high and the threshold is 95. The exam rejects it because confidence is not evidence: an answer whose quote is not in the source is held at any confidence, and a high-consequence action goes to a person at any confidence.

## Quiz

1. Scenario: Dana runs the refund flow of a claims assistant. The service that screens incoming requests stops answering, and the code must choose between two behaviours for a request that asks for a refund. Which one fits the design rules on this page?
   - **a**: Hold it and tell the caller that the check is unavailable
   - **b**: Let the request pass and flag it for a person to look at tomorrow
   - **c**: Run the request once more through the model's own safety training
   - **d**: Let the request pass because the later approval step will catch it

2. Scenario: Ravi reviews a design in which a model must "always ask a person before closing an account", a rule written in the system prompt. What is the main weakness of that design?
   - **a**: The sentence is too short for the model to follow it reliably under load
   - **b**: The rule should sit in the user turn so that the model reads it last
   - **c**: Nothing forces the step to happen, because an instruction only asks
   - **d**: The sentence repeats a rule the platform already applies to every account

<details>
<summary>Answer key</summary>

1. **a**. A refund is a high-consequence action and the screen is an input control, so the design holds. *b* is ruled out because "A screen that is down and lets a request through is the opening an attacker waits for". *c* is ruled out because the layers "answer different attackers and different mistakes", and the model's own training is not the missing screen. *d* is ruled out because "an output control cannot stop a tool call that already happened", and a later step does not repair a gap on an earlier layer.
2. **c**. A line in a prompt is a request and a control is a step the model cannot skip. *a* is ruled out because the page does not argue about length: "a line in a prompt is a request, and a control is a step the model cannot skip". *b* is ruled out because the sentence stays "a request to the model and not a control" wherever it sits. *d* is ruled out because "Every control has a failure mode of its own, and the design must say it", and no platform rule is assumed to hold it.

</details>
