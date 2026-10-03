"""The built-in file tools of Claude Code, modelled offline: Edit's exact match, the way out when it cannot apply, which search tools exist
on which platform, and which permission rule covers which tool.

A teaching model of the "Tools reference" page of the Claude Code documentation (read on 2026-10-03), not the product's code. It covers six tools:
Read, Write, Edit, Bash, Grep and Glob.
"""

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
