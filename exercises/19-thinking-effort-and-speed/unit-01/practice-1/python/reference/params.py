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
    """Refuse an effort level the model cannot take."""
    if haiku:
        raise RejectedRequest("output_config.effort", "this model does not support effort")
    if effort not in EFFORTS:
        raise RejectedRequest("output_config.effort", f"{effort} is not an effort level")


def _check_mode(haiku, kind):
    """Refuse a thinking mode the model does not have."""
    if kind == "adaptive" and haiku:
        raise RejectedRequest("thinking", "adaptive thinking is not available on this model")
    if kind == "enabled" and not haiku:
        raise RejectedRequest("thinking", "manual thinking budgets are not accepted on this model")
    if kind == "disabled" and not haiku:
        raise RejectedRequest("thinking", "thinking cannot be turned off on this model")


def _check_budget(budget, max_tokens):
    """Refuse a manual thinking budget that is missing, below 1024 or not below max_tokens."""
    if budget is None or budget < 1024 or budget >= max_tokens:
        raise RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens")


def _check_between_tools(family, effort):
    """Refuse between_tools off Sonnet 5.5, or when the effective effort is xhigh or max."""
    if family != "claude-sonnet-5-5":
        raise RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5")
    if (effort or DEFAULT_EFFORT[family]) in ("xhigh", "max"):
        raise RejectedRequest("thinking", "between_tools works at low, medium and high effort only")


def _thinking_object(kind, budget):
    """The `thinking` value of the request: the type, and the budget for a manual one."""
    if kind == "enabled":
        return {"type": "enabled", "budget_tokens": budget}
    return {"type": kind}


def _sampling_allowed(haiku, name, value):
    """Whether the model accepts this sampling parameter: Haiku all, the others only temperature 1.0."""
    return haiku or (name == "temperature" and value == 1.0)


def _fast_params(family, batch):
    """The parameters fast mode adds, or a refusal: Opus 5.5 only, and never in a batch."""
    if family != "claude-opus-5-5":
        raise RejectedRequest("speed", "fast mode is not available on this model")
    if batch:
        raise RejectedRequest("speed", "fast mode is not available in a batch")
    return {"speed": "fast", "betas": [FAST_BETA]}


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
