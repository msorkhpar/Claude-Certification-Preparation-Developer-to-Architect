# Service levels, feedback and the lifecycle from pilot to scale

**Level:** Architect Professional · **Module 91:** Stakeholders and the project lifecycle · **Page 2 of 2**
**Exams:** P6, P4

**After this page you can** write a service level as a target, a measure and an owner, report one honestly at its exact edge, align expectations by segment instead of promising an overall figure, close a feedback loop from reviewers and sponsors into evaluation cases, state the assumptions of a pilot with a test and a stop trigger for each, and hand a design over with an owner, a runbook, monitors and a rollback.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, domain 6), and by running the example and the practice offline in the course container. The documentation pages of the Claude platform and of Claude Code give no guidance on service-level agreements, stakeholder feedback or the stages of a project, and nothing here claims that they do. Nothing here called a model, and the targets, measurements and counts are invented. This page deepens module 88 (evaluation and the report by segment) and module 89 (the staged roll-out and the rollback) to the part of the work that outlasts the build. The discovery, the options and the record are the first page.

> **Exam guide and current product.** *What the guide states:* domain 6 asks the candidate to "Manage stakeholder feedback loops and expectation alignment (including SLAs)" and to "Support lifecycle phases (discovery, design, handoff, monitoring, iteration)". *What the current product's documentation says (pages read 2026-10-04):* nothing on either task. Anthropic's own service commitments are in its agreements and on its status page, and what a team promises to its own users is a design choice; the service-level form below and the pilot table are the course's design, taught as the exam's strategy, and the targets and triggers are invented.

## Why it matters

A team promises its sponsor "99 percent accuracy" at launch. The measurement behind it is an average over all disputes, and the credit disputes, the ones that cost 250 when wrong, are right 63 times in 100. Nobody lied; the promise was made in a unit the team did not measure by segment. Two months later a sponsor asks why a quarter of the credits had to be approved by hand, and the answer is a decision that was in the design all along and was never in the expectations. The exam asks how a service level is written, how it is reported when it is missed, and what the lifecycle after the launch looks like.

## The idea

### A service level has three parts

A service level is a **target**, a **measure** and an **owner**. The target has a number and a unit and a direction: a latency is a ceiling (95th percentile of at most 2000 ms), an availability is a floor (at least 99.5 percent of requests answered without error, per month), an accuracy is stated for a named segment (credit at least 98 percent after review). The measure says where the number comes from (trace timings of every answer, a weekly sample read by the quality team) and the owner is a role that can be called when it moves. A target without a measure cannot be reported, and one without an owner is nobody's.

The example reports each one at its edge. The rule has three parts.

- **Met at the limit exactly.** A latency of 2000 ms against a ceiling of 2000 ms is met, and an availability exactly at its floor is met. Writing "below the target" and meaning "at or below" is the edge-case defect that this practice tests.
- **A miss says by how much.** 2150 ms against 2000 ms is "missed by 150 ms", and an availability of 990 per mille against a floor of 995 is "missed by 5 per mille". A bare "failed" starts an argument that a number would have ended.
- **The direction is part of the target.** A ceiling is met below it and a floor is met above it; mixing them up reports a healthy system as broken or the reverse.

### Aligning expectations

A promise is made once and kept for as long as the system runs, so it is made in the unit that is measured. For a system whose errors cost different amounts in different segments, that unit is the **segment**: credit 63 percent, complaint 91 percent, status 98 percent, each with its error cost. The statement to the sponsor is then "a person decides every credit until the measured accuracy rises, and status answers go out automatically", which is a promise the design keeps, where "98 percent overall" is one the design cannot show. Three habits keep expectations honest. Say early what the design cannot promise (accuracy is a distribution, and a confident wrong answer is reduced by a check and never removed). Promise what you measure and measure what you promise. And change a promise through the same channel that made it, with a reason, before the date it matters.

### Feedback loops

The feedback that matters comes from three places, and each has a destination.

- **Reviewers.** The people who approve or correct answers see the failures first. Each correction becomes an evaluation case (module 88), labelled with its segment, so the same failure is tested after every change.
- **Users.** A flag on an answer or an escalation to a person is a signal, counted per 100 disputes and compared with a baseline (module 87).
- **The sponsor.** A regular review of the segments, the saving and the open risks, with the decisions it needs.

A loop is closed when a change goes back out through the gate of module 89 and the person who raised the problem is told what happened. A stakeholder review that produces notes and no cases has not closed anything.

### The lifecycle after the launch

The guide lists five phases: discovery, design, handoff, monitoring and iteration. The first two are the first page. The three that follow are where designs are lost.

**Pilot to scale.** A pilot proves a design under conditions that are kinder than production, and each kindness is an assumption. The record lists them, each with a **test** and a **stop trigger** that has a number in it. The dispute assistant's four: the inputs were typical (sample production disputes and score them by segment; stop if credit accuracy falls below 60 percent on the sample); staff covered the edge cases by hand (count escalations per 100 disputes; stop above 12); capacity was never close (replay peak load against the rate limits; stop at any 429 error at 70 percent of the limit); reviewers kept up (compute reviewer hours from volume and routing; stop when the queue is older than 4 hours). A trigger that says "if it goes wrong" is not a trigger, and an assumption without a test is a hope. The trigger sits exactly at its number, so a case at the limit and a case one past it behave differently, as the practice tests.

**Hand-off.** The people who run the system are not the people who built it, and the record says who they are: the owner of the service (a role), the owner of the runbook that explains each alert, the monitors (at least two signals, such as refusals, tokens per answer, the share of answers a person flagged and the 95th percentile latency, each compared with a baseline in both directions) and the rollback, which sends every request to the previous model that stays configured and tested until its own retirement date (module 89). Implementation guidance is part of the hand-off and not an extra: a diagram without the runbook leaves the first incident to the person who happens to remember.

**Monitoring and iteration.** The record is reviewed at each stage of the roll-out, and each review is allowed to change the design: a new segment appears, a threshold moves, a model is replaced. Iteration is not a failure of the design; it is the part of the design that was planned for.

### The example

The example is the same as on the first page, and its second half is this page: five lines for the service levels (a latency of 1800 ms met, 2000 ms met at the ceiling, 2150 ms missed by 150 ms, an availability of 997 per mille met and 990 missed by 5), the report by segment with the costliest first and the two briefs. It ran offline in every language.

<!-- example: m91-tradeoff-brief tabs: python,typescript,java,kotlin -->
<!-- /example -->

### The practice: a design record

The practice is in [`exercises/91-stakeholders-and-the-project-lifecycle`](../../exercises/91-stakeholders-and-the-project-lifecycle/unit-01/practice-1/statement.md), the same record as on the first page. The sections that belong here are the service levels (each with a number in the right unit, a measure and a named owner), the pilot-to-scale table (at least four rows, every stop trigger with a number) and the hand-off (the owner, the runbook, at least two monitors, the rollback and the previous model).

## Traps

1. **"Promise the sponsor 99 percent accuracy; it is the average we measured."** It is tempting because one big number is easy to say and to approve. The exam rejects it because an average hides the segments that cost the most; promise per segment, in the unit that is measured, and say which ones a person decides.
2. **"Report a service level that is only just missed as met, since it is within noise."** It is tempting because the miss is small and the report is cleaner. The exam rejects it because a service level is met at its limit exactly and missed beyond it; say by how much, and let the owner decide what a small miss means.
3. **"Hand over the design diagram; the runbook can follow later."** It is tempting because the diagram is finished and the runbook is not. The exam rejects it because the people who run the system need an owner, a runbook, monitors and a rollback on day one; a diagram alone leaves the first alert to chance.

## Quiz

1. Scenario: Ola's team reports its latency service level, a ceiling of 2000 ms at the 95th percentile. The week's measured figure is exactly 2000 ms. How does the report read?
   - **a**: Missed by 0 ms, since the measure has reached the limit
   - **b**: Met, since a value at the limit counts as inside it
   - **c**: Met, but with the ceiling raised to 2100 ms for the week
   - **d**: Undecided until a second week of measurements arrives

2. Scenario: Vera is writing the pilot table for a billing assistant and offers the stop trigger "if the quality is worse than we hoped". What is wrong with it?
   - **a**: It should name the reviewer who decides when quality is poor enough
   - **b**: It states a quality aim, which belongs to the sponsor and not the pilot
   - **c**: It should come after the roll-out so that the data exists to judge it
   - **d**: It holds no number, so the moment of reaching it is a matter of opinion

<details>
<summary>Answer key</summary>

1. **b**. A service level is met at its limit exactly. *a* is ruled out because "A latency of 2000 ms against a ceiling of 2000 ms is met". *c* is ruled out because the ceiling is the target, and "Met at the limit exactly" leaves no reason to raise it. *d* is ruled out because "Met at the limit exactly" settles the case without a second week.
2. **d**. A trigger needs a number to be reached. *a* is ruled out because "A trigger that says \"if it goes wrong\" is not a trigger", whoever decides. *c* is ruled out because the record lists each assumption "with a test and a stop trigger that has a number in it" before the pilot runs. *b* is ruled out because the table's triggers belong to the pilot, whose assumptions "each with a test" the record lists.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Noor tells a sponsor that an assistant is "98 percent accurate overall". The system answers three kinds of dispute, and the one that costs 250 per error is right 63 times in 100. What should the record say to the sponsor?
   - **a**: That the costly kind is excluded from the figure because it is handled in another system
   - **b**: That the overall figure stands, since the costly kind is a small share of the traffic
   - **c**: That a person decides each of those until the measured result improves
   - **d**: That the figure will be re-measured once per quarter, with the same overall method

2. Scenario: Lars hands a finished assistant to an operations team with an architecture diagram and a list of settings. The first alert fires on a Saturday and nobody knows who should act on it. Which part of the hand-off was missing?
   - **a**: A longer pilot, so that more alerts would have fired before the launch
   - **b**: A named owner for the service and a runbook that tells staff what to do
   - **c**: A monthly report to the sponsor listing the alerts of the previous period
   - **d**: A second diagram showing the alert thresholds drawn on the architecture

3. Scenario: A reviewer corrects forty answers in a week, and the notes of the sponsor review record them. Nothing else changes, and the same errors appear the next week. Which step would have closed the loop?
   - **a**: Sending the notes to the engineers with a request to read them before the next release
   - **b**: Asking the reviewers to correct the same answers a second time for the record
   - **c**: Moving the review from weekly to daily so that the notes are fresher
   - **d**: Turning each fix into a labelled evaluation case that each later release is run against

<details>
<summary>Answer key</summary>

1. **c**. The promise is made per segment, and a person decides the costly one. *b* is ruled out because "an average hides the segments that cost the most", whatever the traffic share. *a* is ruled out because the design "promise what you measure and measure what you promise", and excluding a segment breaks both. *d* is ruled out because "a promise is made once and kept for as long as the system runs", so re-measuring the same overall figure repeats the same blind spot.
2. **b**. The record names an owner and a runbook for each alert. *a* is ruled out because "a diagram without the runbook leaves the first incident to the person who happens to remember", and a longer pilot adds no owner. *c* is ruled out because "The people who run the system are not the people who built it", and a report to the sponsor does not reach them on a Saturday. *d* is ruled out because "Implementation guidance is part of the hand-off and not an extra", and a second diagram adds none.
3. **d**. A loop is closed when a change goes back out through the gate. *b* is ruled out because "A stakeholder review that produces notes and no cases has not closed anything". *c* is ruled out because the review is "A regular review of the segments, the saving and the open risks", and its speed adds no case. *a* is ruled out because a loop closes only when "the person who raised the problem is told what happened" after a change has gone through the gate.

</details>
