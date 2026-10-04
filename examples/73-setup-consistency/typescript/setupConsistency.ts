// Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.
//
// The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
// documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
// mcp__<server>__<tool> in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
// .claude/agents/. Nothing here starts Claude Code or an MCP server.
import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

export const HERE = fileURLToPath(new URL("..", import.meta.url));
const SECRET_KEY = /token|key|secret|authorization/i;

export type Server = { headers?: Record<string, string>; env?: Record<string, string> };

export function agents(root: string): Record<string, string[] | null> {
  const found: Record<string, string[] | null> = {};
  for (const file of readdirSync(join(root, ".claude/agents")).filter((f) => f.endsWith(".md")).sort()) {
    const head = readFileSync(join(root, ".claude/agents", file), "utf8").match(/^---\n([\s\S]*?)\n---\n/)![1];
    const tools = head.match(/^tools:\s*(.*)$/m);
    found[head.match(/^name:\s*(\S+)/m)![1]] = tools ? tools[1].split(",").map((t) => t.trim()) : null;
  }
  return found;
}

export function load(root: string) {
  const servers: Record<string, Server> = JSON.parse(readFileSync(join(root, ".mcp.json"), "utf8")).mcpServers;
  const perms = JSON.parse(readFileSync(join(root, ".claude/settings.json"), "utf8")).permissions ?? {};
  return { servers, subagents: agents(root), perms };
}

export function literalSecrets(servers: Record<string, Server>): string[] {
  const out: string[] = [];
  for (const [name, server] of Object.entries(servers)) {
    for (const field of ["headers", "env"] as const) {
      for (const [key, value] of Object.entries(server[field] ?? {})) if (SECRET_KEY.test(key) && !value.includes("${")) out.push(`${name} ${field}.${key}`);
    }
  }
  return out;
}

export function audit(root: string): string[] {
  const { servers, subagents, perms } = load(root);
  const found: string[] = literalSecrets(servers).map((s) => `literal-secret: ${s}`);
  const refs: Array<[string, string]> = [];
  for (const [agent, tools] of Object.entries(subagents)) for (const t of tools ?? []) if (t.startsWith("mcp__")) refs.push([agent, t]);
  for (const kind of ["allow", "deny"]) for (const r of perms[kind] ?? []) if (r.startsWith("mcp__")) refs.push(["settings", r]);
  for (const [who, ref] of refs) {
    const server = ref.split("__")[1];
    if (!(server in servers)) found.push(`unknown-server: ${server} (${who})`);
  }
  for (const [name, tools] of Object.entries(subagents)) {
    if (tools === null) found.push(`agent-inherits-all: ${name}`);
    else if (tools.includes("Bash")) found.push(`agent-bare-bash: ${name}`);
  }
  if (!(perms.deny ?? []).includes("Read(./.env)")) found.push("env-readable");
  if ((perms.allow ?? []).some((r: string) => r === "Edit" || r === "Write")) found.push("bare-write-allowed");
  return found;
}

function main() {
  for (const name of ["project-before", "project-after"]) {
    const { servers, subagents, perms } = load(join(HERE, name));
    const rules = (perms.allow ?? []).length + (perms.deny ?? []).length;
    console.log(`${name}: ${Object.keys(servers).length} servers, ${Object.keys(subagents).length} agents, ${rules} permission rules`);
    const found = audit(join(HERE, name));
    for (const finding of found) console.log(`  finding: ${finding}`);
    if (found.length === 0) {
      console.log("  no findings");
      for (const [agent, tools] of Object.entries(subagents)) console.log(`  ${agent}: ${tools!.join(", ")}`);
    }
  }
}

if (import.meta.main) main();
