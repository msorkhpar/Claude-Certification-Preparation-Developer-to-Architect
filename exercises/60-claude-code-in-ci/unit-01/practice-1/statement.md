# Practice: a review job that runs Claude Code in CI and gates on its JSON

A team wants every pull request reviewed by Claude Code in CI. The first workflow is a single line that runs `claude` with a free-text prompt and prints prose
into a file: nobody can tell from the job's status whether the review ran, the key is typed into the file, the tool list lets the run edit the checkout, and
the review criteria are the sentence "be conservative". In this practice you write the job so that it is safe and its result can be trusted: the workflow, the schema
of the answer, the criteria file, the prompt, and the function that turns what Claude printed into a job status and a list of comments. Pick your language
folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and edit the files there. All four editions test the same behaviour. The logic is plain
data work on text and JSON, and the course checks it with a model of the documented command-line behaviour (`examples/60-ci-gate`, in your language): nothing
here starts Claude Code, needs a key or touches the network. The configuration files are the same in every language; the decision function is the code
part, `review_gate.py`, `reviewGate.ts`, `ReviewGate.java` (the class `ReviewGate`) or `ReviewGate.kt` (the object `ReviewGate`), with `reviewPrompt` and
`gate` as methods in Java and Kotlin and a provided `SchemaCheck` that you do not edit. The Java and Kotlin tests read the JSON with Jackson and the workflow
with its YAML module.

## What is already written, and what you write

The starter is a working review job with nine gaps cut out of it. Everything that is plumbing is written and correct: the prompt's instructions, its already-reported and existing-tests sections and its diff, the parsing of the run's output, the refusal of output that is not a JSON object, the subtype and `structured_output` checks, the schema check helper and the workflow's checkout and trigger. Each gap is marked `TODO k of N` with a comment that says what it receives and returns, with one example, and the cases it unlocks. A gap leaves a neutral value (nothing added, an empty list, `null`, the unchanged input), so the starter runs and fails the cases on an assertion. To debug a gap, log its input with the `log` line at the top of the file: a run shows the logged lines under the failing case. Write the gaps in this order (the TypeScript, Java and Kotlin names are the camel-case forms where a name is given):

1. The new-only sentence (unlocks `e4`): the prompt's instructions ask for findings that are new or still unaddressed, in the sentence `Report only findings that are new or still unaddressed.`.
2. The exit status (unlocks `e1`): a non-zero exit status of `claude` fails the job and adds the problem `claude exited with status N`.
3. The schema check (unlocks `e2`): each error of the answer against the schema becomes a problem that starts with `schema ` and the path of the error, such as `schema $.findings[0].line: expected integer`.
4. The comments (unlocks `m1`): a finding becomes a comment `{file, line, severity, body}` when its severity is at or above `min_severity` and its category is not in `disabled_categories`; the body is the issue, then ` Suggested fix: `, then the fix.
5. The blocking severity (unlocks `e3`): the job fails when a posted comment has a severity in `fail_on`, and only comments otherwise.
6. The schema file, in `review-schema.json` (unlocks `e5`): a draft-07 schema in which `category` and `severity` are closed enums (`bug`, `security`, `style`, `other`; `low`, `medium`, `high`), every finding requires `file`, `line`, `category`, `severity`, `issue`, `suggested_fix` and `detected_pattern` (a string), and nothing extra is allowed in a finding; no `minLength`, `maxLength`, `minimum` or `maximum`.
7. The workflow, in `.github/workflows/claude-review.yml` (unlocks `e6`): a `timeout-minutes`, the permissions `contents: read` and `pull-requests: write`, the key from `${{ secrets.ANTHROPIC_API_KEY }}`, and a `claude` command with `--bare -p`, `--output-format json`, `--json-schema`, `--max-turns` (at most 20) and `--allowedTools` limited to read-only tools.
8. The criteria, in `CLAUDE.md` (unlocks `e7`): explicit criteria in place of `Be conservative`: severity examples for medium and low next to the one for high, and the testing standards (where tests live, which fixtures they use, what makes a test valuable).
9. The personal path, in `CLAUDE.md` (unlocks `e8`): the line that names a path in a home folder is removed; no file holds a personal path, an address or a key.

`m1` needs gap 4. About twenty lines in all, spread over the code and the four files (the four language folders hold the same files). The steps below describe the whole job, so you can see how your gaps are used.

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
- In Java and Kotlin the same two are the methods `ReviewGate.reviewPrompt(diff, prior, existingTests)` and `ReviewGate.gate(stdout, exitCode, schema, policy)`:
  `prior` is a list of maps, `policy` is a map with the same three keys, `schema` is a Jackson `JsonNode`, and `gate` returns a map with `exit`, `comments` and
  `problems`.
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
