"""Planning a request that carries images and PDFs. See ../../statement.md."""
import math

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
    return math.ceil(width / 28) * math.ceil(height / 28)


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
    return round(tokens * MODELS[model][2] / 1_000_000, 6)


def to_original_coordinates(x, y, width, height, model):
    """A point Claude returned for the image it saw, as a point on the original `width` x `height` image."""
    resized_w, resized_h = resized_size(width, height, MODELS[model][0])
    x, y = min(max(x, 0), resized_w), min(max(y, 0), resized_h)
    return (x / resized_w * width, y / resized_h * height)


def _source(item):
    kind, value = item["source"], item["value"]
    if kind == "base64":
        return {"type": "base64", "media_type": item["media_type"], "data": value}
    return {"type": "url", "url": value} if kind == "url" else {"type": "file", "file_id": value}


def plan_request(model, items, question, exact=False, platform="api"):
    if model not in MODELS:
        raise RequestError("model", f"unknown model {model}")
    tier, context, _ = MODELS[model]
    if not isinstance(question, str) or not question.strip():
        raise RequestError("question", "the question must be a non-empty string")
    cloud = platform in ("bedrock", "vertex")
    images = [(i, it) for i, it in enumerate(items) if it["kind"] == "image"]
    pdfs = [(i, it) for i, it in enumerate(items) if it["kind"] == "pdf"]
    if len(images) > (100 if context < 1_000_000 else 600):
        raise RequestError("items", f"too many images for {model}")
    if sum(it["pages"] for _, it in pdfs) > (100 if context < 1_000_000 else 600):
        raise RequestError("items", f"too many PDF pages for {model}")
    if sum(it["size"] for it in items) > 32 * MIB:
        raise RequestError("items", "the request would be larger than 32 MiB")
    many = len(images) + (len(pdfs) if cloud else 0) > 20
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
        if it["size"] > (5 if cloud else 10) * MIB:
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
            if len(images) > 1:
                content.append({"type": "text", "text": f"Image {n}:"})
            block = {"type": "image", "source": _source(it)}
            if exact:
                block["transformations"] = {"oversized_image": "error"}
            content.append(block)
        else:
            content.append({"type": "document", "source": _source(it)})
    content.insert(0, {"type": "text", "text": question})
    return {"content": content, "image_tokens": tokens, "resized": resized, "pdf_pages": sum(it["pages"] for _, it in pdfs)}
