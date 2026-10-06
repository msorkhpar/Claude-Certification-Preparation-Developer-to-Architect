# Server-sent events, websockets and connection basics

**Level:** Developer · **Module 13:** One REST API under every SDK · **Page 3 of 3**
**Exams:** DV1, DV2

**After this page you can** explain how a streamed Messages reply travels (server-sent events over one HTTP
request), say why that and not a websocket, and name the connection problems that long requests meet.

Checked against the Claude API documentation (Streaming messages, API versions, errors, and the Python, TypeScript
and Java SDK pages) on 2026-10-02. The framing shown was produced by the course harness and parsed by the real SDK
in the container (module 17 prints the result); the websocket description is general web background, not a Claude
documentation claim. No live API call was made.

## Why it matters

A user watches words appear in a chat box and a nightly job downloads a hundred-page report in one reply. Both are the
same HTTP API, and both can fail in ways that a short request never does: a network box between you and the service
quietly drops a connection that has been silent for a few minutes, or a proxy buffers the stream and the "live" text
arrives in one lump. The exam asks which transport the API uses and why, and which settings matter for long
requests. The answers are small and exact, and they decide whether a streaming feature works behind a corporate proxy.

## The idea

### Request and response, with a long response

HTTP is a request followed by a response. For most calls the response is one body that arrives complete. For a long
generation that is a poor fit: the server has produced the first words in a fraction of a second but would hold them
until the last word exists. The Messages API solves it without changing the protocol. You set `"stream": true` in the
request, the server answers with a normal HTTP response whose body is a **stream of server-sent events**, and the
connection stays open until the message is complete. The documentation puts it as setting `"stream": true` "to
incrementally stream the response using server-sent events (SSE)".

### What a server-sent event looks like

Server-sent events are plain text over HTTP with the content type `text/event-stream`. Each event is a few lines,
ended by a blank line. The Messages API uses **named events**: an `event:` line with the type and a `data:` line with
JSON. The versioning page records that this is the format of the version you use: "All events are named events",
completions are incremental, and the old `data: [DONE]` marker is gone.

<!-- illustrative -->
The start of a streamed reply, in the documented framing. Hand-written for this page; module 17 builds the whole
message from the events.

```text
HTTP/1.1 200 OK
content-type: text/event-stream

event: message_start
data: {"type": "message_start", "message": {"id": "msg_illustrative", "type": "message", "role": "assistant", ...}}

event: content_block_delta
data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "Hel"}}

event: content_block_delta
data: {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "lo"}}
```
<!-- /illustrative -->

Two properties of the format explain most of its engineering consequences. The stream is **one-directional**: after
the request is sent, the client only listens. And it is **ordinary HTTP**: headers, authentication, TLS, proxies,
load balancers and the SDK's retry machinery all work the way they do for any request, which is the reason the
design is simple to operate.

### Why not a websocket

A websocket is a different protocol: it starts as an HTTP request that asks to upgrade, then becomes a persistent
connection in which either side can send messages at any time. That is the right tool for chat rooms, collaborative
editing and games, where the client talks while the server talks. The Messages API does not need it. A model call is
one request and one long answer, so there is no client-to-server traffic to carry after the first message. The
trade-offs a team weighs:

| | Server-sent events | Websocket |
|---|---|---|
| Direction | Server to client, after one request | Both ways, any time |
| Transport | Plain HTTP response | An upgrade to another protocol |
| Proxies, auth headers, retries | Work as for any HTTP call | Need extra care at each hop |
| Reconnect | Start a new request | Reconnect and restore the session state |
| Fits the Messages API | Yes, it is what the API sends | Not what the API sends |

If an interface needs the client to send while the answer is still arriving (for example, to interrupt it), the
client cancels by closing the connection. The TypeScript SDK documents it as breaking out of the loop or calling
`stream.controller.abort()`, and a following request carries the next instruction.

### Connection basics for long requests

The documentation names the failure and the cures in one passage. "Some networks may drop idle connections after a
variable period of time, which can cause the request to fail or time out without receiving a response from
Anthropic." A long non-streaming request is silent on the wire until the whole answer exists, which is the worst case
for an idle-connection timeout. So:

1. **Stream long generations.** Bytes keep flowing, and the connection is not idle. The documentation says to avoid "a
   large `max_tokens` value without using the streaming Messages API or Message Batches API".
2. **Or use batches** when no one is waiting, because the Message Batches API "can help you manage the risk of
   network issues by allowing you to poll for results rather than requiring an uninterrupted network connection".
3. **Set a TCP keep-alive** if you write a direct integration. The SDKs already set the socket option for TCP keep-alive.
4. **Reuse connections.** The Java SDK's page tells you not to create more than one client in an application, because
   each has a connection pool and thread pools that are better shared. In Python, close a client you create, or use it
   as a context manager.
5. **Check what sits in the path.** A proxy that buffers responses turns a stream into one lump and defeats the
   purpose; a client that says streaming does not work behind a corporate proxy is describing buffering.

The SDKs add a safeguard on top: for a non-streaming request that is expected to take longer than about ten minutes,
the Python SDK raises a `ValueError`, and the TypeScript, Java and Go SDKs raise an error too; passing `stream=True` or overriding the
timeout disables it. That is a design hint and not an obstacle: if you see it, you wanted streaming.

## Traps

1. **Reaching for a websocket by habit.** A single prompt and a long answer need one request and one stream. A
   websocket adds an upgrade, a session to restore and trouble at every proxy, and gives nothing in return.
2. **Raising `max_tokens` without streaming.** The call is silent for as long as the generation takes, and an idle
   connection can be dropped before it ends. Streaming or a batch is the documented fix.
3. **Creating a client per request.** A new client means a new connection pool each time, and the cost shows up as
   latency and sockets. Create one and share it.

## Quiz

1. A team's report generator sets a very large `max_tokens` on a non-streaming call and now fails intermittently on
   the office network after several minutes without any reply. Which change is best?
   - **a**: Open a websocket so that the server can push the report when it is ready
   - **b**: Use server-sent events so that data arrives while the text is written
   - **c**: Raise the client timeout so that the call may stay silent for longer
   - **d**: Split the report into three prompts and send them over three connections

2. A developer proposes a websocket between a web page and the model API so that the page can send a stop signal
   during a long answer. Which design is best?
   - **a**: Send a second HTTP request that tells the model to stop generating
   - **b**: Send the stop signal as an event on the open stream of the reply
   - **c**: Switch to the websocket, because only that protocol allows a cancel
   - **d**: Keep the stream and close the connection when the user wants it to end

<details>
<summary>Answer key</summary>

1. **b**. Streaming keeps data moving: "Bytes keep flowing, and the connection is not idle". *a* is ruled out because "there is no client-to-server traffic to carry" in a model call, and a websocket is "not what the API sends". *c* is ruled out because "Some networks may drop idle connections after a variable period of time", so a longer timeout on your side does not help. *d* is ruled out because "the call is silent for as long as the generation takes", and each of three connections would be silent while its own part is written.
2. **d**. The stream runs on one ordinary HTTP request, and the client cancels "by closing the connection". *b* is ruled out because the stream is one-directional: "after the request is sent, the client only listens". *c* is ruled out because "a websocket adds an upgrade, a session to restore and trouble at every proxy, and gives nothing in return". *a* is ruled out because the page documents "breaking out of the loop or calling stream.controller.abort()", not a second request.

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A hand-written client and an SDK client call the API from the same service. After the provider adds a new stop
   reason value, only the hand-written client starts failing. Which cause is most likely?
   - **a**: It has no retries, which the SDK would have supplied automatically
   - **b**: Its logic assumes that the set of labels it saw is complete
   - **c**: It lacks the identifying platform headers that the SDK sends
   - **d**: It was written against an older revision of the contract header

2. A service builds a new SDK client for every streamed call, and its latency and open-socket count grow under
   load. Which change is best?
   - **a**: Make one instance at start-up and pass it to each request handler
   - **b**: Move the calls to a websocket so that one connection is kept open
   - **c**: Turn streaming off so that fewer sockets are used by each of the calls
   - **d**: Raise the output limit so that fewer calls are needed for the same work

3. A call that asks for incremental output returns 200 and a few events, and then the network drops. A developer
   records the call as successful because of the 200. What should the page's reader tell them?
   - **a**: The code shows only that the reply began, so the end must be checked as well
   - **b**: The code is final, since the server sends it after the whole message is written
   - **c**: The events already received form a complete message and can be stored as the result
   - **d**: The rest of the reply can be fetched later by the message id it already carries

4. A team logs every failing call so that it can open tickets with the provider's support. Which record should the
   service keep for each call?
   - **a**: The authorization header and the body, so that the call can be reproduced
   - **b**: The complete request and reply, so that support staff can replay them
   - **c**: The request id, the status and the error type, so staff can locate it
   - **d**: Only the time of the call, since the provider can find the call from it

<details>
<summary>Answer key</summary>

1. **b**. A client that fails on an unknown value breaks "on a change the versioning page allows", and the page says a stop reason is among the values "you do not know" that must be tolerated, not only a field. *a* is ruled out because the new value arrives in a 200, and "a successful reply is a JSON object", so there is no error for a retry to repeat. *c* is ruled out because the SDK's extra headers "help debugging and are not part of your contract". *d* is ruled out because "the only current value of anthropic-version is 2023-06-01", and an older one "may be unavailable for new users".
2. **a**. The page says to create one client and share it, because "a new client means a new connection pool each time". *b* is ruled out because "a websocket adds an upgrade, a session to restore and trouble at every proxy, and gives nothing in return". *c* is ruled out because streaming is what keeps long calls from being dropped as idle: "Bytes keep flowing, and the connection is not idle". *d* is ruled out because "the cost shows up as latency and sockets" and comes from the number of clients, which the output limit does not change.
3. **a**. The page's second trap says "a 200 does not mean the answer is complete", so the end of the stream has to be checked as well. *b* is ruled out because "the connection stays open until the message is complete", so the status comes first. *c* is ruled out because "a streamed reply can start with a 200 and still end with an error event". *d* is ruled out because "the Messages API is stateless, so the history is yours", and no stored reply waits to be fetched by its id.
4. **c**. The request id is what support asks for: "include it when you contact support about a specific request", and the status and type classify the failure without carrying a key or customer text. *b* is ruled out because "customer text does not belong in a log by default". *a* is ruled out because an error message that is logged "is read by people who must not see a key", and a header would carry the key. *d* is ruled out because the request id is "a globally unique identifier for the request", and a time stamp does not single out one call.

</details>
