// Planning a request that carries images and PDFs. See ../../statement.md.
export const MIB = 1024 * 1024;
export const IMAGE_TYPES = ["image/jpeg", "image/png", "image/gif", "image/webp"];
// model id -> [resolution tier, context window in tokens, input price in dollars per million tokens]. Given.
export const MODELS: Record<string, [string, number, number]> = {
  "claude-fable-5-1": ["high", 1_000_000, 10.0],
  "claude-opus-5-5": ["high", 1_000_000, 4.0],
  "claude-sonnet-5-5": ["high", 1_000_000, 2.0],
  "claude-haiku-4-5": ["standard", 200_000, 1.0],
  "claude-haiku-4-5-20251001": ["standard", 200_000, 1.0],
};
// tier -> [longest edge in pixels, visual token budget]. Given.
export const TIERS: Record<string, [number, number]> = { standard: [1568, 1568], high: [2576, 4784] };

/** What your code throws, before any call, for a request the API would reject; `field` names the part. */
export class RequestError extends Error {
  field: string;
  constructor(field: string, message: string) {
    super(`${field}: ${message}`);
    this.field = field;
  }
}

export type Item = { kind: "image" | "pdf"; name: string; media_type: string; width?: number; height?: number; pages?: number; size: number; source: string; value: string };

export function visualTokens(width: number, height: number): any {
  // TODO: the visual tokens of an image of this size (one per 28 x 28 pixel patch, edges rounded up).
  return null;
}

export function resizedSize(width: number, height: number, tier: string): any {
  // TODO: [width, height] the model sees: the largest aspect-preserving size within the tier's edge and token limits.
  return null;
}

export function imageCostUsd(model: string, tokens: number): any {
  // TODO: the input cost in dollars of that many tokens, rounded to 6 decimals.
  return null;
}

export function toOriginalCoordinates(x: number, y: number, width: number, height: number, model: string): any {
  // TODO: a point Claude returned for the image it saw, as a point on the original width x height image.
  return null;
}

export function planRequest(model: string, items: Item[], question: string, exact = false, platform = "api"): any {
  // TODO: validate the items and build the user content. See the statement for the rules and their order.
  return null;
}
