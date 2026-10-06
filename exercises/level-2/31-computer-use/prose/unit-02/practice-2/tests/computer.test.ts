import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { perform, pruneScreenshots, runComputerLoop, scaleFor, scaledSize, toScreen } = await import(pathToFileURL(resolve(dir, "computer.ts")).href);

const NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed.";
const clone = <T,>(x: T): T => JSON.parse(JSON.stringify(x));

function makeScreen(width = 1920, height = 1080): any {
  return { width, height, cursor: [0, 0], typed: "", log: [],
    elements: [{ id: "search", x: 1000, y: 200, w: 400, h: 40, risk: "none" }, { id: "pay", x: 800, y: 600, w: 200, h: 60, risk: "payment" }] };
}
const use = (id: string, name: string, input: Record<string, unknown> = {}) => ({ type: "tool_use", id, name, toolset_name: "computer", input });
const reply = (content: unknown[], stop_reason = "tool_use") => ({ content, stop_reason });
const says = (text: string) => reply([{ type: "text", text }], "end_turn");

/** The model: returns the scripted replies in order and keeps a copy of every request. */
function scripted(...replies: any[]) {
  const seen: any[] = [];
  const fn = (request: any) => {
    seen.push(clone(request));
    return replies.length ? replies.shift() : says("script ran out");
  };
  return { fn, seen };
}
const resultsOf = (request: any) => request?.messages?.[request.messages.length - 1]?.content ?? [];
const close = (a: number, b: number, tol: number) => typeof a === "number" && Math.abs(a - b) <= tol;
const run = (screen: any, name: string, args: Record<string, unknown> = {}, scale = scaleFor(1920, 1080)): any => perform(screen, name, args, scale) ?? ["no result", false];

test("m1 the loop runs scaled actions and sends every result in one message", () => {
  const screen = makeScreen();
  const model = scripted(reply([use("t1", "screenshot")]),
    reply([use("t2", "left_click", { coordinate: [894, 164] }), use("t3", "type", { text: "weather" }), use("t4", "screenshot")]), says("Typed it."));
  const result = runComputerLoop(model.fn, screen) ?? {};
  assert.deepEqual([result.status, result.turns], ["done", 3]);
  assert.deepEqual(screen.log, [["left_click", "search"], ["type", "weather"]]);
  assert.equal(screen.typed, "weather");
  assert.ok(model.seen.length === 3 && model.seen.every((r) => JSON.stringify(r.tools) === JSON.stringify([{ type: "computer_toolset_20260801" }]) && r.model === "claude-sonnet-5-5" && r.max_tokens === 4096));
  assert.deepEqual(resultsOf(model.seen[1]), [{ type: "tool_result", tool_use_id: "t1", toolset_name: "computer", content: [{ type: "image", source: { type: "base64", media_type: "image/png", data: "png:1429x804:0" } }] }]);
  const last = resultsOf(model.seen[2]);
  assert.deepEqual(last.map((r: any) => [r.tool_use_id, r.is_error ?? false]), [["t2", false], ["t3", false], ["t4", false]]);
  assert.deepEqual(last.slice(0, 2).map((r: any) => r.content), ["Clicked search", "Typed 7 characters"]);
  assert.equal(last[2]?.content?.[0]?.source?.data, "png:1429x804:2");
  assert.deepEqual((model.seen[2]?.messages ?? []).map((m: any) => m.role), ["user", "assistant", "user", "assistant", "user"]);
});

test("e1 the screen is scaled to what the model may see and clicks are scaled back", () => {
  assert.ok(scaleFor(1280, 800) === 1.0 && scaleFor(1024, 768) === 1.0);
  assert.ok(close(scaleFor(1920, 1080), 0.744709, 1e-5) && close(scaleFor(2560, 1440), 0.558531, 1e-5) && close(scaleFor(3000, 100), 1568 / 3000, 1e-9));
  assert.deepEqual(scaledSize(1920, 1080), [1429, 804]);
  assert.deepEqual(scaledSize(2560, 1440), [1429, 804]);
  assert.deepEqual(scaledSize(1024, 768), [1024, 768]);
  const screen = makeScreen();
  assert.deepEqual(toScreen(894, 164, scaleFor(1920, 1080), screen), [1200, 220]);
  assert.deepEqual(toScreen(5000, 5000, 1.0, screen), [1919, 1079]);
  assert.deepEqual(toScreen(-3, -3, 1.0, screen), [0, 0]);
});

test("e2 a click on a risky element needs a persons confirmation", () => {
  const scale = scaleFor(1920, 1080);
  const asked: unknown[] = [];
  const yes = (action: unknown) => { asked.push(action); return true; };
  for (const confirm of [null, () => false]) {
    const screen = makeScreen();
    const [content, isError] = (perform as any)(screen, "left_click", { coordinate: [670, 469] }, scale, confirm) ?? ["", false];
    assert.ok(isError === true && String(content).startsWith("Declined: pay needs a person's confirmation (payment)"));
    assert.ok(screen.log.length === 0 && screen.cursor[0] === 0 && screen.cursor[1] === 0);
  }
  const screen = makeScreen();
  assert.deepEqual((perform as any)(screen, "double_click", { coordinate: [670, 469] }, scale, yes) ?? ["", true], ["Clicked pay", false]);
  assert.deepEqual(screen.log, [["double_click", "pay"]]);
  assert.deepEqual(asked, [{ action: "double_click", element: "pay", risk: "payment" }]);
  asked.length = 0;
  assert.deepEqual((perform as any)(screen, "left_click", { coordinate: [894, 164] }, scale, yes) ?? ["", true], ["Clicked search", false]);
  assert.equal(asked.length, 0);
  assert.deepEqual((perform as any)(screen, "left_click", { coordinate: [5, 5] }, scale, yes), ["Clicked nothing", false]);
});

test("e3 a failed action stops the rest of its batch", () => {
  const screen = makeScreen();
  const model = scripted(reply([use("a", "left_click", { coordinate: [894, 164] }), use("b", "left_click", { coordinate: [9999, 5] }), use("c", "type", { text: "x" }), use("d", "screenshot")]), says("Stopped."));
  runComputerLoop(model.fn, screen);
  const results = resultsOf(model.seen[1]);
  assert.deepEqual(results.map((r: any) => [r.tool_use_id, r.is_error ?? false]), [["a", false], ["b", true], ["c", true], ["d", true]]);
  assert.ok(String(results[1]?.content).includes("outside the screenshot") && results[2]?.content === NOT_EXECUTED && results[3]?.content === NOT_EXECUTED);
  assert.ok(results.length === 4 && results.every((r: any) => r.toolset_name === "computer"));
  assert.deepEqual(screen.log, [["left_click", "search"]]);
  assert.equal(screen.typed, "");
});

test("e4 invalid actions come back as error results with a reason", () => {
  const screen = makeScreen();
  assert.deepEqual(run(screen, "key", { text: "ctrl+s", repeat: 2 }), ["Pressed ctrl+s", false]);
  assert.ok(run(screen, "key", { text: "Return", repeat: 101 })[1] === true && run(screen, "key", { repeat: 1 })[1] === true);
  assert.deepEqual(run(screen, "wait", { duration: 2.5 }), ["Waited 2.5s", false]);
  assert.equal(run(screen, "wait", { duration: 301 })[1], true);
  assert.deepEqual(run(screen, "scroll", { scroll_direction: "down", scroll_amount: 3 }), ["Scrolled down 3", false]);
  assert.ok(run(screen, "scroll", { scroll_direction: "sideways", scroll_amount: 3 })[1] === true && run(screen, "scroll", { scroll_direction: "up", scroll_amount: 0 })[1] === true);
  assert.deepEqual(run(screen, "teleport"), ["Unknown action: teleport", true]);
  assert.ok(run(screen, "type")[1] === true && run(screen, "left_click", { coordinate: [5000, 5] })[1] === true);
  assert.deepEqual(run(screen, "mouse_move", { coordinate: [670, 469] }), ["Moved", false]);
  assert.deepEqual(run(screen, "cursor_position"), ["X=670,Y=469", false]);
  assert.ok(run(screen, "zoom", { region: [0, 0, 5000, 5000] })[1] === true && run(screen, "zoom", { region: [300, 200, 100, 100] })[1] === true);
});

test("e5 the loop ends on a stop reason or at the turn limit", () => {
  const model = scripted(...Array.from({ length: 10 }, (_, i) => reply([use(`t${i}`, "screenshot")])));
  const result = runComputerLoop(model.fn, makeScreen(), "claude-sonnet-5-5", 3) ?? {};
  assert.deepEqual([result.status, result.turns, model.seen.length], ["max_turns", 3, 3]);
  assert.equal((runComputerLoop(scripted(reply([{ type: "text", text: "no" }], "refusal")).fn, makeScreen()) ?? {}).status, "refused");
  assert.equal((runComputerLoop(scripted(reply([{ type: "text", text: "cut" }], "max_tokens")).fn, makeScreen()) ?? {}).status, "truncated");
  const paused = scripted(reply([{ type: "text", text: "working" }], "pause_turn"), says("done"));
  const again = runComputerLoop(paused.fn, makeScreen()) ?? {};
  assert.deepEqual([again.status, again.turns], ["done", 2]);
  assert.deepEqual((paused.seen[1]?.messages ?? []).map((m: any) => m.role), ["user", "assistant"]);
});

test("e6 old screenshots are replaced so the context does not fill up", () => {
  const shot = (n: number) => ({ type: "image", source: { type: "base64", media_type: "image/png", data: `png:${n}` } });
  const messages: any[] = [{ role: "user", content: "task" }];
  for (let n = 0; n < 5; n++) {
    messages.push({ role: "assistant", content: [use(`t${n}`, "screenshot")] });
    messages.push({ role: "user", content: [{ type: "tool_result", tool_use_id: `t${n}`, toolset_name: "computer", content: [shot(n)] }] });
  }
  messages.push({ role: "assistant", content: [use("k", "type", { text: "x" })] });
  messages.push({ role: "user", content: [{ type: "tool_result", tool_use_id: "k", content: "Typed 1 characters" }] });
  const before = clone(messages);
  const pruned = pruneScreenshots(messages, 2) ?? [];
  assert.deepEqual(messages, before);
  const contents = (msgs: any[]) => msgs.filter((m) => m.role === "user" && Array.isArray(m.content)).map((m) => m.content[0].content);
  const note = [{ type: "text", text: "[screenshot removed]" }];
  assert.deepEqual(contents(pruned), [note, note, note, [shot(3)], [shot(4)], "Typed 1 characters"]);
  assert.ok(pruned.length === messages.length && JSON.stringify(pruned[0]) === JSON.stringify(messages[0]));
  assert.deepEqual(pruneScreenshots(messages, 9), messages);
  assert.deepEqual(contents(pruneScreenshots(messages, 0) ?? []).slice(0, 5), [note, note, note, note, note]);
});

test("e7 screenshots and zooms are sent at the size the model may see", () => {
  for (const [width, height, size] of [[1920, 1080, "1429x804"], [2560, 1440, "1429x804"], [1280, 800, "1280x800"]] as const) {
    const [content, isError] = run(makeScreen(width, height), "screenshot", {}, scaleFor(width, height));
    assert.ok(isError === false && content?.[0]?.source?.data === `png:${size}:0` && content[0].type === "image" && content[0].source.media_type === "image/png");
  }
  const screen = makeScreen();
  const [zoom, zoomError] = run(screen, "zoom", { region: [0, 0, 300, 200] });
  assert.ok(zoomError === false && zoom?.[0]?.source?.data === "png:403x269:0");
  run(screen, "left_click", { coordinate: [5, 5] });
  assert.equal(run(screen, "screenshot")[0]?.[0]?.source?.data, "png:1429x804:1");
});
