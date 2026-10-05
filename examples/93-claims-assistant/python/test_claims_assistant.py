from claims_assistant import *

WATER = "Water damage is covered up to 5,000 per claim."


def request(text, allowed=("policy",), consequence="low", quote=WATER, confidence=97):
    return Request("r", text, set(allowed), consequence, quote, confidence)


def test_identifiers_become_tokens_and_the_same_value_gets_the_same_token():
    sent, vault = tokenise("write to a@example.com or a@example.com or b@example.org")
    assert sent == "write to <EMAIL_1> or <EMAIL_1> or <EMAIL_2>"
    assert vault["<EMAIL_1>"] == "a@example.com" and "@" not in sent


def test_the_reader_s_rights_come_before_the_ranking():
    assert retrieve("partner commission premiums", {"contracts"}, INDEX).id == "contract-9"
    assert retrieve("partner commission premiums", {"policy"}, INDEX) is None


def test_a_tie_goes_to_the_smaller_id_and_no_overlap_is_no_evidence():
    assert retrieve("water damage", {"policy"}, STALE_INDEX).id == "policy-2-old"
    assert retrieve("water damage", {"policy"}, INDEX).id == "policy-2"
    assert retrieve("zzzz yyyy", {"policy"}, INDEX) is None


def test_a_stale_or_unsupported_answer_is_held_and_confidence_decides_the_rest():
    q = "How much does the policy cover for water damage?"
    assert handle(request(q), STALE_INDEX)[1]["outcome"] == "hold: stale evidence (policy-2-old v2, current v3)"
    assert handle(request(q, quote="Water damage is covered up to 8,000 per claim."), INDEX)[1]["outcome"] == "hold: unsupported"
    assert handle(request(q, confidence=95), INDEX)[1]["outcome"] == "auto"
    assert handle(request(q, confidence=94), INDEX)[1]["outcome"] == "review"
    assert handle(request(q, consequence="high"), INDEX)[1]["outcome"] == "human"


def test_the_trace_holds_ids_and_sizes_and_no_text():
    sent, trace = handle(request("Claims reported from jo@example.com, how many days?", quote="x"), INDEX)
    assert "jo@example.com" not in str(trace) and "@" not in sent
    assert set(trace) == {"request", "chunk", "outcome", "chars"}


def test_a_gate_protects_the_costly_segment_even_when_gains_cover_the_losses():
    cases = [Case("a", "refund", True, False), Case("b", "status", False, True)]
    assert release(cases, {"refund"}) == "no-go: protected segment lost answers: refund"
    assert release(cases, set()) == "go: lost 1, gained 1"
    assert release([Case("a", "x", True, False)], set()) == "no-go: net loss: lost 1, gained 0"
