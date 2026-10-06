import itertools
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
import rhythm_plan as _solution


def choose(job):
    value = _solution.choose(dict(job))
    assert value is not None, "choose returned nothing"
    return value


def refused(job):
    try:
        choose(job)
    except ValueError:
        return True
    return False


def sweep(axes, base):
    """Every combination of the axes, each added to the base job."""
    names = [k for k, _ in axes]
    for values in itertools.product(*[v for _, v in axes]):
        yield {**base, **dict(zip(names, values))}


def pick(mechanism, reason, minutes=0):
    return {"mechanism": mechanism, "reason": reason, "interval_minutes": minutes}


# The bank of the page: id, job, expected mechanism, reason code and interval in minutes.
BANK = [
    ("r01", {}, "loop-self-paced", "pace-by-what-is-seen", 0),
    ("r02", {"interval_seconds": 300}, "loop-fixed", "fixed-cadence", 5),
    ("r03", {"interval_seconds": 30}, "loop-fixed", "fixed-cadence", 1),
    ("r04", {"trigger": "once"}, "one-shot-task", "single-fire", 0),
    ("r05", {"trigger": "once", "machine_off": True}, "routine", "survives-closed-machine", 0),
    ("r06", {"interval_seconds": 86400, "machine_off": True}, "routine", "survives-closed-machine", 1440),
    ("r07", {"interval_seconds": 1800, "lasts_days": 30, "local_files": True}, "desktop-task", "durable-and-local", 30),
    ("r08", {"interval_seconds": 7200, "lasts_days": 30}, "routine", "outlives-seven-days", 120),
    ("r09", {"trigger": "event"}, "monitor", "push-not-poll", 0),
    ("r10", {"trigger": "event", "repo_event": True}, "routine", "react-to-event-unattended", 0),
    ("r11", {"trigger": "condition"}, "goal", "until-condition-holds", 0),
    ("r12", {"trigger": "background"}, "background-task", "work-while-it-runs", 0),
    ("r13", {"ci": True, "interval_seconds": 600}, "headless-run", "no-person-present", 0),
    ("r14", {"interval_seconds": 900, "session_open": False, "local_files": True}, "desktop-task", "durable-and-local", 15),
]


def test_m1_every_job_of_the_bank_gets_its_mechanism_its_reason_and_its_interval():
    wrong = [i for i, j, m, r, n in BANK if choose(j) != pick(m, r, n)]
    assert wrong == [], f"these jobs got the wrong mechanism, reason or interval: {wrong}"


def test_e1_a_pipeline_job_is_a_headless_run_whatever_else_is_true():
    for j in sweep([("trigger", ['interval', 'event', 'condition', 'once', 'background']), ("machine_off", [False, True]), ("lasts_days", [1, 30]), ("repo_event", [False, True]), ("session_open", [True, False])], {"ci": True}):
        assert choose(j) == pick("headless-run", "no-person-present", 0), j
    assert choose({"ci": True, "interval_seconds": 600}) == pick("headless-run", "no-person-present", 0), "{\"ci\": True, \"interval_seconds\": 600}"
    assert choose({"ci": True, "interval_seconds": 90, "local_files": True}) == pick("headless-run", "no-person-present", 0), "{\"ci\": True, \"interval_seconds\": 90, \"local_files\": True}"


def test_e2_a_condition_or_a_background_command_decides_before_an_event_and_an_event_is_watched_unless_it_must_run_unattended():
    for j in sweep([("machine_off", [False, True]), ("repo_event", [False, True]), ("interval_seconds", [0, 600]), ("lasts_days", [1, 30])], {"trigger": "condition"}):
        assert choose(j) == pick("goal", "until-condition-holds", 0), j
    for j in sweep([("machine_off", [False, True]), ("repo_event", [False, True]), ("interval_seconds", [0, 600]), ("lasts_days", [1, 30])], {"trigger": "background"}):
        assert choose(j) == pick("background-task", "work-while-it-runs", 0), j
    assert choose({"trigger": "event"}) == pick("monitor", "push-not-poll", 0), "{\"trigger\": \"event\"}"
    assert choose({"trigger": "event", "interval_seconds": 600}) == pick("monitor", "push-not-poll", 0), "{\"trigger\": \"event\", \"interval_seconds\": 600}"
    assert choose({"trigger": "event", "repo_event": True}) == pick("routine", "react-to-event-unattended", 0), "{\"trigger\": \"event\", \"repo_event\": True}"
    assert choose({"trigger": "event", "machine_off": True}) == pick("routine", "react-to-event-unattended", 0), "{\"trigger\": \"event\", \"machine_off\": True}"
    assert choose({"trigger": "event", "repo_event": True, "machine_off": True}) == pick("routine", "react-to-event-unattended", 0), "{\"trigger\": \"event\", \"repo_event\": True, \"machine_off\": True}"


def test_e3_a_one_off_job_fires_in_the_session_unless_the_machine_is_off_or_the_session_is_closed():
    assert choose({"trigger": "once"}) == pick("one-shot-task", "single-fire", 0), "{\"trigger\": \"once\"}"
    assert choose({"trigger": "once", "interval_seconds": 120, "lasts_days": 30}) == pick("one-shot-task", "single-fire", 0), "{\"trigger\": \"once\", \"interval_seconds\": 120, \"lasts_days\": 30}"
    assert choose({"trigger": "once", "machine_off": True}) == pick("routine", "survives-closed-machine", 0), "{\"trigger\": \"once\", \"machine_off\": True}"
    assert choose({"trigger": "once", "session_open": False}) == pick("desktop-task", "durable-and-local", 0), "{\"trigger\": \"once\", \"session_open\": False}"
    assert choose({"trigger": "once", "session_open": False, "machine_off": True}) == pick("routine", "survives-closed-machine", 0), "{\"trigger\": \"once\", \"session_open\": False, \"machine_off\": True}"
    assert choose({"trigger": "once", "machine_off": True, "lasts_days": 30}) == pick("routine", "survives-closed-machine", 0), "{\"trigger\": \"once\", \"machine_off\": True, \"lasts_days\": 30}"


def test_e4_a_cloud_schedule_is_refused_below_one_hour_and_a_desktop_schedule_below_one_minute():
    assert refused({"interval_seconds": 3599, "machine_off": True}), "{\"interval_seconds\": 3599, \"machine_off\": True} must be an error"
    assert refused({"interval_seconds": 60, "machine_off": True}), "{\"interval_seconds\": 60, \"machine_off\": True} must be an error"
    assert refused({"machine_off": True}), "{\"machine_off\": True} must be an error"
    assert refused({"interval_seconds": 1800, "lasts_days": 30}), "{\"interval_seconds\": 1800, \"lasts_days\": 30} must be an error"
    assert refused({"lasts_days": 30}), "{\"lasts_days\": 30} must be an error"
    assert refused({"interval_seconds": 59, "lasts_days": 30, "local_files": True}), "{\"interval_seconds\": 59, \"lasts_days\": 30, \"local_files\": True} must be an error"
    assert refused({"lasts_days": 30, "local_files": True}), "{\"lasts_days\": 30, \"local_files\": True} must be an error"
    assert refused({"interval_seconds": 30, "session_open": False}), "{\"interval_seconds\": 30, \"session_open\": False} must be an error"
    assert refused({"session_open": False}), "{\"session_open\": False} must be an error"
    assert choose({"interval_seconds": 3600, "machine_off": True}) == pick("routine", "survives-closed-machine", 60), "{\"interval_seconds\": 3600, \"machine_off\": True}"
    assert choose({"interval_seconds": 5400, "machine_off": True}) == pick("routine", "survives-closed-machine", 90), "{\"interval_seconds\": 5400, \"machine_off\": True}"
    assert choose({"interval_seconds": 60, "lasts_days": 30, "local_files": True}) == pick("desktop-task", "durable-and-local", 1), "{\"interval_seconds\": 60, \"lasts_days\": 30, \"local_files\": True}"
    assert choose({"interval_seconds": 61, "session_open": False}) == pick("desktop-task", "durable-and-local", 2), "{\"interval_seconds\": 61, \"session_open\": False}"
    assert choose({"interval_seconds": 30}) == pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 30}"


def test_e5_a_fixed_loop_rounds_seconds_up_to_whole_minutes_and_no_interval_means_self_paced():
    assert choose({"interval_seconds": 1}) == pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 1}"
    assert choose({"interval_seconds": 59}) == pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 59}"
    assert choose({"interval_seconds": 60}) == pick("loop-fixed", "fixed-cadence", 1), "{\"interval_seconds\": 60}"
    assert choose({"interval_seconds": 61}) == pick("loop-fixed", "fixed-cadence", 2), "{\"interval_seconds\": 61}"
    assert choose({"interval_seconds": 300}) == pick("loop-fixed", "fixed-cadence", 5), "{\"interval_seconds\": 300}"
    assert choose({"interval_seconds": 3600}) == pick("loop-fixed", "fixed-cadence", 60), "{\"interval_seconds\": 3600}"
    assert choose({"interval_seconds": 86400}) == pick("loop-fixed", "fixed-cadence", 1440), "{\"interval_seconds\": 86400}"
    assert choose({"interval_seconds": 0}) == pick("loop-self-paced", "pace-by-what-is-seen", 0), "{\"interval_seconds\": 0}"
    assert choose({"trigger": "interval", "lasts_days": 7}) == pick("loop-self-paced", "pace-by-what-is-seen", 0), "{\"trigger\": \"interval\", \"lasts_days\": 7}"


def test_e6_a_recurring_loop_lasts_seven_days_so_a_longer_job_needs_a_durable_home():
    assert choose({"interval_seconds": 600, "lasts_days": 7}) == pick("loop-fixed", "fixed-cadence", 10), "{\"interval_seconds\": 600, \"lasts_days\": 7}"
    assert choose({"interval_seconds": 600, "lasts_days": 8, "local_files": True}) == pick("desktop-task", "durable-and-local", 10), "{\"interval_seconds\": 600, \"lasts_days\": 8, \"local_files\": True}"
    assert choose({"interval_seconds": 3600, "lasts_days": 8}) == pick("routine", "outlives-seven-days", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 8}"
    assert choose({"interval_seconds": 3600, "lasts_days": 8, "machine_off": True}) == pick("routine", "survives-closed-machine", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 8, \"machine_off\": True}"
    assert choose({"interval_seconds": 3600, "lasts_days": 365, "session_open": False}) == pick("routine", "outlives-seven-days", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"session_open\": False}"
    assert choose({"interval_seconds": 3600, "lasts_days": 365, "local_files": True, "session_open": False}) == pick("desktop-task", "durable-and-local", 60), "{\"interval_seconds\": 3600, \"lasts_days\": 365, \"local_files\": True, \"session_open\": False}"
    assert refused({"lasts_days": 8}), "{\"lasts_days\": 8} must be an error"
    assert refused({"lasts_days": 8, "local_files": True}), "{\"lasts_days\": 8, \"local_files\": True} must be an error"


def test_e7_an_unknown_value_is_an_error_a_cloud_run_cannot_see_local_files_and_a_missing_key_takes_its_default():
    assert choose({}) == pick("loop-self-paced", "pace-by-what-is-seen", 0), "{}"
    assert refused({"trigger": "cron"}), "{\"trigger\": \"cron\"} must be an error"
    assert refused({"trigger": ""}), "{\"trigger\": \"\"} must be an error"
    assert refused({"interval_seconds": -1}), "{\"interval_seconds\": -1} must be an error"
    assert refused({"lasts_days": 0}), "{\"lasts_days\": 0} must be an error"
    assert refused({"machine_off": True, "local_files": True, "interval_seconds": 7200}), "{\"machine_off\": True, \"local_files\": True, \"interval_seconds\": 7200} must be an error"
    assert refused({"trigger": "once", "machine_off": True, "local_files": True}), "{\"trigger\": \"once\", \"machine_off\": True, \"local_files\": True} must be an error"
    assert refused({"ci": True, "trigger": "cron"}), "{\"ci\": True, \"trigger\": \"cron\"} must be an error"
    assert refused({"ci": True, "interval_seconds": -5}), "{\"ci\": True, \"interval_seconds\": -5} must be an error"
    assert refused({"ci": True, "machine_off": True, "local_files": True}), "{\"ci\": True, \"machine_off\": True, \"local_files\": True} must be an error"
