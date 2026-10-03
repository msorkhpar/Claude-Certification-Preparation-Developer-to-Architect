from session_state import decide, flags, notice

SAVED = {"a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4"}


def test_an_unchanged_session_is_resumed_and_a_changed_one_gets_a_notice():
    assert decide(SAVED, SAVED, 1) == ("resume", [])
    assert decide(SAVED, {**SAVED, "b.py": "x"}, 1) == ("resume with a notice", ["b.py"])
    assert decide(SAVED, {k: v for k, v in SAVED.items() if k != "c.py"}, 1) == ("resume with a notice", [])


def test_most_files_changed_or_a_week_idle_starts_fresh():
    assert decide(SAVED, {"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4"}, 1)[0] == "start fresh with a summary"
    assert decide(SAVED, {**SAVED, "a.py": "x", "b.py": "x"}, 1)[0] == "resume with a notice"
    assert decide(SAVED, SAVED, 8)[0] == "start fresh with a summary"
    assert decide(SAVED, SAVED, 7)[0] == "resume"


def test_the_notice_names_the_changed_files_and_asks_for_a_new_read():
    text = notice(["a.py", "b.py"])
    assert text.splitlines()[1] == "- changed: a.py, b.py" and "Re-read these files" in text


def test_flags_lists_resume_with_its_id_fork_and_continue_in_order():
    assert flags(["--output-format", "stream-json"]) == "none"
    assert flags(["--resume", "s1", "--fork-session", "--verbose"]) == "--resume s1, --fork-session"
    assert flags(["--continue"]) == "--continue"
    assert flags(["--resume=s1", "--fork-session"]) == "--resume s1, --fork-session"
