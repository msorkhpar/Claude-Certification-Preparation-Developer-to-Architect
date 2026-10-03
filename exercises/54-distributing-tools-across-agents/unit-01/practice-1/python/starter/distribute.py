"""Distributing tools across agents: scoped tool sets, the tool choice of a turn, a check of the reply and the authorisation of a call. See ../../statement.md."""

NO_FORCING = {"claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"}  # models whose API rejects tool_choice any and tool, as read on 2026-10-03


def assign_tools(roles, catalog, budget=5):
    # TODO: give each role the tools of its specialisation, plus what it is explicitly granted; refuse a set that is too large.
    return None


def plan_turn(model, need, tools, forced=None, manual_thinking=False):
    # TODO: the tool_choice and tool list of one request, with the portable fallback where forcing is not accepted.
    return None


def cache_impact(previous, new):
    # TODO: what does a change between two requests cost in prompt caching: none, messages or all?
    return None


def check_turn(blocks, need, forced=None):
    # TODO: compare a reply with the call that was required: ok, missed_call or wrong_tool.
    return None


def authorize(call, policy, approvals):
    # TODO: decide a call to a tool that cannot be undone, in the tool layer.
    return None
