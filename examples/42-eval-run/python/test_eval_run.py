from eval_run import CASES, CRITERIA, PROMPT_V1, PROMPT_V2, compare, gate, grade, run


def test_exact_grading_ignores_case_and_spacing_only():
    case = {"expect": "neutral"}
    assert grade(case, " Neutral\n") and not grade(case, "neutral.") and not grade(case, "positive")


def test_the_first_prompt_passes_four_of_six_and_fails_the_gate_twice():
    report = run(CASES, PROMPT_V1)
    assert [r["id"] for r in report["results"] if not r["passed"]] == ["sarcasm-1", "mixed-1"]
    assert round(report["pass_rate"], 3) == 0.667 and report["by_tag"]["edge"] == [1, 3]
    assert gate(report, CRITERIA) == ["overall", "tag:edge"]


def test_the_second_prompt_has_a_better_average_and_one_regression():
    v1, v2 = run(CASES, PROMPT_V1), run(CASES, PROMPT_V2)
    assert v2["pass_rate"] > v1["pass_rate"]
    assert compare(v1, v2) == {"regressions": ["empty-1"], "fixed": ["sarcasm-1", "mixed-1"]}
    assert gate(v2, CRITERIA) == ["tag:edge"]


def test_a_run_that_changes_nothing_has_no_regressions():
    v1 = run(CASES, PROMPT_V1)
    assert compare(v1, v1) == {"regressions": [], "fixed": []}
