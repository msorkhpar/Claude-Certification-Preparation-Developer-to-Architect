# When to escalate, and when not to

**Level:** Architect · **Module 65:** Escalation and ambiguity · **Page 1 of 2**
**Exams:** A5.2; S1

**After this page you can** name the three triggers that justify handing a case to a person and the two signals that do not, honour an explicit request for a person without investigating first, acknowledge frustration and keep working when the problem is within the agent's reach, treat a policy that is silent as a reason to escalate, and write escalation criteria with examples into an agent's instructions.

Checked on 2026-10-04 against the exam guide's task statement 5.2 and the scenario S1 description, and against the Claude prompting best-practices page on examples. Nothing here called a model: the example is a set of six hand-written cases routed by two rules (`examples/65-escalation-rules`), and its numbers are illustrations and not measurements. No Anthropic documentation page defines when a support agent should escalate. The rules on this page are the guide's, and the exam keys them; where this page uses a product fact, it says which.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* appropriate escalation triggers are "customer requests for a human, policy exceptions/gaps (not just complex cases), and inability to make meaningful progress"; the agent escalates immediately when a customer explicitly demands a person, and offers to resolve when the issue is straightforward and the customer is only frustrated, escalating if the customer then repeats the preference; "sentiment-based escalation and self-reported confidence scores" are "unreliable proxies for actual case complexity"; and the remedy for poor escalation calibration is "explicit escalation criteria with few-shot examples" in the system prompt. *What the product offers now (documentation read 2026-10-04):* the prompting guidance calls examples "one of the most reliable ways to steer Claude's output format, tone, and structure", asks for relevant, diverse examples wrapped in `<example>` tags and suggests three to five; nothing in the product decides escalation for you, so the criteria live in your instructions and your code. On the exam, a question about an agent that escalates too often or too rarely has the answer: write criteria and examples; and a question about a mood or a confidence number has the answer: neither is the signal.

## Why it matters

Two failures look opposite and have one cause. A support agent that escalates whenever the customer sounds upset floods the human queue with simple tickets, and the people on that queue learn that escalations are noise. An agent that escalates only when it is unsure resolves, with confidence, the cases it should never have touched: the refund the policy does not allow, the competitor price it was never authorised to match. Both are routed by a proxy, a mood or a feeling, in place of the properties of the case. Scenario S1 asks for the properties.

## The idea

### Three triggers

An agent hands a case to a person when one of these is true:

1. **The customer asks for a person.** An explicit request is honoured at once. The agent does not first try to solve the problem, ask why, or "just finish this step": the customer has told it what they want, and an investigation before the transfer is the agent deciding it knows better.
2. **The policy does not cover the request.** A policy exception or a gap is a trigger, and it is not the same as a complex case. The guide's example is a customer asking for a competitor's price to be matched when the policy only describes adjustments to the company's own prices. The policy is silent, and an agent that applies the nearest rule is inventing policy. Complexity alone is not a trigger: a long case entirely inside the policy is something the agent can finish.
3. **The agent cannot make progress.** A tool that keeps failing, a verification that cannot be completed, two attempts that moved nothing: after a limit the agent stops and hands off with what it tried. A loop that carries on is the same failure that module 45 bounded in the agent loop.

### Two signals that are not triggers

**Sentiment.** A customer who is angry about a late parcel has a problem the agent can often solve; a customer who is perfectly polite may be asking for something that only a person may approve. Mood tells you how to speak and says little about what the case needs. Routing by it sends easy cases to people and keeps hard ones with the agent.

**The model's own confidence.** A confidence number that the model writes about itself carries its blind spots (module 63 made the same point about review). It is not calibrated to whether the case is within policy, and a model is often most sure when it is most wrong. Calibrated confidence is a measured thing (module 68). For routing a support case, use the criteria.

The example routes six cases both ways and shows the difference: a sentiment rule gets five of the six wrong, and the three criteria get all six right. The cases are invented to make the contrast visible, and the result describes those cases only.

### Frustration is not a request

The guide separates two customers. One says "I want to talk to a person": escalate now. The other says "this is the third time, this is unacceptable!" and asks for nothing else: the agent acknowledges the frustration, offers to resolve the issue (a reship, a refund within its limits), and carries on. If the customer then says they want a person, that is a request, and the first rule applies. The agent does not argue the customer out of a request, and does not turn a complaint into one. `decide` in the practice has this shape: `asked_for_person` first, and `acknowledge` set when the mood is not calm.

### Criteria and examples in the instructions

Criteria written in the system prompt, with examples, beat a vague "escalate when appropriate". The criteria are the three triggers in the deployment's own words. The examples are the border cases, each with its decision and its reason, and some of them are cases that look like triggers and are not (an angry customer with a reship request, resolve) next to cases that look easy and are (a polite price-match request, escalate). The example's `escalation_section` builds such a block. The examples carry the judgement that the criteria cannot state.

Keep the decision in code where it can be code. The triggers that are facts (the customer used the words "human agent", the policy lookup returned nothing, the attempt counter reached two) can be checked by the application, which then does not rely on the model's recollection of an instruction.

### The example

<!-- example: m65-escalation-rules tabs: python,typescript,java,kotlin -->
<!-- /example -->

## Traps

1. **"Escalate when the customer sounds angry; they will want a person."** It is tempting because anger feels urgent. The exam rejects it: the agent acknowledges the frustration and offers to fix what is within its reach, and escalates if the customer asks for a person.
2. **"Escalate when the model's confidence is low, and trust it when it is high."** It is tempting because the number looks like a measurement. The exam rejects it: a self-reported score is not a reliable proxy for the case's difficulty, and the criteria decide.
3. **"The customer asked for a person, but the fix is one click: do it first."** It is tempting because it saves the person's time. The exam rejects it: an explicit request is honoured at once, without an investigation first.
4. **"The policy does not mention competitor prices; apply the closest rule."** It is tempting because the closest rule sounds fair. The exam rejects it: a silent policy is a gap, and the agent escalates and does not invent policy.

## Quiz

1. Scenario S1, customer support resolution agent. A customer writes that they want to speak to a person about a change of delivery address. The agent has a tool that can make the change in one step. What should the agent do?
   - **a**: Make the change first, since it takes one step, then offer a transfer
   - **b**: Hand over to a human straight away, before touching the account
   - **c**: Ask why a human is wanted and transfer only if the reason is unclear
   - **d**: Offer to complete the update and transfer only if the customer repeats the wish

2. Scenario S1, customer support resolution agent. A customer asks the agent to match a competitor's lower price. The policy describes adjustments to the company's own prices and says nothing about competitors. The agent can issue a price adjustment. What should it do?
   - **a**: Apply the nearest own-price rule, as that is the fairest reading
   - **b**: Route it to a colleague authorised to rule on the exception
   - **c**: Decline politely, since the policy does not allow the match
   - **d**: Issue the adjustment and flag it for review afterwards

<details>
<summary>Answer key</summary>

1. **b**. An explicit request for a person is honoured at once, with no investigation first. *a* is ruled out because "an investigation before the transfer is the agent deciding it knows better". *c* is ruled out because the agent "does not first try to solve the problem, ask why", and a question is a delay. *d* is ruled out because "An explicit request is honoured at once"; offering a fix first is the answer for frustration, not for a request.
2. **b**. A silent policy is a gap, and a gap goes to a person who can decide. *a* is ruled out because "an agent that applies the nearest rule is inventing policy". *c* is ruled out because "A policy exception or a gap is a trigger", and declining is also a decision the policy did not make. *d* is ruled out because "the agent escalates and does not invent policy", and acting first leaves the invention in place.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
