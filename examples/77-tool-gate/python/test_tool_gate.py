from tool_gate import check_output, decide


def test_a_denied_permission_refuses_the_tool_whatever_else_it_asks_for():
    assert decide(["read_files", "network"]) == ("refused", ["network"])
    assert decide(["run_process", "network"]) == ("refused", ["run_process", "network"])


def test_a_write_waits_for_a_person_and_a_read_runs_by_itself():
    assert decide(["read_files", "write_files"]) == ("needs_approval", ["write_files"])
    assert decide(["read_files"]) == ("auto", [])


def test_a_result_is_checked_for_fields_types_and_size_before_the_agent_uses_it():
    assert check_output({"headline": "ok", "rows": 3}) == []
    assert check_output({"headline": "ok"}) == ["missing: rows"]
    assert check_output({"headline": "ok", "rows": "3"}) == ["type: rows"]
    assert check_output({"headline": "x" * 201, "rows": 3}) == ["too large"]
    assert check_output({"headline": "x" * 200, "rows": 3}) == []
