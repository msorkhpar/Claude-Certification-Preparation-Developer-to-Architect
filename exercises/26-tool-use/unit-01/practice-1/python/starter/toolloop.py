"""A tool loop against a scripted model. See ../../statement.md."""


class ToolError(Exception):
    """A tool refuses or fails; its message goes back to the model as an error result."""


class RequestError(Exception):
    """The request would be rejected with a 400. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def run_agent(ask, tools, user_text, model="claude-sonnet-5-5", max_turns=8, tool_choice=None):
    # TODO: call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit.
    return None
