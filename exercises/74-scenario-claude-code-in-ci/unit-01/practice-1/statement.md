# Practice: the design of a pull request pipeline that uses Claude

A team wants Claude in its pipeline for three jobs: a review that blocks the merge of a pull request, an overnight report on technical debt, and the generation of tests for changed files. The
first draft has the usual mistakes. The review is a batch that a developer waits for, one pass reads fourteen files at once, the run is not headless, the review is allowed to run commands, the
criteria say "be conservative", and the test prompt does not mention the tests that exist. In this practice you correct the files. There is no program to write and no model is called: the tests
read your files. The pipeline's shape (job, audience, api, passes, session, context, tools, command) is this course's own description of a pipeline, not a product file; the flags, the schema and
the batch behaviour are the documented ones (checked 2026-10-04). It is in Python, TypeScript, Java and Kotlin; pick your language folder, open `starter/` and edit the files there; each language
folder holds its own copy of the files.

## What is already written, and what you write

The starter is the draft of the CI setup with six gaps cut out of it. Part of the criteria and of the test prompt is written, and the schema has its properties; the pipeline file holds three jobs whose settings are wrong. Each gap is marked by a `TODO n of 6` comment in the markdown files (JSON takes none) that says what is missing, with one example and the cases it unlocks. Delete the comment when the file is done. A file holds no program, so there is no logger to write here: the tests read the files and report the failing case with its message. Write them in this order:

1. `ci/pipeline.json`, the `pre-merge-review` job, unlocks `m1`, `e1`, `e2` and `e4`: real time, two passes, a fresh session with the earlier findings, read tools only.
2. `ci/pipeline.json`, the `debt-report` and `test-generation` jobs, unlock `m1` and `e3`: the API for the audience, and a headless, bounded command.
3. `ci/review.schema.json` unlocks `e3`: severity as a closed list, and the required fields.
4. `ci/review-criteria.md` unlocks `e5` and `e7`: the severity section with an example for each level, and no personal path.
5. `ci/testgen-prompt.md` unlocks `e6`: the existing tests and what a useful test is.
6. The review command in `ci/pipeline.json` unlocks `e3` and `e4`: `-p`, the JSON output, the schema, the turn limit and the read tools.

About twenty lines in all. The sections below describe the whole setup.

## What to write

The project folder holds a `ci/` folder with four files.

- `ci/pipeline.json`: a list of `jobs`. Each job has `name`, `kind` (`review`, `report` or `testgen`), `audience` (`waiting` when a person waits for the result, `scheduled` when nobody does),
  `api` (`realtime` or `batch`), `passes`, `session` (`fresh` or `shared`), `context` (what the prompt is given), `tools` and `command`. The jobs are `pre-merge-review`, `debt-report` and
  `test-generation`.
- `ci/review.schema.json`: the JSON schema of the review's answer: a list of findings.
- `ci/review-criteria.md`: the criteria the review follows.
- `ci/testgen-prompt.md`: the prompt that generates tests.

Rules the files must follow:

- A job a person waits for runs in real time; a job nobody waits for runs as a batch, whose command is a script that submits it.
- The review has two passes, in this order: `per-file`, then `integration`. It runs in a `fresh` session and its context includes `prior_findings`.
- Every command that starts `claude` has `-p` (or `--print`), `--output-format json` and `--max-turns` with a number. The review command also has `--json-schema` with a file whose `severity` is a
  closed list and whose findings require `file`, `line`, `severity`, `issue` and `suggestion`.
- The review has only `Read`, `Grep` and `Glob`, in its `tools` and in the `--allowedTools` of its command.
- The criteria have a `## Report` list and a `## Skip` list of at least two items each, and a `## Severity` list with a line for `high`, `medium` and `low`, each with `Example:`. They hold none of
  these vague phrases: "be conservative", "be careful", "only report important", "high confidence", "use good judgement".
- The test prompt holds `{{changed_files}}` and `{{existing_tests}}`, an `## A useful test` list of three items or more and a `## Do not write` list of two items or more.
- No file holds a home path, an address other than `example.com`, or a key.

## Why each part is there, and what you should see

1. **Who waits decides the API.** The Message Batches API costs half and may take up to 24 hours with no guarantee, which suits a report for the next morning and not a check a developer is waiting
   on. *You should see* the merge check in real time and the report as a batch.
2. **One pass over many files is unreliable.** Depth varies from file to file and the review contradicts itself. *You should see* a pass for each file for local issues and a separate pass for what
   crosses files.
3. **A reviewer is not the author.** A review in the session that wrote the code is biased toward it. *You should see* a fresh session, given the earlier findings, so that a re-run reports what is new.
4. **A run without a person must end by itself.** *You should see* `-p`, a turn limit, JSON out and read-only tools.
5. **Criteria are cases, not attitudes.** "Be conservative" does not say what to leave out. *You should see* the kinds of issue to report and to skip named, and an example for each severity.
6. **A test generator needs to know what exists.** *You should see* the existing tests in the prompt and a short definition of a useful test.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A job a person waits for is real time, a job nobody waits for is a batch |
| `e1` | The review has a pass per file, then an integration pass |
| `e2` | The review runs in a fresh session and is given the earlier findings |
| `e3` | Every `claude` command is headless, JSON and bounded; the schema has a closed severity and required fields |
| `e4` | The review has read tools only, in its list and in its command |
| `e5` | The criteria list what to report and skip, give an example for each severity and use no vague phrase |
| `e6` | The test prompt passes the changed files and the existing tests and defines a useful test |
| `e7` | No personal path, address or key in any file |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file (Java, Kotlin).
