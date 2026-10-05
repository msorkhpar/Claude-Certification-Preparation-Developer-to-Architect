# Case lists of module 51-session-state: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/51-session-state/unit-01/practice-1"] = {
    "name": "sessions", "suite": "SessionsTest", "langs": ["python", "typescript"],
    "cases": [
        ("m1", "main", "the plan resumes an unchanged session and tells a changed one what differs and starts fresh when most of it changed"),
        ("e1", "edge", "changed deleted and added files are listed in order and any difference asks for a notice"),
        ("e2", "edge", "more than half of the files changed or gone starts fresh and exactly half does not"),
        ("e3", "edge", "a session idle for more than a week starts fresh and exactly a week is resumed"),
        ("e4", "edge", "no saved session starts fresh and a fork is only planned from a session that is resumed"),
        ("e5", "edge", "the change notice names only what differs and the prompt puts the notice or the summary before the task"),
        ("e6", "edge", "the summary has a fixed layout with blank and repeated items dropped and files listed by path"),
        ("e7", "edge", "options resume by id and fork together and continue is refused unless one session exists and a name must be unique"),
        ("e8", "edge", "a run through the sdk passes resume and fork to the binary and returns the session id even after an error"),
    ],
    "plants": {
        "wrong-ignores-changes": (["e1", "e2", "e5", "m1"], "resumes a session without a notice even when files changed"),
        "wrong-added-ignored": (["e1"], "does not count a new file as a difference"),
        "wrong-half-is-fresh": (["e2", "e5"], "starts fresh when exactly half of the files changed"),
        "wrong-added-counted": (["e2"], "counts new files toward the share of changed files"),
        "wrong-age-ignored": (["e3"], "resumes a session however long it has been idle"),
        "wrong-age-inclusive": (["e3"], "starts fresh when the session was idle for exactly a week"),
        "wrong-fork-without-session": (["e4"], "plans a fork even when the session is not resumed"),
        "wrong-notice-when-nothing": (["e5"], "writes a notice even when no file differs"),
        "wrong-summary-after-task": (["e5"], "puts the summary after the task"),
        "wrong-summary-keeps-repeats": (["e6"], "keeps repeated items in the summary"),
        "wrong-files-unsorted": (["e6"], "lists the files in the order they came"),
        "wrong-fork-alone": (["e7"], "sets the fork option even when there is no session to resume"),
        "wrong-continue-many": (["e7"], "continues the latest session when several exist"),
        "wrong-name-first-match": (["e7"], "resumes the first of several sessions that share a name"),
        "wrong-id-only-on-success": (["e8"], "reads the session id only from a successful result"),
        "wrong-no-fork-flag": (["e7", "e8"], "writes the fork option as false"),
    },
}
