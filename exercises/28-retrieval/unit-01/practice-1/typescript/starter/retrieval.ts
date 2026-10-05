// A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md.
import { logger } from "../logger.ts";

const log = logger("retrieval");

export type Chunk = { id: string; doc: string; text: string };
export type Doc = { id: string; text: string };
export type Query = { query: string; relevant: string[] };
export type Scorer = (query: string, text: string) => number;
export type Options = { pool?: number; contexts?: Record<string, string>; scorer?: Scorer };

/** Lower-case words and numbers: the runs of [a-z0-9] in the text. */
export function tokenize(text: string): string[] {
  return text.toLowerCase().match(/[a-z0-9]+/g) ?? [];
}

// ---- given: a deterministic toy embedding (not a real model) -------------------------------------------------------
const DIMS = 64;

function fnv1a(text: string): number {
  let h = 2166136261;
  for (const byte of new TextEncoder().encode(text)) h = Math.imul(h ^ byte, 16777619) >>> 0;
  return h;
}

/** 64 numbers: letter-trigram counts of the tokenised text hashed into buckets, scaled to length 1.
 *  It sees spelling, not meaning: 'delete' and 'deleting' are close, 'delete' and 'remove' are not. */
export function embed(text: string): number[] {
  const padded = " " + tokenize(text).join(" ") + " ";
  const vector = new Array<number>(DIMS).fill(0);
  for (let i = 0; i < padded.length - 2; i++) vector[fnv1a(padded.slice(i, i + 3)) % DIMS] += 1;
  const length = Math.sqrt(vector.reduce((sum, x) => sum + x * x, 0));
  return length ? vector.map((x) => x / length) : vector;
}
// ---------------------------------------------------------------------------------------------------------------------

/** Windows of `size` words that start `size - overlap` words apart; the last window ends at the last word. */
export function chunk(text: string, size: number, overlap: number): string[] {
  if (size < 1 || overlap < 0 || overlap >= size) throw new RangeError("size must be at least 1 and overlap must be in 0 .. size - 1");
  return windows(text.split(/\s+/).filter((w) => w.length > 0), size, overlap);
}

function windows(words: string[], size: number, overlap: number): string[] {
  // TODO 1 of 7 (finish this to pass e1): the windows over a list of words.
  // Receives the words, size and overlap (already checked). Returns the windows as strings of the words joined by one space: each holds
  // `size` words, the next starts `size - overlap` words later, and the loop stops at the first window that reaches the last word (so the
  // last window ends at the last word and none is repeated). No words gives [].
  // Example: windows(["a", "b", "c", "d", "e"], 3, 1) -> ["a b c", "c d e"]
  return [];
}

/** [{ id: "<doc>#<n>", doc, text }] for every document, in order. */
export function buildChunks(corpus: Doc[], size: number, overlap: number): Chunk[] {
  return corpus.flatMap((doc) => chunk(doc.text, size, overlap).map((text, n) => ({ id: `${doc.id}#${n}`, doc: doc.id, text })));
}

const texts = (chunks: Chunk[], indexText?: Record<string, string>) => new Map(chunks.map((c) => [c.id, indexText?.[c.id] ?? c.text] as const));

function ordered(scores: Map<string, number>): string[] {
  return [...scores].filter(([, s]) => s > 0).sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0)).map(([id]) => id);
}

function termScore(tf: number, df: number, n: number, length: number, average: number, k1: number, b: number): number {
  // TODO 2 of 7 (finish this to pass e2): the BM25 score one query term gives one chunk.
  // Receives the term's count in the chunk (tf, at least 1), the number of chunks that hold the term (df), the number of chunks (n), the
  // chunk's length in tokens, the average length, k1 and b. Returns idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / average)) with
  // idf = ln(1 + (n - df + 0.5) / (df + 0.5)). A rare term has a large idf; a short chunk gets a larger score than a long one.
  // Example: termScore(1, 1, 4, 5, 5, 1.5, 0.75) is about 1.2 times ln(1 + 3.5 / 1.5)
  return 0;
}

/** Chunk ids by BM25 score, best first, ties by id, chunks that share no word with the query left out. */
export function bm25Rank(chunks: Chunk[], query: string, indexText?: Record<string, string>, k1 = 1.5, b = 0.75): string[] {
  const docs = new Map([...texts(chunks, indexText)].map(([id, text]) => [id, tokenize(text)] as const));
  const n = docs.size;
  const average = n ? [...docs.values()].reduce((sum, d) => sum + d.length, 0) / n : 0;
  const df = new Map<string, number>();
  for (const tokens of docs.values()) for (const term of new Set(tokens)) df.set(term, (df.get(term) ?? 0) + 1);
  const terms = [...new Set(tokenize(query))];
  const scores = new Map<string, number>();
  for (const [id, tokens] of docs) {
    let score = 0;
    for (const term of terms) {
      const tf = tokens.filter((t) => t === term).length;
      if (tf === 0) continue;
      score += termScore(tf, df.get(term)!, n, tokens.length, average, k1, b);
    }
    scores.set(id, score);
  }
  return ordered(scores);
}

function dot(a: number[], b: number[]): number {
  // TODO 3 of 7 (finish this to pass m1): the dot product of two vectors of the same length.
  // Receives two arrays of numbers. Returns the sum of the products of the numbers in the same place (the vectors of embed() have length 1,
  // so this is their cosine). Example: dot([1, 2], [3, 4]) -> 11
  return 0;
}

/** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
export function embeddingRank(chunks: Chunk[], query: string, indexText?: Record<string, string>): string[] {
  const q = embed(query);
  const scores = new Map<string, number>();
  for (const [id, text] of texts(chunks, indexText)) {
    scores.set(id, dot(q, embed(text)));
  }
  return ordered(scores);
}

/** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat in
 *  one list ignored); best first, ties by id. */
export function fuse(rankings: string[][], k = 60): string[] {
  // TODO 4 of 7 (finish this to pass e3): reciprocal rank fusion of the lists in `rankings`.
  // Returns the ids best first: an id scores the sum of 1 / (k + rank) over the lists that hold it (rank starts at 1, a repeat in one list is
  // ignored); ordered(scores) sorts a Map of scores best first, ties by id, and leaves out scores of 0 or less.
  // Example: fuse([["a", "b"], ["b", "c"]]) -> ["b", "a", "c"]
  return [];
}

/** The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to topN. */
export function rerank(query: string, ids: string[], textOf: Record<string, string>, scorer: Scorer, topN?: number): string[] {
  // TODO 5 of 7 (finish this to pass e4): the ids ordered by scorer(query, textOf[id]) high to low.
  // Equal scores keep their input order (sort with the input position as the tie-break); cut to topN when it is given. Only the given ids take part.
  // Example: rerank("q", ["a", "b"], { a: "x", b: "xx" }, (q, t) => t.length) -> ["b", "a"]
  return [...ids];
}

/** The share of the relevant documents that appear among the documents of the first k chunk ids. */
export function recallAtK(ids: string[], docOf: Record<string, string>, relevant: string[], k: number): number {
  if (relevant.length === 0) throw new RangeError("relevant must not be empty");
  // TODO 6 of 7 (finish this to pass e5): the share of the relevant documents found among the documents of the first k chunk ids.
  // A document counts once however many of its chunks are there; `relevant` is not empty here. Returns a number from 0 to 1.
  // Example: recallAtK(["a#0", "a#1", "b#0"], { "a#0": "a", "a#1": "a", "b#0": "b" }, ["a", "c"], 3) -> 0.5
  return 0;
}

function indexedText(chunks: Chunk[], contexts?: Record<string, string>): Record<string, string> {
  // TODO 7 of 7 (finish this to pass e6): the text each chunk is indexed under when it has a context sentence.
  // Receives the chunks and `contexts` (undefined or an object from chunk id to a sentence). Returns an object from chunk id to the sentence, a
  // space and the chunk's text, for the chunks that have a sentence only; the chunks themselves are not changed.
  // Example: indexedText([{ id: "a#0", doc: "a", text: "x" }], { "a#0": "About a." }) -> { "a#0": "About a. x" }
  return {};
}

/** The ids of the best k chunks. mode is bm25, embedding or hybrid (fusion of both). `contexts` maps chunk ids to a sentence
 *  that is indexed in front of the chunk's text; `scorer` reranks the first `pool` ids of the ranking. */
export function retrieve(chunks: Chunk[], query: string, mode = "hybrid", k = 3, options: Options = {}): string[] {
  const { pool = 10, contexts, scorer } = options;
  log.debug("retrieve input", { mode, k, query });
  const indexText = indexedText(chunks, contexts);
  const lexical = bm25Rank(chunks, query, indexText);
  const semantic = embeddingRank(chunks, query, indexText);
  const ranked = mode === "bm25" ? lexical : mode === "embedding" ? semantic : fuse([lexical, semantic]);
  if (!scorer) return ranked.slice(0, k);
  return rerank(query, ranked.slice(0, pool), Object.fromEntries(chunks.map((c) => [c.id, c.text])), scorer, k);
}

/** The mean recall@k over [{ query, relevant }]. */
export function evaluate(chunks: Chunk[], queries: Query[], mode = "hybrid", k = 3, options: Options = {}): number {
  const docOf = Object.fromEntries(chunks.map((c) => [c.id, c.doc]));
  const scores = queries.map((q) => recallAtK(retrieve(chunks, q.query, mode, k, options), docOf, q.relevant, k));
  return scores.reduce((a, b) => a + b, 0) / scores.length;
}
