# Fusion, reranking, context and recall

**Level:** Developer · **Module 28:** Retrieval · **Page 2 of 2**
**Exams:** DV4

**After this page you can** merge two rankings with reciprocal rank fusion, say what a reranker adds and what it costs, explain
contextual retrieval and the figures Anthropic reports for it, decide when retrieval is unnecessary, hand retrieved passages to Claude
as search results, and measure a pipeline by recall instead of by how its answers read.

Checked against Anthropic's write-up on contextual retrieval, the Claude API documentation (Embeddings and Search results) and the
course practice, on 2026-10-03. The practice and the example ran offline in the course container and call no API. **Their embedding
is a toy** and their knowledge base is invented, so their recall numbers describe the code, not any real system. The percentages
for contextual retrieval below are Anthropic's measurements on its own datasets, as reported in its write-up.

## Why it matters

A retrieval system can read fluently and still be wrong, because the passage with the answer never reached the prompt. Fluent
output hides that. The way to see it is to measure retrieval on its own: for a set of questions whose source documents are known,
how often does the right document appear in the top few results? The exam asks which stage to change when that number is low.

## The idea

### Merging two rankings: reciprocal rank fusion

The keyword index and the embedding index return rankings with scores on different scales, so the scores cannot be added. Fusion uses
only the ranks. In reciprocal rank fusion an id's score is the sum, over the lists that hold it, of `1 / (k + rank)`, where rank
starts at 1 and `k` is 60 in the practice. An id near the top of both lists scores high; an id at the top of one list and absent from the
other still scores. A repeat of an id in one list is ignored. Ties break by id, so that the output is the same on every run.

The effect shows in the example. The first relevant chunk for "send back parcel" is at rank 1 in BM25 and rank 6 in the embedding list,
and in the fused list it is at rank 2. For "resetting passwords" BM25 finds nothing and the embedding finds the chunk at rank 1; in the fused list
it is at rank 1. Over the eight questions the mean recall at 3 is 0.5 for BM25, 0.75 for the embedding and 1.0 for the fused list. On
a real knowledge base the figures differ, and you measure them on your own questions.

### Reranking

A reranker takes the query and the best few candidates and scores each pair with a stronger and slower model. Because it is slower, it
sees only a pool: the first `pool` ids of the merged ranking, which it re-orders and cuts to `k`. The documentation lists a family of rerankers
from Voyage AI that "take a query and a list of documents and return them ranked by relevance to the query". The practice passes a scoring function
in, since it has no model. Only the pool takes part: a chunk outside it is never considered, however well the scorer would have rated it. So the
pool must be large enough to contain the answer, and reranking cannot fix a retrieval that missed.

### Contextual retrieval

A chunk lifted out of its document can lose its meaning. Anthropic's example is a chunk that says "The company's revenue grew by 3%
over the previous quarter": it does not say which company or which quarter. The write-up's fix is to prepend "chunk-specific
explanatory context to each chunk before embedding" and before building the keyword index, so that the chunk is indexed as a short
sentence of context followed by the text. The model is asked to write that sentence for each chunk, with the whole document in view.

The practice's `contexts` argument models it: a map from chunk id to a sentence that is indexed in front of the chunk's text, for both
indexes, while the chunk shown to the model is unchanged. In the example the query "return window" finds nothing on the bare chunks and finds
the right chunk once a context sentence is indexed in front.

Anthropic reports the effect as a drop in the top-20-chunk retrieval failure rate:

| Technique | Failure rate | Reduction |
|---|---|---|
| Baseline | 5.7% | |
| Contextual embeddings | 3.7% | 35% |
| Contextual embeddings and contextual BM25 | 2.9% | 49% |
| Those two, with reranking added | 1.9% | 67% |

These are the write-up's figures, measured on its datasets. They show an order of magnitude and a ranking of the techniques, and they do not
promise a number for your data. The same write-up gives two practical guides: retrieve 20 chunks rather than fewer, and make
contextualization affordable with prompt caching, at a cost it states as "$1.02 per million document tokens".

### When retrieval is unnecessary

The write-up has a rule of thumb for small knowledge bases: under about 200,000 tokens, put the whole knowledge base in the prompt and cache
it, and skip retrieval. A pipeline adds stages that can each fail, and a model reading everything cannot miss a passage. The
rule is about size, not quality: above the limit, or when the base changes every minute, retrieval is the tool.

### Handing passages to Claude

Retrieved chunks go into the prompt, and answers should cite them. The documentation's search result block exists for this: a
block of type `search_result` with a required `source`, a required `title` and a `content` array of text blocks, "Use them in RAG
(Retrieval-Augmented Generation) applications where Claude needs to attribute answers to your documents." They can come from a tool
call or as top-level content in a user message. Citations are off by default and are switched on with `"enabled": true`; the
rule is all or nothing: "either all search results in a request must have citations enabled, or all must have them disabled."
Search results hold text only. The next module covers citations in full.

### Measure recall

Recall at k is the share of the relevant documents that appear among the first `k` chunk ids. The practice defines it over documents, not chunks: a
document counts once, however many of its chunks are in the list. Three habits follow.

- Build a small set of questions, each with the document or documents that hold the answer, before tuning anything.
- Change one stage at a time (chunk size, overlap, the indexes, fusion, the pool, the reranker, the context sentence) and re-measure.
- Look at the misses. A document that never appears in the top 20 points at chunking or at the index. A document that appears at rank 15 and not at rank 3
  points at fusion or reranking.

A question with no relevant documents has no recall, and the practice treats it as an error.

### The example

The example builds the chunks, ranks the eight questions with each index and with the fusion, reranks a pool and indexes a context sentence.

<!-- example: m28-retrieval-recall tabs: python,typescript -->
```python
"""A retrieval pipeline measured on recall: BM25, a toy embedding, rank fusion, a reranker and contextual indexing.

Everything is local and deterministic. The embedding is a TOY (hashed letter trigrams, no model), and the reranker is a
hand-written scoring function standing in for a reranking model. The knowledge base is invented. No API is called.
"""
import math
import re

CORPUS = [
    ('refunds', 'Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the original payment method within five business days. Digital goods are not refundable after download.'),
    ('shipping', 'Shipping times: standard delivery takes three to five business days. Express delivery arrives the next day. Orders over fifty dollars ship free to domestic addresses.'),
    ('password', 'To reset a forgotten password, open the sign in page and choose forgot password. A reset link is emailed to you and expires after one hour.'),
    ('payment-errors', 'Error E-4012 means the payment gateway rejected the card. Check the billing address and try again, or use another card. Error E-4013 means the card has expired.'),
    ('warranty', 'All hardware carries a two year warranty that covers manufacturing defects. Accidental damage is covered only with the protection plan.'),
    ('deletion', 'You can delete your account from the privacy settings. Deletion removes personal data within thirty days and cannot be undone.'),
    ('api-limits', 'The API allows sixty requests per minute for each key. Exceeding the limit returns status 429 with a retry-after header.'),
    ('invoices', 'Invoices are generated on the first day of each month and sent as PDF attachments. Past invoices can be downloaded from the billing page.'),
    ('returns', 'To return a physical item, print the prepaid label from your orders page and drop the parcel at any carrier point. Returns must arrive within fourteen days and items must be unused.'),
    ('international', 'International orders ship with a tracked carrier and take seven to twelve business days. Customs duties are paid by the recipient and are not included in the order total.'),
    ('billing', 'Your plan renews automatically each month on the billing date. You can switch plans or cancel renewal from the billing page before the date.'),
    ('api-keys', 'Create API keys in the developer console. Rotate a key by creating a new one and deleting the old one. Keys are shown only once, so store them safely.'),
    ('warranty-claims', 'Warranty claims. Hardware owners can open a claim from the support portal. Send the serial number and a photo of the damage within thirty days. We reply within two business days.'),
]

QUERIES = [
    ('send back parcel', ['returns']),
    ('free shipping threshold', ['shipping']),
    ('tracked carrier', ['international']),
    ('deleting accounts', ['deletion']),
    ('resetting passwords', ['password']),
    ('monthly invoicing', ['invoices']),
    ('deliveries arrive', ['shipping']),
    ('refund', ['refunds']),
]

CHUNK_WORDS, OVERLAP = 14, 4


def tokenize(text):
    """Lower-case words and numbers: the runs of [a-z0-9] in the text."""
    return re.findall(r"[a-z0-9]+", text.lower())


# ---- given: a deterministic toy embedding (not a real model) -------------------------------------------------------
DIMS = 64


def _fnv1a(text):
    h = 2166136261
    for byte in text.encode("utf-8"):
        h = ((h ^ byte) * 16777619) & 0xFFFFFFFF
    return h


def embed(text):
    """64 numbers: letter-trigram counts of the tokenised text hashed into buckets, scaled to length 1.
    It sees spelling, not meaning: 'delete' and 'deleting' are close, 'delete' and 'remove' are not."""
    padded = " " + " ".join(tokenize(text)) + " "
    vector = [0.0] * DIMS
    for i in range(len(padded) - 2):
        vector[_fnv1a(padded[i:i + 3]) % DIMS] += 1.0
    length = math.sqrt(sum(x * x for x in vector))
    return [x / length for x in vector] if length else vector
# ---------------------------------------------------------------------------------------------------------------------


def chunk(text, size, overlap):
    """Windows of `size` words that start `size - overlap` words apart; the last window ends at the last word."""
    if size < 1 or overlap < 0 or overlap >= size:
        raise ValueError("size must be at least 1 and overlap must be in 0 .. size - 1")
    words = text.split()
    chunks, start = [], 0
    while start < len(words):
        chunks.append(" ".join(words[start:start + size]))
        if start + size >= len(words):
            break
        start += size - overlap
    return chunks


def _texts(chunks, index_text):
    return {c["id"]: (index_text or {}).get(c["id"], c["text"]) for c in chunks}


def _ordered(scores):
    return [cid for cid, score in sorted(scores.items(), key=lambda kv: (-kv[1], kv[0])) if score > 0]


def bm25_rank(chunks, query, index_text=None, k1=1.5, b=0.75):
    """Chunk ids by BM25 score, best first, ties by id, chunks that share no word with the query left out."""
    docs = {cid: tokenize(text) for cid, text in _texts(chunks, index_text).items()}
    n = len(docs)
    average = sum(len(d) for d in docs.values()) / n if n else 0.0
    df = {}
    for tokens in docs.values():
        for term in set(tokens):
            df[term] = df.get(term, 0) + 1
    terms = list(dict.fromkeys(tokenize(query)))
    scores = {}
    for cid, tokens in docs.items():
        score = 0.0
        for term in terms:
            tf = tokens.count(term)
            if tf == 0:
                continue
            idf = math.log(1 + (n - df[term] + 0.5) / (df[term] + 0.5))
            score += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * len(tokens) / average))
        scores[cid] = score
    return _ordered(scores)


def embedding_rank(chunks, query, index_text=None):
    """Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out."""
    q = embed(query)
    scores = {}
    for cid, text in _texts(chunks, index_text).items():
        total = 0.0
        for a, b in zip(q, embed(text)):
            total += a * b
        scores[cid] = total
    return _ordered(scores)


def fuse(rankings, k=60):
    """Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat
    in one list ignored); best first, ties by id."""
    scores = {}
    for ranking in rankings:
        for rank, cid in enumerate(dict.fromkeys(ranking), start=1):
            scores[cid] = scores.get(cid, 0.0) + 1.0 / (k + rank)
    return _ordered(scores)


def rerank(query, ids, texts, scorer, top_n=None):
    """The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to top_n."""
    ordered = sorted(ids, key=lambda cid: -scorer(query, texts[cid]))  # sorted is stable
    return ordered if top_n is None else ordered[:top_n]


def doc_of(chunks):
    return {c["id"]: c["doc"] for c in chunks}


def first_rank(ids, chunks, relevant):
    """1-based position of the first chunk of a relevant document, or None."""
    docs = doc_of(chunks)
    return next((i for i, cid in enumerate(ids, start=1) if docs[cid] in relevant), None)


def recall(ids, chunks, relevant, k):
    return len({doc_of(chunks)[cid] for cid in ids[:k]} & set(relevant)) / len(set(relevant))


def rankers(chunks, query):
    lexical, semantic = bm25_rank(chunks, query), embedding_rank(chunks, query)
    return {"bm25": lexical, "embedding": semantic, "hybrid": fuse([lexical, semantic])}


SYNONYMS = {"send": ["return", "returns"], "back": ["return", "returns"]}


def toy_reranker(query, text):
    """Stands in for a reranking model: it reads the query with a tiny synonym list added and scores the share of those words that the chunk holds."""
    words = []
    for w in tokenize(query):
        words += [w] + SYNONYMS.get(w, [])
    words = list(dict.fromkeys(words))
    have = set(tokenize(text))
    return sum(w in have for w in words) / len(words)


def main():
    chunks = [{"id": f"{doc}#{n}", "doc": doc, "text": piece} for doc, text in CORPUS for n, piece in enumerate(chunk(text, CHUNK_WORDS, OVERLAP))]
    print(f"{len(CORPUS)} documents, {len(chunks)} chunks of at most {CHUNK_WORDS} words, overlap {OVERLAP}")
    print(f"{'rank of the first relevant chunk':34}{'bm25':>6}{'embedding':>11}{'hybrid':>8}")
    totals = {"bm25": 0.0, "embedding": 0.0, "hybrid": 0.0}
    for query, relevant in QUERIES:
        ranked = rankers(chunks, query)
        cells = {mode: first_rank(ids, chunks, relevant) for mode, ids in ranked.items()}
        for mode, ids in ranked.items():
            totals[mode] += recall(ids, chunks, relevant, 3) / len(QUERIES)
        print(f"{query:34}{cells['bm25'] or '-':>6}{cells['embedding'] or '-':>11}{cells['hybrid'] or '-':>8}")
    print("mean recall@3:", {mode: round(value, 3) for mode, value in totals.items()})
    texts = {c["id"]: c["text"] for c in chunks}
    query = "send back parcel"
    hybrid = rankers(chunks, query)["hybrid"]
    reranked = rerank(query, hybrid[:10], texts, toy_reranker, 3)
    print(f"{query!r}: hybrid top 3 {hybrid[:3]} -> reranked top 3 {reranked}")
    bare = [{"id": "a#0", "doc": "a", "text": "The limit is 30 days after delivery."}, {"id": "b#0", "doc": "b", "text": "The limit is 5 users per workspace."}]
    context = {"a#0": "Returns policy: the return window for physical orders. "}
    indexed = {c["id"]: context.get(c["id"], "") + c["text"] for c in bare}
    print("'return window' on the bare chunks:", bm25_rank(bare, "return window"), "| with a context sentence indexed in front:", bm25_rank(bare, "return window", indexed))


if __name__ == "__main__":
    main()
```
```text
13 documents, 34 chunks of at most 14 words, overlap 4
rank of the first relevant chunk    bm25  embedding  hybrid
send back parcel                       1          6       2
free shipping threshold                1          7       1
tracked carrier                        1          3       1
deleting accounts                      -          3       3
resetting passwords                    -          1       1
monthly invoicing                      -          1       1
deliveries arrive                      -          1       3
refund                                 1          1       1
mean recall@3: {'bm25': 0.5, 'embedding': 0.75, 'hybrid': 1.0}
'send back parcel': hybrid top 3 ['warranty-claims#1', 'returns#1', 'warranty-claims#0'] -> reranked top 3 ['returns#1', 'warranty-claims#1', 'warranty-claims#0']
'return window' on the bare chunks: [] | with a context sentence indexed in front: ['a#0']
```
```typescript
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
```
```text
13 documents, 34 chunks of at most 14 words, overlap 4
rank of the first relevant chunk    bm25  embedding  hybrid
send back parcel                       1          6       2
free shipping threshold                1          7       1
tracked carrier                        1          3       1
deleting accounts                      -          3       3
resetting passwords                    -          1       1
monthly invoicing                      -          1       1
deliveries arrive                      -          1       3
refund                                 1          1       1
mean recall@3: {'bm25': 0.5, 'embedding': 0.75, 'hybrid': 1.0}
'send back parcel': hybrid top 3 ['warranty-claims#1', 'returns#1', 'warranty-claims#0'] -> reranked top 3 ['returns#1', 'warranty-claims#1', 'warranty-claims#0']
'return window' on the bare chunks: [] | with a context sentence indexed in front: ['a#0']
```
<!-- /example -->

Read the table first. A dash means that the index never returned the relevant chunk. The last line of the table shows the mean recall at 3 for the
three modes: 0.5, 0.75 and 1.0. Then read the rerank line: the pool's top three change order and the relevant chunk moves to the front. The last line is the
context sentence: the bare chunks return nothing for "return window".

Java and Kotlin readers: the practice follows in your language, and every function in it is plain list and map code.

## The practice: a retrieval pipeline graded on recall

You write `chunk`, `build_chunks`, `bm25_rank`, `embedding_rank`, `fuse`, `rerank`, `recall_at_k`, `retrieve` and `evaluate`. The statement is in
`exercises/28-retrieval/unit-01/practice-1/statement.md`; each language folder has a `starter`, the tests, a knowledge base in
`tests/fixture.json` and a build file, and the starter fails every test. `embed` is given, and it is a toy.

| Id | What it checks |
|---|---|
| `m1` | Hybrid search finds what each single index misses, measured on a small knowledge base |
| `e1` | Chunks overlap and end at the last word |
| `e2` | A rare word outweighs common ones, and short chunks win |
| `e3` | Fusion rewards agreement between lists and breaks ties by id |
| `e4` | Reranking orders only the pool by the scorer |
| `e5` | Recall counts documents, not chunks |
| `e6` | A context sentence makes a bare chunk findable |

Case `m1` is the measurement habit in code: it asks for the mean recall of each mode over the same questions and requires the hybrid mode
to beat either single index.

## Traps

1. **Judging retrieval by the final answer.** A fluent answer can come from the wrong passage. Measure recall on its own.
2. **A pool smaller than the answer's rank.** A reranker only orders the pool. If the answer is outside it, no scorer helps.
3. **Treating the published percentages as a promise.** They are Anthropic's measurements on its datasets. Measure your own.
4. **Building retrieval for a small base.** Under about 200,000 tokens the write-up suggests the whole base in a cached prompt.

## Quiz

1. Two result lists come from indexes whose scores sit on different scales. How does the fusion method of the practice combine them?
   - **a**: It adds the raw scores of both indexes and sorts the totals that result
   - **b**: It sums `1 / (k + rank)` for each place where an id is found
   - **c**: It keeps the longer list and ignores the shorter one completely
   - **d**: It multiplies each chunk's two scores together before sorting them

2. A reranker is given a pool of 10 chunks, and the passage with the answer is ranked 14th by the merged list. What happens?
   - **a**: The reranker finds it and moves it to the front of the final list
   - **b**: The candidate count is raised for that question by the scorer itself
   - **c**: It is added to the candidates when its score is high enough to qualify
   - **d**: It is never considered, since the scorer sees only the first ten

3. A company's reference material has 80,000 tokens and changes once a month. What does the write-up's rule of thumb suggest?
   - **a**: Build the full pipeline, since every base needs one of its own to work well
   - **b**: Skip retrieval, and place the whole of it in a prompt that is cached
   - **c**: Split it into 20 chunks and index only the first ones that are written
   - **d**: Train an embedding model of its own on the text of the material

<details>
<summary>Answer key</summary>

1. **b**. The page says "an id's score is the sum, over the lists that hold it, of `1 / (k + rank)`". *a* is ruled out because "the scores cannot be added" when they are on different scales. *c* is ruled out because "an id at the top of one list and absent from the other still scores". *d* is ruled out because "Fusion uses only the ranks", with no multiplication of scores.
2. **d**. The page says "Only the pool takes part: a chunk outside it is never considered". *a* is ruled out because "reranking cannot fix a retrieval that missed". *c* is ruled out because the pool is "the first `pool` ids of the merged ranking" and nothing is added to it afterwards. *b* is ruled out because "the pool must be large enough to contain the answer", and a scorer has no control over it.
3. **b**. The page says "under about 200,000 tokens, put the whole knowledge base in the prompt and cache it, and skip retrieval". *a* is ruled out because "A pipeline adds stages that can each fail". *c* is ruled out because the guide for retrieval is to "retrieve 20 chunks rather than fewer", which is a count to retrieve and not a way to index. *d* is ruled out because "The rule is about size, not quality", and it says nothing about training a model.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A question returns a fluent answer from the wrong passage, and the team wants to find which stage failed. What should they do first?
   - **a**: Switch to a larger model and read a few more answers
   - **b**: Measure recall at k on a set with known source documents
   - **c**: Lengthen the prompt with more rules about citing sources
   - **d**: Raise the temperature to see whether another passage appears

2. A passage reads "The company's revenue grew by 3% over the previous quarter" and matches many unrelated questions. Which fix does the write-up describe?
   - **a**: Prepend a sentence of explanatory context before indexing it
   - **b**: Shorten it to a single sentence of three or four words
   - **c**: Delete it and rely on the model's own knowledge of the firm
   - **d**: Embed it twice and average the two vectors that result

3. A keyword index and an embedding index disagree on the best chunk for a query. Why is a hybrid usually better than either alone?
   - **a**: They miss different questions, so merging the results keeps what each finds
   - **b**: Their scores share one scale, which makes adding them a sound choice
   - **c**: The embedding side handles exact strings well, and the keyword side adds speed
   - **d**: The merge makes chunking of the documents unnecessary for later stages

4. A team reads a published failure-rate drop of 67% and promises the same drop to a customer. What is wrong with the promise?
   - **a**: The figure applies only to embeddings, which the customer does not use
   - **b**: The figure is too small to be useful for any real system that is deployed
   - **c**: The figure comes from the source's datasets, and yours needs measuring
   - **d**: The figure was measured with a retired model that no longer exists

<details>
<summary>Answer key</summary>

1. **b**. The page says "The way to see it is to measure retrieval on its own", with questions "whose source documents are known". *a* is ruled out because "Fluent output hides that", and a larger model reads the same retrieved passages. *c* is ruled out because the failure sits in what was retrieved: "the passage with the answer never reached the prompt". *d* is ruled out because temperature changes the wording of the answer, and "A fluent answer can come from the wrong passage".
2. **a**. The page says to prepend "chunk-specific explanatory context to each chunk before embedding" and before building the keyword index. *b* is ruled out because the problem is lost surroundings: "A chunk lifted out of its document can lose its meaning." *c* is ruled out because the fix keeps the chunk: the model is asked "to write that sentence for each chunk". *d* is ruled out because the fix is that "the chunk is indexed as a short sentence of context followed by the text", and a doubled vector adds no information.
3. **a**. The page says "The two indexes fail on different questions, which is why most systems use both." *b* is ruled out because "the scores cannot be added" when they sit on different scales. *c* is ruled out because "BM25 is exact", and the embedding "is weaker with exact strings". *d* is ruled out because "A chunk is a window of words", and the merge works on chunks that already exist.
4. **c**. The page says "These are the write-up's figures, measured on its datasets" and "Measure your own." *b* is ruled out because the figures "show an order of magnitude and a ranking of the techniques". *a* is ruled out because the 67% row is "Those two, with reranking added", so it is not embeddings alone. *d* is ruled out because "The percentages for contextual retrieval below are Anthropic's measurements on its own datasets", and the page names no model.

</details>
