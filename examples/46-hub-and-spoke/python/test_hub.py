import json

from harness import scripted_client
from harness.scripted import message, text, tool_use
from hub import MODEL, REPORTS, SUBAGENT_SYSTEM, SUBTASKS, plan, run_subagent, synthesize


def test_the_plan_is_read_from_the_forced_tool_call():
    client, transport = scripted_client(message([tool_use("t", "plan", subtasks=SUBTASKS)], stop_reason="tool_use", model=MODEL))
    assert plan(client, "q") == SUBTASKS
    assert transport.requests[0]["tool_choice"] == {"type": "tool", "name": "plan"}


def test_a_subagent_request_holds_its_brief_and_the_role_prompt_only():
    client, transport = scripted_client(message([text(REPORTS[0])], model=MODEL))
    run_subagent(client, SUBTASKS[0]["brief"])
    request = transport.requests[0]
    assert request["system"] == SUBAGENT_SYSTEM and request["messages"] == [{"role": "user", "content": SUBTASKS[0]["brief"]}]
    assert "tools" not in request


def test_the_synthesis_request_holds_every_finding():
    client, transport = scripted_client(message([text("done")], model=MODEL))
    synthesize(client, "q", [("chips", REPORTS[0]), ("cars", REPORTS[1])])
    body = json.dumps(transport.requests[0])
    assert "CHIPS-REPORT" in body and "CARS-REPORT" in body and "RATES-REPORT" not in body
