import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
import recovery as _solution


def _got(fn):
    def call(*args, **kwargs):
        value = fn(*args, **kwargs)
        assert value is not None, f"{fn.__name__} returned nothing"
        return value

    return call


add_finding, render_scratchpad, build_manifest, resume_plan, resume_prompt, compact_command = (
    _got(getattr(_solution, n)) for n in ("add_finding", "render_scratchpad", "build_manifest", "resume_plan", "resume_prompt", "compact_command"))


def refused(fn, *args):
    try:
        fn(*args)
    except ValueError:
        return True
    return False


def agent(name, status="done", state_file=None):
    return {"name": name, "state_file": state_file or f"state/{name}.md", "status": status}


def test_m1_a_finding_is_recorded_once_per_area_and_fact_in_first_seen_order():
    found = add_finding([], "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12")
    found = add_finding(found, "billing", "invoices use cents", "billing/Money.java:5")
    again = add_finding(found, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99")
    assert again == found and [f["area"] for f in again] == ["auth", "billing"]
    assert len(add_finding(found, "billing", "a different fact", "x:1")) == 3
    assert len(add_finding(found, "auth", "invoices use cents", "x:1")) == 3 and len(found) == 2


def test_e1_the_scratchpad_groups_findings_under_their_area():
    found = add_finding([], "auth", "f1", "a:1")
    found = add_finding(found, "billing", "f2", "b:2")
    found = add_finding(found, "auth", "f3", "a:3")
    assert render_scratchpad(found) == "## auth\n- f1 (a:1)\n- f3 (a:3)\n\n## billing\n- f2 (b:2)"
    assert render_scratchpad([]) == ""


def test_e2_the_manifest_lists_every_agent_with_its_state_file_and_status_and_refuses_bad_input():
    manifest = build_manifest([agent("search", "running"), agent("auth"), agent("billing", "failed")])
    assert manifest == {"version": 1, "agents": [
        {"name": "auth", "state_file": "state/auth.md", "status": "done"},
        {"name": "billing", "state_file": "state/billing.md", "status": "failed"},
        {"name": "search", "state_file": "state/search.md", "status": "running"}]}
    assert refused(build_manifest, [agent("auth"), agent("auth", "running")])
    assert refused(build_manifest, [agent("auth", "paused")])


def test_e3_a_finished_agent_with_its_state_file_is_reused_and_not_run_again():
    manifest = build_manifest([agent("auth", "done")])
    assert resume_plan(manifest, {"state/auth.md"}) == [("auth", "reuse")]


def test_e4_a_running_or_failed_agent_with_a_state_file_is_resumed_from_it():
    manifest = build_manifest([agent("billing", "failed"), agent("search", "running")])
    assert resume_plan(manifest, {"state/billing.md", "state/search.md"}) == [("billing", "resume"), ("search", "resume")]


def test_e5_an_agent_whose_state_file_is_missing_is_restarted_from_scratch():
    manifest = build_manifest([agent("auth", "done"), agent("search", "running")])
    assert resume_plan(manifest, {"state/auth.md"}) == [("auth", "reuse"), ("search", "restart")]
    assert resume_plan(manifest, set()) == [("auth", "restart"), ("search", "restart")]


def test_e6_the_resume_prompt_carries_the_task_and_the_state_lines_and_nothing_else():
    assert resume_prompt("Map the search module.", ["indexer.py done", "ranker.py not started"]) == (
        "Map the search module.\n\nState from the last run:\n- indexer.py done\n- ranker.py not started\nContinue from the first unfinished step.")
    assert resume_prompt("Map the search module.", []) == "Map the search module."


def test_e7_the_compact_command_names_what_to_keep():
    assert compact_command(["the list of files read", "open questions"]) == "/compact Focus on the list of files read, open questions"
    assert compact_command([]) == "/compact"
