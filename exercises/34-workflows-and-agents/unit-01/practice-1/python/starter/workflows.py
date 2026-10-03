"""Workflow patterns around a model: orchestrator and workers, evaluator and optimiser, routing and voting. See ../../statement.md."""


def orchestrate(ask, task, max_subtasks=5):
    # TODO: plan with one call, run a worker call for each subtask, combine the results with one more call.
    return None


def refine(write, judge, task, max_rounds=3, threshold=8):
    # TODO: write a draft, judge it, and revise with the feedback until the score reaches the threshold or the rounds run out.
    return None


def route(ask, text, routes, default):
    # TODO: classify the text with one model call, then run the handler of the label (or of the default label).
    return None


def vote(ask, prompt, n=5):
    # TODO: ask n times and return the majority answer, the counts and the share of the winner.
    return None
