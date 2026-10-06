// The loop around the computer use tool, against a toy screen. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("computer");
export const TOOLSET = "computer_toolset_20260801";
export const CLICKS = ["left_click", "right_click", "middle_click", "double_click", "triple_click"];
export const NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed.";

export type Screen = { width: number; height: number; cursor: number[]; typed: string; log: unknown[][]; elements: { id: string; x: number; y: number; w: number; h: number; risk: string }[] };

/** Given: the screenshot of the toy screen at `width` x `height`, as the base64 text of a picture. It changes after every logged action. */
export function render(screen: Screen, width: number, height: number): string {
  return `png:${width}x${height}:${screen.log.length}`;
}

function roundHalfEven(x: number): number {
  const floor = Math.floor(x);
  if (x - floor !== 0.5) return Math.round(x);
  return floor % 2 === 0 ? floor : floor + 1;
}

/**
 * TODO 1 of 9 (unlocks e1, e7 and m1): the factor that shrinks a screen to what the model may be sent.
 * Receives the real width and height. Returns min(1, 1568 / longest side, sqrt(1,150,000 / (width * height))).
 * Example: scaleFor(1280, 800) -> 1, scaleFor(1920, 1080) -> 0.7447...
 */
export function scaleFor(width: number, height: number): number {
  return 1.0;
}

/**
 * TODO 2 of 9 (unlocks e7 and m1): the size of the screenshot the model is sent, as [width, height].
 * Receives the real width and height. Returns each side times `scaleFor`, cut to a whole number (Math.trunc).
 * Example: scaledSize(1920, 1080) -> [1429, 804]
 */
export function scaledSize(width: number, height: number): [number, number] {
  return [width, height];
}

/**
 * TODO 3 of 9 (unlocks e1, e2 and m1): a point on the (scaled) screenshot as a point on the real screen.
 * Receives x and y on the screenshot, the scale and the screen. Returns [x, y] on the screen: divide by the scale, round half to even
 * (`roundHalfEven` is written), and clamp into 0 .. width - 1 and 0 .. height - 1.
 * Example: toScreen(894, 164, 0.7447, screen) -> [1200, 220]; toScreen(-3, -3, 1, screen) -> [0, 0]
 */
export function toScreen(x: number, y: number, scale: number, screen: Screen): [number, number] {
  return [0, 0];
}

function elementAt(screen: Screen, x: number, y: number) {
  let hit;
  for (const e of screen.elements) if (e.x <= x && x < e.x + e.w && e.y <= y && y < e.y + e.h) hit = e;
  return hit;
}

function inside(shot: number[], point: any): boolean {
  return Array.isArray(point) && point.length === 2 && point[0] >= 0 && point[0] < shot[0] && point[1] >= 0 && point[1] < shot[1];
}

/**
 * TODO 4 of 9 (unlocks e2): the error text when a click on this element needs a person and does not get one, else null.
 * Receives the action name, the element under the click ({ id, risk } or undefined) and `confirm` (a function or null). A risk other than
 * "none" is asked of `confirm` with { action, element, risk }; with no `confirm` or an answer of false return
 * "Declined: <id> needs a person's confirmation (<risk>)". Example: a "payment" element "pay", no confirm -> "Declined: pay needs a person's confirmation (payment)"
 */
function riskError(name: string, element: { id: string; risk: string } | undefined, confirm?: ((action: Record<string, unknown>) => boolean) | null): string | null {
  return null;
}

/**
 * TODO 5 of 9 (unlocks e4): is this zoom region valid?
 * Receives the scaled screenshot size [width, height] and the region. True for four numbers [x0, y0, x1, y1] where x0, y0 is inside
 * the screenshot (`inside`), x1 and y1 are above 0 and within the size, x0 < x1 and y0 < y1. Example: [1429, 804], [0, 0, 300, 200] -> true
 */
function validRegion(shot: number[], r: any): boolean {
  return true;
}

/**
 * TODO 6 of 9 (unlocks e4): is this the input of a key press?
 * Receives the input object. True when `text` is a non-empty string and `repeat` (default 1) is an integer from 1 to 100.
 * Example: { text: "ctrl+s", repeat: 2 } -> true, { text: "Return", repeat: 101 } -> false
 */
function validKey(args: Record<string, any>): boolean {
  return true;
}

/**
 * TODO 7 of 9 (unlocks e4): is this the input of a scroll?
 * Receives the input object. True when `scroll_direction` is up, down, left or right and `scroll_amount` is an integer of at least 1.
 * Example: { scroll_direction: "down", scroll_amount: 3 } -> true, { scroll_direction: "up", scroll_amount: 0 } -> false
 */
function validScroll(args: Record<string, any>): boolean {
  return true;
}

/**
 * TODO 8 of 9 (unlocks e6): the images to replace by a note.
 * Receives the list of images, oldest first, and `keep`. Returns the part of the list to replace: all but the newest `keep`, and all of
 * them when keep is 0. Example: ["a", "b", "c"] with keep 1 -> ["a", "b"]; with keep 0 -> ["a", "b", "c"]; with keep 9 -> []
 */
function toReplace<T>(images: T[], keep: number): T[] {
  return [];
}

/**
 * TODO 9 of 9 (unlocks e5): the status the loop ends with for a stop reason, or null when it goes on.
 * Receives the stop reason of a reply that is not `tool_use`. Returns "refused" for refusal, "done" for end_turn and stop_sequence,
 * null for pause_turn (call again), and "truncated" for anything else. Example: "max_tokens" -> "truncated"
 */
function finalStatus(stop: string): string | null {
  return null;
}

/** Run one action. Returns [content, isError]: the text or image blocks for the result and whether it failed. */
export function perform(screen: Screen, name: string, args: Record<string, any>, scale: number, confirm?: ((action: Record<string, unknown>) => boolean) | null): [any, boolean] {
  log.debug("perform input", name, args);
  const shot = scaledSize(screen.width, screen.height);
  if (name === "screenshot") return [[{ type: "image", source: { type: "base64", media_type: "image/png", data: render(screen, shot[0], shot[1]) } }], false];
  if (name === "zoom") {
    const r = args.region;
    if (!validRegion(shot, r)) return [`Invalid zoom region: ${JSON.stringify(r)}`, true];
    const [x0, y0] = toScreen(r[0], r[1], scale, screen);
    const [x1, y1] = toScreen(r[2], r[3], scale, screen);
    return [[{ type: "image", source: { type: "base64", media_type: "image/png", data: render(screen, x1 - x0, y1 - y0) } }], false];
  }
  if (CLICKS.includes(name)) {
    const point = args.coordinate;
    let sx: number, sy: number;
    if (point === undefined || point === null) [sx, sy] = screen.cursor;
    else if (!inside(shot, point)) return [`Coordinate ${JSON.stringify(point)} is outside the screenshot`, true];
    else [sx, sy] = toScreen(point[0], point[1], scale, screen);
    const element = elementAt(screen, sx, sy);
    const declined = riskError(name, element, confirm);
    if (declined) return [declined, true];
    screen.cursor = [sx, sy];
    screen.log.push([name, element ? element.id : null]);
    return [`Clicked ${element ? element.id : "nothing"}`, false];
  }
  if (name === "type") {
    if (typeof args.text !== "string") return ["type needs a text", true];
    screen.typed += args.text;
    screen.log.push(["type", args.text]);
    return [`Typed ${[...args.text].length} characters`, false];
  }
  if (name === "key") {
    if (!validKey(args)) return ["key needs a text and a repeat from 1 to 100", true];
    screen.log.push(["key", args.text]);
    return [`Pressed ${args.text}`, false];
  }
  if (name === "wait") {
    const d = args.duration;
    if (typeof d !== "number" || d < 0 || d > 300) return ["wait needs a duration from 0 to 300 seconds", true];
    return [`Waited ${d}s`, false];
  }
  if (name === "scroll") {
    const direction = args.scroll_direction, amount = args.scroll_amount;
    if (!validScroll(args)) return ["scroll needs a direction and a positive amount", true];
    screen.log.push(["scroll", direction]);
    return [`Scrolled ${direction} ${amount}`, false];
  }
  if (name === "mouse_move") {
    if (!inside(shot, args.coordinate)) return [`Coordinate ${JSON.stringify(args.coordinate)} is outside the screenshot`, true];
    screen.cursor = [...toScreen(args.coordinate[0], args.coordinate[1], scale, screen)];
    return ["Moved", false];
  }
  if (name === "cursor_position") return [`X=${roundHalfEven(screen.cursor[0] * scale)},Y=${roundHalfEven(screen.cursor[1] * scale)}`, false];
  return [`Unknown action: ${name}`, true];
}

/** A copy of the conversation in which every screenshot except the newest `keep` is replaced by a text note. */
export function pruneScreenshots(messages: any[], keep = 3): any[] {
  const out = JSON.parse(JSON.stringify(messages));
  const images: [any, number][] = [];
  for (const m of out) {
    if (!Array.isArray(m.content)) continue;
    for (const b of m.content) {
      if (b.type === "tool_result" && Array.isArray(b.content)) b.content.forEach((c: any, i: number) => { if (c.type === "image") images.push([b, i]); });
    }
  }
  for (const [block, i] of toReplace(images, keep)) block.content[i] = { type: "text", text: "[screenshot removed]" };
  return out;
}

export function runComputerLoop(ask: (request: any) => any, screen: Screen, model = "claude-sonnet-5-5", maxTurns = 10, confirm?: ((action: Record<string, unknown>) => boolean) | null) {
  const scale = scaleFor(screen.width, screen.height);
  const messages: any[] = [{ role: "user", content: "Do the task on the screen." }];
  for (let turn = 1; turn <= maxTurns; turn++) {
    const reply = ask({ model, max_tokens: 4096, tools: [{ type: TOOLSET }], messages: JSON.parse(JSON.stringify(messages)) });
    messages.push({ role: "assistant", content: reply.content });
    const stop = reply.stop_reason;
    if (stop === "tool_use") {
      const results: any[] = [];
      let failed = false;
      for (const block of reply.content) {
        if (block.type !== "tool_use") continue;
        const result: any = { type: "tool_result", tool_use_id: block.id };
        if ("toolset_name" in block) result.toolset_name = block.toolset_name;
        if (failed) {
          result.content = NOT_EXECUTED;
          result.is_error = true;
        } else {
          const [content, isError] = perform(screen, block.name, block.input, scale, confirm);
          result.content = content;
          if (isError) {
            result.is_error = true;
            failed = true;
          }
        }
        results.push(result);
      }
      messages.push({ role: "user", content: results });
    } else if (finalStatus(stop)) {
      return { status: finalStatus(stop), turns: turn, messages };
    }
  }
  return { status: "max_turns", turns: maxTurns, messages };
}
