"""What a schema does not give an extraction pipeline: a field the document may lack, checks of meaning, a retry that carries feedback, and an accuracy figure that does not hide the failures.

The rules are the exam guide's for tasks 4.3 and 4.4 and the Claude documentation read on 2026-10-03 (structured outputs, "Define tools"): a schema guarantees syntax and not meaning; a field that may be missing from the source is
nullable so the model is not pushed to invent a value; a retry helps with format and structure and cannot supply what the source does not hold; a request that forces a tool is rejected by the current models, which use
`auto` with strict tool use. The "model" below is a script of fixed replies: it shows the pipeline's decisions, not what a real model would answer.
"""
import json

DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 130.00 EUR"
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}


def scripted_value(document, nullable):
    """What a model under pressure does with a purchase order number the document does not contain: a required string gets filled, a nullable one stays null."""
    marker = "PO "
    if marker in document:
        return document.split(marker)[1].split()[0]
    return None if nullable else "PO-0000"


def check(record, document):
    """Checks a schema cannot make: the items add up to the total, and the quoted evidence is in the document."""
    problems = []
    if abs(sum(record["items"]) - record["total"]) > 0.005:
        problems.append(f"total: the items add up to {sum(record['items'])}, not {record['total']}")
    if record["evidence"] not in document:
        problems.append("evidence: this quotation is not in the document")
    return problems


def extract(document, replies, max_retries=1):
    """Ask, check, and ask again with the document, the failed answer and the problems; give up after max_retries."""
    messages = []
    for attempt, reply in enumerate(replies[: max_retries + 1], 1):
        problems = check(reply, document)
        if not problems:
            return {"status": "valid", "attempts": attempt, "feedback": messages}
        messages.append(f"Document:\n{document}\nYour answer:\n{json.dumps(reply)}\nProblems:\n" + "\n".join(f"- {p}" for p in problems))
    return {"status": "failed", "attempts": min(len(replies), max_retries + 1), "feedback": messages}


def accuracy(outcomes):
    """outcomes: (status, correct) per document. The figure on validated records alone leaves out every document that failed."""
    valid = [correct for status, correct in outcomes if status == "valid"]
    return {"validated_only": round(sum(valid) / len(valid), 2) if valid else 0.0, "all_documents": round(sum(valid) / len(outcomes), 2) if outcomes else 0.0}


def request_choice(model, tools):
    """The tool_choice of an extraction request: any when several schemas fit, the one tool otherwise, auto with strict tool use where forcing is rejected."""
    if model in NO_FORCING:
        return {"tool_choice": "auto", "check_reply": True}
    return {"tool_choice": "any" if len(tools) > 1 else f"tool:{tools[0]}", "check_reply": False}


def main():
    no_po = "Invoice from Acme Tools. Total due: 130.00 EUR"
    print("purchase order, document without one: required ->", scripted_value(no_po, False), "| nullable ->", scripted_value(no_po, True))
    wrong = {"items": [100.0, 20.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
    right = {"items": [100.0, 20.5, 9.5], "total": 130.0, "evidence": "Total due: 130.00 EUR"}
    fabricated = {**right, "evidence": "Total due: 130.00 USD"}
    result = extract(DOC, [wrong, right])
    print("answer 1 wrong, answer 2 right:", result["status"], "after", result["attempts"], "attempts")
    print(result["feedback"][0])
    print("two answers that stay wrong:", extract(DOC, [wrong, fabricated])["status"])
    print("accuracy of 10 documents (6 valid, 5 of them right):", accuracy([("valid", True)] * 5 + [("valid", False)] + [("failed", False)] * 4))
    for model in ("claude-haiku-4-5", "claude-sonnet-5-5"):
        print(f"{model}, two extraction tools:", request_choice(model, ["extract_invoice", "extract_receipt"]))


if __name__ == "__main__":
    main()
