# Where to cut, what a chunk carries and which mechanism answers

**Level:** Architect Professional · **Module 85:** Retrieval pipelines at design level · **Page 1 of 2**
**Exams:** P3

**After this page you can** choose where to cut a document from the structure of the data, make every chunk carry the context it needs to be found and read alone, keep a reader's access rules inside the retrieval step, match the retrieval mechanism to the shape of the data and the pattern of the question, and say when no pipeline is needed at all.

Checked on 2026-10-04 against the Anthropic engineering post "Introducing Contextual Retrieval" (published 2024-09-19), the Claude documentation page "Embeddings", the Claude Certified Architect - Professional exam guide (version 1.0, domain 3 and its sample item 3), and by running the example offline in the course container. Nothing here called a model, and the "semantic" rankings in the example are scripted: they stand in for an embedding index, which the course does not build. This page deepens module 28 (BM25, embeddings, rank fusion, reranking and recall) and does not repeat those mechanisms. The question here is design: what to cut, what to carry and which mechanism to use for which question. Freshness, measurement and the question of weights against prompt are the second page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 3 asks the candidate to "Design a RAG pipeline with appropriate chunking and indexing strategies" and to "Apply retrieval strategies matched to data shape and query pattern". Its sample item 3 treats the retrieval and indexing step as the first suspect when answers turn confident and wrong after a document refresh. It reads as one pipeline of chunks, embeddings and an index. *What the current product does (documentation read 2026-10-04):* "Anthropic does not offer its own embedding model", so the embedding half of a pipeline is a provider's service that the team chooses and versions, and Claude's part is to answer from what it is given. When the source is small, Anthropic's post says that below about 200,000 tokens "you can just include the entire knowledge base in the prompt". On the exam, answer inside the guide's pipeline. In a design review, name the embedding provider and the small-corpus shortcut as part of the design.

## Why it matters

A retailer's support assistant answers from 40,000 policy pages. Four complaints arrive in one week. It said that a final-sale item cannot be returned and left out that damaged items are an exception. Asked how to cancel the annual plan, it quoted the monthly plan's rule. A lookup of error code E-7310 returned a page about error codes in general. And a franchise partner saw a sentence from another partner's contract in an answer. The model wrote fluent text every time. Each failure was decided earlier: where to cut, what a chunk carries, which index answers which question, and who may see what. Domain 3 of the Professional exam asks you to name that decision from the symptom.

## The idea

### The pipeline is a list of decisions

| Decision | Options | What decides it |
|---|---|---|
| Where to cut | Fixed windows, headings or clauses, one record per row | The structure of the data |
| What a chunk carries | Its text alone, title and section, a model-written context sentence | Whether the text names its own subject |
| Who may see it | A filter before ranking, a filter after, none | The reader's rights, enforced in code |
| Which mechanism answers | Cached prompt, structured query, keyword index, embedding index, both, an agent that searches | Corpus size, data shape and query pattern |

Each row has a cheap default and an expensive correction. The architect's work is to pick the row from the data before the symptom arrives.

### Cut where the data has joints

A fixed-size cut ignores what the words mean. The example's refund policy holds a rule and its exception in one sentence: "Items marked final sale cannot be returned, except when they arrive damaged." Cut every 12 words, the policy becomes 4 chunks and the sentence falls across two of them, so no chunk holds the whole rule and an answer built from the first half is wrong. Cut at the headings, the policy becomes 3 chunks and the sentence stays whole.

Two common repairs leave the cause alone. A smaller window cuts in more places and separates more sentences from their neighbours. Returning more chunks per question does not help either, because no chunk holds the whole rule. And changing the index changes how the pieces are scored, not where they were cut.

A heading, a clause number, a function, a table row and a support ticket are the joints of their kinds of data. A contract is cut by clause, a spreadsheet by row, source code by function and a transcript by speaker turn. When a unit is longer than the limit, split it at sentence ends and repeat the same prefix on every part, as the practice does. Size then follows from the unit. Anthropic's post says that "chunk size, chunk boundary, and chunk overlap" can affect retrieval performance and recommends experimenting, which in practice means choosing by measurement on your own questions (page 2).

### A chunk has to stand alone

Retrieval hands the model a chunk without its document. The post's example is a chunk that reads "The company's revenue grew by 3% over the previous quarter." It names neither the company nor the quarter, so it is hard to find and hard to use. Anthropic's fix is "prepending chunk-specific explanatory context to each chunk" before the embedding is made and before the keyword index is built. There are two ways to get that context.

- **From the structure.** The pipeline already knows the document title and the section name, so the chunk text starts with "Annual plan > Cancellation." This costs nothing, is the same on every run and can be audited.
- **From a model.** For text whose structure does not name the subject, a model writes a short context sentence for each chunk with the whole document in view. The post says the result is "usually 50-100 tokens" and shows that prompt caching keeps the one-time cost low.

The example has two plans, each with a section called Cancellation. For the question "cancel the annual plan", the bare chunks tie on the word cancel and the monthly plan's chunk comes first, because it is first in the index. With the title and section in front of each chunk, only one chunk matches the words annual and plan as well, and the annual plan's chunk comes first. Anthropic reports the effect as a drop in the failure rate of the top 20 chunks from 5.7% to 3.7% with contextual embeddings, to 2.9% with contextual keyword scoring added, and to 1.9% with reranking on top. Those are figures from the post's own datasets, an order of magnitude and not a promise for your data.

### Access control belongs in the retrieval step

Chunks carry metadata: the document, its owner or tenant, its version, its date. The reader's rights are applied before the ranking, so that a chunk the reader may not see is never a candidate, cannot win a place and cannot reach the prompt. Filtering after the best k results are taken fails in a quiet way: a reader with access to only a few documents gets fewer than k results although many visible chunks matched, because the top places went to chunks that were then removed. An instruction in the system prompt to ignore other tenants' text is a request to the model and not a control. The practice's `allowed_docs` argument is the control: documents outside it are skipped before any scoring.

### Match the mechanism to the data and the question

| Situation | Mechanism | Why | What goes wrong otherwise |
|---|---|---|---|
| The whole corpus is under about 200,000 tokens | The corpus in a cached prompt | No stage can fail and nothing can be missed | A pipeline adds stages that each can fail |
| The answer lives in a table or a database | A structured query run by a tool | Exact, and able to total or filter | The table is cut into chunks and the model adds numbers by eye |
| The question names an identifier such as an error code | A keyword index | It matches the exact string | An embedding index "can miss crucial exact matches" |
| The question paraphrases the document | An embedding index | It matches meaning | A keyword index finds nothing for "reimbursed" when the document says "refund" |
| Both kinds of question arrive | Both indexes, merged by rank | Each covers the other's blind spot | One index's misses are never seen |
| The answer needs several linked lookups | An agent that searches, one query after another | The next query depends on the last result | A single top-k list that cannot follow a chain |

Module 28 taught how the two indexes score and how fusion merges them. The point here is the choice, and the table is the one the example prints.

### The example

The example is four short documents: two plans, the refund policy and a page of error codes. It cuts the refund policy in two ways, shows the effect of the context prefix on the plan question, compares a keyword index, scripted embedding ranks and their fusion on one identifier question and one paraphrase question, and prints the mechanism chosen for six situations. In every language it ran offline in the container, and the output is the same.

<!-- example: m85-chunking-and-recall tabs: python,typescript,java,kotlin -->
```python
"""Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.

The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
documentation page "Embeddings". Nothing here calls a model.
"""
import logging
import re

log = logging.getLogger(__name__)

STOP = {"a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"}
DOCS = {
    "monthly": "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.",
    "annual": "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.",
    "refunds": "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.",
    "errors": "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.",
}


def tokens(text):
    return [t for t in re.findall(r"[a-z0-9]+(?:-[a-z0-9]+)*", text.lower()) if t not in STOP]


def chunk_fixed(doc_id, text, size):
    """Cut every `size` words, whatever the words mean."""
    words = text.split()
    return [(f"{doc_id}#{i // size}", " ".join(words[i:i + size])) for i in range(0, len(words), size)]


def chunk_sections(doc_id, text, context):
    """Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone."""
    log.debug("chunk_sections input: %r", text)
    head, *sections = text.split("\n## ")
    title = head.removeprefix("# ")
    chunks = []
    for section in sections:
        name, body = section.split("\n", 1)
        chunks.append((f"{doc_id}/{name}", (f"{title} > {name}. " if context else "") + body))
    return chunks


def index(docs, context):
    return [chunk for doc_id, text in docs.items() for chunk in chunk_sections(doc_id, text, context)]


def lexical(chunks, query, k=3):
    """Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order."""
    wanted = set(tokens(query))
    scored = []
    for n, (chunk_id, text) in enumerate(chunks):
        have = set(tokens(text))
        score = sum(3 if any(c.isdigit() for c in t) else 1 for t in wanted if t in have)
        if score:
            scored.append((-score, n, chunk_id))
    return [chunk_id for _, _, chunk_id in sorted(scored)[:k]]


def fuse(rankings, k=60):
    """Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk."""
    score = {}
    for ranking in rankings:
        for rank, chunk_id in enumerate(ranking, 1):
            score[chunk_id] = score.get(chunk_id, 0) + 1000000 // (k + rank)
    return sorted(score, key=lambda chunk_id: (-score[chunk_id], chunk_id))


def holds(chunks, chunk_ids, answer):
    """True when one retrieved chunk holds the whole answer sentence."""
    texts = dict(chunks)
    return any(answer in texts[chunk_id] for chunk_id in chunk_ids)


def reindex_additive(chunks, doc_id, text):
    """The shortcut: add the new chunks and leave the old ones where they are."""
    return chunks + chunk_sections(doc_id, text, True)


def reindex_replace(chunks, doc_id, text):
    """Drop every chunk of the document first, then add the new ones."""
    return [c for c in chunks if not c[0].startswith(doc_id + "/")] + chunk_sections(doc_id, text, True)


def stale(chunks, docs):
    """Chunks that no longer match what their source says now."""
    current = {chunk for doc_id, text in docs.items() for chunk in chunk_sections(doc_id, text, True)}
    return [chunk_id for chunk_id, text in chunks if (chunk_id, text) not in current]


def choose_retrieval(corpus_tokens, shape, pattern):
    """The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index."""
    if corpus_tokens < 200000:
        return "cached prompt"
    if shape == "table":
        return "structured query"
    if pattern == "multi-hop":
        return "agentic search"
    return {"identifier": "keyword index", "paraphrase": "embedding index"}.get(pattern, "hybrid index")


def layer(evidence_retrieved, answer_correct):
    """Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own."""
    if answer_correct:
        return "ok" if evidence_retrieved else "unsupported"
    return "generation" if evidence_retrieved else "retrieval"


def yes(flag):
    return "yes" if flag else "no"


def names(items):
    return ", ".join(items) if items else "none"


def main():
    answer = "Items marked final sale cannot be returned, except when they arrive damaged."
    fixed = chunk_fixed("refunds", DOCS["refunds"], 12)
    sections = chunk_sections("refunds", DOCS["refunds"], False)
    print(f"cut every 12 words: {len(fixed)} chunks, the whole rule in one chunk: {yes(holds(fixed, [c for c, _ in fixed], answer))}")
    print(f"cut at headings:    {len(sections)} chunks, the whole rule in one chunk: {yes(holds(sections, [c for c, _ in sections], answer))}")
    for context in (False, True):
        print(f"query 'cancel the annual plan', chunks {'with' if context else 'without'} context: top chunk {lexical(index(DOCS, context), 'cancel the annual plan', 1)[0]}")
    chunks = index(DOCS, True)
    semantic = {
        "what does E-7310 mean": ["errors/E-4021", "errors/E-7310", "refunds/Process"],
        "when will I be reimbursed": ["refunds/Process", "annual/Cancellation", "monthly/Cancellation"],
    }
    answers = {"what does E-7310 mean": "The warehouse could not reserve stock.", "when will I be reimbursed": "Refunds go back to the original payment method within 5 business days."}
    print(f"{'query':<28}{'lexical':<9}{'semantic':<10}hybrid")
    for query, ranking in semantic.items():
        lex = lexical(chunks, query)
        tops = [lex[:1], ranking[:1], fuse([lex, ranking])[:1]]
        print(f"{query:<28}" + "".join(f"{yes(holds(chunks, t, answers[query])):<{w}}" for t, w in zip(tops, (9, 10, 1))))
    print("mechanism by corpus size, data shape and query pattern:")
    for size, shape, pattern in [(50000, "text", "identifier"), (5000000, "table", "paraphrase"), (5000000, "text", "multi-hop"), (5000000, "text", "identifier"), (5000000, "text", "paraphrase"), (5000000, "text", "mixed")]:
        print(f"  {size:>8} tokens  {shape:<6}{pattern:<11}-> {choose_retrieval(size, shape, pattern)}")
    edited = DOCS["refunds"].replace("within 30 days", "within 60 days")
    live = {**DOCS, "refunds": edited}
    for name, fn in (("add the new chunks only", reindex_additive), ("replace the document's chunks", reindex_replace)):
        after = fn(chunks, "refunds", edited)
        print(f"after the window changes from 30 to 60 days, {name}: {len(after)} chunks, stale {names(stale(after, live))}")
    outcomes = [(True, True)] * 4 + [(True, False), (False, False), (False, False), (False, True)]
    counts = {}
    for retrieved, correct in outcomes:
        counts[layer(retrieved, correct)] = counts.get(layer(retrieved, correct), 0) + 1
    print(f"8 questions: evidence retrieved for {sum(r for r, _ in outcomes)}, answers correct {sum(c for _, c in outcomes)}, by layer {', '.join(f'{k} {v}' for k, v in sorted(counts.items()))}")


if __name__ == "__main__":
    main()
```
```text
cut every 12 words: 4 chunks, the whole rule in one chunk: no
cut at headings:    3 chunks, the whole rule in one chunk: yes
query 'cancel the annual plan', chunks without context: top chunk monthly/Cancellation
query 'cancel the annual plan', chunks with context: top chunk annual/Cancellation
query                       lexical  semantic  hybrid
what does E-7310 mean       yes      no        yes
when will I be reimbursed   no       yes       yes
mechanism by corpus size, data shape and query pattern:
     50000 tokens  text  identifier -> cached prompt
   5000000 tokens  table paraphrase -> structured query
   5000000 tokens  text  multi-hop  -> agentic search
   5000000 tokens  text  identifier -> keyword index
   5000000 tokens  text  paraphrase -> embedding index
   5000000 tokens  text  mixed      -> hybrid index
after the window changes from 30 to 60 days, add the new chunks only: 10 chunks, stale refunds/Eligibility
after the window changes from 30 to 60 days, replace the document's chunks: 7 chunks, stale none
8 questions: evidence retrieved for 5, answers correct 5, by layer generation 1, ok 4, retrieval 2, unsupported 1
```
```typescript
import { logger } from "./logger.ts";
const log = logger("chunking_and_recall");

/**
 * Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.
 *
 * The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
 * offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
 * documentation page "Embeddings". Nothing here calls a model.
 */
export type Chunk = [string, string];

const STOP = new Set(["a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"]);
export const DOCS: Record<string, string> = {
  monthly: "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.",
  annual: "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.",
  refunds: "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.",
  errors: "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.",
};

export function tokens(text: string): string[] {
  return (text.toLowerCase().match(/[a-z0-9]+(?:-[a-z0-9]+)*/g) ?? []).filter((t) => !STOP.has(t));
}

/** Cut every `size` words, whatever the words mean. */
export function chunkFixed(docId: string, text: string, size: number): Chunk[] {
  const words = text.split(/\s+/).filter((w) => w !== "");
  const out: Chunk[] = [];
  for (let i = 0; i < words.length; i += size) out.push([`${docId}#${Math.floor(i / size)}`, words.slice(i, i + size).join(" ")]);
  return out;
}

/** Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone. */
export function chunkSections(docId: string, text: string, context: boolean): Chunk[] {
  log.debug("chunkSections input", text);
  const [head, ...sections] = text.split("\n## ");
  const title = head.replace(/^# /, "");
  return sections.map((section) => {
    const cut = section.indexOf("\n");
    const name = section.slice(0, cut);
    return [`${docId}/${name}`, (context ? `${title} > ${name}. ` : "") + section.slice(cut + 1)] as Chunk;
  });
}

export function index(docs: Record<string, string>, context: boolean): Chunk[] {
  return Object.entries(docs).flatMap(([docId, text]) => chunkSections(docId, text, context));
}

/** Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order. */
export function lexical(chunks: Chunk[], query: string, k = 3): string[] {
  const wanted = new Set(tokens(query));
  const scored: Array<[number, number, string]> = [];
  chunks.forEach(([chunkId, text], n) => {
    const have = new Set(tokens(text));
    let score = 0;
    for (const t of wanted) if (have.has(t)) score += /[0-9]/.test(t) ? 3 : 1;
    if (score) scored.push([-score, n, chunkId]);
  });
  scored.sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  return scored.slice(0, k).map((s) => s[2]);
}

/** Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk. */
export function fuse(rankings: string[][], k = 60): string[] {
  const score = new Map<string, number>();
  for (const ranking of rankings) ranking.forEach((chunkId, i) => score.set(chunkId, (score.get(chunkId) ?? 0) + Math.floor(1000000 / (k + i + 1))));
  return [...score.keys()].sort((a, b) => score.get(b)! - score.get(a)! || (a < b ? -1 : a > b ? 1 : 0));
}

/** True when one retrieved chunk holds the whole answer sentence. */
export function holds(chunks: Chunk[], chunkIds: string[], answer: string): boolean {
  const texts = new Map(chunks);
  return chunkIds.some((id) => texts.get(id)!.includes(answer));
}

/** The shortcut: add the new chunks and leave the old ones where they are. */
export function reindexAdditive(chunks: Chunk[], docId: string, text: string): Chunk[] {
  return [...chunks, ...chunkSections(docId, text, true)];
}

/** Drop every chunk of the document first, then add the new ones. */
export function reindexReplace(chunks: Chunk[], docId: string, text: string): Chunk[] {
  return [...chunks.filter((c) => !c[0].startsWith(docId + "/")), ...chunkSections(docId, text, true)];
}

/** Chunks that no longer match what their source says now. */
export function stale(chunks: Chunk[], docs: Record<string, string>): string[] {
  const current = new Set(index(docs, true).map((c) => JSON.stringify(c)));
  return chunks.filter((c) => !current.has(JSON.stringify(c))).map((c) => c[0]);
}

/** The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index. */
export function chooseRetrieval(corpusTokens: number, shape: string, pattern: string): string {
  if (corpusTokens < 200000) return "cached prompt";
  if (shape === "table") return "structured query";
  if (pattern === "multi-hop") return "agentic search";
  return ({ identifier: "keyword index", paraphrase: "embedding index" } as Record<string, string>)[pattern] ?? "hybrid index";
}

/** Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own. */
export function layer(evidenceRetrieved: boolean, answerCorrect: boolean): string {
  if (answerCorrect) return evidenceRetrieved ? "ok" : "unsupported";
  return evidenceRetrieved ? "generation" : "retrieval";
}

const yes = (flag: boolean) => (flag ? "yes" : "no");
const names = (items: string[]) => (items.length ? items.join(", ") : "none");

function main() {
  const answer = "Items marked final sale cannot be returned, except when they arrive damaged.";
  const fixed = chunkFixed("refunds", DOCS.refunds, 12);
  const sections = chunkSections("refunds", DOCS.refunds, false);
  console.log(`cut every 12 words: ${fixed.length} chunks, the whole rule in one chunk: ${yes(holds(fixed, fixed.map((c) => c[0]), answer))}`);
  console.log(`cut at headings:    ${sections.length} chunks, the whole rule in one chunk: ${yes(holds(sections, sections.map((c) => c[0]), answer))}`);
  for (const context of [false, true]) {
    console.log(`query 'cancel the annual plan', chunks ${context ? "with" : "without"} context: top chunk ${lexical(index(DOCS, context), "cancel the annual plan", 1)[0]}`);
  }
  const chunks = index(DOCS, true);
  const semantic: Record<string, string[]> = {
    "what does E-7310 mean": ["errors/E-4021", "errors/E-7310", "refunds/Process"],
    "when will I be reimbursed": ["refunds/Process", "annual/Cancellation", "monthly/Cancellation"],
  };
  const answers: Record<string, string> = { "what does E-7310 mean": "The warehouse could not reserve stock.", "when will I be reimbursed": "Refunds go back to the original payment method within 5 business days." };
  console.log(`${"query".padEnd(28)}${"lexical".padEnd(9)}${"semantic".padEnd(10)}hybrid`);
  for (const [query, ranking] of Object.entries(semantic)) {
    const lex = lexical(chunks, query);
    const tops = [lex.slice(0, 1), ranking.slice(0, 1), fuse([lex, ranking]).slice(0, 1)];
    console.log(query.padEnd(28) + tops.map((t, i) => yes(holds(chunks, t, answers[query])).padEnd([9, 10, 1][i])).join(""));
  }
  console.log("mechanism by corpus size, data shape and query pattern:");
  const rows: Array<[number, string, string]> = [[50000, "text", "identifier"], [5000000, "table", "paraphrase"], [5000000, "text", "multi-hop"], [5000000, "text", "identifier"], [5000000, "text", "paraphrase"], [5000000, "text", "mixed"]];
  for (const [size, shape, pattern] of rows) console.log(`  ${String(size).padStart(8)} tokens  ${shape.padEnd(6)}${pattern.padEnd(11)}-> ${chooseRetrieval(size, shape, pattern)}`);
  const edited = DOCS.refunds.replace("within 30 days", "within 60 days");
  const live = { ...DOCS, refunds: edited };
  const strategies: Array<[string, (c: Chunk[], d: string, t: string) => Chunk[]]> = [["add the new chunks only", reindexAdditive], ["replace the document's chunks", reindexReplace]];
  for (const [name, fn] of strategies) {
    const after = fn(chunks, "refunds", edited);
    console.log(`after the window changes from 30 to 60 days, ${name}: ${after.length} chunks, stale ${names(stale(after, live))}`);
  }
  const outcomes: Array<[boolean, boolean]> = [[true, true], [true, true], [true, true], [true, true], [true, false], [false, false], [false, false], [false, true]];
  const counts = new Map<string, number>();
  for (const [retrieved, correct] of outcomes) counts.set(layer(retrieved, correct), (counts.get(layer(retrieved, correct)) ?? 0) + 1);
  const byLayer = [...counts.entries()].sort((a, b) => (a[0] < b[0] ? -1 : 1)).map(([k, v]) => `${k} ${v}`).join(", ");
  console.log(`8 questions: evidence retrieved for ${outcomes.filter((o) => o[0]).length}, answers correct ${outcomes.filter((o) => o[1]).length}, by layer ${byLayer}`);
}

if (import.meta.main) main();
```
```text
cut every 12 words: 4 chunks, the whole rule in one chunk: no
cut at headings:    3 chunks, the whole rule in one chunk: yes
query 'cancel the annual plan', chunks without context: top chunk monthly/Cancellation
query 'cancel the annual plan', chunks with context: top chunk annual/Cancellation
query                       lexical  semantic  hybrid
what does E-7310 mean       yes      no        yes
when will I be reimbursed   no       yes       yes
mechanism by corpus size, data shape and query pattern:
     50000 tokens  text  identifier -> cached prompt
   5000000 tokens  table paraphrase -> structured query
   5000000 tokens  text  multi-hop  -> agentic search
   5000000 tokens  text  identifier -> keyword index
   5000000 tokens  text  paraphrase -> embedding index
   5000000 tokens  text  mixed      -> hybrid index
after the window changes from 30 to 60 days, add the new chunks only: 10 chunks, stale refunds/Eligibility
after the window changes from 30 to 60 days, replace the document's chunks: 7 chunks, stale none
8 questions: evidence retrieved for 5, answers correct 5, by layer generation 1, ok 4, retrieval 2, unsupported 1
```
```java
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.
 *
 * <p>The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
 * offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
 * documentation page "Embeddings". Nothing here calls a model.
 */
public final class ChunkingAndRecall {
    private static final System.Logger LOG = System.getLogger(ChunkingAndRecall.class.getName());
    private ChunkingAndRecall() {}

    /** A chunk: its id and its text. */
    record Chunk(String id, String text) {}

    static final Set<String> STOP = Set.of("a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at");
    static final Map<String, String> DOCS = new LinkedHashMap<>();

    static {
        DOCS.put("monthly", "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.");
        DOCS.put("annual", "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.");
        DOCS.put("refunds", "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.");
        DOCS.put("errors", "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.");
    }

    private static final Pattern TOKEN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN.matcher(text.toLowerCase());
        while (m.find()) if (!STOP.contains(m.group())) out.add(m.group());
        return out;
    }

    /** Cut every `size` words, whatever the words mean. */
    static List<Chunk> chunkFixed(String docId, String text, int size) {
        String[] words = text.trim().split("\\s+");
        List<Chunk> out = new ArrayList<>();
        for (int i = 0; i < words.length; i += size) out.add(new Chunk(docId + "#" + i / size, String.join(" ", Arrays.copyOfRange(words, i, Math.min(i + size, words.length)))));
        return out;
    }

    /** Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone. */
    static List<Chunk> chunkSections(String docId, String text, boolean context) {
        LOG.log(System.Logger.Level.DEBUG, "chunkSections input: {0}", text);
        String[] parts = text.split("\n## ");
        String title = parts[0].startsWith("# ") ? parts[0].substring(2) : parts[0];
        List<Chunk> out = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            int cut = parts[i].indexOf('\n');
            String name = parts[i].substring(0, cut);
            out.add(new Chunk(docId + "/" + name, (context ? title + " > " + name + ". " : "") + parts[i].substring(cut + 1)));
        }
        return out;
    }

    static List<Chunk> index(Map<String, String> docs, boolean context) {
        List<Chunk> out = new ArrayList<>();
        docs.forEach((docId, text) -> out.addAll(chunkSections(docId, text, context)));
        return out;
    }

    private static boolean hasDigit(String t) {
        return t.chars().anyMatch(Character::isDigit);
    }

    /** Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order. */
    static List<String> lexical(List<Chunk> chunks, String query, int k) {
        Set<String> wanted = new HashSet<>(tokens(query));
        List<int[]> scored = new ArrayList<>();
        for (int n = 0; n < chunks.size(); n++) {
            Set<String> have = new HashSet<>(tokens(chunks.get(n).text()));
            int score = 0;
            for (String t : wanted) if (have.contains(t)) score += hasDigit(t) ? 3 : 1;
            if (score > 0) scored.add(new int[] {-score, n});
        }
        scored.sort((a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[1], b[1]));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(k, scored.size()); i++) out.add(chunks.get(scored.get(i)[1]).id());
        return out;
    }

    static List<String> lexical(List<Chunk> chunks, String query) {
        return lexical(chunks, query, 3);
    }

    /** Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk. */
    static List<String> fuse(List<List<String>> rankings, int k) {
        Map<String, Integer> score = new HashMap<>();
        for (List<String> ranking : rankings) for (int i = 0; i < ranking.size(); i++) score.merge(ranking.get(i), 1000000 / (k + i + 1), Integer::sum);
        List<String> ids = new ArrayList<>(score.keySet());
        ids.sort((a, b) -> score.get(a).equals(score.get(b)) ? a.compareTo(b) : Integer.compare(score.get(b), score.get(a)));
        return ids;
    }

    static List<String> fuse(List<List<String>> rankings) {
        return fuse(rankings, 60);
    }

    /** True when one retrieved chunk holds the whole answer sentence. */
    static boolean holds(List<Chunk> chunks, List<String> chunkIds, String answer) {
        Map<String, String> texts = new HashMap<>();
        for (Chunk c : chunks) texts.put(c.id(), c.text());
        return chunkIds.stream().anyMatch(id -> texts.get(id).contains(answer));
    }

    /** The shortcut: add the new chunks and leave the old ones where they are. */
    static List<Chunk> reindexAdditive(List<Chunk> chunks, String docId, String text) {
        List<Chunk> out = new ArrayList<>(chunks);
        out.addAll(chunkSections(docId, text, true));
        return out;
    }

    /** Drop every chunk of the document first, then add the new ones. */
    static List<Chunk> reindexReplace(List<Chunk> chunks, String docId, String text) {
        List<Chunk> out = new ArrayList<>();
        for (Chunk c : chunks) if (!c.id().startsWith(docId + "/")) out.add(c);
        out.addAll(chunkSections(docId, text, true));
        return out;
    }

    /** Chunks that no longer match what their source says now. */
    static List<String> stale(List<Chunk> chunks, Map<String, String> docs) {
        Set<Chunk> current = new HashSet<>(index(docs, true));
        List<String> out = new ArrayList<>();
        for (Chunk c : chunks) if (!current.contains(c)) out.add(c.id());
        return out;
    }

    /** The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index. */
    static String chooseRetrieval(int corpusTokens, String shape, String pattern) {
        if (corpusTokens < 200000) return "cached prompt";
        if (shape.equals("table")) return "structured query";
        if (pattern.equals("multi-hop")) return "agentic search";
        return switch (pattern) {
            case "identifier" -> "keyword index";
            case "paraphrase" -> "embedding index";
            default -> "hybrid index";
        };
    }

    /** Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own. */
    static String layer(boolean evidenceRetrieved, boolean answerCorrect) {
        if (answerCorrect) return evidenceRetrieved ? "ok" : "unsupported";
        return evidenceRetrieved ? "generation" : "retrieval";
    }

    private static String yes(boolean flag) {
        return flag ? "yes" : "no";
    }

    private static String names(List<String> items) {
        return items.isEmpty() ? "none" : String.join(", ", items);
    }

    private static List<String> ids(List<Chunk> chunks) {
        return chunks.stream().map(Chunk::id).toList();
    }

    public static void main(String[] args) {
        String answer = "Items marked final sale cannot be returned, except when they arrive damaged.";
        List<Chunk> fixed = chunkFixed("refunds", DOCS.get("refunds"), 12);
        List<Chunk> sections = chunkSections("refunds", DOCS.get("refunds"), false);
        System.out.println("cut every 12 words: " + fixed.size() + " chunks, the whole rule in one chunk: " + yes(holds(fixed, ids(fixed), answer)));
        System.out.println("cut at headings:    " + sections.size() + " chunks, the whole rule in one chunk: " + yes(holds(sections, ids(sections), answer)));
        for (boolean context : new boolean[] {false, true}) {
            System.out.println("query 'cancel the annual plan', chunks " + (context ? "with" : "without") + " context: top chunk " + lexical(index(DOCS, context), "cancel the annual plan", 1).get(0));
        }
        List<Chunk> chunks = index(DOCS, true);
        Map<String, List<String>> semantic = new LinkedHashMap<>();
        semantic.put("what does E-7310 mean", List.of("errors/E-4021", "errors/E-7310", "refunds/Process"));
        semantic.put("when will I be reimbursed", List.of("refunds/Process", "annual/Cancellation", "monthly/Cancellation"));
        Map<String, String> answers = Map.of("what does E-7310 mean", "The warehouse could not reserve stock.", "when will I be reimbursed", "Refunds go back to the original payment method within 5 business days.");
        System.out.println(String.format("%-28s%-9s%-10s%s", "query", "lexical", "semantic", "hybrid"));
        semantic.forEach((query, ranking) -> {
            List<String> lex = lexical(chunks, query);
            List<String> hybrid = fuse(List.of(lex, ranking));
            System.out.println(String.format("%-28s%-9s%-10s%s", query, yes(holds(chunks, lex.subList(0, Math.min(1, lex.size())), answers.get(query))),
                yes(holds(chunks, ranking.subList(0, 1), answers.get(query))), yes(holds(chunks, hybrid.subList(0, 1), answers.get(query)))));
        });
        System.out.println("mechanism by corpus size, data shape and query pattern:");
        Object[][] rows = {{50000, "text", "identifier"}, {5000000, "table", "paraphrase"}, {5000000, "text", "multi-hop"}, {5000000, "text", "identifier"}, {5000000, "text", "paraphrase"}, {5000000, "text", "mixed"}};
        for (Object[] r : rows) System.out.println(String.format("  %8d tokens  %-6s%-11s-> %s", (int) r[0], r[1], r[2], chooseRetrieval((int) r[0], (String) r[1], (String) r[2])));
        String edited = DOCS.get("refunds").replace("within 30 days", "within 60 days");
        Map<String, String> live = new LinkedHashMap<>(DOCS);
        live.put("refunds", edited);
        List<Chunk> additive = reindexAdditive(chunks, "refunds", edited);
        List<Chunk> replaced = reindexReplace(chunks, "refunds", edited);
        System.out.println("after the window changes from 30 to 60 days, add the new chunks only: " + additive.size() + " chunks, stale " + names(stale(additive, live)));
        System.out.println("after the window changes from 30 to 60 days, replace the document's chunks: " + replaced.size() + " chunks, stale " + names(stale(replaced, live)));
        boolean[][] outcomes = {{true, true}, {true, true}, {true, true}, {true, true}, {true, false}, {false, false}, {false, false}, {false, true}};
        Map<String, Integer> counts = new TreeMap<>();
        int retrieved = 0, correct = 0;
        for (boolean[] o : outcomes) {
            counts.merge(layer(o[0], o[1]), 1, Integer::sum);
            if (o[0]) retrieved++;
            if (o[1]) correct++;
        }
        StringBuilder byLayer = new StringBuilder();
        counts.forEach((k, v) -> byLayer.append(byLayer.length() == 0 ? "" : ", ").append(k).append(' ').append(v));
        System.out.println("8 questions: evidence retrieved for " + retrieved + ", answers correct " + correct + ", by layer " + byLayer);
    }
}
```
```text
cut every 12 words: 4 chunks, the whole rule in one chunk: no
cut at headings:    3 chunks, the whole rule in one chunk: yes
query 'cancel the annual plan', chunks without context: top chunk monthly/Cancellation
query 'cancel the annual plan', chunks with context: top chunk annual/Cancellation
query                       lexical  semantic  hybrid
what does E-7310 mean       yes      no        yes
when will I be reimbursed   no       yes       yes
mechanism by corpus size, data shape and query pattern:
     50000 tokens  text  identifier -> cached prompt
   5000000 tokens  table paraphrase -> structured query
   5000000 tokens  text  multi-hop  -> agentic search
   5000000 tokens  text  identifier -> keyword index
   5000000 tokens  text  paraphrase -> embedding index
   5000000 tokens  text  mixed      -> hybrid index
after the window changes from 30 to 60 days, add the new chunks only: 10 chunks, stale refunds/Eligibility
after the window changes from 30 to 60 days, replace the document's chunks: 7 chunks, stale none
8 questions: evidence retrieved for 5, answers correct 5, by layer generation 1, ok 4, retrieval 2, unsupported 1
```
```kotlin
private val log = System.getLogger("chunking_and_recall")

/**
 * Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.
 *
 * The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
 * offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
 * documentation page "Embeddings". Nothing here calls a model.
 */

/** A chunk: its id and its text. */
data class Chunk(val id: String, val text: String)

val STOP = setOf("a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at")
val DOCS = linkedMapOf(
    "monthly" to "# Monthly plan\n## Cancellation\nYou can cancel at any time and the current month is not refunded.",
    "annual" to "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund.",
    "refunds" to "# Refund policy\n## Eligibility\nCustomers may return items within 30 days of delivery.\n## Exceptions\nItems marked final sale cannot be returned, except when they arrive damaged.\n## Process\nRefunds go back to the original payment method within 5 business days.",
    "errors" to "# Error codes\n## E-7310\nThe warehouse could not reserve stock. Retry after the next stock sync.\n## E-4021\nThe payment gateway rejected the card. Ask for another card.",
)

private val TOKEN = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")

fun tokens(text: String): List<String> = TOKEN.findAll(text.lowercase()).map { it.value }.filter { it !in STOP }.toList()

/** Cut every `size` words, whatever the words mean. */
fun chunkFixed(docId: String, text: String, size: Int): List<Chunk> =
    text.trim().split(Regex("\\s+")).chunked(size).mapIndexed { i, words -> Chunk("$docId#$i", words.joinToString(" ")) }

/** Cut at the headings; with context, each chunk starts with the document title and its section name, so it can be found and read alone. */
fun chunkSections(docId: String, text: String, context: Boolean): List<Chunk> {
    log.log(System.Logger.Level.DEBUG, "chunkSections input: {0}", text)
    val parts = text.split("\n## ")
    val title = parts[0].removePrefix("# ")
    return parts.drop(1).map { section ->
        val cut = section.indexOf('\n')
        val name = section.substring(0, cut)
        Chunk("$docId/$name", (if (context) "$title > $name. " else "") + section.substring(cut + 1))
    }
}

fun index(docs: Map<String, String>, context: Boolean): List<Chunk> = docs.flatMap { (docId, text) -> chunkSections(docId, text, context) }

/** Words that appear in the chunk score one, a token with a digit (a code or an id) scores three; ties keep the index order. */
fun lexical(chunks: List<Chunk>, query: String, k: Int = 3): List<String> {
    val wanted = tokens(query).toSet()
    val scored = chunks.mapIndexedNotNull { n, chunk ->
        val have = tokens(chunk.text).toSet()
        val score = wanted.filter { it in have }.map { t -> if (t.any { c -> c.isDigit() }) 3 else 1 }.sum()
        if (score > 0) Triple(-score, n, chunk.id) else null
    }
    return scored.sortedWith(compareBy({ it.first }, { it.second })).take(k).map { it.third }
}

/** Reciprocal rank fusion, in integers so that every language ranks the same: each list adds 1000000 // (k + rank) to a chunk. */
fun fuse(rankings: List<List<String>>, k: Int = 60): List<String> {
    val score = HashMap<String, Int>()
    for (ranking in rankings) ranking.forEachIndexed { i, id -> score[id] = (score[id] ?: 0) + 1000000 / (k + i + 1) }
    return score.keys.sortedWith(compareBy({ -score.getValue(it) }, { it }))
}

/** True when one retrieved chunk holds the whole answer sentence. */
fun holds(chunks: List<Chunk>, chunkIds: List<String>, answer: String): Boolean {
    val texts = chunks.associate { it.id to it.text }
    return chunkIds.any { answer in texts.getValue(it) }
}

/** The shortcut: add the new chunks and leave the old ones where they are. */
fun reindexAdditive(chunks: List<Chunk>, docId: String, text: String): List<Chunk> = chunks + chunkSections(docId, text, true)

/** Drop every chunk of the document first, then add the new ones. */
fun reindexReplace(chunks: List<Chunk>, docId: String, text: String): List<Chunk> = chunks.filter { !it.id.startsWith("$docId/") } + chunkSections(docId, text, true)

/** Chunks that no longer match what their source says now. */
fun stale(chunks: List<Chunk>, docs: Map<String, String>): List<String> {
    val current = index(docs, true).toSet()
    return chunks.filter { it !in current }.map { it.id }
}

/** The cheapest mechanism that fits: a corpus under 200,000 tokens fits a cached prompt; a table is queried; several hops need an agent that searches; otherwise the query pattern picks the index. */
fun chooseRetrieval(corpusTokens: Int, shape: String, pattern: String): String = when {
    corpusTokens < 200000 -> "cached prompt"
    shape == "table" -> "structured query"
    pattern == "multi-hop" -> "agentic search"
    pattern == "identifier" -> "keyword index"
    pattern == "paraphrase" -> "embedding index"
    else -> "hybrid index"
}

/** Where a question went wrong, judged by retrieval and by generation separately; a right answer without its evidence is a risk of its own. */
fun layer(evidenceRetrieved: Boolean, answerCorrect: Boolean): String =
    if (answerCorrect) (if (evidenceRetrieved) "ok" else "unsupported") else (if (evidenceRetrieved) "generation" else "retrieval")

private fun yes(flag: Boolean) = if (flag) "yes" else "no"

private fun names(items: List<String>) = if (items.isEmpty()) "none" else items.joinToString(", ")

private fun ids(chunks: List<Chunk>) = chunks.map { it.id }

fun main() {
    val answer = "Items marked final sale cannot be returned, except when they arrive damaged."
    val fixed = chunkFixed("refunds", DOCS.getValue("refunds"), 12)
    val sections = chunkSections("refunds", DOCS.getValue("refunds"), false)
    println("cut every 12 words: ${fixed.size} chunks, the whole rule in one chunk: ${yes(holds(fixed, ids(fixed), answer))}")
    println("cut at headings:    ${sections.size} chunks, the whole rule in one chunk: ${yes(holds(sections, ids(sections), answer))}")
    for (context in listOf(false, true)) {
        println("query 'cancel the annual plan', chunks ${if (context) "with" else "without"} context: top chunk ${lexical(index(DOCS, context), "cancel the annual plan", 1)[0]}")
    }
    val chunks = index(DOCS, true)
    val semantic = linkedMapOf(
        "what does E-7310 mean" to listOf("errors/E-4021", "errors/E-7310", "refunds/Process"),
        "when will I be reimbursed" to listOf("refunds/Process", "annual/Cancellation", "monthly/Cancellation"),
    )
    val answers = mapOf("what does E-7310 mean" to "The warehouse could not reserve stock.", "when will I be reimbursed" to "Refunds go back to the original payment method within 5 business days.")
    println("%-28s%-9s%-10s%s".format("query", "lexical", "semantic", "hybrid"))
    for ((query, ranking) in semantic) {
        val lex = lexical(chunks, query)
        val hybrid = fuse(listOf(lex, ranking))
        println("%-28s%-9s%-10s%s".format(query, yes(holds(chunks, lex.take(1), answers.getValue(query))), yes(holds(chunks, ranking.take(1), answers.getValue(query))), yes(holds(chunks, hybrid.take(1), answers.getValue(query)))))
    }
    println("mechanism by corpus size, data shape and query pattern:")
    for ((size, shape, pattern) in listOf(Triple(50000, "text", "identifier"), Triple(5000000, "table", "paraphrase"), Triple(5000000, "text", "multi-hop"), Triple(5000000, "text", "identifier"), Triple(5000000, "text", "paraphrase"), Triple(5000000, "text", "mixed"))) {
        println("  %8d tokens  %-6s%-11s-> %s".format(size, shape, pattern, chooseRetrieval(size, shape, pattern)))
    }
    val edited = DOCS.getValue("refunds").replace("within 30 days", "within 60 days")
    val live = DOCS + ("refunds" to edited)
    val additive = reindexAdditive(chunks, "refunds", edited)
    val replaced = reindexReplace(chunks, "refunds", edited)
    println("after the window changes from 30 to 60 days, add the new chunks only: ${additive.size} chunks, stale ${names(stale(additive, live))}")
    println("after the window changes from 30 to 60 days, replace the document's chunks: ${replaced.size} chunks, stale ${names(stale(replaced, live))}")
    val outcomes = listOf(true to true, true to true, true to true, true to true, true to false, false to false, false to false, false to true)
    val counts = outcomes.groupingBy { layer(it.first, it.second) }.eachCount().toSortedMap()
    println("8 questions: evidence retrieved for ${outcomes.count { it.first }}, answers correct ${outcomes.count { it.second }}, by layer ${counts.entries.joinToString(", ") { "${it.key} ${it.value}" }}")
}
```
```text
cut every 12 words: 4 chunks, the whole rule in one chunk: no
cut at headings:    3 chunks, the whole rule in one chunk: yes
query 'cancel the annual plan', chunks without context: top chunk monthly/Cancellation
query 'cancel the annual plan', chunks with context: top chunk annual/Cancellation
query                       lexical  semantic  hybrid
what does E-7310 mean       yes      no        yes
when will I be reimbursed   no       yes       yes
mechanism by corpus size, data shape and query pattern:
     50000 tokens  text  identifier -> cached prompt
   5000000 tokens  table paraphrase -> structured query
   5000000 tokens  text  multi-hop  -> agentic search
   5000000 tokens  text  identifier -> keyword index
   5000000 tokens  text  paraphrase -> embedding index
   5000000 tokens  text  mixed      -> hybrid index
after the window changes from 30 to 60 days, add the new chunks only: 10 chunks, stale refunds/Eligibility
after the window changes from 30 to 60 days, replace the document's chunks: 7 chunks, stale none
8 questions: evidence retrieved for 5, answers correct 5, by layer generation 1, ok 4, retrieval 2, unsupported 1
```
<!-- /example -->

Read the output as four findings. The fixed cut finds no chunk with the whole rule, and the heading cut does. The context prefix moves the right plan's chunk from second to first. On the question that names E-7310 the keyword index is right and the scripted embedding ranking is wrong, since it puts the other code first. On the paraphrase "when will I be reimbursed" it is the other way round: the keyword index finds nothing, because the word reimbursed is in no chunk, and the embedding ranking is right. The fused ranking is right on both, which is the argument for running both. The last block is the decision table as code: a corpus under 200,000 tokens goes into a cached prompt before any index is considered.

## Traps

1. **"Use one chunk size for every kind of document."** It is tempting because one setting is easy to run and to explain. The exam rejects it: the cut follows the structure of the data, so a rule stays with its exception, and a length limit only applies to the units that exceed it.
2. **"Embeddings capture meaning, so the keyword index is redundant."** It is tempting because meaning sounds like a superset of words. The exam rejects it: embedding models "can miss crucial exact matches", and a question that names an identifier is the case where the keyword index is right and the embedding ranking is wrong.
3. **"Check the reader's rights on the results, after retrieval."** It is tempting because the retrieval code stays shared between customers. The exam rejects it: the filter belongs before the ranking, otherwise the visible results shrink while hidden chunks take the top places.

## Quiz

1. A returns policy states a rule and, two lines later, its exception. The pipeline indexes windows of fixed length, and the assistant gives the rule without the exception. Which change addresses the cause?
   - **a**: Break the text at section boundaries so related sentences stay together
   - **b**: Shrink each window so that every single sentence is indexed entirely on its own
   - **c**: Return more windows for each question so that both halves can arrive together
   - **d**: Swap the keyword index for an embedding index to match the meaning of the text

2. A platform serves several customers from one index. An employee of one customer, cleared for two contracts, asks a question that matches fifty passages across all customers, yet the application returns a single result. Which fault explains the single result?
   - **a**: It keeps one index for every customer instead of one index for each
   - **b**: It lets the system prompt ask the model to ignore the other tenants
   - **c**: It trims the list to the person's rights after taking the best few
   - **d**: It stores each contract twice, so that duplicate passages fill the list

<details>
<summary>Answer key</summary>

1. **a**. The cut ignored the structure of the data, and a heading is a joint at which a rule and its exception stay together. *b* is ruled out because "A smaller window cuts in more places and separates more sentences from their neighbours". *c* is ruled out because returning more chunks "does not help either, because no chunk holds the whole rule". *d* is ruled out because changing the index "changes how the pieces are scored, not where they were cut".
2. **c**. The filter ran after the cut, so passages the person may not see took the top places and were removed afterwards. *a* is ruled out because "Chunks carry metadata: the document, its owner or tenant, its version, its date" and that is how one index serves many customers. *b* is ruled out because the shortfall arises "because the top places went to chunks that were then removed", and a prompt instruction has no part in it. *d* is ruled out because a reader gets "fewer than k results although many visible chunks matched", which is the signature of a late filter and not of duplicates.

</details>
