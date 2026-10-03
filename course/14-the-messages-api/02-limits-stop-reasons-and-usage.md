# max_tokens, stop sequences, stop_reason and usage

**Level:** Developer · **Module 14:** The Messages API · **Page 2 of 3**
**Exams:** DV1, DV2

**After this page you can** set an output limit on purpose, read `stop_reason` and `usage` after every call, handle
each documented stop reason, and avoid the sampling settings that current models reject.

Checked against the Claude API documentation (Handling stop reasons, Using the Messages API, rate limits, API
errors) and the course's version notes on 2026-10-02 for `claude-fable-5-1`, `claude-opus-5-5`, `claude-sonnet-5-5` and
`claude-haiku-4-5-20251001`. Replies shown are hand-scripted and labelled illustrative.

## Why it matters

A reply can be successful at the HTTP level and still useless: cut in the middle of a sentence, cut in the middle of a
JSON object, empty, or a refusal. Each of those has a different cause and a different cure, and the response says
which one it is in a single field. The exam asks about this a lot, because it is the cheapest piece of defensive
code in the whole API and the most often skipped. The same response also carries the token counts that feed every cost
and capacity decision in module 12.

## The idea

### max_tokens: a ceiling, not a target

`max_tokens` is required. It is the **largest number of tokens the model may produce for this reply**. The model usually
stops by itself, well before the ceiling; the ceiling only decides where it is cut off if it does not. Three
consequences:

- Set it from the shape of the answer you expect, with headroom. An 80-word summary needs a few hundred tokens, and a
  code file may need thousands. Each model has its own maximum: 128K for Claude Fable 5.1, Opus 5.5 and Sonnet 5.5,
  64K for Claude Haiku 4.5.
- A reply that hits the ceiling stops where it stands. If it was writing JSON, the JSON is incomplete.
- A larger ceiling does not slow short answers and, as far as the rate limits go, it costs nothing extra: the rate-limit
  page says "the `max_tokens` parameter does not factor into OTPM rate limit calculations", so "there is no rate limit
  downside to setting a higher `max_tokens` value". The exception is a very large ceiling on a non-streaming request,
  which module 13 and module 17 explain.

### stop_sequences

`stop_sequences` is a list of strings. When the model produces one, generation stops at once, the reply ends before
the string, and the response reports `stop_reason: "stop_sequence"` with the matching string in the `stop_sequence`
field. They are a cheap way to end a reply at a delimiter you chose, such as `</answer>` or a line you told the
model to write last.

### stop_reason: why the reply ended

Read it after every call. The documentation gives seven values:

| `stop_reason` | Meaning | What your code does |
|---|---|---|
| `end_turn` | The model finished naturally | Use the reply |
| `max_tokens` | The reply reached `max_tokens` | Raise the limit and ask again, or treat the reply as cut; a cut `tool_use` block must be retried with a higher limit |
| `stop_sequence` | The model wrote one of your stop sequences | Read `stop_sequence` to see which one |
| `tool_use` | The model wants a tool run | Run the tools and send back `tool_result` blocks (module 22) |
| `pause_turn` | A server-side tool loop reached its iteration limit (10 by default) | Send the reply back as an assistant turn and call again so the model can finish |
| `refusal` | The model declined to answer | Read `stop_details`, which names the policy category; plan a fallback |
| `model_context_window_exceeded` | The context window filled before `max_tokens` | Treat the reply as truncated: valid but limited |

Two rules sit under the table. First, in the documentation's words, "stop_reason indicates normal completion; HTTP
errors indicate failures": a `max_tokens` or a `refusal` arrives with status 200, so a client that looks only at the
HTTP status will not see it. Second, when you stream, `stop_reason` arrives in the `message_delta` event near the end,
not at the start (module 17).

<!-- illustrative -->
The code that every client needs, as a switch. Hand-written for this page, in the shape of the table.

```text
switch reply.stop_reason
  end_turn, stop_sequence        -> use reply
  max_tokens                     -> mark truncated; retry with a larger limit or continue in a new turn
  model_context_window_exceeded  -> mark truncated; shorten the history
  tool_use                       -> run tools, append tool_result, call again
  pause_turn                     -> append the reply as an assistant turn, call again
  refusal                        -> use the fallback path; log stop_details
  anything else                  -> log it and fail safe: the list of values can grow
```
<!-- /illustrative -->

The last line is the versioning rule of module 13 again: new values can be added to enum-like fields, so the default
branch must exist.

### The empty reply

A reply with `end_turn` and no content is almost always self-inflicted. The documentation traces it to adding text
blocks directly after tool results, which "teaches Claude to expect user input after every tool use". The cure is to
send the result blocks alone, and as a last resort a separate user turn that says to continue.

### usage: the numbers behind the bill

Every response has a `usage` object. The fields you will use:

- `input_tokens` and `output_tokens`: what the call consumed.
- With prompt caching, `cache_creation_input_tokens` and `cache_read_input_tokens`. The rate-limit page is careful about
  `input_tokens`: it "only represents tokens that appear **after your last cache breakpoint**", and the total input is
  the sum of three numbers:

```text
total_input_tokens = cache_read_input_tokens + cache_creation_input_tokens + input_tokens
```

- `inference_geo`, which says where the inference ran (module 12).

For example, with a 200,000-token cached document and a 50-token question, `input_tokens` reads 50 although the total input is 200,050.
Keep the running totals per conversation and per feature. They are your cost meter, and they tell you when a
conversation is growing toward the context window, the subject of the next page.

### Sampling settings you must not send

`temperature`, `top_p` and `top_k` used to be the knobs of randomness. On the models the course uses, Claude Fable 5.1,
Opus 5.5 and Sonnet 5.5, **a non-default value of any of them is rejected with a 400 error**; the documentation tells you
to omit them and steer the behaviour through prompting instead. Claude Haiku 4.5 predates that rule, and the course
does not use a sampling setting on it. If you maintain code that sets `temperature=0` for "determinism", the upgrade is
a deletion, not a change of value.

## Traps

1. **Checking the status and not `stop_reason`.** A 200 with `max_tokens` is a cut reply, and a 200 with `refusal` is
   not an answer. Branch on both.
2. **Parsing a reply that stopped at `max_tokens` as if it were complete.** A half-written JSON object fails to parse
   or, worse, parses into something partial. Check `stop_reason` before you parse.
3. **Keeping `temperature` on an upgrade.** Code that sent `temperature=0.2` to an older model returns a 400 on the
   current ones. Omit the three sampling settings.

## Quiz

1. A nightly job asks for a JSON report and sometimes crashes in the JSON parser, with no error status from the API.
   Which first check does the page recommend?
   - **a**: Lower the sampling temperature so that the report is less variable from one night to the next
   - **b**: Read `stop_reason` and treat `max_tokens` as a cut reply before using the text
   - **c**: Add a stop sequence at the end of every field that the report holds
   - **d**: Catch the HTTP status of the call and retry every status above 399

2. A team upgrades from an older model to Claude Sonnet 5.5 and every call returns a 400 until they delete a line that
   sets the sampling temperature to 0.2. What should they do about `top_p`, which they also set?
   - **a**: Replace it with `top_k`, which the page says the model still accepts
   - **b**: Keep it and raise it to 1.0, which the page says is safest
   - **c**: Keep it, because only the temperature setting is rejected on that model and the others pass
   - **d**: Omit it as well, since any non-default value is rejected for that release

<details>
<summary>Answer key</summary>

1. **b**. The page says "stop_reason indicates normal completion; HTTP errors indicate failures", so a cut reply arrives as a normal 200 and only `stop_reason` shows it. *a* is ruled out because "a larger ceiling does not slow short answers" and variability is not what truncates a reply. *c* is ruled out because a stop sequence ends a reply early at a chosen string and does not stop it being cut: "the model usually stops by itself, well before the ceiling". *d* is ruled out because a `max_tokens` cut carries a 200 status: "a `max_tokens` or a `refusal` arrives with status 200".
2. **d**. The page says a non-default value of any of the three settings "is rejected with a 400 error" on the current models. *b* is ruled out because "the upgrade is a deletion, not a change of value", and the page does not name 1.0 or any value as safe. *c* is ruled out because "a non-default value of any of them is rejected with a 400 error", and the three are named together. *a* is ruled out because "temperature, top_p and top_k used to be the knobs of randomness", and the page rejects all three, so `top_k` is no way out.

</details>
