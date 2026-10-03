# Practice: Claude in a repository's workflow

A team wants Claude in its pull request workflow: it answers when someone writes `@claude` on an issue or a pull request, it reviews
every pull request, its prompts are versioned like code, and every run is bounded. All of that is plain files in the repository. In
this practice you write them for a small service, the invoice API, and tests read the project, using the course's own model of the
documented rules (`examples/40-workflow-lint`).

Pick your language folder (`python` or `typescript`), open `starter/` and edit the files there. The tests are the same in both languages.
The files are YAML and Markdown, so there is no Java or Kotlin edition: no YAML library is available offline for those two here, and
a second edition would test the same files. Nothing here calls GitHub or the network.

## The project

`starter/` holds a mention workflow that someone started: it uses the `@beta` version of the action, holds an API key written into the
file, and has none of the limits. `prompts/triage.md` has no version, `prompts/CHANGELOG.md` already has the `1.1.0` entry, and
`CLAUDE.md` says `TODO`.

## What to write

- `.github/workflows/claude.yml`, the mention workflow, named `claude`:
  - triggers: `issue_comment` and `pull_request_review_comment`, both only for `created`;
  - the job `claude` runs only when `github.event.comment.body` contains `@claude`, so a runner is not started for other comments;
  - a `timeout-minutes` of 30 or less and a `concurrency` group that is named after the issue or pull request number;
  - permissions `contents`, `pull-requests`, `issues` and `id-token` all `write`;
  - steps: `actions/checkout` first (any `v` version), then one `anthropics/claude-code-action@v1` step with the key as
    `${{ secrets.ANTHROPIC_API_KEY }}` and `claude_args` holding `--max-turns` of 10 or fewer.
- `.github/workflows/review.yml`, the review workflow, with the job `review`:
  - trigger: `pull_request` with the types `opened`, `synchronize`, `ready_for_review` and `reopened`, in that order;
  - permissions `contents: read` and `id-token: write` (a reviewer reads the code), with `pull-requests` and `issues` set to `read`;
  - checkout first, then the action step; its `prompt` runs the `/code-review` skill with `--comment` so findings are posted, its
    `claude_args` names `mcp__github_inline_comment__create_inline_comment` after `--allowedTools` and holds `--max-turns` of 10 or
    fewer, and it installs a plugin named `code-review@<marketplace>` with `plugin_marketplaces` and `plugins`;
  - a timeout of 30 minutes or less and a concurrency group named after the pull request number with `cancel-in-progress: true`,
    so a new push replaces a running review.
- `prompts/triage.md`: a semantic `version` in its frontmatter. `prompts/CHANGELOG.md` has one `## <version>` heading per version,
  newest first, and the newest heading equals the prompt's version. Add the entry for the version you set.
- `REVIEW.md`, the guidance the reviewer reads: a `## Always check` section and a `## Skip` section, each with at least two bullets,
  and each naming a path in backticks.
- `CLAUDE.md`: the branch name `feature/<ticket>` in backticks, a rule that a commit message states why the change is needed (the
  words "commit message"), the pull request rule (the words "pull request") and the sentence "never commit to `main`".
- No file in the project holds a key, a personal path or an address.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The mention workflow answers only `@claude` comments, checks the code out, uses the stable action version, holds no key and passes the course's linter |
| `e1` | The review workflow reads the code, runs the review skill with `--comment`, names the inline comment tool and installs the plugin |
| `e2` | Each prompt file carries a version that the changelog's newest entry matches |
| `e3` | The review guidance and the git workflow are files with the required sections |
| `e4` | Every run is bounded by turns, time and concurrency, and a new push replaces a running review |
| `e5` | No file holds a key, a personal path or an address |

Run the tests with the command in the language folder's `run.sh`.
