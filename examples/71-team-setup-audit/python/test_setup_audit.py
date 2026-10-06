from setup_audit import HERE, audit, glob_regex, rule_paths, rules_for, load


def test_a_double_star_crosses_folders_and_a_single_star_does_not():
    assert glob_regex("**/*.test.ts").match("a/b/c.test.ts") and glob_regex("**/*.test.ts").match("c.test.ts")
    assert glob_regex("src/*.ts").match("src/a.ts") and not glob_regex("src/*.ts").match("src/x/a.ts")
    assert not glob_regex("src/api/**/*.ts").match("src/models/a.ts")


def test_the_paths_of_a_rule_are_read_from_its_front_matter_and_absent_means_always():
    assert rule_paths('---\npaths:\n  - "a/**"\n  - b.md\n---\n\nbody\n') == ["a/**", "b.md"]
    assert rule_paths("# no front matter\n") is None and rule_paths("---\nname: x\n---\nbody\n") is None


def test_the_flawed_project_has_every_finding_and_the_fixed_one_has_none():
    assert audit(HERE / "project-before") == [
        "all-in-root: 5 sections in CLAUDE.md and no rule files", "test-uncovered: src/components/Button.test.tsx", "test-uncovered: src/api/orders.test.ts",
        "test-uncovered: src/models/order.test.ts", "no-shared-command", "env-readable", "bare-bash-allowed"]
    assert audit(HERE / "project-after") == []


def test_test_files_load_the_tests_rule_wherever_they_sit():
    rules = load(HERE / "project-after")[2]
    assert rules_for(rules, "src/components/Button.test.tsx") == ["components.md", "tests.md"]
    assert rules_for(rules, "docs/readme.md") == []


def test_a_rule_without_paths_and_a_rule_that_matches_nothing_are_flagged(tmp_path):
    (tmp_path / ".claude/rules").mkdir(parents=True)
    (tmp_path / ".claude/commands").mkdir()
    (tmp_path / ".claude/commands/review.md").write_text("review\n")
    (tmp_path / ".claude/settings.json").write_text('{"permissions": {"deny": ["Read(./.env)"]}}')
    (tmp_path / "files.txt").write_text("src/a.ts\n")
    (tmp_path / "CLAUDE.md").write_text("# Notes\n")
    (tmp_path / ".claude/rules/everywhere.md").write_text("# Always\n")
    (tmp_path / ".claude/rules/typo.md").write_text('---\npaths:\n  - "source/**/*.ts"\n---\n\nx\n')
    assert audit(tmp_path) == ["rule-loads-always: everywhere.md", "rule-matches-nothing: typo.md"]
