# The Agent tool, agent definitions and the permission of a spawn

**Level:** Architect · **Module 47:** Invoking subagents · **Page 1 of 2**
**Exams:** A1.3

**After this page you can** say how a subagent is started in the Agent SDK (the `Agent` tool, an agent definition, and who decides when to call it), write definitions whose tools and description do their job, explain how a subagent's permissions follow from its parent's, recognise a spawn in the message stream under both tool names, and name what a Java or Kotlin team uses in place of the SDK.

Checked on 2026-10-03 against the Claude Code documentation pages "Subagents in the SDK", "Configure permissions", "Intercept and control agent behavior with hooks" and "Subagents in Claude Code": Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287, the versions the course ran. The example ran offline through the course's scripted stand-in for the Claude Code binary (`harness/fake_claude.py`), which speaks the SDK's protocol and replays a script: no model was called, no network was used and no API key was involved. The stand-in is not the real binary, and it orders the messages of a subagent in its own way. This page deepens module 35 (the SDK), module 39 (subagent files) and module 46 (the design of the team).

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* subagents are spawned with the Task tool, and `allowedTools` has to include Task for a coordinator to invoke them; a subagent definition holds a description, a system prompt and tool restrictions; and fork-based session management belongs to the same task statement (module 51 treats forking). *What the current product does (documentation checked 2026-10-03, Claude Code v2.1.x):* the tool is `Agent`, and `Task` is kept as the name in the init tools list and in blocks before v2.1.63. `allowed_tools` adds allow rules, so a listed tool is auto-approved; a tool that is not listed still exists, and the permissions page counts `Agent` among the tools "that don't ask before running", so in the default mode a spawn does not wait for an approval whether or not it is listed. What takes the ability away is the tool list of the agent or `disallowed_tools`. On the exam, keep the guide's answer when a question asks what must be in place for the coordinator to spawn (list the spawn tool in `allowedTools`); in practice, list `Agent` to state the intent, and look first at the description and the prompt when a coordinator never delegates.

## Why it matters

The coordinator of module 46 is a decision. This module is how the decision is carried out in the SDK, and the exam asks about the mechanism: how a subagent gets started, what you must pass it, what it may do, how you see it in the stream, and what changes when several run at once. The mechanism has details that decide whether a team is safe: a definition with no `tools` list, a parent in `bypassPermissions`, a spawn that you cannot recognise because you matched the wrong tool name.

## The idea

### One tool starts a subagent

"Claude invokes subagents through the Agent tool." That sentence is the whole mechanism. A subagent is not a function you call from your code. You describe the subagents that may exist, and the model, in its turn, calls a tool named `Agent` with an input that names the one it wants, gives a short description and holds the prompt. The SDK starts a separate agent instance for that call, and its final message comes back as the tool result. Everything in module 45 still applies: the call is a `tool_use` block, the result is a `tool_result`, and the coordinator's loop continues.

Where the definitions come from:

| Source | How | When |
|---|---|---|
| **Programmatic** | The `agents` option of `query()` | The recommended way for SDK applications; the definition can be built at run time |
| **Filesystem** | Markdown files in `.claude/agents/` | Shared with Claude Code users; a programmatic agent of the same name takes precedence |
| **Built in** | The `general-purpose` subagent | Always there: "When Claude calls the Agent tool without a `subagent_type`, it gets the built-in `general-purpose` subagent" |

Setting `CLAUDE_AGENT_SDK_DISABLE_BUILTIN_AGENTS=1` removes the built-in agent, and such a call then fails with `subagent_type is required`. A team that must only ever use its own named, restricted subagents sets it, because the general-purpose agent has every tool.

**Seeing a spawn.** The tool appears as Agent in tool_use blocks but as Task in the system:init tools list, and before Claude Code v2.1.63 the tool_use blocks also named it Task. So code that looks for spawns must match both names, and a group of messages belongs to a subagent when it carries that spawn's id as `parent_tool_use_id`: "Messages from within a subagent's context include a `parent_tool_use_id` field." The practice does both.

### The definition

An agent definition has a short list of fields that matter:

| Field | Required | What it does |
|---|---|---|
| `description` | Yes | "Natural language description of when to use this agent": the text the model reads to decide whether to delegate |
| `prompt` | Yes | The subagent's system prompt: its role and behaviour |
| `tools` | No | The tools it may use. "If omitted, inherits every tool available to subagents" |
| `disallowedTools` | No | Tools removed from its set; an `mcp__server__*` pattern removes a whole server |
| `model` | No | An alias (`sonnet`, `opus`, `haiku`, `fable`), `inherit` or a full id |
| `maxTurns` | No | Turns before it stops; the output is then marked partial, and the agent can be resumed |
| `background` | No | Force a non-blocking run |
| `permissionMode` | No | A mode for its tool calls, subject to the inheritance rule below |

Three decisions in a definition carry most of the risk.

1. **List the tools.** "A tool you leave out isn't in the subagent's session at all: Claude works without it, with no permission prompt or error." So a definition with no `tools` list is not a read-only agent. It has every tool a subagent can have. Give a reviewer `Read`, `Grep` and `Glob`, give a test runner `Bash`, and let the restriction be a property of the definition and not of a prompt that asks the subagent to behave. The practice defaults to the read-only set.
2. **Write the description for the model, not for people.** "Claude automatically decides when to invoke subagents based on the task and each subagent's `description`." A description that says when to use the agent ("Reviews one module for security problems. Use for any review request.") gets used; "helper" does not. When Claude does not delegate, the documentation gives two fixes: "mention it by name in your prompt", as in "Use the code-reviewer agent to check the authentication module", which "bypasses automatic matching and directly invokes the named subagent", and a clearer description.
3. **Do not hand the right to spawn down.** A subagent can spawn subagents of its own. The practice removes `Agent` from every subagent's list and sets the nesting depth to one, so that only the coordinator decides how large the team is.

Definitions can also be built when the query is made: "Key insight: use a more capable model for high-stakes reviews" is the documentation's own comment on a factory function that returns a stricter definition on a more capable model for a strict review. The definition is data, so a function can choose it.

### Permission follows the parent

A spawn is a tool call, so it goes through the evaluation of module 35: hooks, deny rules, ask rules, the permission mode, allow rules and then the `canUseTool` callback. The permissions page notes that `Agent` is among the tools that "don't ask before running", so a spawn needs no approval of its own in the default mode, and listing `Agent` in `allowed_tools`, as the documentation's examples do, makes the intent explicit. What needs thought is what the subagent may then do.

"A subagent runs in the parent session's permission mode unless you set `permissionMode` on its `AgentDefinition` and the parent session is in `default`, `dontAsk`, or `plan` mode. Even then, Claude Code never applies a `"bypassPermissions"` value. A subagent runs in `bypassPermissions` mode only when the parent session itself does." Read it as two rules. A definition can tighten a subagent's mode only when the parent is in a mode that allows it, and a definition can never loosen it to bypass. And a parent in `bypassPermissions` gives every subagent full autonomy, which the page spells out: "Subagents may have different system prompts and less constrained behavior than your main agent, so inheriting `bypassPermissions` grants them full, autonomous system access." An architect who runs the coordinator in a bypass mode for CI has also decided that for every subagent. Module 35's warning applies to the whole tree: "allowed_tools does not constrain bypassPermissions", so a bypass parent approves every tool that reaches the mode step. The controls that still hold are the ones evaluated first, deny rules, ask rules and hooks, and the subagent's own tool list, because a tool that is not in its session cannot be called at all.

Hooks fire inside subagents too. A hook input has an `agent_id` and an `agent_type` "when the hook fires inside a subagent", and the `SubagentStart` and `SubagentStop` events exist to "track parallel task spawning" and "aggregate results from parallel tasks" (module 49). One practical effect: each subagent may ask for permission on its own, so "subagent permission prompts multiplying" is a known problem, and the documented cure is a `PreToolUse` hook that auto-approves specific tools, or permission rules, which subagents "inherit from the parent conversation".

### What Java and Kotlin teams use

The SDK is Python and TypeScript only. For another language the documentation's route is to run Claude Code as a subprocess in headless mode with `-p` and `--output-format json` (module 35), and the definition of the subagents then lives in `.claude/agents/` files that the subprocess loads. A team that wants the pattern without the binary builds the coordinator on the Messages API, as module 46's example does, and the practices of module 46 are in all four languages for that reason. The practice of this module is in Python and TypeScript only.

### The example

The example runs the real SDK against the stand-in. It registers two agents (a reviewer with two read tools and a finder with three), starts a scripted run in which the coordinator calls `Agent` for the reviewer and the reviewer reads and searches, and prints what the stream and the handshake show.

<!-- example: m47-subagent-invocation tabs: python,typescript -->
```python
"""Invoking a subagent with the Agent SDK, offline: the definitions sent to the binary, the spawn in the stream and the messages that ran inside it.

The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is
used. The stand-in puts the messages of the subagent after the call that started it, which the real binary may order differently. `claude-agent-sdk`
0.2.163, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
"""
import asyncio
import json
import os
import tempfile
from pathlib import Path

from claude_agent_sdk import AgentDefinition, AssistantMessage, ClaudeAgentOptions, ResultMessage, TextBlock, ToolResultBlock, ToolUseBlock, UserMessage, query

FAKE = str(Path(__file__).resolve().parents[3] / "harness" / "fake_claude.py")

AGENTS = {
    "reviewer": AgentDefinition(description="Reviews one module for security problems. Use for any review request.", prompt="You review code and report findings only.",
                                tools=["Read", "Grep"], model="sonnet"),
    "finder": AgentDefinition(description="Finds the files that touch a feature.", prompt="You list files.", tools=["Read", "Grep", "Glob"]),
}

BRIEF = "Task: Review auth.py for injection risks\nFiles:\n- src/auth.py\nKnown:\n- the login query is built with string formatting\nReturn: findings, one line each"

SCRIPT = [{"say": "I will delegate the review."},
          {"tool": {"id": "t1", "name": "Agent", "input": {"subagent_type": "reviewer", "description": "Review auth.py", "prompt": BRIEF}, "output": "1 finding: string-built query in login()."}},
          {"tool": {"id": "t2", "name": "Read", "input": {"file_path": "src/auth.py"}, "output": "def login(): ...", "parent": "t1"}},
          {"tool": {"id": "t3", "name": "Grep", "input": {"pattern": "execute("}, "output": "src/auth.py:12", "parent": "t1"}},
          {"say": "One finding, from the reviewer."},
          {"result": {"subtype": "success", "result": "One finding, from the reviewer.", "cost": 0.04, "turns": 3}}]


def describe(message):
    """One line per interesting message: a spawn, a message from inside a subagent, a report, the result."""
    lines = []
    inside = getattr(message, "parent_tool_use_id", None)
    if isinstance(message, AssistantMessage):
        for block in message.content:
            if isinstance(block, ToolUseBlock) and block.name in ("Agent", "Task"):
                lines.append(f"spawn: {block.name} -> {block.input['subagent_type']}, brief of {len(block.input['prompt'].splitlines())} lines")
            elif isinstance(block, ToolUseBlock):
                lines.append(f"  inside {inside}: {block.name} {json.dumps(block.input, separators=(',', ':'))}" if inside else f"coordinator calls {block.name}")
            elif isinstance(block, TextBlock):
                lines.append(f"coordinator says: {block.text}")
    elif isinstance(message, UserMessage):
        for block in message.content:
            if isinstance(block, ToolResultBlock) and not inside:
                lines.append(f"report to the coordinator: {block.content}")
    elif isinstance(message, ResultMessage):
        lines.append(f"done: {message.subtype}, cost ${message.total_cost_usd:.2f}")
    return lines


async def main():
    workdir = tempfile.mkdtemp()
    script, record = Path(workdir, "script.json"), Path(workdir, "record.jsonl")
    script.write_text(json.dumps({"session_id": "demo", "turns": [SCRIPT]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    options = ClaudeAgentOptions(cli_path=FAKE, cwd=workdir, agents=AGENTS, allowed_tools=["Agent", "Read", "Grep", "Glob"], max_budget_usd=0.5, max_turns=10,
                                 permission_mode="default", setting_sources=[], env={"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1"})
    async for message in query(prompt="Review auth.py", options=options):
        for line in describe(message):
            print(line)
    lines = [json.loads(line) for line in record.read_text().splitlines()]
    sent = next(line["agents"] for line in lines if "agents" in line)
    print()
    for name in sorted(sent):
        print(f"agent sent to the binary: {name} tools={sent[name].get('tools')} model={sent[name].get('model', 'not set')}")
    argv = next(line["argv"] for line in lines if "argv" in line)
    for flag in ("--allowedTools", "--max-budget-usd", "--max-turns"):
        print(f"flag {flag} {argv[argv.index(flag) + 1]}")


if __name__ == "__main__":
    asyncio.run(main())
```
```text
coordinator says: I will delegate the review.
spawn: Agent -> reviewer, brief of 6 lines
report to the coordinator: 1 finding: string-built query in login().
  inside t1: Read {"file_path":"src/auth.py"}
  inside t1: Grep {"pattern":"execute("}
coordinator says: One finding, from the reviewer.
done: success, cost $0.04

agent sent to the binary: finder tools=['Read', 'Grep', 'Glob'] model=not set
agent sent to the binary: reviewer tools=['Read', 'Grep'] model=sonnet
flag --allowedTools Agent,Read,Grep,Glob
flag --max-budget-usd 0.5
flag --max-turns 10
```
```typescript
// Invoking a subagent with the Agent SDK, offline: the definitions sent to the binary, the spawn in the stream and the messages that ran inside it.
//
// The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is
// used. The stand-in puts the messages of the subagent after the call that started it, which the real binary may order differently.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";

export const FAKE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;

export const AGENTS = {
  reviewer: { description: "Reviews one module for security problems. Use for any review request.", prompt: "You review code and report findings only.", tools: ["Read", "Grep"], model: "sonnet" },
  finder: { description: "Finds the files that touch a feature.", prompt: "You list files.", tools: ["Read", "Grep", "Glob"] },
};

export const BRIEF = "Task: Review auth.py for injection risks\nFiles:\n- src/auth.py\nKnown:\n- the login query is built with string formatting\nReturn: findings, one line each";

const SCRIPT = [{ say: "I will delegate the review." },
  { tool: { id: "t1", name: "Agent", input: { subagent_type: "reviewer", description: "Review auth.py", prompt: BRIEF }, output: "1 finding: string-built query in login()." } },
  { tool: { id: "t2", name: "Read", input: { file_path: "src/auth.py" }, output: "def login(): ...", parent: "t1" } },
  { tool: { id: "t3", name: "Grep", input: { pattern: "execute(" }, output: "src/auth.py:12", parent: "t1" } },
  { say: "One finding, from the reviewer." },
  { result: { subtype: "success", result: "One finding, from the reviewer.", cost: 0.04, turns: 3 } }];

/** One line per interesting message: a spawn, a message from inside a subagent, a report, the result. */
export function describe(message: any): string[] {
  const lines: string[] = [];
  const inside = message.parent_tool_use_id;
  if (message.type === "assistant") {
    for (const block of message.message.content) {
      if (block.type === "tool_use" && (block.name === "Agent" || block.name === "Task")) lines.push(`spawn: ${block.name} -> ${block.input.subagent_type}, brief of ${block.input.prompt.split("\n").length} lines`);
      else if (block.type === "tool_use") lines.push(inside ? `  inside ${inside}: ${block.name} ${JSON.stringify(block.input)}` : `coordinator calls ${block.name}`);
      else if (block.type === "text") lines.push(`coordinator says: ${block.text}`);
    }
  } else if (message.type === "user" && Array.isArray(message.message.content)) {
    for (const block of message.message.content) if (block.type === "tool_result" && !inside) lines.push(`report to the coordinator: ${block.content}`);
  } else if (message.type === "result") {
    lines.push(`done: ${message.subtype}, cost $${message.total_cost_usd.toFixed(2)}`);
  }
  return lines;
}

async function main() {
  const workdir = mkdtempSync(join(tmpdir(), "team-"));
  const script = join(workdir, "script.json"), record = join(workdir, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: "demo", turns: [SCRIPT] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  const options = {
    pathToClaudeCodeExecutable: FAKE, cwd: workdir, agents: AGENTS, allowedTools: ["Agent", "Read", "Grep", "Glob"], maxBudgetUsd: 0.5, maxTurns: 10,
    permissionMode: "default" as const, settingSources: [] as ("user" | "project" | "local")[], env: { ...process.env, CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH: "1" },
  };
  for await (const message of query({ prompt: "Review auth.py", options: options as any })) for (const line of describe(message)) console.log(line);
  const lines = readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l));
  const sent = lines.find((l) => l.agents).agents;
  const py = (v: unknown) => (Array.isArray(v) ? `[${v.map((x) => `'${x}'`).join(", ")}]` : v === undefined ? "None" : String(v));
  console.log();
  for (const name of Object.keys(sent).sort()) console.log(`agent sent to the binary: ${name} tools=${py(sent[name].tools)} model=${sent[name].model ?? "not set"}`);
  const argv: string[] = lines.find((l) => l.argv).argv;
  for (const flag of ["--allowedTools", "--max-budget-usd", "--max-turns"]) console.log(`flag ${flag} ${argv[argv.indexOf(flag) + 1]}`);
}

if (import.meta.main) await main();
```
```text
coordinator says: I will delegate the review.
spawn: Agent -> reviewer, brief of 6 lines
report to the coordinator: 1 finding: string-built query in login().
  inside t1: Read {"file_path":"src/auth.py"}
  inside t1: Grep {"pattern":"execute("}
coordinator says: One finding, from the reviewer.
done: success, cost $0.04

agent sent to the binary: finder tools=['Read', 'Grep', 'Glob'] model=not set
agent sent to the binary: reviewer tools=['Read', 'Grep'] model=sonnet
flag --allowedTools Agent,Read,Grep,Glob
flag --max-budget-usd 0.5
flag --max-turns 10
```
<!-- /example -->

Read it from the top. The spawn line shows the tool name `Agent`, the subagent type and the size of the brief (six lines: task, files, known facts and the return format, the layout of page 2). The report line is the subagent's final message arriving as a tool result for the coordinator. The two lines marked `inside t1` are messages that ran inside the reviewer, tagged with the id of the spawn. The `agent sent to the binary` lines are the definitions as the SDK sent them in its handshake: the reviewer with two tools and a model, the finder with three tools and no model set. The flags show the auto-approved tools, the budget and the turn limit that reached the binary. The stand-in puts the inner messages after the report, which the real binary may not, so the example shows how to read the tags and not the timing. Both languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"A subagent with no tool list is safe if its prompt says to be careful."** It is tempting because the prompt is easy to write. The exam rejects it: the subagent inherits every tool available to subagents, including the ones that write and run commands. List the tools; a restriction is a property of the definition.
2. **"Look for the Task tool to see whether the coordinator delegated."** It is tempting because the guide uses that name. The exam keys the guide's name, and the product moved on: spawns appear as `Agent`, and as `Task` before v2.1.63 and in the init tools list. Match both, or an older run looks as if it never delegated.
3. **"Run the parent in bypass mode, because each subagent's definition limits it."** It is tempting because the definition has a `permissionMode` field. The exam rejects it: a definition can never set bypass, but a bypass parent passes it down. The controls that still hold are deny rules, ask rules and hooks.
4. **"If a subagent is never used, strengthen the coordinator's system prompt."** It is tempting because the prompt is the visible lever. The exam rejects it: the subagent's description is what the model reads to decide, so write it as when to use the agent, or name the subagent in the prompt.

## Quiz

1. A team gives its security-review subagent the one-word blurb "Helper." The coordinator keeps reviewing code itself and never delegates. Which change makes delegation likely?
   - **a**: Rewrite the description to state when this specialist should be called
   - **b**: Raise the agent's turn limit so that starting it is worth the extra effort
   - **c**: Move the review instructions from the description into the agent's own prompt
   - **d**: Set the nesting depth to three so that the model is allowed to delegate at all

2. A dashboard counts delegations by looking for calls to the Agent tool in the message stream. Replayed logs from earlier runs show subagent reports, yet the dashboard reports zero delegations. What fixes it?
   - **a**: Read the tools list of the init message, where the tool is always named Agent
   - **b**: Look for the subagent's name in the assistant's text blocks
   - **c**: Match the former name Task in the call blocks too, besides the current one
   - **d**: Add Agent to the allowed tools so that its calls show up in the stream

<details>
<summary>Answer key</summary>

1. **a**. The model reads the description to decide whether to delegate, so it must say when the agent applies. *b* is ruled out because a turn limit only bounds a run that has started: "Turns before it stops; the output is then marked partial, and the agent can be resumed". *c* is ruled out because the prompt is the agent's own role, while the description is "Natural language description of when to use this agent". *d* is ruled out because nesting concerns subagents that spawn their own: "The practice removes `Agent` from every subagent's list and sets the nesting depth to one, so that only the coordinator decides how large the team is."
2. **c**. Older versions named the tool Task in the call blocks, so both names must be matched. *b* is ruled out because a spawn is a tool call, not text: the model "calls a tool named `Agent` with an input that names the one it wants". *a* is ruled out because the two lists differ: the tool "appears as Agent in tool_use blocks but as Task in the system:init tools list". *d* is ruled out because the entry does not change what is logged: a spawn needs no approval of its own, and listing it "makes the intent explicit".

</details>
