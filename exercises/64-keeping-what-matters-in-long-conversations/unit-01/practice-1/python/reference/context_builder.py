"""What a long conversation keeps: trimmed tool output, case facts that newer information replaces, a context that never mixes customers, and a window that keeps tool calls whole. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)


def estimate_tokens(text):
    return -(-len(text) // 4)


def trim_record(record, keep):
    return {k: record[k] for k in keep if k in record}


def update_facts(facts, name, value, as_of):
    new = {n: {**f, "superseded": list(f["superseded"])} for n, f in facts.items()}
    current = new.get(name)
    if current is None:
        new[name] = {"value": value, "as_of": as_of, "superseded": []}
    elif as_of >= current["as_of"]:
        current["superseded"].append(f"{current['value']}@{current['as_of']}")
        current["value"], current["as_of"] = value, as_of
    else:
        current["superseded"].append(f"{value}@{as_of}")
    return new


def build_context(customer, facts, summary, recent):
    log.debug("build_context input: %r", customer)
    parts = []
    mine = [f for f in facts if f["customer"] == customer]
    if mine:
        parts.append("## Case facts\n" + "\n".join(f"{f['name']}: {f['value']} (as of {f['as_of']})" for f in mine))
    parts.append("## Summary so far\n" + summary)
    parts.append("## Recent messages\n" + "\n".join(f"{m['role']}: {m['text']}" for m in recent))
    return "\n\n".join(parts)


def missing_from_summary(summary, facts):
    return [f["name"] for f in facts if f["value"] not in summary]


def window(messages, budget):
    units, i = [], 0
    while i < len(messages):
        m = messages[i]
        if m["kind"] == "tool_use" and i + 1 < len(messages) and messages[i + 1]["kind"] == "tool_result" and messages[i + 1]["id"] == m["id"]:
            units.append([m, messages[i + 1]])
            i += 2
        else:
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
