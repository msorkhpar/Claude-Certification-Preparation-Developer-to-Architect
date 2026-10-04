from escalation_rules import CASES, ask_for_identifier, by_criteria, by_sentiment, errors, escalation_section, pick_most_recent


def test_the_criteria_route_every_case_as_a_careful_person_would_and_the_sentiment_rule_does_not():
    assert errors(by_criteria) == [] and errors(by_sentiment) == [1, 2, 3, 4, 5]
    assert len(CASES) == 6


def test_the_recency_heuristic_picks_one_account_and_the_question_picks_none():
    matches = [{"id": "c1", "last_order": 1}, {"id": "c2", "last_order": 2}]
    assert pick_most_recent(matches) == "c2"
    assert ask_for_identifier(matches, ["the email", "the postcode"]) == "I found 2 accounts for that name. Please give me one of: the email, the postcode."


def test_the_prompt_section_lists_the_criteria_and_the_examples_with_their_reasons():
    text = escalation_section(["a request for a person"], [("Hi", "resolve", "simple")])
    assert text == 'Escalate to a person when:\n- a request for a person\n\nExamples:\nCustomer: "Hi" -> resolve (simple)'
