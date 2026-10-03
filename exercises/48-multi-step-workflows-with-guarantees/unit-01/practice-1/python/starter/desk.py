"""A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md."""


class RefundDesk:
    def __init__(self, backend, limit_cents=10000):
        self.backend, self.limit = backend, limit_cents
        # TODO: keep what the desk has verified, looked up, refunded and blocked.

    def state(self):
        # TODO: a snapshot of the desk's state, as described in the statement.
        return None

    def call(self, name, args):
        # TODO: check the prerequisites in code, then call the backend, and return {"content", "is_error", "blocked"}.
        return None

    def handoff(self, reason):
        # TODO: the structured hand-off a person needs to take the case over.
        return None
