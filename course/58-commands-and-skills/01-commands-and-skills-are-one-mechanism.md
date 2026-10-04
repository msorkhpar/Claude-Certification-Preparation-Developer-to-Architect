# Commands and skills are one mechanism

**Level:** Architect · **Module 58:** Commands and skills · **Page 1 of 2**
**Exams:** A3.2; S2

**After this page you can** say that a command file and a skill both create a slash command, pick the right location and name for a project, personal or organisation procedure, predict which of two same-named skills runs, control who may start a skill, and write a description that says when to use it.

Checked on 2026-10-03 against the Claude Code documentation page "Extend Claude with skills" (which includes custom commands), documenting behaviour up to Claude Code v2.1.286. Nothing here was run against a live session: the example is a model of the documented rules over file paths and texts. This page deepens module 39 (skills and how they compare with other extensions) and module 27 (what each extension costs), and it does not repeat them. Tool permissions, arguments and forked skills are the second page.

> **Exam guide and current product.** *What the guide states (task statement 3.2), and so what the exam keys:* in its task on commands and skills (3.2) it describes project-scoped commands in `.claude/commands/` shared through version control, personal commands in `~/.claude/commands/`, and skills in `.claude/skills/` with a `SKILL.md` whose frontmatter offers `context: fork`, `allowed-tools` and `argument-hint`. *What the current product does (documentation checked 2026-10-03):* "Custom commands have been merged into skills. A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` both create `/deploy` and work the same way." Existing command files keep working, and "Prefer a skill for new work, since skills also support supporting files". When a skill and a command file share a name, the skill runs. On the exam, a question about where a team shares a command still has the guide's answer, the project folder under version control; a question about how the two differ has the product's answer, that they are one mechanism with the skill format as the richer one.

## Why it matters

A team has three recurring chores: reviewing a pull request against a checklist, tagging a release, and writing a standup note. Each is pasted into the chat from a wiki page, and each version drifts. The fix is a file that sits in the repository and is typed as `/name`. The exam's questions then split into two kinds: where does the file go so that the right people get it, and what does a field in its header do. Most wrong answers on the second kind come from reading a field the way its name sounds.

## The idea

### One mechanism, two file shapes

| Shape | Path | Notes |
|---|---|---|
| Skill | `.claude/skills/<name>/SKILL.md` | A folder: it can hold scripts and reference files beside the instructions |
| Command file | `.claude/commands/<name>.md` | The older single-file format; same frontmatter except `name` and `paths` |

Both create `/<name>`. A skill's slash command is its `name` field, or its folder name when the field is absent; a command file's is its file name. The practical rule is to write new work as a skill, and leave existing commands alone, because they keep working.

### Where the file lives decides who gets it

| Location | Who gets it |
|---|---|
| Enterprise (managed settings) | Everyone in the organisation |
| `~/.claude/skills/<name>/` | You, in every project |
| `.claude/skills/<name>/` | Whoever works in the repository, through version control |
| A plugin | Where the plugin is enabled, with the name prefixed by the plugin's name |

The guide's pair, project and personal, is the middle of this table, with the same logic as memory on module 57: what is committed is shared, and what is in your home folder is yours. Project skills are also found from the directory where you start and from every parent up to the repository root, and a skill in a folder below the start directory loads "the first time Claude reads or edits a file in that subdirectory", and the skills "stay available for the rest of the session".

### The name decides which one runs

Of skills that share a name, "Enterprise over personal, and personal over project." With a `deploy` skill in both `~/.claude/skills/` and the project's `.claude/skills/`, `/deploy` runs the personal one, so a personal skill with the team's name silently replaces the team's version on your machine. A skill beats a command file of the same name. A project-root skill and a nested one with one name both load, and the nested one is also reachable as `/apps/web:deploy`. The safe habit for a personal variant is a different name (`review-pr-mine`), which also keeps the team's version available to you.

### Who may start it

Two frontmatter fields control it, and they do opposite things:

| Field | You can type `/name` | Claude can invoke it | Description in context |
|---|---|---|---|
| (default) | Yes | Yes | Yes |
| `disable-model-invocation: true` | Yes | No | No: the full skill loads only when you invoke it |
| `user-invocable: false` | No | Yes | Yes |

A release, a deploy or a message to a channel has side effects, so `disable-model-invocation: true` keeps the model from deciding by itself that the time has come. Background knowledge that is no action, such as an explanation of a legacy system, is `user-invocable: false`. Note the second note in the documentation: `user-invocable: false` hides the skill from you but not from Claude, and it is `disable-model-invocation` that stops Claude.

### The description is how a skill is found

For a skill Claude may invoke, the description is the only thing in context until it loads: "Claude uses this to decide when to apply the skill". It should say what the skill does and when to use it, with the key use case first, because the combined `description` and `when_to_use` text "is truncated at 1,536 characters in the skill listing". A description such as "A skill that reviews pull requests" gives Claude no trigger; "Review a pull request for correctness and risk. Use when the user asks for a review or gives a pull request number" does. The listing as a whole also has a budget, so many long descriptions cost context on every turn, and `/context` shows the Skills row after the budget is applied.

### The example

The example parses a command or skill file, names the slash command, picks the winner among same-named skills, and states who may start it.

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

## Traps

1. **"Commands and skills are different features; migrate every command before the next release."** It is tempting because the guide lists them separately. The product rejects it: they are merged, a command file "work[s] the same way", and existing files keep working. A skill is preferred for new work because it can hold supporting files.
2. **"Save my own `review-pr` skill in `~/.claude/skills/`; it will sit beside the team's."** It is tempting because the two live in different places. The exam rejects it: of two skills with one name, personal wins over project, so the team's review no longer runs for you. Give the variant its own name.
3. **"Set `user-invocable: false` so that Claude cannot trigger the release by itself."** It is tempting because the field sounds like the one that controls invocation. The exam rejects it: that field hides the skill from you and leaves it available to Claude. The field that stops Claude is `disable-model-invocation: true`.
4. **"Describe it as 'A skill that reviews pull requests'; the name does the rest."** It is tempting because the name is clear to a person. The exam rejects it: the description is what Claude reads to decide when to load the skill, so it must say when to use it.

## Quiz

1. A project holds `.claude/commands/deploy.md`, and a teammate then adds `.claude/skills/deploy/SKILL.md`. What does `/deploy` run?
   - **a**: The skill, since it takes precedence over the older format
   - **b**: Both, one after the other, with the command going first
   - **c**: The command, because it was in the project first of the two
   - **d**: Neither, because the duplicate name is refused outright

2. A skill folder sits under `.claude/skills/` in a nested folder two levels below where the session began. When does it become available?
   - **a**: Never, since only skills at the repository root are read
   - **b**: At startup, together with the skills at the repository root
   - **c**: The first time Claude reads or edits a file in that location
   - **d**: Only after a restart that begins inside that folder

<details>
<summary>Answer key</summary>

1. **a**. The product merged the two formats and gives the skill precedence over a command file of the same name. *c* is ruled out because "A skill beats a command file of the same name", and the order of arrival plays no part. *b* is ruled out because the two formats are "one mechanism with the skill format as the richer one", so one name resolves to one definition. *d* is ruled out because "Existing command files keep working", and a shared name is settled by precedence and not refused.
2. **c**. Skills in a lower folder load when Claude first works with a file there. *b* is ruled out because a lower skill loads "the first time Claude reads or edits a file in that subdirectory", and not at startup. *a* is ruled out because project skills are found "from the directory where you start and from every parent up to the repository root", and a lower folder loads on first use. *d* is ruled out because once loaded, the skills "stay available for the rest of the session", with no restart.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
