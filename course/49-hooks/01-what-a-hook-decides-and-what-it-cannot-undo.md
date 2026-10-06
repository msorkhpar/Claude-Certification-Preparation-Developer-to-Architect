# What a hook decides, and what it cannot undo

**Level:** Architect · **Module 49:** Hooks · **Page 1 of 2**
**Exams:** A1.5; A3

**After this page you can** say what a hook is and where it runs in the Agent SDK and in Claude Code, choose the event that fits a control (before the tool, after it, or at the end), write the answer a PreToolUse hook gives (allow, ask, deny, or a changed input), explain what a matcher matches and what it does not, and name what happens when a hook is slow, fails or runs next to another one.

Checked on 2026-10-03 against the Claude Code documentation pages "Intercept and control agent behavior with hooks" (the Agent SDK page), "Automate actions with hooks" and the hooks reference: Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287, the versions the course ran. The example runs offline through the course's scripted stand-in for the Claude Code binary (`harness/fake_claude.py`): no model was called, no network was used and no API key was involved. The stand-in applies hook answers in a simplified way and is not the real binary. This page deepens modules 35 (the SDK and the order in which a call is checked), 39 (hooks in Claude Code) and 41 (hooks that block destructive actions), and it does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* hooks give deterministic guarantees where prompt instructions give only probable compliance; a PostToolUse hook transforms tool results before the model processes them (the guide's examples are Unix timestamps, ISO 8601 dates and numeric status codes returned by different MCP tools); and a hook on the outgoing call blocks a policy violation, such as a refund above 500, and redirects to another workflow such as human escalation. *What the current product does (documentation checked 2026-10-03):* `updatedToolOutput` replaces the output of any tool in both SDKs, and the older field for MCP tools only is deprecated. A PreToolUse hook has no redirect action of its own: a denial carries a reason that the model reads, so the redirect is the path that the reason names; `ask` sends the call to a person; `defer` ends the turn so that the call can be resumed. A hook's `allow` does not skip deny and ask rules, and several events exist only in TypeScript. On the exam, choose a hook for a rule that must hold, and choose a denial whose reason names the escalation path over a post hook that tries to undo what has happened.

## Why it matters

Module 48 put a guarantee in the code that runs the tools. In the Agent SDK you do not write that dispatcher: the Claude Code binary runs the loop and the tools. A hook is the way your code gets a place in that loop. The exam asks the architect's questions about it. Which control belongs in a hook and which in the prompt? Which event? What can the hook change, and what can no hook change because the tool has already run? What does a hook do when it fails? Getting the event wrong is the common mistake, and it is silent: a check on the wrong side of the tool call looks fine and protects nothing.

## The idea

### What a hook is

"Hooks are callback functions that run your code in response to agent events, like a tool being called, a session starting, or execution stopping." In the SDK you pass the callbacks in the `hooks` option, keyed by event name; in Claude Code you write a shell command in a settings file (page 2). Either way the point is the one the hooks guide makes: hooks give "deterministic control: certain actions always happen rather than relying on the LLM to choose to run them." The model is not asked. The harness fires the event, runs the hook and acts on its answer. That is why a hook is the tool for a rule that must hold, and the prompt is the tool for judgement (modules 39 and 48).

A hook is also a program you write, which can fail, hang or be wrong, and the rest of this page is about those cases.

### Choose the event

Three events cover most controls.

| Event | Fires | What the hook can do | What it cannot do |
|---|---|---|---|
| `PreToolUse` | Before a tool runs | Allow, ask, deny or defer the call; change its input | Know what the tool will return |
| `PostToolUse` | After a tool ran | Replace the output the model sees; add context | Stop the call: the tool has run |
| `Stop` | When the run is about to end | Save state before exit | Change anything that happened |

The hooks reference puts the limit of the second row in its exit code table: for `PostToolUse` an exit of 2 "Shows stderr to Claude; the tool already ran". A post hook can tell the model that something went wrong. It cannot make the refund not have happened. A control that must prevent an effect therefore belongs in `PreToolUse`, and a team that writes its limit check as a post hook has written a log line. Use a post hook for what comes after: normalising the result (page 2), recording it, adding context for the next step.

There are more events: the list has ten that both SDKs share (the three above, `PostToolUseFailure`, `UserPromptSubmit`, `SubagentStart`, `SubagentStop`, `PreCompact`, `PermissionRequest` and `Notification`) and many that only TypeScript has. The page of the SDK says: "Some hooks are available in both SDKs, while others are TypeScript-only." `SessionStart` and `SessionEnd` are in the second group, and so are `PostToolBatch`, `PostCompact` and `MessageDisplay`. In Python they exist only "as shell command hooks defined in settings files such as `.claude/settings.json`", and the documentation's advice for a Python callback is to use the first message of the stream as the trigger. An architect who plans a start-up step as a Python callback has planned something the SDK cannot register.

### What a PreToolUse hook answers

The callback receives three arguments (the input, the tool use id that ties a pre and a post event together, and a context), and returns an object. An empty object means "no objection". The answer goes in `hookSpecificOutput`:

| Field | Meaning |
|---|---|
| `permissionDecision` | `"allow"`, `"deny"`, `"ask"` or `"defer"` |
| `permissionDecisionReason` | The explanation; for a denial it is what the model reads |
| `updatedInput` | A replacement input for the tool |

Four behaviours of this answer decide whether a hook does what its author meant.

1. **Deny teaches the model.** In the documentation's example the reason "tells the model why, so it avoids retrying." A reason that says "Writing to /etc is not allowed" ends the attempt; a reason that says "Refunds above 500 are escalated to a person" tells the model what to do next, which is the same design as the refusal sentence of module 48. A reason is part of the interface.
2. **Allow skips the prompt, ask puts a person in the loop.** An `allow` answer approves the call without the usual permission prompt, an `ask` shows the call to a person for approval, and `defer` ends the turn with a result whose `stop_reason` is `"tool_deferred"`, so that the call can be resumed later. An `allow` is not a bypass: "A hook that returns `allow` does not skip the deny and ask rules below; those are evaluated regardless of the hook result." Tiers fit these three: a refund up to 200 is allowed, one up to 500 asks a person, a larger one is denied. The practice writes exactly this gate.
3. **The changed input has one place.** "Ensure `updatedInput` is inside `hookSpecificOutput`, not at the top level." A hook that returns it at the top level changes nothing. The documentation pairs it with a decision: "Pair `updatedInput` with `permissionDecision: 'allow'` to auto-approve the modified input, or `permissionDecision: 'ask'` to show it to the user." If you leave the decision out, "the modified input still applies and flows through the normal permission evaluation", and with `defer` the input is dropped. Return a new object rather than editing the one you were given. Include `hookEventName` too, since it says which event the output is for.
4. **Deny wins, and nothing runs in an order.** When several hooks or rules apply, "`deny` takes priority over `defer`, which takes priority over `ask`, which takes priority over `allow`" (module 44 uses this). The part that is new here is the other half: "all matching hooks run in parallel", and "because completion order is non-deterministic, write each hook to act independently rather than relying on another hook having run first." Two hooks cannot be a pipeline. If the second needs what the first computed, they are one hook. If they are independent checks (an authorization check, a validator, an audit logger), they run side by side and the most restrictive answer applies.

Hooks also run inside subagents, with an `agent_id` and an `agent_type` in the input (module 47), so a hook that must apply only to the coordinator has to look at those fields.

### What a matcher matches

A matcher narrows which calls a hook sees. For tool events it is tested against the tool name: "Hooks without a matcher run for every event of that type", and omitting it on purpose is the way to log every call. The syntax has three cases. A value with only letters, digits, underscores, hyphens, spaces, commas and pipes is read as an exact name or a list of exact names (`Write|Edit`, which is how the SDK page registers its file guard); anything else is read as a regular expression (`^mcp__` for every MCP tool, whose names look like `mcp__<server>__<action>`); a missing, empty or `*` matcher matches all.

The limit of the matcher is the common trap: "Matchers only match tool names, not file paths or other arguments. To filter by file path, check `tool_input.file_path` inside your hook." A matcher such as `src/payments/*` will never fire, because no tool has that name. The hook is registered for the file tools, reads the path from the input and returns an empty object for the paths it does not care about. The same holds for a command: to stop `git push`, register on `Bash` and read `tool_input.command` (module 41 explains why the reading must be careful).

### Slow, failing and silent hooks

A hook is code, so its failures are part of the design.

- **Timeouts.** Each callback runs with a timeout in seconds, set on its matcher; when it is omitted the event's default applies, 600 seconds for most events. When a callback exceeds it, "Claude Code cancels it and discards its output", and the session goes on. What follows depends on the event. For `PreToolUse` the outcome of a timeout is the safe one (on Claude Code v2.1.210 or later; before that the timeout was reported to the model as a user rejection): "Claude Code doesn't run the tool call, Claude receives a tool result stating the hook didn't respond before its timeout, and the turn continues." For `PostToolUse` the tool result is kept. For `UserPromptSubmit` the prompt is blocked: "Claude Code never lets a timed-out prompt through unscreened." A gate that hangs therefore blocks and does not wave the call through, but a gate that is merely slow costs the run a refused call, so keep the timeout short and the callback quick. Hold the HTTP calls of a hook to a timeout of their own and catch their errors: the documentation's advice is to "catch errors inside your hook instead of letting them propagate."
- **Async output.** A callback can return `{"async_": True}` (`async: true` in TypeScript) to let the agent go on at once, for a side effect such as a webhook. The limit is in the same sentence as the use: "Async outputs can't block, modify, or inject context into the operation since the agent has already moved on."
- **The run that ends first.** "Hooks may not fire when the agent hits the `max_turns` limit because the session ends before hooks can execute." An audit that must record every call cannot rely on a hook alone when a run may be cut off by the turn limit (module 45).
- **Hook never fires.** The event name is case-sensitive, the matcher must match the tool name exactly, and the hook must be under the right event key. These three account for most of the cases.

### The example

The example registers a PreToolUse hook on `process_refund` and a PostToolUse hook on `get_order` in the real SDK, against the stand-in, and runs a script of five calls: an order lookup, refunds of 50, 350 and 900, and a refund with no amount. It prints what the model saw after each call and the order in which the hooks and the permission callback ran. The last lines run the command hook of page 2 as a process.

<!-- example: m49-hook-gates tabs: python,typescript -->
```python
"""Hooks in the Agent SDK, offline: a gate before a tool, a normaliser after another, and a command hook run as the process it is.

The SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
The refund and order tools are scripted: their output is what the stand-in reports when a call is allowed. `claude-agent-sdk` 0.2.163, checked on
2026-10-03 against the hooks pages of the Claude Code documentation.
"""
import asyncio
import json
import os
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from claude_agent_sdk import (AssistantMessage, ClaudeAgentOptions, HookMatcher, PermissionResultAllow, PermissionResultDeny, ResultMessage, ToolResultBlock, ToolUseBlock, UserMessage, query)
import logging

log = logging.getLogger(__name__)

HERE = Path(__file__).resolve()
_found = str(HERE.parents[3] / "harness" / "fake_claude.py")
# The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
FAKE = shutil.copy(_found, Path(tempfile.mkdtemp(), "fake_claude.py"))
os.chmod(FAKE, 0o755)
GUARD = str(HERE.parents[1] / "guard_hook.py")
log = []


def tier(amount):
    """The decision for a refund amount: small goes through, a middle one needs a person, a large one is refused."""
    if isinstance(amount, bool) or not isinstance(amount, (int, float)) or amount <= 0:
        return "deny", "the amount is missing or invalid"
    if amount <= 200:
        return "allow", "within the automatic limit"
    if amount <= 500:
        return "ask", "needs a person's approval"
    return "deny", "above the limit of 500, escalate to a person"


async def refund_gate(input_data, tool_use_id, context):
    decision, reason = tier(input_data["tool_input"].get("amount"))
    log.append(f"PreToolUse {input_data['tool_name']} amount={input_data['tool_input'].get('amount')} -> {decision}")
    return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": decision, "permissionDecisionReason": reason}}


async def readable_order(input_data, tool_use_id, context):
    order = json.loads(input_data["tool_response"])
    order["created"] = datetime.fromtimestamp(order["created"], timezone.utc).strftime("%Y-%m-%d")
    order["status"] = {0: "pending", 1: "approved", 2: "declined"}.get(order["status"], "unknown")
    log.append(f"PostToolUse {input_data['tool_name']} -> readable output")
    return {"hookSpecificOutput": {"hookEventName": "PostToolUse", "updatedToolOutput": json.dumps(order, separators=(",", ":"))}}


async def person_says_no(tool_name, tool_input, context):
    if tool_name == "get_order":
        return PermissionResultAllow(updated_input=tool_input)
    log.append(f"a person is asked about {tool_name} amount={tool_input.get('amount')} and declines")
    return PermissionResultDeny(message="A person declined this refund")


def options(cwd):
    return ClaudeAgentOptions(
        cli_path=FAKE, cwd=cwd, permission_mode="default", setting_sources=[], max_turns=8,
        can_use_tool=person_says_no,
        hooks={"PreToolUse": [HookMatcher(matcher="process_refund", hooks=[refund_gate])], "PostToolUse": [HookMatcher(matcher="get_order", hooks=[readable_order])]})


def step(id, name, input, output):
    return {"tool": {"id": id, "name": name, "input": input, "output": output}}


SCRIPT = [step("t1", "get_order", {"order": "A-7"}, json.dumps({"order": "A-7", "created": 1700000000, "status": 1, "total_cents": 12950})),
          step("t2", "process_refund", {"order": "A-7", "amount": 50}, "refund R-1 created"),
          step("t3", "process_refund", {"order": "A-7", "amount": 350}, "refund R-2 created"),
          step("t4", "process_refund", {"order": "A-7", "amount": 900}, "refund R-3 created"),
          step("t5", "process_refund", {"order": "A-7"}, "refund R-4 created"),
          {"result": {"subtype": "success", "result": "done", "cost": 0.02, "turns": 6}}]


def run_guard(event):
    """Run the command hook as Claude Code does: JSON on standard input, then read the exit code and standard error."""
    done = subprocess.run([sys.executable, GUARD], input=event, capture_output=True, text=True)
    return done.returncode, done.stderr.strip()


async def main():
    workdir = tempfile.mkdtemp()
    script = Path(workdir, "script.json")
    script.write_text(json.dumps({"session_id": "demo", "turns": [SCRIPT]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(Path(workdir, "record.jsonl"))
    names = {}
    async for message in query(prompt="Refund order A-7", options=options(workdir)):
        if isinstance(message, AssistantMessage):
            for block in message.content:
                if isinstance(block, ToolUseBlock):
                    names[block.id] = f"{block.name} {json.dumps(block.input, separators=(',', ':'))}"
        elif isinstance(message, UserMessage) and isinstance(message.content, list):
            for block in message.content:
                if isinstance(block, ToolResultBlock):
                    print(f"{names[block.tool_use_id]}\n  the model sees: {block.content}")
        elif isinstance(message, ResultMessage):
            print(f"done: {message.subtype}")
    print("\nwhat ran in this process, in order:")
    for line in log:
        print(" ", line)
    print("\nthe command hook, as a process:")
    for event in ({"tool_name": "Bash", "tool_input": {"command": "git push origin main"}}, {"tool_name": "Bash", "tool_input": {"command": "git status"}}, "{not json"):
        code, reason = run_guard(event if isinstance(event, str) else json.dumps(event))
        print(f"  {event if isinstance(event, str) else event['tool_input']['command']!r}: exit {code}{', ' + reason if reason else ''}")


if __name__ == "__main__":
    asyncio.run(main())
```
```text
get_order {"order":"A-7"}
  the model sees: {"order":"A-7","created":"2023-11-14","status":"approved","total_cents":12950}
process_refund {"order":"A-7","amount":50}
  the model sees: refund R-1 created
process_refund {"order":"A-7","amount":350}
  the model sees: Permission denied: A person declined this refund
process_refund {"order":"A-7","amount":900}
  the model sees: Permission denied: above the limit of 500, escalate to a person
process_refund {"order":"A-7"}
  the model sees: Permission denied: the amount is missing or invalid
done: success

what ran in this process, in order:
  PostToolUse get_order -> readable output
  PreToolUse process_refund amount=50 -> allow
  PreToolUse process_refund amount=350 -> ask
  a person is asked about process_refund amount=350 and declines
  PreToolUse process_refund amount=900 -> deny
  PreToolUse process_refund amount=None -> deny

the command hook, as a process:
  'git push origin main': exit 2, guard: nothing is pushed from an agent session
  'git status': exit 0
  '{not json': exit 2, guard: input is not a JSON object, blocking to be safe
```
```typescript
// Hooks in the Agent SDK, offline: a gate before a tool, a normaliser after another, and a command hook run as the process it is.
//
// The SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
// The refund and order tools are scripted: their output is what the stand-in reports when a call is allowed.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the hooks pages of the Claude Code documentation.
import { spawnSync } from "node:child_process";
import { chmodSync, copyFileSync, mkdtempSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("hook_gates");

// The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
const FAKE_SOURCE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;
export const FAKE = (() => {
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(FAKE_SOURCE, exe);
  chmodSync(exe, 0o755);
  return exe;
})();
const GUARD = new URL("../guard_hook.py", import.meta.url).pathname;
export const ran: string[] = [];

/** The decision for a refund amount: small goes through, a middle one needs a person, a large one is refused. */
export function tier(amount: unknown): [string, string] {
  if (typeof amount !== "number" || !Number.isFinite(amount) || amount <= 0) return ["deny", "the amount is missing or invalid"];
  if (amount <= 200) return ["allow", "within the automatic limit"];
  if (amount <= 500) return ["ask", "needs a person's approval"];
  return ["deny", "above the limit of 500, escalate to a person"];
}

async function refundGate(input: any) {
  const [decision, reason] = tier(input.tool_input.amount);
  ran.push(`PreToolUse ${input.tool_name} amount=${input.tool_input.amount ?? "None"} -> ${decision}`);
  return { hookSpecificOutput: { hookEventName: "PreToolUse" as const, permissionDecision: decision as "allow" | "ask" | "deny", permissionDecisionReason: reason } };
}

export async function readableOrder(input: any) {
  const order = JSON.parse(input.tool_response);
  order.created = new Date(order.created * 1000).toISOString().slice(0, 10);
  order.status = ({ 0: "pending", 1: "approved", 2: "declined" } as Record<number, string>)[order.status] ?? "unknown";
  ran.push(`PostToolUse ${input.tool_name} -> readable output`);
  return { hookSpecificOutput: { hookEventName: "PostToolUse" as const, updatedToolOutput: JSON.stringify(order) } };
}

async function personSaysNo(toolName: string, toolInput: Record<string, any>) {
  if (toolName === "get_order") return { behavior: "allow" as const, updatedInput: toolInput };
  ran.push(`a person is asked about ${toolName} amount=${toolInput.amount} and declines`);
  return { behavior: "deny" as const, message: "A person declined this refund" };
}

function options(cwd: string) {
  return {
    pathToClaudeCodeExecutable: FAKE, cwd, permissionMode: "default" as const,
    settingSources: [] as ("user" | "project" | "local")[], maxTurns: 8, canUseTool: personSaysNo,
    hooks: { PreToolUse: [{ matcher: "process_refund", hooks: [refundGate] }], PostToolUse: [{ matcher: "get_order", hooks: [readableOrder] }] },
  };
}

const step = (id: string, name: string, input: Record<string, unknown>, output: string) => ({ tool: { id, name, input, output } });

const SCRIPT = [
  step("t1", "get_order", { order: "A-7" }, JSON.stringify({ order: "A-7", created: 1700000000, status: 1, total_cents: 12950 })),
  step("t2", "process_refund", { order: "A-7", amount: 50 }, "refund R-1 created"),
  step("t3", "process_refund", { order: "A-7", amount: 350 }, "refund R-2 created"),
  step("t4", "process_refund", { order: "A-7", amount: 900 }, "refund R-3 created"),
  step("t5", "process_refund", { order: "A-7" }, "refund R-4 created"),
  { result: { subtype: "success", result: "done", cost: 0.02, turns: 6 } },
];

/** Run the command hook as Claude Code does: JSON on standard input, then read the exit code and standard error. */
export function runGuard(event: string): [number, string] {
  const done = spawnSync("python3", [GUARD], { input: event, encoding: "utf8" });
  return [done.status ?? -1, done.stderr.trim()];
}

async function main() {
  const workdir = mkdtempSync(join(tmpdir(), "hooks-"));
  const script = join(workdir, "script.json");
  writeFileSync(script, JSON.stringify({ session_id: "demo", turns: [SCRIPT] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = join(workdir, "record.jsonl");
  const names: Record<string, string> = {};
  for await (const message of query({ prompt: "Refund order A-7", options: options(workdir) as any }) as AsyncIterable<any>) {
    if (message.type === "assistant") {
      for (const block of message.message.content) if (block.type === "tool_use") names[block.id] = `${block.name} ${JSON.stringify(block.input)}`;
    } else if (message.type === "user" && Array.isArray(message.message.content)) {
      for (const block of message.message.content) if (block.type === "tool_result") console.log(`${names[block.tool_use_id]}\n  the model sees: ${block.content}`);
    } else if (message.type === "result") console.log(`done: ${message.subtype}`);
  }
  console.log("\nwhat ran in this process, in order:");
  for (const line of ran) console.log(" ", line);
  console.log("\nthe command hook, as a process:");
  for (const event of [{ tool_name: "Bash", tool_input: { command: "git push origin main" } }, { tool_name: "Bash", tool_input: { command: "git status" } }, "{not json"]) {
    const [code, reason] = runGuard(typeof event === "string" ? event : JSON.stringify(event));
    console.log(`  '${typeof event === "string" ? event : event.tool_input.command}': exit ${code}${reason ? ", " + reason : ""}`);
  }
}

if (import.meta.main) await main();
```
```text
get_order {"order":"A-7"}
  the model sees: {"order":"A-7","created":"2023-11-14","status":"approved","total_cents":12950}
process_refund {"order":"A-7","amount":50}
  the model sees: refund R-1 created
process_refund {"order":"A-7","amount":350}
  the model sees: Permission denied: A person declined this refund
process_refund {"order":"A-7","amount":900}
  the model sees: Permission denied: above the limit of 500, escalate to a person
process_refund {"order":"A-7"}
  the model sees: Permission denied: the amount is missing or invalid
done: success

what ran in this process, in order:
  PostToolUse get_order -> readable output
  PreToolUse process_refund amount=50 -> allow
  PreToolUse process_refund amount=350 -> ask
  a person is asked about process_refund amount=350 and declines
  PreToolUse process_refund amount=900 -> deny
  PreToolUse process_refund amount=None -> deny

the command hook, as a process:
  'git push origin main': exit 2, guard: nothing is pushed from an agent session
  'git status': exit 0
  '{not json': exit 2, guard: input is not a JSON object, blocking to be safe
```
<!-- /example -->

Read the output from the top. The order lookup comes back with a date and a status word, which the hook wrote (page 2). The refund of 50 ran: the hook allowed it and nobody was asked. The refund of 350 triggered the permission callback, which declined as a person would have. The refund of 900 never reached the callback: the hook denied it with a reason that names the limit. The refund with no amount was denied too, because a gate that cannot read its input must not let the call through. In the stand-in, as in the documentation, the hook ran in your process and the model never saw a rule, only the results.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Check the refund limit in a PostToolUse hook."** It is tempting because the output shows the amount. The exam rejects it: the tool has run, and the hook can only report. Put every control that must prevent an effect in `PreToolUse`.
2. **"Register the hook on the path with a matcher such as `src/payments/*`."** It is tempting because the matcher looks like a filter on files. The exam rejects it: matchers see tool names. Register for the tool and test the path or the command inside the callback.
3. **"Return the changed input at the top level of the hook's answer."** It is tempting because it looks like the other top-level fields. The exam rejects it: it changes nothing. It belongs inside `hookSpecificOutput`, with an allow or ask when the change should be approved.
4. **"Run the second hook on what the first one produced."** It is tempting because hooks are listed in order. The exam rejects it: they run in parallel and finish in no fixed order. Merge them or make each one stand alone.
5. **"Write the start-up step as a Python callback."** It is tempting because the guide speaks of SDK hooks without naming languages. The exam rejects it: `SessionStart` is TypeScript-only as a callback; in Python it is a command hook in a settings file.

## Quiz

1. A PreToolUse hook is meant to rewrite a command before it runs. It returns the new input in a field next to `hookSpecificOutput`, and the original command runs unchanged. What is the cause?
   - **a**: The value is honoured only when nested in the answer structure
   - **b**: A rewrite takes effect only if the hook answers with a deferral
   - **c**: Commands can be rewritten only by a hook that runs as a separate program
   - **d**: The rewrite was applied, then undone by the session's permission mode

2. A team adds a hook with the matcher `src/payments/*` to review every write under that folder. The hook never fires. What is the best fix?
   - **a**: Switch the matcher to a regular expression that lists every file in the folder
   - **b**: Attach it to the Bash tool and read the folder from the command it runs
   - **c**: Attach it to the file-editing tools and test the path inside the callback
   - **d**: Move the hook to the PostToolUse event and match the path after the write

<details>
<summary>Answer key</summary>

1. **a**. The answer is read only from its documented place. *b* is ruled out because a deferral discards the change: "the input is dropped". *c* is ruled out because callback hooks may change input too: "Pair `updatedInput` with `permissionDecision: 'allow'` to auto-approve the modified input". *d* is ruled out because nothing undoes a change that is applied: "the modified input still applies and flows through the normal permission evaluation".
2. **c**. A matcher sees only the name of the tool, so the path is read from the input. *a* is ruled out because "Matchers only match tool names, not file paths or other arguments." *b* is ruled out because a write under the folder is made by a file tool, not by a shell command, and `Write|Edit` is the matcher "which is how the SDK page registers its file guard". *d* is ruled out because a post hook's matcher is read the same way: "For tool events it is tested against the tool name".

</details>
