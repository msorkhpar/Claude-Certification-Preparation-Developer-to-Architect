"""What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md."""


def estimate_tokens(text):
    return -(-len(text) // 4)


def trim_record(record, keep):
    # TODO: the fields of the record named in keep, in that order, with their exact values.
    return None


def update_facts(facts, name, value, as_of):
    # TODO: a new facts map: {name: {"value", "as_of", "superseded"}}; the input is not changed.
    return None


def build_context(customer, facts, summary, recent):
    # TODO: the text of the context: case facts of this customer, then the summary, then the recent messages, each under a heading.
    return None


def missing_from_summary(summary, facts):
    # TODO: the names of the facts whose value the summary no longer holds.
    return None


def window(messages, budget):
    # TODO: the newest messages that fit the token budget, keeping every tool call together with its result.
    return None
