from read_a_trace import TRACES, first_failure


def test_an_empty_reply_after_text_following_a_tool_result_is_our_message_structure():
    index, what, origin, _ = first_failure(TRACES["A: a tool loop that ends in silence"])
    assert (index, what, origin) == (3, "empty reply", "integration")


def test_the_same_empty_reply_without_that_text_is_the_model():
    trace = [{"kind": "request", "last_user_blocks": ["tool_result"]}, {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": []}]
    assert first_failure(trace)[:3] == (1, "empty reply", "model")


def test_a_529_is_the_service_and_the_next_action_is_a_retry():
    index, what, origin, nxt = first_failure(TRACES["B: a busy service and a retry"])
    assert (index, what, origin, nxt) == (1, "overloaded_error", "service", "retry with back-off")


def test_json_inside_a_code_fence_is_the_parser_and_prose_without_json_is_the_model():
    assert first_failure(TRACES["C: JSON in a code fence"])[:3] == (2, "parse failure", "integration")
    prose = [{"kind": "parse", "ok": False, "text": "I am not sure"}]
    assert first_failure(prose)[:3] == (0, "parse failure", "model")


def test_a_clean_trace_has_no_failure():
    assert first_failure([{"kind": "request", "last_user_blocks": ["text"]}, {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": [{"type": "text"}]}]) is None
