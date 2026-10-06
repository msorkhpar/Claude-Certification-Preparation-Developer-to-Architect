# The error result, and what the loop does with each kind

**Level:** Architect · **Module 53:** Tool errors that agents can act on · **Page 1 of 2**
**Exams:** A2.2; S1

**After this page you can** say what an error reply from a tool has to carry, tell a protocol error from a tool-execution error, name the categories the exam uses (transient, validation, permission, business) and the action the loop takes for each, keep a valid empty result apart from a failure, and explain why a uniform "Operation failed" leaves the agent unable to recover.

Checked on 2026-10-03 against the documentation pages "Handle tool calls" (the `is_error` flag) and "Define tools" of the Claude API, the Model Context Protocol specification (version 2026-07-28, the Tools page, section "Error Handling"), the Agent SDK page "Custom tools" (section "Handle errors"), Anthropic's engineering article "Writing tools for agents", and the exam guide for the Architect Foundations exam (version 1.0, July 2026): Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287, the versions the course ran. The example is plain code with scripted tools: no model was called, no network was used and no API key was involved. This page deepens module 26 (errors are results, `is_error`), module 32 (the two kinds of failure in an MCP server) and module 48 (the refusal a gate returns), and it does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* tools report failure with the MCP `isError` flag; the useful reply carries structured metadata, which the guide's examples name `errorCategory` (transient, validation or permission), `isRetryable` and a human-readable description; a business-rule violation comes back with `retriable: false` and a customer-friendly explanation; a generic "Operation failed" is the wrong answer because it hides what the agent needs; and an access failure, which calls for a retry decision, is told apart from a valid empty result, which is a successful query that found nothing. *What the current product and protocol say (checked 2026-10-03):* the protocol defines the flag and two kinds of error and leaves the content to the server: the Tools page has "Protocol Errors" for a request that cannot be fixed by the model and "Tool Execution Errors" that carry `isError: true`, and it names no field called `errorCategory` or `isRetryable`; the API page defines `is_error` on the `tool_result` block and asks for messages that say "what went wrong and what Claude should try next". The guide's field names are therefore its examples (the guide itself writes `isRetryable` in one place and `retriable` in another), and the design is yours: what counts is that the category, the retry advice and a message that can be acted on reach the model in the result. On the exam, choose the structured reply with a category and a retry flag over the generic message, the empty-but-successful result over an error for "no matches", and a reason for the customer for a business rule; in your own code, put the fields in the result text or in structured content, and keep the names consistent.

## Why it matters

A refund agent calls `process_refund`. The billing service is busy and the tool returns "Operation failed". What should the agent do? Retry? Ask the customer for different details? Hand the case to a person? Nothing in the reply says. A model that has to guess will retry a request that can never succeed, apologise for a failure that a second attempt would have fixed, or tell a customer that the system is broken when the refund is simply above a limit. The exam tests the design of the error reply because it is the cheapest place to make an agent more reliable: the failure is already known to the tool, and the reply can say what to do about it.

## The idea

### Two kinds of failure, and the flag

The protocol separates two situations, which module 32 introduced from the server's side. A **protocol error** "indicate issues with the request structure itself that models are less likely to be able to fix": an unknown tool, a malformed request, a server error. A **tool execution error** carries "actionable feedback that language models can use to self-correct and retry with adjusted parameters": an API failure, an input validation failure, a business logic failure. The second kind is an ordinary result with the flag `isError: true`, and the specification says what a client does with it: "Clients SHOULD provide tool execution errors to language models to enable self-correction." On the Claude API the same idea is the `is_error` field of a `tool_result` block: "Set `is_error` to `true` if the tool execution resulted in an error." The documentation adds what the model then does: "Claude will then incorporate this error into its response to the user."

Three calls show where the line falls. A request that names a tool the server does not have is a protocol error, since there is nothing to run. A request whose date is "next Friday" where a calendar date is required reaches the tool, which rejects the value: an input validation failure, so a tool execution error with the flag, and the message can say which format to use. A request that reaches a backend that answers with a 503 is also a tool execution error. The rule of thumb is that the model can repair or retry the second and the third, and cannot do anything about the first.

Everything on this page is about the second kind, because it is the one the model reads. Setting the flag is the minimum. The reply also has to say something.

### What the reply has to carry

The documentation gives the rule for the message in one sentence: "Write instructive error messages. Instead of generic errors like `"failed"`, include what went wrong and what Claude should try next (for example, `"Rate limit exceeded. Retry after 60 seconds."`). This gives Claude the context it needs to recover or adapt without guessing." The engineering article on writing tools asks for the same: clear communication of "specific and actionable improvements, rather than opaque error codes or tracebacks".

Four pieces of information make a reply that an agent can act on:

| Piece | Why the agent needs it | Example |
|---|---|---|
| The category | It decides which of several recoveries applies | `transient`, `validation`, `permission`, `business` |
| Whether trying again can help | It stops a wasted retry, and it licenses a useful one | retryable: no |
| What went wrong, in terms of the call | A validation failure is repaired by changing one value | "amount must be a positive whole number, for example 40" |
| For a business rule, the words for the customer | The agent can pass on the reason without inventing one | "A colleague will contact you about this refund." |

The course's practice puts the first two and the message into the text of the result (`transient error (retryable: yes): The billing service timed out after 5 s.`), because text is what every client passes to the model, and it keeps the same facts in a structured map next to it for the code. If your server uses structured content, the same fields belong there; the specification asks a server that does so to also return the serialized value as text.

### The categories, and the action each one calls for

The guide names the categories that matter; each one has a different right action, and that is the reason to have categories at all.

| Category | What happened | Retry? | What the loop does |
|---|---|---|---|
| `transient` | A timeout, an unavailable service, a rate limit: the same call may work soon | Yes, with a bound and a wait | Wait and call again; when the retries are used up, say so and try another route or escalate |
| `validation` | The input was wrong: a missing field, a value out of range, a bad format | No, not unchanged | Repair the input from the message and call again with a different value |
| `permission` | The caller may not do this: a key without the right, an identity that does not match | No | Do not retry; escalate or tell the customer that it cannot be done here |
| `business` | The request is understood and refused by a rule: over a limit, outside a window | No | Explain it with the words provided; escalate if the rule allows an exception through a person |

Two more outcomes belong in the course's practice and are not categories of the guide's list: `outcome_unknown` for a call that timed out and may have taken effect (page 2), and `internal` for a bug in the tool itself, which is not retried and goes to a person.

A permission failure and a validation failure look alike to a model that sees only "Error 403" and "Error 400". Both are "not retryable", but the agent repairs one and escalates the other, and a loop that retries a permission failure three times has asked a person's question of a machine three times.

### A valid empty result is not an error

The guide separates an access failure, which calls for a decision about retrying, from a valid empty result, which is a successful query that found no matches. A lookup that finds no orders for a customer has worked. The right reply is a success whose content says that there are none ("No orders found for customer C-9 in the last 90 days"), and the agent can then tell the customer so or ask for another identifier. An error flag on that reply makes the agent think that the system failed, and it will retry, apologise or escalate a case that is simply empty. The reverse mistake is the one the guide warns about for subagents: catching a timeout and returning an empty result marked as successful hides the failure, and the coordinator reports "no results" for a topic that was never searched. A lookup by id and a search differ here. Module 26's tool returns an error result for a city it does not know, because the call named one thing that does not exist, and the agent can fix its input. A search that matches nothing was a valid question with an empty answer, which is a success. The test is whether the call could have succeeded with other input (an error the agent can act on) or has simply found nothing.

The two replies look alike in a log and mean opposite things, so the tool must know which one it is giving.

### Where the error is composed

Who writes the message depends on where the tool runs, and the Agent SDK page says it plainly. In an in-process custom tool, "A handler error doesn't stop the agent loop": an uncaught exception is converted into an error result "carrying the raw exception message", and a handler that catches the error and returns the flag composes its own message: "Claude sees the message you compose. You can add context the raw exception lacks, such as which request failed or what to try instead." The page's conclusion is the design rule of this module: "Catch errors yourself when the raw exception message isn't enough for Claude to act on." A raw `KeyError: 'currency'` is not enough. In a Python MCP server (module 32) the dedicated exception type for an expected failure plays the same part.

### The example

The example runs seven scripted tool calls through a small loop and prints, for each, how many times the tool was called, what the loop waited, what the model would be given and what the loop does next. They are: a busy order service that recovers on the third try; a validation failure; a business rule with a sentence for the customer; a refund that timed out with no key; the same refund with a key; an empty list of orders; and a bug in the tool.

<!-- example: m53-tool-error-flow tabs: python,typescript,java,kotlin -->
```python
"""What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.

The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
answer. The refund service, the orders and the limits are illustrative.
"""
import logging

log = logging.getLogger(__name__)


class ToolError(Exception):
    def __init__(self, kind, message, retry_after_ms=None, explanation=None):
        super().__init__(message)
        self.kind, self.retry_after_ms, self.explanation = kind, retry_after_ms, explanation


RETRYABLE = {"transient": "yes", "validation": "no", "permission": "no", "business": "no", "outcome_unknown": "no", "internal": "no"}
ACTION = {"transient": "retry later", "validation": "repair the input", "permission": "escalate", "business": "explain to the customer", "outcome_unknown": "check the state first", "internal": "escalate"}


def failure(kind, message, attempts, explanation=None):
    text = f"{kind} error (retryable: {RETRYABLE[kind]}): {message}"
    if explanation:
        text += f" Tell the customer: {explanation}"
    return {"is_error": True, "kind": kind, "content": text, "attempts": attempts}


def run(tool, args, key=None, read_only=False, max_retries=2, base_ms=100):
    """Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits."""
    waits, attempts = [], 0
    while True:
        attempts += 1
        call = dict(args, idempotency_key=key) if key else dict(args)
        try:
            value = tool(call)
            return {"is_error": False, "content": value, "attempts": attempts, "empty": value in ("", [], None)}, waits
        except ToolError as error:
            kind = error.kind
            if kind == "timeout":
                if not (key or read_only):
                    return failure("outcome_unknown", f"{error} The call may have taken effect: check the current state before trying again.", attempts), waits
                kind = "transient"
            if kind != "transient":
                return failure(kind, str(error), attempts, error.explanation), waits
            if attempts > max_retries:
                return failure("transient", f"{error} Gave up after {attempts} attempts.", attempts), waits
            waits.append(error.retry_after_ms if error.retry_after_ms is not None else base_ms * 2 ** (attempts - 1))
        except Exception as error:
            return failure("internal", f"unexpected failure in the tool: {error}", attempts), waits


def next_step(result):
    if not result["is_error"]:
        return "accept the empty result" if result["empty"] else "continue"
    return ACTION[result["kind"]]


def scripted(*steps):
    """A tool that does what the script says, one entry per call; the last entry repeats."""
    state = {"n": 0, "seen": []}

    def tool(args):
        state["seen"].append(args.get("idempotency_key"))
        step = steps[min(state["n"], len(steps) - 1)]
        state["n"] += 1
        if isinstance(step, Exception):
            raise step
        return step

    tool.state = state
    return tool


def scenarios():
    return [
        ("get_order order=A-7", scripted(ToolError("transient", "The order service is busy."), ToolError("transient", "The order service is busy."), "order A-7: 2 items"), {}),
        ("process_refund amount=-5", scripted(ToolError("validation", "amount must be a positive whole number, for example 40")), {}),
        ("process_refund amount=900", scripted(ToolError("business", "Refunds above 500 need a person.", explanation="A colleague will contact you about this refund.")), {}),
        ("process_refund amount=40, no key", scripted(ToolError("timeout", "No answer from the refund service.")), {}),
        ("process_refund amount=40, key refund-A-7-1", scripted(ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), {"key": "refund-A-7-1"}),
        ("list_orders customer=C-9", scripted([]), {"read_only": True}),
        ("process_refund amount=40, tool bug", scripted(RuntimeError("the currency table is missing")), {}),
    ]


def main():
    for number, (title, tool, options) in enumerate(scenarios(), start=1):
        result, waits = run(tool, {"order": "A-7"}, **options)
        print(f"{number}. {title}")
        print(f"   attempts {result['attempts']}, waits {waits}, keys sent {tool.state['seen']}")
        print(f"   tool_result is_error={'true' if result['is_error'] else 'false'}: {result['content']!r}")
        print(f"   next: {next_step(result)}")


if __name__ == "__main__":
    main()
```
```text
1. get_order order=A-7
   attempts 3, waits [100, 200], keys sent [None, None, None]
   tool_result is_error=false: 'order A-7: 2 items'
   next: continue
2. process_refund amount=-5
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'validation error (retryable: no): amount must be a positive whole number, for example 40'
   next: repair the input
3. process_refund amount=900
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.'
   next: explain to the customer
4. process_refund amount=40, no key
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'outcome_unknown error (retryable: no): No answer from the refund service. The call may have taken effect: check the current state before trying again.'
   next: check the state first
5. process_refund amount=40, key refund-A-7-1
   attempts 2, waits [100], keys sent ['refund-A-7-1', 'refund-A-7-1']
   tool_result is_error=false: 'refund R-1 created'
   next: continue
6. list_orders customer=C-9
   attempts 1, waits [], keys sent [None]
   tool_result is_error=false: []
   next: accept the empty result
7. process_refund amount=40, tool bug
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'internal error (retryable: no): unexpected failure in the tool: the currency table is missing'
   next: escalate
```
```typescript
// What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.
//
// The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
// answer. The refund service, the orders and the limits are illustrative.
import { logger } from "./logger.ts";
const log = logger("error_flow");

export class ToolError extends Error {
  kind: string;
  retryAfterMs: number | null;
  explanation: string | null;
  constructor(kind: string, message: string, retryAfterMs: number | null = null, explanation: string | null = null) {
    super(message);
    this.kind = kind;
    this.retryAfterMs = retryAfterMs;
    this.explanation = explanation;
  }
}

const RETRYABLE: Record<string, string> = { transient: "yes", validation: "no", permission: "no", business: "no", outcome_unknown: "no", internal: "no" };
const ACTION: Record<string, string> = { transient: "retry later", validation: "repair the input", permission: "escalate", business: "explain to the customer", outcome_unknown: "check the state first", internal: "escalate" };

type Result = { is_error: boolean; kind?: string; content: any; attempts: number; empty?: boolean };
type Tool = ((args: Record<string, any>) => any) & { state: { n: number; seen: Array<string | null> } };

function failure(kind: string, message: string, attempts: number, explanation: string | null = null): Result {
  let text = `${kind} error (retryable: ${RETRYABLE[kind]}): ${message}`;
  if (explanation) text += ` Tell the customer: ${explanation}`;
  return { is_error: true, kind, content: text, attempts };
}

/** Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits. */
export function run(tool: Tool, args: Record<string, any>, options: { key?: string; read_only?: boolean } = {}): [Result, number[]] {
  const waits: number[] = [];
  const maxRetries = 2;
  const baseMs = 100;
  let attempts = 0;
  while (true) {
    attempts++;
    const call = options.key ? { ...args, idempotency_key: options.key } : { ...args };
    try {
      const value = tool(call);
      return [{ is_error: false, content: value, attempts, empty: value === "" || value === null || (Array.isArray(value) && value.length === 0) }, waits];
    } catch (error: any) {
      if (!(error instanceof ToolError)) return [failure("internal", `unexpected failure in the tool: ${error.message}`, attempts), waits];
      let kind = error.kind;
      if (kind === "timeout") {
        if (!(options.key || options.read_only)) return [failure("outcome_unknown", `${error.message} The call may have taken effect: check the current state before trying again.`, attempts), waits];
        kind = "transient";
      }
      if (kind !== "transient") return [failure(kind, error.message, attempts, error.explanation), waits];
      if (attempts > maxRetries) return [failure("transient", `${error.message} Gave up after ${attempts} attempts.`, attempts), waits];
      waits.push(error.retryAfterMs !== null ? error.retryAfterMs : baseMs * 2 ** (attempts - 1));
    }
  }
}

export function nextStep(result: Result): string {
  if (!result.is_error) return result.empty ? "accept the empty result" : "continue";
  return ACTION[result.kind as string];
}

/** A tool that does what the script says, one entry per call; the last entry repeats. */
export function scripted(...steps: any[]): Tool {
  const state = { n: 0, seen: [] as Array<string | null> };
  const tool = ((args: Record<string, any>) => {
    state.seen.push(args.idempotency_key ?? null);
    const step = steps[Math.min(state.n, steps.length - 1)];
    state.n++;
    if (step instanceof Error) throw step;
    return step;
  }) as Tool;
  tool.state = state;
  return tool;
}

export function scenarios(): Array<[string, Tool, { key?: string; read_only?: boolean }]> {
  return [
    ["get_order order=A-7", scripted(new ToolError("transient", "The order service is busy."), new ToolError("transient", "The order service is busy."), "order A-7: 2 items"), {}],
    ["process_refund amount=-5", scripted(new ToolError("validation", "amount must be a positive whole number, for example 40")), {}],
    ["process_refund amount=900", scripted(new ToolError("business", "Refunds above 500 need a person.", null, "A colleague will contact you about this refund.")), {}],
    ["process_refund amount=40, no key", scripted(new ToolError("timeout", "No answer from the refund service.")), {}],
    ["process_refund amount=40, key refund-A-7-1", scripted(new ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), { key: "refund-A-7-1" }],
    ["list_orders customer=C-9", scripted([]), { read_only: true }],
    ["process_refund amount=40, tool bug", scripted(new Error("the currency table is missing")), {}],
  ];
}

function main() {
  scenarios().forEach(([title, tool, options], index) => {
    const [result, waits] = run(tool, { order: "A-7" }, options);
    console.log(`${index + 1}. ${title}`);
    console.log(`   attempts ${result.attempts}, waits [${waits.join(", ")}], keys sent [${tool.state.seen.map((k) => (k === null ? "None" : `'${k}'`)).join(", ")}]`);
    console.log(`   tool_result is_error=${result.is_error ? "true" : "false"}: ${pyRepr(result.content)}`);
    console.log(`   next: ${nextStep(result)}`);
  });
}

/** The text in single quotes as Python prints it; content here is always a string or an empty list. */
function pyRepr(value: any): string {
  if (Array.isArray(value)) return "[]";
  return `'${String(value).replaceAll("\\", "\\\\").replaceAll("'", "\\'")}'`;
}

if (import.meta.main) main();
```
```text
1. get_order order=A-7
   attempts 3, waits [100, 200], keys sent [None, None, None]
   tool_result is_error=false: 'order A-7: 2 items'
   next: continue
2. process_refund amount=-5
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'validation error (retryable: no): amount must be a positive whole number, for example 40'
   next: repair the input
3. process_refund amount=900
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.'
   next: explain to the customer
4. process_refund amount=40, no key
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'outcome_unknown error (retryable: no): No answer from the refund service. The call may have taken effect: check the current state before trying again.'
   next: check the state first
5. process_refund amount=40, key refund-A-7-1
   attempts 2, waits [100], keys sent ['refund-A-7-1', 'refund-A-7-1']
   tool_result is_error=false: 'refund R-1 created'
   next: continue
6. list_orders customer=C-9
   attempts 1, waits [], keys sent [None]
   tool_result is_error=false: []
   next: accept the empty result
7. process_refund amount=40, tool bug
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'internal error (retryable: no): unexpected failure in the tool: the currency table is missing'
   next: escalate
```
```java
import static harness.Show.py;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.
 *
 * <p>The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
 * answer. The refund service, the orders and the limits are illustrative.
 */
public final class ErrorFlow {
    private static final System.Logger LOG = System.getLogger(ErrorFlow.class.getName());
    /** A failure a tool reports about itself: its kind, a message the model can use, an optional wait the service asked for and an explanation for the customer. */
    static final class ToolError extends RuntimeException {
        final String kind;
        final Integer retryAfterMs;
        final String explanation;

        ToolError(String kind, String message, Integer retryAfterMs, String explanation) {
            super(message);
            this.kind = kind;
            this.retryAfterMs = retryAfterMs;
            this.explanation = explanation;
        }

        ToolError(String kind, String message) {
            this(kind, message, null, null);
        }
    }

    static final Map<String, String> RETRYABLE = Map.of("transient", "yes", "validation", "no", "permission", "no", "business", "no", "outcome_unknown", "no", "internal", "no");
    static final Map<String, String> ACTION = Map.of("transient", "retry later", "validation", "repair the input", "permission", "escalate", "business", "explain to the customer",
        "outcome_unknown", "check the state first", "internal", "escalate");

    /** What the loop hands the model: an error flag, the kind of failure, the text, the attempts made, and whether a success was empty. */
    record Result(boolean isError, String kind, Object content, int attempts, boolean empty) {}

    /** The result of a run and the waits it made between attempts. */
    record Run(Result result, List<Integer> waits) {}

    /** A tool: it takes the call's arguments and returns a value or throws. */
    interface ToolFn {
        Object call(Map<String, Object> args) throws Exception;
    }

    /** What a run may be told: an idempotency key, and whether the call only reads. */
    record Options(String key, boolean readOnly) {
        static final Options NONE = new Options(null, false);
    }

    static Result failure(String kind, String message, int attempts, String explanation) {
        String text = kind + " error (retryable: " + RETRYABLE.get(kind) + "): " + message;
        if (explanation != null && !explanation.isEmpty()) text += " Tell the customer: " + explanation;
        return new Result(true, kind, text, attempts, false);
    }

    static boolean isEmpty(Object value) {
        return value == null || "".equals(value) || (value instanceof List<?> l && l.isEmpty());
    }

    /** Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits. */
    static Run run(ToolFn tool, Map<String, Object> args, Options options, int maxRetries, int baseMs) {
        List<Integer> waits = new ArrayList<>();
        int attempts = 0;
        while (true) {
            attempts++;
            Map<String, Object> call = new HashMap<>(args);
            if (options.key() != null) call.put("idempotency_key", options.key());
            try {
                Object value = tool.call(call);
                return new Run(new Result(false, null, value, attempts, isEmpty(value)), waits);
            } catch (ToolError error) {
                String kind = error.kind;
                if (kind.equals("timeout")) {
                    if (options.key() == null && !options.readOnly()) {
                        return new Run(failure("outcome_unknown", error.getMessage() + " The call may have taken effect: check the current state before trying again.", attempts, null), waits);
                    }
                    kind = "transient";
                }
                if (!kind.equals("transient")) return new Run(failure(kind, error.getMessage(), attempts, error.explanation), waits);
                if (attempts > maxRetries) return new Run(failure("transient", error.getMessage() + " Gave up after " + attempts + " attempts.", attempts, null), waits);
                waits.add(error.retryAfterMs != null ? error.retryAfterMs : baseMs * (1 << (attempts - 1)));
            } catch (Exception error) {
                return new Run(failure("internal", "unexpected failure in the tool: " + error.getMessage(), attempts, null), waits);
            }
        }
    }

    static Run run(ToolFn tool, Map<String, Object> args, Options options) {
        return run(tool, args, options, 2, 100);
    }

    static Run run(ToolFn tool, Map<String, Object> args) {
        return run(tool, args, Options.NONE);
    }

    static String nextStep(Result result) {
        if (!result.isError()) return result.empty() ? "accept the empty result" : "continue";
        return ACTION.get(result.kind());
    }

    /** A tool that does what the script says, one entry per call; the last entry repeats. */
    static final class Scripted implements ToolFn {
        final Object[] steps;
        int n = 0;
        final List<String> seen = new ArrayList<>();

        Scripted(Object... steps) {
            this.steps = steps;
        }

        @Override
        public Object call(Map<String, Object> args) throws Exception {
            seen.add((String) args.get("idempotency_key"));
            Object step = steps[Math.min(n, steps.length - 1)];
            n++;
            if (step instanceof Exception e) throw e;
            return step;
        }
    }

    record Scenario(String title, Scripted tool, Options options) {}

    static List<Scenario> scenarios() {
        return List.of(
            new Scenario("get_order order=A-7", new Scripted(new ToolError("transient", "The order service is busy."), new ToolError("transient", "The order service is busy."), "order A-7: 2 items"), Options.NONE),
            new Scenario("process_refund amount=-5", new Scripted(new ToolError("validation", "amount must be a positive whole number, for example 40")), Options.NONE),
            new Scenario("process_refund amount=900", new Scripted(new ToolError("business", "Refunds above 500 need a person.", null, "A colleague will contact you about this refund.")), Options.NONE),
            new Scenario("process_refund amount=40, no key", new Scripted(new ToolError("timeout", "No answer from the refund service.")), Options.NONE),
            new Scenario("process_refund amount=40, key refund-A-7-1", new Scripted(new ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), new Options("refund-A-7-1", false)),
            new Scenario("list_orders customer=C-9", new Scripted(List.of()), new Options(null, true)),
            new Scenario("process_refund amount=40, tool bug", new Scripted(new RuntimeException("the currency table is missing")), Options.NONE));
    }

    public static void main(String[] args) {
        int number = 0;
        for (Scenario s : scenarios()) {
            number++;
            Run r = run(s.tool(), Map.of("order", "A-7"), s.options());
            System.out.println(number + ". " + s.title());
            System.out.println("   attempts " + r.result().attempts() + ", waits " + py(r.waits()) + ", keys sent " + py(s.tool().seen));
            System.out.println("   tool_result is_error=" + (r.result().isError() ? "true" : "false") + ": " + py(r.result().content()));
            System.out.println("   next: " + nextStep(r.result()));
        }
    }
}
```
```text
1. get_order order=A-7
   attempts 3, waits [100, 200], keys sent [None, None, None]
   tool_result is_error=false: 'order A-7: 2 items'
   next: continue
2. process_refund amount=-5
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'validation error (retryable: no): amount must be a positive whole number, for example 40'
   next: repair the input
3. process_refund amount=900
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.'
   next: explain to the customer
4. process_refund amount=40, no key
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'outcome_unknown error (retryable: no): No answer from the refund service. The call may have taken effect: check the current state before trying again.'
   next: check the state first
5. process_refund amount=40, key refund-A-7-1
   attempts 2, waits [100], keys sent ['refund-A-7-1', 'refund-A-7-1']
   tool_result is_error=false: 'refund R-1 created'
   next: continue
6. list_orders customer=C-9
   attempts 1, waits [], keys sent [None]
   tool_result is_error=false: []
   next: accept the empty result
7. process_refund amount=40, tool bug
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'internal error (retryable: no): unexpected failure in the tool: the currency table is missing'
   next: escalate
```
```kotlin
import harness.Show.py

private val log = System.getLogger("error_flow")

/**
 * What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.
 *
 * The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
 * answer. The refund service, the orders and the limits are illustrative.
 */

/** A failure a tool reports about itself: its kind, a message the model can use, an optional wait the service asked for and an explanation for the customer. */
class ToolError(val kind: String, message: String, val retryAfterMs: Int? = null, val explanation: String? = null) : RuntimeException(message)

val RETRYABLE = mapOf("transient" to "yes", "validation" to "no", "permission" to "no", "business" to "no", "outcome_unknown" to "no", "internal" to "no")
val ACTION = mapOf(
    "transient" to "retry later", "validation" to "repair the input", "permission" to "escalate", "business" to "explain to the customer",
    "outcome_unknown" to "check the state first", "internal" to "escalate",
)

/** What the loop hands the model: an error flag, the kind of failure, the text, the attempts made, and whether a success was empty. */
data class Result(val isError: Boolean, val kind: String?, val content: Any?, val attempts: Int, val empty: Boolean)

/** What a run may be told: an idempotency key, and whether the call only reads. */
data class Options(val key: String? = null, val readOnly: Boolean = false)

fun failure(kind: String, message: String, attempts: Int, explanation: String? = null): Result {
    var text = "$kind error (retryable: ${RETRYABLE[kind]}): $message"
    if (!explanation.isNullOrEmpty()) text += " Tell the customer: $explanation"
    return Result(true, kind, text, attempts, false)
}

/** Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits. */
fun run(tool: (Map<String, Any?>) -> Any?, args: Map<String, Any?>, options: Options = Options(), maxRetries: Int = 2, baseMs: Int = 100): Pair<Result, List<Int>> {
    val waits = mutableListOf<Int>()
    var attempts = 0
    while (true) {
        attempts++
        val call = if (options.key != null) args + ("idempotency_key" to options.key) else args
        try {
            val value = tool(call)
            return Result(false, null, value, attempts, value == null || value == "" || (value is List<*> && value.isEmpty())) to waits
        } catch (error: ToolError) {
            var kind = error.kind
            if (kind == "timeout") {
                if (options.key == null && !options.readOnly) {
                    return failure("outcome_unknown", "${error.message} The call may have taken effect: check the current state before trying again.", attempts) to waits
                }
                kind = "transient"
            }
            if (kind != "transient") return failure(kind, error.message!!, attempts, error.explanation) to waits
            if (attempts > maxRetries) return failure("transient", "${error.message} Gave up after $attempts attempts.", attempts) to waits
            waits += error.retryAfterMs ?: (baseMs * (1 shl (attempts - 1)))
        } catch (error: Exception) {
            return failure("internal", "unexpected failure in the tool: ${error.message}", attempts) to waits
        }
    }
}

fun nextStep(result: Result): String = if (!result.isError) (if (result.empty) "accept the empty result" else "continue") else ACTION.getValue(result.kind!!)

/** A tool that does what the script says, one entry per call; the last entry repeats. */
class Scripted(private vararg val steps: Any?) : (Map<String, Any?>) -> Any? {
    var n = 0
    val seen = mutableListOf<String?>()

    override fun invoke(args: Map<String, Any?>): Any? {
        seen += args["idempotency_key"] as String?
        val step = steps[minOf(n, steps.size - 1)]
        n++
        if (step is Exception) throw step
        return step
    }
}

data class Scenario(val title: String, val tool: Scripted, val options: Options = Options())

fun scenarios() = listOf(
    Scenario("get_order order=A-7", Scripted(ToolError("transient", "The order service is busy."), ToolError("transient", "The order service is busy."), "order A-7: 2 items")),
    Scenario("process_refund amount=-5", Scripted(ToolError("validation", "amount must be a positive whole number, for example 40"))),
    Scenario("process_refund amount=900", Scripted(ToolError("business", "Refunds above 500 need a person.", explanation = "A colleague will contact you about this refund."))),
    Scenario("process_refund amount=40, no key", Scripted(ToolError("timeout", "No answer from the refund service."))),
    Scenario("process_refund amount=40, key refund-A-7-1", Scripted(ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), Options(key = "refund-A-7-1")),
    Scenario("list_orders customer=C-9", Scripted(emptyList<String>()), Options(readOnly = true)),
    Scenario("process_refund amount=40, tool bug", Scripted(RuntimeException("the currency table is missing"))),
)

fun main() {
    for ((index, s) in scenarios().withIndex()) {
        val (result, waits) = run(s.tool, mapOf("order" to "A-7"), s.options)
        println("${index + 1}. ${s.title}")
        println("   attempts ${result.attempts}, waits ${py(waits)}, keys sent ${py(s.tool.seen)}")
        println("   tool_result is_error=${if (result.isError) "true" else "false"}: ${py(result.content)}")
        println("   next: ${nextStep(result)}")
    }
}
```
```text
1. get_order order=A-7
   attempts 3, waits [100, 200], keys sent [None, None, None]
   tool_result is_error=false: 'order A-7: 2 items'
   next: continue
2. process_refund amount=-5
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'validation error (retryable: no): amount must be a positive whole number, for example 40'
   next: repair the input
3. process_refund amount=900
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'business error (retryable: no): Refunds above 500 need a person. Tell the customer: A colleague will contact you about this refund.'
   next: explain to the customer
4. process_refund amount=40, no key
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'outcome_unknown error (retryable: no): No answer from the refund service. The call may have taken effect: check the current state before trying again.'
   next: check the state first
5. process_refund amount=40, key refund-A-7-1
   attempts 2, waits [100], keys sent ['refund-A-7-1', 'refund-A-7-1']
   tool_result is_error=false: 'refund R-1 created'
   next: continue
6. list_orders customer=C-9
   attempts 1, waits [], keys sent [None]
   tool_result is_error=false: []
   next: accept the empty result
7. process_refund amount=40, tool bug
   attempts 1, waits [], keys sent [None]
   tool_result is_error=true: 'internal error (retryable: no): unexpected failure in the tool: the currency table is missing'
   next: escalate
```
<!-- /example -->

Read the output call by call. The first call was made three times with waits of 100 and 200, and the model sees only the result. The second was made once and came back as a validation error whose message says what to change. The third carries the sentence for the customer. The fourth, a timed out write with no key, was made once and the next step is to check the state, which page 2 explains. The fifth was repeated with the same key. The sixth is a success with nothing in it, and the loop accepts it. The seventh is a bug in the tool, reported as internal and passed to a person instead of ending the run.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Return the same short message for every failure, so that the agent has one thing to handle."** It is tempting because it is simple and it never leaks detail. The exam rejects it: with "Operation failed" the agent cannot choose between retrying, repairing the input, escalating and apologising. Return a category, a retry flag and a message that says what to do.
2. **"Retry every failed call a few times, whatever the reason."** It is tempting because most failures in a network are transient. The exam rejects it: a validation, permission or business failure gives the same answer every time, and the retries only delay the escalation. Only a transient failure is retried, with a bound.
3. **"Mark a search with no matches as an error, so that the agent notices."** It is tempting because "found nothing" feels like a failure. The exam rejects it: the query worked, and the error flag sends the agent into recovery for a case that needs none. Return a success that says there were no matches.
4. **"Catch the timeout and return an empty list, so that the run goes on."** It is tempting because nothing crashes. The exam rejects it: an empty list marked as a success tells the agent that the search found nothing, when it never completed. Return an error that names the failure.
5. **"Let the exception propagate; the model can read the stack trace."** It is tempting because it needs no code. The exam rejects it: the raw message is rarely enough to act on, and a business rule has no exception text at all. Catch the failure and compose the reply.

## Quiz

1. A refund tool meets a slow card service, and each time it answers with the text "Operation failed". The agent tries three more times and then apologises, though a colleague could have settled the case. Which change helps most?
   - **a**: Raise the attempts from three to ten, with a longer wait between them
   - **b**: Add a prompt rule on retries and a second rule on when to give up
   - **c**: Name in the reply the kind of problem and whether a retry is worthwhile
   - **d**: Pass the card service's raw exception text through to the model

2. A calendar service gets three requests: one asks for a function that it lacks, one gives a date as "next Friday" where a calendar date is needed, and one reaches a backend that answers 503. Which of them, if any, is a protocol error?
   - **a**: The one that has nowhere to be routed
   - **b**: The one whose date has the wrong format
   - **c**: The one that met the busy backend
   - **d**: None, as all three reached the server

<details>
<summary>Answer key</summary>

1. **c**. The agent can only choose a recovery if the reply says which one applies. *a* is ruled out because more attempts do not help when the reply gives no reason: "Retry every failed call a few times, whatever the reason". *b* is ruled out because a model with nothing to go on can only guess: "A model that has to guess will retry a request that can never succeed". *d* is ruled out because a raw exception says nothing about the next step: "the raw message is rarely enough to act on".
2. **a**. Only a request with nothing to run is a protocol error; the other two reached the tool. *b* is ruled out because "an input validation failure" is a tool execution error, which the model can repair. *c* is ruled out because a failed backend call is an "API failure" that comes back with the flag and a message that allows "retry with adjusted parameters". *d* is ruled out because reaching the server is not where the line falls: "A request that names a tool the server does not have is a protocol error".

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License). The first quiz question follows an item of that set on a tool that fails intermittently, rewritten here.
