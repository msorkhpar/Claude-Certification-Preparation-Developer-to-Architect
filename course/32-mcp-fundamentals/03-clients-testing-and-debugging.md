# MCP clients, testing a server and debugging it

**Level:** Developer · **Module 32:** MCP fundamentals · **Page 3 of 3**
**Exams:** DV5; A2.4

**After this page you can** connect an SDK client to a server, test a server the way a host meets it, name the Inspector's three clients, trace the usual startup and connection failures, and say what the practice builds and which cases it grades.

Checked against the Model Context Protocol documentation and specification (revision 2026-07-28) on 2026-10-03. The client calls, the wire exchange and the practice ran offline in the course container with the SDK versions on the previous page. The Inspector was not run in the container, because it needs Node and a package download: its description on this page is the documentation's.

## Why it matters

A server that passes its own unit tests can still fail in front of a host: it prints to stdout, it relies on a variable the host never passes, it opens a file by a relative path from a directory nobody chose. These faults live between the server and the client, so the test has to put a client in front of the server. The exam asks which tool to reach for and what to check first when a connection fails.

## The idea

### A client in code

An SDK client is the same component a host creates, with the same life cycle: it starts the server as a subprocess for stdio (or opens a connection for HTTP), connects, then lists and uses what the server offers. In Python it is `Client` with the server's command, in TypeScript `Client` with a `StdioClientTransport`, in Java `McpClient.sync` with a `StdioClientTransport` and in Kotlin `Client` with the Kotlin SDK's transport. The previous page's example shows the calls: read the server's name and capabilities, `list_tools`, `call_tool`, `read_resource` and `get_prompt`.

Writing your own client is the right way to test a server, because it is the only test that shows what a host receives. The course's tests start the notes server as a separate process and connect the SDK's own client to it. A direct call to a handler checks its logic, and it cannot see the schema, the framing, the stdout rule or the exact text the client receives. Two details of the real client matter in a test. A tool error arrives as a result whose error flag is set, so a test reads the flag and the text and does not wait for an exception. And some SDKs add the tool's name in front of an error message, so a test checks that the message contains your text instead of comparing it whole.

### The Inspector

The reference developer tool is the MCP Inspector. It ships as one package, `@modelcontextprotocol/inspector`, "providing three clients behind one binary":

| Client | What it is for |
|---|---|
| Web | A full graphical inspector in the browser. The default, and the richest surface. |
| CLI | A scriptable, machine-readable client for CI, shell pipelines, and coding agents. |
| TUI | An interactive terminal UI, for when a browser isn't available or wanted. |

The Inspector requires Node 22.19.0 or newer and runs directly through npx. No installation is required. You give it the command that launches your server, or point it at a remote one with `--server-url`, and then you can invoke tools, prompts and resources and watch the traffic. The debugging guide says: "This should be your first stop." Every client behind it connects the same way, with the same transports and configuration, so a result in the web client can be reproduced in a CI job with the CLI.

### Where a server fails in front of a host

The debugging guide names a short list of causes, and most are in the environment the host creates.

- **Working directory.** The directory of a server that a client launches from its configuration "may be undefined (like / on macOS) since the client could be started from anywhere". The guide's examples use Claude Desktop's configuration file, and "the same principles apply to any stdio-based MCP client". It says to use absolute paths in the configuration and in `.env` files, and notes that testing from the command line uses the directory where you run the command, which is why a server can pass there and fail in a host.
- **Environment variables.** "MCP servers launched over stdio inherit only a limited subset of environment variables automatically (the exact set is platform-dependent)." To override the defaults or add your own, set an `env` key in the host's configuration.
- **Startup.** A wrong path to the executable, a missing file, a permission, invalid JSON in the configuration, a missing environment variable.
- **Logging.** "Local MCP servers should not log messages to stdout (standard out), as this will interfere with protocol operation." Messages on stderr are captured by the host. For a server on Streamable HTTP, stderr is not captured by the client, so use your own log aggregation or OpenTelemetry. Logging over the protocol (`notifications/message`) is deprecated as of 2026-07-28.

When a server fails to connect, the guide gives an order of work: "Check client logs", "Verify server process is running", "Test standalone with Inspector", then "Verify protocol compatibility" and the per-request metadata. Three errors identify the common cases in the revision of 2026-07-28:

- An `UnsupportedProtocolVersionError` (code -32022) "lists the server's supported versions in its data field". Call `server/discover` to see them.
- A request missing either required metadata field, the protocol version or the client capabilities, is rejected with error -32602 (Invalid params). A request missing either required field is rejected with error -32602, the same code that many other malformed inputs get.
- If the server needs a capability that the request's client capabilities did not declare, such as elicitation, it returns a `MissingRequiredClientCapabilityError` (code -32021) naming the missing capabilities. It is returned as an error with its own code, so the client can tell it from a failed tool.

### The practice: a notes server

The practice is in `exercises/32-mcp-fundamentals/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin, each with its own SDK. You write a server named `notes`: the tools `add_note` and `search_notes`, the resource `notes://count`, the template `notes://note/{id}` and the prompt `review_notes`. The tests are a real client against your server as a separate process. They grade, among other things, that a refused call is a tool error and not a protocol error, that a search ignores letter case and keeps id order, that the read-only hint is set only on the search, that a missing note is a protocol error that says `No note {id}`, and that a failed call does not use up an id. With no match the search answers `No notes match "{query}"`, "which is not an error". The protocol error that says `No note {id}` is for reading a note that does not exist, and a search that finds nothing is a normal answer. The starter fails all eight cases, the reference passes them, and each of the seven planted wrong solutions fails on an assertion of the case it breaks.

## Traps

1. **Testing only the handlers.** A direct call does not see stray output, the schema or the transport. Start the server as a process and connect a client.
2. **Trusting a path that works in your shell.** A host may launch the server from a different directory with a smaller environment. Use absolute paths and set the variables the server needs in the host's configuration.
3. **Comparing an error message whole.** An SDK can add a prefix. Check that the message contains your text.
4. **Reading a failure as one cause.** A version mismatch, a missing capability and a missing metadata field are three different errors with three different codes. Read the code before you change anything.

## Quiz

1. A server's tools are not showing up in a host, and the team cannot tell why. Which first step does the documentation recommend?
   - **a**: Reinstall the host application, since a corrupt cache hides tools
   - **b**: Write the server again in another language to rule out an SDK defect there
   - **c**: Add output to stdout so that the host can see the server's progress
   - **d**: Try it standalone in the Inspector, with no other application involved

2. A stdio server started by a host cannot find credentials that work in the developer's shell. Which documented behavior explains it?
   - **a**: The host wipes the whole environment on purpose, to keep secrets away from servers
   - **b**: Credentials are read from the shell only when the transport used is HTTP
   - **c**: The server receives its variables only after the first tool call has arrived
   - **d**: Only a limited subset is inherited, so extra variables go in the config

3. A CI job must exercise a server without a browser and print machine-readable results. Which Inspector client fits?
   - **a**: The web one, run headless with the default settings
   - **b**: The terminal-UI one, since it prints results as JSON
   - **c**: None, because the Inspector can only run interactively
   - **d**: The scriptable command-line mode, built for pipelines

<details>
<summary>Answer key</summary>

1. **d**. The page says "Test it standalone with the Inspector" is one of the documented steps and that the guide calls it "This should be your first stop." *a* is ruled out because the documented steps check the server rather than the host: "Verify server process is running". *b* is ruled out because the steps go through logs, the process and "Test standalone with Inspector" before anything is rewritten. *c* is ruled out because "Local MCP servers should not log messages to stdout (standard out), as this will interfere with protocol operation."
2. **d**. The page quotes the guide: "MCP servers launched over stdio inherit only a limited subset of environment variables automatically (the exact set is platform-dependent)." *a* is ruled out because servers "inherit only a limited subset of environment variables automatically", which is some of them, not none. *b* is ruled out because the fix is to "set an env key in the host's configuration", whatever the transport. *c* is ruled out because the variables are fixed when the process is launched: "MCP servers launched over stdio inherit only a limited subset of environment variables automatically".
3. **d**. The page's table says the CLI is "A scriptable, machine-readable client for CI, shell pipelines, and coding agents." *a* is ruled out because the web one is "A full graphical inspector in the browser. The default, and the richest surface." *b* is ruled out because the terminal one is "An interactive terminal UI, for when a browser isn't available or wanted." *c* is ruled out because the CLI is "A scriptable, machine-readable client for CI, shell pipelines, and coding agents."

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A server works in the Inspector, but the host cannot start it from its configuration file, which gives a relative path to the data folder. What does the documentation blame?
   - **a**: A host can launch only servers that were first registered with the Inspector
   - **b**: Relative paths can resolve only when the server runs over Streamable HTTP
   - **c**: A host can pass along no environment variables, so the data path is lost
   - **d**: The directory can be undefined, so locations should be written in full

2. Under the 2026-07-28 revision, a server needs to ask the user a question in the middle of a call, but the incoming call declared no matching feature. What does the server return?
   - **a**: An unsupported-version error that lists the versions it accepts
   - **b**: An invalid-params error, since the metadata fields are absent
   - **c**: A tool result flagged as an error, saying the user cannot be reached
   - **d**: A missing-capability error, naming whatever the metadata omitted

3. A team must build an MCP server that speaks the newest protocol version. Which of the four SDKs used in the course reached that version when it was checked?
   - **a**: All four, since they share one specification and release together
   - **b**: Only the Kotlin library, because it is the newest of the four
   - **c**: Only the Java library, which the documentation ranks in its top tier overall
   - **d**: Only the Python library; the other three stop at an earlier revision

4. A query matches no stored entries. How does the course's notes server answer?
   - **a**: With a tool error that tells the model to ask the user for a new query
   - **b**: With a protocol error, as for an unknown tool, so that the client logs it
   - **c**: With a resource error for the missing note, as for a bad id
   - **d**: With ordinary text saying that nothing fits, and the error flag stays off

<details>
<summary>Answer key</summary>

1. **d**. The third page quotes the guide: the directory of a launched server "may be undefined (like / on macOS) since the client could be started from anywhere", so use absolute paths. *a* is ruled out because the guide says "the same principles apply to any stdio-based MCP client", and no registration with the Inspector exists. *b* is ruled out because "testing from the command line uses the directory where you run the command", which is the contrast, and the transport is not part of it. *c* is ruled out because servers "inherit only a limited subset of environment variables automatically", which is some of them and not none.
2. **d**. The third page says that if the server needs a capability that the request's client capabilities did not declare, "it returns a MissingRequiredClientCapabilityError (code -32021) naming the missing capabilities". *a* is ruled out because an unsupported-version error is one that "lists the server's supported versions in its data field", and the version was fine here. *b* is ruled out because "A request missing either required field is rejected with error -32602", and the required fields were sent. *c* is ruled out because the error is "returned as an error with its own code", so a client can tell it from a failed tool.
3. **d**. The first page says the Python SDK speaks both eras, and that the other three "contain no support for 2026-07-28, and their latest revision is 2025-11-25". *a* is ruled out because three of them "contain no support for 2026-07-28". *b* is ruled out because the Kotlin SDK is one of the three whose "latest revision is 2025-11-25". *c* is ruled out because "Java is Tier 2 and Kotlin is Tier 3", and the Java SDK stops at the earlier revision as well.
4. **d**. The third page says that with no match the search answers `No notes match "{query}"`, "which is not an error". *a* is ruled out because the answer is "which is not an error", so there is no tool error to read. *b* is ruled out because protocol errors "indicate issues with the request structure itself that models are less likely to be able to fix", and the request here is valid. *c* is ruled out because the protocol error that says `No note {id}` is "for reading a note that does not exist", and "a search that finds nothing is a normal answer".

</details>
