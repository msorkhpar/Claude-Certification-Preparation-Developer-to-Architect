// Task decomposition: a per-item pass and a cross-item pass, an adaptive loop, and the choice between them. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("decompose");
type FilePass = (path: string, text: string, part: number, parts: number) => { findings: string[]; summary: string };

// Review each file alone (long files in parts), then let one pass read only the summaries of the files that were reviewed.
export function reviewChanges(files: Array<{ path: string; text: string }>, filePass: FilePass, crossPass: (summaries: Array<{ path: string; summary: string }>) => string[], maxLines = 40): any {
  log.debug("reviewChanges input", files);
  const reviewed: Record<string, { findings: string[]; summary: string; parts: number }> = {};
  const failed: Record<string, string> = {};
  const skipped: string[] = [];
  for (const { path, text } of files) {
    // TODO 2 of 9 (finish this to pass e2): the blank file rule. When the file's text is blank, add its path to
    //   `skipped` and go on to the next file. Example: "\n  \n" -> skipped.
    const lines = text.split(/\r?\n/);
    if (lines[lines.length - 1] === "") lines.pop();
    // TODO 3 of 9 (finish this to pass e2): the number of parts. Receives the lines of the file and max_lines. Return
    //   how many parts of at most max_lines lines the file needs, rounding up. Example: 41 lines, max_lines 40 -> 2.
    const parts = 1;
    const findings: string[] = [];
    const summaries: string[] = [];
    try {
      for (let part = 0; part < parts; part++) {
        // TODO 1 of 9 (finish this to pass m1, e1): the review of one part of a file. Slice the part's lines (part
        //   number 1..parts, max_lines per part), call the file pass with (path, text of the part, part number from 1,
        //   parts), add its findings to `findings` and its summary to `summaries`. Example: a 5 line file, max_lines 3 ->
        //   two calls, parts 1 and 2 of 2.
      }
    } catch (error) {
      // TODO 4 of 9 (finish this to pass e3): the failing file. When the file pass throws, record the path with the
      //   error's message in `failed` and go on to the next file. Example: the pass raises "boom" for a.py -> failed {a.py:
      //   "boom"}, and b.py is still reviewed.
      continue;
    }
    reviewed[path] = { findings, summary: summaries.join(" "), parts };
  }
  let cross: string[] = [];
  let crossError: string | null = null;
  // TODO 5 of 9 (finish this to pass e3): the guard of the cross pass. Run it only when at least two files were reviewed
  //   (a relation between files needs two). Example: one reviewed file and one failed -> no cross pass.
  if (Object.keys(reviewed).length >= 0) {
    try {
      // TODO 6 of 9 (finish this to pass m1, e1): the cross pass call. Call the cross pass with one {path, summary}
      //   entry per reviewed file (the joined summary, never the text) and keep what it returns in `cross`. Example: two
      //   files -> one call with two entries.
      cross = [];
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
    // TODO 7 of 9 (finish this to pass e4): the question to the planner. Call the planner with the goal and a copy of
    //   the steps done so far (each {subtask, result}). Example: after two steps the planner receives a list of two.
    const plan = planner(goal, []);
    if (plan === null || typeof plan !== "object" || Array.isArray(plan) || typeof plan.done !== "boolean") return finish("bad_plan", "", "the planner reply could not be read");
    if (plan.done) return finish("done", String(plan.summary || ""));
    const subtask = String(plan.next || "").trim();
    // TODO 8 of 9 (finish this to pass e5): the stuck rules. When the planner gives no next step, finish with status
    //   stuck and the reason "no next step"; when the next step was already done (compare ignoring case), finish with
    //   status stuck and the reason "repeated subtask: NAME". Example: next "Fix A" after "fix a" -> stuck.
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
  // TODO 9 of 9 (finish this to pass e6): the choice. Receives the validated fields. Return adaptive when the steps are
  //   not known; otherwise per_item_then_cross when there are at least 2 items and items_interact is true; otherwise
  //   fixed_chain. Example: steps known, 3 items that interact -> per_item_then_cross.
  return "fixed_chain";
}
