"""One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md."""
import copy
import logging

log = logging.getLogger(__name__)

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
    """TODO 1 of 7 (finish this to pass e1): the model name without its date.

    Receives a model id. Returns HAIKU when the id starts with HAIKU (so a dated id such as claude-haiku-4-5-20251001 and the plain name
    are one family), the id itself otherwise. Example: _family("claude-haiku-4-5-20251001") -> "claude-haiku-4-5"
    """
    return model


def _check_bedrock(model, family, config):
    """TODO 2 of 7 (finish this to pass e4 and e6): refuse what Bedrock cannot take.

    Receives the model id, its family and the config map. Raises PlatformError("model", reason) when the family is not in BEDROCK_MODELS,
    and PlatformError("config", reason) when config has no region or an empty one. Example: _check_bedrock("claude-sonnet-4-6",
    "claude-sonnet-4-6", {"region": "us-east-1"}) raises with field "model"
    """


def _bedrock_model_id(family):
    """TODO 3 of 7 (finish this to pass m1 and e1): the model id in a Bedrock body.

    Receives the family. Returns "anthropic." followed by it. Example: _bedrock_model_id("claude-opus-5-5") -> "anthropic.claude-opus-5-5"
    """
    return family


def _vertex_model_id(family):
    """TODO 4 of 7 (finish this to pass e1): the model id in a Vertex URL.

    Receives the family. Returns HAIKU_VERTEX for HAIKU and the family itself for the others.
    Example: _vertex_model_id("claude-haiku-4-5") -> "claude-haiku-4-5@20251001"
    """
    return family


def _vertex_host(endpoint, family, model):
    """TODO 5 of 7 (finish this to pass m1 and e3): the host of a Vertex endpoint.

    Receives the endpoint, the model's family and the model id. Returns aiplatform.googleapis.com for "global", aiplatform.<endpoint>.rep.googleapis.com
    for "us" and "eu", and <endpoint>-aiplatform.googleapis.com for a specific region, which raises PlatformError("endpoint", reason) unless
    the family is in REGIONAL_VERTEX_MODELS. Example: _vertex_host("eu", "claude-opus-5-5", "claude-opus-5-5") -> "aiplatform.eu.rep.googleapis.com"
    """
    return ""


def _vertex_body(body):
    """TODO 6 of 7 (finish this to pass e2): the body Vertex takes.

    Receives the caller's body. Returns a deep copy without the `model` key (the model is in the URL) and with `anthropic_version` set to
    VERTEX_VERSION; the caller's own body is never changed. Example: {"model": "x", "max_tokens": 5} -> {"max_tokens": 5, "anthropic_version": ...}
    """
    return copy.deepcopy(body)


def _lacking(platform, features):
    """TODO 7 of 7 (finish this to pass e5): the features this platform lacks.

    Receives a known platform and a list of known feature names. Returns the names that are in MISSING[platform], in the order given.
    Example: _lacking("vertex", ["batches", "thinking"]) -> ["batches"]
    """
    return []


def build_request(platform, model, body, config):
    log.debug("build_request input: %s %s %r %r", platform, model, body, config)
    family = _family(model)
    if platform == "anthropic":
        return {"method": "POST", "url": "https://api.anthropic.com/v1/messages",
                "headers": {"anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json"},
                "body": {**copy.deepcopy(body), "model": model}}
    if platform == "bedrock":
        _check_bedrock(model, family, config)
        region = config.get("region")
        return {"method": "POST", "url": f"https://bedrock-mantle.{region}.api.aws/anthropic/v1/messages",
                "headers": {"anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json"},
                "body": {**copy.deepcopy(body), "model": _bedrock_model_id(family)}}
    if platform == "vertex":
        if family not in VERTEX_MODELS:
            raise PlatformError("model", f"{model} is not served on Google Vertex AI")
        project = config.get("project")
        if not project:
            raise PlatformError("config", "a Vertex request needs a project")
        endpoint = config.get("endpoint", "global")
        path = f"/v1/projects/{project}/locations/{endpoint}/publishers/anthropic/models/{_vertex_model_id(family)}:rawPredict"
        host = _vertex_host(endpoint, family, model)
        return {"method": "POST", "url": f"https://{host}{path}", "headers": {"content-type": "application/json"}, "body": _vertex_body(body)}
    raise PlatformError("platform", f"unknown platform {platform}")


def unsupported_features(platform, features):
    if platform not in MISSING:
        raise PlatformError("platform", f"unknown platform {platform}")
    for name in features:
        if name not in FEATURES:
            raise PlatformError("feature", f"unknown feature {name}")
    return _lacking(platform, features)
