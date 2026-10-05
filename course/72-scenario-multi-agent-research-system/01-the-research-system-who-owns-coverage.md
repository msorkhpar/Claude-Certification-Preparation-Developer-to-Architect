# The research system: who owns coverage

**Level:** Architect · **Module 72:** Scenario: multi-agent research system · **Page 1 of 2**
**Exams:** A1, A2, A5; S3

**After this page you can** describe the exam's research system and what each of its four subagents may do, route a symptom in a finished report to the failure behind it and to the first fix, say what the coordinator owns that no subagent can, and read a run in which the plan is too narrow, a search fails and a scoped tool checks a date.

Checked on 2026-10-04 against Anthropic's engineering article on its multi-agent research system and the Architect exam guide (version 1.0, scenario 3 and its sample questions). The example runs offline in Python, TypeScript, Java and Kotlin: the subagents are functions over made-up data, so the example is about what the coordinator does with what comes back. This page is a capstone: it uses modules 46, 47, 52, 53 and 54, and names modules 66 (Errors across agents) and 69 (Provenance and uncertainty), which treat the failure path and the citations in full.

## Why it matters

Scenario S3 is the architecture the exam keeps returning to: one coordinator and several specialised subagents. The failures it asks about share a property that makes them hard to see. Every subagent can succeed, and the report can still be wrong, because the error is in what was assigned, in what came back and was passed on, or in what the final status claims. The exam rewards the habit of reading the coordinator's side: the plan it made, the errors it received and the status it wrote.

## The idea

### The scenario in plain words

A research system is built on the Claude Agent SDK. A coordinator receives a topic and delegates to four specialised subagents: one searches the web, one analyses documents, one synthesises the findings and one writes the report. The output is a comprehensive report with citations. The domains the exam draws on are the agent architecture, the design of tools and their integration, and the management of context and reliability.

### Read a symptom as a failure shape

| What the finished report or the logs show | The failure shape | The first fix | Taught in |
|---|---|---|---|
| Every subagent succeeded, yet the report covers one part of the topic, and the coordinator's log shows narrow subtasks | A decomposition that is too narrow | Fix the plan: compare it with the scopes the question needs before any subagent runs | Modules 46 and 50 |
| A subagent returns work on the wrong period or the wrong scope | A brief that did not carry its context | Put the objective, the output format, the tools and sources, and the boundaries into the brief | Module 47 |
| The search times out and the coordinator is told "unavailable", or gets an empty result marked as a success | A failure with no context, or a failure turned into a success | A structured error: type, the query, partial results, alternatives | Module 53, and module 66 (Errors across agents) |
| Synthesis keeps asking the coordinator to check a date, and each round trip adds latency | A tool the agent needs sits behind a round trip | A narrow tool for the frequent simple check, and the deep ones still go through the coordinator | Module 54 |
| Two sources give two figures and the report shows one | A conflict resolved silently | Show both values with their sources and dates | Module 62, and module 69 (Provenance and uncertainty) |
| The coordinator's context fills with the raw output of its subagents | Large results passed through the hub | Subagents store their work and pass back references | Module 46 |
| The report says it is complete and a scope is missing | A status that is not derived from coverage | Compute the status from the scopes covered | This module |

The article on Anthropic's own research system gives the brief its four parts: "Each subagent needs an objective, an output format, guidance on the tools and sources to use, and clear task boundaries." It describes the way large results travel: "Subagents call tools to store their work in external systems, then pass lightweight references back to the coordinator." And it says what to do about a failing tool: "Letting the agent know when a tool is failing and letting it adapt works surprisingly well." Each line is a design rule of the table above.

### What the coordinator owns

A subagent owns the scope it was given. The coordinator owns everything that is about the whole question, and four of those duties are exactly where the exam's failures sit.

1. **The decomposition.** A plan is a claim that its subtasks, together, cover the question. The coordinator can check the claim: list the scopes the question needs and compare them with the scopes of the subtasks. The example's first plan covers one of four scopes, so the check names the other three before a single search runs. A subagent cannot do this check: it sees only its own brief. A downstream agent that is asked to notice gaps is a late and unreliable substitute for a check that costs one comparison.
2. **The recovery.** When a subagent fails, the coordinator decides: try an alternative, try another approach, or go on with what is partial. It can decide only if the error says what failed and what could be tried.
3. **The status.** A run is complete when every scope is covered. It is partial otherwise, whatever the reason, and the report says which scopes are missing and why.
4. **The conflicts.** When two subagents return different values for the same claim, the coordinator does not pick. It carries both, with their sources and dates, to the report.

### What each subagent may do

The least-privilege rule of module 54 applies to each of the four. The search agent has the web tools and nothing that writes. The analysis agent reads the documents it is given. The synthesis agent combines findings, and for the frequent simple check (a date, a name) it has one narrow verification tool and no search tools. The report agent writes the report and calls nothing else. A scoped tool of this kind removes the round trip for the common case and leaves the deep verification where it was: with the coordinator, who delegates it to the search agent. In the example, the tool answers for a date it can check, and returns `needs_search` for anything else.

### The example

The example runs the coordinator's side of one research run. A first plan of three subtasks, all in visual arts, is checked against the four scopes the question needs; the check finds three gaps, and the coordinator adds a subtask for each. The search for film then fails as a timeout, and the failure comes back as a result with its type, its query, the partial results (none) and an alternative query. The coordinator tries the alternative once and it works. The synthesis agent's verification tool confirms two dates and sends one statistic back. Finally the report is built twice, once with every source up and once with the film search down for good. In that second run the status is `partial`, three of four scopes are covered, and the note names the two queries that failed.

<!-- example: m72-research-run tabs: python,typescript,java,kotlin -->
```python
"""A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.

The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
"""
import logging

log = logging.getLogger(__name__)

REQUIRED = ["visual arts", "music", "writing", "film"]

# what the web search subagent finds for a query: (claim, value, source, date)
SOURCES = {
    "AI in digital art": [("studios using generative tools", "60%", "survey-a", "2025-02-01")],
    "AI in graphic design": [("designers using generative tools weekly", "48%", "survey-b", "2025-03-10")],
    "AI in photography": [("agencies labelling generated images", "yes", "policy-c", "2024-11-20")],
    "AI in music": [("labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15")],
    "AI in writing": [("publishers with an AI policy", "70%", "survey-e", "2025-04-02")],
    "AI in film production": [("studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12")],
}
ALTERNATIVES = {"AI in film": ["AI in film production"]}
PUBLISHED = {"survey-a": "2025-02-01", "report-d": "2025-01-15"}  # what the synthesis agent can check here


def search(query, down=()):
    """The search subagent. A failure is returned with its type, the query, the partial results and what to try instead."""
    if query in down or query not in SOURCES:
        return {"status": "error", "error": {"type": "timeout", "query": query, "partial": [], "alternatives": ALTERNATIVES.get(query, []), "tried": [query]}}
    return {"status": "ok", "findings": SOURCES[query]}


def coverage(plan):
    covered = [s for s in REQUIRED if any(t["scope"] == s for t in plan)]
    return covered, [s for s in REQUIRED if s not in covered]


def replan(plan):
    """The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out."""
    return plan + [{"scope": s, "query": f"AI in {s}"} for s in coverage(plan)[1]]


def recover(result, down):
    """One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries."""
    error = result.get("error")
    if result["status"] == "ok" or not error["alternatives"]:
        return result
    again = search(error["alternatives"][0], down)
    if again["status"] == "ok":
        return {**again, "recovered_from": error["query"]}
    return {"status": "error", "error": {**error, "tried": error["tried"] + [error["alternatives"][0]]}}


def research(plan, down=()):
    return [{**task, **recover(search(task["query"], down), down)} for task in plan]


def verify_fact(kind, source, value):
    """The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator."""
    if kind == "date":
        return "confirmed" if PUBLISHED.get(source) == value else "mismatch"
    return "needs_search"


def report(results):
    covered = [s for s in REQUIRED if any(r["scope"] == s and r["status"] == "ok" for r in results)]
    notes = [f"{r['scope']} not covered: timeout on " + " and on ".join(f"'{q}'" for q in r["error"]["tried"]) for r in results if r["status"] == "error"]
    findings = [f for r in results if r["status"] == "ok" for f in r["findings"]]
    return {"status": "complete" if len(covered) == len(REQUIRED) else "partial", "covered": len(covered), "findings": len(findings),
            "errors": len(notes), "notes": notes or ["nothing left uncovered"]}


def main():
    narrow = [{"scope": "visual arts", "query": q} for q in ("AI in digital art", "AI in graphic design", "AI in photography")]
    covered, gaps = coverage(narrow)
    print(f"plan 1: {len(narrow)} subtasks, scopes covered {len(covered)} of {len(REQUIRED)}, gaps: {', '.join(gaps)}")
    plan = replan(narrow)
    covered, gaps = coverage(plan)
    print(f"plan 2: {len(plan)} subtasks, scopes covered {len(covered)} of {len(REQUIRED)}, gaps: {', '.join(gaps) or 'none'}")
    first = search("AI in film")
    e = first["error"]
    print(f"search '{e['query']}': {first['status']} {e['type']}, {len(e['partial'])} partial, alternative '{e['alternatives'][0]}'")
    results = research(plan)
    recovered = next(r for r in results if "recovered_from" in r)
    print(f"recovered: '{recovered['recovered_from']}' -> '{ALTERNATIVES[recovered['recovered_from']][0]}' scope {recovered['scope']}, {len(recovered['findings'])} finding")
    checks = [("date", "survey-a", "2025-02-01"), ("date", "report-d", "2025-01-15"), ("statistic", "survey-a", "60%")]
    verdicts = [verify_fact(*c) for c in checks]
    print(f"verify_fact: {verdicts.count('confirmed')} confirmed here, {verdicts.count('needs_search')} sent back to the coordinator")
    for label, down in (("all sources up", ()), ("film search down for good", ("AI in film", "AI in film production"))):
        r = report(research(plan, down))
        print(f"report ({label}): status={r['status']}, covered={r['covered']}/{len(REQUIRED)}, findings={r['findings']}, errors={r['errors']}")
        print(f"  note: {'; '.join(r['notes'])}")


if __name__ == "__main__":
    main()
```
```text
plan 1: 3 subtasks, scopes covered 1 of 4, gaps: music, writing, film
plan 2: 6 subtasks, scopes covered 4 of 4, gaps: none
search 'AI in film': error timeout, 0 partial, alternative 'AI in film production'
recovered: 'AI in film' -> 'AI in film production' scope film, 1 finding
verify_fact: 2 confirmed here, 1 sent back to the coordinator
report (all sources up): status=complete, covered=4/4, findings=6, errors=0
  note: nothing left uncovered
report (film search down for good): status=partial, covered=3/4, findings=5, errors=1
  note: film not covered: timeout on 'AI in film' and on 'AI in film production'
```
```typescript
// A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
// one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.
//
// The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
// shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
import { logger } from "./logger.ts";
const log = logger("research_run");

export const REQUIRED = ["visual arts", "music", "writing", "film"];

type Finding = [claim: string, value: string, source: string, date: string];
export type Task = { scope: string; query: string };
export type SearchError = { type: string; query: string; partial: Finding[]; alternatives: string[]; tried: string[] };
export type Result = { status: "ok" | "error"; findings?: Finding[]; error?: SearchError; recovered_from?: string };
export type Done = Task & Result;

// what the web search subagent finds for a query
const SOURCES: Record<string, Finding[]> = {
  "AI in digital art": [["studios using generative tools", "60%", "survey-a", "2025-02-01"]],
  "AI in graphic design": [["designers using generative tools weekly", "48%", "survey-b", "2025-03-10"]],
  "AI in photography": [["agencies labelling generated images", "yes", "policy-c", "2024-11-20"]],
  "AI in music": [["labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15"]],
  "AI in writing": [["publishers with an AI policy", "70%", "survey-e", "2025-04-02"]],
  "AI in film production": [["studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12"]],
};
const ALTERNATIVES: Record<string, string[]> = { "AI in film": ["AI in film production"] };
const PUBLISHED: Record<string, string> = { "survey-a": "2025-02-01", "report-d": "2025-01-15" }; // what the synthesis agent can check here

/** The search subagent. A failure is returned with its type, the query, the partial results and what to try instead. */
export function search(query: string, down: string[] = []): Result {
  if (down.includes(query) || !(query in SOURCES)) return { status: "error", error: { type: "timeout", query, partial: [], alternatives: ALTERNATIVES[query] ?? [], tried: [query] } };
  return { status: "ok", findings: SOURCES[query] };
}

export function coverage(plan: Task[]): [string[], string[]] {
  const covered = REQUIRED.filter((s) => plan.some((t) => t.scope === s));
  return [covered, REQUIRED.filter((s) => !covered.includes(s))];
}

/** The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out. */
export function replan(plan: Task[]): Task[] {
  return [...plan, ...coverage(plan)[1].map((s) => ({ scope: s, query: `AI in ${s}` }))];
}

/** One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries. */
export function recover(result: Result, down: string[]): Result {
  const error = result.error;
  if (result.status === "ok" || !error || error.alternatives.length === 0) return result;
  const again = search(error.alternatives[0], down);
  if (again.status === "ok") return { ...again, recovered_from: error.query };
  return { status: "error", error: { ...error, tried: [...error.tried, error.alternatives[0]] } };
}

export function research(plan: Task[], down: string[] = []): Done[] {
  return plan.map((task) => ({ ...task, ...recover(search(task.query, down), down) }));
}

/** The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator. */
export function verifyFact(kind: string, source: string, value: string): string {
  if (kind === "date") return PUBLISHED[source] === value ? "confirmed" : "mismatch";
  return "needs_search";
}

export function report(results: Done[]) {
  const covered = REQUIRED.filter((s) => results.some((r) => r.scope === s && r.status === "ok"));
  const notes = results.filter((r) => r.status === "error").map((r) => `${r.scope} not covered: timeout on ` + r.error!.tried.map((q) => `'${q}'`).join(" and on "));
  const findings = results.filter((r) => r.status === "ok").reduce((n, r) => n + r.findings!.length, 0);
  return { status: covered.length === REQUIRED.length ? "complete" : "partial", covered: covered.length, findings, errors: notes.length, notes: notes.length ? notes : ["nothing left uncovered"] };
}

function main() {
  const narrow: Task[] = ["AI in digital art", "AI in graphic design", "AI in photography"].map((query) => ({ scope: "visual arts", query }));
  let [covered, gaps] = coverage(narrow);
  console.log(`plan 1: ${narrow.length} subtasks, scopes covered ${covered.length} of ${REQUIRED.length}, gaps: ${gaps.join(", ")}`);
  const plan = replan(narrow);
  [covered, gaps] = coverage(plan);
  console.log(`plan 2: ${plan.length} subtasks, scopes covered ${covered.length} of ${REQUIRED.length}, gaps: ${gaps.join(", ") || "none"}`);
  const first = search("AI in film");
  const e = first.error!;
  console.log(`search '${e.query}': ${first.status} ${e.type}, ${e.partial.length} partial, alternative '${e.alternatives[0]}'`);
  const recovered = research(plan).find((r) => r.recovered_from)!;
  console.log(`recovered: '${recovered.recovered_from}' -> '${ALTERNATIVES[recovered.recovered_from!][0]}' scope ${recovered.scope}, ${recovered.findings!.length} finding`);
  const checks: [string, string, string][] = [["date", "survey-a", "2025-02-01"], ["date", "report-d", "2025-01-15"], ["statistic", "survey-a", "60%"]];
  const verdicts = checks.map((c) => verifyFact(...c));
  console.log(`verify_fact: ${verdicts.filter((v) => v === "confirmed").length} confirmed here, ${verdicts.filter((v) => v === "needs_search").length} sent back to the coordinator`);
  for (const [label, down] of [["all sources up", []], ["film search down for good", ["AI in film", "AI in film production"]]] as [string, string[]][]) {
    const r = report(research(plan, down));
    console.log(`report (${label}): status=${r.status}, covered=${r.covered}/${REQUIRED.length}, findings=${r.findings}, errors=${r.errors}`);
    console.log(`  note: ${r.notes.join("; ")}`);
  }
}

if (import.meta.main) main();
```
```text
plan 1: 3 subtasks, scopes covered 1 of 4, gaps: music, writing, film
plan 2: 6 subtasks, scopes covered 4 of 4, gaps: none
search 'AI in film': error timeout, 0 partial, alternative 'AI in film production'
recovered: 'AI in film' -> 'AI in film production' scope film, 1 finding
verify_fact: 2 confirmed here, 1 sent back to the coordinator
report (all sources up): status=complete, covered=4/4, findings=6, errors=0
  note: nothing left uncovered
report (film search down for good): status=partial, covered=3/4, findings=5, errors=1
  note: film not covered: timeout on 'AI in film' and on 'AI in film production'
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
 * one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.
 *
 * <p>The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
 * shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
 */
public final class ResearchRun {
    private static final System.Logger LOG = System.getLogger(ResearchRun.class.getName());
    static final List<String> REQUIRED = List.of("visual arts", "music", "writing", "film");

    record Finding(String claim, String value, String source, String date) {}

    record Task(String scope, String query) {}

    record SearchError(String type, String query, List<Finding> partial, List<String> alternatives, List<String> tried) {}

    /** A result: status "ok" with findings, or "error" with its context; recoveredFrom is set when an alternative query succeeded. */
    record Result(String status, List<Finding> findings, SearchError error, String recoveredFrom) {}

    record Done(Task task, Result result) {}

    record Report(String status, int covered, int findings, int errors, List<String> notes) {}

    /** What the web search subagent finds for a query. */
    static final Map<String, List<Finding>> SOURCES = new LinkedHashMap<>();
    static final Map<String, List<String>> ALTERNATIVES = Map.of("AI in film", List.of("AI in film production"));
    static final Map<String, String> PUBLISHED = Map.of("survey-a", "2025-02-01", "report-d", "2025-01-15"); // what the synthesis agent can check here

    static {
        SOURCES.put("AI in digital art", List.of(new Finding("studios using generative tools", "60%", "survey-a", "2025-02-01")));
        SOURCES.put("AI in graphic design", List.of(new Finding("designers using generative tools weekly", "48%", "survey-b", "2025-03-10")));
        SOURCES.put("AI in photography", List.of(new Finding("agencies labelling generated images", "yes", "policy-c", "2024-11-20")));
        SOURCES.put("AI in music", List.of(new Finding("labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15")));
        SOURCES.put("AI in writing", List.of(new Finding("publishers with an AI policy", "70%", "survey-e", "2025-04-02")));
        SOURCES.put("AI in film production", List.of(new Finding("studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12")));
    }

    /** The search subagent. A failure is returned with its type, the query, the partial results and what to try instead. */
    static Result search(String query, List<String> down) {
        if (down.contains(query) || !SOURCES.containsKey(query)) {
            return new Result("error", null, new SearchError("timeout", query, List.of(), ALTERNATIVES.getOrDefault(query, List.of()), List.of(query)), null);
        }
        return new Result("ok", SOURCES.get(query), null, null);
    }

    static Result search(String query) {
        return search(query, List.of());
    }

    /** The covered scopes and the gaps, in the order of the required scopes. */
    static List<List<String>> coverage(List<Task> plan) {
        List<String> covered = REQUIRED.stream().filter(s -> plan.stream().anyMatch(t -> t.scope().equals(s))).toList();
        return List.of(covered, REQUIRED.stream().filter(s -> !covered.contains(s)).toList());
    }

    /** The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out. */
    static List<Task> replan(List<Task> plan) {
        List<Task> out = new ArrayList<>(plan);
        for (String s : coverage(plan).get(1)) out.add(new Task(s, "AI in " + s));
        return out;
    }

    /** One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries. */
    static Result recover(Result result, List<String> down) {
        SearchError error = result.error();
        if (result.status().equals("ok") || error.alternatives().isEmpty()) return result;
        String alternative = error.alternatives().get(0);
        Result again = search(alternative, down);
        if (again.status().equals("ok")) return new Result("ok", again.findings(), null, error.query());
        List<String> tried = new ArrayList<>(error.tried());
        tried.add(alternative);
        return new Result("error", null, new SearchError(error.type(), error.query(), error.partial(), error.alternatives(), tried), null);
    }

    static List<Done> research(List<Task> plan, List<String> down) {
        return plan.stream().map(t -> new Done(t, recover(search(t.query(), down), down))).toList();
    }

    /** The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator. */
    static String verifyFact(String kind, String source, String value) {
        if (kind.equals("date")) return value.equals(PUBLISHED.get(source)) ? "confirmed" : "mismatch";
        return "needs_search";
    }

    static Report report(List<Done> results) {
        List<String> covered = REQUIRED.stream().filter(s -> results.stream().anyMatch(r -> r.task().scope().equals(s) && r.result().status().equals("ok"))).toList();
        List<String> notes = results.stream().filter(r -> r.result().status().equals("error"))
            .map(r -> r.task().scope() + " not covered: timeout on " + r.result().error().tried().stream().map(q -> "'" + q + "'").collect(Collectors.joining(" and on "))).toList();
        int findings = results.stream().filter(r -> r.result().status().equals("ok")).mapToInt(r -> r.result().findings().size()).sum();
        return new Report(covered.size() == REQUIRED.size() ? "complete" : "partial", covered.size(), findings, notes.size(), notes.isEmpty() ? List.of("nothing left uncovered") : notes);
    }

    public static void main(String[] args) {
        List<Task> narrow = List.of("AI in digital art", "AI in graphic design", "AI in photography").stream().map(q -> new Task("visual arts", q)).toList();
        List<List<String>> c = coverage(narrow);
        System.out.println("plan 1: " + narrow.size() + " subtasks, scopes covered " + c.get(0).size() + " of " + REQUIRED.size() + ", gaps: " + String.join(", ", c.get(1)));
        List<Task> plan = replan(narrow);
        c = coverage(plan);
        System.out.println("plan 2: " + plan.size() + " subtasks, scopes covered " + c.get(0).size() + " of " + REQUIRED.size() + ", gaps: " + (c.get(1).isEmpty() ? "none" : String.join(", ", c.get(1))));
        Result first = search("AI in film");
        SearchError e = first.error();
        System.out.println("search '" + e.query() + "': " + first.status() + " " + e.type() + ", " + e.partial().size() + " partial, alternative '" + e.alternatives().get(0) + "'");
        Done recovered = research(plan, List.of()).stream().filter(r -> r.result().recoveredFrom() != null).findFirst().orElseThrow();
        System.out.println("recovered: '" + recovered.result().recoveredFrom() + "' -> '" + ALTERNATIVES.get(recovered.result().recoveredFrom()).get(0) + "' scope " + recovered.task().scope() + ", " + recovered.result().findings().size() + " finding");
        List<String> verdicts = List.of(verifyFact("date", "survey-a", "2025-02-01"), verifyFact("date", "report-d", "2025-01-15"), verifyFact("statistic", "survey-a", "60%"));
        System.out.println("verify_fact: " + verdicts.stream().filter(v -> v.equals("confirmed")).count() + " confirmed here, " + verdicts.stream().filter(v -> v.equals("needs_search")).count() + " sent back to the coordinator");
        Map<String, List<String>> runs = new LinkedHashMap<>();
        runs.put("all sources up", List.of());
        runs.put("film search down for good", List.of("AI in film", "AI in film production"));
        runs.forEach((label, down) -> {
            Report r = report(research(plan, down));
            System.out.println("report (" + label + "): status=" + r.status() + ", covered=" + r.covered() + "/" + REQUIRED.size() + ", findings=" + r.findings() + ", errors=" + r.errors());
            System.out.println("  note: " + String.join("; ", r.notes()));
        });
    }
}
```
```text
plan 1: 3 subtasks, scopes covered 1 of 4, gaps: music, writing, film
plan 2: 6 subtasks, scopes covered 4 of 4, gaps: none
search 'AI in film': error timeout, 0 partial, alternative 'AI in film production'
recovered: 'AI in film' -> 'AI in film production' scope film, 1 finding
verify_fact: 2 confirmed here, 1 sent back to the coordinator
report (all sources up): status=complete, covered=4/4, findings=6, errors=0
  note: nothing left uncovered
report (film search down for good): status=partial, covered=3/4, findings=5, errors=1
  note: film not covered: timeout on 'AI in film' and on 'AI in film production'
```
```kotlin
private val log = System.getLogger("research_run")

/**
 * A multi-agent research run in miniature: a coordinator that checks its own decomposition, a search subagent whose failure comes back as structured context,
 * one retry through an alternative, a synthesis agent with a scoped verification tool, and a report that says what it could not cover.
 *
 * The subagents are functions over made-up data: this example is about what the coordinator does with what comes back, not about what a model writes. The
 * shapes (a result with a status, an error with a type, the query, partial results and alternatives) are this course's design, not an Anthropic interface.
 */
val REQUIRED = listOf("visual arts", "music", "writing", "film")

data class Finding(val claim: String, val value: String, val source: String, val date: String)

data class Task(val scope: String, val query: String)

data class SearchError(val type: String, val query: String, val partial: List<Finding>, val alternatives: List<String>, val tried: List<String>)

/** A result: status "ok" with findings, or "error" with its context; recoveredFrom is set when an alternative query succeeded. */
data class Result(val status: String, val findings: List<Finding> = listOf(), val error: SearchError? = null, val recoveredFrom: String? = null)

data class Done(val task: Task, val result: Result)

data class Report(val status: String, val covered: Int, val findings: Int, val errors: Int, val notes: List<String>)

/** What the web search subagent finds for a query. */
val SOURCES = mapOf(
    "AI in digital art" to listOf(Finding("studios using generative tools", "60%", "survey-a", "2025-02-01")),
    "AI in graphic design" to listOf(Finding("designers using generative tools weekly", "48%", "survey-b", "2025-03-10")),
    "AI in photography" to listOf(Finding("agencies labelling generated images", "yes", "policy-c", "2024-11-20")),
    "AI in music" to listOf(Finding("labels licensing their catalogues for training", "3 of 5", "report-d", "2025-01-15")),
    "AI in writing" to listOf(Finding("publishers with an AI policy", "70%", "survey-e", "2025-04-02")),
    "AI in film production" to listOf(Finding("studios testing AI previsualisation", "4 of 6", "report-f", "2025-05-12")),
)
val ALTERNATIVES = mapOf("AI in film" to listOf("AI in film production"))
val PUBLISHED = mapOf("survey-a" to "2025-02-01", "report-d" to "2025-01-15") // what the synthesis agent can check here

/** The search subagent. A failure is returned with its type, the query, the partial results and what to try instead. */
fun search(query: String, down: List<String> = listOf()): Result {
    if (query in down || query !in SOURCES) return Result("error", error = SearchError("timeout", query, listOf(), ALTERNATIVES[query] ?: listOf(), listOf(query)))
    return Result("ok", SOURCES.getValue(query))
}

/** The covered scopes and the gaps, in the order of the required scopes. */
fun coverage(plan: List<Task>): Pair<List<String>, List<String>> {
    val covered = REQUIRED.filter { s -> plan.any { it.scope == s } }
    return covered to REQUIRED.filter { it !in covered }
}

/** The coordinator compares its plan with the scopes the question needs and adds a subtask for each scope the plan leaves out. */
fun replan(plan: List<Task>): List<Task> = plan + coverage(plan).second.map { Task(it, "AI in $it") }

/** One retry through an alternative that the error offered. A second failure stays a failure and keeps both queries. */
fun recover(result: Result, down: List<String>): Result {
    val error = result.error
    if (result.status == "ok" || error == null || error.alternatives.isEmpty()) return result
    val alternative = error.alternatives[0]
    val again = search(alternative, down)
    if (again.status == "ok") return Result("ok", again.findings, null, error.query)
    return Result("error", error = error.copy(tried = error.tried + alternative))
}

fun research(plan: List<Task>, down: List<String> = listOf()): List<Done> = plan.map { Done(it, recover(search(it.query, down), down)) }

/** The synthesis agent's scoped tool: a date it can check here, and anything else goes back to the coordinator. */
fun verifyFact(kind: String, source: String, value: String): String =
    if (kind == "date") (if (PUBLISHED[source] == value) "confirmed" else "mismatch") else "needs_search"

fun report(results: List<Done>): Report {
    val covered = REQUIRED.filter { s -> results.any { it.task.scope == s && it.result.status == "ok" } }
    val notes = results.filter { it.result.status == "error" }.map { r -> "${r.task.scope} not covered: timeout on " + r.result.error!!.tried.joinToString(" and on ") { "'$it'" } }
    val findings = results.filter { it.result.status == "ok" }.sumOf { it.result.findings.size }
    return Report(if (covered.size == REQUIRED.size) "complete" else "partial", covered.size, findings, notes.size, notes.ifEmpty { listOf("nothing left uncovered") })
}

fun main() {
    val narrow = listOf("AI in digital art", "AI in graphic design", "AI in photography").map { Task("visual arts", it) }
    var (covered, gaps) = coverage(narrow)
    println("plan 1: ${narrow.size} subtasks, scopes covered ${covered.size} of ${REQUIRED.size}, gaps: ${gaps.joinToString(", ")}")
    val plan = replan(narrow)
    coverage(plan).let { covered = it.first; gaps = it.second }
    println("plan 2: ${plan.size} subtasks, scopes covered ${covered.size} of ${REQUIRED.size}, gaps: ${gaps.joinToString(", ").ifEmpty { "none" }}")
    val first = search("AI in film")
    val e = first.error!!
    println("search '${e.query}': ${first.status} ${e.type}, ${e.partial.size} partial, alternative '${e.alternatives[0]}'")
    val recovered = research(plan).first { it.result.recoveredFrom != null }
    println("recovered: '${recovered.result.recoveredFrom}' -> '${ALTERNATIVES.getValue(recovered.result.recoveredFrom!!)[0]}' scope ${recovered.task.scope}, ${recovered.result.findings.size} finding")
    val verdicts = listOf(verifyFact("date", "survey-a", "2025-02-01"), verifyFact("date", "report-d", "2025-01-15"), verifyFact("statistic", "survey-a", "60%"))
    println("verify_fact: ${verdicts.count { it == "confirmed" }} confirmed here, ${verdicts.count { it == "needs_search" }} sent back to the coordinator")
    for ((label, down) in listOf("all sources up" to listOf<String>(), "film search down for good" to listOf("AI in film", "AI in film production"))) {
        val r = report(research(plan, down))
        println("report ($label): status=${r.status}, covered=${r.covered}/${REQUIRED.size}, findings=${r.findings}, errors=${r.errors}")
        println("  note: ${r.notes.joinToString("; ")}")
    }
}
```
```text
plan 1: 3 subtasks, scopes covered 1 of 4, gaps: music, writing, film
plan 2: 6 subtasks, scopes covered 4 of 4, gaps: none
search 'AI in film': error timeout, 0 partial, alternative 'AI in film production'
recovered: 'AI in film' -> 'AI in film production' scope film, 1 finding
verify_fact: 2 confirmed here, 1 sent back to the coordinator
report (all sources up): status=complete, covered=4/4, findings=6, errors=0
  note: nothing left uncovered
report (film search down for good): status=partial, covered=3/4, findings=5, errors=1
  note: film not covered: timeout on 'AI in film' and on 'AI in film production'
```
<!-- /example -->

The point of the last two lines is the second one. A report that did not cover film is still a useful report, and the status and the note are what keep it honest: the reader learns that a scope is missing, and which queries were tried. All four languages print the same lines.

## Traps

These are the wrong answers that the exam's options for this scenario offer, each with the reason it is rejected.

1. **"The synthesis agent needs instructions to spot missing coverage."** It is tempting because the synthesis agent sees all the findings. The exam rejects it as the root cause when the coordinator's plan was narrow: the subagents did what they were assigned, and the fault is the assignment. Fix the plan.
2. **"Make the search agent's queries broader, or loosen the analysis agent's filter."** It is tempting because the agents are visible in the logs. The exam rejects both for the same reason: each agent worked correctly within its scope, so widening it does not reach the scope that was never assigned.
3. **"Give the synthesis agent every search tool so that it never has to ask."** It is tempting because it removes the round trip. The exam rejects it: it gives one agent the tools of another, and the answer to a frequent small need is one narrow tool.

## Quiz

1. A report on AI in the creative industries covers only visual arts, with nothing on music, writing or film. Every subagent finished its task, and the log shows three subtasks: digital art, graphic design and photography. Which change fixes the cause?
   - **a**: Loosen the relevance filter of the analysis agent so that fewer documents are dropped
   - **b**: Instruct the synthesis agent to look for missing coverage in everything it receives
   - **c**: Widen the queries of the search agent so that it returns more kinds of source
   - **d**: Check the coordinator's plan against the question's scopes, ahead of any delegation

2. A run ends with a report marked complete. The film search timed out, nobody retried it, and the report holds nothing on film. Where is the defect?
   - **a**: The status is not derived from the scopes that were covered
   - **b**: The report agent should have written a longer summary of the other scopes
   - **c**: The search agent should have returned an empty list instead of an error
   - **d**: The timeout was set too short, so the search never had time to finish

3. In the example, the film search times out, the alternative query is down as well, and the run goes on. What does the report carry?
   - **a**: Partial, with a note that names only the first query that failed
   - **b**: Complete, since the alternative query was tried before the scope was given up
   - **c**: Partial, with three of four scopes covered and a note that names both failed queries
   - **d**: Failed, with no report at all because one scope has no findings

<details>
<summary>Answer key</summary>

1. **d**. The subagents did what they were assigned, so the plan is what to check, and it can be checked against the question. *b* is ruled out because it is a late substitute: "A downstream agent that is asked to notice gaps is a late and unreliable substitute for a check that costs one comparison." *c* is ruled out because the search agent worked within its scope: "each agent worked correctly within its scope, so widening it does not reach the scope that was never assigned". *a* is ruled out for the same reason: "the subagents did what they were assigned, and the fault is the assignment".
2. **a**. A status must come from coverage. *b* is ruled out because more prose does not repair the claim: "A run is complete when every scope is covered." *c* is ruled out because an empty success hides the gap: "A failure with no context, or a failure turned into a success". *d* is ruled out because the defect is in what the report claims and not in how long the search waited: "It is partial otherwise, whatever the reason, and the report says which scopes are missing and why."
3. **c**. The page says what the second run shows: "In that second run the status is `partial`, three of four scopes are covered, and the note names the two queries that failed." *b* is ruled out because trying is not covering: "A run is complete when every scope is covered." *a* is ruled out because the note names both: "the note names the two queries that failed". *d* is ruled out because the report still has value: "A report that did not cover film is still a useful report".

</details>

Adapted from the sample questions of the Claude Certified Architect, Foundations exam guide, version 1.0 (Anthropic), with credit; the questions are Anthropic's. The first question follows the guide's sample question on a report that covers only part of a topic, rewritten here.
