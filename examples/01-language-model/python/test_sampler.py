import math

from sampler import Lcg, greedy, sample, softmax, LOGITS


def test_probabilities_sum_to_one_at_any_temperature():
    for t in (0.1, 1.0, 5.0):
        assert math.isclose(sum(softmax(LOGITS, t)), 1.0)


def test_low_temperature_sharpens_and_high_flattens():
    low, mid, high = (softmax(LOGITS, t)[0] for t in (0.5, 1.0, 2.0))
    assert low > mid > high


def test_greedy_ignores_temperature():
    assert greedy(softmax(LOGITS, 0.5)) == greedy(softmax(LOGITS, 2.0)) == 0


def test_same_seed_same_draws_different_seed_may_differ():
    probs = softmax(LOGITS, 2.0)
    a = [sample(probs, r) for r in [Lcg(7)] for _ in range(10)]
    b = [sample(probs, r) for r in [Lcg(7)] for _ in range(10)]
    assert a == b
    c = [sample(probs, r) for r in [Lcg(8)] for _ in range(10)]
    assert a != c
