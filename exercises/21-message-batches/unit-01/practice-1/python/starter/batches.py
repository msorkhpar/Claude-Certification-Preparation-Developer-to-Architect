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
    """TODO 1 of 6 (finish this to pass e1): refuse a bad or repeated custom id, and remember a good one.

    Receives the id and the set `seen` of ids already used. Raises BatchError("custom_id", reason) when the id is not a string matching
    CUSTOM_ID (1 to 64 letters, digits, hyphens or underscores) or is already in `seen`; otherwise adds it to `seen`.
    Example: _check_custom_id("has space", set()) raises; calling _check_custom_id("a", seen) twice raises the second time
    """


def _check_params(params):
    """TODO 2 of 6 (finish this to pass e2): refuse the parameters a batch cannot take.

    Receives a request body. Raises BatchError("params.max_tokens", reason) when max_tokens is missing or below 1,
    BatchError("params.stream", reason) when stream is true, and BatchError("params.speed", reason) when speed is present at all.
    Example: _check_params({"max_tokens": 0}) raises with field "params.max_tokens"
    """


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
    """TODO 3 of 6 (finish this to pass e3): must this request go into a new batch?

    Receives the number of requests in the current batch, their total bytes `used`, the size of the next request and both limits.
    Returns True when the current batch is not empty and adding the request would pass either limit (count already at max_requests, or
    used + size above max_bytes). Example: _must_start_new(3, 10, 5, 3, 1000) -> True, _must_start_new(0, 0, 5, 3, 1000) -> False
    """
    return False


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
    """TODO 4 of 6 (finish this to pass m1 and e5): file one parsed result line by its custom id.

    Receives the list `wanted` of request ids, the dict `by_id`, the list `unknown` and the parsed `record` ({"custom_id", "result"}).
    Adds the id to `unknown` when no request carries it; otherwise stores record["result"] in `by_id` unless the id is already there
    (the first result wins). Example: a record for "zzz" when wanted is ["a"] puts "zzz" in unknown
    """


def _needs_fix(error_type):
    """TODO 5 of 6 (finish this to pass e4): does an errored result need a corrected request?

    Receives the error type of an errored result. Returns True for "invalid_request_error" (it fails again until fixed), False for any
    other type, which is worth sending again unchanged. Example: _needs_fix("overloaded_error") -> False
    """
    return False


def _add_usage(usage, used):
    """TODO 6 of 6 (finish this to pass e6): add one succeeded reply's usage to the totals.

    Receives the dict `usage` of totals (every key of USAGE_KEYS starts at 0) and the reply's `usage` map, which may lack a key.
    Adds each key of USAGE_KEYS from `used` to `usage`, a missing key counting as 0. Example: adding {"input_tokens": 5} raises
    usage["input_tokens"] by 5 and leaves the other three alone
    """


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
