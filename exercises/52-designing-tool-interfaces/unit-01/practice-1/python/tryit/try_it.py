"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from toolset import lint_tool

# A well-made tool, like the one the tests start from: a clear description with when to use it and when not.
good = {
    "name": "lookup_order",
    "description": ("Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id "
                    "such as A-1042 or asks where an order is. Do not use it to find a customer by name; use get_customer instead of "
                    "this tool for that. It returns no payment details."),
    "input_schema": {"type": "object", "required": ["order_id"],
                     "properties": {"order_id": {"type": "string", "description": "The order id, for example A-1042."}}},
    "input_examples": [{"order_id": "A-1042"}],
    "annotations": {"readOnlyHint": True},
}
# A poor one: a vague name, a short description and a parameter nobody explained.
poor = {"name": "helper", "description": "Gets stuff.",
        "input_schema": {"type": "object", "properties": {"q": {"type": "string"}}, "required": ["q"]}}

print("good tool:", lint_tool(good))
print("poor tool:", lint_tool(poor))
