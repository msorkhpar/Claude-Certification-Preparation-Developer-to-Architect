# Subagents, their memory, and hooks

**Level:** Developer · **Module 39:** Extending Claude Code · **Page 2 of 3**
**Exams:** DV1, DV7; A3.2

**After this page you can** define a subagent with a bounded toolset, choose a memory scope for it, say which of its fields a plugin ignores, write a hook that blocks a destructive command or an edit to a protected file, and read a hook's exit codes and JSON answer correctly.

Checked on 2026-10-03 against the Claude Code documentation (subagents, the hooks guide and the features overview), which mention behaviour up to Claude Code v2.1.286. The example is a hook written as a process and a linter for skill and subagent files, in Python and TypeScript. It is the course's own model of the documented rules. It was run offline and it sends nothing anywhere.

## Why it matters

Subagents keep a noisy side task out of the main conversation, and hooks turn a rule into code that always runs. Both are on the exam, and both have details that decide whether a configuration is safe: which tools a subagent inherits when you list none, which fields a plugin silently ignores, and which hook answer actually blocks a call.

## The idea

### Subagents

"Each subagent runs in its own context window with a custom system prompt, specific tool access, and independent permissions." Claude delegates a task to a subagent when it matches the subagent's description, and the subagent "does that work in its own context and returns only the summary." So a subagent is for work that "would flood your main conversation with search results, logs, or file contents you won't reference again". A file in `.claude/agents/` defines one: YAML frontmatter, then the system prompt as Markdown. "Only `name` and `description` are required."

The fields to know:

| Field | What it does |
|---|---|
| `description` | Claude reads it to decide when to delegate, so write when to use the agent |
| `tools` | The tools the subagent may use. Omit it and the subagent inherits every tool |
| `model` | `sonnet`, `haiku`, `opus` or `inherit`. The default is the main conversation's model |
| `maxTurns` | Stops the subagent after that many agentic turns |
| `memory` | `user`, `project` or `local`: a persistent directory that survives across conversations |
| `permissionMode`, `hooks`, `mcpServers` | Work for your own agents, and are ignored for plugin agents |

Bound a subagent by listing its tools. A reviewer needs `Read`, `Grep` and `Glob`, and a reviewer with no `tools` line can edit files and run commands. A file with no `name` is skipped without a message, and so is one with a `name` but no `description`, so a subagent that never appears is usually a frontmatter problem.

Where the file lives sets who has it, and the same name resolves by priority: managed settings first, then the command line, then project, then user, and a plugin's `agents/` directory last, at priority 5.

### Subagent memory

The `memory` field "gives the subagent a persistent directory that survives across conversations", where it builds knowledge "such as codebase patterns, debugging insights, and architectural decisions." The scope chooses where that directory lives and who shares it.

| Scope | Location | Use it when |
|---|---|---|
| `user` | `~/.claude/agent-memory/<name>/` | The agent should remember across all projects |
| `project` | `.claude/agent-memory/<name>/` | The knowledge is project-specific and "shareable via version control" |
| `local` | `.claude/agent-memory-local/<name>/` | The knowledge is project-specific and should not be checked in |

Subagent memory is part of auto memory, so turning auto memory off makes the field have no effect.

### Hooks

Hooks are "user-defined shell commands" that run at points in Claude Code's life cycle, "which gives you deterministic control: certain actions always happen rather than relying on the LLM to choose to run them." That is the sentence that separates a hook from a memory line or a skill. The events that matter most are `PreToolUse` (before a tool runs, and it "Can block it"), `PostToolUse` (after a call succeeds), `UserPromptSubmit`, `SessionStart` and `Stop`. A hook is registered under an event with a `matcher` that filters when it fires, such as `Edit|Write` for the file-editing tools, and one or more handlers of type `command`.

A command hook is a process. "When an event fires, Claude Code passes event-specific data as JSON to your script's stdin", and the script answers with its exit code and its output.

- **Exit 0**: no objection. Output is ignored for a `PreToolUse` hook unless it is the JSON answer below.
- **Exit 2**: "Claude Code blocks the action. Write a reason to stderr." For `PreToolUse` the reason goes to Claude as feedback, so it can adjust.
- **Any other exit code**: with empty or plain-text output, "the action proceeds as a non-blocking error". Treat it as a bug in the hook, because it does not stop the call.

Exit 0 with a JSON object on standard output is the structured answer. For `PreToolUse`, put `hookSpecificOutput` with `hookEventName: "PreToolUse"`, `permissionDecision` as `allow`, `deny` or `ask`, and `permissionDecisionReason`. Both forms block. The JSON form lets you say `ask` and carries a reason that is shown to the person.

Several hooks can match one call, and they run in parallel. "For `PreToolUse` permission decisions, the most restrictive answer applies, in the order `deny`, `defer`, `ask`, `allow`." A logging hook that exits 0 does not weaken a guard hook that denies.

Two practical points. A hook that guards a rule must read the whole command and not a prefix. A deny rule `Bash(git push *)` misses `git -C . push`, a push behind an environment assignment, a push inside `sh -c`, and one after `&&`, and a hook can normalise those spellings. And keep the matcher narrow: the documentation warns that an empty or `.*` matcher on a permission hook "would auto-approve every tool permission prompt".

<!-- example: m39-hook-gate tabs: python,typescript -->
```python
"""A PreToolUse hook that blocks destructive commands, and a linter for skill and subagent files.

Claude Code starts a hook as a process, writes one JSON event to its standard input and reads the answer from its exit code, its
standard output and its standard error. Exit code 2 blocks the call and the standard error is the reason; exit code 0 with a JSON
`permissionDecision` answers in a structured way; exit code 0 with nothing printed gives no opinion. This file is such a hook (run it
with --hook) and a demonstration (run it plain). The event and output shapes are those of the Claude Code hooks reference, read on
2026-10-03. The command check normalises what a prefix rule such as `Bash(git push *)` would miss: another form of the same command.
"""
import json
import re
import shlex
import sys

from miniyaml import split_frontmatter

PROTECTED = (".env", "package-lock.json", ".git/")


def _git_subcommand(args):
    i = 0
    while i < len(args):
        if args[i] in ("-C", "-c", "--git-dir", "--work-tree"):
            i += 2
        elif args[i].startswith("-"):
            i += 1
        else:
            return args[i]
    return None


def dangerous(command):
    """The reason a command is refused, or None. Looks through compound commands, `sh -c`, env assignments, paths and git options."""
    for part in re.split(r"&&|\|\||;|\||&|\n", command):
        try:
            words = shlex.split(part)
        except ValueError:
            return "a command that cannot be parsed is not run unreviewed"
        while words and re.fullmatch(r"\w+=\S*", words[0]):
            words = words[1:]
        if not words:
            continue
        program, args = words[0].rsplit("/", 1)[-1], words[1:]
        if program in ("sh", "bash", "zsh") and "-c" in args and args.index("-c") + 1 < len(args):
            inner = dangerous(args[args.index("-c") + 1])
            if inner:
                return inner
        if program == "git" and _git_subcommand(args) == "push":
            return "nothing is pushed from an agent"
        short = "".join(a[1:] for a in args if a.startswith("-") and not a.startswith("--"))
        recursive, force = "r" in short or "R" in short or "--recursive" in args, "f" in short or "--force" in args
        if program == "rm" and recursive and force:
            return "a recursive forced delete is not run from an agent"
    return None


def pre_tool_use(event):
    """(exit code, standard output, standard error) for one PreToolUse event."""
    tool, tool_input = event["tool_name"], event.get("tool_input", {})
    if tool == "Bash":
        reason = dangerous(tool_input.get("command", ""))
        if reason:
            out = {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": reason}}
            return 0, json.dumps(out), ""
    if tool in ("Edit", "Write", "MultiEdit"):
        path = tool_input.get("file_path", "").replace("\\", "/")
        for pattern in PROTECTED:
            if pattern in path:
                return 2, "", f"Blocked: {path} matches protected pattern '{pattern}'"
    return 0, "", ""


# --- linting the files that configure Claude Code -----------------------------------------------------------------------------

SIDE_EFFECTS = re.compile(r"\b(deploy|publish|delete|commit|push|merge)\b", re.I)


def lint_skill(text):
    fm, _ = split_frontmatter(text)
    findings = []
    desc = str(fm.get("description") or "")
    if not desc:
        findings.append("description is missing: Claude uses it to decide when to load the skill")
    if len(desc) + len(str(fm.get("when_to_use") or "")) > 1536:
        findings.append("description and when_to_use together exceed 1,536 characters and are cut")
    if SIDE_EFFECTS.search(f"{fm.get('name', '')} {desc}") and fm.get("disable-model-invocation") is not True:
        findings.append("a skill with side effects should set disable-model-invocation: true")
    allowed = fm.get("allowed-tools")
    if allowed and "Bash" in str(allowed).replace(",", " ").split():
        findings.append("allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead")
    return findings


def lint_agent(text):
    fm, _ = split_frontmatter(text)
    findings = [f"{k} is required" for k in ("name", "description") if not fm.get(k)]
    if "tools" not in fm:
        findings.append("tools is omitted: the subagent inherits every tool")
    if fm.get("memory") is not None and fm["memory"] not in ("user", "project", "local"):
        findings.append("memory must be user, project or local")
    if fm.get("permissionMode") == "bypassPermissions":
        findings.append("permissionMode bypassPermissions skips every prompt in this subagent")
    return findings


SKILL = "---\nname: deploy\ndescription: Deploy the service to production\nallowed-tools: Bash\n---\nRun the release script.\n"
AGENT = "---\nname: reviewer\ndescription: Reviews a diff for bugs\nmemory: team\n---\nYou review code.\n"


def demo():
    events = [("Bash", {"command": "git push origin main"}), ("Bash", {"command": "git -C . push origin main"}), ("Bash", {"command": "bash -c 'rm -rf build'"}),
              ("Bash", {"command": "ls && git status"}), ("Bash", {"command": "rm build/old.txt"}), ("Edit", {"file_path": "/work/app/.env"}), ("Edit", {"file_path": "/work/app/main.py"})]
    for tool, tool_input in events:
        code, out, err = pre_tool_use({"hook_event_name": "PreToolUse", "tool_name": tool, "tool_input": tool_input})
        shown = f"deny (exit 0, JSON): {json.loads(out)['hookSpecificOutput']['permissionDecisionReason']}" if out else f"block (exit 2): {err}" if code == 2 else "no decision (exit 0)"
        print(f"{tool} {next(iter(tool_input.values()))!r} -> {shown}")
    print("skill findings:", lint_skill(SKILL))
    print("agent findings:", lint_agent(AGENT))


if __name__ == "__main__":
    if "--hook" in sys.argv:
        code, out, err = pre_tool_use(json.load(sys.stdin))
        sys.stdout.write(out)
        sys.stderr.write(err)
        sys.exit(code)
    demo()
```
```text
Bash 'git push origin main' -> deny (exit 0, JSON): nothing is pushed from an agent
Bash 'git -C . push origin main' -> deny (exit 0, JSON): nothing is pushed from an agent
Bash "bash -c 'rm -rf build'" -> deny (exit 0, JSON): a recursive forced delete is not run from an agent
Bash 'ls && git status' -> no decision (exit 0)
Bash 'rm build/old.txt' -> no decision (exit 0)
Edit '/work/app/.env' -> block (exit 2): Blocked: /work/app/.env matches protected pattern '.env'
Edit '/work/app/main.py' -> no decision (exit 0)
skill findings: ['a skill with side effects should set disable-model-invocation: true', 'allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead']
agent findings: ['tools is omitted: the subagent inherits every tool', 'memory must be user, project or local']
```
```typescript
// A PreToolUse hook that blocks destructive commands, and a linter for skill and subagent files.
//
// Claude Code starts a hook as a process, writes one JSON event to its standard input and reads the answer from its exit code, its
// standard output and its standard error. Exit code 2 blocks the call and the standard error is the reason; exit code 0 with a JSON
// `permissionDecision` answers in a structured way; exit code 0 with nothing printed gives no opinion. This file is such a hook (run it
// with --hook) and a demonstration (run it plain). The event and output shapes are those of the Claude Code hooks reference, read on
// 2026-10-03. The command check normalises what a prefix rule such as `Bash(git push *)` would miss: another form of the same command.
import { readFileSync } from "node:fs";
import { splitFrontmatter } from "./miniyaml.ts";

const PROTECTED = [".env", "package-lock.json", ".git/"];

/** The words of a shell command, with quotes and backslashes read the way a POSIX shell reads them. */
function shellSplit(text: string): string[] {
  const words: string[] = [];
  let cur = "";
  let inWord = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === "'") {
      const end = text.indexOf("'", i + 1);
      if (end < 0) throw new Error("No closing quotation");
      cur += text.slice(i + 1, end);
      i = end;
      inWord = true;
    } else if (c === '"') {
      i++;
      while (i < text.length && text[i] !== '"') {
        if (text[i] === "\\" && i + 1 < text.length && '"\\$`'.includes(text[i + 1])) i++;
        cur += text[i++];
      }
      if (i >= text.length) throw new Error("No closing quotation");
      inWord = true;
    } else if (c === "\\" && i + 1 < text.length) {
      cur += text[++i];
      inWord = true;
    } else if (/\s/.test(c)) {
      if (inWord) words.push(cur);
      cur = "";
      inWord = false;
    } else {
      cur += c;
      inWord = true;
    }
  }
  if (inWord) words.push(cur);
  return words;
}

function gitSubcommand(args: string[]): string | null {
  let i = 0;
  while (i < args.length) {
    if (["-C", "-c", "--git-dir", "--work-tree"].includes(args[i])) i += 2;
    else if (args[i].startsWith("-")) i += 1;
    else return args[i];
  }
  return null;
}

/** The reason a command is refused, or null. Looks through compound commands, `sh -c`, env assignments, paths and git options. */
export function dangerous(command: string): string | null {
  for (const part of command.split(/&&|\|\||;|\||&|\n/)) {
    let words: string[];
    try {
      words = shellSplit(part);
    } catch {
      return "a command that cannot be parsed is not run unreviewed";
    }
    while (words.length && /^\w+=\S*$/.test(words[0])) words = words.slice(1);
    if (!words.length) continue;
    const program = words[0].split("/").at(-1) as string;
    const args = words.slice(1);
    if (["sh", "bash", "zsh"].includes(program) && args.includes("-c") && args.indexOf("-c") + 1 < args.length) {
      const inner = dangerous(args[args.indexOf("-c") + 1]);
      if (inner) return inner;
    }
    if (program === "git" && gitSubcommand(args) === "push") return "nothing is pushed from an agent";
    const short = args.filter((a) => a.startsWith("-") && !a.startsWith("--")).map((a) => a.slice(1)).join("");
    const recursive = short.includes("r") || short.includes("R") || args.includes("--recursive");
    const force = short.includes("f") || args.includes("--force");
    if (program === "rm" && recursive && force) return "a recursive forced delete is not run from an agent";
  }
  return null;
}

/** [exit code, standard output, standard error] for one PreToolUse event. */
export function preToolUse(event: any): [number, string, string] {
  const tool = event.tool_name;
  const toolInput = event.tool_input ?? {};
  if (tool === "Bash") {
    const reason = dangerous(toolInput.command ?? "");
    if (reason) {
      const out = { hookSpecificOutput: { hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: reason } };
      return [0, JSON.stringify(out), ""];
    }
  }
  if (["Edit", "Write", "MultiEdit"].includes(tool)) {
    const path = String(toolInput.file_path ?? "").replaceAll("\\", "/");
    for (const pattern of PROTECTED) if (path.includes(pattern)) return [2, "", `Blocked: ${path} matches protected pattern '${pattern}'`];
  }
  return [0, "", ""];
}

// --- linting the files that configure Claude Code -------------------------------------------------------------------------------

const SIDE_EFFECTS = /\b(deploy|publish|delete|commit|push|merge)\b/i;

export function lintSkill(text: string): string[] {
  const [fm] = splitFrontmatter(text);
  const findings: string[] = [];
  const desc = String(fm.description ?? "");
  if (!desc) findings.push("description is missing: Claude uses it to decide when to load the skill");
  if (desc.length + String(fm.when_to_use ?? "").length > 1536) findings.push("description and when_to_use together exceed 1,536 characters and are cut");
  if (SIDE_EFFECTS.test(`${fm.name ?? ""} ${desc}`) && fm["disable-model-invocation"] !== true) findings.push("a skill with side effects should set disable-model-invocation: true");
  const allowed = fm["allowed-tools"];
  if (allowed && String(allowed).replaceAll(",", " ").split(/\s+/).includes("Bash")) findings.push("allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead");
  return findings;
}

export function lintAgent(text: string): string[] {
  const [fm] = splitFrontmatter(text);
  const findings = ["name", "description"].filter((k) => !fm[k]).map((k) => `${k} is required`);
  if (!("tools" in fm)) findings.push("tools is omitted: the subagent inherits every tool");
  if (fm.memory != null && !["user", "project", "local"].includes(String(fm.memory))) findings.push("memory must be user, project or local");
  if (fm.permissionMode === "bypassPermissions") findings.push("permissionMode bypassPermissions skips every prompt in this subagent");
  return findings;
}

const SKILL = "---\nname: deploy\ndescription: Deploy the service to production\nallowed-tools: Bash\n---\nRun the release script.\n";
const AGENT = "---\nname: reviewer\ndescription: Reviews a diff for bugs\nmemory: team\n---\nYou review code.\n";

/** A string written the way Python writes it, so both languages show the same output. */
function pyRepr(s: string): string {
  return s.includes("'") && !s.includes('"') ? `"${s}"` : `'${s}'`;
}

function pyList(items: string[]): string {
  return `[${items.map(pyRepr).join(", ")}]`;
}

function demo() {
  const events: Array<[string, Record<string, string>]> = [["Bash", { command: "git push origin main" }], ["Bash", { command: "git -C . push origin main" }], ["Bash", { command: "bash -c 'rm -rf build'" }],
    ["Bash", { command: "ls && git status" }], ["Bash", { command: "rm build/old.txt" }], ["Edit", { file_path: "/work/app/.env" }], ["Edit", { file_path: "/work/app/main.py" }]];
  for (const [tool, toolInput] of events) {
    const [code, out, err] = preToolUse({ hook_event_name: "PreToolUse", tool_name: tool, tool_input: toolInput });
    const shown = out ? `deny (exit 0, JSON): ${JSON.parse(out).hookSpecificOutput.permissionDecisionReason}` : code === 2 ? `block (exit 2): ${err}` : "no decision (exit 0)";
    console.log(`${tool} ${pyRepr(Object.values(toolInput)[0])} -> ${shown}`);
  }
  console.log("skill findings:", pyList(lintSkill(SKILL)));
  console.log("agent findings:", pyList(lintAgent(AGENT)));
}

if (import.meta.main) {
  if (process.argv.includes("--hook")) {
    const [code, out, err] = preToolUse(JSON.parse(readFileSync(0, "utf8")));
    process.stdout.write(out);
    process.stderr.write(err);
    process.exitCode = code;
  } else demo();
}
```
```text
Bash 'git push origin main' -> deny (exit 0, JSON): nothing is pushed from an agent
Bash 'git -C . push origin main' -> deny (exit 0, JSON): nothing is pushed from an agent
Bash "bash -c 'rm -rf build'" -> deny (exit 0, JSON): a recursive forced delete is not run from an agent
Bash 'ls && git status' -> no decision (exit 0)
Bash 'rm build/old.txt' -> no decision (exit 0)
Edit '/work/app/.env' -> block (exit 2): Blocked: /work/app/.env matches protected pattern '.env'
Edit '/work/app/main.py' -> no decision (exit 0)
skill findings: ['a skill with side effects should set disable-model-invocation: true', 'allowed-tools names bare Bash: pre-approve a pattern such as Bash(git add *) instead']
agent findings: ['tools is omitted: the subagent inherits every tool', 'memory must be user, project or local']
```
<!-- /example -->

The program is a hook and a linter. Plain, it runs seven events through the gate. A `git push`, `git -C . push` and `bash -c 'rm -rf build'` are denied through the JSON answer with exit 0 and a reason. A compound `ls && git status` and a plain `rm build/old.txt` get no opinion. An edit of `.env` blocks with exit 2 and a reason on standard error, and an edit of `main.py` gets no opinion. With `--hook` the same code reads one event from standard input and exits with the code, which is how Claude Code would start it. The linter then reads two files. The skill `deploy` has side effects and a bare `Bash` grant, so it gets two findings. The agent has no `tools` line and a `memory` value that is not `user`, `project` or `local`, so it gets two findings as well. Both languages print the same lines.

## Traps

1. **Omitting `tools` on a subagent that should only read.** It inherits every tool, edits and shell included.
2. **Believing a hook's exit 1 blocks the call.** Only exit 2, or exit 0 with a JSON `deny`, blocks. Any other code is a non-blocking error.
3. **Guarding a command with a prefix.** `Bash(git push *)` is not matched by `git -C . push`. Parse the command.
4. **Putting `permissionMode` or `hooks` in a plugin's agent.** The plugin loader ignores them, so the agent runs without the limit you wrote. Ship hooks in the plugin's `hooks/hooks.json`.

## Quiz

1. A reviewer subagent's file has no `tools` line. What can it do?
   - **a**: Read only, because a review of code needs nothing more than reading it
   - **b**: Use every tool of the session, including editing and running commands
   - **c**: Nothing at all, because an empty list gives the subagent no tools to use
   - **d**: Use only the tools that the main conversation allows without asking first

2. A hook script is meant to stop `git push`. Which behaviour stops the call?
   - **a**: Printing a warning to standard output and exiting with code 1
   - **b**: Exiting with code 0 after logging the command to a file
   - **c**: Exiting with code 3 after writing a reason to standard error
   - **d**: Exiting with code 2 with the reason written to standard error

3. A team wants the learnings of a subagent to travel with the repository. Which memory choice fits?
   - **a**: The user scope, which lives in the home directory of each single person
   - **b**: The project scope, whose directory is shareable via version control
   - **c**: The local scope, which is kept out of version control on purpose here
   - **d**: No scope applies, because memory is always private to a single machine

<details>
<summary>Answer key</summary>

1. **b**. The page says "Omit it and the subagent inherits every tool", and "a reviewer with no `tools` line can edit files and run commands". *a* is ruled out because a reviewer is bounded only when you list `Read`, `Grep` and `Glob`, as the page says: "Bound a subagent by listing its tools." *c* is ruled out because the table says "Omit it and the subagent inherits every tool", which is the opposite of none. *d* is ruled out because a subagent has "specific tool access, and independent permissions", and an omitted list does not narrow it to the main conversation's pre-approvals.
2. **d**. The page says "Exit 2: Claude Code blocks the action. Write a reason to stderr." *a* is ruled out because with an exit code of 1 and plain-text output "the action proceeds as a non-blocking error", and a warning does not stop the call. *b* is ruled out because "Exit 0: no objection", and a logging hook "does not weaken a guard hook" and does not block either. *c* is ruled out because exit code 3 is an "other exit code", and the page says to "Treat it as a bug in the hook, because it does not stop the call."
3. **b**. The table says the `project` scope is for knowledge that is "project-specific and shareable via version control". *a* is ruled out because the `user` scope is `~/.claude/agent-memory/<name>/`, for an agent that "should remember across all projects", in each person's home directory. *c* is ruled out because the `local` scope is for knowledge that "should not be checked in". *d* is ruled out because the project scope's directory "shareable via version control" is the page's own counterexample, so not every memory is private.

</details>
