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
    # TODO: one chunk per section, its text starting with "<title> > <section>. ", split at sentence ends when a section is longer than max_words.
    return None


def search(chunks, query, k=3, allowed_docs=None):
    # TODO: the ids of the k best chunks for the query among the documents the caller may read.
    return None


def choose_retrieval(corpus_tokens, shape, pattern):
    # TODO: the retrieval mechanism for a corpus of this size, this data shape ("text" or "table") and this query pattern.
    return None


def reindex(chunks, docs):
    # TODO: bring the chunks in line with the documents and report what was kept, replaced, added and removed.
    return None


def stale(chunks, docs):
    # TODO: the ids of the chunks that no longer match their source.
    return None


def recall_at_k(results, relevant, k):
    # TODO: the share of all labelled questions whose relevant chunk is among the first k results.
    return None
