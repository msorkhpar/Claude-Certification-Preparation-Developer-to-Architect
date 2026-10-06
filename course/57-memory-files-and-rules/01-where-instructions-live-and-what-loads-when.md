# Where instructions live and what loads when

**Level:** Architect · **Module 57:** Memory files and rules · **Page 1 of 2**
**Exams:** A3.1; S2

**After this page you can** say which file an instruction belongs in (managed, user, project, local or directory), predict which memory files are in context for a session started in a given folder, write `@` imports that expand, explain when `AGENTS.md` is read, and check what loaded with `/memory` and `/context` instead of guessing.

Checked on 2026-10-03 against the Claude Code documentation pages "How Claude remembers your project" (memory) and "Best practices for Claude Code", which document behaviour up to Claude Code v2.1.286. The `InstructionsLoaded` sentence is quoted from the troubleshooting section of the memory page; the hook's own reference page was not read, so nothing here says what the hook receives. Nothing here was run against a live session: the example is a model of the documented loading rules, written as plain code over file paths and texts. This page deepens module 38 (the memory file, its size and where the layers sit) and module 27 (what each extension costs), and it does not repeat them. Rules with `paths` and the size of the files are the second page.

> **Exam guide and current product.** *What the guide states (task statement 3.1), and so what the exam keys:* the hierarchy has three levels, user (`~/.claude/CLAUDE.md`), project (`.claude/CLAUDE.md` or the root file) and directory (CLAUDE.md files in subdirectories); user-level instructions "are not shared with teammates via version control"; `@import` keeps a file modular; `.claude/rules/` is the alternative to one monolithic file; and the `/memory` command is how you "verify which memory files are loaded". *What the current product does (documentation checked 2026-10-03):* there are more places than three. A managed policy file sits above the user file, `./CLAUDE.local.md` holds personal notes for one project, `AGENTS.md` is read in one situation, and auto memory is a fourth kind of memory that Claude writes itself. `/memory` "lists your CLAUDE.md, CLAUDE.local.md, and other memory file locations across user and project scopes", including entries "for files that don't exist yet"; the command that shows what loaded into the current session is `/context`. On the exam, answer the scoping question with the guide's three levels and its rule that a user-level file never reaches a teammate; when you debug a real session, run `/context`.

## Why it matters

A new engineer joins a team and Claude Code in their checkout ignores the team's rule that every endpoint validates its input. The rule exists: a senior engineer wrote it into their own `~/.claude/CLAUDE.md` months ago and has been getting the right behaviour ever since. Nothing in the repository carries it, so nobody else gets it. This is the exam's scenario S2 in its simplest form, and the question behind it is always the same one: at which level does an instruction live, and who receives it. The remaining failures come from the same ignorance of loading: a file in a subdirectory that has not been read yet, an import with a typo that imports nothing, and an `AGENTS.md` that is never read because a `CLAUDE.md` sits one folder up.

## The idea

### Five places, one load order

Memory files are plain Markdown that Claude reads "at the start of every session". The documentation lists them "in load order, from broadest scope to most specific, so a project instruction appears in context after a user instruction".

| Scope | Where | Who gets it |
|---|---|---|
| Managed policy | `/etc/claude-code/CLAUDE.md` on Linux and WSL, `/Library/Application Support/ClaudeCode/CLAUDE.md` on macOS | Everyone on the machine, and individual settings cannot exclude it |
| User | `~/.claude/CLAUDE.md` | You, in every project |
| Project | `./CLAUDE.md` or `./.claude/CLAUDE.md` | The team, "via source control" |
| Local | `./CLAUDE.local.md`, added to `.gitignore` | You, in this project |
| Directory | `CLAUDE.md` in a subdirectory | Whoever works in that subdirectory |

The first three rows are the guide's hierarchy plus the organisation layer the product added. The two questions the exam asks of the table are who receives the instruction and whether it travels with the repository. A user-level instruction does not, which is the reason the engineer in the scenario never shared it, and the fix is to move the rule into the project file and commit it. A personal note about one project, such as a sandbox address, belongs in the local file, which is why that file is ignored by git.

The managed layer has a companion that is easy to confuse with it. A managed CLAUDE.md is for behaviour ("Code style and quality guidelines", "Data handling and compliance reminders"); blocking a tool, a command or a path is done with managed settings, because "Claude treats them as context, not enforced configuration".

### What loads at launch and what loads later

Files in the directory hierarchy "above the working directory are loaded at launch". Start in `web/ui/` and Claude loads `web/ui/CLAUDE.md`, `web/CLAUDE.md` and the root file, plus any `CLAUDE.local.md` beside them. The files are not alternatives: "All discovered files are concatenated into context rather than overriding each other", ordered from the filesystem root down to the working directory, so "instructions closer to where you launched Claude are read last". In each folder the local file comes after the shared one.

Files below the working directory behave differently. A CLAUDE.md in a subdirectory "load[s] on demand when Claude reads files in those directories". Start in the repository root, and `web/CLAUDE.md` is not in context until Claude reads something under `web/`. Two consequences follow. A convention that must hold from the first message cannot live in a subdirectory file, and an instruction can seem to appear halfway through a session, which is the file loading the moment Claude opened a file there. The same on-demand behaviour is why the documentation tells you, after `/compact`, that nested files "reload as Claude reads files they apply to" while the root file is re-read from disk at once.

### Imports keep a file modular and save nothing

A CLAUDE.md "can import additional files using `@path/to/import` syntax". Imported files "are expanded and loaded into context at launch alongside the CLAUDE.md that references them". Relative paths "resolve relative to the file containing the import, not the working directory", and imports can import others "with a maximum depth of four hops". Import parsing "skips Markdown code spans and fenced code blocks", so writing `` `@README` `` mentions a file without importing it, and writing `@README` outside backticks imports it.

Two properties matter for the exam. An import changes where text is kept and not how much of it is loaded: imports "help you organize a long file but don't reduce its context cost, because imported files also load at launch". A path that names no file imports nothing and says nothing about it, so a typo is a silent loss; the example's `unresolved_imports` finds it, and in a real session `/context` shows what actually loaded. Imports that point outside the project (a file in your home folder) trigger an approval dialog the first time, because a committed file could otherwise pull in anything.

### AGENTS.md is read in one situation

Claude Code "can read `AGENTS.md` as your project instructions", with a rule that surprises people: "By default, Claude reads `AGENTS.md` only when you have no `CLAUDE.md` in your working directory or above it." A `CLAUDE.md`, a `.claude/CLAUDE.md` or a `CLAUDE.local.md` in the working directory or any directory above it counts, so adding a personal local file to a repository that relies on `AGENTS.md` quietly stops Claude from reading it. The user file and the managed file do not count. The documented ways out are to import it (`@AGENTS.md` in the CLAUDE.md, which then reads both) or to set **Project instructions** to `claude-md-and-agents-md` in `/config`. A version note belongs with the claim: reading `AGENTS.md` directly "requires Claude Code v2.1.277 or later".

### Context, not enforcement

Memory is advice with good delivery. The documentation says it plainly: "CLAUDE.md content is delivered as a user message after the system prompt, not as part of the system prompt itself. Claude reads it and tries to follow it, but there's no guarantee of strict compliance, especially for vague or conflicting instructions." For an instruction that "must run at a specific point, such as before every commit or after each file edit", the documented answer is a hook, and for a path or command that must not be touched it is a permission rule (module 38, module 49). If two files give different guidance for the same behaviour, "Claude may pick one arbitrarily", so the review of memory files includes a search for contradictions across the levels.

Auto memory is the other kind. Claude writes its own notes to `~/.claude/projects/<project>/memory/`, loads "the first 200 lines of `MEMORY.md`, or the first 25KB, whichever comes first" at the start of every conversation, and keeps them on the machine: the files "are not shared across machines or cloud environments". It is a place where Claude records what it learned, and not a place for the team's rules.

### Seeing what loaded

Three tools answer "what does Claude actually have". `/context` lists the memory files that loaded into the session under **Memory files**; if a file is missing there, "Claude can't see it". `/memory` lists the memory file locations and opens them in your editor, including ones you have not created yet. The `InstructionsLoaded` hook logs "which `CLAUDE.md` and rules files are loaded, when they load, and why", which is the way to watch lazy loading from a script. `AGENTS.md` shows in `/memory` when Claude read it, from v2.1.280.

### The example

The example is a model of the loading rules over paths and texts, in the same style as module 38's: the launch set for a folder, the files that join when Claude reads below it, whether `AGENTS.md` is read, the rules a set of touched files brings in (the second page), glob matching, and the import expansion with its hop limit and its report of references that name no file.

<!-- example: m57-memory-loading tabs: python,typescript,java,kotlin -->
```python
"""Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.

The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
Nothing here starts Claude Code: the "project" is a list of file paths and a dict of file texts.
"""
import re
import logging

log = logging.getLogger(__name__)

MAX_IMPORT_HOPS = 4
IMPORT = re.compile(r"(?<![\w`])@([\w./-]+)")


def expand_braces(pattern):
    match = re.search(r"\{([^{}]*)\}", pattern)
    if not match:
        return [pattern]
    return [p for option in match.group(1).split(",") for p in expand_braces(pattern[: match.start()] + option + pattern[match.end():])]


def glob_regex(pattern):
    out, i = "", 0
    while i < len(pattern):
        if pattern.startswith("**/", i):
            out, i = out + "(?:.*/)?", i + 3
        elif pattern.startswith("**", i):
            out, i = out + ".*", i + 2
        elif pattern[i] == "*":
            out, i = out + "[^/]*", i + 1
        elif pattern[i] == "?":
            out, i = out + "[^/]", i + 1
        else:
            out, i = out + re.escape(pattern[i]), i + 1
    return re.compile(out + "$")


def glob_match(pattern, path):
    """`*` stays inside one folder, `**/` crosses folders, braces expand: `*.md` is the project root only, `**/*.ts` is every folder."""
    return any(glob_regex(p).match(path) for p in expand_braces(pattern))


def rules_loaded(rules, touched):
    """Rule files in context after Claude read or edited the `touched` files. A rule with no `paths` is always loaded."""
    return [name for name, paths in rules.items() if paths is None or any(glob_match(p, f) for p in paths for f in touched)]


def launch_files(tree, cwd=""):
    """Memory files loaded when a session starts in `cwd`: the directories from the root down to cwd, CLAUDE.md then CLAUDE.local.md in each."""
    parts = [p for p in cwd.split("/") if p]
    folders = [""] + ["/".join(parts[: i + 1]) for i in range(len(parts))]
    found = []
    for folder in folders:
        for name in ("CLAUDE.md", ".claude/CLAUDE.md", "CLAUDE.local.md"):
            path = f"{folder}/{name}".lstrip("/")
            if path in tree:
                found.append(path)
    return found


def on_demand_files(tree, cwd, touched):
    """Memory files below cwd that join the context when Claude reads a file in their folder (or below it), nearest to cwd first."""
    found = []
    for file in touched:
        parts = file.split("/")[:-1]
        for i in range(len([p for p in cwd.split("/") if p]) + 1, len(parts) + 1):
            for name in ("CLAUDE.md", "CLAUDE.local.md"):
                path = "/".join(parts[:i] + [name])
                if path in tree and path not in found:
                    found.append(path)
    return found


def agents_md_read(tree, cwd=""):
    """AGENTS.md is read only when no CLAUDE.md or CLAUDE.local.md is found in the working directory or above it."""
    return not launch_files(tree, cwd) and "AGENTS.md" in tree


def imports_of(path, texts, hops=MAX_IMPORT_HOPS):
    """Files pulled in by @path imports, in load order, relative to the importing file, at most `hops` deep; code spans and fences are skipped."""
    found = []

    def visit(file, left):
        text = re.sub(r"```.*?```|`[^`]*`", "", texts.get(file, ""), flags=re.S)
        base = file.rsplit("/", 1)[0] if "/" in file else ""
        for ref in IMPORT.findall(text):
            target = "/".join(p for p in f"{base}/{ref}".split("/") if p)
            if target in texts and target not in found:
                found.append(target)
                if left > 1:
                    visit(target, left - 1)

    visit(path, hops)
    return found


def unresolved_imports(path, texts):
    """The @path references of a file that name no file: a typo here silently imports nothing."""
    text = re.sub(r"```.*?```|`[^`]*`", "", texts.get(path, ""), flags=re.S)
    base = path.rsplit("/", 1)[0] if "/" in path else ""
    return [ref for ref in IMPORT.findall(text) if "/".join(p for p in f"{base}/{ref}".split("/") if p) not in texts]


def context_lines(paths, texts):
    """Lines the files put into context. An import does not save any: the imported file loads at launch too."""
    return sum(len(texts[p].splitlines()) for p in paths)


def main():
    tree = {"CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md"}
    rules = {"commit.md": None, "testing.md": ["**/*.test.{ts,tsx}"], "terraform.md": ["terraform/**/*"]}
    print("launch in web/:", launch_files(tree, "web"))
    print("reading web/ui/Button.tsx adds:", on_demand_files(tree, "web", ["web/ui/Button.tsx"]))
    print("AGENTS.md read:", agents_md_read(tree), "- without any CLAUDE.md:", agents_md_read({"AGENTS.md"}))
    for touched in (["web/ui/Button.test.tsx"], ["terraform/main.tf"], ["README.md"]):
        print(f"touching {touched[0]}: {rules_loaded(rules, touched)}")
    for pattern, path in (("*.md", "README.md"), ("*.md", "docs/guide.md"), ("**/*.ts", "a/b/c.ts"), ("src/**/*.{ts,tsx}", "src/ui/x.tsx")):
        print(f"{pattern} matches {path}: {glob_match(pattern, path)}")
    texts = {"CLAUDE.md": "See @docs/a.md and `@not-an-import`", "docs/a.md": "@b.md", "docs/b.md": "@c.md", "docs/c.md": "@d.md", "docs/d.md": "@e.md", "docs/e.md": "x"}
    print("imports:", imports_of("CLAUDE.md", texts))
    print("unresolved:", unresolved_imports("CLAUDE.md", {**texts, "CLAUDE.md": texts["CLAUDE.md"] + " and @docs/typo.md"}))


if __name__ == "__main__":
    main()
```
```text
launch in web/: ['CLAUDE.md', 'CLAUDE.local.md', 'web/CLAUDE.md']
reading web/ui/Button.tsx adds: ['web/ui/CLAUDE.md']
AGENTS.md read: False - without any CLAUDE.md: True
touching web/ui/Button.test.tsx: ['commit.md', 'testing.md']
touching terraform/main.tf: ['commit.md', 'terraform.md']
touching README.md: ['commit.md']
*.md matches README.md: True
*.md matches docs/guide.md: False
**/*.ts matches a/b/c.ts: True
src/**/*.{ts,tsx} matches src/ui/x.tsx: True
imports: ['docs/a.md', 'docs/b.md', 'docs/c.md', 'docs/d.md']
unresolved: ['docs/typo.md']
```
```typescript
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
```
```text
launch in web/: ["CLAUDE.md","CLAUDE.local.md","web/CLAUDE.md"]
reading web/ui/Button.tsx adds: ["web/ui/CLAUDE.md"]
AGENTS.md read: false - without any CLAUDE.md: true
touching web/ui/Button.test.tsx: ["commit.md","testing.md"]
touching terraform/main.tf: ["commit.md","terraform.md"]
touching README.md: ["commit.md"]
*.md matches README.md: true
*.md matches docs/guide.md: false
**/*.ts matches a/b/c.ts: true
src/**/*.{ts,tsx} matches src/ui/x.tsx: true
imports: ["docs/a.md","docs/b.md","docs/c.md","docs/d.md"]
unresolved: ["docs/typo.md"]
```
```java
import static harness.Show.py;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.
 *
 * <p>The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
 * launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
 * import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
 * Nothing here starts Claude Code: the "project" is a list of file paths and a map of file texts.
 */
public final class MemoryLoading {
    private static final System.Logger LOG = System.getLogger(MemoryLoading.class.getName());
    static final int MAX_IMPORT_HOPS = 4;
    static final Pattern IMPORT = Pattern.compile("(?<![\\w`])@([\\w./-]+)");
    private static final Pattern CODE = Pattern.compile("```.*?```|`[^`]*`", Pattern.DOTALL);
    private static final Pattern BRACES = Pattern.compile("\\{([^{}]*)\\}");

    static List<String> expandBraces(String pattern) {
        Matcher match = BRACES.matcher(pattern);
        if (!match.find()) return List.of(pattern);
        List<String> out = new ArrayList<>();
        for (String option : match.group(1).split(",", -1)) out.addAll(expandBraces(pattern.substring(0, match.start()) + option + pattern.substring(match.end())));
        return out;
    }

    static Pattern globRegex(String pattern) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < pattern.length()) {
            if (pattern.startsWith("**/", i)) { out.append("(?:.*/)?"); i += 3; }
            else if (pattern.startsWith("**", i)) { out.append(".*"); i += 2; }
            else if (pattern.charAt(i) == '*') { out.append("[^/]*"); i += 1; }
            else if (pattern.charAt(i) == '?') { out.append("[^/]"); i += 1; }
            else { out.append(Pattern.quote(String.valueOf(pattern.charAt(i)))); i += 1; }
        }
        return Pattern.compile(out.toString());
    }

    /** `*` stays inside one folder, a `**` folder segment crosses folders, braces expand: `*.md` is the project root only, any-folder-prefix plus `*.ts` is every folder. */
    static boolean globMatch(String pattern, String path) {
        return expandBraces(pattern).stream().anyMatch(p -> globRegex(p).matcher(path).matches());
    }

    /** Rule files in context after Claude read or edited the `touched` files. A rule with no `paths` is always loaded. */
    static List<String> rulesLoaded(Map<String, List<String>> rules, List<String> touched) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<String>> r : rules.entrySet()) {
            if (r.getValue() == null || r.getValue().stream().anyMatch(p -> touched.stream().anyMatch(f -> globMatch(p, f)))) out.add(r.getKey());
        }
        return out;
    }

    private static List<String> segments(String path) {
        return Arrays.stream(path.split("/")).filter(p -> !p.isEmpty()).toList();
    }

    /** Memory files loaded when a session starts in `cwd`: the directories from the root down to cwd, CLAUDE.md then CLAUDE.local.md in each. */
    static List<String> launchFiles(Set<String> tree, String cwd) {
        List<String> parts = segments(cwd);
        List<String> folders = new ArrayList<>(List.of(""));
        for (int i = 0; i < parts.size(); i++) folders.add(String.join("/", parts.subList(0, i + 1)));
        List<String> found = new ArrayList<>();
        for (String folder : folders) {
            for (String name : List.of("CLAUDE.md", ".claude/CLAUDE.md", "CLAUDE.local.md")) {
                String path = (folder + "/" + name).replaceFirst("^/+", "");
                if (tree.contains(path)) found.add(path);
            }
        }
        return found;
    }

    /** Memory files below cwd that join the context when Claude reads a file in their folder (or below it), nearest to cwd first. */
    static List<String> onDemandFiles(Set<String> tree, String cwd, List<String> touched) {
        List<String> found = new ArrayList<>();
        for (String file : touched) {
            String[] all = file.split("/");
            List<String> parts = Arrays.asList(all).subList(0, all.length - 1);
            for (int i = segments(cwd).size() + 1; i <= parts.size(); i++) {
                for (String name : List.of("CLAUDE.md", "CLAUDE.local.md")) {
                    List<String> p = new ArrayList<>(parts.subList(0, i));
                    p.add(name);
                    String path = String.join("/", p);
                    if (tree.contains(path) && !found.contains(path)) found.add(path);
                }
            }
        }
        return found;
    }

    /** AGENTS.md is read only when no CLAUDE.md or CLAUDE.local.md is found in the working directory or above it. */
    static boolean agentsMdRead(Set<String> tree, String cwd) {
        return launchFiles(tree, cwd).isEmpty() && tree.contains("AGENTS.md");
    }

    static boolean agentsMdRead(Set<String> tree) {
        return agentsMdRead(tree, "");
    }

    private static String target(String file, String ref) {
        String base = file.contains("/") ? file.substring(0, file.lastIndexOf('/')) : "";
        return String.join("/", segments(base + "/" + ref));
    }

    private static void visit(String file, int left, Map<String, String> texts, List<String> found) {
        String text = CODE.matcher(texts.getOrDefault(file, "")).replaceAll("");
        Matcher m = IMPORT.matcher(text);
        while (m.find()) {
            String target = target(file, m.group(1));
            if (texts.containsKey(target) && !found.contains(target)) {
                found.add(target);
                if (left > 1) visit(target, left - 1, texts, found);
            }
        }
    }

    /** Files pulled in by @path imports, in load order, relative to the importing file, at most `hops` deep; code spans and fences are skipped. */
    static List<String> importsOf(String path, Map<String, String> texts, int hops) {
        List<String> found = new ArrayList<>();
        visit(path, hops, texts, found);
        return found;
    }

    static List<String> importsOf(String path, Map<String, String> texts) {
        return importsOf(path, texts, MAX_IMPORT_HOPS);
    }

    /** The @path references of a file that name no file: a typo here silently imports nothing. */
    static List<String> unresolvedImports(String path, Map<String, String> texts) {
        String text = CODE.matcher(texts.getOrDefault(path, "")).replaceAll("");
        List<String> out = new ArrayList<>();
        Matcher m = IMPORT.matcher(text);
        while (m.find()) if (!texts.containsKey(target(path, m.group(1)))) out.add(m.group(1));
        return out;
    }

    /** Lines the files put into context. An import does not save any: the imported file loads at launch too. */
    static long contextLines(List<String> paths, Map<String, String> texts) {
        return paths.stream().mapToLong(p -> texts.get(p).lines().count()).sum();
    }

    static Map<String, String> texts(String... kv) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    public static void main(String[] args) {
        Set<String> tree = new LinkedHashSet<>(List.of("CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md"));
        Map<String, List<String>> rules = new LinkedHashMap<>();
        rules.put("commit.md", null);
        rules.put("testing.md", List.of("**/*.test.{ts,tsx}"));
        rules.put("terraform.md", List.of("terraform/**/*"));
        System.out.println("launch in web/: " + py(launchFiles(tree, "web")));
        System.out.println("reading web/ui/Button.tsx adds: " + py(onDemandFiles(tree, "web", List.of("web/ui/Button.tsx"))));
        System.out.println("AGENTS.md read: " + py(agentsMdRead(tree)) + " - without any CLAUDE.md: " + py(agentsMdRead(Set.of("AGENTS.md"))));
        for (String touched : List.of("web/ui/Button.test.tsx", "terraform/main.tf", "README.md")) {
            System.out.println("touching " + touched + ": " + py(rulesLoaded(rules, List.of(touched))));
        }
        String[][] globs = {{"*.md", "README.md"}, {"*.md", "docs/guide.md"}, {"**/*.ts", "a/b/c.ts"}, {"src/**/*.{ts,tsx}", "src/ui/x.tsx"}};
        for (String[] g : globs) System.out.println(g[0] + " matches " + g[1] + ": " + py(globMatch(g[0], g[1])));
        Map<String, String> texts = texts("CLAUDE.md", "See @docs/a.md and `@not-an-import`", "docs/a.md", "@b.md", "docs/b.md", "@c.md", "docs/c.md", "@d.md", "docs/d.md", "@e.md", "docs/e.md", "x");
        System.out.println("imports: " + py(importsOf("CLAUDE.md", texts)));
        Map<String, String> withTypo = new LinkedHashMap<>(texts);
        withTypo.put("CLAUDE.md", texts.get("CLAUDE.md") + " and @docs/typo.md");
        System.out.println("unresolved: " + py(unresolvedImports("CLAUDE.md", withTypo)));
    }
}
```
```text
launch in web/: ['CLAUDE.md', 'CLAUDE.local.md', 'web/CLAUDE.md']
reading web/ui/Button.tsx adds: ['web/ui/CLAUDE.md']
AGENTS.md read: False - without any CLAUDE.md: True
touching web/ui/Button.test.tsx: ['commit.md', 'testing.md']
touching terraform/main.tf: ['commit.md', 'terraform.md']
touching README.md: ['commit.md']
*.md matches README.md: True
*.md matches docs/guide.md: False
**/*.ts matches a/b/c.ts: True
src/**/*.{ts,tsx} matches src/ui/x.tsx: True
imports: ['docs/a.md', 'docs/b.md', 'docs/c.md', 'docs/d.md']
unresolved: ['docs/typo.md']
```
```kotlin
import harness.Show.py

private val log = System.getLogger("memory_loading")

/**
 * Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.
 *
 * The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
 * launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
 * import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
 * Nothing here starts Claude Code: the "project" is a list of file paths and a map of file texts.
 */
const val MAX_IMPORT_HOPS = 4
val IMPORT = Regex("""(?<![\w`])@([\w./-]+)""")
private val CODE = Regex("""```.*?```|`[^`]*`""", RegexOption.DOT_MATCHES_ALL)
private val BRACES = Regex("""\{([^{}]*)\}""")

fun expandBraces(pattern: String): List<String> {
    val match = BRACES.find(pattern) ?: return listOf(pattern)
    return match.groupValues[1].split(",").flatMap { option -> expandBraces(pattern.substring(0, match.range.first) + option + pattern.substring(match.range.last + 1)) }
}

fun globRegex(pattern: String): Regex {
    val out = StringBuilder()
    var i = 0
    while (i < pattern.length) {
        when {
            pattern.startsWith("**/", i) -> { out.append("(?:.*/)?"); i += 3 }
            pattern.startsWith("**", i) -> { out.append(".*"); i += 2 }
            pattern[i] == '*' -> { out.append("[^/]*"); i += 1 }
            pattern[i] == '?' -> { out.append("[^/]"); i += 1 }
            else -> { out.append(Regex.escape(pattern[i].toString())); i += 1 }
        }
    }
    return Regex(out.toString())
}

/** `*` stays inside one folder, a `**` folder segment crosses folders, braces expand: `*.md` is the project root only, any-folder-prefix plus `*.ts` is every folder. */
fun globMatch(pattern: String, path: String): Boolean = expandBraces(pattern).any { globRegex(it).matches(path) }

/** Rule files in context after Claude read or edited the `touched` files. A rule with no `paths` is always loaded. */
fun rulesLoaded(rules: Map<String, List<String>?>, touched: List<String>): List<String> =
    rules.filter { (_, paths) -> paths == null || paths.any { p -> touched.any { f -> globMatch(p, f) } } }.keys.toList()

private fun segments(path: String): List<String> = path.split("/").filter { it.isNotEmpty() }

/** Memory files loaded when a session starts in `cwd`: the directories from the root down to cwd, CLAUDE.md then CLAUDE.local.md in each. */
fun launchFiles(tree: Set<String>, cwd: String = ""): List<String> {
    val parts = segments(cwd)
    val folders = listOf("") + parts.indices.map { parts.subList(0, it + 1).joinToString("/") }
    val found = mutableListOf<String>()
    for (folder in folders) {
        for (name in listOf("CLAUDE.md", ".claude/CLAUDE.md", "CLAUDE.local.md")) {
            val path = "$folder/$name".trimStart('/')
            if (path in tree) found += path
        }
    }
    return found
}

/** Memory files below cwd that join the context when Claude reads a file in their folder (or below it), nearest to cwd first. */
fun onDemandFiles(tree: Set<String>, cwd: String, touched: List<String>): List<String> {
    val found = mutableListOf<String>()
    for (file in touched) {
        val parts = file.split("/").dropLast(1)
        for (i in segments(cwd).size + 1..parts.size) {
            for (name in listOf("CLAUDE.md", "CLAUDE.local.md")) {
                val path = (parts.take(i) + name).joinToString("/")
                if (path in tree && path !in found) found += path
            }
        }
    }
    return found
}

/** AGENTS.md is read only when no CLAUDE.md or CLAUDE.local.md is found in the working directory or above it. */
fun agentsMdRead(tree: Set<String>, cwd: String = ""): Boolean = launchFiles(tree, cwd).isEmpty() && "AGENTS.md" in tree

private fun target(file: String, ref: String): String = segments("${if ("/" in file) file.substringBeforeLast("/") else ""}/$ref").joinToString("/")

/** Files pulled in by @path imports, in load order, relative to the importing file, at most `hops` deep; code spans and fences are skipped. */
fun importsOf(path: String, texts: Map<String, String>, hops: Int = MAX_IMPORT_HOPS): List<String> {
    val found = mutableListOf<String>()
    fun visit(file: String, left: Int) {
        val text = CODE.replace(texts[file] ?: "", "")
        for (ref in IMPORT.findAll(text).map { it.groupValues[1] }) {
            val target = target(file, ref)
            if (target in texts && target !in found) {
                found += target
                if (left > 1) visit(target, left - 1)
            }
        }
    }
    visit(path, hops)
    return found
}

/** The @path references of a file that name no file: a typo here silently imports nothing. */
fun unresolvedImports(path: String, texts: Map<String, String>): List<String> {
    val text = CODE.replace(texts[path] ?: "", "")
    return IMPORT.findAll(text).map { it.groupValues[1] }.filter { target(path, it) !in texts }.toList()
}

/** Lines the files put into context. An import does not save any: the imported file loads at launch too. */
fun contextLines(paths: List<String>, texts: Map<String, String>): Int = paths.sumOf { texts.getValue(it).lines().size - (if (texts.getValue(it).endsWith("\n")) 1 else 0) }

fun main() {
    val tree = setOf("CLAUDE.md", "CLAUDE.local.md", "AGENTS.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md")
    val rules = linkedMapOf("commit.md" to null, "testing.md" to listOf("**/*.test.{ts,tsx}"), "terraform.md" to listOf("terraform/**/*"))
    println("launch in web/: ${py(launchFiles(tree, "web"))}")
    println("reading web/ui/Button.tsx adds: ${py(onDemandFiles(tree, "web", listOf("web/ui/Button.tsx")))}")
    println("AGENTS.md read: ${py(agentsMdRead(tree))} - without any CLAUDE.md: ${py(agentsMdRead(setOf("AGENTS.md")))}")
    for (touched in listOf("web/ui/Button.test.tsx", "terraform/main.tf", "README.md")) println("touching $touched: ${py(rulesLoaded(rules, listOf(touched)))}")
    for ((pattern, path) in listOf("*.md" to "README.md", "*.md" to "docs/guide.md", "**/*.ts" to "a/b/c.ts", "src/**/*.{ts,tsx}" to "src/ui/x.tsx")) {
        println("$pattern matches $path: ${py(globMatch(pattern, path))}")
    }
    val texts = linkedMapOf(
        "CLAUDE.md" to "See @docs/a.md and `@not-an-import`", "docs/a.md" to "@b.md", "docs/b.md" to "@c.md", "docs/c.md" to "@d.md", "docs/d.md" to "@e.md", "docs/e.md" to "x",
    )
    println("imports: ${py(importsOf("CLAUDE.md", texts))}")
    println("unresolved: ${py(unresolvedImports("CLAUDE.md", texts + ("CLAUDE.md" to texts.getValue("CLAUDE.md") + " and @docs/typo.md")))}")
}
```
```text
launch in web/: ['CLAUDE.md', 'CLAUDE.local.md', 'web/CLAUDE.md']
reading web/ui/Button.tsx adds: ['web/ui/CLAUDE.md']
AGENTS.md read: False - without any CLAUDE.md: True
touching web/ui/Button.test.tsx: ['commit.md', 'testing.md']
touching terraform/main.tf: ['commit.md', 'terraform.md']
touching README.md: ['commit.md']
*.md matches README.md: True
*.md matches docs/guide.md: False
**/*.ts matches a/b/c.ts: True
src/**/*.{ts,tsx} matches src/ui/x.tsx: True
imports: ['docs/a.md', 'docs/b.md', 'docs/c.md', 'docs/d.md']
unresolved: ['docs/typo.md']
```
<!-- /example -->

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Put the team's rule in `~/.claude/CLAUDE.md`; every developer has one."** It is tempting because the user file loads in every project. The exam rejects it: that file is personal and is "not shared with teammates via version control", so a new teammate never receives it. The rule goes in the project file, committed.
2. **"Split the 600-line file into imports; the context cost falls."** It is tempting because the root file becomes short. The exam, and the documentation, reject it: imports "don't reduce its context cost, because imported files also load at launch". Path-scoped rules, which load only on a match, are the split that saves context (page 2).
3. **"Put the convention in the subdirectory's CLAUDE.md; Claude will have it from the start."** It is tempting because the file sits next to the code. The exam rejects it: a subdirectory file loads "on demand when Claude reads files in those directories", so from the repository root it is absent until Claude opens a file there.
4. **"Add a `CLAUDE.local.md` for my notes; the repository's `AGENTS.md` still loads."** It is tempting because the local file looks additive. The product rejects it: the local file counts as a CLAUDE.md, so Claude "reads only your CLAUDE.md files" and `AGENTS.md` is skipped unless you import it or change the setting.

## Quiz

1. An engineer wants a reminder about a private test server to guide Claude in one repository on their own machine, and never to reach teammates. Where does it go?
   - **a**: The committed instruction file under a heading for personal items
   - **b**: A git-ignored local note beside the root instruction file
   - **c**: The personal file in the home folder for every project
   - **d**: The organisation policy file that only that engineer edits

2. A session starts at the top of a project, and an instruction file sits two levels down in `web/ui/`. When do its lines reach the model?
   - **a**: At launch, together with the root file
   - **b**: Only after a restart that begins inside that directory
   - **c**: When the user runs the memory command
   - **d**: Once Claude opens something beneath that directory

<details>
<summary>Answer key</summary>

1. **b**. The local file is personal to one project and is kept out of version control. *a* is ruled out because a committed file is how a rule reaches the team, as in "the fix is to move the rule into the project file and commit it", so the reminder would reach every teammate. *c* is ruled out because that file reaches "You, in every project", and the reminder concerns one repository. *d* is ruled out because that layer reaches "Everyone on the machine, and individual settings cannot exclude it", whatever the project, so it is not a note for one repository.
2. **d**. A file below the starting directory loads on demand. *a* is ruled out because the files that load first are the ones "above the working directory are loaded at launch", and this one is below it. *b* is ruled out because a lower file loads "on demand when Claude reads files in those directories", with no restart. *c* is ruled out because the memory command "lists your CLAUDE.md, CLAUDE.local.md, and other memory file locations" and loads nothing.

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
