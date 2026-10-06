"""Build, split and read Message Batches. See ../../statement.md for the contract."""
import json
import logging
import re

log = logging.getLogger(__name__)

CUSTOM_ID = re.compile(r"^[a-zA-Z0-9_-]{1,64}$")
MAX_REQUESTS = 100_000
MAX_BYTES = 256 * 1024 * 1024
USAGE_KEYS = ("input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens")


class BatchError(Exception):
    """The batch would be refused. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def _check_custom_id(custom_id, seen):
    if not isinstance(custom_id, str) or not CUSTOM_ID.match(custom_id):
        raise BatchError("custom_id", f"{custom_id!r} is not 1 to 64 letters, digits, hyphens or underscores")
    if custom_id in seen:
        raise BatchError("custom_id", f"{custom_id} is used twice")
    seen.add(custom_id)


def _check_params(params):
    if params.get("max_tokens", 0) < 1:
        raise BatchError("params.max_tokens", "must be at least 1 inside a batch")
    if params.get("stream"):
        raise BatchError("params.stream", "batch results are a file, not a stream")
    if "speed" in params:
        raise BatchError("params.speed", "fast mode is not available in a batch")


def build_requests(items):
    log.debug("build_requests input: %r", items)
    seen, requests = set(), []
    for item in items:
        custom_id, params = item["id"], item["params"]
        _check_custom_id(custom_id, seen)
        _check_params(params)
        requests.append({"custom_id": custom_id, "params": params})
    return requests


def _size(request):
    return len(json.dumps(request, separators=(",", ":")).encode("utf-8"))


def _must_start_new(count, used, size, max_requests, max_bytes):
    return count > 0 and (count >= max_requests or used + size > max_bytes)


def split_batches(requests, max_requests=MAX_REQUESTS, max_bytes=MAX_BYTES):
    batches, current, used = [], [], 0
    for request in requests:
        size = _size(request)
        if size > max_bytes:
            raise BatchError("size", f"{request['custom_id']} alone is larger than a batch may be")
        if _must_start_new(len(current), used, size, max_requests, max_bytes):
            batches.append(current)
            current, used = [], 0
        current.append(request)
        used += size
    if current:
        batches.append(current)
    return batches


def _keep_result(wanted, by_id, unknown, record):
    cid = record["custom_id"]
    if cid not in wanted:
        unknown.append(cid)
    elif cid not in by_id:
        by_id[cid] = record["result"]


def _needs_fix(error_type):
    return error_type == "invalid_request_error"


def _add_usage(usage, used):
    for key in USAGE_KEYS:
        usage[key] += used.get(key, 0)


def collect(requests, result_lines):
    wanted = [r["custom_id"] for r in requests]
    by_id, unknown = {}, []
    for line in result_lines:
        if line.strip():
            _keep_result(wanted, by_id, unknown, json.loads(line))
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
            _add_usage(usage, message["usage"])
        elif result["type"] == "errored":
            kind = result["error"]["error"]["type"]
            outcomes.append({"custom_id": cid, "status": "errored", "error_type": kind})
            (fix if _needs_fix(kind) else retry).append(cid)
        else:  # canceled or expired: the request never reached the model
            outcomes.append({"custom_id": cid, "status": result["type"]})
            retry.append(cid)
    return {"outcomes": outcomes, "retry": retry, "fix": fix, "unknown": unknown, "usage": usage}
