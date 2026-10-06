"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from context_builder import build_context, trim_record, update_facts, window

# A tool result is large; keep only the fields the next turn needs, with their exact values.
order = {"order_id": "A-1042", "items": "2 x kettle", "warehouse_bin": "R7-22", "refund_amount": "$129.50"}
print("trimmed record:", trim_record(order, ["refund_amount", "order_id"]))

# A newer value replaces the current one; the old one is kept as superseded.
facts = update_facts({}, "address", "12 Oak St", "2026-08-01")
facts = update_facts(facts, "address", "9 Elm Rd", "2026-09-10")
print("facts:", facts)

# The context of the next request: case facts, then the summary, then the recent messages.
case_facts = [{"customer": "c1", "name": "refund", "value": "$129.50", "as_of": "2026-09-02"}]
recent = [{"role": "user", "kind": "text", "id": "m1", "text": "Where is my refund?"}]
print(build_context("c1", case_facts, "The customer asked about a refund.", recent))

# A tool call and its result stay together when the window drops old messages.
messages = [{"role": "user", "kind": "text", "id": "m1", "text": "x" * 40},
            {"role": "assistant", "kind": "tool_use", "id": "t1", "text": "lookup"},
            {"role": "user", "kind": "tool_result", "id": "t1", "text": "found"}]
print("window ids:", [m["id"] for m in window(messages, 6)])
