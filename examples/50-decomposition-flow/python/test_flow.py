from flow import CHANGE, SUMMARIES, plan, run_adaptive, work


def test_the_adaptive_run_ends_when_the_planner_says_done_and_keeps_every_step():
    status, steps, summary = run_adaptive("goal")
    assert status == "done" and [s for s, _ in steps] == ["list the test files", "run the failing test", "read the module under test"]
    assert summary == "the failure is in parse()"


def test_the_step_limit_ends_a_run_that_the_planner_does_not():
    status, steps, _ = run_adaptive("goal", max_steps=2)
    assert status == "step_limit" and len(steps) == 2


def test_the_planner_choice_depends_on_the_last_result():
    assert plan("g", [("list the test files", "3 files"), ("run the failing test", "all passed")])["done"] is True
    assert plan("g", [("list the test files", "3 files"), ("run the failing test", "1 failure in test_parse")])["next"] == "read the module under test"


def test_every_file_has_a_summary_and_a_worker_answer_exists_for_each_subtask():
    assert sorted(SUMMARIES) == sorted(CHANGE)
    assert work("list the test files") == "3 files"
