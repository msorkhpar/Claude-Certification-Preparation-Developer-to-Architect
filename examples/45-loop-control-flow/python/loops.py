"""Three loops over the same scripted replies: one ends on stop_reason, one on a word in the text, one after a fixed count.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
Only the first loop is right; the other two show what the two anti-patterns of the Architect exam do to a run.
"""
from harness import scripted_client
from harness.scripted import message, text, tool_use

MODEL = "claude-sonnet-5-5"
TOOLS = [
    {"name": "save_file", "description": "Save the report under a file name. Use it once the report is written.",
     "input_schema": {"type": "object", "properties": {"file": {"type": "string"}}, "required": ["file"]}},
    {"name": "lookup", "description": "Look up item number n and return its record.",
     "input_schema": {"type": "object", "properties": {"n": {"type": "integer"}}, "required": ["n"]}},
]


def run_tools(reply, ran):
    results = []
    for block in reply.content:
        if block.type == "tool_use":
            ran.append(block.name)
            results.append({"type": "tool_result", "tool_use_id": block.id, "content": f"{block.name} ok"})
    return results


def text_of(reply):
    return "".join(b.text for b in reply.content if b.type == "text")


def by_stop_reason(client, task, backstop=10):
    """Right: the model's own stop_reason decides. The count is only a backstop with a status of its own."""
    messages, ran = [{"role": "user", "content": task}], []
    for call in range(1, backstop + 1):
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason != "tool_use":
            return "done", call, ran, text_of(reply)
        messages.append({"role": "user", "content": run_tools(reply, ran)})
    return "max_turns", backstop, ran, text_of(reply)


def by_text_marker(client, task):
    """Wrong: it reads the words. A reply that says 'done' ends the run, even with a tool call in the same reply."""
    messages, ran, call = [{"role": "user", "content": task}], [], 0
    while True:
        call += 1
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if "done" in text_of(reply).lower():
            return "done", call, ran, text_of(reply)
        messages.append({"role": "user", "content": run_tools(reply, ran)})


def by_fixed_count(client, task, turns=3):
    """Wrong: the count is the loop. Whatever the third reply holds is returned as the answer."""
    messages, ran = [{"role": "user", "content": task}], []
    for call in range(1, turns + 1):
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason == "tool_use":
            messages.append({"role": "user", "content": run_tools(reply, ran)})
    return "done", turns, ran, text_of(reply)


def scenario_a():
    return [message([text("All done with the analysis. Saving it now."), tool_use("toolu_01", "save_file", file="report.txt")], stop_reason="tool_use", model=MODEL),
            message([text("Saved report.txt.")], model=MODEL)]


def scenario_b():
    return [message([tool_use(f"toolu_0{n}", "lookup", n=n)], stop_reason="tool_use", model=MODEL) for n in range(1, 5)] + [message([text("Looked up 4 items.")], model=MODEL)]


def show(label, outcome):
    status, calls, ran, answer = outcome
    print(f"  {label:<26} status={status:<9} model calls={calls}  tools run={len(ran)}  text={answer!r}")


def main():
    print("A: the reply says 'All done' and also calls save_file")
    show("stop_reason loop", by_stop_reason(scripted_client(*scenario_a())[0], "Write and save the report."))
    show("text-marker loop", by_text_marker(scripted_client(*scenario_a())[0], "Write and save the report."))
    print("B: the task needs four lookups, then the model ends its turn")
    show("stop_reason loop, cap 10", by_stop_reason(scripted_client(*scenario_b())[0], "Look up items 1 to 4."))
    show("stop_reason loop, cap 3", by_stop_reason(scripted_client(*scenario_b())[0], "Look up items 1 to 4.", backstop=3))
    show("fixed-count loop of 3", by_fixed_count(scripted_client(*scenario_b())[0], "Look up items 1 to 4."))


if __name__ == "__main__":
    main()
