"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import json

from batches import BatchError, build_requests, collect

MODEL = "claude-haiku-4-5-20251001"


def item(id):
    return {"id": id, "params": {"model": MODEL, "max_tokens": 200,
                                 "messages": [{"role": "user", "content": f"Classify ticket {id}"}]}}


def result_line(custom_id, text):
    """One line of the .jsonl results file, like the ones the tests build."""
    message = {"id": "msg_illustrative", "type": "message", "role": "assistant", "model": MODEL, "stop_reason": "end_turn",
               "content": [{"type": "text", "text": text}], "usage": {"input_tokens": 50, "output_tokens": 7}}
    return json.dumps({"custom_id": custom_id, "result": {"type": "succeeded", "message": message}})


try:
    requests = build_requests([item("t-1"), item("t-2"), item("t-3")]) or []
    print("custom ids:", [r["custom_id"] for r in requests])
    # The results come back in any order: they are matched by custom_id, not by position.
    done = collect(requests, [result_line("t-3", "billing"), result_line("t-1", "refund"), result_line("t-2", "shipping")]) or {}
    print("outcomes:", [(o["custom_id"], o["status"], o.get("text")) for o in done.get("outcomes", [])])
    print("to retry / fix / unknown:", done.get("retry"), done.get("fix"), done.get("unknown"))
except BatchError as err:
    print("batch error:", err)
