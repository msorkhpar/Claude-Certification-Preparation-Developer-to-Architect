import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const ROOT = resolve((import.meta.dirname + "/../project"));
const example = (file: string) => {
  const candidates = [join(import.meta.dirname, "examples", file), `/w/examples/${file}`];
  for (let d = resolve("."), i = 0; i < 9; i++, d = resolve(d, "..")) candidates.push(join(d, "examples", file));
  return pathToFileURL(candidates.find((c) => existsSync(c)) as string).href;
};
const { expandServer, lint, mcpDecision, resolveServers, truncate } = await import(example("55-mcp-config/typescript/mcpConfig.ts"));
const COVERED = ["ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "AWS_BEARER_TOKEN_BEDROCK", "HTTPS_PROXY", "NPM_TOKEN"];
const SERVERS = ["core", "docs", "github", "schema"];

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function readJson(rel: string): any {
  let data: any;
  try {
    data = JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
  assert.ok(data && typeof data === "object" && !Array.isArray(data), `${rel} must hold a JSON object`);
  return data;
}

const shared = (): Record<string, any> => readJson(".mcp.json").mcpServers ?? {};
function server(name: string): any {
  const found = shared()[name];
  assert.ok(found && typeof found === "object", `.mcp.json has no server named ${name}`);
  return found;
}

test("m1 the shared file declares the four team servers with the right shape", () => {
  assert.deepEqual(Object.keys(shared()).sort(), SERVERS, "declare exactly core, docs, github and schema");
  assert.ok(server("github").type === "http" && server("core").type === "http", "github and core are remote http servers");
  assert.ok((server("docs").type ?? "stdio") === "stdio" && (server("schema").type ?? "stdio") === "stdio", "docs and schema run as local processes");
  assert.ok(server("docs").command === "python3" && server("schema").command === "python3");
  assert.deepEqual(lint({ mcpServers: shared() }), []);
});

test("e1 credentials are read from the environment and never written in the file", () => {
  assert.equal(server("github").headers?.Authorization, "Bearer ${GITHUB_TOKEN}", "send the token as Bearer ${GITHUB_TOKEN}");
  assert.equal(server("docs").env?.DOCS_API_KEY, "${DOCS_API_KEY}", "pass DOCS_API_KEY through from the environment");
  const names = [...JSON.stringify(server("github").headers ?? {}).matchAll(/\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-[^}]*)?\}/g)].map((m) => m[1]);
  assert.ok(!names.some((n) => COVERED.includes(n)), "a credential name that Claude Code reads as empty toward a remote server cannot carry the token");
  for (const entry of Object.values<any>(shared())) {
    for (const field of ["headers", "env"]) {
      for (const [key, value] of Object.entries<string>(entry[field] ?? {})) {
        assert.ok(/\$\{[A-Za-z_]\w*\}/.test(value) && !value.includes(":-"), `${key}: no default value for a credential, it would be committed`);
      }
    }
  }
  const [out, warnings] = expandServer(server("github"), { GITHUB_TOKEN: "t" });
  assert.ok(out.headers.Authorization === "Bearer t" && warnings.length === 0);
});

test("e2 endpoints and paths that are not secret have a default so the file works unset", () => {
  for (const name of ["github", "core"]) {
    assert.ok(/^\$\{[A-Z_]+:-https:\/\/[^}]+\}$/.test(server(name).url ?? ""), `${name}: url needs a \${VAR:-default} with an https default`);
  }
  for (const name of ["docs", "schema"]) {
    const args: string[] = server(name).args ?? [];
    assert.ok(args.length > 0 && args[0].startsWith("${CLAUDE_PROJECT_DIR:-.}/"), `${name}: CLAUDE_PROJECT_DIR is set for the server, not for the command, so it needs a default`);
    const [out, warnings] = expandServer(server(name), {});
    assert.ok(!warnings.some((w: string) => w.includes("CLAUDE_PROJECT_DIR")) && out.args[0].startsWith("./tools/"));
  }
  const [out] = expandServer(server("github"), {});
  assert.ok(out.url.startsWith("https://") && !out.url.includes("${"));
});

test("e3 only the small core server is loaded at the start", () => {
  assert.equal(server("core").alwaysLoad, true, "core is used on every turn: alwaysLoad true");
  const others = SERVERS.filter((n) => n !== "core" && shared()[n]?.alwaysLoad);
  assert.deepEqual(others, [], "tool search should find the others on demand");
});

test("e4 shared servers stay in the project file and personal ones in the user scope file", () => {
  const user = readJson("user-scope.example.json").mcpServers ?? {};
  assert.deepEqual(Object.keys(user), ["scratch"], "the user-scope example holds one personal server, scratch");
  assert.ok(!("scratch" in shared()), "a personal server does not belong in the committed file");
  assert.ok(!Object.keys(user).some((n) => n in shared()), "the same name in two scopes: the whole entry of the higher scope wins and a warning is shown");
  const [servers, warnings] = resolveServers({ project: shared(), user });
  assert.deepEqual(Object.keys(servers).sort(), [...SERVERS, "scratch"].sort());
  assert.deepEqual(warnings, []);
  assert.ok(!Object.values(user).some((e) => /\/home\/|\/Users\//.test(JSON.stringify(e))), "no absolute home path: use ${HOME}");
});

test("e5 permissions allow the read only servers by name and deny the destructive tool", () => {
  const settings = readJson(".claude/settings.json");
  const table: Record<string, string> = { mcp__docs__search_docs: "allow", mcp__schema__read_schema: "allow", mcp__github__list_pull_requests: "ask",
    mcp__github__create_issue: "ask", mcp__github__delete_repository: "deny", mcp__core__ping: "ask", mcp__scratch__anything: "ask" };
  const got = Object.fromEntries(Object.keys(table).map((t) => [t, mcpDecision(settings, t)]));
  assert.deepEqual(got, table);
  const rules: string[] = settings.permissions?.allow ?? [];
  assert.ok(!rules.some((r) => r === "*" || r.startsWith("mcp__*")), "an allow rule must name its server; mcp__* is ignored");
});

test("e6 the tool description says when to use it instead of grep and fits the limit", () => {
  const tools: any[] = readJson("docs/tool-descriptions.json").tools ?? [];
  const found = tools.filter((t) => t.name === "search_docs");
  assert.ok(found.length > 0, "describe the search_docs tool");
  const text: string = found[0].description ?? "";
  assert.ok(text.length <= 2048 && truncate(text) === text, "Claude Code cuts a description at 2,048 characters");
  assert.ok(/instead of Grep/.test(text.slice(0, 300)), "put the boundary against Grep in the first 300 characters");
  assert.ok(text.includes("Returns") || text.includes("returns"), "say what comes back");
  assert.ok(/\b(does not|doesn't|not search)\b/.test(text), "say what it does not do");
  const props: Record<string, any> = found[0].inputSchema?.properties ?? {};
  assert.ok(Object.keys(props).length > 0 && Object.values(props).every((p) => p.description), "every parameter needs a description");
  assert.deepEqual(found[0].inputSchema.required, ["query"]);
});

test("e7 the notes list every server with its scope and read the catalog as a resource", () => {
  const notes = read("docs/mcp-servers.md");
  for (const name of SERVERS) {
    const row = notes.match(new RegExp("^\\|\\s*`" + name + "`\\s*\\|\\s*(\\w+)\\s*\\|(.*)$", "m"));
    assert.ok(row, `add a table row for ${name}`);
    assert.equal(row[1], "project", `${name} is shared, so its scope is project`);
  }
  const refs = [...notes.matchAll(/@([\w-]+):(\w+:\/\/[\w./-]+)/g)].map((m) => [m[1], m[2]]);
  assert.ok(refs.length > 0 && refs.every(([n]) => SERVERS.includes(n)), "@server:protocol://path must name a configured server");
  assert.ok(refs.some(([n, p]) => n === "schema" && p === "schema://orders"), "show how to read the orders schema");
  const row = (notes.match(/^\|\s*`schema`.*$/m) as RegExpMatchArray)[0];
  assert.ok(row.toLowerCase().includes("resource"), "the schema server exposes resources");
});

test("e8 no file holds a personal path an address or a key", () => {
  const hits: string[] = [];
  const walk = (dir: string) => {
    for (const entry of readdirSync(dir).sort()) {
      const path = join(dir, entry);
      if (statSync(path).isDirectory()) walk(path);
      else {
        const text = readFileSync(path, "utf8");
        for (const [label, pattern] of [["home path", /(\/home\/\w+|\/Users\/\w+|C:\\Users)/], ["email address", /[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/],
          ["key", /(sk-ant-[\w-]{6,}|ghp_\w{6,}|Bearer [A-Za-z0-9]{12,})/]] as const) {
          if (pattern.test(text)) hits.push(`${relative(ROOT, path)}: ${label}`);
        }
      }
    }
  };
  walk(ROOT);
  assert.deepEqual(hits, []);
});
