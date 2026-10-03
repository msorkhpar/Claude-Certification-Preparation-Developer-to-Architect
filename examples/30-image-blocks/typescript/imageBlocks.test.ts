import { test } from "node:test";
import assert from "node:assert/strict";
import { comparison, documentBlock, imageBlock, padded, resizedSize, shape, toOriginal, visualTokens } from "./imageBlocks.ts";

test("the documentation table for two sizes on both tiers", () => {
  assert.deepEqual(resizedSize(1920, 1080, "standard"), [1456, 819]);
  assert.equal(visualTokens(1456, 819), 1560);
  assert.deepEqual(resizedSize(1920, 1080, "high"), [1920, 1080]);
  assert.equal(visualTokens(1920, 1080), 2691);
  assert.deepEqual(resizedSize(3840, 2160, "high"), [2576, 1449]);
  assert.equal(visualTokens(2576, 1449), 4784);
  assert.deepEqual(resizedSize(200, 200, "standard"), [200, 200]);
  assert.equal(visualTokens(200, 200), 64);
});

test("a scan under the edge limit is still resized by the token budget", () => {
  assert.equal(visualTokens(1075, 1520), 2145);
  assert.deepEqual(resizedSize(1075, 1520, "standard"), [924, 1307]);
  assert.deepEqual(padded(924, 1307), [924, 1316]);
  assert.deepEqual(resizedSize(1075, 1520, "high"), [1075, 1520]);
});

test("the three sources and the label order", () => {
  const blocks = [imageBlock("base64", "AAAA", "image/jpeg"), imageBlock("url", "https://example.invalid/a.png"), imageBlock("file", "file_1")];
  const content = comparison(blocks, "Which is larger?");
  assert.deepEqual(content.map(shape), ["text", "image/base64", "text", "image/url", "text", "image/file", "text"]);
  assert.deepEqual(content.filter((b) => b.type === "text").map((b) => b.text), ["Image 1:", "Image 2:", "Image 3:", "Which is larger?"]);
  assert.deepEqual(documentBlock("file", "file_2", "T"), { type: "document", source: { type: "file", file_id: "file_2" }, title: "T" });
});

test("a coordinate maps back by the resized size and not the padded one", () => {
  const [x, y] = toOriginal(462, 654, 1075, 1520, "standard");
  assert.deepEqual([Number(x.toFixed(1)), Number(y.toFixed(1))], [537.5, 760.6]);
  assert.deepEqual(toOriginal(10, 20, 100, 100, "standard"), [10, 20]);
});
