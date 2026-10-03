from settings_layers import decide, effective_settings, load_memory

LAYERS = {
    "managed": {"permissions": {"deny": ["Bash(curl *)"]}},
    "user": {"model": "sonnet", "permissions": {"allow": ["Bash(git status *)"]}},
    "project": {"model": "opus", "permissions": {"defaultMode": "bypassPermissions", "allow": ["Bash(npm run *)", "Bash(curl *)"], "ask": ["Bash(git push *)"], "deny": ["Read(./.env)"]}},
    "local": {"model": "haiku", "permissions": {"allow": ["Bash(git push *)"]}},
}


def test_a_higher_level_wins_a_scalar_key_and_lists_combine():
    s = effective_settings(LAYERS)
    assert s["model"] == "haiku"
    assert s["permissions"]["allow"] == ["Bash(git status *)", "Bash(npm run *)", "Bash(curl *)", "Bash(git push *)"]


def test_a_repository_file_cannot_set_bypass_permissions_and_its_allow_rules_wait_for_trust():
    assert "defaultMode" not in effective_settings(LAYERS)["permissions"]
    assert decide(effective_settings(LAYERS, trusted=False), "Bash", "npm run build") == "ask"
    assert decide(effective_settings(LAYERS), "Bash", "npm run build") == "allow"


def test_deny_beats_ask_beats_allow_whatever_the_level():
    s = effective_settings(LAYERS)
    assert decide(s, "Bash", "curl https://example.com") == "deny"
    assert decide(s, "Bash", "git push origin main") == "ask"
    assert decide(s, "Bash", "npm run build && git push origin main") == "ask"
    assert decide(s, "Bash", "rm -rf build") == "ask"


def test_wildcards_match_the_bare_command_but_not_a_longer_program_name():
    s = {"permissions": {"allow": ["Bash(ls *)", "Bash(npm run build)"]}}
    assert [decide(s, "Bash", c) for c in ("ls", "ls -la", "lsof", "npm run build", "npm run build --watch")] == ["allow", "allow", "ask", "allow", "ask"]


def test_path_rules_follow_the_rule_type_and_a_read_deny_also_blocks_edits():
    s = {"permissions": {"deny": ["Read(./.env)", "Read(secrets/**)"], "allow": ["Edit(src/**)"]}}
    assert [decide(s, "Read", p) for p in ("./.env", "sub/.env", "secrets/a.txt", "vendor/secrets/a.txt", "src/app.ts")] == ["deny", "deny", "deny", "deny", "allow"]
    assert [decide(s, "Edit", p) for p in (".env", "src/app.ts", "vendor/pkg/src/lib.js")] == ["deny", "allow", "ask"]


def test_memory_files_load_broad_to_specific_with_imports_and_without_backticked_ones():
    files = {"/m/CLAUDE.md": "m", "/u/CLAUDE.md": "u", "/r/CLAUDE.md": "See @docs/a.md and `@README`", "/r/docs/a.md": "A, then @b.md", "/r/docs/b.md": "B",
             "/r/s/CLAUDE.md": "s", "/r/s/CLAUDE.local.md": "l", "/r/o/CLAUDE.md": "o"}
    assert load_memory(files, "/r/s", "/m/CLAUDE.md", "/u/CLAUDE.md") == ["/m/CLAUDE.md", "/u/CLAUDE.md", "/r/CLAUDE.md", "/r/docs/a.md", "/r/docs/b.md", "/r/s/CLAUDE.md", "/r/s/CLAUDE.local.md"]


def test_accept_edits_mode_accepts_an_undecided_edit_but_not_a_deny_or_a_command():
    s = {"permissions": {"deny": ["Edit(.env)"]}}
    assert [decide(s, "Edit", p, mode="acceptEdits") for p in ("src/a.py", ".env")] == ["allow", "deny"]
    assert decide(s, "Bash", "make deploy", mode="acceptEdits") == "ask" and decide(s, "Edit", "src/a.py") == "ask"
