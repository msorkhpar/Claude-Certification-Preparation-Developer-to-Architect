"""An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md."""

CURRENCIES = ("USD", "EUR", "GBP", "other", "unclear")
KEYS = ("vendor", "currency", "currency_detail", "line_items", "stated_total", "calculated_total", "conflict_detected", "provenance")
RETRYABLE = ("syntax", "semantic", "ungrounded")
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}  # models whose API rejects tool_choice any and tool, as read on 2026-10-03


def _number(value):
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _quote_found(quote, document):
    return isinstance(quote, str) and quote != "" and quote in document


def _currency_ok(currency):
    return currency in CURRENCIES


def _detail_missing(currency, detail):
    return currency == "other" and not (isinstance(detail, str) and detail.strip())


def _check_semantics(record, err):
    items = record["line_items"]
    if abs(sum(items) - record["calculated_total"]) > 0.005:
        err("semantic", "calculated_total", f"{record['calculated_total']} is not the sum of the line items, {sum(items)}")
    stated = record["stated_total"]
    if stated is not None and abs(stated - record["calculated_total"]) > 0.005 and not record["conflict_detected"]:
        err("semantic", "stated_total", f"the stated total {stated} differs from the calculated total {record['calculated_total']} but conflict_detected is false")


def validate(record, document, required=()):
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
    return [e for e in errors if e["kind"] in RETRYABLE]


def _status(errors, record):
    if errors:
        status = "needs_review" if all(e["kind"] == "absent" for e in errors) else "failed"
    elif record["conflict_detected"]:
        status = "needs_review"
    else:
        status = "valid"
    return status


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
    return value is None or value == "unclear"


def _is_conflict(current, value, field, conflicts):
    return current != value and field not in conflicts


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
    return {"all_documents": round(correct / total, 2) if total else 0.0, "validated_only": round(correct / valid, 2) if valid else 0.0, "validated": valid, "total": total}


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
    if forced is not None:
        return {"tool_choice": {"type": "tool", "name": forced}, "strict": True, "verify_reply": False}
    if len(tools) > 1:
        return {"tool_choice": {"type": "any"}, "strict": True, "verify_reply": False}
    return {"tool_choice": {"type": "tool", "name": tools[0]}, "strict": True, "verify_reply": False}


def request_choice(model, tools, forced=None):
    if model in NO_FORCING:
        return {"tool_choice": {"type": "auto"}, "strict": True, "verify_reply": True}
    return _forced_choice(tools, forced)
