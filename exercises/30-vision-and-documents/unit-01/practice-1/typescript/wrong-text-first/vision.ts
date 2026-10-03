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

export function visualTokens(width: number, height: number): number {
  return Math.ceil(width / 28) * Math.ceil(height / 28);
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
  return Math.round((tokens * MODELS[model][2] / 1_000_000) * 1e6) / 1e6;
}

export function toOriginalCoordinates(x: number, y: number, width: number, height: number, model: string): [number, number] {
  const [resizedW, resizedH] = resizedSize(width, height, MODELS[model][0]);
  const cx = Math.min(Math.max(x, 0), resizedW);
  const cy = Math.min(Math.max(y, 0), resizedH);
  return [(cx / resizedW) * width, (cy / resizedH) * height];
}

function sourceOf(item: Item): Record<string, unknown> {
  if (item.source === "base64") return { type: "base64", media_type: item.media_type, data: item.value };
  return item.source === "url" ? { type: "url", url: item.value } : { type: "file", file_id: item.value };
}

export function planRequest(model: string, items: Item[], question: string, exact = false, platform = "api") {
  if (!(model in MODELS)) throw new RequestError("model", `unknown model ${model}`);
  const [tier, context] = MODELS[model];
  if (typeof question !== "string" || question.trim() === "") throw new RequestError("question", "the question must be a non-empty string");
  const cloud = platform === "bedrock" || platform === "vertex";
  const images = items.filter((it) => it.kind === "image");
  const pdfs = items.filter((it) => it.kind === "pdf");
  const limit = context < 1_000_000 ? 100 : 600;
  if (images.length > limit) throw new RequestError("items", `too many images for ${model}`);
  if (pdfs.reduce((n, it) => n + (it.pages as number), 0) > limit) throw new RequestError("items", `too many PDF pages for ${model}`);
  if (items.reduce((n, it) => n + it.size, 0) > 32 * MIB) throw new RequestError("items", "the request would be larger than 32 MiB");
  const many = images.length + (cloud ? pdfs.length : 0) > 20;
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
    if (it.size > (cloud ? 5 : 10) * MIB) throw new RequestError(`items[${i}].size`, "the image is too large");
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
      if (images.length > 1) content.push({ type: "text", text: `Image ${n}:` });
      const block: Record<string, unknown> = { type: "image", source: sourceOf(it) };
      if (exact) block.transformations = { oversized_image: "error" };
      content.push(block);
    } else {
      content.push({ type: "document", source: sourceOf(it) });
    }
  }
  content.unshift({ type: "text", text: question });
  return { content, image_tokens: tokens, resized, pdf_pages: pdfs.reduce((a, it) => a + (it.pages as number), 0) };
}
