from control_chain import *

SOURCE = "Water damage is covered up to 5,000 per claim."
OK = Answer("ok", 99, SOURCE)


def test_a_down_screen_holds_a_high_consequence_action_and_flags_a_low_one():
    assert route(Action("issue_refund", "high"), OK, SOURCE, False) == "hold: screen down"
    assert route(Action("draft_reply", "low"), OK, SOURCE, False) == "auto (unscreened)"
    assert route(Action("draft_reply", "low"), OK, SOURCE, True) == "auto"


def test_a_confident_answer_that_the_source_does_not_support_is_held():
    wrong = Answer("ok", 100, "Water damage is covered up to 8,000 per claim.")
    assert route(Action("draft_reply", "low"), wrong, SOURCE, True) == "hold: unsupported"
    assert route(Action("issue_refund", "high"), wrong, SOURCE, True) == "hold: unsupported"


def test_confidence_exactly_at_the_threshold_goes_out_and_one_below_is_reviewed():
    reply = Action("draft_reply", "low")
    assert route(reply, Answer("ok", 95, SOURCE), SOURCE, True) == "auto"
    assert route(reply, Answer("ok", 94, SOURCE), SOURCE, True) == "review"


def test_a_high_consequence_action_always_reaches_a_person():
    assert route(Action("issue_refund", "high"), Answer("ok", 100, SOURCE), SOURCE, True) == "human"


def test_the_audit_record_holds_no_content_and_erasure_unlinks_only_the_person():
    record = audit_record("r-1", Action("issue_refund", "high"), "human", "secret text")
    assert record["chars"] == 11 and record["content_stored"] is False and "secret text" not in str(record)
    kept, removed = erase({"<A>": "p1", "<B>": "p2", "<C>": "p1"}, "p1")
    assert kept == {"<B>": "p2"} and removed == 2
