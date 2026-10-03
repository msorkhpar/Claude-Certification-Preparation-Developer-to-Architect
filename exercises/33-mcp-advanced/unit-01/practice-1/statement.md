# Practice: an MCP server's side of a multi round-trip request

In the MCP revision of 2026-07-28 a server no longer calls the client in the middle of a request. When a tool needs a person's
answer or a model completion, it ends the call with an `input_required` result that lists what it needs, and the client retries the
same call with the answers. The server keeps nothing between the two calls, so whatever it needs to remember travels in an opaque
`requestState` that the client echoes back. That state crosses a client the server does not trust, so it must be signed, bound to
the user and the call, and given an expiry. Write the tool handler that does this. Pick your language folder (`python`, `typescript`,
`java` or `kotlin`), open `starter/` and edit the file there. The message shapes, the rules for `requestState`, the error codes and
the capability checks come from the MCP specification of revision 2026-07-28 (the multi round-trip requests, tools, elicitation and
sampling pages), read on 2026-10-03; the lesson pages explain them. Nothing here uses a network or an MCP SDK: the handler is a
function from the parameters of a request to the result, which is what the SDKs call underneath.

## The given parts

| Name | Meaning |
|---|---|
| request | the parameters of a `tools/call`: `{"name", "arguments", "_meta", optionally "inputResponses" and "requestState"}`; `_meta` holds the protocol version under `io.modelcontextprotocol/protocolVersion` and the capabilities the client declares under `io.modelcontextprotocol/clientCapabilities` |
| `TOOLS`, `VERSION`, `TTL_SECONDS` | the two tool definitions (`deploy` and `status`), the one protocol version served, and the lifetime of a state: 300 seconds |
| `mint_state(secret, payload)` | a `requestState` for the payload, signed with the secret |
| `read_state(secret, token)` | the payload of a `requestState`, or nothing when it is not one this secret signed (changed, cut short, signed by another secret, or not a state at all) |
| `args_digest(arguments)` | a fingerprint of the arguments, the same in any key order |

## What to write

- `list_tools(request)`: a request whose version is not `VERSION` is a protocol error (below). Otherwise `{"resultType": "complete",
  "tools": TOOLS sorted by name, "ttlMs": 300000, "cacheScope": "public"}`.
- `call_tool(request, secret, principal, now)` where `principal` is the authenticated user and `now` is the time in seconds. It returns
  a result, or a protocol error `{"error": {"code", "message", optionally "data"}}`. Check in this order:
  1. Version not `VERSION`: code `-32022`, message `Unsupported protocol version`, `data` `{"supported": [VERSION]}`.
  2. A tool name other than `deploy` and `status`: `-32602`, `Unknown tool: NAME`.
  3. `arguments.service` missing, not a string or only spaces: `-32602`, `Invalid params: service is required`. For `deploy`, an `env`
     that is not `staging` or `production`: `-32602`, `Invalid params: env must be staging or production`.
  4. `status` completes with the text `SERVICE: running`. `deploy` to `staging` completes with `Deployed SERVICE to staging`. Both
     need no input from the client.
  - A complete result is `{"resultType": "complete", "content": [{"type": "text", "text": ...}], "isError": false}`.
  - `deploy` to `production` needs a person's confirmation by elicitation. The client can be asked only when it declared an
    `elicitation` capability that is empty (which means form mode) or holds `form`; a capability with only `url`, or none, cannot be
    asked. Then the result is complete with `isError` true and the text `Deploying to production needs confirmation, and this client
    cannot be asked.` and nothing is requested.
  - Otherwise there are up to two steps, kept in the signed state, not in the server:
    - Step `confirm`: with no usable answer, return `{"resultType": "input_required", "inputRequests": {"confirm": ...},
      "requestState": ...}`. The request is `{"method": "elicitation/create", "params": {"mode": "form", "message": "Deploy SERVICE to
      production?", "requestedSchema": {"type": "object", "properties": {"confirm": {"type": "boolean", "title": "Confirm the
      deployment"}}, "required": ["confirm"]}}}`. An answer is usable when `inputResponses.confirm` is an object whose `action` is
      `accept`, `decline` or `cancel`; anything else, or a missing answer, is asked again with a fresh state. Only `accept` with
      `content.confirm` exactly true goes on; every other usable answer completes with `Deployment cancelled` (and `isError` false).
      After a confirmation, a client that declared a `sampling` capability gets step `notes`; one that did not completes with
      `Deployed SERVICE to production`.
    - Step `notes`: ask with `{"notes": {"method": "sampling/createMessage", "params": {"messages": [{"role": "user", "content":
      {"type": "text", "text": "Write one sentence of release notes for SERVICE."}}], "maxTokens": 100}}}`. The answer is usable when
      `inputResponses.notes.content.text` is a string; the result is then `Deployed SERVICE to production. Release notes: TEXT`.
      An unusable or missing answer is asked again.
  - The state payload is `{"v": 1, "tool", "digest": args_digest(arguments), "sub": principal, "exp": now + TTL_SECONDS, "step"}`;
    `step` is the step that the next call must answer (`confirm` or `notes`).
  - A call with no `requestState` starts at step `confirm` and ignores any `inputResponses`: an answer alone never skips the question.
    A call with a `requestState` is checked in this order, with code `-32602`: a state that `read_state` does not accept: `Invalid
    requestState`; `now` later than `exp`: `Expired requestState`; a `sub`, `tool` or `digest` that differs from this call:
    `requestState does not match this request`. A valid state sets the step. Unknown keys in `inputResponses` are ignored.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A production deploy takes three round trips and keeps no state |
| `e1` | A staging deploy and a status call finish at once, and the tool list is cacheable |
| `e2` | The server only asks what the client declared it can answer |
| `e3` | A no or a missing answer is handled without an error |
| `e4` | A state the server did not sign is refused |
| `e5` | A state works only for the same user, the same call and before it expires |
| `e6` | A request the server cannot serve is a protocol error with a code |
| `e7` | The state carries the whole context, and an answer alone never skips a step |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
