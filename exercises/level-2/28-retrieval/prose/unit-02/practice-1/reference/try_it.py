"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from retrieval import build_chunks, evaluate, retrieve

# A tiny corpus like the test fixture: two documents, cut into windows of 14 words that overlap by 4.
corpus = [
    {"id": "refunds", "text": "Refund policy: customers may request a refund within 30 days of purchase. Refunds are issued to the "
                              "original payment method within five business days. Digital goods are not refundable after download."},
    {"id": "shipping", "text": "Shipping: orders over 50 euros ship free of charge. Standard delivery takes three to five business "
                               "days, express delivery takes one day."},
]
chunks = build_chunks(corpus, 14, 4) or []
print("chunks:", [c["id"] for c in chunks])

query = "free shipping threshold"
for mode in ("bm25", "embedding", "hybrid"):
    print(f"{mode} top 3:", retrieve(chunks, query, mode, 3))

# Recall@3 over one question whose answer we know.
print("hybrid recall@3:", evaluate(chunks, [{"query": query, "relevant": ["shipping"]}], "hybrid", 3) if chunks else None)
