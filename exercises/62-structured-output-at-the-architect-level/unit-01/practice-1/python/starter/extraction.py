"""An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)
CURRENCIES = ("USD", "EUR", "GBP", "other", "unclear")
KEYS = ("vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance")
RETRYABLE = ("syntax", "semantic", "ungrounded")
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}  # models whose API rejects tool_choice any and tool, as read on 2026-10-03


def _number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _quote_found(quote, document):
    """TODO 1 of 10 (finish this to pass e1): is a provenance quote real?

    Receives the quote the model gave for a field and the document text. Returns True only when the quote is a non-empty string that
    appears in the document, so an invented value cannot be supported by an invented quote.
    Example: _quote_found("Acme Ltd", "Invoice from Acme Ltd") -> True, _quote_found("Zed Corp", "Invoice from Acme Ltd") -> False
    """
    return True


def _currency_ok(currency):
    """TODO 2 of 10 (finish this to pass e6): is this a currency the schema allows?

    Receives the value of `currency`. Returns True when it is one of CURRENCIES (`unclear` and `other` count, anything else does not).
    Example: _currency_ok("unclear") -> True, _currency_ok("dollars") -> False
    """
    return True


def _detail_missing(currency, detail):
    """TODO 3 of 10 (finish this to pass e6): is a required currency detail missing?

    Receives `currency` and `currency_detail`. Returns True when the currency is `other` and the detail is not a non-blank string.
    Example: _detail_missing("other", "  ") -> True, _detail_missing("other", "CHF") -> False, _detail_missing("USD", None) -> False
    """
    return False


def _check_semantics(record, err):
    """TODO 10 of 10 (finish this to pass e5): report what a schema cannot check about the numbers.

    Receives a record whose types are already valid and `err(kind, field, message)`, which records one error. Call `err("semantic",
    "calculated_total", ...)` when `calculated_total` is not the sum of `line_items` (to half a cent), and `err("semantic",
    "stated_total", ...)` when `stated_total` is not None, differs from `calculated_total` (by more than half a cent) and
    `conflict_detected` is False; a conflict the model flagged is information, not an error. It returns nothing.
    Example: line_items [10, 5], calculated_total 15, stated_total 20, conflict_detected False -> one semantic error on stated_total
    """


def validate(record, document, required=()):
    log.debug("validate input: %r", record)
    errors = []

    def err(kind, field, message):
        errors.append({"kind": kind, "field": field, "message": message})

    if not isinstance(record, dict):
        return [{"kind": "syntax", "field": "$", "message": "the record is not an object"}]
    for key in KEYS:
        if key not in record:
            err("syntax", key, "is missing")
    if errors:
        return errors
    if record["vendor"] is not None and not isinstance(record["vendor"], str):
        err("syntax", "vendor", "must be a string or null")
    if not _currency_ok(record["currency"]):
        err("syntax", "currency", f"{record['currency']!r} is not one of {list(CURRENCIES)}")
    detail = record["currency_detail"]
    if _detail_missing(record["currency"], detail):
        err("syntax", "currency_detail", "is required when the currency is other")
    items = record["line_items"]
    if not (isinstance(items, list) and all(_number(i) for i in items)):
        err("syntax", "line_items", "must be a list of numbers")
    if record["stated_total"] is not None and not _number(record["stated_total"]):
        err("syntax", "stated_total", "must be a number or null")
    if not _number(record["calculated_total"]):
        err("syntax", "calculated_total", "must be a number")
    if not isinstance(record["conflict_detected"], bool):
        err("syntax", "conflict_detected", "must be true or false")
    if not isinstance(record["provenance"], dict):
        err("syntax", "provenance", "must be an object")
    if errors:
        return errors
    _check_semantics(record, err)
    for field in ("vendor", "currency", "stated_total"):
        value = record[field]
        if value is None or value == "unclear":
            continue
        quote = record["provenance"].get(field)
        if not _quote_found(quote, document):
            err("ungrounded", field, f"{field} has no quote that appears in the document")
    for field in required:
        if record.get(field) is None or record.get(field) == "unclear":
            err("absent", field, "the document gave no value")
    return errors


def _retryable(errors):
    """TODO 4 of 10 (finish this to pass e2, e3 and e4): keep the errors a second look can fix.

    Receives a list of {"kind", "field", "message"} errors. Returns those whose kind is in RETRYABLE (syntax, semantic, ungrounded),
    in order; an `absent` error is never retried. Example: [{"kind": "absent", ...}, {"kind": "syntax", ...}] -> [{"kind": "syntax", ...}]
    """
    return []


def _status(errors, record):
    """TODO 5 of 10 (finish this to pass m1, e3 and e5): the status of a finished extraction.

    Receives the errors left after the last attempt and the last record. Returns `needs_review` when the only errors are `absent` ones,
    or when there are no errors and the model flagged a conflict (`conflict_detected`); `failed` for any other error; `valid` otherwise.
    Example: no errors and conflict_detected false -> "valid"; one `absent` error -> "needs_review"; one `syntax` error -> "failed"
    """
    return "failed"


def extract_document(document, call_model, required=(), max_retries=2):
    feedback = None
    attempts = 0
    while True:
        attempts += 1
        record = call_model(document, feedback)
        errors = validate(record, document, required)
        if not errors:
            break
        retryable = _retryable(errors)
        if not retryable:
            break
        if attempts > max_retries:
            break
        feedback = {"previous": record, "errors": retryable}
    status = _status(errors, record)
    return {"status": status, "record": record, "attempts": attempts, "errors": errors}


def _is_unset(value):
    """TODO 6 of 10 (finish this to pass e7): has a merged field no real value yet?

    Receives a value. Returns True for None and for "unclear", so the first real value from a later chunk is kept.
    Example: _is_unset(None) -> True, _is_unset("unclear") -> True, _is_unset("Acme Ltd") -> False
    """
    return False


def _is_conflict(current, value, field, conflicts):
    """TODO 7 of 10 (finish this to pass e7): do two chunks disagree about a field, not yet recorded?

    Receives the value kept so far, a later chunk's value, the field name and the list of fields already in `conflicts`. Returns True when
    the values differ and the field is not yet in that list, so a conflict is recorded once. Example: ("A", "B", "vendor", []) -> True
    """
    return False


def merge_chunks(records):
    merged = {"vendor": None, "currency": "unclear", "currency_detail": None, "line_items": [], "stated_total": None, "calculated_total": 0, "conflict_detected": False, "provenance": {}, "conflicts": []}
    for record in records:
        for field in ("vendor", "currency", "stated_total"):
            value = record.get(field)
            if value is None or value == "unclear":
                continue
            current = merged[field]
            if _is_unset(current):
                merged[field] = value
                merged["provenance"][field] = record["provenance"].get(field)
                if field == "currency":
                    merged["currency_detail"] = record.get("currency_detail")
            elif _is_conflict(current, value, field, merged["conflicts"]):
                merged["conflicts"].append(field)
        merged["line_items"] += record.get("line_items", [])
        if record.get("conflict_detected"):
            merged["conflict_detected"] = True
    merged["calculated_total"] = round(sum(merged["line_items"]), 2)
    if merged["conflicts"]:
        merged["conflict_detected"] = True
    return merged


def _report(correct, valid, total):
    """TODO 8 of 10 (finish this to pass e8): the accuracy report, on every document and on the validated ones.

    Receives the number of correct documents, the number of valid documents and the number of labelled documents. Returns
    {"all_documents", "validated_only", "validated", "total"}: correct over total and correct over valid, each rounded to two decimals
    and 0.0 when the denominator is zero; `validated` is `valid`. Example: _report(3, 6, 6)["all_documents"] -> 0.5, ["validated_only"] -> 0.5
    """
    return {"all_documents": 0.0, "validated_only": 0.0, "validated": valid, "total": total}


def accuracy(results, labels):
    valid = correct = 0
    for doc_id, label in labels.items():
        result = results.get(doc_id)
        is_valid = result is not None and result["status"] == "valid"
        valid += 1 if is_valid else 0
        if is_valid and result["record"]["vendor"] == label["vendor"] and result["record"]["stated_total"] == label["stated_total"]:
            correct += 1
    total = len(labels)
    return _report(correct, valid, total)


def _forced_choice(tools, forced):
    """TODO 9 of 10 (finish this to pass e9): the tool_choice for a model that accepts a forced choice.

    Receives the list of tool names and the name to force, or None. Returns {"tool_choice", "strict", "verify_reply"} with strict True and
    verify_reply False: the forced tool when `forced` is given (`{"type": "tool", "name": forced}`), `{"type": "any"}` when there are several
    tools, otherwise the one tool by name. Example: (["a", "b"], None) -> {"tool_choice": {"type": "any"}, "strict": True, "verify_reply": False}
    """
    return {"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": False}


def request_choice(model, tools, forced=None):
    if model in NO_FORCING:
        return {"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": True}
    return _forced_choice(tools, forced)
