"""The loop around the computer use toolset, on a toy screen: scaling both ways, a batch, a halt after a failure and a confirmation.

There is no desktop and no model: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the
Messages API (claude-sonnet-5-5), illustrative and not captures. The tool entry, the batch rule and the halt text are those of the
"Computer use tool" page of the Claude documentation, checked on 2026-10-03.
"""
import logging
import base64
import json
import math

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
TOOLSET = {"type": "computer_toolset_20260801"}
HALT = "Not executed: an earlier computer action in this turn failed."
SCREEN = {"width": 2560, "height": 1440, "typed": "", "log": [],
          "elements": [{"id": "search", "x": 800, "y": 100, "w": 900, "h": 60, "risk": "none"}, {"id": "buy", "x": 2000, "y": 1200, "w": 300, "h": 80, "risk": "payment"}]}


def scale_for(width, height):
    """The documentation's example limits (1568 px on the long edge, about 1.15 megapixels): small enough for every model."""
    return min(1, 1568 / max(width, height), math.sqrt(1_150_000 / (width * height)))


def to_screen(x, y, scale, screen):
    return (min(max(round(x / scale), 0), screen["width"] - 1), min(max(round(y / scale), 0), screen["height"] - 1))


def element_at(screen, x, y):
    hit = [e for e in screen["elements"] if e["x"] <= x < e["x"] + e["w"] and e["y"] <= y < e["y"] + e["h"]]
    return hit[-1] if hit else None


def screenshot(screen, scale):
    size = f'{int(screen["width"] * scale)}x{int(screen["height"] * scale)}'
    return [{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": base64.b64encode(f"{size}:{len(screen['log'])}".encode()).decode()}}]


def perform(screen, name, args, scale, confirm):
    """Run one member of the toolset on the toy screen; return (content, is_error)."""
    if name == "screenshot":
        return screenshot(screen, scale), False
    if name == "left_click":
        x, y = to_screen(*args["coordinate"], scale, screen)
        target = element_at(screen, x, y)
        if target and target["risk"] != "none" and not (confirm and confirm({"action": name, "element": target["id"], "risk": target["risk"]})):
            return f"Declined: {target['id']} needs a person's confirmation ({target['risk']})", True
        screen["log"].append(["click", target["id"] if target else None])
        return f"Clicked {target['id'] if target else 'nothing'} at {x},{y} on the screen", False
    if name == "type":
        screen["typed"] += args["text"]
        screen["log"].append(["type", args["text"]])
        return f"Typed {len(args['text'])} characters", False
    if name == "key":
        screen["log"].append(["key", args["text"]])
        return f"Pressed {args['text']}", False
    return f"Unknown action: {name}", True


def run_loop(client, screen, confirm=None, max_turns=6):
    scale = scale_for(screen["width"], screen["height"])
    messages = [{"role": "user", "content": "Search for a kettle and buy the first one."}]
    for turn in range(1, max_turns + 1):
        reply = client.messages.create(model=MODEL, max_tokens=4096, tools=[TOOLSET], messages=messages)
        messages.append({"role": "assistant", "content": [b.model_dump(exclude_none=True) for b in reply.content]})
        if reply.stop_reason != "tool_use":
            return {"status": "done", "turns": turn, "answer": reply.content[-1].text}
        results, failed = [], False
        for block in [b for b in reply.content if b.type == "tool_use"]:
            if failed:
                content, is_error = HALT, True
            else:
                content, is_error = perform(screen, block.name, block.input, scale, confirm)
                failed = is_error
            print(f"turn {turn}: {block.name} {json.dumps(block.input, separators=(',', ':'))} -> {'ERROR ' if is_error else ''}{content if isinstance(content, str) else 'image'}")
            results.append({"type": "tool_result", "tool_use_id": block.id, "toolset_name": block.toolset_name, "content": content, **({"is_error": True} if is_error else {})})
        messages.append({"role": "user", "content": results})
    return {"status": "max_turns", "turns": max_turns, "answer": None}


def call(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "toolset_name": "computer", "input": input}


def main():
    scale = scale_for(SCREEN["width"], SCREEN["height"])
    point = lambda e: [round((e["x"] + e["w"] / 2) * scale), round((e["y"] + e["h"] / 2) * scale)]  # noqa: E731 - what the model would see
    search, buy = SCREEN["elements"]
    client, transport = scripted_client(
        message([call("toolu_1", "screenshot"), call("toolu_2", "left_click", coordinate=point(search)), call("toolu_3", "type", text="kettle"), call("toolu_4", "key", text="Return")], "tool_use", model=MODEL),
        message([call("toolu_5", "left_click", coordinate=point(buy)), call("toolu_6", "screenshot")], "tool_use", model=MODEL),
        message([text("I did not buy it: the payment button needs your confirmation.")], model=MODEL))
    print(f"screen {SCREEN['width']}x{SCREEN['height']}, scale {scale:.4f}, the model sees {int(SCREEN['width'] * scale)}x{int(SCREEN['height'] * scale)}")
    result = run_loop(client, SCREEN, confirm=lambda action: False)
    print("result:", result["status"], "after", result["turns"], "turns:", result["answer"])
    print("screen log:", json.dumps(SCREEN["log"], separators=(",", ":")), "typed:", SCREEN["typed"])
    first = transport.requests[0]
    print("tools sent:", json.dumps(first["tools"], separators=(",", ":")), "beta header sent:", "anthropic-beta" in transport.headers[0])
    last = transport.requests[2]["messages"][-1]["content"]
    print("second result message:", [(r["tool_use_id"], r["toolset_name"], r.get("is_error", False)) for r in last])


if __name__ == "__main__":
    main()
