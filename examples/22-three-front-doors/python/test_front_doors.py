from front_doors import DOORS, send


def test_only_the_direct_api_and_bedrock_put_the_model_in_the_body():
    models = {name: send(name)[0].requests[0].get("model") for name in DOORS}
    assert models == {"anthropic": "claude-sonnet-5-5", "bedrock": "anthropic.claude-sonnet-5-5", "vertex": None}


def test_vertex_puts_its_version_in_the_body_and_sends_no_version_header():
    transport, _ = send("vertex")
    assert transport.requests[0]["anthropic_version"] == "vertex-2023-10-16"
    assert "anthropic-version" not in transport.headers[0]


def test_the_model_is_in_the_vertex_url():
    assert "/publishers/anthropic/models/claude-sonnet-5-5:rawPredict" in send("vertex")[0].urls[0]


def test_every_door_returns_the_same_reply_shape():
    texts = {name: send(name)[1].json()["content"][0]["text"] for name in DOORS}
    assert set(texts.values()) == {"Paris."}
