# MCP advanced: Streamable HTTP, state and authorization

**Level:** Developer · **Module 33:** MCP advanced · **Page 2 of 2**
**Exams:** DV5

**After this page you can** describe the Streamable HTTP transport of 2026-07-28, say what replaced protocol sessions and how a server keeps state safely, name the checks a remote server owes its callers (origin, audience, scope), and say what the practice builds and grades.

Checked against the Model Context Protocol specification and security best practices, revision 2026-07-28, read on 2026-10-03. The practice ran offline in the course container in Python, TypeScript, Java and Kotlin; the handler it asks for is a function from the parameters of a request to the result, with no network and no SDK. The authorization flow itself (the browser redirects, the token endpoint) was not run: this page reports what the specification requires of a server and a client.

## Why it matters

A local server on stdio trusts its parent process. A remote one trusts nobody, and it is reachable by anyone who can send an HTTP request. The revision of 2026-07-28 also removed the session that older servers used to remember a client. The exam asks what the transport looks like now, where state goes when there is no session, and which checks keep a remote server from becoming somebody else's deputy.

## The idea

### Streamable HTTP in 2026-07-28

The server "exposes a single HTTP endpoint" (the MCP endpoint) that accepts POST, and the client sends every JSON-RPC request or notification as its own POST. For each request the server answers with either a single JSON object or a Server-Sent Events stream scoped to that request. That stream carries notifications that relate to the request and ends with the final response. The client must accept both forms. The revision changed the transport in two ways: the GET stream endpoint was removed, and so were protocol-level sessions. Resumable streams through `Last-Event-ID` are not supported.

Because every POST stands alone, the transport mirrors a few fields into HTTP headers so that load balancers and gateways can route without reading the body. Each request carries an `MCP-Protocol-Version` header that must match the version in the body's metadata, and `Mcp-Method`, with `Mcp-Name` for calls that name a tool, a resource or a prompt. A mismatch is a `400` with a `HeaderMismatch` error. A server that does not support the version answers `400` with the list of versions it does support, and an unknown method is a `404` with the JSON-RPC code -32601.

Server-to-client interaction does not need a second channel any more. A server "MUST NOT send independent JSON-RPC requests on this stream". Elicitation and the rest ride inside the input-required result of the previous page, and long-lived change notifications arrive on the response stream of a `subscriptions/listen` request. Closing the response stream is cancellation, and the server must stop work and send nothing more for that request.

Three security rules apply to every Streamable HTTP server. "Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks." If the header is present and invalid, the server must answer `403 Forbidden`. A server that runs locally should bind to localhost (127.0.0.1) and not to all interfaces, and servers should implement authentication for all connections. Without these, "attackers could use DNS rebinding to interact with local MCP servers from remote websites."

### State without a session

MCP in this revision "is stateless and has no protocol-level sessions". A server that needs state across several requests mints an explicit handle, such as a shopping cart id or a workflow id, and gets it back as an ordinary tool argument. That makes the handle a target. The security guide describes state handle hijacking: an unauthorized party obtains or guesses a handle and uses it to read or change another user's state. The mitigations are all on the server. "MCP servers MUST NOT treat possession of a state handle as authentication." Handles should come from a secure random generator, never a counter, and may expire. And "MCP servers SHOULD bind handles server-side to the authenticated user", for example by keying stored state as the user id and the handle, with the user id taken from the verified token and not from the client.

The same idea protects `requestState`, as the previous page showed. There the state travels in the message itself, so it is signed and carries the principal, the expiry and the digest of the call. For a handle the state stays on the server and the key does the work. Both answer one question: who is allowed to present this?

### Authorization

Authorization is optional in MCP, and it depends on the transport. "Authorization is OPTIONAL for MCP implementations." For HTTP-based transports, implementations should follow the specification. "Implementations using an STDIO transport SHOULD NOT follow this specification, and instead retrieve credentials from the environment." The flow is OAuth 2.1, and the roles are plain: a protected MCP server is an OAuth resource server, the MCP client is an OAuth client acting for a resource owner, and an authorization server issues the tokens.

What the specification asks of the parties, in the order a connection meets them:

- **Discovery.** A server must implement OAuth 2.0 Protected Resource Metadata, so that a client learns which authorization server to use. A challenge (`401`) can carry a `scope` parameter that tells the client which scopes this operation needs.
- **Registration.** Client ID Metadata Documents are the preferred way for a client to get an ID. Dynamic Client Registration is deprecated in this revision and stays for servers that do not support the documents.
- **The resource parameter.** A client must send the `resource` parameter, naming the canonical URI of the MCP server, in both the authorization request and the token request. That binds the token to a server.
- **PKCE.** A client must use PKCE with the `S256` method, and must refuse to continue when the authorization server's metadata does not show PKCE support.
- **Every request.** The token goes in the `Authorization` header of every HTTP request, and "Access tokens MUST NOT be included in the URI query string".
- **Audience.** "MCP servers MUST validate that access tokens were issued specifically for them as the intended audience". An invalid or expired token gets `401`.

The last item has a name, token passthrough, and the security guide says "Token passthrough is explicitly forbidden in the authorization specification". A server that accepts tokens issued for another service, or forwards the client's token to a downstream API, breaks the security boundary, and it can become a confused deputy: the downstream API trusts the call as though the MCP server had validated it. The rule is short: "MCP servers MUST NOT accept or transit any other tokens." If the server needs to call a downstream API, it uses a token of its own for that API.

Scope design is the last control. A server that lists every scope in `scopes_supported`, and a client that requests them all, gives a stolen token a wide reach and users a consent dialog nobody reads. The guidance is a progressive, least-privilege model: a small initial set of low-risk read operations, and more scope by a targeted challenge when a privileged operation is first attempted.

### The practice: the server side of a multi round-trip call

The practice is in `exercises/33-mcp-advanced/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write the handler of a `deploy` and a `status` tool: protocol errors in a fixed order (an unsupported version, an unknown tool, bad parameters), a production deploy that asks for a confirmation and then, for a client that declared sampling, for release notes, and a `requestState` that is signed, bound to the user and the call, and expires. The server keeps nothing between the calls. The tests judge a three-round-trip deployment, a client that cannot be asked, a "no" and a missing answer, a state the server did not sign, a state used by another user or after its expiry or for another call, and an answer that arrives with no state and must not skip the question. The starter fails all eight cases, the reference passes them, and each of the seven planted wrong solutions fails on an assertion. The module's SDK examples hide the state handling, and the practice shows it by hand.

## Traps

1. **Treating a signed state as single-use.** The signature, the principal, the expiry and the call digest stop forgery and cross-user reuse. They do not stop a replay by the same user inside the lifetime, so a one-time redemption needs a server-side check.
2. **Accepting a token that was not made for you.** Validate the audience, and never forward the caller's token downstream.
3. **Trusting a handle because it exists.** A cart id or a workflow id is not a login. Bind it to the authenticated user.
4. **Skipping the Origin check on a local HTTP server.** Binding to localhost is not enough, because a web page in the user's browser can still reach it through DNS rebinding.

## Quiz

1. An MCP server on Streamable HTTP receives a request whose Origin header shows an unrelated site. What does the specification require?
   - **a**: Accept it, since the Origin header is advisory and only logged
   - **b**: Answer 403 Forbidden, which blocks DNS rebinding attacks
   - **c**: Close the stream of the next response so that the sender retries
   - **d**: Redirect it to the authorization server so that the sender can authenticate

2. An MCP server accepts a credential that was meant for another service and passes it unchanged to a downstream API. What does the specification say?
   - **a**: Allowed, since the downstream API validates the token on its own
   - **b**: Allowed, once the user has consented to the downstream call
   - **c**: Required, so that the downstream API sees the original audience
   - **d**: Forbidden, since only tokens minted for the receiver are honored

3. A server issues a workflow identifier in a tool result and later acts on whoever presents it. Which attack does this invite?
   - **a**: None, because MCP sessions already bind each handle to its caller
   - **b**: Hijacking, because having the handle is accepted as proof of identity
   - **c**: Scope inflation, because the handle carries more permissions than needed
   - **d**: Cross-site scripting, because the identifier is echoed back in a result

<details>
<summary>Answer key</summary>

1. **b**. The page says "Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks", and that an invalid one gets 403 Forbidden. *a* is ruled out because "Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks." *d* is ruled out because "If the header is present and invalid, the server must answer 403 Forbidden", not a redirect. *c* is ruled out because "Closing the response stream is cancellation", which is how a client cancels a request, and it is not an answer to a bad Origin.
2. **d**. The page says "MCP servers MUST NOT accept or transit any other tokens." *a* is ruled out because "MCP servers MUST NOT accept or transit any other tokens." *b* is ruled out because "Token passthrough is explicitly forbidden", with no consent exception. *c* is ruled out because "MCP servers MUST validate that access tokens were issued specifically for them as the intended audience".
3. **b**. The page says "MCP servers MUST NOT treat possession of a state handle as authentication." *a* is ruled out because MCP in this revision "is stateless and has no protocol-level sessions". *d* is ruled out because state handle hijacking is "an unauthorized party obtains or guesses a handle and uses it to read or change another user's state". *c* is ruled out because the remedy is that "MCP servers SHOULD bind handles server-side to the authenticated user", which is about ownership and not about permissions.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A tool wants a person's confirmation, but the calling client declared only roots. What must the server avoid?
   - **a**: Returning a normal result that says the confirmation could not be requested
   - **b**: Including an elicitation request for a feature the caller never announced
   - **c**: Ending the call with an error code that names the missing capability
   - **d**: Signing the state of the call so that the client cannot tamper with it

2. Two requests carry the same requestState and arrive within its lifetime, one from the user it was issued to and one from a different user. How should the server treat them?
   - **a**: Reject the second, because the signed payload names its intended principal
   - **b**: Reject both, since a state may be presented only once
   - **c**: Accept both, because the expiry is the only check the specification requires
   - **d**: Accept both, because a valid signature proves the state is genuine

3. A load balancer sends consecutive calls of one client to different instances of an MCP server on the 2026-07-28 revision. What does the protocol need so that this works?
   - **a**: A self-contained request that carries everything the handler requires
   - **b**: A sticky connection that keeps one client on the same instance
   - **c**: A resumable stream that another instance can pick up by its event id
   - **d**: A session identifier that each instance looks up in a common table

4. A team plans HTTP-style login for an MCP server that runs as a local child process. What does the specification advise?
   - **a**: Follow the same flow, since authorization is mandatory for every transport
   - **b**: Use a query-string token, which stdio clients may append to the command
   - **c**: Skip that flow and read credentials from the environment instead
   - **d**: Ask the user for a password through form-mode elicitation on each start

<details>
<summary>Answer key</summary>

1. **b**. The first page says "Servers MUST NOT send an inputRequests that the client has not declared support for in its capabilities." *a* is ruled out because the practice's server "completes with an error result that says the client cannot be asked", so a normal result of that kind is allowed. *d* is ruled out because "servers MUST treat requestState as an attacker-controlled input", so signing it is required, not avoided. *c* is ruled out because a server that needs a capability the call did not declare "returns a MissingRequiredClientCapabilityError with code -32021", which is an error code.
2. **a**. The first page says the state should carry "the authenticated principal, rejecting state presented by a different principal". *d* is ruled out because the server must reject "rejecting state presented by a different principal" even when the signature is valid. *b* is ruled out because these measures "do not by themselves guarantee single-use", so the same user can present it again within the lifetime. *c* is ruled out because the server also checks "an identifier of the originating request", and the principal.
3. **a**. The first page says the pattern works "without requiring a shared storage layer across server instances or requiring stateful load balancing", and the second page that every POST stands alone. *d* is ruled out because MCP in this revision "is stateless and has no protocol-level sessions". *b* is ruled out because the pattern works "without requiring a shared storage layer across server instances or requiring stateful load balancing". *c* is ruled out because "Resumable streams through `Last-Event-ID` are not supported."
4. **c**. The second page quotes the specification: "Implementations using an STDIO transport SHOULD NOT follow this specification, and instead retrieve credentials from the environment." *a* is ruled out because "Authorization is OPTIONAL for MCP implementations." *b* is ruled out because "Access tokens MUST NOT be included in the URI query string". *d* is ruled out because "Servers MUST NOT use form mode elicitation to request sensitive information".

</details>
