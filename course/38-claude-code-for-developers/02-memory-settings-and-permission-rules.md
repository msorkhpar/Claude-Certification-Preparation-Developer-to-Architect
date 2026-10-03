# The memory file, settings layers and permission rules

**Level:** Developer · **Module 38:** Claude Code for developers · **Page 2 of 3**
**Exams:** DV7; A3

**After this page you can** write a project memory file that Claude follows, say in which order memory files and settings files apply, decide which layer a setting belongs in, write permission rules that do what you mean, and predict the outcome of a call against merged rules.

Checked on 2026-10-03 against the Claude Code documentation (memory, settings and precedence, permissions and permission modes), which mention behaviour up to Claude Code v2.1.286. The example is the course's own model of the documented rules, written to be tested offline in Python and TypeScript. It is not Claude Code's code, and it covers the rules this page states and no others.

## Why it matters

Most of the configuration an exam asks about lives in three places: a memory file that tells Claude how the project works, settings files that say what runs freely, and permission rules inside the settings. People get each of them wrong in a predictable way. They write a memory file that is too long to be followed, they put a personal choice in the shared file, and they write an allow rule that a deny rule silently beats.

## The idea

### The memory file

CLAUDE.md carries persistent instructions. Both memory systems "are loaded at the start of every conversation", and "Claude treats them as context, not enforced configuration." The consequence is stated in the same place: "To block an action regardless of what Claude decides, use a PreToolUse hook instead." A memory file asks and a hook enforces.

What goes in it follows from that. The documentation says to "target under 200 lines per CLAUDE.md file", because "Longer files consume more context and reduce adherence." It should state what Claude would otherwise get wrong, such as the exact commands for tests and lint, in concrete and checkable words. Run `/init` to generate a starting file: "Claude analyzes your codebase and creates a file with build commands, test instructions, and project conventions it discovers." If one exists, `/init` "suggests improvements rather than overwriting it". Then refine it by hand with "instructions Claude wouldn't discover on its own". The command `/memory` opens it for editing.

Files load by location. Managed policy comes first, then the user file in `~/.claude/`, then the project's `CLAUDE.md` files from the repository root down to the working directory, and `CLAUDE.local.md` follows its `CLAUDE.md` for personal, project-specific notes that belong in `.gitignore`. Files in subdirectories below the working directory load on demand, when Claude reads files there. The later files are the more specific, and they are read last.

A memory file can import others with `@path/to/import`. "Imported files are expanded and loaded into context at launch alongside the CLAUDE.md that references them", and they "can recursively import other files, with a maximum depth of four hops." Two details matter. Imports organise a long file but "don't reduce its context cost, because imported files also load at launch", so an import is not a way to hide length. And "Import parsing skips Markdown code spans and fenced code blocks": writing a path in backticks keeps it literal, and the same text outside backticks imports the file.

### Settings: five layers

Settings are JSON files. The settings page gives their order, highest first: managed settings, the command line, project local (`.claude/settings.local.json`), shared project (`.claude/settings.json`) and user (`~/.claude/settings.json`). "A key set at a higher level overrides the same key set lower down." Lists, such as the allow and deny arrays, are not replaced: they combine across layers, which is why a deny rule written anywhere reaches every session.

Pick the layer by who the setting is for. The team's choices go in the shared project file, which is committed: the permission rules that every developer needs, the default model for the repository. A personal override goes in the local file, which belongs in `.gitignore`: this developer's preferred model, a path on their machine. A choice for every project that one person works on goes in the user file. A rule that must hold regardless of what any developer writes goes in managed settings, which nothing below it can override.

Two safeguards live here. A shared or local file cannot set `defaultMode` to `auto` or `bypassPermissions`, as page 1 showed. And project permission rules wait for workspace trust: a rule in a repository you have just cloned does not apply before you have trusted the folder.

### Permission rules

A rule names a tool and optionally a pattern, and it has one of three effects. "Rules are evaluated in order: deny, then ask, then allow. The first match in that order determines the outcome, and rule specificity doesn't change the order." So "An allow rule can't carve an exception out of a deny rule": a broad deny of `Bash(aws *)` blocks `aws s3 ls` even when a narrower allow names it. A call that no rule matches falls to the mode: in Manual, a read runs and anything else asks.

The shapes to know:

| Rule | Meaning |
|---|---|
| `Bash` or `Bash(*)` | Every shell command. As an allow rule, that approves all of them. As a deny rule, it removes the tool from Claude's context |
| `Bash(npm run build)` | That exact command |
| `Bash(git log *)` | `git log` with any arguments, and not `git push` |
| `Read(./.env)` | Reading that file |
| `WebFetch(domain:example.com)` | Fetches to one domain |

In `Bash(git log *)` the space before the `*` matters: the words before it are matched as written, so the rule allows only that subcommand. `Bash(ls *)` also matches the bare `ls` and does not match `lsof`. The compound command `npm run build && git push origin main` is not covered by an allow rule for `npm run build`, because each part is checked. Two more rules of thumb: a `Write(...)` path rule is not consulted, so protect files with `Edit` or `Read` rules, and a deny on `Read` for a path also stops Edit and Write on it.

A bare `Bash` allow rule is the commonest serious mistake. It approves every command that no deny or ask rule catches, which turns a careful configuration into an open one.

<!-- example: m38-settings-layers tabs: python,typescript -->
```python
"""Claude Code settings layers, permission rules and memory files, resolved offline.

Three small functions that follow what the Claude Code documentation says (read on 2026-10-03): settings merge from five levels with
the highest level winning a scalar key and lists combining; permission rules are checked deny, then ask, then allow, with the first
match deciding; and CLAUDE.md files are concatenated from the broadest scope to the most specific, with @imports expanded. It is a
teaching model of the documented behaviour, not the product's code: Read and Edit patterns use a reduced form of the gitignore rules.
"""
import re

LEVELS = ["managed", "command line", "local", "project", "user"]  # highest precedence first
REPO_LEVELS = ("project", "local")  # files that live in the repository


def merge(low, high):
    out = dict(low)
    for key, value in high.items():
        if isinstance(value, dict) and isinstance(out.get(key), dict):
            out[key] = merge(out[key], value)
        elif isinstance(value, list) and isinstance(out.get(key), list):
            out[key] = out[key] + [v for v in value if v not in out[key]]
        else:
            out[key] = value
    return out


def effective_settings(layers, trusted=True):
    """Merge the five levels. A repository file cannot set defaultMode auto or bypassPermissions, and its allow rules wait for trust."""
    result = {}
    for level in reversed(LEVELS):
        part = {k: (dict(v) if isinstance(v, dict) else v) for k, v in layers.get(level, {}).items()}
        perms = part.get("permissions")
        if level in REPO_LEVELS and perms:
            if perms.get("defaultMode") in ("auto", "bypassPermissions"):
                perms.pop("defaultMode")
            if not trusted:
                perms.pop("allow", None)
        result = merge(result, part)
    return result


def _bash_regex(rule):
    rule = rule[:-2] + " *" if rule.endswith(":*") else rule
    if rule.endswith(" *") and "*" not in rule[:-2]:
        return re.compile(re.escape(rule[:-2]) + r"(?: .*)?", re.S)
    return re.compile(".*".join(re.escape(part) for part in rule.split("*")), re.S)


def _glob(p):
    out, i = "", 0
    while i < len(p):
        if p.startswith("**/", i):
            out, i = out + "(?:.*/)?", i + 3
        elif p.startswith("**", i):
            out, i = out + ".*", i + 2
        elif p[i] == "*":
            out, i = out + "[^/]*", i + 1
        else:
            out, i = out + re.escape(p[i]), i + 1
    return out


def _path_matches(pattern, path, kind):
    pattern, path = pattern.removeprefix("./"), path.removeprefix("./")
    if pattern.startswith("//"):
        return re.fullmatch(_glob(pattern[1:]), path) is not None
    if pattern.startswith("/"):
        return re.fullmatch(_glob(pattern[1:]), path) is not None
    if "/" not in pattern:
        return re.fullmatch("(?:.*/)?" + _glob(pattern), path) is not None
    first = pattern.split("/")[0]
    deep = kind != "allow" and "*" not in first and pattern.count("/") >= 1 and not pattern.startswith("**")
    return re.fullmatch(("(?:.*/)?" if deep else "") + _glob(pattern), path) is not None


def _rule_matches(rule, tool, arg, kind):
    m = re.fullmatch(r"(\w+)(?:\((.*)\))?", rule, re.S)
    if not m or m.group(1) != tool:
        return False
    if m.group(2) is None:
        return True
    return bool(_bash_regex(m.group(2)).fullmatch(arg)) if tool == "Bash" else _path_matches(m.group(2), arg, kind)


def decide(settings, tool, arg="", mode="default"):
    """allow, ask or deny. Deny first, then ask, then allow; a Bash call is split at && || ; | & and newlines and every part must pass.
    In acceptEdits mode an edit that no rule decided is accepted instead of asked."""
    perms = settings.get("permissions", {})
    rules = {kind: perms.get(kind, []) for kind in ("deny", "ask", "allow")}
    if tool == "Bash":
        parts = [p.strip() for p in re.split(r"&&|\|\||;|\||&|\n", arg) if p.strip()]
    else:
        parts = [arg]
    for kind in ("deny", "ask"):
        if any(_rule_matches(r, tool, p, kind) for r in rules[kind] for p in parts):
            return kind
    if tool in ("Edit", "Write") and any(_rule_matches(r.replace("Read(", "Edit(", 1), "Edit", arg, "deny") for r in rules["deny"] if r.startswith("Read(")):
        return "deny"  # a Read deny rule also blocks edits and writes on the same path
    if all(any(_rule_matches(r, tool, p, "allow") for r in rules["allow"]) for p in parts):
        return "allow"
    if tool in ("Read", "Grep", "Glob"):
        return "allow"  # read-only tools inside the working directory need no approval
    if mode == "acceptEdits" and tool in ("Edit", "Write"):
        return "allow"
    return "ask"  # nothing matched: Manual mode asks


def load_memory(files, cwd, managed=None, user=None):
    """The memory files in the order they reach the context: managed, user, then each directory from the root down to cwd, where
    CLAUDE.md comes before CLAUDE.local.md. files maps a path to its text; @path lines import files (relative to the importing file,
    at most four hops deep) and a path inside backticks is not an import."""
    order = [p for p in (managed, user) if p in files]
    parts = cwd.strip("/").split("/")
    for depth in range(len(parts) + 1):
        base = "/" + "/".join(parts[:depth])
        base = base.rstrip("/")
        order += [p for p in (f"{base}/CLAUDE.md", f"{base}/CLAUDE.local.md") if p in files]
    loaded = []

    def add(path, hops):
        loaded.append(path)
        if hops == 4:
            return
        for token in re.findall(r"(?<![`\w])@([\w./-]+)", re.sub(r"`[^`]*`", "", files[path])):
            target = token if token.startswith("/") else _join(path, token)
            if target in files:
                add(target, hops + 1)

    for path in order:
        add(path, 0)
    return loaded


def _join(path, rel):
    stack = path.split("/")[:-1]
    for part in rel.split("/"):
        if part == "..":
            stack.pop()
        elif part != ".":
            stack.append(part)
    return "/".join(stack)


def main():
    layers = {
        "managed": {"permissions": {"deny": ["Bash(curl *)"]}},
        "user": {"model": "sonnet", "permissions": {"allow": ["Bash(git status *)"]}},
        "project": {"model": "opus", "permissions": {"defaultMode": "bypassPermissions", "allow": ["Bash(npm run *)", "Bash(curl *)"],
                                                     "ask": ["Bash(git push *)"], "deny": ["Read(./.env)"]}},
        "local": {"permissions": {"allow": ["Bash(git push *)"]}},
    }
    s = effective_settings(layers)
    print("model:", s["model"], "| defaultMode:", s["permissions"].get("defaultMode", "(not set)"))
    print("allow rules:", s["permissions"]["allow"])
    calls = [("Bash", "npm run build"), ("Bash", "npm run build && git push origin main"), ("Bash", "curl https://example.com"),
             ("Read", "./.env"), ("Edit", ".env"), ("Bash", "git status"), ("Bash", "rm -rf build")]
    for tool, arg in calls:
        print(f"{tool}({arg}) -> {decide(s, tool, arg)}")
    print("project not trusted yet: npm run build ->", decide(effective_settings(layers, trusted=False), "Bash", "npm run build"))
    files = {"/etc/claude-code/CLAUDE.md": "managed", "/home/dev/.claude/CLAUDE.md": "user", "/repo/CLAUDE.md": "See @docs/git.md and `@README` here",
             "/repo/docs/git.md": "git rules", "/repo/svc/CLAUDE.md": "service", "/repo/svc/CLAUDE.local.md": "mine", "/repo/other/CLAUDE.md": "other"}
    print("memory order for /repo/svc:", load_memory(files, "/repo/svc", "/etc/claude-code/CLAUDE.md", "/home/dev/.claude/CLAUDE.md"))


if __name__ == "__main__":
    main()
```
```text
model: opus | defaultMode: (not set)
allow rules: ['Bash(git status *)', 'Bash(npm run *)', 'Bash(curl *)', 'Bash(git push *)']
Bash(npm run build) -> allow
Bash(npm run build && git push origin main) -> ask
Bash(curl https://example.com) -> deny
Read(./.env) -> deny
Edit(.env) -> deny
Bash(git status) -> allow
Bash(rm -rf build) -> ask
project not trusted yet: npm run build -> ask
memory order for /repo/svc: ['/etc/claude-code/CLAUDE.md', '/home/dev/.claude/CLAUDE.md', '/repo/CLAUDE.md', '/repo/docs/git.md', '/repo/svc/CLAUDE.md', '/repo/svc/CLAUDE.local.md']
```
```typescript
// Claude Code settings layers, permission rules and memory files, resolved offline.
//
// Three small functions that follow what the Claude Code documentation says (read on 2026-10-03): settings merge from five levels with
// the highest level winning a scalar key and lists combining; permission rules are checked deny, then ask, then allow, with the first
// match deciding; and CLAUDE.md files are concatenated from the broadest scope to the most specific, with @imports expanded. It is a
// teaching model of the documented behaviour, not the product's code: Read and Edit patterns use a reduced form of the gitignore rules.

type Json = Record<string, any>;
type Kind = "allow" | "ask" | "deny";

export const LEVELS = ["managed", "command line", "local", "project", "user"]; // highest precedence first
const REPO_LEVELS = ["project", "local"]; // files that live in the repository

export function merge(low: Json, high: Json): Json {
  const out: Json = { ...low };
  for (const [key, value] of Object.entries(high)) {
    if (value && typeof value === "object" && !Array.isArray(value) && out[key] && typeof out[key] === "object" && !Array.isArray(out[key])) out[key] = merge(out[key], value);
    else if (Array.isArray(value) && Array.isArray(out[key])) out[key] = [...out[key], ...value.filter((v) => !out[key].includes(v))];
    else out[key] = value;
  }
  return out;
}

/** Merge the five levels. A repository file cannot set defaultMode auto or bypassPermissions, and its allow rules wait for trust. */
export function effectiveSettings(layers: Record<string, Json>, trusted = true): Json {
  let result: Json = {};
  for (const level of [...LEVELS].reverse()) {
    const part: Json = {};
    for (const [k, v] of Object.entries(layers[level] ?? {})) part[k] = v && typeof v === "object" && !Array.isArray(v) ? { ...v } : v;
    const perms = part.permissions;
    if (REPO_LEVELS.includes(level) && perms) {
      if (perms.defaultMode === "auto" || perms.defaultMode === "bypassPermissions") delete perms.defaultMode;
      if (!trusted) delete perms.allow;
    }
    result = merge(result, part);
  }
  return result;
}

const escape = (s: string) => s.replace(/[.*+?^${}()|[\]\\\/-]/g, "\\$&");

function bashRegex(ruleIn: string): RegExp {
  const rule = ruleIn.endsWith(":*") ? ruleIn.slice(0, -2) + " *" : ruleIn;
  if (rule.endsWith(" *") && !rule.slice(0, -2).includes("*")) return new RegExp("^" + escape(rule.slice(0, -2)) + "(?: [\\s\\S]*)?$");
  return new RegExp("^" + rule.split("*").map(escape).join("[\\s\\S]*") + "$");
}

function glob(p: string): string {
  let out = "";
  let i = 0;
  while (i < p.length) {
    if (p.startsWith("**/", i)) { out += "(?:.*/)?"; i += 3; }
    else if (p.startsWith("**", i)) { out += ".*"; i += 2; }
    else if (p[i] === "*") { out += "[^/]*"; i += 1; }
    else { out += escape(p[i]); i += 1; }
  }
  return out;
}

function pathMatches(patternIn: string, pathIn: string, kind: Kind): boolean {
  const pattern = patternIn.replace(/^\.\//, "");
  const path = pathIn.replace(/^\.\//, "");
  if (pattern.startsWith("//")) return new RegExp("^" + glob(pattern.slice(1)) + "$").test(path);
  if (pattern.startsWith("/")) return new RegExp("^" + glob(pattern.slice(1)) + "$").test(path);
  if (!pattern.includes("/")) return new RegExp("^(?:.*/)?" + glob(pattern) + "$").test(path);
  const first = pattern.split("/")[0];
  const deep = kind !== "allow" && !first.includes("*") && !pattern.startsWith("**");
  return new RegExp("^" + (deep ? "(?:.*/)?" : "") + glob(pattern) + "$").test(path);
}

function ruleMatches(rule: string, tool: string, arg: string, kind: Kind): boolean {
  const m = /^(\w+)(?:\(([\s\S]*)\))?$/.exec(rule);
  if (!m || m[1] !== tool) return false;
  if (m[2] === undefined) return true;
  return tool === "Bash" ? bashRegex(m[2]).test(arg) : pathMatches(m[2], arg, kind);
}

/** allow, ask or deny. Deny first, then ask, then allow; a Bash call is split at && || ; | & and newlines and every part must pass.
 * In acceptEdits mode an edit that no rule decided is accepted instead of asked. */
export function decide(settings: Json, tool: string, arg = "", mode = "default"): Kind {
  const perms = settings.permissions ?? {};
  const rules: Record<Kind, string[]> = { deny: perms.deny ?? [], ask: perms.ask ?? [], allow: perms.allow ?? [] };
  const parts = tool === "Bash" ? arg.split(/&&|\|\||;|\||&|\n/).map((p) => p.trim()).filter(Boolean) : [arg];
  for (const kind of ["deny", "ask"] as Kind[]) {
    if (rules[kind].some((r) => parts.some((p) => ruleMatches(r, tool, p, kind)))) return kind;
  }
  if ((tool === "Edit" || tool === "Write") && rules.deny.some((r) => r.startsWith("Read(") && ruleMatches(r.replace("Read(", "Edit("), "Edit", arg, "deny"))) return "deny"; // a Read deny rule also blocks edits and writes on the same path
  if (parts.every((p) => rules.allow.some((r) => ruleMatches(r, tool, p, "allow")))) return "allow";
  if (tool === "Read" || tool === "Grep" || tool === "Glob") return "allow"; // read-only tools inside the working directory need no approval
  if (mode === "acceptEdits" && (tool === "Edit" || tool === "Write")) return "allow";
  return "ask"; // nothing matched: Manual mode asks
}

function join(path: string, rel: string): string {
  const stack = path.split("/").slice(0, -1);
  for (const part of rel.split("/")) {
    if (part === "..") stack.pop();
    else if (part !== ".") stack.push(part);
  }
  return stack.join("/");
}

/** The memory files in the order they reach the context: managed, user, then each directory from the root down to cwd, where
 * CLAUDE.md comes before CLAUDE.local.md. files maps a path to its text; @path lines import files (relative to the importing file,
 * at most four hops deep) and a path inside backticks is not an import. */
export function loadMemory(files: Record<string, string>, cwd: string, managed?: string, user?: string): string[] {
  const order = [managed, user].filter((p): p is string => !!p && p in files);
  const parts = cwd.replace(/^\/+|\/+$/g, "").split("/");
  for (let depth = 0; depth <= parts.length; depth++) {
    const base = ("/" + parts.slice(0, depth).join("/")).replace(/\/+$/, "");
    for (const p of [`${base}/CLAUDE.md`, `${base}/CLAUDE.local.md`]) if (p in files) order.push(p);
  }
  const loaded: string[] = [];
  const add = (path: string, hops: number) => {
    loaded.push(path);
    if (hops === 4) return;
    for (const m of files[path].replace(/`[^`]*`/g, "").matchAll(/(?<![`\w])@([\w./-]+)/g)) {
      const target = m[1].startsWith("/") ? m[1] : join(path, m[1]);
      if (target in files) add(target, hops + 1);
    }
  };
  for (const p of order) add(p, 0);
  return loaded;
}

function main() {
  const layers: Record<string, Json> = {
    managed: { permissions: { deny: ["Bash(curl *)"] } },
    user: { model: "sonnet", permissions: { allow: ["Bash(git status *)"] } },
    project: { model: "opus", permissions: { defaultMode: "bypassPermissions", allow: ["Bash(npm run *)", "Bash(curl *)"], ask: ["Bash(git push *)"], deny: ["Read(./.env)"] } },
    local: { permissions: { allow: ["Bash(git push *)"] } },
  };
  const s = effectiveSettings(layers);
  console.log("model:", s.model, "| defaultMode:", s.permissions.defaultMode ?? "(not set)");
  console.log("allow rules:", `[${s.permissions.allow.map((r: string) => `'${r}'`).join(", ")}]`);
  const calls: Array<[string, string]> = [["Bash", "npm run build"], ["Bash", "npm run build && git push origin main"], ["Bash", "curl https://example.com"],
    ["Read", "./.env"], ["Edit", ".env"], ["Bash", "git status"], ["Bash", "rm -rf build"]];
  for (const [tool, arg] of calls) console.log(`${tool}(${arg}) -> ${decide(s, tool, arg)}`);
  console.log("project not trusted yet: npm run build ->", decide(effectiveSettings(layers, false), "Bash", "npm run build"));
  const files: Record<string, string> = { "/etc/claude-code/CLAUDE.md": "managed", "/home/dev/.claude/CLAUDE.md": "user", "/repo/CLAUDE.md": "See @docs/git.md and `@README` here",
    "/repo/docs/git.md": "git rules", "/repo/svc/CLAUDE.md": "service", "/repo/svc/CLAUDE.local.md": "mine", "/repo/other/CLAUDE.md": "other" };
  const order = loadMemory(files, "/repo/svc", "/etc/claude-code/CLAUDE.md", "/home/dev/.claude/CLAUDE.md");
  console.log("memory order for /repo/svc:", `[${order.map((p) => `'${p}'`).join(", ")}]`);
}

if (import.meta.main) main();
```
```text
model: opus | defaultMode: (not set)
allow rules: ['Bash(git status *)', 'Bash(npm run *)', 'Bash(curl *)', 'Bash(git push *)']
Bash(npm run build) -> allow
Bash(npm run build && git push origin main) -> ask
Bash(curl https://example.com) -> deny
Read(./.env) -> deny
Edit(.env) -> deny
Bash(git status) -> allow
Bash(rm -rf build) -> ask
project not trusted yet: npm run build -> ask
memory order for /repo/svc: ['/etc/claude-code/CLAUDE.md', '/home/dev/.claude/CLAUDE.md', '/repo/CLAUDE.md', '/repo/docs/git.md', '/repo/svc/CLAUDE.md', '/repo/svc/CLAUDE.local.md']
```
<!-- /example -->

The example merges layers and decides calls. It starts with a user file that allows `Bash(npm run *)` and a project file that adds `git push` and `curl` rules, and the first line shows the winning `model`. A call to `npm run build` is allowed. The compound command with `git push` asks, and `curl` is denied. A read of `.env` is denied, and so is an edit of it, because the read deny covers both. When the project is not yet trusted, even `npm run build` falls back to asking. The last line prints the memory order for a service folder: the managed file, the user file, the repository `CLAUDE.md`, an imported note, the service's own `CLAUDE.md` and last its `CLAUDE.local.md`. Python and TypeScript print the same text.

## Traps

1. **Writing instructions as if they were enforcement.** A memory line asks. For anything that must hold, use a deny rule or a hook.
2. **Putting a personal choice in the shared file.** The next teammate inherits it. Personal values go in `settings.local.json`, which is ignored by git.
3. **Expecting a narrower allow to beat a broader deny.** Deny is checked first and wins whatever the specificity.
4. **Writing a path rule for `Write`.** It is never consulted. Use `Edit` and `Read` for the files you want to protect.

## Quiz

1. A team wants to block reads of `.env` for every developer, and also adds a line about it to the memory file. Which statement is right?
   - **a**: The memory line enforces it, because that file loads at the start of a session
   - **b**: The deny entry enforces it, while the text only asks Claude
   - **c**: Both enforce it equally, since each is read before the first tool call
   - **d**: Neither enforces it, because project files never override a user file

2. A settings file allows `Bash(git log *)` and another layer lists `Bash(git *)` under deny. What happens when Claude runs `git log --oneline`?
   - **a**: It is allowed, because the allow rule names the narrower command here
   - **b**: It is allowed, because the later layer replaces the earlier one in full
   - **c**: It asks, because the two rules cancel each other out in the end
   - **d**: It is refused, because rules of that kind are checked first and win

3. A developer wants to use a cheaper model on their own machine without changing it for the team. Where does the setting go?
   - **a**: The shared project file, which is committed with the code of the team
   - **b**: The local project file, which is kept out of version control
   - **c**: The managed settings, which override every other layer of the settings
   - **d**: The memory file, which names the model in one plain sentence of text

<details>
<summary>Answer key</summary>

1. **b**. The page says "Claude treats them as context, not enforced configuration", and "To block an action regardless of what Claude decides, use a PreToolUse hook instead", and a deny rule is checked before anything runs. *a* is ruled out because memory files are "context, not enforced configuration". *c* is ruled out because a read of a memory file only asks, while deny rules are "evaluated in order: deny, then ask, then allow". *d* is ruled out because "A key set at a higher level overrides the same key set lower down" is about the same key, and since "they combine across layers", a project deny still applies.
2. **d**. The page says "Rules are evaluated in order: deny, then ask, then allow. The first match in that order determines the outcome, and rule specificity doesn't change the order." *a* is ruled out because "rule specificity doesn't change the order", so a narrower allow does not win. *c* is ruled out because the first match decides and the rules never cancel: "An allow rule can't carve an exception out of a deny rule". *b* is ruled out because "they combine across layers", so the later layer does not replace the earlier one.
3. **b**. The page puts "A personal override" in the local file, which "belongs in `.gitignore`". *a* is ruled out because "The team's choices go in the shared project file, which is committed", so the choice would reach the whole team. *c* is ruled out because managed settings are for "A rule that must hold regardless of what any developer writes", and are not a personal file. *d* is ruled out because the memory file "asks" and does not set a model: "A memory file asks and a hook enforces."

</details>
