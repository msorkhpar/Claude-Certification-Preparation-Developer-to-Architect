import asyncio
import shutil
import json
import os
import sys
import tempfile
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from sessions import build_summary, change_notice, continue_options, first_prompt, plan_session, resolve_name, run_session, session_options

HERE = Path(__file__).resolve()
_found = next(str(p) for p in (Path("/w/harness/fake_claude.py"), *(HERE.parents[i] / "harness" / "fake_claude.py" for i in range(min(len(HERE.parents), 8)))) if p.exists())
# The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
FAKE = shutil.copy(_found, Path(tempfile.mkdtemp(), "fake_claude.py"))
os.chmod(FAKE, 0o755)

NOW = 1_800_000_000
DAY = 24 * 3600
FILES = {"a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4"}
RECORD = {"id": "s-base", "name": "auth-review", "last_used": NOW - 3600, "files": dict(FILES)}


def plan(record=RECORD, current=None, now=NOW, fork=False):
    result = plan_session(record, dict(FILES) if current is None else current, now, fork=fork)
    assert result is not None, "plan_session returned None"
    return result


def test_m1_the_plan_resumes_an_unchanged_session_and_tells_a_changed_one_what_differs_and_starts_fresh_when_most_of_it_changed():
    same = plan()
    assert (same["action"], same["session_id"], same["changed"]) == ("resume", "s-base", [])
    one = plan(current={**FILES, "b.py": "x"})
    assert (one["action"], one["session_id"], one["changed"]) == ("resume_with_notice", "s-base", ["b.py"])
    most = plan(current={"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4"})
    assert (most["action"], most["session_id"]) == ("fresh_with_summary", None)


def test_e1_changed_deleted_and_added_files_are_listed_in_order_and_any_difference_asks_for_a_notice():
    current = {"d.py": "d4", "c.py": "d3", "a.py": "new", "f.py": "n1", "e.py": "n2"}
    p = plan(current=current)
    assert (p["changed"], p["deleted"], p["added"]) == (["a.py"], ["b.py"], ["e.py", "f.py"])
    only_new = plan(current={**FILES, "z.py": "n"})
    assert (only_new["action"], only_new["session_id"], only_new["added"]) == ("resume_with_notice", "s-base", ["z.py"])


def test_e2_more_than_half_of_the_files_changed_or_gone_starts_fresh_and_exactly_half_does_not():
    half = plan(current={**FILES, "a.py": "x", "b.py": "x"})
    assert half["action"] == "resume_with_notice"
    mixed_half = plan(current={"a.py": "x", "c.py": "d3", "d.py": "d4"})
    assert mixed_half["action"] == "resume_with_notice"
    over = plan(current={"a.py": "x", "b.py": "x", "d.py": "d4"})
    assert (over["action"], over["session_id"]) == ("fresh_with_summary", None)
    added_only = plan(current={**FILES, "a.py": "x", **{f"n{i}.py": "n" for i in range(5)}})
    assert added_only["action"] == "resume_with_notice"


def test_e3_a_session_idle_for_more_than_a_week_starts_fresh_and_exactly_a_week_is_resumed():
    week = plan(now=RECORD["last_used"] + 7 * DAY)
    assert (week["action"], week["session_id"]) == ("resume", "s-base")
    older = plan(now=RECORD["last_used"] + 7 * DAY + 1)
    assert (older["action"], older["session_id"]) == ("fresh_with_summary", None)


def test_e4_no_saved_session_starts_fresh_and_a_fork_is_only_planned_from_a_session_that_is_resumed():
    none = plan_session(None, dict(FILES), NOW, fork=True)
    assert none is not None and (none["action"], none["session_id"], none["fork"]) == ("fresh", None, False)
    assert plan(fork=True)["fork"] is True
    assert plan(current={**FILES, "a.py": "x"}, fork=True)["fork"] is True
    stale = plan(current={"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "x"}, fork=True)
    assert stale["fork"] is False
    assert plan()["fork"] is False


def test_e5_the_change_notice_names_only_what_differs_and_the_prompt_puts_the_notice_or_the_summary_before_the_task():
    p = plan(current={"d.py": "d4", "c.py": "d3", "a.py": "new", "f.py": "n1", "e.py": "n2"})
    assert change_notice(p) == ("Since your earlier analysis:\n- changed: a.py\n- deleted: b.py\n- new: e.py, f.py\n"
                                "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged.")
    assert change_notice(plan()) == ""
    assert change_notice(plan(current={**FILES, "z.py": "n"})).splitlines()[1:2] == ["- new: z.py"]
    assert first_prompt(p, "Continue the review.") == change_notice(p) + "\n\nContinue the review."
    fresh = plan(current={"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "x"})
    assert first_prompt(fresh, "Continue the review.", "## Findings\n- none") == "## Findings\n- none\n\nContinue the review."
    assert first_prompt(plan(), "Continue the review.", "ignored") == "Continue the review."
    assert first_prompt(plan_session(None, {}, NOW), "Start.", "ignored") == "Start."


def test_e6_the_summary_has_a_fixed_layout_with_blank_and_repeated_items_dropped_and_files_listed_by_path():
    text = build_summary(["Auth uses JWT", "  ", "Auth uses JWT", " Tokens last 1h "], ["Keep sessions"], [], {"b.py": "d2", "a.py": "d1"})
    assert text == ("## Findings\n- Auth uses JWT\n- Tokens last 1h\n\n## Decisions\n- Keep sessions\n\n## Open questions\n- none\n\n"
                    "## Files\n- a.py (d1)\n- b.py (d2)")
    assert build_summary([], [], ["Is the cache shared?"], {}) == ("## Findings\n- none\n\n## Decisions\n- none\n\n## Open questions\n- Is the cache shared?\n\n## Files\n- none")


def test_e7_options_resume_by_id_and_fork_together_and_continue_is_refused_unless_one_session_exists_and_a_name_must_be_unique():
    assert session_options(plan()) == {"resume": "s-base"}
    assert session_options(plan(fork=True)) == {"resume": "s-base", "fork_session": True}
    fresh = plan_session(None, {}, NOW)
    assert session_options(fresh, max_turns=5) == {"max_turns": 5}
    assert session_options({"action": "fresh_with_summary", "session_id": None, "fork": True}) == {}
    assert continue_options([{"id": "s1"}], max_turns=3) == {"max_turns": 3, "continue_conversation": True}
    for sessions in ([], [{"id": "s1"}, {"id": "s2"}]):
        try:
            continue_options(sessions)
        except ValueError:
            continue
        raise AssertionError("continue_options accepted a directory with %d sessions" % len(sessions))
    index = [{"id": "s1", "name": "auth-review"}, {"id": "s2", "name": "billing"}, {"id": "s3", "name": "billing"}]
    assert resolve_name("auth-review", index) == "s1"
    for name in ("billing", "Auth-Review", "missing"):
        try:
            resolve_name(name, index)
        except ValueError:
            continue
        raise AssertionError(f"resolve_name accepted {name}")


def resumed_id(argv):
    """The id after --resume, written as `--resume=<id>` by the Python SDK or as two words by others."""
    for i, arg in enumerate(argv):
        if arg == "--resume":
            return argv[i + 1]
        if arg.startswith("--resume="):
            return arg.split("=", 1)[1]
    return None


def _run(session_id, result, options, prompt="Continue", folder=None):
    folder = folder or tempfile.mkdtemp()
    script, record = Path(folder, "script.json"), Path(folder, "record.jsonl")
    script.write_text(json.dumps({"session_id": session_id, "turns": [[{"say": "ok"}, {"result": result}]]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    run = asyncio.run(run_session(prompt, {**options, "cli_path": FAKE, "cwd": folder, "setting_sources": []})) or {}
    assert record.exists(), "the SDK was never started: run_session did not call query"
    lines = [json.loads(line) for line in record.read_text().splitlines()]
    return run, next(line["argv"] for line in lines if "argv" in line)


def test_e8_a_run_through_the_sdk_passes_resume_and_fork_to_the_binary_and_returns_the_session_id_even_after_an_error():
    ok = {"subtype": "success", "result": "analysis done", "cost": 0.01, "turns": 1}
    first, argv = _run("s-base", ok, {})
    assert first.get("session_id") == "s-base" and first.get("result") == "analysis done" and first.get("error") is None
    assert resumed_id(argv) is None and "--fork-session" not in argv
    forked, argv = _run("s-fork", ok, session_options(plan(fork=True)))
    assert forked.get("session_id") == "s-fork"
    assert resumed_id(argv) == "s-base" and "--fork-session" in argv
    resumed, argv = _run("s-base", ok, session_options(plan()))
    assert resumed.get("session_id") == "s-base" and resumed_id(argv) == "s-base" and "--fork-session" not in argv
    failed, _ = _run("s-err", {"subtype": "error_max_turns", "result": None}, {})
    assert failed.get("session_id") == "s-err" and failed.get("error")
