import copy

from harness import scripted_client
from harness.scripted import message, text
from screen_loop import HALT, MODEL, SCREEN, call, perform, run_loop, scale_for, to_screen


def test_the_scale_comes_from_the_pixel_budget_for_a_large_screen_and_clicks_map_back():
    scale = scale_for(2560, 1440)
    assert round(scale, 4) == 0.5585 and (int(2560 * scale), int(1440 * scale)) == (1429, 804)
    assert scale_for(1280, 720) == 1
    assert to_screen(0, 0, scale, SCREEN) == (0, 0) and to_screen(10_000, 10_000, scale, SCREEN) == (2559, 1439)


def test_a_risky_click_is_declined_unless_a_person_says_yes():
    screen = copy.deepcopy(SCREEN)
    scale = scale_for(2560, 1440)
    click = {"coordinate": [round(2150 * scale), round(1240 * scale)]}
    assert perform(screen, "left_click", click, scale, None) == ("Declined: buy needs a person's confirmation (payment)", True)
    assert perform(screen, "left_click", click, scale, lambda action: action["risk"] == "payment")[1] is False
    assert screen["log"] == [["click", "buy"]]


def test_a_failure_halts_the_rest_of_its_batch_with_the_documented_text():
    screen = copy.deepcopy(SCREEN)
    client, transport = scripted_client(message([call("t1", "left_click", coordinate=[900, 900]), call("t2", "mystery"), call("t3", "type", text="x")], "tool_use", model=MODEL),
                                        message([text("ok")], model=MODEL))
    assert run_loop(client, screen)["status"] == "done"
    results = transport.requests[1]["messages"][-1]["content"]
    assert [r.get("is_error", False) for r in results] == [False, True, True] and results[2]["content"] == HALT and screen["typed"] == ""
    assert all(r["toolset_name"] == "computer" for r in results)
