"""Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract."""
import logging

log = logging.getLogger(__name__)

EFFORTS = ("low", "medium", "high", "xhigh", "max")
FAST_BETA = "fast-mode-2026-02-01"
DEFAULT_EFFORT = {"claude-fable-5-1": "high", "claude-opus-5-5": "medium", "claude-sonnet-5-5": "high"}


class RejectedRequest(Exception):
    """The API would answer 400. `param` names the offending parameter."""

    def __init__(self, param, reason):
        super().__init__(f"{param}: {reason}")
        self.param, self.reason = param, reason


def _family(model):
    if model.startswith("claude-haiku-4-5"):
        return "claude-haiku-4-5"
    if model in DEFAULT_EFFORT:
        return model
    raise RejectedRequest("model", f"unknown model {model}")


def _check_effort(haiku, effort):
    """TODO 1 of 7 (finish this to pass e3): refuse an effort level the model cannot take.

    Receives whether the model is Haiku and the effort given. Raises RejectedRequest("output_config.effort", ...) for Haiku (it has no effort) and for a level that is not in EFFORTS.
    Example: _check_effort(True, "low") raises, _check_effort(False, "adaptive") raises, _check_effort(False, "high") returns
    """


def _check_mode(haiku, kind):
    """TODO 2 of 7 (finish this to pass e1): refuse a thinking mode the model does not have.

    Receives whether the model is Haiku and the thinking type. Raises RejectedRequest("thinking", ...) for `adaptive` on Haiku, and for `enabled` or `disabled` on any model that is not Haiku.
    Example: _check_mode(False, "disabled") raises, _check_mode(True, "disabled") returns
    """


def _check_budget(budget, max_tokens):
    """TODO 3 of 7 (finish this to pass e6): refuse a manual thinking budget that does not fit.

    Receives the budget (None when missing) and max_tokens. Raises RejectedRequest("thinking.budget_tokens", ...) when the budget is missing, below 1024 or not below max_tokens.
    Example: _check_budget(1023, 4096) raises, _check_budget(2048, 2048) raises, _check_budget(1024, 2048) returns
    """


def _check_between_tools(family, effort):
    """TODO 4 of 7 (finish this to pass e2): refuse between_tools where it does not work.

    Receives the model family and the effort given (None when absent). Raises RejectedRequest("thinking", ...) unless the family is "claude-sonnet-5-5", and when the effective
    effort (the one given, else DEFAULT_EFFORT[family]) is "xhigh" or "max".
    Example: _check_between_tools("claude-sonnet-5-5", "max") raises, _check_between_tools("claude-sonnet-5-5", None) returns
    """


def _thinking_object(kind, budget):
    """TODO 5 of 7 (finish this to pass m1 and e7): the `thinking` value of the request.

    Receives the thinking type and the budget (None unless `enabled`). Returns {"type": kind}, plus "budget_tokens": budget for `enabled`. Effort never goes in here.
    Example: _thinking_object("adaptive", None) -> {"type": "adaptive"}, _thinking_object("enabled", 2048) -> {"type": "enabled", "budget_tokens": 2048}
    """
    return {}


def _sampling_allowed(haiku, name, value):
    """TODO 6 of 7 (finish this to pass e4): whether the model accepts this sampling parameter.

    Receives whether the model is Haiku, the parameter name (temperature, top_p or top_k) and its value. Returns True for Haiku, and for the others only a temperature of exactly 1.0.
    Example: _sampling_allowed(False, "temperature", 0.2) -> False, _sampling_allowed(True, "top_k", 40) -> True
    """
    return True


def _fast_params(family, batch):
    """TODO 7 of 7 (finish this to pass e5): the parameters fast mode adds, or a refusal.

    Receives the model family and whether the request goes into a batch. Raises RejectedRequest("speed", ...) unless the family is "claude-opus-5-5", and when `batch` is true.
    Otherwise returns {"speed": "fast", "betas": [FAST_BETA]}.
    Example: _fast_params("claude-opus-5-5", False) -> {"speed": "fast", "betas": ["fast-mode-2026-02-01"]}
    """
    return {}


def build_params(model, max_tokens, options=None):
    log.debug("build_params input: %r %r %r", model, max_tokens, options)
    options = options or {}
    family = _family(model)
    haiku = family == "claude-haiku-4-5"
    if max_tokens < 1:
        raise RejectedRequest("max_tokens", "must be at least 1")
    params = {"model": model, "max_tokens": max_tokens}

    effort = options.get("effort")
    if effort is not None:
        _check_effort(haiku, effort)
        params["output_config"] = {"effort": effort}

    thinking = options.get("thinking")
    if thinking is not None:
        kind = thinking.get("type")
        if kind not in ("adaptive", "enabled", "disabled", "between_tools"):
            raise RejectedRequest("thinking", f"unknown thinking type {kind}")
        if kind == "between_tools":
            _check_between_tools(family, effort)
        else:
            _check_mode(haiku, kind)
        if kind == "enabled":
            _check_budget(thinking.get("budget_tokens"), max_tokens)
        params["thinking"] = _thinking_object(kind, thinking.get("budget_tokens"))

    for name in ("temperature", "top_p", "top_k"):
        value = options.get(name)
        if value is None:
            continue
        if not _sampling_allowed(haiku, name, value):
            raise RejectedRequest(name, "this model rejects a non-default value")
        params[name] = value

    if options.get("speed") == "fast":
        params.update(_fast_params(family, options.get("batch")))
    return params
