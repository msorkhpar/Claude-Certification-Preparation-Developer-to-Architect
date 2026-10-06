"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from prompt_builder import build_prompt

# A small spec like the first main test case: a role, one document, one constraint and a task with a placeholder.
spec = {
    "role": "You are a careful support analyst for {{company}}.",
    "documents": [{"name": "policy.txt", "text": "Refunds within 30 days."}],
    "constraints": ["Answer in one word."],
    "task": "Classify the message about {{topic}}.",
}
prompt = build_prompt(spec, {"company": "Acme", "topic": "delivery"})

print("prompt length:", len(prompt))
print("starts with:", repr(prompt[:20]))
print("has task block:", "<task>\nClassify the message about delivery.\n</task>" in prompt)
print(prompt)
