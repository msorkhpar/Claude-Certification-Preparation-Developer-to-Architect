# Examples that settle the hard cases, asking and the practice

**Level:** Architect · **Module 61:** Criteria and examples · **Page 2 of 2**
**Exams:** A4.2; S5

**After this page you can** choose few-shot examples that settle the ambiguous cases and show an acceptable pattern next to a finding, give each example a reason, place examples between the criteria and the diff, decide when a request with missing fields is asked about and when its assumptions are stated, and write the module's practice.

Checked on 2026-10-03 against the Claude prompting guidance pages on using examples effectively and on structuring prompts, with the exam guide's task statement 4.2. Nothing here called a model: the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline, on a review specification (`examples/61-criteria-lint` holds the checks the page describes). This page deepens module 24 (examples and structure) and does not repeat it. The criteria and the trust in a category are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* for few-shot prompting (4.2) it prescribes "2-4 targeted few-shot examples" for ambiguous scenarios, each showing the reasoning for why one action was chosen over the plausible alternatives; examples that demonstrate the desired output format; examples that distinguish acceptable patterns from genuine issues, to reduce false positives while enabling generalisation; and examples for consistent handling of varied document structures. *What the current guidance says (checked 2026-10-03):* "A few well-crafted examples ... improve accuracy and consistency", and they should be relevant, diverse and structured: "Include 3-5 examples for best results." Examples go in `<example>` tags, several in `<examples>`. The count differs from the guide's (two to four against three to five); both say "a few", and neither says more is better. On the exam, a count is a sanity check and the stronger signals are targeted, with reasons, and including an acceptable pattern; the practice accepts two to four.

## Why it matters

The criteria say to skip minor style and report a null dereference, and the reviewer still flags a guard clause that is perfectly safe, because it looks like the pattern "unchecked value" at a glance. Rules do not settle a case that looks like two rules at once; a pair of examples does. One shows the unsafe access and why it is reported; the other shows the guarded access and why it is skipped. Scenario S5 asks what to add to a prompt that is accurate on clear cases and noisy on borderline ones, and the answer is a few examples placed on the border.

## The idea

### Examples on the border

Few-shot examples are not a sample of typical cases; typical cases are already handled by the criteria. They are chosen where the reviewer is unsure, which are the cases a human reviewer also hesitates over. Three properties make an example work:

1. **Targeted.** It shows a case that the criteria alone do not settle, such as an access that looks unsafe and is guarded.
2. **Both verdicts.** At least one example is reported and at least one is skipped. A set whose every example is a finding teaches the reviewer to report everything; the acceptable pattern is the example that cuts false positives while still letting the reviewer generalise to new code.
3. **With a reason.** Each example says why the verdict was chosen over the plausible alternative. The reason is what transfers to a case that matches no example.

They should also mirror the real output: show the format the answer must have, since examples are the surest way to steer format as well as judgement. And they should be diverse enough that the reviewer does not learn an accident: five examples that all end with the same label teach the label (module 24 shows the same failure).

### Where examples go

Examples sit between the criteria and the input, each in its own tag, with the diff last. Structure helps the model tell instructions from examples from data: `<criteria>`, `<examples>` with one `<example>` per case, then `<diff>`. The practice asks for the order criteria, examples, diff last, and its tests check it; that order is the course's design, which keeps the criteria and the examples together. The prompting guidance for very long inputs is the reverse, with the long material near the top and the question after it, so for a large diff end the prompt with a one-line restatement of the task.

### Ask, or state the assumption

A request is sometimes incomplete: no branch named, no reviewer. What a run does depends on whether anyone can answer. In an attended session (a developer at the keyboard) the right move is to ask only what cannot be assumed, and to state the assumptions made for the rest ("assuming `main`"). In an unattended run (CI) nobody can answer, so it never asks: it proceeds on stated assumptions where a default exists, and stops with a clear failure where a required field has none. Asking in CI produces a job that waits for a person who is not there. The example for this is the practice's `next_step`: given the request, the required fields, the defaults and whether the run is attended, it returns `proceed`, `ask` or `stop`, with the assumptions listed.

### The example

<!-- example: m61-criteria-lint tabs: python,typescript,java,kotlin -->
```python
"""Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.

The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
"""
import logging

log = logging.getLogger(__name__)

VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment")


def lint_criterion(criterion):
    """Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example."""
    found = []
    for key in ("report", "skip"):
        text = criterion.get(key) or ""
        if not text.strip():
            found.append(f"no-{key}")
        elif any(phrase in text.lower() for phrase in VAGUE):
            found.append(f"vague-{key}")
    for level in ("high", "low"):
        if "`" not in (criterion.get("severity") or {}).get(level, ""):
            found.append(f"no-{level}-example")
    return found


def lint_examples(examples):
    """Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why."""
    found = []
    if not 2 <= len(examples) <= 4:
        found.append("two-to-four")
    if {e["verdict"] for e in examples} != {"report", "skip"}:
        found.append("both-verdicts")
    if any(not e.get("reason") for e in examples):
        found.append("reason-missing")
    return found


def trust(verdicts, min_reviewed=5, min_precision=0.5):
    """Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved."""
    table = {}
    for category, verdict in verdicts:
        row = table.setdefault(category, {"reviewed": 0, "accepted": 0})
        row["reviewed"] += 1
        row["accepted"] += 1 if verdict == "accepted" else 0
    for row in table.values():
        row["precision"] = round(row["accepted"] / row["reviewed"], 2)
        row["off"] = row["reviewed"] >= min_reviewed and row["precision"] < min_precision
    return table


VAGUE_CRITERION = {"report": "Be conservative and only flag important problems.", "skip": "", "severity": {"high": "Something serious.", "low": "A small thing."}}
GOOD_CRITERION = {"report": "A comment whose claimed behaviour contradicts the code.", "skip": "Minor style and patterns the codebase already uses.",
                  "severity": {"high": "A null dereference such as `user.profile.name` when `user` may be None.", "low": "A misleading name such as `total` for a count."}}


def main():
    print("vague criterion:", lint_criterion(VAGUE_CRITERION))
    print("good criterion:", lint_criterion(GOOD_CRITERION))
    one_side = [{"verdict": "report", "reason": "The comment says sum, the code multiplies."}, {"verdict": "report", "reason": ""}, {"verdict": "report", "reason": "Unchecked None."}, {"verdict": "report", "reason": "Off by one."}, {"verdict": "report", "reason": "Wrong key."}]
    print("five reports, one without a reason:", lint_examples(one_side))
    print("a report and a skip:", lint_examples([{"verdict": "report", "reason": "r"}, {"verdict": "skip", "reason": "s"}]))
    verdicts = [("bug", "accepted")] * 9 + [("bug", "dismissed")] * 1 + [("style", "accepted")] * 2 + [("style", "dismissed")] * 6 + [("naming", "dismissed")] * 3
    for category, row in trust(verdicts).items():
        print(f"{category}: reviewed {row['reviewed']}, precision {row['precision']}, switch off: {row['off']}")


if __name__ == "__main__":
    main()
```
```text
vague criterion: ['vague-report', 'no-skip', 'no-high-example', 'no-low-example']
good criterion: []
five reports, one without a reason: ['two-to-four', 'both-verdicts', 'reason-missing']
a report and a skip: []
bug: reviewed 10, precision 0.9, switch off: False
style: reviewed 8, precision 0.25, switch off: True
naming: reviewed 3, precision 0.0, switch off: False
```
```typescript
import { logger } from "./logger.ts";
const log = logger("criteria_lint");
/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
export const VAGUE = ["be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment"];
export type Criterion = { report?: string; skip?: string; severity?: { high?: string; low?: string } };
export type Example = { verdict: string; reason?: string };

/** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
export function lintCriterion(criterion: Criterion): string[] {
  const found: string[] = [];
  for (const key of ["report", "skip"] as const) {
    const text = criterion[key] ?? "";
    if (text.trim() === "") found.push(`no-${key}`);
    else if (VAGUE.some((phrase) => text.toLowerCase().includes(phrase))) found.push(`vague-${key}`);
  }
  for (const level of ["high", "low"] as const) if (!(criterion.severity?.[level] ?? "").includes("`")) found.push(`no-${level}-example`);
  return found;
}

/** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
export function lintExamples(examples: Example[]): string[] {
  const found: string[] = [];
  if (examples.length < 2 || examples.length > 4) found.push("two-to-four");
  if ([...new Set(examples.map((e) => e.verdict))].sort().join() !== "report,skip") found.push("both-verdicts");
  if (examples.some((e) => !e.reason)) found.push("reason-missing");
  return found;
}

export type Row = { reviewed: number; accepted: number; precision: number; off: boolean };

/** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
export function trust(verdicts: Array<[string, string]>, minReviewed = 5, minPrecision = 0.5): Record<string, Row> {
  const table: Record<string, Row> = {};
  for (const [category, verdict] of verdicts) {
    const row = (table[category] ??= { reviewed: 0, accepted: 0, precision: 0, off: false });
    row.reviewed += 1;
    row.accepted += verdict === "accepted" ? 1 : 0;
  }
  for (const row of Object.values(table)) {
    row.precision = Math.round((row.accepted / row.reviewed) * 100) / 100;
    row.off = row.reviewed >= minReviewed && row.precision < minPrecision;
  }
  return table;
}

export const VAGUE_CRITERION: Criterion = { report: "Be conservative and only flag important problems.", skip: "", severity: { high: "Something serious.", low: "A small thing." } };
export const GOOD_CRITERION: Criterion = { report: "A comment whose claimed behaviour contradicts the code.", skip: "Minor style and patterns the codebase already uses.",
  severity: { high: "A null dereference such as `user.profile.name` when `user` may be None.", low: "A misleading name such as `total` for a count." } };

function main() {
  console.log("vague criterion:", JSON.stringify(lintCriterion(VAGUE_CRITERION)));
  console.log("good criterion:", JSON.stringify(lintCriterion(GOOD_CRITERION)));
  const oneSide = [{ verdict: "report", reason: "The comment says sum, the code multiplies." }, { verdict: "report", reason: "" }, { verdict: "report", reason: "Unchecked None." }, { verdict: "report", reason: "Off by one." }, { verdict: "report", reason: "Wrong key." }];
  console.log("five reports, one without a reason:", JSON.stringify(lintExamples(oneSide)));
  console.log("a report and a skip:", JSON.stringify(lintExamples([{ verdict: "report", reason: "r" }, { verdict: "skip", reason: "s" }])));
  const verdicts: Array<[string, string]> = [...Array(9).fill(["bug", "accepted"]), ["bug", "dismissed"], ...Array(2).fill(["style", "accepted"]), ...Array(6).fill(["style", "dismissed"]), ...Array(3).fill(["naming", "dismissed"])];
  for (const [category, row] of Object.entries(trust(verdicts))) console.log(`${category}: reviewed ${row.reviewed}, precision ${row.precision}, switch off: ${row.off}`);
}

if (import.meta.main) main();
```
```text
vague criterion: ["vague-report","no-skip","no-high-example","no-low-example"]
good criterion: []
five reports, one without a reason: ["two-to-four","both-verdicts","reason-missing"]
a report and a skip: []
bug: reviewed 10, precision 0.9, switch off: false
style: reviewed 8, precision 0.25, switch off: true
naming: reviewed 3, precision 0, switch off: false
```
```java
import static harness.Show.py;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * <p>The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
public final class CriteriaLint {
    private static final System.Logger LOG = System.getLogger(CriteriaLint.class.getName());
    static final List<String> VAGUE = List.of("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment");

    /** A review criterion: what to report, what to skip, and a concrete example for each severity level. */
    record Criterion(String report, String skip, Map<String, String> severity) {
        Criterion withSkip(String other) {
            return new Criterion(report, other, severity);
        }
    }

    /** One few-shot example: whether the reviewer reports or skips the code shown, and why. */
    record Example(String verdict, String reason) {}

    /** What the verdicts say about one category. */
    record Row(int reviewed, int accepted, double precision, boolean off) {}

    /** One verdict a developer gave to a finding of a category. */
    record Verdict(String category, String verdict) {}

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
    static List<String> lintCriterion(Criterion criterion) {
        List<String> found = new ArrayList<>();
        for (String key : List.of("report", "skip")) {
            String text = orEmpty(key.equals("report") ? criterion.report() : criterion.skip());
            if (text.isBlank()) found.add("no-" + key);
            else if (VAGUE.stream().anyMatch(text.toLowerCase()::contains)) found.add("vague-" + key);
        }
        for (String level : List.of("high", "low")) {
            if (!(criterion.severity() == null ? "" : criterion.severity().getOrDefault(level, "")).contains("`")) found.add("no-" + level + "-example");
        }
        return found;
    }

    /** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
    static List<String> lintExamples(List<Example> examples) {
        List<String> found = new ArrayList<>();
        if (examples.size() < 2 || examples.size() > 4) found.add("two-to-four");
        Set<String> verdicts = new HashSet<>();
        examples.forEach(e -> verdicts.add(e.verdict()));
        if (!verdicts.equals(Set.of("report", "skip"))) found.add("both-verdicts");
        if (examples.stream().anyMatch(e -> orEmpty(e.reason()).isEmpty())) found.add("reason-missing");
        return found;
    }

    /** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
    static Map<String, Row> trust(List<Verdict> verdicts, int minReviewed, double minPrecision) {
        Map<String, int[]> counts = new LinkedHashMap<>();
        for (Verdict v : verdicts) {
            int[] row = counts.computeIfAbsent(v.category(), k -> new int[2]);
            row[0]++;
            if (v.verdict().equals("accepted")) row[1]++;
        }
        Map<String, Row> table = new LinkedHashMap<>();
        counts.forEach((category, c) -> {
            double precision = Math.round((double) c[1] / c[0] * 100) / 100.0;
            table.put(category, new Row(c[0], c[1], precision, c[0] >= minReviewed && precision < minPrecision));
        });
        return table;
    }

    static Map<String, Row> trust(List<Verdict> verdicts) {
        return trust(verdicts, 5, 0.5);
    }

    static final Criterion VAGUE_CRITERION = new Criterion("Be conservative and only flag important problems.", "", Map.of("high", "Something serious.", "low", "A small thing."));
    static final Criterion GOOD_CRITERION = new Criterion("A comment whose claimed behaviour contradicts the code.", "Minor style and patterns the codebase already uses.",
        Map.of("high", "A null dereference such as `user.profile.name` when `user` may be None.", "low", "A misleading name such as `total` for a count."));

    static List<Verdict> repeat(String category, String verdict, int times) {
        List<Verdict> out = new ArrayList<>();
        for (int i = 0; i < times; i++) out.add(new Verdict(category, verdict));
        return out;
    }

    public static void main(String[] args) {
        System.out.println("vague criterion: " + py(lintCriterion(VAGUE_CRITERION)));
        System.out.println("good criterion: " + py(lintCriterion(GOOD_CRITERION)));
        List<Example> oneSide = List.of(new Example("report", "The comment says sum, the code multiplies."), new Example("report", ""), new Example("report", "Unchecked None."),
            new Example("report", "Off by one."), new Example("report", "Wrong key."));
        System.out.println("five reports, one without a reason: " + py(lintExamples(oneSide)));
        System.out.println("a report and a skip: " + py(lintExamples(List.of(new Example("report", "r"), new Example("skip", "s")))));
        List<Verdict> verdicts = new ArrayList<>();
        verdicts.addAll(repeat("bug", "accepted", 9));
        verdicts.addAll(repeat("bug", "dismissed", 1));
        verdicts.addAll(repeat("style", "accepted", 2));
        verdicts.addAll(repeat("style", "dismissed", 6));
        verdicts.addAll(repeat("naming", "dismissed", 3));
        trust(verdicts).forEach((category, row) ->
            System.out.println(category + ": reviewed " + row.reviewed() + ", precision " + row.precision() + ", switch off: " + py(row.off())));
    }
}
```
```text
vague criterion: ['vague-report', 'no-skip', 'no-high-example', 'no-low-example']
good criterion: []
five reports, one without a reason: ['two-to-four', 'both-verdicts', 'reason-missing']
a report and a skip: []
bug: reviewed 10, precision 0.9, switch off: False
style: reviewed 8, precision 0.25, switch off: True
naming: reviewed 3, precision 0.0, switch off: False
```
```kotlin
import harness.Show.py

private val log = System.getLogger("criteria_lint")

/**
 * Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.
 *
 * The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
 * off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
 */
val VAGUE = listOf("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment")

/** A review criterion: what to report, what to skip, and a concrete example for each severity level. */
data class Criterion(val report: String?, val skip: String?, val severity: Map<String, String>?)

/** One few-shot example: whether the reviewer reports or skips the code shown, and why. */
data class Example(val verdict: String, val reason: String? = null)

/** What the verdicts say about one category. */
data class Row(val reviewed: Int, val accepted: Int, val precision: Double, val off: Boolean)

/** Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example. */
fun lintCriterion(criterion: Criterion): List<String> {
    val found = mutableListOf<String>()
    for ((key, text) in listOf("report" to (criterion.report ?: ""), "skip" to (criterion.skip ?: ""))) {
        if (text.isBlank()) found += "no-$key" else if (VAGUE.any { it in text.lowercase() }) found += "vague-$key"
    }
    for (level in listOf("high", "low")) if ('`' !in (criterion.severity?.get(level) ?: "")) found += "no-$level-example"
    return found
}

/** Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why. */
fun lintExamples(examples: List<Example>): List<String> {
    val found = mutableListOf<String>()
    if (examples.size !in 2..4) found += "two-to-four"
    if (examples.map { it.verdict }.toSet() != setOf("report", "skip")) found += "both-verdicts"
    if (examples.any { it.reason.isNullOrEmpty() }) found += "reason-missing"
    return found
}

/** Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved. */
fun trust(verdicts: List<Pair<String, String>>, minReviewed: Int = 5, minPrecision: Double = 0.5): Map<String, Row> {
    val table = linkedMapOf<String, Row>()
    for ((category, verdict) in verdicts.groupBy({ it.first }, { it.second })) {
        val reviewed = verdict.size
        val accepted = verdict.count { it == "accepted" }
        val precision = Math.round(accepted.toDouble() / reviewed * 100) / 100.0
        table[category] = Row(reviewed, accepted, precision, reviewed >= minReviewed && precision < minPrecision)
    }
    return table
}

val VAGUE_CRITERION = Criterion("Be conservative and only flag important problems.", "", mapOf("high" to "Something serious.", "low" to "A small thing."))
val GOOD_CRITERION = Criterion(
    "A comment whose claimed behaviour contradicts the code.", "Minor style and patterns the codebase already uses.",
    mapOf("high" to "A null dereference such as `user.profile.name` when `user` may be None.", "low" to "A misleading name such as `total` for a count."),
)

fun repeat(category: String, verdict: String, times: Int): List<Pair<String, String>> = List(times) { category to verdict }

fun main() {
    println("vague criterion: ${py(lintCriterion(VAGUE_CRITERION))}")
    println("good criterion: ${py(lintCriterion(GOOD_CRITERION))}")
    val oneSide = listOf(
        Example("report", "The comment says sum, the code multiplies."), Example("report", ""), Example("report", "Unchecked None."), Example("report", "Off by one."), Example("report", "Wrong key."),
    )
    println("five reports, one without a reason: ${py(lintExamples(oneSide))}")
    println("a report and a skip: ${py(lintExamples(listOf(Example("report", "r"), Example("skip", "s"))))}")
    val verdicts = repeat("bug", "accepted", 9) + repeat("bug", "dismissed", 1) + repeat("style", "accepted", 2) + repeat("style", "dismissed", 6) + repeat("naming", "dismissed", 3)
    for ((category, row) in trust(verdicts)) println("$category: reviewed ${row.reviewed}, precision ${row.precision}, switch off: ${py(row.off)}")
}
```
```text
vague criterion: ['vague-report', 'no-skip', 'no-high-example', 'no-low-example']
good criterion: []
five reports, one without a reason: ['two-to-four', 'both-verdicts', 'reason-missing']
a report and a skip: []
bug: reviewed 10, precision 0.9, switch off: False
style: reviewed 8, precision 0.25, switch off: True
naming: reviewed 3, precision 0.0, switch off: False
```
<!-- /example -->

### The practice: a review specification that cuts false positives

The practice is in [`exercises/61-criteria-and-examples`](../../exercises/61-criteria-and-examples/unit-01/practice-1/statement.md). You write the function that builds the review prompt from a specification (refusing vague or incomplete criteria and a bad example set, and putting criteria, examples and the diff in that order), the report that computes each category's precision and the most dismissed patterns, and the function that decides whether an incomplete request is asked about, assumed or stopped. It is graded in Python, TypeScript, Java and Kotlin, and the statement lists eight cases, each saying what you should see when it works.

## Traps

1. **"Give the reviewer as many examples as possible."** It is tempting because more data should help. The exam rejects it: the guide asks for two to four targeted examples, and a long list teaches accidents. A few examples that settle the ambiguous cases do more.
2. **"Show only the issues to report."** It is tempting because those are the cases the reviewer must catch. The exam rejects it: without an acceptable pattern the reviewer reports every look-alike, and the false positives remain.
3. **"Pick examples that are typical of the codebase."** It is tempting because typical cases are the common ones. The exam rejects it: typical cases are settled by the criteria; examples earn their place on the ambiguous ones.
4. **"In CI, ask the author which branch to review."** It is tempting because asking is safer than guessing. The exam rejects it: an unattended run has nobody to answer, so it states the assumption or stops.

## Quiz

1. A review prompt handles clear bugs well but keeps marking a null access that is already protected as risky. What should be added?
   - **a**: Four flagged samples of obvious defects, each explained
   - **b**: A line asking the reviewer to take more care over nulls
   - **c**: One flagged sample and one skipped sample, each explained
   - **d**: A few typical samples taken from recent changes in the codebase

2. In this module's practice, a review job in CI receives a request with no reviewer named, although the review requires one, and the project holds no default reviewer. What should the run do?
   - **a**: Ask the author who the reviewer should be, and wait
   - **b**: Stop with a plain failure that lists the absent field
   - **c**: Name the author as reviewer and state that assumption
   - **d**: Carry on without a reviewer and omit it from the output

<details>
<summary>Answer key</summary>

1. **c**. An acceptable pattern shown next to a finding, with reasons, settles the borderline case. *a* is ruled out because "A set whose every example is a finding teaches the reviewer to report everything", including the guarded look-alike. *b* is ruled out because "Rules do not settle a case that looks like two rules at once", and an instruction to be careful names no pattern. *d* is ruled out because "Few-shot examples are not a sample of typical cases", which the criteria already handle.
2. **b**. An unattended run cannot ask, and a required field with no default cannot be assumed. *a* is ruled out because "Asking in CI produces a job that waits for a person who is not there". *c* is ruled out because an assumption is allowed only where the project supplies a default: "it proceeds on stated assumptions where a default exists". *d* is ruled out because a run with nothing to assume "stops with a clear failure where a required field has none", and dropping the field hides the problem.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests. The labels for how bad a finding is are free text, and the gate cannot compare them with its floor. Which change fixes it?
   - **a**: Ask for stronger adjectives on the most urgent findings
   - **b**: Replace each label with a confidence score for the gate
   - **c**: Use a closed list of levels, each with a worked sample
   - **d**: Drop the floor and let every label through as written

2. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests. One group of findings has 45 percent precision. Its dismissals all trace to line-length remarks, while its other remarks are always accepted. What fits?
   - **a**: Disable the whole group until every remark in it improves
   - **b**: Rework that one pattern's criterion and keep the rest
   - **c**: Delete the group's criteria and rewrite them from scratch
   - **d**: Leave the group unchanged while more reviews accumulate

3. Scenario S5, Claude Code for continuous integration. A team runs Claude Code in CI to review pull requests. Its prompt holds three worked cases, one reported unchecked access and two skipped guarded ones, each made of the code and its verdict. A new kind of guard that resembles none of them is still flagged. What should change?
   - **a**: Add skipped examples for each of the newest guard styles
   - **b**: Wrap each example in its own tag inside an examples block
   - **c**: Give each example a line on why its label was chosen
   - **d**: Move the examples after the diff, nearest the input

<details>
<summary>Answer key</summary>

1. **c**. A level with a code sample has a boundary, and a closed list can be compared with a floor. *a* is ruled out because "A definition with a code example gives the reviewer a case to compare with", and adjectives give none. *b* is ruled out because a number the model gives about its own certainty says nothing about how bad a finding is, and "self-reported confidence is another vague instruction". *d* is ruled out because the gate "drops findings below a floor and fails the job at a severity", and without a floor it has no policy.
2. **b**. The recorded pattern shows that one pattern draws every dismissal, so only its criterion changes and the remarks developers accept keep coming. *a* is ruled out because "The fix is then to adjust the criteria for the one pattern and not to disable the category". *c* is ruled out because "Deleting the category from the criteria loses the examples that the next attempt needs". *d* is ruled out because leaving the group unchanged keeps the noise, which has a cost, since "each dismissed comment lowers trust in the accepted ones".
3. **c**. The set is targeted and shows both verdicts, so what it lacks is the reason, and "The reason is what transfers to a case that matches no example". *a* is ruled out because new guard styles keep coming, the set would grow past four, and "the guide asks for two to four targeted examples, and a long list teaches accidents". *b* is ruled out because tags only help the model "tell instructions from examples from data", and a tagged example still says nothing about a guard it does not show. *d* is ruled out because moving the examples changes no verdict they teach, and "Examples sit between the criteria and the input, each in its own tag, with the diff last".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
