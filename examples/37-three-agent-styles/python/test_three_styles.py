from three_styles import TICKET, run_agent, run_graph, run_typed, scripted, validate


def test_the_graph_follows_its_edges_and_other_topics_skip_the_lookup():
    m = scripted(["Billing", "reply"])
    result = run_graph(m, TICKET)
    assert result["path"] == ["classify", "lookup", "draft"] and result["state"]["invoice"] == "paid twice on 2026-09-30" and len(m.seen) == 2
    other = run_graph(scripted(["other", "hello"]), "How do I log in?")
    assert other["path"] == ["classify", "draft"] and "invoice" not in other["state"]


def test_a_graph_resumes_from_a_checkpoint_without_repeating_earlier_nodes():
    first = run_graph(scripted(["billing", "the reply"]), TICKET)
    again = scripted(["the reply"])
    resumed = run_graph(again, TICKET, resume_from=2, checkpoints=list(first["checkpoints"]))
    assert resumed["path"] == ["draft"] and len(again.seen) == 1 and resumed["state"] == first["state"]


def test_the_agent_loop_runs_the_tool_the_model_picked_and_stops_at_the_step_limit():
    m = scripted(['{"tool": "lookup_invoice", "arg": "1042"}', '{"final": "done"}'])
    assert run_agent(m, TICKET) == {"reply": "done", "trace": ["lookup_invoice(1042) -> paid twice on 2026-09-30"]}
    endless = scripted(['{"tool": "lookup_invoice", "arg": "x"}'] * 4)
    result = run_agent(endless, TICKET, max_steps=4)
    assert result["reply"] is None and result["stopped"] == "max_steps" and len(endless.seen) == 4


def test_a_typed_reply_is_validated_and_a_mismatch_is_fed_back_once():
    assert validate('{"topic": "x", "refund_cents": 5}') == ({"topic": "x", "refund_cents": 5}, None)
    assert validate('{"topic": "x", "refund_cents": true}')[1] == "field refund_cents must be int"
    assert validate("nope")[1] == "the reply is not JSON"
    m = scripted(['{"topic": "billing", "refund_cents": "49"}', '{"topic": "billing", "refund_cents": 49}'])
    result = run_typed(m, TICKET)
    assert result == {"data": {"topic": "billing", "refund_cents": 49}, "attempts": 2} and "field refund_cents must be int" in m.seen[1]
    assert run_typed(scripted(["a", "b"]), TICKET)["attempts"] == 2
