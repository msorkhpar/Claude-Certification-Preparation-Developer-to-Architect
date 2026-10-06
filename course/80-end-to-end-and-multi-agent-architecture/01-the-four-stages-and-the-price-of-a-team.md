# The four stages and the price of a team

**Level:** Architect Professional · **Module 80:** End-to-end and multi-agent architecture · **Page 1 of 2**
**Exams:** P1

**After this page you can** describe a solution as four stages (input, processing, output, feedback) and find the stage that is missing, pick the lowest pattern a task can stand on with the ladder of module 79 written as code, price a workflow, an agent and a team of agents with the two token multipliers Anthropic reports, and say when a team of agents earns its cost.

Checked on 2026-10-04 against Anthropic's engineering articles "Building effective agents" and "How we built our multi-agent research system", and the Claude Certified Architect, Professional exam guide v1.0 (July 2026), domain 1. The example ran in the course's container in Python, TypeScript, Java and Kotlin with the same output and calls no model. Its prices are the articles' reported multipliers applied to made-up task values, a teaching model and not a measurement of your workload.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domain 1 asks the candidate to design an end-to-end solution, and to choose between a workflow, an agent and a multi-agent system by the shape of the work. *What Anthropic's articles say:* the multi-agent write-up reports that "agents typically use about 4× more tokens than chat interactions, and multi-agent systems use about 15× more tokens than chats", so that for economic viability the task's value must be high enough to pay for the extra cost. *How to read both:* the exam keys the lowest pattern that meets the requirement, and a team of agents only for work that splits into independent parts and is worth the multiple. The multipliers are the articles' figures. Your own workload has its own, found by measuring (module 84).

## Why it matters

A review board receives three proposals for the same intake service: a fixed workflow, one agent with tools, and a coordinator with five subagents. Each slide looks modern. The architect's job is to read them on one page, with the same questions for each: what comes in, what is done to it, what leaves and how, and how the system learns when it is wrong. A design that cannot answer those questions is a demo. Then comes the price question, which the team proposal tends to skip.

## The idea

### Four stages that every end-to-end design has

A solution is more than the model call. Draw it as four stages and ask of each design whether it has all four.

| Stage | What it holds | What a missing one looks like |
|---|---|---|
| **Input** | Where requests come from, how they are validated and bounded, what is stripped before the model sees it | Raw text goes straight to the model |
| **Processing** | The model calls, the tools, the retrieval and the code that orders them (the pattern of module 79) | Everything is one long prompt |
| **Output** | How a result is checked and delivered, to a person or to another system | The model's text is written straight into a database |
| **Feedback** | Evaluation, logging, human corrections, and the path by which they change the system | Nobody learns that it was wrong |

The fourth stage is the one most proposals omit. A pipeline that takes input, processes it and answers, and never learns from the result, goes wrong silently and stays wrong. An architecture review therefore starts with the four rows and records a finding for each stage that is absent. The practice of this module is exactly that: a rubric in code, so that the three designs are judged by the same rules.

### The ladder as code

Module 79 gave the ladder in prose: one call, an augmented call, a workflow, an agent, a team. The example turns it into a rule that is short enough to read in a minute. Three questions decide the rung. Is the task one step? Then it is a call, augmented if it needs outside facts. Is the path known? Then it is a workflow, and it costs one chat per step. Is the path open? Then it is an agent, and it becomes a team only when the parts are independent and the value pays for the team.

The price uses the multipliers from the article: a chat is the unit, an agent costs about 4 units and a team about 15. For a team to be worth building, the task's value has to be at least 15 chats. The last task in the example shows the failure the rule prevents: a trivia round-up has independent parts, so it looks like a fit for a team, but its value is below what even one agent costs, and so below the price of a team, and the output says `pays: False`. The answer is not a cheaper team. It is to ask whether the task is worth automating at all.

### When a team earns its cost

A coordinator with subagents is the most expensive rung, so it needs three reasons together.

1. **The parts are independent.** Subagents read, search or analyse apart from one another. Work in which every step depends on the previous result, or in which all agents must share one context, cannot be split without losing information.
2. **The volume exceeds one context.** The reading that has to be done is larger than one agent can hold, or parallel work shortens the wait enough to matter.
3. **The value pays the multiple.** About 15 times a chat is the article's figure for a research system. A task worth less than that is not a candidate, however neat the decomposition.

A team that fails the first test is a worse single agent, and a team that fails the third loses money on every run. Choosing a team for the architecture diagram's sake is the failure this page is about.

### The example

<!-- example: m80-pattern-ladder tabs: python,typescript,java,kotlin -->
```python
"""The pattern ladder: which rung a task needs, and whether its value pays for the rung.

Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
a measurement of your workload, and the rule is the course's own teaching model.
"""
import logging

log = logging.getLogger(__name__)

MULTIPLIER = {"plain call": 1, "augmented call": 1, "agent": 4, "multi-agent": 15}  # a workflow costs one chat per step

TASKS = [
    {"name": "classify ticket", "one_step": True, "steps": 1, "needs_external": False, "steps_known": True, "independent_parts": False, "value": 0.05, "chat_cost": 0.02},
    {"name": "answer from policy", "one_step": True, "steps": 1, "needs_external": True, "steps_known": True, "independent_parts": False, "value": 0.40, "chat_cost": 0.02},
    {"name": "claims intake", "one_step": False, "steps": 4, "needs_external": True, "steps_known": True, "independent_parts": False, "value": 6.00, "chat_cost": 0.02},
    {"name": "investigate outage", "one_step": False, "steps": 0, "needs_external": True, "steps_known": False, "independent_parts": False, "value": 40.00, "chat_cost": 0.02},
    {"name": "market research brief", "one_step": False, "steps": 0, "needs_external": True, "steps_known": False, "independent_parts": True, "value": 25.00, "chat_cost": 0.02},
    {"name": "trivia round-up", "one_step": False, "steps": 0, "needs_external": True, "steps_known": False, "independent_parts": True, "value": 0.05, "chat_cost": 0.02},
]


def choose_pattern(task):
    """The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost."""
    log.debug("choose_pattern input: %r", task)
    if task["one_step"]:
        return "augmented call" if task["needs_external"] else "plain call"
    if task["steps_known"]:
        return "workflow"
    if task["independent_parts"] and task["value"] >= MULTIPLIER["multi-agent"] * task["chat_cost"]:
        return "multi-agent"
    return "agent"


def cost(task, pattern):
    multiplier = task["steps"] if pattern == "workflow" else MULTIPLIER[pattern]
    return multiplier * task["chat_cost"]


def main():
    for task in TASKS:
        pattern = choose_pattern(task)
        price = cost(task, pattern)
        print(f"{task['name']}: {pattern}, cost {price:.2f}, value {task['value']:.2f}, pays: {task['value'] >= price}")


if __name__ == "__main__":
    main()
```
```text
classify ticket: plain call, cost 0.02, value 0.05, pays: True
answer from policy: augmented call, cost 0.02, value 0.40, pays: True
claims intake: workflow, cost 0.08, value 6.00, pays: True
investigate outage: agent, cost 0.08, value 40.00, pays: True
market research brief: multi-agent, cost 0.30, value 25.00, pays: True
trivia round-up: agent, cost 0.08, value 0.05, pays: False
```
```typescript
// The pattern ladder: which rung a task needs, and whether its value pays for the rung.
//
// Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
// complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
// interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
// the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
// a measurement of your workload, and the rule is the course's own teaching model.
import { logger } from "./logger.ts";
const log = logger("pattern_ladder");

export type Task = { name: string; one_step: boolean; steps: number; needs_external: boolean; steps_known: boolean; independent_parts: boolean; value: number; chat_cost: number };

export const MULTIPLIER: Record<string, number> = { "plain call": 1, "augmented call": 1, agent: 4, "multi-agent": 15 }; // a workflow costs one chat per step

export const TASKS: Task[] = [
  { name: "classify ticket", one_step: true, steps: 1, needs_external: false, steps_known: true, independent_parts: false, value: 0.05, chat_cost: 0.02 },
  { name: "answer from policy", one_step: true, steps: 1, needs_external: true, steps_known: true, independent_parts: false, value: 0.40, chat_cost: 0.02 },
  { name: "claims intake", one_step: false, steps: 4, needs_external: true, steps_known: true, independent_parts: false, value: 6.00, chat_cost: 0.02 },
  { name: "investigate outage", one_step: false, steps: 0, needs_external: true, steps_known: false, independent_parts: false, value: 40.00, chat_cost: 0.02 },
  { name: "market research brief", one_step: false, steps: 0, needs_external: true, steps_known: false, independent_parts: true, value: 25.00, chat_cost: 0.02 },
  { name: "trivia round-up", one_step: false, steps: 0, needs_external: true, steps_known: false, independent_parts: true, value: 0.05, chat_cost: 0.02 },
];

/** The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost. */
export function choosePattern(task: Task): string {
  log.debug("choosePattern input", task);
  if (task.one_step) return task.needs_external ? "augmented call" : "plain call";
  if (task.steps_known) return "workflow";
  if (task.independent_parts && task.value >= MULTIPLIER["multi-agent"] * task.chat_cost) return "multi-agent";
  return "agent";
}

export function cost(task: Task, pattern: string): number {
  const multiplier = pattern === "workflow" ? task.steps : MULTIPLIER[pattern];
  return multiplier * task.chat_cost;
}

function main() {
  for (const task of TASKS) {
    const pattern = choosePattern(task);
    const price = cost(task, pattern);
    console.log(`${task.name}: ${pattern}, cost ${price.toFixed(2)}, value ${task.value.toFixed(2)}, pays: ${task.value >= price ? "True" : "False"}`);
  }
}

if (import.meta.main) main();
```
```text
classify ticket: plain call, cost 0.02, value 0.05, pays: True
answer from policy: augmented call, cost 0.02, value 0.40, pays: True
claims intake: workflow, cost 0.08, value 6.00, pays: True
investigate outage: agent, cost 0.08, value 40.00, pays: True
market research brief: multi-agent, cost 0.30, value 25.00, pays: True
trivia round-up: agent, cost 0.08, value 0.05, pays: False
```
```java
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The pattern ladder: which rung a task needs, and whether its value pays for the rung.
 *
 * <p>Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
 * complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
 * interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
 * the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
 * a measurement of your workload, and the rule is the course's own teaching model.
 */
public final class PatternLadder {
    private static final System.Logger LOG = System.getLogger(PatternLadder.class.getName());
    record Task(String name, boolean oneStep, int steps, boolean needsExternal, boolean stepsKnown, boolean independentParts, double value, double chatCost) {}

    static final Map<String, Integer> MULTIPLIER = Map.of("plain call", 1, "augmented call", 1, "agent", 4, "multi-agent", 15); // a workflow costs one chat per step

    static final List<Task> TASKS = List.of(
        new Task("classify ticket", true, 1, false, true, false, 0.05, 0.02),
        new Task("answer from policy", true, 1, true, true, false, 0.40, 0.02),
        new Task("claims intake", false, 4, true, true, false, 6.00, 0.02),
        new Task("investigate outage", false, 0, true, false, false, 40.00, 0.02),
        new Task("market research brief", false, 0, true, false, true, 25.00, 0.02),
        new Task("trivia round-up", false, 0, true, false, true, 0.05, 0.02));

    /** The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost. */
    static String choosePattern(Task task) {
        LOG.log(System.Logger.Level.DEBUG, "choosePattern input: {0}", task);
        if (task.oneStep()) return task.needsExternal() ? "augmented call" : "plain call";
        if (task.stepsKnown()) return "workflow";
        if (task.independentParts() && task.value() >= MULTIPLIER.get("multi-agent") * task.chatCost()) return "multi-agent";
        return "agent";
    }

    static double cost(Task task, String pattern) {
        int multiplier = pattern.equals("workflow") ? task.steps() : MULTIPLIER.get(pattern);
        return multiplier * task.chatCost();
    }

    public static void main(String[] args) {
        for (Task task : TASKS) {
            String pattern = choosePattern(task);
            double price = cost(task, pattern);
            System.out.println(String.format(Locale.ROOT, "%s: %s, cost %.2f, value %.2f, pays: %s", task.name(), pattern, price, task.value(), task.value() >= price ? "True" : "False"));
        }
    }
}
```
```text
classify ticket: plain call, cost 0.02, value 0.05, pays: True
answer from policy: augmented call, cost 0.02, value 0.40, pays: True
claims intake: workflow, cost 0.08, value 6.00, pays: True
investigate outage: agent, cost 0.08, value 40.00, pays: True
market research brief: multi-agent, cost 0.30, value 25.00, pays: True
trivia round-up: agent, cost 0.08, value 0.05, pays: False
```
```kotlin
private val log = System.getLogger("pattern_ladder")

/**
 * The pattern ladder: which rung a task needs, and whether its value pays for the rung.
 *
 * Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
 * complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
 * interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
 * the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
 * a measurement of your workload, and the rule is the course's own teaching model.
 */
data class Task(
    val name: String, val oneStep: Boolean, val steps: Int, val needsExternal: Boolean, val stepsKnown: Boolean,
    val independentParts: Boolean, val value: Double, val chatCost: Double,
)

val MULTIPLIER = mapOf("plain call" to 1, "augmented call" to 1, "agent" to 4, "multi-agent" to 15) // a workflow costs one chat per step

val TASKS = listOf(
    Task("classify ticket", true, 1, false, true, false, 0.05, 0.02),
    Task("answer from policy", true, 1, true, true, false, 0.40, 0.02),
    Task("claims intake", false, 4, true, true, false, 6.00, 0.02),
    Task("investigate outage", false, 0, true, false, false, 40.00, 0.02),
    Task("market research brief", false, 0, true, false, true, 25.00, 0.02),
    Task("trivia round-up", false, 0, true, false, true, 0.05, 0.02),
)

/** The lowest rung that fits: a call, an augmented call, a workflow, an agent, and a team only when its value covers the team's cost. */
fun choosePattern(task: Task): String {
    log.log(System.Logger.Level.DEBUG, "choosePattern input: {0}", task)
    return when {
        task.oneStep -> if (task.needsExternal) "augmented call" else "plain call"
        task.stepsKnown -> "workflow"
        task.independentParts && task.value >= MULTIPLIER.getValue("multi-agent") * task.chatCost -> "multi-agent"
        else -> "agent"
    }
}

fun cost(task: Task, pattern: String): Double = (if (pattern == "workflow") task.steps else MULTIPLIER.getValue(pattern)) * task.chatCost

private fun py(value: Boolean) = if (value) "True" else "False"

fun main() {
    for (task in TASKS) {
        val pattern = choosePattern(task)
        val price = cost(task, pattern)
        println("${task.name}: $pattern, cost ${"%.2f".format(price)}, value ${"%.2f".format(task.value)}, pays: ${py(task.value >= price)}")
    }
}
```
```text
classify ticket: plain call, cost 0.02, value 0.05, pays: True
answer from policy: augmented call, cost 0.02, value 0.40, pays: True
claims intake: workflow, cost 0.08, value 6.00, pays: True
investigate outage: agent, cost 0.08, value 40.00, pays: True
market research brief: multi-agent, cost 0.30, value 25.00, pays: True
trivia round-up: agent, cost 0.08, value 0.05, pays: False
```
<!-- /example -->

The output is the same in all four languages. Six tasks land on five rungs, and five of the six pay. The one that does not pay is the trivia round-up: its parts are independent, but its value is below the price of a team, so the rule keeps it on the agent rung, where its value still does not cover the cost.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Use a coordinator with subagents, because the task has several parts."** It is tempting because several parts look like parallel work. The exam rejects it: parts that depend on one another or share one context cannot be split, and a team costs about 15 times a chat, so it needs independence, volume and value together.
2. **"Review the model's prompt and choose the best model; the architecture is the details."** It is tempting because the prompt is the visible part. The exam rejects it: the missing stage, usually the feedback loop, is an architecture finding that no prompt can fix.
3. **"Pick the pattern from its price alone, since the cheapest design is the best one."** It is tempting because cost is easy to compare. The exam rejects it: the cheapest design counts only among those that pass the review, and a design with a missing stage or an unapproved write is rejected first.

## Quiz

1. Scenario: Cobalt Logistics has three proposals for a returns-handling service. One has no way to learn from wrong answers, one is a workflow and one is a team of agents. A board member wants the cheapest. What should the architect do first?
   - **a**: Reject the cheapest design if the board has not yet seen its performance figures on live data
   - **b**: Judge all of them on the same four stages and drop any that lacks one
   - **c**: Ask each team to run its design on a hundred real returns and compare the token bills
   - **d**: Choose the workflow because workflows cost less than an agent on this workload

2. Scenario: Heron Analytics' task splits into six independent parts, and one context could not hold all the reading, but its value is about the cost of a single chat. What does the rule conclude?
   - **a**: Use the team, since independence and volume are the hard conditions and value follows
   - **b**: Use the team, since value can be raised later by adding more agents to it
   - **c**: Neither level of autonomy pays, so leave it on a plain call or drop it
   - **d**: Use one agent, since a team needs only independence among the parts

<details>
<summary>Answer key</summary>

1. **b**. A review applies the same questions to every design, and the missing stage is the first finding. *a* is ruled out because rejection follows a missing stage, not missing live figures: "a design with a missing stage or an unapproved write is rejected first". *c* is ruled out because price is compared only "among those that pass the review". *d* is ruled out because the page prices a workflow at "one chat per step", so a long workflow can cost more than the rung above it.
2. **c**. Two of the three conditions hold and the third fails, so no autonomous rung pays. *a* is ruled out because "a team that fails the third loses money on every run". *b* is ruled out because the page says "it needs independence, volume and value together", and adding agents raises cost, not value. *d* is ruled out because for this task "its value is below what even one agent costs".

</details>
