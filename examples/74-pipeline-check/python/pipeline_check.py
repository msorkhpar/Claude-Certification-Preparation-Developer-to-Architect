"""Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.

The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
Claude.
"""
import logging
import json
import shlex
from pathlib import Path

log = logging.getLogger(__name__)

HERE = Path(__file__).resolve().parent.parent
WRITERS = ("Bash", "Edit", "Write")


def load(root):
    return json.loads((root / "ci/pipeline.json").read_text())["jobs"]


def audit(root):
    found = []
    for job in load(root):
        name, kind = job["name"], job["kind"]
        tokens = shlex.split(job["command"])
        if tokens[0] == "claude" and "-p" not in tokens and "--print" not in tokens:
            found.append(f"no-print-flag: {name}")
        if job["audience"] == "waiting" and job["api"] == "batch":
            found.append(f"blocking-batch: {name}")
        if job["audience"] == "scheduled" and job["api"] == "realtime":
            found.append(f"batchable: {name}")
        if kind == "review" and job["passes"] != ["per-file", "integration"]:
            found.append(f"single-pass-review: {name}")
        if kind == "review" and job["session"] != "fresh":
            found.append(f"shared-session: {name}")
        if kind == "review" and "prior_findings" not in job["context"]:
            found.append(f"no-prior-findings: {name}")
        if kind == "testgen" and "existing_tests" not in job["context"]:
            found.append(f"no-existing-tests: {name}")
        if kind == "review" and any(t in WRITERS for t in job["tools"]):
            found.append(f"writes: {name}")
    return found


def main():
    for name in ("project-before", "project-after"):
        jobs = load(HERE / name)
        batch = sum(1 for j in jobs if j["api"] == "batch")
        print(f"{name}: {len(jobs)} jobs ({len(jobs) - batch} real-time, {batch} batch)")
        found = audit(HERE / name)
        for finding in found:
            print(f"  finding: {finding}")
        if not found:
            print("  no findings")
            for j in jobs:
                print(f"  {j['name']}: {j['api']}, passes {'+'.join(j['passes']) or 'none'}")


if __name__ == "__main__":
    main()
