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
    return {"resultType": "complete", "tools": sorted(TOOLS, key=lambda t: t["name"]), "ttlMs": 300000, "cacheScope": "public"}


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
    return {"v": 1, "tool": name, "digest": args_digest(arguments), "sub": principal, "exp": now + TTL_SECONDS, "step": step}


def _validate(name, arguments):
    if name not in ("deploy", "status"):
        return _error(-32602, f"Unknown tool: {name}")
    service = arguments.get("service")
    if not isinstance(service, str) or not service.strip():
        return _error(-32602, "Invalid params: service is required")
    if name == "deploy" and arguments.get("env") not in ("staging", "production"):
        return _error(-32602, "Invalid params: env must be staging or production")
    return None


def _can_elicit(caps):
    elicitation = caps.get("elicitation")
    return elicitation is not None and (elicitation == {} or "form" in elicitation)


def _state_error(secret, token, principal, name, arguments, now):
    state = read_state(secret, token)
    if state is None:
        return _error(-32602, "Invalid requestState")
    if now > state.get("exp", 0):
        return _error(-32602, "Expired requestState")
    if state.get("sub") != principal or state.get("tool") != name or state.get("digest") != args_digest(arguments):
        return _error(-32602, "requestState does not match this request")
    return None


def _confirm_usable(answer):
    return isinstance(answer, dict) and answer.get("action") in ("accept", "decline", "cancel")


def _confirmed(answer):
    return answer["action"] == "accept" and (answer.get("content") or {}).get("confirm") is True


def _notes_text(answers):
    notes = answers.get("notes")
    content = notes.get("content") if isinstance(notes, dict) else None
    return content["text"] if isinstance(content, dict) and isinstance(content.get("text"), str) else None


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
