# Practice: connect a team's MCP servers to Claude Code

The orders platform team wants four MCP servers in Claude Code: a GitHub server, a documentation search server, a small server that
holds the tools used on every turn, and a server that serves the database schemas. Each developer has their own tokens and a few
experiments of their own. In this practice you write the files that make that work for everyone without a secret in the repository,
and the tests read the project the way Claude Code does. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and
edit the files there. The tests are the same in all four languages and read only the files: they are configuration, not code in a
language, and the Java and Kotlin tests read the JSON with Jackson. The checks run on the course's own model of the documented rules
(`examples/55-mcp-config`, in your language). Nothing here starts Claude Code, starts a server or touches the network.

## What to write

- `.mcp.json`, the project-scoped file that is committed. Four servers, named `github`, `docs`, `core` and `schema`.
  - `github` is a remote `http` server. Its URL comes from `GITHUB_MCP_URL`, with the default `https://github-mcp.example.com/mcp`. It sends the
    header `Authorization` with the value `Bearer ${GITHUB_TOKEN}`.
  - `docs` is a local `stdio` server: `python3` with the argument `tools/docs_server.py` under the project directory. It gets `DOCS_API_KEY` in its
    environment, passed through from yours.
  - `core` is a remote `http` server with the URL from `CORE_MCP_URL` and the default `https://core-mcp.example.com/mcp`. It is the only server loaded
    at the start (`alwaysLoad`).
  - `schema` is a local `stdio` server like `docs`, running `tools/schema_server.py`.
  - A variable that is not secret gets a default (`${NAME:-default}`); a credential never does. Claude Code sets `CLAUDE_PROJECT_DIR` in the
    environment of a stdio server it starts, not in its own, so a path in `command` or `args` that uses it needs a default of `.`.
  - A credential variable must not be one that Claude Code reads as empty toward a remote server (`NPM_TOKEN` is an example).
- `.claude/settings.json`: allow every tool of `docs` and of `schema` without asking, by naming the server (`mcp__docs__*`), and deny the
  `github` tool `delete_repository`. Do not write an allow rule that does not name its server; it is ignored.
- `user-scope.example.json`: what a developer would put in `~/.claude.json` for a personal server called `scratch`: a `stdio` server that runs
  `python3` on a script under `${HOME}/experiments/`. It is an example file in the repository so that the shape is shared; the server itself is not.
  Nothing from it belongs in `.mcp.json`, and it does not reuse a shared server's name.
- `docs/tool-descriptions.json`: the `tools/list` entry for the `docs` server's one tool, `search_docs`, with a `query` string (required) and a `limit` integer,
  each with a description. The tool description fits in 2,048 characters, the point where Claude Code cuts it, so the details that matter come first: when to
  use it instead of `Grep`, what it takes, what it returns and what it does not search.
- `docs/mcp-servers.md`: a table with one row per server, giving its scope (all four are `project`) and what it exposes, and one line that shows how to read
  the orders schema as a resource with an `@` mention, `@schema:schema://orders`. The schema server is a catalog, so the row says it exposes resources.
- No file holds a personal path, an email address or a key. (The starter has one of each kind; find them.)

## Why each part is there, and what you should see

1. **The project file and the shape of each server.** The exam asks which scope shares tooling with a team and what a server entry holds. *You should see*
   four servers, remote ones with a `url` and local ones with a `command`, and the course's lint finding nothing to report.
2. **Credentials by reference.** The exam asks how to share a configuration without committing a secret. *You should see* the token expand when your
   environment has it, no default hiding a token in the file, and a credential name that is not one of those Claude Code blanks out toward a remote server.
3. **Defaults for what is not secret.** An unset variable with no default leaves the text `${VAR}` in the URL or path and the server fails. *You should
   see* every URL and path usable with an empty environment.
4. **Load at the start, or find on demand.** With tool search the definitions of the other servers wait until they are needed. *You should see* `core` with
   `alwaysLoad` and no other server with it.
5. **Scope.** *You should see* the shared servers in the project file, the personal server only in the user-scope example, and the course's resolution of both
   scopes giving five servers with no conflict.
6. **Permissions that name a server.** *You should see* the documentation and schema tools allowed, the rest of GitHub asking, the destructive tool denied.
7. **The description.** A model that has Grep in front of it prefers Grep unless the description says why not. *You should see* the boundary in the first 300
   characters and the whole text within the limit.
8. **Resources for a catalog.** *You should see* one row per server and an `@` reference that names a configured server.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The shared file declares the four team servers with the right shape |
| `e1` | Credentials come from the environment, never from the file, and not under a name Claude Code blanks out |
| `e2` | Endpoints and paths that are not secret have a default, so the file works with nothing set |
| `e3` | Only the small core server is loaded at the start |
| `e4` | Shared servers are in the project file and the personal one only in the user-scope example |
| `e5` | Permissions allow the read-only servers by name and deny the destructive tool |
| `e6` | The tool description gives the boundary against Grep early and fits the limit |
| `e7` | The notes list every server with its scope and read the catalog as a resource |
| `e8` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
