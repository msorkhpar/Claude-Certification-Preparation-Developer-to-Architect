from builtin_tools import covered_by, edit, plan_edit, rule_tool, tool_set

TEXT = "a = 1\nb = 1\n"


def test_edit_is_an_exact_replacement_that_needs_one_match():
    assert edit(TEXT, "a = 1", "a = 2") == {"ok": True, "text": "a = 2\nb = 1\n", "replaced": 1}
    assert edit(TEXT, "= 1", "= 2") == {"ok": False, "error": "old_string appears 2 times"}
    assert edit(TEXT, "z", "y") == {"ok": False, "error": "old_string not found"}
    assert edit(TEXT, "a = .", "x")["ok"] is False  # no regex


def test_replace_all_changes_every_match():
    assert edit(TEXT, "= 1", "= 2", replace_all=True) == {"ok": True, "text": "a = 2\nb = 2\n", "replaced": 2}


def test_the_plan_widens_the_anchor_before_it_rewrites_the_file():
    assert plan_edit(TEXT, "a = 1") == ("edit", "a = 1")
    assert plan_edit(TEXT, "= 1", anchors=["b = 1"]) == ("edit", "b = 1")
    assert plan_edit(TEXT, "= 1", every=True) == ("replace_all", "= 1")
    assert plan_edit(TEXT, "= 1", anchors=["= 1"]) == ("read_write", None)
    assert plan_edit(TEXT, "z") == ("read_again", None)


def test_search_tools_are_default_on_windows_only_and_named_back_elsewhere():
    assert tool_set("windows") == ["Read", "Write", "Edit", "Bash", "Grep", "Glob"]
    assert tool_set("linux") == ["Read", "Write", "Edit", "Bash"]
    assert tool_set("linux", allowed_tools=["Glob"]) == ["Read", "Write", "Edit", "Bash", "Grep", "Glob"]
    assert tool_set("macos", tools=["Read", "Grep"]) == ["Read", "Grep"]
    assert tool_set("wsl", disallowed_tools=["Bash"]) == ["Read", "Write", "Edit", "Grep", "Glob"]


def test_a_rule_covers_the_tools_its_name_implies():
    assert covered_by("Read(x)") == ["Read", "Grep", "Glob"] and covered_by("Edit(x)") == ["Edit", "Write"]
    assert covered_by("Write(x)") == [] and covered_by("Bash(ls *)") == ["Bash"]


def test_a_call_is_checked_under_the_rule_name_of_its_family():
    assert [rule_tool(t) for t in ("Read", "Grep", "Glob", "Edit", "Write", "Bash")] == ["Read", "Read", "Read", "Edit", "Edit", "Bash"]
