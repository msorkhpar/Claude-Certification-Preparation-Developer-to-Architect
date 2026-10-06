"""Three checks that keep a review prompt precise: lint a criterion for vague wording, check a set of few-shot examples, and measure the precision of each finding category from the verdicts developers gave.

The rules are the exam guide's for tasks 4.1 and 4.2 (explicit categorical criteria instead of "be conservative", two to four targeted examples that include an acceptable pattern, a category with a high false positive rate is switched
off while its prompt is improved) and the prompting guide's advice on examples (read on 2026-10-03: relevant, diverse and structured, three to five). No model is called.
"""
import logging

log = logging.getLogger(__name__)

VAGUE = ("be conservative", "high-confidence", "high confidence", "only important", "only significant", "if you are sure", "use your judgment")


def lint_criterion(criterion):
    """Rule ids a review criterion breaks: wording that names no pattern, a missing skip list, a severity level without a concrete example."""
    found = []
    for key in ("report", "skip"):
        text = criterion.get(key) or ""
        if not text.strip():
            found.append(f"no-{key}")
        elif any(phrase in text.lower() for phrase in VAGUE):
            found.append(f"vague-{key}")
    for level in ("high", "low"):
        if "`" not in (criterion.get("severity") or {}).get(level, ""):
            found.append(f"no-{level}-example")
    return found


def lint_examples(examples):
    """Rule ids for a set of few-shot examples: how many, whether both a finding and an acceptable pattern are shown, whether each says why."""
    found = []
    if not 2 <= len(examples) <= 4:
        found.append("two-to-four")
    if {e["verdict"] for e in examples} != {"report", "skip"}:
        found.append("both-verdicts")
    if any(not e.get("reason") for e in examples):
        found.append("reason-missing")
    return found


def trust(verdicts, min_reviewed=5, min_precision=0.5):
    """Per category: how many findings were reviewed, the share developers accepted, and whether to switch the category off while its prompt is improved."""
    table = {}
    for category, verdict in verdicts:
        row = table.setdefault(category, {"reviewed": 0, "accepted": 0})
        row["reviewed"] += 1
        row["accepted"] += 1 if verdict == "accepted" else 0
    for row in table.values():
        row["precision"] = round(row["accepted"] / row["reviewed"], 2)
        row["off"] = row["reviewed"] >= min_reviewed and row["precision"] < min_precision
    return table


VAGUE_CRITERION = {"report": "Be conservative and only flag important problems.", "skip": "", "severity": {"high": "Something serious.", "low": "A small thing."}}
GOOD_CRITERION = {"report": "A comment whose claimed behaviour contradicts the code.", "skip": "Minor style and patterns the codebase already uses.",
                  "severity": {"high": "A null dereference such as `user.profile.name` when `user` may be None.", "low": "A misleading name such as `total` for a count."}}


def main():
    print("vague criterion:", lint_criterion(VAGUE_CRITERION))
    print("good criterion:", lint_criterion(GOOD_CRITERION))
    one_side = [{"verdict": "report", "reason": "The comment says sum, the code multiplies."}, {"verdict": "report", "reason": ""}, {"verdict": "report", "reason": "Unchecked None."}, {"verdict": "report", "reason": "Off by one."}, {"verdict": "report", "reason": "Wrong key."}]
    print("five reports, one without a reason:", lint_examples(one_side))
    print("a report and a skip:", lint_examples([{"verdict": "report", "reason": "r"}, {"verdict": "skip", "reason": "s"}]))
    verdicts = [("bug", "accepted")] * 9 + [("bug", "dismissed")] * 1 + [("style", "accepted")] * 2 + [("style", "dismissed")] * 6 + [("naming", "dismissed")] * 3
    for category, row in trust(verdicts).items():
        print(f"{category}: reviewed {row['reviewed']}, precision {row['precision']}, switch off: {row['off']}")


if __name__ == "__main__":
    main()
