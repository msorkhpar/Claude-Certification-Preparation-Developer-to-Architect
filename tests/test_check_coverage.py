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
