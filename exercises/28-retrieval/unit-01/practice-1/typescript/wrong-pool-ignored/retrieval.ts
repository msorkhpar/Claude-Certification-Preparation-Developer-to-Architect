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

/** Windows of `size` words that start `size - overlap` words apart; the last window ends at the last word. */
export function chunk(text: string, size: number, overlap: number): string[] {
  if (size < 1 || overlap < 0 || overlap >= size) throw new RangeError("size must be at least 1 and overlap must be in 0 .. size - 1");
  const words = text.split(/\s+/).filter((w) => w.length > 0);
  const chunks: string[] = [];
  for (let start = 0; start < words.length; start += size - overlap) {
    chunks.push(words.slice(start, start + size).join(" "));
    if (start + size >= words.length) break;
  }
  return chunks;
}

/** [{ id: "<doc>#<n>", doc, text }] for every document, in order. */
export function buildChunks(corpus: Doc[], size: number, overlap: number): Chunk[] {
  return corpus.flatMap((doc) => chunk(doc.text, size, overlap).map((text, n) => ({ id: `${doc.id}#${n}`, doc: doc.id, text })));
}

const texts = (chunks: Chunk[], indexText?: Record<string, string>) => new Map(chunks.map((c) => [c.id, indexText?.[c.id] ?? c.text] as const));

function ordered(scores: Map<string, number>): string[] {
  return [...scores].filter(([, s]) => s > 0).sort((a, b) => b[1] - a[1] || (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0)).map(([id]) => id);
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
      const idf = Math.log(1 + (n - df.get(term)! + 0.5) / (df.get(term)! + 0.5));
      score += (idf * tf * (k1 + 1)) / (tf + k1 * (1 - b + (b * tokens.length) / average));
    }
    scores.set(id, score);
  }
  return ordered(scores);
}

/** Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out. */
export function embeddingRank(chunks: Chunk[], query: string, indexText?: Record<string, string>): string[] {
  const q = embed(query);
  const scores = new Map<string, number>();
  for (const [id, text] of texts(chunks, indexText)) {
    const v = embed(text);
    let total = 0;
    for (let i = 0; i < q.length; i++) total += q[i] * v[i];
    scores.set(id, total);
  }
  return ordered(scores);
}

/** Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat in
 *  one list ignored); best first, ties by id. */
export function fuse(rankings: string[][], k = 60): string[] {
  const scores = new Map<string, number>();
  for (const ranking of rankings) {
    [...new Set(ranking)].forEach((id, i) => scores.set(id, (scores.get(id) ?? 0) + 1 / (k + i + 1)));
  }
  return ordered(scores);
}

/** The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to topN. */
export function rerank(query: string, ids: string[], textOf: Record<string, string>, scorer: Scorer, topN?: number): string[] {
  const sorted = ids.map((id, i) => ({ id, i, s: scorer(query, textOf[id]) })).sort((a, b) => b.s - a.s || a.i - b.i).map((x) => x.id);
  return topN === undefined ? sorted : sorted.slice(0, topN);
}

/** The share of the relevant documents that appear among the documents of the first k chunk ids. */
export function recallAtK(ids: string[], docOf: Record<string, string>, relevant: string[], k: number): number {
  if (relevant.length === 0) throw new RangeError("relevant must not be empty");
  const found = new Set(ids.slice(0, k).map((id) => docOf[id]));
  const wanted = new Set(relevant);
  return [...wanted].filter((d) => found.has(d)).length / wanted.size;
}

/** The ids of the best k chunks. mode is bm25, embedding or hybrid (fusion of both). `contexts` maps chunk ids to a sentence
 *  that is indexed in front of the chunk's text; `scorer` reranks the first `pool` ids of the ranking. */
export function retrieve(chunks: Chunk[], query: string, mode = "hybrid", k = 3, options: Options = {}): string[] {
  const { pool = 10, contexts, scorer } = options;
  const indexText: Record<string, string> = {};
  for (const c of chunks) if (contexts && c.id in contexts) indexText[c.id] = `${contexts[c.id]} ${c.text}`;
  const lexical = bm25Rank(chunks, query, indexText);
  const semantic = embeddingRank(chunks, query, indexText);
  const ranked = mode === "bm25" ? lexical : mode === "embedding" ? semantic : fuse([lexical, semantic]);
  if (!scorer) return ranked.slice(0, k);
  return rerank(query, ranked, Object.fromEntries(chunks.map((c) => [c.id, c.text])), scorer, k);
}

/** The mean recall@k over [{ query, relevant }]. */
export function evaluate(chunks: Chunk[], queries: Query[], mode = "hybrid", k = 3, options: Options = {}): number {
  const docOf = Object.fromEntries(chunks.map((c) => [c.id, c.doc]));
  const scores = queries.map((q) => recallAtK(retrieve(chunks, q.query, mode, k, options), docOf, q.relevant, k));
  return scores.reduce((a, b) => a + b, 0) / scores.length;
}
