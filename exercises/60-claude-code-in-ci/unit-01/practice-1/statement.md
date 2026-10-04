# Practice: a review job that runs Claude Code in CI and gates on its JSON

A team wants every pull request reviewed by Claude Code in CI. The first workflow is a single line that runs `claude` with a free-text prompt and prints prose
into a file: nobody can tell from the job's status whether the review ran, the key is typed into the file, the tool list lets the run edit the checkout, and
the review criteria are the sentence "be conservative". In this practice you write the job so that it is safe and its result can be trusted: the workflow, the schema
of the answer, the criteria file, the prompt, and the function that turns what Claude printed into a job status and a list of comments. Pick your language
folder (`python` or `typescript`), open `starter/` and edit the files there. Both editions test the same behaviour. The logic is plain data work on text and
JSON, and the course checks it with a model of the documented command-line behaviour: nothing here starts Claude Code, needs a key or touches the network. The
configuration files have no Java or Kotlin edition (no YAML reader is available offline for those two here); the decision function is the code part and is
written in Python and TypeScript.

## What to write

- `.github/workflows/claude-review.yml`:
  - runs `claude` headless with `-p`, `--bare`, `--output-format json`, `--json-schema` (passing the schema file), `--max-turns` (at most 20) and the project
    criteria through `--append-system-prompt-file CLAUDE.md` (a bare run does not read the project file by itself);
  - gives the run read-only tools with `--allowedTools`, such as `Read,Grep,Glob`: no bare `Bash`, no `Edit`, no `Write`; and `contents: read` permission, with
    `pull-requests: write` only if the job posts comments;
  - has a `timeout-minutes` for the job;
  - takes the key from a secret (`${{ secrets.ANTHROPIC_API_KEY }}`), never a literal (the starter has one).
- `review-schema.json`: a draft-07 schema of `{"findings": [...]}` in which every finding requires `file`, `line`, `category`, `severity`, `issue`, `suggested_fix`
  and `detected_pattern`, `category` and `severity` are closed enums (`bug`, `security`, `style`, `other`; `low`, `medium`, `high`), and nothing extra is allowed.
  Keep to the features the structured outputs support: no `minLength` or `maxLength` and no numeric `minimum` or `maximum` (the API rejects them with a 400 error), and optional fields only where the value can really be absent.
- `CLAUDE.md`: explicit review criteria. Say what to report (with categories), what to skip, a severity definition with a concrete code example for each
  level, and the testing standards (where tests live, which fixtures they use, what makes a test valuable). "Be conservative" is not a criterion.
- `review_prompt(diff, prior, existing_tests)`: instructions first, then the findings already reported, then the existing tests, then the diff last; it asks for new or
  unaddressed issues only, and says not to repeat a finding or suggest a test that exists.
- `gate(stdout, exit_code, schema, policy)`: returns `{"exit", "comments", "problems"}`.
  - A run that failed in any way fails the job: a non-zero status, output that is not a JSON object, `is_error` or a subtype other than `success`, no
    `structured_output`, or an answer that breaks the schema (name the path of the problem). It never passes by saying nothing.
  - For a valid answer, keep the findings at or above `min_severity` and outside `disabled_categories` as comments; the job fails when a kept comment has a severity
    in `fail_on`, and only comments otherwise.
- No file holds a personal path, an email address or a key.

## Why each part is there, and what you should see

1. **Headless with a contract.** `-p` runs without a person; `--output-format json` with `--json-schema` returns one object whose `structured_output` field holds the
   answer. *You should see* a workflow whose output is machine-readable and whose command carries every flag a CI run needs.
2. **Fail closed.** A review job that passes when Claude hit its turn limit, printed an error or printed nothing is worse than no job. *You should see* every failed run
   give a non-zero exit and a problem that says why.
3. **Schema and severity.** Closed enums and required fields make the answer checkable; the severity decides whether a finding comments or blocks. *You should see* a
   high finding fail the job, a low one only comment, and a style finding in a disabled category dropped.
4. **Context that CI does not have.** A bare run skips CLAUDE.md, so the criteria are passed by hand; a new run does not remember the last, so the prompt lists what was
   reported and what tests exist. *You should see* the prompt carry both lists and put the diff last.
5. **Least privilege.** A review reads. *You should see* read-only tools, a read permission on the contents, a job timeout and a secret reference.
6. **Explicit criteria.** A stated floor and a code example per severity are what move a reviewer's precision. *You should see* report and skip lists and an example at each level.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A valid run produces the comments above the floor and outside the disabled categories |
| `e1` | A failed run (non-zero status, not JSON, turn limit, no output) fails the job and says why |
| `e2` | An answer that breaks the schema fails the job and names the path of the problem |
| `e3` | A finding at a failing severity blocks, and lower ones only comment |
| `e4` | The prompt lists earlier findings and existing tests, asks for new or unaddressed issues only, and ends with the diff |
| `e5` | The schema file is valid draft-07, requires every field of a finding and keeps enums closed |
| `e6` | The workflow runs Claude headless with JSON output, a schema, a turn limit, read-only tools and the context file, with a timeout and a secret key |
| `e7` | The criteria file says what to report and skip, and gives a severity example at each level |
| `e8` | No file holds a personal path, an address or a key |

Run the tests with the command in the language folder's `run.sh`.
