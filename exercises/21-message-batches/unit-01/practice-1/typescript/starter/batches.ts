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
  // TODO 1 of 6 (finish this to pass e1): refuse a bad or repeated custom id, and remember a good one.
  // Receives the id and the set `seen` of ids already used. Throws BatchError("custom_id", reason) when the id is not a string matching
  // CUSTOM_ID (1 to 64 letters, digits, hyphens or underscores) or is already in `seen`; otherwise adds it to `seen`.
  // Example: checkCustomId("has space", new Set()) throws; calling checkCustomId("a", seen) twice throws the second time
}

function checkParams(params: Record<string, any>): void {
  // TODO 2 of 6 (finish this to pass e2): refuse the parameters a batch cannot take.
  // Receives a request body. Throws BatchError("params.max_tokens", reason) when max_tokens is missing or below 1,
  // BatchError("params.stream", reason) when stream is true, and BatchError("params.speed", reason) when speed is present at all.
  // Example: checkParams({ max_tokens: 0 }) throws with field "params.max_tokens"
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
  // TODO 3 of 6 (finish this to pass e3): must this request go into a new batch?
  // Receives the number of requests in the current batch, their total bytes `used`, the size of the next request and both limits.
  // Returns true when the current batch is not empty and adding the request would pass either limit (count already at maxRequests, or
  // used + bytes above maxBytes). Example: mustStartNew(3, 10, 5, 3, 1000) -> true, mustStartNew(0, 0, 5, 3, 1000) -> false
  return false;
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
  // TODO 4 of 6 (finish this to pass m1 and e5): file one parsed result line by its custom id.
  // Receives the array `wanted` of request ids, the map `byId`, the array `unknown` and the parsed `record` ({ custom_id, result }).
  // Adds the id to `unknown` when no request carries it; otherwise stores record.result in `byId` unless the id is already there
  // (the first result wins). Example: a record for "zzz" when wanted is ["a"] puts "zzz" in unknown
}

function needsFix(errorType: string): boolean {
  // TODO 5 of 6 (finish this to pass e4): does an errored result need a corrected request?
  // Receives the error type of an errored result. Returns true for "invalid_request_error" (it fails again until fixed), false for any
  // other type, which is worth sending again unchanged. Example: needsFix("overloaded_error") -> false
  return false;
}

function addUsage(usage: Record<string, number>, used: Record<string, number>): void {
  // TODO 6 of 6 (finish this to pass e6): add one succeeded reply's usage to the totals.
  // Receives the object `usage` of totals (every key of USAGE_KEYS starts at 0) and the reply's usage, which may lack a key.
  // Adds each key of USAGE_KEYS from `used` to `usage`, a missing key counting as 0. Example: adding { input_tokens: 5 } raises
  // usage.input_tokens by 5 and leaves the other three alone
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
