"""Replay a recorded exchange file through the SDK's transport hook.

File shape (JSON): {"label": "...", "illustrative": bool, "model": id, "sdk": "anthropic X",
"captured": date or null, "exchanges": [{"expect": {"model": id, "messages": n}, "response": {...}}]}
`expect` is optional and only checks the request the reader's code sent, so a changed prompt that
no longer matches the capture fails loudly instead of replaying a stale answer.
"""
import json
from pathlib import Path

import anthropic
import httpx2
from anthropic.types import Message


def load_exchange(path):
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    for key in ("label", "illustrative", "model", "sdk", "captured", "exchanges"):
        if key not in data:
            raise ValueError(f"exchange file lacks '{key}'")
    for ex in data["exchanges"]:
        Message.model_validate(ex["response"])
    return data


class ReplayTransport(httpx2.BaseTransport):
    def __init__(self, exchange):
        self.exchange = exchange
        self.pending = list(exchange["exchanges"])
        self.requests = []

    def handle_request(self, request):
        body = json.loads(request.content or b"{}")
        self.requests.append(body)
        if not self.pending:
            return httpx2.Response(500, json={"type": "error", "error": {
                "type": "api_error", "message": "replay exhausted"}})
        ex = self.pending.pop(0)
        expect = ex.get("expect", {})
        if "model" in expect and body.get("model") != expect["model"]:
            return self._mismatch(f"model {body.get('model')!r} != {expect['model']!r}")
        if "messages" in expect and len(body.get("messages", [])) != expect["messages"]:
            return self._mismatch(f"{len(body.get('messages', []))} messages != {expect['messages']}")
        return httpx2.Response(200, json=ex["response"])

    @staticmethod
    def _mismatch(why):
        return httpx2.Response(400, json={"type": "error", "error": {
            "type": "invalid_request_error", "message": "replay mismatch: " + why}})


def replay_client(exchange):
    transport = ReplayTransport(exchange)
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0,
                                 http_client=httpx2.Client(transport=transport))
    return client, transport
