// Image and document blocks: three kinds of source, a labelled comparison, the resize rule, the cost and the way back.
// The model reply is illustrative, a hand-written body in the shape of the Messages API (claude-sonnet-5-5), not a capture. The resize
// rule is the reference implementation of the "Coordinates and bounding boxes" page of the Claude documentation, checked on 2026-10-03.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
const TIERS: Record<string, [number, number]> = { standard: [1568, 1568], high: [2576, 4784] }; // tier -> [longest edge in pixels, visual token budget]
const PRICE = 2.0; // dollars per million input tokens for claude-sonnet-5-5 (docs/VERSIONS.md)

export const visualTokens = (width: number, height: number) => Math.ceil(width / 28) * Math.ceil(height / 28);

/** Claude pads every image up to the next multiple of 28 on the bottom and the right; the padding holds no content. */
export const padded = (width: number, height: number): [number, number] => [Math.ceil(width / 28) * 28, Math.ceil(height / 28) * 28];

function roundHalfEven(x: number): number {
  const floor = Math.floor(x);
  if (x - floor !== 0.5) return Math.round(x);
  return floor % 2 === 0 ? floor : floor + 1;
}

/** The size Claude sees: the largest aspect-preserving size within the tier's edge limit and token budget. */
export function resizedSize(width: number, height: number, tier: string): [number, number] {
  const [maxEdge, maxTokens] = TIERS[tier];
  const fits = (w: number, h: number) => Math.max(...padded(w, h)) <= maxEdge && visualTokens(w, h) <= maxTokens;
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

/** An image block with one of the three sources of the Messages API: base64, url or file (a Files API id). */
export function imageBlock(kind: "base64" | "url" | "file", value: string, mediaType = "image/png") {
  const source = { base64: { type: "base64", media_type: mediaType, data: value }, url: { type: "url", url: value }, file: { type: "file", file_id: value } }[kind];
  return { type: "image", source };
}

export function documentBlock(kind: "base64" | "url" | "file", value: string, title?: string) {
  const source = { base64: { type: "base64", media_type: "application/pdf", data: value }, url: { type: "url", url: value }, file: { type: "file", file_id: value } }[kind];
  return { type: "document", source, ...(title ? { title } : {}) };
}

/** Several images are each introduced by a label, and the question comes last. */
export function comparison(images: any[], question: string): any[] {
  const content: any[] = [];
  images.forEach((image, i) => content.push({ type: "text", text: `Image ${i + 1}:` }, image));
  return [...content, { type: "text", text: question }];
}

/** A point on the picture Claude saw, as a point on the original: divide by the resized size, never by the padded one. */
export function toOriginal(x: number, y: number, width: number, height: number, tier: string): [number, number] {
  const [rw, rh] = resizedSize(width, height, tier);
  return [(x / rw) * width, (y / rh) * height];
}

export const shape = (block: any) => (block.type === "text" ? "text" : `${block.type}/${block.source.type}`);

async function main() {
  const fake = scriptedFetch([{ body: message([text("Image 1 has the larger bars; image 2 matches the table in the PDF.")], "end_turn", { input_tokens: 1, output_tokens: 1 }, MODEL) }]);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  const content = comparison([imageBlock("base64", "iVBORw0KGgo="), imageBlock("url", "https://example.invalid/chart.png"), imageBlock("file", "file_011CNha8iCJcU1wXNR6q4V8w")],
    "Compare the images with the table in the document.");
  content.splice(content.length - 1, 0, documentBlock("file", "file_011CPMxVD3fHLUhvTqtsQA5w", "Quarterly table"));
  const reply = await client.messages.create({ model: MODEL, max_tokens: 300, messages: [{ role: "user", content }] });
  const sent = fake.seen[0].body.messages[0].content;
  console.log("content order:", sent.map(shape).join(", "));
  console.log("labels:", sent.filter((b: any) => b.type === "text" && b.text.startsWith("Image")).map((b: any) => b.text).join(" "));
  console.log("one image block:", JSON.stringify(sent[1]));
  console.log("reply:", (reply.content[0] as any).text);
  console.log();
  console.log("size         tier       seen        padded      tokens  dollars per 1000 images");
  for (const [width, height] of [[200, 200], [1920, 1080], [3840, 2160], [1075, 1520]]) {
    for (const tier of ["standard", "high"]) {
      const seen = resizedSize(width, height, tier);
      const tokens = visualTokens(...seen);
      console.log(`${`${width}x${height}`.padEnd(12)} ${tier.padEnd(10)} ${`${seen[0]}x${seen[1]}`.padEnd(11)} ${padded(...seen).join("x").padEnd(11)} ${String(tokens).padStart(6)}  ${((tokens * PRICE) / 1000).toFixed(2)}`);
    }
  }
  console.log();
  const [x, y] = toOriginal(462, 654, 1075, 1520, "standard");
  const [pw, ph] = padded(...resizedSize(1075, 1520, "standard"));
  const [wx, wy] = [(462 / pw) * 1075, (654 / ph) * 1520];
  console.log(`point (462, 654) on the picture Claude saw of a 1075x1520 scan: (${x.toFixed(1)}, ${y.toFixed(1)}) on the original`);
  console.log(`dividing by the padded size instead gives (${wx.toFixed(1)}, ${wy.toFixed(1)}), ${Math.abs(y - wy).toFixed(1)} pixels off`);
}

if (import.meta.main) await main();
