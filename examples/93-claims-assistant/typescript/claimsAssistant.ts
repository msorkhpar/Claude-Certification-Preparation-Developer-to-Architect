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
