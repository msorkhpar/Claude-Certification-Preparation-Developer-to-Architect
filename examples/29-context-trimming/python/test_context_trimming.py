from context_trimming import POLICY, cite, clear_tool_results, cited_reply, conversation, footnotes, tokens, verify


def test_clearing_keeps_the_calls_and_shrinks_the_conversation():
    before = conversation()
    after = clear_tool_results(before, 2)
    assert tokens(after) < tokens(before) * 0.6
    assert [m for m in after if m["role"] == "assistant"] == [m for m in before if m["role"] == "assistant"]
    assert sum(1 for m in after if isinstance(m["content"], list) for b in m["content"] if b["type"] == "tool_result" and b["content"] == "[cleared]") == 3
    assert before == conversation()


def test_a_citation_that_matches_its_document_passes_and_a_changed_one_is_caught():
    blocks = [{"type": "text", "text": "x", "citations": [cite(0, 19)]}]
    assert verify(blocks, [POLICY]) == []
    blocks[0]["citations"][0]["cited_text"] = "The grass is red."
    assert verify(blocks, [POLICY]) == [{"block": 0, "citation": 0, "problem": "text_mismatch"}]


def test_a_source_cited_twice_gets_one_number():
    blocks = cited_reply()["content"]
    text = footnotes(blocks, ["Policy"])
    assert text.count("[1]") == 3 and text.count("[2]") == 2 and text.endswith('[2] Policy: "Water is essential for life."')
