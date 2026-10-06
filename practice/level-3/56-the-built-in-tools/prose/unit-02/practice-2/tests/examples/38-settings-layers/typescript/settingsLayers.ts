// Claude Code settings layers, permission rules and memory files, resolved offline.
//
// Three small functions that follow what the Claude Code documentation says (read on 2026-10-03): settings merge from five levels with
// the highest level winning a scalar key and lists combining; permission rules are checked deny, then ask, then allow, with the first
// match deciding; and CLAUDE.md files are concatenated from the broadest scope to the most specific, with @imports expanded. It is a
// teaching model of the documented behaviour, not the product's code: Read and Edit patterns use a reduced form of the gitignore rules.
import { logger } from "./example-logger.ts";
const log = logger("settings_layers");

type Json = Record<string, any>;
type Kind = "allow" | "ask" | "deny";

export const LEVELS = ["managed", "command line", "local", "project", "user"]; // highest precedence first
const REPO_LEVELS = ["project", "local"]; // files that live in the repository

export function merge(low: Json, high: Json): Json {
  const out: Json = { ...low };
  for (const [key, value] of Object.entries(high)) {
    if (value && typeof value === "object" && !Array.isArray(value) && out[key] && typeof out[key] === "object" && !Array.isArray(out[key])) out[key] = merge(out[key], value);
    else if (Array.isArray(value) && Array.isArray(out[key])) out[key] = [...out[key], ...value.filter((v) => !out[key].includes(v))];
    else out[key] = value;
  }
  return out;
}

/** Merge the five levels. A repository file cannot set defaultMode auto or bypassPermissions, and its allow rules wait for trust. */
export function effectiveSettings(layers: Record<string, Json>, trusted = true): Json {
  let result: Json = {};
  for (const level of [...LEVELS].reverse()) {
    const part: Json = {};
    for (const [k, v] of Object.entries(layers[level] ?? {})) part[k] = v && typeof v === "object" && !Array.isArray(v) ? { ...v } : v;
    const perms = part.permissions;
    if (REPO_LEVELS.includes(level) && perms) {
      if (perms.defaultMode === "auto" || perms.defaultMode === "bypassPermissions") delete perms.defaultMode;
      if (!trusted) delete perms.allow;
    }
    result = merge(result, part);
  }
  return result;
}

const escape = (s: string) => s.replace(/[.*+?^${}()|[\]\\\/-]/g, "\\$&");

function bashRegex(ruleIn: string): RegExp {
  const rule = ruleIn.endsWith(":*") ? ruleIn.slice(0, -2) + " *" : ruleIn;
  if (rule.endsWith(" *") && !rule.slice(0, -2).includes("*")) return new RegExp("^" + escape(rule.slice(0, -2)) + "(?: [\\s\\S]*)?$");
  return new RegExp("^" + rule.split("*").map(escape).join("[\\s\\S]*") + "$");
}

function glob(p: string): string {
  let out = "";
  let i = 0;
  while (i < p.length) {
    if (p.startsWith("**/", i)) { out += "(?:.*/)?"; i += 3; }
    else if (p.startsWith("**", i)) { out += ".*"; i += 2; }
    else if (p[i] === "*") { out += "[^/]*"; i += 1; }
    else { out += escape(p[i]); i += 1; }
  }
  return out;
}

function pathMatches(patternIn: string, pathIn: string, kind: Kind): boolean {
  const pattern = patternIn.replace(/^\.\//, "");
  const path = pathIn.replace(/^\.\//, "");
  if (pattern.startsWith("//")) return new RegExp("^" + glob(pattern.slice(1)) + "$").test(path);
  if (pattern.startsWith("/")) return new RegExp("^" + glob(pattern.slice(1)) + "$").test(path);
  if (!pattern.includes("/")) return new RegExp("^(?:.*/)?" + glob(pattern) + "$").test(path);
  const first = pattern.split("/")[0];
  const deep = kind !== "allow" && !first.includes("*") && !pattern.startsWith("**");
  return new RegExp("^" + (deep ? "(?:.*/)?" : "") + glob(pattern) + "$").test(path);
}

function ruleMatches(rule: string, tool: string, arg: string, kind: Kind): boolean {
  const m = /^(\w+)(?:\(([\s\S]*)\))?$/.exec(rule);
  if (!m || m[1] !== tool) return false;
  if (m[2] === undefined) return true;
  return tool === "Bash" ? bashRegex(m[2]).test(arg) : pathMatches(m[2], arg, kind);
}

/** allow, ask or deny. Deny first, then ask, then allow; a Bash call is split at && || ; | & and newlines and every part must pass.
 * In acceptEdits mode an edit that no rule decided is accepted instead of asked. */
export function decide(settings: Json, tool: string, arg = "", mode = "default"): Kind {
  const perms = settings.permissions ?? {};
  const rules: Record<Kind, string[]> = { deny: perms.deny ?? [], ask: perms.ask ?? [], allow: perms.allow ?? [] };
  const parts = tool === "Bash" ? arg.split(/&&|\|\||;|\||&|\n/).map((p) => p.trim()).filter(Boolean) : [arg];
  for (const kind of ["deny", "ask"] as Kind[]) {
    if (rules[kind].some((r) => parts.some((p) => ruleMatches(r, tool, p, kind)))) return kind;
  }
  if ((tool === "Edit" || tool === "Write") && rules.deny.some((r) => r.startsWith("Read(") && ruleMatches(r.replace("Read(", "Edit("), "Edit", arg, "deny"))) return "deny"; // a Read deny rule also blocks edits and writes on the same path
  if (parts.every((p) => rules.allow.some((r) => ruleMatches(r, tool, p, "allow")))) return "allow";
  if (tool === "Read" || tool === "Grep" || tool === "Glob") return "allow"; // read-only tools inside the working directory need no approval
  if (mode === "acceptEdits" && (tool === "Edit" || tool === "Write")) return "allow";
  return "ask"; // nothing matched: Manual mode asks
}

function join(path: string, rel: string): string {
  const stack = path.split("/").slice(0, -1);
  for (const part of rel.split("/")) {
    if (part === "..") stack.pop();
    else if (part !== ".") stack.push(part);
  }
  return stack.join("/");
}

/** The memory files in the order they reach the context: managed, user, then each directory from the root down to cwd, where
 * CLAUDE.md comes before CLAUDE.local.md. files maps a path to its text; @path lines import files (relative to the importing file,
 * at most four hops deep) and a path inside backticks is not an import. */
export function loadMemory(files: Record<string, string>, cwd: string, managed?: string, user?: string): string[] {
  const order = [managed, user].filter((p): p is string => !!p && p in files);
  const parts = cwd.replace(/^\/+|\/+$/g, "").split("/");
  for (let depth = 0; depth <= parts.length; depth++) {
    const base = ("/" + parts.slice(0, depth).join("/")).replace(/\/+$/, "");
    for (const p of [`${base}/CLAUDE.md`, `${base}/CLAUDE.local.md`]) if (p in files) order.push(p);
  }
  const loaded: string[] = [];
  const add = (path: string, hops: number) => {
    loaded.push(path);
    if (hops === 4) return;
    for (const m of files[path].replace(/`[^`]*`/g, "").matchAll(/(?<![`\w])@([\w./-]+)/g)) {
      const target = m[1].startsWith("/") ? m[1] : join(path, m[1]);
      if (target in files) add(target, hops + 1);
    }
  };
  for (const p of order) add(p, 0);
  return loaded;
}

function main() {
  const layers: Record<string, Json> = {
    managed: { permissions: { deny: ["Bash(curl *)"] } },
    user: { model: "sonnet", permissions: { allow: ["Bash(git status *)"] } },
    project: { model: "opus", permissions: { defaultMode: "bypassPermissions", allow: ["Bash(npm run *)", "Bash(curl *)"], ask: ["Bash(git push *)"], deny: ["Read(./.env)"] } },
    local: { permissions: { allow: ["Bash(git push *)"] } },
  };
  const s = effectiveSettings(layers);
  console.log("model:", s.model, "| defaultMode:", s.permissions.defaultMode ?? "(not set)");
  console.log("allow rules:", `[${s.permissions.allow.map((r: string) => `'${r}'`).join(", ")}]`);
  const calls: Array<[string, string]> = [["Bash", "npm run build"], ["Bash", "npm run build && git push origin main"], ["Bash", "curl https://example.com"],
    ["Read", "./.env"], ["Edit", ".env"], ["Bash", "git status"], ["Bash", "rm -rf build"]];
  for (const [tool, arg] of calls) console.log(`${tool}(${arg}) -> ${decide(s, tool, arg)}`);
  console.log("project not trusted yet: npm run build ->", decide(effectiveSettings(layers, false), "Bash", "npm run build"));
  const files: Record<string, string> = { "/etc/claude-code/CLAUDE.md": "managed", "/home/dev/.claude/CLAUDE.md": "user", "/repo/CLAUDE.md": "See @docs/git.md and `@README` here",
    "/repo/docs/git.md": "git rules", "/repo/svc/CLAUDE.md": "service", "/repo/svc/CLAUDE.local.md": "mine", "/repo/other/CLAUDE.md": "other" };
  const order = loadMemory(files, "/repo/svc", "/etc/claude-code/CLAUDE.md", "/home/dev/.claude/CLAUDE.md");
  console.log("memory order for /repo/svc:", `[${order.map((p) => `'${p}'`).join(", ")}]`);
}

if (import.meta.main) main();
