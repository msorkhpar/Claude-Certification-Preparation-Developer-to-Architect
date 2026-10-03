"""One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md."""
import copy

ANTHROPIC_VERSION = "2023-06-01"
VERTEX_VERSION = "vertex-2023-10-16"
HAIKU = "claude-haiku-4-5"
HAIKU_VERTEX = "claude-haiku-4-5@20251001"
BEDROCK_MODELS = {"claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", HAIKU}
VERTEX_MODELS = BEDROCK_MODELS | {"claude-sonnet-4-6"}
REGIONAL_VERTEX_MODELS = {"claude-sonnet-4-6"}  # specific regional endpoints serve Sonnet 4.6 and earlier only
MISSING = {
    "anthropic": set(),
    "bedrock": {"batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector",
                "structured_outputs", "skills", "managed_agents"},
    "vertex": {"batches", "fast_mode", "files_api", "web_fetch", "code_execution", "mcp_connector", "skills", "managed_agents"},
}
FEATURES = {"batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector",
            "structured_outputs", "skills", "managed_agents", "prompt_caching", "thinking", "tool_use", "citations"}


class PlatformError(Exception):
    """The request cannot be built for this platform. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def _family(model):
    return HAIKU if model.startswith(HAIKU) else model


def build_request(platform, model, body, config):
    family = _family(model)
    if platform == "anthropic":
        return {"method": "POST", "url": "https://api.anthropic.com/v1/messages",
                "headers": {"anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json"},
                "body": {**copy.deepcopy(body), "model": model}}
    if platform == "bedrock":
        if family not in BEDROCK_MODELS:
            raise PlatformError("model", f"{model} is not served by Claude in Amazon Bedrock")
        region = config.get("region")
        if not region:
            raise PlatformError("config", "a Bedrock request needs a region")
        return {"method": "POST", "url": f"https://bedrock-mantle.{region}.api.aws/anthropic/v1/messages",
                "headers": {"anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json"},
                "body": {**copy.deepcopy(body), "model": f"anthropic.{family}"}}
    if platform == "vertex":
        if family not in VERTEX_MODELS:
            raise PlatformError("model", f"{model} is not served on Google Vertex AI")
        project = config.get("project")
        if not project:
            raise PlatformError("config", "a Vertex request needs a project")
        endpoint = config.get("endpoint", "global")
        model_id = HAIKU_VERTEX if family == HAIKU else family
        path = f"/v1/projects/{project}/locations/{endpoint}/publishers/anthropic/models/{model_id}:rawPredict"
        if endpoint == "global":
            host = "aiplatform.googleapis.com"
        elif endpoint in ("us", "eu"):
            host = f"aiplatform.{endpoint}.rep.googleapis.com"
        else:
            if family not in REGIONAL_VERTEX_MODELS:
                raise PlatformError("endpoint", f"{model} is served on the global and multi-region endpoints, not on a specific region")
            host = f"{endpoint}-aiplatform.googleapis.com"
        sent = body
        sent.pop("model", None)
        sent["anthropic_version"] = VERTEX_VERSION
        return {"method": "POST", "url": f"https://{host}{path}", "headers": {"content-type": "application/json"}, "body": sent}
    raise PlatformError("platform", f"unknown platform {platform}")


def unsupported_features(platform, features):
    if platform not in MISSING:
        raise PlatformError("platform", f"unknown platform {platform}")
    for name in features:
        if name not in FEATURES:
            raise PlatformError("feature", f"unknown feature {name}")
    return [name for name in features if name in MISSING[platform]]
