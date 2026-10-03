"""Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md."""
import re

from claude_agent_sdk import AgentDefinition, ClaudeAgentOptions, query

READ_ONLY = ["Read", "Grep", "Glob"]
SPAWN_TOOLS = ("Agent", "Task")  # current and old name of the tool that starts a subagent


def build_options(agents, cwd, cli_path=None, max_budget_usd=2.0, max_turns=20, max_concurrent=5):
    definitions = {}
    for name, spec in agents.items():
        if not re.fullmatch(r"[a-z][a-z0-9-]*", name):
            raise ValueError(f"bad agent name: {name}")
        if not str(spec.get("description", "")).strip() or not str(spec.get("prompt", "")).strip():
            raise ValueError(f"agent {name} needs a description and a prompt")
        listed = spec.get("tools")
        tools = list(READ_ONLY) if listed is None else [t for t in listed if t != "Agent"]  # read-only unless told otherwise, and no spawning from below
        definitions[name] = AgentDefinition(description=spec["description"], prompt=spec["prompt"], tools=tools, model=spec.get("model") or "inherit")
    extra = {"cli_path": cli_path} if cli_path else {}
    return ClaudeAgentOptions(
        agents=definitions, allowed_tools=["Agent", *READ_ONLY], max_turns=max_turns, max_budget_usd=max_budget_usd, cwd=cwd,
        env={"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", "CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS": str(max_concurrent)},
        permission_mode="default", setting_sources=[], **extra)


def _blocks(message):
    content = getattr(message, "content", None)
    return content if isinstance(content, list) else []


def spawned(messages):
    found = []
    for message in messages:
        for block in _blocks(message):
            if getattr(block, "name", None) in SPAWN_TOOLS and hasattr(block, "input"):
                call = block.input or {}
                found.append({"id": block.id, "subagent_type": call.get("subagent_type") or "general-purpose", "description": call.get("description", ""), "prompt": call.get("prompt", "")})
    return found


def by_subagent(messages):
    groups = {call["id"]: {"subagent_type": call["subagent_type"], "messages": 0, "tools": []} for call in spawned(messages)}
    for message in messages:
        parent = getattr(message, "parent_tool_use_id", None) or next(iter(groups), None)
        if parent in groups:
            groups[parent]["messages"] += 1
            for block in _blocks(message):
                name = getattr(block, "name", None)
                if name and name not in groups[parent]["tools"]:
                    groups[parent]["tools"].append(name)
    return groups


def make_brief(task, files=None, facts=None, output=None):
    if not str(task or "").strip():
        raise ValueError("a brief needs a task")
    lines = [f"Task: {task.strip()}"]
    for title, items in (("Files", files), ("Known", facts)):
        kept = [str(item).strip() for item in items or [] if str(item).strip()]
        if kept:
            lines.append(f"{title}:")
            lines.extend(f"- {item}" for item in kept)
    if output and output.strip():
        lines.append(f"Return: {output.strip()}")
    return "\n".join(lines)


def package_finding(claim, url=None, title=None, page=None):
    source = {k: v for k, v in (("url", url), ("title", title), ("page", page)) if v is not None}
    return {"claim": claim.strip(), "source": source or None}


def merge_findings(findings):
    merged = {}
    for finding in findings:
        key = " ".join(finding["claim"].lower().split())
        entry = merged.setdefault(key, {"claim": finding["claim"], "sources": []})
        if finding["source"] is not None and finding["source"] not in entry["sources"]:
            entry["sources"].append(finding["source"])
    return [{"claim": e["claim"], "sources": e["sources"], "attributed": bool(e["sources"])} for e in merged.values()]


async def run_team(prompt, options):
    """Collect every message of a single-shot run. The SDK raises after an error result: what was received is kept."""
    messages, error = [], None
    try:
        async for message in query(prompt=prompt, options=options):
            messages.append(message)
    except Exception as exc:  # the error result, if there was one, is already in `messages`
        error = str(exc)
    return {"messages": messages, "error": error}
