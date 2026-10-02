"""Capture hygiene: strip identifying values from a capture, and check that none remain.

A capture never carries an API key, a request id, or an organisation, workspace or account id.
`scan_text` is the gate; `scrub` removes what a capture run could have picked up from headers
and error bodies before the file is written. Both are standard library only.
"""
import json
import re

PATTERNS = {
    "api key": re.compile(r"\bsk-(?:ant-)?[A-Za-z0-9_\-]{16,}"),
    "request id": re.compile(r"\breq_[A-Za-z0-9]{8,}|\"request_id\"\s*:|\brequest-id\b", re.I),
    "organisation or account id": re.compile(
        r"\b(?:org|acct|ws|wrkspc)_[A-Za-z0-9]{6,}|\"(?:organization|org|account|workspace|user)_?id\"\s*:"
        r"|anthropic-organization-id", re.I),
    "email address": re.compile(r"[\w.+-]+@(?!example\.(?:com|invalid)\b)[\w-]+\.[\w.-]+"),
    "authorization header": re.compile(r"\"?(?:x-api-key|authorization)\"?\s*[:=]", re.I),
}
DROP_KEYS = {"request_id", "organization_id", "org_id", "account_id", "workspace_id", "user_id",
             "headers", "x-api-key", "authorization"}


class CaptureLeak(Exception):
    pass


def scan_text(text):
    """Return [(kind, matched_text)] for everything in `text` that must not be in a capture."""
    return [(kind, m.group(0)) for kind, rx in PATTERNS.items() for m in rx.finditer(text)]


def scrub(obj):
    """Copy of a JSON-like value with identifying keys removed at any depth."""
    if isinstance(obj, dict):
        return {k: scrub(v) for k, v in obj.items() if k.lower() not in DROP_KEYS}
    if isinstance(obj, list):
        return [scrub(v) for v in obj]
    return obj


def check_capture(path):
    """Raise CaptureLeak if the capture file contains a key, request id, org or account id."""
    with open(path, encoding="utf-8") as f:
        found = scan_text(f.read())
    if found:
        raise CaptureLeak(f"{path}: " + "; ".join(f"{k}: {v!r}" for k, v in found))


if __name__ == "__main__":
    import sys
    bad = 0
    for p in sys.argv[1:]:
        try:
            check_capture(p)
            print("clean:", p)
        except CaptureLeak as e:
            bad += 1
            print("LEAK:", e)
    sys.exit(1 if bad else 0)
