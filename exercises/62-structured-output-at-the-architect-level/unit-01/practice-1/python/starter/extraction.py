"""An extraction pipeline that admits absence, checks what a schema cannot, retries with feedback and is measured on every document. See ../../statement.md."""

CURRENCIES = ("USD", "EUR", "GBP", "other", "unclear")
NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}  # models whose API rejects tool_choice any and tool, as read on 2026-10-03


def validate(record, document, required=()):
    # TODO: the errors of a record, each {"kind", "field", "message"}: syntax, semantic, ungrounded and absent.
    return None


def extract_document(document, call_model, required=(), max_retries=2):
    # TODO: call the model, validate, retry with feedback only for errors a second look can fix, and report a status.
    return None


def merge_chunks(records):
    # TODO: merge the records of the chunks of one long document, keeping the first value and recording a conflict.
    return None


def accuracy(results, labels):
    # TODO: the share of documents extracted correctly, measured on all of them and on the validated ones only.
    return None


def request_choice(model, tools, forced=None):
    # TODO: the tool_choice of the request, with the fallback for models that reject a forced choice.
    return None
