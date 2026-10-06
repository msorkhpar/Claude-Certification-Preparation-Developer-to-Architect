"""Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.

The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
"""
import logging
import json
import math

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
TIERS = {"standard": (1568, 1568), "high": (2576, 4784)}  # tier -> (longest edge in pixels, visual token budget)
PRICE = 2.0  # dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)


def visual_tokens(width, height):
    return math.ceil(width / 28) * math.ceil(height / 28)


def padded(width, height):
    """Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content."""
    return (math.ceil(width / 28) * 28, math.ceil(height / 28) * 28)


def resized_size(width, height, tier):
    """The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget."""
    max_edge, max_tokens = TIERS[tier]

    def fits(w, h):
        return max(padded(w, h)) <= max_edge and visual_tokens(w, h) <= max_tokens

    if fits(width, height):
        return (width, height)
    if height > width:
        h, w = resized_size(height, width, tier)
        return (w, h)
    ratio = width / height
    for long_edge in range(width - 1, 0, -1):
        short = max(round(long_edge / ratio), 1)
        if fits(long_edge, short):
            return (long_edge, short)
    return (1, 1)


def image_block(kind, value, media_type="image/png"):
    """An image block with one of the three sources of the Messages API: base64, url or file (a Files API id)."""
    source = {"base64": {"type": "base64", "media_type": media_type, "data": value}, "url": {"type": "url", "url": value}, "file": {"type": "file", "file_id": value}}[kind]
    return {"type": "image", "source": source}


def document_block(kind, value, title=None):
    source = {"base64": {"type": "base64", "media_type": "application/pdf", "data": value}, "url": {"type": "url", "url": value}, "file": {"type": "file", "file_id": value}}[kind]
    return {"type": "document", "source": source, **({"title": title} if title else {})}


def comparison(images, question):
    """Several images are each introduced by a label, and the question comes last."""
    content = []
    for number, image in enumerate(images, 1):
        content += [{"type": "text", "text": f"Image {number}:"}, image]
    return content + [{"type": "text", "text": question}]


def to_original(x, y, width, height, tier):
    """A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one."""
    rw, rh = resized_size(width, height, tier)
    return (x / rw * width, y / rh * height)


def shape(block):
    if block["type"] == "text":
        return "text"
    return f'{block["type"]}/{block["source"]["type"]}'


def main():
    client, transport = scripted_client(message([text("Image 1 has the larger bars; image 2 matches the table in the PDF.")], model=MODEL))
    content = comparison([image_block("base64", "iVBORw0KGgo="), image_block("url", "https://example.invalid/chart.png"), image_block("file", "file_011CNha8iCJcU1wXNR6q4V8w")],
                         "Compare the images with the table in the document.")
    content.insert(len(content) - 1, document_block("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"))
    reply = client.messages.create(model=MODEL, max_tokens=300, messages=[{"role": "user", "content": content}])
    sent = transport.requests[0]["messages"][0]["content"]
    print("content order:", ", ".join(shape(b) for b in sent))
    print("labels:", " ".join(b["text"] for b in sent if b["type"] == "text" and b["text"].startswith("Image")))
    print("one image block:", json.dumps(sent[1], separators=(",", ":")))
    print("reply:", reply.content[0].text)
    print()
    print("size         tier       seen        padded      tokens  dollars per 1000 images")
    for width, height in [(200, 200), (1920, 1080), (3840, 2160), (1075, 1520)]:
        for tier in ("standard", "high"):
            seen = resized_size(width, height, tier)
            tokens = visual_tokens(*seen)
            print(f"{f'{width}x{height}':<12} {tier:<10} {f'{seen[0]}x{seen[1]}':<11} {'x'.join(map(str, padded(*seen))):<11} {tokens:>6}  {tokens * PRICE / 1000:.2f}")
    print()
    x, y = to_original(462, 654, 1075, 1520, "standard")
    wrong_x, wrong_y = 462 / padded(*resized_size(1075, 1520, "standard"))[0] * 1075, 654 / padded(*resized_size(1075, 1520, "standard"))[1] * 1520
    print(f"point (462, 654) on the picture Claude saw of a 1075x1520 scan: ({x:.1f}, {y:.1f}) on the original")
    print(f"dividing by the padded size instead gives ({wrong_x:.1f}, {wrong_y:.1f}), {abs(y - wrong_y):.1f} pixels off")


if __name__ == "__main__":
    main()
