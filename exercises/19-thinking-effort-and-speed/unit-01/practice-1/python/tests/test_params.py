import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from params import RejectedRequest, build_params

FABLE, OPUS, SONNET, HAIKU = "claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-4-5-20251001"


def refused(model, max_tokens=4096, **options):
    """The parameter named by the RejectedRequest, or None when the request was accepted."""
    try:
        build_params(model, max_tokens, options)
    except RejectedRequest as err:
        return err.param
    except Exception as err:  # noqa: BLE001
        return f"crash: {err!r}"
    return None


def test_m1_each_course_model_gets_the_request_it_accepts():
    assert build_params(FABLE, 8000, {"effort": "xhigh"}) == {"model": FABLE, "max_tokens": 8000, "output_config": {"effort": "xhigh"}}
    assert build_params(OPUS, 4096) == {"model": OPUS, "max_tokens": 4096}
    assert build_params(SONNET, 4096, {"thinking": {"type": "adaptive"}, "effort": "medium"}) == {
        "model": SONNET, "max_tokens": 4096, "thinking": {"type": "adaptive"}, "output_config": {"effort": "medium"}}
    assert build_params(HAIKU, 4096, {"thinking": {"type": "enabled", "budget_tokens": 2048}}) == {
        "model": HAIKU, "max_tokens": 4096, "thinking": {"type": "enabled", "budget_tokens": 2048}}


def test_e1_thinking_modes_a_model_does_not_have_are_refused():
    for model in (FABLE, OPUS, SONNET):
        assert refused(model, thinking={"type": "disabled"}) == "thinking", model
    for model in (OPUS, SONNET, FABLE):
        assert refused(model, thinking={"type": "enabled", "budget_tokens": 2048}) == "thinking", model
    assert refused(HAIKU, thinking={"type": "adaptive"}) == "thinking"
    assert refused(HAIKU, thinking={"type": "disabled"}) is None
    assert refused(OPUS, thinking={"type": "adaptive"}) is None


def test_e2_the_mode_that_skips_thinking_up_front_is_sonnet_only_and_needs_high_effort_or_below():
    assert (build_params(SONNET, 4096, {"thinking": {"type": "between_tools"}}) or {}).get("thinking") == {"type": "between_tools"}
    for level in ("low", "medium", "high"):
        assert refused(SONNET, thinking={"type": "between_tools"}, effort=level) is None, level
    for level in ("xhigh", "max"):
        assert refused(SONNET, thinking={"type": "between_tools"}, effort=level) == "thinking", level
    assert refused(OPUS, thinking={"type": "between_tools"}) == "thinking"
    assert refused(HAIKU, thinking={"type": "between_tools"}) == "thinking"


def test_e3_effort_needs_a_supporting_model_and_a_real_level():
    assert refused(HAIKU, effort="low") == "output_config.effort"
    for level in ("low", "medium", "high", "xhigh", "max"):
        assert refused(SONNET, effort=level) is None, level
        assert refused(FABLE, effort=level) is None, level
    assert refused(OPUS, effort="adaptive") == "output_config.effort"
    assert refused(OPUS, effort="extreme") == "output_config.effort"


def test_e4_newer_models_reject_sampling_parameters_and_haiku_keeps_them():
    assert refused(OPUS, temperature=0.2) == "temperature"
    assert refused(SONNET, top_p=0.9) == "top_p"
    assert refused(FABLE, top_k=40) == "top_k"
    assert refused(OPUS, temperature=1.0) is None
    kept = build_params(HAIKU, 1024, {"temperature": 0.2, "top_k": 40})
    assert kept == {"model": HAIKU, "max_tokens": 1024, "temperature": 0.2, "top_k": 40}


def test_e5_fast_mode_is_opus_only_with_its_beta_header_and_never_in_a_batch():
    params = build_params(OPUS, 4096, {"speed": "fast"}) or {}
    assert params.get("speed") == "fast" and params.get("betas") == ["fast-mode-2026-02-01"]
    assert refused(SONNET, speed="fast") == "speed"
    assert refused(HAIKU, speed="fast") == "speed"
    assert refused(OPUS, speed="fast", batch=True) == "speed"
    assert "speed" not in (build_params(OPUS, 4096, {"speed": "standard"}) or {"speed": None})


def test_e6_a_manual_budget_is_at_least_1024_and_below_max_tokens():
    assert refused(HAIKU, 4096, thinking={"type": "enabled", "budget_tokens": 1023}) == "thinking.budget_tokens"
    assert refused(HAIKU, 2048, thinking={"type": "enabled", "budget_tokens": 1024}) is None
    assert refused(HAIKU, 2048, thinking={"type": "enabled", "budget_tokens": 2048}) == "thinking.budget_tokens"
    assert refused(HAIKU, 2048, thinking={"type": "enabled"}) == "thinking.budget_tokens"
    assert refused(OPUS, 0) == "max_tokens"


def test_e7_effort_lives_in_output_config_and_never_inside_thinking():
    params = build_params(SONNET, 4096, {"thinking": {"type": "adaptive"}, "effort": "high"}) or {}
    assert params.get("thinking") == {"type": "adaptive"}
    assert params.get("output_config") == {"effort": "high"}
    options = {"thinking": {"type": "adaptive"}, "effort": "low"}
    build_params(SONNET, 4096, options)
    assert options == {"thinking": {"type": "adaptive"}, "effort": "low"}
