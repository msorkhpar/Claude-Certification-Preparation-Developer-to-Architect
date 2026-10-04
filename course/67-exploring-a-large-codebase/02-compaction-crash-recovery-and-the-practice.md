# Compaction, crash recovery and the practice

**Level:** Architect · **Module 67:** Exploring a large codebase · **Page 2 of 2**
**Exams:** A5.4; S4

**After this page you can** steer `/compact` and automatic compaction so that what an exploration needs survives them, choose between compacting and clearing, design the state export and the manifest that let a coordinator recover after a crash, read a manifest against the files that exist and decide for each agent whether to reuse, resume or restart it, build the prompt that continues an agent from its exported state, and write the module's practice.

Checked on 2026-10-04 against the exam guide's task statement 5.4 and scenario S4, the Claude Code documentation pages "Best practices for Claude Code" (compaction, `/clear`, summarising from a checkpoint) and "Manage costs effectively" (compaction instructions in `CLAUDE.md`), and the Claude Agent SDK page on subagents (resuming a subagent). Nothing here called a model: the example is a crash and a recovery over an in-memory file system, and the practice is graded by Python, TypeScript, Java and Kotlin test suites, offline. The first page covered why explorations degrade and where findings go; this page covers what survives compaction and what survives a crash.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* use `/compact` to reduce context usage "during extended exploration sessions when context fills with verbose discovery output"; for crash recovery, "each agent exports state to a known location, and the coordinator loads a manifest on resume", injecting the exported state into the agents' prompts. *What the product documents now (read 2026-10-04):* Claude Code "automatically compacts conversation history when you approach context limits"; `/compact` accepts instructions ("`/compact Focus on the API changes`"); a project instructions file can steer compaction with a sentence such as "When compacting, always preserve the full list of modified files and any test commands"; Esc then Esc (or `/rewind`) opens a checkpoint from which to summarise either forwards or backwards; `/clear` "resets the context window entirely"; and a custom subagent can be resumed with its full history, while the built-in `Explore` and `Plan` agents are one-shot. The product has compaction and subagent resumption. It has no coordinator manifest: the manifest, the state files and the decision on resume are yours. On the exam, a question about recovery after a crash has the answer: state exported to a known place, a manifest, and a coordinator that reads the manifest and injects the state.

## Why it matters

Six agents explore six modules of a large service for most of an afternoon. The machine restarts. Without a design, the coordinator has nothing: six transcripts that were in memory, a conversation that no longer exists, and an afternoon to repeat. With a design, each agent wrote what it learned to a file as it went, the coordinator wrote one line per agent into a manifest, and on restart it reads that file, sees that four agents finished, one was half-way and one had not started, and runs only the work that is missing, handing the survivors' findings to the agents that continue. The same session may also hit the window limit long before any crash, and then the question is what compaction keeps.

## The idea

### Steer compaction

Compaction replaces the conversation with a summary. Left to itself the summary keeps what looks important, which in an exploration is rarely the list of files already read, the commands that run the tests, or the open questions. Three controls exist:

- run `/compact` with instructions: `/compact Focus on the list of files read and the open questions`;
- put standing instructions in the project's instructions file ("When compacting, always preserve the full list of modified files and any test commands"), which also steers the automatic compaction that happens near the limit;
- summarise only part of the conversation, from a checkpoint, when the early context is the part to keep.

Turning compaction off is not a remedy: the window then fills, and performance degrades. Compaction is not the same as `/clear`. Compaction continues the same work with a shorter memory of it. `/clear` starts over and is right between unrelated tasks, and wrong in the middle of an exploration whose findings are only in the window. The reliable version of "keep this" is the scratchpad of page 1, since a summary is a lossy copy and a file is not. The practice's `compact_command` builds the command with its focus.

### State export and the manifest

A crash does not warn. The design that survives one has two parts written as the work goes.

**Each agent exports its state to a known location**, a file per agent whose path is fixed by the design (`state/<agent>.md`), holding its findings in the scratchpad form of page 1: specific, once, grouped. It is written as the agent learns and not only at the end, so that a crash half-way leaves something.

**The coordinator keeps a manifest**: one entry per agent with its name, its state file and its status (`running`, `done`, `failed`). It writes an agent's entry when the agent starts, so that a crash leaves a trace of work that began and has nothing to show. The manifest is small, readable by a person, and the one thing the coordinator reads first.

### Reading the manifest on resume

On restart the coordinator loads the manifest and compares it with the files that exist. The rule of the practice has three branches and one principle:

| State file | Status | Action |
|---|---|---|
| Missing | any | `restart`: there is nothing to continue from, whatever the manifest says |
| Exists | `done` | `reuse`: do not run it again; its findings go into the next prompts |
| Exists | `running` or `failed` | `resume`: continue from the exported state |

The principle is that the file outranks the status. A status is a claim written at one moment; a state file is evidence. A manifest that says `done` for an agent whose file is missing was written by a crash between two steps, and trusting it loses that agent's work without anyone noticing. The same discipline protects against a manifest that says `running` for an agent that finished and wrote its file just before the crash: the file is there, so the work is kept.

A resumed or restarted agent gets a prompt that carries its task, the state lines it exported, and one instruction: continue from the first unfinished step. It does not get the old transcript, which is what filled the first window. The practice's `resume_prompt` is that shape. When the agent is a custom subagent that was resumable in the same session, resuming it with its own history is an alternative, but a crash that ended the session leaves only the files.

### The example

<!-- example: m67-state-manifest tabs: python,typescript,java,kotlin -->
```python
"""Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.

The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a dictionary
stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
"""
MANIFEST = "state/manifest.txt"
TRANSCRIPT_CHARS = {"auth": 3200, "billing": 2400, "search": 1600}


def tokens(text_or_chars):
    chars = text_or_chars if isinstance(text_or_chars, int) else len(text_or_chars)
    return -(-chars // 4)


def read_manifest(fs):
    entries = {}
    for line in fs.get(MANIFEST, "").splitlines():
        name, status, path = line.split("|")
        entries[name] = (status, path)
    return entries


def write_manifest(fs, entries):
    fs[MANIFEST] = "\n".join(f"{name}|{status}|{path}" for name, (status, path) in entries.items())


def start(fs, agent):
    """The manifest is written when the agent starts, so that a crash leaves a trace."""
    entries = read_manifest(fs)
    entries[agent] = ("running", f"state/{agent}.md")
    write_manifest(fs, entries)


def finish(fs, agent, findings):
    """The state file is written when the agent has something to keep, and the manifest then says done."""
    entries = read_manifest(fs)
    path = entries[agent][1]
    fs[path] = "\n".join(f"- {fact} ({where})" for fact, where in findings)
    entries[agent] = ("done", path)
    write_manifest(fs, entries)


def recovery_plan(fs, planned):
    entries = read_manifest(fs)
    plan = []
    for agent in planned:
        status, path = entries.get(agent, ("running", f"state/{agent}.md"))
        plan.append((agent, "restart" if path not in fs else "reuse" if status == "done" else "resume"))
    return plan


def injected_state(fs, plan):
    """What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again."""
    entries = read_manifest(fs)
    return "\n".join(f"{agent}:\n{fs[entries[agent][1]]}" for agent, action in plan if action in ("reuse", "resume"))


def main():
    fs = {}
    work = {
        "auth": [("sessions expire after 30 minutes", "auth/Session.java:18"), ("tokens are signed in TokenSigner", "auth/TokenSigner.java:12"), ("the login route is POST /login", "auth/Routes.java:7")],
        "billing": [("amounts are integer cents", "billing/Money.java:5"), ("refunds go through RefundService", "billing/RefundService.java:41")],
    }
    for agent, findings in work.items():
        start(fs, agent)
        finish(fs, agent, findings)
        print(f"{agent}: exported {read_manifest(fs)[agent][1]} ({len(findings)} findings), manifest says {read_manifest(fs)[agent][0]}")
    start(fs, "search")
    print("search: manifest says running, state file never written (crash)")
    plan = recovery_plan(fs, ["auth", "billing", "search"])
    print("recovery plan: " + ", ".join(f"{agent} {action}" for agent, action in plan))
    state = injected_state(fs, plan)
    replay = sum(tokens(n) for n in TRANSCRIPT_CHARS.values())
    print(f"injected into the next phase: {sum(len(f) for f in work.values())} findings from {len(work)} agents, about {tokens(state)} tokens")
    print(f"replaying the three transcripts instead: about {replay} tokens")


if __name__ == "__main__":
    main()
```
```text
auth: exported state/auth.md (3 findings), manifest says done
billing: exported state/billing.md (2 findings), manifest says done
search: manifest says running, state file never written (crash)
recovery plan: auth reuse, billing reuse, search restart
injected into the next phase: 5 findings from 2 agents, about 77 tokens
replaying the three transcripts instead: about 1800 tokens
```
```typescript
/**
 * Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.
 *
 * The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
 * restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a dictionary
 * stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
 */
export type Fs = Record<string, string>;
const MANIFEST = "state/manifest.txt";
const TRANSCRIPT_CHARS: Record<string, number> = { auth: 3200, billing: 2400, search: 1600 };

export function tokens(textOrChars: string | number): number {
  const chars = typeof textOrChars === "number" ? textOrChars : textOrChars.length;
  return Math.ceil(chars / 4);
}

export function readManifest(fs: Fs): Record<string, [string, string]> {
  const entries: Record<string, [string, string]> = {};
  for (const line of (fs[MANIFEST] ?? "").split("\n").filter((l) => l !== "")) {
    const [name, status, path] = line.split("|");
    entries[name] = [status, path];
  }
  return entries;
}

function writeManifest(fs: Fs, entries: Record<string, [string, string]>): void {
  fs[MANIFEST] = Object.entries(entries).map(([name, [status, path]]) => `${name}|${status}|${path}`).join("\n");
}

/** The manifest is written when the agent starts, so that a crash leaves a trace. */
export function start(fs: Fs, agent: string): void {
  const entries = readManifest(fs);
  entries[agent] = ["running", `state/${agent}.md`];
  writeManifest(fs, entries);
}

/** The state file is written when the agent has something to keep, and the manifest then says done. */
export function finish(fs: Fs, agent: string, findings: Array<[string, string]>): void {
  const entries = readManifest(fs);
  const path = entries[agent][1];
  fs[path] = findings.map(([fact, where]) => `- ${fact} (${where})`).join("\n");
  entries[agent] = ["done", path];
  writeManifest(fs, entries);
}

export function recoveryPlan(fs: Fs, planned: string[]): Array<[string, string]> {
  const entries = readManifest(fs);
  return planned.map((agent): [string, string] => {
    const [status, path] = entries[agent] ?? ["running", `state/${agent}.md`];
    return [agent, !(path in fs) ? "restart" : status === "done" ? "reuse" : "resume"];
  });
}

/** What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again. */
export function injectedState(fs: Fs, plan: Array<[string, string]>): string {
  const entries = readManifest(fs);
  return plan.filter(([, action]) => action === "reuse" || action === "resume").map(([agent]) => `${agent}:\n${fs[entries[agent][1]]}`).join("\n");
}

function main() {
  const fs: Fs = {};
  const work: Record<string, Array<[string, string]>> = {
    auth: [["sessions expire after 30 minutes", "auth/Session.java:18"], ["tokens are signed in TokenSigner", "auth/TokenSigner.java:12"], ["the login route is POST /login", "auth/Routes.java:7"]],
    billing: [["amounts are integer cents", "billing/Money.java:5"], ["refunds go through RefundService", "billing/RefundService.java:41"]],
  };
  for (const [agent, findings] of Object.entries(work)) {
    start(fs, agent);
    finish(fs, agent, findings);
    console.log(`${agent}: exported ${readManifest(fs)[agent][1]} (${findings.length} findings), manifest says ${readManifest(fs)[agent][0]}`);
  }
  start(fs, "search");
  console.log("search: manifest says running, state file never written (crash)");
  const plan = recoveryPlan(fs, ["auth", "billing", "search"]);
  console.log("recovery plan: " + plan.map(([agent, action]) => `${agent} ${action}`).join(", "));
  const state = injectedState(fs, plan);
  const replay = Object.values(TRANSCRIPT_CHARS).reduce((sum, n) => sum + tokens(n), 0);
  console.log(`injected into the next phase: ${Object.values(work).reduce((s, f) => s + f.length, 0)} findings from ${Object.keys(work).length} agents, about ${tokens(state)} tokens`);
  console.log(`replaying the three transcripts instead: about ${replay} tokens`);
}

if (import.meta.main) main();
```
```text
auth: exported state/auth.md (3 findings), manifest says done
billing: exported state/billing.md (2 findings), manifest says done
search: manifest says running, state file never written (crash)
recovery plan: auth reuse, billing reuse, search restart
injected into the next phase: 5 findings from 2 agents, about 77 tokens
replaying the three transcripts instead: about 1800 tokens
```
```java
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.
 *
 * <p>The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
 * restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a map
 * stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
 */
public final class StateManifest {
    static final String MANIFEST = "state/manifest.txt";

    record Entry(String status, String path) {}

    record Finding(String fact, String where) {}

    record Step(String agent, String action) {}

    static int tokens(int chars) {
        return (chars + 3) / 4;
    }

    static int tokens(String text) {
        return tokens(text.length());
    }

    static Map<String, Entry> readManifest(Map<String, String> fs) {
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (String line : fs.getOrDefault(MANIFEST, "").split("\n")) {
            if (line.isEmpty()) continue;
            String[] p = line.split("\\|");
            entries.put(p[0], new Entry(p[1], p[2]));
        }
        return entries;
    }

    private static void writeManifest(Map<String, String> fs, Map<String, Entry> entries) {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) lines.add(e.getKey() + "|" + e.getValue().status() + "|" + e.getValue().path());
        fs.put(MANIFEST, String.join("\n", lines));
    }

    /** The manifest is written when the agent starts, so that a crash leaves a trace. */
    static void start(Map<String, String> fs, String agent) {
        Map<String, Entry> entries = readManifest(fs);
        entries.put(agent, new Entry("running", "state/" + agent + ".md"));
        writeManifest(fs, entries);
    }

    /** The state file is written when the agent has something to keep, and the manifest then says done. */
    static void finish(Map<String, String> fs, String agent, List<Finding> findings) {
        Map<String, Entry> entries = readManifest(fs);
        String path = entries.get(agent).path();
        List<String> lines = new ArrayList<>();
        for (Finding f : findings) lines.add("- " + f.fact() + " (" + f.where() + ")");
        fs.put(path, String.join("\n", lines));
        entries.put(agent, new Entry("done", path));
        writeManifest(fs, entries);
    }

    static List<Step> recoveryPlan(Map<String, String> fs, List<String> planned) {
        Map<String, Entry> entries = readManifest(fs);
        List<Step> plan = new ArrayList<>();
        for (String agent : planned) {
            Entry e = entries.getOrDefault(agent, new Entry("running", "state/" + agent + ".md"));
            plan.add(new Step(agent, !fs.containsKey(e.path()) ? "restart" : e.status().equals("done") ? "reuse" : "resume"));
        }
        return plan;
    }

    /** What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again. */
    static String injectedState(Map<String, String> fs, List<Step> plan) {
        Map<String, Entry> entries = readManifest(fs);
        List<String> parts = new ArrayList<>();
        for (Step s : plan) if (s.action().equals("reuse") || s.action().equals("resume")) parts.add(s.agent() + ":\n" + fs.get(entries.get(s.agent()).path()));
        return String.join("\n", parts);
    }

    public static void main(String[] args) {
        Map<String, String> fs = new LinkedHashMap<>();
        Map<String, List<Finding>> work = new LinkedHashMap<>();
        work.put("auth", List.of(new Finding("sessions expire after 30 minutes", "auth/Session.java:18"), new Finding("tokens are signed in TokenSigner", "auth/TokenSigner.java:12"), new Finding("the login route is POST /login", "auth/Routes.java:7")));
        work.put("billing", List.of(new Finding("amounts are integer cents", "billing/Money.java:5"), new Finding("refunds go through RefundService", "billing/RefundService.java:41")));
        int total = 0;
        for (Map.Entry<String, List<Finding>> w : work.entrySet()) {
            start(fs, w.getKey());
            finish(fs, w.getKey(), w.getValue());
            total += w.getValue().size();
            System.out.println(w.getKey() + ": exported " + readManifest(fs).get(w.getKey()).path() + " (" + w.getValue().size() + " findings), manifest says " + readManifest(fs).get(w.getKey()).status());
        }
        start(fs, "search");
        System.out.println("search: manifest says running, state file never written (crash)");
        List<Step> plan = recoveryPlan(fs, List.of("auth", "billing", "search"));
        List<String> parts = new ArrayList<>();
        for (Step s : plan) parts.add(s.agent() + " " + s.action());
        System.out.println("recovery plan: " + String.join(", ", parts));
        String state = injectedState(fs, plan);
        int replay = tokens(3200) + tokens(2400) + tokens(1600);
        System.out.println("injected into the next phase: " + total + " findings from " + work.size() + " agents, about " + tokens(state) + " tokens");
        System.out.println("replaying the three transcripts instead: about " + replay + " tokens");
    }
}
```
```text
auth: exported state/auth.md (3 findings), manifest says done
billing: exported state/billing.md (2 findings), manifest says done
search: manifest says running, state file never written (crash)
recovery plan: auth reuse, billing reuse, search restart
injected into the next phase: 5 findings from 2 agents, about 77 tokens
replaying the three transcripts instead: about 1800 tokens
```
```kotlin
/**
 * Surviving a crash during a long exploration: each agent exports its state to a known place, and the coordinator reads a manifest on resume.
 *
 * The exam guide (task 5.4) describes crash recovery as agents that export structured state to a known location and a coordinator that loads a manifest on resume and injects the state into the prompts of the agents it
 * restarts. The Claude Code documentation (read 2026-10-04) says that subagents explore in a separate context and report back summaries, and that a context window which fills up degrades Claude's work. Below, a map
 * stands for the file system, three agents explore three modules, one crashes, and the coordinator recovers. The sizes of the transcripts are invented for the illustration; nothing here calls a model.
 */
const val MANIFEST = "state/manifest.txt"

data class Entry(val status: String, val path: String)

data class Finding(val fact: String, val where: String)

data class Step(val agent: String, val action: String)

fun tokens(chars: Int): Int = (chars + 3) / 4

fun tokens(text: String): Int = tokens(text.length)

fun readManifest(fs: Map<String, String>): LinkedHashMap<String, Entry> {
    val entries = linkedMapOf<String, Entry>()
    for (line in (fs[MANIFEST] ?: "").lines().filter { it.isNotEmpty() }) {
        val (name, status, path) = line.split("|")
        entries[name] = Entry(status, path)
    }
    return entries
}

private fun writeManifest(fs: MutableMap<String, String>, entries: Map<String, Entry>) {
    fs[MANIFEST] = entries.entries.joinToString("\n") { "${it.key}|${it.value.status}|${it.value.path}" }
}

/** The manifest is written when the agent starts, so that a crash leaves a trace. */
fun start(fs: MutableMap<String, String>, agent: String) {
    val entries = readManifest(fs)
    entries[agent] = Entry("running", "state/$agent.md")
    writeManifest(fs, entries)
}

/** The state file is written when the agent has something to keep, and the manifest then says done. */
fun finish(fs: MutableMap<String, String>, agent: String, findings: List<Finding>) {
    val entries = readManifest(fs)
    val path = entries.getValue(agent).path
    fs[path] = findings.joinToString("\n") { "- ${it.fact} (${it.where})" }
    entries[agent] = Entry("done", path)
    writeManifest(fs, entries)
}

fun recoveryPlan(fs: Map<String, String>, planned: List<String>): List<Step> {
    val entries = readManifest(fs)
    return planned.map { agent ->
        val e = entries[agent] ?: Entry("running", "state/$agent.md")
        Step(agent, if (e.path !in fs) "restart" else if (e.status == "done") "reuse" else "resume")
    }
}

/** What the coordinator puts into the next phase's prompt: the exported findings of every agent that need not run again. */
fun injectedState(fs: Map<String, String>, plan: List<Step>): String {
    val entries = readManifest(fs)
    return plan.filter { it.action == "reuse" || it.action == "resume" }.joinToString("\n") { "${it.agent}:\n${fs.getValue(entries.getValue(it.agent).path)}" }
}

fun main() {
    val fs = linkedMapOf<String, String>()
    val work = linkedMapOf(
        "auth" to listOf(Finding("sessions expire after 30 minutes", "auth/Session.java:18"), Finding("tokens are signed in TokenSigner", "auth/TokenSigner.java:12"), Finding("the login route is POST /login", "auth/Routes.java:7")),
        "billing" to listOf(Finding("amounts are integer cents", "billing/Money.java:5"), Finding("refunds go through RefundService", "billing/RefundService.java:41")),
    )
    for ((agent, findings) in work) {
        start(fs, agent)
        finish(fs, agent, findings)
        println("$agent: exported ${readManifest(fs).getValue(agent).path} (${findings.size} findings), manifest says ${readManifest(fs).getValue(agent).status}")
    }
    start(fs, "search")
    println("search: manifest says running, state file never written (crash)")
    val plan = recoveryPlan(fs, listOf("auth", "billing", "search"))
    println("recovery plan: " + plan.joinToString(", ") { "${it.agent} ${it.action}" })
    val state = injectedState(fs, plan)
    val replay = tokens(3200) + tokens(2400) + tokens(1600)
    println("injected into the next phase: ${work.values.sumOf { it.size }} findings from ${work.size} agents, about ${tokens(state)} tokens")
    println("replaying the three transcripts instead: about $replay tokens")
}
```
```text
auth: exported state/auth.md (3 findings), manifest says done
billing: exported state/billing.md (2 findings), manifest says done
search: manifest says running, state file never written (crash)
recovery plan: auth reuse, billing reuse, search restart
injected into the next phase: 5 findings from 2 agents, about 77 tokens
replaying the three transcripts instead: about 1800 tokens
```
<!-- /example -->

### The practice: an exploration that can be resumed

The practice is in [`exercises/67-exploring-a-large-codebase`](../../exercises/67-exploring-a-large-codebase/unit-01/practice-1/statement.md). You write the scratchpad (findings recorded once, grouped by area), the manifest, the plan for resuming, the prompt that continues an agent and the compaction command. It is graded in Python, TypeScript, Java and Kotlin; the statement lists eight cases, each saying what you should see when it works. The outline marks this module as configuration, and the practice treats the manifest and the compaction instructions as the configuration of an exploration.

## Traps

1. **"Compact the session; the summary will keep what matters."** It is tempting because compaction is automatic. The exam rejects it: a summary keeps what looks important, so tell it what to keep, and keep the findings in a file.
2. **"Clear the context when it fills; the agent will rediscover everything."** It is tempting because it is the cheapest reset. The exam rejects it: clearing in the middle of an exploration throws away findings that exist only in the window.
3. **"The manifest says the agent is done; skip it."** It is tempting because the manifest is the record. The exam rejects it: a state file that does not exist outranks a status that says done, and the agent restarts.
4. **"After the crash, replay every agent's transcript into the new coordinator."** It is tempting because it restores everything. The exam rejects it: the transcripts are what filled the window. Load the manifest and inject the state, not the history.

## Quiz

1. Scenario S4, developer productivity with Claude. During a long exploration the team wants automatic summarising near the limit to keep the roster of sources already opened and the commands that run the tests. What should they do?
   - **a**: Turn automatic summarising off so that nothing is condensed at all
   - **b**: Add a sentence on what to preserve to the project's instructions file
   - **c**: Raise the output limit so that the summary can run longer than usual
   - **d**: Clear the conversation at each phase so that nothing needs preserving

2. Scenario S4, developer productivity with Claude. A coordinator restarts after a crash. Its manifest lists 6 agents as running, and notes written by only 4 of them can be found on disk. What should it do?
   - **a**: Restart the whole set, since a manifest written before a crash cannot be trusted
   - **b**: Resume each of the 6 on the manifest's word, giving empty notes to the 2 without any
   - **c**: Replay the 6 transcripts into the new coordinator before it continues
   - **d**: Carry on from the saved material where it exists and begin afresh where it does not

<details>
<summary>Answer key</summary>

1. **b**. A standing instruction in the project's file steers the automatic summary toward what the exploration needs. *a* is ruled out because "Turning compaction off is not a remedy: the window then fills, and performance degrades". *c* is ruled out because "Left to itself the summary keeps what looks important", and a longer summary still chooses by that. *d* is ruled out because clearing "starts over and is right between unrelated tasks", and a phase of the same exploration is not one.
2. **d**. The agents with files continue from them and the others start over, since the file outranks the status. *a* is ruled out because for an agent whose file survived, "the file is there, so the work is kept". *b* is ruled out because for a missing file there is "nothing to continue from, whatever the manifest says". *c* is ruled out because "the transcripts are what filled the window".

</details>

## Module quiz

This quiz covers both pages of the module.

1. Scenario S4, developer productivity with Claude. After a crash, one agent is started again from material it wrote out earlier. What should its new prompt hold?
   - **a**: Its assignment alone, because the saved material is implied by it
   - **b**: Its assignment and the complete old transcript, so that nothing is lost
   - **c**: Its assignment, the exported notes and an instruction to continue
   - **d**: The saved notes alone, because the assignment is contained in them

2. Scenario S4, developer productivity with Claude. A team needs a code-searching helper whose work can be picked up later in the same session. Which fits?
   - **a**: The built-in Explore subagent, continued with its own history
   - **b**: A custom subagent, continued with its own history
   - **c**: A fresh Explore each time, handed a copy of the earlier transcript
   - **d**: A second coordinator that reads the first coordinator's conversation

3. Scenario S4, developer productivity with Claude. A session finishes one exploration, and an unrelated piece of work then begins in the same window. Which step fits?
   - **a**: Turn compaction off for the new job so that no detail is lost
   - **b**: Compact it with instructions to keep everything the first job found
   - **c**: Enlarge the context limit so that both jobs fit side by side
   - **d**: Clear the conversation, since nothing from the first job is needed

<details>
<summary>Answer key</summary>

1. **c**. The page says the prompt "carries its task, the state lines it exported, and one instruction: continue from the first unfinished step". *b* is ruled out because "It does not get the old transcript, which is what filled the first window". *a* is ruled out because the prompt "carries its task, the state lines it exported", so the notes are not implied. *d* is ruled out because the prompt "carries its task, the state lines it exported", and the task comes first.
2. **b**. A custom subagent can be resumed with its history, and the built-in one cannot. *a* is ruled out because "The built-in Explore agent is one-shot". *c* is ruled out because "the transcripts are what filled the window". *d* is ruled out because "a crash that ended the session leaves only the files", and an ordinary session leaves a resumable subagent.
3. **d**. The page says "`/clear` starts over and is right between unrelated tasks". *b* is ruled out because "Compaction continues the same work with a shorter memory of it". *c* is ruled out because "a bigger window only moves the problem later". *a* is ruled out because "Turning compaction off is not a remedy: the window then fills, and performance degrades".

</details>

Adapted from the Claude Certified Architect - Foundations exam preparation guide by Daron Yondem (CC BY 4.0, https://creativecommons.org/licenses/by/4.0/), changed for this course.
