# A claims assistant end to end, across the seven domains

**Level:** Architect Professional · **Module 93:** Professional capstone · **Page 1 of 2**
**Exams:** P1, P2, P3, P4, P5, P6, P7

**After this page you can** take one design and walk it through the seven domains of the Professional exam, name the decision each domain forces and the module that taught it, follow a request through the chain of an assistant (identifiers swapped for tokens, retrieval by the reader's rights, a stale-evidence check, a source check, a route, a trace without content), and use a release gate that protects the costly segment.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, all seven domains), the Claude documentation pages named in modules 79 to 92, and by running the example offline in the course container. Nothing here called a model, and the documents, requests, answers and cases are invented. This page draws on every module of Level 4. The review of a design by rule, and the practice, are the second page.

> **Exam guide and current product.** *What the guide states:* the Professional exam draws its items from seven domains: solution design and architecture (17 percent), models, prompting and context (13), integration (19), evaluation, testing and optimisation (16), governance, safety and risk (14), stakeholder communication and lifecycle (14) and developer productivity and operational enablement (7). *What the current product's documentation says (pages read 2026-10-04):* the pages behind each module are named on that module's pages; nothing on the platform ties them into one design, and no page prescribes the chain of the example, whose thresholds (a confidence of 95) are the course's design values. A capstone question names a scenario and asks which domain's rule decides it, so the skill is to find the rule and apply it, not to remember a page.

## Why it matters

The exam's scenario items do not announce their domain. A stale answer after a document refresh is an integration question, an evaluation question and a governance question at once, and the choice among the answers is made by noticing which fact in the stem carries the weight. This page shows one design in which every domain contributes a decision, so that the pattern of the questions becomes familiar: read the scenario, find the failing layer, apply the rule of the domain that owns it, and check that the other domains have not been broken by the fix.

## The idea

### The scenario

Larch Mutual, an insurer, wants an assistant that answers policy questions for its claims staff and drafts replies to customers. About 3,000 questions a day. A wrong refund costs about 250 and a person's check costs 5. The documents are policy wordings and partner contracts, and they are revised often. The claims staff may read policy documents; only the partnership team may read contracts. A sponsor will decide on a pilot.

### One design, seven decisions

Each domain forces a decision, and each decision was taught in a module.

- **P1, solution design.** The decision statement says what the assistant helps decide and what it does not. Four stages (input, processing, output, feedback) are all present, and the feedback stage is the one most often missing. The pattern is the lowest rung that meets the need: an augmented call with a small fixed workflow, not an agent, because the path is known (module 79). A team of agents would cost about 15 times a chat, and the value does not pay it.
- **P2, models, prompts and context.** The static instructions and the policy wording come first, the question last, so that the prefix can be cached; the model is chosen by measured quality, latency and price and not by name (module 82).
- **P3, integration.** Retrieval filters by the reader's rights before it ranks, so a document the reader may not see never reaches the model. The index is replaced when a document changes, and an answer drawn from a superseded version is refused. The assistant holds the reader's rights and not its own (modules 85 and 86).
- **P4, evaluation.** A suite by segment with the refund segment protected, a gate that refuses a change that loses an answer there, a staged roll-out and a way back (modules 88 and 89).
- **P5, governance.** Identifiers are swapped for tokens before the call, the audit trace holds ids and sizes and no content, a person stands before a refund, the retention has a floor and a ceiling and users are told that AI helped (modules 83 and 90).
- **P6, stakeholders.** A summary of at most 80 words for the sponsor with one decision; service levels with owners; a pilot whose assumptions each have a stop trigger (module 91).
- **P7, enablement.** The team that builds it works under managed settings with a model list and an effort cap, and its adoption is measured against a baseline (module 92).

### The chain, request by request

The example is the middle of that design on one page: the part a request travels through. Its order is itself a design decision, and each step answers one failure.

1. **Tokenise.** An email address in the question becomes `<EMAIL_1>` before anything is sent, and the same address always gets the same token, so a question that mentions it twice still reads as one person. The map from token to address stays with the caller.
2. **Retrieve by the reader's rights.** Documents the reader may not read are removed first, then the rest are ranked by the words they share with the question; a tie goes to the smaller id, and a question that shares nothing with any readable document gets no evidence.
3. **Check the version.** Each document has a current version. A chunk from an older one means the index was not replaced: the request is held with the reason `stale evidence (policy-2-old v2, current v3)`. This is the failure of the exam's retrieval sample, and the check is cheap.
4. **Check the source.** The answer's quote must be in the chunk; an answer whose quote is missing is held as `unsupported`.
5. **Route.** A high-consequence action goes to a person; otherwise a confidence of at least 95 goes out and a lower one goes to review (page 1 of module 90).
6. **Trace and keep.** The trace holds the request id, the chunk and its version, the outcome and the number of characters, and no text. Tail-based keeping stores every trace that was held or reached a person and leaves the rest to sampling (module 87).

The example runs five requests. The first is answered. The second meets a stale index and is held. The third asks for a refund and goes to a person. The fourth asks about a partner commission, a document the reader may not read, and gets no evidence. The fifth carries an email address, which the output shows as a token, and is answered from the policy about reporting a claim. Three traces are kept: the held ones and the one that went to a person.

### The gate on the last line

The example ends with the release gate of module 89 in small. A change is judged on twelve cases in three segments. It gains answers in the status and complaint segments and loses one in the refund segment, which is protected, so the gate says no-go and names the segment, although the gains outnumber the loss. After the refund case is fixed, the same change is a go with no loss and three gains. The point of the capstone is that this gate and the first request in the chain belong to the same design: a change to the retrieval step that pleases the status segment is refused if it costs a refund answer.

### The example

<!-- example: m93-claims-assistant tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: a launch review

The practice is in [`exercises/93-professional-capstone`](../../exercises/93-professional-capstone/unit-01/practice-1/statement.md) and is the second page's subject: you write the review that turns a design into findings, a verdict and a scorecard by domain.

## Traps

1. **"Fix the wrong answer in the prompt; the model got it wrong."** It is tempting because the answer is wrong and the prompt is the nearest lever. The exam rejects it because a confident wrong answer after a document refresh points to the retrieval step, where a stale chunk is the usual cause; check the version of the evidence before the model.
2. **"Rank first and filter by rights afterwards; the ranking is the hard part."** It is tempting because ranking over all documents finds the best match. The exam rejects it because a document that the reader may not see can reach the model before the filter removes it; filter by rights first, then rank.
3. **"The change gains more answers than it loses, so release it."** It is tempting because the net count is positive. The exam rejects it because a loss in a protected segment is a veto whatever the gains elsewhere; the segment that costs most when wrong is protected by the gate.

## Quiz

1. Scenario: Larch Mutual's assistant answers a coverage question with a limit of 3,000, and the current policy says 5,000. Latency and the model version have not changed since the documents were refreshed. Where does the investigation start?
   - **a**: The retrieval step, to see whether a superseded chunk was returned
   - **b**: The model, to see whether its weights were updated by the vendor
   - **c**: The sampling temperature, to see whether it was lowered by a release
   - **d**: The context window, to see whether the prompt was cut short

2. Scenario: A change to the retrieval step gains three answers in a minor segment and loses one in the segment where an error costs 250. The totals favour the change. What does the gate return?
   - **a**: Release it, because the gains outnumber the losses
   - **b**: Release it, with a note asking the owner to review the lost case
   - **c**: Refuse it, naming the area where the damage fell
   - **d**: Refuse it, because every loss blocks every change

<details>
<summary>Answer key</summary>

1. **a**. A confident wrong answer after a refresh points to stale evidence. *b* is ruled out because a confident wrong answer after a refresh "points to the retrieval step, where a stale chunk is the usual cause". *c* is ruled out because "Fix the wrong answer in the prompt; the model got it wrong" is the tempting lever the exam rejects. *d* is ruled out because the stem leaves latency and version unchanged, and the page says "check the version of the evidence before the model".
2. **c**. A loss in a protected segment is a veto whatever the gains. *a* is ruled out because "a loss in a protected segment is a veto whatever the gains elsewhere". *b* is ruled out because the gate returns a decision and not a note, and the page says it "says no-go and names the segment". *d* is ruled out because the gate refuses a change when it "loses one in the refund segment, which is protected", and a loss elsewhere is weighed against the gains.

</details>
