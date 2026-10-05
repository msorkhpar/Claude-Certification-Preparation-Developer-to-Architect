"""An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md."""
import base64
import hashlib
import hmac
import json
import logging

log = logging.getLogger(__name__)

VERSION = "2026-07-28"
META_VERSION = "io.modelcontextprotocol/protocolVersion"
META_CAPS = "io.modelcontextprotocol/clientCapabilities"
TTL_SECONDS = 300

TOOLS = [
    {"name": "deploy", "description": "Deploy a service to an environment. Production needs a person's confirmation.",
     "inputSchema": {"type": "object", "properties": {"service": {"type": "string"}, "env": {"type": "string", "enum": ["staging", "production"]}}, "required": ["service", "env"]}},
    {"name": "status", "description": "Report whether a service is running.",
     "inputSchema": {"type": "object", "properties": {"service": {"type": "string"}}, "required": ["service"]}},
]


def _b64(data):
    return base64.urlsafe_b64encode(data).decode().rstrip("=")


def _unb64(text):
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def mint_state(secret, payload):
    """Given: a requestState for `payload`: its JSON, then a HMAC-SHA256 signature of it, both in base64url, joined by a dot."""
    body = _b64(json.dumps(payload, sort_keys=True, separators=(",", ":")).encode())
    return body + "." + _b64(hmac.new(secret.encode(), body.encode(), hashlib.sha256).digest())


def read_state(secret, token):
    """Given: the payload of a requestState, or None when it is not one this secret signed (tampered, truncated, foreign, garbage)."""
    try:
        body, sig = token.split(".")
        if not hmac.compare_digest(_b64(hmac.new(secret.encode(), body.encode(), hashlib.sha256).digest()), sig):
            return None
        payload = json.loads(_unb64(body))
    except Exception:  # noqa: BLE001
        return None
    return payload if isinstance(payload, dict) else None


def args_digest(arguments):
    """Given: a fingerprint of the call's arguments, the same for the same arguments in any key order."""
    return hashlib.sha256(json.dumps(arguments, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def _error(code, message, data=None):
    return {"error": {"code": code, "message": message, **({"data": data} if data else {})}}


def _meta_error(request):
    version = (request.get("_meta") or {}).get(META_VERSION)
    if version != VERSION:
        return _error(-32022, "Unsupported protocol version", {"supported": [VERSION]})
    return None


def _tool_listing():
    """TODO 1 of 8 (unlocks e1): the result of tools/list.

    Receives nothing and returns the dict from the statement: resultType "complete", the TOOLS sorted by name, ttlMs 300000, cacheScope "public".
    Example: _tool_listing()["tools"][0]["name"] -> "deploy"
    """
    return {}


def list_tools(request):
    return _meta_error(request) or _tool_listing()


def _complete(text, is_error=False):
    return {"resultType": "complete", "content": [{"type": "text", "text": text}], "isError": is_error}


def _confirm_request(service):
    return {"confirm": {"method": "elicitation/create", "params": {"mode": "form", "message": f"Deploy {service} to production?", "requestedSchema": {
        "type": "object", "properties": {"confirm": {"type": "boolean", "title": "Confirm the deployment"}}, "required": ["confirm"]}}}}


def _notes_request(service):
    return {"notes": {"method": "sampling/createMessage", "params": {"messages": [{"role": "user", "content": {"type": "text", "text": f"Write one sentence of release notes for {service}."}}], "maxTokens": 100}}}


def _new_state(name, arguments, principal, now, step):
    """TODO 2 of 8 (unlocks e7): the payload of a requestState.

    Receives the tool name, the arguments, the user, the time in seconds and the step. Returns the dict from the statement: v, tool, digest, sub, exp, step.
    Example: _new_state("deploy", {"service": "api"}, "alice", 1000, "confirm")["exp"] -> 1300
    """
    return {}


def _validate(name, arguments):
    """TODO 3 of 8 (unlocks e6): the protocol error for a call that cannot be served, in the order of the statement.

    Receives the tool name and the arguments. Returns `_error(-32602, ...)` for an unknown tool, a missing or blank service, or (for deploy) an env
    that is not staging or production; None when the call is valid.
    Example: _validate("deploy", {"service": "api", "env": "dev"}) -> _error(-32602, "Invalid params: env must be staging or production")
    """
    return None


def _can_elicit(caps):
    """TODO 4 of 8 (unlocks e2): can the client be asked for a confirmation?

    Receives the client capabilities dict. Returns True when `elicitation` is present and is empty or holds `form`; False for none or only `url`.
    Example: _can_elicit({"elicitation": {}}) -> True, _can_elicit({"elicitation": {"url": {}}}) -> False
    """
    return False


def _state_error(secret, token, principal, name, arguments, now):
    """TODO 5 of 8 (unlocks e4 and e5): the error for a requestState that cannot be used.

    Receives the secret, the token, the user, the tool name, the arguments and the time. Returns `_error(-32602, ...)` with `Invalid requestState`
    (`read_state` gives None), `Expired requestState` (now after exp) or `requestState does not match this request` (sub, tool or digest differ), checked in
    that order; None when the state is good.
    Example: a state minted for "alice" and read for "bob" -> _error(-32602, "requestState does not match this request")
    """
    return None


def _confirm_usable(answer):
    """TODO 6 of 8 (unlocks m1 and e3): is this an answer to the confirmation question?

    Receives `inputResponses.confirm` (anything, or None). Returns True when it is a dict whose `action` is accept, decline or cancel.
    Example: _confirm_usable({"action": "decline"}) -> True, _confirm_usable("yes") -> False
    """
    return False


def _confirmed(answer):
    """TODO 7 of 8 (unlocks e3): did the person accept?

    Receives a usable answer. Returns True only for action `accept` with `content.confirm` exactly True.
    Example: _confirmed({"action": "accept", "content": {"confirm": False}}) -> False
    """
    return False


def _notes_text(answers):
    """TODO 8 of 8 (unlocks m1): the release notes the client wrote.

    Receives the `inputResponses` dict. Returns `answers["notes"]["content"]["text"]` when that is a string; None for anything else.
    Example: _notes_text({"notes": {"content": {"type": "text", "text": "Faster."}}}) -> "Faster."
    """
    return None


def _ask(requests, step, secret, name, arguments, principal, now):
    state = _new_state(name, arguments, principal, now, step)
    return {"resultType": "input_required", "inputRequests": requests, "requestState": mint_state(secret, state)}


def call_tool(request, secret, principal, now):
    log.debug("call_tool input: %r", request)
    bad = _meta_error(request)
    if bad:
        return bad
    name, arguments = request.get("name"), request.get("arguments") or {}
    invalid = _validate(name, arguments)
    if invalid:
        return invalid
    service = arguments["service"]
    if name == "status":
        return _complete(f"{service}: running")
    if arguments["env"] == "staging":
        return _complete(f"Deployed {service} to staging")
    caps = (request.get("_meta") or {}).get(META_CAPS) or {}
    if not _can_elicit(caps):
        return _complete("Deploying to production needs confirmation, and this client cannot be asked.", True)
    step = "confirm"
    token = request.get("requestState")
    if token is not None:
        invalid = _state_error(secret, token, principal, name, arguments, now)
        if invalid:
            return invalid
        step = (read_state(secret, token) or {}).get("step")
    answers = request.get("inputResponses") if token is not None else None
    answers = answers if isinstance(answers, dict) else {}
    if step == "confirm":
        answer = answers.get("confirm")
        if not _confirm_usable(answer):
            return _ask(_confirm_request(service), "confirm", secret, name, arguments, principal, now)
        if not _confirmed(answer):
            return _complete("Deployment cancelled")
        if "sampling" not in caps:
            return _complete(f"Deployed {service} to production")
        return _ask(_notes_request(service), "notes", secret, name, arguments, principal, now)
    text = _notes_text(answers)
    if text is None:
        return _ask(_notes_request(service), "notes", secret, name, arguments, principal, now)
    return _complete(f"Deployed {service} to production. Release notes: {text}")
