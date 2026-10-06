"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from prompt_plan import assemble, choose_model

# Prompt modules: static ones first (they can be cached), then the changing ones by priority.
role = {"name": "role", "static": True, "text": "r" * 400}
policy = {"name": "policy", "static": True, "text": "p" * 1648}
history = {"name": "history", "static": False, "priority": 1, "text": "h" * 200}
question = {"name": "question", "static": False, "priority": 9, "text": "Q: {q}"}

prompt = assemble([question, role, history, policy], {"q": "hello"}, 10_000)
print("block order:", [b["name"] for b in prompt["blocks"]])
print("cache breakpoint after block:", prompt["breakpoint"], "| tokens:", prompt["tokens"], "| dropped:", prompt["dropped"])

# With a small budget the lowest-priority changing module is dropped.
small = assemble([question, role, history, policy], {"q": "hello"}, 520)
print("dropped with a budget of 520:", small["dropped"])

# The cheapest model that meets the tier and the latency.
models = [{"name": "haiku", "tier": 1, "latency_ms": 300, "price_out": 5}, {"name": "sonnet", "tier": 2, "latency_ms": 900, "price_out": 15}]
print("model:", choose_model({"tier": 2, "max_latency_ms": 1000}, models))
