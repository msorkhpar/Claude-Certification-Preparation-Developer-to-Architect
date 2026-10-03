"""Invoking subagents with the Agent SDK: definitions, limits, detecting a spawn, grouping inner messages, briefs and findings. See ../../statement.md."""
from claude_agent_sdk import AgentDefinition, ClaudeAgentOptions, query


def build_options(agents, cwd, cli_path=None, max_budget_usd=2.0, max_turns=20, max_concurrent=5):
    # TODO: turn the specs into AgentDefinitions and set the tools, limits and environment on the options.
    return None


def spawned(messages):
    # TODO: list the calls of the tool that starts a subagent.
    return None


def by_subagent(messages):
    # TODO: group the messages that ran inside a subagent under the call that started it.
    return None


def make_brief(task, files=None, facts=None, output=None):
    # TODO: write the prompt a subagent needs, with every fact it cannot get any other way.
    return None


def package_finding(claim, url=None, title=None, page=None):
    # TODO: keep the claim apart from its source.
    return None


def merge_findings(findings):
    # TODO: merge repeated claims and keep every source.
    return None


async def run_team(prompt, options):
    # TODO: collect every message of a single-shot run, and keep what was received when the SDK raises after an error result.
    return None
