# Case lists of module 85-retrieval-pipelines-at-design-level: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/85-retrieval-pipelines-at-design-level/unit-01/practice-1"] = {
    "name": "pipeline", "suite": "PipelineTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "sections become chunks that carry their title and section and the version of their document"),
        ("e1", "edge", "a long section splits at sentence ends under the word limit and every part keeps the prefix"),
        ("e2", "edge", "a code outweighs a word and a word in no chunk matches nothing"),
        ("e3", "edge", "a search for a reader never returns a chunk of a document that reader may not see"),
        ("e4", "edge", "the mechanism follows the corpus size then the data shape then the query pattern"),
        ("e5", "edge", "a reindex keeps unchanged documents replaces changed ones adds new ones and drops removed ones"),
        ("e6", "edge", "stale lists the chunks whose document changed or vanished"),
        ("e7", "edge", "recall counts every labelled question and a question with no results is a miss"),
    ],
    "plants": {
        "wrong-no-prefix": (["m1"], "leaves the title and section out of the chunk text"),
        "wrong-no-split": (["e1"], "never splits a long section"),
        "wrong-code-weight": (["e2"], "scores a code like any other word"),
        "wrong-filter-after-cut": (["e3"], "applies the access filter after it has taken the best k, so a reader gets fewer results than exist"),
        "wrong-always-index": (["e4"], "builds an index even for a corpus that fits a cached prompt"),
        "wrong-table-chunked": (["e4"], "cuts a table into chunks and searches them instead of querying it"),
        "wrong-reindex-additive": (["e5"], "keeps the old chunks of a document that changed"),
        "wrong-reindex-keeps-removed": (["e5"], "keeps the chunks of a document that no longer exists"),
        "wrong-stale-ignores-removed": (["e6"], "does not call the chunks of a removed document stale"),
        "wrong-recall-answered-only": (["e7"], "divides by the questions that had results instead of all labelled questions"),
    },
}
