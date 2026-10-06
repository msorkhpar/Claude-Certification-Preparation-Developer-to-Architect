# Freshness after a re-index, measuring retrieval alone and the practice

**Level:** Architect Professional · **Module 85:** Retrieval pipelines at design level · **Page 2 of 2**
**Exams:** P3

**After this page you can** say what a re-index must remove as well as add, find stale text in an index and trace a confident wrong answer to it, judge retrieval and generation separately so that you know where to work, choose between the prompt, retrieval and the weights for knowledge that changes, and write the module's practice.

Checked on 2026-10-04 against the Claude Certified Architect - Professional exam guide (version 1.0, sample item 3 and its rationale), the Anthropic post "Introducing Contextual Retrieval" (how it measures retrieval), the Claude documentation glossary (fine-tuning and retrieval augmented generation) and the prompt caching page, and by running the example and the practice offline in the course container. Nothing here called a model. This page deepens module 28 (recall) and module 2 (what fine-tuning is) and builds on page 1, which covers the cut, the context and the choice of mechanism.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* its sample item 3 describes a retrieval system that "suddenly returns confident but incorrect answers after a document refresh, while latency and model version are unchanged" and keys the retrieval and indexing step as the first place to look. Its rationale says such answers, "with model and latency unchanged, point to retrieval feeding the model poor context, for example a broken re-index or mismatched embeddings". *What the current product does (documentation read 2026-10-04):* the guide's pipeline has an embedding model in it, and "Anthropic does not offer its own embedding model", so the vectors, their version and the re-embedding after a change of model belong to the provider the team picked. The glossary also states that "The Claude API does not currently offer fine-tuning", so for the question of weights against retrieval against the prompt the product leaves two practical answers, the prompt and retrieval. On the exam, the three-way comparison is a design question and the weights answer is a real option in the guide's framing. In a product review, say which platform offers it.

## Why it matters

The refund policy changes from 30 days to 60 days on Monday. By Tuesday the support assistant has been asked about returns two thousand times. Half the answers say 30 days and half say 60. Latency is normal, every reply is well formed, and the model has not changed. The nightly job ran and reported success: it added the new chunks and left the old ones where they were. Nothing in a document refresh touches the model, its sampling settings or the size of its window; the only thing that changed is what retrieval can return. Scenario questions of this kind give you the symptom and ask where to look. The exam's answer is the index, and an architect's answer adds how to prove it and how to prevent it.

## The idea

### What a re-index must do

Every chunk carries the version of the document it came from, so that the pipeline can ask a plain question of each document: has it changed since its chunks were made? The four outcomes are:

| Document | What the re-index does |
|---|---|
| Unchanged | Keeps its chunks as they are, which keeps the run cheap |
| Changed | Removes all its old chunks, then adds the new ones |
| New | Adds its chunks |
| Removed | Removes its chunks |

The shortcut is to add the chunks of the changed documents and leave the rest. The example shows what that costs. After the refund window changes from 30 to 60 days, the shortcut leaves 10 chunks in the index, and one of them, the Eligibility chunk, still says 30 days beside its new twin. The replacing re-index leaves 7 chunks and no stale one. Both runs finish without an error and both are fast. The old chunk is not a malformed record, it is a correct record of a past fact, and it competes with the new one for a place in the top results. A re-index that runs less often than the documents change leaves old text in the index between runs, so the schedule belongs to the design too, and for a document that is withdrawn the removal is a correctness and, for personal data, a compliance matter, not a tidiness one.

A change of embedding model has the same shape on a larger scale. Vectors made by two models are not in one space, so the index must be rebuilt as a whole, or queries embedded by the new model will be compared with chunks embedded by the old one. The guide's phrase for it is "mismatched embeddings".

### Staleness is something you measure

An index is stale when a chunk no longer matches its source. The check is cheap and exact: compare each chunk's version with the current version of its document, and list the chunks that differ or whose document is gone. The practice's `stale` does this. Run it after every re-index and alert on any result, because the symptom, a confident wrong answer, arrives long after the cause. When a wrong answer is reported, the trace should hold the ids and versions of the chunks that were retrieved (module 87), so that the question "was it stale?" has an answer in one lookup.

Recall by chunk id, or by document as in module 28, does not see staleness: an old chunk with the right id still counts as a hit. A retrieval metric that passes while users see old answers is the sign that the metric counts the wrong thing.

### Judge retrieval and generation separately

An end-to-end score of right and wrong answers cannot say where to work. Anthropic's post measures the retrieval step on its own, as one minus recall at 20, "the percentage of relevant documents that fail to be retrieved within the top 20 chunks". A labelled question set lets you do the same and add a second judgement: was the evidence retrieved, and was the answer right? The two together give four outcomes.

| Outcome | Meaning | Where to work |
|---|---|---|
| ok | The evidence was retrieved and the answer is right | Nowhere |
| generation | The evidence was retrieved and the answer is wrong | The prompt or the model |
| retrieval | The evidence was not retrieved and the answer is wrong | The cut, the context, the index, the freshness |
| unsupported | The answer is right although the evidence was not retrieved | Retrieval, because the answer rests on the model's memory or on luck |

The example runs eight questions: the evidence was retrieved for 5 and 5 answers were right, which looks like a balanced system. The split is 4 ok, 1 generation, 2 retrieval and 1 unsupported. So the pipeline has two retrieval failures and one generation failure, and one right answer that has no evidence behind it and will not stay right. Counting only correct answers hides all three. A question with no results counts as a miss in recall over the labelled set, and so does a question that was never answered: the denominator is every labelled question.

### Knowledge that changes: the prompt, retrieval or the weights

| Where the knowledge lives | Updating means | Strong when | Weak when |
|---|---|---|---|
| In the prompt, cached | Editing the text | The corpus is small, shared by every request and edited rarely | It grows past the window or every reader must not see all of it |
| In retrieval | Re-indexing the changed documents | The corpus is large, changes often, must cite its sources and is read under access rules | Answers need facts spread over many chunks, or retrieval misses |
| In the weights | Training again | A style, a format or a behaviour must hold with a short prompt | Facts change, a source must be shown or a reader's access must be respected |

Anthropic's glossary says that "Fine-tuning can be useful for adapting a language model to a specific domain, task, or writing style" and that "The Claude API does not currently offer fine-tuning". Facts that change weekly, belong to different readers and must be cited are retrieval's case: a weight cannot be edited for one document, cannot name its source and cannot be taken away from one reader. A small, stable body of rules shared by every request is the prompt's case, with caching to keep the repeated cost down (the default cache lifetime is 5 minutes and is refreshed at no extra cost each time the content is used, as module 20 covers).

### The example

The example is the one from page 1. Its second half is this page: the shortcut and the replacing re-index side by side, the stale check and the four outcomes of the eight questions. It ran offline in every language, with the same output.

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

### The practice: a pipeline graded on recall and staleness

The practice is in [`exercises/85-retrieval-pipelines-at-design-level`](../../exercises/85-retrieval-pipelines-at-design-level/unit-01/practice-1/statement.md). You write the pieces of a pipeline that the pages describe: chunks that carry their title, section and the version of their document, the split of a long section at sentence ends, a search that scores a code above plain words and never reads a document the caller may not see, the choice of mechanism, a re-index that keeps, replaces, adds and removes, the stale check, and recall over every labelled question. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see when it works.

## Traps

1. **"Add the new chunks of a changed document to the index."** It is tempting because adding is simple and the job reports success. The exam rejects it: the old chunk is a correct record of a past fact and competes with the new one. A changed document loses its old chunks first.
2. **"Retrieval recall is high, so the index is fresh."** It is tempting because recall is the metric the team already tracks. It fails because recall by chunk id counts an old chunk with the right id as a hit. Staleness needs its own check against the source versions.
3. **"Report end-to-end accuracy to find the weak layer."** It is tempting because it is one number that everyone understands. The exam rejects it: only retrieval and generation judged separately say where to work, and a right answer without its evidence is a defect that accuracy counts as a success.

## Quiz

1. A team reports a retrieval recall of nine in ten. It counts only the 30 labelled questions that returned something, and 27 of those had the relevant chunk in the top results. Ten more labelled questions came back empty. Which figure follows the page?
   - **a**: 90 percent, since only questions that returned something can be judged
   - **b**: 68 percent, since a blank query still belongs in the denominator
   - **c**: 100 percent, since every result that was returned held the relevant chunk
   - **d**: 75 percent, since 30 of the 40 labelled questions were answered

2. In a test of 8 questions, the evidence was retrieved for 5 and the answers were right for 5. One answer was wrong although its evidence was retrieved, and one was right although its evidence was not. How many of the questions have no retrieved evidence and a wrong answer?
   - **a**: Five
   - **b**: One
   - **c**: Three
   - **d**: Two

<details>
<summary>Answer key</summary>

1. **b**. The page says "the denominator is every labelled question", so 27 of 40 is about 68 percent. *a* is ruled out because "A question with no results counts as a miss in recall over the labelled set". *c* is ruled out because only 27 of the 30 returned results held the relevant chunk, and "the denominator is every labelled question" in any case. *d* is ruled out because "and so does a question that was never answered": answered questions are not the denominator.
2. **d**. The outcomes are 4 ok, 1 generation, 2 retrieval and 1 unsupported, so two questions had no evidence and a wrong answer. *b* is ruled out because the wrong answer that had its evidence is one where "The evidence was retrieved and the answer is wrong", a generation failure. *c* is ruled out because the right answer without evidence is one where "The answer is right although the evidence was not retrieved", a defect but not a retrieval failure by definition. *a* is ruled out because five is the number of questions whose evidence was retrieved ("the evidence was retrieved for 5"), the opposite count.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A legal-tech firm's assistant must answer from 3,000 agreements that are revised weekly, and every answer must cite its source. Clients may open only their own agreements. An engineer proposes fine-tuning a model on the agreements each month. Which design fits?
   - **a**: A model fine-tuned each month on the newest texts of the agreements, used with a short prompt
   - **b**: The whole set of agreements placed in a single cached prompt that is shared by the clients
   - **c**: Section chunks in a permission-filtered index, replaced when their document changes
   - **d**: A keyword index over each whole agreement, rebuilt from scratch each quarter by a batch job

2. On a labelled set of 8 questions, a pipeline scores 4 ok, 1 generation, 2 retrieval and 1 unsupported. Where should the next week of work go?
   - **a**: The prompt alone, since one answer was wrong although its evidence was retrieved
   - **b**: The cut, the index and the freshness, since three problem cases lack evidence
   - **c**: Nowhere in particular, since five of the eight answers are right
   - **d**: The unsupported case first, since a right answer needs no work at all

3. A team re-indexes by adding the chunks of every changed document. Recall on its labelled questions stays high, yet users keep reporting outdated answers. What does the team's measurement miss?
   - **a**: Whether the keyword index outweighs the embedding index when the query holds a code
   - **b**: Whether the model follows the prompt when the evidence is present in its context
   - **c**: Whether the labelled set holds enough questions about each topic of the support area
   - **d**: Whether the passage that was returned still matches what its source says now

<details>
<summary>Answer key</summary>

1. **c**. Frequent changes, citations and per-client access are retrieval's case, and a changed document must lose its old chunks. *a* is ruled out because in the table the weights are weak when "Facts change, a source must be shown or a reader's access must be respected", which describes this firm. *b* is ruled out because the prompt suits a corpus that is "small, shared by every request and edited rarely", and these agreements are revised weekly and read under access rules. *d* is ruled out because "A re-index that runs less often than the documents change leaves old text in the index between runs", and a quarterly rebuild leaves it for months.
2. **b**. The table sends retrieval outcomes to "The cut, the context, the index, the freshness". *a* is ruled out because "the pipeline has two retrieval failures and one generation failure". *c* is ruled out because "Counting only correct answers hides all three". *d* is ruled out because the table sends it to "Retrieval, because the answer rests on the model's memory or on luck".
3. **d**. Recall by chunk id counts an old chunk with the right id as a hit, so staleness needs a check against the source. *b* is ruled out because "the only thing that changed is what retrieval can return", so the old text is already in the evidence before the prompt is read. *c* is ruled out because the problem is what the metric counts, as "an old chunk with the right id still counts as a hit", and more questions would count the same way. *a* is ruled out because the weighting of codes decides ranking among current chunks, since "a question that names an identifier is the case where the keyword index is right", and it does not decide whether an old chunk is returned.

</details>

Question 1 of the first quiz is adapted from sample item 3 of the Claude Certified Architect - Professional exam guide (Anthropic, version 1.0), which the guide offers as an illustration of item style.
