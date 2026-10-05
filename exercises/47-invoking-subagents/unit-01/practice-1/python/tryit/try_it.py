"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from subagents import build_options, make_brief, merge_findings, package_finding

# The options register each named subagent the model may start with the Agent tool.
specs = {"reviewer": {"description": "Reviews one module for security problems.", "prompt": "You review code.",
                      "tools": ["Read", "Grep"], "model": "sonnet"}}
options = build_options(specs, "/proj")
print("registered agents:", sorted(options.agents) if options else None)
print("allowed tools:", options.allowed_tools if options else None)

# The brief carries the facts a subagent cannot get any other way: it does not see the conversation.
try:
    brief = make_brief("Review the payment module for security problems",
                       files=["src/payments.py"], facts=["Amounts are in cents"], output="a list of findings, one per line")
    print(brief)
except ValueError as err:
    print("brief refused:", err)

# Findings keep their sources; the same claim from two subagents is merged and keeps both.
a = package_finding("Refunds take 5 days", url="https://example.invalid/refunds", title="Refund policy")
b = package_finding("refunds take 5 days", url="https://example.invalid/faq", page=3)
c = package_finding("Shipping is free over 50 euros")
print("merged:", merge_findings([a, b, c]))
