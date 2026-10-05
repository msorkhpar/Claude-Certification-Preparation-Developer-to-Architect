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
    return math.ceil(seconds / 60)


def _cloud(seconds, reason):
    if seconds < CLOUD_MIN_SECONDS:
        raise ValueError("a routine runs at most once an hour")
    return _pick("routine", reason, _minutes(seconds))


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
    if trigger not in TRIGGERS:
        raise ValueError(f"unknown trigger: {trigger}")
    if seconds < 0:
        raise ValueError("interval_seconds cannot be negative")
    if days < 1:
        raise ValueError("lasts_days starts at 1")
    if machine_off and local_files:
        raise ValueError("a cloud run starts from a fresh clone and sees no local files")
    if job.get("ci", False):
        return _pick("headless-run", "no-person-present")
    if trigger == "condition":
        return _pick("goal", "until-condition-holds")
    if trigger == "background":
        return _pick("background-task", "work-while-it-runs")
    if trigger == "event":
        if job.get("repo_event", False) or machine_off:
            return _pick("routine", "react-to-event-unattended")
        return _pick("monitor", "push-not-poll")
    if trigger == "once":
        if machine_off:
            return _pick("routine", "survives-closed-machine")
        if not session_open:
            return _pick("desktop-task", "durable-and-local")
        return _pick("one-shot-task", "single-fire")
    if machine_off:
        return _cloud(seconds, "survives-closed-machine")
    if days > LOOP_EXPIRY_DAYS:
        return _local(seconds) if local_files else _cloud(seconds, "outlives-seven-days")
    if not session_open:
        return _local(seconds)
    if seconds == 0:
        return _pick("loop-self-paced", "pace-by-what-is-seen")
    return _pick("loop-fixed", "fixed-cadence", max(1, _minutes(seconds)))
