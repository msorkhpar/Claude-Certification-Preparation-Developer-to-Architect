# Changing safely, diagnosing a wrong answer and the practice

**Level:** Architect Professional · **Module 88:** Evaluation and optimisation · **Page 2 of 2**
**Exams:** P4

**After this page you can** run an A/B test with a minimum sample and a 95 percent bar, gate a change with a shadow run that protects the segments that matter, diagnose a wrong answer in the order that is cheapest to check, choose a model by an accuracy floor and a latency limit before price, and write the module's practice.

Checked on 2026-10-04 against the Claude documentation pages "Reduce hallucinations", "Reducing latency" and "Define success criteria and build evaluations", Anthropic's article "How we built our multi-agent research system", the Claude Certified Architect - Professional exam guide (version 1.0, domain 4 and its sample item 3), and by running the example and the practice offline in the course container. Nothing here called a model, and the cases, counts and timings in the example are invented. This page deepens module 43 (debugging Claude applications), module 18 (model choice, cost and migration) and module 19 (thinking, effort and speed). Metrics, evaluation sets and the report by segment are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 4 asks the candidate to "Conduct A/B testing and iterative improvements", to "Diagnose system issues (prompt failure, hallucinations, model mismatch)" and to "Optimize token usage, latency, and cost-performance trade-offs". Its sample item 3 keys retrieval as the first place to look when "confident but incorrect answers" follow a document refresh. *What the current product's documentation says (pages read 2026-10-04):* the hallucination page advises to "Explicitly give Claude permission to admit uncertainty" and to make answers "auditable by having it cite quotes and sources"; the latency page defines the measures (baseline latency and time to first token) and says "One of the most direct ways to reduce latency is to select the appropriate model for your use case". Neither page describes an A/B test or a shadow run: they are general engineering practice, taught here as the exam's strategies. On the exam, answer with the method and its order. The 95 percent bar and the minimum sample are values to tune to your own traffic.

## Why it matters

A team has a new prompt that raised the score on its offline set. It is tried on 80 live cases per arm, scores five points higher, and is shipped on the spot. A month later a new model version is trialled on a shadow copy of the traffic; its overall score equals the old one, and the team ships it. Refund answers regress and nobody notices for a week. A third answer goes wrong with confidence, and the team spends two days rewording the prompt before anyone looks at what was retrieved. Three decisions, three missing disciplines: a test that can tell a gain from chance, a gate that protects what matters, and a diagnosis that starts where checking is cheap.

## The idea

### A/B tests: telling a gain from chance

An A/B test sends live traffic to two versions at random and compares an outcome. Its first enemy is chance: with few cases, two equal versions will differ, and a gap of that size can be chance alone. The example's test is the plain two-proportion comparison, done at 95 percent: the gap must be large enough that equal versions would show it less than about one time in twenty. Four rules make it honest.

- **Decide the metric and the sample before you look.** One primary metric, set in advance, and a minimum number of cases per arm. The example's minimum is 200; below it the verdict is `too few cases`, whatever the gap.
- **The larger number is not the better version.** With 80 cases in an arm, a gap of five points is within chance. The same gap on 500 cases each can be clear. In the example, 410 right of 500 against 438 is a clear gain, and 410 against 425 is not.
- **Do not stop by who is ahead.** Stopping or extending a test according to who is ahead produces false wins. Running the same cases again repeats the same sample and adds no information. Collect the cases you planned.
- **Name the direction.** A clear difference can go either way. The verdict says `new is better` or `old is better`, and a test that can only report wins is not a test.

Size matters in both directions. Big early gains need few cases, which is why the first page starts small; a polish of a point or two needs hundreds per arm, which is why a mature system must plan its tests.

### Shadow runs: seeing a change before users do

A shadow run sends copies of live requests to the new version, keeps the answers away from users, and compares them with the answers the current version gave. It costs a second round of inference and gives no signal about how users react, but it exposes no user to a regression. It comes first; an A/B test comes after it, once the shadow run says the change is safe to show.

The comparison is by case: a case is *lost* when the current version was right and the new one is not, and *gained* in the opposite case. The gate has two keys, and the example's rule is the plain version of both. First, no protected segment may lose a case, because those segments are protected for their cost. A gain in one segment does not pay for a loss in another. Second, in the rest, the losses must not outnumber the gains. A change that ships to half of the traffic exposes half of the users to the regression before anyone has measured it, so that is not a gate. An average of segment accuracies weights a rare segment like a common one and hides its price. The example's shadow run holds the change for exactly this reason: two cases lost, two gained, and one of the losses is in the complaint segment.

### Diagnosing a wrong answer in the order that is cheapest to check

The exam names three kinds of fault: prompt failure, hallucination and model mismatch. A diagnosis puts them in an order, and the order follows the cost of checking.

1. **Was the evidence found?** Look at what was retrieved or supplied. If the right passage was never there, nothing downstream can be fixed by wording. The exam's third sample item is this case, and its rationale names "a broken re-index or mismatched embeddings" (module 85).
2. **Is the answer supported by the evidence?** If the evidence was there and the answer claims more than it says, the fault is grounding. The documentation's remedies: permission to say it does not know, quotes extracted first, a citation for every claim, and a check of "Inconsistencies across outputs", which "could indicate hallucinations".
3. **Is the reply in the right shape?** Format and instruction failures are cheap to see and cheap to fix in the prompt.
4. **Does the case fail on a stronger model too?** If it does, the task or the prompt is at fault and not the model. If it passes only on the stronger model, the model is a mismatch for that task, and the decision is a price decision (below).

The order puts the model last because a model change is the most expensive fix and the one most often wrongly blamed. A larger model does not know a document it was never shown. Sampling settings change how an answer is worded and not what evidence it was given. Examples teach a format; they do not supply a missing fact. The example runs five invented cases through the order, one for each answer.

### Optimising tokens, latency and cost without breaking quality

Optimisation is a search for the cheapest and fastest system that still meets the floor, so the floor comes first. The documentation: "It's always better to first engineer a prompt that works well without model or prompt constraints, and then try latency reduction strategies afterward. Trying to reduce latency prematurely might prevent you from discovering what top performance looks like." The steps are:

- **Measure the tail.** A mean hides the tail: the slowest five in a hundred requests are the ones users complain about. The example's twelve timings have a mean of 1,441 ms, a median of 980 ms and a 95th percentile of 4,800 ms. For streamed answers the time to the first token is the number users feel.
- **Choose the model by two limits, then by price.** Keep the options that reach the accuracy floor and fit the latency limit, and take the cheapest of them. Choosing the most accurate model pays for accuracy that the floor does not ask for. Cheapest first ignores the two limits, and the limits come before the price. When nothing fits, the answer is that no option fits, and the choice is between relaxing a limit and changing the design (a cache, a batch, a shorter output: modules 20 and 21).
- **Cut tokens where the evals say it is safe.** Trim the context and the output, and re-run the set. A saving that moves a protected segment is not a saving.

### The example

The example is the one from the first page. Its second half is this page: the latency figures, three live tests, the shadow gate, the diagnosis of five cases and the choice of a model under two sets of limits. It ran offline in every language.

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

### The practice: an evaluation kit

The practice is in [`exercises/88-evaluation-and-optimisation`](../../exercises/88-evaluation-and-optimisation/unit-01/practice-1/statement.md). You write the report by segment with the cost of the errors, the nearest-rank percentile, the A/B verdict at 95 percent with its minimum sample, the shadow gate with protected segments, the diagnosis order and the choice of a model under limits. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see.

## Traps

1. **"The new version scored five points higher, so ship it."** It is tempting because the number is higher. It fails because a small sample cannot tell a gain from chance. Plan the sample, fix the metric in advance, and call a result only when it clears the bar.
2. **"The totals are equal and the gains cover the losses, so the change is safe."** It is tempting because the sums balance. It fails because the losses may be in the segment that costs the most. Protect those segments with a gate of their own.
3. **"The answer is wrong, so try a bigger model."** It is tempting because a model change is easy to try. It fails because the fault is most often upstream, in what was retrieved or how the task was put, and a larger model cannot answer from evidence it never saw. Check the evidence first, the model last.

## Quiz

1. A new prompt is tried on 80 live cases in each arm and scores five points higher than the current one. What should the team conclude?
   - **a**: Adopt the new prompt now, because five points is the larger number and both arms have the same size
   - **b**: Too small a sample to tell, so gather the planned numbers
   - **c**: Run the same 80 cases again and average the two results, to get a steadier estimate
   - **d**: Extend the test only if the new prompt is behind, and stop at once if it is ahead

2. A new model version matches the current one on overall accuracy in a shadow run. It gains answers in order status and loses answers in refunds, an area the team guards. What does the gate do?
   - **a**: Ship it to everyone, because the overall totals are equal and the gains in one segment cover the losses in another
   - **b**: Average the accuracy of the two segments and ship if that average has risen
   - **c**: Ship it to half of the live traffic and watch whether any customers complain
   - **d**: Hold the change, since the costly segment regressed and improvements elsewhere cannot pay for that

<details>
<summary>Answer key</summary>

1. **b**. With 80 cases in an arm the gap is inside chance, so the verdict is that there are too few cases. *a* is ruled out because "The larger number is not the better version". *c* is ruled out because "Running the same cases again repeats the same sample and adds no information". *d* is ruled out because "Stopping or extending a test according to who is ahead produces false wins".
2. **d**. The gate has a key for guarded segments, and a loss there blocks the change. *a* is ruled out because "A gain in one segment does not pay for a loss in another". *c* is ruled out because "A change that ships to half of the traffic exposes half of the users to the regression before anyone has measured it". *b* is ruled out because "An average of segment accuracies weights a rare segment like a common one and hides its price".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A reply to a policy question is confident and wrong. The right passage was not among the chunks the model was given. Which first step fits?
   - **a**: Lower the temperature so that the wording of the answers stops varying between runs
   - **b**: Move the question to a larger model, which is more likely to know what the policy says than the current one is
   - **c**: Inspect the index and how the documents are split
   - **d**: Add more examples of confident replies to the prompt, so that the style is consistent

2. A team's check for its research agent demands exactly three searches in a fixed order, and it fails runs that reach the right answer by another route. What should the check judge instead?
   - **a**: Whether the runs repeated the same steps, because identical steps prove reliability
   - **b**: Whether the outcome is correct, then whether the process was sensible
   - **c**: The number of tool calls, since fewer calls mean a better agent
   - **d**: The final text, without any look at how it was reached

3. A team delays building graded examples until it can afford several hundred of them, and early prompt changes are judged by feel. What does the research team's experience suggest?
   - **a**: Test on the examples that sit in the prompt, since those are already written down
   - **b**: Wait until the full set has been built and checked, because a small set of examples cannot show a change at all
   - **c**: Keep judging by feel until launch, and begin to measure once real users arrive
   - **d**: Start now with a small set of real queries

<details>
<summary>Answer key</summary>

1. **c**. The evidence was never retrieved, so the index and the splitting of documents are the cheapest place to look. *b* is ruled out because "A larger model does not know a document it was never shown". *a* is ruled out because "Sampling settings change how an answer is worded and not what evidence it was given". *d* is ruled out because "Examples teach a format; they do not supply a missing fact".
2. **b**. "Even with identical starting points, agents might take completely different valid paths to reach their goal", so the rule is "Check that the result is right, then check that the process was reasonable". *a* is ruled out because "an eval that checks for a prescribed sequence of steps fails correct runs". *c* is ruled out because tool efficiency is only one of "five criteria (factual accuracy, citation accuracy, completeness, source quality and tool efficiency)". *d* is ruled out because the rule is to "check that the process was reasonable" after the result.
3. **d**. Early changes are large, so a small set shows them. *b* is ruled out because "We often hear that AI developer teams delay creating evals because they believe that only large evals with hundreds of test cases are useful", which the team found untrue. *c* is ruled out because "every week without an eval is a week of judging by feel". *a* is ruled out because the set should "mirror your real-world task distribution", and "The examples written into a prompt are not that distribution".

</details>
