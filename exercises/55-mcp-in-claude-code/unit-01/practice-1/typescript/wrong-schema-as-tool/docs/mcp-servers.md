# MCP servers of the orders platform

The shared servers live in `.mcp.json` at the project root and are committed. Credentials are never written in it: each one is read from
an environment variable that every developer sets in their own shell (`GITHUB_TOKEN` and `DOCS_API_KEY`). A personal or experimental server
is not added here; it goes into the user scope, and `user-scope.example.json` shows the shape.

| Server | Scope | Exposes | Notes |
|---|---|---|---|
| `github` | project | tools: pull requests and issues | Needs `GITHUB_TOKEN`. Deleting a repository is denied in `.claude/settings.json`. |
| `docs` | project | one tool, `search_docs` | Allowed without asking. Its description is in `docs/tool-descriptions.json`. |
| `core` | project | a handful of tools used on every turn | The only server loaded at the start with `alwaysLoad`; the others are found by tool search. |
| `schema` | project | tools: a schema lookup | Queried through a lookup tool. |

To read a schema, mention the resource in a prompt: `@schema:schema://orders`. Claude Code fetches it and includes it as an attachment,
so there is no exploratory sequence of calls to find the table first. Run `claude mcp list` to see which servers are connected.
