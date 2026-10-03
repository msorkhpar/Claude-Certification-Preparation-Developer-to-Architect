# Practice: a retrieval pipeline graded on recall

A retrieval pipeline cuts documents into chunks, finds the chunks that match a question by two different indexes, merges
the two rankings, and optionally lets a second model re-order the best few. You do not judge it by how it reads: you
measure it, by recall, over questions whose answers you know. Pick your language folder (`python`, `typescript`, `java` or
`kotlin`), open `starter/` and edit the file there. The ideas come from the Claude documentation on embeddings and search
results, read on 2026-10-03, and from the Anthropic write-up on contextual retrieval; the lesson pages explain them.

**The embedding here is a toy.** Anthropic offers no embedding model, and nothing in this practice touches a network or a
vendor. `embed(text)` is given: it counts letter trigrams of the tokenised text into 64 buckets with a fixed hash (FNV-1a
over the UTF-8 bytes of the trigram, modulo 64) and scales the vector to length 1. It sees spelling, not meaning, so
"delete" and "deleting" are close and "delete" and "remove" are not. It stands in for a real embedding model so that the
tests are deterministic; the reranker is likewise a function the tests pass in. The fusion, BM25 and recall code you write
is the real thing.

## The given parts

| Name | Meaning |
|---|---|
| `tokenize(text)` | lower-case words and numbers: the runs of `[a-z0-9]` |
| `embed(text)` | the toy embedding above: 64 numbers of length 1 |
| chunk | `{id: "<doc>#<n>", doc, text}` |
| `scorer(query, text)` | a reranker: a number, higher is more relevant |

## What to write

- `chunk(text, size, overlap)`: split the text on whitespace into words and return windows of `size` words that start
  `size - overlap` words apart; the last window ends at the last word and no window is repeated. No words gives an empty list.
  A `size` below 1, or an `overlap` below 0 or not below `size`, is an error (`ValueError` in Python, `RangeError` in
  TypeScript, `IllegalArgumentException` in Java and Kotlin).
- `build_chunks(corpus, size, overlap)`: every document `{id, text}` becomes its chunks, with ids `<doc>#0`, `<doc>#1` and so on.
- `bm25_rank(chunks, query, index_text=None)`: chunk ids by BM25 with `k1 = 1.5`, `b = 0.75` and
  `idf = ln(1 + (N - df + 0.5) / (df + 0.5))`; a distinct query term scores `idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * len / avg_len))`
  and a chunk's score is the sum, adding the terms in the order they first appear in the query. `N` is the number of chunks,
  `len` a chunk's token count. Best first, ties by id, score 0 left out. When `index_text` maps a chunk id to a string, that
  string is indexed instead of the chunk's text.
- `embedding_rank(chunks, query, index_text=None)`: chunk ids by the dot product of the `embed` vectors (they have length 1, so
  it is the cosine), best first, ties by id, a similarity of 0 or less left out; `index_text` as above.
- `fuse(rankings, k=60)`: reciprocal rank fusion. An id's score is the sum, over the lists that hold it, of `1 / (k + rank)`,
  where rank starts at 1 and a repeat of an id in one list is ignored. Best first, ties by id.
- `rerank(query, ids, texts, scorer, top_n=None)`: the given ids, ordered by `scorer(query, texts[id])` from high to low; ids with
  equal scores keep their input order; cut to `top_n` when it is given. Only the given ids take part.
- `recall_at_k(ids, doc_of, relevant, k)`: the share of the relevant documents that appear among the documents of the first `k`
  chunk ids. A document counts once however many of its chunks are there. No relevant documents is an error.
- `retrieve(chunks, query, mode="hybrid", k=3, pool=10, contexts=None, scorer=None)`: the ids of the best `k` chunks. Mode `bm25` and
  `embedding` use one ranking, `hybrid` fuses both. `contexts` maps chunk ids to a sentence that is indexed in front of the chunk's
  text (the sentence, a space, the text) for both indexes; the chunk itself is not changed. With a `scorer`, take the first
  `pool` ids of the ranking and rerank them to `k`.
- `evaluate(chunks, queries, mode, k, ...)`: the mean `recall_at_k` over `[{query, relevant}]`, with the remaining arguments passed
  on to `retrieve`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Hybrid search finds what each single index misses, measured on a small knowledge base |
| `e1` | Chunks overlap and end at the last word |
| `e2` | A rare word outweighs common ones, and short chunks win |
| `e3` | Fusion rewards agreement between lists and breaks ties by id |
| `e4` | Reranking orders only the pool by the scorer |
| `e5` | Recall counts documents, not chunks |
| `e6` | A context sentence makes a bare chunk findable |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file. The tests read the
knowledge base from `tests/fixture.json` in the language folder.
