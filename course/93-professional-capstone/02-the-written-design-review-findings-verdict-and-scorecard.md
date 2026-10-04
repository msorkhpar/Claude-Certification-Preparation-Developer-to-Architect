# The written design review: findings, verdict and scorecard

**Level:** Architect Professional · **Module 93:** Professional capstone · **Page 2 of 2**
**Exams:** P1, P2, P3, P4, P5, P6, P7

**After this page you can** review a design against a fixed set of rules and write the result as findings with a severity, a domain and a rule; order them so that two reviewers produce the same list; give a verdict of reject, revise or approve; summarise a review as a count per domain; compute the accuracy a design needs from two costs; and say which of the 22 rules a given flaw trips.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, all seven domains) and by running the example and the practice offline in the course container. The rules, severities and thresholds below are this course's own design, taught as the exam's strategy: the guide does not list them, and the product documentation prescribes no review procedure. Nothing here called a model. The review in the practice reads a design given as facts and numbers, and no real system is assessed. The first page is the design that the review is applied to.

> **Exam guide and current product.** *What the guide states:* the Professional exam asks the candidate to "Evaluate" and "Analyze" designs, to "Identify risks, limitations, and failure modes" and to "Communicate architectural decisions and trade-offs". *What the current product's documentation says (pages read 2026-10-04):* the documents behind each rule are named in the module that teaches it. A written review is how an architect applies them under time pressure, and the thresholds (a team worth at least 15 chats, tool definitions of at most 10,000 tokens without deferral, an evaluation set of at least 20 cases, a roll-out of at least three stages, a team of at most 10 without managed settings) come from earlier modules' design values and from documented figures, so they carry that qualification here too.

## Why it matters

A design review that is a conversation produces a list that depends on who is in the room. A written review turns the design into a fixed list of questions, each with a severity, so the same design gets the same findings on Monday and on Friday, a new reviewer can run it, and the sponsor can read the verdict without reading the analysis. The exam asks the reverse of the same skill: given a scenario, which flaw is present, how serious it is and which domain owns it. Writing the rules out once is the best way to see them.

## The idea

### A finding has three parts

Each finding is a **severity**, a **domain** and a **rule**, written `<severity> <domain> <rule>`, for example `high P3 filter-after-ranking`. The severity says what a reviewer does about it.

- **High.** A flaw that can cause harm that cannot be undone, expose data or lose a protected segment. The design is rejected until it is fixed.
- **Medium.** A flaw that weakens a design without breaking it. The design is revised.
- **Low.** A matter to improve. It is recorded and does not hold the design back.

The **verdict** follows from the findings: **reject** if any finding is high, **revise** if none is high and any is medium, and **approve** otherwise, so a design with only low findings is approved. A fixed ladder keeps a pile of small findings from becoming a rejection and a single serious one from hiding in a long list.

### The order is part of the review

Two reviewers must produce the same list. The order is by severity (high, medium, low), then by domain (P1 to P7), then by the rule id alphabetically. A list ordered only by domain buries the high findings in the middle, and a list ordered by whim cannot be compared with last month's. The **scorecard** is a count of findings per domain, in the order P1 to P7, with a zero for a domain that has none. It answers the question a sponsor asks first: where is the weakness?

### The 22 rules

The rules are the lessons of Level 4 written as checks. Each is a fact about the design or a number with a threshold.

| Domain | Rule | Severity | What it says |
|---|---|---|---|
| P1 | `missing-feedback` | high | the design has no feedback loop |
| P1 | `autonomy-without-need` | medium | an agent or a team is used where the path is known |
| P1 | `team-below-price` | medium | a team's value is below 15 chats' worth |
| P2 | `volatile-prefix` | medium | changing text sits in front of the static prompt, so the cache cannot hold |
| P2 | `model-not-measured` | low | the model was chosen without a measurement |
| P3 | `filter-after-ranking` | high | retrieval ranks first and filters by rights after |
| P3 | `stale-index` | high | the index is not replaced when a document changes |
| P3 | `tool-bloat` | medium | tool definitions above 10,000 tokens with no deferral |
| P3 | `agent-rights-only` | high | the agent's rights are used and not the user's |
| P4 | `no-protected-segment` | medium | no segment is protected in the suite |
| P4 | `small-eval-set` | low | fewer than 20 evaluation cases |
| P4 | `no-way-back` | high | no rollback |
| P4 | `big-bang-rollout` | medium | fewer than three roll-out stages |
| P5 | `identifiers-reach-model` | high | identifiers go to the model |
| P5 | `residency-unmet` | high | the region requirement is not met |
| P5 | `audit-keeps-content` | medium | the audit log stores content |
| P5 | `irreversible-without-person` | high | an irreversible action has no person before it |
| P5 | `retention-outside-window` | medium | the days kept lie outside the floor and the ceiling |
| P6 | `no-accountable-owner` | medium | no owner for the service |
| P6 | `sla-without-numbers` | medium | a latency or an availability with no number |
| P6 | `accuracy-unstated` | low | the needed accuracy is not stated |
| P7 | `unmanaged-team-settings` | medium | a team above 10 without managed settings |

Two rules are conditional on a second fact: an agent is only flagged when the path is known, and an irreversible action is only flagged when no person stands before it. A rule that fires without its second fact is a false alarm, and the practice tests each.

### Thresholds sit at an edge

Each numeric rule has an edge, and the review must treat the edge as the design's friend: a team worth exactly 15 chats passes, tool definitions of exactly 10,000 tokens pass, an evaluation set of exactly 20 cases passes, three stages pass, a retention of exactly the floor or exactly the ceiling passes, and a team of exactly 10 passes without managed settings. One step past any of them is a finding. The practice has a case at every edge and a wrong solution that flips each.

### The accuracy a design needs

The review also computes one number from two costs: the accuracy above which a check no longer pays. It is 100 minus the review cost as a percent of the error cost, with that percent rounded up, and never below 0, with 0 when an error costs nothing. For an error of 250 and a check of 5 it is 98. For 3 and 1 it is 66, and the rounding matters: a third of a hundred is 33.3, which rounds up to 34. A check that costs as much as an error, or more, needs no accuracy, because the check never pays; the result stays 0 and does not go negative.

### Writing the review

A written review is a short document built in the order of the data: the verdict first, then the scorecard, then the findings in their order, each with the evidence from the design and the fix. The sponsor stops at the verdict and the scorecard, and the engineers read the findings. Where the review says `reject`, it names the high findings first, so that the fix list starts with what blocks.

### The example

The example is the claims assistant of the first page, and the review of this page is the process around it: a design that routes by confidence, retrieves by the reader's rights, checks the version, keeps traces without content and gates on a protected segment passes the rules the example demonstrates (`filter-after-ranking`, `stale-index`, `audit-keeps-content`, `irreversible-without-person`, `no-protected-segment`). It ran offline in every language.

<!-- example: m93-claims-assistant tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: a launch review

The practice is in [`exercises/93-professional-capstone`](../../exercises/93-professional-capstone/unit-01/practice-1/statement.md). You write `launch_review`, `verdict`, `scorecard` and `needed_accuracy`; the tests give you designs as facts and numbers. It is graded in Python, TypeScript, Java and Kotlin; the statement lists ten cases, each saying what you should see.

## Traps

1. **"Reject the design whenever it has any finding."** It is tempting because a clean design is the aim and any flaw looks like a reason. The exam rejects it because severity decides: a high finding rejects, a medium one asks for a revision and low findings alone approve; a review that rejects everything cannot be acted on.
2. **"List the findings by domain; it is the tidiest order."** It is tempting because domains are how the course is organised. The exam rejects it because the reader needs the blocking findings first; order by severity, then domain, then rule, so the high findings lead.
3. **"A team of exactly 10 has no managed settings, so flag it."** It is tempting because the policy says a team above ten needs them and ten feels close. The exam rejects it because a threshold is met at its edge: above 10 is a finding and exactly 10 is not; the same holds at every edge in the rule table.

## Quiz

1. Scenario: A review of a pilot finds one medium finding, a missing owner, and two low findings, a small evaluation set and an unstated accuracy. There is no high finding. What does the review conclude?
   - **a**: Approve it, since no finding is serious enough to stop the pilot
   - **b**: Send it back for a change before approval
   - **c**: Reject it, since the findings differ in kind and several are present
   - **d**: Approve it, since the two low findings outnumber the medium one

2. Scenario: A reviewer proposes to write the findings grouped by domain, P1 first, because the course is organised in that order. What is the objection?
   - **a**: The sponsor reads the domains in another order, so the list would confuse
   - **b**: Two reviewers would still produce different lists from the same design
   - **c**: The domains do not match the rules that the review applies
   - **d**: The blocking items would be buried among the milder ones

<details>
<summary>Answer key</summary>

1. **b**. With no high finding and a medium one, the review asks for a revision. *a* is ruled out because "revise if none is high and any is medium", so a medium finding stops an approval. *c* is ruled out because "reject if any finding is high" and none is. *d* is ruled out because "A fixed ladder keeps a pile of small findings from becoming a rejection", and by the same ladder a pile of small findings cannot outweigh a medium one.
2. **d**. A list ordered only by domain hides the blocking items. *a* is ruled out because the order is chosen for the reader of the findings, and the page says "The sponsor stops at the verdict and the scorecard". *b* is ruled out because "Two reviewers must produce the same list", and a fixed order by domain and rule id also gives the same list. *c* is ruled out because the page says of the rules "Each is a fact about the design or a number with a threshold", and the table gives every one a domain from P1 to P7.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: A proposal uses a team of agents for a task whose steps are known in advance and whose value equals ten chats. The designer says the team is more flexible. What does the review record?
   - **a**: One medium item under P1, so it goes back for revision
   - **b**: One high item under P1, so it is rejected
   - **c**: Two medium items under P1, so it goes back for revision
   - **d**: Two low items under P1, so it is approved

2. Scenario: A review is run on two proposals that differ in one number: the first has an evaluation set of 20 cases and the second has 19. What differs in the outcome?
   - **a**: Both are flagged, and the thinner suite more severely
   - **b**: Only the thinner suite is flagged, with a low item under P4
   - **c**: Neither is flagged, since the threshold is only a guide
   - **d**: Only the fuller suite is flagged, with a medium item under P4

3. Scenario: A pipeline sends customer email addresses to the model in the prompt, and the audit log stores every prompt. Which description of the result fits?
   - **a**: Two blocking items, so the pipeline is rejected twice over
   - **b**: Two items that ask for revision and nothing that blocks
   - **c**: One minor item that is only recorded and one that blocks
   - **d**: One blocking item and one that asks for revision, both in the same domain

<details>
<summary>Answer key</summary>

1. **c**. The path is known, so the team is unneeded, and a value of ten is below fifteen, which makes two medium rules fire. *a* is ruled out because the first page's cost rule gives a second item: "A team of agents would cost about 15 times a chat". *b* is ruled out because "reject if any finding is high", and neither rule is high. *d* is ruled out because the rule table lists "an agent or a team is used where the path is known" as medium and not low.
2. **b**. The edge is met at 20 and missed at 19, and the rule is low. *a* is ruled out because the table lists the rule as low for "fewer than 20 evaluation cases", whatever the shortfall. *c* is ruled out because "One step past any of them is a finding". *d* is ruled out because "an evaluation set of exactly 20 cases passes", so the fuller suite has no finding.
3. **d**. Identifiers reaching the model is high and the audit log keeping content is medium, both in P5. *a* is ruled out because the table lists "the audit log stores content" as medium, which asks for revision. *b* is ruled out because the table lists "identifiers go to the model" as high, which blocks. *c* is ruled out because the low bullet reads "It is recorded and does not hold the design back", and neither flaw is low.

</details>
