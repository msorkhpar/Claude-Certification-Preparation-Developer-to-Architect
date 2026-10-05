"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from extraction import extract_document, validate

DOC = "Invoice from Acme Tools.\nItems: 100.00 + 20.50\nTotal due: 120.50 EUR\nThank you."
GOOD = {"vendor": "Acme Tools", "currency": "EUR", "currency_detail": None, "line_items": [100.0, 20.5], "stated_total": 120.5,
        "calculated_total": 120.5, "conflict_detected": False,
        "provenance": {"vendor": "Invoice from Acme Tools", "currency": "120.50 EUR", "stated_total": "Total due: 120.50 EUR"}}


def call_model(document, feedback):
    """A stand-in for the model, like the tests use: it always answers with the same record."""
    print("model called, feedback:", feedback)
    return dict(GOOD)


result = extract_document(DOC, call_model)
print("status:", result["status"] if result else result)
print("attempts:", result["attempts"] if result else result)

# validate() on its own: a record whose calculated total disagrees with its line items.
bad = {**GOOD, "calculated_total": 99.0}
print("errors:", validate(bad, DOC))
