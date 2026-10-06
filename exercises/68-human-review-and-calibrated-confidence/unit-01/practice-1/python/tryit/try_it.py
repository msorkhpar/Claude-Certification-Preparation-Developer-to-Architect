"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from review_routing import accuracy_by, calibrate_threshold, checkpoint, route

# Accuracy per document type and field, next to the overall figure.
records = [{"doc_type": "invoice", "field": "total", "correct": True}] * 90 + \
          [{"doc_type": "receipt", "field": "date", "correct": True}] * 8 + \
          [{"doc_type": "receipt", "field": "date", "correct": False}] * 2
for row in accuracy_by(records):
    print("accuracy:", row)

# The lowest confidence at which the model still reaches the target accuracy, from labelled outcomes.
labeled = [(95, True), (90, True), (85, True), (80, False), (60, False)]
print("threshold for 90% accuracy:", calibrate_threshold(labeled, 90))

# Which extractions a person reviews: conflicts and low confidence first, up to the capacity.
extractions = [{"id": "x1", "confidence": 95, "conflict": False}, {"id": "x2", "confidence": 60, "conflict": False},
               {"id": "x3", "confidence": 90, "conflict": True}, {"id": "x4", "confidence": 70, "conflict": False}]
print("routing:", route(extractions, 80, 2))
print("checkpoints:", checkpoint("send_payment", 5), checkpoint("update_note", 50))
