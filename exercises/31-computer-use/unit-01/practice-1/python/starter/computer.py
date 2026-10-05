"""The loop around the computer use tool, against a toy screen. See ../../statement.md."""
import copy
import logging
import math

log = logging.getLogger(__name__)

TOOLSET = "computer_toolset_20260801"
CLICKS = ("left_click", "right_click", "middle_click", "double_click", "triple_click")
NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed."


def render(screen, width, height):
    """Given: the screenshot of the toy screen at `width` x `height`, as the base64 text of a picture. It changes after every logged action."""
    return f"png:{width}x{height}:{len(screen['log'])}"


def scale_for(width, height):
    """TODO 1 of 9 (unlocks e1, e7 and m1): the factor that shrinks a screen to what the model may be sent.

    Receives the real width and height. Returns a float: min(1, 1568 / longest side, sqrt(1,150,000 / (width * height))).
    Example: scale_for(1280, 800) -> 1.0, scale_for(1920, 1080) -> 0.7447...
    """
    return 1.0


def scaled_size(width, height):
    """TODO 2 of 9 (unlocks e7 and m1): the size of the screenshot the model is sent, as (width, height).

    Receives the real width and height. Returns two ints: each side times `scale_for`, cut to a whole number.
    Example: scaled_size(1920, 1080) -> (1429, 804)
    """
    return (width, height)


def to_screen(x, y, scale, screen):
    """TODO 3 of 9 (unlocks e1, e2 and m1): a point on the (scaled) screenshot as a point on the real screen.

    Receives x and y on the screenshot, the scale and the screen. Returns (x, y) on the screen: divide by the scale, round half to even
    (Python's `round`), and clamp into 0 .. width - 1 and 0 .. height - 1.
    Example: to_screen(894, 164, 0.7447, screen) -> (1200, 220); to_screen(-3, -3, 1.0, screen) -> (0, 0)
    """
    return (0, 0)


def _element_at(screen, x, y):
    hit = None
    for element in screen["elements"]:
        if element["x"] <= x < element["x"] + element["w"] and element["y"] <= y < element["y"] + element["h"]:
            hit = element
    return hit


def _inside(shot, point):
    return isinstance(point, (list, tuple)) and len(point) == 2 and 0 <= point[0] < shot[0] and 0 <= point[1] < shot[1]


def _risk_error(name, element, confirm):
    """TODO 4 of 9 (unlocks e2): the error text when a click on this element needs a person and does not get one, else None.

    Receives the action name, the element under the click (a dict with "id" and "risk", or None) and `confirm` (a function or None).
    A risk other than "none" is asked of `confirm` with {"action", "element", "risk"}; with no `confirm` or an answer of False return
    "Declined: <id> needs a person's confirmation (<risk>)". Example: a "payment" element "pay", confirm None -> "Declined: pay needs a person's confirmation (payment)"
    """
    return None


def _valid_region(shot, region):
    """TODO 5 of 9 (unlocks e4): is this zoom region valid?

    Receives the scaled screenshot size (width, height) and the region. True for four numbers [x0, y0, x1, y1] where x0, y0 is inside
    the screenshot (`_inside`), x1 and y1 are above 0 and within the size, x0 < x1 and y0 < y1. Example: (1429, 804), [0, 0, 300, 200] -> True
    """
    return True


def _valid_key(args):
    """TODO 6 of 9 (unlocks e4): is this the input of a key press?

    Receives the input dict. True when `text` is a non-empty string and `repeat` (default 1) is an integer from 1 to 100.
    Example: {"text": "ctrl+s", "repeat": 2} -> True, {"text": "Return", "repeat": 101} -> False
    """
    return True


def _valid_scroll(args):
    """TODO 7 of 9 (unlocks e4): is this the input of a scroll?

    Receives the input dict. True when `scroll_direction` is up, down, left or right and `scroll_amount` is an integer of at least 1.
    Example: {"scroll_direction": "down", "scroll_amount": 3} -> True, {"scroll_direction": "up", "scroll_amount": 0} -> False
    """
    return True


def _to_replace(images, keep):
    """TODO 8 of 9 (unlocks e6): the images to replace by a note.

    Receives the list of images, oldest first, and `keep`. Returns the part of the list to replace: all but the newest `keep`, and all
    of them when keep is 0. Example: ["a", "b", "c"] with keep 1 -> ["a", "b"]; with keep 0 -> ["a", "b", "c"]; with keep 9 -> []
    """
    return []


def _final_status(stop):
    """TODO 9 of 9 (unlocks e5): the status the loop ends with for a stop reason, or None when it goes on.

    Receives the stop reason of a reply that is not `tool_use`. Returns "refused" for refusal, "done" for end_turn and stop_sequence,
    None for pause_turn (call again), and "truncated" for anything else. Example: "max_tokens" -> "truncated"
    """
    return None


def perform(screen, name, args, scale, confirm=None):
    """Run one action. Returns (content, is_error): the text or image blocks for the result and whether it failed."""
    log.debug("perform input: %r %r", name, args)
    shot = scaled_size(screen["width"], screen["height"])
    if name == "screenshot":
        return ([{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": render(screen, *shot)}}], False)
    if name == "zoom":
        region = args.get("region")
        if not _valid_region(shot, region):
            return (f"Invalid zoom region: {region}", True)
        x0, y0 = to_screen(region[0], region[1], scale, screen)
        x1, y1 = to_screen(region[2], region[3], scale, screen)
        return ([{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": render(screen, x1 - x0, y1 - y0)}}], False)
    if name in CLICKS:
        point = args.get("coordinate")
        if point is None:
            sx, sy = screen["cursor"]
        elif not _inside(shot, point):
            return (f"Coordinate {list(point) if isinstance(point, (list, tuple)) else point} is outside the screenshot", True)
        else:
            sx, sy = to_screen(point[0], point[1], scale, screen)
        element = _element_at(screen, sx, sy)
        declined = _risk_error(name, element, confirm)
        if declined:
            return (declined, True)
        screen["cursor"] = [sx, sy]
        screen["log"].append((name, element["id"] if element else None))
        return (f"Clicked {element['id'] if element else 'nothing'}", False)
    if name == "type":
        text = args.get("text")
        if not isinstance(text, str):
            return ("type needs a text", True)
        screen["typed"] += text
        screen["log"].append(("type", text))
        return (f"Typed {len(text)} characters", False)
    if name == "key":
        if not _valid_key(args):
            return ("key needs a text and a repeat from 1 to 100", True)
        screen["log"].append(("key", args["text"]))
        return (f"Pressed {args['text']}", False)
    if name == "wait":
        duration = args.get("duration")
        if not isinstance(duration, (int, float)) or not 0 <= duration <= 300:
            return ("wait needs a duration from 0 to 300 seconds", True)
        return (f"Waited {duration}s", False)
    if name == "scroll":
        direction, amount = args.get("scroll_direction"), args.get("scroll_amount")
        if not _valid_scroll(args):
            return ("scroll needs a direction and a positive amount", True)
        screen["log"].append(("scroll", direction))
        return (f"Scrolled {direction} {amount}", False)
    if name == "mouse_move":
        point = args.get("coordinate")
        if not _inside(shot, point):
            return (f"Coordinate {point} is outside the screenshot", True)
        screen["cursor"] = list(to_screen(point[0], point[1], scale, screen))
        return ("Moved", False)
    if name == "cursor_position":
        return (f"X={round(screen['cursor'][0] * scale)},Y={round(screen['cursor'][1] * scale)}", False)
    return (f"Unknown action: {name}", True)


def prune_screenshots(messages, keep=3):
    """A copy of the conversation in which every screenshot except the newest `keep` is replaced by a text note."""
    out = copy.deepcopy(messages)
    images = [(m, b, i) for m in out if isinstance(m["content"], list) for b in m["content"] if b.get("type") == "tool_result"
              and isinstance(b.get("content"), list) for i, c in enumerate(b["content"]) if c.get("type") == "image"]
    for _, block, i in _to_replace(images, keep):
        block["content"][i] = {"type": "text", "text": "[screenshot removed]"}
    return out


def run_computer_loop(ask, screen, model="claude-sonnet-5-5", max_turns=10, confirm=None):
    scale = scale_for(screen["width"], screen["height"])
    messages = [{"role": "user", "content": "Do the task on the screen."}]
    for turn in range(1, max_turns + 1):
        reply = ask({"model": model, "max_tokens": 4096, "tools": [{"type": TOOLSET}], "messages": copy.deepcopy(messages)})
        messages.append({"role": "assistant", "content": reply["content"]})
        stop = reply.get("stop_reason")
        if stop == "tool_use":
            results, failed = [], False
            for block in reply["content"]:
                if block["type"] != "tool_use":
                    continue
                result = {"type": "tool_result", "tool_use_id": block["id"]}
                if "toolset_name" in block:
                    result["toolset_name"] = block["toolset_name"]
                if failed:
                    result.update(content=NOT_EXECUTED, is_error=True)
                else:
                    content, is_error = perform(screen, block["name"], block["input"], scale, confirm)
                    result["content"] = content
                    if is_error:
                        result["is_error"] = True
                        failed = True
                results.append(result)
            messages.append({"role": "user", "content": results})
        elif _final_status(stop):
            return {"status": _final_status(stop), "turns": turn, "messages": messages}
    return {"status": "max_turns", "turns": max_turns, "messages": messages}
