from state_manifest import finish, injected_state, read_manifest, recovery_plan, start, tokens


def test_the_manifest_is_written_at_the_start_and_updated_when_the_agent_finishes():
    fs = {}
    start(fs, "auth")
    assert read_manifest(fs) == {"auth": ("running", "state/auth.md")} and "state/auth.md" not in fs
    finish(fs, "auth", [("a fact", "x.py:1")])
    assert read_manifest(fs) == {"auth": ("done", "state/auth.md")} and fs["state/auth.md"] == "- a fact (x.py:1)"


def test_a_crash_before_the_state_file_is_written_means_a_restart():
    fs = {}
    start(fs, "auth")
    finish(fs, "auth", [("a fact", "x.py:1")])
    start(fs, "search")
    assert recovery_plan(fs, ["auth", "search", "never_started"]) == [("auth", "reuse"), ("search", "restart"), ("never_started", "restart")]


def test_a_state_file_without_a_finished_manifest_entry_is_resumed():
    fs = {"state/manifest.txt": "billing|running|state/billing.md", "state/billing.md": "- half done (b.py:2)"}
    assert recovery_plan(fs, ["billing"]) == [("billing", "resume")]


def test_the_injected_state_holds_only_what_need_not_run_again():
    fs = {}
    start(fs, "auth")
    finish(fs, "auth", [("a fact", "x.py:1")])
    start(fs, "search")
    assert injected_state(fs, recovery_plan(fs, ["auth", "search"])) == "auth:\n- a fact (x.py:1)"


def test_tokens_round_up_by_four_characters():
    assert [tokens(0), tokens(4), tokens(5), tokens("abcde")] == [0, 1, 2, 2]
