"""Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md."""


def pct(part, whole):
    """Whole percent, half up, integers only (written for you)."""
    return (200 * part + whole) // (2 * whole) if whole else 0


def segment_table(results, costs):
    # TODO: (segment, cases, right, percent, error cost) per segment, the highest error cost first, ties by segment name.
    return None


def percentile(values, p):
    # TODO: the nearest-rank percentile of the values, 0 for no values.
    return None


def ab_verdict(x1, n1, x2, n2, min_n=200):
    # TODO: too few cases, no clear difference, new is better or old is better, at 95 percent.
    return None


def shadow_gate(pairs, protected):
    # TODO: {"decision", "lost", "gained", "blocked"} for a shadow run of the new version against the old.
    return None


def diagnose(found, supported, format_ok, passes_on_stronger):
    # TODO: retrieval or data, ungrounded answer, format instructions, prompt or task, or model mismatch, in that order.
    return None


def choose_model(options, min_accuracy, max_p95):
    # TODO: the cheapest option that meets the accuracy floor and the latency limit, or none.
    return None
