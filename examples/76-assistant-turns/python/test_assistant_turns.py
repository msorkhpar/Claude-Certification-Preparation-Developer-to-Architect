from assistant_turns import recall, route, window


def test_a_signal_of_risk_is_routed_to_a_person_before_anything_else():
    assert route("I might hurt myself, please get me a human") == "handoff:safety"
    assert route("this is an emergency", misses=5) == "handoff:safety"


def test_a_request_for_a_person_is_honoured_and_a_stall_needs_two_misses():
    assert route("Can I speak to a person?") == "handoff:requested"
    assert route("what?", misses=1) == "answer" and route("what?", misses=2) == "handoff:stalled"


def test_the_window_keeps_the_newest_turns_that_fit_and_all_the_pinned_facts():
    turns = ["one two", "three four five", "six"]
    assert window(turns, 4, ["fact"]) == {"kept": ["three four five", "six"], "dropped": 1, "facts": ["fact"]}
    assert window(turns, 3, ["fact"])["kept"] == ["six"] and window(turns, 6, ["fact"])["dropped"] == 0


def test_memory_is_read_per_customer_and_old_facts_are_marked_to_verify():
    assert recall("ada", "2026-10-04") == [("address", "12 Elm Road", "current"), ("plan", "Plus", "verify")]
    assert recall("bob", "2026-10-04") == []
    assert recall("ada", "2026-10-20")[0][2] == "current" and recall("ada", "2026-10-21")[0][2] == "verify"
