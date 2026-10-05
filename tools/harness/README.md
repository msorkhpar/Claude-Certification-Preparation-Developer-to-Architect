# What a run reports per case: the contract and the four harness files

The panel shows, per declared case, the failure message the test tool wrote (put input, expected and actual in
it), the lines the reader's code LOGGED while that case ran, and what it PRINTED. It reads them from the JUnit
report the practice already declares (`report.path`); nothing else is added to the wire, and a page only gets the
`--- detail {json} ---` lines when it asks (`X-Studyforge-Detail: 1`, sent by `practice.js` through the run client).

## The one marker

A logged line is `[log] LEVEL name: message` inside a case's own `<system-out>`, or `[log:<test name>] LEVEL
name: message` in the suite's (or `<testsuites>`') `<system-out>`/`<system-err>`, where `<test name>` is the
`<testcase name>` of the running test. Everything else is printed text. Debug logging never changes a verdict.

## The files, one per language, dropped beside the practice by the course adapter

| language | file(s) | what it does |
|---|---|---|
| Python | `pytest.ini` | pytest writes log + stdout + stderr per testcase into the JUnit XML, logs as `[log] ...` |
| TypeScript | `logger.ts`, `junit-file.mjs` | `logger(name)` writes `[log:<test>] ...` (the test hook gives the name); the reporter keeps stdout as `<system-out>` |
| Java, Kotlin | `StudyforgeLog.java` + `src/test/resources/junit-platform.properties` (`junit.jupiter.extensions.autodetection.enabled=true`) + `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension` (`studyforge.StudyforgeLog`) | a JUL handler writes every `System.Logger` line as `[log:<test>] ...` to stderr (Gradle records it in `<system-err>`) |

Learner code declares its own logger in one line (`logging.getLogger(__name__)`, `System.getLogger(...)`,
`logger("name")`) and never imports the harness. Tool limits: Gradle records printed text once per suite, so it
is shown once for the whole run, and the panel says so; Node's reporter has no per-test output either.

## Run shows the same report

Run uses the practice's `run_command`. Where that command also writes the report (set `run_command` to the
`test_command`), Run draws the same cases and text as Submit, and records nothing different.
