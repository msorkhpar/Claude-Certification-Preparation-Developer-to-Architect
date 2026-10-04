"""A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md."""
from datetime import date

SAFE_INPUT = ("tokenise", "redact")  # pii_handling values that keep identifiers out of the prompt
WANT = {"us": "us", "eu": "eu", "other": "global"}  # the residency of the deployment that may serve a user region


def check_deployment(config, req):
    # TODO: the sorted list of finding ids for a deployment configuration against the requirements.
    return None


def retention_actions(entries, max_days, today):
    # TODO: {"purge": [ids], "keep": [ids]}, both sorted by id.
    return None


def pick_deployment(user_region, deployments):
    # TODO: the name of the deployment that may serve the user's region, or None.
    return None
