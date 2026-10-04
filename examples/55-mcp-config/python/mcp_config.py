"""MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.

A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
"""
import fnmatch
import json
import re

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
