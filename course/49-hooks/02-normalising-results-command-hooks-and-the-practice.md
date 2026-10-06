# Normalising results, command hooks and the practice

**Level:** Architect · **Module 49:** Hooks · **Page 2 of 2**
**Exams:** A1.5; A3

**After this page you can** normalise tool results with a PostToolUse hook so that the model reads one format whatever the tool returns, write a command hook for Claude Code and register it in a settings file, say which exit codes block and which silently do not, decide whether a rule belongs in a hook, in the prompt or in a model-judged hook, and write the module's practice: the refund gate, the normaliser, the options and the command hook.

Checked on 2026-10-03 against the Claude Code documentation pages "Intercept and control agent behavior with hooks", "Automate actions with hooks" and the hooks reference, with the same Python and TypeScript SDK versions as page 1. The practice runs offline: the callbacks are tested directly, and the last case runs the real SDK against the course's scripted stand-in for the Claude Code binary. The tiers, field names and the guard's reasons in the practice are this course's own design and the statement says so; the hook protocol is Anthropic's.

## Why it matters

Page 1 was about what a hook can stop. This page is about what a hook can clean up and about the second kind of hook, the command hook that Claude Code itself runs. Tool results arrive in whatever form their systems use: epoch seconds, numeric status codes, amounts in cents, dates in three layouts. The model reads all of them, and nothing makes it read them consistently. A PostToolUse hook is the place to make them consistent, once, in code. And the command hook is the part of the exam that is not code at all: a JSON block in a settings file and a script that speaks through a process's exit code, where one wrong exit code turns a guard into a suggestion.

## The idea

### Normalise in a PostToolUse hook

The SDK page gives the PostToolUse hook two outputs: `additionalContext`, "to append information to the tool result", and `updatedToolOutput`: "To replace the tool's output before Claude sees it, set `updatedToolOutput`, which works for any tool in both SDKs." The older `updatedMCPToolOutput` "replaces MCP tool output only and is deprecated", so a hook that must work for built-in and MCP tools alike uses the first.

A support agent has three tools from three systems. The order service returns `created` in epoch seconds and a numeric `status`; the billing service returns amounts in cents; a third returns local dates as text. Asked "was the refund after the order date?", the model has to convert in its head, and a conversion done in the model's head is a conversion that can be wrong. A normalising hook converts in code:

| Field | Arrives as | The model sees | Notes |
|---|---|---|---|
| `created` | `1700000000` | `"2023-11-14"` | Seconds, or milliseconds when the number is above 100 billion; always UTC |
| `status` | `2` | `"declined"` | A code that is not in the table becomes `"unknown"`, never a guess |
| `amount_cents` | `12950` | `"amount": "129.50"` | Integer arithmetic, so no rounding error |

Four rules make a normaliser safe.

1. **Change only what you understand.** Fields that are not in the table pass through as they were. An output that is not a JSON object (an error message, plain text) is left alone with an empty answer, since rewriting an error hides the failure.
2. **Be idempotent.** A value that is already readable is not touched, and when nothing changes the hook returns `{}`. A second run, or a retry, changes nothing.
3. **Convert, do not delete.** The practice removes `amount_cents` and writes `amount`, because both would invite the model to use the wrong one. If an audit needs the raw value, log it in the hook before it is replaced.
4. **Remember the order of events.** The hook runs after the tool. The refund exists whatever the model is shown. Normalising is for reading, not for control.

The same job could be done by changing the services, or by telling the model how to convert. The first is right when you own the services and can change them everywhere at once. For third-party tools the hook is the single place you own. The second is a prompt for a task with a right answer, which module 48 gave to code.

### The command hook

Claude Code runs a command hook as a separate process. The guide describes the protocol in one sentence: "Hooks communicate with Claude Code through stdin, stdout, stderr, and exit codes." The event arrives as JSON on standard input (`tool_name`, `tool_input`, `session_id`, `cwd`), and the process answers with its exit code:

| Exit code | Meaning |
|---|---|
| `0` | No objection from the exit code; with JSON printed on standard output, the JSON decides |
| `2` | Blocks the action: "Claude Code blocks the action. Write a reason to stderr." Where the reason lands depends on the event |
| Any other | For most events a non-blocking error: the action goes ahead and a notice appears in the transcript |

The third row is the one that bites. A guard written in Python that fails with an unhandled exception exits with code 1, and exit 1 does not block. A guard that crashes on unexpected input is therefore a guard that lets the call through. The hook must catch its own failures and exit 2, which is the practice's rule: input that is not a JSON object blocks. This is the same design as the denial of an amount that cannot be read in the SDK hook, and the same as module 41's "fail closed".

The guide offers a second way to speak: exit 0 and print a JSON object, for structured control. It adds: "Use exit 2 to block with a stderr message, or exit 0 with JSON for structured control. Choose one approach per hook." Mixing them leaves it to the reference to say which wins, and a guard should not depend on that.

The example's guard is the smallest useful one. It reads the event, fails closed if it cannot, and blocks `git push`:

```python
#!/usr/bin/env python3
import json, re, sys

try:
    event = json.load(sys.stdin)
    command = str((event.get("tool_input") or {}).get("command", ""))
except (ValueError, AttributeError):
    print("guard: input is not a JSON object, blocking to be safe", file=sys.stderr)
    sys.exit(2)
if event.get("tool_name") == "Bash" and re.search(r"\bgit\s+push\b", command):
    print("guard: nothing is pushed from an agent session", file=sys.stderr)
    sys.exit(2)
sys.exit(0)
```

It is registered in a settings file. The block names the event, a matcher for the tool and the command, with the timeout in seconds:

```json
{
  "hooks": {
    "PreToolUse": [
      { "matcher": "Bash",
        "hooks": [ { "type": "command", "command": "python3 .claude/hooks/guard.py", "timeout": 10 } ] }
    ]
  }
}
```

Where the file lives decides who is covered, and the guide's table of locations is short: `~/.claude/settings.json` covers all your projects and is local to your machine, `.claude/settings.json` covers a single project and can be committed to the repository, and `.claude/settings.local.json` covers a single project and is not shared. A rule for the team goes in the committed file (module 40), so that it arrives with the code. If a team needs it in several repositories, it goes in a plugin (module 39). If the same handler is defined in several settings files, it runs once.

### Where the two kinds meet

An SDK application can have both kinds. The SDK page says what it collects when an event fires: "callback hooks you pass in `options.hooks` and shell command hooks from settings files when the corresponding `setting_sources` entry is enabled, which it is for default `query()` options." Two consequences follow. A service that runs with `setting_sources=[]`, as the course's examples do to stay predictable, ignores the command hooks of every settings file: a guard that its team committed to `.claude/settings.json` does not protect that service, and the callbacks in its options are all that run. And a service that uses the defaults runs the project's command hooks too, next to its callbacks, in parallel and with the most restrictive answer applying. Choose on purpose, and write the choice down.

### Hook, prompt or hook that judges

| The rule | Put it in | Because |
|---|---|---|
| Never push; never read `.env`; refunds above 500 are not run | A hook (before the tool) | It has one right answer and the cost of a miss is high |
| Convert a date; map a status code | A hook (after the tool) | It has one right answer and it should be cheap and repeatable |
| Whether a complaint is covered by the policy | The prompt | It is judgement |
| A decision that needs judgement and must still run at a fixed point | A prompt-based or agent-based hook | The guide offers them "for decisions that require judgment rather than deterministic rules" |

The last row keeps the point of a hook (it runs at a fixed point, whether or not the model remembers) and gives up the point of determinism: the judge is a model, so it is evaluated as a model is (module 42), by rate.

### The practice

The practice is `exercises/49-hooks/unit-01/practice-1/statement.md`, in Python and TypeScript only, since the callbacks belong to the Agent SDK. You write the refund gate (`pre_refund`: allow up to 200, ask up to 500, deny above, deny on any amount that cannot be read), the normaliser (`post_normalise`), `build_options` (both hooks, on tool-name matchers, with timeouts), the command hook (`command_hook`: exit 2 with a reason for `git push` and `rm -rf`, exit 0 otherwise, exit 2 for input that is not JSON) and the settings block (`settings_hooks`).

Nine cases grade it: the three tiers, the boundaries (each limit belongs to the lower tier, and the next cent goes up), the amounts that must be refused, the three conversions and their edges, the outputs that must be left alone, the registration, the command hook, the settings block, and a run through the real SDK against the stand-in, in which the model sees an order with a date and a status word and a refund of 900 never runs. The starter fails all nine, the reference passes them, and each of fourteen planted wrong solutions per language fails on an assertion of the case it breaks: a boundary that asks one cent early, a limit that denies its own value, a missing amount that is allowed, an amount coerced from text, a gate that applies to every tool, an epoch always read as milliseconds, cents left beside the amount, plain text rewritten, a normaliser that is not idempotent, a matcher left out, no timeout, a guard that exits with 1, a guard that lets bad input through, and a settings block with no timeout.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"The guard script fails on bad input, which stops the call."** It is tempting because a crash looks like a refusal. The exam rejects it: an unhandled exception exits with 1, which does not block. Catch failures and exit 2.
2. **"Normalise by rewriting every result, errors included."** It is tempting because the output then always looks tidy. The exam rejects it: a hook that rewrites an error message into a tidy result hides the failure from the model. Touch only the fields you know, and leave the rest.
3. **"Replace the raw value and keep no record."** It is tempting because the model should see one value only. The exam rejects it when anyone will need the original: convert for the model, and log the original in the hook.
4. **"The committed guard protects every deployment."** It is tempting because the file is in the repository. The exam rejects it: with `setting_sources=[]` the command hooks in settings files are not collected. Say which kinds of hook run in each deployment.
5. **"Use a prompt-based hook as the guarantee."** It is tempting because it runs at a fixed point. The exam rejects it: it is a model's decision at a fixed point, evaluated by rate, and not a gate.

## Quiz

3. A support agent uses three vendor systems: one gives epoch seconds, one a numeric status code, one local date text. The model keeps comparing dates wrongly. Which fix fits best, given that the team does not own them?
   - **a**: A longer system prompt that teaches the model each vendor's date format by example
   - **b**: A PostToolUse hook that supplies one readable format as the replacement output
   - **c**: A prompt-based hook that judges each result and rewrites the dates it finds wrong
   - **d**: A larger model for the support agent, so that it converts each format correctly

4. A team commits a git-push guard to the project's `.claude/settings.json`. Its batch service runs the SDK with `setting_sources=[]` and a callback hook for refunds. A push attempt from the service is not blocked. Why?
   - **a**: The callback hook overrides every command hook that the settings define
   - **b**: Command hooks are meant for interactive sessions of Claude Code only
   - **c**: The guard sits in the project file, while a service reads the user's file instead
   - **d**: Nothing reads that checked-in file here, so its shell commands are skipped

<details>
<summary>Answer key</summary>

3. **b**. The hook converts once, in code, for any tool, before the model reads the result. *a* is ruled out because a conversion in the model's head can be wrong, and the page assigns that task to code: "is a prompt for a task with a right answer". *c* is ruled out because a model judging each result keeps the conversion probabilistic and "gives up the point of determinism". *d* is ruled out because a larger model still converts by reasoning, and "a conversion done in the model's head is a conversion that can be wrong".
4. **d**. With no setting sources the SDK collects only the callbacks. *a* is ruled out because the two kinds run side by side: "next to its callbacks, in parallel and with the most restrictive answer applying". *b* is ruled out because the SDK collects "shell command hooks from settings files when the corresponding `setting_sources` entry is enabled". *c* is ruled out because the user's file is skipped as well: the service "ignores the command hooks of every settings file".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team checks the refund limit in a PostToolUse hook and, when a refund is above the limit, the hook exits with code 2. Large refunds still happen. What is the cause, and the fix?
   - **a**: Exit 2 does not block in the SDK, so the hook must return a denial
   - **b**: Post hooks may not fire at all, so the limit needs a much longer timeout
   - **c**: The money has already moved by then, so the control belongs ahead of the call
   - **d**: The hook must return replacement output, so the model never sees the refund

2. On Claude Code v2.1.210 or later, a PreToolUse gate in the SDK waits on a slow vendor lookup and exceeds its time limit. What does the model receive, and is the action carried out?
   - **a**: A normal tool result, since a silent hook counts as raising no objection
   - **b**: A notice that the hook never replied by its deadline, and the operation is withheld
   - **c**: A rejection from a person, and the whole run stops until someone responds to it
   - **d**: Nothing at all, and the turn simply waits until the gate finally gives its answer

3. A second callback was written to use the corrected input that a first callback produces, and it sometimes sees the original. What is the right design?
   - **a**: Register the first one earlier so that it always runs before the other
   - **b**: Add a delay to the second one so that the first has time to finish
   - **c**: Return the corrected input from both, so that the last one applies
   - **d**: Merge both steps into one function, as handlers finish in any order

<details>
<summary>Answer key</summary>

1. **c**. A post hook runs after the effect. *a* is ruled out because exit 2 reports back and cannot stop what has run: for `PostToolUse` it "Shows stderr to Claude; the tool already ran". *b* is ruled out because the turn-limit case is a different one: "Hooks may not fire when the agent hits the `max_turns` limit because the session ends before hooks can execute". *d* is ruled out because changing what the model reads does not undo the effect: "The refund exists whatever the model is shown."
2. **b**. A timed-out gate is the safe case: nothing runs and the model is told. *a* is ruled out because the tool is not run: "Claude Code doesn't run the tool call". *c* is ruled out because the model gets a result and the turn goes on: "Claude receives a tool result stating the hook didn't respond before its timeout". *d* is ruled out because a callback past its timeout is stopped: "Claude Code cancels it and discards its output".
3. **d**. Callbacks are not a pipeline. *a* is ruled out because the order of registration does not set the order of completion: "all matching hooks run in parallel". *b* is ruled out because a delay still relies on an order, and the advice is to act "rather than relying on another hook having run first". *c* is ruled out because there is no fixed last one: "completion order is non-deterministic".

</details>
