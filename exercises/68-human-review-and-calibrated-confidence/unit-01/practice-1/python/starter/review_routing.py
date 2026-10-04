"""Human review without fooling yourself: accuracy by segment, the decision to automate, a calibrated confidence threshold, a stratified sample, review routing within capacity, and checkpoints for irreversible actions. See ../../statement.md."""

IRREVERSIBLE = ("delete_records", "send_payment", "close_account")


def accuracy_by(records):
    # TODO: the overall accuracy and the accuracy of every doc_type/field segment, as dicts {segment, correct, total, percent}.
    return None


def can_automate(records, threshold, min_n):
    # TODO: {"automate", "failing", "undersampled"}: every segment must have enough records and reach the threshold.
    return None


def calibrate_threshold(labeled, target):
    # TODO: the lowest confidence whose auto-accepted items (confidence at or above it) reach the target precision, or None.
    return None


def stratified_sample(items, per_stratum):
    # TODO: the ids of the lowest-ranked items of every stratum.
    return None


def route(extractions, threshold, capacity):
    # TODO: {"review", "backlog", "auto"}: low confidence and conflicts go to review, the weakest first, within the capacity.
    return None


def checkpoint(action, amount, limit=1000):
    # TODO: "human" or "auto" for an action.
    return None
