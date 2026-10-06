// An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md.
import { createHash, createHmac, timingSafeEqual } from "node:crypto";
import { logger } from "./logger.ts";

const log = logger("mrtr");

export const VERSION = "2026-07-28";
export const META_VERSION = "io.modelcontextprotocol/protocolVersion";
export const META_CAPS = "io.modelcontextprotocol/clientCapabilities";
export const TTL_SECONDS = 300;

export const TOOLS = [
  { name: "deploy", description: "Deploy a service to an environment. Production needs a person's confirmation.",
    inputSchema: { type: "object", properties: { service: { type: "string" }, env: { type: "string", enum: ["staging", "production"] } }, required: ["service", "env"] } },
  { name: "status", description: "Report whether a service is running.",
    inputSchema: { type: "object", properties: { service: { type: "string" } }, required: ["service"] } },
];

/** JSON with the keys of every object in sorted order. */
function canonical(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(canonical).join(",")}]`;
  if (value !== null && typeof value === "object") {
    return `{${Object.keys(value as object).sort().map((k) => `${JSON.stringify(k)}:${canonical((value as any)[k])}`).join(",")}}`;
  }
  return JSON.stringify(value);
}

const b64 = (data: Buffer | string) => Buffer.from(data).toString("base64url");

/** Given: a requestState for `payload`: its JSON, then a HMAC-SHA256 signature of it, both in base64url, joined by a dot. */
export function mintState(secret: string, payload: Record<string, unknown>): string {
  const body = b64(canonical(payload));
  return body + "." + b64(createHmac("sha256", secret).update(body).digest());
}

/** Given: the payload of a requestState, or null when it is not one this secret signed (tampered, truncated, foreign, garbage). */
export function readState(secret: string, token: unknown): Record<string, any> | null {
  try {
    if (typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length !== 2) return null;
    const expected = Buffer.from(b64(createHmac("sha256", secret).update(parts[0]).digest()));
    const given = Buffer.from(parts[1]);
    if (expected.length !== given.length || !timingSafeEqual(expected, given)) return null;
    const payload = JSON.parse(Buffer.from(parts[0], "base64url").toString("utf8"));
    return payload !== null && typeof payload === "object" && !Array.isArray(payload) ? payload : null;
  } catch {
    return null;
  }
}

/** Given: a fingerprint of the call's arguments, the same for the same arguments in any key order. */
export function argsDigest(args: unknown): string {
  return createHash("sha256").update(canonical(args)).digest("hex");
}

const error = (code: number, message: string, data?: unknown) => ({ error: { code, message, ...(data ? { data } : {}) } });

function metaError(request: any) {
  if (request._meta?.[META_VERSION] !== VERSION) return error(-32022, "Unsupported protocol version", { supported: [VERSION] });
  return null;
}

/**
 * TODO 1 of 8 (unlocks e1): the result of tools/list.
 * Receives nothing and returns the object from the statement: resultType "complete", the TOOLS sorted by name, ttlMs 300000, cacheScope "public".
 * Example: toolListing().tools[0].name -> "deploy"
 */
function toolListing(): Record<string, unknown> {
  return {};
}

export function listTools(request: any) {
  return metaError(request) ?? toolListing();
}

const complete = (text: string, isError = false) => ({ resultType: "complete", content: [{ type: "text", text }], isError });

const confirmRequest = (service: string) => ({ confirm: { method: "elicitation/create", params: { mode: "form", message: `Deploy ${service} to production?`, requestedSchema: {
  type: "object", properties: { confirm: { type: "boolean", title: "Confirm the deployment" } }, required: ["confirm"] } } } });

const notesRequest = (service: string) => ({ notes: { method: "sampling/createMessage", params: { messages: [{ role: "user", content: { type: "text", text: `Write one sentence of release notes for ${service}.` } }], maxTokens: 100 } } });

/**
 * TODO 2 of 8 (unlocks e7): the payload of a requestState.
 * Receives the tool name, the arguments, the user, the time in seconds and the step. Returns the object from the statement: v, tool, digest, sub, exp, step.
 * Example: newState("deploy", { service: "api" }, "alice", 1000, "confirm").exp -> 1300
 */
function newState(name: string, args: unknown, principal: string, now: number, step: string): Record<string, unknown> {
  return {};
}

/**
 * TODO 3 of 8 (unlocks e6): the protocol error for a call that cannot be served, in the order of the statement.
 * Receives the tool name and the arguments. Returns error(-32602, ...) for an unknown tool, a missing or blank service, or (for deploy) an env
 * that is not staging or production; null when the call is valid.
 * Example: validate("deploy", { service: "api", env: "dev" }) -> error(-32602, "Invalid params: env must be staging or production")
 */
function validate(name: unknown, args: Record<string, any>): any {
  return null;
}

/**
 * TODO 4 of 8 (unlocks e2): can the client be asked for a confirmation?
 * Receives the client capabilities object. Returns true when `elicitation` is present and is empty or holds `form`; false for none or only `url`.
 * Example: canElicit({ elicitation: {} }) -> true, canElicit({ elicitation: { url: {} } }) -> false
 */
function canElicit(caps: Record<string, any>): boolean {
  return false;
}

/**
 * TODO 5 of 8 (unlocks e4 and e5): the error for a requestState that cannot be used.
 * Receives the secret, the token, the user, the tool name, the arguments and the time. Returns error(-32602, ...) with `Invalid requestState`
 * (readState gives null), `Expired requestState` (now after exp) or `requestState does not match this request` (sub, tool or digest differ), checked in
 * that order; null when the state is good.
 * Example: a state minted for "alice" and read for "bob" -> error(-32602, "requestState does not match this request")
 */
function stateError(secret: string, token: unknown, principal: string, name: string, args: unknown, now: number): any {
  return null;
}

/**
 * TODO 6 of 8 (unlocks m1 and e3): is this an answer to the confirmation question?
 * Receives inputResponses.confirm (anything, or undefined). Returns true when it is an object whose `action` is accept, decline or cancel.
 * Example: confirmUsable({ action: "decline" }) -> true, confirmUsable("yes") -> false
 */
function confirmUsable(answer: any): boolean {
  return false;
}

/**
 * TODO 7 of 8 (unlocks e3): did the person accept?
 * Receives a usable answer. Returns true only for action "accept" with content.confirm exactly true.
 * Example: confirmed({ action: "accept", content: { confirm: false } }) -> false
 */
function confirmed(answer: any): boolean {
  return false;
}

/**
 * TODO 8 of 8 (unlocks m1): the release notes the client wrote.
 * Receives the inputResponses object. Returns answers.notes.content.text when that is a string; null for anything else.
 * Example: notesText({ notes: { content: { type: "text", text: "Faster." } } }) -> "Faster."
 */
function notesText(answers: Record<string, any>): string | null {
  return null;
}

function ask(requests: unknown, step: string, secret: string, name: string, args: unknown, principal: string, now: number) {
  const state = newState(name, args, principal, now, step);
  return { resultType: "input_required", inputRequests: requests, requestState: mintState(secret, state) };
}

export function callTool(request: any, secret: string, principal: string, now: number): any {
  log.debug("callTool input", request);
  const bad = metaError(request);
  if (bad) return bad;
  const name = request.name;
  const args = request.arguments ?? {};
  const invalid = validate(name, args);
  if (invalid) return invalid;
  const service: string = args.service;
  if (name === "status") return complete(`${service}: running`);
  if (args.env === "staging") return complete(`Deployed ${service} to staging`);
  const caps = request._meta?.[META_CAPS] ?? {};
  if (!canElicit(caps)) return complete("Deploying to production needs confirmation, and this client cannot be asked.", true);
  let step = "confirm";
  const token = request.requestState;
  if (token !== undefined && token !== null) {
    const problem = stateError(secret, token, principal, name, args, now);
    if (problem) return problem;
    step = readState(secret, token)?.step;
  }
  const given = token !== undefined && token !== null ? request.inputResponses : undefined;
  const answers = given !== null && typeof given === "object" && !Array.isArray(given) ? given : {};
  if (step === "confirm") {
    const answer = answers.confirm;
    if (!confirmUsable(answer)) return ask(confirmRequest(service), "confirm", secret, name, args, principal, now);
    if (!confirmed(answer)) return complete("Deployment cancelled");
    if (!("sampling" in caps)) return complete(`Deployed ${service} to production`);
    return ask(notesRequest(service), "notes", secret, name, args, principal, now);
  }
  const text = notesText(answers);
  if (text === null) return ask(notesRequest(service), "notes", secret, name, args, principal, now);
  return complete(`Deployed ${service} to production. Release notes: ${text}`);
}
