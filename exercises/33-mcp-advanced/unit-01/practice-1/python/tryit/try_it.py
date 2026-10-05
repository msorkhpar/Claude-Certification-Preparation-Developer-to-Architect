"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from mrtr import call_tool

SECRET, NOW = "s3cret", 1000
META = {"io.modelcontextprotocol/protocolVersion": "2026-07-28",
        "io.modelcontextprotocol/clientCapabilities": {"elicitation": {}, "sampling": {}}}


def request(**extra):
    return {"name": "deploy", "arguments": {"service": "api", "env": "production"}, "_meta": META, **extra}


# Round trip 1: the server needs a person's confirmation, so it ends the call with input_required and a signed state.
first = call_tool(request(), SECRET, "alice", NOW) or {}
print("first call:", first.get("resultType"), "| asks for:", list((first.get("inputRequests") or {})))
print("state is a string:", isinstance(first.get("requestState"), str))

# Round trip 2: the client retries the same call with the answer and echoes the state back.
answer = {"confirm": {"action": "accept", "content": {"confirm": True}}}
second = call_tool(request(inputResponses=answer, requestState=first.get("requestState")), SECRET, "alice", NOW + 10) or {}
print("second call:", second.get("resultType"), "| asks for:", list((second.get("inputRequests") or {})))
