# MCP: hosts, clients, servers and the three primitives

**Level:** Developer · **Module 32:** MCP fundamentals · **Page 1 of 3**
**Exams:** DV5; A2.4

**After this page you can** name the three participants of the Model Context Protocol and what each one does, say who controls a tool, a resource and a prompt, tell the data layer from the transport layer, and explain how the revision of 2026-07-28 differs from the earlier handshake-based ones.

Checked against the Model Context Protocol documentation and specification, revision 2026-07-28, read on 2026-10-03, and against the four SDKs the course uses (listed below, run offline in the course container). Nothing on this page was run against a remote server.

## Why it matters

Before MCP, each AI application wrote its own connector for each service. MCP is the agreement that removes that work: a service writes one server, and any application that speaks the protocol can use it. The exam asks which part does what, which primitive fits a given need, and what the protocol leaves to the application. The vocabulary on this page is the vocabulary of the next two pages and of module 33.

## The idea

### Three participants

MCP follows a client-server architecture. The documentation names three roles:

- **MCP Host**: the AI application that coordinates and manages one or multiple MCP clients. Claude Code and Claude Desktop are hosts.
- **MCP Client**: "A component that maintains a connection to an MCP server and obtains context from an MCP server for the MCP host to use".
- **MCP Server**: a program that provides context to MCP clients.

The host does not talk to a server itself. "The MCP host accomplishes this by creating one MCP client for each MCP server." Each client "maintains a dedicated connection with its corresponding MCP server", so a host connected to a ticketing server and a file server holds two clients. The word "server" says nothing about where the program runs. A filesystem server that a desktop application launches on the same machine is a local server, and the documentation's example of a remote one is a vendor's server on that vendor's platform. Local servers that use the stdio transport typically serve a single client, and remote servers on Streamable HTTP will typically serve many.

One boundary matters for the exam. MCP "does not dictate how AI applications use LLMs or manage the provided context." The protocol moves context between a server and a client. What the model does with it, and what a person must approve, belongs to the host.

### Two layers

The **data layer** is the inner one: a JSON-RPC 2.0 exchange that defines the messages, discovery, the primitives and the notifications. The **transport layer** is the outer one: how the messages travel and how a connection is made and authorized. There are two transports.

- **stdio**: the client launches the server as a subprocess and the two talk over its standard input and output, "providing optimal performance with no network overhead". Messages are one JSON-RPC message per line.
- **Streamable HTTP**: HTTP POST for client-to-server messages with optional Server-Sent Events for streaming, which "enables remote server communication and supports standard HTTP authentication methods". Module 33 covers it in detail.

The same JSON-RPC messages run on both, which is why a server written once can be offered either way.

### Three primitives, three controllers

A server offers three kinds of things, and each has a different party in charge of using it.

| Primitive | What it is | Who controls it |
|---|---|---|
| Tools | Functions that the model can call and decide to use based on the user's request | The model |
| Resources | Passive data sources with read-only access to information, such as a file or a database schema | The application |
| Prompts | Pre-built instruction templates | The user |

Tools "are model-controlled, meaning AI models can discover and invoke them automatically", which is why the documentation adds a human check: applications can show which tools are available, ask for approval for individual executions, pre-approve safe ones and keep an activity log. Resources are chosen by the application, which "can access this information directly and decide how to use it", whether by selecting parts, searching with embeddings or passing everything on. Each resource has a URI and a MIME type, and a template such as `travel://activities/{city}/{category}` describes a family of them. Prompts are picked by a person, for example from a menu: "Plan a vacation" or "Summarize my meetings".

Each kind has methods to discover and use it: `tools/list` and `tools/call`, `resources/list`, `resources/templates/list` and `resources/read`, and the equivalents for prompts. Listings are requests, so they can change with time.

A client has primitives of its own that a server can ask for. Elicitation lets a server ask the user for more information, such as a confirmation. In the revision of 2026-07-28, **sampling** (the server asks the client's model for a completion), **roots** and **logging over the protocol** are deprecated. The documentation's advice for new work is to call a model provider directly instead of sampling, and to log to stderr (stdio) or use OpenTelemetry. Module 33 explains elicitation and the pattern that carries it.

### Two eras of the protocol

Protocol versions are dates, and the current one is 2026-07-28. The revision changed how a conversation starts, and an engineer meets both forms.

- **Modern** versions (2026-07-28 and later) carry no handshake. "Every request declares the protocol version it is using" in its `_meta` field, together with the client's capabilities, and the server "accepts or rejects each request independently". A server must implement a `server/discover` request that lists its versions, capabilities and identity. A client "is free to invoke any RPC inline", or it can call discovery first. A server that does not support the requested version answers with an error that lists the versions it does support.
- **Legacy** versions (2025-11-25 and earlier) "establish a session with an initialize handshake".

The SDKs the course pins differ here, and the course checked it by running them. The Python SDK `mcp` 2.2.0 speaks both eras, with handshake versions up to 2025-11-25 and the modern 2026-07-28. The TypeScript SDK `@modelcontextprotocol/sdk` 1.31.0, the Java SDK `io.modelcontextprotocol.sdk:mcp` 2.0.1 and the Kotlin SDK `kotlin-sdk` 0.15.0 contain no support for 2026-07-28, and their latest revision is 2025-11-25. The documentation ranks the SDKs in tiers: TypeScript and Python are Tier 1, Java is Tier 2 and Kotlin is Tier 3. The two pages that follow use the handshake form of the protocol, because all four SDKs speak it.

## Traps

1. **Counting one client per host.** The host creates one client for each server, and each client holds one dedicated connection.
2. **Confusing who controls what.** A tool is chosen by the model, a resource by the application and a prompt by the user. Offering a database schema as a tool, or a "do this" action as a resource, puts it under the wrong controller.
3. **Reading "local" and "remote" as two kinds of protocol.** The messages are the same. Only the transport differs.
4. **Assuming every SDK speaks the newest revision.** Of the four SDKs the course uses, only the Python one reached 2026-07-28 when it was checked. Pin the version and read its notes.

## Quiz

1. An AI application connects to a ticketing system and a document store, both exposed through MCP. How is the connecting side organized?
   - **a**: A single client multiplexes both servers over one shared connection
   - **b**: The host talks to each server directly, with no client component
   - **c**: Each server creates a client inside itself to call the application back
   - **d**: The host creates one client for each server, each with a dedicated link

2. A developer offers three things: a function the model may call to book flights, a read-only data document that the application attaches as context, and a ready-made instruction that a user selects from a menu. Which primitives fit, in that order?
   - **a**: A tool, a resource, a prompt
   - **b**: A resource, a prompt, a tool
   - **c**: A tool, a prompt, a resource
   - **d**: A prompt, a tool, a resource

3. How does a client written for the 2026-07-28 revision tell a server which protocol version it speaks?
   - **a**: An initialize exchange that opens a session and fixes it for later calls
   - **b**: A header on the first call only, which the server remembers afterwards
   - **c**: Each call carries it in its metadata field, with no opening handshake
   - **d**: A mandatory discovery call that must precede every other call

<details>
<summary>Answer key</summary>

1. **d**. The page says "The MCP host accomplishes this by creating one MCP client for each MCP server", and that each client "maintains a dedicated connection with its corresponding MCP server". *a* is ruled out because each client "maintains a dedicated connection with its corresponding MCP server", so none is shared. *b* is ruled out because the client is "A component that maintains a connection to an MCP server and obtains context from an MCP server for the MCP host to use". *c* is ruled out because it is the host that does the creating: "The MCP host accomplishes this by creating one MCP client for each MCP server".
2. **a**. A function the model calls is a tool, because tools "are model-controlled, meaning AI models can discover and invoke them automatically". Read-only data that the application attaches is a resource, which the application "can access this information directly and decide how to use it". A template that a person picks is a prompt. *d* is ruled out because a resource is the one the application "can access this information directly and decide how to use it", so it cannot stand for the function the model calls. *b* is ruled out because tools "are model-controlled, meaning AI models can discover and invoke them automatically", so the first item is a tool. *c* is ruled out because the data document is a resource, which gives "read-only access to information", and the menu item is a prompt.
3. **c**. The page says "Every request declares the protocol version it is using" in its metadata field, and that the server "accepts or rejects each request independently". *a* is ruled out because the legacy versions "establish a session with an initialize handshake", and the modern ones have none. *b* is ruled out because the server "accepts or rejects each request independently", so nothing is remembered from a first call. *d* is ruled out because a client "is free to invoke any RPC inline", so discovery is optional.

</details>
