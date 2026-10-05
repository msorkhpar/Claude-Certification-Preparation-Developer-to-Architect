# Case lists of module 28-retrieval: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/28-retrieval/unit-01/practice-1"] = {
    "name": "retrieval", "suite": "RetrievalTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "hybrid search finds what each single index misses"),
        ("e1", "edge", "chunks overlap and end at the last word"),
        ("e2", "edge", "a rare word outweighs common ones and short chunks win"),
        ("e3", "edge", "fusion rewards agreement and breaks ties by id"),
        ("e4", "edge", "reranking orders the pool by the scorer"),
        ("e5", "edge", "recall counts documents not chunks"),
        ("e6", "edge", "a context sentence makes a bare chunk findable"),
    ],
    "plants": {
        "wrong-no-overlap": (["e1"], "moves the window by its whole size, so the overlap is lost"),
        "wrong-no-idf": (["e2"], "scores every matching word the same, common or rare"),
        "wrong-fuse-by-votes": (["e3"], "counts the lists that hold an id and ignores its rank in them"),
        "wrong-rerank-ascending": (["e4"], "puts the lowest scored chunk first"),
        "wrong-pool-ignored": (["e4"], "reranks the whole ranking instead of the first pool ids"),
        "wrong-recall-by-chunk": (["e5"], "counts the chunks of a relevant document each time"),
        "wrong-context-ignored": (["e6"], "indexes the bare chunk text and drops the context sentence"),
    },
}
