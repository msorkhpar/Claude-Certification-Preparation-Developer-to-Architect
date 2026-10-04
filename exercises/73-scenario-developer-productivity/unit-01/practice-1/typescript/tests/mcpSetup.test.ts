import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds .mcp.json, .claude/ and docs/.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");

const AGENT_FILES = [".claude/agents/explorer.md", ".claude/agents/scaffolder.md"];
const SECRET_KEY = /token|key|secret|authorization/i;

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function loadJson(rel: string): any {
  try {
    return JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
}

function frontMatter(text: string): [string, string] {
  const head = text.match(/^---\n([\s\S]*?)\n---\n/);
  return head ? [head[1], text.slice(head[0].length)] : ["", text];
}

function agent(rel: string): [string, string[] | null] {
  const [head] = frontMatter(read(rel));
  const description = head.match(/^description:\s*(.*)$/m);
  const tools = head.match(/^tools:\s*(.*)$/m);
  return [description ? description[1].trim() : "", tools ? tools[1].split(",").map((t) => t.trim()) : null];
}

const servers = (): Record<string, any> => loadJson(".mcp.json").mcpServers ?? {};
const rules = (kind: string): string[] => loadJson(".claude/settings.json").permissions?.[kind] ?? [];

test("m1 every mcp tool reference belongs to a configured server", () => {
  const configured = servers();
  assert.ok(Object.keys(configured).length > 0, "configure at least one server in .mcp.json");
  const refs = AGENT_FILES.flatMap((rel) => agent(rel)[1] ?? []).filter((t) => t.startsWith("mcp__"));
  for (const kind of ["allow", "deny"]) refs.push(...rules(kind).filter((r) => r.startsWith("mcp__")));
  assert.ok(refs.length > 0, "the setup refers to at least one MCP tool");
  for (const ref of refs) assert.ok(ref.split("__")[1] in configured, `${ref} names a server that .mcp.json does not configure`);
});

test("e1 credentials come from the environment and the token has no default", () => {
  const configured = servers();
  for (const [name, server] of Object.entries(configured)) {
    for (const field of ["headers", "env"]) {
      for (const [key, value] of Object.entries<string>(server[field] ?? {})) assert.ok(!SECRET_KEY.test(key) || value.includes("${"), `${name} ${field}.${key} holds a literal credential`);
    }
  }
  const auth: string = configured.tickets?.headers?.Authorization ?? "";
  assert.ok(/\$\{[A-Z_][A-Z0-9_]*\}/.test(auth), "the tickets token is a ${VAR} reference");
  assert.ok(!auth.includes(":-"), "a default for a token would be a credential in the file");
});

test("e2 the explorer is read only and says when to use it", () => {
  const [description, tools] = agent(".claude/agents/explorer.md");
  assert.ok(description.startsWith("Use when"), "the description says when to delegate");
  assert.ok(tools !== null, "list the tools: without a tools line the subagent inherits every tool");
  assert.ok(tools!.includes("Read") && tools!.includes("Grep"), "an explorer reads and searches");
  assert.deepEqual(tools!.filter((t) => !["Read", "Grep", "Glob", "mcp__docs__search"].includes(t)), [], `read-only tools only, found ${tools}`);
});

test("e3 the scaffolder writes only in the generated folder", () => {
  const [, tools] = agent(".claude/agents/scaffolder.md");
  assert.ok(tools !== null, "list the tools: without a tools line the subagent inherits every tool");
  assert.ok(tools!.includes("Edit") && !tools!.includes("Bash"), "the scaffolder edits files and runs no commands");
  const writes = rules("allow").filter((r) => /^(Edit|Write|MultiEdit)\b/.test(r));
  assert.ok(writes.length > 0, "allow the scaffolder to edit the generated folder");
  for (const rule of writes) assert.ok(/^Edit\(src\/generated\/[^)]*\)$/.test(rule), `${rule} is not an Edit rule limited to the generated folder (a Write path rule is never consulted)`);
});

test("e4 the tickets server is read only for agents", () => {
  const deny = rules("deny");
  for (const tool of ["mcp__tickets__create_ticket", "mcp__tickets__delete_ticket"]) assert.ok(deny.includes(tool), `deny ${tool}`);
  for (const rule of rules("allow")) {
    if (rule.startsWith("mcp__tickets")) assert.ok(/^mcp__tickets__(get|list|search)_\w+$/.test(rule), `${rule} approves more than reading tickets`);
  }
});

test("e5 the environment file is denied and no whole tool is approved", () => {
  assert.ok(rules("deny").includes("Read(./.env)"), "deny reading the environment file");
  const whole = rules("allow").filter((r) => ["Bash", "Bash(*)", "Edit", "Write", "Read", "mcp__tickets"].includes(r));
  assert.deepEqual(whole, [], "a bare allow rule approves every call of that tool");
});

test("e6 the team note lists every variable and says which session to start", () => {
  const note = read("docs/team-setup.md");
  for (const match of new Set([...read(".mcp.json").matchAll(/\$\{([A-Za-z_]\w*)/g)].map((m) => m[1]))) assert.ok(new RegExp(`\\b${match}\\b`).test(note), `the note does not list ${match}`);
  const rows = new Map<string, string>();
  for (const line of note.split("\n")) {
    const cells = line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
    if (line.startsWith("|") && cells.length >= 2 && ["resume", "fork", "fresh"].includes(cells[1].toLowerCase())) rows.set(cells[0].toLowerCase(), cells[1].toLowerCase());
  }
  for (const [keyword, mode] of [["yesterday", "resume"], ["compare", "fork"], ["rewritten", "fresh"]]) {
    const situation = [...rows.keys()].find((t) => t.includes(keyword));
    assert.ok(situation, `the table has no row for the '${keyword}' situation`);
    assert.equal(rows.get(situation!), mode, `'${keyword}' should be ${mode}`);
  }
});

test("e7 no file holds a personal path an address or a key", () => {
  const hits: string[] = [];
  const walk = (dir: string) => {
    for (const entry of readdirSync(dir).sort()) {
      const path = join(dir, entry);
      if (statSync(path).isDirectory()) walk(path);
      else {
        const text = readFileSync(path, "utf8");
        for (const [label, pattern] of [["home path", /(\/home\/\w+|\/Users\/\w+|C:\\Users)/], ["email address", /[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/], ["key", /sk-ant-[\w-]{6,}/]] as const) {
          if (pattern.test(text)) hits.push(`${relative(ROOT, path)}: ${label}`);
        }
      }
    }
  };
  walk(ROOT);
  assert.deepEqual(hits, []);
});
