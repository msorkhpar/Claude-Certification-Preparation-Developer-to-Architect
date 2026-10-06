import json
import os
import re
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))
HOME = "home/.claude"
PROJECT = "project/.claude"
BUILTIN_STYLES = {"Proactive", "Concise", "Explanatory", "Learning"}
STYLE_FIELDS = {"name", "description", "keep-coding-instructions", "force-for-plugin"}
CONTEXTS = {"Global", "Chat", "Autocomplete", "Settings", "Confirmation", "Tabs", "Help", "Transcript", "HistorySearch", "Task", "ThemePicker", "Attachments", "Footer",
            "MessageSelector", "DiffDialog", "DiffPanel", "ModelPicker", "EffortSlider", "Select", "Plugin", "Pane", "PaneField", "Agents", "Scroll"}
MODIFIERS = {"ctrl": "ctrl", "control": "ctrl", "shift": "shift", "alt": "alt", "opt": "alt", "option": "alt", "meta": "alt", "cmd": "cmd", "command": "cmd", "super": "cmd", "win": "cmd"}
NAMED_KEYS = {"escape", "esc", "enter", "return", "tab", "space", "up", "down", "left", "right", "pageup", "pagedown", "home", "end", "backspace", "delete", "wheelup", "wheeldown"}
RESERVED = {"ctrl+c", "ctrl+d", "ctrl+m", "ctrl+[", "ctrl+i", "ctrl+h"}
ACTION = re.compile(r"^[a-z][A-Za-z]*:[A-Za-z]+$")
STATUS_FIELDS = {"model.id", "model.display_name", "cwd", "workspace.current_dir", "workspace.project_dir", "workspace.added_dirs", "workspace.git_worktree", "cost.total_cost_usd",
                 "cost.total_duration_ms", "cost.total_api_duration_ms", "cost.total_lines_added", "cost.total_lines_removed", "context_window.total_input_tokens",
                 "context_window.total_output_tokens", "context_window.context_window_size", "context_window.used_percentage", "context_window.remaining_percentage",
                 "exceeds_200k_tokens", "fast_mode", "effort.level", "thinking.enabled", "rate_limits.five_hour.used_percentage", "rate_limits.seven_day.used_percentage",
                 "session_id", "session_name", "transcript_path", "version", "output_style.name", "vim.mode", "agent.name", "pr.number", "pr.url", "pr.review_state",
                 "worktree.name", "worktree.path", "worktree.branch"}
PERSONAL = re.compile(r"/home/|/Users/|C:\\Users|[\w.+-]+@[\w-]+\.[\w.-]+|sk-ant|TODO|FIXME")


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def read_json(rel):
    try:
        data = json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")
    assert isinstance(data, dict), f"{rel} must hold an object"
    return data


def front(rel):
    match = re.match(r"^---\n(.*?)\n---\n?(.*)$", read(rel), re.S)
    assert match, f"{rel} has no front matter"
    fields = {}
    for line in match.group(1).splitlines():
        key, _, value = line.partition(":")
        if key.strip():
            fields[key.strip()] = value.strip()
    return fields, match.group(2)


def style_files():
    folder = ROOT / HOME / "output-styles"
    return sorted(folder.glob("*.md")) if folder.is_dir() else []


def custom_styles():
    names = set()
    for path in style_files():
        fields, _ = front(f"{HOME}/output-styles/{path.name}")
        names.add(fields.get("name") or path.stem)
    return names


def key_problem(keystroke):
    """Why a keystroke string is not valid, or None. A chord is keystrokes separated by spaces."""
    for part in keystroke.split():
        pieces = part.split("+")
        key = pieces[-1].lower()
        if not (len(key) == 1 or key in NAMED_KEYS):
            return f"{part}: unknown key {key!r}"
        for mod in pieces[:-1]:
            if mod.lower() not in MODIFIERS:
                return f"{part}: unknown modifier {mod!r}"
    return None


def normal(keystroke):
    parts = keystroke.lower().split("+")
    return "+".join([MODIFIERS.get(m, m) for m in parts[:-1]] + [parts[-1]])


def test_m1_the_user_level_holds_a_style_a_status_line_and_a_keybindings_file_that_fit_together():
    settings = read_json(f"{HOME}/settings.json")
    style = settings.get("outputStyle")
    assert isinstance(style, str) and style, "the user settings select an output style"
    assert style in BUILTIN_STYLES | custom_styles(), f"outputStyle {style!r} names no built-in style and no style file"
    line = settings.get("statusLine")
    assert isinstance(line, dict) and line.get("type") == "command", "statusLine is an object of type command"
    command = line.get("command")
    assert isinstance(command, str) and command.startswith("~/.claude/"), "the command points at a script in the user's .claude folder"
    assert (ROOT / "home" / ".claude" / command[len("~/.claude/"):]).is_file(), f"{command} is not a file"
    bindings = read_json(f"{HOME}/keybindings.json").get("bindings")
    assert isinstance(bindings, list) and bindings, "keybindings.json holds a non-empty bindings array"
    assert all(isinstance(b, dict) and isinstance(b.get("context"), str) and isinstance(b.get("bindings"), dict) for b in bindings), "each block has a context and a bindings object"


def test_e1_the_style_file_keeps_the_coding_instructions_and_carries_only_documented_fields():
    files = style_files()
    assert files, "an output style file is needed"
    for path in files:
        fields, body = front(f"{HOME}/output-styles/{path.name}")
        unknown = set(fields) - STYLE_FIELDS
        assert not unknown, f"{path.name}: unknown front matter fields {sorted(unknown)} (a misspelled field is ignored without an error)"
        assert fields.get("name"), f"{path.name}: name the style"
        assert fields.get("description"), f"{path.name}: describe the style"
        assert fields.get("keep-coding-instructions") == "true", f"{path.name}: a style that only changes communication keeps the coding instructions"
        assert len([line for line in body.splitlines() if line.strip()]) >= 3, f"{path.name}: the instructions are too thin"


def test_e2_the_status_line_reads_only_fields_the_session_sends_and_refreshes_no_faster_than_every_second():
    line = read_json(f"{HOME}/settings.json").get("statusLine")
    assert isinstance(line, dict), "statusLine is an object"
    if "padding" in line:
        assert isinstance(line["padding"], int) and not isinstance(line["padding"], bool) and line["padding"] >= 0, "padding is a number of characters, 0 or more"
    if "refreshInterval" in line:
        assert isinstance(line["refreshInterval"], int) and not isinstance(line["refreshInterval"], bool) and line["refreshInterval"] >= 1, "refreshInterval is in seconds, 1 or more"
    command = line.get("command", "")
    assert command.startswith("~/.claude/"), "the command points at a script in the user's .claude folder"
    script = read(f"{HOME}/{command[len('~/.claude/'):]}")
    assert script.startswith("#!/bin/sh") or script.startswith("#!/usr/bin/env bash") or script.startswith("#!/bin/bash"), "the script starts with a shebang line"
    used = set()
    for expression in re.findall(r"jq -r '([^']*)'", script):
        used.update(re.findall(r"(?<![\w$\"])\.([A-Za-z_][\w.]*)", expression))
    assert used, "the script reads at least one field of the session JSON with jq"
    unknown = used - STATUS_FIELDS
    assert not unknown, f"fields the session does not send: {sorted(unknown)}"


def test_e3_the_keybindings_use_real_contexts_and_actions_free_keys_and_a_null_to_unbind():
    blocks = read_json(f"{HOME}/keybindings.json").get("bindings")
    assert isinstance(blocks, list) and blocks, "the file holds a bindings array"
    rebound = unbound = 0
    for block in blocks:
        assert block.get("context") in CONTEXTS, f"{block.get('context')!r} is not a context (names are case sensitive)"
        for keystroke, action in block.get("bindings", {}).items():
            problem = key_problem(keystroke)
            assert problem is None, problem
            assert normal(keystroke) not in RESERVED, f"{keystroke} is reserved and cannot be rebound"
            if action is None:
                unbound += 1
            else:
                assert isinstance(action, str) and ACTION.match(action), f"{action!r} is not a namespace:action name"
                rebound += 1
    assert rebound >= 1 and unbound >= 1, "rebind one key to an action and unbind another with null"


def test_e4_personal_settings_stay_in_the_user_files_and_the_project_file_stays_shared():
    shared = read_json(f"{PROJECT}/settings.json")
    for key in ("outputStyle", "statusLine"):
        assert key not in shared, f"{key} is personal: a shared project file would override every teammate's own choice"
    allow = shared.get("permissions", {}).get("allow") if isinstance(shared.get("permissions"), dict) else None
    assert isinstance(allow, list) and allow, "the shared file carries the team's permission rules"
    local = read_json(f"{PROJECT}/settings.local.json")
    style = local.get("outputStyle")
    assert isinstance(style, str) and style in BUILTIN_STYLES | custom_styles(), "the local file overrides the style for this project with an exact style name"


def test_e5_the_local_settings_file_is_kept_out_of_git():
    lines = [line.strip() for line in read("project/.gitignore").splitlines()]
    assert ".claude/settings.local.json" in lines or "**/.claude/settings.local.json" in lines, "list the local settings file in the ignore file"


def test_e6_no_file_holds_a_personal_path_an_address_a_key_or_an_unfinished_marker():
    files = [p for p in ROOT.rglob("*") if p.is_file()]
    assert files, "there are no files"
    for path in files:
        match = PERSONAL.search(path.read_text())
        assert match is None, f"{path.relative_to(ROOT)} holds {match.group(0)!r}"
