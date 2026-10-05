# Case lists of module 13-one-rest-api-under-every-sdk: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/13-one-rest-api-under-every-sdk/unit-01/practice-1"] = {
    "name": "raw-client", "suite": "RawClientTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "request has method url three headers and json body"),
        ("e1", "edge", "blank or absent system is left out of the body"),
        ("e2", "edge", "bad input is refused before anything is sent"),
        ("e3", "edge", "success returns the message and text joins text blocks only"),
        ("e4", "edge", "an error reply becomes an api error with the header request id"),
        ("e5", "edge", "a reply that is not json still gives an api error"),
        ("e6", "edge", "the api key never appears in an error"),
    ],
    "plants": {
        "wrong-no-version-header": (["m1"], "leaves out the anthropic-version header"),
        "wrong-body-id-first": (["e4"], "prefers the request id in the body over the request-id header"),
        "wrong-no-redaction": (["e6"], "puts the server's message into the error without removing the API key"),
    },
}
