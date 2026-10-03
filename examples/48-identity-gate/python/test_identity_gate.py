from harness import scripted_client

from identity_gate import Backend, gate, replies, run


def test_the_gate_lets_only_verification_through_before_a_customer_is_verified():
    backend = Backend()
    assert gate(backend, "process_refund", {"order_id": "O1", "amount_cents": 1}) == ("BLOCKED identity_required: Verify the customer's identity before this action.", True)
    assert gate(backend, "lookup_order", {"order_id": "O1"})[1] is True and backend.log == []
    assert gate(backend, "verify_identity", {"code": "1234"}) == ("verified=yes", False)
    assert gate(backend, "lookup_order", {"order_id": "O1"}) == ("order_id=O1; total_cents=5000", False)


def test_a_wrong_code_unlocks_nothing():
    backend = Backend()
    gate(backend, "verify_identity", {"code": "0000"})
    assert gate(backend, "process_refund", {"order_id": "O1", "amount_cents": 1})[1] is True and backend.log == ["verify_identity"]


def test_the_ungated_loop_refunds_before_any_verification_and_the_gated_one_does_not():
    plain = Backend()
    run(scripted_client(*replies(False))[0], plain, False)
    assert plain.log == ["process_refund"]
    gated = Backend()
    run(scripted_client(*replies(True))[0], gated, True)
    assert gated.log == ["verify_identity", "lookup_order", "process_refund"]
