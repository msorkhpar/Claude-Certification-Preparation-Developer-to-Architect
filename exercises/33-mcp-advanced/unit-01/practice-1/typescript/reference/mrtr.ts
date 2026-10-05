// An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md.
import { createHash, createHmac, timingSafeEqual } from "node:crypto";
import { logger } from "../logger.ts";

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

function toolListing(): Record<string, unknown> {
  return { resultType: "complete", tools: [...TOOLS].sort((a, b) => a.name.localeCompare(b.name)), ttlMs: 300000, cacheScope: "public" };
}

export function listTools(request: any) {
  return metaError(request) ?? toolListing();
}

const complete = (text: string, isError = false) => ({ resultType: "complete", content: [{ type: "text", text }], isError });

const confirmRequest = (service: string) => ({ confirm: { method: "elicitation/create", params: { mode: "form", message: `Deploy ${service} to production?`, requestedSchema: {
  type: "object", properties: { confirm: { type: "boolean", title: "Confirm the deployment" } }, required: ["confirm"] } } } });

const notesRequest = (service: string) => ({ notes: { method: "sampling/createMessage", params: { messages: [{ role: "user", content: { type: "text", text: `Write one sentence of release notes for ${service}.` } }], maxTokens: 100 } } });

function newState(name: string, args: unknown, principal: string, now: number, step: string): Record<string, unknown> {
  return { v: 1, tool: name, digest: argsDigest(args), sub: principal, exp: now + TTL_SECONDS, step };
}

function validate(name: unknown, args: Record<string, any>): any {
  if (name !== "deploy" && name !== "status") return error(-32602, `Unknown tool: ${name}`);
  if (typeof args.service !== "string" || !args.service.trim()) return error(-32602, "Invalid params: service is required");
  if (name === "deploy" && args.env !== "staging" && args.env !== "production") return error(-32602, "Invalid params: env must be staging or production");
  return null;
}

function canElicit(caps: Record<string, any>): boolean {
  const elicitation = caps.elicitation;
  return elicitation !== undefined && elicitation !== null && (Object.keys(elicitation).length === 0 || "form" in elicitation);
}

function stateError(secret: string, token: unknown, principal: string, name: string, args: unknown, now: number): any {
  const state = readState(secret, token);
  if (state === null) return error(-32602, "Invalid requestState");
  if (now > (state.exp ?? 0)) return error(-32602, "Expired requestState");
  if (state.sub !== principal || state.tool !== name || state.digest !== argsDigest(args)) return error(-32602, "requestState does not match this request");
  return null;
}

function confirmUsable(answer: any): boolean {
  return answer !== null && typeof answer === "object" && ["accept", "decline", "cancel"].includes(answer.action);
}

function confirmed(answer: any): boolean {
  return answer.action === "accept" && (answer.content ?? {}).confirm === true;
}

function notesText(answers: Record<string, any>): string | null {
  const content = answers.notes !== null && typeof answers.notes === "object" ? answers.notes.content : undefined;
  return content !== null && typeof content === "object" && typeof content.text === "string" ? content.text : null;
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
