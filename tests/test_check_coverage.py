import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / "tools"))
import check_coverage as cc  # noqa: E402

MAP = (ROOT / "docs" / "EXAM-MAP.md").read_text(encoding="utf-8")
OUTLINE = (ROOT / "docs" / "COURSE-OUTLINE.md").read_text(encoding="utf-8")


def test_real_docs_pass():
    assert cc.check(MAP, OUTLINE) == []


def test_planted_unknown_code_is_reported():
    bad = OUTLINE.replace("| DV7; A3 |", "| DV7; A9.9 |", 1)
    assert bad != OUTLINE, "plant did not change the outline"
    assert any("A9.9" in p for p in cc.check(MAP, bad))


def test_planted_unknown_domain_is_reported():
    bad = OUTLINE.replace("| DV5; A1.1, A2.1 |", "| DV9; A1.1, A2.1 |", 1)
    assert bad != OUTLINE, "plant did not change the outline"
    assert any("DV9" in p for p in cc.check(MAP, bad))


def test_planted_uncovered_topic_is_reported():
    bad = OUTLINE.replace("A5.6; S3", "S3", 1)
    assert bad != OUTLINE, "plant did not change the outline"
    assert "topic A5.6 has no module" in cc.check(MAP, bad)


def test_planted_uncovered_scenario_is_reported():
    bad = OUTLINE.replace("| A4.3, A4.4; S6 |", "| A4.3, A4.4 |", 1).replace("| Capstone | S6 |", "| Capstone | A4.3 |", 1)
    assert bad.count("S6") == OUTLINE.count("S6") - 2, "plant did not change the outline"
    assert "topic S6 has no module" in cc.check(MAP, bad)


def test_x_is_defined_and_carried_by_modules():
    assert "X" in cc.defined_codes(MAP)
    carriers = [n for n, (_, cell) in cc.modules(OUTLINE).items() if "X" in cell.replace(";", ",").split(", ") or cell == "X"]
    assert carriers, "no module carries X"


def test_planted_x_without_definition_is_reported():
    bad_map = MAP.replace("| X Beyond the exam blueprints |", "| Y Beyond the exam blueprints |", 1)
    assert bad_map != MAP, "plant did not change the map"
    assert any("undefined code X" in p for p in cc.check(bad_map, OUTLINE))


def test_module_carrying_only_x_passes():
    ok = OUTLINE.replace("| AS; X |", "| X |", 1)
    assert ok != OUTLINE, "plant did not change the outline"
    assert cc.check(MAP, ok) == []
