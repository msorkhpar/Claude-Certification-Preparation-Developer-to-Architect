# Path-scoped rules, file size and the practice

**Level:** Architect · **Module 57:** Memory files and rules · **Page 2 of 2**
**Exams:** A3.1, A3.3; S2

**After this page you can** split one oversized instruction file into a short root file and rules that load only for matching files, write `paths` globs that follow file type and not folder, decide which instruction is guidance and which needs a permission rule, keep personal lines out of shared files, and write the module's practice.

Checked on 2026-10-03 against the Claude Code documentation pages "How Claude remembers your project" (memory, rules and `paths`) and "Best practices for Claude Code", documenting behaviour up to Claude Code v2.1.286. The practice is a set of files graded by Python, TypeScript, Java and Kotlin test suites, offline, on the course's model of the documented loading rules (`examples/57-memory-loading`, with `examples/38-settings-layers` for permission rules); nothing in it starts Claude Code. This page deepens module 38 (the memory file and its size) and does not repeat it. The loading rules it builds on are on the first page.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* in its task on conventions (3.3) it describes `.claude/rules/` files "with YAML frontmatter `paths` fields containing glob patterns" that "load only when editing matching files", and says path-scoping beats directory-level CLAUDE.md files "for conventions that span multiple directories (e.g., test files spread throughout a codebase)". *What the current product does (documentation checked 2026-10-03):* the same, with details. A rule with no `paths` loads at launch with the priority of the project file. A rule with `paths` loads when Claude "uses the Read, Write, or Edit tool on a file matching the pattern, not on every tool use". `paths` is the only field Claude Code reads in a rule. On the exam, a convention that follows a file type across many folders is a `.claude/rules/` file with a glob; a convention for one folder may be a directory file.

## Why it matters

One team's `CLAUDE.md` has grown to four hundred lines: testing conventions, API conventions, Terraform conventions, a commit-message rule, and a line that says never to edit the migrations. Claude reads all of it in every session, so the Terraform section is in context while someone fixes a button, and the one rule that must not be broken is a single line among hundreds. Scenario S2 asks how to reorganise it, and the tempting answers all keep the text loading every time. The documentation states the cost: files over 200 lines "consume more context and may reduce adherence". The remedy is to load text only where it applies.

## The idea

### Rules that load on a match

Every Markdown file under `.claude/rules/` is a rule. Without frontmatter it behaves like part of the project file and loads at launch. With a `paths` list in YAML frontmatter, it loads only when Claude works with a file that matches:

```yaml
---
paths:
  - "src/api/**/*.ts"
---
```

The globs are the usual ones: `**/*.ts` matches TypeScript files in any directory, `src/**/*` matches everything under `src/`, `*.md` matches Markdown files in the project root, and `src/**/*.{ts,tsx}` matches a set, with brace groups expanded. Two details decide exam answers. First, matching is by file, and the trigger is a Read, Write or Edit of a matching file, so a rule can be absent at the start of a session and present after Claude opens its first test file. Second, a glob follows the file's type wherever it sits. Test files "spread throughout a codebase", `src/auth/login.test.ts`, `web/ui/Button.test.tsx` and `tools/export.test.ts`, are all reached by one `**/*.test.{ts,tsx}` rule, which a directory-level CLAUDE.md cannot do: a directory file covers one folder and what is below it.

A broken `paths` value fails quietly in two ways. If the YAML does not parse, "Claude Code ignores the frontmatter and loads the rule as if it had no `paths`", so a rule meant to be scoped loads in every session. In the course's model of the glob rules, a bare folder name such as `terraform` matches no file, so the rule never loads; the documentation's own examples all use a wildcard or an extension, so write `terraform/**/*`. Both are caught by checking, for each pattern, that it matches at least one file in the project, which the practice's sixth case does.

### The split that saves context

The three splits compare as follows. An import moves text to another file and loads it at launch, so the cost is unchanged. A directory CLAUDE.md loads on demand, but only for one folder, so it cannot follow a file type. A path-scoped rule loads on a match, wherever the matching files are. A short root file keeps what every task needs: the commit-message rule, how to run the tests, the habit of asking before adding a dependency. Everything that concerns one kind of file moves to a rule. The documentation's own size guidance is a target of "under 200 lines per CLAUDE.md file", and the practice asks for far fewer, because a root file with scoped rules has little left to carry.

A word on dilution, which the guide names as a reason to keep files short: when guidance is long, the sentence that matters competes with all the others. The rule that must never break is better placed where it cannot be diluted at all, which is the next section.

### Guidance and enforcement

"Never edit files under `db/migrations/`" is a sentence Claude will usually follow. The documentation is explicit that it is not a guarantee: a memory file is context, delivered as a user message, and "there's no guarantee of strict compliance". If breaking the rule is costly, the same rule also becomes a permission rule that denies edits to that path, in the project's `.claude/settings.json`. The sentence stays, because it tells Claude why the edit is refused and what to do instead. The practice's fifth case checks the deny rule, and checks that it denies only that path.

### Personal lines belong in personal files

Two kinds of line do not belong in a committed file. A preference that follows you into every project, such as short answers, goes in the user file or in `~/.claude/rules/`, which apply to every project on your machine. A note about one project that nobody else needs, such as a sandbox address, goes in `CLAUDE.local.md`, which is added to `.gitignore`. A shared file that carries someone's home path, an address or a key shares them with the team, so the practice also checks that none of the files holds one.

### Reorganising an oversized file, in order

A workable order, which the practice follows:

1. Read the file and mark each line with who needs it: everyone always, a kind of file, one person.
2. Move each kind-of-file group, whole, into a rule with a glob that names the files by type; never leave half of a group behind.
3. Move personal lines to the user file or the local file.
4. Turn the line that must not be broken into a permission rule as well, and keep the sentence.
5. Keep the root file to the lines everyone needs, with imports spelled correctly (an import saves nothing, and a typo imports nothing).
6. Open a session and run `/context` to see the files that loaded, then touch a file of each kind and look again.

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

### The practice: split one oversized memory file

The practice is in [`exercises/57-memory-files-and-rules`](../../exercises/57-memory-files-and-rules/unit-01/practice-1/statement.md). You split a long `CLAUDE.md` into a short root file with a correct import, three rules with `paths`, a permission rule for the migrations, and the personal lines in their own files. It is graded by test suites in Python, TypeScript, Java and Kotlin, offline, on files that are not code in any language, so the four suites test the same files; the Java and Kotlin ones read the JSON with Jackson and the rules' front matter with its YAML module. The statement lists eight cases, and each says what you should see when it works.

## Traps

1. **"Move the testing, API and Terraform sections into files and import them from `CLAUDE.md`."** It is tempting because the root file becomes short. The exam rejects it because imported files "load at launch", so every session still carries all of it. Scoped rules are the answer.
2. **"Put a `CLAUDE.md` in each folder that has tests."** It is tempting because directory files load on demand. The exam rejects it because test files "spread throughout a codebase" are in many folders, and each folder would need its own copy to be kept in step. One glob covers them.
3. **"The migrations rule is in `CLAUDE.md`, so edits there are blocked."** It is tempting because the sentence is clear and emphatic. The exam rejects it because memory is context, not enforcement; a permission rule that denies the path is what blocks the edit.
4. **"Write `paths: terraform` so the rule covers the folder."** It is tempting because it reads like the folder name. The exam rejects it because a pattern that matches no file scopes nothing, and the rule never loads; the pattern is `terraform/**/*`.

## Quiz

1. Testing conventions must govern spec files that live in dozens of different folders, and no other file. What delivers that?
   - **a**: A scoped rule whose glob matches by extension anywhere
   - **b**: A separate memory file placed in every one of those folders
   - **c**: An import of the conventions from the project root file
   - **d**: A section for each project in the personal memory file

2. A rule meant for infrastructure code loads in every session instead. What is the most likely cause?
   - **a**: The rules folder sits below the working directory
   - **b**: Infrastructure files are always opened at the start of a session
   - **c**: Its frontmatter is invalid YAML, so the scoping is dropped
   - **d**: The rule's file name does not match the folder it governs

<details>
<summary>Answer key</summary>

1. **a**. One glob follows the type across folders, and the rule loads when a matching file is read or edited. *b* is ruled out because the guide's reason for the glob is "test files spread throughout a codebase", and a copy in each folder would have to be kept in step. *c* is ruled out because an import "moves text to another file and loads it at launch", so the conventions would reach every task. *d* is ruled out because personal rules "apply to every project on your machine", so they neither scope to test files nor reach the team.
2. **c**. When the frontmatter does not parse, the rule is treated as unscoped. *a* is ruled out because "Every Markdown file under .claude/rules/ is a rule", wherever the session starts. *b* is ruled out because a scoped rule loads when Claude uses Read, Write or Edit on a matching file, "not on every tool use". *d* is ruled out because the file name plays no part, since `paths` "is the only field Claude Code reads in a rule".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A project holds `AGENTS.md` for other tools and nothing named `CLAUDE.md`, and Claude follows it. An engineer adds `CLAUDE.local.md` with a sandbox address, and Claude stops following `AGENTS.md`. What explains it?
   - **a**: The sandbox address contradicts a rule in the team's shared notes
   - **b**: Local notes load first and replace everything that follows them
   - **c**: The local note counts as an instruction file, so the fallback no longer applies
   - **d**: Ignored files are read in place of the ones kept in version control

2. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. Its single `CLAUDE.md` has grown to four hundred lines, and a developer splits it into five files that the root pulls in with `@` references, expecting sessions to start lighter. What results?
   - **a**: The loaded text is unchanged, since imported material still arrives at launch
   - **b**: Context shrinks by four fifths, because each part loads only on demand
   - **c**: Context shrinks for any session that never touches the split-out areas
   - **d**: Claude loads the root alone and fetches the others when it needs them

3. Scenario S2, code generation with Claude Code. A team uses Claude Code for refactoring and tests. A new rule for component files should apply to `.tsx` sources anywhere in the project and to nothing else. Which `paths` entry does it?
   - **a**: `*.tsx`
   - **b**: `src/components`
   - **c**: `components/**`
   - **d**: `**/*.tsx`

<details>
<summary>Answer key</summary>

1. **c**. The default is that AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above, and the local file is one. *a* is ruled out because presence decides, not content: "A `CLAUDE.md`, a `.claude/CLAUDE.md` or a `CLAUDE.local.md` in the working directory or any directory above it counts". *b* is ruled out because "All discovered files are concatenated into context rather than overriding each other", and "In each folder the local file comes after the shared one". *d* is ruled out because the file is ignored by git only so that it stays personal, "which is why that file is ignored by git", and the ignore setting plays no part in what Claude reads.
2. **a**. An import changes where text is kept and not how much of it is loaded. *b* is ruled out because imported files "are expanded and loaded into context at launch alongside the CLAUDE.md that references them". *c* is ruled out because that describes a scoped rule, since "A path-scoped rule loads on a match, wherever the matching files are", while an import "moves text to another file and loads it at launch". *d* is ruled out because an import "moves text to another file and loads it at launch, so the cost is unchanged".
3. **d**. The leading `**/` makes the pattern match the file type at any depth. *a* is ruled out because without it the pattern behaves like "`*.md` matches Markdown files in the project root", so only files at the top of the project match. *b* is ruled out because a bare folder name matches nothing: "matches no file, so the rule never loads". *c* is ruled out because it names a folder and not a type, while "a glob follows the file's type wherever it sits".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
