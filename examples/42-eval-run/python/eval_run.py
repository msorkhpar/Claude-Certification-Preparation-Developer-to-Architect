"""An eval run, a success gate and a regression comparison, on a scripted classifier.

The Claude documentation on success criteria and evaluations (read on 2026-10-03) says to design evals that mirror the real task, edge
cases included, to automate the grading, and to judge several dimensions at once ("an F1 score of at least 0.85", "99.5% of outputs are
non-toxic"). This file runs six sentiment cases through two scripted versions of a prompt, grades them by exact match, and shows that a
better average can still hide a regression. The two models are lookup tables standing in for the application: no model is called.
"""
CASES = [
    {"id": "pos-1", "input": "Love it, works great", "expect": "positive", "tags": ["core"]},
    {"id": "neg-1", "input": "Broke after two days", "expect": "negative", "tags": ["core"]},
    {"id": "neu-1", "input": "It arrived on Tuesday", "expect": "neutral", "tags": ["core"]},
    {"id": "sarcasm-1", "input": "Oh great, another crash", "expect": "negative", "tags": ["edge"]},
    {"id": "mixed-1", "input": "Fast shipping but the screen is dim", "expect": "neutral", "tags": ["edge"]},
    {"id": "empty-1", "input": "", "expect": "neutral", "tags": ["edge"]},
]

PROMPT_V1 = {"Love it, works great": "positive", "Broke after two days": "negative", "It arrived on Tuesday": "neutral",
             "Oh great, another crash": "positive", "Fast shipping but the screen is dim": "positive", "": "Neutral"}
PROMPT_V2 = {**PROMPT_V1, "Oh great, another crash": "negative", "Fast shipping but the screen is dim": "neutral", "": "positive"}

CRITERIA = {"min_pass_rate": 0.8, "tags": {"edge": 0.75}}


def grade(case, output):
    """Code-graded: the answer must equal the expected label, ignoring case and surrounding white space."""
    return output.strip().lower() == case["expect"]


def run(cases, model):
    results = [{"id": c["id"], "passed": grade(c, model[c["input"]]), "tags": c["tags"]} for c in cases]
    rate = sum(r["passed"] for r in results) / len(results)
    by_tag = {}
    for r in results:
        for tag in r["tags"]:
            row = by_tag.setdefault(tag, [0, 0])
            row[0] += r["passed"]
            row[1] += 1
    return {"results": results, "pass_rate": rate, "by_tag": by_tag}


def gate(report, criteria):
    """Every dimension of the success criteria must hold, not only the average."""
    failures = []
    if report["pass_rate"] < criteria["min_pass_rate"]:
        failures.append("overall")
    for tag, minimum in criteria["tags"].items():
        passed, total = report["by_tag"][tag]
        if passed / total < minimum:
            failures.append(f"tag:{tag}")
    return failures


def compare(baseline, current):
    before = {r["id"]: r["passed"] for r in baseline["results"]}
    regressions = [r["id"] for r in current["results"] if before[r["id"]] and not r["passed"]]
    fixed = [r["id"] for r in current["results"] if not before[r["id"]] and r["passed"]]
    return {"regressions": regressions, "fixed": fixed}


def main():
    v1, v2 = run(CASES, PROMPT_V1), run(CASES, PROMPT_V2)
    for name, report in (("prompt v1", v1), ("prompt v2", v2)):
        edge = report["by_tag"]["edge"]
        print(f"{name}: pass rate {report['pass_rate']:.3f}, edge {edge[0]}/{edge[1]}, gate failures {gate(report, CRITERIA)}")
    diff = compare(v1, v2)
    print("v2 against v1: fixed", diff["fixed"], "regressions", diff["regressions"])
    print("average improved:", v2["pass_rate"] > v1["pass_rate"], "- safe to ship:", not diff["regressions"] and not gate(v2, CRITERIA))


if __name__ == "__main__":
    main()
