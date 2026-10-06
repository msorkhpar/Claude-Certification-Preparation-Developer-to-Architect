"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from run_audit import audit

POLICY = {"target": 90, "min_n": 3, "gap": 5}

def run(kind="typed", status="valid", correct=True, invented=False, wasted=False):
    return {"id": "d", "kind": kind, "status": status, "correct": correct, "invented": invented, "retried_absent": wasted, "sum_ok": True}

# A run of seven documents: three typed ones right, two scans, two handwritten ones that went wrong.
runs = [run() for _ in range(3)] + [run("scanned"), run("scanned", "needs_review", False),
                                    run("handwritten", "failed", False, invented=True), run("handwritten", "needs_review", False, wasted=True)]
report = audit(runs, POLICY)
print("documents:", report["n"], "| valid:", report["valid"], "| needs review:", report["needs_review"], "| failed:", report["failed"])
print("accuracy, all vs validated:", report["accuracy_all"], report["accuracy_validated"])
print("segments:", report["segments"])
print("first fix:", report["first_fix"])
