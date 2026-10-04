"""Check that the pieces of a developer-productivity setup agree with each other: the servers of the project file, the tools of each subagent, the permission rules and the credentials.

The projects are two small folders of plain files, project-before and project-after, beside this example. The checks are this course's own checklist, built on the
documented behaviour (checked 2026-10-04): the project's .mcp.json holds the servers and expands ${VAR} and ${VAR:-default} from the environment; an MCP tool is named
mcp__<server>__<tool> in permission rules and in a subagent's tools field; a subagent that omits tools inherits every tool available to subagents; project subagents live in
.claude/agents/. Nothing here starts Claude Code or an MCP server.
"""
import json
import re
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
SECRET_KEY = re.compile(r"token|key|secret|authorization", re.I)


def agents(root):
    found = {}
    for path in sorted((root / ".claude/agents").glob("*.md")):
        head = re.match(r"---\n(.*?)\n---\n", path.read_text(), re.S).group(1)
        tools = re.search(r"^tools:\s*(.*)$", head, re.M)
        found[re.search(r"^name:\s*(\S+)", head, re.M).group(1)] = [t.strip() for t in tools.group(1).split(",")] if tools else None
    return found


def load(root):
    return json.loads((root / ".mcp.json").read_text())["mcpServers"], agents(root), json.loads((root / ".claude/settings.json").read_text()).get("permissions", {})


def literal_secrets(servers):
    out = []
    for name, server in servers.items():
        for field in ("headers", "env"):
            for key, value in server.get(field, {}).items():
                if SECRET_KEY.search(key) and "${" not in value:
                    out.append(f"{name} {field}.{key}")
    return out


def audit(root):
    servers, subagents, perms = load(root)
    found = [f"literal-secret: {s}" for s in literal_secrets(servers)]
    refs = [(agent, t) for agent, tools in subagents.items() for t in tools or [] if t.startswith("mcp__")]
    refs += [("settings", r) for kind in ("allow", "deny") for r in perms.get(kind, []) if r.startswith("mcp__")]
    for who, ref in refs:
        server = ref.split("__")[1]
        if server not in servers:
            found.append(f"unknown-server: {server} ({who})")
    for name, tools in subagents.items():
        if tools is None:
            found.append(f"agent-inherits-all: {name}")
        elif "Bash" in tools:
            found.append(f"agent-bare-bash: {name}")
    if "Read(./.env)" not in perms.get("deny", []):
        found.append("env-readable")
    if any(r in ("Edit", "Write") for r in perms.get("allow", [])):
        found.append("bare-write-allowed")
    return found


def main():
    for name in ("project-before", "project-after"):
        servers, subagents, perms = load(HERE / name)
        rules = sum(len(perms.get(k, [])) for k in ("allow", "deny"))
        print(f"{name}: {len(servers)} servers, {len(subagents)} agents, {rules} permission rules")
        found = audit(HERE / name)
        for finding in found:
            print(f"  finding: {finding}")
        if not found:
            print("  no findings")
            for agent, tools in subagents.items():
                print(f"  {agent}: {', '.join(tools)}")


if __name__ == "__main__":
    main()
