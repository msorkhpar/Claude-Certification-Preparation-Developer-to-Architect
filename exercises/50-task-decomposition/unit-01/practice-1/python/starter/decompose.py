"""Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md."""


def review_changes(files, file_pass, cross_pass, max_lines=40):
    # TODO: review each file alone (long files in parts), then run one cross pass over the summaries of the reviewed files.
    return None


def run_adaptive(planner, worker, goal, max_steps=6):
    # TODO: ask the planner what to do next after every step, and stop when it is done, stuck or out of steps.
    return None


def choose_strategy(task):
    # TODO: fixed_chain, per_item_then_cross or adaptive.
    return None
