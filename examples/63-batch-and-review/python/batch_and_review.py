"""What a batch asks of its caller, and what an independent review is given.

Read on 2026-10-04 in the Claude API documentation ("Batch processing"): a batch is processed asynchronously, results are available when every request has finished or after 24 hours, whichever comes first,
a request is identified by its `custom_id` (1 to 64 letters, digits, hyphens and underscores), results can come back in any order, and `stream`, `speed` and a `max_tokens` of 0 are refused. The exam guide's wording for
task 4.5 (a batch has no latency guarantee and cannot run a tool mid-request) and 4.6 (an independent instance reviews better than the generator) is what the functions below make visible. Nothing here calls a model.
"""
import re
import logging

log = logging.getLogger(__name__)

CUSTOM_ID = re.compile(r"^[a-zA-Z0-9_-]{1,64}$")


def worst_case_wait(interval_hours, window_hours=24, handling_hours=2):
    """An item that arrives just after a submission waits one interval for the next batch, then the processing window, then the handling."""
    return interval_hours + window_hours + handling_hours


def batch_entry(custom_id, params):
    """One entry of a batch request, refused for the same reasons the API refuses it."""
    if not CUSTOM_ID.match(custom_id):
        raise ValueError(f"custom_id '{custom_id}' must be 1 to 64 letters, digits, hyphens or underscores")
    if params.get("stream") is True:
        raise ValueError("stream is not supported in a batch")
    if "speed" in params:
        raise ValueError("speed is not supported in a batch")
    if params.get("max_tokens") == 0:
        raise ValueError("a max_tokens of 0 is not supported in a batch")
    return {"custom_id": custom_id, "params": params}


def match_results(requests, results):
    """Results come back in any order: pair them with the requests by custom_id, and report a result nobody asked for."""
    by_id = dict(results)
    asked = set(requests)
    return [(r, by_id.get(r, "missing")) for r in requests], [i for i, _ in results if i not in asked]


def review_request(code, reasoning, independent):
    """What the reviewing instance receives: an independent one gets the code alone, a self-review also gets the reasoning that produced it."""
    parts = ["Review this code for defects.", f"<code>{code}</code>"]
    if not independent:
        parts.append(f"<your_earlier_reasoning>{reasoning}</your_earlier_reasoning>")
    return "\n".join(parts)


def main():
    for interval in (4, 6):
        print(f"worst-case wait with a {interval} hour interval: {worst_case_wait(interval)} hours (SLA 30 hours)")
    for custom_id in ("invoice-0042", "invoice 0042"):
        try:
            batch_entry(custom_id, {"max_tokens": 1024})
            print(f"custom_id {custom_id}: accepted")
        except ValueError as e:
            print(f"custom_id {custom_id}: refused, {e}")
    for params in ({"stream": True}, {"speed": "fast"}, {"max_tokens": 0}):
        try:
            batch_entry("a1", params)
        except ValueError as e:
            print(f"refused: {e}")
    matched, orphans = match_results(["a1", "a2", "a3"], [("a2", "expired"), ("z9", "succeeded"), ("a1", "succeeded")])
    print("matched: " + ", ".join(f"{i}={kind}" for i, kind in matched) + "; unrequested: " + ", ".join(orphans))
    code, reasoning = "total = price * qty", "qty is always positive, so no check"
    for independent in (False, True):
        request = review_request(code, reasoning, independent)
        print(f"independent={'yes' if independent else 'no'}: carries the reasoning: {'yes' if reasoning in request else 'no'}")


if __name__ == "__main__":
    main()
