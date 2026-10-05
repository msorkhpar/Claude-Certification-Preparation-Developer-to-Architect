# Planted wrong solutions of module 55-mcp-in-claude-code: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_P55 = {
    "wrong-github-type": {".mcp.json": [('"type": "http",\n      "url": "${GITHUB_MCP_URL', '"type": "sse",\n      "url": "${GITHUB_MCP_URL')]},
    "wrong-docs-no-command": {".mcp.json": [('      "command": "python3",\n      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],', '      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],')]},
    "wrong-literal-token": {".mcp.json": [('"Authorization": "Bearer ${GITHUB_TOKEN}"', '"Authorization": "Bearer ' + 'ghp_0123456789abcdefghij"')]},
    "wrong-covered-credential": {".mcp.json": [("Bearer ${GITHUB_TOKEN}", "Bearer ${NPM_TOKEN}")]},
    "wrong-secret-default": {".mcp.json": [('"DOCS_API_KEY": "${DOCS_API_KEY}"', '"DOCS_API_KEY": "${DOCS_API_KEY:-dev-key}"')]},
    "wrong-url-no-default": {".mcp.json": [('"url": "${GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}"', '"url": "${GITHUB_MCP_URL}"')]},
    "wrong-project-dir-no-default": {".mcp.json": [('"args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"]', '"args": ["${CLAUDE_PROJECT_DIR}/tools/docs_server.py"]')]},
    "wrong-always-load-all": {".mcp.json": [('      "command": "python3",\n      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],', '      "command": "python3",\n      "alwaysLoad": true,\n      "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py"],')]},
    "wrong-core-deferred": {".mcp.json": [('core-mcp.example.com/mcp}",\n      "alwaysLoad": true\n', 'core-mcp.example.com/mcp}"\n')]},
    "wrong-personal-in-shared": {".mcp.json": [('    "schema": {', '    "scratch": {"type": "stdio", "command": "python3", "args": ["scratch.py"]},\n    "schema": {')]},
    "wrong-user-duplicates": {"user-scope.example.json": [('    "scratch": {', '    "github": {"type": "http", "url": "https://other.example.com/mcp"},\n    "scratch": {')]},
    "wrong-allow-unanchored": {".claude/settings.json": [('"allow": ["mcp__docs__*", "mcp__schema__*"]', '"allow": ["mcp__*"]')]},
    "wrong-allow-github": {".claude/settings.json": [('"mcp__schema__*"]', '"mcp__schema__*", "mcp__github__*"]')]},
    "wrong-no-delete-deny": {".claude/settings.json": [('"deny": ["mcp__github__delete_repository"]', '"deny": []')]},
    "wrong-description-long": {"docs/tool-descriptions.json": [("open a link for that.\"", "open a link for that. " + "Extra detail. " * 150 + "\"")]},
    "wrong-boundary-buried": {"docs/tool-descriptions.json": [("Use this instead of Grep to search", "A search tool for the documentation. " + "It is maintained by the platform team and indexed nightly. " * 6 + "Use this instead of Grep to search")]},
    "wrong-no-resource-ref": {"docs/mcp-servers.md": [("`@schema:schema://orders`", "`@catalog:schema://orders`")]},
    "wrong-schema-as-tool": {"docs/mcp-servers.md": [("resources: the database schemas", "tools: a schema lookup"), ("A catalog, so it is read as a resource and not through a search tool.", "Queried through a lookup tool.")]},
    "wrong-scope-user": {"docs/mcp-servers.md": [("| `github` | project |", "| `github` | user |")]},
    "wrong-home-path": {"docs/mcp-servers.md": [("and `DOCS_API_KEY`). A personal", "and `DOCS_API_KEY`, kept in /home/dev/.profile). A personal")]},
}

PLANTS[f"{X}/55-mcp-in-claude-code/unit-01/practice-1"] = both(".mcp.json", _P55)
