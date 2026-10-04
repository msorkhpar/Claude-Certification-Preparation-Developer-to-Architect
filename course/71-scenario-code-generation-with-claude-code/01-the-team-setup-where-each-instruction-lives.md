# The team setup: where each instruction lives

**Level:** Architect · **Module 71:** Scenario: code generation with Claude Code · **Page 1 of 2**
**Exams:** A3; S2

**After this page you can** place each requirement of the code generation scenario in the mechanism that provides it (the root memory file, a path-scoped rule, a shared command, a permission rule, a skill), say why each tempting wrong placement fails, and audit a team's setup with a checklist that reads its files.

Checked on 2026-10-04 against the Claude Code documentation pages "How Claude remembers your project", "Extend Claude with skills", "Best practices for Claude Code" and "Choose a permission mode" (the pages name Claude Code version 2.1.286), and against the Architect exam guide (version 1.0, scenario 2 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin and reads two small folders of plain files; it does not start Claude Code. This page is a capstone: it uses modules 49 and 56 to 59 and puts them in the order the exam asks about.

## Why it matters

Scenario S2 of the Architect exam is a team that uses Claude Code for generation, refactoring, debugging and documentation, and wants to fit it into its workflow with slash commands, memory files and a rule for when to plan first. The questions are almost all of one kind: a requirement is stated, four places are offered, and one of them gives the requirement exactly. A command every developer must get on clone. Conventions that depend on the kind of file and not on the folder. A restriction that must hold whatever the model decides. Each has a mechanism built for it, and the wrong options are the mechanisms built for something else.

## The idea

### The scenario in plain words

A team generates code, refactors, debugs and writes documentation with Claude Code. It wants three things from the setup. The team's conventions should reach Claude without anyone retyping them. Common tasks, such as a review against the team checklist, should be one slash command that every developer has. And the team should know when to let Claude plan before it edits, and when to let it just do the work.

### The requirement decides the mechanism

| The requirement | The mechanism | Why the others fail |
|---|---|---|
| Every developer has the `/review` command after a clone or a pull | A command file in the project, `.claude/commands/review.md`, committed to the repository (or a skill at `.claude/skills/review/SKILL.md`) | A command in the home folder is personal and is not shared. The root memory file holds context, not command definitions. A `commands` array in a config file is a mechanism that does not exist |
| Conventions that depend on the kind of file, wherever the file sits (tests next to the code they test) | Rule files in `.claude/rules/`, each with a `paths` list of globs | One root file with a section per area leaves Claude to infer which section applies. Skills load when they are invoked or chosen, not because a path matched. A memory file per folder is bound to its directory, and test files are spread over many |
| What every task needs: the test command, the repository's etiquette | The root `CLAUDE.md`, short | Nothing else loads in every session at the same priority |
| A preference of one developer | `CLAUDE.local.md` (kept out of version control) or a file in the home folder | The shared root file is for the team |
| A rule that must hold whatever Claude decides: never read `.env` | A permission rule or a hook | A memory file is context: Claude "treats them as context, not enforced configuration" |
| Domain knowledge needed only sometimes | A skill, which loads on demand | The root file loads in every session and costs context each time |

The sixth row and the fifth carry the course's recurring distinction: a sentence in a memory file is a request that the model weighs, and a permission rule or a hook is code that runs. The documentation draws the line on the memory page itself: "To block an action regardless of what Claude decides, use a PreToolUse hook instead."

### What loads when

The memory page gives the loading rules that make the table work.

- The root `CLAUDE.md` and the rule files without a `paths` list load at the start of every session, at the same priority.
- A rule with a `paths` list loads when Claude works with a file that matches one of its globs, so a convention costs context only in the sessions that touch its files.
- A `CLAUDE.md` in a subdirectory is not loaded at launch. In the page's words: "Files in subdirectories load on demand when Claude reads files in those directories."
- Size counts. The page's target is "under 200 lines per CLAUDE.md file", because "Longer files consume more context and reduce adherence", and it adds that imports "help you organize a long file but don't reduce its context cost, because imported files also load at launch". Splitting a long file into imported pieces changes nothing about the load. Moving the area-specific parts into path-scoped rules does.

The practice of this module uses a stricter limit of 25 lines for the root file of a small project. That number is the course's, chosen to make the point testable. The documented target is 200.

### What a glob matches

A `paths` entry is a glob. `**` crosses folders and `*` stays inside one, so `src/api/**/*.ts` reaches every TypeScript file below `src/api/`, and `**/*.test.tsx` reaches test files in any folder. A bare folder name such as `src/api` is not a glob; write `src/api/**`, because the audit reports a rule whose globs match none of the project's files. The pages of module 57 give the full rules; the example below uses these three forms only.

### The audit: a checklist that reads the files

A setup is a set of files, so a checklist can read it. The example implements the one this course uses for the scenario, on two projects beside the example: `project-before`, where everything sits in one root file, and `project-after`, where each requirement has its mechanism. For each project the audit reports:

- `all-in-root`: three or more sections in the root file and no rule files, which means the area conventions load everywhere and Claude must infer which applies.
- `rule-loads-always` and `rule-matches-nothing`: a rule with no `paths` list, or whose globs match none of the project's files.
- `test-uncovered`: a test file that no rule reaches, which is the colocated-tests question of the exam.
- `no-shared-command`: no command file and no skill in the project.
- `env-readable` and `bare-bash-allowed`: the settings do not deny reading `.env`, or allow the whole Bash tool.

For a project with no findings, the example prints which rule files load for each of its sample files, so that the effect of the globs is visible.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* project-scoped custom commands live in `.claude/commands/` in the repository, where they are version-controlled and reach every developer; commands in the home folder are personal, and the memory file is for instructions and context. *What the product does now (the "Extend Claude with skills" page, checked 2026-10-04):* "Custom commands have been merged into skills. A file at `.claude/commands/deploy.md` and a skill at `.claude/skills/deploy/SKILL.md` both create `/deploy` and work the same way", the existing command files keep working, and when a skill and a command file share a name the skill wins. Project skills live in `.claude/skills/<skill-name>/SKILL.md` and are committed so that the team gets them. So a question that asks where to put a shared slash command is answered with the project's `.claude/commands/` folder, and in a current setup a skill in `.claude/skills/` does the same work and also carries more options (module 58).

### The example

The example loads each project and runs the checklist. It prints one line of facts per project, then the findings for the flawed one, and for the fixed one the rule files that load for each sample file. The test files of the fixed project load two rule files each: the rule of their area and the testing rule, which is what "regardless of location" means. The program and its output are the same in all four languages.

<!-- example: m71-team-setup-audit tabs: python,typescript,java,kotlin -->
```python
"""Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.

The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
"""
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent


def glob_regex(glob):
    """A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character."""
    out, i = "", 0
    while i < len(glob):
        if glob.startswith("**/", i):
            out, i = out + "(?:.*/)?", i + 3
        elif glob.startswith("**", i):
            out, i = out + ".*", i + 2
        elif glob[i] == "*":
            out, i = out + "[^/]*", i + 1
        elif glob[i] == "?":
            out, i = out + "[^/]", i + 1
        else:
            out, i = out + re.escape(glob[i]), i + 1
    return re.compile(out + r"\Z")


def rule_paths(text):
    """The paths list of a rule file's front matter, or None when it has none (the rule then loads at launch)."""
    head = re.match(r"---\n(.*?)\n---\n", text, re.S)
    listed = re.search(r"^paths:\s*\n((?:[ \t]+-[ \t]+.*\n?)+)", head.group(1) + "\n", re.M) if head else None
    if not listed:
        return None
    return [re.sub(r"^\s*-\s+", "", line).strip().strip("\"'") for line in listed.group(1).splitlines() if line.strip()]


def load(root):
    rules = {p.name: rule_paths(p.read_text()) for p in sorted((root / ".claude/rules").glob("*.md"))}
    commands = [p.name for p in sorted((root / ".claude/commands").glob("*.md"))]
    return (root / "files.txt").read_text().split(), (root / "CLAUDE.md").read_text(), rules, commands, json.loads((root / ".claude/settings.json").read_text())


def matches(paths, file):
    return any(glob_regex(g).match(file) for g in paths or [])


def rules_for(rules, file):
    return [name for name, paths in rules.items() if paths is None or matches(paths, file)]


def audit(root):
    files, memory, rules, commands, settings = load(root)
    found = []
    sections = len(re.findall(r"^## ", memory, re.M))
    if not rules and sections >= 3:
        found.append(f"all-in-root: {sections} sections in CLAUDE.md and no rule files")
    for name, paths in rules.items():
        if paths is None:
            found.append(f"rule-loads-always: {name}")
        elif not any(matches(paths, f) for f in files):
            found.append(f"rule-matches-nothing: {name}")
    for f in files:
        if re.search(r"\.test\.tsx?$", f) and not any(matches(p, f) for p in rules.values()):
            found.append(f"test-uncovered: {f}")
    if not commands:
        found.append("no-shared-command")
    perms = settings.get("permissions", {})
    if "Read(./.env)" not in perms.get("deny", []):
        found.append("env-readable")
    if "Bash" in perms.get("allow", []):
        found.append("bare-bash-allowed")
    return found


def main():
    for name in ("project-before", "project-after"):
        root = HERE / name
        files, memory, rules, commands, _ = load(root)
        print(f"{name}: CLAUDE.md {len(memory.splitlines())} lines, {len(rules)} rule files, {len(commands)} shared commands")
        for finding in audit(root):
            print(f"  finding: {finding}")
        if not audit(root):
            print("  no findings")
            for f in files:
                print(f"  {f} <- {', '.join(rules_for(rules, f)) or 'CLAUDE.md only'}")


if __name__ == "__main__":
    main()
```
```text
project-before: CLAUDE.md 16 lines, 0 rule files, 0 shared commands
  finding: all-in-root: 5 sections in CLAUDE.md and no rule files
  finding: test-uncovered: src/components/Button.test.tsx
  finding: test-uncovered: src/api/orders.test.ts
  finding: test-uncovered: src/models/order.test.ts
  finding: no-shared-command
  finding: env-readable
  finding: bare-bash-allowed
project-after: CLAUDE.md 5 lines, 4 rule files, 1 shared commands
  no findings
  src/components/Button.tsx <- components.md
  src/components/Button.test.tsx <- components.md, tests.md
  src/api/orders.ts <- api.md
  src/api/orders.test.ts <- api.md, tests.md
  src/models/order.ts <- models.md
  src/models/order.test.ts <- models.md, tests.md
  docs/readme.md <- CLAUDE.md only
```
```typescript
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
```
```text
project-before: CLAUDE.md 16 lines, 0 rule files, 0 shared commands
  finding: all-in-root: 5 sections in CLAUDE.md and no rule files
  finding: test-uncovered: src/components/Button.test.tsx
  finding: test-uncovered: src/api/orders.test.ts
  finding: test-uncovered: src/models/order.test.ts
  finding: no-shared-command
  finding: env-readable
  finding: bare-bash-allowed
project-after: CLAUDE.md 5 lines, 4 rule files, 1 shared commands
  no findings
  src/components/Button.tsx <- components.md
  src/components/Button.test.tsx <- components.md, tests.md
  src/api/orders.ts <- api.md
  src/api/orders.test.ts <- api.md, tests.md
  src/models/order.ts <- models.md
  src/models/order.test.ts <- models.md, tests.md
  docs/readme.md <- CLAUDE.md only
```
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.
 *
 * <p>The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
 * at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
 */
public final class SetupAudit {
    static final Path HERE = Path.of("..").toAbsolutePath().normalize();

    /** A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character. */
    static Pattern globRegex(String glob) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < glob.length()) {
            if (glob.startsWith("**/", i)) { out.append("(?:.*/)?"); i += 3; }
            else if (glob.startsWith("**", i)) { out.append(".*"); i += 2; }
            else if (glob.charAt(i) == '*') { out.append("[^/]*"); i += 1; }
            else if (glob.charAt(i) == '?') { out.append("[^/]"); i += 1; }
            else { out.append(Pattern.quote(String.valueOf(glob.charAt(i)))); i += 1; }
        }
        return Pattern.compile(out.toString());
    }

    /** The paths list of a rule file's front matter, or null when it has none (the rule then loads at launch). */
    static List<String> rulePaths(String text) {
        Matcher head = Pattern.compile("^---\\n(.*?)\\n---\\n", Pattern.DOTALL).matcher(text);
        if (!head.find()) return null;
        Matcher listed = Pattern.compile("^paths:\\s*\\n((?:[ \\t]+-[ \\t]+.*\\n?)+)", Pattern.MULTILINE).matcher(head.group(1) + "\n");
        if (!listed.find()) return null;
        List<String> paths = new ArrayList<>();
        for (String line : listed.group(1).split("\n")) {
            if (!line.isBlank()) paths.add(line.replaceFirst("^\\s*-\\s+", "").strip().replaceAll("^[\"']|[\"']$", ""));
        }
        return paths;
    }

    record Project(List<String> files, String memory, Map<String, List<String>> rules, List<String> commands, JsonNode settings) {}

    static List<String> list(Path dir, String suffix) throws IOException {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(suffix)).sorted().toList();
        }
    }

    static Project load(Path root) {
        try {
            Map<String, List<String>> rules = new LinkedHashMap<>();
            for (String name : list(root.resolve(".claude/rules"), ".md")) rules.put(name, rulePaths(Files.readString(root.resolve(".claude/rules").resolve(name))));
            List<String> files = List.of(Files.readString(root.resolve("files.txt")).strip().split("\\s+"));
            return new Project(files, Files.readString(root.resolve("CLAUDE.md")), rules, list(root.resolve(".claude/commands"), ".md"),
                new ObjectMapper().readTree(Files.readString(root.resolve(".claude/settings.json"))));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static boolean matches(List<String> paths, String file) {
        return paths != null && paths.stream().anyMatch(g -> globRegex(g).matcher(file).matches());
    }

    static List<String> rulesFor(Map<String, List<String>> rules, String file) {
        return rules.entrySet().stream().filter(e -> e.getValue() == null || matches(e.getValue(), file)).map(Map.Entry::getKey).toList();
    }

    static boolean has(JsonNode array, String value) {
        if (array == null) return false;
        for (JsonNode n : array) if (n.asText().equals(value)) return true;
        return false;
    }

    static List<String> audit(Path root) {
        Project p = load(root);
        List<String> found = new ArrayList<>();
        int sections = (int) Pattern.compile("^## ", Pattern.MULTILINE).matcher(p.memory()).results().count();
        if (p.rules().isEmpty() && sections >= 3) found.add("all-in-root: " + sections + " sections in CLAUDE.md and no rule files");
        p.rules().forEach((name, paths) -> {
            if (paths == null) found.add("rule-loads-always: " + name);
            else if (p.files().stream().noneMatch(f -> matches(paths, f))) found.add("rule-matches-nothing: " + name);
        });
        for (String f : p.files()) {
            if (f.matches(".*\\.test\\.tsx?") && p.rules().values().stream().noneMatch(paths -> matches(paths, f))) found.add("test-uncovered: " + f);
        }
        if (p.commands().isEmpty()) found.add("no-shared-command");
        JsonNode perms = p.settings().path("permissions");
        if (!has(perms.get("deny"), "Read(./.env)")) found.add("env-readable");
        if (has(perms.get("allow"), "Bash")) found.add("bare-bash-allowed");
        return found;
    }

    public static void main(String[] args) {
        for (String name : List.of("project-before", "project-after")) {
            Path root = HERE.resolve(name);
            Project p = load(root);
            System.out.println(name + ": CLAUDE.md " + p.memory().stripTrailing().split("\n").length + " lines, " + p.rules().size() + " rule files, " + p.commands().size() + " shared commands");
            List<String> found = audit(root);
            for (String finding : found) System.out.println("  finding: " + finding);
            if (found.isEmpty()) {
                System.out.println("  no findings");
                for (String f : p.files()) {
                    List<String> names = rulesFor(p.rules(), f);
                    System.out.println("  " + f + " <- " + (names.isEmpty() ? "CLAUDE.md only" : String.join(", ", names)));
                }
            }
        }
    }
}
```
```text
project-before: CLAUDE.md 16 lines, 0 rule files, 0 shared commands
  finding: all-in-root: 5 sections in CLAUDE.md and no rule files
  finding: test-uncovered: src/components/Button.test.tsx
  finding: test-uncovered: src/api/orders.test.ts
  finding: test-uncovered: src/models/order.test.ts
  finding: no-shared-command
  finding: env-readable
  finding: bare-bash-allowed
project-after: CLAUDE.md 5 lines, 4 rule files, 1 shared commands
  no findings
  src/components/Button.tsx <- components.md
  src/components/Button.test.tsx <- components.md, tests.md
  src/api/orders.ts <- api.md
  src/api/orders.test.ts <- api.md, tests.md
  src/models/order.ts <- models.md
  src/models/order.test.ts <- models.md, tests.md
  docs/readme.md <- CLAUDE.md only
```
```kotlin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Audit a team's Claude Code setup for the exam's code generation scenario: which instructions load for which files, who gets the shared command and what is protected.
 *
 * The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
 * documented behaviour (checked 2026-10-04): a rule file under .claude/rules/ with a paths list loads when Claude works with a matching file and one without paths loads
 * at launch; a command file under .claude/commands/ in the project is shared through version control; permission rules sit in .claude/settings.json. Nothing here starts Claude Code.
 */
val HERE: Path = Path.of("..").toAbsolutePath().normalize()

/** A glob as a regular expression: ** crosses folders, * stays inside one, ? is one character. */
fun globRegex(glob: String): Regex {
    val out = StringBuilder()
    var i = 0
    while (i < glob.length) {
        when {
            glob.startsWith("**/", i) -> { out.append("(?:.*/)?"); i += 3 }
            glob.startsWith("**", i) -> { out.append(".*"); i += 2 }
            glob[i] == '*' -> { out.append("[^/]*"); i += 1 }
            glob[i] == '?' -> { out.append("[^/]"); i += 1 }
            else -> { out.append(Regex.escape(glob[i].toString())); i += 1 }
        }
    }
    return Regex(out.toString())
}

/** The paths list of a rule file's front matter, or null when it has none (the rule then loads at launch). */
fun rulePaths(text: String): List<String>? {
    val head = Regex("^---\\n(.*?)\\n---\\n", RegexOption.DOT_MATCHES_ALL).find(text) ?: return null
    val listed = Regex("^paths:\\s*\\n((?:[ \\t]+-[ \\t]+.*\\n?)+)", RegexOption.MULTILINE).find(head.groupValues[1] + "\n") ?: return null
    return listed.groupValues[1].split("\n").filter { it.isNotBlank() }.map { it.replaceFirst(Regex("^\\s*-\\s+"), "").trim().replace(Regex("^[\"']|[\"']$"), "") }
}

class Project(val files: List<String>, val memory: String, val rules: Map<String, List<String>?>, val commands: List<String>, val settings: JsonNode)

private fun list(dir: Path, suffix: String): List<String> =
    if (!Files.isDirectory(dir)) listOf() else Files.list(dir).use { s -> s.map { it.fileName.toString() }.filter { it.endsWith(suffix) }.sorted().toList() }

fun load(root: Path): Project {
    val rules = linkedMapOf<String, List<String>?>()
    for (name in list(root.resolve(".claude/rules"), ".md")) rules[name] = rulePaths(Files.readString(root.resolve(".claude/rules").resolve(name)))
    return Project(Files.readString(root.resolve("files.txt")).trim().split(Regex("\\s+")), Files.readString(root.resolve("CLAUDE.md")), rules,
        list(root.resolve(".claude/commands"), ".md"), ObjectMapper().readTree(Files.readString(root.resolve(".claude/settings.json"))))
}

private fun matches(paths: List<String>?, file: String) = paths != null && paths.any { globRegex(it).matches(file) }

fun rulesFor(rules: Map<String, List<String>?>, file: String): List<String> = rules.filter { (_, paths) -> paths == null || matches(paths, file) }.keys.toList()

private fun has(array: JsonNode?, value: String) = array != null && array.any { it.asText() == value }

fun audit(root: Path): List<String> {
    val p = load(root)
    val found = mutableListOf<String>()
    val sections = Regex("^## ", RegexOption.MULTILINE).findAll(p.memory).count()
    if (p.rules.isEmpty() && sections >= 3) found += "all-in-root: $sections sections in CLAUDE.md and no rule files"
    for ((name, paths) in p.rules) {
        if (paths == null) found += "rule-loads-always: $name"
        else if (p.files.none { matches(paths, it) }) found += "rule-matches-nothing: $name"
    }
    for (f in p.files) if (Regex(".*\\.test\\.tsx?").matches(f) && p.rules.values.none { matches(it, f) }) found += "test-uncovered: $f"
    if (p.commands.isEmpty()) found += "no-shared-command"
    val perms = p.settings.path("permissions")
    if (!has(perms.get("deny"), "Read(./.env)")) found += "env-readable"
    if (has(perms.get("allow"), "Bash")) found += "bare-bash-allowed"
    return found
}

fun main() {
    for (name in listOf("project-before", "project-after")) {
        val root = HERE.resolve(name)
        val p = load(root)
        println("$name: CLAUDE.md ${p.memory.trimEnd().split("\n").size} lines, ${p.rules.size} rule files, ${p.commands.size} shared commands")
        val found = audit(root)
        for (finding in found) println("  finding: $finding")
        if (found.isEmpty()) {
            println("  no findings")
            for (f in p.files) println("  $f <- ${rulesFor(p.rules, f).joinToString(", ").ifEmpty { "CLAUDE.md only" }}")
        }
    }
}
```
```text
project-before: CLAUDE.md 16 lines, 0 rule files, 0 shared commands
  finding: all-in-root: 5 sections in CLAUDE.md and no rule files
  finding: test-uncovered: src/components/Button.test.tsx
  finding: test-uncovered: src/api/orders.test.ts
  finding: test-uncovered: src/models/order.test.ts
  finding: no-shared-command
  finding: env-readable
  finding: bare-bash-allowed
project-after: CLAUDE.md 5 lines, 4 rule files, 1 shared commands
  no findings
  src/components/Button.tsx <- components.md
  src/components/Button.test.tsx <- components.md, tests.md
  src/api/orders.ts <- api.md
  src/api/orders.test.ts <- api.md, tests.md
  src/models/order.ts <- models.md
  src/models/order.test.ts <- models.md, tests.md
  docs/readme.md <- CLAUDE.md only
```
<!-- /example -->

The flawed project's root file has five sections and no rules, so the audit reports `all-in-root`, and none of its three test files is reached by a rule. It has no shared command, it does not deny `.env`, and it allows the whole Bash tool. The fixed project has none of those findings: four rule files, a shared command and settings that deny the environment file and allow one named command.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Put all conventions in the root file under one heading per area, and let Claude work out which applies."** It is tempting because it is one file and easy to read. The exam rejects it: it relies on inference, so it is not reliable, and every area's conventions cost context in every session.
2. **"Make a skill for each kind of code."** It is tempting because skills are a modern feature. The exam rejects it for conventions that must apply automatically by path: a skill is invoked, or chosen by the model from its description, and a path match does not start it.
3. **"Put a `CLAUDE.md` in every subdirectory."** It is tempting because it matches the folder structure. The exam rejects it when the files to cover are spread over many folders: each file is bound to its directory, and test files next to their sources are in all of them.
4. **"Put the shared command in the home folder or in the root memory file."** It is tempting because both are places Claude reads. The exam rejects both: the first is personal and the second is not a place for command definitions.

## Quiz

1. A team wants a review command that every developer has after cloning the repository. Where does the file go?
   - **a**: In a settings file that holds a list of commands for Claude Code
   - **b**: In a commands folder in each developer's home directory, created by hand
   - **c**: In the root instructions file, under a heading that names the review checklist
   - **d**: In the project's own configuration folder, committed with the sources

2. An audit of a team's setup prints `test-uncovered`. What does that finding report?
   - **a**: A file of tests that no rule reaches
   - **b**: A rule whose globs match none of the project's files
   - **c**: A root file with several sections and no rule files
   - **d**: A project with no command file and no skill

3. In the fixed project, a spec file beside a handler gets two sets of conventions. Why?
   - **a**: A skill for each area starts as soon as the path matches
   - **b**: The root file lists every rule that applies to a spec
   - **c**: Its area's rule and the testing rule both have globs that match its path
   - **d**: Every rule loads as soon as the first file of the session is opened

<details>
<summary>Answer key</summary>

1. **d**. A command file in the project's own folder is committed and reaches every clone. *b* is ruled out because a home folder is personal: "commands in the home folder are personal". *c* is ruled out because the memory file holds instructions and not definitions: "the memory file is for instructions and context". *a* is ruled out because no such mechanism exists: "A `commands` array in a config file is a mechanism that does not exist".
2. **a**. The finding is the colocated-tests question: "`test-uncovered`: a test file that no rule reaches, which is the colocated-tests question of the exam." *b* is ruled out because that is another finding: "a rule with no `paths` list, or whose globs match none of the project's files". *c* is ruled out for the same reason: "three or more sections in the root file and no rule files". *d* is ruled out as a third finding: "no command file and no skill in the project".
3. **c**. Both rules have a glob that matches the path. The page says the spec files "load two rule files each: the rule of their area and the testing rule". *b* is ruled out because the root file is not where rules are chosen: "Nothing else loads in every session at the same priority". *a* is ruled out because a path starts no skill: "a skill is invoked, or chosen by the model from its description, and a path match does not start it". *d* is ruled out because a rule waits for a matching file: "A rule with a `paths` list loads when Claude works with a file that matches one of its globs".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on where a shared slash command goes, rewritten here.
