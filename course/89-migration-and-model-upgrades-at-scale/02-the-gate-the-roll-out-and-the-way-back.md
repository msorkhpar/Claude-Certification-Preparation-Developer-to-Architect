# The gate, the roll-out and the way back

**Level:** Architect Professional · **Module 89:** Migration and model upgrades at scale · **Page 2 of 2**
**Exams:** P4, P6

**After this page you can** design a regression suite that gates an upgrade with cases that must pass, protected segments, and budgets for cost and latency, run the change in stages with a defined health check and a rollback, say how long a rollback stays possible, tell the stakeholders what the change trades, and write the module's practice.

Checked on 2026-10-04 against the Claude documentation pages "Model deprecations" and "Migration guide" for Claude Sonnet 5.5, the Claude Certified Architect - Professional exam guide (version 1.0, domains 4 and 6), and by running the example and the practice offline in the course container. Nothing here called a model, and the cases, costs, timings and counts are invented. This page deepens module 42 (evaluation) and module 88 (shadow runs and the gate with protected segments) to a change you must ship by a date. The calendar, the audit and the settings a new model changes are the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 4 asks the candidate to "Conduct A/B testing and iterative improvements" and domain 6 to "Communicate architectural decisions and trade-offs", to "Manage stakeholder feedback loops and expectation alignment (including SLAs)" and to "Support lifecycle phases (discovery, design, handoff, monitoring, iteration)". *What the current product's documentation says (pages read 2026-10-04):* the deprecations page advises to "Test your applications with newer models well before the retirement date of your current model" and says retired models fail; neither it nor the migration guide describes a staged roll-out or a gate. A regression gate, a staged roll-out and a rollback are general engineering practice, taught here as the exam's strategies, and the stage sizes, the minimum request count and the error limit in the example are values to tune to your own traffic.

## Why it matters

A team has migrated the request settings and the new model answers. Its regression suite shows an overall pass rate that is two points higher, so the lead switches all traffic overnight. By noon three things are known: a case that must pass (a refund amount stated to the cent) fails; the bill per answer is a third higher; and a spike of errors from one client library has no rollback because the old model was removed from the configuration. Each was visible beforehand to a gate with the right rules. The exam asks what the gate holds, how the change goes out, and what keeps the way back open.

## The idea

### A regression suite that gates

The suite from module 42 and the report by segment from module 88 become a gate. A gate is a decision procedure: it takes the results of the old and the new model on the same cases and returns go or no-go with the reasons. The example's gate has five checks, in a fixed order, and every failing check adds a reason, so that a refusal can be read and argued with.

1. **Must-pass cases.** Some cases are rules and not statistics: a refund amount that must be exact, a refusal that must happen, a prohibited disclosure that must not. If any fails, the change is refused. An average can rise while a case that must pass breaks. Re-weighting a failed case turns a rule into a preference, and watching a known failure in production makes customers the test.
2. **Protected segments.** A segment with costly errors (module 88) must not lose an answer, even when the totals are equal. A gain in one segment does not pay for a loss in another.
3. **Net change.** Across the rest, losses must not outnumber gains. The reason names both counts.
4. **Cost.** The rise in total cost, in whole percent, against a budget set before the run. A rise exactly at the budget passes and one point over it is refused. The budget is set from the re-baselined cost of the first page, because the new model's price and token counts differ.
5. **Latency.** The tail, by the nearest-rank 95th percentile of the new timings, against a limit. A single slow case among forty does not block the change, and three do. The example's check is the percentile and not the worst case, for the reason given in module 88.

The gate does not decide the migration: the calendar does, and the gate says what must be fixed or accepted first. A refused gate with a retirement date in 40 days becomes a work list with an owner for each reason.

### A staged roll-out

Passing a suite is a prediction about traffic, and traffic is where it is tested. A staged roll-out sends a small share to the new model first and moves on only when that share is healthy. The example's stages are 1, 5, 25 and 100 percent of requests, and its step rule has three parts.

- **Hold until there is evidence.** A stage with fewer requests than the minimum (1,000 in the example) says nothing yet: hold at the stage.
- **Roll back when the limit is passed.** The errors per thousand requests, taken as a whole number rounded down, are compared with a limit (5 in the example). Over it, the share goes back to zero.
- **Otherwise advance.** One stage up, and `complete` at 100 percent.

The health check is not only the error rate. Use the signals of module 87: refusals, tokens per answer, the share of answers a user flagged, the tail latency. And use the drift habit of that page: compare each with the baseline of the old model, and expect cost and token counts to differ by design.

### Rolling back, and how long you can

When the limit is passed, the response is to send every request back to the previous model. Holding at the stage while errors continue leaves the share of customers who see them where it is. Advancing to 100 percent to see the failure in the full data makes every customer the sample. Reducing to a smaller share keeps part of the damage on the path.

A rollback is only as good as the model it returns to. Keep the previous model's id available, tested and configured until its own retirement date; once that date has passed, "Requests to retired models will fail", and the way back is closed. This is why a migration has two deadlines: the date by which the new model must be ready, and the date after which the old one cannot be the fallback. The first page's calendar shows both, and a roll-out that starts the week before retirement has no rollback at all. Start early enough that the rollback window lasts through the last stage and a soak period.

### Telling the stakeholders

A migration is a decision with a trade, and the architect's job (domain 6) is to state the trade. What improves: refund accuracy up in the segment that costs most. What it costs: a rise against the budget, with the new baseline and the reason. What is at risk: the cases that regressed, the roll-out plan and the rollback. And the date that forces the choice. A recommended replacement is a starting point, not a decision. A rise in cost is a number to put before the owner, not a verdict: staying has a date that ends it. Waiting for the retirement date hands the decision to the calendar, and gives up the rollback.

### The example

The example is the one from the first page. Its second half is this page: a suite of 20 cases on which the gate says no-go, naming a protected segment and a cost rise of 35 percent against a 25 percent budget; the same suite after the refund fix, with a 40 percent budget, which passes; and the roll-out steps for five observations. It ran offline in every language.

<!-- example: m89-rollout-gate tabs: python,typescript,java,kotlin -->
```python
"""Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.

The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
"""
from collections import namedtuple
from datetime import date

Case = namedtuple("Case", "id segment must_pass old_ok new_ok old_cost new_cost new_ms")
Request = namedtuple("Request", "model temperature top_p top_k thinking tool_choice strict prefill")
STAGES = [1, 5, 25, 100]
TARGET = "claude-sonnet-5-5"


def days_until(today, when):
    return (date.fromisoformat(when) - date.fromisoformat(today)).days


def level(days):
    if days < 0:
        return "retired"
    if days <= 14:
        return "urgent"
    return "migrate now" if days <= 60 else "watch"


def retirement_status(models, today):
    """One line per model, the nearest retirement first. A date marked tentative is a date that may move later."""
    rows = sorted((days_until(today, when), name, tentative) for name, when, tentative in models)
    return [f"{name}: {days} days, {level(days)}{' (tentative)' if tentative else ''}" for days, name, tentative in rows]


def migrate_request(request, target=TARGET):
    """The settings the target model refuses are removed or replaced, and each change is named."""
    changes = []
    if request.model != target:
        changes.append(f"model set to {target}")
    for field in ("temperature", "top_p", "top_k"):
        if getattr(request, field) is not None:
            changes.append(f"removed {field}")
    thinking = request.thinking
    if thinking == "budget":
        thinking = "adaptive"
        changes.append("thinking budget replaced by adaptive thinking; sweep the effort")
    elif thinking == "disabled":
        thinking = "between_tools"
        changes.append("thinking disabled replaced by between_tools")
    tool_choice, strict = request.tool_choice, request.strict
    if tool_choice in ("any", "tool"):
        tool_choice, strict = "auto", True
        changes.append("forced tool choice replaced by auto with strict tools")
    if request.prefill:
        changes.append("assistant prefill removed; state the format in the instructions")
    new = Request(target, None, None, None, thinking, tool_choice, strict, False)
    return new, changes


def percentile(values, p):
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def gate(cases, protected, max_cost_up, max_p95):
    """A go needs every check to pass; every check that fails adds a reason, in a fixed order."""
    reasons = []
    failed = sorted(c.id for c in cases if c.must_pass and not c.new_ok)
    if failed:
        reasons.append("must-pass failed: " + ", ".join(failed))
    lost = [c for c in cases if c.old_ok and not c.new_ok]
    gained = [c for c in cases if c.new_ok and not c.old_ok]
    hit = sorted({c.segment for c in lost if c.segment in protected})
    if hit:
        reasons.append("protected segment lost answers: " + ", ".join(hit))
    if len(lost) > len(gained):
        reasons.append(f"net loss: lost {len(lost)}, gained {len(gained)}")
    old_total, new_total = sum(c.old_cost for c in cases), sum(c.new_cost for c in cases)
    up = (new_total - old_total) * 100 // old_total if old_total > 0 and new_total > old_total else 0
    if up > max_cost_up:
        reasons.append(f"cost up {up}% over the {max_cost_up}% limit")
    p95 = percentile([c.new_ms for c in cases], 95)
    if p95 > max_p95:
        reasons.append(f"p95 latency {p95} ms over the {max_p95} ms limit")
    return {"decision": "no-go" if reasons else "go", "reasons": reasons}


def rollout_step(stage, requests, errors, min_requests, max_errors_per_1000):
    """Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on."""
    if requests < min_requests:
        return f"hold at {stage}"
    if errors * 1000 // requests > max_errors_per_1000:
        return "rollback to 0"
    if stage == STAGES[-1]:
        return "complete"
    return f"advance to {STAGES[STAGES.index(stage) + 1]}"


def suite():
    rows = []
    for i, ms in enumerate((900, 950, 1000, 1100, 1200), 1):
        rows.append(Case(f"b{i}", "billing", True, True, True, 4, 5, ms))
    for i, ms in enumerate((1500, 1600, 1700, 1800, 2100), 1):
        rows.append(Case(f"r{i}", "refund", i <= 2, True, i != 4, 6, 8, ms))
    for i in range(1, 11):
        rows.append(Case(f"f{i}", "faq", False, i not in (8, 9, 10), i != 10, 2, 3, 500 + 20 * i + 80 * (i // 2)))
    return rows


def main():
    models = [("claude-haiku-4-5-20251001", "2026-10-15", True), ("claude-sonnet-4-5-20250929", "2026-11-30", False), ("claude-opus-4-1-20250805", "2026-08-05", False)]
    print("retirement calendar on 2026-10-04:")
    for line in retirement_status(models, "2026-10-04"):
        print("  " + line)
    old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, None, "budget", "tool", False, True)
    new, changes = migrate_request(old)
    print(f"request for {new.model}: {len(changes)} changes")
    for change in changes:
        print("  " + change)
    cases = suite()
    first = gate(cases, {"refund"}, 25, 2000)
    print(f"gate on {len(cases)} cases: {first['decision']}")
    for reason in first["reasons"]:
        print("  " + reason)
    fixed = [c._replace(new_ok=True) if c.id == "r4" else c for c in cases]
    second = gate(fixed, {"refund"}, 40, 2000)
    print(f"gate after the refund fix, cost limit 40%: {second['decision']}, {len(second['reasons'])} reasons")
    for stage, requests, errors in ((1, 2000, 6), (5, 300, 0), (5, 10000, 20), (25, 50000, 400), (100, 50000, 10)):
        print(f"roll-out at {stage}% with {requests} requests and {errors} errors: {rollout_step(stage, requests, errors, 1000, 5)}")


if __name__ == "__main__":
    main()
```
```text
retirement calendar on 2026-10-04:
  claude-opus-4-1-20250805: -60 days, retired
  claude-haiku-4-5-20251001: 11 days, urgent (tentative)
  claude-sonnet-4-5-20250929: 57 days, migrate now
request for claude-sonnet-5-5: 6 changes
  model set to claude-sonnet-5-5
  removed temperature
  removed top_p
  thinking budget replaced by adaptive thinking; sweep the effort
  forced tool choice replaced by auto with strict tools
  assistant prefill removed; state the format in the instructions
gate on 20 cases: no-go
  protected segment lost answers: refund
  cost up 35% over the 25% limit
gate after the refund fix, cost limit 40%: go, 0 reasons
roll-out at 1% with 2000 requests and 6 errors: advance to 5
roll-out at 5% with 300 requests and 0 errors: hold at 5
roll-out at 5% with 10000 requests and 20 errors: advance to 25
roll-out at 25% with 50000 requests and 400 errors: rollback to 0
roll-out at 100% with 50000 requests and 10 errors: complete
```
```typescript
/**
 * Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.
 *
 * The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
 */
export type Case = { id: string; segment: string; mustPass: boolean; oldOk: boolean; newOk: boolean; oldCost: number; newCost: number; newMs: number };
export type Request = { model: string; temperature: number | null; topP: number | null; topK: number | null; thinking: string; toolChoice: string; strict: boolean; prefill: boolean };

export const STAGES = [1, 5, 25, 100];
export const TARGET = "claude-sonnet-5-5";

export function daysUntil(today: string, when: string): number {
  const [y1, m1, d1] = today.split("-").map(Number);
  const [y2, m2, d2] = when.split("-").map(Number);
  return Math.round((Date.UTC(y2, m2 - 1, d2) - Date.UTC(y1, m1 - 1, d1)) / 86400000);
}

export function level(days: number): string {
  if (days < 0) return "retired";
  if (days <= 14) return "urgent";
  return days <= 60 ? "migrate now" : "watch";
}

/** One line per model, the nearest retirement first. A date marked tentative is a date that may move later. */
export function retirementStatus(models: [string, string, boolean][], today: string): string[] {
  const rows = models.map(([name, when, tentative]) => ({ days: daysUntil(today, when), name, tentative }));
  rows.sort((a, b) => a.days - b.days || (a.name < b.name ? -1 : a.name > b.name ? 1 : Number(a.tentative) - Number(b.tentative)));
  return rows.map((r) => `${r.name}: ${r.days} days, ${level(r.days)}${r.tentative ? " (tentative)" : ""}`);
}

/** The settings the target model refuses are removed or replaced, and each change is named. */
export function migrateRequest(request: Request, target = TARGET): [Request, string[]] {
  const changes: string[] = [];
  if (request.model !== target) changes.push(`model set to ${target}`);
  if (request.temperature !== null) changes.push("removed temperature");
  if (request.topP !== null) changes.push("removed top_p");
  if (request.topK !== null) changes.push("removed top_k");
  let thinking = request.thinking;
  if (thinking === "budget") {
    thinking = "adaptive";
    changes.push("thinking budget replaced by adaptive thinking; sweep the effort");
  } else if (thinking === "disabled") {
    thinking = "between_tools";
    changes.push("thinking disabled replaced by between_tools");
  }
  let toolChoice = request.toolChoice;
  let strict = request.strict;
  if (toolChoice === "any" || toolChoice === "tool") {
    toolChoice = "auto";
    strict = true;
    changes.push("forced tool choice replaced by auto with strict tools");
  }
  if (request.prefill) changes.push("assistant prefill removed; state the format in the instructions");
  return [{ model: target, temperature: null, topP: null, topK: null, thinking, toolChoice, strict, prefill: false }, changes];
}

export function percentile(values: number[], p: number): number {
  const ordered = [...values].sort((a, b) => a - b);
  return ordered.length ? ordered[Math.floor((p * ordered.length + 99) / 100) - 1] : 0;
}

/** A go needs every check to pass; every check that fails adds a reason, in a fixed order. */
export function gate(cases: Case[], protectedSegments: Set<string>, maxCostUp: number, maxP95: number): { decision: string; reasons: string[] } {
  const reasons: string[] = [];
  const failed = cases.filter((c) => c.mustPass && !c.newOk).map((c) => c.id).sort();
  if (failed.length) reasons.push("must-pass failed: " + failed.join(", "));
  const lost = cases.filter((c) => c.oldOk && !c.newOk);
  const gained = cases.filter((c) => c.newOk && !c.oldOk);
  const hit = [...new Set(lost.filter((c) => protectedSegments.has(c.segment)).map((c) => c.segment))].sort();
  if (hit.length) reasons.push("protected segment lost answers: " + hit.join(", "));
  if (lost.length > gained.length) reasons.push(`net loss: lost ${lost.length}, gained ${gained.length}`);
  const oldTotal = cases.reduce((a, c) => a + c.oldCost, 0);
  const newTotal = cases.reduce((a, c) => a + c.newCost, 0);
  const up = oldTotal > 0 && newTotal > oldTotal ? Math.floor(((newTotal - oldTotal) * 100) / oldTotal) : 0;
  if (up > maxCostUp) reasons.push(`cost up ${up}% over the ${maxCostUp}% limit`);
  const p95 = percentile(cases.map((c) => c.newMs), 95);
  if (p95 > maxP95) reasons.push(`p95 latency ${p95} ms over the ${maxP95} ms limit`);
  return { decision: reasons.length ? "no-go" : "go", reasons };
}

/** Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on. */
export function rolloutStep(stage: number, requests: number, errors: number, minRequests: number, maxErrorsPer1000: number): string {
  if (requests < minRequests) return `hold at ${stage}`;
  if (Math.floor((errors * 1000) / requests) > maxErrorsPer1000) return "rollback to 0";
  if (stage === STAGES[STAGES.length - 1]) return "complete";
  return `advance to ${STAGES[STAGES.indexOf(stage) + 1]}`;
}

export function suite(): Case[] {
  const rows: Case[] = [];
  [900, 950, 1000, 1100, 1200].forEach((ms, k) => rows.push({ id: `b${k + 1}`, segment: "billing", mustPass: true, oldOk: true, newOk: true, oldCost: 4, newCost: 5, newMs: ms }));
  [1500, 1600, 1700, 1800, 2100].forEach((ms, k) => rows.push({ id: `r${k + 1}`, segment: "refund", mustPass: k + 1 <= 2, oldOk: true, newOk: k + 1 !== 4, oldCost: 6, newCost: 8, newMs: ms }));
  for (let i = 1; i <= 10; i++) rows.push({ id: `f${i}`, segment: "faq", mustPass: false, oldOk: ![8, 9, 10].includes(i), newOk: i !== 10, oldCost: 2, newCost: 3, newMs: 500 + 20 * i + 80 * Math.floor(i / 2) });
  return rows;
}

function main(): void {
  const models: [string, string, boolean][] = [["claude-haiku-4-5-20251001", "2026-10-15", true], ["claude-sonnet-4-5-20250929", "2026-11-30", false], ["claude-opus-4-1-20250805", "2026-08-05", false]];
  console.log("retirement calendar on 2026-10-04:");
  for (const line of retirementStatus(models, "2026-10-04")) console.log("  " + line);
  const old: Request = { model: "claude-sonnet-4-5-20250929", temperature: 0.7, topP: 0.9, topK: null, thinking: "budget", toolChoice: "tool", strict: false, prefill: true };
  const [next, changes] = migrateRequest(old);
  console.log(`request for ${next.model}: ${changes.length} changes`);
  for (const change of changes) console.log("  " + change);
  const cases = suite();
  const first = gate(cases, new Set(["refund"]), 25, 2000);
  console.log(`gate on ${cases.length} cases: ${first.decision}`);
  for (const reason of first.reasons) console.log("  " + reason);
  const fixed = cases.map((c) => (c.id === "r4" ? { ...c, newOk: true } : c));
  const second = gate(fixed, new Set(["refund"]), 40, 2000);
  console.log(`gate after the refund fix, cost limit 40%: ${second.decision}, ${second.reasons.length} reasons`);
  for (const [stage, requests, errors] of [[1, 2000, 6], [5, 300, 0], [5, 10000, 20], [25, 50000, 400], [100, 50000, 10]]) {
    console.log(`roll-out at ${stage}% with ${requests} requests and ${errors} errors: ${rolloutStep(stage, requests, errors, 1000, 5)}`);
  }
}

if (import.meta.main) main();
```
```text
retirement calendar on 2026-10-04:
  claude-opus-4-1-20250805: -60 days, retired
  claude-haiku-4-5-20251001: 11 days, urgent (tentative)
  claude-sonnet-4-5-20250929: 57 days, migrate now
request for claude-sonnet-5-5: 6 changes
  model set to claude-sonnet-5-5
  removed temperature
  removed top_p
  thinking budget replaced by adaptive thinking; sweep the effort
  forced tool choice replaced by auto with strict tools
  assistant prefill removed; state the format in the instructions
gate on 20 cases: no-go
  protected segment lost answers: refund
  cost up 35% over the 25% limit
gate after the refund fix, cost limit 40%: go, 0 reasons
roll-out at 1% with 2000 requests and 6 errors: advance to 5
roll-out at 5% with 300 requests and 0 errors: hold at 5
roll-out at 5% with 10000 requests and 20 errors: advance to 25
roll-out at 25% with 50000 requests and 400 errors: rollback to 0
roll-out at 100% with 50000 requests and 10 errors: complete
```
```java
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.
 *
 * The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
 */
public class RolloutGate {
    record Case(String id, String segment, boolean mustPass, boolean oldOk, boolean newOk, int oldCost, int newCost, int newMs) {}

    record Request(String model, Double temperature, Double topP, Double topK, String thinking, String toolChoice, boolean strict, boolean prefill) {}

    record Model(String name, String when, boolean tentative) {}

    record Migration(Request request, List<String> changes) {}

    record Verdict(String decision, List<String> reasons) {}

    static final int[] STAGES = {1, 5, 25, 100};
    static final String TARGET = "claude-sonnet-5-5";

    static long daysUntil(String today, String when) {
        return ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(when));
    }

    static String level(long days) {
        if (days < 0) return "retired";
        if (days <= 14) return "urgent";
        return days <= 60 ? "migrate now" : "watch";
    }

    /** One line per model, the nearest retirement first. A date marked tentative is a date that may move later. */
    static List<String> retirementStatus(List<Model> models, String today) {
        List<Model> sorted = new ArrayList<>(models);
        sorted.sort(Comparator.comparingLong((Model m) -> daysUntil(today, m.when())).thenComparing(Model::name).thenComparing(Model::tentative));
        List<String> out = new ArrayList<>();
        for (Model m : sorted) {
            long days = daysUntil(today, m.when());
            out.add(m.name() + ": " + days + " days, " + level(days) + (m.tentative() ? " (tentative)" : ""));
        }
        return out;
    }

    /** The settings the target model refuses are removed or replaced, and each change is named. */
    static Migration migrateRequest(Request request) {
        List<String> changes = new ArrayList<>();
        if (!request.model().equals(TARGET)) changes.add("model set to " + TARGET);
        if (request.temperature() != null) changes.add("removed temperature");
        if (request.topP() != null) changes.add("removed top_p");
        if (request.topK() != null) changes.add("removed top_k");
        String thinking = request.thinking();
        if (thinking.equals("budget")) {
            thinking = "adaptive";
            changes.add("thinking budget replaced by adaptive thinking; sweep the effort");
        } else if (thinking.equals("disabled")) {
            thinking = "between_tools";
            changes.add("thinking disabled replaced by between_tools");
        }
        String toolChoice = request.toolChoice();
        boolean strict = request.strict();
        if (toolChoice.equals("any") || toolChoice.equals("tool")) {
            toolChoice = "auto";
            strict = true;
            changes.add("forced tool choice replaced by auto with strict tools");
        }
        if (request.prefill()) changes.add("assistant prefill removed; state the format in the instructions");
        return new Migration(new Request(TARGET, null, null, null, thinking, toolChoice, strict, false), changes);
    }

    static int percentile(List<Integer> values, int p) {
        if (values.isEmpty()) return 0;
        List<Integer> ordered = new ArrayList<>(values);
        ordered.sort(null);
        return ordered.get((p * ordered.size() + 99) / 100 - 1);
    }

    /** A go needs every check to pass; every check that fails adds a reason, in a fixed order. */
    static Verdict gate(List<Case> cases, Set<String> protectedSegments, int maxCostUp, int maxP95) {
        List<String> reasons = new ArrayList<>();
        TreeSet<String> failed = new TreeSet<>();
        TreeSet<String> hit = new TreeSet<>();
        int lost = 0;
        int gained = 0;
        int oldTotal = 0;
        int newTotal = 0;
        List<Integer> times = new ArrayList<>();
        for (Case c : cases) {
            if (c.mustPass() && !c.newOk()) failed.add(c.id());
            if (c.oldOk() && !c.newOk()) {
                lost++;
                if (protectedSegments.contains(c.segment())) hit.add(c.segment());
            }
            if (c.newOk() && !c.oldOk()) gained++;
            oldTotal += c.oldCost();
            newTotal += c.newCost();
            times.add(c.newMs());
        }
        if (!failed.isEmpty()) reasons.add("must-pass failed: " + String.join(", ", failed));
        if (!hit.isEmpty()) reasons.add("protected segment lost answers: " + String.join(", ", hit));
        if (lost > gained) reasons.add("net loss: lost " + lost + ", gained " + gained);
        int up = oldTotal > 0 && newTotal > oldTotal ? (newTotal - oldTotal) * 100 / oldTotal : 0;
        if (up > maxCostUp) reasons.add("cost up " + up + "% over the " + maxCostUp + "% limit");
        int p95 = percentile(times, 95);
        if (p95 > maxP95) reasons.add("p95 latency " + p95 + " ms over the " + maxP95 + " ms limit");
        return new Verdict(reasons.isEmpty() ? "go" : "no-go", reasons);
    }

    /** Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on. */
    static String rolloutStep(int stage, int requests, int errors, int minRequests, int maxErrorsPer1000) {
        if (requests < minRequests) return "hold at " + stage;
        if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0";
        if (stage == STAGES[STAGES.length - 1]) return "complete";
        for (int i = 0; i < STAGES.length; i++) if (STAGES[i] == stage) return "advance to " + STAGES[i + 1];
        throw new IllegalArgumentException("unknown stage " + stage);
    }

    static List<Case> suite() {
        List<Case> rows = new ArrayList<>();
        int[] billing = {900, 950, 1000, 1100, 1200};
        for (int i = 1; i <= 5; i++) rows.add(new Case("b" + i, "billing", true, true, true, 4, 5, billing[i - 1]));
        int[] refund = {1500, 1600, 1700, 1800, 2100};
        for (int i = 1; i <= 5; i++) rows.add(new Case("r" + i, "refund", i <= 2, true, i != 4, 6, 8, refund[i - 1]));
        for (int i = 1; i <= 10; i++) rows.add(new Case("f" + i, "faq", false, !(i == 8 || i == 9 || i == 10), i != 10, 2, 3, 500 + 20 * i + 80 * (i / 2)));
        return rows;
    }

    public static void main(String[] args) {
        List<Model> models = List.of(new Model("claude-haiku-4-5-20251001", "2026-10-15", true), new Model("claude-sonnet-4-5-20250929", "2026-11-30", false), new Model("claude-opus-4-1-20250805", "2026-08-05", false));
        System.out.println("retirement calendar on 2026-10-04:");
        for (String line : retirementStatus(models, "2026-10-04")) System.out.println("  " + line);
        Request old = new Request("claude-sonnet-4-5-20250929", 0.7, 0.9, null, "budget", "tool", false, true);
        Migration migration = migrateRequest(old);
        System.out.println("request for " + migration.request().model() + ": " + migration.changes().size() + " changes");
        for (String change : migration.changes()) System.out.println("  " + change);
        List<Case> cases = suite();
        Verdict first = gate(cases, Set.of("refund"), 25, 2000);
        System.out.println("gate on " + cases.size() + " cases: " + first.decision());
        for (String reason : first.reasons()) System.out.println("  " + reason);
        List<Case> fixed = new ArrayList<>();
        for (Case c : cases) fixed.add(c.id().equals("r4") ? new Case(c.id(), c.segment(), c.mustPass(), c.oldOk(), true, c.oldCost(), c.newCost(), c.newMs()) : c);
        Verdict second = gate(fixed, Set.of("refund"), 40, 2000);
        System.out.println("gate after the refund fix, cost limit 40%: " + second.decision() + ", " + second.reasons().size() + " reasons");
        int[][] observations = {{1, 2000, 6}, {5, 300, 0}, {5, 10000, 20}, {25, 50000, 400}, {100, 50000, 10}};
        for (int[] o : observations) System.out.println("roll-out at " + o[0] + "% with " + o[1] + " requests and " + o[2] + " errors: " + rolloutStep(o[0], o[1], o[2], 1000, 5));
    }
}
```
```text
retirement calendar on 2026-10-04:
  claude-opus-4-1-20250805: -60 days, retired
  claude-haiku-4-5-20251001: 11 days, urgent (tentative)
  claude-sonnet-4-5-20250929: 57 days, migrate now
request for claude-sonnet-5-5: 6 changes
  model set to claude-sonnet-5-5
  removed temperature
  removed top_p
  thinking budget replaced by adaptive thinking; sweep the effort
  forced tool choice replaced by auto with strict tools
  assistant prefill removed; state the format in the instructions
gate on 20 cases: no-go
  protected segment lost answers: refund
  cost up 35% over the 25% limit
gate after the refund fix, cost limit 40%: go, 0 reasons
roll-out at 1% with 2000 requests and 6 errors: advance to 5
roll-out at 5% with 300 requests and 0 errors: hold at 5
roll-out at 5% with 10000 requests and 20 errors: advance to 25
roll-out at 25% with 50000 requests and 400 errors: rollback to 0
roll-out at 100% with 50000 requests and 10 errors: complete
```
```kotlin
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.
 *
 * The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
 */
data class Case(val id: String, val segment: String, val mustPass: Boolean, val oldOk: Boolean, val newOk: Boolean, val oldCost: Int, val newCost: Int, val newMs: Int)

data class Request(val model: String, val temperature: Double?, val topP: Double?, val topK: Double?, val thinking: String, val toolChoice: String, val strict: Boolean, val prefill: Boolean)

data class Model(val name: String, val date: String, val tentative: Boolean)

data class Verdict(val decision: String, val reasons: List<String>)

val STAGES = listOf(1, 5, 25, 100)
const val TARGET = "claude-sonnet-5-5"

fun daysUntil(today: String, date: String): Long = ChronoUnit.DAYS.between(LocalDate.parse(today), LocalDate.parse(date))

fun level(days: Long): String = if (days < 0) "retired" else if (days <= 14) "urgent" else if (days <= 60) "migrate now" else "watch"

/** One line per model, the nearest retirement first. A date marked tentative is a date that may move later. */
fun retirementStatus(models: List<Model>, today: String): List<String> =
    models.sortedWith(compareBy<Model>({ daysUntil(today, it.date) }, { it.name }, { it.tentative })).map {
        val days = daysUntil(today, it.date)
        "${it.name}: $days days, ${level(days)}${if (it.tentative) " (tentative)" else ""}"
    }

/** The settings the target model refuses are removed or replaced, and each change is named. */
fun migrateRequest(request: Request): Pair<Request, List<String>> {
    val changes = mutableListOf<String>()
    if (request.model != TARGET) changes.add("model set to $TARGET")
    if (request.temperature != null) changes.add("removed temperature")
    if (request.topP != null) changes.add("removed top_p")
    if (request.topK != null) changes.add("removed top_k")
    var thinking = request.thinking
    if (thinking == "budget") {
        thinking = "adaptive"
        changes.add("thinking budget replaced by adaptive thinking; sweep the effort")
    } else if (thinking == "disabled") {
        thinking = "between_tools"
        changes.add("thinking disabled replaced by between_tools")
    }
    var toolChoice = request.toolChoice
    var strict = request.strict
    if (toolChoice == "any" || toolChoice == "tool") {
        toolChoice = "auto"
        strict = true
        changes.add("forced tool choice replaced by auto with strict tools")
    }
    if (request.prefill) changes.add("assistant prefill removed; state the format in the instructions")
    return Pair(Request(TARGET, null, null, null, thinking, toolChoice, strict, false), changes)
}

fun percentile(values: List<Int>, p: Int): Int {
    if (values.isEmpty()) return 0
    val ordered = values.sorted()
    return ordered[(p * ordered.size + 99) / 100 - 1]
}

/** A go needs every check to pass; every check that fails adds a reason, in a fixed order. */
fun gate(cases: List<Case>, protectedSegments: Set<String>, maxCostUp: Int, maxP95: Int): Verdict {
    val reasons = mutableListOf<String>()
    val failed = cases.filter { it.mustPass && !it.newOk }.map { it.id }.sorted()
    if (failed.isNotEmpty()) reasons.add("must-pass failed: " + failed.joinToString(", "))
    val lost = cases.filter { it.oldOk && !it.newOk }
    val gained = cases.filter { it.newOk && !it.oldOk }
    val hit = lost.filter { it.segment in protectedSegments }.map { it.segment }.toSortedSet().toList()
    if (hit.isNotEmpty()) reasons.add("protected segment lost answers: " + hit.joinToString(", "))
    if (lost.size > gained.size) reasons.add("net loss: lost ${lost.size}, gained ${gained.size}")
    val oldTotal = cases.map { it.oldCost }.sum()
    val newTotal = cases.map { it.newCost }.sum()
    val up = if (oldTotal > 0 && newTotal > oldTotal) (newTotal - oldTotal) * 100 / oldTotal else 0
    if (up > maxCostUp) reasons.add("cost up $up% over the $maxCostUp% limit")
    val p95 = percentile(cases.map { it.newMs }, 95)
    if (p95 > maxP95) reasons.add("p95 latency $p95 ms over the $maxP95 ms limit")
    return Verdict(if (reasons.isEmpty()) "go" else "no-go", reasons)
}

/** Hold until the stage has enough requests, roll back to zero when errors pass the limit, otherwise go on. */
fun rolloutStep(stage: Int, requests: Int, errors: Int, minRequests: Int, maxErrorsPer1000: Int): String {
    if (requests < minRequests) return "hold at $stage"
    if (errors * 1000L / requests > maxErrorsPer1000) return "rollback to 0"
    if (stage == STAGES.last()) return "complete"
    return "advance to ${STAGES[STAGES.indexOf(stage) + 1]}"
}

fun suite(): List<Case> {
    val rows = mutableListOf<Case>()
    val billing = listOf(900, 950, 1000, 1100, 1200)
    for (i in 1..5) rows.add(Case("b$i", "billing", true, true, true, 4, 5, billing[i - 1]))
    val refund = listOf(1500, 1600, 1700, 1800, 2100)
    for (i in 1..5) rows.add(Case("r$i", "refund", i <= 2, true, i != 4, 6, 8, refund[i - 1]))
    for (i in 1..10) rows.add(Case("f$i", "faq", false, i !in listOf(8, 9, 10), i != 10, 2, 3, 500 + 20 * i + 80 * (i / 2)))
    return rows
}

fun main() {
    val models = listOf(Model("claude-haiku-4-5-20251001", "2026-10-15", true), Model("claude-sonnet-4-5-20250929", "2026-11-30", false), Model("claude-opus-4-1-20250805", "2026-08-05", false))
    println("retirement calendar on 2026-10-04:")
    for (line in retirementStatus(models, "2026-10-04")) println("  $line")
    val old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, null, "budget", "tool", false, true)
    val (next, changes) = migrateRequest(old)
    println("request for ${next.model}: ${changes.size} changes")
    for (change in changes) println("  $change")
    val cases = suite()
    val first = gate(cases, setOf("refund"), 25, 2000)
    println("gate on ${cases.size} cases: ${first.decision}")
    for (reason in first.reasons) println("  $reason")
    val fixed = cases.map { if (it.id == "r4") it.copy(newOk = true) else it }
    val second = gate(fixed, setOf("refund"), 40, 2000)
    println("gate after the refund fix, cost limit 40%: ${second.decision}, ${second.reasons.size} reasons")
    for ((stage, requests, errors) in listOf(Triple(1, 2000, 6), Triple(5, 300, 0), Triple(5, 10000, 20), Triple(25, 50000, 400), Triple(100, 50000, 10))) {
        println("roll-out at $stage% with $requests requests and $errors errors: ${rolloutStep(stage, requests, errors, 1000, 5)}")
    }
}
```
```text
retirement calendar on 2026-10-04:
  claude-opus-4-1-20250805: -60 days, retired
  claude-haiku-4-5-20251001: 11 days, urgent (tentative)
  claude-sonnet-4-5-20250929: 57 days, migrate now
request for claude-sonnet-5-5: 6 changes
  model set to claude-sonnet-5-5
  removed temperature
  removed top_p
  thinking budget replaced by adaptive thinking; sweep the effort
  forced tool choice replaced by auto with strict tools
  assistant prefill removed; state the format in the instructions
gate on 20 cases: no-go
  protected segment lost answers: refund
  cost up 35% over the 25% limit
gate after the refund fix, cost limit 40%: go, 0 reasons
roll-out at 1% with 2000 requests and 6 errors: advance to 5
roll-out at 5% with 300 requests and 0 errors: hold at 5
roll-out at 5% with 10000 requests and 20 errors: advance to 25
roll-out at 25% with 50000 requests and 400 errors: rollback to 0
roll-out at 100% with 50000 requests and 10 errors: complete
```
<!-- /example -->

### The practice: a roll-out gate

The practice is in [`exercises/89-migration-and-model-upgrades-at-scale`](../../exercises/89-migration-and-model-upgrades-at-scale/unit-01/practice-1/statement.md). You write the retirement calendar with its levels, the migration of a request to the settings a new model accepts, the gate on a regression suite with its reasons in a fixed order, and the step of a staged roll-out. It is graded in Python, TypeScript, Java and Kotlin; the statement lists nine cases, each saying what you should see.

## Traps

1. **"The average pass rate rose, so the change is good."** It is tempting because it is one number and it went up. It fails because a case that must pass can break while the average rises. Gate on must-pass cases and protected segments before you look at the average.
2. **"Switch all traffic at once to finish before the deadline."** It is tempting because it ends the project in one night. It fails because a suite predicts and traffic proves, and an error found at 100 percent is found by every customer. Go in stages and define the rollback before the first one.
3. **"Remove the old model from the configuration as soon as the new one is live."** It is tempting because it keeps the code tidy. It fails because the old model is the rollback, and it stays so until its own retirement date. Keep it configured and tested until that date.

## Quiz

1. A staged roll-out sits at 5 percent of traffic. Only 300 requests have been counted, with no errors, and the minimum is 1,000. What is the next step?
   - **a**: Advance to 25 percent, since a clean record needs no further evidence
   - **b**: Hold where it is until enough volume has arrived to say anything
   - **c**: Roll back to zero, since too few requests counts as a failed stage
   - **d**: Declare the roll-out complete, since no error has been seen so far

2. A staged roll-out is at 25 percent of requests when the error rate passes its limit. What happens next?
   - **a**: Drop to 5 percent and keep the roll-out running, to gather more evidence from a smaller share
   - **b**: Hold at 25 percent and investigate, leaving those customers on the new model while the errors continue to arrive
   - **c**: Advance to 100 percent so that the failure shows up clearly in the data of the whole base
   - **d**: Send all traffic back to the previous model, which stays available until its own retirement

<details>
<summary>Answer key</summary>

1. **b**. A stage with fewer requests than the minimum says nothing yet, so the rule is to hold at the stage. *a* is ruled out because "A stage with fewer requests than the minimum (1,000 in the example) says nothing yet". *c* is ruled out because the share goes back to zero only when errors pass the limit: "Over it, the share goes back to zero". *d* is ruled out because a roll-out ends with "One stage up, and `complete` at 100 percent".
2. **d**. The limit is passed, so the share goes back to zero and the previous model, still configured, takes the traffic. *b* is ruled out because "Holding at the stage while errors continue leaves the share of customers who see them where it is". *c* is ruled out because "Advancing to 100 percent to see the failure in the full data makes every customer the sample". *a* is ruled out because "Reducing to a smaller share keeps part of the damage on the path".

</details>

## Module quiz

This quiz covers both pages of the module.

1. The new model costs about a third more per answer and fixes the refund errors. The product owner asks whether to move. Which message fits the architect's role?
   - **a**: Recommend staying on the old model, because a higher cost is reason enough to avoid the whole change
   - **b**: Recommend the move, because the newer model is the one that the vendor has recommended as the replacement
   - **c**: State the trade: what improves, what the rise takes from the budget, and the date that forces the choice
   - **d**: Defer the decision until the old model retires, so that the choice is made by the calendar instead

2. After a migration, the parser reads the wrong field because the first content block of a reply is thinking text. Which step was missed?
   - **a**: Lowering max_tokens so that the thinking is cut off before the text of the reply begins
   - **b**: Choosing the part of the response by its declared type and not by its position in the list
   - **c**: Turning thinking off by sending the disabled setting, which the new model accepts as before
   - **d**: Dropping the first block of every reply before parsing, since it always holds only thinking

3. On the day the new model went live, the team deleted the old model's id from its configuration to keep it tidy. Two days later errors spike. What did the cleanup take away?
   - **a**: Nothing, since the gate already approved the new model and a rollback is no longer needed
   - **b**: The record of which key called which model, since only the configuration holds it
   - **c**: The new cost baseline, which can only be computed while the old id stays configured
   - **d**: The way back, which needs the previous one kept in place and tested until its own retirement

<details>
<summary>Answer key</summary>

1. **c**. The decision is a trade, and the architect states it with its date. *b* is ruled out because "A recommended replacement is a starting point, not a decision". *a* is ruled out because "A rise in cost is a number to put before the owner, not a verdict". *d* is ruled out because "Waiting for the retirement date hands the decision to the calendar, and gives up the rollback".
2. **b**. A reply may begin with thinking blocks, so the parser reads by type. *a* is ruled out because "`max_tokens` covers thinking plus text, and thinking tokens are billed as output tokens". *c* is ruled out because the guide lists `thinking: {"type": "disabled"}` among the "five settings that return a 400 error". *d* is ruled out because "A reply need not begin with thinking text either, so a rule that always drops the first block loses real text".
3. **d**. The page says "the old model is the rollback, and it stays so until its own retirement date". *a* is ruled out because "Passing a suite is a prediction about traffic, and traffic is where it is tested". *b* is ruled out because the Console export gives "usage broken down by API key and model", so the record does not live in the configuration. *c* is ruled out because the baseline is reset by the rule "Recount tokens and re-baseline cost" on the replacement, which needs no old id.

</details>
