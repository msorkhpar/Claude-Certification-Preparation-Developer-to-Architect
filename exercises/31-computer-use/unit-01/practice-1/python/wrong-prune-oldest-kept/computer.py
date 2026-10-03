"""The loop around the computer use tool, against a toy screen. See ../../statement.md."""
import copy
import math

TOOLSET = "computer_toolset_20260801"
CLICKS = ("left_click", "right_click", "middle_click", "double_click", "triple_click")
NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed."


def render(screen, width, height):
    """Given: the screenshot of the toy screen at `width` x `height`, as the base64 text of a picture. It changes after every logged action."""
    return f"png:{width}x{height}:{len(screen['log'])}"


def scale_for(width, height):
    return min(1.0, 1568 / max(width, height), math.sqrt(1_150_000 / (width * height)))


def scaled_size(width, height):
    scale = scale_for(width, height)
    return (int(width * scale), int(height * scale))


def to_screen(x, y, scale, screen):
    """A point on the (scaled) screenshot as a point on the real screen, clamped into the screen."""
    return (min(max(round(x / scale), 0), screen["width"] - 1), min(max(round(y / scale), 0), screen["height"] - 1))


def _element_at(screen, x, y):
    hit = None
    for element in screen["elements"]:
        if element["x"] <= x < element["x"] + element["w"] and element["y"] <= y < element["y"] + element["h"]:
            hit = element
    return hit


def _inside(shot, point):
    return isinstance(point, (list, tuple)) and len(point) == 2 and 0 <= point[0] < shot[0] and 0 <= point[1] < shot[1]


def perform(screen, name, args, scale, confirm=None):
    """Run one action. Returns (content, is_error): the text or image blocks for the result and whether it failed."""
    shot = scaled_size(screen["width"], screen["height"])
    if name == "screenshot":
        return ([{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": render(screen, *shot)}}], False)
    if name == "zoom":
        region = args.get("region")
        if not (isinstance(region, (list, tuple)) and len(region) == 4 and _inside(shot, region[:2]) and 0 < region[2] <= shot[0] and 0 < region[3] <= shot[1]
                and region[0] < region[2] and region[1] < region[3]):
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
        if element and element.get("risk", "none") != "none":
            if confirm is None or not confirm({"action": name, "element": element["id"], "risk": element["risk"]}):
                return (f"Declined: {element['id']} needs a person's confirmation ({element['risk']})", True)
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
        repeat = args.get("repeat", 1)
        if not args.get("text") or not isinstance(repeat, int) or not 1 <= repeat <= 100:
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
        if direction not in ("up", "down", "left", "right") or not isinstance(amount, int) or amount < 1:
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
    for _, block, i in (images[keep:] if keep > 0 else images):
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
        elif stop == "refusal":
            return {"status": "refused", "turns": turn, "messages": messages}
        elif stop != "pause_turn":
            return {"status": "done" if stop in ("end_turn", "stop_sequence") else "truncated", "turns": turn, "messages": messages}
    return {"status": "max_turns", "turns": max_turns, "messages": messages}
