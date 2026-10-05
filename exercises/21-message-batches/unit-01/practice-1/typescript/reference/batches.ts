// Build, split and read Message Batches. See ../../statement.md for the contract.
import { logger } from "../logger.ts";
const log = logger("batches");

const CUSTOM_ID = /^[a-zA-Z0-9_-]{1,64}$/;
const MAX_REQUESTS = 100_000;
const MAX_BYTES = 256 * 1024 * 1024;
const USAGE_KEYS = ["input_tokens", "output_tokens", "cache_creation_input_tokens", "cache_read_input_tokens"];

export type Item = { id: string; params: Record<string, any> };
export type BatchRequest = { custom_id: string; params: Record<string, any> };

/** The batch would be refused. `field` names the offending part. */
export class BatchError extends Error {
  field: string;
  reason: string;
  constructor(field: string, reason: string) {
    super(`${field}: ${reason}`);
    this.field = field;
    this.reason = reason;
  }
}

function checkCustomId(id: unknown, seen: Set<string>): void {
  if (typeof id !== "string" || !CUSTOM_ID.test(id)) throw new BatchError("custom_id", `${JSON.stringify(id)} is not 1 to 64 letters, digits, hyphens or underscores`);
  if (seen.has(id)) throw new BatchError("custom_id", `${id} is used twice`);
  seen.add(id);
}

function checkParams(params: Record<string, any>): void {
  if ((params.max_tokens ?? 0) < 1) throw new BatchError("params.max_tokens", "must be at least 1 inside a batch");
  if (params.stream) throw new BatchError("params.stream", "batch results are a file, not a stream");
  if ("speed" in params) throw new BatchError("params.speed", "fast mode is not available in a batch");
}

export function buildRequests(items: Item[]): BatchRequest[] {
  log.debug("buildRequests input", items);
  const seen = new Set<string>();
  const requests: BatchRequest[] = [];
  for (const { id, params } of items) {
    checkCustomId(id, seen);
    checkParams(params);
    requests.push({ custom_id: id, params });
  }
  return requests;
}

const size = (request: BatchRequest) => Buffer.byteLength(JSON.stringify(request), "utf8");

function mustStartNew(count: number, used: number, bytes: number, maxRequests: number, maxBytes: number): boolean {
  return count > 0 && (count >= maxRequests || used + bytes > maxBytes);
}

export function splitBatches(requests: BatchRequest[], maxRequests = MAX_REQUESTS, maxBytes = MAX_BYTES): BatchRequest[][] {
  const batches: BatchRequest[][] = [];
  let current: BatchRequest[] = [];
  let used = 0;
  for (const request of requests) {
    const bytes = size(request);
    if (bytes > maxBytes) throw new BatchError("size", `${request.custom_id} alone is larger than a batch may be`);
    if (mustStartNew(current.length, used, bytes, maxRequests, maxBytes)) {
      batches.push(current);
      current = [];
      used = 0;
    }
    current.push(request);
    used += bytes;
  }
  if (current.length > 0) batches.push(current);
  return batches;
}

function keepResult(wanted: string[], byId: Map<string, any>, unknown: string[], record: { custom_id: string; result: any }): void {
  if (!wanted.includes(record.custom_id)) unknown.push(record.custom_id);
  else if (!byId.has(record.custom_id)) byId.set(record.custom_id, record.result);
}

function needsFix(errorType: string): boolean {
  return errorType === "invalid_request_error";
}

function addUsage(usage: Record<string, number>, used: Record<string, number>): void {
  for (const key of USAGE_KEYS) usage[key] += used[key] ?? 0;
}

export function collect(requests: BatchRequest[], resultLines: string[]) {
  const wanted = requests.map((r) => r.custom_id);
  const byId = new Map<string, any>();
  const unknown: string[] = [];
  for (const line of resultLines) {
    if (line.trim()) keepResult(wanted, byId, unknown, JSON.parse(line));
  }
  const outcomes: Record<string, any>[] = [];
  const retry: string[] = [];
  const fix: string[] = [];
  const usage: Record<string, number> = Object.fromEntries(USAGE_KEYS.map((k) => [k, 0]));
  for (const cid of wanted) {
    const result = byId.get(cid);
    if (result === undefined) {
      outcomes.push({ custom_id: cid, status: "missing" });
      retry.push(cid);
    } else if (result.type === "succeeded") {
      const message = result.message;
      const text = message.content.filter((b: any) => b.type === "text").map((b: any) => b.text ?? "").join("");
      outcomes.push({ custom_id: cid, status: "succeeded", text, usage: message.usage });
      addUsage(usage, message.usage);
    } else if (result.type === "errored") {
      const kind = result.error.error.type;
      outcomes.push({ custom_id: cid, status: "errored", error_type: kind });
      (needsFix(kind) ? fix : retry).push(cid);
    } else {
      // canceled or expired: the request never reached the model
      outcomes.push({ custom_id: cid, status: result.type });
      retry.push(cid);
    }
  }
  return { outcomes, retry, fix, unknown, usage };
}
