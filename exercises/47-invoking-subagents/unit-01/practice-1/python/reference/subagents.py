"""Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md."""
import logging
import re

from claude_agent_sdk import AgentDefinition, ClaudeAgentOptions, query

log = logging.getLogger(__name__)

READ_ONLY = ["Read", "Grep", "Glob"]
SPAWN_TOOLS = ("Agent", "Task")  # current and old name of the tool that starts a subagent


def _valid_name(name):
    return re.fullmatch(r"[a-z][a-z0-9-]*", name) is not None


def _complete(spec):
    return bool(str(spec.get("description", "")).strip()) and bool(str(spec.get("prompt", "")).strip())


def _agent_tools(listed):
    if listed is None:
        return list(READ_ONLY)  # read-only unless told otherwise
    return [t for t in listed if t != "Agent"]  # and no spawning from below


def _limit_env(max_concurrent):
    return {"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", "CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS": str(max_concurrent)}


def build_options(agents, cwd, cli_path=None, max_budget_usd=2.0, max_turns=20, max_concurrent=5):
    log.debug("build_options input: %r", agents)
    definitions = {}
    for name, spec in agents.items():
        if not _valid_name(name):
            raise ValueError(f"bad agent name: {name}")
        if not _complete(spec):
            raise ValueError(f"agent {name} needs a description and a prompt")
        tools = _agent_tools(spec.get("tools"))
        definitions[name] = AgentDefinition(description=spec["description"], prompt=spec["prompt"], tools=tools, model=spec.get("model") or "inherit")
    extra = {"cli_path": cli_path} if cli_path else {}
    return ClaudeAgentOptions(
        agents=definitions, allowed_tools=["Agent", *READ_ONLY], max_turns=max_turns, max_budget_usd=max_budget_usd, cwd=cwd,
        env=_limit_env(max_concurrent),
        permission_mode="default", setting_sources=[], **extra)


def _blocks(message):
    content = getattr(message, "content", None)
    return content if isinstance(content, list) else []


def _is_spawn(block):
    return getattr(block, "name", None) in SPAWN_TOOLS and hasattr(block, "input")


def spawned(messages):
    found = []
    for message in messages:
        for block in _blocks(message):
            if _is_spawn(block):
                call = block.input or {}
                found.append({"id": block.id, "subagent_type": call.get("subagent_type") or "general-purpose", "description": call.get("description", ""), "prompt": call.get("prompt", "")})
    return found


def _note_tools(group, blocks):
    for block in blocks:
        name = getattr(block, "name", None)
        if name and name not in group["tools"]:
            group["tools"].append(name)


def by_subagent(messages):
    groups = {call["id"]: {"subagent_type": call["subagent_type"], "messages": 0, "tools": []} for call in spawned(messages)}
    for message in messages:
        parent = getattr(message, "parent_tool_use_id", None)
        if parent in groups:
            groups[parent]["messages"] += 1
            _note_tools(groups[parent], _blocks(message))
    return groups


def _section(title, items):
    kept = [str(item).strip() for item in items or [] if str(item).strip()]
    return [f"{title}:", *(f"- {item}" for item in kept)] if kept else []


def make_brief(task, files=None, facts=None, output=None):
    if not str(task or "").strip():
        raise ValueError("a brief needs a task")
    lines = [f"Task: {task.strip()}"]
    for title, items in (("Files", files), ("Known", facts)):
        lines.extend(_section(title, items))
    if output and output.strip():
        lines.append(f"Return: {output.strip()}")
    return "\n".join(lines)


def _source(url, title, page):
    return {k: v for k, v in (("url", url), ("title", title), ("page", page)) if v is not None}


def package_finding(claim, url=None, title=None, page=None):
    source = _source(url, title, page)
    return {"claim": claim.strip(), "source": source or None}


def _add_source(entry, source):
    if source is not None and source not in entry["sources"]:
        entry["sources"].append(source)


def merge_findings(findings):
    merged = {}
    for finding in findings:
        key = " ".join(finding["claim"].lower().split())
        entry = merged.setdefault(key, {"claim": finding["claim"], "sources": []})
        _add_source(entry, finding["source"])
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
