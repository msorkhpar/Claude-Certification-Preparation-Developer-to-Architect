# What each built-in tool is for, and how Edit fails

**Level:** Architect · **Module 56:** The built-in tools · **Page 1 of 2**
**Exams:** A2.5; S4

**After this page you can** choose between Read, Write, Edit, Bash, Grep and Glob for a job, say what each one does and does not do, explain why an Edit fails and which remedy to try first, and say which of the search tools a session actually has on your platform.

Checked on 2026-10-03 against the Claude Code documentation page "Tools reference" (the tool behaviour sections for Read, Write, Edit, Bash, Grep and Glob) and the Agent SDK pages "Give Claude custom tools" and "Configure permissions", with `claude-agent-sdk` 0.2.163 and `@anthropic-ai/claude-agent-sdk` 0.3.287. The example is the course's own model of the documented rules, run offline in Python, TypeScript, Java and Kotlin; no tool was run by Claude Code. This page deepens module 38 (the working loop and the permission rules) and module 35 (tools and permissions in the SDK), and it does not repeat them. Page 2 covers the permissions that bound these tools and the order in which to explore a codebase.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* Grep searches file contents (function names, error messages, import statements) and Glob matches file paths by name pattern such as `**/*.test.tsx`; Read and Write handle whole files and Edit makes targeted changes by unique text matching; when Edit fails because the text is not unique, use Read plus Write as the fallback; and scenario S4 lists the built-in tools as Read, Write, Bash, Grep and Glob. *What the current product does (documentation checked 2026-10-03):* Grep and Glob are two of the tools, with the roles the guide gives them, but on macOS, Linux and WSL Claude Code "leaves Glob and Grep out of the default tool set, and Claude searches with `find` and `grep` through the Bash tool instead"; they are on by default on Windows, and you get them back by naming them in `--tools`, `--allowedTools` or the equivalent SDK options, when `Bash` is removed, or in a subagent's `tools` list that leaves `Bash` out. For a failed Edit the documentation's first remedies are narrower than a rewrite: "Claude either supplies a longer string with enough surrounding context to pin down one occurrence, or sets `replace_all: true`". Read plus Write remains the answer when no unique text exists. On the exam, choose Grep for content, Glob for names and Read plus Write for a non-unique Edit; in your own code, give a search agent `Grep` and `Glob` by name and try a longer anchor first.

## Why it matters

The exam's scenario S4 is an agent that explores unfamiliar codebases, understands legacy systems and automates repetitive tasks using built-in tools. The questions in A2.5 are small and practical: which tool finds all callers of a function, which one finds every test file, what to do when an edit will not apply. They are easy to get wrong in a pattern: reading every file to find one name, using a content search to find files by name, rewriting a file whole to change one line. A wrong tool choice costs tokens and, with Write, risks the rest of the file.

## The idea

### Six tools, six jobs

"The tool names are the exact strings you use in permission rules, subagent tool lists, and hook matchers."

| Tool | Job | Remember |
|---|---|---|
| `Read` | Returns a file's contents with line numbers | Whole file by default; a long file returns a first page with a `PARTIAL view` notice and `offset` and `limit` read more. Reads images, PDFs and notebooks. Reads files, not directories (a folder is listed with a shell command) |
| `Write` | Creates a file or overwrites one with the full content | "It doesn't append or merge." Use it for new files and whole rewrites |
| `Edit` | Replaces one exact string with another in a file | Exact match, one occurrence, `replace_all` for many; for partial changes to an existing file "Claude uses Edit instead of Write" |
| `Bash` | Runs a shell command | Each command is a separate process; environment variables do not persist; the working directory may |
| `Grep` | Searches file contents | Built on ripgrep, so its regex syntax is not POSIX grep; skips gitignored files |
| `Glob` | Finds files by name pattern | Supports `**`; sorted by modification time and capped at 100 files; does not respect `.gitignore` by default |

The split to remember is the guide's: content or names? Grep finds lines inside files, and Glob finds the files themselves. Use Grep to find all callers of a function or every place an error message is produced; use Glob for every `**/*.test.tsx` or every `src/**/handlers/*.py`.

A Glob result is a window, not a census. The documentation says that results are sorted by modification time and capped at 100 files, and that when the cap is hit "Claude sees a truncation flag in the result and can narrow the pattern." A list that ends at exactly 100 is therefore a prompt to narrow the pattern to the folder the question concerns, and never a statement that no other file matches.

Grep has three output modes, and the choice is about cost: `files_with_matches` (paths only, the default), `content` (matching lines with file and line number) and `count` (matches per file). Start with paths when you only need to know where to look, and ask for content when you need the lines. Patterns match within a single line unless `multiline: true` is set, and a pattern with regex metacharacters needs escaping: finding `interface{}` in Go takes `interface\{\}`.

### Which search tools exist on your machine

Before the exam's answer, check the platform. On macOS, Linux and WSL, Claude Code leaves Glob and Grep out of the default tool set and Claude searches with `find` and `grep` through Bash, where "those two commands run embedded versions of `bfs` and `ugrep`, and the searches reach your hooks and permission rules as `Bash` calls". On Windows both are part of the default set. A rule that matches `Grep` calls therefore sees nothing in a default Linux session, and a hook with the matcher `Grep` never fires, because the searches arrive as `Bash`. You get the tools back in three ways: name `Glob` or `Grep` in `--tools` or `--allowedTools` (or the SDK's `tools` and `allowedTools`), where "with `--tools` you get the ones you list, and naming either tool in `--allowedTools` restores both"; remove `Bash` from the session; or give a subagent a `tools` list that names them and leaves out `Bash`. An allow rule in a settings file does not have this effect.

For an Architect, the consequence is concrete. An S4 agent built with the SDK that is meant to use Grep and Glob must name them. An agent that is told to use Grep on a Linux machine, with the default tool set, will run `grep` through Bash instead, which a permission rule on Grep does not cover.

### Edit needs one exact match

"The Edit tool performs exact string replacement. It takes an `old_string` and a `new_string` and replaces the first with the second. It doesn't use regex or fuzzy matching." Three checks must pass:

1. **Read before edit.** Claude reads the file in the current conversation first. Opus 4.6, Haiku 4.5 and older models always require it; newer models can edit an unread file when reading it would not need a permission prompt and Read is available. A read cut short with a `PARTIAL view` notice does not count.
2. **Match.** `old_string` must appear in the file exactly as written: "A single character of whitespace or indentation difference is enough to miss."
3. **Uniqueness.** "`old_string` must appear exactly once." When it appears more than once, the documentation names two remedies: "Claude either supplies a longer string with enough surrounding context to pin down one occurrence, or sets `replace_all: true` to replace them all."

A file that changed on disk since the last read can still be edited when `old_string` matches the current content exactly and unambiguously and Claude Code can read the file without prompting (this relaxed handling needs Claude Code v2.1.208 or later; before that, an edit to a file that had changed after the read was refused); in any other case, such as a stale string or one that matches more than once without `replace_all`, Claude reads the file again before editing.

The order of remedies is the order of risk. A longer anchor changes only the occurrence you meant. `replace_all` changes every occurrence, which is right for a rename and wrong when only one of three similar lines should change. And Read plus Write, which is the guide's fallback, changes the file by rewriting all of it: the model writes every line again, a long file costs the tokens twice, and any line it drops or alters by accident is lost without a diff to show it. Use it only when no unique text exists, for example when a file is made of repeated blocks that differ only by position.

### The example

The example models three of these facts in all four languages. `edit` is the exact replacement with its three outcomes (replaced, not found, appears more than once). `plan_edit` chooses the remedy in the documented order. `tool_set` shows which of the six a session has on each platform and how the options bring Grep and Glob back. `covered_by` and `rule_tool` give the tools that a permission rule covers, which page 2 uses.

<!-- example: m56-builtin-tools tabs: python,typescript,java,kotlin -->
```python
"""The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
on which platform, and which permission rule covers which tool.

A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
Read, Write, Edit, Bash, Grep and Glob.
"""
import logging

log = logging.getLogger(__name__)

SEARCH_TOOLS = ("Grep", "Glob")
BASE_TOOLS = ("Read", "Write", "Edit", "Bash")
RULE_COVERS = {"Read": ("Read", "Grep", "Glob"), "Edit": ("Edit", "Write"), "Bash": ("Bash",)}  # a Write(path) rule is never matched


def edit(text, old, new, replace_all=False):
    """Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replace_all is set."""
    count = text.count(old)
    if count == 0:
        return {"ok": False, "error": "old_string not found"}
    if count > 1 and not replace_all:
        return {"ok": False, "error": f"old_string appears {count} times"}
    return {"ok": True, "text": text.replace(old, new) if replace_all else text.replace(old, new, 1), "replaced": count if replace_all else 1}


def plan_edit(text, old, every=False, anchors=()):
    """What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
    and only when no unique anchor exists, read the file and write it back whole."""
    count = text.count(old)
    if count == 0:
        return ("read_again", None)
    if count == 1:
        return ("edit", old)
    if every:
        return ("replace_all", old)
    for anchor in anchors:
        if old in anchor and text.count(anchor) == 1:
            return ("edit", anchor)
    return ("read_write", None)


def tool_set(platform, tools=None, allowed_tools=(), disallowed_tools=()):
    """The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
    or `allowed_tools` (naming either in allowed_tools restores both), or when Bash is removed."""
    if tools is not None:
        have = [t for t in tools if t in BASE_TOOLS + SEARCH_TOOLS]
    else:
        have = list(BASE_TOOLS)
        if platform == "windows" or any(t in SEARCH_TOOLS for t in allowed_tools) or "Bash" in disallowed_tools:
            have += list(SEARCH_TOOLS)
    return [t for t in have if t not in disallowed_tools]


def covered_by(rule):
    """The tools that a permission rule such as Read(secrets/**) applies to."""
    name = rule.split("(", 1)[0]
    return list(RULE_COVERS.get(name, ()))


def rule_tool(tool):
    """The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write."""
    for name, tools in RULE_COVERS.items():
        if tool in tools:
            return name
    return tool


def main():
    text = "def a():\n    return 1\n\ndef b():\n    return 1\n"
    for label, old, every in (("unique", "def a():", False), ("twice", "    return 1", False), ("twice, every", "    return 1", True), ("absent", "def c():", False)):
        r = edit(text, old, "X", every)
        print(f"edit {label}:", r["error"] if not r["ok"] else f"replaced {r['replaced']}")
    anchors = ["def b():\n    return 1"]
    print("plan, unique:", plan_edit(text, "def a():"))
    print("plan, twice with an anchor:", plan_edit(text, "    return 1", anchors=anchors))
    print("plan, twice, every one:", plan_edit(text, "    return 1", every=True))
    print("plan, twice, no unique anchor:", plan_edit(text, "    return 1", anchors=["return 1"]))
    for platform in ("linux", "windows"):
        print(f"{platform}, default:", ", ".join(tool_set(platform)))
    print("linux, allowedTools Grep:", ", ".join(tool_set("linux", allowed_tools=["Grep"])))
    print("linux, tools Read Grep Glob:", ", ".join(tool_set("linux", tools=["Read", "Grep", "Glob"])))
    print("linux, Bash removed:", ", ".join(tool_set("linux", disallowed_tools=["Bash"])))
    print("rules are written under:", ", ".join(f"{t} as {rule_tool(t)}" for t in ("Grep", "Glob", "Write", "Bash")))
    for rule in ("Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)"):
        print(f"{rule} covers:", ", ".join(covered_by(rule)) or "nothing")


if __name__ == "__main__":
    main()
```
```text
edit unique: replaced 1
edit twice: old_string appears 2 times
edit twice, every: replaced 2
edit absent: old_string not found
plan, unique: ('edit', 'def a():')
plan, twice with an anchor: ('edit', 'def b():\n    return 1')
plan, twice, every one: ('replace_all', '    return 1')
plan, twice, no unique anchor: ('read_write', None)
linux, default: Read, Write, Edit, Bash
windows, default: Read, Write, Edit, Bash, Grep, Glob
linux, allowedTools Grep: Read, Write, Edit, Bash, Grep, Glob
linux, tools Read Grep Glob: Read, Grep, Glob
linux, Bash removed: Read, Write, Edit, Grep, Glob
rules are written under: Grep as Read, Glob as Read, Write as Edit, Bash as Bash
Read(secrets/**) covers: Read, Grep, Glob
Edit(src/**) covers: Edit, Write
Write(src/**) covers: nothing
Bash(git log *) covers: Bash
```
```typescript
// The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
// on which platform, and which permission rule covers which tool.
//
// A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
// Read, Write, Edit, Bash, Grep and Glob.
import { logger } from "./logger.ts";
const log = logger("builtin_tools");
const SEARCH_TOOLS = ["Grep", "Glob"];
const BASE_TOOLS = ["Read", "Write", "Edit", "Bash"];
const RULE_COVERS: Record<string, string[]> = { Read: ["Read", "Grep", "Glob"], Edit: ["Edit", "Write"], Bash: ["Bash"] }; // a Write(path) rule is never matched

type EditResult = { ok: false; error: string } | { ok: true; text: string; replaced: number };

/** Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replaceAll is set. */
export function edit(text: string, old: string, replacement: string, replaceAll = false): EditResult {
  const count = text.split(old).length - 1;
  if (count === 0) return { ok: false, error: "old_string not found" };
  if (count > 1 && !replaceAll) return { ok: false, error: `old_string appears ${count} times` };
  return { ok: true, text: replaceAll ? text.split(old).join(replacement) : text.replace(old, () => replacement), replaced: replaceAll ? count : 1 };
}

const occurrences = (text: string, part: string) => text.split(part).length - 1;

/** What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
 * and only when no unique anchor exists, read the file and write it back whole. */
export function planEdit(text: string, old: string, every = false, anchors: string[] = []): [string, string | null] {
  const count = occurrences(text, old);
  if (count === 0) return ["read_again", null];
  if (count === 1) return ["edit", old];
  if (every) return ["replace_all", old];
  for (const anchor of anchors) if (anchor.includes(old) && occurrences(text, anchor) === 1) return ["edit", anchor];
  return ["read_write", null];
}

/** The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
 * or `allowedTools` (naming either in allowedTools restores both), or when Bash is removed. */
export function toolSet(platform: string, tools: string[] | null = null, allowedTools: string[] = [], disallowedTools: string[] = []): string[] {
  let have: string[];
  if (tools !== null) have = tools.filter((t) => [...BASE_TOOLS, ...SEARCH_TOOLS].includes(t));
  else {
    have = [...BASE_TOOLS];
    if (platform === "windows" || allowedTools.some((t) => SEARCH_TOOLS.includes(t)) || disallowedTools.includes("Bash")) have.push(...SEARCH_TOOLS);
  }
  return have.filter((t) => !disallowedTools.includes(t));
}

/** The tools that a permission rule such as Read(secrets/**) applies to. */
export function coveredBy(rule: string): string[] {
  return [...(RULE_COVERS[rule.split("(")[0]] ?? [])];
}

/** The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write. */
export function ruleTool(tool: string): string {
  for (const [name, tools] of Object.entries(RULE_COVERS)) if (tools.includes(tool)) return name;
  return tool;
}

function main() {
  const text = "def a():\n    return 1\n\ndef b():\n    return 1\n";
  for (const [label, old, every] of [["unique", "def a():", false], ["twice", "    return 1", false], ["twice, every", "    return 1", true], ["absent", "def c():", false]] as const) {
    const r = edit(text, old, "X", every);
    console.log(`edit ${label}:`, r.ok ? `replaced ${r.replaced}` : r.error);
  }
  const anchors = ["def b():\n    return 1"];
  const show = (p: [string, string | null]) => `(${p[0] === null ? "None" : `'${p[0]}'`}, ${p[1] === null ? "None" : JSON.stringify(p[1]).replace(/^"|"$/g, "'")})`;
  console.log("plan, unique:", show(planEdit(text, "def a():")));
  console.log("plan, twice with an anchor:", show(planEdit(text, "    return 1", false, anchors)));
  console.log("plan, twice, every one:", show(planEdit(text, "    return 1", true)));
  console.log("plan, twice, no unique anchor:", show(planEdit(text, "    return 1", false, ["return 1"])));
  for (const platform of ["linux", "windows"]) console.log(`${platform}, default:`, toolSet(platform).join(", "));
  console.log("linux, allowedTools Grep:", toolSet("linux", null, ["Grep"]).join(", "));
  console.log("linux, tools Read Grep Glob:", toolSet("linux", ["Read", "Grep", "Glob"]).join(", "));
  console.log("linux, Bash removed:", toolSet("linux", null, [], ["Bash"]).join(", "));
  console.log("rules are written under:", ["Grep", "Glob", "Write", "Bash"].map((t) => `${t} as ${ruleTool(t)}`).join(", "));
  for (const rule of ["Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)"]) console.log(`${rule} covers:`, coveredBy(rule).join(", ") || "nothing");
}

if (import.meta.main) main();
```
```text
edit unique: replaced 1
edit twice: old_string appears 2 times
edit twice, every: replaced 2
edit absent: old_string not found
plan, unique: ('edit', 'def a():')
plan, twice with an anchor: ('edit', 'def b():\n    return 1')
plan, twice, every one: ('replace_all', '    return 1')
plan, twice, no unique anchor: ('read_write', None)
linux, default: Read, Write, Edit, Bash
windows, default: Read, Write, Edit, Bash, Grep, Glob
linux, allowedTools Grep: Read, Write, Edit, Bash, Grep, Glob
linux, tools Read Grep Glob: Read, Grep, Glob
linux, Bash removed: Read, Write, Edit, Grep, Glob
rules are written under: Grep as Read, Glob as Read, Write as Edit, Bash as Bash
Read(secrets/**) covers: Read, Grep, Glob
Edit(src/**) covers: Edit, Write
Write(src/**) covers: nothing
Bash(git log *) covers: Bash
```
```java
import static harness.Show.py;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
 * on which platform, and which permission rule covers which tool.
 *
 * <p>A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
 * Read, Write, Edit, Bash, Grep and Glob.
 */
public final class BuiltinTools {
    private static final System.Logger LOG = System.getLogger(BuiltinTools.class.getName());
    static final List<String> SEARCH_TOOLS = List.of("Grep", "Glob");
    static final List<String> BASE_TOOLS = List.of("Read", "Write", "Edit", "Bash");
    static final Map<String, List<String>> RULE_COVERS = Map.of("Read", List.of("Read", "Grep", "Glob"), "Edit", List.of("Edit", "Write"), "Bash", List.of("Bash")); // a Write(path) rule is never matched
    static final List<String> RULE_ORDER = List.of("Read", "Edit", "Bash");

    /** The result of Edit: whether it applied, the new text and the number replaced, or the error. */
    record EditResult(boolean ok, String text, Integer replaced, String error) {}

    /** What to do for a change: the action and the string to edit with (null when none). */
    record Plan(String action, String anchor) {
        @Override
        public String toString() {
            return "(" + py(action) + ", " + py(anchor) + ")";
        }
    }

    static int count(String text, String part) {
        int n = 0;
        for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) n++;
        return n;
    }

    /** Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replaceAll is set. */
    static EditResult edit(String text, String old, String replacement, boolean replaceAll) {
        int count = count(text, old);
        if (count == 0) return new EditResult(false, null, null, "old_string not found");
        if (count > 1 && !replaceAll) return new EditResult(false, null, null, "old_string appears " + count + " times");
        int first = text.indexOf(old);
        String changed = replaceAll ? text.replace(old, replacement) : text.substring(0, first) + replacement + text.substring(first + old.length());
        return new EditResult(true, changed, replaceAll ? count : 1, null);
    }

    static EditResult edit(String text, String old, String replacement) {
        return edit(text, old, replacement, false);
    }

    /**
     * What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
     * and only when no unique anchor exists, read the file and write it back whole.
     */
    static Plan planEdit(String text, String old, boolean every, List<String> anchors) {
        int count = count(text, old);
        if (count == 0) return new Plan("read_again", null);
        if (count == 1) return new Plan("edit", old);
        if (every) return new Plan("replace_all", old);
        for (String anchor : anchors) if (anchor.contains(old) && count(text, anchor) == 1) return new Plan("edit", anchor);
        return new Plan("read_write", null);
    }

    static Plan planEdit(String text, String old) {
        return planEdit(text, old, false, List.of());
    }

    /**
     * The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
     * or `allowedTools` (naming either in allowedTools restores both), or when Bash is removed.
     */
    static List<String> toolSet(String platform, List<String> tools, List<String> allowedTools, List<String> disallowedTools) {
        List<String> have = new ArrayList<>();
        if (tools != null) {
            for (String t : tools) if (BASE_TOOLS.contains(t) || SEARCH_TOOLS.contains(t)) have.add(t);
        } else {
            have.addAll(BASE_TOOLS);
            if (platform.equals("windows") || allowedTools.stream().anyMatch(SEARCH_TOOLS::contains) || disallowedTools.contains("Bash")) have.addAll(SEARCH_TOOLS);
        }
        return have.stream().filter(t -> !disallowedTools.contains(t)).toList();
    }

    static List<String> toolSet(String platform) {
        return toolSet(platform, null, List.of(), List.of());
    }

    /** The tools that a permission rule such as Read(secrets/**) applies to. */
    static List<String> coveredBy(String rule) {
        return RULE_COVERS.getOrDefault(rule.split("\\(", 2)[0], List.of());
    }

    /** The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write. */
    static String ruleTool(String tool) {
        for (String name : RULE_ORDER) if (RULE_COVERS.get(name).contains(tool)) return name;
        return tool;
    }

    static String join(List<String> items) {
        return String.join(", ", items);
    }

    public static void main(String[] args) {
        String text = "def a():\n    return 1\n\ndef b():\n    return 1\n";
        Object[][] cases = {{"unique", "def a():", false}, {"twice", "    return 1", false}, {"twice, every", "    return 1", true}, {"absent", "def c():", false}};
        for (Object[] c : cases) {
            EditResult r = edit(text, (String) c[1], "X", (Boolean) c[2]);
            System.out.println("edit " + c[0] + ": " + (!r.ok() ? r.error() : "replaced " + r.replaced()));
        }
        List<String> anchors = List.of("def b():\n    return 1");
        System.out.println("plan, unique: " + planEdit(text, "def a():"));
        System.out.println("plan, twice with an anchor: " + planEdit(text, "    return 1", false, anchors));
        System.out.println("plan, twice, every one: " + planEdit(text, "    return 1", true, List.of()));
        System.out.println("plan, twice, no unique anchor: " + planEdit(text, "    return 1", false, List.of("return 1")));
        for (String platform : List.of("linux", "windows")) System.out.println(platform + ", default: " + join(toolSet(platform)));
        System.out.println("linux, allowedTools Grep: " + join(toolSet("linux", null, List.of("Grep"), List.of())));
        System.out.println("linux, tools Read Grep Glob: " + join(toolSet("linux", List.of("Read", "Grep", "Glob"), List.of(), List.of())));
        System.out.println("linux, Bash removed: " + join(toolSet("linux", null, List.of(), List.of("Bash"))));
        List<String> written = new ArrayList<>();
        for (String t : List.of("Grep", "Glob", "Write", "Bash")) written.add(t + " as " + ruleTool(t));
        System.out.println("rules are written under: " + join(written));
        for (String rule : List.of("Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)")) {
            List<String> covered = coveredBy(rule);
            System.out.println(rule + " covers: " + (covered.isEmpty() ? "nothing" : join(covered)));
        }
    }
}
```
```text
edit unique: replaced 1
edit twice: old_string appears 2 times
edit twice, every: replaced 2
edit absent: old_string not found
plan, unique: ('edit', 'def a():')
plan, twice with an anchor: ('edit', 'def b():\n    return 1')
plan, twice, every one: ('replace_all', '    return 1')
plan, twice, no unique anchor: ('read_write', None)
linux, default: Read, Write, Edit, Bash
windows, default: Read, Write, Edit, Bash, Grep, Glob
linux, allowedTools Grep: Read, Write, Edit, Bash, Grep, Glob
linux, tools Read Grep Glob: Read, Grep, Glob
linux, Bash removed: Read, Write, Edit, Grep, Glob
rules are written under: Grep as Read, Glob as Read, Write as Edit, Bash as Bash
Read(secrets/**) covers: Read, Grep, Glob
Edit(src/**) covers: Edit, Write
Write(src/**) covers: nothing
Bash(git log *) covers: Bash
```
```kotlin
import harness.Show.py

private val log = System.getLogger("builtin_tools")

/**
 * The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
 * on which platform, and which permission rule covers which tool.
 *
 * A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
 * Read, Write, Edit, Bash, Grep and Glob.
 */
val SEARCH_TOOLS = listOf("Grep", "Glob")
val BASE_TOOLS = listOf("Read", "Write", "Edit", "Bash")
val RULE_COVERS = mapOf("Read" to listOf("Read", "Grep", "Glob"), "Edit" to listOf("Edit", "Write"), "Bash" to listOf("Bash")) // a Write(path) rule is never matched

/** The result of Edit: whether it applied, the new text and the number replaced, or the error. */
data class EditResult(val ok: Boolean, val text: String? = null, val replaced: Int? = null, val error: String? = null)

/** What to do for a change: the action and the string to edit with (null when none). */
data class Plan(val action: String, val anchor: String?) {
    override fun toString() = "(${py(action)}, ${py(anchor)})"
}

private fun count(text: String, part: String): Int {
    var n = 0
    var i = text.indexOf(part)
    while (i >= 0) {
        n++
        i = text.indexOf(part, i + part.length)
    }
    return n
}

/** Edit is an exact string replacement: no regex, no fuzzy match. old must be present, and appear once unless replaceAll is set. */
fun edit(text: String, old: String, replacement: String, replaceAll: Boolean = false): EditResult {
    val count = count(text, old)
    if (count == 0) return EditResult(false, error = "old_string not found")
    if (count > 1 && !replaceAll) return EditResult(false, error = "old_string appears $count times")
    val changed = if (replaceAll) text.replace(old, replacement) else text.replaceFirst(old, replacement)
    return EditResult(true, text = changed, replaced = if (replaceAll) count else 1)
}

/**
 * What to do for a change to `old`: Edit as it is, Edit with a longer unique string that holds it, replace_all for every occurrence,
 * and only when no unique anchor exists, read the file and write it back whole.
 */
fun planEdit(text: String, old: String, every: Boolean = false, anchors: List<String> = emptyList()): Plan {
    val count = count(text, old)
    if (count == 0) return Plan("read_again", null)
    if (count == 1) return Plan("edit", old)
    if (every) return Plan("replace_all", old)
    for (anchor in anchors) if (old in anchor && count(text, anchor) == 1) return Plan("edit", anchor)
    return Plan("read_write", null)
}

/**
 * The six tools a session has. Grep and Glob are in the default set on Windows only; elsewhere they return when named in `tools`
 * or `allowedTools` (naming either in allowedTools restores both), or when Bash is removed.
 */
fun toolSet(platform: String, tools: List<String>? = null, allowedTools: List<String> = emptyList(), disallowedTools: List<String> = emptyList()): List<String> {
    val have = if (tools != null) tools.filter { it in BASE_TOOLS + SEARCH_TOOLS } else {
        BASE_TOOLS + if (platform == "windows" || allowedTools.any { it in SEARCH_TOOLS } || "Bash" in disallowedTools) SEARCH_TOOLS else emptyList()
    }
    return have.filter { it !in disallowedTools }
}

/** The tools that a permission rule such as Read(secrets/...) applies to. */
fun coveredBy(rule: String): List<String> = RULE_COVERS[rule.substringBefore("(")] ?: emptyList()

/** The tool name that a permission rule is written under: Read(...) covers Read, Grep and Glob; Edit(...) covers Edit and Write. */
fun ruleTool(tool: String): String = RULE_COVERS.entries.firstOrNull { tool in it.value }?.key ?: tool

fun main() {
    val text = "def a():\n    return 1\n\ndef b():\n    return 1\n"
    for ((label, old, every) in listOf(Triple("unique", "def a():", false), Triple("twice", "    return 1", false), Triple("twice, every", "    return 1", true), Triple("absent", "def c():", false))) {
        val r = edit(text, old, "X", every)
        println("edit $label: ${if (!r.ok) r.error else "replaced ${r.replaced}"}")
    }
    val anchors = listOf("def b():\n    return 1")
    println("plan, unique: ${planEdit(text, "def a():")}")
    println("plan, twice with an anchor: ${planEdit(text, "    return 1", anchors = anchors)}")
    println("plan, twice, every one: ${planEdit(text, "    return 1", every = true)}")
    println("plan, twice, no unique anchor: ${planEdit(text, "    return 1", anchors = listOf("return 1"))}")
    for (platform in listOf("linux", "windows")) println("$platform, default: ${toolSet(platform).joinToString(", ")}")
    println("linux, allowedTools Grep: ${toolSet("linux", allowedTools = listOf("Grep")).joinToString(", ")}")
    println("linux, tools Read Grep Glob: ${toolSet("linux", tools = listOf("Read", "Grep", "Glob")).joinToString(", ")}")
    println("linux, Bash removed: ${toolSet("linux", disallowedTools = listOf("Bash")).joinToString(", ")}")
    println("rules are written under: ${listOf("Grep", "Glob", "Write", "Bash").joinToString(", ") { "$it as ${ruleTool(it)}" }}")
    for (rule in listOf("Read(secrets/**)", "Edit(src/**)", "Write(src/**)", "Bash(git log *)")) println("$rule covers: ${coveredBy(rule).joinToString(", ").ifEmpty { "nothing" }}")
}
```
```text
edit unique: replaced 1
edit twice: old_string appears 2 times
edit twice, every: replaced 2
edit absent: old_string not found
plan, unique: ('edit', 'def a():')
plan, twice with an anchor: ('edit', 'def b():\n    return 1')
plan, twice, every one: ('replace_all', '    return 1')
plan, twice, no unique anchor: ('read_write', None)
linux, default: Read, Write, Edit, Bash
windows, default: Read, Write, Edit, Bash, Grep, Glob
linux, allowedTools Grep: Read, Write, Edit, Bash, Grep, Glob
linux, tools Read Grep Glob: Read, Grep, Glob
linux, Bash removed: Read, Write, Edit, Grep, Glob
rules are written under: Grep as Read, Glob as Read, Write as Edit, Bash as Bash
Read(secrets/**) covers: Read, Grep, Glob
Edit(src/**) covers: Edit, Write
Write(src/**) covers: nothing
Bash(git log *) covers: Bash
```
<!-- /example -->

Read the output from the top. A unique string is replaced; the same string twice is refused with its count, unless `replace_all` is set; an absent string is refused as not found. The plan edits as it is when the text is unique, widens the anchor when a longer unique string holds it, replaces all when every occurrence should change, and reaches for Read and Write only when the only anchor it has is itself repeated. On Linux the default set has no Grep and no Glob, naming one of them in `allowedTools` brings back both, listing them in `tools` gives exactly the list, and removing Bash gives them back as well. The last lines show that a rule written for Read covers Grep and Glob, and a rule written for Edit covers Write; a rule written for Write covers nothing.

## Traps

These are the wrong answers that the exam's options for this task statement offer, each with the reason it is rejected.

1. **"Read every file in the repository first, so that nothing is missed."** It is tempting because it feels thorough. The exam rejects it: it spends the context on files that do not matter. Start with a Grep for entry points and follow the imports with Read.
2. **"Use Grep to find all the test files."** It is tempting because Grep is the search tool everyone knows. The exam rejects it: Grep matches contents, and Glob matches names. A pattern such as `**/*.test.tsx` is a Glob.
3. **"When Edit finds the text twice, rewrite the file with Write."** It is tempting because the guide names Read plus Write as the fallback. The exam keys that fallback for a non-unique match, and the documentation offers two cheaper remedies first: a longer unique string, or `replace_all` when every occurrence should change. Rewrite the file when no unique anchor exists.
4. **"Use Write to change one line, since it replaces the file anyway."** It is tempting because it never fails to match. The exam rejects it: Write overwrites with the full content, so an accidental change elsewhere goes unseen. For partial changes to an existing file, use Edit.
5. **"A rule on Grep covers the searches of a default Linux session."** It is tempting because the guide lists Grep as the content search. The exam keys Grep for content search; the product's default set on macOS, Linux and WSL has no Grep, and the searches arrive as Bash calls that only a Bash rule or a hook on Bash sees.

## Quiz

1. An agent must list every component's spec file in a repository, such as `Button.test.tsx`, wherever it sits. Which tool fits?
   - **a**: A content search for the string `describe(` or `it(`
   - **b**: A name pattern search across every folder
   - **c**: A full read of each folder in turn
   - **d**: A shell loop that opens each file in turn

2. A `routes.yaml` holds ten copies of the same block, told apart only by where each one sits. An edit to the third copy is refused because the text to replace appears ten times, and every snippet around it also appears ten times. What should the agent do?
   - **a**: Read it and write the whole thing again with the one alteration
   - **b**: Retry with `replace_all` set, then repair the nine others by hand
   - **c**: Widen the snippet with more surrounding lines until it is unique
   - **d**: Use a content search to locate the third block, then retry unchanged

<details>
<summary>Answer key</summary>

1. **b**. The job is matching names, which is what a name pattern search is for. *a* is ruled out because a content search finds lines inside files: "Grep finds lines inside files" and not the names that match a pattern. *c* is ruled out because a folder cannot be read: the table's "Reads files, not directories". *d* is ruled out because opening every file spends the context on files that do not matter: "it spends the context on files that do not matter".
2. **a**. The guide keys Read plus Write as the fallback for a non-unique Edit, and here no unique anchor exists. The product's documented remedies are a longer unique string or `replace_all`; Read plus Write is the guide's route when neither applies, and it is where this case lands. *b* is ruled out because "`replace_all` changes every occurrence, which is right for a rename and wrong when only one of three similar lines should change." *c* is ruled out because the page names this very case as the one for the fallback: "for example when a file is made of repeated blocks that differ only by position". *d* is ruled out because "Grep finds lines inside files" and does not change them, so the retried edit would be refused again.

</details>
