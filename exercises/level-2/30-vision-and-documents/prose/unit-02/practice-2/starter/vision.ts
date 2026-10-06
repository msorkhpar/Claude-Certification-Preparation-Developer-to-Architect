// Planning a request that carries images and PDFs. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("vision");
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

export function visualTokens(width: number, height: number): number {
  // TODO 1 of 8 (unlocks e1 and e7): the visual tokens of an image of this size.
  // Receives the width and height in pixels. Returns one token per 28 x 28 pixel patch, each side rounded up to whole patches.
  // Example: visualTokens(29, 28) -> 2
  return 0;
}

/** Round half to even, like Python's round() and Java's Math.rint. */
function roundHalfEven(x: number): number {
  const floor = Math.floor(x);
  if (x - floor !== 0.5) return Math.round(x);
  return floor % 2 === 0 ? floor : floor + 1;
}

export function resizedSize(width: number, height: number, tier: string): [number, number] {
  const [maxEdge, maxTokens] = TIERS[tier];
  const fits = (w: number, h: number) => Math.ceil(w / 28) * 28 <= maxEdge && Math.ceil(h / 28) * 28 <= maxEdge && visualTokens(w, h) <= maxTokens;
  if (fits(width, height)) return [width, height];
  if (height > width) {
    const [h, w] = resizedSize(height, width, tier);
    return [w, h];
  }
  const ratio = width / height;
  for (let longEdge = width - 1; longEdge > 0; longEdge--) {
    const short = Math.max(roundHalfEven(longEdge / ratio), 1);
    if (fits(longEdge, short)) return [longEdge, short];
  }
  return [1, 1];
}

export function imageCostUsd(model: string, tokens: number): number {
  // TODO 2 of 8 (unlocks e7): the input cost in dollars of that many tokens for the model.
  // Receives a model id and a token count. Returns tokens * price / 1,000,000 rounded to 6 decimals; the price is MODELS[model][2].
  // Example: imageCostUsd("claude-haiku-4-5", 1000) -> 0.001
  return 0;
}

export function toOriginalCoordinates(x: number, y: number, width: number, height: number, model: string): [number, number] {
  const [resizedW, resizedH] = resizedSize(width, height, MODELS[model][0]);
  // TODO 3 of 8 (unlocks e7): a point Claude returned for the image it saw, as a point on the original width x height image.
  // Clamp x and y into the resized size (resizedW x resizedH), then scale them onto the original size. Example: a 3000 x 2000 image on
  // a "high" model is seen as 2576 x 1717 (not padded), so the point (1288, 0) becomes about [1500, 0].
  return [x, y];
}

function sourceOf(item: Item): Record<string, unknown> {
  // TODO 8 of 8 (unlocks m1, e5 and e6): the `source` object of an item.
  // Returns { type: "base64", media_type, data } for source "base64", { type: "url", url } for "url" and { type: "file", file_id }
  // for "file"; the text is item.value.
  // Example: { source: "url", value: "https://example.invalid/a.png" } -> { type: "url", url: "https://example.invalid/a.png" }
  return {};
}

function maxCount(context: number): number {
  // TODO 4 of 8 (unlocks e2 and e5): how many images, and how many PDF pages, a request may hold.
  // Receives the model's context window in tokens. Returns 100 when it is under 1,000,000 and 600 otherwise.
  // Example: maxCount(200_000) -> 100
  return Number.MAX_SAFE_INTEGER;
}

function maxImageSize(cloud: boolean): number {
  // TODO 5 of 8 (unlocks e2 and e6): the largest image payload in bytes.
  // Receives true on bedrock and vertex. Returns 10 MiB, or 5 MiB when it is true.
  // Example: maxImageSize(true) -> 5 * MIB
  return Number.MAX_SAFE_INTEGER;
}

function isMany(images: number, pdfs: number, cloud: boolean): boolean {
  // TODO 6 of 8 (unlocks e3): does the request hold more than 20 image blocks?
  // Receives the image count, the PDF count and the cloud flag; on bedrock and vertex the PDFs count as well.
  // Example: isMany(18, 3, true) -> true, isMany(18, 3, false) -> false
  return false;
}

function imageBlocks(item: Item, n: number, imageCount: number, exact: boolean): Record<string, unknown>[] {
  // TODO 7 of 8 (unlocks m1 and e4): the content blocks of the n-th image (counting from 1).
  // Returns a text block `Image n:` first when there are two or more images, then the image block { type: "image", source:
  // sourceOf(item) }, which also carries transformations: { oversized_image: "error" } when `exact` is true.
  // Example: imageBlocks(item, 2, 2, false) -> [{ type: "text", text: "Image 2:" }, { type: "image", source: { ... } }]
  return [];
}

export function planRequest(model: string, items: Item[], question: string, exact = false, platform = "api") {
  log.debug("planRequest input", items);
  if (!(model in MODELS)) throw new RequestError("model", `unknown model ${model}`);
  const [tier, context] = MODELS[model];
  if (typeof question !== "string" || question.trim() === "") throw new RequestError("question", "the question must be a non-empty string");
  const cloud = platform === "bedrock" || platform === "vertex";
  const images = items.filter((it) => it.kind === "image");
  const pdfs = items.filter((it) => it.kind === "pdf");
  const limit = maxCount(context);
  if (images.length > limit) throw new RequestError("items", `too many images for ${model}`);
  if (pdfs.reduce((n, it) => n + (it.pages as number), 0) > limit) throw new RequestError("items", `too many PDF pages for ${model}`);
  if (items.reduce((n, it) => n + it.size, 0) > 32 * MIB) throw new RequestError("items", "the request would be larger than 32 MiB");
  const many = isMany(images.length, pdfs.length, cloud);
  let tokens = 0;
  const resized: string[] = [];
  items.forEach((it, i) => {
    if (cloud && it.source !== "base64") throw new RequestError(`items[${i}].source`, `${platform} accepts base64 sources only`);
    if (it.kind === "pdf") {
      if (it.media_type !== "application/pdf") throw new RequestError(`items[${i}].media_type`, "a PDF must be application/pdf");
      return;
    }
    const [w, h] = [it.width as number, it.height as number];
    if (!IMAGE_TYPES.includes(it.media_type)) throw new RequestError(`items[${i}].media_type`, `${it.media_type} is not a supported image format`);
    if (w > 8000 || h > 8000) throw new RequestError(`items[${i}].dimensions`, "an image may not exceed 8000 x 8000 pixels");
    if (it.size > maxImageSize(cloud)) throw new RequestError(`items[${i}].size`, "the image is too large");
    if (many && Math.max(w, h) > 2000) throw new RequestError(`items[${i}].dimensions`, "with more than 20 images, no side may exceed 2000 pixels");
    const seen = resizedSize(w, h, tier);
    if (seen[0] !== w || seen[1] !== h) {
      if (exact) throw new RequestError(`items[${i}].dimensions`, `would be resized to ${seen[0]}x${seen[1]}`);
      resized.push(it.name);
    }
    tokens += visualTokens(seen[0], seen[1]);
  });
  const content: Record<string, unknown>[] = [];
  let n = 0;
  for (const it of items) {
    if (it.kind === "image") {
      n++;
      content.push(...imageBlocks(it, n, images.length, exact));
    } else {
      content.push({ type: "document", source: sourceOf(it) });
    }
  }
  content.push({ type: "text", text: question });
  return { content, image_tokens: tokens, resized, pdf_pages: pdfs.reduce((a, it) => a + (it.pages as number), 0) };
}
