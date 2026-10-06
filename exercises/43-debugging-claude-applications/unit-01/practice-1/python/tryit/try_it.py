"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from diagnose import diagnose

# A trace is the list of what happened: the request, then what came back.
REQUEST = {"kind": "request", "model": "claude-sonnet-5-5", "max_tokens": 1024, "tools": ["get_weather"], "last_user_blocks": ["text"]}


def error(status, error_type):
    return {"kind": "error", "status": status, "error_type": error_type, "message": "m"}


for status, error_type in ((401, "authentication_error"), (504, "timeout_error"), (529, "overloaded_error")):
    d = diagnose([REQUEST, error(status, error_type)]) or {}
    print(f"HTTP {status}: type={d.get('type')} origin={d.get('origin')} recovery={d.get('recovery')}")
