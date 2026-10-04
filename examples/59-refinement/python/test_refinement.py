from refinement import choose_mode, failure_report, group_feedback


def task(**over):
    return {"diff_in_one_sentence": False, "files": 1, "architectural": False, "approaches": 1, **over}


def test_a_change_you_can_say_in_one_sentence_is_done_directly():
    assert choose_mode(task(diff_in_one_sentence=True)) == ["implement"]


def test_large_architectural_or_ambiguous_changes_are_planned_first():
    assert choose_mode(task(architectural=True)) == ["explore", "plan", "implement"]
    assert choose_mode(task(files=45)) == ["explore", "plan", "implement"]
    assert choose_mode(task(approaches=2)) == ["explore", "plan", "implement"]
    assert choose_mode(task(diff_in_one_sentence=True, architectural=True)) == ["explore", "plan", "implement"]


def test_interacting_problems_travel_together_and_independent_ones_go_one_at_a_time():
    issues = [{"id": "a", "interacts_with": ["b"]}, {"id": "b", "interacts_with": []}, {"id": "c", "interacts_with": []}, {"id": "d", "interacts_with": ["c"]}, {"id": "e"}]
    assert group_feedback(issues) == [["a", "b"], ["c", "d"], ["e"]]
    assert group_feedback([]) == []


def test_a_failure_report_names_each_failing_test_with_input_and_expected_output():
    results = [{"name": "ok", "input": 1, "expected": 2, "actual": 2}, {"name": "empty", "input": [], "expected": [], "actual": None}]
    assert failure_report(results) == "1 of 2 tests fail:\n- empty: input [], expected [], got None"
    assert failure_report(results[:1]) == "All tests pass."
