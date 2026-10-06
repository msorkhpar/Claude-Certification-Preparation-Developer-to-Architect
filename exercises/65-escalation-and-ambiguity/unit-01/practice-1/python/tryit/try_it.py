"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from escalation import clarifying_fields, decide, handoff_text

# A customer who asks for a person is escalated at once; a calm one the agent can resolve is not.
asked = {"asked_for_person": True, "matches": 1, "policy_covers": True, "attempts_without_progress": 0, "sentiment": "calm", "confidence": 95}
print("asked for a person:", decide(asked))
calm = {**asked, "asked_for_person": False}
print("calm and covered:", decide(calm))

# Two customers match the name: ask only for the field that tells them apart.
matches = [{"id": "c1", "name": "Ana Ruiz", "email": "ana@example.com", "zip": "10115"},
           {"id": "c2", "name": "Ana Ruiz", "email": "ana.r@example.com", "zip": "10115"}]
print("ask for:", clarifying_fields(matches))

# The hand-off carries the facts, not the transcript.
case = {"customer_id": "C-77", "issue": "refund over the limit", "root_cause": "duplicate charge", "amount": "$129.50",
        "actions": ["verified identity", "checked order"], "recommended": "approve the refund", "transcript": "user: hello ... 40 turns ..."}
print(handoff_text(case))
