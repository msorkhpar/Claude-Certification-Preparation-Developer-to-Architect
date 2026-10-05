"""A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def _drop_reason(brief, key, seen, kept_count, max_agents):
    if not brief.strip():
        return "empty brief"
    if key in seen:
        return "duplicate scope"
    if kept_count >= max_agents:
        return "over limit"
    return None


def _report_problem(report):
    if not isinstance(report, str) or not report.strip():
        return "empty report"
    return None


def _follow_up(gap, question):
    return f"Follow up: {gap}\nQuestion: {question}"


def _may_refine(gaps, rounds, max_rounds):
    return bool(gaps) and rounds < max_rounds


def _final_status(gaps, failed):
    return "complete" if not gaps and not failed else "partial"


def _wants_team(plan):
    return bool(plan.get("delegate"))


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
    out = []
    for gap in gaps or []:
        gap = str(gap).strip()
        if gap and gap not in out:
            out.append(gap)
    return out[:max_agents]


def _brief_of(brief):
    """What a subagent is told: its own brief and nothing the others found."""
    return brief


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
