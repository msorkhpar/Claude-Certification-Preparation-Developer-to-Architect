"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from architecture_review import cheapest_adequate, review, verdict

STAGES = {"input": ["parse"], "processing": ["classify", "route"], "output": ["validate", "send"], "feedback": ["review a sample"]}

def design(**over):
    base = {"name": "intake", "pattern": "workflow", "agents": 1, "cost": 3, "path_known": True, "parallel_independent": False,
            "shared_context": False, "needs_audit": True, "writes_without_approval": False, "stages": STAGES}
    return {**base, **over}

# A sound workflow design passes; a multi-agent design that writes without approval and has no feedback loop does not.
sound = design()
risky = design(name="research", pattern="multi-agent", agents=4, writes_without_approval=True, stages={**STAGES, "feedback": []})
for d in (sound, risky):
    findings = review(d)
    print(d["name"], "->", verdict(findings), [f["rule"] for f in findings])
print("cheapest design that is not rejected:", cheapest_adequate([sound, risky]))
