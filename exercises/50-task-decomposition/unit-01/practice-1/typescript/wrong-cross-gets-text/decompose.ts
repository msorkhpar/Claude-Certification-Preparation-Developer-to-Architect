// Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md.
type FilePass = (path: string, text: string, part: number, parts: number) => { findings: string[]; summary: string };

// Review each file alone (long files in parts), then let one pass read only the summaries of the files that were reviewed.
export function reviewChanges(files: Array<{ path: string; text: string }>, filePass: FilePass, crossPass: (summaries: Array<{ path: string; summary: string }>) => string[], maxLines = 40): any {
  const reviewed: Record<string, { findings: string[]; summary: string; parts: number }> = {};
  const failed: Record<string, string> = {};
  const skipped: string[] = [];
  for (const { path, text } of files) {
    if (!text.trim()) {
      skipped.push(path); // nothing to review is not worth a model call
      continue;
    }
    const lines = text.split(/\r?\n/);
    if (lines[lines.length - 1] === "") lines.pop();
    const parts = Math.ceil(lines.length / maxLines);
    const findings: string[] = [];
    const summaries: string[] = [];
    try {
      for (let part = 0; part < parts; part++) {
        const result = filePass(path, lines.slice(part * maxLines, (part + 1) * maxLines).join("\n"), part + 1, parts);
        findings.push(...result.findings);
        summaries.push(result.summary);
      }
    } catch (error) { // one file failing must not stop the others
      failed[path] = error instanceof Error ? error.message : String(error);
      continue;
    }
    reviewed[path] = { findings, summary: summaries.join(" "), parts };
  }
  let cross: string[] = [];
  let crossError: string | null = null;
  if (Object.keys(reviewed).length >= 2) { // a relation between files needs at least two of them
    try {
      cross = [...crossPass(Object.entries(reviewed).map(([path, r]) => ({ path, summary: r.summary, text: "source" })))];
    } catch (error) {
      crossError = error instanceof Error ? error.message : String(error);
    }
  }
  return { files: reviewed, cross, failed, skipped, cross_error: crossError };
}

// Ask the planner what to do next after every step, and stop when it is done, stuck or out of steps.
export function runAdaptive(planner: (goal: string, steps: any[]) => any, worker: (subtask: string) => string, goal: string, maxSteps = 6): any {
  const steps: Array<{ subtask: string; result: string }> = [];
  const finish = (status: string, summary = "", reason = "") => ({ status, summary, steps, reason });
  while (true) {
    const plan = planner(goal, steps.map((s) => ({ ...s })));
    if (plan === null || typeof plan !== "object" || Array.isArray(plan) || typeof plan.done !== "boolean") return finish("bad_plan", "", "the planner reply could not be read");
    if (plan.done) return finish("done", String(plan.summary || ""));
    const subtask = String(plan.next || "").trim();
    if (!subtask) return finish("stuck", "", "no next step");
    if (steps.some((s) => s.subtask.toLowerCase() === subtask.toLowerCase())) return finish("stuck", "", `repeated subtask: ${subtask}`);
    if (steps.length >= maxSteps) return finish("step_limit", "", "step limit reached");
    let result: string;
    try {
      result = worker(subtask);
    } catch (error) { // the planner decides what a failed step means
      result = `ERROR: ${error instanceof Error ? error.message : String(error)}`;
    }
    steps.push({ subtask, result });
  }
}

// fixed_chain, per_item_then_cross or adaptive, from what is known about the steps and whether the items affect each other.
export function chooseStrategy(task: Record<string, unknown>): string {
  const { steps_known: stepsKnown, items } = task;
  if (typeof stepsKnown !== "boolean" || typeof items !== "number" || !Number.isInteger(items) || items < 0) {
    throw new Error("steps_known must be true or false and items a whole number of at least 0");
  }
  if (!stepsKnown) return "adaptive";
  if (items >= 2 && task.items_interact === true) return "per_item_then_cross";
  return "fixed_chain";
}
