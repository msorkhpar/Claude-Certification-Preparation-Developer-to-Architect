"""A Message Batch from submission to results, against a scripted server.

The replies are illustrative, hand-written bodies in the shapes of the batch processing page (claude-haiku-4-5), not
captures. The results arrive out of order, as the page warns they may, and one request of each non-success kind is in
them. Waiting between polls is recorded, not slept.
"""
import json

import httpx2

from harness import scripted_client
from harness.scripted import message, text

MODEL = "claude-haiku-4-5-20251001"
TICKETS = {"t-1": "My parcel never arrived.", "t-2": "How do I change my address?", "t-3": "Charge me twice? Refund please.", "t-4": "x" * 10}


def batch(status, counts, results_url=None):
    return {"id": "msgbatch_illustrative", "type": "message_batch", "processing_status": status, "request_counts": counts,
            "ended_at": None if status != "ended" else "2026-10-02T10:40:00Z", "created_at": "2026-10-02T10:00:00Z",
            "expires_at": "2026-10-03T10:00:00Z", "cancel_initiated_at": None, "results_url": results_url}


def counts(processing=0, succeeded=0, errored=0, canceled=0, expired=0):
    return {"processing": processing, "succeeded": succeeded, "errored": errored, "canceled": canceled, "expired": expired}


def result_line(custom_id, result):
    return json.dumps({"custom_id": custom_id, "result": result})


def succeeded(label):
    return {"type": "succeeded", **{"message": message([text(label)], usage={"input_tokens": 30, "output_tokens": 3})}}


RESULTS = "\n".join([
    result_line("t-3", succeeded("billing")),
    result_line("t-1", succeeded("shipping")),
    result_line("t-4", {"type": "errored", "error": {"type": "error", "error": {"type": "invalid_request_error", "message": "messages: at least one message is required"}}}),
    result_line("t-2", {"type": "expired"}),
]) + "\n"

SCRIPT = [
    (200, batch("in_progress", counts(processing=4))),
    (200, batch("in_progress", counts(processing=2, succeeded=2))),
    (200, batch("ended", counts(succeeded=2, errored=1, expired=1), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results")),
    (200, batch("ended", counts(succeeded=2, errored=1, expired=1), "https://api.anthropic.com/v1/messages/batches/msgbatch_illustrative/results")),  # results() looks the batch up first
    httpx2.Response(200, headers={"content-type": "application/x-jsonl"}, content=RESULTS.encode()),
]


def main():
    client, transport = scripted_client(*SCRIPT)
    requests = [{"custom_id": cid, "params": {"model": MODEL, "max_tokens": 50, "messages": [{"role": "user", "content": f"Label this ticket: {body}"}]}}
                for cid, body in TICKETS.items()]
    created = client.messages.batches.create(requests=requests)
    print("created:", created.id, created.processing_status, "| request ids sent:", [r["custom_id"] for r in transport.requests[0]["requests"]])
    waits = []
    status = created
    while status.processing_status != "ended":
        status = client.messages.batches.retrieve(created.id)
        waits.append(60)
        print("poll:", status.processing_status, status.request_counts.model_dump())
    print("waited between polls (recorded, not slept):", waits, "seconds")
    outcomes = {}
    for item in client.messages.batches.results(created.id):
        kind = item.result.type
        detail = item.result.message.content[0].text if kind == "succeeded" else (item.result.error.error.type if kind == "errored" else "")
        outcomes[item.custom_id] = (kind, detail)
        print(f"result: {item.custom_id} {kind} {detail}")
    print("in request order:", [(cid, outcomes[cid][0]) for cid in TICKETS])
    fix = [cid for cid, (kind, detail) in outcomes.items() if kind == "errored" and detail == "invalid_request_error"]
    retry = [cid for cid, (kind, _) in outcomes.items() if kind in ("expired", "canceled") or (kind == "errored" and cid not in fix)]
    print("fix before resubmitting:", fix, "| resubmit unchanged:", retry)


if __name__ == "__main__":
    main()
