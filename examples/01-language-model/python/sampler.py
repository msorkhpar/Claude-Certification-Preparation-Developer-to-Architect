"""A toy next-token sampler. It is not Claude: it only shows what temperature does."""
import math

TOKENS = ["blue", " clear", " falling", "green"]
LOGITS = [4.0, 2.5, 1.0, -1.0]


def softmax(logits, temperature):
    """Turn scores into probabilities. Lower temperature sharpens, higher flattens."""
    scaled = [x / temperature for x in logits]
    top = max(scaled)
    exps = [math.exp(x - top) for x in scaled]
    total = sum(exps)
    return [e / total for e in exps]


class Lcg:
    """A tiny seeded random generator, the same in every language of this course."""

    def __init__(self, seed):
        self.state = seed % 2**32

    def next(self):
        self.state = (self.state * 1664525 + 1013904223) % 2**32
        return self.state / 2**32


def sample(probs, rng):
    u = rng.next()
    acc = 0.0
    for i, p in enumerate(probs):
        acc += p
        if u < acc:
            return i
    return len(probs) - 1


def greedy(probs):
    return max(range(len(probs)), key=lambda i: probs[i])


def main():
    for t in (0.5, 1.0, 2.0):
        probs = softmax(LOGITS, t)
        print(f"T={t}: " + "  ".join(f"{tok.strip()}={p:.3f}" for tok, p in zip(TOKENS, probs)))
    print("greedy:", TOKENS[greedy(softmax(LOGITS, 1.0))])
    for t in (0.2, 1.0, 2.0):
        probs = softmax(LOGITS, t)
        rng = Lcg(7)
        picks = [TOKENS[sample(probs, rng)].strip() for _ in range(10)]
        print(f"T={t} ten draws:", " ".join(picks))


if __name__ == "__main__":
    main()
