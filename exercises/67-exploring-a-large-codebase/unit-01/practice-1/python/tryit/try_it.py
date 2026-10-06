"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from recovery import add_finding, build_manifest, compact_command, render_scratchpad, resume_plan

# The scratchpad: one line per finding, a fact is recorded once per area.
findings = add_finding([], "auth", "tokens are signed in TokenSigner", "auth/TokenSigner.java:12")
findings = add_finding(findings, "billing", "invoices use cents", "billing/Money.java:5")
findings = add_finding(findings, "auth", "tokens are signed in TokenSigner", "auth/Other.java:99")
print(render_scratchpad(findings))

# The manifest of the subagents, and what to do with each after a crash.
agents = [
    {"name": "search", "state_file": "state/search.md", "status": "running"},
    {"name": "auth", "state_file": "state/auth.md", "status": "done"},
    {"name": "billing", "state_file": "state/billing.md", "status": "failed"},
]
manifest = build_manifest(agents)
print("manifest:", manifest)
print("resume plan:", resume_plan(manifest, {"state/auth.md", "state/search.md"}))
print("compact command:", compact_command(["the open questions", "file paths"]))
