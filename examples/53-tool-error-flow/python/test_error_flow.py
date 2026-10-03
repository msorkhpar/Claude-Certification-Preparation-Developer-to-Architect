from error_flow import ToolError, next_step, run, scenarios, scripted


def test_a_transient_failure_is_retried_with_doubling_waits_until_it_works():
    result, waits = run(scripted(ToolError("transient", "busy."), ToolError("transient", "busy."), "ok"), {})
    assert result["content"] == "ok" and result["attempts"] == 3 and waits == [100, 200]


def test_a_timeout_without_a_key_is_not_repeated_and_names_the_check_to_make():
    tool = scripted(ToolError("timeout", "No answer."), "created")
    result, waits = run(tool, {})
    assert result["kind"] == "outcome_unknown" and tool.state["n"] == 1 and waits == []
    assert next_step(result) == "check the state first"


def test_the_same_key_goes_out_with_every_retry():
    tool = scripted(ToolError("timeout", "No answer."), "created")
    result, _ = run(tool, {}, key="k-1")
    assert result["is_error"] is False and tool.state["seen"] == ["k-1", "k-1"]


def test_an_empty_result_is_accepted_and_every_scenario_has_a_next_step():
    result, _ = run(scripted([]), {}, read_only=True)
    assert next_step(result) == "accept the empty result"
    for _, tool, options in scenarios():
        result, _ = run(tool, {}, **options)
        assert next_step(result)
