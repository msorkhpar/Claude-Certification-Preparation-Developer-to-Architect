"""A review specification that cuts false positives: the prompt, the trust in each category and the next step when a request is incomplete. See ../../statement.md."""

VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "when you are sure", "use your judgment")  # phrases that name no pattern


def build_review_prompt(spec, diff):
    # TODO: refuse a specification that is vague or incomplete (ValueError), then write the prompt: criteria, examples, diff last.
    return None


def category_report(findings, min_reviewed=5, min_precision=0.5):
    # TODO: per category the number reviewed, the precision, whether to disable it and its most dismissed patterns.
    return None


def next_step(request, required, defaults, attended):
    # TODO: proceed, ask or stop for a request with missing fields, and state the assumptions made.
    return None
