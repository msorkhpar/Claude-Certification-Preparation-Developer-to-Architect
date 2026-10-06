import json
import os
import re
import sys
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))
HERE = Path(__file__).resolve()
for name in ("38-settings-layers/python", "39-hook-gate/python", "56-builtin-tools/python", "57-memory-loading/python"):
    sys.path.insert(0, str(next(p for p in (Path("/w/examples") / name, *(HERE.parents[i] / "examples" / name for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from builtin_tools import rule_tool  # noqa: E402
from memory_loading import glob_match, imports_of, rules_loaded, unresolved_imports  # noqa: E402
from miniyaml import split_frontmatter  # noqa: E402
from settings_layers import decide  # noqa: E402

T = ["Name test files `<module>.test.ts` or `<Component>.test.tsx`, next to the file they test.",
     "Every test builds its data from a fixture in `tests/fixtures/`; no ad-hoc data inline.",
     "Mock the network with `msw`; a test never calls a live service.",
     "Each test checks one behaviour and its name states that behaviour."]
A = ["Every handler under `src/api/` validates its input with the shared `parse()` helper before it uses it.",
     "Return errors as `{ code, message }` with the HTTP status from `src/api/status.ts`.",
     "Handlers are `async` and never swallow a rejected promise.",
     "Document each endpoint with an OpenAPI comment above its handler."]
R = ["Every resource carries the `owner` and `cost-centre` tags.",
     "Run `terraform fmt` and `terraform validate` before proposing a change.",
     "Pin provider versions with `~>` constraints."]
U = ["Commit messages use the imperative mood and stay under 72 characters in the first line.",
     "Run `npm test` before saying a task is done.",
     "Do not add dependencies without asking."]
GROUPS = {"U": U, "T": T, "A": A, "R": R}
P1 = "I prefer short answers with no preamble."
P2 = "My sandbox API is at http://localhost:4010 with the dev token from my shell profile."
FILES = ["src/api/users.ts", "src/api/orders.ts", "src/api/status.ts", "src/ui/Button.tsx", "src/ui/Button.test.tsx", "src/api/users.test.ts", "tools/cli/run.test.ts",
         "terraform/prod/main.tf", "terraform/modules/net/vpc.tf", "db/migrations/0001_init.sql", "README.md", "package.json"]
TESTS = ["src/ui/Button.test.tsx", "src/api/users.test.ts", "tools/cli/run.test.ts"]


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def rule_files():
    folder = ROOT / ".claude" / "rules"
    return sorted(str(p.relative_to(ROOT)) for p in folder.rglob("*.md")) if folder.is_dir() else []


def paths_of(text):
    """The `paths` of a rule as Claude Code reads it: a YAML list or a comma-separated string; None when there are none."""
    meta, _ = split_frontmatter(text)
    value = meta.get("paths")
    if value is None:
        return None
    return [p.strip() for p in value.split(",")] if isinstance(value, str) else list(value)


def rule_bodies():
    out = {}
    for rel in rule_files():
        _, body = split_frontmatter(read(rel))
        out[rel] = (paths_of(read(rel)), body)
    return out


def project_texts():
    """Every Markdown file outside .claude/, by project-relative path: what an @import can reach."""
    return {p.relative_to(ROOT).as_posix(): p.read_text() for p in ROOT.rglob("*.md") if ".claude" not in p.parts}


def launch_text():
    """CLAUDE.md and everything it imports: all of it is in context from the first message."""
    texts = project_texts()
    return "\n".join(texts[rel] for rel in ["CLAUDE.md", *imports_of("CLAUDE.md", texts)])


def context_for(touched):
    """The text in context at launch plus the rules that the touched files bring in."""
    bodies = rule_bodies()
    loaded = rules_loaded({rel: paths for rel, (paths, _) in bodies.items()}, touched)
    return "\n".join([launch_text()] + [bodies[rel][1] for rel in loaded])


def groups_in(text):
    found = set()
    for name, lines in GROUPS.items():
        have = [line in text for line in lines]
        assert all(have) or not any(have), f"group {name} is only partly there: move whole conventions, not some of them"
        if all(have):
            found.add(name)
    return found


def test_m1_the_conventions_of_each_area_load_for_exactly_the_files_that_area_governs():
    table = {("src/api/users.ts",): {"U", "A"}, ("src/ui/Button.tsx",): {"U"}, ("src/ui/Button.test.tsx",): {"U", "T"}, ("src/api/users.test.ts",): {"U", "A", "T"},
             ("terraform/prod/main.tf",): {"U", "R"}, ("README.md",): {"U"}, ("tools/cli/run.test.ts",): {"U", "T"}}
    got = {touched: groups_in(context_for(list(touched))) for touched in table}
    assert got == table, {k[0]: (got[k], v) for k, v in table.items() if got[k] != v}


def test_e1_the_root_file_is_short_and_holds_only_what_every_task_needs():
    root = read("CLAUDE.md")
    assert len(root.splitlines()) <= 50, "the root file is long: it is read in every session, so only what every task needs belongs in it"
    assert all(line in root for line in U), "the three rules that every task needs stay in the root file"
    for name in ("T", "A", "R"):
        assert not any(line in launch_text() for line in GROUPS[name]), f"the {name} conventions are in the launch context: an import loads at launch too"


def test_e2_the_testing_rule_follows_the_file_type_and_not_the_folder():
    testing = [paths for paths, body in rule_bodies().values() if T[0] in body]
    assert len(testing) == 1 and testing[0], "put the testing conventions in one rule file with paths"
    for file in TESTS:
        assert any(glob_match(p, file) for p in testing[0]), f"{file} is a test file and the rule must cover it"
    for file in [f for f in FILES if f not in TESTS]:
        assert not any(glob_match(p, file) for p in testing[0]), f"{file} is not a test file"


def test_e3_the_import_names_a_file_that_exists_and_loads_at_launch():
    read("CLAUDE.md")
    texts = project_texts()
    assert unresolved_imports("CLAUDE.md", texts) == [], "an import that names no file imports nothing: check the spelling"
    assert imports_of("CLAUDE.md", texts) == ["docs/standards/architecture.md"], "import the architecture notes with @docs/standards/architecture.md outside a code span"


def test_e4_personal_lines_sit_in_personal_files_and_the_local_file_is_ignored():
    user, local = read("user-memory.example.md"), read("CLAUDE.local.example.md")
    assert P1 in user and P2 not in user and P2 in local and P1 not in local, "a preference of yours goes in the user file, a sandbox note in the local file"
    shared = [read(rel) for rel in ["CLAUDE.md", "docs/standards/architecture.md"] + rule_files()]
    assert not any(P1 in text or P2 in text for text in shared), "a teammate would load your personal lines from the shared files"
    assert "CLAUDE.local.md" in [line.strip() for line in read(".gitignore").splitlines()], "CLAUDE.local.md must be ignored by git"


def test_e5_a_rule_that_must_always_hold_is_a_permission_rule_and_not_a_sentence():
    path = ROOT / ".claude" / "settings.json"
    assert path.is_file(), ".claude/settings.json is missing"
    settings = json.loads(path.read_text())
    assert decide(settings, rule_tool("Edit"), "db/migrations/0001_init.sql") == "deny", "memory is context, not enforcement: deny the edit in settings"
    assert decide(settings, rule_tool("Edit"), "src/api/users.ts") != "deny", "only the migrations are denied"


def test_e6_every_rule_scopes_itself_with_paths_that_are_valid_and_match_something():
    bodies = rule_bodies()
    assert len(bodies) >= 3, "write the three rule files: testing, API and Terraform"
    for rel, (paths, _) in bodies.items():
        assert paths, f"{rel} has no usable paths: without them (or with frontmatter that does not parse) the rule loads in every session"
        for pattern in paths:
            assert pattern.count("{") == pattern.count("}") and pattern.count("[") == pattern.count("]"), f"{rel}: the pattern {pattern!r} is not balanced"
            assert any(glob_match(pattern, f) for f in FILES), f"{rel}: the pattern {pattern!r} matches no file of the project: a bare folder name is not a glob"


def test_e7_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
