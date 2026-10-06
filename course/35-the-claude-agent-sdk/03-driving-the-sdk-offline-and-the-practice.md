# Driving the Agent SDK offline, and the practice

**Level:** Developer · **Module 35:** The Claude Agent SDK · **Page 3 of 3**
**Exams:** DV3; A1

**After this page you can** run the real Agent SDK against a scripted stand-in for the Claude Code binary, read what the SDK sent and what the binary asked of your process, say which parts of that behaviour belong to the stand-in and not to the real binary, and say what the practice builds and grades.

Checked against the Agent SDK pages of the Claude Code documentation on 2026-10-03, with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287 in the course container. The stand-in is the course's own file, `harness/fake_claude.py`. It speaks the SDK's protocol, replays a script and applies a simplified copy of the documented rules. It is not the real binary, and nothing here shows how the real binary words its messages or schedules its calls.

## Why it matters

A test that calls a model is slow, costs money and gives a different answer each time, so an agent's policy (what is allowed, what a hook vetoes, what happens at a limit) is usually left untested. The SDK makes a better test possible, because the model and the tools sit behind one process boundary. Replace the binary, and the SDK code in your program runs for real: it builds the flags, answers the control requests and parses the stream. What you test is the part that you own.

## The idea

### How the SDK reaches the binary

Both SDKs let you choose the executable that they start: `cli_path` in Python and `pathToClaudeCodeExecutable` in TypeScript. By default it is the bundled native binary. The binary and the SDK talk with newline-delimited JSON on standard input and output (the `stream-json` protocol), plus a small control protocol on the same pipes: the SDK sends an initialize request that registers the hooks, and the binary sends control requests back when it needs your process, for a permission decision (`can_use_tool`), for a hook callback (`hook_callback`) and for a call to one of your in-process tools (`mcp_message`).

The stand-in uses exactly that. A script, a JSON file named in an environment variable, lists the steps of a run: text the assistant says, a tool call with the output it would produce, and the final result message. For each tool step the stand-in applies a simplified copy of the rules of page 2: a tool that is not in `--tools` does not exist, `PreToolUse` hooks that were registered at the handshake are called and may deny, a tool in `--disallowedTools` is denied, a tool in `--allowedTools` runs without asking, `bypassPermissions` runs it, `dontAsk` denies it, and anything else goes to your callback. It also records the command line, the working directory and every request it sent, so a test can read what the SDK did. After an error result it exits with a nonzero code, as the documentation says a single-shot run does, so the SDK raises after it yields the result. This is "a simplified copy of the documented behaviour, not the real binary", and the practice's cases are written against the documented behaviour that the copy models.

### The example: three kinds of callback in one run

The example below builds two custom tools (`add` and `divide`) on an in-process server named `calc`, a `PreToolUse` hook that refuses `git push`, and a permission callback. It runs one scripted conversation in which the model adds, divides by zero, tries to push and lists the files.

<!-- example: m35-agent-sdk-offline tabs: python,typescript -->
```python
"""The Agent SDK driven offline: a custom tool, a hook and a permission callback, against a scripted stand-in for the Claude Code binary.

The Agent SDK is a library around the Claude Code binary: it starts the binary, translates the options into flags, and answers the
binary's control requests (hooks, permission questions, calls to your in-process tools). Here the binary is `harness/fake_claude.py`,
which speaks the same stream-json protocol and replays a script, so no model is called and no network is used. `claude-agent-sdk`
0.2.163, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
"""
import logging
import asyncio
import json
import os
import shutil
import tempfile
from collections import Counter
from pathlib import Path

from claude_agent_sdk import (AssistantMessage, ClaudeAgentOptions, HookMatcher, PermissionResultAllow, PermissionResultDeny, ResultMessage, SystemMessage,
                              TextBlock, ToolResultBlock, ToolUseBlock, UserMessage, create_sdk_mcp_server, query, tool)

log = logging.getLogger(__name__)

_found = str(Path(__file__).resolve().parents[3] / "harness" / "fake_claude.py")
# The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
FAKE = shutil.copy(_found, Path(tempfile.mkdtemp(), "fake_claude.py"))
os.chmod(FAKE, 0o755)


@tool("add", "Add two whole numbers", {"a": int, "b": int})
async def add(args):
    return {"content": [{"type": "text", "text": f"Sum: {args['a'] + args['b']}"}]}


@tool("divide", "Divide one whole number by another", {"a": int, "b": int})
async def divide(args):
    if args["b"] == 0:
        return {"content": [{"type": "text", "text": "Cannot divide by zero"}], "is_error": True}  # the message Claude reads, written by you
    return {"content": [{"type": "text", "text": f"Quotient: {args['a'] // args['b']}"}]}


async def no_push(input_data, tool_use_id, context):
    """A PreToolUse hook: it runs in your process before the tool and can deny the call."""
    if "git push" in input_data["tool_input"].get("command", ""):
        return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "Nothing is pushed from an agent"}}
    return {}


async def ask_me(tool_name, tool_input, context):
    """The permission callback: it is asked about a call that no rule has decided. The tools of your own server are allowed here, not by
    allowed_tools: an allowed_tools entry approves a tool before this callback is consulted, and the SDK warns when it would shadow it."""
    if tool_name.startswith("mcp__calc__") or (tool_name == "Bash" and tool_input["command"].split()[0] == "ls"):
        return PermissionResultAllow(updated_input=tool_input)
    return PermissionResultDeny(message=f"{tool_name} is not allowed here")


SCRIPT = [{"say": "I will add, then divide."},
          {"tool": {"id": "t1", "name": "mcp__calc__add", "input": {"a": 2, "b": 3}}},
          {"tool": {"id": "t2", "name": "mcp__calc__divide", "input": {"a": 1, "b": 0}}},
          {"tool": {"id": "t3", "name": "Bash", "input": {"command": "git push origin main"}, "output": "pushed"}},
          {"tool": {"id": "t4", "name": "Bash", "input": {"command": "ls"}, "output": "README.md"}},
          {"say": "Two sums, one refused push."},
          {"result": {"subtype": "success", "result": "Two sums, one refused push.", "cost": 0.02, "turns": 5}}]


def options(workdir):
    return ClaudeAgentOptions(cli_path=FAKE, cwd=workdir, tools=["Read", "Bash"], disallowed_tools=["Bash(rm *)"], max_turns=6,
                              permission_mode="default", setting_sources=[], mcp_servers={"calc": create_sdk_mcp_server("calc", tools=[add, divide])}, can_use_tool=ask_me,
                              hooks={"PreToolUse": [HookMatcher(matcher="Bash", hooks=[no_push])]})


def show(message):
    if isinstance(message, SystemMessage) and message.subtype == "init":
        print("init: the binary reports tools", message.data["tools"])
    elif isinstance(message, AssistantMessage):
        for block in message.content:
            print("claude says:" if isinstance(block, TextBlock) else "claude calls:", block.text if isinstance(block, TextBlock) else f"{block.name} {json.dumps(block.input, separators=(',', ':'))}")
    elif isinstance(message, UserMessage):
        for block in message.content:
            if isinstance(block, ToolResultBlock):
                print("  result:", "ERROR" if block.is_error else "ok   ", block.content)
    elif isinstance(message, ResultMessage):
        print(f"done: {message.subtype}, {message.num_turns} turns, cost ${message.total_cost_usd:.2f}")


async def main():
    workdir = tempfile.mkdtemp()
    script, record = Path(workdir, "script.json"), Path(workdir, "record.jsonl")
    script.write_text(json.dumps({"session_id": "demo", "turns": [SCRIPT]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    async for message in query(prompt="Add 2 and 3, divide 1 by 0, push, then list the files.", options=options(workdir)):
        show(message)
    lines = [json.loads(line) for line in record.read_text().splitlines()]
    argv = next(line["argv"] for line in lines if "argv" in line)
    print()
    for flag in ("--tools", "--disallowedTools", "--max-turns", "--permission-mode"):
        print(f"flag {flag} {argv[argv.index(flag) + 1]}")
    asked = Counter(line["ask"]["subtype"] for line in lines if "ask" in line)
    print("what the binary asked your process:", dict(sorted(asked.items())))


if __name__ == "__main__":
    asyncio.run(main())
```
```text
init: the binary reports tools ['Read', 'Bash']
claude says: I will add, then divide.
claude calls: mcp__calc__add {"a":2,"b":3}
  result: ok    Sum: 5
claude calls: mcp__calc__divide {"a":1,"b":0}
  result: ERROR Cannot divide by zero
claude calls: Bash {"command":"git push origin main"}
  result: ERROR Permission denied: Nothing is pushed from an agent
claude calls: Bash {"command":"ls"}
  result: ok    README.md
claude says: Two sums, one refused push.
done: success, 5 turns, cost $0.02

flag --tools Read,Bash
flag --disallowedTools Bash(rm *)
flag --max-turns 6
flag --permission-mode default
what the binary asked your process: {'can_use_tool': 3, 'hook_callback': 2, 'mcp_message': 6}
```
```typescript
// The Agent SDK driven offline: a custom tool, a hook and a permission callback, against a scripted stand-in for the Claude Code binary.
//
// The Agent SDK is a library around the Claude Code binary: it starts the binary, translates the options into flags, and answers the
// binary's control requests (hooks, permission questions, calls to your in-process tools). Here the binary is `harness/fake_claude.py`,
// which speaks the same stream-json protocol and replays a script, so no model is called and no network is used.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
import { chmodSync, copyFileSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { createSdkMcpServer, query, tool } from "@anthropic-ai/claude-agent-sdk";
import { z } from "zod";
import { logger } from "./logger.ts";
const log = logger("agent_offline");

// The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
const FAKE_SOURCE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;
export const FAKE = (() => {
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(FAKE_SOURCE, exe);
  chmodSync(exe, 0o755);
  return exe;
})();

export const add = tool("add", "Add two whole numbers", { a: z.number().int(), b: z.number().int() }, async (args) => ({ content: [{ type: "text" as const, text: `Sum: ${args.a + args.b}` }] }));

export const divide = tool("divide", "Divide one whole number by another", { a: z.number().int(), b: z.number().int() }, async (args) => {
  if (args.b === 0) return { content: [{ type: "text" as const, text: "Cannot divide by zero" }], isError: true }; // the message Claude reads, written by you
  return { content: [{ type: "text" as const, text: `Quotient: ${Math.floor(args.a / args.b)}` }] };
});

/** A PreToolUse hook: it runs in your process before the tool and can deny the call. */
export async function noPush(input: any) {
  if ((input.tool_input?.command ?? "").includes("git push")) {
    return { hookSpecificOutput: { hookEventName: "PreToolUse" as const, permissionDecision: "deny" as const, permissionDecisionReason: "Nothing is pushed from an agent" } };
  }
  return {};
}

/** The permission callback: it is asked about a call that no rule has decided. The tools of your own server are allowed here, not by
 * allowedTools: an allowedTools entry approves a tool before this callback is consulted, and the SDK warns when it would shadow it. */
export async function askMe(toolName: string, toolInput: Record<string, any>) {
  if (toolName.startsWith("mcp__calc__") || (toolName === "Bash" && toolInput.command.split(" ")[0] === "ls")) return { behavior: "allow" as const, updatedInput: toolInput };
  return { behavior: "deny" as const, message: `${toolName} is not allowed here` };
}

const SCRIPT = [
  { say: "I will add, then divide." },
  { tool: { id: "t1", name: "mcp__calc__add", input: { a: 2, b: 3 } } },
  { tool: { id: "t2", name: "mcp__calc__divide", input: { a: 1, b: 0 } } },
  { tool: { id: "t3", name: "Bash", input: { command: "git push origin main" }, output: "pushed" } },
  { tool: { id: "t4", name: "Bash", input: { command: "ls" }, output: "README.md" } },
  { say: "Two sums, one refused push." },
  { result: { subtype: "success", result: "Two sums, one refused push.", cost: 0.02, turns: 5 } },
];

function options(workdir: string) {
  return {
    pathToClaudeCodeExecutable: FAKE, cwd: workdir, tools: ["Read", "Bash"], disallowedTools: ["Bash(rm *)"], maxTurns: 6,
    permissionMode: "default" as const, settingSources: [] as ("user" | "project" | "local")[], mcpServers: { calc: createSdkMcpServer({ name: "calc", tools: [add, divide] }) },
    canUseTool: askMe, hooks: { PreToolUse: [{ matcher: "Bash", hooks: [noPush] }] },
  };
}

function show(message: any) {
  if (message.type === "system" && message.subtype === "init") console.log(`init: the binary reports tools [${message.tools.map((t: string) => `'${t}'`).join(", ")}]`);
  else if (message.type === "assistant") {
    for (const block of message.message.content) console.log(block.type === "text" ? "claude says:" : "claude calls:", block.type === "text" ? block.text : `${block.name} ${JSON.stringify(block.input)}`);
  } else if (message.type === "user" && Array.isArray(message.message.content)) {
    for (const block of message.message.content) if (block.type === "tool_result") console.log("  result:", block.is_error ? "ERROR" : "ok   ", block.content);
  } else if (message.type === "result") console.log(`done: ${message.subtype}, ${message.num_turns} turns, cost $${message.total_cost_usd.toFixed(2)}`);
}

async function main() {
  const workdir = mkdtempSync(join(tmpdir(), "agent-"));
  const script = join(workdir, "script.json"), record = join(workdir, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: "demo", turns: [SCRIPT] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  for await (const message of query({ prompt: "Add 2 and 3, divide 1 by 0, push, then list the files.", options: options(workdir) as any })) show(message);
  const lines = readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l));
  const argv: string[] = lines.find((l) => l.argv).argv;
  console.log();
  for (const flag of ["--tools", "--disallowedTools", "--max-turns", "--permission-mode"]) console.log(`flag ${flag} ${argv[argv.indexOf(flag) + 1]}`);
  const asked: Record<string, number> = {};
  for (const l of lines.filter((l) => l.ask)) asked[l.ask.subtype] = (asked[l.ask.subtype] ?? 0) + 1;
  console.log("what the binary asked your process:", `{${Object.keys(asked).sort().map((k) => `'${k}': ${asked[k]}`).join(", ")}}`);
}

if (import.meta.main) await main();
```
```text
init: the binary reports tools ['Read', 'Bash']
claude says: I will add, then divide.
claude calls: mcp__calc__add {"a":2,"b":3}
  result: ok    Sum: 5
claude calls: mcp__calc__divide {"a":1,"b":0}
  result: ERROR Cannot divide by zero
claude calls: Bash {"command":"git push origin main"}
  result: ERROR Permission denied: Nothing is pushed from an agent
claude calls: Bash {"command":"ls"}
  result: ok    README.md
claude says: Two sums, one refused push.
done: success, 5 turns, cost $0.02

flag --tools Read,Bash
flag --disallowedTools Bash(rm *)
flag --max-turns 6
flag --permission-mode default
what the binary asked your process: {'can_use_tool': 3, 'hook_callback': 2, 'mcp_message': 6}
```
<!-- /example -->

Read the output as a trace of who decided. The init line shows the tools the binary reported: only `Read` and `Bash`, because the options set `tools`, and the custom tools arrive through the MCP server. The two `mcp__calc__` calls reach your handlers over `mcp_message` requests: the first returns `Sum: 5`, and the second returns an error result with the message that you wrote, `Cannot divide by zero`, and the run continues. The push never reaches the callback. The hook runs first and denies it with the reason `Nothing is pushed from an agent`, which is the text of your hook. The prefix `Permission denied:` in front of it is the stand-in's wording and not the real binary's. The `ls` goes through the callback, which allows it.

The last lines show the flags that the options became (`--tools Read,Bash`, `--disallowedTools Bash(rm *)`, `--max-turns 6`, `--permission-mode default`) and what the binary asked your process: three permission questions (the two custom tools and `ls`, but not the push, which the hook had already decided), two hook callbacks and six MCP messages. The MCP count is the stand-in's own sequence of an initialize, a notification and a tool call for each custom tool, so it is not a number to expect from the real binary. The options allow six turns, and the run ends in success after five. Both languages produce the same output.

### The practice: an agent that may read, but not do harm

The practice is in `exercises/35-the-claude-agent-sdk/unit-01/practice-1/statement.md`, in Python and TypeScript. There is no Java or Kotlin edition of the SDK, so the practice has none. The tests never call a model: they start the stand-in through the SDK's own path option, so the SDK code that you call is the real one.

You write the policy as plain data first: `decide` for a tool call, in a read-only mode or an edit mode. It keeps file tools inside the project (a sibling folder whose name merely starts like the project's is outside), refuses secrets such as `.env` but not `.env.example`, keeps edits out of `.git`, limits `Bash` to `ls`, `cat` and `pytest` with no chaining or redirection, and treats `sudo` and `rm -rf` as dangerous: the denial interrupts the whole run. Then you wrap it: `make_can_use_tool` for the callback, a `PreToolUse` hook that denies `git push` as a whole word, `build_options` with no pre-approved tools, `Bash(rm *)` disallowed, six turns, a half-dollar budget and an explicit permission mode, `summarize` to fold the messages of a run into a status, the text, the tools used, the turns, the cost and the denials, and `run_agent`, which keeps the summary when a single-shot query raises after an error result and does not hide a crash that has no result before it.

The tests grade the flags that reach the binary, the file and shell policy, the hook running before the callback, the summary, the two kinds of error, and the count of denied calls. The starter fails all eight cases, the reference passes them, and each of the ten planted wrong solutions fails on an assertion of the case it breaks, for example an auto-approving callback, an edit allowed in read-only mode, a push check by substring, a missing turn limit, or a summary that raises after an error result.

## Traps

1. **Treating the stand-in as the binary.** It models the documented rules and its wording is its own. Use it to test your code's behaviour, and check the real binary's messages against the documentation.
2. **Testing only the policy function.** `decide` can be right while `build_options` leaves an allow list in, or the hook is not registered. Run the SDK against the stand-in so the whole path is exercised.
3. **Hiding a crash.** An error result followed by a raise is an expected end. A crash with no result is a failure, and swallowing it makes a dead run look like an empty one.
4. **Counting on a default mode.** Set the permission mode in the options and read it back from the flags in a test.

## Quiz

1. A team wants to test its permission policy without a model. Which part of the run can it replace, according to the page?
   - **a**: The executable that the SDK starts, through its path option
   - **b**: The control messages, since the policy never sees them
   - **c**: The prompt text, because the policy ignores what is asked
   - **d**: The SDK library, since its code holds the policy

2. In the example, the git push never reaches the permission callback. Why?
   - **a**: The hook runs first and denies it, so no later step is consulted
   - **b**: The stand-in hides every Bash call that mentions a remote
   - **c**: The turn limit stops the run before the push can be asked
   - **d**: The callback allows only the list command, so the push is skipped

3. The example's output lists six MCP messages for two custom tool calls. What does the page say that number is?
   - **a**: It is the number of messages that the real binary sends for two tools
   - **b**: It is a protocol limit of three messages for each tool
   - **c**: It is the sequence that the stand-in itself produces
   - **d**: It is a count measured on the real binary and recorded here

<details>
<summary>Answer key</summary>

1. **a**. The page says both SDKs let you choose the executable that they start, `cli_path` in Python and `pathToClaudeCodeExecutable` in TypeScript, so the SDK code runs for real. *d* is ruled out because "the SDK code in your program runs for real", which is the point of the method. *b* is ruled out because "the binary sends control requests back when it needs your process", and the SDK answers them, so they belong to the code under test. *c* is ruled out because "A script, a JSON file named in an environment variable, lists the steps of a run", so what is replaced is the binary behind the script and not the prompt.
2. **a**. The page says "The push never reaches the callback. The hook runs first and denies it", which follows the order of page 2. *d* is ruled out because the callback is asked about three calls (the two custom tools and the list command), "but not the push, which the hook had already decided". *b* is ruled out because the stand-in applies "a simplified copy of the documented behaviour", and its hook step is the SDK's registered hook, not a filter on remotes. *c* is ruled out because the options allow six turns and "the run ends in success after five".
3. **c**. The page says "The MCP count is the stand-in's own sequence of an initialize, a notification and a tool call for each custom tool". *a* is ruled out because the count is "not a number to expect from the real binary". *b* is ruled out because the three messages are "an initialize, a notification and a tool call for each custom tool", the stand-in's own sequence and not a limit of the protocol. *d* is ruled out because the page says "It is not the real binary, and nothing here shows how the real binary words its messages or schedules its calls."

</details>

## Module quiz

This quiz covers all three pages of the module.

1. A team writes its services in Java and wants the same agent loop. What does the documentation offer?
   - **a**: A Java edition of the SDK, installed from Maven
   - **b**: Run the CLI as a subprocess in headless mode with JSON output
   - **c**: Managed Agents, which expose the loop as a Java library
   - **d**: The Client SDK's tool runner, which is the same loop as the Agent SDK

2. In the example output a denied push appears as "Permission denied: Nothing is pushed from an agent". Which part is written by the example's own hook and not by the stand-in's code?
   - **a**: The words Permission denied, before the colon
   - **b**: The whole line, from the first word to the last
   - **c**: The reason that follows the colon
   - **d**: No part of the line, which is copied from the real binary

3. An agent in production must not run unbounded. Which two limits does the SDK offer?
   - **a**: A cap on tokens for each message and a cap on wall-clock time
   - **b**: A cap on files read and a cap on files written
   - **c**: A cap on hooks and a cap on tools for each turn
   - **d**: A cap on tool-use turns and a cap on spend in dollars

4. A CI job uses bypassPermissions and also lists Read in allowed_tools, hoping to keep the agent read-only. Which fix does the documentation give?
   - **a**: Nothing is needed, since the allowed list already limits the agent
   - **b**: Block the unwanted ones with deny rules
   - **c**: Move the list into the callback, which is consulted first
   - **d**: Switch to the default mode, which makes the list exclusive

<details>
<summary>Answer key</summary>

1. **b**. The first page says the documentation's route for other languages is to run the CLI as a subprocess, with the `-p` flag and `--output-format json`. *a* is ruled out because the SDK is "programmable in Python and TypeScript", and there is no Java edition. *d* is ruled out because for the Client SDK "You write the tool loop yourself, or let the client SDK's beta tool runner drive it", which is not the Agent SDK's loop. *c* is ruled out because Managed Agents are "A hosted agent harness that runs the agent loop, with sessions in an Anthropic-managed cloud sandbox", and not a library.
2. **c**. The third page says the reason "is the text of your hook". *a* is ruled out because "The prefix Permission denied: in front of it is the stand-in's wording and not the real binary's." *b* is ruled out because part of the line, the reason, is "which is the text of your hook", so the line is not all the stand-in's. *d* is ruled out because "It is not the real binary, and nothing here shows how the real binary words its messages or schedules its calls."
3. **d**. The first page lists `max_turns` ("Maximum tool-use round trips") and `max_budget_usd` ("Maximum cost before stopping"). *a* is ruled out because the two options are "Maximum tool-use round trips" and "Maximum cost before stopping", with no per-message token cap. *b* is ruled out because `max_turns` "counts tool-use turns only", and no file cap exists. *c* is ruled out because without limits "the loop runs until Claude finishes on its own", and hooks and tools are not what the two options count.
4. **b**. The second page says "allowed_tools does not constrain bypassPermissions", and that the way to block specific tools in that mode is deny rules. *a* is ruled out because "allowed_tools does not constrain bypassPermissions." *d* is ruled out because "This does not restrict Claude to only these tools", in any mode. *c* is ruled out because "Auto-approved tools never reach canUseTool", so the callback does not see them.

</details>
