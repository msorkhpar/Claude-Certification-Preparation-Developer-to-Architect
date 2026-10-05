# Continue, resume and fork: what a session keeps

**Level:** Architect · **Module 51:** Session state · **Page 1 of 2**
**Exams:** A1.7; S4

**After this page you can** say what a session holds and what it does not, choose between continue, resume and fork for a given return to earlier work, read the session id from a result and keep it, name what a resumed session restores and what you must pass again, and state what each way of going back costs.

Checked on 2026-10-03 against the Claude Code documentation pages "Work with sessions" (the Agent SDK page), "Sessions" (the command-line page) and the CLI reference, with Python `claude-agent-sdk` 0.2.163 and TypeScript `@anthropic-ai/claude-agent-sdk` 0.3.287, the versions the course ran. The example runs offline through the course's scripted stand-in for the Claude Code binary (`harness/fake_claude.py`): no model was called, no network was used and no API key was involved. The stand-in invents the session ids it reports and does not store sessions; what the example shows is the flags that the real SDK code builds. This page deepens module 35 (sessions in the SDK), module 38 (sessions in Claude Code) and module 45 (the context that accumulates in a loop), and it does not repeat them.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* it names two controls, continuing a named investigation with `--resume <session-name>` and `fork_session` for branching a shared analysis into independent explorations, as if they were two separate actions. *What the current product does (documentation checked 2026-10-03):* forking is not a separate action. It is an option that is set together with a resume: in the SDK `resume` plus `fork_session` (TypeScript `forkSession`), on the command line `--fork-session` "use with `--resume` or `--continue`", and inside a session `/branch`. Without it a resume appends to the original; with it the original stays unchanged and the new session has its own id. Names work on the command line (`claude --resume <name>`, set with `--name` or `/rename`), while the SDK guide has you "Capture the session ID and pass it to `resume`". On the exam, choose resume plus fork for independent branches from one baseline and a named resume for returning to one investigation; in your own code, keep the id from the result message.

## Why it matters

Scenario S4 builds an agent that explores unfamiliar codebases, and an exploration is rarely finished in one sitting. On Monday the agent maps a payments module; on Tuesday the team wants to compare two ways of changing it; on Wednesday someone has to return to the Monday thread. Three different needs, three different controls, and each has a price. The exam asks which one fits, and the common mistake is to treat "go back to the old conversation" as a single action, so that a team forks when it meant to resume or resumes when it meant to branch, and then wonders why two approaches have been written into one history.

## The idea

### What a session holds

The SDK guide defines it in one sentence: "A session is the conversation history the SDK accumulates while your agent works." That is the prompt, every tool call, every tool result and every response, written to disk automatically so that you can return to it. Returning gives the agent "files it already read, analysis it already performed, decisions it already made". The limit is in the next note of the guide: "Sessions persist the conversation, not the filesystem." The conversation comes back as it was; the files do not. Whatever happened to the code in between is not in the session (page 2 is about that).

### Continue, resume and fork

These are three option fields on the same call (`ClaudeAgentOptions` in Python, `Options` in TypeScript). The guide separates two ways of picking an existing session and one way of leaving it.

| Control | Python | TypeScript | What it does |
|---|---|---|---|
| Continue | `continue_conversation=True` | `continue: true` | Picks up the most recent session in the current directory and adds to it |
| Resume | `resume=<id>` | `resume: <id>` | Picks up the session with that id and adds to it |
| Fork | `resume=<id>` with `fork_session=True` | `resume: <id>` with `forkSession: true` | Starts a new session from a copy of that session's history |

In the guide's words: "**Continue** finds the most recent session in the current directory. You don't track anything. Works well when your app runs one conversation at a time." And: "**Resume** takes a specific session ID. You track the ID. Required when you have multiple sessions (for example, one per user in a multi-user app) or want to return to one that isn't the most recent." "**Continue** and **resume** both pick up an existing session and add to it." Of a fork it says: "**Fork** is different: it creates a new session that starts with a copy of the original's history. The original stays unchanged." The new session "gets its own session ID", so afterwards there are two independent sessions that can each be resumed.

Python also has `ClaudeSDKClient`, which holds the session id inside the object so that each `client.query()` continues the same conversation; TypeScript has no client object and passes `continue: true` on later calls. Both are for several prompts in one process. They are not a way to find an older session.

The id comes from the result. The guide says to read the `session_id` field on the result message, "which is present on every result regardless of success or error", and a single-shot `query()` raises after an error result, so catch the error and keep the id, which is what lets you resume a run that stopped at `error_max_turns` with a higher limit. A fork's id is the one on the new run's messages, and it differs from the base id; resuming the base afterwards continues the original thread.

### Names and ids

On the command line a session can be named: `claude --name my-feature-work`, or `/rename auth-refactor` during a session, and `claude --resume auth-refactor` "Resumes the named session directly". If two sessions share a name, the name leads to the picker with the name pre-filled, not to a session, so a script should not rely on a name being unique. In the SDK the guide's examples pass an id: "Capture the session ID and pass it to `resume`." A program that wants names keeps its own map from name to id, and refuses a name that matches none or several (the practice has you write it).

Sessions started by `claude -p` or the Agent SDK are left out of the session picker and out of `claude --continue`: "You can still resume one by passing its session ID to `claude --resume <session-id>`." So a team that runs the SDK in a service and wants an engineer to open one of its sessions in the terminal needs the id, not the picker.

### What each way of going back costs

| Way back | What you keep | What it costs |
|---|---|---|
| Resume (or continue) | The whole conversation | The full history is in every later request; after a long pause the prompt cache has expired, so the next request processes all of it again |
| Fork | The whole conversation, in a second copy | The same history again, in a new session; any file changes the fork makes are real |
| Fresh session with a summary | Only what you wrote down | A short prompt; whatever the summary leaves out is gone |

The documentation puts numbers on the second part for Claude Code on a Pro or Max plan. When a session "has been inactive for more than about an hour and is over 100,000 tokens", Claude Code opens a dialog before the first message, because "The session's prompt cache has expired by then, so the next request processes the full history once no matter which of the dialog's options you pick." The dialog offers a summary (it runs `/compact` at once, which replaces the history with a summary, the most recent exchanges and up to five recently read files) or the full session as it was. The page weighs them in one sentence each: resuming as-is "keeps every detail of the conversation available, at a per-request cost that scales with the conversation's size", and resuming from the summary "costs less on each later request because it carries the summary instead of the full history, but whatever the summary leaves out is no longer in Claude's context." That is the same trade that page 2 makes by hand with a summary you write.

A fork has a subtler cost. "Forking branches the conversation history, not the filesystem. If a forked agent edits files, those changes are real and visible to any session working in the same directory." Two forks that each try a refactor in the same working tree write over each other. For a comparison that must not collide, fork for the analysis and let each branch work in its own copy of the code (the guide points to file checkpointing for branching and reverting changes).

### What a resumed session restores, and what it does not

A resumed Claude Code session restores the conversation history, the agent it ran as and, for most terminal resumes, its permission mode. A tool that was still running when a process died "doesn't finish or run again when you resume. Claude sees the call marked as cut off before its result was recorded and is told to check whether it took effect before running it again": a good habit to copy in your own tools (module 53). What is not restored is configuration passed on the command line: "If the session depended on `--mcp-config`, `--settings`, `--plugin-dir`, `--fallback-model`, or directories added with `--add-dir`, pass them again when you resume". The settings files are read again at launch, so what lives in them needs no repeat. A resumed run that forgot `--mcp-config` has the old conversation and fewer tools, and that mismatch looks like a model failure.

### Where sessions live and how far they travel

Claude Code stores sessions under `~/.claude/projects/<encoded-cwd>/*.jsonl` and, as the guide puts it, "Session files are local to the machine that created them." You can resume from another directory on the same machine, since Claude Code searches beyond the current project for the id. To resume on another host (a CI worker, a container, a serverless function) the guide lists three routes: a session store adapter that mirrors transcripts to your own backend, moving the session file, or "Don't rely on session resume": "Capture the results you need (analysis output, decisions, file diffs) as application state and pass them into a fresh session's prompt." That is "often more robust than shipping transcript files around". For a task that must leave nothing on disk, TypeScript has `persistSession: false`; in Python the guide points to the `CLAUDE_CODE_SKIP_PROMPT_HISTORY` environment variable.

### What Java and Kotlin teams use

The Agent SDK is Python and TypeScript only. A Java or Kotlin team that wants these controls runs the Claude Code command line as a subprocess with `-p`, keeps the session id from its JSON output and passes `--resume <id>` (with `--fork-session` to branch) on the next run, or builds the loop on the Messages API and keeps the message list itself, as module 45 does. The practice of this module is in Python and TypeScript only.

### The example

The example sends five runs through the real SDK against the stand-in and prints the session flags the SDK put on the command line and the id each result carried. It then applies the plan of page 2 to four saved sessions.

<!-- example: m51-session-state tabs: python,typescript -->
```python
"""Session state, offline: what continue, resume and fork send to the binary, and when a saved session is worth resuming.

The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
The session ids come from the script (the stand-in does not store sessions); the flags are the ones the real SDK builds. `claude-agent-sdk` 0.2.163,
checked on 2026-10-03 against the "Work with sessions" page of the Claude Code documentation.
"""
import asyncio
import json
import os
import tempfile
from pathlib import Path

from claude_agent_sdk import ClaudeAgentOptions, ResultMessage, query
import logging

log = logging.getLogger(__name__)

FAKE = str(Path(__file__).resolve().parents[3] / "harness" / "fake_claude.py")
DAY = 24 * 3600


def decide(saved, current, idle_days):
    """resume, resume with a notice, or start fresh with a summary, from the files a session analysed and the files now."""
    changed = sorted(p for p in saved if p in current and saved[p] != current[p])
    gone = sorted(p for p in saved if p not in current)
    if (len(changed) + len(gone)) / len(saved) > 0.5 or idle_days > 7:
        return "start fresh with a summary", changed
    return ("resume with a notice", changed) if changed or gone else ("resume", changed)


def notice(changed):
    return "\n".join(["Since your earlier analysis:", f"- changed: {', '.join(changed)}",
                      "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."])


def flags(argv):
    """The session flags of one command line, in the order they appear."""
    out = []
    for i, arg in enumerate(argv):
        if arg == "--resume":
            out.append(f"--resume {argv[i + 1]}")
        elif arg.startswith("--resume="):  # the Python SDK writes the id after an equals sign
            out.append(f"--resume {arg.split('=', 1)[1]}")
        elif arg in ("--fork-session", "--continue"):
            out.append(arg)
    return ", ".join(out) or "none"


async def run(session_id, **options):
    """One single-shot run against the stand-in, scripted to report `session_id`; returns the id the result carries and the session flags sent."""
    folder = tempfile.mkdtemp()
    script, record = Path(folder, "script.json"), Path(folder, "record.jsonl")
    script.write_text(json.dumps({"session_id": session_id, "turns": [[{"say": "ok"}, {"result": {"subtype": "success", "result": "ok", "cost": 0.01, "turns": 1}}]]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    seen = None
    async for message in query(prompt="Continue the review", options=ClaudeAgentOptions(cli_path=FAKE, cwd=folder, setting_sources=[], **options)):
        if isinstance(message, ResultMessage):
            seen = message.session_id
    argv = next(json.loads(line)["argv"] for line in record.read_text().splitlines() if '"argv"' in line)
    return seen, flags(argv)


async def main():
    print("what each control sends to the binary")
    runs = [("new session", "s-auth-1", {}), ("resume s-auth-1", "s-auth-1", {"resume": "s-auth-1"}),
            ("resume s-auth-1 and fork", "s-auth-2", {"resume": "s-auth-1", "fork_session": True}),
            ("resume s-auth-1 again", "s-auth-1", {"resume": "s-auth-1"}), ("continue the latest", "s-auth-1", {"continue_conversation": True})]
    for label, scripted, options in runs:
        seen, sent = await run(scripted, **options)
        print(f"  {label:<26} -> session {seen}, flags: {sent}")
    saved = {"a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4"}
    print("\nwhat to do with a saved session (4 files analysed)")
    cases = [("nothing changed, idle 1 day", saved, 1), ("b.py changed", {**saved, "b.py": "x"}, 1),
             ("3 of 4 files changed", {"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4"}, 1), ("nothing changed, idle 8 days", saved, 8)]
    for label, current, idle in cases:
        print(f"  {label:<30} -> {decide(saved, current, idle)[0]}")
    print("\nthe notice for the second case")
    for line in notice(decide(saved, cases[1][1], 1)[1]).splitlines():
        print(f"  {line}")


if __name__ == "__main__":
    asyncio.run(main())
```
```text
what each control sends to the binary
  new session                -> session s-auth-1, flags: none
  resume s-auth-1            -> session s-auth-1, flags: --resume s-auth-1
  resume s-auth-1 and fork   -> session s-auth-2, flags: --resume s-auth-1, --fork-session
  resume s-auth-1 again      -> session s-auth-1, flags: --resume s-auth-1
  continue the latest        -> session s-auth-1, flags: --continue

what to do with a saved session (4 files analysed)
  nothing changed, idle 1 day    -> resume
  b.py changed                   -> resume with a notice
  3 of 4 files changed           -> start fresh with a summary
  nothing changed, idle 8 days   -> start fresh with a summary

the notice for the second case
  Since your earlier analysis:
  - changed: b.py
  Re-read these files before relying on earlier conclusions about them. Every other file is unchanged.
```
```typescript
// Session state, offline: what continue, resume and fork send to the binary, and when a saved session is worth resuming.
//
// The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
// The session ids come from the script (the stand-in does not store sessions); the flags are the ones the real SDK builds.
// `@anthropic-ai/claude-agent-sdk` 0.3.287, checked on 2026-10-03 against the "Work with sessions" page of the Claude Code documentation.
import { mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { query } from "@anthropic-ai/claude-agent-sdk";
import { logger } from "./logger.ts";
const log = logger("session_state");

const FAKE = new URL("../../../harness/fake_claude.py", import.meta.url).pathname;

/** resume, resume with a notice, or start fresh with a summary, from the files a session analysed and the files now. */
export function decide(saved: Record<string, string>, current: Record<string, string>, idleDays: number): [string, string[]] {
  const changed = Object.keys(saved).filter((p) => p in current && saved[p] !== current[p]).sort();
  const gone = Object.keys(saved).filter((p) => !(p in current)).sort();
  if ((changed.length + gone.length) / Object.keys(saved).length > 0.5 || idleDays > 7) return ["start fresh with a summary", changed];
  return changed.length || gone.length ? ["resume with a notice", changed] : ["resume", changed];
}

export function notice(changed: string[]): string {
  return ["Since your earlier analysis:", `- changed: ${changed.join(", ")}`,
    "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."].join("\n");
}

/** The session flags of one command line, in the order they appear. */
export function flags(argv: string[]): string {
  const out: string[] = [];
  argv.forEach((arg, i) => {
    if (arg === "--resume") out.push(`--resume ${argv[i + 1]}`);
    else if (arg.startsWith("--resume=")) out.push(`--resume ${arg.split("=")[1]}`); // the Python SDK writes the id after an equals sign
    else if (arg === "--fork-session" || arg === "--continue") out.push(arg);
  });
  return out.join(", ") || "none";
}

/** One single-shot run against the stand-in, scripted to report `sessionId`; returns the id the result carries and the session flags sent. */
async function run(sessionId: string, options: Record<string, unknown>): Promise<[string | undefined, string]> {
  const folder = mkdtempSync(join(tmpdir(), "session-"));
  const script = join(folder, "script.json"), record = join(folder, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: sessionId, turns: [[{ say: "ok" }, { result: { subtype: "success", result: "ok", cost: 0.01, turns: 1 } }]] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  let seen: string | undefined;
  const all = { pathToClaudeCodeExecutable: FAKE, cwd: folder, settingSources: [], env: { ...process.env }, ...options };
  for await (const message of query({ prompt: "Continue the review", options: all as any }) as AsyncIterable<any>) {
    if (message.type === "result") seen = message.session_id;
  }
  const line = readFileSync(record, "utf8").split("\n").find((l) => l.includes('"argv"')) as string;
  return [seen, flags(JSON.parse(line).argv)];
}

async function main() {
  console.log("what each control sends to the binary");
  const runs: Array<[string, string, Record<string, unknown>]> = [
    ["new session", "s-auth-1", {}], ["resume s-auth-1", "s-auth-1", { resume: "s-auth-1" }],
    ["resume s-auth-1 and fork", "s-auth-2", { resume: "s-auth-1", forkSession: true }],
    ["resume s-auth-1 again", "s-auth-1", { resume: "s-auth-1" }], ["continue the latest", "s-auth-1", { continue: true }],
  ];
  for (const [label, scripted, options] of runs) {
    const [seen, sent] = await run(scripted, options);
    console.log(`  ${label.padEnd(26)} -> session ${seen}, flags: ${sent}`);
  }
  const saved = { "a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4" };
  console.log("\nwhat to do with a saved session (4 files analysed)");
  const cases: Array<[string, Record<string, string>, number]> = [
    ["nothing changed, idle 1 day", saved, 1], ["b.py changed", { ...saved, "b.py": "x" }, 1],
    ["3 of 4 files changed", { "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4" }, 1], ["nothing changed, idle 8 days", saved, 8],
  ];
  for (const [label, current, idle] of cases) console.log(`  ${label.padEnd(30)} -> ${decide(saved, current, idle)[0]}`);
  console.log("\nthe notice for the second case");
  for (const line of notice(decide(saved, cases[1][1], 1)[1]).split("\n")) console.log(`  ${line}`);
}

if (import.meta.main) await main();
```
```text
what each control sends to the binary
  new session                -> session s-auth-1, flags: none
  resume s-auth-1            -> session s-auth-1, flags: --resume s-auth-1
  resume s-auth-1 and fork   -> session s-auth-2, flags: --resume s-auth-1, --fork-session
  resume s-auth-1 again      -> session s-auth-1, flags: --resume s-auth-1
  continue the latest        -> session s-auth-1, flags: --continue

what to do with a saved session (4 files analysed)
  nothing changed, idle 1 day    -> resume
  b.py changed                   -> resume with a notice
  3 of 4 files changed           -> start fresh with a summary
  nothing changed, idle 8 days   -> start fresh with a summary

the notice for the second case
  Since your earlier analysis:
  - changed: b.py
  Re-read these files before relying on earlier conclusions about them. Every other file is unchanged.
```
<!-- /example -->

Read the first block: a new session sends no session flag; a resume sends `--resume` with the id; the fork sends `--resume` and `--fork-session` together and its result carries a different id (scripted here, real in the product); resuming the base again continues the original; and `continue_conversation` sends `--continue`, which carries no id at all, so it can only mean "the latest".

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Fork the session to carry on the same line of work."** It is tempting because forking keeps the whole history. The exam rejects it: a fork creates a new session with its own id, so the line of work now exists twice. Resume to continue one thread; fork to branch it.
2. **"Use `continue` to go back to Monday's session."** It is tempting because the word means going on. The exam rejects it: continue finds the most recent session in the directory, and it is only safe where one conversation runs at a time. A specific earlier session needs its id.
3. **"Resume it, and the agent is back where the code was."** It is tempting because the conversation is complete. The exam rejects it: sessions persist the conversation, not the filesystem, and a fork's edits are real in the shared directory. Anything that must be undone needs file checkpointing, a copy of the tree or version control.
4. **"Pass the session name to the SDK's `resume` option."** It is tempting because the command line resumes by name. The exam rejects it: the SDK guide resumes by id, and the names of the command line are not unique. Keep the id from the result, and keep your own map of names.

## Quiz

1. A review agent finished mapping a payments module on Monday. On Tuesday the team wants to compare a rewrite with a refactor from that same baseline, and the Monday thread must stay usable. What should the SDK call carry?
   - **a**: `resume` with the saved id and `fork_session` enabled
   - **b**: `continue_conversation` alone, which branches the latest run
   - **c**: `resume` with the saved id, once for each approach
   - **d**: A new run whose first prompt only names the Monday thread

2. A multi-user app keeps one conversation per customer. After a restart it must return each customer to the right earlier conversation, not just the latest one. What does the code store for each customer?
   - **a**: A `continue` flag, with one working directory for each customer
   - **b**: A display name for each customer, passed to `resume` in the SDK
   - **c**: The session id from the last result message
   - **d**: A fork of the first session, created for each customer

<details>
<summary>Answer key</summary>

1. **a**. Fork is an option set together with a resume id, so it branches a copy and leaves the original usable. *b* is ruled out because "**Continue** finds the most recent session in the current directory", so it adds to that session and branches nothing. *c* is ruled out because "Continue and resume both pick up an existing session and add to it", which puts both approaches into one history. *d* is ruled out because a name carries none of the analysis, and the guide says to "Capture the results you need (analysis output, decisions, file diffs) as application state and pass them into a fresh session's prompt."
2. **c**. Resume needs a specific id, and the result carries it on every result. *b* is ruled out because "Resume takes a specific session ID", not a display name; the guide's `--resume <session-name>` is a command-line form, and the SDK guide passes an id. *a* is ruled out because "**Continue** finds the most recent session in the current directory", so a customer would get the latest conversation only and never an earlier one. *d* is ruled out because "Fork is different: it creates a new session that starts with a copy of the original's history", which is a second conversation and not a return to the first.

</details>
