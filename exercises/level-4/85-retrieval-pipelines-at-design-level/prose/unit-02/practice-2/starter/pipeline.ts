/** A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md. */
import { logger } from "./logger.ts";
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
  // TODO 1 of 8 (unlocks m1): the text of one chunk.
  // Receives the document title, the section name and one part of the section's text. Returns the part with its context in front, as
  // "<title> > <name>. <part>", so a chunk still says where it came from.
  // Example: chunkText("Annual plan", "Cancellation", "You can cancel.") -> "Annual plan > Cancellation. You can cancel."
  return part;
}

function splitSection(body: string, maxWords: number): string[] {
  // TODO 2 of 8 (unlocks e1): the parts of a long section.
  // Receives the text of one section and the word limit. Splits the text after each sentence end (a full stop followed by a space) and fills parts
  // with whole sentences: a part is closed before the sentence that would push it over `maxWords` words, and a single sentence longer than the limit
  // stays whole. Returns the parts as an array of strings (a short section is one part).
  // Example: "Install it. Configure it. Restart it." with maxWords 4 -> ["Install it. Configure it.", "Restart it."]
  return [body];
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
  // TODO 3 of 8 (unlocks e2): how well a chunk matches a query.
  // Receives the set of query words and the set of words of one chunk. Returns the sum, over the query words that the chunk has, of 3 for a code (a word that
  // contains a digit, such as E-7310) and 1 for any other word; 0 when the chunk has none of them.
  // Example: wanted {"cancel", "e-7310"}, have {"cancel", "e-7310", "card"} -> 4
  return 0;
}

function visible(chunk: Chunk, allowedDocs: Set<string> | null): boolean {
  // TODO 4 of 8 (unlocks e3): may this reader see this chunk?
  // Receives a chunk and the set of document ids the reader may read, or null when the reader may read all of them. Returns true when the chunk's `doc` is allowed.
  // Example: visible(chunk of "annual", new Set(["monthly"])) -> false, visible(chunk of "annual", null) -> true
  return true;
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
  // TODO 5 of 8 (unlocks e4): the retrieval mechanism.
  // Receives the corpus size in tokens, the data shape ("text" or "table") and the query pattern. Decide in this order: under 200000 tokens "cached prompt";
  // a table "structured query"; a "multi-hop" pattern "agentic search"; then "identifier" gives "keyword index", "paraphrase" gives "embedding index" and any other pattern "hybrid index".
  // Example: chooseRetrieval(2000000, "text", "identifier") -> "keyword index"
  return null;
}

function status(oldChunks: Chunk[], text: string): "added" | "kept" | "replaced" {
  // TODO 6 of 8 (unlocks e5): what a re-index does with one document.
  // Receives the chunks the index already holds for the document (an empty array when it has none) and the document's current text. Returns "added" when
  // there are no chunks, "kept" when the version of the first chunk equals `docVersion(text)`, and "replaced" when it differs.
  // Example: no chunks -> "added"; chunks made from the same text -> "kept"
  return "added";
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
  // TODO 7 of 8 (unlocks e6): the chunks that no longer match their source.
  // Receives the chunks and an object of the current documents (id to text). Returns the ids of the chunks, in order, whose document is gone or whose
  // version differs from `docVersion` of the current text.
  // Example: a chunk of a document that is no longer in `docs` is stale
  return null;
}

export function recallAtK(results: Record<string, string[]>, relevant: Record<string, string>, k: number): number | null {
  // TODO 8 of 8 (unlocks e7): the share of questions answered in the first k results.
  // Receives `results` (question to the array of chunk ids returned) and `relevant` (every labelled question to the chunk id that answers it). Returns the
  // number of labelled questions whose relevant id is among the first k results, divided by the number of labelled questions, rounded to two decimals;
  // a question with no results counts as a miss, and 0 when there are no labelled questions.
  // Example: 2 of 3 questions hit -> 0.67
  return null;
}
