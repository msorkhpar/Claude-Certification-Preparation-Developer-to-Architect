# Case lists of module 17-streaming: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/17-streaming/unit-01/practice-1"] = {
    "name": "assemble", "suite": "AssembleTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "text deltas are joined and the message is complete"),
        ("e1", "edge", "tool input is the fragments joined then parsed and empty input is an empty object"),
        ("e2", "edge", "ping and unknown event types are ignored"),
        ("e3", "edge", "an error event raises with its type and message"),
        ("e4", "edge", "a stream that ends before message stop is an error not a short message"),
        ("e5", "edge", "usage takes input tokens from the start and the cumulative output from the end"),
        ("e6", "edge", "blocks keep their index order and thinking fields are assembled"),
    ],
    "plants": {
        "wrong-overwrite-text": (["m1"], "keeps only the last text fragment instead of appending"),
        "wrong-sum-usage": (["e5"], "adds the output tokens of message_delta to the output tokens of message_start"),
        "wrong-ignore-error": (["e3"], "skips an error event and returns what it has"),
        "wrong-accept-incomplete": (["e4"], "returns the partial message when message_stop never arrives"),
        "wrong-empty-input-null": (["e1"], "gives a tool block with no fragments a null input instead of an empty object"),
        "wrong-unknown-event-kept": (["e2"], "adds a block for a ping or an event type it does not know"),
        "wrong-signature-dropped": (["e6"], "drops the signature of a thinking block"),
    },
}
