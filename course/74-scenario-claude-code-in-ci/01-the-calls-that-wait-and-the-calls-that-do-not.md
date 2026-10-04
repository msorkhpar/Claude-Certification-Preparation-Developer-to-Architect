# The calls that wait and the calls that do not

**Level:** Architect · **Module 74:** Scenario: Claude Code in CI · **Page 1 of 2**
**Exams:** A3.6; S5

**After this page you can** decide for each job of a pipeline whether it runs in real time or as a batch by asking who waits for it, make a Claude Code run end by itself in a pipeline, bound it with a turn limit and a list of read-only tools, and audit a pipeline description for the mistakes the exam's scenario is built on.

Checked on 2026-10-04 against the Claude Code documentation pages "Run Claude Code programmatically" and "Best practices for Claude Code" (the pages name Claude Code version 2.1.286), the Claude API documentation page on batch processing, and the Architect exam guide (version 1.0, scenario 5 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin and reads two small JSON files; it does not call Claude. This page is a capstone: it uses modules 21, 60 and 61 and puts them in the order the exam asks about.

## Why it matters

Scenario S5 of the Architect exam is Claude Code inside a CI/CD pipeline: automated code review, generated test cases and feedback on pull requests, with prompts that give actionable feedback and few false positives. Its primary domains are Claude Code configuration and workflows, and prompt engineering with structured output. The questions begin with the plumbing and then move to the design. A step hangs because it waits for a person who is not there. A manager wants the discount of batch processing for everything. A review of many files contradicts itself. Each is answered by a rule about where a call sits in the pipeline, and the wrong options are rules that fit another place.

## The idea

### The scenario in plain words

A team runs three Claude jobs. A review of each pull request that has to finish before the merge button works. A report on technical debt that is generated overnight and read the next morning. And the generation of tests for the files a pull request changes. The team wants the bill to fall, the review to be useful instead of noisy, and the pipeline never to hang or to turn green by accident.

### The requirement decides the design

| The requirement | The design | Why the tempting choice fails |
|---|---|---|
| The step must run with nobody at the keyboard | `claude -p "..."` | There is no headless environment variable and no `--batch` flag on the command line; `-p` is the documented way to run without a person. Redirecting standard input from an empty file is a Unix workaround that does not change how Claude Code runs |
| Lower the cost of the overnight report | The Message Batches API, from a script | Nothing is wrong with it: nobody waits for the report |
| Lower the cost of the check that blocks the merge | Keep it in real time | A batch has no latency guarantee, and a developer is waiting |
| The run must end whatever the model does | A turn limit and a list of tools in the command | A prompt that asks the run to be quick is a request, not a limit |
| The job must fail when no valid answer came back | A gate on the exit status and the JSON (module 60) | A green job that said nothing is the failure the gate prevents |

### Run without a person

In a pipeline nobody can answer a prompt, so a run that would ask a question stops there for ever. The way out is `-p` (or `--print`): it processes the prompt, prints the result and exits. There is no headless environment variable and no `--batch` flag on the command line; `-p` is the documented way to run without a person. Redirecting standard input from an empty file is a Unix workaround that does not change how Claude Code runs. With `-p` the useful companions are `--output-format json`, which prints one object that a script can read, and `--json-schema`, which makes the answer follow a schema (module 60 gives the full command).

A run with no person must end by itself: `--max-turns` stops a loop, and `--allowedTools` lists what runs without asking, because nobody can approve a prompt. A prompt that asks the run to be quick is a request, not a limit. A longer job timeout only lets a loop run longer. When a run fails, re-running it repeats the cost with no reason to expect a different result, so the job reports the failure and a person decides.

### Who waits decides the API

The Message Batches API costs half as much and may take up to 24 hours, with no guaranteed latency. That is the whole trade, and it settles which jobs may use it. A check that blocks a merge has a developer waiting, so it cannot depend on a job that may take a day. A technical debt report that is read the next morning has nobody waiting, so it takes the discount. Results of a batch come back matched by `custom_id`, so their order is no reason to avoid it. Polling a batch does not make it finish sooner. A fallback to real time when a batch is slow adds a second path and a second bill, and the simple answer is to match each job to the API that fits it. One more detail of the example: a batch job's command is a script that submits the work, so the `-p` rule applies to commands that start with `claude` and not to a script.

### The audit: a checklist that reads the pipeline

The example describes a pipeline in a small JSON file of jobs, with the fields that the checklist needs: who the job's `audience` is (`waiting` or `scheduled`), its `api` (`realtime` or `batch`), its `passes`, its `session`, what it is given as `context`, its `tools` and its `command`. That shape is this course's own, not a product file. The checklist reports, for each job in order:

- `no-print-flag`: a command that starts `claude` without `-p` or `--print`.
- `blocking-batch`: a job someone waits for that runs as a batch. `batchable`: a scheduled job that runs in real time and pays full price.
- `single-pass-review`, `shared-session`, `no-prior-findings` and `writes`: the design of a review, which page 2 explains. A `no-existing-tests` finding is the same idea for the test job.

For a pipeline with no findings the example prints each job with its API and its passes.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* the `-p` (or `--print`) flag is the documented way to run non-interactively, the answers that name a headless environment variable or a `--batch` flag describe things that do not exist, and the Message Batches API saves 50% at the cost of a processing time of up to 24 hours with no latency guarantee, which suits overnight jobs and not blocking checks. *What the product does now (documentation checked 2026-10-04):* the same. `claude -p` runs a prompt and exits, `--output-format json` and `--json-schema` shape the output, and a batch is completed within 24 hours with results matched by `custom_id`. So on the exam, choose by who waits; in a real pipeline, also choose by how much the job costs and how soon its result is read, because a fast batch is a bonus and never a promise. Module 63, "Batch and multi-pass review", goes further.

### The example

The example loads each pipeline and runs the checklist. It prints one line of facts per pipeline, then the findings for the draft, and for the fixed pipeline each job with its API and passes. The program and its output are the same in all four languages.

<!-- example: m74-pipeline-check tabs: python,typescript,java,kotlin -->
```python
"""Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.

The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
Claude.
"""
import json
import shlex
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
WRITERS = ("Bash", "Edit", "Write")


def load(root):
    return json.loads((root / "ci/pipeline.json").read_text())["jobs"]


def audit(root):
    found = []
    for job in load(root):
        name, kind = job["name"], job["kind"]
        tokens = shlex.split(job["command"])
        if tokens[0] == "claude" and "-p" not in tokens and "--print" not in tokens:
            found.append(f"no-print-flag: {name}")
        if job["audience"] == "waiting" and job["api"] == "batch":
            found.append(f"blocking-batch: {name}")
        if job["audience"] == "scheduled" and job["api"] == "realtime":
            found.append(f"batchable: {name}")
        if kind == "review" and job["passes"] != ["per-file", "integration"]:
            found.append(f"single-pass-review: {name}")
        if kind == "review" and job["session"] != "fresh":
            found.append(f"shared-session: {name}")
        if kind == "review" and "prior_findings" not in job["context"]:
            found.append(f"no-prior-findings: {name}")
        if kind == "testgen" and "existing_tests" not in job["context"]:
            found.append(f"no-existing-tests: {name}")
        if kind == "review" and any(t in WRITERS for t in job["tools"]):
            found.append(f"writes: {name}")
    return found


def main():
    for name in ("project-before", "project-after"):
        jobs = load(HERE / name)
        batch = sum(1 for j in jobs if j["api"] == "batch")
        print(f"{name}: {len(jobs)} jobs ({len(jobs) - batch} real-time, {batch} batch)")
        found = audit(HERE / name)
        for finding in found:
            print(f"  finding: {finding}")
        if not found:
            print("  no findings")
            for j in jobs:
                print(f"  {j['name']}: {j['api']}, passes {'+'.join(j['passes']) or 'none'}")


if __name__ == "__main__":
    main()
```
```text
project-before: 3 jobs (2 real-time, 1 batch)
  finding: blocking-batch: pre-merge-review
  finding: single-pass-review: pre-merge-review
  finding: shared-session: pre-merge-review
  finding: no-prior-findings: pre-merge-review
  finding: writes: pre-merge-review
  finding: batchable: debt-report
  finding: no-print-flag: test-generation
  finding: no-existing-tests: test-generation
project-after: 3 jobs (2 real-time, 1 batch)
  no findings
  pre-merge-review: realtime, passes per-file+integration
  debt-report: batch, passes none
  test-generation: realtime, passes none
```
```typescript
// Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.
//
// The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
// course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
// without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
// better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
// Claude.
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

export const HERE = fileURLToPath(new URL("..", import.meta.url));
const WRITERS = ["Bash", "Edit", "Write"];

export type Job = { name: string; kind: string; audience: string; api: string; passes: string[]; session: string; context: string[]; tools: string[]; command: string };

export const load = (root: string): Job[] => JSON.parse(readFileSync(join(root, "ci/pipeline.json"), "utf8")).jobs;

/** Split a command line the way a shell does for the quoting this pipeline uses: spaces separate words, double quotes group them and a backslash escapes a quote. */
export function words(command: string): string[] {
  const out: string[] = [];
  let word = "", quoted = false, started = false;
  for (let i = 0; i < command.length; i++) {
    const c = command[i];
    if (c === "\\" && i + 1 < command.length) { word += command[++i]; started = true; }
    else if (c === '"') { quoted = !quoted; started = true; }
    else if (c === " " && !quoted) { if (started) out.push(word); word = ""; started = false; }
    else { word += c; started = true; }
  }
  if (started) out.push(word);
  return out;
}

export function audit(root: string): string[] {
  const found: string[] = [];
  for (const job of load(root)) {
    const { name, kind } = job;
    const tokens = words(job.command);
    if (tokens[0] === "claude" && !tokens.includes("-p") && !tokens.includes("--print")) found.push(`no-print-flag: ${name}`);
    if (job.audience === "waiting" && job.api === "batch") found.push(`blocking-batch: ${name}`);
    if (job.audience === "scheduled" && job.api === "realtime") found.push(`batchable: ${name}`);
    if (kind === "review" && job.passes.join(",") !== "per-file,integration") found.push(`single-pass-review: ${name}`);
    if (kind === "review" && job.session !== "fresh") found.push(`shared-session: ${name}`);
    if (kind === "review" && !job.context.includes("prior_findings")) found.push(`no-prior-findings: ${name}`);
    if (kind === "testgen" && !job.context.includes("existing_tests")) found.push(`no-existing-tests: ${name}`);
    if (kind === "review" && job.tools.some((t) => WRITERS.includes(t))) found.push(`writes: ${name}`);
  }
  return found;
}

function main() {
  for (const name of ["project-before", "project-after"]) {
    const jobs = load(join(HERE, name));
    const batch = jobs.filter((j) => j.api === "batch").length;
    console.log(`${name}: ${jobs.length} jobs (${jobs.length - batch} real-time, ${batch} batch)`);
    const found = audit(join(HERE, name));
    for (const finding of found) console.log(`  finding: ${finding}`);
    if (found.length === 0) {
      console.log("  no findings");
      for (const j of jobs) console.log(`  ${j.name}: ${j.api}, passes ${j.passes.join("+") || "none"}`);
    }
  }
}

if (import.meta.main) main();
```
```text
project-before: 3 jobs (2 real-time, 1 batch)
  finding: blocking-batch: pre-merge-review
  finding: single-pass-review: pre-merge-review
  finding: shared-session: pre-merge-review
  finding: no-prior-findings: pre-merge-review
  finding: writes: pre-merge-review
  finding: batchable: debt-report
  finding: no-print-flag: test-generation
  finding: no-existing-tests: test-generation
project-after: 3 jobs (2 real-time, 1 batch)
  no findings
  pre-merge-review: realtime, passes per-file+integration
  debt-report: batch, passes none
  test-generation: realtime, passes none
```
```java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.
 *
 * <p>The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
 * course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
 * without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
 * better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
 * Claude.
 */
public final class PipelineCheck {
    static final Path HERE = Path.of("..").toAbsolutePath().normalize();
    private static final List<String> WRITERS = List.of("Bash", "Edit", "Write");
    private static final ObjectMapper JSON = new ObjectMapper();

    static JsonNode load(Path root) {
        try {
            return JSON.readTree(Files.readString(root.resolve("ci/pipeline.json"))).get("jobs");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Split a command line the way a shell does for the quoting this pipeline uses: spaces separate words, double quotes group them and a backslash escapes a quote. */
    static List<String> words(String command) {
        List<String> out = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        boolean quoted = false, started = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '\\' && i + 1 < command.length()) { word.append(command.charAt(++i)); started = true; }
            else if (c == '"') { quoted = !quoted; started = true; }
            else if (c == ' ' && !quoted) { if (started) out.add(word.toString()); word.setLength(0); started = false; }
            else { word.append(c); started = true; }
        }
        if (started) out.add(word.toString());
        return out;
    }

    private static List<String> list(JsonNode node) {
        List<String> out = new ArrayList<>();
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    static List<String> audit(Path root) {
        List<String> found = new ArrayList<>();
        for (JsonNode job : load(root)) {
            String name = job.get("name").asText(), kind = job.get("kind").asText();
            List<String> tokens = words(job.get("command").asText());
            String audience = job.get("audience").asText(), api = job.get("api").asText();
            if (tokens.get(0).equals("claude") && !tokens.contains("-p") && !tokens.contains("--print")) found.add("no-print-flag: " + name);
            if (audience.equals("waiting") && api.equals("batch")) found.add("blocking-batch: " + name);
            if (audience.equals("scheduled") && api.equals("realtime")) found.add("batchable: " + name);
            if (kind.equals("review") && !String.join(",", list(job.get("passes"))).equals("per-file,integration")) found.add("single-pass-review: " + name);
            if (kind.equals("review") && !job.get("session").asText().equals("fresh")) found.add("shared-session: " + name);
            if (kind.equals("review") && !list(job.get("context")).contains("prior_findings")) found.add("no-prior-findings: " + name);
            if (kind.equals("testgen") && !list(job.get("context")).contains("existing_tests")) found.add("no-existing-tests: " + name);
            if (kind.equals("review") && list(job.get("tools")).stream().anyMatch(WRITERS::contains)) found.add("writes: " + name);
        }
        return found;
    }

    public static void main(String[] args) {
        for (String name : List.of("project-before", "project-after")) {
            JsonNode jobs = load(HERE.resolve(name));
            int batch = 0;
            for (JsonNode j : jobs) if (j.get("api").asText().equals("batch")) batch++;
            System.out.println(name + ": " + jobs.size() + " jobs (" + (jobs.size() - batch) + " real-time, " + batch + " batch)");
            List<String> found = audit(HERE.resolve(name));
            found.forEach(f -> System.out.println("  finding: " + f));
            if (found.isEmpty()) {
                System.out.println("  no findings");
                for (JsonNode j : jobs) {
                    String passes = String.join("+", list(j.get("passes")));
                    System.out.println("  " + j.get("name").asText() + ": " + j.get("api").asText() + ", passes " + (passes.isEmpty() ? "none" : passes));
                }
            }
        }
    }
}
```
```text
project-before: 3 jobs (2 real-time, 1 batch)
  finding: blocking-batch: pre-merge-review
  finding: single-pass-review: pre-merge-review
  finding: shared-session: pre-merge-review
  finding: no-prior-findings: pre-merge-review
  finding: writes: pre-merge-review
  finding: batchable: debt-report
  finding: no-print-flag: test-generation
  finding: no-existing-tests: test-generation
project-after: 3 jobs (2 real-time, 1 batch)
  no findings
  pre-merge-review: realtime, passes per-file+integration
  debt-report: batch, passes none
  test-generation: realtime, passes none
```
```kotlin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.
 *
 * The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
 * course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
 * without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
 * better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
 * Claude.
 */
val HERE: Path = Path.of("..").toAbsolutePath().normalize()
private val WRITERS = listOf("Bash", "Edit", "Write")
private val JSON = ObjectMapper()

fun load(root: Path): JsonNode = JSON.readTree(Files.readString(root.resolve("ci/pipeline.json"))).get("jobs")

/** Split a command line the way a shell does for the quoting this pipeline uses: spaces separate words, double quotes group them and a backslash escapes a quote. */
fun words(command: String): List<String> {
    val out = mutableListOf<String>()
    val word = StringBuilder()
    var quoted = false
    var started = false
    var i = 0
    while (i < command.length) {
        val c = command[i]
        when {
            c == '\\' && i + 1 < command.length -> { i++; word.append(command[i]); started = true }
            c == '"' -> { quoted = !quoted; started = true }
            c == ' ' && !quoted -> { if (started) out += word.toString(); word.setLength(0); started = false }
            else -> { word.append(c); started = true }
        }
        i++
    }
    if (started) out += word.toString()
    return out
}

private fun list(node: JsonNode): List<String> = node.map { it.asText() }

fun audit(root: Path): List<String> {
    val found = mutableListOf<String>()
    for (job in load(root)) {
        val name = job.get("name").asText()
        val kind = job.get("kind").asText()
        val tokens = words(job.get("command").asText())
        val audience = job.get("audience").asText()
        val api = job.get("api").asText()
        if (tokens[0] == "claude" && "-p" !in tokens && "--print" !in tokens) found += "no-print-flag: $name"
        if (audience == "waiting" && api == "batch") found += "blocking-batch: $name"
        if (audience == "scheduled" && api == "realtime") found += "batchable: $name"
        if (kind == "review" && list(job.get("passes")).joinToString(",") != "per-file,integration") found += "single-pass-review: $name"
        if (kind == "review" && job.get("session").asText() != "fresh") found += "shared-session: $name"
        if (kind == "review" && "prior_findings" !in list(job.get("context"))) found += "no-prior-findings: $name"
        if (kind == "testgen" && "existing_tests" !in list(job.get("context"))) found += "no-existing-tests: $name"
        if (kind == "review" && list(job.get("tools")).any { it in WRITERS }) found += "writes: $name"
    }
    return found
}

fun main() {
    for (name in listOf("project-before", "project-after")) {
        val jobs = load(HERE.resolve(name))
        val batch = jobs.count { it.get("api").asText() == "batch" }
        println("$name: ${jobs.size()} jobs (${jobs.size() - batch} real-time, $batch batch)")
        val found = audit(HERE.resolve(name))
        for (finding in found) println("  finding: $finding")
        if (found.isEmpty()) {
            println("  no findings")
            for (j in jobs) println("  ${j.get("name").asText()}: ${j.get("api").asText()}, passes ${list(j.get("passes")).joinToString("+").ifEmpty { "none" }}")
        }
    }
}
```
```text
project-before: 3 jobs (2 real-time, 1 batch)
  finding: blocking-batch: pre-merge-review
  finding: single-pass-review: pre-merge-review
  finding: shared-session: pre-merge-review
  finding: no-prior-findings: pre-merge-review
  finding: writes: pre-merge-review
  finding: batchable: debt-report
  finding: no-print-flag: test-generation
  finding: no-existing-tests: test-generation
project-after: 3 jobs (2 real-time, 1 batch)
  no findings
  pre-merge-review: realtime, passes per-file+integration
  debt-report: batch, passes none
  test-generation: realtime, passes none
```
<!-- /example -->

The draft puts the merge check in a batch, reads all files in one pass in the session that wrote the code, gives it a shell tool, leaves the overnight report in real time, and starts the test job without `-p`. The fixed pipeline has none of those findings.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"Set an environment variable that turns on headless mode."** It is tempting because it sounds like how tools are switched. The exam rejects it: no such variable exists.
2. **"Redirect the input from an empty file so that the run cannot wait."** It is tempting because it stops the hang on some programs. The exam rejects it: it is a workaround and does not change how Claude Code runs.
3. **"Move both jobs to batch for the discount, and poll until the check is ready."** It is tempting because the saving is real. The exam rejects it for the merge check: polling does not make it finish sooner.
4. **"Keep both in real time, because batch results come back in any order."** It is tempting because order sounds like a risk. The exam rejects it: `custom_id` matches the results.

## Quiz

1. A pipeline step runs `claude "Analyze this pull request"` and hangs. What fixes it?
   - **a**: Point its input at an empty file so it cannot wait
   - **b**: Export an environment variable that selects headless mode
   - **c**: Add a batch flag so the call is queued
   - **d**: Add the print flag so that the run answers and exits

2. A pipeline has a gate that stops merges until it passes and a dependency digest that people read on Monday. Which calls move to batch?
   - **a**: Just the one nobody waits on, while the other stays in real time
   - **b**: Both of them, with polling to find out when the gate is ready
   - **c**: Neither of them, since the results of a batch arrive in any order
   - **d**: Both of them, with a fallback to real time when a batch is slow

3. To which jobs does the audit's `no-print-flag` finding apply?
   - **a**: Any job whose api is batch, whatever its command
   - **b**: A command that starts `python` and submits a script without `-p`
   - **c**: A command that starts `claude` and has neither `-p` nor `--print`
   - **d**: Any job that a developer waits for, whatever its command

<details>
<summary>Answer key</summary>

1. **d**. `-p` is the documented way to run without a person. *b* is ruled out because no such variable exists: "There is no headless environment variable and no `--batch` flag on the command line". *c* is ruled out for the same reason: "There is no headless environment variable and no `--batch` flag on the command line". *a* is ruled out because it is a workaround: "Redirecting standard input from an empty file is a Unix workaround that does not change how Claude Code runs."
2. **a**. Nobody waits for the digest and a developer waits for the gate. *b* is ruled out because waiting is the problem: "Polling a batch does not make it finish sooner." *c* is ruled out because matching exists: "Results of a batch come back matched by `custom_id`, so their order is no reason to avoid it." *d* is ruled out because it adds a path: "A fallback to real time when a batch is slow adds a second path and a second bill".
3. **c**. The finding is about the command line: "`no-print-flag`: a command that starts `claude` without `-p` or `--print`." *b* is ruled out because a script is not covered: "the `-p` rule applies to commands that start with `claude` and not to a script". *a* is ruled out for the same reason: "the `-p` rule applies to commands that start with `claude` and not to a script". *d* is ruled out because waiting has its own finding: "`blocking-batch`: a job someone waits for that runs as a batch."

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on a pipeline step that hangs, and the second its sample question on moving workflows to batch processing, both rewritten here.
