# Prerequisite chains, limits, hand-offs and the practice

**Level:** Architect · **Module 48:** Multi-step workflows with guarantees · **Page 2 of 2**
**Exams:** A1.4; S1

**After this page you can** design the chain of prerequisites for a sensitive workflow (who, which object, how much, who decides), keep an escape route open when everything else is blocked, write the structured hand-off that lets a person take over a case, mix deterministic gates with the model's judgement without confusing the two, and write the module's practice: a refund desk that cannot skip identity.

Checked on 2026-10-03 against Anthropic's engineering article on building effective agents, the Claude Code documentation page "Automate actions with hooks", and the Claude API page "Handle tool calls". The practice is offline in Python, TypeScript, Java and Kotlin: the backend is a set of functions that the tests script, and the tests make the calls in the order the model would. The desk, its refusal codes and the hand-off record are this course's own design, not an Anthropic interface, and the statement says so.

## Why it matters

Identity is one prerequisite. A real refund flow has a chain: this customer, this order, this amount, within this limit, with a person above it. Every link is a place where the model can be talked into something, or can simply get it wrong, and every link is a rule that code can check. The exam asks which links belong in code and what the agent does when the chain says no. The second question matters as much as the first: an agent that is blocked must still be able to reach a person.

## The idea

### Design the chain from the consequence backwards

Start from the action that cannot be undone, and list what must be true for it to be acceptable.

| Link | The question | Who checks it |
|---|---|---|
| **Who** | Has this person proved who they are, in this session? | The gate, from state set only by a successful check |
| **Which object** | Is this order theirs? | The gate, by comparing the order's owner with the verified person |
| **How much** | Is the amount valid and within what is left on the order? | The gate, with arithmetic |
| **Within what authority** | Is it below the limit at which a person decides? | The gate, against a configured number |
| **Is it a good idea** | Does the situation justify a refund at all? | The model, in conversation, and a person above the limit |

The last row is the boundary. Everything above it is a rule with a right answer, which code checks the same way every time. The last row is judgement, which is what the model is good at and what the prompt is for: reading the customer's message, deciding whether the complaint is covered, writing the reply. A design that puts judgement in code ends up with a brittle rule engine, and one that puts rules in the prompt ends up with a refund policy that holds only as often as the model happens to follow it. The words of the policy go in the prompt, the thresholds and the order of steps go in the gate, and the two are kept in step by tests (the practice's cases are the tests of the gate).

### Each link needs a code the model can act on

The practice's refusals are `identity_required`, `order_not_owned`, `order_not_checked`, `bad_amount`, `exceeds_order`, `needs_human` and `locked`. A refusal has a code and a sentence, as page 1 described. The code is for logs, dashboards and tests. The sentence is for the model, and it should name the next step when there is one: "Look up the order before refunding it." A refusal with no next step, such as `needs_human` and `locked`, tells the model to stop trying and to hand the case over, which is the next section.

### The way out must stay open

Two of the refusals close the desk for the model: a refund over the limit, and a lock after repeated failed identity checks. The practice keeps one tool outside the gate, `escalate`, which is always allowed. This is a design rule for any gated agent: **the path to a person must not depend on a prerequisite that may be the thing that failed**. An agent that cannot verify identity and is also barred from escalating is a dead end that loops until the turn limit (module 45). The lock matters for a second reason: without it, a person who is guessing at an identity code, or a model that is being led to guess, gets unlimited tries. Three failures in a row lock the desk, and a successful check resets the count, so ordinary typing mistakes do not lock anyone out.

### Several concerns in one message

Customers rarely send one concern at a time: "I want a refund on order O9, my address changed, and why was I charged twice?" The exam's third skill for this task is to split such a message into distinct items, look into each of them in parallel with the shared context they need, and then write one answer. Three things make that work.

- **Split first.** The coordinator lists the items before any tool runs, so that each has its own question and its own result, and none is lost behind another.
- **Share the facts, not the conclusions.** The items share what is true of the session: the verified customer and the orders already looked up. In this module that is the desk's state, which lives in the code that runs the tools, so every parallel investigation sees the same verified identity and none can verify itself. Each item is told its own question and not the other items' findings (module 46).
- **One synthesis, with the gate still in force.** A refund that comes out of the second item passes the same gate as any other. If one item needs a person, the hand-off below is for that item, and the others are answered.

### The structured hand-off

When the case goes to a person, the person should not have to read the conversation to learn what happened. The hand-off is a record that the gate produces from its own state, which the model cannot embellish:

```text
{"customer_id": "C1", "identity_verified": true, "reason": "Customer asks for a 250.00 refund",
 "orders_checked": ["O9"], "refunds_done": [{"order_id": "O9", "amount_cents": 10000, "refund_id": "R1"}],
 "blocked": [{"tool": "process_refund", "code": "needs_human"}, {"tool": "process_refund", "code": "needs_human"}],
 "recommended_action": "review_refund"}
```

Each field answers a question the person would otherwise ask. Who is it, and was identity verified? Why was the case escalated, in the agent's words? What was already looked at and already done? What did the system refuse, and under which rule? What does the system suggest? The recommended action is derived by rule (locked means verify the person manually, a refusal for the limit means review the refund, anything else means review the case), so that it is the same for the same facts. The agent's own summary of the reason is still useful, and it is one field among several, not the whole record.

Two things stay out of the record. The gate's state is the source for what was verified, looked up and refunded; the model's account of them is not. And the record holds no more of the customer's data than the person needs: identifiers and amounts, not the whole order contents.

The building-effective-agents article describes the pattern from the agent's side: "Agents can then pause for human feedback at checkpoints or when encountering blockers." A refusal that needs a person is such a blocker, and the hand-off is the checkpoint's payload.

### Deterministic and probabilistic parts in one flow

A workflow with guarantees is a mix, and the architect's job is to draw the line.

- **The model** reads the request, decides which tool to try, writes the answer and judges whether the situation fits the policy.
- **The gate** decides whether the call is allowed to run.
- **The backend** does the thing.
- **The person** decides what the gate cannot: cases above the limit, locked identities, anything the policy does not cover.

Tests follow the line. The gate is tested like any code: the same inputs, the same outputs, every refusal with its code, and a planted bug is caught (that is what the practice does). The model's behaviour is tested with an evaluation set (module 42), where a failure is a rate and not a bug. A team that tests the gate with a model, or the model with unit tests, has mixed up the two.

### The practice

The practice is `exercises/48-multi-step-workflows-with-guarantees/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `RefundDesk`: the gate for the four tools with the refusal codes above, state kept in code (the verified customer, the orders looked up, the refunds made, the refusals, the backend calls), a lock after three failed checks, a refund limit above which nothing runs, backend errors reported without changing state, and the `handoff` record.

The tests play the model by making the calls in the order it would, and grade seven cases: the main path, a refund before identity, another customer's order, the order, amount and remaining-balance checks, the limit and the hand-off, the failed checks and the lock, and unknown tools with backend errors. The starter fails all seven, the reference passes them, and each of eight planted wrong solutions per language fails on an assertion of the case it breaks: a refund that skips the identity check, an order of another customer remembered, a refund over the limit that runs, a failed check that keeps the earlier customer, a desk that never locks, a refund beyond what is left, a refund of zero cents and a hand-off with no refusals.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"If the agent is blocked, make every tool wait for the prerequisite, escalation included."** It is tempting because it is the strictest gate. The exam rejects it: if every tool is behind the prerequisite, a blocked agent can only loop. Keep `escalate` outside the gate.
2. **"Check that the order exists."** It is tempting because a lookup succeeded. The exam rejects it: an order id that exists is not an order that belongs to this customer. Compare the owner with the verified person, and do not reveal whose it is.
3. **"Let the model write the hand-off summary."** It is tempting because the model has the whole conversation. The exam rejects it: a summary written by the model can leave out what went wrong. Build the record from the gate's own state, and add the model's reason as one field.
4. **"Put the policy in the prompt, including its thresholds, and put judgement in code."** It is tempting because both are rules of some kind. The exam rejects it: the order of steps and the limits belong in the gate. Whether a complaint deserves a refund belongs to the model and, above the limit, to a person.

## Quiz

3. A refund gate becomes stricter after three failed identity checks: it now refuses every protected step. A locked-out customer's chat loops until the turn limit. What went wrong?
   - **a**: The lock should have waited for ten failures instead of three
   - **b**: The route to a person depended on the same safeguard that had tripped
   - **c**: The turn limit should have been set a lot lower, so that the loop ended sooner
   - **d**: The model should have been asked to hand the case over on its own

4. A refund over the limit is escalated with the full chat transcript attached, and reviewers spend minutes finding what was verified and what was blocked. Which change helps most?
   - **a**: Keep the transcript, but have the model highlight its key lines
   - **b**: Attach a model-written summary of the conversation beside the full transcript
   - **c**: Add the customer's complete order history to the escalation
   - **d**: Provide a record from the gate's own state, with the agent's reason as a field

<details>
<summary>Answer key</summary>

3. **b**. An escape route must not sit behind a prerequisite that may be what failed. *a* is ruled out because the lock is not the fault: "a successful check resets the count, so ordinary typing mistakes do not lock anyone out". *c* is ruled out because a lower limit only shortens a dead end: "An agent that cannot verify identity and is also barred from escalating is a dead end that loops until the turn limit". *d* is ruled out because the hand-off should come from the gate, not from the model's own account: "A summary written by the model can leave out what went wrong."
4. **d**. The gate holds the facts, and a record answers the reviewer's questions in a fixed place. *b* is ruled out because "A summary written by the model can leave out what went wrong." *c* is ruled out because a record carries only what is needed: "identifiers and amounts, not the whole order contents". *a* is ruled out because the model is again choosing what the reviewer sees: "The gate's state is the source for what was verified, looked up and refunded; the model's account of them is not."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A gate verified customer A earlier in a session. Now someone presents another identity code for customer B, and the check fails. What should the gate's state say next?
   - **a**: Customers A and B are both verified, because the new check adds evidence
   - **b**: Customer A is still verified, since an earlier check succeeded
   - **c**: No one is cleared, so every protected step is refused again
   - **d**: Customer A stays verified until the model confirms which one is meant

2. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A policy allows refunds only for items that arrived damaged. The team encodes "damaged" as a keyword list in the gate, and many valid claims are refused. Which fix fits best?
   - **a**: Keep extending the keyword list until the valid claims finally pass through it
   - **b**: Let the model judge condition, and keep steps, amounts and limits as rules
   - **c**: Move the refund limit into the prompt as well, so that the gate has fewer rules
   - **d**: Drop the ownership check altogether, so that more claims reach the model

3. Scenario S1, a customer support resolution agent. The agent handles returns, billing disputes and account problems with tools that verify identity, look up orders and issue refunds, and it escalates to a person when it cannot resolve a case. A team tests its refund gate by running five hundred live conversations, counts zero skipped verifications and ships. What is still missing?
   - **a**: A judge model that grades every single conversation for the order of its steps in it
   - **b**: A larger live sample, since zero in five hundred proves the rate is zero
   - **c**: The same live run with the verification rule written in capitals
   - **d**: Direct checks that make the forbidden call and assert it never hits the backend

<details>
<summary>Answer key</summary>

1. **c**. A failed check clears the verified customer. *b* is ruled out because "a failed verification clears the state, so that an earlier success cannot be reused." *a* is ruled out because the state holds one verified customer, and a failure does not add one: "set only by a successful verification call". *d* is ruled out because the gate does not consult the model about its own state: "does not ask the model whether it did".
2. **b**. Judgement is the model's work, and rules with a right answer are the gate's. *a* is ruled out because "A design that puts judgement in code ends up with a brittle rule engine". *c* is ruled out because "the thresholds and the order of steps go in the gate". *d* is ruled out because ownership is a rule with a right answer: "Everything above it is a rule with a right answer, which code checks the same way every time."
3. **d**. The gate is code, so it is tested with the forbidden call and an assertion on the backend log. *b* is ruled out because a sample measures a rate, which is the way the model is tested: "where a failure is a rate and not a bug". *c* is ruled out because capitals only change the text the model weighs: "text that the model reads and weighs with everything else in its context". *a* is ruled out because the gate is tested the way all code is: "the same inputs, the same outputs".

</details>
