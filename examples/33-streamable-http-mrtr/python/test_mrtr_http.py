import asyncio

from mcp import Client
from mcp.shared.exceptions import MCPError
from mcp_types import CreateMessageResult, ElicitResult, TextContent

from mrtr_http import build_server


def run(steps, **callbacks):
    async def go():
        async with Client(build_server(), **callbacks) as client:
            return await steps(client)
    return asyncio.run(go())


def answer(action, confirm=True):
    async def person(context, params):
        return ElicitResult(action=action, content={"confirm": confirm} if action == "accept" else None)
    return person


def test_a_staging_deploy_asks_nobody():
    result = run(lambda c: c.call_tool("deploy", {"service": "api", "env": "staging"}))
    assert result.content[0].text == "Deployed api to staging"


def test_a_production_deploy_follows_the_persons_answer():
    for action, confirm, text in (("accept", True, "Deployed api to production"), ("accept", False, "Deployment cancelled"), ("decline", False, "Deployment cancelled")):
        result = run(lambda c: c.call_tool("deploy", {"service": "api", "env": "production"}), elicitation_callback=answer(action, confirm))
        assert result.content[0].text == text, (action, confirm)


def test_the_release_notes_come_from_the_clients_model():
    async def model(context, params):
        return CreateMessageResult(role="assistant", content=TextContent(type="text", text="Faster."), model="scripted", stop_reason="endTurn")
    assert run(lambda c: c.call_tool("release_notes", {"service": "api"}), sampling_callback=model).content[0].text == "api: Faster."


def test_a_client_that_cannot_be_asked_gets_a_protocol_error_not_a_question():
    async def steps(c):
        try:
            await c.call_tool("deploy", {"service": "api", "env": "production"})
        except MCPError as e:
            return e.code
    assert run(steps) == -32021
