from policy_resolver import *


def test_a_higher_level_wins_and_managed_is_the_highest():
    settings, _ = effective({"managed": {"cleanupPeriodDays": 7}, "command line": {"cleanupPeriodDays": 14}, "project": {"cleanupPeriodDays": 30}, "user": {"cleanupPeriodDays": 60}})
    assert settings["cleanupPeriodDays"] == 7
    settings, _ = effective({"project": {"cleanupPeriodDays": 30}, "user": {"cleanupPeriodDays": 60}, "local": {"cleanupPeriodDays": 20}})
    assert settings["cleanupPeriodDays"] == 20


def test_lists_merge_across_levels_without_duplicates_until_a_lock_stops_it():
    open_layers = {"project": {"permissions.allow": ["a", "b"]}, "user": {"permissions.allow": ["b", "c"]}}
    assert effective(open_layers)[0]["permissions.allow"] == ["a", "b", "c"]
    locked = {"managed": {"allowManagedPermissionRulesOnly": True, "permissions.allow": ["m"]}, **open_layers}
    settings, notes = effective(locked)
    assert settings["permissions.allow"] == ["m"]
    assert notes == ["ignored permissions.allow from project: managed settings are the only source of permission rules",
                     "ignored permissions.allow from user: managed settings are the only source of permission rules"]


def test_a_key_only_an_organisation_can_set_is_ignored_anywhere_else():
    settings, notes = effective({"user": {"strictKnownMarketplaces": []}})
    assert "strictKnownMarketplaces" not in settings and notes == ["ignored strictKnownMarketplaces from user: a managed-only key"]
    settings, _ = effective({"managed": {"strictKnownMarketplaces": ["one"]}, "user": {"strictKnownMarketplaces": ["two"]}})
    assert settings["strictKnownMarketplaces"] == ["one"]


def test_the_lowest_effort_cap_wins_and_a_connector_ban_from_any_level_stands():
    settings, _ = effective({"managed": {"maxEffortLevel": "high"}, "user": {"maxEffortLevel": "low"}, "project": {"maxEffortLevel": "max"}})
    assert settings["maxEffortLevel"] == "low"
    settings, _ = effective({"managed": {"disableClaudeAiConnectors": False}, "project": {"disableClaudeAiConnectors": True}})
    assert settings["disableClaudeAiConnectors"] is True


def test_a_managed_model_list_refuses_every_other_choice_exactly():
    settings, _ = effective({"managed": {"availableModels": ["sonnet", "haiku"]}, "user": {"availableModels": ["opus"]}})
    assert settings["availableModels"] == ["sonnet", "haiku"]
    assert allowed_model("haiku", settings) == "allowed"
    assert allowed_model("opus", settings) == "refused (not in availableModels)"
    assert allowed_model("anything", {}) == "allowed"
