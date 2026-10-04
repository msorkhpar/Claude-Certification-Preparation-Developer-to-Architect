# Discovery, the record and one decision for two audiences

**Level:** Architect Professional · **Module 91:** Stakeholders and the project lifecycle · **Page 1 of 2**
**Exams:** P6, P1

**After this page you can** run a discovery that ends in numbers and an owner instead of adjectives, write the decision statement a design aims at, compare at least four options and recommend the cheapest one that meets the service levels, tell one decision in the words a sponsor decides with and in the figures an engineer builds with, and put both in one design record with a fixed set of sections.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, domain 6), and by running the example and the practice offline in the course container. The documentation pages of the Claude platform and of Claude Code give no guidance on discovery, stakeholders or design records, and nothing on this page claims that they do. Nothing here called a model, and the figures are invented for a utility's billing-dispute assistant. This page deepens module 12 (from a business need to a testable spec) and module 79 (the decision statement and the break-even accuracy) to a decision that other people must approve and build. Service levels, feedback and the lifecycle are the second page.

> **Exam guide and current product.** *What the guide states:* domain 6 asks the candidate to "Conduct structured discovery and requirement gathering", to "Communicate architectural decisions and trade-offs" and to "Document architectures and provide implementation guidance". *What the current product's documentation says (pages read 2026-10-04):* nothing on these tasks; they concern people and not the platform. The structure below (the discovery questions, the eight sections of the record, the limit of 80 words for the sponsor) is the course's own design, taught as the exam's strategy: the guide names the tasks and does not give a template.

## Why it matters

An engineer designs a billing-dispute assistant for three months and brings the result to the sponsor in a forty-slide deck. The sponsor asks one question, "what do you want from me?", and the deck does not say. The engineer, for his part, finds out in the build that "fast" meant two seconds to the agents and that the credit decisions cost 250 when wrong, which nobody had written down. Both failures come from the same place: the facts were in the room and not in a document. The exam treats communication as a technical skill, and asks what a discovery must produce, how a trade-off is told and what a design record holds.

## The idea

### Discovery ends in numbers and an owner

Discovery is a set of questions with answers that can be tested. "Fast", "accurate" and "safe" are not answers, because nobody can fail them. A useful discovery asks, and writes down, each of these.

- **Who waits, and for how long?** The billing agent on screen waits, so the answer must come within 2 seconds at the 95th percentile; a batch job would allow minutes.
- **How many, how often?** About 3,000 disputes a day, with a peak. This sizes the rate limits and the review staff (module 84).
- **What does an error cost, and what does a check cost?** A wrong credit about 250; a check by a person 5. Two numbers decide where a person belongs (module 79).
- **Who is accountable?** A role that can be telephoned: the billing operations manager. If the model prepares and a person decides, the record says so.
- **What exists today?** The cost and quality of the current way (315,000 a month with people alone). Without a baseline no saving can be shown.
- **What counts as done?** A measure, a threshold, an owner: availability of at least 99.5 percent, measured per month by the platform lead.
- **What is out of bounds?** Data that may not leave a region, actions that need a person (page 1 of module 90).

The output is the **decision statement** of module 79 plus a table of facts. A requirement is complete when it has a number, a way to measure it and a person who owns it; a requirement missing one of the three goes back to the stakeholder as a question.

### Options, and the rule for the recommendation

A design record that shows one option is an announcement. The record compares at least four, including the ones that are obviously wrong, because the reader's first question is "why not the simple thing?". For the dispute assistant: people decide every dispute; the model decides every dispute; the model drafts and a person decides every item; the model drafts and a person checks only what it is unsure about. Each has a monthly cost, whether it meets the service levels, a status and a reason.

- **Exactly one is recommended.** Two recommended options push the choice back to the reader.
- **The recommended option is the cheapest one that meets the service levels.** An option that misses a service level is not a candidate, however cheap, and among those that meet them the cheapest wins; "the newest" and "the most capable" are not criteria.
- **A rejected option has a reason in a sentence.** "Too dear" tells the reader nothing; "the most expensive option and the slowest to answer" can be argued with.

### The trade-off, told

The architect's job is to state what a decision trades. Four things go in, in this order: what improves, what it costs, what is at risk and what is asked. For the recommended design: the cost falls from 315,000 to 80,000 a month; it gives up speed in the credit slice, where a person must approve; it is exposed to confident wrong answers, which an independent check against the source reduces and does not remove; and the decision asked is to approve a pilot. Leaving out the downside is the common failure, and a sponsor who finds it later stops trusting the numbers before it.

### Two audiences, the same facts

The sponsor decides and the engineer builds. They need the same facts in different words, and one document serves both when it is built in layers.

- **For the sponsor:** money, risk and one decision, in plain words and at most 80 words: the monthly cost of the recommendation against the baseline, the weakest answers in terms of "right N times in 100" and what each wrong one costs, who decides them, and the decision asked. No percentages of percentages, no model names.
- **For the engineer:** the numbers that produced it: the break-even accuracy, the table by segment, the service levels with their owners, the pilot assumptions and stop triggers.

The example prints both from the same figures. The sponsor line says the design costs 80,000 a month against 315,000 for people alone, a saving of 235,000, that credit answers are right 63 in 100 and cost 250 each when wrong, so a person decides them, and asks to approve the pilot. The engineer line gives the same facts as `key=value` pairs. The two never disagree because both come from one function over one set of numbers.

### The design record

The record is the document that carries the decision, and a fixed shape makes it readable and checkable. The practice's record has eight sections, in this order: the summary for the sponsor; the decision statement; the options considered; the recommendation and trade-offs; accuracy by segment; service levels; pilot to scale; hand-off and monitoring. The summary is first because the sponsor stops reading early. The rest is for the engineer, and the last two sections are the second page. A record is graded by a rubric because its failures are structural: a missing section, a recommendation that is not the cheapest, a service level with no owner.

### The example

The example is the brief and the figures behind it. It has the whole-percent rounding that rounds halves up, the break-even accuracy (98 for an error cost of 250 and a check cost of 5, 91 for 60 and 5, 58 for 12 and 5), the report by segment with the costliest first and whether a person checks it, the lines for the service levels (the second page) and the two briefs. Each segment uses its own error cost here; the practice uses the case's 250 for its single rule. It ran offline in every language.

<!-- example: m91-tradeoff-brief tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: a design record

The practice is in [`exercises/91-stakeholders-and-the-project-lifecycle`](../../exercises/91-stakeholders-and-the-project-lifecycle/unit-01/practice-1/statement.md). The draft record recommends the newest option and not the cheapest, promises a latency the agents cannot live with, lists one segment and one assumption and leaves the sponsor nothing to decide. You write the record, and the tests grade it against a rubric. It is graded in Python, TypeScript, Java and Kotlin; the statement lists nine cases, each saying what you should see.

## Traps

1. **"Recommend the most capable option; it is the safest choice."** It is tempting because capability sounds like quality and nobody is blamed for choosing it. The exam rejects it because the recommendation is the cheapest option that meets the service levels; capability beyond the requirement is cost without a requirement behind it.
2. **"Show the sponsor the full analysis and let them find the decision."** It is tempting because completeness looks like rigour. The exam rejects it because a sponsor decides and an engineer builds; the summary is plain, short, and ends with the one decision asked, and the analysis follows for the people who need it.
3. **"Describe the benefits and leave out what the design gives up."** It is tempting because a clean case is easier to approve. The exam rejects it because a trade-off has a cost side, and a sponsor who finds the omission later discounts everything else; state what improves, what it costs, what is at risk and what is asked.

## Quiz

1. Scenario: Priya compares four designs for a call-centre assistant. Two of them meet every service level, one at 90,000 a month and one at 140,000. A third costs 40,000 but misses the latency target. Which does the record recommend?
   - **a**: The cheaper of the two that qualify, since price decides among them
   - **b**: The 140,000 design, because the larger budget leaves room to grow beyond the target
   - **c**: The 40,000 design, because it is the cheapest of the four and the target can be tuned later
   - **d**: Both of the designs that meet every level, so that the sponsor can pick the one preferred

2. Scenario: Hugo must brief a sponsor who will decide in one meeting on whether a pilot goes ahead. His draft is six pages of tables, ending with the break-even derivation. What should the first page of the record hold?
   - **a**: The derivation of the break-even, which is the reason for every later number
   - **b**: The table by segment, so the sponsor sees the accuracy figures before anything else
   - **c**: A short summary in plain words: the cost, the risk and the decision requested
   - **d**: The list of the model's settings, so the sponsor can confirm the configuration

<details>
<summary>Answer key</summary>

1. **a**. A design must qualify first, and among those that do, the cheapest is recommended. *c* is ruled out because "An option that misses a service level is not a candidate, however cheap". *b* is ruled out because "the newest" and "the most capable" are not criteria, as "the cheapest one that meets the service levels" is. *d* is ruled out because "Two recommended options push the choice back to the reader".
2. **c**. The sponsor stops reading early, so the summary comes first and ends in the decision. *b* is ruled out because the table by segment belongs with "the numbers that produced it" for the engineer. *a* is ruled out because "The summary is first because the sponsor stops reading early". *d* is ruled out because the sponsor's summary has "no percentages of percentages, no model names".

</details>
