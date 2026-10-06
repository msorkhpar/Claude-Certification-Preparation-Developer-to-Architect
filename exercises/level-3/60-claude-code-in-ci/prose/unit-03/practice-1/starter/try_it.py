"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from rhythm_plan import choose

# Jobs a team wants to repeat: each is a small dictionary of what is known about it.
jobs = {
    "check the build while I watch": {},
    "poll a status page every 5 minutes": {"interval_seconds": 300},
    "nightly report, laptop may be closed": {"interval_seconds": 86400, "machine_off": True},
    "react to a pull request unattended": {"trigger": "event", "repo_event": True},
    "run in a pipeline": {"ci": True, "interval_seconds": 600},
}
for name, job in jobs.items():
    print(f"{name}: {choose(job)}")
