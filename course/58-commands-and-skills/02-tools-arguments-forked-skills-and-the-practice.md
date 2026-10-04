# Tools, arguments, forked skills and the practice

**Level:** Architect · **Module 58:** Commands and skills · **Page 2 of 2**
**Exams:** A3.2; S2

**After this page you can** tell a pre-approval from a removal in a skill's frontmatter, fill a skill's text from the arguments typed after its name, run a skill in a separate subagent and know what that subagent sees, put each piece of guidance where it loads the way it is used, and write the module's practice.

Checked on 2026-10-03 against the Claude Code documentation page "Extend Claude with skills", documenting behaviour up to Claude Code v2.1.286. The practice is a set of files graded by Python and TypeScript test suites, offline, on the course's model of the documented rules (`examples/58-skill-model`); nothing in it starts Claude Code. This page deepens module 39 and module 56 (where the pre-approval point was first made) and does not repeat them. Names, locations and who may start a skill are on the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* it describes configuring `allowed-tools` in skill frontmatter "to restrict tool access during skill execution", for example to limit a skill to file writes and so prevent destructive actions; it describes `context: fork` as running a skill in an isolated sub-agent context so that verbose output does not pollute the main conversation; and it lists `argument-hint` as the prompt for required parameters. *What the current product does (documentation checked 2026-10-03):* `allowed-tools` lists "Tools Claude can use without asking permission during the turn that invokes this skill"; it "does not restrict which tools are available: every tool remains callable, and your permission settings still govern tools that are not listed". Restriction is a separate field, `disallowed-tools`, which removes tools "from Claude's available pool while this skill is active". `context: fork` and `argument-hint` behave as the guide says. On the exam, the guide's word "restrict" is the exam's word, so a question that offers `allowed-tools` as the restriction keys it; in a real skill, use `disallowed-tools` or deny rules for a restriction.

## Why it matters

The team's review skill must read a pull request and report, and must never change files. Someone writes `allowed-tools: Read Grep` and believes the skill is now read-only. It is not: Edit and Write remain callable, and the permission settings decide whether Claude is asked. The same confusion repeats with arguments (a placeholder that is spelled wrong silently becomes text) and with forks (a skill that holds only guidelines is handed to a subagent that has nothing to do). Each is a field that means something narrower than its name suggests.

## The idea

### Pre-approve, remove, deny

| Field or rule | What it does |
|---|---|
| `allowed-tools` | Pre-approves the listed tools for the turn that invokes the skill; the grant "clears when you send your next message"; nothing is restricted |
| `disallowed-tools` | Removes the listed tools from the available pool while the skill is active; it clears at the next message |
| A deny rule in permission settings | Blocks the tool or path whatever skill is running |

The patterns in `allowed-tools` are as exact as the rules in module 38: `Bash(git tag *)` pre-approves `git tag v1.2.0` and not `git push --force`, and a bare `Bash` pre-approves every shell command, which is rarely what a release skill should do. In `disallowed-tools`, which works "Like deny rules", only a bare name removes a tool: the permissions page says "A bare tool name like `Bash` removes the tool from Claude's context entirely", so `Edit` takes Edit away, while a scoped entry such as `Edit(src/**)` is a narrower rule that leaves the tool in place. A project skill's `allowed-tools` is applied even in a headless run in a folder never trusted, so review the field of every skill committed to a repository before you run Claude Code there; an organisation that sets `allowManagedPermissionRulesOnly` has the field ignored in project and personal skills.

So a read-only review skill is written in two lines: pre-approve what it needs (`Bash(gh pr view *) Bash(gh pr diff *)`) and remove what it must not have (`disallowed-tools: Edit Write`).

### Arguments

Text typed after the skill name is available to the body:

| Placeholder | Receives |
|---|---|
| `$ARGUMENTS` | All arguments, as typed |
| `$ARGUMENTS[N]`, `$N` | The argument at index N, counting from 0 |
| `$name` | A name declared in `arguments: [name, ...]`, by position |

Indexed arguments use shell-style quoting, so `/my-skill "hello world" second` makes `$0` expand to `hello world` and `$1` to `second`. An indexed placeholder with no argument "stays in the content unchanged", while a named one expands to an empty string. If the user types arguments and no placeholder receives them, Claude Code appends `ARGUMENTS: <your input>` to the end of the content, so Claude still sees them. `argument-hint` only shows the expected shape in autocomplete, such as `[pr-number]`; it fills nothing.

### A skill that runs in its own subagent

`context: fork` starts a new subagent of the type named in `agent` (the default is `general-purpose`) and gives it the skill content as its prompt. "The subagent doesn't see your conversation history, so the skill's instructions have to stand on their own." The name misleads: this is not a fork of the conversation, and when the task depends on the conversation, forking the conversation is the tool. The documentation also warns that `context: fork` "only makes sense for skills with explicit instructions": a body that holds guidelines such as "use these API conventions" gives the subagent no task, and it returns nothing useful. The reason to fork is the guide's: the work reads many files and the main conversation needs only the result. In a headless run the forked skill is waited for; interactively it runs in the background unless `background: false`.

### Where each piece of guidance goes

| The guidance is | It goes in | Because |
|---|---|---|
| Needed in every task of the project | The root memory file | It loads at launch |
| About one kind of file, wherever it sits | A scoped rule with `paths` | It loads on a match |
| A procedure you start on demand | A project skill | Its body loads only when invoked |
| A procedure with side effects | A skill with `disable-model-invocation: true` | A person decides the time |
| A personal variant of a team procedure | A personal skill under a new name | Personal wins over project on a shared name |

### The example

<!-- example: m58-skill-model tabs: python,typescript -->
```python
"""What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.

The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
`/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work.
"""
import re
import shlex
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "40-workflow-lint" / "python"))
from miniyaml import parse_yaml  # noqa: E402

LEVELS = ["enterprise", "personal", "project"]  # the order in which a skill name is resolved: the first level wins


def parse(text):
    """(frontmatter mapping, body) of a command or skill file."""
    m = re.match(r"^---\n(.*?)\n---\n?(.*)$", text, re.S)
    return (parse_yaml(m.group(1)), m.group(2)) if m else ({}, text)


def command_name(path, meta):
    """The slash command a file creates: a skill's `name`, else its folder; a command's file name."""
    parts = path.split("/")
    if parts[-1] == "SKILL.md":
        return str(meta.get("name") or parts[-2])
    return parts[-1][:-3]


def winner(candidates):
    """Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. candidates: {level: path}."""
    return next((candidates[level] for level in LEVELS if level in candidates), None)


def invocation(meta):
    """Who can start it, and whether its description is always in context."""
    manual = meta.get("disable-model-invocation") is True
    hidden = meta.get("user-invocable") is False
    return {"you": not hidden, "claude": not manual, "description_in_context": not manual}


def _names(value):
    return re.findall(r"[^\s,(]+(?:\([^)]*\))?", value) if isinstance(value, str) else list(value or [])


def pre_approved(meta, tool, command=""):
    """True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command."""
    for item in _names(meta.get("allowed-tools")):
        name, _, pattern = item.partition("(")
        if name != tool:
            continue
        pattern = pattern.rstrip(")")
        if not pattern or (pattern.endswith(" *") and (command == pattern[:-2] or command.startswith(pattern[:-1]))) or pattern == command:
            return True
    return False


def removed(meta, tool):
    """True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/**)` leaves the tool in place."""
    return tool in [n for n in _names(meta.get("disallowed-tools")) if "(" not in n]


def tool_status(meta, tool, command=""):
    if removed(meta, tool):
        return "removed"
    return "pre-approved" if pre_approved(meta, tool, command) else "permission settings decide"


def render(body, raw, names=()):
    """Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input."""
    args = shlex.split(raw) if raw else []
    named = dict(zip(names, args))
    used = []

    def sub(m):
        token = m.group(1)
        used.append(token)
        if token == "ARGUMENTS":
            return raw
        index = re.fullmatch(r"ARGUMENTS\[(\d+)\]|(\d+)", token)
        if index:
            i = int(index.group(1) or index.group(2))
            return args[i] if i < len(args) else m.group(0)  # an indexed placeholder with no argument stays as written
        return named.get(token, "")  # a named placeholder with no argument is empty

    out = re.sub(r"\$(ARGUMENTS\[\d+\]|ARGUMENTS|\d+|" + "|".join(map(re.escape, names)) + r")" if names else r"\$(ARGUMENTS\[\d+\]|ARGUMENTS|\d+)", sub, body)
    if raw and not used:
        out = out.rstrip("\n") + f"\nARGUMENTS: {raw}\n"
    return out


def fork_agent(meta):
    """The subagent a forked skill runs in, or None when it runs in the conversation. The subagent does not see the conversation."""
    return None if meta.get("context") != "fork" else str(meta.get("agent") or "general-purpose")


SKILL = """---
name: release-tag
description: Tag a release and push the tag. Use when the user asks to cut a release.
disable-model-invocation: true
argument-hint: "[version]"
arguments: [version]
allowed-tools: Bash(git tag *) Bash(git push origin *)
disallowed-tools: Edit
---
Create the tag $version and push it.
"""


def main():
    meta, body = parse(SKILL)
    print("name:", command_name(".claude/skills/release-tag/SKILL.md", meta), "| legacy file:", command_name(".claude/commands/standup.md", {}))
    print("winner of three same-name skills:", winner({"project": "p/SKILL.md", "personal": "u/SKILL.md"}))
    print("who can start it:", invocation(meta))
    for tool, command in (("Bash", "git tag v1.2.0"), ("Bash", "git push --force"), ("Bash", "rm -rf build"), ("Edit", ""), ("Read", "")):
        print(f"{tool} {command!r}:", tool_status(meta, tool, command))
    print("bare Bash allowed:", pre_approved({"allowed-tools": "Bash"}, "Bash", "rm -rf build"), "| scoped disallow removes Edit:", removed({"disallowed-tools": "Edit(src/**)"}, "Edit"))
    print("render:", render(body, "v1.2.0", ["version"]).strip())
    print("render, no placeholder:", repr(render("Review the change.\n", "123")))
    print("quoted:", render("first=$0 second=$1", '"hello world" second'))
    print("fork:", fork_agent({"context": "fork"}), fork_agent({"context": "fork", "agent": "Explore"}), fork_agent({}))


if __name__ == "__main__":
    main()
```
```text
name: release-tag | legacy file: standup
winner of three same-name skills: u/SKILL.md
who can start it: {'you': True, 'claude': False, 'description_in_context': False}
Bash 'git tag v1.2.0': pre-approved
Bash 'git push --force': permission settings decide
Bash 'rm -rf build': permission settings decide
Edit '': removed
Read '': permission settings decide
bare Bash allowed: True | scoped disallow removes Edit: False
render: Create the tag v1.2.0 and push it.
render, no placeholder: 'Review the change.\nARGUMENTS: 123\n'
quoted: first=hello world second=second
fork: general-purpose Explore None
```
```typescript
/**
 * What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.
 *
 * The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
 * `/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
 * indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work.
 */
import { parseYaml } from "../../40-workflow-lint/typescript/miniyaml.ts";

export type Meta = { [key: string]: any };
export const LEVELS = ["enterprise", "personal", "project"]; // the order in which a skill name is resolved: the first level wins

/** [frontmatter mapping, body] of a command or skill file. */
export function parse(text: string): [Meta, string] {
  const m = /^---\n([\s\S]*?)\n---\n?([\s\S]*)$/.exec(text);
  return m ? [parseYaml(m[1]) as Meta, m[2]] : [{}, text];
}

/** The slash command a file creates: a skill's `name`, else its folder; a command's file name. */
export function commandName(path: string, meta: Meta): string {
  const parts = path.split("/");
  if (parts[parts.length - 1] === "SKILL.md") return String(meta.name || parts[parts.length - 2]);
  return parts[parts.length - 1].slice(0, -3);
}

/** Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. */
export function winner(candidates: Record<string, string>): string | null {
  const level = LEVELS.find((l) => l in candidates);
  return level ? candidates[level] : null;
}

/** Who can start it, and whether its description is always in context. */
export function invocation(meta: Meta): { you: boolean; claude: boolean; description_in_context: boolean } {
  const manual = meta["disable-model-invocation"] === true;
  const hidden = meta["user-invocable"] === false;
  return { you: !hidden, claude: !manual, description_in_context: !manual };
}

function names(value: unknown): string[] {
  return typeof value === "string" ? value.match(/[^\s,(]+(?:\([^)]*\))?/g) ?? [] : [...((value as string[]) ?? [])];
}

/** True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command. */
export function preApproved(meta: Meta, tool: string, command = ""): boolean {
  for (const item of names(meta["allowed-tools"])) {
    const open = item.indexOf("(");
    const name = open < 0 ? item : item.slice(0, open);
    if (name !== tool) continue;
    const pattern = open < 0 ? "" : item.slice(open + 1).replace(/\)+$/, "");
    if (!pattern || (pattern.endsWith(" *") && (command === pattern.slice(0, -2) || command.startsWith(pattern.slice(0, -1)))) || pattern === command) return true;
  }
  return false;
}

/** True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/**)` leaves the tool in place. */
export function removed(meta: Meta, tool: string): boolean {
  return names(meta["disallowed-tools"]).filter((n) => !n.includes("(")).includes(tool);
}

export function toolStatus(meta: Meta, tool: string, command = ""): string {
  if (removed(meta, tool)) return "removed";
  return preApproved(meta, tool, command) ? "pre-approved" : "permission settings decide";
}

/** Shell-style split: whitespace separates, single or double quotes keep a value together. */
export function shellSplit(text: string): string[] {
  return [...text.matchAll(/"([^"]*)"|'([^']*)'|(\S+)/g)].map((m) => m[1] ?? m[2] ?? m[3]);
}

/** Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input. */
export function render(body: string, raw: string, argNames: string[] = []): string {
  const args = raw ? shellSplit(raw) : [];
  const named: Record<string, string> = Object.fromEntries(argNames.slice(0, args.length).map((n, i) => [n, args[i]]));
  let used = false;
  const escaped = argNames.map((n) => n.replace(/[.*+?^${}()|[\]\\]/g, "\\$&"));
  const pattern = new RegExp("\\$(ARGUMENTS\\[\\d+\\]|ARGUMENTS|\\d+" + escaped.map((n) => "|" + n).join("") + ")", "g");
  let out = body.replace(pattern, (whole, token: string) => {
    used = true;
    if (token === "ARGUMENTS") return raw;
    const index = /^ARGUMENTS\[(\d+)\]$|^(\d+)$/.exec(token);
    if (index) return args[Number(index[1] ?? index[2])] ?? whole; // an indexed placeholder with no argument stays as written
    return named[token] ?? ""; // a named placeholder with no argument is empty
  });
  if (raw && !used) out = out.replace(/\n+$/, "") + `\nARGUMENTS: ${raw}\n`;
  return out;
}

/** The subagent a forked skill runs in, or null when it runs in the conversation. The subagent does not see the conversation. */
export function forkAgent(meta: Meta): string | null {
  return meta.context !== "fork" ? null : String(meta.agent || "general-purpose");
}

const SKILL = `---
name: release-tag
description: Tag a release and push the tag. Use when the user asks to cut a release.
disable-model-invocation: true
argument-hint: "[version]"
arguments: [version]
allowed-tools: Bash(git tag *) Bash(git push origin *)
disallowed-tools: Edit
---
Create the tag $version and push it.
`;

function main() {
  const [meta, body] = parse(SKILL);
  console.log("name:", commandName(".claude/skills/release-tag/SKILL.md", meta), "| legacy file:", commandName(".claude/commands/standup.md", {}));
  console.log("winner of three same-name skills:", winner({ project: "p/SKILL.md", personal: "u/SKILL.md" }));
  console.log("who can start it:", JSON.stringify(invocation(meta)));
  for (const [tool, command] of [["Bash", "git tag v1.2.0"], ["Bash", "git push --force"], ["Bash", "rm -rf build"], ["Edit", ""], ["Read", ""]]) {
    console.log(`${tool} '${command}':`, toolStatus(meta, tool, command));
  }
  console.log("bare Bash allowed:", preApproved({ "allowed-tools": "Bash" }, "Bash", "rm -rf build"), "| scoped disallow removes Edit:", removed({ "disallowed-tools": "Edit(src/**)" }, "Edit"));
  console.log("render:", render(body, "v1.2.0", ["version"]).trim());
  console.log("render, no placeholder:", JSON.stringify(render("Review the change.\n", "123")));
  console.log("quoted:", render("first=$0 second=$1", '"hello world" second'));
  console.log("fork:", forkAgent({ context: "fork" }), forkAgent({ context: "fork", agent: "Explore" }), forkAgent({}));
}

if (import.meta.main) main();
```
```text
name: release-tag | legacy file: standup
winner of three same-name skills: u/SKILL.md
who can start it: {"you":true,"claude":false,"description_in_context":false}
Bash 'git tag v1.2.0': pre-approved
Bash 'git push --force': permission settings decide
Bash 'rm -rf build': permission settings decide
Edit '': removed
Read '': permission settings decide
bare Bash allowed: true | scoped disallow removes Edit: false
render: Create the tag v1.2.0 and push it.
render, no placeholder: "Review the change.\nARGUMENTS: 123\n"
quoted: first=hello world second=second
fork: general-purpose Explore null
```
<!-- /example -->

### The practice: a forked review, a manual release and a command

The practice is in [`exercises/58-commands-and-skills`](../../exercises/58-commands-and-skills/unit-01/practice-1/statement.md). You write the review skill (forked, with an explicit task, pre-approving two commands and removing the edit tools), the release skill (manual, with a named argument and two pre-approved patterns), a command file that takes an argument, a personal variant with a new name, and a placement table for five pieces of guidance. It is graded by test suites in Python and TypeScript, offline, on files that are not code in any language, which is why it has no Java or Kotlin edition. The statement lists eight cases, and each says what you should see when it works.

## Traps

1. **"Give the review skill `allowed-tools: Read Grep` so that it cannot write."** It is tempting because the guide says the field restricts tool access. The exam keys the guide, and the product treats it as a pre-approval: Edit and Write stay callable. Removal is `disallowed-tools`, or a deny rule.
2. **"Write `disallowed-tools: Edit(src/**)` to take editing away from the skill."** It is tempting because the scoped form is how deny rules are written. It fails because only a bare name removes a tool; a scoped entry leaves Edit in the pool.
3. **"Fork the skill and write the API conventions in its body."** It is tempting because forking keeps the output out of the main conversation. It fails because a forked skill's body is its prompt, and guidelines without a task give the subagent nothing to do.
4. **"Add `argument-hint: [version]` and the version reaches the text."** It is tempting because the hint is shown where the user types the argument. It fails because the hint only shows the expected shape in autocomplete; the text needs a placeholder such as `$version` (declared with `arguments`) or `$0`, and a body with none gets `ARGUMENTS: <input>` appended, which leaves Claude to guess where the version belongs.

## Quiz

1. A skill's header lists `allowed-tools: Read Grep`, and its author expects editing to be impossible while the skill is active. What actually happens?
   - **a**: Editing is removed from the available tools until the next message
   - **b**: Editing is refused because only the two listed tools are permitted
   - **c**: Editing runs without any prompt because the skill vouches for it
   - **d**: Changing files remains allowed under the usual permission settings

2. A skill's text says only 'Use these naming conventions for new endpoints', and its header sets `context: fork`. What does the isolated helper do?
   - **a**: It reads the whole conversation and applies the conventions to it
   - **b**: It gets guidelines but no assignment, and returns nothing useful
   - **c**: It asks the user which endpoint the conventions concern
   - **d**: It runs inside the main conversation as if nothing were forked

<details>
<summary>Answer key</summary>

1. **d**. The field pre-approves and restricts nothing. *a* is ruled out because removal belongs to the other field, which removes tools "from Claude's available pool while this skill is active". *b* is ruled out because the field "does not restrict which tools are available: every tool remains callable". *c* is ruled out because "your permission settings still govern tools that are not listed".
2. **b**. A forked skill's body is its prompt, and guidelines without a task give the helper nothing to do. *a* is ruled out because the helper "doesn't see your conversation history". *c* is ruled out because the helper starts from the skill text alone, which "gives it the skill content as its prompt", so it has no one to ask. *d* is ruled out because the setting "starts a new subagent of the type named in" the agent field, so it does not stay in the conversation.

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A skill explains how an old billing system works, and Claude should consult it when relevant, while typing its name would do nothing useful. Which header line fits?
   - **a**: `disable-model-invocation: true`
   - **b**: `allowed-tools: Bash(git log *) Bash(git show *)`
   - **c**: `user-invocable: false`
   - **d**: `argument-hint: [system-name]` with `context: fork`

2. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. The team's review skill must be unable to change files for the turn that runs it. Which header line does that?
   - **a**: `allowed-tools: Read Grep Glob`
   - **b**: `disallowed-tools: Edit Write`
   - **c**: `argument-hint: [read-only]`
   - **d**: `disallowed-tools: Edit(src/**)`

3. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A command text reads 'Summarise the commits by $ARGUMENTS since yesterday', and a developer runs `/standup ana`. What does Claude receive?
   - **a**: The sentence unchanged, with the name in a separate note
   - **b**: The sentence with a blank where the name belongs
   - **c**: The sentence with the name filled in and a line `ARGUMENTS: ana` after it
   - **d**: The sentence with the name filled in and nothing appended

<details>
<summary>Answer key</summary>

1. **c**. It hides the skill from the person and leaves it available to Claude, which suits background knowledge. *a* is ruled out because it does the opposite, so that only the person starts it and "the full skill loads only when you invoke it". *b* is ruled out because a pre-approval for git commands says nothing about who may start the skill, since the field "Pre-approves the listed tools for the turn that invokes the skill". *d* is ruled out because `argument-hint` "only shows the expected shape in autocomplete", and a fork would give a background note no task.
2. **b**. A bare name in the removal field takes the tool out of the pool while the skill is active. *a* is ruled out because the pre-approval field "does not restrict which tools are available". *c* is ruled out because `argument-hint` "only shows the expected shape in autocomplete". *d* is ruled out because "only a bare name removes a tool", so the scoped entry leaves editing in place.
3. **d**. The placeholder received the input, so it is substituted and nothing is added. *a* is ruled out because the placeholder takes "All arguments, as typed" and is replaced in the text. *b* is ruled out because a blank arises only for a named placeholder without an argument, since an indexed one "stays in the content unchanged", and here the input is given. *c* is ruled out because the extra line appears only when "no placeholder receives them", and this one did.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
