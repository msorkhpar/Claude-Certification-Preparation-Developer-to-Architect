from retrieval_recall import CORPUS, OVERLAP, CHUNK_WORDS, QUERIES, bm25_rank, chunk, embed, embedding_rank, first_rank, fuse, rankers, recall


def chunks():
    return [{"id": f"{doc}#{n}", "doc": doc, "text": piece} for doc, text in CORPUS for n, piece in enumerate(chunk(text, CHUNK_WORDS, OVERLAP))]


def mean_recall(mode, k=3):
    cs = chunks()
    return sum(recall(rankers(cs, q)[mode], cs, rel, k) for q, rel in QUERIES) / len(QUERIES)


def test_hybrid_beats_each_single_index_at_recall_3():
    assert (mean_recall("bm25"), mean_recall("embedding"), mean_recall("hybrid")) == (0.5, 0.75, 1.0)


def test_the_embedding_has_64_dimensions_and_length_one():
    v = embed("deleting accounts")
    assert len(v) == 64 and abs(sum(x * x for x in v) - 1.0) < 1e-9


def test_fusion_puts_a_chunk_found_by_both_lists_first():
    assert fuse([["a", "b"], ["c", "b"]])[0] == "b"


def test_a_query_with_no_shared_word_has_no_lexical_hit():
    assert bm25_rank(chunks(), "zzzz qqqq") == []
    assert first_rank(rankers(chunks(), "deleting accounts")["bm25"], chunks(), ["deletion"]) is None
