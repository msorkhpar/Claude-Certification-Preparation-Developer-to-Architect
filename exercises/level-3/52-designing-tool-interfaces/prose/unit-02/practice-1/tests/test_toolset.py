import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from toolset import effective_hints, lint_tool, lint_tool_set, page_results, parallel_safe

ORDER_TEXT = ("Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id such as A-1042 "
              "or asks where an order is. Do not use it to find a customer by name; use get_customer instead of this tool for that. It returns no payment details.")
CUSTOMER_TEXT = ("Finds one customer by email address and returns the customer id, name and plan. Use when the person gives an email or asks about their account. "
                 "Do not use it for orders; call lookup_order instead of this tool for those. It returns no payment details.")


def tool(name="lookup_order", description=ORDER_TEXT, properties=None, required=None, **more):
    props = {"order_id": {"type": "string", "description": "The order id, for example A-1042."}} if properties is None else properties
    return {"name": name, "description": description, "input_schema": {"type": "object", "properties": props, "required": ["order_id"] if required is None else required}, **more}


def rules(t):
    found = lint_tool(t)
    assert found is not None, "lint_tool returned None"
    return found


def pairs(tools, **kw):
    found = lint_tool_set(tools, **kw)
    assert found is not None, "lint_tool_set returned None"
    return found


def test_m1_a_well_made_tool_lints_clean_and_a_poor_one_is_named_for_every_rule_it_breaks():
    assert rules(tool(input_examples=[{"order_id": "A-1042"}], annotations={"readOnlyHint": True})) == []
    poor = {"name": "helper", "description": "Gets stuff.", "input_schema": {"type": "object", "properties": {"q": {"type": "string"}}, "required": ["q"]}}
    assert rules(poor) == ["no-boundary", "no-use-when", "param-undescribed", "short-description", "vague-name"]


def test_e1_names_must_match_the_pattern_and_a_vague_name_is_flagged():
    for bad in ("get order", "get.order", "a" * 129, "order#1"):
        assert rules(tool(name=bad)) == ["bad-name"], bad
    for good in ("a" * 128, "look-up-order", "run_report"):
        assert rules(tool(name=good)) == [], good
    for vague in ("run", "Run", "helper", "query"):
        assert rules(tool(name=vague)) == ["vague-name"], vague


def test_e2_a_description_needs_three_sentences_a_when_to_use_phrase_and_a_boundary_against_the_neighbour():
    assert rules(tool(description="Looks up one order by id. Use when the customer gives an order id, not for customers.")) == ["short-description"]
    assert rules(tool(description="Looks up one order by id. It returns the status and total. Do not use it for customers.")) == ["no-use-when"]
    assert rules(tool(description="Looks up one order by id. Use when the customer gives an order id. It returns the status and total.")) == ["no-boundary"]
    assert rules(tool(description="Looks up one order by id. USE WHEN the customer gives an id! Prefer it instead of get_customer for orders.")) == []
    assert rules(tool(description="")) == ["no-boundary", "no-use-when", "short-description"]


def test_e3_parameters_are_described_and_required_names_exist_and_closed_sets_are_enums_and_examples_fit_the_schema():
    assert rules(tool(properties={"order_id": {"type": "string", "description": "  "}})) == ["param-undescribed"]
    assert rules(tool(required=["order_id", "missing"])) == ["required-unknown"]
    status = {"type": "string", "description": "One of open, shipped or closed."}
    order_id = {"type": "string", "description": "The order id."}
    assert rules(tool(properties={"order_id": order_id, "status": status})) == ["open-set"]
    assert rules(tool(properties={"order_id": order_id, "status": {"type": "string", "description": "Either open or closed."}})) == ["open-set"]
    assert rules(tool(properties={"order_id": order_id, "status": {**status, "enum": ["open", "shipped", "closed"]}})) == []
    assert rules(tool(properties={"order_id": order_id, "reasoning": {"type": "string", "description": "Why."}})) == ["reasoning-param"]
    assert rules(tool(properties={"order_id": order_id, "note": {"type": "string", "description": "Your step by step thinking."}})) == ["reasoning-param"]
    assert rules(tool(properties={"order_id": order_id, "note": {"type": "string", "description": "A short explanation of why the call is made."}})) == []
    props = {"order_id": order_id, "status": {"type": "string", "description": "The status.", "enum": ["open", "closed"]}, "limit": {"type": "integer", "description": "Page size."}}
    assert rules(tool(properties=props, input_examples=[{"order_id": "A", "status": "open", "limit": 5}, {"order_id": "B"}])) == []
    for bad in ({"order_id": 5}, {}, {"order_id": "A", "extra": 1}, {"order_id": "A", "status": "lost"}, {"order_id": "A", "limit": True}, {"order_id": "A", "limit": "5"}):
        assert rules(tool(properties=props, input_examples=[bad])) == ["bad-example"], bad


def test_e4_a_list_tool_needs_a_limit_and_a_cursor_and_a_hint_may_not_contradict_the_name():
    base = {"query": {"type": "string", "description": "Search text."}}
    limit = {"type": "integer", "description": "Page size."}
    cursor = {"type": "string", "description": "Opaque cursor from the last page."}
    assert rules(tool(name="search_orders", properties=base, required=[])) == ["list-unbounded"]
    assert rules(tool(name="search_orders", properties={**base, "limit": limit}, required=[])) == ["list-unbounded"]
    assert rules(tool(name="list_orders", properties={}, required=[])) == ["list-unbounded"]
    assert rules(tool(name="find_customer", properties={**base, "limit": limit, "cursor": cursor}, required=[])) == []
    assert rules(tool(name="get_order", properties=base, required=[])) == []
    assert rules(tool(name="delete_order", annotations={"readOnlyHint": True})) == ["hint-contradicts-name"]
    assert rules(tool(name="delete_order", annotations={"destructiveHint": False})) == ["hint-contradicts-name"]
    assert rules(tool(name="remove_item", annotations={"destructiveHint": False})) == ["hint-contradicts-name"]
    assert rules(tool(name="send_receipt", annotations={"readOnlyHint": True})) == ["hint-contradicts-name"]
    for ok in (tool(name="delete_order", annotations={"destructiveHint": True}), tool(name="get_order", annotations={"readOnlyHint": True}), tool(name="create_order", annotations={"readOnlyHint": False})):
        assert rules(ok) == []


def test_e5_a_set_is_graded_for_duplicate_names_overlapping_descriptions_and_size():
    twin = tool(name="get_order_status")
    assert pairs([tool(), twin]) == [["get_order_status", "overlap:lookup_order"], ["lookup_order", "overlap:get_order_status"]]
    customer = tool(name="get_customer", description=CUSTOMER_TEXT)
    assert pairs([tool(), customer]) == []
    assert pairs([tool(), tool(description=CUSTOMER_TEXT)]) == [["lookup_order", "duplicate-name"]]

    def overlap(a, b):
        found = pairs([tool(name="a_tool", description=a), tool(name="b_tool", description=b)])
        return [p for p in found if p[1].startswith("overlap:")]

    assert overlap("alpha beta gamma delta", "alpha beta gamma epsilon") == [["a_tool", "overlap:b_tool"], ["b_tool", "overlap:a_tool"]]
    assert overlap("alpha beta gamma delta epsilon", "alpha beta gamma zeta eta") == []
    assert pairs([tool(), customer], max_tools=1) == [["*", "too-many-tools"]]
    assert pairs([tool(), customer], max_tools=2) == []


def paged(*args, **kwargs):
    result = page_results(*args, **kwargs)
    assert result is not None, "page_results returned None"
    return result


def test_e6_pages_carry_an_opaque_cursor_and_a_clamped_limit_and_a_note():
    rows = [f"row-{i:02d}" for i in range(25)]
    first = paged(rows)
    assert first is not None and first["items"] == rows[:10] and first["truncated"] is False
    assert first["note"] == "Showing 10 of 25 results; pass next_cursor to continue, or narrow the query with a filter."
    cursor = first["next_cursor"]
    assert isinstance(cursor, str) and "10" not in cursor and "offset" not in cursor
    second = paged(rows, cursor)
    assert second["items"] == rows[10:20] and second["next_cursor"] is not None
    last = paged(rows, second["next_cursor"])
    assert last["items"] == rows[20:] and last["next_cursor"] is None and last["note"] is None
    assert len(paged([f"r{i:03d}" for i in range(120)], limit=500)["items"]) == 50
    assert paged(rows, limit=3)["items"] == rows[:3]
    for bad_limit in (0, -1, True, "5", 2.5):
        try:
            paged(rows, limit=bad_limit)
        except ValueError:
            continue
        raise AssertionError(f"limit {bad_limit!r} was accepted")
    for bad_cursor in ("not a cursor!", ""):
        try:
            paged(rows, bad_cursor)
        except ValueError:
            continue
        raise AssertionError(f"cursor {bad_cursor!r} was accepted")


def test_e7_a_page_stops_at_the_size_cap_and_says_so_but_always_carries_one_item():
    ten = ["0123456789"] * 5
    cut = paged(ten, limit=10, max_chars=25)
    assert cut["items"] == ten[:2] and cut["truncated"] is True and cut["next_cursor"] is not None
    assert str(cut["note"]).startswith("Showing 2 of 5 results")
    exact = paged(ten, limit=10, max_chars=20)
    assert len(exact["items"]) == 2
    big = paged(["x" * 100, "y"], max_chars=10)
    assert big["items"] == ["x" * 100] and big["truncated"] is True
    whole = paged(ten, limit=10, max_chars=1000)
    assert whole["items"] == ten and whole["truncated"] is False and whole["next_cursor"] is None


def test_e8_a_tools_own_hints_are_trusted_only_from_a_trusted_server_and_the_defaults_apply_otherwise():
    defaults = {"readOnlyHint": False, "destructiveHint": True, "idempotentHint": False, "openWorldHint": True}
    reader = {"name": "get_order", "server": "orders", "annotations": {"readOnlyHint": True, "idempotentHint": True}}
    assert effective_hints(reader, True) == {"readOnlyHint": True, "destructiveHint": True, "idempotentHint": True, "openWorldHint": True}
    assert effective_hints(reader, False) == defaults
    assert effective_hints({"name": "x", "annotations": {"readOnlyHint": "yes"}}, True) == defaults
    assert effective_hints({"name": "x"}, True) == defaults
    writer = {"name": "update_order", "server": "orders", "annotations": {"readOnlyHint": False}}
    stranger = {"name": "get_notes", "server": "unknown", "annotations": {"readOnlyHint": True}}
    assert parallel_safe([reader, writer, stranger], {"orders"}) == ["get_order"]
    assert parallel_safe([reader, writer, stranger], {"orders", "unknown"}) == ["get_order", "get_notes"]
    assert parallel_safe([reader], set()) == []
