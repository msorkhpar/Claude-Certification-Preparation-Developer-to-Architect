import json

from claude_agent_sdk import AssistantMessage, ResultMessage, TextBlock, ToolUseBlock, UserMessage, ToolResultBlock

from subagent_run import AGENTS, BRIEF, describe


def test_a_spawn_is_described_with_the_length_of_its_brief():
    message = AssistantMessage(content=[ToolUseBlock(id="t1", name="Agent", input={"subagent_type": "reviewer", "description": "d", "prompt": BRIEF})], model="m")
    assert describe(message) == ["spawn: Agent -> reviewer, brief of 6 lines"]


def test_a_message_from_inside_a_subagent_is_marked_with_its_parent():
    message = AssistantMessage(content=[ToolUseBlock(id="r", name="Read", input={"file_path": "a"})], model="m", parent_tool_use_id="t1")
    assert describe(message) == ['  inside t1: Read {"file_path":"a"}']
    outer = AssistantMessage(content=[ToolUseBlock(id="r", name="Read", input={"file_path": "a"})], model="m")
    assert describe(outer) == ["coordinator calls Read"]


def test_only_the_report_of_the_subagent_itself_reaches_the_coordinator_lines():
    inner = UserMessage(content=[ToolResultBlock(tool_use_id="r", content="code")], parent_tool_use_id="t1")
    report = UserMessage(content=[ToolResultBlock(tool_use_id="t1", content="one finding")])
    assert describe(inner) == [] and describe(report) == ["report to the coordinator: one finding"]


def test_the_reviewer_is_limited_to_two_read_tools():
    assert AGENTS["reviewer"].tools == ["Read", "Grep"] and "Agent" not in AGENTS["finder"].tools
