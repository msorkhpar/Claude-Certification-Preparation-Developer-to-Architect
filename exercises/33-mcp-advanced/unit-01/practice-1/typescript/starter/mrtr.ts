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

export function listTools(request: any): any {
  // TODO: the tools sorted by name, as a complete result with ttlMs and cacheScope; a request of another protocol version is a protocol error.
  return null;
}

export function callTool(request: any, secret: string, principal: string, now: number): any {
  // TODO: run the tool; when the server needs the client's input, return an input_required result with a signed requestState. No state is kept here.
  return null;
}
