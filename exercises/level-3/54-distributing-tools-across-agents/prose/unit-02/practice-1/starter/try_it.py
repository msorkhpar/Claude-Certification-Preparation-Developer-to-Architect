"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from distribute import assign_tools, authorize

# The catalog and roles the tests use for the first main case m1.
CATALOG = [
    {"name": "web_search", "tags": ["web"]},
    {"name": "fetch_page", "tags": ["web"]},
    {"name": "load_document", "tags": ["documents"]},
    {"name": "verify_fact", "tags": ["web"], "scoped": True},
    {"name": "publish_report", "tags": ["reports"], "irreversible": True},
]
ROLES = {"searcher": {"specialisation": ["web"]}, "analyst": {"specialisation": ["documents"]}}

assigned = assign_tools(ROLES, CATALOG)
print("searcher tools:", assigned["searcher"] if assigned else assigned)
print("analyst tools:", assigned["analyst"] if assigned else assigned)

# A call to a tool that cannot be undone, checked against the policy.
POLICY = {"tools": {"process_refund": {"cap": 500, "irreversible": True}}}
call = {"id": "c1", "tool": "process_refund", "amount": 50, "customer": "C-1", "verified_customer": "C-1"}
print("without approval:", authorize(call, POLICY, []))
print("with approval:", authorize(call, POLICY, ["c1"]))
