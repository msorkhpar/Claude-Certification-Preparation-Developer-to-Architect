"""The same question sent to the direct API, to Claude in Amazon Bedrock and to Claude on Google Vertex AI.

Each request is written out by hand and sent through a scripted transport, so nothing leaves the container and nothing
is signed: AWS SigV4 (Bedrock), a Google access token (Vertex) or an API key (direct) is added by an SDK or a proxy. The
reply is the same illustrative, hand-written Messages response for all three, because the response body keeps the same shape.
"""
import logging
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

log = logging.getLogger(__name__)

BASE = {"max_tokens": 64, "messages": [{"role": "user", "content": "Capital of France?"}]}
PROJECT, REGION = "example-project", "us-east-1"
DOORS = {
    "anthropic": ("https://api.anthropic.com/v1/messages",
                  {"anthropic-version": "2023-06-01", "content-type": "application/json"},
                  {"model": "claude-sonnet-5-5", **BASE}),
    "bedrock": (f"https://bedrock-mantle.{REGION}.api.aws/anthropic/v1/messages",
                {"anthropic-version": "2023-06-01", "content-type": "application/json"},
                {"model": "anthropic.claude-sonnet-5-5", **BASE}),
    "vertex": (f"https://aiplatform.googleapis.com/v1/projects/{PROJECT}/locations/global/publishers/anthropic/models/claude-sonnet-5-5:rawPredict",
               {"content-type": "application/json"},
               {"anthropic_version": "vertex-2023-10-16", **BASE}),
}


def send(name):
    url, headers, body = DOORS[name]
    transport = ScriptedTransport(message([text("Paris.")]))
    with httpx2.Client(transport=transport) as http:
        reply = http.post(url, headers=headers, json=body)
    return transport, reply


def main():
    for name in DOORS:
        transport, reply = send(name)
        sent, headers = transport.requests[0], transport.headers[0]
        print(f"{name:9} {transport.urls[0]}")
        print(f"          version header: {headers.get('anthropic-version', 'none')} | body model: {sent.get('model', 'none')} | body version: {sent.get('anthropic_version', 'none')}")
        print(f"          reply: {reply.status_code} {reply.json()['content'][0]['text']!r} (same parser for every door)")


if __name__ == "__main__":
    main()
