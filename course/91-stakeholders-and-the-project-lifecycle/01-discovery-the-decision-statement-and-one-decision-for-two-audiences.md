# Discovery, the record and one decision for two audiences

**Level:** Architect Professional · **Module 91:** Stakeholders and the project lifecycle · **Page 1 of 2**
**Exams:** P6, P1

**After this page you can** run a discovery that ends in numbers and an owner instead of adjectives, write the decision statement a design aims at, compare at least four options and recommend the cheapest one that meets the service levels, tell one decision in the words a sponsor decides with and in the figures an engineer builds with, and put both in one design record with a fixed set of sections.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, domain 6), and by running the example and the practice offline in the course container. The documentation pages of the Claude platform and of Claude Code give no guidance on discovery, stakeholders or design records, and nothing on this page claims that they do. Nothing here calls a model, and the figures are invented for a utility's billing-dispute assistant. This page deepens module 12 (from a business need to a testable spec) and module 79 (the decision statement and the break-even accuracy) to a decision that other people must approve and build. Service levels, feedback and the lifecycle are the second page.

> **Exam guide and current product.** *What the guide states:* domain 6 asks the candidate to "Conduct structured discovery and requirement gathering", to "Communicate architectural decisions and trade-offs" and to "Document architectures and provide implementation guidance". *What the current product's documentation says (pages read 2026-10-04):* nothing on these tasks; they concern people and not the platform. The structure below (the discovery questions, the eight sections of the record, the limit of 80 words for the sponsor) is the course's own design, taught as the exam's strategy: the guide names the tasks and does not give a template.

## Why it matters

An engineer designs a billing-dispute assistant for three months and brings the result to the sponsor in a forty-slide deck. The sponsor asks one question, "what do you want from me?", and the deck does not say. The engineer, for their part, finds out in the build that "fast" meant two seconds to the agents and that the credit decisions cost 250 when wrong, which nobody had written down. Both failures come from the same place: the facts were in the room and not in a document. The exam treats communication as a technical skill, and asks what a discovery must produce, how a trade-off is told and what a design record holds.

## The idea

### Discovery ends in numbers and an owner

Discovery is a set of questions with answers that can be tested. "Fast", "accurate" and "safe" are not answers, because nobody can fail them. A useful discovery asks, and writes down, each of these.

- **Who waits, and for how long?** The billing agent on screen waits, so the answer must come within 2 seconds at the 95th percentile; a batch job would allow minutes.
- **How many, how often?** About 3,000 disputes a day, with a peak. This sizes the rate limits and the review staff (module 84).
- **What does an error cost, and what does a check cost?** A wrong credit about 250; a check by a person 5. Two numbers decide where a person belongs (module 79).
- **Who is accountable?** A role that can be telephoned: the billing operations manager. If the model prepares and a person decides, the record says so.
- **What exists today?** The cost and quality of the current way (315,000 a month with people alone). Without a baseline no saving can be shown.
- **What counts as done?** A measure, a threshold, an owner: availability of at least 99.5 percent, measured per month by the platform lead.
- **What is out of bounds?** Data that may not leave a region, actions that need a person (page 1 of module 90).

The output is the **decision statement** of module 79 plus a table of facts. A requirement is complete when it has a number, a way to measure it and a person who owns it; a requirement missing one of the three goes back to the stakeholder as a question.

### Options, and the rule for the recommendation

A design record that shows one option is an announcement. The record compares at least four, including the ones that are obviously wrong, because the reader's first question is "why not the simple thing?". For the dispute assistant: people decide every dispute; the model decides every dispute; the model drafts and a person decides every item; the model drafts and a person checks only what it is unsure about. Each has a monthly cost, whether it meets the service levels, a status and a reason.

- **Exactly one is recommended.** Two recommended options push the choice back to the reader.
- **The recommended option is the cheapest one that meets the service levels.** An option that misses a service level is not a candidate, however cheap, and among those that meet them the cheapest wins; "the newest" and "the most capable" are not criteria.
- **A rejected option has a reason in a sentence.** "Too dear" tells the reader nothing; "the most expensive option and the slowest to answer" can be argued with.

### The trade-off, told

The architect's job is to state what a decision trades. Four things go in, in this order: what improves, what it costs, what is at risk and what is asked. For the recommended design: the cost falls from 315,000 to 80,000 a month; it gives up speed in the credit slice, where a person must approve; it is exposed to confident wrong answers, which an independent check against the source reduces and does not remove; and the decision asked is to approve a pilot. Leaving out the downside is the common failure, and a sponsor who finds it later stops trusting the numbers before it.

### Two audiences, the same facts

The sponsor decides and the engineer builds. They need the same facts in different words, and one document serves both when it is built in layers.

- **For the sponsor:** money, risk and one decision, in plain words and at most 80 words: the monthly cost of the recommendation against the baseline, the weakest answers in terms of "right N times in 100" and what each wrong one costs, who decides them, and the decision asked. No percentages of percentages, no model names.
- **For the engineer:** the numbers that produced it: the break-even accuracy, the table by segment, the service levels with their owners, the pilot assumptions and stop triggers.

The example prints both from the same figures. The sponsor line says the design costs 80,000 a month against 315,000 for people alone, a saving of 235,000, that credit answers are right 63 in 100 and cost 250 each when wrong, so a person decides them, and asks to approve the pilot. The engineer line gives the same facts as `key=value` pairs. The two never disagree because both come from one function over one set of numbers.

### The design record

The record is the document that carries the decision, and a fixed shape makes it readable and checkable. The practice's record has eight sections, in this order: the summary for the sponsor; the decision statement; the options considered; the recommendation and trade-offs; accuracy by segment; service levels; pilot to scale; hand-off and monitoring. The summary is first because the sponsor stops reading early. The rest is for the engineer, and the last two sections are the second page. A record is graded by a rubric because its failures are structural: a missing section, a recommendation that is not the cheapest, a service level with no owner.

### The example

The example is the brief and the figures behind it. It has the whole-percent rounding that rounds halves up, the break-even accuracy (98 for an error cost of 250 and a check cost of 5, 91 for 60 and 5, 58 for 12 and 5), the report by segment with the costliest first and whether a person checks it, the lines for the service levels (the second page) and the two briefs. Each segment uses its own error cost here; the practice uses the case's 250 for its single rule. It ran offline in every language.

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

The practice is in [`exercises/91-stakeholders-and-the-project-lifecycle`](../../exercises/91-stakeholders-and-the-project-lifecycle/unit-01/practice-1/statement.md). The draft record recommends the newest option and not the cheapest, promises a latency the agents cannot live with, lists one segment and one assumption and leaves the sponsor nothing to decide. You write the record, and the tests grade it against a rubric. It is graded in Python, TypeScript, Java and Kotlin; the statement lists nine cases, each saying what you should see.

## Traps

1. **"Recommend the most capable option; it is the safest choice."** It is tempting because capability sounds like quality and nobody is blamed for choosing it. The exam rejects it because the recommendation is the cheapest option that meets the service levels; capability beyond the requirement is cost without a requirement behind it.
2. **"Show the sponsor the full analysis and let them find the decision."** It is tempting because completeness looks like rigour. The exam rejects it because a sponsor decides and an engineer builds; the summary is plain, short, and ends with the one decision asked, and the analysis follows for the people who need it.
3. **"Describe the benefits and leave out what the design gives up."** It is tempting because a clean case is easier to approve. The exam rejects it because a trade-off has a cost side, and a sponsor who finds the omission later discounts everything else; state what improves, what it costs, what is at risk and what is asked.

## Quiz

1. Scenario: Priya compares four designs for a call-centre assistant. Two of them meet every service level, one at 90,000 a month and one at 140,000. A third costs 40,000 but misses the latency target. Which does the record recommend?
   - **a**: The cheaper of the two that qualify, since price decides among them
   - **b**: The 140,000 design, because the larger budget leaves room to grow beyond the target
   - **c**: The 40,000 design, because it is the cheapest of the four and the target can be tuned later
   - **d**: Both of the designs that meet every level, so that the sponsor can pick the one preferred

2. Scenario: Hugo must brief a sponsor who will decide in one meeting on whether a pilot goes ahead. His draft is six pages of tables, ending with the break-even derivation. What should the first page of the record hold?
   - **a**: The derivation of the break-even, which is the reason for every later number
   - **b**: The table by segment, so the sponsor sees the accuracy figures before anything else
   - **c**: A short summary in plain words, covering the cost, the risk and the decision requested
   - **d**: The list of the model's settings, so the sponsor can confirm the configuration

<details>
<summary>Answer key</summary>

1. **a**. A design must qualify first, and among those that do, the cheapest is recommended. *c* is ruled out because "An option that misses a service level is not a candidate, however cheap". *b* is ruled out because "the newest" and "the most capable" are not criteria, as "the cheapest one that meets the service levels" is. *d* is ruled out because "Two recommended options push the choice back to the reader".
2. **c**. The sponsor stops reading early, so the summary comes first and ends in the decision. *b* is ruled out because the table by segment belongs with "the numbers that produced it" for the engineer. *a* is ruled out because "The summary is first because the sponsor stops reading early". *d* is ruled out because the sponsor's summary has "no percentages of percentages, no model names".

</details>
