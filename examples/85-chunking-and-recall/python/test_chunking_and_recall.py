from chunking_and_recall import DOCS, choose_retrieval, chunk_fixed, chunk_sections, fuse, holds, index, layer, lexical, reindex_additive, reindex_replace, stale

RULE = "Items marked final sale cannot be returned, except when they arrive damaged."


def test_a_fixed_cut_splits_the_rule_from_its_exception_and_a_heading_cut_keeps_them_together():
    fixed = chunk_fixed("refunds", DOCS["refunds"], 12)
    assert len(fixed) == 4 and not holds(fixed, [c for c, _ in fixed], RULE)
    sections = chunk_sections("refunds", DOCS["refunds"], False)
    assert [c for c, _ in sections] == ["refunds/Eligibility", "refunds/Exceptions", "refunds/Process"] and holds(sections, ["refunds/Exceptions"], RULE)


def test_a_chunk_that_carries_its_title_and_section_can_be_told_from_its_twin():
    assert lexical(index(DOCS, False), "cancel the annual plan", 1) == ["monthly/Cancellation"]
    assert lexical(index(DOCS, True), "cancel the annual plan", 1) == ["annual/Cancellation"]
    assert chunk_sections("annual", DOCS["annual"], True)[0][1].startswith("Annual plan > Cancellation. You can cancel")


def test_a_code_outweighs_a_word_and_the_fusion_rewards_agreement():
    chunks = index(DOCS, True)
    assert lexical(chunks, "what does E-7310 mean") == ["errors/E-7310"]
    assert lexical(chunks, "when will I be reimbursed") == []
    assert fuse([["a", "b"], ["b", "c"]]) == ["b", "a", "c"]
    assert fuse([[], ["x"]]) == ["x"]


def test_adding_chunks_without_removing_the_old_ones_leaves_a_stale_answer_in_the_index():
    chunks = index(DOCS, True)
    edited = DOCS["refunds"].replace("within 30 days", "within 60 days")
    live = {**DOCS, "refunds": edited}
    assert stale(chunks, live) == ["refunds/Eligibility"]
    assert stale(reindex_additive(chunks, "refunds", edited), live) == ["refunds/Eligibility"]
    replaced = reindex_replace(chunks, "refunds", edited)
    assert stale(replaced, live) == [] and len(replaced) == len(chunks)


def test_the_mechanism_follows_the_size_of_the_corpus_then_the_shape_of_the_data_then_the_query():
    assert [choose_retrieval(n, s, p) for n, s, p in [(199999, "table", "multi-hop"), (200000, "table", "identifier"), (5000000, "text", "multi-hop"), (5000000, "text", "identifier"), (5000000, "text", "paraphrase"), (5000000, "text", "mixed")]] == [
        "cached prompt", "structured query", "agentic search", "keyword index", "embedding index", "hybrid index"]


def test_retrieval_and_generation_are_judged_apart():
    assert [layer(r, c) for r, c in [(True, True), (True, False), (False, False), (False, True)]] == ["ok", "generation", "retrieval", "unsupported"]
