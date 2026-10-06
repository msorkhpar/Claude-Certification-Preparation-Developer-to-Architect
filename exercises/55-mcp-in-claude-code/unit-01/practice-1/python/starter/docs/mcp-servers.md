# MCP servers of the orders platform

The shared servers live in `.mcp.json` at the project root and are committed. Credentials are never written in it: each one is read from
an environment variable that every developer sets in their own shell (`GITHUB_TOKEN` and `DOCS_API_KEY`). A personal or experimental server
is not added here; it goes into the user scope, and `user-scope.example.json` shows the shape. The checkout of the original author was /home/dev/orders.

| Server | Scope | Exposes | Notes |
|---|---|---|---|
| `github` | project | tools: pull requests and issues | Needs `GITHUB_TOKEN`. Deleting a repository is denied in `.claude/settings.json`. |
| `docs` | project | one tool, `search_docs` | Allowed without asking. Its description is in `docs/tool-descriptions.json`. |
<!-- TODO 7 of 8 (finish this to pass e7): add the rows of `core` (a handful of tools used on every turn) and of `schema` (resources: the database schemas), both with the scope project, and under the table the line that reads the orders schema as a resource: @schema:schema://orders. Remove the personal path above for e8. -->
