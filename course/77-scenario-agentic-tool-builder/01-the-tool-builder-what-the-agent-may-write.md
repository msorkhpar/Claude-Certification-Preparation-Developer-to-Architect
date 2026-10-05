# The tool builder: what the agent may write

**Level:** Architect · **Module 77:** Scenario: agentic tool builder · **Page 1 of 2**
**Exams:** X

**After this page you can** say what an agent that builds its own tools adds to the risks of an agent that only uses them, split the work into design, containment, permission, validation and audit, explain why a text scan of generated code is a first check and never a sandbox, and follow a run in which proposed tools are refused, held for a person or run and checked.

Checked on 2026-10-04 against the Claude API documentation pages "Define tools" (descriptions and `input_examples`) and "Code execution tool" (the sandboxed container), and against the Architect exam guide's task statements 2.1, 2.3 and 1.4, which this module reuses. No published blueprint tests this scenario: it goes beyond the exam's six settings and is taught to make the course complete. The example runs offline in Python, TypeScript, Java and Kotlin, and it executes no generated code: the proposals and their results are made-up data, so the example is about the decisions around a run. This page is a capstone: it uses modules 26, 41, 48, 52 and 54, which treat tool use, injection, gates, interfaces and least privilege in full.

## Why it matters

An agent that uses a fixed set of tools has an attack surface that the team chose and reviewed. An agent that writes its own tools has a surface that it chooses at run time, in text, and that text is code. Every question of the earlier modules comes back in a sharper form: who decides what the tool may touch, who reads what it returns, who can undo what it did. The architect's answer is not to forbid the pattern, which is useful, but to put each decision where it can be enforced: in a container, in a policy, in a gate and in a log, and not in the agent's promise.

## The idea

### The scenario in plain words

An agent is given a task that no existing tool covers. It designs a tool for it: a name, a description, an input schema, the permissions it needs and the code. A gate reviews the proposal. An approved tool runs in a container, and its result goes back to the agent as data. The team keeps an audit of what was proposed, decided and run. The setting is beyond the exam's six, and it applies what the exam teaches about tool interfaces, least privilege and guarantees in code to a tool that did not exist an hour ago.

### Five jobs, five homes

| Job | What it is | Its home |
|---|---|---|
| Design | A name and a description that let the model choose the tool correctly, and a schema | The proposal, checked by the gate |
| Containment | What the code can reach when it runs | The environment: a container with nothing the tool does not need |
| Permission | Which effects the tool may have, and which need a person | A policy that the gate enforces |
| Validation | Whether the result is what the schema promised | Code that checks the result before the agent uses it |
| Audit | A record of what was proposed, decided and run | A log that the agent cannot edit |

The first row is old advice with a new owner. The documentation says of descriptions: "Provide extremely detailed descriptions. This is by far the most important factor in tool performance." A tool the agent has written is chosen by the same model that wrote it, which makes the description both easier to get wrong and easier to check: a gate can require a minimum of words, a name in a fixed form and an example that is valid for the schema. The documentation adds that "Each example must be valid according to the tool's `input_schema`."

### A text scan is not a sandbox

It is tempting to read the generated code for dangerous calls and to trust a clean scan. The gate in this module does read the code, and its first lines say why that is only the first check. A scan finds what its authors thought of: a call spelled differently, built from strings or imported by name slips past it. The scan is useful for what it does well: it refuses the obvious, it compares what the code does with what the proposal declares, and it costs nothing.

Containment is the environment's job. The documentation describes what a managed sandbox looks like: "All operations run in a secure, sandboxed container. The container has no internet access, so Claude can't download packages at runtime". It lists the same properties as a design: "Internet access: Completely disabled for security", "Full isolation from host system and other containers", and "Each request runs in a new container unless you pass an earlier response's container ID back". The lesson for a team that runs generated code itself is the same, and it is a list of absences: no network, no secrets, no shared state, a short life and limits on time and memory.

### Read a symptom as a failure shape

| What the team finds | The failure shape | The first fix | Taught in |
|---|---|---|---|
| A generated tool fetched a web page that the proposal never mentioned | Code that does more than it declared | Compare what the code does with the permissions declared, and refuse the difference | This module |
| The agent chose the wrong one of two tools it wrote | A description that does not separate them | Require a description of a minimum length with the input form and when to use it | Module 52 |
| A tool wrote a file that nobody approved | An effect with no gate | Hold every write for a person, in code | Modules 48 and 54 |
| A tool's result contained instructions and the agent followed them | A result trusted as an instruction | Treat the result as data and check it against its schema | Module 41 |
| A run used far more time and memory than expected | No limits | A timeout and a memory limit in the policy, and in the container | This module |
| Nobody can say which tool changed a file last week | No audit | A log line for every proposal, decision and run | This module |

### The example

The example decides four proposals from the permissions each asks for. The first tool only reads, so it runs without a person. The second reads and writes, so it waits for a reviewer, who approves it; its result is missing a field that its schema promised, and the check rejects it. The third asks for the network, which the policy denies, so it is refused whatever its code says. The fourth asks only to write, waits for the reviewer, and is declined. The last line is the audit tally. Nothing generated is executed: the results are made-up data, and the point of the example is the path around the run.

<!-- example: m77-tool-gate tabs: python,typescript,java,kotlin -->
```python
"""An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.

The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
"""
import logging

log = logging.getLogger(__name__)

DENIED = ("network", "run_process")
NEEDS_APPROVAL = ("write_files",)
SCHEMA = {"headline": str, "rows": int}
MAX_CHARS = 200

PROPOSALS = [
    {"name": "read_report", "permissions": ["read_files"], "result": {"headline": "Q3 up 4%", "rows": 12}},
    {"name": "write_summary", "permissions": ["read_files", "write_files"], "result": {"headline": "Q3 up 4%"}},
    {"name": "fetch_prices", "permissions": ["network"], "result": {"headline": "x", "rows": 1}},
    {"name": "tidy_up", "permissions": ["write_files"], "result": {"headline": "x", "rows": 1}},
]
REVIEWER = {"write_summary": True, "tidy_up": False}  # the person's answers, by tool name


def decide(permissions):
    """Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic."""
    log.debug("decide input: %r", permissions)
    denied = [p for p in permissions if p in DENIED]
    if denied:
        return "refused", denied
    if any(p in NEEDS_APPROVAL for p in permissions):
        return "needs_approval", [p for p in permissions if p in NEEDS_APPROVAL]
    return "auto", []


def check_output(result):
    """The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text."""
    problems = [f"missing: {field}" for field in SCHEMA if field not in result]
    problems += [f"type: {field}" for field, kind in SCHEMA.items() if field in result and not isinstance(result[field], kind)]
    if sum(len(v) for v in result.values() if isinstance(v, str)) > MAX_CHARS:
        problems.append("too large")
    return problems


def main():
    log = []
    for proposal in PROPOSALS:
        name = proposal["name"]
        decision, why = decide(proposal["permissions"])
        if decision == "refused":
            print(f"{name}: refused ({', '.join(why)})")
            log.append((name, "refused"))
            continue
        if decision == "needs_approval":
            answer = REVIEWER[name]
            print(f"{name}: needs_approval -> {'approved' if answer else 'declined'}")
            if not answer:
                log.append((name, "declined"))
                continue
        else:
            print(f"{name}: auto")
        problems = check_output(proposal["result"])
        log.append((name, "ran" if not problems else "rejected"))
        if problems:
            print(f"  result of {name} rejected ({'; '.join(problems)})")
    ran = sum(1 for _, s in log if s == "ran")
    print(f"audit: {len(PROPOSALS)} proposals, {ran} ran, {sum(1 for _, s in log if s == 'rejected')} rejected, "
          f"{sum(1 for _, s in log if s == 'refused')} refused, {sum(1 for _, s in log if s == 'declined')} declined")


if __name__ == "__main__":
    main()
```
```text
read_report: auto
write_summary: needs_approval -> approved
  result of write_summary rejected (missing: rows)
fetch_prices: refused (network)
tidy_up: needs_approval -> declined
audit: 4 proposals, 1 ran, 1 rejected, 1 refused, 1 declined
```
```typescript
// An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
// an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.
//
// The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
// The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
import { logger } from "./logger.ts";
const log = logger("tool_gate");
const DENIED = ["network", "run_process"];
const NEEDS_APPROVAL = ["write_files"];
const SCHEMA: Record<string, string> = { headline: "string", rows: "number" };
const MAX_CHARS = 200;

export type Proposal = { name: string; permissions: string[]; result: Record<string, unknown> };

export const PROPOSALS: Proposal[] = [
  { name: "read_report", permissions: ["read_files"], result: { headline: "Q3 up 4%", rows: 12 } },
  { name: "write_summary", permissions: ["read_files", "write_files"], result: { headline: "Q3 up 4%" } },
  { name: "fetch_prices", permissions: ["network"], result: { headline: "x", rows: 1 } },
  { name: "tidy_up", permissions: ["write_files"], result: { headline: "x", rows: 1 } },
];
const REVIEWER: Record<string, boolean> = { write_summary: true, tidy_up: false }; // the person's answers, by tool name

/** Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic. */
export function decide(permissions: string[]): [string, string[]] {
  log.debug("decide input", permissions);
  const denied = permissions.filter((p) => DENIED.includes(p));
  if (denied.length > 0) return ["refused", denied];
  const gated = permissions.filter((p) => NEEDS_APPROVAL.includes(p));
  if (gated.length > 0) return ["needs_approval", gated];
  return ["auto", []];
}

/** The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text. */
export function checkOutput(result: Record<string, unknown>): string[] {
  const problems = Object.keys(SCHEMA).filter((f) => !(f in result)).map((f) => `missing: ${f}`);
  for (const [field, kind] of Object.entries(SCHEMA)) if (field in result && typeof result[field] !== kind) problems.push(`type: ${field}`);
  if (Object.values(result).reduce((n: number, v) => n + (typeof v === "string" ? v.length : 0), 0) > MAX_CHARS) problems.push("too large");
  return problems;
}

function main() {
  const log: [string, string][] = [];
  for (const proposal of PROPOSALS) {
    const name = proposal.name;
    const [decision, why] = decide(proposal.permissions);
    if (decision === "refused") {
      console.log(`${name}: refused (${why.join(", ")})`);
      log.push([name, "refused"]);
      continue;
    }
    if (decision === "needs_approval") {
      const answer = REVIEWER[name];
      console.log(`${name}: needs_approval -> ${answer ? "approved" : "declined"}`);
      if (!answer) {
        log.push([name, "declined"]);
        continue;
      }
    } else {
      console.log(`${name}: auto`);
    }
    const problems = checkOutput(proposal.result);
    log.push([name, problems.length === 0 ? "ran" : "rejected"]);
    if (problems.length > 0) console.log(`  result of ${name} rejected (${problems.join("; ")})`);
  }
  const count = (s: string) => log.filter(([, status]) => status === s).length;
  console.log(`audit: ${PROPOSALS.length} proposals, ${count("ran")} ran, ${count("rejected")} rejected, ${count("refused")} refused, ${count("declined")} declined`);
}

if (import.meta.main) main();
```
```text
read_report: auto
write_summary: needs_approval -> approved
  result of write_summary rejected (missing: rows)
fetch_prices: refused (network)
tidy_up: needs_approval -> declined
audit: 4 proposals, 1 ran, 1 rejected, 1 refused, 1 declined
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
 * an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.
 *
 * <p>The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
 * The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
 */
public final class ToolGate {
    private static final System.Logger LOG = System.getLogger(ToolGate.class.getName());
    record Proposal(String name, List<String> permissions, Map<String, Object> result) {}

    record Decision(String decision, List<String> why) {}

    static final List<String> DENIED = List.of("network", "run_process");
    static final List<String> NEEDS_APPROVAL = List.of("write_files");
    static final int MAX_CHARS = 200;

    static final List<Proposal> PROPOSALS = List.of(
        new Proposal("read_report", List.of("read_files"), Map.of("headline", "Q3 up 4%", "rows", 12)),
        new Proposal("write_summary", List.of("read_files", "write_files"), Map.of("headline", "Q3 up 4%")),
        new Proposal("fetch_prices", List.of("network"), Map.of("headline", "x", "rows", 1)),
        new Proposal("tidy_up", List.of("write_files"), Map.of("headline", "x", "rows", 1)));
    static final Map<String, Boolean> REVIEWER = Map.of("write_summary", true, "tidy_up", false); // the person's answers, by tool name

    /** Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic. */
    static Decision decide(List<String> permissions) {
        LOG.log(System.Logger.Level.DEBUG, "decide input: {0}", permissions);
        List<String> denied = permissions.stream().filter(DENIED::contains).toList();
        if (!denied.isEmpty()) return new Decision("refused", denied);
        List<String> gated = permissions.stream().filter(NEEDS_APPROVAL::contains).toList();
        if (!gated.isEmpty()) return new Decision("needs_approval", gated);
        return new Decision("auto", List.of());
    }

    /** The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text. */
    static List<String> checkOutput(Map<String, Object> result) {
        List<String> problems = new ArrayList<>();
        if (!result.containsKey("headline")) problems.add("missing: headline");
        if (!result.containsKey("rows")) problems.add("missing: rows");
        if (result.containsKey("headline") && !(result.get("headline") instanceof String)) problems.add("type: headline");
        if (result.containsKey("rows") && !(result.get("rows") instanceof Integer)) problems.add("type: rows");
        int chars = result.values().stream().filter(v -> v instanceof String).mapToInt(v -> ((String) v).length()).sum();
        if (chars > MAX_CHARS) problems.add("too large");
        return problems;
    }

    public static void main(String[] args) {
        Map<String, String> log = new LinkedHashMap<>();
        for (Proposal proposal : PROPOSALS) {
            String name = proposal.name();
            Decision d = decide(proposal.permissions());
            if (d.decision().equals("refused")) {
                System.out.println(name + ": refused (" + String.join(", ", d.why()) + ")");
                log.put(name, "refused");
                continue;
            }
            if (d.decision().equals("needs_approval")) {
                boolean answer = REVIEWER.get(name);
                System.out.println(name + ": needs_approval -> " + (answer ? "approved" : "declined"));
                if (!answer) {
                    log.put(name, "declined");
                    continue;
                }
            } else {
                System.out.println(name + ": auto");
            }
            List<String> problems = checkOutput(proposal.result());
            log.put(name, problems.isEmpty() ? "ran" : "rejected");
            if (!problems.isEmpty()) System.out.println("  result of " + name + " rejected (" + String.join("; ", problems) + ")");
        }
        System.out.println("audit: " + PROPOSALS.size() + " proposals, " + log.values().stream().filter(s -> s.equals("ran")).count() + " ran, "
            + log.values().stream().filter(s -> s.equals("rejected")).count() + " rejected, " + log.values().stream().filter(s -> s.equals("refused")).count() + " refused, "
            + log.values().stream().filter(s -> s.equals("declined")).count() + " declined");
    }
}
```
```text
read_report: auto
write_summary: needs_approval -> approved
  result of write_summary rejected (missing: rows)
fetch_prices: refused (network)
tidy_up: needs_approval -> declined
audit: 4 proposals, 1 ran, 1 rejected, 1 refused, 1 declined
```
```kotlin
private val log = System.getLogger("tool_gate")

/**
 * An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
 * an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.
 *
 * The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
 * The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
 */
data class Proposal(val name: String, val permissions: List<String>, val result: Map<String, Any>)

data class Decision(val decision: String, val why: List<String>)

val DENIED = listOf("network", "run_process")
val NEEDS_APPROVAL = listOf("write_files")
const val MAX_CHARS = 200

val PROPOSALS = listOf(
    Proposal("read_report", listOf("read_files"), mapOf("headline" to "Q3 up 4%", "rows" to 12)),
    Proposal("write_summary", listOf("read_files", "write_files"), mapOf("headline" to "Q3 up 4%")),
    Proposal("fetch_prices", listOf("network"), mapOf("headline" to "x", "rows" to 1)),
    Proposal("tidy_up", listOf("write_files"), mapOf("headline" to "x", "rows" to 1)),
)
val REVIEWER = mapOf("write_summary" to true, "tidy_up" to false) // the person's answers, by tool name

/** Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic. */
fun decide(permissions: List<String>): Decision {
    log.log(System.Logger.Level.DEBUG, "decide input: {0}", permissions)
    val denied = permissions.filter { it in DENIED }
    if (denied.isNotEmpty()) return Decision("refused", denied)
    val gated = permissions.filter { it in NEEDS_APPROVAL }
    if (gated.isNotEmpty()) return Decision("needs_approval", gated)
    return Decision("auto", listOf())
}

/** The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text. */
fun checkOutput(result: Map<String, Any>): List<String> {
    val problems = mutableListOf<String>()
    if ("headline" !in result) problems += "missing: headline"
    if ("rows" !in result) problems += "missing: rows"
    if ("headline" in result && result["headline"] !is String) problems += "type: headline"
    if ("rows" in result && result["rows"] !is Int) problems += "type: rows"
    if (result.values.sumOf { if (it is String) it.length else 0 } > MAX_CHARS) problems += "too large"
    return problems
}

fun main() {
    val log = linkedMapOf<String, String>()
    for (proposal in PROPOSALS) {
        val name = proposal.name
        val d = decide(proposal.permissions)
        if (d.decision == "refused") {
            println("$name: refused (${d.why.joinToString(", ")})")
            log[name] = "refused"
            continue
        }
        if (d.decision == "needs_approval") {
            val answer = REVIEWER.getValue(name)
            println("$name: needs_approval -> ${if (answer) "approved" else "declined"}")
            if (!answer) {
                log[name] = "declined"
                continue
            }
        } else {
            println("$name: auto")
        }
        val problems = checkOutput(proposal.result)
        log[name] = if (problems.isEmpty()) "ran" else "rejected"
        if (problems.isNotEmpty()) println("  result of $name rejected (${problems.joinToString("; ")})")
    }
    println("audit: ${PROPOSALS.size} proposals, ${log.values.count { it == "ran" }} ran, ${log.values.count { it == "rejected" }} rejected, ${log.values.count { it == "refused" }} refused, ${log.values.count { it == "declined" }} declined")
}
```
```text
read_report: auto
write_summary: needs_approval -> approved
  result of write_summary rejected (missing: rows)
fetch_prices: refused (network)
tidy_up: needs_approval -> declined
audit: 4 proposals, 1 ran, 1 rejected, 1 refused, 1 declined
```
<!-- /example -->

Every line is the output of the container, and all four languages print the same lines. The check at the end is the part to notice. A tool that the agent wrote returns data that the agent will read, and a result that does not match its schema is rejected before the agent sees it, which is also the answer to a result that tries to speak in the agent's voice.

## Traps

These are the answers that sound sensible and fail in an agent that builds tools, each with the reason it fails.

1. **"Scan the generated code for dangerous calls, and run it if the scan is clean."** It is tempting because a scan is cheap and catches the obvious. It fails because a scan finds only what its authors thought of, and containment belongs to the environment: no network, no secrets, short life, limits.
2. **"Let the agent declare its own permissions and trust the declaration."** It is tempting because the agent knows what its code does. It fails because the declaration is the claim to be checked: the gate compares what the code does with what was declared and refuses the difference.
3. **"Approve once, and let the tool run unattended afterwards."** It is tempting because it removes a person from the loop. It fails for a tool with an irreversible effect: a write waits for a person every time, and the approval covers the proposal that was reviewed and not a later version.

## Quiz

1. A generated tool declares that it reads files, and its code fetches a web page. What does the gate in this module's practice do?
   - **a**: Approves it with a gate, since reading and fetching are both low-risk effects here
   - **b**: Refuses it, since the proposal never mentioned that effect
   - **c**: Sends it back for a better description, since the declaration was incomplete
   - **d**: Runs it once in a container to see whether the fetch succeeds as intended

2. A team runs generated tools in a container that has no network access, no secrets and a short lifetime. A colleague says that the code scan is therefore unnecessary. What is the best reply?
   - **a**: It is a cheap first check for the obvious, and the surroundings hold back the rest
   - **b**: The scan is unnecessary only when the container is also given the secrets it needs to work
   - **c**: The scan is the real protection, and the container only adds some speed to each run
   - **d**: The scan is unnecessary, since containers make generated tools safe whatever their code does

3. In the example, a tool that was approved by the reviewer returns a result without the field that its schema promised. What happens to the result?
   - **a**: It is passed to the agent with a warning attached to the front of the result
   - **b**: It is filled with a default and passed on, so that the run can continue
   - **c**: It is refused before the agent uses it, and the audit records a rejection
   - **d**: It is rejected, and the reviewer's approval is withdrawn for every later tool in the run

<details>
<summary>Answer key</summary>

1. **b**. Code that does more than it declared is refused. *a* is ruled out because the difference is the finding: "the gate compares what the code does with what was declared and refuses the difference". *c* is ruled out because a refusal is not a revision: "Code that does more than it declared". *d* is ruled out because nothing proposed is run to find out: "Containment is the environment's job."
2. **a**. The scan is the cheap first check. *b* is ruled out because the container holds no secrets: "The lesson for a team that runs generated code itself is the same, and it is a list of absences: no network, no secrets, no shared state, a short life and limits on time and memory." *c* is ruled out because the scan finds only what its authors thought of: "A scan finds what its authors thought of: a call spelled differently, built from strings or imported by name slips past it." *d* is ruled out because the scan still does useful work: "The scan is useful for what it does well: it refuses the obvious".
3. **c**. A result is checked before the agent uses it. *a* is ruled out because a result that does not match is not passed on: "a result that does not match its schema is rejected before the agent sees it". *b* is ruled out because the check does not repair data: "Treat the result as data and check it against its schema". *d* is ruled out because the approval covers one proposal: "the approval covers the proposal that was reviewed and not a later version".

</details>
