import json
import subprocess
import sys
from pathlib import Path

from hook_gate import dangerous, lint_agent, lint_skill, pre_tool_use

HERE = Path(__file__).resolve().parent


def bash(command):
    return pre_tool_use({"hook_event_name": "PreToolUse", "tool_name": "Bash", "tool_input": {"command": command}})


def test_other_forms_of_a_forbidden_command_are_still_refused():
    for command in ("git push origin main", "git -C . push", "git -c push.default=current push", "FOO=1 git push", "ls; git push", "bash -c 'git push'", "/usr/bin/git push"):
        code, out, _ = bash(command)
        assert code == 0 and json.loads(out)["hookSpecificOutput"]["permissionDecision"] == "deny", command
    assert dangerous("git status") is None and dangerous("git log --oneline") is None and dangerous("echo push") is None


def test_a_recursive_forced_delete_is_refused_in_every_spelling_and_a_plain_delete_is_not():
    assert all(dangerous(c) for c in ("rm -rf build", "rm -fr build", "rm -r -f build", "/bin/rm -rf x", "sh -c 'rm -rf x'", "rm --recursive --force x"))
    assert not any(dangerous(c) for c in ("rm build/old.txt", "rm -r build", "rm -f build/a"))


def test_a_protected_path_is_blocked_with_exit_two_and_a_reason_on_standard_error():
    code, out, err = pre_tool_use({"tool_name": "Write", "tool_input": {"file_path": "C:\\work\\.git\\config"}})
    assert (code, out) == (2, "") and "protected pattern '.git/'" in err
    assert pre_tool_use({"tool_name": "Edit", "tool_input": {"file_path": "/w/src/main.py"}}) == (0, "", "")
    assert pre_tool_use({"tool_name": "Read", "tool_input": {"file_path": "/w/.env"}}) == (0, "", "")


def test_the_file_works_as_a_hook_process():
    event = json.dumps({"tool_name": "Bash", "tool_input": {"command": "git push"}})
    run = subprocess.run([sys.executable, str(HERE / "hook_gate.py"), "--hook"], input=event, capture_output=True, text=True)
    assert run.returncode == 0 and json.loads(run.stdout)["hookSpecificOutput"]["permissionDecision"] == "deny"
    blocked = subprocess.run([sys.executable, str(HERE / "hook_gate.py"), "--hook"], input=json.dumps({"tool_name": "Edit", "tool_input": {"file_path": ".env"}}), capture_output=True, text=True)
    assert blocked.returncode == 2 and "Blocked" in blocked.stderr


def test_skill_and_agent_files_are_linted():
    assert len(lint_skill("---\nname: deploy\ndescription: Deploy it\nallowed-tools: Bash\n---\nx")) == 2
    assert lint_skill("---\nname: deploy\ndescription: Deploy it\ndisable-model-invocation: true\nallowed-tools: Bash(git add *) Read\n---\nx") == []
    assert lint_skill("---\nname: x\n---\nbody") == ["description is missing: Claude uses it to decide when to load the skill"]
    assert lint_agent("---\nname: r\ndescription: d\ntools: Read, Grep\nmemory: project\n---\nx") == []
    assert len(lint_agent("---\nname: r\ndescription: d\nmemory: team\npermissionMode: bypassPermissions\n---\nx")) == 3
