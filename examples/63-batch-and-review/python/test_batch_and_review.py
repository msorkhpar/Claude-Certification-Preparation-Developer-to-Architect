import pytest

from batch_and_review import batch_entry, match_results, review_request, worst_case_wait


def test_the_worst_wait_is_an_interval_plus_the_window_plus_the_handling():
    assert worst_case_wait(4) == 30 and worst_case_wait(6) == 32 and worst_case_wait(1, window_hours=24, handling_hours=0) == 25


def test_a_custom_id_is_one_to_sixty_four_letters_digits_hyphens_or_underscores():
    assert batch_entry("invoice-0042_a", {})["custom_id"] == "invoice-0042_a"
    for bad in ("", "has space", "dot.dot", "x" * 65):
        with pytest.raises(ValueError):
            batch_entry(bad, {})
    assert batch_entry("x" * 64, {})["custom_id"] == "x" * 64


def test_stream_speed_and_a_zero_max_tokens_are_refused():
    for params in ({"stream": True}, {"speed": "fast"}, {"max_tokens": 0}):
        with pytest.raises(ValueError):
            batch_entry("a1", params)
    assert batch_entry("a1", {"stream": False, "max_tokens": 10})["params"] == {"stream": False, "max_tokens": 10}


def test_results_are_paired_by_custom_id_whatever_their_order():
    matched, orphans = match_results(["a1", "a2", "a3"], [("a2", "expired"), ("z9", "succeeded"), ("a1", "succeeded")])
    assert matched == [("a1", "succeeded"), ("a2", "expired"), ("a3", "missing")] and orphans == ["z9"]


def test_an_independent_review_request_leaves_the_generators_reasoning_out():
    own = review_request("code", "because", False)
    independent = review_request("code", "because", True)
    assert "because" in own and "because" not in independent and "<code>code</code>" in independent
