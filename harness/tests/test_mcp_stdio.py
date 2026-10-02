"""An MCP server and client talk over stdio inside the offline container."""
import os
import sys
from pathlib import Path

import anyio
from mcp import ClientSession, StdioServerParameters, stdio_client

SERVER = str(Path(__file__).resolve().parent / "mcp_demo_server.py")


def params():
    env = {"PYTHONPATH": os.environ["PYTHONPATH"]} if "PYTHONPATH" in os.environ else None
    return StdioServerParameters(command=sys.executable, args=[SERVER], env=env)


def test_list_and_call_tool_and_read_resource_over_stdio():
    async def go():
        async with stdio_client(params()) as (read, write):
            async with ClientSession(read, write) as session:
                await session.initialize()
                tools = await session.list_tools()
                assert [t.name for t in tools.tools] == ["add"]
                assert tools.tools[0].input_schema["required"] == ["a", "b"]
                result = await session.call_tool("add", {"a": 1, "b": 2})
                assert result.is_error is False
                assert result.content[0].text == "3"
                res = await session.read_resource("greeting://Ada")
                assert res.contents[0].text == "Hello, Ada!"
    anyio.run(go)
