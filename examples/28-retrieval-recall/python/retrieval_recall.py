"""A retrieval pipeline measured on recall: BM25, a toy embedding, rank fusion, a reranker and contextual indexing.

Everything is local and deterministic. The embedding is a TOY (hashed letter trigrams, no model), and the reranker is a
hand-written scoring function standing in for a reranking model. The knowledge base is invented. No API is called.
"""
import logging
import math
import re

log = logging.getLogger(__name__)

CORPUS = [
    ('refunds', 'Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the original payment method within five business days. Digital goods are not refundable after download.'),
    ('shipping', 'Shipping times: standard delivery takes three to five business days. Express delivery arrives the next day. Orders over fifty dollars ship free to domestic addresses.'),
    ('password', 'To reset a forgotten password, open the sign in page and choose forgot password. A reset link is emailed to you and expires after one hour.'),
    ('payment-errors', 'Error E-4012 means the payment gateway rejected the card. Check the billing address and try again, or use another card. Error E-4013 means the card has expired.'),
    ('warranty', 'All hardware carries a two year warranty that covers manufacturing defects. Accidental damage is covered only with the protection plan.'),
    ('deletion', 'You can delete your account from the privacy settings. Deletion removes personal data within thirty days and cannot be undone.'),
    ('api-limits', 'The API allows sixty requests per minute for each key. Exceeding the limit returns status 429 with a retry-after header.'),
    ('invoices', 'Invoices are generated on the first day of each month and sent as PDF attachments. Past invoices can be downloaded from the billing page.'),
    ('returns', 'To return a physical item, print the prepaid label from your orders page and drop the parcel at any carrier point. Returns must arrive within fourteen days and items must be unused.'),
    ('international', 'International orders ship with a tracked carrier and take seven to twelve business days. Customs duties are paid by the recipient and are not included in the order total.'),
    ('billing', 'Your plan renews automatically each month on the billing date. You can switch plans or cancel renewal from the billing page before the date.'),
    ('api-keys', 'Create API keys in the developer console. Rotate a key by creating a new one and deleting the old one. Keys are shown only once, so store them safely.'),
    ('warranty-claims', 'Warranty claims. Hardware owners can open a claim from the support portal. Send the serial number and a photo of the damage within thirty days. We reply within two business days.'),
]

QUERIES = [
    ('send back parcel', ['returns']),
    ('free shipping threshold', ['shipping']),
    ('tracked carrier', ['international']),
    ('deleting accounts', ['deletion']),
    ('resetting passwords', ['password']),
    ('monthly invoicing', ['invoices']),
    ('deliveries arrive', ['shipping']),
    ('refund', ['refunds']),
]

CHUNK_WORDS, OVERLAP = 14, 4


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
            scores[cid] = scores.get(cid, 0.0) + 1.0 / (k + rank)
    return _ordered(scores)


def rerank(query, ids, texts, scorer, top_n=None):
    """The given ids reordered by scorer(query, text) high to low (equal scores keep their input order), cut to top_n."""
    ordered = sorted(ids, key=lambda cid: -scorer(query, texts[cid]))  # sorted is stable
    return ordered if top_n is None else ordered[:top_n]


def doc_of(chunks):
    return {c["id"]: c["doc"] for c in chunks}


def first_rank(ids, chunks, relevant):
    """1-based position of the first chunk of a relevant document, or None."""
    docs = doc_of(chunks)
    return next((i for i, cid in enumerate(ids, start=1) if docs[cid] in relevant), None)


def recall(ids, chunks, relevant, k):
    return len({doc_of(chunks)[cid] for cid in ids[:k]} & set(relevant)) / len(set(relevant))


def rankers(chunks, query):
    lexical, semantic = bm25_rank(chunks, query), embedding_rank(chunks, query)
    return {"bm25": lexical, "embedding": semantic, "hybrid": fuse([lexical, semantic])}


SYNONYMS = {"send": ["return", "returns"], "back": ["return", "returns"]}


def toy_reranker(query, text):
    """Stands in for a reranking model: it reads the query with a tiny synonym list added and scores the share of those words that the chunk holds."""
    words = []
    for w in tokenize(query):
        words += [w] + SYNONYMS.get(w, [])
    words = list(dict.fromkeys(words))
    have = set(tokenize(text))
    return sum(w in have for w in words) / len(words)


def main():
    chunks = [{"id": f"{doc}#{n}", "doc": doc, "text": piece} for doc, text in CORPUS for n, piece in enumerate(chunk(text, CHUNK_WORDS, OVERLAP))]
    print(f"{len(CORPUS)} documents, {len(chunks)} chunks of at most {CHUNK_WORDS} words, overlap {OVERLAP}")
    print(f"{'rank of the first relevant chunk':34}{'bm25':>6}{'embedding':>11}{'hybrid':>8}")
    totals = {"bm25": 0.0, "embedding": 0.0, "hybrid": 0.0}
    for query, relevant in QUERIES:
        ranked = rankers(chunks, query)
        cells = {mode: first_rank(ids, chunks, relevant) for mode, ids in ranked.items()}
        for mode, ids in ranked.items():
            totals[mode] += recall(ids, chunks, relevant, 3) / len(QUERIES)
        print(f"{query:34}{cells['bm25'] or '-':>6}{cells['embedding'] or '-':>11}{cells['hybrid'] or '-':>8}")
    print("mean recall@3:", {mode: round(value, 3) for mode, value in totals.items()})
    texts = {c["id"]: c["text"] for c in chunks}
    query = "send back parcel"
    hybrid = rankers(chunks, query)["hybrid"]
    reranked = rerank(query, hybrid[:10], texts, toy_reranker, 3)
    print(f"{query!r}: hybrid top 3 {hybrid[:3]} -> reranked top 3 {reranked}")
    bare = [{"id": "a#0", "doc": "a", "text": "The limit is 30 days after delivery."}, {"id": "b#0", "doc": "b", "text": "The limit is 5 users per workspace."}]
    context = {"a#0": "Returns policy: the return window for physical orders. "}
    indexed = {c["id"]: context.get(c["id"], "") + c["text"] for c in bare}
    print("'return window' on the bare chunks:", bm25_rank(bare, "return window"), "| with a context sentence indexed in front:", bm25_rank(bare, "return window", indexed))


if __name__ == "__main__":
    main()
