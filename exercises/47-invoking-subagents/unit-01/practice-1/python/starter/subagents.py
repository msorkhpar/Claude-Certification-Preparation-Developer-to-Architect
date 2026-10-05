"""Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md."""
import logging
import re

from claude_agent_sdk import AgentDefinition, ClaudeAgentOptions, query

log = logging.getLogger(__name__)

READ_ONLY = ["Read", "Grep", "Glob"]
SPAWN_TOOLS = ("Agent", "Task")  # current and old name of the tool that starts a subagent


def _valid_name(name):
    """TODO 1 of 9 (unlocks e2): is this a legal agent name?

    Receives a name. Returns True for lower-case letters, digits and hyphens that start with a letter, False otherwise.
    Example: _valid_name("ok-name-2") -> True, _valid_name("under_score") -> False
    """
    return True


def _complete(spec):
    """TODO 2 of 9 (unlocks e2): does the spec have a description and a prompt?

    Receives a spec dict. Returns True only when both "description" and "prompt" are present and not blank.
    Example: _complete({"prompt": "p"}) -> False, _complete({"description": "d", "prompt": "p"}) -> True
    """
    return True


def _agent_tools(listed):
    """TODO 3 of 9 (unlocks e1): the tool list of one subagent.

    Receives the spec's "tools" (a list, or None when it gave none). Returns READ_ONLY (a copy) for None, else the list without "Agent"
    (an empty list stays empty).
    Example: _agent_tools(["Read", "Agent", "Bash"]) -> ["Read", "Bash"], _agent_tools(None) -> ["Read", "Grep", "Glob"]
    """
    return []


def _limit_env(max_concurrent):
    """TODO 4 of 9 (unlocks e3): the environment that bounds the team.

    Receives the number of subagents allowed at once. Returns a dict with CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH as "1" and
    CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS as the number written as a string.
    Example: _limit_env(3) -> {"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", "CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS": "3"}
    """
    return {}


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
    """TODO 5 of 9 (unlocks e4): does this content block start a subagent?

    Receives a block. Returns True for a tool call (it has an `input`) whose name is in SPAWN_TOOLS, False for anything else.
    Example: a ToolUseBlock named "Task" -> True, a ToolUseBlock named "Read" -> False, a TextBlock -> False
    """
    return False


def spawned(messages):
    found = []
    for message in messages:
        for block in _blocks(message):
            if _is_spawn(block):
                call = block.input or {}
                found.append({"id": block.id, "subagent_type": call.get("subagent_type") or "general-purpose", "description": call.get("description", ""), "prompt": call.get("prompt", "")})
    return found


def _note_tools(group, blocks):
    """TODO 6 of 9 (unlocks e5): record the tools used in one inner message.

    Receives a group dict (with a "tools" list) and the blocks of one message. Appends the name of each block that has one, once, in order.
    Example: blocks named Read, Read, Grep added to a group whose tools are [] -> ["Read", "Grep"]
    """
    return None


def by_subagent(messages):
    groups = {call["id"]: {"subagent_type": call["subagent_type"], "messages": 0, "tools": []} for call in spawned(messages)}
    for message in messages:
        parent = getattr(message, "parent_tool_use_id", None)
        if parent in groups:
            groups[parent]["messages"] += 1
            _note_tools(groups[parent], _blocks(message))
    return groups


def _section(title, items):
    """TODO 7 of 9 (unlocks e6): the lines of one section of a brief.

    Receives a title and a list of items (or None). Returns [] when no item is left after stripping and dropping blanks, else
    `Title:` followed by one `- item` line per item.
    Example: _section("Known", ["a", " "]) -> ["Known:", "- a"], _section("Files", None) -> []
    """
    return []


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
    """TODO 8 of 9 (unlocks e7): the source of a finding.

    Receives url, title and page (each may be None). Returns a dict with only the parts that were given, keys in that order.
    Example: _source("https://example.com/b", None, None) -> {"url": "https://example.com/b"}, _source(None, None, None) -> {}
    """
    return {}


def package_finding(claim, url=None, title=None, page=None):
    source = _source(url, title, page)
    return {"claim": claim.strip(), "source": source or None}


def _add_source(entry, source):
    """TODO 9 of 9 (unlocks e7): keep every different source of a merged claim.

    Receives the merged entry (with a "sources" list) and one source (a dict or None). Appends it unless it is None or already there.
    Example: sources [A], add B -> [A, B]; add A again -> [A, B]
    """
    return None


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
