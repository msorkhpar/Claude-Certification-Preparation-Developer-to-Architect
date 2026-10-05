"""One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.

The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
"""
import logging
import json

log = logging.getLogger(__name__)

TICKET = "I was charged twice for invoice 1042."
INVOICES = {"1042": "paid twice on 2026-09-30"}


def scripted(replies):
    """A stand-in model: it returns the next scripted reply and keeps the prompts it was shown."""
    queue, seen = list(replies), []

    def model(prompt):
        seen.append(prompt)
        return queue.pop(0)

    model.seen = seen
    return model


# 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

def run_graph(model, ticket, resume_from=0, checkpoints=None):
    def classify(s):
        return {"topic": model(f"Classify as billing or other: {s['ticket']}").strip().lower()}

    def lookup(s):
        return {"invoice": INVOICES.get(s["ticket"].split("invoice ")[1].rstrip("."), "unknown")}

    def draft(s):
        return {"reply": model(f"Write a reply. Topic: {s['topic']}. Invoice: {s.get('invoice', 'none')}")}

    nodes = {"classify": classify, "lookup": lookup, "draft": draft}
    edges = {"classify": lambda s: "lookup" if s["topic"] == "billing" else "draft", "lookup": lambda s: "draft", "draft": lambda s: None}
    checkpoints = checkpoints if checkpoints is not None else [("classify", {"ticket": ticket})]
    node, state = checkpoints[resume_from]
    del checkpoints[resume_from + 1:]
    path = []
    while node:
        state = {**state, **nodes[node](state)}
        path.append(node)
        node = edges[node](state)
        if node:
            checkpoints.append((node, dict(state)))
    return {"path": path, "state": state, "checkpoints": checkpoints}


# 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

def run_agent(model, ticket, max_steps=5):
    tools = {"lookup_invoice": lambda arg: INVOICES.get(arg, "unknown")}
    trace, observation = [], ""
    for _ in range(max_steps):
        step = json.loads(model(f"Ticket: {ticket}\nTools: {sorted(tools)}\nLast result: {observation}\nReply JSON: a tool call or a final reply."))
        if "final" in step:
            return {"reply": step["final"], "trace": trace}
        observation = tools[step["tool"]](step["arg"])
        trace.append(f"{step['tool']}({step['arg']}) -> {observation}")
    return {"reply": None, "trace": trace, "stopped": "max_steps"}


# 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

SCHEMA = {"topic": str, "refund_cents": int}


def validate(raw):
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return None, "the reply is not JSON"
    for field, kind in SCHEMA.items():
        if not isinstance(data.get(field), kind) or isinstance(data.get(field), bool):
            return None, f"field {field} must be {kind.__name__}"
    return data, None


def run_typed(model, ticket, retries=1):
    prompt, attempts = f"Return JSON with topic and refund_cents for: {ticket}", 0
    while True:
        attempts += 1
        data, error = validate(model(prompt))
        if data is not None:
            return {"data": data, "attempts": attempts}
        if attempts > retries:
            return {"data": None, "attempts": attempts, "error": error}
        prompt = f"{prompt}\nYour last reply was refused: {error}. Fix it."


def main():
    m = scripted(["billing", "We refunded the duplicate charge on invoice 1042."])
    graph = run_graph(m, TICKET)
    print("graph:", " -> ".join(graph["path"]), "| model calls:", len(m.seen), "| checkpoints:", len(graph["checkpoints"]))
    again = scripted(["We refunded the duplicate charge on invoice 1042."])
    resumed = run_graph(again, TICKET, resume_from=2, checkpoints=list(graph["checkpoints"]))
    print("graph resumed from checkpoint 2:", " -> ".join(resumed["path"]), "| model calls:", len(again.seen), "| same reply:", resumed["state"]["reply"] == graph["state"]["reply"])
    a = scripted(['{"tool": "lookup_invoice", "arg": "1042"}', '{"final": "Refunded the second payment."}'])
    agent = run_agent(a, TICKET)
    print("agent:", agent["trace"], "->", agent["reply"], "| model calls:", len(a.seen))
    loop = scripted(['{"tool": "lookup_invoice", "arg": "1"}'] * 3)
    print("agent that never finishes:", run_agent(loop, TICKET, max_steps=3)["stopped"], "after", len(loop.seen), "calls")
    t = scripted(['{"topic": "billing", "refund_cents": "4999"}', '{"topic": "billing", "refund_cents": 4999}'])
    typed = run_typed(t, TICKET)
    print("typed:", typed["data"], "| attempts:", typed["attempts"], "| second prompt ends:", t.seen[1].split("\n")[-1])
    bad = run_typed(scripted(["no json", "still no json"]), TICKET)
    print("typed, never valid:", bad["error"], "after", bad["attempts"], "attempts")


if __name__ == "__main__":
    main()
