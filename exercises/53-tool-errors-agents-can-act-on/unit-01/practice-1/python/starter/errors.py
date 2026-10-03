"""Tool errors that an agent can act on: a structured error, bounded retries, an unknown outcome and the next action. See ../../statement.md."""


class ToolError(Exception):
    """What a tool raises. `kind` is transient, validation, permission, business or timeout (no answer was received, so the effect is unknown)."""

    def __init__(self, kind, message, retry_after_ms=None, explanation=None):
        super().__init__(message)
        self.kind, self.retry_after_ms, self.explanation = kind, retry_after_ms, explanation


def make_error(kind, message, explanation=None, attempts=1, attempted=None):
    # TODO: the structured result of a failed call.
    return None


def to_tool_result(tool_use_id, result):
    # TODO: the tool_result block for the API.
    return None


def run_tool(tool, args, policy, sleep):
    # TODO: call the tool, retry only what is safe to retry, return a result or a structured error.
    return None


def next_action(result):
    # TODO: what the loop does next with a result.
    return None
