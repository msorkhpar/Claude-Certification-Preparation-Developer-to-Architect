"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from vision import RequestError, plan_request


def img(name, w, h, media_type="image/png", source="base64", value="AAAA"):
    return {"kind": "image", "name": name, "media_type": media_type, "width": w, "height": h, "size": 1000, "source": source, "value": value}


# Two images and a question: the planner puts the images first, each labelled, and the question last.
items = [img("chart", 1000, 1000),
         img("photo", 200, 200, "image/jpeg", "url", "https://example.invalid/p.jpg")]
try:
    plan = plan_request("claude-opus-5-5", items, "What changed?") or {}
    for block in plan.get("content", []):
        print("block:", block["type"], block.get("text", ""))
    print("image tokens:", plan.get("image_tokens"))
    print("resized:", plan.get("resized"))
except RequestError as err:
    print("refused:", err)
