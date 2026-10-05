"""Planning a request that carries images and PDFs. See ../../statement.md."""
import logging
import math

log = logging.getLogger(__name__)

MIB = 1024 * 1024
IMAGE_TYPES = ("image/jpeg", "image/png", "image/gif", "image/webp")
# model id -> (resolution tier, context window in tokens, input price in dollars per million tokens). Given.
MODELS = {
    "claude-fable-5-1": ("high", 1_000_000, 10.0),
    "claude-opus-5-5": ("high", 1_000_000, 4.0),
    "claude-sonnet-5-5": ("high", 1_000_000, 2.0),
    "claude-haiku-4-5": ("standard", 200_000, 1.0),
    "claude-haiku-4-5-20251001": ("standard", 200_000, 1.0),
}
# tier -> (longest edge in pixels, visual token budget). Given.
TIERS = {"standard": (1568, 1568), "high": (2576, 4784)}


class RequestError(Exception):
    """What your code raises, before any call, for a request the API would reject; `field` names the part."""

    def __init__(self, field, message):
        super().__init__(f"{field}: {message}")
        self.field = field


def visual_tokens(width, height):
    """TODO 1 of 8 (unlocks e1 and e7): the visual tokens of an image of this size.

    Receives the width and height in pixels. Returns one token per 28 x 28 pixel patch, each side rounded up to whole patches.
    Example: visual_tokens(29, 28) -> 2
    """
    return 0


def resized_size(width, height, tier):
    """The (width, height) the model sees: the largest aspect-preserving size within the tier's edge and token limits."""
    max_edge, max_tokens = TIERS[tier]

    def fits(w, h):
        return math.ceil(w / 28) * 28 <= max_edge and math.ceil(h / 28) * 28 <= max_edge and visual_tokens(w, h) <= max_tokens

    if fits(width, height):
        return (width, height)
    if height > width:
        h, w = resized_size(height, width, tier)
        return (w, h)
    ratio = width / height
    for long_edge in range(width - 1, 0, -1):
        short = max(round(long_edge / ratio), 1)  # round() is half to even, like the API
        if fits(long_edge, short):
            return (long_edge, short)
    return (1, 1)


def image_cost_usd(model, tokens):
    """TODO 2 of 8 (unlocks e7): the input cost in dollars of that many tokens for the model.

    Receives a model id and a token count. Returns tokens * price / 1,000,000 rounded to 6 decimals; the price is MODELS[model][2].
    Example: image_cost_usd("claude-haiku-4-5", 1000) -> 0.001
    """
    return 0.0


def to_original_coordinates(x, y, width, height, model):
    """A point Claude returned for the image it saw, as a point on the original `width` x `height` image."""
    """TODO 3 of 8 (unlocks e7): a point Claude returned for the image it saw, as a point on the original width x height image."""
    resized_w, resized_h = resized_size(width, height, MODELS[model][0])
    # Clamp x and y into the resized size, then scale them onto the original size. Example: a 3000 x 2000 image on a "high" model is
    # seen as 2576 x 1717 (not padded), so the point (1288, 0) becomes about (1500.0, 0.0).
    return (x, y)


def _source(item):
    """TODO 8 of 8 (unlocks m1, e5 and e6): the `source` object of an item.

    Receives an item. Returns {"type": "base64", "media_type", "data"} for source "base64", {"type": "url", "url"} for "url" and
    {"type": "file", "file_id"} for "file"; the text is item["value"].
    Example: {"source": "url", "value": "https://example.invalid/a.png"} -> {"type": "url", "url": "https://example.invalid/a.png"}
    """
    return {}


def _max_count(context):
    """TODO 4 of 8 (unlocks e2 and e5): how many images, and how many PDF pages, a request may hold.

    Receives the model's context window in tokens. Returns 100 when it is under 1,000,000 and 600 otherwise.
    Example: _max_count(200_000) -> 100
    """
    return 10**9


def _max_image_size(cloud):
    """TODO 5 of 8 (unlocks e2 and e6): the largest image payload in bytes.

    Receives True on bedrock and vertex. Returns 10 MiB, or 5 MiB when it is True.
    Example: _max_image_size(True) -> 5 * MIB
    """
    return 10**12


def _many(images, pdfs, cloud):
    """TODO 6 of 8 (unlocks e3): does the request hold more than 20 image blocks?

    Receives the image count, the PDF count and the cloud flag; on bedrock and vertex the PDFs count as well.
    Example: _many(18, 3, True) -> True, _many(18, 3, False) -> False
    """
    return False


def _image_blocks(item, n, image_count, exact):
    """TODO 7 of 8 (unlocks m1 and e4): the content blocks of the n-th image (counting from 1).

    Returns a text block `Image n:` first when there are two or more images, then the image block `{"type": "image", "source":
    _source(item)}`, which also carries `"transformations": {"oversized_image": "error"}` when `exact` is true.
    Example: _image_blocks(item, 2, 2, False) -> [{"type": "text", "text": "Image 2:"}, {"type": "image", "source": {...}}]
    """
    return []


def plan_request(model, items, question, exact=False, platform="api"):
    log.debug("plan_request input: %r", items)
    if model not in MODELS:
        raise RequestError("model", f"unknown model {model}")
    tier, context, _ = MODELS[model]
    if not isinstance(question, str) or not question.strip():
        raise RequestError("question", "the question must be a non-empty string")
    cloud = platform in ("bedrock", "vertex")
    images = [(i, it) for i, it in enumerate(items) if it["kind"] == "image"]
    pdfs = [(i, it) for i, it in enumerate(items) if it["kind"] == "pdf"]
    if len(images) > _max_count(context):
        raise RequestError("items", f"too many images for {model}")
    if sum(it["pages"] for _, it in pdfs) > _max_count(context):
        raise RequestError("items", f"too many PDF pages for {model}")
    if sum(it["size"] for it in items) > 32 * MIB:
        raise RequestError("items", "the request would be larger than 32 MiB")
    many = _many(len(images), len(pdfs), cloud)
    max_edge = TIERS[tier][0]
    tokens, resized = 0, []
    for i, it in enumerate(items):
        if cloud and it["source"] != "base64":
            raise RequestError(f"items[{i}].source", f"{platform} accepts base64 sources only")
        if it["kind"] == "pdf":
            if it["media_type"] != "application/pdf":
                raise RequestError(f"items[{i}].media_type", "a PDF must be application/pdf")
            continue
        if it["media_type"] not in IMAGE_TYPES:
            raise RequestError(f"items[{i}].media_type", f"{it['media_type']} is not a supported image format")
        if it["width"] > 8000 or it["height"] > 8000:
            raise RequestError(f"items[{i}].dimensions", "an image may not exceed 8000 x 8000 pixels")
        if it["size"] > _max_image_size(cloud):
            raise RequestError(f"items[{i}].size", "the image is too large")
        if many and max(it["width"], it["height"]) > 2000:
            raise RequestError(f"items[{i}].dimensions", "with more than 20 images, no side may exceed 2000 pixels")
        seen = resized_size(it["width"], it["height"], tier)
        if seen != (it["width"], it["height"]):
            if exact:
                raise RequestError(f"items[{i}].dimensions", f"would be resized to {seen[0]}x{seen[1]}")
            resized.append(it["name"])
        tokens += visual_tokens(*seen)
    content, n = [], 0
    for _, it in sorted(images + pdfs, key=lambda p: p[0]):
        if it["kind"] == "image":
            n += 1
            content.extend(_image_blocks(it, n, len(images), exact))
        else:
            content.append({"type": "document", "source": _source(it)})
    content.append({"type": "text", "text": question})
    return {"content": content, "image_tokens": tokens, "resized": resized, "pdf_pages": sum(it["pages"] for _, it in pdfs)}
