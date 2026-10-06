import itertools
import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
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
    ("s19", {"timing": "interval"}, "loop", "session-rhythm"),
    ("s20", {"timing": "interval", "presence": "away"}, "routine", "runs-unattended"),
    ("s21", {"timing": "event"}, "monitor", "push-not-poll"),
    ("s22", {"timing": "background"}, "background-task", "work-while-it-runs"),
    ("s23", {"presence": "pipeline"}, "headless-ci", "no-person-present"),
    ("s24", {"timing": "condition"}, "goal", "until-condition-holds"),
    ("s25", {"personal": "voice"}, "output-style", "response-voice"),
    ("s26", {"personal": "display"}, "status-line", "personal-display"),
    ("s27", {"timing": "interval", "lasts_days": 30, "local_files": True}, "desktop-task", "durable-and-local"),
    ("s28", {"personal": "keys"}, "keybinding", "personal-keys"),
]


def test_m1_every_situation_of_the_bank_gets_its_mechanism_and_its_reason():
    wrong = [sid for sid, s, mech, reason in BANK if choose(s) != {"mechanism": mech, "reason": reason}]
    assert wrong == [], f"these situations got the wrong mechanism or reason: {wrong}"


def test_e1_a_rule_that_must_hold_goes_to_a_hook_whatever_else_is_true():
    for s in sweep([("knowledge", ['none', 'convention', 'reference', 'procedure']), ("external_system", [False, True]), ("noisy", [False, True]), ("path_scoped", [False, True]), ("timing", ['none', 'interval', 'event', 'condition', 'background']), ("presence", ['session', 'pipeline', 'away']), ("personal", ["none", "voice"])], {"guarantee": True}):
        assert choose(s) == {"mechanism": "hook", "reason": "must-hold-every-time"}, s


def test_e2_an_outside_system_needs_a_server_and_noisy_work_alone_needs_a_subagent():
    for s in sweep([("knowledge", ['none', 'convention', 'reference', 'procedure']), ("noisy", [False, True]), ("path_scoped", [False, True]), ("timing", ['none', 'interval', 'event', 'condition', 'background']), ("presence", ['session', 'pipeline', 'away'])], {"external_system": True}):
        assert choose(s) == {"mechanism": "mcp", "reason": "external-system"}, s
    for s in sweep([("knowledge", ['none', 'convention', 'reference', 'procedure']), ("path_scoped", [False, True]), ("timing", ['none', 'interval', 'event', 'condition', 'background']), ("presence", ['session', 'pipeline', 'away'])], {"noisy": True}):
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
    assert refused({"surface": 'cli'}), '{"surface": "cli"} must be an error'
    assert refused({"knowledge": 'tips'}), '{"knowledge": "tips"} must be an error'
    assert refused({"repos": 0}), '{"repos": 0} must be an error'
    assert refused({"repos": -1}), '{"repos": -1} must be an error'
    assert refused({"surface": 'api', "knowledge": 'tips'}), '{"surface": "api", "knowledge": "tips"} must be an error'
    assert refused({"timing": "weekly"}), "{\"timing\": \"weekly\"} must be an error"
    assert refused({"presence": "cloud"}), "{\"presence\": \"cloud\"} must be an error"
    assert refused({"personal": "theme"}), "{\"personal\": \"theme\"} must be an error"
    assert refused({"lasts_days": 0}), "{\"lasts_days\": 0} must be an error"
    assert refused({"presence": "away", "local_files": True, "timing": "interval"}), "{\"presence\": \"away\", \"local_files\": True, \"timing\": \"interval\"} must be an error"
    assert refused({"surface": "api", "timing": "weekly"}), "{\"surface\": \"api\", \"timing\": \"weekly\"} must be an error"


def test_e7_a_pipeline_a_condition_a_long_command_and_an_event_are_not_intervals():
    assert choose({"presence": "pipeline"}) == {"mechanism": "headless-ci", "reason": "no-person-present"}, "{\"presence\": \"pipeline\"}"
    assert choose({"presence": "pipeline", "timing": "interval", "lasts_days": 30}) == {"mechanism": "headless-ci", "reason": "no-person-present"}, "{\"presence\": \"pipeline\", \"timing\": \"interval\", \"lasts_days\": 30}"
    assert choose({"timing": "condition", "lasts_days": 30}) == {"mechanism": "goal", "reason": "until-condition-holds"}, "{\"timing\": \"condition\", \"lasts_days\": 30}"
    assert choose({"timing": "condition", "presence": "away"}) == {"mechanism": "goal", "reason": "until-condition-holds"}, "{\"timing\": \"condition\", \"presence\": \"away\"}"
    assert choose({"timing": "background", "presence": "away"}) == {"mechanism": "background-task", "reason": "work-while-it-runs"}, "{\"timing\": \"background\", \"presence\": \"away\"}"
    assert choose({"timing": "event"}) == {"mechanism": "monitor", "reason": "push-not-poll"}, "{\"timing\": \"event\"}"
    assert choose({"timing": "event", "presence": "away"}) == {"mechanism": "routine", "reason": "runs-unattended"}, "{\"timing\": \"event\", \"presence\": \"away\"}"


def test_e8_an_interval_is_a_loop_in_the_session_and_a_routine_or_desktop_task_when_it_must_outlive_it():
    assert choose({"timing": "interval", "lasts_days": 7}) == {"mechanism": "loop", "reason": "session-rhythm"}, "{\"timing\": \"interval\", \"lasts_days\": 7}"
    assert choose({"timing": "interval", "lasts_days": 8}) == {"mechanism": "routine", "reason": "runs-unattended"}, "{\"timing\": \"interval\", \"lasts_days\": 8}"
    assert choose({"timing": "interval", "lasts_days": 8, "local_files": True}) == {"mechanism": "desktop-task", "reason": "durable-and-local"}, "{\"timing\": \"interval\", \"lasts_days\": 8, \"local_files\": True}"
    assert choose({"timing": "interval", "local_files": True}) == {"mechanism": "loop", "reason": "session-rhythm"}, "{\"timing\": \"interval\", \"local_files\": True}"
    assert choose({"timing": "interval", "presence": "away", "lasts_days": 30}) == {"mechanism": "routine", "reason": "runs-unattended"}, "{\"timing\": \"interval\", \"presence\": \"away\", \"lasts_days\": 30}"
    assert choose({"timing": "interval", "presence": "away"}) == {"mechanism": "routine", "reason": "runs-unattended"}, "{\"timing\": \"interval\", \"presence\": \"away\"}"
    assert choose({"timing": "interval", "presence": "session"}) == {"mechanism": "loop", "reason": "session-rhythm"}, "{\"timing\": \"interval\", \"presence\": \"session\"}"


def test_e9_a_personal_preference_goes_to_a_style_a_status_line_or_a_key_binding_and_knowledge_keeps_its_own_rules():
    assert choose({"personal": "voice"}) == {"mechanism": "output-style", "reason": "response-voice"}, "{\"personal\": \"voice\"}"
    assert choose({"personal": "display"}) == {"mechanism": "status-line", "reason": "personal-display"}, "{\"personal\": \"display\"}"
    assert choose({"personal": "keys"}) == {"mechanism": "keybinding", "reason": "personal-keys"}, "{\"personal\": \"keys\"}"
    assert choose({"personal": "voice", "knowledge": "convention"}) == {"mechanism": "output-style", "reason": "response-voice"}, "{\"personal\": \"voice\", \"knowledge\": \"convention\"}"
    assert choose({"personal": "keys", "knowledge": "reference", "repos": 4}) == {"mechanism": "keybinding", "reason": "personal-keys"}, "{\"personal\": \"keys\", \"knowledge\": \"reference\", \"repos\": 4}"
    assert choose({"personal": "display", "repos": 3}) == {"mechanism": "status-line", "reason": "personal-display"}, "{\"personal\": \"display\", \"repos\": 3}"
    assert choose({"personal": "none", "knowledge": "convention"}) == {"mechanism": "claude-md", "reason": "always-known"}, "{\"personal\": \"none\", \"knowledge\": \"convention\"}"
    assert choose({"personal": "voice", "timing": "interval"}) == {"mechanism": "loop", "reason": "session-rhythm"}, "{\"personal\": \"voice\", \"timing\": \"interval\"}"
    assert choose({"personal": "voice", "surface": "api"}) == {"mechanism": "api-tool", "reason": "own-schema-and-code"}, "{\"personal\": \"voice\", \"surface\": \"api\"}"
