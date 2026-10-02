def _text(content):
    return "".join(b["text"] for b in content if b["type"] == "text")


def run_agent(model, tools, user_text, max_turns=5):
    messages = [{"role": "user", "content": user_text}]
    for _ in range(max_turns):
        resp = model(messages)
        messages.append({"role": "assistant", "content": resp["content"]})
        if resp["stop_reason"] != "tool_use":
            return _text(resp["content"])
        results = []
        for block in resp["content"]:
            if block["type"] != "tool_use":
                continue
            try:
                out = str(tools[block["name"]](**block["input"]))
                results.append({"type": "tool_result", "tool_use_id": block["id"], "content": out})
            except Exception as exc:  # unknown tool (KeyError) or a failing tool
                msg = f"unknown tool {block['name']}" if isinstance(exc, KeyError) else str(exc)
                results.append({"type": "tool_result", "tool_use_id": block["id"],
                                "content": msg, "is_error": True})
        messages.append({"role": "user", "content": results})
    raise RuntimeError("max_turns exceeded")
