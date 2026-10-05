"""Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md."""

SURFACES = ("code", "api")
KNOWLEDGE = ("none", "convention", "reference", "procedure")
CARRIED = ("skill", "hook", "subagent", "mcp")  # what a plugin can bundle
TIMINGS = ("none", "interval", "event", "condition", "background")
PRESENCE = ("session", "pipeline", "away")
PERSONAL = ("none", "voice", "display", "keys")
LOOP_EXPIRY_DAYS = 7  # a recurring task of a session expires after seven days


def _pick(mechanism, reason):
    return {"mechanism": mechanism, "reason": reason}


def _rhythm(situation):
    """The mechanism for work that runs without a person, on a rhythm or on an event, or None when the situation has none of these."""
    timing = situation.get("timing", "none")
    presence = situation.get("presence", "session")
    if presence == "pipeline":
        return _pick("headless-ci", "no-person-present")
    if timing == "condition":
        return _pick("goal", "until-condition-holds")
    if timing == "background":
        return _pick("background-task", "work-while-it-runs")
    if timing == "event":
        return _pick("routine", "runs-unattended") if presence == "away" else _pick("monitor", "push-not-poll")
    if timing == "interval":
        if presence == "away":
            return _pick("routine", "runs-unattended")
        if situation.get("lasts_days", 1) > LOOP_EXPIRY_DAYS:
            return _pick("desktop-task", "durable-and-local") if situation.get("local_files", False) else _pick("routine", "runs-unattended")
        return _pick("loop", "session-rhythm")
    return None


def choose(situation):
    surface = situation.get("surface", "code")
    knowledge = situation.get("knowledge", "none")
    repos = situation.get("repos", 1)
    if surface not in SURFACES:
        raise ValueError(f"unknown surface: {surface}")
    if knowledge not in KNOWLEDGE:
        raise ValueError(f"unknown knowledge kind: {knowledge}")
    if repos < 1:
        raise ValueError("repos starts at 1")
    if situation.get("timing", "none") not in TIMINGS:
        raise ValueError(f"unknown timing: {situation.get('timing')}")
    if situation.get("presence", "session") not in PRESENCE:
        raise ValueError(f"unknown presence: {situation.get('presence')}")
    if situation.get("personal", "none") not in PERSONAL:
        raise ValueError(f"unknown personal preference: {situation.get('personal')}")
    if situation.get("lasts_days", 1) < 1:
        raise ValueError("lasts_days starts at 1")
    if situation.get("presence", "session") == "away" and situation.get("local_files", False):
        raise ValueError("a cloud run starts from a fresh clone and sees no local files")
    if surface == "api":
        if situation.get("builtin_covers", False):
            return _pick("builtin-tool", "provided-schema")
        if situation.get("external_system", False) and situation.get("remote_server", False):
            return _pick("mcp", "remote-server")
        return _pick("api-tool", "own-schema-and-code")
    if situation.get("guarantee", False):
        choice = _pick("hook", "must-hold-every-time")
    elif situation.get("external_system", False):
        choice = _pick("mcp", "external-system")
    elif situation.get("noisy", False):
        choice = _pick("subagent", "isolate-context")
    elif _rhythm(situation) is not None:
        choice = _rhythm(situation)
    elif situation.get("personal", "none") == "voice":
        choice = _pick("output-style", "response-voice")
    elif situation.get("personal", "none") == "display":
        choice = _pick("status-line", "personal-display")
    elif situation.get("personal", "none") == "keys":
        choice = _pick("keybinding", "personal-keys")
    elif knowledge == "convention":
        choice = _pick("path-rule", "scoped-convention") if situation.get("path_scoped", False) else _pick("claude-md", "always-known")
    elif knowledge == "reference":
        choice = _pick("skill", "on-demand-reference")
    elif knowledge == "procedure":
        choice = _pick("skill", "repeatable-procedure")
    else:
        choice = _pick("builtin-tool", "built-in-covers")
    if repos >= 2 and choice["mechanism"] in CARRIED:
        return _pick("plugin", "shared-setup")
    return choice
