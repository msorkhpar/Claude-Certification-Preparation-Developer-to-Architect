import os
import re
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the repository folder.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))
HERE = Path(__file__).resolve()
sys.path.insert(0, str(next(p for p in (Path("/w/examples/40-workflow-lint/python"), *(HERE.parents[i] / "examples" / "40-workflow-lint" / "python" for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from miniyaml import parse_yaml, split_frontmatter  # noqa: E402
from workflow_lint import lint  # noqa: E402


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def workflow(rel):
    text = read(rel)
    try:
        wf = parse_yaml(text)
    except ValueError as error:
        raise AssertionError(f"{rel} is not readable YAML: {error}")
    assert isinstance(wf, dict) and isinstance(wf.get("jobs"), dict), f"{rel} needs a jobs map"
    return text, wf


def claude_step(job):
    steps = [s for s in job.get("steps", []) if str(s.get("uses", "")).startswith("anthropics/claude-code-action")]
    assert len(steps) == 1, "one claude-code-action step"
    return steps[0]


def test_m1_the_mention_workflow_answers_only_claude_comments_and_holds_no_key():
    text, wf = workflow(".github/workflows/claude.yml")
    on = wf.get("on")
    assert isinstance(on, dict) and set(on) == {"issue_comment", "pull_request_review_comment"}, "trigger on issue comments and review comments"
    assert all(v.get("types") == ["created"] for v in on.values()), "only new comments"
    job = wf["jobs"].get("claude")
    assert job, "the job is called claude"
    assert "@claude" in str(job.get("if", "")) and "github.event.comment.body" in str(job.get("if", "")), "start the runner only for comments that mention @claude"
    assert job["steps"][0].get("uses", "").startswith("actions/checkout@v"), "check the repository out first"
    step = claude_step(job)
    assert step["uses"] == "anthropics/claude-code-action@v1"
    assert step["with"].get("anthropic_api_key") == "${{ secrets.ANTHROPIC_API_KEY }}"
    perms = job.get("permissions") or {}
    assert {k: perms.get(k) for k in ("contents", "pull-requests", "issues", "id-token")} == {"contents": "write", "pull-requests": "write", "issues": "write", "id-token": "write"}
    assert lint(text) == [], lint(text)


def test_e1_the_review_workflow_reads_the_code_and_posts_the_review():
    text, wf = workflow(".github/workflows/review.yml")
    on = wf.get("on")
    assert isinstance(on, dict) and list(on) == ["pull_request"] and on["pull_request"].get("types") == ["opened", "synchronize", "ready_for_review", "reopened"]
    job = wf["jobs"].get("review")
    assert job, "the job is called review"
    perms = job.get("permissions") or {}
    assert perms.get("contents") == "read" and perms.get("id-token") == "write", "a review reads the code"
    uses = [s.get("uses", "") for s in job["steps"]]
    assert uses[0].startswith("actions/checkout@v") and uses[1] == "anthropics/claude-code-action@v1"
    with_ = claude_step(job)["with"]
    assert "/code-review" in str(with_.get("prompt")) and "--comment" in str(with_.get("prompt")), "run the review skill and post its findings"
    assert "mcp__github_inline_comment__create_inline_comment" in str(with_.get("claude_args")), "name the inline comment tool in claude_args"
    assert re.fullmatch(r"code-review@[\w-]+", str(with_.get("plugins"))) and with_.get("plugin_marketplaces"), "install the review plugin from a marketplace"
    assert lint(text) == [], lint(text)


def test_e2_each_prompt_file_carries_a_version_that_its_changelog_explains():
    prompts = sorted(p for p in (ROOT / "prompts").glob("*.md") if p.name != "CHANGELOG.md") if (ROOT / "prompts").is_dir() else []
    assert prompts, "prompts/ holds at least one prompt"
    changelog = read("prompts/CHANGELOG.md")
    headings = re.findall(r"(?m)^## (\S+)\s*$", changelog)
    assert headings, "the changelog has one ## heading per version, newest first"
    for prompt in prompts:
        fm, body = split_frontmatter(prompt.read_text())
        version = str(fm.get("version", ""))
        assert re.fullmatch(r"\d+\.\d+\.\d+", version), f"{prompt.name} needs a semantic version in its frontmatter"
        assert headings[0] == version, f"the newest changelog entry ({headings[0]}) must match {prompt.name} ({version})"
        assert body.strip()


def test_e3_the_review_guidance_and_the_git_workflow_are_files_the_reviewer_and_claude_read():
    review = read("REVIEW.md")
    for heading in ("Always check", "Skip"):
        m = re.search(rf"(?ms)^## {heading}\s*\n(.*?)(?=^## |\Z)", review)
        assert m, f"REVIEW.md needs a '## {heading}' section"
        bullets = re.findall(r"(?m)^- .+$", m.group(1))
        assert len(bullets) >= 2, f"'{heading}' needs at least two bullets"
        assert re.search(r"`[\w./*-]+/`|`[\w./*-]+\.\w+`", m.group(1)), f"'{heading}' names a path in backticks"
    memory = read("CLAUDE.md")
    assert re.search(r"`feature/<ticket>`", memory) and re.search(r"(?i)commit message", memory) and re.search(r"(?i)pull request", memory), "CLAUDE.md states the branch name, the commit message and the pull request rule"
    assert re.search(r"(?i)never commit to `main`", memory)


def test_e4_every_run_is_bounded_by_turns_time_and_concurrency():
    for rel, job_name in ((".github/workflows/claude.yml", "claude"), (".github/workflows/review.yml", "review")):
        _, wf = workflow(rel)
        job = wf["jobs"][job_name]
        assert isinstance(job.get("timeout-minutes"), int) and 1 <= job["timeout-minutes"] <= 30, f"{rel}: a timeout of 30 minutes or less"
        turns = re.search(r"--max-turns (\d+)", str(claude_step(job)["with"].get("claude_args", "")))
        assert turns and 1 <= int(turns.group(1)) <= 10, f"{rel}: --max-turns of 10 or fewer"
        assert (wf.get("concurrency") or {}).get("group"), f"{rel}: a concurrency group"
    assert workflow(".github/workflows/review.yml")[1]["concurrency"].get("cancel-in-progress") is True, "a new push replaces a running review"


def test_e5_no_file_holds_a_key_a_personal_path_or_an_address():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
