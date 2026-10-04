from skill_model import command_name, fork_agent, invocation, parse, pre_approved, removed, render, tool_status, winner

META = {"allowed-tools": "Bash(git tag *) Bash(git push origin *)", "disallowed-tools": "Edit Write(src/**)"}


def test_a_command_and_a_skill_with_one_name_create_one_slash_command():
    assert command_name(".claude/commands/deploy.md", {}) == "deploy" == command_name(".claude/skills/deploy/SKILL.md", {})
    assert command_name(".claude/skills/deploy/SKILL.md", {"name": "ship"}) == "ship"


def test_the_higher_level_wins_a_shared_name():
    assert winner({"project": "p", "personal": "u", "enterprise": "e"}) == "e"
    assert winner({"project": "p", "personal": "u"}) == "u"
    assert winner({"project": "p"}) == "p" and winner({}) is None


def test_who_can_start_a_skill():
    assert invocation({}) == {"you": True, "claude": True, "description_in_context": True}
    assert invocation({"disable-model-invocation": True}) == {"you": True, "claude": False, "description_in_context": False}
    assert invocation({"user-invocable": False}) == {"you": False, "claude": True, "description_in_context": True}


def test_allowed_tools_pre_approves_patterns_and_a_bare_name_approves_everything():
    assert pre_approved(META, "Bash", "git tag v1") and pre_approved(META, "Bash", "git push origin v1")
    assert not pre_approved(META, "Bash", "git push --force") and not pre_approved(META, "Bash", "rm -rf build")
    assert pre_approved({"allowed-tools": "Bash"}, "Bash", "rm -rf build") and pre_approved({"allowed-tools": "Read, Grep"}, "Grep")


def test_only_a_bare_disallowed_name_removes_a_tool():
    assert removed(META, "Edit") and not removed(META, "Write") and not removed({"disallowed-tools": "Edit(src/**)"}, "Edit")
    assert tool_status(META, "Edit") == "removed" and tool_status(META, "Bash", "git tag v1") == "pre-approved"
    assert tool_status(META, "Read") == "permission settings decide"


def test_arguments_fill_placeholders_in_shell_style():
    assert render("pr $0 by $1", "123 ana") == "pr 123 by ana"
    assert render("first=$ARGUMENTS[0] all=$ARGUMENTS", '"hello world" second') == 'first=hello world all="hello world" second'
    assert render("tag $version on $branch", "v2 main", ["version", "branch"]) == "tag v2 on main"
    assert render("only $1", "a") == "only $1"
    assert render("tag $version on $branch", "v2", ["version", "branch"]) == "tag v2 on "


def test_input_that_no_placeholder_receives_is_appended():
    assert render("Review the change.\n", "123") == "Review the change.\nARGUMENTS: 123\n"
    assert render("Review the change.\n", "") == "Review the change.\n"
    assert render("Tag $version\n", "v1", ["version"]) == "Tag v1\n"


def test_a_forked_skill_runs_in_a_subagent_that_defaults_to_general_purpose():
    assert fork_agent({"context": "fork"}) == "general-purpose" and fork_agent({"context": "fork", "agent": "Explore"}) == "Explore" and fork_agent({}) is None


def test_the_frontmatter_is_split_from_the_body():
    meta, body = parse("---\nname: x\narguments: [a, b]\n---\nHello $a\n")
    assert meta == {"name": "x", "arguments": ["a", "b"]} and body == "Hello $a\n"
