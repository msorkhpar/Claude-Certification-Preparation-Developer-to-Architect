from harness import scripted_client
from harness.scripted import message, text, tool_use
from tool_loop import MODEL, REPLIES, TOOLS, loop, run_tool


class Block:
    def __init__(self, id, name, input):
        self.id, self.name, self.input = id, name, input


def test_a_failing_call_becomes_an_error_result_with_a_hint():
    result = run_tool(Block("t1", "get_weather", {"city": "Atlantis"}))
    assert result["is_error"] is True and "Known cities" in result["content"]


def test_all_results_go_back_in_one_user_message_in_call_order():
    client, transport = scripted_client(*REPLIES)
    final, messages = loop(client, "q")
    assert [m["role"] for m in messages] == ["user", "assistant", "user", "assistant"]
    assert [r["tool_use_id"] for r in messages[2]["content"]] == ["toolu_01", "toolu_02", "toolu_03"]
    assert final.stop_reason == "end_turn" and len(transport.requests) == 2


def test_a_forced_choice_is_sent_once():
    client, transport = scripted_client(message([tool_use("toolu_01", "get_time", city="Oslo")], stop_reason="tool_use", model=MODEL), message([text("09:15")], model=MODEL))
    loop(client, "time?", tool_choice={"type": "tool", "name": "get_time"})
    assert [r.get("tool_choice") for r in transport.requests] == [{"type": "tool", "name": "get_time"}, None]
