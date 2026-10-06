"""Claude Code settings layers, permission rules and memory files, resolved offline.

Three small functions that follow what the Claude Code documentation says (read on 2026-10-03): settings merge from five levels with
the highest level winning a scalar key and lists combining; permission rules are checked deny, then ask, then allow, with the first
match deciding; and CLAUDE.md files are concatenated from the broadest scope to the most specific, with @imports expanded. It is a
teaching model of the documented behaviour, not the product's code: Read and Edit patterns use a reduced form of the gitignore rules.
"""
import logging
import re

log = logging.getLogger(__name__)

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
