"""Build, split and read Message Batches. See ../../statement.md for the contract."""
import json
import re

CUSTOM_ID = re.compile(r"^[a-zA-Z0-9_-]{1,64}$")
MAX_REQUESTS = 100_000
MAX_BYTES = 256 * 1024 * 1024
USAGE_KEYS = ("input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens")


class BatchError(Exception):
    """The batch would be refused. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def build_requests(items):
    seen, requests = set(), []
    for item in items:
        custom_id, params = item["id"], item["params"]
        if not isinstance(custom_id, str) or not CUSTOM_ID.match(custom_id):
            raise BatchError("custom_id", f"{custom_id!r} is not 1 to 64 letters, digits, hyphens or underscores")
        if custom_id in seen:
            raise BatchError("custom_id", f"{custom_id} is used twice")
        seen.add(custom_id)
        if params.get("max_tokens", 0) < 1:
            raise BatchError("params.max_tokens", "must be at least 1 inside a batch")
        requests.append({"custom_id": custom_id, "params": params})
    return requests


def _size(request):
    return len(json.dumps(request, separators=(",", ":")).encode("utf-8"))


def split_batches(requests, max_requests=MAX_REQUESTS, max_bytes=MAX_BYTES):
    batches, current, used = [], [], 0
    for request in requests:
        size = _size(request)
        if size > max_bytes:
            raise BatchError("size", f"{request['custom_id']} alone is larger than a batch may be")
        if current and (len(current) >= max_requests or used + size > max_bytes):
            batches.append(current)
            current, used = [], 0
        current.append(request)
        used += size
    if current:
        batches.append(current)
    return batches


def collect(requests, result_lines):
    wanted = [r["custom_id"] for r in requests]
    by_id, unknown = {}, []
    for line in result_lines:
        if not line.strip():
            continue
        record = json.loads(line)
        cid = record["custom_id"]
        if cid not in wanted:
            unknown.append(cid)
        elif cid not in by_id:
            by_id[cid] = record["result"]
    outcomes, retry, fix = [], [], []
    usage = {key: 0 for key in USAGE_KEYS}
    for cid in wanted:
        result = by_id.get(cid)
        if result is None:
            outcomes.append({"custom_id": cid, "status": "missing"})
            retry.append(cid)
        elif result["type"] == "succeeded":
            message = result["message"]
            text = "".join(b.get("text", "") for b in message["content"] if b.get("type") == "text")
            outcomes.append({"custom_id": cid, "status": "succeeded", "text": text, "usage": message["usage"]})
            for key in USAGE_KEYS:
                usage[key] += message["usage"].get(key, 0)
        elif result["type"] == "errored":
            kind = result["error"]["error"]["type"]
            outcomes.append({"custom_id": cid, "status": "errored", "error_type": kind})
            (fix if kind == "invalid_request_error" else retry).append(cid)
        else:  # canceled or expired: the request never reached the model
            outcomes.append({"custom_id": cid, "status": result["type"]})
            retry.append(cid)
    return {"outcomes": outcomes, "retry": retry, "fix": fix, "unknown": unknown, "usage": usage}
