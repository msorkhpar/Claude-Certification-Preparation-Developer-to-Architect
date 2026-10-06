# Scopes, the project file and environment expansion

**Level:** Architect · **Module 55:** MCP in Claude Code · **Page 1 of 2**
**Exams:** A2.4; S4

**After this page you can** say which of the three scopes a server belongs in and who gets it, write a shared `.mcp.json` whose credentials come from each developer's environment and never from the file, use `${VAR:-default}` where a value is not secret, predict what happens when a variable is unset, and explain why a project's servers load without a prompt in `claude -p` and SDK runs.

Checked on 2026-10-03 against the Claude Code documentation page "Connect Claude Code to tools via MCP" (the scopes, `.mcp.json`, environment expansion and project approvals), with the Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287 the course ran. The example runs offline: it is the course's own model of the documented rules, not the product's code, and no server was started. This page deepens module 32 (what MCP is and how a server is written) and module 38 (settings layers), and it does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* servers are scoped at the project level in `.mcp.json` for shared team tooling, or at the user level in `~/.claude.json` for personal and experimental servers, and `.mcp.json` supports environment variable expansion such as `${GITHUB_TOKEN}` so that credentials are never committed. *What the current product does (documentation checked 2026-10-03):* there are three scopes, not two. Local is the default for `claude mcp add`: it loads only in the project where it was added, stays private, and is stored in `~/.claude.json` under that project's path, in the same file as the user scope. Project is `.mcp.json` at the project root, shared through version control. User is `~/.claude.json` for all your projects. When a name appears in more than one place the order is local, project, user, then plugins and connectors, and the whole entry of the winner is used with no merging of fields. Expansion also has a default form, `${VAR:-default}`, and in a remote server's `url` and `headers` credential variables read as empty. On the exam, answer project for shared and user for personal; in practice a server you add without a flag is local, so say `--scope project` when the team should have it.

## Why it matters

A server is a door into your systems, and the questions about it are about who gets the door and what is written on it. The Architect exam asks where a shared server belongs, where a personal experiment belongs, and how a token reaches a server without reaching the repository. In S4, a developer-productivity agent that "integrates with Model Context Protocol (MCP) servers", the cost of a wrong answer is a token in version control, a teammate's experiment loaded for everyone, or a shared server that fails on every machine but yours. Each of them comes from a few lines of configuration, and each is easy to test, which is why module 55's practice is a set of files with a test suite.

## The idea

### Three scopes: who loads it and who shares it

"MCP servers can be configured at three scopes." The scope controls which projects a server loads in and whether the configuration is shared.

| Scope | Loads in | Shared with the team | Stored in |
|---|---|---|---|
| Local (the default) | The current project only | No | `~/.claude.json` |
| Project | The current project only | Yes, through version control | `.mcp.json` in the project root |
| User | All your projects | No | `~/.claude.json` |

Local scope is "the default". "A local-scoped server loads only in the project where you added it and stays private to you." The documentation warns that the word means something else here than in settings: local MCP servers live in your home directory's `~/.claude.json`, while the general local settings file is `.claude/settings.local.json` in the project. Project scope is for the team: "Check `.mcp.json` into version control so everyone on your team gets the same MCP tools and services." A community guide adds the lesson that matters when a token is involved: local scope suits "Sensitive credentials for personal accounts, experimental servers under evaluation, project-specific tooling not yet ready to share", and "Selecting the wrong scope for a personal tool can leak credentials into a shared repo or, conversely, hide a tool the developer expected to see in every project." The administrator's route is separate: "Administrators can also deploy or provide servers for every user via managed configuration." User scope "works well for personal utility servers, development tools, or services you frequently use across different projects."

When the same name is defined in more than one place, "Claude Code connects to it once, using the definition from the highest-precedence source. The entire server entry from that source is used; fields are not merged across scopes." The order is local, project, user, plugin-provided servers, then claude.ai connectors. A developer who defines `github` locally with a different URL therefore replaces the team's entry for themselves, header and all; they do not inherit the team's `Authorization` line. Claude Code warns about a conflict in `claude mcp list` and in `/mcp`. A server your organisation provides through managed settings ranks above all of them.

The command line writes the file for you. `claude mcp add --transport http shared-server --scope project https://example.com/mcp` creates or updates `.mcp.json`; without `--scope` it writes the local scope. `claude mcp list` and `claude mcp get <name>` show what is configured and its status.

### The project file

`.mcp.json` has one top-level key, `mcpServers`, and an entry per server. A remote server has a `type` (`http`, `sse` or `ws`) and a `url`, and may have `headers`. A local server has a `command`, `args` and an `env` map of variables for its process; its `type` is `stdio`. The same entry shapes appear in `~/.claude.json`.

```json
{
  "mcpServers": {
    "api-server": {
      "type": "http",
      "url": "${API_BASE_URL:-https://api.example.com}/mcp",
      "headers": { "Authorization": "Bearer ${API_KEY}" }
    }
  }
}
```

### Environment expansion: credentials by reference

"Claude Code supports environment variable expansion in `.mcp.json` files, allowing teams to share configurations while maintaining flexibility for machine-specific paths and sensitive values like API keys." Two forms exist: `${VAR}` "expands to the value of environment variable `VAR`", and `${VAR:-default}` "expands to `VAR` if set, otherwise uses `default`". Expansion applies to five places: `command` (the server executable path), `args`, `env`, `url` ("for HTTP server types") and `headers` ("for HTTP server authentication"). Anywhere else the text stays as written.

Four rules decide whether a shared file works.

1. **A credential is a reference, never a value and never a default.** `Bearer ${GITHUB_TOKEN}` ships in the file and each developer exports the variable. A default such as `${GITHUB_TOKEN:-ghp_abc}` writes the secret into the repository again, so it is as wrong as a literal. A value that is not secret, such as a base URL or a path, should have a default so that the file works with nothing set.
2. **An unset variable without a default does not stop the load.** "If a referenced environment variable isn't set and has no default value, the config still loads: Claude Code reports a missing-variable warning for that server in `claude mcp list` output and uses the unexpanded `${VAR}` text as-is." The server then receives the literal text `${GITHUB_TOKEN}` and rejects it, so the symptom is an authentication failure and the first place to look is the warning in `claude mcp list`.
3. **Some credential names read as empty toward a remote server.** "In a remote server's `url` and `headers`, Claude Code reads credential variables from your environment as empty rather than expanding them." The reason is that a repository's file must not be able to send your Claude Code or cloud credentials to a server it names. The covered names are Claude Code's own credentials such as `ANTHROPIC_API_KEY`, cloud provider credentials such as `AWS_BEARER_TOKEN_BEDROCK`, and others such as `HTTPS_PROXY` and `NPM_TOKEN`. A covered name is empty whether or not you set it, and a `:-` default on it is ignored. If you write `Bearer ${NPM_TOKEN}`, the server receives `Bearer ` with nothing after it and usually answers 401. The fix is to copy the secret into a variable with a name of your own and reference that name. A name outside the set, such as `API_KEY`, "expands as written".
4. **`CLAUDE_PROJECT_DIR` needs a default in `command` and `args`.** Claude Code sets it "in the spawned server's environment to the project root", not in its own, so `${CLAUDE_PROJECT_DIR}` in a project file has nothing to expand from. The documentation's answer is a default such as `${CLAUDE_PROJECT_DIR:-.}`. The server itself can read the variable once it runs.

### Who approves a project server

A server that came with a repository is code from somebody else. "Claude Code prompts for approval in interactive sessions before using project-scoped servers from `.mcp.json` files", and until you approve it `claude mcp list` shows it as pending. A cloned repository cannot approve its own servers: approvals committed to the project's `.claude/settings.json` are ignored in a folder you have not trusted. That prompt needs a person, and an unattended run has none. "In `claude -p` runs, Agent SDK sessions, and cloud sessions, Claude Code can't show that prompt: it loads project-scoped servers without asking." So a CI job or an SDK application that opens a freshly cloned repository loads whatever its `.mcp.json` names. To keep a server out anyway, list it in `disabledMcpjsonServers`, exclude project settings with `--setting-sources` or the SDK's `settingSources`, or start the session with `--strict-mcp-config` and pass only the servers you want with `--mcp-config`. A change to `.mcp.json` in a pull request deserves the review of a change to code.

### The example

The example is a model of these rules in Python, TypeScript, Java and Kotlin: `expand` for `${VAR}`, `${VAR:-default}` and the covered credential names, `expand_server` for the five fields, `resolve_servers` for the order of scopes, `lint` for the shape of a shared file and for a literal secret, and `mcp_decision` for the permission rules of page 2. It prints the expanded entries of a shared file, a conflict between scopes, and the lint findings of a file that holds a literal key.

<!-- example: m55-mcp-config tabs: python,typescript,java,kotlin -->
```python
"""MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.

A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
"""
import fnmatch
import json
import re
import logging

log = logging.getLogger(__name__)

SCOPES = ["local", "project", "user"]  # highest precedence first
COVERED = {"ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN"}
REMOTE = ("http", "sse", "ws")
VAR = re.compile(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\}")
SECRET_KEY = re.compile(r"token|key|secret|authorization|password", re.I)
DESCRIPTION_LIMIT = 2048  # characters kept of a tool description and of a server's instructions


def expand(text, env, remote=False):
    """Expand ${VAR} and ${VAR:-default}. Returns (text, names that were unset and had no default)."""
    missing = []

    def one(m):
        name, default = m.group(1), m.group(2)
        if remote and name in COVERED:
            return ""  # never sent toward a remote server, whether or not it is set, and a default is ignored
        if name in env:
            return env[name]
        if default is not None:
            return default
        missing.append(name)
        return m.group(0)  # the unexpanded text is used as written

    return VAR.sub(one, text), missing


def expand_server(entry, env):
    """Expand the five fields where expansion applies. Returns (entry, warnings)."""
    remote = entry.get("type") in REMOTE
    warnings, out = [], dict(entry)
    for field in ("command", "url"):
        if field in out:
            out[field], miss = expand(out[field], env, remote)
            warnings += [f"{n} is not set" for n in miss]
    if "args" in out:
        out["args"] = []
        for arg in entry["args"]:
            text, miss = expand(arg, env, remote)
            out["args"].append(text)
            warnings += [f"{n} is not set" for n in miss]
    for field in ("env", "headers"):
        if field in out:
            out[field] = {}
            for key, value in entry[field].items():
                text, miss = expand(value, env, remote)
                out[field][key] = text
                warnings += [f"{n} is not set" for n in miss]
    return out, warnings


def resolve_servers(scopes):
    """scopes maps a scope name to {server name: entry}. A name defined in several scopes is used once, from the highest scope, whole."""
    servers, warnings = {}, []
    for scope in SCOPES:
        for name, entry in scopes.get(scope, {}).items():
            if name not in servers:
                servers[name] = (scope, entry)
            elif servers[name][1] != entry:
                warnings.append(f"{name} is defined in {servers[name][0]} and {scope} with different settings: {servers[name][0]} wins")
    return servers, warnings


def lint(config):
    """Findings for a shared `.mcp.json`: a server's shape and any credential written out instead of referenced."""
    findings = []
    for name, entry in config.get("mcpServers", {}).items():
        kind = entry.get("type", "stdio")
        if kind in REMOTE and "url" not in entry:
            findings.append(f"{name}: a {kind} server needs a url")
        if kind == "stdio" and "command" not in entry:
            findings.append(f"{name}: a stdio server needs a command")
        for field in ("env", "headers"):
            for key, value in entry.get(field, {}).items():
                if SECRET_KEY.search(key) and "${" not in value:
                    findings.append(f"{name}: {field}.{key} holds a literal value, reference an environment variable")
    return findings


def mcp_decision(settings, tool):
    """allow, ask or deny for an MCP tool named mcp__<server>__<tool>. Deny rules may use globs; an allow rule counts only when the
    server part is written out and glob-free (mcp__docs__* is honoured, mcp__* and * are ignored). Deny wins, then allow, else ask."""
    perms = settings.get("permissions", {})
    if any(fnmatch.fnmatchcase(tool, rule) for rule in perms.get("deny", [])):
        return "deny"
    for rule in perms.get("allow", []):
        parts = rule.split("__")
        if len(parts) >= 3 and parts[0] == "mcp" and parts[1] and "*" not in parts[1] and fnmatch.fnmatchcase(tool, rule):
            return "allow"
    return "ask"


def truncate(text, limit=DESCRIPTION_LIMIT):
    return text[:limit]


def show(value):
    return json.dumps(value, separators=(",", ":"))


def main():
    env = {"GITHUB_TOKEN": "demo-gh", "NPM_TOKEN": "demo-npm"}
    shared = {"mcpServers": {
        "github": {"type": "http", "url": "${GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}", "headers": {"Authorization": "Bearer ${GITHUB_TOKEN}"}},
        "registry": {"type": "http", "url": "https://registry-mcp.example.com/mcp", "headers": {"Authorization": "Bearer ${NPM_TOKEN}"}},
        "docs": {"command": "python3", "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py", "${DOCS_INDEX}"], "env": {"DOCS_API_KEY": "${DOCS_API_KEY}"}},
    }}
    print("expanded entries:")
    for name, entry in shared["mcpServers"].items():
        out, warnings = expand_server(entry, env)
        print(f"  {name}: {show(out)}")
        for w in warnings:
            print(f"    warning: {w}")
    scopes = {"project": shared["mcpServers"], "user": {"github": {"type": "http", "url": "https://other.example.com/mcp"}, "scratch": {"command": "python3", "args": ["scratch.py"]}}}
    servers, warnings = resolve_servers(scopes)
    print("resolved servers:", ", ".join(f"{n} from {s}" for n, (s, _) in servers.items()))
    for w in warnings:
        print("  warning:", w)
    bad = {"mcpServers": {"api": {"type": "http", "headers": {"X-Api-Key": "abc123"}}, "tool": {"args": ["x"]}}}
    print("lint of a shared file with literal secrets:")
    for f in lint(bad):
        print("  " + f)
    print("lint of the file above:", lint(shared) or "no findings")
    settings = {"permissions": {"allow": ["mcp__docs__*", "mcp__*"], "deny": ["mcp__github__delete_*"]}}
    for tool in ("mcp__docs__search_docs", "mcp__github__list_prs", "mcp__github__delete_repository"):
        print(f"{tool} ->", mcp_decision(settings, tool))
    print("a description of 3000 characters keeps", len(truncate("x" * 3000)), "of them")


if __name__ == "__main__":
    main()
```
```text
expanded entries:
  github: {"type":"http","url":"https://github-mcp.example.com/mcp","headers":{"Authorization":"Bearer demo-gh"}}
  registry: {"type":"http","url":"https://registry-mcp.example.com/mcp","headers":{"Authorization":"Bearer "}}
  docs: {"command":"python3","args":["./tools/docs_server.py","${DOCS_INDEX}"],"env":{"DOCS_API_KEY":"${DOCS_API_KEY}"}}
    warning: DOCS_INDEX is not set
    warning: DOCS_API_KEY is not set
resolved servers: github from project, registry from project, docs from project, scratch from user
  warning: github is defined in project and user with different settings: project wins
lint of a shared file with literal secrets:
  api: a http server needs a url
  api: headers.X-Api-Key holds a literal value, reference an environment variable
  tool: a stdio server needs a command
lint of the file above: no findings
mcp__docs__search_docs -> allow
mcp__github__list_prs -> ask
mcp__github__delete_repository -> deny
a description of 3000 characters keeps 2048 of them
```
```typescript
// MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.
//
// A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
// the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
// expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
// variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
import { logger } from "./logger.ts";
const log = logger("mcp_config");
export const SCOPES = ["local", "project", "user"]; // highest precedence first
const COVERED = new Set(["ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN"]);
const REMOTE = ["http", "sse", "ws"];
const SECRET_KEY = /token|key|secret|authorization|password/i;
const DESCRIPTION_LIMIT = 2048; // characters kept of a tool description and of a server's instructions

type Entry = Record<string, any>;

/** Expand ${VAR} and ${VAR:-default}. Returns [text, names that were unset and had no default]. */
export function expand(text: string, env: Record<string, string>, remote = false): [string, string[]] {
  const missing: string[] = [];
  const out = text.replace(/\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\}/g, (whole, name: string, fallback?: string) => {
    if (remote && COVERED.has(name)) return ""; // never sent toward a remote server, whether or not it is set, and a default is ignored
    if (name in env) return env[name];
    if (fallback !== undefined) return fallback;
    missing.push(name);
    return whole; // the unexpanded text is used as written
  });
  return [out, missing];
}

/** Expand the five fields where expansion applies. Returns [entry, warnings]. */
export function expandServer(entry: Entry, env: Record<string, string>): [Entry, string[]] {
  const remote = REMOTE.includes(entry.type);
  const warnings: string[] = [];
  const out: Entry = { ...entry };
  const one = (text: string) => {
    const [value, miss] = expand(text, env, remote);
    warnings.push(...miss.map((n) => `${n} is not set`));
    return value;
  };
  for (const field of ["command", "url"]) if (field in out) out[field] = one(out[field]);
  if ("args" in out) out.args = entry.args.map((a: string) => one(a));
  for (const field of ["env", "headers"]) {
    if (field in out) out[field] = Object.fromEntries(Object.entries(entry[field] as Record<string, string>).map(([k, v]) => [k, one(v)]));
  }
  return [out, warnings];
}

/** scopes maps a scope name to {server name: entry}. A name defined in several scopes is used once, from the highest scope, whole. */
export function resolveServers(scopes: Record<string, Record<string, Entry>>): [Record<string, [string, Entry]>, string[]] {
  const servers: Record<string, [string, Entry]> = {};
  const warnings: string[] = [];
  for (const scope of SCOPES) {
    for (const [name, entry] of Object.entries(scopes[scope] ?? {})) {
      if (!(name in servers)) servers[name] = [scope, entry];
      else if (JSON.stringify(servers[name][1]) !== JSON.stringify(entry)) {
        warnings.push(`${name} is defined in ${servers[name][0]} and ${scope} with different settings: ${servers[name][0]} wins`);
      }
    }
  }
  return [servers, warnings];
}

/** Findings for a shared `.mcp.json`: a server's shape and any credential written out instead of referenced. */
export function lint(config: Entry): string[] {
  const findings: string[] = [];
  for (const [name, entry] of Object.entries<Entry>(config.mcpServers ?? {})) {
    const kind = entry.type ?? "stdio";
    if (REMOTE.includes(kind) && !("url" in entry)) findings.push(`${name}: a ${kind} server needs a url`);
    if (kind === "stdio" && !("command" in entry)) findings.push(`${name}: a stdio server needs a command`);
    for (const field of ["env", "headers"]) {
      for (const [key, value] of Object.entries<string>(entry[field] ?? {})) {
        if (SECRET_KEY.test(key) && !value.includes("${")) findings.push(`${name}: ${field}.${key} holds a literal value, reference an environment variable`);
      }
    }
  }
  return findings;
}

/** allow, ask or deny for an MCP tool named mcp__<server>__<tool>. Deny rules may use globs; an allow rule counts only when the
 * server part is written out and glob-free (mcp__docs__* is honoured, mcp__* and * are ignored). Deny wins, then allow, else ask. */
const globMatch = (rule: string, name: string) => new RegExp("^" + rule.split("*").map((s) => s.replace(/[.+?^${}()|[\]\\]/g, "\\$&")).join(".*") + "$").test(name);
export function mcpDecision(settings: Entry, tool: string): string {
  const perms = settings.permissions ?? {};
  if ((perms.deny ?? []).some((rule: string) => globMatch(rule, tool))) return "deny";
  for (const rule of perms.allow ?? []) {
    const parts = rule.split("__");
    if (parts.length >= 3 && parts[0] === "mcp" && parts[1] && !parts[1].includes("*") && globMatch(rule, tool)) return "allow";
  }
  return "ask";
}

export function truncate(text: string, limit = DESCRIPTION_LIMIT): string {
  return text.slice(0, limit);
}

const show = (value: unknown) => JSON.stringify(value);

function main() {
  const env = { GITHUB_TOKEN: "demo-gh", NPM_TOKEN: "demo-npm" };
  const shared: Entry = { mcpServers: {
    github: { type: "http", url: "${GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}", headers: { Authorization: "Bearer ${GITHUB_TOKEN}" } },
    registry: { type: "http", url: "https://registry-mcp.example.com/mcp", headers: { Authorization: "Bearer ${NPM_TOKEN}" } },
    docs: { command: "python3", args: ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py", "${DOCS_INDEX}"], env: { DOCS_API_KEY: "${DOCS_API_KEY}" } },
  } };
  console.log("expanded entries:");
  for (const [name, entry] of Object.entries<Entry>(shared.mcpServers)) {
    const [out, warnings] = expandServer(entry, env);
    console.log(`  ${name}: ${show(out)}`);
    for (const w of warnings) console.log(`    warning: ${w}`);
  }
  const scopes = { project: shared.mcpServers, user: { github: { type: "http", url: "https://other.example.com/mcp" }, scratch: { command: "python3", args: ["scratch.py"] } } };
  const [servers, warnings] = resolveServers(scopes);
  console.log("resolved servers:", Object.entries(servers).map(([n, [s]]) => `${n} from ${s}`).join(", "));
  for (const w of warnings) console.log("  warning:", w);
  const bad = { mcpServers: { api: { type: "http", headers: { "X-Api-Key": "abc123" } }, tool: { args: ["x"] } } };
  console.log("lint of a shared file with literal secrets:");
  for (const f of lint(bad)) console.log("  " + f);
  const clean = lint(shared);
  console.log("lint of the file above:", clean.length ? clean : "no findings");
  const settings = { permissions: { allow: ["mcp__docs__*", "mcp__*"], deny: ["mcp__github__delete_*"] } };
  for (const tool of ["mcp__docs__search_docs", "mcp__github__list_prs", "mcp__github__delete_repository"]) console.log(`${tool} ->`, mcpDecision(settings, tool));
  console.log("a description of 3000 characters keeps", truncate("x".repeat(3000)).length, "of them");
}

if (import.meta.main) main();
```
```text
expanded entries:
  github: {"type":"http","url":"https://github-mcp.example.com/mcp","headers":{"Authorization":"Bearer demo-gh"}}
  registry: {"type":"http","url":"https://registry-mcp.example.com/mcp","headers":{"Authorization":"Bearer "}}
  docs: {"command":"python3","args":["./tools/docs_server.py","${DOCS_INDEX}"],"env":{"DOCS_API_KEY":"${DOCS_API_KEY}"}}
    warning: DOCS_INDEX is not set
    warning: DOCS_API_KEY is not set
resolved servers: github from project, registry from project, docs from project, scratch from user
  warning: github is defined in project and user with different settings: project wins
lint of a shared file with literal secrets:
  api: a http server needs a url
  api: headers.X-Api-Key holds a literal value, reference an environment variable
  tool: a stdio server needs a command
lint of the file above: no findings
mcp__docs__search_docs -> allow
mcp__github__list_prs -> ask
mcp__github__delete_repository -> deny
a description of 3000 characters keeps 2048 of them
```
```java
import static harness.Show.py;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.
 *
 * <p>A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
 * the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
 * expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
 * variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
 * The configuration is JSON, read with Jackson.
 */
public final class McpConfig {
    private static final System.Logger LOG = System.getLogger(McpConfig.class.getName());
    static final List<String> SCOPES = List.of("local", "project", "user"); // highest precedence first
    static final Set<String> COVERED = Set.of("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN");
    static final Set<String> REMOTE = Set.of("http", "sse", "ws");
    static final Pattern VAR = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\\}");
    static final Pattern SECRET_KEY = Pattern.compile("token|key|secret|authorization|password", Pattern.CASE_INSENSITIVE);
    static final int DESCRIPTION_LIMIT = 2048; // characters kept of a tool description and of a server's instructions
    private static final ObjectMapper JSON = new ObjectMapper();

    /** The text after expansion and the names that were unset and had no default. */
    record Expanded(String text, List<String> missing) {}

    /** A server entry after expansion, and the warnings it raised. */
    record ExpandedServer(Map<String, Object> entry, List<String> warnings) {}

    /** A server's winning scope and entry. */
    record Source(String scope, Map<String, Object> entry) {}

    /** The servers in use, and the warnings about conflicts. */
    record Resolved(Map<String, Source> servers, List<String> warnings) {}

    static Map<String, Object> parse(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = JSON.readValue(json, LinkedHashMap.class);
            return m;
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /** Expand ${VAR} and ${VAR:-default}. Returns the text and the names that were unset and had no default. */
    static Expanded expand(String text, Map<String, String> env, boolean remote) {
        List<String> missing = new ArrayList<>();
        Matcher m = VAR.matcher(text);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String name = m.group(1), dflt = m.group(2);
            String value;
            if (remote && COVERED.contains(name)) value = ""; // never sent toward a remote server, whether or not it is set, and a default is ignored
            else if (env.containsKey(name)) value = env.get(name);
            else if (dflt != null) value = dflt;
            else {
                missing.add(name);
                value = m.group(0); // the unexpanded text is used as written
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        return new Expanded(out.toString(), missing);
    }

    static Expanded expand(String text, Map<String, String> env) {
        return expand(text, env, false);
    }

    private static void warn(List<String> warnings, List<String> missing) {
        for (String n : missing) warnings.add(n + " is not set");
    }

    /** Expand the five fields where expansion applies. Returns the entry and the warnings. */
    @SuppressWarnings("unchecked")
    static ExpandedServer expandServer(Map<String, Object> entry, Map<String, String> env) {
        boolean remote = entry.get("type") != null && REMOTE.contains(entry.get("type"));
        List<String> warnings = new ArrayList<>();
        Map<String, Object> out = new LinkedHashMap<>(entry);
        for (String field : List.of("command", "url")) {
            if (!out.containsKey(field)) continue;
            Expanded e = expand((String) out.get(field), env, remote);
            out.put(field, e.text());
            warn(warnings, e.missing());
        }
        if (out.containsKey("args")) {
            List<String> args = new ArrayList<>();
            for (Object arg : (List<Object>) entry.get("args")) {
                Expanded e = expand((String) arg, env, remote);
                args.add(e.text());
                warn(warnings, e.missing());
            }
            out.put("args", args);
        }
        for (String field : List.of("env", "headers")) {
            if (!out.containsKey(field)) continue;
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, Object> kv : ((Map<String, Object>) entry.get(field)).entrySet()) {
                Expanded e = expand((String) kv.getValue(), env, remote);
                values.put(kv.getKey(), e.text());
                warn(warnings, e.missing());
            }
            out.put(field, values);
        }
        return new ExpandedServer(out, warnings);
    }

    /** scopes maps a scope name to {server name: entry}. A name defined in several scopes is used once, from the highest scope, whole. */
    static Resolved resolveServers(Map<String, Map<String, Map<String, Object>>> scopes) {
        Map<String, Source> servers = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        for (String scope : SCOPES) {
            for (Map.Entry<String, Map<String, Object>> e : scopes.getOrDefault(scope, Map.of()).entrySet()) {
                String name = e.getKey();
                if (!servers.containsKey(name)) servers.put(name, new Source(scope, e.getValue()));
                else if (!servers.get(name).entry().equals(e.getValue())) {
                    warnings.add(name + " is defined in " + servers.get(name).scope() + " and " + scope + " with different settings: " + servers.get(name).scope() + " wins");
                }
            }
        }
        return new Resolved(servers, warnings);
    }

    /** Findings for a shared `.mcp.json`: a server's shape and any credential written out instead of referenced. */
    @SuppressWarnings("unchecked")
    static List<String> lint(Map<String, Object> config) {
        List<String> findings = new ArrayList<>();
        Map<String, Object> servers = (Map<String, Object>) config.getOrDefault("mcpServers", Map.of());
        for (Map.Entry<String, Object> s : servers.entrySet()) {
            String name = s.getKey();
            Map<String, Object> entry = (Map<String, Object>) s.getValue();
            String kind = (String) entry.getOrDefault("type", "stdio");
            if (REMOTE.contains(kind) && !entry.containsKey("url")) findings.add(name + ": a " + kind + " server needs a url");
            if (kind.equals("stdio") && !entry.containsKey("command")) findings.add(name + ": a stdio server needs a command");
            for (String field : List.of("env", "headers")) {
                for (Map.Entry<String, Object> kv : ((Map<String, Object>) entry.getOrDefault(field, Map.of())).entrySet()) {
                    if (SECRET_KEY.matcher(kv.getKey()).find() && !((String) kv.getValue()).contains("${")) {
                        findings.add(name + ": " + field + "." + kv.getKey() + " holds a literal value, reference an environment variable");
                    }
                }
            }
        }
        return findings;
    }

    /** Python's fnmatch.fnmatchcase: `*` any run, `?` one character, `[...]` a set. */
    static boolean fnmatch(String name, String pattern) {
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*') re.append(".*");
            else if (c == '?') re.append('.');
            else if (c == '[') {
                int j = pattern.indexOf(']', i + 2);
                if (j < 0) re.append("\\[");
                else {
                    String set = pattern.substring(i + 1, j);
                    re.append('[').append(set.startsWith("!") ? "^" + set.substring(1) : set).append(']');
                    i = j;
                }
            } else re.append(Pattern.quote(String.valueOf(c)));
        }
        return Pattern.compile(re.toString(), Pattern.DOTALL).matcher(name).matches();
    }

    /**
     * allow, ask or deny for an MCP tool named mcp__&lt;server&gt;__&lt;tool&gt;. Deny rules may use globs; an allow rule counts only when the
     * server part is written out and glob-free (mcp__docs__* is honoured, mcp__* and * are ignored). Deny wins, then allow, else ask.
     */
    @SuppressWarnings("unchecked")
    static String mcpDecision(Map<String, Object> settings, String tool) {
        Map<String, Object> perms = (Map<String, Object>) settings.getOrDefault("permissions", Map.of());
        for (Object rule : (List<Object>) perms.getOrDefault("deny", List.of())) if (fnmatch(tool, (String) rule)) return "deny";
        for (Object r : (List<Object>) perms.getOrDefault("allow", List.of())) {
            String rule = (String) r;
            String[] parts = rule.split("__", -1);
            if (parts.length >= 3 && parts[0].equals("mcp") && !parts[1].isEmpty() && !parts[1].contains("*") && fnmatch(tool, rule)) return "allow";
        }
        return "ask";
    }

    static String truncate(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    static String truncate(String text) {
        return truncate(text, DESCRIPTION_LIMIT);
    }

    static String show(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        Map<String, String> env = Map.of("GITHUB_TOKEN", "demo-gh", "NPM_TOKEN", "demo-npm");
        Map<String, Object> shared = parse("""
            {"mcpServers": {
              "github": {"type": "http", "url": "${GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}", "headers": {"Authorization": "Bearer ${GITHUB_TOKEN}"}},
              "registry": {"type": "http", "url": "https://registry-mcp.example.com/mcp", "headers": {"Authorization": "Bearer ${NPM_TOKEN}"}},
              "docs": {"command": "python3", "args": ["${CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py", "${DOCS_INDEX}"], "env": {"DOCS_API_KEY": "${DOCS_API_KEY}"}}
            }}""");
        Map<String, Map<String, Object>> sharedServers = (Map<String, Map<String, Object>>) (Object) shared.get("mcpServers");
        System.out.println("expanded entries:");
        for (Map.Entry<String, Map<String, Object>> s : sharedServers.entrySet()) {
            ExpandedServer out = expandServer(s.getValue(), env);
            System.out.println("  " + s.getKey() + ": " + show(out.entry()));
            for (String w : out.warnings()) System.out.println("    warning: " + w);
        }
        Map<String, Map<String, Object>> user = (Map<String, Map<String, Object>>) (Object) parse("""
            {"github": {"type": "http", "url": "https://other.example.com/mcp"}, "scratch": {"command": "python3", "args": ["scratch.py"]}}""");
        Resolved resolved = resolveServers(Map.of("project", sharedServers, "user", user));
        List<String> parts = new ArrayList<>();
        resolved.servers().forEach((n, s) -> parts.add(n + " from " + s.scope()));
        System.out.println("resolved servers: " + String.join(", ", parts));
        for (String w : resolved.warnings()) System.out.println("  warning: " + w);
        Map<String, Object> bad = parse("""
            {"mcpServers": {"api": {"type": "http", "headers": {"X-Api-Key": "abc123"}}, "tool": {"args": ["x"]}}}""");
        System.out.println("lint of a shared file with literal secrets:");
        for (String f : lint(bad)) System.out.println("  " + f);
        List<String> clean = lint(shared);
        System.out.println("lint of the file above: " + (clean.isEmpty() ? "no findings" : py(clean)));
        Map<String, Object> settings = parse("""
            {"permissions": {"allow": ["mcp__docs__*", "mcp__*"], "deny": ["mcp__github__delete_*"]}}""");
        for (String tool : List.of("mcp__docs__search_docs", "mcp__github__list_prs", "mcp__github__delete_repository")) {
            System.out.println(tool + " -> " + mcpDecision(settings, tool));
        }
        System.out.println("a description of 3000 characters keeps " + truncate("x".repeat(3000)).length() + " of them");
    }
}
```
```text
expanded entries:
  github: {"type":"http","url":"https://github-mcp.example.com/mcp","headers":{"Authorization":"Bearer demo-gh"}}
  registry: {"type":"http","url":"https://registry-mcp.example.com/mcp","headers":{"Authorization":"Bearer "}}
  docs: {"command":"python3","args":["./tools/docs_server.py","${DOCS_INDEX}"],"env":{"DOCS_API_KEY":"${DOCS_API_KEY}"}}
    warning: DOCS_INDEX is not set
    warning: DOCS_API_KEY is not set
resolved servers: github from project, registry from project, docs from project, scratch from user
  warning: github is defined in project and user with different settings: project wins
lint of a shared file with literal secrets:
  api: a http server needs a url
  api: headers.X-Api-Key holds a literal value, reference an environment variable
  tool: a stdio server needs a command
lint of the file above: no findings
mcp__docs__search_docs -> allow
mcp__github__list_prs -> ask
mcp__github__delete_repository -> deny
a description of 3000 characters keeps 2048 of them
```
```kotlin
import com.fasterxml.jackson.databind.ObjectMapper
import harness.Show.py

private val log = System.getLogger("mcp_config")

/**
 * MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.
 *
 * A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
 * the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
 * expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
 * variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
 * The configuration is JSON, read with Jackson.
 */
val SCOPES = listOf("local", "project", "user") // highest precedence first
val COVERED = setOf("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN")
val REMOTE = setOf("http", "sse", "ws")
val VAR = Regex("""\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-([^}]*))?\}""")
val SECRET_KEY = Regex("token|key|secret|authorization|password", RegexOption.IGNORE_CASE)
const val DESCRIPTION_LIMIT = 2048 // characters kept of a tool description and of a server's instructions
private val JSON = ObjectMapper()

typealias Entry = Map<String, Any?>

/** The text after expansion and the names that were unset and had no default. */
data class Expanded(val text: String, val missing: List<String>)

/** A server entry after expansion, and the warnings it raised. */
data class ExpandedServer(val entry: Entry, val warnings: List<String>)

/** A server's winning scope and entry. */
data class Source(val scope: String, val entry: Entry)

/** The servers in use, and the warnings about conflicts. */
data class Resolved(val servers: Map<String, Source>, val warnings: List<String>)

@Suppress("UNCHECKED_CAST")
fun parse(json: String): Map<String, Any?> = JSON.readValue(json, LinkedHashMap::class.java) as Map<String, Any?>

/** Expand ${VAR} and ${VAR:-default}. Returns the text and the names that were unset and had no default. */
fun expand(text: String, env: Map<String, String>, remote: Boolean = false): Expanded {
    val missing = mutableListOf<String>()
    val out = VAR.replace(text) { m ->
        val name = m.groupValues[1]
        val default = m.groups[2]?.value
        when {
            remote && name in COVERED -> "" // never sent toward a remote server, whether or not it is set, and a default is ignored
            name in env -> env.getValue(name)
            default != null -> default
            else -> {
                missing += name
                m.value // the unexpanded text is used as written
            }
        }
    }
    return Expanded(out, missing)
}

/** Expand the five fields where expansion applies. Returns the entry and the warnings. */
fun expandServer(entry: Entry, env: Map<String, String>): ExpandedServer {
    val remote = entry["type"] in REMOTE
    val warnings = mutableListOf<String>()
    val out = LinkedHashMap(entry)
    fun one(value: Any?): String {
        val e = expand(value as String, env, remote)
        e.missing.forEach { warnings += "$it is not set" }
        return e.text
    }
    for (field in listOf("command", "url")) if (field in out) out[field] = one(out[field])
    if ("args" in out) out["args"] = (entry["args"] as List<*>).map { one(it) }
    for (field in listOf("env", "headers")) if (field in out) out[field] = (entry[field] as Map<*, *>).entries.associate { (k, v) -> k as String to one(v) }
    return ExpandedServer(out, warnings)
}

/** scopes maps a scope name to {server name: entry}. A name defined in several scopes is used once, from the highest scope, whole. */
fun resolveServers(scopes: Map<String, Map<String, Entry>>): Resolved {
    val servers = linkedMapOf<String, Source>()
    val warnings = mutableListOf<String>()
    for (scope in SCOPES) {
        for ((name, entry) in scopes[scope] ?: emptyMap()) {
            val seen = servers[name]
            if (seen == null) servers[name] = Source(scope, entry)
            else if (seen.entry != entry) warnings += "$name is defined in ${seen.scope} and $scope with different settings: ${seen.scope} wins"
        }
    }
    return Resolved(servers, warnings)
}

/** Findings for a shared `.mcp.json`: a server's shape and any credential written out instead of referenced. */
fun lint(config: Entry): List<String> {
    val findings = mutableListOf<String>()
    for ((name, e) in (config["mcpServers"] as Map<*, *>? ?: emptyMap<String, Any?>())) {
        val entry = e as Map<*, *>
        val kind = entry["type"] as String? ?: "stdio"
        if (kind in REMOTE && "url" !in entry) findings += "$name: a $kind server needs a url"
        if (kind == "stdio" && "command" !in entry) findings += "$name: a stdio server needs a command"
        for (field in listOf("env", "headers")) {
            for ((key, value) in (entry[field] as Map<*, *>? ?: emptyMap<String, Any?>())) {
                if (SECRET_KEY.containsMatchIn(key as String) && "\${" !in (value as String)) findings += "$name: $field.$key holds a literal value, reference an environment variable"
            }
        }
    }
    return findings
}

/** Python's fnmatch.fnmatchcase: `*` any run, `?` one character, `[...]` a set. */
fun fnmatch(name: String, pattern: String): Boolean {
    val re = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        val c = pattern[i]
        when {
            c == '*' -> re.append(".*")
            c == '?' -> re.append('.')
            c == '[' && pattern.indexOf(']', i + 2) >= 0 -> {
                val j = pattern.indexOf(']', i + 2)
                val set = pattern.substring(i + 1, j)
                re.append('[').append(if (set.startsWith("!")) "^" + set.substring(1) else set).append(']')
                i = j
            }
            else -> re.append(Regex.escape(c.toString()))
        }
        i++
    }
    return Regex(re.toString(), RegexOption.DOT_MATCHES_ALL).matches(name)
}

/**
 * allow, ask or deny for an MCP tool named mcp__<server>__<tool>. Deny rules may use globs; an allow rule counts only when the
 * server part is written out and glob-free (mcp__docs__* is honoured, mcp__* and * are ignored). Deny wins, then allow, else ask.
 */
fun mcpDecision(settings: Entry, tool: String): String {
    val perms = settings["permissions"] as Map<*, *>? ?: emptyMap<String, Any?>()
    if ((perms["deny"] as List<*>? ?: emptyList<String>()).any { fnmatch(tool, it as String) }) return "deny"
    for (rule in (perms["allow"] as List<*>? ?: emptyList<String>())) {
        val parts = (rule as String).split("__")
        if (parts.size >= 3 && parts[0] == "mcp" && parts[1].isNotEmpty() && "*" !in parts[1] && fnmatch(tool, rule)) return "allow"
    }
    return "ask"
}

fun truncate(text: String, limit: Int = DESCRIPTION_LIMIT): String = text.take(limit)

fun show(value: Any?): String = JSON.writeValueAsString(value)

@Suppress("UNCHECKED_CAST")
fun main() {
    val env = mapOf("GITHUB_TOKEN" to "demo-gh", "NPM_TOKEN" to "demo-npm")
    val shared = parse(
        """
        {"mcpServers": {
          "github": {"type": "http", "url": "${'$'}{GITHUB_MCP_URL:-https://github-mcp.example.com/mcp}", "headers": {"Authorization": "Bearer ${'$'}{GITHUB_TOKEN}"}},
          "registry": {"type": "http", "url": "https://registry-mcp.example.com/mcp", "headers": {"Authorization": "Bearer ${'$'}{NPM_TOKEN}"}},
          "docs": {"command": "python3", "args": ["${'$'}{CLAUDE_PROJECT_DIR:-.}/tools/docs_server.py", "${'$'}{DOCS_INDEX}"], "env": {"DOCS_API_KEY": "${'$'}{DOCS_API_KEY}"}}
        }}
        """,
    )
    val sharedServers = shared["mcpServers"] as Map<String, Entry>
    println("expanded entries:")
    for ((name, entry) in sharedServers) {
        val out = expandServer(entry, env)
        println("  $name: ${show(out.entry)}")
        out.warnings.forEach { println("    warning: $it") }
    }
    val user = parse("""{"github": {"type": "http", "url": "https://other.example.com/mcp"}, "scratch": {"command": "python3", "args": ["scratch.py"]}}""") as Map<String, Entry>
    val resolved = resolveServers(mapOf("project" to sharedServers, "user" to user))
    println("resolved servers: " + resolved.servers.entries.joinToString(", ") { (n, s) -> "$n from ${s.scope}" })
    resolved.warnings.forEach { println("  warning: $it") }
    val bad = parse("""{"mcpServers": {"api": {"type": "http", "headers": {"X-Api-Key": "abc123"}}, "tool": {"args": ["x"]}}}""")
    println("lint of a shared file with literal secrets:")
    lint(bad).forEach { println("  $it") }
    val clean = lint(shared)
    println("lint of the file above: ${if (clean.isEmpty()) "no findings" else py(clean)}")
    val settings = parse("""{"permissions": {"allow": ["mcp__docs__*", "mcp__*"], "deny": ["mcp__github__delete_*"]}}""")
    for (tool in listOf("mcp__docs__search_docs", "mcp__github__list_prs", "mcp__github__delete_repository")) println("$tool -> ${mcpDecision(settings, tool)}")
    println("a description of 3000 characters keeps ${truncate("x".repeat(3000)).length} of them")
}
```
```text
expanded entries:
  github: {"type":"http","url":"https://github-mcp.example.com/mcp","headers":{"Authorization":"Bearer demo-gh"}}
  registry: {"type":"http","url":"https://registry-mcp.example.com/mcp","headers":{"Authorization":"Bearer "}}
  docs: {"command":"python3","args":["./tools/docs_server.py","${DOCS_INDEX}"],"env":{"DOCS_API_KEY":"${DOCS_API_KEY}"}}
    warning: DOCS_INDEX is not set
    warning: DOCS_API_KEY is not set
resolved servers: github from project, registry from project, docs from project, scratch from user
  warning: github is defined in project and user with different settings: project wins
lint of a shared file with literal secrets:
  api: a http server needs a url
  api: headers.X-Api-Key holds a literal value, reference an environment variable
  tool: a stdio server needs a command
lint of the file above: no findings
mcp__docs__search_docs -> allow
mcp__github__list_prs -> ask
mcp__github__delete_repository -> deny
a description of 3000 characters keeps 2048 of them
```
<!-- /example -->

Read the output from the top. The `github` entry gets its default URL and the token from the environment. The `registry` entry shows the covered name at work: the header is `Bearer ` and nothing more, although `NPM_TOKEN` is set. The `docs` entry keeps `${DOCS_INDEX}` and `${DOCS_API_KEY}` as written and reports both as unset, while `${CLAUDE_PROJECT_DIR:-.}` becomes `.`. The user scope's `github` loses to the project's, with a warning, and its whole entry is ignored. The lint of the second file flags a missing URL, a missing command and a header whose name says "key" and whose value is a literal.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Add the experimental server to `.mcp.json`; teammates can ignore it."** It is tempting because `.mcp.json` is where MCP configuration lives. The exam rejects it: a project server is shared, so every teammate and every unattended run loads it. A personal or experimental server goes in the user scope.
2. **"Put the token in `.mcp.json` and add the file to `.gitignore`."** It is tempting because the secret then stays off the remote. The exam rejects it: the file exists to be committed so that the team gets the same tools. Reference `${GITHUB_TOKEN}` and let each developer set it.
3. **"Give the variable a default so the server starts for everyone."** It is tempting because an unset variable produces a warning. The exam rejects it for credentials: a default for a token is a token in the repository. Defaults are for URLs and paths.
4. **"A user entry can add a header to the team's server."** It is tempting because settings files merge their lists. The exam rejects it: for servers "fields are not merged across scopes", and the whole entry of the highest scope wins.
5. **"The user-level file is `~/.claude.json`, so the default scope is the user scope."** It is tempting because the guide names that file for personal servers. The exam keys user for personal, but the default of `claude mcp add` is local, which is stored in the same file under the project's path and loads only there.

## Quiz

1. A team commits `.mcp.json` with the header `Bearer ${NPM_TOKEN}` for a hosted package registry. Every developer exports the variable, yet the registry answers 401. What is the cause?
   - **a**: The file loads only after an interactive approval that the developers skipped
   - **b**: Header values are expanded only for entries that start a local process
   - **c**: Claude Code reads names of this kind, which carry sign-in secrets, as empty
   - **d**: A variable in a header needs a fallback after the colon and dash before use

2. A developer needs an integration that signs in with a personal account's token. It should exist in this repository only, and the token must never reach teammates through version control. Where should it be registered?
   - **a**: Local scope, the default for a newly added server
   - **b**: Project scope, kept in the shared `.mcp.json` file
   - **c**: User scope, which loads the server in every project
   - **d**: The local settings file, kept inside the repository

<details>
<summary>Answer key</summary>

1. **c**. Credential names such as this one read as empty toward a remote endpoint. *a* is ruled out because a server that waits for approval is not connected at all: "claude mcp list shows it as pending", and it does not answer with a 401. *b* is ruled out because the documentation lists the places where expansion applies: `headers` ("for HTTP server authentication"). *d* is ruled out because a default is optional, and the plain form works: "expands to the value of environment variable".
2. **a**. Local scope is the default and belongs to its owner and that one project. The guide names user-level scope for personal servers, and the product has a third scope between the two, which is the one that fits a single repository. *b* is ruled out because "Selecting the wrong scope for a personal tool can leak credentials into a shared repo". *c* is ruled out because user scope is for "services you frequently use across different projects", which is wider than this repository. *d* is ruled out because that file holds settings and not servers: "while the general local settings file is `.claude/settings.local.json` in the project".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
