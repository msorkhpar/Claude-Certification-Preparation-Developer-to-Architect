"""Tool interfaces graded on rules: lint a tool and a tool set, page large results, and weigh a tool's annotations. See ../../statement.md."""
import base64
import re

NAME = re.compile(r"[A-Za-z0-9_-]{1,128}")
VAGUE = {"tool", "helper", "do", "run", "process", "handle", "data", "util", "utils", "query"}
READ_PREFIXES = ("list_", "search_", "find_")
WRITE_PREFIXES = ("create_", "update_", "delete_", "remove_", "send_", "write_")
DELETE_PREFIXES = ("delete_", "remove_")
OVERLAP = 0.6  # token overlap of two descriptions from which the tools count as overlapping
MAX_LIMIT = 50
DEFAULT_HINTS = {"readOnlyHint": False, "destructiveHint": True, "idempotentHint": False, "openWorldHint": True}


def _valid(value, schema):
    kind = schema.get("type")
    if kind == "string" and not isinstance(value, str):
        return False
    if kind == "integer" and (isinstance(value, bool) or not isinstance(value, int)):
        return False
    if kind == "number" and (isinstance(value, bool) or not isinstance(value, (int, float))):
        return False
    if kind == "boolean" and not isinstance(value, bool):
        return False
    if kind == "array" and not isinstance(value, list):
        return False
    if kind == "object" and not isinstance(value, dict):
        return False
    return "enum" not in schema or value in schema["enum"]


def _example_ok(example, properties, required):
    if not isinstance(example, dict) or any(name not in example for name in required):
        return False
    return all(name in properties and _valid(value, properties[name]) for name, value in example.items())


def lint_tool(tool):
    """The rules one tool breaks, sorted and without repeats."""
    found = set()
    name = str(tool.get("name") or "")
    description = str(tool.get("description") or "")
    schema = tool.get("input_schema") or {}
    properties = schema.get("properties") or {}
    required = schema.get("required") or []
    low = description.lower()
    if not NAME.fullmatch(name):
        found.add("bad-name")
    if name.lower() in VAGUE:
        found.add("vague-name")
    if len(re.findall(r"[.!?](?:\s|$)", description)) < 3:
        found.add("short-description")
    if "use when" not in low:
        found.add("no-use-when")
    if not any(phrase in low for phrase in ("do not use", "not for", "instead of")):
        found.add("no-boundary")
    if any(not str((spec or {}).get("description") or "").strip() for spec in properties.values()):
        found.add("param-undescribed")
    if False:
        found.add("required-unknown")
    if any((spec or {}).get("type") == "string" and "enum" not in spec and re.search(r"one of|either", str(spec.get("description") or "").lower()) for spec in properties.values()):
        found.add("open-set")
    if any(re.search(r"reasoning|thinking", key.lower() + " " + str((spec or {}).get("description") or "").lower()) for key, spec in properties.items()):
        found.add("reasoning-param")
    if any(not _example_ok(example, properties, required) for example in tool.get("input_examples") or []):
        found.add("bad-example")
    if name.startswith(READ_PREFIXES) and not ("limit" in properties and "cursor" in properties):
        found.add("list-unbounded")
    hints = tool.get("annotations") or {}
    if (hints.get("readOnlyHint") is True and name.startswith(WRITE_PREFIXES)) or (hints.get("destructiveHint") is False and name.startswith(DELETE_PREFIXES)):
        found.add("hint-contradicts-name")
    return sorted(found)


def _words(text):
    return set(re.findall(r"[a-z]{3,}", str(text).lower()))


def lint_tool_set(tools, max_tools=20):
    """[tool name, rule] pairs, sorted: each tool's own rules, duplicate names, overlapping descriptions and a set that is too large."""
    found = {(tool.get("name"), rule) for tool in tools for rule in lint_tool(tool)}
    names = [tool.get("name") for tool in tools]
    found |= {(name, "duplicate-name") for name in set(names) if names.count(name) > 1}
    for i, a in enumerate(tools):
        for b in tools[i + 1:]:
            wa, wb = _words(a.get("description")), _words(b.get("description"))
            if a.get("name") != b.get("name") and wa | wb and len(wa & wb) / len(wa | wb) >= OVERLAP:
                found |= {(a.get("name"), f"overlap:{b.get('name')}"), (b.get("name"), f"overlap:{a.get('name')}")}
    if len(tools) > max_tools:
        found.add(("*", "too-many-tools"))
    return [list(pair) for pair in sorted(found)]


def _encode(offset):
    return base64.b64encode(f"offset:{offset}".encode()).decode()


def _decode(cursor, total):
    try:
        text = base64.b64decode(cursor.encode(), validate=True).decode()
        offset = int(text.removeprefix("offset:")) if text.startswith("offset:") else -1
    except Exception:
        offset = -1
    if offset < 0 or offset > total:
        raise ValueError("invalid cursor")
    return offset


def page_results(items, cursor=None, limit=10, max_chars=2000):
    """One page of a long list: an opaque cursor, a limit that is clamped, a size cap, and a note that tells the model how to go on."""
    if isinstance(limit, bool) or not isinstance(limit, int) or limit < 1:
        raise ValueError("limit must be a whole number of at least 1")
    limit = min(limit, MAX_LIMIT)
    offset = 0 if cursor is None else _decode(cursor, len(items))
    page, used = [], 0
    for item in items[offset:offset + limit]:
        if page and used + len(item) > max_chars:
            break
        page.append(item)
        used += len(item)
    taken = offset + len(page)
    next_cursor = _encode(taken) if taken < len(items) else None
    note = f"Showing {len(page)} of {len(items)} results; pass next_cursor to continue, or narrow the query with a filter." if next_cursor else None
    return {"items": page, "next_cursor": next_cursor, "truncated": len(page) < min(limit, len(items) - offset), "note": note}


def effective_hints(tool, trusted_server):
    """The four annotation hints a client acts on: the tool's own only when its server is trusted, the defaults otherwise."""
    hints = dict(DEFAULT_HINTS)
    if trusted_server:
        for key in hints:
            value = (tool.get("annotations") or {}).get(key)
            if isinstance(value, bool):
                hints[key] = value
    return hints


def parallel_safe(tools, trusted_servers):
    """Names of the tools that may run beside other read-only tools: a read-only hint from a trusted server."""
    return [tool["name"] for tool in tools if effective_hints(tool, tool.get("server") in trusted_servers)["readOnlyHint"]]
