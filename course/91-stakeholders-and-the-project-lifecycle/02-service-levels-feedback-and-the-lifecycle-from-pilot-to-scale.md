# Service levels, feedback and the lifecycle from pilot to scale

**Level:** Architect Professional · **Module 91:** Stakeholders and the project lifecycle · **Page 2 of 2**
**Exams:** P6, P4

**After this page you can** write a service level as a target, a measure and an owner, report one honestly at its exact edge, align expectations by segment instead of promising an overall figure, close a feedback loop from reviewers and sponsors into evaluation cases, state the assumptions of a pilot with a test and a stop trigger for each, and hand a design over with an owner, a runbook, monitors and a rollback.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, domain 6), and by running the example and the practice offline in the course container. The documentation pages of the Claude platform and of Claude Code give no guidance on service-level agreements, stakeholder feedback or the stages of a project, and nothing here claims that they do. Nothing here calls a model, and the targets, measurements and counts are invented. This page deepens module 88 (evaluation and the report by segment) and module 89 (the staged roll-out and the rollback) to the part of the work that outlasts the build. The discovery, the options and the record are the first page.

> **Exam guide and current product.** *What the guide states:* domain 6 asks the candidate to "Manage stakeholder feedback loops and expectation alignment (including SLAs)" and to "Support lifecycle phases (discovery, design, handoff, monitoring, iteration)". *What the current product's documentation says (pages read 2026-10-04):* nothing on either task. Anthropic's own service commitments are in its agreements and on its status page, and what a team promises to its own users is a design choice; the service-level form below and the pilot table are the course's design, taught as the exam's strategy, and the targets and triggers are invented.

## Why it matters

A team promises its sponsor "99 percent accuracy" at launch. The measurement behind it is an average over all disputes, and the credit disputes, the ones that cost 250 when wrong, are right 63 times in 100. Nobody lied; the promise was made in a unit the team did not measure by segment. Two months later a sponsor asks why the credits had to be approved by hand, and the answer is a decision that was in the design all along and was never in the expectations. The exam asks how a service level is written, how it is reported when it is missed, and what the lifecycle after the launch looks like.

## The idea

### A service level has three parts

A service level is a **target**, a **measure** and an **owner**. The target has a number and a unit and a direction: a latency is a ceiling (95th percentile of at most 2000 ms), an availability is a floor (at least 99.5 percent of requests answered without error, per month), an accuracy is stated for a named segment (credit at least 98 percent after review). The measure says where the number comes from (trace timings of every answer, a weekly sample read by the quality team) and the owner is a role that can be called when it moves. A target without a measure cannot be reported, and one without an owner is nobody's.

The example reports each one at its edge. The rule has three parts.

- **Met at the limit exactly.** A latency of 2000 ms against a ceiling of 2000 ms is met, and an availability exactly at its floor is met. Writing "below the target" and meaning "at or below" is the edge-case defect that this practice tests.
- **A miss says by how much.** 2150 ms against 2000 ms is "missed by 150 ms", and an availability of 990 per mille against a floor of 995 is "missed by 5 per mille". A bare "failed" starts an argument that a number would have ended.
- **The direction is part of the target.** A ceiling is met at or below it and a floor is met at or above it; mixing them up reports a healthy system as broken or the reverse.

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

**Pilot to scale.** A pilot proves a design under conditions that are kinder than production, and each kindness is an assumption. The record lists them, each with a **test** and a **stop trigger** that has a number in it. The dispute assistant's four: the inputs were typical (sample production disputes and score them by segment; stop if credit accuracy falls below 60 percent on the sample); staff covered the edge cases by hand (count escalations per 100 disputes; stop above 12); capacity was never close (replay peak load against the rate limits; stop at any 429 error at 70 percent of the limit); reviewers kept up (compute reviewer hours from volume and routing; stop when the queue is older than 4 hours). A trigger that says "if it goes wrong" is not a trigger, and an assumption without a test is a hope. The aim is the sponsor's, but the stop trigger is the pilot's own, and it turns that aim into a number. The trigger sits exactly at its number, so a case at the limit and a case one past it behave differently, as the practice tests.

**Hand-off.** The people who run the system are not the people who built it, and the record says who they are: the owner of the service (a role), the owner of the runbook that explains each alert, the monitors (at least two signals, such as refusals, tokens per answer, the share of answers a person flagged and the 95th percentile latency, each compared with a baseline in both directions) and the rollback, which sends every request to the previous model that stays configured and tested until its own retirement date (module 89). Implementation guidance is part of the hand-off and not an extra: a diagram without the runbook leaves the first incident to the person who happens to remember.

**Monitoring and iteration.** The record is reviewed at each stage of the roll-out, and each review is allowed to change the design: a new segment appears, a threshold moves, a model is replaced. Iteration is not a failure of the design; it is the part of the design that was planned for.

### The example

The example is the same as on the first page, and its second half is this page: five lines for the service levels (a latency of 1800 ms met, 2000 ms met at the ceiling, 2150 ms missed by 150 ms, an availability of 997 per mille met and 990 missed by 5), the report by segment with the costliest first and the two briefs. It ran offline in every language.

<!-- example: m91-tradeoff-brief tabs: python,typescript,java,kotlin -->
```python
"""One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.

The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)
from collections import namedtuple

Sla = namedtuple("Sla", "name limit direction unit")
Segment = namedtuple("Segment", "name right total error_cost")


def pct(right, total):
    """Whole percent, halves rounded up, and 0 for no cases."""
    return (200 * right + total) // (2 * total) if total else 0


def break_even(error_cost, review_cost):
    """The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost."""
    return 100 - (-(-100 * review_cost // error_cost))


def sla_line(sla, measured):
    """A service level is met at its limit exactly, and a miss says by how much."""
    log.debug("sla_line input: %r", sla)
    met = measured <= sla.limit if sla.direction == "max" else measured >= sla.limit
    verdict = "met" if met else f"missed by {abs(measured - sla.limit)} {sla.unit}"
    word = "limit" if sla.direction == "max" else "floor"
    return f"{sla.name}: {measured} {sla.unit} against a {word} of {sla.limit} {sla.unit}: {verdict}"


def segment_report(segments, review_cost):
    """The costliest segment first, with its accuracy and whether a person checks it."""
    lines = []
    for s in sorted(segments, key=lambda s: (-s.error_cost, s.name)):
        floor = break_even(s.error_cost, review_cost)
        handling = "auto" if pct(s.right, s.total) >= floor else "reviewed"
        lines.append(f"{s.name}: {pct(s.right, s.total)} percent right, error cost {s.error_cost}, {handling} (break-even {floor})")
    return lines


def brief(audience, design, cost, baseline, weakest, ask):
    """The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them)."""
    if audience == "sponsor":
        return (f"{design} costs {cost:,} a month against {baseline:,} for people alone, a saving of {baseline - cost:,}. "
                f"The weakest answers are {weakest.name}: {pct(weakest.right, weakest.total)} in 100 are right and each wrong one costs {weakest.error_cost}, "
                f"so a person decides them. Decision asked: {ask}.")
    return f"design={design}; cost={cost}; baseline={baseline}; saving={baseline - cost}; weakest={weakest.name} {pct(weakest.right, weakest.total)}% at {weakest.error_cost} an error"


def main():
    slas = [(Sla("p95 latency", 2000, "max", "ms"), 1800), (Sla("p95 latency", 2000, "max", "ms"), 2000), (Sla("p95 latency", 2000, "max", "ms"), 2150),
            (Sla("availability", 995, "min", "per mille"), 997), (Sla("availability", 995, "min", "per mille"), 990)]
    for sla, measured in slas:
        print(sla_line(sla, measured))
    segments = [Segment("status", 98, 100, 12), Segment("credit", 63, 100, 250), Segment("complaint", 91, 100, 60)]
    for line in segment_report(segments, 5):
        print(line)
    weakest = min(segments, key=lambda s: pct(s.right, s.total))
    for audience in ("sponsor", "engineer"):
        print(f"{audience}: {brief(audience, 'Routing by confidence', 80000, 315000, weakest, 'approve the pilot')}")


if __name__ == "__main__":
    main()
```
```text
p95 latency: 1800 ms against a limit of 2000 ms: met
p95 latency: 2000 ms against a limit of 2000 ms: met
p95 latency: 2150 ms against a limit of 2000 ms: missed by 150 ms
availability: 997 per mille against a floor of 995 per mille: met
availability: 990 per mille against a floor of 995 per mille: missed by 5 per mille
credit: 63 percent right, error cost 250, reviewed (break-even 98)
complaint: 91 percent right, error cost 60, auto (break-even 91)
status: 98 percent right, error cost 12, auto (break-even 58)
sponsor: Routing by confidence costs 80,000 a month against 315,000 for people alone, a saving of 235,000. The weakest answers are credit: 63 in 100 are right and each wrong one costs 250, so a person decides them. Decision asked: approve the pilot.
engineer: design=Routing by confidence; cost=80000; baseline=315000; saving=235000; weakest=credit 63% at 250 an error
```
```typescript
import { logger } from "./logger.ts";
const log = logger("tradeoff_brief");
/**
 * One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.
 *
 * The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
 */
export type Sla = { name: string; limit: number; direction: string; unit: string };
export type Segment = { name: string; right: number; total: number; errorCost: number };

const group = (n: number): string => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ",");

/** Whole percent, halves rounded up, and 0 for no cases. */
export function pct(right: number, total: number): number {
  return total ? Math.floor((200 * right + total) / (2 * total)) : 0;
}

/** The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost. */
export function breakEven(errorCost: number, reviewCost: number): number {
  return 100 - Math.ceil((100 * reviewCost) / errorCost);
}

/** A service level is met at its limit exactly, and a miss says by how much. */
export function slaLine(sla: Sla, measured: number): string {
  log.debug("slaLine input", sla);
  const met = sla.direction === "max" ? measured <= sla.limit : measured >= sla.limit;
  const verdict = met ? "met" : `missed by ${Math.abs(measured - sla.limit)} ${sla.unit}`;
  const word = sla.direction === "max" ? "limit" : "floor";
  return `${sla.name}: ${measured} ${sla.unit} against a ${word} of ${sla.limit} ${sla.unit}: ${verdict}`;
}

/** The costliest segment first, with its accuracy and whether a person checks it. */
export function segmentReport(segments: Segment[], reviewCost: number): string[] {
  const sorted = [...segments].sort((a, b) => b.errorCost - a.errorCost || (a.name < b.name ? -1 : a.name > b.name ? 1 : 0));
  return sorted.map((s) => {
    const floor = breakEven(s.errorCost, reviewCost);
    const handling = pct(s.right, s.total) >= floor ? "auto" : "reviewed";
    return `${s.name}: ${pct(s.right, s.total)} percent right, error cost ${s.errorCost}, ${handling} (break-even ${floor})`;
  });
}

/** The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them). */
export function brief(audience: string, design: string, cost: number, baseline: number, weakest: Segment, ask: string): string {
  if (audience === "sponsor") {
    return `${design} costs ${group(cost)} a month against ${group(baseline)} for people alone, a saving of ${group(baseline - cost)}. ` +
      `The weakest answers are ${weakest.name}: ${pct(weakest.right, weakest.total)} in 100 are right and each wrong one costs ${weakest.errorCost}, ` +
      `so a person decides them. Decision asked: ${ask}.`;
  }
  return `design=${design}; cost=${cost}; baseline=${baseline}; saving=${baseline - cost}; weakest=${weakest.name} ${pct(weakest.right, weakest.total)}% at ${weakest.errorCost} an error`;
}

function main(): void {
  const latency: Sla = { name: "p95 latency", limit: 2000, direction: "max", unit: "ms" };
  const availability: Sla = { name: "availability", limit: 995, direction: "min", unit: "per mille" };
  const slas: [Sla, number][] = [[latency, 1800], [latency, 2000], [latency, 2150], [availability, 997], [availability, 990]];
  for (const [sla, measured] of slas) console.log(slaLine(sla, measured));
  const segments: Segment[] = [{ name: "status", right: 98, total: 100, errorCost: 12 }, { name: "credit", right: 63, total: 100, errorCost: 250 }, { name: "complaint", right: 91, total: 100, errorCost: 60 }];
  for (const line of segmentReport(segments, 5)) console.log(line);
  const weakest = segments.reduce((a, b) => (pct(b.right, b.total) < pct(a.right, a.total) ? b : a));
  for (const audience of ["sponsor", "engineer"]) console.log(`${audience}: ${brief(audience, "Routing by confidence", 80000, 315000, weakest, "approve the pilot")}`);
}

if (import.meta.main) main();
```
```text
p95 latency: 1800 ms against a limit of 2000 ms: met
p95 latency: 2000 ms against a limit of 2000 ms: met
p95 latency: 2150 ms against a limit of 2000 ms: missed by 150 ms
availability: 997 per mille against a floor of 995 per mille: met
availability: 990 per mille against a floor of 995 per mille: missed by 5 per mille
credit: 63 percent right, error cost 250, reviewed (break-even 98)
complaint: 91 percent right, error cost 60, auto (break-even 91)
status: 98 percent right, error cost 12, auto (break-even 58)
sponsor: Routing by confidence costs 80,000 a month against 315,000 for people alone, a saving of 235,000. The weakest answers are credit: 63 in 100 are right and each wrong one costs 250, so a person decides them. Decision asked: approve the pilot.
engineer: design=Routing by confidence; cost=80000; baseline=315000; saving=235000; weakest=credit 63% at 250 an error
```
```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.
 *
 * The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
 */
public class TradeoffBrief {
    private static final System.Logger LOG = System.getLogger(TradeoffBrief.class.getName());
    record Sla(String name, int limit, String direction, String unit) {}

    record Segment(String name, int right, int total, int errorCost) {}

    record Measured(Sla sla, int value) {}

    static String group(long n) {
        return String.format(java.util.Locale.US, "%,d", n);
    }

    /** Whole percent, halves rounded up, and 0 for no cases. */
    static int pct(int right, int total) {
        return total == 0 ? 0 : (200 * right + total) / (2 * total);
    }

    /** The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost. */
    static int breakEven(int errorCost, int reviewCost) {
        return 100 - (100 * reviewCost + errorCost - 1) / errorCost;
    }

    /** A service level is met at its limit exactly, and a miss says by how much. */
    static String slaLine(Sla sla, int measured) {
        LOG.log(System.Logger.Level.DEBUG, "slaLine input: {0}", sla);
        boolean met = sla.direction().equals("max") ? measured <= sla.limit() : measured >= sla.limit();
        String verdict = met ? "met" : "missed by " + Math.abs(measured - sla.limit()) + " " + sla.unit();
        String word = sla.direction().equals("max") ? "limit" : "floor";
        return sla.name() + ": " + measured + " " + sla.unit() + " against a " + word + " of " + sla.limit() + " " + sla.unit() + ": " + verdict;
    }

    /** The costliest segment first, with its accuracy and whether a person checks it. */
    static List<String> segmentReport(List<Segment> segments, int reviewCost) {
        List<Segment> sorted = new ArrayList<>(segments);
        sorted.sort(Comparator.comparingInt((Segment s) -> -s.errorCost()).thenComparing(Segment::name));
        List<String> lines = new ArrayList<>();
        for (Segment s : sorted) {
            int floor = breakEven(s.errorCost(), reviewCost);
            String handling = pct(s.right(), s.total()) >= floor ? "auto" : "reviewed";
            lines.add(s.name() + ": " + pct(s.right(), s.total()) + " percent right, error cost " + s.errorCost() + ", " + handling + " (break-even " + floor + ")");
        }
        return lines;
    }

    /** The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them). */
    static String brief(String audience, String design, int cost, int baseline, Segment weakest, String ask) {
        if (audience.equals("sponsor")) {
            return design + " costs " + group(cost) + " a month against " + group(baseline) + " for people alone, a saving of " + group(baseline - cost) + ". "
                + "The weakest answers are " + weakest.name() + ": " + pct(weakest.right(), weakest.total()) + " in 100 are right and each wrong one costs " + weakest.errorCost() + ", "
                + "so a person decides them. Decision asked: " + ask + ".";
        }
        return "design=" + design + "; cost=" + cost + "; baseline=" + baseline + "; saving=" + (baseline - cost) + "; weakest=" + weakest.name() + " " + pct(weakest.right(), weakest.total()) + "% at " + weakest.errorCost() + " an error";
    }

    public static void main(String[] args) {
        Sla latency = new Sla("p95 latency", 2000, "max", "ms");
        Sla availability = new Sla("availability", 995, "min", "per mille");
        List<Measured> slas = List.of(new Measured(latency, 1800), new Measured(latency, 2000), new Measured(latency, 2150), new Measured(availability, 997), new Measured(availability, 990));
        for (Measured m : slas) System.out.println(slaLine(m.sla(), m.value()));
        List<Segment> segments = List.of(new Segment("status", 98, 100, 12), new Segment("credit", 63, 100, 250), new Segment("complaint", 91, 100, 60));
        for (String line : segmentReport(segments, 5)) System.out.println(line);
        Segment weakest = segments.get(0);
        for (Segment s : segments) if (pct(s.right(), s.total()) < pct(weakest.right(), weakest.total())) weakest = s;
        for (String audience : List.of("sponsor", "engineer")) System.out.println(audience + ": " + brief(audience, "Routing by confidence", 80000, 315000, weakest, "approve the pilot"));
    }
}
```
```text
p95 latency: 1800 ms against a limit of 2000 ms: met
p95 latency: 2000 ms against a limit of 2000 ms: met
p95 latency: 2150 ms against a limit of 2000 ms: missed by 150 ms
availability: 997 per mille against a floor of 995 per mille: met
availability: 990 per mille against a floor of 995 per mille: missed by 5 per mille
credit: 63 percent right, error cost 250, reviewed (break-even 98)
complaint: 91 percent right, error cost 60, auto (break-even 91)
status: 98 percent right, error cost 12, auto (break-even 58)
sponsor: Routing by confidence costs 80,000 a month against 315,000 for people alone, a saving of 235,000. The weakest answers are credit: 63 in 100 are right and each wrong one costs 250, so a person decides them. Decision asked: approve the pilot.
engineer: design=Routing by confidence; cost=80000; baseline=315000; saving=235000; weakest=credit 63% at 250 an error
```
```kotlin
/**
 * One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.
 *
 * The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
 */

private val log = System.getLogger("tradeoff_brief")

data class Sla(val name: String, val limit: Int, val direction: String, val unit: String)

data class Segment(val name: String, val right: Int, val total: Int, val errorCost: Int)

fun group(n: Long): String = String.format(java.util.Locale.US, "%,d", n)

/** Whole percent, halves rounded up, and 0 for no cases. */
fun pct(right: Int, total: Int): Int = if (total == 0) 0 else (200 * right + total) / (2 * total)

/** The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost. */
fun breakEven(errorCost: Int, reviewCost: Int): Int = 100 - (100 * reviewCost + errorCost - 1) / errorCost

/** A service level is met at its limit exactly, and a miss says by how much. */
fun slaLine(sla: Sla, measured: Int): String {
    log.log(System.Logger.Level.DEBUG, "slaLine input: {0}", sla)
    val met = if (sla.direction == "max") measured <= sla.limit else measured >= sla.limit
    val verdict = if (met) "met" else "missed by ${Math.abs(measured - sla.limit)} ${sla.unit}"
    val word = if (sla.direction == "max") "limit" else "floor"
    return "${sla.name}: $measured ${sla.unit} against a $word of ${sla.limit} ${sla.unit}: $verdict"
}

/** The costliest segment first, with its accuracy and whether a person checks it. */
fun segmentReport(segments: List<Segment>, reviewCost: Int): List<String> =
    segments.sortedWith(compareBy<Segment>({ -it.errorCost }, { it.name })).map { s ->
        val floor = breakEven(s.errorCost, reviewCost)
        val handling = if (pct(s.right, s.total) >= floor) "auto" else "reviewed"
        "${s.name}: ${pct(s.right, s.total)} percent right, error cost ${s.errorCost}, $handling (break-even $floor)"
    }

/** The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them). */
fun brief(audience: String, design: String, cost: Int, baseline: Int, weakest: Segment, ask: String): String {
    if (audience == "sponsor") {
        return "$design costs ${group(cost.toLong())} a month against ${group(baseline.toLong())} for people alone, a saving of ${group((baseline - cost).toLong())}. " +
            "The weakest answers are ${weakest.name}: ${pct(weakest.right, weakest.total)} in 100 are right and each wrong one costs ${weakest.errorCost}, " +
            "so a person decides them. Decision asked: $ask."
    }
    return "design=$design; cost=$cost; baseline=$baseline; saving=${baseline - cost}; weakest=${weakest.name} ${pct(weakest.right, weakest.total)}% at ${weakest.errorCost} an error"
}

fun main() {
    val latency = Sla("p95 latency", 2000, "max", "ms")
    val availability = Sla("availability", 995, "min", "per mille")
    for ((sla, measured) in listOf(latency to 1800, latency to 2000, latency to 2150, availability to 997, availability to 990)) println(slaLine(sla, measured))
    val segments = listOf(Segment("status", 98, 100, 12), Segment("credit", 63, 100, 250), Segment("complaint", 91, 100, 60))
    for (line in segmentReport(segments, 5)) println(line)
    val weakest = segments.minByOrNull { pct(it.right, it.total) }!!
    for (audience in listOf("sponsor", "engineer")) println("$audience: ${brief(audience, "Routing by confidence", 80000, 315000, weakest, "approve the pilot")}")
}
```
```text
p95 latency: 1800 ms against a limit of 2000 ms: met
p95 latency: 2000 ms against a limit of 2000 ms: met
p95 latency: 2150 ms against a limit of 2000 ms: missed by 150 ms
availability: 997 per mille against a floor of 995 per mille: met
availability: 990 per mille against a floor of 995 per mille: missed by 5 per mille
credit: 63 percent right, error cost 250, reviewed (break-even 98)
complaint: 91 percent right, error cost 60, auto (break-even 91)
status: 98 percent right, error cost 12, auto (break-even 58)
sponsor: Routing by confidence costs 80,000 a month against 315,000 for people alone, a saving of 235,000. The weakest answers are credit: 63 in 100 are right and each wrong one costs 250, so a person decides them. Decision asked: approve the pilot.
engineer: design=Routing by confidence; cost=80000; baseline=315000; saving=235000; weakest=credit 63% at 250 an error
```
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
   - **c**: At risk, since a figure on the ceiling is neither met nor missed
   - **d**: Missed, since one answer in twenty took longer than 2000 ms

2. Scenario: Vera is writing the pilot table for a billing assistant and offers the stop trigger "if the quality is worse than we hoped". What is wrong with it?
   - **a**: It should name the reviewer who decides when quality is poor enough
   - **b**: It states a quality aim, which belongs to the sponsor and not the pilot
   - **c**: It names quality, which a pilot has no data to judge before launch
   - **d**: It holds no number, so whether it was reached is a matter of opinion

<details>
<summary>Answer key</summary>

1. **b**. A service level is met at its limit exactly. *a* is ruled out because "A latency of 2000 ms against a ceiling of 2000 ms is met". *c* is ruled out because "a service level is met at its limit exactly and missed beyond it", which leaves no third state. *d* is ruled out because the ceiling is the "95th percentile of at most 2000 ms", so the slowest one in twenty may lie above it.
2. **d**. A trigger needs a number to be reached. *a* is ruled out because a trigger worded as "if it goes wrong" "is not a trigger", whoever decides. *c* is ruled out because the pilot's own test is to "sample production disputes and score them by segment", with a trigger on credit accuracy. *b* is ruled out because "the stop trigger is the pilot's own", so the aim may be the sponsor's while the trigger is not.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: Noor tells a sponsor that an assistant is "98 percent accurate overall". The system answers three kinds of dispute, and the one that costs 250 per error is right 63 times in 100. What should the record say to the sponsor?
   - **a**: That the costly kind will reach 98 percent by launch, so the figure still holds
   - **b**: That the overall figure stands, since the costly kind is a small share of the traffic
   - **c**: That results go by segment, with a person deciding each of those until they improve
   - **d**: That the figure will be re-measured once per quarter, with the same overall method

2. Scenario: Lars hands a finished assistant to an operations team with an architecture diagram and a list of settings. The first alert fires on a Saturday and nobody knows who should act on it. Which part of the hand-off was missing?
   - **a**: A longer pilot, so that more alerts would have fired before the launch
   - **b**: A runbook kept by a named service owner, saying who responds and how
   - **c**: A monthly report to the sponsor listing the alerts of the previous period
   - **d**: A second diagram showing the alert thresholds drawn on the architecture

3. Scenario: A reviewer corrects forty answers in a week, and the notes of the sponsor review record them. Nothing else changes, and the same errors appear the next week. Which step would have closed the loop?
   - **a**: Sending the notes to the engineers with a request to read them before the next release
   - **b**: Counting the corrections per 100 disputes and comparing the count with a baseline
   - **c**: Moving the review from weekly to daily so that the notes are fresher
   - **d**: Turning each fix into a labelled evaluation case that each later release is run against

<details>
<summary>Answer key</summary>

1. **c**. The promise is made per segment, and a person decides the costly one. *b* is ruled out because "an average hides the segments that cost the most", whatever the traffic share. *a* is ruled out because the page says to "Promise what you measure and measure what you promise", and a future figure is not yet measured. *d* is ruled out because a promise "is made in the unit that is measured", which here is the segment, so re-measuring the same overall figure repeats the same blind spot.
2. **b**. The record names an owner and a runbook for each alert. *a* is ruled out because "the people who run the system need an owner, a runbook, monitors and a rollback on day one", and a longer pilot adds none of them. *c* is ruled out because an alert goes to "the owner of the runbook that explains each alert", and a monthly report to the sponsor arrives weeks after the Saturday. *d* is ruled out because "a diagram without the runbook leaves the first incident to the person who happens to remember", and a second diagram is still a diagram.
3. **d**. A correction kept as a labelled evaluation case is run against every later release, so the same error cannot return unseen. *b* is ruled out because a count against a baseline is a signal, while the reviewer's fix must become a case "so the same failure is tested after every change". *c* is ruled out because "A loop is closed when a change goes back out through the gate of module 89", and a fresher review sends no change. *a* is ruled out because "A stakeholder review that produces notes and no cases has not closed anything", and notes sent on are still notes.

</details>
