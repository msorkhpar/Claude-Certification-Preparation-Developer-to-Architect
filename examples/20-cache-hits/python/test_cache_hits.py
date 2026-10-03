from cache_hits import CacheSim, POLICY, run, stable, stamp_first, stamp_last, tokens


def usage(sim, body, wait=0):
    sim.clock += wait
    from harness import scripted_client
    client, _ = scripted_client(sim)
    return client.messages.create(**body).usage


def test_the_first_call_writes_and_the_second_reads_the_same_prefix():
    sim = CacheSim()
    first = usage(sim, stable("a"))
    second = usage(sim, stable("b"), wait=60)
    assert first.cache_creation_input_tokens > 500 and first.cache_read_input_tokens == 0
    assert second.cache_read_input_tokens == first.cache_creation_input_tokens and second.cache_creation_input_tokens == 0
    assert tokens(POLICY) == first.cache_creation_input_tokens


def test_a_hit_refreshes_the_five_minute_lifetime_and_silence_loses_it():
    sim = CacheSim()
    usage(sim, stable("a"))
    assert usage(sim, stable("b"), wait=250).cache_read_input_tokens > 0
    assert usage(sim, stable("c"), wait=250).cache_read_input_tokens > 0  # 500 s after the write, 250 after the last hit
    assert usage(sim, stable("d"), wait=400).cache_read_input_tokens == 0


def test_a_changing_block_before_the_breakpoint_defeats_the_cache():
    sim = CacheSim()
    usage(sim, stamp_first("10:01", "q"))
    assert usage(sim, stamp_first("10:02", "q"), wait=60).cache_read_input_tokens == 0


def test_the_same_block_after_the_breakpoint_costs_nothing():
    sim = CacheSim()
    usage(sim, stamp_last("10:01", "q"))
    assert usage(sim, stamp_last("10:02", "q"), wait=60).cache_read_input_tokens > 500


def test_a_prefix_under_the_minimum_is_not_cached_and_no_error_is_raised():
    sim = CacheSim(minimum=100000)
    first = usage(sim, stable("a"))
    assert first.cache_creation_input_tokens == 0 and first.input_tokens > 500
