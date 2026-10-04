import os
import re
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds .claude/, personal/ and docs/.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))
HERE = Path(__file__).resolve()
for name in ("58-skill-model/python",):
    sys.path.insert(0, str(next(p for p in (Path("/w/examples") / name, *(HERE.parents[i] / "examples" / name for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from skill_model import command_name, fork_agent, invocation, parse, pre_approved, render, tool_status  # noqa: E402

REVIEW, TAG, STANDUP, MINE = ".claude/skills/review-pr/SKILL.md", ".claude/skills/release-tag/SKILL.md", ".claude/commands/standup.md", "personal/review-pr-mine/SKILL.md"
ALL = [REVIEW, TAG, STANDUP, MINE]


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def load(rel):
    return parse(read(rel))


def test_m1_the_review_skill_is_a_forked_skill_with_an_explicit_task_and_an_argument():
    meta, body = load(REVIEW)
    assert fork_agent(meta), "a review that prints a lot belongs in a forked context: set context: fork"
    assert str(meta.get("agent") or "").strip(), "name the subagent type in agent"
    assert re.search(r"(?m)^1\. ", body), "a forked skill is given its content as the task: write numbered steps, not guidelines"
    assert str(meta.get("argument-hint") or "").strip(), "show what to type with argument-hint"
    assert re.search(r"\$0|\$ARGUMENTS", body), "use the pull request number in the steps with $0"


def test_e1_the_release_skill_is_started_only_by_a_person_and_pre_approves_patterns():
    meta, _ = load(TAG)
    assert invocation(meta) == {"you": True, "claude": False, "description_in_context": False}, "a skill with side effects sets disable-model-invocation: true"
    items = re.findall(r"[^\s,(]+(?:\([^)]*\))?", str(meta.get("allowed-tools") or ""))
    assert items and all("(" in i for i in items), "pre-approve patterns such as Bash(git tag *), never a whole tool"
    assert pre_approved(meta, "Bash", "git tag -a v1.2.0 -m x") and pre_approved(meta, "Bash", "git push origin v1.2.0")
    assert not pre_approved(meta, "Bash", "git push --force origin main") and not pre_approved(meta, "Bash", "rm -rf build")


def test_e2_tools_are_taken_away_with_disallowed_tools_and_allowed_tools_only_pre_approves():
    meta, _ = load(REVIEW)
    assert tool_status(meta, "Edit") == "removed" and tool_status(meta, "Write") == "removed", "list Edit and Write by bare name in disallowed-tools"
    assert tool_status(meta, "Read") != "removed", "only the tools that change things are removed"
    assert pre_approved(meta, "Bash", "gh pr diff 12") and pre_approved(meta, "Bash", "gh pr view 12")
    assert not pre_approved(meta, "Bash", "gh pr merge 12") and not pre_approved(meta, "Bash", "rm -rf build"), "allowed-tools pre-approves the read-only gh commands and no more"


def test_e3_arguments_fill_the_placeholders_of_every_file():
    meta, body = load(REVIEW)
    out = render(body, "123", [])
    assert "$0" not in out and "gh pr diff 123" in out and "ARGUMENTS:" not in out
    meta, body = load(TAG)
    names = meta.get("arguments")
    assert names == ["version"], "declare the named argument in arguments"
    out = render(body, "v1.4.0", names)
    assert "$version" not in out and 'git tag -a v1.4.0 -m "Release v1.4.0"' in out and "ARGUMENTS:" not in out
    _, body = load(STANDUP)
    out = render(body, "ana")
    assert "$ARGUMENTS" not in out and "by ana since" in out and "ARGUMENTS:" not in out


def test_e4_every_file_creates_its_own_slash_command_and_the_personal_variant_has_a_new_name():
    names = {rel: command_name(rel, load(rel)[0]) for rel in ALL}
    assert names[STANDUP] == "standup" and names[REVIEW] == "review-pr" and names[TAG] == "release-tag"
    assert len(set(names.values())) == 4, f"two files create one command: {names}"
    assert names[MINE] != "review-pr", "a personal skill with the team's name replaces it for you: give the variant its own name"
    meta, _ = load(STANDUP)
    assert str(meta.get("description") or "").strip() and str(meta.get("argument-hint") or "").strip(), "the old command file keeps working: give it a description and a hint"


def test_e5_each_piece_of_guidance_lives_where_it_loads_the_way_it_is_used():
    rows = {}
    for line in read("docs/placement.md").splitlines():
        cells = [c.strip().strip("`") for c in line.strip().strip("|").split("|")]
        if len(cells) == 2 and cells[0] not in ("Guidance", "---"):
            rows[cells[0]] = cells[1]
    want = {"The team's pull request review checklist, run on demand": ".claude/skills/review-pr/SKILL.md",
            "My own variant of that review with extra style notes": "~/.claude/skills/review-pr-mine/SKILL.md",
            "Coding standards that apply to every task in the repository": "CLAUDE.md",
            "Test file conventions for test files in many folders": ".claude/rules/testing.md",
            "A release procedure with side effects that only a person starts": ".claude/skills/release-tag/SKILL.md"}
    assert rows == want, {k: (rows.get(k), v) for k, v in want.items() if rows.get(k) != v}


def test_e6_every_skill_says_when_to_use_it_and_stays_inside_the_listing_budget():
    for rel in (REVIEW, TAG, MINE):
        meta, _ = load(rel)
        description = str(meta.get("description") or "")
        assert re.search(r"(?:^|\. )Use when\b", description), f"{rel}: the description starts a sentence with Use when"
        assert len(description) + len(str(meta.get("when_to_use") or "")) <= 1536, f"{rel}: the listing keeps 1,536 characters of description and when_to_use"


def test_e7_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
