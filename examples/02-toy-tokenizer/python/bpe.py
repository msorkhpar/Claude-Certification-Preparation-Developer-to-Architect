"""A toy byte-pair tokenizer. It is not Claude's tokenizer: it shows why tokens are not words."""
import logging
from collections import Counter

log = logging.getLogger(__name__)


def train(corpus, merges):
    """Learn merge rules: repeatedly join the most frequent adjacent pair (ties: first seen)."""
    words = [list(w) for w in corpus.split()]
    rules = []
    for _ in range(merges):
        pairs = Counter()
        for w in words:
            for a, b in zip(w, w[1:]):
                pairs[(a, b)] += 1
        if not pairs:
            break
        best = max(pairs, key=lambda p: (pairs[p], -list(pairs).index(p)))
        rules.append(best)
        words = [_merge(w, best) for w in words]
    return rules


def _merge(word, pair):
    out, i = [], 0
    while i < len(word):
        if i + 1 < len(word) and (word[i], word[i + 1]) == pair:
            out.append(word[i] + word[i + 1])
            i += 2
        else:
            out.append(word[i])
            i += 1
    return out


def encode(word, rules):
    pieces = list(word)
    for rule in rules:
        pieces = _merge(pieces, rule)
    return pieces


def main():
    corpus = "low low low lower lower lowest newest newest widest widest"
    rules = train(corpus, 6)
    print("merges:", " ".join("+".join(r) for r in rules))
    for word in ["low", "lowest", "newer", "widest", "lowish"]:
        print(f"{word:7} -> {' | '.join(encode(word, rules))}")


if __name__ == "__main__":
    main()
