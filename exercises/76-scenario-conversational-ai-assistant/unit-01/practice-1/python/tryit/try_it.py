"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from conversation_review import review

POLICY = {"max_turns": 12, "max_repeat": 10, "min_resolved": 80, "min_n": 3}

def conv(segment="billing", turns=5, resolved=True, handoff="none", needed=False, repeated=False, risk=False):
    return {"id": "c", "segment": segment, "turns": turns, "resolved": resolved, "handoff": handoff,
            "needed_person": needed, "repeated": repeated, "risk": risk}

# A batch of conversations: billing mostly resolved, a safety case handed off, one overlong smalltalk.
batch = [conv() for _ in range(3)] + [conv(resolved=False, handoff="requested", needed=True), conv("smalltalk"),
                                       conv("smalltalk", turns=15), conv("safety", resolved=False, handoff="safety", needed=True, risk=True),
                                       conv(repeated=True)]
report = review(batch, POLICY)
print("conversations:", report["n"], "| resolved:", report["resolved"], f"({report['resolved_pct']}%)")
print("safety missed:", report["safety_missed"], "| overlong:", report["overlong"], "| repeat %:", report["repeat_pct"])
print("segments:", report["segments"])
print("verdict:", report["verdict"], "-", report["reason"])
