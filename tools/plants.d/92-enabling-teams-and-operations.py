# Planted wrong solutions of module 92-enabling-teams-and-operations: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_M92, _S92, _R92 = "managed/managed-settings.json", ".claude/settings.json", "docs/rollout.md"

_P92 = {
    "wrong-managed-rules-open": {_M92: [('"allowManagedPermissionRulesOnly": true', '"allowManagedPermissionRulesOnly": false')]},
    "wrong-bypass-allowed": {_M92: [('"deny": ["Read(./.env)", "Read(./secrets/**)"],\n    "disableBypassPermissionsMode": "disable"', '"deny": ["Read(./.env)", "Read(./secrets/**)"]')]},
    "wrong-env-readable": {_M92: [('"Read(./.env)", ', "")]},
    "wrong-hooks-in-project": {_S92: [('  "permissions": {\n    "allow"', '  "allowManagedHooksOnly": true,\n  "permissions": {\n    "allow"')]},
    "wrong-marketplaces-in-project": {_S92: [('"enabledPlugins"', '"strictKnownMarketplaces": [], "enabledPlugins"')]},
    "wrong-hooks-open": {_M92: [('"allowManagedHooksOnly": true', '"allowManagedHooksOnly": false')]},
    "wrong-marketplace-empty": {_M92: [('"strictKnownMarketplaces": [\n    {"source": "github", "repo": "example-org/claude-plugins"}\n  ]', '"strictKnownMarketplaces": []')]},
    "wrong-marketplace-foreign": {_M92: [('"repo": "example-org/claude-plugins"', '"repo": "other-org/claude-plugins"')]},
    "wrong-sideload-open": {_M92: [('"disableSideloadFlags": true', '"disableSideloadFlags": false')]},
    "wrong-mcp-not-exclusive": {_M92: [('"allowManagedMcpServersOnly": true', '"allowManagedMcpServersOnly": false')]},
    "wrong-mcp-two-keys": {_M92: [('{"serverName": "docs"}', '{"serverName": "docs", "serverUrl": "https://docs.example.com/*"}')]},
    "wrong-mcp-bad-name": {_M92: [('{"serverName": "docs"}', '{"serverName": "docs server"}')]},
    "wrong-mcp-allowed-and-denied": {_M92: [('"deniedMcpServers": [{"serverName": "scratch"}]', '"deniedMcpServers": [{"serverName": "tickets"}]')]},
    "wrong-no-model-list": {_M92: [('  "availableModels": ["sonnet", "haiku"],\n', "")]},
    "wrong-model-outside-list": {_M92: [('"model": "sonnet"', '"model": "opus"')]},
    "wrong-effort-xhigh": {_M92: [('"maxEffortLevel": "high"', '"maxEffortLevel": "xhigh"')]},
    "wrong-effort-max": {_M92: [('"maxEffortLevel": "high"', '"maxEffortLevel": "max"')]},
    "wrong-groups-over-org": {_R92: [("| group | Data | 6000 |", "| group | Data | 6001 |")]},
    "wrong-member-over-group": {_R92: [("| member | default | 500 |", "| member | default | 6500 |")]},
    "wrong-no-usage-credits": {_R92: [("Usage credits are on, so members", "Overage is off, so members")]},
    "wrong-baseline-3-weeks": {_R92: [("Baseline: 4 weeks", "Baseline: 3 weeks")]},
    "wrong-activity-target": {_R92: [("- Time to merge for pull requests", "- Lines accepted per developer")]},
    "wrong-one-target": {_R92: [("- Time to merge for pull requests\n", "")]},
    "wrong-precedence-order": {_R92: [("| 3 | Project local | The developer, for one project |\n| 4 | Shared project | The team, in version control |", "| 3 | Shared project | The team, in version control |\n| 4 | Project local | The developer, for one project |")]},
    "wrong-no-group-note": {_R92: [("and per-group policy is not yet supported there", "and it is the same for every group")]},
    "wrong-home-path": {_R92: [("# Rollout plan\n", "# Rollout plan\n\nNotes live in /home/dev/notes.\n")]},
}

PLANTS[f"{X}/92-enabling-teams-and-operations/unit-01/practice-1"] = {l: (_M92, _P92) for l in ("python", "typescript", "java", "kotlin")}
