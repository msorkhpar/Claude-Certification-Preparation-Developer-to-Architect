// The loop around the computer use toolset, on a toy screen: scaling both ways, a batch, a halt after a failure and a confirmation.
//
// There is no desktop and no model: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the
// Messages API (claude-sonnet-5-5), illustrative and not captures. The tool entry, the batch rule and the halt text are those of the
// "Computer use tool" page of the Claude documentation, checked on 2026-10-03.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("screen_loop");

export const MODEL = "claude-sonnet-5-5";
export const TOOLSET = { type: "computer_toolset_20260801" };
export const HALT = "Not executed: an earlier computer action in this turn failed.";

type Element = { id: string; x: number; y: number; w: number; h: number; risk: string };
export type Screen = { width: number; height: number; typed: string; log: unknown[]; elements: Element[] };
export const newScreen = (): Screen => ({
  width: 2560, height: 1440, typed: "", log: [],
  elements: [{ id: "search", x: 800, y: 100, w: 900, h: 60, risk: "none" }, { id: "buy", x: 2000, y: 1200, w: 300, h: 80, risk: "payment" }],
});

/** The documentation's example limits (1568 px on the long edge, about 1.15 megapixels): small enough for every model. */
export const scaleFor = (width: number, height: number) => Math.min(1, 1568 / Math.max(width, height), Math.sqrt(1_150_000 / (width * height)));

function roundHalfEven(x: number): number {
  const floor = Math.floor(x);
  if (x - floor !== 0.5) return Math.round(x);
  return floor % 2 === 0 ? floor : floor + 1;
}

export const toScreen = (x: number, y: number, scale: number, screen: Screen): [number, number] => [
  Math.min(Math.max(roundHalfEven(x / scale), 0), screen.width - 1), Math.min(Math.max(roundHalfEven(y / scale), 0), screen.height - 1)];

const elementAt = (screen: Screen, x: number, y: number) => screen.elements.filter((e) => e.x <= x && x < e.x + e.w && e.y <= y && y < e.y + e.h).at(-1);

function screenshot(screen: Screen, scale: number) {
  const size = `${Math.floor(screen.width * scale)}x${Math.floor(screen.height * scale)}`;
  return [{ type: "image", source: { type: "base64", media_type: "image/png", data: Buffer.from(`${size}:${screen.log.length}`).toString("base64") } }];
}

/** Run one member of the toolset on the toy screen; return [content, isError]. */
export function perform(screen: Screen, name: string, args: any, scale: number, confirm?: (a: { action: string; element: string; risk: string }) => boolean): [any, boolean] {
  if (name === "screenshot") return [screenshot(screen, scale), false];
  if (name === "left_click") {
    const [x, y] = toScreen(args.coordinate[0], args.coordinate[1], scale, screen);
    const target = elementAt(screen, x, y);
    if (target && target.risk !== "none" && !(confirm && confirm({ action: name, element: target.id, risk: target.risk }))) {
      return [`Declined: ${target.id} needs a person's confirmation (${target.risk})`, true];
    }
    screen.log.push(["click", target ? target.id : null]);
    return [`Clicked ${target ? target.id : "nothing"} at ${x},${y} on the screen`, false];
  }
  if (name === "type") {
    screen.typed += args.text;
    screen.log.push(["type", args.text]);
    return [`Typed ${args.text.length} characters`, false];
  }
  if (name === "key") {
    screen.log.push(["key", args.text]);
    return [`Pressed ${args.text}`, false];
  }
  return [`Unknown action: ${name}`, true];
}

export async function runLoop(client: Anthropic, screen: Screen, confirm?: (a: any) => boolean, maxTurns = 6) {
  const scale = scaleFor(screen.width, screen.height);
  const messages: any[] = [{ role: "user", content: "Search for a kettle and buy the first one." }];
  for (let turn = 1; turn <= maxTurns; turn++) {
    const reply = await client.messages.create({ model: MODEL, max_tokens: 4096, tools: [TOOLSET] as any, messages });
    messages.push({ role: "assistant", content: reply.content });
    if (reply.stop_reason !== "tool_use") return { status: "done", turns: turn, answer: (reply.content.at(-1) as any).text as string | null };
    const results: any[] = [];
    let failed = false;
    for (const block of reply.content.filter((b) => b.type === "tool_use") as any[]) {
      let content: any, isError: boolean;
      if (failed) [content, isError] = [HALT, true];
      else {
        [content, isError] = perform(screen, block.name, block.input, scale, confirm);
        failed = isError;
      }
      console.log(`turn ${turn}: ${block.name} ${JSON.stringify(block.input)} -> ${isError ? "ERROR " : ""}${typeof content === "string" ? content : "image"}`);
      results.push({ type: "tool_result", tool_use_id: block.id, toolset_name: block.toolset_name, content, ...(isError ? { is_error: true } : {}) });
    }
    messages.push({ role: "user", content: results });
  }
  return { status: "max_turns", turns: maxTurns, answer: null as string | null };
}

export const call = (id: string, name: string, input: Record<string, unknown> = {}) => ({ type: "tool_use", id, name, toolset_name: "computer", input });

async function main() {
  const screen = newScreen();
  const scale = scaleFor(screen.width, screen.height);
  const point = (e: Element) => [roundHalfEven((e.x + e.w / 2) * scale), roundHalfEven((e.y + e.h / 2) * scale)]; // what the model would see
  const [search, buy] = screen.elements;
  const fake = scriptedFetch([
    { body: message([call("toolu_1", "screenshot"), call("toolu_2", "left_click", { coordinate: point(search) }), call("toolu_3", "type", { text: "kettle" }), call("toolu_4", "key", { text: "Return" })], "tool_use", undefined, MODEL) },
    { body: message([call("toolu_5", "left_click", { coordinate: point(buy) }), call("toolu_6", "screenshot")], "tool_use", undefined, MODEL) },
    { body: message([text("I did not buy it: the payment button needs your confirmation.")], "end_turn", undefined, MODEL) },
  ]);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  console.log(`screen ${screen.width}x${screen.height}, scale ${scale.toFixed(4)}, the model sees ${Math.floor(screen.width * scale)}x${Math.floor(screen.height * scale)}`);
  const result = await runLoop(client, screen, () => false);
  console.log("result:", result.status, "after", result.turns, "turns:", result.answer);
  console.log("screen log:", JSON.stringify(screen.log), "typed:", screen.typed);
  console.log("tools sent:", JSON.stringify(fake.seen[0].body.tools), "beta header sent:", "anthropic-beta" in fake.seen[0].headers ? "True" : "False");
  const last = fake.seen[2].body.messages.at(-1).content;
  console.log("second result message:", `[${last.map((r: any) => `('${r.tool_use_id}', '${r.toolset_name}', ${r.is_error ? "True" : "False"})`).join(", ")}]`);
}

if (import.meta.main) await main();
