"""The loop around the computer use tool, against a toy screen. See ../../statement.md."""
TOOLSET = "computer_toolset_20260801"
CLICKS = ("left_click", "right_click", "middle_click", "double_click", "triple_click")
NOT_EXECUTED = "Not executed: an earlier computer action in this turn failed."


def render(screen, width, height):
    """Given: the screenshot of the toy screen at `width` x `height`, as the base64 text of a picture. It changes after every logged action."""
    return f"png:{width}x{height}:{len(screen['log'])}"


def scale_for(width, height):
    # TODO: the factor that shrinks a screen to what the model may be sent: min(1, 1568 / longest side, sqrt(1,150,000 / pixels)).
    return None


def scaled_size(width, height):
    # TODO: (int(width * scale), int(height * scale)).
    return None


def to_screen(x, y, scale, screen):
    # TODO: a point on the scaled screenshot as a point on the real screen: divide by the scale, round, clamp into the screen.
    return None


def perform(screen, name, args, scale, confirm=None):
    # TODO: run one action on the toy screen and return (content, is_error). See the statement for every action.
    return None


def prune_screenshots(messages, keep=3):
    # TODO: a copy of the conversation in which every screenshot except the newest `keep` is replaced by a text note.
    return None


def run_computer_loop(ask, screen, model="claude-sonnet-5-5", max_turns=10, confirm=None):
    # TODO: call the model with the computer toolset, run its actions, send the results back, until it ends or a limit is hit.
    return None
