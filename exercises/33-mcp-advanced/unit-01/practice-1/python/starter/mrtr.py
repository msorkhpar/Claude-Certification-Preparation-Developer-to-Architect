"""An MCP server's side of multi round-trip requests, with no state kept between calls. See ../../statement.md."""
import base64
import hashlib
import hmac
import json

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


def list_tools(request):
    # TODO: the tools sorted by name, as a complete result with ttlMs and cacheScope; a request of another protocol version is a protocol error.
    return None


def call_tool(request, secret, principal, now):
    # TODO: run the tool; when the server needs the client's input, return an input_required result with a signed requestState. No state is kept here.
    return None
