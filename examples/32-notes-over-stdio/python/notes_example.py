"""One file, two roles: run with --serve it is an MCP server over stdio; run alone it starts itself as a server and talks to it twice.

First through the SDK client (what a host application does), then by hand with raw JSON-RPC lines (what the SDK hides). Everything is
local: the server is a child process and the two sides talk through pipes. `mcp` 2.2.0, checked on 2026-10-03.
"""
import asyncio
import json
import logging
import os
import subprocess
import sys

from mcp import Client
from mcp.client.stdio import StdioServerParameters
from mcp.server.mcpserver import MCPServer
from mcp.server.mcpserver.exceptions import ToolError
from mcp.types import ToolAnnotations

# logging writes to stderr; print() would write to stdout and corrupt the protocol
log = logging.getLogger(__name__)


def build_server():
    server, notes = MCPServer("notes", version="1.0.0"), []

    @server.tool(description="Save a note.", annotations=ToolAnnotations(read_only_hint=False))
    def add_note(title: str, text: str) -> str:
        if not title.strip():
            raise ToolError("title is required")  # an expected failure: the model reads the message and can retry
        notes.append((title.strip(), text))
        log.info("saved note %d", len(notes))
        return f"Saved note {len(notes)}: {title.strip()}"

    @server.tool(description="Find notes by a word in the title.", annotations=ToolAnnotations(read_only_hint=True))
    def search_notes(query: str, limit: int = 5) -> str:
        hits = [f"{i}. {t}" for i, (t, _) in enumerate(notes, 1) if query.lower() in t.lower()]
        return "\n".join(hits[:limit]) or f'No notes match "{query}"'

    @server.resource("notes://count")
    def count() -> str:
        return f"{len(notes)} note" + ("" if len(notes) == 1 else "s")

    @server.prompt()
    def review_notes(tone: str = "brief") -> str:
        return f"Review these notes in a {tone} tone:\n" + "\n".join(f"- {t}" for t, _ in notes)

    return server


def words(result):
    return "".join(getattr(c, "text", "") for c in result.content)


async def with_sdk_client(command):
    async with Client(StdioServerParameters(command=command[0], args=command[1:], env=dict(os.environ))) as client:
        info = client.server_info
        print(f"server: {info.name} {info.version}")
        caps = client.server_capabilities
        print("declares:", ", ".join(name for name in ("tools", "resources", "prompts") if getattr(caps, name) is not None))
        for tool in (await client.list_tools()).tools:
            props = tool.input_schema["properties"]
            args = ", ".join(p if p in tool.input_schema["required"] else f"[{p}]" for p in props)
            print(f"tool: {tool.name}({args}) read-only hint {tool.annotations.read_only_hint}")
        ok = await client.call_tool("add_note", {"title": "Plan", "text": "ship it"})
        print(f"add_note -> {words(ok)!r}, isError {ok.is_error}")
        bad = await client.call_tool("add_note", {"title": " ", "text": "x"})
        print(f"add_note with a blank title -> isError {bad.is_error}, says why: {'title is required' in words(bad)}")
        print("search_notes ->", repr(words(await client.call_tool("search_notes", {"query": "PLAN"}))))
        print("resource notes://count ->", (await client.read_resource("notes://count")).contents[0].text)
        message = (await client.get_prompt("review_notes", {})).messages[0]
        print(f"prompt review_notes -> role {message.role}, {message.content.text!r}")


def wire(command):
    """The same server, spoken to line by line: one JSON-RPC message per line on stdin and stdout, nothing else on stdout."""
    child = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, env={**os.environ, "NOTES_LOG": "INFO"})
    stray = 0

    def send(message):
        child.stdin.write(json.dumps(message) + "\n")
        child.stdin.flush()

    def receive(expected_id):
        nonlocal stray
        while True:
            line = child.stdout.readline()
            try:
                message = json.loads(line)
            except ValueError:
                stray += 1
                continue
            if message.get("id") == expected_id:
                return message

    send({"jsonrpc": "2.0", "id": 1, "method": "initialize", "params": {"protocolVersion": "2025-11-25", "capabilities": {}, "clientInfo": {"name": "wire", "version": "1"}}})
    first = receive(1)["result"]
    print(f"-> initialize (id 1)      <- protocol {first['protocolVersion']}, server {first['serverInfo']['name']}, capabilities {sorted(k for k in first['capabilities'] if k in ('tools', 'resources', 'prompts'))}")
    send({"jsonrpc": "2.0", "method": "notifications/initialized"})
    send({"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
    print("-> tools/list (id 2)      <- tools", sorted(t["name"] for t in receive(2)["result"]["tools"]))
    send({"jsonrpc": "2.0", "id": 3, "method": "tools/call", "params": {"name": "add_note", "arguments": {"title": "Wire", "text": "by hand"}}})
    print("-> tools/call (id 3)      <- ", receive(3)["result"]["content"][0]["text"])
    send({"jsonrpc": "2.0", "id": 4, "method": "tools/call", "params": {"name": "search_notes", "arguments": {"query": "wire", "limit": "two"}}})
    answer = receive(4)
    print("-> tools/call with limit 'two' (id 4)   <-", "a result with isError" if answer.get("result", {}).get("isError") else "a protocol error" if "error" in answer else "a result")
    child.stdin.close()
    logs = child.stderr.read()
    child.wait(timeout=10)
    print("lines on stdout that were not JSON:", stray)
    print("the server's own log went to stderr:", "saved note 1" in logs)


def main():
    command = [sys.executable, __file__, "--serve"]
    asyncio.run(with_sdk_client(command))
    print()
    wire(command)


if __name__ == "__main__":
    if "--serve" in sys.argv:
        logging.basicConfig(level=os.environ.get("NOTES_LOG", "WARNING"))
        build_server().run()
    else:
        main()
