"""A coordinator that delegates to isolated subagents, reviews what comes back and synthesizes. See ../../statement.md."""


def _clean(subtasks, max_agents):
    kept, dropped, seen = [], [], set()
    for task in subtasks:
        scope, brief = str(task.get("scope", "")), str(task.get("brief", ""))
        key = scope.strip().lower()
        if not brief.strip():
            dropped.append({"scope": scope, "reason": "empty brief"})
        elif key in seen:
            dropped.append({"scope": scope, "reason": "duplicate scope"})
        elif len(kept) >= max_agents:
            dropped.append({"scope": scope, "reason": "over limit"})
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


def coordinate(planner, subagent, reviewer, synthesizer, question, max_agents=4, max_rounds=2):
    plan = planner(question)
    if not plan.get("delegate"):  # a question the coordinator can answer itself is not worth a team
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
        if not isinstance(report, str) or not report.strip():
            failed.append({"scope": scope, "error": "empty report"})
        else:
            findings.append({"scope": scope, "text": report})

    for task in tasks:
        run(task["scope"], task["brief"])
    if not findings:
        return {"status": "failed", "answer": None, "findings": [], "failed": failed, "dropped": dropped, "gaps": [], "rounds": 0, "subagent_calls": calls}
    rounds = 0
    gaps = _clean_gaps(reviewer(question, [dict(f) for f in findings]), max_agents)
    while gaps and rounds < max_rounds:
        rounds += 1
        for gap in gaps:
            run(gap, f"Follow up: {gap}\nQuestion: {question}")
        gaps = _clean_gaps(reviewer(question, [dict(f) for f in findings]), max_agents)
    answer = synthesizer(question, [dict(f) for f in findings])
    status = "complete" if not gaps and not failed else "partial"
    return {"status": status, "answer": answer, "findings": findings, "failed": failed, "dropped": dropped, "gaps": gaps, "rounds": rounds, "subagent_calls": calls}
