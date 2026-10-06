"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from evalkit import ab_verdict, choose_model, segment_table, shadow_gate

# Results of an evaluation as (segment, correct) rows; a wrong refund costs more than a wrong order status.
COSTS = {"order status": 1, "refund": 20, "policy": 5}
results = [("order status", True)] * 30 + [("refund", True)] * 5 + [("refund", False)] * 3 + [("policy", True)] * 9 + [("policy", False)]
for line in segment_table(results, COSTS):
    print("segment:", line)

# The same cases under the old and the new prompt: a gain in one segment must not hide a loss in a protected one.
pairs = [("refund", True, False), ("policy", False, True), ("policy", False, True), ("order status", True, True)]
print("shadow gate:", shadow_gate(pairs, {"refund"}))
print("A/B verdict:", ab_verdict(100, 200, 160, 200))

# The cheapest model that is accurate and fast enough: (name, accuracy, p95 latency, cost).
print("model:", choose_model([("small", 88, 900, 1), ("medium", 94, 1500, 3), ("large", 97, 4000, 9)], 90, 2000))
