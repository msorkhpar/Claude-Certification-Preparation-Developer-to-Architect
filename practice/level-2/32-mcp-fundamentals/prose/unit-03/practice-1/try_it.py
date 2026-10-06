"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from notes_server import add_note, count, note, search_notes

# The tools and resources of the server are plain functions, so you can call them here without starting a client.
# (The tests start the server as a separate process and connect the SDK's client to it.)
try:
    print("add_note:", add_note("Plan", "ship it"))
    print("search_notes:", search_notes("ship"))
    print("count resource:", count())
    print("note 1 resource:", repr(note("1")))
except Exception as err:  # a gap that is not written yet may raise: show it instead of a traceback
    print("raised:", type(err).__name__, err)
