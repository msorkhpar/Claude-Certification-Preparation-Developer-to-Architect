# Case lists of module 55-mcp-in-claude-code: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/55-mcp-in-claude-code/unit-01/practice-1"] = {
    "name": "mcp_setup", "suite": "McpSetupTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the shared file declares the four team servers with the right shape"),
        ("e1", "edge", "credentials are read from the environment and never written in the file"),
        ("e2", "edge", "endpoints and paths that are not secret have a default so the file works unset"),
        ("e3", "edge", "only the small core server is loaded at the start"),
        ("e4", "edge", "shared servers stay in the project file and personal ones in the user scope file"),
        ("e5", "edge", "permissions allow the read only servers by name and deny the destructive tool"),
        ("e6", "edge", "the tool description says when to use it instead of grep and fits the limit"),
        ("e7", "edge", "the notes list every server with its scope and read the catalog as a resource"),
        ("e8", "edge", "no file holds a personal path an address or a key"),
    ],
    "plants": {
        "wrong-github-type": (["m1"], "declares the GitHub server with a transport that is not http"),
        "wrong-docs-no-command": (["m1"], "declares the local docs server without a command"),
        "wrong-literal-token": (["e1", "e8", "m1"], "writes the GitHub token into the file"),
        "wrong-covered-credential": (["e1"], "sends the token under a name Claude Code blanks out toward a remote server"),
        "wrong-secret-default": (["e1"], "gives the documentation key a default value in the file"),
        "wrong-url-no-default": (["e1", "e2"], "leaves the GitHub URL without a default"),
        "wrong-project-dir-no-default": (["e2"], "uses the project directory variable without a default in a path"),
        "wrong-always-load-all": (["e3"], "loads the documentation server at the start as well"),
        "wrong-core-deferred": (["e3"], "leaves the core server to be found on demand"),
        "wrong-personal-in-shared": (["e4", "m1"], "puts the personal scratch server into the committed file"),
        "wrong-user-duplicates": (["e4"], "gives the user scope file a server with a shared name"),
        "wrong-allow-unanchored": (["e5"], "allows every MCP tool with a rule that names no server"),
        "wrong-allow-github": (["e5"], "allows every GitHub tool without asking"),
        "wrong-no-delete-deny": (["e5"], "drops the denial of the destructive GitHub tool"),
        "wrong-description-long": (["e6"], "writes a description longer than Claude Code keeps"),
        "wrong-boundary-buried": (["e6"], "puts the boundary against Grep after the first 300 characters"),
        "wrong-no-resource-ref": (["e7"], "shows an @ mention that names a server that is not configured"),
        "wrong-schema-as-tool": (["e7"], "describes the schema server as a tool instead of a catalog of resources"),
        "wrong-scope-user": (["e7"], "gives a shared server the scope user in the notes"),
        "wrong-home-path": (["e8"], "writes a personal home path into the notes"),
    },
}
