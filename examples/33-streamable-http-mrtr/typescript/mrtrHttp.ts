// A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.
//
// The server and the client are `@modelcontextprotocol/sdk` 1.31.0, with scripted callbacks in place of a person and a model. This SDK
// speaks the 2025-11-25 revision, so the flow differs from the Python example next to it: there is an `initialize` handshake and a session
// id, and the server calls the client in the middle of the request, as a request of its own on the response stream. The log under the
// program output is what the fetch hook of the client saw. Checked on 2026-10-03 against the "Streamable HTTP" page of the MCP specification.
import { randomUUID } from "node:crypto";
import { createServer, type Server } from "node:http";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/sdk/client/streamableHttp.js";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { CreateMessageRequestSchema, ElicitRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { z } from "zod";

const text = (t: string) => ({ content: [{ type: "text" as const, text: t }] });

export function buildServer() {
  const server = new McpServer({ name: "deployer", version: "1.0.0" });
  server.registerTool("deploy", { description: "Deploy a service; production needs a person's confirmation.", inputSchema: { service: z.string(), env: z.string() } }, async ({ service, env }, extra) => {
    if (env === "production") {
      if (!server.server.getClientCapabilities()?.elicitation) return { isError: true, content: [{ type: "text" as const, text: "Deploying to production needs confirmation, and this client cannot be asked." }] };
      // relatedRequestId ties the question to this call, so it travels on this call's response stream and not on the standalone one
      const answer = await server.server.elicitInput({ message: `Deploy ${service} to production?`, requestedSchema: { type: "object", properties: { confirm: { type: "boolean", title: "Confirm the deployment" } }, required: ["confirm"] } }, { relatedRequestId: extra.requestId });
      if (answer.action !== "accept" || !answer.content?.confirm) return text("Deployment cancelled");
    }
    return text(`Deployed ${service} to ${env}`);
  });
  server.registerTool("release_notes", { description: "Write release notes with the client's model.", inputSchema: { service: z.string() } }, async ({ service }, extra) => {
    const completion = await server.server.createMessage({ messages: [{ role: "user", content: { type: "text", text: `Write one sentence of release notes for ${service}.` } }], maxTokens: 100 }, { relatedRequestId: extra.requestId });
    return text(`${service}: ${(completion.content as any).text}`);
  });
  return server;
}

/** The MCP endpoint on 127.0.0.1: one session transport for the one client of this program. */
export async function serveOnLoopback(server: McpServer): Promise<{ http: Server; url: string }> {
  const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: () => randomUUID() });
  await server.connect(transport);
  const http = createServer(async (req, res) => {
    let body = "";
    for await (const chunk of req) body += chunk;
    await transport.handleRequest(req, res, body ? JSON.parse(body) : undefined);
  });
  await new Promise<void>((resolve) => http.listen(0, "127.0.0.1", resolve));
  return { http, url: `http://127.0.0.1:${(http.address() as any).port}/mcp` };
}

/** One line for a JSON-RPC message that crossed the wire. */
function describe(message: any, fromServer: boolean): string {
  if (message.method) return `${message.method}${message.params?.name ? " " + message.params.name : ""}${fromServer ? " (a request from the server)" : ""}`;
  if (message.error) return `error ${message.error.code}`;
  if (Array.isArray(message.result?.content)) return `complete: ${message.result.content[0].text}`;
  return "the answer to the server's request";
}

async function main() {
  const { http, url } = await serveOnLoopback(buildServer());
  const log: Array<{ at: number; line: string }> = [];
  const reads: Promise<void>[] = [];
  let tick = 0;
  let sessionOnEvery = true;
  let sawInitialize = false;
  const recordingFetch: typeof fetch = async (input, init) => {
    const at = tick++;
    const body = typeof init?.body === "string" ? JSON.parse(init.body) : undefined;
    if (body?.method === "initialize") sawInitialize = true;
    else if (body && !new Headers(init?.headers).has("mcp-session-id")) sessionOnEvery = false;
    if (body?.method === "tools/call" || (!body?.method && body)) log.push({ at, line: `-> ${describe(body, false)}` });
    const response = await fetch(input, init);
    if (body?.method === "tools/call" && response.headers.get("content-type")?.includes("text/event-stream")) {
      reads.push((async () => { // read a copy of the stream message by message, so the log is in the order things happened
        const reader = response.clone().body!.getReader();
        const decoder = new TextDecoder();
        let pending = "";
        for (;;) {
          const { done, value } = await reader.read();
          if (done) break;
          pending += decoder.decode(value, { stream: true });
          const lines = pending.split("\n");
          pending = lines.pop()!;
          for (const l of lines.filter((l) => l.startsWith("data: "))) log.push({ at: tick++, line: `<- ${describe(JSON.parse(l.slice(6)), true)}` });
        }
      })());
    }
    return response;
  };
  const connect = async (capabilities: Record<string, object>, answerWith?: (name: string) => void) => {
    const client = new Client({ name: "host", version: "1.0.0" }, { capabilities });
    if (capabilities.elicitation) client.setRequestHandler(ElicitRequestSchema, async (request) => (console.log(`the person is asked: ${request.params.message}`), { action: "accept" as const, content: { confirm: true } }));
    if (capabilities.sampling) client.setRequestHandler(CreateMessageRequestSchema, async (request) => (console.log(`the client's model is asked: ${(request.params.messages[0].content as any).text}`), { role: "assistant" as const, content: { type: "text" as const, text: "Checkout is faster." }, model: "scripted" }));
    await client.connect(new StreamableHTTPClientTransport(new URL(url), { fetch: recordingFetch }));
    return client;
  };
  const textOf = (r: any) => r.content[0].text;
  const client = await connect({ elicitation: {}, sampling: {} });
  for (const [name, args] of [["deploy", { service: "api", env: "production" }], ["deploy", { service: "api", env: "staging" }], ["release_notes", { service: "api" }]] as const) {
    log.length = 0;
    const result = await client.callTool({ name, arguments: args });
    await Promise.all(reads);
    console.log(`${name}${"env" in args ? " " + args.env : ""} -> ${textOf(result)}`);
    log.sort((a, b) => a.at - b.at).forEach((e) => console.log(`   ${e.line}`));
  }
  console.log("the first request was initialize:", sawInitialize ? "True" : "False", "and every later request carried a session id:", sessionOnEvery ? "True" : "False");
  await client.close();
  http.close();
  const bare = await (async () => {
    const second = await serveOnLoopback(buildServer());
    const c = new Client({ name: "host", version: "1.0.0" });
    await c.connect(new StreamableHTTPClientTransport(new URL(second.url)));
    const result: any = await c.callTool({ name: "deploy", arguments: { service: "api", env: "production" } });
    await c.close();
    second.http.close();
    return result;
  })();
  console.log("a client that cannot be asked ->", bare.isError ? "a tool error" : "a result", "that says so:", textOf(bare).includes("cannot be asked") ? "True" : "False");
  http.closeAllConnections();
}

if (import.meta.main) {
  await main();
  process.exit(0);
}
