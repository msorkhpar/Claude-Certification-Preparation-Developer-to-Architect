# The computer use tool: the toolset, the loop and the scale

**Level:** Developer · **Module 31:** Computer use · **Page 1 of 2**
**Exams:** DV5, DV6 (and X: this module goes beyond what the exam guides name)

**After this page you can** declare the computer use toolset, explain who does what in its loop, answer a batch of actions correctly, and keep screenshots inside the image limits while mapping Claude's coordinates back onto the real screen.

Checked against the Claude API documentation (Computer use tool, and Vision) on 2026-10-03, with the example run offline in the course container (`anthropic` 1.11.0, `@anthropic-ai/sdk` 0.131.0). There is no desktop and no model in the example: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the Messages API for `claude-sonnet-5-5`, labelled illustrative. Nothing on this page was run against the live API.

## Why it matters

Most tools return data. The computer use tool returns a picture and takes a click. Claude looks at a screenshot, decides on a click, a keystroke or a scroll, and your code carries it out. That makes the loop around the model the product: it decides what a screenshot may contain, what happens when an action fails, and what a person must approve. The exam asks who runs what, how the request is shaped, and what goes wrong when the scale is not mapped back.

## The idea

### One entry, seventeen members

The computer use tool is a client toolset. One entry, `{"type": "computer_toolset_20260801"}`, in the `tools` array "gives Claude 17 member tools such as screenshot, left_click, type, and zoom, and your application runs every call in an environment you control." The request needs no beta header. On the Claude API and Google Cloud, Claude 5.5 and later models support computer use only through this toolset. Earlier models and other platforms still use the earlier `computer_20251124` tool, which needs a beta header.

The members cover looking (`screenshot`, `zoom`), the pointer (`left_click`, `right_click`, `middle_click`, `double_click`, `triple_click`, `left_click_drag`, `mouse_move`, `left_mouse_down`, `left_mouse_up`, `cursor_position`, `scroll`), the keyboard (`type`, `key`, `hold_key`) and `wait`. All of them are on by default, `zoom` included. An environment that cannot produce zoom images withholds the member with the `configs` parameter of the entry, rather than leaving it on and returning errors.

The entry takes no display size. The documentation lists the parameters of the earlier tool that it rejects, with the reason: "coordinates are always in the pixel space of the screenshots you return". A request that includes `name`, `display_width_px`, `display_height_px`, `display_number` or `enable_zoom` is an `invalid_request_error`.

A call arrives as a `tool_use` block whose `name` is the member and which carries `"toolset_name": "computer"`. Dispatch on the pair. A custom tool in the same request can share a member's name, and `toolset_name` is what marks a block as a computer action.

### Who does what

Claude never touches the machine. Your application receives the tool use requests, translates them into actions in your computing environment, captures the results and returns them. The reference implementation puts all of this in a Docker container: a virtual display, a light desktop, a few applications and the loop itself. The loop is the same as in module 26, with a screenshot as the main result:

1. Send the toolset, the user's task and the conversation so far.
2. When `stop_reason` is `tool_use`, run every `tool_use` block, in order, in your environment.
3. Send one new user message with one `tool_result` per call, matched by `tool_use_id` and echoing `"toolset_name": "computer"`. Return an image for `screenshot` and `zoom`. A short text such as `OK` is enough for the other actions, and `cursor_position` returns its coordinates as text.
4. Repeat until Claude answers without a tool call, or until a turn limit that you set stops the loop. The limit is a safeguard against "potential infinite loops that could result in unexpected API costs".

### Batch actions

Claude often plans a short sequence, such as click, type, then screenshot, and returns it as several blocks in one reply. This is a batch action. It has the shape of parallel tool use with one difference: you run the blocks in order, never at the same time. Later actions depend on earlier ones, because the `type` enters text into whatever the click focused.

The rule is to run the blocks in order and stop at the first failure. The documentation says "if one fails, don't run the rest". Every block still needs a result, and the answer has three parts:

- A normal result for each action that succeeded.
- `is_error: true` with a description for the one that failed.
- `is_error: true` with exactly this text for every later action: `Not executed: an earlier computer action in this turn failed.`

Claude then sees which actions ran, which failed and which were skipped, and replans. Leaving a block unanswered is an `invalid_request_error`, "so an agent loop that reads only the first block fails on its next call". If your application asks a person to confirm risky actions, make that check before each block runs, because a batch can finish a multistep action inside one turn.

### Screenshots must fit, and coordinates are theirs

A screen is usually larger than the picture a model may be sent, and the toolset does nothing about it. The documentation says "the toolset takes no display dimensions and the API doesn't downscale for you", and an oversized image in a `tool_result` is rejected with a validation error. So you resize each screenshot before returning it, and you keep the scale factor.

Claude's coordinates are in the pixel space of the image it saw. That holds after a `zoom` too: Claude "still expresses coordinates in the full screenshot's space, never relative to the zoomed image". The rule that follows is the documentation's own: "If you scale screenshots down before returning them, scale Claude's coordinates back up before applying them to the real display." On a macOS Retina display the screenshot is already twice the logical size, so halve either the picture or the coordinates.

The limit depends on the model. Claude Opus 4.7 and later, "including every model that supports" this toolset, accept up to 2576 pixels on the long edge and 4784 visual tokens, about 3.75 megapixels. Earlier models accept 1568 pixels and about 1.15 megapixels. The documentation's own example uses the smaller numbers, and so does the example below and the practice of the next page: it is safe on every model, and it costs detail on a high-resolution model. For that tier, size to the visual-token limit with the reference helper of the vision page (module 30).

A failed action is reported the same way as in any tool loop: a `tool_result` with `is_error: true`, the `toolset_name`, and a short message that says what went wrong, such as a coordinate outside the display.

<!-- example: m31-screen-loop tabs: python,typescript -->
```python
"""The loop around the computer use toolset, on a toy screen: scaling both ways, a batch, a halt after a failure and a confirmation.

There is no desktop and no model: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the
Messages API (claude-sonnet-5-5), illustrative and not captures. The tool entry, the batch rule and the halt text are those of the
"Computer use tool" page of the Claude documentation, checked on 2026-10-03.
"""
import base64
import json
import math

from harness import scripted_client
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
TOOLSET = {"type": "computer_toolset_20260801"}
HALT = "Not executed: an earlier computer action in this turn failed."
SCREEN = {"width": 2560, "height": 1440, "typed": "", "log": [],
          "elements": [{"id": "search", "x": 800, "y": 100, "w": 900, "h": 60, "risk": "none"}, {"id": "buy", "x": 2000, "y": 1200, "w": 300, "h": 80, "risk": "payment"}]}


def scale_for(width, height):
    """The documentation's example limits (1568 px on the long edge, about 1.15 megapixels): small enough for every model."""
    return min(1, 1568 / max(width, height), math.sqrt(1_150_000 / (width * height)))


def to_screen(x, y, scale, screen):
    return (min(max(round(x / scale), 0), screen["width"] - 1), min(max(round(y / scale), 0), screen["height"] - 1))


def element_at(screen, x, y):
    hit = [e for e in screen["elements"] if e["x"] <= x < e["x"] + e["w"] and e["y"] <= y < e["y"] + e["h"]]
    return hit[-1] if hit else None


def screenshot(screen, scale):
    size = f'{int(screen["width"] * scale)}x{int(screen["height"] * scale)}'
    return [{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": base64.b64encode(f"{size}:{len(screen['log'])}".encode()).decode()}}]


def perform(screen, name, args, scale, confirm):
    """Run one member of the toolset on the toy screen; return (content, is_error)."""
    if name == "screenshot":
        return screenshot(screen, scale), False
    if name == "left_click":
        x, y = to_screen(*args["coordinate"], scale, screen)
        target = element_at(screen, x, y)
        if target and target["risk"] != "none" and not (confirm and confirm({"action": name, "element": target["id"], "risk": target["risk"]})):
            return f"Declined: {target['id']} needs a person's confirmation ({target['risk']})", True
        screen["log"].append(["click", target["id"] if target else None])
        return f"Clicked {target['id'] if target else 'nothing'} at {x},{y} on the screen", False
    if name == "type":
        screen["typed"] += args["text"]
        screen["log"].append(["type", args["text"]])
        return f"Typed {len(args['text'])} characters", False
    if name == "key":
        screen["log"].append(["key", args["text"]])
        return f"Pressed {args['text']}", False
    return f"Unknown action: {name}", True


def run_loop(client, screen, confirm=None, max_turns=6):
    scale = scale_for(screen["width"], screen["height"])
    messages = [{"role": "user", "content": "Search for a kettle and buy the first one."}]
    for turn in range(1, max_turns + 1):
        reply = client.messages.create(model=MODEL, max_tokens=4096, tools=[TOOLSET], messages=messages)
        messages.append({"role": "assistant", "content": [b.model_dump(exclude_none=True) for b in reply.content]})
        if reply.stop_reason != "tool_use":
            return {"status": "done", "turns": turn, "answer": reply.content[-1].text}
        results, failed = [], False
        for block in [b for b in reply.content if b.type == "tool_use"]:
            if failed:
                content, is_error = HALT, True
            else:
                content, is_error = perform(screen, block.name, block.input, scale, confirm)
                failed = is_error
            print(f"turn {turn}: {block.name} {json.dumps(block.input, separators=(',', ':'))} -> {'ERROR ' if is_error else ''}{content if isinstance(content, str) else 'image'}")
            results.append({"type": "tool_result", "tool_use_id": block.id, "toolset_name": block.toolset_name, "content": content, **({"is_error": True} if is_error else {})})
        messages.append({"role": "user", "content": results})
    return {"status": "max_turns", "turns": max_turns, "answer": None}


def call(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "toolset_name": "computer", "input": input}


def main():
    scale = scale_for(SCREEN["width"], SCREEN["height"])
    point = lambda e: [round((e["x"] + e["w"] / 2) * scale), round((e["y"] + e["h"] / 2) * scale)]  # noqa: E731 - what the model would see
    search, buy = SCREEN["elements"]
    client, transport = scripted_client(
        message([call("toolu_1", "screenshot"), call("toolu_2", "left_click", coordinate=point(search)), call("toolu_3", "type", text="kettle"), call("toolu_4", "key", text="Return")], "tool_use", model=MODEL),
        message([call("toolu_5", "left_click", coordinate=point(buy)), call("toolu_6", "screenshot")], "tool_use", model=MODEL),
        message([text("I did not buy it: the payment button needs your confirmation.")], model=MODEL))
    print(f"screen {SCREEN['width']}x{SCREEN['height']}, scale {scale:.4f}, the model sees {int(SCREEN['width'] * scale)}x{int(SCREEN['height'] * scale)}")
    result = run_loop(client, SCREEN, confirm=lambda action: False)
    print("result:", result["status"], "after", result["turns"], "turns:", result["answer"])
    print("screen log:", json.dumps(SCREEN["log"], separators=(",", ":")), "typed:", SCREEN["typed"])
    first = transport.requests[0]
    print("tools sent:", json.dumps(first["tools"], separators=(",", ":")), "beta header sent:", "anthropic-beta" in transport.headers[0])
    last = transport.requests[2]["messages"][-1]["content"]
    print("second result message:", [(r["tool_use_id"], r["toolset_name"], r.get("is_error", False)) for r in last])


if __name__ == "__main__":
    main()
```
```text
screen 2560x1440, scale 0.5585, the model sees 1429x804
turn 1: screenshot {} -> image
turn 1: left_click {"coordinate":[698,73]} -> Clicked search at 1250,131 on the screen
turn 1: type {"text":"kettle"} -> Typed 6 characters
turn 1: key {"text":"Return"} -> Pressed Return
turn 2: left_click {"coordinate":[1201,693]} -> ERROR Declined: buy needs a person's confirmation (payment)
turn 2: screenshot {} -> ERROR Not executed: an earlier computer action in this turn failed.
result: done after 3 turns: I did not buy it: the payment button needs your confirmation.
screen log: [["click","search"],["type","kettle"],["key","Return"]] typed: kettle
tools sent: [{"type":"computer_toolset_20260801"}] beta header sent: False
second result message: [('toolu_5', 'computer', True), ('toolu_6', 'computer', True)]
```
```typescript
// The loop around the computer use toolset, on a toy screen: scaling both ways, a batch, a halt after a failure and a confirmation.
//
// There is no desktop and no model: the screen is a few rectangles in memory and the replies are hand-written bodies in the shape of the
// Messages API (claude-sonnet-5-5), illustrative and not captures. The tool entry, the batch rule and the halt text are those of the
// "Computer use tool" page of the Claude documentation, checked on 2026-10-03.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

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
```
```text
screen 2560x1440, scale 0.5585, the model sees 1429x804
turn 1: screenshot {} -> image
turn 1: left_click {"coordinate":[698,73]} -> Clicked search at 1250,131 on the screen
turn 1: type {"text":"kettle"} -> Typed 6 characters
turn 1: key {"text":"Return"} -> Pressed Return
turn 2: left_click {"coordinate":[1201,693]} -> ERROR Declined: buy needs a person's confirmation (payment)
turn 2: screenshot {} -> ERROR Not executed: an earlier computer action in this turn failed.
result: done after 3 turns: I did not buy it: the payment button needs your confirmation.
screen log: [["click","search"],["type","kettle"],["key","Return"]] typed: kettle
tools sent: [{"type":"computer_toolset_20260801"}] beta header sent: False
second result message: [('toolu_5', 'computer', True), ('toolu_6', 'computer', True)]
```
<!-- /example -->

The toy screen is 2560 by 1440. The area limit binds first (a scale of 0.5585, against 0.6125 from the edge alone), so the model sees 1429 by 804. Claude clicks at 698, 73 in that picture, and the loop divides by the scale and clicks at 1250, 131 on the real screen. The batch in turn 2 shows the halt rule: the click on the payment button is declined, and the screenshot after it is answered with the fixed text instead of being run. The last lines show that the tools array holds one entry and that no beta header was sent.

## Traps

1. **Reading only the first block of a reply.** A batch has several `tool_use` blocks, and every one needs a result. Run them in order, stop at the first failure and answer the rest with the halt text.
2. **Clicking where Claude said.** The coordinates are in the picture you returned. If you shrank it, divide by the scale, and clamp the result into the screen.
3. **Sending the full screen.** The API does not downscale a screenshot for you. An oversized one is rejected, and a coordinate from a resized one is only right after you map it back.
4. **Reusing the earlier tool by habit.** On the Claude API, Claude 5.5 and later models use the toolset with no beta header. The earlier `computer_20251124` entry belongs to older models and other platforms.

## Quiz

1. A team adds the computer use toolset to a request and expects Claude to press keys on the machine. Which part performs the keystroke?
   - **a**: Claude itself, through a remote link that the toolset opens
   - **b**: An Anthropic server that replays the call on the desktop
   - **c**: The beta header on the request, which enables key presses
   - **d**: Your own program, inside an environment that you set up

2. A single reply from Claude contains a click, a text entry and a screenshot request, and the click fails. What should the application send back for the last two calls?
   - **a**: Normal results for both, because the earlier failure does not matter
   - **b**: The screenshot for the last call and an error for the text entry
   - **c**: Nothing for them, since the batch is abandoned after a failure
   - **d**: Error results with the fixed halt wording, and neither action run

3. The application shrinks every screenshot to half its size before returning it. Claude replies with a click at a spot in the shrunken image. What must the application do before clicking?
   - **a**: Click exactly where Claude said, since the toolset converts coordinates
   - **b**: Treat the numbers as relative to the last zoomed region
   - **c**: Give the toolset the true display size so that the API scales for you
   - **d**: Scale the position back up by the factor it used, onto the real display

<details>
<summary>Answer key</summary>

1. **d**. The page says the toolset "gives Claude 17 member tools such as screenshot, left_click, type, and zoom, and your application runs every call in an environment you control." *a* is ruled out because the documentation describes the loop as one where the application translates the calls, and Claude "never touches the machine" in this design. *b* is ruled out because "Your application receives the tool use requests, translates them into actions in your computing environment", and no Anthropic server replays them. *c* is ruled out because "The request needs no beta header."
2. **d**. The page says "if one fails, don't run the rest", and that every later action gets `is_error: true` with exactly the text `Not executed: an earlier computer action in this turn failed.` *a* is ruled out because "Later actions depend on earlier ones, because the type enters text into whatever the click focused." *b* is ruled out because a screenshot after a failed click would run against a screen that no longer looks as expected, and "if one fails, don't run the rest". *c* is ruled out because leaving a block unanswered is an invalid request, "so an agent loop that reads only the first block fails on its next call".
3. **d**. The page quotes the documentation: "If you scale screenshots down before returning them, scale Claude's coordinates back up before applying them to the real display." *a* is ruled out because "coordinates are always in the pixel space of the screenshots you return", which is the shrunken picture here. *b* is ruled out because after a zoom Claude "still expresses coordinates in the full screenshot's space, never relative to the zoomed image". *c* is ruled out because "the toolset takes no display dimensions and the API doesn't downscale for you".

</details>
