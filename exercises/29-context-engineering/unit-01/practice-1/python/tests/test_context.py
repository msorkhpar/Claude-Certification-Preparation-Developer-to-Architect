import copy
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from context import clear_tool_results, compact, count_tokens, footnotes, verify_citations, window


def user(text):
    return {"role": "user", "content": text}


def said(text):
    return {"role": "assistant", "content": [{"type": "text", "text": text}]}


def call(id, name, **input):
    return {"role": "assistant", "content": [{"type": "tool_use", "id": id, "name": name, "input": input}]}


def result(id, text, error=False):
    block = {"type": "tool_result", "tool_use_id": id, "content": text}
    if error:
        block["is_error"] = True
    return {"role": "user", "content": [block]}


def result_blocks(messages):
    return [b for m in messages if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_result"]


def orphans(messages):
    """Tool results without their call, and calls without their result."""
    calls = {b["id"] for m in messages if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_use"}
    answered = {b["tool_use_id"] for b in result_blocks(messages)}
    return sorted(calls ^ answered)


def first_text(message):
    content = message["content"]
    return content if isinstance(content, str) else content[0].get("text")


def conversation():
    return [user("find the invoice"), call("t1", "search", q="invoice"), result("t1", "A" * 400), call("t2", "read", doc=7), result("t2", "B" * 400, error=True),
            call("t3", "search", q="total"), result("t3", "C" * 400), call("t4", "calc", expr="1+1"), result("t4", "D" * 40), said("The total is 2.")]


def turns_conversation():
    return [user("first question"), said("first answer " + "x" * 80),
            user("second question"), call("a1", "search"), result("a1", "R" * 300), said("second answer"),
            user("third question"), said("third answer " + "y" * 60),
            user("fourth question"), call("a2", "search"), result("a2", "S" * 100), said("fourth answer")]


class Summariser:
    def __init__(self, *texts):
        self.texts, self.calls = list(texts), []

    def __call__(self, messages):
        self.calls.append(copy.deepcopy(messages))
        return self.texts.pop(0)


def test_m1_an_over_budget_conversation_becomes_a_summary_and_the_newest_turn():
    messages = turns_conversation()
    budget = count_tokens(messages) // 2
    summariser = Summariser("The user asked three things.")
    done = compact(messages, budget, summariser, 1) or []
    assert len(summariser.calls) == 1 and summariser.calls[0] == messages[:8]
    assert [m["role"] for m in done] == ["user", "assistant", "user", "assistant"][:len(done)] and len(done) == 4
    assert done[0]["content"] == [{"type": "text", "text": "<summary>\nThe user asked three things.\n</summary>"}, {"type": "text", "text": "fourth question"}]
    assert done[1:] == messages[9:]
    assert count_tokens(done) <= budget


def test_e1_old_tool_results_are_cleared_but_their_calls_and_flags_stay():
    messages = conversation()
    before = copy.deepcopy(messages)
    cleared = clear_tool_results(messages, 2) or []
    assert messages == before
    blocks = result_blocks(cleared)
    assert [(b["tool_use_id"], b["content"] == "[cleared]", b.get("is_error", False)) for b in blocks] == [("t1", True, False), ("t2", True, True), ("t3", False, False), ("t4", False, False)]
    assert [m for m in cleared if m["role"] == "assistant"] == [m for m in messages if m["role"] == "assistant"]
    assert [b["content"] for b in result_blocks(clear_tool_results(messages, 0, placeholder="gone") or [])] == ["gone"] * 4
    kept = result_blocks(clear_tool_results(messages, 2, exclude=("read",)) or [])
    assert [(b["tool_use_id"], b["content"] == "[cleared]") for b in kept] == [("t1", True), ("t2", False), ("t3", False), ("t4", False)]


def test_e2_the_window_drops_whole_turns_and_never_splits_a_tool_call_from_its_result():
    messages = turns_conversation()
    total = count_tokens(messages)
    for budget in range(total, 0, -7):
        kept = window(messages, budget) or []
        assert orphans(kept) == [], budget
        assert kept and first_text(kept[0]) in ("first question", "second question", "third question", "fourth question"), budget
    two = window(messages, count_tokens(messages[6:]) + 1) or []
    assert [first_text(m) for m in two if m["role"] == "user" and isinstance(m["content"], str)] == ["third question", "fourth question"]
    assert window(messages, total) == messages
    assert window(messages, 1) == messages[8:]
    pinned = window(messages, count_tokens(messages[:2] + messages[8:]) + 1, pin=True) or []
    assert pinned == messages[:2] + messages[8:]


def test_e3_a_conversation_within_budget_or_with_nothing_older_is_left_alone():
    messages = turns_conversation()
    summariser = Summariser("unused")
    assert compact(messages, count_tokens(messages), summariser) == messages
    single = [user("one long question " + "z" * 400), said("answer")]
    assert compact(single, 5, summariser) == single
    assert compact(messages, 10_000, summariser, 2) == messages
    assert summariser.calls == []


def test_e4_a_second_compaction_folds_the_earlier_summary_into_the_new_one():
    messages = turns_conversation()
    summariser = Summariser("SUMMARY ONE", "SUMMARY TWO")
    first = compact(messages, 60, summariser, 1) or []
    grown = first + [said("noted"), user("fifth question " + "q" * 200), said("fifth answer " + "w" * 200)]
    second = compact(grown, 60, summariser, 1) or []
    assert len(summariser.calls) == 2
    older = summariser.calls[1]
    assert any(isinstance(b, dict) and b.get("text", "").startswith("<summary>\nSUMMARY ONE") for m in older if isinstance(m["content"], list) for b in m["content"])
    summaries = [b for m in second if isinstance(m["content"], list) for b in m["content"] if b.get("type") == "text" and b["text"].startswith("<summary>")]
    assert [b["text"] for b in summaries] == ["<summary>\nSUMMARY TWO\n</summary>"]


DOCS = [{"title": "Policy", "text": "The grass is green. The sky is blue."}, {"title": "Notes", "text": "Water is essential for life."}]


def cite(doc, start, end, cited):
    return {"type": "char_location", "cited_text": cited, "document_index": doc, "start_char_index": start, "end_char_index": end}


def test_e5_a_citation_that_does_not_match_its_document_is_reported():
    good = {"type": "text", "text": "Grass is green.", "citations": [cite(0, 0, 19, "The grass is green.")]}
    assert verify_citations([good, {"type": "text", "text": "No source."}], DOCS) == []
    blocks = [{"type": "text", "text": "x", "citations": [cite(0, 0, 19, "The grass is green."), cite(0, 0, 19, "The grass is red."), cite(5, 0, 3, "The"), cite(0, 20, 99, "The sky is blue."),
                                                          cite(1, 3, 3, ""), {"type": "page_location", "cited_text": "The", "document_index": 0, "start_page_number": 1, "end_page_number": 2}]}]
    got = verify_citations(blocks, DOCS) or []
    assert [(p["block"], p["citation"], p["problem"]) for p in got] == [(0, 1, "text_mismatch"), (0, 2, "unknown_document"), (0, 3, "bad_range"), (0, 4, "bad_range"), (0, 5, "unsupported_type")]
    assert verify_citations([{"type": "text", "text": "x", "citations": [cite(0, 20, 36, "The sky is blue.")]}], DOCS) == []


def test_e6_footnotes_number_each_distinct_source_once_in_order_of_appearance():
    blocks = [{"type": "text", "text": "Grass is green. ", "citations": [cite(0, 0, 19, "The grass is green.")]},
              {"type": "text", "text": "Water matters. ", "citations": [cite(1, 0, 28, "Water is essential for life.")]},
              {"type": "text", "text": "Again, green.", "citations": [cite(0, 0, 19, "The grass is green.")]},
              {"type": "text", "text": " No source here."}]
    assert footnotes(blocks, DOCS) == ('Grass is green. [1]Water matters. [2]Again, green.[1] No source here.\n\nSources:\n'
                                       '[1] Policy: "The grass is green."\n[2] Notes: "Water is essential for life."')
    assert footnotes([{"type": "text", "text": "Plain."}], DOCS) == "Plain."
