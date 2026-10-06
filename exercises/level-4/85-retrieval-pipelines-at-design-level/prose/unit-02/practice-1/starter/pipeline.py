"""A retrieval pipeline: chunks that carry their context, a search that respects access, rank fusion, a re-index that removes what changed, and recall over every question. See ../../statement.md."""
import logging
import re
from collections import namedtuple

log = logging.getLogger(__name__)

Chunk = namedtuple("Chunk", "id doc version text")
STOP = {"a", "an", "the", "is", "are", "can", "i", "what", "does", "do", "how", "my", "it", "of", "to", "for", "and", "or", "in", "when", "will", "be", "that", "this", "with", "by", "at"}


def doc_version(text):
    """A cheap fingerprint of a document's text: when it changes, the document changed."""
    return sum(ord(c) for c in text) % 1000003


def tokens(text):
    return [t for t in re.findall(r"[a-z0-9]+(?:-[a-z0-9]+)*", text.lower()) if t not in STOP]


def _chunk_text(title, name, part):
    """TODO 1 of 8 (unlocks m1): the text of one chunk.

    Receives the document title, the section name and one part of the section's text. Returns the part with its context in front, as
    "<title> > <name>. <part>", so a chunk still says where it came from.
    Example: _chunk_text("Annual plan", "Cancellation", "You can cancel.") -> "Annual plan > Cancellation. You can cancel."
    """
    return part


def _split_section(body, max_words):
    """TODO 2 of 8 (unlocks e1): the parts of a long section.

    Receives the text of one section and the word limit. Splits the text after each sentence end (a full stop followed by a space) and fills parts
    with whole sentences: a part is closed before the sentence that would push it over `max_words` words, and a single sentence longer than the limit
    stays whole. Returns the parts as a list of strings (a short section is one part).
    Example: "Install it. Configure it. Restart it." with max_words 4 -> ["Install it. Configure it.", "Restart it."]
    """
    return [body]


def chunk_sections(doc_id, text, max_words=30):
    log.debug("chunk_sections input: %r", text)
    head, *sections = text.split("\n## ")
    title = head.removeprefix("# ")
    version = doc_version(text)
    chunks = []
    for section in sections:
        name, body = section.split("\n", 1)
        parts = _split_section(body, max_words)
        for n, part in enumerate(parts, 1):
            suffix = "" if len(parts) == 1 else f"#{n}"
            chunks.append(Chunk(f"{doc_id}/{name}{suffix}", doc_id, version, _chunk_text(title, name, part)))
    return chunks


def _score(wanted, have):
    """TODO 3 of 8 (unlocks e2): how well a chunk matches a query.

    Receives the set of query words and the set of words of one chunk. Returns the sum, over the query words that the chunk has, of 3 for a code (a word that
    contains a digit, such as E-7310) and 1 for any other word; 0 when the chunk has none of them.
    Example: wanted {"cancel", "e-7310"}, have {"cancel", "e-7310", "card"} -> 4
    """
    return 0


def _visible(chunk, allowed_docs):
    """TODO 4 of 8 (unlocks e3): may this reader see this chunk?

    Receives a chunk and the set of document ids the reader may read, or None when the reader may read all of them. Returns True when the chunk's `doc` is allowed.
    Example: _visible(chunk of "annual", {"monthly"}) -> False, _visible(chunk of "annual", None) -> True
    """
    return True


def search(chunks, query, k=3, allowed_docs=None):
    log.debug("search input: %r", query)
    wanted = set(tokens(query))
    scored = []
    for n, chunk in enumerate(chunks):
        if not _visible(chunk, allowed_docs):
            continue
        score = _score(wanted, set(tokens(chunk.text)))
        if score:
            scored.append((-score, n, chunk.id))
    return [chunk_id for _, _, chunk_id in sorted(scored)[:k]]


def choose_retrieval(corpus_tokens, shape, pattern):
    """TODO 5 of 8 (unlocks e4): the retrieval mechanism.

    Receives the corpus size in tokens, the data shape ("text" or "table") and the query pattern. Decide in this order: under 200000 tokens "cached prompt";
    a table "structured query"; a "multi-hop" pattern "agentic search"; then "identifier" gives "keyword index", "paraphrase" gives "embedding index" and any other pattern "hybrid index".
    Example: choose_retrieval(2000000, "text", "identifier") -> "keyword index"
    """
    return None


def _status(old_chunks, text):
    """TODO 6 of 8 (unlocks e5): what a re-index does with one document.

    Receives the chunks the index already holds for the document (an empty list when it has none) and the document's current text. Returns "added" when
    there are no chunks, "kept" when the version of the first chunk equals `doc_version(text)`, and "replaced" when it differs.
    Example: no chunks -> "added"; chunks made from the same text -> "kept"
    """
    return "added"


def reindex(chunks, docs):
    old = {}
    for chunk in chunks:
        old.setdefault(chunk.doc, []).append(chunk)
    report = {"added": [], "replaced": [], "removed": [d for d in old if d not in docs], "kept": []}
    result = []
    for doc_id, text in docs.items():
        status = _status(old.get(doc_id, []), text)
        report[status].append(doc_id)
        result += old[doc_id] if status == "kept" else chunk_sections(doc_id, text)
    return result, report


def stale(chunks, docs):
    """TODO 7 of 8 (unlocks e6): the chunks that no longer match their source.

    Receives the chunks and a dict of the current documents (id to text). Returns the ids of the chunks, in order, whose document is gone or whose
    version differs from `doc_version` of the current text.
    Example: a chunk of a document that is no longer in `docs` is stale
    """
    return None


def recall_at_k(results, relevant, k):
    """TODO 8 of 8 (unlocks e7): the share of questions answered in the first k results.

    Receives `results` (question to the list of chunk ids returned) and `relevant` (every labelled question to the chunk id that answers it). Returns the
    number of labelled questions whose relevant id is among the first k results, divided by the number of labelled questions, rounded to two decimals;
    a question with no results counts as a miss, and 0.0 when there are no labelled questions.
    Example: 2 of 3 questions hit -> 0.67
    """
    return None
