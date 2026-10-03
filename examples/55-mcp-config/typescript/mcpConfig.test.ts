import { test } from "node:test";
import assert from "node:assert/strict";
import { SCOPES, expand, expandServer, lint, mcpDecision, resolveServers, truncate } from "./mcpConfig.ts";

test("a variable expands and a default fills an unset one", () => {
  assert.deepEqual(expand("${A}-${B:-fallback}-${C:-}", { A: "x" }), ["x-fallback-", []]);
});

test("an unset variable without a default keeps its text and is reported", () => {
  assert.deepEqual(expand("Bearer ${TOKEN}", {}), ["Bearer ${TOKEN}", ["TOKEN"]]);
});

test("credential variables read empty toward a remote server but not for a local one", () => {
  const env = { NPM_TOKEN: "t", MY_TOKEN: "m" };
  assert.deepEqual(expand("Bearer ${NPM_TOKEN}", env, true), ["Bearer ", []]);
  assert.deepEqual(expand("Bearer ${NPM_TOKEN:-d}", {}, true), ["Bearer ", []]);
  assert.deepEqual(expand("Bearer ${MY_TOKEN}", env, true), ["Bearer m", []]);
  assert.deepEqual(expand("${NPM_TOKEN}", env, false), ["t", []]);
});

test("expansion covers command args env url and headers only", () => {
  const entry = { type: "stdio", command: "${BIN:-run}", args: ["${A:-1}"], env: { K: "${V:-v}" }, note: "${A}" };
  assert.deepEqual(expandServer(entry, {}), [{ type: "stdio", command: "run", args: ["1"], env: { K: "v" }, note: "${A}" }, []]);
});

test("the highest scope wins the whole entry and a conflict is reported", () => {
  const [servers, warnings] = resolveServers({ user: { s: { url: "u", extra: 1 } }, project: { s: { url: "p" } }, local: {} });
  assert.deepEqual(servers.s, ["project", { url: "p" }]);
  assert.equal(warnings.length, 1);
  assert.equal(SCOPES[0], "local");
});

test("lint finds literal secrets and missing fields", () => {
  assert.deepEqual(lint({ mcpServers: { a: { type: "http", headers: { Authorization: "x" } }, b: {} } }), [
    "a: a http server needs a url", "a: headers.Authorization holds a literal value, reference an environment variable", "b: a stdio server needs a command"]);
  assert.deepEqual(lint({ mcpServers: { a: { type: "http", url: "u", headers: { Authorization: "Bearer ${T}" } } } }), []);
});

test("a description is cut at the limit", () => {
  assert.equal(truncate("x".repeat(5000)).length, 2048);
});

test("only an allow rule that names its server is honoured and deny wins", () => {
  const s = { permissions: { allow: ["mcp__docs__*", "mcp__*", "mcp__github__get_*"], deny: ["mcp__github__get_secret"] } };
  assert.deepEqual(["mcp__docs__a", "mcp__other__a", "mcp__github__get_pr", "mcp__github__get_secret", "mcp__github__push"].map((t) => mcpDecision(s, t)), ["allow", "ask", "allow", "deny", "ask"]);
});
