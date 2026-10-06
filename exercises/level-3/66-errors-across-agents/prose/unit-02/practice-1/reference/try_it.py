"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from error_flow import coordinator_plan, coverage_note, search_with_recovery

def flaky(query, attempt):
    """A stand-in for a search subagent, like the tests use: it times out once, then answers."""
    if attempt == 1:
        return {"status": "error", "type": "timeout", "partial": []}
    return {"status": "ok", "items": ["a", "b"]}


def forbidden(query, attempt):
    """A search the agent may not run: the failure is not transient."""
    return {"status": "error", "type": "permission", "partial": []}


news = search_with_recovery("news", flaky)
print("transient failure:", news)
filings = search_with_recovery("filings", forbidden)
print("permission failure:", filings)

# What the coordinator does with each outcome, and what the final report admits.
results = {"news": news, "filings": filings}
print("plan:", coordinator_plan(results))
print(coverage_note(results, ["news", "filings", "patents"]))
