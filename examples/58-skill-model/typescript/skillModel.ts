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
