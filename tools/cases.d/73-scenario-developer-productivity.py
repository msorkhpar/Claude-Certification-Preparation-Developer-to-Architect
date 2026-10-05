# Case lists of module 73-scenario-developer-productivity: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/73-scenario-developer-productivity/unit-01/practice-1"] = {
    "name": "mcp_setup", "suite": "McpSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "every mcp tool reference belongs to a configured server"),
        ("e1", "edge", "credentials come from the environment and the token has no default"),
        ("e2", "edge", "the explorer is read only and says when to use it"),
        ("e3", "edge", "the scaffolder writes only in the generated folder"),
        ("e4", "edge", "the tickets server is read only for agents"),
        ("e5", "edge", "the environment file is denied and no whole tool is approved"),
        ("e6", "edge", "the team note lists every variable and says which session to start"),
        ("e7", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-settings-ghost": (["m1"], "allows a ticket tool under a server name that is not configured"),
        "wrong-literal-token": (["e1"], "writes the token into the header"),
        "wrong-token-default": (["e1"], "gives the token variable a default value"),
        "wrong-explorer-bash": (["e2"], "gives the explorer the Bash tool"),
        "wrong-explorer-inherits": (["e2"], "leaves the tools line out, so the explorer inherits every tool"),
        "wrong-explorer-description": (["e2"], "describes the explorer without saying when to use it"),
        "wrong-scaffolder-bash": (["e3"], "gives the scaffolder the Bash tool"),
        "wrong-write-wide": (["e3"], "adds a Write path rule for the source folder, which Claude Code never consults"),
        "wrong-tickets-wildcard": (["e4"], "allows every ticket tool, creating and deleting included"),
        "wrong-no-delete-deny": (["e4"], "leaves deleting a ticket open"),
        "wrong-no-env-deny": (["e5"], "leaves the environment file readable"),
        "wrong-bash-allow": (["e5"], "approves the whole Bash tool"),
        "wrong-note-missing-variable": (["e6"], "leaves one variable out of the team note"),
        "wrong-rewritten-resumes": (["e6"], "tells the team to resume a session after the code was rewritten"),
        "wrong-home-path": (["e7"], "writes a personal home path into the team note"),
    },
}
