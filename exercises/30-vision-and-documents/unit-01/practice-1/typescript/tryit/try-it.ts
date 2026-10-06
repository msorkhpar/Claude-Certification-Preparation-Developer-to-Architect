// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { RequestError, planRequest } from "./vision.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const img = (name: string, width: number, height: number, media_type = "image/png", source = "base64", value = "AAAA") =>
  ({ kind: "image" as const, name, media_type, width, height, size: 1000, source, value });

// Two images and a question: the planner puts the images first, each labelled, and the question last.
const items = [img("chart", 1000, 1000), img("photo", 200, 200, "image/jpeg", "url", "https://example.invalid/p.jpg")];
try {
  const plan: any = planRequest("claude-opus-5-5", items, "What changed?") ?? {};
  for (const block of plan.content ?? []) console.log("block:", block.type, block.text ?? "");
  console.log("image tokens:", plan.image_tokens);
  console.log("resized:", JSON.stringify(plan.resized));
} catch (err) {
  if (err instanceof RequestError) console.log("refused:", err.message);
  else throw err;
}
