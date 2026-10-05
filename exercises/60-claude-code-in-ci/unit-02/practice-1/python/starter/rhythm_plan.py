"""Which way of running Claude Code unattended or on a rhythm a job calls for, and why. See ../../statement.md."""
import math
import logging

log = logging.getLogger(__name__)

TRIGGERS = ("interval", "event", "condition", "once", "background")
CLOUD_MIN_SECONDS = 3600  # a routine never runs more often than hourly
LOCAL_MIN_SECONDS = 60  # a desktop task or a loop never runs more often than every minute
LOOP_EXPIRY_DAYS = 7  # a recurring task of a session expires after seven days


def _pick(mechanism, reason, minutes=0):
    return {"mechanism": mechanism, "reason": reason, "interval_minutes": minutes}


def _minutes(seconds):
    # TODO 6 of 8 (finish this to pass e5): the rounding. Receives seconds and returns whole minutes, rounding up.
    #   Example: 59 -> 1, 60 -> 1, 61 -> 2.
    return seconds // 60


def _cloud(seconds, reason):
    # TODO 5 of 8 (finish this to pass e4): the floor of a cloud routine. Refuse with an error ("a routine runs at most
    #   once an hour") when the interval is below CLOUD_MIN_SECONDS. Example: 1800 seconds -> refused; 3600 -> allowed.
    # TODO 1 of 8 (finish this to pass m1): the plan of a cloud routine. Return the plan with mechanism routine, the
    #   given reason and the interval in minutes (the helper that rounds seconds up). Example: 7200 seconds -> routine,
    #   120 minutes.
    return _pick("routine", reason)


def _local(seconds):
    if seconds < LOCAL_MIN_SECONDS:
        raise ValueError("a desktop task runs at most once a minute")
    return _pick("desktop-task", "durable-and-local", _minutes(seconds))


def choose(job):
    log.debug("choose input: %r", job)
    trigger = job.get("trigger", "interval")
    seconds = job.get("interval_seconds", 0)
    days = job.get("lasts_days", 1)
    machine_off = job.get("machine_off", False)
    local_files = job.get("local_files", False)
    session_open = job.get("session_open", True)
    # TODO 8 of 8 (finish this to pass e7): the refusals. Refuse with an error when the trigger is not one of TRIGGERS,
    #   when the interval is negative, when lasts_days is below 1, and when the job needs the machine off and local files
    #   at once (a cloud run starts from a fresh clone). Example: trigger weekly -> refused.
    # TODO 2 of 8 (finish this to pass e1): the pipeline rule. When the job runs in CI, return mechanism headless-run
    #   with the reason no-person-present, whatever its trigger, interval or machine. Example: ci true with machine_off
    #   true -> headless-run.
    if trigger == "condition":
        return _pick("goal", "until-condition-holds")
    if trigger == "background":
        return _pick("background-task", "work-while-it-runs")
    # TODO 3 of 8 (finish this to pass e2): the event rows. For an event trigger: a routine with the reason react-to-
    #   event-unattended when the job is a repository event or the machine is off, otherwise a monitor with the reason
    #   push-not-poll. Example: event with the machine on and no repo event -> monitor.
    if trigger == "once":
        if machine_off:
            return _pick("routine", "survives-closed-machine")
        # TODO 4 of 8 (finish this to pass e3): the one-off rows. For a once trigger on a machine that stays on, when
        #   the session is closed return a desktop-task with the reason durable-and-local; with the session open it fires
        #   there as a one-shot-task. Example: once, session closed -> desktop-task.
        return _pick("one-shot-task", "single-fire")
    if machine_off:
        return _cloud(seconds, "survives-closed-machine")
    # TODO 7 of 8 (finish this to pass e6): the expiry of a loop. When the job lasts more than LOOP_EXPIRY_DAYS days,
    #   use a local desktop task if it needs local files, otherwise a cloud routine with the reason outlives-seven-days.
    #   Example: 10 days, no local files -> routine.
    if not session_open:
        return _local(seconds)
    if seconds == 0:
        return _pick("loop-self-paced", "pace-by-what-is-seen")
    return _pick("loop-fixed", "fixed-cadence", max(1, _minutes(seconds)))
