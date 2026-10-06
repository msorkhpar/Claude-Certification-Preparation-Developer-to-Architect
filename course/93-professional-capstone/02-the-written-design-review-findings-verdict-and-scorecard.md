# The written design review: findings, verdict and scorecard

**Level:** Architect Professional · **Module 93:** Professional capstone · **Page 2 of 2**
**Exams:** P1, P2, P3, P4, P5, P6, P7

**After this page you can** review a design against a fixed set of rules and write the result as findings with a severity, a domain and a rule; order them so that two reviewers produce the same list; give a verdict of reject, revise or approve; summarise a review as a count per domain; compute the accuracy a design needs from two costs; and say which of the 22 rules a given flaw trips.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, all seven domains) and by running the example and the practice offline in the course container. The rules, severities and thresholds below are this course's own design, taught as the exam's strategy: the guide does not list them, and the product documentation prescribes no review procedure. Nothing here called a model. The review in the practice reads a design given as facts and numbers, and no real system is assessed. The first page is the design that the review is applied to.

> **Exam guide and current product.** *What the guide states:* the Professional exam asks the candidate to "Evaluate" and "Analyze" designs, to "Identify risks, limitations, and failure modes" and to "Communicate architectural decisions and trade-offs". *What the current product's documentation says (pages read 2026-10-04):* the documents behind each rule are named in the module that teaches it. A written review is how an architect applies them under time pressure, and the thresholds (a team worth at least 15 chats, tool definitions of at most 10,000 tokens without deferral, an evaluation set of at least 20 cases, a roll-out of at least three stages, a team of at most 10 without managed settings) come from earlier modules' design values and from documented figures, so they carry that qualification here too.

## Why it matters

A design review that is a conversation produces a list that depends on who is in the room. A written review turns the design into a fixed list of questions, each with a severity, so the same design gets the same findings on Monday and on Friday, a new reviewer can run it, and the sponsor can read the verdict without reading the analysis. The exam asks the reverse of the same skill: given a scenario, which flaw is present, how serious it is and which domain owns it. Writing the rules out once is the best way to see them.

## The idea

### A finding has three parts

Each finding is a **severity**, a **domain** and a **rule**, written `<severity> <domain> <rule>`, for example `high P3 filter-after-ranking`. The severity says what a reviewer does about it.

- **High.** A flaw that can cause harm that cannot be undone, expose data or lose a protected segment. The design is rejected until it is fixed.
- **Medium.** A flaw that weakens a design without breaking it. The design is revised.
- **Low.** A matter to improve. It is recorded and does not hold the design back.

The **verdict** follows from the findings: **reject** if any finding is high, **revise** if none is high and any is medium, and **approve** otherwise, so a design with only low findings is approved. A fixed ladder keeps a pile of small findings from becoming a rejection and a single serious one from hiding in a long list.

### The order is part of the review

Two reviewers must produce the same list. The order is by severity (high, medium, low), then by domain (P1 to P7), then by the rule id alphabetically. A list ordered only by domain buries the high findings in the middle, and a list ordered by whim cannot be compared with last month's. The **scorecard** is a count of findings per domain, in the order P1 to P7, with a zero for a domain that has none. It answers the question a sponsor asks first: where is the weakness?

### The 22 rules

The rules are the lessons of Level 4 written as checks. Each is a fact about the design or a number with a threshold.

| Domain | Rule | Severity | What it says |
|---|---|---|---|
| P1 | `missing-feedback` | high | the design has no feedback loop |
| P1 | `autonomy-without-need` | medium | an agent or a team is used where the path is known |
| P1 | `team-below-price` | medium | a team's value is below 15 chats' worth |
| P2 | `volatile-prefix` | medium | changing text sits in front of the static prompt, so the cache cannot hold |
| P2 | `model-not-measured` | low | the model was chosen without a measurement |
| P3 | `filter-after-ranking` | high | retrieval ranks first and filters by rights after |
| P3 | `stale-index` | high | the index is not replaced when a document changes |
| P3 | `tool-bloat` | medium | tool definitions above 10,000 tokens with no deferral |
| P3 | `agent-rights-only` | high | the agent's rights are used and not the user's |
| P4 | `no-protected-segment` | medium | no segment is protected in the suite |
| P4 | `small-eval-set` | low | fewer than 20 evaluation cases |
| P4 | `no-way-back` | high | no rollback |
| P4 | `big-bang-rollout` | medium | fewer than three roll-out stages |
| P5 | `identifiers-reach-model` | high | identifiers go to the model |
| P5 | `residency-unmet` | high | the region requirement is not met |
| P5 | `audit-keeps-content` | medium | the audit log stores content |
| P5 | `irreversible-without-person` | high | an irreversible action has no person before it |
| P5 | `retention-outside-window` | medium | the days kept lie outside the floor and the ceiling |
| P6 | `no-accountable-owner` | medium | no owner for the service |
| P6 | `sla-without-numbers` | medium | a latency or an availability with no number |
| P6 | `accuracy-unstated` | low | the needed accuracy is not stated |
| P7 | `unmanaged-team-settings` | medium | a team above 10 without managed settings |

Two rules are conditional on a second fact: an agent is only flagged when the path is known, and an irreversible action is only flagged when no person stands before it. A rule that fires without its second fact is a false alarm, and the practice tests each. The `tool-bloat` threshold is this review's own design value: module 86 teaches deferral for ten or more tools or for definitions over 10,000 tokens, and the review keeps only the token figure, flagged when nothing is deferred.

### Thresholds sit at an edge

Each numeric rule has an edge, and the review must treat the edge as the design's friend: a team worth exactly 15 chats passes, tool definitions of exactly 10,000 tokens pass, an evaluation set of exactly 20 cases passes, three stages pass, a retention of exactly the floor or exactly the ceiling passes, and a team of exactly 10 passes without managed settings. One step past any of them is a finding. The practice has a case at every edge and a wrong solution that flips each.

### The accuracy a design needs

The review also computes one number from two costs: the accuracy above which a check no longer pays. It is 100 minus the review cost as a percent of the error cost, with that percent rounded up, and never below 0, with 0 when an error costs nothing. For an error of 250 and a check of 5 it is 98. For 3 and 1 it is 66, and the rounding matters: a third of a hundred is 33.3, which rounds up to 34. A check that costs as much as an error, or more, needs no accuracy, because the check never pays; the result stays 0 and does not go negative.

### Writing the review

A written review is a short document built in the order of the data: the verdict first, then the scorecard, then the findings in their order, each with the evidence from the design and the fix. The sponsor stops at the verdict and the scorecard, and the engineers read the findings. Where the review says `reject`, it names the high findings first, so that the fix list starts with what blocks.

### The example

The example is the claims assistant of the first page, and the review of this page is the process around it: a design that routes by confidence, retrieves by the reader's rights, checks the version, keeps traces without content and gates on a protected segment passes the rules the example demonstrates (`filter-after-ranking`, `stale-index`, `audit-keeps-content`, `irreversible-without-person`, `no-protected-segment`). It ran offline in every language.

<!-- example: m93-claims-assistant tabs: python,typescript,java,kotlin -->
```python
"""A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.

The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
"""
import logging

log = logging.getLogger(__name__)
import re
from collections import namedtuple

Chunk = namedtuple("Chunk", "id doc version text")
Request = namedtuple("Request", "id text allowed consequence quote confidence")
Case = namedtuple("Case", "id segment old_ok new_ok")

CURRENT = {"policy": 3, "contracts": 1}
INDEX = [Chunk("policy-1", "policy", 3, "Claims must be reported within 30 days of the loss."),
         Chunk("policy-2", "policy", 3, "Water damage is covered up to 5,000 per claim."),
         Chunk("contract-9", "contracts", 1, "Partner commission is 12 percent of premiums.")]
STALE_INDEX = [INDEX[0], Chunk("policy-2-old", "policy", 2, "Water damage is covered up to 3,000 per claim."), INDEX[2]]


def tokenise(text):
    """Identifiers become tokens before the text leaves the caller; the map from token to value stays here."""
    vault = {}
    def swap(match):
        value = match.group(0)
        for token, known in vault.items():
            if known == value:
                return token
        token = f"<EMAIL_{len(vault) + 1}>"
        vault[token] = value
        return token
    return re.sub(r"[\w.+-]+@[\w-]+\.[\w.]+", swap, text), vault


def words(text):
    return {w for w in re.findall(r"[a-z]+", text.lower()) if len(w) > 3}


def retrieve(question, allowed, index):
    """The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence."""
    scored = sorted(((-len(words(question) & words(c.text)), c.id, c) for c in index if c.doc in allowed), key=lambda t: t[:2])
    return scored[0][2] if scored and scored[0][0] < 0 else None


def handle(request, index):
    """One request through the chain; the outcome says why a request was held."""
    log.debug("handle input: %r", request)
    sent, _ = tokenise(request.text)
    chunk = retrieve(sent, request.allowed, index)
    if chunk is None:
        outcome = "hold: no evidence"
    elif chunk.version != CURRENT[chunk.doc]:
        outcome = f"hold: stale evidence ({chunk.id} v{chunk.version}, current v{CURRENT[chunk.doc]})"
    elif request.quote not in chunk.text:
        outcome = "hold: unsupported"
    elif request.consequence == "high":
        outcome = "human"
    else:
        outcome = "auto" if request.confidence >= 95 else "review"
    trace = {"request": request.id, "chunk": f"{chunk.id}@v{chunk.version}" if chunk else "none", "outcome": outcome, "chars": len(sent)}
    return sent, trace


def keep(trace):
    """Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere."""
    return trace["outcome"].startswith("hold") or trace["outcome"] == "human"


def release(cases, protected):
    """A change ships only when no protected segment loses an answer and the losses do not outnumber the gains."""
    lost = [c for c in cases if c.old_ok and not c.new_ok]
    gained = [c for c in cases if c.new_ok and not c.old_ok]
    hit = sorted({c.segment for c in lost if c.segment in protected})
    if hit:
        return "no-go: protected segment lost answers: " + ", ".join(hit)
    if len(lost) > len(gained):
        return f"no-go: net loss: lost {len(lost)}, gained {len(gained)}"
    return f"go: lost {len(lost)}, gained {len(gained)}"


def main():
    water = "Water damage is covered up to 5,000 per claim."
    late = "Claims must be reported within 30 days of the loss."
    requests = [
        (Request("r1", "How much does the policy cover for water damage?", {"policy"}, "low", water, 97), INDEX),
        (Request("r2", "How much does the policy cover for water damage?", {"policy"}, "low", water, 97), STALE_INDEX),
        (Request("r3", "Can I get a refund of 400 for water damage?", {"policy"}, "high", water, 99), INDEX),
        (Request("r4", "What is the partner commission?", {"policy"}, "low", water, 99), INDEX),
        (Request("r5", "I reported my claim from jo@example.com, how many days do I have?", {"policy"}, "low", late, 96), INDEX),
    ]
    traces = []
    for request, index in requests:
        sent, trace = handle(request, index)
        traces.append(trace)
        print(f"{request.id}: sent={sent!r}; evidence={trace['chunk']}; outcome={trace['outcome']}")
    print("traces kept:", ", ".join(t["request"] for t in traces if keep(t)))
    cases = [Case(f"s{i}", "status", i > 2, True) for i in range(1, 7)] + [Case(f"f{i}", "refund", True, i != 4) for i in range(1, 5)] + [Case("c1", "complaint", False, True), Case("c2", "complaint", True, True)]
    print("release with refunds protected:", release(cases, {"refund"}))
    fixed = [Case(c.id, c.segment, c.old_ok, True) if c.id == "f4" else c for c in cases]
    print("release after the refund fix:", release(fixed, {"refund"}))


if __name__ == "__main__":
    main()
```
```text
r1: sent='How much does the policy cover for water damage?'; evidence=policy-2@v3; outcome=auto
r2: sent='How much does the policy cover for water damage?'; evidence=policy-2-old@v2; outcome=hold: stale evidence (policy-2-old v2, current v3)
r3: sent='Can I get a refund of 400 for water damage?'; evidence=policy-2@v3; outcome=human
r4: sent='What is the partner commission?'; evidence=none; outcome=hold: no evidence
r5: sent='I reported my claim from <EMAIL_1>, how many days do I have?'; evidence=policy-1@v3; outcome=auto
traces kept: r2, r3, r4
release with refunds protected: no-go: protected segment lost answers: refund
release after the refund fix: go: lost 0, gained 3
```
```typescript
import { logger } from "./logger.ts";
const log = logger("claims_assistant");
/**
 * A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.
 *
 * The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
 */
export type Chunk = { id: string; doc: string; version: number; text: string };
export type Request = { id: string; text: string; allowed: Set<string>; consequence: string; quote: string; confidence: number };
export type Case = { id: string; segment: string; oldOk: boolean; newOk: boolean };
export type Trace = { request: string; chunk: string; outcome: string; chars: number };

export const CURRENT: Record<string, number> = { policy: 3, contracts: 1 };
export const INDEX: Chunk[] = [
  { id: "policy-1", doc: "policy", version: 3, text: "Claims must be reported within 30 days of the loss." },
  { id: "policy-2", doc: "policy", version: 3, text: "Water damage is covered up to 5,000 per claim." },
  { id: "contract-9", doc: "contracts", version: 1, text: "Partner commission is 12 percent of premiums." },
];
export const STALE_INDEX: Chunk[] = [INDEX[0], { id: "policy-2-old", doc: "policy", version: 2, text: "Water damage is covered up to 3,000 per claim." }, INDEX[2]];

/** Identifiers become tokens before the text leaves the caller; the map from token to value stays here. */
export function tokenise(text: string): [string, Record<string, string>] {
  const vault: Record<string, string> = {};
  const sent = text.replace(/[\w.+-]+@[\w-]+\.[\w.]+/g, (value) => {
    for (const [token, known] of Object.entries(vault)) if (known === value) return token;
    const token = `<EMAIL_${Object.keys(vault).length + 1}>`;
    vault[token] = value;
    return token;
  });
  return [sent, vault];
}

function words(text: string): Set<string> {
  return new Set((text.toLowerCase().match(/[a-z]+/g) ?? []).filter((w) => w.length > 3));
}

/** The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence. */
export function retrieve(question: string, allowed: Set<string>, index: Chunk[]): Chunk | null {
  const mine = words(question);
  const scored = index.filter((c) => allowed.has(c.doc)).map((c) => ({ overlap: [...words(c.text)].filter((w) => mine.has(w)).length, chunk: c }));
  scored.sort((a, b) => b.overlap - a.overlap || (a.chunk.id < b.chunk.id ? -1 : a.chunk.id > b.chunk.id ? 1 : 0));
  return scored.length && scored[0].overlap > 0 ? scored[0].chunk : null;
}

/** One request through the chain; the outcome says why a request was held. */
export function handle(request: Request, index: Chunk[]): [string, Trace] {
  log.debug("handle input", request);
  const [sent] = tokenise(request.text);
  const chunk = retrieve(sent, request.allowed, index);
  let outcome: string;
  if (chunk === null) outcome = "hold: no evidence";
  else if (chunk.version !== CURRENT[chunk.doc]) outcome = `hold: stale evidence (${chunk.id} v${chunk.version}, current v${CURRENT[chunk.doc]})`;
  else if (!chunk.text.includes(request.quote)) outcome = "hold: unsupported";
  else if (request.consequence === "high") outcome = "human";
  else outcome = request.confidence >= 95 ? "auto" : "review";
  return [sent, { request: request.id, chunk: chunk ? `${chunk.id}@v${chunk.version}` : "none", outcome, chars: sent.length }];
}

/** Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere. */
export function keep(trace: Trace): boolean {
  return trace.outcome.startsWith("hold") || trace.outcome === "human";
}

/** A change ships only when no protected segment loses an answer and the losses do not outnumber the gains. */
export function release(cases: Case[], protectedSegments: Set<string>): string {
  const lost = cases.filter((c) => c.oldOk && !c.newOk);
  const gained = cases.filter((c) => c.newOk && !c.oldOk);
  const hit = [...new Set(lost.filter((c) => protectedSegments.has(c.segment)).map((c) => c.segment))].sort();
  if (hit.length) return "no-go: protected segment lost answers: " + hit.join(", ");
  if (lost.length > gained.length) return `no-go: net loss: lost ${lost.length}, gained ${gained.length}`;
  return `go: lost ${lost.length}, gained ${gained.length}`;
}

function main(): void {
  const water = "Water damage is covered up to 5,000 per claim.";
  const late = "Claims must be reported within 30 days of the loss.";
  const policy = new Set(["policy"]);
  const requests: [Request, Chunk[]][] = [
    [{ id: "r1", text: "How much does the policy cover for water damage?", allowed: policy, consequence: "low", quote: water, confidence: 97 }, INDEX],
    [{ id: "r2", text: "How much does the policy cover for water damage?", allowed: policy, consequence: "low", quote: water, confidence: 97 }, STALE_INDEX],
    [{ id: "r3", text: "Can I get a refund of 400 for water damage?", allowed: policy, consequence: "high", quote: water, confidence: 99 }, INDEX],
    [{ id: "r4", text: "What is the partner commission?", allowed: policy, consequence: "low", quote: water, confidence: 99 }, INDEX],
    [{ id: "r5", text: "I reported my claim from jo@example.com, how many days do I have?", allowed: policy, consequence: "low", quote: late, confidence: 96 }, INDEX],
  ];
  const traces: Trace[] = [];
  for (const [request, index] of requests) {
    const [sent, trace] = handle(request, index);
    traces.push(trace);
    console.log(`${request.id}: sent=${pyRepr(sent)}; evidence=${trace.chunk}; outcome=${trace.outcome}`);
  }
  console.log("traces kept:", traces.filter(keep).map((t) => t.request).join(", "));
  const cases: Case[] = [];
  for (let i = 1; i <= 6; i++) cases.push({ id: `s${i}`, segment: "status", oldOk: i > 2, newOk: true });
  for (let i = 1; i <= 4; i++) cases.push({ id: `f${i}`, segment: "refund", oldOk: true, newOk: i !== 4 });
  cases.push({ id: "c1", segment: "complaint", oldOk: false, newOk: true }, { id: "c2", segment: "complaint", oldOk: true, newOk: true });
  console.log("release with refunds protected:", release(cases, new Set(["refund"])));
  const fixed = cases.map((c) => (c.id === "f4" ? { ...c, newOk: true } : c));
  console.log("release after the refund fix:", release(fixed, new Set(["refund"])));
}

/** The text between single quotes, as the Python edition prints a string. */
function pyRepr(text: string): string {
  return `'${text}'`;
}

if (import.meta.main) main();
```
```text
r1: sent='How much does the policy cover for water damage?'; evidence=policy-2@v3; outcome=auto
r2: sent='How much does the policy cover for water damage?'; evidence=policy-2-old@v2; outcome=hold: stale evidence (policy-2-old v2, current v3)
r3: sent='Can I get a refund of 400 for water damage?'; evidence=policy-2@v3; outcome=human
r4: sent='What is the partner commission?'; evidence=none; outcome=hold: no evidence
r5: sent='I reported my claim from <EMAIL_1>, how many days do I have?'; evidence=policy-1@v3; outcome=auto
traces kept: r2, r3, r4
release with refunds protected: no-go: protected segment lost answers: refund
release after the refund fix: go: lost 0, gained 3
```
```java
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.
 *
 * The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
 */
public class ClaimsAssistant {
    private static final System.Logger LOG = System.getLogger(ClaimsAssistant.class.getName());
    record Chunk(String id, String doc, int version, String text) {}

    record Request(String id, String text, Set<String> allowed, String consequence, String quote, int confidence) {}

    record Case(String id, String segment, boolean oldOk, boolean newOk) {}

    record Trace(String request, String chunk, String outcome, int chars) {}

    record Handled(String sent, Trace trace) {}

    record Tokenised(String sent, Map<String, String> vault) {}

    static final Map<String, Integer> CURRENT = Map.of("policy", 3, "contracts", 1);
    static final List<Chunk> INDEX = List.of(
        new Chunk("policy-1", "policy", 3, "Claims must be reported within 30 days of the loss."),
        new Chunk("policy-2", "policy", 3, "Water damage is covered up to 5,000 per claim."),
        new Chunk("contract-9", "contracts", 1, "Partner commission is 12 percent of premiums."));
    static final List<Chunk> STALE_INDEX = List.of(INDEX.get(0), new Chunk("policy-2-old", "policy", 2, "Water damage is covered up to 3,000 per claim."), INDEX.get(2));

    /** Identifiers become tokens before the text leaves the caller; the map from token to value stays here. */
    static Tokenised tokenise(String text) {
        Map<String, String> vault = new LinkedHashMap<>();
        Matcher m = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.]+").matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String value = m.group();
            String token = null;
            for (Map.Entry<String, String> e : vault.entrySet()) if (e.getValue().equals(value)) token = e.getKey();
            if (token == null) {
                token = "<EMAIL_" + (vault.size() + 1) + ">";
                vault.put(token, value);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(token));
        }
        m.appendTail(out);
        return new Tokenised(out.toString(), vault);
    }

    static Set<String> words(String text) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = Pattern.compile("[a-z]+").matcher(text.toLowerCase());
        while (m.find()) if (m.group().length() > 3) out.add(m.group());
        return out;
    }

    /** The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence. */
    static Chunk retrieve(String question, Set<String> allowed, List<Chunk> index) {
        Set<String> mine = words(question);
        Chunk best = null;
        int bestOverlap = 0;
        List<Chunk> candidates = new ArrayList<>(index.stream().filter(c -> allowed.contains(c.doc())).toList());
        candidates.sort(Comparator.comparing(Chunk::id));
        for (Chunk c : candidates) {
            Set<String> shared = new LinkedHashSet<>(words(c.text()));
            shared.retainAll(mine);
            if (shared.size() > bestOverlap) {
                bestOverlap = shared.size();
                best = c;
            }
        }
        return best;
    }

    /** One request through the chain; the outcome says why a request was held. */
    static Handled handle(Request request, List<Chunk> index) {
        LOG.log(System.Logger.Level.DEBUG, "handle input: {0}", request);
        String sent = tokenise(request.text()).sent();
        Chunk chunk = retrieve(sent, request.allowed(), index);
        String outcome;
        if (chunk == null) outcome = "hold: no evidence";
        else if (chunk.version() != CURRENT.get(chunk.doc())) outcome = "hold: stale evidence (" + chunk.id() + " v" + chunk.version() + ", current v" + CURRENT.get(chunk.doc()) + ")";
        else if (!chunk.text().contains(request.quote())) outcome = "hold: unsupported";
        else if (request.consequence().equals("high")) outcome = "human";
        else outcome = request.confidence() >= 95 ? "auto" : "review";
        return new Handled(sent, new Trace(request.id(), chunk == null ? "none" : chunk.id() + "@v" + chunk.version(), outcome, sent.length()));
    }

    /** Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere. */
    static boolean keep(Trace trace) {
        return trace.outcome().startsWith("hold") || trace.outcome().equals("human");
    }

    /** A change ships only when no protected segment loses an answer and the losses do not outnumber the gains. */
    static String release(List<Case> cases, Set<String> protectedSegments) {
        int lost = 0;
        int gained = 0;
        TreeSet<String> hit = new TreeSet<>();
        for (Case c : cases) {
            if (c.oldOk() && !c.newOk()) {
                lost++;
                if (protectedSegments.contains(c.segment())) hit.add(c.segment());
            }
            if (c.newOk() && !c.oldOk()) gained++;
        }
        if (!hit.isEmpty()) return "no-go: protected segment lost answers: " + String.join(", ", hit);
        if (lost > gained) return "no-go: net loss: lost " + lost + ", gained " + gained;
        return "go: lost " + lost + ", gained " + gained;
    }

    public static void main(String[] args) {
        String water = "Water damage is covered up to 5,000 per claim.";
        String late = "Claims must be reported within 30 days of the loss.";
        Set<String> policy = Set.of("policy");
        List<Request> requests = List.of(
            new Request("r1", "How much does the policy cover for water damage?", policy, "low", water, 97),
            new Request("r2", "How much does the policy cover for water damage?", policy, "low", water, 97),
            new Request("r3", "Can I get a refund of 400 for water damage?", policy, "high", water, 99),
            new Request("r4", "What is the partner commission?", policy, "low", water, 99),
            new Request("r5", "I reported my claim from jo@example.com, how many days do I have?", policy, "low", late, 96));
        List<Trace> traces = new ArrayList<>();
        for (Request request : requests) {
            Handled h = handle(request, request.id().equals("r2") ? STALE_INDEX : INDEX);
            traces.add(h.trace());
            System.out.println(request.id() + ": sent='" + h.sent() + "'; evidence=" + h.trace().chunk() + "; outcome=" + h.trace().outcome());
        }
        System.out.println("traces kept: " + traces.stream().filter(ClaimsAssistant::keep).map(Trace::request).collect(Collectors.joining(", ")));
        List<Case> cases = new ArrayList<>();
        for (int i = 1; i <= 6; i++) cases.add(new Case("s" + i, "status", i > 2, true));
        for (int i = 1; i <= 4; i++) cases.add(new Case("f" + i, "refund", true, i != 4));
        cases.add(new Case("c1", "complaint", false, true));
        cases.add(new Case("c2", "complaint", true, true));
        System.out.println("release with refunds protected: " + release(cases, Set.of("refund")));
        List<Case> fixed = new ArrayList<>();
        for (Case c : cases) fixed.add(c.id().equals("f4") ? new Case(c.id(), c.segment(), c.oldOk(), true) : c);
        System.out.println("release after the refund fix: " + release(fixed, Set.of("refund")));
    }
}
```
```text
r1: sent='How much does the policy cover for water damage?'; evidence=policy-2@v3; outcome=auto
r2: sent='How much does the policy cover for water damage?'; evidence=policy-2-old@v2; outcome=hold: stale evidence (policy-2-old v2, current v3)
r3: sent='Can I get a refund of 400 for water damage?'; evidence=policy-2@v3; outcome=human
r4: sent='What is the partner commission?'; evidence=none; outcome=hold: no evidence
r5: sent='I reported my claim from <EMAIL_1>, how many days do I have?'; evidence=policy-1@v3; outcome=auto
traces kept: r2, r3, r4
release with refunds protected: no-go: protected segment lost answers: refund
release after the refund fix: go: lost 0, gained 3
```
```kotlin
/**
 * A claims assistant on one page of code: identifiers swapped for tokens before anything is sent, retrieval that filters by the reader's rights first and refuses stale evidence, a source check, a route to a person, a trace that holds no content, and a release gate that protects the costly segment.
 *
 * The documents, requests, answers and cases are invented, and the model is a scripted answer, so nothing here calls a model. The thresholds (a confidence of 95) are design values.
 */

private val log = System.getLogger("claims_assistant")

data class Chunk(val id: String, val doc: String, val version: Int, val text: String)

data class Request(val id: String, val text: String, val allowed: Set<String>, val consequence: String, val quote: String, val confidence: Int)

data class Case(val id: String, val segment: String, val oldOk: Boolean, val newOk: Boolean)

data class Trace(val request: String, val chunk: String, val outcome: String, val chars: Int)

data class Handled(val sent: String, val trace: Trace)

data class Tokenised(val sent: String, val vault: Map<String, String>)

val CURRENT = mapOf("policy" to 3, "contracts" to 1)
val INDEX = listOf(
    Chunk("policy-1", "policy", 3, "Claims must be reported within 30 days of the loss."),
    Chunk("policy-2", "policy", 3, "Water damage is covered up to 5,000 per claim."),
    Chunk("contract-9", "contracts", 1, "Partner commission is 12 percent of premiums."),
)
val STALE_INDEX = listOf(INDEX[0], Chunk("policy-2-old", "policy", 2, "Water damage is covered up to 3,000 per claim."), INDEX[2])

/** Identifiers become tokens before the text leaves the caller; the map from token to value stays here. */
fun tokenise(text: String): Tokenised {
    val vault = linkedMapOf<String, String>()
    val sent = Regex("[\\w.+-]+@[\\w-]+\\.[\\w.]+").replace(text) { m ->
        vault.entries.firstOrNull { it.value == m.value }?.key ?: "<EMAIL_${vault.size + 1}>".also { vault[it] = m.value }
    }
    return Tokenised(sent, vault)
}

fun words(text: String): Set<String> = Regex("[a-z]+").findAll(text.lowercase()).map { it.value }.filter { it.length > 3 }.toSet()

/** The reader's rights are applied before ranking; the best match wins, a tie goes to the smaller id, and no overlap is no evidence. */
fun retrieve(question: String, allowed: Set<String>, index: List<Chunk>): Chunk? {
    val mine = words(question)
    val scored = index.filter { it.doc in allowed }.map { c -> words(c.text).count { it in mine } to c }.sortedWith(compareBy({ -it.first }, { it.second.id }))
    return if (scored.isNotEmpty() && scored[0].first > 0) scored[0].second else null
}

/** One request through the chain; the outcome says why a request was held. */
fun handle(request: Request, index: List<Chunk>): Handled {
    log.log(System.Logger.Level.DEBUG, "handle input: {0}", request)
    val sent = tokenise(request.text).sent
    val chunk = retrieve(sent, request.allowed, index)
    val outcome = when {
        chunk == null -> "hold: no evidence"
        chunk.version != CURRENT.getValue(chunk.doc) -> "hold: stale evidence (${chunk.id} v${chunk.version}, current v${CURRENT.getValue(chunk.doc)})"
        request.quote !in chunk.text -> "hold: unsupported"
        request.consequence == "high" -> "human"
        else -> if (request.confidence >= 95) "auto" else "review"
    }
    return Handled(sent, Trace(request.id, if (chunk == null) "none" else "${chunk.id}@v${chunk.version}", outcome, sent.length))
}

/** Tail-based: a trace that was held or reached a person is kept, the rest are sampled elsewhere. */
fun keep(trace: Trace): Boolean = trace.outcome.startsWith("hold") || trace.outcome == "human"

/** A change ships only when no protected segment loses an answer and the losses do not outnumber the gains. */
fun release(cases: List<Case>, protectedSegments: Set<String>): String {
    val lost = cases.filter { it.oldOk && !it.newOk }
    val gained = cases.filter { it.newOk && !it.oldOk }
    val hit = lost.filter { it.segment in protectedSegments }.map { it.segment }.toSortedSet().toList()
    if (hit.isNotEmpty()) return "no-go: protected segment lost answers: " + hit.joinToString(", ")
    if (lost.size > gained.size) return "no-go: net loss: lost ${lost.size}, gained ${gained.size}"
    return "go: lost ${lost.size}, gained ${gained.size}"
}

fun main() {
    val water = "Water damage is covered up to 5,000 per claim."
    val late = "Claims must be reported within 30 days of the loss."
    val policy = setOf("policy")
    val requests = listOf(
        Request("r1", "How much does the policy cover for water damage?", policy, "low", water, 97) to INDEX,
        Request("r2", "How much does the policy cover for water damage?", policy, "low", water, 97) to STALE_INDEX,
        Request("r3", "Can I get a refund of 400 for water damage?", policy, "high", water, 99) to INDEX,
        Request("r4", "What is the partner commission?", policy, "low", water, 99) to INDEX,
        Request("r5", "I reported my claim from jo@example.com, how many days do I have?", policy, "low", late, 96) to INDEX,
    )
    val traces = mutableListOf<Trace>()
    for ((request, index) in requests) {
        val h = handle(request, index)
        traces.add(h.trace)
        println("${request.id}: sent='${h.sent}'; evidence=${h.trace.chunk}; outcome=${h.trace.outcome}")
    }
    println("traces kept: " + traces.filter { keep(it) }.joinToString(", ") { it.request })
    val cases = (1..6).map { Case("s$it", "status", it > 2, true) } + (1..4).map { Case("f$it", "refund", true, it != 4) } + listOf(Case("c1", "complaint", false, true), Case("c2", "complaint", true, true))
    println("release with refunds protected: " + release(cases, setOf("refund")))
    val fixed = cases.map { if (it.id == "f4") it.copy(newOk = true) else it }
    println("release after the refund fix: " + release(fixed, setOf("refund")))
}
```
```text
r1: sent='How much does the policy cover for water damage?'; evidence=policy-2@v3; outcome=auto
r2: sent='How much does the policy cover for water damage?'; evidence=policy-2-old@v2; outcome=hold: stale evidence (policy-2-old v2, current v3)
r3: sent='Can I get a refund of 400 for water damage?'; evidence=policy-2@v3; outcome=human
r4: sent='What is the partner commission?'; evidence=none; outcome=hold: no evidence
r5: sent='I reported my claim from <EMAIL_1>, how many days do I have?'; evidence=policy-1@v3; outcome=auto
traces kept: r2, r3, r4
release with refunds protected: no-go: protected segment lost answers: refund
release after the refund fix: go: lost 0, gained 3
```
<!-- /example -->

### The practice: a launch review

The practice is in [`exercises/93-professional-capstone`](../../exercises/93-professional-capstone/unit-01/practice-1/statement.md). You write `launch_review`, `verdict`, `scorecard` and `needed_accuracy`; the tests give you designs as facts and numbers. It is graded in Python, TypeScript, Java and Kotlin; the statement lists ten cases, each saying what you should see.

## Traps

1. **"Reject the design whenever it has any finding."** It is tempting because a clean design is the aim and any flaw looks like a reason. The exam rejects it because severity decides: a high finding rejects, a medium one asks for a revision and low findings alone approve; a review that rejects everything cannot be acted on.
2. **"List the findings by domain; it is the tidiest order."** It is tempting because domains are how the course is organised. The exam rejects it because the reader needs the blocking findings first; order by severity, then domain, then rule, so the high findings lead.
3. **"A team of exactly 10 has no managed settings, so flag it."** It is tempting because the policy says a team above ten needs them and ten feels close. The exam rejects it because a threshold is met at its edge: above 10 is a finding and exactly 10 is not; the same holds at every edge in the rule table.

## Quiz

1. Scenario: A review of a pilot finds one medium finding, a missing owner, and two low findings, a small evaluation set and an unstated accuracy. There is no high finding. What does the review conclude?
   - **a**: Approve it, since no finding is serious enough to stop the pilot
   - **b**: Revise it, since its worst flaw weakens but does not block it
   - **c**: Reject it, since three findings together are too many to approve
   - **d**: Approve it, since two of its three findings are only low ones

2. Scenario: A reviewer proposes to write the findings grouped by domain, P1 first, because the course is organised in that order. What is the objection?
   - **a**: The sponsor would have to read every finding to reach the verdict
   - **b**: Two reviewers would still produce different lists from the same design
   - **c**: A finding that spans two domains would be listed twice in the review
   - **d**: A rejecting P5 item could fall below P1 items that ask for revision

<details>
<summary>Answer key</summary>

1. **b**. With no high finding and a medium one, the review asks for a revision. *a* is ruled out because "revise if none is high and any is medium", so a medium finding stops an approval. *c* is ruled out because "A fixed ladder keeps a pile of small findings from becoming a rejection", and a rejection needs a high finding, which is absent. *d* is ruled out because the ladder approves "a design with only low findings", and this one also has a medium finding.
2. **d**. A list ordered only by domain puts severity second, so a high finding in a later domain falls below milder ones in earlier domains. *a* is ruled out because the review is written with "the verdict first, then the scorecard, then the findings in their order", so the sponsor reaches the verdict before any finding. *b* is ruled out because "Two reviewers must produce the same list", and a fixed order by domain and rule id also gives the same list. *c* is ruled out because "Each finding is a severity, a domain and a rule", so a finding sits under one domain and is listed once.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario: A proposal uses a team of agents for a task whose steps are known in advance and whose value equals ten chats. The designer says the team is more flexible. What does the review record?
   - **a**: One medium item under P1, so it goes back for revision
   - **b**: One high item under P1, so it is rejected
   - **c**: Two medium items under P1, so it goes back for revision
   - **d**: Two low items under P1, so it is approved

2. Scenario: A review is run on two proposals that differ in one number: the first has an evaluation set of 20 cases and the second has 19. What does the review record for each?
   - **a**: Both are flagged, and the thinner suite more severely
   - **b**: Only the thinner suite is flagged, with a low item under P4
   - **c**: Neither is flagged, since one case short is within the tolerance
   - **d**: Both are flagged, each with the same low item under P4

3. Scenario: A pipeline sends customer email addresses to the model in the prompt, and the audit log stores every prompt. Which description of the result fits?
   - **a**: Two blocking items, each enough on its own to reject it
   - **b**: Two items that ask for revision and nothing that blocks
   - **c**: One minor item that is only recorded and one that blocks
   - **d**: One blocking item and one revision item, with none minor

<details>
<summary>Answer key</summary>

1. **c**. The path is known, so the team is unneeded, and a value of ten is below fifteen, which makes two medium rules fire. *a* is ruled out because the first page's cost rule gives a second item: "A team of agents would cost about 15 times a chat". *b* is ruled out because "reject if any finding is high", and neither rule is high. *d* is ruled out because the rule table lists "an agent or a team is used where the path is known" as medium and not low.
2. **b**. The edge is met at 20 and missed at 19, and the rule is low. *a* is ruled out because "an evaluation set of exactly 20 cases passes", so the fuller suite is not flagged, and the table lists the rule as low for "fewer than 20 evaluation cases", whatever the shortfall. *c* is ruled out because "One step past any of them is a finding". *d* is ruled out because "an evaluation set of exactly 20 cases passes", so the fuller suite has no finding.
3. **d**. Identifiers reaching the model is high and the audit log keeping content is medium, both in P5. *a* is ruled out because the table lists "the audit log stores content" as medium, which asks for revision. *b* is ruled out because the table lists "identifiers go to the model" as high, which blocks. *c* is ruled out because the low bullet reads "It is recorded and does not hold the design back", and neither flaw is low.

</details>
