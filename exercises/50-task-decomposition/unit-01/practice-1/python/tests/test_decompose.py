import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from decompose import choose_strategy, review_changes, run_adaptive

FILES = [{"path": "api.py", "text": "def get(): ..."}, {"path": "db.py", "text": "def query(): ..."}, {"path": "ui.py", "text": "def show(): ..."}]


class FilePass:
    """A scripted file pass: findings and summary by path; every call is kept. A path scripted as an exception raises it."""

    def __init__(self, **scripted):
        self.scripted, self.calls = scripted, []

    def __call__(self, path, text, part, parts):
        self.calls.append((path, text, part, parts))
        script = self.scripted.get(path.split(".")[0], {"findings": [], "summary": f"{path} part {part}"})
        if isinstance(script, Exception):
            raise script
        return script


class CrossPass:
    def __init__(self, findings=None, error=None):
        self.findings, self.error, self.seen = findings or [], error, []

    def __call__(self, summaries):
        self.seen.append(summaries)
        if self.error:
            raise self.error
        return self.findings


def review(files=FILES, file_pass=None, cross_pass=None, **extra):
    result = review_changes(files, file_pass or FilePass(), cross_pass or CrossPass(), **extra)
    assert result is not None, "review_changes returned None"
    return result


def test_m1_each_file_is_reviewed_alone_and_the_cross_pass_reads_their_summaries():
    file_pass = FilePass(api={"findings": ["api: no auth"], "summary": "api calls db.query(id)"}, db={"findings": [], "summary": "db.query takes a name"},
                         ui={"findings": ["ui: unused"], "summary": "ui shows rows"})
    cross = CrossPass(["api passes id but db expects a name"])
    result = review(file_pass=file_pass, cross_pass=cross)
    assert [c[0] for c in file_pass.calls] == ["api.py", "db.py", "ui.py"]
    assert result["files"]["api.py"] == {"findings": ["api: no auth"], "summary": "api calls db.query(id)", "parts": 1}
    assert result["files"]["ui.py"]["findings"] == ["ui: unused"]
    assert result["cross"] == ["api passes id but db expects a name"]
    assert cross.seen == [[{"path": "api.py", "summary": "api calls db.query(id)"}, {"path": "db.py", "summary": "db.query takes a name"},
                           {"path": "ui.py", "summary": "ui shows rows"}]]
    assert result["failed"] == {} and result["skipped"] == [] and result["cross_error"] is None


def test_e1_a_file_pass_sees_only_its_own_file_and_the_cross_pass_sees_summaries_never_the_text():
    file_pass, cross = FilePass(), CrossPass()
    review(file_pass=file_pass, cross_pass=cross)
    assert file_pass.calls == [("api.py", "def get(): ...", 1, 1), ("db.py", "def query(): ...", 1, 1), ("ui.py", "def show(): ...", 1, 1)]
    seen = cross.seen[0] if cross.seen else []
    assert len(seen) == 3 and all(sorted(item) == ["path", "summary"] for item in seen)
    assert "def " not in repr(seen)


def test_e2_a_long_file_is_reviewed_in_labelled_parts_and_a_blank_file_is_skipped():
    long_text = "\n".join(f"line {n}" for n in range(1, 8))
    files = [{"path": "big.py", "text": long_text}, {"path": "empty.py", "text": "  \n \n"}, {"path": "small.py", "text": "x = 1"}]
    file_pass = FilePass()
    result = review(files, file_pass, max_lines=3)
    assert file_pass.calls == [("big.py", "line 1\nline 2\nline 3", 1, 3), ("big.py", "line 4\nline 5\nline 6", 2, 3), ("big.py", "line 7", 3, 3), ("small.py", "x = 1", 1, 1)]
    assert result["files"]["big.py"]["parts"] == 3 and result["files"]["big.py"]["summary"] == "big.py part 1 big.py part 2 big.py part 3"
    assert result["skipped"] == ["empty.py"] and "empty.py" not in result["files"]
    exact = review([{"path": "a.py", "text": "1\n2\n3"}], max_lines=3)
    assert exact["files"]["a.py"]["parts"] == 1


def test_e3_a_failing_file_is_reported_and_left_out_of_the_cross_pass_which_needs_two_files():
    file_pass = FilePass(db=RuntimeError("model timed out"), api={"findings": ["f1"], "summary": "api summary"}, ui={"findings": ["f2"], "summary": "ui summary"})
    cross = CrossPass(["relation"])
    result = review(file_pass=file_pass, cross_pass=cross)
    assert result["failed"] == {"db.py": "model timed out"} and sorted(result["files"]) == ["api.py", "ui.py"]
    assert cross.seen == [[{"path": "api.py", "summary": "api summary"}, {"path": "ui.py", "summary": "ui summary"}]]
    alone, lone_cross = FilePass(db=RuntimeError("boom"), ui=RuntimeError("boom")), CrossPass(["never"])
    one = review(file_pass=alone, cross_pass=lone_cross)
    assert sorted(one["files"]) == ["api.py"] and lone_cross.seen == [] and one["cross"] == [] and sorted(one["failed"]) == ["db.py", "ui.py"]
    broken = review(cross_pass=CrossPass(error=RuntimeError("cross failed")))
    assert broken["cross"] == [] and broken["cross_error"] == "cross failed" and len(broken["files"]) == 3


class Planner:
    """A scripted planner: the replies in order; every call is kept with the steps it was given."""

    def __init__(self, *replies):
        self.replies, self.calls = list(replies), []

    def __call__(self, goal, steps):
        self.calls.append((goal, steps))
        return self.replies[min(len(self.calls), len(self.replies)) - 1]


def adapt(planner, worker=None, goal="map the module", **extra):
    seen = []
    result = run_adaptive(planner, worker or (lambda subtask: seen.append(subtask) or f"did {subtask}"), goal, **extra)
    assert result is not None, "run_adaptive returned None"
    return result, seen


def test_e4_the_planner_is_asked_again_after_each_step_with_the_steps_so_far_and_the_loop_ends_when_it_says_done():
    planner = Planner({"done": False, "next": "list the files"}, {"done": False, "next": "read the entry point"}, {"done": True, "summary": "two modules, one entry point"})
    result, ran = adapt(planner)
    assert ran == ["list the files", "read the entry point"]
    assert result["status"] == "done" and result["summary"] == "two modules, one entry point"
    assert result["steps"] == [{"subtask": "list the files", "result": "did list the files"}, {"subtask": "read the entry point", "result": "did read the entry point"}]
    assert [len(steps) for _, steps in planner.calls] == [0, 1, 2] and planner.calls[0][0] == "map the module"
    assert planner.calls[2][1][1] == {"subtask": "read the entry point", "result": "did read the entry point"}
    failing = Planner({"done": False, "next": "open the file"}, {"done": True, "summary": "saw the error"})
    result, _ = adapt(failing, lambda subtask: (_ for _ in ()).throw(RuntimeError("no such file")))
    assert result["steps"] == [{"subtask": "open the file", "result": "ERROR: no such file"}] and failing.calls[1][1][0]["result"] == "ERROR: no such file"


def test_e5_the_loop_stops_on_a_repeated_subtask_or_no_next_step_or_an_unreadable_reply_and_counts_the_step_limit_exactly():
    repeated, ran = adapt(Planner({"done": False, "next": "list the files"}, {"done": False, "next": "  List The Files "}))
    assert repeated["status"] == "stuck" and ran == ["list the files"] and "list the files" in repeated["reason"].lower()
    blank, _ = adapt(Planner({"done": False, "next": "   "}))
    assert blank["status"] == "stuck" and blank["steps"] == []
    for reply in ("not a plan", {"next": "x"}, {"done": "yes"}, None):
        bad, ran = adapt(Planner(reply))
        assert bad["status"] == "bad_plan" and ran == [], f"{reply!r} must be a bad plan"
    endless = Planner(*[{"done": False, "next": f"step {n}"} for n in range(1, 10)])
    limited, ran = adapt(endless, max_steps=3)
    assert limited["status"] == "step_limit" and ran == ["step 1", "step 2", "step 3"] and len(endless.calls) == 4
    last = Planner({"done": False, "next": "one"}, {"done": False, "next": "two"}, {"done": True, "summary": "finished on the last step"})
    result, ran = adapt(last, max_steps=2)
    assert result["status"] == "done" and ran == ["one", "two"]


def test_e6_the_strategy_follows_what_is_known_about_the_steps_and_whether_the_items_interact():
    assert choose_strategy({"steps_known": True, "items": 1, "items_interact": False}) == "fixed_chain"
    assert choose_strategy({"steps_known": True, "items": 12, "items_interact": False}) == "fixed_chain"
    assert choose_strategy({"steps_known": True, "items": 12, "items_interact": True}) == "per_item_then_cross"
    assert choose_strategy({"steps_known": True, "items": 1, "items_interact": True}) == "fixed_chain"
    assert choose_strategy({"steps_known": False, "items": 12, "items_interact": True}) == "adaptive"
    assert choose_strategy({"steps_known": False, "items": 0}) == "adaptive"
    assert choose_strategy({"steps_known": True, "items": 3}) == "fixed_chain"
    bad = 0
    for task in ({"items": 3}, {"steps_known": True}, {"steps_known": True, "items": -1}, {"steps_known": "yes", "items": 2}, {"steps_known": True, "items": 2.5}):
        try:
            choose_strategy(task)
        except ValueError:
            bad += 1
    assert bad == 5
