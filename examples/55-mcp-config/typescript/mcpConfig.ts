// MCP server configuration in Claude Code, resolved offline: scopes, environment expansion and a lint of a shared `.mcp.json`.
//
// A teaching model of what the Claude Code documentation says (page "Connect Claude Code to tools via MCP", read on 2026-10-03): three scopes with
// the order local, project, user, where the whole entry of the highest scope is used and fields are not merged; `${VAR}` and `${VAR:-default}`
// expanded in command, args, env, url and headers; an unset variable with no default keeps its text; and, toward a remote server, credential
// variables read as empty. The set of credential names here is the documentation's examples, not its full list. Not the product's code.
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
