# A claims assistant end to end, across the seven domains

**Level:** Architect Professional · **Module 93:** Professional capstone · **Page 1 of 2**
**Exams:** P1, P2, P3, P4, P5, P6, P7

**After this page you can** take one design and walk it through the seven domains of the Professional exam, name the decision each domain forces and the module that taught it, follow a request through the chain of an assistant (identifiers swapped for tokens, retrieval by the reader's rights, a stale-evidence check, a source check, a route, a trace without content), and use a release gate that protects the costly segment.

Checked on 2026-10-04 against the Claude Certified Architect, Professional exam guide (version 1.0, all seven domains), the Claude documentation pages named in modules 79 to 92, and by running the example offline in the course container. Nothing here called a model, and the documents, requests, answers and cases are invented. This page draws on every module of Level 4. The review of a design by rule, and the practice, are the second page.

> **Exam guide and current product.** *What the guide states:* the Professional exam draws its items from seven domains: solution design and architecture (17 percent), models, prompting and context (13), integration (19), evaluation, testing and optimisation (16), governance, safety and risk (14), stakeholder communication and lifecycle (14) and developer productivity and operational enablement (7). *What the current product's documentation says (pages read 2026-10-04):* the pages behind each module are named on that module's pages; nothing on the platform ties them into one design, and no page prescribes the chain of the example, whose thresholds (a confidence of 95) are the course's design values. A capstone question names a scenario and asks which domain's rule decides it, so the skill is to find the rule and apply it, not to remember a page.

## Why it matters

The exam's scenario items do not announce their domain. A stale answer after a document refresh is an integration question, an evaluation question and a governance question at once, and the choice among the answers is made by noticing which fact in the stem carries the weight. This page shows one design in which every domain contributes a decision, so that the pattern of the questions becomes familiar: read the scenario, find the failing layer, apply the rule of the domain that owns it, and check that the other domains have not been broken by the fix.

## The idea

### The scenario

Larch Mutual, an insurer, wants an assistant that answers policy questions for its claims staff and drafts replies to customers. About 3,000 questions a day. A wrong refund costs about 250 and a person's check costs 5. The documents are policy wordings and partner contracts, and they are revised often. The claims staff may read policy documents; only the partnership team may read contracts. A sponsor will decide on a pilot.

### One design, seven decisions

Each domain forces a decision, and each decision was taught in a module.

- **P1, solution design.** The decision statement says what the assistant helps decide and what it does not. Four stages (input, processing, output, feedback) are all present, and the feedback stage is the one most often missing. The pattern is the lowest rung that meets the need: an augmented call with a small fixed workflow, not an agent, because the path is known (modules 79 and 80). A team of agents would cost about 15 times a chat, and the value does not pay it.
- **P2, models, prompts and context.** The static instructions and the policy wording come first, the question last, so that the prefix can be cached; the model is chosen by measured quality, latency and price and not by name (module 82).
- **P3, integration.** Retrieval filters by the reader's rights before it ranks, so a document the reader may not see never reaches the model. The index is replaced when a document changes, and an answer drawn from a superseded version is refused. The assistant holds the reader's rights and not its own (modules 85 and 86).
- **P4, evaluation.** A suite by segment with the refund segment protected, a gate that refuses a change that loses an answer there, a staged roll-out and a way back (modules 88 and 89).
- **P5, governance.** Identifiers are swapped for tokens before the call, the audit trace holds ids and sizes and no content, a person stands before a refund, the retention has a floor and a ceiling and users are told that AI helped (modules 83 and 90).
- **P6, stakeholders.** A summary of at most 80 words for the sponsor with one decision; service levels with owners; a pilot whose assumptions each have a stop trigger (module 91).
- **P7, enablement.** The team that builds it works under managed settings with a model list and an effort cap, and its adoption is measured against a baseline (module 92).

### The chain, request by request

The example is the middle of that design on one page: the part a request travels through. Its order is itself a design decision, and each step answers one failure.

1. **Tokenise.** An email address in the question becomes `<EMAIL_1>` before anything is sent, and the same address always gets the same token, so a question that mentions it twice still reads as one person. The map from token to address stays with the caller.
2. **Retrieve by the reader's rights.** Documents the reader may not read are removed first, then the rest are ranked by the words they share with the question; a tie goes to the smaller id, and a question that shares nothing with any readable document gets no evidence.
3. **Check the version.** Each document has a current version. A chunk from an older one means the index was not replaced: the request is held with the reason `stale evidence (policy-2-old v2, current v3)`. This is the failure of the exam's retrieval sample, and the check is cheap.
4. **Check the source.** The answer's quote must be in the chunk; an answer whose quote is missing is held as `unsupported`.
5. **Route.** A high-consequence action goes to a person; otherwise a confidence of at least 95 goes out and a lower one goes to review (page 1 of module 90).
6. **Trace and keep.** The trace holds the request id, the chunk and its version, the outcome and the number of characters, and no text. Tail-based keeping stores every trace that was held or reached a person and leaves the rest to sampling (module 87).

The example runs five requests. The first is answered. The second meets a stale index and is held. The third asks for a refund and goes to a person. The fourth asks about a partner commission, a document the reader may not read, and gets no evidence. The fifth carries an email address, which the output shows as a token, and is answered from the policy about reporting a claim. Three traces are kept: the held ones and the one that went to a person.

### The gate on the last line

The example ends with the release gate of module 89 in small. A change is judged on twelve cases in three segments. It gains answers in the status and complaint segments and loses one in the refund segment, which is protected, so the gate says no-go and names the segment, although the gains outnumber the loss. After the refund case is fixed, the same change is a go with no loss and three gains. The point of the capstone is that this gate and the first request in the chain belong to the same design: a change to the retrieval step that pleases the status segment is refused if it costs a refund answer.

### The example

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

The practice is in [`exercises/93-professional-capstone`](../../exercises/93-professional-capstone/unit-01/practice-1/statement.md) and is the second page's subject: you write the review that turns a design into findings, a verdict and a scorecard by domain.

## Traps

1. **"Fix the wrong answer in the prompt; the model got it wrong."** It is tempting because the answer is wrong and the prompt is the nearest lever. The exam rejects it because a confident wrong answer after a document refresh points to the retrieval step, where a stale chunk is the usual cause; check the version of the evidence before the model.
2. **"Rank first and filter by rights afterwards; the ranking is the hard part."** It is tempting because ranking over all documents finds the best match. The exam rejects it because a document that the reader may not see can reach the model before the filter removes it; filter by rights first, then rank.
3. **"The change gains more answers than it loses, so release it."** It is tempting because the net count is positive. The exam rejects it because a loss in a protected segment is a veto whatever the gains elsewhere; the segment that costs most when wrong is protected by the gate.

## Quiz

1. Scenario: Larch Mutual's assistant answers a coverage question with a limit of 3,000, and the current policy says 5,000. Latency and the model version have not changed since the documents were refreshed. Where does the investigation start?
   - **a**: The retrieval step, to see whether a stale chunk was returned
   - **b**: The model, to see whether its weights were updated by the vendor
   - **c**: The sampling temperature, to see whether it was lowered by a release
   - **d**: The context window, to see whether the prompt was cut short

2. Scenario: A change to the retrieval step gains three answers in a minor segment and loses one in the segment where an error costs 250. The totals favour the change. What does the gate return?
   - **a**: Release it, because the gains outnumber the losses
   - **b**: Release it, with a note asking the owner to review the lost case
   - **c**: Refuse it, naming the area where the damage fell
   - **d**: Refuse it, because every loss blocks every change

<details>
<summary>Answer key</summary>

1. **a**. A confident wrong answer after a refresh points to stale evidence. *b* is ruled out because the stem leaves the model version unchanged, and the page says "check the version of the evidence before the model". *c* is ruled out because "Fix the wrong answer in the prompt; the model got it wrong" is the tempting lever the exam rejects, and a temperature change would not return the exact old figure. *d* is ruled out because the stale index holds the chunk "Water damage is covered up to 3,000 per claim.", the very figure the assistant gave, which a cut-short prompt would not produce.
2. **c**. A loss in a protected segment is a veto whatever the gains. *a* is ruled out because "a loss in a protected segment is a veto whatever the gains elsewhere". *b* is ruled out because the gate returns a decision and not a note, and the page says it "says no-go and names the segment". *d* is ruled out because the gate's rule is that "the losses do not outnumber the gains" for a loss outside a protected segment, so not every loss blocks a change.

</details>
