import json
import os
import re
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds CLAUDE.md, .claude/ and docs/.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))

UI, UI_TEST = "src/ui/Button.tsx", "src/ui/Button.spec.tsx"
HANDLER, HANDLER_TEST = "server/handlers/orders.ts", "server/handlers/orders.spec.ts"
DB, DB_TEST = "server/db/orderRepo.ts", "server/db/orderRepo.spec.ts"
DOC = "docs/readme.md"
# the words each convention is written with, the files it must reach and the files it must not reach
AREAS = [("hooks", [UI], [HANDLER, DB, DOC]), ("async/await", [HANDLER], [UI, DB, DOC]), ("repository", [DB], [UI, HANDLER, DOC]),
         ("describe", [UI_TEST, HANDLER_TEST, DB_TEST], [UI, HANDLER, DB, DOC])]
SAMPLES = [UI, UI_TEST, HANDLER, HANDLER_TEST, DB, DB_TEST, DOC]


def glob_regex(glob):
    out, i = "", 0
    while i < len(glob):
        if glob.startswith("**/", i):
            out, i = out + "(?:.*/)?", i + 3
        elif glob.startswith("**", i):
            out, i = out + ".*", i + 2
        elif glob[i] == "*":
            out, i = out + "[^/]*", i + 1
        elif glob[i] == "?":
            out, i = out + "[^/]", i + 1
        else:
            out, i = out + re.escape(glob[i]), i + 1
    return re.compile(out + r"\Z")


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def front_matter(text):
    head = re.match(r"---\n(.*?)\n---\n", text, re.S)
    return (head.group(1), text[head.end():]) if head else ("", text)


def rule_paths(head):
    listed = re.search(r"^paths:\s*\n((?:[ \t]+-[ \t]+.*\n?)+)", head + "\n", re.M)
    return [re.sub(r"^\s*-\s+", "", line).strip().strip("\"'") for line in listed.group(1).splitlines() if line.strip()] if listed else None


def rules():
    folder = ROOT / ".claude/rules"
    found = []
    for path in sorted(folder.glob("*.md")) if folder.is_dir() else []:
        head, body = front_matter(path.read_text())
        found.append((path.name, path.read_text().lower(), rule_paths(head)))
    return found


def reaches(paths, file):
    return paths is None or any(glob_regex(g).match(file) for g in paths)


def test_m1_each_convention_loads_for_exactly_the_files_of_its_area():
    for marker, expected, forbidden in AREAS:
        holders = [(name, paths) for name, text, paths in rules() if marker in text]
        assert holders, f"no rule file holds the {marker!r} convention"
        for file in expected:
            assert any(reaches(p, file) for _, p in holders), f"the {marker!r} convention does not load for {file}"
        for file in forbidden:
            assert not any(reaches(p, file) for _, p in holders), f"the {marker!r} convention loads for {file}, which is not its area"


def test_e1_the_root_file_is_short_and_holds_only_what_every_task_needs():
    text = read("CLAUDE.md")
    assert len([line for line in text.splitlines() if line.strip()]) <= 25, "keep the root file to 25 lines that matter"
    assert not [m for m, _, _ in AREAS if m in text.lower()], "area conventions belong in rule files, not in the root file"


def test_e2_the_review_command_is_shared_read_only_and_says_what_it_does():
    head, body = front_matter(read(".claude/commands/review.md"))
    description = re.search(r"^description:\s*(\S.*)$", head, re.M)
    allowed = re.search(r"^allowed-tools:\s*(.*)$", head, re.M)
    assert description, "the command needs a description"
    assert allowed, "the command lists the tools it pre-approves"
    tools = re.findall(r"[^\s,(]+(?:\([^)]*\))?", allowed.group(1))
    assert "Read" in tools and not [t for t in tools if t in ("Bash", "Edit", "Write", "MultiEdit")], "a review reads: no bare Bash, no edits"
    assert "git diff" in body, "the body says how to get the changes"


def test_e3_the_settings_protect_the_environment_file_and_approve_no_whole_tool():
    try:
        perms = json.loads(read(".claude/settings.json")).get("permissions", {})
    except json.JSONDecodeError as error:
        raise AssertionError(f".claude/settings.json is not valid JSON: {error}")
    assert "Read(./.env)" in perms.get("deny", []), "deny reading the environment file"
    assert not [r for r in perms.get("allow", []) if r in ("Bash", "Bash(*)", "Edit", "Write")], "a bare allow rule approves every call of that tool"


def test_e4_the_modes_table_sends_open_design_work_to_plan_mode_and_clear_small_work_to_direct():
    rows = {}
    for line in read("docs/working-modes.md").splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if line.startswith("|") and len(cells) >= 2 and cells[1].lower() in ("plan", "direct"):
            rows[cells[0].lower()] = cells[1].lower()
    for keyword, mode in (("typo", "direct"), ("monolith", "plan"), ("validation", "direct"), ("auth library", "plan"), ("rename", "direct"), ("unclear", "plan")):
        task = next((t for t in rows if keyword in t), None)
        assert task, f"the table has no row for the {keyword!r} task"
        assert rows[task] == mode, f"{keyword!r} should be {mode}"


def test_e5_every_rule_scopes_itself_with_a_glob_that_matches_a_file():
    found = rules()
    assert found, "write the rule files"
    for name, _, paths in found:
        assert paths, f"{name} has no paths list, so it loads in every session"
        assert all("*" in g for g in paths), f"{name}: a path without * is not a glob (a bare folder name matches no file)"
        assert any(reaches(paths, f) for f in SAMPLES), f"{name} matches none of the sample files"


def test_e6_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
