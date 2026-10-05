// One file, two roles: run with --serve it is an MCP server over stdio; run alone it starts itself as a server and talks to it twice.
//
// First through the SDK client (what a host application does), then by hand with raw JSON-RPC lines (what the SDK hides). Everything is
// local: the server is a child process and the two sides talk through pipes. `@modelcontextprotocol/sdk` 1.31.0, checked on 2026-10-03.
import { spawn } from "node:child_process";
import { createInterface } from "node:readline";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { McpServer, ResourceTemplate } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { logger } from "./logger.ts";
const log = logger("notes_example");

const textOf = (result: any) => (result.content ?? []).map((c: any) => c.text ?? "").join("");
const fail = (text: string) => ({ isError: true, content: [{ type: "text" as const, text }] });
const ok = (text: string) => ({ content: [{ type: "text" as const, text }] });

export function buildServer() {
  const server = new McpServer({ name: "notes", version: "1.0.0" });
  const notes: string[] = [];
  server.registerTool("add_note", { description: "Save a note.", inputSchema: { title: z.string(), text: z.string() }, annotations: { readOnlyHint: false } }, async ({ title }) => {
    if (!title.trim()) return fail("title is required"); // an expected failure: the model reads the message and can retry
    notes.push(title.trim());
    console.error(`saved note ${notes.length}`); // stderr is for logs; console.log would write to stdout and corrupt the protocol
    return ok(`Saved note ${notes.length}: ${title.trim()}`);
  });
  server.registerTool("search_notes", { description: "Find notes by a word in the title.", inputSchema: { query: z.string(), limit: z.number().int().default(5) }, annotations: { readOnlyHint: true } }, async ({ query, limit }) => {
    const hits = notes.flatMap((t, i) => (t.toLowerCase().includes(query.toLowerCase()) ? [`${i + 1}. ${t}`] : []));
    return ok(hits.slice(0, limit).join("\n") || `No notes match "${query}"`);
  });
  server.registerResource("count", "notes://count", { mimeType: "text/plain" }, async (uri) => ({ contents: [{ uri: uri.href, text: `${notes.length} note${notes.length === 1 ? "" : "s"}` }] }));
  server.registerPrompt("review_notes", { argsSchema: { tone: z.string().optional() } }, ({ tone }) => ({
    messages: [{ role: "user" as const, content: { type: "text" as const, text: `Review these notes in a ${tone ?? "brief"} tone:\n` + notes.map((t) => `- ${t}`).join("\n") } }],
  }));
  return server;
}

async function withSdkClient(command: string[]) {
  const client = new Client({ name: "host", version: "1.0.0" });
  await client.connect(new StdioClientTransport({ command: command[0], args: command.slice(1), env: process.env as Record<string, string>, stderr: "ignore" }));
  const info = client.getServerVersion()!;
  console.log(`server: ${info.name} ${info.version}`);
  const caps = client.getServerCapabilities()!;
  console.log("declares:", (["tools", "resources", "prompts"] as const).filter((n) => caps[n] !== undefined).join(", "));
  for (const tool of (await client.listTools()).tools) {
    const props = Object.keys((tool.inputSchema as any).properties);
    const args = props.map((p) => ((tool.inputSchema as any).required.includes(p) ? p : `[${p}]`)).join(", ");
    console.log(`tool: ${tool.name}(${args}) read-only hint ${tool.annotations!.readOnlyHint ? "True" : "False"}`);
  }
  const good: any = await client.callTool({ name: "add_note", arguments: { title: "Plan", text: "ship it" } });
  console.log(`add_note -> '${textOf(good)}', isError ${good.isError ? "True" : "False"}`);
  const bad: any = await client.callTool({ name: "add_note", arguments: { title: " ", text: "x" } });
  console.log(`add_note with a blank title -> isError ${bad.isError ? "True" : "False"}, says why: ${textOf(bad).includes("title is required") ? "True" : "False"}`);
  console.log("search_notes ->", `'${textOf(await client.callTool({ name: "search_notes", arguments: { query: "PLAN" } }))}'`);
  console.log("resource notes://count ->", ((await client.readResource({ uri: "notes://count" })).contents[0] as any).text);
  const message = (await client.getPrompt({ name: "review_notes", arguments: {} })).messages[0];
  console.log(`prompt review_notes -> role ${message.role}, '${(message.content as any).text.replace(/\n/g, "\\n")}'`);
  await client.close();
}

/** The same server, spoken to line by line: one JSON-RPC message per line on stdin and stdout, nothing else on stdout. */
async function wire(command: string[]) {
  const child = spawn(command[0], command.slice(1), { stdio: ["pipe", "pipe", "pipe"] });
  let logs = "";
  child.stderr.on("data", (chunk) => (logs += chunk));
  const lines = createInterface({ input: child.stdout })[Symbol.asyncIterator]();
  let stray = 0;
  const send = (message: object) => child.stdin.write(JSON.stringify(message) + "\n");
  const receive = async (id: number): Promise<any> => {
    for (;;) {
      const { value } = await lines.next();
      let message: any;
      try {
        message = JSON.parse(value);
      } catch {
        stray++;
        continue;
      }
      if (message.id === id) return message;
    }
  };
  send({ jsonrpc: "2.0", id: 1, method: "initialize", params: { protocolVersion: "2025-11-25", capabilities: {}, clientInfo: { name: "wire", version: "1" } } });
  const first = (await receive(1)).result;
  console.log(`-> initialize (id 1)      <- protocol ${first.protocolVersion}, server ${first.serverInfo.name}, capabilities [${Object.keys(first.capabilities).filter((k) => ["tools", "resources", "prompts"].includes(k)).sort().map((k) => `'${k}'`).join(", ")}]`);
  send({ jsonrpc: "2.0", method: "notifications/initialized" });
  send({ jsonrpc: "2.0", id: 2, method: "tools/list" });
  console.log("-> tools/list (id 2)      <- tools", `[${(await receive(2)).result.tools.map((t: any) => `'${t.name}'`).sort().join(", ")}]`);
  send({ jsonrpc: "2.0", id: 3, method: "tools/call", params: { name: "add_note", arguments: { title: "Wire", text: "by hand" } } });
  console.log("-> tools/call (id 3)      <- ", (await receive(3)).result.content[0].text);
  send({ jsonrpc: "2.0", id: 4, method: "tools/call", params: { name: "search_notes", arguments: { query: "wire", limit: "two" } } });
  const answer = await receive(4);
  console.log("-> tools/call with limit 'two' (id 4)   <-", answer.result?.isError ? "a result with isError" : answer.error ? "a protocol error" : "a result");
  child.stdin.end();
  await new Promise((resolve) => child.on("exit", resolve));
  console.log("lines on stdout that were not JSON:", stray);
  console.log("the server's own log went to stderr:", logs.includes("saved note 1") ? "True" : "False");
}

async function main() {
  const command = [process.execPath, new URL(import.meta.url).pathname, "--serve"];
  await withSdkClient(command);
  console.log();
  await wire(command);
}

if (import.meta.main) {
  if (process.argv.includes("--serve")) await buildServer().connect(new StdioServerTransport());
  else await main();
}
