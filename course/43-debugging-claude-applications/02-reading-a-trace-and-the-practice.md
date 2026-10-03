# Reading a trace, isolating the origin and the practice

**Level:** Developer · **Module 43:** Debugging Claude applications · **Page 2 of 2**
**Exams:** DV8

**After this page you can** read a trace of a Claude application from its first failing event, tell whether the integration or the model is at fault with a question that has a checkable answer, keep the evidence that support and your own team need, turn a fixed bug into a regression case, and write the module's practice, a failure diagnosis.

Checked on 2026-10-03 against the Claude API documentation pages "Claude API errors", "Stop reasons and fallback" and "Handle streaming refusals", and against the Developer exam guide (version 1.0). The traces in the example and the practice are hand-written, shaped like the documented replies, and carry no live output. The example runs offline in Python and TypeScript, and the practice in Python, TypeScript, Java and Kotlin.

## Why it matters

A trace is what you have at 2 a.m.: a list of what the application sent, what came back and what it did next. The exam's trace question gives you a few lines of one and asks what failed and who owns it. The skill is not to match a symptom to a story. It is to read the events in order, stop at the first one that went wrong, and ask the question that separates your code from the model's output.

## The idea

### What a trace holds

A useful trace is an ordered list of events, each small enough to read at a glance:

- **Requests:** the model id, `max_tokens`, the tool names offered, and the kinds of block in the last user message (a tool result? text after it?).
- **Responses and errors:** the status, the `type` and `message` of an error, the `stop_reason` of a success, and the content blocks.
- **Tool calls and results:** the tool name and input, whether the result was flagged as an error, and the exception if your own tool code raised one.
- **Parse steps:** whether your code could read the model's output, and the text it was given.
- **The request id** of every call. The errors page says "Every API response includes a unique `request-id` header", and the same value is the `request_id` field of an error body. The Python and TypeScript SDKs expose it as `_request_id` on a response. Log it, and give it to support when a fault is the provider's.

The logging rules of module 15 apply: record the shape and the ids, and never a key or a customer's text in the clear.

### The reading routine

This routine is the course's own. It is short enough to run in your head, and the practice encodes it.

1. **Find the first failure.** Later events are often consequences: a parse error after a truncated reply, a second 400 after a bad retry. Diagnose the first event that went wrong.
2. **Is there a status?** An `error` event has one, and the table of page 1 gives its origin and recovery. A dropped connection has none: it is the network, and it belongs to the service side.
3. **Was a 200 really a success?** Read `stop_reason`. A refusal, a `max_tokens` stop, a context-window stop or a pause is a failure of a successful response.
4. **Is the content there?** An `end_turn` with no content blocks is an empty reply. Look at the request before it.
5. **Did your code read it?** A parse step that failed is the next question, below.
6. **Did a tool misbehave?** Compare the tool name the model called with the list you offered, and look for an exception in your own tool code.
7. **Name the origin, pick the recovery, and note whether the application recovered.** A later good response means the failure was survived, which still matters for the capacity plan.

### Integration or model: the question that decides

An integration fault and a model fault can look the same in the output, so each needs a question whose answer is in the trace.

| Symptom | Ask | If yes | If no |
|---|---|---|---|
| An empty `end_turn` | Did a text block follow a `tool_result` in the last user message? | The integration: send the tool result alone | The model: add a new user message that asks it to continue |
| A parse step failed | Is there a JSON object in the text, from the first `{` to the last `}`? | The integration: the parser was too strict, so extract the object first | The model: it did not produce JSON, so validate and ask again |
| A tool call came back as an error | Is the tool name one that the request offered? | The tool's own code is next to check | The model: it invented a name, so return an error result and let it correct itself |
| A tool result carries an exception | Did your tool code raise it? | The integration: fix the tool | Not applicable: a tool that reports an error as designed is not a failure |
| A reply is cut off | Is the stop reason `max_tokens`? | The integration's limit | Look elsewhere |

Two replay techniques turn these questions into evidence. **Replay the same request:** a bug in your request fails the same way each time, and an output problem varies. **Replace the model with a script:** if your code fails against a scripted reply that has the right shape, the fault is in your code, and if it holds, the fault is in the output the model gave. Module 15 made this its most valuable tool: reproduce the failure offline.

### Cascades and the first failure

A trace often holds several failures. A 529 is retried and succeeds, so the application recovered. A truncated reply is parsed, and the parse fails: the parse error is a symptom, and the first failure is the stop reason. Read from the top. The diagnosis names the first failing event, and a flag says whether a later response recovered, so a dashboard can tell "survived a blip" from "down".

### A trace read in the container

The example reads three hand-written traces, each a list of events. Trace A is a tool loop that ends in an empty reply, and the last user message had text after the tool result. Trace B is a 529 followed by a retry that succeeded. Trace C is a parse failure on JSON inside a code fence. The function that reads them is a small version of the practice's: it returns the first failure, its origin and the next action. The source of both languages is shown, and under each is what it printed in the container.

<!-- example: m43-read-a-trace tabs: python,typescript -->
```python
"""Reading a trace: where did it fail, in the integration or in the model, and what should happen next?

The Claude documentation on API errors and on stop reasons (read on 2026-10-03) lists the error types and says that a stop reason is part of
a successful response ("Response contains valid content") while an error is a 4xx or 5xx status. It also says that adding text right after
a tool result can make Claude end its turn with an empty reply. This file reads three hand-written traces, each a list of events, and names
the first failure, its origin and the next action. The traces are scripted and carry no live output.
"""
import json

ORIGIN = {"invalid_request_error": "integration", "authentication_error": "account", "rate_limit_error": "service", "api_error": "service",
          "overloaded_error": "service", "timeout_error": "service"}
NEXT = {"invalid_request_error": "fix the request, do not retry", "authentication_error": "fix the credential", "rate_limit_error": "wait, then retry",
        "api_error": "retry with back-off", "overloaded_error": "retry with back-off", "timeout_error": "stream the request"}

TRACES = {
    "A: a tool loop that ends in silence": [
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "response", "status": 200, "stop_reason": "tool_use", "content": [{"type": "tool_use"}]},
        {"kind": "request", "last_user_blocks": ["tool_result", "text"]},
        {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": []},
    ],
    "B: a busy service and a retry": [
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "error", "status": 529, "error_type": "overloaded_error"},
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": [{"type": "text"}]},
    ],
    "C: JSON in a code fence": [
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": [{"type": "text"}]},
        {"kind": "parse", "ok": False, "text": '```json\n{"label": "spam"}\n```'},
    ],
}


def first_failure(trace):
    """(index, what, origin, next action) of the first failing event, or None."""
    last_blocks = []
    for i, e in enumerate(trace):
        if e["kind"] == "request":
            last_blocks = e["last_user_blocks"]
        elif e["kind"] == "error":
            return i, e["error_type"], ORIGIN[e["error_type"]], NEXT[e["error_type"]]
        elif e["kind"] == "response" and e["stop_reason"] == "end_turn" and not e["content"]:
            if "tool_result" in last_blocks and "text" in last_blocks[last_blocks.index("tool_result"):]:
                return i, "empty reply", "integration", "send the tool result alone, with no text after it"
            return i, "empty reply", "model", "add a new user message that asks it to continue"
        elif e["kind"] == "parse" and not e["ok"]:
            start, end = e["text"].find("{"), e["text"].rfind("}")
            try:
                json.loads(e["text"][start:end + 1])
                return i, "parse failure", "integration", "extract the JSON object before parsing"
            except ValueError:
                return i, "parse failure", "model", "validate the output and retry"
    return None


def main():
    for name, trace in TRACES.items():
        found = first_failure(trace)
        recovered = found is not None and any(e["kind"] == "response" and e["stop_reason"] == "end_turn" and e["content"] for e in trace[found[0] + 1:])
        print(name)
        print(f"  first failure: event {found[0]}, {found[1]}; origin: {found[2]}; next: {found[3]}; recovered later: {recovered}")


if __name__ == "__main__":
    main()
```
```text
A: a tool loop that ends in silence
  first failure: event 3, empty reply; origin: integration; next: send the tool result alone, with no text after it; recovered later: False
B: a busy service and a retry
  first failure: event 1, overloaded_error; origin: service; next: retry with back-off; recovered later: True
C: JSON in a code fence
  first failure: event 2, parse failure; origin: integration; next: extract the JSON object before parsing; recovered later: False
```
```typescript
// Reading a trace: where did it fail, in the integration or in the model, and what should happen next?
//
// The Claude documentation on API errors and on stop reasons (read on 2026-10-03) lists the error types and says that a stop reason is part of
// a successful response ("Response contains valid content") while an error is a 4xx or 5xx status. It also says that adding text right after
// a tool result can make Claude end its turn with an empty reply. This file reads three hand-written traces, each a list of events, and names
// the first failure, its origin and the next action. The traces are scripted and carry no live output.

type Event = Record<string, any>;

const ORIGIN: Record<string, string> = {
  invalid_request_error: "integration", authentication_error: "account", rate_limit_error: "service", api_error: "service",
  overloaded_error: "service", timeout_error: "service",
};
const NEXT: Record<string, string> = {
  invalid_request_error: "fix the request, do not retry", authentication_error: "fix the credential", rate_limit_error: "wait, then retry",
  api_error: "retry with back-off", overloaded_error: "retry with back-off", timeout_error: "stream the request",
};

export const TRACES: Record<string, Event[]> = {
  "A: a tool loop that ends in silence": [
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "response", status: 200, stop_reason: "tool_use", content: [{ type: "tool_use" }] },
    { kind: "request", last_user_blocks: ["tool_result", "text"] },
    { kind: "response", status: 200, stop_reason: "end_turn", content: [] },
  ],
  "B: a busy service and a retry": [
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "error", status: 529, error_type: "overloaded_error" },
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "response", status: 200, stop_reason: "end_turn", content: [{ type: "text" }] },
  ],
  "C: JSON in a code fence": [
    { kind: "request", last_user_blocks: ["text"] },
    { kind: "response", status: 200, stop_reason: "end_turn", content: [{ type: "text" }] },
    { kind: "parse", ok: false, text: '```json\n{"label": "spam"}\n```' },
  ],
};

/** [index, what, origin, next action] of the first failing event, or null. */
export function firstFailure(trace: Event[]): [number, string, string, string] | null {
  let lastBlocks: string[] = [];
  for (let i = 0; i < trace.length; i++) {
    const e = trace[i];
    if (e.kind === "request") {
      lastBlocks = e.last_user_blocks;
    } else if (e.kind === "error") {
      return [i, e.error_type, ORIGIN[e.error_type], NEXT[e.error_type]];
    } else if (e.kind === "response" && e.stop_reason === "end_turn" && e.content.length === 0) {
      const at = lastBlocks.indexOf("tool_result");
      if (at >= 0 && lastBlocks.slice(at).includes("text")) return [i, "empty reply", "integration", "send the tool result alone, with no text after it"];
      return [i, "empty reply", "model", "add a new user message that asks it to continue"];
    } else if (e.kind === "parse" && !e.ok) {
      const start = e.text.indexOf("{");
      const end = e.text.lastIndexOf("}");
      try {
        JSON.parse(e.text.slice(start, end + 1));
        return [i, "parse failure", "integration", "extract the JSON object before parsing"];
      } catch {
        return [i, "parse failure", "model", "validate the output and retry"];
      }
    }
  }
  return null;
}

function main() {
  for (const [name, trace] of Object.entries(TRACES)) {
    const found = firstFailure(trace)!;
    const recovered = trace.slice(found[0] + 1).some((e) => e.kind === "response" && e.stop_reason === "end_turn" && e.content.length > 0);
    console.log(name);
    console.log(`  first failure: event ${found[0]}, ${found[1]}; origin: ${found[2]}; next: ${found[3]}; recovered later: ${recovered ? "True" : "False"}`);
  }
}

if (import.meta.main) main();
```
```text
A: a tool loop that ends in silence
  first failure: event 3, empty reply; origin: integration; next: send the tool result alone, with no text after it; recovered later: False
B: a busy service and a retry
  first failure: event 1, overloaded_error; origin: service; next: retry with back-off; recovered later: True
C: JSON in a code fence
  first failure: event 2, parse failure; origin: integration; next: extract the JSON object before parsing; recovered later: False
```
<!-- /example -->

The three outputs show the three cases of the table. The example's table of origins is simpler than the practice's: it files every `rate_limit_error` under the service, and the spend-cap 429 that page 1 sends to the account is a case it leaves out. The practice tells the two apart by the `retry-after` header and the error code. Trace A's empty reply is the integration's: the page on stop reasons says text after a tool result teaches the model to end its turn, so the fix is to send the tool result alone, and a prompt change would not help. Trace B's 529 is the service's, the next action is a retry with back-off, and the example reports that a later response recovered. Trace C's JSON was in the text, inside a fence, so the integration's parser was too strict and the fix is to extract the object. The same text with no object in it would have been the model's, with a different recovery: validate the output and ask again. Both languages print the same text.

### Fix at the right layer, then keep the case

A fix belongs where the origin is. An integration fault is fixed in code and proved with a test that scripts the trace. A model fault is handled with a prompt, a validation step, a fallback model or a retry with the specific error, and it is measured with an eval (module 42). A service fault gets back-off and a budget (module 15). An account fault goes to a person.

Then keep the evidence. A trace that went wrong in production is the best test case you will ever get: scrub it, give it an id and a tag, and add it to the eval set or the test suite. The bug cannot return unseen, and the set grows toward the real distribution of inputs.

### The practice

The practice is the diagnosis function. You write, in the language of your choice (Python, TypeScript, Java or Kotlin), `diagnose`, which reads a trace of events (requests, responses, errors, network errors, tool calls and results, parse steps) and returns the first failure: its position, type, origin and recovery, and whether a later response recovered. The statement lists the events and every rule: the HTTP table, the two kinds of 429, the 400 spend limit, the stop reasons, the empty reply, the parse failure, the tool failures and the network error. The Java and Kotlin folders give you a small `Json` helper, because those two have no JSON library in the course's offline image. The starter fails every test, the reference passes, and each planted wrong solution fails on an assertion. The statement is at `exercises/43-debugging-claude-applications/unit-01/practice-1/statement.md`.

| Case | What it checks |
|---|---|
| `m1` | Each documented HTTP error maps to a type, an origin and a recovery |
| `e1` | The two kinds of 429, the 400 spend limit and the fallback by class |
| `e2` | A 200 response that fails by its stop reason |
| `e3` | An empty reply: the integration or the model |
| `e4` | A parse failure: the integration or the model |
| `e5` | A tool name the model invented against a tool that raised |
| `e6` | The first failure, its position and whether it recovered |
| `e7` | A dropped connection |

## Traps

1. **Diagnosing the last symptom.** A parse error after a truncated reply is not the cause. Start at the first failing event.
2. **Assuming the model when the output is odd.** Check your own message structure and parser first: they are cheaper to test and they are yours to fix.
3. **Treating a tool-reported error as a failure of your code.** A tool that returns an error result is the loop working as designed. Look for an exception instead.
4. **Fixing and forgetting.** A bug that is fixed without a test or an eval case will return. Keep the trace as a case.


## Quiz

1. In a trace, the final user entry carried a tool's output and then a line of commentary. The reply came back as `end_turn` with an empty content list. Which diagnosis fits?
   - **a**: The model chose to stop, so add a user message asking it to continue
   - **b**: The request layout is at fault, so return the result unaccompanied
   - **c**: The account reached a limit, so raise the quota before anything else
   - **d**: The service dropped the output, so retry the same call unchanged

2. Our code could not read this reply: "Sure! Here is the answer: {"label": "spam"} Hope it helps." What does the routine conclude?
   - **a**: The account lacks access to structured output, so request it
   - **b**: The model failed to produce JSON, so validate and ask it again
   - **c**: The service truncated the body, so retry the call with back-off
   - **d**: The parser was too strict, so pull out the object first

3. A trace holds a 529, then a retry that returns a good response, then nothing else. What is the finding?
   - **a**: No failure at all, since the final response was good
   - **b**: A service failure that the application survived
   - **c**: A failure of the model, with no recovery afterwards
   - **d**: An integration failure that needs a fix in code

<details>
<summary>Answer key</summary>

1. **b**. The table says that when text follows the tool result the origin is "The integration: send the tool result alone", and the routine's step 4 says "An `end_turn` with no content blocks is an empty reply. Look at the request before it." *a* is ruled out because that is the answer only when no text followed: the table gives "The model: add a new user message that asks it to continue" for "If no". *d* is ruled out because "A dropped connection has none: it is the network, and it belongs to the service side", while here a response arrived with a stop reason. *c* is ruled out because a limit arrives as an error: "An `error` event has one, and the table of page 1 gives its origin and recovery".
2. **d**. The table says that an object in the text makes it "The integration: the parser was too strict, so extract the object first". *b* is ruled out because "The model: it did not produce JSON, so validate and ask again" applies only when no object is in the text, and here one is. *c* is ruled out because a cut-off arrives as a stop reason: "A refusal, a `max_tokens` stop, a context-window stop or a pause is a failure of a successful response". *a* is ruled out because an access failure arrives as an error status: "An `error` event has one, and the table of page 1 gives its origin and recovery".
3. **b**. The page says "A 529 is retried and succeeds, so the application recovered", and "A later good response means the failure was survived". *a* is ruled out because the diagnosis names a failure even when it recovered: "Read from the top. The diagnosis names the first failing event". *c* is ruled out because the first failing event is a 529, which is not model output, and the page says "A 529 is retried and succeeds, so the application recovered". *d* is ruled out because "a bug in your request fails the same way each time", while this request succeeded on the retry.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A call returns a 429 that carries no `retry-after` header, and its error code says the organisation reached its monthly spend ceiling. What does the module advise?
   - **a**: Back off with jitter, since capacity frees up within seconds
   - **b**: Hand it to a person, since only a higher limit or the reset restores access
   - **c**: Split the work into smaller calls so that each one fits beneath the ceiling
   - **d**: Move the call to a fallback model, since this one is overloaded

2. After switching the model id, every call returns a 400 whose message says that forcing a call is unsupported. The incident notes list four suspects. Which should the team investigate first?
   - **a**: The `tool_choice` setting that the new release rejects
   - **b**: The provider's capacity in the region where the traffic is sent
   - **c**: The wording of the system message, which may have drifted
   - **d**: The key's permissions on the organisation's workspace

3. A reply ends cleanly with stop reason `end_turn`, but the downstream code throws on it, and the logged output is a sentence followed by a fenced block of JSON. Which statement is true?
   - **a**: The fault is likely the integration's parser, and a test should script this sample
   - **b**: The fault is the service's, so the call should be repeated until it is clean
   - **c**: The fault is the account's, so the output format needs a higher plan
   - **d**: The fault is the model's, since it ignored the instruction to return only JSON

4. A bug in a tool-use loop is fixed in production on a Friday evening. Which follow-up does the module recommend?
   - **a**: Close the incident, since the fix was deployed
   - **b**: Add a note to the runbook and rely on the next on-call
   - **c**: Scrub the trace and add it to the eval set as a case
   - **d**: Lower the retry budget so that the failure is less visible

<details>
<summary>Answer key</summary>

1. **b**. The table sends "Your credential or account (401, 402, 403, a spend cap)" to a person, because "a spend cap lifts only at the monthly reset or with a higher limit", and "No retry can supply a key or a payment". *a* is ruled out because capacity failures are the ones where you "Wait for the header, or back off with jitter, then retry", and this response has no such header. *c* is ruled out because the module's rule is to "retry only what can change by itself", and smaller calls do not change an organisation's ceiling. *d* is ruled out because a fallback model is the recovery for a refusal, where the table says "Use a fallback model, or reset the context", and this is not an overload.
2. **a**. The migration table lists `tool_choice` of `any` or a named tool as returning a 400 on Claude Opus 5.5 and Sonnet 5.5, with the recovery "change the request". *b* is ruled out because a 400 is a request status and capacity failures are "429 with `retry-after`, 529". *c* is ruled out because "None is cured by a retry, a longer timeout or a different prompt". *d* is ruled out because permissions are the 403 row, "Your credential or account (401, 402, 403, a spend cap)", and a 400 names a rejected request.
3. **a**. The table says an object between the first `{` and the last `}` makes it "The integration: the parser was too strict, so extract the object first", and the page says to prove a fix "with a test that scripts the trace". *d* is ruled out because the trap warns "Assuming the model when the output is odd", and the output does hold the JSON. *b* is ruled out because the module's rule is to "retry only what can change by itself", and a repeat gives the same shape. *c* is ruled out because the account row is "Your credential or account (401, 402, 403, a spend cap)", which arrives as an error status, and this call returned text.
4. **c**. The page says "A trace that went wrong in production is the best test case you will ever get: scrub it, give it an id and a tag, and add it to the eval set or the test suite." *a* is ruled out because "A bug that is fixed without a test or an eval case will return", as the last trap puts it. *b* is ruled out because a runbook note records the bug and does not test for it, and the page says "The bug cannot return unseen" once it is a case. *d* is ruled out because a smaller budget hides failures, while the routine says to "note whether the application recovered".

</details>
