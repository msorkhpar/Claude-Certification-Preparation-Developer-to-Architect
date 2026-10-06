"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from rollout import Request, migrate_request, retirement_status, rollout_step

# The retirement calendar: days left per model, nearest first, with the level of urgency.
models = [("claude-old-a", "2026-10-18", True), ("claude-old-b", "2026-11-30", False), ("claude-old-c", "2027-01-01", False)]
for line in retirement_status(models, "2026-10-04"):
    print("calendar:", line)

# A request written for an older model, and what the migration changes in it.
old = Request("claude-sonnet-4-5-20250929", 0.7, 0.9, 40, "disabled", "any", False, True)
new, changes = migrate_request(old)
print("migrated model:", new.model)
for change in changes:
    print("change:", change)

# A staged rollout: one decision per stage from the traffic and the errors seen so far.
print("stage 1:", rollout_step(1, 2000, 6, 1000, 5))
print("stage 25:", rollout_step(25, 50000, 400, 1000, 5))
