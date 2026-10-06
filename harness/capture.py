"""Recorder for an approved capture run (never used by graded tests against a live API).

RecordingTransport wraps any transport (the live one in a capture run, a scripted one in tests),
keeps only request fields the replay checks and the response body, and drops every header.
The key comes from the environment in the capture run; nothing here reads or stores it.
"""
import json

import httpx2

from .scrub import check_capture, scrub


class RecordingTransport(httpx2.BaseTransport):
    def __init__(self, inner, model, sdk, captured):
        self.inner, self.meta, self.exchanges = inner, (model, sdk, captured), []

    def handle_request(self, request):
        body = json.loads(request.content or b"{}")
        response = self.inner.handle_request(request)
        response.read()
        if response.status_code == 200:
            self.exchanges.append({"expect": {"model": body.get("model"), "messages": len(body.get("messages", []))},
                                   "response": scrub(json.loads(response.content))})
        return response

    def write(self, path, label):
        model, sdk, captured = self.meta
        data = {"label": label, "illustrative": False, "model": model, "sdk": sdk,
                "captured": captured, "exchanges": self.exchanges}
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
        check_capture(path)  # a capture that fails the scrub check is an error, not a file to keep
