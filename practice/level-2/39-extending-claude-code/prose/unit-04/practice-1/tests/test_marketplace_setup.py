import json
import os
import re
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))
PLUGIN = "plugins/standards-kit"
ID = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]*")
SEMVER = re.compile(r"\d+\.\d+\.\d+")
SHA = re.compile(r"[0-9a-f]{40}")
TAG = re.compile(r"v\d+\.\d+\.\d+")
RESERVED = {"claude-code-marketplace", "claude-code-plugins", "claude-plugins-official", "anthropic-marketplace", "anthropic-plugins", "agent-skills",
            "anthropic-agent-skills", "life-sciences", "knowledge-work-plugins", "claude-for-legal", "claude-for-financial-services",
            "financial-services-plugins", "first-party-plugins", "claude-tag-plugins", "claude-community", "claude-plugins-community", "healthcare",
            "anthropic-plugin-directory", "claude-plugin-directory", "inline", "builtin", "skills-dir", "synced", "claude-plugin-test",
            "npm", "pip", "uv", "cargo", "github", "gh"}
SOURCE_TYPES = {"github", "url", "git-subdir", "npm", "archive", "command"}


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def read_json(rel):
    try:
        return json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")


def market():
    data = read_json(".claude-plugin/marketplace.json")
    assert isinstance(data, dict), "marketplace.json must hold an object"
    return data


def entries():
    plugins = market().get("plugins")
    assert isinstance(plugins, list) and plugins, "plugins must be a non-empty list"
    assert all(isinstance(p, dict) for p in plugins), "every plugin entry is an object"
    return plugins


def entry(name):
    found = [p for p in entries() if p.get("name") == name]
    assert found, f"no entry named {name}"
    return found[0]


def manifest():
    data = read_json(f"{PLUGIN}/.claude-plugin/plugin.json")
    assert isinstance(data, dict), "plugin.json must hold an object"
    return data


def front(rel):
    match = re.match(r"^---\n(.*?)\n---\n?(.*)$", read(rel), re.S)
    assert match, f"{rel} has no front matter"
    fields = {}
    for line in match.group(1).splitlines():
        key, _, value = line.partition(":")
        fields[key.strip()] = value.strip()
    return fields, match.group(2)


def test_m1_the_marketplace_file_names_an_owner_and_lists_each_plugin_by_a_source_that_resolves():
    data = market()
    assert isinstance(data.get("name"), str) and data["name"].strip(), "a marketplace needs a name"
    assert isinstance(data.get("owner"), dict) and str(data["owner"].get("name", "")).strip(), "the owner needs a name"
    assert str(data.get("description", "")).strip(), "describe the marketplace"
    names = [p.get("name") for p in entries()]
    assert all(isinstance(n, str) and n for n in names), "every entry needs a name"
    assert len(set(names)) == len(names), "entry names are unique"
    for p in entries():
        assert "source" in p, f"{p['name']} needs a source"
        source = p["source"]
        if isinstance(source, str):
            assert source.startswith("./") and ".." not in source and "\\" not in source, f"{p['name']}: a relative path starts with ./ and stays inside the marketplace"
            assert (ROOT / source).is_dir(), f"{p['name']}: {source} is not a folder of the marketplace"
            inner = json.loads((ROOT / source / ".claude-plugin" / "plugin.json").read_text())
            assert inner.get("name") == p["name"], f"{p['name']}: the entry name and the manifest name must be the same"
        else:
            assert isinstance(source, dict) and source.get("source") in SOURCE_TYPES, f"{p['name']}: a source object names a type"


def test_e1_names_are_valid_in_a_plugin_id_and_are_not_reserved_or_mistaken_for_official_ones():
    name = market().get("name", "")
    assert name == "example-org-tools", "call the marketplace example-org-tools"
    assert ID.fullmatch(name) and ".." not in name
    assert name not in RESERVED and not name.startswith("claudeai-")
    assert not re.search(r"claude|anthropic", name.lower()), "a name that mentions claude or anthropic risks being refused as an impersonation"
    plugin_names = sorted(p.get("name") for p in entries())
    assert plugin_names == ["db-tools", "lint-helper", "standards-kit"], "list standards-kit, lint-helper and db-tools"
    for n in plugin_names:
        low = n.lower()
        assert ID.fullmatch(n), f"{n}: letters, digits, dots, underscores and hyphens only"
        assert not low.startswith(("claude-", "anthropic-", "anthropics-", "cc-plugin-")) and low not in ("claude", "anthropic", "anthropics", "claude-code", "claude-mods"), f"{n} passes as an Anthropic plugin"
        assert not ("official" in low and re.search(r"claude|anthropic", low)), f"{n} passes as an official plugin"
    assert manifest().get("name") == "standards-kit"


def test_e2_the_plugin_keeps_only_its_manifest_in_the_manifest_folder_and_finds_its_files_through_the_plugin_root():
    assert sorted(p.name for p in (ROOT / PLUGIN / ".claude-plugin").iterdir()) == ["plugin.json"], "only plugin.json goes inside .claude-plugin"
    assert not (ROOT / PLUGIN / "CLAUDE.md").exists(), "an instruction file at the plugin root is not loaded as context"
    fields, body = front(f"{PLUGIN}/skills/changelog/SKILL.md")
    assert fields.get("name") == "changelog" and "Use when" in fields.get("description", "") and body.strip() and "TODO" not in body
    hooks = read_json(f"{PLUGIN}/hooks/hooks.json").get("hooks")
    assert isinstance(hooks, dict), "hooks.json wraps the event map in a top-level hooks key"
    groups = hooks.get("PreToolUse", [])
    assert len(groups) == 1 and groups[0].get("matcher") == "Edit|Write", "one PreToolUse group for Edit|Write"
    handler = groups[0]["hooks"][0]
    assert handler.get("type") == "command" and "${CLAUDE_PLUGIN_ROOT}" in handler.get("command", ""), "reach the script through ${CLAUDE_PLUGIN_ROOT}"
    assert (ROOT / PLUGIN / "scripts" / "protect.sh").is_file()
    servers = read_json(f"{PLUGIN}/.mcp.json").get("mcpServers", {})
    assert servers and all("${CLAUDE_PLUGIN_ROOT}" in " ".join(s.get("args", [])) for s in servers.values()), "every server reaches its code through ${CLAUDE_PLUGIN_ROOT}"
    for key in ("skills", "commands", "agents", "hooks", "mcpServers"):
        value = manifest().get(key)
        for path in ([value] if isinstance(value, str) else value if isinstance(value, list) else []):
            assert str(path).startswith("./"), f"a component path in plugin.json starts with ./ ({key})"


def test_e3_the_version_lives_in_the_manifest_alone_and_is_semantic():
    data = manifest()
    assert SEMVER.fullmatch(str(data.get("version", ""))), "give the plugin a semantic version in plugin.json"
    assert str(data.get("description", "")).strip()
    assert "version" not in entry("standards-kit"), "set the version in plugin.json or in the entry, not both"


def test_e4_each_source_kind_is_written_in_its_own_form_and_pinned_to_a_tag_and_a_commit():
    assert entry("standards-kit")["source"] == "./plugins/standards-kit"
    helper = entry("lint-helper")["source"]
    assert helper.get("source") == "github" and re.fullmatch(r"[\w.-]+/[\w.-]+", helper.get("repo", "")), "a github source takes owner/repo"
    sub = entry("db-tools")["source"]
    assert sub.get("source") == "git-subdir" and sub.get("path") == "tools/db-tools" and sub.get("url"), "a git-subdir source takes a url and a path"
    for p in entries():
        source = p["source"]
        if isinstance(source, str):
            continue
        if source["source"] == "url":
            assert re.match(r"(https?://|file://|git@)", source.get("url", "")), f"{p['name']}: a url source takes a full git url, not owner/repo"
        if source["source"] in ("github", "url", "git-subdir"):
            assert TAG.fullmatch(str(source.get("ref", ""))), f"{p['name']}: pin ref to a release tag such as v1.2.0"
            assert SHA.fullmatch(str(source.get("sha", ""))), f"{p['name']}: pin sha to a full 40-character lowercase commit"
        if source["source"] == "archive":
            assert re.fullmatch(r"[0-9a-fA-F]{64}", str(source.get("sha256", ""))), f"{p['name']}: pin an archive with sha256"


def test_e5_a_dependency_carries_a_range_and_one_from_another_marketplace_is_allowed_by_name():
    assert manifest().get("dependencies") == [{"name": "lint-helper", "version": "~1.2.0"}], "depend on lint-helper ~1.2.0"
    assert entry("db-tools").get("dependencies") == [{"name": "audit-logger", "marketplace": "shared-tools"}], "db-tools depends on audit-logger from shared-tools"
    assert market().get("allowCrossMarketplaceDependenciesOn") == ["shared-tools"], "the root marketplace must allow shared-tools"


def test_e6_the_team_settings_register_the_marketplace_and_enable_only_what_installs_from_it():
    settings = read_json(".claude/settings.json")
    markets = settings.get("extraKnownMarketplaces", {})
    assert list(markets) == [market().get("name")], "register the marketplace under its own name"
    source = markets[market()["name"]].get("source", {})
    assert source.get("source") == "github" and re.fullmatch(r"[\w.-]+/[\w.-]+", source.get("repo", "")), "a github source with an owner/name repo"
    enabled = settings.get("enabledPlugins", {})
    assert enabled and all(v is True for v in enabled.values())
    for plugin_id in enabled:
        name, _, marketplace = plugin_id.partition("@")
        assert marketplace == market()["name"], f"{plugin_id}: enable plugins from the marketplace you register"
        assert isinstance(entry(name)["source"], str), f"{plugin_id}: a plugin from an external source is not installed by the repository settings alone"
    assert "standards-kit@example-org-tools" in enabled


def test_e7_renames_lead_every_former_name_to_a_current_plugin_or_to_null():
    renames = market().get("renames")
    assert isinstance(renames, dict) and renames.get("std-kit") == "standards-kit" and "old-linter" in renames and renames["old-linter"] is None
    current = {p["name"] for p in entries()}
    for old in renames:
        assert old not in current, f"{old} is still listed as a plugin"
        seen, step = {old}, renames[old]
        while step is not None and step not in current:
            assert step in renames and step not in seen, f"the rename of {old} does not end at a plugin or at null"
            seen.add(step)
            step = renames[step]


def test_e8_no_file_is_left_unfinished_or_holds_a_personal_path_an_address_or_a_key():
    checked = 0
    for path in sorted(p for p in ROOT.rglob("*") if p.is_file()):
        text = path.read_text()
        checked += 1
        rel = path.relative_to(ROOT)
        assert "TODO" not in text, f"{rel} still has a TODO"
        assert not re.search(r"/home/|/Users/|[A-Za-z]:\\\\Users|sk-ant-", text), f"{rel} holds a personal path or a key"
        assert all(a.endswith("@example.com") for a in re.findall(r"[\w.+-]+@[\w-]+\.[\w.-]+", text)), f"{rel} holds an address that is not a placeholder"
    assert checked >= 8
