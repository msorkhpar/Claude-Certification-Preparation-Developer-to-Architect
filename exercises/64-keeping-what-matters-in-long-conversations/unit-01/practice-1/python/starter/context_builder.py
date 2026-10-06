"""What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)


def estimate_tokens(text):
    return -(-len(text) // 4)


def trim_record(record, keep):
    # TODO 1 of 6 (finish this to pass m1, e1): the trim. Receives a tool's record and the list of fields to keep.
    #   Return a new record with only those fields, in the order of the list, with their exact values, skipping a field
    #   that the record does not have. Example: record {id, status, notes}, keep [status, id] -> {status, id}.
    return dict(record)


def update_facts(facts, name, value, as_of):
    new = {n: {**f, "superseded": list(f["superseded"])} for n, f in facts.items()}
    current = new.get(name)
    if current is None:
        new[name] = {"value": value, "as_of": as_of, "superseded": []}
    # TODO 2 of 6 (finish this to pass e2, e3): the update of a known fact. When the new date is the same as or later
    #   than the stored one, replace the value and the date and append "oldvalue@olddate" to the history; when it is
    #   earlier, keep the current value and append "newvalue@newdate" to the history. Example: stored 5@2026-01-02, new
    #   7@2026-01-05 -> value 7, history [5@2026-01-02].
    else:
        current["value"], current["as_of"] = value, as_of
    return new


def build_context(customer, facts, summary, recent):
    log.debug("build_context input: %r", customer)
    parts = []
    # TODO 3 of 6 (finish this to pass e4): the facts of one customer. Receives the customer and all the fact entries.
    #   Keep only the entries whose customer is that customer. Example: facts of C1 and C2, customer C1 -> the C1 entries
    #   only.
    mine = list(facts)
    # TODO 4 of 6 (finish this to pass e5): the case facts section. When there are facts for the customer, add first a
    #   section titled "## Case facts" with one line per fact, "name: value (as of date)". Example: one fact -> "## Case
    #   facts\norder: A-7 (as of 2026-01-02)", before the summary.
    parts.append("## Summary so far\n" + summary)
    parts.append("## Recent messages\n" + "\n".join(f"{m['role']}: {m['text']}" for m in recent))
    return "\n\n".join(parts)


def missing_from_summary(summary, facts):
    # TODO 5 of 6 (finish this to pass e6): the check of a summary. Receives the summary and the fact entries. Return
    #   the names of the facts whose exact value does not appear in the summary text. Example: fact order = A-7, summary
    #   "customer wants a refund" -> [order].
    return []


def window(messages, budget):
    units, i = [], 0
    while i < len(messages):
        m = messages[i]
        # TODO 6 of 6 (finish this to pass e7): the units of the window. Walk the messages in order: a tool_use message
        #   followed by the tool_result with the same id forms one unit of two, which is never split; any other message is
        #   a unit of one. Example: [user, tool_use t1, tool_result t1] -> two units.
        units.append([m])
        i += 1
    kept, used = [], 0
    for unit in reversed(units):
        cost = sum(estimate_tokens(x["text"]) for x in unit)
        if used + cost > budget:
            break
        kept.insert(0, unit)
        used += cost
    return [m for unit in kept for m in unit]
