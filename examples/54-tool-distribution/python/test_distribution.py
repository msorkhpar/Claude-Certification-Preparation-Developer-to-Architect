from distribution import ROLES, allowed, cache_cost, made_the_call, tools_for, turn_for


def test_each_role_gets_its_own_tools_and_the_irreversible_tool_is_nobodys_by_default():
    assert tools_for("searcher") == ["web_search", "fetch_page", "verify_fact"]
    assert tools_for("synthesizer") == ["verify_fact", "summarize_content"]
    assert "send_report" not in tools_for("reporter") and all(len(tools_for(r)) <= 5 for r in ROLES)


def test_a_model_that_rejects_forcing_gets_auto_one_tool_and_a_check_on_the_reply():
    assert turn_for("claude-sonnet-5-5", "a", ["a", "b"]) == {"tool_choice": "auto", "tools": ["a"], "check_reply": True}
    assert turn_for("claude-opus-5", "a", ["a", "b"]) == {"tool_choice": "tool:a", "tools": ["a", "b"], "check_reply": False}


def test_narrowing_the_tools_costs_more_than_changing_the_choice():
    base = {"tool_choice": "auto", "tools": ["a", "b"]}
    assert cache_cost(base, turn_for("claude-opus-5", "a", ["a", "b"])).startswith("the cached messages")
    assert cache_cost(base, turn_for("claude-sonnet-5-5", "a", ["a", "b"])).startswith("everything")
    assert cache_cost(base, dict(base)) == "nothing"


def test_a_reply_made_the_call_only_when_the_first_tool_use_is_the_forced_one():
    assert made_the_call([("tool_use", "a")], "a") and not made_the_call([("text", "x")], "a") and not made_the_call([("tool_use", "b"), ("tool_use", "a")], "a")


def test_a_refund_needs_approval_and_stays_under_the_limit():
    assert allowed("refund", 150, False).startswith("wait") and allowed("refund", 150, True) == "run"
    assert allowed("refund", 400, True).startswith("refused") and allowed("delete_account", 0, True).startswith("refused")
