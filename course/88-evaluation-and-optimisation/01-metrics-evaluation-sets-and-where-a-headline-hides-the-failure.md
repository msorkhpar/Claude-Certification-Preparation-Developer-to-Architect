# Metrics, evaluation sets and where a headline hides the failure

**Level:** Architect Professional · **Module 88:** Evaluation and optimisation · **Page 1 of 2**
**Exams:** P4

**After this page you can** define the metrics of a Claude system along its several dimensions, build an evaluation set that mirrors real traffic and its awkward cases, combine code grading, a model grader and human review, report accuracy by case type with the cost of each kind of error, and say why two systems with the same headline score are not the same system.

Checked on 2026-10-04 against the Claude documentation page "Define success criteria and build evaluations", Anthropic's article "How we built our multi-agent research system", the Claude Certified Architect - Professional exam guide (version 1.0, domain 4), and by running the example offline in the course container. Nothing here called a model, and the cases, counts and costs in the example are invented. This page deepens module 42 (success criteria, test sets and the kinds of grader) and module 68 (human review and calibrated confidence) to the level of a system that keeps changing. Changing it safely, the diagnosis of a wrong answer, cost and latency, and the practice are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 4 asks the candidate to "Define evaluation metrics (accuracy, latency, cost, safety, security)", to "Design evaluation datasets and test frameworks using mixed methodologies" and to "Monitor system performance using logging and observability tools". Its stated objectives name no tool. *What the current product and its documentation say (pages read 2026-10-04):* the documentation's guidance is that "Most use cases need multidimensional evaluation along several success criteria", that an eval should "mirror your real-world task distribution" and that graders come in three kinds, code, human and model. On the exam, answer with the design: which dimensions, which cases, which grader for which output. The report by segment and its error costs on this page are the course's own teaching device and not a feature of Claude.

## Why it matters

A support assistant has been rewritten and a second version scores 92 percent on the team's evaluation set, the same as the first. The lead proposes to ship it because "nothing got worse". A closer look shows that the new version answers more refund questions wrongly and fewer order-status questions wrongly. The two kinds of error are not equal: a wrong refund answer costs about twenty times what a wrong order-status answer does. The headline figure was the same, the systems were not, and the exam asks what the report should have shown.

## The idea

### Metrics along several dimensions

A single score cannot hold what a system must be. The guide lists five dimensions: accuracy, latency, cost, safety and security. The documentation puts it plainly: "Most use cases need multidimensional evaluation along several success criteria." Each dimension gets a number that can fail a release:

- **Accuracy:** the share of cases answered correctly, by case type.
- **Latency:** a percentile and not a mean, and for streamed answers the time to the first content.
- **Cost:** per answered case, with the tokens that explain it.
- **Safety:** the share of harmful or out-of-policy replies on cases built to provoke them.
- **Security:** the share of injection or leak attempts that got through, on attack cases.

A criterion must be one you can fail. The documentation's own example: "Specific: Clearly define what you want to achieve. Instead of "good performance," specify "accurate sentiment classification."" A target such as "good answers" is not a metric. A target such as 95 percent of refund answers matching the policy, on a fixed set, is.

### An evaluation set that looks like the traffic

The set decides what the score means, so it is built from the traffic and not from the developers' imagination. The documentation's first principle: "Design evals that mirror your real-world task distribution. Don't forget to factor in edge cases!" The examples written into a prompt are not that distribution: they were picked to teach a format, so a score on them says how well the prompt repeats itself. Three habits follow.

- **Sample real usage.** Take cases from logs, with privacy rules applied (page 1 of module 87), and keep their proportions: if half of the traffic asks about order status, half the set does.
- **Add the awkward cases on purpose.** Typos, empty messages, pasted logs, an angry customer, a question outside the product. Tag them, so that the report can show them apart.
- **Prefer volume to polish for the cases that can be graded by a machine.** The documentation: "More questions with slightly lower signal automated grading is better than fewer questions with high-quality human hand-graded evals."

Start before the set is big. Anthropic's research team reports that "A prompt tweak might boost success rates from 30% to 80%. With effect sizes this large, you can spot changes with just a few test cases", and that they began with "about 20 queries representing real usage patterns". The article notes: "We often hear that AI developer teams delay creating evals because they believe that only large evals with hundreds of test cases are useful." Waiting for hundreds of cases delays the feedback that matters most early. The small set grows as the changes get subtler, which is the subject of the second page.

### Mixed methods: one grader per kind of output

No single grader fits every output, and the exam asks which fits which. The documentation lists the three with their trade-offs: code-based grading is "Fastest and most reliable, extremely scalable, but also lacks nuance"; human grading is "Most flexible and high quality, but slow and expensive. Avoid if possible."; model-based grading is "Fast and flexible, scalable and suitable for complex judgment. Test to ensure reliability first then scale."

- **Code grading** for outputs with one correct form: a label, a number, a field of a structure, a command that must be run. Exact match suits answers with one correct form and rejects a correct report that is worded differently.
- **A model grader** for free-form text with a rubric. Anthropic's team graded each research report against five criteria (factual accuracy, citation accuracy, completeness, source quality and tool efficiency) with one grader call and one prompt, and found it "the most consistent and aligned with human judgements". Check that agreement before you trust the grader.
- **People** for what neither catches. "Human evaluation catches what automation misses": the same team's testers found that early agents "consistently chose SEO-optimized content farms over authoritative but less highly-ranked sources". Human review of every output does not scale; a model grader covers hundreds, and people sample for what it misses.

For agents there is one more rule: judge the outcome and not the path. "Even with identical starting points, agents might take completely different valid paths to reach their goal", so an eval that checks for a prescribed sequence of steps fails correct runs. Check that the result is right, then check that the process was reasonable (module 87 traces it).

### Where a headline hides the failure

An average is a statement about the whole set, and a system fails in parts of it. The report that serves an architect is a table by segment: for each type of case, the number of cases, the accuracy, and the cost of its errors. Equal overall scores can hide unequal failures: a headline figure averages away exactly what costs money. More cases make the score steadier and do not show where it is low. A figure from another dimension cannot stand in for the segment that is failing: a faster or cheaper system is not better where it is wrong.

Weighting each error by its cost is how expectations are managed, with a product owner as much as with an engineer. "92 percent right" invites the question of what the other eight percent are. "Refund answers are right 63 times in 100, and each of the wrong ones costs twenty times a status error" answers it, says where the next week of work goes, and shows the budget a safeguard (a human check on refunds) must be sized for. The failure shape belongs in the same table. Name what the wrong answers look like: a refusal, an outdated figure, a made-up clause. Each shape has a different remedy (page 2).

### The example

The example holds a set of 52 invented cases in four segments, run on a current prompt and a new one. Both score the same overall. The table by segment shows that the new prompt repairs refunds and loses answers in policy and complaints, and it prices the difference. It ran offline in every language, and the output is the same in all four.

<!-- example: m88-eval-report tabs: python,typescript,java,kotlin -->
```python
"""Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.

The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)
COSTS = {"order status": 1, "refund": 20, "policy": 5, "complaint": 10}
GROUPS = [("order status", 30), ("refund", 8), ("policy", 10), ("complaint", 4)]


def build_cases():
    """One (segment, old_ok, new_ok) row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both."""
    rows = []
    both = {"order status": 30, "refund": 5, "policy": 8, "complaint": 3}
    old_only = {"order status": 0, "refund": 0, "policy": 1, "complaint": 1}
    new_only = {"order status": 0, "refund": 2, "policy": 0, "complaint": 0}
    for segment, count in GROUPS:
        rows += [(segment, True, True)] * both[segment]
        rows += [(segment, True, False)] * old_only[segment]
        rows += [(segment, False, True)] * new_only[segment]
        rows += [(segment, False, False)] * (count - both[segment] - old_only[segment] - new_only[segment])
    return rows


def pct(part, whole):
    """Whole percent, half up, with integers only so that every language agrees."""
    return (200 * part + whole) // (2 * whole) if whole else 0


def segment_table(rows, which):
    """Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first."""
    out = {}
    for segment, old_ok, new_ok in rows:
        ok = old_ok if which == "old" else new_ok
        n, right = out.get(segment, (0, 0))
        out[segment] = (n + 1, right + (1 if ok else 0))
    table = [(s, n, r, pct(r, n), (n - r) * COSTS.get(s, 1)) for s, (n, r) in out.items()]
    return sorted(table, key=lambda t: (-t[4], t[0]))


def percentile(values, p):
    """Nearest rank: the value at rank ceil(p * n / 100) of the sorted list."""
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def ab_verdict(x1, n1, x2, n2, min_n=200):
    """Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared."""
    if n1 < min_n or n2 < min_n:
        return "too few cases"
    big_n, x = n1 + n2, x1 + x2
    if x == 0 or x == big_n:
        return "no clear difference"
    d = x2 * n1 - x1 * n2
    if d * d * big_n * 10000 < 38416 * n1 * n2 * x * (big_n - x):
        return "no clear difference"
    return "new is better" if d > 0 else "old is better"


def shadow_gate(rows, protected):
    """Ship only when no protected segment lost a right answer and the new version lost fewer than it gained."""
    log.debug("shadow_gate input: %r", rows)
    lost = [s for s, old_ok, new_ok in rows if old_ok and not new_ok]
    gained = [s for s, old_ok, new_ok in rows if new_ok and not old_ok]
    blocked = sorted({s for s in lost if s in protected})
    ship = not blocked and len(lost) <= len(gained)
    return {"decision": "ship" if ship else "hold", "lost": len(lost), "gained": len(gained), "blocked": blocked}


def diagnose(found, supported, format_ok, passes_on_stronger):
    """Where to look first: the evidence, then the grounding, then the format, then the task, and the model last."""
    if not found:
        return "retrieval or data"
    if not supported:
        return "ungrounded answer"
    if not format_ok:
        return "format instructions"
    if not passes_on_stronger:
        return "prompt or task"
    return "model mismatch"


def choose_model(options, min_accuracy, max_p95):
    """The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does."""
    fit = [o for o in options if o[1] >= min_accuracy and o[2] <= max_p95]
    return min(fit, key=lambda o: (o[3], o[0]))[0] if fit else "none"


def main():
    rows = build_cases()
    for which, label in (("old", "current prompt"), ("new", "new prompt")):
        table = segment_table(rows, which)
        right = sum(t[2] for t in table)
        print(f"{label}: {right}/{len(rows)} right, {pct(right, len(rows))}% overall, error cost {sum(t[4] for t in table)}")
        for s, n, r, p, c in table:
            print(f"  {s:<13} {r}/{n} {p}% cost {c}")
    latencies = [800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800]
    print(f"latency ms: mean {sum(latencies) // len(latencies)}, p50 {percentile(latencies, 50)}, p95 {percentile(latencies, 95)}")
    print("live test, 500 cases each, 410 right against 438:", ab_verdict(410, 500, 438, 500))
    print("live test, 500 cases each, 410 right against 425:", ab_verdict(410, 500, 425, 500))
    print("live test, 100 cases each, 82 right against 90:", ab_verdict(82, 100, 90, 100))
    gate = shadow_gate(rows, {"refund", "complaint"})
    print(f"shadow run: {gate['decision']}, lost {gate['lost']}, gained {gate['gained']}, protected segments hit: {', '.join(gate['blocked']) or 'none'}")
    for name, args in (("no chunk had the answer", (False, False, True, True)), ("a claim no chunk supports", (True, False, True, True)), ("a reply in the wrong shape", (True, True, False, True)),
                       ("fails on a stronger model too", (True, True, True, False)), ("passes only on a stronger model", (True, True, True, True))):
        print(f"diagnose, {name}: {diagnose(*args)}")
    options = [("small", 84, 900, 1), ("medium", 91, 1800, 3), ("large", 95, 4200, 9)]
    print(f"model for 90% accuracy within 2000 ms: {choose_model(options, 90, 2000)}; for 94% within 2000 ms: {choose_model(options, 94, 2000)}")


if __name__ == "__main__":
    main()
```
```text
current prompt: 48/52 right, 92% overall, error cost 65
  refund        5/8 63% cost 60
  policy        9/10 90% cost 5
  complaint     4/4 100% cost 0
  order status  30/30 100% cost 0
new prompt: 48/52 right, 92% overall, error cost 40
  refund        7/8 88% cost 20
  complaint     3/4 75% cost 10
  policy        8/10 80% cost 10
  order status  30/30 100% cost 0
latency ms: mean 1441, p50 980, p95 4800
live test, 500 cases each, 410 right against 438: new is better
live test, 500 cases each, 410 right against 425: no clear difference
live test, 100 cases each, 82 right against 90: too few cases
shadow run: hold, lost 2, gained 2, protected segments hit: complaint
diagnose, no chunk had the answer: retrieval or data
diagnose, a claim no chunk supports: ungrounded answer
diagnose, a reply in the wrong shape: format instructions
diagnose, fails on a stronger model too: prompt or task
diagnose, passes only on a stronger model: model mismatch
model for 90% accuracy within 2000 ms: medium; for 94% within 2000 ms: none
```
```typescript
import { logger } from "./logger.ts";
const log = logger("eval_report");
/**
 * Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.
 *
 * The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
 */
export type Row = [segment: string, oldOk: boolean, newOk: boolean];
export type Line = [segment: string, cases: number, right: number, percent: number, cost: number];
export type Option = [name: string, accuracy: number, p95: number, cost: number];

export const COSTS: Record<string, number> = { "order status": 1, refund: 20, policy: 5, complaint: 10 };
const GROUPS: [string, number][] = [["order status", 30], ["refund", 8], ["policy", 10], ["complaint", 4]];

/** One row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both. */
export function buildCases(): Row[] {
  const rows: Row[] = [];
  const both: Record<string, number> = { "order status": 30, refund: 5, policy: 8, complaint: 3 };
  const oldOnly: Record<string, number> = { "order status": 0, refund: 0, policy: 1, complaint: 1 };
  const newOnly: Record<string, number> = { "order status": 0, refund: 2, policy: 0, complaint: 0 };
  for (const [segment, count] of GROUPS) {
    const rest = count - both[segment] - oldOnly[segment] - newOnly[segment];
    for (let i = 0; i < both[segment]; i++) rows.push([segment, true, true]);
    for (let i = 0; i < oldOnly[segment]; i++) rows.push([segment, true, false]);
    for (let i = 0; i < newOnly[segment]; i++) rows.push([segment, false, true]);
    for (let i = 0; i < rest; i++) rows.push([segment, false, false]);
  }
  return rows;
}

/** Whole percent, half up, with integers only so that every language agrees. */
export function pct(part: number, whole: number): number {
  return whole ? Math.floor((200 * part + whole) / (2 * whole)) : 0;
}

/** Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first. */
export function segmentTable(rows: Row[], which: "old" | "new"): Line[] {
  const out = new Map<string, [number, number]>();
  for (const [segment, oldOk, newOk] of rows) {
    const ok = which === "old" ? oldOk : newOk;
    const [n, right] = out.get(segment) ?? [0, 0];
    out.set(segment, [n + 1, right + (ok ? 1 : 0)]);
  }
  const table: Line[] = [...out.entries()].map(([s, [n, r]]) => [s, n, r, pct(r, n), (n - r) * (COSTS[s] ?? 1)]);
  return table.sort((a, b) => b[4] - a[4] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
}

/** Nearest rank: the value at rank ceil(p * n / 100) of the sorted list. */
export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

/** Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared. */
export function abVerdict(x1: number, n1: number, x2: number, n2: number, minN = 200): string {
  if (n1 < minN || n2 < minN) return "too few cases";
  const bigN = n1 + n2;
  const x = x1 + x2;
  if (x === 0 || x === bigN) return "no clear difference";
  const d = BigInt(x2 * n1 - x1 * n2);
  if (d * d * BigInt(bigN) * 10000n < 38416n * BigInt(n1) * BigInt(n2) * BigInt(x) * BigInt(bigN - x)) return "no clear difference";
  return d > 0n ? "new is better" : "old is better";
}

/** Ship only when no protected segment lost a right answer and the new version lost fewer than it gained. */
export function shadowGate(rows: Row[], protectedSegments: Set<string>): { decision: string; lost: number; gained: number; blocked: string[] } {
  log.debug("shadowGate input", rows);
  const lost = rows.filter(([, o, n]) => o && !n).map((r) => r[0]);
  const gained = rows.filter(([, o, n]) => n && !o).length;
  const blocked = [...new Set(lost.filter((s) => protectedSegments.has(s)))].sort();
  const ship = blocked.length === 0 && lost.length <= gained;
  return { decision: ship ? "ship" : "hold", lost: lost.length, gained, blocked };
}

/** Where to look first: the evidence, then the grounding, then the format, then the task, and the model last. */
export function diagnose(found: boolean, supported: boolean, formatOk: boolean, passesOnStronger: boolean): string {
  if (!found) return "retrieval or data";
  if (!supported) return "ungrounded answer";
  if (!formatOk) return "format instructions";
  if (!passesOnStronger) return "prompt or task";
  return "model mismatch";
}

/** The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does. */
export function chooseModel(options: Option[], minAccuracy: number, maxP95: number): string {
  const fit = options.filter((o) => o[1] >= minAccuracy && o[2] <= maxP95);
  fit.sort((a, b) => a[3] - b[3] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0));
  return fit.length ? fit[0][0] : "none";
}

function main(): void {
  const rows = buildCases();
  for (const [which, label] of [["old", "current prompt"], ["new", "new prompt"]] as const) {
    const table = segmentTable(rows, which);
    const right = table.reduce((a, t) => a + t[2], 0);
    console.log(`${label}: ${right}/${rows.length} right, ${pct(right, rows.length)}% overall, error cost ${table.reduce((a, t) => a + t[4], 0)}`);
    for (const [s, n, r, p, c] of table) console.log(`  ${s.padEnd(13)} ${r}/${n} ${p}% cost ${c}`);
  }
  const latencies = [800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800];
  console.log(`latency ms: mean ${Math.floor(latencies.reduce((a, b) => a + b, 0) / latencies.length)}, p50 ${percentile(latencies, 50)}, p95 ${percentile(latencies, 95)}`);
  console.log("live test, 500 cases each, 410 right against 438:", abVerdict(410, 500, 438, 500));
  console.log("live test, 500 cases each, 410 right against 425:", abVerdict(410, 500, 425, 500));
  console.log("live test, 100 cases each, 82 right against 90:", abVerdict(82, 100, 90, 100));
  const gate = shadowGate(rows, new Set(["refund", "complaint"]));
  console.log(`shadow run: ${gate.decision}, lost ${gate.lost}, gained ${gate.gained}, protected segments hit: ${gate.blocked.join(", ") || "none"}`);
  const cases: [string, boolean, boolean, boolean, boolean][] = [["no chunk had the answer", false, false, true, true], ["a claim no chunk supports", true, false, true, true], ["a reply in the wrong shape", true, true, false, true],
    ["fails on a stronger model too", true, true, true, false], ["passes only on a stronger model", true, true, true, true]];
  for (const [name, a, b, c, d] of cases) console.log(`diagnose, ${name}: ${diagnose(a, b, c, d)}`);
  const options: Option[] = [["small", 84, 900, 1], ["medium", 91, 1800, 3], ["large", 95, 4200, 9]];
  console.log(`model for 90% accuracy within 2000 ms: ${chooseModel(options, 90, 2000)}; for 94% within 2000 ms: ${chooseModel(options, 94, 2000)}`);
}

if (import.meta.main) main();
```
```text
current prompt: 48/52 right, 92% overall, error cost 65
  refund        5/8 63% cost 60
  policy        9/10 90% cost 5
  complaint     4/4 100% cost 0
  order status  30/30 100% cost 0
new prompt: 48/52 right, 92% overall, error cost 40
  refund        7/8 88% cost 20
  complaint     3/4 75% cost 10
  policy        8/10 80% cost 10
  order status  30/30 100% cost 0
latency ms: mean 1441, p50 980, p95 4800
live test, 500 cases each, 410 right against 438: new is better
live test, 500 cases each, 410 right against 425: no clear difference
live test, 100 cases each, 82 right against 90: too few cases
shadow run: hold, lost 2, gained 2, protected segments hit: complaint
diagnose, no chunk had the answer: retrieval or data
diagnose, a claim no chunk supports: ungrounded answer
diagnose, a reply in the wrong shape: format instructions
diagnose, fails on a stronger model too: prompt or task
diagnose, passes only on a stronger model: model mismatch
model for 90% accuracy within 2000 ms: medium; for 94% within 2000 ms: none
```
```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.
 *
 * The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
 */
public class EvalReport {
    private static final System.Logger LOG = System.getLogger(EvalReport.class.getName());
    record Row(String segment, boolean oldOk, boolean newOk) {}

    record Line(String segment, int cases, int right, int percent, int cost) {}

    record Option(String name, int accuracy, int p95, int cost) {}

    record Gate(String decision, int lost, int gained, List<String> blocked) {}

    static final Map<String, Integer> COSTS = Map.of("order status", 1, "refund", 20, "policy", 5, "complaint", 10);

    /** One row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both. */
    static List<Row> buildCases() {
        List<Row> rows = new ArrayList<>();
        String[] segments = {"order status", "refund", "policy", "complaint"};
        int[] counts = {30, 8, 10, 4};
        int[] both = {30, 5, 8, 3};
        int[] oldOnly = {0, 0, 1, 1};
        int[] newOnly = {0, 2, 0, 0};
        for (int g = 0; g < segments.length; g++) {
            int rest = counts[g] - both[g] - oldOnly[g] - newOnly[g];
            for (int i = 0; i < both[g]; i++) rows.add(new Row(segments[g], true, true));
            for (int i = 0; i < oldOnly[g]; i++) rows.add(new Row(segments[g], true, false));
            for (int i = 0; i < newOnly[g]; i++) rows.add(new Row(segments[g], false, true));
            for (int i = 0; i < rest; i++) rows.add(new Row(segments[g], false, false));
        }
        return rows;
    }

    /** Whole percent, half up, with integers only so that every language agrees. */
    static int pct(int part, int whole) {
        return whole != 0 ? (200 * part + whole) / (2 * whole) : 0;
    }

    /** Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first. */
    static List<Line> segmentTable(List<Row> rows, String which) {
        Map<String, int[]> out = new LinkedHashMap<>();
        for (Row row : rows) {
            boolean ok = which.equals("old") ? row.oldOk() : row.newOk();
            int[] counts = out.computeIfAbsent(row.segment(), k -> new int[2]);
            counts[0]++;
            if (ok) counts[1]++;
        }
        List<Line> table = new ArrayList<>();
        out.forEach((s, c) -> table.add(new Line(s, c[0], c[1], pct(c[1], c[0]), (c[0] - c[1]) * COSTS.getOrDefault(s, 1))));
        table.sort(Comparator.comparingInt((Line l) -> -l.cost()).thenComparing(Line::segment));
        return table;
    }

    /** Nearest rank: the value at rank ceil(p * n / 100) of the sorted list. */
    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    /** Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared. */
    static String abVerdict(int x1, int n1, int x2, int n2, int minN) {
        if (n1 < minN || n2 < minN) return "too few cases";
        long bigN = n1 + n2;
        long x = x1 + x2;
        if (x == 0 || x == bigN) return "no clear difference";
        long d = (long) x2 * n1 - (long) x1 * n2;
        if (d * d * bigN * 10000L < 38416L * n1 * n2 * x * (bigN - x)) return "no clear difference";
        return d > 0 ? "new is better" : "old is better";
    }

    static String abVerdict(int x1, int n1, int x2, int n2) {
        return abVerdict(x1, n1, x2, n2, 200);
    }

    /** Ship only when no protected segment lost a right answer and the new version lost fewer than it gained. */
    static Gate shadowGate(List<Row> rows, Set<String> protectedSegments) {
        LOG.log(System.Logger.Level.DEBUG, "shadowGate input: {0}", rows);
        int lost = 0;
        int gained = 0;
        TreeSet<String> blocked = new TreeSet<>();
        for (Row row : rows) {
            if (row.oldOk() && !row.newOk()) {
                lost++;
                if (protectedSegments.contains(row.segment())) blocked.add(row.segment());
            }
            if (row.newOk() && !row.oldOk()) gained++;
        }
        boolean ship = blocked.isEmpty() && lost <= gained;
        return new Gate(ship ? "ship" : "hold", lost, gained, new ArrayList<>(blocked));
    }

    /** Where to look first: the evidence, then the grounding, then the format, then the task, and the model last. */
    static String diagnose(boolean found, boolean supported, boolean formatOk, boolean passesOnStronger) {
        if (!found) return "retrieval or data";
        if (!supported) return "ungrounded answer";
        if (!formatOk) return "format instructions";
        if (!passesOnStronger) return "prompt or task";
        return "model mismatch";
    }

    /** The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does. */
    static String chooseModel(List<Option> options, int minAccuracy, int maxP95) {
        return options.stream().filter(o -> o.accuracy() >= minAccuracy && o.p95() <= maxP95)
            .min(Comparator.comparingInt(Option::cost).thenComparing(Option::name)).map(Option::name).orElse("none");
    }

    public static void main(String[] args) {
        List<Row> rows = buildCases();
        for (String[] which : new String[][] {{"old", "current prompt"}, {"new", "new prompt"}}) {
            List<Line> table = segmentTable(rows, which[0]);
            int right = 0;
            int cost = 0;
            for (Line l : table) {
                right += l.right();
                cost += l.cost();
            }
            System.out.println(which[1] + ": " + right + "/" + rows.size() + " right, " + pct(right, rows.size()) + "% overall, error cost " + cost);
            for (Line l : table) System.out.println("  " + String.format("%-13s", l.segment()) + " " + l.right() + "/" + l.cases() + " " + l.percent() + "% cost " + l.cost());
        }
        List<Integer> latencies = List.of(800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800);
        int total = 0;
        for (int v : latencies) total += v;
        System.out.println("latency ms: mean " + total / latencies.size() + ", p50 " + percentile(latencies, 50) + ", p95 " + percentile(latencies, 95));
        System.out.println("live test, 500 cases each, 410 right against 438: " + abVerdict(410, 500, 438, 500));
        System.out.println("live test, 500 cases each, 410 right against 425: " + abVerdict(410, 500, 425, 500));
        System.out.println("live test, 100 cases each, 82 right against 90: " + abVerdict(82, 100, 90, 100));
        Gate gate = shadowGate(rows, Set.of("refund", "complaint"));
        System.out.println("shadow run: " + gate.decision() + ", lost " + gate.lost() + ", gained " + gate.gained() + ", protected segments hit: " + (gate.blocked().isEmpty() ? "none" : String.join(", ", gate.blocked())));
        System.out.println("diagnose, no chunk had the answer: " + diagnose(false, false, true, true));
        System.out.println("diagnose, a claim no chunk supports: " + diagnose(true, false, true, true));
        System.out.println("diagnose, a reply in the wrong shape: " + diagnose(true, true, false, true));
        System.out.println("diagnose, fails on a stronger model too: " + diagnose(true, true, true, false));
        System.out.println("diagnose, passes only on a stronger model: " + diagnose(true, true, true, true));
        List<Option> options = List.of(new Option("small", 84, 900, 1), new Option("medium", 91, 1800, 3), new Option("large", 95, 4200, 9));
        System.out.println("model for 90% accuracy within 2000 ms: " + chooseModel(options, 90, 2000) + "; for 94% within 2000 ms: " + chooseModel(options, 94, 2000));
    }
}
```
```text
current prompt: 48/52 right, 92% overall, error cost 65
  refund        5/8 63% cost 60
  policy        9/10 90% cost 5
  complaint     4/4 100% cost 0
  order status  30/30 100% cost 0
new prompt: 48/52 right, 92% overall, error cost 40
  refund        7/8 88% cost 20
  complaint     3/4 75% cost 10
  policy        8/10 80% cost 10
  order status  30/30 100% cost 0
latency ms: mean 1441, p50 980, p95 4800
live test, 500 cases each, 410 right against 438: new is better
live test, 500 cases each, 410 right against 425: no clear difference
live test, 100 cases each, 82 right against 90: too few cases
shadow run: hold, lost 2, gained 2, protected segments hit: complaint
diagnose, no chunk had the answer: retrieval or data
diagnose, a claim no chunk supports: ungrounded answer
diagnose, a reply in the wrong shape: format instructions
diagnose, fails on a stronger model too: prompt or task
diagnose, passes only on a stronger model: model mismatch
model for 90% accuracy within 2000 ms: medium; for 94% within 2000 ms: none
```
```kotlin
/**
 * Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.
 *
 * The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
 */

private val log = System.getLogger("eval_report")

data class Row(val segment: String, val oldOk: Boolean, val newOk: Boolean)

data class Line(val segment: String, val cases: Int, val right: Int, val percent: Int, val cost: Int)

data class Option(val name: String, val accuracy: Int, val p95: Int, val cost: Int)

data class Gate(val decision: String, val lost: Int, val gained: Int, val blocked: List<String>)

val COSTS = mapOf("order status" to 1, "refund" to 20, "policy" to 5, "complaint" to 10)

/** One row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both. */
fun buildCases(): List<Row> {
    val rows = mutableListOf<Row>()
    val segments = listOf("order status", "refund", "policy", "complaint")
    val counts = listOf(30, 8, 10, 4)
    val both = listOf(30, 5, 8, 3)
    val oldOnly = listOf(0, 0, 1, 1)
    val newOnly = listOf(0, 2, 0, 0)
    for (g in segments.indices) {
        val rest = counts[g] - both[g] - oldOnly[g] - newOnly[g]
        repeat(both[g]) { rows.add(Row(segments[g], true, true)) }
        repeat(oldOnly[g]) { rows.add(Row(segments[g], true, false)) }
        repeat(newOnly[g]) { rows.add(Row(segments[g], false, true)) }
        repeat(rest) { rows.add(Row(segments[g], false, false)) }
    }
    return rows
}

/** Whole percent, half up, with integers only so that every language agrees. */
fun pct(part: Int, whole: Int): Int = if (whole != 0) (200 * part + whole) / (2 * whole) else 0

/** Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first. */
fun segmentTable(rows: List<Row>, which: String): List<Line> {
    val out = linkedMapOf<String, IntArray>()
    for (row in rows) {
        val ok = if (which == "old") row.oldOk else row.newOk
        val counts = out.getOrPut(row.segment) { IntArray(2) }
        counts[0]++
        if (ok) counts[1]++
    }
    val table = out.map { (s, c) -> Line(s, c[0], c[1], pct(c[1], c[0]), (c[0] - c[1]) * (COSTS[s] ?: 1)) }
    return table.sortedWith(compareBy<Line>({ -it.cost }, { it.segment }))
}

/** Nearest rank: the value at rank ceil(p * n / 100) of the sorted list. */
fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

/** Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared. */
fun abVerdict(x1: Int, n1: Int, x2: Int, n2: Int, minN: Int = 200): String {
    if (n1 < minN || n2 < minN) return "too few cases"
    val bigN = (n1 + n2).toLong()
    val x = (x1 + x2).toLong()
    if (x == 0L || x == bigN) return "no clear difference"
    val d = x2.toLong() * n1 - x1.toLong() * n2
    if (d * d * bigN * 10000L < 38416L * n1 * n2 * x * (bigN - x)) return "no clear difference"
    return if (d > 0) "new is better" else "old is better"
}

/** Ship only when no protected segment lost a right answer and the new version lost fewer than it gained. */
fun shadowGate(rows: List<Row>, protectedSegments: Set<String>): Gate {
    log.log(System.Logger.Level.DEBUG, "shadowGate input: {0}", rows)
    val lost = rows.filter { it.oldOk && !it.newOk }.map { it.segment }
    val gained = rows.count { it.newOk && !it.oldOk }
    val blocked = lost.filter { it in protectedSegments }.toSortedSet().toList()
    val ship = blocked.isEmpty() && lost.size <= gained
    return Gate(if (ship) "ship" else "hold", lost.size, gained, blocked)
}

/** Where to look first: the evidence, then the grounding, then the format, then the task, and the model last. */
fun diagnose(found: Boolean, supported: Boolean, formatOk: Boolean, passesOnStronger: Boolean): String {
    if (!found) return "retrieval or data"
    if (!supported) return "ungrounded answer"
    if (!formatOk) return "format instructions"
    if (!passesOnStronger) return "prompt or task"
    return "model mismatch"
}

/** The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does. */
fun chooseModel(options: List<Option>, minAccuracy: Int, maxP95: Int): String =
    options.filter { it.accuracy >= minAccuracy && it.p95 <= maxP95 }.sortedWith(compareBy<Option>({ it.cost }, { it.name })).firstOrNull()?.name ?: "none"

fun main() {
    val rows = buildCases()
    for ((which, label) in listOf("old" to "current prompt", "new" to "new prompt")) {
        val table = segmentTable(rows, which)
        val right = table.map { it.right }.sum()
        println("$label: $right/${rows.size} right, ${pct(right, rows.size)}% overall, error cost ${table.map { it.cost }.sum()}")
        for (l in table) println("  ${l.segment.padEnd(13)} ${l.right}/${l.cases} ${l.percent}% cost ${l.cost}")
    }
    val latencies = listOf(800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800)
    println("latency ms: mean ${latencies.sum() / latencies.size}, p50 ${percentile(latencies, 50)}, p95 ${percentile(latencies, 95)}")
    println("live test, 500 cases each, 410 right against 438: ${abVerdict(410, 500, 438, 500)}")
    println("live test, 500 cases each, 410 right against 425: ${abVerdict(410, 500, 425, 500)}")
    println("live test, 100 cases each, 82 right against 90: ${abVerdict(82, 100, 90, 100)}")
    val gate = shadowGate(rows, setOf("refund", "complaint"))
    println("shadow run: ${gate.decision}, lost ${gate.lost}, gained ${gate.gained}, protected segments hit: ${if (gate.blocked.isEmpty()) "none" else gate.blocked.joinToString(", ")}")
    println("diagnose, no chunk had the answer: ${diagnose(false, false, true, true)}")
    println("diagnose, a claim no chunk supports: ${diagnose(true, false, true, true)}")
    println("diagnose, a reply in the wrong shape: ${diagnose(true, true, false, true)}")
    println("diagnose, fails on a stronger model too: ${diagnose(true, true, true, false)}")
    println("diagnose, passes only on a stronger model: ${diagnose(true, true, true, true)}")
    val options = listOf(Option("small", 84, 900, 1), Option("medium", 91, 1800, 3), Option("large", 95, 4200, 9))
    println("model for 90% accuracy within 2000 ms: ${chooseModel(options, 90, 2000)}; for 94% within 2000 ms: ${chooseModel(options, 94, 2000)}")
}
```
```text
current prompt: 48/52 right, 92% overall, error cost 65
  refund        5/8 63% cost 60
  policy        9/10 90% cost 5
  complaint     4/4 100% cost 0
  order status  30/30 100% cost 0
new prompt: 48/52 right, 92% overall, error cost 40
  refund        7/8 88% cost 20
  complaint     3/4 75% cost 10
  policy        8/10 80% cost 10
  order status  30/30 100% cost 0
latency ms: mean 1441, p50 980, p95 4800
live test, 500 cases each, 410 right against 438: new is better
live test, 500 cases each, 410 right against 425: no clear difference
live test, 100 cases each, 82 right against 90: too few cases
shadow run: hold, lost 2, gained 2, protected segments hit: complaint
diagnose, no chunk had the answer: retrieval or data
diagnose, a claim no chunk supports: ungrounded answer
diagnose, a reply in the wrong shape: format instructions
diagnose, fails on a stronger model too: prompt or task
diagnose, passes only on a stronger model: model mismatch
model for 90% accuracy within 2000 ms: medium; for 94% within 2000 ms: none
```
<!-- /example -->

## Traps

1. **"The two versions score the same, so they are equal."** It is tempting because one number is easy to compare. It fails because the same total can hide opposite changes in different segments, and the segments cost different amounts. Report accuracy by case type with the cost of each error.
2. **"Grade everything by hand; people are the only valid judges."** It is tempting because human judgement is the standard that other graders are tested against. It fails on scale and on cost. Use code grading where the form is fixed, a model grader with a rubric for free text, and people for a sample and the cases the others miss.
3. **"Wait until there is a large evaluation set."** It is tempting because a small set feels unscientific. It fails because the early changes are large, a few real cases show them, and every week without an eval is a week of judging by feel. Start with a small set of real queries and grow it.

## Quiz

1. Two versions of a support assistant both score 92 percent on an evaluation set of refund and order-status questions. A wrong refund answer does far more harm than a wrong status answer. What should the report show?
   - **a**: The accuracy and the error cost of each, split by kind of case
   - **b**: The overall accuracy and the average latency of each version
   - **c**: The cost per answered case and the response time of both
   - **d**: The size of the set and the margin of error on each score

2. A team grades a research assistant's free-form reports only with exact-match checks, and the score barely moves when the quality of the reports visibly changes. Which addition fits best?
   - **a**: A longer answer key, with many more phrasings of each answer listed
   - **b**: Human review of every report, with a person setting each of the scores
   - **c**: A model judge working from a rubric, with people sampling its misses
   - **d**: Fewer test cases, with every one of them carefully graded by hand

<details>
<summary>Answer key</summary>

1. **a**. A table by segment with the cost of the errors shows any regression that the overall figure averages away, and the refund rows weigh more. *b* is ruled out because "Equal overall scores can hide unequal failures". *c* is ruled out because "A figure from another dimension cannot stand in for the segment that is failing: a faster or cheaper system is not better where it is wrong". *d* is ruled out because "More cases make the score steadier and do not show where it is low".
2. **c**. Free text has no single correct form, so a rubric grader scales and people sample for what it misses. *a* is ruled out because "Exact match suits answers with one correct form and rejects a correct report that is worded differently". *b* is ruled out because "Human review of every output does not scale". *d* is ruled out because the documentation advises "More questions with slightly lower signal automated grading is better than fewer questions with high-quality human hand-graded evals".

</details>
