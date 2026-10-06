import copy
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from mrtr import args_digest, call_tool, list_tools, mint_state, read_state

SECRET = "s3cret"
VERSION = "2026-07-28"
BOTH = {"elicitation": {}, "sampling": {}}
ARGS = {"service": "api", "env": "production"}
CONFIRM = {"confirm": {"method": "elicitation/create", "params": {"mode": "form", "message": "Deploy api to production?", "requestedSchema": {
    "type": "object", "properties": {"confirm": {"type": "boolean", "title": "Confirm the deployment"}}, "required": ["confirm"]}}}}
NOTES = {"notes": {"method": "sampling/createMessage", "params": {"messages": [{"role": "user", "content": {"type": "text", "text": "Write one sentence of release notes for api."}}], "maxTokens": 100}}}
YES = {"confirm": {"action": "accept", "content": {"confirm": True}}}


def request(name="deploy", arguments=None, caps=None, version=VERSION, **extra):
    meta = {} if version is None else {"io.modelcontextprotocol/protocolVersion": version}
    meta["io.modelcontextprotocol/clientCapabilities"] = BOTH if caps is None else caps
    return {"name": name, "arguments": ARGS if arguments is None else arguments, "_meta": meta, **extra}


def call(req, now=1000, principal="alice", secret=SECRET):
    return call_tool(req, secret, principal, now) or {}


def retry(first, responses, **kw):
    return request(inputResponses=responses, requestState=first.get("requestState"), **kw)


def complete(text, is_error=False):
    return {"resultType": "complete", "content": [{"type": "text", "text": text}], "isError": is_error}


def error_of(result):
    return (result.get("error", {}).get("code"), result.get("error", {}).get("message"))


def test_m1_a_production_deploy_takes_three_round_trips_and_keeps_no_state():
    first = call(request())
    assert first.get("resultType") == "input_required" and first.get("inputRequests") == CONFIRM and isinstance(first.get("requestState"), str) and "content" not in first
    second = call(retry(first, YES), now=1010)
    assert second.get("resultType") == "input_required" and second.get("inputRequests") == NOTES
    assert isinstance(second.get("requestState"), str) and second["requestState"] != first["requestState"]
    notes = {"notes": {"role": "assistant", "content": {"type": "text", "text": "Faster checkout."}, "model": "m", "stopReason": "endTurn"}}
    assert call(retry(second, notes), now=1020) == complete("Deployed api to production. Release notes: Faster checkout.")


def test_e1_a_staging_deploy_and_a_status_call_finish_at_once_and_the_tool_list_is_cacheable():
    assert call(request(arguments={"service": "api", "env": "staging"}, caps={})) == complete("Deployed api to staging")
    assert call(request("status", {"service": "api"}, caps={})) == complete("api: running")
    listing = list_tools({"_meta": {"io.modelcontextprotocol/protocolVersion": VERSION}}) or {}
    assert (listing.get("resultType"), listing.get("ttlMs"), listing.get("cacheScope")) == ("complete", 300000, "public")
    assert [t["name"] for t in listing.get("tools", [])] == ["deploy", "status"]
    assert listing["tools"][0]["inputSchema"]["required"] == ["service", "env"] and listing["tools"][1]["inputSchema"]["required"] == ["service"]


def test_e2_the_server_only_asks_what_the_client_declared_it_can_answer():
    refusal = "Deploying to production needs confirmation, and this client cannot be asked."
    assert call(request(caps={})) == complete(refusal, True)
    assert call(request(caps={"sampling": {}})) == complete(refusal, True)
    assert call(request(caps={"elicitation": {"url": {}}})) == complete(refusal, True)
    form_only = {"elicitation": {"form": {}}}
    first = call(request(caps=form_only))
    assert first.get("inputRequests") == CONFIRM
    assert call(retry(first, YES, caps=form_only), now=1010) == complete("Deployed api to production")


def test_e3_a_no_or_a_missing_answer_is_handled_without_an_error():
    first = call(request())
    for answer in ({"action": "decline"}, {"action": "cancel"}, {"action": "accept", "content": {"confirm": False}}, {"action": "accept"},
                   {"action": "decline", "content": {"confirm": True}}):
        assert call(retry(first, {"confirm": answer}), now=1010) == complete("Deployment cancelled")
    for responses in ({}, {"other": 1}, {"confirm": {"action": "maybe"}}, {"confirm": "yes"}):
        again = call(retry(first, responses), now=1100)
        assert again.get("resultType") == "input_required" and again.get("inputRequests") == CONFIRM
        assert again.get("requestState") not in (None, first["requestState"])
    second = call(retry(first, YES), now=1010)
    assert call(retry(second, {}), now=1020).get("inputRequests") == NOTES
    assert call(retry(second, {"notes": {"role": "assistant", "content": {"type": "image"}}}), now=1020).get("inputRequests") == NOTES


def test_e4_a_state_the_server_did_not_sign_is_refused():
    first = call(request())
    token = first.get("requestState") or "x.y"
    flipped = token[:-1] + ("A" if token[-1] != "A" else "B")
    foreign = (call(request(), secret="another secret") or {}).get("requestState")
    for bad in (flipped, "abc", "", foreign):
        result = call(request(inputResponses=YES, requestState=bad), now=1010)
        assert error_of(result) == (-32602, "Invalid requestState"), bad


def test_e5_a_state_works_only_for_the_same_user_the_same_call_and_before_it_expires():
    def state(**over):
        payload = {"v": 1, "tool": "deploy", "digest": args_digest(ARGS), "sub": "alice", "exp": 999, "step": "confirm", **over}
        return mint_state(SECRET, payload)

    def go(token, now=999, principal="alice", arguments=None):
        return call(request(arguments=arguments, inputResponses=YES, requestState=token), now=now, principal=principal)

    assert go(state()).get("inputRequests") == NOTES
    assert error_of(go(state(), now=1000)) == (-32602, "Expired requestState")
    assert error_of(go(state(), principal="bob")) == (-32602, "requestState does not match this request")
    assert error_of(go(state(), arguments={"service": "billing", "env": "production"})) == (-32602, "requestState does not match this request")
    assert error_of(go(state(tool="status"))) == (-32602, "requestState does not match this request")
    assert go(state(step="notes")).get("inputRequests") == NOTES


def test_e6_a_request_the_server_cannot_serve_is_a_protocol_error_with_a_code():
    old = call(request(version="2025-11-25"))
    assert error_of(old) == (-32022, "Unsupported protocol version") and old["error"].get("data") == {"supported": [VERSION]}
    assert error_of(call(request(version=None)))[0] == -32022
    assert error_of(call(request("rollback"))) == (-32602, "Unknown tool: rollback")
    assert error_of(call(request(arguments={"env": "staging"}))) == (-32602, "Invalid params: service is required")
    assert error_of(call(request(arguments={"service": "  ", "env": "staging"}))) == (-32602, "Invalid params: service is required")
    assert error_of(call(request(arguments={"service": "api", "env": "dev"}))) == (-32602, "Invalid params: env must be staging or production")
    assert error_of(call(request("status", {})))[1] == "Invalid params: service is required"
    assert error_of(list_tools({"_meta": {"io.modelcontextprotocol/protocolVersion": "2024-11-05"}}) or {})[0] == -32022


def test_e7_the_state_carries_the_whole_context_and_an_answer_alone_never_skips_a_step():
    first = call(request(), now=1000)
    again = call(request(), now=1000)
    assert first.get("inputRequests") == again.get("inputRequests")
    payload = read_state(SECRET, first.get("requestState") or "") or {}
    assert payload == {"v": 1, "tool": "deploy", "digest": args_digest(ARGS), "sub": "alice", "exp": 1300, "step": "confirm"}
    assert args_digest({"env": "production", "service": "api"}) == args_digest(ARGS)
    alone = call(request(inputResponses=YES))
    assert alone.get("resultType") == "input_required" and alone.get("inputRequests") == CONFIRM
    noisy = dict(YES, junk={"action": "accept"})
    assert call(retry(first, noisy), now=1010).get("inputRequests") == NOTES
    second = call(retry(first, YES), now=1010)
    assert (read_state(SECRET, second.get("requestState") or "") or {}).get("step") == "notes"
    before = copy.deepcopy(first)
    call(retry(first, YES), now=1010)
    assert first == before
