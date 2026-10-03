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
    # TODO: the visual tokens of an image of this size (one per 28 x 28 pixel patch, edges rounded up).
    return None


def resized_size(width, height, tier):
    # TODO: the (width, height) the model sees: the largest aspect-preserving size within the tier's edge and token limits.
    return None


def image_cost_usd(model, tokens):
    # TODO: the input cost in dollars of that many tokens, rounded to 6 decimals.
    return None


def to_original_coordinates(x, y, width, height, model):
    # TODO: a point Claude returned for the image it saw, as a point on the original width x height image.
    return None


def plan_request(model, items, question, exact=False, platform="api"):
    # TODO: validate the items and build the user content. See the statement for the rules and their order.
    return None
