import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from coordinator import coordinate


def plan(*subtasks, delegate=True, answer=None):
    return lambda question: {"delegate": delegate, "answer": answer, "subtasks": [{"scope": s, "brief": b} for s, b in subtasks]}


class Spokes:
    """Subagents: the report is looked up by the start of the brief; every brief received is kept."""

    def __init__(self, **reports):
        self.reports, self.briefs = reports, []

    def __call__(self, brief):
        self.briefs.append(brief)
        for prefix, report in self.reports.items():
            if brief.startswith(prefix.replace("_", " ")):
                if isinstance(report, Exception):
                    raise report
                return report
        raise AssertionError(f"no scripted report for {brief!r}")


def no_gaps(question, findings):
    return []


def echo(question, findings):
    return " | ".join(f["text"] for f in findings)


THREE = (("chips", "chips: find 2024 chip supply news"), ("cars", "cars: find 2024 car output news"), ("rates", "rates: find 2024 interest rates"))
REPORTS = {"chips": "chips report", "cars": "cars report", "rates": "rates report"}


def test_m1_a_hub_sends_one_brief_to_each_spoke_and_synthesizes_what_comes_back():
    spokes = Spokes(**REPORTS)
    result = coordinate(plan(*THREE), spokes, no_gaps, echo, "How did supply change?") or {}
    assert (result.get("status"), result.get("answer"), result.get("subagent_calls"), result.get("rounds")) == ("complete", "chips report | cars report | rates report", 3, 0)
    assert result.get("findings") == [{"scope": "chips", "text": "chips report"}, {"scope": "cars", "text": "cars report"}, {"scope": "rates", "text": "rates report"}]
    assert result.get("failed") == [] and result.get("dropped") == [] and result.get("gaps") == []


def test_e1_a_question_the_coordinator_can_answer_itself_is_answered_without_any_subagent():
    spokes = Spokes()
    asked = []
    result = coordinate(plan(*THREE, delegate=False, answer="Paris."), spokes, lambda q, f: asked.append(q) or [], lambda q, f: asked.append("synth") or "x", "Capital of France?") or {}
    assert (result.get("status"), result.get("answer"), result.get("subagent_calls"), result.get("findings")) == ("direct", "Paris.", 0, [])
    assert spokes.briefs == [] and asked == []


def test_e2_a_subagent_sees_its_own_brief_and_nothing_the_others_found():
    spokes = Spokes(**REPORTS)
    coordinate(plan(*THREE), spokes, no_gaps, echo, "How did supply change?")
    assert len(spokes.briefs) == 3
    assert spokes.briefs[1] == "cars: find 2024 car output news" and spokes.briefs[2] == "rates: find 2024 interest rates"
    assert not any("report" in brief for brief in spokes.briefs)


def test_e3_the_plan_is_cleaned_of_empty_briefs_and_duplicate_scopes_and_capped():
    spokes = Spokes(a="a report", b="b report", c="c report")
    messy = plan(("A", "a: first"), ("a ", "a: again"), ("B", "   "), ("b", "b: second"), ("c", "c: third"), ("d", "d: fourth"))
    result = coordinate(messy, spokes, no_gaps, echo, "q", max_agents=3) or {}
    assert spokes.briefs == ["a: first", "b: second", "c: third"] and result.get("subagent_calls") == 3
    assert result.get("dropped") == [{"scope": "a ", "reason": "duplicate scope"}, {"scope": "B", "reason": "empty brief"}, {"scope": "d", "reason": "over limit"}]
    empty = coordinate(plan(("x", ""), ("y", "  ")), Spokes(), no_gaps, echo, "q") or {}
    assert (empty.get("status"), empty.get("subagent_calls"), empty.get("answer")) == ("failed", 0, None)


def test_e4_a_failing_subagent_does_not_stop_the_others_and_a_run_with_no_findings_does_not_synthesize():
    spokes = Spokes(chips=RuntimeError("search offline"), cars="cars report", rates="   ")
    result = coordinate(plan(*THREE), spokes, no_gaps, echo, "q") or {}
    assert (result.get("status"), result.get("answer"), result.get("subagent_calls")) == ("partial", "cars report", 3)
    assert result.get("failed") == [{"scope": "chips", "error": "search offline"}, {"scope": "rates", "error": "empty report"}]
    synthesized = []
    dead = coordinate(plan(*THREE), Spokes(chips=RuntimeError("x"), cars=RuntimeError("y"), rates=""), no_gaps, lambda q, f: synthesized.append(f) or "z", "q") or {}
    assert (dead.get("status"), dead.get("answer"), dead.get("findings"), synthesized) == ("failed", None, [], [])


def test_e5_a_review_sends_only_the_gaps_back_out_and_stops_when_none_are_left():
    spokes = Spokes(chips="chips report", cars="cars report", Follow="follow-up report")
    reviews = []

    def reviewer(question, findings):
        reviews.append([f["scope"] for f in findings])
        return ["2023 baseline", "  ", "2023 baseline"] if len(reviews) == 1 else []

    result = coordinate(plan(*THREE[:2]), spokes, reviewer, echo, "How did supply change?") or {}
    assert spokes.briefs == [THREE[0][1], THREE[1][1], "Follow up: 2023 baseline\nQuestion: How did supply change?"]
    assert reviews == [["chips", "cars"], ["chips", "cars", "2023 baseline"]]
    assert (result.get("status"), result.get("rounds"), result.get("subagent_calls"), result.get("gaps")) == ("complete", 1, 3, [])
    assert result.get("answer") == "chips report | cars report | follow-up report"


def test_e6_the_rounds_are_capped_and_the_gaps_that_remain_are_reported():
    spokes = Spokes(chips="chips report", Follow="more")
    reviews = []

    def reviewer(question, findings):
        reviews.append(len(findings))
        return ["still missing"]

    result = coordinate(plan(THREE[0]), spokes, reviewer, echo, "q", max_rounds=2) or {}
    assert (result.get("status"), result.get("rounds"), result.get("subagent_calls"), result.get("gaps")) == ("partial", 2, 3, ["still missing"])
    assert reviews == [1, 2, 3]
