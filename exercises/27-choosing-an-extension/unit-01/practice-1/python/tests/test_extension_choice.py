import itertools
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
import extension_choice as _solution


def choose(situation):
    value = _solution.choose(dict(situation))
    assert value is not None, "choose returned nothing"
    return value


def refused(situation):
    try:
        choose(situation)
    except ValueError:
        return True
    return False


def sweep(axes, base):
    """Every combination of the axes, each added to the base situation."""
    names = [k for k, _ in axes]
    for values in itertools.product(*[v for _, v in axes]):
        yield {**base, **dict(zip(names, values))}


# The scenario bank of the page: id, situation, expected mechanism and reason code.
BANK = [
    ("s01", {"knowledge": 'convention'}, "claude-md", "always-known"),
    ("s02", {"guarantee": True, "knowledge": 'convention'}, "hook", "must-hold-every-time"),
    ("s03", {"guarantee": True}, "hook", "must-hold-every-time"),
    ("s04", {"knowledge": 'convention', "path_scoped": True}, "path-rule", "scoped-convention"),
    ("s05", {"knowledge": 'reference'}, "skill", "on-demand-reference"),
    ("s06", {"knowledge": 'procedure'}, "skill", "repeatable-procedure"),
    ("s07", {"external_system": True}, "mcp", "external-system"),
    ("s08", {"noisy": True}, "subagent", "isolate-context"),
    ("s09", {}, "builtin-tool", "built-in-covers"),
    ("s10", {"external_system": True, "knowledge": 'procedure', "repos": 6}, "plugin", "shared-setup"),
    ("s11", {"knowledge": 'procedure', "repos": 2}, "plugin", "shared-setup"),
    ("s12", {"guarantee": True, "external_system": True}, "hook", "must-hold-every-time"),
    ("s13", {"external_system": True, "noisy": True}, "mcp", "external-system"),
    ("s14", {"knowledge": 'convention', "repos": 6}, "claude-md", "always-known"),
    ("s15", {"noisy": True, "repos": 4}, "plugin", "shared-setup"),
    ("s16", {"surface": 'api'}, "api-tool", "own-schema-and-code"),
    ("s17", {"surface": 'api', "builtin_covers": True}, "builtin-tool", "provided-schema"),
    ("s18", {"surface": 'api', "external_system": True, "remote_server": True}, "mcp", "remote-server"),
]


def test_m1_every_situation_of_the_bank_gets_its_mechanism_and_its_reason():
    wrong = [sid for sid, s, mech, reason in BANK if choose(s) != {"mechanism": mech, "reason": reason}]
    assert wrong == [], f"these situations got the wrong mechanism or reason: {wrong}"


def test_e1_a_rule_that_must_hold_goes_to_a_hook_whatever_else_is_true():
    for s in sweep([("knowledge", ['none', 'convention', 'reference', 'procedure']), ("external_system", [False, True]), ("noisy", [False, True]), ("path_scoped", [False, True])], {"guarantee": True}):
        assert choose(s) == {"mechanism": "hook", "reason": "must-hold-every-time"}, s


def test_e2_an_outside_system_needs_a_server_and_noisy_work_alone_needs_a_subagent():
    for s in sweep([("knowledge", ['none', 'convention', 'reference', 'procedure']), ("noisy", [False, True]), ("path_scoped", [False, True])], {"external_system": True}):
        assert choose(s) == {"mechanism": "mcp", "reason": "external-system"}, s
    for s in sweep([("knowledge", ['none', 'convention', 'reference', 'procedure']), ("path_scoped", [False, True])], {"noisy": True}):
        assert choose(s) == {"mechanism": "subagent", "reason": "isolate-context"}, s


def test_e3_knowledge_goes_to_the_file_or_skill_that_loads_it_at_the_right_time():
    assert choose({"knowledge": 'convention'}) == {"mechanism": "claude-md", "reason": "always-known"}, '{"knowledge": \'convention\'}'
    assert choose({"knowledge": 'convention', "path_scoped": True}) == {"mechanism": "path-rule", "reason": "scoped-convention"}, '{"knowledge": \'convention\', "path_scoped": True}'
    assert choose({"knowledge": 'reference'}) == {"mechanism": "skill", "reason": "on-demand-reference"}, '{"knowledge": \'reference\'}'
    assert choose({"knowledge": 'procedure'}) == {"mechanism": "skill", "reason": "repeatable-procedure"}, '{"knowledge": \'procedure\'}'
    assert choose({"knowledge": 'reference', "path_scoped": True}) == {"mechanism": "skill", "reason": "on-demand-reference"}, '{"knowledge": \'reference\', "path_scoped": True}'
    assert choose({"path_scoped": True}) == {"mechanism": "builtin-tool", "reason": "built-in-covers"}, '{"path_scoped": True}'
    assert choose({"builtin_covers": True, "knowledge": 'procedure'}) == {"mechanism": "skill", "reason": "repeatable-procedure"}, '{"builtin_covers": True, "knowledge": \'procedure\'}'


def test_e4_a_plugin_carries_a_skill_hook_subagent_or_server_to_a_second_repository_and_nothing_else():
    assert choose({"knowledge": 'procedure', "repos": 1}) == {"mechanism": "skill", "reason": "repeatable-procedure"}, '{"knowledge": \'procedure\', "repos": 1}'
    assert choose({"guarantee": True, "repos": 1}) == {"mechanism": "hook", "reason": "must-hold-every-time"}, '{"guarantee": True, "repos": 1}'
    assert choose({"external_system": True, "repos": 1}) == {"mechanism": "mcp", "reason": "external-system"}, '{"external_system": True, "repos": 1}'
    assert choose({"noisy": True, "repos": 1}) == {"mechanism": "subagent", "reason": "isolate-context"}, '{"noisy": True, "repos": 1}'
    assert choose({"knowledge": 'procedure', "repos": 2}) == {"mechanism": "plugin", "reason": "shared-setup"}, '{"knowledge": \'procedure\', "repos": 2}'
    assert choose({"guarantee": True, "repos": 2}) == {"mechanism": "plugin", "reason": "shared-setup"}, '{"guarantee": True, "repos": 2}'
    assert choose({"external_system": True, "repos": 3}) == {"mechanism": "plugin", "reason": "shared-setup"}, '{"external_system": True, "repos": 3}'
    assert choose({"noisy": True, "repos": 2}) == {"mechanism": "plugin", "reason": "shared-setup"}, '{"noisy": True, "repos": 2}'
    assert choose({"knowledge": 'convention', "repos": 2}) == {"mechanism": "claude-md", "reason": "always-known"}, '{"knowledge": \'convention\', "repos": 2}'
    assert choose({"knowledge": 'convention', "path_scoped": True, "repos": 5}) == {"mechanism": "path-rule", "reason": "scoped-convention"}, '{"knowledge": \'convention\', "path_scoped": True, "repos": 5}'
    assert choose({"repos": 9}) == {"mechanism": "builtin-tool", "reason": "built-in-covers"}, '{"repos": 9}'


def test_e5_in_an_application_the_platform_may_supply_the_schema_and_only_a_remote_server_replaces_your_own_tool():
    assert choose({"surface": 'api'}) == {"mechanism": "api-tool", "reason": "own-schema-and-code"}, '{"surface": \'api\'}'
    assert choose({"surface": 'api', "builtin_covers": True, "external_system": True, "remote_server": True}) == {"mechanism": "builtin-tool", "reason": "provided-schema"}, '{"surface": \'api\', "builtin_covers": True, "external_system": True, "remote_server": True}'
    assert choose({"surface": 'api', "external_system": True, "remote_server": True}) == {"mechanism": "mcp", "reason": "remote-server"}, '{"surface": \'api\', "external_system": True, "remote_server": True}'
    assert choose({"surface": 'api', "external_system": True}) == {"mechanism": "api-tool", "reason": "own-schema-and-code"}, '{"surface": \'api\', "external_system": True}'
    assert choose({"surface": 'api', "remote_server": True}) == {"mechanism": "api-tool", "reason": "own-schema-and-code"}, '{"surface": \'api\', "remote_server": True}'
    assert choose({"surface": 'api', "guarantee": True, "knowledge": 'convention', "noisy": True, "repos": 4}) == {"mechanism": "api-tool", "reason": "own-schema-and-code"}, '{"surface": \'api\', "guarantee": True, "knowledge": \'convention\', "noisy": True, "repos": 4}'


def test_e6_an_unknown_value_is_an_error_and_a_missing_key_takes_its_default():
    assert choose({}) == {"mechanism": "builtin-tool", "reason": "built-in-covers"}, '{}'
    assert refused({"surface": 'cli'}), "{"surface": "cli"} must be an error"
    assert refused({"knowledge": 'tips'}), "{"knowledge": "tips"} must be an error"
    assert refused({"repos": 0}), "{"repos": 0} must be an error"
    assert refused({"repos": -1}), "{"repos": -1} must be an error"
    assert refused({"surface": 'api', "knowledge": 'tips'}), "{"surface": "api", "knowledge": "tips"} must be an error"
