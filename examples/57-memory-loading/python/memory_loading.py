"""Which instruction files are in Claude Code's context, and when: the launch set, the files that load on demand, path-scoped rules, imports and AGENTS.md.

The model follows the memory documentation read on 2026-10-03 (Claude Code v2.1.286): files in the directories above the working directory load at
launch, root first; files below it load when Claude reads there; a rule with `paths` loads when a matching file is read, written or edited; an
import expands at launch to at most four hops; AGENTS.md is read only when no CLAUDE.md file exists in the working directory or above it.
Nothing here starts Claude Code: the "project" is a list of file paths and a dict of file texts.
"""
import re

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
