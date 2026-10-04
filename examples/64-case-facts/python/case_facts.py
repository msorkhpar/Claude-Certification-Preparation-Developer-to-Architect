"""What a long support conversation should keep, and where it should sit.

The exam guide (task 5.1) names four risks: a progressive summary turns numbers, dates and the customer's stated expectations into vague prose; models attend well to the start and the end of a long input and may miss the middle;
tool results pile up in the context out of proportion to their use (40 fields in an order lookup, five of them wanted); and the whole history must be sent on each request. The Claude documentation on long-context prompts
(read 2026-10-04) says to put long documents at the top and the question at the end, which can improve quality in tests by up to 30 percent, and to structure documents with tags. The functions below show the bookkeeping;
nothing here calls a model, and the numbers come from the sample data, not from a measurement.
"""
TOOL_FIELDS = {
    "lookup_order": ["order_id", "purchase_date", "items", "return_window", "refund_amount"],
    "lookup_customer": ["customer_id", "tier"],
}


def tokens(text):
    return -(-len(text) // 4)


def render(record):
    return ";".join(f"{k}={v}" for k, v in record.items())


def shrink(tool, result):
    """Keep what the next decision needs from a tool result, with its exact values."""
    return {k: result[k] for k in TOOL_FIELDS[tool] if k in result}


def case_facts_block(facts):
    """Facts the conversation must not lose, as a block that goes into every request outside the summarised history."""
    return "## Case facts\n" + "\n".join(f"{name}: {value} (as of day {day})" for name, value, day in facts)


def assemble(block, findings, documents, question):
    """Key facts first, a short findings summary, the long documents under headers, the question last."""
    parts = [block, "## Key findings\n" + "\n".join(f"- {f}" for f in findings), "## Documents\n" + "\n".join(f"### {title}\n{text}" for title, text in documents), "## Question\n" + question]
    return "\n\n".join(parts)


def stale(seen_day, today, max_age_days):
    """A value read days ago is re-read before it is acted on."""
    return today - seen_day > max_age_days


def main():
    order = {"order_id": "A-1042", "purchase_date": "2026-09-02", "items": "2 x kettle", "return_window": "30 days", "refund_amount": "$129.50"}
    for n in range(1, 36):
        order[f"internal_{n:02d}"] = f"backend-value-{n:02d}"
    small = shrink("lookup_order", order)
    print(f"lookup_order result: {len(order)} fields, {len(render(order))} characters, about {tokens(render(order))} tokens")
    print(f"after shrinking: {len(small)} fields, {len(render(small))} characters, about {tokens(render(small))} tokens")
    print(f"twenty lookups kept whole: {20 * tokens(render(order))} tokens; shrunk: {20 * tokens(render(small))} tokens")
    block = case_facts_block([("refund_amount", "$129.50", 118), ("return_deadline", "2026-09-30", 118)])
    print(block)
    prompt = assemble(block, ["The order qualifies for a refund", "The deadline is the binding fact"], [("Policy", "..."), ("Order history", "...")], "What should the customer be told?")
    print("section order: " + " | ".join(line for line in prompt.splitlines() if line.startswith("#")))
    for seen in (118, 124):
        print(f"refund_amount seen on day {seen}, today day 125, limit 3 days: " + ("read it again before acting" if stale(seen, 125, 3) else "still fresh"))


if __name__ == "__main__":
    main()
