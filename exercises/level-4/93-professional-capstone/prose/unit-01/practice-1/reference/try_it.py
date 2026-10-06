"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from launch_review import launch_review, needed_accuracy, scorecard, verdict

# A design that sits at every threshold of the review, as the tests' clean design does.
CLEAN_FLAGS = {"feedback_loop", "model_measured", "replace_on_change", "deferral", "protected_segment", "rollback", "human_step", "owner",
               "accuracy_stated", "managed_settings", "irreversible_action", "team"}
CLEAN_NUMBERS = {"team_value_chats": 15, "tool_tokens": 10000, "eval_cases": 20, "rollout_stages": 3, "retain_days": 365, "floor_days": 90,
                 "ceiling_days": 365, "team_size": 10, "latency_ms": 2000, "availability_tenths": 995}

findings = launch_review(CLEAN_FLAGS, CLEAN_NUMBERS)
print("clean design:", findings, "->", verdict(findings))

# The same design with an agent that does not need to be one, and PII reaching the model.
risky = launch_review(CLEAN_FLAGS | {"agent", "path_known", "pii_reaches_model"}, CLEAN_NUMBERS)
print("risky design:", risky, "->", verdict(risky))
print("findings per domain:", scorecard(risky))
print("accuracy needed when an error costs 250 and a review 5:", needed_accuracy(250, 5))
