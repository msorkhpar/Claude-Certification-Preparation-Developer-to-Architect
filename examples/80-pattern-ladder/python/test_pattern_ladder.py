from pattern_ladder import MULTIPLIER, TASKS, choose_pattern, cost


def by_name(name):
    return next(t for t in TASKS if t["name"] == name)


def test_a_single_step_stays_on_the_first_two_rungs():
    assert choose_pattern(by_name("classify ticket")) == "plain call"
    assert choose_pattern(by_name("answer from policy")) == "augmented call"


def test_a_known_path_is_a_workflow_and_costs_one_chat_per_step():
    task = by_name("claims intake")
    assert choose_pattern(task) == "workflow"
    assert round(cost(task, "workflow"), 2) == 0.08


def test_an_open_path_is_an_agent_and_a_team_needs_independent_parts_and_value():
    assert choose_pattern(by_name("investigate outage")) == "agent"
    assert choose_pattern(by_name("market research brief")) == "multi-agent"
    assert choose_pattern(by_name("trivia round-up")) == "agent"


def test_the_team_threshold_is_fifteen_chats():
    task = dict(by_name("market research brief"))
    task["value"] = MULTIPLIER["multi-agent"] * task["chat_cost"]
    assert choose_pattern(task) == "multi-agent"
    task["value"] = task["value"] - 0.01
    assert choose_pattern(task) == "agent"
