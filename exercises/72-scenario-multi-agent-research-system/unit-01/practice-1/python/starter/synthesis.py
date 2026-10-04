"""The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.

Read statement.md for the shapes of a result and of the report, then replace the body of synthesize().
"""


def synthesize(required, results):
    return {"status": "", "covered": [], "gaps": [], "partial": [], "claims": [], "conflicts": [], "errors": [], "note": ""}
