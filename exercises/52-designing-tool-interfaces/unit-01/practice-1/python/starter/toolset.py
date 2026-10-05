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


def _name_rules(name):
    """TODO 1 of 8 (finish this to pass e1): the rule ids a tool name breaks.

    Receives the tool's name. Returns a set with `bad-name` when the name does not match NAME in full, and `vague-name` when the
    name in lower case is in VAGUE. Example: _name_rules("Helper") -> {"vague-name"}, _name_rules("my tool") -> {"bad-name"}
    """
    found = set()
    return found


def _description_rules(description):
    """TODO 2 of 8 (finish this to pass e2): the rule ids a description breaks.

    Receives the description text. Returns a set with `short-description` when it has fewer than 3 sentences (a `.`, `!` or `?`
    followed by white space or the end) and `no-boundary` when the lower-cased text has none of "do not use", "not for", "instead of".
    Example: _description_rules("Gets stuff.") -> {"short-description", "no-boundary"}
    """
    found = set()
    return found


def _parameter_rules(properties, required):
    """TODO 3 of 8 (finish this to pass e3): the rule ids the parameters break.

    Receives the schema's `properties` (name -> spec) and the `required` list. Returns a set with `param-undescribed` when a property
    has no description or a blank one, and `required-unknown` when `required` names something that is not a property.
    Example: _parameter_rules({"q": {"type": "string"}}, ["limit"]) -> {"param-undescribed", "required-unknown"}
    """
    found = set()
    return found


def _list_and_hint_rules(name, properties, hints):
    """TODO 4 of 8 (finish this to pass e4): the rule ids a list tool and its annotations break.

    Receives the name, the `properties` and the tool's `annotations` map. Returns a set with `list-unbounded` when the name starts
    with a READ_PREFIXES entry and `properties` lacks `limit` or `cursor`, and `hint-contradicts-name` when `readOnlyHint` is True
    and the name starts with a WRITE_PREFIXES entry, or `destructiveHint` is False and it starts with a DELETE_PREFIXES entry.
    Example: _list_and_hint_rules("list_users", {"limit": {}}, {}) -> {"list-unbounded"}
    """
    found = set()
    return found


def lint_tool(tool):
    """The rules one tool breaks, sorted and without repeats."""
    found = set()
    name = str(tool.get("name") or "")
    description = str(tool.get("description") or "")
    schema = tool.get("input_schema") or {}
    properties = schema.get("properties") or {}
    required = schema.get("required") or []
    found |= _name_rules(name)
    found |= _description_rules(description)
    if "use when" not in description.lower():
        found.add("no-use-when")
    found |= _parameter_rules(properties, required)
    if any((spec or {}).get("type") == "string" and "enum" not in spec and re.search(r"one of|either", str(spec.get("description") or "").lower()) for spec in properties.values()):
        found.add("open-set")
    if any(re.search(r"reasoning|thinking", key.lower() + " " + str((spec or {}).get("description") or "").lower()) for key, spec in properties.items()):
        found.add("reasoning-param")
    if any(not _example_ok(example, properties, required) for example in tool.get("input_examples") or []):
        found.add("bad-example")
    found |= _list_and_hint_rules(name, properties, tool.get("annotations") or {})
    return sorted(found)


def _words(text):
    return set(re.findall(r"[a-z]{3,}", str(text).lower()))


def _similar(wa, wb):
    """TODO 5 of 8 (finish this to pass e5): do two descriptions overlap?

    Receives two sets of words. Returns True when the share of common words over all the words is at least OVERLAP (0.6), and False
    when both sets are empty. Example: _similar({"a", "b", "c"}, {"a", "b", "c", "d"}) -> True (3 of 4), _similar({"a", "b"}, {"c", "d"}) -> False
    """
    return False


def lint_tool_set(tools, max_tools=20):
    """[tool name, rule] pairs, sorted: each tool's own rules, duplicate names, overlapping descriptions and a set that is too large."""
    found = {(tool.get("name"), rule) for tool in tools for rule in lint_tool(tool)}
    names = [tool.get("name") for tool in tools]
    found |= {(name, "duplicate-name") for name in set(names) if names.count(name) > 1}
    for i, a in enumerate(tools):
        for b in tools[i + 1:]:
            wa, wb = _words(a.get("description")), _words(b.get("description"))
            if a.get("name") != b.get("name") and _similar(wa, wb):
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


def _check_limit(limit):
    """TODO 6 of 8 (finish this to pass e6): validate and clamp a page limit.

    Receives the requested limit. Raises ValueError unless it is a whole number (not a bool) of at least 1; otherwise returns it cut
    to MAX_LIMIT. Example: _check_limit(500) -> 50, _check_limit(0) raises ValueError
    """
    return limit


def _over_cap(page, used, item, max_chars):
    """TODO 7 of 8 (finish this to pass e7): would this item push the page over the size cap?

    Receives the items already in the page, their total length `used`, the next item and `max_chars`. Returns True when the page is
    not empty and adding the item would make the total longer than max_chars; the first item is always taken, however long.
    Example: _over_cap(["a" * 5], 5, "b" * 6, 10) -> True, _over_cap([], 0, "b" * 99, 10) -> False
    """
    return False


def page_results(items, cursor=None, limit=10, max_chars=2000):
    """One page of a long list: an opaque cursor, a limit that is clamped, a size cap, and a note that tells the model how to go on."""
    limit = _check_limit(limit)
    offset = 0 if cursor is None else _decode(cursor, len(items))
    page, used = [], 0
    for item in items[offset:offset + limit]:
        if _over_cap(page, used, item, max_chars):
            break
        page.append(item)
        used += len(item)
    taken = offset + len(page)
    next_cursor = _encode(taken) if taken < len(items) else None
    note = f"Showing {len(page)} of {len(items)} results; pass next_cursor to continue, or narrow the query with a filter." if next_cursor else None
    return {"items": page, "next_cursor": next_cursor, "truncated": len(page) < min(limit, len(items) - offset), "note": note}


def effective_hints(tool, trusted_server):
    """TODO 8 of 8 (finish this to pass e8): the four annotation hints a client acts on.

    Receives a tool (its `annotations` map holds its own hints) and whether its server is trusted. Returns a dict with the four keys
    of DEFAULT_HINTS: the tool's own Boolean value for a key replaces the default only when the server is trusted (values that are
    not Booleans are ignored); an untrusted server gets the defaults.
    Example: effective_hints({"annotations": {"readOnlyHint": True}}, False)["readOnlyHint"] -> False; with True -> True
    """
    hints = dict(DEFAULT_HINTS)
    return hints


def parallel_safe(tools, trusted_servers):
    """Names of the tools that may run beside other read-only tools: a read-only hint from a trusted server."""
    return [tool["name"] for tool in tools if effective_hints(tool, tool.get("server") in trusted_servers)["readOnlyHint"]]
