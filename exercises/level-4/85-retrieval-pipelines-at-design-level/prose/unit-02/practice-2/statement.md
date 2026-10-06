# Practice: a retrieval pipeline graded on recall and staleness

A support assistant answers from a handbook that changes every week. Its pipeline cuts documents at fixed lengths, so a rule lands in one chunk and its exception in the next; its chunks carry nothing but their own text, so two plans with a section called Cancellation look the same; its search filters by the reader's rights after it has taken the best results, so a reader gets one answer where fifty matched; its nightly job adds the new chunks of a changed document and leaves the old ones, so half the answers quote last month's policy; and its report counts recall only over the questions that returned something. In this practice you write the pieces that fix all of that: chunks that carry their title, section and the version of their document, the split of a long section, a search that scores a code above plain words and applies access before ranking, the choice of retrieval mechanism, a re-index that keeps, replaces, adds and removes, the stale check, and recall over every labelled question. The model is not called and there is no embedding service: the tests give you documents, queries and labels. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`chunk_sections`, `search`, `choose_retrieval`, `reindex`, `stale`, `recall_at_k`); TypeScript has the camel-case names (`chunkSections`, `chooseRetrieval`, `recallAtK`); Java has the same camel-case names as static methods of `Pipeline`; Kotlin has top-level functions. A chunk is `Chunk(id, doc, version, text)`; the starter shows it in each language, with `doc_version` and `tokens` already written.

## What is already written, and what you write

The starter is a working pipeline with eight gaps cut out of it. Everything that is plumbing is written and correct: `doc_version` and `tokens`, the cutting of a document into sections with their ids and versions, the search loop that collects and ranks scores, and the re-index loop that builds the report. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value (the unchanged text, an empty result, `None`), so the starter runs and fails every case on an assertion. Debug a gap by logging its input with the `log` line at the top of the file (the starter already logs the input of one function; add your own `log.debug` lines the same way); a run shows the lines you logged under the failing case. Write the gaps in this order (the Java and Kotlin names are the camel-case forms, TypeScript has no leading underscore):

1. `_chunk_text` unlocks `m1`: the text of a chunk with its title and section in front.
2. `_split_section` unlocks `e1`: the parts of a long section, split at sentence ends under the word limit.
3. `_score` unlocks `e2`: a code outweighs a plain word.
4. `_visible` unlocks `e3`: a reader sees only the chunks of the documents allowed.
5. `choose_retrieval` unlocks `e4`: the mechanism by corpus size, data shape and query pattern.
6. `_status` unlocks `e5`: what a re-index does with one document (added, kept or replaced).
7. `stale` unlocks `e6`: the chunks whose document changed or vanished.
8. `recall_at_k` unlocks `e7`: recall over every labelled question.

About twenty lines in all. The sections below describe the whole pipeline.

## What to write

- `chunk_sections(doc_id, text, max_words=30)` cuts a document of the form `# Title`, then `## Section` headings, each followed by its body. Each section becomes one chunk with the id `<doc_id>/<Section>`, the version `doc_version(text)` and the text `<Title> > <Section>. <body>`. When a body has more than `max_words` words, split it at sentence ends (a full stop followed by a space) into parts that each hold at most `max_words` words, packed greedily; a single sentence longer than the limit stays whole. Every part keeps the prefix, and the ids of a split section end in `#1`, `#2` and so on (an unsplit section has no suffix).
- `search(chunks, query, k=3, allowed_docs=None)` returns the ids of the `k` best chunks. Tokens come from `tokens`; a distinct query token that appears in a chunk adds 1 to its score, or 3 when it contains a digit (a code or an id). A chunk with score 0 is not a result, ties keep the order of `chunks`, and when `allowed_docs` is given (even an empty set) only chunks of those documents are candidates at all.
- `choose_retrieval(corpus_tokens, shape, pattern)` returns one of `cached prompt` (a corpus of fewer than 200,000 tokens), `structured query` (the shape is `table`), `agentic search` (the pattern is `multi-hop`), `keyword index` (`identifier`), `embedding index` (`paraphrase`) or `hybrid index` (anything else), checked in that order.
- `reindex(chunks, docs)` brings the chunks in line with `docs` (document id to text) and returns the new chunks with a report. A document whose chunks carry the version of its current text keeps them unchanged; a document whose version differs gets all its old chunks replaced by new ones; a document that has no chunks yet is added; a document with chunks that is no longer in `docs` is removed. The new list follows the order of `docs`. The report maps `added`, `replaced`, `removed` and `kept` to lists of document ids.
- `stale(chunks, docs)` returns the ids of the chunks whose document is gone from `docs` or whose version differs from `doc_version` of its current text, in chunk order.
- `recall_at_k(results, relevant, k)` returns the share of the labelled questions (`relevant` maps a question to the id of its relevant chunk) whose relevant chunk is among the first `k` ids of its result list, rounded to two decimals. A question with no entry in `results` is a miss, and with no labelled questions the result is 0.

## Why each part is there, and what you should see

1. **Chunks that stand alone.** A chunk that names its title and section can be found and read without its document. *You should see* the text start with the title and the section.
2. **Cut at the joints, split at sentences.** *You should see* a long section become parts that each keep the prefix and never cut a sentence, except one that is longer than the limit.
3. **Codes outweigh words.** *You should see* a query with one code and two plain words rank the chunk with the code first.
4. **Access before ranking.** *You should see* a reader with a narrow set of documents get every visible match, not what is left of the best few.
5. **The mechanism follows the data and the question.** *You should see* a small corpus go into a cached prompt before any index is considered, and a table queried and not searched.
6. **A re-index removes what changed.** *You should see* the old chunks of a changed document gone, and the chunks of a removed document gone with it.
7. **Staleness is checked against the source.** *You should see* an old chunk listed, and nothing listed after a correct re-index.
8. **Recall over every question.** *You should see* a question with no results lower the figure.

## The cases

| Id | What it checks |
|---|---|
| `m1` | Sections become chunks that carry their title and section and the version of their document |
| `e1` | A long section splits at sentence ends under the word limit and every part keeps the prefix |
| `e2` | A code outweighs a word and a word in no chunk matches nothing |
| `e3` | A search for a reader never returns a chunk of a document that reader may not see, even with fewer matches than `k` |
| `e4` | The mechanism follows the corpus size, then the data shape, then the query pattern |
| `e5` | A reindex keeps unchanged documents, replaces changed ones, adds new ones and drops removed ones |
| `e6` | Stale lists the chunks whose document changed or vanished |
| `e7` | Recall counts every labelled question and a question with no results is a miss |
