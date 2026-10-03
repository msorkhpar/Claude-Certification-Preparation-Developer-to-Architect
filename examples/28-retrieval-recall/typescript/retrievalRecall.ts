// A retrieval pipeline measured on recall: BM25, a toy embedding, rank fusion, a reranker and contextual indexing.
// Everything is local and deterministic. The embedding is a TOY (hashed letter trigrams, no model), and the reranker is a
// hand-written scoring function standing in for a reranking model. The knowledge base is invented. No API is called.
type Chunk = { id: string; doc: string; text: string };
type Scorer = (query: string, text: string) => number;

export const CORPUS: Array<[string, string]> = [
  ["refunds", "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the original payment method within five business days. Digital goods are not refundable after download."],
  ["shipping", "Shipping times: standard delivery takes three to five business days. Express delivery arrives the next day. Orders over fifty dollars ship free to domestic addresses."],
  ["password", "To reset a forgotten password, open the sign in page and choose forgot password. A reset link is emailed to you and expires after one hour."],
  ["payment-errors", "Error E-4012 means the payment gateway rejected the card. Check the billing address and try again, or use another card. Error E-4013 means the card has expired."],
  ["warranty", "All hardware carries a two year warranty that covers manufacturing defects. Accidental damage is covered only with the protection plan."],
  ["deletion", "You can delete your account from the privacy settings. Deletion removes personal data within thirty days and cannot be undone."],
  ["api-limits", "The API allows sixty requests per minute for each key. Exceeding the limit returns status 429 with a retry-after header."],
  ["invoices", "Invoices are generated on the first day of each month and sent as PDF attachments. Past invoices can be downloaded from the billing page."],
  ["returns", "To return a physical item, print the prepaid label from your orders page and drop the parcel at any carrier point. Returns must arrive within fourteen days and items must be unused."],
  ["international", "International orders ship with a tracked carrier and take seven to twelve business days. Customs duties are paid by the recipient and are not included in the order total."],
  ["billing", "Your plan renews automatically each month on the billing date. You can switch plans or cancel renewal from the billing page before the date."],
  ["api-keys", "Create API keys in the developer console. Rotate a key by creating a new one and deleting the old one. Keys are shown only once, so store them safely."],
  ["warranty-claims", "Warranty claims. Hardware owners can open a claim from the support portal. Send the serial number and a photo of the damage within thirty days. We reply within two business days."],
];

export const QUERIES: Array<[string, string[]]> = [
  ["send back parcel", ["returns"]],
  ["free shipping threshold", ["shipping"]],
  ["tracked carrier", ["international"]],
  ["deleting accounts", ["deletion"]],
  ["resetting passwords", ["password"]],
  ["monthly invoicing", ["invoices"]],
  ["deliveries arrive", ["shipping"]],
  ["refund", ["refunds"]],
];

export const CHUNK_WORDS = 14;
export const OVERLAP = 4;

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

const docOf = (chunks: Chunk[]) => new Map(chunks.map((c) => [c.id, c.doc] as const));

/** 1-based position of the first chunk of a relevant document, or null. */
export function firstRank(ids: string[], chunks: Chunk[], relevant: string[]): number | null {
  const docs = docOf(chunks);
  const i = ids.findIndex((id) => relevant.includes(docs.get(id)!));
  return i === -1 ? null : i + 1;
}

export function recall(ids: string[], chunks: Chunk[], relevant: string[], k: number): number {
  const docs = docOf(chunks);
  const found = new Set(ids.slice(0, k).map((id) => docs.get(id)!));
  return new Set(relevant.filter((d) => found.has(d))).size / new Set(relevant).size;
}

export function rankers(chunks: Chunk[], query: string): Record<string, string[]> {
  const lexical = bm25Rank(chunks, query);
  const semantic = embeddingRank(chunks, query);
  return { bm25: lexical, embedding: semantic, hybrid: fuse([lexical, semantic]) };
}

const SYNONYMS: Record<string, string[]> = { send: ["return", "returns"], back: ["return", "returns"] };

/** Stands in for a reranking model: it reads the query with a tiny synonym list added and scores the share of those words that the chunk holds. */
export function toyReranker(query: string, text: string): number {
  const words = [...new Set(tokenize(query).flatMap((w) => [w, ...(SYNONYMS[w] ?? [])]))];
  const have = new Set(tokenize(text));
  return words.filter((w) => have.has(w)).length / words.length;
}

export function buildAll(): Chunk[] {
  return CORPUS.flatMap(([doc, text]) => chunk(text, CHUNK_WORDS, OVERLAP).map((piece, n) => ({ id: `${doc}#${n}`, doc, text: piece })));
}

const py = (ids: string[]) => `[${ids.map((i) => `'${i}'`).join(", ")}]`;

function main() {
  const chunks = buildAll();
  console.log(`${CORPUS.length} documents, ${chunks.length} chunks of at most ${CHUNK_WORDS} words, overlap ${OVERLAP}`);
  console.log(`${"rank of the first relevant chunk".padEnd(34)}${"bm25".padStart(6)}${"embedding".padStart(11)}${"hybrid".padStart(8)}`);
  const totals: Record<string, number> = { bm25: 0, embedding: 0, hybrid: 0 };
  for (const [query, relevant] of QUERIES) {
    const ranked = rankers(chunks, query);
    const cell = (mode: string) => String(firstRank(ranked[mode], chunks, relevant) ?? "-");
    for (const mode of Object.keys(ranked)) totals[mode] += recall(ranked[mode], chunks, relevant, 3) / QUERIES.length;
    console.log(`${query.padEnd(34)}${cell("bm25").padStart(6)}${cell("embedding").padStart(11)}${cell("hybrid").padStart(8)}`);
  }
  const num = (v: number) => (Number.isInteger(v) ? v.toFixed(1) : String(Math.round(v * 1000) / 1000));
  console.log("mean recall@3:", `{${Object.entries(totals).map(([m, v]) => `'${m}': ${num(v)}`).join(", ")}}`);
  const texts = Object.fromEntries(chunks.map((c) => [c.id, c.text]));
  const query = "send back parcel";
  const hybrid = rankers(chunks, query).hybrid;
  const reranked = rerank(query, hybrid.slice(0, 10), texts, toyReranker, 3);
  console.log(`'${query}': hybrid top 3 ${py(hybrid.slice(0, 3))} -> reranked top 3 ${py(reranked)}`);
  const bare = [{ id: "a#0", doc: "a", text: "The limit is 30 days after delivery." }, { id: "b#0", doc: "b", text: "The limit is 5 users per workspace." }];
  const context: Record<string, string> = { "a#0": "Returns policy: the return window for physical orders. " };
  const indexed = Object.fromEntries(bare.map((c) => [c.id, (context[c.id] ?? "") + c.text]));
  console.log("'return window' on the bare chunks:", py(bm25Rank(bare, "return window")), "| with a context sentence indexed in front:", py(bm25Rank(bare, "return window", indexed)));
}

if (import.meta.main) main();
