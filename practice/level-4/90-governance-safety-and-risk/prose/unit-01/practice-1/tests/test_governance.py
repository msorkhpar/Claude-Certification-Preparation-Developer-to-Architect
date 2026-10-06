import json
import os
import re
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))

BAD_OWNERS = {"", "tbd", "everyone", "team", "n/a", "none"}


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def load_json(rel):
    try:
        return json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")


def controls():
    return load_json("governance/controls.json").get("controls", [])


def register_rows():
    rows = []
    for line in read("docs/risk-register.md").splitlines():
        if line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if not all(set(c) <= set("-: ") for c in cells):
                rows.append(cells)
    return rows[1:]


def test_m1_no_control_that_guards_an_input_or_an_action_fails_open():
    found = controls()
    assert found, "list the controls"
    for c in found:
        assert c.get("on_failure") in ("hold", "proceed-flagged"), f"{c.get('id')}: on_failure is hold or proceed-flagged"
        if c.get("tier") == "high" or c.get("layer") in ("input", "action"):
            assert c["on_failure"] == "hold", f"{c['id']} guards an input or an action and must hold when it fails"


def test_e1_every_high_consequence_action_has_a_human_step_that_exists_and_holds():
    data = load_json("governance/controls.json")
    by_id = {c["id"]: c for c in data.get("controls", [])}
    actions = data.get("high_consequence_actions", [])
    assert actions, "name the high consequence actions"
    for action in actions:
        step = data.get("human_review", {}).get(action)
        assert step, f"{action} has no human review step"
        assert step in by_id, f"{action} names the control {step}, which is not defined"
        assert by_id[step].get("tier") == "high" and by_id[step].get("on_failure") == "hold", f"{step} must be a high tier control that holds"


def test_e2_the_automatic_threshold_is_at_least_95_and_a_high_consequence_action_is_never_automatic():
    routing = load_json("governance/routing.json")
    threshold = routing.get("auto_confidence_min")
    assert isinstance(threshold, int) and 95 <= threshold <= 100, "auto_confidence_min is a whole number from 95 to 100"
    assert routing.get("high_consequence_auto") is False, "a high consequence action is never automatic"
    assert routing.get("unsupported_answer") == "hold", "an unsupported answer is held"


def test_e3_retention_keeps_at_least_90_days_within_the_ceiling_and_stores_no_content():
    audit = load_json("governance/retention.json").get("audit", {})
    assert audit.get("floor_days", 0) >= 90, "the audit floor is at least 90 days"
    assert audit["floor_days"] <= audit.get("retain_days", -1) <= audit.get("ceiling_days", -1), "retain between the floor and the ceiling"
    assert audit.get("store_content") is False, "the audit log stores no content"
    assert audit.get("legal_hold_overrides_ceiling") is True, "a legal hold outranks the ceiling"


def test_e4_the_risk_register_names_a_control_and_an_owner_for_each_of_four_failure_modes():
    ids = {c["id"] for c in controls()}
    rows = register_rows()
    assert len(rows) >= 4, "the register has a row for each of four failure modes"
    for row in rows:
        assert len(row) >= 5, f"{row} is missing a column"
        assert row[2] in ids, f"the control {row[2]!r} is not defined in controls.json"
        assert row[3].lower() not in BAD_OWNERS, f"{row[1]}: name an owner"
    modes = " ".join(row[1].lower() for row in rows)
    for word in ("hallucination", "prompt injection", "privacy", "unfair"):
        assert word in modes, f"no row covers {word}"


def test_e5_users_are_told_that_ai_helped_and_no_file_holds_personal_data():
    assert re.search(r"told that ai (helped|assisted)", read("docs/risk-register.md"), re.I), "say that users are told that AI helped"
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits


def test_e6_erasure_removes_the_vault_mapping_within_30_days():
    erasure = load_json("governance/retention.json").get("erasure", {})
    assert erasure.get("remove_vault_mapping") is True, "erasure removes the map from tokens to people"
    assert 1 <= erasure.get("max_days_to_complete", 999) <= 30, "erasure completes within 30 days"
