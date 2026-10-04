# The assistant: persona, memory and the window

**Level:** Architect · **Module 76:** Scenario: conversational AI assistant · **Page 1 of 2**
**Exams:** X

**After this page you can** say what a conversational assistant's system prompt carries and what must live in code, store and read a memory that crosses sessions without leaking it between customers or trusting it when it is old, keep what matters when a conversation outgrows its window, and follow a run in which messages are routed, a window is cut and a memory is recalled.

Checked on 2026-10-04 against the Claude API documentation pages "Prompting best practices" (giving Claude a role) and "Memory tool", and against the Architect exam guide's task statements 5.1 and 5.2, which this module reuses. No published blueprint tests this scenario: it goes beyond the exam's six settings and is taught to make the course complete. The example runs offline in Python, TypeScript, Java and Kotlin: the messages are made up and the routing is a list of phrases, so the example is about where each decision lives. This page is a capstone: it uses modules 29, 41, 64 and 65, which treat the window, the guardrails, the long conversation and the hand-off in full.

## Why it matters

A conversational assistant is judged one conversation at a time, by a person who is talking to it. What that person notices is not the architecture. They notice that the assistant forgot the order number it was given twenty turns ago, that it greeted them with an old address, that it sounded like someone else the next day, and, rarely and most seriously, that it answered a message about their safety with a product suggestion. The architect's work is to decide which of these are matters of wording, which are matters of data and which must never depend on the model's judgement at all.

## The idea

### The scenario in plain words

An assistant for a book shop talks with customers about orders, returns and recommendations, over many turns and across many days. It has a name and a manner, it remembers what a customer told it, it hands over to a person when it should, and the team measures how good its conversations are. Nothing about the setting is exotic. The difficulty is that the same product has three kinds of state with three different homes.

### What lives where

| Kind of decision | Its home | Why |
|---|---|---|
| Name, manner, boundaries, when to hand over, with a few examples | The system prompt | These are requests that shape behaviour, and one stable block is reusable on every turn |
| Whether a message goes to a person, which turns stay in the window, whose memory is read | Code | These must hold on every turn, and a prompt is a request that a model weighs |
| Facts about this customer and their dates | A store, read per customer | Facts outlive a conversation and must be scoped and checked for age |

The documentation says why the first row is short: "Setting a role in the system prompt focuses Claude's behavior and tone for your use case. Even a single sentence makes a difference." A persona needs a name, a manner and its limits, not a page of rules, and the rules that must hold belong in the second row.

### Read a symptom as a failure shape

| What the customer or the logs show | The failure shape | The first fix | Taught in |
|---|---|---|---|
| The assistant asks again for an order number it was given | A fact that fell out of the window | Pin the facts outside the transcript and keep the newest turns that fit | Module 64 |
| A returning customer is greeted with an address that has changed | A remembered fact that is old | Mark a fact by its age and verify the old ones before using them | Module 64 |
| One customer is shown another's details | A memory read without the customer's key | Read and write the store under the authenticated customer | Modules 64 and 65 |
| The assistant sounds different from one week to the next | A persona held only in the conversation | One short system prompt, first and unchanged | This module |
| A distressed message gets a cheerful product answer | A safety signal left to the prompt | Route the message in code before the model sees it | Modules 41 and 65 |
| A customer who asked for a person keeps being asked questions | An escalation decided by the model | Honour the request at once, in code | Module 65 |

### Memory that crosses sessions

The memory tool gives the model a place to keep facts between conversations, and the documentation is plain about who does the work: "The memory tool operates client-side: Claude requests file operations, and your application executes them." That puts three duties on the application. It must keep each customer's files apart, because "Memory lives entirely in your application", and nothing in the protocol knows who the customer is. It must reject any path outside the memory directory, since "A malicious path such as `/memories/../../secrets.env` can reach files outside the `/memories` directory", and the page asks for every path in every command to be validated. And it must expect old facts: the documentation advises to "Periodically delete memory files that haven't been accessed in a long time", and a fact that has not been touched for months is more likely to be wrong than one from last week.

Two further points matter for this scenario. The documentation says that "Claude usually refuses to write sensitive information to memory files", and then adds the architect's reading: "For stronger guarantees, add validation that strips sensitive data before your handler writes the file." A refusal that usually happens is a request, and a handler that strips is a guarantee. And a recalled fact is a claim with a date: the example marks each fact `current` when it is no older than a limit, and `verify` when it is older, so that the assistant asks the customer before it relies on an address from last year.

### The window

A conversation outgrows its window one turn at a time. The pipeline of module 64 applies: the facts that decisions depend on are pinned outside the transcript, and the transcript itself is cut from the old end. The example's window keeps all the pinned facts and then takes turns from the newest backwards while they fit a budget, and it stops at the first turn that does not fit, so that what remains is one unbroken block at the end of the conversation. The budget in the example is counted in words, which is a stand-in for tokens that keeps the output the same in every language.

### Routing in code

The most important decision in the example is the first one. Before the model sees a message, code checks it in a fixed order: a signal of risk goes to a person, a request for a person is honoured, a stalled conversation is handed over after two misses, and otherwise the model answers. The order is the point: a message that asks for a human and also signals risk is a safety hand-off, and a conversation that has stalled is not allowed to keep a customer who asked to leave.

### The example

The example routes four messages, cuts a five-turn conversation to a twelve-word window with two pinned facts, and recalls the memory of two customers on one day. Ada has an address saved two weeks ago and a plan saved ten months ago, so the first is current and the second is marked to be verified. Bob has nothing stored, and the recall for him returns nothing and not Ada's facts.

<!-- example: m76-assistant-turns tabs: python,typescript,java,kotlin -->
```python
```
<!-- /example -->

Every line is the output of the container, and all four languages print the same lines. The routing, the window and the recall are small functions on purpose: each one is a rule that a test can hold, and none depends on what a model would say.

## Traps

These are the answers that sound sensible and fail in a conversational assistant, each with the reason it fails.

1. **"Put every rule in the system prompt, in capitals."** It is tempting because the prompt is the easiest thing to change. It fails because a prompt is a request that the model weighs, and the rules that must hold on every turn, a hand-off above all, belong in code.
2. **"Trust the memory, since the model wrote it."** It is tempting because a remembered fact saves a question. It fails because a fact has an age and an owner: an old fact is verified before it is used, and the store is read for one customer.
3. **"Keep the whole conversation, since the window is large."** It is tempting because nothing is lost. It fails because a long transcript buries the facts that decisions rest on, and the pinned facts and the newest turns carry the conversation better.

## Quiz

1. A customer's message to the example assistant asks to speak to a human and, in the same sentence, says they might hurt themselves. Where does the route lead?
   - **a**: To a request hand-off, since asking for a person is stated first in the sentence
   - **b**: To a safety hand-off, since a signal of risk is checked first
   - **c**: To the model, which weighs the two parts of the message together before it replies
   - **d**: To a stalled hand-off, after the second unanswered question in the conversation

2. The example recalls two facts for a customer on 2026-10-04: an address saved on 2026-09-20 and a plan saved on 2025-12-01, with a limit of thirty days. What does the assistant do with them?
   - **a**: Treats both as current, since nobody has complained about either of them in the meantime
   - **b**: Uses the postal one and has the subscription one confirmed before relying on it
   - **c**: Drops both facts from the conversation, since a memory that is not read each day goes stale
   - **d**: Verifies both facts with the person before saying anything else to them in the conversation

3. A team stores memory files for all customers under one directory and serves any path that begins with `/memories`. A request arrives for `/memories/../../secrets.env`. What is the documentation's guidance?
   - **a**: Allow it, because the string begins with the expected prefix of the memory area
   - **b**: Let the model decide whether the file is sensitive before anything is returned to it
   - **c**: Check each one in every command and refuse whatever resolves outside the root
   - **d**: Rely on the model, which usually refuses to touch files that it was never asked about

<details>
<summary>Answer key</summary>

1. **b**. Risk is checked first. *a* is ruled out because the order puts safety above a request: "a signal of risk goes to a person, a request for a person is honoured, a stalled conversation is handed over after two misses". *c* is ruled out because the decision is made before the model sees the message: "Before the model sees a message, code checks it in a fixed order". *d* is ruled out because the stall comes last: "a message that asks for a human and also signals risk is a safety hand-off".
2. **b**. The address is current and the plan is marked to be verified. *a* is ruled out because an old fact is not trusted: "a fact that has not been touched for months is more likely to be wrong than one from last week". *c* is ruled out because the old fact is checked and not thrown away: "so that the assistant asks the customer before it relies on an address from last year". *d* is ruled out because only the old fact needs confirming: "the example marks each fact `current` when it is no older than a limit, and `verify` when it is older".
3. **c**. Every path is validated. *a* is ruled out because the prefix is not enough: "A malicious path such as `/memories/../../secrets.env` can reach files outside the `/memories` directory". *b* is ruled out because a decision by the model is a request and not a guarantee: "A refusal that usually happens is a request, and a handler that strips is a guarantee." *d* is ruled out for the same reason: "Claude usually refuses to write sensitive information to memory files".

</details>
