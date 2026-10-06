"""A tool that asks for a person's confirmation and for a model completion, over Streamable HTTP on the loopback interface.

The server is `mcp` 2.2.0 and the client is the same SDK with scripted callbacks in place of a person and a model. This SDK speaks the
2026-07-28 revision: the server never calls the client in the middle of a request; it ends the call with an input-required result and
the client retries with the answers. The log under the program output is what the HTTP hook of the client saw on the wire.
Checked on 2026-10-03 against the "Multi Round-Trip Requests" page of the MCP specification.
"""
import logging
import asyncio
import json
import logging
import socket
import threading
import time
from typing import Annotated

import httpx2
import uvicorn
from mcp import Client
from mcp.client.streamable_http import streamable_http_client
from mcp.server.elicitation import ElicitationResult
from mcp.server.mcpserver import Context, MCPServer
from mcp.server.mcpserver.resolve import Elicit, Resolve, Sample
from mcp.shared.exceptions import MCPError
from mcp_types import CreateMessageResult, ElicitResult, SamplingMessage, TextContent
from pydantic import BaseModel

log = logging.getLogger(__name__)


class Confirm(BaseModel):
    confirm: bool


def build_server():
    server = MCPServer("deployer", version="1.0.0")

    def ask_confirmation(service: str, env: str) -> Elicit[Confirm] | Confirm:
        return Elicit(f"Deploy {service} to production?", Confirm) if env == "production" else Confirm(confirm=True)

    @server.tool(description="Deploy a service; production needs a person's confirmation.")
    async def deploy(service: str, env: str, answer: Annotated[ElicitationResult[Confirm], Resolve(ask_confirmation)], ctx: Context) -> str:
        if answer.action != "accept" or not answer.data.confirm:
            return "Deployment cancelled"
        return f"Deployed {service} to {env}"

    def ask_model(service: str) -> Sample:
        return Sample([SamplingMessage(role="user", content=TextContent(type="text", text=f"Write one sentence of release notes for {service}."))], max_tokens=100)

    @server.tool(description="Write release notes with the client's model.")
    async def release_notes(service: str, completion: Annotated[CreateMessageResult, Resolve(ask_model)]) -> str:
        return f"{service}: {completion.content.text}"

    return server


def serve_on_loopback(server):
    probe = socket.socket()
    probe.bind(("127.0.0.1", 0))
    port = probe.getsockname()[1]
    probe.close()
    http = uvicorn.Server(uvicorn.Config(server.streamable_http_app(json_response=True), host="127.0.0.1", port=port, log_level="error"))
    threading.Thread(target=http.run, daemon=True).start()
    while not http.started:
        time.sleep(0.02)
    return http, f"http://127.0.0.1:{port}/mcp"


def describe(body):
    """One line for a JSON-RPC message that crossed the wire."""
    if "method" in body:
        params = body.get("params") or {}
        extra = [k for k in ("inputResponses", "requestState") if k in params]
        return f"{body['method']}{' ' + params['name'] if 'name' in params else ''}{' with ' + ' and '.join(extra) if extra else ''}"
    result = body.get("result") or {}
    if "error" in body:
        return f"error {body['error']['code']}"
    if result.get("resultType") == "input_required":
        kinds = sorted(r["method"] for r in result.get("inputRequests", {}).values())
        return f"input_required: asks for {', '.join(kinds)}" + (", with a requestState" if "requestState" in result else "")
    return "complete" + (f": {result['content'][0]['text']}" if "content" in result else "")


async def main():
    logging.disable(logging.INFO)  # the SDK and the HTTP server log every request at INFO; this program prints its own log
    http, url = serve_on_loopback(build_server())
    wire = []

    async def on_response(response):
        await response.aread()
        request = json.loads(response.request.content)
        if request.get("method") == "tools/call":  # the SDK's own housekeeping requests (server/discover, tools/list) are left out of this log
            wire.append(("->", request))
            wire.append(("<-", response.json()))

    async def person(context, params):
        print(f"the person is asked: {params.message}")
        return ElicitResult(action="accept", content={"confirm": True})

    async def model(context, params):
        print(f"the client's model is asked: {params.messages[0].content.text}")
        return CreateMessageResult(role="assistant", content=TextContent(type="text", text="Checkout is faster."), model="scripted", stop_reason="endTurn")

    def transport():
        return streamable_http_client(url, http_client=httpx2.AsyncClient(event_hooks={"response": [on_response]}))

    async with Client(transport(), elicitation_callback=person, sampling_callback=model) as client:
        for name, arguments in (("deploy", {"service": "api", "env": "production"}), ("deploy", {"service": "api", "env": "staging"}), ("release_notes", {"service": "api"})):
            wire.clear()
            result = await client.call_tool(name, arguments)
            print(f"{name} {arguments['env'] if 'env' in arguments else ''}".rstrip(), "->", result.content[0].text)
            for direction, body in wire:
                print(f"   {direction} {describe(body)}")
    async with Client(transport()) as bare:  # no callbacks: this client declares no elicitation or sampling capability
        wire.clear()
        try:
            await bare.call_tool("deploy", {"service": "api", "env": "production"})
        except MCPError as e:
            print("a client that cannot be asked ->", f"error {e.code}", "(MissingRequiredClientCapability)")
    http.should_exit = True


if __name__ == "__main__":
    asyncio.run(main())
