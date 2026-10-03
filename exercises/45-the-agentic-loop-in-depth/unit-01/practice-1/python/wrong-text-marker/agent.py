"""The agent loop, driven by the stop reason. See ../../statement.md."""


def _text(content):
    return "".join(block.get("text", "") for block in content if block.get("type") == "text")


def _run_tool(block, tools):
    handler = tools.get(block.get("name"))
    if handler is None:
        return {"type": "tool_result", "tool_use_id": block["id"], "content": f"Unknown tool: {block.get('name')}", "is_error": True}
    try:
        return {"type": "tool_result", "tool_use_id": block["id"], "content": handler(block.get("input", {}))}
    except Exception as error:  # a failing tool is a result for the model, not a crash of the loop
        return {"type": "tool_result", "tool_use_id": block["id"], "content": str(error), "is_error": True}


def run_agent(model, tools, task, max_turns=8):
    messages = [{"role": "user", "content": task}]
    turns, last_text = 0, ""
    while True:
        if turns >= max_turns:  # the count is a backstop: it only ends a run the model has not ended itself
            return {"status": "max_turns", "text": last_text, "turns": turns, "messages": messages}
        turns += 1
        reply = model(list(messages))
        content = reply["content"]
        last_text = _text(content)
        messages.append({"role": "assistant", "content": content})
        reason = reply["stop_reason"]
        if "done" in last_text.lower():
            return {"status": "done", "text": last_text, "turns": turns, "messages": messages}
        if reason == "tool_use":
            calls = [block for block in content if block.get("type") == "tool_use"]
            if not calls:
                return {"status": "malformed", "text": last_text, "turns": turns, "messages": messages}
            messages.append({"role": "user", "content": [_run_tool(block, tools) for block in calls]})
        elif reason in ("end_turn", "stop_sequence"):
            return {"status": "done", "text": last_text, "turns": turns, "messages": messages}
        elif reason == "max_tokens":
            return {"status": "truncated", "text": last_text, "turns": turns, "messages": messages}
        elif reason == "refusal":
            return {"status": "refused", "text": last_text, "turns": turns, "messages": messages}
        else:
            return {"status": "unexpected", "text": last_text, "turns": turns, "messages": messages}
