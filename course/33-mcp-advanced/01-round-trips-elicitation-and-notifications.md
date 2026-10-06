# MCP advanced: multi round-trip requests, elicitation, sampling and notifications

**Level:** Developer · **Module 33:** MCP advanced · **Page 1 of 2**
**Exams:** DV5

**After this page you can** explain how a server asks a client for input under the 2026-07-28 revision, say what `requestState` is and what the server must do with it, choose between the two elicitation modes, state what happened to sampling, and name the notification and cancellation mechanisms.

Checked against the Model Context Protocol specification and documentation, revision 2026-07-28, read on 2026-10-03. The example ran offline in the course container over a loopback interface, with the SDK versions named on the next page: the Python SDK `mcp` 2.2.0 speaks the 2026-07-28 revision, and the TypeScript SDK 1.31.0 speaks 2025-11-25, so the two outputs below differ on purpose. Nothing was sent over a real network.

## Why it matters

A tool often needs something from the other side in the middle of its work: a person's confirmation before a deployment, a model's help to write a summary, a missing parameter. Until the revision of 2026-07-28 the server did that by sending its own request to the client on an open connection, which tied each conversation to one server instance. The revision replaced it with a pattern that keeps no state on the server. The exam asks how the pattern works, what the server must protect, and which parts of the older feature set are going away.

## The idea

### Multi round-trip requests

The specification states the change without softening it: servers must send server-to-client requests such as `roots/list`, `sampling/createMessage` or `elicitation/create` using the multi round-trip (MRTR) pattern, and "The previous pattern of server-initiated requests is no longer supported." It is a breaking change. The pattern "provides a standardized way to handle these server-requests without requiring a shared storage layer across server instances or requiring stateful load balancing". Four steps make up the flow.

1. "Client sends an initial request to the server with the parameters needed to perform the operation."
2. The server decides that it needs more and answers with an **input-required result** instead of the final one.
3. The client gathers the information, from the user or elsewhere, "then retries the original request including the additional requested information".
4. The server now has enough and answers with the final result.

Only three client requests can be answered this way: `tools/call`, `resources/read` and `prompts/get`. The input-required result has two parts, and the server must include at least one.

- `inputRequests`: a map from server-chosen keys to requests the client must fulfil: an elicitation, a sampling request or a roots request. The client answers in an `inputResponses` map with the same keys.
- `requestState`: "An opaque string meaningful only to the server." The rule for the client is "the client MUST echo back the exact value of that field when retrying the original request", and "Clients MUST NOT inspect, parse, modify, or make any assumptions about its contents." If the result carries no state, the client must not invent one. Both fields affect only the retry: "They MUST NOT be used for any other request that the client may be sending in parallel."

The pattern "allows servers to request additional information without maintaining any server-side state". The retry is a new request with a new JSON-RPC `id`. That is the point of the design: "the requests in each step are completely independent: the server processing the retry does not need any information beyond what is directly present in the retry request." Whatever the server needs to remember travels in `requestState`.

### What the server owes the state

The state crosses a client that the server does not control, so the specification says "servers MUST treat requestState as an attacker-controlled input". If it influences authorization, access or business logic, the server must protect its integrity (an HMAC, or authenticated encryption) and reject state that fails verification. To stop replay the server should put three things inside the protected payload and check each on receipt: the authenticated principal, "rejecting state presented by a different principal"; a short expiry; and an identifier of the originating request, such as the method name and a digest of its parameters. The specification adds a limit: these measures "bound the replay window and prevent cross-user and cross-request reuse, but do not by themselves guarantee single-use". A one-time redemption must be enforced on the server side. The practice on the next page builds exactly this.

Two more rules shape the handler. A server must not ask for what the client did not declare: "Servers MUST NOT send an inputRequests that the client has not declared support for in its capabilities." A server that needs a capability the call did not declare returns a `MissingRequiredClientCapabilityError` with code -32021, which names the missing capabilities. And a missing or unusable answer is not an error: the server "SHOULD respond with a new InputRequiredResult requesting the missing information again, rather than returning an error".

### Elicitation: forms and URLs

Elicitation lets a server ask a person for more information, such as a confirmation. A client declares the `elicitation` capability and supports at least one of two modes.

- **Form mode**: the server sends a message and a schema, the client builds a form and validates the answer. The schema is limited to a flat object.
- **URL mode**: the server gives a URL that the person opens out of band, and the data never passes through the client. It is for secrets and for third-party authorization.

The line between them is a rule: "Servers MUST NOT use form mode elicitation to request sensitive information such as" passwords, API keys, access tokens or payment credentials. Those belong in URL mode, "which keeps the data out of band so it never passes through the client or the LLM context". A person's answer has three forms, and a server handles each: accept (with the data in form mode), decline (an explicit no) and cancel (a dismissal). A server must not send a mode the client does not support. In the practice a client counts as able to be asked only when its elicitation capability is empty, which means form mode, or holds `form`. A capability that holds only `url`, or none, cannot be asked, and the call then completes with an error result that says the client cannot be asked, and nothing is requested.

### Sampling, roots and logging are deprecated

Sampling lets a server ask the client's model for a completion, without a key of its own. In 2026-07-28 it is deprecated, together with roots and logging over the protocol. For sampling the specification says: "New implementations SHOULD NOT adopt it; existing implementations SHOULD migrate to integrating directly with LLM provider APIs." Deprecated features stay in the specification for at least twelve months and then become eligible for removal; the registry gives 2027-07-28 as the earliest. The migration for roots is to pass directories through tool parameters, resource URIs or configuration, and for logging to write to stderr (stdio) or use OpenTelemetry. The course still teaches sampling, because the Python SDK still implements it and the example shows it, and because the exam asks what it is.

### Notifications, progress and cancellation

Change notifications are opt-in. A client opens a long-lived `subscriptions/listen` stream with a filter that names what it wants: tools, prompts or resources list changes, or updates to listed resource URIs. "The server MUST NOT send notification types the client has not explicitly requested", and its first message on the stream is an acknowledgement of the subset it will honor. The request replaces the former `resources/subscribe` call.

Progress is request-scoped. A client that wants updates for a request puts a `progressToken` in its metadata, and the server may send progress notifications that carry the token, a value that must increase with each one, and optionally a total and a message. Those flow only on the response stream of that request.

Cancellation depends on the transport. On stdio the client sends a `notifications/cancelled` message that names the request. On Streamable HTTP "Closing the SSE response stream" is the signal, and no cancel message is expected. The specification asks implementations to set timeouts for all requests.

### The example: a tool that asks twice

The example runs a small server and a client over Streamable HTTP on the loopback interface. The tool `deploy` needs a person's confirmation for production, and `release_notes` asks the client's model for one sentence. Scripted callbacks stand in for the person and the model, and a log of the `tools/call` requests shows what crossed the wire.

<!-- example: m33-streamable-http-mrtr tabs: python,typescript,java,kotlin -->
```python
"""A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.

The server is `mcp` 2.2.0 and the client is the same SDK with scripted callbacks in place of a person and a model. This SDK speaks the
2026-07-28 revision: the server never calls the client in the middle of a request; it ends the call with an input-required result and
the client retries with the answers. The log under the program output is what the HTTP hook of the client saw on the wire.
Checked on 2026-10-03 against the "Multi Round-Trip Requests" page of the MCP specification.
"""
import logging
import asyncio
import json
import logging
import socket
import threading
import time
from typing import Annotated

import httpx2
import uvicorn
from mcp import Client
from mcp.client.streamable_http import streamable_http_client
from mcp.server.elicitation import ElicitationResult
from mcp.server.mcpserver import Context, MCPServer
from mcp.server.mcpserver.resolve import Elicit, Resolve, Sample
from mcp.shared.exceptions import MCPError
from mcp_types import CreateMessageResult, ElicitResult, SamplingMessage, TextContent
from pydantic import BaseModel

log = logging.getLogger(__name__)


class Confirm(BaseModel):
    confirm: bool


def build_server():
    server = MCPServer("deployer", version="1.0.0")

    def ask_confirmation(service: str, env: str) -> Elicit[Confirm] | Confirm:
        return Elicit(f"Deploy {service} to production?", Confirm) if env == "production" else Confirm(confirm=True)

    @server.tool(description="Deploy a service; production needs a person's confirmation.")
    async def deploy(service: str, env: str, answer: Annotated[ElicitationResult[Confirm], Resolve(ask_confirmation)], ctx: Context) -> str:
        if answer.action != "accept" or not answer.data.confirm:
            return "Deployment cancelled"
        return f"Deployed {service} to {env}"

    def ask_model(service: str) -> Sample:
        return Sample([SamplingMessage(role="user", content=TextContent(type="text", text=f"Write one sentence of release notes for {service}."))], max_tokens=100)

    @server.tool(description="Write release notes with the client's model.")
    async def release_notes(service: str, completion: Annotated[CreateMessageResult, Resolve(ask_model)]) -> str:
        return f"{service}: {completion.content.text}"

    return server


def serve_on_loopback(server):
    probe = socket.socket()
    probe.bind(("127.0.0.1", 0))
    port = probe.getsockname()[1]
    probe.close()
    http = uvicorn.Server(uvicorn.Config(server.streamable_http_app(json_response=True), host="127.0.0.1", port=port, log_level="error"))
    threading.Thread(target=http.run, daemon=True).start()
    while not http.started:
        time.sleep(0.02)
    return http, f"http://127.0.0.1:{port}/mcp"


def describe(body):
    """One line for a JSON-RPC message that crossed the wire."""
    if "method" in body:
        params = body.get("params") or {}
        extra = [k for k in ("inputResponses", "requestState") if k in params]
        return f"{body['method']}{' ' + params['name'] if 'name' in params else ''}{' with ' + ' and '.join(extra) if extra else ''}"
    result = body.get("result") or {}
    if "error" in body:
        return f"error {body['error']['code']}"
    if result.get("resultType") == "input_required":
        kinds = sorted(r["method"] for r in result.get("inputRequests", {}).values())
        return f"input_required: asks for {', '.join(kinds)}" + (", with a requestState" if "requestState" in result else "")
    return "complete" + (f": {result['content'][0]['text']}" if "content" in result else "")


async def main():
    logging.disable(logging.INFO)  # the SDK and the HTTP server log every request at INFO; this program prints its own log
    http, url = serve_on_loopback(build_server())
    wire = []

    async def on_response(response):
        await response.aread()
        request = json.loads(response.request.content)
        if request.get("method") == "tools/call":  # the SDK's own housekeeping requests (server/discover, tools/list) are left out of this log
            wire.append(("->", request))
            wire.append(("<-", response.json()))

    async def person(context, params):
        print(f"the person is asked: {params.message}")
        return ElicitResult(action="accept", content={"confirm": True})

    async def model(context, params):
        print(f"the client's model is asked: {params.messages[0].content.text}")
        return CreateMessageResult(role="assistant", content=TextContent(type="text", text="Checkout is faster."), model="scripted", stop_reason="endTurn")

    def transport():
        return streamable_http_client(url, http_client=httpx2.AsyncClient(event_hooks={"response": [on_response]}))

    async with Client(transport(), elicitation_callback=person, sampling_callback=model) as client:
        for name, arguments in (("deploy", {"service": "api", "env": "production"}), ("deploy", {"service": "api", "env": "staging"}), ("release_notes", {"service": "api"})):
            wire.clear()
            result = await client.call_tool(name, arguments)
            print(f"{name} {arguments['env'] if 'env' in arguments else ''}".rstrip(), "->", result.content[0].text)
            for direction, body in wire:
                print(f"   {direction} {describe(body)}")
    async with Client(transport()) as bare:  # no callbacks: this client declares no elicitation or sampling capability
        wire.clear()
        try:
            await bare.call_tool("deploy", {"service": "api", "env": "production"})
        except MCPError as e:
            print("a client that cannot be asked ->", f"error {e.code}", "(MissingRequiredClientCapability)")
    http.should_exit = True


if __name__ == "__main__":
    asyncio.run(main())
```
```text
the person is asked: Deploy api to production?
deploy production -> Deployed api to production
   -> tools/call deploy
   <- input_required: asks for elicitation/create, with a requestState
   -> tools/call deploy with inputResponses and requestState
   <- complete: Deployed api to production
deploy staging -> Deployed api to staging
   -> tools/call deploy
   <- complete: Deployed api to staging
the client's model is asked: Write one sentence of release notes for api.
release_notes -> api: Checkout is faster.
   -> tools/call release_notes
   <- input_required: asks for sampling/createMessage, with a requestState
   -> tools/call release_notes with inputResponses and requestState
   <- complete: api: Checkout is faster.
a client that cannot be asked -> error -32021 (MissingRequiredClientCapability)
```
```typescript
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
import { logger } from "./logger.ts";
const log = logger("mrtr_http");

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
```
```text
the person is asked: Deploy api to production?
deploy production -> Deployed api to production
   -> tools/call deploy
   <- elicitation/create (a request from the server)
   -> the answer to the server's request
   <- complete: Deployed api to production
deploy staging -> Deployed api to staging
   -> tools/call deploy
   <- complete: Deployed api to staging
the client's model is asked: Write one sentence of release notes for api.
release_notes -> api: Checkout is faster.
   -> tools/call release_notes
   <- sampling/createMessage (a request from the server)
   -> the answer to the server's request
   <- complete: api: Checkout is faster.
the first request was initialize: True and every later request carried a session id: True
a client that cannot be asked -> a tool error that says so: True
```
```java
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import reactor.core.publisher.Mono;

/**
 * A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.
 *
 * <p>The server and the client are `io.modelcontextprotocol.sdk:mcp` 2.0.1 (the server runs in an embedded Tomcat), with scripted callbacks in
 * place of a person and a model. This SDK speaks the 2025-11-25 revision, so the flow differs from the Python example next to it: there is an
 * `initialize` handshake and a session id, and the server calls the client in the middle of the request, as a request of its own on the
 * response stream. The log under the program output is what the client's transport sent and received, one JSON-RPC message at a time.
 * Checked on 2026-10-04 against the "Streamable HTTP" page of the MCP specification.
 */
public final class MrtrHttp {
    private static final System.Logger LOG = System.getLogger(MrtrHttp.class.getName());
    static final Map<String, Object> CONFIRM_SCHEMA = Map.of("type", "object", "properties", Map.of("confirm", Map.of("type", "boolean", "title", "Confirm the deployment")), "required", List.of("confirm"));

    static McpSchema.CallToolResult text(String text, boolean isError) {
        return McpSchema.CallToolResult.builder().addTextContent(text).isError(isError).build();
    }

    static SyncToolSpecification deploy() {
        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("service", Map.of("type", "string"), "env", Map.of("type", "string")), "required", List.of("service", "env"));
        McpSchema.Tool tool = McpSchema.Tool.builder("deploy", schema).description("Deploy a service; production needs a person's confirmation.").build();
        return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
            String service = (String) request.arguments().get("service"), env = (String) request.arguments().get("env");
            if (env.equals("production")) {
                if (exchange.getClientCapabilities().elicitation() == null) return text("Deploying to production needs confirmation, and this client cannot be asked.", true);
                McpSchema.ElicitResult answer = exchange.createElicitation(new McpSchema.ElicitFormRequest("Deploy " + service + " to production?", CONFIRM_SCHEMA, null));
                if (answer.action() != McpSchema.ElicitResult.Action.ACCEPT || !Boolean.TRUE.equals(answer.content().get("confirm"))) return text("Deployment cancelled", false);
            }
            return text("Deployed " + service + " to " + env, false);
        }).build();
    }

    static SyncToolSpecification releaseNotes() {
        Map<String, Object> schema = Map.of("type", "object", "properties", Map.of("service", Map.of("type", "string")), "required", List.of("service"));
        McpSchema.Tool tool = McpSchema.Tool.builder("release_notes", schema).description("Write release notes with the client's model.").build();
        return SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
            String service = (String) request.arguments().get("service");
            McpSchema.CreateMessageResult completion = exchange.createMessage(McpSchema.CreateMessageRequest.builder(
                List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent("Write one sentence of release notes for " + service + "."))), 100).build());
            return text(service + ": " + ((McpSchema.TextContent) completion.content()).text(), false);
        }).build();
    }

    /** The MCP endpoint on 127.0.0.1: an embedded Tomcat on a free port, with one session-aware transport for the one client of this program. */
    record Running(Tomcat tomcat, String url) implements AutoCloseable {
        @Override
        public void close() throws Exception {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    static Running serveOnLoopback() throws Exception {
        Logger.getLogger("org.apache").setLevel(Level.WARNING);
        HttpServletStreamableServerTransportProvider transport = HttpServletStreamableServerTransportProvider.builder().jsonMapper(McpJsonDefaults.getMapper()).mcpEndpoint("/mcp").build();
        McpServer.sync(transport).serverInfo("deployer", "1.0.0").capabilities(McpSchema.ServerCapabilities.builder().tools(false).build()).tools(deploy(), releaseNotes()).build();
        Tomcat tomcat = new Tomcat();
        String base = Files.createTempDirectory("mrtr-tomcat").toString();
        tomcat.setBaseDir(base);
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        Context context = tomcat.addContext("", base);
        Tomcat.addServlet(context, "mcp", transport).setAsyncSupported(true);
        context.addServletMappingDecoded("/mcp", "mcp");
        tomcat.start();
        return new Running(tomcat, "http://127.0.0.1:" + tomcat.getConnector().getLocalPort());
    }

    /** One line for a JSON-RPC message that crossed the wire. */
    @SuppressWarnings("unchecked")
    static String describe(McpSchema.JSONRPCMessage message, boolean fromServer) {
        if (message instanceof McpSchema.JSONRPCRequest request) {
            Object name = request.params() instanceof Map<?, ?> p ? p.get("name") : request.params() instanceof McpSchema.CallToolRequest c ? c.name() : null;
            return request.method() + (name != null ? " " + name : "") + (fromServer ? " (a request from the server)" : "");
        }
        if (message instanceof McpSchema.JSONRPCResponse response) {
            if (response.error() != null) return "error " + response.error().code();
            if (fromServer && response.result() instanceof Map<?, ?> r && r.get("content") instanceof List<?> content) return "complete: " + ((Map<String, Object>) content.get(0)).get("text");
            return "the answer to the server's request";
        }
        return ((McpSchema.JSONRPCNotification) message).method();
    }

    /** What the client's transport carried, in the order it happened, and whether every request after the first carried the session id. */
    static final class WireLog {
        final List<String> lines = Collections.synchronizedList(new ArrayList<>());
        final Set<Object> callIds = java.util.concurrent.ConcurrentHashMap.newKeySet();
        volatile boolean sawInitialize = false, sessionOnEvery = true;

        /** Wraps a transport so that each tools/call, each request from the server and each response is logged as it passes. */
        McpClientTransport around(McpClientTransport inner) {
            return new McpClientTransport() {
                @Override
                public Mono<Void> connect(Function<Mono<McpSchema.JSONRPCMessage>, Mono<McpSchema.JSONRPCMessage>> handler) {
                    return inner.connect(incoming -> handler.apply(incoming.doOnNext(message -> {
                        if (message instanceof McpSchema.JSONRPCRequest || (message instanceof McpSchema.JSONRPCResponse r && callIds.contains(r.id()))) lines.add("<- " + describe(message, true));
                    })));
                }

                @Override
                public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
                    if (message instanceof McpSchema.JSONRPCRequest request && request.method().equals("tools/call")) {
                        callIds.add(request.id());
                        lines.add("-> " + describe(message, false));
                    } else if (message instanceof McpSchema.JSONRPCResponse) {
                        lines.add("-> " + describe(message, false));
                    }
                    return inner.sendMessage(message);
                }

                @Override
                public Mono<Void> closeGracefully() {
                    return inner.closeGracefully();
                }

                @Override
                public <T> T unmarshalFrom(Object data, TypeRef<T> typeRef) {
                    return inner.unmarshalFrom(data, typeRef);
                }

                @Override
                public List<String> protocolVersions() {
                    return inner.protocolVersions();
                }

                @Override
                public void setExceptionHandler(Consumer<Throwable> handler) {
                    inner.setExceptionHandler(handler);
                }
            };
        }

        /** Looks at each HTTP request the transport is about to send: the first must be initialize, the others must carry a session id. */
        void check(java.net.http.HttpRequest.Builder builder, String method, java.net.URI endpoint, String body) {
            boolean hasSession = builder.build().headers().firstValue("mcp-session-id").isPresent();
            if (body != null && body.contains("\"method\":\"initialize\"")) sawInitialize = true;
            else if (!hasSession) sessionOnEvery = false;
        }
    }

    static McpSyncClient connect(String url, WireLog log, Function<McpSchema.ElicitFormRequest, McpSchema.ElicitResult> person, Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> model) {
        HttpClientStreamableHttpTransport http = HttpClientStreamableHttpTransport.builder(url).endpoint("/mcp")
            .httpRequestCustomizer((builder, method, endpoint, body, context) -> log.check(builder, method, endpoint, body)).build();
        McpClient.SyncSpec spec = McpClient.sync(log.around(http)).clientInfo(new McpSchema.Implementation("host", "1.0.0"));
        if (person != null) spec.elicitation(person);
        if (model != null) spec.sampling(model);
        McpSyncClient client = spec.build();
        client.initialize();
        return client;
    }

    static String textOf(McpSchema.CallToolResult result) {
        return ((McpSchema.TextContent) result.content().get(0)).text();
    }

    static String py(boolean value) {
        return value ? "True" : "False";
    }

    public static void main(String[] args) throws Exception {
        WireLog log = new WireLog();
        Function<McpSchema.ElicitFormRequest, McpSchema.ElicitResult> person = request -> {
            System.out.println("the person is asked: " + request.message());
            return new McpSchema.ElicitResult(McpSchema.ElicitResult.Action.ACCEPT, Map.of("confirm", true));
        };
        Function<McpSchema.CreateMessageRequest, McpSchema.CreateMessageResult> model = request -> {
            System.out.println("the client's model is asked: " + ((McpSchema.TextContent) request.messages().get(0).content()).text());
            return new McpSchema.CreateMessageResult(McpSchema.Role.ASSISTANT, new McpSchema.TextContent("Checkout is faster."), "scripted", McpSchema.CreateMessageResult.StopReason.END_TURN);
        };
        try (Running server = serveOnLoopback()) {
            McpSyncClient client = connect(server.url(), log, person, model);
            Object[][] calls = {{"deploy", Map.of("service", "api", "env", "production")}, {"deploy", Map.of("service", "api", "env", "staging")}, {"release_notes", Map.of("service", "api")}};
            for (Object[] call : calls) {
                @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) call[1];
                log.lines.clear();
                McpSchema.CallToolResult result = client.callTool(new McpSchema.CallToolRequest((String) call[0], arguments));
                System.out.println(call[0] + (arguments.containsKey("env") ? " " + arguments.get("env") : "") + " -> " + textOf(result));
                synchronized (log.lines) {
                    log.lines.forEach(line -> System.out.println("   " + line));
                }
            }
            System.out.println("the first request was initialize: " + py(log.sawInitialize) + " and every later request carried a session id: " + py(log.sessionOnEvery));
            client.closeGracefully();
        }
        try (Running second = serveOnLoopback()) {
            McpSyncClient bare = connect(second.url(), new WireLog(), null, null);
            McpSchema.CallToolResult result = bare.callTool(new McpSchema.CallToolRequest("deploy", Map.of("service", "api", "env", "production")));
            System.out.println("a client that cannot be asked -> " + (Boolean.TRUE.equals(result.isError()) ? "a tool error" : "a result") + " that says so: " + py(textOf(result).contains("cannot be asked")));
            bare.closeGracefully();
        }
        System.exit(0);
    }
}
```
```text
the person is asked: Deploy api to production?
deploy production -> Deployed api to production
   -> tools/call deploy
   <- elicitation/create (a request from the server)
   -> the answer to the server's request
   <- complete: Deployed api to production
deploy staging -> Deployed api to staging
   -> tools/call deploy
   <- complete: Deployed api to staging
the client's model is asked: Write one sentence of release notes for api.
release_notes -> api: Checkout is faster.
   -> tools/call release_notes
   <- sampling/createMessage (a request from the server)
   -> the answer to the server's request
   <- complete: api: Checkout is faster.
the first request was initialize: True and every later request carried a session id: True
a client that cannot be asked -> a tool error that says so: True
```
```kotlin
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.plugin
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpMethod
import io.ktor.server.application.install
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttpTransport
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.shared.TransportSendOptions
import io.modelcontextprotocol.kotlin.sdk.types.BooleanSchema
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ClientCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequest
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CreateMessageResult
import io.modelcontextprotocol.kotlin.sdk.types.ElicitRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.ElicitResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCError
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCNotification
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCRequest
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCResponse
import io.modelcontextprotocol.kotlin.sdk.types.Method
import io.modelcontextprotocol.kotlin.sdk.types.RequestId
import io.modelcontextprotocol.kotlin.sdk.types.Role
import io.modelcontextprotocol.kotlin.sdk.types.SamplingMessage
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.StopReason
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val log = System.getLogger("mrtr_http")

/**
 * A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.
 *
 * The server and the client are `io.modelcontextprotocol:kotlin-sdk` 0.15.0 (the server runs on Ktor's CIO engine), with scripted callbacks
 * in place of a person and a model. This SDK speaks the 2025-11-25 revision, so the flow differs from the Python example next to it: there
 * is an `initialize` handshake and a session id, and the server calls the client in the middle of the request, as a request of its own on
 * the response stream. The log under the program output is what the client's transport sent and received, one JSON-RPC message at a time.
 * Checked on 2026-10-04 against the "Streamable HTTP" page of the MCP specification.
 */
private fun text(text: String, isError: Boolean) = CallToolResult(content = listOf(TextContent(text)), isError = isError)

private fun property(type: String) = buildJsonObject { put("type", type) }

fun buildServer(): Server {
    val server = Server(Implementation("deployer", "1.0.0"), ServerOptions(ServerCapabilities(tools = ServerCapabilities.Tools(false))))
    server.addTool(
        "deploy", "Deploy a service; production needs a person's confirmation.",
        ToolSchema(properties = buildJsonObject { put("service", property("string")); put("env", property("string")) }, required = listOf("service", "env")),
    ) { request ->
        val service = request.arguments!!["service"]!!.jsonPrimitive.content
        val env = request.arguments!!["env"]!!.jsonPrimitive.content
        if (env == "production") {
            val answer = try {
                createElicitation("Deploy $service to production?", ElicitRequestParams.RequestedSchema(properties = mapOf("confirm" to BooleanSchema(title = "Confirm the deployment")), required = listOf("confirm")))
            } catch (e: IllegalStateException) { // the SDK refuses to ask a client that did not declare elicitation
                return@addTool text("Deploying to production needs confirmation, and this client cannot be asked.", true)
            }
            if (answer.action != ElicitResult.Action.Accept || answer.content?.get("confirm")?.jsonPrimitive?.booleanOrNull != true) return@addTool text("Deployment cancelled", false)
        }
        text("Deployed $service to $env", false)
    }
    server.addTool(
        "release_notes", "Write release notes with the client's model.",
        ToolSchema(properties = buildJsonObject { put("service", property("string")) }, required = listOf("service")),
    ) { request ->
        val service = request.arguments!!["service"]!!.jsonPrimitive.content
        val completion = createMessage(CreateMessageRequest(CreateMessageRequestParams(
            maxTokens = 100, messages = listOf(SamplingMessage(Role.User, listOf(TextContent("Write one sentence of release notes for $service.")))))))
        text("$service: ${(completion.content[0] as TextContent).text}", false)
    }
    return server
}

/** The MCP endpoint on 127.0.0.1: Ktor's CIO engine on a free port. */
class Running(val engine: EmbeddedServer<*, *>, val url: String) : AutoCloseable {
    override fun close() = engine.stop(0, 0)
}

suspend fun serveOnLoopback(): Running {
    val engine = embeddedServer(ServerCIO, port = 0, host = "127.0.0.1") { mcpStreamableHttp { buildServer() } }.startSuspend(wait = false)
    return Running(engine, "http://127.0.0.1:${engine.engine.resolvedConnectors().first().port}/mcp")
}

/** One line for a JSON-RPC message that crossed the wire. */
fun describe(message: JSONRPCMessage, fromServer: Boolean): String = when (message) {
    is JSONRPCRequest -> message.method + (message.params?.jsonObject?.get("name")?.jsonPrimitive?.content?.let { " $it" } ?: "") + if (fromServer) " (a request from the server)" else ""
    is JSONRPCResponse -> if (fromServer && message.result is CallToolResult) "complete: " + ((message.result as CallToolResult).content[0] as TextContent).text else "the answer to the server's request"
    is JSONRPCError -> "error ${message.error.code}"
    is JSONRPCNotification -> message.method
    else -> ""
}

/** What the client's transport carried, in the order it happened, and whether every request after the first carried the session id. */
class WireLog {
    val lines = CopyOnWriteArrayList<String>()
    private val callIds = java.util.concurrent.ConcurrentHashMap.newKeySet<RequestId>()
    @Volatile var sawInitialize = false
    @Volatile var sessionOnEvery = true
    private val first = AtomicBoolean(true)

    /** Wraps a transport so that each tools/call, each request from the server and each response is logged as it passes. */
    fun around(inner: Transport) = object : Transport by inner {
        override fun onMessage(block: suspend (JSONRPCMessage) -> Unit) = inner.onMessage { message ->
            if (message is JSONRPCRequest || (message is JSONRPCResponse && message.id in callIds)) lines += "<- ${describe(message, true)}"
            block(message)
        }

        override suspend fun send(message: JSONRPCMessage, options: TransportSendOptions?) {
            if (message is JSONRPCRequest && message.method == Method.Defined.Initialize.value) sawInitialize = true
            if (message is JSONRPCRequest && message.method == Method.Defined.ToolsCall.value) {
                callIds += message.id
                lines += "-> ${describe(message, false)}"
            } else if (message is JSONRPCResponse) {
                lines += "-> ${describe(message, false)}"
            }
            inner.send(message, options)
        }
    }

    private val eventStreamOpen = CompletableDeferred<Unit>()

    /** Looks at each HTTP request the client is about to send: the first is the initialize, the others must carry a session id. */
    fun attach(http: HttpClient) {
        http.plugin(HttpSend).intercept { request ->
            if (!first.getAndSet(false) && request.headers["mcp-session-id"] == null) sessionOnEvery = false
            val call = execute(request)
            if (request.method == HttpMethod.Get) eventStreamOpen.complete(Unit) // the server has answered the client's request for its event stream
            call
        }
    }

    /**
     * This server answers each call with plain JSON, so a request that it makes in the middle of a call can only travel on the client's
     * standalone event stream (a GET that the client opens on its own after the handshake). A caller waits for that stream before the first call.
     */
    suspend fun awaitEventStream() = withTimeout(10_000) { eventStreamOpen.await() }
}

suspend fun connect(url: String, log: WireLog, person: ((ElicitRequestParams) -> ElicitResult)?, model: ((CreateMessageRequest) -> CreateMessageResult)?): Client {
    val http = HttpClient(ClientCIO) {
        install(SSE)
        install(HttpTimeout) { requestTimeoutMillis = 120_000 } // the engine's default of 15 seconds also applies to a stream that stays open
    }
    log.attach(http)
    val capabilities = ClientCapabilities(
        sampling = if (model != null) ClientCapabilities.Sampling() else null,
        elicitation = if (person != null) ClientCapabilities.Elicitation(form = JsonObject(emptyMap())) else null)
    val client = Client(Implementation("host", "1.0.0"), ClientOptions(capabilities = capabilities))
    if (person != null) client.setElicitationHandler { request -> person(request.params) }
    if (model != null) client.setRequestHandler<CreateMessageRequest>(Method.Defined.SamplingCreateMessage) { request, _ -> model(request) }
    client.connect(log.around(http.mcpStreamableHttpTransport(url)))
    log.awaitEventStream()
    return client
}

fun textOf(result: CallToolResult) = (result.content[0] as TextContent).text

fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
  System.setProperty("kotlin-logging.logStartupMessage", "false") // the SDK's logging library would otherwise print one line to stdout
  runBlocking {
    val log = WireLog()
    val person = { request: ElicitRequestParams ->
        println("the person is asked: ${request.message}")
        ElicitResult(ElicitResult.Action.Accept, buildJsonObject { put("confirm", true) })
    }
    val model = { request: CreateMessageRequest ->
        println("the client's model is asked: ${(request.params.messages[0].content[0] as TextContent).text}")
        CreateMessageResult(Role.Assistant, listOf(TextContent("Checkout is faster.")), "scripted", StopReason.EndTurn)
    }
    serveOnLoopback().use { server ->
        val client = connect(server.url, log, person, model)
        for ((name, arguments) in listOf("deploy" to mapOf("service" to "api", "env" to "production"), "deploy" to mapOf("service" to "api", "env" to "staging"), "release_notes" to mapOf("service" to "api"))) {
            log.lines.clear()
            val result = client.callTool(name, arguments)
            println("$name${if ("env" in arguments) " " + arguments["env"] else ""} -> ${textOf(result)}")
            log.lines.forEach { println("   $it") }
        }
        println("the first request was initialize: ${py(log.sawInitialize)} and every later request carried a session id: ${py(log.sessionOnEvery)}")
        client.close()
    }
    serveOnLoopback().use { second ->
        val bare = connect(second.url, WireLog(), null, null)
        val result = bare.callTool("deploy", mapOf("service" to "api", "env" to "production"))
        println("a client that cannot be asked -> ${if (result.isError == true) "a tool error" else "a result"} that says so: ${py("cannot be asked" in textOf(result))}")
        bare.close()
    }
  }
  exitProcess(0)
}
```
```text
the person is asked: Deploy api to production?
deploy production -> Deployed api to production
   -> tools/call deploy
   <- elicitation/create (a request from the server)
   -> the answer to the server's request
   <- complete: Deployed api to production
deploy staging -> Deployed api to staging
   -> tools/call deploy
   <- complete: Deployed api to staging
the client's model is asked: Write one sentence of release notes for api.
release_notes -> api: Checkout is faster.
   -> tools/call release_notes
   <- sampling/createMessage (a request from the server)
   -> the answer to the server's request
   <- complete: api: Checkout is faster.
the first request was initialize: True and every later request carried a session id: True
a client that cannot be asked -> a tool error that says so: True
```
<!-- /example -->

Read the two outputs side by side. In the Python output, the first request ends with an input-required result that asks for an `elicitation/create` and carries a `requestState`. The second `tools/call` is the retry, with `inputResponses` and the state, and it is answered with the final result. A staging deploy needs no input, and it completes in one round trip. A client that did not declare elicitation gets the -32021 error. The TypeScript output shows the other era, because the TypeScript SDK in the course speaks 2025-11-25: the server sends its own `elicitation/create` request on the stream of the call and waits for the client's answer, the first request of the conversation is `initialize`, and every later request carries a session id. A client that cannot be asked gets a tool error that says so. Same tools, same answers, and two different wires. A server written for 2026-07-28 does not work this way, and a server written for 2025-11-25 does.

The Java and Kotlin tabs print the same lines as the TypeScript tab, because the JVM MCP SDKs (`io.modelcontextprotocol.sdk:mcp` 2.0.1, with the server in an embedded Tomcat, and `io.modelcontextprotocol:kotlin-sdk` 0.15.0, on Ktor) also speak 2025-11-25. The log in those tabs is the JSON-RPC messages that the client's transport sent and received, taken from a wrapper around the transport; the session-id check looks at each HTTP request the client is about to send. One difference in how it is built: the Kotlin SDK's server answers each call with plain JSON, so a request that it makes in the middle of a call can only travel on the client's standalone event stream, and the Kotlin tab waits for that stream to open before its first call (the Java SDK's server ties the request to the call's own stream).

## Traps

1. **Reading the state.** The client must echo `requestState` byte for byte. A client that decodes it, edits it or reuses it for another request breaks the contract, and a server that trusts it without a signature has let the client write its own authorization.
2. **Asking what the client did not declare.** Check the capability before building an input request, and complete with an error result, or return the -32021 error, when it cannot be asked.
3. **Turning a "no" into an error.** An answer of decline or cancel is a normal outcome. Complete the call with a result that says it was cancelled.
4. **Collecting a secret in a form.** Passwords, keys, tokens and payment credentials go through URL mode, so that they never pass through the client.

## Quiz

1. Under the 2026-07-28 revision, a tool needs a person's answer halfway through a call. How does the server get it?
   - **a**: It sends the client its own request on an open stream and waits for the reply
   - **b**: It pauses the call and holds the connection open until the client answers
   - **c**: It stores the question in a session, which the client resumes later
   - **d**: It returns an input-required result, and the client repeats the request with the replies

2. A client receives an opaque string called requestState together with an input-required result. What must it do with that string?
   - **a**: Send back the exact same value on the retry, without reading it
   - **b**: Discard it, since only the answers to the questions matter on the retry
   - **c**: Keep it in a cookie so that other calls running in parallel can reuse it
   - **d**: Decode it to learn what the server wants, then adjust it before the retry

3. A server must collect a payment card number from a user during a tool call. Which mode of elicitation may it use?
   - **a**: The form one, since the client validates every field against a schema
   - **b**: Either one, as long as the client shows the form to the person first
   - **c**: The out-of-band one, where the person visits a separate web page
   - **d**: Neither one, since a server may never request data from a person

<details>
<summary>Answer key</summary>

1. **d**. The page says the server answers with an input-required result, and the client "then retries the original request including the additional requested information". *a* is ruled out because "The previous pattern of server-initiated requests is no longer supported." *b* is ruled out because the pattern works "without requiring a shared storage layer across server instances or requiring stateful load balancing", which a held connection would need. *c* is ruled out because the pattern allows "servers to request additional information without maintaining any server-side state", so there is no session to resume.
2. **a**. The page says "Clients MUST NOT inspect, parse, modify, or make any assumptions about its contents", and that the client must echo the exact value on the retry. *d* is ruled out because "Clients MUST NOT inspect, parse, modify, or make any assumptions about its contents." *b* is ruled out because "the client MUST echo back the exact value of that field when retrying the original request", and the retry needs it, since the server keeps nothing else. *c* is ruled out because both fields "MUST NOT be used for any other request that the client may be sending in parallel".
3. **c**. The page says "Servers MUST NOT use form mode elicitation to request sensitive information", and that such interactions belong in URL mode. *a* is ruled out because "Servers MUST NOT use form mode elicitation to request sensitive information", whatever the schema checks. *b* is ruled out because URL mode "keeps the data out of band so it never passes through the client or the LLM context", and a form always passes through the client. *d* is ruled out because "Elicitation lets a server ask a person for more information", with URL mode for sensitive data.

</details>
