// Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract.

/** The API would answer 400. `param` names the offending parameter. */
export class RejectedRequest extends Error {
  param: string;
  reason: string;
  constructor(param: string, reason: string) {
    super(`${param}: ${reason}`);
    this.param = param;
    this.reason = reason;
  }
}

export function buildParams(model: string, maxTokens: number, options: Record<string, any> = {}): Record<string, any> {
  // TODO: return the request parameters for the model, or throw RejectedRequest for a request the API would refuse.
  return undefined as unknown as Record<string, any>;
}
