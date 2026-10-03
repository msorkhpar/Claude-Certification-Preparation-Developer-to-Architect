// The loop around the computer use tool, against a toy screen. See ../../statement.md.
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

export function scaleFor(width: number, height: number): number {
  return Math.min(1.0, 1568 / Math.max(width, height), Math.sqrt(1_150_000 / (width * height)));
}

export function scaledSize(width: number, height: number): [number, number] {
  const scale = scaleFor(width, height);
  return [Math.trunc(width * scale), Math.trunc(height * scale)];
}

/** A point on the (scaled) screenshot as a point on the real screen, clamped into the screen. */
export function toScreen(x: number, y: number, scale: number, screen: Screen): [number, number] {
  return [Math.min(Math.max(roundHalfEven(x / scale), 0), screen.width - 1), Math.min(Math.max(roundHalfEven(y / scale), 0), screen.height - 1)];
}

function elementAt(screen: Screen, x: number, y: number) {
  let hit;
  for (const e of screen.elements) if (e.x <= x && x < e.x + e.w && e.y <= y && y < e.y + e.h) hit = e;
  return hit;
}

function inside(shot: number[], point: any): boolean {
  return Array.isArray(point) && point.length === 2 && point[0] >= 0 && point[0] < shot[0] && point[1] >= 0 && point[1] < shot[1];
}

/** Run one action. Returns [content, isError]: the text or image blocks for the result and whether it failed. */
export function perform(screen: Screen, name: string, args: Record<string, any>, scale: number, confirm?: ((action: Record<string, unknown>) => boolean) | null): [any, boolean] {
  const shot = scaledSize(screen.width, screen.height);
  if (name === "screenshot") return [[{ type: "image", source: { type: "base64", media_type: "image/png", data: render(screen, shot[0], shot[1]) } }], false];
  if (name === "zoom") {
    const r = args.region;
    if (!(Array.isArray(r) && r.length === 4 && inside(shot, r.slice(0, 2)) && r[2] > 0 && r[2] <= shot[0] && r[3] > 0 && r[3] <= shot[1] && r[0] < r[2] && r[1] < r[3])) return [`Invalid zoom region: ${JSON.stringify(r)}`, true];
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
    if (element && (element.risk ?? "none") !== "none") {
      if (!confirm || !confirm({ action: name, element: element.id, risk: element.risk })) return [`Declined: ${element.id} needs a person's confirmation (${element.risk})`, true];
    }
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
    const repeat = args.repeat ?? 1;
    if (!args.text || !Number.isInteger(repeat) || repeat < 1 || repeat > 100) return ["key needs a text and a repeat from 1 to 100", true];
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
    if (!["up", "down", "left", "right"].includes(direction) || !Number.isInteger(amount) || amount < 1) return ["scroll needs a direction and a positive amount", true];
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
  for (const [block, i] of keep > 0 ? images.slice(0, Math.max(images.length - keep, 0)) : images) block.content[i] = { type: "text", text: "[screenshot removed]" };
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
          }
        }
        results.push(result);
      }
      messages.push({ role: "user", content: results });
    } else if (stop === "refusal") {
      return { status: "refused", turns: turn, messages };
    } else if (stop !== "pause_turn") {
      return { status: stop === "end_turn" || stop === "stop_sequence" ? "done" : "truncated", turns: turn, messages };
    }
  }
  return { status: "max_turns", turns: maxTurns, messages };
}
