"""Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md."""
SEVERITY = {"high": 0, "medium": 1, "low": 2}
DOMAINS = ["P1", "P2", "P3", "P4", "P5", "P6", "P7"]


def launch_review(flags, numbers):
    """The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule."""
    f, n = set(flags), dict(numbers)
    found = []

    def add(condition, severity, domain, rule):
        if condition:
            found.append(f"{severity} {domain} {rule}")

    add("feedback_loop" not in f, "high", "P1", "missing-feedback")
    add(("agent" in f or "team" in f) and "path_known" in f, "medium", "P1", "autonomy-without-need")
    add("team" in f and n.get("team_value_chats", 0) < 15, "medium", "P1", "team-below-price")
    add("volatile_prefix" in f, "medium", "P2", "volatile-prefix")
    add("model_measured" not in f, "low", "P2", "model-not-measured")
    add("filter_after_ranking" in f, "high", "P3", "filter-after-ranking")
    add("replace_on_change" not in f, "high", "P3", "stale-index")
    add(n.get("tool_tokens", 0) > 10000 and "deferral" not in f, "medium", "P3", "tool-bloat")
    add("agent_rights_only" in f, "high", "P3", "agent-rights-only")
    add("protected_segment" not in f, "medium", "P4", "no-protected-segment")
    add(n.get("eval_cases", 0) < 20, "low", "P4", "small-eval-set")
    add("rollback" not in f, "high", "P4", "no-way-back")
    add(n.get("rollout_stages", 0) < 3, "medium", "P4", "big-bang-rollout")
    add("pii_reaches_model" in f, "high", "P5", "identifiers-reach-model")
    add("residency_unmet" in f, "high", "P5", "residency-unmet")
    add("audit_keeps_content" in f, "medium", "P5", "audit-keeps-content")
    add("irreversible_action" in f and "human_step" not in f, "high", "P5", "irreversible-without-person")
    add(n.get("retain_days", 0) < n.get("floor_days", 0) or n.get("retain_days", 0) > n.get("ceiling_days", 0), "medium", "P5", "retention-outside-window")
    add("owner" not in f, "medium", "P6", "no-accountable-owner")
    add(n.get("latency_ms", 0) <= 0 or n.get("availability_tenths", 0) <= 0, "medium", "P6", "sla-without-numbers")
    add("accuracy_stated" not in f, "low", "P6", "accuracy-unstated")
    add(n.get("team_size", 0) > 10 and "managed_settings" not in f, "medium", "P7", "unmanaged-team-settings")
    return sorted(found, key=lambda s: (SEVERITY[s.split()[0]], s.split()[1], s.split()[2]))


def verdict(findings):
    """reject for any high finding, revise for any medium one, otherwise approve."""
    if any(x.startswith("high ") for x in findings):
        return "reject"
    if any(x.startswith("medium ") for x in findings):
        return "revise"
    return "approve"


def scorecard(findings):
    """The number of findings in each domain, P1 to P7."""
    return [sum(1 for x in findings if x.split()[1] == d) for d in DOMAINS]


def needed_accuracy(error_cost, review_cost):
    """The break-even accuracy in whole percent, rounded up on the cost side; 0 when an error costs nothing or no more than a check."""
    if error_cost <= 0:
        return 0
    needed = -(-100 * review_cost // error_cost)
    return max(0, 100 - needed)
