import { logger } from "./logger.ts";
const log = logger("memory_loading");
/**
 * Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.
 *
 * The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
 * launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
 * import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
 * Nothing here starts Claude Code: the "project" is a set of file paths and a map of file texts.
 */
export const MAX_IMPORT_HOPS = 4;
const IMPORT = /(?<![\w`])@([\w./-]+)/g;

export function expandBraces(pattern: string): string[] {
  const match = /\{([^{}]*)\}/.exec(pattern);
  if (!match) return [pattern];
  return match[1].split(",").flatMap((option) => expandBraces(pattern.slice(0, match.index) + option + pattern.slice(match.index + match[0].length)));
}

function escapeRegex(c: string): string {
  return c.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export function globRegex(pattern: string): RegExp {
  let out = "";
  let i = 0;
  while (i < pattern.length) {
    if (pattern.startsWith("**/", i)) {
      out += "(?:.*/)?";
      i += 3;
    } else if (pattern.startsWith("**", i)) {
      out += ".*";
      i += 2;
    } else if (pattern[i] === "*") {
      out += "[^/]*";
      i += 1;
    } else if (pattern[i] === "?") {
      out += "[^/]";
      i += 1;
    } else {
      out += escapeRegex(pattern[i]);
      i += 1;
    }
  }
  return new RegExp("^" + out + "$");
}

/** `*` stays inside one folder, `**\/` crosses folders, braces expand: `*.md` is the project root only, `**\/*.ts` is every folder. */
export function globMatch(pattern: string, path: string): boolean {
  return expandBraces(pattern).some((p) => globRegex(p).test(path));
}

/** Rule files in context after Claude read or edited the `touched` files. A rule with no `paths` is always loaded. */
export function rulesLoaded(rules: Record<string, string[] | null>, touched: string[]): string[] {
  return Object.entries(rules)
    .filter(([, paths]) => paths === null || paths.some((p) => touched.some((f) => globMatch(p, f))))
    .map(([name]) => name);
}

const words = (path: string) => path.split("/").filter((p) => p !== "");

/** Memory files loaded when a session starts in `cwd`: the directories from the root down to cwd, CLAUDE.md then CLAUDE.local.md in each. */
export function launchFiles(tree: Set<string>, cwd = ""): string[] {
  const parts = words(cwd);
  const folders = [""].concat(parts.map((_, i) => parts.slice(0, i + 1).join("/")));
  const found: string[] = [];
  for (const folder of folders) {
    for (const name of ["CLAUDE.md", ".claude/CLAUDE.md", "CLAUDE.local.md"]) {
      const path = `${folder}/${name}`.replace(/^\/+/, "");
      if (tree.has(path)) found.push(path);
    }
  }
  return found;
}

/** Memory files below cwd that join the context when Claude reads a file in their folder (or below it), nearest to cwd first. */
export function onDemandFiles(tree: Set<string>, cwd: string, touched: string[]): string[] {
  const found: string[] = [];
  for (const file of touched) {
    const parts = file.split("/").slice(0, -1);
    for (let i = words(cwd).length + 1; i <= parts.length; i++) {
      for (const name of ["CLAUDE.md", "CLAUDE.local.md"]) {
        const path = parts.slice(0, i).concat([name]).join("/");
        if (tree.has(path) && !found.includes(path)) found.push(path);
      }
    }
  }
  return found;
}

/** AGENTS.md is read only when no CLAUDE.md or CLAUDE.local.md is found in the working directory or above it. */
export function agentsMdRead(tree: Set<string>, cwd = ""): boolean {
  return launchFiles(tree, cwd).length === 0 && tree.has("AGENTS.md");
}

/** Files pulled in by @path imports, in load order, relative to the importing file, at most `hops` deep; code spans and fences are skipped. */
export function importsOf(path: string, texts: Record<string, string>, hops = MAX_IMPORT_HOPS): string[] {
  const found: string[] = [];
  const visit = (file: string, left: number) => {
    const text = (texts[file] ?? "").replace(/```[\s\S]*?```|`[^`]*`/g, "");
    const base = file.includes("/") ? file.slice(0, file.lastIndexOf("/")) : "";
    for (const m of text.matchAll(IMPORT)) {
      const target = words(`${base}/${m[1]}`).join("/");
      if (target in texts && !found.includes(target)) {
        found.push(target);
        if (left > 1) visit(target, left - 1);
      }
    }
  };
  visit(path, hops);
  return found;
}

/** The @path references of a file that name no file: a typo here silently imports nothing. */
export function unresolvedImports(path: string, texts: Record<string, string>): string[] {
  const text = (texts[path] ?? "").replace(/```[\s\S]*?```|`[^`]*`/g, "");
  const base = path.includes("/") ? path.slice(0, path.lastIndexOf("/")) : "";
  return [...text.matchAll(IMPORT)].map((m) => m[1]).filter((ref) => !(words(`${base}/${ref}`).join("/") in texts));
}

/** Lines the files put into context. An import does not save any: the imported file loads at launch too. */
export function contextLines(paths: string[], texts: Record<string, string>): number {
  return paths.reduce((sum, p) => sum + texts[p].split("\n").length, 0);
}

function main() {
  const tree = new Set(["CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md"]);
  const rules = { "commit.md": null, "testing.md": ["**/*.test.{ts,tsx}"], "terraform.md": ["terraform/**/*"] };
  const show = (v: unknown) => JSON.stringify(v);
  console.log("launch in web/:", show(launchFiles(tree, "web")));
  console.log("reading web/ui/Button.tsx adds:", show(onDemandFiles(tree, "web", ["web/ui/Button.tsx"])));
  console.log("AGENTS.md read:", agentsMdRead(tree), "- without any CLAUDE.md:", agentsMdRead(new Set(["AGENTS.md"])));
  for (const touched of [["web/ui/Button.test.tsx"], ["terraform/main.tf"], ["README.md"]]) console.log(`touching ${touched[0]}:`, show(rulesLoaded(rules, touched)));
  for (const [pattern, path] of [["*.md", "README.md"], ["*.md", "docs/guide.md"], ["**/*.ts", "a/b/c.ts"], ["src/**/*.{ts,tsx}", "src/ui/x.tsx"]]) {
    console.log(`${pattern} matches ${path}: ${globMatch(pattern, path)}`);
  }
  const texts = { "CLAUDE.md": "See @docs/a.md and `@not-an-import`", "docs/a.md": "@b.md", "docs/b.md": "@c.md", "docs/c.md": "@d.md", "docs/d.md": "@e.md", "docs/e.md": "x" };
  console.log("imports:", show(importsOf("CLAUDE.md", texts)));
  console.log("unresolved:", show(unresolvedImports("CLAUDE.md", { ...texts, "CLAUDE.md": texts["CLAUDE.md"] + " and @docs/typo.md" })));
}

if (import.meta.main) main();
