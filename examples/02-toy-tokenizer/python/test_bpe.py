from bpe import encode, train

CORPUS = "low low low lower lower lowest newest newest widest widest"


def test_a_frequent_word_becomes_one_token_and_a_rare_one_splits():
    rules = train(CORPUS, 6)
    assert encode("low", rules) == ["low"]
    assert len(encode("lowish", rules)) > 1


def test_unseen_word_still_encodes_with_known_pieces():
    rules = train(CORPUS, 6)
    assert "".join(encode("newer", rules)) == "newer"


def test_no_merges_means_one_token_per_character():
    assert encode("low", train(CORPUS, 0)) == ["l", "o", "w"]
