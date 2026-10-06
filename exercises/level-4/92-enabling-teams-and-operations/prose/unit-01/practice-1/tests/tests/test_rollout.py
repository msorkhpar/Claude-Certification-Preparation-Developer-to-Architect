import json
import os
import re
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))

MANAGED_ONLY = ["allowManagedPermissionRulesOnly", "allowManagedHooksOnly", "allowManagedMcpServersOnly", "strictKnownMarketplaces", "disableSideloadFlags"]
EFFORT = ["low", "medium", "high", "xhigh", "max"]
PRECEDENCE = ["managed settings", "command line", "project local", "shared project", "user"]


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def load_json(rel):
    try:
        return json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")


def managed():
    return load_json("managed/managed-settings.json")


def section(title):
    parts = re.split(r"^## (.*)$", read("docs/rollout.md"), flags=re.M)
    for i in range(1, len(parts) - 1, 2):
        if parts[i].strip() == title:
            return parts[i + 1].strip()
    raise AssertionError(f"the section '{title}' is missing")


def table(text):
    rows = []
    for line in text.splitlines():
        if line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if not all(set(c) <= set("-: ") for c in cells):
                rows.append(cells)
    return rows[1:]


def test_m1_the_managed_file_locks_permissions_and_protects_the_environment_file():
    settings = managed()
    assert settings.get("allowManagedPermissionRulesOnly") is True, "make managed settings the only source of permission rules"
    assert settings.get("permissions", {}).get("disableBypassPermissionsMode") == "disable", "turn off bypass mode"
    assert "Read(./.env)" in settings.get("permissions", {}).get("deny", []), "deny reading the environment file"


def test_e1_keys_only_an_organisation_can_set_live_in_the_managed_file_and_never_in_the_project_file():
    project = load_json(".claude/settings.json")
    for key in MANAGED_ONLY:
        assert key not in project, f"{key} has no effect in the project file: move it to the managed file"
    assert managed().get("allowManagedHooksOnly") is True, "only managed hooks run"


def test_e2_plugins_come_only_from_the_companys_marketplace_and_cannot_be_sideloaded():
    settings = managed()
    sources = settings.get("strictKnownMarketplaces")
    assert isinstance(sources, list) and sources, "allow the company's marketplace; an empty list blocks every source, the official one too"
    for entry in sources:
        ok = (entry.get("source") == "github" and str(entry.get("repo", "")).startswith("example-org/")) or (entry.get("source") == "url" and str(entry.get("url", "")).startswith("https://plugins.example.com/"))
        assert ok, f"{entry} is not a source the company owns"
    assert settings.get("disableSideloadFlags") is True, "reject the flags that load plugins from a folder or an address"


def test_e3_only_the_managed_mcp_allowlist_applies_and_a_server_is_never_allowed_and_denied():
    settings = managed()
    assert settings.get("allowManagedMcpServersOnly") is True, "ignore allowlists in user, project and local files"
    allowed = settings.get("allowedMcpServers", [])
    assert allowed, "allow at least one server"
    for entry in allowed:
        assert len(entry) == 1 and next(iter(entry)) in ("serverName", "serverCommand", "serverUrl"), f"{entry}: exactly one of serverName, serverCommand or serverUrl"
        if "serverName" in entry:
            assert re.fullmatch(r"[A-Za-z0-9_-]+", entry["serverName"]), f"{entry['serverName']}: letters, numbers, hyphens and underscores only"
    names = {e["serverName"] for e in allowed if "serverName" in e}
    denied = {e["serverName"] for e in settings.get("deniedMcpServers", []) if "serverName" in e}
    assert not names & denied, f"{sorted(names & denied)} is allowed and denied: the denial wins"


def test_e4_the_model_choice_is_locked_by_a_list_and_the_effort_cap_is_at_most_high():
    settings = managed()
    models = settings.get("availableModels")
    assert isinstance(models, list) and models, "a managed model is only a default: list availableModels to lock the choice"
    if "model" in settings:
        assert settings["model"] in models, "the default model is one of the available models"
    cap = settings.get("maxEffortLevel")
    assert cap in EFFORT and EFFORT.index(cap) <= EFFORT.index("high"), "cap the effort at high or lower; max sets no cap"


def test_e5_the_group_spend_limits_add_up_to_the_organisation_limit_and_no_more():
    text = section("Spend limits")
    assert "usage credits" in text.lower(), "say that usage credits are on"
    rows = table(text)
    limit = lambda row: int(row[2].replace(",", ""))
    org = [limit(r) for r in rows if r[0] == "organization"]
    groups = [limit(r) for r in rows if r[0] == "group"]
    members = [limit(r) for r in rows if r[0] == "member"]
    assert len(org) == 1 and groups and members, "list the organisation, its groups and the member default"
    assert sum(groups) <= org[0], "the group limits add up to the organisation limit or less"
    assert members[0] <= min(groups), "a member's limit is within the smallest group's"


def test_e6_adoption_is_measured_by_outcomes_against_a_baseline_of_at_least_4_weeks():
    text = section("Adoption")
    baseline = re.search(r"Baseline:\s*(\d+) weeks", text)
    assert baseline and int(baseline.group(1)) >= 4, "take a baseline of at least 4 weeks before the rollout"
    block = re.search(r"Outcome targets:\n((?:- .*\n?)+)", text + "\n")
    assert block, "list the outcome targets"
    targets = [line[2:].lower() for line in block.group(1).splitlines()]
    assert len(targets) >= 2, "set at least two outcome targets"
    for target in targets:
        assert any(word in target for word in ("pull requests", "time to merge", "review", "defects")), f"'{target}' is not an outcome"
        assert not any(word in target for word in ("lines accepted", "prompts", "suggestions accepted")), f"'{target}' measures activity"


def test_e7_the_precedence_table_is_in_the_documented_order_and_no_file_holds_personal_data():
    rows = table(section("Precedence"))
    assert [r[1].lower() for r in rows] == PRECEDENCE, "the levels are managed, command line, project local, shared project and user, in that order"
    assert "per-group" in read("docs/rollout.md") and "not yet supported" in read("docs/rollout.md"), "say that server-managed settings cannot target a group yet"
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
