from prompt_budget import MIN_CACHEABLE, MODULES, assemble, cached_prefix, tokens

VARS = {"customer": "Ana", "tier": "gold", "question": "Q"}


def test_tokens_are_the_ceiling_of_characters_over_four():
    assert [tokens(t) for t in ("", "a", "abcd", "abcde")] == [0, 1, 1, 2]


def test_static_modules_come_first_and_the_breakpoint_follows_the_last_one():
    swapped = [MODULES[2], MODULES[0], MODULES[3], MODULES[1]]
    prompt = assemble(swapped, VARS)
    assert [b["name"] for b in prompt["blocks"]] == ["role", "policy", "customer", "question"]
    assert prompt["blocks"][prompt["breakpoint"]]["name"] == "policy"


def test_dynamic_variables_are_filled_and_static_text_is_left_alone():
    prompt = assemble(MODULES, VARS)
    assert prompt["blocks"][2]["text"] == "Customer: Ana. Tier: gold."
    assert "{" not in prompt["blocks"][1]["text"]


def test_a_prefix_under_the_minimum_gets_no_breakpoint():
    prompt = assemble(MODULES[:1] + MODULES[2:], VARS)
    assert prompt["prefix_tokens"] < MIN_CACHEABLE and prompt["breakpoint"] is None


def test_the_prefix_survives_a_dynamic_change_and_breaks_on_a_static_one():
    base = assemble(MODULES, VARS)
    other = assemble(MODULES, {"customer": "Ben", "tier": "basic", "question": "Other"})
    assert cached_prefix(base) == cached_prefix(other) != ""
    edited = [dict(m, text=m["text"] + " Extra.") if m["name"] == "role" else m for m in MODULES]
    assert cached_prefix(assemble(edited, VARS)) != cached_prefix(base)
