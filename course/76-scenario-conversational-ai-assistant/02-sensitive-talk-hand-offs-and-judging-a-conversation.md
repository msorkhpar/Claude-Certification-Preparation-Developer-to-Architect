# Sensitive talk, hand-offs and judging a conversation

**Level:** Architect · **Module 76:** Scenario: conversational AI assistant · **Page 2 of 2**
**Exams:** X

**After this page you can** explain why a safety signal is routed in code and what the assistant does while a person takes over, say what a hand-off record must hold, judge a batch of conversations with limits that are met at the limit, and write the module's practice: the review of a batch as a function.

Checked on 2026-10-04 against the Architect exam guide's task statements 1.5 and 5.2 (the deterministic guarantee and the escalation triggers), which this module reuses for a setting the exam does not test, and against the Claude API documentation page "Memory tool". The practice is offline in Python, TypeScript, Java and Kotlin: the tests build a labelled batch and read your report. The shapes of the batch, of the policy and of the report and the limits are this course's own design for the capstone, not an Anthropic interface, and the statement says so.

## Why it matters

Page 1 gave the assistant a persona, a memory and a window. This page is about the moments where an assistant must not be trusted to be clever: a customer who may be in danger, a customer who wants a person, a conversation that has gone nowhere. And it is about how a team knows, before a release, that the assistant behaves well in a batch of real conversations and not just in the demo.

## The idea

### Sensitive talk is routed, not prompted

A message that signals risk to someone's safety is not a prompt-engineering problem. The assistant's manner can be warm and its answer can still miss the signal, because a model's judgement is a probability and this is the one decision where a miss costs far more than a needless hand-off. So the first decision is made in code, before the model sees the message, and a person receives the conversation.

The example's list of phrases is short on purpose, and it is a floor and not a design to copy. A real system uses a vetted list or a classifier and tunes it to catch more at the price of some false alarms, and the model's own judgement can be a second layer on top. What the design must not do is make the model the only layer.

While the hand-off happens, the assistant acknowledges the message and says that a person will take over; it does not diagnose, advise or keep chatting about the order. Which person or which resource receives the conversation is the operator's decision, made in advance, and the assistant's job is to get the customer there.

### The hand-off record

A person who receives a conversation needs five things: who the customer is, what they asked, what the assistant already did, why the hand-off happened, and what cannot wait. A transcript is not that record. A person who has to read forty turns to find the five things has been handed a search task, and the customer waits meanwhile. The record is built from the code's own state, as module 48 built it for a refund, and the reason is one of the routes: `safety`, `requested` or `stalled`.

A customer who asks for a person gets one at once, without an attempt to investigate first. A conversation that has stalled is handed over after two misses and not after ten, because a customer who has said it twice has told the assistant what it needs to know.

### Judging a conversation

A team reads a labelled batch of conversations before a release, and the review has one number that is never averaged and several that are.

- **Safety missed.** A conversation with a signal of risk counts as handled only when it ended in a safety hand-off. A customer who asked for a person in the same conversation and got a request hand-off was not handled as the policy says. A safety miss is not averaged: one missed signal holds the release however good the rest is.
- **Resolved.** A conversation counts as resolved by the assistant only when the assistant settled it alone. A conversation that a person settled is a good outcome and is not the assistant's.
- **Overlong.** A conversation with more turns than the limit and no hand-off. A conversation that was handed over is not overlong, because somebody took over.
- **Repeated questions.** The share of conversations in which the assistant asked the same question twice. A limit is met at the limit, so exactly ten percent repeated questions pass and twenty do not.
- **Over- and under-escalation.** A hand-off nobody needed and that had no safety signal behind it is over-escalation, and a conversation that needed a person and never got one is under-escalation. Both are counted, because a fix for one makes the other worse.
- **Segments.** The figure for resolution is broken down by kind of conversation. A segment is weak only below the floor, and only when it has at least a minimum number of conversations, because two unresolved conversations prove nothing and three begin to.

The verdict names one reason, in the order safety, then a weak segment, then repeated questions, so that a release that is held says first what matters most.

### The practice: the review of a batch

The practice is `exercises/76-scenario-conversational-ai-assistant/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `review(conversations, policy)`: the counts, the resolved share, the missed safety signals, the overlong conversations, the repeated-question share and whether it is within the limit, the over- and under-escalations, one segment per kind with its weakness, and the verdict with its reason.

The tests build a labelled batch and grade ten cases: a mixed batch, an empty batch, a safety signal that reached a person only as a request, the turn limit met and exceeded, the repeat limit met and exceeded, a segment exactly at the floor and one below it, a segment one conversation short of the minimum, the escalation counts, the rule that only the assistant's own settlements are resolved, and the rounding. The starter fails all ten, the reference passes them, and each of ten planted wrong solutions per language fails on an assertion of the case it breaks: a safety signal counted as handled by any hand-off, an overlong limit that counts when met, a repeat limit that must be stayed under, a floor that counts when met, a minimum that is ignored, a minimum that must be exceeded, a safety hand-off counted as over-escalation, a settlement by a person counted as the assistant's, a percentage rounded down, and a verdict that ships although a signal was missed.

## Traps

These are the answers that sound sensible and fail in a conversational assistant, each with the reason it fails.

1. **"Let the model judge whether a message signals risk and hand over when it is concerned."** It is tempting because a model reads tone well. It fails because it makes a probabilistic judgement the only barrier on the one decision that must not be missed: the route is code, and the model's judgement is a second layer.
2. **"Ship at a 90 percent average."** It is tempting because the figure is high. It fails because an average hides a weak segment and a missed signal, and the review reports those first.
3. **"Offer to help first, and hand over only if the customer asks twice."** It is tempting because it keeps the conversation in the assistant's hands. It fails because a customer who asks for a person gets one at once.

## Quiz

1. In a reviewed batch of 200 conversations, 199 are excellent, and one customer's message about their own safety got a product tip. The average quality score is 98. What does the review decide?
   - **a**: Ship it, since the average clears any reasonable bar by a wide margin
   - **b**: Hold it until such cases are routed to a person every time
   - **c**: Ship it with a note, since one case in two hundred is within normal noise
   - **d**: Hold it only if the next batch shows a second case of the same kind

2. A conversation is handed over after a signal of risk. What should the assistant do while the person takes over?
   - **a**: Keep the conversation going as usual, to avoid alarming the customer before the person arrives
   - **b**: Acknowledge the message, say that someone is coming, and stop advising
   - **c**: Offer a diagnosis of what the customer is going through and a plan to address it
   - **d**: Finish the order question first, then mention that a person may join afterwards

3. A policy allows at most 10 percent repeated questions. A batch of 20 conversations has 2 of them. Does the batch pass that check?
   - **a**: No, because any repeated question counts against the assistant in a review
   - **b**: No, because the limit has to be stayed under and cannot simply be touched
   - **c**: Yes, since the share equals the limit and a limit is met when reached
   - **d**: Yes, but only when the repeats all came from a single customer in the batch

<details>
<summary>Answer key</summary>

1. **b**. A missed signal holds the release. *a* is ruled out because the average is not the test: "A safety miss is not averaged: one missed signal holds the release however good the rest is." *c* is ruled out because the safety count has no noise allowance: "the review has one number that is never averaged and several that are". *d* is ruled out because one case is enough: "A safety miss is not averaged: one missed signal holds the release however good the rest is."
2. **b**. The assistant acknowledges and hands over. *a* is ruled out because the assistant does not keep chatting: "it does not diagnose, advise or keep chatting about the order". *c* is ruled out because it does not diagnose: "it does not diagnose, advise or keep chatting about the order". *d* is ruled out because the order question does not come first: "it does not diagnose, advise or keep chatting about the order".
3. **c**. Two of twenty is exactly ten percent. *a* is ruled out because the policy allows some: "A limit is met at the limit, so exactly ten percent repeated questions pass and twenty do not." *b* is ruled out for the same reason: "A limit is met at the limit, so exactly ten percent repeated questions pass and twenty do not." *d* is ruled out because the source of the repeats plays no part: "The share of conversations in which the assistant asked the same question twice."

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: a conversational assistant for a book shop that remembers customers and hands over to people. The prompt tells the assistant to hand over on any sign of distress, and the team finds three missed signals in a week. What is the first fix?
   - **a**: Add three more examples of distress to the system prompt, in capitals
   - **b**: Route such messages in code before the model sees them
   - **c**: Raise the assistant's confidence threshold before it answers anything
   - **d**: Ask the model to rate every customer message for distress and hand over above a score

2. Scenario: a conversational assistant for a book shop that remembers customers and hands over to people. A returning shopper is greeted with an address saved 200 days ago, and the policy limit is thirty days. What should the assistant do first?
   - **a**: Use it as it stands, since a saved address is the best evidence there is
   - **b**: Use it and ask the account holder about it at the end of the call
   - **c**: Delete every stored fact older than the limit without telling anyone
   - **d**: Have the account holder confirm it before relying on it

3. Scenario: a conversational assistant for a book shop that remembers customers and hands over to people. A segment of three conversations has none resolved by the assistant, the floor is 80 percent and the minimum is three. What does the review say about it?
   - **a**: It is weak, since the count is enough to judge and the result is short of the bar
   - **b**: The segment is not weak, as three conversations are too few to judge
   - **c**: The segment is weak only when a fourth conversation confirms the pattern
   - **d**: The segment is not weak, as a person settled some of the conversations

4. Scenario: a conversational assistant for a book shop that remembers customers and hands over to people. A conversation with forty turns ends in a stalled hand-off. Is it counted as overlong?
   - **a**: Yes, since forty turns is above any sensible limit
   - **b**: Yes, because a stall proves that the assistant should have stopped earlier
   - **c**: No, since a person took over, and the measure covers only cases that nobody took over
   - **d**: No, because stalled hand-offs are excluded from every figure in the review

<details>
<summary>Answer key</summary>

1. **b**. The route is code. *a* is ruled out because a prompt is a request: "Put every rule in the system prompt, in capitals." *c* is ruled out because a confidence threshold is the model's own judgement: "It fails because it makes a probabilistic judgement the only barrier on the one decision that must not be missed". *d* is ruled out for the same reason: "What the design must not do is make the model the only layer."
2. **d**. An old fact is checked with the account holder before it is relied on: "an old fact is verified before it is used". *a* is ruled out because an old fact is not trusted: "a fact that has not been touched for months is more likely to be wrong than one from last week". *b* is ruled out because an old fact is verified before use: "so that the assistant asks the customer before it relies on an address from last year". *c* is ruled out because an old fact is verified and not discarded: "A recalled fact is a claim with a date".
3. **a**. A segment at the minimum and below the floor is weak. *b* is ruled out because the minimum is met: "two unresolved conversations prove nothing and three begin to". *c* is ruled out because the minimum is a floor and not a margin: "only when it has at least a minimum number of conversations". *d* is ruled out because a settlement by a person is not the assistant's: "A conversation that a person settled is a good outcome and is not the assistant's."
4. **c**. A hand-over takes the conversation out of the measure. *a* is ruled out because the length alone is not the test: "A conversation with more turns than the limit and no hand-off." *b* is ruled out because the stall is a hand-off: "A conversation that was handed over is not overlong, because somebody took over." *d* is ruled out because the hand-off is counted elsewhere: "A hand-off nobody needed and that had no safety signal behind it is over-escalation".

</details>

Adapted from the sample scenario of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the scenario format is Anthropic's, and this module's setting and questions are written for this course.
