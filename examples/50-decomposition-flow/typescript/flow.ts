// Two decompositions with scripted model replies: a per-file pass followed by one cross-file pass, and an adaptive loop that plans each step from the last.
//
// The "model" is a set of hand-written functions, so the output shows what each pass was given and what the control flow did with the answers, and nothing about
// how a real model would review the code. The files, summaries and findings are illustrative.
export const CHANGE: Record<string, string> = {
  "api.py": "def get_user(id):\n    return db.find(id)\n",
  "db.py": "def find(name):\n    return rows.get(name)\n",
  "ui.py": "def show(user):\n    print(user['name'])\n",
};
export const SUMMARIES: Record<string, string> = { "api.py": "get_user passes an id to db.find", "db.py": "find looks a row up by name", "ui.py": "show prints the name field" };

const lineCount = (text: string) => text.split("\n").filter((_, i, all) => i < all.length - 1 || all[i] !== "").length;

/** One call per file: it is given this file and nothing else. */
function filePass(path: string, text: string): string {
  console.log(`  file pass ${path}: ${lineCount(text)} lines, no other file`);
  return SUMMARIES[path];
}

/** One call over the summaries: the relations between files, never the text. */
function crossPass(summaries: Array<[string, string]>): string[] {
  const withheld = Object.values(CHANGE).reduce((n, s) => n + s.length, 0);
  console.log(`  cross pass: ${summaries.length} summaries, ${withheld} characters of source withheld`);
  const names = Object.fromEntries(summaries);
  return names["api.py"].includes("id") && names["db.py"].includes("name") ? ["api.py passes an id but db.py looks up by name"] : [];
}

type Step = [string, string];

/** A scripted planner: what it answers depends on what the steps so far found. */
export function plan(goal: string, steps: Step[]): { done: boolean; next?: string; summary?: string } {
  const done = steps.map(([s]) => s);
  if (steps.length === 0) return { done: false, next: "list the test files" };
  if (done.includes("list the test files") && !done.includes("run the failing test")) return { done: false, next: "run the failing test" };
  if (done.includes("read the module under test")) return { done: true, summary: "the failure is in parse()" };
  if (steps[steps.length - 1][1].startsWith("1 failure")) return { done: false, next: "read the module under test" };
  return { done: true, summary: "nothing failed" };
}

export function work(subtask: string): string {
  return ({ "list the test files": "3 files", "run the failing test": "1 failure in test_parse", "read the module under test": "parse() drops the last field" } as Record<string, string>)[subtask];
}

export function runAdaptive(goal: string, maxSteps = 5): [string, Step[], string] {
  const steps: Step[] = [];
  while (true) {
    const reply = plan(goal, [...steps]);
    if (reply.done) return ["done", steps, reply.summary ?? ""];
    if (steps.length >= maxSteps) return ["step_limit", steps, ""];
    steps.push([reply.next as string, work(reply.next as string)]);
    console.log(`  step ${steps.length}: ${reply.next} -> ${steps[steps.length - 1][1]}`);
  }
}

function main() {
  console.log("per file, then across files:");
  const summaries: Array<[string, string]> = Object.entries(CHANGE).map(([path, text]) => [path, filePass(path, text)]);
  for (const finding of crossPass(summaries)) console.log("  finding:", finding);
  console.log("\nadaptive, each step planned from the last:");
  const [status, steps, summary] = runAdaptive("find why the parser test fails");
  console.log(`  status: ${status} after ${steps.length} steps; ${summary}`);
}

if (import.meta.main) main();
