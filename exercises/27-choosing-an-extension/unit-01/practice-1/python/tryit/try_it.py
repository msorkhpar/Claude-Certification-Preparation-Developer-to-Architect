"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from extension_choice import choose

# Three situations from the scenario bank, described by a few features.
situations = {
    "a rule that must never be broken": {"guarantee": True, "knowledge": "convention"},
    "a database that needs a connection": {"external_system": True},
    "work that must run while the laptop is closed": {"timing": "interval", "presence": "away"},
}
for name, features in situations.items():
    choice = choose(features)
    print(f"{name}: {choice}")
