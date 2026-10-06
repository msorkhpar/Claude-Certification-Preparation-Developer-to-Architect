// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Start your server as a separate process over stdio and connect the SDK's own client to it, as the tests do.
// (The server's stdout is the protocol channel, so its log lines cannot show here.)
const server = resolve(dirname(fileURLToPath(import.meta.url)), "notes_server.ts");
const transport = new StdioClientTransport({ command: process.execPath, args: [server], env: process.env as Record<string, string>, stderr: "ignore" });
const client = new Client({ name: "try-it", version: "1.0.0" });
const text = (result: any) => (result?.content ?? []).map((c: any) => c.text ?? "").join("");

try {
  await client.connect(transport);
  console.log("add_note:", text(await client.callTool({ name: "add_note", arguments: { title: "Plan", text: "ship it" } })));
  console.log("search_notes:", text(await client.callTool({ name: "search_notes", arguments: { query: "ship" } })));
  const counted: any = await client.readResource({ uri: "notes://count" });
  console.log("count resource:", counted.contents?.[0]?.text);
  const read: any = await client.readResource({ uri: "notes://note/1" });
  console.log("note 1 resource:", JSON.stringify(read.contents?.[0]?.text));
} catch (err) {
  console.log("raised:", String(err));
} finally {
  await client.close();
}
