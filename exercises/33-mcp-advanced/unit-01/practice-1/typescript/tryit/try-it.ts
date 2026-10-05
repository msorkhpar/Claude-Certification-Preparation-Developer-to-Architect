// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { callTool } from "./mrtr.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const SECRET = "s3cret";
const NOW = 1000;
const META = { "io.modelcontextprotocol/protocolVersion": "2026-07-28",
  "io.modelcontextprotocol/clientCapabilities": { elicitation: {}, sampling: {} } };

const request = (extra: Record<string, unknown> = {}) =>
  ({ name: "deploy", arguments: { service: "api", env: "production" }, _meta: META, ...extra });

// Round trip 1: the server needs a person's confirmation, so it ends the call with input_required and a signed state.
const first = callTool(request(), SECRET, "alice", NOW) ?? {};
console.log("first call:", first.resultType, "| asks for:", Object.keys(first.inputRequests ?? {}).join(", "));
console.log("state is a string:", typeof first.requestState === "string");

// Round trip 2: the client retries the same call with the answer and echoes the state back.
const answer = { confirm: { action: "accept", content: { confirm: true } } };
const second = callTool(request({ inputResponses: answer, requestState: first.requestState }), SECRET, "alice", NOW + 10) ?? {};
console.log("second call:", second.resultType, "| asks for:", Object.keys(second.inputRequests ?? {}).join(", "));
