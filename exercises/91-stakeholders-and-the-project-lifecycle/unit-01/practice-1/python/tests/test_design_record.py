import os
import re
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds docs/design-record.md.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))

SECTIONS = ["Summary for the sponsor", "Decision statement", "Options considered", "Recommendation and trade-offs", "Accuracy by segment",
            "Service levels", "Pilot to scale", "Hand-off and monitoring"]
ROLE = "billing operations manager"
ERROR_COST, REVIEW_COST = 250, 5
MAX_LATENCY_MS, MIN_AVAILABILITY = 2000, 99.5
BAD_OWNERS = {"", "tbd", "everyone", "team", "n/a", "none"}
SHAPES = {"wrong amount", "outdated figure", "refusal", "made-up clause", "omitted exception"}
ERROR_COSTS = {"credit": 250, "complaint": 60, "status": 12}


def record():
    path = ROOT / "docs" / "design-record.md"
    assert path.is_file(), "docs/design-record.md is missing"
    return path.read_text()


def sections():
    """[(title, body)] for every level 2 heading, in order."""
    parts = re.split(r"^## (.*)$", record(), flags=re.M)
    return [(parts[i].strip(), parts[i + 1].strip()) for i in range(1, len(parts) - 1, 2)]


def body(title):
    for name, text in sections():
        if name == title:
            return text
    raise AssertionError(f"the section '{title}' is missing")


def table(text):
    """The rows of the first table in a section, without the header and the separator."""
    rows = []
    for line in text.splitlines():
        if line.startswith("|"):
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if not all(set(c) <= set("-: ") for c in cells):
                rows.append(cells)
    return rows[1:]


def number(cell):
    found = re.search(r"\d[\d,]*\.?\d*", cell)
    assert found, f"no number in '{cell}'"
    return float(found.group(0).replace(",", ""))


def test_m1_the_eight_sections_are_present_in_order_and_filled():
    assert [name for name, _ in sections()] == SECTIONS, "the sections are the eight headings, in order"
    for name, text in sections():
        assert len(text.split()) >= 15, f"'{name}' says too little"
    assert "TODO" not in record(), "replace every TODO"


def test_e1_the_decision_statement_carries_the_volume_the_latency_the_error_cost_and_the_accountable_role():
    text = body("Decision statement").lower()
    for needed in ("3,000", "2 seconds", ROLE):
        assert needed in text, f"the decision statement does not say '{needed}'"
    for value in (ERROR_COST, REVIEW_COST):
        assert re.search(rf"\b{value}\b", text), f"the decision statement does not give the cost {value}"


def test_e2_the_options_include_a_recommended_one_that_is_the_cheapest_to_meet_the_service_levels_and_a_reason_for_each_rejection():
    rows = table(body("Options considered"))
    assert len(rows) >= 4, "compare at least four options"
    for row in rows:
        assert len(row) == 5 and row[3] in ("recommended", "alternative", "rejected"), f"{row}: status is recommended, alternative or rejected"
        if row[3] == "rejected":
            assert len(row[4].split()) >= 5, f"{row[0]}: give a reason for the rejection"
    chosen = [r for r in rows if r[3] == "recommended"]
    assert len(chosen) == 1, "exactly one option is recommended"
    assert chosen[0][2].lower() == "yes", "the recommended option meets the service levels"
    cheapest = min(number(r[1]) for r in rows if r[2].lower() == "yes")
    assert number(chosen[0][1]) == cheapest, "the recommended option is the cheapest one that meets the service levels"


def test_e3_the_break_even_accuracy_is_98_percent_and_the_recommendation_states_the_cost():
    text = body("Recommendation and trade-offs")
    expected = 100 - (-(-100 * REVIEW_COST // ERROR_COST))
    found = re.search(r"at or above (\d+) percent", text)
    assert found and int(found.group(1)) == expected, f"state the rule 'at or above {expected} percent'"
    chosen = [r for r in table(body("Options considered")) if r[3] == "recommended"]
    assert chosen and f"{int(number(chosen[0][1])):,}" in text, "the recommendation states the monthly cost of the recommended option"


def test_e4_each_service_level_has_a_target_within_the_case_limits_and_a_named_owner():
    rows = {r[0].lower(): r for r in table(body("Service levels")) if len(r) == 4}
    pick = lambda word: next((r for name, r in rows.items() if word in name), None)
    latency, availability, accuracy = pick("latency"), pick("availability"), pick("accuracy")
    assert latency and availability and accuracy, "list latency, availability and accuracy"
    assert "ms" in latency[1] and number(latency[1]) <= MAX_LATENCY_MS, f"the latency target is at most {MAX_LATENCY_MS} ms"
    assert "percent" in availability[1] and number(availability[1]) >= MIN_AVAILABILITY, f"the availability target is at least {MIN_AVAILABILITY} percent"
    assert "credit" in accuracy[1].lower(), "the accuracy target is stated for the credit segment"
    for row in (latency, availability, accuracy):
        assert row[2] and row[3].lower() not in BAD_OWNERS, f"{row[0]}: say how it is measured and who owns it"


def test_e5_segments_are_listed_by_error_cost_and_handled_by_the_break_even():
    rows = table(body("Accuracy by segment"))
    assert {r[0] for r in rows} == set(ERROR_COSTS), "list the credit, complaint and status segments"
    costs = []
    for row in rows:
        assert len(row) == 6, f"{row} is missing a column"
        segment, accuracy, shape, cost, handling = row[0], number(row[2]), row[3], number(row[4]), row[5]
        assert 0 <= accuracy <= 100 and shape in SHAPES, f"{segment}: give an accuracy and one of the failure shapes {sorted(SHAPES)}"
        assert cost == ERROR_COSTS[segment], f"{segment}: the cost per error is {ERROR_COSTS[segment]}"
        assert handling == ("auto" if accuracy >= 98 else "reviewed"), f"{segment}: handling follows the break-even accuracy of 98 percent"
        costs.append(cost)
    assert costs == sorted(costs, reverse=True), "list the costliest segment first"


def test_e6_the_pilot_to_scale_table_has_four_assumptions_each_with_a_test_and_a_stop_trigger_with_a_number():
    rows = table(body("Pilot to scale"))
    assert len(rows) >= 4, "name at least four assumptions of the pilot"
    for row in rows:
        assert len(row) == 3 and all(row), f"{row}: give the assumption, how to test it and what stops the roll-out"
        assert re.search(r"\d", row[2]), f"{row[0]}: the stop trigger needs a number"


def test_e7_the_hand_off_names_an_owner_a_runbook_a_rollback_and_monitors():
    text = body("Hand-off and monitoring").lower()
    for needed in ("owner", "runbook", "rollback", "previous model"):
        assert needed in text, f"the hand-off does not mention '{needed}'"
    assert sum(signal in text for signal in ("refusals", "tokens per answer", "flagged", "latency")) >= 2, "name at least two monitors"


def test_e8_the_sponsor_summary_has_at_most_80_words_and_states_the_cost_and_the_decision_and_no_file_holds_personal_data():
    summary = body("Summary for the sponsor")
    assert len(summary.split()) <= 80, "the sponsor summary has at most 80 words"
    chosen = [r for r in table(body("Options considered")) if r[3] == "recommended"]
    assert chosen and f"{int(number(chosen[0][1])):,}" in summary, "the summary states the monthly cost"
    assert "decision" in summary.lower() and "`" not in summary, "the summary asks for a decision, in plain words"
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
