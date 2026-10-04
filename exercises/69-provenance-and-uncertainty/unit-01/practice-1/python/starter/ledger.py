"""Claims that keep their sources: required provenance fields, a merge that records agreement, change over time and conflict, a coverage note with gaps, and rendering by content type. See ../../statement.md."""

REQUIRED = ("claim", "value", "source", "date")
KINDS = ("financial", "news", "technical")


def check_finding(finding):
    # TODO: the names of the required fields that are missing or empty, in the order of REQUIRED.
    return None


def merge(findings):
    # TODO: one entry per claim, with its values, their sources and a status; refuse an incomplete finding.
    return None


def coverage_note(planned, merged, unavailable):
    # TODO: which claims are well supported, single-source, changed, contested, and which planned claims are gaps.
    return None


def render(entry, kind):
    # TODO: a table for financial data, prose for news, a bulleted list for technical findings.
    return None
