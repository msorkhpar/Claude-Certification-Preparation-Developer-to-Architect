# Case lists of module 52-designing-tool-interfaces: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/52-designing-tool-interfaces/unit-01/practice-1"] = {
    "name": "toolset", "suite": "ToolsetTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a well made tool lints clean and a poor one is named for every rule it breaks"),
        ("e1", "edge", "names must match the pattern and a vague name is flagged"),
        ("e2", "edge", "a description needs three sentences a when to use phrase and a boundary against the neighbour"),
        ("e3", "edge", "parameters are described and required names exist and closed sets are enums and examples fit the schema"),
        ("e4", "edge", "a list tool needs a limit and a cursor and a hint may not contradict the name"),
        ("e5", "edge", "a set is graded for duplicate names overlapping descriptions and size"),
        ("e6", "edge", "pages carry an opaque cursor and a clamped limit and a note"),
        ("e7", "edge", "a page stops at the size cap and says so but always carries one item"),
        ("e8", "edge", "a tools own hints are trusted only from a trusted server and the defaults apply otherwise"),
    ],
    "plants": {
        "wrong-name-spaces-allowed": (["e1"], "accepts a name with spaces, dots or a hash sign"),
        "wrong-vague-case-sensitive": (["e1"], "flags a vague name only when it is written in lower case"),
        "wrong-two-sentences-ok": (["e2"], "accepts a description of two sentences"),
        "wrong-boundary-do-not-only": (["e2"], "accepts only the words do not use as a boundary against a neighbouring tool"),
        "wrong-required-unchecked": (["e3"], "does not notice a required name that is not a property"),
        "wrong-example-enum-ignored": (["e3"], "accepts an example whose value is outside the enum"),
        "wrong-bool-is-integer": (["e3"], "accepts true as an integer example"),
        "wrong-undescribed-ok": (["m1", "e3"], "does not notice a parameter without a description"),
        "wrong-limit-only": (["e4"], "asks a list tool for a limit but not for a cursor"),
        "wrong-hint-readonly-only": (["e4"], "checks the read-only hint against the name but not the destructive hint"),
        "wrong-duplicate-allowed": (["e5"], "lets two tools share a name"),
        "wrong-overlap-strict": (["e5"], "does not flag descriptions that overlap by exactly the threshold"),
        "wrong-count-off-by-one": (["e5"], "flags a set that is exactly as large as the budget"),
        "wrong-limit-unclamped": (["e6"], "lets the limit grow beyond the maximum page size"),
        "wrong-no-note": (["e6", "e7"], "leaves out the note that tells the model how to continue"),
        "wrong-cursor-unchecked": (["e6"], "starts from the beginning when the cursor is not valid"),
        "wrong-cap-exclusive": (["e7"], "stops one item early when a page fills the size cap exactly"),
        "wrong-truncated-never": (["e7"], "never reports that a page was cut by the size cap"),
        "wrong-untrusted-honoured": (["e8"], "reads the hints of a server that is not trusted"),
        "wrong-parallel-untrusted": (["e8"], "lets a tool of an untrusted server run in parallel on its own say so"),
    },
}
