import copy
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from platforms import PlatformError, build_request, unsupported_features

BODY = {"model": "ignored-by-the-builder", "max_tokens": 256, "messages": [{"role": "user", "content": "Hello, Claude"}]}
OPUS, SONNET5, SONNET46, HAIKU, FABLE = "claude-opus-5-5", "claude-sonnet-5-5", "claude-sonnet-4-6", "claude-haiku-4-5-20251001", "claude-fable-5-1"


def built(platform, model, body=BODY, **config):
    return build_request(platform, model, copy.deepcopy(body), config) or {}


def g(request, *path):
    """request[path...] or None when a step is missing (a starter returns nothing)."""
    for step in path:
        request = request.get(step) if isinstance(request, dict) else None
    return request


def field_of(platform, model, **config):
    try:
        build_request(platform, model, copy.deepcopy(BODY), config)
    except PlatformError as err:
        return err.field
    except Exception as err:  # noqa: BLE001
        return f"crash: {err!r}"
    return None


def test_m1_the_same_message_takes_three_shapes():
    direct = built("anthropic", OPUS)
    assert g(direct, "url") == "https://api.anthropic.com/v1/messages"
    assert g(direct, "headers") == {"anthropic-version": "2023-06-01", "content-type": "application/json"}
    assert g(direct, "body", "model") == OPUS
    bedrock = built("bedrock", OPUS, region="us-east-1")
    assert g(bedrock, "url") == "https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages"
    assert g(bedrock, "headers", "anthropic-version") == "2023-06-01"
    assert g(bedrock, "body", "model") == "anthropic.claude-opus-5-5"
    vertex = built("vertex", OPUS, project="my-project")
    assert g(vertex, "url") == "https://aiplatform.googleapis.com/v1/projects/my-project/locations/global/publishers/anthropic/models/claude-opus-5-5:rawPredict"
    assert "anthropic-version" not in (g(vertex, "headers") or {"anthropic-version": 1})
    assert g(vertex, "method") == "POST"


def test_e1_model_ids_change_with_the_platform():
    assert g(built("anthropic", HAIKU), "body", "model") == HAIKU
    assert g(built("bedrock", HAIKU, region="eu-west-1"), "body", "model") == "anthropic.claude-haiku-4-5"
    assert g(built("bedrock", "claude-haiku-4-5", region="eu-west-1"), "body", "model") == "anthropic.claude-haiku-4-5"
    assert g(built("bedrock", FABLE, region="us-east-1"), "body", "model") == "anthropic.claude-fable-5-1"
    assert (g(built("vertex", HAIKU, project="p"), "url") or "").endswith("/models/claude-haiku-4-5@20251001:rawPredict")
    assert (g(built("vertex", SONNET5, project="p"), "url") or "").endswith("/models/claude-sonnet-5-5:rawPredict")


def test_e2_vertex_moves_the_model_into_the_url_and_the_version_into_the_body():
    body = copy.deepcopy(BODY)
    sent = (build_request("vertex", OPUS, body, {"project": "p"}) or {}).get("body", {})
    assert "model" not in sent and sent.get("anthropic_version") == "vertex-2023-10-16"
    assert sent.get("max_tokens") == 256 and sent.get("messages") == BODY["messages"]
    assert body == BODY  # the caller's body is left as it was
    other = (build_request("bedrock", OPUS, body, {"region": "us-east-1"}) or {}).get("body", {})
    assert "anthropic_version" not in other and other.get("max_tokens") == 256
    assert body == BODY


def test_e3_vertex_endpoints_global_multi_region_and_regional():
    assert g(built("vertex", OPUS, project="p", endpoint="us"), "url") == "https://aiplatform.us.rep.googleapis.com/v1/projects/p/locations/us/publishers/anthropic/models/claude-opus-5-5:rawPredict"
    assert (g(built("vertex", SONNET5, project="p", endpoint="eu"), "url") or "").startswith("https://aiplatform.eu.rep.googleapis.com/v1/projects/p/locations/eu/")
    assert g(built("vertex", SONNET46, project="p", endpoint="europe-west1"), "url") == "https://europe-west1-aiplatform.googleapis.com/v1/projects/p/locations/europe-west1/publishers/anthropic/models/claude-sonnet-4-6:rawPredict"
    assert field_of("vertex", OPUS, project="p", endpoint="europe-west1") == "endpoint"
    assert field_of("vertex", HAIKU, project="p", endpoint="us-east5") == "endpoint"


def test_e4_a_platform_serves_only_its_own_models():
    assert field_of("bedrock", SONNET46, region="us-east-1") == "model"
    assert field_of("bedrock", "claude-nonexistent-9", region="us-east-1") == "model"
    assert field_of("vertex", "claude-nonexistent-9", project="p") == "model"
    assert field_of("vertex", SONNET46, project="p") is None
    assert field_of("mystery", OPUS) == "platform"


def test_e5_each_platform_lacks_its_own_features():
    names = ["batches", "fast_mode", "prompt_caching", "thinking", "web_search", "structured_outputs", "files_api", "tool_use"]
    assert unsupported_features("anthropic", names) == []
    assert unsupported_features("bedrock", names) == ["batches", "fast_mode", "web_search", "structured_outputs", "files_api"]
    assert unsupported_features("vertex", names) == ["batches", "fast_mode", "files_api"]
    assert unsupported_features("vertex", ["web_fetch", "mcp_connector", "citations"]) == ["web_fetch", "mcp_connector"]


def test_e6_a_request_needs_the_place_it_is_sent_to():
    assert field_of("bedrock", OPUS) == "config"
    assert field_of("vertex", OPUS) == "config"
    assert field_of("bedrock", OPUS, region="") == "config"
    assert field_of("anthropic", OPUS) is None
