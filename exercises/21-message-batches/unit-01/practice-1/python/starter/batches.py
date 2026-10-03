"""Build, split and read Message Batches. See ../../statement.md for the contract."""


class BatchError(Exception):
    """The batch would be refused. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def build_requests(items):
    # TODO: turn [{"id", "params"}] into [{"custom_id", "params"}], refusing what a batch refuses.
    return None


def split_batches(requests, max_requests=100_000, max_bytes=256 * 1024 * 1024):
    # TODO: cut the requests, in order, into batches that respect both limits.
    return None


def collect(requests, result_lines):
    # TODO: match the result lines to the requests by custom_id and sort out what to retry and what to fix.
    return None
