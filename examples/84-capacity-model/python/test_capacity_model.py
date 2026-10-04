from capacity_model import CACHED, TIERS, UNCACHED, dollars, monthly_cents, required_capacity, smallest_tier


def test_cache_reads_do_not_count_toward_the_input_limit():
    need = required_capacity(CACHED, 0)
    assert need == {"rpm": 800, "itpm": 800 * 1700, "otpm": 320_000}


def test_headroom_rounds_every_figure_up():
    assert required_capacity({"rpm": 1, "input": 1, "cache_write": 0, "cache_read": 0, "output": 1}, 30) == {"rpm": 2, "itpm": 2, "otpm": 2}
    assert required_capacity(CACHED, 30) == {"rpm": 1040, "itpm": 1_768_000, "otpm": 416_000}


def test_the_tier_must_cover_all_three_limits_and_custom_is_the_last_resort():
    assert smallest_tier({"rpm": 100, "itpm": 100, "otpm": 100}, TIERS) == "Start"
    assert smallest_tier({"rpm": 100, "itpm": 100, "otpm": 400_001}, TIERS) == "Build"
    assert smallest_tier({"rpm": 10_001, "itpm": 1, "otpm": 1}, TIERS) == "Custom"
    assert smallest_tier(required_capacity(UNCACHED, 30), TIERS) == "Scale"


def test_the_monthly_bill_counts_every_token_kind_and_batch_is_half_price():
    assert monthly_cents(CACHED, 2_000_000, 0) == 1_740_000
    assert monthly_cents(UNCACHED, 2_000_000, 0) == 3_880_000
    assert monthly_cents(CACHED, 2_000_000, 100) == 870_000
    assert monthly_cents(CACHED, 2_000_000, 30) == 1_479_000


def test_dollars_prints_cents_with_a_thousands_separator():
    assert (dollars(1_740_000), dollars(5), dollars(100)) == ("$17,400.00", "$0.05", "$1.00")
