// An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md.
import { createHash, createHmac, timingSafeEqual } from "node:crypto";

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

export function listTools(request: any) {
  return metaError(request) ?? { resultType: "complete", tools: [...TOOLS].sort((a, b) => a.name.localeCompare(b.name)), ttlMs: 300000, cacheScope: "public" };
}

const complete = (text: string, isError = false) => ({ resultType: "complete", content: [{ type: "text", text }], isError });

const confirmRequest = (service: string) => ({ confirm: { method: "elicitation/create", params: { mode: "form", message: `Deploy ${service} to production?`, requestedSchema: {
  type: "object", properties: { confirm: { type: "boolean", title: "Confirm the deployment" } }, required: ["confirm"] } } } });

const notesRequest = (service: string) => ({ notes: { method: "sampling/createMessage", params: { messages: [{ role: "user", content: { type: "text", text: `Write one sentence of release notes for ${service}.` } }], maxTokens: 100 } } });

function ask(requests: unknown, step: string, secret: string, name: string, args: unknown, principal: string, now: number) {
  const state = { v: 1, tool: name, digest: argsDigest(args), sub: principal, exp: now + TTL_SECONDS, step };
  return { resultType: "input_required", inputRequests: requests, requestState: mintState(secret, state) };
}

export function callTool(request: any, secret: string, principal: string, now: number): any {
  const bad = metaError(request);
  if (bad) return bad;
  const name = request.name;
  const args = request.arguments ?? {};
  if (name !== "deploy" && name !== "status") return error(-32602, `Unknown tool: ${name}`);
  const service = args.service;
  if (typeof service !== "string" || !service.trim()) return error(-32602, "Invalid params: service is required");
  if (name === "status") return complete(`${service}: running`);
  const env = args.env;
  if (env !== "staging" && env !== "production") return error(-32602, "Invalid params: env must be staging or production");
  if (env === "staging") return complete(`Deployed ${service} to staging`);
  const caps = request._meta?.[META_CAPS] ?? {};
  const elicitation = caps.elicitation;
  if (elicitation === undefined || elicitation === null || (Object.keys(elicitation).length > 0 && !("form" in elicitation))) {
    return complete("Deploying to production needs confirmation, and this client cannot be asked.", true);
  }
  let step = "confirm";
  const token = request.requestState;
  if (token !== undefined && token !== null) {
    const state = readState(secret, token);
    if (state === null) return error(-32602, "Invalid requestState");
    if (now > (state.exp ?? 0)) return error(-32602, "Expired requestState");
    if (state.sub !== principal || state.tool !== name || state.digest !== argsDigest(args)) return error(-32602, "requestState does not match this request");
    step = state.step;
  }
  const given = token !== undefined && token !== null ? request.inputResponses : undefined;
  const answers = given !== null && typeof given === "object" && !Array.isArray(given) ? given : {};
  if (step === "confirm") {
    const answer = answers.confirm;
    if (answer === null || typeof answer !== "object" || !["accept", "decline", "cancel"].includes(answer.action)) return ask(confirmRequest(service), "confirm", secret, name, args, principal, now);
    if (answer.action !== "accept" || (answer.content ?? {}).confirm !== true) return complete("Deployment cancelled");
    if (!("sampling" in caps)) return complete(`Deployed ${service} to production`);
    return ask(notesRequest(service), "notes", secret, name, args, principal, now);
  }
  const content = answers.notes !== null && typeof answers.notes === "object" ? answers.notes.content : undefined;
  if (content === undefined || content === null || typeof content !== "object" || typeof content.text !== "string") return ask(notesRequest(service), "notes", secret, name, args, principal, now);
  return complete(`Deployed ${service} to production. Release notes: ${content.text}`);
}
