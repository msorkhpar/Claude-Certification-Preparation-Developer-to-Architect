from harness import scripted_client
from harness.scripted import message, text, tool_use
from loops import MODEL, by_fixed_count, by_stop_reason, by_text_marker, scenario_a, scenario_b


def test_the_stop_reason_loop_runs_the_tool_of_a_reply_that_says_done():
    status, calls, ran, answer = by_stop_reason(scripted_client(*scenario_a())[0], "t")
    assert (status, calls, ran, answer) == ("done", 2, ["save_file"], "Saved report.txt.")


def test_the_text_marker_loop_stops_before_the_tool_runs():
    status, calls, ran, _ = by_text_marker(scripted_client(*scenario_a())[0], "t")
    assert (status, calls, ran) == ("done", 1, [])


def test_a_backstop_below_the_real_need_gets_its_own_status():
    status, calls, ran, _ = by_stop_reason(scripted_client(*scenario_b())[0], "t", backstop=3)
    assert (status, calls, len(ran)) == ("max_turns", 3, 3)


def test_a_backstop_above_the_need_is_never_reached():
    assert by_stop_reason(scripted_client(*scenario_b())[0], "t", backstop=10)[:2] == ("done", 5)


def test_the_fixed_count_loop_reports_done_without_a_final_answer():
    status, _, ran, answer = by_fixed_count(scripted_client(*scenario_b())[0], "t")
    assert (status, len(ran), answer) == ("done", 3, "")


def test_an_end_turn_that_announces_a_tool_call_is_still_the_end():
    client, transport = scripted_client(message([text("Let me call the lookup tool next.")], model=MODEL))
    assert by_stop_reason(client, "t")[:2] == ("done", 1) and len(transport.requests) == 1
