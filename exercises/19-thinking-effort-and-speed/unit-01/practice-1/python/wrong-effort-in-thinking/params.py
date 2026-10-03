"""Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract."""
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


def build_params(model, max_tokens, options=None):
    options = options or {}
    family = _family(model)
    haiku = family == "claude-haiku-4-5"
    if max_tokens < 1:
        raise RejectedRequest("max_tokens", "must be at least 1")
    params = {"model": model, "max_tokens": max_tokens}

    effort = options.get("effort")
    if effort is not None:
        if haiku:
            raise RejectedRequest("output_config.effort", "this model does not support effort")
        if effort not in EFFORTS:
            raise RejectedRequest("output_config.effort", f"{effort} is not an effort level")
        params["output_config"] = {"effort": effort}

    thinking = options.get("thinking")
    if thinking is not None:
        kind = thinking.get("type")
        if kind == "adaptive":
            if haiku:
                raise RejectedRequest("thinking", "adaptive thinking is not available on this model")
            params["thinking"] = {"type": "adaptive", **({"effort": effort} if effort else {})}
        elif kind == "enabled":
            budget = thinking.get("budget_tokens")
            if not haiku:
                raise RejectedRequest("thinking", "manual thinking budgets are not accepted on this model")
            if budget is None or budget < 1024 or budget >= max_tokens:
                raise RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens")
            params["thinking"] = {"type": "enabled", "budget_tokens": budget}
        elif kind == "disabled":
            if not haiku:
                raise RejectedRequest("thinking", "thinking cannot be turned off on this model")
            params["thinking"] = {"type": "disabled"}
        elif kind == "between_tools":
            if family != "claude-sonnet-5-5":
                raise RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5")
            if (effort or DEFAULT_EFFORT[family]) in ("xhigh", "max"):
                raise RejectedRequest("thinking", "between_tools works at low, medium and high effort only")
            params["thinking"] = {"type": "between_tools"}
        else:
            raise RejectedRequest("thinking", f"unknown thinking type {kind}")

    for name in ("temperature", "top_p", "top_k"):
        value = options.get(name)
        if value is None:
            continue
        if not haiku and not (name == "temperature" and value == 1.0):
            raise RejectedRequest(name, "this model rejects a non-default value")
        params[name] = value

    if options.get("speed") == "fast":
        if family != "claude-opus-5-5":
            raise RejectedRequest("speed", "fast mode is not available on this model")
        if options.get("batch"):
            raise RejectedRequest("speed", "fast mode is not available in a batch")
        params["speed"] = "fast"
        params["betas"] = [FAST_BETA]
    return params
