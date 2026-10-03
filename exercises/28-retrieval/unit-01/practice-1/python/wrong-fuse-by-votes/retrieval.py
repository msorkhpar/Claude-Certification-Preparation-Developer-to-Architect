"""A retrieval pipeline: chunking, lexical and embedding search, fusion, reranking and recall. See ../../statement.md."""
import math
import re


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
    words = text.split()
    chunks, start = [], 0
    while start < len(words):
        chunks.append(" ".join(words[start:start + size]))
        if start + size >= len(words):
            break
        start += size - overlap
    return chunks


def build_chunks(corpus, size, overlap):
    """[{"id": "<doc>#<n>", "doc", "text"}] for every document of [{"id", "text"}], in order."""
    return [{"id": f"{doc['id']}#{n}", "doc": doc["id"], "text": piece}
            for doc in corpus for n, piece in enumerate(chunk(doc["text"], size, overlap))]


def _texts(chunks, index_text):
    return {c["id"]: (index_text or {}).get(c["id"], c["text"]) for c in chunks}


def _ordered(scores):
    return [cid for cid, score in sorted(scores.items(), key=lambda kv: (-kv[1], kv[0])) if score > 0]


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
            idf = math.log(1 + (n - df[term] + 0.5) / (df[term] + 0.5))
            score += idf * tf * (k1 + 1) / (tf + k1 * (1 - b + b * len(tokens) / average))
        scores[cid] = score
    return _ordered(scores)


def embedding_rank(chunks, query, index_text=None):
    """Chunk ids by cosine similarity of embed() vectors, best first, ties by id, zero similarity left out."""
    q = embed(query)
    scores = {}
    for cid, text in _texts(chunks, index_text).items():
        total = 0.0
        for a, b in zip(q, embed(text)):
            total += a * b
        scores[cid] = total
    return _ordered(scores)


def fuse(rankings, k=60):
    """Reciprocal rank fusion: each id scores the sum of 1 / (k + rank) over the lists that hold it (rank from 1, a repeat
    in one list ignored); best first, ties by id."""
    scores = {}
    for ranking in rankings:
        for rank, cid in enumerate(dict.fromkeys(ranking), start=1):
            scores[cid] = scores.get(cid, 0.0) + 1.0
    return _ordered(scores)


def rerank(query, ids, texts, scorer, top_n=None):
    """The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to top_n."""
    ordered = sorted(ids, key=lambda cid: -scorer(query, texts[cid]))  # sorted is stable
    return ordered if top_n is None else ordered[:top_n]


def recall_at_k(ids, doc_of, relevant, k):
    """The share of the relevant documents that appear among the documents of the first k chunk ids."""
    if not relevant:
        raise ValueError("relevant must not be empty")
    found = {doc_of[cid] for cid in ids[:k]}
    return len(found & set(relevant)) / len(set(relevant))


def retrieve(chunks, query, mode="hybrid", k=3, pool=10, contexts=None, scorer=None):
    """The ids of the best k chunks. mode is bm25, embedding or hybrid (fusion of both). `contexts` maps chunk ids to a
    sentence that is indexed in front of the chunk's text; `scorer` reranks the first `pool` ids of the ranking."""
    index_text = {c["id"]: f"{contexts[c['id']]} {c['text']}" for c in chunks if contexts and c["id"] in contexts}
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
