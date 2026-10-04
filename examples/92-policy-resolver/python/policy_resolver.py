"""Which value does a developer's Claude Code actually use when a team, a person and an organisation all set the same key? A small resolver that applies the documented precedence, the keys only an organisation can set, the lists that merge and the locks that stop them merging.

The layers are invented and the key list is a subset of the settings documentation read on 2026-10-04 (Claude Code settings and managed-settings pages). Nothing here starts Claude Code or calls a model.
"""
LEVELS = ["managed", "command line", "local", "project", "user"]
MANAGED_ONLY = {"allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"}
EFFORT = ["low", "medium", "high", "xhigh", "max"]


def effective(layers):
    """The settings Claude Code applies, and a note for every entry that was ignored and why."""
    managed = layers.get("managed", {})
    lock_rules = managed.get("allowManagedPermissionRulesOnly") is True
    lock_mcp = managed.get("allowManagedMcpServersOnly") is True
    out, notes = {}, []
    for key in sorted({k for level in layers.values() for k in level}):
        values = [(level, layers[level][key]) for level in LEVELS if level in layers and key in layers[level]]
        reason = None
        if key in MANAGED_ONLY:
            reason = "a managed-only key"
        elif key in ("permissions.allow", "permissions.deny") and lock_rules:
            reason = "managed settings are the only source of permission rules"
        elif key == "allowedMcpServers" and lock_mcp:
            reason = "managed settings are the only source of the MCP allowlist"
        elif key == "availableModels" and any(level == "managed" for level, _ in values):
            reason = "the managed list applies as it is"
        if reason:
            notes += [f"ignored {key} from {level}: {reason}" for level, _ in values if level != "managed"]
            values = [(level, v) for level, v in values if level == "managed"]
        if not values:
            continue
        out[key] = combine(key, [v for _, v in values])
    return out, notes


def combine(key, values):
    """Lists merge without duplicates, the lowest effort cap wins, a connector ban from any level stands, and any other key takes the highest level's value."""
    if isinstance(values[0], list):
        merged = []
        for value in values:
            merged += [item for item in value if item not in merged]
        return merged
    if key == "maxEffortLevel":
        return min(values, key=EFFORT.index)
    if key == "disableClaudeAiConnectors":
        return any(v is True for v in values)
    return values[0]


def allowed_model(requested, settings):
    """A managed list of available models refuses any other choice, whoever makes it."""
    listed = settings.get("availableModels")
    return "allowed" if listed is None or requested in listed else "refused (not in availableModels)"


def show(value):
    if isinstance(value, list):
        return ", ".join(value)
    return "True" if value is True else "False" if value is False else str(value)


def main():
    layers = {
        "managed": {"allowManagedPermissionRulesOnly": True, "permissions.deny": ["Read(./.env)"], "permissions.allow": ["Read(./docs/**)"],
                    "maxEffortLevel": "high", "availableModels": ["sonnet", "haiku"], "cleanupPeriodDays": 7, "spinnerTipsEnabled": True},
        "command line": {"cleanupPeriodDays": 14},
        "local": {"permissions.allow": ["Bash(npm test)"], "allowManagedHooksOnly": False},
        "project": {"permissions.allow": ["Bash(git status)"], "permissions.deny": ["Read(./secrets/**)"], "maxEffortLevel": "xhigh",
                    "availableModels": ["opus"], "disableClaudeAiConnectors": False, "spinnerTipsEnabled": False},
        "user": {"permissions.allow": ["Edit(./notes/**)"], "disableClaudeAiConnectors": True, "spinnerTipsEnabled": False},
    }
    settings, notes = effective(layers)
    print("effective settings:")
    for key, value in settings.items():
        print(f"  {key} = {show(value)}")
    print("notes:")
    for note in notes:
        print(f"  {note}")
    for model in ("opus", "haiku"):
        print(f"model {model}: {allowed_model(model, settings)}")
    open_layers = {"project": {"permissions.allow": ["Bash(git status)"]}, "user": {"permissions.allow": ["Edit(./notes/**)", "Bash(git status)"]}}
    merged, _ = effective(open_layers)
    print(f"without a lock the lists merge: {show(merged['permissions.allow'])}")


if __name__ == "__main__":
    main()
