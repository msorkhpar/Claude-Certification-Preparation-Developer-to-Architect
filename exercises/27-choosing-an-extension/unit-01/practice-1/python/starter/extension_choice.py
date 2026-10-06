"""Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)

SURFACES = ("code", "api")
KNOWLEDGE = ("none", "convention", "reference", "procedure")
CARRIED = ("skill", "hook", "subagent", "mcp")  # what a plugin can bundle
TIMINGS = ("none", "interval", "event", "condition", "background")
PRESENCE = ("session", "pipeline", "away")
PERSONAL = ("none", "voice", "display", "keys")
LOOP_EXPIRY_DAYS = 7  # a recurring task of a session expires after seven days


def _pick(mechanism, reason):
    return {"mechanism": mechanism, "reason": reason}


def _check_counts(repos, lasts_days, presence, local_files):
    """TODO 1 of 8 (finish this to pass e6): refuse numbers and a combination that make no sense.

    Receives repos, lasts_days, presence and local_files (already defaulted). Raises ValueError when repos or lasts_days is below 1, or
    when presence is "away" and local_files is true (a cloud run starts from a fresh clone). Returns nothing otherwise.
    Example: _check_counts(0, 1, "session", False) raises; _check_counts(1, 1, "away", True) raises; _check_counts(2, 30, "away", False) returns
    """


def _api_choice(situation):
    """TODO 2 of 8 (finish this to pass e5): the pick of an application on the Messages API, or None for the default own tool.

    Receives the situation. Returns the pick builtin-tool / provided-schema when builtin_covers is true, else mcp / remote-server when
    external_system and remote_server are both true, else None (the caller then answers api-tool / own-schema-and-code).
    Example: _api_choice({"builtin_covers": True, "remote_server": True}) -> builtin-tool; _api_choice({"external_system": True}) -> None
    """
    return None


def _priority_choice(situation):
    """TODO 3 of 8 (finish this to pass e1 and e2): the pick for a rule that must hold, an outside system or noisy work.

    Receives the situation. The first match wins: guarantee gives hook / must-hold-every-time; external_system gives mcp / external-system;
    noisy gives subagent / isolate-context; otherwise return None. Whatever else is in the situation (timing, knowledge) does not matter here.
    Example: _priority_choice({"guarantee": True, "noisy": True}) -> hook; _priority_choice({"timing": "event"}) -> None
    """
    return None


def _event_choice(presence):
    """TODO 4 of 8 (finish this to pass e7): the pick for work that reacts to an event.

    Receives presence. Returns routine / runs-unattended when presence is "away", else monitor / push-not-poll.
    Example: _event_choice("session") -> monitor / push-not-poll
    """
    return None


def _interval_choice(situation):
    """TODO 5 of 8 (finish this to pass e8): the pick for work that repeats every so often.

    Receives the situation. Returns routine / runs-unattended when presence is "away"; else, when lasts_days is above LOOP_EXPIRY_DAYS,
    desktop-task / durable-and-local if local_files is true and routine / runs-unattended if not; else loop / session-rhythm.
    Example: _interval_choice({"lasts_days": 8, "local_files": True}) -> desktop-task; _interval_choice({"lasts_days": 7}) -> loop
    """
    return None


def _rhythm(situation):
    """The pick for work that runs without a person, on a rhythm or on an event, or None when the situation has none of these."""
    timing = situation.get("timing", "none")
    presence = situation.get("presence", "session")
    if presence == "pipeline":
        return _pick("headless-ci", "no-person-present")
    if timing == "condition":
        return _pick("goal", "until-condition-holds")
    if timing == "background":
        return _pick("background-task", "work-while-it-runs")
    if timing == "event":
        return _event_choice(presence)
    if timing == "interval":
        return _interval_choice(situation)
    return None


def _personal_choice(personal):
    """TODO 6 of 8 (finish this to pass e9): the pick for a preference of one person, or None.

    Receives the personal value. Returns output-style / response-voice for "voice", status-line / personal-display for "display",
    keybinding / personal-keys for "keys", and None for "none".
    Example: _personal_choice("keys") -> keybinding / personal-keys; _personal_choice("none") -> None
    """
    return None


def _knowledge_choice(knowledge, path_scoped):
    """TODO 7 of 8 (finish this to pass e3): the pick for what Claude must be told, or None when nothing needs telling.

    Receives knowledge and path_scoped. "convention" gives path-rule / scoped-convention when path_scoped, else claude-md / always-known;
    "reference" gives skill / on-demand-reference; "procedure" gives skill / repeatable-procedure; anything else gives None. path_scoped
    matters only for a convention.
    Example: _knowledge_choice("reference", True) -> skill / on-demand-reference; _knowledge_choice("none", True) -> None
    """
    return None


def _package(choice, repos):
    """TODO 8 of 8 (finish this to pass e4): a plugin carries a skill, a hook, a subagent or a server to a second repository.

    Receives the pick and repos. Returns plugin / shared-setup when repos is 2 or more and the pick's mechanism is in CARRIED; otherwise
    returns the pick unchanged (an instruction file, a path rule and a built-in tool are never packaged).
    Example: _package(_pick("hook", "must-hold-every-time"), 2) -> plugin; _package(_pick("claude-md", "always-known"), 2) -> unchanged
    """
    return choice


def choose(situation):
    log.debug("choose input: %r", situation)
    surface = situation.get("surface", "code")
    knowledge = situation.get("knowledge", "none")
    repos = situation.get("repos", 1)
    if surface not in SURFACES:
        raise ValueError(f"unknown surface: {surface}")
    if knowledge not in KNOWLEDGE:
        raise ValueError(f"unknown knowledge kind: {knowledge}")
    if situation.get("timing", "none") not in TIMINGS:
        raise ValueError(f"unknown timing: {situation.get('timing')}")
    if situation.get("presence", "session") not in PRESENCE:
        raise ValueError(f"unknown presence: {situation.get('presence')}")
    if situation.get("personal", "none") not in PERSONAL:
        raise ValueError(f"unknown personal preference: {situation.get('personal')}")
    _check_counts(repos, situation.get("lasts_days", 1), situation.get("presence", "session"), situation.get("local_files", False))
    if surface == "api":
        return _api_choice(situation) or _pick("api-tool", "own-schema-and-code")
    choice = (
        _priority_choice(situation)
        or _rhythm(situation)
        or _personal_choice(situation.get("personal", "none"))
        or _knowledge_choice(knowledge, situation.get("path_scoped", False))
        or _pick("builtin-tool", "built-in-covers")
    )
    return _package(choice, repos)
