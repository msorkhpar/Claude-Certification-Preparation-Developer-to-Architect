import { logger } from "./logger.ts";
const log = logger("refinement");
/**
 * Three decisions of a Claude Code session on a code-generation task: plan mode or direct execution, one message or several for a list of problems, and what a failing test run must say.
 *
 * The rules are the ones the exam guide states for tasks 3.4 and 3.5 and the best-practices page confirms (read on 2026-10-03): plan when the change is large, architectural,
 * touches many files or has more than one valid approach; execute directly when you could describe the diff in one sentence; send interacting problems in one message and
 * independent problems one after another; and give the model the failing tests, with input and expected output, as the target. No model is called.
 */
export type Task = { diff_in_one_sentence: boolean; files: number; architectural: boolean; approaches: number };
export type Issue = { id: string; interacts_with?: string[] };
export type Result = { name: string; input: unknown; expected: unknown; actual: unknown };

/** Phases of the work: `plan` first when the change is large, architectural, spread over files or has several valid approaches. */
export function chooseMode(task: Task): string[] {
  const small = task.diff_in_one_sentence && task.files <= 1 && !task.architectural;
  if (small) return ["implement"];
  if (task.architectural || task.approaches > 1 || task.files > 1) return ["explore", "plan", "implement"];
  return ["implement"];
}

/** Messages to send, in order: problems that interact travel together, independent ones go one at a time. */
export function groupFeedback(issues: Issue[]): string[][] {
  const parent = new Map(issues.map((i) => [i.id, i.id]));
  const find = (x: string): string => {
    while (parent.get(x) !== x) {
      parent.set(x, parent.get(parent.get(x) as string) as string);
      x = parent.get(x) as string;
    }
    return x;
  };
  for (const issue of issues) {
    for (const other of issue.interacts_with ?? []) if (parent.has(other)) parent.set(find(issue.id), find(other));
  }
  const groups = new Map<string, string[]>();
  for (const issue of issues) {
    const root = find(issue.id);
    groups.set(root, [...(groups.get(root) ?? []), issue.id]);
  }
  return [...groups.values()];
}

const show = (v: unknown): string => (v === undefined ? "None" : v === null ? "null" : JSON.stringify(v));

/** The message that closes the loop: each failing test with its input, the expected output and the actual one; nothing about the passing tests. */
export function failureReport(results: Result[]): string {
  const failing = results.filter((r) => JSON.stringify(r.actual) !== JSON.stringify(r.expected));
  if (failing.length === 0) return "All tests pass.";
  const lines = [`${failing.length} of ${results.length} tests fail:`];
  for (const r of failing) lines.push(`- ${r.name}: input ${show(r.input)}, expected ${show(r.expected)}, got ${show(r.actual)}`);
  return lines.join("\n");
}

function main() {
  const tasks: Record<string, Task> = {
    "rename a variable in one function": { diff_in_one_sentence: true, files: 1, architectural: false, approaches: 1 },
    "add a date check to one handler": { diff_in_one_sentence: true, files: 1, architectural: false, approaches: 1 },
    "split a monolith into services": { diff_in_one_sentence: false, files: 60, architectural: true, approaches: 3 },
    "migrate a library used in 45 files": { diff_in_one_sentence: false, files: 45, architectural: false, approaches: 1 },
  };
  for (const [name, task] of Object.entries(tasks)) console.log(`${name}: ${chooseMode(task).join(" > ")}`);
  const issues: Issue[] = [{ id: "sort-order", interacts_with: ["pagination"] }, { id: "pagination", interacts_with: ["sort-order"] }, { id: "typo-in-label", interacts_with: [] }, { id: "null-date", interacts_with: [] }];
  console.log("messages:", JSON.stringify(groupFeedback(issues)));
  const results: Result[] = [{ name: "keeps order", input: [3, 1, 2], expected: [1, 2, 3], actual: [1, 2, 3] }, { name: "empty list", input: [], expected: [], actual: null }, { name: "null entry", input: [2, null], expected: [2], actual: [2, null] }];
  console.log(failureReport(results));
}

if (import.meta.main) main();
