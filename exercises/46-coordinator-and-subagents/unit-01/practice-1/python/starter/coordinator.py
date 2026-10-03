"""A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md."""


def coordinate(planner, subagent, reviewer, synthesizer, question, max_agents=4, max_rounds=2):
    # TODO: plan, delegate one brief per subagent, review the findings for gaps, delegate the gaps, then synthesize once.
    return None
