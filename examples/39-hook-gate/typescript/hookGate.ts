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
