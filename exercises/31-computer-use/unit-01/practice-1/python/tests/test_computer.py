import copy
import math
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from computer import perform, prune_screenshots, run_computer_loop, scale_for, scaled_size, to_screen

NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed."


def make_screen(width=1920, height=1080):
    return {"width": width, "height": height, "cursor": [0, 0], "typed": "", "log": [],
            "elements": [{"id": "search", "x": 1000, "y": 200, "w": 400, "h": 40, "risk": "none"},
                         {"id": "pay", "x": 800, "y": 600, "w": 200, "h": 60, "risk": "payment"}]}


def use(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "toolset_name": "computer", "input": input}


def reply(content, stop_reason="tool_use"):
    return {"content": content, "stop_reason": stop_reason}


def says(text):
    return reply([{"type": "text", "text": text}], "end_turn")


class Scripted:
    """The model: returns the scripted replies in order and keeps a copy of every request."""

    def __init__(self, *replies):
        self.replies, self.seen = list(replies), []

    def __call__(self, request):
        self.seen.append(copy.deepcopy(request))
        return self.replies.pop(0) if self.replies else says("script ran out")


def results_of(request, index=-1):
    return request["messages"][index]["content"]


def test_m1_the_loop_runs_scaled_actions_and_sends_every_result_in_one_message():
    screen = make_screen()
    model = Scripted(reply([use("t1", "screenshot")]),
                     reply([use("t2", "left_click", coordinate=[894, 164]), use("t3", "type", text="weather"), use("t4", "screenshot")]),
                     says("Typed it."))
    result = run_computer_loop(model, screen) or {}
    assert (result.get("status"), result.get("turns")) == ("done", 3)
    assert screen["log"] == [("left_click", "search"), ("type", "weather")] and screen["typed"] == "weather"
    assert all(r["tools"] == [{"type": "computer_toolset_20260801"}] and r["model"] == "claude-sonnet-5-5" and r["max_tokens"] == 4096 for r in model.seen) and len(model.seen) == 3
    first = results_of(model.seen[1]) if len(model.seen) > 1 else []
    assert first == [{"type": "tool_result", "tool_use_id": "t1", "toolset_name": "computer",
                      "content": [{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": "png:1429x804:0"}}]}]
    last = results_of(model.seen[2]) if len(model.seen) > 2 else []
    assert [(r["tool_use_id"], r.get("is_error", False)) for r in last] == [("t2", False), ("t3", False), ("t4", False)]
    assert [r["content"] for r in last][:2] == ["Clicked search", "Typed 7 characters"]
    assert last[2]["content"][0]["source"]["data"] == "png:1429x804:2"
    assert [m["role"] for m in model.seen[2]["messages"]] == ["user", "assistant", "user", "assistant", "user"]


def test_e1_the_screen_is_scaled_to_what_the_model_may_see_and_clicks_are_scaled_back():
    assert scale_for(1280, 800) == 1.0 and scale_for(1024, 768) == 1.0
    assert math.isclose(scale_for(1920, 1080), 0.744709, abs_tol=1e-5)
    assert math.isclose(scale_for(2560, 1440), 0.558531, abs_tol=1e-5)
    assert math.isclose(scale_for(3000, 100), 1568 / 3000, abs_tol=1e-9)
    assert scaled_size(1920, 1080) == (1429, 804) and scaled_size(2560, 1440) == (1429, 804) and scaled_size(1024, 768) == (1024, 768)
    screen = make_screen()
    scale = scale_for(1920, 1080)
    assert to_screen(894, 164, scale, screen) == (1200, 220)
    assert to_screen(5000, 5000, 1.0, screen) == (1919, 1079) and to_screen(-3, -3, 1.0, screen) == (0, 0)


def test_e2_a_click_on_a_risky_element_needs_a_persons_confirmation():
    scale = scale_for(1920, 1080)
    asked = []

    def yes(action):
        asked.append(action)
        return True

    for confirm in (None, lambda action: False):
        screen = make_screen()
        content, is_error = perform(screen, "left_click", {"coordinate": [670, 469]}, scale, confirm) or ("", False)
        assert is_error is True and str(content).startswith("Declined: pay needs a person's confirmation (payment)")
        assert screen["log"] == [] and screen["cursor"] == [0, 0]
    screen = make_screen()
    content, is_error = perform(screen, "double_click", {"coordinate": [670, 469]}, scale, yes) or ("", True)
    assert (content, is_error) == ("Clicked pay", False) and screen["log"] == [("double_click", "pay")]
    assert asked == [{"action": "double_click", "element": "pay", "risk": "payment"}]
    asked.clear()
    safe = perform(screen, "left_click", {"coordinate": [894, 164]}, scale, yes) or ("", True)
    assert safe == ("Clicked search", False) and asked == []
    assert perform(screen, "left_click", {"coordinate": [5, 5]}, scale, yes) == ("Clicked nothing", False)


def test_e3_a_failed_action_stops_the_rest_of_its_batch():
    screen = make_screen()
    model = Scripted(reply([use("a", "left_click", coordinate=[894, 164]), use("b", "left_click", coordinate=[9999, 5]), use("c", "type", text="x"), use("d", "screenshot")]),
                     says("Stopped."))
    run_computer_loop(model, screen)
    results = results_of(model.seen[1]) if len(model.seen) > 1 else []
    assert [(r["tool_use_id"], r.get("is_error", False)) for r in results] == [("a", False), ("b", True), ("c", True), ("d", True)]
    assert "outside the screenshot" in results[1]["content"] and results[2]["content"] == NOT_EXECUTED and results[3]["content"] == NOT_EXECUTED
    assert all(r["toolset_name"] == "computer" for r in results)
    assert screen["log"] == [("left_click", "search")] and screen["typed"] == ""


def test_e4_invalid_actions_come_back_as_error_results_with_a_reason():
    scale = scale_for(1920, 1080)
    screen = make_screen()

    def run(name, **args):
        return perform(screen, name, args, scale) or ("no result", False)

    assert run("key", text="ctrl+s", repeat=2) == ("Pressed ctrl+s", False)
    assert run("key", text="Return", repeat=101)[1] is True and run("key", repeat=1)[1] is True
    assert run("wait", duration=2.5) == ("Waited 2.5s", False) and run("wait", duration=301)[1] is True
    assert run("scroll", scroll_direction="down", scroll_amount=3) == ("Scrolled down 3", False)
    assert run("scroll", scroll_direction="sideways", scroll_amount=3)[1] is True and run("scroll", scroll_direction="up", scroll_amount=0)[1] is True
    assert run("teleport") == ("Unknown action: teleport", True)
    assert run("type")[1] is True and run("left_click", coordinate=[5000, 5])[1] is True
    assert run("mouse_move", coordinate=[670, 469]) == ("Moved", False)
    assert run("cursor_position") == ("X=670,Y=469", False)
    assert run("zoom", region=[0, 0, 5000, 5000])[1] is True and run("zoom", region=[300, 200, 100, 100])[1] is True


def test_e5_the_loop_ends_on_a_stop_reason_or_at_the_turn_limit():
    endless = [reply([use(f"t{i}", "screenshot")]) for i in range(10)]
    model = Scripted(*endless)
    result = run_computer_loop(model, make_screen(), max_turns=3) or {}
    assert (result.get("status"), result.get("turns"), len(model.seen)) == ("max_turns", 3, 3)
    assert (run_computer_loop(Scripted(reply([{"type": "text", "text": "no"}], "refusal")), make_screen()) or {}).get("status") == "refused"
    assert (run_computer_loop(Scripted(reply([{"type": "text", "text": "cut"}], "max_tokens")), make_screen()) or {}).get("status") == "truncated"
    paused = Scripted(reply([{"type": "text", "text": "working"}], "pause_turn"), says("done"))
    result = run_computer_loop(paused, make_screen()) or {}
    assert (result.get("status"), result.get("turns")) == ("done", 2)
    assert [m["role"] for m in paused.seen[1]["messages"]] == ["user", "assistant"] if len(paused.seen) > 1 else False


def test_e6_old_screenshots_are_replaced_so_the_context_does_not_fill_up():
    def shot(n):
        return {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": f"png:{n}"}}

    messages = [{"role": "user", "content": "task"}]
    for n in range(5):
        messages.append({"role": "assistant", "content": [use(f"t{n}", "screenshot")]})
        messages.append({"role": "user", "content": [{"type": "tool_result", "tool_use_id": f"t{n}", "toolset_name": "computer", "content": [shot(n)]}]})
    messages.append({"role": "assistant", "content": [use("k", "type", text="x")]})
    messages.append({"role": "user", "content": [{"type": "tool_result", "tool_use_id": "k", "content": "Typed 1 characters"}]})
    before = copy.deepcopy(messages)
    pruned = prune_screenshots(messages, keep=2) or []
    assert messages == before

    def contents(msgs):
        return [m["content"][0]["content"] for m in msgs if m["role"] == "user" and isinstance(m["content"], list)]

    note = [{"type": "text", "text": "[screenshot removed]"}]
    assert contents(pruned) == [note, note, note, [shot(3)], [shot(4)], "Typed 1 characters"]
    assert len(pruned) == len(messages) and pruned[0] == messages[0]
    assert prune_screenshots(messages, keep=9) == messages
    assert contents(prune_screenshots(messages, keep=0) or [])[:5] == [note] * 5


def test_e7_screenshots_and_zooms_are_sent_at_the_size_the_model_may_see():
    for width, height, size in ((1920, 1080, "1429x804"), (2560, 1440, "1429x804"), (1280, 800, "1280x800")):
        screen = make_screen(width, height)
        content, is_error = perform(screen, "screenshot", {}, scale_for(width, height)) or ([{}], False)
        assert is_error is False and content[0].get("source", {}).get("data") == f"png:{size}:0"
        assert content[0].get("type") == "image" and content[0]["source"].get("media_type") == "image/png"
    screen = make_screen()
    content, is_error = perform(screen, "zoom", {"region": [0, 0, 300, 200]}, scale_for(1920, 1080)) or ([{}], False)
    assert is_error is False and content[0].get("source", {}).get("data") == "png:403x269:0"
    perform(screen, "left_click", {"coordinate": [5, 5]}, scale_for(1920, 1080))
    shot, _ = perform(screen, "screenshot", {}, scale_for(1920, 1080)) or ([{}], False)
    assert shot[0].get("source", {}).get("data") == "png:1429x804:1"
