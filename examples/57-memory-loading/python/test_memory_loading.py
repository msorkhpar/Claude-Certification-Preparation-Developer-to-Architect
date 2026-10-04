from memory_loading import agents_md_read, context_lines, expand_braces, glob_match, imports_of, launch_files, on_demand_files, rules_loaded, unresolved_imports

TREE = {"CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md", "web/ui/CLAUDE.md", "api/CLAUDE.md", "AGENTS.md"}


def test_globs_stay_in_one_folder_unless_they_say_otherwise():
    assert glob_match("*.md", "README.md") and not glob_match("*.md", "docs/guide.md")
    assert glob_match("**/*.ts", "a.ts") and glob_match("**/*.ts", "a/b/c.ts") and not glob_match("**/*.ts", "a/b/c.tsx")
    assert glob_match("src/**/*", "src/a/b.py") and not glob_match("src/**/*", "lib/a.py")
    assert glob_match("src/components/*.tsx", "src/components/A.tsx") and not glob_match("src/components/*.tsx", "src/components/x/A.tsx")


def test_braces_expand_and_multiply():
    assert expand_braces("src/*.{ts,tsx}") == ["src/*.ts", "src/*.tsx"]
    assert len(expand_braces("{a,b}/{c,d}/*.{ts,tsx}")) == 8
    assert glob_match("src/**/*.{ts,tsx}", "src/ui/x.tsx")


def test_a_rule_without_paths_is_always_loaded_and_a_scoped_one_waits_for_a_match():
    rules = {"commit.md": None, "testing.md": ["**/*.test.tsx"], "terraform.md": ["terraform/**/*"]}
    assert rules_loaded(rules, []) == ["commit.md"]
    assert rules_loaded(rules, ["a/b/Button.test.tsx"]) == ["commit.md", "testing.md"]
    assert rules_loaded(rules, ["terraform/prod/main.tf", "x/Y.test.tsx"]) == ["commit.md", "testing.md", "terraform.md"]


def test_launch_loads_the_folders_above_and_on_demand_loads_the_folders_below():
    assert launch_files(TREE, "") == ["CLAUDE.md", "CLAUDE.local.md"]
    assert launch_files(TREE, "web") == ["CLAUDE.md", "CLAUDE.local.md", "web/CLAUDE.md"]
    assert on_demand_files(TREE, "", ["web/ui/Button.tsx"]) == ["web/CLAUDE.md", "web/ui/CLAUDE.md"]
    assert on_demand_files(TREE, "web", ["web/ui/Button.tsx", "api/x.py"]) == ["web/ui/CLAUDE.md"]


def test_agents_md_is_read_only_when_there_is_no_claude_md():
    assert agents_md_read({"AGENTS.md"}) is True
    assert agents_md_read(TREE) is False
    assert agents_md_read({"AGENTS.md", "CLAUDE.local.md"}) is False


def test_imports_follow_four_hops_and_skip_code_spans():
    texts = {"CLAUDE.md": "See @docs/a.md and `@code` and @missing.md", "docs/a.md": "@b.md", "docs/b.md": "@c.md", "docs/c.md": "@d.md", "docs/d.md": "@e.md", "docs/e.md": "end"}
    assert imports_of("CLAUDE.md", texts) == ["docs/a.md", "docs/b.md", "docs/c.md", "docs/d.md"]
    assert imports_of("CLAUDE.md", {"CLAUDE.md": "```\n@docs/a.md\n```", "docs/a.md": "x"}) == []


def test_an_import_saves_no_context():
    texts = {"CLAUDE.md": "line\n@docs/a.md", "docs/a.md": "1\n2\n3"}
    assert context_lines(["CLAUDE.md", *imports_of("CLAUDE.md", texts)], texts) == 5


def test_an_import_that_names_no_file_is_reported():
    texts = {"CLAUDE.md": "@docs/a.md and @docs/typo.md and `@code`", "docs/a.md": "x"}
    assert unresolved_imports("CLAUDE.md", texts) == ["docs/typo.md"]
