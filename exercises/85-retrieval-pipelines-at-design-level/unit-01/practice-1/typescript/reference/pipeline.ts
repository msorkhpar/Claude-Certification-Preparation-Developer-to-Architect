/** A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("pipeline");

export type Chunk = { id: string; doc: string; version: number; text: string };
export type Report = { added: string[]; replaced: string[]; removed: string[]; kept: string[] };
const STOP = new Set(["a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"]);

/** A cheap fingerprint of a document's text: when it changes, the document changed. */
export function docVersion(text: string): number {
  let sum = 0;
  for (let i = 0; i < text.length; i++) sum += text.charCodeAt(i);
  return sum % 1000003;
}

export function tokens(text: string): string[] {
  return (text.toLowerCase().match(/[a-z0-9]+(?:-[a-z0-9]+)*/g) ?? []).filter((t) => !STOP.has(t));
}

function chunkText(title: string, name: string, part: string): string {
  return `${title} > ${name}. ${part}`;
}

function splitSection(body: string, maxWords: number): string[] {
  const parts: string[] = [];
  let current: string[] = [];
  for (const sentence of body.split(/(?<=\.) /)) {
    if (current.length && [...current, sentence].join(" ").split(/\s+/).length > maxWords) {
      parts.push(current.join(" "));
      current = [];
    }
    current.push(sentence);
  }
  parts.push(current.join(" "));
  return parts;
}

export function chunkSections(docId: string, text: string, maxWords = 30): Chunk[] {
  log.debug("chunkSections input", text);
  const [head, ...sections] = text.split("\n## ");
  const title = head.replace(/^# /, "");
  const version = docVersion(text);
  const chunks: Chunk[] = [];
  for (const section of sections) {
    const cut = section.indexOf("\n");
    const name = section.slice(0, cut);
    const parts = splitSection(section.slice(cut + 1), maxWords);
    parts.forEach((part, i) => chunks.push({ id: `${docId}/${name}${parts.length === 1 ? "" : `#${i + 1}`}`, doc: docId, version, text: chunkText(title, name, part) }));
  }
  return chunks;
}

function score(wanted: Set<string>, have: Set<string>): number {
  let total = 0;
  for (const t of wanted) if (have.has(t)) total += /[0-9]/.test(t) ? 3 : 1;
  return total;
}

function visible(chunk: Chunk, allowedDocs: Set<string> | null): boolean {
  return allowedDocs === null || allowedDocs.has(chunk.doc);
}

export function search(chunks: Chunk[], query: string, k = 3, allowedDocs: Set<string> | null = null): string[] {
  log.debug("search input", query);
  const wanted = new Set(tokens(query));
  const scored: Array<[number, number, string]> = [];
  chunks.forEach((chunk, n) => {
    if (!visible(chunk, allowedDocs)) return;
    const points = score(wanted, new Set(tokens(chunk.text)));
    if (points) scored.push([-points, n, chunk.id]);
  });
  scored.sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  return scored.slice(0, k).map((s) => s[2]);
}

export function chooseRetrieval(corpusTokens: number, shape: string, pattern: string): string | null {
  if (corpusTokens < 200000) return "cached prompt";
  if (shape === "table") return "structured query";
  if (pattern === "multi-hop") return "agentic search";
  return ({ identifier: "keyword index", paraphrase: "embedding index" } as Record<string, string>)[pattern] ?? "hybrid index";
}

function status(oldChunks: Chunk[], text: string): "added" | "kept" | "replaced" {
  if (oldChunks.length === 0) return "added";
  return oldChunks[0].version === docVersion(text) ? "kept" : "replaced";
}

export function reindex(chunks: Chunk[], docs: Record<string, string>): [Chunk[], Report] {
  const old = new Map<string, Chunk[]>();
  for (const chunk of chunks) old.set(chunk.doc, [...(old.get(chunk.doc) ?? []), chunk]);
  const report: Report = { added: [], replaced: [], removed: [...old.keys()].filter((d) => !(d in docs)), kept: [] };
  const result: Chunk[] = [];
  for (const [docId, text] of Object.entries(docs)) {
    const state = status(old.get(docId) ?? [], text);
    report[state].push(docId);
    result.push(...(state === "kept" ? old.get(docId)! : chunkSections(docId, text)));
  }
  return [result, report];
}

export function stale(chunks: Chunk[], docs: Record<string, string>): string[] | null {
  return chunks.filter((c) => !(c.doc in docs) || c.version !== docVersion(docs[c.doc])).map((c) => c.id);
}

export function recallAtK(results: Record<string, string[]>, relevant: Record<string, string>, k: number): number | null {
  const entries = Object.entries(relevant);
  if (entries.length === 0) return 0;
  const hits = entries.filter(([query, id]) => (results[query] ?? []).slice(0, k).includes(id)).length;
  return Math.round((hits / entries.length) * 100) / 100;
}
