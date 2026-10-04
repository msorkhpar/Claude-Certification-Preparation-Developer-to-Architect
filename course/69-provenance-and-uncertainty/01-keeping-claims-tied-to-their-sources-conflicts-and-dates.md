# Keeping claims tied to their sources: summaries, conflicts and dates

**Level:** Architect · **Module 69:** Provenance and uncertainty · **Page 1 of 2**
**Exams:** A5.6; S3

**After this page you can** say where a source is lost when findings are compressed, require a claim to carry its source and its date from the subagent that found it to the report that uses it, annotate two credible sources that disagree instead of choosing between them, tell a change over time from a contradiction, and read a ledger of claims with a status for each.

Checked on 2026-10-04 against the exam guide's task statement 5.6 and scenario S3 (multi-agent research system). Nothing here called a model: the example compresses seven invented findings two ways and counts what each keeps (`examples/69-provenance-loss`), and the practice is graded by test suites. Module 47 kept the content of a finding apart from its source so that a merge keeps every source, and module 29 covered citations on a single answer. This page is about what happens to a finding on its way through several agents and a summary, and what the report does when sources disagree.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* attribution "is lost during summarization steps when findings are compressed without preserving claim-source mappings"; the synthesis agent must "preserve and merge" structured claim-source mappings; for "conflicting statistics from credible sources" the answer is "annotating conflicts with source attribution rather than arbitrarily selecting one value"; and subagents are required to output publication or data collection dates "to prevent temporal differences from being misinterpreted as contradictions". *What the product offers now:* the citations feature (module 29) returns the passage and location behind a claim in one answer, and the Agent SDK passes a subagent's final message to its parent as a result (module 47). Neither carries a claim-source mapping from one agent to the next: the mapping is data that your schema defines and your code preserves. The merge, the conflict status and the date rule below are the exam's design and the course's, not a product feature. On the exam, a question about sources that vanish or numbers that disagree has the answer: structured claim and source fields kept through every step, conflicts annotated with their sources, dates required.

## Why it matters

Three subagents read three kinds of source for a report on a market. One returns "the market grew 12% in 2024" from a consultancy report, another "9%" from a trade survey, and a third "7%" from a yearbook. The synthesis agent writes "the market grew about 10%". The number belongs to nobody: it is not in any source, it hides that two credible sources disagree, and it mixes a 2022 forecast with 2024 measurements. A reader who wants to check it cannot. The scenario (S3) tests whether the design keeps the evidence that a research report is for.

## The idea

### Where a source is lost

A source survives as long as it travels with its claim in a field. It is lost the moment a step turns findings into prose: a subagent that writes "according to several reports, growth was strong", a summary that shortens forty findings to one paragraph, a synthesis that merges two sentences into one. Each step is reasonable, and each is lossy: a summary has no obligation to keep what it was not told to keep. The guide's point is that the loss happens in compression, so the fix is in the step. Every hand-off between agents carries claim-source mappings (claim, value, source, excerpt, date), and a compression step keeps them next to the text it produces, or passes them on untouched for the final step to use.

Module 47's lesson is the first half of this: keep content and source in separate fields so that a merge sees one claim. The second half is the discipline over the whole pipeline. A single prose hand-off anywhere in the chain is where attribution drops out.

### Provenance is a required field

The guide asks for subagents that "output structured claim-source mappings (source URLs, document names, relevant excerpts)". The design choice is to make the source part of the schema and required, so that a finding without one is refused where it enters the pipeline, and not discovered in the final report. The practice's `check_finding` names the missing fields and `merge` refuses an incomplete finding.

A required source has a trap beside it, which module 62 met in extraction: a required field pushes a model to invent a value when there is none. The answer is the same here. A subagent that cannot name a source for a statement does not return it as a finding: it reports it separately as unsourced, or leaves it out, and the schema stays honest. What the pipeline never does is fill the field with a plausible name.

### Conflicts are annotated, not settled

When two credible sources give different values for the same claim, the report has learned something: the figure is contested. An agent that picks the newer, the larger or the one from the more familiar publisher has made a decision that nobody asked it to make and that the reader cannot see. Averaging is worse, as the example above shows. The practice keeps both values, each with its sources, and gives the claim the status `conflict`. The guide adds a step before the synthesis: finish the document analysis with the conflicting values included and annotated, and let the coordinator decide how to reconcile before the synthesis. The coordinator may know that one survey sampled a narrower population, or may send a subagent to look for a third source. The one thing the system does not do is hide the disagreement.

### A date can explain a difference

Two figures that differ may be answers to different questions. A 2022 forecast of 7% and a 2024 measurement of 9% do not contradict each other: the world, or the estimate, moved. Without dates, an agent cannot tell, and it reports a contradiction that is not one, or it silently takes one value. So the date is required, like the source: the publication date or the data collection date, whichever the source gives. The practice's rule is a simple one. Two findings for a claim with different values and the same date are a `conflict`. With different dates they are `changed`, listed oldest first, and the report says that the figures come from different dates. A date can only tell a change from a conflict when it describes the data: a publication date is a proxy for the collection date, and a careful report says which one it holds.

### Reading the ledger

The merged claims are a ledger, and each entry has one of three statuses:

| Status | Meaning | The report |
|---|---|---|
| `agreed` | One value, with one or more sources | States it with its sources; two or more sources make it well supported |
| `changed` | Several values on different dates | Shows each value with its date, oldest first |
| `conflict` | Several values on the same date | Shows each value with its sources and says the sources disagree |

### The example

<!-- example: m69-provenance-loss tabs: python,typescript,java,kotlin -->
<!-- /example -->

The example compresses seven findings from five invented sources twice. The plain summary keeps the first value of each claim: it is shorter, it hides the second source of the conflict entirely, and it names none of the five sources. The ledger lines keep every value with its source and date, flag the conflict, put the changed claim's older value first and name all five sources. The figures are invented for the illustration, and the count of sources named is the measurement the example is about.

## Traps

1. **"Summarise the findings first; the writer will add sources later."** It is tempting because short input is easier to write from. The exam rejects it: the sources are lost in the compression and cannot be rebuilt from prose, so the claim-source mappings travel through every step.
2. **"Where two sources disagree, report the more recent figure."** It is tempting because newer sounds more reliable. The exam rejects it: choosing one value hides a disagreement the reader needs, so annotate the conflict with both sources.
3. **"Average the two figures into one number."** It is tempting because it reads as a compromise. The exam rejects it: the number appears in no source and cannot be checked.
4. **"Two different figures mean the sources contradict each other."** It is tempting because the numbers differ. The exam rejects it: one may be a forecast from 2022 and the other a measurement from 2024, so require dates before calling it a conflict.

## Quiz

1. Scenario S3, multi-agent research system. A consultancy report gives a market's growth for 2024 as 12%, and an equally reputable trade survey gives 9% for the same year. How should the final report treat the two?
   - **a**: Present both values with the name of each source and a note that they disagree
   - **b**: Present the value from the larger of the two publishers, because it is likelier to be right
   - **c**: Present a single figure between the two, which fairly reflects both of the sources
   - **d**: Present only the more recently published value and leave the other out for brevity

2. Scenario S3, multi-agent research system. A report calls two numbers contradictory: one subagent found 7% in a source and another found 9% in a different source. Which change to the subagents' output prevents the misreading?
   - **a**: Require each subagent to rank its sources by how well known they are
   - **b**: Require a longer excerpt from each source around every number
   - **c**: Require a publication or data collection date next to every value
   - **d**: Require the subagents to agree a single number before they return

<details>
<summary>Answer key</summary>

1. **a**. The guide's answer is "annotating conflicts with source attribution rather than arbitrarily selecting one value", so both values stay, each with its sources. *b* is ruled out because "choosing one value hides a disagreement the reader needs". *c* is ruled out because the averaged number "appears in no source and cannot be checked". *d* is ruled out because an agent that picks the newer figure "has made a decision that nobody asked it to make and that the reader cannot see".
2. **c**. The guide asks for dates "to prevent temporal differences from being misinterpreted as contradictions", since the date shows whether the two numbers describe the same moment. *b* is ruled out because an excerpt shows the wording and not the moment, and "Without dates, an agent cannot tell". *a* is ruled out because ranking sources by reputation says nothing about when a number was gathered, and the page's rule turns on whether "Two findings for a claim with different values and the same date" exist. *d* is ruled out because "choosing one value hides a disagreement the reader needs".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
