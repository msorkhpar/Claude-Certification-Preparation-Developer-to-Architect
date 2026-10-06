"""A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md."""
import logging
import math
import re

log = logging.getLogger(__name__)


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
    return _windows(text.split(), size, overlap)


def _windows(words, size, overlap):
    """TODO 1 of 7 (finish this to pass e1): the windows over a list of words.

    Receives the words, size and overlap (already checked). Returns the windows as strings of the words joined by one space: each holds
    `size` words, the next starts `size - overlap` words later, and the loop stops at the first window that reaches the last word (so the
    last window ends at the last word and none is repeated). No words gives [].
    Example: _windows("a b c d e".split(), 3, 1) -> ["a b c", "c d e"]
    """
    return []


def build_chunks(corpus, size, overlap):
    """[{"id": "<doc>#<n>", "doc", "text"}] for every document of [{"id", "text"}], in order."""
    return [{"id": f"{doc['id']}#{n}", "doc": doc["id"], "text": piece}
            for doc in corpus for n, piece in enumerate(chunk(doc["text"], size, overlap))]


def _texts(chunks, index_text):
    return {c["id"]: (index_text or {}).get(c["id"], c["text"]) for c in chunks}


def _ordered(scores):
    return [cid for cid, score in sorted(scores.items(), key=lambda kv: (-kv[1], kv[0])) if score > 0]


def _term_score(tf, df, n, length, average, k1, b):
    """TODO 2 of 7 (finish this to pass e2): the BM25 score one query term gives one chunk.

    Receives the term's count in the chunk (tf, at least 1), the number of chunks that hold the term (df), the number of chunks (n), the chunk's
    length in tokens, the average length, k1 and b. Returns idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * length / average)) with
    idf = ln(1 + (n - df + 0.5) / (df + 0.5)). A rare term has a large idf; a short chunk gets a larger score than a long one.
    Example: _term_score(1, 1, 4, 5, 5.0, 1.5, 0.75) is about 1.2 times ln(1 + 3.5 / 1.5)
    """
    return 0.0


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
            score += _term_score(tf, df[term], n, len(tokens), average, k1, b)
        scores[cid] = score
    return _ordered(scores)


def _dot(a, b):
    """TODO 3 of 7 (finish this to pass m1): the dot product of two vectors of the same length.

    Receives two lists of numbers. Returns the sum of the products of the numbers in the same place (the vectors of embed() have length 1,
    so this is their cosine). Example: _dot([1.0, 2.0], [3.0, 4.0]) -> 11.0
    """
    return 0.0


def embedding_rank(chunks, query, index_text=None):
    """Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out."""
    q = embed(query)
    scores = {}
    for cid, text in _texts(chunks, index_text).items():
        scores[cid] = _dot(q, embed(text))
    return _ordered(scores)


def fuse(rankings, k=60):
    """Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat
    in one list ignored); best first, ties by id."""
    # TODO 4 of 7 (finish this to pass e3): reciprocal rank fusion of the lists in `rankings`.
    # Returns the ids best first: an id scores the sum of 1 / (k + rank) over the lists that hold it (rank starts at 1, a repeat in one list is
    # ignored); _ordered(scores) sorts a dict of scores best first, ties by id, and leaves out scores of 0 or less.
    # Example: fuse([["a", "b"], ["b", "c"]]) -> ["b", "a", "c"]
    return []


def rerank(query, ids, texts, scorer, top_n=None):
    """The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to top_n."""
    # TODO 5 of 7 (finish this to pass e4): the ids ordered by scorer(query, texts[id]) high to low.
    # Equal scores keep their input order (a stable sort); cut to top_n when it is not None. Only the given ids take part.
    # Example: rerank("q", ["a", "b"], {"a": "x", "b": "xx"}, lambda q, t: len(t)) -> ["b", "a"]
    return list(ids)


def recall_at_k(ids, doc_of, relevant, k):
    """The share of the relevant documents that appear among the documents of the first k chunk ids."""
    if not relevant:
        raise ValueError("relevant must not be empty")
    # TODO 6 of 7 (finish this to pass e5): the share of the relevant documents found among the documents of the first k chunk ids.
    # A document counts once however many of its chunks are there; `relevant` is not empty here. Returns a number from 0 to 1.
    # Example: recall_at_k(["a#0", "a#1", "b#0"], {"a#0": "a", "a#1": "a", "b#0": "b"}, ["a", "c"], 3) -> 0.5
    return 0.0


def _indexed_text(chunks, contexts):
    """TODO 7 of 7 (finish this to pass e6): the text each chunk is indexed under when it has a context sentence.

    Receives the chunks and `contexts` (None or a dict from chunk id to a sentence). Returns a dict from chunk id to the sentence, a space
    and the chunk's text, for the chunks that have a sentence only; the chunks themselves are not changed.
    Example: _indexed_text([{"id": "a#0", "text": "x"}], {"a#0": "About a."}) -> {"a#0": "About a. x"}
    """
    return {}


def retrieve(chunks, query, mode="hybrid", k=3, pool=10, contexts=None, scorer=None):
    """The ids of the best k chunks. mode is bm25, embedding or hybrid (fusion of both). `contexts` maps chunk ids to a
    sentence that is indexed in front of the chunk's text; `scorer` reranks the first `pool` ids of the ranking."""
    log.debug("retrieve input: mode=%s k=%s query=%r", mode, k, query)
    index_text = _indexed_text(chunks, contexts)
    lexical = bm25_rank(chunks, query, index_text)
    semantic = embedding_rank(chunks, query, index_text)
    ranked = lexical if mode == "bm25" else semantic if mode == "embedding" else fuse([lexical, semantic])
    if scorer is None:
        return ranked[:k]
    return rerank(query, ranked[:pool], {c["id"]: c["text"] for c in chunks}, scorer, k)


def evaluate(chunks, queries, mode="hybrid", k=3, **options):
    """The mean recall@k over [{"query", "relevant": [doc ids]}]."""
    doc_of = {c["id"]: c["doc"] for c in chunks}
    scores = [recall_at_k(retrieve(chunks, q["query"], mode, k, **options), doc_of, q["relevant"], k) for q in queries]
    return sum(scores) / len(scores)
