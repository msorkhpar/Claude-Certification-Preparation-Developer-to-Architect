"""Four decisions about tools in a research and refund system: who gets which tool, what tool_choice a turn can use, whether a reply made the call it had to, and whether a refund may run.

No model is called. The catalog, the models and the limits are illustrative; the models that reject a forced choice are the ones the "Define tools" page lists, read on 2026-10-03.
"""

CATALOG = {
    "web_search": ["web"], "fetch_page": ["web"], "verify_fact": ["web", "synthesis"], "load_document": ["documents"], "extract_data_points": ["documents"],
    "summarize_content": ["synthesis"], "write_report": ["reports"], "send_report": ["reports"],
}
IRREVERSIBLE = {"send_report"}
ROLES = {"searcher": "web", "analyst": "documents", "synthesizer": "synthesis", "reporter": "reports"}
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}


def tools_for(role):
    return [name for name, tags in CATALOG.items() if ROLES[role] in tags and name not in IRREVERSIBLE]


def turn_for(model, forced, tools):
    """The request settings for a turn whose first call must be `forced`."""
    if model in NO_FORCING:
        return {"tool_choice": "auto", "tools": [forced], "check_reply": True}
    return {"tool_choice": f"tool:{forced}", "tools": tools, "check_reply": False}


def cache_cost(before, after):
    """What switching from one request to another costs in prompt caching."""
    if before["tools"] != after["tools"]:
        return "everything (the tool definitions changed)"
    if before["tool_choice"] != after["tool_choice"]:
        return "the cached messages (tool_choice changed)"
    return "nothing"


def made_the_call(reply, forced):
    calls = [block for block in reply if block[0] == "tool_use"]
    return bool(calls) and calls[0][1] == forced


def allowed(tool, amount, approved, cap=200):
    if tool not in {"refund", "lookup"}:
        return "refused: unknown tool"
    if tool == "lookup":
        return "run"
    if amount > cap:
        return f"refused: above the limit of {cap}, send to a person"
    return "run" if approved else "wait: a person must approve"


def main():
    print(f"catalog: {len(CATALOG)} tools")
    for role in ROLES:
        print(f"  {role}: {', '.join(tools_for(role))}")
    print("a synthesizer that may also check one fact has verify_fact, and nothing else from the web")
    print("first call must be extract_metadata:")
    both = ["extract_metadata", "enrich"]
    for model in ("claude-opus-5", "claude-sonnet-5-5"):
        turn = turn_for(model, "extract_metadata", both)
        print(f"  {model}: tool_choice={turn['tool_choice']}, tools={turn['tools']}, check the reply={turn['check_reply']}")
        print(f"    cost against a turn with auto and both tools: {cache_cost({'tool_choice': 'auto', 'tools': both}, turn)}")
    print("a reply to the fallback turn:")
    for label, reply in (("text only", [("text", "I will look at the metadata.")]), ("the right call", [("tool_use", "extract_metadata")]), ("another tool", [("tool_use", "enrich")])):
        print(f"  {label}: {'accept' if made_the_call(reply, 'extract_metadata') else 're-ask once, then escalate'}")
    print("refund decisions:")
    for tool, amount, approved in (("lookup", 0, False), ("refund", 150, False), ("refund", 150, True), ("refund", 400, True), ("delete_account", 0, True)):
        print(f"  {tool} {amount}, approved={'yes' if approved else 'no'}: {allowed(tool, amount, approved)}")


if __name__ == "__main__":
    main()
