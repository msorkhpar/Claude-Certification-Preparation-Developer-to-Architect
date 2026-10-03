# Writing an MCP server: tools, resources, prompts and the stdout rule

**Level:** Developer · **Module 32:** MCP fundamentals · **Page 2 of 3**
**Exams:** DV5; A2.4

**After this page you can** write a small MCP server with a tool, a resource and a prompt in the official SDK of your language, report an expected failure as a tool error rather than a protocol error, mark a tool as read-only and know how far that hint can be trusted, and keep a stdio server from corrupting its own channel.

Checked against the Model Context Protocol specification and documentation (revision 2026-07-28) on 2026-10-03, with the example and the practice run offline in the course container: Python `mcp` 2.2.0, TypeScript `@modelcontextprotocol/sdk` 1.31.0, Java `io.modelcontextprotocol.sdk:mcp` 2.0.1 and Kotlin `kotlin-sdk` 0.15.0. The server and its client run on the same machine and talk through pipes. Nothing was sent over a network.

## Why it matters

A server is the part of an MCP integration that a team writes and maintains, and its mistakes are the ones that reach production: a tool whose failure the model cannot read, a stray line of output that breaks every client, a hint that a client trusts too much. The SDKs do the protocol work. You write what the server offers and decide what a failure looks like.

## The idea

### What a server declares

A server has a name and a version, and it declares the capabilities it offers: `tools`, `resources`, `prompts`. A server that declares `tools` must answer `tools/list`, and the specification asks it to return the tools in a deterministic order. Each tool has a name, a description and an input schema, and may have annotations.

- The specification says a tool name should be between 1 and 128 characters, case-sensitive, made only of letters, digits, underscore, hyphen and dot, and unique within the server.
- The input schema is JSON Schema. The Python SDK builds it from the type hints of the function and its docstring, and the TypeScript SDK from a schema library (`zod`). In the Java and Kotlin SDKs of the course's versions you hand the SDK a schema and read the arguments yourself, so you also check ranges and required values in the handler.
- A resource has a URI and a MIME type. A template such as `notes://note/{id}` covers a family of URIs, and a client discovers it with `resources/templates/list`.
- A prompt has a name, optional arguments and returns messages.

### Two kinds of failure

Tools use two error mechanisms, and choosing the right one is the main design decision in a handler.

1. **Protocol errors** "indicate issues with the request structure itself that models are less likely to be able to fix": an unknown tool, a malformed request, a server error. They are standard JSON-RPC errors.
2. **Tool execution errors** "contain actionable feedback that language models can use to self-correct and retry with adjusted parameters": API failures, "Input validation errors (e.g., date in wrong format, value out of range)" and business logic errors. They are ordinary results with `isError: true`.

Clients may pass either kind to the model, but the documentation says what each deserves. "Clients MAY provide protocol errors to language models, though these are less likely to result in successful recovery." And: "Clients SHOULD provide tool execution errors to language models to enable self-correction." So a validation failure belongs in the result, with text that says what to change, and a missing resource belongs in a protocol error. The course's server follows this: a blank title is a tool error that says `title is required`, and asking for a note that does not exist is a protocol error that says `No note 7`.

In the Python SDK, raise `ToolError` for an expected failure. A different exception that escapes the function is reported without its message, which is right for a bug and useless to a model that was supposed to read it. In TypeScript, Java and Kotlin you return a result whose error flag is set.

### Annotations are hints, not guarantees

A tool can carry annotations, which the specification calls "Optional properties describing tool behavior": whether it is read-only, destructive or idempotent. They help a client decide whether to ask a person. The documentation is explicit about their weight: "For trust & safety and security, clients MUST consider tool annotations to be untrusted unless they come from trusted servers." A hint of "read-only" is a claim by the server, so a client that skips a confirmation because of it has trusted the server's own word. The same page asks for a human check around tools: "there SHOULD always be a human in the loop with the ability to deny tool invocations". Annotations belong in your server because they make a good client better, not because they enforce anything.

### The stdout rule

On the stdio transport the server reads messages from stdin and writes them to stdout, one per line. "The server MUST NOT write anything to its stdout that is not a valid MCP message." Everything else goes to stderr, which the client may capture, forward or ignore, and "SHOULD NOT assume stderr output indicates error conditions". Every SDK's server guide has the same warning for its language: "Never write to stdout. Writing to stdout will corrupt the JSON-RPC messages and break your server." In Python the culprit is `print()`, in TypeScript `console.log()`, in Java `System.out.println()` and in Kotlin `println()`. Use a logger that writes to stderr (in Python the standard `logging` module). For a server on Streamable HTTP the rule does not apply, because stdout is not the channel, and the documentation says standard output logging is fine there.

### The example: one file, two roles

The example below is one file in each language. Started alone it launches itself as a server on stdio and talks to itself twice: first through the SDK's own client, as a host would, then by hand with raw JSON-RPC lines, which shows what the SDK hides. The server has two tools, a resource and a prompt. Run with `--serve` it is only the server.

<!-- example: m32-notes-over-stdio tabs: python,typescript -->
```python
"""One file, two roles: run with --serve it is an MCP server over stdio; run alone it starts itself as a server and talks to it twice.

First through the SDK client (what a host application does), then by hand with raw JSON-RPC lines (what the SDK hides). Everything is
local: the server is a child process and the two sides talk through pipes. `mcp` 2.2.0, checked on 2026-10-03.
"""
import asyncio
import json
import logging
import os
import subprocess
import sys

from mcp import Client
from mcp.client.stdio import StdioServerParameters
from mcp.server.mcpserver import MCPServer
from mcp.server.mcpserver.exceptions import ToolError
from mcp.types import ToolAnnotations

log = logging.getLogger("notes")  # logging writes to stderr; print() would write to stdout and corrupt the protocol


def build_server():
    server, notes = MCPServer("notes", version="1.0.0"), []

    @server.tool(description="Save a note.", annotations=ToolAnnotations(read_only_hint=False))
    def add_note(title: str, text: str) -> str:
        if not title.strip():
            raise ToolError("title is required")  # an expected failure: the model reads the message and can retry
        notes.append((title.strip(), text))
        log.info("saved note %d", len(notes))
        return f"Saved note {len(notes)}: {title.strip()}"

    @server.tool(description="Find notes by a word in the title.", annotations=ToolAnnotations(read_only_hint=True))
    def search_notes(query: str, limit: int = 5) -> str:
        hits = [f"{i}. {t}" for i, (t, _) in enumerate(notes, 1) if query.lower() in t.lower()]
        return "\n".join(hits[:limit]) or f'No notes match "{query}"'

    @server.resource("notes://count")
    def count() -> str:
        return f"{len(notes)} note" + ("" if len(notes) == 1 else "s")

    @server.prompt()
    def review_notes(tone: str = "brief") -> str:
        return f"Review these notes in a {tone} tone:\n" + "\n".join(f"- {t}" for t, _ in notes)

    return server


def words(result):
    return "".join(getattr(c, "text", "") for c in result.content)


async def with_sdk_client(command):
    async with Client(StdioServerParameters(command=command[0], args=command[1:], env=dict(os.environ))) as client:
        info = client.server_info
        print(f"server: {info.name} {info.version}")
        caps = client.server_capabilities
        print("declares:", ", ".join(name for name in ("tools", "resources", "prompts") if getattr(caps, name) is not None))
        for tool in (await client.list_tools()).tools:
            props = tool.input_schema["properties"]
            args = ", ".join(p if p in tool.input_schema["required"] else f"[{p}]" for p in props)
            print(f"tool: {tool.name}({args}) read-only hint {tool.annotations.read_only_hint}")
        ok = await client.call_tool("add_note", {"title": "Plan", "text": "ship it"})
        print(f"add_note -> {words(ok)!r}, isError {ok.is_error}")
        bad = await client.call_tool("add_note", {"title": " ", "text": "x"})
        print(f"add_note with a blank title -> isError {bad.is_error}, says why: {'title is required' in words(bad)}")
        print("search_notes ->", repr(words(await client.call_tool("search_notes", {"query": "PLAN"}))))
        print("resource notes://count ->", (await client.read_resource("notes://count")).contents[0].text)
        message = (await client.get_prompt("review_notes", {})).messages[0]
        print(f"prompt review_notes -> role {message.role}, {message.content.text!r}")


def wire(command):
    """The same server, spoken to line by line: one JSON-RPC message per line on stdin and stdout, nothing else on stdout."""
    child = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env={**os.environ, "NOTES_LOG": "INFO"})
    stray = 0

    def send(message):
        child.stdin.write(json.dumps(message) + "\n")
        child.stdin.flush()

    def receive(expected_id):
        nonlocal stray
        while True:
            line = child.stdout.readline()
            try:
                message = json.loads(line)
            except ValueError:
                stray += 1
                continue
            if message.get("id") == expected_id:
                return message

    send({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2025-11-25", "capabilities": {}, "clientInfo": {"name": "wire", "version": "1"}}})
    first = receive(1)["result"]
    print(f"-> initialize (id 1)      <- protocol {first['protocolVersion']}, server {first['serverInfo']['name']}, capabilities {sorted(k for k in first['capabilities'] if k in ('tools', 'resources', 'prompts'))}")
    send({"jsonrpc": "2.0", "method": "notifications/initialized"})
    send({"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
    print("-> tools/list (id 2)      <- tools", sorted(t["name"] for t in receive(2)["result"]["tools"]))
    send({"jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": {"name": "add_note", "arguments": {"title": "Wire", "text": "by hand"}}})
    print("-> tools/call (id 3)      <- ", receive(3)["result"]["content"][0]["text"])
    send({"jsonrpc": "2.0", "id": 4, "method": "tools/call", "params": {"name": "search_notes", "arguments": {"query": "wire", "limit": "two"}}})
    answer = receive(4)
    print("-> tools/call with limit 'two' (id 4)   <-", "a result with isError" if answer.get("result", {}).get("isError") else "a protocol error" if "error" in answer else "a result")
    child.stdin.close()
    logs = child.stderr.read()
    child.wait(timeout=10)
    print("lines on stdout that were not JSON:", stray)
    print("the server's own log went to stderr:", "saved note 1" in logs)


def main():
    command = [sys.executable, __file__, "--serve"]
    asyncio.run(with_sdk_client(command))
    print()
    wire(command)


if __name__ == "__main__":
    if "--serve" in sys.argv:
        logging.basicConfig(level=os.environ.get("NOTES_LOG", "WARNING"))
        build_server().run()
    else:
        main()
```
```text
server: notes 1.0.0
declares: tools, resources, prompts
tool: add_note(title, text) read-only hint False
tool: search_notes(query, [limit]) read-only hint True
add_note -> 'Saved note 1: Plan', isError False
add_note with a blank title -> isError True, says why: True
search_notes -> '1. Plan'
resource notes://count -> 1 note
prompt review_notes -> role user, 'Review these notes in a brief tone:\n- Plan'

-> initialize (id 1)      <- protocol 2025-11-25, server notes, capabilities ['prompts', 'resources', 'tools']
-> tools/list (id 2)      <- tools ['add_note', 'search_notes']
-> tools/call (id 3)      <-  Saved note 1: Wire
-> tools/call with limit 'two' (id 4)   <- a result with isError
lines on stdout that were not JSON: 0
the server's own log went to stderr: True
```
```typescript
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
```
```text
server: notes 1.0.0
declares: tools, resources, prompts
tool: add_note(title, text) read-only hint False
tool: search_notes(query, [limit]) read-only hint True
add_note -> 'Saved note 1: Plan', isError False
add_note with a blank title -> isError True, says why: True
search_notes -> '1. Plan'
resource notes://count -> 1 note
prompt review_notes -> role user, 'Review these notes in a brief tone:\n- Plan'

-> initialize (id 1)      <- protocol 2025-11-25, server notes, capabilities ['prompts', 'resources', 'tools']
-> tools/list (id 2)      <- tools ['add_note', 'search_notes']
-> tools/call (id 3)      <-  Saved note 1: Wire
-> tools/call with limit 'two' (id 4)   <- a result with isError
lines on stdout that were not JSON: 0
the server's own log went to stderr: True
```
<!-- /example -->

The first block of the output is the SDK client's view. It reads the server's name and version, sees that the server declares tools, resources and prompts, and lists two tools with the read-only hint of `search_notes` set. The blank-title call comes back as an error result that says why, and the search ignores letter case. The second block is the wire. The raw `initialize` request, sent with protocol version 2025-11-25, is answered with that same version, and then `tools/list` and two `tools/call` messages follow, each correlated by its `id`. The last call passes `limit` as the text `two`. With both SDKs the answer is a result with `isError` set, not a protocol error, because the SDK validates the arguments against the generated schema and reports the failure as a tool error. The last two lines count the lines on stdout that were not JSON (none) and confirm that the server's own log went to stderr.

## Traps

1. **Raising a protocol error for a failure the model could fix.** A blank title or an out-of-range limit is input the model can correct on the next call. Return it as a tool error with text that says what to change.
2. **Printing to stdout in a stdio server.** One stray line corrupts the stream. Log to stderr.
3. **Trusting a hint from the same server.** "Read-only" is the server's claim. A client treats it as untrusted unless the server is trusted, and a human check stays in place.
4. **Letting an unexpected exception carry the message.** In the Python SDK, the message of an exception that is not a `ToolError` is not shown to the client. Raise `ToolError` for failures that you expect.

## Quiz

1. A stdio server prints a debug line with the language's ordinary print function, and the client then fails to parse the stream. Why?
   - **a**: The client reads standard error as well, and treats any text there as fatal
   - **b**: Standard output carries only protocol messages, so stray text corrupts it
   - **c**: The SDK forbids logging libraries, so every debug line is rejected
   - **d**: Printed lines are queued and delivered after the tool call has ended

2. A search tool receives a date range in the wrong format, and the server wants the model to try again with a fix. How should the failure be reported?
   - **a**: As a JSON-RPC error response, which is passed to the model for retries
   - **b**: As a protocol error that says the request itself is malformed
   - **c**: By returning empty text, which signals the model to ask the user
   - **d**: As an ordinary result flagged as an error, with a readable explanation

3. A tool declares that it only reads data, and a client skips the confirmation prompt for it because of that declaration. What does the specification say about trusting the declaration?
   - **a**: Hints are binding, so a read-only tool never needs confirmation
   - **b**: Hints are unverified claims, unless the origin is vetted
   - **c**: Hints are trusted once the first call has succeeded without an error
   - **d**: Hints are checked by the SDK, which blocks any tool that writes

<details>
<summary>Answer key</summary>

1. **b**. The page says "The server MUST NOT write anything to its stdout that is not a valid MCP message." *a* is ruled out because the client "SHOULD NOT assume stderr output indicates error conditions". *d* is ruled out because the stream carries one message per line and "The server MUST NOT write anything to its stdout that is not a valid MCP message", with no queue of printed lines. *c* is ruled out because the guide says to use a logger that writes to stderr: "Never write to stdout. Writing to stdout will corrupt the JSON-RPC messages and break your server."
2. **d**. The page says tool execution errors "contain actionable feedback that language models can use to self-correct and retry with adjusted parameters", and lists "Input validation errors (e.g., date in wrong format, value out of range)" among them. *a* is ruled out because "Clients MAY provide protocol errors to language models, though these are less likely to result in successful recovery." *b* is ruled out because a wrong format is an "Input validation errors (e.g., date in wrong format, value out of range)", which is a tool execution error and not a malformed request. *c* is ruled out because "Clients SHOULD provide tool execution errors to language models to enable self-correction", which needs text that says what to change.
3. **b**. The page quotes the specification: "clients MUST consider tool annotations to be untrusted unless they come from trusted servers." *a* is ruled out because "there SHOULD always be a human in the loop with the ability to deny tool invocations". *d* is ruled out because annotations are only "Optional properties describing tool behavior" and nothing in the SDK enforces them. *c* is ruled out because trust depends on where the server comes from, "unless they come from trusted servers", and not on how a first call went.

</details>
