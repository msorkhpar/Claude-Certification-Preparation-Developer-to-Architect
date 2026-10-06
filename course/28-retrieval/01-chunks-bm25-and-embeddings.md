# Chunks, BM25 and embeddings

**Level:** Developer · **Module 28:** Retrieval · **Page 1 of 2**
**Exams:** DV4

**After this page you can** describe the stages of a retrieval pipeline, cut documents into overlapping chunks, explain how BM25
scores a chunk, say what an embedding adds and what it costs, say where embeddings come from when Anthropic offers no model, and
explain why the two indexes miss different things.

Checked against the Claude API documentation (Embeddings) and Anthropic's write-up on contextual retrieval on 2026-10-03, and by
running the retrieval example and the practice offline in the course container (they call no API and need no SDK). **The embedding in the example and the practice is a toy** that counts letter trigrams. It stands in for a real model so that
the numbers are the same on every run, and it says nothing about how real models score.

## Why it matters

A model answers from what is in its context, and a knowledge base is usually bigger than a context window or too costly to send on
every request. Retrieval picks the few passages that matter for the question. When the answers are wrong, the cause is
often not the model but a passage that was never retrieved. The exam describes a failing question-answering system and asks which stage
to fix, and how anyone would know.

## The idea

### The stages

A retrieval pipeline has the same stages in most systems:

1. **Chunk.** Cut each document into passages of a size that carries one idea.
2. **Index.** Build one or more indexes over the chunks: a keyword index, an embedding index or both.
3. **Retrieve.** For a question, rank the chunks by each index and take the best ones.
4. **Merge.** Combine the rankings from several indexes into one.
5. **Rerank.** Optionally let a stronger scorer re-order the best few.
6. **Generate.** Put the chosen chunks in the prompt, ask the question, and ask for citations.

Stages 1 to 5 can be tested without a model. The practice at the end of the next page builds them and grades them on recall.

### Chunking

A chunk is a window of words. The practice's `chunk(text, size, overlap)` returns windows of `size` words that start `size - overlap`
words apart, so each window repeats the last `overlap` words of the one before it, and the last window ends at the last word. The
overlap exists because a sentence cut at a boundary loses its meaning on both sides; with an overlap at least as long as the sentence, at least one chunk
holds it whole. Too small a chunk loses context, and too large a chunk dilutes the match and spends the prompt. There is no right size
in general: measure it on your questions. The example uses 14 words with an overlap of 4, which suits its short invented
documents and nothing else.

A chunk loses something else: its surroundings. The next page covers the fix that Anthropic describes for it.

### BM25: a keyword score

BM25 scores a chunk by the query words that it contains. Each distinct query term scores

`idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * len / avg_len))`

where `tf` is how often the term occurs in the chunk, `len` is the chunk's length in tokens, `avg_len` is the average chunk length,
`k1` is 1.5 and `b` is 0.75. The inverse document frequency `idf = ln(1 + (N - df + 0.5) / (df + 0.5))` is large for a term in few chunks
and small for a term in many. A chunk's score is the sum over the terms. Three properties matter. A rare word such as an error code
outweighs common words. Repeating a word helps less each time, since `tf` sits in both the numerator and the denominator. And a short
chunk beats a long one that has the same match, because of the length term.

BM25 is exact. It finds `E-4012` when the query holds it. It does not find a chunk that says "send the parcel back" for the query
"return an item", since it matches words, not meaning.

### Embeddings: a meaning score

An embedding model turns text into a vector of numbers, so that texts with similar meaning have vectors that point the same way. The
documentation describes them as "numerical representations of text that enable measuring semantic similarity." Retrieval embeds every
chunk once, embeds the query at request time, and ranks chunks by similarity. For vectors of length 1, the cosine similarity and the
dot product are the same, so the dot product is used because it is faster. The documentation says of Voyage embeddings that they are
normalized to length 1, which means that "Cosine similarity is equivalent to dot-product similarity, while the latter can be computed
more quickly."

An embedding finds "return an item" for "send the parcel back". It is weaker with exact strings: an error code, a product number or a
name that the model has hardly seen can land far from a query that contains it.

### Where embeddings come from

"Anthropic does not offer its own embedding model." The documentation names one provider with a wide range of options, Voyage AI, and
adds that you "should assess a variety of embeddings vendors to find the best fit for your specific use case". Three practical points
from that page:

- Choose by the three factors it lists: dataset and domain fit, "Inference performance" and "Customization".
- For retrieval, use the `input_type` parameter to say whether the text is a query or a document, and "Do not omit `input_type` or set
  `input_type=None`." The provider then prepends a different prompt to each, which can lead to better retrieval quality.
- Separate models are offered for code, law and finance, and a family of rerankers (the current `rerank-3`, and `rerank-2.5` before it) takes a query and a list of documents
  and returns them ranked.

The course does not call an embedding service. The practice's `embed(text)` hashes letter trigrams into 64 buckets and scales the
vector to length 1. It sees spelling and not meaning: "delete" and "deleting" are close, "delete" and "remove" are not. That is enough
to make a deterministic test and to show the weakness of an index that sees only surface forms. It is not a model of any real model.

### Why use both

The two indexes fail on different questions, which is why most systems use both. In the example, the keyword index finds
"send back parcel" at rank 1, since the chunk about returns contains the word "parcel", while the toy embedding places the same chunk at
rank 6. For "resetting passwords" it is the other way round: BM25 finds nothing, since the document says "reset" and "password" and the
query says "resetting" and "passwords", and the embedding finds the chunk at rank 1, since the spellings overlap. Neither index is
better. The next page shows how to merge them.

## Traps

1. **One chunk size, chosen once.** Size and overlap decide what a retrieved chunk can answer. Measure on real questions.
2. **Expecting an embedding to match exact strings.** Identifiers and codes are a keyword index's strength. Use both.
3. **Looking for an Anthropic embedding model.** There is none. The documentation points to a provider such as Voyage AI, and the
   choice of vendor is yours.

## Quiz

1. A support bot must find the passage that contains the error code E-4012. Which approach is the reliable one?
   - **a**: Only a reranker, since the earlier stages cannot see digits
   - **b**: An embedding of every passage, because a code has a meaning like other codes
   - **c**: Neither, because a code is too short to be indexed at all
   - **d**: A keyword index, because it matches the exact term as written

2. A team asks where to get embeddings for Claude-based retrieval. What does the documentation say?
   - **a**: Anthropic has no model of its own, and names Voyage AI as a provider
   - **b**: Use the embedding endpoint of the Messages API, which every model has
   - **c**: Ask Claude to write the vectors, since it understands the text
   - **d**: Train a model on the Claude API's logs, which are provided for this

3. A cut falls in the middle of a key sentence of three words, and the overlap is four words. What does overlap do about it?
   - **a**: The sentence is stored twice, as a single longer chunk of its own
   - **b**: The sentence is dropped from every chunk so that no half survives
   - **c**: At least one window keeps it intact, as the repeated span is longer
   - **d**: The cut moves to the next paragraph without being asked

<details>
<summary>Answer key</summary>

1. **d**. The page says "BM25 is exact. It finds `E-4012` when the query holds it." *b* is ruled out because an embedding "is weaker with exact strings". *c* is ruled out because "A rare word such as an error code outweighs common words". *a* is ruled out because the reranker is the optional fifth stage, and the page says the keyword index "finds `E-4012` when the query holds it".
2. **a**. The page quotes "Anthropic does not offer its own embedding model." and names Voyage AI. *b* is ruled out because "Anthropic does not offer its own embedding model", so no endpoint exists. *c* is ruled out because an embedding model is a trained model that "turns text into a vector of numbers", not a text generator. *d* is ruled out because the documentation tells you to "assess a variety of embeddings vendors", and says nothing about logs.
3. **c**. The page says "with an overlap at least as long as the sentence, at least one chunk holds it whole". *b* is ruled out because "a sentence cut at a boundary loses its meaning on both sides", and overlap keeps it. *a* is ruled out because each window "repeats the last `overlap` words of the one before it", and does not build a separate chunk. *d* is ruled out because windows are cut by words: "the last window ends at the last word".

</details>
