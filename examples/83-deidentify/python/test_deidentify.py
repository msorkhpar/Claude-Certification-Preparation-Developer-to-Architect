from deidentify import audit_entry, restore, tokenise


def test_the_same_value_gets_the_same_token_and_each_kind_is_counted_on_its_own():
    vault = {}
    out = tokenise("a@x.io b@x.io a@x.io M-123456", vault)
    assert out == "<EMAIL_1> <EMAIL_2> <EMAIL_1> <MEMBER_1>"
    assert len(vault) == 3


def test_restore_puts_every_value_back():
    vault = {}
    original = "write to a@x.io about M-123456"
    assert restore(tokenise(original, vault), vault) == original


def test_text_without_identifiers_is_unchanged_and_the_vault_stays_empty():
    vault = {}
    assert tokenise("nothing to find", vault) == "nothing to find" and vault == {}


def test_a_name_is_not_found_by_these_patterns():
    assert tokenise("Jane Doe", {}) == "Jane Doe"


def test_the_audit_entry_holds_sizes_and_counts_only():
    vault = {}
    original = "a@x.io"
    tokenise(original, vault)
    entry = audit_entry("r1", original, vault)
    assert entry == {"request_id": "r1", "chars": 6, "tokens_issued": 1, "prompt_stored": False}
    assert "a@x.io" not in str(entry)
