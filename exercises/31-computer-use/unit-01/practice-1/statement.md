# Practice: the loop around the computer use tool

The computer use tool turns Claude into someone who looks at a screenshot and decides on a click, a keystroke or a scroll. Claude
runs nothing: your code takes each action, performs it, and answers with the new screenshot. Three things make that loop hard. The
screen is usually larger than the picture the model may be sent, so every coordinate has to be scaled both ways. A model that reads a
page can be pushed into clicking something it should not, so a risky click needs a person. And an action that fails must not let the
rest of its batch run on a screen that no longer looks as expected. Write the executor and the loop, against a toy screen, and prune
the screenshots that fill the context. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit
the file there. The tool, the actions, the scaling rule and the safety advice come from the Claude documentation on the computer use
tool and on vision, read on 2026-10-03; the lesson pages explain them. Nothing here touches a real desktop or the network: the
screen is a map in memory and the model is a function that you call.

## The given parts

| Name | Meaning |
|---|---|
| screen | `{"width", "height", "cursor": [x, y], "typed", "log", "elements"}`; the log gets one entry per action that changes it (a click is `[name, element id or null]`, a typed text is `["type", text]`, a key press is `["key", text]`, a scroll is `["scroll", direction]`); `elements` are `{"id", "x", "y", "w", "h", "risk"}` rectangles in real screen pixels, where `risk` is `"none"` or a reason such as `"payment"`; when elements overlap the later one wins |
| `render(screen, width, height)` | the screenshot of the screen at that size, as the base64 text of a picture; it changes with every logged action |
| `ask(request)` | the model: takes `{"model", "max_tokens", "tools", "messages"}` and returns `{"content": [blocks], "stop_reason"}` |
| `confirm(action)` | a person's answer: takes `{"action", "element", "risk"}` and returns true or false; it may be missing |
| `TOOLSET` | `computer_toolset_20260801`, the tool type of the current computer use toolset |

A `tool_use` block of the toolset looks like `{"type": "tool_use", "id", "name": "left_click", "toolset_name": "computer", "input":
{"coordinate": [x, y]}}`. Coordinates in `input` are pixels on the screenshot the model was sent.

## What is already written, and what you write

The starter is a working executor and loop with nine small gaps cut out of it. The plumbing is written and correct: the `render`
stub, the element lookup, the click, `type`, `wait`, `mouse_move` and `cursor_position` handling, the batch that stops after a failed
action, the copying of the conversation and the loop itself. Each gap is one small function with its signature, a comment that says
what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value, so the starter runs and fails
the cases on an assertion. To see what a gap receives, log its input with the `log` line at the top of the file (the starter's `perform`
already logs its input); a run shows the logged lines under the failing case. Write them in this order (the TypeScript, Java and Kotlin
names are the camel-case forms):

1. `scale_for` unlocks `e1`, `e7` and `m1`: the factor that shrinks the screen.
2. `scaled_size` unlocks `e7` and `m1`: the size of the screenshot the model is sent.
3. `to_screen` unlocks `e1`, `e2` and `m1`: scale a point back, round half to even, clamp.
4. `_risk_error` unlocks `e2`: the confirmation a risky click needs.
5. `_valid_region` unlocks `e4`: which zoom regions are valid.
6. `_valid_key` unlocks `e4`: the text and the repeat of a key press.
7. `_valid_scroll` unlocks `e4`: the direction and the amount of a scroll.
8. `_to_replace` unlocks `e6`: which screenshots become a note (Java and Kotlin: `removeCount`, how many).
9. `_final_status` unlocks `e5`: the status each stop reason ends the loop with.

About twenty lines in all (Python and TypeScript fewer). The section below describes the whole executor and loop; the parts you do not write
are there so you can see how your functions are used.

## What to write

- `scale_for(width, height)`: `min(1, 1568 / longest side, sqrt(1,150,000 / (width * height)))`. `scaled_size(width, height)` is
  `(int(width * scale), int(height * scale))`.
  This is the documentation's example for models with the 1568 px and about 1.15 megapixel limits, so it is safe for every model. A model in the
  high-resolution tier accepts up to 2576 px on the long edge and 4784 visual tokens, and the lesson explains how that changes the scale.
- `to_screen(x, y, scale, screen)`: divide by the scale, round half to even, and clamp into `0 .. width - 1` and `0 .. height - 1`.
- `perform(screen, name, args, scale, confirm)` returns `(content, is_error)`. The content is a string, or for `screenshot` and `zoom` a
  list holding one block `{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": render(...)}}`.
  A coordinate is valid when it lies inside the scaled screenshot (`0 <= x < scaled width`, the same for y); an invalid one is an error
  `Coordinate <the value> is outside the screenshot`. All actions below change the screen only when they succeed.
  - `screenshot`: the picture at the scaled size. `zoom` with `region` `[x0, y0, x1, y1]` (scaled coordinates, `x0 < x1`, `y0 < y1`, the
    corner `x0, y0` valid, `x1` and `y1` within the scaled size and above 0): the picture at the size of the region on the real screen
    (`to_screen` of both corners, subtracted); otherwise the error `Invalid zoom region: <region>`.
  - `left_click`, `right_click`, `middle_click`, `double_click` and `triple_click`: with a `coordinate` use it; without one use the
    current cursor (real pixels). Find the element under the real point. When it has a `risk` other than `none`, ask `confirm` with
    `{"action": name, "element": id, "risk": risk}`; with no `confirm` or an answer of false the result is the error
    `Declined: <id> needs a person's confirmation (<risk>)` and nothing changes. A click moves the cursor, adds its log entry and
    returns `Clicked <id>` or `Clicked nothing`.
  - `type` with a string `text`: appends to `typed`, logs, returns `Typed <n> characters`; a missing text is an error.
  - `key` with a non-empty `text` and `repeat` (default 1, an integer from 1 to 100): logs, returns `Pressed <text>`; otherwise an error.
  - `wait` with a `duration` from 0 to 300 seconds: returns `Waited <duration>s`; otherwise an error. `scroll` with
    `scroll_direction` (`up`, `down`, `left`, `right`) and an integer `scroll_amount` of at least 1: logs, returns `Scrolled <direction>
    <amount>`; otherwise an error.
  - `mouse_move` with a valid `coordinate`: moves the cursor, returns `Moved`. `cursor_position`: returns `X=<x>,Y=<y>`, the cursor in
    scaled coordinates (multiply by the scale, round half to even).
  - Any other name: the error `Unknown action: <name>`.
- `prune_screenshots(messages, keep=3)`: a copy of the conversation in which every image inside a `tool_result` content list is
  replaced by the text block `{"type": "text", "text": "[screenshot removed]"}`, except the newest `keep` images. The input is not
  changed. With `keep` of 0 every screenshot goes; with `keep` above the number of screenshots none does.
- `run_computer_loop(ask, screen, model="claude-sonnet-5-5", max_turns=10, confirm=None)` returns `{"status", "turns", "messages"}`.
  The first message is the user text `Do the task on the screen.` Each request carries `model`, `max_tokens` 4096,
  `tools` `[{"type": "computer_toolset_20260801"}]` and a snapshot of the messages. After each reply, append it as an assistant
  message. Stop reason `tool_use`: run the `tool_use` blocks in order and append ONE user message with one `tool_result` per call
  (`tool_use_id`, the `toolset_name` of the call when it has one, `content`, and `is_error: true` only on an error). When an action
  fails, every later call of that reply is not run and gets the error result `Not executed: an earlier computer action in this turn
  failed.` `end_turn` or `stop_sequence`: status `done`. `refusal`: status `refused`. `pause_turn`: call again with the assistant
  message appended and no user message. Any other stop reason: status `truncated`. After `max_turns` model calls without an end:
  status `max_turns`.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The loop runs scaled actions and sends every result in one message |
| `e1` | The screen is scaled to what the model may see, and clicks are scaled back |
| `e2` | A click on a risky element needs a person's confirmation |
| `e3` | A failed action stops the rest of its batch |
| `e4` | Invalid actions come back as error results with a reason |
| `e5` | The loop ends on a stop reason or at the turn limit |
| `e6` | Old screenshots are replaced so the context does not fill up |
| `e7` | Screenshots and zooms are sent at the size the model may see |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
