"""Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract."""


class RejectedRequest(Exception):
    """The API would answer 400. `param` names the offending parameter."""

    def __init__(self, param, reason):
        super().__init__(f"{param}: {reason}")
        self.param, self.reason = param, reason


def build_params(model, max_tokens, options=None):
    # TODO: return the request parameters for the model, or raise RejectedRequest for a request the API would refuse.
    return None
