from workflow_lint import BAD, GOOD, lint
from miniyaml import parse_yaml


def rules(text):
    return sorted(rule for _, rule, _ in lint(text))


def test_the_yaml_reader_handles_the_workflow_shapes():
    wf = parse_yaml(GOOD)
    assert wf["on"]["pull_request"]["types"] == ["opened", "synchronize"] and wf["jobs"]["review"]["steps"][1]["with"]["claude_args"] == "--max-turns 8"
    assert wf["jobs"]["review"]["timeout-minutes"] == 15 and wf["concurrency"]["cancel-in-progress"] is True


def test_the_good_workflow_has_no_findings():
    assert lint(GOOD) == []


def test_the_bad_workflow_is_flagged_for_every_documented_risk():
    assert rules(BAD) == ["action-version", "literal-key", "literal-key", "no-checkout", "no-concurrency", "no-max-turns", "no-timeout", "review-writes", "unguarded-trigger"]


def test_each_rule_is_independent():
    assert rules(GOOD.replace("      - uses: actions/checkout@v6\n        with:\n          fetch-depth: 1\n", "")) == ["no-checkout"]
    assert rules(GOOD.replace("    timeout-minutes: 15\n", "")) == ["no-timeout"]
    assert rules(GOOD.replace("          claude_args: --max-turns 8\n", "")) == ["no-max-turns"]
    assert rules(GOOD.replace("    permissions:\n      contents: read\n      pull-requests: write\n      id-token: write\n", "")) == ["no-permissions"]
    assert rules(GOOD.replace("contents: read", "contents: write")) == ["review-writes"]
    assert rules(GOOD.replace("${{ secrets.ANTHROPIC_API_KEY }}", "abc")) == ["literal-key"]
