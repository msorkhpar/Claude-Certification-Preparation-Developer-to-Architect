import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { MIB, RequestError, imageCostUsd, planRequest, resizedSize, toOriginalCoordinates, visualTokens } = await import(pathToFileURL(resolve(dir, "vision.ts")).href);

const OPUS = "claude-opus-5-5";
const HAIKU = "claude-haiku-4-5";

const img = (name: string, w: number, h: number, media_type = "image/png", size = 1000, source = "base64", value = "AAAA") => ({ kind: "image", name, media_type, width: w, height: h, size, source, value });
const pdf = (name: string, pages: number, size = 1000, source = "base64", value = "JVBER") => ({ kind: "pdf", name, media_type: "application/pdf", pages, size, source, value });
const plan = (...args: any[]): any => (planRequest as any)(...args) ?? {};

/** The RequestError field fn throws, "crash" for another error, null when it returns. */
function failureOf(fn: () => unknown): string | null {
  try {
    fn();
  } catch (err) {
    return err instanceof RequestError ? err.field : "crash";
  }
  return null;
}
const types = (content: any[]) => content.map((b) => b.type);

test("m1 images come first with labels and the question last", () => {
  const result = plan(OPUS, [img("chart", 1000, 1000), img("photo", 200, 200, "image/jpeg", 1000, "url", "https://example.invalid/p.jpg")], "What changed?");
  assert.deepEqual(result.content, [
    { type: "text", text: "Image 1:" }, { type: "image", source: { type: "base64", media_type: "image/png", data: "AAAA" } },
    { type: "text", text: "Image 2:" }, { type: "image", source: { type: "url", url: "https://example.invalid/p.jpg" } },
    { type: "text", text: "What changed?" }]);
  assert.deepEqual([result.image_tokens, result.resized, result.pdf_pages], [1296 + 64, [], 0]);
  const one = plan(OPUS, [img("only", 200, 200, "image/png", 1000, "file", "file_abc")], "Describe it.");
  assert.deepEqual(one.content, [{ type: "image", source: { type: "file", file_id: "file_abc" } }, { type: "text", text: "Describe it." }]);
});

test("e1 the token cost follows the models resolution tier", () => {
  assert.ok(visualTokens(1000, 1000) === 1296 && visualTokens(200, 200) === 64);
  assert.deepEqual(resizedSize(1920, 1080, "standard"), [1456, 819]);
  assert.deepEqual(resizedSize(1080, 1920, "standard"), [819, 1456]);
  assert.deepEqual(resizedSize(1075, 1520, "standard"), [924, 1307]);
  assert.deepEqual(resizedSize(3840, 2160, "high"), [2576, 1449]);
  assert.deepEqual(resizedSize(1920, 1080, "high"), [1920, 1080]);
  const full = plan(HAIKU, [img("shot", 1920, 1080)], "q");
  assert.deepEqual([full.image_tokens, full.resized], [1560, ["shot"]]);
  const wide = plan(OPUS, [img("shot", 1920, 1080)], "q");
  assert.deepEqual([wide.image_tokens, wide.resized], [2691, []]);
  assert.equal(plan(HAIKU, [img("a", 3840, 2160)], "q").image_tokens, 1560);
  assert.equal(plan(OPUS, [img("a", 3840, 2160)], "q").image_tokens, 4784);
  assert.equal(plan(HAIKU, [img("scan", 1075, 1520)], "q").image_tokens, 1551);
  assert.equal(plan(OPUS, [img("scan", 1075, 1520)], "q").image_tokens, 2145);
});

test("e2 formats dimensions sizes and counts are checked before any call", () => {
  const ok = img("ok", 100, 100);
  const f = (model: string, items: any[], q = "q") => failureOf(() => planRequest(model, items, q));
  assert.equal(f(OPUS, [ok, img("b", 100, 100, "image/bmp")]), "items[1].media_type");
  assert.equal(f(OPUS, [img("a", 8001, 100)]), "items[0].dimensions");
  assert.equal(f(OPUS, [img("a", 8000, 8000)]), null);
  assert.equal(f(OPUS, [img("a", 100, 100, "image/png", 10 * MIB + 1)]), "items[0].size");
  assert.equal(f(OPUS, [img("a", 100, 100, "image/png", 10 * MIB)]), null);
  const many = Array.from({ length: 101 }, (_, n) => img(`i${n}`, 100, 100));
  assert.equal(f(HAIKU, many), "items");
  assert.equal(f(OPUS, many), null);
  assert.equal(f(OPUS, Array.from({ length: 601 }, (_, n) => img(`i${n}`, 100, 100))), "items");
  assert.equal(f("claude-unknown-1", [ok]), "model");
  assert.equal(f(OPUS, [ok], "  "), "question");
});

test("e3 more than twenty images lower the side limit to 2000 pixels", () => {
  const twenty = Array.from({ length: 20 }, (_, n) => img(`i${n}`, 100, 100));
  const big = img("big", 2500, 100);
  const f = (items: any[], platform?: string) => failureOf(() => (planRequest as any)(OPUS, items, "q", false, platform));
  assert.equal(f([...twenty.slice(0, 19), big]), null);
  const four = [...twenty.slice(0, 4), big, ...twenty.slice(5), img("extra", 100, 100)];
  assert.equal(f(four), "items[4].dimensions");
  assert.equal(f([...twenty.slice(0, 19), img("edge", 2000, 100), img("edge2", 100, 2000)]), null);
  assert.equal(f([...twenty.slice(0, 19), big, pdf("doc", 3)]), null);
  assert.equal(f([pdf("doc", 3), ...twenty.slice(0, 19), img("w", 2500, 100)], "bedrock"), "items[20].dimensions");
});

test("e4 an image whose size matters is rejected instead of resized", () => {
  const exact = plan(OPUS, [img("shot", 1000, 1000)], "Where is the button?", true);
  assert.deepEqual(exact.content?.[0]?.transformations, { oversized_image: "error" });
  assert.ok(plan(OPUS, [img("shot", 1000, 1000)], "q").content?.[0] && !("transformations" in plan(OPUS, [img("shot", 1000, 1000)], "q").content[0]));
  assert.equal(failureOf(() => (planRequest as any)(HAIKU, [img("a", 100, 100), img("shot", 1920, 1080)], "q", true)), "items[1].dimensions");
  assert.equal(failureOf(() => (planRequest as any)(OPUS, [img("shot", 1920, 1080)], "q", true)), null);
  assert.deepEqual(plan(HAIKU, [img("shot", 1920, 1080)], "q").resized, ["shot"]);
});

test("e5 pdfs become document blocks in order and are limited by pages", () => {
  const result = plan(OPUS, [pdf("report", 12), img("logo", 200, 200)], "Summarise.");
  assert.deepEqual(types(result.content ?? []), ["document", "image", "text"]);
  assert.deepEqual(result.content?.[0], { type: "document", source: { type: "base64", media_type: "application/pdf", data: "JVBER" } });
  assert.equal(result.pdf_pages, 12);
  const link = plan(OPUS, [pdf("report", 3, 1000, "url", "https://example.invalid/r.pdf")], "q");
  assert.deepEqual(link.content?.[0], { type: "document", source: { type: "url", url: "https://example.invalid/r.pdf" } });
  const f = (model: string, items: any[]) => failureOf(() => planRequest(model, items, "q"));
  assert.equal(f(OPUS, [pdf("a", 300), pdf("b", 301)]), "items");
  assert.equal(f(OPUS, [pdf("a", 300), pdf("b", 300)]), null);
  assert.equal(f(HAIKU, [pdf("a", 101)]), "items");
  assert.equal(f(HAIKU, [pdf("a", 100)]), null);
  assert.equal(f(OPUS, [pdf("ok", 1), { ...pdf("a", 3), media_type: "text/plain" }]), "items[1].media_type");
  assert.equal(f(OPUS, [pdf("a", 5, 20 * MIB), pdf("b", 5, 13 * MIB)]), "items");
});

test("e6 bedrock and vertex take only base64 sources and smaller images", () => {
  const link = img("a", 100, 100, "image/png", 1000, "url", "https://example.invalid/a.png");
  const f = (items: any[], platform?: string) => failureOf(() => (planRequest as any)(OPUS, items, "q", false, platform));
  assert.equal(f([link]), null);
  assert.equal(f([img("ok", 100, 100), link], "bedrock"), "items[1].source");
  assert.equal(f([img("f", 100, 100, "image/png", 1000, "file", "file_1")], "vertex"), "items[0].source");
  const six = img("six", 100, 100, "image/png", 6 * MIB);
  assert.equal(f([img("ok", 100, 100), six]), null);
  assert.equal(f([img("ok", 100, 100), six], "bedrock"), "items[1].size");
  assert.equal(f([img("five", 100, 100, "image/png", 5 * MIB)], "vertex"), null);
});

test("e7 coordinates map back to the original and cost follows the price", () => {
  assert.deepEqual(toOriginalCoordinates(462, 653.5, 1075, 1520, HAIKU), [537.5, 760.0]);
  assert.deepEqual(toOriginalCoordinates(462, 653.5, 1075, 1520, OPUS), [462.0, 653.5]);
  assert.deepEqual(toOriginalCoordinates(2000, -5, 1075, 1520, HAIKU), [1075.0, 0.0]);
  assert.deepEqual(toOriginalCoordinates(500, 400, 1000, 1000, "claude-haiku-4-5-20251001"), [500.0, 400.0]);
  assert.ok(imageCostUsd(OPUS, 1296) === 0.005184 && imageCostUsd(HAIKU, 1296) === 0.001296);
  assert.ok(imageCostUsd("claude-fable-5-1", 1000) === 0.01 && imageCostUsd("claude-sonnet-5-5", 4784) === 0.009568);
});
