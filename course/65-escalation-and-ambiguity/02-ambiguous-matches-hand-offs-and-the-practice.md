# Ambiguous matches, hand-offs, what must not leak and the practice

**Level:** Architect · **Module 65:** Escalation and ambiguity · **Page 2 of 2**
**Exams:** A5.2; S1

**After this page you can** handle a lookup that returns several customers without guessing and without revealing any of them, ask for the identifier that tells the candidates apart, write a hand-off that gives a person what they need in six lines, keep the order of the escalation rules so that an explicit request is never overtaken by anything else, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 5.2 and the scenario S1 description. Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on cases written by hand. The pattern of this page (ask, do not choose; summarise, do not forward) is the guide's, and the exam keys it. Module 48 built a hand-off record from a workflow gate's own state, and this page does not repeat that. It adds the ambiguous lookup and the rule that nothing about a candidate is revealed before identity is settled, and a plain-text hand-off for the case in which no gate exists.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* when tool results return multiple customer matches, the agent should be instructed "to ask for additional identifiers" and not select "based on heuristics"; the guide's examples of heuristics are choices by recency or similarity. Its list of skills for escalation fixes no hand-off format. The practice's six lines (customer, issue, root cause, amount, actions taken, recommendation) are the course's own, in the spirit of module 48's record. *What the product does now:* a lookup tool returns whatever its code returns, including several records, and nothing in the platform chooses among them; the safe behaviour is something the agent's instructions and the tool's design must state. A tool that returns the records' contents to the model puts every candidate's data into the context before the question is asked, so the tool should return only what is needed to ask. On the exam, a question about several matches has the answer: ask for an identifier that tells them apart.

## Why it matters

A caller says their name is Ana Ruiz and that their parcel is late. The lookup finds three Ana Ruizes. An agent that picks the most recent order is right two times in three and tells a stranger about someone else's parcel the third time. An agent that reads the three candidates back ("is it the one in Leeds, the one in Bath, or the one in York?") has already told a stranger where three customers live. An agent that asks for the email on the account loses ten seconds and is right every time. Scenario S1 puts this question in front of you with different details every time.

## The idea

### Several matches are a question

When a lookup returns more than one customer, the agent does not know whose case it is, and every action that follows (a refund, an address change, an answer about an order) is on the account of someone who may be the wrong person. The agent therefore asks. The question names a field that tells the candidates apart, and it asks the caller to supply the value: "Please give me the email address on the account". It is chosen from the fields on which the candidates actually differ, which is what `clarifying_fields` computes: if all three share a postcode, asking for the postcode is a wasted turn, and if only the email differs, the email is the question.

Two other answers look sensible and are wrong. Choosing by a heuristic (the latest order, the best name match, the account with the most activity) is a guess that succeeds often enough to go unnoticed until it fails. And escalating at once is premature: an ambiguous match is a reason to ask, and not by itself one of the three triggers. If the caller cannot supply any identifier, the agent is stuck, which is the third trigger, no progress.

### Ask for the field, never read out the values

The question must not leak. Offering the candidates' values ("the Leeds one or the Bath one?") gives data about three accounts to someone who has not been identified, and the real customer's answer is easy to guess. So the agent names the field and not the values, and the tool that did the lookup returns what the agent needs to choose the field and no more. Until one account is confirmed, the agent does not read from any of them. This is the same scope rule as in module 64: a conversation is bound to one customer, and ambiguity is the one moment at which the binding is not yet made.

### A hand-off is a summary

When the agent does hand a case over, the person on the other side has seconds, not minutes. The hand-off carries a short structured summary and not the transcript: who the customer is, what the issue is, what the root cause appears to be, the amounts involved, what the agent already did (and what it verified), and what it recommends. The practice's `handoff_text` produces six labelled lines, supplies `unknown`, `none` or a default for what is missing so that a gap is visible, and refuses a hand-off that has no customer or no issue, since a person cannot act on that. The transcript stays available behind a link for anyone who wants it. A forty-turn paste is not a hand-off. It is the work of the hand-off passed to someone else.

### The order of the rules

The rules in `decide` have an order, and the order is part of the design: an explicit request for a person first, because nothing may delay it (not even an ambiguous match); then the ambiguous match, because without a customer there is no case to judge; then the policy gap; then the lack of progress; and otherwise the agent resolves, acknowledging frustration if there is any. Sentiment and the model's confidence appear nowhere in the order. Changing the order changes behaviour that customers feel, and so a test for it belongs next to a test for the rules.

### The practice: an escalation policy

The practice is in [`exercises/65-escalation-and-ambiguity`](../../exercises/65-escalation-and-ambiguity/unit-01/practice-1/statement.md). You write the decision (resolve, clarify or escalate, with its reason), the choice of the fields to ask about when several records match, and the text of a hand-off. It is graded in Python, TypeScript, Java and Kotlin; the statement lists nine cases, each saying what you should see when it works.

## Traps

1. **"Three customers match; take the one with the latest order."** It is tempting because it is usually right. The exam rejects it: choosing by heuristic is the failure the guide names. Ask for an identifier that tells the candidates apart.
2. **"Ask the caller which of the three it is, listing their cities."** It is tempting because it is friendly. The exam rejects it: the question reveals data about three accounts to someone who is not yet identified. Name the field and let the caller supply the value.
3. **"Several matches: escalate, a person can sort it out."** It is tempting because it ends the agent's problem. The exam rejects it: an ambiguity is a reason to ask, and not one of the triggers by itself.
4. **"Forward the whole conversation with the escalation, so nothing is lost."** It is tempting because it feels complete. The exam rejects it: the person needs the facts and the recommendation in a few lines. The transcript is there to consult, not to read first.

## Quiz

1. Scenario S1, customer support resolution agent. A lookup by name returns three customers, and the caller has given no other detail. Which action fits best?
   - **a**: Pick the customer with the most recent order and proceed from there
   - **b**: Ask for an identifier that tells them apart, such as the email on the account
   - **c**: Read the three candidates' cities back and let the caller choose
   - **d**: Escalate at once, since an ambiguous match is beyond the agent

2. Scenario S1, customer support resolution agent. A caller demands to speak to a human in the same message in which their name matches three records. What happens next?
   - **a**: Read the candidates' cities back and pass the case on with the caller's choice
   - **b**: Ask for an identifier first, then pass the case on once one account is confirmed
   - **c**: Choose the most active account by a rule, then pass the case on with it
   - **d**: Pass the case to a person at once, before settling which account is meant

<details>
<summary>Answer key</summary>

1. **b**. The agent asks for something that tells the candidates apart and leaves the choice to the caller's answer. *a* is ruled out because choosing by a heuristic "is a guess that succeeds often enough to go unnoticed until it fails". *d* is ruled out because "an ambiguous match is a reason to ask, and not by itself one of the three triggers". *c* is ruled out because offering the candidates' values "gives data about three accounts to someone who has not been identified".
2. **d**. The page puts "an explicit request for a person first, because nothing may delay it". *b* is ruled out because that sentence continues "(not even an ambiguous match)". *c* is ruled out because choosing by a rule "is a guess that succeeds often enough to go unnoticed until it fails". *a* is ruled out because offering the values "gives data about three accounts to someone who has not been identified".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S1, customer support resolution agent. A team routes conversations to staff when a message sounds upset or when the model rates its own confidence below 70. Most such routings turn out to be simple, while cases outside the written guidelines are settled by the agent at confidence above 90. Which change fits best?
   - **a**: Lower the cutoff to 50 and add more mood keywords to the detector
   - **b**: Ask the model to score each case's difficulty from 1 to 10 and escalate the top ones
   - **c**: Escalate on a request for a person, a gap in the policy or stalled work, with examples
   - **d**: Escalate any conversation that runs past six turns, whatever its content

2. Scenario S1, customer support resolution agent. A customer writes in capitals that the third late parcel is unacceptable and asks for nothing else. A reship is within the agent's tools. What should the agent do?
   - **a**: Escalate at once, because an upset customer wants a person
   - **b**: Acknowledge the anger and offer to fix the problem itself
   - **c**: Ask the customer whether they would prefer to speak to a person
   - **d**: Send the replacement quietly without mentioning the complaint

3. Scenario S1, customer support resolution agent. For the second time, the tool that reads a customer's order times out, and the customer is still waiting. What should happen?
   - **a**: Keep retrying quietly in the background until the tool finally answers
   - **b**: Tell the customer to come back later and then close the chat politely
   - **c**: Answer from the customer's earlier orders, which are probably similar to this one
   - **d**: Stop at the attempt limit and hand over a short account of what was tried

<details>
<summary>Answer key</summary>

1. **c**. The triggers are properties of the case, and examples carry the judgement. *a* is ruled out because the guide calls these signals "unreliable proxies for actual case complexity". *b* is ruled out because "A confidence number that the model writes about itself carries its blind spots". *d* is ruled out because "Complexity alone is not a trigger".
2. **b**. An upset customer with a problem inside the agent's reach is helped, with the frustration acknowledged. *a* is ruled out because "Mood tells you how to speak and says little about what the case needs". *c* is ruled out because the agent "does not turn a complaint into one" (a request for a person). *d* is ruled out because "the agent acknowledges the frustration, offers to resolve the issue", and a silent fix does not.
3. **d**. Two attempts without progress hit the third trigger, and the hand-off carries what was tried. *a* is ruled out because "A loop that carries on is the same failure that module 45 bounded in the agent loop". *b* is ruled out because "the agent stops and hands off with what it tried", not with a request to come back. *c* is ruled out because a guess from earlier orders "is a guess that succeeds often enough to go unnoticed until it fails".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
