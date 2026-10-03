"""Diagnose a failure from a trace. See ../../statement.md."""


def diagnose(trace):
    """The first failure in the trace: its index, type, origin, recovery, and whether a later response recovered."""
    return {"index": -1, "type": "ok", "origin": "none", "recovery": "none", "recovered": False}
