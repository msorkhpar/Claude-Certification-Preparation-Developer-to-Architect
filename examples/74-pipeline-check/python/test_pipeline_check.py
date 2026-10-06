import json
import tempfile
from pathlib import Path

from pipeline_check import HERE, audit, load


def pipeline(*jobs):
    root = Path(tempfile.mkdtemp())
    (root / "ci").mkdir()
    (root / "ci/pipeline.json").write_text(json.dumps({"jobs": list(jobs)}))
    return root


def job(**changes):
    base = {"name": "j", "kind": "report", "audience": "scheduled", "api": "batch", "passes": [], "session": "fresh", "context": [], "tools": [], "command": "python ci/run.py"}
    return {**base, **changes}


def test_the_flawed_pipeline_has_every_finding_and_the_fixed_one_has_none():
    assert audit(HERE / "project-before") == ["blocking-batch: pre-merge-review", "single-pass-review: pre-merge-review", "shared-session: pre-merge-review",
                                              "no-prior-findings: pre-merge-review", "writes: pre-merge-review", "batchable: debt-report",
                                              "no-print-flag: test-generation", "no-existing-tests: test-generation"]
    assert audit(HERE / "project-after") == []


def test_who_waits_decides_between_batch_and_real_time():
    assert audit(pipeline(job(audience="waiting", api="batch"))) == ["blocking-batch: j"]
    assert audit(pipeline(job(audience="scheduled", api="realtime", command="claude -p x"))) == ["batchable: j"]
    assert audit(pipeline(job(audience="waiting", api="realtime", command="claude -p x"))) == []


def test_a_command_is_headless_with_either_spelling_and_a_script_needs_no_flag():
    assert audit(pipeline(job(api="realtime", audience="waiting", command='claude "x"'))) == ["no-print-flag: j"]
    assert audit(pipeline(job(api="realtime", audience="waiting", command='claude --print "x"'))) == []
    assert audit(pipeline(job())) == []


def test_a_review_needs_a_pass_per_file_and_then_an_integration_pass_in_that_order():
    review = dict(kind="review", audience="waiting", api="realtime", command="claude -p x", context=["prior_findings"])
    assert audit(pipeline(job(passes=["per-file", "integration"], **review))) == []
    assert audit(pipeline(job(passes=["integration", "per-file"], **review))) == ["single-pass-review: j"]
    assert audit(pipeline(job(passes=["per-file"], **review))) == ["single-pass-review: j"]


def test_only_a_review_is_held_to_read_only_tools():
    assert audit(pipeline(job(kind="testgen", audience="waiting", api="realtime", command="claude -p x", context=["existing_tests"], tools=["Edit"]))) == []
    assert load(HERE / "project-after")[2]["tools"] == ["Read", "Glob", "Edit"]
