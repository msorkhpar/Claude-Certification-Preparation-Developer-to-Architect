"""The pattern ladder: which rung a task needs, and whether its value pays for the rung.

Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
a measurement of your workload, and the rule is the course's own teaching model.
"""
MULTIPLIER = {"plain call": 1, "augmented call": 1, "agent": 4, "multi-agent": 15}  # a workflow costs one chat per step

TASKS = [
    {"name": "classify ticket", "one_step": True, "steps": 1, "needs_external": False, "steps_known": True, "independent_parts": False, "value": 0.05, "chat_cost": 0.02},
    {"name": "answer from policy", "one_step": True, "steps": 1, "needs_external": True, "steps_known": True, "independent_parts": False, "value": 0.40, "chat_cost": 0.02},
    {"name": "claims intake", "one_step": False, "steps": 4, "needs_external": True, "steps_known": True, "independent_parts": False, "value": 6.00, "chat_cost": 0.02},
    {"name": "investigate outage", "one_step": False, "steps": 0, "needs_external": True, "steps_known": False, "independent_parts": False, "value": 40.00, "chat_cost": 0.02},
    {"name": "market research brief", "one_step": False, "steps": 0, "needs_external": True, "steps_known": False, "independent_parts": True, "value": 25.00, "chat_cost": 0.02},
    {"name": "trivia round-up", "one_step": False, "steps": 0, "needs_external": True, "steps_known": False, "independent_parts": True, "value": 0.05, "chat_cost": 0.02},
]


def choose_pattern(task):
    """The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost."""
    if task["one_step"]:
        return "augmented call" if task["needs_external"] else "plain call"
    if task["steps_known"]:
        return "workflow"
    if task["independent_parts"] and task["value"] >= MULTIPLIER["multi-agent"] * task["chat_cost"]:
        return "multi-agent"
    return "agent"


def cost(task, pattern):
    multiplier = task["steps"] if pattern == "workflow" else MULTIPLIER[pattern]
    return multiplier * task["chat_cost"]


def main():
    for task in TASKS:
        pattern = choose_pattern(task)
        price = cost(task, pattern)
        print(f"{task['name']}: {pattern}, cost {price:.2f}, value {task['value']:.2f}, pays: {task['value'] >= price}")


if __name__ == "__main__":
    main()
