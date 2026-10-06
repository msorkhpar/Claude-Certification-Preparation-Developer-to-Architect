"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from sessions import change_notice, plan_session

NOW = 1_800_000_000
FILES = {"a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4"}
# What was saved about the session last time: its id, name, when it was used and the digest of each file it analysed.
record = {"id": "s-base", "name": "auth-review", "last_used": NOW - 3600, "files": dict(FILES)}

# Three situations: nothing changed, one file changed, most files changed.
cases = {
    "unchanged": dict(FILES),
    "one file changed": {**FILES, "b.py": "x"},
    "most files changed": {"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4"},
}
for name, current in cases.items():
    plan = plan_session(record, current, NOW) or {}
    print(f"{name}: action={plan.get('action')} session={plan.get('session_id')} changed={plan.get('changed')}")

print("notice for the changed one:", repr(change_notice(plan_session(record, cases["one file changed"], NOW) or {})))
