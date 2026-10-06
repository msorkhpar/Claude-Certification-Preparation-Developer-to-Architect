import json
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from retrieval import bm25_rank, build_chunks, chunk, embed, embedding_rank, evaluate, fuse, recall_at_k, rerank, retrieve

FIXTURE = json.loads((Path(__file__).resolve().parent / "fixture.json").read_text())


def c(id, text):
    return {"id": id, "doc": id.split("#")[0], "text": text}


def failure_of(fn):
    """'ValueError' when fn raises it, 'crash' for another exception, None when it returns."""
    try:
        fn()
    except ValueError:
        return "ValueError"
    except Exception:  # noqa: BLE001
        return "crash"
    return None


def close(a, b):
    return a is not None and abs(a - b) < 1e-9


def test_m1_hybrid_search_finds_what_each_single_index_misses():
    chunks = build_chunks(FIXTURE["corpus"], FIXTURE["chunk_size"], FIXTURE["overlap"]) or []
    queries = FIXTURE["queries"]
    lexical = evaluate(chunks, queries, "bm25", 3)
    semantic = evaluate(chunks, queries, "embedding", 3)
    hybrid = evaluate(chunks, queries, "hybrid", 3)
    assert (close(lexical, 0.5), close(semantic, 0.75), close(hybrid, 1.0)) == (True, True, True), (lexical, semantic, hybrid)


def test_e1_chunks_overlap_and_end_at_the_last_word():
    words = " ".join(f"w{i}" for i in range(1, 11))
    assert chunk(words, 4, 1) == ["w1 w2 w3 w4", "w4 w5 w6 w7", "w7 w8 w9 w10"]
    assert chunk(words, 4, 0) == ["w1 w2 w3 w4", "w5 w6 w7 w8", "w9 w10"]
    assert chunk("a b c d e f g", 4, 1) == ["a b c d", "d e f g"]
    assert chunk("a b c", 5, 2) == ["a b c"]
    assert chunk("a b c d", 4, 1) == ["a b c d"]
    assert chunk("", 3, 1) == [] and chunk("   ", 3, 1) == []
    assert [failure_of(lambda: chunk("a b", 2, 2)), failure_of(lambda: chunk("a b", 0, 0)), failure_of(lambda: chunk("a b", 2, -1))] == ["ValueError"] * 3
    made = build_chunks([{"id": "doc", "text": "a b c d e"}, {"id": "other", "text": "x y"}], 3, 1) or []
    assert made == [{"id": "doc#0", "doc": "doc", "text": "a b c"}, {"id": "doc#1", "doc": "doc", "text": "c d e"}, {"id": "other#0", "doc": "other", "text": "x y"}]


def test_e2_a_rare_word_outweighs_common_ones_and_short_chunks_win():
    chunks = [c("x#0", "alpha beta"), c("d1#0", "alpha beta delta"), c("d2#0", "alpha beta epsilon"), c("d3#0", "alpha beta zeta"),
              c("d4#0", "alpha beta eta"), c("y#0", "gamma theta")]
    assert bm25_rank(chunks, "alpha beta gamma") == ["y#0", "x#0", "d1#0", "d2#0", "d3#0", "d4#0"]
    shorter = [c("long#0", "needle alpha beta gamma delta"), c("short#0", "needle alpha"), c("none#0", "other words")]
    assert bm25_rank(shorter, "needle") == ["short#0", "long#0"]
    assert bm25_rank(shorter, "absent terms") == []
    assert bm25_rank(shorter, "Needle NEEDLE") == ["short#0", "long#0"]
    assert bm25_rank(shorter, "needle", {"none#0": "needle needle needle"})[0] == "none#0"


def test_e3_fusion_rewards_agreement_and_breaks_ties_by_id():
    assert fuse([["x", "y", "z"], ["z", "x", "w"]]) == ["x", "z", "y", "w"]
    assert fuse([["p", "q"], ["r", "q"]]) == ["q", "p", "r"]
    assert fuse([["b"], ["a"]]) == ["a", "b"]
    assert fuse([["x", "x", "y"]]) == ["x", "y"]
    assert fuse([["a", "b"], ["b", "c"]], 1) == ["b", "a", "c"]
    assert fuse([]) == []


def test_e4_reranking_orders_the_pool_by_the_scorer():
    texts = {"a": "x", "b": "y", "c": "z", "d": "w"}
    table = {"x": 0.2, "y": 0.9, "z": 0.9, "w": 1.0}

    def scorer(query, text):
        return table[text]

    assert rerank("q", ["a", "b", "c"], texts, scorer, 2) == ["b", "c"]
    assert rerank("q", ["a", "b", "c"], texts, scorer) == ["b", "c", "a"]
    chunks = [c("c1#0", "alpha"), c("c2#0", "alpha one"), c("c3#0", "alpha one two"), c("c4#0", "alpha one two three")]
    prefer_last = {"alpha one two three": 1.0, "alpha one two": 0.5}

    def by_table(query, text):
        return prefer_last.get(text, 0.0)

    assert bm25_rank(chunks, "alpha") == ["c1#0", "c2#0", "c3#0", "c4#0"]
    assert retrieve(chunks, "alpha", "bm25", 1, 2, None, by_table) == ["c1#0"]
    assert retrieve(chunks, "alpha", "bm25", 1, 4, None, by_table) == ["c4#0"]
    assert retrieve(chunks, "alpha", "bm25", 2) == ["c1#0", "c2#0"]


def test_e5_recall_counts_documents_not_chunks():
    doc_of = {"a#0": "A", "a#1": "A", "b#0": "B", "c#0": "C"}
    ids = ["a#0", "a#1", "b#0", "c#0"]
    assert close(recall_at_k(ids, doc_of, ["A", "B"], 2), 0.5)
    assert close(recall_at_k(ids, doc_of, ["A", "B"], 3), 1.0)
    assert close(recall_at_k(ids, doc_of, ["A", "Z"], 4), 0.5)
    assert close(recall_at_k(ids, doc_of, ["B"], 0), 0.0)
    assert close(recall_at_k(ids, doc_of, ["A"], 10), 1.0)
    assert failure_of(lambda: recall_at_k(ids, doc_of, [], 3)) == "ValueError"


def test_e6_a_context_sentence_makes_a_bare_chunk_findable():
    chunks = [c("a#0", "The limit is 30 days after delivery."), c("b#0", "The limit is 5 users per workspace."), c("c#0", "Our mascot is a friendly otter.")]
    contexts = {"a#0": "Returns policy: the return window for physical orders."}
    assert retrieve(chunks, "return window", "bm25", 2) == []
    assert retrieve(chunks, "return window", "bm25", 2, contexts=contexts) == ["a#0"]
    doc_of = {"a#0": "a", "b#0": "b", "c#0": "c"}
    queries = [{"query": "return window", "relevant": ["a"]}]
    assert close(evaluate(chunks, queries, "bm25", 1), 0.0)
    assert close(evaluate(chunks, queries, "bm25", 1, contexts=contexts), 1.0)
    assert chunks[0]["text"] == "The limit is 30 days after delivery."
    assert len(embed("x")) == 64
