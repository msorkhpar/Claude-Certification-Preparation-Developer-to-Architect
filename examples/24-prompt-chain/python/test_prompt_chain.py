import pytest

from harness import scripted_client
from prompt_chain import DOCUMENTS, QUESTION, REPLIES, TEMPLATES, documents_block, render, step


def test_a_missing_variable_is_an_error_not_a_blank():
    with pytest.raises(KeyError):
        render("Hello {{name}} and {{other}}", name="x")


def test_a_value_that_looks_like_a_placeholder_is_left_alone():
    assert render("Q: {{a}} / {{b}}", a="{{b}}", b="real") == "Q: {{b}} / real"


def test_documents_are_numbered_and_carry_their_source():
    block = documents_block(DOCUMENTS)
    assert block.startswith("<documents>") and '<document index="2">' in block and "<source>expenses-faq.txt</source>" in block


def test_the_long_documents_come_first_and_the_question_last():
    client, transport = scripted_client(*REPLIES[:1])
    step(client, "extract-quotes@2", documents=documents_block(DOCUMENTS), question=QUESTION)
    prompt = transport.requests[0]["messages"][0]["content"]
    assert prompt.index("<documents>") < prompt.index("<question>") and prompt.rstrip().endswith("</question>")


def test_each_step_sends_its_own_system_prompt_and_ends_on_a_user_turn():
    client, transport = scripted_client(*REPLIES)
    step(client, "extract-quotes@2", documents=documents_block(DOCUMENTS), question=QUESTION)
    step(client, "answer-from-quotes@1", quotes="<quotes></quotes>", question=QUESTION)
    assert [r["system"] for r in transport.requests] == [TEMPLATES["extract-quotes@2"]["system"], TEMPLATES["answer-from-quotes@1"]["system"]]
    assert all(r["messages"][-1]["role"] == "user" for r in transport.requests)
    assert not ({"temperature", "top_p", "top_k"} & set(transport.requests[0]))
