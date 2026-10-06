from case_facts import assemble, case_facts_block, render, shrink, stale, tokens


def test_shrinking_keeps_the_fields_the_tool_is_used_for_with_exact_values():
    result = {"order_id": "A-1", "refund_amount": "$129.50", "secret_hash": "zz", "items": "kettle", "purchase_date": "d", "return_window": "30 days"}
    assert render(shrink("lookup_order", result)) == "order_id=A-1;purchase_date=d;items=kettle;return_window=30 days;refund_amount=$129.50"
    assert shrink("lookup_customer", {"tier": "gold", "x": "1"}) == {"tier": "gold"}


def test_tokens_round_up_by_four_characters():
    assert [tokens(""), tokens("abcd"), tokens("abcde")] == [0, 1, 2]


def test_the_facts_block_carries_the_value_and_the_day_it_was_read():
    assert case_facts_block([("refund_amount", "$129.50", 118)]) == "## Case facts\nrefund_amount: $129.50 (as of day 118)"


def test_the_prompt_puts_key_facts_first_and_the_question_last():
    prompt = assemble("## Case facts\nx: 1 (as of day 1)", ["f1"], [("Doc", "text")], "Q?")
    assert [line for line in prompt.splitlines() if line.startswith("#")] == ["## Case facts", "## Key findings", "## Documents", "### Doc", "## Question"]
    assert prompt.endswith("Q?")


def test_a_value_older_than_the_limit_is_read_again():
    assert stale(118, 125, 3) is True and stale(124, 125, 3) is False and stale(122, 125, 3) is False
