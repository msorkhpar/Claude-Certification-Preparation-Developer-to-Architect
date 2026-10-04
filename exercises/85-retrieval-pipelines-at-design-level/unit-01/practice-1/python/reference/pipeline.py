"""A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md."""
import re
from collections import namedtuple

Chunk = namedtuple("Chunk", "id doc version text")
STOP = {"a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"}


def doc_version(text):
    """A cheap fingerprint of a document's text: when it changes, the document changed."""
    return sum(ord(c) for c in text) % 1000003


def tokens(text):
    return [t for t in re.findall(r"[a-z0-9]+(?:-[a-z0-9]+)*", text.lower()) if t not in STOP]


def chunk_sections(doc_id, text, max_words=30):
    head, *sections = text.split("\n## ")
    title = head.removeprefix("# ")
    version = doc_version(text)
    chunks = []
    for section in sections:
        name, body = section.split("\n", 1)
        parts, current = [], []
        for sentence in re.split(r"(?<=\.) ", body):
            if current and len(" ".join(current + [sentence]).split()) > max_words:
                parts.append(" ".join(current))
                current = []
            current.append(sentence)
        parts.append(" ".join(current))
        for n, part in enumerate(parts, 1):
            suffix = "" if len(parts) == 1 else f"#{n}"
            chunks.append(Chunk(f"{doc_id}/{name}{suffix}", doc_id, version, f"{title} > {name}. {part}"))
    return chunks


def search(chunks, query, k=3, allowed_docs=None):
    wanted = set(tokens(query))
    scored = []
    for n, chunk in enumerate(chunks):
        if allowed_docs is not None and chunk.doc not in allowed_docs:
            continue
        have = set(tokens(chunk.text))
        score = sum(3 if any(c.isdigit() for c in t) else 1 for t in wanted if t in have)
        if score:
            scored.append((-score, n, chunk.id))
    return [chunk_id for _, _, chunk_id in sorted(scored)[:k]]


def choose_retrieval(corpus_tokens, shape, pattern):
    if corpus_tokens < 200000:
        return "cached prompt"
    if shape == "table":
        return "structured query"
    if pattern == "multi-hop":
        return "agentic search"
    return {"identifier": "keyword index", "paraphrase": "embedding index"}.get(pattern, "hybrid index")


def reindex(chunks, docs):
    old = {}
    for chunk in chunks:
        old.setdefault(chunk.doc, []).append(chunk)
    report = {"added": [], "replaced": [], "removed": [d for d in old if d not in docs], "kept": []}
    result = []
    for doc_id, text in docs.items():
        if doc_id in old and old[doc_id][0].version == doc_version(text):
            report["kept"].append(doc_id)
            result += old[doc_id]
        else:
            report["replaced" if doc_id in old else "added"].append(doc_id)
            result += chunk_sections(doc_id, text)
    return result, report


def stale(chunks, docs):
    return [c.id for c in chunks if c.doc not in docs or c.version != doc_version(docs[c.doc])]


def recall_at_k(results, relevant, k):
    if not relevant:
        return 0.0
    hits = sum(1 for query, chunk_id in relevant.items() if chunk_id in results.get(query, [])[:k])
    return round(hits / len(relevant), 2)
