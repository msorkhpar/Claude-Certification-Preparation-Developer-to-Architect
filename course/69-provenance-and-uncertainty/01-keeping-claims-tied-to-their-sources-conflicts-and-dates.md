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
```python
"""What a summary loses, and what a ledger keeps: sources, dates and disagreement.

The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
"""
import logging

log = logging.getLogger(__name__)

FINDINGS = [
    ("market growth 2024", "12%", "Firm A report", "2024-05-01"),
    ("market growth 2024", "9%", "Firm B survey", "2024-05-01"),
    ("growth forecast", "7%", "Firm C yearbook", "2022-04-01"),
    ("growth forecast", "9%", "Firm B survey", "2024-05-01"),
    ("inflation 2023", "4%", "Firm A report", "2024-05-01"),
    ("inflation 2023", "4%", "Trade paper", "2024-06-10"),
    ("headcount", "910", "Press release", "2024-03-01"),
]


def claims_in_order(findings):
    return list(dict.fromkeys(claim for claim, _, _, _ in findings))


def status(rows):
    """agreed: one value; conflict: different values on the same date; changed: different values on different dates."""
    if len({value for _, value, _, _ in rows}) == 1:
        return "agreed"
    if any(a[1] != b[1] and a[3] == b[3] for a in rows for b in rows):
        return "conflict"
    return "changed"


def plain_summary(findings):
    """One line per claim with the first value seen: short, and the sources are gone."""
    return "\n".join(f"{claim}: {next(v for c, v, _, _ in findings if c == claim)}" for claim in claims_in_order(findings))


def ledger_lines(findings):
    """One line per claim: its status, then every value with its source and date, the oldest date first for a change."""
    lines = []
    for claim in claims_in_order(findings):
        rows = [f for f in findings if f[0] == claim]
        if status(rows) == "changed":
            rows = sorted(rows, key=lambda r: r[3])
        lines.append(f"{claim} [{status(rows)}]: " + "; ".join(f"{value} ({source}, {date})" for _, value, source, date in rows))
    return "\n".join(lines)


def sources_named(text, findings):
    return len({source for _, _, source, _ in findings if source in text})


def main():
    total = len({source for _, _, source, _ in FINDINGS})
    print(f"findings: {len(FINDINGS)} from {total} sources")
    summary = plain_summary(FINDINGS)
    print("plain summary:")
    print(summary)
    print(f"sources named by the plain summary: {sources_named(summary, FINDINGS)} of {total}")
    ledger = ledger_lines(FINDINGS)
    print("ledger:")
    print(ledger)
    print(f"sources named by the ledger: {sources_named(ledger, FINDINGS)} of {total}")


if __name__ == "__main__":
    main()
```
```text
findings: 7 from 5 sources
plain summary:
market growth 2024: 12%
growth forecast: 7%
inflation 2023: 4%
headcount: 910
sources named by the plain summary: 0 of 5
ledger:
market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)
growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)
inflation 2023 [agreed]: 4% (Firm A report, 2024-05-01); 4% (Trade paper, 2024-06-10)
headcount [agreed]: 910 (Press release, 2024-03-01)
sources named by the ledger: 5 of 5
```
```typescript
import { logger } from "./logger.ts";
const log = logger("provenance_loss");
/**
 * What a summary loses, and what a ledger keeps: sources, dates and disagreement.
 *
 * The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
 * settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
 * a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
 */
export type Row = [string, string, string, string];

export const FINDINGS: Row[] = [
  ["market growth 2024", "12%", "Firm A report", "2024-05-01"],
  ["market growth 2024", "9%", "Firm B survey", "2024-05-01"],
  ["growth forecast", "7%", "Firm C yearbook", "2022-04-01"],
  ["growth forecast", "9%", "Firm B survey", "2024-05-01"],
  ["inflation 2023", "4%", "Firm A report", "2024-05-01"],
  ["inflation 2023", "4%", "Trade paper", "2024-06-10"],
  ["headcount", "910", "Press release", "2024-03-01"],
];

const claimsInOrder = (findings: Row[]): string[] => [...new Set(findings.map((f) => f[0]))];

/** agreed: one value; conflict: different values on the same date; changed: different values on different dates. */
export function status(rows: Row[]): string {
  if (new Set(rows.map((r) => r[1])).size === 1) return "agreed";
  if (rows.some((a) => rows.some((b) => a[1] !== b[1] && a[3] === b[3]))) return "conflict";
  return "changed";
}

/** One line per claim with the first value seen: short, and the sources are gone. */
export function plainSummary(findings: Row[]): string {
  return claimsInOrder(findings).map((claim) => `${claim}: ${findings.find((f) => f[0] === claim)![1]}`).join("\n");
}

/** One line per claim: its status, then every value with its source and date, the oldest date first for a change. */
export function ledgerLines(findings: Row[]): string {
  return claimsInOrder(findings).map((claim) => {
    let rows = findings.filter((f) => f[0] === claim);
    if (status(rows) === "changed") rows = [...rows].sort((a, b) => (a[3] < b[3] ? -1 : a[3] > b[3] ? 1 : 0));
    return `${claim} [${status(rows)}]: ` + rows.map(([, value, source, date]) => `${value} (${source}, ${date})`).join("; ");
  }).join("\n");
}

export function sourcesNamed(text: string, findings: Row[]): number {
  return new Set(findings.filter((f) => text.includes(f[2])).map((f) => f[2])).size;
}

export function main(): void {
  const total = new Set(FINDINGS.map((f) => f[2])).size;
  console.log(`findings: ${FINDINGS.length} from ${total} sources`);
  const summary = plainSummary(FINDINGS);
  console.log("plain summary:");
  console.log(summary);
  console.log(`sources named by the plain summary: ${sourcesNamed(summary, FINDINGS)} of ${total}`);
  const ledger = ledgerLines(FINDINGS);
  console.log("ledger:");
  console.log(ledger);
  console.log(`sources named by the ledger: ${sourcesNamed(ledger, FINDINGS)} of ${total}`);
}

if (import.meta.main) main();
```
```text
findings: 7 from 5 sources
plain summary:
market growth 2024: 12%
growth forecast: 7%
inflation 2023: 4%
headcount: 910
sources named by the plain summary: 0 of 5
ledger:
market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)
growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)
inflation 2023 [agreed]: 4% (Firm A report, 2024-05-01); 4% (Trade paper, 2024-06-10)
headcount [agreed]: 910 (Press release, 2024-03-01)
sources named by the ledger: 5 of 5
```
```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a summary loses, and what a ledger keeps: sources, dates and disagreement.
 *
 * <p>The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
 * settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
 * a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
 */
public final class ProvenanceLoss {
    private static final System.Logger LOG = System.getLogger(ProvenanceLoss.class.getName());
    record Row(String claim, String value, String source, String date) {}

    static final List<Row> FINDINGS = List.of(
        new Row("market growth 2024", "12%", "Firm A report", "2024-05-01"),
        new Row("market growth 2024", "9%", "Firm B survey", "2024-05-01"),
        new Row("growth forecast", "7%", "Firm C yearbook", "2022-04-01"),
        new Row("growth forecast", "9%", "Firm B survey", "2024-05-01"),
        new Row("inflation 2023", "4%", "Firm A report", "2024-05-01"),
        new Row("inflation 2023", "4%", "Trade paper", "2024-06-10"),
        new Row("headcount", "910", "Press release", "2024-03-01"));

    private static List<String> claimsInOrder(List<Row> findings) {
        Set<String> claims = new LinkedHashSet<>();
        for (Row r : findings) claims.add(r.claim());
        return new ArrayList<>(claims);
    }

    /** agreed: one value; conflict: different values on the same date; changed: different values on different dates. */
    static String status(List<Row> rows) {
        if (rows.stream().map(Row::value).distinct().count() == 1) return "agreed";
        if (rows.stream().anyMatch(a -> rows.stream().anyMatch(b -> !a.value().equals(b.value()) && a.date().equals(b.date())))) return "conflict";
        return "changed";
    }

    /** One line per claim with the first value seen: short, and the sources are gone. */
    static String plainSummary(List<Row> findings) {
        List<String> lines = new ArrayList<>();
        for (String claim : claimsInOrder(findings)) lines.add(claim + ": " + findings.stream().filter(f -> f.claim().equals(claim)).findFirst().orElseThrow().value());
        return String.join("\n", lines);
    }

    /** One line per claim: its status, then every value with its source and date, the oldest date first for a change. */
    static String ledgerLines(List<Row> findings) {
        List<String> lines = new ArrayList<>();
        for (String claim : claimsInOrder(findings)) {
            List<Row> rows = new ArrayList<>(findings.stream().filter(f -> f.claim().equals(claim)).toList());
            String status = status(rows);
            if (status.equals("changed")) rows.sort(Comparator.comparing(Row::date));
            List<String> parts = new ArrayList<>();
            for (Row r : rows) parts.add(r.value() + " (" + r.source() + ", " + r.date() + ")");
            lines.add(claim + " [" + status + "]: " + String.join("; ", parts));
        }
        return String.join("\n", lines);
    }

    static int sourcesNamed(String text, List<Row> findings) {
        return (int) findings.stream().filter(f -> text.contains(f.source())).map(Row::source).distinct().count();
    }

    public static void main(String[] args) {
        int total = (int) FINDINGS.stream().map(Row::source).distinct().count();
        System.out.println("findings: " + FINDINGS.size() + " from " + total + " sources");
        String summary = plainSummary(FINDINGS);
        System.out.println("plain summary:");
        System.out.println(summary);
        System.out.println("sources named by the plain summary: " + sourcesNamed(summary, FINDINGS) + " of " + total);
        String ledger = ledgerLines(FINDINGS);
        System.out.println("ledger:");
        System.out.println(ledger);
        System.out.println("sources named by the ledger: " + sourcesNamed(ledger, FINDINGS) + " of " + total);
    }
}
```
```text
findings: 7 from 5 sources
plain summary:
market growth 2024: 12%
growth forecast: 7%
inflation 2023: 4%
headcount: 910
sources named by the plain summary: 0 of 5
ledger:
market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)
growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)
inflation 2023 [agreed]: 4% (Firm A report, 2024-05-01); 4% (Trade paper, 2024-06-10)
headcount [agreed]: 910 (Press release, 2024-03-01)
sources named by the ledger: 5 of 5
```
```kotlin
private val log = System.getLogger("provenance_loss")

/**
 * What a summary loses, and what a ledger keeps: sources, dates and disagreement.
 *
 * The exam guide (task 5.6) says that source attribution is lost when findings are compressed without their claim-source mappings, that conflicting statistics from credible sources are annotated with their sources and not
 * settled by choosing one, and that dates are required so that a difference over time is not read as a contradiction. Below, seven findings from five invented sources are compressed twice (nothing here calls a model): once into
 * a plain summary that keeps one value per claim, and once into a ledger line per claim that keeps every value with its source and date. The names and figures are invented for the illustration.
 */
data class Row(val claim: String, val value: String, val source: String, val date: String)

val FINDINGS = listOf(
    Row("market growth 2024", "12%", "Firm A report", "2024-05-01"),
    Row("market growth 2024", "9%", "Firm B survey", "2024-05-01"),
    Row("growth forecast", "7%", "Firm C yearbook", "2022-04-01"),
    Row("growth forecast", "9%", "Firm B survey", "2024-05-01"),
    Row("inflation 2023", "4%", "Firm A report", "2024-05-01"),
    Row("inflation 2023", "4%", "Trade paper", "2024-06-10"),
    Row("headcount", "910", "Press release", "2024-03-01"),
)

/** agreed: one value; conflict: different values on the same date; changed: different values on different dates. */
fun status(rows: List<Row>): String = when {
    rows.map { it.value }.distinct().size == 1 -> "agreed"
    rows.any { a -> rows.any { b -> a.value != b.value && a.date == b.date } } -> "conflict"
    else -> "changed"
}

/** One line per claim with the first value seen: short, and the sources are gone. */
fun plainSummary(findings: List<Row>): String = findings.map { it.claim }.distinct().joinToString("\n") { claim -> "$claim: ${findings.first { it.claim == claim }.value}" }

/** One line per claim: its status, then every value with its source and date, the oldest date first for a change. */
fun ledgerLines(findings: List<Row>): String = findings.map { it.claim }.distinct().joinToString("\n") { claim ->
    val group = findings.filter { it.claim == claim }
    val rows = if (status(group) == "changed") group.sortedBy { it.date } else group
    "$claim [${status(rows)}]: " + rows.joinToString("; ") { "${it.value} (${it.source}, ${it.date})" }
}

fun sourcesNamed(text: String, findings: List<Row>): Int = findings.filter { text.contains(it.source) }.map { it.source }.distinct().size

fun main() {
    val total = FINDINGS.map { it.source }.distinct().size
    println("findings: ${FINDINGS.size} from $total sources")
    val summary = plainSummary(FINDINGS)
    println("plain summary:")
    println(summary)
    println("sources named by the plain summary: ${sourcesNamed(summary, FINDINGS)} of $total")
    val ledger = ledgerLines(FINDINGS)
    println("ledger:")
    println(ledger)
    println("sources named by the ledger: ${sourcesNamed(ledger, FINDINGS)} of $total")
}
```
```text
findings: 7 from 5 sources
plain summary:
market growth 2024: 12%
growth forecast: 7%
inflation 2023: 4%
headcount: 910
sources named by the plain summary: 0 of 5
ledger:
market growth 2024 [conflict]: 12% (Firm A report, 2024-05-01); 9% (Firm B survey, 2024-05-01)
growth forecast [changed]: 7% (Firm C yearbook, 2022-04-01); 9% (Firm B survey, 2024-05-01)
inflation 2023 [agreed]: 4% (Firm A report, 2024-05-01); 4% (Trade paper, 2024-06-10)
headcount [agreed]: 910 (Press release, 2024-03-01)
sources named by the ledger: 5 of 5
```
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
