"""Design decisions around a retrieval pipeline: where a document is cut, what a chunk carries, which index answers which query, and what a re-index must remove.

The corpus is four short documents. The "semantic" rankings are scripted: they stand in for an embedding index, which this course does not build (the Claude documentation says Anthropic
offers no embedding model and points to a provider). What the code shows is the design around that index. Read on 2026-10-04 against the Anthropic post on contextual retrieval and the Claude
documentation page "Embeddings". Nothing here calls a model.
"""
import re

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
