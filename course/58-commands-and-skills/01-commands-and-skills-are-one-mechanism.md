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

The guide's pair, project and personal, is the middle of this table, with the same logic as memory in module 57: what is committed is shared, and what is in your home folder is yours. Project skills are also found from the directory where you start and from every parent up to the repository root, and a skill in a folder below the start directory loads "the first time Claude reads or edits a file in that subdirectory", and the skills "stay available for the rest of the session".

### The name decides which one runs

Of skills that share a name, "Enterprise over personal, and personal over project." With a `deploy` skill in both `~/.claude/skills/` and the project's `.claude/skills/`, `/deploy` runs the personal one, so a personal skill with the team's name silently replaces the team's version on your machine. A skill beats a command file of the same name. A project-root skill and a nested one with one name both load, and the nested one is also reachable as `/apps/web:deploy`. The safe habit for a personal variant is a different name (`review-pr-mine`), which also keeps the team's version available to you.

### Who may start it

Two frontmatter fields control it, and they do opposite things:

| Field | You can type `/name` | Claude can invoke it | Description in context |
|---|---|---|---|
| (default) | Yes | Yes | Yes |
| `disable-model-invocation: true` | Yes | No | No: the full skill loads only when you invoke it |
| `user-invocable: false` | No | Yes | Yes |

A release, a deploy or a message to a channel has side effects, so `disable-model-invocation: true` keeps the model from deciding by itself that the time has come. Background knowledge that is no action, such as an explanation of a legacy system, is `user-invocable: false`. Note the documentation's caveat: `user-invocable: false` hides the skill from you but not from Claude, and it is `disable-model-invocation` that stops Claude.

### The description is how a skill is found

For a skill Claude may invoke, the description is the only thing in context until it loads: "Claude uses this to decide when to apply the skill". It should say what the skill does and when to use it, with the key use case first, because the combined `description` and `when_to_use` text "is truncated at 1,536 characters in the skill listing". A description such as "A skill that reviews pull requests" gives Claude no trigger; "Review a pull request for correctness and risk. Use when the user asks for a review or gives a pull request number" does. The listing as a whole also has a budget, so many long descriptions cost context on every turn, and `/context` shows the Skills row after the budget is applied.

### The example

The example parses a command or skill file, names the slash command, picks the winner among same-named skills, and states who may start it.

<!-- example: m58-skill-model tabs: python,typescript,java,kotlin -->
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
import logging

log = logging.getLogger(__name__)

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
import { logger } from "./logger.ts";
const log = logger("skill_model");
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
```java
import static harness.Show.py;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.
 *
 * <p>The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
 * `/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
 * indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work:
 * the front matter is YAML, read with Jackson.
 */
public final class SkillModel {
    private static final System.Logger LOG = System.getLogger(SkillModel.class.getName());
    static final List<String> LEVELS = List.of("enterprise", "personal", "project"); // the order in which a skill name is resolved: the first level wins
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final Pattern FRONTMATTER = Pattern.compile("^---\\n(.*?)\\n---\\n?(.*)$", Pattern.DOTALL);

    /** The front matter of a command or skill file, and its body. */
    record Parsed(Map<String, Object> meta, String body) {}

    /** The words of a command line as a POSIX shell would split them (quotes and backslashes); a bad quote is an error. */
    static List<String> shlexSplit(String s) {
        List<String> words = new ArrayList<>();
        StringBuilder word = null;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (Character.isWhitespace(c)) {
                if (word != null) { words.add(word.toString()); word = null; }
                continue;
            }
            if (word == null) word = new StringBuilder();
            if (c == '\\') {
                if (i >= s.length()) throw new IllegalArgumentException("No escaped character");
                word.append(s.charAt(i++));
            } else if (c == '\'') {
                int end = s.indexOf('\'', i);
                if (end < 0) throw new IllegalArgumentException("No closing quotation");
                word.append(s, i, end);
                i = end + 1;
            } else if (c == '"') {
                while (true) {
                    if (i >= s.length()) throw new IllegalArgumentException("No closing quotation");
                    char d = s.charAt(i++);
                    if (d == '"') break;
                    if (d == '\\' && i < s.length() && "\"\\$`\n".indexOf(s.charAt(i)) >= 0) d = s.charAt(i++);
                    word.append(d);
                }
            } else {
                word.append(c);
            }
        }
        if (word != null) words.add(word.toString());
        return words;
    }

    /** (front matter mapping, body) of a command or skill file. */
    @SuppressWarnings("unchecked")
    static Parsed parse(String text) throws IOException {
        Matcher m = FRONTMATTER.matcher(text);
        if (!m.matches()) return new Parsed(Map.of(), text);
        Map<String, Object> meta = YAML.readValue(m.group(1), LinkedHashMap.class);
        return new Parsed(meta == null ? Map.of() : meta, m.group(2));
    }

    /** The slash command a file creates: a skill's `name`, else its folder; a command's file name. */
    static String commandName(String path, Map<String, Object> meta) {
        String[] parts = path.split("/");
        if (parts[parts.length - 1].equals("SKILL.md")) {
            Object name = meta.get("name");
            return name == null || String.valueOf(name).isEmpty() ? parts[parts.length - 2] : String.valueOf(name);
        }
        String file = parts[parts.length - 1];
        return file.substring(0, file.length() - 3);
    }

    /** Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. candidates: {level: path}. */
    static String winner(Map<String, String> candidates) {
        for (String level : LEVELS) if (candidates.containsKey(level)) return candidates.get(level);
        return null;
    }

    /** Who can start it, and whether its description is always in context. */
    static Map<String, Boolean> invocation(Map<String, Object> meta) {
        boolean manual = Boolean.TRUE.equals(meta.get("disable-model-invocation"));
        boolean hidden = Boolean.FALSE.equals(meta.get("user-invocable"));
        Map<String, Boolean> out = new LinkedHashMap<>();
        out.put("you", !hidden);
        out.put("claude", !manual);
        out.put("description_in_context", !manual);
        return out;
    }

    private static List<String> names(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof String s) {
            Matcher m = Pattern.compile("[^\\s,(]+(?:\\([^)]*\\))?").matcher(s);
            while (m.find()) out.add(m.group());
        } else if (value instanceof List<?> l) {
            for (Object o : l) out.add(String.valueOf(o));
        }
        return out;
    }

    /** True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command. */
    static boolean preApproved(Map<String, Object> meta, String tool, String command) {
        for (String item : names(meta.get("allowed-tools"))) {
            int open = item.indexOf('(');
            String name = open < 0 ? item : item.substring(0, open);
            if (!name.equals(tool)) continue;
            String pattern = open < 0 ? "" : item.substring(open + 1);
            while (pattern.endsWith(")")) pattern = pattern.substring(0, pattern.length() - 1);
            if (pattern.isEmpty() || (pattern.endsWith(" *") && (command.equals(pattern.substring(0, pattern.length() - 2)) || command.startsWith(pattern.substring(0, pattern.length() - 1)))) || pattern.equals(command)) return true;
        }
        return false;
    }

    static boolean preApproved(Map<String, Object> meta, String tool) {
        return preApproved(meta, tool, "");
    }

    /** True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/**)` leaves the tool in place. */
    static boolean removed(Map<String, Object> meta, String tool) {
        return names(meta.get("disallowed-tools")).stream().filter(n -> !n.contains("(")).anyMatch(n -> n.equals(tool));
    }

    static String toolStatus(Map<String, Object> meta, String tool, String command) {
        if (removed(meta, tool)) return "removed";
        return preApproved(meta, tool, command) ? "pre-approved" : "permission settings decide";
    }

    static String toolStatus(Map<String, Object> meta, String tool) {
        return toolStatus(meta, tool, "");
    }

    /** Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input. */
    static String render(String body, String raw, List<String> names) {
        List<String> args = raw.isEmpty() ? List.of() : shlexSplit(raw);
        Map<String, String> named = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(names.size(), args.size()); i++) named.put(names.get(i), args.get(i));
        String alternatives = "ARGUMENTS\\[\\d+\\]|ARGUMENTS|\\d+" + names.stream().map(n -> "|" + Pattern.quote(n)).collect(Collectors.joining());
        Matcher m = Pattern.compile("\\$(" + alternatives + ")").matcher(body);
        Pattern indexed = Pattern.compile("ARGUMENTS\\[(\\d+)\\]|(\\d+)");
        boolean used = false;
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            used = true;
            String token = m.group(1);
            String value;
            if (token.equals("ARGUMENTS")) value = raw;
            else {
                Matcher index = indexed.matcher(token);
                if (index.matches()) {
                    int i = Integer.parseInt(index.group(1) != null ? index.group(1) : index.group(2));
                    value = i < args.size() ? args.get(i) : m.group(0); // an indexed placeholder with no argument stays as written
                } else value = named.getOrDefault(token, ""); // a named placeholder with no argument is empty
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        String result = out.toString();
        if (!raw.isEmpty() && !used) result = result.replaceFirst("\\n*$", "") + "\nARGUMENTS: " + raw + "\n";
        return result;
    }

    static String render(String body, String raw) {
        return render(body, raw, List.of());
    }

    /** The subagent a forked skill runs in, or null when it runs in the conversation. The subagent does not see the conversation. */
    static String forkAgent(Map<String, Object> meta) {
        if (!"fork".equals(meta.get("context"))) return null;
        Object agent = meta.get("agent");
        return agent == null || String.valueOf(agent).isEmpty() ? "general-purpose" : String.valueOf(agent);
    }

    static final String SKILL = """
        ---
        name: release-tag
        description: Tag a release and push the tag. Use when the user asks to cut a release.
        disable-model-invocation: true
        argument-hint: "[version]"
        arguments: [version]
        allowed-tools: Bash(git tag *) Bash(git push origin *)
        disallowed-tools: Edit
        ---
        Create the tag $version and push it.
        """;

    public static void main(String[] args) throws IOException {
        Parsed skill = parse(SKILL);
        System.out.println("name: " + commandName(".claude/skills/release-tag/SKILL.md", skill.meta()) + " | legacy file: " + commandName(".claude/commands/standup.md", Map.of()));
        System.out.println("winner of three same-name skills: " + winner(Map.of("project", "p/SKILL.md", "personal", "u/SKILL.md")));
        System.out.println("who can start it: " + py(invocation(skill.meta())));
        String[][] calls = {{"Bash", "git tag v1.2.0"}, {"Bash", "git push --force"}, {"Bash", "rm -rf build"}, {"Edit", ""}, {"Read", ""}};
        for (String[] c : calls) System.out.println(c[0] + " " + py(c[1]) + ": " + toolStatus(skill.meta(), c[0], c[1]));
        System.out.println("bare Bash allowed: " + py(preApproved(Map.of("allowed-tools", "Bash"), "Bash", "rm -rf build")) + " | scoped disallow removes Edit: " + py(removed(Map.of("disallowed-tools", "Edit(src/**)"), "Edit")));
        System.out.println("render: " + render(skill.body(), "v1.2.0", List.of("version")).strip());
        System.out.println("render, no placeholder: " + py(render("Review the change.\n", "123")));
        System.out.println("quoted: " + render("first=$0 second=$1", "\"hello world\" second"));
        System.out.println("fork: " + py2(forkAgent(Map.of("context", "fork"))) + " " + py2(forkAgent(Map.of("context", "fork", "agent", "Explore"))) + " " + py2(forkAgent(Map.of())));
    }

    /** Python's str() of a string or None. */
    private static String py2(String s) {
        return s == null ? "None" : s;
    }
}
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
```kotlin
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import harness.Show.py

private val log = System.getLogger("skill_model")

/**
 * What a command or skill file means to Claude Code: its slash name, who may start it, which tools it pre-approves or removes, and how arguments fill its text.
 *
 * The model follows the Claude Code skills documentation read on 2026-10-03 (v2.1.286): `.claude/commands/deploy.md` and `.claude/skills/deploy/SKILL.md` both create
 * `/deploy`; `allowed-tools` pre-approves for the turn and does not restrict; a bare name in `disallowed-tools` removes a tool while the skill is active;
 * indexed arguments use shell-style quoting; an invocation whose arguments no placeholder receives gets `ARGUMENTS: <input>` appended. Reading a file is plain data work:
 * the front matter is YAML, read with Jackson.
 */
val LEVELS = listOf("enterprise", "personal", "project") // the order in which a skill name is resolved: the first level wins
private val YAML = ObjectMapper(YAMLFactory())
private val FRONTMATTER = Regex("""^---\n(.*?)\n---\n?(.*)$""", RegexOption.DOT_MATCHES_ALL)

typealias Meta = Map<String, Any?>

/** The front matter of a command or skill file, and its body. */
data class Parsed(val meta: Meta, val body: String)

/** The words of a command line as a POSIX shell would split them (quotes and backslashes); a bad quote is an error. */
fun shlexSplit(s: String): List<String> {
    val words = mutableListOf<String>()
    var word: StringBuilder? = null
    var i = 0
    while (i < s.length) {
        val c = s[i++]
        if (c.isWhitespace()) {
            word?.let { words += it.toString() }
            word = null
            continue
        }
        val w = word ?: StringBuilder().also { word = it }
        when (c) {
            '\\' -> {
                require(i < s.length) { "No escaped character" }
                w.append(s[i++])
            }
            '\'' -> {
                val end = s.indexOf('\'', i)
                require(end >= 0) { "No closing quotation" }
                w.append(s, i, end)
                i = end + 1
            }
            '"' -> while (true) {
                require(i < s.length) { "No closing quotation" }
                var d = s[i++]
                if (d == '"') break
                if (d == '\\' && i < s.length && s[i] in "\"\\$`\n") d = s[i++]
                w.append(d)
            }
            else -> w.append(c)
        }
    }
    word?.let { words += it.toString() }
    return words
}

/** (front matter mapping, body) of a command or skill file. */
@Suppress("UNCHECKED_CAST")
fun parse(text: String): Parsed {
    val m = FRONTMATTER.matchEntire(text) ?: return Parsed(emptyMap(), text)
    val meta = YAML.readValue(m.groupValues[1], LinkedHashMap::class.java) as Meta?
    return Parsed(meta ?: emptyMap(), m.groupValues[2])
}

/** The slash command a file creates: a skill's `name`, else its folder; a command's file name. */
fun commandName(path: String, meta: Meta): String {
    val parts = path.split("/")
    if (parts.last() == "SKILL.md") return meta["name"]?.toString()?.takeIf { it.isNotEmpty() } ?: parts[parts.size - 2]
    return parts.last().dropLast(3)
}

/** Of several skills with one name, the one whose level wins: enterprise over personal, personal over project. candidates: {level: path}. */
fun winner(candidates: Map<String, String>): String? = LEVELS.firstOrNull { it in candidates }?.let { candidates[it] }

/** Who can start it, and whether its description is always in context. */
fun invocation(meta: Meta): Map<String, Boolean> {
    val manual = meta["disable-model-invocation"] == true
    val hidden = meta["user-invocable"] == false
    return linkedMapOf("you" to !hidden, "claude" to !manual, "description_in_context" to !manual)
}

private fun names(value: Any?): List<String> = when (value) {
    is String -> Regex("""[^\s,(]+(?:\([^)]*\))?""").findAll(value).map { it.value }.toList()
    is List<*> -> value.map { it.toString() }
    else -> emptyList()
}

/** True when `allowed-tools` lists the tool bare or with a pattern the command matches: `Bash(git tag *)` covers `git tag v1`, and a bare `Bash` covers every command. */
fun preApproved(meta: Meta, tool: String, command: String = ""): Boolean {
    for (item in names(meta["allowed-tools"])) {
        val name = item.substringBefore("(")
        if (name != tool) continue
        val pattern = if ("(" in item) item.substringAfter("(").trimEnd(')') else ""
        if (pattern.isEmpty() || (pattern.endsWith(" *") && (command == pattern.dropLast(2) || command.startsWith(pattern.dropLast(1)))) || pattern == command) return true
    }
    return false
}

/** True when a bare name in `disallowed-tools` takes the tool away while the skill is active; a scoped rule such as `Edit(src/...)` leaves the tool in place. */
fun removed(meta: Meta, tool: String): Boolean = tool in names(meta["disallowed-tools"]).filter { "(" !in it }

fun toolStatus(meta: Meta, tool: String, command: String = ""): String = when {
    removed(meta, tool) -> "removed"
    preApproved(meta, tool, command) -> "pre-approved"
    else -> "permission settings decide"
}

/** Fill $ARGUMENTS, $ARGUMENTS[N], $N (from 0) and named arguments; append `ARGUMENTS: <input>` when no placeholder received the input. */
fun render(body: String, raw: String, names: List<String> = emptyList()): String {
    val args = if (raw.isEmpty()) emptyList() else shlexSplit(raw)
    val named = names.zip(args).toMap()
    val alternatives = """ARGUMENTS\[\d+\]|ARGUMENTS|\d+""" + names.joinToString("") { "|" + Regex.escape(it) }
    val indexed = Regex("""ARGUMENTS\[(\d+)\]|(\d+)""")
    var used = false
    var out = Regex("""\$($alternatives)""").replace(body) { m ->
        used = true
        val token = m.groupValues[1]
        if (token == "ARGUMENTS") raw else {
            val index = indexed.matchEntire(token)
            if (index != null) {
                val i = (index.groups[1]?.value ?: index.groups[2]!!.value).toInt()
                if (i < args.size) args[i] else m.value // an indexed placeholder with no argument stays as written
            } else named[token] ?: "" // a named placeholder with no argument is empty
        }
    }
    if (raw.isNotEmpty() && !used) out = out.trimEnd('\n') + "\nARGUMENTS: $raw\n"
    return out
}

/** The subagent a forked skill runs in, or null when it runs in the conversation. The subagent does not see the conversation. */
fun forkAgent(meta: Meta): String? = if (meta["context"] != "fork") null else meta["agent"]?.toString()?.takeIf { it.isNotEmpty() } ?: "general-purpose"

val SKILL = """---
name: release-tag
description: Tag a release and push the tag. Use when the user asks to cut a release.
disable-model-invocation: true
argument-hint: "[version]"
arguments: [version]
allowed-tools: Bash(git tag *) Bash(git push origin *)
disallowed-tools: Edit
---
Create the tag ${'$'}version and push it.
"""

fun main() {
    val skill = parse(SKILL)
    println("name: ${commandName(".claude/skills/release-tag/SKILL.md", skill.meta)} | legacy file: ${commandName(".claude/commands/standup.md", emptyMap())}")
    println("winner of three same-name skills: ${winner(mapOf("project" to "p/SKILL.md", "personal" to "u/SKILL.md"))}")
    println("who can start it: ${py(invocation(skill.meta))}")
    for ((tool, command) in listOf("Bash" to "git tag v1.2.0", "Bash" to "git push --force", "Bash" to "rm -rf build", "Edit" to "", "Read" to "")) {
        println("$tool ${py(command)}: ${toolStatus(skill.meta, tool, command)}")
    }
    println("bare Bash allowed: ${py(preApproved(mapOf("allowed-tools" to "Bash"), "Bash", "rm -rf build"))} | scoped disallow removes Edit: ${py(removed(mapOf("disallowed-tools" to "Edit(src/**)"), "Edit"))}")
    println("render: ${render(skill.body, "v1.2.0", listOf("version")).trim()}")
    println("render, no placeholder: ${py(render("Review the change.\n", "123"))}")
    println("quoted: ${render("first=\$0 second=\$1", "\"hello world\" second")}")
    println("fork: ${forkAgent(mapOf("context" to "fork")) ?: "None"} ${forkAgent(mapOf("context" to "fork", "agent" to "Explore")) ?: "None"} ${forkAgent(emptyMap()) ?: "None"}")
}
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
<!-- /example -->

## Traps

1. **"Commands and skills are different features; migrate every command before the next release."** It is tempting because the guide lists them separately. The product rejects it: they are merged, a command file "work[s] the same way", and existing files keep working. A skill is preferred for new work because it can hold supporting files.
2. **"Save my own `review-pr` skill in `~/.claude/skills/`; it will sit beside the team's."** It is tempting because the two live in different places. The exam rejects it: of two skills with one name, personal wins over project, so the team's review no longer runs for you. Give the variant its own name.
3. **"Set `user-invocable: false` so that Claude cannot trigger the release by itself."** It is tempting because the field sounds like the one that controls invocation. The exam rejects it: that field hides the skill from you and leaves it available to Claude. The field that stops Claude is `disable-model-invocation: true`.
4. **"Describe it as 'A skill that reviews pull requests'; the name does the rest."** It is tempting because the name is clear to a person. The exam rejects it: the description is what Claude reads to decide when to load the skill, so it must say when to use it.

## Quiz

1. A project holds `.claude/commands/deploy.md`, and a teammate then adds `.claude/skills/deploy/SKILL.md`. What does `/deploy` run?
   - **a**: The command, because it was in the project first of the two
   - **b**: Both, with the command kept under a prefixed second name
   - **c**: The skill, since it takes precedence over the older format
   - **d**: Neither, because the duplicate name is refused outright

2. A session starts at a monorepo's root, and a skill sits in `packages/api/.claude/skills/`. So far Claude has worked only inside `docs/`. When can the skill first be used?
   - **a**: As soon as a file under `packages/api` is read or edited
   - **b**: Already, since it loaded at startup with the root skills
   - **c**: Never, since only the start folder and its parents are searched
   - **d**: Only after a new session is started inside `packages/api`

<details>
<summary>Answer key</summary>

1. **c**. The product merged the two formats and gives the skill precedence over a command file of the same name, whichever file came first. *a* is ruled out because "A skill beats a command file of the same name", and the order of arrival plays no part. *b* is ruled out because a prefixed second name belongs to two skills, where "the nested one is also reachable as" a path-prefixed command, while for a skill and a command file "the skill runs" alone. *d* is ruled out because "When a skill and a command file share a name, the skill runs", so the clash is settled and not refused.
2. **a**. A skill below the start folder is not found at launch; it loads when Claude first works with a file in that folder, which has not happened yet. *b* is ruled out because the launch search covers "the directory where you start and from every parent up to the repository root", and `packages/api` is neither. *c* is ruled out because "a skill in a folder below the start directory loads" later, when a file there is first used. *d* is ruled out because the skill loads "the first time Claude reads or edits a file in that subdirectory", inside the running session, and then the skills "stay available for the rest of the session".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
