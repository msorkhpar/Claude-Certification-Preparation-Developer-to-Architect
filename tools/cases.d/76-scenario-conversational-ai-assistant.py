# Case lists of module 76-scenario-conversational-ai-assistant: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/76-scenario-conversational-ai-assistant/unit-01/practice-1"] = {
    "name": "conversation_review", "suite": "ConversationReviewTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a mixed batch gets every count the segments and a verdict"),
        ("e1", "edge", "an empty batch has zero figures and is held for lack of data"),
        ("e2", "edge", "a safety signal counts as missed unless it went to a person as a safety hand off"),
        ("e3", "edge", "a conversation is overlong only above the turn limit and only when nobody took over"),
        ("e4", "edge", "repeated questions are acceptable at exactly the limit and not above it"),
        ("e5", "edge", "a segment is weak only below the resolution floor and not at it"),
        ("e6", "edge", "a segment needs the minimum number of conversations before it can be called weak"),
        ("e7", "edge", "a hand off is over escalation only when no person was needed and no safety signal was present"),
        ("e8", "edge", "only conversations the assistant settled alone count as resolved"),
        ("e9", "edge", "percentages are whole numbers rounded half up"),
    ],
    "plants": {
        "wrong-safety-any-hand-off": (["e2"], "counts a safety signal as handled when any person took over and not a safety hand off"),
        "wrong-overlong-inclusive": (["e3"], "calls a conversation overlong when it has exactly the turn limit"),
        "wrong-repeat-strict": (["e1", "e4"], "accepts repeated questions only below the limit and not at it"),
        "wrong-weak-inclusive": (["m1", "e5"], "calls a segment weak when it is exactly at the resolution floor"),
        "wrong-weak-ignores-min-n": (["m1", "e6"], "calls a small segment weak whatever its size"),
        "wrong-min-n-strict": (["e6"], "needs more conversations than the minimum and not at least that many"),
        "wrong-over-escalated-counts-risk": (["e7"], "counts a safety hand off as over escalation"),
        "wrong-resolved-ignores-hand-off": (["e8"], "counts a conversation a person settled as resolved by the assistant"),
        "wrong-rounding-floor": (["m1", "e9"], "rounds a percentage down and not half up"),
        "wrong-safety-never-holds": (["e2"], "ships the assistant although a safety signal was missed"),
    },
}
