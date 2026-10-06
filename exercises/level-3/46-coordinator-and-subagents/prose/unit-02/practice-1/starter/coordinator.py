"""A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def _drop_reason(brief, key, seen, kept_count, max_agents):
    """TODO 1 of 8 (unlocks e3): why a subtask is dropped, or None to keep it.

    Receives the brief, the lower-cased scope key, the set of keys already kept, how many are kept and the limit. Returns
    "empty brief" for a blank brief, else "duplicate scope" for a key already seen, else "over limit" when kept_count has reached
    max_agents, else None. Example: _drop_reason("  ", "a", set(), 0, 3) -> "empty brief"
    """
    return None


def _report_problem(report):
    """TODO 2 of 8 (unlocks e4): what is wrong with a subagent's report, or None.

    Receives whatever the subagent returned. Returns "empty report" when it is not a string or only blank, else None.
    Example: _report_problem("   ") -> "empty report"
    """
    return None


def _follow_up(gap, question):
    """TODO 3 of 8 (unlocks e5): the brief for a follow-up on one gap.

    Returns "Follow up: <gap>" then a newline then "Question: <question>". Example: _follow_up("2023", "q") -> "Follow up: 2023\nQuestion: q"
    """
    return gap


def _may_refine(gaps, rounds, max_rounds):
    """TODO 4 of 8 (unlocks e5 and e6): is another refinement round allowed?

    True while there are gaps and fewer than max_rounds rounds have run. Example: _may_refine(["x"], 2, 2) -> False
    """
    return False


def _final_status(gaps, failed):
    """TODO 5 of 8 (unlocks m1 and e6): the status of a run that has an answer.

    "complete" when no gaps remain and nothing failed, else "partial". Example: _final_status(["x"], []) -> "partial"
    """
    return "complete"


def _wants_team(plan):
    """TODO 6 of 8 (unlocks e1): does the plan ask for subagents?

    Receives the planner's dict. True when its "delegate" value is truthy, else False. Example: _wants_team({"delegate": False}) -> False
    """
    return True


def _clean(subtasks, max_agents):
    kept, dropped, seen = [], [], set()
    for task in subtasks:
        scope, brief = str(task.get("scope", "")), str(task.get("brief", ""))
        key = scope.strip().lower()
        reason = _drop_reason(brief, key, seen, len(kept), max_agents)
        if reason:
            dropped.append({"scope": scope, "reason": reason})
        else:
            seen.add(key)
            kept.append({"scope": scope, "brief": brief})
    return kept, dropped


def _clean_gaps(gaps, max_agents):
    """TODO 7 of 8 (unlocks e5 and e6): the reviewer's gaps, tidied.

    Receives the reviewer's list (or None) and the limit. Returns the gaps stripped of blanks, without empty or repeated ones, in order, at most
    max_agents of them. Example: _clean_gaps([" a ", "", "a", "b"], 1) -> ["a"]
    """
    return list(gaps or [])


def _brief_of(brief):
    """TODO 8 of 8 (unlocks m1 and e2): the text a subagent is handed.

    Receives the subtask's brief. Returns exactly what the subagent is told: that brief and nothing else (no scope list, no earlier findings).
    Example: _brief_of("cars: find 2024 car output news") -> "cars: find 2024 car output news"
    """
    return ""


def coordinate(planner, subagent, reviewer, synthesizer, question, max_agents=4, max_rounds=2):
    log.debug("coordinate input: %r", question)
    plan = planner(question)
    if not _wants_team(plan):  # a question the coordinator can answer itself is not worth a team
        return {"status": "direct", "answer": plan.get("answer"), "findings": [], "failed": [], "dropped": [], "gaps": [], "rounds": 0, "subagent_calls": 0}
    tasks, dropped = _clean(plan.get("subtasks") or [], max_agents)
    findings, failed, calls = [], [], 0

    def run(scope, brief):
        nonlocal calls
        calls += 1
        try:
            report = subagent(brief)  # the brief is everything the subagent knows
        except Exception as error:  # one subagent failing must not stop the others
            failed.append({"scope": scope, "error": str(error)})
            return
        problem = _report_problem(report)
        if problem:
            failed.append({"scope": scope, "error": problem})
        else:
            findings.append({"scope": scope, "text": report})

    for task in tasks:
        run(task["scope"], _brief_of(task["brief"]))
    if not findings:
        return {"status": "failed", "answer": None, "findings": [], "failed": failed, "dropped": dropped, "gaps": [], "rounds": 0, "subagent_calls": calls}
    rounds = 0
    gaps = _clean_gaps(reviewer(question, [dict(f) for f in findings]), max_agents)
    while _may_refine(gaps, rounds, max_rounds):
        rounds += 1
        for gap in gaps:
            run(gap, _follow_up(gap, question))
        gaps = _clean_gaps(reviewer(question, [dict(f) for f in findings]), max_agents)
    answer = synthesizer(question, [dict(f) for f in findings])
    status = _final_status(gaps, failed)
    return {"status": status, "answer": answer, "findings": findings, "failed": failed, "dropped": dropped, "gaps": gaps, "rounds": rounds, "subagent_calls": calls}
