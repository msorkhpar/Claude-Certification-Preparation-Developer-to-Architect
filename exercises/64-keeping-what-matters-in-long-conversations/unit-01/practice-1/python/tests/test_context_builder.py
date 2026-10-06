import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
import context_builder as _solution


def _got(fn):
    def call(*args, **kwargs):
        value = fn(*args, **kwargs)
        assert value is not None, f"{fn.__name__} returned nothing"
        return value

    return call


build_context, missing_from_summary, trim_record, update_facts, window = (_got(getattr(_solution, n)) for n in ("build_context", "missing_from_summary", "trim_record", "update_facts", "window"))

ORDER = {"order_id": "A-1042", "purchase_date": "2026-09-02", "items": "2 x kettle", "return_window": "30 days", "warehouse_bin": "R7-22", "carrier_hash": "9f3c", "refund_amount": "$129.50"}


def fact(customer="c1", name="refund", value="$129.50", as_of="2026-09-02"):
    return {"customer": customer, "name": name, "value": value, "as_of": as_of}


def msg(role, kind, id, text):
    return {"role": role, "kind": kind, "id": id, "text": text}


def test_m1_trimming_keeps_only_the_named_fields_with_their_exact_values_in_the_named_order():
    assert list(trim_record(ORDER, ["refund_amount", "order_id"]).items()) == [("refund_amount", "$129.50"), ("order_id", "A-1042")]
    assert "warehouse_bin" not in trim_record(ORDER, ["order_id", "items"])


def test_e1_a_field_that_the_record_does_not_have_is_skipped():
    assert trim_record(ORDER, ["order_id", "tracking_url"]) == {"order_id": "A-1042"}
    assert trim_record({}, ["order_id"]) == {}


def test_e2_a_newer_fact_replaces_the_old_one_and_the_old_value_is_kept_as_history():
    start = update_facts({}, "address", "12 Oak St", "2026-08-01")
    assert start == {"address": {"value": "12 Oak St", "as_of": "2026-08-01", "superseded": []}}
    later = update_facts(start, "address", "9 Elm Rd", "2026-09-10")
    assert later["address"] == {"value": "9 Elm Rd", "as_of": "2026-09-10", "superseded": ["12 Oak St@2026-08-01"]}
    assert start["address"]["value"] == "12 Oak St" and start["address"]["superseded"] == []
    same_day = update_facts(start, "address", "9 Elm Rd", "2026-08-01")
    assert same_day["address"] == {"value": "9 Elm Rd", "as_of": "2026-08-01", "superseded": ["12 Oak St@2026-08-01"]}


def test_e3_an_older_fact_that_arrives_late_does_not_replace_the_current_one():
    current = update_facts({}, "address", "9 Elm Rd", "2026-09-10")
    after = update_facts(current, "address", "12 Oak St", "2026-08-01")
    assert after["address"]["value"] == "9 Elm Rd" and after["address"]["as_of"] == "2026-09-10"
    assert after["address"]["superseded"] == ["12 Oak St@2026-08-01"]


def test_e4_the_case_facts_of_another_customer_never_enter_the_context():
    text = build_context("c1", [fact(), fact("c2", "refund", "$20.00")], "summary", [])
    assert "$129.50" in text and "$20.00" not in text
    assert "## Case facts" not in build_context("c3", [fact()], "summary", [])


def test_e5_the_context_puts_case_facts_first_then_the_summary_then_the_recent_messages():
    text = build_context("c1", [fact(name="refund"), fact(name="order", value="A-1042")], "They want the money back.", [{"role": "user", "text": "Any news?"}])
    assert text == "## Case facts\nrefund: $129.50 (as of 2026-09-02)\norder: A-1042 (as of 2026-09-02)\n\n## Summary so far\nThey want the money back.\n\n## Recent messages\nuser: Any news?"


def test_e6_a_summary_that_loses_an_exact_value_is_reported():
    facts = [fact(name="refund", value="$129.50"), fact(name="deadline", value="2026-09-30"), fact(name="order", value="A-1042")]
    assert missing_from_summary("Refund of about $130 for order A-1042, due end of month.", facts) == ["refund", "deadline"]
    assert missing_from_summary("$129.50, 2026-09-30, A-1042", facts) == []


def test_e7_the_window_drops_the_oldest_messages_and_keeps_a_tool_call_with_its_result():
    messages = [msg("user", "text", "", "x" * 40), msg("assistant", "tool_use", "t1", "y" * 40), msg("user", "tool_result", "t1", "z" * 40), msg("assistant", "text", "", "done.....")]
    assert [m["text"][0] for m in window(messages, 14)] == ["d"]
    assert [m["kind"] for m in window(messages, 23)] == ["tool_use", "tool_result", "text"]
    assert len(window(messages, 1000)) == 4 and window(messages, 0) == []
