import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from pipeline import choose_retrieval, chunk_sections, doc_version, recall_at_k, reindex, search, stale

PLAN = "# Annual plan\n## Cancellation\nYou can cancel within 14 days for a full refund."
MONTHLY = "# Monthly plan\n## Cancellation\nYou can cancel at any time."
ERRORS = "# Error codes\n## E-7310\nThe warehouse could not reserve stock.\n## E-4021\nThe payment gateway rejected the card."
LONG = "# Guide\n## Setup\nInstall the agent. Configure the proxy. Restart the service. Check the logs for errors."


def cs(*args, **kwargs):
    result = chunk_sections(*args, **kwargs)
    assert isinstance(result, list), "no chunks returned"
    return result


def texts(chunks):
    assert isinstance(chunks, list), "no chunks returned"
    return [(c.id, c.text) for c in chunks]


def test_m1_sections_become_chunks_that_carry_their_title_and_section_and_the_version_of_their_document():
    chunks = cs("annual", PLAN)
    assert texts(chunks) == [("annual/Cancellation", "Annual plan > Cancellation. You can cancel within 14 days for a full refund.")]
    assert chunks[0].doc == "annual" and chunks[0].version == doc_version(PLAN)
    assert [c.id for c in cs("errors", ERRORS)] == ["errors/E-7310", "errors/E-4021"]


def test_e1_a_long_section_splits_at_sentence_ends_under_the_word_limit_and_every_part_keeps_the_prefix():
    chunks = cs("guide", LONG, max_words=8)
    assert texts(chunks) == [
        ("guide/Setup#1", "Guide > Setup. Install the agent. Configure the proxy."),
        ("guide/Setup#2", "Guide > Setup. Restart the service. Check the logs for errors."),
    ]
    assert len(cs("guide", LONG, max_words=3)) == 4, "a sentence longer than the limit stays whole"


def test_e2_a_code_outweighs_a_word_and_a_word_in_no_chunk_matches_nothing():
    chunks = cs("errors", ERRORS) + cs("annual", PLAN)
    assert search(chunks, "what does E-7310 mean") == ["errors/E-7310"]
    assert search(chunks, "cancel E-4021 card") == ["errors/E-4021", "annual/Cancellation"]
    assert search(chunks, "card gateway E-7310") == ["errors/E-7310", "errors/E-4021"], "a code outweighs two plain words"
    assert search(chunks, "when will I be reimbursed") == []
    assert search(chunks, "cancel", k=1) == ["annual/Cancellation"]


def test_e3_a_search_for_a_reader_never_returns_a_chunk_of_a_document_that_reader_may_not_see():
    chunks = cs("monthly", MONTHLY) + cs("annual", PLAN)
    assert search(chunks, "cancel annual plan", k=1) == ["annual/Cancellation"]
    assert search(chunks, "cancel annual plan", k=1, allowed_docs={"monthly"}) == ["monthly/Cancellation"]
    assert search(chunks, "cancel annual plan", k=3, allowed_docs={"monthly"}) == ["monthly/Cancellation"]
    assert search(chunks, "cancel", allowed_docs=set()) == []


def test_e4_the_mechanism_follows_the_corpus_size_then_the_data_shape_then_the_query_pattern():
    assert choose_retrieval(50000, "text", "identifier") == "cached prompt"
    assert choose_retrieval(199999, "table", "multi-hop") == "cached prompt"
    assert choose_retrieval(200000, "text", "paraphrase") == "embedding index"
    assert choose_retrieval(2000000, "table", "paraphrase") == "structured query"
    assert choose_retrieval(2000000, "table", "multi-hop") == "structured query"
    assert choose_retrieval(2000000, "text", "multi-hop") == "agentic search"
    assert choose_retrieval(2000000, "text", "identifier") == "keyword index"
    assert choose_retrieval(2000000, "text", "mixed") == "hybrid index"


def test_e5_a_reindex_keeps_unchanged_documents_replaces_changed_ones_adds_new_ones_and_drops_removed_ones():
    old = cs("annual", PLAN) + cs("monthly", MONTHLY) + cs("errors", ERRORS)
    edited = PLAN.replace("14 days", "30 days")
    docs = {"annual": edited, "errors": ERRORS, "guide": LONG}
    result = reindex(old, docs)
    assert isinstance(result, tuple), "reindex returned nothing"
    new, report = result
    assert report == {"added": ["guide"], "replaced": ["annual"], "removed": ["monthly"], "kept": ["errors"]}
    assert [c.id for c in new] == ["annual/Cancellation", "errors/E-7310", "errors/E-4021", "guide/Setup"]
    assert [c.text for c in new if c.doc == "annual"] == ["Annual plan > Cancellation. You can cancel within 30 days for a full refund."]
    assert all(c.doc != "monthly" for c in new)


def test_e6_stale_lists_the_chunks_whose_document_changed_or_vanished():
    old = cs("annual", PLAN) + cs("monthly", MONTHLY) + cs("errors", ERRORS)
    docs = {"annual": PLAN.replace("14 days", "30 days"), "errors": ERRORS}
    assert stale(old, docs) == ["annual/Cancellation", "monthly/Cancellation"]
    result = reindex(old, docs)
    assert isinstance(result, tuple), "reindex returned nothing"
    assert stale(result[0], docs) == []
    assert stale(old, {"annual": PLAN, "monthly": MONTHLY, "errors": ERRORS}) == []


def test_e7_recall_counts_every_labelled_question_and_a_question_with_no_results_is_a_miss():
    relevant = {"q1": "a", "q2": "b", "q3": "c"}
    results = {"q1": ["a", "x"], "q2": ["x", "y", "b"]}
    assert recall_at_k(results, relevant, 3) == 0.67
    assert recall_at_k(results, relevant, 1) == 0.33
    assert recall_at_k({}, relevant, 3) == 0.0
    assert recall_at_k(results, {}, 3) == 0.0
