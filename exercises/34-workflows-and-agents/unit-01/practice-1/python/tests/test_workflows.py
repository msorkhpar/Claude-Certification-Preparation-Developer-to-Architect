import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from workflows import orchestrate, refine, route, vote

PLAN3 = '["research the topic", "draft the outline", "check the facts"]'


class Model:
    """A stand-in for the model: the first handler whose prefix starts the prompt answers; every prompt is kept."""

    def __init__(self, **by_prefix):
        self.by_prefix, self.prompts = by_prefix, []

    def __call__(self, prompt):
        self.prompts.append(prompt)
        for prefix, handler in self.by_prefix.items():
            if prompt.startswith(prefix):
                return handler(prompt) if callable(handler) else handler
        raise AssertionError(f"no scripted answer for {prompt!r}")


def lines(model, prefix):
    return [p for p in model.prompts if p.startswith(prefix)]


def test_m1_an_orchestrator_plans_runs_a_worker_per_subtask_and_combines():
    model = Model(Plan=PLAN3, Subtask=lambda p: "done: " + p.split("\n")[0][len("Subtask: "):], Combine="FINAL")
    result = orchestrate(model, "Write a guide") or {}
    assert (result.get("status"), result.get("fallback"), result.get("answer"), result.get("calls")) == ("done", False, "FINAL", 5)
    assert result.get("plan") == ["research the topic", "draft the outline", "check the facts"]
    assert [(r["subtask"], r["status"], r.get("output")) for r in result.get("results", [])] == [
        ("research the topic", "ok", "done: research the topic"), ("draft the outline", "ok", "done: draft the outline"), ("check the facts", "ok", "done: check the facts")]
    assert model.prompts[:1] == ["Plan: split the task into at most 5 independent subtasks. Reply with a JSON array of strings only.\nTask: Write a guide"]
    assert lines(model, "Subtask")[0] == "Subtask: research the topic\nTask: Write a guide" and len(lines(model, "Subtask")) == 3
    assert lines(model, "Combine") == ["Combine: write one answer to the task from the results.\nTask: Write a guide\n1. research the topic -> done: research the topic\n"
                                       "2. draft the outline -> done: draft the outline\n3. check the facts -> done: check the facts"]


def test_e1_the_plan_is_read_from_prose_cleaned_capped_and_replaced_when_unusable():
    reply = 'Here is the plan:\n```json\n["a", "b", "a", "  c  ", "", 5, "d", "e"]\n```\nGood luck.'
    capped = orchestrate(Model(Plan=reply, Subtask="x", Combine="F"), "T", max_subtasks=3) or {}
    assert capped.get("plan") == ["a", "b", "c"] and capped.get("calls") == 5
    assert (orchestrate(Model(Plan=reply, Subtask="x", Combine="F"), "T") or {}).get("plan") == ["a", "b", "c", "d", "e"]
    seen = Model(Plan=reply, Subtask="x", Combine="F")
    orchestrate(seen, "T", max_subtasks=2)
    assert seen.prompts[0].startswith("Plan: split the task into at most 2 independent subtasks.")
    for bad in ("I cannot plan this.", "[]", '["", 7]', "[not json]"):
        model = Model(Plan=bad, Subtask="x", Combine="F")
        result = orchestrate(model, "T") or {}
        assert (result.get("plan"), result.get("fallback"), result.get("calls")) == (["T"], True, 3)
        assert lines(model, "Subtask") == ["Subtask: T\nTask: T"]


def test_e2_one_failing_worker_does_not_stop_the_others_or_the_answer():
    def worker(prompt):
        if prompt.startswith("Subtask: b"):
            raise RuntimeError("disk full")
        return "" if prompt.startswith("Subtask: c") else "ok " + prompt[9]

    model = Model(Plan='["a", "b", "c"]', Subtask=worker, Combine="FINAL")
    result = orchestrate(model, "T") or {}
    assert (result.get("status"), result.get("answer"), result.get("calls")) == ("partial", "FINAL", 5)
    assert [(r["subtask"], r["status"], r.get("error")) for r in result.get("results", [])] == [("a", "ok", None), ("b", "failed", "disk full"), ("c", "failed", "empty reply")]
    assert lines(model, "Combine") == ["Combine: write one answer to the task from the results.\nTask: T\n1. a -> ok a\n2. b -> FAILED\n3. c -> FAILED"]
    nothing = Model(Plan='["a", "b"]', Subtask=lambda p: "  ", Combine="never")
    result = orchestrate(nothing, "T") or {}
    assert (result.get("status"), result.get("answer"), result.get("calls")) == ("failed", None, 3) and lines(nothing, "Combine") == []


def judged(*replies):
    queue = list(replies)
    return Model(Judge=lambda p: queue.pop(0))


def test_e3_a_draft_is_revised_with_the_feedback_until_the_judge_accepts_it():
    drafts = iter(["draft1", "draft2", "draft3"])
    writer = Model(Task=lambda p: next(drafts))
    judge = judged('{"score": 5, "feedback": "add examples"}', '{"score": 9, "feedback": "good"}')
    result = refine(writer, judge, "T") or {}
    assert (result.get("status"), result.get("draft"), result.get("score"), result.get("rounds")) == ("accepted", "draft2", 9, 2)
    assert writer.prompts == ["Task: T", "Task: T\nPrevious draft: draft1\nFeedback: add examples\nRevise the draft."]
    assert judge.prompts[0] == 'Judge: score the draft from 0 to 10 and reply with JSON {"score": n, "feedback": "..."}.\nTask: T\nDraft: draft1'
    assert result.get("history") == [{"round": 1, "score": 5, "feedback": "add examples"}, {"round": 2, "score": 9, "feedback": "good"}]
    edge = refine(Model(Task=lambda p: "d"), judged('{"score": 8, "feedback": ""}'), "T") or {}
    assert (edge.get("status"), edge.get("rounds")) == ("accepted", 1)
    custom = refine(Model(Task=lambda p: "d"), judged('{"score": 8, "feedback": ""}', '{"score": 10}'), "T", max_rounds=4, threshold=10) or {}
    assert (custom.get("status"), custom.get("rounds"), custom.get("score")) == ("accepted", 2, 10)


def test_e4_when_the_rounds_run_out_the_best_draft_wins_and_an_unreadable_judge_scores_zero():
    drafts = iter(["draft1", "draft2", "draft3"])
    result = refine(Model(Task=lambda p: next(drafts)), judged('{"score": 6, "feedback": "x"}', '{"score": 7, "feedback": "y"}', '{"score": 7, "feedback": "z"}'), "T") or {}
    assert (result.get("status"), result.get("draft"), result.get("score"), result.get("rounds")) == ("max_rounds", "draft2", 7, 3)
    drafts = iter(["draft1", "draft2", "draft3", "draft4"])
    unreadable = refine(Model(Task=lambda p: next(drafts)), judged("not json", '{"score": "high"}', '{"score": 11}', '{"score": true, "feedback": "x"}'), "T", max_rounds=4) or {}
    assert (unreadable.get("status"), unreadable.get("draft"), unreadable.get("score")) == ("max_rounds", "draft1", 0)
    assert [h["feedback"] for h in unreadable.get("history", [])] == ["The judge reply could not be read."] * 4
    drafts = iter(["d1", "d2"])
    prose = refine(Model(Task=lambda p: next(drafts)), judged("hmm", 'Verdict: {"score": 9, "feedback": "ok"} thanks'), "T") or {}
    assert (prose.get("status"), prose.get("draft"), prose.get("score")) == ("accepted", "d2", 9)


def test_e5_a_label_is_read_from_the_reply_and_anything_else_takes_the_default_route():
    routes = {"billing": lambda t: "B:" + t, "technical": lambda t: "T:" + t, "other": lambda t: "O:" + t}
    text = "My card was charged twice"
    model = Model(Classify=" Billing. ")
    result = route(model, text, routes, "other") or {}
    assert result == {"label": "billing", "output": "B:" + text, "fallback": False}
    assert model.prompts == ["Classify: My card was charged twice\nLabels: billing, technical, other"]
    assert (route(Model(Classify="TECHNICAL"), text, routes, "other") or {}).get("label") == "technical"
    for reply in ("I think it is billing", "", "refund"):
        result = route(Model(Classify=reply), text, routes, "other") or {}
        assert result == {"label": "other", "output": "O:" + text, "fallback": True}


def test_e6_the_majority_answer_wins_and_a_tie_goes_to_the_one_seen_first():
    replies = iter(["Yes", "yes ", " NO", "yes"])
    model = Model(Is=lambda p: next(replies))
    result = vote(model, "Is it safe?", 4) or {}
    assert (result.get("answer"), result.get("votes"), result.get("agreement")) == ("yes", {"yes": 3, "no": 1}, 0.75)
    assert model.prompts == ["Is it safe?"] * 4
    replies = iter(["b", "a", "a", "b"])
    assert (vote(Model(Is=lambda p: next(replies)), "Is it safe?", 4) or {}).get("answer") == "b"
    assert (vote(Model(Is="x"), "Is it safe?", 0) or {"answer": "missing"}).get("answer") is None
    assert (vote(Model(Is="x"), "Is it safe?") or {}).get("votes") == {"x": 5}


def test_e7_a_writer_that_fails_ends_the_loop_with_the_best_draft_so_far():
    drafts = iter(["draft1"])

    def flaky(prompt):
        try:
            return next(drafts)
        except StopIteration:
            raise RuntimeError("boom") from None

    result = refine(Model(Task=flaky), judged('{"score": 4, "feedback": "more"}'), "T") or {}
    assert (result.get("status"), result.get("draft"), result.get("score"), result.get("rounds"), result.get("error")) == ("error", "draft1", 4, 1, "boom")
    assert len(result.get("history", [])) == 1
    first = refine(Model(Task=lambda p: (_ for _ in ()).throw(RuntimeError("down"))), judged(), "T") or {}
    assert (first.get("status"), first.get("draft"), first.get("score"), first.get("rounds"), first.get("error")) == ("error", None, None, 0, "down")
