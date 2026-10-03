from batch_round_trip import MODEL, SCRIPT, TICKETS
from harness import scripted_client


def test_create_posts_one_request_per_ticket_with_its_custom_id():
    client, transport = scripted_client(*SCRIPT)
    requests = [{"custom_id": cid, "params": {"model": MODEL, "max_tokens": 50, "messages": [{"role": "user", "content": body}]}} for cid, body in TICKETS.items()]
    client.messages.batches.create(requests=requests)
    assert transport.urls[0].endswith("/v1/messages/batches")
    assert [r["custom_id"] for r in transport.requests[0]["requests"]] == list(TICKETS)


def test_results_come_back_in_a_different_order_than_the_requests():
    client, _ = scripted_client(*SCRIPT[3:])
    order = [item.custom_id for item in client.messages.batches.results("msgbatch_illustrative")]
    assert order == ["t-3", "t-1", "t-4", "t-2"] and order != list(TICKETS)


def test_each_non_success_result_has_its_own_type():
    client, _ = scripted_client(*SCRIPT[3:])
    kinds = {item.custom_id: item.result.type for item in client.messages.batches.results("msgbatch_illustrative")}
    assert kinds == {"t-3": "succeeded", "t-1": "succeeded", "t-4": "errored", "t-2": "expired"}
