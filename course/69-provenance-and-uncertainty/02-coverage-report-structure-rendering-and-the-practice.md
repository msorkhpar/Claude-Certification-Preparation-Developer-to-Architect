# Coverage, report structure, rendering by content type, and the practice

**Level:** Architect · **Module 69:** Provenance and uncertainty · **Page 2 of 2**
**Exams:** A5.6; S3

**After this page you can** write a coverage note that separates well-supported claims from single-source, changed and contested ones and lists what could not be covered, lay a report out so that established and contested findings are apart, keep each source's own characterisation and method next to its figure, render financial data as a table, news as prose and technical findings as a list, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 5.6 and scenario S3. Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline. Module 66 built a coverage note from what happened to each subagent (a failure, an empty result, a gap); this page builds one from the evidence itself. The first page kept claims tied to their sources; this page is about presenting them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* reports are structured "with explicit sections distinguishing well-established findings from contested ones, preserving original source characterizations and methodological context"; and different content types are rendered "appropriately in synthesis outputs", with "financial data as tables, news as prose, technical findings as structured lists", "rather than converting everything to a uniform format". The guide's task 5.3 asked for coverage annotations on "which findings are well-supported versus which topic areas have gaps"; this page adds the contested and the single-source claim to them. *What the product offers now:* the product has no report format or coverage facility: the structure of a synthesis is the prompt and the code of the synthesis agent, and the rendering rules are instructions or code of yours. Module 47 and module 66 covered the mechanisms that carry the data. On the exam, a question about the shape of a research report has the answer: sections for established and contested findings, sources and methods kept, each content type in its fitting form.

## Why it matters

A research agent's report is read by someone who will act on it. If every claim is a bullet of the same weight, the reader cannot tell the figure that three sources confirm from the one that a single blog states, or the one on which two reports disagree. If a table of quarterly revenue is turned into a paragraph, the reader cannot compare the quarters; if a news story is turned into a table, it loses its causes and its sequence. The scenario (S3) tests whether the report's structure carries the uncertainty that the pipeline measured.

## The idea

### A coverage note from the evidence

Module 66's note said which topics had no result and why. A note built from the ledger of page 1 says more. For each planned claim it puts the claim in one place:

| In the note | When |
|---|---|
| Well supported | `agreed`, and the value has at least two distinct sources |
| Single source | `agreed`, and one source only |
| Changed | Several values on different dates |
| Contested | Several values on the same date |
| Gap | Planned, and no finding: with the reason, or `no source found` |

The practice's `coverage_note` is that table. Two details matter. "At least two distinct sources" counts source names: the same report cited on two dates is one source, and agreement needs independence. And the gap line carries the reason from the failed or missing source, for the same reason as in module 66: a reader acts on "the registry timed out", and cannot act on "some sources may be unavailable".

### Structure the report by certainty

The guide asks for a report with explicit sections for well-established and for contested findings. A reader then meets the settled facts first and the open questions in a place of their own, each with what is needed to judge it. Inside a finding the report keeps what the source said about itself and how it knew: the source's own characterisation ("preliminary", "estimate", "audited") and the method ("survey of 400 firms", "from filings"). A figure from a survey and a figure from an audit are not interchangeable, and dropping the method turns a disagreement of method into a mystery. These are fields of the finding like the source and the date, and the synthesis keeps them as it keeps the others.

### Render each content type fittingly

The last skill on the list looks cosmetic and is not. Each kind of content has a form that serves its use:

- **Financial and other numeric data: a table.** The reader compares values across sources, dates and periods. The practice renders a table with a row for each source of each value, so a conflict is visible as two rows.
- **News and narrative: prose.** The reader needs the sequence and the causes, which a table breaks apart. The practice renders a sentence, adds that the sources disagree for a conflict, and says that the figures are from different dates for a change.
- **Technical findings: a structured list.** The reader scans for items, such as limits, versions or steps, each with its source.

The rule against "a uniform format" is about the failure of the single template: a pipeline that turns everything into bullets, or everything into paragraphs, makes some content unusable. The renderer is chosen per content type, and an unknown type is refused and not guessed.

### The practice: a ledger that keeps its sources

The practice is in [`exercises/69-provenance-and-uncertainty`](../../exercises/69-provenance-and-uncertainty/unit-01/practice-1/statement.md). You write the check for required fields, the merge with its three statuses, the coverage note and the three renderings. It is graded in Python, TypeScript, Java and Kotlin; the statement lists nine cases, each saying what you should see when it works. The outline marks this module as code, and the practice is the data layer of a research report without the model.

## Traps

1. **"List every finding as a bullet; a long uniform list is easier to read."** It is tempting because it needs no judgement. The exam rejects it: the reader cannot tell what is established from what is contested, so give each its own section.
2. **"Render all of the data as prose, so that the report reads smoothly."** It is tempting because prose looks finished. The exam rejects it: a table of figures turned into sentences cannot be compared, so render each content type in its fitting form.
3. **"The same report cited on two dates counts as two sources."** It is tempting because there are two entries. The exam rejects it: agreement needs independent sources, so count distinct source names.
4. **"Drop the survey method from the figure; it clutters the line."** It is tempting because the number is what the reader wants. The exam rejects it: the method is part of the finding, and without it a disagreement between a survey and an audit looks like an error.

## Quiz

1. Scenario S3, multi-agent research system. A synthesis agent holds sales numbers for five companies over four quarters, a short account of a takeover and the usage caps of a vendor's service. How should it present them?
   - **a**: Present them in one uniform bulleted layout, for consistency across the whole report
   - **b**: Lay out the figures as a grid, the story as running text and the quotas as bullet points
   - **c**: Present them in one uniform block of paragraphs, so that the report reads smoothly
   - **d**: Present them in one uniform grid, so that each value sits in a column of its own, whatever it is

2. Scenario S3, multi-agent research system. A draft report mixes claims that three independent sources confirm with one where two reports disagree. A reviewer says readers cannot tell the two kinds apart. Which change fits the guide's advice?
   - **a**: Place the oldest results first and the newest last within each part
   - **b**: Add a confidence percentage in brackets after each sentence of the report
   - **c**: Remove the disputed claims until the sources can be reconciled by someone
   - **d**: Give the established and the contested results sections of their own

<details>
<summary>Answer key</summary>

1. **b**. The guide renders "financial data as tables, news as prose, technical findings as structured lists". *a* is ruled out because the guide warns against "converting everything to a uniform format". *c* is ruled out because "a table of figures turned into sentences cannot be compared". *d* is ruled out because when a news story is turned into a table, "it loses its causes and its sequence".
2. **d**. The guide asks for "explicit sections distinguishing well-established findings from contested ones". *b* is ruled out because a number after each sentence leaves the two mixed, while the structure puts "the open questions in a place of their own". *c* is ruled out because removal takes away the open questions "each with what is needed to judge it". *a* is ruled out because ordering by age does not let "A reader then meets the settled facts first".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S3, multi-agent research system. A summariser squeezes forty items gathered by four subagents into one paragraph for the writer, and the finished report cannot say where any number came from. Where did the fault lie?
   - **a**: The writer lacked an instruction to cite sources while composing the finished document
   - **b**: The subagents gathered far too much material for one writer to handle within a single run
   - **c**: The compression stage converted structured entries into prose and dropped the claim-source mappings
   - **d**: The four subagents used different search tools to reach their results, which broke the trail

2. Scenario S3, multi-agent research system. A forecast dated 2022 gives 7% and a measurement dated 2024 gives 9% for one quantity. What should the write-up show?
   - **a**: A contested finding, since two numbers cannot both hold for one quantity at the same time
   - **b**: Both values with their dates, the older first, and a note that the dates differ
   - **c**: The newer value by itself, because the older one has been superseded
   - **d**: A single value between the two, to cover the whole span of years

3. Scenario S3, multi-agent research system. A subagent half remembers a statistic but finds no document stating it. What should its output hold?
   - **a**: An entry that names the subagent itself as the origin of the number
   - **b**: An entry that fills the source field with the most likely publication
   - **c**: An entry with an empty source field that the writer is asked to complete before it goes out
   - **d**: No entry for it among the results, plus a note that it could not be traced

<details>
<summary>Answer key</summary>

1. **c**. The guide says attribution "is lost during summarization steps when findings are compressed without preserving claim-source mappings". *b* is ruled out because "A source survives as long as it travels with its claim in a field", whatever the volume. *a* is ruled out because "the sources are lost in the compression and cannot be rebuilt from prose". *d* is ruled out because "Every hand-off between agents carries claim-source mappings", and the search tool does not decide that.
2. **b**. The page lists them "listed oldest first, and the report says that the figures come from different dates". *a* is ruled out because "Two findings for a claim with different values and the same date are a `conflict`", and these dates differ. *c* is ruled out because "choosing one value hides a disagreement the reader needs". *d* is ruled out because "the number appears in no source and cannot be checked".
3. **d**. The subagent "reports it separately as unsourced, or leaves it out". *b* is ruled out because "What the pipeline never does is fill the field with a plausible name". *c* is ruled out because "a finding without one is refused where it enters the pipeline". *a* is ruled out because the design is to "make the source part of the schema and required", and a subagent is not the source of a fact it recalls.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
