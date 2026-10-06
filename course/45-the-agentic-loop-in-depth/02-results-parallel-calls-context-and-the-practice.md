# Results, parallel calls, growing context and the practice

**Level:** Architect · **Module 45:** The agentic loop, in depth · **Page 2 of 2**
**Exams:** A1.1 (A1)

**After this page you can** keep the conversation of an agent loop in the shape the API accepts, say what the Agent SDK does with several tool calls in one turn and what that means for the order of their effects, explain why a long run needs a plan for its context, read the cost of a run that used subagents correctly, and write the module's practice: a loop that is driven by the stop reason.

Checked on 2026-10-03 against the Claude Code documentation page "How the agent loop works" (message types, parallel tool execution, the context window, compaction and the result), and the Claude API pages "Handle tool calls" and "Stop reasons and fallback". The practice is offline in Python, TypeScript, Java and Kotlin: the model is a function that the tests script, and no model or network is used. The Agent SDK itself is Python and TypeScript only; the loop of the practice is the one you write when you call the Messages API yourself, which is the loop the exam shows.

## Why it matters

The stop reason ends the run. Everything the loop does between two replies decides whether the next call is a good one. The mistakes are in the details the exam turns into questions: a result in the wrong place, an order assumed where none is promised, an instruction given once at the start of a run that compaction later drops, a cost read from the field that covers only part of the work. An architect is expected to know where each detail is decided, and which of them the SDK has already taken off your hands.

## The idea

### What the loop owns when you write it yourself

Module 26 listed the formatting rules; this is the short version of what a hand-written loop must keep true, and the reason for each.

- **The assistant turn goes back unchanged.** Add the reply's content list as one assistant message. Text, tool calls and anything else in it stay as they came, because the next request is the whole conversation.
- **One user message answers the whole turn.** "Tool result blocks must immediately follow their corresponding tool use blocks in the message history", and "the tool_result blocks must come FIRST in the content array." A turn with three calls gets one user message with three results, matched by id. Splitting them is the mistake of module 26, page 3, and an empty message, which a loop can send by accident when a `tool_use` reply holds no call, is refused.
- **A failure is a result.** A tool that throws, or a name that does not exist, still gets a result with `is_error` set and a message that says what went wrong. The loop does not crash and does not invent a success.
- **The count is checked before the call.** The practice records the status `max_turns` and leaves the messages ending in the last tool results, so that the run can be resumed or read.

### What the Agent SDK does in that position

When you use the Agent SDK, the same loop runs inside the Claude Code binary, and you see it as messages: a system message that starts the session, an assistant message for each content block, a user message after each tool execution with the results, and a final result message. Three details of how it executes the tools matter for the architect.

**Order of effects.** "When Claude requests multiple tool calls in a single turn, both SDKs can run them concurrently or sequentially depending on the tool." The documentation draws the line by what a tool does: "Read-only tools (like Read, Glob, Grep, and MCP tools marked as read-only) can run concurrently. Tools that modify state (like Edit, Write, and Bash) run sequentially to avoid conflicts." Your own tools sit on the cautious side: "Custom tools default to sequential execution. To enable parallel execution for a custom tool, set readOnlyHint in its annotations." So a custom tool that only reads is slower than it needs to be until it says so, and a tool that writes must not be marked read-only to make a run faster. Do not write a tool that depends on another tool from the same turn having finished first unless you know it runs sequentially; the model decides what to ask for in one turn, and a dependency belongs in two turns.

**The context grows.** "The context window does not reset between turns within a session." Everything accumulates: the system prompt, the tool definitions, the history and every tool output, and a verbose output uses thousands of tokens in one turn. When the window nears its limit the SDK compacts: "Compaction replaces older messages with a summary, so specific instructions from early in the conversation may not be preserved." The consequence is a rule of placement. An instruction that must hold for the whole run belongs in a `CLAUDE.md` file loaded through the setting sources, which is re-injected on every request, and not only in the first prompt. Work that would flood the context with intermediate output belongs in a subagent (module 46), because "only its final response returns to the parent."

**Reading the result.** The result message is where the run ends, and its subtype is "the primary way to check termination state" (module 35). Two details complete the picture. After a session crash, "the final result is an `error_during_execution` whose cost fields may be zeroed and whose `stop_reason` is `null`", so a null stop reason is not an error in your code. A run that the loop itself ends, for example at the turn limit, is different: its result is `error_max_turns`, and "`stop_reason` carries the value from the last assistant response before the loop ended", so only the crash result has none. And the cost fields do not all cover the same work: "The `usage` field covers only the main agent loop. Use `modelUsage`, or `model_usage` in Python, for whole-tree token and cost accounting." A run that delegated to subagents has spent more than `usage` says.

### Where each control belongs

| You want | Put it in | Because |
|---|---|---|
| The run to end when the work is done | The model's stop reason | The model decides; your code reads it |
| Every call of a kind checked, for example refunds | A hook before the tool (module 49) | It runs on every call, and a prompt does not always |
| A run that cannot go on for ever | A turn limit and a budget, with their own statuses | A backstop catches what the model does not end |
| A rule that holds through compaction | `CLAUDE.md` through the setting sources | It is re-injected on every request |
| A noisy side task kept out of the main context | A subagent | Only its final report comes back |
| The whole cost of a run | `modelUsage` | `usage` covers the main loop only |

### The practice

The practice is `exercises/45-the-agentic-loop-in-depth/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `run_agent`, the loop of page 1 with every rule above: it ends only on the stop reason (`done`, `truncated`, `refused`, `unexpected`), answers every call of a turn in one user message in order with the assistant turn kept as it came, turns a failing or unknown tool into an error result, treats a `tool_use` reply with no call as `malformed`, and checks `max_turns` before each model call, so that an endless run ends with `max_turns` and a run that ends on its last allowed turn is still `done`.

The tests script the model and grade seven cases: the main loop, the stop reason against the words of the text, the one-message answer, tool errors, the turn limit, the other stop reasons and the malformed reply. The starter fails all seven, the reference passes them, and each of seven planted wrong solutions per language fails on an assertion of the case it breaks: a loop that stops on the word "done", one that reports `done` at the limit, one that allows an extra call, one that answers each call in its own message, one that sends an error as an ordinary result, one that treats a cut-off reply as finished and one that goes on after a `tool_use` reply with no call.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"The tools of one turn run in the order the model listed them."** It is tempting because the list is ordered. The exam rejects it: read-only tools may run concurrently, and your custom tools run one after another unless they are marked read-only. A dependency between two tools belongs in two turns.
2. **"Say the rule once, in the first prompt."** It is tempting because the prompt is read on every turn. The exam rejects it: compaction can drop it. Put rules that must hold for the whole run in `CLAUDE.md`, or enforce them in a hook.
3. **"The usage field tells what the run cost."** It is tempting because it is the field in the result. The exam rejects it: it covers the main loop only. Use `modelUsage` when subagents were used.
4. **"A null stop reason is a bug in the parser."** It is tempting because every other reply has one. The exam rejects it: the result of a session crash has none, and its cost fields may be zero.

## Quiz

3. A custom tool `deduct_stock` writes to inventory, and a custom tool `notify_warehouse` reads the new level. The model asks for both in one turn, and the developer marks only `notify_warehouse` as read-only to save time. What should the team conclude?
   - **a**: That the write finishes first, since state-changing tools always run ahead of any readers
   - **b**: That no order between them is promised, since the notifier is now free to run in parallel
   - **c**: That both run in the order requested, since every custom tool runs one after another here
   - **d**: That the reader waits, since the SDK works out the dependency from the tool names

4. A finance dashboard reads the `usage` field of every result to report what agent runs cost, but the invoices are higher for the runs that delegated to subagents. What explains the gap, and what is the fix?
   - **a**: The helpers bill on their own, so parse each helper's messages out of the stream
   - **b**: The figure is complete, so look for unlogged retries elsewhere in the application
   - **c**: It leaves out the helpers' work, so use the per-model figures for the whole tree
   - **d**: The figure is zeroed whenever a run is cut off, so leave those runs out


<details>
<summary>Answer key</summary>

3. **b**. Marking the reader read-only lets it run alongside the write, so it may read the old level, and the page's remedy is to split the calls: "a dependency belongs in two turns." *a* is ruled out because the documented rule only keeps state-changing tools from clashing with one another, and says nothing of holding readers back: "Tools that modify state (like Edit, Write, and Bash) run sequentially to avoid conflicts." *c* is ruled out because a tool marked read-only is allowed to run alongside others: "To enable parallel execution for a custom tool, set readOnlyHint in its annotations." *d* is ruled out because the SDK leaves the order to the tool's author, not to its name: "Do not write a tool that depends on another tool from the same turn having finished first unless you know it runs sequentially".
4. **c**. The `usage` figure counts the main loop only, so subagent work is missing, and the page names the field for the whole tree. *a* is ruled out because the SDK already provides that accounting: "Use `modelUsage`, or `model_usage` in Python, for whole-tree token and cost accounting." *b* is ruled out because the figure is incomplete by design: "A run that delegated to subagents has spent more than `usage` says." *d* is ruled out because zeroed figures belong to a crash and not to a cut-off: "the final result is an `error_during_execution` whose cost fields may be zeroed".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team's runs often end with the status `max_turns` because one tool keeps failing, and raising the cap from ten to forty only made each failed run cost four times as much. What should they do?
   - **a**: Turn the cap off, and let the model decide when a failure is final
   - **b**: Lower the cap back to ten, so that each failed run costs less again
   - **c**: Read the kept conversations, and fix whatever breaks those calls
   - **d**: Report such runs as done, so that the dashboard stops flagging them

2. A loop that follows this module's practice receives a reply whose stop reason asks for a tool, but its content holds only a sentence of text. What should it do?
   - **a**: Send an empty user message and call the model once more to see what it does
   - **b**: Finish with a malformed status and make no further call
   - **c**: Return the sentence to the user as the finished answer
   - **d**: Call the model again with the same messages to get the missing call

3. A dashboard alerts on every run that ends with a null stop reason and zero cost, and the team suspects a parsing bug. What explains those runs?
   - **a**: A turn limit, which can report a zero cost for the unfinished run of the agent
   - **b**: A refusal by the model, which ends the reply without a stop reason to report
   - **c**: A run that ended normally, whose last reply needed no stop reason at all
   - **d**: A session crash, whose closing error result may leave those fields blank

<details>
<summary>Answer key</summary>

1. **c**. The status is a signal about a run that does not converge, and the conversation shows why. *b* is ruled out because the cost is a symptom and the count is not what is wrong: "When the status `max_turns` shows up often, the count is not the thing to change." *a* is ruled out because a run needs a backstop: "A count is added so that a run that does not converge is stopped." *d* is ruled out because a run at the count must stay distinct: "A run that meets the count reports `max_turns`, and the caller can tell it from `done`."
2. **b**. There is nothing to answer, and the practice stops with its own status. *a* is ruled out because the API does not accept it: "an empty message, which a loop can send by accident when a `tool_use` reply holds no call, is refused." *c* is ruled out because narration is not an answer: "only the reply that ends the turn holds the answer." *d* is ruled out because calling again is one of the planted wrong solutions the tests reject: "one that goes on after a `tool_use` reply with no call".
3. **d**. The documentation describes this result for a crash: "the final result is an `error_during_execution` whose cost fields may be zeroed and whose `stop_reason` is `null`". *b* is ruled out because a refusal sets the reason: "Claude declined to respond." *c* is ruled out because a normal end carries a value: "Claude finished its response naturally." *a* is ruled out because a run that the loop ends has a value: "`stop_reason` carries the value from the last assistant response before the loop ended".

</details>
