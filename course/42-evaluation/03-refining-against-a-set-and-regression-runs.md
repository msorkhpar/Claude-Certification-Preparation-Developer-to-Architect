# Refining against an eval set, regression runs and the practice

**Level:** Developer · **Module 42:** Evaluation · **Page 3 of 3**
**Exams:** DV8; A4.4

**After this page you can** improve a prompt with a loop that an eval set judges, tell a better average from a safe change, run a regression comparison that names what broke, keep a model or setting change under the same gate, and write the module's practice, an eval harness.

Checked on 2026-10-03 against the Claude API documentation pages "Define success criteria and build evaluations", "Prompt engineering overview" and "Claude API errors", and against the pinned model table in the course's version notes. The example runs offline in Python, TypeScript, Java and Kotlin with scripted stand-ins for the application and no model. The practice runs offline in Python, TypeScript, Java and Kotlin.

## Why it matters

Prompt refinement without an eval is a walk in the dark: each edit fixes the case you were looking at and may break three you were not. The exam's evaluation questions describe a team that changed something and asks what it should have done before shipping: run the set, compare per case, look at every dimension, and keep the comparison after the next model release. This page is that routine, and the practice builds its machinery.

## The idea

### The refinement loop

The loop has five steps, and the order is what makes it work.

1. **Baseline.** Run the whole set on the current prompt and keep the report. Every later number is judged against it.
2. **One change.** Edit one thing: an instruction, an example, a field name. Two edits at once leave you unable to say which one helped.
3. **Run the whole set.** Not the case you were fixing. The edit may have changed any of them.
4. **Compare per case.** The average says whether the change was good on balance. The per-case diff says what it cost.
5. **Keep or revert.** Keep the change only if every success dimension still holds and nothing regressed without a reason you accept. Then the new report is the baseline.

The prompt engineering overview supplies the guard on step 1: the techniques assume "Some ways to empirically test against those criteria". The same page supplies the guard on the whole loop: "Not every success criteria or failing eval is best solved by prompt engineering." If the failing dimension is latency or price, the cure may be a different model, a smaller output or a cache, and the loop's report is how you notice that.

### A run, a gate and a comparison

The example below does the five steps on six sentiment cases. It uses two scripted "versions" of a prompt, which are lookup tables standing in for the application, so the program is exact and needs no model. Code grading is by exact label (the example's grader trims and lower-cases only, a simpler rule than the practice's `exact` check, which also collapses runs of white space), the criteria are a pass rate of at least 0.8 overall and at least 0.75 among the cases tagged `edge`, and the comparison names regressions and fixes. The source of all four languages is shown, and under each is what it printed in the container.

<!-- example: m42-eval-run tabs: python,typescript,java,kotlin -->
```python
"""An eval run, a success gate and a regression comparison, on a scripted classifier.

The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
"""
import logging

log = logging.getLogger(__name__)

CASES = [
    {"id": "pos-1", "input": "Love it, works great", "expect": "positive", "tags": ["core"]},
    {"id": "neg-1", "input": "Broke after two days", "expect": "negative", "tags": ["core"]},
    {"id": "neu-1", "input": "It arrived on Tuesday", "expect": "neutral", "tags": ["core"]},
    {"id": "sarcasm-1", "input": "Oh great, another crash", "expect": "negative", "tags": ["edge"]},
    {"id": "mixed-1", "input": "Fast shipping but the screen is dim", "expect": "neutral", "tags": ["edge"]},
    {"id": "empty-1", "input": "", "expect": "neutral", "tags": ["edge"]},
]

PROMPT_V1 = {"Love it, works great": "positive", "Broke after two days": "negative", "It arrived on Tuesday": "neutral",
             "Oh great, another crash": "positive", "Fast shipping but the screen is dim": "positive", "": "Neutral"}
PROMPT_V2 = {**PROMPT_V1, "Oh great, another crash": "negative", "Fast shipping but the screen is dim": "neutral", "": "positive"}

CRITERIA = {"min_pass_rate": 0.8, "tags": {"edge": 0.75}}


def grade(case, output):
    """Code-graded: the answer must equal the expected label, ignoring case and surrounding white space."""
    return output.strip().lower() == case["expect"]


def run(cases, model):
    results = [{"id": c["id"], "passed": grade(c, model[c["input"]]), "tags": c["tags"]} for c in cases]
    rate = sum(r["passed"] for r in results) / len(results)
    by_tag = {}
    for r in results:
        for tag in r["tags"]:
            row = by_tag.setdefault(tag, [0, 0])
            row[0] += r["passed"]
            row[1] += 1
    return {"results": results, "pass_rate": rate, "by_tag": by_tag}


def gate(report, criteria):
    """Every dimension of the success criteria must hold, not only the average."""
    failures = []
    if report["pass_rate"] < criteria["min_pass_rate"]:
        failures.append("overall")
    for tag, minimum in criteria["tags"].items():
        passed, total = report["by_tag"][tag]
        if passed / total < minimum:
            failures.append(f"tag:{tag}")
    return failures


def compare(baseline, current):
    before = {r["id"]: r["passed"] for r in baseline["results"]}
    regressions = [r["id"] for r in current["results"] if before[r["id"]] and not r["passed"]]
    fixed = [r["id"] for r in current["results"] if not before[r["id"]] and r["passed"]]
    return {"regressions": regressions, "fixed": fixed}


def main():
    v1, v2 = run(CASES, PROMPT_V1), run(CASES, PROMPT_V2)
    for name, report in (("prompt v1", v1), ("prompt v2", v2)):
        edge = report["by_tag"]["edge"]
        print(f"{name}: pass rate {report['pass_rate']:.3f}, edge {edge[0]}/{edge[1]}, gate failures {gate(report, CRITERIA)}")
    diff = compare(v1, v2)
    print("v2 against v1: fixed", diff["fixed"], "regressions", diff["regressions"])
    print("average improved:", v2["pass_rate"] > v1["pass_rate"], "- safe to ship:", not diff["regressions"] and not gate(v2, CRITERIA))


if __name__ == "__main__":
    main()
```
```text
prompt v1: pass rate 0.667, edge 1/3, gate failures ['overall', 'tag:edge']
prompt v2: pass rate 0.833, edge 2/3, gate failures ['tag:edge']
v2 against v1: fixed ['sarcasm-1', 'mixed-1'] regressions ['empty-1']
average improved: True - safe to ship: False
```
```typescript
// An eval run, a success gate and a regression comparison, on a scripted classifier.
//
// The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
// cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
// non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
// better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
import { logger } from "./logger.ts";
const log = logger("eval_run");

type Case = { id: string; input: string; expect: string; tags: string[] };
type Report = { results: { id: string; passed: boolean; tags: string[] }[]; pass_rate: number; by_tag: Record<string, [number, number]> };

export const CASES: Case[] = [
  { id: "pos-1", input: "Love it, works great", expect: "positive", tags: ["core"] },
  { id: "neg-1", input: "Broke after two days", expect: "negative", tags: ["core"] },
  { id: "neu-1", input: "It arrived on Tuesday", expect: "neutral", tags: ["core"] },
  { id: "sarcasm-1", input: "Oh great, another crash", expect: "negative", tags: ["edge"] },
  { id: "mixed-1", input: "Fast shipping but the screen is dim", expect: "neutral", tags: ["edge"] },
  { id: "empty-1", input: "", expect: "neutral", tags: ["edge"] },
];

export const PROMPT_V1: Record<string, string> = {
  "Love it, works great": "positive", "Broke after two days": "negative", "It arrived on Tuesday": "neutral",
  "Oh great, another crash": "positive", "Fast shipping but the screen is dim": "positive", "": "Neutral",
};
export const PROMPT_V2: Record<string, string> = { ...PROMPT_V1, "Oh great, another crash": "negative", "Fast shipping but the screen is dim": "neutral", "": "positive" };

export const CRITERIA = { min_pass_rate: 0.8, tags: { edge: 0.75 } as Record<string, number> };

/** Code-graded: the answer must equal the expected label, ignoring case and surrounding white space. */
export function grade(c: { expect: string }, output: string): boolean {
  return output.trim().toLowerCase() === c.expect;
}

export function run(cases: Case[], model: Record<string, string>): Report {
  const results = cases.map((c) => ({ id: c.id, passed: grade(c, model[c.input]), tags: c.tags }));
  const by_tag: Record<string, [number, number]> = {};
  for (const r of results) {
    for (const tag of r.tags) {
      const row = (by_tag[tag] ??= [0, 0]);
      row[0] += r.passed ? 1 : 0;
      row[1] += 1;
    }
  }
  return { results, pass_rate: results.filter((r) => r.passed).length / results.length, by_tag };
}

/** Every dimension of the success criteria must hold, not only the average. */
export function gate(report: Report, criteria: typeof CRITERIA): string[] {
  const failures: string[] = [];
  if (report.pass_rate < criteria.min_pass_rate) failures.push("overall");
  for (const [tag, minimum] of Object.entries(criteria.tags)) {
    const [passed, total] = report.by_tag[tag];
    if (passed / total < minimum) failures.push(`tag:${tag}`);
  }
  return failures;
}

export function compare(baseline: Report, current: Report) {
  const before = new Map(baseline.results.map((r) => [r.id, r.passed]));
  return {
    regressions: current.results.filter((r) => before.get(r.id) && !r.passed).map((r) => r.id),
    fixed: current.results.filter((r) => !before.get(r.id) && r.passed).map((r) => r.id),
  };
}

const list = (items: string[]) => `[${items.map((i) => `'${i}'`).join(", ")}]`;

function main() {
  const v1 = run(CASES, PROMPT_V1);
  const v2 = run(CASES, PROMPT_V2);
  for (const [name, report] of [["prompt v1", v1], ["prompt v2", v2]] as const) {
    const edge = report.by_tag.edge;
    console.log(`${name}: pass rate ${report.pass_rate.toFixed(3)}, edge ${edge[0]}/${edge[1]}, gate failures ${list(gate(report, CRITERIA))}`);
  }
  const diff = compare(v1, v2);
  console.log("v2 against v1: fixed", list(diff.fixed), "regressions", list(diff.regressions));
  const safe = diff.regressions.length === 0 && gate(v2, CRITERIA).length === 0;
  console.log("average improved:", v2.pass_rate > v1.pass_rate ? "True" : "False", "- safe to ship:", safe ? "True" : "False");
}

if (import.meta.main) main();
```
```text
prompt v1: pass rate 0.667, edge 1/3, gate failures ['overall', 'tag:edge']
prompt v2: pass rate 0.833, edge 2/3, gate failures ['tag:edge']
v2 against v1: fixed ['sarcasm-1', 'mixed-1'] regressions ['empty-1']
average improved: True - safe to ship: False
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An eval run, a success gate and a regression comparison, on a scripted classifier.
 *
 * <p>The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
 * cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
 * non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
 * better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
 */
public final class EvalRun {
    private static final System.Logger LOG = System.getLogger(EvalRun.class.getName());
    record Case(String id, String input, String expect, List<String> tags) {}

    record Result(String id, boolean passed, List<String> tags) {}

    /** pass rate of a run, and per tag {passed, total}. */
    record Report(List<Result> results, double passRate, Map<String, int[]> byTag) {}

    record Criteria(double minPassRate, Map<String, Double> tags) {}

    record Diff(List<String> regressions, List<String> fixed) {}

    static final List<Case> CASES = List.of(
        new Case("pos-1", "Love it, works great", "positive", List.of("core")),
        new Case("neg-1", "Broke after two days", "negative", List.of("core")),
        new Case("neu-1", "It arrived on Tuesday", "neutral", List.of("core")),
        new Case("sarcasm-1", "Oh great, another crash", "negative", List.of("edge")),
        new Case("mixed-1", "Fast shipping but the screen is dim", "neutral", List.of("edge")),
        new Case("empty-1", "", "neutral", List.of("edge")));

    static final Map<String, String> PROMPT_V1 = Map.of(
        "Love it, works great", "positive", "Broke after two days", "negative", "It arrived on Tuesday", "neutral",
        "Oh great, another crash", "positive", "Fast shipping but the screen is dim", "positive", "", "Neutral");
    static final Map<String, String> PROMPT_V2 = Map.of(
        "Love it, works great", "positive", "Broke after two days", "negative", "It arrived on Tuesday", "neutral",
        "Oh great, another crash", "negative", "Fast shipping but the screen is dim", "neutral", "", "positive");

    static final Criteria CRITERIA = new Criteria(0.8, Map.of("edge", 0.75));

    /** Code-graded: the answer must equal the expected label, ignoring case and surrounding white space. */
    static boolean grade(Case c, String output) {
        return output.strip().toLowerCase(Locale.ROOT).equals(c.expect());
    }

    static Report run(List<Case> cases, Map<String, String> model) {
        List<Result> results = new ArrayList<>();
        Map<String, int[]> byTag = new LinkedHashMap<>();
        int passed = 0;
        for (Case c : cases) {
            boolean ok = grade(c, model.get(c.input()));
            results.add(new Result(c.id(), ok, c.tags()));
            if (ok) passed++;
            for (String tag : c.tags()) {
                int[] row = byTag.computeIfAbsent(tag, t -> new int[2]);
                if (ok) row[0]++;
                row[1]++;
            }
        }
        return new Report(results, passed / (double) results.size(), byTag);
    }

    /** Every dimension of the success criteria must hold, not only the average. */
    static List<String> gate(Report report, Criteria criteria) {
        List<String> failures = new ArrayList<>();
        if (report.passRate() < criteria.minPassRate()) failures.add("overall");
        for (Map.Entry<String, Double> tag : criteria.tags().entrySet()) {
            int[] row = report.byTag().get(tag.getKey());
            if (row[0] / (double) row[1] < tag.getValue()) failures.add("tag:" + tag.getKey());
        }
        return failures;
    }

    static Diff compare(Report baseline, Report current) {
        Map<String, Boolean> before = new LinkedHashMap<>();
        for (Result r : baseline.results()) before.put(r.id(), r.passed());
        List<String> regressions = new ArrayList<>();
        List<String> fixed = new ArrayList<>();
        for (Result r : current.results()) {
            if (before.get(r.id()) && !r.passed()) regressions.add(r.id());
            if (!before.get(r.id()) && r.passed()) fixed.add(r.id());
        }
        return new Diff(regressions, fixed);
    }

    private static String py(List<String> names) {
        return names.stream().map(n -> "'" + n + "'").collect(Collectors.joining(", ", "[", "]"));
    }

    private static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) {
        Report v1 = run(CASES, PROMPT_V1);
        Report v2 = run(CASES, PROMPT_V2);
        for (Map.Entry<String, Report> e : List.of(Map.entry("prompt v1", v1), Map.entry("prompt v2", v2))) {
            Report report = e.getValue();
            int[] edge = report.byTag().get("edge");
            System.out.println(String.format(Locale.ROOT, "%s: pass rate %.3f, edge %d/%d, gate failures %s", e.getKey(), report.passRate(), edge[0], edge[1], py(gate(report, CRITERIA))));
        }
        Diff diff = compare(v1, v2);
        System.out.println("v2 against v1: fixed " + py(diff.fixed()) + " regressions " + py(diff.regressions()));
        System.out.println("average improved: " + py(v2.passRate() > v1.passRate()) + " - safe to ship: " + py(diff.regressions().isEmpty() && gate(v2, CRITERIA).isEmpty()));
    }
}
```
```text
prompt v1: pass rate 0.667, edge 1/3, gate failures ['overall', 'tag:edge']
prompt v2: pass rate 0.833, edge 2/3, gate failures ['tag:edge']
v2 against v1: fixed ['sarcasm-1', 'mixed-1'] regressions ['empty-1']
average improved: True - safe to ship: False
```
```kotlin
private val log = System.getLogger("eval_run")

/**
 * An eval run, a success gate and a regression comparison, on a scripted classifier.
 *
 * The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
 * cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
 * non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
 * better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
 */
data class Case(val id: String, val input: String, val expect: String, val tags: List<String>)

data class Result(val id: String, val passed: Boolean, val tags: List<String>)

/** The pass rate of a run, and per tag (passed, total). */
data class Report(val results: List<Result>, val passRate: Double, val byTag: Map<String, Pair<Int, Int>>)

data class Criteria(val minPassRate: Double, val tags: Map<String, Double>)

data class Diff(val regressions: List<String>, val fixed: List<String>)

val CASES = listOf(
    Case("pos-1", "Love it, works great", "positive", listOf("core")),
    Case("neg-1", "Broke after two days", "negative", listOf("core")),
    Case("neu-1", "It arrived on Tuesday", "neutral", listOf("core")),
    Case("sarcasm-1", "Oh great, another crash", "negative", listOf("edge")),
    Case("mixed-1", "Fast shipping but the screen is dim", "neutral", listOf("edge")),
    Case("empty-1", "", "neutral", listOf("edge")),
)

val PROMPT_V1 = mapOf(
    "Love it, works great" to "positive", "Broke after two days" to "negative", "It arrived on Tuesday" to "neutral",
    "Oh great, another crash" to "positive", "Fast shipping but the screen is dim" to "positive", "" to "Neutral",
)
val PROMPT_V2 = PROMPT_V1 + mapOf("Oh great, another crash" to "negative", "Fast shipping but the screen is dim" to "neutral", "" to "positive")

val CRITERIA = Criteria(0.8, mapOf("edge" to 0.75))

/** Code-graded: the answer must equal the expected label, ignoring case and surrounding white space. */
fun grade(case: Case, output: String) = output.trim().lowercase() == case.expect

fun run(cases: List<Case>, model: Map<String, String>): Report {
    val results = cases.map { Result(it.id, grade(it, model.getValue(it.input)), it.tags) }
    val byTag = linkedMapOf<String, Pair<Int, Int>>()
    for (r in results) for (tag in r.tags) {
        val (passed, total) = byTag[tag] ?: (0 to 0)
        byTag[tag] = (passed + if (r.passed) 1 else 0) to (total + 1)
    }
    return Report(results, results.count { it.passed } / results.size.toDouble(), byTag)
}

/** Every dimension of the success criteria must hold, not only the average. */
fun gate(report: Report, criteria: Criteria): List<String> {
    val failures = mutableListOf<String>()
    if (report.passRate < criteria.minPassRate) failures += "overall"
    for ((tag, minimum) in criteria.tags) {
        val (passed, total) = report.byTag.getValue(tag)
        if (passed / total.toDouble() < minimum) failures += "tag:$tag"
    }
    return failures
}

fun compare(baseline: Report, current: Report): Diff {
    val before = baseline.results.associate { it.id to it.passed }
    return Diff(
        regressions = current.results.filter { before.getValue(it.id) && !it.passed }.map { it.id },
        fixed = current.results.filter { !before.getValue(it.id) && it.passed }.map { it.id },
    )
}

private fun py(names: List<String>) = names.joinToString(", ", "[", "]") { "'$it'" }

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    val v1 = run(CASES, PROMPT_V1)
    val v2 = run(CASES, PROMPT_V2)
    for ((name, report) in listOf("prompt v1" to v1, "prompt v2" to v2)) {
        val (passed, total) = report.byTag.getValue("edge")
        println("$name: pass rate ${"%.3f".format(report.passRate)}, edge $passed/$total, gate failures ${py(gate(report, CRITERIA))}")
    }
    val diff = compare(v1, v2)
    println("v2 against v1: fixed ${py(diff.fixed)} regressions ${py(diff.regressions)}")
    println("average improved: ${py(v2.passRate > v1.passRate)} - safe to ship: ${py(diff.regressions.isEmpty() && gate(v2, CRITERIA).isEmpty())}")
}
```
```text
prompt v1: pass rate 0.667, edge 1/3, gate failures ['overall', 'tag:edge']
prompt v2: pass rate 0.833, edge 2/3, gate failures ['tag:edge']
v2 against v1: fixed ['sarcasm-1', 'mixed-1'] regressions ['empty-1']
average improved: True - safe to ship: False
```
<!-- /example -->

Read the output as a reviewer would. Prompt v2 raises the pass rate from 0.667 to 0.833: the average improved. It fixed two edge cases, the sarcastic message and the mixed review. It also broke one, the empty message, which it now labels `positive` where v1 said `neutral`. The edge dimension is at 2 of 3, below its 0.75 minimum, so the gate still fails, and the comparison names `empty-1` as a regression. "Safe to ship" is false, and the reason is on the screen. A team that watched only the average would have shipped a prompt that guesses a sentiment for an empty input.

Three rules follow from that run.

- **An average never excuses a regression.** If a case passed before and fails now, a person looks at it. Sometimes the old pass was an accident and the team accepts the change; the point is that the decision is made, not discovered.
- **A dimension that was set aside is where regressions hide.** The edge tag has its own threshold because edge cases are few and rare, and a count that is small in the average can matter to a customer.
- **A case that disappears is not a pass.** If a case is deleted from the set between two runs, the harness reports it as removed and fails the comparison, because dropping the case that fails is the easiest way to improve a score.

### Do not tune to the set

The loop rewards changes that make the set pass. If you copy a failing case into the prompt as an example, the case will pass, and the set's score has measured your editing and not the prompt. Two habits keep the number honest. Keep a held-out slice that the loop never touches and run it at the end (page 1). When a failure suggests an instruction, write the instruction in general words, and check it against the slice.

### Non-determinism in the loop

One run is one sample. When a case matters, run it several times and pass it only if every run passes. A case that passes in some runs and fails in others is flaky, and a flaky case is a finding: the instruction it tests is followed most of the time and not always. The criteria can say how many flaky cases are tolerated. With sampling parameters held at their defaults on the newer models (module 18), repetition is the tool you have.

### Regression runs: when they run

A regression run is the same set, run again, with a comparison against the last accepted report. It runs whenever something that could change behaviour changes:

| Change | Why it can regress | Where the run sits |
|---|---|---|
| A prompt edit | The edit fixes one case and breaks another | Before the merge, as a check on the pull request |
| A new model id | Models differ, and a migration changes behaviour | Before the id is changed in configuration |
| A changed setting | Effort, thinking, `max_tokens` and the output format all change results | Before the setting is merged |
| A changed tool or schema | The model reads the description, so a rename changes its choices | Before the merge |
| A scheduled run | The provider may change what an alias points to, and traffic drifts | Nightly, with an alert on a drop |

The pinned model ids the course uses are snapshots, which is what makes a before-and-after comparison fair. The errors page shows that some migrations fail at the request and not in the output: a prefilled assistant message returns a 400 on Claude 4.6 and later models, a forced `tool_choice` returns a 400 on Claude Opus 5.5 and Sonnet 5.5, among others, and `thinking: {"type": "enabled"}` returns a 400 on models from 4.7. A regression run on the new id turns each of those into a failed case the same afternoon, and not a failed request in production. Module 43 reads such failures.

A pull request check is the natural home for the run: module 40 shows a workflow triggered by a repository event, and an eval job is one more job in it. The set lives in the repository beside the prompt, so a change to either shows in the same diff.

### What an eval run costs

A set of a few hundred cases, graded by a model, is a few hundred requests per run. Two documented features cut the price when the run is large and nobody waits: the Message Batches API charges half the standard price for work that can wait up to a day (module 21), and prompt caching makes a long shared prefix, such as a long system prompt, cheaper after the first call (module 20). A nightly regression run is the typical batch job. A quick check on a pull request stays synchronous and small.

### The practice

The practice is the harness. You write, in the language of your choice (Python, TypeScript, Java or Kotlin):

- `grade`, which checks one output against its case with an `exact`, `regex`, `json_field` or `judge` check and returns a pass or fail with a reason, and, for a judge check, the score;
- `run_eval`, which runs every case through the model, repeats each run as asked, lets one failing model call fail only its own case, marks flaky cases, and reports the pass rate and the rate per tag;
- `meets`, which compares a report with success criteria for the overall rate, for each tag and for the number of flaky cases;
- `compare`, which names the regressions, the fixes and the added and removed cases between two reports, and is not ok for a regression or a removed case.

The Java and Kotlin folders give you a small `Json` helper, because those two have no JSON library in the course's offline image. The statement lists every rule and every reason string. The starter fails every test, the reference passes, and each planted wrong solution fails on an assertion. The statement is at `exercises/42-evaluation/unit-01/practice-1/statement.md`.

| Case | What it checks |
|---|---|
| `m1` | A run grades each case with its own check and reports the pass rate |
| `e1` | An exact check ignores case and spacing only |
| `e2` | A JSON field check needs an object, the field and the same type |
| `e3` | A judge check sends the rubric prompt and accepts only a bare score |
| `e4` | A model error fails its own case and the run goes on |
| `e5` | Tags report their own rates and the criteria judge each dimension |
| `e6` | A comparison names regressions, fixes and removed cases |
| `e7` | Repeated runs expose flaky cases |

## Traps

1. **Judging by the average.** A rise in the pass rate can hide a regression on a rare case. Compare per case and gate each dimension.
2. **Changing two things at once.** You cannot say which edit helped, so you cannot keep one and drop the other.
3. **Tuning to the set.** Copying failing cases into the prompt makes the set pass and the score meaningless. Keep a held-out slice.
4. **Running the set only on prompt edits.** A model id, a setting or a tool description changes behaviour too, and a migration can fail the request before it reaches the output.

## Quiz

1. A new prompt raises the overall pass rate from 0.78 to 0.84. The per-item comparison shows that one input which passed before now fails. What should the team do?
   - **a**: Review that regression, then accept or repair it
   - **b**: Ship it, because the pass rate rose
   - **c**: Delete the failing item from the set, since it is noisy
   - **d**: Rerun the set until that input passes

2. A regression run is scheduled for the day a team changes the model id in its configuration. Which failure does that run catch that a prompt-only check would not?
   - **a**: A prefilled assistant turn that the new release rejects at the request
   - **b**: A typo that has crept into one of the example sentences
   - **c**: A grader scale written from one to five instead of zero to five
   - **d**: A label that was copied wrongly from a spreadsheet into the set

3. A team re-grades 800 items with a model every night, and nobody waits for the result. Which option lowers the cost of that work the most?
   - **a**: Remove the edge items because they are rare
   - **b**: Run the work again on every commit instead of once a day
   - **c**: Submit it through the Message Batches API
   - **d**: Grade every item twice and keep the higher score

<details>
<summary>Answer key</summary>

1. **a**. The page says that "An average never excuses a regression": "If a case passed before and fails now, a person looks at it", and then the decision is made. *b* is ruled out because the example's v2 also raised the average and still failed its gate, and the rule for step 5 is to keep a change "only if every success dimension still holds and nothing regressed without a reason you accept". *c* is ruled out because "A case that disappears is not a pass", and dropping the case that fails "is the easiest way to improve a score". *d* is ruled out because a rerun until the input passes treats a signal as noise, while the page says "a flaky case is a finding".
2. **a**. The page says a prefilled assistant message "returns a 400 on Claude 4.6 and later models", and that a regression run on the new id "turns each of those into a failed case the same afternoon". *b* is ruled out because an example sentence typo is a prompt edit, which the table already puts "Before the merge, as a check on the pull request". *c* is ruled out because the grader's scale belongs to the grader, and the new id changes the application's behaviour: "Models differ, and a migration changes behaviour". *d* is ruled out because a wrongly copied label is a data error, and "The set lives in the repository beside the prompt", so a review of the diff finds it.
3. **c**. The page says the Message Batches API "charges half the standard price for work that can wait up to a day", and calls a nightly regression run "the typical batch job". *b* is ruled out because a run on every commit multiplies the requests, and the page says "A quick check on a pull request stays synchronous and small". *a* is ruled out because "The edge tag has its own threshold", since the edge cases are where regressions hide. *d* is ruled out because grading twice doubles the requests, and keeping the higher score contradicts the rule to "pass it only if every run passes".

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A team writes one criterion, "replies should be accurate". Which addition turns it into something an eval can enforce?
   - **a**: A promise to review the replies at the end of each month
   - **b**: A threshold on a named metric, over a fixed set of cases
   - **c**: A longer list of the qualities that a good reply has
   - **d**: A statement that accuracy matters more than speed or cost

2. A ticket-routing prompt is edited, and the team reruns a set in which every case is graded by a script. Which grader fits a case whose output must parse as a JSON object holding a `queue` key?
   - **a**: A model grader that rates the whole output from one to five
   - **b**: A similarity score against a reference output of the same kind
   - **c**: A structure check that decodes and compares the value
   - **d**: A human who reads every single item that the set holds

3. A prompt change scores better overall, and the team wants to be sure the improvement is real before launch. Which pair of checks gives the most trustworthy answer?
   - **a**: A larger set of the same easy cases, and a stricter threshold
   - **b**: A second run of the same cases, and a review of the average
   - **c**: A review of the edits, and the same cases graded by a person
   - **d**: A comparison per case, and a run on held-out cases

4. A case passes in two of three repeated runs. How should the harness treat it?
   - **a**: As a pass, since most runs passed
   - **b**: As a fail, and as flaky in the report
   - **c**: As a pass, after the failing run is thrown away
   - **d**: As a fail, kept out of the flaky count

<details>
<summary>Answer key</summary>

1. **b**. A good criterion names "a metric, a threshold, a data set, and a baseline to beat", and the four traits ask for "quantitative metrics or well-defined qualitative scales". *a* is ruled out because a promise to review is a process and does not make the criterion measurable: "Nobody can say what number or scale a result would have". *c* is ruled out because a longer list of qualities repeats the problem, since good, accurate and natural are "wishes until they carry a number or a scale and a data set". *d* is ruled out because the page says "Most use cases need multidimensional evaluation along several success criteria", which means a number for each, not one ranking in a sentence.
2. **c**. The page lists a "Structure check" for "A reply that must parse and carry the right fields", and says it "parses the whole output as JSON". *a* is ruled out because a model grader belongs to "A judgement no code can make", and a field comparison is code. *b* is ruled out because similarity answers "Whether two texts say the same thing", and a queue name has an exact right value. *d* is ruled out because the case is graded by a script, and a human belongs in "A sample, to check the model grader".
3. **d**. The page says "Compare per case" and to "Keep a held-out slice that the loop never touches and run it at the end". *b* is ruled out because rerunning the same cases repeats the tuned set, and "The score is then a memory test". *c* is ruled out because a list of edits records effort and a person grading the same cases still scores the cases the prompt was shaped by, and the page says "More questions with slightly lower signal automated grading is better than fewer questions with high-quality human hand-graded evals". *a* is ruled out because "Grading only what is easy to grade" measures a different task, and a threshold on easy cases does not change that.
4. **b**. The page says to "pass it only if every run passes", and that "A case that passes in some runs and fails in others is flaky". *a* is ruled out because a case that fails sometimes will fail for some customers, which is why "a flaky case is a finding". *c* is ruled out because discarding the failing run removes the evidence, and the page says to "pass it only if every run passes". *d* is ruled out because the criteria "can say how many flaky cases are tolerated", which needs the count to include it.

</details>
