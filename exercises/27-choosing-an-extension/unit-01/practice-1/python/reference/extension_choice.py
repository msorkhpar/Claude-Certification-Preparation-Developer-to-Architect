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
    if repos < 1 or lasts_days < 1 or (presence == "away" and local_files):
        raise ValueError("repos and lasts_days start at 1, and a cloud run starts from a fresh clone and sees no local files")


def _api_choice(situation):
    """The pick of an application on the Messages API, or None for the default own tool."""
    if situation.get("builtin_covers", False):
        return _pick("builtin-tool", "provided-schema")
    if situation.get("external_system", False) and situation.get("remote_server", False):
        return _pick("mcp", "remote-server")


def _priority_choice(situation):
    """The pick for a rule that must hold, an outside system or noisy work, or None when none of them applies."""
    if situation.get("guarantee", False):
        return _pick("hook", "must-hold-every-time")
    if situation.get("external_system", False):
        return _pick("mcp", "external-system")
    if situation.get("noisy", False):
        return _pick("subagent", "isolate-context")


def _event_choice(presence):
    """The pick for work that reacts to an event."""
    return _pick("routine", "runs-unattended") if presence == "away" else _pick("monitor", "push-not-poll")


def _interval_choice(situation):
    """The pick for work that repeats every so often."""
    if situation.get("presence", "session") == "away":
        return _pick("routine", "runs-unattended")
    if situation.get("lasts_days", 1) > LOOP_EXPIRY_DAYS:
        return _pick("desktop-task", "durable-and-local") if situation.get("local_files", False) else _pick("routine", "runs-unattended")
    return _pick("loop", "session-rhythm")


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
    """The pick for a preference of one person, or None."""
    return {
        "voice": _pick("output-style", "response-voice"),
        "display": _pick("status-line", "personal-display"),
        "keys": _pick("keybinding", "personal-keys"),
    }.get(personal)


def _knowledge_choice(knowledge, path_scoped):
    """The pick for what Claude must be told, or None when nothing needs telling."""
    if knowledge == "convention":
        return _pick("path-rule", "scoped-convention") if path_scoped else _pick("claude-md", "always-known")
    if knowledge == "reference":
        return _pick("skill", "on-demand-reference")
    if knowledge == "procedure":
        return _pick("skill", "repeatable-procedure")


def _package(choice, repos):
    """A plugin carries a skill, a hook, a subagent or a server to a second repository; nothing else."""
    if repos >= 2 and choice["mechanism"] in CARRIED:
        return _pick("plugin", "shared-setup")
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
