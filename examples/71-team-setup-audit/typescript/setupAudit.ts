// Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.
//
// The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
// documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
// at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
import { existsSync, readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

export const HERE = fileURLToPath(new URL("..", import.meta.url));

/** A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character. */
export function globRegex(glob: string): RegExp {
  let out = "";
  for (let i = 0; i < glob.length; ) {
    if (glob.startsWith("**/", i)) { out += "(?:.*/)?"; i += 3; }
    else if (glob.startsWith("**", i)) { out += ".*"; i += 2; }
    else if (glob[i] === "*") { out += "[^/]*"; i += 1; }
    else if (glob[i] === "?") { out += "[^/]"; i += 1; }
    else { out += glob[i].replace(/[.+^${}()|[\]\\]/g, "\\$&"); i += 1; }
  }
  return new RegExp(`^${out}$`);
}

/** The paths list of a rule file's front matter, or null when it has none (the rule then loads at launch). */
export function rulePaths(text: string): string[] | null {
  const head = text.match(/^---\n([\s\S]*?)\n---\n/);
  const listed = head ? `${head[1]}\n`.match(/^paths:\s*\n((?:[ \t]+-[ \t]+.*\n?)+)/m) : null;
  if (!listed) return null;
  return listed[1].split("\n").filter((l) => l.trim()).map((l) => l.replace(/^\s*-\s+/, "").trim().replace(/^["']|["']$/g, ""));
}

const list = (dir: string, keep: (name: string) => boolean) => (existsSync(dir) ? readdirSync(dir).filter(keep).sort() : []);

export function load(root: string) {
  const rules: Record<string, string[] | null> = {};
  for (const name of list(join(root, ".claude/rules"), (n) => n.endsWith(".md"))) rules[name] = rulePaths(readFileSync(join(root, ".claude/rules", name), "utf8"));
  const commands = list(join(root, ".claude/commands"), (n) => n.endsWith(".md"));
  const files = readFileSync(join(root, "files.txt"), "utf8").split(/\s+/).filter(Boolean);
  return { files, memory: readFileSync(join(root, "CLAUDE.md"), "utf8"), rules, commands, settings: JSON.parse(readFileSync(join(root, ".claude/settings.json"), "utf8")) };
}

const matches = (paths: string[] | null, file: string) => (paths ?? []).some((g) => globRegex(g).test(file));

export function rulesFor(rules: Record<string, string[] | null>, file: string): string[] {
  return Object.entries(rules).filter(([, paths]) => paths === null || matches(paths, file)).map(([name]) => name);
}

export function audit(root: string): string[] {
  const { files, memory, rules, commands, settings } = load(root);
  const found: string[] = [];
  const sections = (memory.match(/^## /gm) ?? []).length;
  if (Object.keys(rules).length === 0 && sections >= 3) found.push(`all-in-root: ${sections} sections in CLAUDE.md and no rule files`);
  for (const [name, paths] of Object.entries(rules)) {
    if (paths === null) found.push(`rule-loads-always: ${name}`);
    else if (!files.some((f) => matches(paths, f))) found.push(`rule-matches-nothing: ${name}`);
  }
  for (const f of files) if (/\.test\.tsx?$/.test(f) && !Object.values(rules).some((p) => matches(p, f))) found.push(`test-uncovered: ${f}`);
  if (commands.length === 0) found.push("no-shared-command");
  const perms = settings.permissions ?? {};
  if (!(perms.deny ?? []).includes("Read(./.env)")) found.push("env-readable");
  if ((perms.allow ?? []).includes("Bash")) found.push("bare-bash-allowed");
  return found;
}

function main() {
  for (const name of ["project-before", "project-after"]) {
    const root = join(HERE, name);
    const { files, memory, rules, commands } = load(root);
    console.log(`${name}: CLAUDE.md ${memory.replace(/\n$/, "").split("\n").length} lines, ${Object.keys(rules).length} rule files, ${commands.length} shared commands`);
    const found = audit(root);
    for (const finding of found) console.log(`  finding: ${finding}`);
    if (found.length === 0) {
      console.log("  no findings");
      for (const f of files) console.log(`  ${f} <- ${rulesFor(rules, f).join(", ") || "CLAUDE.md only"}`);
    }
  }
}

if (import.meta.main) main();
