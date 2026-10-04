// The pattern ladder: which rung a task needs, and whether its value pays for the rung.
//
// Anthropic's article "Building effective agents" (read on 2026-10-04) says to "find the simplest solution possible, and only increasing
// complexity when needed", and its multi-agent research write-up reports that "agents typically use about 4× more tokens than chat
// interactions, and multi-agent systems use about 15× more tokens than chats". This file turns those two statements into a rule that picks
// the lowest rung a task can stand on and prices it with the article's multipliers. The multipliers are the articles' reported figures, not
// a measurement of your workload, and the rule is the course's own teaching model.

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
