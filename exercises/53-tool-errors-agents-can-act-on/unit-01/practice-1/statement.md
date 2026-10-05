# Practice: tool errors that an agent can act on

A tool that fails has to tell the agent three things: what kind of failure it was, whether trying again can help, and what to do next. This practice has you
write the part of a tool loop that does this: the structured error, the retry logic that is allowed to retry only what is safe to retry, the honest
answer to a call whose outcome nobody knows, and the choice of the next action. The tools are functions you are given (the tests script what they do), so the
practice grades the part that is yours. It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the file there.
The contract (the six categories, the result shapes, the retry limits and the wording of the text sent to the model) is this course's own design, built on
the documented `is_error` flag and the guide's categories; the documentation fixes the flag and says that an error message should say what went wrong and
what to try next, and it leaves the rest to you.

Names are Python's (`make_error`, `to_tool_result`, `run_tool`, `next_action`, `ToolError`); TypeScript has `makeError`, `toToolResult`, `runTool`,
`nextAction` and the class `ToolError`; Java has the same camel-case names as static methods of `Errors` with `Errors.ToolError`; Kotlin has top-level
functions and the class `ToolError`. The class `ToolError` is given in every starter: a tool raises it with a `kind` (`transient`, `validation`,
`permission`, `business` or `timeout`), a message, and optionally `retry_after_ms` and an `explanation` for the customer. Results are maps, as the examples
show. Waits are whole milliseconds.

## What is already written, and what you write

The starter is a working tool-error helper with nine gaps cut out of it. Everything that is plumbing is written and correct: the tool error class, the constants, the tool-result block, the retry loop with its bound, the arguments of every attempt and the message of an error that gives up. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The structured error (unlocks `m1`): a failed call becomes a map with `is_error` true, the `category`, the `retryable` flag of its category from KINDS, the trimmed `message` and the `attempts`.
2. The refusals (unlocks `e1`): an unknown category and a generic message (`Operation failed`, `error`, an empty text, with or without a final period) are refused with an error.
3. Only transient failures are retried (unlocks `e2`): a failure of any other kind (validation, permission, business) returns at once as a structured error with the tool's message and explanation.
4. The wait between attempts (unlocks `e2`, `e3`): the wait the service asked for (`retry_after_ms`) is honoured; otherwise the wait doubles from `base`: base, 2 x base, 4 x base.
5. The empty result (unlocks `e4`): none, an empty text, an empty list and an empty map count as a valid empty result; zero and false do not.
6. The timeout (unlocks `e5`): a timeout on a call that is not safe to repeat (not read-only, no idempotency key) returns `outcome_unknown` with a message that says to check the state; otherwise it counts as a transient failure.
7. The idempotency key (unlocks `e5`): when the policy has an idempotency key, every attempt carries it in a copy of the arguments; the caller's own arguments are never changed.
8. The next action (unlocks `e6`): an error maps to the action of its category in ACTIONS; a result that is not an error is `accept_empty` when it is empty and `continue` otherwise.
9. The unexpected exception (unlocks `e7`): an exception that is not a tool error becomes an `internal` structured error that says the tool failed unexpectedly, and the run goes on.

`m1` needs gap 1. About ten lines in all. The steps below describe the whole helper, so you can see how your gaps are used.

## Build it in four steps

### Step 1: `make_error(kind, message, explanation=None, attempts=1, attempted=None)` and `to_tool_result(tool_use_id, result)`

- `make_error` returns `{"is_error": true, "category": kind, "retryable", "message": the stripped message, "attempts"}`, and adds `explanation` when one is given
  and `attempted` (the arguments of the call) when given. The categories are `transient` (retryable), `validation`, `permission`, `business`,
  `outcome_unknown` and `internal` (none of them retryable). An unknown category, or a message that is blank or only says that something failed (`Operation
  failed`, `Error`, `Failed.`, `Something went wrong`, compared ignoring case and final full stops), is refused: raise `ValueError` (TypeScript: throw an
  `Error`; Java and Kotlin: `IllegalArgumentException`).
- `to_tool_result` returns the block the API wants: `{"type": "tool_result", "tool_use_id", "content", "is_error"}`. For an error the content is
  `<category> error (retryable: yes|no): <message>`, followed by ` Tell the customer: <explanation>` when there is one, and `is_error` is true. For a
  success (`{"ok": true, "content": text}`) the content is the text and `is_error` is false.

**Why the exam cares.** The Architect exam asks for structured error metadata (a category, a retryable flag, a readable description) and rejects the uniform
"Operation failed", because with it the agent cannot choose between retrying, fixing the input, escalating and apologising. **What you should see when it
works:** the error map and the tool-result text carry the category and the flag, the business error carries the sentence for the customer, and a generic
message is refused before it reaches the model.

### Step 2: `run_tool(tool, args, policy, sleep)`

`tool(args)` returns a value or raises. `policy` holds `max_retries` (default 2), `base_delay_ms` (default 100), `read_only` (default false) and
`idempotency_key` (optional). `sleep(ms)` is how you wait: call it, never sleep for real.

- Call the tool with a copy of the arguments; when there is a key, the copy also holds `idempotency_key`, on every attempt, and the caller's own map is never
  changed. A value comes back as `{"ok": true, "content": value, "empty": whether it is None, an empty string, list or map, "attempts"}`: an empty result is a
  success.
- A `transient` failure is retried up to `max_retries` times. Before retry number n the wait is `retry_after_ms` when the error carries one, and
  `base_delay_ms * 2^(n-1)` otherwise. When the retries are used up, return a `transient` error whose message is the tool's message followed by
  ` Gave up after N attempts.`, with the attempts and the arguments.
- `validation`, `permission` and `business` failures are returned at once as errors of that category with the tool's message, the explanation if any, the number
  of attempts (1) and the arguments in `attempted`. They are never retried.
- A `timeout` means no answer arrived, so nobody knows whether the call took effect. When the call is `read_only` or has an idempotency key, treat it as
  `transient`. Otherwise do not call again: return `outcome_unknown` with the tool's message followed by ` The call may have taken effect: check the current
  state before trying again.`
- Any other exception is an `internal` error with the message `unexpected failure in the tool: <text>`; it is not retried and does not escape.

**Why the exam cares.** The guide distinguishes errors that are worth retrying from those that are not, and asks for local recovery inside the tool or
subagent before anything is propagated. The step the exam leaves to you is the hard one: a write that timed out. **What you should see when it works:** a
busy service is called three times with waits of 100 and 200; a validation error is called once; a timed out refund with no key is called once and says to check
first, and with a key it is called again with the same key.

### Step 3: `next_action(result)`

Return `accept_empty` for a successful empty result and `continue` for any other success. For an error: `retry_later` for `transient`, `repair_input` for
`validation`, `escalate` for `permission` and `internal`, `explain` for `business`, `verify_first` for `outcome_unknown`.

**Why the exam cares.** The category is only useful if the loop does something different for each. **What you should see when it works:** an empty list of
orders is accepted, a permission failure is never answered with another try, and a business rule is explained to the customer instead of being retried.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A failed call becomes an error with a category, a retry flag and a message, and an error result with the error flag |
| `e1` | A generic message and an unknown category are refused |
| `e2` | Only transient failures are retried, with doubling waits; the other kinds return at once, with their message and explanation |
| `e3` | The retries are bounded, the wait a service asks for is honoured and the give-up message names the attempts |
| `e4` | A valid empty result is a success, not an error |
| `e5` | A timed out write is an unknown outcome and is retried only with a key or when read only, with the same key each time and the caller's map untouched |
| `e6` | The next action follows the category |
| `e7` | An unexpected exception becomes an internal error and the run goes on |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
