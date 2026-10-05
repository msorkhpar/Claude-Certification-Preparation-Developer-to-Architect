# Case lists of module 32-mcp-fundamentals: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/32-mcp-fundamentals/unit-01/practice-1"] = {
    "name": "notes", "suite": "NotesTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a client can save a note find it and read it back"),
        ("e1", "edge", "the server declares tools resources and prompts and names its tools"),
        ("e2", "edge", "bad input comes back as a tool error the model can read"),
        ("e3", "edge", "search ignores case keeps id order honours the limit and says when nothing matches"),
        ("e4", "edge", "tool annotations tell a client which tool only reads"),
        ("e5", "edge", "resources give the count a note by id and an error for a missing one"),
        ("e6", "edge", "the prompt lists the notes and defaults the tone"),
        ("e7", "edge", "ids are sequential and a failed call does not use one"),
    ],
    "plants": {
        "wrong-limit-default": (["e1"], "states 10 as the default limit in the search tool's input schema"),
        "wrong-long-not-flagged": (["e2"], "saves a text longer than 500 characters instead of refusing it with a tool error"),
        "wrong-limit-bound": (["e2"], "accepts a limit of 21"),
        "wrong-case-sensitive": (["e3"], "matches the query only with the same letter case"),
        "wrong-no-limit": (["e3"], "returns every hit and ignores the limit argument"),
        "wrong-read-only-unmarked": (["e4"], "does not mark the search tool as read-only"),
        "wrong-plural": (["e5"], "says 1 notes instead of 1 note"),
        "wrong-default-tone": (["e6"], "uses another default tone when the client gives none"),
        "wrong-saves-untrimmed": (["e7"], "saves the title and text with the spaces around them and answers with the untrimmed title"),
    },
}
