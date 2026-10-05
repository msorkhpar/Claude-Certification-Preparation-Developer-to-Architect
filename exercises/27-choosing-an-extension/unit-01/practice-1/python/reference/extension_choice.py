"""Which Claude Code extension (or API tool) a situation calls for, and why. See ../../statement.md."""

SURFACES = ("code", "api")
KNOWLEDGE = ("none", "convention", "reference", "procedure")
CARRIED = ("skill", "hook", "subagent", "mcp")  # what a plugin can bundle


def _pick(mechanism, reason):
    return {"mechanism": mechanism, "reason": reason}


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
