// Build, split and read Message Batches. See ../../statement.md for the contract.
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

export function buildRequests(items: Item[]): BatchRequest[] {
  // TODO: turn [{ id, params }] into [{ custom_id, params }], refusing what a batch refuses.
  return undefined as unknown as BatchRequest[];
}

export function splitBatches(requests: BatchRequest[], maxRequests = 100_000, maxBytes = 256 * 1024 * 1024): BatchRequest[][] {
  // TODO: cut the requests, in order, into batches that respect both limits.
  return undefined as unknown as BatchRequest[][];
}

export function collect(requests: BatchRequest[], resultLines: string[]): any {
  // TODO: match the result lines to the requests by custom_id and sort out what to retry and what to fix.
  return undefined;
}
