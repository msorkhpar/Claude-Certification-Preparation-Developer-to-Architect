// A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md.
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

export function chunk(text: string, size: number, overlap: number): string[] {
  // TODO: windows of `size` words starting `size - overlap` words apart; throw RangeError for a size or overlap that cannot work.
  return undefined as unknown as string[];
}

export function buildChunks(corpus: Doc[], size: number, overlap: number): Chunk[] {
  // TODO: [{ id: "<doc>#<n>", doc, text }] for every document, in order.
  return undefined as unknown as Chunk[];
}

export function bm25Rank(chunks: Chunk[], query: string, indexText?: Record<string, string>, k1 = 1.5, b = 0.75): string[] {
  // TODO: chunk ids by BM25 score, best first, ties by id; chunks with no word in common with the query are left out.
  return undefined as unknown as string[];
}

export function embeddingRank(chunks: Chunk[], query: string, indexText?: Record<string, string>): string[] {
  // TODO: chunk ids by the dot product of embed() vectors, best first, ties by id; zero similarity is left out.
  return undefined as unknown as string[];
}

export function fuse(rankings: string[][], k = 60): string[] {
  // TODO: reciprocal rank fusion of the rankings; best first, ties by id.
  return undefined as unknown as string[];
}

export function rerank(query: string, ids: string[], textOf: Record<string, string>, scorer: Scorer, topN?: number): string[] {
  // TODO: the given ids reordered by scorer(query, text), high to low, equal scores keeping their order, cut to topN.
  return undefined as unknown as string[];
}

export function recallAtK(ids: string[], docOf: Record<string, string>, relevant: string[], k: number): number {
  // TODO: the share of the relevant documents among the documents of the first k chunk ids; throw RangeError when none is relevant.
  return undefined as unknown as number;
}

export function retrieve(chunks: Chunk[], query: string, mode = "hybrid", k = 3, options: Options = {}): string[] {
  // TODO: the ids of the best k chunks for mode bm25, embedding or hybrid; contexts are indexed in front of a chunk's text;
  // a scorer reranks the first `pool` ids of the ranking.
  return undefined as unknown as string[];
}

export function evaluate(chunks: Chunk[], queries: Query[], mode = "hybrid", k = 3, options: Options = {}): number {
  // TODO: the mean recall@k over [{ query, relevant }].
  return undefined as unknown as number;
}
