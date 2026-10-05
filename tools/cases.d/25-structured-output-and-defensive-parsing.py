# Case lists of module 25-structured-output-and-defensive-parsing: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/25-structured-output-and-defensive-parsing/unit-01/practice-1"] = {
    "name": "extractor", "suite": "ExtractorTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a valid reply is returned after one call"),
        ("e1", "edge", "json is found in fences and prose and bad text is retried"),
        ("e2", "edge", "every schema violation is listed and sent back to the model"),
        ("e3", "edge", "the number of attempts is bounded"),
        ("e4", "edge", "a refusal or a cut off reply is not retried"),
        ("e5", "edge", "a quote that is not in the document is rejected"),
        ("e6", "edge", "types are exact booleans are not numbers and integers have no fraction"),
    ],
    "plants": {
        "wrong-no-prose-skip": (["e1"], "reads the whole reply as JSON and fails on prose around the object"),
        "wrong-generic-retry": (["e2"], "tells the model about only the first problem when it re-prompts"),
        "wrong-extra-attempt": (["e3"], "makes one call more than the attempt limit"),
        "wrong-retry-refusal": (["e4"], "treats a refusal as a bad reply and asks again"),
        "wrong-no-grounding": (["e5"], "accepts a quote that the document does not contain"),
        "wrong-integer-allows-fraction": (["e6"], "accepts 2.5 as an integer"),
    },
}
