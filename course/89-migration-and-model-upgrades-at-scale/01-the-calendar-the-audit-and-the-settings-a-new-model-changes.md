# The calendar, the audit and the settings a new model changes

**Level:** Architect Professional · **Module 89:** Migration and model upgrades at scale · **Page 1 of 2**
**Exams:** P4, P6

**After this page you can** read a model's lifecycle and plan from its retirement date, find every place a deprecated model is still used, list the request settings a newer model refuses and the behaviours it changes without an error, and say what the change does to cost and to what you must measure again.

Checked on 2026-10-04 against the Claude documentation pages "Model deprecations" and "Migration guide" for Claude Sonnet 5.5, the Claude Certified Architect - Professional exam guide (version 1.0, domains 4 and 6), and by running the example offline in the course container. Nothing here called a model, and the cases, costs and timings in the example are invented; the model names and dates are those the documentation listed that day, and they will change. This page deepens module 18 (model choice, cost and migration, including pinned model ids) and module 42 (evaluation) to a fleet of applications. The gate, the staged roll-out, the rollback and the practice are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 4 asks the candidate to "Conduct A/B testing and iterative improvements", and domain 6 to "Support lifecycle phases (discovery, design, handoff, monitoring, iteration)" and to "Communicate architectural decisions and trade-offs". It names no model and no setting. *What the current product does (documentation read 2026-10-04):* models move through the states active, legacy, deprecated and retired, with "at least 60 days' notice before model retirement for publicly released models"; the migration guide for Claude Sonnet 5.5 lists five request settings that return a 400 error on that model. On the exam, answer with the discipline: a calendar, an audit, a gate and a way back. In a design, name the model and the date you have checked, since both change. The dates and the settings below were true on the day the documentation was read, and are an example of the method more than a reference.

## Why it matters

A company runs forty applications on three generations of Claude models. One day an email says that a model several of them use will retire in two months. The platform team has no list of which application uses which model id, three applications set a temperature and a forced tool choice that the replacement refuses, and the finance lead has budgeted next quarter from the old model's token counts. Each problem is small, and together they decide whether the migration is an ordinary change or an outage. The exam asks what to do first, and what a safe migration contains.

## The idea

### The lifecycle and the calendar

The documentation defines four states. **Active:** "The model is fully supported and recommended for use." **Legacy:** "The model will no longer receive updates and may be deprecated in the future." **Deprecated:** "The model is still functional but no longer recommended. Anthropic provides a recommended replacement and assigns a retirement date." **Retired:** "The model is no longer available for use. Requests to retired models will fail." It adds a warning: "Deprecated models are likely to be less reliable than active models."

Three facts shape the plan.

- **There is notice, and it is finite.** Anthropic notifies customers with active deployments, "providing at least 60 days' notice before model retirement for publicly released models". Sixty days is the shortest warning, and a migration of forty applications can take longer than that. A date also has a certainty: the table can say "not sooner than" for a model that is still active, which marks a date that may move later and is never a date to wait for.
- **Platforms differ.** The page's dates apply to Anthropic-operated platforms. "Partner-operated platforms (Amazon Bedrock and Google Cloud) set their own retirement schedules, so a model's lifecycle status and dates can differ." A multi-platform estate keeps one calendar per platform.
- **A replacement is a starting point.** The table names a recommended replacement for each deprecated model. That is where to begin testing, and the decision is yours to make on your own tasks: "consider thorough testing of your applications with the new models well before the retirement date."

The example turns the calendar into a list. Each model gets its days left and a level: `retired` below zero, `urgent` up to 14 days, `migrate now` up to 60 days, which is the notice window, and `watch` beyond. The levels are the course's choice of thresholds and not the documentation's; the nearest retirement comes first.

### The audit: where is the old model used?

You cannot migrate what you cannot find. The documentation describes an audit of usage: on the Usage page of the Claude Console, export a CSV, and the file shows "usage broken down by API key and model". That finds the keys; the keys point to the applications. Two habits make the audit last. Pin the model id in one configuration place and not in forty call sites (module 18), so that a migration changes a value and not a code base. And keep the calendar next to the audit, so that an alert is raised from the date, not from an email that someone read.

### Settings a new model refuses

Some changes are loud. On Claude Sonnet 5.5 the migration guide lists five settings that return a 400 error: "thinking budgets, sampling parameters, assistant prefill, forced tool choice, and `thinking: {"type": "disabled"}`". A loud change is the easy kind: the first test run shows it, and a table maps each setting to its replacement. The example's `migrate_request` is that table as code, and it names each change so that a reviewer can read what happened to a request.

| Setting that returns an error | What replaces it on that model |
|---|---|
| `temperature`, `top_p`, `top_k` (sampling parameters) | Remove them: a non-default value returns the error |
| A thinking budget | An effort level, found by a sweep, with adaptive thinking as the default |
| `thinking` set to disabled | The lowest thinking setting, `between_tools` (accepted at `low`, `medium` and `high` effort) |
| A forced tool choice (`any` or a named tool) | `auto`, with strict tools |
| An assistant prefill | Remove it and state the format in the instructions |

A 400 on one of these is a refusal of the shape of the request. It is not a timeout, a cache that holds the old model, or a value out of range: the setting itself is no longer accepted at a non-default value. The fix is to stop sending it, not to change its value.

### Changes that throw nothing

The quiet changes cost more, because no test fails. The same guide lists several for this model. First, "a request with no `thinking` field runs with adaptive thinking", where the older models ran without thinking; so the first content block of a reply may be thinking text, and code that reads `content[0].text` breaks, and the rule is to "Read content blocks by `type`". A reply need not begin with thinking text either, so a rule that always drops the first block loses real text. The guide says to "Pass `thinking` blocks back unchanged" in tool loops, and to keep conversations append-only. `max_tokens` covers thinking plus text, and thinking tokens are billed as output tokens. Refusals need handling, with a fallback configured.

And the bill moves: "The price per token is higher, and the same text produces more tokens. Recount tokens and re-baseline cost." The checklist ends with the instruction to "Re-run your effort sweep": effort is a setting that was tuned for the old model and means something different on the new one. A rise in cost per answer after a migration is therefore not a billing error. Shortening every prompt until the old budget fits again trades quality for a number that no longer applies, and a budget that cannot change is a decision to stay on a model whose date is fixed. The honest response is to measure usage afresh, set a new baseline, and report the difference (page 2).

### The example

The example holds one calendar, one request and one suite. It prints the retirement calendar for three real model names on 2026-10-04, migrates a request that uses four of the five settings the new model refuses, and then runs the gate and the roll-out steps of the second page. It ran offline in every language, and the output is the same in all four.

<!-- example: m89-rollout-gate tabs: python,typescript,java,kotlin -->
```python
"""Moving a system to a new model at scale: the calendar of retirements, the settings a new model refuses, a gate that a regression suite must pass, and a staged roll-out with a way back.

The cases, costs, timings and counts are invented. The model names and dates are the ones the Claude documentation listed on 2026-10-04; the rules about settings are those of its migration guide for Claude Sonnet 5.5. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)
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
    log.debug("gate input: %r", cases)
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
import { logger } from "./logger.ts";
const log = logger("rollout_gate");
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
  log.debug("gate input", cases);
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
    private static final System.Logger LOG = System.getLogger(RolloutGate.class.getName());
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
        LOG.log(System.Logger.Level.DEBUG, "gate input: {0}", cases);
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

private val log = System.getLogger("rollout_gate")

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
    log.log(System.Logger.Level.DEBUG, "gate input: {0}", cases)
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

## Traps

1. **"Wait for the retirement date; the old model works until then."** It is tempting because nothing is broken today. It fails because the notice is finite, a deprecated model is "likely to be less reliable", and the migration of an estate takes longer than it looks. Start from the date you know, and test the replacement early.
2. **"Change the model id in the configuration and ship."** It is tempting because the id is the only visible difference. It fails because the new model refuses settings the old one accepted, changes defaults without an error, and costs more per answer. Migrate the request, run the suite, and re-baseline cost.
3. **"A 400 after the swap means the new model is flaky; retry."** It is tempting because transient errors are common. It fails because a 400 on a setting is deterministic: the same request will be refused every time. Read the error, find the setting, and stop sending it.

## Quiz

1. A deprecation table lists one of a company's models with a retirement of "not sooner than" a date nine weeks away. How should the team treat that entry?
   - **a**: Plan against it as the earliest cut-off and begin early, since it slips later, not earlier
   - **b**: Wait for the vendor to confirm a final cut-off before spending any effort on the move
   - **c**: Treat it as a fixed cut-off that cannot move, and schedule the whole migration to end on that day
   - **d**: Ignore it until the status changes to retired, because nothing breaks before that point

2. A model's entry in the documentation carries the status legacy. What does that status say?
   - **a**: It has a retirement date and will fail on that day, so requests must already be moved
   - **b**: It is the recommended choice for new work, since only deprecated models are withdrawn
   - **c**: It stops receiving updates and heads toward deprecation, so plan the move before any date is set
   - **d**: It has been retired already, so every request that names it comes back as a failure

<details>
<summary>Answer key</summary>

1. **a**. The table can say "not sooner than" for a model that is still active, which marks a date that may move later. *b* is ruled out because "is never a date to wait for". *c* is ruled out because "marks a date that may move later". *d* is ruled out because "Deprecated models are likely to be less reliable than active models", so nothing is safe until the retired state.
2. **c**. Legacy means "The model will no longer receive updates and may be deprecated in the future." *a* is ruled out because deprecated is the state where Anthropic "provides a recommended replacement and assigns a retirement date". *b* is ruled out because active is the state where "The model is fully supported and recommended for use". *d* is ruled out because "Requests to retired models will fail" describes the retired state, not legacy.

</details>
