# Compliance, fairness, transparency and the register

**Level:** Architect Professional · **Module 90:** Governance, safety and risk · **Page 2 of 2**
**Exams:** P5, P6

**After this page you can** turn a compliance regime into design requirements without claiming a certification, set a retention window with a floor, a ceiling and a legal hold, make erasure a matter of removing the map from tokens to people, measure fairness as a gap between segments, tell users that AI helped, and write the risk register that ties every risk to a control and an owner.

Checked on 2026-10-04 against the Claude documentation pages "Zero data retention" and "Legal and compliance" for Claude Code and the platform page on content moderation, the Claude Certified Architect, Professional exam guide (version 1.0, domain 5), and by running the example and the practice offline in the course container. Nothing here called a model, and the records, tokens and counts are invented. This page deepens module 10 (safety, privacy and policy) and module 83 (where the data goes, and the identifier boundary) to a design that must be defended to an auditor. The controls and the failure modes are the first page.

> **Exam guide and current product.** *What the guide states:* domain 5 asks the candidate to "Ensure compliance with regulations (e.g., GDPR, HIPAA, FedRAMP)" and to "Address ethical AI considerations (bias, fairness, transparency)". *What the current product's documentation says (pages read 2026-10-04):* the documentation describes the data arrangements a deployment can choose (retention periods, zero data retention for eligible products, the organisations that act as data processor on each platform) and points to a trust centre for the compliance material; it does not state that a design is compliant with any regulation, and it gives no fairness metric or threshold. Compliance, fairness and transparency are therefore design judgements, taught here as the exam's strategies. This page makes no claim that any regime is met, and the day counts, thresholds and counts of the example and the practice are this course's design values: take the legal reading from your own counsel and the vendor's trust centre.

## Why it matters

A regulator asks an insurer to show what its claims assistant did on a given day, and a customer asks that her data be deleted. The first team kept every prompt and reply for ten years, so it can show everything and has a second copy of the customers' data to defend. The second team kept nothing, so it can show nothing. A third team kept a record of identifiers, sizes and outcomes for a year, with the map from tokens to people in one place that it can delete: it proves what happened, and the deletion is one operation. The exam asks which design a given requirement calls for, and how a fairness or transparency duty becomes a part of the system.

## The idea

### A regime is a list of requirements, not a feature

The guide names three regimes: GDPR, HIPAA and FedRAMP. The architect's use of them is the same for all. Read the regime as requirements on the design (where data may sit, who may process it, how long it is kept, what a person may demand, what must be proven), map each requirement to a part of the system, and name the arrangements that have to exist outside the code, such as an agreement with the processor. Three limits keep this honest.

- **Compliance belongs to the deployment and its agreements.** A model does not make a system compliant, and a statement that "zero data retention is enabled" is only as good as who it applies to (module 83: the arrangement belongs to the party that runs the platform).
- **The course states design values, not legal ones.** A floor of 90 days, an erasure in 30 days and a ceiling of 365 days are numbers that fit an example. The numbers for a real system come from the requirement and from counsel.
- **A claim about a regime needs a source.** If a design review says "this is GDPR compliant", the question is which requirement, which part of the system meets it and what evidence shows that. The documentation points to a trust centre for compliance material, and that is where a certification claim is checked.

### Retention: a floor, a ceiling and a legal hold

The audit log serves two opposite duties. It must be kept long enough to prove what happened, and no longer than needed, because every extra month is exposure. The example writes this as a window: a **floor** (the shortest period kept, so that an incident found late can still be examined), a **ceiling** (the longest period kept without a reason) and the days actually kept, which lie between them, the ceiling itself allowed. A **legal hold** is the one thing that outranks the ceiling: when a matter is open the records in scope are kept past it, and they are released when the hold ends. What the log holds matters as much as how long: request id, action, consequence, outcome and the size of the text, and never the text itself (module 83). Content that was never stored cannot be breached, subpoenaed by accident or kept past a deletion request.

### Erasure by removing the map

A person asks to be forgotten. The data in the system is the audit record, which holds tokens, and the vault, which maps tokens to people. Deleting every audit entry would destroy the proof; keeping both leaves the person identifiable. The design removes the **map**: the vault entries for that person go, the audit entries stay, and the tokens in them can no longer be linked to anyone. The example shows it: a vault of three mappings holds two for one person; erasing that person removes two, the audit entries stay and one other token remains linkable. The deadline (30 days in the example, and exactly 30 passes) is part of the design and is tested like any threshold, with a case at 30 and a plant at 31.

### Fairness is a gap you measure

Bias, in the guide's wording, is a property of outcomes. The design cannot prevent unfairness by saying so; it measures it. The monitor layer produces a **parity report**: the same metric (the accuracy of module 88, the share of answers sent to a person, the share refused) computed per group, with the gap between the best and the worst group shown and a threshold that triggers a review. Two cautions follow from earlier modules. The groups must be defined where they can be measured (a segment you cannot label cannot be reported), and a headline average hides a failing group exactly as it hides a failing segment. The report does not fix anything on its own, which is why the register's residual column says "monitored and not prevented" and gives the report an owner.

### Transparency: tell people, and show the basis

Two duties travel together. Tell every person who receives output that AI helped produce it, and make that part of the deployment and not a setting someone may switch off; the Level 1 module on policy covers disclosure for decisions about people. And let a reader see the basis of an answer: the quote from the source that the grounding check used (page 1), the document version, and the way to reach a person. A trace that holds ids and sizes can show which document supported an answer without holding the answer.

### The register

The risk register ties it together, and its test is mechanical. Each row names a risk, its failure mode (hallucination, prompt injection, privacy leak, unfair outcome), a control that is defined in the design, a named owner and the residual risk in words. A row with no owner has nobody to call; a row with a control that does not exist is a wish; a register of two rows covers two of four modes. The register also carries the disclosure sentence, so a reviewer finds the transparency duty in the same place.

### The example

The example is the router from the first page, and its second half is this page: the **audit record** and the **erasure**. The record for the supported refund holds the request id, the action, the consequence `high`, the outcome `human`, the number of characters and `content_stored=False`. The erasure runs on a vault of three mappings, two of them for one person. It ran offline in every language.

<!-- example: m90-control-chain tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: the governance files

The practice is in [`exercises/90-governance-safety-and-risk`](../../exercises/90-governance-safety-and-risk/unit-01/practice-1/statement.md), the same one as the first page. The retention and erasure files and the risk register are the part of it that belongs here: a floor of at least 90 days, the days kept within the ceiling, no content stored, a legal hold that outranks the ceiling, erasure in at most 30 days, four register rows each with a defined control and a named owner, and the disclosure sentence.

## Traps

1. **"Keep every prompt and reply so that nothing can be disputed."** It is tempting because more records feel safer. The exam rejects it because a complete copy of the content is a second copy of the customers' data, to be protected, retained and deleted; keep proof of what happened, and keep content only where the requirement says so.
2. **"To honour a deletion request, delete the audit log."** It is tempting because the log is where the person's data is. The exam rejects it because the audit entries hold tokens and the proof is worth keeping; remove the vault map that links tokens to people, and the entries stay without a person attached.
3. **"The model is unbiased, so no fairness check is needed."** It is tempting because the model was tested in general and the average looks good. The exam rejects it because fairness is a property of outcomes by group, and an average hides a group that fails; measure the gap per segment and give the report an owner.

## Quiz

1. Scenario: Mira is asked to honour a deletion request from a customer whose requests appear in the audit entries of a claims assistant. Those entries hold tokens, and one table maps each token to a person. What does the design do?
   - **a**: Delete the entries of that customer from the log
   - **b**: Remove the vault rows that tie them to anything in the log
   - **c**: Replace the tokens in that customer's entries with new tokens
   - **d**: Encrypt that customer's entries with a key that is then kept

2. Scenario: Tomas must set how long a support assistant keeps its audit records. His design has a shortest period, a longest period and a number of days between them. A court order then opens a case that covers a set of those records. What happens to those records?
   - **a**: They are deleted at once because the court may inspect any copy
   - **b**: They follow the same schedule and the case uses what is left
   - **c**: They are moved to a table with no limit on their retention
   - **d**: Hold them past the ceiling until the matter is settled

<details>
<summary>Answer key</summary>

1. **b**. Removing the vault rows leaves the entries and unlinks them from the person. *a* is ruled out because "Deleting every audit entry would destroy the proof". *c* is ruled out because "keeping both leaves the person identifiable", and new tokens in the entries leave the vault map in place. *d* is ruled out because the design removes "the map: the vault entries for that person go", and encryption keeps a key that can link them again.
2. **d**. A legal hold is the one thing that outranks the ceiling, and the records are released when it ends. *a* is ruled out because the hold keeps records, as "the records in scope are kept past it". *c* is ruled out because "they are released when the hold ends", and an unlimited table keeps them after the case. *b* is ruled out because "A legal hold is the one thing that outranks the ceiling", so the ordinary schedule does not apply.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Ines's claims assistant keeps its record of each request for a year, and a reviewer notices that the screen that checks incoming requests is allowed to let them through when it is down. Which change does the first page support?
   - **a**: Keep the pass-through and add a monitor that alerts on the gap
   - **b**: Keep the pass-through and shorten the retention of the records
   - **c**: Stop the call whenever that service cannot answer, and log why
   - **d**: Move the screening into the system prompt so that it cannot be down

2. Scenario: A review finds that a model system's average accuracy is 94 percent, and that one language group, a small part of the traffic, scores 70 percent. The team says the system passes. What does the second page say about that claim?
   - **a**: A small group cannot be measured, so the average is the only usable figure
   - **b**: Fairness is a gap between segments, and a headline figure masks a failing one
   - **c**: The group's score should be dropped when it is below the volume needed
   - **d**: The system passes because the model was tested on a broad set before launch

3. Scenario: An auditor asks a team to prove that a refund over a set amount was approved by a person. The team has an audit entry for the refund with a request id, an outcome of human and a character count. What do they have?
   - **a**: Nothing, because a record without the content is not admissible
   - **b**: A record that is too thin, since only the text proves what happened
   - **c**: A record that holds the refund amount and the approver's name
   - **d**: Proof of the step, with no copy of the customer's text kept

<details>
<summary>Answer key</summary>

1. **c**. An input control holds when it fails, and the reason goes in the record. *b* is ruled out because retention does not close the gap: "A screen that is down and lets a request through is the opening an attacker waits for". *a* is ruled out because the first page puts the monitor in the layers that watch "afterwards", so "an output control cannot stop a tool call that already happened". *d* is ruled out because "a line in a prompt is a request, and a control is a step the model cannot skip".
2. **b**. The page measures fairness as a gap per group and warns that an average hides one. *a* is ruled out because "the groups must be defined where they can be measured" and a group that can be labelled can be reported. *c* is ruled out because "a headline average hides a failing group exactly as it hides a failing segment", so dropping the score hides it. *d* is ruled out because "fairness is a property of outcomes by group", and a broad launch test is not a gap measure.
3. **d**. The record shows the step happened without a copy of the data. *b* is ruled out because "Content that was never stored cannot be breached, subpoenaed by accident or kept past a deletion request". *c* is ruled out because the entry holds "request id, action, consequence, outcome and the size of the text", which names no amount or approver. *a* is ruled out because the design keeps "proof of what happened", and the page does not tie proof to stored content.

</details>
