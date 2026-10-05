"""A two-step prompt chain with versioned templates, against a scripted model.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
"""
import logging
import re

from harness import scripted_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"

TEMPLATES = {
    "extract-quotes@2": {
        "system": "You answer questions about company documents and use only what the documents say.",
        "user": "{{documents}}\n\nFirst quote the passages that bear on the question, each in <quote> tags inside <quotes>. "
                "If nothing bears on it, write <quotes></quotes>.\n\n<question>{{question}}</question>",
    },
    "answer-from-quotes@1": {
        "system": "You answer from quoted evidence. If the quotes do not settle the question, say what is missing.",
        "user": "{{quotes}}\n\nAnswer the question in one or two sentences, and say which quote supports each claim.\n\n<question>{{question}}</question>",
    },
}

DOCUMENTS = [
    ("travel-policy.txt", "Flights above 400 dollars need approval from a manager before booking. Economy class is the default for flights under six hours."),
    ("expenses-faq.txt", "Meals are reimbursed up to 60 dollars a day when travelling. Receipts are required for every claim over 25 dollars."),
]
QUESTION = "Does a 450 dollar economy flight need approval?"


def render(template, **values):
    """Fill {{name}} placeholders in one pass; a value is data and is never read as a template."""
    missing = set(re.findall(r"{{(\w+)}}", template)) - set(values)
    if missing:
        raise KeyError(f"unfilled variables: {sorted(missing)}")
    return re.sub(r"{{(\w+)}}", lambda m: values[m.group(1)], template)


def documents_block(documents):
    body = "".join(f'<document index="{i}">\n<source>{name}</source>\n<document_content>\n{content}\n</document_content>\n</document>\n'
                   for i, (name, content) in enumerate(documents, start=1))
    return f"<documents>\n{body}</documents>"


def step(client, version, **values):
    template = TEMPLATES[version]
    return client.messages.create(model=MODEL, max_tokens=500, system=template["system"],
                                  messages=[{"role": "user", "content": render(template["user"], **values)}])


REPLIES = [
    message([text("<quotes>\n<quote>Flights above 400 dollars need approval from a manager before booking.</quote>\n</quotes>")], model=MODEL),
    message([text("Yes: at 450 dollars it is above the 400 dollar limit, so it needs a manager's approval first (quote 1).")], model=MODEL),
]


def main():
    client, transport = scripted_client(*REPLIES)
    first = step(client, "extract-quotes@2", documents=documents_block(DOCUMENTS), question=QUESTION)
    quotes = first.content[0].text
    second = step(client, "answer-from-quotes@1", quotes=quotes, question=QUESTION)
    one, two = (r["messages"][0]["content"] for r in transport.requests)
    print("templates used:", ["extract-quotes@2", "answer-from-quotes@1"])
    print("step 1: documents come before the question:", one.index("<documents>") < one.index("<question>"))
    print("step 1: the prompt ends with the question:", one.rstrip().endswith("</question>"))
    print("step 2: the full documents are not resent:", "<documents>" not in two, f"({len(one)} characters then {len(two)})")
    print("last message of each request is a user turn (no prefill):", [r["messages"][-1]["role"] for r in transport.requests])
    print("sampling parameters sent:", sorted(set(transport.requests[0]) & {"temperature", "top_p", "top_k"}) or "none")
    print("system prompts differ per step:", transport.requests[0]["system"] != transport.requests[1]["system"])
    print("data is not read as a template:", render(TEMPLATES["answer-from-quotes@1"]["user"], quotes="{{question}} stays", question="Q?").count("{{question}} stays") == 1)
    try:
        render(TEMPLATES["answer-from-quotes@1"]["user"], quotes=quotes)
    except KeyError as err:
        print("a missing variable is an error:", err.args[0])
    print("step 1 output:", quotes.replace("\n", " "))
    print("step 2 output:", second.content[0].text)


if __name__ == "__main__":
    main()
