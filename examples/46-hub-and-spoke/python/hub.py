"""Hub and spoke on the Messages API: a coordinator plans by calling a plan tool, each subagent is its own conversation, the coordinator synthesizes.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures. The point is what each
request contains: a subagent's request holds its brief and nothing else, and only the synthesis request holds the findings.
"""
import json

from harness import scripted_client
from harness.scripted import message, text, tool_use

MODEL = "claude-sonnet-5-5"
PLAN_TOOL = {"name": "plan", "description": "Record the plan: one subtask per independent part of the question, each with a scope and a self-contained brief.",
             "input_schema": {"type": "object", "properties": {"subtasks": {"type": "array", "items": {"type": "object", "properties": {"scope": {"type": "string"}, "brief": {"type": "string"}}, "required": ["scope", "brief"]}}}, "required": ["subtasks"]}}
SUBAGENT_SYSTEM = "You are a research subagent. Answer only the brief you are given and end with a one-line source note."

SUBTASKS = [
    {"scope": "chips", "brief": "Find what changed in 2024 chip supply. Return three bullet points and a source note. Do not cover cars or interest rates."},
    {"scope": "cars", "brief": "Find what changed in 2024 car output. Return three bullet points and a source note. Do not cover chips or interest rates."},
    {"scope": "rates", "brief": "Find what changed in 2024 interest rates. Return three bullet points and a source note. Do not cover chips or cars."},
]
REPORTS = ["CHIPS-REPORT: output of foundries rose, lead times fell. Source: industry survey.",
           "CARS-REPORT: plants ran fuller as chips arrived. Source: producer filings.",
           "RATES-REPORT: central banks held rates, loans stayed dear. Source: bank statements."]


def plan(client, question):
    # tool_choice stays auto: Claude Sonnet 5.5 returns a 400 error for a forced choice, so the request asks for the tool in words
    reply = client.messages.create(model=MODEL, max_tokens=800, tools=[PLAN_TOOL], messages=[{"role": "user", "content": f"{question}\nRecord your plan by calling the plan tool."}])
    return next(b.input["subtasks"] for b in reply.content if b.type == "tool_use")


def run_subagent(client, brief):
    """A fresh conversation: the system prompt of the role and the brief. Nothing of the coordinator's history is passed."""
    reply = client.messages.create(model=MODEL, max_tokens=800, system=SUBAGENT_SYSTEM, messages=[{"role": "user", "content": brief}])
    return "".join(b.text for b in reply.content if b.type == "text")


def synthesize(client, question, findings):
    listing = "\n".join(f"[{scope}] {report}" for scope, report in findings)
    reply = client.messages.create(model=MODEL, max_tokens=800, messages=[{"role": "user", "content": f"Question: {question}\nFindings:\n{listing}\nWrite one answer that uses every finding."}])
    return "".join(b.text for b in reply.content if b.type == "text")


def main():
    question = "How did the 2024 supply picture change for chips, cars and interest rates?"
    replies = [message([text("Three independent parts."), tool_use("toolu_01", "plan", subtasks=SUBTASKS)], stop_reason="tool_use", model=MODEL)]
    replies += [message([text(r)], model=MODEL) for r in REPORTS]
    replies += [message([text("Chip supply recovered first, car output followed, and rates stayed high.")], model=MODEL)]
    client, transport = scripted_client(*replies)
    subtasks = plan(client, question)
    print("plan:", len(subtasks), "subtasks ->", [s["scope"] for s in subtasks])
    findings = []
    for number, task in enumerate(subtasks, start=1):
        report = run_subagent(client, task["brief"])
        findings.append((task["scope"], report))
        request = transport.requests[number]
        body = json.dumps(request)
        others = [r.split(":")[0] for r in REPORTS if r != report and r.split(":")[0] in body]
        print(f"subagent {number} request: {len(request['messages'])} message, system prompt of the role: {request['system'] == SUBAGENT_SYSTEM}, "
              f"holds the coordinator's question: {question in body}, holds another report: {bool(others)}")
    answer = synthesize(client, question, findings)
    synthesis = json.dumps(transport.requests[4])
    print("synthesis request: reports included:", sum(r.split(":")[0] in synthesis for r in REPORTS), "of", len(REPORTS))
    print("model calls:", len(transport.requests), "| one plan, three subagents, one synthesis")
    print("answer:", answer)


if __name__ == "__main__":
    main()
